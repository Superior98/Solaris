package com.fireheart.city;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Network messages for the bank window: the server sends the account state, the client sends button actions. */
public final class BankNet {
    private BankNet() {}

    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(FireheartCity.MODID, "bank"), () -> VERSION, VERSION::equals, VERSION::equals);

    static final Set<UUID> QUIET = new HashSet<>();
    static final java.util.Map<UUID, String> NOTICE = new java.util.HashMap<>();

    public static void register() {
        CHANNEL.messageBuilder(State.class, 0, NetworkDirection.PLAY_TO_CLIENT).encoder(State::write).decoder(State::read).consumerMainThread(BankNet::onState).add();
        CHANNEL.messageBuilder(Action.class, 1, NetworkDirection.PLAY_TO_SERVER).encoder(Action::write).decoder(Action::read).consumerMainThread(BankNet::onAction).add();
    }

    public static final class State {
        public boolean has, teller, open;
        public String name = "", notice = "";
        public int number, balance, cash, owed, perDay, missed, tickets, maxTickets, pot, price, rate, loanRate;
        public String lastWinner = "";
        public int lastPrize;
        public final List<String[]> residents = new ArrayList<>();
        public final List<String> statement = new ArrayList<>();

        void write(FriendlyByteBuf b) {
            b.writeBoolean(has); b.writeBoolean(teller); b.writeBoolean(open);
            b.writeUtf(name); b.writeUtf(notice);
            for (int v : new int[]{number, balance, cash, owed, perDay, missed, tickets, maxTickets, pot, price, rate, loanRate, lastPrize}) b.writeVarInt(v);
            b.writeUtf(lastWinner);
            b.writeVarInt(residents.size());
            for (String[] r : residents) { b.writeUtf(r[0]); b.writeUtf(r[1]); b.writeUtf(r[2]); }
            b.writeVarInt(statement.size());
            for (String s : statement) b.writeUtf(s);
        }

        static State read(FriendlyByteBuf b) {
            State s = new State();
            s.has = b.readBoolean(); s.teller = b.readBoolean(); s.open = b.readBoolean();
            s.name = b.readUtf(); s.notice = b.readUtf();
            s.number = b.readVarInt(); s.balance = b.readVarInt(); s.cash = b.readVarInt(); s.owed = b.readVarInt(); s.perDay = b.readVarInt();
            s.missed = b.readVarInt(); s.tickets = b.readVarInt(); s.maxTickets = b.readVarInt(); s.pot = b.readVarInt(); s.price = b.readVarInt();
            s.rate = b.readVarInt(); s.loanRate = b.readVarInt(); s.lastPrize = b.readVarInt();
            s.lastWinner = b.readUtf();
            int n = b.readVarInt();
            for (int i = 0; i < n; i++) s.residents.add(new String[]{b.readUtf(), b.readUtf(), b.readUtf()});
            n = b.readVarInt();
            for (int i = 0; i < n; i++) s.statement.add(b.readUtf());
            return s;
        }
    }

    public static final class Action {
        public final String kind, a, b;

        public Action(String kind, String a, String b) {
            this.kind = kind;
            this.a = a == null ? "" : a;
            this.b = b == null ? "" : b;
        }

        void write(FriendlyByteBuf buf) {
            buf.writeUtf(kind); buf.writeUtf(a); buf.writeUtf(b);
        }

        static Action read(FriendlyByteBuf buf) {
            return new Action(buf.readUtf(), buf.readUtf(), buf.readUtf());
        }
    }

    static void onState(State s, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.fireheart.city.client.ClientBank.show(s));
    }

    static void onAction(Action a, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
        ServerPlayer pl = ctx.get().getSender();
        if (pl == null) return;
        try {
            if (!Bank.session(pl)) {
                NOTICE.put(pl.getUUID(), "§cYou walked away from the bank - come back to the counter or the cash machine.");
                send(pl);
                return;
            }
            QUIET.add(pl.getUUID());
            NOTICE.remove(pl.getUUID());
            Bank.startSession(pl, Boolean.TRUE.equals(Bank.atTeller(pl)));
            switch (a.kind) {
                case "open" -> Bank.cmdOpen(pl);
                case "deposit" -> Bank.cmdDeposit(pl, a.a);
                case "withdraw" -> Bank.cmdWithdraw(pl, a.a);
                case "pay" -> Bank.cmdPay(pl, a.a, a.b);
                case "ticket" -> Bank.cmdTicket(pl, a.a);
                case "loan" -> Bank.cmdLoan(pl, a.a);
                case "repay" -> Bank.cmdRepay(pl, a.a);
                default -> {}
            }
        } catch (Throwable t) {
            FireheartCity.LOG.error("Bank action failed", t);
            NOTICE.put(pl.getUUID(), "§cSomething went wrong - try again.");
        } finally {
            QUIET.remove(pl.getUUID());
        }
        send(pl);
    }

    public static void send(ServerPlayer pl) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> pl), state(pl));
    }

    static State state(ServerPlayer pl) {
        ServerLevel sl = pl.serverLevel();
        CityData d = CityData.get(sl);
        String name = pl.getName().getString();
        String key = Bank.playerKey(name);
        Bank.Holder h = Bank.holder(d, name);
        State s = new State();
        s.name = name;
        s.has = h != null;
        s.teller = Boolean.TRUE.equals(Bank.atTeller(pl));
        s.open = Bank.open(sl, d);
        s.number = h == null ? 0 : h.number;
        s.balance = Bank.savings(d, key);
        s.cash = Bank.cash(pl);
        Bank.Loan l = Bank.loan(d, key);
        if (l != null) { s.owed = l.owed; s.perDay = l.perDay; s.missed = l.missed; }
        s.tickets = Lottery.tickets(d, key);
        s.maxTickets = Lottery.PLAYER_MAX;
        s.pot = Lottery.pot(d);
        s.price = Lottery.PRICE;
        s.rate = Bank.rate(d);
        s.loanRate = Bank.LOAN_RATE;
        s.lastWinner = d.civic.lastWinner.isEmpty() ? "" : d.accountName(d.civic.lastWinner);
        s.lastPrize = d.civic.lastPrize;
        String n = NOTICE.remove(pl.getUUID());
        s.notice = n == null ? "" : n;
        for (CityData.Profile p : d.profiles.values()) s.residents.add(new String[]{p.id, p.name, p.jobTitle()});
        String acct = Bank.sav(key);
        for (int i = d.ledger.size() - 1; i >= 0 && s.statement.size() < 12; i--) {
            CityData.Tx t = d.ledger.get(i);
            if (!t.from.equals(acct) && !t.to.equals(acct)) continue;
            boolean in = t.to.equals(acct);
            String memo = t.memo;
            CityData.Profile to = in ? null : d.profiles.get(t.to);
            if (to != null) memo = "Paid " + to.name;
            else if (memo.startsWith("Loan - ")) memo = "Loan";
            s.statement.add("§8" + Calendar.name(t.day).substring(0, 3) + " " + Calendar.clock(t.time) + " " + (in ? "§a+" : "§c-") + t.amount + " §7" + memo);
        }
        return s;
    }
}
