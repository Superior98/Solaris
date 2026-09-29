package com.fireheart.city;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The residents' own airship, the Fireheart Sky Ferry. It shuttles between the city ferry pad and the
 * Neon Heights ferry terminal. Residents walk up, take a seat and ride along in plain sight; Jet flies it
 * during his shift, otherwise it runs on autopilot. Players can hop on too.
 */
public class Ferry extends Entity {
    public static final int CITY = 0, ISLE = 1, FLYING = 2;
    public static final Vec3 CITY_DOCK = new Vec3(18.5, 71.0, 86.5);
    public static final Vec3 ISLE_DOCK = new Vec3(24.5, 181.0, 234.5);
    public static final BlockPos CITY_PAD = new BlockPos(15, 71, 86);
    public static final BlockPos ISLE_PAD = new BlockPos(20, 181, 234);
    public static final int SEATS = 8;
    public static final double[][] SEAT = {
            {0, 3}, {-1, 2}, {1, 2}, {-1, 0}, {1, 0}, {-1, -2}, {1, -2}, {-1, -4}, {1, -4}};
    private static final TicketType<ChunkPos> TICKET = TicketType.create("fireheartcity_ferry", Comparator.comparingLong(ChunkPos::toLong), 60);
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(Ferry.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> THRUST = SynchedEntityData.defineId(Ferry.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<java.util.Optional<UUID>> PILOT = SynchedEntityData.defineId(Ferry.class, EntityDataSerializers.OPTIONAL_UUID);

    private int side = CITY;
    private int target = ISLE;
    private int leg;
    private int dockTicks;
    private int lastBoard;
    private double speed;
    private long callCity = -1, callIsle = -1;
    private UUID pilot;
    private int lerpSteps;
    private boolean grounded;
    private double lx, ly, lz, lyaw;
    public static boolean debug;
    public static boolean strict;
    private static UUID known;

    public Ferry(EntityType<? extends Ferry> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.blocksBuilding = false;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(STATE, CITY);
        entityData.define(THRUST, 0f);
        entityData.define(PILOT, java.util.Optional.empty());
    }

    public int state() {
        return entityData.get(STATE);
    }

    public float thrust() {
        return entityData.get(THRUST);
    }

    public int dockedAt() {
        int s = state();
        return s == FLYING ? -1 : s;
    }

    public static Vec3 dock(int side) {
        return side == ISLE ? ISLE_DOCK : CITY_DOCK;
    }

    public static BlockPos pad(int side) {
        return side == ISLE ? ISLE_PAD : CITY_PAD;
    }

    public static Ferry find(ServerLevel sl) {
        if (known != null && sl.getEntity(known) instanceof Ferry f && f.isAlive()) return f;
        UUID id = CityData.get(sl).ferryId;
        if (id != null && sl.getEntity(id) instanceof Ferry f && f.isAlive()) {
            known = id;
            return f;
        }
        return null;
    }

    private static long wantAt = -10000;
    private static int wantSide;

    public static void call(ServerLevel sl, int fromSide) {
        Ferry f = find(sl);
        long now = sl.getGameTime();
        if (f == null) {
            wantAt = now;
            wantSide = fromSide;
            return;
        }
        if (fromSide == ISLE) { if (f.callIsle < 0) f.callIsle = now; } else if (f.callCity < 0) f.callCity = now;
    }

    public boolean hasSeat() {
        return getPassengers().size() < SEATS + 1 && dockedAt() >= 0 && dockTicks > 20;
    }

    public boolean isPilot(Entity e) {
        UUID p = level().isClientSide ? entityData.get(PILOT).orElse(null) : pilot;
        return p != null && e.getUUID().equals(p);
    }

    private void setPilot(UUID id) {
        pilot = id;
        entityData.set(PILOT, java.util.Optional.ofNullable(id));
    }

    public boolean board(Entity e, boolean asPilot) {
        if (level().isClientSide || !hasSeat() || e.isPassenger()) return false;
        if (asPilot) setPilot(e.getUUID());
        if (!e.startRiding(this, true)) {
            if (asPilot) setPilot(null);
            return false;
        }
        lastBoard = tickCount;
        playSound(SoundEvents.WOOD_STEP, 0.8f, 1.1f);
        return true;
    }

    @Override
    protected boolean canAddPassenger(Entity e) {
        return getPassengers().size() < SEATS + 1;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(8, 10, 8);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 256 * 256;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (dockedAt() < 0) {
            player.displayClientMessage(Component.literal("§bThe Sky Ferry is in the air - wait for it to land."), true);
            return InteractionResult.CONSUME;
        }
        if (!player.startRiding(this)) {
            player.displayClientMessage(Component.literal("§cThe Sky Ferry is full - next one's soon!"), true);
            return InteractionResult.CONSUME;
        }
        lastBoard = tickCount;
        player.displayClientMessage(Component.literal("§b§lSolaris Sky Ferry §r§7- next stop " + (dockedAt() == CITY ? "Neon Heights" : "Solaris") + ". Sneak to get off."), true);
        return InteractionResult.CONSUME;
    }

    private int seatOf(Entity e) {
        List<Entity> ps = getPassengers();
        if (isPilot(e)) return 0;
        int i = 1;
        for (Entity p : ps) {
            if (isPilot(p)) continue;
            if (p == e) return Math.min(i, SEATS);
            i++;
        }
        return 1;
    }

    public Vec3 local(double x, double y, double z) {
        float r = getYRot() * Mth.DEG_TO_RAD;
        double c = Mth.cos(r), s = Mth.sin(r);
        return new Vec3(getX() + x * c - z * s, getY() + y, getZ() + x * s + z * c);
    }

    @Override
    protected void positionRider(Entity e, Entity.MoveFunction move) {
        if (!hasPassenger(e)) return;
        double[] s = SEAT[seatOf(e)];
        double feet = 1.65;
        Vec3 p = local(s[0], 0, s[1]);
        double sway = Math.sin((tickCount + e.getId() * 7) * 0.08) * 0.02 * thrust();
        move.accept(e, p.x, getY() + feet + sway, p.z);
        if (e instanceof LivingEntity le && !(e instanceof Player)) {
            le.setYBodyRot(getYRot());
            le.yBodyRotO = getYRot();
            if (isPilot(e)) {
                le.setYHeadRot(getYRot());
                le.setYRot(getYRot());
            }
        }
    }

    @Override
    public void onPassengerTurned(Entity e) {
        if (e instanceof Player) return;
        e.setYBodyRot(getYRot());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity e) {
        int s = dockedAt() < 0 ? side : dockedAt();
        Vec3 d = dock(s);
        int i = Math.floorMod(e.getId(), 4);
        double off = -1.5 + i;
        return new Vec3(d.x - 4.5, d.y, d.z + off);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {
        lx = x;
        ly = y;
        lz = z;
        lyaw = yaw;
        lerpSteps = 3;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (lerpSteps > 0) {
                double nx = getX() + (lx - getX()) / lerpSteps;
                double ny = getY() + (ly - getY()) / lerpSteps;
                double nz = getZ() + (lz - getZ()) / lerpSteps;
                float ny2 = (float) (getYRot() + Mth.wrapDegrees(lyaw - getYRot()) / lerpSteps);
                lerpSteps--;
                setPos(nx, ny, nz);
                setYRot(ny2);
            }
            if (thrust() > 0.05f && tickCount % 2 == 0) {
                Vec3 t = local(0, 1.2, -6.2);
                level().addParticle(ParticleTypes.CLOUD, t.x, t.y, t.z, 0, 0.02, 0);
                if (tickCount % 6 == 0) {
                    Vec3 a = local(-2.6, 5.5, -3.5), b = local(2.6, 5.5, -3.5);
                    level().addParticle(ParticleTypes.END_ROD, a.x, a.y, a.z, 0, -0.01, 0);
                    level().addParticle(ParticleTypes.END_ROD, b.x, b.y, b.z, 0, -0.01, 0);
                }
            }
            return;
        }
        serverTick((ServerLevel) level());
    }

    private void serverTick(ServerLevel sl) {
        CityData d = CityData.get(sl);
        if (d.ferryId == null || !d.ferryId.equals(getUUID())) {
            if (d.ferryId != null && tickCount > 40 && sl.getEntity(d.ferryId) instanceof Ferry other && other != this) {
                ejectAll();
                discard();
                return;
            }
            d.ferryId = getUUID();
            d.setDirty();
        }
        known = getUUID();
        if (tickCount % 20 == 0) {
            d.ferryPos = blockPosition();
            d.ferryState = state();
            d.setDirty();
        }
        if (state() == FLYING || !getPassengers().isEmpty() || callCity >= 0 || callIsle >= 0) keepLoaded(sl);
        Entity pe = pilot == null ? null : sl.getEntity(pilot);
        if (pilot != null && (pe == null || pe.getVehicle() != this) && tickCount % 20 == 0) setPilot(null);
        if (state() == FLYING) fly(sl);
        else docked(sl, d);
    }

    private void keepLoaded(ServerLevel sl) {
        if (tickCount % 20 != 0) return;
        ticketHere(sl);
    }

    void ticketHere(ServerLevel sl) {
        ChunkPos cp = new ChunkPos(blockPosition());
        sl.getChunkSource().addRegionTicket(TICKET, cp, 3, cp);
        Vec3 ahead = position().add(getDeltaMovement().scale(40));
        ChunkPos ap = new ChunkPos(BlockPos.containing(ahead));
        if (!ap.equals(cp)) sl.getChunkSource().addRegionTicket(TICKET, ap, 3, ap);
    }

    private void docked(ServerLevel sl, CityData d) {
        side = state();
        Vec3 home = dock(side);
        if (position().distanceToSqr(home) > 0.01) setPos(home.x, home.y, home.z);
        setDeltaMovement(Vec3.ZERO);
        float want = side == CITY ? 0f : 180f;
        setYRot(approachAngle(getYRot(), want, 4f));
        entityData.set(THRUST, 0f);
        dockTicks++;
        if (side == CITY) callCity = -1; else callIsle = -1;
        List<Entity> riders = new ArrayList<>(getPassengers());
        int travellers = 0;
        for (Entity e : riders) if (!isPilot(e)) travellers++;
        long pending = side == CITY ? callIsle : callCity;
        if (sl.isThundering()) {
            if (!grounded) {
                grounded = true;
                Entity gp = pilotEntity(sl);
                if (gp instanceof Resident r) r.sayTo("Sorry folks - we're grounded until this storm passes!", 90);
                for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(30))) p.displayClientMessage(Component.literal("§cSky Ferry grounded - thunderstorm"), true);
            }
            return;
        }
        if (grounded) {
            grounded = false;
            Entity gp = pilotEntity(sl);
            if (gp instanceof Resident r) r.sayTo("Storm's over - Sky Ferry is flying again!", 80);
        }
        boolean boardingSoon = tickCount - lastBoard < 120;
        boolean walkingUp = residentsHeadingHere(sl) && dockTicks < 600;
        if (travellers > 0 && !boardingSoon && dockTicks > 100 && (!walkingUp || dockTicks > 400)) depart(sl);
        else if (travellers == 0 && pending >= 0 && dockTicks > 100 && !walkingUp && sl.getGameTime() - pending > 40) depart(sl);
        else if (travellers == 0 && pilot != null && dockTicks > 1500 && !walkingUp) depart(sl);
        else if (dockTicks == 30 && travellers == 0 && pilotEntity(sl) != null && pilotEntity(sl) instanceof Resident r) r.sayTo(r.pick("Sky Ferry now boarding!", "All aboard for " + (side == CITY ? "Neon Heights" : "Solaris") + "!"), 60);
    }

