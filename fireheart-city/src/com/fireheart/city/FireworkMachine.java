package com.fireheart.city;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Solaris Firework Machine: an offshore launch island south of the Skyport. Press the big red button (on the island
 * or at the bridge) or wait for a show night, and it runs a fully choreographed two-minute spectacular: power-up and
 * countdown, a ring of fire, sweeping fans, shapes drawn in the sky, flame and water jets, SOLARIS written across the
 * sky, and a finale barrage. Giant particle bursts are sent long-range so the whole city sees them.
 */
public final class FireworkMachine {
    private FireworkMachine() {}

    public static final int CX = -2, CY = 70, CZ = 130, R = 15;
    public static final BlockPos CENTRE = new BlockPos(CX, CY, CZ);
    static final BlockPos BUTTON = new BlockPos(CX, CY + 2, CZ - R + 2);
    static final BlockPos BRIDGE_BUTTON = new BlockPos(-5, 72, 97);
    static final int BRIDGE_Z1 = 96, BRIDGE_Z2 = CZ - R;
    static final int SKY = CY + 70;
    public static final int SHOW_LENGTH = 2400;

    private static final List<BlockPos> MORTARS = new ArrayList<>();
    private static final List<BlockPos> TOWERS = new ArrayList<>();
    private static final List<BlockPos> LAMPS = new ArrayList<>();
    private static long startedAt = -1;
    private static boolean btnPrev, bridgePrev;

