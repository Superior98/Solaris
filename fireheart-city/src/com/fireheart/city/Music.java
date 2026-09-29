package com.fireheart.city;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Busking tunes for the street musician (public-domain melodies), played on real note-block sounds. */
public final class Music {
    private Music() {}

    public static final int TPH = 4;

    public record Song(String name, int[] notes, int[] bass) {
        public int halfBeats() {
            int n = 0;
            for (int i = 1; i < notes.length; i += 2) n += notes[i];
            return n;
        }

        public int ticks() {
            return halfBeats() * TPH;
        }
    }

    public static final Song ODE = new Song("Ode to Joy", new int[]{
            17, 2, 17, 2, 18, 2, 20, 2, 20, 2, 18, 2, 17, 2, 15, 2, 13, 2, 13, 2, 15, 2, 17, 2, 17, 3, 15, 1, 15, 4,
            17, 2, 17, 2, 18, 2, 20, 2, 20, 2, 18, 2, 17, 2, 15, 2, 13, 2, 13, 2, 15, 2, 17, 2, 15, 3, 13, 1, 13, 4},
            new int[]{13, 8, 13, 8, 13, 8, 8, 13});
    public static final Song TWINKLE = new Song("Twinkle, Twinkle, Little Star", new int[]{
            6, 2, 6, 2, 13, 2, 13, 2, 15, 2, 15, 2, 13, 4, 11, 2, 11, 2, 10, 2, 10, 2, 8, 2, 8, 2, 6, 4,
            13, 2, 13, 2, 11, 2, 11, 2, 10, 2, 10, 2, 8, 4, 13, 2, 13, 2, 11, 2, 11, 2, 10, 2, 10, 2, 8, 4,
            6, 2, 6, 2, 13, 2, 13, 2, 15, 2, 15, 2, 13, 4, 11, 2, 11, 2, 10, 2, 10, 2, 8, 2, 8, 2, 6, 4},
            new int[]{6, 11, 6, 13, 6, 13, 6, 13, 6, 11, 6, 6});
    public static final Song JACQUES = new Song("Frere Jacques", new int[]{
            6, 2, 8, 2, 10, 2, 6, 2, 6, 2, 8, 2, 10, 2, 6, 2, 10, 2, 11, 2, 13, 4, 10, 2, 11, 2, 13, 4,
            13, 1, 15, 1, 13, 1, 11, 1, 10, 2, 6, 2, 13, 1, 15, 1, 13, 1, 11, 1, 10, 2, 6, 2, 6, 2, 1, 2, 6, 4, 6, 2, 1, 2, 6, 4},
            new int[]{6, 6, 6, 6, 6, 6, 6, 6});
    public static final Song SAINTS = new Song("When the Saints Go Marching In", new int[]{
            6, 2, 10, 2, 11, 2, 13, 8, 6, 2, 10, 2, 11, 2, 13, 8, 6, 2, 10, 2, 11, 2, 13, 4, 10, 4, 6, 4, 10, 4, 8, 8,
            10, 2, 10, 2, 8, 2, 6, 6, 6, 2, 10, 4, 13, 4, 13, 2, 11, 6, 10, 2, 11, 2, 13, 4, 10, 4, 6, 4, 8, 4, 6, 8},
            new int[]{6, 6, 6, 6, 6, 13, 6, 6, 11, 6, 13, 6, 6});
    public static final Song BIRTHDAY = new Song("Happy Birthday", new int[]{
            6, 1, 6, 1, 8, 2, 6, 2, 11, 2, 10, 4, 6, 1, 6, 1, 8, 2, 6, 2, 13, 2, 11, 4,
            6, 1, 6, 1, 18, 2, 15, 2, 11, 2, 10, 2, 8, 4, 16, 1, 16, 1, 15, 2, 11, 2, 13, 2, 11, 4},
            new int[]{11, 6, 6, 11, 11, 16, 6, 11});

    public static final Song[] SONGS = {ODE, TWINKLE, JACQUES, SAINTS};

    public static float pitch(int n) {
        return (float) Math.pow(2.0, (n - 12) / 12.0);
    }

    public static void play(Resident r, ServerLevel l, Song s, int elapsed) {
        if (elapsed < 0 || elapsed % TPH != 0) return;
        int hb = elapsed / TPH;
        int at = 0;
        for (int i = 0; i + 1 < s.notes.length; i += 2) {
            if (at == hb) {
                l.playSound(null, r.getX(), r.getY() + 1, r.getZ(), SoundEvents.NOTE_BLOCK_GUITAR.value(), SoundSource.RECORDS, 1.6f, pitch(s.notes[i]));
                l.sendParticles(ParticleTypes.NOTE, r.getX(), r.getY() + 2.2, r.getZ(), 1, 0.3, 0.1, 0.3, s.notes[i] / 24.0);
                r.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                break;
            }
            at += s.notes[i + 1];
            if (at > hb) break;
        }
        if (hb % 8 == 0) {
            int bar = hb / 8;
            if (bar < s.bass.length) l.playSound(null, r.getX(), r.getY() + 1, r.getZ(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.RECORDS, 1.2f, pitch(s.bass[bar]));
        }
        if (hb % 4 == 2) l.playSound(null, r.getX(), r.getY() + 1, r.getZ(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.RECORDS, 0.4f, 1.0f);
    }
}
