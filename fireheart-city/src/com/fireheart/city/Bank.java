package com.fireheart.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.phys.Vec3;

/** Fireheart City Bank: savings, business accounts, loans, weekly interest, the teller and the cash machine. */
public final class Bank {
    private Bank() {}

    public static final String KEY = "bank";
    public static final String ATM = "atm";
    public static final String RESERVE = "biz:bank";
    public static final BlockPos WINDOW = new BlockPos(-40, 71, 22);
    public static final BlockPos TELLER = new BlockPos(-42, 71, 22);
    public static final BlockPos COUNTER = new BlockPos(-41, 71, 22);
    public static final BlockPos ATM_SPOT = new BlockPos(-35, 71, 26);
    public static final BlockPos ATM_SCREEN = new BlockPos(-36, 72, 26);
    public static final BlockPos VAULT_GOLD = new BlockPos(-38, 71, 17);
    public static final BlockPos VAULT_STAND = new BlockPos(-40, 71, 17);
    public static final BlockPos ATM_BACK = new BlockPos(-37, 72, 27);
    public static final BlockPos ATM_BACK_STAND = new BlockPos(-38, 71, 27);
    public static final BlockPos DESK = new BlockPos(-42, 71, 26);
    public static final BlockPos DESK_STAND = new BlockPos(-43, 71, 25);
    public static final BlockPos LEDGER = new BlockPos(-43, 71, 23);
    public static final BlockPos LEDGER_STAND = new BlockPos(-42, 71, 23);
    public static final BlockPos BOARD = new BlockPos(-40, 71, 20);
    public static final BlockPos BELL = new BlockPos(-41, 72, 23);
    public static final int RATE = 3;

    public static int rate(CityData d) {
        return "savings".equals(d.civic.policy) ? 5 : RATE;
    }

    static String lbl(String s) {
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }
    public static final int LOAN_RATE = 10;
    public static final int SESSION = 3600;
    public static boolean debug;

    static void log(String s) {
        if (debug) FireheartCity.LOG.info("[Bank] " + s);
    }

    public static final class Loan {
        public String who = "";
        public int principal;
        public int owed;
        public int perDay;
        public long taken;
        public long lastPaid;
        public int missed;
        public int paid;
        public String purpose = "";

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putString("who", who);
            t.putInt("principal", principal);
            t.putInt("owed", owed);
            t.putInt("perDay", perDay);
            t.putLong("taken", taken);
            t.putLong("lastPaid", lastPaid);
            t.putInt("missed", missed);
            t.putInt("paid", paid);
            t.putString("purpose", purpose);
            return t;
        }

