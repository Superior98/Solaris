package com.fireheart.city.client;

import com.fireheart.city.PcNet;
import com.fireheart.city.VoiceApi;
import com.fireheart.city.VoiceConfig;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Resident voices. Uses this PC's own ElevenLabs key when there is one; otherwise asks the host, whose key fetches the
 * audio and streams it here. Text bubbles always show, so any failure just means silence.
 */
public final class VoiceClient {
    private static final Map<Long, Long> lastSpoken = new ConcurrentHashMap<>();
    private static final Map<String, Pending> pending = new ConcurrentHashMap<>();
    private static final Map<String, byte[][]> parts = new ConcurrentHashMap<>();
    private static volatile boolean relay;

    record Pending(long entityId, double x, double y, double z, boolean call, long at) {}

    private VoiceClient() {}

    public static void relay(boolean on) {
        relay = on;
    }

    public static boolean relayOn() {
        return relay;
    }

    public static boolean available() {
        return VoiceApi.usable() || (relay && VoiceConfig.enabledInConfig());
    }

    public static void speak(long entityId, String voiceId, String text, double x, double y, double z) {
        speak(entityId, voiceId, text, x, y, z, 0, false);
    }

    public static void speakCall(long entityId, String voiceId, String text, double x, double y, double z) {
        speak(entityId, voiceId, text, x, y, z, 0, true);
    }

    public static void speak(long entityId, String voiceId, String text, double x, double y, double z, int gesture) {
        speak(entityId, voiceId, text, x, y, z, gesture, false);
    }

    static void speak(long entityId, String voiceId, String text, double x, double y, double z, int gesture, boolean call) {
        if (!available() || text == null || text.isBlank() || voiceId == null || voiceId.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (!call && mc.level != null && VoiceConfig.inQuietHours(mc.level.getDayTime())) return;
        if (mc.player == null || mc.player.position().distanceTo(new Vec3(x, y, z)) > VoiceConfig.hearRangeBlocks()) return;
        String line = text.replaceAll("\\*[^*]*\\*", "").replaceAll("§.", "").trim();
        if (line.isEmpty()) return;
        long now = System.currentTimeMillis();
        Long last = lastSpoken.get(entityId);
        if (last != null && now - last < 1200) return;
        lastSpoken.put(entityId, now);
        String tone = voiceId.equals(com.fireheart.city.Cast.SULTRY) ? "calm" : VoiceApi.tone(line, gesture);
        String key = VoiceApi.cacheKey(voiceId, line, tone);
        if (VoiceApi.usable()) {
            VoiceApi.POOL.submit(() -> {
                try {
                    byte[] pcm = VoiceApi.fetchOrCache(voiceId, line, tone);
                    play(pcm, entityId, x, y, z, call);
                } catch (VoiceApi.QuotaException e) {
                    VoiceApi.trip(e.getMessage());
                } catch (Exception e) {
                    VoiceApi.error(e.toString());
                }
            });
            return;
        }
        byte[] c = VoiceApi.cachedOnly(key);
        if (c != null) {
            VoiceApi.POOL.submit(() -> play(c, entityId, x, y, z, call));
            return;
        }
        pending.put(key, new Pending(entityId, x, y, z, call, now));
        PcNet.CHANNEL.sendToServer(new PcNet.Act(BlockPos.ZERO, "voice_req", voiceId + "|" + tone, line));
    }

    /** A chunk of relayed voice audio from the host. */
    public static void receive(PcNet.Blob m) {
        byte[][] ps = parts.computeIfAbsent(m.id, k -> new byte[m.total][]);
        if (ps.length != m.total || m.index < 0 || m.index >= ps.length) { parts.remove(m.id); return; }
        ps[m.index] = m.bytes;
        for (byte[] b : ps) if (b == null) return;
        parts.remove(m.id);
        int n = 0;
        for (byte[] b : ps) n += b.length;
        byte[] all = new byte[n];
        int o = 0;
        for (byte[] b : ps) { System.arraycopy(b, 0, all, o, b.length); o += b.length; }
        Pending p = pending.remove(m.id);
        VoiceApi.POOL.submit(() -> {
            VoiceApi.store(m.id, all);
            if (p != null && System.currentTimeMillis() - p.at() < 15000) play(all, p.entityId(), p.x(), p.y(), p.z(), p.call());
        });
    }

    private static void play(byte[] pcm, long entityId, double x, double y, double z, boolean call) {
        try {
            if (pcm == null || pcm.length <= 44) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            Vec3 at = new Vec3(x, y, z);
            if (!call && mc.level != null && entityId >= 0 && entityId < Integer.MAX_VALUE) {
                Entity e = mc.level.getEntity((int) entityId);
                if (e != null) at = e.position();
            }
            double range = VoiceConfig.hearRangeBlocks();
            double dist = call ? 0 : mc.player.position().distanceTo(at);
            if (dist > range) return;
            float master = mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER) * mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.VOICE);
            float vol = (float) Math.max(0f, Math.min(1f, 1.0 - dist / range)) * master;
            if (vol <= 0.02f) return;
            AudioFormat format = new AudioFormat(VoiceConfig.sampleRate(), 16, 1, true, false);
            try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format))) {
                line.open(format);
                try {
                    var gain = (javax.sound.sampled.FloatControl) line.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
                    gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), (float) (20.0 * Math.log10(Math.max(0.02f, vol))))));
                } catch (IllegalArgumentException ignored) {}
                line.start();
                line.write(pcm, 0, pcm.length);
                line.drain();
            }
        } catch (Exception e) {
            VoiceApi.error("playback: " + e);
        }
    }
}
