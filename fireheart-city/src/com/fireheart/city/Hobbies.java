package com.fireheart.city;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Little everyday things residents do: passing hellos, picnics, selfies, rain dancing, snowball fights, yoga and moods. */
public final class Hobbies {
    private Hobbies() {}

    static final Map<String, Long> COOL = new HashMap<>();

    static boolean ready(String key, long now, long gap) {
        Long t = COOL.get(key);
        if (t != null && now - t < gap && now >= t) return false;
        COOL.put(key, now);
        return true;
    }

    static boolean outside(Resident r) {
        return r.level().canSeeSky(r.blockPosition().above());
    }

    public static void tick(ServerLevel sl, CityData d) {
        long gt = sl.getGameTime();
        if (gt % 20 != 17) return;
        if (COOL.size() > 4000) COOL.clear();
        long day = Calendar.worldDay(sl);
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        RandomSource rnd = sl.getRandom();
        boolean winter = Skies.seasonIndex(day) == 3;
        for (Resident r : Skies.residents(sl, d)) {
            if (!r.isSleeping() && !r.inShuttle() && r.profile() != null) body(sl, r, gt, tod, winter, rnd);
        }
        for (Resident r : Skies.residents(sl, d)) {
            CityData.Profile p = r.profile();
            if (p == null || r.isSleeping() || r.inShuttle() || !r.isFree() || r.isSpeaking()) continue;
            if (passingHello(sl, d, r, p, gt, rnd)) continue;
            if (flyers(sl, r, gt, rnd)) continue;
            if (sl.isRaining() && outside(r) && rainDance(r, p, gt, rnd)) continue;
            if (winter && !sl.isRaining() && outside(r) && snowball(sl, r, p, gt, rnd)) continue;
            if (!sl.isRaining() && tod < 11500 && picnic(sl, d, r, p, day, rnd)) continue;
            if (selfie(sl, d, r, p, day, rnd)) continue;
            mood(sl, r, p, gt, rnd);
        }
    }

    /* ------------------------------------------------------------ Body realism */

    static final Map<String, Long> WET = new HashMap<>();

    /** Dripping after rain, visible breath in the cold, and the odd slip on wet ground. */
    static void body(ServerLevel sl, Resident r, long now, long tod, boolean winter, RandomSource rnd) {
        String id = r.profileId();
        boolean out = outside(r);
        if (sl.isRaining() && out) WET.put(id, now + 900);
        else {
            Long w = WET.get(id);
            if (w != null && w > now && rnd.nextFloat() < 0.35f) sl.sendParticles(ParticleTypes.DRIPPING_WATER, r.getX(), r.getY() + 1.2 + rnd.nextFloat() * 0.6, r.getZ(), 1, 0.2, 0.2, 0.2, 0);
            else if (w != null && w <= now) WET.remove(id);
        }
        boolean cold = winter || r.onIsland() && (tod > 13000 && tod < 23000);
        if (cold && out && rnd.nextFloat() < 0.3f) {
            Vec3 look = r.getLookAngle();
            sl.sendParticles(ParticleTypes.CLOUD, r.getX() + look.x * 0.35, r.getEyeY() - 0.1, r.getZ() + look.z * 0.35, 1, 0.02, 0.02, 0.02, 0.004);
        }
        if (sl.isRaining() && out && r.getDeltaMovement().horizontalDistanceSqr() > 0.03 && rnd.nextFloat() < 0.004f && ready("slip:" + id, now, 6000)) {
            r.gesture(Resident.G_SURPRISED, 20);
            r.setDeltaMovement(r.getDeltaMovement().multiply(0.2, 1, 0.2).add(0, 0.25, 0));
            r.say(r.pick("Whoa! Slippery!", "Woah-oh-oh! ...Phew.", "Nearly went flying there!"), 40);
            sl.sendParticles(ParticleTypes.SPLASH, r.getX(), r.getY() + 0.1, r.getZ(), 10, 0.3, 0.05, 0.3, 0.1);
        }
    }

    /* ------------------------------------------------------------ Passing hellos */

