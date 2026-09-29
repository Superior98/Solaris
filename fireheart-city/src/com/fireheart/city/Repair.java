package com.fireheart.city;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * City upkeep: a blueprint of Solaris (baked from the world before any damage) plus a record of blocks blown up by mobs
 * or burnt away. The repair crew walks to the damage and rebuilds it block by block - every block flies from their
 * tool belt into place - and the same crew builds new city projects (like the beach bar) with that animation.
 */
public final class Repair {
    private Repair() {}

    static final class Job {
        final BlockPos pos;
        final String state;
        final boolean build;
        long seen;
        int tries;

        Job(BlockPos pos, String state, boolean build, long seen) {
            this.pos = pos;
            this.state = state;
            this.build = build;
            this.seen = seen;
        }
    }

    public static final class Store extends SavedData {
        public final LinkedHashMap<Long, Job> jobs = new LinkedHashMap<>();
        final LongOpenHashSet ignored = new LongOpenHashSet();
        int fixed;
        boolean enabled = true;

        static Store load(CompoundTag t) {
            Store s = new Store();
            for (Tag e : t.getList("jobs", Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) e;
                BlockPos p = BlockPos.of(c.getLong("p"));
                s.jobs.put(p.asLong(), new Job(p, c.getString("s"), c.getBoolean("b"), c.getLong("t")));
            }
            for (long l : t.getLongArray("ignored")) s.ignored.add(l);
            s.fixed = t.getInt("fixed");
            s.enabled = !t.contains("enabled") || t.getBoolean("enabled");
            return s;
        }

        @Override
        public CompoundTag save(CompoundTag t) {
            ListTag l = new ListTag();
            for (Job j : jobs.values()) {
                CompoundTag c = new CompoundTag();
                c.putLong("p", j.pos.asLong());
                c.putString("s", j.state);
                c.putBoolean("b", j.build);
                c.putLong("t", j.seen);
                l.add(c);
            }
            t.put("jobs", l);
            t.putLongArray("ignored", ignored.toLongArray());
            t.putInt("fixed", fixed);
            t.putBoolean("enabled", enabled);
            return t;
        }
    }

    public static Store store(ServerLevel sl) {
        ServerLevel ow = sl.getServer().overworld();
        return ow.getDataStorage().computeIfAbsent(Store::load, Store::new, "fireheartcity_repair");
    }

    static String[] palette;
    static final Map<Long, long[]> BP_POS = new HashMap<>();
    static final Map<Long, int[]> BP_PAL = new HashMap<>();
    static final Map<String, BlockState> PARSED = new HashMap<>();
    static List<Long> chunkOrder = new ArrayList<>();
    static int scanIdx;
    static boolean loaded;

    static synchronized void loadBlueprint() {
        if (loaded) return;
        loaded = true;
        try (InputStream in = Repair.class.getResourceAsStream("/data/fireheartcity/blueprint.bin")) {
            if (in == null) return;
            DataInputStream d = new DataInputStream(new GZIPInputStream(in));
            byte[] magic = new byte[5];
            d.readFully(magic);
            int np = varint(d);
            palette = new String[np];
            for (int i = 0; i < np; i++) {
                byte[] b = new byte[varint(d)];
                d.readFully(b);
                palette[i] = new String(b, java.nio.charset.StandardCharsets.UTF_8);
            }
            int nc = varint(d);
            for (int c = 0; c < nc; c++) {
                int cx = unzz(varint(d)), cz = unzz(varint(d)), n = varint(d);
                long[] pos = new long[n];
                int[] pal = new int[n];
                for (int i = 0; i < n; i++) {
                    int xz = d.readUnsignedByte();
                    int y = unzz(varint(d));
                    pos[i] = BlockPos.asLong(cx * 16 + (xz >> 4), y, cz * 16 + (xz & 15));
                    pal[i] = varint(d);
                }
                long key = ChunkPos.asLong(cx, cz);
                BP_POS.put(key, pos);
                BP_PAL.put(key, pal);
                chunkOrder.add(key);
            }
            FireheartCity.LOG.info("Repair blueprint: " + nc + " chunks, " + np + " block states");
        } catch (IOException e) {
            FireheartCity.LOG.warn("Couldn't read the city blueprint", e);
        }
    }

