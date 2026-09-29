package com.fireheart.city;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.Animal;
import org.joml.Vector3f;

/** Small resident moments and ambient touches: card games, jokes, arguments, reading, coffee, animals, seasons, sunsets, fireflies and full moons. */
public final class Moments {
    private Moments() {}

    static boolean ready(String key, long now, long gap) {
        return Hobbies.ready(key, now, gap);
    }

    public static void tick(ServerLevel sl, CityData d) {
        long gt = sl.getGameTime();
        if (gt % 10 == 1) fireflies(sl);
        if (gt % 20 != 7) return;
        long day = Calendar.worldDay(sl);
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        int season = Skies.seasonIndex(day);
        RandomSource rnd = sl.getRandom();
        for (Resident r : Skies.residents(sl, d)) {
            CityData.Profile p = r.profile();
            if (p == null || r.isSleeping() || r.inShuttle() || r.isSpeaking() || !r.isFree()) continue;
            if (rnd.nextFloat() > 0.2f) continue;
            if (argue(sl, d, r, p, gt, rnd)) continue;
            if (cards(sl, d, r, p, gt, day, rnd)) continue;
            if (joke(sl, r, p, gt, rnd)) continue;
            if (reading(r, p, gt, rnd)) continue;
            if (coffee(r, p, gt, day, rnd)) continue;
            if (animals(sl, r, p, gt, rnd)) continue;
            if (seasonal(sl, r, p, gt, season, tod, rnd)) continue;
            if (sky(sl, r, p, gt, day, tod, rnd)) continue;
        }
    }

    /* ------------------------------------------------------------ Rival arguments */

    static boolean argue(ServerLevel sl, CityData d, Resident r, CityData.Profile p, long now, RandomSource rnd) {
        for (Resident o : sl.getEntitiesOfClass(Resident.class, r.getBoundingBox().inflate(5, 2, 5), x -> x != r && x.profile() != null && x.isFree() && !x.isSpeaking())) {
            CityData.Rel rel = d.peekRel(p.id, o.profileId());
            if (rel == null || !rel.rival || !r.canSee(o, 5)) continue;
            if (!ready("argue:" + Resident.pairKey(p.id, o.profileId()), now, 12000) || rnd.nextFloat() > 0.3f) return false;
            CityData.Profile op = o.profile();
            r.getLookControl().setLookAt(o, 30, 30);
            o.getLookControl().setLookAt(r, 30, 30);
            r.gesture(Resident.G_ANGRY, 60);
            o.gesture(rnd.nextBoolean() ? Resident.G_ANGRY : Resident.G_FACEPALM, 60);
            r.sayTo(r.pick("Oh great, it's " + op.name + ".", "You still owe me an apology, " + op.name + ".", "Don't even start, " + op.name + ".", "Nice of you to finally show up, " + op.name + "."), 70);
            o.sayTo(o.pick("Whatever, " + p.name + ".", "I'm not doing this today.", "Keep walking, " + p.name + ".", "Says YOU."), 70);
            r.particles(ParticleTypes.ANGRY_VILLAGER, 3);
            p.social = Math.max(0, p.social - 3);
            op.social = Math.max(0, op.social - 3);
            if (rnd.nextFloat() < 0.15f) {
                rel.rival = false;
                CityData.Rel back = d.peekRel(op.id, p.id);
                if (back != null) back.rival = false;
                r.say(r.pick("...Look, maybe we should just call a truce.", "Fine. Truce?"), 60);
                d.event(r.day(), "social", p.name + " and " + op.name + " finally called a truce", r.blockPosition(), p.id, op.id);
            }
            d.setDirty();
            return true;
        }
        return false;
    }

    /* ------------------------------------------------------------ Card games */

    static final String[] GAMES = {"cards", "chess", "dominoes", "checkers"};

