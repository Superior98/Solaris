package com.fireheart.city.client;

import com.fireheart.city.FireheartCity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Cinematic overlays for Solaris PD ultimates and finishers: letterbox bars, title cards, flash, FOV punch and camera roll. */
@Mod.EventBusSubscriber(modid = FireheartCity.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CinemaFx {
    private CinemaFx() {}

    static String kind = "", title = "", sub = "";
    static long start = -1;
    static int length, focusId = -1;

    public static void handle(String line) {
        String[] p = line.split("\\|");
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || p.length < 4) return;
        start = mc.level.getGameTime();
        kind = p[1];
        if (kind.equals("ult")) {
            focusId = parse(p[2]);
            title = "§b§lULTIMATE";
            sub = "§f" + p[3] + " §7- §e" + (p.length > 4 ? p[4] : "");
            length = 60;
        } else if (kind.equals("finish") && p.length >= 6) {
            focusId = parse(p[3]);
            title = "§6§lFINISHER";
            sub = "§f" + p[4] + " §7- §c" + p[5];
            length = 70;
        }
    }

    static int parse(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return -1; }
    }

    static float age(float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (start < 0 || mc.level == null) return -1;
        float a = mc.level.getGameTime() - start + pt;
        if (a > length) { start = -1; return -1; }
        return a;
    }

    public static void hud(GuiGraphics g, int w, int h, float pt) {
        float a = age(pt);
        if (a < 0) return;
        float in = Mth.clamp(a / 8f, 0, 1), out = Mth.clamp((length - a) / 10f, 0, 1);
        int bar = (int) (h * 0.12f * Math.min(in, out));
        g.fill(0, 0, w, bar, 0xFF000000);
        g.fill(0, h - bar, w, h, 0xFF000000);
        int flashAt = kind.equals("finish") ? 27 : 30;
        float fl = a - flashAt;
        if (fl >= 0 && fl < 8) g.fill(0, 0, w, h, ((int) (220 * (1 - fl / 8f)) << 24) | 0xFFFFFF);
        var f = Minecraft.getInstance().font;
        float ta = Mth.clamp((a - 4) / 6f, 0, 1) * out;
        if (ta <= 0.02f) return;
        int alpha = (int) (ta * 255) << 24;
        float slide = (1 - Mth.clamp((a - 4) / 6f, 0, 1)) * 60;
        g.pose().pushPose();
        g.pose().translate(w / 2f - slide, h * 0.62f, 0);
        g.pose().scale(3f, 3f, 1);
        g.drawCenteredString(f, title, 0, 0, 0xFFFFFF | alpha);
        g.pose().popPose();
        g.pose().pushPose();
        g.pose().translate(w / 2f + slide, h * 0.62f + 32, 0);
        g.pose().scale(1.4f, 1.4f, 1);
        g.drawCenteredString(f, sub, 0, 0, 0xFFFFFF | alpha);
        g.pose().popPose();
        int lw = (int) (w * 0.35f * ta);
        g.fill(w / 2 - lw, (int) (h * 0.62f) - 6, w / 2 + lw, (int) (h * 0.62f) - 5, (alpha & 0xAA000000) | (kind.equals("ult") ? 0x55CCFF : 0xFFC030));
    }

    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov e) {
        float a = age((float) e.getPartialTick());
        if (a < 0) return;
        float k;
        if (kind.equals("finish")) k = a < 27 ? -0.28f * Mth.clamp(a / 20f, 0, 1) : 0.12f * Mth.clamp(1 - (a - 27) / 25f, 0, 1);
        else k = a < 30 ? -0.18f * Mth.clamp(a / 25f, 0, 1) : 0.1f * Mth.clamp(1 - (a - 30) / 20f, 0, 1);
        e.setFOV(e.getFOV() * (1 + k));
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles e) {
        float a = age((float) e.getPartialTick());
        if (a < 0) return;
        int hit = kind.equals("finish") ? 27 : 30;
        float roll = Mth.sin(a * 0.12f) * 3f * Mth.clamp(1 - Math.abs(a - hit) / 30f, 0, 1);
        float shake = a >= hit && a < hit + 14 ? (1 - (a - hit) / 14f) * 2.5f : 0;
        e.setRoll(e.getRoll() + roll + Mth.sin(a * 5.1f) * shake);
        e.setPitch(e.getPitch() + Mth.cos(a * 4.3f) * shake);
    }
}
