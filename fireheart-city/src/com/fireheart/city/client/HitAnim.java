package com.fireheart.city.client;

import com.fireheart.city.FireheartCity;
import com.fireheart.city.Resident;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Impact animations for mobs hit by Solaris PD: recoil tilts, squash and stretch, launch spins, pancakes and taser shakes. */
@Mod.EventBusSubscriber(modid = FireheartCity.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class HitAnim {
    private HitAnim() {}

    record Hit(String kind, float dx, float dz, int power, float at) {}

    static final Map<Integer, Hit> HITS = new HashMap<>();
    static final Map<Integer, Boolean> PUSHED = new HashMap<>();
    static float shake;
    static float shakeAt;

    public static void handle(String line) {
        String[] p = line.split("\\|");
        if (p.length < 6) return;
        try {
            int id = Integer.parseInt(p[1]);
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            var e = mc.level.getEntity(id);
            float now = mc.level.getGameTime();
            if (p[2].equals("quake")) {
                if (mc.player != null && e != null && mc.player.distanceTo(e) < 16) {
                    shake = Integer.parseInt(p[5]) * (1 - mc.player.distanceTo(e) / 16f);
                    shakeAt = now;
                }
                return;
            }
            HITS.put(id, new Hit(p[2], Float.parseFloat(p[3]), Float.parseFloat(p[4]), Integer.parseInt(p[5]), now));
        } catch (Exception ignored) {}
    }

    static float duration(String kind) {
        return switch (kind) {
            case "launch" -> 24;
            case "spin" -> 18;
            case "spike", "slam" -> 22;
            case "zap" -> 30;
            case "heavy" -> 14;
            default -> 9;
        };
    }

    @SubscribeEvent
    public static void pre(RenderLivingEvent.Pre<?, ?> e) {
        LivingEntity ent = e.getEntity();
        if (ent instanceof Resident) return;
        Hit h = HITS.get(ent.getId());
        if (h == null) return;
        float now = ent.level().getGameTime() + e.getPartialTick();
        float t = now - h.at();
        float dur = duration(h.kind());
        if (t < 0 || t > dur) {
            HITS.remove(ent.getId());
            return;
        }
        float k = t / dur;
        float decay = (1 - k) * (1 - k);
        float wobble = Mth.sin(t * 1.3f) * decay;
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        PUSHED.put(ent.getId(), true);
        float half = ent.getBbHeight() / 2f;
        float yaw = (float) Math.toDegrees(Math.atan2(h.dz(), h.dx()));
        float power = Math.max(1, h.power());
        switch (h.kind()) {
            case "jab" -> {
                ps.mulPose(Axis.YP.rotationDegrees(-yaw + 90));
                ps.mulPose(Axis.XP.rotationDegrees(-14 * wobble - 8 * decay));
                ps.mulPose(Axis.YP.rotationDegrees(yaw - 90));
                float sq = 1 - 0.1f * decay;
                ps.scale(1 / sq, sq, 1 / sq);
            }
            case "heavy" -> {
                ps.mulPose(Axis.YP.rotationDegrees(-yaw + 90));
                ps.mulPose(Axis.XP.rotationDegrees(-35 * decay - 10 * wobble));
                ps.mulPose(Axis.YP.rotationDegrees(yaw - 90));
                float sq = 1 - 0.25f * decay * (t < 3 ? 1 : 0.4f);
                ps.scale(1 / sq, sq, 1 / sq);
            }
            case "launch", "spike" -> {
                ps.translate(0, half, 0);
                ps.mulPose(Axis.YP.rotationDegrees(-yaw + 90));
                ps.mulPose(Axis.XP.rotationDegrees((h.kind().equals("spike") ? 1 : -1) * 540 * (1 - decay) ));
                ps.mulPose(Axis.YP.rotationDegrees(yaw - 90));
                ps.translate(0, -half, 0);
                float st = 1 + 0.3f * decay;
                ps.scale(1 / Mth.sqrt(st), st, 1 / Mth.sqrt(st));
            }
            case "spin" -> {
                ps.translate(0, half, 0);
                ps.mulPose(Axis.YP.rotationDegrees(1080 * (1 - decay)));
                ps.mulPose(Axis.ZP.rotationDegrees(25 * decay));
                ps.translate(0, -half, 0);
            }
            case "slam" -> {
                float sq = t < 4 ? 1 - 0.55f * (t / 4f) : 0.45f + 0.55f * Math.min(1, (t - 4) / 10f) + 0.15f * Mth.sin((t - 4) * 1.4f) * decay;
                ps.scale(1 + (1 - sq) * 0.7f, sq, 1 + (1 - sq) * 0.7f);
            }
            case "zap" -> {
                float j = 0.06f * (1 - k) * power;
                ps.translate((ent.getRandom().nextFloat() - 0.5f) * j * 2, (ent.getRandom().nextFloat() - 0.5f) * j, (ent.getRandom().nextFloat() - 0.5f) * j * 2);
                float pulse = 1 + 0.06f * Mth.sin(t * 3f) * (1 - k);
                ps.scale(pulse, pulse, pulse);
            }
            default -> {}
        }
    }

    @SubscribeEvent
    public static void post(RenderLivingEvent.Post<?, ?> e) {
        if (PUSHED.remove(e.getEntity().getId()) != null) e.getPoseStack().popPose();
    }

    @SubscribeEvent
    public static void camera(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || shake <= 0) return;
        float t = mc.level.getGameTime() + (float) e.getPartialTick() - shakeAt;
        if (t > 12) { shake = 0; return; }
        float a = shake * (1 - t / 12f) * 1.6f;
        e.setPitch(e.getPitch() + Mth.sin(t * 2.7f) * a);
        e.setYaw(e.getYaw() + Mth.cos(t * 3.1f) * a * 0.7f);
    }
}
