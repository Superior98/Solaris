package com.fireheart.city.client;

import com.fireheart.city.FireheartCity;
import com.fireheart.city.Vehicle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Renders the Solaris vehicles. Each model has four layers: paint (tinted with the car's colour), trim (tyres, bumpers,
 * seats), glass (translucent) and lamps (full-bright when the headlights are on or the brakes are pressed).
 */
public class VehicleRenderer extends EntityRenderer<Vehicle> {
    public static final ModelLayerLocation CAR = new ModelLayerLocation(new ResourceLocation(FireheartCity.MODID, "car"), "main");
    public static final ModelLayerLocation BIKE = new ModelLayerLocation(new ResourceLocation(FireheartCity.MODID, "bike"), "main");
    public static final ModelLayerLocation BOAT = new ModelLayerLocation(new ResourceLocation(FireheartCity.MODID, "boat"), "main");
    static final ResourceLocation TEX = new ResourceLocation(FireheartCity.MODID, "textures/entity/vehicle.png");
    static final int PAINT = 0, TRIM = 256, GLASS_V = 256, LAMP = 384;

    final ModelPart[] roots = new ModelPart[3];

    public VehicleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        roots[0] = ctx.bakeLayer(CAR);
        roots[1] = ctx.bakeLayer(BIKE);
        roots[2] = ctx.bakeLayer(BOAT);
        shadowRadius = 1.0f;
    }

    static CubeListBuilder paint() { return CubeListBuilder.create().texOffs(0, 0); }

    static CubeListBuilder trim() { return CubeListBuilder.create().texOffs(0, 256); }

    static CubeListBuilder glass() { return CubeListBuilder.create().texOffs(256, 256); }

    static CubeListBuilder lamp() { return CubeListBuilder.create().texOffs(256, 384); }

    static CubeListBuilder tail() { return CubeListBuilder.create().texOffs(384, 384); }

    static CubeListBuilder chrome() { return CubeListBuilder.create().texOffs(0, 384); }

    static void layers(PartDefinition r, PartDefinition[] out) {
        out[0] = r.addOrReplaceChild("paint", CubeListBuilder.create(), PartPose.ZERO);
        out[1] = r.addOrReplaceChild("trim", CubeListBuilder.create(), PartPose.ZERO);
        out[2] = r.addOrReplaceChild("glass", CubeListBuilder.create(), PartPose.ZERO);
        out[3] = r.addOrReplaceChild("lamp", CubeListBuilder.create(), PartPose.ZERO);
        out[4] = r.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.ZERO);
    }

    static void wheel(PartDefinition trim, String name, float x, float y, float z, float r, float wdt) {
        PartDefinition w = trim.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offset(x, y, z));
        w.addOrReplaceChild("tyre_a", trim().addBox(-wdt / 2, -r, -r * 0.42f, wdt, r * 2, r * 0.84f), PartPose.ZERO);
        w.addOrReplaceChild("tyre_b", trim().addBox(-wdt / 2, -r * 0.42f, -r, wdt, r * 0.84f, r * 2), PartPose.ZERO);
        w.addOrReplaceChild("tyre_c", trim().addBox(-wdt / 2, -r * 0.9f, -r * 0.9f, wdt, r * 1.8f, r * 1.8f), PartPose.rotation(Mth.PI / 4, 0, 0));
        w.addOrReplaceChild("rim", chrome().addBox(-wdt / 2 - 0.1f, -r * 0.5f, -r * 0.5f, wdt + 0.2f, r, r), PartPose.rotation(Mth.PI / 4, 0, 0));
    }

    public static LayerDefinition car() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition[] l = new PartDefinition[5];
        layers(m.getRoot(), l);
        PartDefinition p = l[0], t = l[1], g = l[2], lp = l[3], tl = l[4];
        p.addOrReplaceChild("body", paint().addBox(-14, -13, -25, 28, 8, 50), PartPose.ZERO);
        p.addOrReplaceChild("hood", paint().addBox(-13, -15, -25, 26, 2, 15), PartPose.ZERO);
        p.addOrReplaceChild("trunk", paint().addBox(-13, -15, 14, 26, 2, 11), PartPose.ZERO);
        p.addOrReplaceChild("roof", paint().addBox(-12, -23.5f, -5, 24, 1.5f, 15), PartPose.ZERO);
        p.addOrReplaceChild("pillar_fl", paint().addBox(-12.5f, -23, -9, 1.5f, 8, 1.5f), PartPose.rotation(-0.45f, 0, 0));
        p.addOrReplaceChild("pillar_fr", paint().addBox(11, -23, -9, 1.5f, 8, 1.5f), PartPose.rotation(-0.45f, 0, 0));
        p.addOrReplaceChild("pillar_rl", paint().addBox(-12.5f, -22, 11, 1.5f, 8, 2), PartPose.ZERO);
        p.addOrReplaceChild("pillar_rr", paint().addBox(11, -22, 11, 1.5f, 8, 2), PartPose.ZERO);
        p.addOrReplaceChild("arch_fl", paint().addBox(-14.5f, -12, -19, 1, 4, 10), PartPose.ZERO);
        p.addOrReplaceChild("arch_fr", paint().addBox(13.5f, -12, -19, 1, 4, 10), PartPose.ZERO);
        p.addOrReplaceChild("arch_rl", paint().addBox(-14.5f, -12, 11, 1, 4, 10), PartPose.ZERO);
        p.addOrReplaceChild("arch_rr", paint().addBox(13.5f, -12, 11, 1, 4, 10), PartPose.ZERO);
        p.addOrReplaceChild("spoiler", paint().addBox(-12, -18, 23, 24, 1, 3), PartPose.ZERO);
        g.addOrReplaceChild("windshield", glass().addBox(-11.5f, -8.5f, -0.5f, 23, 9, 1), PartPose.offsetAndRotation(0, -15, -9, 0.95f, 0, 0));
        g.addOrReplaceChild("rear_glass", glass().addBox(-11.5f, -8, -0.5f, 23, 8, 1), PartPose.offsetAndRotation(0, -15, 13.5f, -0.35f, 0, 0));
        g.addOrReplaceChild("side_l", glass().addBox(-12.2f, -22, -6, 0.5f, 7, 18), PartPose.ZERO);
        g.addOrReplaceChild("side_r", glass().addBox(11.7f, -22, -6, 0.5f, 7, 18), PartPose.ZERO);
        t.addOrReplaceChild("bumper_f", trim().addBox(-14.5f, -8, -26.5f, 29, 3, 2), PartPose.ZERO);
        t.addOrReplaceChild("bumper_r", trim().addBox(-14.5f, -8, 24.5f, 29, 3, 2), PartPose.ZERO);
        t.addOrReplaceChild("skirt", trim().addBox(-14.2f, -6, -18, 28.4f, 1.5f, 36), PartPose.ZERO);
        t.addOrReplaceChild("grill", trim().addBox(-7, -12, -25.6f, 14, 3, 1), PartPose.ZERO);
        t.addOrReplaceChild("mirror_l", trim().addBox(-16, -16, -8, 3, 2, 2), PartPose.ZERO);
        t.addOrReplaceChild("mirror_r", trim().addBox(13, -16, -8, 3, 2, 2), PartPose.ZERO);
        t.addOrReplaceChild("seat_l", trim().addBox(-10, -16, -2, 8, 3, 7).addBox(-10, -24, 4, 8, 9, 2), PartPose.ZERO);
        t.addOrReplaceChild("seat_r", trim().addBox(2, -16, -2, 8, 3, 7).addBox(2, -24, 4, 8, 9, 2), PartPose.ZERO);
        t.addOrReplaceChild("wheel_base", CubeListBuilder.create(), PartPose.ZERO);
        wheel(t, "wheel_fl", -12.5f, -5, -14, 5, 4);
        wheel(t, "wheel_fr", 12.5f, -5, -14, 5, 4);
        wheel(t, "wheel_rl", -12.5f, -5, 16, 5, 4);
        wheel(t, "wheel_rr", 12.5f, -5, 16, 5, 4);
        lp.addOrReplaceChild("head_l", lamp().addBox(-13, -13, -25.6f, 6, 2, 1), PartPose.ZERO);
        lp.addOrReplaceChild("head_r", lamp().addBox(7, -13, -25.6f, 6, 2, 1), PartPose.ZERO);
        lp.addOrReplaceChild("drl", lamp().addBox(-12, -14.2f, -25.3f, 24, 0.6f, 0.6f), PartPose.ZERO);
        tl.addOrReplaceChild("tail_bar", tail().addBox(-13, -13, 24.6f, 26, 1.5f, 1), PartPose.ZERO);
        return LayerDefinition.create(m, 512, 512);
    }

    public static LayerDefinition bike() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition[] l = new PartDefinition[5];
        layers(m.getRoot(), l);
        PartDefinition p = l[0], t = l[1], g = l[2], lp = l[3], tl = l[4];
        p.addOrReplaceChild("tank", paint().addBox(-3.5f, -17, -8, 7, 5, 10), PartPose.ZERO);
        p.addOrReplaceChild("fairing", paint().addBox(-4.5f, -19, -15, 9, 8, 5), PartPose.rotation(-0.3f, 0, 0));
        p.addOrReplaceChild("tail", paint().addBox(-3, -17, 7, 6, 3, 9), PartPose.rotation(0.12f, 0, 0));
        p.addOrReplaceChild("belly", paint().addBox(-3, -11, -8, 6, 4, 13), PartPose.ZERO);
        t.addOrReplaceChild("seat", trim().addBox(-3, -18, 1, 6, 2, 9), PartPose.ZERO);
        t.addOrReplaceChild("engine", chrome().addBox(-3.5f, -11, -5, 7, 6, 8), PartPose.ZERO);
        t.addOrReplaceChild("fork", chrome().addBox(-2.5f, -18, -15, 1, 14, 1).addBox(1.5f, -18, -15, 1, 14, 1), PartPose.rotation(-0.35f, 0, 0));
        t.addOrReplaceChild("bars", trim().addBox(-7, -21, -12, 14, 1, 1), PartPose.ZERO);
        t.addOrReplaceChild("swingarm", chrome().addBox(-2.5f, -7, 4, 5, 2, 13), PartPose.ZERO);
        t.addOrReplaceChild("exhaust", chrome().addBox(3, -8, 2, 2, 2, 14), PartPose.ZERO);
        wheel(t, "wheel_f", 0, -6, -17, 6, 3);
        wheel(t, "wheel_r", 0, -6, 16, 6, 3.5f);
        g.addOrReplaceChild("screen", glass().addBox(-3.5f, -6, -0.5f, 7, 6, 1), PartPose.offsetAndRotation(0, -19, -14, -0.5f, 0, 0));
        lp.addOrReplaceChild("head", lamp().addBox(-2, -17, -18.2f, 4, 3, 1), PartPose.ZERO);
        tl.addOrReplaceChild("tail_light", tail().addBox(-2, -16, 16, 4, 1.5f, 1), PartPose.ZERO);
        return LayerDefinition.create(m, 512, 512);
    }

    public static LayerDefinition boat() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition[] l = new PartDefinition[5];
        layers(m.getRoot(), l);
        PartDefinition p = l[0], t = l[1], g = l[2], lp = l[3], tl = l[4];
        p.addOrReplaceChild("keel", paint().addBox(-6, -4, -30, 12, 4, 58), PartPose.ZERO);
        p.addOrReplaceChild("hull_l", paint().addBox(-1, -10, -26, 2, 10, 54), PartPose.offsetAndRotation(-12, 0, 0, 0, 0, -0.45f));
        p.addOrReplaceChild("hull_r", paint().addBox(-1, -10, -26, 2, 10, 54), PartPose.offsetAndRotation(12, 0, 0, 0, 0, 0.45f));
        p.addOrReplaceChild("bow_l", paint().addBox(-1, -10, -14, 2, 10, 16), PartPose.offsetAndRotation(-8, 0, -30, 0, 0.55f, -0.35f));
        p.addOrReplaceChild("bow_r", paint().addBox(-1, -10, -14, 2, 10, 16), PartPose.offsetAndRotation(8, 0, -30, 0, -0.55f, 0.35f));
        p.addOrReplaceChild("stern", paint().addBox(-15, -11, 27, 30, 11, 2), PartPose.ZERO);
        t.addOrReplaceChild("deck", chrome().addBox(-14, -8.5f, -24, 28, 1, 50), PartPose.ZERO);
        t.addOrReplaceChild("bow_deck", chrome().addBox(-8, -10, -38, 16, 1.5f, 16), PartPose.rotation(0.08f, 0, 0));
        t.addOrReplaceChild("console", trim().addBox(-10, -17, -8, 9, 8, 5), PartPose.ZERO);
        t.addOrReplaceChild("wheel", chrome().addBox(-6.5f, -19, -6, 4, 4, 1), PartPose.ZERO);
        t.addOrReplaceChild("seat_f", trim().addBox(-11, -12, -1, 22, 3, 7).addBox(-11, -19, 5, 22, 7, 2), PartPose.ZERO);
        t.addOrReplaceChild("seat_r", trim().addBox(-12, -12, 14, 24, 3, 8), PartPose.ZERO);
        t.addOrReplaceChild("motor", trim().addBox(-4, -16, 28, 8, 9, 6).addBox(-1.5f, -7, 30, 3, 10, 2), PartPose.ZERO);
        t.addOrReplaceChild("rail_l", chrome().addBox(-14.5f, -12, -22, 1, 1, 30), PartPose.ZERO);
        t.addOrReplaceChild("rail_r", chrome().addBox(13.5f, -12, -22, 1, 1, 30), PartPose.ZERO);
        g.addOrReplaceChild("screen", glass().addBox(-13, -7, -0.5f, 26, 7, 1), PartPose.offsetAndRotation(0, -10, -12, -0.7f, 0, 0));
        lp.addOrReplaceChild("nav_l", lamp().addBox(-9, -11, -30, 2, 1.5f, 1.5f), PartPose.ZERO);
        tl.addOrReplaceChild("nav_r", tail().addBox(7, -11, -30, 2, 1.5f, 1.5f), PartPose.ZERO);
        return LayerDefinition.create(m, 512, 512);
    }

    @Override
    public void render(Vehicle v, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        int k = v.kind();
        ModelPart root = roots[Math.max(0, Math.min(2, k))];
        ps.pushPose();
        float ry = Mth.rotLerp(pt, v.yRotO, v.getYRot());
        ps.mulPose(Axis.YP.rotationDegrees(180 - ry));
        if (k == Vehicle.BIKE) ps.mulPose(Axis.ZP.rotation(Mth.lerp(pt, v.prevLean, v.lean)));
        if (k == Vehicle.BOAT) {
            float t = v.tickCount + pt;
            float sp = Math.abs(v.speed);
            ps.translate(0, Mth.sin(t * 0.12f) * 0.03f, 0);
            ps.mulPose(Axis.XP.rotation(-sp * 0.12f + Mth.sin(t * 0.09f) * 0.02f));
            ps.mulPose(Axis.ZP.rotation(Mth.sin(t * 0.07f) * 0.025f));
        }
        ps.scale(-1, -1, 1);
        float spin = Mth.lerp(pt, v.prevWheelSpin, v.wheelSpin);
        ModelPart trim = root.getChild("trim");
        for (String w : new String[]{"wheel_fl", "wheel_fr", "wheel_rl", "wheel_rr", "wheel_f", "wheel_r"}) {
            if (!trim.hasChild(w)) continue;
            ModelPart wp = trim.getChild(w);
            wp.xRot = -spin;
            wp.yRot = w.endsWith("fl") || w.endsWith("fr") || w.equals("wheel_f") ? -v.steerVis * 1.3f : 0;
        }
        int c = Vehicle.PAINT_RGB[v.paint()];
        float r = ((c >> 16) & 255) / 255f, g = ((c >> 8) & 255) / 255f, b = (c & 255) / 255f;
        int ov = OverlayTexture.NO_OVERLAY;
        root.getChild("paint").render(ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light, ov, r, g, b, 1);
        trim.render(ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light, ov);
        root.getChild("glass").render(ps, buf.getBuffer(RenderType.entityTranslucent(TEX)), light, ov, 1, 1, 1, 0.55f);
        boolean on = v.lights();
        root.getChild("lamp").render(ps, buf.getBuffer(on ? RenderType.eyes(TEX) : RenderType.entityCutoutNoCull(TEX)), on ? 0xF000F0 : light, ov);
        boolean brake = v.braking || v.speed < -0.01f;
        root.getChild("tail").render(ps, buf.getBuffer(on || brake ? RenderType.eyes(TEX) : RenderType.entityCutoutNoCull(TEX)), on || brake ? 0xF000F0 : light, ov, 1, brake ? 1 : 0.6f, brake ? 1 : 0.6f, 1);
        ps.popPose();
        super.render(v, yaw, pt, ps, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(Vehicle v) {
        return TEX;
    }
}
