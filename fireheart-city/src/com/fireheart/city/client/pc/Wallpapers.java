package com.fireheart.city.client.pc;

import net.minecraft.client.gui.GuiGraphics;

/** SolPhone wallpapers: painted presets, or one of your own photos. */
final class Wallpapers {
    private Wallpapers() {}

    static final String[] NAMES = {"Skyline", "Sunset", "Aurora", "Starry night", "Ocean", "Neon grid", "Solar flare", "Fox moon"};

    static void draw(GuiGraphics g, String wall, int x, int y, int w, int h, int body) {
        if (PhotoCache.isPhoto(wall)) {
            if (PhotoCache.draw(g, wall, x, y, w, h)) {
                g.fillGradient(x, y, x + w, y + h, 0x22000000, 0x88000000);
                return;
            }
        }
        int i;
        try { i = Math.floorMod(Integer.parseInt(wall), NAMES.length); } catch (NumberFormatException e) { i = 0; }
        long t = System.currentTimeMillis();
        switch (i) {
            case 1 -> {
                g.fillGradient(x, y, x + w, y + h * 2 / 3, 0xFF6A0572, 0xFFFF7B54);
                g.fillGradient(x, y + h * 2 / 3, x + w, y + h, 0xFFFFB26B, 0xFF3D1766);
                circle(g, x + w / 2, y + h * 2 / 3, 18, 0xCCFFD56B);
                for (int k = 0; k < 5; k++) g.fill(x, y + h * 2 / 3 + 4 + k * 6, x + w, y + h * 2 / 3 + 5 + k * 6, 0x33FFFFFF);
            }
            case 2 -> {
                g.fillGradient(x, y, x + w, y + h, 0xFF03071E, 0xFF0B3D2E);
                for (int k = 0; k < w; k += 2) {
                    double a = Math.sin(k * 0.05 + t / 1400.0) * 18 + Math.sin(k * 0.13) * 6;
                    int top = y + h / 4 + (int) a;
                    g.fillGradient(x + k, top, x + k + 2, top + 50, 0x9938F2A0, 0x0038F2A0);
                }
                stars(g, x, y, w, h / 2, 30, t);
            }
            case 3 -> {
                g.fillGradient(x, y, x + w, y + h, 0xFF0B1026, 0xFF2B2F77);
                stars(g, x, y, w, h, 70, t);
                g.fill(x, y + h - 30, x + w, y + h, 0xFF05070F);
            }
            case 4 -> {
                g.fillGradient(x, y, x + w, y + h / 2, 0xFF90E0EF, 0xFFCAF0F8);
                g.fillGradient(x, y + h / 2, x + w, y + h, 0xFF0077B6, 0xFF03045E);
                for (int k = 0; k < 8; k++) {
                    int wy = y + h / 2 + 6 + k * 12;
                    int off = (int) ((t / 60 + k * 17) % 40);
                    for (int xx = -40 + off; xx < w; xx += 40) g.fill(x + Math.max(0, xx), wy, x + Math.min(w, xx + 16), wy + 1, 0x55FFFFFF);
                }
            }
            case 5 -> {
                g.fillGradient(x, y, x + w, y + h, 0xFF10002B, 0xFF3C096C);
                int hz = y + h / 2;
                g.fill(x, hz, x + w, hz + 1, 0xFFFF4D6D);
                for (int k = 1; k < 12; k++) {
                    int gy = hz + (int) (k * k * 1.3 + (t / 80 % 10) * k * 0.26);
                    if (gy < y + h) g.fill(x, gy, x + w, gy + 1, 0x88F72585);
                }
                for (int k = -8; k <= 8; k++) for (int yy = hz; yy < y + h; yy += 2) g.fill(x + w / 2 + (int) (k * (yy - hz) * 0.9), yy, x + w / 2 + (int) (k * (yy - hz) * 0.9) + 1, yy + 1, 0x664CC9F0);
                circle(g, x + w / 2, hz - 20, 20, 0xDDFFBE0B);
            }
            case 6 -> {
                g.fillGradient(x, y, x + w, y + h, 0xFFFFBA08, 0xFFD00000);
                int cx = x + w / 2, cy = y + h / 3;
                for (int r = 60; r > 20; r -= 8) circle(g, cx, cy, r + (int) (Math.sin(t / 500.0 + r) * 2), 0x18FFFFFF);
                circle(g, cx, cy, 20, 0xFFFFF3B0);
            }
            case 7 -> {
                g.fillGradient(x, y, x + w, y + h, 0xFF050A30, 0xFF1B2A6B);
                stars(g, x, y, w, h, 45, t);
                circle(g, x + w * 2 / 3, y + h / 4, 16, 0xFFE8EAF6);
                circle(g, x + w * 2 / 3 + 7, y + h / 4 - 4, 14, 0xFF0A1440);
                int base = y + h - 24;
                g.fill(x, base, x + w, y + h, 0xFF030616);
                int fx = x + w / 3;
                g.fill(fx, base - 14, fx + 12, base, 0xFF030616);
                g.fill(fx + 1, base - 19, fx + 4, base - 14, 0xFF030616);
                g.fill(fx + 8, base - 19, fx + 11, base - 14, 0xFF030616);
                g.fill(fx + 12, base - 4, fx + 22, base - 1, 0xFF030616);
            }
            default -> {
                int top = (body & 0xFFFFFF) | 0xFF000000;
                g.fillGradient(x, y, x + w, y + h, top, 0xFF101828);
                int base = y + h;
                int[] bx = {4, 18, 30, 46, 62, 78, 92, 108, 122};
                int[] bh = {24, 40, 30, 52, 34, 60, 28, 44, 32};
                for (int k = 0; k < bx.length; k++) {
                    int bw = 10 + (k * 5) % 8;
                    g.fill(x + bx[k], base - bh[k], x + bx[k] + bw, base, 0x6614213D);
                }
            }
        }
    }

    static void circle(GuiGraphics g, int cx, int cy, int r, int col) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.sqrt(r * r - dy * dy);
            g.fill(cx - dx, cy + dy, cx + dx, cy + dy + 1, col);
        }
    }

    static void stars(GuiGraphics g, int x, int y, int w, int h, int n, long t) {
        for (int k = 0; k < n; k++) {
            int sx = x + Math.floorMod(k * 7919, w), sy = y + Math.floorMod(k * 104729, Math.max(1, h));
            boolean tw = (t / 400 + k) % 7 == 0;
            g.fill(sx, sy, sx + 1, sy + 1, tw ? 0xFFFFFFFF : 0x99FFFFFF);
        }
    }
}
