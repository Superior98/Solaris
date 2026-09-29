package com.fireheart.city.mixin;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerModel.class, remap = false)
public abstract class PlayerModelMixin {
    @Inject(method = "m_6973_(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"), remap = false)
    private void fireheart$deviceAnim(LivingEntity e, float limbSwing, float limbAmount, float age, float headYaw, float headPitch, CallbackInfo ci) {
        try {
            com.fireheart.city.client.DeviceAnim.apply((PlayerModel<?>) (Object) this, e, age);
        } catch (Throwable ignored) {
        }
    }
}
