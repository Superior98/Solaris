package com.fireheart.city;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Resident pets: each lives at its owner's home, greets them and curls up when they sleep. */
public final class Pets {
    private static final Map<String, String[]> PETS = new HashMap<>();
    private static final Map<String, Integer> missing = new HashMap<>();

    static {
        PETS.put("mia", new String[]{"cat", "Biscuit"});
        PETS.put("ivy", new String[]{"cat", "Clover"});
        PETS.put("zara", new String[]{"cat", "Tick"});
        PETS.put("omar", new String[]{"cat", "Pumpkin"});
        PETS.put("sam", new String[]{"parrot", "Captain"});
        PETS.put("luna", new String[]{"cat", "Neko"});
    }

    private Pets() {}

    public static String petName(String residentId) {
        String[] s = PETS.get(residentId);
        return s == null ? null : s[1];
    }

    public static void tick(ServerLevel sl, CityData d) {
        for (CityData.Profile p : d.profiles.values()) {
            String[] spec = PETS.get(p.id);
            if (spec == null) continue;
            Place home = p.homePlace();
            if (home == null || !sl.isLoaded(home.pos) || !sl.isPositionEntityTicking(home.pos)) continue;
            Entity pet = p.pet == null ? null : sl.getEntity(p.pet);
            if (pet == null || !pet.isAlive()) {
                List<Animal> found = sl.getEntitiesOfClass(Animal.class, new AABB(home.pos).inflate(48), a -> a.isAlive() && a.getTags().contains("fhc_pet_" + p.id));
                if (!found.isEmpty()) {
                    pet = found.get(0);
                    p.pet = pet.getUUID();
                    d.setDirty();
                }
            }
            if (pet == null || !pet.isAlive()) {
                int m = missing.merge(p.id, 1, Integer::sum);
                if (m >= 3) {
                    missing.put(p.id, 0);
                    spawn(sl, d, p, spec, home.pos, p.pet == null);
                }
                continue;
            }
            missing.put(p.id, 0);
            if (pet instanceof Mob mob) behave(sl, d, p, spec, home.pos, mob);
        }
    }

