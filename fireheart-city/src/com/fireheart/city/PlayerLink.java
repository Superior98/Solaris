package com.fireheart.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Player-to-player texting and calls on the SolPhone (contacts with ids "player:Name"). */
public final class PlayerLink {
    private PlayerLink() {}

    static final class P2P {
        UUID a, b;
        String an, bn;
        boolean live;
        long start;
    }

    static final Map<UUID, P2P> CALLS = new HashMap<>();

    public static boolean isPlayer(String id) {
        return id != null && id.startsWith("player:");
    }

    static String name(String id) {
        return id.substring(7);
    }

    public static void seen(CityData d, String name) {
        if (!d.knownPlayers.contains(name)) {
            d.knownPlayers.add(name);
            d.setDirty();
        }
    }

    public static void contacts(ServerPlayer pl, CityData d, List<String[]> out) {
        String me = pl.getName().getString();
        seen(d, me);
        List<String> names = new ArrayList<>(d.knownPlayers);
        for (ServerPlayer o : pl.server.getPlayerList().getPlayers()) if (!names.contains(o.getName().getString())) names.add(o.getName().getString());
        for (String n : names) {
            if (n.equals(me) || n.startsWith("[") || n.equals("Tester")) continue;
            boolean on = pl.server.getPlayerList().getPlayerByName(n) != null;
            out.add(0, new String[]{"player:" + n, n, on ? "Player · online" : "Player · offline", "1", "1", "-1"});
        }
    }

    public static void text(ServerPlayer pl, CityData d, String id, String text) {
        ServerLevel sl = pl.serverLevel();
        String me = pl.getName().getString(), to = name(id);
        if (text.length() > 300) text = text.substring(0, 300);
        long day = Calendar.worldDay(sl);
        Computers.addInbox(d, Bank.playerKey(me), id, ">", day, text);
        PcNet.send(pl, new PcNet.Msg(id + "|>|" + day + "|" + text));
        Computers.deliver(sl, d, to, "player:" + me, text);
    }

    static void send(ServerPlayer p, String line) {
        if (p != null) PcNet.send(p, new PcNet.Msg(line));
    }

    public static boolean dial(ServerPlayer pl, String id) {
        if (!isPlayer(id)) return false;
        String me = pl.getName().getString(), to = name(id);
        ServerPlayer t = pl.server.getPlayerList().getPlayerByName(to);
        if (CALLS.containsKey(pl.getUUID()) || Phones.PLAYER_CALLS.containsKey(pl.getUUID())) return true;
        if (t == null) {
            send(pl, "#call|end|" + id + "|" + to + " is offline. Send them a message instead!");
            return true;
        }
        if (CALLS.containsKey(t.getUUID()) || Phones.PLAYER_CALLS.containsKey(t.getUUID())) {
            send(pl, "#call|end|" + id + "|Busy tone... " + to + " is on another call.");
            return true;
        }
        if (!Phones.playerHasPhone(t)) {
            send(pl, "#call|end|" + id + "|" + to + " doesn't have their SolPhone on them.");
            return true;
        }
        P2P c = new P2P();
        c.a = pl.getUUID();
        c.b = t.getUUID();
        c.an = me;
        c.bn = to;
        c.start = pl.serverLevel().getGameTime();
        CALLS.put(c.a, c);
        CALLS.put(c.b, c);
        send(pl, "#call|ring_out|" + id + "|" + to);
        send(t, "#call|incoming|player:" + me + "|" + me);
        Phones.toast(t, "SolPhone", me + " is calling you - press P to answer");
        return true;
    }

    public static boolean answer(ServerPlayer pl) {
        P2P c = CALLS.get(pl.getUUID());
        if (c == null) return false;
        if (c.live || !pl.getUUID().equals(c.b)) return true;
        c.live = true;
        ServerPlayer a = pl.server.getPlayerList().getPlayer(c.a);
        send(a, "#call|live|player:" + c.bn + "|" + c.bn);
        send(pl, "#call|live|player:" + c.an + "|" + c.an);
        return true;
    }

    public static boolean say(ServerPlayer pl, String text) {
        P2P c = CALLS.get(pl.getUUID());
        if (c == null) return false;
        if (!c.live || text.isBlank()) return true;
        boolean isA = pl.getUUID().equals(c.a);
        ServerPlayer o = pl.server.getPlayerList().getPlayer(isA ? c.b : c.a);
        send(o, "#call|line|player:" + (isA ? c.an : c.bn) + "|" + Phones.clean(text));
        return true;
    }

    public static boolean hangup(ServerPlayer pl) {
        P2P c = CALLS.get(pl.getUUID());
        if (c == null) return false;
        end(pl.server, c, pl.getName().getString() + " hung up.");
        return true;
    }

    static void end(net.minecraft.server.MinecraftServer s, P2P c, String why) {
        CALLS.remove(c.a);
        CALLS.remove(c.b);
        ServerPlayer a = s.getPlayerList().getPlayer(c.a), b = s.getPlayerList().getPlayer(c.b);
        send(a, "#call|end|player:" + c.bn + "|" + why);
        send(b, "#call|end|player:" + c.an + "|" + why);
    }

    public static void tick(net.minecraft.server.MinecraftServer s, long now) {
        for (P2P c : new ArrayList<>(CALLS.values())) {
            if (!CALLS.containsKey(c.a)) continue;
            boolean gone = s.getPlayerList().getPlayer(c.a) == null || s.getPlayerList().getPlayer(c.b) == null;
            if (gone) end(s, c, "The call dropped.");
            else if (!c.live && now - c.start > 600) end(s, c, "No answer.");
        }
    }
}
