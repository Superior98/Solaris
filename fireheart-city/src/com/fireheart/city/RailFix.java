package com.fireheart.city;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Closes gaps in the Neon Heights railings that residents can walk through and fall off: glass-pane corners on the
 * diagonal rim that only cover half their block, and missing rail blocks. Only edges a resident can actually reach
 * from the plaza are touched; the open skyport pad and the ferry terminal are left alone.
 */
public final class RailFix {
    private RailFix() {}

    static final int X0 = -72, X1 = 62, Z0 = 218, Z1 = 336, FLOOR = 180;
    static final Direction[] DIRS = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    static boolean excluded(int x, int z) {
        return x >= -14 && x <= 5 && z >= 222 && z <= 240 || x >= 5 && x <= 36 && z >= 224 && z <= 246;
    }

    public static boolean loaded(ServerLevel sl) {
        for (int x = X0; x <= X1 + 15; x += 16) for (int z = Z0; z <= Z1 + 15; z += 16) if (!sl.isLoaded(new BlockPos(Math.min(x, X1), FLOOR, Math.min(z, Z1)))) return false;
        return true;
    }

    static void tick(ServerLevel sl, CityData d) {
        if (d.railsFixed || !d.skyxBuilt || !loaded(sl)) return;
        int n = run(sl);
        d.railsFixed = true;
        d.setDirty();
        FireheartCity.LOG.info("Neon Heights railing check: closed " + n + " gaps");
    }

    static boolean floor(ServerLevel sl, int x, int z) {
        return !sl.getBlockState(new BlockPos(x, FLOOR, z)).isAir();
    }

    static boolean drop(ServerLevel sl, int x, int z) {
        for (int y = FLOOR - 2; y <= FLOOR; y++) if (!sl.getBlockState(new BlockPos(x, y, z)).isAir()) return false;
        return true;
    }

    static boolean pane(BlockState s) {
        return s.getBlock() instanceof IronBarsBlock;
    }

    static boolean barrier(ServerLevel sl, BlockPos p) {
        BlockState s = sl.getBlockState(p);
        if (s.getBlock() instanceof WallBlock || s.getBlock() instanceof FenceBlock) return true;
        return !pane(s) && s.isCollisionShapeFullBlock(sl, p);
    }

    static boolean passable(ServerLevel sl, BlockPos p) {
        BlockState s = sl.getBlockState(p);
        if (s.isAir()) return true;
        if (pane(s) || s.getBlock() instanceof WallBlock || s.getBlock() instanceof FenceBlock) return false;
        var shape = s.getCollisionShape(sl, p);
        return shape.isEmpty() || shape.max(Direction.Axis.Y) <= 0.5;
    }

    static boolean paneBlocks(BlockState s, Direction d) {
        if (d.getAxis() == Direction.Axis.X) return s.getValue(CrossCollisionBlock.NORTH) && s.getValue(CrossCollisionBlock.SOUTH);
        return s.getValue(CrossCollisionBlock.EAST) && s.getValue(CrossCollisionBlock.WEST);
    }

    static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    /** Returns the number of railing gaps closed. */
    public static int run(ServerLevel sl) {
        Place start = Place.get("isle_plaza");
        int sx = start == null ? -4 : start.pos.getX(), sz = start == null ? 261 : start.pos.getZ();
        Set<Long> reach = new HashSet<>();
        ArrayDeque<int[]> q = new ArrayDeque<>();
        reach.add(key(sx, sz));
        q.add(new int[]{sx, sz});
        while (!q.isEmpty()) {
            int[] c = q.poll();
            for (Direction d : DIRS) {
                int nx = c[0] + d.getStepX(), nz = c[1] + d.getStepZ();
                if (nx <= X0 || nx >= X1 || nz <= Z0 || nz >= Z1 || reach.contains(key(nx, nz)) || !floor(sl, nx, nz)) continue;
                if (!passable(sl, new BlockPos(nx, FLOOR + 1, nz)) || !passable(sl, new BlockPos(nx, FLOOR + 2, nz))) continue;
                reach.add(key(nx, nz));
                q.add(new int[]{nx, nz});
            }
        }
        int fixed = 0;
        BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
        for (int x = X0 + 1; x < X1; x++) {
            for (int z = Z0 + 1; z < Z1; z++) {
                if (excluded(x, z) || !floor(sl, x, z)) continue;
                BlockPos rail = new BlockPos(x, FLOOR + 1, z);
                BlockState s = sl.getBlockState(rail);
                boolean isPane = pane(s);
                if (!isPane && !passable(sl, rail)) continue;
                boolean near = reach.contains(key(x, z));
                if (isPane) for (Direction d : DIRS) near |= reach.contains(key(x + d.getStepX(), z + d.getStepZ()));
                if (!near) continue;
                for (Direction d : DIRS) {
                    int nx = x + d.getStepX(), nz = z + d.getStepZ();
                    if (!drop(sl, nx, nz) || barrier(sl, new BlockPos(nx, FLOOR + 1, nz))) continue;
                    if (isPane && paneBlocks(s, d)) continue;
                    sl.setBlock(rail, glass, 3);
                    fixed++;
                    break;
                }
            }
        }
        return fixed;
    }
}
