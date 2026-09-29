package com.fireheart.city;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Read receipts and "is typing..." indicators for SolNet / SolPhone messages. */
public final class Receipts {
    private Receipts() {}

    record Due(String player, String line, long at) {}

    static final List<Due> QUEUE = new ArrayList<>();

    public static void reset() {
        QUEUE.clear();
    }

    static void schedule(String player, String line, long at) {
        QUEUE.add(new Due(player, line, at));
    }

    /** A player messaged a resident: they read it a bit later, then start typing just before the reply lands. */
    public static void residentMessage(ServerLevel sl, String player, CityData.Profile p, long due) {
        long now = sl.getGameTime();
        Resident r = p.entity == null ? null : sl.getEntity(p.entity) instanceof Resident rr ? rr : null;
        boolean asleep = r == null || r.isSleeping() || r.activityName().equals("sleep");
        long span = due - now;
        long read = asleep ? due - 20 : now + Math.max(20, Math.min(span / 3, 400));
        long typing = Math.max(read + 20, due - 40 - sl.random.nextInt(60));
        schedule(player, "#rd|" + p.id, read);
        schedule(player, "#ty|" + p.id + "|1", typing);
    }

    public static void tick(ServerLevel sl) {
        long now = sl.getGameTime();
        for (Iterator<Due> it = QUEUE.iterator(); it.hasNext(); ) {
            Due d = it.next();
            if (now < d.at()) continue;
            it.remove();
            ServerPlayer pl = sl.getServer().getPlayerList().getPlayerByName(d.player());
            if (pl != null) PcNet.send(pl, new PcNet.Msg(d.line()));
        }
    }

    public static void playerRead(ServerPlayer reader, String threadId) {
        if (!PlayerLink.isPlayer(threadId)) return;
        ServerPlayer other = reader.server.getPlayerList().getPlayerByName(threadId.substring(7));
        if (other != null) PcNet.send(other, new PcNet.Msg("#rd|player:" + reader.getName().getString()));
    }

    public static void playerTyping(ServerPlayer typer, String threadId, boolean on) {
        if (!PlayerLink.isPlayer(threadId)) return;
        ServerPlayer other = typer.server.getPlayerList().getPlayerByName(threadId.substring(7));
        if (other != null) PcNet.send(other, new PcNet.Msg("#ty|player:" + typer.getName().getString() + "|" + (on ? "1" : "0")));
    }
}