    static boolean cards(ServerLevel sl, CityData d, Resident r, CityData.Profile p, long now, long day, RandomSource rnd) {
        if (!r.idleHere() || rnd.nextFloat() > 0.15f) return false;
        Place here = r.destination();
        if (here == null || !(here.key.equals("plaza") || here.key.equals("library") || here.key.equals("diner") || here.key.equals("park") || here.key.equals("isle_plaza") || here.key.equals("arcade") || here.key.equals(p.home))) return false;
        for (Resident o : sl.getEntitiesOfClass(Resident.class, r.getBoundingBox().inflate(3.5, 1, 3.5), x -> x != r && x.profile() != null && x.idleHere())) {
            CityData.Rel rel = d.peekRel(p.id, o.profileId());
            if (rel == null || !rel.met || rel.rival) continue;
            if (!ready("cards:" + Resident.pairKey(p.id, o.profileId()) + ":" + day, now, 24000)) return false;
            CityData.Profile op = o.profile();
            String game = GAMES[rnd.nextInt(GAMES.length)];
            r.getLookControl().setLookAt(o, 30, 30);
            o.getLookControl().setLookAt(r, 30, 30);
            r.showItem(game.equals("cards") ? "minecraft:paper" : "minecraft:stone_button", 120);
            r.gesture(Resident.G_THINK, 80);
            o.gesture(Resident.G_THINK, 80);
            r.say(r.pick("Fancy a game of " + game + ", " + op.name + "?", "Rematch at " + game + "? I've been practising.", game.substring(0, 1).toUpperCase() + game.substring(1) + "? You're on."), 60);
            boolean iWin = rnd.nextBoolean();
            Resident w = iWin ? r : o, l = iWin ? o : r;
            w.gesture(Resident.G_CHEER, 50);
            l.gesture(Resident.G_FACEPALM, 50);
            w.say(w.pick("Checkmate! Well, you know what I mean.", "And THAT is how it's done!", "I win! Again!"), 60);
            l.say(l.pick("Best of three!", "You cheated. Probably.", "I let you win."), 60);
            p.fun = Math.min(100, p.fun + 8);
            op.fun = Math.min(100, op.fun + 8);
            rel.fam = Math.min(100, rel.fam + 2);
            p.log(r.routineDay()).note("I played " + game + " with " + op.name + (iWin ? " and won" : " and lost"));
            op.log(o.routineDay()).note("I played " + game + " with " + p.name + (iWin ? " and lost" : " and won"));
            d.setDirty();
            return true;
        }
        return false;
    }

    /* ------------------------------------------------------------ Jokes between residents */

    static boolean joke(ServerLevel sl, Resident r, CityData.Profile p, long now, RandomSource rnd) {
        if (p.trait != Trait.CHEERFUL && p.trait != Trait.TALKATIVE && p.trait != Trait.FRIENDLY || !r.idleHere() || rnd.nextFloat() > 0.1f) return false;
        for (Resident o : sl.getEntitiesOfClass(Resident.class, r.getBoundingBox().inflate(4, 1, 4), x -> x != r && x.profile() != null && x.idleHere())) {
            if (!r.canSee(o, 4) || !ready("joke:" + p.id, now, 6000)) return false;
            r.getLookControl().setLookAt(o, 30, 30);
            r.sayTo(Lines.pick(rnd, Intents.JOKES), 110);
            r.gesture(Resident.G_LAUGH, 40);
            boolean grumpy = o.profile().trait == Trait.GRUMPY;
            o.gesture(grumpy ? Resident.G_FACEPALM : Resident.G_LAUGH, 60);
            o.particles(grumpy ? ParticleTypes.SMOKE : ParticleTypes.HAPPY_VILLAGER, 2);
            o.profile().fun = Math.min(100, o.profile().fun + (grumpy ? 0 : 3));
            return true;
        }
        return false;
    }

    /* ------------------------------------------------------------ Bench reading */

