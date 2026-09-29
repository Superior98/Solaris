package com.fireheart.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Server side of the Fireheart PC: registry of placed PCs, FireOS data, messages, high scores, residents buying and using PCs. */
public final class Computers {
    private Computers() {}

    public static final int PRICE = 90;
    public static final String[] GAMES = {"snake", "blocks", "mines", "2048", "flap", "drift", "foxrun", "maze3d", "beat"};
    public static final String[] GAME_NAMES = {"Snake", "Blocks", "Mines", "2048", "Ferry Flap", "Neon Drift", "Stellar Fox Run", "Neon Maze 3D", "Beat Solaris"};
    public static final String[] VIDEOS = {"Slime Tries To Jump (Gone Wrong)", "Sky Ferry Timelapse", "Neon Heights at Night - 4K", "Cooking with Leo: The Double Diesel", "Fireworks Over the Pier", "Top 10 Clock Tower Facts", "Solaris Skyline: Day to Night", "Festival of the Founder Highlights", "Deep Blue: Life Under Solaris Bay"};
    static final Map<UUID, BlockPos> OPEN = new HashMap<>();
    public record Pending(String player, String resident, String text, long due) {}
    public static final List<Pending> PENDING = new ArrayList<>();
    private static long purchaseDay = -1, textDay = -1;

    public static void register(ServerLevel sl, BlockPos pos) {
        CityData d = CityData.get(sl);
        d.pcs.putIfAbsent(pos.asLong(), "");
        d.setDirty();
    }

    public static void unregister(ServerLevel sl, BlockPos pos) {
        CityData d = CityData.get(sl);
        d.pcs.remove(pos.asLong());
        for (CityData.Profile p : d.profiles.values()) if (pos.equals(p.pcPos)) p.pcPos = null;
        d.setDirty();
    }

    public static boolean isPc(ServerLevel sl, BlockPos pos) {
        return pos != null && sl.isLoaded(pos) && sl.getBlockState(pos).getBlock() instanceof ComputerBlock;
    }

    public static void setScreen(ServerLevel sl, BlockPos pos, int v) {
        if (pos != null && sl.isLoaded(pos) && sl.getBlockState(pos).getBlock() instanceof TvBlock) {
            BlockState ts = sl.getBlockState(pos);
            if (ts.getValue(TvBlock.ON) != (v > 0)) sl.setBlock(pos, ts.setValue(TvBlock.ON, v > 0), 3);
            return;
        }
        if (!isPc(sl, pos)) return;
        BlockState st = sl.getBlockState(pos);
        if (st.getValue(ComputerBlock.SCREEN) != v) sl.setBlock(pos, st.setValue(ComputerBlock.SCREEN, v), 3);
    }

    public static void openFor(ServerPlayer pl, BlockPos pos) {
        OPEN.put(pl.getUUID(), pos);
        PcNet.send(pl, data(pl, pos));
    }

    static String key(ServerPlayer pl) {
        return Bank.playerKey(pl.getName().getString());
    }

