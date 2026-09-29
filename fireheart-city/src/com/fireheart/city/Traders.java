package com.fireheart.city;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Residents who spot a wandering trader (or a stray villager) walk over and haggle. A fair deal makes their day;
 * a rip-off makes them furious, and they don't take it lying down.
 */
public final class Traders {
    private Traders() {}

    static final class Haggle {
        UUID trader;
        int stage;
        int t;
        boolean good;
        String offer = "", price = "";
        int hits;
    }

    static final Map<UUID, Haggle> ACTIVE = new HashMap<>();
    static final String DONE = "fhcHaggled";

    public static void reset() {
        ACTIVE.clear();
    }

    public static Vec3 target(Resident r) {
        Haggle h = ACTIVE.get(r.getUUID());
        if (h == null || !(r.level() instanceof ServerLevel sl)) return null;
        Entity e = sl.getEntity(h.trader);
        return e == null ? null : e.position();
    }

    public static void scan(ServerLevel sl, CityData d) {
        ACTIVE.values().removeIf(h -> !(sl.getEntity(h.trader) instanceof AbstractVillager v) || !v.isAlive());
        for (AbstractVillager v : sl.getEntitiesOfClass(AbstractVillager.class, new AABB(-220, 50, -220, 220, 260, 380), Traders::wanderer)) {
            if (v.getPersistentData().getBoolean(DONE)) continue;
            boolean taken = false;
            for (Haggle h : ACTIVE.values()) if (h.trader.equals(v.getUUID())) taken = true;
            if (taken) continue;
            Resident best = null;
            double bd = 18 * 18;
            for (Resident r : sl.getEntitiesOfClass(Resident.class, v.getBoundingBox().inflate(18, 6, 18), r -> r.profile() != null && r.isFree() && !ACTIVE.containsKey(r.getUUID()))) {
                CityData.Profile p = r.profile();
                if (p.job == Job.POLICE || p.job == Job.FIREFIGHTER || p.job == Job.RECEPTIONIST || p.job == Job.REPAIR) continue;
                double dd = r.distanceToSqr(v);
                if (dd < bd && r.hasLineOfSight(v)) { bd = dd; best = r; }
            }
            if (best == null) continue;
            Haggle h = new Haggle();
            h.trader = v.getUUID();
            ACTIVE.put(best.getUUID(), h);
            v.getPersistentData().putBoolean(DONE, true);
            best.say(best.pick("Ooh, a travelling trader! Let's see what they've got.", "Hey! You there with the llamas - what are you selling?", "A wandering merchant in Solaris? This I gotta see.", "Psst - got anything good today?"), 50);
        }
    }

    static boolean wanderer(AbstractVillager v) {
        if (!v.isAlive() || v.isBaby()) return false;
        if (v instanceof WanderingTrader) return true;
        return v instanceof Villager vi && vi.getVillagerData().getProfession() != net.minecraft.world.entity.npc.VillagerProfession.NONE && vi.getOffers() != null && !vi.getOffers().isEmpty();
    }

