package com.fireheart.city;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

/** Lets residents ride the Neon Skyliner (datapack "sky": #state 0 = docked at city, 1 = flying up, 2 = docked at island, 3 = flying down). */
public final class Skyliner {
    public static final int CITY = 0, UP = 1, ISLE = 2, DOWN = 3;
    private static final Set<UUID> aboard = new HashSet<>();
    private static long boardStart = -1;
    private static long callCity = -1, callIsle = -1;
    private static long lastCommand = -1000;
    public static boolean debug;

    private Skyliner() {}

    private static int score(ServerLevel sl, String holder) {
        Scoreboard sb = sl.getScoreboard();
        Objective o = sb.getObjective("sky");
        if (o == null || !sb.hasPlayerScore(holder, o)) return -1;
        return sb.getOrCreatePlayerScore(holder, o).getScore();
    }

    public static int state(ServerLevel sl) {
        return score(sl, "#state");
    }

    public static boolean available(ServerLevel sl) {
        return state(sl) >= 0;
    }

    public static void board(Resident r) {
        aboard.add(r.getUUID());
    }

    public static void leave(Resident r) {
        aboard.remove(r.getUUID());
    }

    public static void requestCall(ServerLevel sl, boolean fromIsland) {
        long now = sl.getGameTime();
        if (fromIsland) { if (callIsle < 0) callIsle = now; } else if (callCity < 0) callCity = now;
    }

    private static void run(ServerLevel sl, String cmd) {
        lastCommand = sl.getGameTime();
        sl.getServer().getCommands().performPrefixedCommand(sl.getServer().createCommandSourceStack().withSuppressedOutput().withPermission(4), cmd);
        if (debug) FireheartCity.LOG.info("[Skyliner] " + cmd + " (state " + state(sl) + ")");
    }

    public static void tick(ServerLevel sl) {
        long now = sl.getGameTime();
        if (now % 20 != 0) return;
        int st = state(sl);
        if (st < 0) return;
        aboard.removeIf(id -> !(sl.getEntity(id) instanceof Resident r) || !r.isSkyRider());
        int lock = score(sl, "#lock"), wait = score(sl, "#wait");
        boolean docked = st == CITY || st == ISLE;
        if (!docked) {
            boardStart = -1;
            return;
        }
        if (now - lastCommand < 100) return;
        if (!aboard.isEmpty() && lock <= 0 && wait <= 0) {
            if (boardStart < 0) boardStart = now;
            if (now - boardStart >= 200) {
                boardStart = -1;
                run(sl, "function sky:depart");
                if (st == CITY) callCity = -1; else callIsle = -1;
                return;
            }
        } else if (aboard.isEmpty()) boardStart = -1;
        if (aboard.isEmpty() && lock <= 0 && wait <= 0) {
            long pending = st == CITY ? callIsle : callCity;
            if (pending >= 0 && now - pending >= 100) {
                if (st == CITY) callIsle = -1; else callCity = -1;
                run(sl, "function sky:call");
            }
        }
        if (st == CITY) callCity = -1;
        if (st == ISLE) callIsle = -1;
    }

    public static String describe(ServerLevel sl) {
        int st = state(sl);
        String where = switch (st) {
            case CITY -> "docked at Solaris";
            case UP -> "flying up to Neon Heights";
            case ISLE -> "docked at Neon Heights";
            case DOWN -> "flying down to the city";
            default -> "not found (sky datapack missing?)";
        };
        return "Skyliner: " + where + " | residents aboard: " + aboard.size() + " | lock=" + score(sl, "#lock") + " wait=" + score(sl, "#wait");
    }
}
