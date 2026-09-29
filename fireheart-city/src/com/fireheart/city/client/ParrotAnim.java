package com.fireheart.city.client;

import com.fireheart.city.FireheartCity;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Pet parrots near their owner bob, sway and tilt their heads cutely. */
@Mod.EventBusSubscriber(modid = FireheartCity.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ParrotAnim {
    private ParrotAnim() {}

    static boolean cute(Parrot p) {
        return p.isTame() && p.getOwnerUUID() != null && !p.isFlying() && p.onGround();
    }

    @SubscribeEvent
    public static void pre(RenderLivingEvent.Pre<?, ?> e) {
        if (!(e.getEntity() instanceof Parrot p) || !cute(p)) return;
        float t = p.tickCount + e.getPartialTick();
        float phase = (p.getId() % 7) * 0.9f;
        float bob = Math.max(0, Mth.sin(t * 0.25f + phase)) * 0.06f;
        float sway = Mth.sin(t * 0.12f + phase) * 6f;
        float squash = 1 + Mth.sin(t * 0.5f + phase) * 0.03f;
        e.getPoseStack().pushPose();
        e.getPoseStack().translate(0, bob, 0);
        e.getPoseStack().mulPose(Axis.ZP.rotationDegrees(sway));
        e.getPoseStack().scale(1 / squash, squash, 1 / squash);
    }

    @SubscribeEvent
    public static void post(RenderLivingEvent.Post<?, ?> e) {
        if (!(e.getEntity() instanceof Parrot p) || !cute(p)) return;
        e.getPoseStack().popPose();
    }
}
