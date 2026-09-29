package com.fireheart.city;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;

/** Stargazing Night at the Neon Heights Observatory: Wednesdays and Fridays when the sky is clear, hosted by Nova. */
public final class Stars {
    private Stars() {}

    public static final BlockPos OBS = new BlockPos(46, 181, 314);
    public static final long START = 12400, END = 13900;
    private static long plannedDay = -1;
    private static long guests = -1;
    private static final Set<String> TONIGHT = new HashSet<>();
    public static boolean forced;

    public static boolean nightDay(long day) {
        int w = Calendar.weekday(day);
        return w == 2 || w == 4 || forced;
    }

    public static boolean tonight(String id, long day) {
        return guests == day && TONIGHT.contains(id);
    }

    public static void plan(ServerLevel sl, CityData d) {
        long day = Calendar.worldDay(sl);
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (plannedDay == day || tod < 8000 || tod > 12000) return;
        plannedDay = day;
        if (!nightDay(day) || sl.isRaining()) return;
        TONIGHT.clear();
        List<String> who = new ArrayList<>();
        for (CityData.Profile p : d.profiles.values()) {
            boolean busy = false;
            for (CityData.Plan pl : d.plansFor(p.id, day)) if (pl.what.equals("party") || pl.what.equals("campaign") || pl.what.equals("speech")) busy = true;
            if (busy) continue;
            int chance = switch (p.trait) {
                case CURIOUS, DREAMY -> 70;
                case ADVENTUROUS -> 55;
                case GRUMPY -> 15;
                default -> 35;
            };
            chance += Math.max(0, p.mind.places.getOrDefault("observatory", 0)) / 2;
            if (p.job == Job.GUIDE || Math.floorMod(p.id.hashCode() * 13 + day * 31, 100) < chance) who.add(p.id);
        }
        if (who.size() < 2) return;
        d.plans.removeIf(pl -> pl.day == day && pl.what.equals("stargaze"));
        d.addPlan(day, "observatory", "stargaze", who.toArray(new String[0]));
        TONIGHT.addAll(who);
        guests = day;
        d.news(day, "Stargazing night at the Neon Heights Observatory tonight - " + who.size() + " residents are going.");
        forced = false;
    }

    public static void restore(CityData d, long day) {
        if (guests == day) return;
        for (CityData.Plan pl : d.plans) if (pl.day == day && pl.what.equals("stargaze")) { TONIGHT.addAll(pl.who); guests = day; plannedDay = day; }
    }

    public static void tick(ServerLevel sl, CityData d) {
        long day = Calendar.worldDay(sl);
        restore(d, day);
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (guests != day || tod < START || tod > END) return;
        if (!sl.isPositionEntityTicking(OBS)) return;
        if (sl.random.nextFloat() < 0.35f) shootingStar(sl);
        List<Resident> there = sl.getEntitiesOfClass(Resident.class, new AABB(OBS).inflate(12), r -> r.profile() != null);
        if (there.isEmpty()) return;
        Resident host = null;
        for (Resident r : there) if (r.profile().job == Job.GUIDE) host = r;
        if (sl.getGameTime() % 300 < 100 && host != null && host.isFree()) {
            host.sayTo(host.pick("See that bright one? That's the North Star - sailors used it to find their way home.",
                    "That cluster up there is the Seven Sisters. On a clear night you can count all of them.",
                    "The band of light across the sky is our galaxy, seen edge-on. Beautiful, right?",
                    "Make a wish if you see a shooting star - tonight's a good night for them!",
                    "That reddish dot? A planet, not a star. It doesn't twinkle, see?",
                    "The Neon Heights beacon is the only thing up here brighter than the Moon."), 140);
        } else {
            Resident r = there.get(sl.random.nextInt(there.size()));
            if (r != host && r.isFree() && sl.random.nextFloat() < 0.4f) r.say(r.pick("Ooooh...", "I've never seen so many stars.", "Did you see that one?!", "I made a wish!", "It's so peaceful up here.", "Look, a shooting star!"), 70);
        }
        for (Resident r : there) {
            CityData.Profile p = r.profile();
            DayLog lg = p.log(r.routineDay());
            if (lg.once("stars")) lg.note("I went stargazing at the Neon Heights Observatory - it was beautiful");
            p.fun = Math.min(100, p.fun + 1);
        }
        for (ServerPlayer pl : sl.getEntitiesOfClass(ServerPlayer.class, new AABB(OBS).inflate(40))) {
            if (sl.getGameTime() % 600 < 100) pl.displayClientMessage(Component.literal("§b✦ §fStargazing Night at the Observatory §b✦"), true);
        }
    }

    private static void shootingStar(ServerLevel sl) {
        double x = OBS.getX() + sl.random.nextInt(120) - 60, y = 240 + sl.random.nextInt(30), z = OBS.getZ() + sl.random.nextInt(120) - 60;
        double dx = (sl.random.nextDouble() - 0.5) * 2, dz = (sl.random.nextDouble() - 0.5) * 2;
        for (int i = 0; i < 18; i++) {
            for (ServerPlayer pl : sl.players()) sl.sendParticles(pl, ParticleTypes.END_ROD, true, x + dx * i, y - i * 0.35, z + dz * i, 1, 0, 0, 0, 0);
        }
        sl.playSound(null, OBS, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 0.3f, 1.8f);
    }

    public static boolean nightNow(ServerLevel sl) {
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        return guests == Calendar.worldDay(sl) && tod >= START && tod <= END;
    }

    static int count() {
        return TONIGHT.size();
    }

    public static int forceTonight(ServerLevel sl, CityData d) {
        forced = true;
        plannedDay = -1;
        long day = Calendar.worldDay(sl);
        d.plans.removeIf(pl -> pl.day == day && pl.what.equals("stargaze"));
        guests = -1;
        plannedDayOverride(sl, d);
        return TONIGHT.size();
    }

    private static void plannedDayOverride(ServerLevel sl, CityData d) {
        long day = Calendar.worldDay(sl);
        plannedDay = day - 1;
        TONIGHT.clear();
        List<String> who = new ArrayList<>();
        for (CityData.Profile p : d.profiles.values()) {
            int chance = p.trait == Trait.CURIOUS || p.trait == Trait.DREAMY ? 75 : p.trait == Trait.GRUMPY ? 20 : 45;
            if (p.job == Job.GUIDE || Math.floorMod(p.id.hashCode() * 13 + day * 31, 100) < chance) who.add(p.id);
        }
        d.addPlan(day, "observatory", "stargaze", who.toArray(new String[0]));
        TONIGHT.addAll(who);
        guests = day;
        plannedDay = day;
        forced = false;
    }
}
