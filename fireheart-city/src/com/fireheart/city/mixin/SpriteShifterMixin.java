package com.fireheart.city.mixin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.createmod.catnip.render.SpriteShifter", remap = false)
public abstract class SpriteShifterMixin {
    @Shadow(remap = false) @Final @Mutable
    private static Map ENTRY_CACHE;

    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false)
    private static void fireheart$safeCache(CallbackInfo ci) {
        ENTRY_CACHE = new ConcurrentHashMap(ENTRY_CACHE);
    }
}
