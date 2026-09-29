package com.fireheart.city.client.pc;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Drawing for the SolBox: boot intro, animated home menu, and what onlookers see while someone plays. */
final class ConsoleUi {
    private ConsoleUi() {}

    static final String[] TILES = {"Neon Drift", "Stellar Fox Run", "Neon Maze 3D", "Beat Solaris", "Snake", "2048", "Ferry Flap", "Mines", "Blocks", "SolTube", "Photos"};
    static final int[] TILE_COL = {0xFFFF2E88, 0xFF1B3A6B, 0xFF00B4D8, 0xFF7209B7, 0xFF38B000, 0xFFEDC22E, 0xFF48CAE4, 0xFFE63946, 0xFF9B5DE5, 0xFFD62828, 0xFFFF8FAB};
    static final String[] GLYPH = {"»", "✦", "▣", "♪", "S", "2", "F", "✹", "▦", "▶", "◉"};
    static final int GAME_TILES = 9, GEN2_TILES = 4;
    static final int BOOT = 120;

    static void boot(GuiGraphics g, Font f, int t, int w, int h) {
        g.fill(0, 0, w, h, 0xFF000000);
        float k = Mth.clamp(t / 60f, 0, 1);
        int cx = w / 2, cy = h / 2 - 6;
        if (t > 8) {
            int rays = 16;
            for (int i = 0; i < rays; i++) {
                float a = i / (float) rays * Mth.TWO_PI + t * 0.02f;
                float len = 12 + k * 60;
                for (int s = 0; s < 18; s++) {
                    float d = 14 + s * len / 18f;
                    int px = cx + (int) (Mth.cos(a) * d), py = cy + (int) (Mth.sin(a) * d * 0.6f);
                    int alpha = (int) (Mth.clamp(1 - s / 18f, 0, 1) * 160 * k);
                    g.fill(px, py, px + 2, py + 2, (alpha << 24) | 0xFFB703);
                }
            }
        }
        int r = (int) (4 + k * 14 + Mth.sin(t * 0.2f) * 1.5f);
        Wallpapers.circle(g, cx, cy, r + 3, 0x55FB8500);
        Wallpapers.circle(g, cx, cy, r, 0xFFFFB703);
        Wallpapers.circle(g, cx, cy, Math.max(1, r - 4), 0xFFFFF3B0);
        if (t > 50) {
            int a = (int) (Mth.clamp((t - 50) / 25f, 0, 1) * 255);
            g.pose().pushPose();
            g.pose().translate(cx, cy + 26, 0);
            g.pose().scale(2f, 2f, 1f);
            g.drawCenteredString(f, "SolBox", 0, 0, (Math.max(4, a) << 24) | 0xFFFFFF);
            g.pose().popPose();
        }
        if (t > 85) {
            int a = (int) (Mth.clamp((t - 85) / 20f, 0, 1) * 200);
            g.drawCenteredString(f, "Solaris Entertainment", cx, cy + 46, (Math.max(4, a) << 24) | 0xAAAAAA);
        }
        if (t > BOOT - 12) g.fill(0, 0, w, h, (int) ((t - (BOOT - 12)) / 12f * 255) << 24 | 0xFFFFFF);
    }