    static boolean passingHello(ServerLevel sl, CityData d, Resident r, CityData.Profile p, long now, RandomSource rnd) {
        if (r.getNavigation().isDone() || rnd.nextFloat() > 0.25f) return false;
        for (Resident o : sl.getEntitiesOfClass(Resident.class, r.getBoundingBox().inflate(6, 2, 6), x -> x != r && x.profile() != null && x.isFree() && !x.isSpeaking())) {
            CityData.Rel rel = d.peekRel(p.id, o.profileId());
            if (rel == null || !rel.met || rel.rival || !r.canSee(o, 6)) continue;
            if (!ready("hi:" + Resident.pairKey(p.id, o.profileId()), now, 3600)) continue;
            CityData.Profile op = o.profile();
            r.getLookControl().setLookAt(o, 30, 30);
            o.getLookControl().setLookAt(r, 30, 30);
            r.gesture(Resident.G_WAVE, 30);
            o.gesture(Resident.G_WAVE, 30);
            boolean close = rel.friend() || op.id.equals(p.partner);
            r.say(close ? r.pick("Hey " + op.name + "!", op.name + "! Looking good!", "Oh hi " + op.name + ", can't stop!", "Morning, " + op.name + "!") : r.pick("Hi " + op.name + ".", "Hello!", "*nods at " + op.name + "*"), 40);
            if (close && rnd.nextFloat() < 0.6f) o.say(o.pick("Hi " + p.name + "!", "Hey you!", "See you later, " + p.name + "!"), 40);
            return true;
        }
        return false;
    }

    /* ------------------------------------------------------------ Flying players */

    static boolean flyers(ServerLevel sl, Resident r, long now, RandomSource rnd) {
        for (ServerPlayer pl : sl.players()) {
            if (!pl.isFallFlying() || pl.distanceToSqr(r) > 24 * 24 || pl.getY() < r.getY() + 3) continue;
            if (!ready("fly:" + r.profileId(), now, 1200) || rnd.nextFloat() > 0.5f) return false;
            r.getLookControl().setLookAt(pl, 40, 40);
            r.gesture(rnd.nextBoolean() ? Resident.G_POINT : Resident.G_SURPRISED, 40);
            String pn = pl.getName().getString();
            r.say(r.pick("Is it a bird? Is it a ferry? No, it's " + pn + "!", "Whoa, " + pn + " can fly?!", "Show-off!", "Take me with you, " + pn + "!", "Careful up there!"), 50);
            return true;
        }
        return false;
    }

    /* ------------------------------------------------------------ Rain dancing */

    static boolean rainDance(Resident r, CityData.Profile p, long now, RandomSource rnd) {
        if (p.trait != Trait.CHEERFUL && p.trait != Trait.ADVENTUROUS && p.trait != Trait.DREAMY) return false;
        if (!r.idleHere() || rnd.nextFloat() > 0.1f || !ready("rain:" + p.id, now, 3000)) return false;
        r.gesture(Resident.G_DANCE, 100);
        r.say(r.pick("Dancing in the rain!", "Who needs an umbrella? Woo!", "Singin' in the rain~", "It's just water! Come on!"), 60);
        p.fun = Math.min(100, p.fun + 4);
        return true;
    }

    /* ------------------------------------------------------------ Snowball fights */

    static boolean snowball(ServerLevel sl, Resident r, CityData.Profile p, long now, RandomSource rnd) {
        if (rnd.nextFloat() > 0.12f || p.job == Job.POLICE || p.job == Job.FIREFIGHTER) return false;
        String act = r.activityName();
        if (!act.equals("leisure") && !act.equals("lunch")) return false;
        for (Resident o : sl.getEntitiesOfClass(Resident.class, r.getBoundingBox().inflate(9, 2, 9), x -> x != r && x.profile() != null && x.isFree() && x.distanceToSqr(r) > 9)) {
            if (!outside(o) || !r.canSee(o, 9)) continue;
            if (!ready("snow:" + p.id, now, 200)) return false;
            throwAt(sl, r, o);
            r.say(r.pick("Snowball fight!", "Heads up, " + o.profile().name + "!", "Gotcha!", "Incoming!"), 40);
            if (rnd.nextFloat() < 0.7f && ready("snow:" + o.profileId(), now, 60)) {
                throwAt(sl, o, r);
                o.say(o.pick("Oh, it's ON!", "Hey! Take that!", "You'll pay for that!"), 40);
            }
            p.fun = Math.min(100, p.fun + 3);
            return true;
        }
        return false;
    }

