package com.fireheart.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Solaris PD: officers hunt down hostile mobs near people and fight them with a moveset - jab combos, dash strikes,
 * rising uppercut into an air spike, 360 spin kicks, ground-slam shockwaves and a taser. Every hit tells nearby
 * clients to play an impact animation on the mob (see client.HitAnim).
 */
public final class Police {
    private Police() {}

    static final class Fight {
        UUID target;
        int move;
        int moveT;
        int combo;
        long lastSeen;
        int dashCd, taserCd, slamCd, kickCd, upperCd, freeze;
        Vec3 pendingKb;
        Mob pendingMob;
        String pendingKind;
        boolean announced;
        int wins;
        int shootCd, barrageCd, railCd, meteorCd, warpCd, ult, backupCd, burst;
        boolean backup;
        UUID lastTarget;
    }

    static final int NONE = 0, COMBO = 1, DASH = 2, UPPER = 3, SPIKE = 4, KICK = 5, SLAM = 6, TASER = 7, PUNT = 8,
            SHOOT = 9, BARRAGE = 10, RAIL = 11, ULT_START = 12, METEOR = 13, WARP = 14, FINISHER = 15, SPRAY = 16;
    static final int ULT_TICKS = 420, ULT_COOLDOWN = 36000, REST_TICKS = 4800;
    private static final Map<UUID, Fight> FIGHTS = new HashMap<>();
    private static final Map<UUID, UUID> CLAIMED = new HashMap<>();
    private static final Map<UUID, Long> ULT_READY = new HashMap<>();
    private static final Map<UUID, Long> RESTING = new HashMap<>();
    static final SoundEvent SHOT = snd("police.shot"), RAILGUN = snd("police.rail"), ULT = snd("police.ult"), FINISH = snd("police.finisher"), RADIO = snd("police.radio");

