package com.fireheart.city;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The Sky Launch: a 70-block glass launch tube on its own little plaza at the edge of the city. The launch pad at the
 * bottom fires whoever stands on it straight up the tube and high into the sky for a skydive.
 */
public final class SkyTower {
    private SkyTower() {}

    public static final String KEY = "skylaunch";
    public static final int HEIGHT = 70, R = 2, PLAZA = 6;
    static final int CX = -20, CZ = 30;

    public static BlockPos pad(CityData d) {
        return d.skyPad == Long.MIN_VALUE ? null : BlockPos.of(d.skyPad);
    }

    public static boolean built(CityData d) {
        return d.skyPad != Long.MIN_VALUE;
    }

    static void register(CityData d) {
        BlockPos p = pad(d);
        if (p != null && Place.get(KEY) == null) Place.addDynamic(KEY, "the Sky Launch", p.south(PLAZA - 1));
    }

    static boolean natural(BlockState s) {
        return s.is(BlockTags.DIRT) || s.is(Blocks.GRASS_BLOCK) || s.is(BlockTags.SAND) || s.is(Blocks.GRAVEL) || s.is(Blocks.STONE) || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.DIRT_PATH) || s.is(Blocks.MOSS_BLOCK);
    }

    /** Ground Y of a candidate 13x13 site, or Integer.MIN_VALUE if it isn't flat, natural and open to the sky. */
    static int site(ServerLevel sl, int cx, int cz) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int x = cx - PLAZA; x <= cx + PLAZA; x++) {
            for (int z = cz - PLAZA; z <= cz + PLAZA; z++) {
                if (!sl.isLoaded(new BlockPos(x, 64, z))) return Integer.MIN_VALUE;
                int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                BlockState top = sl.getBlockState(new BlockPos(x, y, z));
                if (!natural(top) || !sl.getFluidState(new BlockPos(x, y + 1, z)).isEmpty()) return Integer.MIN_VALUE;
                min = Math.min(min, y);
                max = Math.max(max, y);
                if (max - min > 4) return Integer.MIN_VALUE;
            }
        }
        for (int x = cx - PLAZA; x <= cx + PLAZA; x += 3) for (int z = cz - PLAZA; z <= cz + PLAZA; z += 3) if (sl.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) > max + 3) return Integer.MIN_VALUE;
        return max;
    }

    public static BlockPos find(ServerLevel sl) {
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (int r = 56; r <= 140; r += 7) {
            for (int a = 0; a < 360; a += 12) {
                int cx = CX + (int) Math.round(r * Math.cos(Math.toRadians(a))), cz = CZ + (int) Math.round(r * Math.sin(Math.toRadians(a)));
                if (cx > -80 && cx < 70 && cz > 200 && cz < 350) continue;
                int y = site(sl, cx, cz);
                if (y == Integer.MIN_VALUE || y < 60 || y > 110) continue;
                double dd = (cx - CX) * (cx - CX) + (cz - CZ) * (cz - CZ);
                if (dd < bd) { bd = dd; best = new BlockPos(cx, y, cz); }
            }
            if (best != null) return best;
        }
        return null;
    }

    static void set(ServerLevel sl, int x, int y, int z, BlockState s) {
        sl.setBlock(new BlockPos(x, y, z), s, 2);
    }

    /** Builds the tower with its ground block at {@code g}; returns the launch pad position. */
    public static BlockPos build(ServerLevel sl, CityData d, BlockPos g) {
        int gx = g.getX(), gy = g.getY(), gz = g.getZ();
        BlockState stone = Blocks.POLISHED_ANDESITE.defaultBlockState(), dark = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState orange = Blocks.ORANGE_CONCRETE.defaultBlockState(), black = Blocks.BLACK_CONCRETE.defaultBlockState();
        BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), frame = Blocks.WAXED_CUT_COPPER.defaultBlockState();
        BlockState light = Blocks.SEA_LANTERN.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
        for (int x = gx - PLAZA; x <= gx + PLAZA; x++) {
            for (int z = gz - PLAZA; z <= gz + PLAZA; z++) {
                for (int y = gy - 5; y < gy; y++) if (sl.getBlockState(new BlockPos(x, y, z)).canBeReplaced() || sl.getBlockState(new BlockPos(x, y, z)).isAir()) set(sl, x, y, z, Blocks.STONE.defaultBlockState());
                int ring = Math.max(Math.abs(x - gx), Math.abs(z - gz));
                set(sl, x, gy, z, ring == PLAZA ? black : ring == PLAZA - 1 && (x + z) % 2 == 0 ? orange : ring <= R + 1 ? dark : stone);
                for (int y = gy + 1; y <= gy + HEIGHT + 6; y++) set(sl, x, y, z, air);
            }
        }
        for (int y = gy + 1; y <= gy + HEIGHT; y++) {
            boolean band = (y - gy) % 10 == 0;
            for (int x = gx - R; x <= gx + R; x++) {
                for (int z = gz - R; z <= gz + R; z++) {
                    int ax = Math.abs(x - gx), az = Math.abs(z - gz);
                    if (ax < R && az < R) continue;
                    boolean corner = ax == R && az == R;
                    boolean door = z == gz + R && ax <= 0 && y <= gy + 3;
                    if (door) { set(sl, x, y, z, air); continue; }
                    set(sl, x, y, z, corner ? (band ? light : frame) : band ? frame : glass);
                }
            }
        }
        int top = gy + HEIGHT;
        for (int x = gx - R - 1; x <= gx + R + 1; x++) for (int z = gz - R - 1; z <= gz + R + 1; z++) {
            int ax = Math.abs(x - gx), az = Math.abs(z - gz);
            if (ax <= R - 1 && az <= R - 1) continue;
            set(sl, x, top + 1, z, ax == R + 1 || az == R + 1 ? Blocks.ORANGE_CONCRETE.defaultBlockState() : frame);
            if ((ax == R + 1) && (az == R + 1)) {
                set(sl, x, top + 2, z, Blocks.END_ROD.defaultBlockState());
                set(sl, x, top + 3, z, Blocks.END_ROD.defaultBlockState());
            }
        }
        for (int x = gx - R + 1; x <= gx + R - 1; x++) for (int z = gz - R + 1; z <= gz + R - 1; z++) set(sl, x, gy, z, orange);
        for (int i = -1; i <= 1; i += 2) {
            set(sl, gx + i * 2, gy + 1, gz + R + 2, Blocks.LANTERN.defaultBlockState());
            set(sl, gx + i * 2, gy + 1, gz + R + 1, Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
            set(sl, gx + i * 2, gy + 2, gz + R + 1, Blocks.LANTERN.defaultBlockState());
        }
        BlockPos pad = new BlockPos(gx, gy + 1, gz);
        set(sl, gx, gy + 1, gz, FireheartCity.LAUNCH_PAD.get().defaultBlockState());
        d.skyPad = pad.asLong();
        d.skyTop = top + 1;
        d.setDirty();
        Place.ALL.remove(KEY);
        register(d);
        FireheartCity.LOG.info("Sky Launch tower built at " + pad.toShortString());
        return pad;
    }

    /** Removes a tower whose launch pad is at {@code pad}: tube, crown, pad and lamps go, the plaza becomes grass. */
    public static void demolish(ServerLevel sl, CityData d, BlockPos pad) {
        int gx = pad.getX(), gy = pad.getY() - 1, gz = pad.getZ();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = gx - PLAZA; x <= gx + PLAZA; x++) for (int z = gz - PLAZA; z <= gz + PLAZA; z++) {
            for (int y = gy + 1; y <= gy + HEIGHT + 4; y++) if (!sl.getBlockState(new BlockPos(x, y, z)).isAir()) set(sl, x, y, z, air);
            set(sl, x, gy, z, Blocks.GRASS_BLOCK.defaultBlockState());
        }
        if (d.skyPad == pad.asLong()) {
            d.skyPad = Long.MIN_VALUE;
            Place.ALL.remove(KEY);
        }
        d.setDirty();
        FireheartCity.LOG.info("Sky Launch tower removed at " + pad.toShortString());
    }

    static final BlockPos DUPLICATE = new BlockPos(-61, 70, 156);

    public static void fixDuplicate(ServerLevel sl, CityData d) {
        if (!d.towerFixed && sl.isLoaded(DUPLICATE)) {
            d.towerFixed = true;
            d.setDirty();
            if (d.skyPad != DUPLICATE.asLong() && sl.getBlockState(DUPLICATE).is(FireheartCity.LAUNCH_PAD.get())) demolish(sl, d, DUPLICATE);
        }
    }

    public static void ensure(ServerLevel sl, CityData d) {
        register(d);
        if (built(d) || d.skySearched || sl.players().isEmpty() || sl.getGameTime() < 600) return;
        if (!sl.isLoaded(new BlockPos(CX, 70, CZ))) return;
        BlockPos g = find(sl);
        d.skySearched = true;
        d.setDirty();
        if (g == null) {
            FireheartCity.LOG.info("Sky Launch: no flat open site found near the city - use /city skytower here");
            return;
        }
        build(sl, d, g);
        d.news(Calendar.worldDay(sl), "The Sky Launch opened on the edge of the city - step on the pad and skydive!");
        d.event(Calendar.worldDay(sl), "fun", "the Sky Launch opened - you can skydive from it", g);
    }

    /** Rebuilds the Founder's statue from StellarFox1's skin once, when every chunk it covers is loaded. */
    public static void statue(ServerLevel sl, CityData d) {
        if (d.statueV2) return;
        for (int cx = -2; cx <= 1; cx++) for (int cz = -6; cz <= -5; cz++) if (sl.getChunkSource().getChunkNow(cx, cz) == null) return;
        d.statueV2 = true;
        d.setDirty();
        net.minecraft.commands.CommandSourceStack src = sl.getServer().createCommandSourceStack().withLevel(sl).withSuppressedOutput().withPermission(4);
        sl.getServer().getCommands().performPrefixedCommand(src, "function fireheartcity:statue/stellar1");
        sl.getServer().getCommands().performPrefixedCommand(src, "function fireheartcity:statue/stellar2");
        FireheartCity.LOG.info("Rebuilt the Founder's statue as StellarFox1");
    }
}
