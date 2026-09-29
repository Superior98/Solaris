package com.fireheart.city;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.ServerLevelData;

/** Player-facing extras: daily rewards, achievements, resident finder, friends list, leaderboards, the morning bulletin and courier jobs. */
public final class Perks {
    private Perks() {}

    static void say(ServerPlayer pl, String s) {
        pl.sendSystemMessage(Component.literal(s));
    }

    /* ------------------------------------------------------------ Per-player settings */

    static final String[][] OPTS = {
            {"chimes", "Clock tower chimes"}, {"sky", "Sky effects (rainbows, stars, aurora, fireflies, seasonal particles)"},
            {"bulletin", "Morning bulletin"}, {"gifts", "Residents give you small gifts"}, {"hints", "Action-bar hints (courier compass, guides)"},
            {"visits", "Friends visit your home"}, {"texts", "Goodnight texts from your sweetheart"}
    };

    public static boolean opt(CityData d, String pn, String key) {
        return !d.setting(pn, "opt:" + key, "1").equals("0");
    }

    public static boolean opt(ServerPlayer pl, String key) {
        return opt(CityData.get(pl.serverLevel()), pl.getName().getString(), key);
    }

    public static String settings(ServerPlayer pl, CityData d, String toggle) {
        String pn = pl.getName().getString();
        if (toggle != null) {
            boolean known = false;
            for (String[] o : OPTS) if (o[0].equals(toggle)) known = true;
            if (!known) return "§cUnknown setting. §7Try: /sol settings";
            boolean now = !opt(d, pn, toggle);
            d.setSetting(pn, "opt:" + toggle, now ? "1" : "0");
            if (toggle.equals("bulletin")) d.setSetting(pn, "bulletinOff", now ? "0" : "1");
        }
        net.minecraft.network.chat.MutableComponent m = Component.literal("§6§lYour Solaris settings §7(click to toggle)");
        for (String[] o : OPTS) {
            boolean on = opt(d, pn, o[0]);
            m.append(Component.literal("\n" + (on ? "§a[ON] " : "§c[OFF] ") + "§f" + o[1]).withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sol settings " + o[0]))));
        }
        pl.sendSystemMessage(m);
        return "";
    }

    /** Pays into the player's bank savings if they have an account, otherwise hands over gold nuggets. */
    static void reward(ServerPlayer pl, CityData d, int coins, String memo) {
        if (coins <= 0) return;
        String pn = pl.getName().getString();
        ServerLevel sl = pl.serverLevel();
        if (Bank.holder(d, pn) != null) {
            d.pay(CityData.CITY, Bank.sav(Bank.playerKey(pn)), coins, memo, Calendar.worldDay(sl), (int) Math.floorMod(sl.getDayTime(), 24000L), false);
        } else {
            Bank.giveCash(pl, coins);
        }
        d.setDirty();
    }

    /* ------------------------------------------------------------ Daily reward */

    static final int[] DAILY = {5, 8, 10, 12, 15, 20, 35};

    public static void onLogin(ServerPlayer pl, CityData d) {
        String pn = pl.getName().getString();
        long today = Calendar.worldDay(pl.serverLevel());
        long last = parse(d.setting(pn, "lastSeenDay", String.valueOf(today)));
        if (today - last >= 3) {
            d.setSetting(pn, "absentDay", String.valueOf(today));
            say(pl, "§6Welcome back to Solaris! §7You've been away " + (today - last) + " days - your friends have missed you.");
        }
        d.setSetting(pn, "lastSeenDay", String.valueOf(today));
        daily(pl, d, false);
    }

    public static String daily(ServerPlayer pl, CityData d, boolean asked) {
        String pn = pl.getName().getString();
        long today = Calendar.worldDay(pl.serverLevel());
        long last = parse(d.setting(pn, "dailyDay", "-99"));
        int streak = (int) parse(d.setting(pn, "streak", "0"));
        if (last == today) {
            return "§6[Daily] §7Already claimed today. Streak: §e" + streak + " day" + (streak == 1 ? "" : "s") + "§7. Come back tomorrow for §e" + DAILY[Math.min(streak, DAILY.length - 1)] + "§7 coins.";
        }
        boolean grace = last == today - 2 && streak > 1 && !d.setting(pn, "graceWeek", "-1").equals(String.valueOf(Math.floorDiv(today, 7L)));
        if (grace) d.setSetting(pn, "graceWeek", String.valueOf(Math.floorDiv(today, 7L)));
        streak = last == today - 1 || grace ? streak + 1 : 1;
        if (grace) say(pl, "§6[Daily] §7You missed a day, but your weekly grace day saved your streak.");
        int coins = DAILY[Math.min(streak - 1, DAILY.length - 1)];
        d.setSetting(pn, "dailyDay", String.valueOf(today));
        d.setSetting(pn, "streak", String.valueOf(streak));
        reward(pl, d, coins, "Daily visit bonus (day " + streak + ")");
        Calendar.banner(pl, "§6Welcome back to Solaris!", "§eDaily bonus: " + coins + " coins §7· streak " + streak);
        pl.serverLevel().playSound(null, pl.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
        String msg = "§6[Daily] §e+" + coins + " coins§7 (" + streak + "-day streak" + (streak >= 7 ? ", max bonus!" : ", tomorrow: " + DAILY[Math.min(streak, DAILY.length - 1)]) + ")";
        if (!asked) say(pl, msg);
        return msg;
    }

    static long parse(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /* ------------------------------------------------------------ Achievements */

    record Ach(String id, String name, String how, int coins) {}

    static final Ach[] ACHS = {
            new Ach("meet1", "First Hello", "Meet a resident", 5),
            new Ach("meet10", "Social Butterfly", "Meet 10 residents", 20),
            new Ach("meetall", "Everybody Knows Your Name", "Meet every resident", 60),
            new Ach("friend", "Good Friends", "Reach high affection with a resident", 15),
            new Ach("friend5", "Popular", "Reach high affection with 5 residents", 40),
            new Ach("isle", "Head in the Clouds", "Visit Neon Heights", 10),
            new Ach("rich", "Saver", "Have 250 coins in the bank", 25),
            new Ach("streak7", "Regular", "Keep a 7-day visit streak", 30),
            new Ach("courier5", "Special Delivery", "Complete 5 courier jobs", 30),
            new Ach("rps", "Rock Solid", "Win rock-paper-scissors against a resident", 5),
            new Ach("wish", "Wish Upon a Star", "Be outside when a shooting star falls", 10),
            new Ach("rainbow", "Somewhere Over It", "See a rainbow over Solaris", 10),
            new Ach("crowd", "Crowd Pleaser", "Get 3 residents to react to one emote", 10),
            new Ach("legend", "Solaris Legend", "Reach the top reputation rank", 100),
            new Ach("treasure5", "Treasure Hunter", "Find 5 daily treasures", 40),
            new Ach("angler", "Angler", "Catch 10 fish", 15),
            new Ach("birthday", "Another Year", "Celebrate your birthday in Solaris", 10),
            new Ach("aurora", "Northern Lights", "See the aurora over Solaris", 15),
            new Ach("penpal", "Pen Pal", "Send 5 letters to residents", 15),
            new Ach("lucky", "Lucky Find", "Find something hidden in the city's grass or flowers", 5),
            new Ach("nightowl", "Night Owl", "Be outside in Solaris at midnight", 10),
            new Ach("wedding", "Wedding Guest", "Attend a resident wedding at the plaza", 25),
            new Ach("architect", "Architect", "Place 500 blocks around Solaris", 30),
            new Ach("confidant", "Confidant", "Be trusted enough to read a resident's diary", 20),
            new Ach("bestie", "Best Friends Forever", "Become a resident's best friend", 30),
            new Ach("married", "Happily Ever After", "Marry your sweetheart", 50),
            new Ach("seeker", "Seeker", "Find a resident in hide and seek", 10),
            new Ach("racer", "Speedster", "Win a race against a resident", 10),
            new Ach("runner", "Fun Runner", "Finish the Solaris Fun Run", 15),
            new Ach("champion", "Master Angler", "Win the Sunday fishing tournament", 30),
            new Ach("quizwhiz", "Quiz Whiz", "Win Quiz Night", 25),
            new Ach("quests10", "Go-Getter", "Finish all daily quests on 10 days", 50),
            new Ach("donor", "Philanthropist", "Donate 100 coins to the City Fund", 20),
            new Ach("nurse", "Get Well Soon", "Bring soup to a sick resident", 15),
    };

    public static boolean unlock(ServerPlayer pl, CityData d, String id) {
        String pn = pl.getName().getString();
        if (d.setting(pn, "ach:" + id, "0").equals("1")) return false;
        Ach a = null;
        for (Ach x : ACHS) if (x.id().equals(id)) a = x;
        if (a == null) return false;
        d.setSetting(pn, "ach:" + id, "1");
        reward(pl, d, a.coins(), "Achievement: " + a.name());
        Calendar.banner(pl, "§d★ Achievement unlocked", "§f" + a.name() + " §7· +" + a.coins() + " coins");
        say(pl, "§d[Achievement] §f" + a.name() + "§7 - " + a.how() + " §e(+" + a.coins() + " coins)");
        pl.serverLevel().playSound(null, pl.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.5f, 1.0f);
        int got = 0;
        for (Ach x : ACHS) if (d.setting(pn, "ach:" + x.id(), "0").equals("1")) got++;
        if (got % 10 == 0) {
            reward(pl, d, 50, "Achievement milestone (" + got + ")");
            say(pl, "§d✦ Milestone: " + got + " achievements! §e+50 bonus coins");
        }
        Applause.forAchievement(pl, a.name());
        return true;
    }

    static void checkAchievements(ServerPlayer pl, CityData d) {
        String pn = pl.getName().getString();
        int met = 0, close = 0;
        for (CityData.Profile p : d.profiles.values()) {
            if (!d.playerNames(p.id).contains(pn)) continue;
            CityData.Rel r = d.playerRel(p.id, pn);
            if (r.met) met++;
            if (r.aff >= 60) close++;
        }
        if (met >= 1) unlock(pl, d, "meet1");
        if (met >= 10) unlock(pl, d, "meet10");
        if (met >= d.profiles.size() && met > 0) unlock(pl, d, "meetall");
        if (close >= 1) unlock(pl, d, "friend");
        if (close >= 5) unlock(pl, d, "friend5");
        if (pl.getY() > 150) unlock(pl, d, "isle");
        if (Bank.savings(d, Bank.playerKey(pn)) >= 250) unlock(pl, d, "rich");
        if (parse(d.setting(pn, "streak", "0")) >= 7) unlock(pl, d, "streak7");
        if (parse(d.setting(pn, "couriers", "0")) >= 5) unlock(pl, d, "courier5");
        long tod = Math.floorMod(pl.serverLevel().getDayTime(), 24000L);
        if (tod > 17700 && tod < 18300 && Skies.outside(pl)) unlock(pl, d, "nightowl");
    }

    static String progress(CityData d, String pn, String id) {
        int met = 0, close = 0;
        if (id.startsWith("meet") || id.startsWith("friend")) {
            for (CityData.Profile p : d.profiles.values()) {
                if (!d.playerNames(p.id).contains(pn)) continue;
                CityData.Rel r = d.playerRel(p.id, pn);
                if (r.met) met++;
                if (r.aff >= 60) close++;
            }
        }
        return switch (id) {
            case "meet10" -> met + "/10";
            case "meetall" -> met + "/" + d.profiles.size();
            case "friend5" -> close + "/5";
            case "rich" -> Bank.savings(d, Bank.playerKey(pn)) + "/250";
            case "streak7" -> d.setting(pn, "streak", "0") + "/7";
            case "courier5" -> d.setting(pn, "couriers", "0") + "/5";
            case "treasure5" -> d.setting(pn, "treasures", "0") + "/5";
            case "angler" -> d.setting(pn, "fish", "0") + "/10";
            case "penpal" -> d.setting(pn, "lettersSent", "0") + "/5";
            case "architect" -> d.setting(pn, "built", "0") + "/500";
            case "quests10" -> d.setting(pn, "questsDone", "0") + "/10";
            case "donor" -> d.setting(pn, "donated", "0") + "/100";
            default -> "";
        };
    }

    public static String achievements(ServerPlayer pl, CityData d) {
        String pn = pl.getName().getString();
        StringBuilder sb = new StringBuilder("§d§lAchievements");
        int got = 0;
        for (Ach a : ACHS) {
            boolean has = d.setting(pn, "ach:" + a.id(), "0").equals("1");
            if (has) got++;
            String prog = has ? "" : progress(d, pn, a.id());
            sb.append("\n").append(has ? "§a✔ §f" : "§8✘ §7").append(a.name()).append(" §8- ").append(a.how()).append(has ? "" : " §e(" + a.coins() + ")").append(prog.isEmpty() ? "" : " §b" + prog);
        }
        return sb.append("\n§7").append(got).append("/").append(ACHS.length).append(" unlocked").toString();
    }

    /* ------------------------------------------------------------ Finder, friends, leaderboards */

    static Resident find(ServerLevel sl, CityData d, String name) {
        String n = name.toLowerCase(Locale.ROOT);
        for (CityData.Profile p : d.profiles.values()) {
            if (!p.name.toLowerCase(Locale.ROOT).equals(n) && !p.id.equalsIgnoreCase(name)) continue;
            if (p.entity != null && sl.getEntity(p.entity) instanceof Resident r) return r;
        }
        return null;
    }

    static String compass(double dx, double dz) {
        String[] dirs = {"south", "south-west", "west", "north-west", "north", "north-east", "east", "south-east"};
        double a = Math.toDegrees(Math.atan2(-dx, dz));
        return dirs[Math.floorMod((int) Math.round(a / 45.0), 8)];
    }

    public static String whereis(ServerPlayer pl, CityData d, String name) {
        ServerLevel sl = pl.serverLevel();
        CityData.Profile p = null;
        for (CityData.Profile q : d.profiles.values()) if (q.name.equalsIgnoreCase(name) || q.id.equalsIgnoreCase(name)) p = q;
        if (p == null) {
            ServerPlayer o = sl.getServer().getPlayerList().getPlayerByName(name);
            if (o == null) return "§cNobody called " + name + " lives in Solaris or is online.";
            if (o.level() != sl) return "§b" + o.getName().getString() + "§7 is in another dimension.";
            double dx = o.getX() - pl.getX(), dz = o.getZ() - pl.getZ();
            int dist = (int) Math.sqrt(dx * dx + dz * dz);
            return "§b" + o.getName().getString() + "§7 is " + (dist < 6 ? "right next to you" : dist + " blocks " + compass(dx, dz)) + " §8(" + o.blockPosition().toShortString() + ")";
        }
        String doing = p.doing == null || p.doing.isEmpty() ? "out and about" : p.doing;
        Resident r = find(sl, d, name);
        if (r == null) return "§b" + p.name + "§7 is " + doing + "§7 (somewhere out of range).";
        double dx = r.getX() - pl.getX(), dz = r.getZ() - pl.getZ();
        int dist = (int) Math.sqrt(dx * dx + dz * dz);
        String where = dist < 6 ? "right next to you" : dist + " blocks " + compass(dx, dz) + (r.onIsland() != pl.getY() > 150 ? (r.onIsland() ? ", up on Neon Heights" : ", down in the city") : "");
        return "§b" + p.name + "§7 is " + doing + " §8(" + where + ", " + r.blockPosition().toShortString() + ")";
    }

    public static String friends(ServerPlayer pl, CityData d) {
        String pn = pl.getName().getString();
        List<CityData.Profile> known = new ArrayList<>();
        for (CityData.Profile p : d.profiles.values()) if (d.playerNames(p.id).contains(pn) && d.playerRel(p.id, pn).met) known.add(p);
        if (known.isEmpty()) return "§7You haven't met anyone yet. Right-click a resident to say hi!";
        known.sort(Comparator.comparingInt((CityData.Profile p) -> -d.playerRel(p.id, pn).aff));
        StringBuilder sb = new StringBuilder("§6§lYour Solaris friends §7(" + known.size() + "/" + d.profiles.size() + " met)");
        for (CityData.Profile p : known) {
            CityData.Rel r = d.playerRel(p.id, pn);
            int hearts = Math.max(0, Math.min(5, (r.aff + 10) / 20));
            String label = r.aff >= 80 ? "best friend" : r.aff >= 60 ? "close friend" : r.aff >= 30 ? "friend" : r.aff >= 0 ? "acquaintance" : "not a fan";
            sb.append("\n§c").append("❤".repeat(hearts)).append("§8").append("❤".repeat(5 - hearts)).append(" §f").append(p.name).append(" §7- ").append(label).append(" §8(").append(p.job.title).append(")");
        }
        return sb.toString();
    }

    public static String top(CityData d) {
        List<CityData.Profile> all = new ArrayList<>(d.profiles.values());
        StringBuilder sb = new StringBuilder("§6§lSolaris leaderboards");
        all.sort(Comparator.comparingInt((CityData.Profile p) -> -(p.coins + Bank.savings(d, p.id))));
        sb.append("\n§e💰 Richest: §f");
        for (int i = 0; i < Math.min(3, all.size()); i++) sb.append(i > 0 ? "§7, §f" : "").append(all.get(i).name).append(" §7(").append(all.get(i).coins + Bank.savings(d, all.get(i).id)).append(")§f");
        all.sort(Comparator.comparingInt((CityData.Profile p) -> -d.friendsOf(p.id).size()));
        sb.append("\n§e🤝 Most friends: §f");
        for (int i = 0; i < Math.min(3, all.size()); i++) sb.append(i > 0 ? "§7, §f" : "").append(all.get(i).name).append(" §7(").append(d.friendsOf(all.get(i).id).size()).append(")§f");
        all.sort(Comparator.comparingInt((CityData.Profile p) -> -p.fish));
        sb.append("\n§e🎣 Top anglers: §f");
        for (int i = 0; i < Math.min(3, all.size()); i++) sb.append(i > 0 ? "§7, §f" : "").append(all.get(i).name).append(" §7(").append(all.get(i).fish).append(")§f");
        all.sort(Comparator.comparingInt((CityData.Profile p) -> -p.mood()));
        sb.append("\n§e😊 Happiest: §f");
        for (int i = 0; i < Math.min(3, all.size()); i++) sb.append(i > 0 ? "§7, §f" : "").append(all.get(i).name).append(" §7(").append(all.get(i).mood()).append("%)§f");
        for (Map.Entry<String, Map<String, Integer>> g : d.scores.entrySet()) {
            String best = null;
            int bs = -1;
            for (Map.Entry<String, Integer> e : g.getValue().entrySet()) if (e.getValue() > bs) { bs = e.getValue(); best = e.getKey(); }
            if (best != null) sb.append("\n§e🕹 ").append(Computers.gameName(g.getKey())).append(": §f").append(best).append(" §7(").append(bs).append(")");
        }
        return sb.toString();
    }

    /* ------------------------------------------------------------ Morning bulletin + forecast */

    public static String forecast(ServerLevel sl) {
        if (!(sl.getLevelData() instanceof ServerLevelData w)) return "§7The forecast office is closed today.";
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (sl.isThundering()) return "§9⛈ Storms right now§7, clearing in about " + hours(w.getThunderTime()) + ".";
        if (sl.isRaining()) return "§9🌧 Rain right now§7, drying up in about " + hours(w.getRainTime()) + ".";
        int clear = w.getClearWeatherTime() > 0 ? w.getClearWeatherTime() : w.getRainTime();
        if (clear > 24000) return "§e☀ Clear skies§7 for the rest of the day" + (tod < 12000 ? " - perfect beach weather." : ".");
        return "§e☀ Clear for now§7, but rain is expected in about " + hours(clear) + ".";
    }

    static String hours(int ticks) {
        int h = Math.max(1, Math.round(ticks / 1000f));
        return h >= 24 ? "a day or more" : h + " hour" + (h == 1 ? "" : "s");
    }

    static void bulletin(ServerLevel sl, CityData d) {
        long day = Calendar.worldDay(sl);
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (tod < 500 || tod > 2500) return;
        List<String> heads = new ArrayList<>();
        for (int i = d.events.size() - 1; i >= 0 && heads.size() < 3; i--) {
            CityData.Event e = d.events.get(i);
            if (e.day == day - 1 || e.day == day) heads.add(Events.sentence(e.text));
        }
        for (ServerPlayer pl : sl.players()) {
            String pn = pl.getName().getString();
            if (d.setting(pn, "bulletin", "-1").equals(String.valueOf(day)) || d.setting(pn, "bulletinOff", "0").equals("1") || !opt(d, pn, "bulletin")) continue;
            d.setSetting(pn, "bulletin", String.valueOf(day));
            say(pl, "§6§l☀ Solaris Morning Bulletin §r§7· " + Calendar.stamp(day) + " · " + Skies.season(day));
            say(pl, "§7Weather: " + forecast(sl));
            if (heads.isEmpty()) say(pl, "§7Headlines: §fA quiet night in the city.");
            else for (String h : heads) say(pl, "§8• §f" + h + ".");
            for (String ev : Happenings.on(day)) say(pl, "§d✦ " + ev);
            for (ServerPlayer o : sl.players()) if (Quests.birthday(d, o.getName().getString(), day)) say(pl, "§d🎂 It's " + o.getName().getString() + "'s birthday!");
            int sick = 0;
            for (CityData.Profile p : d.profiles.values()) if (Health.sick(d, p.id, day)) sick++;
            if (sick > 0) say(pl, "§7" + sick + " resident" + (sick == 1 ? " is" : "s are") + " off sick today.");
            net.minecraft.network.chat.MutableComponent row = Component.literal("§7Today: ");
            for (String[] b : new String[][]{{"[Quests]", "/sol quests"}, {"[Treasure]", "/sol treasure"}, {"[Calendar]", "/sol calendar"}, {"[Horoscope]", "/sol horoscope"}}) {
                row.append(Component.literal("§e" + b[0] + " ").withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, b[1]))));
            }
            row.append(Component.literal("§8[off]").withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sol bulletin off"))));
            pl.sendSystemMessage(row);
        }
    }

    /* ------------------------------------------------------------ Courier jobs */

    static final String TAG = "SolCourier";

    static boolean isParcel(ItemStack st) {
        return st.is(Items.PAPER) && st.hasTag() && st.getTag().contains(TAG);
    }

    public static String cancelCourier(ServerPlayer pl) {
        int n = 0;
        for (int i = 0; i < pl.getInventory().getContainerSize(); i++) {
            ItemStack st = pl.getInventory().getItem(i);
            if (isParcel(st)) {
                n += st.getCount();
                pl.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        return n == 0 ? "§6[Courier] §7You're not carrying any parcels." : "§6[Courier] §7Parcel returned to Pip. No pay, no hard feelings.";
    }

    public static String courier(ServerPlayer pl, CityData d) {
        String pn = pl.getName().getString();
        ServerLevel sl = pl.serverLevel();
        long day = Calendar.worldDay(sl);
        for (int i = 0; i < pl.getInventory().getContainerSize(); i++) {
            ItemStack st = pl.getInventory().getItem(i);
            if (!isParcel(st)) continue;
            CityData.Profile to = d.profiles.get(st.getTag().getString(TAG));
            if (to == null || sl.getGameTime() > st.getTag().getLong("Due") + 24000) {
                pl.getInventory().setItem(i, ItemStack.EMPTY);
                say(pl, "§6[Courier] §7An old parcel was too late to deliver - Pip took it back.");
                continue;
            }
            return "§6[Courier] §7You're already carrying a parcel for §b" + to.name + "§7. " + whereis(pl, d, to.name) + " §8(/sol courier cancel to give it back)";
        }
        if (parse(d.setting(pn, "courierDay", "-1")) == day && parse(d.setting(pn, "courierCount", "0")) >= 5) return "§6[Courier] §7Pip says that's enough runs for today. Come back tomorrow!";
        List<Resident> targets = new ArrayList<>();
        for (CityData.Profile p : d.profiles.values()) {
            if (p.entity == null || !(sl.getEntity(p.entity) instanceof Resident r)) continue;
            double dd = r.distanceToSqr(pl);
            if (dd > 25 * 25 && dd < 180 * 180 && r.onIsland() == pl.getY() > 150) targets.add(r);
        }
        if (targets.isEmpty()) return "§6[Courier] §7No deliveries right now - nobody's within reach. Try again in a bit.";
        Resident r = targets.get(sl.getRandom().nextInt(targets.size()));
        CityData.Profile p = r.profile();
        int dist = (int) r.distanceTo(pl);
        int pay = 6 + dist / 10;
        String[] what = {"a birthday card", "a mystery box", "fresh flowers", "a book from the library", "spare keys", "a mixtape", "a lost scarf", "cookies from the bakery"};
        String item = what[sl.getRandom().nextInt(what.length)];
        ItemStack parcel = new ItemStack(Items.PAPER);
        parcel.getOrCreateTag().putString(TAG, p.id);
        parcel.getOrCreateTag().putInt("Pay", pay);
        parcel.getOrCreateTag().putLong("Due", sl.getGameTime() + 12000);
        parcel.setHoverName(Component.literal("§6Parcel for " + p.name + " §7(" + item + ")"));
        if (!pl.getInventory().add(parcel)) pl.drop(parcel, false);
        return "§6[Courier] §fDeliver " + item + " to §b" + p.name + "§f - hand it over by right-clicking them. §7Pays §e" + pay + "§7 coins, faster is better. " + whereis(pl, d, p.name);
    }

    /** Called when a player right-clicks a resident; true if a parcel was handed over. */
    public static boolean deliver(ServerPlayer pl, Resident r, CityData d) {
        ItemStack st = pl.getMainHandItem();
        if (!isParcel(st)) return false;
        CityData.Profile p = r.profile();
        if (p == null) return false;
        String to = st.getTag().getString(TAG);
        if (!to.equals(p.id)) {
            CityData.Profile q = d.profiles.get(to);
            r.sayTo(r.pick("That's not for me - it says " + (q == null ? "someone else" : q.name) + " on it.", "Wrong door! This one's for " + (q == null ? "someone else" : q.name) + "."), 60);
            return true;
        }
        ServerLevel sl = pl.serverLevel();
        int pay = st.getTag().getInt("Pay");
        boolean late = sl.getGameTime() > st.getTag().getLong("Due");
        if (late) pay = Math.max(2, pay / 2);
        st.shrink(1);
        String pn = pl.getName().getString();
        long day = Calendar.worldDay(sl);
        if (parse(d.setting(pn, "courierDay", "-1")) != day) {
            d.setSetting(pn, "courierDay", String.valueOf(day));
            d.setSetting(pn, "courierCount", "0");
        }
        d.setSetting(pn, "courierCount", String.valueOf(parse(d.setting(pn, "courierCount", "0")) + 1));
        d.setSetting(pn, "couriers", String.valueOf(parse(d.setting(pn, "couriers", "0")) + 1));
        reward(pl, d, pay, "Courier delivery to " + p.name);
        Quests.bump(d, pn, "courier");
        if (!late) Letters.thankYou(sl, d, pn, p, "bringing my parcel");
        CityData.Rel rel = d.playerRel(p.id, pn);
        rel.met = true;
        rel.aff = Math.min(100, rel.aff + 2);
        r.showItem("minecraft:paper", 40);
        r.gesture(Resident.G_CHEER, 40);
        r.sayTo(late ? r.pick("Finally! I was starting to think it got lost.", "Better late than never, I suppose. Thanks, " + pn + ".") : r.pick("Oh, my parcel! Thank you, " + pn + "!", "Right on time! You're a star, " + pn + ".", "Ooh, I've been waiting for this! Thanks!"), 70);
        say(pl, "§6[Courier] §aDelivered! §e+" + pay + " coins" + (late ? " §7(late - half pay)" : "") + ". §7Type §f/sol courier§7 for another.");
        checkAchievements(pl, d);
        return true;
    }

    /* ------------------------------------------------------------ Tick */

    public static void tick(ServerLevel sl, CityData d) {
        long gt = sl.getGameTime();
        if (gt % 200 == 61) for (ServerPlayer pl : sl.players()) checkAchievements(pl, d);
        if (gt % 100 == 43) bulletin(sl, d);
        if (gt % 40 == 3) for (ServerPlayer pl : sl.players()) courierCompass(sl, d, pl);
        if (gt % 1200 == 77) for (ServerPlayer pl : sl.players()) d.setSetting(pl.getName().getString(), "lastSeenDay", String.valueOf(Calendar.worldDay(sl)));
    }

    static void courierCompass(ServerLevel sl, CityData d, ServerPlayer pl) {
        ItemStack st = pl.getMainHandItem();
        if (!isParcel(st) || !opt(d, pl.getName().getString(), "hints")) return;
        CityData.Profile p = d.profiles.get(st.getTag().getString(TAG));
        if (p == null) return;
        Resident r = find(sl, d, p.name);
        if (r == null) {
            pl.displayClientMessage(Component.literal("§6📦 " + p.name + " §7- out of range"), true);
            return;
        }
        double dx = r.getX() - pl.getX(), dz = r.getZ() - pl.getZ();
        int dist = (int) Math.sqrt(dx * dx + dz * dz);
        long left = Math.max(0, st.getTag().getLong("Due") - sl.getGameTime()) / 20;
        pl.displayClientMessage(Component.literal("§6📦 " + p.name + " §f" + (dist < 5 ? "right here!" : dist + " blocks " + compass(dx, dz)) + " §7· " + (left > 0 ? left / 60 + "m " + left % 60 + "s left" : "§clate")), true);
    }
}
