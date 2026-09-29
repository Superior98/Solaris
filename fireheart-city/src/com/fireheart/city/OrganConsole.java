package com.fireheart.city;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.joml.Vector3f;

/** Keeps the Sky Organ console working and lets its songs play either on the organ or in the Hall of Lights. */
public final class OrganConsole {
    private OrganConsole() {}

    static final BlockPos PLAY = new BlockPos(53, 181, 283);
    static final BlockPos PLAY_BACK = new BlockPos(54, 181, 283);
    static final BlockPos NEXT = new BlockPos(53, 181, 285);
    static final BlockPos STOP = new BlockPos(53, 181, 287);
    static final BlockPos VENUE = new BlockPos(53, 181, 281);
    static final BlockPos VENUE_BACK = new BlockPos(54, 181, 281);
    static final BlockPos VENUE_SIGN = new BlockPos(53, 182, 281);
    static final BlockPos VENUE_SIGN_BACK = new BlockPos(54, 182, 281);
    static final String[] NAMES = {"An Ending", "Fallen Down", "Finale", "Frozen Time"};
    static final BlockPos HALL_CENTRE = new BlockPos(30, 182, 285);

    record Note(int t, char inst, int n) {}

    private static final Map<Integer, List<Note>> SONGS = new HashMap<>();
    private static boolean venuePrev, playPrev, stopPrev, nextPrev;
    private static int hallSong = 0;
    private static int hallT;
    private static int hallIdx;
    private static final Map<Integer, Integer> COLUMN_GLOW = new HashMap<>();
    private static int ceilingGlow;

    public static void reset() {
        hallSong = 0;
        COLUMN_GLOW.clear();
        venuePrev = playPrev = stopPrev = nextPrev = false;
    }

    public static boolean hallPlaying() {
        return hallSong > 0;
    }

    static List<Note> song(int n) {
        return SONGS.computeIfAbsent(n, k -> {
            List<Note> out = new ArrayList<>();
            try (InputStream in = OrganConsole.class.getResourceAsStream("/data/fireheartcity/organ/song" + k + ".txt")) {
                if (in == null) return out;
                BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String line;
                while ((line = r.readLine()) != null) {
                    String[] p = line.trim().split(" ");
                    if (p.length == 3) out.add(new Note(Integer.parseInt(p[0]), p[1].charAt(0), Integer.parseInt(p[2])));
                }
            } catch (Exception e) {
                FireheartCity.LOG.error("Could not read organ song " + k, e);
            }
            return out;
        });
    }

    private static boolean loaded(ServerLevel sl, BlockPos p) {
        return sl.getChunkSource().getChunkNow(p.getX() >> 4, p.getZ() >> 4) != null;
    }

    private static void repair(ServerLevel sl, CityData d) {
        Builder b = new Builder(sl);
        if (!sl.getBlockState(PLAY).is(Blocks.WARPED_BUTTON)) {
            if (sl.getBlockState(PLAY_BACK).isAir()) b.set(PLAY_BACK.getX(), PLAY_BACK.getY(), PLAY_BACK.getZ(), "betterblockz:zeon_black_blockz_9|minecraft:polished_blackstone");
            b.set(PLAY.getX(), PLAY.getY(), PLAY.getZ(), "minecraft:warped_button[face=wall,facing=west,powered=false]");
            FireheartCity.LOG.info("Sky Organ: restored the PLAY button");
        }
        if (!sl.getBlockState(NEXT).is(Blocks.POLISHED_BLACKSTONE_BUTTON)) b.set(NEXT.getX(), NEXT.getY(), NEXT.getZ(), "minecraft:polished_blackstone_button[face=wall,facing=west,powered=false]");
        if (!sl.getBlockState(STOP).is(Blocks.CRIMSON_BUTTON)) b.set(STOP.getX(), STOP.getY(), STOP.getZ(), "minecraft:crimson_button[face=wall,facing=west,powered=false]");
        if (!sl.getBlockState(VENUE).is(Blocks.MANGROVE_BUTTON)) {
            if (sl.getBlockState(VENUE_BACK).isAir()) b.set(VENUE_BACK.getX(), VENUE_BACK.getY(), VENUE_BACK.getZ(), "betterblockz:zeon_black_blockz_9|minecraft:polished_blackstone");
            if (sl.getBlockState(VENUE_SIGN_BACK).isAir()) b.set(VENUE_SIGN_BACK.getX(), VENUE_SIGN_BACK.getY(), VENUE_SIGN_BACK.getZ(), "betterblockz:cyberlight_alt_blockz_8|minecraft:sea_lantern");
            b.set(VENUE.getX(), VENUE.getY(), VENUE.getZ(), "minecraft:mangrove_button[face=wall,facing=west,powered=false]");
            venueSign(sl, d);
        }
        if (!(sl.getBlockEntity(VENUE_SIGN) instanceof SignBlockEntity)) venueSign(sl, d);
    }

