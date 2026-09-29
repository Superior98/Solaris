package com.fireheart.city;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * What each TV is showing, so everyone nearby sees the same picture: a SolTube video, the SolBox console, a photo
 * slideshow... Programs are short strings sent by the player driving the TV; idle TVs show the SolTV channel.
 */
public final class TvShows {
    private TvShows() {}

    record Show(String program, long start, long seen, String by) {}

    static final Map<Long, Show> SHOWS = new HashMap<>();

    public static void set(ServerPlayer pl, BlockPos pos, String program, int offset) {
        if (!(pl.serverLevel().getBlockState(pos).getBlock() instanceof TvBlock) || pos.distSqr(pl.blockPosition()) > 32 * 32) return;
        if (program.length() > 200) program = program.substring(0, 200);
        program = program.replace("|", "/");
        long now = pl.serverLevel().getGameTime();
        Show old = SHOWS.get(pos.asLong());
        long start = old != null && old.program.equals(program) && offset < 0 ? old.start : now - Math.max(0, offset);
        Show s = new Show(program, start, now, pl.getName().getString());
        SHOWS.put(pos.asLong(), s);
        if (old == null || !old.program.equals(program) || offset >= 0) broadcast(pl.serverLevel(), pos, s);
    }

    /** Puts a video link on the cinema booth TV (plays on the big screen). Stays until changed or stopped. */
    public static void cinema(ServerLevel sl, String link, String by) {
        CityData d = CityData.get(sl);
        d.cinemaUrl = link == null ? "" : link;
        d.setDirty();
        BlockPos pos = Expansion.BOOTH_TV;
        if (link == null || link.isEmpty()) {
            clear(sl, pos);
            return;
        }
        long now = sl.getGameTime();
        Show s = new Show("url;" + link.replace("|", "%7C"), now, now, by);
        SHOWS.put(pos.asLong(), s);
        broadcast(sl, pos, s);
    }

    public static void clear(ServerLevel sl, BlockPos pos) {
        if (SHOWS.remove(pos.asLong()) != null) broadcast(sl, pos, new Show("", sl.getGameTime(), sl.getGameTime(), ""));
    }

    static void broadcast(ServerLevel sl, BlockPos pos, Show s) {
        String line = "#tv|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ() + "|" + (sl.getGameTime() - s.start) + "|" + s.program;
        for (ServerPlayer p : sl.players()) if (p.blockPosition().distSqr(pos) < 96 * 96) PcNet.send(p, new PcNet.Msg(line));
    }

    public static void tick(ServerLevel sl) {
        long now = sl.getGameTime();
        CityData cd = CityData.get(sl);
        if (!cd.cinemaUrl.isEmpty() && !SHOWS.containsKey(Expansion.BOOTH_TV.asLong())) {
            Show s = new Show("url;" + cd.cinemaUrl, now, now, "");
            SHOWS.put(Expansion.BOOTH_TV.asLong(), s);
        }
        Iterator<Map.Entry<Long, Show>> it = SHOWS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Show> e = it.next();
            BlockPos pos = BlockPos.of(e.getKey());
            Show s = e.getValue();
            boolean console = s.program.startsWith("console");
            if (s.program.startsWith("url;")) {
                if (now % 100 == 0) broadcast(sl, pos, s);
                continue;
            }
            if (now - s.seen > (console ? 200 : 2400) || sl.isLoaded(pos) && !(sl.getBlockState(pos).getBlock() instanceof TvBlock)) {
                it.remove();
                broadcast(sl, pos, new Show("", now, now, ""));
                continue;
            }
            broadcast(sl, pos, s);
        }
    }
}
