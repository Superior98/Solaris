package com.fireheart.city;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;

/** Residents hanging out at the same spot turn towards each other, so idle groups read as groups. */
public class MingleGoal extends Goal {
    private final Resident mob;
    private Resident other;
    private int ticks;

    public MingleGoal(Resident mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (mob.getRandom().nextInt(30) != 0 || !mob.idleHere()) return false;
        other = null;
        double best = 5.5 * 5.5;
        for (Resident o : mob.level().getEntitiesOfClass(Resident.class, mob.getBoundingBox().inflate(5.5, 2, 5.5), x -> x != mob && !x.inShuttle() && !x.isSleeping())) {
            double d = mob.distanceToSqr(o);
            boolean talking = o.isSpeaking() && mob.getRandom().nextBoolean();
            if ((d < best || talking) && mob.canSee(o, 5.5)) {
                best = talking ? 0 : d;
                other = o;
            }
        }
        return other != null;
    }

    @Override
    public void start() {
        ticks = 60 + mob.getRandom().nextInt(120);
    }

    @Override
    public boolean canContinueToUse() {
        return ticks > 0 && other != null && other.isAlive() && mob.convo == null && mob.distanceToSqr(other) < 7 * 7;
    }

    @Override
    public void tick() {
        ticks--;
        mob.getLookControl().setLookAt(other.getX(), other.getEyeY(), other.getZ(), 20.0F, 20.0F);
    }

    @Override
    public void stop() {
        other = null;
    }
}
