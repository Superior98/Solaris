package com.fireheart.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * StellarFox1's home: a cantilevered glass-and-neon villa on the hill above the beach, in the navy / ice-blue / starlight
 * colours of his skin, with a secret underground base below. Everything is driven from here: sliding doors, presence
 * lighting, neon streaks racing round the facade, lifts (levitation tubes), the fox constellation crest, rooftop
 * searchlights, and in the base a pulsing reactor, a live holographic city map, server racks, a laser grid and ASTRA,
 * the house AI.
 */
public final class StellarHome {
    private StellarHome() {}

    public static final String[] OWNERS = {"StellarFox1", "magmagamer9", "Fireheart_4743"};

    static final int X1 = 66, X2 = 98, Z1 = 7, Z2 = 21, F = 84, C1 = 90, R = 96;
    static final int BX1 = 64, BX2 = 100, BZ1 = 6, BZ2 = 22, BF = 56, BC = 68;
    static final BlockPos CORE = new BlockPos(88, 57, 14);
    static final BlockPos HOLO = new BlockPos(76, 58, 16);

    static final String WALL = "betterblockz:zeon_blue_blockz_0|minecraft:black_concrete";
    static final String WALL_L = "betterblockz:zeon_light_blue_blockz_0|minecraft:light_gray_concrete";
    static final String PANEL = "betterblockz:zeno_blue_blockz_18|minecraft:blue_concrete";
    static final String DARK = "minecraft:black_concrete";
    static final String FLOOR = "betterblockz:zenohex_blockz_3|minecraft:polished_deepslate";
    static final String CIRCUIT = "betterblockz:azur_blockz_9|minecraft:polished_blackstone";
    static final String GLOW = "betterblockz:cyberlight_blockz_11[lit=true]|minecraft:sea_lantern";
    static final String STAR = "betterblockz:cyberlight_blockz_0[lit=true]|minecraft:sea_lantern";
    static final String OFF = "betterblockz:cyberlight_blockz_3[lit=false]|minecraft:black_concrete";
    static final String GLASS = "minecraft:light_blue_stained_glass";
    static final String TINT = "betterblockz:tintedglass_blockz_11|minecraft:tinted_glass";
    static final String VNEON = "betterblockz:zeon_light_blue_blockz_2[axis=y]|minecraft:light_blue_concrete";
    static final String CYCLE = "betterblockz:cyberlight_cycle_blockz_0[lit=true]|minecraft:sea_lantern";
    static final String AURORA = "betterblockz:aurora_blockz|minecraft:black_stained_glass";
    static final String ARROWS = "betterblockz:z_moving|minecraft:light_blue_glazed_terracotta";

    record Lift(String name, int x, int z, int bottom, int top, int r, boolean secret, String hatch) {}

    static final List<Lift> LIFTS = List.of(
            new Lift("secret", 71, 10, BF, F, 1, true, FLOOR),
            new Lift("stairs", 92, 11, F, C1, 0, false, FLOOR),
            new Lift("beach", 63, 20, 71, F, 0, false, FLOOR));

    record Door(int x1, int y1, int z1, int x2, int y2, int z2, String closed) {}

    static final List<Door> DOORS = List.of(
            new Door(81, F + 1, Z1, 83, F + 3, Z1, GLASS),
            new Door(80, F + 1, Z2, 84, F + 3, Z2, GLASS),
            new Door(90, C1 + 1, 19, 92, C1 + 3, 19, GLASS));

    record Room(String name, int x1, int y1, int z1, int x2, int y2, int z2, List<BlockPos> lights) {}

    static final List<Room> ROOMS = new ArrayList<>();
    static final List<BlockPos> STREAK = new ArrayList<>();
    static final List<BlockPos> RACKS = new ArrayList<>();
    static final Map<UUID, Integer> RIDING = new HashMap<>();
    static final Map<UUID, Long> WELCOMED = new HashMap<>();
    static final Map<Integer, Boolean> DOOR_OPEN = new HashMap<>();
    static final Map<String, Long> HATCH_CLOSE_AT = new HashMap<>();
    static final Map<String, Boolean> ROOM_LIT = new HashMap<>();
    static long laserOffUntil, alarmUntil;
    static int streakHead;

    static {
        ROOMS.add(new Room("study", 67, F + 1, 8, 75, F + 5, 20, lightsGrid(68, C1, 9, 74, 19)));
        ROOMS.add(new Room("living", 76, F + 1, 8, 88, F + 5, 20, lightsGrid(77, C1, 9, 87, 19)));
        ROOMS.add(new Room("kitchen", 89, F + 1, 8, 97, F + 5, 20, lightsGrid(90, C1, 9, 96, 19)));
        ROOMS.add(new Room("suite", 71, C1 + 1, 10, 93, C1 + 5, 18, lightsGrid(72, R, 11, 92, 17)));
        ROOMS.add(new Room("base", BX1 + 1, BF + 1, BZ1 + 1, BX2 - 1, BC - 1, BZ2 - 1, lightsGrid(67, BC, 8, 97, 20)));
        for (int x = X1; x <= X2; x++) STREAK.add(new BlockPos(x, F, Z2 + 1));
        for (int z = Z2; z >= Z1; z--) STREAK.add(new BlockPos(X2 + 1, F, z));
        for (int x = X2; x >= X1; x--) STREAK.add(new BlockPos(x, F, Z1 - 1));
        for (int z = Z1; z <= Z2; z++) STREAK.add(new BlockPos(X1 - 1, F, z));
        for (int x = 66; x <= 98; x += 2) for (int y = BF + 1; y <= BF + 6; y++) RACKS.add(new BlockPos(x, y, BZ2 - 1));
    }

