package com.fireheart.city;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Notice boards: lecterns holding a "Solaris Gazette" book that the mod keeps up to date. */
public final class Gazette {
    private static int lastSignature = Integer.MIN_VALUE;

    private Gazette() {}

    public static void syncPlaces(CityData d) {
        Place.ALL.keySet().removeIf(k -> k.startsWith("board"));
        for (int i = 0; i < d.boards.size(); i++) Place.addDynamic("board" + i, "the notice board", d.boards.get(i));
    }

    public static String nearestBoardKey(CityData d, boolean island, BlockPos from, double max) {
        String best = null;
        double bd = max * max;
        for (int i = 0; i < d.boards.size(); i++) {
            BlockPos b = d.boards.get(i);
            if ((b.getY() > 150) != island) continue;
            double dd = b.distSqr(from);
            if (dd < bd) { bd = dd; best = "board" + i; }
        }
        return best;
    }

    public static BlockPos boardNear(CityData d, BlockPos from, double r) {
        for (BlockPos b : d.boards) if (b.closerThan(from, r)) return b;
        return null;
    }

    public static void tick(ServerLevel sl, CityData d, boolean force) {
        if (d.boards.isEmpty()) return;
        int sig = signature(sl, d);
        if (!force && sig == lastSignature) return;
        lastSignature = sig;
        ItemStack book = null;
        Iterator<BlockPos> it = d.boards.iterator();
        boolean removed = false;
        while (it.hasNext()) {
            BlockPos pos = it.next();
            if (!sl.isLoaded(pos)) continue;
            BlockState st = sl.getBlockState(pos);
            if (!(st.getBlock() instanceof LecternBlock)) {
                it.remove();
                removed = true;
                continue;
            }
            BlockEntity be = sl.getBlockEntity(pos);
            if (!(be instanceof LecternBlockEntity lec)) continue;
            if (book == null) book = book(sl, d);
            lec.setBook(book.copy());
            if (!st.getValue(LecternBlock.HAS_BOOK)) LecternBlock.resetBookState(null, sl, pos, st, true);
            lec.setChanged();
        }
        if (removed) {
            syncPlaces(d);
            d.setDirty();
        }
    }

    private static int signature(ServerLevel sl, CityData d) {
        int s = d.nextEvent * 31 + (int) Calendar.dayOf(sl.getDayTime());
        for (CityData.Profile p : d.profiles.values()) s = s * 17 + p.tier * 3 + (p.partner.isEmpty() ? 0 : 1);
        return s * 7 + d.boards.size() + (sl.isRaining() ? 1 : 0);
    }