    static SoundEvent snd(String id) {
        return SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation(FireheartCity.MODID, id));
    }

    public static void reset() {
        FIGHTS.clear();
        CLAIMED.clear();
        ULT_READY.clear();
        RESTING.clear();
    }

    public static boolean tank(Entity e) {
        return e instanceof net.minecraft.world.entity.monster.warden.Warden || e instanceof net.minecraft.world.entity.monster.Ravager
                || e instanceof net.minecraft.world.entity.boss.wither.WitherBoss || e instanceof net.minecraft.world.entity.monster.ElderGuardian
                || e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon || (e instanceof Mob m && m.getMaxHealth() >= 60);
    }

    static boolean flying(Entity e) {
        return e instanceof net.minecraft.world.entity.FlyingMob || e instanceof net.minecraft.world.entity.monster.Blaze || e instanceof net.minecraft.world.entity.monster.Vex
                || (e instanceof Mob m && !m.onGround() && m.getY() - m.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, m.getBlockX(), m.getBlockZ()) > 3);
    }

    public static boolean resting(Resident r) {
        Long t = RESTING.get(r.getUUID());
        return t != null && r.level().getGameTime() < t;
    }

    public static boolean inUlt(Resident r) {
        Fight f = FIGHTS.get(r.getUUID());
        return f != null && f.ult > 0;
    }

    static int copsOn(UUID mob) {
        int n = 0;
        for (UUID u : CLAIMED.values()) if (u.equals(mob)) n++;
        return n;
    }

    public static void buildBunks(ServerLevel sl, CityData d) {
        if (d.policeBunks || !d.expanded || sl.getChunkSource().getChunkNow(67 >> 4, -46 >> 4) == null) return;
        d.policeBunks = true;
        d.setDirty();
        Builder b = new Builder(sl);
        int x1 = 65, x2 = 69, z1 = -51, z2 = -42, g = 70;
        b.fill(x1, g, z1, x2, g, z2, "minecraft:polished_andesite");
        b.air(x1, g + 1, z1, x2, g + 4, z2);
        b.walls(x1, g + 1, z1, x2, g + 3, z2, "minecraft:blue_concrete");
        b.fill(x1, g + 4, z1, x2, g + 4, z2, "minecraft:smooth_stone");
        b.fill(x1 + 1, g + 1, z1 + 1, x2 - 1, g + 3, z2 - 1, "minecraft:air");
        b.set(67, g + 1, z2, "minecraft:oak_door[facing=south,half=lower,hinge=left]");
        b.set(67, g + 2, z2, "minecraft:oak_door[facing=south,half=upper,hinge=left]");
        b.set(66, g + 1, -50, "minecraft:blue_bed[facing=north,part=head]");
        b.set(66, g + 1, -49, "minecraft:blue_bed[facing=north,part=foot]");
        b.set(68, g + 1, -50, "minecraft:blue_bed[facing=north,part=head]");
        b.set(68, g + 1, -49, "minecraft:blue_bed[facing=north,part=foot]");
        b.set(66, g + 1, -44, "minecraft:barrel[facing=up]");
        b.set(68, g + 1, -44, "minecraft:potted_bamboo");
        b.set(67, g + 3, -47, "minecraft:lantern[hanging=true]");
        b.fill(x1, g + 2, -48, x1, g + 2, -46, "minecraft:glass_pane");
        b.fill(x2, g + 2, -48, x2, g + 2, -46, "minecraft:glass_pane");
        b.sign(67, g + 3, z2 + 1, "minecraft:oak_wall_sign[facing=south]", "§9§lSOLARIS PD", "§9Bunkhouse", "", "");
        FireheartCity.LOG.info("Built the police bunkhouse");
    }

    public static boolean isOfficer(CityData.Profile p) {
        return p != null && p.job == Job.POLICE;
    }

    public static BlockPos target(Resident r) {
        Fight f = FIGHTS.get(r.getUUID());
        if (f == null || f.target == null || f.move != NONE || !(r.level() instanceof ServerLevel sl)) return null;
        Entity e = sl.getEntity(f.target);
        return e == null || r.distanceTo(e) < 3 ? null : e.blockPosition();
    }

    public static boolean fighting(Resident r) {
        Fight f = FIGHTS.get(r.getUUID());
        return f != null && f.target != null;
    }

    static boolean awake(Resident r) {
        return !r.isSleeping() && r.skyPhase() == 0 && !r.isPassenger() && !Elevator.isRider(r) && !r.inShuttle();
    }

    static boolean hostile(Entity e) {
        return e instanceof Mob m && e instanceof Enemy && m.isAlive() && !m.isNoAi() && !(e instanceof net.minecraft.world.entity.monster.EnderMan em && em.getTarget() == null);
    }

    /** Called every 10 ticks: find mobs that threaten people and send the nearest free officer. */
    public static void dispatch(ServerLevel sl, CityData d) {
        List<Resident> cops = new ArrayList<>();
        List<Resident> sleepers = new ArrayList<>();
        for (CityData.Profile p : d.profiles.values()) {
            if (!isOfficer(p) || p.entity == null) continue;
            if (sl.getEntity(p.entity) instanceof Resident r) {
                if (awake(r) && !resting(r)) cops.add(r);
                else if (r.isSleeping()) sleepers.add(r);
            }
        }
        if (cops.isEmpty()) return;
        CLAIMED.values().removeIf(u -> !(sl.getEntity(u) instanceof Mob m) || !m.isAlive());
        List<Entity> people = new ArrayList<>(sl.players());
        for (Resident r : sl.getEntitiesOfClass(Resident.class, new AABB(-200, 40, -200, 200, 120, 200))) if (!isOfficer(r.profile())) people.add(r);
        for (Entity who : people) {
            for (Mob m : sl.getEntitiesOfClass(Mob.class, who.getBoundingBox().inflate(16, 6, 16), Police::hostile)) {
                if (CLAIMED.containsValue(m.getUUID()) && !(tank(m) && copsOn(m.getUUID()) < 3)) continue;
                Resident best = null;
                double bd = 110 * 110;
                for (Resident c : cops) {
                    if (fighting(c)) continue;
                    double dd = c.distanceToSqr(m);
                    if (dd < bd) { bd = dd; best = c; }
                }
                if (best == null) return;
                engage(best, m);
                if (tank(m)) callBackup(sl, best, m, cops, sleepers);
            }
        }
        for (Resident c : cops) {
            if (fighting(c)) continue;
            for (Mob m : sl.getEntitiesOfClass(Mob.class, c.getBoundingBox().inflate(14, 5, 14), Police::hostile)) {
                if (CLAIMED.containsValue(m.getUUID())) continue;
                engage(c, m);
                if (tank(m)) callBackup(sl, c, m, cops, sleepers);
                break;
            }
        }
    }

    static void callBackup(ServerLevel sl, Resident caller, Mob m, List<Resident> cops, List<Resident> sleepers) {
        Fight cf = FIGHTS.get(caller.getUUID());
        if (cf == null || cf.backup) return;
        cf.backup = true;
        String mob = m.getType().getDescription().getString();
        CityData.Profile cp = caller.profile();
        caller.sayTo(Lines.pick(sl.random, "Dispatch, this is " + (cp == null ? "unit 1" : cp.name) + " - I've got a " + mob + "! Requesting ALL units!", "10-78! Big one near " + Dialogue.here(caller) + " - send everyone!", "Code red! " + mob + " in the city - backup, NOW!"), 70);
        caller.gesture(Resident.G_CALL, 30);
        sl.playSound(null, caller.blockPosition(), RADIO, SoundSource.NEUTRAL, 1.5f, 1f);
        for (Resident s : sleepers) {
            if (s.distanceTo(m) > 220) continue;
            s.stopSleeping();
        }
        for (Resident c : sl.getEntitiesOfClass(Resident.class, caller.getBoundingBox().inflate(220, 80, 220), r -> isOfficer(r.profile()) && r != caller && !fighting(r))) {
            if (c.isSleeping()) c.stopSleeping();
            engage(c, m);
            Fight f = FIGHTS.get(c.getUUID());
            f.backup = true;
            c.sayTo(Lines.pick(sl.random, "On my way!", "Copy that - en route!", "Hang on, I'm coming!", "Backup inbound!"), 40);
            sl.playSound(null, c.blockPosition(), RADIO, SoundSource.NEUTRAL, 1f, 1.1f);
        }
        for (ServerPlayer pl : sl.players()) {
            if (pl.distanceTo(m) > 120) continue;
            pl.displayClientMessage(net.minecraft.network.chat.Component.literal("§9§l[SOLARIS PD] §bAll units respond - " + mob + " near " + Dialogue.here(caller) + "!"), true);
        }
        d(sl).event(caller.day(), "police", "Solaris PD called every unit to fight a " + mob.toLowerCase(java.util.Locale.ROOT) + " near " + Dialogue.here(caller), caller.blockPosition());
    }

    static CityData d(ServerLevel sl) {
        return CityData.get(sl);
    }

    static void engage(Resident cop, Mob m) {
        Fight f = FIGHTS.computeIfAbsent(cop.getUUID(), k -> new Fight());
        f.target = m.getUUID();
        f.move = NONE;
        f.moveT = 0;
        f.announced = false;
        CLAIMED.put(cop.getUUID(), m.getUUID());
        if (cop.convo != null) cop.leaveConversation("Duty calls!");
    }

    /** Returns true while the officer is in a fight (normal routine is suspended). */
    public static boolean tick(Resident cop, CityData.Profile p) {
        Fight f = FIGHTS.get(cop.getUUID());
        if (f == null || f.target == null) return false;
        ServerLevel sl = (ServerLevel) cop.level();
        Entity te = sl.getEntity(f.target);
        if (f.freeze > 0) {
            f.freeze--;
            cop.setDeltaMovement(Vec3.ZERO);
            if (f.pendingMob != null && f.pendingMob.isAlive()) f.pendingMob.setDeltaMovement(Vec3.ZERO);
            if (f.freeze == 0 && f.pendingMob != null) {
                launch(f.pendingMob, f.pendingKb, f.pendingKind, cop);
                f.pendingMob = null;
            }
            return true;
        }
        if (!(te instanceof Mob m) || !m.isAlive() || cop.distanceTo(m) > 60 || !awake(cop)) {
            boolean won = te instanceof Mob mm && !mm.isAlive();
            finish(cop, p, f, won, te);
            return false;
        }
        RandomSource r = cop.getRandom();
        cop.getLookControl().setLookAt(m, 60, 60);
        double dist = cop.distanceTo(m);
        tickCooldowns(f);
        if (f.ult > 0) ultTick(cop, p, f, sl);
        if (!f.announced) {
            f.announced = true;
            if (!f.backup) cop.sayTo(line(p, "engage", m, r), 50);
            sl.playSound(null, cop.blockPosition(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.NEUTRAL, 1f, 1.8f);
        }
        if (f.move == NONE && f.ult <= 0 && cop.tickCount % 20 == 0 && ultReady(cop) && r.nextFloat() < ultChance(sl, cop, m)) start(f, ULT_START);
        if (f.move == NONE && (f.ult > 0 || tank(m)) && finishable(m, f)) start(f, FINISHER);
        if (f.move == NONE && f.ult > 0) {
            if (f.warpCd <= 0 && dist > 2.5) start(f, WARP);
            else if (f.meteorCd <= 0 && (tank(m) || crowd(sl, cop) >= 2)) start(f, METEOR);
            else if (f.railCd <= 0 && dist > 4 && cop.hasLineOfSight(m)) start(f, RAIL);
            else if (f.barrageCd <= 0 && dist <= 3) start(f, BARRAGE);
        }
        if (f.move == NONE && tank(m)) {
            if (f.railCd <= 0 && dist > 5 && dist < 30 && cop.hasLineOfSight(m) && r.nextInt(3) == 0) start(f, RAIL);
            else if (f.barrageCd <= 0 && dist <= 3) start(f, BARRAGE);
        }
        if (f.move == NONE) {
            boolean creeper = m instanceof Creeper;
            int crowd = crowd(sl, cop);
            if (creeper && ((Creeper) m).getSwellDir() > 0 && dist < 3.2) start(f, PUNT);
            else if ((creeper || flying(m)) && f.shootCd <= 0 && dist < 28 && cop.hasLineOfSight(m)) start(f, SHOOT);
            else if (crowd >= 3 && f.shootCd <= 0 && dist > 3) start(f, SPRAY);
            else if (creeper && dist < 5) {
                Vec3 away = cop.position().add(flat(m, cop).scale(4));
                cop.getNavigation().moveTo(away.x, away.y, away.z, 1.5);
            } else if (flying(m) && !cop.hasLineOfSight(m)) cop.getNavigation().moveTo(m, 1.5);
            else if (dist > 11 && dist < 26 && f.shootCd <= 0 && cop.hasLineOfSight(m) && r.nextInt(2) == 0) start(f, SHOOT);
            else if (dist > 5 && dist < 11 && f.dashCd <= 0) start(f, DASH);
            else if (dist >= 6 && dist < 16 && f.taserCd <= 0 && cop.hasLineOfSight(m)) start(f, TASER);
            else if (dist <= 2.6) {
                int nearby = sl.getEntitiesOfClass(Mob.class, cop.getBoundingBox().inflate(4), Police::hostile).size();
                if (nearby >= 2 && f.kickCd <= 0) start(f, KICK);
                else if (f.slamCd <= 0 && (nearby >= 2 || r.nextInt(3) == 0)) start(f, SLAM);
                else if (f.upperCd <= 0 && f.combo >= 2) start(f, UPPER);
                else start(f, COMBO);
            } else {
                cop.getNavigation().moveTo(m, 1.55);
                if (cop.tickCount % 8 == 0) cop.gesture(Resident.G_GUARD, 10);
            }
        }
        if (f.move != NONE) runMove(cop, p, f, m, dist, r, sl);
        return true;
    }

    static void tickCooldowns(Fight f) {
        if (f.shootCd > 0) f.shootCd--;
        if (f.barrageCd > 0) f.barrageCd--;
        if (f.railCd > 0) f.railCd--;
        if (f.meteorCd > 0) f.meteorCd--;
        if (f.warpCd > 0) f.warpCd--;
        if (f.dashCd > 0) f.dashCd--;
        if (f.taserCd > 0) f.taserCd--;
        if (f.slamCd > 0) f.slamCd--;
        if (f.kickCd > 0) f.kickCd--;
        if (f.upperCd > 0) f.upperCd--;
    }

    static void start(Fight f, int move) {
        f.move = move;
        f.moveT = 0;
    }

    static Vec3 flat(Entity from, Entity to) {
        Vec3 v = new Vec3(to.getX() - from.getX(), 0, to.getZ() - from.getZ());
        return v.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : v.normalize();
    }

    static void runMove(Resident cop, CityData.Profile p, Fight f, Mob m, double dist, RandomSource r, ServerLevel sl) {
        int t = f.moveT++;
        Vec3 dir = flat(cop, m);
        switch (f.move) {
            case COMBO -> {
                cop.getNavigation().stop();
                if (t == 0 || t == 6 || t == 12) {
                    boolean right = (t / 6) % 2 == 0;
                    cop.gesture(right ? Resident.G_JAB_R : Resident.G_JAB_L, 5);
                    if (dist < 3.2) {
                        boolean last = t == 12;
                        hit(cop, m, last ? 5f : 3f, dir.scale(last ? 0.9 : 0.35).add(0, last ? 0.35 : 0.12, 0), last ? "heavy" : "jab", last ? 2 : 0, f);
                        if (last) f.combo++;
                    }
                }
                if (t >= 16) { f.move = NONE; }
            }
            case DASH -> {
                if (t == 0) {
                    cop.gesture(Resident.G_DASH, 14);
                    cop.getNavigation().stop();
                    cop.setDeltaMovement(dir.scale(1.45).add(0, 0.28, 0));
                    cop.hurtMarked = true;
                    sl.playSound(null, cop.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 1.2f, 0.6f);
                    if (r.nextInt(2) == 0) cop.sayTo(line(p, "dash", m, r), 30);
                }
                if (t < 10) {
                    sl.sendParticles(new DustParticleOptions(new Vector3f(0.2f, 0.45f, 1f), 1.4f), cop.getX(), cop.getY() + 1, cop.getZ(), 4, 0.2, 0.5, 0.2, 0);
                    sl.sendParticles(ParticleTypes.CLOUD, cop.getX(), cop.getY() + 0.1, cop.getZ(), 1, 0.1, 0, 0.1, 0.01);
                    if (cop.distanceTo(m) < 2.4) {
                        hit(cop, m, 6f, dir.scale(1.6).add(0, 0.45, 0), "heavy", 3, f);
                        f.move = NONE;
                        f.dashCd = 60;
                        f.combo++;
                    }
                }
                if (t >= 12) { f.move = NONE; f.dashCd = 60; }
            }
            case TASER -> {
                cop.getNavigation().stop();
                if (t == 0) {
                    cop.gesture(Resident.G_TASER, 22);
                    cop.sayTo(line(p, "taser", m, r), 30);
                }
                if (t == 8) {
                    Vec3 a = cop.getEyePosition().add(dir.scale(0.6)).add(0, -0.3, 0), b = m.position().add(0, m.getBbHeight() * 0.6, 0);
                    int n = (int) (a.distanceTo(b) * 3);
                    for (int i = 0; i <= n; i++) {
                        Vec3 q = a.lerp(b, i / (double) n).add((r.nextDouble() - 0.5) * 0.25, (r.nextDouble() - 0.5) * 0.25, (r.nextDouble() - 0.5) * 0.25);
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, q.x, q.y, q.z, 1, 0, 0, 0, 0);
                        if (i % 3 == 0) sl.sendParticles(new DustParticleOptions(new Vector3f(0.55f, 0.85f, 1f), 0.9f), q.x, q.y, q.z, 1, 0, 0, 0, 0);
                    }
                    sl.playSound(null, m.blockPosition(), SoundEvents.BEE_STING, SoundSource.NEUTRAL, 1.2f, 0.5f);
                    sl.playSound(null, m.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.NEUTRAL, 0.5f, 2f);
                    hit(cop, m, 4f, dir.scale(0.25), "zap", 0, f);
                    m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 5));
                    m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0));
                    m.getNavigation().stop();
                }
                if (t > 8 && t < 30 && t % 3 == 0) sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, m.getX(), m.getY() + m.getBbHeight() / 2, m.getZ(), 5, 0.35, 0.5, 0.35, 0.1);
                if (t >= 22) { f.move = NONE; f.taserCd = 140; }
            }
            case UPPER -> {
                cop.getNavigation().stop();
                if (t == 0) {
                    cop.gesture(Resident.G_UPPERCUT, 12);
                    cop.sayTo(line(p, "upper", m, r), 30);
                }
                if (t == 4) {
                    hit(cop, m, 5f, new Vec3(dir.x * 0.15, 1.25, dir.z * 0.15), "launch", 3, f);
                    sl.playSound(null, m.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.NEUTRAL, 1.4f, 0.7f);
                }
                if (t == 12) {
                    f.move = SPIKE;
                    f.moveT = 0;
                    f.upperCd = 160;
                    f.combo = 0;
                }
            }
            case SPIKE -> {
                if (t == 0) {
                    cop.setDeltaMovement(dir.scale(0.2).add(0, 1.1, 0));
                    cop.hurtMarked = true;
                    cop.gesture(Resident.G_SLAM, 16);
                    sl.playSound(null, cop.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.NEUTRAL, 1f, 1.4f);
                }
                if (t > 0 && t < 14) sl.sendParticles(ParticleTypes.END_ROD, cop.getX(), cop.getY() + 0.5, cop.getZ(), 2, 0.2, 0.3, 0.2, 0.01);
                if (t == 9 && cop.distanceTo(m) < 4.5) {
                    hit(cop, m, 8f, new Vec3(dir.x * 0.3, -2.2, dir.z * 0.3), "spike", 4, f);
                    sl.playSound(null, m.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.NEUTRAL, 1.5f, 0.5f);
                }
                if (t >= 10 && (cop.onGround() || t > 26)) {
                    shockwave(sl, cop, 4.0, 0.9, f, false);
                    f.move = NONE;
                }
            }
            case KICK -> {
                cop.getNavigation().stop();
                if (t == 0) {
                    cop.gesture(Resident.G_KICK, 14);
                    cop.sayTo(line(p, "kick", m, r), 30);
                    sl.playSound(null, cop.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 1.4f, 0.5f);
                }
                if (t < 12) {
                    double a = t * Math.PI * 2 / 12;
                    for (int k = 0; k < 3; k++) sl.sendParticles(ParticleTypes.SWEEP_ATTACK, cop.getX() + Math.cos(a + k * 2.1) * 1.8, cop.getY() + 0.7, cop.getZ() + Math.sin(a + k * 2.1) * 1.8, 1, 0, 0, 0, 0);
                }
                if (t == 6) {
                    for (Mob o : sl.getEntitiesOfClass(Mob.class, cop.getBoundingBox().inflate(3.6, 1.5, 3.6), Police::hostile)) {
                        Vec3 out = flat(cop, o);
                        hit(cop, o, 6f, out.scale(1.5).add(0, 0.55, 0), "spin", 2, f);
                    }
                }
                if (t >= 14) { f.move = NONE; f.kickCd = 120; }
            }
            case SLAM -> {
                cop.getNavigation().stop();
                if (t == 0) {
                    cop.gesture(Resident.G_SLAM, 22);
                    cop.setDeltaMovement(0, 0.85, 0);
                    cop.hurtMarked = true;
                    cop.sayTo(line(p, "slam", m, r), 30);
                }
                if (t > 2 && t < 14) sl.sendParticles(new DustParticleOptions(new Vector3f(1f, 0.8f, 0.2f), 1.2f), cop.getX(), cop.getY() + 1, cop.getZ(), 3, 0.4, 0.4, 0.4, 0);
                if (t == 8) cop.setDeltaMovement(0, -1.6, 0);
                if (t >= 9 && (cop.onGround() || t > 24)) {
                    shockwave(sl, cop, 5.5, 1.3, f, true);
                    f.move = NONE;
                    f.slamCd = 140;
                }
            }
            case PUNT -> {
                cop.getNavigation().stop();
                if (t == 0) {
                    cop.gesture(Resident.G_KICK, 10);
                    cop.sayTo(p.trait == Trait.GRUMPY ? "Not today, creeper." : "FORE!", 30);
                }
                if (t == 3) {
                    hit(cop, m, 3f, dir.scale(2.6).add(0, 1.0, 0), "launch", 3, f);
                    if (m instanceof Creeper cr) cr.setSwellDir(-1);
                    sl.playSound(null, m.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.NEUTRAL, 1.6f, 0.6f);
                }
                if (t >= 10) f.move = NONE;
            }
            case SHOOT -> {
                cop.getNavigation().stop();
                if (t == 0) {
                    draw(cop);
                    cop.gesture(Resident.G_AIM, 26);
                    if (r.nextInt(2) == 0) cop.sayTo(line(p, m instanceof Creeper ? "creeper" : flying(m) ? "air" : "shoot", m, r), 30);
                }
                if ((t == 6 || t == 11 || t == 16) && m.isAlive()) fire(sl, cop, m, tank(m) ? 5f : 4f, f);
                if (t >= 22) { f.move = NONE; f.shootCd = m instanceof Creeper || flying(m) ? 18 : 50; holster(cop); }
            }
            case SPRAY -> {
                cop.getNavigation().stop();
                if (t == 0) {
                    draw(cop);
                    cop.gesture(Resident.G_AIM, 34);
                    cop.sayTo(Lines.pick(r, "Suppressing fire!", "Everybody get down!", "I'll thin the crowd!"), 30);
                }
                if (t >= 4 && t <= 28 && t % 4 == 0) {
                    List<Mob> ms = sl.getEntitiesOfClass(Mob.class, cop.getBoundingBox().inflate(16, 6, 16), x -> hostile(x) && cop.hasLineOfSight(x));
                    if (!ms.isEmpty()) {
                        Mob tgt = ms.get((t / 4) % ms.size());
                        cop.getLookControl().setLookAt(tgt, 90, 90);
                        fire(sl, cop, tgt, 3.5f, f);
                    }
                }
                if (t >= 32) { f.move = NONE; f.shootCd = 90; holster(cop); }
            }
            case BARRAGE -> {
                cop.getNavigation().stop();
                if (t == 0) {
                    cop.sayTo(Lines.pick(r, "Hundred Fist Protocol!", "Take THIS!", "ORA ORA ORA!", "Full combo!"), 30);
                    sl.playSound(null, cop.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.NEUTRAL, 1.2f, 1.4f);
                }
                if (t % 3 == 0 && t < 30) {
                    cop.gesture(t % 6 == 0 ? Resident.G_JAB_R : Resident.G_JAB_L, 3);
                    for (int k = 0; k < 3; k++) {
                        double ox = (r.nextDouble() - 0.5) * 0.8, oy = r.nextDouble() * 0.8, oz = (r.nextDouble() - 0.5) * 0.8;
                        sl.sendParticles(new DustParticleOptions(new Vector3f(0.4f, 0.75f, 1f), 1.6f), m.getX() + ox, m.getY() + m.getBbHeight() * 0.5 + oy, m.getZ() + oz, 2, 0.05, 0.05, 0.05, 0);
                    }
                    if (dist < 3.6) hit(cop, m, (t >= 27 ? 9f : 2.5f) * mult(f, m), t >= 27 ? dir.scale(1.8).add(0, 0.6, 0) : dir.scale(0.05), t >= 27 ? "heavy" : "jab", t >= 27 ? 3 : 0, f);
                }
                if (t >= 32) { f.move = NONE; f.barrageCd = f.ult > 0 ? 50 : 140; }
            }
            case RAIL -> {
                cop.getNavigation().stop();
                if (t == 0) {
                    draw(cop);
                    cop.gesture(Resident.G_AIM, 36);
                    cop.sayTo(Lines.pick(r, "Charging the rail!", "Hold... still...", "Lights out."), 30);
                    sl.playSound(null, cop.blockPosition(), RAILGUN, SoundSource.NEUTRAL, 2.5f, 1f);
                }
                if (t < 12) {
                    Vec3 mz = muzzle(cop);
                    double a = t * 0.9;
                    for (int k = 0; k < 3; k++) sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, mz.x + Math.cos(a + k * 2.1) * 0.35, mz.y + Math.sin(a + k * 2.1) * 0.35, mz.z, 1, 0, 0, 0, 0);
                }
                if (t == 13 && m.isAlive()) {
                    Vec3 a0 = muzzle(cop), b0 = m.position().add(0, m.getBbHeight() * 0.55, 0);
                    Vec3 d0 = b0.subtract(a0).normalize();
                    double len = Math.min(48, a0.distanceTo(b0) + 6);
                    for (double s0 = 0; s0 < len; s0 += 0.35) {
                        Vec3 q = a0.add(d0.scale(s0));
                        far(sl, ParticleTypes.END_ROD, q, 1, 0.02);
                        if (((int) (s0 * 3)) % 3 == 0) far(sl, new DustParticleOptions(new Vector3f(0.3f, 0.8f, 1f), 2.2f), q, 2, 0.08);
                    }
                    for (Mob o : sl.getEntitiesOfClass(Mob.class, new AABB(a0, a0.add(d0.scale(len))).inflate(1.2), Police::hostile)) {
                        Vec3 rel = o.position().add(0, o.getBbHeight() / 2, 0).subtract(a0);
                        if (rel.cross(d0).length() < 1.4) hit(cop, o, 14f * mult(f, o), d0.scale(1.4).add(0, 0.5, 0), "heavy", 3, f);
                    }
                    far(sl, ParticleTypes.FLASH, b0, 1, 0);
                    far(sl, ParticleTypes.EXPLOSION, b0, 2, 0.4);
                    impact(sl, cop, "quake", Vec3.ZERO, 1);
                }
                if (t >= 26) { f.move = NONE; f.railCd = f.ult > 0 ? 60 : 200; holster(cop); }
            }
            case ULT_START -> ultStart(cop, p, f, m, t, sl, r);
            case WARP -> {
                if (t == 0 || t == 8 || t == 16) {
                    Vec3 behind = m.position().add(flat(cop, m).scale(1.6).yRot((float) (Math.PI * (t / 8) * 0.66)));
                    sl.sendParticles(ParticleTypes.REVERSE_PORTAL, cop.getX(), cop.getY() + 1, cop.getZ(), 30, 0.3, 0.7, 0.3, 0.3);
                    cop.teleportTo(behind.x, m.getY(), behind.z);
                    sl.sendParticles(ParticleTypes.REVERSE_PORTAL, behind.x, m.getY() + 1, behind.z, 30, 0.3, 0.7, 0.3, 0.3);
                    sl.playSound(null, cop.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1f, 1.6f);
                    cop.gesture(t == 16 ? Resident.G_KICK : Resident.G_JAB_R, 6);
                }
                if ((t == 3 || t == 11 || t == 19) && m.isAlive()) {
                    Vec3 k = flat(cop, m);
                    hit(cop, m, 7f * mult(f, m), k.scale(t == 19 ? 1.6 : 0.3).add(0, t == 19 ? 0.7 : 0.1, 0), t == 19 ? "spin" : "heavy", 2, f);
                    for (int i = 0; i < 12; i++) {
                        double a = i * Math.PI / 6;
                        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, m.getX() + Math.cos(a) * 1.2, m.getY() + 1, m.getZ() + Math.sin(a) * 1.2, 1, 0, 0, 0, 0);
                    }
                }
                if (t >= 24) { f.move = NONE; f.warpCd = 90; }
            }
            case METEOR -> {
                cop.getNavigation().stop();
                if (t == 0) {
                    cop.sayTo(Lines.pick(r, "METEOR... STRIKE!", "From the sky!", "Solaris... IMPACT!"), 40);
                    cop.setDeltaMovement(0, 1.9, 0);
                    cop.hurtMarked = true;
                    cop.gesture(Resident.G_SLAM, 40);
                    sl.playSound(null, cop.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 1.4f, 0.6f);
                }
                if (t > 0 && t < 30) {
                    far(sl, ParticleTypes.FLAME, cop.position().add(0, 0.8, 0), 6, 0.3);
                    far(sl, new DustParticleOptions(new Vector3f(1f, 0.55f, 0.1f), 2.4f), cop.position().add(0, 0.8, 0), 4, 0.35);
                }
                if (t == 14) {
                    Vec3 v = m.position().subtract(cop.position());
                    cop.setDeltaMovement(v.x * 0.25, -2.6, v.z * 0.25);
                    cop.hurtMarked = true;
                }
                if (t >= 16 && (cop.onGround() || t > 40)) {
                    shockwave(sl, cop, 7.5, 1.8, f, true);
                    for (int i = 0; i < 3; i++) {
                        net.minecraft.world.entity.LightningBolt lb = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(sl);
                        if (lb != null) {
                            lb.moveTo(cop.getX() + (r.nextDouble() - 0.5) * 6, cop.getY(), cop.getZ() + (r.nextDouble() - 0.5) * 6);
                            lb.setVisualOnly(true);
                            sl.addFreshEntity(lb);
                        }
                    }
                    f.move = NONE;
                    f.meteorCd = 120;
                }
            }
            case FINISHER -> finisher(cop, p, f, m, t, sl, r);
            default -> f.move = NONE;
        }
    }

    static float mult(Fight f, Entity m) {
        return (tank(m) ? 1.7f : 1f) * (f.ult > 0 ? 2.4f : 1f);
    }

    static int crowd(ServerLevel sl, Resident cop) {
        return sl.getEntitiesOfClass(Mob.class, cop.getBoundingBox().inflate(9, 4, 9), Police::hostile).size();
    }

    static void far(ServerLevel sl, net.minecraft.core.particles.ParticleOptions o, Vec3 q, int n, double spread) {
        for (ServerPlayer pl : sl.players()) if (pl.distanceToSqr(q) < 160 * 160) sl.sendParticles(pl, o, true, q.x, q.y, q.z, n, spread, spread, spread, 0);
    }

    static Vec3 muzzle(Resident cop) {
        float yaw = cop.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(cop.getYHeadRot() * Mth.DEG_TO_RAD), 0, Mth.cos(cop.getYHeadRot() * Mth.DEG_TO_RAD));
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        return cop.position().add(0, 1.38, 0).add(fwd.scale(0.9)).add(right.scale(-0.33));
    }

    static void draw(Resident cop) {
        if (!(cop.getMainHandItem().getItem() == FireheartCity.SIDEARM.get())) {
            cop.getPersistentData().put("fhcHolster", cop.getMainHandItem().save(new net.minecraft.nbt.CompoundTag()));
            cop.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(FireheartCity.SIDEARM.get()));
        }
    }

    static void holster(Resident cop) {
        if (cop.getMainHandItem().getItem() != FireheartCity.SIDEARM.get()) return;
        var tag = cop.getPersistentData().getCompound("fhcHolster");
        cop.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, tag.isEmpty() ? net.minecraft.world.item.ItemStack.EMPTY : net.minecraft.world.item.ItemStack.of(tag));
        cop.getPersistentData().remove("fhcHolster");
    }

    static void fire(ServerLevel sl, Resident cop, Mob m, float dmg, Fight f) {
        cop.getLookControl().setLookAt(m, 90, 90);
        Vec3 a = muzzle(cop), b = m.position().add((sl.random.nextDouble() - 0.5) * 0.3, m.getBbHeight() * 0.6, (sl.random.nextDouble() - 0.5) * 0.3);
        var hitRes = sl.clip(new net.minecraft.world.level.ClipContext(a, b, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, cop));
        boolean clear = hitRes.getType() == net.minecraft.world.phys.HitResult.Type.MISS || hitRes.getLocation().distanceTo(b) < 0.6;
        Vec3 end = clear ? b : hitRes.getLocation();
        sl.playSound(null, cop.getX(), cop.getY() + 1.4, cop.getZ(), SHOT, SoundSource.NEUTRAL, 2.2f, 0.95f + sl.random.nextFloat() * 0.1f);
        far(sl, ParticleTypes.SMOKE, a, 3, 0.03);
        far(sl, ParticleTypes.FLAME, a, 1, 0.01);
        far(sl, ParticleTypes.ELECTRIC_SPARK, a, 4, 0.05);
        Vec3 d0 = end.subtract(a);
        double len = d0.length();
        for (double s0 = 0.4; s0 < len; s0 += 0.8) {
            Vec3 q = a.add(d0.scale(s0 / len));
            far(sl, new DustParticleOptions(new Vector3f(1f, 0.9f, 0.55f), 0.55f), q, 1, 0);
        }
        if (!clear) {
            far(sl, new BlockParticleOption(ParticleTypes.BLOCK, sl.getBlockState(hitRes.getBlockPos())), end, 6, 0.1);
            return;
        }
        Vec3 kb = flat(cop, m).scale(m instanceof Creeper ? 0.7 : 0.25).add(0, 0.12, 0);
        hit(cop, m, dmg * mult(f, m), kb, "jab", 0, f);
        if (m instanceof Creeper cr) cr.setSwellDir(-1);
    }

    static boolean ultReady(Resident cop) {
        Long t = ULT_READY.get(cop.getUUID());
        return t == null || cop.level().getGameTime() >= t;
    }

    static float ultChance(ServerLevel sl, Resident cop, Mob m) {
        if (tank(m)) return m.getHealth() < m.getMaxHealth() * 0.8f ? 0.25f : 0.06f;
        int c = crowd(sl, cop);
        return c >= 4 ? 0.08f : c >= 2 ? 0.015f : 0.004f;
    }

    static boolean finishable(Mob m, Fight f) {
        return m.isAlive() && (m.getHealth() <= Math.max(10f, m.getMaxHealth() * (tank(m) ? 0.12f : 0.35f)));
    }

    static void ultStart(Resident cop, CityData.Profile p, Fight f, Mob m, int t, ServerLevel sl, RandomSource r) {
        cop.getNavigation().stop();
        cop.setDeltaMovement(0, t < 20 ? 0.04 : 0, 0);
        cop.hurtMarked = true;
        if (t == 0) {
            ULT_READY.put(cop.getUUID(), sl.getGameTime() + ULT_COOLDOWN);
            cop.gesture(Resident.G_ULT, 50);
            sl.playSound(null, cop.blockPosition(), ULT, SoundSource.NEUTRAL, 3f, 1f);
            cutscene(sl, cop, "ult|" + cop.getId() + "|" + (p == null ? "Officer" : p.name) + "|" + ultName(p));
            cop.sayTo(ultCall(p, r), 60);
        }
        for (Mob o : sl.getEntitiesOfClass(Mob.class, cop.getBoundingBox().inflate(14), Police::hostile)) o.setDeltaMovement(Vec3.ZERO);
        double a = t * 0.45;
        for (int k = 0; k < 4; k++) {
            double h = (t % 20) / 10.0 + k * 0.5;
            far(sl, ParticleTypes.END_ROD, cop.position().add(Math.cos(a + k * 1.57) * 1.2, h, Math.sin(a + k * 1.57) * 1.2), 1, 0);
            far(sl, new DustParticleOptions(new Vector3f(0.35f, 0.7f, 1f), 1.8f), cop.position().add(Math.cos(-a + k * 1.57) * 0.8, 2.2 - h * 0.5, Math.sin(-a + k * 1.57) * 0.8), 1, 0);
        }
        if (t == 30) {
            net.minecraft.world.entity.LightningBolt lb = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(sl);
            if (lb != null) { lb.moveTo(cop.getX(), cop.getY(), cop.getZ()); lb.setVisualOnly(true); sl.addFreshEntity(lb); }
            far(sl, ParticleTypes.FLASH, cop.position().add(0, 1, 0), 1, 0);
            shockwave(sl, cop, 5, 1.0, f, true);
        }
        if (t >= 44) {
            f.move = NONE;
            f.ult = ULT_TICKS;
            f.warpCd = f.meteorCd = f.railCd = f.barrageCd = 0;
            cop.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ULT_TICKS, 2, false, false));
        }
    }

    static void ultTick(Resident cop, CityData.Profile p, Fight f, ServerLevel sl) {
        f.ult--;
        if (cop.tickCount % 2 == 0) {
            far(sl, new DustParticleOptions(new Vector3f(0.3f, 0.75f, 1f), 1.3f), cop.position().add(0, 1, 0), 3, 0.35);
            if (cop.tickCount % 6 == 0) far(sl, ParticleTypes.ELECTRIC_SPARK, cop.position().add(0, 1.2, 0), 3, 0.4);
        }
        if (f.ult == 0) endUlt(cop, p, sl);
    }

    static void endUlt(Resident cop, CityData.Profile p, ServerLevel sl) {
        RESTING.put(cop.getUUID(), sl.getGameTime() + REST_TICKS);
        cop.removeEffect(MobEffects.MOVEMENT_SPEED);
        cop.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, REST_TICKS, 1, false, false));
        cop.gesture(Resident.G_YAWN, 60);
        cop.sayTo(Lines.pick(sl.random, "Haah... haah... that took everything I had.", "I need... a very long break.", "Ultimate's spent. Someone else take the next one.", "Whew. Coffee. Now."), 80);
        sl.sendParticles(ParticleTypes.CLOUD, cop.getX(), cop.getY() + 1, cop.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
        if (p != null) p.log(cop.routineDay()).note("I used my ultimate and I'm completely wiped out");
    }

    static String ultName(CityData.Profile p) {
        if (p == null) return "Overdrive";
        return switch (p.id) {
            case "dex" -> "Night Justice";
            case "kira" -> "Solar Flare Protocol";
            case "bruno" -> "Iron Verdict";
            default -> "Overdrive";
        };
    }

    static String ultCall(CityData.Profile p, RandomSource r) {
        if (p == null) return "OVERDRIVE!";
        return switch (p.id) {
            case "dex" -> "The night is MINE. Night Justice - ACTIVATE!";
            case "kira" -> "Everyone shield your eyes! SOLAR FLARE PROTOCOL!";
            case "bruno" -> "...Fine. You asked for it. IRON VERDICT.";
            default -> "OVERDRIVE!";
        };
    }

    static String finisherName(CityData.Profile p, Entity m) {
        String base = p == null ? "Final Arrest" : switch (p.id) {
            case "dex" -> "Midnight Guillotine";
            case "kira" -> "Supernova Cuffs";
            case "bruno" -> "Gavel of the Law";
            default -> "Final Arrest";
        };
        return base;
    }

    static void cutscene(ServerLevel sl, Entity at, String payload) {
        for (ServerPlayer pl : sl.players()) if (pl.distanceTo(at) < 72) PcNet.send(pl, new PcNet.Msg("#fx|" + payload));
    }

    static void finisher(Resident cop, CityData.Profile p, Fight f, Mob m, int t, ServerLevel sl, RandomSource r) {
        cop.getNavigation().stop();
        if (!m.isAlive()) { f.move = NONE; return; }
        m.setDeltaMovement(0, t < 40 ? 0.02 : 0, 0);
        m.hurtMarked = true;
        m.getNavigation().stop();
        if (t == 0) {
            Vec3 front = m.position().add(flat(m, cop).scale(Math.max(1.8, m.getBbWidth() * 0.6 + 1.2)));
            cop.teleportTo(front.x, m.getY(), front.z);
            cop.gesture(Resident.G_ULT, 40);
            cop.sayTo(Lines.pick(r, "It's over.", "You're under arrest. PERMANENTLY.", "This ends now!", "Case... CLOSED."), 60);
            sl.playSound(null, cop.blockPosition(), FINISH, SoundSource.NEUTRAL, 3f, 1f);
            cutscene(sl, cop, "finish|" + cop.getId() + "|" + m.getId() + "|" + (p == null ? "Officer" : p.name) + "|" + finisherName(p, m));
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 10, false, false));
        }
        cop.getLookControl().setLookAt(m, 90, 90);
        if (t < 24) {
            double a = t * 0.6;
            for (int k = 0; k < 6; k++) {
                double rr = 3.0 - t * 0.1;
                far(sl, ParticleTypes.END_ROD, m.position().add(Math.cos(a + k * 1.05) * rr, m.getBbHeight() * 0.5 + Math.sin(a * 0.5 + k) * 0.6, Math.sin(a + k * 1.05) * rr), 1, 0);
            }
            if (t % 4 == 0) far(sl, ParticleTypes.ELECTRIC_SPARK, cop.position().add(0, 1.3, 0), 8, 0.3);
        }
        if (t == 24) {
            cop.gesture(Resident.G_UPPERCUT, 16);
            cop.setDeltaMovement(flat(cop, m).scale(0.6).add(0, 0.2, 0));
            cop.hurtMarked = true;
        }
        if (t == 27) {
            Vec3 c = m.position().add(0, m.getBbHeight() * 0.5, 0);
            far(sl, ParticleTypes.FLASH, c, 2, 0.1);
            far(sl, ParticleTypes.EXPLOSION_EMITTER, c, 1, 0);
            far(sl, ParticleTypes.SONIC_BOOM, c, 1, 0);
            for (int i = 0; i < 40; i++) {
                double a = i * Math.PI * 2 / 40;
                far(sl, new DustParticleOptions(new Vector3f(1f, 0.85f, 0.3f), 2.5f), c.add(Math.cos(a) * 2.5, 0, Math.sin(a) * 2.5), 2, 0.1);
                far(sl, ParticleTypes.END_ROD, c.add(0, i * 0.4, 0), 1, 0.05);
            }
            net.minecraft.world.entity.LightningBolt lb = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(sl);
            if (lb != null) { lb.moveTo(m.getX(), m.getY(), m.getZ()); lb.setVisualOnly(true); sl.addFreshEntity(lb); }
            sl.playSound(null, m.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 2f, 0.6f);
            m.invulnerableTime = 0;
            m.hurt(sl.damageSources().mobAttack(cop), Math.max(m.getHealth() + 50, 200));
            if (m.isAlive()) m.kill();
            impact(sl, m, "launch", flat(cop, m), 4);
            impact(sl, cop, "quake", Vec3.ZERO, 3);
        }
        if (t >= 44) {
            f.move = NONE;
            cop.gesture(Resident.G_VICTORY, 50);
        }
    }

    static void shockwave(ServerLevel sl, Resident cop, double radius, double power, Fight f, boolean big) {
        BlockPos under = cop.blockPosition().below();
        BlockState bs = sl.getBlockState(under);
        if (bs.isAir()) bs = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        for (int ring = 1; ring <= (big ? 3 : 2); ring++) {
            double rr = radius * ring / (big ? 3.0 : 2.0);
            int n = (int) (rr * 10);
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2 / n;
                double x = cop.getX() + Math.cos(a) * rr, z = cop.getZ() + Math.sin(a) * rr;
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, bs), x, cop.getY() + 0.1, z, 3, 0.1, 0.1, 0.1, 0.3);
                if (i % 3 == 0) sl.sendParticles(ParticleTypes.CLOUD, x, cop.getY() + 0.2, z, 1, 0, 0.05, 0, 0.05);
            }
        }
        sl.sendParticles(big ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION, cop.getX(), cop.getY() + 0.3, cop.getZ(), 1, 0, 0, 0, 0);
        sl.playSound(null, cop.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, big ? 1.4f : 0.8f, big ? 0.7f : 1.1f);
        sl.playSound(null, cop.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 0.8f, 0.5f);
        for (Mob o : sl.getEntitiesOfClass(Mob.class, cop.getBoundingBox().inflate(radius, 2, radius), Police::hostile)) {
            Vec3 out = flat(cop, o);
            double fall = 1 - Math.min(1, cop.distanceTo(o) / radius) * 0.5;
            hit(cop, o, big ? 7f : 5f, out.scale(power * 1.3 * fall).add(0, power * 0.8 * fall, 0), "slam", big ? 3 : 1, f);
        }
        impact(sl, cop, "quake", Vec3.ZERO, big ? 2 : 1);
    }

    static void hit(Resident cop, Mob m, float dmg, Vec3 kb, String kind, int freeze, Fight f) {
        ServerLevel sl = (ServerLevel) cop.level();
        m.invulnerableTime = 0;
        m.hurt(sl.damageSources().mobAttack(cop), dmg);
        m.setTarget(null);
        double hx = m.getX(), hy = m.getY() + m.getBbHeight() * 0.6, hz = m.getZ();
        sl.sendParticles(ParticleTypes.CRIT, hx, hy, hz, 10, 0.3, 0.3, 0.3, 0.4);
        sl.sendParticles(ParticleTypes.ENCHANTED_HIT, hx, hy, hz, 6, 0.3, 0.3, 0.3, 0.3);
        if (!kind.equals("jab")) sl.sendParticles(ParticleTypes.SWEEP_ATTACK, hx, hy, hz, 1, 0, 0, 0, 0);
        if (freeze >= 2) sl.sendParticles(ParticleTypes.FLASH, hx, hy, hz, 1, 0, 0, 0, 0);
        SoundEvent s = switch (kind) {
            case "jab" -> SoundEvents.PLAYER_ATTACK_WEAK;
            case "zap" -> SoundEvents.PLAYER_ATTACK_NODAMAGE;
            default -> SoundEvents.PLAYER_ATTACK_STRONG;
        };
        sl.playSound(null, m.blockPosition(), s, SoundSource.NEUTRAL, 1.2f, 0.8f + sl.random.nextFloat() * 0.3f);
        if (freeze >= 3) sl.playSound(null, m.blockPosition(), SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.NEUTRAL, 0.6f, 1.6f);
        impact(sl, m, kind, kb, freeze);
        if (freeze > 0 && m.isAlive()) {
            f.freeze = freeze;
            f.pendingMob = m;
            f.pendingKb = kb;
            f.pendingKind = kind;
        } else launch(m, kb, kind, cop);
    }

    static void launch(Mob m, Vec3 kb, String kind, Resident cop) {
        if (!m.isAlive() && !kind.equals("launch")) return;
        m.setDeltaMovement(kb);
        m.hurtMarked = true;
        m.hasImpulse = true;
        if (kind.equals("launch") || kind.equals("spike") || kind.equals("slam")) m.fallDistance = 0;
    }

    static void impact(ServerLevel sl, Entity e, String kind, Vec3 dir, int power) {
        String line = "#hit|" + e.getId() + "|" + kind + "|" + String.format(java.util.Locale.ROOT, "%.2f|%.2f|%d", dir.x, dir.z, power);
        for (ServerPlayer pl : sl.players()) if (pl.distanceToSqr(e) < 80 * 80) PcNet.send(pl, new PcNet.Msg(line));
    }

    static void finish(Resident cop, CityData.Profile p, Fight f, boolean won, Entity te) {
        CLAIMED.remove(cop.getUUID());
        f.target = null;
        f.move = NONE;
        f.combo = 0;
        f.backup = false;
        holster(cop);
        if (f.ult > 0 && cop.level() instanceof ServerLevel sl0) {
            Mob next = null;
            for (Mob o : sl0.getEntitiesOfClass(Mob.class, cop.getBoundingBox().inflate(20, 6, 20), Police::hostile)) { next = o; break; }
            if (next != null) {
                f.target = next.getUUID();
                CLAIMED.put(cop.getUUID(), next.getUUID());
                return;
            }
            f.ult = 0;
            endUlt(cop, p, sl0);
        }
        if (!won) return;
        f.wins++;
        RandomSource r = cop.getRandom();
        cop.gesture(Resident.G_VICTORY, 40);
        cop.sayTo(line(p, "win", te, r), 60);
        ServerLevel sl = (ServerLevel) cop.level();
        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, cop.getX(), cop.getY() + 2, cop.getZ(), 6, 0.4, 0.3, 0.4, 0.02);
        CityData d = cop.data();
        long day = cop.day();
        String what = te == null ? "a monster" : "a " + te.getType().getDescription().getString().toLowerCase(java.util.Locale.ROOT);
        p.log(cop.routineDay()).note("I took down " + what + " on patrol");
        if (f.wins % 3 == 1) d.event(day, "police", "Officer " + p.name + " fought off " + what + " near " + Dialogue.here(cop), cop.blockPosition(), p.id);
        for (Resident o : sl.getEntitiesOfClass(Resident.class, cop.getBoundingBox().inflate(14), x -> x != cop && !x.isSleeping() && x.isFree())) {
            if (r.nextInt(2) != 0) continue;
            o.getLookControl().setLookAt(cop, 30, 30);
            o.gesture(Resident.G_CLAP, 40);
            o.say(Lines.pick(r, "Go " + p.name + "!", "Thank you, officer!", "Whoa, did you SEE that?!", "Solaris PD, heck yeah!", "My hero!"), 50);
        }
        d.setDirty();
    }

    static String line(CityData.Profile p, String kind, Entity m, RandomSource r) {
        String mob = m == null ? "creep" : m.getType().getDescription().getString().toLowerCase(java.util.Locale.ROOT);
        boolean grumpy = p.trait == Trait.GRUMPY, friendly = p.trait == Trait.FRIENDLY;
        return switch (kind) {
            case "engage" -> grumpy ? Lines.pick(r, "Hey, " + mob + ". You picked the wrong street.", "Great. A " + mob + ". Just what my shift needed.") : friendly ? Lines.pick(r, "Everyone stay back - I've got this " + mob + "!", "Solaris PD! Nobody panic!") : Lines.pick(r, "Solaris PD! Freeze, " + mob + "!", "Not on my watch!", "Suspect sighted - engaging!");
            case "dash" -> Lines.pick(r, "Coming through!", "Too slow!", "Hyah!");
            case "taser" -> Lines.pick(r, "Taser!", "Hold still!", "Zap time.");
            case "upper" -> grumpy ? "Up you go." : Lines.pick(r, "Rising Dragon!", "Sky high!", "Upsy-daisy!");
            case "kick" -> Lines.pick(r, "Back off, all of you!", "Tornado kick!", "Spin cycle!");
            case "slam" -> grumpy ? "Enough." : Lines.pick(r, "Shockwave!", "Ground... POUND!", "Everybody down!");
            case "creeper" -> Lines.pick(r, "Not letting that thing get close!", "Creeper! Keep your distance, folks!", "Long range for this one.");
            case "air" -> Lines.pick(r, "Target in the air!", "Taking the shot!", "Flyer - bringing it down!");
            case "shoot" -> Lines.pick(r, "Shots fired!", "Covering fire!", "Freeze!");
            default -> grumpy ? Lines.pick(r, "And stay down.", "Paperwork's gonna be a pain.") : friendly ? Lines.pick(r, "All clear! Everyone okay?", "You're safe now, folks!") : Lines.pick(r, "Justice... served.", "Area secure.", "Another one for the record books!");
        };
    }
}