    static void throwAt(ServerLevel sl, Resident from, Resident to) {
        from.getLookControl().setLookAt(to, 40, 40);
        from.gesture(Resident.G_THROW, 16);
        from.swing(InteractionHand.MAIN_HAND);
        Snowball sb = new Snowball(sl, from);
        sb.setItem(new ItemStack(Items.SNOWBALL));
        Vec3 dv = to.getEyePosition().subtract(from.getEyePosition());
        sb.shoot(dv.x, dv.y + dv.horizontalDistance() * 0.15, dv.z, 1.1f, 3f);
        sl.addFreshEntity(sb);
        sl.playSound(null, from.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5f, 0.8f);
    }

    /* ------------------------------------------------------------ Picnics */

    static final String[] PICNIC_SPOTS = {"park", "plaza", "gardens", "beach", "pier", "isle_plaza"};
    static final String[] PICNIC_FOOD = {"minecraft:bread", "minecraft:apple", "minecraft:cookie", "minecraft:sweet_berries", "minecraft:pumpkin_pie"};

    static boolean picnic(ServerLevel sl, CityData d, Resident r, CityData.Profile p, long day, RandomSource rnd) {
        if (!r.idleHere() || rnd.nextFloat() > 0.05f) return false;
        Place here = r.destination();
        boolean spot = false;
        if (here != null) for (String k : PICNIC_SPOTS) if (k.equals(here.key)) spot = true;
        if (!spot) return false;
        for (Resident o : sl.getEntitiesOfClass(Resident.class, r.getBoundingBox().inflate(4, 1, 4), x -> x != r && x.profile() != null && x.idleHere())) {
            CityData.Rel rel = d.peekRel(p.id, o.profileId());
            boolean partner = o.profileId().equals(p.partner);
            if (!partner && (rel == null || !rel.friend())) continue;
            if (!ready("picnic:" + Resident.pairKey(p.id, o.profileId()) + ":" + day, sl.getGameTime(), 24000)) return false;
            CityData.Profile op = o.profile();
            String fa = PICNIC_FOOD[rnd.nextInt(PICNIC_FOOD.length)], fb = PICNIC_FOOD[rnd.nextInt(PICNIC_FOOD.length)];
            r.getLookControl().setLookAt(o, 30, 30);
            o.getLookControl().setLookAt(r, 30, 30);
            r.showItem(fa, 120);
            o.showItem(fb, 120);
            r.gesture(Resident.G_EAT, 100);
            o.gesture(Resident.G_EAT, 100);
            r.say(r.pick("Picnic time! I brought " + Economy.label(fa) + ".", op.name + ", want to share? Picnic!", "Perfect day for a picnic."), 70);
            o.say(o.pick("Ooh, I've got " + Economy.label(fb) + "!", "Best idea you've had all week.", "Don't let the seagulls see!"), 70);
            p.hunger = Math.min(100, p.hunger + 12);
            op.hunger = Math.min(100, op.hunger + 12);
            p.fun = Math.min(100, p.fun + 6);
            op.fun = Math.min(100, op.fun + 6);
            d.rel(p.id, op.id).aff = Math.min(100, d.rel(p.id, op.id).aff + 2);
            d.rel(op.id, p.id).aff = Math.min(100, d.rel(op.id, p.id).aff + 2);
            if (partner) {
                r.particles(ParticleTypes.HEART, 3);
                o.particles(ParticleTypes.HEART, 3);
            }
            p.log(r.routineDay()).note("I had a picnic with " + op.name + " at " + here.label);
            op.log(o.routineDay()).note("I had a picnic with " + p.name + " at " + here.label);
            if (rnd.nextFloat() < 0.4f) d.news(day, p.name + " and " + op.name + " had a picnic at " + here.label + ".");
            d.setDirty();
            return true;
        }
        return false;
    }