        static Loan load(CompoundTag t) {
            Loan l = new Loan();
            l.who = t.getString("who");
            l.principal = t.getInt("principal");
            l.owed = t.getInt("owed");
            l.perDay = t.getInt("perDay");
            l.taken = t.getLong("taken");
            l.lastPaid = t.getLong("lastPaid");
            l.missed = t.getInt("missed");
            l.paid = t.getInt("paid");
            l.purpose = t.getString("purpose");
            return l;
        }
    }

    public static final class Holder {
        public String name = "";
        public int number;
        public long opened;
        public int paidOut;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putString("name", name);
            t.putInt("number", number);
            t.putLong("opened", opened);
            t.putInt("paidOut", paidOut);
            return t;
        }

        static Holder load(CompoundTag t) {
            Holder h = new Holder();
            h.name = t.getString("name");
            h.number = t.getInt("number");
            h.opened = t.getLong("opened");
            h.paidOut = t.getInt("paidOut");
            return h;
        }
    }

    public static String sav(String owner) {
        return "sav:" + owner;
    }

    public static String playerKey(String name) {
        return "player:" + name;
    }

    public static int savings(CityData d, String owner) {
        return d.savings.getOrDefault(owner, 0);
    }

    public static Loan loan(CityData d, String owner) {
        Loan l = d.loans.get(owner);
        return l == null || l.owed <= 0 ? null : l;
    }

    static long day(ServerLevel l) {
        return Calendar.day(l);
    }

    static int tod(ServerLevel l) {
        return (int) Math.floorMod(l.getDayTime(), 24000L);
    }

    public static boolean inBuilding(BlockPos p) {
        return p.getX() >= -44 && p.getX() <= -36 && p.getZ() >= 15 && p.getZ() <= 28 && p.getY() >= 70 && p.getY() <= 78;
    }

    public static boolean isAtmBlock(BlockPos p) {
        int x = p.getX(), y = p.getY(), z = p.getZ();
        if (z < 26 || z > 27) return false;
        return x == -36 && y >= 71 && y <= 73 || x == -35 && y >= 72 && y <= 73;
    }

    public static int keep(CityData.Profile p) {
        return switch (p.trait) {
            case SHY, CURIOUS, GRUMPY -> 15;
            case FRIENDLY, CHEERFUL -> 25;
            default -> 35;
        };
    }

    // ---------------------------------------------------------------- goals

    private static final Map<String, Object[]> GOALS = new HashMap<>();
    private static final Object[][] POOL = {
            {"a comfy new armchair", 80}, {"a shiny new bike", 110}, {"a weekend away on Neon Heights", 70}, {"a record player", 100},
            {"a big oak bookshelf", 90}, {"a smart new coat", 75}, {"a painting for the living room", 85}, {"a brass telescope", 120},
            {"a new pair of boots", 60}, {"a fancy lamp for the flat", 65}
    };

    static {
        goal("mia", "a shiny stand mixer for the bakery", 120);
        goal("leo", "a proper chef's knife set", 90);
        goal("ava", "a brass telescope", 110);
        goal("omar", "a new striped awning for the market", 100);
        goal("rosa", "a pneumatic wrench", 130);
        goal("ben", "a new pair of steel-toe boots", 70);
        goal("kai", "a little fishing boat", 200);
        goal("nina", "a leather flight jacket", 90);
        goal("sam", "a new outboard motor", 180);
        goal("ivy", "a greenhouse kit", 150);
        goal("theo", "a vintage jukebox", 140);
        goal("zara", "an antique pocket watch", 110);
        goal("luna", "a noodle-pulling machine", 160);
        goal("rex", "a brand new arcade cabinet", 190);
        goal("nova", "a camera for tour photos", 120);
        goal("jet", "a pilot's leather jacket", 100);
        goal("nell", "a first edition of 'The Brass Clock'", 90);
        goal("hugo", "a gold fountain pen", 80);
    }

    private static void goal(String id, String what, int cost) {
        GOALS.put(id, new Object[]{what, cost});
    }

    public static void ensureGoal(CityData.Profile p) {
        if (!p.goal.isEmpty() && p.goalCost > 0) return;
        Object[] g = p.goalsDone == 0 ? GOALS.get(p.id) : null;
        if (g == null) g = POOL[Math.floorMod(p.id.hashCode() + p.goalsDone * 7, POOL.length)];
        p.goal = (String) g[0];
        p.goalCost = (Integer) g[1];
    }

    public static boolean impatient(CityData.Profile p) {
        return p.trait == Trait.ADVENTUROUS || p.trait == Trait.DREAMY || p.trait == Trait.TALKATIVE;
    }

    // ---------------------------------------------------------------- opening / daily upkeep

    public static void tick(ServerLevel sl, CityData d) {
        long now = sl.getGameTime();
        if (!d.bankOpened) init(sl, d);
        long day = Calendar.worldDay(sl);
        long rd = day(sl);
        int tod = tod(sl);
        for (CityData.Profile p : d.profiles.values()) ensureGoal(p);
        if (now % 1200 == 0) registerBoard(sl, d);
        if (Calendar.weekday(day) == 0 && tod >= 1000 && tod < 9000 && d.interestDay != day) payInterest(sl, d, day);
        if (tod >= 12400 && tod < 20000 && d.collectDay != rd) collect(sl, d, rd);
        if (tod >= 13000 && tod < 20000 && d.sweepDay != rd) sweep(sl, d, rd);
        callBanker(sl, d);
    }

    public static void onClick(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().isClientSide() || e.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND || !(e.getEntity() instanceof ServerPlayer pl)) return;
        try {
            BlockPos p = e.getPos();
            if (isAtmBlock(p)) {
                e.setCanceled(true);
                e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
                atmInteract(pl);
            } else if (isBell(p)) bell(pl);
        } catch (Throwable t) {
            FireheartCity.LOG.error("Bank click failed", t);
        }
    }

    static void init(ServerLevel sl, CityData d) {
        d.bankOpened = true;
        long day = day(sl);
        d.pay(CityData.CITY, RESERVE, 600, "Opening capital for the Solaris City Bank", day, tod(sl), false);
        for (CityData.Profile p : d.profiles.values()) ensureGoal(p);
        d.event(Calendar.worldDay(sl), "bank", "the Solaris City Bank opened its doors next to the plaza", WINDOW);
        d.setDirty();
    }

    static void registerBoard(ServerLevel sl, CityData d) {
        if (!sl.isLoaded(BOARD) || !(sl.getBlockState(BOARD).getBlock() instanceof LecternBlock)) return;
        if (d.boards.contains(BOARD)) return;
        d.boards.add(BOARD);
        Gazette.syncPlaces(d);
        Gazette.tick(sl, d, true);
        d.setDirty();
    }

    static void payInterest(ServerLevel sl, CityData d, long day) {
        d.interestDay = day;
        long rd = day(sl);
        int total = 0, savers = 0;
        for (Map.Entry<String, Integer> e : new ArrayList<>(d.savings.entrySet())) {
            String k = e.getKey();
            if (k.startsWith("biz:")) continue;
            int bal = e.getValue();
            if (bal < 10) continue;
            int amt = Math.min(60, Math.max(1, bal * rate(d) / 100));
            String from = d.balance(RESERVE) >= amt ? RESERVE : CityData.CITY;
            if (!d.pay(from, sav(k), amt, "Weekly interest (" + rate(d) + "%)", rd, tod(sl), false)) continue;
            total += amt;
            savers++;
            CityData.Profile p = d.profiles.get(k);
            if (p != null) {
                p.lastInterest = amt;
                p.interestDay = day;
                p.log(rd).note("The bank paid me " + amt + " coins interest");
            } else if (k.startsWith("player:")) {
                ServerPlayer pl = sl.getServer().getPlayerList().getPlayerByName(k.substring(7));
                if (pl != null) pl.sendSystemMessage(Component.literal("§6[Solaris City Bank] §eYour savings earned §f" + amt + " coins§e interest this week. Balance: §f" + savings(d, k) + " coins"));
                Loan pl2 = loan(d, k);
                Post.send(d, "bank", k, "Weekly statement\n\nAccount FH-" + String.format("%04d", d.holders.containsKey(k) ? d.holders.get(k).number : 0) + "\nInterest this week: +" + amt + " coins (" + rate(d) + "%)\nBalance: " + savings(d, k) + " coins" + (pl2 != null ? "\nLoan outstanding: " + pl2.owed + " coins" : "") + "\n\nThank you for banking with us!\n\n- Hugo, Solaris City Bank", "bank", day);
            }
        }
        d.interestTotal += total;
        if (savers > 0) d.event(day, "bank", "the Solaris City Bank paid " + total + " coins of interest to " + savers + (savers == 1 ? " saver" : " savers"), WINDOW);
        d.setDirty();
    }

    static void collect(ServerLevel sl, CityData d, long rd) {
        d.collectDay = rd;
        if (Calendar.weekend(rd)) return;
        long day = Calendar.worldDay(sl);
        for (Loan l : new ArrayList<>(d.loans.values())) {
            if (l.owed <= 0 || l.lastPaid >= rd) continue;
            int due = Math.min(l.owed, l.perDay);
            CityData.Profile p = d.profiles.get(l.who);
            boolean ok = false;
            if (p != null && d.balance(p.id) >= due) ok = d.pay(p.id, RESERVE, due, "Loan payment (auto)", rd, tod(sl), false);
            if (!ok && d.balance(sav(l.who)) >= due) ok = d.pay(sav(l.who), RESERVE, due, "Loan payment (auto)", rd, tod(sl), false);
            if (ok) {
                repaid(sl, d, l, due, rd, day);
                if (p != null && l.owed > 0) p.log(rd).note("The bank took my loan payment of " + due + " coins");
                continue;
            }
            l.missed++;
            l.lastPaid = rd;
            l.owed += 2;
            if (p != null) {
                p.missedTotal++;
                p.log(rd).note("I missed a loan payment at the bank");
                if (l.missed == 1) Post.send(d, "bank", p.id, "Dear " + p.name + ",\n\nToday's loan payment of " + due + " coins could not be collected, so a 2 coin late fee was added. You now owe " + l.owed + " coins.\n\nPlease pop in and see me.\n\n- Hugo", "bank", day);
                if (l.missed == 2) d.event(day, "bank", p.name + " has fallen behind on their bank loan", WINDOW);
            } else if (l.who.startsWith("player:")) {
                ServerPlayer pl = sl.getServer().getPlayerList().getPlayerByName(l.who.substring(7));
                if (pl != null) pl.sendSystemMessage(Component.literal("§6[Solaris City Bank] §cYour loan payment of " + due + " coins bounced - a 2 coin late fee was added. Owed: " + l.owed));
                Post.send(d, "bank", l.who, "Payment reminder\n\nYour loan payment of " + due + " coins could not be taken from your savings today. A 2 coin late fee was added.\n\nOutstanding: " + l.owed + " coins.\n\nPlease deposit some gold at the teller or cash machine.\n\n- Hugo", "bank", day);
            }
        }
        d.setDirty();
    }

    static void sweep(ServerLevel sl, CityData d, long rd) {
        d.sweepDay = rd;
        for (String k : new String[]{"biz:rent_city", "biz:rent_isle"}) {
            int b = d.balance(k);
            if (b > 0) d.pay(k, sav(k), b, "Rent banked", rd, tod(sl), false);
        }
        CityData.Profile hugo = banker(d);
        if (hugo != null) {
            int extra = hugo.coins - keep(hugo);
            if (extra > 5) d.pay(hugo.id, sav(hugo.id), extra, "Deposit", rd, tod(sl), false);
            Loan l = loan(d, hugo.id);
            if (l != null) l.lastPaid = rd;
        }
        for (Job j : Job.values()) {
            String biz = Economy.business(j);
            if (biz.equals(RESERVE)) continue;
            int till = d.balance(biz);
            if (till > 120) d.pay(biz, sav(biz), till - 40, "Night safe deposit", rd, tod(sl), false);
        }
        d.setDirty();
    }

    static void repaid(ServerLevel sl, CityData d, Loan l, int amt, long rd, long day) {
        l.owed -= amt;
        l.paid += amt;
        l.lastPaid = rd;
        if (l.owed > 0) return;
        d.loans.remove(l.who);
        CityData.Profile p = d.profiles.get(l.who);
        if (p != null) {
            p.loanPaidDay = day;
            p.rep += 3;
            p.log(rd).note("I paid off my bank loan");
            d.event(day, "bank", p.name + " paid off their bank loan", WINDOW, p.id);
        } else if (l.who.startsWith("player:")) {
            ServerPlayer pl = sl.getServer().getPlayerList().getPlayerByName(l.who.substring(7));
            if (pl != null) pl.sendSystemMessage(Component.literal("§6[Solaris City Bank] §aYour loan is fully paid off. Thank you!"));
        }
    }

    public static Loan grant(CityData d, String who, int amount, String purpose, long rd, int tod, String payTo) {
        log("loan granted to " + who + ": " + amount + " for " + purpose);
        Loan l = new Loan();
        l.who = who;
        l.principal = amount;
        l.owed = amount + (amount * LOAN_RATE + 99) / 100;
        l.perDay = Math.max(1, (l.owed + 4) / 5);
        l.taken = rd;
        l.lastPaid = rd;
        l.purpose = purpose;
        String from = d.balance(RESERVE) >= amount ? RESERVE : CityData.CITY;
        if (!d.pay(from, payTo, amount, "Loan - " + purpose, rd, tod, false)) return null;
        d.loans.put(who, l);
        d.setDirty();
        return l;
    }

    public static int maxLoan(CityData.Profile p) {
        return Math.min(150, Economy.wage(p) * 6);
    }

    public static String refuse(CityData d, CityData.Profile p) {
        if (loan(d, p.id) != null) return "you already have a loan with us. Let's get that one paid off first";
        if (p.missedTotal >= 4) return "you've missed too many payments lately. Come back when things are steadier";
        return null;
    }

    // ---------------------------------------------------------------- banker

    public static CityData.Profile banker(CityData d) {
        for (CityData.Profile p : d.profiles.values()) if (p.job == Job.BANKER) return p;
        return null;
    }

    public static Resident bankerEntity(ServerLevel sl, CityData d) {
        CityData.Profile p = banker(d);
        if (p == null || p.entity == null) return null;
        Entity e = sl.getEntity(p.entity);
        return e instanceof Resident r ? r : null;
    }

    public static boolean onDuty(Resident r) {
        CityData.Profile p = r.profile();
        return p != null && p.job == Job.BANKER && r.activityName().equals("work") && inBuilding(r.blockPosition());
    }

    public static boolean atCounter(Resident r) {
        return onDuty(r) && r.distanceToSqr(Vec3.atBottomCenterOf(TELLER)) < 1.2 * 1.2;
    }

    public static boolean forceOpen;

    public static boolean open(ServerLevel sl, CityData d) {
        if (forceOpen) return true;
        Resident b = bankerEntity(sl, d);
        return b != null && onDuty(b);
    }

    static void callBanker(ServerLevel sl, CityData d) {
        Resident b = bankerEntity(sl, d);
        if (b == null || !onDuty(b) || b.convo != null) return;
        if ("serve customers at the counter".equals(b.work.doing())) return;
        for (Resident r : sl.getEntitiesOfClass(Resident.class, new net.minecraft.world.phys.AABB(-44, 70, 15, -35, 76, 29), x -> x.waitingForTeller())) {
            b.work.reset();
            return;
        }
    }

    // ---------------------------------------------------------------- resident errands

    public static final class Need {
        public final String kind;
        public final int amount;

        Need(String kind, int amount) {
            this.kind = kind;
            this.amount = amount;
        }
    }

    public static String tillOf(CityData d, CityData.Profile p) {
        String biz = Economy.business(p.job);
        if (biz.equals(RESERVE) || !d.accounts.containsKey(biz)) return null;
        for (CityData.Profile q : d.profiles.values()) if (q.job == p.job) return q.id.equals(p.id) ? biz : null;
        return null;
    }

    public static List<Need> needs(CityData d, CityData.Profile p, long rd, boolean teller) {
        List<Need> out = new ArrayList<>();
        if (p.job == Job.BANKER) return out;
        ensureGoal(p);
        int sv = savings(d, p.id);
        int keep = keep(p);
        String till = tillOf(d, p);
        if (till != null && d.balance(till) > 45) out.add(new Need("till", d.balance(till) - 20));
        Loan l = loan(d, p.id);
        if (l != null && l.lastPaid < rd && !Calendar.weekend(rd) && p.coins + sv >= Math.min(l.owed, l.perDay)) out.add(new Need("repay", Math.min(l.owed, l.perDay)));
        int cash = p.coins - (out.stream().anyMatch(n -> n.kind.equals("repay")) ? Math.min(l.owed, l.perDay) : 0);
        if (l == null && sv + cash - keep >= p.goalCost && p.goalCost > 0) out.add(new Need("goal", p.goalCost));
        else if (teller && l == null && p.loanWish > 0 && refuse(d, p) == null) out.add(new Need("loan", Math.min(p.loanWish, maxLoan(p))));
        else if (teller && l == null && p.loanWish == 0 && impatient(p) && p.goalCost > 0 && sv + cash >= p.goalCost * 45 / 100 && refuse(d, p) == null && p.goalsDone < 3) {
            int want = p.goalCost - Math.max(0, sv + cash - keep);
            if (want > 0 && want <= maxLoan(p)) out.add(new Need("goalloan", want));
        }
        boolean spending = out.stream().anyMatch(n -> n.kind.equals("goal") || n.kind.equals("goalloan"));
        int tk = Lottery.wants(d, p, rd);
        if (tk > 0 && !spending) {
            out.add(new Need("ticket", tk));
            cash -= tk * Lottery.PRICE;
        }
        if (!spending && cash > keep + 10) out.add(new Need("deposit", cash - keep));
        else if (!spending && cash < 8 && sv >= 10) out.add(new Need("withdraw", Math.min(sv, keep - Math.max(0, cash))));
        return out;
    }

    public static String errand(Resident r, CityData d, CityData.Profile p) {
        if (p.job == Job.BANKER) return null;
        long rd = r.routineDay();
        if (p.bankDay == rd) return null;
        ServerLevel sl = (ServerLevel) r.level();
        boolean teller = open(sl, d);
        List<Need> n = needs(d, p, rd, teller);
        if (n.isEmpty()) return null;
        boolean island = p.livesOnIsland();
        if (island) {
            boolean urgent = n.stream().anyMatch(x -> x.kind.equals("goal") || x.kind.equals("loan") || x.kind.equals("goalloan") || x.kind.equals("withdraw"));
            if (!urgent && p.coins < 80 && !(r.getY() < 150)) return null;
        }
        boolean needsTeller = n.stream().allMatch(x -> x.kind.equals("loan") || x.kind.equals("goalloan"));
        if (needsTeller && !teller) return null;
        return teller ? KEY : ATM;
    }

    /** Emergency cash: out of coins but money in the bank. */
    public static boolean needsCash(CityData d, CityData.Profile p, long rd) {
        return p.coins < 3 && savings(d, p.id) >= 5 && p.cashDay != rd && p.job != Job.BANKER;
    }

    public static String atm(Resident r, CityData d, CityData.Profile p) {
        ServerLevel sl = (ServerLevel) r.level();
        long rd = r.routineDay();
        long day = r.day();
        int tod = tod(sl);
        List<String> did = new ArrayList<>();
        for (Need n : needs(d, p, rd, false)) {
            String s = perform(sl, d, p, n, rd, day, tod);
            if (s != null) did.add(s);
        }
        if (p.coins < 3 && savings(d, p.id) >= 5) {
            int amt = Math.min(savings(d, p.id), keep(p));
            if (d.pay(sav(p.id), p.id, amt, "Cash machine withdrawal", rd, tod, false)) did.add("took out " + amt);
        }
        p.bankDay = rd;
        p.cashDay = rd;
        d.setDirty();
        if (did.isEmpty()) return "*beep* Balance: " + savings(d, p.id) + " coins. Nothing to do today.";
        p.log(rd).note("I used the cash machine at the bank");
        return "*beep boop* " + Events.sentence(Economy.join(did)) + ". Savings: " + savings(d, p.id) + " coins.";
    }

    static String perform(ServerLevel sl, CityData d, CityData.Profile p, Need n, long rd, long day, int tod) {
        log(p.name + " " + n.kind + " " + n.amount + " (wallet " + p.coins + ", savings " + savings(d, p.id) + ")");
        switch (n.kind) {
            case "till": {
                String till = tillOf(d, p);
                if (till == null) return null;
                int amt = Math.min(n.amount, d.balance(till) - 20);
                if (amt <= 0 || !d.pay(till, sav(till), amt, "Takings banked by " + p.name, rd, tod, false)) return null;
                p.log(rd).note("I banked " + amt + " coins of takings from " + p.job.work().label);
                return "banked " + amt + " from the till";
            }
            case "repay": {
                Loan l = loan(d, p.id);
                if (l == null || l.lastPaid >= rd) return null;
                int amt = Math.min(l.owed, l.perDay);
                boolean ok = d.balance(p.id) >= amt ? d.pay(p.id, RESERVE, amt, "Loan payment", rd, tod, false) : d.pay(sav(p.id), RESERVE, amt, "Loan payment", rd, tod, false);
                if (!ok) return null;
                repaid(sl, d, l, amt, rd, day);
                return "paid " + amt + " off my loan";
            }
            case "deposit": {
                int amt = Math.min(n.amount, p.coins - keep(p));
                if (amt <= 0 || !d.pay(p.id, sav(p.id), amt, "Deposit", rd, tod, false)) return null;
                p.log(rd).note("I put " + amt + " coins in the bank");
                return "saved " + amt;
            }
            case "withdraw": {
                int amt = Math.min(n.amount, savings(d, p.id));
                if (amt <= 0 || !d.pay(sav(p.id), p.id, amt, "Withdrawal", rd, tod, false)) return null;
                p.cashDay = rd;
                p.log(rd).note("I took " + amt + " coins out of the bank");
                return "took out " + amt;
            }
            case "goal":
                return buyGoal(sl, d, p, rd, day, tod) ? "paid for " + p.lastGoal : null;
            case "ticket": {
                int cost = n.amount * Lottery.PRICE;
                String payer = p.coins >= cost ? p.id : savings(d, p.id) >= cost ? sav(p.id) : null;
                if (payer == null || !Lottery.buy(sl, d, p.id, payer, n.amount, rd, tod)) return null;
                return "bought " + (n.amount == 1 ? "a lottery ticket" : n.amount + " lottery tickets");
            }
            default:
                return null;
        }
    }

    static boolean buyGoal(ServerLevel sl, CityData d, CityData.Profile p, long rd, long day, int tod) {
        int cost = p.goalCost;
        if (cost <= 0) return false;
        int fromSav = Math.min(cost, savings(d, p.id));
        if (fromSav > 0) d.pay(sav(p.id), p.id, fromSav, "Withdrawal for " + p.goal, rd, tod, false);
        if (p.coins < cost) return false;
        String shop = p.goal.contains("Neon Heights") ? "biz:arcade" : "biz:supply";
        d.pay(p.id, shop, cost, p.goal, rd, tod, false);
        String what = p.goal;
        p.lastGoal = what;
        p.lastGoalDay = day;
        p.goalsDone++;
        p.fun = Math.min(100, p.fun + 25);
        p.rep += 2;
        p.loanWish = 0;
        p.log(rd).note("I finally bought " + what + " with my savings");
        d.event(day, "bank", p.name + " saved up and bought " + what, WINDOW, p.id);
        p.goal = "";
        p.goalCost = 0;
        ensureGoal(p);
        d.setDirty();
        return true;
    }

    // ---------------------------------------------------------------- teller conversation

    public static Dialogue.Script script(Resident a, Resident b, CityData d, long day) {
        Dialogue.Script s = new Dialogue.Script();
        s.kind = "bank";
        RandomSource r = a.getRandom();
        CityData.Profile pa = a.profile(), pb = b.profile();
        CityData.Rel ab = d.rel(pa.id, pb.id), ba = d.rel(pb.id, pa.id);
        ServerLevel sl = (ServerLevel) a.level();
        long rd = a.routineDay();
        int tod = tod(sl);
        boolean met = ab.met && ba.met;
        if (!met) {
            s.a("Hello! I'm " + pa.name + ", " + pa.job.title.toLowerCase() + " at " + lbl(pa.job.work().label) + ". First time in here!", Resident.G_WAVE);
            s.b("Welcome to the Solaris City Bank! I'm Hugo. What can I do for you?", Resident.G_WAVE);
        }
        String hi = met ? pick(r, "Hi " + pb.name + "! ", "Evening, " + pb.name + ". ", "Hello again! ", "") : "";
        List<Need> list = needs(d, pa, rd, true);
        int shown = 0;
        for (Need n : list) {
            if (shown >= 3) break;
            shown++;
            switch (n.kind) {
                case "till" -> {
                    String till = tillOf(d, pa);
                    String place = pa.job.work().label;
                    s.a(hi + "Here are today's takings from " + place + " - " + n.amount + " coins.", Resident.G_GIVE, () -> a.showItem("minecraft:gold_nugget", 40));
                    s.b("Counted and stamped - the " + (till == null ? "business" : d.accountName(till)) + " account holds " + (savings(d, till == null ? "" : till) + n.amount) + " coins now.", Resident.G_GIVE, () -> {
                        perform(sl, d, pa, n, rd, day, tod);
                        b.showItem("minecraft:writable_book", 40);
                        sound(sl, b, false);
                    });
                }
                case "repay" -> {
                    Loan l = loan(d, pa.id);
                    boolean last = l != null && l.owed <= n.amount;
                    s.a(hi + (shown > 1 ? "And here's" : "Here's") + " today's loan payment.", Resident.G_GIVE, () -> a.showItem("minecraft:gold_nugget", 40));
                    s.b(last ? "That's the final payment - your loan is paid off! Congratulations!" : "Thank you, " + n.amount + " coins received. " + (l == null ? 0 : l.owed - n.amount) + " to go.", last ? Resident.G_CHEER : Resident.G_GIVE, () -> {
                        perform(sl, d, pa, n, rd, day, tod);
                        sound(sl, b, last);
                    });
                    if (last) s.a(pick(r, "Debt-free! What a feeling.", "Finally! Thank you, " + pb.name + "."), Resident.G_CHEER);
                }
                case "deposit" -> {
                    int now = savings(d, pa.id) + n.amount;
                    s.a(hi + pick(r, "I'd like to put " + n.amount + " coins in my savings, please.", "Could you deposit " + n.amount + " coins for me?"), Resident.G_GIVE, () -> a.showItem("minecraft:gold_nugget", 40));
                    String goalBit = pa.goalCost > now ? " Only " + (pa.goalCost - now) + " more for " + pa.goal + "." : "";
                    s.b("Done! You've got " + now + " coins saved with us." + goalBit, Resident.G_GIVE, () -> {
                        perform(sl, d, pa, n, rd, day, tod);
                        sound(sl, b, false);
                    });
                    if (!goalBit.isEmpty() && r.nextFloat() < 0.5f) s.a(pick(r, "I'm getting there!", "Every coin counts.", "Can't wait!"), Resident.G_CHEER);
                }
                case "withdraw" -> {
                    s.a(hi + pick(r, "I'm running a bit low - could I take out " + n.amount + " coins?", "I need to withdraw " + n.amount + " coins, please."), Resident.G_THINK);
                    s.b("Of course. Here you are - " + Math.max(0, savings(d, pa.id) - n.amount) + " left in your savings.", Resident.G_GIVE, () -> {
                        perform(sl, d, pa, n, rd, day, tod);
                        b.showItem("minecraft:gold_nugget", 40);
                        sound(sl, b, false);
                    });
                }
                case "goal" -> {
                    String g = pa.goal;
                    s.a(hi + "I've finally saved enough for " + g + "! " + n.amount + " coins, please.", Resident.G_CHEER);
                    s.b("Congratulations! That's what saving is for. Enjoy " + g + "!", Resident.G_GIVE, () -> {
                        if (buyGoal(sl, d, pa, rd, day, tod)) {
                            a.particles(ParticleTypes.HAPPY_VILLAGER, 10);
                            a.gesture(Resident.G_CHEER, 50);
                        }
                        sound(sl, b, true);
                    });
                }
                case "ticket" -> {
                    int pot = Lottery.pot(d) + n.amount * Lottery.PRICE;
                    s.a(hi + (shown > 1 ? "Oh, and " : "") + (n.amount == 1 ? "one lottery ticket" : n.amount + " lottery tickets") + ", please!", Resident.G_GIVE, () -> a.showItem("minecraft:gold_nugget", 30));
                    s.b(pick(r, "Here you go! The jackpot's " + pot + " coins this week. Good luck on Sunday!", "Feeling lucky? Draw's Sunday evening at the plaza.", "One ticket to riches! Well... maybe."), Resident.G_GIVE, () -> {
                        perform(sl, d, pa, n, rd, day, tod);
                        a.showItem("minecraft:paper", 50);
                        sound(sl, b, false);
                    });
                }
                case "loan", "goalloan" -> {
                    boolean forGoal = n.kind.equals("goalloan");
                    String why = forGoal ? "so I can get " + pa.goal + " now" : pa.loanWhy.equals("rent") ? "to cover my rent" : "to tide me over";
                    int amt = n.amount;
                    int owed = amt + (amt * LOAN_RATE + 99) / 100;
                    int per = Math.max(1, (owed + 4) / 5);
                    String refusal = refuse(d, pa);
                    s.a(hi + "Um... I was hoping to borrow " + amt + " coins, " + why + ".", Resident.G_THINK);
                    if (refusal != null) {
                        s.b("I'm sorry, " + pa.name + ", " + refusal + ".", Resident.G_THINK);
                        s.a("I understand...", Resident.G_THINK);
                    } else {
                        s.b("Let me look at your account... " + pa.name + ", " + pa.jobTitle() + ", steady wages...", Resident.G_THINK, () -> b.showItem("minecraft:writable_book", 60));
                        s.b("Approved! " + amt + " coins at " + LOAN_RATE + "% - you'll pay back " + owed + ", " + per + " coins each workday.", Resident.G_GIVE, () -> {
                            Loan l = grant(d, pa.id, amt, forGoal ? pa.goal : (pa.loanWhy.isEmpty() ? "a personal loan" : pa.loanWhy), rd, tod, pa.id);
                            if (l != null) {
                                pa.loanWish = 0;
                                pa.log(rd).note("I took out a loan of " + amt + " coins at the bank");
                                d.event(day, "bank", pa.name + " took out a " + amt + " coin loan at the bank" + (forGoal ? " for " + pa.goal : ""), WINDOW, pa.id);
                                if (forGoal) buyGoal(sl, d, pa, rd, day, tod);
                            }
                            a.showItem("minecraft:gold_nugget", 50);
                            sound(sl, b, true);
                        });
                        s.a(pick(r, "Thank you so much, " + pb.name + "! I'll pay it back, promise.", "You're a lifesaver!"), Resident.G_CHEER);
                    }
                }
                default -> {}
            }
            hi = "";
        }
        if (shown == 0) {
            int sv = savings(d, pa.id);
            s.a("Just checking my balance, actually.", Resident.G_THINK);
            s.b("You have " + sv + " coins in savings" + (pa.goalCost > 0 ? " - saving for " + pa.goal + ", right?" : "."), Resident.G_THINK);
        }
        if (r.nextFloat() < 0.25f && pa.goalCost > 0 && shown > 0) s.b(pick(r, "Remember, savings earn " + rate(d) + "% every Monday.", "Interest gets paid every Monday morning, by the way."));
        else if (r.nextFloat() < 0.5f) s.b(pick(r, "Have a lovely evening!", "Thank you for banking with us!", "See you next time, " + pa.name + "."), Resident.G_WAVE);
        s.onDone = () -> {
            pa.bankDay = rd;
            for (CityData.Rel x : new CityData.Rel[]{ab, ba}) { x.met = true; x.knowsJob = true; x.fam = Math.min(100, x.fam + 4); x.chats++; x.lastChatDay = day; }
            pb.log(b.routineDay()).served++;
            if (!met) d.news(day, pa.name + " met " + pb.name + " at the bank.");
            d.setDirty();
        };
        s.after = a::doneErrand;
        return s;
    }

    static void sound(ServerLevel sl, Resident at, boolean big) {
        sl.playSound(null, at.blockPosition(), big ? SoundEvents.PLAYER_LEVELUP : SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, big ? 0.3f : 0.4f, 1.4f);
    }

    static String pick(RandomSource r, String... o) {
        return Lines.pick(r, o);
    }

    // ---------------------------------------------------------------- small talk about money

    public static boolean talk(Dialogue.Script s, Resident a, Resident b, CityData.Profile pa, CityData.Profile pb, CityData d, long day, RandomSource r, List<Runnable> effects) {
        int sa = savings(d, pa.id), sb = savings(d, pb.id);
        Loan la = loan(d, pa.id), lb = loan(d, pb.id);
        List<Runnable> opts = new ArrayList<>();
        if (day - pa.loanPaidDay <= 2) opts.add(() -> {
            s.a("Guess what? I paid off my bank loan! Debt-free!", Resident.G_CHEER);
            s.b(pick(r, "Congratulations! Hugo must be pleased.", "Nice! That calls for a celebration."), Resident.G_CHEER);
        });
        if (day - pa.lastGoalDay <= 3 && !pa.lastGoal.isEmpty()) opts.add(() -> {
            s.a("Did I tell you? I bought " + pa.lastGoal + " with my savings!", Resident.G_CHEER);
            s.b(pick(r, "No way! You have to show me.", "You actually did it! All that saving paid off.", "Lucky! I'm still saving for " + pb.goal + "."), Resident.G_CHEER);
            effects.add(() -> { CityData.Rel ba = d.rel(pb.id, pa.id); ba.facts.put("bought", pa.lastGoal); });
        });
        if (la != null) opts.add(() -> {
            if (la.missed > 0) {
                s.a("I missed a payment on my bank loan... Hugo gave me a look.", Resident.G_THINK);
                s.b(pick(r, "Uh oh. Better catch up before the fees pile up.", "Hugo's fair. Just pay a bit extra next time."));
            } else {
                s.a("Still paying off my loan - " + la.owed + " coins to go.", Resident.G_THINK);
                s.b(lb != null ? "Same here. Mine's got " + lb.owed + " left." : pick(r, "You'll get there, bit by bit.", "Hang in there!", "At least it's only " + la.perDay + " a day."));
            }
        });
        if (Calendar.weekday(day) <= 1 && day - pa.interestDay <= 1 && pa.lastInterest > 0) opts.add(() -> {
            s.a("The bank paid me " + pa.lastInterest + " coins interest this week. Free money!", Resident.G_CHEER);
            s.b(sb >= 10 ? "I got " + (pb.lastInterest > 0 ? pb.lastInterest : 1) + " myself. Love Mondays." : "Wait, really? I should start saving.", sb >= 10 ? Resident.G_CHEER : Resident.G_THINK);
        });
        if (pa.goalCost > 0 && sa > 0) opts.add(() -> {
            int left = Math.max(0, pa.goalCost - sa);
            s.a("I've got " + sa + " coins saved at the bank now. Saving up for " + pa.goal + (left > 0 ? " - " + left + " to go." : "!"));
            if (pb.goalCost > 0 && sb > 0) s.b("Nice! I'm saving for " + pb.goal + " - " + sb + " coins so far.");
            else s.b(pick(r, "That's so sensible. I spend everything the second I get paid.", "Ooh, " + pa.goal + "! Good luck.", "Maybe I should open a savings account too."));
            effects.add(() -> d.rel(pb.id, pa.id).facts.put("saving", pa.goal));
        });
        if (pb.coins > 60 && sb < 20 && pb.job != Job.BANKER) opts.add(() -> {
            s.a("You're carrying a lot of coins around, " + pb.name + ". The bank pays " + rate(d) + "% a week, you know.", Resident.G_THINK);
            s.b(pick(r, "Hmm, maybe I should put some in savings.", "I like having cash on me!", "Good point. I'll stop by Hugo's after work."));
            effects.add(() -> { if (r.nextFloat() < 0.6f) pb.bankDay = -1; });
        });
        if (pb.job == Job.BANKER) opts.add(() -> {
            s.a(pick(r, "Is my money really safe in that vault, " + pb.name + "?", "How's business at the bank, " + pb.name + "?"), Resident.G_THINK);
            s.b(pick(r, "Behind a wall of solid iron and a very heavy door. Safe as houses.", "Busy! Half the town dropped by after work today.", "Deposits are up, loans are paid on time... mostly."));
        });
        if (!d.holders.isEmpty()) opts.add(() -> {
            Holder h = d.holders.values().iterator().next();
            s.a("Did you know " + h.name + " has an account at the bank too?");
            s.b(pick(r, "Makes sense - they built half the city!", "I saw them at the cash machine the other day."));
        });
        CityData.Rel ab = d.rel(pa.id, pb.id);
        String saving = ab.facts.get("saving");
        if (saving != null && pb.goal.equals(saving) && sb > 0) opts.add(() -> {
            s.a("How's the saving going for " + saving + "?");
            s.b(sb >= pb.goalCost ? "I've got enough now! Going to the bank to get it soon." : "Slowly! " + sb + " out of " + pb.goalCost + " coins.", Resident.G_THINK);
        });
        if (opts.isEmpty()) return false;
        opts.get(r.nextInt(opts.size())).run();
        return true;
    }

    // ---------------------------------------------------------------- player accounts

    private static final Map<UUID, Long> SESSIONS = new HashMap<>();
    private static final Map<UUID, Boolean> AT_TELLER = new HashMap<>();

    public static void startSession(ServerPlayer pl, boolean teller) {
        SESSIONS.put(pl.getUUID(), pl.level().getGameTime());
        AT_TELLER.put(pl.getUUID(), teller);
    }

    static boolean session(ServerPlayer pl) {
        Long t = SESSIONS.get(pl.getUUID());
        if (t == null || pl.level().getGameTime() - t > SESSION) return false;
        return pl.distanceToSqr(-38.0, 71.0, 22.0) < 16 * 16;
    }

    static boolean teller(ServerPlayer pl) {
        return session(pl) && Boolean.TRUE.equals(AT_TELLER.get(pl.getUUID())) && open(pl.serverLevel(), CityData.get(pl.serverLevel()));
    }

    public static Holder holder(CityData d, String name) {
        return d.holders.get(playerKey(name));
    }

    public static int cash(ServerPlayer pl) {
        int n = 0;
        Inventory inv = pl.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) n += value(inv.getItem(i));
        return n;
    }

    static int value(ItemStack st) {
        if (st.is(Items.GOLD_NUGGET)) return st.getCount();
        if (st.is(Items.GOLD_INGOT)) return st.getCount() * 9;
        if (st.is(Items.GOLD_BLOCK)) return st.getCount() * 81;
        return 0;
    }

    static boolean takeCash(ServerPlayer pl, int amount) {
        if (cash(pl) < amount) return false;
        Inventory inv = pl.getInventory();
        int taken = 0;
        for (Item it : new Item[]{Items.GOLD_NUGGET, Items.GOLD_INGOT, Items.GOLD_BLOCK}) {
            for (int i = 0; i < inv.getContainerSize() && taken < amount; i++) {
                ItemStack st = inv.getItem(i);
                if (!st.is(it)) continue;
                int each = value(new ItemStack(it));
                while (!st.isEmpty() && taken < amount) {
                    st.shrink(1);
                    taken += each;
                }
            }
        }
        if (taken > amount) giveCash(pl, taken - amount);
        inv.setChanged();
        return true;
    }

    static void giveCash(ServerPlayer pl, int amount) {
        int ingots = amount / 9, nuggets = amount % 9;
        while (ingots > 0) {
            int n = Math.min(64, ingots);
            give(pl, new ItemStack(Items.GOLD_INGOT, n));
            ingots -= n;
        }
        if (nuggets > 0) give(pl, new ItemStack(Items.GOLD_NUGGET, nuggets));
    }

    static void give(ServerPlayer pl, ItemStack st) {
        if (!pl.getInventory().add(st) && !st.isEmpty()) pl.drop(st, false);
    }

    static MutableComponent button(String label, String cmd, String hover, ChatFormatting color) {
        return Component.literal("[" + label + "]").withStyle(Style.EMPTY.withColor(color).withBold(false)
                .withClickEvent(new ClickEvent(cmd.endsWith(" ") ? ClickEvent.Action.SUGGEST_COMMAND : ClickEvent.Action.RUN_COMMAND, cmd))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover))));
    }

    static void line(ServerPlayer pl, Component... parts) {
        if (BankNet.QUIET.contains(pl.getUUID())) return;
        MutableComponent m = Component.literal("");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) m.append(Component.literal(" "));
            m.append(parts[i]);
        }
        pl.sendSystemMessage(m);
    }

    static void msg(ServerPlayer pl, String s) {
        if (BankNet.QUIET.contains(pl.getUUID())) {
            BankNet.NOTICE.put(pl.getUUID(), s);
            return;
        }
        pl.sendSystemMessage(Component.literal(s));
    }

    static Boolean atTeller(ServerPlayer pl) {
        return AT_TELLER.get(pl.getUUID());
    }

    public static void menu(ServerPlayer pl) {
        if (BankNet.QUIET.contains(pl.getUUID())) return;
        CityData d = CityData.get(pl.serverLevel());
        String name = pl.getName().getString();
        Holder h = holder(d, name);
        boolean teller = teller(pl);
        msg(pl, "§6§l=== Solaris City Bank ===§r §7" + (teller ? "Teller window" : "Cash machine"));
        if (h == null) {
            if (teller) {
                msg(pl, "§eYou don't have an account yet. Opening one is free and comes with a bank card.");
                line(pl, button("Open an account", "/bank open", "Open a savings account with Hugo", ChatFormatting.GREEN));
            } else msg(pl, "§eNo account found. Please see the teller inside to open one (weekdays during work hours).");
            return;
        }
        int bal = savings(d, playerKey(name));
        Loan l = loan(d, playerKey(name));
        msg(pl, "§7Account §fFH-" + String.format("%04d", h.number) + "§7 · §e" + name + "§7 · Balance §a" + bal + " coins§7 · Cash on you §f" + cash(pl) + " §7(gold)");
        if (l != null) msg(pl, "§7Loan: §c" + l.owed + " coins owed§7 · " + l.perDay + " per workday (auto-paid from savings at closing time)");
        line(pl, Component.literal("§7Deposit:"), button("10", "/bank deposit 10", "Deposit 10 coins (gold nuggets/ingots/blocks)", ChatFormatting.GREEN),
                button("50", "/bank deposit 50", "Deposit 50 coins", ChatFormatting.GREEN), button("All cash", "/bank deposit all", "Deposit all the gold you carry", ChatFormatting.GREEN),
                button("Other...", "/bank deposit ", "Type an amount", ChatFormatting.DARK_GREEN));
        line(pl, Component.literal("§7Withdraw:"), button("10", "/bank withdraw 10", "Withdraw 10 coins as gold", ChatFormatting.AQUA),
                button("50", "/bank withdraw 50", "Withdraw 50 coins", ChatFormatting.AQUA), button("Other...", "/bank withdraw ", "Type an amount", ChatFormatting.DARK_AQUA));
        line(pl, button("Pay a resident", "/bank payees", "Send coins from your account to a resident", ChatFormatting.GOLD),
                button("Statement", "/bank statement", "Your last transactions", ChatFormatting.YELLOW),
                l != null ? button("Repay loan", "/bank repay all", "Pay off the loan from your savings", ChatFormatting.RED) : Component.literal(""));
        if (teller && l == null) line(pl, Component.literal("§7Loans (" + LOAN_RATE + "%, 5 workdays):"), button("50", "/bank loan 50", "Borrow 50, repay " + (50 + 5), ChatFormatting.LIGHT_PURPLE),
                button("100", "/bank loan 100", "Borrow 100, repay 110", ChatFormatting.LIGHT_PURPLE), button("250", "/bank loan 250", "Borrow 250, repay 275", ChatFormatting.LIGHT_PURPLE));
        int mine = Lottery.tickets(d, playerKey(name));
        line(pl, Component.literal("§7Lottery §e(jackpot " + Lottery.pot(d) + ", draw Sunday evening at the plaza)§7 · your tickets: §f" + mine), button("Buy 1", "/bank ticket 1", "1 ticket for " + Lottery.PRICE + " coins from your savings", ChatFormatting.YELLOW), button("Buy 5", "/bank ticket 5", "5 tickets for " + (5 * Lottery.PRICE) + " coins", ChatFormatting.YELLOW));
        msg(pl, "§8Savings earn " + rate(d) + "% interest every Monday. 1 gold nugget = 1 coin, ingot = 9, block = 81.");
    }

    public static void tellerInteract(ServerPlayer pl, Resident hugo) {
        CityData d = CityData.get(pl.serverLevel());
        String name = pl.getName().getString();
        startSession(pl, true);
        Holder h = holder(d, name);
        hugo.getLookControl().setLookAt(pl, 30, 30);
        hugo.gesture(Resident.G_WAVE, 30);
        if (h == null) hugo.sayTo("Welcome to the Solaris City Bank, " + name + "! Would you like to open an account?", 100);
        else hugo.sayTo(pick(hugo.getRandom(), "Good to see you, " + name + ". What can I do for you?", "Ah, " + name + "! Your balance is " + savings(d, playerKey(name)) + " coins.", "Welcome back! Deposits, withdrawals, loans?"), 90);
        BankNet.send(pl);
    }

    public static void atmInteract(ServerPlayer pl) {
        startSession(pl, false);
        ServerLevel sl = pl.serverLevel();
        sl.playSound(null, ATM_SCREEN, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.6f, 1.6f);
        BankNet.send(pl);
    }

    static boolean check(ServerPlayer pl) {
        if (session(pl)) return true;
        msg(pl, "§cUse the teller or the cash machine at the Solaris City Bank first (right-click Hugo or the machine's screen).");
        return false;
    }

    static Holder need(ServerPlayer pl, CityData d) {
        Holder h = holder(d, pl.getName().getString());
        if (h == null) msg(pl, "§cYou don't have an account yet - ask the teller to open one.");
        return h;
    }

    public static int cmdOpen(ServerPlayer pl) {
        if (!check(pl)) return 0;
        if (!teller(pl)) { msg(pl, "§cAccounts are opened at the teller window inside, while Hugo is working."); return 0; }
        CityData d = CityData.get(pl.serverLevel());
        String name = pl.getName().getString();
        if (holder(d, name) != null) { menu(pl); return 1; }
        Holder h = new Holder();
        h.name = name;
        h.number = d.nextAccount++;
        h.opened = Calendar.worldDay(pl.serverLevel());
        d.holders.put(playerKey(name), h);
        d.savings.put(playerKey(name), 0);
        ItemStack card = new ItemStack(Items.PAPER);
        card.setHoverName(Component.literal("§6Solaris City Bank Card"));
        ListTag lore = new ListTag();
        lore.add(StringTag.valueOf("{\"text\":\"Account FH-" + String.format("%04d", h.number) + "\",\"color\":\"gray\",\"italic\":false}"));
        lore.add(StringTag.valueOf("{\"text\":\"Holder: " + name + "\",\"color\":\"white\",\"italic\":false}"));
        lore.add(StringTag.valueOf("{\"text\":\"Savings earn interest every Monday\",\"color\":\"dark_gray\",\"italic\":false}"));
        card.getOrCreateTagElement("display").put("Lore", lore);
        give(pl, card);
        d.event(Calendar.worldDay(pl.serverLevel()), "bank", name + " opened an account at the Solaris City Bank", WINDOW);
        d.setDirty();
        Resident hugo = bankerEntity(pl.serverLevel(), d);
        if (hugo != null) {
            hugo.sayTo("All done! Account FH-" + String.format("%04d", h.number) + " is yours. Here's your bank card.", 100);
            hugo.gesture(Resident.G_GIVE, 40);
            hugo.showItem("minecraft:paper", 40);
        }
        pl.serverLevel().playSound(null, pl.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.3f);
        msg(pl, "§aAccount FH-" + String.format("%04d", h.number) + " opened! You received your bank card.");
        menu(pl);
        return 1;
    }

    static int amount(ServerPlayer pl, String arg, int all) {
        if (arg.equalsIgnoreCase("all")) return all;
        try {
            return Integer.parseInt(arg.trim());
        } catch (NumberFormatException e) {
            msg(pl, "§cThat's not an amount: " + arg);
            return -1;
        }
    }

    public static int cmdDeposit(ServerPlayer pl, String arg) {
        if (!check(pl)) return 0;
        CityData d = CityData.get(pl.serverLevel());
        if (need(pl, d) == null) return 0;
        int amt = amount(pl, arg, cash(pl));
        if (amt <= 0) { if (amt == 0) msg(pl, "§cYou have no gold on you to deposit."); return 0; }
        if (!takeCash(pl, amt)) { msg(pl, "§cYou only have " + cash(pl) + " coins of gold on you."); return 0; }
        String key = playerKey(pl.getName().getString());
        ServerLevel sl = pl.serverLevel();
        d.pay(CityData.CITY, sav(key), amt, "Cash deposit", day(sl), tod(sl), false);
        sl.playSound(null, pl.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.2f);
        msg(pl, "§aDeposited " + amt + " coins. New balance: " + savings(d, key) + " coins.");
        teller(pl, "Deposited " + amt + " coins - thank you!");
        return 1;
    }

    public static int cmdWithdraw(ServerPlayer pl, String arg) {
        if (!check(pl)) return 0;
        CityData d = CityData.get(pl.serverLevel());
        if (need(pl, d) == null) return 0;
        String key = playerKey(pl.getName().getString());
        int amt = amount(pl, arg, savings(d, key));
        if (amt <= 0) return 0;
        ServerLevel sl = pl.serverLevel();
        if (!d.pay(sav(key), CityData.CITY, amt, "Cash withdrawal", day(sl), tod(sl), false)) { msg(pl, "§cInsufficient funds - your balance is " + savings(d, key) + " coins."); return 0; }
        giveCash(pl, amt);
        sl.playSound(null, pl.blockPosition(), SoundEvents.ARMOR_EQUIP_GOLD, SoundSource.PLAYERS, 0.6f, 1.2f);
        msg(pl, "§aWithdrew " + amt + " coins as gold. Balance: " + savings(d, key) + " coins.");
        teller(pl, "Here are your " + amt + " coins.");
        return 1;
    }

    static void teller(ServerPlayer pl, String line) {
        if (!Boolean.TRUE.equals(AT_TELLER.get(pl.getUUID()))) return;
        Resident hugo = bankerEntity(pl.serverLevel(), CityData.get(pl.serverLevel()));
        if (hugo != null && hugo.distanceToSqr(pl) < 12 * 12) {
            hugo.sayTo(line, 70);
            hugo.gesture(Resident.G_GIVE, 30);
        }
    }

    public static int cmdPayees(ServerPlayer pl) {
        if (!check(pl)) return 0;
        CityData d = CityData.get(pl.serverLevel());
        if (need(pl, d) == null) return 0;
        msg(pl, "§6Pay a resident from your account §7(balance " + savings(d, playerKey(pl.getName().getString())) + "):");
        for (CityData.Profile p : d.profiles.values()) {
            line(pl, Component.literal("§e" + p.name + " §8(" + p.job.title + ")"),
                    button("5", "/bank pay " + p.id + " 5", "Send 5 coins to " + p.name, ChatFormatting.GREEN),
                    button("10", "/bank pay " + p.id + " 10", "Send 10 coins to " + p.name, ChatFormatting.GREEN),
                    button("25", "/bank pay " + p.id + " 25", "Send 25 coins to " + p.name, ChatFormatting.GREEN),
                    button("...", "/bank pay " + p.id + " ", "Type an amount", ChatFormatting.DARK_GREEN));
        }
        return 1;
    }

    public static int cmdPay(ServerPlayer pl, String who, String arg) {
        if (!check(pl)) return 0;
        return appPay(pl, who, arg);
    }

    public static int appPay(ServerPlayer pl, String who, String arg) {
        CityData d = CityData.get(pl.serverLevel());
        if (need(pl, d) == null) return 0;
        CityData.Profile p = d.byName(who);
        if (p == null) { msg(pl, "§cNo resident called " + who + "."); return 0; }
        String key = playerKey(pl.getName().getString());
        int amt = amount(pl, arg, savings(d, key));
        if (amt <= 0) return 0;
        ServerLevel sl = pl.serverLevel();
        String name = pl.getName().getString();
        if (!d.pay(sav(key), p.id, amt, "Payment from " + name, day(sl), tod(sl), false)) { msg(pl, "§cInsufficient funds - your balance is " + savings(d, key) + " coins."); return 0; }
        Holder h = holder(d, name);
        h.paidOut += amt;
        CityData.Rel pr = d.playerRel(p.id, name);
        pr.facts.put("paid", String.valueOf(amt + (pr.facts.containsKey("paid") ? Integer.parseInt(pr.facts.get("paid")) : 0)));
        pr.aff = Math.min(100, pr.aff + Math.min(15, 2 + amt / 5));
        pr.fam = Math.min(100, pr.fam + 3);
        p.log(day(sl)).note(name + " sent me " + amt + " coins through the bank");
        Mind.playerEvent(d, p, name, day(sl), "{P} sent me " + amt + " coins through the bank", amt >= 20 ? 3 : 2, amt >= 20 ? 6 : 4);
        if (amt >= 50) d.event(Calendar.worldDay(sl), "bank", name + " sent " + p.name + " " + amt + " coins", WINDOW, p.id);
        d.setDirty();
        msg(pl, "§aSent " + amt + " coins to " + p.name + ". Balance: " + savings(d, key) + " coins.");
        Entity e = p.entity == null ? null : sl.getEntity(p.entity);
        if (e instanceof Resident r && r.distanceToSqr(pl) < 16 * 16) {
            pr.facts.remove("paid");
            r.getLookControl().setLookAt(pl, 30, 30);
            r.gesture(Resident.G_CHEER, 40);
            r.sayTo(amt >= 25 ? "Whoa, " + amt + " coins?! Thank you so much, " + name + "!" : "Thanks for the " + amt + " coins, " + name + "!", 80);
            r.particles(ParticleTypes.HAPPY_VILLAGER, 8);
        } else teller(pl, "Transfer to " + p.name + " sent.");
        return 1;
    }

    public static int cmdStatement(ServerPlayer pl) {
        if (!check(pl)) return 0;
        CityData d = CityData.get(pl.serverLevel());
        if (need(pl, d) == null) return 0;
        String key = sav(playerKey(pl.getName().getString()));
        msg(pl, "§6=== Statement §7(balance " + d.balance(key) + ")§6 ===");
        int n = 0;
        for (int i = d.ledger.size() - 1; i >= 0 && n < 10; i--) {
            CityData.Tx t = d.ledger.get(i);
            if (!t.from.equals(key) && !t.to.equals(key)) continue;
            boolean in = t.to.equals(key);
            msg(pl, "§8" + Calendar.name(t.day).substring(0, 3) + " " + Calendar.clock(t.time) + " " + (in ? "§a+" : "§c-") + t.amount + " §7" + t.memo);
            n++;
        }
        if (n == 0) msg(pl, "§7No transactions yet.");
        return 1;
    }

    public static int cmdLoan(ServerPlayer pl, String arg) {
        if (!check(pl)) return 0;
        if (!teller(pl)) { msg(pl, "§cLoans are arranged at the teller window with Hugo."); return 0; }
        CityData d = CityData.get(pl.serverLevel());
        if (need(pl, d) == null) return 0;
        String key = playerKey(pl.getName().getString());
        if (loan(d, key) != null) { msg(pl, "§cYou already have a loan. Pay it off first."); teller(pl, "Let's settle the current loan first, shall we?"); return 0; }
        int amt = amount(pl, arg, 0);
        if (amt <= 0 || amt > 500) { msg(pl, "§cLoans are between 1 and 500 coins."); return 0; }
        ServerLevel sl = pl.serverLevel();
        Loan l = grant(d, key, amt, "a personal loan", day(sl), tod(sl), sav(key));
        if (l == null) { msg(pl, "§cThe bank can't lend that much right now."); return 0; }
        msg(pl, "§dLoan approved: " + amt + " coins paid into your account. You owe " + l.owed + " (" + l.perDay + " per workday, taken from your savings at closing time).");
        teller(pl, "Approved! " + amt + " coins are in your account. " + l.perDay + " a day, and we're square in a week.");
        d.event(Calendar.worldDay(sl), "bank", pl.getName().getString() + " took out a loan at the bank", WINDOW);
        menu(pl);
        return 1;
    }

    public static int cmdTicket(ServerPlayer pl, String arg) {
        if (!check(pl)) return 0;
        CityData d = CityData.get(pl.serverLevel());
        if (need(pl, d) == null) return 0;
        String key = playerKey(pl.getName().getString());
        int n = amount(pl, arg, 1);
        int have = Lottery.tickets(d, key);
        if (n <= 0) return 0;
        if (have + n > Lottery.PLAYER_MAX) { msg(pl, "§cYou can hold at most " + Lottery.PLAYER_MAX + " tickets per week (you have " + have + ")."); return 0; }
        ServerLevel sl = pl.serverLevel();
        if (!Lottery.buy(sl, d, key, sav(key), n, day(sl), tod(sl))) { msg(pl, "§cNot enough in your account - tickets cost " + Lottery.PRICE + " coins each."); return 0; }
        msg(pl, "§eBought " + n + " lottery ticket" + (n == 1 ? "" : "s") + ". You hold " + Lottery.tickets(d, key) + " · jackpot now " + Lottery.pot(d) + " coins · draw Sunday evening at Solaris Plaza.");
        teller(pl, "Good luck on Sunday, " + pl.getName().getString() + "!");
        return 1;
    }

    public static int cmdRepay(ServerPlayer pl, String arg) {
        if (!check(pl)) return 0;
        CityData d = CityData.get(pl.serverLevel());
        if (need(pl, d) == null) return 0;
        String key = playerKey(pl.getName().getString());
        Loan l = loan(d, key);
        if (l == null) { msg(pl, "§aYou don't owe the bank anything."); return 0; }
        int amt = Math.min(l.owed, amount(pl, arg, l.owed));
        if (amt <= 0) return 0;
        ServerLevel sl = pl.serverLevel();
        if (!d.pay(sav(key), RESERVE, amt, "Loan repayment", day(sl), tod(sl), false)) { msg(pl, "§cNot enough in your account (" + savings(d, key) + " coins). Deposit some gold first."); return 0; }
        repaid(sl, d, l, amt, day(sl), Calendar.worldDay(sl));
        if (d.loans.containsKey(key)) msg(pl, "§aPaid " + amt + " coins. Still owed: " + l.owed + ".");
        teller(pl, d.loans.containsKey(key) ? "Payment received, thank you." : "And that's your loan paid off. Splendid!");
        return 1;
    }

    // ---------------------------------------------------------------- admin

    public static List<String> report(CityData d) {
        List<String> out = new ArrayList<>();
        int wallets = 0, saved = 0;
        for (CityData.Profile p : d.profiles.values()) {
            int sv = savings(d, p.id);
            wallets += p.coins;
            saved += sv;
            Loan l = loan(d, p.id);
            out.add("§e" + p.name + "§7: wallet §f" + p.coins + "§7 · savings §a" + sv + "§7" + (l != null ? " · loan §c" + l.owed + "§7 (" + l.perDay + "/day, missed " + l.missed + ")" : "") + " · goal " + p.goal + " (" + p.goalCost + ")");
        }
        out.add("§7Wallets " + wallets + " · resident savings " + saved + " · bank reserve " + d.balance(RESERVE) + " · interest paid so far " + d.interestTotal);
        for (Map.Entry<String, Integer> e : d.savings.entrySet()) if (!d.profiles.containsKey(e.getKey())) out.add("§b" + d.accountName(e.getKey()) + "§7: " + e.getValue());
        for (Map.Entry<String, Integer> e : d.accounts.entrySet()) if (e.getKey().startsWith("biz:")) out.add("§3till " + d.accountName(e.getKey()) + "§7: " + e.getValue());
        return out;
    }

    public static boolean isBell(BlockPos p) {
        return p.equals(BELL);
    }

    public static void bell(ServerPlayer pl) {
        CityData d = CityData.get(pl.serverLevel());
        Resident hugo = bankerEntity(pl.serverLevel(), d);
        if (hugo != null && onDuty(hugo)) {
            hugo.sayTo(pick(hugo.getRandom(), "Coming, coming!", "Be right with you!", "Yes? How can I help?"), 50);
            if (!atCounter(hugo) && hugo.convo == null) hugo.work.reset();
            startSession(pl, true);
            BankNet.send(pl);
        } else {
            msg(pl, "§6[Solaris City Bank] §7The teller window is closed right now. Opening hours: weekdays, work hours until about 17:30. The cash machine outside works 24/7.");
            startSession(pl, false);
        }
    }

    static boolean blockFree(ServerLevel sl, BlockPos p) {
        return sl.getBlockState(p).isAir() || sl.getBlockState(p).is(Blocks.RED_CARPET);
    }
}
