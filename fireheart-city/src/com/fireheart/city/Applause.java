package com.fireheart.city;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.joml.Vector3f;

/** Residents celebrating with players: applause for achievements and confetti bursts at celebrations. */
public final class Applause {
    private Applause() {}

    static final DustParticleOptions[] CONFETTI = {
            new DustParticleOptions(new Vector3f(1f, 0.25f, 0.3f), 1.1f), new DustParticleOptions(new Vector3f(1f, 0.85f, 0.2f), 1.1f),
            new DustParticleOptions(new Vector3f(0.3f, 0.6f, 1f), 1.1f), new DustParticleOptions(new Vector3f(0.4f, 1f, 0.45f), 1.1f),
            new DustParticleOptions(new Vector3f(0.9f, 0.4f, 1f), 1.1f)
    };

    public static void confetti(ServerLevel sl, BlockPos at, int n) {
        for (int i = 0; i < n; i++) {
            DustParticleOptions c = CONFETTI[sl.getRandom().nextInt(CONFETTI.length)];
            sl.sendParticles(c, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 3, 2.5, 1.5, 2.5, 0);
        }
        sl.playSound(null, at, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.NEUTRAL, 0.8f, 1.2f);
    }

    /** Nearby residents clap and congratulate a player who just unlocked an achievement. */
    public static void forAchievement(ServerPlayer pl, String name) {
        ServerLevel sl = pl.serverLevel();
        int spoke = 0;
        for (Resident r : sl.getEntitiesOfClass(Resident.class, pl.getBoundingBox().inflate(12, 4, 12), x -> x.profile() != null && x.isFree() && !x.isSleeping())) {
            if (!r.hasLineOfSight(pl)) continue;
            r.getLookControl().setLookAt(pl, 30, 30);
            r.gesture(sl.getRandom().nextBoolean() ? Resident.G_CLAP : Resident.G_CHEER, 50);
            if (spoke++ < 2) r.say(r.pick("Congrats, " + pl.getName().getString() + "!", "Whoa, nice one!", "\"" + name + "\"? Impressive!", "Go " + pl.getName().getString() + "!"), 50);
        }
        if (spoke > 0) sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pl.getX(), pl.getY() + 2, pl.getZ(), 10, 0.5, 0.3, 0.5, 0);
    }
}
