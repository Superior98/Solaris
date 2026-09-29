package com.fireheart.city;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Idle residents step aside for players walking into them, and for residents standing on top of them. */
public class MakeWayGoal extends Goal {
    private final Resident mob;
    private Vec3 to;
    private int ticks;

    public MakeWayGoal(Resident mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    private boolean mayMove() {
        return mob.isFree() && !mob.isSeated() && !mob.isPassenger() && !mob.isSleeping() && !mob.sunbathing() && !mob.dancing() && !mob.usingPc() && !mob.workBusy()
                && !mob.fleeing() && !mob.seeking() && mob.emergencyTarget() == null && !Elevator.controls(mob) && !Elevator.isQueued(mob) && !Bank.onDuty(mob) && !Reception.busy(mob);
    }

    @Override
    public boolean canUse() {
        if ((mob.tickCount + mob.getId()) % 5 != 0 || !mayMove()) return false;
        Player pl = null;
        double best = 1.9 * 1.9;
        for (Player p : mob.level().players()) {
            if (p.isSpectator() || Math.abs(p.getY() - mob.getY()) > 1.5) continue;
            double dx = mob.getX() - p.getX(), dz = mob.getZ() - p.getZ(), d = dx * dx + dz * dz;
            if (d >= best) continue;
            double mx = p.getX() - p.xo, mz = p.getZ() - p.zo;
            boolean toward = mx * dx + mz * dz > 0.02;
            if (d < 1.1 * 1.1 || toward) {
                best = d;
                pl = p;
            }
        }
        if (pl != null) {
            Vec3 move = new Vec3(pl.getX() - pl.xo, 0, pl.getZ() - pl.zo);
            Vec3 rel = new Vec3(mob.getX() - pl.getX(), 0, mob.getZ() - pl.getZ());
            Vec3 side = move.lengthSqr() > 1.0E-4 ? new Vec3(-move.z, 0, move.x).normalize() : rel.normalize();
            if (side.dot(rel) < 0) side = side.scale(-1);
            if (pick(side, rel)) {
                mob.excuseMe(pl);
                return true;
            }
            return false;
        }
        if (!mob.getNavigation().isDone()) return false;
        for (Resident o : mob.level().getEntitiesOfClass(Resident.class, mob.getBoundingBox().inflate(0.4, 0.5, 0.4), x -> x != mob && x.getId() < mob.getId() && !x.isPassenger())) {
            Vec3 rel = new Vec3(mob.getX() - o.getX(), 0, mob.getZ() - o.getZ());
            if (rel.lengthSqr() < 1.0E-4) rel = new Vec3(mob.getRandom().nextGaussian(), 0, mob.getRandom().nextGaussian());
            return pick(rel.normalize(), rel);
        }
        return false;
    }

    private boolean pick(Vec3 side, Vec3 rel) {
        Vec3 back = rel.lengthSqr() > 1.0E-4 ? rel.normalize() : side;
        Vec3[] tries = {side.scale(1.8), side.add(back).normalize().scale(1.8), back.scale(1.8), side.scale(-1.8)};
        for (Vec3 t : tries) {
            BlockPos c = BlockPos.containing(mob.getX() + t.x, mob.getY(), mob.getZ() + t.z);
            BlockPos s = Nav.standable(mob.level(), c);
            if (s != null && Nav.clear(mob.level(), mob.blockPosition(), s, mob)) {
                to = Vec3.atBottomCenterOf(s);
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        ticks = 25;
        mob.getNavigation().moveTo(to.x, to.y, to.z, 0.9);
    }

    @Override
    public boolean canContinueToUse() {
        return ticks > 0 && !mob.getNavigation().isDone() && mob.convo == null;
    }

    @Override
    public void tick() {
        ticks--;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }
}