    /* ------------------------------------------------------------ Selfies */

    static boolean selfie(ServerLevel sl, CityData d, Resident r, CityData.Profile p, long day, RandomSource rnd) {
        if (!p.ownsPhone || !r.idleHere() || rnd.nextFloat() > 0.01f) return false;
        Place here = r.destination();
        if (here == null || here.key.equals(p.home) || here.key.startsWith("apt") || !r.blockPosition().closerThan(here.pos, 8)) return false;
        if (!ready("selfie:" + p.id + ":" + day, sl.getGameTime(), 24000)) return false;
        r.showItem("fireheartcity:phone", 50);
        r.getNavigation().stop();
        r.gesture(Resident.G_PHOTO, 45);
        r.say(r.pick("Say cheese!", "*snap*", "One for SolFeed!", "Hold on, the lighting is perfect..."), 40);
        sl.sendParticles(ParticleTypes.FLASH, r.getX(), r.getEyeY(), r.getZ(), 1, 0, 0, 0, 0);
        sl.playSound(null, r.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.NEUTRAL, 0.4f, 1.6f);
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        String light = tod > 11500 && tod < 13000 ? "Sunset" : tod < 1500 || tod > 22500 ? "Sunrise" : sl.isRaining() ? "Rainy day" : "Lovely day";
        String[] caps = {light + " at " + here.label + " 📸", "Me at " + here.label + ". Not bad, right?", "Found my new favourite spot: " + here.label, light + " vibes ☺"};
        Phones.post(d, p.id, caps[rnd.nextInt(caps.length)], day, Phones.tod(sl));
        p.log(r.routineDay()).note("I took a photo at " + here.label + " and posted it on SolFeed");
        return true;
    }

    /* ------------------------------------------------------------ Moods */

    static void mood(ServerLevel sl, Resident r, CityData.Profile p, long now, RandomSource rnd) {
        if (rnd.nextFloat() > 0.02f || !r.idleHere()) return;
        int m = p.mood();
        if (m >= 85 && ready("mood:" + p.id, now, 2400)) {
            sl.sendParticles(ParticleTypes.NOTE, r.getX(), r.getY() + 2.2, r.getZ(), 2, 0.3, 0.1, 0.3, 1.0);
            r.say(r.pick("♪ la la la ♪", "*hums happily*", "What a day!", "Life is good."), 40);
        } else if (m <= 25 && ready("mood:" + p.id, now, 2400)) {
            r.gesture(Resident.G_SIGH, 40);
            r.say(r.pick("*sigh*", "Could today just be over?", "...", "I need a hug."), 50);
        }
    }

    /* ------------------------------------------------------------ Yoga */

    public static void yogaTick(Resident r, CityData.Profile p, Place dest) {
        if (!r.isFree() || r.isSeated()) return;
        r.getNavigation().stop();
        long phase = (r.level().getGameTime() / 100) % 4;
        r.setYRot(Math.floorMod(dest.key.hashCode(), 4) * 90f);
        r.yBodyRot = r.getYRot();
        int g = phase == 0 ? Resident.G_STRETCH : phase == 1 ? Resident.G_YOGA_TREE : phase == 2 ? Resident.G_YOGA_WARRIOR : Resident.G_BOW;
        r.gesture(g, 45);
        p.fun = Math.min(100, p.fun + 1);
        p.social = Math.min(100, p.social + 1);
        if (r.getRandom().nextFloat() < 0.05f) r.say(r.pick("Breathe in... and out.", "Downward creeper!", "Namaste, Solaris.", "My back just made a noise.", "Find your centre..."), 50);
        DayLog lg = p.log(r.routineDay());
        if (lg.once("yoga")) lg.note("I did morning yoga at " + dest.label);
    }
}
