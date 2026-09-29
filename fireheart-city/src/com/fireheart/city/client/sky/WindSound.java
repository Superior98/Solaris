package com.fireheart.city.client.sky;

import com.fireheart.city.Skydive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Rushing wind that follows the diver and gets louder the faster they fall. */
class WindSound extends AbstractTickableSoundInstance {
    WindSound() {
        super(SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.1f;
        this.relative = true;
        this.attenuation = Attenuation.NONE;
    }

    @Override
    public void tick() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || SkyClient.phase == Skydive.NONE || SkyClient.phase == Skydive.LANDED) {
            this.volume *= 0.8f;
            if (this.volume < 0.02f) stop();
            return;
        }
        double speed = p.getDeltaMovement().length();
        float target = (float) Math.min(1.0, speed * 0.55);
        if (SkyClient.phase == Skydive.CHUTE) target = 0.12f;
        this.volume += (target - this.volume) * 0.15f;
        this.pitch = 0.7f + (float) Math.min(0.6, speed * 0.25);
    }
}
