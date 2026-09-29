package com.fireheart.city.client.pc;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Fireheart weather: now and the forecast. */
class WeatherApp extends App {
    private int t;

    public String title() { return "Weather"; }

    public int color() { return 0xFFFB8500; }

    public void tick() { t++; }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        Font f = os.font();
        String[] p = os.data.weather.split("\\|", 4);
        if (p.length < 4) return;
        boolean rain = !p[0].equals("Clear"), storm = p[0].startsWith("Thunder");
        g.fillGradient(x, y, x + w, y + h, rain ? 0xFF5C677D : 0xFF4CC9F0, rain ? 0xFF33415C : 0xFF4361EE);
        int cx = x + w / 2, cy = y + 34;
        if (!rain) {
            g.fill(cx - 12, cy - 12, cx + 12, cy + 12, 0xFFFFD166);
            for (int i = 0; i < 8; i++) {
                float a = i / 8f * Mth.TWO_PI + t * 0.02f;
                int rx = cx + (int) (Mth.cos(a) * 19), ry = cy + (int) (Mth.sin(a) * 19);
                g.fill(rx - 2, ry - 2, rx + 2, ry + 2, 0xFFFFD166);
            }
        } else {
            g.fill(cx - 18, cy - 6, cx + 18, cy + 8, 0xFFE9ECEF);
            g.fill(cx - 10, cy - 13, cx + 8, cy - 4, 0xFFE9ECEF);
            for (int i = 0; i < 7; i++) {
                int dx = cx - 15 + i * 5, dy = cy + 11 + (t * 2 + i * 7) % 14;
                g.fill(dx, dy, dx + 1, dy + 3, 0xFFADE8F4);
            }
            if (storm && (t / 10) % 6 == 0) g.fill(cx - 2, cy + 8, cx + 2, cy + 22, 0xFFFFD60A);
        }
        g.pose().pushPose();
        g.pose().translate(cx, cy + 30, 0);
        g.pose().scale(3, 3, 1);
        g.drawCenteredString(f, p[1] + "°", 0, 0, 0xFFFFFFFF);
        g.pose().popPose();
        g.drawCenteredString(f, p[0] + " · Solaris", cx, cy + 62, 0xFFFFFFFF);
        g.drawCenteredString(f, p[3], cx, cy + 74, 0xFFDDDDDD);
        g.fill(x + 6, cy + 88, x + w - 6, cy + 110, 0x33FFFFFF);
        g.drawCenteredString(f, f.plainSubstrByWidth(p[2], w - 16), cx, cy + 95, 0xFFFFFFFF);
    }
}
