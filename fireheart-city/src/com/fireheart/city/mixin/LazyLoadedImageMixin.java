package com.fireheart.city.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fixes a race in vanilla atlas stitching: armor-trim palette permutations share one lazily loaded base image, and one
 * worker thread could close it while another was still reading it ("Image is not allocated" crash at startup). The
 * shared image is now kept until the reload is garbage collected - a few kilobytes per resource reload.
 */
@Mixin(targets = "net.minecraft.client.renderer.texture.atlas.sources.LazyLoadedImage", remap = false)
public abstract class LazyLoadedImageMixin {
    @Inject(method = "m_266458_()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void fireheart$keepImage(CallbackInfo ci) {
        ci.cancel();
    }
}