    static final String[] BOOKS = {"a mystery novel", "a cookbook", "a book about the Founder", "poetry", "a sci-fi paperback", "the Solaris Gazette", "a guide to Create machines"};

    static boolean reading(Resident r, CityData.Profile p, long now, RandomSource rnd) {
        if (!r.isSeated() || r.onPhone() || rnd.nextFloat() > 0.3f || !ready("read:" + p.id, now, 1200)) return false;
        boolean curious = p.trait == Trait.CURIOUS || p.trait == Trait.SHY || p.trait == Trait.DREAMY;
        if (!curious && rnd.nextFloat() > 0.3f) return false;
        r.showItem("minecraft:book", 400);
        r.gesture(Resident.G_THINK, 80);
        if (rnd.nextFloat() < 0.5f) r.say(r.pick("*turns page*", "Ooh, plot twist!", "Just one more chapter...", "Reading " + BOOKS[rnd.nextInt(BOOKS.length)] + ". It's good!"), 50);
        p.fun = Math.min(100, p.fun + 3);
        return true;
    }

    /* ------------------------------------------------------------ Morning coffee */

    static boolean coffee(Resident r, CityData.Profile p, long now, long day, RandomSource rnd) {
        if (!r.activityName().equals("morning") || r.isEating() || rnd.nextFloat() > 0.3f || !ready("coffee:" + p.id + ":" + day, now, 24000)) return false;
        r.showItem("minecraft:honey_bottle", 160);
        r.gesture(Resident.G_EAT, 40);
        r.say(r.pick("*sips coffee* Okay. NOW I'm awake.", "Coffee first, talking later.", "Mmm, morning coffee.", "Is it too early for a second cup?"), 60);
        p.fun = Math.min(100, p.fun + 2);
        return true;
    }

    /* ------------------------------------------------------------ Animals */

    static boolean animals(ServerLevel sl, Resident r, CityData.Profile p, long now, RandomSource rnd) {
        if (!r.idleHere() || rnd.nextFloat() > 0.2f) return false;
        for (Animal a : sl.getEntitiesOfClass(Animal.class, r.getBoundingBox().inflate(5, 2, 5), x -> x.isAlive())) {
            if (!r.canSee(a, 5) || !ready("animal:" + p.id, now, 4800)) return false;
            String name = a.hasCustomName() ? a.getCustomName().getString() : a.getType().getDescription().getString().toLowerCase(java.util.Locale.ROOT);
            r.getLookControl().setLookAt(a, 30, 30);
            r.showItem("minecraft:wheat_seeds", 60);
            r.gesture(Resident.G_PETTING, 50);
            sl.sendParticles(ParticleTypes.HEART, a.getX(), a.getY() + a.getBbHeight() + 0.3, a.getZ(), 2, 0.2, 0.1, 0.2, 0);
            r.say(r.pick("Aww, hello little " + name + "!", "Who's a good " + name + "? You are!", "Here, have a snack.", "I could watch " + (a.hasCustomName() ? name : "this " + name) + " all day."), 60);
            p.fun = Math.min(100, p.fun + 4);
            return true;
        }
        return false;
    }

    /* ------------------------------------------------------------ Seasons */