    private static void venueSign(ServerLevel sl, CityData d) {
        Builder b = new Builder(sl);
        b.sign(VENUE_SIGN.getX(), VENUE_SIGN.getY(), VENUE_SIGN.getZ(), "minecraft:crimson_wall_sign[facing=west]", "§b§lPLAY SONGS AT", d.organInHall ? "§d§l✦ HALL OF LIGHTS ✦" : "§e§l♪ SKY ORGAN ♪", "§7press to switch", "");
    }

    static int selected(ServerLevel sl) {
        Scoreboard sb = sl.getScoreboard();
        Objective o = sb.getObjective("organ");
        if (o == null || !sb.hasPlayerScore("#song", o)) return 1;
        int s = sb.getOrCreatePlayerScore("#song", o).getScore();
        return s < 1 || s > 4 ? 1 : s;
    }

    private static void setScore(ServerLevel sl, String who, int v) {
        Scoreboard sb = sl.getScoreboard();
        Objective o = sb.getObjective("organ");
        if (o != null) sb.getOrCreatePlayerScore(who, o).setScore(v);
    }

    private static boolean pressed(ServerLevel sl, BlockPos p) {
        BlockState s = sl.getBlockState(p);
        return s.getBlock() instanceof ButtonBlock && s.getValue(ButtonBlock.POWERED);
    }

    private static void bar(ServerLevel sl, String text) {
        for (ServerPlayer p : sl.players()) if (p.getY() > 160 && p.distanceToSqr(40, 181, 285) < 60 * 60) p.displayClientMessage(Component.literal(text), true);
    }

    public static void tick(ServerLevel sl, CityData d) {
        if (!loaded(sl, PLAY)) return;
        long now = sl.getGameTime();
        if (now % 100 == 23) repair(sl, d);
        boolean v = pressed(sl, VENUE), pl = pressed(sl, PLAY), st = pressed(sl, STOP), nx = pressed(sl, NEXT);
        if (v && !venuePrev) {
            d.organInHall = !d.organInHall;
            d.setDirty();
            venueSign(sl, d);
            sl.playSound(null, VENUE, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1f, d.organInHall ? 1.5f : 0.9f);
            bar(sl, d.organInHall ? "§d✦ Songs will play in the Hall of Lights ✦" : "§e♪ Songs will play on the Sky Organ ♪");
        }
        if (pl && !playPrev && d.organInHall) startHall(sl, selected(sl));
        if ((st && !stopPrev) || (nx && !nextPrev)) stopHall(sl);
        venuePrev = v;
        playPrev = pl;
        stopPrev = st;
        nextPrev = nx;
        if (hallSong > 0) hallTick(sl);
    }

    public static void startHall(ServerLevel sl, int n) {
        setScore(sl, "#on", 0);
        for (int y : new int[]{182, 185, 188}) for (int z = 273; z <= 297; z++) {
            BlockPos p = new BlockPos(60, y, z);
            if (sl.getBlockState(p).is(Blocks.REDSTONE_BLOCK)) sl.setBlock(p, Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 3);
        }
        hallSong = n;
        hallT = -40;
        hallIdx = 0;
        COLUMN_GLOW.clear();
        bar(sl, "§d✦ Now playing in the Hall of Lights: " + NAMES[n - 1] + " ✦");
        sl.playSound(null, HALL_CENTRE, SoundEvents.BEACON_ACTIVATE, SoundSource.RECORDS, 1.5f, 1.2f);
    }

