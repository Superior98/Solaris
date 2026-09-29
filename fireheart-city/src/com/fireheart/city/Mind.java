package com.fireheart.city;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * A resident's long-term mind: episodic memories that fade unless they matter or get talked about,
 * feelings about places learned from experience, trust in each player built from what they did,
 * and a nightly reflection that turns the day into a thought and an intention for tomorrow.
 */
public final class Mind {
    public static final int CAP = 48;
    public static final Set<String> NEW_PLACES = Set.of("gardens", "observatory", "ferry_isle", "ferry_city");

    public static final class Ep {
        public long day;
        public int time;
        public String kind = "life";
        public String text = "";
        public String place = "";
        public final List<String> who = new ArrayList<>();
        public int emo;
        public int imp = 3;
        public int recalls;
        public long recallDay = -100;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putLong("d", day);
            t.putInt("t", time);
            t.putString("k", kind);
            t.putString("x", text);
            t.putString("p", place);
            ListTag w = new ListTag();
            for (String s : who) w.add(StringTag.valueOf(s));
            t.put("w", w);
            t.putInt("e", emo);
            t.putInt("i", imp);
            t.putInt("r", recalls);
            t.putLong("rd", recallDay);
            return t;
        }

        static Ep load(CompoundTag t) {
            Ep e = new Ep();
            e.day = t.getLong("d");
            e.time = t.getInt("t");
            e.kind = t.getString("k");
            e.text = t.getString("x");
            e.place = t.getString("p");
            for (Tag w : t.getList("w", Tag.TAG_STRING)) e.who.add(w.getAsString());
            e.emo = t.getInt("e");
            e.imp = t.getInt("i");
            e.recalls = t.getInt("r");
            e.recallDay = t.getLong("rd");
            return e;
        }

        public boolean involves(String id) {
            return who.contains(id);
        }

        public String forPlayer(String player) {
            return text.replace("{P}", "you");
        }

