package com.fireheart.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

/**
 * Solaris Fire Dept: watches the city for real fire blocks, rings the station bell and sends the nearest firefighter,
 * who runs over and hoses it down block by block. /city firedrill lights a safe practice fire in the street.
 */
public final class FireDept {
    private FireDept() {}

    static final BlockPos BELL = new BlockPos(79, 84, -43);
    static final BlockPos DRILL = new BlockPos(64, 71, -29);
    static final int RANGE = 192;

    static final class Call {
        BlockPos at;
        long since;
        boolean announced;
        int putOut;
        double bestDist = 1e9;
        long improvedAt;
        int mode;
        boolean spraying;
        final List<BlockPos> ladder = new ArrayList<>();
        long bombAt = -1;
        Vec3 bombFrom, bombTo;
    }

    static final int WALK = 0, CLIMB = 1, DOWN = 2;

    private static final Map<UUID, Call> CALLS = new HashMap<>();
    private static final List<BlockPos> FIRES = new ArrayList<>();
    private static long lastAlarm;
    private static boolean drill;
    private static net.minecraft.world.level.block.state.BlockState drillFloor;

    public static void reset() {
        CALLS.clear();
        FIRES.clear();
        drill = false;
    }

    public static boolean isFirefighter(CityData.Profile p) {
        return p != null && p.job == Job.FIREFIGHTER;
    }

    public static BlockPos target(Resident r) {
        Call c = CALLS.get(r.getUUID());
        if (c == null || c.mode != WALK || c.spraying) return null;
        BlockPos f = nearestFire(c.at, 10);
        BlockPos t = f != null ? f : c.at;
        return r.distanceToSqr(t.getX() + 0.5, t.getY(), t.getZ() + 0.5) > 25 ? t : null;
    }

    public static boolean busy(Resident r) {
        return CALLS.containsKey(r.getUUID());
    }

