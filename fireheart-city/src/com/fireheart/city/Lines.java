package com.fireheart.city;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.RandomSource;

/**
 * Picks spoken lines so the city doesn't sound like a broken record: an option said by anyone in the last few
 * minutes is skipped while fresher options exist, and if everything was used recently the stalest one wins.
 */
public final class Lines {
    private Lines() {}

    public static final long FRESH = 9000;
    static long clock;
    private static final Map<String, Long> USED = new LinkedHashMap<>(512, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> e) {
            return size() > 3000;
        }
    };

    public static void tick(long gameTime) {
        clock = gameTime;
    }

    public static String pick(RandomSource r, String... opts) {
        if (opts.length == 0) return "";
        if (opts.length == 1) {
            USED.put(opts[0], clock);
            return opts[0];
        }
        List<String> fresh = new ArrayList<>();
        String stalest = opts[0];
        long oldest = Long.MAX_VALUE;
        for (String o : opts) {
            Long t = USED.get(o);
            if (t == null || clock - t > FRESH || t > clock) fresh.add(o);
            long age = t == null ? Long.MIN_VALUE : t;
            if (age < oldest) { oldest = age; stalest = o; }
        }
        String out = fresh.isEmpty() ? stalest : fresh.get(r.nextInt(fresh.size()));
        USED.put(out, clock);
        return out;
    }

    /** Picks one row of a table of line pairs/sets, preferring rows whose first line wasn't said recently. */
    public static String[] pickRow(RandomSource r, String[][] rows) {
        List<String[]> fresh = new ArrayList<>();
        for (String[] row : rows) {
            Long t = USED.get(row[0]);
            if (t == null || clock - t > FRESH || t > clock) fresh.add(row);
        }
        String[] out = fresh.isEmpty() ? rows[r.nextInt(rows.length)] : fresh.get(r.nextInt(fresh.size()));
        USED.put(out[0], clock);
        return out;
    }

    /** True if this exact text was said anywhere recently. */
    public static boolean recent(String text, long window) {
        Long t = USED.get(text);
        return t != null && clock - t < window && t <= clock;
    }

    public static void mark(String text) {
        USED.put(text, clock);
    }
}
