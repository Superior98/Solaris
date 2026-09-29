package com.fireheart.city;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;

public class ConversationGoal extends Goal {
    private final Resident mob;

    public ConversationGoal(Resident mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return mob.convo != null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        Conversation c = mob.convo;
        if (c == null) return;
        Resident other = c.partnerOf(mob);
        mob.getLookControl().setLookAt(other, 30.0F, 30.0F);
        if ("bank".equals(c.kind())) {
            mob.getNavigation().stop();
            return;
        }
        double d = mob.distanceToSqr(other);
        if (d < 1.3D * 1.3D && mob.getNavigation().isDone()) {
            net.minecraft.world.phys.Vec3 away = mob.position().subtract(other.position());
            if (away.lengthSqr() < 1.0E-4) away = new net.minecraft.world.phys.Vec3(mob.getRandom().nextGaussian(), 0, mob.getRandom().nextGaussian());
            away = away.normalize();
            mob.getMoveControl().setWantedPosition(mob.getX() + away.x * 0.9, mob.getY(), mob.getZ() + away.z * 0.9, 0.45D);
        } else if (d > 6.0D) {
            if (mob.getNavigation().isDone() || mob.tickCount % 20 == 0) mob.getNavigation().moveTo(other, 0.8D);
        } else {
            mob.getNavigation().stop();
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }
}
