package com.fireheart.city;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Keeps the city's Create machines running. Every diesel engine is checked for fuel and rotation; a stalled engine
 * (it can run dry and then its own fuel pump stops) is restarted by re-placing it with a full tank. The factory worker
 * does this on their rounds; a quiet safety net covers the city when nobody is on shift.
 */
public final class Maintenance {
    private Maintenance() {}

    public record Machine(String name, BlockPos engine) {}

    public static final List<Machine> MACHINES = List.of(
            new Machine("the wheat farm harvester", new BlockPos(-5, 71, -2)),
            new Machine("the iron belt line", new BlockPos(0, 71, 3)),
            new Machine("the sheet press", new BlockPos(3, 73, 4)),
            new Machine("the factory door drive", new BlockPos(3, 72, 15)),
            new Machine("the Auto Bakery line", new BlockPos(-28, 71, 1)),
            new Machine("the aggregates crusher", new BlockPos(-38, 72, 54)),
            new Machine("the clock tower drive", new BlockPos(-26, 66, 17)));

    static final ResourceLocation ENGINE = new ResourceLocation("createdieselgenerators", "diesel_engine");

    static boolean isEngine(BlockState s) {
        return ENGINE.equals(ForgeRegistries.BLOCKS.getKey(s.getBlock()));
    }

    /** Returns null if fine, otherwise a short description of the fault. */
    public static String fault(ServerLevel sl, Machine m) {
        if (!sl.isLoaded(m.engine())) return null;
        BlockState s = sl.getBlockState(m.engine());
        if (!isEngine(s)) return null;
        BlockEntity be = sl.getBlockEntity(m.engine());
        if (be == null) return "no engine data";
        CompoundTag t = be.saveWithoutMetadata();
        int fuel = 0;
        ListTag tanks = t.getList("Tanks", 10);
        if (!tanks.isEmpty()) fuel = tanks.getCompound(0).getCompound("TankContent").getInt("Amount");
        float speed = Math.abs(t.getFloat("Speed"));
        if (fuel < 150) return "out of fuel";
        if (speed < 1) return "stalled";
        return null;
    }

    public static List<Machine> broken(ServerLevel sl) {
        List<Machine> out = new ArrayList<>();
        for (Machine m : MACHINES) if (fault(sl, m) != null) out.add(m);
        return out;
    }

    /** Restarts a stalled engine: re-place it with a full 1000 mB tank (the only reliable way to restart it). */
    public static boolean fix(ServerLevel sl, Machine m) {
        BlockPos p = m.engine();
        if (!sl.isLoaded(p)) return false;
        BlockState s = sl.getBlockState(p);
        if (!isEngine(s)) return false;
        sl.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        sl.setBlock(p, s, 3);
        BlockEntity be = sl.getBlockEntity(p);
        if (be != null) {
            CompoundTag t = be.saveWithoutMetadata();
            ListTag tanks = new ListTag();
            CompoundTag tank = new CompoundTag();
            CompoundTag content = new CompoundTag();
            content.putString("FluidName", "createdieselgenerators:diesel");
            content.putInt("Amount", 1000);
            tank.put("TankContent", content);
            tanks.add(tank);
            t.put("Tanks", tanks);
            be.load(t);
            be.setChanged();
        }
        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.01);
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX() + 0.5, p.getY() + 0.7, p.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.1);
        sl.playSound(null, p, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5f, 1.4f);
        FireheartCity.LOG.info("[Maint] restarted " + m.name() + " at " + p.toShortString());
        return true;
    }

    static long lastNet;

    /** Safety net: if something has been broken for a long time and no factory worker is on shift, fix it quietly. */
    static void tick(ServerLevel sl, CityData d) {
        long now = sl.getGameTime();
        if (now - lastNet < 6000) return;
        lastNet = now;
        boolean onShift = false;
        for (CityData.Profile p : d.profiles.values()) {
            if (p.job != Job.FACTORY_WORKER) continue;
            Resident r = Phones.entity(sl, p);
            if (r != null && r.activityName().equals("work")) onShift = true;
        }
        int fixed = 0;
        for (int cx = -8; cx <= 7; cx++) for (int cz = -7; cz <= 8; cz++) {
            var ch = sl.getChunkSource().getChunkNow(cx, cz);
            if (ch == null) continue;
            for (BlockPos p : new java.util.ArrayList<>(ch.getBlockEntities().keySet())) {
                if (!isEngine(sl.getBlockState(p))) continue;
                Machine m = new Machine("engine", p);
                boolean listed = false;
                for (Machine k : MACHINES) if (k.engine().equals(p)) listed = true;
                if (listed && onShift) continue;
                if (fault(sl, m) != null && fix(sl, m)) fixed++;
            }
        }
        if (fixed > 0) FireheartCity.LOG.info("[Maint] refreshed " + fixed + " diesel engines around the city");
    }

    /* ---------------------------------------------------------------- farm */

    public static final int FX0 = -10, FX1 = -6, FZ0 = 1, FZ1 = 4, FY = 71;

    /** Hand-harvests ripe wheat in the factory field and replants it. Returns the wheat gathered. */
    public static int harvest(ServerLevel sl) {
        int n = 0;
        for (int x = FX0; x <= FX1; x++) for (int z = FZ0; z <= FZ1; z++) {
            BlockPos p = new BlockPos(x, FY, z);
            BlockState s = sl.getBlockState(p);
            if (s.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop) {
                if (crop.isMaxAge(s)) {
                    sl.setBlock(p, crop.getStateForAge(0), 3);
                    sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, s), x + 0.5, FY + 0.3, z + 0.5, 6, 0.2, 0.2, 0.2, 0.05);
                    n++;
                }
            } else if (s.isAir() && sl.getBlockState(p.below()).is(Blocks.FARMLAND)) {
                sl.setBlock(p, Blocks.WHEAT.defaultBlockState(), 3);
            }
        }
        if (n > 0) sl.playSound(null, new BlockPos(-8, FY, 2), SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 0.8f, 1f);
        return n;
    }
}
