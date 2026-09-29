package com.fireheart.city;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Solaris vehicles: a compact city car, a motorbike and a speedboat. Driven like vanilla boats (the driver's client
 * simulates and the server follows), with acceleration, braking, reverse, speed-sensitive steering, handbrake drifts,
 * kerb climbing, headlights, horn and engine sound.
 */
public class Vehicle extends Entity {
    public static final int CAR = 0, BIKE = 1, BOAT = 2;
    public static final String[] NAMES = {"Solaris Coupe", "Solaris Street Bike", "Solaris Speedboat"};
    public static final String[] PAINTS = {"Magma Orange", "Midnight Navy", "Racing Red", "Pearl White", "Stealth Black", "Neon Teal"};
    public static final int[] PAINT_RGB = {0xFF6A1F, 0x1B2A5A, 0xD7263D, 0xF1F1EE, 0x1A1A1E, 0x16C7B7};
    static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(Vehicle.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> PAINT = SynchedEntityData.defineId(Vehicle.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Boolean> LIGHTS = SynchedEntityData.defineId(Vehicle.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<String> OWNER = SynchedEntityData.defineId(Vehicle.class, EntityDataSerializers.STRING);

    public float speed, steerVis, wheelSpin, prevWheelSpin, lean, prevLean;
    public float steer, rpm = 0.15f, pitchVis, prevPitchVis, rollVis, prevRollVis, bodyY, prevBodyY, yawRate, slip;
    public int gear = 1, shiftT, blink;
    public boolean braking, reversing, drifting, throttle;
    private float lastSpeed, slope;
    private double lastY;
    private int crashCd, reverseHold;
    static final float[][] GEARS = {{0.2f, 0.38f, 0.56f, 0.77f, 1f}, {0.15f, 0.29f, 0.44f, 0.6f, 0.8f, 1f}, {1f}};
    static final float[] WHEELBASE = {1.9f, 2.1f, 2.3f};
    static final float[] WHEEL_R = {0.3125f, 0.375f, 0.3125f};
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ, lerpYRot;
    private int hits;
    private long lastHit;

    public Vehicle(EntityType<? extends Vehicle> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        setMaxUpStep(1.05f);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(KIND, CAR);
        entityData.define(PAINT, 0);
        entityData.define(LIGHTS, false);
        entityData.define(OWNER, "");
    }

    public int kind() { return entityData.get(KIND); }

    public int paint() { return Math.floorMod(entityData.get(PAINT), PAINTS.length); }

    public boolean lights() { return entityData.get(LIGHTS); }

    public String owner() { return entityData.get(OWNER); }

    public void setup(int kind, int paint, String owner) {
        entityData.set(KIND, kind);
        entityData.set(PAINT, paint);
        entityData.set(OWNER, owner);
        refreshDimensions();
    }

    @Override
    public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        return switch (kind()) {
            case BIKE -> net.minecraft.world.entity.EntityDimensions.scalable(0.95f, 1.3f);
            case BOAT -> net.minecraft.world.entity.EntityDimensions.scalable(2.3f, 1.0f);
            default -> net.minecraft.world.entity.EntityDimensions.scalable(1.9f, 1.4f);
        };
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (KIND.equals(key)) refreshDimensions();
    }

    public static float maxSpeed(int kind) {
        return kind == BIKE ? 1.45f : kind == BOAT ? 0.95f : 1.25f;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        entityData.set(KIND, t.getInt("Kind"));
        refreshDimensions();
        entityData.set(PAINT, t.getInt("Paint"));
        entityData.set(LIGHTS, t.getBoolean("Lights"));
        entityData.set(OWNER, t.getString("Owner"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        t.putInt("Kind", kind());
        t.putInt("Paint", paint());
        t.putBoolean("Lights", lights());
        t.putString("Owner", owner());
    }

    @Override
    public boolean canCollideWith(Entity e) {
        return (e.canBeCollidedWith() || e.isPushable()) && !isPassengerOfSameVehicle(e);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    protected boolean canAddPassenger(Entity e) {
        return getPassengers().size() < 2;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        Entity e = getFirstPassenger();
        return e instanceof Player p ? p : null;
    }

    @Override
    public double getPassengersRidingOffset() {
        return kind() == BIKE ? 0.55 : kind() == BOAT ? 0.35 : 0.25;
    }

    @Override
    protected void positionRider(Entity p, MoveFunction f) {
        if (!hasPassenger(p)) return;
        int i = getPassengers().indexOf(p);
        double fwd, side = 0, up = getY() + getPassengersRidingOffset() + p.getMyRidingOffset();
        if (kind() == BIKE) fwd = i == 0 ? 0.05 : -0.55;
        else if (kind() == BOAT) { fwd = i == 0 ? 0.2 : -0.8; side = i == 0 ? -0.35 : 0.35; }
        else { fwd = -0.1; side = i == 0 ? -0.42 : 0.42; }
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        double dx = -Mth.sin(yaw) * fwd + Mth.cos(yaw) * side, dz = Mth.cos(yaw) * fwd + Mth.sin(yaw) * side;
        f.accept(p, getX() + dx, up, getZ() + dz);
        p.setYBodyRot(getYRot());
        float d = Mth.wrapDegrees(p.getYRot() - getYRot());
        float c = Mth.clamp(d, -105, 105);
        p.yRotO += c - d;
        p.setYRot(p.getYRot() + c - d);
        p.setYHeadRot(p.getYRot());
    }

    @Override
    public void onPassengerTurned(Entity p) {
        p.setYBodyRot(getYRot());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity p) {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        Vec3 side = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw)).scale(kind() == BIKE ? 1.0 : 1.6);
        for (Vec3 c : List.of(position().add(side), position().subtract(side), position().add(0, 1.2, 0))) {
            BlockPos b = BlockPos.containing(c);
            if (level().getBlockState(b).getCollisionShape(level(), b).isEmpty() && level().getBlockState(b.above()).getCollisionShape(level(), b.above()).isEmpty())
                return c;
        }
        return super.getDismountLocationForPassenger(p);
    }

    @Override
    public InteractionResult interact(Player pl, InteractionHand hand) {
        if (pl.isSecondaryUseActive()) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (getPassengers().size() >= 2) return InteractionResult.PASS;
        if (pl.startRiding(this)) {
            if (getPassengers().size() == 1) level().playSound(null, blockPosition(), sound("vehicle.start"), SoundSource.NEUTRAL, 1f, kind() == BIKE ? 1.3f : 1f);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    static SoundEvent sound(String id) {
        return SoundEvent.createVariableRangeEvent(new ResourceLocation(FireheartCity.MODID, id));
    }

    @Override
    public boolean hurt(DamageSource src, float amt) {
        if (level().isClientSide || isRemoved()) return true;
        if (!(src.getEntity() instanceof ServerPlayer pl)) return false;
        String on = owner();
        if (!on.isEmpty() && !on.equals(pl.getName().getString()) && !pl.hasPermissions(2)) {
            pl.displayClientMessage(Component.literal("§7That's " + on + "'s " + NAMES[kind()] + "."), true);
            return false;
        }
        long now = level().getGameTime();
        hits = now - lastHit < 20 ? hits + 1 : 1;
        lastHit = now;
        if (hits < 3) {
            pl.displayClientMessage(Component.literal("§7Hit " + (3 - hits) + " more time" + (hits == 2 ? "" : "s") + " to pack up the " + NAMES[kind()]), true);
            return true;
        }
        ejectPassengers();
        ItemStack key = Vehicles.keyFor(kind(), paint());
        if (!pl.getInventory().add(key)) spawnAtLocation(key);
        discard();
        return true;
    }

    public void toggleLights() {
        entityData.set(LIGHTS, !lights());
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpSteps = 10;
    }

    void tickLerp() {
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
        }
        if (lerpSteps > 0) {
            double nx = getX() + (lerpX - getX()) / lerpSteps, ny = getY() + (lerpY - getY()) / lerpSteps, nz = getZ() + (lerpZ - getZ()) / lerpSteps;
            setYRot(getYRot() + (float) Mth.wrapDegrees(lerpYRot - getYRot()) / lerpSteps);
            lerpSteps--;
            setPos(nx, ny, nz);
        }
    }

    public boolean inWater() {
        return level().getFluidState(blockPosition()).is(FluidTags.WATER) || level().getFluidState(blockPosition().below()).is(FluidTags.WATER) && getY() - blockPosition().getY() < 0.3;
    }

    double waterTop() {
        BlockPos b = blockPosition();
        for (int dy = 1; dy >= -2; dy--) {
            BlockPos q = b.offset(0, dy, 0);
            if (level().getFluidState(q).is(FluidTags.WATER) && !level().getFluidState(q.above()).is(FluidTags.WATER)) return q.getY() + level().getFluidState(q).getHeight(level(), q);
        }
        return Double.NaN;
    }

    @Override
    public void tick() {
        prevWheelSpin = wheelSpin;
        prevLean = lean;
        prevPitchVis = pitchVis;
        prevRollVis = rollVis;
        prevBodyY = bodyY;
        float oldYaw = getYRot();
        double ox = getX(), oy = getY(), oz = getZ();
        boolean wasGround = onGround();
        double fallV = getDeltaMovement().y;
        super.tick();
        tickLerp();
        if (crashCd > 0) crashCd--;
        if (isControlledByLocalInstance()) {
            if (level().isClientSide && getControllingPassenger() instanceof Player) VehicleDrive.drive(this);
            physics();
            move(MoverType.SELF, getDeltaMovement());
            if (horizontalCollision) crash();
        } else if (getControllingPassenger() == null && !level().isClientSide) {
            speed *= 0.9f;
            steer *= 0.8f;
            physics();
            move(MoverType.SELF, getDeltaMovement());
        } else if (!level().isClientSide) {
            setDeltaMovement(Vec3.ZERO);
        }
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 mv = new Vec3(getX() - ox, 0, getZ() - oz);
        double moved = mv.length();
        float dir = mv.dot(fwd) < -1e-4 ? -1 : 1;
        if (!level().isClientSide && !isControlledByLocalInstance()) speed = (float) (moved * dir);
        if (level().isClientSide) {
            if (!isControlledByLocalInstance()) {
                float ns = Mth.lerp(0.5f, speed, (float) (moved * dir));
                braking = Math.abs(ns) < Math.abs(speed) - 0.015f && Math.abs(speed) > 0.1f;
                speed = ns;
                yawRate = Mth.wrapDegrees(getYRot() - oldYaw) * Mth.DEG_TO_RAD;
                steer = Mth.lerp(0.3f, steer, Mth.clamp(yawRate * WHEELBASE[kind()] / Math.max(0.05f, Math.abs(speed)) * 1.6f, -1, 1) * Math.signum(speed == 0 ? 1 : speed));
                autoGear();
            }
            wheelSpin += (float) (moved / WHEEL_R[kind()]) * dir;
            steerVis = steer * steerAngle() * Mth.DEG_TO_RAD;
            visuals(oy, wasGround, fallV);
            effects();
        }
        lastSpeed = speed;
        checkInsideBlocks();
        if (!level().isClientSide && Math.abs(speed) > 0.3f && kind() != BOAT) bump();
    }

    float steerAngle() {
        float max = kind() == BIKE ? 30 : kind() == BOAT ? 28 : 36;
        return max / (1 + Math.abs(speed) * 1.5f);
    }

    void autoGear() {
        float[] tops = GEARS[kind()];
        float max = maxSpeed(kind()), abs = Math.abs(speed);
        if (speed < -0.01f) {
            gear = -1;
            rpm = 0.18f + 0.8f * Mth.clamp(abs / (max * 0.3f), 0, 1);
            return;
        }
        if (gear < 1) gear = 1;
        while (gear < tops.length && abs > tops[gear - 1] * max * 0.97f) gear++;
        while (gear > 1 && abs < tops[gear - 2] * max * 0.7f) gear--;
        rpm = Mth.clamp(0.16f + 0.84f * abs / (tops[gear - 1] * max), 0.12f, 1.02f);
    }

    void visuals(double oy, boolean wasGround, double fallV) {
        float acc = speed - lastSpeed;
        float k = kind();
        float targetPitch, targetRoll;
        float lateral = yawRate * speed;
        if (kind() == BOAT) {
            float sp = Math.abs(speed);
            float plane = sp < 0.25f ? sp * 0.5f : sp < 0.6f ? 0.125f + (sp - 0.25f) * 0.35f : Math.max(0.05f, 0.25f - (sp - 0.6f) * 0.4f);
            targetPitch = plane + Mth.clamp(acc * 3, -0.1f, 0.1f);
            targetRoll = Mth.clamp(lateral * 2.5f, -0.3f, 0.3f);
        } else {
            targetPitch = Mth.clamp(acc * (k == BIKE ? 2.2f : 1.6f), -0.08f, 0.08f) + slope;
            if (kind() == BIKE && throttle && gear == 1 && acc > 0.02f && Math.abs(speed) < 0.5f) targetPitch += 0.35f;
            targetRoll = kind() == BIKE ? 0 : Mth.clamp(lateral * 1.8f, -0.09f, 0.09f);
        }
        pitchVis = Mth.lerp(kind() == BIKE && targetPitch > 0.2f ? 0.08f : 0.2f, pitchVis, targetPitch);
        rollVis = Mth.lerp(0.18f, rollVis, targetRoll);
        double dy = getY() - oy;
        if (onGround() && dy > 0.2 && dy < 1.2) bodyY -= (float) dy * 0.75f;
        if (onGround() && !wasGround && fallV < -0.35) {
            bodyY -= (float) Math.min(0.3, -fallV * 0.25);
            level().playLocalSound(getX(), getY(), getZ(), sound("vehicle.land"), SoundSource.NEUTRAL, (float) Math.min(1, -fallV), 1f, false);
            for (int i = 0; i < 8; i++) level().addParticle(ParticleTypes.POOF, getX() + (random.nextDouble() - 0.5) * 2, getY() + 0.1, getZ() + (random.nextDouble() - 0.5) * 2, 0, 0.02, 0);
        }
        bodyY = Mth.lerp(0.25f, bodyY, 0) + (onGround() && Math.abs(speed) > 0.3f && kind() != BOAT ? (random.nextFloat() - 0.5f) * 0.012f * Math.abs(speed) : 0);
        if (kind() == BIKE) lean = Mth.lerp(0.18f, lean, Mth.clamp(-lateral * 3.2f, -0.75f, 0.75f));
        if (Math.abs(steer) > 0.5f && Math.abs(speed) < 0.5f && kind() != BOAT) {
            if (++blink % 10 == 1 && isControlledByLocalInstance()) level().playLocalSound(getX(), getY(), getZ(), sound("vehicle.indicator"), SoundSource.NEUTRAL, 0.35f, 1f, false);
        } else blink = 0;
    }

    void crash() {
        float abs = Math.abs(speed);
        if (abs > 0.5f && crashCd == 0) {
            crashCd = 20;
            level().playLocalSound(getX(), getY(), getZ(), sound("vehicle.crash"), SoundSource.NEUTRAL, Math.min(1.2f, abs), 0.9f + random.nextFloat() * 0.2f, false);
            float yaw = getYRot() * Mth.DEG_TO_RAD;
            Vec3 f = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw)).scale(Math.signum(speed) * 1.4);
            for (int i = 0; i < (int) (abs * 14); i++)
                level().addParticle(ParticleTypes.CRIT, getX() + f.x + (random.nextDouble() - 0.5), getY() + 0.6, getZ() + f.z + (random.nextDouble() - 0.5), -f.x * 0.2, 0.2, -f.z * 0.2);
            level().addParticle(ParticleTypes.EXPLOSION, getX() + f.x, getY() + 0.6, getZ() + f.z, 0, 0, 0);
            pitchVis += -Math.signum(speed) * 0.12f;
            speed = -speed * 0.22f;
        } else speed *= 0.5f;
    }

    void physics() {
        Vec3 v = getDeltaMovement();
        double vy = v.y;
        int k = kind();
        if (k == BOAT) {
            double top = waterTop();
            if (!Double.isNaN(top)) {
                double target = top - 0.25 + Mth.clamp(Math.abs(speed) * 0.2, 0, 0.16) + Mth.sin(tickCount * 0.15f) * 0.02;
                vy += (target - getY()) * 0.2 - vy * 0.3;
            } else vy -= 0.06;
        } else {
            vy = onGround() ? -0.02 : Math.max(-2.5, vy - 0.08);
            slope = groundSlope();
            if (onGround() && getControllingPassenger() != null) speed -= Mth.sin(slope) * 0.03f;
        }
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 flat = new Vec3(v.x, 0, v.z);
        double vf = flat.dot(fwd);
        Vec3 lat = flat.subtract(fwd.scale(vf));
        double grip = k == BOAT ? 0.1 : drifting ? 0.045 : k == BIKE ? 0.6 : 0.45;
        if (!onGround() && k != BOAT) grip = 0.02;
        lat = lat.scale(1 - grip);
        double latLen = lat.length();
        slip = (float) Math.atan2(latLen, Math.abs(speed) + 1e-3);
        if (drifting) speed *= 0.992f;
        Vec3 nv = fwd.scale(speed).add(lat);
        if (!onGround() && k != BOAT) nv = flat.lerp(nv, 0.08);
        setDeltaMovement(nv.x, vy, nv.z);
    }

    float groundSlope() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);
        double half = WHEELBASE[kind()] / 2;
        double hf = groundAt(getX() + fx * half, getZ() + fz * half), hb = groundAt(getX() - fx * half, getZ() - fz * half);
        if (Double.isNaN(hf) || Double.isNaN(hb)) return slope * 0.8f;
        return (float) Math.atan2(hf - hb, half * 2);
    }