    public static PcNet.Data data(ServerPlayer pl, BlockPos pos) {
        ServerLevel sl = pl.serverLevel();
        CityData d = CityData.get(sl);
        String name = pl.getName().getString();
        String k = Bank.playerKey(name);
        PcNet.Data o = new PcNet.Data();
        o.pos = pos;
        String own = d.pcs.getOrDefault(pos.asLong(), "");
        CityData.Profile op = own.isEmpty() ? null : d.profiles.get(own);
        o.owner = op == null ? "" : op.name;
        o.device = sl.getBlockState(pos).getBlock() instanceof ConsoleBlock ? 5 : pos.equals(BlockPos.ZERO) ? 1 : pos.equals(Extras.TABLET) ? 2 : pos.equals(Extras.CONSOLE) ? 3 : d.tvs.containsKey(pos.asLong()) || sl.getBlockState(pos).getBlock() instanceof TvBlock ? 4 : 0;
        o.kiosk = "store".equals(own);
        if (o.device == 5) { BlockPos tv = ConsoleBlock.findTv(sl, pos); o.tv = tv == null ? 0 : tv.asLong(); }
        else if (o.device == 4) o.tv = pos.asLong();
        o.player = name;
        o.clock = Calendar.name(Calendar.worldDay(sl)) + " " + Calendar.clock(sl.getDayTime());
        o.notes = d.notes.getOrDefault(k, "");
        o.coins = Bank.cash(pl);
        o.savings = Bank.savings(d, k);
        o.hasAccount = Bank.holder(d, name) != null;
        for (int i = d.news.size() - 1; i >= 0 && o.news.size() < 18; i--) o.news.add(d.news.get(i));
        for (CityData.Profile p : d.profiles.values()) o.residents.add(new String[]{p.id, p.name, p.jobTitle(), p.ownsPC ? "1" : "0", p.ownsPhone ? "1" : "0", String.valueOf(Cast.skinFor(p.id, p.skin))});
        PlayerLink.contacts(pl, d, o.residents);
        for (int i = d.uploads.size() - 1; i >= 0 && o.uploads.size() < 30; i--) o.uploads.add(d.uploads.get(i));
        List<String> box = d.inbox.getOrDefault(k, List.of());
        for (int i = Math.max(0, box.size() - 80); i < box.size(); i++) o.inbox.add(box.get(i));
        for (CityData.Profile p : d.profiles.values()) {
            int n = unread(d, k, p.id);
            if (n > 0) o.unread.add(p.id + "|" + n);
        }
        String acct = Bank.sav(k);
        for (int i = d.ledger.size() - 1; i >= 0 && o.statement.size() < 10; i--) {
            CityData.Tx t = d.ledger.get(i);
            if (!t.from.equals(acct) && !t.to.equals(acct)) continue;
            boolean in = t.to.equals(acct);
            o.statement.add(Calendar.name(t.day).substring(0, 3) + " " + (in ? "+" : "-") + t.amount + " " + t.memo);
        }
        for (String g : GAMES) {
            Map<String, Integer> m = d.scores.getOrDefault(g, Map.of());
            m.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(6).forEach(e -> o.scores.add(g + "|" + e.getKey() + "|" + e.getValue()));
        }
        Phones.fill(pl, d, o);
        Extras.fill(pl, d, o);
        for (Extras.Group g : Extras.groups(d, name)) {
            int n = unread(d, k, g.id());
            if (n > 0) o.unread.add(g.id() + "|" + n);
        }
        return o;
    }

    public static void action(ServerPlayer pl, PcNet.Act a) {
        ServerLevel sl = pl.serverLevel();
        CityData d = CityData.get(sl);
        String k = key(pl);
        String name = pl.getName().getString();
        switch (a.kind) {
            case "close" -> {
                OPEN.remove(pl.getUUID());
                boolean other = OPEN.values().stream().anyMatch(p -> p.equals(a.pos));
                if (!other) setScreen(sl, a.pos, 0);
            }
            case "voice_req" -> VoiceServer.request(pl, a.a, a.b);
            case "vehicle" -> Vehicles.act(pl, a.a);
            case "game" -> setScreen(sl, a.pos, 2);
            case "video" -> setScreen(sl, a.pos, 3);
            case "desktop" -> setScreen(sl, a.pos, 1);
            case "notes" -> {
                String t = a.a.length() > 4000 ? a.a.substring(0, 4000) : a.a;
                d.notes.put(k, t);
                d.setDirty();
            }
            case "msg" -> {
                if (PlayerLink.isPlayer(a.a)) {
                    String text = a.b.trim();
                    if (!text.isEmpty()) PlayerLink.text(pl, d, a.a, text);
                    return;
                }
                if (a.a.startsWith("group:")) {
                    String text = a.b.trim();
                    if (!text.isEmpty()) Extras.groupMessage(pl, d, a.a, text.length() > 200 ? text.substring(0, 200) : text);
                    return;
                }
                CityData.Profile p = d.profiles.get(a.a);
                String text = a.b.trim();
                if (p == null || text.isEmpty()) return;
                if (text.length() > 200) text = text.substring(0, 200);
                addInbox(d, k, p.id, ">", Calendar.worldDay(sl), text);
                PcNet.send(pl, new PcNet.Msg(p.id + "|>|" + Calendar.worldDay(sl) + "|" + text));
                long delay = p.ownsPhone ? 900 + sl.random.nextInt(300) : p.ownsPC ? 40 + sl.random.nextInt(80) : 200 + sl.random.nextInt(300);
                PENDING.add(new Pending(name, p.id, text, sl.getGameTime() + delay));
                Receipts.residentMessage(sl, name, p, sl.getGameTime() + delay);
                d.playerRel(p.id, name).met = true;
            }
            case "score" -> {
                int score;
                try { score = Integer.parseInt(a.b); } catch (NumberFormatException e) { return; }
                boolean ok = false;
                for (String g : GAMES) if (g.equals(a.a)) ok = true;
                if (!ok || score <= 0) return;
                int best = record(d, a.a, name, score);
                if (best == score) {
                    String top = topName(d, a.a);
                    if (name.equals(top)) d.news(Calendar.worldDay(sl), name + " set a new " + gameName(a.a) + " record on SolOS: " + score + "!");
                }
                PcNet.send(pl, data(pl, a.pos));
            }
            case "refresh" -> PcNet.send(pl, data(pl, a.pos));
            case "read" -> {
                if (d.profiles.containsKey(a.a) || a.a.startsWith("group:")) markRead(d, k, a.a);
                else if (PlayerLink.isPlayer(a.a)) { markRead(d, k, a.a); Receipts.playerRead(pl, a.a); }
            }
            case "typing" -> Receipts.playerTyping(pl, a.a, a.b.equals("1"));
            default -> Phones.action(pl, a);
        }
    }

