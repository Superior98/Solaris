package com.fireheart.city;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.minecraftforge.fml.loading.FMLPaths;

/** Reads config/fireheartcity-voice.properties. Client-side only; never touches the save. */
public final class VoiceConfig {
    private static Properties props;
    private static long loadedAt;

    public static synchronized Properties get() {
        Path path = FMLPaths.CONFIGDIR.get().resolve("fireheartcity-voice.properties");
        long now = System.currentTimeMillis();
        if (props != null && now - loadedAt < 5000) return props;
        Properties p = new Properties();
        try {
            if (!Files.exists(path)) {
                p.setProperty("apiKey", "");
                p.setProperty("enabled", "true");
                p.setProperty("hearRangeBlocks", "18");
                p.setProperty("outputFormat", "pcm_22050");
                p.setProperty("modelId", "eleven_turbo_v2_5");
                p.setProperty("quietHours", "false");
                p.setProperty("quietStart", "13500");
                p.setProperty("quietEnd", "23000");
                try (FileOutputStream out = new FileOutputStream(path.toFile())) {
                    p.store(out, "Solaris voices - paste your ElevenLabs API key below.\n"
                            + "Get one at https://elevenlabs.io (Profile -> API Keys).\n"
                            + "enabled=false always shows text bubbles only, never calls the API.\n"
                            + "hearRangeBlocks: only fetch/play voice when a player is this close (saves credits).\n"
                            + "If credits run out or the key is invalid, voice turns itself off automatically\n"
                            + "and the game falls back to text bubbles until it can reach the API again.");
                }
            } else {
                try (FileInputStream in = new FileInputStream(path.toFile())) {
                    p.load(in);
                }
            }
        } catch (IOException e) {
            FireheartCity.LOG.warn("Could not read/create voice config", e);
        }
        props = p;
        loadedAt = now;
        return p;
    }

    public static String apiKey() {
        return get().getProperty("apiKey", "").trim();
    }

    public static boolean enabledInConfig() {
        return Boolean.parseBoolean(get().getProperty("enabled", "true"));
    }

    public static double hearRangeBlocks() {
        try {
            return Double.parseDouble(get().getProperty("hearRangeBlocks", "18"));
        } catch (NumberFormatException e) {
            return 18;
        }
    }

    public static String outputFormat() {
        return get().getProperty("outputFormat", "pcm_22050");
    }

    public static int sampleRate() {
        String f = outputFormat();
        int i = f.lastIndexOf('_');
        try {
            return i >= 0 ? Integer.parseInt(f.substring(i + 1)) : 22050;
        } catch (NumberFormatException e) {
            return 22050;
        }
    }

    public static boolean inQuietHours(long dayTime) {
        if (!Boolean.parseBoolean(get().getProperty("quietHours", "false"))) return false;
        long t = Math.floorMod(dayTime, 24000L);
        long a = num("quietStart", 13500), b = num("quietEnd", 23000);
        return a <= b ? t >= a && t < b : t >= a || t < b;
    }

    private static long num(String key, long def) {
        try {
            return Long.parseLong(get().getProperty(key, String.valueOf(def)).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static String modelId() {
        return get().getProperty("modelId", "eleven_turbo_v2_5");
    }
}
