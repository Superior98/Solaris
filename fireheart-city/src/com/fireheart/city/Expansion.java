package com.fireheart.city;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Solaris East: the island expansion east of the library - a new avenue with the police station, the fire station,
 * the research lab, the cinema and the Sky Launch tower (moved inside the city). Built once, when the area is loaded.
 */
public final class Expansion {
    private Expansion() {}

    public static final int X1 = 41, X2 = 106, Z1 = -62, Z2 = 6, G = 70;
    public static final BlockPos BOOTH_TV = new BlockPos(82, 78, -16);
    public static final BlockPos TOWER = new BlockPos(98, 70, -44);
    static final BlockPos OLD_PAD = new BlockPos(-86, 71, 145);

    public static boolean ready(ServerLevel sl) {
        for (int cx = X1 >> 4; cx <= X2 >> 4; cx++) for (int cz = Z1 >> 4; cz <= Z2 >> 4; cz++) if (sl.getChunkSource().getChunkNow(cx, cz) == null) return false;
        return true;
    }

    public static void tick(ServerLevel sl, CityData d) {
        if (!d.expanded && ready(sl)) {
            d.expanded = true;
            d.setDirty();
            long t0 = System.currentTimeMillis();
            Builder b = new Builder(sl);
            ground(sl, b);
            roads(b);
            police(b);
            fire(b);
            lab(b);
            cinema(b);
            SkyTower.build(sl, d, TOWER);
            d.skySearched = true;
            d.news(Calendar.worldDay(sl), "Solaris East opened: a new police station, fire station, research lab, cinema and the Sky Launch tower!");
            d.event(Calendar.worldDay(sl), "city", "Solaris East opened with a police station, fire station, research lab and a cinema", new BlockPos(75, 71, -26));
            FireheartCity.LOG.info("Built Solaris East (" + b.placed + " blocks, " + (System.currentTimeMillis() - t0) + " ms)");
        }
        if (d.expanded && !d.oldTowerGone && sl.getChunkSource().getChunkNow(OLD_PAD.getX() >> 4, OLD_PAD.getZ() >> 4) != null) {
            d.oldTowerGone = true;
            d.setDirty();
            if (sl.getBlockState(OLD_PAD).is(FireheartCity.LAUNCH_PAD.get())) SkyTower.demolish(sl, d, OLD_PAD);
        }
    }

    static void ground(ServerLevel sl, Builder b) {
        for (int x = X1; x <= X2; x++) for (int z = Z1; z <= Z2; z++) {
            int top = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            for (int y = G + 1; y <= Math.max(top + 2, G + 12); y++) if (!sl.getBlockState(new BlockPos(x, y, z)).isAir()) b.set(x, y, z, "minecraft:air");
            for (int y = G - 1; y >= G - 12; y--) {
                var s = sl.getBlockState(new BlockPos(x, y, z));
                if (s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty()) b.set(x, y, z, "minecraft:dirt");
                else break;
            }
            b.set(x, G, z, "minecraft:grass_block");
        }
        for (int x = X1; x <= X2; x++) {
            wall(sl, b, x, Z1, x, Z1 - 1);
            wall(sl, b, x, Z2, x, Z2 + 1);
        }
        for (int z = Z1; z <= Z2; z++) wall(sl, b, X2, z, X2 + 1, z);
    }

