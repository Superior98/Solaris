package com.fireheart.city;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

public class CommuteGoal extends Goal {
    private final Resident mob;
    private int recalc;
    private int stuckTicks;
    private double bestDist;
    private BlockPos lastTarget;

    public CommuteGoal(Resident mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    private double dist(BlockPos t) {
        return mob.distanceToSqr(Vec3.atBottomCenterOf(t));
    }

    @Override
    public boolean canUse() {
        if (mob.emergencyTarget() != null) return true;
        if (mob.convo != null || mob.inShuttle() || mob.isEating() || Elevator.controls(mob) || mob.sunbathing() || mob.dancing() || mob.listening()) return false;
        BlockPos t = mob.navTarget();
        return t != null && dist(t) > 9.0D;
    }

    @Override
    public boolean canContinueToUse() {
        if (mob.emergencyTarget() != null) return true;
        if (mob.convo != null || mob.inShuttle() || mob.isEating() || Elevator.controls(mob) || mob.listening()) return false;
        BlockPos t = mob.navTarget();
        return t != null && dist(t) > 2.0D;
    }

    @Override
    public void start() {
        recalc = 0;
        stuckTicks = 0;
        lastTarget = null;
        bestDist = Double.MAX_VALUE;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        BlockPos t = mob.navTarget();
        if (t == null) return;
        if (!t.equals(lastTarget)) {
            lastTarget = t;
            bestDist = dist(t);
            stuckTicks = 0;
            recalc = 0;
        }
        Vec3 dest = Vec3.atBottomCenterOf(t);
        double d = dist(t);
        if (d < bestDist - 1.0D) {
            bestDist = d;
            stuckTicks = 0;
        } else if (++stuckTicks > 400) {
            stuckTicks = 0;
            bestDist = d;
            mob.stuckRescue();
            return;
        }
        if (--recalc > 0 && !mob.getNavigation().isDone()) return;
        recalc = 40;
        double speed = mob.emergencyTarget() != null ? 1.6D : mob.activityName().equals("leisure") ? 0.8D : 1.0D;
        if (mob.level().isRaining() && mob.level().canSeeSky(mob.blockPosition())) speed *= 1.2D;
        if (d > 36.0D * 36.0D) {
            Vec3 step = DefaultRandomPos.getPosTowards(mob, 24, 8, dest, Math.PI / 3.0D);
            if (step != null && mob.getNavigation().moveTo(step.x, step.y, step.z, speed)) return;
        }
        mob.getNavigation().moveTo(dest.x, dest.y, dest.z, speed);
    }
}
