package com.fireheart.city;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Small navigation helpers shared by the resident goals. */
public final class Nav {
    private Nav() {}

    public static boolean walkable(BlockGetter level, BlockPos p) {
        return WalkNodeEvaluator.getBlockPathTypeStatic(level, p.mutable()) == BlockPathTypes.WALKABLE;
    }

    /** The walkable position at or one block above/below {@code c}, or null. */
    public static BlockPos standable(Level level, BlockPos c) {
        if (!level.isLoaded(c)) return null;
        if (walkable(level, c)) return c.immutable();
        if (walkable(level, c.above())) return c.above().immutable();
        if (walkable(level, c.below())) return c.below().immutable();
        return null;
    }

    /** True when nothing solid blocks the straight line between the two spots at knee and head height. */
    public static boolean clear(Level level, BlockPos from, BlockPos to, Entity who) {
        Vec3 a = Vec3.atBottomCenterOf(from), b = Vec3.atBottomCenterOf(to);
        return sees(level, a.add(0, 0.6, 0), b.add(0, 0.6, 0), who) && sees(level, a.add(0, 1.5, 0), b.add(0, 1.5, 0), who);
    }

    public static boolean sees(Level level, Vec3 from, Vec3 to, Entity who) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, who)).getType() == HitResult.Type.MISS;
    }
}