    public static ItemStack book(ServerLevel sl, CityData d) {
        long day = Calendar.dayOf(sl.getDayTime());
        List<String> pages = new ArrayList<>();
        int couples = 0, friendships = 0;
        CityData.Profile top = null;
        int topScore = -1;
        for (CityData.Profile p : d.profiles.values()) {
            if (!p.partner.isEmpty()) couples++;
            friendships += d.friendsCount(p.id);
            int sc = d.statusScore(p);
            if (sc > topScore) { topScore = sc; top = p; }
        }
        String weather = sl.isThundering() ? "Stormy" : sl.isRaining() ? "Rainy" : "Clear skies";
        StringBuilder front = new StringBuilder();
        front.append("§l FIREHEART\n   GAZETTE§r\n§8------------------§r\n");
        front.append(Calendar.stamp(day)).append(" · ").append(weather).append("\n\n");
        front.append(d.profiles.size()).append(" residents\n");
        front.append(couples / 2).append(couples / 2 == 1 ? " couple\n" : " couples\n");
        front.append(friendships / 2).append(" friendships\n\n");
        if (top != null) front.append("§oMost admired:§r\n").append(top.name).append(", ").append(CityData.TIERS[top.tier]);
        pages.add(front.toString());

        StringBuilder hall = new StringBuilder("§lCity Hall§r\n");
        CityData.Profile mayor = Mayor.mayor(d);
        hall.append(mayor == null ? "No mayor yet.\n" : "Mayor: " + mayor.name + "\nPolicy: " + Mayor.policyName(d.civic.policy) + "\n§8" + Mayor.policyText(d.civic.policy) + "§r\n");
        if (!d.civic.candidates.isEmpty() && d.civic.electionWeek != d.civic.announceWeek) {
            hall.append("\n§oElection Saturday:§r\n");
            for (String c : d.civic.candidates) if (d.profiles.get(c) != null) hall.append(d.profiles.get(c).name).append(" - ").append(Mayor.policyName(Mayor.platform(d.profiles.get(c)))).append("\n");
        }
        hall.append("\n§lLottery§r jackpot " + Lottery.pot(d) + "\n");
        if (!d.civic.lastWinner.isEmpty()) hall.append("Last winner: " + d.accountName(d.civic.lastWinner) + " (" + d.civic.lastPrize + ")\n");
        pages.add(hall.toString());

        List<String> lines = new ArrayList<>();
        for (int i = d.events.size() - 1; i >= 0 && lines.size() < 18; i--) {
            CityData.Event e = d.events.get(i);
            String t = e.text.substring(0, 1).toUpperCase() + e.text.substring(1);
            lines.add("§6Day " + (e.day + 1) + "§r " + t + ".");
        }
        if (lines.isEmpty()) lines.add("No news yet. Solaris is quiet today.");
        StringBuilder cur = new StringBuilder("§lLatest news§r\n");
        for (String l : lines) {
            if (cur.length() + l.length() > 190) {
                pages.add(cur.toString());
                cur = new StringBuilder();
            }
            cur.append(l).append("\n\n");
        }
        if (cur.length() > 0) pages.add(cur.toString());

        cur = new StringBuilder("§lOverheard§r\n§8what residents are thinking§r\n\n");
        int th = 0;
        for (CityData.Profile p : d.profiles.values()) {
            if (p.mind.thought.isEmpty() || p.mind.thought.startsWith("A quiet day") || th >= 6) continue;
            String l = "§6" + p.name + ":§r \"" + p.mind.thought + "\"";
            if (cur.length() + l.length() > 200) {
                pages.add(cur.toString());
                cur = new StringBuilder();
            }
            cur.append(l).append("\n\n");
            th++;
        }
        if (th > 0) pages.add(cur.toString());

        StringBuilder hs = new StringBuilder("§lSolNet high scores§r\n\n");
        int rows = 0;
        for (int gi = 0; gi < Computers.GAMES.length; gi++) {
            java.util.Map<String, Integer> m = d.scores.get(Computers.GAMES[gi]);
            if (m == null || m.isEmpty()) continue;
            var best = m.entrySet().stream().max(java.util.Map.Entry.comparingByValue()).get();
            hs.append(Computers.GAME_NAMES[gi]).append(": §6").append(best.getKey()).append("§r ").append(best.getValue()).append("\n");
            rows++;
        }
        long owners = d.profiles.values().stream().filter(x -> x.ownsPC).count();
        hs.append("\n").append(owners).append(owners == 1 ? " resident owns" : " residents own").append(" a Solaris PC (").append(Computers.PRICE).append(" coins at Create Supply Co.).");
        if (rows > 0 || owners > 0) pages.add(hs.toString());

        cur = new StringBuilder("§lTrending on SolFeed§r\n\n");
        List<Phones.Post> top3 = new ArrayList<>(d.feed);
        top3.removeIf(x -> day - x.day > 2);
        top3.sort((a, b) -> (b.likes.size() * 2 + b.comments.size()) - (a.likes.size() * 2 + a.comments.size()));
        int tn = 0;
        for (Phones.Post x : top3) {
            if (tn >= 2) break;
            String l = "§6" + Phones.authorName(d, x.author) + "§r: " + (x.text.length() > 60 ? x.text.substring(0, 58) + "..." : x.text) + " §c♥" + x.likes.size() + "§r";
            cur.append(l).append("\n\n");
            tn++;
        }
        long phones = d.profiles.values().stream().filter(x -> x.ownsPhone).count();
        cur.append("§8").append(phones).append(" on SolPhone. Get yours at SolTech!");
        if (tn > 0 || phones > 0) pages.add(cur.toString());

        cur = new StringBuilder("§lSky Ferry§r\n§8city pad <-> Neon Heights§r\n\nFlies whenever someone's waiting. Captain: Jet (weekdays).\n\nRight-click the ferry to hop on. Sneak to get off.\n\nNew up top: §2Sky Gardens§r (west bridge) and the §3Observatory§r (south of the Sky Organ).");
        pages.add(cur.toString());

        cur = new StringBuilder("§lWho's who§r\n");
        int n = 0;
        for (CityData.Profile p : d.profiles.values()) {
            String stars = p.tier > 0 ? " " + "★".repeat(p.tier) : "";
            String l = p.name + " §8" + p.jobTitle() + "§r" + stars + (p.partner.isEmpty() ? "" : " §c❤§r");
            if (n > 0 && n % 6 == 0) {
                pages.add(cur.toString());
                cur = new StringBuilder();
            }
            cur.append(l).append("\n");
            n++;
        }
        pages.add(cur.toString());

        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("title", "Solaris Gazette");
        tag.putString("author", "The Residents");
        tag.putBoolean("resolved", true);
        ListTag pl = new ListTag();
        for (String p : pages) pl.add(StringTag.valueOf(json(p)));
        tag.put("pages", pl);
        return stack;
    }

    private static String json(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                default -> b.append(ch);
            }
        }
        return b.append('"').toString();
    }
}
