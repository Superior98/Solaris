package com.fireheart.city.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Skips key handling on the one frame where the world is loaded but the player isn't (crashed with Essential's world switching). */
@Mixin(value = Minecraft.class, remap = false)
public abstract class MinecraftKeybindMixin {
    @Inject(method = "m_91279_()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void fireheart$noPlayer(CallbackInfo ci) {
        if (((Minecraft) (Object) this).player == null) ci.cancel();
    }
}