    /** Runs a haggling resident. Returns true while the haggle has control. */
    public static boolean tick(Resident r, CityData.Profile p) {
        Haggle h = ACTIVE.get(r.getUUID());
        if (h == null) return false;
        ServerLevel sl = (ServerLevel) r.level();
        if (!(sl.getEntity(h.trader) instanceof AbstractVillager v) || !v.isAlive()) {
            if (h.stage == 3) celebrateKill(r, p, sl);
            ACTIVE.remove(r.getUUID());
            return false;
        }
        if (r.convo != null || r.isSleeping()) { ACTIVE.remove(r.getUUID()); return false; }
        RandomSource rnd = r.getRandom();
        r.getLookControl().setLookAt(v, 40, 40);
        double dist = r.distanceTo(v);
        h.t++;
        if (h.t > 20 * 90) { ACTIVE.remove(r.getUUID()); return false; }
        if (h.stage == 0) {
            if (dist > 2.4) {
                if (h.t % 10 == 1) r.getNavigation().moveTo(v, h.stage == 3 ? 1.5 : 1.0);
                return true;
            }
            r.getNavigation().stop();
            v.getNavigation().stop();
            v.getLookControl().setLookAt(r, 40, 40);
            pickOffer(v, h, p, rnd);
            h.stage = 1;
            h.t = 0;
            r.gesture(Resident.G_POINT, 30);
            r.sayTo(rnd.nextBoolean() ? "What'll it cost me for the " + h.offer + "?" : "How much for your " + h.offer + "?", 50);
            return true;
        }
        v.getNavigation().stop();
        v.getLookControl().setLookAt(r, 40, 40);
        if (h.stage == 1) {
            if (h.t == 40) {
                v.playSound(SoundEvents.WANDERING_TRADER_YES, 1f, 1f);
                v.setUnhappyCounter(0);
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, v.getX(), v.getY() + 2, v.getZ(), 3, 0.3, 0.2, 0.3, 0);
                r.sayTo(h.price + "? " + (h.good ? "Hmm..." : "...excuse me?"), 40);
                r.gesture(Resident.G_THINK, 30);
            }
            if (h.t == 90) {
                if (h.good) {
                    r.gesture(Resident.G_THUMBS, 40);
                    r.sayTo(Lines.pick(rnd, "Deal! Pleasure doing business.", "Sold! That's a steal.", "You drive a fair bargain, stranger.", "Wrap it up, I'll take it!"), 50);
                    v.playSound(SoundEvents.WANDERING_TRADER_TRADE, 1f, 1f);
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, v.getX(), v.getY() + 1.5, v.getZ(), 10, 0.4, 0.4, 0.4, 0);
                    p.log(r.routineDay()).note("I bought " + h.offer + " from a wandering trader for " + h.price);
                    r.data().event(r.day(), "social", p.name + " struck a deal with a wandering trader", r.blockPosition(), p.id);
                    ACTIVE.remove(r.getUUID());
                    return false;
                }
                r.gesture(Resident.G_ANGRY, 40);
                r.sayTo(Lines.pick(rnd, h.price + "?! For THAT? You're a crook!", "That's daylight robbery!", "Are you kidding me? You're scamming the whole city!", "Nobody rips off a Solaris local. NOBODY."), 60);
                v.playSound(SoundEvents.WANDERING_TRADER_NO, 1f, 1f);
                sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, r.getX(), r.getY() + 2.1, r.getZ(), 4, 0.3, 0.2, 0.3, 0);
                h.stage = 2;
                h.t = 0;
            }
            return true;
        }
        if (h.stage == 2) {
            if (h.t == 30) {
                r.sayTo(Lines.pick(rnd, "That's it. You're done, pal!", "I'll give you a deal - a knuckle sandwich!", "Right. Say goodbye to your llamas."), 50);
                r.gesture(Resident.G_GUARD, 20);
                h.stage = 3;
                h.t = 0;
                for (Resident o : sl.getEntitiesOfClass(Resident.class, r.getBoundingBox().inflate(12), x -> x != r && x.isFree())) {
                    if (rnd.nextInt(2) == 0) o.say(o.pick("Whoa, whoa! " + p.name + "?!", "Somebody stop " + p.name + "!", "Get 'em, " + p.name + "!", "Oh no, not again..."), 40);
                }
            }
            return true;
        }
        if (dist > 2.3) {
            if (h.t % 5 == 0) r.getNavigation().moveTo(v, 1.45);
            return true;
        }
        r.getNavigation().stop();
        if (h.t % 9 == 0) {
            boolean right = (h.hits++ % 2) == 0;
            r.gesture(right ? Resident.G_JAB_R : Resident.G_JAB_L, 6);
            v.invulnerableTime = 0;
            v.hurt(sl.damageSources().mobAttack(r), 3f + rnd.nextFloat() * 2f);
            Vec3 kb = v.position().subtract(r.position()).multiply(1, 0, 1).normalize().scale(0.35);
            v.setDeltaMovement(v.getDeltaMovement().add(kb.x, 0.2, kb.z));
            v.hurtMarked = true;
            sl.sendParticles(ParticleTypes.CRIT, v.getX(), v.getY() + 1.2, v.getZ(), 6, 0.3, 0.3, 0.3, 0.3);
            sl.playSound(null, v.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.NEUTRAL, 1f, 0.9f + rnd.nextFloat() * 0.2f);
            if (h.hits % 4 == 0) r.sayTo(Lines.pick(rnd, "Refund THIS!", "Customer service!", "Here's my review!", "No returns!"), 20);
        }
        return true;
    }

    static void pickOffer(AbstractVillager v, Haggle h, CityData.Profile p, RandomSource rnd) {
        MerchantOffer o = null;
        if (v.getOffers() != null && !v.getOffers().isEmpty()) o = v.getOffers().get(rnd.nextInt(v.getOffers().size()));
        if (o == null) {
            h.offer = "stuff";
            h.price = "9 emeralds";
            h.good = rnd.nextBoolean();
            return;
        }
        ItemStack res = o.getResult();
        ItemStack cost = o.getCostA();
        h.offer = (res.getCount() > 1 ? res.getCount() + " " : "") + res.getHoverName().getString().toLowerCase(java.util.Locale.ROOT);
        h.price = cost.getCount() + " " + cost.getHoverName().getString().toLowerCase(java.util.Locale.ROOT);
        int emeralds = cost.is(Items.EMERALD) ? cost.getCount() : cost.getCount() / 2;
        double tolerance = switch (p.trait) {
            case GRUMPY -> 2;
            case LAIDBACK, CHEERFUL -> 7;
            case FRIENDLY, DREAMY -> 6;
            default -> 4.5;
        };
        double value = tolerance + rnd.nextGaussian() * 2 + (res.getRarity() != net.minecraft.world.item.Rarity.COMMON ? 4 : 0);
        h.good = emeralds <= value;
    }

    static void celebrateKill(Resident r, CityData.Profile p, ServerLevel sl) {
        r.gesture(Resident.G_VICTORY, 40);
        r.sayTo(Lines.pick(r.getRandom(), "And THAT'S what happens when you rip off Solaris.", "Scam somewhere else. Oh wait - you can't.", "Consumer protection, Solaris style.", "Hmph. Should've given me a fair price."), 60);
        p.log(r.routineDay()).note("A wandering trader tried to rip me off, so I took them out");
        r.data().event(r.day(), "social", p.name + " got into a brawl with a crooked wandering trader - the trader lost", r.blockPosition(), p.id);
    }

    public static boolean busy(Resident r) {
        return ACTIVE.containsKey(r.getUUID());
    }

    static void cleanup() {
        for (Iterator<Haggle> it = ACTIVE.values().iterator(); it.hasNext(); ) if (it.next() == null) it.remove();
    }
}
