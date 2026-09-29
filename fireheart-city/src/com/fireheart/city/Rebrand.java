package com.fireheart.city;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.chunk.LevelChunk;

/** Rewrites the old city name on signs to Solaris, chunk by chunk as players move around. */
public final class Rebrand {
    private Rebrand() {}

    private static final Set<Long> DONE = new HashSet<>();
    private static boolean loaded;
    static final String[][] WORDS = {{"Fireheart City", "Solaris"}, {"FIREHEART CITY", "SOLARIS"}, {"Fireheart", "Solaris"}, {"FIREHEART", "SOLARIS"}, {"FirePhone", "SolPhone"}, {"FireTech", "SolTech"}, {"FireEats", "SolEats"}, {"FireTube", "SolTube"}, {"FireFeed", "SolFeed"}};

    public static void reset() {
        DONE.clear();
        loaded = false;
    }

    static String fix(String s) {
        for (String[] w : WORDS) s = s.replace(w[0], w[1]);
        return s;
    }

    static SignText fix(SignText t, boolean[] changed) {
        for (int i = 0; i < 4; i++) {
            Component c = t.getMessage(i, false);
            String old = c.getString();
            String nu = fix(old);
            if (!nu.equals(old)) {
                t = t.setMessage(i, Component.literal(nu).withStyle(c.getStyle()));
                changed[0] = true;
            }
        }
        return t;
    }

    static int chunk(ServerLevel sl, LevelChunk ch) {
        int n = 0;
        for (BlockEntity be : ch.getBlockEntities().values()) {
            if (!(be instanceof SignBlockEntity s)) continue;
            boolean[] changed = {false};
            SignText f = fix(s.getFrontText(), changed);
            SignText b = fix(s.getBackText(), changed);
            if (!changed[0]) continue;
            s.setText(f, true);
            s.setText(b, false);
            s.setChanged();
            BlockPos p = s.getBlockPos();
            sl.sendBlockUpdated(p, s.getBlockState(), s.getBlockState(), 3);
            n++;
        }
        return n;
    }

    public static void tick(ServerLevel sl, CityData d) {
        if (!loaded) {
            loaded = true;
            DONE.addAll(d.rebrandChunks);
        }
        int changed = 0;
        for (ServerPlayer pl : sl.players()) {
            ChunkPos c = pl.chunkPosition();
            for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) {
                long k = ChunkPos.asLong(c.x + dx, c.z + dz);
                if (DONE.contains(k)) continue;
                LevelChunk ch = sl.getChunkSource().getChunkNow(c.x + dx, c.z + dz);
                if (ch == null) continue;
                DONE.add(k);
                d.rebrandChunks.add(k);
                changed += chunk(sl, ch);
            }
        }
        if (changed > 0) FireheartCity.LOG.info("Rebrand: updated " + changed + " signs to Solaris");
        d.setDirty();
    }
}