    double groundAt(double x, double z) {
        BlockPos b = BlockPos.containing(x, getY() + 1.2, z);
        for (int i = 0; i < 4; i++, b = b.below()) {
            var sh = level().getBlockState(b).getCollisionShape(level(), b);
            if (!sh.isEmpty()) return b.getY() + sh.max(net.minecraft.core.Direction.Axis.Y);
        }
        return Double.NaN;
    }

    public void input(boolean up, boolean down, boolean left, boolean right, boolean brake) {
        int k = kind();
        boolean water = k == BOAT && inWater();
        boolean ground = k == BOAT ? water : onGround();
        float max = maxSpeed(k) * (k == BOAT && !water ? 0.08f : 1);
        float[] tops = GEARS[k];
        throttle = up;
        float abs = Math.abs(speed);
        if (shiftT > 0) shiftT--;
        if (speed < -0.01f) gear = -1;
        else if (gear < 1) gear = 1;
        if (gear >= 1) {
            float hi = tops[gear - 1] * max;
            rpm = Mth.lerp(0.35f, rpm, Mth.clamp(0.16f + 0.84f * abs / hi, 0.12f, 1.04f) + (up && abs < 0.05f ? 0.25f : 0));
            if (rpm > 0.94f && gear < tops.length && up && shiftT == 0) {
                gear++;
                shiftT = k == BIKE ? 3 : 5;
                level().playLocalSound(getX(), getY(), getZ(), sound("vehicle.shift"), SoundSource.NEUTRAL, 0.5f, 1f, false);
                if (gear >= 3 && random.nextFloat() < 0.45f) backfire();
            } else if (gear > 1 && abs < tops[gear - 2] * max * 0.68f) gear--;
        } else rpm = Mth.lerp(0.3f, rpm, 0.18f + 0.8f * Mth.clamp(abs / (max * 0.3f), 0, 1));
        braking = false;
        reversing = speed < -0.01f;
        if (!ground) {
            speed *= 0.995f;
        } else if (up && speed >= -0.02f) {
            if (shiftT == 0) {
                float torque = 0.72f + 0.5f * Mth.sin(Mth.PI * Mth.clamp(rpm, 0, 1));
                float ratio = (float) Math.sqrt(tops[tops.length - 1] / tops[Math.max(0, gear - 1)]);
                float base = k == BIKE ? 0.0145f : k == BOAT ? 0.011f : 0.0125f;
                speed += base * torque * ratio;
            }
            reverseHold = 0;
        } else if (up) {
            speed = Math.min(0, speed + 0.05f);
            braking = true;
        } else if (down && speed > 0.02f) {
            speed = Math.max(0, speed - (k == BOAT ? 0.025f : 0.055f));
            braking = true;
            reverseHold = 0;
        } else if (down) {
            if (++reverseHold > 5) speed = Math.max(-max * 0.3f, speed - 0.012f);
        } else {
            reverseHold = 0;
            speed -= Math.signum(speed) * Math.min(Math.abs(speed), k == BOAT ? 0.006f : 0.004f + abs * 0.004f);
        }
        float drag = k == BIKE ? 0.0048f : k == BOAT ? 0.0085f : 0.0058f;
        speed -= drag * speed * Math.abs(speed);
        drifting = false;
        if (brake) {
            if (abs > 0.4f && k != BOAT && ground) drifting = true;
            speed -= Math.signum(speed) * Math.min(Math.abs(speed), drifting ? 0.012f : 0.03f);
            braking = true;
        }
        if (Math.abs(speed) < 0.003f && !up && !down) speed = 0;
        if (speed > max) speed = Mth.lerp(0.2f, speed, max);
        if (speed < -max * 0.3f) speed = -max * 0.3f;
        float target = (left ? -1 : 0) + (right ? 1 : 0);
        float rate = target == 0 || Math.signum(target) != Math.signum(steer) && steer != 0 ? 0.2f : 0.12f;
        steer += Mth.clamp(target - steer, -rate, rate);
        if (ground || k == BOAT) {
            float ang = steer * steerAngle() * Mth.DEG_TO_RAD;
            float wr = k == BOAT ? Math.min(1, Math.abs(speed) / 0.2f) * 0.8f : 1;
            float yr = speed / WHEELBASE[k] * (float) Math.tan(ang) * wr / (1 + speed * speed * 0.35f);
            if (drifting) yr *= 1.55f;
            if (k == BOAT && water) yr *= 1.2f;
            yawRate = yr;
            float d = yr * Mth.RAD_TO_DEG;
            setYRot(getYRot() + d);
            yRotO = getYRot() - d;
        } else yawRate *= 0.9f;
    }