    private static void spawn(ServerLevel sl, CityData d, CityData.Profile p, String[] spec, BlockPos home, boolean firstTime) {
        EntityType<? extends TamableAnimal> type = spec[0].equals("parrot") ? EntityType.PARROT : EntityType.CAT;
        TamableAnimal a = type.create(sl);
        if (a == null) return;
        a.moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, sl.random.nextFloat() * 360f, 0f);
        a.finalizeSpawn(sl, sl.getCurrentDifficultyAt(home), MobSpawnType.MOB_SUMMONED, null, null);
        a.setTame(true);
        a.setCustomName(Component.literal(spec[1]));
        a.setPersistenceRequired();
        a.addTag("fhc_pet");
        a.addTag("fhc_pet_" + p.id);
        sl.addFreshEntity(a);
        p.pet = a.getUUID();
        if (firstTime) d.event(Calendar.dayOf(sl.getDayTime()), "pets", p.name + " adopted a " + spec[0] + " named " + spec[1], home, p.id);
        d.setDirty();
    }

    private static void behave(ServerLevel sl, CityData d, CityData.Profile p, String[] spec, BlockPos home, Mob pet) {
        Vec3 h = Vec3.atBottomCenterOf(home);
        Entity oe = p.entity == null ? null : sl.getEntity(p.entity);
        Resident owner = oe instanceof Resident r ? r : null;
        boolean ownerHome = owner != null && owner.distanceToSqr(h) < 10 * 10 && Math.abs(owner.getY() - home.getY()) < 3;
        TamableAnimal t = pet instanceof TamableAnimal ta ? ta : null;
        if (ownerHome && owner.isSleeping()) {
            BlockPos bed = owner.getSleepingPos().orElse(owner.blockPosition());
            if (pet.distanceToSqr(Vec3.atBottomCenterOf(bed)) > 2.5) {
                if (t != null) t.setInSittingPose(false);
                pet.getNavigation().moveTo(bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5, 1.0);
            } else if (t != null) t.setInSittingPose(true);
            return;
        }
        if (t != null) t.setInSittingPose(false);
        if (ownerHome) {
            if (pet.distanceToSqr(owner) > 6) pet.getNavigation().moveTo(owner, 1.0);
            else if (sl.random.nextFloat() < 0.15f) {
                sl.sendParticles(ParticleTypes.HEART, pet.getX(), pet.getY() + 0.7, pet.getZ(), 1, 0.2, 0.2, 0.2, 0.0);
                if (owner.isFree() && sl.random.nextFloat() < 0.35f) {
                    String n = spec[1], k = spec[0];
                    owner.say(owner.pick("Hey " + n + "! Who's a good " + k + "?", n + ", did you miss me?", "*pets " + n + "*", "Look at you, " + n + "!"), 50);
                    owner.getLookControl().setLookAt(pet, 30f, 30f);
                    p.fun = Math.min(100, p.fun + 2);
                }
            }
            return;
        }
        double dh = pet.distanceToSqr(h);
        if (dh > 30 * 30 || Math.abs(pet.getY() - home.getY()) > 2.5) pet.teleportTo(h.x, h.y, h.z);
        else if (dh > 8 * 8) pet.getNavigation().moveTo(h.x, h.y, h.z, 1.0);
    }

    private static final Map<String, Long> NOTICED = new HashMap<>();

    /** Tame animals owned by a player, and name-tagged animals right next to them. */
    public static List<Animal> playerPets(net.minecraft.world.entity.player.Player pl, double range) {
        return pl.level().getEntitiesOfClass(Animal.class, pl.getBoundingBox().inflate(range), a -> a.isAlive() && !a.getTags().contains("fhc_pet") && owned(a, pl));
    }

    static boolean owned(Animal a, net.minecraft.world.entity.player.Player pl) {
        if (a instanceof TamableAnimal t && t.isTame()) return pl.getUUID().equals(t.getOwnerUUID());
        if (a instanceof net.minecraft.world.entity.animal.horse.AbstractHorse h && h.isTamed()) return pl.getUUID().equals(h.getOwnerUUID());
        return a.hasCustomName() && a.distanceTo(pl) < 5;
    }

    public static String kind(Animal a) {
        String k = a.getType().getDescription().getString().toLowerCase(java.util.Locale.ROOT);
        if (a instanceof net.minecraft.world.entity.animal.Wolf) k = "dog";
        return k;
    }

    public static String label(Animal a) {
        return a.hasCustomName() ? a.getCustomName().getString() : "your " + kind(a);
    }

    static boolean male(Animal a) {
        return Math.floorMod(a.getUUID().hashCode(), 2) == 0;
    }

    /** A resident who sees a player's pet nearby fusses over it (once a day per pet). */
    public static void notice(Resident r) {
        if (!(r.level() instanceof ServerLevel sl) || !r.isFree() || r.speaking() || r.convo != null) return;
        CityData.Profile p = r.profile();
        if (p == null) return;
        for (net.minecraft.server.level.ServerPlayer pl : sl.players()) {
            if (pl.distanceToSqr(r) > 14 * 14) continue;
            for (Animal a : playerPets(pl, 10)) {
                if (a.distanceToSqr(r) > 7 * 7 || !r.hasLineOfSight(a)) continue;
                String key = p.id + "|" + a.getUUID();
                long day = Calendar.worldDay(sl);
                Long seen = NOTICED.get(key);
                if (seen != null && seen == day) continue;
                NOTICED.put(key, day);
                String pn = pl.getName().getString();
                String n = a.hasCustomName() ? a.getCustomName().getString() : null;
                String k = kind(a);
                String he = male(a) ? "he" : "she";
                String line = n != null
                        ? r.pick("Aww, is that " + n + "? Hi " + n + "!", n + "! Come here, cutie!", pn + ", " + n + " is adorable. I'm stealing " + (male(a) ? "him" : "her") + ".", "Hello " + n + "! Who's a good " + k + "?", "Oh my gosh, " + n + " is so cute today!")
                        : r.pick("Aww, what a cute " + k + "!", "Is that your " + k + ", " + pn + "? " + cap(he) + "'s adorable!", "Look at that " + k + "! So fluffy.", "Hi little " + k + "! You should give " + (male(a) ? "him" : "her") + " a name, " + pn + ".");
                if (a instanceof net.minecraft.world.entity.animal.Parrot) line = n != null ? r.pick(n + "! Can you say hello? Hello!", "Pretty bird, " + n + "! Pretty bird!", "Your parrot " + n + " is so colourful, " + pn + "!") : r.pick("A parrot! Can it talk? Hello! Hellooo!", "What a pretty bird!");
                r.getLookControl().setLookAt(a, 30, 30);
                r.gesture(a.distanceTo(r) < 2.5 ? Resident.G_PETTING : Resident.G_POINT, 50);
                if (Resident.mayAddress(sl, pn)) {
                    r.sayTo(line, 90);
                    Resident.addressed(sl, pn);
                } else r.say(line, 70);
                sl.sendParticles(ParticleTypes.HEART, a.getX(), a.getY() + a.getBbHeight() + 0.3, a.getZ(), 2, 0.2, 0.1, 0.2, 0);
                p.fun = Math.min(100, p.fun + 2);
                Mind.playerEvent(r.data(), p, pn, day, "I met {P}'s " + k + (n != null ? " " + n : "") + " - so cute", 2, 3);
                return;
            }
        }
    }

    static String cap(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + s.substring(1);
    }

    /** Replies when the player talks about their pet. */
    public static String playerPetLine(net.minecraft.server.level.ServerPlayer pl, Resident r, String t) {
        List<Animal> pets = playerPets(pl, 12);
        Animal mine = null;
        for (Animal a : pets) {
            String n = a.hasCustomName() ? a.getCustomName().getString().toLowerCase(java.util.Locale.ROOT) : "";
            if (!n.isEmpty() && t.contains(" " + n + " ")) mine = a;
        }
        boolean talk = mine != null || t.matches(".* (my|our) (pet|dog|cat|parrot|bird|horse|wolf|puppy|kitty|kitten|fox|pup|doggo|pupper|frog|axolotl) .*") || t.contains(" meet my ");
        if (!talk) return null;
        if (mine == null && !pets.isEmpty()) mine = pets.get(0);
        if (mine == null) {
            if (!t.contains(" meet my ") && !t.matches(".* (i have|i got|i own|look at|this is) (a |my )?.*")) return null;
            return r.pick("You have a pet? Bring them next time, I want to meet them!", "Aww! Where are they? I want to see!");
        }
        if (t.matches(".* (sick|hurt|ill|lost|missing|died|dead|injured) .*")) return r.pick("Oh no, poor " + (mine.hasCustomName() ? mine.getCustomName().getString() : "thing") + "! I hope they feel better soon.", "Aww... give them lots of cuddles from me.");
        String n = label(mine);
        String k = kind(mine);
        r.getLookControl().setLookAt(mine, 30, 30);
        r.gesture(mine.distanceTo(r) < 2.5 ? Resident.G_PETTING : Resident.G_CLAP, 50);
        ((ServerLevel) r.level()).sendParticles(ParticleTypes.HEART, mine.getX(), mine.getY() + mine.getBbHeight() + 0.3, mine.getZ(), 3, 0.2, 0.1, 0.2, 0);
        String on = mine.hasCustomName() ? n : "your " + k;
        return r.pick(cap(on) + " is honestly the cutest " + k + " in Solaris.", "I love " + on + "! Can I give " + (male(mine) ? "him" : "her") + " a treat?", "Hi " + (mine.hasCustomName() ? n : "buddy") + "! *waves at " + on + "*", cap(on) + " looks so happy with you, " + pl.getName().getString() + ".");
    }
}
