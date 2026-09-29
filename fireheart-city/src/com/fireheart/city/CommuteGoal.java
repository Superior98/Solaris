package com.fireheart.city;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class CommuteGoal extends Goal {
    private static final int JUMP = 60, DETOUR = 140, ALTERNATE = 260, RESCUE = 400;
    private final Resident mob;
    private int recalc;
    private int stuckTicks;
    private int detourTicks;
    private int stage;
    private double bestDist;
    private double speed;
    private BlockPos lastTarget;
    private BlockPos alt;

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
        if (mob.convo != null || mob.inShuttle() || mob.isEating() || Elevator.controls(mob) || mob.sunbathing() || mob.dancing() || mob.listening() || mob.jogging()) return false;
        BlockPos t = mob.navTarget();
        return t != null && dist(t) > 9.0D;
    }

    @Override
    public boolean canContinueToUse() {
        if (mob.emergencyTarget() != null) return true;
        if (mob.convo != null || mob.inShuttle() || mob.isEating() || Elevator.controls(mob) || mob.listening() || mob.jogging()) return false;
        BlockPos t = mob.navTarget();
        return t != null && dist(t) > 2.0D;
    }

    @Override
    public void start() {
        recalc = 0;
        resetStuck();
        lastTarget = null;
        bestDist = Double.MAX_VALUE;
    }

    private void resetStuck() {
        stuckTicks = 0;
        stage = 0;
        detourTicks = 0;
        alt = null;
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
            resetStuck();
            recalc = 0;
        }
        double d = dist(t);
        if (d < bestDist - 1.0D) {
            bestDist = d;
            resetStuck();
        } else {
            stuckTicks++;
        }
        if (mob.tickCount % 10 == 0) pace(t, d);
        if (unstick(t, d)) return;
        if (detourTicks > 0) {
            detourTicks--;
            if (!mob.getNavigation().isDone()) return;
            detourTicks = 0;
            recalc = 0;
        }
        if (--recalc > 0 && !mob.getNavigation().isDone()) return;
        recalc = 40;
        BlockPos goal = alt != null ? alt : t;
        Vec3 dest = Vec3.atBottomCenterOf(goal);
        speed = baseSpeed();
        if (d > 36.0D * 36.0D) {
            Vec3 step = DefaultRandomPos.getPosTowards(mob, 24, 8, dest, Math.PI / 3.0D);
            if (step != null && mob.getNavigation().moveTo(step.x, step.y, step.z, speed)) return;
        }
        mob.getNavigation().moveTo(dest.x, dest.y, dest.z, speed);
    }

    private double baseSpeed() {
        if (mob.emergencyTarget() != null) return 1.6D;
        double s = (mob.activityName().equals("leisure") ? 0.8D : 1.0D) * mob.gait();
        if (mob.level().isRaining() && mob.level().canSeeSky(mob.blockPosition())) s *= 1.2D;
        return s;
    }

    /** Matches pace with a partner or friend heading to the same place, so they end up walking side by side. */
    private void pace(BlockPos t, double d) {
        if (mob.getNavigation().isDone() || mob.emergencyTarget() != null) return;
        double s = baseSpeed();
        Resident c = mob.companion();
        if (c != null) {
            double mine = Math.sqrt(d), theirs = Math.sqrt(c.distanceToSqr(Vec3.atBottomCenterOf(t)));
            if (theirs - mine > 2.5D) s *= 0.6D;
            else if (mine - theirs > 2.5D) s *= 1.15D;
            if (mob.distanceToSqr(c) < 4.0D && mob.tickCount % 40 == 0) mob.getLookControl().setLookAt(c, 20.0F, 20.0F);
        }
        if (Math.abs(s - speed) > 0.01D) {
            speed = s;
            mob.getNavigation().setSpeedModifier(s);
        }
    }

    /** Escalates from a hop, to a sidestep detour, to an alternate spot near the goal, to an unseen teleport. */
    private boolean unstick(BlockPos t, double d) {
        if (stuckTicks >= JUMP && stage < 1) {
            stage = 1;
            if (mob.onGround() && (mob.horizontalCollision || mob.getNavigation().isStuck())) mob.getJumpControl().jump();
            recalc = 0;
        }
        if (stuckTicks >= DETOUR && stage < 2) {
            stage = 2;
            Vec3 side = LandRandomPos.getPos(mob, 7, 3);
            if (side != null && mob.getNavigation().moveTo(side.x, side.y, side.z, speed > 0 ? speed : 1.0D)) {
                detourTicks = 50;
                return true;
            }
        }
        if (stuckTicks >= ALTERNATE && stage < 3) {
            stage = 3;
            Path p = mob.getNavigation().createPath(t, 1);
            if (p == null || !p.canReach()) {
                for (int i = 0; i < 10 && alt == null; i++) {
                    BlockPos c = t.offset(mob.getRandom().nextInt(7) - 3, 0, mob.getRandom().nextInt(7) - 3);
                    BlockPos s = Nav.standable(mob.level(), c);
                    if (s == null) continue;
                    Path q = mob.getNavigation().createPath(s, 0);
                    if (q != null && q.canReach()) alt = s;
                }
            }
            recalc = 0;
        }
        if (stuckTicks >= RESCUE) {
            if (mob.stuckRescue()) {
                bestDist = dist(t);
                resetStuck();
                return true;
            }
            stuckTicks = ALTERNATE;
            stage = 2;
            alt = null;
        }
        return false;
    }
}
