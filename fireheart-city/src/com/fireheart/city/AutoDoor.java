package com.fireheart.city;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Sliding doors that open when a player or resident walks up and close behind them. The panel slides one block per
 * step between a closed and an open position. Replaces the factory's piston door, whose moving contraption was lost.
 */
public final class AutoDoor {
    final String name;
    final BlockPos closedMin;
    final int w, h, dx, dz, steps;
    final BlockState panel;
    final AABB sense;
    int step, idle;

    AutoDoor(String name, BlockPos closedMin, int w, int h, int dx, int dz, int steps, BlockState panel, AABB sense) {
        this.name = name;
        this.closedMin = closedMin;
        this.w = w;
        this.h = h;
        this.dx = dx;
        this.dz = dz;
        this.steps = steps;
        this.panel = panel;
        this.sense = sense;
    }

    public static final List<AutoDoor> DOORS = new ArrayList<>();

    static {
        DOORS.add(new AutoDoor("factory", new BlockPos(-3, 71, 12), 3, 3, 1, 0, 3, Blocks.IRON_BLOCK.defaultBlockState(), new AABB(-5, 69, 8, 1, 75, 16)));
    }

    List<BlockPos> cells(int s) {
        List<BlockPos> l = new ArrayList<>();
        for (int i = 0; i < w; i++) for (int y = 0; y < h; y++) {
            int ox = dx != 0 ? i : 0, oz = dz != 0 ? i : 0;
            l.add(closedMin.offset(ox + dx * s, y, oz + dz * s));
        }
        return l;
    }

    static boolean free(BlockState s, BlockState panel) {
        return s.isAir() || s.is(panel.getBlock()) || s.canBeReplaced();
    }

    void place(ServerLevel sl, int s) {
        for (BlockPos p : cells(step)) if (sl.getBlockState(p).is(panel.getBlock())) sl.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        for (BlockPos p : cells(s)) if (free(sl.getBlockState(p), panel)) sl.setBlock(p, panel, 3);
        step = s;
    }

    void tick(ServerLevel sl) {
        if (!sl.isLoaded(closedMin)) return;
        boolean near = !sl.getEntitiesOfClass(LivingEntity.class, sense, e -> e instanceof net.minecraft.world.entity.player.Player || e instanceof Resident).isEmpty();
        int target = near ? steps : 0;
        if (near) idle = 0;
        else if (++idle < 8) target = step;
        if (target == step) {
            boolean ok = true;
            for (BlockPos p : cells(step)) if (sl.getBlockState(p).isAir()) ok = false;
            if (!ok) place(sl, step);
            return;
        }
        int next = step + Integer.signum(target - step);
        if (step == 0 || next == 0) sl.playSound(null, closedMin, next > step ? SoundEvents.PISTON_CONTRACT : SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5f, 0.8f);
        place(sl, next);
    }

    /** Called every few ticks; also removes the old broken piston once. */
    static void tickAll(ServerLevel sl, CityData d) {
        if (sl.getGameTime() % 4 != 0) return;
        if (!d.doorsMigrated && sl.isLoaded(new BlockPos(3, 72, 12))) {
            d.doorsMigrated = true;
            d.setDirty();
            if (sl.getBlockState(new BlockPos(3, 72, 12)).getBlock().getDescriptionId().contains("mechanical_piston")) {
                sl.setBlock(new BlockPos(3, 72, 12), net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation("create", "andesite_casing")).defaultBlockState(), 3);
                FireheartCity.LOG.info("Factory door: replaced the broken piston contraption with an automatic sliding door");
            }
        }
        for (AutoDoor dr : DOORS) dr.tick(sl);
    }
}