    static List<BlockPos> lightsGrid(int x1, int y, int z1, int x2, int z2) {
        List<BlockPos> out = new ArrayList<>();
        for (int x = x1; x <= x2; x += 3) for (int z = z1; z <= z2; z += 3) out.add(new BlockPos(x, y, z));
        return out;
    }

    public static boolean owner(ServerPlayer p) {
        String n = p.getName().getString();
        for (String o : OWNERS) if (o.equalsIgnoreCase(n)) return true;
        return false;
    }

    static boolean loaded(ServerLevel sl) {
        for (int x = BX1 - 4; x <= BX2 + 4; x += 8) for (int z = BZ1; z <= 28; z += 8) if (sl.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) return false;
        return true;
    }

    // ---------------------------------------------------------------- build

    public static void build(ServerLevel sl) {
        Builder b = new Builder(sl);
        prepare(b);
        base(b);
        shafts(b);
        house(b, sl);
        roof(b);
        pool(b);
        crest(b);
        b.sign(80, F + 2, Z1 - 1, "minecraft:dark_oak_wall_sign[facing=north]", "§b§l✦ STELLAR ✦", "§f§lHOUSE", "§7home of", "§bStellarFox1");
        FireheartCity.LOG.info("Built StellarFox1's house and secret base (" + b.placed + " blocks)");
    }

    static void prepare(Builder b) {
        for (int y = F; y <= 120; y += 8) b.air(X1 - 4, y, Z1 - 2, X2 + 4, Math.min(120, y + 7), Z2 + 5);
        b.fill(X1 - 3, F - 1, Z1 - 2, X2 + 3, F - 1, Z2 + 1, "minecraft:polished_deepslate");
        b.fill(X1 - 3, F, Z1 - 2, X2 + 3, F, Z2 + 1, FLOOR);
        for (int[] c : new int[][]{{X1 + 2, Z2 - 1}, {X2 - 2, Z2 - 1}, {82, Z2 - 1}, {X1 + 2, 14}, {X2 - 2, 14}}) {
            for (int y = 60; y < F - 1; y++) {
                b.set(c[0], y, c[1], y % 4 == 0 ? GLOW : WALL);
                b.set(c[0] + 1, y, c[1], VNEON);
            }
        }
        for (int x = X1 - 3; x <= X2 + 3; x += 2) b.set(x, F - 2, Z2 + 1, "betterblockz:cyberlight_bar_11[facing=down]|minecraft:light_blue_stained_glass");
    }

