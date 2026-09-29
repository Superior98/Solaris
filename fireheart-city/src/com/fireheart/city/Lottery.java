package com.fireheart.city;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** The Fireheart Lottery: tickets all week at the bank and cash machine, Sunday evening draw at the plaza. */
public final class Lottery {
    private Lottery() {}

    public static final String POT = "biz:lottery";
    public static final int PRICE = 2;
    public static final int SEED = 25;
    public static final int DRAW_TOD = 11300;
    public static final int PLAYER_MAX = 10;
    public static final BlockPos STAGE = new BlockPos(-20, 71, 28);

    static void newWeek(CityData d, long day) {
        long w = d.civic.week(day);
        if (d.civic.ticketWeek == w) return;
        d.civic.ticketWeek = w;
        d.civic.tickets.clear();
        d.setDirty();
    }

    public static int tickets(CityData d, String owner) {
        return d.civic.tickets.getOrDefault(owner, 0);
    }

    public static int pot(CityData d) {
        return d.balance(POT) + SEED;
    }

    public static long drawDay(long day) {
        return day + (6 - Calendar.weekday(day));
    }

    public static double appetite(CityData.Profile p) {
        return switch (p.trait) {
            case ADVENTUROUS -> 0.75;
            case DREAMY -> 0.65;
            case CHEERFUL, TALKATIVE -> 0.5;
            case FRIENDLY -> 0.4;
            default -> 0.22;
        };
    }

    public static int wants(CityData d, CityData.Profile p, long day) {
        newWeek(d, day);
        if (tickets(d, p.id) > 0) return 0;
        if (p.coins + Bank.savings(d, p.id) < 12) return 0;
        long seed = (p.id.hashCode() * 31L + d.civic.week(day) * 7919L);
        double roll = Math.floorMod(seed, 1000) / 1000.0;
        if (roll >= appetite(p)) return 0;
        return p.trait == Trait.ADVENTUROUS && roll < 0.2 ? 3 : roll < 0.3 ? 2 : 1;
    }

    public static boolean buy(ServerLevel sl, CityData d, String owner, String payer, int n, long rd, int tod) {
        newWeek(d, Calendar.worldDay(sl));
        if (n <= 0 || !d.pay(payer, POT, n * PRICE, n + " lottery ticket" + (n == 1 ? "" : "s"), rd, tod, false)) return false;
        d.civic.tickets.merge(owner, n, Integer::sum);
        CityData.Profile p = d.profiles.get(owner);
        if (p != null) {
            p.log(rd).note("I bought " + (n == 1 ? "a lottery ticket" : n + " lottery tickets"));
            long dd = drawDay(Calendar.worldDay(sl));
            boolean has = false;
            for (CityData.Plan pl : d.plansFor(p.id, dd)) if (pl.what.equals("lottery")) has = true;
            if (!has && !p.livesOnIsland()) d.addPlan(dd, "plaza", "lottery", p.id);
        }
        d.setDirty();
        return true;
    }

    public static void tick(ServerLevel sl, CityData d) {
        long day = Calendar.worldDay(sl);
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        newWeek(d, day);
        if (Calendar.weekday(day) != 6 || tod < DRAW_TOD || tod > 14000 || d.civic.drawDay == day) return;
        draw(sl, d, day);
    }

    public static String draw(ServerLevel sl, CityData d, long day) {
        d.civic.drawDay = day;
        List<String> pool = new ArrayList<>();
        for (Map.Entry<String, Integer> e : d.civic.tickets.entrySet()) for (int i = 0; i < e.getValue(); i++) pool.add(e.getKey());
        long rd = Calendar.day(sl);
        int tod = (int) Math.floorMod(sl.getDayTime(), 24000L);
        if (pool.isEmpty()) {
            d.event(day, "lottery", "nobody bought a lottery ticket this week, so the jackpot rolls over", STAGE);
            return "no tickets";
        }
        String win = pool.get(sl.random.nextInt(pool.size()));
        int sold = pool.size();
        int prize = d.balance(POT);
        int seed = Math.min(SEED, Math.max(0, d.balance(Bank.RESERVE)));
        if (seed > 0) d.pay(Bank.RESERVE, POT, seed, "Lottery jackpot boost", rd, tod, false);
        prize = d.balance(POT);
        d.pay(POT, Bank.sav(win), prize, "Solaris Lottery jackpot!", rd, tod, false);
        d.civic.lastWinner = win;
        d.civic.lastPrize = prize;
        d.civic.lastWinDay = day;
        d.civic.draws++;
        d.civic.tickets.clear();
        String name = d.accountName(win);
        CityData.Profile p = d.profiles.get(win);
        if (p != null) {
            p.fun = 100;
            p.rep += 4;
            p.log(rd).note("I WON THE LOTTERY - " + prize + " coins!");
        }
        d.event(day, "lottery", name + " won the " + prize + " coin Solaris Lottery jackpot", STAGE, p == null ? new String[0] : new String[]{p.id});
        d.setDirty();
        Fireworks.finale(sl, new BlockPos(STAGE.getX(), STAGE.getY() + 1, STAGE.getZ()));
        for (ServerPlayer pl : sl.players()) {
            Calendar.banner(pl, "§6§l★ LOTTERY DRAW ★", "§e" + name + " wins " + prize + " coins! §7(" + sold + " tickets sold)");
            if (win.equals(Bank.playerKey(pl.getName().getString()))) pl.sendSystemMessage(Component.literal("§6[Solaris Lottery] §a§lYOU WON! §r§a" + prize + " coins were paid into your bank account."));
        }
        if (p != null && p.entity != null) {
            Entity e = sl.getEntity(p.entity);
            if (e instanceof Resident r) {
                r.gesture(Resident.G_CHEER, 80);
                r.particles(ParticleTypes.TOTEM_OF_UNDYING, 30);
                r.sayTo(r.pick("I WON?! I WON THE LOTTERY!", "No way... " + prize + " coins?! I'm rich!", "AAAH! Best Sunday EVER!"), 100);
            }
        }
        for (Resident r : sl.getEntitiesOfClass(Resident.class, new net.minecraft.world.phys.AABB(STAGE).inflate(18))) {
            CityData.Profile q = r.profile();
            if (q == null || q.id.equals(win) || r.convo != null) continue;
            if (sl.random.nextFloat() < 0.5f) r.say(r.pick("Aww, maybe next week!", "Congratulations, " + name + "!", "So close... I had the wrong numbers.", "Lucky " + name + "!"), 70);
            r.gesture(sl.random.nextBoolean() ? Resident.G_CHEER : Resident.G_WAVE, 40);
        }
        Post.send(d, "bank", win, "Congratulations from the Solaris City Bank!\n\nYour lottery ticket was drawn on Sunday. The jackpot of " + prize + " coins has been paid into your savings account.\n\nSpend it wisely!\n\n- Hugo, Banker", "lottery", day);
        return name + " won " + prize;
    }

    public static String line(CityData d, CityData.Profile p) {
        int n = tickets(d, p.id);
        if (n <= 0) return null;
        return "I've got " + (n == 1 ? "a lottery ticket" : n + " lottery tickets") + " for Sunday. If I win, I'm getting " + (p.goal.isEmpty() ? "something fancy" : p.goal) + "!";
    }
}