    static int varint(DataInputStream d) throws IOException {
        int v = 0, s = 0;
        while (true) {
            int b = d.readUnsignedByte();
            v |= (b & 0x7F) << s;
            if ((b & 0x80) == 0) return v;
            s += 7;
        }
    }

    static int unzz(int v) {
        return (v >>> 1) ^ -(v & 1);
    }

    static BlockState parse(ServerLevel sl, String s) {
        return PARSED.computeIfAbsent(s, k -> {
            try {
                return net.minecraft.commands.arguments.blocks.BlockStateParser.parseForBlock(sl.holderLookup(net.minecraft.core.registries.Registries.BLOCK), k, false).f_234748_();
            } catch (Exception e) {
                return null;
            }
        });
    }

    static boolean missing(BlockState cur) {
        return cur.isAir() || cur.is(BlockTags.FIRE) || (cur.canBeReplaced() && cur.getFluidState().isEmpty()) || (!cur.getFluidState().isEmpty() && !cur.getFluidState().isSource());
    }

    public static void reset() {
        CREW.clear();
        scanIdx = 0;
    }

    /** Called every tick from Events: scans a blueprint chunk every couple of seconds for holes. */
    public static void tick(ServerLevel sl, CityData d) {
        if (sl.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
        long now = sl.getGameTime();
        if (now % 40 != 7) return;
        loadBlueprint();
        Store st = store(sl);
        if (!st.enabled || chunkOrder.isEmpty()) return;
        for (int k = 0; k < 3; k++) {
            long key = chunkOrder.get(scanIdx++ % chunkOrder.size());
            int cx = ChunkPos.getX(key), cz = ChunkPos.getZ(key);
            if (sl.getChunkSource().getChunkNow(cx, cz) == null) continue;
            long[] pos = BP_POS.get(key);
            int[] pal = BP_PAL.get(key);
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            List<AABB> avoid = avoid(sl, cx, cz);
            int added = 0;
            for (int i = 0; i < pos.length; i++) {
                if (st.ignored.contains(pos[i]) || st.jobs.containsKey(pos[i])) continue;
                m.set(pos[i]);
                if (blocked(avoid, m)) continue;
                BlockState want = parse(sl, palette[pal[i]]);
                BlockState cur0 = sl.getBlockState(m);
                if (want == null || cur0.is(want.getBlock()) || !missing(cur0)) continue;
                st.jobs.put(pos[i], new Job(m.immutable(), palette[pal[i]], false, now));
                added++;
            }
            if (added > 0) st.setDirty();
        }
    }

    static List<AABB> avoid(ServerLevel sl, int cx, int cz) {
        List<AABB> l = new ArrayList<>();
        AABB box = new AABB(cx * 16 - 24, -64, cz * 16 - 24, cx * 16 + 40, 320, cz * 16 + 40);
        for (Entity e : sl.getEntities((Entity) null, box, e -> {
            var k = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
            return k != null && (k.getPath().contains("contraption") || k.getNamespace().equals("valkyrienskies"));
        })) l.add(e.getBoundingBox().inflate(4));
        l.add(new AABB(34, 60, 22, 39, 103, 27));
        for (AutoDoor d : AutoDoor.DOORS) l.add(d.sense.inflate(1));
        return l;
    }

    static boolean blocked(List<AABB> avoid, BlockPos p) {
        for (AABB a : avoid) if (a.contains(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5)) return true;
        return false;
    }

    public static void onPlayerBreak(ServerLevel sl, BlockPos pos, Player pl) {
        if (sl.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
        Store st = store(sl);
        if (st.jobs.remove(pos.asLong()) != null) st.setDirty();
        if (inBlueprint(pos)) {
            st.ignored.add(pos.asLong());
            st.setDirty();
        }
    }

    public static void onPlayerPlace(ServerLevel sl, BlockPos pos) {
        Store st = store(sl);
        if (st.jobs.remove(pos.asLong()) != null) st.setDirty();
    }

    static boolean inBlueprint(BlockPos pos) {
        loadBlueprint();
        long[] a = BP_POS.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        if (a == null) return false;
        long l = pos.asLong();
        for (long v : a) if (v == l) return true;
        return false;
    }

    public static void onExplosion(ServerLevel sl, net.minecraft.world.level.Explosion ex, List<BlockPos> blocks) {
        if (sl.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
        Vec3 c = ex.getPosition();
        if (Math.abs(c.x) > 400 || Math.abs(c.z) > 400) return;
        Store st = store(sl);
        long now = sl.getGameTime();
        for (BlockPos p : blocks) {
            BlockState s = sl.getBlockState(p);
            if (s.isAir() || !s.getFluidState().isEmpty() || s.is(Blocks.TNT) || s.is(BlockTags.FIRE)) continue;
            st.jobs.putIfAbsent(p.asLong(), new Job(p.immutable(), NbtUtils.writeBlockState(s).toString(), false, now));
        }
        st.setDirty();
    }

    /** Queues a new construction for the crew: blocks are placed bottom-up with the build animation. */
    public static void construct(ServerLevel sl, List<BlockPos> positions, List<String> states) {
        Store st = store(sl);
        long now = sl.getGameTime();
        for (int i = 0; i < positions.size(); i++) st.jobs.put(positions.get(i).asLong(), new Job(positions.get(i).immutable(), states.get(i), true, now));
        st.setDirty();
    }

    static BlockState state(ServerLevel sl, String s) {
        if (s.startsWith("{")) {
            try {
                return NbtUtils.readBlockState(sl.holderLookup(net.minecraft.core.registries.Registries.BLOCK), net.minecraft.nbt.TagParser.parseTag(s));
            } catch (Exception e) {
                return null;
            }
        }
        return parse(sl, s);
    }

    static final class Crew {
        BlockPos site;
        int placed;
        boolean building, holding;
        Job current;
        long since;
        int swing;
        net.minecraft.world.item.ItemStack saved;
        long lastWork;
        boolean announced;
    }

    static final Map<UUID, Crew> CREW = new HashMap<>();

    public static BlockPos target(Resident r) {
        Crew c = CREW.get(r.getUUID());
        if (c == null || c.current == null) return null;
        return r.getEyePosition().distanceTo(Vec3.atCenterOf(c.current.pos)) > 4.3 ? c.current.pos : r.blockPosition();
    }

    public static int pending(ServerLevel sl) {
        return store(sl).jobs.size();
    }

    static Job nearest(Store st, BlockPos from, double max, long now) {
        Job best = null;
        double bd = max * max;
        for (Job j : st.jobs.values()) {
            if (now < j.seen) continue;
            if (!j.build && now - j.seen < (j.state.startsWith("{") ? 60 : 1200)) continue;
            double dd = j.pos.distSqr(from) + j.pos.getY() * 0.01;
            if (dd < bd) { bd = dd; best = j; }
        }
        return best;
    }

    /** Runs the repair crew member. Returns true while they're busy with a repair or construction. */
    public static boolean tick(Resident r, CityData.Profile p) {
        ServerLevel sl = (ServerLevel) r.level();
        Store st = store(sl);
        long now = sl.getGameTime();
        Crew c = CREW.computeIfAbsent(r.getUUID(), k -> new Crew());
        if (!st.enabled || r.isSleeping() || r.skyPhase() != 0 || r.isPassenger()) { holster(r, c); return false; }
        if (st.jobs.isEmpty() && c.current == null) {
            if (c.site != null) finish(r, p, c, sl);
            holster(r, c);
            return false;
        }
        if (c.current == null) {
            Job j = nearest(st, c.site != null ? c.site : r.blockPosition(), c.site != null ? 24 : 600, now);
            if (j == null && c.site != null) j = nearest(st, r.blockPosition(), 600, now);
            if (j == null) {
                if (c.site != null) finish(r, p, c, sl);
                holster(r, c);
                return false;
            }
            st.jobs.remove(j.pos.asLong());
            st.setDirty();
            c.current = j;
            c.since = now;
            c.swing = 0;
            c.site = j.pos;
            if (!c.announced) {
                c.announced = true;
                if (r.convo != null) r.leaveConversation("Something needs fixing - be right back!");
                r.sayTo(j.build ? Lines.pick(r.getRandom(), "New build today - let's get to work!", "Hard hat on. Time to build!", "Blueprints are in. Let's make it happen!")
                        : Lines.pick(r.getRandom(), "Something got wrecked - I'm on it!", "Uh oh, damage report. On my way!", "Don't worry, I'll have that patched up in no time!"), 50);
            }
        }
        Job j = c.current;
        BlockState want = state(sl, j.state);
        BlockState cur = sl.getBlockState(j.pos);
        if (want == null || (!j.build && (!missing(cur) || cur.is(want.getBlock()))) || (j.build && cur.equals(want))) {
            c.current = null;
            return true;
        }
        Vec3 target = Vec3.atCenterOf(j.pos);
        double reach = r.getEyePosition().distanceTo(target);
        if (reach > 4.6) {
            if (now % 10 == 0 || r.getNavigation().isDone()) r.getNavigation().moveTo(j.pos.getX() + 0.5, j.pos.getY(), j.pos.getZ() + 0.5, 1.05);
            if (now - c.since > 400) giveUp(st, c, j, now);
            return true;
        }
        r.getNavigation().stop();
        r.getLookControl().setLookAt(target.x, target.y, target.z, 40, 40);
        if (!sl.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new AABB(j.pos), e -> !e.isSpectator()).isEmpty() && want.isCollisionShapeFullBlock(sl, j.pos)) {
            if (now - c.since > 200) giveUp(st, c, j, now);
            return true;
        }
        if (!want.canSurvive(sl, j.pos)) {
            giveUp(st, c, j, now);
            return true;
        }
        hold(r, c, want);
        if (c.swing++ < 5) return true;
        r.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        place(sl, j.pos, want);
        c.current = null;
        c.placed++;
        c.building |= j.build;
        st.fixed++;
        st.setDirty();
        return true;
    }

    static void giveUp(Store st, Crew c, Job j, long now) {
        j.tries++;
        j.seen = now + 400L * j.tries;
        if (j.tries < 4) st.jobs.put(j.pos.asLong(), j);
        else if (!j.build) st.ignored.add(j.pos.asLong());
        st.setDirty();
        c.current = null;
    }

    static void hold(Resident r, Crew c, BlockState want) {
        net.minecraft.world.item.Item it = want.getBlock().asItem();
        if (it == net.minecraft.world.item.Items.AIR) it = net.minecraft.world.item.Items.BRICKS;
        if (!c.holding) {
            c.saved = r.getMainHandItem().copy();
            c.holding = true;
        }
        if (r.getMainHandItem().getItem() != it) r.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(it));
    }

    static void holster(Resident r, Crew c) {
        if (!c.holding) return;
        r.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, c.saved == null ? net.minecraft.world.item.ItemStack.EMPTY : c.saved);
        c.holding = false;
        c.saved = null;
    }