    static void scan(ServerLevel sl) {
        FIRES.clear();
        for (int cx = -RANGE >> 4; cx <= RANGE >> 4; cx++) {
            for (int cz = -RANGE >> 4; cz <= RANGE >> 4; cz++) {
                LevelChunk ch = sl.getChunkSource().getChunkNow(cx, cz);
                if (ch == null) continue;
                LevelChunkSection[] secs = ch.getSections();
                for (int i = 0; i < secs.length; i++) {
                    LevelChunkSection s = secs[i];
                    if (s == null || s.hasOnlyAir() || !s.maybeHas(st -> st.is(BlockTags.FIRE))) continue;
                    int by = ch.getMinBuildHeight() + i * 16;
                    if (by < 50 || by > 200) continue;
                    for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                        if (s.getBlockState(x, y, z).is(BlockTags.FIRE)) {
                            FIRES.add(new BlockPos(cx * 16 + x, by + y, cz * 16 + z));
                            if (FIRES.size() > 200) return;
                        }
                    }
                }
            }
        }
    }

    public static void tick(ServerLevel sl, CityData d) {
        long now = sl.getGameTime();
        if (now % 40 == 17) {
            scan(sl);
            dispatch(sl, d, now);
        }
    }

    static BlockPos nearestFire(BlockPos from, double max) {
        BlockPos best = null;
        double bd = max * max;
        for (BlockPos f : FIRES) {
            double dd = f.distSqr(from);
            if (dd < bd) { bd = dd; best = f; }
        }
        return best;
    }

    static void dispatch(ServerLevel sl, CityData d, long now) {
        if (FIRES.isEmpty()) return;
        List<Resident> crew = new ArrayList<>();
        for (CityData.Profile p : d.profiles.values()) {
            if (!isFirefighter(p) || p.entity == null) continue;
            if (sl.getEntity(p.entity) instanceof Resident r && !r.isSleeping() && r.skyPhase() == 0 && !r.isPassenger()) crew.add(r);
        }
        if (crew.isEmpty()) {
            for (CityData.Profile p : d.profiles.values()) if (isFirefighter(p) && p.entity != null && sl.getEntity(p.entity) instanceof Resident r && r.isSleeping()) {
                r.stopSleeping();
                r.sayTo("Huh?! Fire? I'm up, I'm up!", 50);
                crew.add(r);
            }
        }
        for (BlockPos f : FIRES) {
            boolean covered = false;
            for (Call c : CALLS.values()) if (c.at.distSqr(f) < 12 * 12) covered = true;
            if (covered) continue;
            Resident best = null;
            double bd = Double.MAX_VALUE;
            for (Resident r : crew) {
                if (CALLS.containsKey(r.getUUID())) continue;
                double dd = r.distanceToSqr(f.getX(), f.getY(), f.getZ());
                if (dd < bd) { bd = dd; best = r; }
            }
            if (best == null) break;
            Call c = new Call();
            c.at = f;
            c.since = now;
            CALLS.put(best.getUUID(), c);
            if (best.convo != null) best.leaveConversation("Fire call - gotta go!");
            if (now - lastAlarm > 400) {
                lastAlarm = now;
                for (int i = 0; i < 3; i++) sl.playSound(null, BELL, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 3f, 1f);
                String where = nearPlace(f);
                for (ServerPlayer pl : sl.players()) if (pl.distanceToSqr(f.getX(), f.getY(), f.getZ()) < 250 * 250)
                    pl.displayClientMessage(Component.literal("§c🔥 Solaris Fire Dept is responding to a fire near " + where + "!"), true);
            }
        }
    }

    static String nearPlace(BlockPos f) {
        Place best = null;
        double bd = Double.MAX_VALUE;
        for (Place p : Place.ALL.values()) {
            double dd = p.pos.distSqr(f);
            if (dd < bd) { bd = dd; best = p; }
        }
        return best == null ? "town" : best.label;
    }

    /** Runs the firefighter while on a call. Returns true while it has control. */
    public static boolean tick(Resident r, CityData.Profile p) {
        Call c = CALLS.get(r.getUUID());
        if (c == null) return false;
        ServerLevel sl = (ServerLevel) r.level();
        long now = sl.getGameTime();
        RandomSource rnd = r.getRandom();
        if (c.mode == DOWN) return teardown(r, p, c, sl, now);
        BlockPos fire = nearestFire(c.at, 12);
        if (fire == null || !sl.getBlockState(fire).is(BlockTags.FIRE)) {
            fire = null;
            for (int i = 0; i < 20 && fire == null; i++) {
                BlockPos q = c.at.offset(rnd.nextInt(13) - 6, rnd.nextInt(5) - 2, rnd.nextInt(13) - 6);
                if (sl.getBlockState(q).is(BlockTags.FIRE)) fire = q;
            }
        }
        if (c.bombAt > 0) bombTick(sl, c, now);
        if (fire == null && now - c.since > 60 && c.bombAt < 0) {
            if (!c.ladder.isEmpty()) { c.mode = DOWN; return true; }
            done(r, p, c, sl);
            return false;
        }
        if (now - c.since > 20 * 240) {
            if (!c.ladder.isEmpty()) { c.mode = DOWN; return true; }
            CALLS.remove(r.getUUID());
            return false;
        }
        if (!c.announced) {
            c.announced = true;
            r.sayTo(Lines.pick(rnd, "Fire! Stand back, everyone!", "Solaris Fire Dept, coming through!", "On my way - keep clear!"), 50);
        }
        if (fire == null) return true;
        double dist = Math.sqrt(r.distanceToSqr(fire.getX() + 0.5, fire.getY(), fire.getZ() + 0.5));
        r.getLookControl().setLookAt(fire.getX() + 0.5, fire.getY() + 0.5, fire.getZ() + 0.5);
        boolean los = sees(sl, r, fire);
        c.spraying = false;
        if (dist < c.bestDist - 0.8) { c.bestDist = dist; c.improvedAt = now; }
        if (c.mode == CLIMB) {
            if ((los && dist <= 14) || r.getY() >= fire.getY() + 1 || c.ladder.size() > 40) {
                if (!(los && dist <= 16)) {
                    if (c.bombAt < 0 && dist < 26) throwBomb(sl, r, c, fire, now);
                    return true;
                }
            } else {
                climb(sl, r, c, now);
                return true;
            }
        } else if (!(los && dist <= (c.ladder.isEmpty() ? 12 : 16))) {
            boolean stuck = now - c.improvedAt > 100 || (r.getNavigation().isDone() && dist > 5);
            double horiz = Math.sqrt(Math.pow(r.getX() - fire.getX() - 0.5, 2) + Math.pow(r.getZ() - fire.getZ() - 0.5, 2));
            if (stuck && fire.getY() > r.getY() + 2 && horiz < 12) {
                c.mode = CLIMB;
                r.getNavigation().stop();
                r.sayTo(Lines.pick(rnd, "It's up high - deploying the ladder!", "Can't reach it from here. Going up!", "Aerial ladder, coming up!"), 50);
                return true;
            }
            if (stuck && c.bombAt < 0 && dist < 26) {
                throwBomb(sl, r, c, fire, now);
                return true;
            }
            if (now % 10 == 0) r.getNavigation().moveTo(fire.getX() + 0.5, fire.getY(), fire.getZ() + 0.5, 1.6);
            return true;
        }
        r.getNavigation().stop();
        c.spraying = true;
        if (now % 20 == 0) r.gesture(Resident.G_HOSE, 22);
        Vec3 from = r.getEyePosition().add(r.getLookAngle().scale(0.6)).add(0, -0.4, 0);
        Vec3 to = Vec3.atCenterOf(fire);
        int steps = Math.max(10, (int) (dist * 2.5));
        double arc = Math.min(2.5, 0.5 + dist * 0.12);
        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 q = from.lerp(to, t).add(0, Math.sin(t * Math.PI) * arc, 0);
            sl.sendParticles(ParticleTypes.SPLASH, q.x, q.y, q.z, 3, 0.06, 0.06, 0.06, 0.1);
            if (i % 3 == 0) sl.sendParticles(ParticleTypes.FALLING_WATER, q.x, q.y, q.z, 1, 0.05, 0.05, 0.05, 0);
            if (i % 5 == 0) sl.sendParticles(ParticleTypes.BUBBLE_POP, q.x, q.y, q.z, 1, 0.05, 0.05, 0.05, 0.02);
        }
        if (now % 6 == 0) sl.playSound(null, r.blockPosition(), SoundEvents.WEATHER_RAIN, SoundSource.NEUTRAL, 0.6f, 1.6f);
        if (now % 4 == 0) {
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos q = fire.offset(dx, dy, dz);
                if (!sl.getBlockState(q).is(BlockTags.FIRE)) continue;
                sl.setBlock(q, Blocks.AIR.defaultBlockState(), 3);
                c.putOut++;
                sl.sendParticles(ParticleTypes.CLOUD, q.getX() + 0.5, q.getY() + 0.3, q.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.03);
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, q.getX() + 0.5, q.getY() + 0.3, q.getZ() + 0.5, 3, 0.2, 0.3, 0.2, 0.02);
                sl.playSound(null, q, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7f, 1f);
                FIRES.remove(q);
            }
        }
        return true;
    }

    static boolean sees(ServerLevel sl, Resident r, BlockPos fire) {
        Vec3 a = r.getEyePosition(), b = Vec3.atCenterOf(fire);
        var hit = sl.clip(new net.minecraft.world.level.ClipContext(a, b, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, r));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS || hit.getBlockPos().closerThan(fire, 1.8);
    }

    static void climb(ServerLevel sl, Resident r, Call c, long now) {
        r.getNavigation().stop();
        if (now % 6 != 0) return;
        BlockPos at = r.blockPosition();
        BlockPos head = at.above(2);
        if (!sl.getBlockState(head).isAir() && !sl.getBlockState(head).canBeReplaced()) {
            c.mode = WALK;
            c.improvedAt = now;
            return;
        }
        r.gesture(Resident.G_HAMMER, 8);
        r.setDeltaMovement(0, 0, 0);
        r.teleportTo(at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5);
        if (sl.getBlockState(at).isAir() || sl.getBlockState(at).canBeReplaced()) {
            sl.setBlock(at, Blocks.SCAFFOLDING.defaultBlockState(), 3);
            c.ladder.add(at);
        }
        sl.playSound(null, at, SoundEvents.SCAFFOLDING_PLACE, SoundSource.BLOCKS, 1f, 1f);
        sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, Blocks.SCAFFOLDING.defaultBlockState()), at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.1);
    }

    static boolean teardown(Resident r, CityData.Profile p, Call c, ServerLevel sl, long now) {
        r.getNavigation().stop();
        if (now % 4 != 0) return true;
        if (c.ladder.isEmpty()) {
            done(r, p, c, sl);
            return false;
        }
        BlockPos top = c.ladder.remove(c.ladder.size() - 1);
        if (sl.getBlockState(top).is(Blocks.SCAFFOLDING)) {
            sl.setBlock(top, Blocks.AIR.defaultBlockState(), 3);
            sl.playSound(null, top, SoundEvents.SCAFFOLDING_BREAK, SoundSource.BLOCKS, 0.8f, 1.1f);
        }
        r.teleportTo(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
        r.gesture(Resident.G_HAMMER, 6);
        return true;
    }

    static void throwBomb(ServerLevel sl, Resident r, Call c, BlockPos fire, long now) {
        c.bombAt = now;
        c.bombFrom = r.getEyePosition();
        c.bombTo = Vec3.atCenterOf(fire);
        r.gesture(Resident.G_UPPERCUT, 12);
        r.sayTo(Lines.pick(r.getRandom(), "Water bomb - incoming!", "Can't get close - catch!", "Fire suppression grenade!"), 40);
        sl.playSound(null, r.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 1.2f, 0.6f);
    }

    static void bombTick(ServerLevel sl, Call c, long now) {
        int flight = (int) Math.max(12, c.bombFrom.distanceTo(c.bombTo) * 1.3);
        double t = (now - c.bombAt) / (double) flight;
        if (t < 1) {
            Vec3 q = c.bombFrom.lerp(c.bombTo, t).add(0, Math.sin(t * Math.PI) * (2 + c.bombFrom.distanceTo(c.bombTo) * 0.25), 0);
            sl.sendParticles(ParticleTypes.DRIPPING_WATER, q.x, q.y, q.z, 3, 0.1, 0.1, 0.1, 0);
            sl.sendParticles(ParticleTypes.BUBBLE_POP, q.x, q.y, q.z, 2, 0.08, 0.08, 0.08, 0);
            return;
        }
        c.bombAt = -1;
        BlockPos at = BlockPos.containing(c.bombTo);
        sl.sendParticles(ParticleTypes.SPLASH, c.bombTo.x, c.bombTo.y, c.bombTo.z, 120, 1.6, 1, 1.6, 0.4);
        sl.sendParticles(ParticleTypes.CLOUD, c.bombTo.x, c.bombTo.y, c.bombTo.z, 30, 1.5, 0.8, 1.5, 0.05);
        sl.playSound(null, at, SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 1.6f, 0.8f);
        sl.playSound(null, at, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1f, 0.9f);
        for (int dx = -3; dx <= 3; dx++) for (int dy = -2; dy <= 2; dy++) for (int dz = -3; dz <= 3; dz++) {
            BlockPos q = at.offset(dx, dy, dz);
            if (!sl.getBlockState(q).is(BlockTags.FIRE)) continue;
            sl.setBlock(q, Blocks.AIR.defaultBlockState(), 3);
            c.putOut++;
            FIRES.remove(q);
        }
    }

    static void done(Resident r, CityData.Profile p, Call c, ServerLevel sl) {
        CALLS.remove(r.getUUID());
        if (c.putOut == 0) return;
        RandomSource rnd = r.getRandom();
        r.gesture(Resident.G_THUMBS, 40);
        r.sayTo(Lines.pick(rnd, "Fire's out! Everyone okay?", "All clear - that one's done.", "Nothing but steam now."), 60);
        p.log(r.routineDay()).note("I put out a fire near " + nearPlace(c.at));
        CityData d = r.data();
        d.event(r.day(), "fire", p.name + " put out a fire near " + nearPlace(c.at), c.at, p.id);
        for (Resident o : sl.getEntitiesOfClass(Resident.class, r.getBoundingBox().inflate(16), x -> x != r && !x.isSleeping() && x.isFree())) {
            if (rnd.nextInt(2) != 0) continue;
            o.getLookControl().setLookAt(r, 30, 30);
            o.gesture(Resident.G_CLAP, 40);
            o.say(Lines.pick(rnd, "Thank you, " + p.name + "!", "Our hero!", "Phew, that was close!"), 50);
        }
        if (drill && c.at.closerThan(DRILL, 4)) {
            drill = false;
            sl.setBlock(DRILL, Blocks.AIR.defaultBlockState(), 3);
            if (drillFloor != null) sl.setBlock(DRILL.below(), drillFloor, 3);
        }
        d.setDirty();
    }

    public static String fireDrill(ServerLevel sl) {
        if (sl.getChunkSource().getChunkNow(DRILL.getX() >> 4, DRILL.getZ() >> 4) == null) return "§cThe drill spot (" + DRILL.toShortString() + ") isn't loaded - go to Solaris East.";
        if (!drill) drillFloor = sl.getBlockState(DRILL.below());
        sl.setBlock(DRILL.below(), Blocks.NETHERRACK.defaultBlockState(), 3);
        sl.setBlock(DRILL, Blocks.FIRE.defaultBlockState(), 3);
        drill = true;
        return "§6Fire drill! A practice fire is burning at " + DRILL.toShortString() + " - watch the fire crew respond.";
    }

    public static void buildBunks(ServerLevel sl, CityData d) {
        if (d.fireBunks || !d.expanded || sl.getChunkSource().getChunkNow(78 >> 4, -50 >> 4) == null) return;
        d.fireBunks = true;
        d.setDirty();
        Builder b = new Builder(sl);
        b.set(77, 71, -51, "minecraft:red_bed[facing=north,part=head]");
        b.set(77, 71, -50, "minecraft:red_bed[facing=north,part=foot]");
        b.set(79, 71, -51, "minecraft:red_bed[facing=north,part=head]");
        b.set(79, 71, -50, "minecraft:red_bed[facing=north,part=foot]");
        b.set(78, 71, -51, "minecraft:barrel[facing=up]");
        FireheartCity.LOG.info("Added firefighter bunks to the fire station");
    }
}
