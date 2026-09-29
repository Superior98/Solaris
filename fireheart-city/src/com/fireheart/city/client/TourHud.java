package com.fireheart.city.client;

import com.fireheart.city.FireheartCity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Nova's holo-guide overlay for the Sky Tour: stop cards, typewriter narration and a camera that drifts to each sight. */
@Mod.EventBusSubscriber(modid = FireheartCity.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TourHud {
    private TourHud() {}

    static final ResourceLocation NOVA = new ResourceLocation(FireheartCity.MODID, "textures/entity/residents/skin_27.png");
    static boolean on;
    static String title = "", line = "";
    static int stop, total, lineT, titleT, ticks;
    static Vec3 look;

    public static void handle(String msg) {
        String[] p = msg.split("\\|", 8);
        if (p.length < 2) return;
        switch (p[1]) {
            case "start" -> { on = true; title = "Solaris Sky Tour"; line = ""; titleT = 0; stop = 0; look = null; }
            case "end" -> { on = false; look = null; }
            case "stop" -> {
                if (p.length < 8) return;
                stop = parse(p[2]);
                total = parse(p[3]);
                title = p[4];
                titleT = 0;
                look = new Vec3(dbl(p[5]), dbl(p[6]), dbl(p[7]));
            }
            case "say" -> { line = msg.substring(msg.indexOf("|say|") + 5); lineT = 0; }
            default -> {}
        }
    }

    static int parse(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    static double dbl(String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !on) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { on = false; return; }
        ticks++;
        titleT++;
        lineT++;
        int prev = Math.min(line.length(), (lineT - 1) / 2 * 3 + 3), now = Math.min(line.length(), lineT / 2 * 3 + 3);
        if (now > prev && prev < line.length() && lineT % 4 == 0 && !line.substring(prev, now).isBlank())
            mc.player.playSound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HAT.value(), 0.08f, 1.8f + (lineT % 7) * 0.05f);
        if (look != null && mc.player.isPassenger()) {
            Vec3 eye = mc.player.getEyePosition();
            Vec3 d = look.subtract(eye);
            float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f;
            float pitch = (float) (-(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG));
            float cy = mc.player.getYRot(), cp = mc.player.getXRot();
            float ny = cy + Mth.wrapDegrees(yaw - cy) * 0.06f, np = cp + (pitch - cp) * 0.06f;
            mc.player.setYRot(ny);
            mc.player.setXRot(np);
            mc.player.yRotO = ny;
            mc.player.xRotO = np;
        }
    }

    public static void hud(GuiGraphics g, int w, int h, float pt) {
        if (!on) return;
        Minecraft mc = Minecraft.getInstance();
        Font f = mc.font;
        float ta = Mth.clamp((titleT + pt) / 12f, 0, 1);
        if (titleT < 120) {
            int a = (int) (255 * ta * Mth.clamp((120 - titleT) / 20f, 0, 1));
            if (a > 8) {
                g.pose().pushPose();
                g.pose().translate(w / 2f, h * 0.2f + (1 - ta) * 12, 0);
                g.pose().scale(2.2f, 2.2f, 1);
                g.drawCenteredString(f, "§b" + title, 0, 0, (a << 24) | 0xFFFFFF);
                g.pose().popPose();
                if (stop > 0) g.drawCenteredString(f, "§7STOP " + stop + " / " + total, w / 2, (int) (h * 0.2f) + 24, (a << 24) | 0xFFFFFF);
            }
        }
        if (!line.isEmpty()) {
            int bw = Math.min(360, w - 40), bx = w / 2 - bw / 2;
            int shown = Math.min(line.length(), lineT / 2 * 3 + 3);
            String txt = line.substring(0, shown);
            var lines = f.split(Component.literal(txt), bw - 46);
            int bh = Math.max(40, 18 + lines.size() * 10);
            int by = h - bh - 46;
            g.fill(bx, by, bx + bw, by + bh, 0xC0081624);
            int glow = 0xFF3AC8FF;
            g.fill(bx, by, bx + bw, by + 1, glow);
            g.fill(bx, by + bh - 1, bx + bw, by + bh, glow);
            g.fill(bx, by, bx + 1, by + bh, glow);
            g.fill(bx + bw - 1, by, bx + bw, by + bh, glow);
            for (int sy = by + 2; sy < by + bh - 1; sy += 3) g.fill(bx + 1, sy, bx + bw - 1, sy + 1, 0x103AC8FF);
            int scan = by + (int) ((ticks + pt) * 1.5f % bh);
            g.fill(bx + 1, scan, bx + bw - 1, scan + 1, 0x303AC8FF);
            PlayerFaceRenderer.draw(g, NOVA, bx + 8, by + 8, 28);
            g.fill(bx + 7, by + 7, bx + 37, by + 8, glow);
            g.drawString(f, "§b§lNOVA §7· Sky Guide", bx + 42, by + 5, 0xFFFFFFFF, false);
            int ly = by + 16;
            for (FormattedCharSequence s : lines) {
                g.drawString(f, s, bx + 42, ly, 0xFFE6F7FF, false);
                ly += 10;
            }
            if (shown < line.length() && (ticks / 4) % 2 == 0) g.fill(bx + bw - 10, by + bh - 8, bx + bw - 6, by + bh - 4, glow);
        }
        g.drawString(f, "§8Sneak to leave the tour", 6, h - 12, 0xFFFFFFFF, false);
    }
}
