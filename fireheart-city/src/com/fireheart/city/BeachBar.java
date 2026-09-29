package com.fireheart.city;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Magma Beach Bar - the bar magmagamer9 planned for the new beach. It's handed to the repair crew as a construction job,
 * so Gus builds it block by block on the sand: stilted deck, tiki bar with a thatched roof, stools, umbrellas and torches.
 */
public final class BeachBar {
    private BeachBar() {}

    static final int X1 = 84, X2 = 98, Z1 = 38, Z2 = 51, DECK = 71;

    final static class Plan {
        final Map<BlockPos, String> blocks = new LinkedHashMap<>();

        void set(int x, int y, int z, String s) {
            blocks.put(new BlockPos(x, y, z), s);
        }

        void fill(int x1, int y1, int z1, int x2, int y2, int z2, String s) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
                    for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, s);
        }
    }

    public static void tick(ServerLevel sl, CityData d) {
        if (d.beachBar || sl.getChunkSource().getChunkNow(X1 >> 4, Z1 >> 4) == null || sl.getChunkSource().getChunkNow(X2 >> 4, Z2 >> 4) == null) return;
        if (!d.profiles.containsKey("gus")) return;
        queue(sl);
        d.beachBar = true;
        d.setDirty();
        d.event(Calendar.worldDay(sl), "city", "construction started on the Magma Beach Bar by the sea", new BlockPos(91, 72, 45), "gus");
    }

    public static int queue(ServerLevel sl) {
        Builder b = new Builder(sl);
        Plan p = plan(sl);
        List<BlockPos> pos = new ArrayList<>();
        List<String> st = new ArrayList<>();
        List<Map.Entry<BlockPos, String>> order = new ArrayList<>(p.blocks.entrySet());
        order.sort((a, c) -> a.getKey().getY() != c.getKey().getY() ? Integer.compare(a.getKey().getY(), c.getKey().getY()) : Integer.compare(a.getKey().getX() * 64 + a.getKey().getZ(), c.getKey().getX() * 64 + c.getKey().getZ()));
        for (var e : order) {
            BlockState s = b.st(e.getValue());
            pos.add(e.getKey());
            st.add(net.minecraft.nbt.NbtUtils.writeBlockState(s).toString());
        }
        Repair.construct(sl, pos, st);
        return pos.size();
    }

    static int ground(ServerLevel sl, int x, int z) {
        for (int y = DECK - 1; y > 40; y--) {
            BlockState s = sl.getBlockState(new BlockPos(x, y, z));
            if (!s.isAir() && s.getFluidState().isEmpty() && !s.canBeReplaced()) return y;
        }
        return 40;
    }

    static Plan plan(ServerLevel sl) {
        Plan p = new Plan();
        String plank = "minecraft:spruce_planks", log = "minecraft:stripped_spruce_log[axis=y]", fence = "minecraft:bamboo_fence|minecraft:spruce_fence";
        for (int x = X1; x <= X2; x++) for (int z = Z1; z <= Z2; z++) {
            boolean edge = x == X1 || x == X2 || z == Z1 || z == Z2;
            p.set(x, DECK, z, edge ? "minecraft:stripped_spruce_wood[axis=x]" : ((x + z) % 2 == 0 ? plank : "minecraft:bamboo_planks|minecraft:birch_planks"));
            if (((x - X1) % 4 == 0 || x == X2) && ((z - Z1) % 4 == 0 || z == Z2)) {
                int g = ground(sl, x, z);
                for (int y = g + 1; y < DECK; y++) p.set(x, y, z, log);
            }
        }
        for (int x = X1; x <= X2; x++) for (int z : new int[]{Z1, Z2}) if (x % 2 == 0 && x != X1 + 6 && x != X1 + 7) p.set(x, DECK + 1, z, fence);
        for (int z = Z1; z <= Z2; z++) for (int x : new int[]{X2}) if (z % 2 == 0) p.set(x, DECK + 1, z, fence);
        for (int[] t : new int[][]{{X1, Z1}, {X2, Z1}, {X1, Z2}, {X2, Z2}, {X2, Z1 + 6}, {X2, Z1 + 7}}) {
            p.set(t[0], DECK + 1, t[1], fence);
            p.set(t[0], DECK + 2, t[1], fence);
            p.set(t[0], DECK + 3, t[1], "minecraft:torch");
        }
        int bx1 = 88, bx2 = 94, bz1 = 41, bz2 = 48;
        for (int x = bx1; x <= bx2; x++) {
            p.set(x, DECK + 1, bz1, "minecraft:stripped_bamboo_block[axis=x]|minecraft:stripped_oak_log[axis=x]");
            p.set(x, DECK + 2, bz1, "minecraft:smooth_quartz_slab[type=bottom]");
            p.set(x, DECK + 1, bz2, "minecraft:stripped_bamboo_block[axis=x]|minecraft:stripped_oak_log[axis=x]");
            p.set(x, DECK + 2, bz2, "minecraft:smooth_quartz_slab[type=bottom]");
        }
        for (int z = bz1 + 1; z < bz2; z++) {
            p.set(bx1, DECK + 1, z, "minecraft:stripped_bamboo_block[axis=z]|minecraft:stripped_oak_log[axis=z]");
            p.set(bx1, DECK + 2, z, "minecraft:smooth_quartz_slab[type=bottom]");
        }
        p.set(bx2, DECK + 1, bz1 + 3, "minecraft:barrel[facing=up]");
        p.set(bx2, DECK + 1, bz1 + 4, "minecraft:barrel[facing=up]");
        p.set(bx2 - 1, DECK + 1, bz1 + 2, "minecraft:smoker[facing=west]");
        p.set(bx2 - 1, DECK + 1, bz2 - 2, "minecraft:brewing_stand");
        p.set(bx1 + 2, DECK + 3, bz1, "minecraft:potted_cactus");
        p.set(bx1 + 4, DECK + 3, bz1, "minecraft:candle[candles=3,lit=true]");
        p.set(bx1, DECK + 3, bz1 + 3, "minecraft:potted_azalea_bush");
        p.set(bx1 + 3, DECK + 3, bz2, "minecraft:sea_pickle[pickles=3,waterlogged=false]");
        p.set(bx1 + 5, DECK + 3, bz2, "minecraft:candle[candles=2,lit=true]");
        for (int[] c : new int[][]{{bx1, bz1}, {bx2, bz1}, {bx1, bz2}, {bx2, bz2}}) for (int y = DECK + 3; y <= DECK + 4; y++) p.set(c[0], y, c[1], "minecraft:bamboo_fence|minecraft:oak_fence");
        for (int layer = 0; layer < 4; layer++) {
            int y = DECK + 5 + layer;
            for (int x = bx1 - 1 + layer; x <= bx2 + 1 - layer; x++) for (int z = bz1 - 1 + layer; z <= bz2 + 1 - layer; z++) {
                boolean rim = x == bx1 - 1 + layer || x == bx2 + 1 - layer || z == bz1 - 1 + layer || z == bz2 + 1 - layer;
                if (layer < 3 && !rim) continue;
                p.set(x, y, z, layer == 0 ? "minecraft:hay_block[axis=y]" : (x + z) % 3 == 0 ? "minecraft:hay_block[axis=y]" : "minecraft:bamboo_mosaic_slab[type=bottom]|minecraft:oak_slab[type=bottom]");
            }
        }
        p.set((bx1 + bx2) / 2, DECK + 4, (bz1 + bz2) / 2, "minecraft:lantern[hanging=true]");
        p.set(bx1, DECK + 4, bz1 + 3, "minecraft:lantern[hanging=false]");
        String stool = "another_furniture:oak_stool|minecraft:spruce_fence";
        for (int x = bx1 + 1; x < bx2; x += 2) {
            p.set(x, DECK + 1, bz1 - 1, stool);
            p.set(x, DECK + 1, bz2 + 1, stool);
        }
        for (int z = bz1 + 1; z < bz2; z += 2) p.set(bx1 - 1, DECK + 1, z, stool);
        umbrella(p, sl, 86, 35, "minecraft:red_wool", "minecraft:white_wool");
        umbrella(p, sl, 86, 54, "minecraft:orange_wool", "minecraft:yellow_wool");
        umbrella(p, sl, 80, 40, "minecraft:cyan_wool", "minecraft:white_wool");
        p.set(96, DECK + 1, 40, "minecraft:jukebox");
        p.set(X1 + 1, DECK + 1, Z1 + 1, "minecraft:potted_bamboo");
        p.set(X1 + 1, DECK + 1, Z2 - 1, "minecraft:potted_bamboo");
        return p;
    }

    static void umbrella(Plan p, ServerLevel sl, int x, int z, String a, String b) {
        int y = ground(sl, x, z) + 1;
        if (!sl.getBlockState(new BlockPos(x, DECK, z)).isAir() && y <= DECK) y = DECK + 1;
        for (int i = 0; i < 3; i++) p.set(x, y + i, z, "minecraft:bamboo_fence|minecraft:oak_fence");
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) p.set(x + dx, y + 3, z + dz, (dx + dz) % 2 == 0 ? a : b);
        p.set(x + 2, y + 3, z, a.replace("_wool", "_carpet"));
        p.set(x - 2, y + 3, z, b.replace("_wool", "_carpet"));
        p.set(x, y + 3, z + 2, b.replace("_wool", "_carpet"));
        p.set(x, y + 3, z - 2, a.replace("_wool", "_carpet"));
    }
}
