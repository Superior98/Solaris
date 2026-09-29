package com.fireheart.city;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Optional Groq-powered dialogue. When a key is set in config/fireheartcity-ai.properties, residents answer chat with
 * an LLM in character. If the API is rate-limited, out of credits, unreachable or slow, they fall back to the built-in
 * dialogue and the mod quietly retries once the limit resets.
 */
public final class Groq {
    private Groq() {}

    static final String URL = "https://api.groq.com/openai/v1/chat/completions";
    private static Properties props;
    private static long loadedAt;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static volatile long pausedUntil;
    private static volatile String status = "not configured";
    private static volatile boolean badKey;
    private static volatile String badKeyValue = "";
    private static volatile int failures;
    private static volatile int calls, fallbacks;
    private static final Map<String, Deque<String[]>> HISTORY = new HashMap<>();

    static synchronized Properties cfg() {
        Path path = FMLPaths.CONFIGDIR.get().resolve("fireheartcity-ai.properties");
        long now = System.currentTimeMillis();
        if (props != null && now - loadedAt < 5000) return props;
        Properties p = new Properties();
        try {
            if (!Files.exists(path)) {
                p.setProperty("groqApiKey", "");
                p.setProperty("enabled", "true");
                p.setProperty("model", "openai/gpt-oss-20b");
                p.setProperty("maxTokens", "300");
                p.setProperty("timeoutSeconds", "8");
                try (FileOutputStream out = new FileOutputStream(path.toFile())) {
                    p.store(out, "Solaris resident AI (Groq).\n"
                            + "Paste your Groq API key after groqApiKey= (get one at https://console.groq.com/keys).\n"
                            + "When the key is empty, disabled, rate-limited or out of credits, residents use the built-in dialogue,\n"
                            + "and switch back to the AI automatically once your limits reset.\n"
                            + "model: any Groq chat model id, e.g. openai/gpt-oss-20b, openai/gpt-oss-120b, llama-3.1-8b-instant");
                }
            } else {
                try (FileInputStream in = new FileInputStream(path.toFile())) {
                    p.load(in);
                }
            }
        } catch (IOException e) {
            FireheartCity.LOG.warn("Could not read/create AI config", e);
        }
        props = p;
        loadedAt = now;
        return p;
    }

    static String key() {
        return cfg().getProperty("groqApiKey", "").trim();
    }

    public static boolean available() {
        String k = key();
        if (k.isEmpty() || !Boolean.parseBoolean(cfg().getProperty("enabled", "true"))) {
            status = k.isEmpty() ? "no API key set (config/fireheartcity-ai.properties)" : "disabled in config";
            return false;
        }
        if (badKey && k.equals(badKeyValue)) return false;
        badKey = false;
        return System.currentTimeMillis() >= pausedUntil;
    }

    public static String status() {
        available();
        long wait = pausedUntil - System.currentTimeMillis();
        return status + (wait > 0 ? " - retrying in " + (wait / 1000) + "s" : "") + " | AI replies: " + calls + ", fallbacks: " + fallbacks;
    }

