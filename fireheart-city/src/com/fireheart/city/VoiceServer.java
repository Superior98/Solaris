package com.fireheart.city;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/** The host's key speaks for everyone: players without their own ElevenLabs key get voice audio relayed from the server. */
public final class VoiceServer {
    private VoiceServer() {}

    static final int CHUNK = 30000;
    private static final Map<UUID, Long> last = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> burst = new ConcurrentHashMap<>();

    public static boolean relaying() {
        return VoiceApi.usable();
    }

    public static void announce(ServerPlayer pl) {
        PcNet.send(pl, new PcNet.Msg("#voice|" + (relaying() ? "1" : "0")));
    }

    static void request(ServerPlayer pl, String a, String text) {
        if (!relaying() || text == null || text.isBlank() || text.length() > 400) return;
        String[] p = a.split("\\|", 2);
        if (p.length < 2 || !p[0].matches("[A-Za-z0-9]{8,40}") || !p[1].matches("[a-z]{3,10}")) return;
        long now = System.currentTimeMillis();
        Long t = last.get(pl.getUUID());
        int b = t != null && now - t < 3000 ? burst.getOrDefault(pl.getUUID(), 0) + 1 : 0;
        if (b > 12) return;
        last.put(pl.getUUID(), now);
        burst.put(pl.getUUID(), b);
        String voiceId = p[0], tone = p[1];
        String key = VoiceApi.cacheKey(voiceId, text, tone);
        var server = pl.server;
        VoiceApi.POOL.submit(() -> {
            try {
                byte[] pcm = VoiceApi.fetchOrCache(voiceId, text, tone);
                server.execute(() -> send(pl, key, pcm));
            } catch (VoiceApi.QuotaException q) {
                VoiceApi.trip(q.getMessage());
                server.execute(() -> PcNet.send(pl, new PcNet.Msg("#voice|0")));
            } catch (Exception e) {
                VoiceApi.error(e.toString());
            }
        });
    }

    static void send(ServerPlayer pl, String key, byte[] pcm) {
        if (pl.hasDisconnected()) return;
        int total = Math.max(1, (pcm.length + CHUNK - 1) / CHUNK);
        for (int i = 0; i < total; i++) {
            int a = i * CHUNK, b = Math.min(pcm.length, a + CHUNK);
            byte[] part = new byte[b - a];
            System.arraycopy(pcm, a, part, 0, part.length);
            PcNet.send(pl, new PcNet.Blob("voice", key, "", i, total, part));
        }
    }
}
