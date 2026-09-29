package com.fireheart.city;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

public class StayNearGoal extends Goal {
    private final Resident mob;
    private BlockPos spot;

    public StayNearGoal(Resident mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (mob.convo != null || mob.inShuttle() || mob.isEating() || Elevator.controls(mob) || mob.activityName().equals("sleep") || mob.workBusy() || mob.sunbathing() || mob.dancing() || mob.listening() || mob.playingMusic() || mob.jogging() || mob.onErrand()) return false;
        BlockPos t = mob.navTarget();
        if (t == null || mob.getRandom().nextInt(100) != 0) return false;
        boolean tower = Elevator.floorOfPos(t) >= 0;
        int r = tower ? 1 : mob.activityName().equals("leisure") ? 4 : 2;
        spot = t.offset(mob.getRandom().nextInt(2 * r + 1) - r, 0, mob.getRandom().nextInt(2 * r + 1) - r);
        if (tower && spot.getX() >= 33) return false;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return mob.convo == null && !Elevator.controls(mob) && !mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        mob.getNavigation().moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, 0.6D);
    }
}