    public static void stopHall(ServerLevel sl) {
        if (hallSong == 0) return;
        hallSong = 0;
        COLUMN_GLOW.clear();
        MusicHall.lamps();
        for (BlockPos p : MusicHall.LAMPS) MusicHall.set(sl, p, false);
    }

    private static SoundEvent sound(char inst) {
        return switch (inst) {
            case 'b' -> SoundEvents.NOTE_BLOCK_BASS.value();
            case 'e' -> SoundEvents.NOTE_BLOCK_BELL.value();
            default -> SoundEvents.NOTE_BLOCK_HARP.value();
        };
    }

    private static void hallTick(ServerLevel sl) {
        List<Note> notes = song(hallSong);
        hallT++;
        if (hallT < 0) {
            if (hallT % 10 == 0) for (int x = MusicHall.X1; x <= MusicHall.X2; x += 3) sl.sendParticles(ParticleTypes.END_ROD, x + 0.5, 183, 285.5, 3, 0.4, 1, 1.5, 0.01);
            return;
        }
        while (hallIdx < notes.size() && notes.get(hallIdx).t() <= hallT) {
            Note nt = notes.get(hallIdx++);
            float pitch = (float) Math.pow(2, (nt.n() - 12) / 12.0);
            int col = MusicHall.X1 + Math.round(nt.n() / 24f * (MusicHall.X2 - MusicHall.X1));
            BlockPos at = new BlockPos(col, 182, 285);
            sl.playSound(null, at, sound(nt.inst()), SoundSource.RECORDS, nt.inst() == 'b' ? 2.2f : 1.8f, pitch);
            if (nt.inst() == 'h') sl.playSound(null, at, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.RECORDS, 0.35f, pitch);
            COLUMN_GLOW.merge(col, 6, Math::max);
            if (nt.inst() == 'b') ceilingGlow = 6;
            float hue = nt.n() / 24f;
            int rgb = java.awt.Color.HSBtoRGB(hue * 0.8f, 0.9f, 1f);
            DustParticleOptions dust = new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), 1.6f);
            sl.sendParticles(dust, col + 0.5, 181.5 + (nt.inst() == 'b' ? 0 : nt.inst() == 'h' ? 1.5 : 3), 285.5, 6, 0.3, 0.4, 1.4, 0);
            sl.sendParticles(ParticleTypes.NOTE, col + 0.5, 184, 285.5, 1, 0.3, 0.2, 1.2, hue);
        }
        MusicHall.lamps();
        for (BlockPos lp : MusicHall.LAMPS) {
            boolean on;
            if (lp.getY() == MusicHall.Y0 + 6) on = ceilingGlow > 0 && (lp.getX() + hallT / 2) % 4 < 2 || COLUMN_GLOW.getOrDefault(lp.getX(), 0) > 3;
            else {
                int g = Math.max(COLUMN_GLOW.getOrDefault(lp.getX(), 0), Math.max(COLUMN_GLOW.getOrDefault(lp.getX() - 1, 0), COLUMN_GLOW.getOrDefault(lp.getX() + 1, 0)) - 2);
                on = g > 0 && lp.getY() - MusicHall.Y0 <= 1 + g;
            }
            MusicHall.set(sl, lp, on);
        }
        COLUMN_GLOW.replaceAll((k, val) -> val - 1);
        COLUMN_GLOW.values().removeIf(val -> val <= 0);
        if (ceilingGlow > 0) ceilingGlow--;
        int end = notes.isEmpty() ? 0 : notes.get(notes.size() - 1).t() + 40;
        if (hallT > end) {
            for (BlockPos lp : MusicHall.LAMPS) MusicHall.set(sl, lp, true);
            sl.sendParticles(ParticleTypes.END_ROD, 30, 183, 285.5, 80, 8, 1.5, 2, 0.05);
            sl.playSound(null, HALL_CENTRE, SoundEvents.PLAYER_LEVELUP, SoundSource.RECORDS, 1.2f, 1f);
            bar(sl, "§d♪ Thanks for listening ♪");
            hallSong = 0;
            MusicHall.finaleUntil = sl.getGameTime() + 60;
        }
    }
}
