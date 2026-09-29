package com.fireheart.city.client.pc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.GuiGraphics;

/** Photos in feeds, texts and the camera roll: real captured photos, or painted scenes of Solaris landmarks. */
final class Pics {
    private Pics() {}

    static final Pattern TAG = Pattern.compile("\\[(pic|scene):([a-z0-9_]*)\\]");

    static String scene(String text) {
        Matcher m = TAG.matcher(text);
        return m.find() ? m.group(2) : null;
    }

    static String strip(String text) {
        return TAG.matcher(text).replaceAll("").replaceAll("\\s+", " ").trim();
    }

    static String label(String key) {
        return switch (key) {
            case "gardens" -> "Sky Gardens";
            case "observatory" -> "Observatory";
            case "pier" -> "The old pier";
            case "marina" -> "Marina";
            case "boardwalk" -> "Boardwalk";
            case "plaza" -> "Solaris Plaza";
            case "statue" -> "Founder's statue";
            case "memorial" -> "Memorial";
            case "organ" -> "Sky Organ";
            case "clock" -> "Clock tower";
            case "park" -> "The park";
            case "ferry_isle" -> "Ferry terminal";
            case "isle_plaza" -> "Neon Heights";
            default -> "Solaris";
        };
    }

    static void draw(GuiGraphics g, String key, int x, int y, int w, int h) {
        if (PhotoCache.isPhoto(key)) {
            PhotoCache.draw(g, key, x, y, w, h);
            return;
        }
        int hash = key == null ? 0 : key.hashCode();
        boolean night = key != null && (key.equals("observatory") || key.equals("organ") || key.equals("isle_plaza"));
        int skyTop = night ? 0xFF10163A : 0xFF4EA8DE, skyBot = night ? 0xFF5A189A : 0xFFFFD6A5;
        if (key != null && (key.equals("pier") || key.equals("marina") || key.equals("boardwalk"))) skyBot = 0xFFFFB4A2;
        g.fillGradient(x, y, x + w, y + h, skyTop, skyBot);
        int ground = y + h * 2 / 3;
        if (night) for (int i = 0; i < 9; i++) g.fill(x + Math.floorMod(hash * (i + 3), w), y + Math.floorMod(hash / (i + 2), h / 2), x + Math.floorMod(hash * (i + 3), w) + 1, y + Math.floorMod(hash / (i + 2), h / 2) + 1, 0xFFFFFFFF);
        else g.fill(x + w - 14, y + 5, x + w - 7, y + 12, 0xFFFFF3B0);
        String k = key == null ? "" : key;
        switch (k) {
            case "pier", "marina", "boardwalk" -> {
                g.fill(x, ground, x + w, y + h, 0xFF1D6FA3);
                g.fill(x, ground, x + w, ground + 1, 0xFF9AD1F5);
                g.fill(x + 6, ground - 3, x + w / 2 + 10, ground, 0xFF7F5539);
                for (int i = 0; i < 4; i++) g.fill(x + 10 + i * 12, ground, x + 12 + i * 12, y + h - 3, 0xFF5E3B1E);
                if (k.equals("marina")) { g.fill(x + w - 30, ground - 6, x + w - 12, ground, 0xFFFFFFFF); g.fill(x + w - 22, ground - 16, x + w - 21, ground - 6, 0xFF333333); }
            }
            case "gardens" -> {
                g.fill(x, ground, x + w, y + h, 0xFF52B788);
                for (int i = 0; i < 6; i++) {
                    int tx = x + 6 + i * (w - 12) / 6;
                    g.fill(tx + 2, ground - 6, tx + 3, ground, 0xFF6B4226);
                    g.fill(tx, ground - 11, tx + 6, ground - 5, i % 2 == 0 ? 0xFF2D6A4F : 0xFFFF8FAB);
                }
            }
            case "observatory" -> {
                g.fill(x, ground, x + w, y + h, 0xFF3C096C);
                g.fill(x + w / 2 - 12, ground - 12, x + w / 2 + 12, ground, 0xFFD8D8D8);
                g.fill(x + w / 2 - 9, ground - 17, x + w / 2 + 9, ground - 12, 0xFFBDBDBD);
                g.fill(x + w / 2 + 4, ground - 22, x + w / 2 + 7, ground - 14, 0xFF6C757D);
            }
            case "organ" -> {
                g.fill(x, ground, x + w, y + h, 0xFF240046);
                for (int i = 0; i < 9; i++) g.fill(x + w / 2 - 18 + i * 4, ground - 8 - (i < 5 ? i : 8 - i) * 3, x + w / 2 - 16 + i * 4, ground, 0xFFE0AAFF);
                g.fill(x + 4, ground + 2, x + w - 4, ground + 3, 0xFFFF4D6D);
            }
            case "clock" -> {
                g.fill(x, ground, x + w, y + h, 0xFF6C757D);
                g.fill(x + w / 2 - 6, ground - 26, x + w / 2 + 6, ground, 0xFFB08968);
                g.fill(x + w / 2 - 4, ground - 22, x + w / 2 + 4, ground - 14, 0xFFFFFFFF);
                g.fill(x + w / 2, ground - 21, x + w / 2 + 1, ground - 18, 0xFF000000);
            }
            case "statue", "memorial" -> {
                g.fill(x, ground, x + w, y + h, 0xFFADB5BD);
                g.fill(x + w / 2 - 8, ground - 5, x + w / 2 + 8, ground, 0xFF6C757D);
                g.fill(x + w / 2 - 3, ground - 20, x + w / 2 + 3, ground - 5, k.equals("statue") ? 0xFFD4A017 : 0xFFE9ECEF);
                g.fill(x + w / 2 - 2, ground - 24, x + w / 2 + 2, ground - 20, k.equals("statue") ? 0xFFD4A017 : 0xFFE9ECEF);
            }
            case "ferry_isle" -> {
                g.fill(x, ground, x + w, y + h, 0xFF7209B7);
                g.fill(x + 10, y + 12, x + 34, y + 20, 0xFFE63946);
                g.fill(x + 10, y + 15, x + 34, y + 16, 0xFFFFFFFF);
                g.fill(x + 16, y + 21, x + 28, y + 25, 0xFF7F5539);
            }
            case "isle_plaza" -> {
                g.fill(x, ground, x + w, y + h, 0xFF3C096C);
                int[] bh = {14, 24, 18, 30, 20};
                for (int i = 0; i < bh.length; i++) {
                    int bx = x + 6 + i * (w - 12) / bh.length;
                    g.fill(bx, ground - bh[i], bx + 8, ground, 0xFF1B1B3A);
                    g.fill(bx + 2, ground - bh[i] + 3, bx + 3, ground - bh[i] + 4, 0xFFFF4D6D);
                    g.fill(bx + 5, ground - bh[i] + 7, bx + 6, ground - bh[i] + 8, 0xFF4CC9F0);
                }
            }
            default -> {
                g.fill(x, ground, x + w, y + h, 0xFF8D99AE);
                int[] bh = {16, 22, 12, 28, 18, 14};
                for (int i = 0; i < bh.length; i++) {
                    int bx = x + 4 + i * (w - 8) / bh.length;
                    g.fill(bx, ground - bh[i], bx + (w - 8) / bh.length - 2, ground, i % 2 == 0 ? 0xFFB5838D : 0xFF6D6875);
                }
                g.fill(x + 3, ground + 3, x + w - 3, ground + 4, 0xFFFFFFFF);
            }
        }
        g.fill(x, y, x + w, y + 1, 0x33000000);
        g.fill(x, y + h - 1, x + w, y + h, 0x33000000);
    }
}
