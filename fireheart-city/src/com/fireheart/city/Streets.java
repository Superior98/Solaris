package com.fireheart.city;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Street life: comforting friends, hungry residents, police salutes, bench naps, whistling, ride reactions, jukebox dancing, storm-shy residents. */
public final class Streets {
    private Streets() {}

    static boolean ready(String key, long now, long gap) {
        return Hobbies.ready(key, now, gap);
    }

    static final List<BlockPos> JUKEBOXES = new ArrayList<>();
    static boolean thunderWas;

    public static void reset() {
        JUKEBOXES.clear();
        thunderWas = false;
    }

    public static void tick(ServerLevel sl, CityData d) {
        long now = sl.getGameTime();
        if (now % 100 == 29) scanJukeboxes(sl);
        if (now % 20 == 9) storms(sl, d);
        if (now % 20 != 13 || !FhcConfig.ambient()) return;
        long day = Calendar.worldDay(sl);
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        RandomSource rnd = sl.getRandom();
        for (Resident r : Crowd.nearPlayers(sl, d)) {
            CityData.Profile p = r.profile();
            if (p == null || r.isSleeping() || r.inShuttle()) continue;
            if (salute(sl, d, r, p, now, day)) continue;
            if (!r.isFree() || r.isSpeaking()) continue;
            if (jukebox(sl, r, p, now, rnd)) continue;
            if (rides(sl, r, now, rnd)) continue;
            if (comfort(sl, d, r, p, now, day, rnd)) continue;
            if (hungry(sl, d, r, p, now, day, rnd)) continue;
            if (nap(r, p, now, tod, rnd)) continue;
            whistle(sl, r, p, now, rnd);
        }
    }

    /* ------------------------------------------------------------ Comforting sad friends */

    static boolean comfort(ServerLevel sl, CityData d, Resident r, CityData.Profile p, long now, long day, RandomSource rnd) {
        if (p.mood() > 30 || !r.idleHere() || rnd.nextFloat() > 0.3f) return false;
        for (Resident o : sl.getEntitiesOfClass(Resident.class, r.getBoundingBox().inflate(5, 2, 5), x -> x != r && x.profile() != null && x.isFree() && !x.isSpeaking())) {
            CityData.Rel rel = d.peekRel(o.profileId(), p.id);
            boolean close = o.profileId().equals(p.partner) || rel != null && rel.friend();
            if (!close || !r.canSee(o, 5) || !ready("comfort:" + Resident.pairKey(p.id, o.profileId()) + ":" + day, now, 24000)) continue;
            CityData.Profile op = o.profile();
            o.getLookControl().setLookAt(r, 30, 30);
            r.getLookControl().setLookAt(o, 30, 30);
            o.gesture(Resident.G_HUG, 60);
            r.gesture(Resident.G_HUG, 60);
            o.sayTo(o.pick("Hey... you okay, " + p.name + "? Come here.", "You look like you need a hug.", "Whatever it is, we'll sort it out together."), 90);
            r.sayTo(r.pick("Thanks, " + op.name + ". I needed that.", "*sniff* You're a good friend.", "I'm okay. Better now."), 80);
            r.particles(ParticleTypes.HEART, 3);
            p.social = Math.min(100, p.social + 12);
            p.fun = Math.min(100, p.fun + 8);
            d.rel(p.id, op.id).aff = Math.min(100, d.rel(p.id, op.id).aff + 3);
            p.log(r.routineDay()).note(op.name + " cheered me up when I was feeling low");
            return true;
        }
        return false;
    }

    /* ------------------------------------------------------------ Hungry residents ask for food */

    static boolean hungry(ServerLevel sl, CityData d, Resident r, CityData.Profile p, long now, long day, RandomSource rnd) {
        if (p.hunger > 25 || Economy.foodCount(p) > 0 || rnd.nextFloat() > 0.4f) return false;
        for (ServerPlayer pl : sl.players()) {
            ItemStack held = pl.getMainHandItem();
            if (!held.isEdible() || pl.distanceTo(r) > 5 || !r.hasLineOfSight(pl)) continue;
            String pn = pl.getName().getString();
            if (!ready("beg:" + p.id + ":" + pn + ":" + day, now, 24000)) continue;
            r.getLookControl().setLookAt(pl, 30, 30);
            r.gesture(Resident.G_POINT, 40);
            r.sayTo(r.pick("Is that " + held.getHoverName().getString().toLowerCase(java.util.Locale.ROOT) + "? I'm starving... could I have a bite?", "Sorry to ask, " + pn + ", but I haven't eaten all day...", "That smells amazing. I'm so hungry."), 90);
            return true;
        }
        return false;
    }

    /* ------------------------------------------------------------ Police salutes */

    static boolean salute(ServerLevel sl, CityData d, Resident r, CityData.Profile p, long now, long day) {
        if (p.job != Job.POLICE || r.emergencyTarget() != null || r.convo != null) return false;
        for (ServerPlayer pl : sl.players()) {
            if (pl.distanceTo(r) > 8 || !r.hasLineOfSight(pl)) continue;
            String pn = pl.getName().getString();
            if (Quests.repScore(d, pn) < 30 || !ready("salute:" + p.id + ":" + pn + ":" + day, now, 24000)) continue;
            r.getLookControl().setLookAt(pl, 30, 30);
            r.gesture(Resident.G_SALUTE, 50);
            r.sayTo(r.pick("Officer " + p.name + ", at your service!", "Evening, " + pn + ". Streets are safe tonight.", "Solaris PD salutes you, " + pn + "!"), 70);
            return true;
        }
        return false;
    }

    /* ------------------------------------------------------------ Bench naps */