    static void base(Builder b) {
        for (int y = BF; y <= BC; y += 4) b.fill(BX1, y, BZ1, BX2, Math.min(BC, y + 3), BZ2, WALL);
        b.air(BX1 + 1, BF + 1, BZ1 + 1, BX2 - 1, BC - 1, BZ2 - 1);
        b.fill(BX1 + 1, BF, BZ1 + 1, BX2 - 1, BF, BZ2 - 1, FLOOR);
        for (int x = 72; x <= 88; x++) b.set(x, BF, 14, x % 3 == 0 ? CIRCUIT : ARROWS);
        for (int z = 9; z <= 19; z++) b.set(80, BF, z, CIRCUIT);
        b.fill(BX1 + 1, BC, BZ1 + 1, BX2 - 1, BC, BZ2 - 1, DARK);
        b.fill(70, BC, 12, 96, BC, 16, AURORA);
        RandomSource r = RandomSource.create(42);
        for (int i = 0; i < 40; i++) b.set(BX1 + 2 + r.nextInt(BX2 - BX1 - 3), BC, BZ1 + 2 + r.nextInt(BZ2 - BZ1 - 3), STAR);
        for (int x = BX1 + 1; x <= BX2 - 1; x += 4) {
            b.fill(x, BF + 1, BZ1 + 1, x, BC - 1, BZ1 + 1, VNEON);
            b.fill(x, BF + 1, BZ2 - 1, x, BC - 1, BZ2 - 1, VNEON);
        }
        for (int x = BX1 + 1; x <= BX2 - 1; x++) {
            b.set(x, BF + 1, BZ1 + 1, "betterblockz:cyberlight_bar_11[facing=up]|minecraft:light_blue_carpet");
        }
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            int x = CORE.getX() + dx, z = CORE.getZ() + dz;
            if (d > 3.4) continue;
            if (d > 2.5) b.fill(x, BF + 1, z, x, BC - 1, z, TINT);
            else if (d < 0.5) b.fill(x, BF + 1, z, x, BC - 1, z, CYCLE);
            else if (d < 1.5 && (dx == 0 || dz == 0)) b.fill(x, BF + 1, z, x, BC - 1, z, VNEON);
            b.set(x, BF, z, d > 2.5 ? GLOW : "betterblockz:cyberlight_secret_blockz_12[lit=true]|minecraft:sea_lantern");
        }
        for (int x = 76; x <= 96; x += 4) for (int y = BF + 3; y <= BF + 5; y++) b.set(x, y, BZ1 + 2, "fireheartcity:tv[facing=south,on=true]");
        for (int x = 76; x <= 96; x += 4) {
            b.set(x, BF + 1, BZ1 + 4, "betterblockz:zeon_white_blockz_8|minecraft:white_concrete");
            b.set(x, BF + 2, BZ1 + 4, "fireheartcity:computer[facing=north]|minecraft:crafting_table");
            b.set(x, BF + 1, BZ1 + 5, "create:light_blue_seat|minecraft:light_blue_carpet");
        }
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) b.set(HOLO.getX() + dx, BF + 1, HOLO.getZ() + dz, dx == 0 && dz == 0 ? GLOW : "betterblockz:zeon_light_blue_blockz_8|minecraft:light_blue_concrete");
        for (BlockPos p : RACKS) b.set(p.getX(), p.getY(), p.getZ(), (p.getY() + p.getX()) % 2 == 0 ? "minecraft:redstone_lamp" : DARK);
        for (int x = 66; x <= 98; x += 2) b.set(x + 1, BF + 3, BZ2 - 1, "betterblockz:zeon_blue_blockz_2[axis=y]|minecraft:blue_concrete");
        for (int z = 8; z <= 20; z += 3) {
            b.set(BX1 + 10, BF + 1, z, "minecraft:end_rod[facing=east]");
            b.set(BX1 + 10, BF + 4, z, "minecraft:end_rod[facing=east]");
        }
        jet(b, 69, BF + 1, 17);
        b.sign(80, BF + 4, BZ2 - 2, "minecraft:dark_oak_wall_sign[facing=north]", "§b§lSTELLAR", "§b§lCOMMAND", "§7authorised", "§7personnel only");
    }

    static void jet(Builder b, int x, int y, int z) {
        String body = "betterblockz:zeon_blue_blockz_9|minecraft:blue_concrete", white = "minecraft:white_concrete", glass = "minecraft:light_blue_stained_glass";
        b.fill(x - 3, y + 1, z, x + 3, y + 1, z, body);
        b.set(x + 4, y + 1, z, white);
        b.set(x + 1, y + 2, z, glass);
        b.set(x + 2, y + 2, z, glass);
        b.fill(x - 1, y + 1, z - 3, x, y + 1, z + 3, body);
        b.set(x - 1, y + 1, z - 3, white);
        b.set(x - 1, y + 1, z + 3, white);
        b.set(x - 3, y + 2, z, body);
        b.set(x - 3, y + 3, z, white);
        b.set(x - 4, y + 1, z, CYCLE);
        b.fill(x - 2, y, z, x + 2, y, z, "minecraft:iron_bars");
    }

    static void shafts(Builder b) {
        Lift s = LIFTS.get(0);
        for (int y = BF + 1; y <= F; y++) {
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                boolean ring = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                if (y >= BC && ring) b.set(s.x() + dx, y, s.z() + dz, y % 4 == 0 ? "betterblockz:cyberlight_bar_11[facing=up]|minecraft:sea_lantern" : GLASS);
                else if (!ring && y > BF) b.set(s.x() + dx, y, s.z() + dz, "minecraft:air");
            }
        }
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            b.set(s.x() + dx, BF, s.z() + dz, "betterblockz:cyberlight_secret_blockz_12[lit=true]|minecraft:sea_lantern");
            b.set(s.x() + dx, F, s.z() + dz, s.hatch());
        }
        Lift st = LIFTS.get(1);
        b.set(st.x(), F, st.z(), GLOW);
        b.set(st.x(), C1, st.z(), st.hatch());
        for (int y = F + 1; y < C1; y++) for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}}) b.set(st.x() + o[0], y, st.z() + o[1], GLASS);
        Lift bl = LIFTS.get(2);
        b.fill(bl.x() - 2, bl.bottom(), bl.z() - 2, bl.x() + 2, bl.bottom(), bl.z() + 2, FLOOR);
        b.set(bl.x(), bl.bottom(), bl.z(), GLOW);
        for (int y = bl.bottom() + 1; y < F; y++) {
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) b.set(bl.x(), y, bl.z(), "minecraft:air");
                else if (!(dz == 1 && dx == 0 && y <= bl.bottom() + 2)) b.set(bl.x() + dx, y, bl.z() + dz, (y - bl.bottom()) % 4 == 0 ? GLOW : GLASS);
            }
        }
        b.air(bl.x(), bl.bottom() + 1, bl.z() + 1, bl.x(), bl.bottom() + 2, bl.z() + 1);
        b.fill(bl.x() - 1, F, bl.z() - 1, bl.x() + 3, F, bl.z() + 1, FLOOR);
        b.set(bl.x(), F, bl.z(), bl.hatch());
    }

    static void house(Builder b, ServerLevel sl) {
        for (int x = X1; x <= X2; x++) for (int z = Z1; z <= Z2; z++) {
            boolean edgeX = x == X1 || x == X2, edgeZ = z == Z1 || z == Z2;
            if (!edgeX && !edgeZ) continue;
            for (int y = F + 1; y < C1; y++) {
                String s;
                if (z == Z2) s = x % 6 == 0 ? VNEON : GLASS;
                else if (edgeX && edgeZ) s = VNEON;
                else if (y == F + 1 || y == C1 - 1) s = WALL;
                else s = (x + z) % 5 == 0 ? GLASS : WALL;
                b.set(x, y, z, s);
            }
        }
        b.fill(X1, C1, Z1, X2, C1, Z2, "minecraft:polished_deepslate");
        for (Room r : ROOMS) if (r.y1() < C1 && r.y1() > BC) for (BlockPos p : r.lights()) b.set(p.getX(), p.getY(), p.getZ(), STAR);
        for (int z = Z1 + 1; z < Z2; z++) {
            for (int y = F + 1; y < C1; y++) {
                boolean opening = z >= 13 && z <= 15 && y <= F + 3;
                b.set(75, y, z, opening ? "minecraft:air" : (y == F + 1 ? "betterblockz:cyberlight_bar_11[facing=up]|minecraft:light_blue_stained_glass" : GLASS));
                b.set(88, y, z, opening ? "minecraft:air" : (y == F + 1 ? "betterblockz:cyberlight_bar_11[facing=up]|minecraft:light_blue_stained_glass" : GLASS));
            }
        }
        for (int x = 77; x <= 87; x++) {
            if (x >= 80 && x <= 84) continue;
            b.set(x, F + 1, 9, WALL_L);
            if (x == 78 || x == 86) b.set(x, F + 2, 9, "fireheartcity:tv[facing=south,on=true]");
        }
        for (int x = 77; x <= 87; x++) b.set(x, F + 4, 8, x % 2 == 0 ? GLOW : WALL);
        for (int x = 78; x <= 86; x++) b.set(x, F + 1, 16, x == 78 || x == 86 ? "another_furniture:blue_sofa[facing=north,type=single]|create:blue_seat" : "another_furniture:light_blue_sofa[facing=north,type=single]|create:light_blue_seat");
        for (int z = 13; z <= 15; z++) b.set(77, F + 1, z, "another_furniture:blue_sofa[facing=east,type=single]|create:blue_seat");
        b.fill(80, F + 1, 12, 84, F + 1, 13, "betterblockz:zeon_white_blockz_8|minecraft:white_concrete");
        for (int x = 90; x <= 96; x++) {
            b.set(x, F + 1, 8, x % 3 == 0 ? "farmersdelight:stove[facing=south,lit=true]|minecraft:smoker[facing=south]" : "betterblockz:zeon_white_blockz_0|minecraft:white_concrete");
            b.set(x, F + 2, 8, x % 3 == 1 ? "farmersdelight:cutting_board[facing=south]|minecraft:air" : "minecraft:air");
            b.set(x, F + 3, 8, "betterblockz:zeon_light_blue_blockz_0|minecraft:light_blue_concrete");
        }
        b.fill(91, F + 1, 13, 95, F + 1, 14, "betterblockz:zeon_white_blockz_8|minecraft:white_concrete");
        for (int x = 91; x <= 95; x += 2) b.set(x, F + 1, 16, "create:blue_seat|minecraft:blue_carpet");
        b.set(96, F + 1, 12, "minecraft:iron_block");
        b.set(96, F + 2, 12, "minecraft:iron_block");
        b.set(68, F + 1, 18, "fireheartcity:computer[facing=north]|minecraft:crafting_table");
        b.set(68, F + 1, 17, "betterblockz:zeon_white_blockz_8|minecraft:white_concrete");
        b.set(69, F + 1, 17, "create:blue_seat|minecraft:blue_carpet");
        for (int z = 14; z <= 19; z++) b.set(67, F + 1, z, "minecraft:bookshelf");
        for (int z = 14; z <= 19; z++) b.set(67, F + 2, z, "minecraft:bookshelf");
        b.set(73, F + 1, 18, "minecraft:potted_warped_fungus");
        for (int x = X1 - 1; x <= X2 + 1; x++) {
            b.set(x, F, Z1 - 1, GLOW);
            b.set(x, F, Z2 + 1, GLOW);
        }
        for (int z = Z1 - 1; z <= Z2 + 1; z++) {
            b.set(X1 - 1, F, z, GLOW);
            b.set(X2 + 1, F, z, GLOW);
        }
        int ux1 = 70, ux2 = 94, uz1 = 9, uz2 = 19;
        for (int x = ux1; x <= ux2; x++) for (int z = uz1; z <= uz2; z++) {
            boolean edge = x == ux1 || x == ux2 || z == uz1 || z == uz2;
            if (!edge) continue;
            for (int y = C1 + 1; y < R; y++) {
                String s = z == uz2 ? (x % 5 == 0 ? VNEON : GLASS) : (x == ux1 || x == ux2) && (z == uz1 || z == uz2) ? VNEON : y == C1 + 1 ? WALL : (x + y) % 4 == 0 ? GLASS : WALL;
                b.set(x, y, z, s);
            }
        }
        b.fill(ux1, R, uz1, ux2, R, uz2, DARK);
        RandomSource r = RandomSource.create(7);
        for (int i = 0; i < 45; i++) b.set(ux1 + 1 + r.nextInt(ux2 - ux1 - 1), R, uz1 + 1 + r.nextInt(uz2 - uz1 - 1), i % 5 == 0 ? GLOW : STAR);
        b.set(80, C1 + 1, 11, "minecraft:light_blue_bed[facing=south,part=head]");
        b.set(80, C1 + 1, 12, "minecraft:light_blue_bed[facing=south,part=foot]");
        b.set(81, C1 + 1, 11, "minecraft:light_blue_bed[facing=south,part=head]");
        b.set(81, C1 + 1, 12, "minecraft:light_blue_bed[facing=south,part=foot]");
        b.set(79, C1 + 1, 10, "another_furniture:light_blue_lamp|minecraft:sea_lantern");
        b.set(82, C1 + 1, 10, "another_furniture:light_blue_lamp|minecraft:sea_lantern");
        b.set(86, C1 + 1, 17, "fireheartcity:tv[facing=north,on=true]|minecraft:black_concrete");
        b.fill(84, C1 + 1, 13, 88, C1 + 1, 13, "another_furniture:light_blue_sofa[facing=south,type=single]|create:light_blue_seat");
        b.set(92, C1 + 1, 17, "minecraft:ender_chest[facing=west]");
        b.fill(ux1 - 3, C1, uz2 + 1, ux2 + 3, C1, Z2, FLOOR);
        for (int x = X1; x <= X2; x++) b.set(x, C1 + 1, Z2, x % 2 == 0 ? GLASS : "minecraft:light_blue_stained_glass_pane");
    }

    static void roof(Builder b) {
        for (int x = 70; x <= 94; x++) for (int z = 9; z <= 19; z++) if (x == 70 || x == 94 || z == 9 || z == 19) b.set(x, R + 1, z, "minecraft:light_blue_stained_glass_pane");
        int cx = 88, cz = 14;
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > 4.4) continue;
            b.set(cx + dx, R, cz + dz, d > 3.5 ? GLOW : d > 2.5 ? DARK : d > 1.5 ? "betterblockz:zeon_white_blockz_0|minecraft:white_concrete" : DARK);
        }
        for (int y = R + 1; y <= R + 12; y++) b.set(74, y, 11, y % 3 == 0 ? GLOW : "minecraft:iron_bars");
        b.set(74, R + 13, 11, "minecraft:lightning_rod");
        b.set(74, R + 14, 11, CYCLE);
    }

    static void pool(Builder b) {
        int x1 = 72, x2 = 92, z1 = Z2 + 2, z2 = Z2 + 5;
        b.fill(x1 - 1, F - 1, z1, x2 + 1, F - 1, z2 + 1, "minecraft:polished_deepslate");
        b.fill(x1, F - 1, z1, x2, F - 1, z2, "betterblockz:cyberlight_blockz_11[lit=true]|minecraft:sea_lantern");
        for (int x = x1 - 1; x <= x2 + 1; x++) b.set(x, F, z2 + 1, GLASS);
        for (int z = z1; z <= z2 + 1; z++) {
            b.set(x1 - 1, F, z, FLOOR);
            b.set(x2 + 1, F, z, FLOOR);
        }
        b.fill(x1, F, z1, x2, F, z2, "minecraft:water");
        for (int x = x1 - 1; x <= x2 + 1; x += 3) b.set(x, F - 2, z2 + 1, "betterblockz:cyberlight_bar_11[facing=down]|minecraft:sea_lantern");
    }

    static final String[] FOX = {
            "#...........#",
            "##.........##",
            "#.#.......#.#",
            "#..#######..#",
            "#...........#",
            "#..##...##..#",
            ".#.........#.",
            "..#...#...#..",
            "...#.....#...",
            "....#####....",
            "......#......"};

    static void crest(Builder b) {
        int cz = 12, top = R + 13, x0 = 76;
        for (int row = 0; row < FOX.length; row++) {
            for (int col = 0; col < FOX[row].length(); col++) {
                int x = x0 + 12 - col, y = top - row;
                b.set(x, y, cz, FOX[row].charAt(col) == '#' ? STAR : "betterblockz:zeon_blue_blockz_0|minecraft:black_stained_glass");
            }
        }
        b.set(x0 + 12 - 4, top - 5, cz, GLOW);
        b.set(x0 + 12 - 8, top - 5, cz, GLOW);
        for (int y = R + 1; y < top - FOX.length + 1; y++) b.set(x0 + 6, y, cz, VNEON);
    }

    // ---------------------------------------------------------------- automation

    public static void tick(ServerLevel sl, CityData d) {
        long now = sl.getGameTime();
        if (!d.stellarHome) {
            if (now % 100 != 53 || !loaded(sl)) return;
            d.stellarHome = true;
            d.setDirty();
            build(sl);
            for (ServerPlayer p : sl.players()) if (owner(p)) p.displayClientMessage(Component.literal("§b✦ ASTRA: §fYour Stellar House is ready above the beach, StellarFox1. §7(/city home)"), false);
            return;
        }
        if (sl.getChunkSource().getChunkNow(82 >> 4, 14 >> 4) == null) return;
        List<ServerPlayer> near = new ArrayList<>();
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(82, 76, 14) < 70 * 70) near.add(p);
        lifts(sl, near, now);
        if (near.isEmpty()) return;
        doors(sl, near);
        if (now % 10 == 0) lights(sl, near);
        if (now % 3 == 0) streak(sl);
        welcome(sl, near, now);
        baseFx(sl, near, now);
        roofFx(sl, near, now);
    }

    static void lifts(ServerLevel sl, List<ServerPlayer> near, long now) {
        for (Lift l : LIFTS) {
            String key = l.name();
            Long closeAt = HATCH_CLOSE_AT.get(key);
            if (closeAt != null && now >= closeAt) {
                boolean blocked = false;
                for (ServerPlayer p : sl.players()) if (inColumn(p, l) && p.getY() > l.top() - 0.5 && p.getY() < l.top() + 1.2) blocked = true;
                if (!blocked) {
                    hatch(sl, l, false);
                    HATCH_CLOSE_AT.remove(key);
                }
            }
        }
        for (ServerPlayer p : near) {
            Integer riding = RIDING.get(p.getUUID());
            if (riding != null) {
                Lift l = LIFTS.get(Math.abs(riding) - 1);
                boolean up = riding > 0;
                p.fallDistance = 0;
                if (up) {
                    if (p.getY() >= l.top() + 1.15) {
                        p.removeEffect(MobEffects.LEVITATION);
                        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30, 0, false, false));
                        hatch(sl, l, false);
                        RIDING.remove(p.getUUID());
                        sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.6f, 1.6f);
                    } else if (!p.hasEffect(MobEffects.LEVITATION)) p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 200, l.top() - l.bottom() > 10 ? 9 : 4, false, false));
                } else {
                    if (p.getY() < l.bottom() + 5 && !p.hasEffect(MobEffects.SLOW_FALLING)) p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false));
                    if (p.onGround() && p.getY() < l.bottom() + 1.6) {
                        RIDING.remove(p.getUUID());
                        HATCH_CLOSE_AT.put(l.name(), now + 20);
                        sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.6f, 1.2f);
                    }
                }
                if (now % 2 == 0) sl.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 3, 0.4, 0.8, 0.4, 0.02);
                if (!inColumn(p, l) && Math.abs(p.getY() - l.top()) > 2) {
                    RIDING.remove(p.getUUID());
                    p.removeEffect(MobEffects.LEVITATION);
                }
                continue;
            }
            for (int i = 0; i < LIFTS.size(); i++) {
                Lift l = LIFTS.get(i);
                if (!inColumn(p, l)) continue;
                boolean atBottom = p.onGround() && Math.abs(p.getY() - (l.bottom() + 1)) < 0.6;
                boolean atTop = p.onGround() && Math.abs(p.getY() - (l.top() + 1)) < 0.6;
                if (l.secret() && (atBottom || atTop && p.isShiftKeyDown()) && !owner(p)) {
                    if (now % 40 == 0) p.displayClientMessage(Component.literal("§c✦ ASTRA: Access denied."), true);
                    continue;
                }
                if (atBottom && !p.isShiftKeyDown()) {
                    hatch(sl, l, true);
                    RIDING.put(p.getUUID(), i + 1);
                    p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 200, l.top() - l.bottom() > 10 ? 9 : 4, false, false));
                    sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8f, 1.8f);
                    if (l.secret()) p.displayClientMessage(Component.literal("§b✦ ASTRA: §fGoing up. Welcome back to the surface."), true);
                } else if (atTop && p.isShiftKeyDown()) {
                    hatch(sl, l, true);
                    RIDING.put(p.getUUID(), -(i + 1));
                    sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8f, 0.8f);
                    if (l.secret()) {
                        p.displayClientMessage(Component.literal("§b✦ ASTRA: §fIdentity confirmed. Descending to Stellar Command."), true);
                        sl.playSound(null, p.blockPosition(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.BLOCKS, 1f, 1f);
                    }
                }
            }
        }
    }

    static boolean inColumn(ServerPlayer p, Lift l) {
        return Math.abs(p.getX() - (l.x() + 0.5)) <= l.r() + 0.45 && Math.abs(p.getZ() - (l.z() + 0.5)) <= l.r() + 0.45 && p.getY() > l.bottom() - 1 && p.getY() < l.top() + 2.5;
    }

    static void hatch(ServerLevel sl, Lift l, boolean open) {
        Builder b = new Builder(sl);
        for (int dx = -l.r(); dx <= l.r(); dx++) for (int dz = -l.r(); dz <= l.r(); dz++) {
            BlockPos p = new BlockPos(l.x() + dx, l.top(), l.z() + dz);
            if (open) {
                sl.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 4, 0.3, 0.1, 0.3, 0.05);
            } else b.set(p.getX(), p.getY(), p.getZ(), l.hatch());
        }
        sl.playSound(null, new BlockPos(l.x(), l.top(), l.z()), open ? SoundEvents.PISTON_CONTRACT : SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.6f, open ? 1.4f : 1.2f);
    }

    static void doors(ServerLevel sl, List<ServerPlayer> near) {
        Builder b = null;
        for (int i = 0; i < DOORS.size(); i++) {
            Door dr = DOORS.get(i);
            double cx = (dr.x1() + dr.x2()) / 2.0 + 0.5, cy = dr.y1() + 1, cz = (dr.z1() + dr.z2()) / 2.0 + 0.5;
            boolean want = false;
            for (ServerPlayer p : near) if (p.distanceToSqr(cx, cy, cz) < 3.6 * 3.6) want = true;
            boolean open = DOOR_OPEN.getOrDefault(i, false);
            if (want == open) continue;
            DOOR_OPEN.put(i, want);
            if (b == null) b = new Builder(sl);
            for (int x = dr.x1(); x <= dr.x2(); x++) for (int y = dr.y1(); y <= dr.y2(); y++) for (int z = dr.z1(); z <= dr.z2(); z++) b.set(x, y, z, want ? "minecraft:air" : dr.closed());
            sl.playSound(null, cx, cy, cz, want ? SoundEvents.PISTON_CONTRACT : SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.4f, 1.7f);
            sl.sendParticles(ParticleTypes.END_ROD, cx, cy + 0.5, cz, 6, 1, 1, 0.2, 0.01);
        }
    }

    static void lights(ServerLevel sl, List<ServerPlayer> near) {
        Builder b = new Builder(sl);
        boolean night = Math.floorMod(sl.getDayTime(), 24000L) > 12500 && Math.floorMod(sl.getDayTime(), 24000L) < 23500;
        for (Room r : ROOMS) {
            boolean occupied = false;
            for (ServerPlayer p : near) if (p.getX() >= r.x1() - 0.5 && p.getX() <= r.x2() + 1.5 && p.getY() >= r.y1() - 1 && p.getY() <= r.y2() + 1 && p.getZ() >= r.z1() - 0.5 && p.getZ() <= r.z2() + 1.5) occupied = true;
            boolean want = occupied || (night && !r.name().equals("base"));
            Boolean was = ROOM_LIT.get(r.name());
            if (was != null && was == want) continue;
            ROOM_LIT.put(r.name(), want);
            for (BlockPos p : r.lights()) b.set(p.getX(), p.getY(), p.getZ(), want ? STAR : OFF);
            if (occupied && was != null) sl.playSound(null, r.x1() + (r.x2() - r.x1()) / 2.0, r.y1() + 2, r.z1() + (r.z2() - r.z1()) / 2.0, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.25f, 2f);
        }
    }

    static void streak(ServerLevel sl) {
        Builder b = new Builder(sl);
        int n = STREAK.size();
        for (int k = 0; k < 3; k++) {
            int head = (streakHead + k * n / 3) % n;
            int tail = Math.floorMod(head - 6, n);
            BlockPos h = STREAK.get(head), t = STREAK.get(tail);
            b.set(h.getX(), h.getY(), h.getZ(), STAR);
            b.set(t.getX(), t.getY(), t.getZ(), GLOW);
        }
        streakHead = (streakHead + 1) % n;
    }

    static void welcome(ServerLevel sl, List<ServerPlayer> near, long now) {
        for (ServerPlayer p : near) {
            boolean onSite = p.getX() > X1 - 6 && p.getX() < X2 + 6 && p.getZ() > BZ1 - 4 && p.getZ() < Z2 + 8 && p.getY() > BF - 2 && p.getY() < R + 20;
            Long last = WELCOMED.get(p.getUUID());
            if (!onSite) {
                if (last != null && now - last > 1200) WELCOMED.remove(p.getUUID());
                continue;
            }
            if (last != null) {
                WELCOMED.put(p.getUUID(), now);
                continue;
            }
            WELCOMED.put(p.getUUID(), now);
            String n = p.getName().getString();
            if (owner(p)) {
                p.displayClientMessage(Component.literal("§b✦ ASTRA: §fWelcome home, " + n + ". All systems nominal."), true);
                sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 1.2f);
                sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.6f, 1.5f);
            } else p.displayClientMessage(Component.literal("§b✦ ASTRA: §fHello, " + n + ". StellarFox1 is expecting you."), true);
        }
    }

    static void baseFx(ServerLevel sl, List<ServerPlayer> near, long now) {
        boolean anyInBase = false;
        for (ServerPlayer p : near) if (p.getY() < BC + 2 && p.getY() > BF - 1 && p.getX() > BX1 && p.getX() < BX2 && p.getZ() > BZ1 && p.getZ() < BZ2) anyInBase = true;
        RandomSource r = sl.random;
        if (!anyInBase) {
            if (now % 20 == 0) guard(sl, now);
            return;
        }
        double cx = CORE.getX() + 0.5, cz = CORE.getZ() + 0.5;
        for (int ring = 0; ring < 3; ring++) {
            double a = now * (0.12 + ring * 0.05) + ring * 2.1;
            double y = BF + 2 + ((now * 0.15 + ring * 3.3) % 9);
            for (int k = 0; k < 6; k++) {
                double aa = a + k * Math.PI / 3;
                sl.sendParticles(new DustParticleOptions(new Vector3f(0.35f, 0.8f, 1f), 1.1f), cx + Math.cos(aa) * 4.2, y, cz + Math.sin(aa) * 4.2, 1, 0, 0, 0, 0);
            }
        }
        if (now % 4 == 0) sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx, BF + 2 + r.nextInt(9), cz, 3, 0.4, 0.4, 0.4, 0.05);
        if (now % 50 == 0) sl.playSound(null, CORE.above(4), SoundEvents.CONDUIT_AMBIENT, SoundSource.BLOCKS, 1.2f, 0.7f);
        if (now % 80 == 20) sl.playSound(null, CORE.above(4), SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 1f, 0.6f);
        if (now % 7 == 0) {
            BlockPos rack = RACKS.get(r.nextInt(RACKS.size()));
            BlockState s = sl.getBlockState(rack);
            if (s.is(Blocks.REDSTONE_LAMP)) sl.setBlock(rack, s.setValue(RedstoneLampBlock.LIT, !s.getValue(RedstoneLampBlock.LIT)), 2 | 16);
        }
        if (now % 23 == 0) sl.playSound(null, new BlockPos(84, BF + 2, BZ1 + 4), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.25f, 1.2f + r.nextFloat());
        if (now % 10 == 0) hologram(sl, near);
        lasers(sl, near, now);
    }

    static void lasers(ServerLevel sl, List<ServerPlayer> near, long now) {
        int lx = BX1 + 10;
        for (ServerPlayer p : near) if (owner(p) && Math.abs(p.getX() - lx) < 6 && p.getY() < BC) {
            if (now > laserOffUntil) {
                p.displayClientMessage(Component.literal("§a✦ ASTRA: Access granted."), true);
                sl.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.7f, 1.8f);
            }
            laserOffUntil = now + 60;
        }
        if (now < laserOffUntil || now % 2 != 0) return;
        DustParticleOptions red = new DustParticleOptions(new Vector3f(1f, 0.05f, 0.1f), 0.7f);
        for (int y = 0; y < 2; y++) for (double z = BZ1 + 1.5; z < BZ2 - 0.5; z += 0.5) sl.sendParticles(red, lx + 0.5, BF + 1.5 + y * 3, z, 1, 0, 0, 0, 0);
    }

    static void guard(ServerLevel sl, long now) {
        AABB box = new AABB(BX1, BF, BZ1, BX2 + 1, BC, BZ2 + 1);
        List<Mob> intruders = sl.getEntitiesOfClass(Mob.class, box, m -> m instanceof Enemy && m.isAlive());
        if (intruders.isEmpty()) return;
        if (now > alarmUntil) {
            alarmUntil = now + 200;
            for (ServerPlayer p : sl.players()) if (owner(p)) p.displayClientMessage(Component.literal("§c⚠ ASTRA: Intruder in Stellar Command! Countermeasures active."), false);
        }
        for (Mob m : intruders) {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, m.getX(), m.getY() + 1, m.getZ(), 20, 0.3, 0.6, 0.3, 0.2);
            sl.playSound(null, m.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 0.6f, 1.8f);
            m.hurt(sl.damageSources().magic(), 6f);
        }
    }

    static void hologram(ServerLevel sl, List<ServerPlayer> near) {
        double ox = HOLO.getX() + 0.5, oy = BF + 2.3, oz = HOLO.getZ() + 0.5;
        int span = 160, step = 10;
        double scale = 3.0 / span;
        for (int x = -span; x <= span; x += step) for (int z = -span; z <= span; z += step) {
            int wx = 10 + x, wz = 40 + z;
            if (sl.getChunkSource().getChunkNow(wx >> 4, wz >> 4) == null) continue;
            int h = sl.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz);
            BlockState top = sl.getBlockState(new BlockPos(wx, h - 1, wz));
            boolean water = top.getFluidState().isSource();
            float hh = Math.max(0, h - 62) / 60f;
            Vector3f col = water ? new Vector3f(0.1f, 0.35f, 1f) : h > 76 ? new Vector3f(0.75f, 0.9f, 1f) : new Vector3f(0.2f, 0.9f, 0.9f);
            double px = ox + x * scale, pz = oz + z * scale, py = oy + hh * 1.2;
            for (ServerPlayer p : near) if (p.distanceToSqr(ox, oy, oz) < 24 * 24) sl.sendParticles(p, new DustParticleOptions(col, 0.35f), false, px, py, pz, 1, 0, 0, 0, 0);
        }
        CityData d = CityData.get(sl);
        for (CityData.Profile pr : d.profiles.values()) {
            if (pr.entity == null || !(sl.getEntity(pr.entity) instanceof Resident res) || res.getY() > 150) continue;
            double px = ox + (res.getX() - 10) * scale, pz = oz + (res.getZ() - 40) * scale;
            if (Math.abs(px - ox) > 3 || Math.abs(pz - oz) > 3) continue;
            for (ServerPlayer p : near) if (p.distanceToSqr(ox, oy, oz) < 24 * 24) sl.sendParticles(p, new DustParticleOptions(new Vector3f(1f, 0.85f, 0.2f), 0.6f), false, px, oy + 1.4, pz, 1, 0, 0, 0, 0);
        }
        for (ServerPlayer pl : sl.players()) {
            double px = ox + (pl.getX() - 10) * scale, pz = oz + (pl.getZ() - 40) * scale;
            if (Math.abs(px - ox) > 3 || Math.abs(pz - oz) > 3) continue;
            for (ServerPlayer p : near) if (p.distanceToSqr(ox, oy, oz) < 24 * 24) sl.sendParticles(p, new DustParticleOptions(new Vector3f(1f, 0.2f, 0.9f), 0.9f), false, px, oy + 1.7, pz, 1, 0, 0, 0, 0);
        }
    }

    static void roofFx(ServerLevel sl, List<ServerPlayer> near, long now) {
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        boolean night = tod > 12500 && tod < 23500;
        if (now % 3 == 0) {
            double a = now * 0.03;
            for (int i = 0; i < 12; i++) {
                double aa = a + i * Math.PI / 6;
                sl.sendParticles(ParticleTypes.END_ROD, 88.5 + Math.cos(aa) * 3.2, R + 1.2, 14.5 + Math.sin(aa) * 3.2, 1, 0, 0, 0, 0);
            }
        }
        if (night && now % 2 == 0) {
            for (int beam = 0; beam < 2; beam++) {
                double a = now * 0.02 + beam * Math.PI;
                Vec3 dir = new Vec3(Math.cos(a) * 0.5, 1, Math.sin(a) * 0.5).normalize();
                for (int s = 2; s < 60; s += 3) {
                    Vec3 q = new Vec3(74.5, R + 15, 11.5).add(dir.scale(s));
                    for (ServerPlayer p : near) sl.sendParticles(p, ParticleTypes.END_ROD, true, q.x, q.y, q.z, 1, 0, 0, 0, 0);
                }
            }
        }
        if (now % 40 == 0) {
            for (int i = 0; i < 6; i++) sl.sendParticles(ParticleTypes.END_ROD, 76 + sl.random.nextInt(18), C1 + 1 + sl.random.nextInt(4), 10 + sl.random.nextInt(9), 1, 0.1, 0.1, 0.1, 0.005);
        }
    }

    public static String tp(ServerPlayer p) {
        p.teleportTo(p.serverLevel(), 82.5, F + 1, 18.5, 180, 10);
        return "§b✦ ASTRA: §fWelcome to the Stellar House.";
    }
}