    static boolean seasonal(ServerLevel sl, Resident r, CityData.Profile p, long now, int season, long tod, RandomSource rnd) {
        boolean out = Hobbies.outside(r);
        if (season == 0 && out && rnd.nextFloat() < 0.04f && ready("sneeze:" + p.id, now, 3600)) {
            r.gesture(Resident.G_SURPRISED, 20);
            r.say(r.pick("Ah... ah... ACHOO!", "*sneezes* Stupid pollen!", "ACHOO! ...Excuse me."), 40);
            sl.sendParticles(ParticleTypes.POOF, r.getX(), r.getEyeY(), r.getZ(), 3, 0.1, 0.1, 0.1, 0.02);
            return true;
        }
        if (season == 1 && out && tod > 4000 && tod < 9000 && !sl.isRaining() && r.idleHere() && rnd.nextFloat() < 0.05f && ready("heat:" + p.id, now, 4800)) {
            r.gesture(Resident.G_FACEPALM, 40);
            r.say(r.pick("Phew, it's a scorcher today!", "Is it just me or is it boiling?", "I'd kill for an ice cream right now.", "Beach. Now. Who's coming?"), 60);
            return true;
        }
        if (season == 2 && out && r.idleHere() && rnd.nextFloat() < 0.04f && ready("autumn:" + p.id, now, 6000)) {
            r.showItem("minecraft:pumpkin_pie", 80);
            r.say(r.pick("Pumpkin pie season is the best season.", "I love the crunchy leaves this time of year.", "Cosy jumper weather!"), 60);
            return true;
        }
        return false;
    }

    /* ------------------------------------------------------------ Sunsets and full moons */

    static boolean sky(ServerLevel sl, Resident r, CityData.Profile p, long now, long day, long tod, RandomSource rnd) {
        if (sl.isRaining() || !Hobbies.outside(r) || !r.idleHere()) return false;
        if (tod > 11600 && tod < 12700 && rnd.nextFloat() < 0.15f && ready("sunset:" + p.id + ":" + day, now, 24000)) {
            r.getLookControl().setLookAt(r.getX() - 100, r.getEyeY() + 8, r.getZ());
            r.say(r.pick("Look at that sunset...", "The sky's on fire tonight. Gorgeous.", "Best view in Solaris, right here.", "Every sunset's different, you know?"), 70);
            r.gesture(Resident.G_POINT, 40);
            p.log(r.routineDay()).note("I watched the sunset");
            return true;
        }
        if (tod > 22800 && rnd.nextFloat() < 0.1f && ready("sunrise:" + p.id + ":" + day, now, 24000)) {
            r.getLookControl().setLookAt(r.getX() + 100, r.getEyeY() + 5, r.getZ());
            r.gesture(Resident.G_STRETCH, 40);
            r.say(r.pick("Good morning, sun!", "Sunrise. A brand new day.", "I love being up this early."), 60);
            return true;
        }
        if (sl.getMoonPhase() == 0 && tod > 13500 && tod < 22000 && rnd.nextFloat() < 0.08f && ready("moon:" + p.id + ":" + day, now, 24000)) {
            r.getLookControl().setLookAt(r.getX(), r.getEyeY() + 50, r.getZ() + 20);
            boolean silly = p.trait == Trait.CHEERFUL || p.trait == Trait.ADVENTUROUS;
            r.gesture(silly ? Resident.G_LAUGH : Resident.G_POINT, 40);
            r.say(silly ? r.pick("Awoooooo! ...What? It's a full moon!", "Full moon! Everyone act normal.") : r.pick("What a full moon tonight.", "The moon's so bright you could read by it."), 60);
            return true;
        }
        return false;
    }

    /* ------------------------------------------------------------ Fireflies */

    static final DustParticleOptions FIREFLY = new DustParticleOptions(new Vector3f(0.85f, 1f, 0.3f), 0.6f);

    static void fireflies(ServerLevel sl) {
        long day = Calendar.worldDay(sl);
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (Skies.seasonIndex(day) != 1 || tod < 13000 || tod > 20000 || sl.isRaining()) return;
        RandomSource rnd = sl.getRandom();
        for (ServerPlayer pl : sl.players()) {
            if (!Skies.outside(pl) || pl.getY() > 150) continue;
            for (int i = 0; i < 5; i++) {
                double x = pl.getX() + rnd.nextGaussian() * 9, z = pl.getZ() + rnd.nextGaussian() * 9;
                double y = pl.getY() + 0.5 + rnd.nextDouble() * 2.5;
                sl.sendParticles(pl, FIREFLY, false, x, y, z, 1, 0.1, 0.1, 0.1, 0);
            }
        }
    }
}