    static void place(ServerLevel sl, BlockPos pos, BlockState want) {
        sl.setBlock(pos, want, 3);
        if (want.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)
                && want.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {
            BlockPos up = pos.above();
            if (sl.getBlockState(up).canBeReplaced()) sl.setBlock(up, want.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), 3);
        }
        if (want.getBlock() instanceof net.minecraft.world.level.block.BedBlock && want.getValue(net.minecraft.world.level.block.BedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.FOOT) {
            BlockPos head = pos.relative(want.getValue(net.minecraft.world.level.block.BedBlock.FACING));
            if (sl.getBlockState(head).canBeReplaced()) sl.setBlock(head, want.setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD), 3);
        }
        var snd = want.getSoundType();
        sl.playSound(null, pos, snd.getPlaceSound(), SoundSource.BLOCKS, (snd.getVolume() + 1) / 2f, snd.getPitch() * 0.8f);
    }

    static CompoundTag transform(float x, float y, float z, float s) {
        CompoundTag t = new CompoundTag();
        t.put("translation", floats(x, y, z));
        t.put("scale", floats(s, s, s));
        t.put("left_rotation", floats(0, 0, 0, 1));
        t.put("right_rotation", floats(0, 0, 0, 1));
        return t;
    }

    static ListTag floats(float... v) {
        ListTag l = new ListTag();
        for (float f : v) l.add(FloatTag.valueOf(f));
        return l;
    }

    static void finish(Resident r, CityData.Profile p, Crew c, ServerLevel sl) {
        if (c.placed > 0) {
            r.gesture(Resident.G_THUMBS, 40);
            r.sayTo(Lines.pick(r.getRandom(), "Good as new!", "All patched up. You'd never know!", "Done! Solaris looks brand new again.", "Another job well done."), 60);
            String where = FireDept.nearPlace(c.site);
            p.log(r.routineDay()).note((c.building ? "I built " : "I rebuilt ") + c.placed + " blocks near " + where);
            if (c.placed >= 8) r.data().event(r.day(), "city", p.name + (c.building ? " finished building near " : " repaired damage near ") + where + " (" + c.placed + " blocks)", c.site, p.id);
        }
        c.site = null;
        c.placed = 0;
        c.building = false;
        c.announced = false;
    }

    public static String status(ServerLevel sl) {
        loadBlueprint();
        Store st = store(sl);
        int build = 0;
        for (Job j : st.jobs.values()) if (j.build) build++;
        return "§6Repair crew§7: " + (st.enabled ? "§aon" : "§coff") + "§7 | blueprint chunks " + chunkOrder.size() + " | queued repairs " + (st.jobs.size() - build) + ", builds " + build + " | fixed so far " + st.fixed + " | ignored (your edits) " + st.ignored.size();
    }

    public static int accept(ServerLevel sl, BlockPos at, int radius) {
        loadBlueprint();
        Store st = store(sl);
        int n = 0;
        for (var e : BP_POS.entrySet()) {
            int[] pal = BP_PAL.get(e.getKey());
            long[] a = e.getValue();
            for (int i = 0; i < a.length; i++) {
                BlockPos p = BlockPos.of(a[i]);
                if (p.distSqr(at) > (double) radius * radius) continue;
                BlockState want = parse(sl, palette[pal[i]]);
                if (want != null && !sl.getBlockState(p).equals(want)) {
                    st.ignored.add(a[i]);
                    n++;
                }
            }
        }
        st.jobs.values().removeIf(j -> !j.build && j.pos.distSqr(at) <= (double) radius * radius);
        st.setDirty();
        return n;
    }

    public static int scanNow(ServerLevel sl) {
        loadBlueprint();
        int before = store(sl).jobs.size();
        for (int i = 0; i < chunkOrder.size(); i++) {
            long saved = sl.getGameTime();
            scanIdx = i;
            Store st = store(sl);
            long key = chunkOrder.get(i);
            if (sl.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key)) == null) continue;
            long[] pos = BP_POS.get(key);
            int[] pal = BP_PAL.get(key);
            List<AABB> avoid = avoid(sl, ChunkPos.getX(key), ChunkPos.getZ(key));
            for (int k = 0; k < pos.length; k++) {
                if (st.ignored.contains(pos[k]) || st.jobs.containsKey(pos[k])) continue;
                BlockPos p = BlockPos.of(pos[k]);
                if (blocked(avoid, p)) continue;
                BlockState want = parse(sl, palette[pal[k]]);
                BlockState cur0 = sl.getBlockState(p);
                if (want == null || cur0.is(want.getBlock()) || !missing(cur0)) continue;
                st.jobs.put(pos[k], new Job(p, palette[pal[k]], false, saved - 1300));
            }
            st.setDirty();
        }
        return store(sl).jobs.size() - before;
    }
}