    static String gameName(String g) {
        for (int i = 0; i < GAMES.length; i++) if (GAMES[i].equals(g)) return GAME_NAMES[i];
        return g;
    }

    public static int record(CityData d, String game, String who, int score) {
        Map<String, Integer> m = d.scores.computeIfAbsent(game, x -> new java.util.LinkedHashMap<>());
        int prev = m.getOrDefault(who, 0);
        if (score > prev) m.put(who, score);
        d.setDirty();
        return Math.max(prev, score);
    }

    static String topName(CityData d, String game) {
        Map<String, Integer> m = d.scores.get(game);
        if (m == null || m.isEmpty()) return "";
        return m.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
    }

    public static int unread(CityData d, String key, String resident) {
        int in = d.inboxIn.getOrDefault(key, Map.of()).getOrDefault(resident, 0);
        int seen = d.inboxSeen.getOrDefault(key, Map.of()).getOrDefault(resident, 0);
        return Math.max(0, in - seen);
    }

    public static void markRead(CityData d, String key, String resident) {
        int in = d.inboxIn.getOrDefault(key, Map.of()).getOrDefault(resident, 0);
        d.inboxSeen.computeIfAbsent(key, x -> new java.util.LinkedHashMap<>()).put(resident, in);
        d.setDirty();
    }

    static void addInbox(CityData d, String key, String resident, String dir, long day, String text) {
        List<String> l = d.inbox.computeIfAbsent(key, x -> new ArrayList<>());
        l.add(resident + "|" + dir + "|" + day + "|" + text.replace("|", "/"));
        while (l.size() > 150) l.remove(0);
        if (dir.equals("<") || dir.equals("~")) d.inboxIn.computeIfAbsent(key, x -> new java.util.LinkedHashMap<>()).merge(resident, 1, Integer::sum);
        d.setDirty();
    }

    public static void deliver(ServerLevel sl, CityData d, String player, String resident, String text) {
        String k = Bank.playerKey(player);
        long day = Calendar.worldDay(sl);
        addInbox(d, k, resident, "<", day, text);
        ServerPlayer pl = sl.getServer().getPlayerList().getPlayerByName(player);
        if (pl == null) return;
        CityData.Profile p = d.profiles.get(resident);
        String who = resident.startsWith("group:") ? Extras.displayName(d, player, resident) : PlayerLink.isPlayer(resident) ? resident.substring(7) : p == null ? resident : p.name;
        if (OPEN.containsKey(pl.getUUID()) || Phones.OPEN_PHONE.contains(pl.getUUID())) PcNet.send(pl, new PcNet.Msg(resident + "|<|" + day + "|" + text));
        if (Phones.OPEN_PHONE.contains(pl.getUUID())) return;
        if (Phones.playerHasPhone(pl) || Phones.hasWatch(pl)) Phones.toast(pl, who, text);
        else if (!OPEN.containsKey(pl.getUUID())) pl.displayClientMessage(Component.literal("§b[SolNet] §fNew message from §e" + who + "§f - read it on a Solaris PC or SolPhone."), true);
    }

