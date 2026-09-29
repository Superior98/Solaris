package com.fireheart.city;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

/** Nova's Sky Tour: weekend mornings Nova leads a group of residents around Neon Heights, narrating at each stop. */
public final class Tours {
    private Tours() {}

    public static final String KEY = "tour";
    record Stop(String place, BlockPos pos, String[] lines) {}

    static final Stop[] ROUTE = {
            new Stop("isle_plaza", new BlockPos(-4, 181, 261), new String[]{"Welcome to Neon Heights, everyone! Stay close - first stop, the plaza.", "This plaza was the first thing built up here. The flame sculpture never goes out!", "Right, follow me to the gardens - mind the bridge."}),
            new Stop("gardens", new BlockPos(-47, 181, 259), new String[]{"The Sky Gardens! Ivy looks after every flower up here.", "Those cherry trees came up from the city park as saplings.", "Listen - you can hear the bees. Next stop, the memorial."}),
            new Stop("memorial", new BlockPos(-24, 182, 251), new String[]{"This is Benson's memorial. He was a very good boy.", "He faces north, towards the city, so he can watch over everyone.", "Let's head across to the Observatory - it's a bit of a walk!"}),
            new Stop("observatory", new BlockPos(46, 181, 314), new String[]{"And here's the Observatory! On Wednesdays and Fridays we stargaze here.", "The telescope points south, towards the brightest part of the sky.", "That's the end of the tour - thanks for coming, everybody!"}),
    };

    private static long plannedDay = -1;
    private static int stop = -1;
    private static int atStop;
    private static int line;
    private static long tourDay = -1;
    private static int waitGuide;

    public static boolean tourDay(long day) {
        return Calendar.weekend(day);
    }

    public static void plan(ServerLevel sl, CityData d) {
        long day = Calendar.worldDay(sl);
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (plannedDay == day || tod < 2000 || tod > 4000) return;
        plannedDay = day;
        if (!tourDay(day) || sl.isRaining()) return;
        start(sl, d, day);
    }

    public static int start(ServerLevel sl, CityData d, long day) {
        CityData.Profile guide = null;
        for (CityData.Profile p : d.profiles.values()) if (p.job == Job.GUIDE) guide = p;
        if (guide == null) return 0;
        for (CityData.Plan pl : d.plansFor(guide.id, day)) if (pl.what.equals("party") || pl.what.equals("speech") || pl.what.equals("campaign")) return 0;
        List<String> who = new ArrayList<>();
        who.add(guide.id);
        for (CityData.Profile p : d.profiles.values()) {
            if (p == guide || who.size() >= 7) continue;
            boolean busy = d.plansFor(p.id, day).stream().anyMatch(pl -> !pl.what.equals("hangout") && !pl.what.equals("visit"));
            if (busy) continue;
            int chance = switch (p.trait) {
                case CURIOUS, ADVENTUROUS -> 60;
                case TALKATIVE, FRIENDLY, CHEERFUL -> 40;
                default -> 20;
            };
            if (Math.floorMod(p.id.hashCode() * 7 + day * 13, 100) < chance) who.add(p.id);
        }
        if (who.size() < 2) return 0;
        d.plans.removeIf(pl -> pl.day == day && pl.what.equals("tour"));
        d.addPlan(day, KEY, "tour", who.toArray(new String[0]));
        d.news(day, guide.name + " is running a Sky Tour of Neon Heights this morning - " + (who.size() - 1) + " residents signed up.");
        tourDay = day;
        plannedDay = day;
        stop = 0;
        atStop = 0;
        line = 0;
        waitGuide = 0;
        Place.addDynamic(KEY, "Nova's Sky Tour", ROUTE[0].pos);
        return who.size() - 1;
    }

    public static void tick(ServerLevel sl, CityData d) {
        long day = Calendar.worldDay(sl);
        if (tourDay != day) {
            for (CityData.Plan pl : d.plans) if (pl.day == day && pl.what.equals("tour") && stop < 0) { tourDay = day; stop = 0; }
            if (tourDay != day) return;
        }
        if (stop < 0 || stop >= ROUTE.length) return;
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (tod > 9500 && tod < 22000) {
            finish(d, day);
            return;
        }
        Stop st = ROUTE[stop];
        if (!sl.isPositionEntityTicking(st.pos)) return;
        Resident guide = null;
        for (Resident r : sl.getEntitiesOfClass(Resident.class, new AABB(st.pos).inflate(40, 12, 40), r -> r.profile() != null && r.profile().job == Job.GUIDE)) guide = r;
        if (guide == null || guide.distanceToSqr(st.pos.getX() + 0.5, st.pos.getY(), st.pos.getZ() + 0.5) > 7 * 7) {
            if (++waitGuide > 36) {
                d.news(day, "Nova's Sky Tour was cut short today.");
                finish(d, day);
            }
            return;
        }
        waitGuide = 0;
        List<Resident> group = sl.getEntitiesOfClass(Resident.class, new AABB(st.pos).inflate(9, 4, 9), r -> r.profile() != null && r.profile().job != Job.GUIDE && d.plansFor(r.profileId(), day).stream().anyMatch(pl -> pl.what.equals("tour")));
        atStop++;
        if (atStop < 2 && group.isEmpty()) return;
        if (guide.isFree() && line < st.lines.length && atStop % 2 == 0) {
            guide.gesture(Resident.G_WAVE, 40);
            guide.sayTo(st.lines[line++], 120);
            if (!group.isEmpty()) {
                Resident g = group.get(sl.random.nextInt(group.size()));
                if (g.isFree() && sl.random.nextFloat() < 0.5f) g.say(g.pick("Ooh!", "I never knew that!", "So pretty up here.", "Can we take a photo?", "Wow."), 60);
            }
        }
        for (Resident g : group) {
            DayLog lg = g.profile().log(g.routineDay());
            if (lg.once("tour:" + st.place)) {
                Place pl = Place.get(st.place);
                lg.note("I went on Nova's Sky Tour and saw " + (pl == null ? "Neon Heights" : pl.label));
            }
            g.getLookControl().setLookAt(guide, 30, 30);
        }
        for (ServerPlayer pl : sl.getEntitiesOfClass(ServerPlayer.class, new AABB(st.pos).inflate(30))) {
            if (atStop == 2) pl.displayClientMessage(Component.literal("§b" + guide.profile().name + "'s Sky Tour §7- stop " + (stop + 1) + " of " + ROUTE.length + ". Follow along!"), true);
        }
        if (line >= st.lines.length && atStop >= 8) {
            stop++;
            atStop = 0;
            line = 0;
            if (stop >= ROUTE.length) finish(d, day);
            else Place.addDynamic(KEY, "Nova's Sky Tour", ROUTE[stop].pos);
        }
    }

    static void finish(CityData d, long day) {
        d.plans.removeIf(pl -> pl.day == day && pl.what.equals("tour"));
        stop = ROUTE.length;
        Place.addDynamic(KEY, "Nova's Sky Tour", ROUTE[0].pos);
        d.setDirty();
    }

    public static boolean onTour(CityData d, String id, long day) {
        if (tourDay != day || stop < 0 || stop >= ROUTE.length) return false;
        for (CityData.Plan pl : d.plans) if (pl.day == day && pl.what.equals("tour") && pl.who.contains(id)) return true;
        return false;
    }

    public static String describe() {
        if (stop < 0 || stop >= ROUTE.length) return "No tour running.";
        return "Sky Tour at stop " + (stop + 1) + "/" + ROUTE.length + " (" + ROUTE[stop].place + "), line " + line;
    }
}
