package com.fireheart.city.client;

import com.fireheart.city.FireheartCity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The Watcher: a tall, thin figure only the targeted player can see. It waits behind them breathing heavily; when they
 * turn and look, it cocks its head sideways, lunges at the camera and is gone. Purely client-side, so nothing else in
 * the world (mobs, residents, other players) is touched.
 */
@Mod.EventBusSubscriber(modid = FireheartCity.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Stalker {
    private Stalker() {}

    static final ResourceLocation TEX = new ResourceLocation(FireheartCity.MODID, "textures/entity/stalker/body.png");
    static final ResourceLocation EYES = new ResourceLocation(FireheartCity.MODID, "textures/entity/stalker/eyes.png");
    static final SoundEvent BREATH = ev("stalker.breath"), CRACK = ev("stalker.crack"), SCREAM = ev("stalker.scream"), DRONE = ev("stalker.drone");
    static final int IDLE = 0, WAITING = 1, WATCHING = 2, NOTICED = 3, LUNGE = 4;

    static int phase = IDLE, t, life, breathT, flash;
    static Vec3 pos, prevPos;
    static float yaw, tilt, prevTilt;
    static SimpleSoundInstance breath;
    static ModelPart root, head, body, armL, armR, legL, legR;

    static SoundEvent ev(String id) {
        return SoundEvent.createVariableRangeEvent(new ResourceLocation(FireheartCity.MODID, id));
    }

    public static void handle(String line) {
        if (line.endsWith("stop")) { vanish(); return; }
        phase = WAITING;
        t = 40 + RandomSource.create().nextInt(120);
        life = 0;
        pos = null;
    }

    static void model() {
        if (root != null) return;
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -7, -3, 6, 7, 6), PartPose.offset(0, -39, 0));
        r.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(24, 0).addBox(-1, -3, -1, 2, 3, 2), PartPose.offset(0, -36, 0));
        r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-3, 0, -1.5f, 6, 14, 3).texOffs(18, 16).addBox(-2.5f, 3, -1.8f, 5, 5, 1), PartPose.offset(0, -37, 0));
        r.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(0, 34).addBox(-1.25f, 0, -1.25f, 2.5f, 24, 2.5f), PartPose.offset(1.6f, -24, 0));
        r.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(12, 34).addBox(-1.25f, 0, -1.25f, 2.5f, 24, 2.5f), PartPose.offset(-1.6f, -24, 0));
        r.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(32, 16).addBox(-1, 0, -1, 2, 26, 2).texOffs(40, 16).addBox(-1.2f, 26, -1.2f, 2.4f, 6, 1), PartPose.offset(4, -36.5f, 0));
        r.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(48, 16).addBox(-1, 0, -1, 2, 26, 2).texOffs(56, 16).addBox(-1.2f, 26, -1.2f, 2.4f, 6, 1), PartPose.offset(-4, -36.5f, 0));
        root = LayerDefinition.create(mesh, 64, 64).bakeRoot();
        head = root.getChild("head");
        body = root.getChild("body");
        armL = root.getChild("arm_l");
        armR = root.getChild("arm_r");
        legL = root.getChild("leg_l");
        legR = root.getChild("leg_r");
    }

    static void vanish() {
        phase = IDLE;
        pos = null;
        if (breath != null) Minecraft.getInstance().getSoundManager().stop(breath);
        breath = null;
    }

    static Vec3 behind(Minecraft mc, double dist) {
        var pl = mc.player;
        Vec3 look = pl.getViewVector(1).multiply(1, 0, 1);
        if (look.lengthSqr() < 1e-3) look = new Vec3(0, 0, 1);
        look = look.normalize();
        for (int tries = 0; tries < 8; tries++) {
            double ang = (tries - 3.5) * 0.18;
            Vec3 dir = look.yRot((float) ang).scale(-(dist + (tries % 3) * 0.4));
            Vec3 p = pl.position().add(dir);
            BlockPos b = BlockPos.containing(p.x, pl.getY() + 2, p.z);
            for (int dy = 0; dy < 7; dy++) {
                BlockPos q = b.below(dy);
                if (!mc.level.getBlockState(q).isAir() && mc.level.getBlockState(q.above()).getCollisionShape(mc.level, q.above()).isEmpty()
                        && mc.level.getBlockState(q.above(2)).getCollisionShape(mc.level, q.above(2)).isEmpty() && mc.level.getBlockState(q.above(3)).getCollisionShape(mc.level, q.above(3)).isEmpty()) {
                    return new Vec3(p.x, q.getY() + 1, p.z);
                }
            }
        }
        return null;
    }

    static double lookDot(Minecraft mc) {
        Vec3 eye = mc.player.getEyePosition();
        Vec3 to = pos.add(0, 2.7, 0).subtract(eye).normalize();
        return mc.player.getViewVector(1).dot(to);
    }

    static boolean clear(Minecraft mc) {
        Vec3 eye = mc.player.getEyePosition();
        var hit = mc.level.clip(new ClipContext(eye, pos.add(0, 2.6, 0), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mc.player));
        return hit.getType() == HitResult.Type.MISS;
    }

    static void sound(SoundEvent e, float vol, float pitch, Vec3 at) {
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(e, SoundSource.HOSTILE, vol, pitch, RandomSource.create(), at.x, at.y, at.z));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || phase == IDLE) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) { vanish(); return; }
        if (flash > 0) flash--;
        prevPos = pos;
        prevTilt = tilt;
        life++;
        if (phase == WAITING) {
            if (--t > 0) return;
            Vec3 b = behind(mc, 2.6);
            if (b == null) { t = 20; return; }
            pos = b;
            prevPos = b;
            if (lookDot(mc) > 0.2) { pos = null; t = 20; return; }
            phase = WATCHING;
            t = 0;
            breathT = 0;
            tilt = prevTilt = 0;
            return;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        yaw = (float) Math.atan2(-(cam.x - pos.x), -(cam.z - pos.z));
        if (phase == WATCHING) {
            t++;
            if (breathT-- <= 0) {
                breathT = 118;
                if (breath != null) mc.getSoundManager().stop(breath);
                breath = new SimpleSoundInstance(BREATH, SoundSource.HOSTILE, 1.0f, 0.92f + mc.player.getRandom().nextFloat() * 0.08f, RandomSource.create(), pos.x, pos.y + 2.6, pos.z);
                mc.getSoundManager().play(breath);
                if (mc.player.getRandom().nextInt(3) == 0) sound(DRONE, 0.35f, 0.8f, pos);
            }
            double dot = lookDot(mc);
            double dist = mc.player.position().distanceTo(pos);
            if (dot > 0.9 && clear(mc) && dist < 14) {
                phase = NOTICED;
                t = 0;
                if (breath != null) mc.getSoundManager().stop(breath);
                return;
            }
            if (dot < 0.25 && (dist > 5.5 || dist < 1.4)) {
                Vec3 b = behind(mc, 2.6);
                if (b != null) { pos = b; prevPos = b; }
            }
            if (life > 20 * 120) vanish();
            return;
        }
        if (phase == NOTICED) {
            t++;
            tilt = Mth.clamp(t / 12f, 0, 1) * 1.15f;
            if (t == 2) sound(CRACK, 1.0f, 0.8f, pos.add(0, 2.7, 0));
            if (t == 9) sound(CRACK, 0.8f, 0.6f, pos.add(0, 2.7, 0));
            if (t >= 20) {
                phase = LUNGE;
                t = 0;
                sound(SCREAM, 1.6f, 1f, cam);
            }
            return;
        }
        if (phase == LUNGE) {
            t++;
            Vec3 target = cam.subtract(0, 2.4, 0).add(mc.player.getViewVector(1).scale(0.35));
            pos = pos.lerp(target, Math.min(1, 0.45 + t * 0.08));
            HitAnim.shake = 2.5f;
            HitAnim.shakeAt = mc.level.getGameTime();
            if (t >= 6) {
                flash = 14;
                vanish();
            }
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || pos == null || phase < WATCHING) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        model();
        float pt = e.getPartialTick();
        Vec3 p = prevPos == null ? pos : prevPos.lerp(pos, pt);
        Vec3 cam = e.getCamera().getPosition();
        float time = (mc.level.getGameTime() + pt);
        head.zRot = Mth.lerp(pt, prevTilt, tilt);
        head.xRot = 0.15f + Mth.sin(time * 0.05f) * 0.03f;
        body.xRot = 0.06f;
        armL.xRot = Mth.sin(time * 0.04f) * 0.04f;
        armR.xRot = -Mth.sin(time * 0.04f + 1) * 0.04f;
        armL.zRot = -0.05f;
        armR.zRot = 0.05f;
        if (phase == LUNGE) {
            armL.xRot = -1.3f;
            armR.xRot = -1.3f;
            head.zRot = 1.15f;
        }
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(p.x - cam.x, p.y - cam.y, p.z - cam.z);
        ps.mulPose(Axis.YP.rotation(yaw));
        float s = 1.12f;
        ps.scale(-s, -s, s);
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(p.x, p.y + 1, p.z));
        root.render(ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light, OverlayTexture.NO_OVERLAY);
        root.render(ps, buf.getBuffer(RenderType.eyes(EYES)), 0xF000F0, OverlayTexture.NO_OVERLAY);
        buf.endBatch();
        ps.popPose();
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.ComputeFov e) {
        if (phase == NOTICED) e.setFOV(e.getFOV() * (1 - 0.12f * Mth.clamp(t / 20f, 0, 1)));
        if (phase == LUNGE) e.setFOV(e.getFOV() * 0.8f);
    }

    public static void hud(GuiGraphics g, int w, int h) {
        if (flash > 0) {
            int a = (int) (255 * Math.min(1, flash / 8f));
            g.fill(0, 0, w, h, a << 24);
        }
        if (phase == WATCHING) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || pos == null) return;
            float d = (float) mc.player.position().distanceTo(pos);
            int a = (int) (Mth.clamp(1 - d / 6f, 0, 1) * 70);
            if (a > 0) {
                for (int i = 0; i < 12; i++) {
                    int in = i * 6, aa = a * (12 - i) / 12;
                    g.fill(0, in, w, in + 6, aa << 24);
                    g.fill(0, h - in - 6, w, h - in, aa << 24);
                }
            }
        }
    }
}
