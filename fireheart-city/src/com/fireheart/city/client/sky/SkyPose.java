package com.fireheart.city.client.sky;

import com.fireheart.city.Skydive;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Skydiving body poses (spread-eagle, dive, parachute, launch, landing roll) and the parachute canopy. */
public final class SkyPose {
    private SkyPose() {}

    /** Rotates the body for the pose; call where LivingEntityRenderer.setupRotations would run. */
    public static void body(PoseStack ps, int phase, float t, float dive, float turn, float yaw) {
        ps.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        switch (phase) {
            case Skydive.LAUNCH -> {
                ps.mulPose(Axis.YP.rotationDegrees(t * 22f));
                ps.translate(0, 0.9, 0);
                ps.mulPose(Axis.XP.rotationDegrees(Mth.sin(t * 0.6f) * 4f));
                ps.translate(0, -0.9, 0);
            }
            case Skydive.FREEFALL -> {
                float pitch = -90f - 50f * dive + Mth.sin(t * 0.11f) * 5f * (1 - dive);
                float roll = turn * 35f + Mth.sin(t * 0.07f) * 7f * (1 - dive * 0.6f);
                ps.translate(0, 0.9, 0);
                ps.mulPose(Axis.XP.rotationDegrees(pitch));
                ps.mulPose(Axis.YP.rotationDegrees(roll));
                ps.translate(0, -0.9, 0);
            }
            case Skydive.CHUTE -> {
                float open = Math.min(1f, t / 10f);
                float pitch = Mth.lerp(open, -90f, Mth.sin(t * 0.09f) * 7f - turn * 4f);
                ps.translate(0, 2.2, 0);
                ps.mulPose(Axis.XP.rotationDegrees(pitch));
                ps.mulPose(Axis.ZP.rotationDegrees(turn * 16f + Mth.sin(t * 0.06f) * 6f));
                ps.translate(0, -2.2, 0);
            }
            case Skydive.LANDED -> {
                if (t < 12) {
                    float k = t / 12f;
                    ps.translate(0, 0.5, 0);
                    ps.mulPose(Axis.XP.rotationDegrees(k * 360f));
                    ps.translate(0, -0.5, 0);
                }
            }
            default -> {}
        }
    }

    public static void limbs(PlayerModel<?> m, int phase, float t, float dive, float turn) {
        m.head.zRot = 0;
        switch (phase) {
            case Skydive.LAUNCH -> {
                m.rightArm.xRot = -3.0f + Mth.sin(t * 0.8f) * 0.05f;
                m.leftArm.xRot = -3.0f - Mth.sin(t * 0.8f) * 0.05f;
                m.rightArm.yRot = 0;
                m.leftArm.yRot = 0;
                m.rightArm.zRot = 0.12f;
                m.leftArm.zRot = -0.12f;
                m.rightLeg.xRot = 0.05f;
                m.leftLeg.xRot = -0.05f;
                m.rightLeg.zRot = 0.03f;
                m.leftLeg.zRot = -0.03f;
                m.head.xRot = -0.5f;
                m.head.yRot = 0;
            }
            case Skydive.FREEFALL -> {
                float s = 1 - dive;
                float sway = Mth.sin(t * 0.21f) * 0.11f, flap = Mth.sin(t * 0.37f) * 0.05f;
                m.rightArm.xRot = Mth.lerp(dive, 0.35f, 0.3f) + flap;
                m.leftArm.xRot = Mth.lerp(dive, 0.35f, 0.3f) - flap;
                m.rightArm.yRot = 0;
                m.leftArm.yRot = 0;
                m.rightArm.zRot = Mth.lerp(dive, 1.35f, 0.22f) + sway * s + turn * 0.35f * s;
                m.leftArm.zRot = -Mth.lerp(dive, 1.35f, 0.22f) + sway * s + turn * 0.35f * s;
                m.rightLeg.xRot = Mth.lerp(dive, 0.55f, 0.08f) + Mth.sin(t * 0.25f + 1f) * 0.1f * s;
                m.leftLeg.xRot = Mth.lerp(dive, 0.55f, 0.08f) - Mth.sin(t * 0.25f + 1f) * 0.1f * s;
                m.rightLeg.yRot = 0;
                m.leftLeg.yRot = 0;
                m.rightLeg.zRot = Mth.lerp(dive, 0.42f, 0.04f);
                m.leftLeg.zRot = -Mth.lerp(dive, 0.42f, 0.04f);
                m.head.xRot = Mth.lerp(dive, -1.15f, -0.45f) + Mth.sin(t * 0.13f) * 0.05f;
                m.head.yRot = -turn * 0.3f;
            }
            case Skydive.CHUTE -> {
                float pull = turn * 0.45f;
                m.rightArm.xRot = -2.75f + Math.max(0, pull) + Mth.sin(t * 0.1f) * 0.04f;
                m.leftArm.xRot = -2.75f + Math.max(0, -pull) - Mth.sin(t * 0.1f) * 0.04f;
                m.rightArm.yRot = 0;
                m.leftArm.yRot = 0;
                m.rightArm.zRot = 0.28f;
                m.leftArm.zRot = -0.28f;
                m.rightLeg.xRot = Mth.sin(t * 0.12f) * 0.22f;
                m.leftLeg.xRot = -Mth.sin(t * 0.12f + 0.6f) * 0.22f;
                m.rightLeg.zRot = 0.08f;
                m.leftLeg.zRot = -0.08f;
                m.head.xRot = 0.25f;
            }
            case Skydive.LANDED -> {
                float k = Math.min(1f, t / 20f);
                m.rightArm.zRot = Mth.lerp(k, 0.9f, 0.05f);
                m.leftArm.zRot = -Mth.lerp(k, 0.9f, 0.05f);
                m.rightArm.xRot = Mth.lerp(k, -0.6f, 0f);
                m.leftArm.xRot = Mth.lerp(k, -0.6f, 0f);
            }
            default -> {}
        }
        m.hat.copyFrom(m.head);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftPants.copyFrom(m.leftLeg);
        m.rightPants.copyFrom(m.rightLeg);
        m.jacket.copyFrom(m.body);
    }

