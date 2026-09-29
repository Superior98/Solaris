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
 * The Watcher: a tall, thin figure only the targeted player can see. It starts with omens (footsteps behind them,
 * whispers, the lights dimming), then glimpses at the edge of their vision that vanish when looked at, each one closer.
 * Finally it stands right behind them, breathing. When they turn around its head twitches and cracks sideways, its jaw
 * drops, the camera is dragged onto it, and it lunges. Purely client-side: nothing else in the world is touched.
 */
@Mod.EventBusSubscriber(modid = FireheartCity.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Stalker {
    private Stalker() {}

    static final ResourceLocation TEX = new ResourceLocation(FireheartCity.MODID, "textures/entity/stalker/body.png");
    static final ResourceLocation EYES = new ResourceLocation(FireheartCity.MODID, "textures/entity/stalker/eyes.png");
    static final SoundEvent BREATH = ev("stalker.breath"), CRACK = ev("stalker.crack"), SCREAM = ev("stalker.scream"), DRONE = ev("stalker.drone"),
            STEP = ev("stalker.step"), HEART = ev("stalker.heart"), WHISPER = ev("stalker.whisper"), STATIC = ev("stalker.static"), STING = ev("stalker.sting");
    static final int IDLE = 0, WAITING = 1, OMENS = 2, GLIMPSE = 3, WATCHING = 4, NOTICED = 5, LUNGE = 6;

    static int phase = IDLE, t, life, breathT, glimpses, nextGlimpse, dim, afterT, heartT;
    static Vec3 pos, prevPos;
    static float yaw, tilt, prevTilt, jaw, creep;
    static SimpleSoundInstance breath;
    static ModelPart root, head, jawPart, body, armL, armR, legL, legR;
    static final RandomSource RND = RandomSource.create();

    static SoundEvent ev(String id) {
        return SoundEvent.createVariableRangeEvent(new ResourceLocation(FireheartCity.MODID, id));
    }

    public static void handle(String line) {
        if (line.endsWith("stop")) {
            vanish();
            afterT = 0;
            dim = 0;
            return;
        }
        phase = WAITING;
        t = 60 + RND.nextInt(100);
        life = 0;
        glimpses = 0;
        pos = null;
    }

    static void model() {
        if (root != null) return;
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        PartDefinition h = r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -8, -3, 6, 6, 6), PartPose.offset(0, -40, 0));
        h.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 0).addBox(-2.6f, 0, -3, 5.2f, 2.5f, 5.6f), PartPose.offset(0, -2.2f, 0.2f));
        r.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(24, 0).addBox(-0.8f, -4, -0.8f, 1.6f, 4, 1.6f), PartPose.offset(0, -36, 0));
        r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-2.6f, 0, -1.3f, 5.2f, 14, 2.6f)
                .texOffs(18, 16).addBox(-3.1f, 1.5f, -1.6f, 6.2f, 0.7f, 0.5f).addBox(-3.1f, 3.5f, -1.6f, 6.2f, 0.7f, 0.5f)
                .addBox(-2.9f, 5.5f, -1.6f, 5.8f, 0.7f, 0.5f).addBox(-2.6f, 7.5f, -1.6f, 5.2f, 0.7f, 0.5f), PartPose.offset(0, -37, 0));
        r.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(0, 34).addBox(-1, 0, -1, 2, 24, 2), PartPose.offset(1.4f, -24, 0));
        r.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(12, 34).addBox(-1, 0, -1, 2, 24, 2), PartPose.offset(-1.4f, -24, 0));
        CubeListBuilder arm = CubeListBuilder.create().texOffs(32, 16).addBox(-0.8f, 0, -0.8f, 1.6f, 29, 1.6f)
                .texOffs(40, 16).addBox(-1.2f, 29, -0.4f, 0.5f, 7, 0.5f).addBox(-0.4f, 29, -0.8f, 0.5f, 8, 0.5f).addBox(0.4f, 29, -0.4f, 0.5f, 7.5f, 0.5f).addBox(-0.4f, 29, 0.3f, 0.5f, 6, 0.5f);
        r.addOrReplaceChild("arm_l", arm, PartPose.offset(3.6f, -36.5f, 0));
        r.addOrReplaceChild("arm_r", arm, PartPose.offset(-3.6f, -36.5f, 0));
        root = LayerDefinition.create(mesh, 64, 64).bakeRoot();
        head = root.getChild("head");
        jawPart = head.getChild("jaw");
        body = root.getChild("body");
        armL = root.getChild("arm_l");
        armR = root.getChild("arm_r");
        legL = root.getChild("leg_l");
        legR = root.getChild("leg_r");
    }

    static void vanish() {
        phase = IDLE;
        pos = null;
        prevPos = null;
        tilt = prevTilt = jaw = 0;
        if (breath != null) Minecraft.getInstance().getSoundManager().stop(breath);
        breath = null;
    }

    static Vec3 ground(Minecraft mc, double x, double z, double fromY) {
        BlockPos b = BlockPos.containing(x, fromY + 3, z);
        for (int dy = 0; dy < 10; dy++) {
            BlockPos q = b.below(dy);
            if (!mc.level.getBlockState(q).getCollisionShape(mc.level, q).isEmpty() && mc.level.getBlockState(q.above()).getCollisionShape(mc.level, q.above()).isEmpty()
                    && mc.level.getBlockState(q.above(2)).getCollisionShape(mc.level, q.above(2)).isEmpty())
                return new Vec3(x, q.getY() + 1, z);
        }
        return null;
    }

    static Vec3 around(Minecraft mc, double angleFromLook, double dist) {
        var pl = mc.player;
        Vec3 look = pl.getViewVector(1).multiply(1, 0, 1);
        if (look.lengthSqr() < 1e-3) look = new Vec3(0, 0, 1);
        look = look.normalize();
        for (int tries = 0; tries < 8; tries++) {
            double a = angleFromLook + (tries - 3.5) * 0.12;
            Vec3 dir = look.yRot((float) a).scale(dist + (tries % 3) * 0.5);
            Vec3 g = ground(mc, pl.getX() + dir.x, pl.getZ() + dir.z, pl.getY());
            if (g != null) return g;
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

    static Vec3 behindPoint(Minecraft mc, double d) {
        Vec3 look = mc.player.getViewVector(1).multiply(1, 0, 1);
        if (look.lengthSqr() < 1e-3) look = new Vec3(0, 0, 1);
        return mc.player.position().subtract(look.normalize().scale(d)).add(0, 1, 0);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (afterT > 0) afterT--;
        if (dim > 0) dim--;
        if (heartT > 0) heartT--;
        if (phase == IDLE) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) { vanish(); afterT = 0; dim = 0; return; }
        if (mc.isPaused()) return;
        prevPos = pos;
        prevTilt = tilt;
        life++;
        switch (phase) {
            case WAITING -> {
                if (--t <= 0) { phase = OMENS; t = 0; }
            }
            case OMENS -> {
                t++;
                if (t % 50 == 10 && t < 400) {
                    Vec3 b = behindPoint(mc, 3 + RND.nextInt(4));
                    sound(STEP, 0.9f, 0.8f + RND.nextFloat() * 0.2f, b);
                    if (RND.nextInt(3) == 0) sound(STEP, 0.8f, 0.85f, b.add(0.4, 0, 0.4));
                }
                if (t == 120 || t == 300) sound(WHISPER, 0.8f, 0.85f + RND.nextFloat() * 0.2f, behindPoint(mc, 1.5));
                if (t == 200) sound(DRONE, 0.5f, 0.7f, mc.player.position());
                if (t % 90 == 45) dim = 6 + RND.nextInt(6);
                if (t >= 420) { phase = GLIMPSE; t = 0; nextGlimpse = 20; }
            }
            case GLIMPSE -> glimpse(mc);
            case WATCHING -> watching(mc);
            case NOTICED -> noticed(mc);
            case LUNGE -> lunge(mc);
            default -> {}
        }
        if (life > 20 * 240 && phase < NOTICED) vanish();
    }

    static void glimpse(Minecraft mc) {
        if (pos == null) {
            if (--nextGlimpse > 0) return;
            double dist = 22 - glimpses * 6;
            double side = (RND.nextBoolean() ? 1 : -1) * (1.05 + RND.nextDouble() * 0.35);
            pos = around(mc, side, dist);
            prevPos = pos;
            t = 0;
            if (pos != null && mc.player.getRandom().nextInt(2) == 0) sound(STEP, 0.6f, 0.7f, pos);
            return;
        }
        t++;
        faceCamera(mc);
        double dot = lookDot(mc);
        if ((dot > 0.93 && clear(mc)) || t > 200) {
            if (dot > 0.93) {
                sound(STATIC, 0.6f, 1f, mc.player.position());
                dim = 4;
            }
            pos = null;
            glimpses++;
            nextGlimpse = 120 + RND.nextInt(160);
            if (glimpses >= 3) {
                phase = WATCHING;
                t = 0;
                breathT = 0;
                creep = 4.5f;
                nextGlimpse = 80;
            }
        }
    }

    static void faceCamera(Minecraft mc) {
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        yaw = (float) Math.atan2(-(cam.x - pos.x), -(cam.z - pos.z));
    }

    static void watching(Minecraft mc) {
        if (pos == null) {
            if (--nextGlimpse > 0) return;
            Vec3 b = around(mc, Math.PI, creep);
            if (b == null || lookDot0(mc, b) > 0.1) { nextGlimpse = 20; return; }
            pos = b;
            prevPos = b;
            return;
        }
        t++;
        faceCamera(mc);
        if (breathT-- <= 0) {
            breathT = 118;
            if (breath != null) mc.getSoundManager().stop(breath);
            breath = new SimpleSoundInstance(BREATH, SoundSource.HOSTILE, 1.1f, 0.88f + RND.nextFloat() * 0.08f, RandomSource.create(), pos.x, pos.y + 2.6, pos.z);
            mc.getSoundManager().play(breath);
            if (RND.nextInt(2) == 0) sound(DRONE, 0.35f, 0.7f, pos);
        }
        double dot = lookDot(mc);
        double dist = mc.player.position().distanceTo(pos);
        if (dot > 0.88 && clear(mc) && dist < 16) {
            phase = NOTICED;
            t = 0;
            if (breath != null) mc.getSoundManager().stop(breath);
            sound(STING, 1.3f, 1f, mc.player.position());
            return;
        }
        if (dot < 0.2 && t % 60 == 30 && creep > 1.4f) {
            creep = Math.max(1.4f, creep - 0.8f);
            Vec3 b = around(mc, Math.PI, creep);
            if (b != null) {
                sound(STEP, 0.7f, 0.75f, b);
                pos = b;
                prevPos = b;
            }
        }
        if (dot < 0.25 && (dist > 7 || dist < 1.1)) {
            Vec3 b = around(mc, Math.PI, creep);
            if (b != null) { pos = b; prevPos = b; }
        }
    }

    static double lookDot0(Minecraft mc, Vec3 p) {
        Vec3 to = p.add(0, 2.7, 0).subtract(mc.player.getEyePosition()).normalize();
        return mc.player.getViewVector(1).dot(to);
    }

    static void noticed(Minecraft mc) {
        t++;
        faceCamera(mc);
        if (t < 10) tilt = (RND.nextFloat() - 0.5f) * 0.5f;
        else tilt = Mth.clamp((t - 10) / 16f, 0, 1) * 1.35f + (t % 4 < 2 ? 0.05f : -0.05f);
        jaw = Mth.clamp((t - 18) / 10f, 0, 1);
        if (t == 2 || t == 6 || t == 12) sound(CRACK, 1.1f, 0.6f + t * 0.02f, pos.add(0, 2.7, 0));
        if (t % 12 == 0) { sound(HEART, 1.2f, 1f + t / 60f, mc.player.position()); heartT = 6; }
        Vec3 head = pos.add(0, 2.9, 0);
        Vec3 d = head.subtract(mc.player.getEyePosition());
        float wy = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f;
        float wp = (float) (-(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG));
        float cy = mc.player.getYRot(), cp = mc.player.getXRot();
        float ny = cy + Mth.wrapDegrees(wy - cy) * 0.35f, np = cp + (wp - cp) * 0.35f;
        mc.player.setYRot(ny);
        mc.player.setXRot(np);
        mc.player.yRotO = ny;
        mc.player.xRotO = np;
        if (t >= 34) {
            phase = LUNGE;
            t = 0;
            sound(SCREAM, 2f, 0.9f + RND.nextFloat() * 0.15f, mc.gameRenderer.getMainCamera().getPosition());
        }
    }

    static void lunge(Minecraft mc) {
        t++;
        faceCamera(mc);
        jaw = 1;
        tilt = 1.35f + (RND.nextFloat() - 0.5f) * 0.3f;
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 target = cam.subtract(0, 2.55, 0).add(mc.player.getViewVector(1).scale(0.3));
        pos = pos.lerp(target, Math.min(1, 0.5 + t * 0.1));
        HitAnim.shake = 3f;
        HitAnim.shakeAt = mc.level.getGameTime();
        if (t >= 6) {
            afterT = 50;
            vanish();
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || pos == null || phase < GLIMPSE) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        model();
        float pt = e.getPartialTick();
        Vec3 p = prevPos == null ? pos : prevPos.lerp(pos, pt);
        Vec3 cam = e.getCamera().getPosition();
        float time = mc.level.getGameTime() + pt;
        head.zRot = Mth.lerp(pt, prevTilt, tilt);
        head.xRot = 0.2f + Mth.sin(time * 0.05f) * 0.03f + (phase == WATCHING && RND.nextInt(40) == 0 ? 0.3f : 0);
        head.yRot = phase == WATCHING ? Mth.sin(time * 0.02f) * 0.15f : 0;
        jawPart.xRot = jaw * 0.9f;
        jawPart.y = -2.2f + jaw * 1.5f;
        body.xRot = 0.1f;
        armL.xRot = Mth.sin(time * 0.04f) * 0.05f;
        armR.xRot = -Mth.sin(time * 0.04f + 1) * 0.05f;
        armL.zRot = -0.06f;
        armR.zRot = 0.06f;
        legL.xRot = legR.xRot = 0;
        if (phase == NOTICED) {
            armL.zRot = -0.06f - jaw * 0.5f;
            armR.zRot = 0.06f + jaw * 0.5f;
        }
        if (phase == LUNGE) {
            armL.xRot = -1.5f;
            armR.xRot = -1.5f;
            armL.zRot = -0.3f;
            armR.zRot = 0.3f;
        }
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(p.x - cam.x, p.y - cam.y, p.z - cam.z);
        ps.mulPose(Axis.YP.rotation(yaw));
        float s = 1.18f;
        ps.scale(-s, -s, s);
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        int light = Math.min(LevelRenderer.getLightColor(mc.level, BlockPos.containing(p.x, p.y + 1, p.z)), 0x500050);
        root.render(ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light, OverlayTexture.NO_OVERLAY);
        boolean flicker = phase == WATCHING && RND.nextInt(12) == 0;
        if (!flicker) {
            float glow = phase >= NOTICED ? 1f : 0.75f;
            root.render(ps, buf.getBuffer(RenderType.eyes(EYES)), 0xF000F0, OverlayTexture.NO_OVERLAY, glow, phase >= NOTICED ? 0.85f : glow, phase >= NOTICED ? 0.85f : glow, 1);
        }
        buf.endBatch();
        ps.popPose();
    }

    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov e) {
        if (phase == NOTICED) e.setFOV(e.getFOV() * (1 - 0.18f * Mth.clamp(t / 30f, 0, 1)));
        else if (phase == LUNGE) e.setFOV(e.getFOV() * 0.72f);
    }

    public static void hud(GuiGraphics g, int w, int h) {
        if (afterT > 0) {
            float k = afterT > 30 ? 1f : afterT / 30f;
            g.fill(0, 0, w, h, ((int) (255 * k) << 24));
            if (afterT > 8) for (int i = 0; i < 40; i++) {
                int y = RND.nextInt(h), x = RND.nextInt(w);
                g.fill(x, y, x + RND.nextInt(60) + 10, y + 1, ((int) (90 * k) << 24) | 0xFFFFFF);
            }
            return;
        }
        if (dim > 0) g.fill(0, 0, w, h, (Math.min(170, dim * 28)) << 24);
        if (phase == NOTICED) {
            int red = heartT > 0 ? heartT * 18 : 0;
            if (red > 0) g.fill(0, 0, w, h, (red << 24) | 0x550000);
            vignette(g, w, h, 150);
        } else if (phase == WATCHING && pos != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            float d = (float) mc.player.position().distanceTo(pos);
            vignette(g, w, h, (int) (Mth.clamp(1 - d / 7f, 0, 1) * 110));
        }
    }

    static void vignette(GuiGraphics g, int w, int h, int a) {
        if (a <= 0) return;
        for (int i = 0; i < 14; i++) {
            int in = i * 5, aa = a * (14 - i) / 14;
            g.fill(0, in, w, in + 5, aa << 24);
            g.fill(0, h - in - 5, w, h - in, aa << 24);
            g.fill(in, 0, in + 5, h, (aa / 2) << 24);
            g.fill(w - in - 5, 0, w - in, h, (aa / 2) << 24);
        }
    }
}
