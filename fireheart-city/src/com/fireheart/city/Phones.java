package com.fireheart.city;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** FirePhone: resident phones, texting, calls, the FireFeed social network, and the player's phone. */
public final class Phones {
    private Phones() {}

    public static final int PRICE = 40;
    public static final String[] COLORS = {"Graphite", "Snow", "Ember", "Ocean", "Mint", "Blush", "Violet", "Gold"};
    public static final int[] RGB = {0x34363C, 0xEDEFF2, 0xE4572E, 0x2E86DE, 0x3DDC97, 0xF4A6C1, 0x8E5CF7, 0xE9C46A};
    public static final String SEP = "\u001F", CSEP = "\u001E";

    /* ================================================================ data */

    public static final class Text {
        public int id;
        public String from = "", to = "", text = "", kind = "", arg = "";
        public long day;
        public int tod;
        public boolean read;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putInt("id", id);
            t.putString("f", from);
            t.putString("t", to);
            t.putString("x", text);
            t.putString("k", kind);
            t.putString("a", arg);
            t.putLong("d", day);
            t.putInt("h", tod);
            t.putBoolean("r", read);
            return t;
        }

        static Text load(CompoundTag t) {
            Text x = new Text();
            x.id = t.getInt("id");
            x.from = t.getString("f");
            x.to = t.getString("t");
            x.text = t.getString("x");
            x.kind = t.getString("k");
            x.arg = t.getString("a");
            x.day = t.getLong("d");
            x.tod = t.getInt("h");
            x.read = t.getBoolean("r");
            return x;
        }
    }

    public static final class Post {
        public int id;
        public String author = "", text = "";
        public long day;
        public int tod;
        public final LinkedHashSet<String> likes = new LinkedHashSet<>();
        public final List<String> comments = new ArrayList<>();

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putInt("id", id);
            t.putString("a", author);
            t.putString("x", text);
            t.putLong("d", day);
            t.putInt("h", tod);
            ListTag l = new ListTag();
            for (String s : likes) l.add(StringTag.valueOf(s));
            t.put("l", l);
            ListTag c = new ListTag();
            for (String s : comments) c.add(StringTag.valueOf(s));
            t.put("c", c);
            return t;
        }

        static Post load(CompoundTag t) {
            Post p = new Post();
            p.id = t.getInt("id");
            p.author = t.getString("a");
            p.text = t.getString("x");
            p.day = t.getLong("d");
            p.tod = t.getInt("h");
            for (Tag x : t.getList("l", Tag.TAG_STRING)) p.likes.add(x.getAsString());
            for (Tag x : t.getList("c", Tag.TAG_STRING)) p.comments.add(x.getAsString());
            return p;
        }

        public boolean commented(String who) {
            for (String c : comments) if (c.startsWith(who + "|")) return true;
            return false;
        }
    }

    public static final class Call {
        public String player;
        public UUID pid;
        public String res;
        public String a, b;
        public int state;
        public boolean incoming, voicemail;
        public long start, last, next;
        public int exchanges;
        public boolean workWarned;
        public String opener = "", invite = "", group = "";
        public final List<String> members = new ArrayList<>();
        public int turn;
        public Dialogue.Script script;
        public int idx;
        public final List<Runnable> effects = new ArrayList<>();
    }

    record React(String resident, int post, long due, String kind) {}

    public static final Map<UUID, Call> PLAYER_CALLS = new HashMap<>();
    public static final List<Call> RES_CALLS = new ArrayList<>();
    static final List<React> REACTS = new ArrayList<>();
    static final Map<String, Deque<String>> CAND = new HashMap<>();
    public static boolean debug;
    public static final java.util.Set<UUID> OPEN_PHONE = new java.util.HashSet<>();
    private static long planDay = -1, callPlayerTick;

    /* ================================================================ helpers */

    public static String colorName(int c) {
        return COLORS[Math.floorMod(c, COLORS.length)];
    }

    public static Resident entity(ServerLevel sl, CityData.Profile p) {
        if (p == null || p.entity == null) return null;
        Entity e = sl.getEntity(p.entity);
        return e instanceof Resident r && r.isAlive() ? r : null;
    }

    static String clean(String s) {
        s = s.replace("|", "/").replace(SEP, " ").replace(CSEP, " ").replace("\n", " ").trim();
        return s.length() > 220 ? s.substring(0, 220) : s;
    }

    static String pick(RandomSource r, String... o) {
        return Lines.pick(r, o);
    }

    static String norm(String raw) {
        return " " + raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9' ]", " ").replaceAll("\\s+", " ") + " ";
    }

    static boolean any(String t, String... keys) {
        for (String k : keys) if (t.contains(k)) return true;
        return false;
    }

    public static boolean playerHasPhone(ServerPlayer pl) {
        Inventory inv = pl.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).getItem() instanceof PhoneItem) return true;
        return false;
    }

    public static int playerPhoneColor(ServerPlayer pl) {
        ItemStack h = pl.getMainHandItem();
        if (h.getItem() instanceof PhoneItem) return PhoneItem.color(h);
        Inventory inv = pl.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).getItem() instanceof PhoneItem) return PhoneItem.color(inv.getItem(i));
        return 0;
    }

    public static String authorName(CityData d, String key) {
        if (key.startsWith("player:")) return key.substring(7);
        CityData.Profile p = d.profiles.get(key);
        return p == null ? key : p.name;
    }

    static int tod(ServerLevel sl) {
        return (int) Math.floorMod(sl.getDayTime(), 24000L);
    }

    public static void toast(ServerPlayer pl, String title, String body) {
        if (pl == null || !playerHasPhone(pl) && !hasWatch(pl) && !Computers.OPEN.containsKey(pl.getUUID())) return;
        if (!Computers.OPEN.containsKey(pl.getUUID()) && (Extras.flat(pl) || Extras.dnd(CityData.get(pl.serverLevel()), pl.getName().getString()) && !OPEN_PHONE.contains(pl.getUUID()))) return;
        PcNet.send(pl, new PcNet.Msg("#toast|" + clean(title) + "|" + clean(body.replaceAll("\\[(pic|scene):[a-z0-9_]*\\] ?", "◉ "))));
    }

    static boolean hasWatch(ServerPlayer pl) {
        return pl.getInventory().contains(new net.minecraft.world.item.ItemStack(FireheartCity.WATCH.get()));
    }

    static void refreshPhone(ServerPlayer pl) {
        if (pl != null && OPEN_PHONE.contains(pl.getUUID())) PcNet.send(pl, Computers.data(pl, BlockPos.ZERO));
    }

    public static void give(ServerLevel sl, CityData d, CityData.Profile p, int color, String how) {
        p.ownsPhone = true;
        p.phoneColor = color;
        if ("phone".equals(p.wantDevice)) p.wantDevice = "";
        long day = Calendar.worldDay(sl);
        p.log(Calendar.day(sl)).note("I got a brand new " + colorName(color) + " SolPhone" + how);
        p.fun = Math.min(100, p.fun + 15);
        d.event(day, "shopping", p.name + " got a new SolPhone", null, p.id);
        queue(p, "Just got my new SolPhone in " + colorName(color) + "! Add me on SolFeed ★ #SolTech");
        d.setDirty();
    }

    /* ================================================================ texts */

    public static Text text(CityData d, String from, String to, String body, String kind, String arg, long day, int tod) {
        Text t = new Text();
        t.id = d.nextText++;
        t.from = from;
        t.to = to;
        t.text = clean(body);
        t.kind = kind;
        t.arg = arg == null ? "" : arg;
        t.day = day;
        t.tod = tod;
        d.texts.add(t);
        while (d.texts.size() > 300) d.texts.remove(0);
        d.setDirty();
        return t;
    }

    public static List<Text> unread(CityData d, String id) {
        List<Text> out = new ArrayList<>();
        for (Text t : d.texts) if (!t.read && t.to.equals(id)) out.add(t);
        return out;
    }

    public static boolean playerWaiting(String resident) {
        for (Computers.Pending p : Computers.PENDING) if (p.resident().equals(resident)) return true;
        return false;
    }

    /** The resident reads their texts: replies go out, invitations are accepted or declined. */
    public static void readTexts(Resident r, CityData.Profile p) {
        ServerLevel sl = (ServerLevel) r.level();
        CityData d = r.data();
        long day = Calendar.worldDay(sl);
        int tod = tod(sl);
        RandomSource rnd = r.getRandom();
        List<Text> un = unread(d, p.id);
        String last = null;
        for (Text t : un) {
            t.read = true;
            CityData.Profile f = d.profiles.get(t.from);
            if (f == null) continue;
            last = f.name;
            CityData.Rel rel = d.rel(p.id, f.id);
            rel.fam = Math.min(100, rel.fam + 1);
            rel.aff = Math.min(100, rel.aff + 1);
            String reply = null, rk = "reply", ra = "";
            switch (t.kind) {
                case "invite" -> {
                    Place pl = Place.get(t.arg);
                    if (pl == null) break;
                    long when = tod < 10800 ? day : day + 1;
                    boolean free = d.plansFor(p.id, when).isEmpty() && d.plansFor(f.id, when).isEmpty();
                    double chance = 0.45 + rel.aff / 150.0 + (rel.friend() ? 0.15 : 0) + (p.partner.equals(f.id) ? 0.3 : 0);
                    if (p.trait == Trait.FRIENDLY || p.trait == Trait.CHEERFUL || p.trait == Trait.TALKATIVE) chance += 0.15;
                    if (p.trait == Trait.SHY || p.trait == Trait.GRUMPY) chance -= 0.15;
                    if (rel.rival) chance = 0.05;
                    if (free && rnd.nextDouble() < chance) {
                        d.addPlan(when, pl.key, "hangout", f.id, p.id);
                        reply = pick(rnd, "Yes! See you at " + pl.label + (when == day ? " later" : " tomorrow") + " ☺", "I'm in! " + Events.sentence(pl.label.replaceFirst("^the ", "")) + (when == day ? " after work" : " tomorrow") + " it is.", "Ooh yes, count me in!");
                        rk = "yes";
                        ra = pl.key;
                        p.mind.remember(p, day, tod, "phone", f.name + " texted me and we made plans to meet at " + pl.label, pl.key, 2, 4, f.id);
                        f.mind.remember(f, day, tod, "phone", "I texted " + p.name + " and we made plans to meet at " + pl.label, pl.key, 2, 4, p.id);
                        d.news(day, p.name + " and " + f.name + " made plans over text to meet at " + pl.label + ".");
                        Resident fr = entity(sl, f);
                        if (fr != null) fr.replan();
                        r.replan();
                    } else reply = !free ? "Aww, I've already got plans. Another time?" : pick(rnd, "Can't today, sorry! Rain check?", "I think I'll stay in tonight. Next time!", "Ugh, I'm so tired. Maybe tomorrow?");
                }
                case "gossip" -> {
                    try {
                        int id = Integer.parseInt(t.arg);
                        CityData.Event e = d.eventById(id);
                        if (e != null) {
                            p.learn(e.id);
                            p.heard(e.id, f.name);
                            reply = Events.reaction(e, p, rnd);
                        }
                    } catch (NumberFormatException ignored) {}
                }
                case "love" -> reply = pick(rnd, "Aww ♥ I love you too!", "You're the sweetest ♥", "Can't wait to see you ♥", "Miss you more!");
                case "checkin" -> {
                    DayLog lg = p.log(r.routineDay());
                    reply = lg.notes.isEmpty() ? pick(rnd, "Pretty quiet so far! You?", "Good! Just taking it easy.") : "Good! " + Events.sentence(DayLog.lower(lg.notes.get(lg.notes.size() - 1))) + ".";
                }
                case "brag" -> reply = pick(rnd, "No way! Challenge accepted.", "Pfft, I'll beat that tonight.", "Okay that's actually impressive.", "Teach me your ways!");
                case "photo" -> reply = pick(rnd, "Wow, so pretty! Jealous!", "Ooh, I need to go there!", "Beautiful! ☀");
                case "video" -> reply = pick(rnd, "HAHAHA I'm crying", "Why is this so funny", "I've watched it five times now lol");
                case "player" -> {
                    CityData.Rel pr = d.playerRel(p.id, t.arg);
                    reply = pr.met ? pick(rnd, "Yes! " + t.arg + " is great.", "Totally agree, " + t.arg + " is awesome.") : "Not yet! I should say hi next time I see them.";
                }
                case "yes" -> {
                    Place pl = Place.get(t.arg);
                    if (pl != null && rnd.nextBoolean()) reply = pick(rnd, "Yay! See you there ☺", "Perfect!");
                }
                case "hello" -> reply = pick(rnd, "Hey! ☺", "Hiii! What's up?", "Hey " + f.name + "!");
                default -> {}
            }
            if (reply != null) text(d, p.id, f.id, reply, rk, ra, day, tod);
        }
        for (int i = 0; i < Computers.PENDING.size(); i++) {
            Computers.Pending q = Computers.PENDING.get(i);
            if (q.resident().equals(p.id)) Computers.PENDING.set(i, new Computers.Pending(q.player(), q.resident(), q.text(), sl.getGameTime() + 30));
        }
        if (last != null && rnd.nextFloat() < 0.4f) r.say(pick(rnd, "Oh, a text from " + last + "!", "Aww, " + last + " texted me.", "Haha, " + last + "...", "Hm, " + last + " again!"), 50);
        d.setDirty();
    }

    /** A resident decides to text someone. */
    static boolean compose(ServerLevel sl, CityData d, Resident r, CityData.Profile p, long day, int tod) {
        RandomSource rnd = r.getRandom();
        List<CityData.Profile> to = new ArrayList<>();
        if (!p.partner.isEmpty()) {
            CityData.Profile q = d.profiles.get(p.partner);
            if (q != null && q.ownsPhone) { to.add(q); to.add(q); }
        }
        for (String fid : d.friendsOf(p.id)) {
            CityData.Profile q = d.profiles.get(fid);
            if (q != null && q.ownsPhone) to.add(q);
        }
        for (CityData.Profile q : d.profiles.values()) {
            CityData.Rel rr = d.peekRel(p.id, q.id);
            if (q != p && q.ownsPhone && rr != null && rr.met && rr.fam >= 15 && !rr.rival && !to.contains(q)) to.add(q);
        }
        if (to.isEmpty()) {
            if (debug) FireheartCity.LOG.info("[Phone] " + p.name + " has nobody to text");
            return false;
        }
        CityData.Profile q = to.get(rnd.nextInt(to.size()));
        String[] msg = message(sl, d, r, p, q, day, tod, rnd);
        if (msg == null) return false;
        if (debug) FireheartCity.LOG.info("[Phone] " + p.name + " texting " + q.name + ": " + msg[0]);
        r.usePhone(1, 60 + rnd.nextInt(50), "texting " + q.name, () -> {
            text(d, p.id, q.id, msg[0], msg[1], msg[2], day, tod(sl));
            p.log(r.routineDay()).note("I texted " + q.name);
            if (p.trait == Trait.TALKATIVE && rnd.nextFloat() < 0.3f) r.say(pick(rnd, "Sent!", "Hehe, " + q.name + " will love this."), 40);
        });
        return true;
    }

    static String[] message(ServerLevel sl, CityData d, Resident r, CityData.Profile p, CityData.Profile q, long day, int tod, RandomSource rnd) {
        CityData.Rel rel = d.rel(p.id, q.id);
        List<String[]> opts = new ArrayList<>();
        if (p.partner.equals(q.id)) {
            boolean date = false;
            for (CityData.Plan pl : d.plansFor(p.id, day)) if (pl.what.equals("date") && pl.who.contains(q.id)) date = true;
            opts.add(new String[]{date ? pick(rnd, "Can't wait for our date later ♥", "Counting down to tonight ♥") : pick(rnd, "Thinking about you ♥", "Miss you! ♥", "Hope your day's going great ♥"), "love", ""});
        }
        if (tod < 10500 && d.plansFor(p.id, day).isEmpty() && d.plansFor(q.id, day).isEmpty() && !rel.rival) {
            boolean isle = p.livesOnIsland() && q.livesOnIsland();
            String[] spots = isle ? Place.ISLE_HANGOUTS : level(sl).isRaining() ? Place.CITY_INDOOR : Place.CITY_HANGOUTS;
            String k = spots[rnd.nextInt(spots.length)];
            Place pl = Place.get(k);
            if (pl != null) opts.add(new String[]{pick(rnd, "Want to hang out at " + pl.label + " after work?", "Hey! " + Events.sentence(pl.label.replaceFirst("^the ", "")) + " later? My treat ☺", "Free tonight? Thinking " + pl.label + "."), "invite", k});
        }
        CityData.Event g = Events.gossipFor(d, p, q, day);
        if (g != null) opts.add(new String[]{pick(rnd, "Did you hear? ", "OMG. ", "Have you heard? ") + Events.sentence(g.text) + "!", "gossip", String.valueOf(g.id)});
        opts.add(new String[]{pick(rnd, "How's your day going?", "Hey! How's work?", "What are you up to?"), "checkin", ""});
        for (String gm : Computers.GAMES) {
            Integer s = d.scores.getOrDefault(gm, Map.of()).get(p.name);
            if (s != null && rnd.nextFloat() < 0.3f) { opts.add(new String[]{"Just got " + s + " on " + Computers.gameName(gm) + ". Beat that ;)", "brag", ""}); break; }
        }
        Place scenic = scenicNear(r);
        if (scenic != null && r.activityName().equals("leisure")) opts.add(new String[]{"[scene:" + scenic.key + "] " + pick(rnd, "Look at this view from " + scenic.label + "! ☀", "Sending you a pic from " + scenic.label + " ☺", "Wish you were here at " + scenic.label + "!"), "photo", scenic.key});
        if (rnd.nextFloat() < 0.35f) opts.add(new String[]{"You HAVE to watch this on SolTube: \"" + Computers.VIDEOS[rnd.nextInt(Computers.VIDEOS.length)] + "\" lol", "video", ""});
        for (ServerPlayer pl : sl.players()) {
            String pn = pl.getName().getString();
            if (p.mind.trustIn(pn) >= 20 && rnd.nextFloat() < 0.4f) opts.add(new String[]{"Have you met " + pn + "? They're really nice ☺", "player", pn});
        }
        if (!rel.met) opts.add(new String[]{"Hi! It's " + p.name + " ☺", "hello", ""});
        return opts.isEmpty() ? null : opts.get(rnd.nextInt(opts.size()));
    }

    static final String[] SCENIC = {"pier", "boardwalk", "marina", "plaza", "park", "clock", "statue", "isle_plaza", "memorial", "gardens", "observatory", "organ", "ferry_isle"};

    static Place scenicNear(Resident r) {
        Place best = null;
        double bd = 12 * 12;
        for (String k : SCENIC) {
            Place p = Place.get(k);
            if (p == null) continue;
            double d = r.distanceToSqr(p.pos.getX() + 0.5, p.pos.getY(), p.pos.getZ() + 0.5);
            if (d < bd) { bd = d; best = p; }
        }
        return best;
    }

    static ServerLevel level(ServerLevel sl) {
        return sl;
    }

    /* ================================================================ FireFeed */

    public static void candidate(CityData.Profile p, long day, String text, int emo, int imp) {
        if (p == null || !p.ownsPhone && !p.ownsPC) return;
        if (imp < 5 && Math.abs(emo) < 2) return;
        String lo = text.toLowerCase(Locale.ROOT);
        if (lo.startsWith("i texted") || lo.startsWith("i messaged") || lo.contains("firefeed") || lo.startsWith("i posted") || lo.contains("letter")) return;
        queue(p, postify(text, emo, p));
    }

    static void queue(CityData.Profile p, String post) {
        Deque<String> q = CAND.computeIfAbsent(p.id, k -> new ArrayDeque<>());
        if (q.contains(post)) return;
        q.addLast(post);
        while (q.size() > 4) q.removeFirst();
    }

    static String postify(String note, int emo, CityData.Profile p) {
        String t = note.trim();
        if (t.endsWith(".")) t = t.substring(0, t.length() - 1);
        int h = Math.floorMod(t.hashCode(), 5);
        String tail;
        if (emo >= 2) tail = new String[]{"! ★", "!! Best day", "! ☺", "! Feeling lucky", "!!"}[h];
        else if (emo >= 1) tail = new String[]{" ☺", "!", ". Nice", " :)", "!"}[h];
        else if (emo <= -2) tail = new String[]{". Ugh ☹", "... not my day", ". Send snacks", ". Sigh", ". Why me"}[h];
        else tail = new String[]{".", "!", " :)", ".", "."}[h];
        String tag = "";
        for (Place pl : Place.ALL.values()) {
            String lab = pl.label.replaceFirst("^the ", "");
            if (t.contains(lab) && !pl.key.startsWith("apt") && !pl.key.startsWith("board")) { tag = " #" + lab.replaceAll("[^A-Za-z]", ""); break; }
        }
        return t + tail + tag;
    }

    public static Post post(CityData d, String author, String text, long day, int tod) {
        Post p = new Post();
        p.id = d.nextPost++;
        p.author = author;
        p.text = clean(text);
        p.day = day;
        p.tod = tod;
        d.feed.add(p);
        while (d.feed.size() > 120) d.feed.remove(0);
        d.setDirty();
        return p;
    }

    public static Post postById(CityData d, int id) {
        for (Post p : d.feed) if (p.id == id) return p;
        return null;
    }

    /** Scrolling FireFeed: post something, like friends' posts, leave a comment. */
    public static void browse(Resident r, CityData.Profile p, boolean pc) {
        ServerLevel sl = (ServerLevel) r.level();
        CityData d = r.data();
        long day = Calendar.worldDay(sl);
        int tod = tod(sl);
        RandomSource rnd = r.getRandom();
        Deque<String> q = CAND.get(p.id);
        boolean posted = false;
        if (q != null && !q.isEmpty() && rnd.nextFloat() < 0.7f) {
            publish(sl, d, p, q.pollFirst(), day, tod);
            posted = true;
        } else if (!p.mind.thought.isEmpty() && !p.mind.thought.startsWith("A quiet day") && rnd.nextFloat() < 0.25f && !postedToday(d, p.id, p.mind.thought, day)) {
            publish(sl, d, p, p.mind.thought, day, tod);
            posted = true;
        } else if (!pc && rnd.nextFloat() < 0.2f && Extras.sceneFor(r) != null && !postedToday(d, p.id, "[scene:", day)) {
            String sc = Extras.sceneFor(r);
            Place spot = Place.get(sc);
            publish(sl, d, p, "[scene:" + sc + "] " + pick(rnd, "Golden hour at ", "Can't get enough of ", "Snapped this at ", "View from ") + (spot == null ? "here" : spot.label.replaceFirst("^the ", "")) + (p.phoneModel >= 2 ? " (shot on SolPhone 2)" : "") + " #" + (spot == null ? "Solaris" : spot.label.replaceFirst("^the ", "").replaceAll("[^A-Za-z]", "")), day, tod);
            posted = true;
        }
        if (rnd.nextFloat() < 0.04f) r.say(pick(rnd, "Ugh, my battery's at 5%...", "Where's my charger...", "Phone's about to die!"), 40);
        int likes = 0;
        Post commented = null;
        for (int i = d.feed.size() - 1; i >= 0 && i >= d.feed.size() - 20; i--) {
            Post f = d.feed.get(i);
            if (f.author.equals(p.id) || day - f.day > 3) continue;
            boolean player = f.author.startsWith("player:");
            boolean close = player ? p.mind.trustIn(f.author.substring(7)) >= 0 : p.partner.equals(f.author) || d.rel(p.id, f.author).friend() || d.rel(p.id, f.author).fam >= 40;
            if (!close) continue;
            if (!f.likes.contains(p.id) && likes < 3 && rnd.nextFloat() < (player ? 0.8f : 0.55f)) {
                like(sl, d, p, f);
                likes++;
            }
            if (commented == null && !f.commented(p.id) && rnd.nextFloat() < (player ? 0.5f : 0.22f)) {
                comment(sl, d, r, p, f);
                commented = f;
            }
        }
        if (!posted && commented == null && rnd.nextFloat() < 0.3f) r.say(pick(rnd, "Haha, look at this...", "Ooh, SolFeed is busy today.", "Everyone's posting about the ferry again.", "Aww, cute."), 40);
        p.fun = Math.min(100, p.fun + 3);
        p.social = Math.min(100, p.social + 3);
        d.setDirty();
    }

    static boolean postedToday(CityData d, String author, String text, long day) {
        for (Post f : d.feed) if (f.author.equals(author) && f.day == day && f.text.startsWith(text.length() > 20 ? text.substring(0, 20) : text)) return true;
        return false;
    }

    static void publish(ServerLevel sl, CityData d, CityData.Profile p, String text, long day, int tod) {
        Post f = post(d, p.id, text, day, tod);
        p.log(Calendar.day(sl)).once("posted:" + f.id);
        for (ServerPlayer pl : sl.players()) {
            String pn = pl.getName().getString();
            if (Extras.follows(d, pn).contains(p.id) || p.mind.trustIn(pn) >= 10 || d.playerRel(p.id, pn).fam >= 30) toast(pl, "SolFeed · " + p.name, f.text.replaceAll("\\[(pic|scene):[^\\]]*\\] ?", "◉ "));
            refreshPhone(pl);
        }
    }

    static void like(ServerLevel sl, CityData d, CityData.Profile p, Post f) {
        f.likes.add(p.id);
        if (f.author.startsWith("player:")) {
            String pn = f.author.substring(7);
            ServerPlayer pl = sl.getServer().getPlayerList().getPlayerByName(pn);
            toast(pl, "SolFeed", p.name + " liked your post");
            refreshPhone(pl);
        } else {
            CityData.Rel rel = d.rel(f.author, p.id);
            rel.aff = Math.min(100, rel.aff + 1);
        }
    }

    static void comment(ServerLevel sl, CityData d, Resident r, CityData.Profile p, Post f) {
        RandomSource rnd = r.getRandom();
        String c;
        if (f.author.startsWith("player:")) {
            String pn = f.author.substring(7);
            ServerPlayer pl = sl.getServer().getPlayerList().getPlayerByName(pn);
            String t = norm(f.text);
            c = pl != null && (f.text.contains("?") || any(t, " hi ", " hello ", " hey ", " how ", " what ", " who ", " anyone ", " news ", " remember ")) ? Chat.reply(pl, r, t, true) : null;
            if (f.text.contains("[pic:")) c = pick(rnd, "Great shot, " + pn + "! ◉", "Wow, where was this taken?", "Frame it! ☺", "The lighting!! ★");
            if (c == null || c.isEmpty()) {
                int emo = Mind.emotionOf(f.text);
                c = emo < 0 ? pick(rnd, "Aww, hope things get better ♥", "Sending hugs, " + pn + "!") : pick(rnd, "Love this! ☺", "Haha, classic " + pn + ".", "So cool!", "Hi " + pn + "! ☺", "Solaris's lucky to have you!");
            }
            p.mind.trust.put(pn, Math.min(100, p.mind.trustIn(pn) + 1));
            toast(pl, "SolFeed · " + p.name + " commented", c);
        } else {
            CityData.Profile a = d.profiles.get(f.author);
            if (a == null) return;
            int emo = Mind.emotionOf(f.text);
            boolean love = p.partner.equals(a.id);
            if (emo <= -1) c = pick(rnd, "Aww, hope tomorrow's better ♥", "Sending hugs!", "Want to grab food later? ♥", "Ugh, sorry " + a.name + "!");
            else if (f.text.contains("high score") || f.text.contains("scored")) c = pick(rnd, "Show-off! ☺", "I'm coming for that record.", "Nice one!");
            else if (f.text.contains("[scene:")) c = pick(rnd, "Gorgeous! ☀", "Ooh I need to go there", "Stunning shot!", "Take me with you next time!");
            else if (f.text.contains("SolPhone")) c = pick(rnd, "Welcome to SolFeed!!", "Finally!! ☺", "Nice colour!");
            else c = love ? pick(rnd, "♥♥♥", "That's my " + (a.trait == Trait.GRUMPY ? "grumpy " : "") + "favourite person ♥", "So proud of you ♥") : pick(rnd, "Congrats!!", "Love this ☺", "Wow!", "Yesss " + a.name + "!", "So jealous!", "Haha amazing");
            CityData.Rel rel = d.rel(a.id, p.id);
            rel.aff = Math.min(100, rel.aff + 2);
        }
        f.comments.add(p.id + "|" + clean(c));
        while (f.comments.size() > 12) f.comments.remove(0);
    }

    /* ================================================================ player actions */

    public static void open(ServerPlayer pl) {
        if (Extras.flat(pl)) {
            pl.displayClientMessage(Component.literal("§cYour SolPhone battery is flat. §7Charge it next to a PC, a TV, at SolTech or by sleeping."), true);
            return;
        }
        OPEN_PHONE.add(pl.getUUID());
        PcNet.send(pl, Computers.data(pl, BlockPos.ZERO));
        Call c = PLAYER_CALLS.get(pl.getUUID());
        if (c != null) {
            String n = c.group.isEmpty() ? Extras.displayName(CityData.get(pl.serverLevel()), pl.getName().getString(), c.res) : Extras.displayName(CityData.get(pl.serverLevel()), pl.getName().getString(), c.group);
            PcNet.send(pl, new PcNet.Msg("#call|" + (c.state == 1 ? "live" : c.incoming ? "incoming" : "ring_out") + "|" + c.res + "|" + n));
        }
    }

    public static boolean action(ServerPlayer pl, PcNet.Act a) {
        ServerLevel sl = pl.serverLevel();
        CityData d = CityData.get(sl);
        String pn = pl.getName().getString();
        String key = "player:" + pn;
        long day = Calendar.worldDay(sl);
        switch (a.kind) {
            case "phone_close" -> OPEN_PHONE.remove(pl.getUUID());
            case "phone_open" -> {
                Tour.onPhoneOpen(pl);
                if (playerHasPhone(pl)) open(pl);
                else pl.displayClientMessage(Component.literal("§7You don't have a SolPhone. Get one at SolTech!"), true);
            }
            case "post" -> {
                String t = clean(a.a);
                if (t.isEmpty()) return true;
                Post f = post(d, key, t, day, tod(sl));
                List<CityData.Profile> fans = new ArrayList<>();
                for (CityData.Profile p : d.profiles.values()) if ((p.ownsPhone || p.ownsPC) && (d.playerRel(p.id, pn).met || p.mind.trustIn(pn) > 0) && p.mind.trustIn(pn) > -20) fans.add(p);
                java.util.Collections.shuffle(fans, new java.util.Random(sl.random.nextLong()));
                long now = sl.getGameTime();
                for (int i = 0; i < Math.min(fans.size(), 2 + sl.random.nextInt(3)); i++) REACTS.add(new React(fans.get(i).id, f.id, now + 100 + sl.random.nextInt(900), i == 0 || sl.random.nextBoolean() ? "comment" : "like"));
                PcNet.send(pl, Computers.data(pl, a.pos));
            }
            case "like" -> {
                Post f = postById(d, parse(a.a));
                if (f == null) return true;
                if (!f.likes.remove(key)) {
                    f.likes.add(key);
                    CityData.Profile au = d.profiles.get(f.author);
                    if (au != null) {
                        au.mind.trust.put(pn, Math.min(100, au.mind.trustIn(pn) + 1));
                        au.fun = Math.min(100, au.fun + 2);
                        if (sl.random.nextFloat() < 0.35f) {
                            Resident r = entity(sl, au);
                            if (r != null) r.say(pick(sl.random, pn + " liked my post!", "Ooh, a like from " + pn + " ☺"), 50);
                        }
                    }
                }
                d.setDirty();
                PcNet.send(pl, Computers.data(pl, a.pos));
            }
            case "comment" -> {
                Post f = postById(d, parse(a.a));
                String t = clean(a.b);
                if (f == null || t.isEmpty()) return true;
                f.comments.add(key + "|" + t);
                if (d.profiles.containsKey(f.author)) REACTS.add(new React(f.author, f.id, sl.getGameTime() + 80 + sl.random.nextInt(300), "reply:" + t));
                d.setDirty();
                PcNet.send(pl, Computers.data(pl, a.pos));
            }
            case "call" -> { if (!PlayerLink.dial(pl, a.a)) dial(pl, d, a.a); }
            case "answer" -> { if (!PlayerLink.answer(pl)) answer(pl, d); }
            case "hangup" -> { if (!PlayerLink.hangup(pl)) hangup(sl, d, PLAYER_CALLS.get(pl.getUUID()), "You hung up.", true); }
            case "say" -> { if (!PlayerLink.say(pl, a.a)) speak(pl, d, a.a); }
            case "buy" -> TechStore.playerBuy(pl, d, a.a, a.b, a.pos);
            default -> { return Extras.action(pl, d, a); }
        }
        return true;
    }

    static int parse(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return -1; }
    }

    /* ================================================================ calls with the player */

    static void send(ServerPlayer pl, String line) {
        if (pl != null) PcNet.send(pl, new PcNet.Msg(line));
    }

    static void dialGroup(ServerPlayer pl, CityData d, String gid) {
        ServerLevel sl = pl.serverLevel();
        String pn = pl.getName().getString();
        Extras.Group g = Extras.group(d, pn, gid);
        if (g == null) return;
        Call c = new Call();
        c.player = pn;
        c.pid = pl.getUUID();
        c.group = gid;
        c.start = sl.getGameTime();
        c.last = c.start;
        for (String m : g.members()) {
            CityData.Profile p = d.profiles.get(m);
            Resident r = entity(sl, p);
            boolean busy = false;
            for (Call o : PLAYER_CALLS.values()) if (m.equals(o.res) || o.members.contains(m)) busy = true;
            if (p == null || p.phoneBroken || r == null || busy || r.isSleeping() || r.activityName().equals("sleep") || r.inCall() || r.convo != null || !r.isFree()) continue;
            c.members.add(m);
            if (c.members.size() >= 4) break;
        }
        if (c.members.isEmpty()) {
            send(pl, "#call|end|" + gid + "|Nobody in " + g.name() + " picked up. Try the group chat!");
            return;
        }
        c.res = c.members.get(0);
        c.next = c.start + 60 + sl.random.nextInt(30);
        PLAYER_CALLS.put(pl.getUUID(), c);
        send(pl, "#call|ring_out|" + gid + "|" + g.name());
        for (String m : c.members) {
            Resident r = entity(sl, d.profiles.get(m));
            if (r != null) r.ringPhone(60);
        }
    }

    static void dial(ServerPlayer pl, CityData d, String id) {
        ServerLevel sl = pl.serverLevel();
        if (PLAYER_CALLS.containsKey(pl.getUUID())) return;
        if (id.startsWith("group:")) {
            dialGroup(pl, d, id);
            return;
        }
        CityData.Profile p = d.profiles.get(id);
        if (p == null) return;
        if (!p.ownsPhone) {
            send(pl, "#call|end|" + id + "|" + p.name + " doesn't have a SolPhone yet. Send them a message instead!");
            return;
        }
        for (Call o : PLAYER_CALLS.values()) if (id.equals(o.res)) {
            send(pl, "#call|end|" + id + "|Busy tone... " + p.name + " is on another call.");
            return;
        }
        Call c = new Call();
        c.player = pl.getName().getString();
        c.pid = pl.getUUID();
        c.res = id;
        c.start = sl.getGameTime();
        c.last = c.start;
        Resident r = entity(sl, p);
        boolean can = r != null && !r.isSleeping() && !r.activityName().equals("sleep") && !r.inCall() && r.convo == null;
        c.voicemail = !can;
        c.next = c.start + (can ? 50 + sl.random.nextInt(40) : 110);
        PLAYER_CALLS.put(pl.getUUID(), c);
        send(pl, "#call|ring_out|" + id + "|" + p.name);
        if (r != null && can) r.ringPhone(60);
    }

    static void answer(ServerPlayer pl, CityData d) {
        Call c = PLAYER_CALLS.get(pl.getUUID());
        if (c == null || !c.incoming || c.state != 0) return;
        ServerLevel sl = pl.serverLevel();
        CityData.Profile p = d.profiles.get(c.res);
        Resident r = entity(sl, p);
        if (r == null || p == null) {
            hangup(sl, d, c, "The call dropped.", false);
            return;
        }
        c.state = 1;
        c.last = sl.getGameTime();
        send(pl, "#call|live|" + c.res + "|" + p.name);
        resLine(sl, pl, r, c, c.opener);
    }

    static void resLine(ServerLevel sl, ServerPlayer pl, Resident r, Call c, String line) {
        if (line == null || line.isEmpty()) return;
        r.sayTo("☎ " + line, Math.min(160, 50 + line.length() * 2));
        String who = "";
        if (!c.group.isEmpty() && r.profile() != null) who = r.profile().name + ": ";
        send(pl, "#call|line|" + (c.group.isEmpty() ? c.res : c.group) + "|" + clean(who + line));
        send(pl, "#callvoice|" + r.getId() + "|" + r.getVoiceId() + "|" + clean(line));
    }

    static void speak(ServerPlayer pl, CityData d, String raw) {
        Call c = PLAYER_CALLS.get(pl.getUUID());
        ServerLevel sl = pl.serverLevel();
        String text = clean(raw);
        if (c == null || text.isEmpty()) return;
        if (!c.group.isEmpty() && c.state == 1 && !c.members.isEmpty()) {
            String low = " " + norm(text) + " ";
            String pick = null;
            for (String m : c.members) {
                CityData.Profile mp = d.profiles.get(m);
                if (mp != null && low.contains(" " + mp.name.toLowerCase(java.util.Locale.ROOT) + " ")) pick = m;
            }
            if (pick == null) pick = c.members.get(Math.floorMod(c.turn++, c.members.size()));
            c.res = pick;
        }
        CityData.Profile p = d.profiles.get(c.res);
        if (c.voicemail) {
            Computers.addInbox(d, Bank.playerKey(c.player), c.res, ">", Calendar.worldDay(sl), "(voicemail) " + text);
            Computers.PENDING.add(new Computers.Pending(c.player, c.res, text, sl.getGameTime() + 600 + sl.random.nextInt(600)));
            hangup(sl, d, c, "Voicemail sent. They'll get back to you.", false);
            return;
        }
        if (c.state != 1 || p == null) return;
        Resident r = entity(sl, p);
        if (r == null) {
            hangup(sl, d, c, "The call dropped.", false);
            return;
        }
        c.last = sl.getGameTime();
        c.exchanges++;
        String t = norm(text);
        String reply;
        boolean end = false;
        String pn = c.player;
        if (!c.invite.isEmpty() && any(t, " yes ", " sure ", " ok ", " okay ", " yeah ", " coming ", " on my way ", " be there ", " yep ")) {
            Place pl2 = Place.get(c.invite);
            reply = pick(sl.random, "Yay! See you soon at " + (pl2 == null ? "the spot" : pl2.label) + "!", "Amazing! I'll wait for you ☺");
            Invites.put(p.id + "|" + pn, new Object[]{pn, c.invite, sl.getGameTime() + 6000});
            c.invite = "";
        } else if (!c.invite.isEmpty() && any(t, " no ", " can't ", " cant ", " busy ", " sorry ", " nah ")) {
            reply = pick(sl.random, "Aww, okay. Next time!", "No worries! Another day.");
            c.invite = "";
        } else if (Intents.meetAsk(t) && (Intents.place(t, p) != null || Intents.time(t, sl.getDayTime()) >= 0) || Meets.with(p.id, pn) != null && any(t, " late ", " omw ", " on my way ", " cancel ", " can't make it ", " cant make it ", " i'm here ", " im here ", " still on ")) {
            reply = Chat.reply(pl, r, t, true);
            if (reply == null || reply.isEmpty()) reply = pick(sl.random, "Sorry, say that again?", "Hm? You broke up a bit there.");
        } else if (any(t, " where are you ", " where r u ", " where you at ", " wya ")) {
            String here = Dialogue.here(r);
            reply = "I'm " + (here.equals("around town") || here.equals("Neon Heights") ? "out and about" : "at " + here) + (r.onIsland() ? ", up on Neon Heights!" : " in the city.");
        } else if (any(t, " come here ", " come to me ", " meet me ", " come over ", " come find me ", " come see me ", " come to where i am ")) {
            if (r.isFree() && !r.activityName().equals("work") && r.onIsland() == (pl.getY() > 150) && r.distanceTo(pl) < 90) {
                r.comeTo(pl, 2400);
                reply = pick(sl.random, "On my way!", "Be right there!", "Coming! Don't move ☺");
                end = true;
            } else if (r.activityName().equals("work")) reply = "I'm at work right now! Come see me at " + p.job.work().label + " instead?";
            else reply = "That's a bit far for me right now - maybe later?";
        } else if (any(t, " bye ", " goodbye ", " see you ", " see ya ", " gotta go ", " talk later ", " ttyl ", " good night ", " goodnight ")) {
            reply = pick(sl.random, "Bye " + pn + "! Talk soon ☺", "Okay, bye bye!", "Thanks for calling!");
            end = true;
        } else {
            reply = Chat.reply(pl, r, t, true);
            if (reply == null || reply.isEmpty()) reply = pick(sl.random, "Haha, yeah!", "Mm-hm!", "Oh really?", "Totally.");
        }
        final String rep = reply;
        final boolean fin = end || c.exchanges >= 10;
        if (c.exchanges == 1) Mind.playerEvent(d, p, pn, Calendar.worldDay(sl), "{P} called me on the phone", 1, 3);
        LATER.add(new Object[]{sl.getGameTime() + 20 + sl.random.nextInt(20), (Runnable) () -> {
            if (PLAYER_CALLS.get(c.pid) != c) return;
            resLine(sl, pl, r, c, rep);
            if (fin) {
                if (!rep.contains("Bye") && !rep.contains("bye") && !rep.contains("On my way") && !rep.contains("right there") && !rep.contains("Coming")) resLine(sl, pl, r, c, pick(sl.random, "Anyway, I'd better go. Talk soon!", "Oops, gotta run - bye!"));
                LATER.add(new Object[]{sl.getGameTime() + 50, (Runnable) () -> hangup(sl, d, c, p.name + " hung up.", false)});
            }
        }});
    }

    /** Phone invitations the player accepted, keyed "resident|player": {player, place, due game time}. */
    static final Map<String, Object[]> Invites = new HashMap<>();
    private static boolean restored;

    /** Copies the in-memory phone queues into CityData so a restart keeps them. */
    static void persist(CityData d) {
        if (!restored) return;
        d.invites.clear();
        Invites.forEach((k, v) -> d.invites.add(k.substring(0, Math.max(0, k.indexOf('|'))) + "|" + v[0] + "|" + v[1] + "|" + v[2]));
        d.pendingReplies.clear();
        for (Computers.Pending q : Computers.PENDING) d.pendingReplies.add(q.player() + "|" + q.resident() + "|" + q.due() + "|" + q.text().replace("|", "/"));
        d.postIdeas.clear();
        CAND.forEach((k, q) -> { for (String s : q) d.postIdeas.add(k + "|" + s); });
        d.reacts.clear();
        for (React x : REACTS) d.reacts.add(x.resident() + "|" + x.post() + "|" + x.due() + "|" + x.kind());
    }

    static void restore(CityData d) {
        restored = true;
        try {
            for (String s : d.invites) {
                String[] p = s.split("\\|", 4);
                if (p.length == 4) Invites.put(p[0] + "|" + p[1], new Object[]{p[1], p[2], Long.parseLong(p[3])});
            }
            for (String s : d.pendingReplies) {
                String[] p = s.split("\\|", 4);
                if (p.length == 4) Computers.PENDING.add(new Computers.Pending(p[0], p[1], p[3], Long.parseLong(p[2])));
            }
            for (String s : d.postIdeas) {
                String[] p = s.split("\\|", 2);
                if (p.length == 2) CAND.computeIfAbsent(p[0], k -> new ArrayDeque<>()).addLast(p[1]);
            }
            for (String s : d.reacts) {
                String[] p = s.split("\\|", 4);
                if (p.length == 4) REACTS.add(new React(p[0], Integer.parseInt(p[1]), Long.parseLong(p[2]), p[3]));
            }
        } catch (RuntimeException e) {
            FireheartCity.LOG.warn("Couldn't restore saved phone queues", e);
        }
    }

    public static void reset() {
        restored = false;
        Invites.clear();
        CAND.clear();
        REACTS.clear();
        Computers.PENDING.clear();
    }
    static final List<Object[]> LATER = new ArrayList<>();

    public static void hangup(ServerLevel sl, CityData d, Call c, String why, boolean byPlayer) {
        if (c == null) return;
        if (c.pid != null) PLAYER_CALLS.remove(c.pid);
        RES_CALLS.remove(c);
        ServerPlayer pl = c.pid == null ? null : sl.getServer().getPlayerList().getPlayer(c.pid);
        if (c.res != null) {
            send(pl, "#call|end|" + c.res + "|" + why);
            CityData.Profile p = d.profiles.get(c.res);
            Resident r = entity(sl, p);
            if (r != null && r.inCall()) {
                if (byPlayer && c.state == 1) r.say(pick(sl.random, "Oh - they hung up. Bye!", "Bye!"), 40);
                r.putPhoneAway();
            }
            if (p != null && c.state == 1) {
                p.log(Calendar.day(sl)).note("I talked to " + c.player + " on the phone");
                p.social = Math.min(100, p.social + 8);
            }
        }
        for (String m : c.members) {
            Resident r = entity(sl, d.profiles.get(m));
            if (r != null && r.inCall()) r.putPhoneAway();
        }
        if (c.a != null) {
            for (String id : new String[]{c.a, c.b}) {
                Resident r = entity(sl, d.profiles.get(id));
                if (r != null && r.inCall()) r.putPhoneAway();
            }
        }
    }

    /** Called when the player shows up after accepting a phone invitation. */
    static void invites(ServerLevel sl, CityData d) {
        long now = sl.getGameTime();
        Iterator<Map.Entry<String, Object[]>> it = Invites.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object[]> e = it.next();
            Object[] v = e.getValue();
            String rid = e.getKey().substring(0, Math.max(0, e.getKey().indexOf('|')));
            if (now > (Long) v[2]) {
                CityData.Profile p = d.profiles.get(rid);
                if (p != null) Mind.playerEvent(d, p, (String) v[0], Calendar.worldDay(sl), "{P} said they'd come meet me but never showed up", -1, 4);
                it.remove();
                continue;
            }
            ServerPlayer pl = sl.getServer().getPlayerList().getPlayerByName((String) v[0]);
            CityData.Profile p = d.profiles.get(rid);
            Resident r = entity(sl, p);
            if (pl == null || r == null || r.distanceTo(pl) > 6) continue;
            r.getLookControl().setLookAt(pl, 30, 30);
            r.gesture(Resident.G_CHEER, 40);
            r.sayTo(pick(sl.random, "You came! ☺", pl.getName().getString() + "! You made it!", "Yay, you're here!"), 70);
            Mind.playerEvent(d, p, pl.getName().getString(), Calendar.worldDay(sl), "{P} came to meet me after I called them", 3, 5);
            it.remove();
        }
    }

    /* ================================================================ calls between residents */

    static boolean callFriend(ServerLevel sl, CityData d, Resident a, CityData.Profile pa, long day) {
        RandomSource rnd = a.getRandom();
        List<CityData.Profile> opts = new ArrayList<>();
        if (!pa.partner.isEmpty()) { CityData.Profile q = d.profiles.get(pa.partner); if (q != null) { opts.add(q); opts.add(q); } }
        for (String fid : d.friendsOf(pa.id)) { CityData.Profile q = d.profiles.get(fid); if (q != null) opts.add(q); }
        opts.removeIf(q -> !q.ownsPhone);
        if (opts.isEmpty()) return false;
        CityData.Profile pb = opts.get(rnd.nextInt(opts.size()));
        Resident b = entity(sl, pb);
        if (b == null || !b.isFree() || b.inCall() || b.isSleeping() || b.activityName().equals("work") || b.distanceTo(a) < 10) return false;
        Call c = new Call();
        c.a = pa.id;
        c.b = pb.id;
        c.start = sl.getGameTime();
        c.next = c.start + 50;
        RES_CALLS.add(c);
        a.usePhone(2, 4000, "calling " + pb.name, null);
        a.say(pick(rnd, "Let me call " + pb.name + "...", "I'll give " + pb.name + " a ring."), 40);
        b.ringPhone(50);
        return true;
    }

    static Dialogue.Script phoneScript(Resident a, Resident b, CityData d, long day, List<Runnable> effects) {
        CityData.Profile pa = a.profile(), pb = b.profile();
        RandomSource r = a.getRandom();
        Dialogue.Script s = new Dialogue.Script();
        s.kind = "phone";
        boolean partners = pa.partner.equals(pb.id);
        s.a(partners ? pick(r, "Hey you ♥", "Hi sweetie!", "Hey, it's me!") : pick(r, "Hey " + pb.name + "! It's " + pa.name + ".", "Hi " + pb.name + "! Got a sec?", "Heyyy, " + pb.name + "!"));
        s.b(pick(r, "Hey! What's up?", "Oh hi! Good timing.", "Hi " + pa.name + "! I was just thinking about you."));
        int topics = 1 + r.nextInt(2);
        try {
            topics -= Mind.talk(s, a, b, pa, pb, d, day, r, effects);
        } catch (Throwable ignored) {}
        CityData.Event g = Events.gossipFor(d, pa, pb, day);
        if (topics > 0 && g != null) {
            s.a(pick(r, "So did you hear? ", "Okay, gossip: ") + Events.sentence(g.text) + "!");
            s.b(Events.reaction(g, pb, r));
            effects.add(() -> { pb.learn(g.id); pb.heard(g.id, pa.name); });
            topics--;
        }
        if (topics > 0) {
            DayLog lg = pb.log(b.routineDay());
            s.a("How was your day?");
            s.b(lg.notes.isEmpty() ? pick(r, "Pretty quiet, honestly.", "Long! But good.") : Events.sentence(DayLog.lower(lg.notes.get(lg.notes.size() - 1))) + ".");
        }
        long tomorrow = day + 1;
        if (d.plansFor(pa.id, tomorrow).isEmpty() && d.plansFor(pb.id, tomorrow).isEmpty() && r.nextFloat() < 0.45f) {
            String[] spots = pa.livesOnIsland() && pb.livesOnIsland() ? Place.ISLE_HANGOUTS : Place.CITY_HANGOUTS;
            String k = spots[r.nextInt(spots.length)];
            Place pl = Place.get(k);
            s.a(partners ? "Date at " + pl.label + " tomorrow? ♥" : "Want to meet at " + pl.label + " tomorrow after work?");
            s.b(pick(r, "Yes! It's a plan.", "Deal!", "I'd love that ☺"));
            effects.add(() -> {
                d.addPlan(tomorrow, k, partners ? "date" : "hangout", pa.id, pb.id);
                d.news(day, pa.name + " and " + pb.name + " made plans over the phone to meet at " + pl.label + ".");
            });
        }
        s.a(partners ? pick(r, "Love you. Bye ♥", "Okay, see you later ♥") : pick(r, "Anyway, I'll let you go. Bye!", "Talk soon!", "Okay, bye bye!"));
        s.b(partners ? "Love you too ♥" : pick(r, "Bye!", "See ya!", "Thanks for calling!"));
        s.onDone = () -> {
            for (Runnable e : effects) e.run();
            CityData.Rel ab = d.rel(pa.id, pb.id), ba = d.rel(pb.id, pa.id);
            for (CityData.Rel x : new CityData.Rel[]{ab, ba}) { x.fam = Math.min(100, x.fam + 3); x.aff = Math.min(100, x.aff + 3); x.chats++; x.lastChatDay = day; }
            pa.social = Math.min(100, pa.social + 12);
            pb.social = Math.min(100, pb.social + 12);
            pa.log(a.routineDay()).note("I called " + pb.name + " on the phone");
            pb.log(b.routineDay()).note(pa.name + " called me on the phone");
            d.setDirty();
        };
        return s;
    }

    static void resCallTick(ServerLevel sl, CityData d, Call c, long now) {
        Resident a = entity(sl, d.profiles.get(c.a)), b = entity(sl, d.profiles.get(c.b));
        if (a == null || b == null || !a.inCall() || now - c.start > 4800) {
            hangup(sl, d, c, "", false);
            return;
        }
        if (now < c.next) return;
        if (c.state == 0) {
            if (!b.isFree() || b.inCall() || b.isSleeping()) {
                a.say(pick(a.getRandom(), "No answer. I'll text instead.", "Voicemail again..."), 50);
                text(d, c.a, c.b, "Tried to call you! Call me back ☺", "hello", "", Calendar.worldDay(sl), tod(sl));
                hangup(sl, d, c, "", false);
                return;
            }
            c.state = 1;
            b.usePhone(2, 4000, "on the phone with " + d.profiles.get(c.a).name, null);
            a.setPhoneWhat("on the phone with " + d.profiles.get(c.b).name);
            c.script = phoneScript(a, b, d, Calendar.worldDay(sl), c.effects);
            c.next = now + 20;
            return;
        }
        if (c.idx >= c.script.lines.size()) {
            c.script.onDone.run();
            hangup(sl, d, c, "", false);
            return;
        }
        Dialogue.Line line = c.script.lines.get(c.idx++);
        Resident who = line.byA() ? a : b;
        (line.byA() ? b : a).hush();
        int dur = 45 + line.text().length() * 2;
        who.sayTo("☎ " + line.text(), dur + 10);
        c.next = now + dur;
    }

    /* ================================================================ residents calling the player */

    static void callPlayer(ServerLevel sl, CityData d, long day) {
        for (ServerPlayer pl : sl.players()) {
            if (PLAYER_CALLS.containsKey(pl.getUUID()) || !playerHasPhone(pl)) continue;
            String pn = pl.getName().getString();
            List<CityData.Profile> opts = new ArrayList<>();
            for (CityData.Profile p : d.profiles.values()) if (p.ownsPhone && p.mind.trustIn(pn) >= 15 && d.playerRel(p.id, pn).met) opts.add(p);
            if (opts.isEmpty()) continue;
            CityData.Profile p = opts.get(sl.random.nextInt(opts.size()));
            Resident r = entity(sl, p);
            if (r == null || !r.isFree() || r.inCall() || r.activityName().equals("work") || r.activityName().equals("sleep") || r.distanceTo(pl) < 12) continue;
            incoming(sl, d, pl, p, r);
            return;
        }
    }

    public static boolean incoming(ServerLevel sl, CityData d, ServerPlayer pl, CityData.Profile p, Resident r) {
        if (PLAYER_CALLS.containsKey(pl.getUUID()) || r == null) return false;
        String pn = pl.getName().getString();
        long day = Calendar.worldDay(sl);
        RandomSource rnd = sl.random;
        Call c = new Call();
        c.player = pn;
        c.pid = pl.getUUID();
        c.res = p.id;
        c.incoming = true;
        c.start = sl.getGameTime();
        c.last = c.start;
        String here = Dialogue.here(r);
        CityData.Plan party = null;
        for (CityData.Plan pl2 : d.plansFor(p.id, day)) if (pl2.what.equals("party") && !pl2.who.isEmpty() && pl2.who.get(0).equals(p.id)) party = pl2;
        Mind.Ep mem = p.mind.best(day + 1, x -> x.involves("@" + pn) && x.emo > 0 && day - x.day <= 3);
        if (party != null && Place.get(party.place) != null) {
            c.opener = "Hi " + pn + "! It's my birthday today - there's a party at " + Place.label(party.place) + " this evening. Will you come?";
            c.invite = party.place;
        } else if (r.activityName().equals("leisure") && p.lastPlace != null && Place.get(p.lastPlace) != null && !p.lastPlace.startsWith("apt") && !p.lastPlace.startsWith("pod") && rnd.nextBoolean()) {
            c.opener = "Hey " + pn + "! I'm at " + here + " right now - come hang out?";
            c.invite = p.lastPlace;
        } else if (mem != null) {
            c.opener = "Hi " + pn + "! I just wanted to say thanks again for " + Calendar.relative(mem.day, day) + " - " + mem.forPlayer(pn) + ". Made my day!";
        } else if (Festival.guest(p.id, day)) {
            c.opener = "Hi " + pn + "! Are you coming to the Festival of the Founder? Everyone's going - there's cake and fireworks!";
        } else {
            c.opener = pick(rnd, "Hey " + pn + "! Just calling to say hi. How are you?", "Hi " + pn + "! Sorry, I was bored - what are you up to?", "Heyyy " + pn + "! Guess who got a SolPhone? ☺");
        }
        PLAYER_CALLS.put(pl.getUUID(), c);
        r.usePhone(2, 2400, "calling " + pn, null);
        send(pl, "#call|incoming|" + p.id + "|" + p.name);
        pl.displayClientMessage(Component.literal("§b☎ §f" + p.name + " is calling you! §7Press §e[P]§7 to answer."), true);
        return true;
    }

    /* ================================================================ ticking */

    public static void callTick(ServerLevel sl, CityData d) {
        PlayerLink.tick(sl.getServer(), sl.getGameTime());
        long now = sl.getGameTime();
        for (int i = 0; i < LATER.size(); i++) {
            Object[] o = LATER.get(i);
            if (now >= (Long) o[0]) {
                LATER.remove(i--);
                try { ((Runnable) o[1]).run(); } catch (Throwable t) { FireheartCity.LOG.error("Phone task failed", t); }
            }
        }
        for (Call c : new ArrayList<>(PLAYER_CALLS.values())) {
            ServerPlayer pl = sl.getServer().getPlayerList().getPlayer(c.pid);
            CityData.Profile p = d.profiles.get(c.res);
            if (pl == null || p == null) {
                hangup(sl, d, c, "The call dropped.", false);
                continue;
            }
            Resident r = entity(sl, p);
            if (c.incoming && c.state == 0) {
                if ((now - c.start) % 20 == 0) pl.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.6f, (now - c.start) % 40 == 0 ? 1.4f : 1.2f);
                if (now - c.start > 300 || r == null || !r.inCall()) {
                    Computers.addInbox(d, Bank.playerKey(c.player), c.res, "~", Calendar.worldDay(sl), "Missed call");
                    Computers.deliver(sl, d, c.player, c.res, "(voicemail) " + c.opener);
                    if (r != null) {
                        r.say(pick(sl.random, "No answer... I'll leave a message.", "Hm, voicemail."), 40);
                    }
                    hangup(sl, d, c, "Missed call from " + p.name + ".", false);
                }
                continue;
            }
            if (c.state == 0) {
                if (now < c.next) continue;
                if (c.voicemail || r == null) {
                    c.voicemail = true;
                    send(pl, "#call|voicemail|" + c.res + "|Hi, you've reached " + p.name + "! I can't pick up right now - leave a message ☺");
                    c.state = 2;
                    c.last = now;
                    continue;
                }
                if (r.convo != null || !r.isFree()) {
                    c.voicemail = true;
                    continue;
                }
                c.state = 1;
                c.last = now;
                if (!c.group.isEmpty()) {
                    Extras.Group g = Extras.group(d, c.player, c.group);
                    send(pl, "#call|live|" + c.group + "|" + (g == null ? "Group call" : g.name()));
                    int k = 0;
                    for (String m : new ArrayList<>(c.members)) {
                        Resident mr = entity(sl, d.profiles.get(m));
                        if (mr == null) { c.members.remove(m); continue; }
                        mr.usePhone(2, 2400, "on a group call with " + c.player, null);
                        String line = pick(sl.random, "Hey everyone! Hi " + c.player + "!", "Oh fun, a group call! Hi!", "Hiii! Who's here?", "Hey " + c.player + "! What's the occasion?");
                        LATER.add(new Object[]{now + 20 + k++ * 40L, (Runnable) () -> resLine(sl, pl, mr, c, line)});
                    }
                    continue;
                }
                r.usePhone(2, 2400, "on the phone with " + c.player, null);
                send(pl, "#call|live|" + c.res + "|" + p.name);
                int trust = p.mind.trustIn(c.player);
                String hi = trust <= -25 ? "...Oh. It's you. What do you want?" : r.activityName().equals("work") ? "Hi " + c.player + "! I'm at work, so I've only got a minute - what's up?" : trust > 30 ? pick(sl.random, "Hey " + c.player + "! So good to hear from you ☺", c.player + "! Hiii! What's up?") : pick(sl.random, "Hello? Oh, hi " + c.player + "!", "Hi " + c.player + "! What's up?");
                resLine(sl, pl, r, c, hi);
                continue;
            }
            if (c.state == 2) {
                if (now - c.last > 1200) hangup(sl, d, c, "Call ended.", false);
                continue;
            }
            if (r == null || !r.inCall() || r.isSleeping()) {
                hangup(sl, d, c, "The call dropped.", false);
                continue;
            }
            r.extendPhone(40);
            for (String m : c.members) {
                Resident mr = entity(sl, d.profiles.get(m));
                if (mr != null && mr.inCall()) mr.extendPhone(40);
            }
            if (!c.workWarned && r.activityName().equals("work") && !r.weekendNow() && now - c.start > 1800) {
                c.workWarned = true;
                resLine(sl, pl, r, c, pick(sl.random, "Sorry " + c.player + ", my boss is giving me a look - I need to get back to work!", "I'd better get back to work before anyone notices. Talk later!", "Customers waiting - gotta go! Call me after work ☺"));
                LATER.add(new Object[]{now + 60, (Runnable) () -> hangup(sl, d, c, p.name + " went back to work.", false)});
                continue;
            }
            if (now - c.last > 1400) {
                resLine(sl, pl, r, c, pick(sl.random, "Hello? ... Guess you're busy. Bye!", "You still there? Okay, talk later!"));
                hangup(sl, d, c, p.name + " hung up.", false);
            }
        }
        for (Call c : new ArrayList<>(RES_CALLS)) resCallTick(sl, d, c, now);
        for (int i = 0; i < REACTS.size(); i++) {
            React x = REACTS.get(i);
            if (now < x.due()) continue;
            REACTS.remove(i--);
            react(sl, d, x);
        }
    }

    static void react(ServerLevel sl, CityData d, React x) {
        CityData.Profile p = d.profiles.get(x.resident());
        Post f = postById(d, x.post());
        if (p == null || f == null) return;
        Resident r = entity(sl, p);
        Runnable act = () -> {
            if (x.kind().startsWith("reply:")) {
                String pn = null;
                for (int i = f.comments.size() - 1; i >= 0; i--) if (f.comments.get(i).startsWith("player:")) { pn = f.comments.get(i).substring(7, f.comments.get(i).indexOf('|')); break; }
                ServerPlayer pl = pn == null ? null : sl.getServer().getPlayerList().getPlayerByName(pn);
                if (pl == null || r == null) return;
                String c = Chat.reply(pl, r, norm(x.kind().substring(6)), true);
                if (c == null || c.isEmpty()) c = pick(sl.random, "Haha thank you ☺", "Aww ☺", "Right?!");
                f.comments.add(p.id + "|@" + pn + " " + clean(c));
                toast(pl, "SolFeed · " + p.name + " replied", c);
                refreshPhone(pl);
                return;
            }
            if (!f.likes.contains(p.id)) like(sl, d, p, f);
            if (x.kind().equals("comment") && !f.commented(p.id) && r != null) comment(sl, d, r, p, f);
            for (ServerPlayer pl : sl.players()) refreshPhone(pl);
        };
        if (r != null && r.isFree() && !r.onPhone() && p.ownsPhone) r.usePhone(1, 50, "scrolling SolFeed", act);
        else act.run();
        d.setDirty();
    }

    /** Every second: residents decide to text, call, browse and buy. */
    public static void tick(ServerLevel sl, CityData d) {
        long now = sl.getGameTime();
        long day = Calendar.worldDay(sl);
        int tod = tod(sl);
        if (!restored) restore(d);
        invites(sl, d);
        if (!d.phonesSeeded) seed(sl, d);
        if (planDay != day && tod > 1000 && tod < 9000) {
            planDay = day;
            TechStore.daily(sl, d, day);
        }
        if ((now / 20) % 30 == 0 && FhcConfig.autoBuild()) TechStore.ensure(sl, d);
        for (CityData.Profile p : d.profiles.values()) {
            if (!p.ownsPhone || p.phoneBroken) continue;
            if (Math.floorMod(p.id.hashCode() + now / 20, 30) != 0) continue;
            Resident r = entity(sl, p);
            if (debug) FireheartCity.LOG.info("[Phone] check " + p.name + " loaded=" + (r != null) + (r == null ? "" : " free=" + r.isFree() + " on=" + r.onPhone() + " pc=" + r.usingPc() + " cd=" + r.phoneCooldown() + " act=" + r.activityName()));
            if (r == null || !r.isFree() || r.onPhone() || r.usingPc() || r.isSleeping() || r.phoneCooldown() > 0) continue;
            String act = r.activityName();
            RandomSource rnd = r.getRandom();
            if (!unread(d, p.id).isEmpty() || playerWaiting(p.id)) {
                r.usePhone(1, 60 + rnd.nextInt(60), "reading texts", () -> readTexts(r, p));
                continue;
            }
            boolean workBusy = act.equals("work") && !r.weekendNow();
            float talk = p.trait == Trait.TALKATIVE || p.trait == Trait.CHEERFUL ? 1.5f : p.trait == Trait.SHY || p.trait == Trait.GRUMPY ? 0.6f : 1f;
            float tr = (float) FhcConfig.text(), cr = (float) FhcConfig.call();
            if (!workBusy && rnd.nextFloat() < 0.12f * talk * tr && compose(sl, d, r, p, day, tod)) continue;
            if ((act.equals("evening") || act.equals("leisure")) && rnd.nextFloat() < 0.035f * talk * cr && callFriend(sl, d, r, p, day)) continue;
            if (!workBusy && rnd.nextFloat() < (r.isSeated() ? 0.22f : 0.1f) * tr * (p.trait == Trait.CURIOUS || p.trait == Trait.ADVENTUROUS ? 1.4f : 1f)) {
                r.usePhone(1, 100 + rnd.nextInt(140), "scrolling SolFeed on their phone", () -> browse(r, p, false));
            }
        }
        if (now - callPlayerTick > 3000 && sl.random.nextFloat() < 0.04f * FhcConfig.call()) {
            callPlayerTick = now;
            callPlayer(sl, d, day);
        }
        unprompted(sl, d, day, tod);
    }

    private static long textDay = -1;

    static void unprompted(ServerLevel sl, CityData d, long day, int tod) {
        if (textDay == day || tod < 3000 || tod > 11000) return;
        textDay = day;
        for (ServerPlayer pl : sl.players()) {
            String pn = pl.getName().getString();
            for (CityData.Profile p : d.profiles.values()) {
                if (!p.ownsPhone || p.phoneBroken || p.mind.trustIn(pn) < 10 || sl.random.nextFloat() > (p.ownsPC ? 0.15f : 0.3f) * FhcConfig.text()) continue;
                String text = pick(sl.random, "Hi " + pn + "! Texting you from my new SolPhone ☺", "Hey " + pn + "! Hope you're having a good day!", "Heyy! Are you around the city today?");
                Mind.Ep mem = p.mind.best(day + 1, x -> x.involves("@" + pn) && x.emo > 0);
                if (mem != null && sl.random.nextBoolean()) text = "Hey " + pn + "! Still thinking about " + Calendar.relative(mem.day, day) + " - " + mem.forPlayer(pn) + " ☺";
                else if (sl.random.nextFloat() < 0.35f) {
                    String sc = Extras.SCENES[sl.random.nextInt(Extras.SCENES.length)];
                    Place spot = Place.get(sc);
                    text = "[scene:" + sc + "] " + pick(sl.random, "Look at this view", "Took this earlier", "Wish you were here") + (spot == null ? "!" : " - " + spot.label + "!");
                }
                Computers.deliver(sl, d, pn, p.id, text);
            }
        }
    }

    static void seed(ServerLevel sl, CityData d) {
        d.phonesSeeded = true;
        String[][] early = {{"ava", "4"}, {"rex", "6"}, {"nova", "3"}, {"leo", "2"}, {"zara", "5"}};
        long day = Calendar.worldDay(sl);
        for (String[] e : early) {
            CityData.Profile p = d.profiles.get(e[0]);
            if (p == null || p.ownsPhone) continue;
            p.ownsPhone = true;
            p.phoneColor = Integer.parseInt(e[1]);
        }
        Post f = post(d, "ava", "SolTech is open next to the Library road! SolPhones in 8 colours + Solaris PCs. Come say hi ☺ #SolTech", day, tod(sl));
        f.likes.add("rex");
        f.likes.add("nova");
        f.comments.add("rex|Already got mine. The Snake high score is MINE.");
        post(d, "leo", "Diner special today: double cheeseburger. You know you want it ☺ #DieselDiner", day, tod(sl));
        post(d, "nova", "Stargazing from the Observatory is even better with a SolPhone camera ★ #NeonHeights", day, tod(sl));
        d.news(day, "SolTech opened its doors, selling SolPhones and Solaris PCs.");
        d.setDirty();
    }

    /* ================================================================ client data */

    public static void fill(ServerPlayer pl, CityData d, PcNet.Data o) {
        ServerLevel sl = pl.serverLevel();
        String pn = pl.getName().getString();
        String key = "player:" + pn;
        long day = Calendar.worldDay(sl);
        for (int i = d.feed.size() - 1; i >= 0 && o.feed.size() < 40; i--) {
            Post f = d.feed.get(i);
            StringBuilder cm = new StringBuilder();
            for (String c : f.comments) {
                int bar = c.indexOf('|');
                if (bar < 0) continue;
                if (cm.length() > 0) cm.append(CSEP);
                cm.append(authorName(d, c.substring(0, bar))).append(": ").append(c.substring(bar + 1));
            }
            o.feed.add(f.id + SEP + authorName(d, f.author) + SEP + f.author + SEP + Calendar.relative(f.day, day) + SEP + f.likes.size() + SEP + (f.likes.contains(key) ? 1 : 0) + SEP + f.text + SEP + cm);
        }
        for (CityData.Profile p : d.profiles.values()) {
            Resident r = entity(sl, p);
            double x = r == null ? (p.homePlace() == null ? 0 : p.homePlace().pos.getX()) : r.getX();
            double z = r == null ? (p.homePlace() == null ? 0 : p.homePlace().pos.getZ()) : r.getZ();
            boolean isle = r == null ? p.livesOnIsland() : r.onIsland();
            o.map.add(p.id + "|" + p.name + "|" + (int) x + "|" + (int) z + "|" + (isle ? 1 : 0) + "|" + clean(p.doing.isEmpty() ? "somewhere in the city" : p.doing));
        }
        o.px = (int) pl.getX();
        o.pz = (int) pl.getZ();
        o.pisle = pl.getY() > 150;
        Ferry f = Ferry.find(sl);
        if (f == null) o.ferry = "The Sky Ferry isn't running right now.";
        else if (f.grounded()) o.ferry = "Grounded by the storm. Service resumes when it clears.";
        else if (f.dockedAt() == Ferry.CITY) o.ferry = "Boarding at the city pad (by the pier).";
        else if (f.dockedAt() == Ferry.ISLE) o.ferry = "Boarding at the Neon Heights terminal.";
        else o.ferry = "In the air to " + (f.targetSide() == Ferry.ISLE ? "Neon Heights" : "the city") + " - about " + Math.max(1, f.etaSeconds()) + "s.";
        o.ferry += "|" + f(sl, f, Ferry.CITY) + "|" + f(sl, f, Ferry.ISLE) + "|" + (f != null && f.hasPilot() ? "Jet" : "autopilot");
        var wd = sl.getServer().getWorldData().overworldData();
        String now = sl.isThundering() ? "Thunderstorm" : sl.isRaining() ? "Rain" : "Clear";
        int mins = Math.max(1, wd.getRainTime() / 1200);
        String fc = sl.isRaining() ? "Clearing in about " + mins + " min" : wd.getRainTime() < 24000 ? "Rain likely in about " + mins + " min" : "Dry for the rest of the day";
        long t = Math.floorMod(sl.getDayTime(), 24000L);
        float base = sl.getBiome(pl.blockPosition()).value().getBaseTemperature();
        int temp = Math.round(base * 25f) + (sl.isRaining() ? -5 : 0) + (sl.isThundering() ? -2 : 0) + (t > 3000 && t < 10000 ? 3 : t > 13000 && t < 23000 ? -6 : 0) - (pl.getY() > 150 ? 3 : 0);
        o.weather = now + "|" + temp + "|" + fc + "|" + Calendar.name(day);
        for (String s : TechStore.catalog(d, day)) o.catalog.add(s);
        o.phoneColor = playerPhoneColor(pl);
        o.inStore = pl.blockPosition().closerThan(TechStore.CENTER, 14);
    }

    static int f(ServerLevel sl, Ferry f, int side) {
        return f == null ? 0 : f.waitingAt(sl, side);
    }
}
