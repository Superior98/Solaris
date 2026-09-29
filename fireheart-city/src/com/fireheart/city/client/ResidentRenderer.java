package com.fireheart.city.client;

import com.fireheart.city.Resident;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class ResidentRenderer extends HumanoidMobRenderer<Resident, PlayerModel<Resident>> {
    private static final String[] NAMES = {"steve", "alex", "ari", "efe", "kai", "makena", "noor", "sunny", "zuri"};
    private static final ResourceLocation[] SKINS = new ResourceLocation[NAMES.length];
    private static final ResourceLocation[] CUSTOM = new ResourceLocation[Resident.SKINS];

    static {
        for (int i = 0; i < CUSTOM.length; i++) CUSTOM[i] = new ResourceLocation("fireheartcity", String.format("textures/entity/residents/skin_%02d.png", i));
    }

    private static final int[] ACCENTS = {0x2EF2FF, 0xFF3FD2, 0x9DFF4A, 0xFFA23A, 0xA66BFF, 0xFFE04A, 0xFF6B9A, 0x4A8BFF, 0x3AFFC0};
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float DOTS = 8f;
    private static final float GROW = 4f;
    private static final float CPS = 1.6f;
    private static final int MAX_W = 150;

    static {
        for (int i = 0; i < NAMES.length; i++) {
            SKINS[i] = new ResourceLocation("minecraft", "textures/entity/player/wide/" + NAMES[i] + ".png");
        }
    }

    private final ResidentModel wide;
    private final ResidentModel slim;

    public ResidentRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new ResidentModel(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.wide = (ResidentModel) this.model;
        this.slim = new ResidentModel(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }

    public static boolean slim(int skin) {
        int s = Math.floorMod(skin, Resident.SKINS);
        return s >= Resident.FIRST_SLIM && s < 28;
    }

    @Override
    protected void setupRotations(Resident e, PoseStack pose, float bob, float bodyYaw, float pt) {
        int ph = e.skyPhase();
        if (ph == 0) {
            super.setupRotations(e, pose, bob, bodyYaw, pt);
            int g = e.getGesture();
            if (g == Resident.G_KICK) pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(((e.tickCount + pt) * 45f) % 360f));
            else if (g == Resident.G_DASH) pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-18f));
            return;
        }
        double vy = e.getY() - e.yo;
        float dive = ph == com.fireheart.city.Skydive.FREEFALL ? (float) Mth.clamp((-vy - 0.9) / 0.5, 0, 1) : 0;
        com.fireheart.city.client.sky.SkyPose.body(pose, ph, e.skyTime(pt), dive, 0, bodyYaw);
    }

    @Override
    public ResourceLocation getTextureLocation(Resident entity) {
        return CUSTOM[Math.floorMod(entity.getSkin(), CUSTOM.length)];
    }

    private static final net.minecraft.world.level.block.Block[] CARPETS = {
            net.minecraft.world.level.block.Blocks.WHITE_CARPET, net.minecraft.world.level.block.Blocks.ORANGE_CARPET, net.minecraft.world.level.block.Blocks.MAGENTA_CARPET, net.minecraft.world.level.block.Blocks.LIGHT_BLUE_CARPET,
            net.minecraft.world.level.block.Blocks.YELLOW_CARPET, net.minecraft.world.level.block.Blocks.LIME_CARPET, net.minecraft.world.level.block.Blocks.PINK_CARPET, net.minecraft.world.level.block.Blocks.CYAN_CARPET,
            net.minecraft.world.level.block.Blocks.PURPLE_CARPET, net.minecraft.world.level.block.Blocks.BLUE_CARPET, net.minecraft.world.level.block.Blocks.RED_CARPET, net.minecraft.world.level.block.Blocks.GREEN_CARPET,
            net.minecraft.world.level.block.Blocks.BLACK_CARPET, net.minecraft.world.level.block.Blocks.BROWN_CARPET, net.minecraft.world.level.block.Blocks.LIGHT_GRAY_CARPET, net.minecraft.world.level.block.Blocks.GRAY_CARPET};

    @Override
    public void render(Resident e, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        int w = e.getWeather();
        boolean shiver = (w & Resident.W_SHIVER) != 0 && !e.isPassenger() && !e.isSleeping() && (e.tickCount + e.getId() * 7) % 90 < 22;
        if (shiver) {
            pose.pushPose();
            float t = e.tickCount + pt;
            float j = Mth.sin(t * 2.2f) * 0.012f;
            pose.translate(j, 0, 0);
        }
        this.model = slim(e.getSkin()) ? slim : wide;
        super.render(e, yaw, pt, pose, buffers, light);
        if (shiver) pose.popPose();
        int sky = e.skyPhase();
        if (sky == com.fireheart.city.Skydive.CHUTE || sky == com.fireheart.city.Skydive.LANDED) {
            try {
                com.fireheart.city.client.sky.SkyPose.chute(pose, buffers, light, Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot), sky, e.skyTime(pt), 0, e.getSkin());
            } catch (Throwable t) {
                if (errors++ < 5) com.fireheart.city.FireheartCity.LOG.error("Parachute render failed", t);
            }
        }
        if (e.isInvisible()) return;
        if ((w & Resident.W_UMBRELLA) != 0) {
            try {
                renderUmbrella(e, pt, pose, buffers, light);
            } catch (Throwable t) {
                if (errors++ < 5) com.fireheart.city.FireheartCity.LOG.error("Umbrella render failed", t);
            }
        }
        try {
            renderBubble(e, pt, pose, buffers);
        } catch (Throwable t) {
            if (errors++ < 5) com.fireheart.city.FireheartCity.LOG.error("Speech bubble render failed", t);
        }
    }

    private static int errors;

    private void renderUmbrella(Resident e, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        net.minecraft.client.renderer.block.BlockRenderDispatcher br = net.minecraft.client.Minecraft.getInstance().getBlockRenderer();
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        float body = Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot);
        pose.pushPose();
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - body));
        pose.translate(-0.33, 0, 0.1);
        pose.pushPose();
        pose.translate(-0.5, 0.95, -0.5);
        pose.scale(1.0f, 1.2f, 1.0f);
        br.renderSingleBlock(net.minecraft.world.level.block.Blocks.CHAIN.defaultBlockState(), pose, buffers, light, overlay);
        pose.popPose();
        pose.translate(0.33, 0, 0);
        pose.pushPose();
        pose.translate(-0.7, 2.18, -0.7);
        pose.scale(1.4f, 1.0f, 1.4f);
        br.renderSingleBlock(CARPETS[Math.floorMod(e.umbrellaColor(), CARPETS.length)].defaultBlockState(), pose, buffers, light, overlay);
        pose.popPose();
        pose.pushPose();
        pose.translate(-0.45, 2.24, -0.45);
        pose.scale(0.9f, 1.0f, 0.9f);
        br.renderSingleBlock(CARPETS[Math.floorMod(e.umbrellaColor() + 5, CARPETS.length)].defaultBlockState(), pose, buffers, light, overlay);
        pose.popPose();
        pose.popPose();
    }

    private void renderBubble(Resident e, float pt, PoseStack pose, MultiBufferSource buffers) {
        float now = e.tickCount + pt;
        String s = e.getSpeech();
        if (s == null) s = "";
        if (!s.equals(e.cSpeech)) {
            if (s.isEmpty()) {
                e.cPrev = e.cSpeech;
                e.cEnd = now;
            } else {
                e.cStart = now;
                e.cBlip = 0;
                if (!s.startsWith("Zzz") && !(s.startsWith("☎") && com.fireheart.city.client.pc.ClientCall.active())) {
                    VoiceClient.speak(e.getId(), e.getVoiceId(), s, e.getX(), e.getY() + 1.6, e.getZ(), e.getGesture());
                }
            }
            e.cSpeech = s;
        }
        double dist2 = this.entityRenderDispatcher.distanceToSqr(e);
        if (dist2 > 26 * 26) return;
        float fade = dist2 < 18 * 18 ? 1f : 1f - (float) ((Math.sqrt(dist2) - 18) / 8);
        if (!e.cSpeech.isEmpty()) {
            drawSpeech(e, e.cSpeech, now - e.cStart, 1f, fade, dist2, pose, buffers);
        } else if (!e.cPrev.isEmpty()) {
            float t = now - e.cEnd;
            if (t > 5f) e.cPrev = "";
            else drawSpeech(e, e.cPrev, 9999f, 1f - t / 5f, fade, dist2, pose, buffers);
        }
    }

    private void drawSpeech(Resident e, String text, float t, float close, float fade, double dist2, PoseStack pose, MultiBufferSource buf) {
        if (text.startsWith("Zzz")) {
            drawSleep(e, fade * close, pose, buf);
            return;
        }
        Font font = this.getFont();
        List<String> lines = wrap(font, text);
        int fullW = 0;
        for (String l : lines) fullW = Math.max(fullW, font.width(l));
        int fullH = lines.size() * 10 - 1;
        boolean dots = text.length() > 14;
        float pop = easeOutBack(Mth.clamp(t / 6f, 0f, 1f));
        float grow = dots ? easeOut(Mth.clamp((t - DOTS) / GROW, 0f, 1f)) : 1f;
        float w = Mth.lerp(grow, 17f, fullW);
        float h = Mth.lerp(grow, 7f, fullH);
        float alpha = Mth.clamp(Math.min(t / 4f, 1f) * close * fade, 0f, 1f);
        if (alpha < 0.03f) return;
        float scale = 0.025f * pop * (0.85f + 0.15f * close);
        float bob = Mth.sin((e.tickCount + e.getId() * 7) * 0.08f) * 0.035f;

        pose.pushPose();
        pose.translate(0.0D, e.getBbHeight() + 0.95D + bob, 0.0D);
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.scale(-scale, -scale, scale);
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = buf.getBuffer(RenderType.textBackground());
        int accent = ACCENTS[Math.floorMod(e.getSkin(), ACCENTS.length)];
        float pulse = 0.75f + 0.25f * Mth.sin((e.tickCount + e.getId()) * 0.15f);
        int a8 = (int) (alpha * 255);
        int border = ((int) (a8 * pulse) << 24) | accent;
        int fill = ((int) (a8 * 0.88f) << 24) | 0x12121C;
        float pad = 4f;
        float x0 = -w / 2f - pad, x1 = w / 2f + pad, y1 = pad - 1f, y0 = -h - pad - 1f;
        roundRect(vc, m, x0 - 1, y0 - 1, x1 + 1, y1 + 1, 2f, border);
        tri(vc, m, -5f, y1, 5f, y1, 0f, y1 + 6.5f, 2f, border);
        roundRect(vc, m, x0, y0, x1, y1, 1f, fill);
        tri(vc, m, -3.8f, y1 - 0.2f, 3.8f, y1 - 0.2f, 0f, y1 + 5f, 1f, fill);

        float textTop = -h - 1f;
        int textColor = (Math.max(a8, 8) << 24) | 0xFFFFFF;
        if (dots && t < DOTS + 1f) {
            for (int i = 0; i < 3; i++) {
                float jump = Math.max(0f, Mth.sin((t * 0.6f) - i * 0.9f)) * 2.5f;
                int c = (Math.max(a8, 8) << 24) | accent;
                font.drawInBatch("•", -6.5f + i * 5f, textTop - jump, c, false, m, buf, Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
            }
        } else if (grow >= 0.999f) {
            float typeT = t - (dots ? DOTS + GROW : 0f);
            int reveal = t > 9000f ? Integer.MAX_VALUE : (int) (typeT * CPS);
            int total = text.length();
            if (reveal < total && reveal > e.cBlip + 2 && dist2 < 12 * 12) {
                e.cBlip = reveal;
                float pitch = 0.9f + Math.floorMod(e.getSkin() * 7 + e.getId(), 9) * 0.07f;
                e.level().playLocalSound(e.getX(), e.getY() + 1.6D, e.getZ(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.NEUTRAL, 0.06f, pitch + (e.getRandom().nextFloat() - 0.5f) * 0.1f, false);
            }
            int left = reveal;
            for (int i = 0; i < lines.size() && left > 0; i++) {
                String l = lines.get(i);
                String shown = left >= l.length() ? l : l.substring(0, left);
                left -= l.length() + 1;
                font.drawInBatch(shown, -fullW / 2f, textTop + i * 10, textColor, false, m, buf, Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
            }
        }
        pose.popPose();
    }

    private void drawSleep(Resident e, float alpha, PoseStack pose, MultiBufferSource buf) {
        Font font = this.getFont();
        float now = e.tickCount;
        for (int i = 0; i < 3; i++) {
            float ph = ((now * 0.02f) + i / 3f) % 1f;
            float a = Mth.sin(ph * Mth.PI) * alpha;
            if (a < 0.05f) continue;
            float sc = 0.018f * (0.7f + ph * 0.8f);
            pose.pushPose();
            pose.translate(0.15D + ph * 0.35D, e.getBbHeight() + 0.6D + ph * 0.7D, 0.0D);
            pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
            pose.scale(-sc, -sc, sc);
            int c = ((int) (a * 255) << 24) | 0xB8C8FF;
            font.drawInBatch("z", -2.5f, -4f, c, false, pose.last().pose(), buf, Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
            pose.popPose();
        }
    }

    private static void roundRect(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float z, int c) {
        quad(vc, m, x0 + 2, y0, x1 - 2, y1, z, c);
        quad(vc, m, x0 + 1, y0 + 1, x0 + 2, y1 - 1, z, c);
        quad(vc, m, x1 - 2, y0 + 1, x1 - 1, y1 - 1, z, c);
        quad(vc, m, x0, y0 + 2, x0 + 1, y1 - 2, z, c);
        quad(vc, m, x1 - 1, y0 + 2, x1, y1 - 2, z, c);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float z, int c) {
        v(vc, m, x0, y0, z, c); v(vc, m, x0, y1, z, c); v(vc, m, x1, y1, z, c); v(vc, m, x1, y0, z, c);
        v(vc, m, x0, y0, z, c); v(vc, m, x1, y0, z, c); v(vc, m, x1, y1, z, c); v(vc, m, x0, y1, z, c);
    }

    private static void tri(VertexConsumer vc, Matrix4f m, float ax, float ay, float bx, float by, float cx, float cy, float z, int c) {
        v(vc, m, ax, ay, z, c); v(vc, m, cx, cy, z, c); v(vc, m, bx, by, z, c); v(vc, m, bx, by, z, c);
        v(vc, m, ax, ay, z, c); v(vc, m, bx, by, z, c); v(vc, m, cx, cy, z, c); v(vc, m, cx, cy, z, c);
    }

    private static void v(VertexConsumer vc, Matrix4f m, float x, float y, float z, int c) {
        vc.vertex(m, x, y, z).color((c >> 16) & 255, (c >> 8) & 255, c & 255, (c >>> 24) & 255).uv2(FULL_BRIGHT).endVertex();
    }

    private static float easeOutBack(float x) {
        float c1 = 1.70158f, c3 = c1 + 1f;
        return 1f + c3 * (float) Math.pow(x - 1f, 3) + c1 * (float) Math.pow(x - 1f, 2);
    }

    private static float easeOut(float x) {
        return 1f - (1f - x) * (1f - x) * (1f - x);
    }

    private static List<String> wrap(Font font, String s) {
        List<String> out = new ArrayList<>();
        String cur = "";
        for (String w : s.split(" ")) {
            String test = cur.isEmpty() ? w : cur + " " + w;
            if (font.width(test) > MAX_W && !cur.isEmpty()) {
                out.add(cur);
                cur = w;
            } else {
                cur = test;
            }
        }
        if (!cur.isEmpty()) out.add(cur);
        if (out.size() > 5) {
            out = new ArrayList<>(out.subList(0, 5));
            out.set(4, out.get(4) + "...");
        }
        return out;
    }
}
