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
    public boolean braking;
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
        return kind == BIKE ? 1.3f : kind == BOAT ? 0.9f : 1.1f;
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
        float oldYaw = getYRot();
        super.tick();
        tickLerp();
        if (isControlledByLocalInstance()) {
            if (level().isClientSide && getControllingPassenger() instanceof Player) VehicleDrive.drive(this);
            physics();
            move(MoverType.SELF, getDeltaMovement());
        } else if (getControllingPassenger() == null && !level().isClientSide) {
            speed *= 0.9f;
            physics();
            move(MoverType.SELF, getDeltaMovement());
        } else if (!level().isClientSide) {
            setDeltaMovement(Vec3.ZERO);
        }
        if (level().isClientSide) {
            double moved = new Vec3(getX() - xo, 0, getZ() - zo).length();
            float dir = speed != 0 ? Math.signum(speed) : 1;
            if (!isControlledByLocalInstance()) speed = (float) moved * dir;
            wheelSpin += (float) moved * 2.2f * dir;
            float yawRate = Mth.wrapDegrees(getYRot() - oldYaw);
            steerVis = Mth.lerp(0.3f, steerVis, Mth.clamp(yawRate * 0.12f, -0.55f, 0.55f));
            if (kind() == BIKE) lean = Mth.lerp(0.2f, lean, Mth.clamp(-yawRate * 0.09f * Math.min(1, Math.abs(speed) * 2), -0.6f, 0.6f));
            effects();
        }
        checkInsideBlocks();
        if (!level().isClientSide && Math.abs(speed) > 0.35f && kind() != BOAT) bump();
    }

    void physics() {
        Vec3 v = getDeltaMovement();
        double vy = v.y;
        if (kind() == BOAT) {
            double top = waterTop();
            if (!Double.isNaN(top)) {
                double target = top - 0.25 + (Math.abs(speed) > 0.5f ? 0.15 : 0);
                vy += (target - getY()) * 0.2 - vy * 0.3;
            } else vy -= 0.06;
        } else {
            vy = onGround() ? -0.02 : Math.max(-2.5, vy - 0.08);
        }
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 flat = new Vec3(v.x, 0, v.z);
        Vec3 want = fwd.scale(speed);
        double grip = braking ? 0.18 : kind() == BOAT ? 0.35 : 0.75;
        Vec3 nv = flat.lerp(want, grip);
        setDeltaMovement(nv.x, vy, nv.z);
        if (horizontalCollision) speed *= 0.5f;
    }

    public void input(boolean up, boolean down, boolean left, boolean right, boolean brake) {
        int k = kind();
        boolean water = k == BOAT && inWater();
        float max = maxSpeed(k) * (k == BOAT && !water ? 0.08f : 1);
        float accel = k == BIKE ? 0.042f : k == BOAT ? 0.03f : 0.034f;
        braking = brake && Math.abs(speed) > 0.2f;
        if (up) speed = speed < 0 ? speed + 0.08f : Math.min(max, speed + accel * (1 - speed / (max * 1.05f)));
        else if (down) speed = speed > 0.02f ? speed - 0.07f : Math.max(-max * 0.3f, speed - 0.02f);
        else speed *= k == BOAT ? 0.985f : 0.975f;
        if (brake) speed *= k == BOAT ? 0.96f : 0.92f;
        if (Math.abs(speed) < 0.003f) speed = 0;
        if (Math.abs(speed) > max) speed = Math.signum(speed) * max;
        float steer = (left ? -1 : 0) + (right ? 1 : 0);
        float grip = Math.min(1f, Math.abs(speed) / 0.25f);
        float rate = (k == BIKE ? 4.6f : k == BOAT ? 3.0f : 3.8f) * grip * (1 - Math.abs(speed) / (max * 2.4f)) * (brake ? 1.6f : 1f);
        if (steer != 0) {
            float d = steer * rate * (speed < 0 ? -1 : 1);
            setYRot(getYRot() + d);
            yRotO = getYRot() - d;
        }
    }

    void effects() {
        double sp = Math.abs(speed);
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        Vec3 back = new Vec3(Mth.sin(yaw), 0, -Mth.cos(yaw));
        if (kind() == BOAT) {
            if (inWater() && sp > 0.15) {
                Vec3 p = position().add(back.scale(2.1));
                for (int i = 0; i < (int) (sp * 6); i++)
                    level().addParticle(ParticleTypes.SPLASH, p.x + (random.nextDouble() - 0.5) * 1.2, getY() + 0.3, p.z + (random.nextDouble() - 0.5) * 1.2, back.x * 0.2, 0.15, back.z * 0.2);
                if (random.nextFloat() < sp) level().addParticle(ParticleTypes.BUBBLE_POP, p.x, getY() + 0.2, p.z, 0, 0.05, 0);
            }
            return;
        }
        if (sp > 0.05 && random.nextFloat() < 0.3f) {
            Vec3 p = position().add(back.scale(kind() == BIKE ? 1.0 : 1.7)).add(0, 0.35, 0);
            level().addParticle(ParticleTypes.SMOKE, p.x, p.y, p.z, back.x * 0.02, 0.01, back.z * 0.02);
        }
        if (braking && sp > 0.4 && onGround()) {
            for (int i = 0; i < 2; i++) level().addParticle(ParticleTypes.CLOUD, getX() + (random.nextDouble() - 0.5) * 1.6, getY() + 0.1, getZ() + (random.nextDouble() - 0.5) * 1.6, 0, 0.02, 0);
        }
    }

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
