package com.fireheart.city;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Hall of Lights on the bridge to the Sky Organ: walls and ceiling of redstone lamps. Lamps glow around whoever
 * walks through, light waves ripple outward with rising chimes as you go, and reaching the organ sets off a finale.
 */
public final class MusicHall {
    private MusicHall() {}

    static final int X1 = 21, X2 = 38, Z1 = 282, Z2 = 288, Y0 = 180;
    static final List<BlockPos> LAMPS = new ArrayList<>();
    static boolean lit;
    static long finaleUntil = -1, lastNote;
    static final int[] SCALE = {0, 2, 4, 7, 9, 12, 14, 16, 19, 21, 24};

    public static boolean ready(ServerLevel sl) {
        for (int cx = X1 >> 4; cx <= X2 >> 4; cx++) for (int cz = Z1 >> 4; cz <= Z2 >> 4; cz++) if (sl.getChunkSource().getChunkNow(cx, cz) == null) return false;
        return true;
    }

    static void lamps() {
        if (!LAMPS.isEmpty()) return;
        for (int x = X1; x <= X2; x++) {
            for (int y = Y0 + 1; y <= Y0 + 5; y++) if ((x + y) % 2 == 0) { LAMPS.add(new BlockPos(x, y, Z1)); LAMPS.add(new BlockPos(x, y, Z2)); }
            if (x % 2 == 0) for (int z = Z1 + 1; z <= Z2 - 1; z++) LAMPS.add(new BlockPos(x, Y0 + 6, z));
        }
    }

    static void build(ServerLevel sl) {
        Builder b = new Builder(sl);
        for (int x = X1; x <= X2; x++) {
            for (int y = Y0 + 1; y <= Y0 + 5; y++) {
                String s = (x + y) % 2 == 0 ? "minecraft:redstone_lamp" : "minecraft:black_concrete";
                b.set(x, y, Z1, s);
                b.set(x, y, Z2, s);
            }
            for (int z = Z1; z <= Z2; z++) b.set(x, Y0 + 6, z, x % 2 == 0 && z > Z1 && z < Z2 ? "minecraft:redstone_lamp" : "minecraft:black_concrete");
            b.set(x, Y0 + 1, Z1 + 1, "minecraft:air");
            b.set(x, Y0 + 1, Z2 - 1, "minecraft:air");
            b.set(x, Y0, Z1, "minecraft:black_concrete");
            b.set(x, Y0, Z2, "minecraft:black_concrete");
            b.set(x, Y0, Z1 + 1, "betterblockz:zeon_black_blockz_9[lit=true]|minecraft:sea_lantern");
            b.set(x, Y0, Z2 - 1, "betterblockz:zeon_black_blockz_9[lit=true]|minecraft:sea_lantern");
        }
        b.sign(X1 - 1, Y0 + 4, Z1 + 1, "minecraft:dark_oak_wall_sign[facing=west]", "§d§lHALL OF", "§d§lLIGHTS", "walk slowly...", "");
    }

    static void set(ServerLevel sl, BlockPos p, boolean on) {
        BlockState s = sl.getBlockState(p);
        if (s.is(Blocks.REDSTONE_LAMP) && s.getValue(RedstoneLampBlock.LIT) != on) sl.setBlock(p, s.setValue(RedstoneLampBlock.LIT, on), 2 | 16);
    }

    public static void tick(ServerLevel sl, CityData d) {
        if (!d.hallBuilt) {
            if (!ready(sl)) return;
            d.hallBuilt = true;
            d.setDirty();
            build(sl);
        }
        lamps();
        if (!sl.isPositionEntityTicking(LAMPS.get(0)) || OrganConsole.hallPlaying()) return;
        long now = sl.getGameTime();
        List<ServerPlayer> in = new ArrayList<>();
        for (ServerPlayer p : sl.players()) if (p.getX() >= X1 - 1 && p.getX() <= X2 + 2 && p.getZ() >= Z1 && p.getZ() <= Z2 + 1 && p.getY() > Y0 && p.getY() < Y0 + 7) in.add(p);
        boolean finale = now < finaleUntil;
        if (in.isEmpty() && !finale) {
            if (lit) { for (BlockPos p : LAMPS) set(sl, p, false); lit = false; }
            return;
        }
        lit = true;
        if (finale) {
            boolean on = ((finaleUntil - now) / 5) % 2 == 0;
            for (BlockPos p : LAMPS) set(sl, p, on);
            return;
        }
        int t = (int) (now % 24);
        for (BlockPos lp : LAMPS) {
            boolean on = false;
            for (ServerPlayer p : in) {
                double dx = Math.abs(lp.getX() + 0.5 - p.getX());
                double dy = Math.abs(lp.getY() + 0.5 - p.getEyeY());
                if (dx < 2.2 && dy < 3.5) on = true;
                if (Math.abs(dx - t) < 0.9) on = true;
                if (lp.getY() == Y0 + 6 && Math.abs(((now / 2) % 18) - (lp.getX() - X1)) < 1) on = true;
            }
            set(sl, lp, on);
        }
        for (ServerPlayer p : in) {
            if (t == 0 && now - lastNote >= 6) {
                lastNote = now;
                int step = (int) Math.max(0, Math.min(SCALE.length - 1, (p.getX() - X1) / (X2 - X1 + 1) * SCALE.length));
                float pitch = (float) Math.pow(2, (SCALE[step] - 12) / 12.0);
                sl.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.RECORDS, 0.9f, pitch);
                sl.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.RECORDS, 0.5f, pitch * 0.5f);
                sl.sendParticles(ParticleTypes.NOTE, p.getX(), p.getY() + 2.3, p.getZ(), 3, 0.6, 0.2, 0.6, step / 10.0);
            }
            if (p.getX() > X2 + 0.5 && finaleUntil < now - 200) {
                finaleUntil = now + 60;
                for (int i = 0; i < 4; i++) sl.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.RECORDS, 1f, (float) Math.pow(2, (new int[]{0, 4, 7, 12}[i] - 6) / 12.0));
                sl.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 40, 1.5, 1.5, 1.5, 0.05);
            }
        }
    }
}
