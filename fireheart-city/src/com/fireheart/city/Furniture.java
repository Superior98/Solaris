package com.fireheart.city;

import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

public final class Furniture {
    private static final String[] SEAT_CLASSES = {
            "com.simibubi.create.content.contraptions.actors.seat.SeatBlock",
            "com.starfish_studios.another_furniture.block.SeatBlock"
    };
    private static final java.util.List<Seat> SEATS = new java.util.ArrayList<>();
    private static boolean seatLookupDone;

    private record Seat(Class<?> cls, Method sit, Method occupied, Method sittable) {}

    private Furniture() {}

    private static void lookup() {
        if (seatLookupDone) return;
        seatLookupDone = true;
        for (String name : SEAT_CLASSES) {
            try {
                Class<?> c = Class.forName(name);
                Method sit = c.getMethod("sitDown", Level.class, BlockPos.class, Entity.class);
                Method occ = c.getMethod("isSeatOccupied", Level.class, BlockPos.class);
                Method sittable = null;
                try {
                    sittable = c.getMethod("isSittable", BlockState.class);
                } catch (NoSuchMethodException ignored) {
                }
                SEATS.add(new Seat(c, sit, occ, sittable));
            } catch (Throwable t) {
                FireheartCity.LOG.info("Seat type not available: " + name);
            }
        }
    }

    private static Seat seatFor(BlockState s) {
        lookup();
        for (Seat seat : SEATS) {
            if (!seat.cls().isInstance(s.getBlock())) continue;
            try {
                if (seat.sittable() != null && !(Boolean) seat.sittable().invoke(s.getBlock(), s)) return null;
            } catch (Throwable t) {
                return null;
            }
            return seat;
        }
        return null;
    }

    public static boolean isSeat(BlockState s) {
        return seatFor(s) != null;
    }

    public static boolean seatFree(ServerLevel lvl, BlockPos pos) {
        Seat seat = seatFor(lvl.getBlockState(pos));
        if (seat == null) return false;
        try {
            return !(Boolean) seat.occupied().invoke(null, lvl, pos);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean sit(ServerLevel lvl, BlockPos pos, Entity e) {
        Seat seat = seatFor(lvl.getBlockState(pos));
        if (seat == null || !seatFree(lvl, pos)) return false;
        try {
            seat.sit().invoke(null, lvl, pos, e);
            return e.isPassenger();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean onSeat(Entity e) {
        Entity v = e.getVehicle();
        return v != null && v.getClass().getSimpleName().equals("SeatEntity");
    }

    public static BlockPos findSeat(ServerLevel lvl, BlockPos center, int r, BlockPos near, double maxFromNear) {
        lookup();
        if (SEATS.isEmpty()) return null;
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -1, -r), center.offset(r, 1, r))) {
            if (!lvl.isLoaded(p) || !isSeat(lvl.getBlockState(p))) continue;
            if (!lvl.getBlockState(p.above()).isAir() || !lvl.getBlockState(p.above(2)).isAir()) continue;
            if (!seatFree(lvl, p)) continue;
            double d = p.distSqr(near);
            if (d > maxFromNear * maxFromNear) continue;
            if (d < bd) { bd = d; best = p.immutable(); }
        }
        return best;
    }

    public static boolean isFreeBedHead(ServerLevel lvl, BlockPos p) {
        BlockState s = lvl.getBlockState(p);
        return s.getBlock() instanceof BedBlock && s.getValue(BedBlock.PART) == BedPart.HEAD && !s.getValue(BedBlock.OCCUPIED);
    }

    public static BlockPos findBed(ServerLevel lvl, BlockPos home, int r) {
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(home.offset(-r, -1, -r), home.offset(r, 1, r))) {
            if (!lvl.isLoaded(p) || !isFreeBedHead(lvl, p)) continue;
            double d = p.distSqr(home);
            if (d < bd) { bd = d; best = p.immutable(); }
        }
        return best;
    }

    public static boolean hasBedNear(ServerLevel lvl, BlockPos home, int r) {
        for (BlockPos p : BlockPos.betweenClosed(home.offset(-r, -1, -r), home.offset(r, 1, r))) {
            BlockState s = lvl.getBlockState(p);
            if (s.getBlock() instanceof BedBlock && s.getValue(BedBlock.PART) == BedPart.HEAD) return true;
        }
        return false;
    }

    /** Places a red bed near home on free floor. Returns the head position or null. Only touches air blocks. */
    public static BlockPos placeBed(ServerLevel lvl, BlockPos home) {
        for (int ring = 1; ring <= 4; ring++) {
            for (BlockPos p : BlockPos.betweenClosed(home.offset(-ring, 0, -ring), home.offset(ring, 0, ring))) {
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockPos head = p.immutable(), foot = head.relative(d.getOpposite());
                    if (!placeable(lvl, head) || !placeable(lvl, foot)) continue;
                    BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, d);
                    lvl.setBlock(foot, bed.setValue(BedBlock.PART, BedPart.FOOT), Block.UPDATE_ALL);
                    lvl.setBlock(head, bed.setValue(BedBlock.PART, BedPart.HEAD), Block.UPDATE_ALL);
                    return head;
                }
            }
        }
        return null;
    }

    private static boolean placeable(ServerLevel lvl, BlockPos p) {
        return lvl.getBlockState(p).isAir() && lvl.getBlockState(p.above()).isAir()
                && lvl.getBlockState(p.below()).isFaceSturdy(lvl, p.below(), Direction.UP);
    }
}
