package com.fireheart.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class CityData extends SavedData {
    public static final String NAME = "fireheartcity";

    public static class Profile {
        public String id;
        public String name;
        public Job job;
        public Trait trait;
        public String home;
        public int skin;
        public UUID entity;
        public String lastPlace = "";
        public String doing = "";
        public int coins = 25;
        public int hunger = 80;
        public int social = 60;
        public int fun = 60;
        public int rep;
        public int tier;
        public int xp;
        public int level = 1;
        public long partnerSince = -1;
        public long lastBirthday = -1;
        public long lastAnniv = -1;
        public UUID pet;
        public int fish;
        public Mind mind = new Mind();
        public boolean ownsPC;
        public BlockPos pcPos;
        public boolean ownsPhone;
        public int phoneColor;
        public String wantDevice = "";
        public long wantDay = -1;
        public boolean phoneBroken, hasTv, headphones;
        public int phoneModel = 1;
        public long pcDeliverDay = -1;
        public static final String[] LEVELS = {"Junior ", "", "Senior ", "Head "};

        public String jobTitle() {
            if (job == Job.CLERK && "ava".equals(id) && CityData.managerAva) return "SolTech Store Manager";
            return LEVELS[Math.max(0, Math.min(3, level))] + job.title;
        }

        public int birthdayIndex() {
            return Math.floorMod(id.hashCode() * 31 + 7, 16);
        }
        public String partner = "";
        public DayLog today = new DayLog();
        public DayLog yesterday = new DayLog();
        public final Map<Integer, String> heardFrom = new LinkedHashMap<>();
        public long paidDay = -1;
        public long workDay = -1;
        public int workTicks;
        public long rentDay = -1;
        public String goal = "";
        public int goalCost;
        public int goalsDone;
        public String lastGoal = "";
        public long lastGoalDay = -100;
        public long bankDay = -1;
        public long cashDay = -1;
        public int loanWish;
        public String loanWhy = "";
        public int lastInterest;
        public long interestDay = -100;
        public long loanPaidDay = -100;
        public int missedTotal;

        public DayLog log(long day) {
            if (today.day != day) {
                if (today.day >= 0) yesterday = today;
                today = new DayLog();
                today.day = day;
            }
            today.owner = this;
            return today;
        }

        public void heard(int eventId, String from) {
            heardFrom.put(eventId, from);
            while (heardFrom.size() > 40) heardFrom.remove(heardFrom.keySet().iterator().next());
        }
        public final Map<String, Integer> inv = new LinkedHashMap<>();
        public final LinkedHashSet<Integer> known = new LinkedHashSet<>();

        public int count(String item) {
            return inv.getOrDefault(item, 0);
        }

        public void add(String item, int n) {
            int v = count(item) + n;
            if (v <= 0) inv.remove(item); else inv.put(item, v);
        }

        public void learn(int eventId) {
            known.add(eventId);
            while (known.size() > 60) known.remove(known.iterator().next());
        }

        public int mood() {
            int base = (hunger + social + fun) / 3;
            return Math.max(0, Math.min(100, base + (mind == null ? 0 : mind.moodBias(today.day))));
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putString("id", id);
            t.putString("name", name);
            t.putString("job", job.name());
            t.putString("trait", trait.name());
            t.putString("home", home);
            t.putInt("skin", skin);
            if (entity != null) t.putUUID("entity", entity);
            t.putString("last", lastPlace);
            t.putString("doing", doing);
            t.putInt("coins", coins);
            t.putInt("hunger", hunger);
            t.putInt("social", social);
            t.putInt("fun", fun);
            t.putInt("rep", rep);
            t.putInt("tier", tier);
            t.putString("partner", partner);
            t.putInt("xp", xp);
            t.putInt("level", level);
            t.putLong("partnerSince", partnerSince);
            t.putLong("lastBirthday", lastBirthday);
            t.putLong("lastAnniv", lastAnniv);
            if (pet != null) t.putUUID("pet", pet);
            t.putInt("fish", fish);
            CompoundTag iv = new CompoundTag();
            inv.forEach(iv::putInt);
            t.put("inv", iv);
            t.putIntArray("known", known.stream().mapToInt(Integer::intValue).toArray());
            t.put("today", today.save());
            t.put("yday", yesterday.save());
            CompoundTag hf = new CompoundTag();
            heardFrom.forEach((k, v) -> hf.putString(String.valueOf(k), v));
            t.put("heardFrom", hf);
            t.putLong("paidDay", paidDay);
            t.putLong("workDay", workDay);
            t.putInt("workTicks", workTicks);
            t.putLong("rentDay", rentDay);
            t.putString("goal", goal);
            t.putInt("goalCost", goalCost);
            t.putInt("goalsDone", goalsDone);
            t.putString("lastGoal", lastGoal);
            t.putLong("lastGoalDay", lastGoalDay);
            t.putLong("bankDay", bankDay);
            t.putLong("cashDay", cashDay);
            t.putInt("loanWish", loanWish);
            t.putString("loanWhy", loanWhy);
            t.putInt("lastInterest", lastInterest);
            t.putLong("interestDay", interestDay);
            t.putLong("loanPaidDay", loanPaidDay);
            t.putInt("missedTotal", missedTotal);
            t.put("mind", mind.save());
            t.putBoolean("ownsPC", ownsPC);
            if (pcPos != null) t.putLong("pcPos", pcPos.asLong());
            t.putBoolean("ownsPhone", ownsPhone);
            t.putInt("phoneColor", phoneColor);
            t.putString("wantDevice", wantDevice);
            t.putLong("wantDay", wantDay);
            t.putBoolean("phoneBroken", phoneBroken);
            t.putBoolean("hasTv", hasTv);
            t.putBoolean("headphones", headphones);
            t.putInt("phoneModel", phoneModel);
            t.putLong("pcDeliverDay", pcDeliverDay);
            return t;
        }

        static Profile load(CompoundTag t) {
            Profile p = new Profile();
            p.id = t.getString("id");
            p.name = t.getString("name");
            p.job = Job.byName(t.getString("job"));
            p.trait = Trait.valueOf(t.getString("trait"));
            p.home = t.getString("home");
            p.skin = t.getInt("skin");
            if (t.hasUUID("entity")) p.entity = t.getUUID("entity");
            p.lastPlace = t.getString("last");
            p.doing = t.getString("doing");
            if (t.contains("coins")) {
                p.coins = t.getInt("coins");
                p.hunger = t.getInt("hunger");
                p.social = t.getInt("social");
                p.fun = t.getInt("fun");
            }
            p.rep = t.getInt("rep");
            p.tier = t.getInt("tier");
            p.partner = t.getString("partner");
            p.xp = t.getInt("xp");
            p.level = t.contains("level") ? t.getInt("level") : 1;
            p.partnerSince = t.contains("partnerSince") ? t.getLong("partnerSince") : (p.partner.isEmpty() ? -1 : 0);
            p.lastBirthday = t.contains("lastBirthday") ? t.getLong("lastBirthday") : -1;
            p.lastAnniv = t.contains("lastAnniv") ? t.getLong("lastAnniv") : -1;
            if (t.hasUUID("pet")) p.pet = t.getUUID("pet");
            p.fish = t.getInt("fish");
            CompoundTag iv = t.getCompound("inv");
            for (String k : iv.getAllKeys()) p.inv.put(k, iv.getInt(k));
            for (int k : t.getIntArray("known")) p.known.add(k);
            if (t.contains("today")) p.today = DayLog.load(t.getCompound("today"));
            if (t.contains("yday")) p.yesterday = DayLog.load(t.getCompound("yday"));
            CompoundTag hf = t.getCompound("heardFrom");
            for (String k : hf.getAllKeys()) { try { p.heardFrom.put(Integer.parseInt(k), hf.getString(k)); } catch (NumberFormatException ignored) {} }
            p.paidDay = t.contains("paidDay") ? t.getLong("paidDay") : -1;
            p.workDay = t.contains("workDay") ? t.getLong("workDay") : -1;
            p.workTicks = t.getInt("workTicks");
            p.rentDay = t.contains("rentDay") ? t.getLong("rentDay") : -1;
            p.goal = t.getString("goal");
            p.goalCost = t.getInt("goalCost");
            p.goalsDone = t.getInt("goalsDone");
            p.lastGoal = t.getString("lastGoal");
            p.lastGoalDay = t.contains("lastGoalDay") ? t.getLong("lastGoalDay") : -100;
            p.bankDay = t.contains("bankDay") ? t.getLong("bankDay") : -1;
            p.cashDay = t.contains("cashDay") ? t.getLong("cashDay") : -1;
            p.loanWish = t.getInt("loanWish");
            p.loanWhy = t.getString("loanWhy");
            p.lastInterest = t.getInt("lastInterest");
            p.interestDay = t.contains("interestDay") ? t.getLong("interestDay") : -100;
            p.loanPaidDay = t.contains("loanPaidDay") ? t.getLong("loanPaidDay") : -100;
            p.missedTotal = t.getInt("missedTotal");
            if (t.contains("mind")) p.mind = Mind.load(t.getCompound("mind"));
            p.ownsPC = t.getBoolean("ownsPC");
            if (t.contains("pcPos")) p.pcPos = BlockPos.of(t.getLong("pcPos"));
            p.ownsPhone = t.getBoolean("ownsPhone");
            p.phoneColor = t.getInt("phoneColor");
            p.wantDevice = t.getString("wantDevice");
            p.wantDay = t.contains("wantDay") ? t.getLong("wantDay") : -1;
            p.phoneBroken = t.getBoolean("phoneBroken");
            p.hasTv = t.getBoolean("hasTv");
            p.headphones = t.getBoolean("headphones");
            p.phoneModel = Math.max(1, t.getInt("phoneModel"));
            p.pcDeliverDay = t.contains("pcDeliverDay") ? t.getLong("pcDeliverDay") : -1;
            return p;
        }

        public Place homePlace() {
            String stay = Hotel.stayRoom(id);
            return Place.get(stay != null ? stay : home);
        }

        public boolean livesOnIsland() {
            Place h = homePlace();
            return h != null && h.island;
        }
    }

    public static class Rel {
        public int fam;
        public int chats;
        public boolean met;
        public boolean knowsJob;
        public boolean knowsHome;
        public long seenDay = -1;
        public int seenToday;
        public long lastChatDay = -1;
        public int aff;
        public int romance;
        public int dates;
        public boolean rival;
        public final Map<String, Long> talked = new HashMap<>();
        public final Map<String, String> facts = new LinkedHashMap<>();
        public final List<String> shared = new ArrayList<>();
        public String heard = "";
        public long heardDay = -1;
        public int kept;
        public int broken;

        public boolean talkedRecently(String topic, long day, int days) {
            Long d = talked.get(topic);
            return d != null && day - d < days;
        }

        public void remember(String memory) {
            shared.remove(memory);
            shared.add(memory);
            while (shared.size() > 8) shared.remove(0);
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            CompoundTag tk = new CompoundTag();
            talked.forEach(tk::putLong);
            t.put("talk", tk);
            CompoundTag fc = new CompoundTag();
            facts.forEach(fc::putString);
            t.put("facts", fc);
            ListTag sh = new ListTag();
            for (String m : shared) sh.add(StringTag.valueOf(m));
            t.put("shared", sh);
            t.putString("heard", heard);
            t.putLong("hd", heardDay);
            t.putInt("kept", kept);
            t.putInt("broke", broken);
            t.putInt("fam", fam);
            t.putInt("chats", chats);
            t.putBoolean("met", met);
            t.putBoolean("job", knowsJob);
            t.putBoolean("home", knowsHome);
            t.putLong("sd", seenDay);
            t.putInt("st", seenToday);
            t.putLong("cd", lastChatDay);
            t.putInt("aff", aff);
            t.putInt("rom", romance);
            t.putInt("dates", dates);
            t.putBoolean("rival", rival);
            return t;
        }

        static Rel load(CompoundTag t) {
            Rel r = new Rel();
            r.fam = t.getInt("fam");
            r.chats = t.getInt("chats");
            r.met = t.getBoolean("met");
            r.knowsJob = t.getBoolean("job");
            r.knowsHome = t.getBoolean("home");
            r.seenDay = t.getLong("sd");
            r.seenToday = t.getInt("st");
            r.lastChatDay = t.getLong("cd");
            r.aff = t.getInt("aff");
            r.romance = t.getInt("rom");
            r.dates = t.getInt("dates");
            r.rival = t.getBoolean("rival");
            CompoundTag tk = t.getCompound("talk");
            for (String k : tk.getAllKeys()) r.talked.put(k, tk.getLong(k));
            CompoundTag fc = t.getCompound("facts");
            for (String k : fc.getAllKeys()) r.facts.put(k, fc.getString(k));
            for (Tag m : t.getList("shared", Tag.TAG_STRING)) r.shared.add(m.getAsString());
            r.heard = t.getString("heard");
            r.heardDay = t.contains("hd") ? t.getLong("hd") : -1;
            r.kept = t.getInt("kept");
            r.broken = t.getInt("broke");
            return r;
        }

        public boolean friend() {
            return met && fam >= 50 && chats >= 3 && !rival;
        }

        public boolean bestFriend() {
            return met && fam >= 85 && chats >= 8;
        }

        public String stage() {
            if (rival) return "rivals";
            if (bestFriend()) return "best friends";
            if (friend()) return "friends";
            if (met) return "acquaintances";
            if (knowsJob) return "heard of them";
            if (fam > 0) return "seen around";
            return "strangers";
        }
    }

    public static class Event {
        public int id;
        public long day;
        public String kind;
        public String text;
        public BlockPos pos;
    }

    public static class Plan {
        public long day;
        public String place;
        public List<String> who = new ArrayList<>();
        public String what;
        public String by = "";
        public final Set<String> came = new LinkedHashSet<>();
        public final Set<String> reviewed = new LinkedHashSet<>();
    }

    public static class Tx {
        public long day;
        public int time;
        public String from;
        public String to;
        public int amount;
        public String memo;
    }

    public final Map<String, Profile> profiles = new LinkedHashMap<>();
    private final Map<String, Map<String, Rel>> rels = new HashMap<>();

    long maxChatDay() {
        long m = Long.MIN_VALUE;
        for (Map<String, Rel> in : rels.values()) for (Rel r : in.values()) m = Math.max(m, r.lastChatDay);
        return m;
    }
    private final Map<String, Map<String, Rel>> playerRels = new HashMap<>();
    public final List<Plan> plans = new ArrayList<>();
    public final List<String> news = new ArrayList<>();
    public final List<Event> events = new ArrayList<>();
    public final Map<String, Integer> stock = new HashMap<>();
    public final Map<String, LinkedHashSet<Integer>> playerKnown = new HashMap<>();
    public final List<BlockPos> boards = new ArrayList<>();
    public int nextEvent = 1;
    public boolean chatter = false;
    public final List<Tx> ledger = new ArrayList<>();
    public final Map<String, Integer> accounts = new LinkedHashMap<>();
    public final Map<String, Integer> savings = new LinkedHashMap<>();
    public final Map<String, Bank.Loan> loans = new LinkedHashMap<>();
    public final Map<String, Bank.Holder> holders = new LinkedHashMap<>();
    public boolean bankOpened;
    public long interestDay = -1;
    public long collectDay = -1;
    public long sweepDay = -1;
    public int nextAccount = 1;
    public int interestTotal;
    public Civic civic = new Civic();
    public final Map<String, Integer> pantry = new LinkedHashMap<>();
    public final Map<String, Map<String, Integer>> shelf = new LinkedHashMap<>();
    public long announcedDay = -1;
    public long partyDay = -100;
    public long partyRolled = -100;
    public long partyForced = -100;
    public long partyMusicDone = -100;
    public int partySong;
    public java.util.UUID ferryId;
    public BlockPos ferryPos;
    public int ferryState;
    public int ferryMissing;
    public boolean skyxBuilt;
    public final Map<Long, String> pcs = new LinkedHashMap<>();
    public final Map<String, List<String>> inbox = new LinkedHashMap<>();
    public final Map<String, String> notes = new LinkedHashMap<>();
    public final Map<String, Map<String, Integer>> scores = new LinkedHashMap<>();
    public long festivalDay = -100;
    public long festivalRolled = -100;
    public long festivalStart = -1;
    public final java.util.List<Long> festivalDeco = new java.util.ArrayList<>();
    public final List<Phones.Text> texts = new ArrayList<>();
    public final List<Phones.Post> feed = new ArrayList<>();
    public int nextText = 1, nextPost = 1;
    public boolean techBuilt, phonesSeeded;
    public long skyPad = Long.MIN_VALUE;
    public int skyTop;
    public boolean skySearched, doorsMigrated, towerFixed, statueV2;
    public final Map<String, Map<String, Integer>> inboxIn = new LinkedHashMap<>();
    public final Map<String, Map<String, Integer>> inboxSeen = new LinkedHashMap<>();
    public boolean ferryStrict, railsFixed;
    public final Map<String, String> settings = new LinkedHashMap<>();
    public final Map<Long, String> tvs = new LinkedHashMap<>();
    public long launchDay = Long.MIN_VALUE, dayOffset, lastDayTime = -1;
    public boolean launchDone, avaPromoted;
    public int techSales;
    public final Map<String, Integer> techStock = new LinkedHashMap<>();
    public long techRestockDay = Long.MIN_VALUE, techPayDay = Long.MIN_VALUE;
    public final List<String> invites = new ArrayList<>();
    public final List<String> pendingReplies = new ArrayList<>();
    public final List<String> meets = new ArrayList<>();
    public final List<Long> rebrandChunks = new ArrayList<>();
    public final List<String> knownPlayers = new ArrayList<>();
    public final Map<Long, String> mailboxes = new LinkedHashMap<>();
    public final Map<Long, List<String>> mail = new LinkedHashMap<>();
    public final Map<Long, List<String>> parcels = new LinkedHashMap<>();
    public boolean mailboxSetup, expanded, oldTowerGone, hallBuilt, organInHall, fireworkMachineBuilt, policeBunks, fireBunks, stellarHome, beachBar, stellarCozy;
    public long fireworkShowDay = -1;
    public String cinemaUrl = "";
    public final List<String> uploads = new ArrayList<>();
    public final List<String> postIdeas = new ArrayList<>();
    public final List<String> reacts = new ArrayList<>();

    public static final String CITY = "city";
    public static boolean managerAva;

    public String setting(String player, String key, String def) {
        return settings.getOrDefault(player + "|" + key, def);
    }

    public void setSetting(String player, String key, String value) {
        settings.put(player + "|" + key, value);
        setDirty();
    }

    public static boolean isAccount(String who) {
        return who.startsWith("biz:") || who.equals(CITY) || who.startsWith("sav:");
    }

    public int balance(String who) {
        if (who.startsWith("sav:")) return savings.getOrDefault(who.substring(4), 0);
        if (isAccount(who)) return accounts.getOrDefault(who, 0);
        Profile p = profiles.get(who);
        return p == null ? 0 : p.coins;
    }

    public String accountName(String who) {
        if (who.equals(CITY)) return "City of Solaris";
        if (who.startsWith("sav:")) return accountName(who.substring(4)) + " (bank)";
        if (who.startsWith("player:")) return who.substring(7);
        if (who.startsWith("biz:")) {
            String k = who.substring(4);
            if (k.equals("rent_city")) return "Ember Heights Management";
            if (k.equals("rent_isle")) return "Neon Heights Housing";
            Place pl = Place.get(k);
            return pl == null ? k : Events.sentence(pl.label.replaceFirst("^the ", ""));
        }
        Profile p = profiles.get(who);
        return p == null ? who : p.name;
    }

    public boolean pay(String from, String to, int amount, String memo, long day, int time) {
        return pay(from, to, amount, memo, day, time, true);
    }

    public boolean pay(String from, String to, int amount, String memo, long day, int time, boolean diary) {
        if (amount <= 0) return false;
        if (!from.equals(CITY)) {
            if (balance(from) < amount) return false;
            adjust(from, -amount);
        }
        if (!to.equals(CITY)) adjust(to, amount);
        Tx t = new Tx();
        t.day = day;
        t.time = time;
        t.from = from;
        t.to = to;
        t.amount = amount;
        t.memo = memo;
        ledger.add(t);
        while (ledger.size() > 500) ledger.remove(0);
        Profile pf = diary ? profiles.get(from) : null;
        if (pf != null) pf.log(day).spent += amount;
        Profile pt = diary ? profiles.get(to) : null;
        if (pt != null) pt.log(day).earned += amount;
        setDirty();
        return true;
    }

    private void adjust(String who, int delta) {
        if (who.startsWith("sav:")) savings.merge(who.substring(4), delta, Integer::sum);
        else if (isAccount(who)) accounts.merge(who, delta, Integer::sum);
        else {
            Profile p = profiles.get(who);
            if (p != null) p.coins += delta;
        }
    }

    public int pantry(String key) {
        return pantry.getOrDefault(key, 0);
    }

    public void pantryAdd(String key, int n) {
        int v = Math.max(0, pantry(key) + n);
        pantry.put(key, v);
        setDirty();
    }

    public Map<String, Integer> shelf(String shop) {
        return shelf.computeIfAbsent(shop, k -> new LinkedHashMap<>());
    }

    public static CityData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(CityData::load, CityData::new, NAME);
    }

    public Rel rel(String a, String b) {
        return rels.computeIfAbsent(a, k -> new HashMap<>()).computeIfAbsent(b, k -> new Rel());
    }

    public Rel peekRel(String a, String b) {
        Map<String, Rel> m = rels.get(a);
        return m == null ? null : m.get(b);
    }

    public Rel playerRel(String resident, String playerName) {
        return playerRels.computeIfAbsent(resident, k -> new HashMap<>()).computeIfAbsent(playerName, k -> new Rel());
    }

    public java.util.Set<String> playerNames(String resident) {
        Map<String, Rel> m = playerRels.get(resident);
        return m == null ? java.util.Set.of() : m.keySet();
    }

    public List<String> friendsOf(String a) {
        List<String> out = new ArrayList<>();
        Map<String, Rel> m = rels.get(a);
        if (m != null) m.forEach((k, v) -> { if (v.friend()) out.add(k); });
        return out;
    }

    public List<Plan> plansFor(String id, long day) {
        List<Plan> out = new ArrayList<>();
        for (Plan p : plans) if (p.day == day && p.who.contains(id)) out.add(p);
        return out;
    }

    public void addPlan(long day, String place, String what, String... who) {
        plans.removeIf(p -> p.day < day - 3);
        Plan p = new Plan();
        p.day = day;
        p.place = place;
        p.what = what;
        for (String w : who) p.who.add(w);
        if (who.length > 0) p.by = who[0];
        plans.add(p);
        setDirty();
    }

    public void news(long day, String text) {
        news.add("Day " + (day + 1) + ": " + text);
        while (news.size() > 60) news.remove(0);
        setDirty();
    }

    public Event event(long day, String kind, String text, BlockPos pos, String... knownBy) {
        Event e = new Event();
        e.id = nextEvent++;
        e.day = day;
        e.kind = kind;
        e.text = text;
        e.pos = pos;
        events.add(e);
        while (events.size() > 80) events.remove(0);
        for (String k : knownBy) {
            Profile p = profiles.get(k);
            if (p != null) p.learn(e.id);
        }
        news(day, text.substring(0, 1).toUpperCase() + text.substring(1) + ".");
        return e;
    }

    public Event eventById(int id) {
        for (Event e : events) if (e.id == id) return e;
        return null;
    }

    public int friendsCount(String id) {
        return friendsOf(id).size();
    }

    public int bestFriendsCount(String id) {
        int n = 0;
        Map<String, Rel> m = rels.get(id);
        if (m != null) for (Rel r : m.values()) if (r.bestFriend()) n++;
        return n;
    }

    public List<String> rivalsOf(String a) {
        List<String> out = new ArrayList<>();
        Map<String, Rel> m = rels.get(a);
        if (m != null) m.forEach((k, v) -> { if (v.rival) out.add(k); });
        return out;
    }

    public int statusScore(Profile p) {
        int s = Economy.prestige(p.job) * 6;
        s += friendsCount(p.id) * 5 + bestFriendsCount(p.id) * 4;
        s += Math.min((p.coins + savings.getOrDefault(p.id, 0)) / 5, 20);
        s += Math.min(p.rep, 30);
        if (!p.partner.isEmpty()) s += 6;
        s += Math.min(p.known.size() / 3, 8);
        s -= rivalsOf(p.id).size() * 3;
        Map<String, Rel> pr = playerRels.get(p.id);
        if (pr != null) for (Rel r : pr.values()) if (r.met) { s += 4; break; }
        return Math.max(0, s);
    }

    public static final String[] TIERS = {"Newcomer", "Local", "Well-known", "Respected", "Pillar of the Community"};

    public static int tierOf(int score) {
        if (score < 15) return 0;
        if (score < 30) return 1;
        if (score < 48) return 2;
        if (score < 68) return 3;
        return 4;
    }

    public Profile byName(String name) {
        for (Profile p : profiles.values()) if (p.name.equalsIgnoreCase(name) || p.id.equalsIgnoreCase(name)) return p;
        return null;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag ps = new ListTag();
        for (Profile p : profiles.values()) ps.add(p.save());
        tag.put("profiles", ps);
        tag.put("rels", saveRels(rels));
        tag.put("prels", saveRels(playerRels));
        ListTag pl = new ListTag();
        for (Plan p : plans) {
            CompoundTag t = new CompoundTag();
            t.putLong("day", p.day);
            t.putString("place", p.place);
            t.putString("what", p.what);
            ListTag w = new ListTag();
            for (String s : p.who) w.add(StringTag.valueOf(s));
            t.put("who", w);
            t.putString("by", p.by);
            ListTag cm = new ListTag();
            for (String x : p.came) cm.add(StringTag.valueOf(x));
            t.put("came", cm);
            ListTag rv = new ListTag();
            for (String x : p.reviewed) rv.add(StringTag.valueOf(x));
            t.put("rev", rv);
            pl.add(t);
        }
        tag.put("plans", pl);
        ListTag nw = new ListTag();
        for (String s : news) nw.add(StringTag.valueOf(s));
        tag.put("news", nw);
        tag.putBoolean("chatlog", chatter);
        ListTag ev = new ListTag();
        for (Event e : events) {
            CompoundTag t = new CompoundTag();
            t.putInt("id", e.id);
            t.putLong("day", e.day);
            t.putString("kind", e.kind);
            t.putString("text", e.text);
            if (e.pos != null) t.putLong("pos", e.pos.asLong());
            ev.add(t);
        }
        tag.put("events", ev);
        tag.putInt("nextEvent", nextEvent);
        CompoundTag st = new CompoundTag();
        stock.forEach(st::putInt);
        tag.put("stock", st);
        CompoundTag pk = new CompoundTag();
        playerKnown.forEach((k, v) -> pk.putIntArray(k, v.stream().mapToInt(Integer::intValue).toArray()));
        tag.put("pknown", pk);
        tag.putLongArray("boards", boards.stream().mapToLong(BlockPos::asLong).toArray());
        ListTag lg = new ListTag();
        for (Tx x : ledger) {
            CompoundTag t = new CompoundTag();
            t.putLong("d", x.day);
            t.putInt("t", x.time);
            t.putString("f", x.from);
            t.putString("to", x.to);
            t.putInt("a", x.amount);
            t.putString("m", x.memo);
            lg.add(t);
        }
        tag.put("ledger", lg);
        CompoundTag ac = new CompoundTag();
        accounts.forEach(ac::putInt);
        tag.put("accounts", ac);
        CompoundTag pa = new CompoundTag();
        pantry.forEach(pa::putInt);
        tag.put("pantry", pa);
        CompoundTag sh = new CompoundTag();
        shelf.forEach((k, m) -> { CompoundTag in = new CompoundTag(); m.forEach(in::putInt); sh.put(k, in); });
        tag.put("shelf", sh);
        tag.putLong("announced", announcedDay);
        tag.putLong("partyDay", partyDay);
        tag.putLong("partyRolled", partyRolled);
        tag.putLong("partyForced", partyForced);
        tag.putLong("partyDone", partyMusicDone);
        tag.putInt("partySong", partySong);
        CompoundTag sv = new CompoundTag();
        savings.forEach(sv::putInt);
        tag.put("savings", sv);
        CompoundTag ln = new CompoundTag();
        loans.forEach((k, v) -> ln.put(k, v.save()));
        tag.put("loans", ln);
        CompoundTag hd = new CompoundTag();
        holders.forEach((k, v) -> hd.put(k, v.save()));
        tag.put("holders", hd);
        tag.putBoolean("bankOpened", bankOpened);
        tag.putLong("interestDay", interestDay);
        tag.putLong("collectDay", collectDay);
        tag.putLong("sweepDay", sweepDay);
        tag.putInt("nextAccount", nextAccount);
        tag.putInt("interestTotal", interestTotal);
        tag.put("civic", civic.save());
        if (ferryId != null) tag.putUUID("ferryId", ferryId);
        if (ferryPos != null) tag.putLong("ferryPos", ferryPos.asLong());
        tag.putInt("ferryState", ferryState);
        tag.putBoolean("skyxBuilt", skyxBuilt);
        CompoundTag pc = new CompoundTag();
        pcs.forEach((k, v) -> pc.putString(String.valueOf(k), v));
        tag.put("pcs", pc);
        CompoundTag ib = new CompoundTag();
        inbox.forEach((k, v) -> { ListTag l = new ListTag(); for (String m : v) l.add(StringTag.valueOf(m)); ib.put(k, l); });
        tag.put("inbox", ib);
        CompoundTag nt = new CompoundTag();
        notes.forEach(nt::putString);
        tag.put("notes", nt);
        CompoundTag sc = new CompoundTag();
        scores.forEach((g, m) -> { CompoundTag in = new CompoundTag(); m.forEach(in::putInt); sc.put(g, in); });
        tag.put("scores", sc);
        tag.putLong("festivalDay", festivalDay);
        tag.putLong("festivalRolled", festivalRolled);
        tag.putLong("festivalStart", festivalStart);
        tag.putLongArray("festivalDeco", festivalDeco.stream().mapToLong(Long::longValue).toArray());
        ListTag tx = new ListTag();
        for (Phones.Text x : texts) tx.add(x.save());
        tag.put("texts", tx);
        ListTag fd = new ListTag();
        for (Phones.Post x : feed) fd.add(x.save());
        tag.put("feed", fd);
        tag.putInt("nextText", nextText);
        tag.putInt("nextPost", nextPost);
        tag.putBoolean("techBuilt", techBuilt);
        tag.putLong("skyPad", skyPad);
        tag.putInt("skyTop", skyTop);
        tag.putBoolean("skySearched", skySearched);
        tag.putBoolean("doorsMigrated", doorsMigrated);
        tag.putBoolean("towerFixed", towerFixed);
        tag.putBoolean("statueV2", statueV2);
        tag.putBoolean("phonesSeeded", phonesSeeded);
        tag.put("inboxIn", saveCounts(inboxIn));
        tag.put("inboxSeen", saveCounts(inboxSeen));
        tag.putBoolean("ferryStrict", ferryStrict);
        tag.putBoolean("railsFixed", railsFixed);
        CompoundTag stg = new CompoundTag();
        settings.forEach(stg::putString);
        tag.put("settings", stg);
        CompoundTag tv = new CompoundTag();
        tvs.forEach((k, v) -> tv.putString(String.valueOf(k), v));
        tag.put("tvs", tv);
        tag.putLong("launchDay", launchDay);
        tag.putLong("dayOffset", dayOffset);
        tag.putLong("lastDayTime", lastDayTime);
        tag.putBoolean("launchDone", launchDone);
        tag.putBoolean("avaPromoted", avaPromoted);
        tag.putInt("techSales", techSales);
        CompoundTag ts = new CompoundTag();
        techStock.forEach(ts::putInt);
        tag.put("techStock", ts);
        tag.putLong("techRestockDay", techRestockDay);
        tag.putLong("techPayDay", techPayDay);
        Phones.persist(this);
        tag.put("invites", strings(invites));
        tag.put("pendingReplies", strings(pendingReplies));
        tag.put("meets", strings(meets));
        tag.put("knownPlayers", strings(knownPlayers));
        CompoundTag mbx = new CompoundTag();
        mailboxes.forEach((k, v) -> mbx.putString(String.valueOf(k), v));
        tag.put("mailboxes", mbx);
        CompoundTag mls = new CompoundTag();
        mail.forEach((k, v) -> mls.put(String.valueOf(k), strings(v)));
        tag.put("mailItems", mls);
        CompoundTag pcs = new CompoundTag();
        parcels.forEach((k, v) -> pcs.put(String.valueOf(k), strings(v)));
        tag.put("parcels", pcs);
        tag.putBoolean("mailboxSetup", mailboxSetup);
        tag.putBoolean("expanded", expanded);
        tag.putBoolean("oldTowerGone", oldTowerGone);
        tag.putBoolean("hallBuilt", hallBuilt);
        tag.putBoolean("organInHall", organInHall);
        tag.putBoolean("beachBar", beachBar);
        tag.putBoolean("stellarCozy", stellarCozy);
        tag.putBoolean("fireworkMachineBuilt", fireworkMachineBuilt);
        tag.putBoolean("policeBunks", policeBunks);
        tag.putBoolean("fireBunks", fireBunks);
        tag.putString("cinemaUrl", cinemaUrl);
        tag.putBoolean("stellarHome", stellarHome);
        tag.putLong("fireworkShowDay", fireworkShowDay);
        tag.put("uploads", strings(uploads));
        tag.putLongArray("rebrandChunks", rebrandChunks.stream().mapToLong(Long::longValue).toArray());
        tag.put("postIdeas", strings(postIdeas));
        tag.put("reacts", strings(reacts));
        return tag;
    }

    private static ListTag strings(List<String> l) {
        ListTag t = new ListTag();
        for (String s : l) t.add(StringTag.valueOf(s));
        return t;
    }

    private static void rstrings(CompoundTag tag, String k, List<String> into) {
        for (Tag x : tag.getList(k, Tag.TAG_STRING)) into.add(x.getAsString());
    }

    private static CompoundTag saveCounts(Map<String, Map<String, Integer>> m) {
        CompoundTag t = new CompoundTag();
        m.forEach((k, v) -> { CompoundTag in = new CompoundTag(); v.forEach(in::putInt); t.put(k, in); });
        return t;
    }

    private static void loadCounts(CompoundTag t, Map<String, Map<String, Integer>> m) {
        for (String k : t.getAllKeys()) {
            CompoundTag in = t.getCompound(k);
            Map<String, Integer> v = new LinkedHashMap<>();
            for (String r : in.getAllKeys()) v.put(r, in.getInt(r));
            m.put(k, v);
        }
    }

    private static CompoundTag saveRels(Map<String, Map<String, Rel>> map) {
        CompoundTag t = new CompoundTag();
        map.forEach((a, m) -> {
            CompoundTag inner = new CompoundTag();
            m.forEach((b, r) -> inner.put(b, r.save()));
            t.put(a, inner);
        });
        return t;
    }

    private static void loadRels(CompoundTag t, Map<String, Map<String, Rel>> map) {
        for (String a : t.getAllKeys()) {
            CompoundTag inner = t.getCompound(a);
            Map<String, Rel> m = new HashMap<>();
            for (String b : inner.getAllKeys()) m.put(b, Rel.load(inner.getCompound(b)));
            map.put(a, m);
        }
    }

    public static CityData load(CompoundTag tag) {
        CityData d = new CityData();
        for (Tag t : tag.getList("profiles", Tag.TAG_COMPOUND)) {
            Profile p = Profile.load((CompoundTag) t);
            d.profiles.put(p.id, p);
        }
        loadRels(tag.getCompound("rels"), d.rels);
        loadRels(tag.getCompound("prels"), d.playerRels);
        for (Tag t : tag.getList("plans", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Plan p = new Plan();
            p.day = c.getLong("day");
            p.place = c.getString("place");
            p.what = c.getString("what");
            for (Tag w : c.getList("who", Tag.TAG_STRING)) p.who.add(w.getAsString());
            p.by = c.getString("by");
            for (Tag w : c.getList("came", Tag.TAG_STRING)) p.came.add(w.getAsString());
            for (Tag w : c.getList("rev", Tag.TAG_STRING)) p.reviewed.add(w.getAsString());
            d.plans.add(p);
        }
        for (Tag t : tag.getList("news", Tag.TAG_STRING)) d.news.add(t.getAsString());
        d.chatter = false;
        for (Tag t : tag.getList("events", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Event e = new Event();
            e.id = c.getInt("id");
            e.day = c.getLong("day");
            e.kind = c.getString("kind");
            e.text = c.getString("text");
            if (c.contains("pos")) e.pos = BlockPos.of(c.getLong("pos"));
            d.events.add(e);
        }
        d.nextEvent = Math.max(1, tag.getInt("nextEvent"));
        CompoundTag st = tag.getCompound("stock");
        for (String k : st.getAllKeys()) d.stock.put(k, st.getInt(k));
        CompoundTag pk = tag.getCompound("pknown");
        for (String k : pk.getAllKeys()) {
            LinkedHashSet<Integer> set = new LinkedHashSet<>();
            for (int v : pk.getIntArray(k)) set.add(v);
            d.playerKnown.put(k, set);
        }
        for (long b : tag.getLongArray("boards")) d.boards.add(BlockPos.of(b));
        for (Tag t : tag.getList("ledger", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Tx x = new Tx();
            x.day = c.getLong("d");
            x.time = c.getInt("t");
            x.from = c.getString("f");
            x.to = c.getString("to");
            x.amount = c.getInt("a");
            x.memo = c.getString("m");
            d.ledger.add(x);
        }
        CompoundTag ac = tag.getCompound("accounts");
        for (String k : ac.getAllKeys()) d.accounts.put(k, ac.getInt(k));
        CompoundTag pa = tag.getCompound("pantry");
        for (String k : pa.getAllKeys()) d.pantry.put(k, pa.getInt(k));
        CompoundTag sh = tag.getCompound("shelf");
        for (String k : sh.getAllKeys()) {
            CompoundTag in = sh.getCompound(k);
            Map<String, Integer> m = new LinkedHashMap<>();
            for (String i : in.getAllKeys()) m.put(i, in.getInt(i));
            d.shelf.put(k, m);
        }
        if (d.shelf.isEmpty()) {
            for (Map.Entry<String, Integer> e : d.stock.entrySet()) {
                Job j = null;
                for (Job x : Job.values()) if (x.workKey.equals(e.getKey())) j = x;
                if (j != null && e.getValue() > 0) d.shelf(e.getKey()).put(Economy.products(j).get(0), e.getValue());
            }
        }
        d.announcedDay = tag.contains("announced") ? tag.getLong("announced") : -1;
        if (tag.contains("partyDay")) {
            d.partyDay = tag.getLong("partyDay");
            d.partyRolled = tag.getLong("partyRolled");
            d.partyForced = tag.getLong("partyForced");
            d.partyMusicDone = tag.getLong("partyDone");
            d.partySong = tag.getInt("partySong");
        }
        CompoundTag sv = tag.getCompound("savings");
        for (String k : sv.getAllKeys()) d.savings.put(k, sv.getInt(k));
        CompoundTag ln = tag.getCompound("loans");
        for (String k : ln.getAllKeys()) d.loans.put(k, Bank.Loan.load(ln.getCompound(k)));
        CompoundTag hd = tag.getCompound("holders");
        for (String k : hd.getAllKeys()) d.holders.put(k, Bank.Holder.load(hd.getCompound(k)));
        d.bankOpened = tag.getBoolean("bankOpened");
        d.interestDay = tag.contains("interestDay") ? tag.getLong("interestDay") : -1;
        d.collectDay = tag.contains("collectDay") ? tag.getLong("collectDay") : -1;
        d.sweepDay = tag.contains("sweepDay") ? tag.getLong("sweepDay") : -1;
        d.nextAccount = Math.max(1, tag.getInt("nextAccount"));
        d.interestTotal = tag.getInt("interestTotal");
        if (tag.contains("civic")) d.civic = Civic.load(tag.getCompound("civic"));
        if (tag.hasUUID("ferryId")) d.ferryId = tag.getUUID("ferryId");
        if (tag.contains("ferryPos")) d.ferryPos = BlockPos.of(tag.getLong("ferryPos"));
        d.ferryState = tag.getInt("ferryState");
        d.skyxBuilt = tag.getBoolean("skyxBuilt");
        CompoundTag pc = tag.getCompound("pcs");
        for (String k : pc.getAllKeys()) { try { d.pcs.put(Long.parseLong(k), pc.getString(k)); } catch (NumberFormatException ignored) {} }
        CompoundTag ib = tag.getCompound("inbox");
        for (String k : ib.getAllKeys()) { List<String> l = new ArrayList<>(); for (Tag x : ib.getList(k, Tag.TAG_STRING)) l.add(x.getAsString()); d.inbox.put(k, l); }
        CompoundTag nt = tag.getCompound("notes");
        for (String k : nt.getAllKeys()) d.notes.put(k, nt.getString(k));
        CompoundTag sc = tag.getCompound("scores");
        for (String g : sc.getAllKeys()) { CompoundTag in = sc.getCompound(g); Map<String, Integer> m = new LinkedHashMap<>(); for (String k : in.getAllKeys()) m.put(k, in.getInt(k)); d.scores.put(g, m); }
        if (tag.contains("festivalDay")) { d.festivalDay = tag.getLong("festivalDay"); d.festivalRolled = tag.getLong("festivalRolled"); d.festivalStart = tag.contains("festivalStart") ? tag.getLong("festivalStart") : -1; for (long l : tag.getLongArray("festivalDeco")) d.festivalDeco.add(l); }
        for (Tag t : tag.getList("texts", Tag.TAG_COMPOUND)) d.texts.add(Phones.Text.load((CompoundTag) t));
        for (Tag t : tag.getList("feed", Tag.TAG_COMPOUND)) d.feed.add(Phones.Post.load((CompoundTag) t));
        d.nextText = Math.max(1, tag.getInt("nextText"));
        d.nextPost = Math.max(1, tag.getInt("nextPost"));
        d.techBuilt = tag.getBoolean("techBuilt");
        if (tag.contains("skyPad")) d.skyPad = tag.getLong("skyPad");
        d.skyTop = tag.getInt("skyTop");
        d.skySearched = tag.getBoolean("skySearched");
        d.doorsMigrated = tag.getBoolean("doorsMigrated");
        d.towerFixed = tag.getBoolean("towerFixed");
        d.statueV2 = tag.getBoolean("statueV2");
        d.phonesSeeded = tag.getBoolean("phonesSeeded");
        loadCounts(tag.getCompound("inboxIn"), d.inboxIn);
        loadCounts(tag.getCompound("inboxSeen"), d.inboxSeen);
        d.ferryStrict = tag.getBoolean("ferryStrict");
        d.railsFixed = tag.getBoolean("railsFixed");
        CompoundTag stg = tag.getCompound("settings");
        for (String k : stg.getAllKeys()) d.settings.put(k, stg.getString(k));
        CompoundTag tv = tag.getCompound("tvs");
        for (String k : tv.getAllKeys()) { try { d.tvs.put(Long.parseLong(k), tv.getString(k)); } catch (NumberFormatException ignored) {} }
        if (tag.contains("launchDay")) d.launchDay = tag.getLong("launchDay");
        d.dayOffset = tag.getLong("dayOffset");
        d.lastDayTime = tag.contains("lastDayTime") ? tag.getLong("lastDayTime") : -1;
        d.launchDone = tag.getBoolean("launchDone");
        d.avaPromoted = tag.getBoolean("avaPromoted");
        d.techSales = tag.getInt("techSales");
        CompoundTag ts = tag.getCompound("techStock");
        for (String k : ts.getAllKeys()) d.techStock.put(k, ts.getInt(k));
        if (tag.contains("techRestockDay")) d.techRestockDay = tag.getLong("techRestockDay");
        if (tag.contains("techPayDay")) d.techPayDay = tag.getLong("techPayDay");
        rstrings(tag, "invites", d.invites);
        rstrings(tag, "pendingReplies", d.pendingReplies);
        rstrings(tag, "meets", d.meets);
        rstrings(tag, "knownPlayers", d.knownPlayers);
        CompoundTag mbx = tag.getCompound("mailboxes");
        for (String k : mbx.getAllKeys()) { try { d.mailboxes.put(Long.parseLong(k), mbx.getString(k)); } catch (NumberFormatException ignored) {} }
        CompoundTag mls = tag.getCompound("mailItems");
        for (String k : mls.getAllKeys()) { try { List<String> l = new ArrayList<>(); rstrings(mls, k, l); d.mail.put(Long.parseLong(k), l); } catch (NumberFormatException ignored) {} }
        CompoundTag pcs = tag.getCompound("parcels");
        for (String k : pcs.getAllKeys()) { try { List<String> l = new ArrayList<>(); rstrings(pcs, k, l); d.parcels.put(Long.parseLong(k), l); } catch (NumberFormatException ignored) {} }
        d.mailboxSetup = tag.getBoolean("mailboxSetup");
        d.expanded = tag.getBoolean("expanded");
        d.oldTowerGone = tag.getBoolean("oldTowerGone");
        d.hallBuilt = tag.getBoolean("hallBuilt");
        d.organInHall = tag.getBoolean("organInHall");
        d.beachBar = tag.getBoolean("beachBar");
        d.stellarCozy = tag.getBoolean("stellarCozy");
        d.fireworkMachineBuilt = tag.getBoolean("fireworkMachineBuilt");
        d.policeBunks = tag.getBoolean("policeBunks");
        d.fireBunks = tag.getBoolean("fireBunks");
        d.cinemaUrl = tag.getString("cinemaUrl");
        d.stellarHome = tag.getBoolean("stellarHome");
        d.fireworkShowDay = tag.contains("fireworkShowDay") ? tag.getLong("fireworkShowDay") : -1;
        rstrings(tag, "uploads", d.uploads);
        for (long l : tag.getLongArray("rebrandChunks")) d.rebrandChunks.add(l);
        rstrings(tag, "postIdeas", d.postIdeas);
        rstrings(tag, "reacts", d.reacts);
        Gazette.syncPlaces(d);
        return d;
    }
}