    static void wall(ServerLevel sl, Builder b, int x, int z, int ox, int oz) {
        int out = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ox, oz) - 1;
        if (out > G + 1) b.fill(x, G + 1, z, x, Math.min(out, G + 30), z, "minecraft:stone_bricks");
    }

    static void roads(Builder b) {
        b.fill(31, G, -28, X2, G, -24, "createdieselgenerators:asphalt_block|minecraft:gray_concrete");
        b.air(31, G + 1, -28, 40, G + 3, -24);
        for (int x = 42; x <= X2; x += 4) b.fill(x, G, -26, x + 1, G, -26, "minecraft:yellow_concrete");
        b.fill(X1, G, -29, X2, G, -29, "minecraft:smooth_stone");
        b.fill(X1, G, -23, X2, G, -23, "minecraft:smooth_stone");
        b.fill(65, G, -52, 68, G, -30, "minecraft:smooth_stone");
        b.fill(63, G, -22, 66, G, 2, "minecraft:smooth_stone");
        b.fill(89, G, -37, 91, G, -30, "minecraft:smooth_stone");
        for (int x = 44; x <= X2 - 2; x += 8) {
            b.lamp(x, G + 1, -30);
            b.lamp(x, G + 1, -22);
        }
        b.sign(42, G + 1, -30, "minecraft:oak_sign[rotation=4]", "§lSolaris East", "Police · Fire", "Lab · Cinema", "Sky Launch →");
    }

    static void police(Builder b) {
        int x1 = 48, x2 = 64, z1 = -52, z2 = -34;
        b.fill(x1, G, z1, x2, G, z2, "minecraft:polished_andesite");
        b.walls(x1, G + 1, z1, x2, G + 2, z2, "minecraft:blue_concrete");
        b.walls(x1, G + 3, z1, x2, G + 6, z2, "minecraft:white_concrete");
        for (int[] c : new int[][]{{x1, z1}, {x1, z2}, {x2, z1}, {x2, z2}}) b.fill(c[0], G + 1, c[1], c[0], G + 7, c[1], "minecraft:light_gray_concrete");
        b.fill(x1, G + 7, z1, x2, G + 7, z2, "minecraft:smooth_stone");
        b.walls(x1, G + 8, z1, x2, G + 8, z2, "minecraft:light_gray_concrete");
        for (int x : new int[]{50, 51, 53, 54, 58, 59, 61, 62}) { b.fill(x, G + 3, z1, x, G + 4, z1, "minecraft:glass_pane"); b.fill(x, G + 3, z2, x, G + 4, z2, "minecraft:glass_pane"); }
        for (int z : new int[]{-50, -49, -46, -45, -41, -40, -37, -36}) { b.fill(x1, G + 3, z, x1, G + 4, z, "minecraft:glass_pane"); b.fill(x2, G + 3, z, x2, G + 4, z, "minecraft:glass_pane"); }
        b.air(55, G + 1, z2, 57, G + 3, z2);
        b.fill(53, G + 4, z2 + 1, 59, G + 4, z2 + 1, "minecraft:smooth_quartz_slab[type=top]");
        b.set(54, G + 3, z2 + 1, "minecraft:soul_lantern[hanging=true]");
        b.set(58, G + 3, z2 + 1, "minecraft:soul_lantern[hanging=true]");
        b.set(53, G + 5, z2, "minecraft:blue_stained_glass");
        b.set(59, G + 5, z2, "minecraft:red_stained_glass");
        b.sign(56, G + 5, z2 + 1, "minecraft:oak_wall_sign[facing=south]", "§9§lSOLARIS", "§9§lPOLICE", "Serve & Protect", "");
        b.set(52, G + 1, z2 + 2, "minecraft:blue_banner[rotation=0]");
        b.set(60, G + 1, z2 + 2, "minecraft:blue_banner[rotation=0]");
        b.fill(52, G + 1, -39, 60, G + 1, -39, "minecraft:polished_andesite");
        b.fill(52, G + 2, -39, 60, G + 2, -39, "minecraft:smooth_quartz_slab[type=bottom]");
        b.set(56, G + 1, -39, "minecraft:oak_fence_gate[facing=south]");
        b.set(56, G + 2, -39, "minecraft:air");
        b.set(54, G + 2, -39, "fireheartcity:computer[facing=south]");
        b.set(58, G + 2, -39, "minecraft:lectern[facing=south]");
        b.set(54, G + 1, -40, "minecraft:spruce_stairs[facing=south]");
        b.set(58, G + 1, -40, "minecraft:spruce_stairs[facing=south]");
        for (int x : new int[]{50, 51, 61, 62}) b.set(x, G + 1, -36, "minecraft:oak_stairs[facing=north]");
        b.set(49, G + 1, -35, "minecraft:potted_bamboo");
        b.set(63, G + 1, -35, "minecraft:potted_bamboo");
        b.fill(49, G + 1, -46, 63, G + 3, -46, "minecraft:iron_bars");
        b.fill(56, G + 1, -51, 56, G + 3, -47, "minecraft:white_concrete");
        b.air(52, G + 1, -46, 52, G + 2, -46);
        b.air(60, G + 1, -46, 60, G + 2, -46);
        b.set(52, G + 1, -46, "minecraft:iron_door[facing=south,half=lower,hinge=left]");
        b.set(52, G + 2, -46, "minecraft:iron_door[facing=south,half=upper,hinge=left]");
        b.set(60, G + 1, -46, "minecraft:iron_door[facing=south,half=lower,hinge=left]");
        b.set(60, G + 2, -46, "minecraft:iron_door[facing=south,half=upper,hinge=left]");
        b.set(50, G + 1, -50, "minecraft:white_bed[facing=north,part=head]");
        b.set(50, G + 1, -49, "minecraft:white_bed[facing=north,part=foot]");
        b.set(62, G + 1, -50, "minecraft:white_bed[facing=north,part=head]");
        b.set(62, G + 1, -49, "minecraft:white_bed[facing=north,part=foot]");
        b.set(54, G + 1, -51, "minecraft:cauldron");
        b.set(58, G + 1, -51, "minecraft:cauldron");
        for (int z = -45; z <= -41; z++) { b.set(63, G + 1, z, "minecraft:iron_trapdoor[facing=west,half=bottom,open=true]"); b.set(63, G + 2, z, "minecraft:iron_trapdoor[facing=west,half=bottom,open=true]"); }
        b.fill(49, G + 1, -44, 50, G + 1, -42, "minecraft:dark_oak_planks");
        b.set(51, G + 1, -43, "minecraft:dark_oak_stairs[facing=west]");
        b.set(49, G + 2, -43, "fireheartcity:computer[facing=east]");
        b.set(49, G + 1, -41, "minecraft:blue_banner[rotation=4]");
        for (int x = 58; x <= 61; x++) { b.set(x, G + 1, -44, "minecraft:barrel[facing=up]"); b.set(x, G + 2, -44, "minecraft:barrel[facing=up]"); }
        for (int x : new int[]{51, 56, 61}) for (int z : new int[]{-49, -43, -37}) b.set(x, G + 6, z, "fireheartcity:ceiling_panel");
        int cx = 43, cz = -43;
        b.fill(cx, G + 1, cz, cx + 2, G + 1, cz + 4, "minecraft:white_concrete");
        for (int[] w : new int[][]{{cx, cz + 1}, {cx + 2, cz + 1}, {cx, cz + 3}, {cx + 2, cz + 3}}) b.set(w[0], G + 1, w[1], "minecraft:black_concrete");
        b.fill(cx, G + 2, cz + 1, cx + 2, G + 2, cz + 3, "minecraft:black_concrete");
        b.fill(cx, G + 2, cz + 2, cx + 2, G + 2, cz + 2, "minecraft:white_concrete");
        b.set(cx + 1, G + 2, cz, "minecraft:white_concrete");
        b.set(cx + 1, G + 2, cz + 4, "minecraft:white_concrete");
        b.fill(cx, G + 3, cz + 1, cx + 2, G + 3, cz + 3, "minecraft:white_concrete");
        b.set(cx, G + 3, cz + 1, "minecraft:glass");
        b.set(cx + 2, G + 3, cz + 1, "minecraft:glass");
        b.set(cx, G + 4, cz + 2, "minecraft:blue_stained_glass");
        b.set(cx + 2, G + 4, cz + 2, "minecraft:red_stained_glass");
        b.set(cx + 1, G + 4, cz + 2, "minecraft:white_concrete");
    }

    static void fire(Builder b) {
        int x1 = 70, x2 = 88, z1 = -52, z2 = -34;
        b.fill(x1, G, z1, x2, G, z2, "minecraft:smooth_stone");
        b.walls(x1, G + 1, z1, x2, G + 9, z2, "minecraft:bricks");
        b.walls(x1, G + 1, z1, x2, G + 1, z2, "minecraft:red_concrete");
        b.walls(x1, G + 5, z1, x2, G + 5, z2, "minecraft:red_concrete");
        b.fill(x1, G + 10, z1, x2, G + 10, z2, "minecraft:smooth_stone");
        b.walls(x1, G + 11, z1, x2, G + 11, z2, "minecraft:red_nether_bricks");
        b.air(72, G + 1, z2, 76, G + 4, z2);
        b.air(80, G + 1, z2, 84, G + 4, z2);
        b.fill(72, G + 4, z2, 76, G + 4, z2, "minecraft:red_stained_glass_pane");
        b.fill(80, G + 4, z2, 84, G + 4, z2, "minecraft:red_stained_glass_pane");
        for (int x : new int[]{72, 74, 76, 80, 82, 84}) b.fill(x, G + 7, z2, x, G + 8, z2, "minecraft:glass_pane");
        for (int z = -50; z <= -36; z += 3) { b.fill(x1, G + 7, z, x1, G + 8, z, "minecraft:glass_pane"); b.fill(x2, G + 7, z, x2, G + 8, z, "minecraft:glass_pane"); }
        b.sign(78, G + 6, z2 + 1, "minecraft:oak_wall_sign[facing=south]", "§c§lSOLARIS", "§c§lFIRE DEPT", "Station 1", "");
        truck(b, 73, -45);
        truck(b, 81, -45);
        b.fill(x1 + 1, G + 5, z1 + 1, x2 - 1, G + 5, -46, "minecraft:smooth_stone");
        b.air(86, G + 5, -50, 86, G + 5, -50);
        b.fill(86, G + 1, -50, 86, G + 9, -50, "minecraft:end_rod[facing=up]");
        for (int y = G + 1; y <= G + 5; y++) b.set(71, y, -47, "minecraft:ladder[facing=east]");
        b.air(71, G + 5, -47, 71, G + 5, -47);
        b.set(71, G + 5, -47, "minecraft:ladder[facing=east]");
        for (int x = 73; x <= 84; x += 3) {
            b.set(x, G + 6, -50, "minecraft:red_bed[facing=north,part=foot]");
            b.set(x, G + 6, -51, "minecraft:red_bed[facing=north,part=head]");
        }
        b.fill(74, G + 6, -48, 78, G + 6, -48, "minecraft:spruce_slab[type=top]");
        b.set(80, G + 6, -48, "minecraft:jukebox");
        for (int z = -44; z <= -37; z += 2) b.fill(87, G + 2, z, 87, G + 3, z, "minecraft:chain");
        b.fill(87, G + 1, -44, 87, G + 1, -37, "minecraft:red_wool");
        b.set(79, G + 11, -43, "minecraft:iron_bars");
        b.set(79, G + 12, -43, "minecraft:iron_bars");
        b.set(79, G + 13, -43, "minecraft:red_concrete");
        b.set(79, G + 14, -43, "minecraft:bell[attachment=floor,facing=south]");
        for (int x : new int[]{74, 82}) for (int z : new int[]{-44, -38}) b.set(x, G + 4, z, "fireheartcity:ceiling_spot");
        b.set(69, G + 1, -31, "minecraft:red_nether_brick_wall");
        b.set(69, G + 2, -31, "minecraft:red_concrete");
    }

    static void truck(Builder b, int x, int z) {
        b.fill(x, G + 1, z, x + 2, G + 3, z + 7, "minecraft:red_concrete");
        for (int zz : new int[]{z + 1, z + 2, z + 6}) { b.set(x - 1, G + 1, zz, "minecraft:black_concrete"); b.set(x + 3, G + 1, zz, "minecraft:black_concrete"); }
        b.fill(x, G + 3, z + 7, x + 2, G + 3, z + 7, "minecraft:glass");
        b.fill(x, G + 2, z + 7, x + 2, G + 2, z + 7, "minecraft:iron_block");
        b.fill(x + 1, G + 4, z, x + 1, G + 4, z + 5, "minecraft:iron_bars");
        b.set(x, G + 4, z + 7, "minecraft:red_stained_glass");
        b.set(x + 2, G + 4, z + 7, "minecraft:red_stained_glass");
        b.fill(x, G + 2, z + 3, x + 2, G + 2, z + 4, "minecraft:white_concrete");
        b.set(x + 1, G + 1, z - 1, "minecraft:chain");
    }

    static void lab(Builder b) {
        int x1 = 46, x2 = 62, z1 = -20, z2 = -4;
        b.fill(x1, G, z1, x2, G, z2, "minecraft:smooth_quartz");
        b.walls(x1, G + 1, z1, x2, G + 6, z2, "minecraft:white_concrete");
        b.walls(x1, G + 1, z1, x2, G + 1, z2, "minecraft:light_gray_concrete");
        for (int x = 48; x <= 60; x++) if (x < 53 || x > 55) b.fill(x, G + 2, z1, x, G + 5, z1, "minecraft:light_blue_stained_glass_pane");
        for (int z = -18; z <= -6; z += 2) { b.fill(x1, G + 3, z, x1, G + 4, z, "minecraft:light_blue_stained_glass_pane"); b.fill(x2, G + 3, z, x2, G + 4, z, "minecraft:light_blue_stained_glass_pane"); }
        b.fill(x1, G + 7, z1, x2, G + 7, z2, "minecraft:light_gray_concrete");
        b.fill(52, G + 7, -15, 56, G + 7, -9, "minecraft:glass");
        b.air(53, G + 1, z1, 55, G + 3, z1);
        b.sign(54, G + 5, z1 - 1, "minecraft:birch_wall_sign[facing=north]", "§b§lSOLARIS", "§b§lRESEARCH LAB", "Authorised", "staff only");
        b.walls(51, G, -15, 57, G, -9, "minecraft:yellow_concrete");
        b.walls(52, G + 1, -14, 56, G + 5, -10, "minecraft:light_blue_stained_glass");
        b.fill(53, G, -13, 55, G, -11, "minecraft:sea_lantern");
        b.fill(53, G + 1, -13, 55, G + 5, -11, "minecraft:water");
        b.set(54, G + 3, -12, "minecraft:conduit[waterlogged=true]");
        b.fill(52, G + 6, -14, 56, G + 6, -10, "minecraft:iron_block");
        String[] glow = {"minecraft:verdant_froglight", "minecraft:pearlescent_froglight", "minecraft:ochre_froglight", "minecraft:sea_lantern"};
        int k = 0;
        for (int z : new int[]{-18, -15, -9, -6}) {
            b.set(48, G + 1, z, "minecraft:polished_andesite");
            b.fill(48, G + 2, z, 48, G + 4, z, glow[k++ % glow.length]);
            b.set(48, G + 5, z, "minecraft:iron_block");
            b.set(48, G + 6, z, "create:fluid_pipe|minecraft:chain");
            b.set(49, G + 2, z, "minecraft:glass_pane");
            b.set(49, G + 3, z, "minecraft:glass_pane");
            b.set(49, G + 4, z, "minecraft:glass_pane");
        }
        b.fill(48, G + 6, -18, 48, G + 6, -6, "create:fluid_pipe[axis=z]|minecraft:chain[axis=z]");
        b.fill(49, G + 6, -12, 51, G + 6, -12, "create:fluid_pipe|minecraft:chain[axis=x]");
        for (int z : new int[]{-18, -6}) {
            b.fill(60, G + 1, z, 60, G + 3, z, "create:fluid_tank|minecraft:glass");
            b.nbt(60, G + 1, z, z == -18 ? "{TankContent:{FluidName:\"minecraft:lava\",Amount:8000}}" : "{TankContent:{FluidName:\"minecraft:water\",Amount:8000}}");
            b.set(60, G + 4, z, "create:mechanical_pump[facing=up]|minecraft:iron_block");
            b.fill(60, G + 5, z, 60, G + 6, z, "create:fluid_pipe|minecraft:chain");
        }
        b.fill(57, G + 6, -12, 60, G + 6, -12, "create:fluid_pipe|minecraft:chain[axis=x]");
        b.fill(60, G + 6, -17, 60, G + 6, -7, "create:fluid_pipe[axis=z]|minecraft:chain[axis=z]");
        for (int z = -16; z <= -8; z++) b.set(61, G + 1, z, "minecraft:polished_diorite");
        b.set(61, G + 2, -16, "minecraft:brewing_stand");
        b.set(61, G + 2, -15, "minecraft:water_cauldron[level=3]");
        b.set(61, G + 2, -14, "minecraft:lava_cauldron");
        b.set(61, G + 2, -13, "fireheartcity:computer[facing=west]");
        b.set(61, G + 2, -11, "minecraft:powder_snow_cauldron[level=3]");
        b.set(61, G + 2, -10, "minecraft:brewing_stand");
        b.set(61, G + 2, -9, "minecraft:amethyst_cluster[facing=up]");
        b.set(61, G + 2, -8, "minecraft:potted_crimson_fungus");
        for (int z = -16; z <= -8; z += 4) b.set(59, G + 1, z, "minecraft:quartz_stairs[facing=east]");
        for (int x = 50; x <= 58; x += 2) { b.set(x, G + 1, -5, "minecraft:observer[facing=north]"); b.set(x, G + 2, -5, "minecraft:observer[facing=north]"); b.set(x, G + 3, -5, "minecraft:target"); }
        b.fill(51, G + 1, -18, 57, G + 1, -18, "minecraft:polished_diorite");
        b.set(52, G + 2, -18, "fireheartcity:computer[facing=north]");
        b.set(54, G + 2, -18, "minecraft:enchanting_table");
        b.set(56, G + 2, -18, "fireheartcity:computer[facing=north]");
        for (int x : new int[]{49, 59}) for (int z : new int[]{-17, -7}) b.set(x, G + 6, z, "fireheartcity:ceiling_panel");
    }

    static void cinema(Builder b) {
        int x1 = 68, x2 = 96, z1 = -21, z2 = 2;
        b.fill(x1, G, z1, x2, G, z2, "minecraft:blackstone");
        b.walls(x1, G + 1, z1, x2, G + 12, z2, "minecraft:black_concrete");
        b.walls(x1, G + 1, z1, x2, G + 1, z2, "minecraft:red_concrete");
        b.fill(x1, G + 13, z1, x2, G + 13, z2, "minecraft:blackstone");
        b.air(80, G + 1, z1, 84, G + 3, z1);
        b.fill(76, G + 5, z1 - 2, 88, G + 5, z1 - 1, "minecraft:smooth_quartz");
        b.fill(76, G + 4, z1 - 2, 88, G + 4, z1 - 2, "minecraft:ochre_froglight");
        b.fill(76, G + 6, z1 - 1, 88, G + 9, z1 - 1, "minecraft:white_concrete");
        b.fill(75, G + 10, z1 - 1, 89, G + 10, z1 - 1, "minecraft:ochre_froglight");
        b.fill(75, G + 5, z1 - 1, 75, G + 10, z1 - 1, "minecraft:ochre_froglight");
        b.fill(89, G + 5, z1 - 1, 89, G + 10, z1 - 1, "minecraft:ochre_froglight");
        b.sign(80, G + 8, z1 - 2, "minecraft:dark_oak_wall_sign[facing=north]", "§6§lSOLARIS", "§6§lCINEMA", "", "");
        b.sign(82, G + 8, z1 - 2, "minecraft:dark_oak_wall_sign[facing=north]", "§e★ NOW ★", "§e★ SHOWING ★", "Festival of", "the Founder");
        b.sign(84, G + 8, z1 - 2, "minecraft:dark_oak_wall_sign[facing=north]", "§fTonight:", "Solaris Skyline", "Deep Blue", "+ your films");
        b.fill(x1 + 1, G + 1, -20, x2 - 1, G + 1, -15, "minecraft:red_carpet");
        b.fill(70, G + 1, -19, 74, G + 1, -19, "minecraft:dark_oak_planks");
        b.fill(70, G + 2, -19, 74, G + 2, -19, "minecraft:dark_oak_slab[type=bottom]");
        b.set(71, G + 2, -19, "minecraft:cake");
        b.set(73, G + 2, -19, "minecraft:cake");
        b.set(72, G + 1, -20, "minecraft:dark_oak_stairs[facing=south]");
        b.fill(90, G + 1, -19, 94, G + 3, -19, "minecraft:glass");
        b.fill(90, G + 1, -19, 94, G + 1, -19, "minecraft:dark_oak_planks");
        b.sign(92, G + 3, -20, "minecraft:dark_oak_wall_sign[facing=north]", "§lTickets", "Free for", "Solaris", "residents!");
        b.fill(x1 + 1, G + 7, -20, x2 - 1, G + 7, -15, "minecraft:blackstone");
        b.fill(x1 + 1, G + 1, -14, x2 - 1, G + 7, -14, "minecraft:black_concrete");
        b.air(81, G + 1, -14, 83, G + 3, -14);
        for (int x : new int[]{72, 78, 86, 92}) b.set(x, G + 6, -18, "fireheartcity:ceiling_round");
        b.fill(79, G + 8, -20, 85, G + 8, -15, "minecraft:dark_oak_planks");
        b.walls(79, G + 9, -20, 85, G + 11, -15, "minecraft:black_concrete");
        b.fill(80, G + 9, -14, 84, G + 10, -14, "minecraft:tinted_glass");
        b.air(80, G + 9, -19, 84, G + 11, -16);
        b.set(BOOTH_TV.getX(), BOOTH_TV.getY(), BOOTH_TV.getZ(), "fireheartcity:tv[facing=south,on=true]");
        b.set(82, G + 9, -18, "minecraft:dark_oak_stairs[facing=south]");
        for (int y = G + 1; y <= G + 9; y++) b.set(86, y, -16, "minecraft:ladder[facing=west]");
        b.air(86, G + 8, -16, 86, G + 8, -16);
        b.set(86, G + 8, -16, "minecraft:ladder[facing=west]");
        b.air(85, G + 9, -16, 85, G + 10, -16);
        for (int row = 0; row < 5; row++) {
            int z = -12 + row * 2, y = G + (4 - row);
            b.fill(x1 + 1, G + 1, z, x2 - 1, y, z + 1, "minecraft:black_concrete");
            b.fill(x1 + 1, y, z, x2 - 1, y, z + 1, "minecraft:red_carpet|minecraft:black_concrete");
            for (int x = x1 + 2; x <= x2 - 2; x++) {
                if (x >= 81 && x <= 83) continue;
                b.set(x, y + 1, z, "another_furniture:red_sofa[facing=south]|minecraft:crimson_stairs[facing=north]");
            }
            b.fill(81, G + 1, z, 83, y, z + 1, "minecraft:black_concrete");
            b.set(80, y + 1, z + 1, "minecraft:soul_lantern");
            b.set(84, y + 1, z + 1, "minecraft:soul_lantern");
        }
        b.fill(x1 + 1, G + 1, -2, x2 - 1, G + 1, 1, "minecraft:black_concrete");
        b.fill(x1 + 1, G + 1, -2, x2 - 1, G + 1, -2, "minecraft:dark_oak_slab[type=bottom]");
        b.fill(71, G + 3, 1, 93, G + 11, 1, "minecraft:white_concrete");
        b.fill(70, G + 2, 1, 94, G + 2, 1, "minecraft:black_concrete");
        b.fill(70, G + 12, 1, 94, G + 12, 1, "minecraft:black_concrete");
        b.fill(70, G + 2, 0, 70, G + 12, 0, "another_furniture:red_curtain|minecraft:red_wool");
        b.fill(94, G + 2, 0, 94, G + 12, 0, "another_furniture:red_curtain|minecraft:red_wool");
        b.fill(69, G + 2, 0, 69, G + 12, 0, "minecraft:red_wool");
        b.fill(95, G + 2, 0, 95, G + 12, 0, "minecraft:red_wool");
    }
}