    static boolean nap(Resident r, CityData.Profile p, long now, long tod, RandomSource rnd) {
        if (!r.isSeated() || r.onPhone() || rnd.nextFloat() > 0.05f) return false;
        boolean sleepy = tod > 11800 || r.activityName().equals("evening") || p.trait == Trait.LAIDBACK || p.trait == Trait.DREAMY;
        if (!sleepy || !ready("nap:" + p.id, now, 3000)) return false;
        r.gesture(Resident.G_NAP, 240);
        r.say("Zzz...", 200);
        return true;
    }

    /* ------------------------------------------------------------ Whistling */

    static void whistle(ServerLevel sl, Resident r, CityData.Profile p, long now, RandomSource rnd) {
        if (r.getNavigation().isDone() || rnd.nextFloat() > 0.03f) return;
        if (p.trait != Trait.CHEERFUL && p.trait != Trait.LAIDBACK && p.trait != Trait.FRIENDLY) return;
        if (!ready("whistle:" + p.id, now, 2400)) return;
        r.gesture(Resident.G_WHISTLE, 90);
        float[] tune = {1.4f, 1.6f, 1.8f, 1.5f, 1.9f};
        for (int i = 0; i < 3; i++) sl.playSound(null, r.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.NEUTRAL, 0.3f, tune[rnd.nextInt(tune.length)]);
        sl.sendParticles(ParticleTypes.NOTE, r.getX(), r.getY() + 2.1, r.getZ(), 2, 0.2, 0.1, 0.2, 0.8);
    }

    /* ------------------------------------------------------------ Reactions to riding players */

    static boolean rides(ServerLevel sl, Resident r, long now, RandomSource rnd) {
        for (ServerPlayer pl : sl.players()) {
            Entity v = pl.getVehicle();
            if (v == null || pl.distanceTo(r) > 10 || !r.hasLineOfSight(pl)) continue;
            double moved = (v.getX() - v.xo) * (v.getX() - v.xo) + (v.getZ() - v.zo) * (v.getZ() - v.zo);
            if (moved < 0.004 || !ready("ride:" + r.profileId(), now, 1800) || rnd.nextFloat() > 0.5f) return false;
            String pn = pl.getName().getString();
            String line;
            if (v instanceof AbstractHorse) line = r.pick("What a beautiful horse!", "Giddy up, " + pn + "!", "Can I pet your horse?");
            else if (v instanceof Boat) line = r.pick("Ahoy, " + pn + "!", "Nice boat!", "Don't capsize!");
            else if (v instanceof AbstractMinecart) line = r.pick("Choo choo!", "Riding the rails, " + pn + "?", "Mind the tracks!");
            else if (v instanceof Vehicle) line = r.pick("Nice wheels, " + pn + "!", "Slow down in the city!", "Ooh, can I get a ride?", "Beep beep!");
            else line = r.pick("Whee!", "Where are you off to, " + pn + "?");
            r.getLookControl().setLookAt(pl, 30, 30);
            r.gesture(Resident.G_WAVE, 30);
            r.say(line, 50);
            return true;
        }
        return false;
    }

    /* ------------------------------------------------------------ Jukebox dancing */

    static void scanJukeboxes(ServerLevel sl) {
        JUKEBOXES.clear();
        for (ServerPlayer pl : sl.players()) {
            BlockPos c = pl.blockPosition();
            for (BlockPos q : BlockPos.betweenClosed(c.offset(-10, -4, -10), c.offset(10, 4, 10))) {
                BlockState st = sl.getBlockState(q);
                if (st.getBlock() instanceof JukeboxBlock && st.getValue(JukeboxBlock.HAS_RECORD)) JUKEBOXES.add(q.immutable());
            }
        }
    }

    static boolean jukebox(ServerLevel sl, Resident r, CityData.Profile p, long now, RandomSource rnd) {
        if (JUKEBOXES.isEmpty()) return false;
        for (BlockPos j : JUKEBOXES) {
            if (!r.blockPosition().closerThan(j, 10)) continue;
            r.getLookControl().setLookAt(j.getX() + 0.5, j.getY() + 0.5, j.getZ() + 0.5);
            if (rnd.nextFloat() < 0.6f) r.gesture(p.trait == Trait.SHY ? Resident.G_NOD : Resident.G_DANCE, 60);
            sl.sendParticles(ParticleTypes.NOTE, r.getX(), r.getY() + 2.2, r.getZ(), 1, 0.3, 0.1, 0.3, rnd.nextDouble());
            p.fun = Math.min(100, p.fun + 1);
            if (ready("juke:" + p.id, now, 1800) && rnd.nextFloat() < 0.4f) r.say(r.pick("I love this song!", "Who put this on? Great choice!", "*dances*", "Turn it up!"), 50);
            return true;
        }
        return false;
    }

    /* ------------------------------------------------------------ Storm-shy residents */

    static void storms(ServerLevel sl, CityData d) {
        boolean t = sl.isThundering();
        if (t && !thunderWas) {
            long day = Calendar.worldDay(sl);
            for (Resident r : Crowd.all(sl, d)) {
                CityData.Profile p = r.profile();
                if (p == null || p.trait != Trait.SHY && p.trait != Trait.DREAMY) continue;
                p.mind.intentKind = "rest";
                p.mind.intentDay = day;
                p.mind.intent = "stay inside until the storm passes";
                r.replan();
                if (r.isFree() && sl.getRandom().nextFloat() < 0.5f) {
                    r.gesture(Resident.G_HUGSELF, 40);
                    r.say(r.pick("I hate thunder. I'm going home!", "Nope. Staying in until this storm's over.", "Eek! Home, now!"), 50);
                }
            }
        }
        thunderWas = t;
    }
}
