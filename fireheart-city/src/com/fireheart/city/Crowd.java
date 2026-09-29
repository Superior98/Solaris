package com.fireheart.city;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Loaded residents, looked up once per tick and shared by every ambient system. */
public final class Crowd {
    private Crowd() {}

    private static long at = Long.MIN_VALUE;
    private static List<Resident> all = Collections.emptyList();
    private static long nearAt = Long.MIN_VALUE;
    private static List<Resident> near = Collections.emptyList();

    public static List<Resident> all(ServerLevel sl, CityData d) {
        long gt = sl.getGameTime();
        if (gt != at) {
            List<Resident> out = new ArrayList<>(d.profiles.size());
            for (CityData.Profile p : d.profiles.values()) if (p.entity != null && sl.getEntity(p.entity) instanceof Resident r && r.isAlive()) out.add(r);
            all = out;
            at = gt;
        }
        return all;
    }

    /** Residents close enough to a player that ambient behaviour is worth simulating. */
    public static List<Resident> nearPlayers(ServerLevel sl, CityData d) {
        long gt = sl.getGameTime();
        if (gt != nearAt) {
            double r = FhcConfig.ambientRange();
            List<Resident> out = new ArrayList<>();
            List<ServerPlayer> players = sl.players();
            for (Resident res : all(sl, d)) {
                for (ServerPlayer pl : players) {
                    if (!pl.isSpectator() && pl.distanceToSqr(res) < r * r) {
                        out.add(res);
                        break;
                    }
                }
            }
            near = out;
            nearAt = gt;
        }
        return near;
    }

    public static void reset() {
        at = nearAt = Long.MIN_VALUE;
        all = near = Collections.emptyList();
    }
}
