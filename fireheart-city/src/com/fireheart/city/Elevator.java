package com.fireheart.city;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public final class Elevator {
    public static final int[] FLOORS = {70, 74, 78, 82, 86, 90, 94};
    public static final String[] FLOOR_NAMES = {"the lobby", "floor 1", "floor 2", "floor 3", "floor 4", "floor 5", "the roof"};
    static final BlockPos PULLEY = new BlockPos(36, 99, 24);
    static final AABB SHAFT = new AABB(34, 60, 22, 39, 103, 27);
    static final ResourceLocation CONTRAPTION = new ResourceLocation("create", "stationary_contraption");

    enum Phase { CALL, BOARD, RIDE, EXIT, WALKOFF, TELEPORT }

    static final class Req {
        final UUID id;
        int from;
        int to;
        long touched;
        final long since;

        Req(UUID id, int from, int to, long now) {
            this.id = id;
            this.from = from;
            this.to = to;
            this.touched = now;
            this.since = now;
        }
    }

    private static final LinkedList<Req> QUEUE = new LinkedList<>();
    private static Req cur;
    private static Phase phase;
    private static int phaseTicks;
    private static int gap;
    private static Entity cabin;
    private static double lastMinY = Double.NaN;
    private static int still;
    private static int missing;
    private static long lastRepair = -100000;
    private static int failedRepairs;
    private static boolean announcedBroken;
    public static String lastInfo = "";

    private Elevator() {}

    public static void reset() {
        QUEUE.clear();
        cur = null;
        phase = null;
        cabin = null;
        missing = 0;
        failedRepairs = 0;
        announcedBroken = false;
    }

    public static boolean inTower(double x, double y, double z) {
        return x >= 18 && x < 39 && z >= 16 && z < 33 && y >= 69.5 && y < 99.5;
    }

    public static int floorOf(double y) {
        int f = (int) Math.floor((y - 70.5) / 4.0);
        return Math.max(0, Math.min(FLOORS.length - 1, f));
    }

    public static int floorOfEntity(Entity e) {
        return inTower(e.getX(), e.getY(), e.getZ()) ? floorOf(e.getY()) : -1;
    }

    public static int floorOfPos(BlockPos p) {
        return inTower(p.getX() + 0.5, p.getY(), p.getZ() + 0.5) ? floorOf(p.getY()) : -1;
    }

    public static BlockPos callSpot(int f) {
        return new BlockPos(32, FLOORS[f] + 1, 24);
    }

    private static final int[][] QUEUE_SPOTS = {{30, 23}, {30, 25}, {29, 24}, {28, 23}, {28, 25}, {27, 24}, {26, 23}, {26, 25}};

    public static BlockPos spotFor(Resident r, int f) {
        int idx = 0;
        for (Req q : QUEUE) {
            if (q.id.equals(r.getUUID())) break;
            if (q.from == f) idx++;
        }
        if (isRider(r) || idx == 0 && (cur == null || cur.from != f)) return callSpot(f);
        int[] s = QUEUE_SPOTS[Math.min(idx, QUEUE_SPOTS.length - 1)];
        return new BlockPos(s[0], FLOORS[f] + 1, s[1]);
    }

    public static boolean isRider(Resident r) {
        return cur != null && cur.id.equals(r.getUUID());
    }

    public static boolean controls(Resident r) {
        return isRider(r) && phase != Phase.WALKOFF;
    }

    public static boolean isQueued(Resident r) {
        for (Req q : QUEUE) if (q.id.equals(r.getUUID())) return true;
        return false;
    }

    public static int position(Resident r) {
        int i = 1;
        for (Req q : QUEUE) {
            if (q.id.equals(r.getUUID())) return i;
            i++;
        }
        return 0;
    }

    public static String riderName(ServerLevel lvl) {
        if (cur == null) return null;
        Entity e = lvl.getEntity(cur.id);
        return e instanceof Resident r && r.profile() != null ? r.profile().name : null;
    }

    public static void request(Resident r, int from, int to, long now) {
        if (isRider(r)) {
            if (phase == Phase.WALKOFF && from != cur.to) finishRide();
            else return;
        }
        for (Req q : QUEUE) {
            if (q.id.equals(r.getUUID())) {
                q.from = from;
                q.to = to;
                q.touched = now;
                return;
            }
        }
        QUEUE.add(new Req(r.getUUID(), from, to, now));
        if (debug && r.profile() != null) {
            Place d = r.destination();
            FireheartCity.LOG.info("[Elevator] " + r.profile().name + " queued " + FLOOR_NAMES[from] + " -> " + FLOOR_NAMES[to] + " (" + r.activityName() + ", heading to " + (d == null ? "?" : d.key) + ")");
        }
        int ahead = QUEUE.size() - 1 + (cur != null ? 1 : 0);
        r.swing(InteractionHand.MAIN_HAND);
        if (ahead > 0) r.say(ahead == 1 ? "I'll catch the elevator next." : "Elevator's busy... " + ahead + " people ahead of me.", 70);
    }

    public static void cancel(Resident r) {
        QUEUE.removeIf(q -> q.id.equals(r.getUUID()));
        if (isRider(r)) finishRide();
    }

    private static void finishRide() {
        cur = null;
        phase = null;
        gap = 30;
    }

    public static boolean isBroken() {
        return failedRepairs >= 2;
    }

    private static boolean isContraption(Entity e) {
        return CONTRAPTION.equals(ForgeRegistries.ENTITY_TYPES.getKey(e.getType()));
    }

    private static void scanCabin(ServerLevel lvl) {
        BlockEntity be = lvl.getBlockEntity(PULLEY);
        if (be != null) {
            try {
                Field f = field(be.getClass(), "movedContraption");
                Object o = f == null ? null : f.get(be);
                if (o instanceof Entity e && e.isAlive()) {
                    cabin = e;
                    return;
                }
            } catch (Throwable ignored) {
            }
        }
        List<Entity> found = lvl.getEntities((Entity) null, SHAFT, Elevator::isContraption);
        cabin = found.isEmpty() ? null : found.get(0);
    }

    private static double cabinFloorY() {
        return cabin == null ? Double.NaN : cabin.getBoundingBox().minY;
    }

    public static int cabinFloor() {
        if (cabin == null || still < 10) return -1;
        double y = cabinFloorY();
        for (int i = 0; i < FLOORS.length; i++) if (Math.abs(y - FLOORS[i]) < 0.2) return i;
        return -1;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void resetContact(ServerLevel lvl, int f) {
        BlockPos c = new BlockPos(34, FLOORS[f] + 3, 23);
        BlockState st = lvl.getBlockState(c);
        BlockState ns = st;
        for (net.minecraft.world.level.block.state.properties.Property prop : st.getProperties()) {
            String n = prop.getName();
            if ((n.equals("calling") || n.equals("powered")) && prop instanceof net.minecraft.world.level.block.state.properties.BooleanProperty bp)
                ns = ns.setValue(bp, false);
        }
        if (ns != st) lvl.setBlock(c, ns, 2);
    }

    public static void resetAllContacts(ServerLevel lvl) {
        for (int i = 0; i < FLOORS.length; i++) resetContact(lvl, i);
    }

    private static void press(ServerLevel lvl, int f) {
        BlockPos b = new BlockPos(33, FLOORS[f] + 3, 23);
        BlockState s = lvl.getBlockState(b);
        if (s.getBlock() instanceof ButtonBlock bb) {
            if (!s.getValue(ButtonBlock.POWERED)) {
                bb.press(s, lvl, b);
                lvl.playSound(null, b, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.4f, 0.7f);
            }
        } else {
            lvl.setBlock(b, Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACING, net.minecraft.core.Direction.WEST).setValue(ButtonBlock.FACE, net.minecraft.world.level.block.state.properties.AttachFace.WALL), 3);
        }
    }

    public static void tick(ServerLevel lvl) {
        debugLevel = lvl;
        if (!lvl.isPositionEntityTicking(PULLEY) || !lvl.isPositionEntityTicking(new BlockPos(32, 71, 24))) return;
        long now = lvl.getGameTime();
        scanCabin(lvl);
        double y = cabinFloorY();
        if (cabin != null && !Double.isNaN(lastMinY) && Math.abs(y - lastMinY) < 0.001) still++;
        else still = 0;
        lastMinY = y;
        if (now % 20 == 0) health(lvl, now);

        QUEUE.removeIf(q -> {
            Entity e = lvl.getEntity(q.id);
            return !(e instanceof Resident) || now - q.touched > 200;
        });

        if (cur != null) {
            Entity e = lvl.getEntity(cur.id);
            if (!(e instanceof Resident r) || r.isRemoved()) {
                finishRide();
                return;
            }
            drive(lvl, r, now);
            return;
        }
        stairs(lvl, now);
        if (gap > 0) { gap--; return; }
        Iterator<Req> it = QUEUE.iterator();
        while (it.hasNext()) {
            Req q = it.next();
            Entity e = lvl.getEntity(q.id);
            if (!(e instanceof Resident r)) { it.remove(); continue; }
            if (r.blockPosition().distSqr(callSpot(q.from)) > 8 * 8) continue;
            it.remove();
            cur = q;
            phase = isBroken() ? Phase.TELEPORT : Phase.CALL;
            phaseTicks = 0;
            if (r.convo != null) r.leaveConversation("Oh - that's my elevator! Talk later!");
            return;
        }
    }

    public static final int STAIRS_AFTER = 1200;

    /** Anyone who has waited more than a minute takes the stairs instead, so a long queue never traps people. */
    private static void stairs(ServerLevel lvl, long now) {
        if (now % 20 != 0) return;
        Iterator<Req> it = QUEUE.iterator();
        while (it.hasNext()) {
            Req q = it.next();
            if (now - q.since < STAIRS_AFTER) continue;
            Entity e = lvl.getEntity(q.id);
            if (!(e instanceof Resident r)) continue;
            if (r.convo != null || r.blockPosition().distSqr(callSpot(q.from)) > 10 * 10) continue;
            it.remove();
            if (r.isPassenger()) r.stopRiding();
            BlockPos t = callSpot(q.to);
            r.say(pick(r, "Forget it, I'm taking the stairs!", "The stairs it is.", q.to == 0 ? "I'll just walk down." : "I'll just walk up.", "Stairs. Good exercise, right?"), 60);
            r.teleportTo(t.getX() + 0.5, t.getY(), t.getZ() + 0.5);
            if (debug && r.profile() != null) FireheartCity.LOG.info("[Elevator] " + r.profile().name + " took the stairs " + FLOOR_NAMES[q.from] + " -> " + FLOOR_NAMES[q.to] + " after waiting " + (now - q.since) + "t");
        }
    }

    public static boolean debug;
    private static ServerLevel debugLevel;

    private static void setPhase(Phase p) {
        if (debug && cur != null && debugLevel != null) {
            Entity e = debugLevel.getEntity(cur.id);
            FireheartCity.LOG.info("[Elevator] " + (e instanceof Resident r && r.profile() != null ? r.profile().name : "?") + " " + phase + " -> " + p + " after " + phaseTicks + "t at " + (e == null ? "?" : String.format("%.1f %.1f %.1f", e.getX(), e.getY(), e.getZ())) + " cabinY=" + String.format("%.2f", cabinFloorY()));
        }
        phase = p;
        phaseTicks = 0;
    }

    private static void drive(ServerLevel lvl, Resident r, long now) {
        phaseTicks++;
        if (r.isPassenger() && phase != Phase.RIDE) {
            r.stopRiding();
            if (debug) FireheartCity.LOG.info("[Elevator] stood " + (r.profile() == null ? "?" : r.profile().name) + " up from a seat");
        }
        if (r.isSleeping()) r.stopSleeping();
        int from = cur.from, to = cur.to;
        switch (phase) {
            case CALL -> {
                BlockPos spot = callSpot(from);
                Vec3 sv = Vec3.atBottomCenterOf(spot);
                if (r.position().distanceToSqr(sv) > 1.2) {
                    if (phaseTicks % 20 == 1) r.getNavigation().moveTo(sv.x, sv.y, sv.z, 0.9);
                } else {
                    r.getNavigation().stop();
                    r.getLookControl().setLookAt(33.5, FLOORS[from] + 3.5, 23.5);
                }
                if (cabinFloor() == from) {
                    setPhase(Phase.BOARD);
                    return;
                }
                if (phaseTicks == 30 || phaseTicks % 300 == 0) {
                    r.swing(InteractionHand.MAIN_HAND);
                    if (phaseTicks >= 300) resetContact(lvl, from);
                    press(lvl, from);
                    if (phaseTicks == 30) r.say(pick(r, "Come on, elevator...", "Going to " + FLOOR_NAMES[to] + ".", "*presses the button*"), 60);
                }
                if (phaseTicks > 1400) {
                    failedRepairs++;
                    setPhase(Phase.TELEPORT);
                }
            }
            case BOARD -> {
                if (cabinFloor() != from && still > 20) {
                    setPhase(Phase.CALL);
                    return;
                }
                Vec3 c = new Vec3(36.5, FLOORS[from] + 1, 24.5);
                Vec3 spot = new Vec3(32.5, FLOORS[from] + 1, 24.5);
                boolean lined = Math.abs(r.getZ() - 24.5) < 0.45 && r.getX() > 32.0;
                if (!lined && r.position().distanceToSqr(spot) > 0.5) {
                    double far = r.position().distanceToSqr(spot);
                    if (far < 9.0 && Math.abs(r.getY() - spot.y) < 1.0) {
                        r.getNavigation().stop();
                        walkTo(r, spot.x, spot.z, 0.14);
                    } else if (phaseTicks % 20 == 1) r.getNavigation().moveTo(spot.x, spot.y, spot.z, 0.9);
                    if (phaseTicks > 60 && far < 9.0 || phaseTicks > 200) r.teleportTo(spot.x, spot.y, spot.z);
                    return;
                }
                r.getNavigation().stop();
                walkTo(r, c.x, c.z, 0.16);
                double dx = r.getX() - c.x, dz = r.getZ() - c.z;
                if (dx * dx + dz * dz < 0.2 || phaseTicks > 60) {
                    if (dx * dx + dz * dz >= 0.2) r.teleportTo(c.x, c.y + 0.05, c.z);
                    r.setDeltaMovement(Vec3.ZERO);
                    setPhase(Phase.RIDE);
                }
            }
            case RIDE -> {
                r.getNavigation().stop();
                double cy = cabinFloorY();
                if (phaseTicks == 15) {
                    press(lvl, to);
                    r.getLookControl().setLookAt(33.5, r.getEyeY(), 24.5);
                }
                if (cabin != null) {
                    double dx = r.getX() - 36.5, dz = r.getZ() - 24.5;
                    if (dx * dx + dz * dz > 0.3) walkTo(r, 36.5, 24.5, 0.08);
                    if (r.getY() < cy - 1.5 || r.getY() > cy + 3) r.teleportTo(36.5, cy + 1.05, 24.5);
                    if (phaseTicks % 20 == 0) r.getLookControl().setLookAt(33.5, r.getEyeY(), 24.5);
                }
                int at = cabinFloor();
                if (at == to && phaseTicks > 40 && Math.abs(r.getY() - (FLOORS[to] + 1)) < 1.3) {
                    setPhase(Phase.EXIT);
                    return;
                }
                if (at >= 0 && at != to && phaseTicks > 60 && phaseTicks % 200 == 0) {
                    resetContact(lvl, to);
                    press(lvl, to);
                }
                if (phaseTicks > 1400 || cabin == null && phaseTicks > 100) {
                    r.teleportTo(32.5, FLOORS[to] + 1, 24.5);
                    setPhase(Phase.WALKOFF);
                }
            }
            case EXIT -> {
                r.getNavigation().stop();
                if (phaseTicks < 10) return;
                walkTo(r, 32.5, 24.5, 0.16);
                if (r.getX() < 33.2 || phaseTicks > 60) {
                    if (r.getX() >= 33.2) r.teleportTo(32.5, FLOORS[to] + 1, 24.5);
                    r.setDeltaMovement(Vec3.ZERO);
                    setPhase(Phase.WALKOFF);
                    if (r.getRandom().nextFloat() < 0.35f) r.say(to == 0 ? "Lobby! Off I go." : pick(r, "Home sweet home.", "Thanks, elevator!", FLOOR_NAMES[to].substring(0, 1).toUpperCase() + FLOOR_NAMES[to].substring(1) + ", finally."), 50);
                }
            }
            case WALKOFF -> {
                boolean done;
                if (to == 0) done = Elevator.floorOfEntity(r) < 0 || r.getX() < 25 || phaseTicks > 400;
                else {
                    BlockPos home = r.homePos();
                    done = home != null && r.blockPosition().closerThan(home, 2.5) || Elevator.floorOfEntity(r) != to || phaseTicks > 600;
                }
                if (done) finishRide();
            }
            case TELEPORT -> {
                BlockPos spot = callSpot(from);
                if (phaseTicks == 1) {
                    r.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
                    lvl.playSound(null, spot, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.8f, 1.4f);
                    r.say("The elevator's out again... taking the service lift.", 60);
                }
                if (phaseTicks == 80) {
                    BlockPos t = callSpot(to);
                    r.teleportTo(t.getX() + 0.5, t.getY(), t.getZ() + 0.5);
                    lvl.playSound(null, t, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.8f, 1.4f);
                    setPhase(Phase.WALKOFF);
                }
            }
        }
    }

    private static void walkTo(Resident r, double x, double z, double speed) {
        double dx = x - r.getX(), dz = z - r.getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d < 0.05) return;
        double s = Math.min(speed, d);
        Vec3 v = r.getDeltaMovement();
        r.setDeltaMovement(dx / d * s, v.y, dz / d * s);
        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
        r.setYRot(yaw);
        r.yBodyRot = yaw;
        r.yHeadRot = yaw;
        r.hurtMarked = true;
    }

    private static String pick(Resident r, String... s) {
        return Lines.pick(r.getRandom(), s);
    }

    private static void health(ServerLevel lvl, long now) {
        lastInfo = cabin == null ? "cabin missing (" + missing + ")" : "cabin floor y=" + String.format("%.2f", cabinFloorY()) + " at " + (cabinFloor() >= 0 ? FLOOR_NAMES[cabinFloor()] : "moving");
        if (cabin != null) {
            missing = 0;
            if (failedRepairs > 0 || announcedBroken) {
                failedRepairs = 0;
                if (announcedBroken) {
                    CityData d = CityData.get(lvl);
                    d.event(Calendar.dayOf(lvl.getDayTime()), "elevator", "the Ember Heights elevator is working again", PULLEY);
                    announcedBroken = false;
                }
            }
            return;
        }
        missing++;
        if (missing >= 30 && now - lastRepair > 2400) {
            lastRepair = now;
            boolean ok = repair(lvl);
            if (!ok) failedRepairs++;
            if (!announcedBroken) {
                announcedBroken = true;
                CityData d = CityData.get(lvl);
                d.event(Calendar.dayOf(lvl.getDayTime()), "elevator", "the Ember Heights elevator broke down and had to be rebuilt", PULLEY);
            }
        }
    }

    private static final String[][] CABIN = {
            {"35 94 23", "minecraft:smooth_quartz"}, {"36 94 23", "minecraft:smooth_quartz"}, {"37 94 23", "minecraft:smooth_quartz"},
            {"35 94 24", "minecraft:smooth_quartz"}, {"36 94 24", "minecraft:black_concrete"}, {"37 94 24", "minecraft:smooth_quartz"},
            {"35 94 25", "minecraft:smooth_quartz"}, {"36 94 25", "minecraft:smooth_quartz"}, {"37 94 25", "minecraft:smooth_quartz"},
            {"35 98 23", "minecraft:white_concrete"}, {"36 98 23", "minecraft:white_concrete"}, {"37 98 23", "minecraft:white_concrete"},
            {"35 98 24", "minecraft:white_concrete"}, {"36 98 24", "minecraft:sea_lantern"}, {"37 98 24", "minecraft:white_concrete"},
            {"35 98 25", "minecraft:white_concrete"}, {"36 98 25", "minecraft:white_concrete"}, {"37 98 25", "minecraft:white_concrete"},
            {"35 95 23", "create:framed_glass_pane"}, {"35 96 23", "create:framed_glass_pane"},
            {"35 95 25", "create:framed_glass_pane"}, {"35 96 25", "create:framed_glass_pane"}, {"35 97 25", "create:framed_glass_pane"},
            {"35 97 23", "create:redstone_contact[facing=west,powered=false]"},
            {"35 97 24", "create:framed_glass"},
            {"35 95 24", "create:framed_glass_door[facing=east,half=lower,hinge=left,open=false,visible=true]"},
            {"35 96 24", "create:framed_glass_door[facing=east,half=upper,hinge=left,open=false,visible=true]"},
            {"37 95 24", "create:contraption_controls[facing=west,open=false,virtual=false]"},
            {"37 95 23", "minecraft:polished_blackstone_slab[type=bottom]"},
            {"37 95 25", "minecraft:polished_blackstone_slab[type=bottom]"}
    };

    public static boolean repair(ServerLevel lvl) {
        scanCabin(lvl);
        if (cabin != null) return true;
        BlockEntity be = lvl.getBlockEntity(PULLEY);
        if (be == null) {
            lastInfo = "repair failed: pulley not loaded";
            return false;
        }
        try {
            setField(be, "running", false);
            setField(be, "offset", 0f);
            setField(be, "movedContraption", null);
            setField(be, "needsContraption", false);
            for (int y = 70; y <= 98; y++)
                for (int x = 35; x <= 37; x++)
                    for (int z = 23; z <= 25; z++) {
                        BlockPos p = new BlockPos(x, y, z);
                        if (!lvl.getBlockState(p).isAir()) lvl.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
            var lookup = lvl.holderLookup(net.minecraft.core.registries.Registries.BLOCK);
            for (String[] c : CABIN) {
                String[] xyz = c[0].split(" ");
                BlockPos p = new BlockPos(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
                BlockState st = net.minecraft.commands.arguments.blocks.BlockStateParser.parseForBlock(lookup, c[1], false).f_234748_();
                lvl.setBlock(p, st, 2);
            }
            for (Entity g : lvl.getEntities((Entity) null, SHAFT, e -> new ResourceLocation("create", "super_glue").equals(ForgeRegistries.ENTITY_TYPES.getKey(e.getType())))) g.discard();
            net.minecraft.nbt.CompoundTag tag = net.minecraft.nbt.TagParser.parseTag("{id:\"create:super_glue\",From:[0.0d,0.0d,0.0d],To:[3.0d,5.0d,3.0d]}");
            Entity glue = net.minecraft.world.entity.EntityType.loadEntityRecursive(tag, lvl, e -> {
                e.moveTo(35.0, 94.0, 23.0, 0, 0);
                return e;
            });
            if (glue != null) {
                glue.load(mergePos(glue.saveWithoutId(new net.minecraft.nbt.CompoundTag()), tag));
                lvl.addFreshEntity(glue);
            }
            resetAllContacts(lvl);
            setField(be, "assembleNextTick", true);
            be.setChanged();
            lastInfo = "repair started (glue " + (glue != null) + ")";
            FireheartCity.LOG.info("Solaris: rebuilt the Ember Heights elevator cabin");
            return true;
        } catch (Throwable t) {
            lastInfo = "repair failed: " + t;
            FireheartCity.LOG.error("Elevator repair failed", t);
            return false;
        }
    }

    private static net.minecraft.nbt.CompoundTag mergePos(net.minecraft.nbt.CompoundTag saved, net.minecraft.nbt.CompoundTag extra) {
        saved.put("From", extra.get("From"));
        saved.put("To", extra.get("To"));
        return saved;
    }

    private static void setField(Object o, String name, Object v) throws IllegalAccessException {
        Field f = field(o.getClass(), name);
        if (f != null) f.set(o, v);
    }

    private static Field field(Class<?> c, String name) {
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    public static String describe(ServerLevel lvl) {
        StringBuilder sb = new StringBuilder("Elevator: " + lastInfo);
        BlockEntity be = lvl.getBlockEntity(PULLEY);
        if (be != null) {
            try {
                sb.append(" | pulley running=").append(field(be.getClass(), "running").get(be))
                        .append(" offset=").append(field(be.getClass(), "offset").get(be))
                        .append(" assembleNext=").append(field(be.getClass(), "assembleNextTick").get(be))
                        .append(" error=").append(field(be.getClass(), "lastException").get(be));
            } catch (Throwable ignored) {
            }
        }
        if (isBroken()) sb.append(" [BROKEN - using fallback]");
        String rn = riderName(lvl);
        if (rn != null) sb.append(" | riding: ").append(rn).append(" (").append(phase).append(" ").append(FLOOR_NAMES[cur.from]).append(" -> ").append(FLOOR_NAMES[cur.to]).append(")");
        sb.append(" | queue: ");
        if (QUEUE.isEmpty()) sb.append("empty");
        for (Req q : QUEUE) {
            Entity e = lvl.getEntity(q.id);
            if (e instanceof Resident r && r.profile() != null) sb.append(r.profile().name).append(" ");
        }
        return sb.toString();
    }
}