    static {
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI * 2 / 12;
            MORTARS.add(new BlockPos(CX + (int) Math.round(Math.cos(a) * (R - 3)), CY + 1, CZ + (int) Math.round(Math.sin(a) * (R - 3))));
        }
        int[][] tw = {{-9, -9}, {9, -9}, {9, 9}, {-9, 9}};
        for (int[] t : tw) TOWERS.add(new BlockPos(CX + t[0], CY + 14, CZ + t[1]));
    }

    public static void reset() {
        startedAt = -1;
        btnPrev = bridgePrev = false;
    }

    public static boolean running() {
        return startedAt >= 0;
    }

    static boolean loaded(ServerLevel sl) {
        for (int x = CX - R; x <= CX + R; x += 16) for (int z = BRIDGE_Z1; z <= CZ + R; z += 16) if (sl.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) return false;
        return sl.getChunkSource().getChunkNow((CX + R) >> 4, (CZ + R) >> 4) != null;
    }

    static void build(ServerLevel sl) {
        Builder b = new Builder(sl);
        String deck = "minecraft:polished_deepslate", trim = "create:industrial_iron_block|minecraft:iron_block", glow = "minecraft:sea_lantern";
        for (int x = CX - R; x <= CX + R; x++) {
            for (int z = CZ - R; z <= CZ + R; z++) {
                double d = Math.sqrt((x - CX) * (x - CX) + (z - CZ) * (z - CZ));
                if (d > R + 0.4) continue;
                String s = d > R - 0.8 ? trim : Math.abs(d - (R - 5)) < 0.5 ? glow : Math.abs(d - 5.5) < 0.5 ? "create:brass_block|minecraft:gold_block" : ((x + z) % 2 == 0 ? deck : "minecraft:polished_blackstone");
                b.set(x, CY, z, s);
                b.fill(x, CY - 1, z, x, CY - 1, z, "minecraft:deepslate_tiles");
                if (d > R - 0.8) b.set(x, CY + 1, z, "minecraft:iron_bars");
                b.air(x, CY + 2, z, x, CY + 30, z);
            }
        }
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            int px = CX + (int) Math.round(Math.cos(a) * (R - 2)), pz = CZ + (int) Math.round(Math.sin(a) * (R - 2));
            b.fill(px, 41, pz, px, CY - 2, pz, "minecraft:deepslate_bricks");
        }
        for (BlockPos m : MORTARS) {
            b.set(m.getX(), CY, m.getZ(), "minecraft:redstone_lamp");
            b.set(m.getX(), CY + 1, m.getZ(), "minecraft:dispenser[facing=up]");
            b.fill(m.getX(), CY + 2, m.getZ(), m.getX(), CY + 3, m.getZ(), "minecraft:iron_bars");
            b.set(m.getX(), CY + 4, m.getZ(), "minecraft:lightning_rod[facing=up]");
        }
        for (BlockPos t : TOWERS) {
            int x = t.getX(), z = t.getZ();
            for (int y = CY + 1; y <= CY + 12; y++) {
                for (int dx = -1; dx <= 1; dx += 2) for (int dz = -1; dz <= 1; dz += 2) b.set(x + dx, y, z + dz, y % 4 == 0 ? "create:brass_casing|minecraft:gold_block" : "minecraft:iron_bars");
                b.set(x, y, z, y % 3 == 0 ? "minecraft:redstone_lamp" : "minecraft:chain");
            }
            b.fill(x - 1, CY + 13, z - 1, x + 1, CY + 13, z + 1, "create:copper_casing|minecraft:cut_copper");
            b.set(x, CY + 14, z, "minecraft:end_rod");
        }
        b.fill(CX - 3, CY + 1, CZ - 3, CX + 3, CY + 1, CZ + 3, "create:brass_casing|minecraft:gold_block");
        b.fill(CX - 2, CY + 2, CZ - 2, CX + 2, CY + 9, CZ + 2, "create:copper_casing|minecraft:cut_copper");
        b.fill(CX - 1, CY + 2, CZ - 3, CX + 1, CY + 4, CZ - 3, "minecraft:tinted_glass");
        b.fill(CX - 1, CY + 10, CZ - 1, CX + 1, CY + 16, CZ + 1, "create:andesite_casing|minecraft:polished_andesite");
        b.set(CX, CY + 17, CZ, "minecraft:campfire[lit=true,signal_fire=true]");
        for (int i = 0; i < 4; i++) {
            int[] o = {0, 3, 0, -3};
            int ox = o[i], oz = o[(i + 1) % 4];
            String axis = ox != 0 ? "x" : "z";
            b.set(CX + ox, CY + 7, CZ + oz, "create:large_cogwheel[axis=" + axis + "]|minecraft:polished_blackstone");
            b.set(CX + ox, CY + 12, CZ + oz, "create:cogwheel[axis=" + axis + "]|minecraft:polished_blackstone");
            b.set(CX + ox * 2, CY + 7, CZ + oz * 2, "minecraft:redstone_lamp");
        }
        for (int[] s : new int[][]{{-6, 0}, {6, 0}}) {
            b.fill(CX + s[0], CY + 1, CZ + s[1] + 5, CX + s[0], CY + 3, CZ + s[1] + 5, "create:fluid_tank|minecraft:copper_block");
            b.fill(CX + s[0], CY + 1, CZ + s[1] - 5, CX + s[0], CY + 3, CZ + s[1] - 5, "create:fluid_tank|minecraft:copper_block");
        }
        b.fill(-3, CY, BRIDGE_Z1, -1, CY, BRIDGE_Z2, "minecraft:polished_deepslate");
        for (int z = BRIDGE_Z1; z <= BRIDGE_Z2; z++) {
            b.set(-4, CY + 1, z, z % 4 == 0 ? "minecraft:sea_lantern" : "minecraft:iron_bars");
            b.set(0, CY + 1, z, z % 4 == 0 ? "minecraft:sea_lantern" : "minecraft:iron_bars");
            b.air(-3, CY + 1, z, -1, CY + 3, z);
            if (z % 8 == 0) b.fill(-4, 41, z, -4, CY - 1, z, "minecraft:deepslate_bricks");
            if (z % 8 == 0) b.fill(0, 41, z, 0, CY - 1, z, "minecraft:deepslate_bricks");
        }
        b.air(-3, CY + 1, BRIDGE_Z2, -1, CY + 1, BRIDGE_Z2 + 1);
        consoles(sl, b);
    }

    static void consoles(ServerLevel sl, Builder b) {
        int bx = BUTTON.getX(), by = BUTTON.getY(), bz = BUTTON.getZ();
        b.set(bx, by - 1, bz, "minecraft:red_concrete");
        b.set(bx, by - 1, bz + 1, "minecraft:redstone_lamp");
        b.set(bx, by, bz + 1, "minecraft:red_concrete");
        b.set(bx, by, bz, "minecraft:crimson_button[face=wall,facing=north,powered=false]");
        b.set(bx, by + 1, bz + 1, "minecraft:red_concrete");
        b.sign(bx, by + 1, bz, "minecraft:crimson_wall_sign[facing=north]", "§c§l✦ LAUNCH ✦", "§6§lFIREWORK", "§6§lSPECTACULAR", "§7press the button");
        int rx = BRIDGE_BUTTON.getX(), ry = BRIDGE_BUTTON.getY(), rz = BRIDGE_BUTTON.getZ();
        b.fill(rx, CY + 1, rz, rx, ry, rz, "minecraft:red_concrete");
        b.set(rx + 1, ry, rz, "minecraft:crimson_button[face=wall,facing=east,powered=false]");
        b.sign(rx + 1, ry + 1, rz, "minecraft:crimson_wall_sign[facing=east]", "§6§lSOLARIS", "§6§lFIREWORK", "§6§lMACHINE", "§c▶ LAUNCH SHOW");
        b.set(rx, ry + 1, rz, "minecraft:red_concrete");
        b.set(rx, ry + 2, rz, "minecraft:redstone_lamp");
    }

    static boolean pressed(ServerLevel sl, BlockPos p) {
        BlockState s = sl.getBlockState(p);
        return s.getBlock() instanceof ButtonBlock && s.getValue(ButtonBlock.POWERED);
    }

    public static void tick(ServerLevel sl, CityData d) {
        long now = sl.getGameTime();
        if (!d.fireworkMachineBuilt) {
            if (now % 100 != 61 || !loaded(sl)) return;
            d.fireworkMachineBuilt = true;
            d.setDirty();
            build(sl);
            FireheartCity.LOG.info("Built the Solaris Firework Machine at " + CENTRE.toShortString());
        }
        if (LAMPS.isEmpty()) lampList();
        if (sl.getChunkSource().getChunkNow(BUTTON.getX() >> 4, BUTTON.getZ() >> 4) != null) {
            if (now % 200 == 83 && !(sl.getBlockState(BUTTON).getBlock() instanceof ButtonBlock)) consoles(sl, new Builder(sl));
            boolean a = pressed(sl, BUTTON);
            if (a && !btnPrev) start(sl, "the big red button");
            btnPrev = a;
        }
        if (sl.getChunkSource().getChunkNow((BRIDGE_BUTTON.getX() + 1) >> 4, BRIDGE_BUTTON.getZ() >> 4) != null) {
            boolean a = pressed(sl, BRIDGE_BUTTON.east());
            if (a && !bridgePrev) start(sl, "the bridge console");
            bridgePrev = a;
        }
        if (now % 20 == 5) autoShow(sl, d);
        if (startedAt >= 0) show(sl, d, (int) (now - startedAt));
    }

    static void lampList() {
        for (BlockPos m : MORTARS) LAMPS.add(new BlockPos(m.getX(), CY, m.getZ()));
        for (BlockPos t : TOWERS) for (int y = CY + 3; y <= CY + 12; y += 3) LAMPS.add(new BlockPos(t.getX(), y, t.getZ()));
        int[] o = {0, 3, 0, -3};
        for (int i = 0; i < 4; i++) LAMPS.add(new BlockPos(CX + o[i] * 2, CY + 7, CZ + o[(i + 1) % 4] * 2));
    }

    static void autoShow(ServerLevel sl, CityData d) {
        if (running()) return;
        long day = Calendar.worldDay(sl);
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (tod < Fireworks.SHOW_START || tod > Fireworks.SHOW_START + 200 || d.fireworkShowDay == day) return;
        int wd = Calendar.weekday(day);
        if (wd != 2 && wd != 5 && !"festival".equals(d.civic.policy)) return;
        if (!loaded(sl)) return;
        d.fireworkShowDay = day;
        d.setDirty();
        start(sl, null);
        d.event(day, "show", "the Firework Machine put on a spectacular show over the bay", Fireworks.PIER_SKY);
    }

    public static boolean start(ServerLevel sl, String by) {
        if (running() || !loaded(sl)) return false;
        startedAt = sl.getGameTime();
        CityData d = CityData.get(sl);
        d.fireworkShowDay = Calendar.worldDay(sl);
        d.setDirty();
        FireheartCity.LOG.info("Firework Machine show started" + (by == null ? " (show night)" : " by " + by));
        for (ServerPlayer p : sl.players()) if (near(p, 400)) p.displayClientMessage(Component.literal("§6✦ §eThe Solaris Firework Machine is powering up... §6✦"), true);
        return true;
    }

    public static void stop() {
        startedAt = -1;
    }

    static boolean near(ServerPlayer p, double r) {
        return p.level().dimension() == net.minecraft.world.level.Level.OVERWORLD && p.distanceToSqr(CX, CY + 40, CZ) < r * r;
    }

    // ---------- show ----------

    static final int[] RAINBOW = {0xFF2A2A, 0xFF8A1C, 0xFFE21C, 0x3BFF4A, 0x1CE4FF, 0x3B5BFF, 0xB43BFF, 0xFF3BD2};
    static final int GOLD = 0xFFC83B, WHITE = 0xFFFFFF, SILVER = 0xDDE6FF;

    static void show(ServerLevel sl, CityData d, int t) {
        RandomSource r = sl.random;
        if (t == 0) {
            title(sl, "§6§l✦ SOLARIS ✦", "§eThe Firework Machine Spectacular", 10, 60, 20);
            sound(sl, SoundEvents.BEACON_ACTIVATE, 6f, 0.6f);
        }
        if (t < 100) powerUp(sl, t);
        if (t == 40) countdown(sl, "§c§l3");
        if (t == 60) countdown(sl, "§6§l2");
        if (t == 80) countdown(sl, "§e§l1");
        if (t == 100) {
            countdown(sl, "§f§l✦");
            sound(sl, SoundEvents.GENERIC_EXPLODE, 8f, 0.5f);
            for (BlockPos m : MORTARS) rocket(sl, top(m), Vec3.ZERO, 2, 1, GOLD, WHITE, true, true);
            comet(sl, 0, SKY + 10, 0, 1.3);
        }
        if (t == 122) mega(sl, 0, SKY + 12, 0, 16, Palette.GOLD, 420);
        if (t == 130) mega(sl, 0, SKY + 12, 0, 10, Palette.WHITE, 220);

        if (t >= 150 && t < 520) {
            int k = t - 150;
            if (k % 5 == 0) {
                int i = (k / 5) % MORTARS.size();
                int lap = (k / 5) / MORTARS.size();
                int col = RAINBOW[(i + lap * 3) % RAINBOW.length];
                rocket(sl, top(MORTARS.get(i)), Vec3.ZERO, 2, lap % 2 == 0 ? 0 : 2, col, WHITE, lap > 1, lap > 0);
                lamp(sl, MORTARS.get(i), 10);
            }
            if (k % 120 == 110) ring(sl, SKY, 22, Palette.values()[(k / 120) % Palette.values().length], 180);
            if (k % 60 == 30) mega(sl, (r.nextDouble() - 0.5) * 50, SKY + r.nextInt(20), (r.nextDouble() - 0.5) * 20, 9 + r.nextInt(5), Palette.values()[r.nextInt(Palette.values().length)], 200);
        }

        if (t >= 520 && t < 900) {
            int k = t - 520;
            if (k % 8 == 0) {
                double sweep = Math.sin(k / 60.0) * 0.9;
                for (int i = 0; i < TOWERS.size(); i++) {
                    BlockPos tw = TOWERS.get(i);
                    double side = (i == 0 || i == 3) ? -1 : 1;
                    Vec3 dir = new Vec3(sweep * 0.9 + side * 0.25, 1.0, (i < 2 ? -0.25 : 0.25)).normalize().scale(1.1);
                    rocket(sl, top(tw), dir, 2, i % 2 == 0 ? 4 : 0, RAINBOW[(k / 8 + i * 2) % RAINBOW.length], SILVER, true, true);
                }
            }
            if (k % 45 == 0) {
                double side = (k / 45) % 2 == 0 ? -1 : 1;
                comet(sl, side * 30, SKY, -5, 1.0);
                mega(sl, side * 30, SKY + 6, -5, 12, (k / 45) % 2 == 0 ? Palette.CYAN : Palette.PURPLE, 260);
            }
            if (k % 90 == 60) crossComets(sl);
            if (k == 360) mega(sl, 0, SKY + 18, 0, 18, Palette.WHITE, 500);
        }

        if (t >= 900 && t < 1320) {
            int k = t - 900;
            switch (k) {
                case 0 -> { shape(sl, heart(), 0, SKY + 8, 1.6, Palette.PINK); boom(sl); }
                case 90 -> { shape(sl, star(), -30, SKY + 6, 1.4, Palette.GOLD); boom(sl); }
                case 150 -> { shape(sl, star(), 30, SKY + 6, 1.4, Palette.CYAN); boom(sl); }
                case 230 -> { shape(sl, smiley(), 0, SKY + 10, 1.5, Palette.GOLD); boom(sl); }
                case 320 -> { planet(sl, 0, SKY + 12); boom(sl); }
                default -> {}
            }
            if (k % 30 == 15) for (int i = 0; i < 3; i++) rocket(sl, top(MORTARS.get(r.nextInt(MORTARS.size()))), Vec3.ZERO, 1, 2, RAINBOW[r.nextInt(8)], GOLD, true, false);
            if (k >= 330 && k % 3 == 0) spiral(sl, k - 330);
        }

        if (t >= 1320 && t < 1560) {
            int k = t - 1320;
            if (k % 2 == 0) for (int i = 0; i < MORTARS.size(); i++) {
                if ((i + k / 20) % 2 != 0) continue;
                BlockPos m = MORTARS.get(i);
                jet(sl, m, (k / 20) % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME, 1.0);
                lamp(sl, m, 3);
            }
            if (k % 4 == 0) fountain(sl, k);
            if (k % 10 == 0) for (int i = 0; i < 4; i++) {
                BlockPos m = MORTARS.get((k / 10 + i * 3) % MORTARS.size());
                Vec3 out = new Vec3(m.getX() - CX, 12, m.getZ() - CZ).normalize().scale(0.9);
                rocket(sl, top(m), out, 1, 4, RAINBOW[r.nextInt(8)], WHITE, true, true);
            }
            if (k % 40 == 20) sound(sl, SoundEvents.FIRECHARGE_USE, 4f, 0.6f);
        }

        if (t >= 1560 && t < 1840) {
            int k = t - 1560;
            if (k < 70 && k % 2 == 0) skyText(sl, "SOLARIS", k / 70.0, Palette.GOLD);
            if (k == 70) { skyText(sl, "SOLARIS", 1, Palette.WHITE); sound(sl, SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR, 10f, 1f); sound(sl, SoundEvents.PLAYER_LEVELUP, 8f, 0.7f); }
            if (k > 70 && k < 200 && k % 20 == 0) skyText(sl, "SOLARIS", 1, k % 40 == 0 ? Palette.GOLD : Palette.WHITE);
            if (k > 100 && k % 12 == 0) rocket(sl, top(MORTARS.get(r.nextInt(MORTARS.size()))), Vec3.ZERO, 3, 1, GOLD, WHITE, true, true);
            if (k == 210) willow(sl, 0, SKY + 30, 40);
        }

        if (t >= 1840 && t < 2240) {
            int k = t - 1840;
            double heat = k / 400.0;
            int every = Math.max(1, 4 - (int) (heat * 4));
            if (k % every == 0) {
                BlockPos m = r.nextBoolean() ? MORTARS.get(r.nextInt(MORTARS.size())) : TOWERS.get(r.nextInt(TOWERS.size()));
                Vec3 dir = new Vec3((r.nextDouble() - 0.5) * 0.8, 1, (r.nextDouble() - 0.5) * 0.5).normalize().scale(1.05);
                rocket(sl, top(m), dir, 1 + r.nextInt(3), r.nextInt(5), RAINBOW[r.nextInt(8)], RAINBOW[r.nextInt(8)], r.nextBoolean(), r.nextBoolean());
            }
            if (k % Math.max(6, 20 - (int) (heat * 14)) == 0) mega(sl, (r.nextDouble() - 0.5) * 70, SKY + r.nextInt(30), (r.nextDouble() - 0.5) * 30, 8 + r.nextInt(8), Palette.values()[r.nextInt(Palette.values().length)], 160);
            if (k % 60 == 0) ring(sl, SKY + 10 + r.nextInt(10), 18 + r.nextInt(10), Palette.values()[r.nextInt(Palette.values().length)], 160);
            if (k % 3 == 0) for (BlockPos lp : LAMPS) setLamp(sl, lp, r.nextBoolean());
            if (k % 20 == 0) sound(sl, SoundEvents.FIREWORK_ROCKET_BLAST_FAR, 10f, 0.5f + (float) heat);
            if (k > 300 && k % 5 == 0) for (int j = 0; j < 3; j++) jet(sl, MORTARS.get(r.nextInt(MORTARS.size())), ParticleTypes.FLAME, 1.4);
        }
        if (t == 2250) {
            mega(sl, 0, SKY + 20, 0, 22, Palette.GOLD, 700);
            mega(sl, -35, SKY + 10, 0, 14, Palette.WHITE, 300);
            mega(sl, 35, SKY + 10, 0, 14, Palette.CYAN, 300);
            sound(sl, SoundEvents.GENERIC_EXPLODE, 10f, 0.4f);
            sound(sl, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR, 12f, 0.5f);
            spark(sl, ParticleTypes.FLASH, CX + 0.5, SKY + 20, CZ + 0.5, 0, 0, 0);
        }
        if (t == 2262) willow(sl, 0, SKY + 30, 70);
        if (t == 2300) {
            title(sl, "§6§l✦ Thank you, Solaris! ✦", "§eThe Firework Machine will return", 10, 70, 30);
            for (BlockPos lp : LAMPS) setLamp(sl, lp, true);
            cheer(sl, true);
        }
        if (t > 2300 && t < SHOW_LENGTH && t % 4 == 0) sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, CX + 0.5, CY + 18, CZ + 0.5, 2, 1, 0.5, 1, 0.02);
        if (t % 40 == 0 && t > 100 && t < 2300) cheer(sl, false);
        if (t % 8 == 0 && t < 2300) sl.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, CX + 0.5, CY + 18, CZ + 0.5, 1, 0.3, 0.3, 0.3, 0.01);
        if (t >= SHOW_LENGTH) {
            for (BlockPos lp : LAMPS) setLamp(sl, lp, false);
            startedAt = -1;
            FireheartCity.LOG.info("Firework Machine show finished");
        }
    }

    static BlockPos top(BlockPos m) {
        return new BlockPos(m.getX(), m.getY() == CY + 1 ? CY + 5 : m.getY() + 1, m.getZ());
    }

    static void powerUp(ServerLevel sl, int t) {
        int lit = t * LAMPS.size() / 90;
        for (int i = 0; i < LAMPS.size(); i++) setLamp(sl, LAMPS.get(i), i < lit);
        if (t % 5 == 0) {
            sl.sendParticles(ParticleTypes.CLOUD, CX + 0.5, CY + 18, CZ + 0.5, 6, 0.6, 0.4, 0.6, 0.05);
            for (BlockPos m : MORTARS) sl.sendParticles(ParticleTypes.SMOKE, m.getX() + 0.5, CY + 5, m.getZ() + 0.5, 2, 0.1, 0.2, 0.1, 0.02);
            sl.playSound(null, CENTRE.above(5), SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 2f, 0.5f + t / 100f);
        }
        if (t % 20 == 10) sound(sl, SoundEvents.NOTE_BLOCK_BELL.value(), 6f, 0.5f + t / 120f);
    }

    static void countdown(ServerLevel sl, String s) {
        title(sl, s, "", 0, 18, 4);
        sound(sl, SoundEvents.NOTE_BLOCK_PLING.value(), 8f, s.contains("✦") ? 2f : 1f);
    }

    static void title(ServerLevel sl, String t, String sub, int in, int stay, int out) {
        for (ServerPlayer p : sl.players()) {
            if (!near(p, 400)) continue;
            p.connection.send(new ClientboundSetTitlesAnimationPacket(in, stay, out));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(sub)));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(t)));
        }
    }

    static void sound(ServerLevel sl, SoundEvent e, float vol, float pitch) {
        sl.playSound(null, CX + 0.5, SKY - 20, CZ + 0.5, e, SoundSource.AMBIENT, vol, pitch);
    }

    static void boom(ServerLevel sl) {
        sound(sl, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR, 12f, 0.8f);
        sound(sl, SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR, 10f, 1f);
    }

    static void lamp(ServerLevel sl, BlockPos m, int ticks) {
        setLamp(sl, new BlockPos(m.getX(), CY, m.getZ()), true);
    }

    static void setLamp(ServerLevel sl, BlockPos p, boolean on) {
        BlockState s = sl.getBlockState(p);
        if (s.is(Blocks.REDSTONE_LAMP) && s.getValue(RedstoneLampBlock.LIT) != on) sl.setBlock(p, s.setValue(RedstoneLampBlock.LIT, on), 2 | 16);
    }

    static void cheer(ServerLevel sl, boolean end) {
        RandomSource r = sl.random;
        for (Resident res : sl.getEntitiesOfClass(Resident.class, new net.minecraft.world.phys.AABB(CX - 160, CY - 20, CZ - 160, CX + 160, CY + 60, CZ + 60))) {
            if (res.isSleeping() || !res.isFree() || r.nextInt(end ? 1 : 4) != 0) continue;
            res.getLookControl().setLookAt(CX, SKY, CZ);
            res.gesture(end ? Resident.G_CLAP : Resident.G_CHEER, 50);
            if (r.nextInt(end ? 2 : 6) == 0) res.say(Lines.pick(r, end ? new String[]{"BRAVO!", "That was INCREDIBLE!", "Best show ever!", "Encore! Encore!"} : new String[]{"Woooow!", "Ooooh!", "Look at THAT one!", "So pretty!", "Did you see that?!", "Aaaah!"}), 50);
        }
    }

    // ---------- real rockets ----------

    static void rocket(ServerLevel sl, BlockPos from, Vec3 dir, int flight, int type, int c1, int c2, boolean trail, boolean flicker) {
        if (!sl.isLoaded(from)) return;
        ItemStack st = new ItemStack(Items.FIREWORK_ROCKET);
        CompoundTag fw = new CompoundTag();
        fw.putByte("Flight", (byte) flight);
        ListTag ex = new ListTag();
        CompoundTag e = new CompoundTag();
        e.putByte("Type", (byte) type);
        e.putIntArray("Colors", new int[]{c1, c2});
        e.putIntArray("FadeColors", new int[]{WHITE});
        e.putBoolean("Trail", trail);
        e.putBoolean("Flicker", flicker);
        ex.add(e);
        fw.put("Explosions", ex);
        st.getOrCreateTag().put("Fireworks", fw);
        boolean angled = dir.lengthSqr() > 0;
        FireworkRocketEntity fr = new FireworkRocketEntity(sl, st, from.getX() + 0.5, from.getY() + 0.2, from.getZ() + 0.5, angled);
        if (angled) fr.setDeltaMovement(dir);
        sl.addFreshEntity(fr);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, from.getX() + 0.5, from.getY(), from.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.03);
        sl.sendParticles(ParticleTypes.FLAME, from.getX() + 0.5, from.getY(), from.getZ() + 0.5, 6, 0.15, 0.05, 0.15, 0.05);
    }

    // ---------- long-range particle effects ----------

    enum Palette {
        GOLD(ParticleTypes.WAX_ON, ParticleTypes.FLAME, ParticleTypes.END_ROD),
        WHITE(ParticleTypes.END_ROD, ParticleTypes.FIREWORK, ParticleTypes.WAX_OFF),
        CYAN(ParticleTypes.GLOW, ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.SCRAPE),
        PURPLE(ParticleTypes.DRAGON_BREATH, ParticleTypes.END_ROD, ParticleTypes.REVERSE_PORTAL),
        GREEN(ParticleTypes.TOTEM_OF_UNDYING, ParticleTypes.GLOW, ParticleTypes.HAPPY_VILLAGER),
        PINK(ParticleTypes.DRAGON_BREATH, ParticleTypes.WAX_ON, ParticleTypes.FIREWORK);

        final ParticleOptions[] p;

        Palette(ParticleOptions... p) {
            this.p = p;
        }
    }

    static void spark(ServerLevel sl, ParticleOptions type, double x, double y, double z, double vx, double vy, double vz) {
        for (ServerPlayer pl : sl.players()) if (near(pl, 480)) sl.sendParticles(pl, type, true, x, y, z, 0, vx, vy, vz, 1);
    }

    static void mega(ServerLevel sl, double ox, double y, double oz, double radius, Palette pal, int n) {
        double x = CX + 0.5 + ox, z = CZ + 0.5 + oz;
        double speed = radius * 0.04;
        double golden = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < n; i++) {
            double yy = 1 - (i / (double) (n - 1)) * 2;
            double rad = Math.sqrt(1 - yy * yy);
            double th = golden * i;
            double vx = Math.cos(th) * rad, vz = Math.sin(th) * rad;
            ParticleOptions p = pal.p[i % pal.p.length];
            double s = speed * (0.85 + (i % 7) * 0.03);
            spark(sl, p, x, y, z, vx * s, yy * s, vz * s);
        }
        spark(sl, ParticleTypes.FLASH, x, y, z, 0, 0, 0);
        sl.playSound(null, x, y - 20, z, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR, SoundSource.AMBIENT, 10f, 0.6f + sl.random.nextFloat() * 0.3f);
        sl.playSound(null, x, y - 20, z, SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR, SoundSource.AMBIENT, 8f, 0.8f + sl.random.nextFloat() * 0.4f);
    }

    static void ring(ServerLevel sl, double y, double radius, Palette pal, int n) {
        double speed = radius * 0.04;
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            spark(sl, pal.p[i % pal.p.length], CX + 0.5, y, CZ + 0.5, Math.cos(a) * speed, 0, Math.sin(a) * speed * 0.35);
        }
        sl.playSound(null, CX, y - 20, CZ, SoundEvents.FIREWORK_ROCKET_BLAST_FAR, SoundSource.AMBIENT, 10f, 0.7f);
    }

    static void comet(ServerLevel sl, double ox, double topY, double oz, double speed) {
        double x = CX + 0.5 + ox, z = CZ + 0.5 + oz;
        for (double y = CY + 18; y < topY; y += 1.5) spark(sl, ParticleTypes.END_ROD, x + Math.sin(y) * 0.2, y, z, 0, -0.02, 0);
        sl.playSound(null, x, CY + 20, z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.AMBIENT, 8f, 0.6f);
    }

    static void crossComets(ServerLevel sl) {
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 40; i++) {
                double f = i / 40.0;
                spark(sl, side < 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, CX + side * (40 - f * 80), SKY - 20 + f * 30, CZ, side * -0.3, 0.1, 0);
            }
        }
        sound(sl, SoundEvents.FIREWORK_ROCKET_LAUNCH, 10f, 0.8f);
    }

    static void jet(ServerLevel sl, BlockPos m, ParticleOptions type, double power) {
        for (int i = 0; i < 4; i++) spark(sl, type, m.getX() + 0.5, CY + 5, m.getZ() + 0.5, (sl.random.nextDouble() - 0.5) * 0.08, power * (0.8 + sl.random.nextDouble() * 0.4), (sl.random.nextDouble() - 0.5) * 0.08);
    }

    static void fountain(ServerLevel sl, int k) {
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + k * 0.05;
            double x = CX + 0.5 + Math.cos(a) * (R + 2), z = CZ + 0.5 + Math.sin(a) * (R + 2);
            for (int j = 0; j < 6; j++) spark(sl, ParticleTypes.SPLASH, x, CY - 7, z, Math.cos(a) * 0.1, 1.2 + j * 0.1, Math.sin(a) * 0.1);
            spark(sl, ParticleTypes.BUBBLE_COLUMN_UP, x, CY - 8, z, 0, 0.5, 0);
        }
        if (k % 20 == 0) sl.playSound(null, CX, CY, CZ, SoundEvents.GENERIC_SPLASH, SoundSource.AMBIENT, 4f, 0.8f);
    }

    static void willow(ServerLevel sl, double ox, double y, int strands) {
        for (int i = 0; i < strands; i++) {
            double a = i * Math.PI * 2 / strands;
            double rr = 8 + sl.random.nextDouble() * 18;
            double x = CX + 0.5 + ox + Math.cos(a) * rr, z = CZ + 0.5 + Math.sin(a) * rr * 0.5;
            for (int j = 0; j < 10; j++) spark(sl, j % 3 == 0 ? ParticleTypes.FLAME : ParticleTypes.WAX_ON, x, y - j * 2.5, z, Math.cos(a) * 0.05, -0.25, Math.sin(a) * 0.02);
        }
        sl.playSound(null, CX, y - 20, CZ, SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR, SoundSource.AMBIENT, 12f, 0.6f);
    }

    static void spiral(ServerLevel sl, int k) {
        for (int arm = 0; arm < 3; arm++) {
            double a = k * 0.12 + arm * Math.PI * 2 / 3;
            double rr = 4 + (k % 90) * 0.3;
            spark(sl, arm == 0 ? ParticleTypes.GLOW : arm == 1 ? ParticleTypes.WAX_ON : ParticleTypes.DRAGON_BREATH, CX + 0.5 + Math.cos(a) * rr, SKY + 10 + Math.sin(a) * rr * 0.6, CZ + 0.5, 0, 0, 0);
        }
    }

    static void planet(ServerLevel sl, double oy, double y) {
        mega(sl, 0, y, 0, 7, Palette.PURPLE, 260);
        for (int i = 0; i < 160; i++) {
            double a = i * Math.PI * 2 / 160;
            double sp = 0.8;
            spark(sl, i % 2 == 0 ? ParticleTypes.GLOW : ParticleTypes.END_ROD, CX + 0.5, y, CZ + 0.5, Math.cos(a) * sp, Math.sin(a) * sp * 0.25, Math.sin(a) * sp * 0.15);
        }
    }

    static List<double[]> heart() {
        List<double[]> out = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            double t = i * Math.PI * 2 / 120;
            out.add(new double[]{16 * Math.pow(Math.sin(t), 3) / 16.0, (13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t)) / 16.0});
        }
        return out;
    }

    static List<double[]> star() {
        List<double[]> out = new ArrayList<>();
        double[][] pts = new double[10][];
        for (int i = 0; i < 10; i++) {
            double a = Math.PI / 2 + i * Math.PI / 5;
            double rr = i % 2 == 0 ? 1 : 0.42;
            pts[i] = new double[]{Math.cos(a) * rr, Math.sin(a) * rr};
        }
        for (int i = 0; i < 10; i++) {
            double[] a = pts[i], b = pts[(i + 1) % 10];
            for (int j = 0; j < 10; j++) out.add(new double[]{Mth.lerp(j / 10.0, a[0], b[0]), Mth.lerp(j / 10.0, a[1], b[1])});
        }
        return out;
    }

    static List<double[]> smiley() {
        List<double[]> out = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            double a = i * Math.PI * 2 / 80;
            out.add(new double[]{Math.cos(a), Math.sin(a)});
        }
        for (int i = 0; i <= 20; i++) {
            double a = Math.PI + 0.5 + i * (Math.PI - 1) / 20;
            out.add(new double[]{Math.cos(a) * 0.6, Math.sin(a) * 0.6 - 0.05});
        }
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI * 2 / 12;
            out.add(new double[]{-0.35 + Math.cos(a) * 0.1, 0.3 + Math.sin(a) * 0.12});
            out.add(new double[]{0.35 + Math.cos(a) * 0.1, 0.3 + Math.sin(a) * 0.12});
        }
        return out;
    }

    static void shape(ServerLevel sl, List<double[]> pts, double ox, double y, double scale, Palette pal) {
        double size = 14 * scale;
        for (int i = 0; i < pts.size(); i++) {
            double[] p = pts.get(i);
            double x = CX + 0.5 + ox - p[0] * size, yy = y + p[1] * size;
            ParticleOptions type = pal.p[i % 2];
            spark(sl, type, x, yy, CZ + 0.5, 0, -0.01, 0);
            spark(sl, ParticleTypes.END_ROD, x, yy, CZ + 0.5, (sl.random.nextDouble() - 0.5) * 0.02, -0.02, 0);
        }
        spark(sl, ParticleTypes.FLASH, CX + 0.5 + ox, y, CZ + 0.5, 0, 0, 0);
    }

    static final String[] FONT_KEYS = {"S", "O", "L", "A", "R", "I"};
    static final String[][] FONT = {
            {"01111", "10000", "10000", "01110", "00001", "00001", "11110"},
            {"01110", "10001", "10001", "10001", "10001", "10001", "01110"},
            {"10000", "10000", "10000", "10000", "10000", "10000", "11111"},
            {"01110", "10001", "10001", "11111", "10001", "10001", "10001"},
            {"11110", "10001", "10001", "11110", "10100", "10010", "10001"},
            {"11111", "00100", "00100", "00100", "00100", "00100", "11111"}};

    static void skyText(ServerLevel sl, String text, double reveal, Palette pal) {
        double px = 2.2;
        int cols = text.length() * 6 - 1;
        double width = cols * px;
        int shown = (int) Math.ceil(cols * reveal);
        for (int c = 0; c < text.length(); c++) {
            int fi = java.util.Arrays.asList(FONT_KEYS).indexOf(String.valueOf(text.charAt(c)));
            if (fi < 0) continue;
            String[] g = FONT[fi];
            for (int row = 0; row < 7; row++) {
                for (int col = 0; col < 5; col++) {
                    if (g[row].charAt(col) != '1') continue;
                    int gc = c * 6 + col;
                    if (gc > shown) continue;
                    if (reveal < 1 && gc < shown - 3) continue;
                    double x = CX + 0.5 + width / 2 - gc * px;
                    double y = SKY + 22 - row * px;
                    spark(sl, pal.p[(row + col) % 2], x, y, CZ + 0.5, 0, 0, 0);
                    if (reveal >= 1) spark(sl, ParticleTypes.END_ROD, x, y, CZ + 0.5, 0, -0.005, 0);
                }
            }
        }
    }
}
