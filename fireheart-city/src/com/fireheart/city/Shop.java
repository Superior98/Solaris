package com.fireheart.city;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Shop stock: real display blocks (depots, barrels) plus a back-room shelf, per workplace. */
public final class Shop {
    private Shop() {}

    public static final Map<String, List<BlockPos>> DISPLAYS = new LinkedHashMap<>();
    public static final int SHELF_MAX = 24;

    static {
        DISPLAYS.put("bakery", List.of(new BlockPos(-38, 71, 6), new BlockPos(-37, 71, 6), new BlockPos(-36, 71, 6)));
        DISPLAYS.put("supply", List.of(new BlockPos(17, 71, 53), new BlockPos(22, 71, 53)));
        DISPLAYS.put("market", List.of(new BlockPos(28, 71, 54), new BlockPos(29, 71, 54), new BlockPos(30, 71, 54)));
    }

    public static boolean sellable(Job j, String id) {
        return Economy.products(j).contains(id) || (j == Job.BAKER && Economy.isFood(id)) || (j == Job.COOK && Economy.isFood(id));
    }

    public static Map<String, Integer> contents(ServerLevel l, CityData d, Job j) {
        Map<String, Integer> out = new LinkedHashMap<>();
        List<BlockPos> disp = DISPLAYS.get(j.workKey);
        if (disp != null) {
            for (BlockPos p : disp) {
                String id = Inv.firstItem(l, p);
                if (id != null && sellable(j, id)) out.merge(id, Inv.count(l, p, id), Integer::sum);
            }
        }
        d.shelf(j.workKey).forEach((k, v) -> { if (v > 0) out.merge(k, v, Integer::sum); });
        return out;
    }

    public static int stock(ServerLevel l, CityData d, Job j) {
        int n = 0;
        for (int v : contents(l, d, j).values()) n += v;
        return n;
    }

    public static boolean hasFood(ServerLevel l, CityData d, Job j) {
        for (Map.Entry<String, Integer> e : contents(l, d, j).entrySet()) if (Economy.isFood(e.getKey()) && e.getValue() > 0) return true;
        return false;
    }

    public static String choose(ServerLevel l, CityData d, Job j, boolean foodOnly, CityData.Profile buyer) {
        List<String> opts = new ArrayList<>();
        for (Map.Entry<String, Integer> e : contents(l, d, j).entrySet()) {
            if (e.getValue() <= 0 || foodOnly && !Economy.isFood(e.getKey())) continue;
            if (buyer != null && Economy.price(e.getKey()) > buyer.coins) continue;
            opts.add(e.getKey());
        }
        if (opts.isEmpty()) return null;
        if (buyer != null) {
            String fav = Memory.favourite(buyer);
            if (opts.contains(fav) && Math.random() < 0.5) return fav;
        }
        return opts.get((int) (Math.random() * opts.size()));
    }

    public static boolean take(ServerLevel l, CityData d, Job j, String id) {
        Map<String, Integer> sh = d.shelf(j.workKey);
        List<BlockPos> disp = DISPLAYS.get(j.workKey);
        if (disp != null) for (BlockPos p : disp) if (Inv.take(l, p, id, 1) == 1) { d.setDirty(); return true; }
        int v = sh.getOrDefault(id, 0);
        if (v > 0) {
            if (v == 1) sh.remove(id); else sh.put(id, v - 1);
            d.setDirty();
            return true;
        }
        return false;
    }

    public static int put(ServerLevel l, CityData d, Job j, String id, int n) {
        int placed = 0;
        List<BlockPos> disp = DISPLAYS.get(j.workKey);
        if (disp != null) {
            for (BlockPos p : disp) {
                if (placed >= n) break;
                String cur = Inv.firstItem(l, p);
                if (cur != null && !cur.equals(id)) continue;
                if (cur == null && j.workKey.equals("bakery") && !bakerySlot(p, id)) continue;
                placed += Inv.put(l, p, id, Math.min(n - placed, 16 - Inv.count(l, p, id)));
            }
        }
        Map<String, Integer> sh = d.shelf(j.workKey);
        int room = SHELF_MAX - sh.getOrDefault(id, 0);
        int back = Math.max(0, Math.min(room, n - placed));
        if (back > 0) sh.merge(id, back, Integer::sum);
        d.setDirty();
        return placed + back;
    }

    private static boolean bakerySlot(BlockPos p, String id) {
        int x = p.getX();
        if (id.equals("minecraft:bread")) return x == -38;
        if (id.contains("cookie")) return x == -36;
        return x == -37 || x == -36;
    }

    public static String sell(ServerLevel l, CityData d, CityData.Profile buyer, Job j, boolean foodOnly, CityData.Profile vendor) {
        String item = choose(l, d, j, foodOnly, buyer);
        if (item == null) return null;
        int price = Economy.price(item);
        if (buyer.coins < price) return null;
        if (!take(l, d, j, item)) return null;
        long day = Calendar.day(l);
        int tod = (int) Math.floorMod(l.getDayTime(), 24000L);
        d.pay(Civic.foodPayer(d, buyer.id, day), Economy.business(j), price, Economy.label(item) + " at " + j.work().label, day, tod);
        buyer.add(item, 1);
        buyer.log(day).note("I bought " + Economy.label(item) + " at " + j.work().label);
        if (vendor != null && vendor != buyer) {
            DayLog vl = vendor.log(day);
            vl.served++;
            vl.note("Sold " + Economy.label(item) + " to " + buyer.name);
        }
        return item;
    }
}