    void backfire() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        Vec3 p = position().add(new Vec3(Mth.sin(yaw), 0, -Mth.cos(yaw)).scale(kind() == BIKE ? 1.1 : 1.75)).add(kind() == BIKE ? 0.3 : 0.35, 0.35, 0);
        level().playLocalSound(p.x, p.y, p.z, sound("vehicle.backfire"), SoundSource.NEUTRAL, 0.7f, 0.9f + random.nextFloat() * 0.3f, false);
        for (int i = 0; i < 4; i++) level().addParticle(ParticleTypes.FLAME, p.x, p.y, p.z, Mth.sin(yaw) * 0.08, 0.01, -Mth.cos(yaw) * 0.08);
        level().addParticle(ParticleTypes.LARGE_SMOKE, p.x, p.y, p.z, 0, 0.02, 0);
    }

    void effects() {
        double sp = Math.abs(speed);
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        Vec3 back = new Vec3(Mth.sin(yaw), 0, -Mth.cos(yaw));
        Vec3 right = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
        if (kind() == BOAT) {
            if (inWater() && sp > 0.12) {
                Vec3 p = position().add(back.scale(2.2));
                for (int i = 0; i < (int) (sp * 8); i++)
                    level().addParticle(ParticleTypes.SPLASH, p.x + (random.nextDouble() - 0.5) * 1.4, getY() + 0.3, p.z + (random.nextDouble() - 0.5) * 1.4, back.x * 0.25, 0.18, back.z * 0.25);
                for (int s = -1; s <= 1; s += 2) {
                    Vec3 bow = position().add(back.scale(-1.6)).add(right.scale(s * 1.0));
                    if (random.nextFloat() < sp) level().addParticle(ParticleTypes.SPLASH, bow.x, getY() + 0.35, bow.z, right.x * s * 0.2, 0.2, right.z * s * 0.2);
                    Vec3 wake = position().add(back.scale(3.5 + random.nextDouble() * 3)).add(right.scale(s * (1.2 + random.nextDouble() * 1.5)));
                    if (random.nextFloat() < sp * 0.8) level().addParticle(ParticleTypes.BUBBLE_POP, wake.x, getY() + 0.25, wake.z, 0, 0.02, 0);
                }
                if (random.nextFloat() < sp * 0.5) level().addParticle(ParticleTypes.CLOUD, p.x, getY() + 0.5, p.z, back.x * 0.05, 0.02, back.z * 0.05);
            }
            return;
        }
        Vec3 ex = position().add(back.scale(kind() == BIKE ? 1.1 : 1.75)).add(right.scale(kind() == BIKE ? 0.3 : 0.35)).add(0, 0.35, 0);
        if (random.nextFloat() < 0.2f + rpm * 0.5f) level().addParticle(ParticleTypes.SMOKE, ex.x, ex.y, ex.z, back.x * 0.03, 0.01, back.z * 0.03);
        boolean skid = onGround() && ((drifting || slip > 0.25f) && sp > 0.3 || braking && sp > 0.6 || throttle && gear == 1 && rpm > 0.7f && sp < 0.35f);
        if (skid) {
            double wb = WHEELBASE[kind()] / 2;
            for (int s = kind() == BIKE ? 0 : -1; s <= 1; s += 2) {
                Vec3 w = position().add(back.scale(wb)).add(right.scale(kind() == BIKE ? 0 : s * 0.8));
                level().addParticle(ParticleTypes.CLOUD, w.x, getY() + 0.15, w.z, (random.nextDouble() - 0.5) * 0.05, 0.03, (random.nextDouble() - 0.5) * 0.05);
                if (random.nextFloat() < 0.4f) level().addParticle(ParticleTypes.LARGE_SMOKE, w.x, getY() + 0.2, w.z, 0, 0.02, 0);
            }
        }
        skidding = skid;
        if (onGround() && sp > 0.5 && random.nextFloat() < sp * 0.3) {
            BlockPos under = blockPosition().below();
            var st = level().getBlockState(under);
            if (!st.isAir()) {
                Vec3 w = position().add(back.scale(WHEELBASE[kind()] / 2)).add(right.scale((random.nextBoolean() ? 1 : -1) * (kind() == BIKE ? 0 : 0.8)));
                level().addParticle(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, st), w.x, getY() + 0.1, w.z, back.x * 0.5, 0.2, back.z * 0.5);
            }
        }
    }

    public boolean skidding;

    void bump() {
        AABB box = getBoundingBox().inflate(0.3);
        for (Entity e : level().getEntities(this, box, e -> e instanceof LivingEntity && !hasPassenger(e) && !(e instanceof Player p && p.isSpectator()))) {
            Vec3 push = e.position().subtract(position()).multiply(1, 0, 1).normalize().scale(0.5 + Math.abs(speed) * 0.6);
            e.push(push.x, 0.25, push.z);
            e.hurtMarked = true;
        }
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    @Override
    public ItemStack getPickResult() {
        return Vehicles.keyFor(kind(), paint());
    }
}