    static void menu(GuiGraphics g, Font f, int t, int w, int h, int sel, String player, String clock) {
        g.fillGradient(0, 0, w, h, 0xFF0B132B, 0xFF1C2541);
        for (int k = 0; k < w; k += 3) {
            int wy = (int) (h * 0.62f + Mth.sin(k * 0.035f + t * 0.05f) * 8 + Mth.sin(k * 0.011f - t * 0.02f) * 6);
            g.fill(k, wy, k + 3, h, 0x223A86FF);
            int wy2 = (int) (h * 0.72f + Mth.sin(k * 0.05f - t * 0.04f) * 5);
            g.fill(k, wy2, k + 3, h, 0x225BC0EB);
        }
        g.drawString(f, "§l" + player, 8, 6, 0xFFFFFFFF, false);
        g.drawString(f, clock, w - 8 - f.width(clock), 6, 0xFFDDDDDD, false);
        Wallpapers.circle(g, w - 16 - f.width(clock), 10, 3, 0xFFFFB703);
        int tw = 44, th = 44, gap = 8;
        int total = TILES.length * (tw + gap);
        float scroll = Mth.clamp(sel * (tw + gap) - w / 2f + tw / 2f + 10, 0, Math.max(0, total - w + 16));
        int y0 = h / 2 - th / 2 - 4;
        for (int i = 0; i < TILES.length; i++) {
            int x0 = 8 + i * (tw + gap) - (int) scroll;
            boolean on = i == sel;
            int grow = on ? 4 + (int) (Mth.sin(t * 0.15f) * 1.5f) : 0;
            int bx0 = x0 - grow, by0 = y0 - grow, bx1 = x0 + tw + grow, by1 = y0 + th + grow;
            if (on) PhoneScreen.roundRect(g, bx0 - 2, by0 - 2, bx1 + 2, by1 + 2, 7, 0xFFFFFFFF);
            PhoneScreen.roundRect(g, bx0, by0, bx1, by1, 6, TILE_COL[i]);
            g.fillGradient(bx0, by0, bx1, by0 + (by1 - by0) / 2, 0x33FFFFFF, 0x00FFFFFF);
            g.pose().pushPose();
            g.pose().translate((bx0 + bx1) / 2f, (by0 + by1) / 2f - 7, 0);
            g.pose().scale(2f, 2f, 1f);
            g.drawCenteredString(f, GLYPH[i], 0, 0, 0xFFFFFFFF);
            g.pose().popPose();
            if (i < GEN2_TILES) g.drawString(f, "§d2", bx1 - 8, by0 + 2, 0xFFFFFFFF, false);
            if (on) g.drawCenteredString(f, "§l" + TILES[i] + (i < GEN2_TILES ? " §d· Gen 2" : i < GAME_TILES ? " §7· Classic" : ""), (bx0 + bx1) / 2, by1 + 6, 0xFFFFFFFF);
        }
        g.drawCenteredString(f, "§7◀ ▶ choose   ⏎ play   P pause   Esc power off" + (Gamepad.present ? "   §a🎮" : ""), w / 2, h - 12, 0xFFFFFFFF);
    }

    /** What other players see on the TV while someone plays a game. */
    static void watching(GuiGraphics g, Font f, int t, int w, int h, String game, int score, String player) {
        int idx = 0;
        for (int i = 0; i < TILES.length; i++) if (TILES[i].equals(game)) idx = i;
        int col = TILE_COL[idx];
        g.fillGradient(0, 0, w, h, 0xFF101418, (col & 0xFFFFFF) | 0x66000000);
        for (int i = 0; i < 24; i++) {
            int px = Math.floorMod(i * 37 + t * (1 + i % 3), w), py = Math.floorMod(i * 53 + (int) (Mth.sin(t * 0.05f + i) * 20), h - 20) + 10;
            int s = 3 + i % 4;
            g.fill(px, py, px + s, py + s, (col & 0xFFFFFF) | 0xAA000000);
        }
        g.pose().pushPose();
        g.pose().translate(w / 2f, h / 2f - 16, 0);
        g.pose().scale(2f, 2f, 1f);
        g.drawCenteredString(f, "§l" + game, 0, 0, 0xFFFFFFFF);
        g.pose().popPose();
        g.drawCenteredString(f, player + " is playing", w / 2, h / 2 + 6, 0xFFDDDDDD);
        g.drawCenteredString(f, "Score §e" + score, w / 2, h / 2 + 18, 0xFFFFFFFF);
    }

    static void slideshow(GuiGraphics g, Font f, java.util.List<String> keys, int t, int w, int h) {
        g.fill(0, 0, w, h, 0xFF000000);
        if (keys.isEmpty()) {
            g.drawCenteredString(f, "No photos yet - take some with your SolPhone!", w / 2, h / 2 - 4, 0xFFFFFFFF);
            return;
        }
        int i = (t / 100) % keys.size();
        float zoom = 1 + (t % 100) / 100f * 0.06f;
        int pw = (int) (w * zoom), ph = (int) (h * zoom);
        Pics.draw(g, keys.get(i), (w - pw) / 2, (h - ph) / 2, pw, ph);
        int fade = t % 100;
        if (fade < 10) g.fill(0, 0, w, h, (int) ((10 - fade) / 10f * 255) << 24);
        if (fade > 90) g.fill(0, 0, w, h, (int) ((fade - 90) / 10f * 255) << 24);
        g.drawString(f, (i + 1) + " / " + keys.size(), 6, h - 12, 0xCCFFFFFF, false);
    }
}