    static final Block[] CANOPY = {Blocks.ORANGE_WOOL, Blocks.WHITE_WOOL, Blocks.RED_WOOL, Blocks.WHITE_WOOL, Blocks.ORANGE_WOOL, Blocks.WHITE_WOOL, Blocks.RED_WOOL};
    static final Block[][] PALETTES = {
            {Blocks.ORANGE_WOOL, Blocks.WHITE_WOOL, Blocks.RED_WOOL},
            {Blocks.CYAN_WOOL, Blocks.WHITE_WOOL, Blocks.BLUE_WOOL},
            {Blocks.MAGENTA_WOOL, Blocks.PINK_WOOL, Blocks.PURPLE_WOOL},
            {Blocks.LIME_WOOL, Blocks.YELLOW_WOOL, Blocks.GREEN_WOOL},
            {Blocks.YELLOW_WOOL, Blocks.BLACK_WOOL, Blocks.ORANGE_WOOL}};

    /** Draws the parachute: a curved canopy of seven cells 4.5 blocks overhead with lines to the shoulders. */
    public static void chute(PoseStack ps, MultiBufferSource buf, int light, float yaw, int phase, float t, float turn, int seed) {
        float open = phase == Skydive.CHUTE ? Math.min(1f, t / 8f) : Math.max(0f, 1f - t / 10f);
        if (open <= 0.01f) return;
        var br = Minecraft.getInstance().getBlockRenderer();
        Block[] pal = PALETTES[Math.floorMod(seed, PALETTES.length)];
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-yaw));
        float swing = phase == Skydive.CHUTE ? Mth.sin(t * 0.09f) * 7f : 0;
        ps.mulPose(Axis.XP.rotationDegrees(swing * 0.4f));
        ps.mulPose(Axis.ZP.rotationDegrees(-turn * 12f));
        float lift = phase == Skydive.LANDED ? -t * 0.35f : 0;
        float height = 1.8f + 2.3f * open + lift;
        int cells = 7;
        float cw = 0.78f * (0.4f + 0.6f * open), depth = 2.3f * (0.5f + 0.5f * open);
        float[][] tips = new float[cells + 1][];
        for (int i = 0; i < cells; i++) {
            float c = i - (cells - 1) / 2f;
            float ang = c * 11f * open;
            float cx = c * cw * 0.97f, cy = height - c * c * 0.06f * open;
            ps.pushPose();
            ps.translate(cx, cy, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(-ang));
            ps.translate(-cw / 2f, 0, -depth / 2f);
            ps.scale(cw, 0.14f, depth);
            br.renderSingleBlock(pal[i % 3 == 1 ? 1 : i % 2 == 0 ? 0 : 2].defaultBlockState(), ps, buf, light, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }
        VertexConsumer vc = buf.getBuffer(RenderType.lines());
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        float edge = (cells / 2f) * cw * 0.95f;
        float[] xs = {-edge, -edge * 0.4f, edge * 0.4f, edge};
        for (float x : xs) {
            float y = height - (x / cw) * (x / cw) * 0.06f * open;
            for (float z : new float[]{-depth / 2f + 0.1f, depth / 2f - 0.1f}) {
                float sx = x > 0 ? 0.3f : -0.3f;
                line(vc, m, n, x, y, z, sx, 1.45f, 0f);
            }
        }
        ps.popPose();
    }

    static void line(VertexConsumer vc, Matrix4f m, Matrix3f n, float x0, float y0, float z0, float x1, float y1, float z1) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= len; dy /= len; dz /= len;
        vc.vertex(m, x0, y0, z0).color(40, 40, 40, 200).normal(n, dx, dy, dz).endVertex();
        vc.vertex(m, x1, y1, z1).color(40, 40, 40, 200).normal(n, dx, dy, dz).endVertex();
    }
}
