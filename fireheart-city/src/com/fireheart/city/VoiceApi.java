package com.fireheart.city;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import net.minecraftforge.fml.loading.FMLPaths;

/** ElevenLabs text-to-speech shared by the client (own key) and the host (relays voices to players without a key). */
public final class VoiceApi {
    private VoiceApi() {}

    public static final ExecutorService POOL = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "fireheartcity-voice");
        t.setDaemon(true);
        return t;
    });
    static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    static final String FALLBACK_FEMALE = "EXAVITQu4vr4xnSDxMaL";
    static final Map<String, String> MODERN = Map.ofEntries(
            Map.entry("21m00Tcm4TlvDq8ikWAM", "FGY2WhTYpPnrIDTdsKH5"), Map.entry("AZnzlk1XvdvUeBnXmlld", "cgSgspJ2msm6clMCkdW9"),
            Map.entry("MF3mGyEYCl7XYWbV9V6O", "pFZP5JQG7iQjIQuC4Bku"), Map.entry("jsCqWAovK2LkecY7zXl4", "Xb7hH8MSUJpSbSDYk0k2"),
            Map.entry("oWAxZDx7w5VEj9dCyTzz", "XrExE9yKIg1WjnnlVkGX"), Map.entry("pNInz6obpgDQGcFmaJgB", "nPczCjzI2devNBz1zQrb"),
            Map.entry("ErXwobaYiN019PkySvjV", "iP95p4xoKVk53GoZ742B"), Map.entry("VR6AewLTigWG4xSOukaG", "cjVigY5qzO86Huf0OWal"),
            Map.entry("TxGEqnHWrfWFTfGW9XjX", "TX3LPaxmHKxFdv7VOQHJ"), Map.entry("yoZ06aMxZJJ28mfd3POQ", "pqHfZKP75CvOlQylNhV4"),
            Map.entry("g5CIjZEefAph4nQFvHAz", "bIHbv24MWmeRgasZH58o"), Map.entry("XB0fDUnXU5powFXDhCwa", "EXAVITQu4vr4xnSDxMaL"));
    static final Map<String, Boolean> MISSING = new ConcurrentHashMap<>();
    private static volatile boolean tripped;
    private static volatile long trippedAt;
    private static volatile String lastError = "";
    private static volatile long lastErrorAt;
    private static final Map<String, Long> warned = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 10 * 60 * 1000L;
    public static volatile int fetched, cached, failed;

    public static class QuotaException extends Exception {
        public QuotaException(String msg) { super(msg); }
    }

    public static boolean usable() {
        if (!VoiceConfig.enabledInConfig() || VoiceConfig.apiKey().isEmpty()) return false;
        if (tripped && System.currentTimeMillis() - trippedAt < COOLDOWN_MS) return false;
        tripped = false;
        return true;
    }

    public static String lastError() {
        return lastError.isEmpty() ? "none" : lastError + " (" + (System.currentTimeMillis() - lastErrorAt) / 1000 + "s ago)";
    }

    public static boolean tripped() {
        return tripped && System.currentTimeMillis() - trippedAt < COOLDOWN_MS;
    }

    public static void trip(String reason) {
        tripped = true;
        trippedAt = System.currentTimeMillis();
        error(reason);
        FireheartCity.LOG.warn("ElevenLabs voice paused for 10 min (" + reason + "). Residents use text bubbles until it recovers.");
    }

    public static void error(String e) {
        lastError = e;
        lastErrorAt = System.currentTimeMillis();
        failed++;
        Long t = warned.get(e);
        if (t == null || System.currentTimeMillis() - t > 300_000) {
            warned.put(e, System.currentTimeMillis());
            FireheartCity.LOG.warn("Voice: " + e);
        }
    }

    public static String tone(String text, int gesture) {
        if (gesture == 5) return "angry";
        if (gesture == 6 || text.endsWith("...")) return "calm";
        if (gesture == 4 || text.endsWith("!")) return "excited";
        return "normal";
    }

    static String settings(String voiceId, String tone) {
        if (voiceId.equals(Cast.SULTRY)) return "{\"stability\":0.55,\"similarity_boost\":0.85,\"style\":0.45,\"use_speaker_boost\":true}";
        return switch (tone) {
            case "angry" -> "{\"stability\":0.25,\"similarity_boost\":0.8,\"style\":0.75,\"use_speaker_boost\":true}";
            case "excited" -> "{\"stability\":0.3,\"similarity_boost\":0.8,\"style\":0.55,\"use_speaker_boost\":true}";
            case "calm" -> "{\"stability\":0.75,\"similarity_boost\":0.8,\"style\":0.1,\"use_speaker_boost\":true}";
            default -> "{\"stability\":0.45,\"similarity_boost\":0.8,\"style\":0.25,\"use_speaker_boost\":true}";
        };
    }

    public static String cacheKey(String voiceId, String text, String tone) {
        return sha1(voiceId + "|" + VoiceConfig.outputFormat() + "|" + tone + "|" + text);
    }

    public static Path cacheDir() throws IOException {
        Path d = FMLPaths.CONFIGDIR.get().resolve("fireheartcity").resolve("voicecache");
        Files.createDirectories(d);
        return d;
    }

    public static byte[] cachedOnly(String key) {
        try {
            Path p = cacheDir().resolve(key + ".pcm");
            return Files.exists(p) ? Files.readAllBytes(p) : null;
        } catch (IOException e) {
            return null;
        }
    }

    public static void store(String key, byte[] pcm) {
        try {
            Path dir = cacheDir();
            Path tmp = Files.createTempFile(dir, "tmp", ".pcm");
            Files.write(tmp, pcm);
            Files.move(tmp, dir.resolve(key + ".pcm"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {}
    }

    public static byte[] fetchOrCache(String voiceId, String text, String tone) throws Exception {
        String key = cacheKey(voiceId, text, tone);
        byte[] c = cachedOnly(key);
        if (c != null) { cached++; return c; }
        byte[] pcm;
        String v = MISSING.containsKey(voiceId) ? MODERN.getOrDefault(voiceId, FALLBACK_FEMALE) : voiceId;
        try {
            pcm = request(v, text, tone);
        } catch (VoiceMissing m) {
            MISSING.put(voiceId, true);
            String alt = MODERN.getOrDefault(voiceId, FALLBACK_FEMALE);
            FireheartCity.LOG.info("Voice " + voiceId + " isn't available on this ElevenLabs account; using " + alt + " instead.");
            try {
                pcm = request(alt, text, tone);
            } catch (VoiceMissing m2) {
                pcm = request(FALLBACK_FEMALE, text, tone);
            }
        }
        store(key, pcm);
        fetched++;
        return pcm;
    }

    static final class VoiceMissing extends Exception {}

    static byte[] request(String voiceId, String text, String tone) throws Exception {
        String body = "{\"text\":" + json(text) + ",\"model_id\":" + json(VoiceConfig.modelId()) + ",\"voice_settings\":" + settings(voiceId, tone) + "}";
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.elevenlabs.io/v1/text-to-speech/" + voiceId + "?output_format=" + VoiceConfig.outputFormat()))
                .timeout(Duration.ofSeconds(25))
                .header("xi-api-key", VoiceConfig.apiKey())
                .header("Content-Type", "application/json")
                .header("Accept", "audio/*")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<byte[]> resp;
        try {
            resp = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            resp = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
        }
        int sc = resp.statusCode();
        String snippet = sc == 200 ? "" : new String(resp.body(), StandardCharsets.UTF_8);
        if (sc == 401 || sc == 403) {
            if (snippet.contains("quota") || snippet.contains("credit")) throw new QuotaException("out of credits (" + sc + ")");
            throw new QuotaException("API key rejected (" + sc + "): " + cut(snippet));
        }
        if (sc == 429) throw new QuotaException("rate limited / quota exceeded");
        if ((sc == 404 || sc == 400) && snippet.contains("voice") && !voiceId.equals(FALLBACK_FEMALE)) throw new VoiceMissing();
        if (sc >= 500) throw new IOException("ElevenLabs server error " + sc);
        if (sc != 200) {
            if (snippet.contains("quota") || snippet.contains("credit")) throw new QuotaException("out of credits");
            throw new IOException("ElevenLabs returned " + sc + ": " + cut(snippet));
        }
        return resp.body();
    }

    static String cut(String s) {
        return s.substring(0, Math.min(180, s.length()));
    }

    /** Asks ElevenLabs about the key: tier and characters used. Result is delivered on the calling pool thread. */
    public static void status(Consumer<String> out) {
        String key = VoiceConfig.apiKey();
        if (key.isEmpty()) { out.accept("§cNo ElevenLabs key in config/fireheartcity-voice.properties (apiKey=)."); return; }
        if (!VoiceConfig.enabledInConfig()) { out.accept("§cVoices are turned off (enabled=false in config/fireheartcity-voice.properties)."); return; }
        POOL.submit(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.elevenlabs.io/v1/user/subscription")).timeout(Duration.ofSeconds(15)).header("xi-api-key", key).GET().build();
                HttpResponse<String> r = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                String b = r.body();
                if (r.statusCode() == 200) {
                    long used = num(b, "character_count"), lim = num(b, "character_limit");
                    String tier = str(b, "tier");
                    long reset = num(b, "next_character_count_reset_unix");
                    String when = reset > 0 ? java.time.Instant.ofEpochSecond(reset).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString() : "?";
                    boolean out0 = lim > 0 && used >= lim;
                    out.accept((out0 ? "§c" : "§a") + "ElevenLabs key OK §7| plan: §f" + tier + " §7| used §f" + used + "§7/§f" + lim + " §7characters" + (out0 ? " §c(out of credits)" : "") + " §7| resets " + when);
                    if (!out0) tripped = false;
                } else if (r.statusCode() == 401) {
                    if (b.contains("missing_permissions") || b.contains("user_read")) {
                        testSpeak(out);
                    } else out.accept("§cElevenLabs rejected the key (401). Check apiKey= in config/fireheartcity-voice.properties: " + cut(b));
                } else out.accept("§cElevenLabs answered " + r.statusCode() + ": " + cut(b));
            } catch (Exception e) {
                out.accept("§cCan't reach api.elevenlabs.io from this PC: " + e);
            }
        });
    }

    static void testSpeak(Consumer<String> out) {
        try {
            byte[] pcm = request(Cast.SULTRY, "Hey there.", "calm");
            out.accept("§aElevenLabs key works §7(it has no permission to read usage, but speech came back: " + pcm.length / 1000 + " KB).");
            tripped = false;
        } catch (QuotaException q) {
            out.accept("§cElevenLabs: " + q.getMessage());
        } catch (Exception e) {
            out.accept("§cElevenLabs test failed: " + e.getMessage());
        }
    }

    static long num(String json, String k) {
        var m = java.util.regex.Pattern.compile("\"" + k + "\"\\s*:\\s*(\\d+)").matcher(json);
        return m.find() ? Long.parseLong(m.group(1)) : -1;
    }

    static String str(String json, String k) {
        var m = java.util.regex.Pattern.compile("\"" + k + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : "?";
    }

    static String json(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                default -> { if (c >= ' ') sb.append(c); }
            }
        }
        return sb.append('"').toString();
    }

    public static String sha1(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}