    /* ---------------------------------------------------------------- ticking */

    public static void tick(ServerLevel sl, CityData d) {
        long now = sl.getGameTime();
        OPEN.entrySet().removeIf(e -> {
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(e.getKey());
            if (p != null && p.blockPosition().closerThan(e.getValue(), 8)) return false;
            setScreen(sl, e.getValue(), 0);
            return true;
        });
        Receipts.tick(sl);
        Iterator<Pending> it = PENDING.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (now < p.due) continue;
            it.remove();
            reply(sl, d, p);
        }
        long day = Calendar.worldDay(sl);
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (tod >= 9000 && tod < 12000 && purchaseDay != day) {
            purchaseDay = day;
            purchases(sl, d, day);
        }
        if (tod >= 7000 && tod < 9000 && textDay != day) {
            textDay = day;
            unpromptedTexts(sl, d, day);
        }
        if (now % 1200 == 0) for (CityData.Profile p : d.profiles.values()) if (p.ownsPC && p.pcPos == null) placeAtHome(sl, d, p);
    }

    public static void reply(ServerLevel sl, CityData d, Pending p) {
        CityData.Profile rp = d.profiles.get(p.resident);
        if (rp == null) return;
        ServerPlayer pl = sl.getServer().getPlayerList().getPlayerByName(p.player);
        Entity e = rp.entity == null ? null : sl.getEntity(rp.entity);
        String text;
        if (!(e instanceof Resident r) || pl == null) text = "Sorry, I was away from my computer! Talk soon :)";
        else if (r.isSleeping() || r.activityName().equals("sleep")) text = "zzz... sorry, just saw this. I was asleep! Talk tomorrow?";
        else {
            String t = " " + p.text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9' ]", " ").replaceAll("\\s+", " ") + " ";
            text = Chat.reply(pl, r, t, true);
            if (text == null || text.isEmpty()) text = "Haha :)";
            if (!rp.ownsPC && !rp.ownsPhone && sl.random.nextFloat() < 0.5f) text += " (sent from the library computer)";
        }
        deliver(sl, d, p.player, p.resident, text);
        rp.log(Calendar.day(sl)).note("I messaged " + p.player + " on SolNet");
    }

    static void unpromptedTexts(ServerLevel sl, CityData d, long day) {
        for (ServerPlayer pl : sl.players()) {
            String pn = pl.getName().getString();
            for (CityData.Profile p : d.profiles.values()) {
                if (!p.ownsPC && !p.ownsPhone || p.mind.trustIn(pn) < 10 || sl.random.nextFloat() > 0.3f) continue;
                String dr = p.mind.dreamToday(day);
                String text;
                Mind.Ep mem = p.mind.best(day + 1, x -> x.involves("@" + pn) && x.emo > 0);
                Integer best = null;
                String game = GAMES[sl.random.nextInt(GAMES.length)];
                Map<String, Integer> m = d.scores.get(game);
                if (m != null) best = m.get(p.name);
                if (dr != null && sl.random.nextBoolean()) text = "Morning! " + dr + " lol";
                else if (best != null && sl.random.nextBoolean()) text = "Just got " + best + " on " + gameName(game) + ". Bet you can't beat it ;)";
                else if (mem != null) text = "Hey " + pn + "! Still thinking about " + Calendar.relative(mem.day, day) + " - " + mem.forPlayer(pn) + ". Thanks again!";
                else if (!p.mind.intent.isEmpty() && p.mind.intentDay == day) text = "Hey! Today I'm going to " + p.mind.intent + ". Wish me luck!";
                else text = "Hi " + pn + "! Just saying hi from my new PC :)";
                deliver(sl, d, pn, p.id, text);
            }
        }
    }

    static void purchases(ServerLevel sl, CityData d, long day) {
        long rd = Calendar.day(sl);
        int tod = (int) Math.floorMod(sl.getDayTime(), 24000L);
        for (CityData.Profile p : d.profiles.values()) {
            if (p.ownsPC || p.pcDeliverDay >= 0) continue;
            int money = p.coins + Bank.savings(d, p.id);
            if (money < PRICE + 30) continue;
            int chance = switch (p.trait) {
                case ADVENTUROUS, CURIOUS, TALKATIVE -> 30;
                case GRUMPY -> 5;
                default -> 12;
            };
            if (p.job == Job.ARCADE_KEEPER || p.job == Job.CLERK) chance += 25;
            boolean usedPublic = p.mind.eps.stream().anyMatch(e -> e.kind.equals("pc"));
            if (usedPublic) chance += 20;
            if (sl.random.nextInt(100) >= chance || !p.wantDevice.isEmpty()) continue;
            TechStore.want(d, p, "pc", day);
        }
    }

    public static boolean buy(ServerLevel sl, CityData d, CityData.Profile p, long day, long rd, int tod) {
        return buy(sl, d, p, day, rd, tod, true);
    }

    public static boolean buy(ServerLevel sl, CityData d, CityData.Profile p, long day, long rd, int tod, boolean charge) {
        if (!charge) {
            p.ownsPC = true;
            p.log(rd).note("I bought my very own Solaris PC from SolTech");
            p.fun = Math.min(100, p.fun + 20);
            d.event(day, "shopping", p.name + " bought a brand new Solaris PC", null, p.id);
            placeAtHome(sl, d, p);
            d.setDirty();
            return true;
        }
        int fromSav = Math.min(PRICE, Bank.savings(d, p.id));
        if (p.coins < PRICE && fromSav > 0) d.pay(Bank.sav(p.id), p.id, Math.min(fromSav, PRICE - p.coins), "Withdrawal for a gaming PC", rd, tod, false);
        if (p.coins < PRICE) return false;
        d.pay(p.id, "biz:supply", PRICE, "Solaris PC", rd, tod, false);
        p.ownsPC = true;
        p.log(rd).note("I bought my very own Solaris PC from Create Supply Co.");
        p.fun = Math.min(100, p.fun + 20);
        d.event(day, "shopping", p.name + " bought a brand new Solaris PC", null, p.id);
        placeAtHome(sl, d, p);
        d.setDirty();
        return true;
    }

    public static boolean placeAtHome(ServerLevel sl, CityData d, CityData.Profile p) {
        Place home = p.homePlace();
        if (home == null || !sl.isLoaded(home.pos)) return false;
        BlockPos best = null;
        Direction face = null;
        double bd = Double.MAX_VALUE;
        for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) for (int dy = -1; dy <= 1; dy++) {
            BlockPos c = home.pos.offset(dx, dy, dz);
            if (c.equals(home.pos) || !sl.getBlockState(c).isAir() || !sl.getBlockState(c.above()).isAir()) continue;
            if (!sl.getBlockState(c.below()).isFaceSturdy(sl, c.below(), Direction.UP)) continue;
            Direction wall = null;
            boolean bad = false;
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockState n = sl.getBlockState(c.relative(dir));
                if (n.getBlock() instanceof DoorBlock || n.getBlock() instanceof BedBlock || n.getBlock() instanceof ComputerBlock) bad = true;
                if (n.isFaceSturdy(sl, c.relative(dir), dir.getOpposite()) && sl.getBlockState(c.relative(dir).above()).isFaceSturdy(sl, c.relative(dir).above(), dir.getOpposite())) wall = dir;
            }
            if (bad || wall == null) continue;
            BlockPos front = c.relative(wall.getOpposite());
            if (!sl.getBlockState(front).isAir() || !sl.getBlockState(front.above()).isAir()) continue;
            double dist = c.distSqr(home.pos) + Math.abs(c.getY() - home.pos.getY()) * 40;
            if (dist < 4) continue;
            if (dist < bd) { bd = dist; best = c; face = wall.getOpposite(); }
        }
        if (best == null) return false;
        sl.setBlock(best, FireheartCity.COMPUTER.get().defaultBlockState().setValue(ComputerBlock.FACING, face), 3);
        d.pcs.put(best.asLong(), p.id);
        p.pcPos = best;
        d.setDirty();
        FireheartCity.LOG.info("Placed " + p.name + "'s PC at " + best.toShortString());
        return true;
    }

    public static BlockPos publicPcNear(ServerLevel sl, CityData d, BlockPos at, int r) {
        BlockPos best = null;
        double bd = r * r;
        for (Map.Entry<Long, String> e : d.pcs.entrySet()) {
            if (!e.getValue().isEmpty()) continue;
            BlockPos p = BlockPos.of(e.getKey());
            double dd = p.distSqr(at);
            if (dd < bd && isPc(sl, p) && !OPEN.containsValue(p)) { bd = dd; best = p; }
        }
        return best;
    }

    public static int residentScore(net.minecraft.util.RandomSource r, CityData.Profile p, String game) {
        double skill = switch (p.trait) {
            case CURIOUS, ADVENTUROUS -> 1.2;
            case TALKATIVE, CHEERFUL -> 1.0;
            case SHY -> 1.1;
            default -> 0.85;
        };
        if (p.job == Job.ARCADE_KEEPER) skill += 0.6;
        double roll = 0.4 + r.nextDouble() * 0.8;
        double base = switch (game) {
            case "snake" -> 28;
            case "blocks" -> 1400;
            case "mines" -> 120;
            case "2048" -> 2600;
            default -> 14;
        };
        return Math.max(1, (int) (base * skill * roll));
    }

    /* ---------------------------------------------------------------- residents using FireOS apps */

    public static String chooseApp(Resident r, CityData.Profile p, boolean pub) {
        CityData d = r.data();
        net.minecraft.util.RandomSource rnd = r.getRandom();
        java.util.LinkedHashMap<String, Integer> w = new java.util.LinkedHashMap<>();
        w.put("game", p.job == Job.ARCADE_KEEPER ? 50 : 32);
        w.put("feed", p.ownsPhone ? 10 : 22);
        w.put("news", p.trait == Trait.CURIOUS ? 20 : 11);
        w.put("tube", p.fun < 40 ? 26 : 16);
        if (Bank.savings(d, p.id) > 0) w.put("bank", p.goalCost > 0 ? 10 : 5);
        if (!d.friendsOf(p.id).isEmpty() || !p.partner.isEmpty()) w.put("msg", p.trait == Trait.TALKATIVE ? 16 : 9);
        if (!p.ownsPhone && p.coins + Bank.savings(d, p.id) >= Phones.PRICE) w.put("shop", 14);
        if (p.trait == Trait.DREAMY || p.trait == Trait.SHY) w.put("notes", 8);
        int total = 0;
        for (int v : w.values()) total += v;
        int roll = rnd.nextInt(Math.max(1, total));
        for (java.util.Map.Entry<String, Integer> e : w.entrySet()) {
            roll -= e.getValue();
            if (roll < 0) return e.getKey();
        }
        return "game";
    }

    public static String appChatter(Resident r, CityData.Profile p, String app) {
        return switch (app) {
            case "feed" -> r.pick("Haha, look at this post...", "Aww, cute!", "Everyone's on SolFeed tonight.", null);
            case "news" -> r.pick("Huh, interesting...", "Wait, really?", "Ooh, what's this?", null);
            case "tube" -> r.pick("HAHAHA!", "Okay, one more video...", "This slime video gets me every time.", "Wait, rewind that!");
            case "bank" -> r.pick("Okay... budgeting time.", "Ooh, interest came in!", "Where did all my coins go?", null);
            case "msg" -> r.pick("Typing, typing...", "Hehe, sent!", null);
            case "shop" -> r.pick("Ooh, look at the " + Phones.colorName(TechStore.pickColor(p)) + " SolPhone...", "Do I need a new phone? ...Yes.", null);
            case "notes" -> r.pick("Dear diary...", "Hmm, how do I say this...", null);
            default -> r.pick("One more round...", "Come on, come on!", "Nooo, so close!", "This level is impossible!", "Ooh, nearly!", "Okay, focus...");
        };
    }

    public static void appDone(Resident r, CityData.Profile p, String app, boolean pub) {
        ServerLevel sl = (ServerLevel) r.level();
        CityData d = r.data();
        long day = Calendar.worldDay(sl);
        long rd = r.routineDay();
        int tod = (int) Math.floorMod(sl.getDayTime(), 24000L);
        DayLog lg = p.log(rd);
        switch (app) {
            case "feed" -> Phones.browse(r, p, true);
            case "news" -> {
                int learned = 0;
                for (int i = d.events.size() - 1; i >= 0 && i >= d.events.size() - 12 && learned < 2; i--) {
                    CityData.Event e = d.events.get(i);
                    if (p.known.contains(e.id) || e.text.contains(p.name)) continue;
                    p.learn(e.id);
                    p.heard(e.id, "SolNet");
                    if (learned == 0) {
                        p.mind.remember(p, day, tod, "news", "I read on SolNet that " + e.text, "", 0, 3);
                        if (r.getRandom().nextFloat() < 0.5f) r.say("Whoa - " + e.text + "!", 60);
                    }
                    learned++;
                }
                if (lg.once("pc:news")) lg.note("I caught up on the news on SolNet");
                p.fun = Math.min(100, p.fun + 3);
            }
            case "tube" -> {
                String v = VIDEOS[r.getRandom().nextInt(VIDEOS.length)];
                if (lg.once("pc:tube")) lg.note("I watched \"" + v + "\" on SolTube");
                p.fun = Math.min(100, p.fun + 14);
                r.say(r.pick("That was hilarious!", "Okay, that's enough SolTube for today.", "I need to show everyone that video."), 50);
            }
            case "bank" -> {
                int sv = Bank.savings(d, p.id);
                if (lg.once("pc:bank")) lg.note("I checked my savings online: " + sv + " coins");
                r.say(p.goalCost > 0 ? sv + " of " + p.goalCost + " saved for " + p.goal + ". Getting there!" : r.pick("Savings looking healthy!", sv + " coins in the bank. Not bad!"), 60);
            }
            case "msg" -> {
                java.util.List<String> fr = new java.util.ArrayList<>(d.friendsOf(p.id));
                if (!p.partner.isEmpty()) fr.add(p.partner);
                String sent = null;
                for (ServerPlayer pl : sl.players()) {
                    String pn = pl.getName().getString();
                    if (p.mind.trustIn(pn) >= 10 && r.getRandom().nextFloat() < 0.35f) {
                        deliver(sl, d, pn, p.id, r.pick("Hey " + pn + "! What are you up to?", "Hi " + pn + "! Just wanted to say hi ☺", "Bored at home... entertain me " + pn + " lol"));
                        sent = pn;
                        break;
                    }
                }
                if (sent == null && !fr.isEmpty()) {
                    CityData.Profile q = d.profiles.get(fr.get(r.getRandom().nextInt(fr.size())));
                    if (q != null && q.ownsPhone) {
                        String[] m = Phones.message(sl, d, r, p, q, day, tod, r.getRandom());
                        if (m != null) { Phones.text(d, p.id, q.id, m[0], m[1], m[2], day, tod); sent = q.name; }
                    }
                }
                if (sent != null && lg.once("pc:msg")) lg.note("I messaged " + sent + " on SolNet");
                p.social = Math.min(100, p.social + 6);
            }
            case "shop" -> {
                if (!p.ownsPhone && p.wantDevice.isEmpty()) {
                    TechStore.want(d, p, "phone", day);
                    r.say(r.pick("That's it - I'm getting a SolPhone. SolTech, here I come!", "I need that phone. Tomorrow, I'm going to SolTech."), 70);
                } else if ("phone".equals(p.wantDevice) && r.getRandom().nextBoolean()) {
                    TechStore.orderOnline(sl, d, p, day);
                    if (p.ownsPhone) r.say("Ordered it online! Wait... it's here already? Wow.", 60);
                }
            }
            case "notes" -> {
                if (lg.once("pc:notes")) lg.note("I wrote in my diary on my PC");
                p.fun = Math.min(100, p.fun + 4);
            }
            default -> {}
        }
        d.setDirty();
    }
}
