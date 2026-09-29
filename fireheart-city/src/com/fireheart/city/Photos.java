package com.fireheart.city;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Real photos: the client captures the view, uploads it in chunks, and the server keeps it in the world folder
 * (fhc_photos/). Anyone who sees the photo in a feed, chat or album downloads it on demand.
 */
public final class Photos {
    private Photos() {}

    static final int CHUNK = 30000, MAX = 600_000, MAX_VIDEO = 6_000_000;
    private static final Map<String, byte[][]> UPLOADS = new HashMap<>();

    static Path dir(MinecraftServer s) {
        return s.getWorldPath(LevelResource.ROOT).resolve("fhc_photos");
    }

    static boolean validId(String id) {
        return id.matches("[a-z0-9]{4,24}");
    }

    public static void receive(ServerPlayer pl, PcNet.Blob m) throws IOException {
        boolean video = m.kind.equals("video");
        int max = video ? MAX_VIDEO : MAX;
        if (!validId(m.id) || m.total <= 0 || m.total > max / CHUNK + 1 || m.index < 0 || m.index >= m.total) return;
        if (m.kind.equals("get")) {
            send(pl, m.id);
            return;
        }
        if (!m.kind.equals("photo") && !video) return;
        String key = pl.getUUID() + "|" + m.id;
        byte[][] parts = UPLOADS.computeIfAbsent(key, k -> new byte[m.total][]);
        if (parts.length != m.total) return;
        parts[m.index] = m.bytes;
        for (byte[] b : parts) if (b == null) return;
        UPLOADS.remove(key);
        int n = 0;
        for (byte[] b : parts) n += b.length;
        if (n > max) return;
        byte[] all = new byte[n];
        int o = 0;
        for (byte[] b : parts) { System.arraycopy(b, 0, all, o, b.length); o += b.length; }
        if (all.length < 8 || (all[0] & 0xFF) != 0x89 || all[1] != 'P' || all[2] != 'N' || all[3] != 'G') return;
        Path d = dir(pl.server);
        Files.createDirectories(d);
        Files.write(d.resolve(m.id + ".png"), all);
        added(pl, m.id, m.meta, video ? "vd_" : "ph_");
    }

    static void added(ServerPlayer pl, String id, String meta, String prefix) {
        CityData d = CityData.get(pl.serverLevel());
        String pn = pl.getName().getString();
        List<String> g = Extras.gallery(d, pn);
        String scene = Extras.sceneAt(pl);
        g.add(0, prefix + id + ":" + Calendar.worldDay(pl.serverLevel()) + ":" + scene);
        while (g.size() > 60) g.remove(g.size() - 1);
        d.setSetting(pn, "gallery", String.join(",", g));
        pl.serverLevel().playSound(null, pl.blockPosition(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 0.6f, 1.6f);
        for (Resident r : pl.serverLevel().getEntitiesOfClass(Resident.class, pl.getBoundingBox().inflate(6), x -> x.profile() != null && x.isFree())) {
            if (r.getRandom().nextFloat() < 0.5f) r.say(r.pick("Ooh, take one of me!", "Say cheese!", "Is that for SolFeed?", "*strikes a pose*", "Did you get my good side?"), 50);
            break;
        }
        PcNet.send(pl, new PcNet.Msg("#photo|" + id + "|" + prefix));
        if (Phones.OPEN_PHONE.contains(pl.getUUID())) PcNet.send(pl, Computers.data(pl, net.minecraft.core.BlockPos.ZERO));
    }

    public static void send(ServerPlayer pl, String id) throws IOException {
        Path f = dir(pl.server).resolve(id + ".png");
        if (!Files.exists(f)) {
            PcNet.send(pl, new PcNet.Blob("photo", id, "missing", 0, 1, new byte[0]));
            return;
        }
        byte[] all = Files.readAllBytes(f);
        int total = Math.max(1, (all.length + CHUNK - 1) / CHUNK);
        for (int i = 0; i < total; i++) {
            int a = i * CHUNK, b = Math.min(all.length, a + CHUNK);
            byte[] part = new byte[b - a];
            System.arraycopy(all, a, part, 0, part.length);
            PcNet.send(pl, new PcNet.Blob("photo", id, "", i, total, part));
        }
    }

    public static String newId() {
        return Long.toString(System.currentTimeMillis(), 36) + Integer.toString(Math.abs(UUID.randomUUID().hashCode() % 1296), 36);
    }
}
