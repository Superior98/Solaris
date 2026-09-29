package com.fireheart.city;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

/** Rare city-wide dance party at the Sky Organ stage on Neon Heights, with random tracks. */
public final class Party {
    private Party() {}

    public static final String KEY = "organ";
    public static final int SONGS = 4;
    public static final String[] TITLES = {"An Ending", "Fallen Down", "Finale", "Frozen Time"};
    private static final int START = 10300, END = 13200;
    private static long lastCommand = -1000;
    public static final long FORCED_LENGTH = 7200;
    static long forcedUntil = -1;

    /** A party started with /city party pulls everyone out of work and home until it ends. */
    public static boolean forcedNow(ServerLevel sl) {
        CityData d = CityData.get(sl);
        long day = Calendar.worldDay(sl);
        return d.partyForced == day && d.partyMusicDone != day && sl.getGameTime() < forcedUntil;
    }

    public static void replanAll(ServerLevel sl) {
        for (Resident r : sl.getEntitiesOfClass(Resident.class, new AABB(-400, -64, -400, 400, 400, 400), x -> x.profile() != null)) r.replan();
    }

    public static void stop(ServerLevel sl, CityData d) {
        long day = Calendar.worldDay(sl);
        d.partyMusicDone = day;
        d.plans.removeIf(p -> p.what.equals("dance") && p.day == day);
        forcedUntil = -1;
        if (playing(sl)) run(sl, "function organ:stop");
        d.setDirty();
        replanAll(sl);
    }
    public static boolean debug;

    public static boolean today(CityData d, long day) {
        return d.partyDay == day;
    }

    private static Objective obj(ServerLevel sl) {
        return sl.getScoreboard().getObjective("organ");
    }

    private static int score(ServerLevel sl, String holder) {
        Scoreboard sb = sl.getScoreboard();
        Objective o = obj(sl);
        if (o == null || !sb.hasPlayerScore(holder, o)) return -1;
        return sb.getOrCreatePlayerScore(holder, o).getScore();
    }

    public static boolean organAvailable(ServerLevel sl) {
        return obj(sl) != null;
    }

    public static boolean playing(ServerLevel sl) {
        return score(sl, "#on") == 1;
    }

    private static void run(ServerLevel sl, String cmd) {
        sl.getServer().getCommands().performPrefixedCommand(sl.getServer().createCommandSourceStack().withSuppressedOutput().withPermission(4), cmd);
    }

    public static void playRandom(ServerLevel sl, CityData d) {
        Scoreboard sb = sl.getScoreboard();
        Objective o = obj(sl);
        if (o == null) return;
        int song;
        do song = 1 + sl.random.nextInt(SONGS); while (song == d.partySong && SONGS > 1);
        d.partySong = song;
        d.setDirty();
        sb.getOrCreatePlayerScore("#song", o).setScore(song);
        run(sl, "function organ:sel" + song);
        run(sl, "function organ:play");
        lastCommand = sl.getGameTime();
        for (ServerPlayer p : sl.players()) {
            if (p.blockPosition().distSqr(Place.get(KEY).pos) < 80 * 80)
                p.displayClientMessage(Component.literal("§d♪ Party track: §f" + TITLES[song - 1] + " §d♪"), true);
        }
    }

    public static void start(ServerLevel sl, CityData d, long day, boolean forced) {
        d.partyDay = day;
        d.partyForced = forced ? day : -1;
        d.partyMusicDone = -1;
        if (forced) forcedUntil = sl.getGameTime() + FORCED_LENGTH;
        List<String> who = new ArrayList<>();
        for (CityData.Profile p : d.profiles.values()) {
            boolean birthday = false;
            for (CityData.Plan pl : d.plansFor(p.id, day)) if (pl.what.equals("party")) birthday = true;
            if (birthday) continue;
            d.plans.removeIf(pl -> pl.day == day && pl.who.contains(p.id) && !pl.what.equals("party"));
            who.add(p.id);
        }
        if (who.isEmpty()) return;
        d.addPlan(day, KEY, "dance", who.toArray(new String[0]));
        d.event(day, "party", "there's a big dance party at the Sky Organ on Neon Heights tonight", Place.get(KEY).pos, who.toArray(new String[0]));
        for (ServerPlayer p : sl.players()) Calendar.banner(p, "§d♫ Sky Organ Party! ♫", "§fEveryone's heading to the Sky Organ stage on Neon Heights tonight");
        d.setDirty();
        replanAll(sl);
        FireheartCity.LOG.info("[Party] Sky Organ party planned for day " + day + " with " + who.size() + " residents" + (forced ? " (forced)" : ""));
    }

    public static void tick(ServerLevel sl, CityData d) {
        long day = Calendar.worldDay(sl);
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (d.partyRolled != day && tod >= 8500 && tod < 10300 && !d.profiles.isEmpty()) {
            d.partyRolled = day;
            d.setDirty();
            float chance = switch (Calendar.weekday(day)) { case 4 -> 0.12f; case 5 -> 0.15f; case 6 -> 0.08f; default -> 0.03f; };
            if (sl.random.nextFloat() < chance) start(sl, d, day, false);
        }
        if (!today(d, day)) return;
        boolean forced = d.partyForced == day;
        if (forced && d.partyMusicDone != day && sl.getGameTime() >= forcedUntil) {
            stop(sl, d);
            d.event(day, "party", "the Sky Organ party wound down", Place.get(KEY).pos);
            for (ServerPlayer p : sl.players()) p.displayClientMessage(Component.literal("§d♫ The Sky Organ party is winding down - everyone's heading back. ♫"), true);
            return;
        }
        boolean window = forced ? true : tod >= START && tod < END;
        if (!organAvailable(sl)) return;
        if (window && d.partyMusicDone != day) {
            int crowd = sl.getEntitiesOfClass(Resident.class, new AABB(Place.get(KEY).pos).inflate(18), r -> r.profile() != null).size();
            if (!playing(sl) && sl.getGameTime() - lastCommand > 200 && (crowd >= 2 || forced || tod > START + 900)) playRandom(sl, d);
        } else if (!window && d.partyMusicDone != day && tod >= END) {
            d.partyMusicDone = day;
            if (playing(sl)) run(sl, "function organ:stop");
            d.event(day, "party", "the Sky Organ party wound down for the night", Place.get(KEY).pos);
            d.setDirty();
        }
    }

    public static boolean active(ServerLevel sl, CityData d) {
        long day = Calendar.worldDay(sl);
        return today(d, day) && d.partyMusicDone != day;
    }

    public static BlockPos danceSpot(java.util.Random r) {
        for (int i = 0; i < 20; i++) {
            int x = 41 + r.nextInt(15), z = 274 + r.nextInt(23);
            boolean seat = (x == 44 || x == 46 || x == 48 || x == 50) && (z >= 277 && z <= 282 || z >= 288 && z <= 293);
            if (seat || x == 54 && z >= 283 && z <= 287 || x == 53 && z >= 283 && z <= 287) continue;
            return new BlockPos(x, 181, z);
        }
        return new BlockPos(47, 181, 285);
    }
}