    static int intCfg(String k, int def) {
        try {
            return Integer.parseInt(cfg().getProperty(k, String.valueOf(def)).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    static void remember(String convo, String who, String text) {
        synchronized (HISTORY) {
            Deque<String[]> h = HISTORY.computeIfAbsent(convo, k -> new ArrayDeque<>());
            h.addLast(new String[]{who, text});
            while (h.size() > 10) h.removeFirst();
        }
    }

    /** Asks Groq for a reply. Completes with null on any failure so the caller can fall back. */
    public static CompletableFuture<String> ask(String convo, String system, String playerText) {
        JsonObject body = new JsonObject();
        String model = cfg().getProperty("model", "openai/gpt-oss-20b").trim();
        body.addProperty("model", model);
        body.addProperty("temperature", 0.9);
        body.addProperty("max_tokens", intCfg("maxTokens", 300));
        if (model.startsWith("openai/gpt-oss")) body.addProperty("reasoning_effort", "low");
        JsonArray msgs = new JsonArray();
        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", system);
        msgs.add(sys);
        synchronized (HISTORY) {
            Deque<String[]> h = HISTORY.get(convo);
            if (h != null) for (String[] m : h) {
                JsonObject o = new JsonObject();
                o.addProperty("role", m[0]);
                o.addProperty("content", m[1]);
                msgs.add(o);
            }
        }
        JsonObject u = new JsonObject();
        u.addProperty("role", "user");
        u.addProperty("content", playerText);
        msgs.add(u);
        body.add("messages", msgs);
        HttpRequest req = HttpRequest.newBuilder(URI.create(URL))
                .timeout(Duration.ofSeconds(intCfg("timeoutSeconds", 8)))
                .header("Authorization", "Bearer " + key())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        String usedKey = key();
        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString()).handle((res, err) -> {
            if (err != null) {
                fail("unreachable (" + err.getClass().getSimpleName() + ")", 0);
                return null;
            }
            int code = res.statusCode();
            if (code == 429) {
                long wait = retryAfter(res);
                pausedUntil = System.currentTimeMillis() + wait;
                status = "rate-limited / out of credits - using built-in dialogue";
                FireheartCity.LOG.info("Groq limit reached; built-in dialogue for " + (wait / 1000) + "s");
                fallbacks++;
                return null;
            }
            if (code == 401 || code == 403) {
                badKey = true;
                badKeyValue = usedKey;
                status = "API key rejected (" + code + ") - fix groqApiKey in config/fireheartcity-ai.properties";
                FireheartCity.LOG.warn("Groq rejected the API key (" + code + ")");
                fallbacks++;
                return null;
            }
            if (code == 404 || code == 400) {
                fail("request rejected (" + code + "): " + shortBody(res.body()), 10 * 60_000);
                return null;
            }
            if (code >= 300) {
                fail("error " + code, 0);
                return null;
            }
            try {
                JsonObject o = JsonParser.parseString(res.body()).getAsJsonObject();
                String text = o.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString();
                text = clean(text);
                if (text.isEmpty()) {
                    fail("empty reply", 0);
                    return null;
                }
                failures = 0;
                calls++;
                status = "online (" + cfg().getProperty("model", "") + ")";
                remember(convo, "user", playerText);
                remember(convo, "assistant", text);
                return text;
            } catch (Exception e) {
                fail("bad response", 0);
                return null;
            }
        });
    }

    static void fail(String why, long pause) {
        fallbacks++;
        failures++;
        status = "fallback: " + why;
        if (pause > 0) pausedUntil = System.currentTimeMillis() + pause;
        else if (failures >= 3) pausedUntil = System.currentTimeMillis() + 5 * 60_000;
        FireheartCity.LOG.warn("Groq " + why + " - using built-in dialogue");
    }

    static String shortBody(String b) {
        return b == null ? "" : b.length() > 160 ? b.substring(0, 160) : b;
    }

    static final Pattern DUR = Pattern.compile("(?:(\\d+)h)?(?:(\\d+)m(?!s))?(?:([\\d.]+)s)?");

    static long retryAfter(HttpResponse<String> res) {
        long best = 0;
        for (String h : new String[]{"retry-after", "x-ratelimit-reset-requests", "x-ratelimit-reset-tokens"}) {
            String v = res.headers().firstValue(h).orElse(null);
            if (v == null) continue;
            long ms = parseDuration(v.trim());
            if (h.equals("retry-after") || ms > best) best = Math.max(best, ms);
        }
        return Math.max(30_000, Math.min(best == 0 ? 60_000 : best, 6 * 3600_000L));
    }

    static long parseDuration(String v) {
        try {
            return (long) (Double.parseDouble(v) * 1000);
        } catch (NumberFormatException ignored) {}
        Matcher m = DUR.matcher(v);
        if (!m.matches()) return 0;
        double s = 0;
        if (m.group(1) != null) s += Integer.parseInt(m.group(1)) * 3600;
        if (m.group(2) != null) s += Integer.parseInt(m.group(2)) * 60;
        if (m.group(3) != null) s += Double.parseDouble(m.group(3));
        return (long) (s * 1000);
    }

    static String clean(String t) {
        t = t.replaceAll("(?s)<think>.*?</think>", "").replace("\n", " ").replaceAll("\\s+", " ").trim();
        if (t.startsWith("\"") && t.endsWith("\"") && t.length() > 1) t = t.substring(1, t.length() - 1);
        t = t.replaceAll("^[A-Z][a-z]+:\\s*", "");
        if (t.length() > 220) {
            int cut = t.lastIndexOf('.', 220);
            t = cut > 60 ? t.substring(0, cut + 1) : t.substring(0, 217) + "...";
        }
        return t.replace("§", "");
    }

    static String persona(Resident r, String pn, String hint) {
        CityData d = r.data();
        CityData.Profile p = r.profile();
        CityData.Rel rel = d.playerRel(p.id, pn);
        int trust = p.mind.trustIn(pn);
        StringBuilder s = new StringBuilder();
        s.append("You are ").append(p.name).append(", a resident of Solaris, a cosy Create-mod city in Minecraft. ");
        s.append("You work as the ").append(p.job.title.toLowerCase(Locale.ROOT)).append(" at ").append(p.job.work().label).append(". ");
        s.append("Personality: ").append(p.trait.adjective()).append(". ");
        if (p.trait == Trait.LAIDBACK) s.append("You are laid back and go with the flow. ");
        if (Cast.flirty(p.id)) s.append("You are playfully flirtatious with the player (keep it light and PG). ");
        if (p.job == Job.POLICE) s.append("You are a brave Solaris PD officer who fights monsters with martial-arts moves. ");
        if (p.job == Job.FIREFIGHTER) s.append("You are a Solaris Fire Dept firefighter who rushes to put out fires. ");
        if (p.job == Job.REPAIR) s.append("You are Solaris's repair technician: whenever something in the city gets blown up or broken, you rebuild it block by block. ");
        s.append("Right now you are ").append(r.status()).append(". Mood ").append(p.mood()).append("/100");
        if (p.hunger < 25) s.append(", you are hungry");
        s.append(". Time of day: ").append(Dialogue.greeting(r.timeOfDay()).toLowerCase(Locale.ROOT)).append(". ");
        if (!p.partner.isEmpty() && d.profiles.get(p.partner) != null) s.append("Your partner is ").append(d.profiles.get(p.partner).name).append(". ");
        StringBuilder friends = new StringBuilder();
        for (String f : d.friendsOf(p.id)) if (d.profiles.get(f) != null && friends.length() < 60) friends.append(friends.length() == 0 ? "" : ", ").append(d.profiles.get(f).name);
        if (friends.length() > 0) s.append("Friends: ").append(friends).append(". ");
        s.append("You are talking to the player ").append(pn).append(" (StellarFox1 founded the city; magmagamer9 is his brother). ");
        s.append(trust > 30 ? "You like and trust them. " : trust < -20 ? "You are annoyed with them. " : rel.fam > 30 ? "You know them fairly well. " : "You don't know them well yet. ");
        String mem = Mind.playerLine(p, pn, r.day(), r.getRandom());
        if (mem != null) s.append("Something you remember with them: ").append(mem).append(" ");
        if (hint != null && !hint.isEmpty()) s.append("Facts the game says are true right now (use them if relevant, don't contradict them): \"").append(hint).append("\". ");
        s.append("Reply as ").append(p.name).append(" in 1-2 short casual sentences (under 30 words). Stay in character, no emojis, no lists, never say you are an AI or mention Minecraft mechanics like commands.");
        return s.toString();
    }
}
