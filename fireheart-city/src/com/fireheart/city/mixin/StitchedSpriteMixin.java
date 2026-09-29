package com.fireheart.city.mixin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.createmod.catnip.render.StitchedSprite", remap = false)
public abstract class StitchedSpriteMixin {
    @Shadow(remap = false) @Final @Mutable
    private static Map ALL;

    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false)
    private static void fireheart$safeMap(CallbackInfo ci) {
        ALL = new ConcurrentHashMap(ALL);
    }

    @Redirect(method = "<init>(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;"),
            remap = false)
    private Object fireheart$safeList(Map map, Object key, Function fn) {
        return map.computeIfAbsent(key, k -> new CopyOnWriteArrayList());
    }
}