        public String told(String player) {
            return text.replace("{P}", player == null ? "the founder" : player);
        }
    }

    public final List<Ep> eps = new ArrayList<>();
    public final Map<String, Integer> places = new LinkedHashMap<>();
    public final Map<String, Long> lastVisit = new LinkedHashMap<>();
    public final Map<String, Integer> trust = new LinkedHashMap<>();
    public final Map<String, Long> seenPlayer = new LinkedHashMap<>();
    public final Map<String, String> seenWhere = new LinkedHashMap<>();
    public String intent = "";
    public String intentKind = "";
    public String intentTarget = "";
    public long intentDay = -1;
    public String thought = "";
    public long reflectDay = -1;
    public long greetedIntentDay = -1;
    public long lastDiscover = -1;
    public final Map<String, Long> gifted = new LinkedHashMap<>();
    public final Set<String> milestones = new LinkedHashSet<>();
    public String dream = "";
    public long dreamDay = -1;

    public static Mind of(CityData.Profile p) {
        return p.mind;
    }

    public static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public Ep remember(CityData.Profile p, long day, int time, String kind, String text, String place, int emo, int imp, String... who) {
        if (text == null || text.isBlank()) return null;
        for (Ep e : eps) if (e.day == day && e.text.equals(text)) return e;
        Ep e = new Ep();
        e.day = day;
        e.time = time;
        e.kind = kind;
        e.text = text;
        e.place = place == null ? "" : place;
        for (String w : who) if (w != null && !w.isEmpty() && !w.equals(p.id) && !e.who.contains(w)) e.who.add(w);
        e.emo = clamp(emo, -5, 5);
        e.imp = clamp(imp, 1, 10);
        eps.add(e);
        if (!e.place.isEmpty() && e.emo != 0) places.put(e.place, clamp(places.getOrDefault(e.place, 0) + e.emo * 4, -100, 100));
        forget(day);
        return e;
    }

    public double retention(Ep e, long today) {
        double age = Math.max(0, today - e.day);
        return e.imp * 3.0 + Math.abs(e.emo) * 2.0 + Math.min(e.recalls, 4) * 2.5 - age * (e.imp >= 7 ? 0.4 : 1.1);
    }

    public void forget(long today) {
        while (eps.size() > CAP) {
            Ep worst = null;
            double ws = Double.MAX_VALUE;
            for (Ep e : eps) {
                double s = retention(e, today);
                if (s < ws) { ws = s; worst = e; }
            }
            eps.remove(worst);
        }
    }

    public void nightlyFade(long today) {
        eps.removeIf(e -> retention(e, today) < -2 && today - e.day > 2);
        places.replaceAll((k, v) -> v > 0 ? v - 1 : v < 0 ? v + 1 : 0);
    }

    public boolean remembers(String withId, long day) {
        for (Ep e : eps) if (e.involves(withId) && Math.abs(e.day - day) <= 0) return true;
        return false;
    }

    public int moodBias(long today) {
        int s = 0, n = 0;
        for (Ep e : eps) if (today - e.day <= 1) { s += e.emo; n++; }
        return n == 0 ? 0 : clamp(s * 3, -12, 12);
    }

    public int trustIn(String player) {
        return trust.getOrDefault(player, 0);
    }

    public void visit(String place, long day) {
        if (place == null || place.isEmpty()) return;
        Long lv = lastVisit.get(place);
        if ((lv == null || day - lv > 6) && !place.startsWith("apt") && !place.startsWith("pod")) lastDiscover = day;
        lastVisit.put(place, day);
    }

    public Ep best(long today, java.util.function.Predicate<Ep> filter) {
        Ep best = null;
        double bs = -1e9;
        for (Ep e : eps) {
            if (!filter.test(e)) continue;
            double s = retention(e, today) + (e.recallDay >= today - 1 ? -20 : 0);
            if (s > bs) { bs = s; best = e; }
        }
        return best;
    }

    /* ---------- notes from the day log become memories ---------- */

    private static final String[] GOOD = {"won", "love", "best", "fun", "party", "danced", "date", "thank", "gift", "bought", "promot", "made up", "delicious", "birthday",
            "together", "jackpot", "key to the city", "mayor", "amazing", "paid off", "fireworks", "tip", "cheer", "beautiful", "view", "flew", "ferry", "great", "treat", "picnic", "festival", "honoured", "blessed", "high score", "tour"};
    private static final String[] BAD = {"argu", "fell out", "broke up", "missed", "late fee", "lost", "refused", "rain", "storm", "hungry", "starving", "couldn't", "sad",
            "angry", "stood me up", "overdue", "hit me", "sold out", "monster", "zombie", "skeleton", "creeper", "spider", "never got", "boo"};
    private static final String[] BIG = {"won", "jackpot", "mayor", "broke up", "together", "partner", "key to the city", "birthday", "married", "promot", "fell out", "elected"};
    private static final String[] MID = {"date", "party", "bought", "loan", "favour", "argu", "made up", "fireworks", "lottery", "ferry", "trip", "letter", "vote", "speech", "danced", "festival", "founder", "pc"};

    private static final Set<String> WHOLE = Set.of("won", "tip", "fun", "sad", "boo", "rain", "date", "lost", "best", "view");

    static boolean has(String t, String w) {
        int i = t.indexOf(w);
        while (i >= 0) {
            boolean startOk = i == 0 || !Character.isLetter(t.charAt(i - 1));
            int end = i + w.length();
            boolean endOk = !WHOLE.contains(w) || end >= t.length() || !Character.isLetter(t.charAt(end));
            if (startOk && endOk) return true;
            i = t.indexOf(w, i + 1);
        }
        return false;
    }

    public static int emotionOf(String text) {
        String t = text.toLowerCase(Locale.ROOT);
        int e = 0;
        for (String g : GOOD) if (has(t, g)) e++;
        for (String b : BAD) if (has(t, b)) e -= 2;
        return clamp(e, -4, 4);
    }

    public static int importanceOf(String text) {
        String t = text.toLowerCase(Locale.ROOT);
        int i = 3;
        for (String b : BIG) if (has(t, b)) { i += 4; break; }
        for (String m : MID) if (has(t, m)) { i += 2; break; }
        return clamp(i, 1, 10);
    }

    public static void fromNote(CityData.Profile p, long day, String text) {
        if (p == null || text == null || text.isEmpty() || p.mind == null) return;
        List<String> who = new ArrayList<>();
        for (Cast.Member m : Cast.ALL) if (!m.id().equals(p.id) && java.util.regex.Pattern.compile("\\b" + m.name() + "\\b").matcher(text).find()) who.add(m.id());
        int emo = emotionOf(text);
        int imp = importanceOf(text) + (who.isEmpty() ? 0 : 1);
        String place = p.lastPlace;
        if (text.contains("Sky Ferry")) place = "ferry";
        else {
            int best = 0;
            for (Place pl : Place.ALL.values()) {
                String lab = pl.label.replaceFirst("^the ", "");
                if (lab.length() > best && text.contains(lab)) { best = lab.length(); place = pl.key; }
            }
        }
        p.mind.remember(p, day, 0, "diary", text, place, emo, imp, who.toArray(new String[0]));
        Phones.candidate(p, day, text, emo, imp);
    }

    /* ---------- the player ---------- */

    public static void playerEvent(CityData d, CityData.Profile p, String player, long day, String text, int emo, int imp) {
        if (p == null || player == null) return;
        Mind m = p.mind;
        m.remember(p, day, 0, "player", text, p.lastPlace, emo, imp, "@" + player);
        m.trust.put(player, clamp(m.trustIn(player) + emo * 4, -100, 100));
        milestone(d, p, player, day);
        d.setDirty();
    }

    public static void milestone(CityData d, CityData.Profile p, String player, long day) {
        Mind m = p.mind;
        int t = m.trustIn(player);
        String key = "@" + player;
        if (t >= 25 && m.milestones.add("friend:" + player)) {
            m.remember(p, day, 0, "player", "{P} and I became real friends", p.lastPlace, 3, 7, key);
            Post.send(d, p.id, Bank.playerKey(player), "Dear " + player + ",\n\nI just wanted to say - I'm really glad you're around. You've become a real friend to me.\n\nDrop by " + p.job.work().label + " any time!\n\n" + p.name, "friend", day);
            d.news(day, p.name + " and " + player + " have become good friends.");
        }
        if (t >= 45 && m.milestones.add("best:" + player)) {
            m.remember(p, day, 0, "player", "{P} became my best friend", p.lastPlace, 4, 9, key);
            Post.Letter l = Post.send(d, p.id, Bank.playerKey(player), "Dear " + player + ",\n\nI've been thinking... you're honestly my best friend in Solaris. I made you something - I hope you like it.\n\nYour best friend,\n" + p.name, "bestfriend", day);
            l.gift = Economy.products(p.job).get(0);
            l.giftCount = 3;
            d.event(day, "social", p.name + " calls " + player + " their best friend", null, p.id);
        }
    }

    public static String bond(CityData.Profile p, String player) {
        if (p.mind.milestones.contains("best:" + player)) return "§d❤ best friend";
        if (p.mind.milestones.contains("friend:" + player)) return "§a★ friend";
        return "";
    }

    /** Morning post: residents who like the player sometimes mail a small parcel with a note. */
    public static void friendMail(ServerLevel sl, CityData d) {
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (tod < 1000 || tod > 3000) return;
        long day = Calendar.worldDay(sl);
        for (CityData.Profile p : d.profiles.values()) {
            Mind m = p.mind;
            for (Map.Entry<String, Integer> e : new ArrayList<>(m.trust.entrySet())) {
                String player = e.getKey();
                if (e.getValue() < 30) continue;
                long last = m.gifted.getOrDefault(player, -100L);
                if (day - last < 3) continue;
                m.gifted.put(player, day);
                if (Math.floorMod(p.id.hashCode() + day * 17, 100) >= 35) continue;
                String item = sl.random.nextBoolean() ? Memory.favourite(p) : Economy.products(p.job).get(sl.random.nextInt(Economy.products(p.job).size()));
                Ep mem = m.best(day + 1, x -> x.involves("@" + player) && x.emo > 0);
                String memLine = mem != null ? "I keep thinking about " + (mem.day == day - 1 ? "yesterday" : Calendar.name(mem.day)) + ", when " + mem.forPlayer(player) + ". " : "";
                Post.Letter l = Post.send(d, p.id, Bank.playerKey(player), "Dear " + player + ",\n\n" + memLine + "I saw this and thought of you - " + Economy.label(item) + ". Enjoy!\n\n" + (p.trait == Trait.SHY ? "(Don't make a fuss about it.)\n\n" : "") + p.name, "parcel", day);
                l.gift = item;
                l.giftCount = 1 + sl.random.nextInt(2);
                p.log(Calendar.day(sl)).note("I mailed " + player + " a little parcel");
            }
        }
        d.setDirty();
    }

    public static void sawPlayer(CityData.Profile p, String player, long day, String place) {
        Mind m = p.mind;
        Long last = m.seenPlayer.get(player);
        if (last != null && last == day) return;
        m.seenPlayer.put(player, day);
        if (place != null && !place.isEmpty()) m.seenWhere.put(player, place);
    }

    public static String playerLine(CityData.Profile p, String player, long today, RandomSource r) {
        Mind m = p.mind;
        String key = "@" + player;
        Ep e = m.best(today, x -> x.involves(key) && today - x.day >= 1 && x.imp >= 4);
        if (e != null && r.nextFloat() < 0.55f) {
            e.recalls++;
            e.recallDay = today;
            String when = Calendar.relative(e.day, today);
            String what = e.forPlayer(player);
            String lead = Events.sentence(when.startsWith("on ") ? when.substring(3) : when) + ", " + what;
            if (e.emo > 0) return pick(r, lead + ". That really meant a lot.", "I still think about it - " + (lead.startsWith("Yesterday") || lead.startsWith("Today") || lead.startsWith("Last") ? DayLog.lower(lead) : lead) + "!", lead + ". I won't forget that, " + player + ".");
            if (e.emo < 0) return pick(r, "I haven't forgotten. " + lead + ".", lead + ". Not cool, " + player + ".");
            return "I remember - " + (lead.startsWith("Yesterday") || lead.startsWith("Today") || lead.startsWith("Last") ? DayLog.lower(lead) : lead) + ".";
        }
        Long seen = m.seenPlayer.get(player);
        String where = m.seenWhere.get(player);
        if (seen != null && seen == today - 1 && where != null && Place.get(where) != null && r.nextFloat() < 0.6f) {
            return "I saw you at " + Place.label(where) + " yesterday. What were you up to?";
        }
        if (seen != null && today - seen >= 3 && m.trustIn(player) > 10) return "Where have you been? I haven't seen you in " + (today - seen) + " days!";
        return null;
    }

    public static String opinionOf(CityData.Profile p, String player) {
        int t = p.mind.trustIn(player);
        if (t >= 40) return player + " is honestly one of the kindest people in this city.";
        if (t >= 15) return player + "? I like them. They've helped me out before.";
        if (t <= -30) return "Between you and me, I don't trust " + player + " one bit.";
        if (t <= -10) return player + " can be a bit rough, if you ask me.";
        return null;
    }

    /* ---------- deciding where to go ---------- */

    public static final class Choice {
        public final String key;
        public final String why;
        public final String reason;
        public final double score;

        Choice(String key, String why, String reason, double score) {
            this.key = key;
            this.why = why;
            this.reason = reason;
            this.score = score;
        }
    }

    public static Choice decide(Resident r, CityData d, CityData.Profile p, List<Choice> options, RandomSource rng) {
        if (options.isEmpty()) return null;
        double temp = switch (p.trait) {
            case ADVENTUROUS, DREAMY -> 1.2;
            case GRUMPY, SHY -> 0.6;
            default -> 0.85;
        };
        double max = options.stream().mapToDouble(c -> c.score).max().orElse(0);
        double sum = 0;
        double[] w = new double[options.size()];
        for (int i = 0; i < w.length; i++) {
            w[i] = Math.exp((options.get(i).score - max) / temp);
            sum += w[i];
        }
        double roll = rng.nextDouble() * sum;
        for (int i = 0; i < w.length; i++) {
            roll -= w[i];
            if (roll <= 0) return options.get(i);
        }
        return options.get(options.size() - 1);
    }

    public static double placeScore(Resident r, CityData d, CityData.Profile p, String key, long day, StringBuilder why) {
        Mind m = p.mind;
        double s = 0;
        int love = m.places.getOrDefault(key, 0);
        s += love / 35.0;
        if (love >= 20) why.append("I love it there");
        else if (love <= -20) why.append("bad memories there");
        Long lv = m.lastVisit.get(key);
        boolean fresh = NEW_PLACES.contains(key);
        if (lv == null && fresh) {
            s += p.trait == Trait.CURIOUS || p.trait == Trait.ADVENTUROUS ? 1.1 : 0.5;
            if (why.length() == 0) why.append("I've never been");
        } else if (lv == null || day - lv > 6) {
            if (p.trait == Trait.CURIOUS || p.trait == Trait.ADVENTUROUS) { s += 0.6; if (why.length() == 0) why.append("haven't been in ages"); }
            else s += 0.15;
        } else if (lv >= day - 1) s -= 0.35;
        if (m.intentKind.equals("place") && m.intentTarget.equals(key) && m.intentDay == day) { s += 2.5; why.setLength(0); why.append("I planned to go today"); }
        if (m.intentKind.equals("explore") && m.intentDay == day && (lv == null || day - lv > 6)) { s += 1.8; why.setLength(0); why.append("trying somewhere different"); }
        int friendsThere = 0, rivalsThere = 0;
        String friendName = null;
        for (CityData.Profile q : d.profiles.values()) {
            if (q == p || q.entity == null) continue;
            if (!(((ServerLevel) r.level()).getEntity(q.entity) instanceof Resident qr)) continue;
            String qk = qr.currentLeisureKey();
            if (!key.equals(qk)) continue;
            CityData.Rel rel = d.peekRel(p.id, q.id);
            if (rel == null) continue;
            if (rel.rival) rivalsThere++;
            else if (rel.friend() || q.id.equals(p.partner)) { friendsThere++; friendName = q.name; }
        }
        if (friendsThere > 0) {
            s += (p.social < 45 ? 1.4 : 0.8) + 0.3 * (friendsThere - 1);
            why.setLength(0);
            why.append(friendName).append(friendsThere > 1 ? " and friends are" : " is").append(" there");
        }
        if (rivalsThere > 0) s -= 1.2 * rivalsThere;
        return s;
    }

    /* ---------- nightly reflection ---------- */

    public static void nightly(ServerLevel sl, CityData d) {
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (tod < 17600 || tod > 23000) return;
        long day = Calendar.dayOf(sl.getDayTime());
        for (CityData.Profile p : d.profiles.values()) {
            Mind m = p.mind;
            if (m.reflectDay == day) continue;
            m.reflectDay = day;
            reflect(sl, d, p, day);
        }
        d.setDirty();
    }

    public static void forceReflect(ServerLevel sl, CityData d) {
        long day = Calendar.dayOf(sl.getDayTime());
        for (CityData.Profile p : d.profiles.values()) {
            p.mind.reflectDay = day;
            reflect(sl, d, p, day);
            p.mind.intentDay = day;
        }
        d.setDirty();
    }

    private static void reflect(ServerLevel sl, CityData d, CityData.Profile p, long day) {
        Mind m = p.mind;
        RandomSource r = sl.random;
        if (m.intentDay == day && !m.intentKind.isEmpty()) {
            boolean done = fulfilled(d, p, day);
            if (done) m.remember(p, day, 18000, "intent", "I managed to " + m.intent, "", 2, 4);
            else if (!m.intentKind.equals("rest")) m.remember(p, day, 18000, "intent", "I never got round to " + gerund(m.intent), "", -1, 3);
        }
        Ep top = m.best(day, e -> e.day == day);
        if (top != null) {
            String mood = top.emo >= 2 ? "What a day - " : top.emo <= -2 ? "Rough day. " : "";
            String tx = top.told(null);
            m.thought = mood + (tx.startsWith("I ") || mood.isEmpty() ? tx : DayLog.lower(tx)) + ".";
            top.recalls++;
        } else m.thought = "A quiet day. Nothing much happened.";
        m.nightlyFade(day);
        long tomorrow = day + 1;
        m.dream = "";
        if (r.nextFloat() < 0.5f) {
            Ep seed = null;
            List<Ep> pool = new ArrayList<>();
            for (Ep e : m.eps) if (e.imp >= 4) pool.add(e);
            if (!pool.isEmpty()) seed = pool.get(r.nextInt(pool.size()));
            m.dream = dreamOf(p, seed, d, r);
            m.dreamDay = tomorrow;
        }
        m.intent = "";
        m.intentKind = "";
        m.intentTarget = "";
        m.intentDay = tomorrow;
        List<String[]> opts = new ArrayList<>();
        for (String fid : d.friendsOf(p.id)) {
            CityData.Profile f = d.profiles.get(fid);
            CityData.Rel rel = d.rel(p.id, fid);
            if (f == null) continue;
            long gap = rel.lastChatDay < 0 ? 99 : day - rel.lastChatDay;
            if (gap >= 2 && f.livesOnIsland() == p.livesOnIsland()) opts.add(new String[]{"friend", fid, "catch up with " + f.name, String.valueOf(Math.min(gap, 6) + (p.social < 40 ? 3 : 0))});
        }
        if (p.trait != Trait.GRUMPY) for (String rid : d.rivalsOf(p.id)) {
            CityData.Profile q = d.profiles.get(rid);
            if (q != null) opts.add(new String[]{"makeup", rid, "make up with " + q.name, String.valueOf(p.trait == Trait.FRIENDLY || p.trait == Trait.CHEERFUL ? 5 : 2)});
        }
        String fav = null;
        int favLove = 25;
        for (Map.Entry<String, Integer> e : m.places.entrySet()) {
            Place pl = Place.get(e.getKey());
            if (pl == null || e.getKey().startsWith("apt") || e.getKey().startsWith("pod")) continue;
            Long lv = m.lastVisit.get(e.getKey());
            if (e.getValue() > favLove && (lv == null || day - lv >= 2)) { favLove = e.getValue(); fav = e.getKey(); }
        }
        if (fav != null) opts.add(new String[]{"place", fav, "go back to " + Place.label(fav), String.valueOf(3 + favLove / 20)});
        if (p.trait == Trait.CURIOUS || p.trait == Trait.ADVENTUROUS || p.trait == Trait.DREAMY) {
            int unseen = 0;
            for (String k : allHangouts()) { Long v = m.lastVisit.get(k); if (v == null || day - v > 6) unseen++; }
            if (unseen > 0) opts.add(new String[]{"explore", "", "go somewhere different for a change", String.valueOf(2 + unseen)});
        }
        if (!p.goal.isEmpty() && p.goalCost > 0 && Bank.savings(d, p.id) >= p.goalCost * 0.6) opts.add(new String[]{"save", "", "save every coin for " + p.goal, "3"});
        for (String player : d.playerNames(p.id)) {
            int t = m.trustIn(player);
            Long seen = m.seenPlayer.get(player);
            if (t >= 12 && (seen == null || day - seen >= 1)) opts.add(new String[]{"player", player, "find " + player + " and say hi", String.valueOf(2 + t / 15)});
        }
        if (p.mood() < 35) opts.add(new String[]{"rest", "", "take it easy and have a quiet day", "4"});
        if (opts.isEmpty()) return;
        opts.sort(Comparator.comparingInt((String[] o) -> -Integer.parseInt(o[3])));
        int pickIdx = r.nextFloat() < 0.65f ? 0 : r.nextInt(Math.min(3, opts.size()));
        String[] o = opts.get(pickIdx);
        m.intentKind = o[0];
        m.intentTarget = o[1];
        m.intent = o[2];
        if (o[0].equals("friend")) {
            CityData.Profile f = d.profiles.get(o[1]);
            if (f != null && d.plansFor(f.id, tomorrow).isEmpty() && d.plansFor(p.id, tomorrow).isEmpty()) {
                String spot = sharedSpot(p, f, r);
                d.addPlan(tomorrow, spot, "hangout", p.id, f.id);
            }
        }
        if (o[0].equals("makeup")) d.rel(p.id, o[1]).facts.put("makeup", String.valueOf(tomorrow));
    }

    static String dreamOf(CityData.Profile p, Ep seed, CityData d, RandomSource r) {
        String[] twists = {"but everyone was a cat", "and the Sky Ferry was made of cake", "except the whole city was underwater", "and I could fly without the ferry",
                "and Remy was playing a giant piano on the clock tower", "but my teeth kept falling out", "and the island floated away into space", "and Hugo was chasing me for a library fine",
                "and it was snowing gold coins", "and the neon signs were singing"};
        String twist = twists[r.nextInt(twists.length)];
        if (seed == null) return "I dreamt I was lost in the Sky Gardens " + twist + ".";
        String t = seed.told(null).replaceAll("[.!]+$", "");
        if (t.startsWith("I ")) t = t.substring(2);
        return "I dreamt I " + DayLog.lower(t) + " again, " + twist + "!";
    }

    public String dreamToday(long day) {
        return dreamDay == day && !dream.isEmpty() ? dream : null;
    }

    private static String sharedSpot(CityData.Profile p, CityData.Profile f, RandomSource r) {
        Mind a = p.mind, b = f.mind;
        String best = null;
        int bs = Integer.MIN_VALUE;
        for (String k : p.livesOnIsland() ? Place.ISLE_HANGOUTS : Place.CITY_HANGOUTS) {
            int s = a.places.getOrDefault(k, 0) + b.places.getOrDefault(k, 0) + r.nextInt(12);
            if (s > bs) { bs = s; best = k; }
        }
        return best == null ? "plaza" : best;
    }

    private static List<String> allHangouts() {
        Set<String> s = new LinkedHashSet<>();
        for (String k : Place.CITY_HANGOUTS) s.add(k);
        for (String k : Place.ISLE_HANGOUTS) s.add(k);
        return new ArrayList<>(s);
    }

    private static boolean fulfilled(CityData d, CityData.Profile p, long day) {
        Mind m = p.mind;
        return switch (m.intentKind) {
            case "friend" -> d.rel(p.id, m.intentTarget).lastChatDay == day;
            case "makeup" -> !d.rel(p.id, m.intentTarget).rival;
            case "place" -> {
                Long v = m.lastVisit.get(m.intentTarget);
                yield v != null && v == day;
            }
            case "explore" -> m.lastDiscover == day;
            case "save" -> p.today.day == day && p.today.spent <= 4;
            case "player" -> {
                Long s = m.seenPlayer.get(m.intentTarget);
                yield s != null && s == day;
            }
            default -> true;
        };
    }

    static String gerund(String intent) {
        String[] w = intent.split(" ", 2);
        String v = w[0];
        String g = switch (v) {
            case "catch" -> "catching";
            case "make" -> "making";
            case "go" -> "going";
            case "explore" -> "exploring";
            case "save" -> "saving";
            case "find" -> "finding";
            case "take" -> "taking";
            default -> v + "ing";
        };
        return w.length > 1 ? g + " " + w[1] : g;
    }

    /* ---------- conversation ---------- */

    public static int talk(Dialogue.Script s, Resident a, Resident b, CityData.Profile pa, CityData.Profile pb, CityData data, long day, RandomSource r, List<Runnable> effects) {
        Mind ma = pa.mind, mb = pb.mind;
        CityData.Rel ab = data.rel(pa.id, pb.id);
        if (!ab.talkedRecently("recall", day, 1) && r.nextFloat() < 0.55f) {
            Ep shared = ma.best(day, e -> e.involves(pb.id) && day - e.day >= 1 && e.imp >= 4);
            if (shared != null) {
                Ep sh = shared;
                String when = Calendar.relative(sh.day, day);
                String gist = gist(sh.text, pa, pb);
                s.a("Remember when " + gist + " " + when + "?" + (sh.emo > 0 ? " That was so good." : sh.emo < 0 ? " I still feel bad about that." : ""), Resident.G_THINK);
                boolean theyRemember = mb.remembers(pa.id, sh.day);
                if (theyRemember) {
                    s.b(sh.emo >= 0 ? pick(r, "How could I forget? Best " + Calendar.name(sh.day) + " in ages.", "Ha! Of course I remember.", "I was just thinking about that!") : pick(r, "Yeah... let's not do that again.", "I remember. Water under the bridge."), sh.emo >= 0 ? Resident.G_CHEER : 0);
                    effects.add(() -> {
                        sh.recalls++;
                        sh.recallDay = day;
                        for (Ep e : mb.eps) if (e.involves(pa.id) && e.day == sh.day) { e.recalls++; e.recallDay = day; }
                        ab.aff += sh.emo >= 0 ? 3 : 1;
                        data.rel(pb.id, pa.id).aff += sh.emo >= 0 ? 3 : 1;
                    });
                } else {
                    s.b(pick(r, "Hmm... honestly, I don't remember that at all.", "Did we? My memory's terrible, sorry!", "Wait, was I there? I can't picture it."), Resident.G_THINK);
                    s.a(pick(r, "Seriously? Wow. Okay.", "You're hopeless! Ha.", "I'll take that as a no."));
                    effects.add(() -> {
                        sh.recalls++;
                        sh.recallDay = day;
                        mb.remember(pb, day, 0, "told", pa.name + " reminded me about the time " + gist.replace("we ", pa.name + " and I ").replace("you", "I"), "", 0, 3, pa.id);
                        if (pa.trait == Trait.GRUMPY || pa.trait == Trait.SHY) ab.aff -= 2;
                    });
                }
                effects.add(() -> { ab.talked.put("recall", day); data.rel(pb.id, pa.id).talked.put("recall", day); });
                return 1;
            }
        }
        if (!ab.talkedRecently("story", day, 1) && r.nextFloat() < 0.45f) {
            Ep big = ma.best(day, e -> !e.involves(pb.id) && e.imp >= 5 && day - e.day <= 4 && !e.kind.equals("told") && !e.text.contains("{P}"));
            if (big != null) {
                Ep bg = big;
                String when = bg.day == day ? "today" : Calendar.relative(bg.day, day);
                String body = DayLog.lower(bg.text);
                s.a((bg.emo >= 2 ? pick(r, "Guess what happened " + when + "? ", "You won't believe it - ") : bg.emo <= -2 ? pick(r, "Ugh, " + when + " was rough. ", "Can I vent for a second? ") : "So " + when + ", ") + body + ".", bg.emo >= 0 ? Resident.G_CHEER : Resident.G_THINK);
                if (bg.emo >= 2) s.b(pick(r, "No way! That's amazing, " + pa.name + "!", "Lucky you! Tell me everything.", "That's brilliant!"), Resident.G_CHEER);
                else if (bg.emo <= -2) s.b(pick(r, "Oh no, I'm sorry. That sounds awful.", "That's rough. Want to grab a bite later?", "Hang in there, " + pa.name + "."), Resident.G_THINK);
                else s.b(pick(r, "Oh yeah? Sounds like a busy day.", "Huh, nice.", "Ha, that's so you."));
                effects.add(() -> {
                    bg.recalls++;
                    bg.recallDay = day;
                    mb.remember(pb, day, 0, "told", pa.name + " told me " + (bg.text.startsWith("I ") ? pa.name + " " + bg.text.substring(2) : DayLog.lower(bg.text)), "", Math.max(-1, Math.min(1, bg.emo)), Math.max(2, bg.imp - 2), pa.id);
                    ab.talked.put("story", day);
                    data.rel(pb.id, pa.id).aff += bg.emo <= -2 ? 3 : 1;
                });
                return 1;
            }
        }
        String dr = ma.dreamToday(day);
        if (dr != null && !ab.talkedRecently("dream", day, 1) && r.nextFloat() < 0.4f) {
            s.a("I had the strangest dream last night. " + dr, Resident.G_THINK);
            s.b(pick(r, "Ha! What does that even mean?", "That's so weird. I dreamt I was late for work. Again.", "You need to stop eating cheese before bed!", "Okay, that's amazing."), Resident.G_CHEER);
            effects.add(() -> ab.talked.put("dream", day));
            return 1;
        }
        if (!ma.intent.isEmpty() && ma.intentDay == day && !ab.talkedRecently("intent", day, 1) && r.nextFloat() < 0.35f) {
            boolean aboutB = ma.intentTarget.equals(pb.id);
            if (aboutB && ma.intentKind.equals("friend")) {
                boolean phone = "phone".equals(s.kind);
                s.a(phone ? "I've been meaning to call you all day! It's been ages." : "I was actually hoping I'd bump into you today! It's been ages.", Resident.G_WAVE);
                s.b(phone ? "Aww, I'm so glad you did! We should talk more often." : "Aww, me too! We should do this more often.", Resident.G_CHEER);
                effects.add(() -> { ab.aff += 4; data.rel(pb.id, pa.id).aff += 4; });
            } else {
                s.a("My plan for today? " + Events.sentence(ma.intent) + ".", Resident.G_THINK);
                s.b(switch (ma.intentKind) {
                    case "save" -> "Good for you. I'm terrible at saving.";
                    case "rest" -> "Honestly, you deserve it.";
                    case "explore" -> "Ooh, tell me if you find somewhere good!";
                    case "player" -> "phone".equals(s.kind) ? "Text them! I bet they'd love to hear from you." : "They're around somewhere, I saw them earlier maybe.";
                    case "makeup" -> "That's big of you. Good luck.";
                    default -> pick(r, "Sounds like a plan!", "Nice, enjoy!");
                });
            }
            effects.add(() -> ab.talked.put("intent", day));
            return 1;
        }
        if (!ab.talkedRecently("player", day, 2) && r.nextFloat() < 0.35f) {
            for (String player : data.playerNames(pa.id)) {
                String op = opinionOf(pa, player);
                if (op == null) continue;
                Ep why = ma.best(day, e -> e.involves("@" + player) && Math.abs(e.emo) >= 1);
                s.a(op + (why != null ? " " + Events.sentence(why.told(player)) + (why.emo > 0 ? "!" : ".") : ""), Resident.G_THINK);
                int ta = pa.mind.trustIn(player), tb = mb.trustIn(player);
                if (tb >= 15 && ta >= 0) s.b("Totally agree. " + player + " is great.");
                else if (tb <= -15 && ta > 0) s.b("Really? That's not my experience with " + player + " at all.");
                else if (!data.playerRel(pb.id, player).met) s.b("I've never actually met " + player + ". Maybe I should!");
                else s.b(pick(r, "Huh, good to know.", "I'll keep that in mind."));
                final String pl = player;
                effects.add(() -> {
                    ab.talked.put("player", day);
                    int shift = Integer.signum(ta) * 3;
                    mb.trust.put(pl, clamp(mb.trustIn(pl) + shift, -100, 100));
                });
                return 1;
            }
        }
        return 0;
    }

    private static String gist(String text, CityData.Profile me, CityData.Profile you) {
        String t = text.replaceAll("[.!]+$", "");
        String with = " with " + you.name;
        if (t.startsWith("I ") && t.contains(with)) t = "we " + t.substring(2).replace(with, "");
        else if (t.startsWith(you.name + " came over")) t = "you came over" + t.substring((you.name + " came over").length());
        else {
            t = t.replace(you.name, "you");
            if (t.startsWith("I ")) t = "I " + t.substring(2);
            else t = DayLog.lower(t);
        }
        return t;
    }

    private static String pick(RandomSource r, String... o) {
        return Lines.pick(r, o);
    }

    /* ---------- player-facing views ---------- */

    public static String feeling(int trust) {
        if (trust >= 45) return "§a❤ adores you";
        if (trust >= 20) return "§atrusts you";
        if (trust >= 6) return "§2likes you";
        if (trust <= -35) return "§4can't stand you";
        if (trust <= -12) return "§cis wary of you";
        return "§7neutral";
    }

    public static int friendsList(net.minecraft.server.level.ServerPlayer pl) {
        CityData d = CityData.get(pl.serverLevel());
        String pn = pl.getName().getString();
        long today = Calendar.worldDay(pl.serverLevel());
        pl.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6=== What Solaris thinks of " + pn + " ===§7 (/friends <name> for details)"));
        List<CityData.Profile> ps = new ArrayList<>(d.profiles.values());
        ps.sort(Comparator.comparingInt((CityData.Profile p) -> -p.mind.trustIn(pn)));
        for (CityData.Profile p : ps) {
            CityData.Rel r = d.playerRel(p.id, pn);
            if (!r.met) continue;
            Ep e = p.mind.best(today + 5, x -> x.involves("@" + pn));
            String bd = bond(p, pn);
            pl.sendSystemMessage(net.minecraft.network.chat.Component.literal("§e" + p.name + " §7" + feeling(p.mind.trustIn(pn)) + (bd.isEmpty() ? "" : " " + bd) + (e != null ? " §8- remembers: " + e.forPlayer(pn) + " (" + Calendar.relative(e.day, today) + ")" : "")));
        }
        return 1;
    }

    public static int friendDetail(net.minecraft.server.level.ServerPlayer pl, String name) {
        CityData d = CityData.get(pl.serverLevel());
        CityData.Profile p = d.byName(name);
        String pn = pl.getName().getString();
        if (p == null) {
            pl.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cNobody called " + name + " lives here."));
            return 0;
        }
        long today = Calendar.worldDay(pl.serverLevel());
        Mind m = p.mind;
        if (!d.playerRel(p.id, pn).met) {
            pl.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7You haven't met " + p.name + " yet. Go say hi!"));
            return 1;
        }
        pl.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6" + p.name + "§7, " + p.jobTitle() + " - " + feeling(m.trustIn(pn))));
        if (!m.intent.isEmpty() && m.intentDay == today) pl.sendSystemMessage(net.minecraft.network.chat.Component.literal("§ePlan for today: §f" + m.intent));
        if (!m.thought.isEmpty()) pl.sendSystemMessage(net.minecraft.network.chat.Component.literal("§eLast night " + p.name + " thought: §f\"" + m.thought + "\""));
        if (m.dreamToday(today) != null) pl.sendSystemMessage(net.minecraft.network.chat.Component.literal("§eAnd dreamt: §f\"" + m.dreamToday(today) + "\""));
        int n = 0;
        for (Ep e : m.eps) {
            if (!e.involves("@" + pn)) continue;
            pl.sendSystemMessage(net.minecraft.network.chat.Component.literal((e.emo > 0 ? "§a" : e.emo < 0 ? "§c" : "§7") + "• " + Calendar.relative(e.day, today) + ": " + e.forPlayer(pn)));
            n++;
        }
        if (n == 0) pl.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7" + p.name + " doesn't have any special memories of you yet."));
        String fav = null;
        int best = 15;
        for (Map.Entry<String, Integer> e : m.places.entrySet()) if (e.getValue() > best && Place.get(e.getKey()) != null && !e.getKey().startsWith("apt") && !e.getKey().startsWith("pod") && !e.getKey().equals(p.home) && !e.getKey().equals(p.job.workKey)) { best = e.getValue(); fav = e.getKey(); }
        if (fav != null) pl.sendSystemMessage(net.minecraft.network.chat.Component.literal("§eFavourite place lately: §f" + Place.label(fav)));
        return 1;
    }

    /* ---------- save / load ---------- */

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        ListTag l = new ListTag();
        for (Ep e : eps) l.add(e.save());
        t.put("eps", l);
        CompoundTag pl = new CompoundTag();
        places.forEach(pl::putInt);
        t.put("places", pl);
        CompoundTag lv = new CompoundTag();
        lastVisit.forEach(lv::putLong);
        t.put("visits", lv);
        CompoundTag tr = new CompoundTag();
        trust.forEach(tr::putInt);
        t.put("trust", tr);
        CompoundTag sp = new CompoundTag();
        seenPlayer.forEach(sp::putLong);
        t.put("seen", sp);
        CompoundTag sw = new CompoundTag();
        seenWhere.forEach(sw::putString);
        t.put("seenWhere", sw);
        t.putString("intent", intent);
        t.putString("intentKind", intentKind);
        t.putString("intentTarget", intentTarget);
        t.putLong("intentDay", intentDay);
        t.putString("thought", thought);
        t.putLong("reflectDay", reflectDay);
        t.putLong("discover", lastDiscover);
        CompoundTag gf = new CompoundTag();
        gifted.forEach(gf::putLong);
        t.put("gifted", gf);
        ListTag ms = new ListTag();
        for (String x : milestones) ms.add(StringTag.valueOf(x));
        t.put("milestones", ms);
        t.putString("dream", dream);
        t.putLong("dreamDay", dreamDay);
        t.putLong("greeted", greetedIntentDay);
        return t;
    }

    public static Mind load(CompoundTag t) {
        Mind m = new Mind();
        for (Tag e : t.getList("eps", Tag.TAG_COMPOUND)) m.eps.add(Ep.load((CompoundTag) e));
        CompoundTag pl = t.getCompound("places");
        for (String k : pl.getAllKeys()) m.places.put(k, pl.getInt(k));
        CompoundTag lv = t.getCompound("visits");
        for (String k : lv.getAllKeys()) m.lastVisit.put(k, lv.getLong(k));
        CompoundTag tr = t.getCompound("trust");
        for (String k : tr.getAllKeys()) m.trust.put(k, tr.getInt(k));
        CompoundTag sp = t.getCompound("seen");
        for (String k : sp.getAllKeys()) m.seenPlayer.put(k, sp.getLong(k));
        CompoundTag sw = t.getCompound("seenWhere");
        for (String k : sw.getAllKeys()) m.seenWhere.put(k, sw.getString(k));
        m.intent = t.getString("intent");
        m.intentKind = t.getString("intentKind");
        m.intentTarget = t.getString("intentTarget");
        m.intentDay = t.contains("intentDay") ? t.getLong("intentDay") : -1;
        m.thought = t.getString("thought");
        m.reflectDay = t.contains("reflectDay") ? t.getLong("reflectDay") : -1;
        m.lastDiscover = t.contains("discover") ? t.getLong("discover") : -1;
        CompoundTag gf = t.getCompound("gifted");
        for (String k : gf.getAllKeys()) m.gifted.put(k, gf.getLong(k));
        for (Tag x : t.getList("milestones", Tag.TAG_STRING)) m.milestones.add(x.getAsString());
        m.dream = t.getString("dream");
        m.dreamDay = t.contains("dreamDay") ? t.getLong("dreamDay") : -1;
        m.greetedIntentDay = t.contains("greeted") ? t.getLong("greeted") : -1;
        return m;
    }

    public List<String> describe(CityData.Profile p, long today, int max) {
        List<String> out = new ArrayList<>();
        out.add("§6" + p.name + "'s mind §7(" + eps.size() + " memories)");
        if (!thought.isEmpty()) out.add("§eLast night: §f" + thought);
        if (!intent.isEmpty()) out.add("§ePlan for " + Calendar.name(intentDay) + ": §f" + intent);
        List<Ep> sorted = new ArrayList<>(eps);
        sorted.sort(Comparator.comparingDouble((Ep e) -> -retention(e, today)));
        int n = 0;
        for (Ep e : sorted) {
            if (n++ >= max) break;
            String c = e.emo > 0 ? "§a" : e.emo < 0 ? "§c" : "§7";
            out.add(c + "• " + Calendar.relative(e.day, today) + ": " + e.told(null) + " §8[imp " + e.imp + ", feel " + e.emo + ", recalled " + e.recalls + "]");
        }
        StringBuilder pl = new StringBuilder();
        places.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue()).limit(5).forEach(e -> {
            Place pp = Place.get(e.getKey());
            if (pp != null && e.getValue() != 0) pl.append(pl.length() == 0 ? "" : ", ").append(pp.label.replaceFirst("^the ", "")).append(" ").append(e.getValue() > 0 ? "+" : "").append(e.getValue());
        });
        if (pl.length() > 0) out.add("§eFeelings about places: §f" + pl);
        if (!trust.isEmpty()) {
            StringBuilder tb = new StringBuilder();
            trust.forEach((k, v) -> tb.append(tb.length() == 0 ? "" : ", ").append(k).append(" ").append(v > 0 ? "+" : "").append(v));
            out.add("§eTrust: §f" + tb);
        }
        return out;
    }
}