    private Entity pilotEntity(ServerLevel sl) {
        return pilot == null ? null : sl.getEntity(pilot);
    }

    private boolean residentsHeadingHere(ServerLevel sl) {
        BlockPos p = pad(side);
        for (Resident r : sl.getEntitiesOfClass(Resident.class, new AABB(p).inflate(14, 6, 14), r -> !r.isPassenger())) {
            if (r.wantsFerry()) return true;
        }
        return false;
    }

    private void depart(ServerLevel sl) {
        target = side == CITY ? ISLE : CITY;
        leg = 0;
        speed = 0;
        dockTicks = 0;
        entityData.set(STATE, FLYING);
        if (target == ISLE) callIsle = -1; else callCity = -1;
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL, 1.2f, 1.4f);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 0.8f, 1.8f);
        Entity pe = pilotEntity(sl);
        if (pe instanceof Resident r) r.sayTo(r.pick("Sky Ferry departing for " + (target == ISLE ? "Neon Heights" : "Solaris") + " - hold on to your hats!", "Cleared for take-off! Next stop, " + (target == ISLE ? "Neon Heights" : "the city") + ".", "Here we go! Enjoy the view, everyone."), 80);
        else for (Entity e : getPassengers()) if (e instanceof Resident r && sl.random.nextFloat() < 0.5f) { r.sayTo(r.pick("Here we go!", "Whee, we're off!", "I love this part."), 60); break; }
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(24))) p.displayClientMessage(Component.literal("§b§lSky Ferry §r§7departing for " + (target == ISLE ? "Neon Heights" : "Solaris")), true);
        if (debug) FireheartCity.LOG.info("[Ferry] depart " + (target == ISLE ? "up" : "down") + " with " + getPassengers().size() + " aboard");
    }

    private List<Vec3> route() {
        Vec3 a = dock(side), b = dock(target);
        List<Vec3> w = new ArrayList<>();
        w.add(a.add(0, 14, 0));
        if (target == ISLE) {
            w.add(new Vec3(b.x, b.y + 15, b.z - 16));
            w.add(new Vec3(b.x, b.y + 12, b.z));
        } else {
            w.add(new Vec3(a.x, a.y + 16, a.z - 16));
            w.add(new Vec3(b.x, b.y + 32, b.z + 40));
            w.add(new Vec3(b.x, b.y + 14, b.z));
        }
        w.add(b);
        return w;
    }

    private void fly(ServerLevel sl) {
        List<Vec3> w = route();
        if (leg >= w.size()) {
            arrive(sl);
            return;
        }
        Vec3 goal = w.get(leg);
        Vec3 to = goal.subtract(position());
        double dist = to.length();
        boolean last = leg == w.size() - 1;
        boolean vertical = Math.abs(to.y) > Math.sqrt(to.x * to.x + to.z * to.z) * 2.5;
        double cruise = vertical ? 0.28 : 0.62;
        double brake = last ? Math.max(0.06, Math.min(cruise, dist * 0.12)) : cruise;
        speed = speed < brake ? Math.min(brake, speed + 0.012) : Math.max(brake, speed - 0.03);
        entityData.set(THRUST, (float) Math.min(1.0, speed / 0.5));
        if (dist <= Math.max(speed, 0.08)) {
            setPos(goal.x, goal.y, goal.z);
            leg++;
            if (last) arrive(sl);
            return;
        }
        Vec3 step = to.scale(speed / dist);
        setDeltaMovement(step);
        setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
        double hx = to.x, hz = to.z;
        float want = getYRot();
        if (!vertical && hx * hx + hz * hz > 1) want = (float) (Mth.atan2(hz, hx) * Mth.RAD_TO_DEG) - 90f;
        if (last || leg == w.size() - 2 && to.horizontalDistance() < 3) want = target == CITY ? 0f : 180f;
        setYRot(approachAngle(getYRot(), want, 2.5f));
        if (tickCount % 40 == 0) sl.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_AMBIENT, SoundSource.NEUTRAL, 0.9f, 1.5f);
        if (tickCount % 20 == 0) sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() - 0.5, getZ(), 2, 0.8, 0.1, 0.8, 0.01);
        if (tickCount % 200 == 0) {
            for (Entity e : getPassengers()) {
                if (e instanceof Resident r && !isPilot(e) && sl.random.nextFloat() < 0.35f) {
                    r.ferryChatter(target == ISLE);
                    break;
                }
            }
        }
    }

    private void arrive(ServerLevel sl) {
        side = target;
        entityData.set(STATE, side);
        Vec3 d = dock(side);
        setPos(d.x, d.y, d.z);
        setDeltaMovement(Vec3.ZERO);
        entityData.set(THRUST, 0f);
        dockTicks = 0;
        speed = 0;
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.NEUTRAL, 0.8f, 1.6f);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL, 1.0f, 1.2f);
        Entity pe = pilotEntity(sl);
        if (pe instanceof Resident r) r.sayTo(side == ISLE ? r.pick("Welcome to Neon Heights! Mind the gap.", "Neon Heights, everybody off!") : r.pick("Solaris! Thanks for flying with me.", "Back on solid ground, folks!"), 80);
        int i = 0;
        for (Entity e : new ArrayList<>(getPassengers())) {
            if (isPilot(e)) continue;
            e.stopRiding();
            Vec3 out = new Vec3(d.x - 4.5, d.y, d.z - 1.5 + (i++ % 4));
            e.teleportTo(out.x, out.y, out.z);
            if (e instanceof Resident r) r.leftFerry(side == ISLE);
            if (e instanceof ServerPlayer p) p.displayClientMessage(Component.literal("§b§lSky Ferry §r§7- welcome to " + (side == ISLE ? "Neon Heights" : "Solaris") + "!"), true);
        }
        if (debug) FireheartCity.LOG.info("[Ferry] arrived at " + (side == ISLE ? "island" : "city"));
    }

    public void ejectAll() {
        for (Entity e : new ArrayList<>(getPassengers())) e.stopRiding();
    }

    public void pilotOff(Entity e) {
        if (isPilot(e)) {
            setPilot(null);
            e.stopRiding();
            Vec3 d = dock(dockedAt() < 0 ? side : dockedAt());
            e.teleportTo(d.x - 4.5, d.y, d.z + 2.5);
        }
    }

    public boolean grounded() {
        return grounded || level().isThundering() && dockedAt() >= 0;
    }

    public boolean hasPilot() {
        return pilot != null;
    }

    public int targetSide() {
        return target;
    }

    public int waitingAt(ServerLevel sl, int s) {
        return sl.getEntitiesOfClass(Resident.class, new AABB(pad(s)).inflate(8, 4, 8), r -> !r.isPassenger() && r.wantsFerry()).size();
    }

    public int etaSeconds() {
        if (state() != FLYING) return 0;
        List<Vec3> w = route();
        double dist = 0;
        Vec3 at = position();
        for (int i = Math.max(0, leg); i < w.size(); i++) {
            dist += at.distanceTo(w.get(i));
            at = w.get(i);
        }
        return (int) Math.ceil(dist / 10.0);
    }

    public String describe() {
        int st = state();
        String where = switch (st) {
            case CITY -> "docked at the city ferry pad";
            case ISLE -> "docked at the Neon Heights terminal";
            default -> "flying to " + (target == ISLE ? "Neon Heights" : "the city") + " (leg " + leg + ")";
        };
        int n = getPassengers().size();
        return "Sky Ferry: " + where + " | " + n + " aboard" + (pilot != null ? " incl. pilot" : "") + " | calls city=" + callCity + " isle=" + callIsle + " | at " + blockPosition().toShortString();
    }

    public void sendTo(int s) {
        if (state() == FLYING) return;
        side = state();
        if (s == side) return;
        depart((ServerLevel) level());
    }

    public void snap(int s) {
        ejectAll();
        side = s;
        target = s;
        entityData.set(STATE, s);
        Vec3 d = dock(s);
        setPos(d.x, d.y, d.z);
        dockTicks = 0;
    }

    private static float approachAngle(float cur, float want, float step) {
        float diff = Mth.wrapDegrees(want - cur);
        if (Math.abs(diff) <= step) return want;
        return cur + Math.signum(diff) * step;
    }

    public static void tickManager(ServerLevel sl) {
        long now = sl.getGameTime();
        if (now % 20 == 3) {
            Ferry lf = find(sl);
            if (lf != null && (lf.state() == FLYING || !lf.getPassengers().isEmpty() || lf.callCity >= 0 || lf.callIsle >= 0)) lf.ticketHere(sl);
        }
        if (now % 100 != 17) return;
        CityData d = CityData.get(sl);
        if (d.profiles.isEmpty()) return;
        Ferry f = find(sl);
        if (f != null) {
            d.ferryMissing = 0;
            if (now - wantAt < 400) {
                call(sl, wantSide);
                wantAt = -10000;
            }
            return;
        }
        if (d.ferryId != null && d.ferryPos != null) {
            if (now - wantAt > 600 && d.ferryState != FLYING) return;
            ChunkPos cp = new ChunkPos(d.ferryPos);
            sl.getChunkSource().addRegionTicket(TICKET, cp, 3, cp);
            if (sl.isPositionEntityTicking(d.ferryPos)) {
                d.ferryMissing++;
                if (d.ferryMissing < 3) return;
            } else return;
        }
        BlockPos c = BlockPos.containing(CITY_DOCK);
        if (!sl.isPositionEntityTicking(c)) return;
        Ferry nf = FireheartCity.FERRY.get().create(sl);
        if (nf == null) return;
        nf.setPos(CITY_DOCK.x, CITY_DOCK.y, CITY_DOCK.z);
        nf.setYRot(0f);
        nf.setCustomName(Component.literal("Solaris Sky Ferry"));
        sl.addFreshEntity(nf);
        d.ferryId = nf.getUUID();
        d.ferryPos = c;
        d.ferryState = CITY;
        d.ferryMissing = 0;
        d.setDirty();
        known = nf.getUUID();
        FireheartCity.LOG.info("Solaris Sky Ferry launched at the city pad");
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        side = t.getInt("Side");
        target = t.getInt("Target");
        leg = t.getInt("Leg");
        entityData.set(STATE, t.contains("State") ? t.getInt("State") : side);
        if (t.hasUUID("Pilot")) setPilot(t.getUUID("Pilot"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        t.putInt("Side", side);
        t.putInt("Target", target);
        t.putInt("Leg", leg);
        t.putInt("State", state());
        if (pilot != null) t.putUUID("Pilot", pilot);
    }
}
