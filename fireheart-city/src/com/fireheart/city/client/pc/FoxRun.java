package com.fireheart.city.client.pc;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/** Stellar Fox Run: a parallax night-city runner. Double jump, star dash, star coins, patrol drones and rooftop gaps. */
class FoxRun extends Gen2Game {
    final List<float[]> plats = new ArrayList<>();
    final List<float[]> stars = new ArrayList<>();
    final List<float[]> drones = new ArrayList<>();
    float fx, fy, vy, py, cam, pcam, speed;
    int jumps, dashT, dashCd, coins, dronesHit, stomp;
    boolean onGround;

    public String title() { return "Stellar Fox Run"; }

    public int color() { return 0xFF1B3A6B; }

    int accent() { return 0xFF7FDBFF; }

    String id() { return "foxrun"; }

    String help() { return "Space/↑ jump (x2) · → star dash · land on drones"; }

    String[][] achievementList() {
        return new String[][]{{"coins25", "Stargazer (25 star coins)"}, {"far", "Rooftop Marathon (1000 m)"}, {"stomp5", "Drone Stomper (5)"}, {"coins100", "Constellation (100 coins)"}};
    }

    boolean achieved(String k) {
        return switch (k) {
            case "coins25" -> coins >= 25;
            case "coins100" -> coins >= 100;
            case "far" -> cam / 10 >= 1000;
            case "stomp5" -> stomp >= 5;
            default -> false;
        };
    }

    void reset() {
        plats.clear();
        stars.clear();
        drones.clear();
        fx = 60;
        fy = h - 60;
        py = fy;
        vy = 0;
        cam = 0;
        pcam = 0;
        speed = 2.4f;
        coins = 0;
        stomp = 0;
        jumps = 0;
        dashT = 0;
        dashCd = 0;
        float px = -20;
        while (px < w * 3) px = addPlat(px);
    }

    float addPlat(float px) {
        float pw = 60 + rnd.nextInt(120), gap = plats.isEmpty() ? 0 : 20 + rnd.nextInt(40) + speed * 6;
        float top = Mth.clamp((plats.isEmpty() ? h - 40 : plats.get(plats.size() - 1)[1]) + (rnd.nextInt(3) - 1) * 18, h * 0.45f, h - 24);
        float x0 = px + gap;
        plats.add(new float[]{x0, top, pw});
        for (int i = 0; i < 3 && rnd.nextFloat() < 0.8f; i++) stars.add(new float[]{x0 + 15 + i * 16, top - 22 - (i == 1 ? 8 : 0), 0});
        if (rnd.nextFloat() < 0.35f) drones.add(new float[]{x0 + pw * 0.6f, top - 28, 0, rnd.nextFloat() * 6});
        return x0 + pw;
    }

    void step() {
        pcam = cam;
        py = fy;
        speed = Math.min(6.5f, 2.4f + cam / 6000f);
        float sp = speed * (dashT > 0 ? 2.2f : 1);
        cam += sp;
        if (dashT > 0) dashT--;
        if (dashCd > 0) dashCd--;
        vy = dashT > 0 ? 0 : Math.min(7, vy + 0.42f);
        fy += vy;
        onGround = false;
        float wx = cam + fx;
        for (float[] p : plats) {
            if (wx + 6 > p[0] && wx - 6 < p[0] + p[2] && py <= p[1] && fy >= p[1]) {
                fy = p[1];
                vy = 0;
                onGround = true;
                jumps = 0;
            }
        }
        if (onGround && ticks % 4 == 0) burst(fx - 6, fy - 1, 1, 0x557FDBFF, 0.4f);
        while (plats.get(plats.size() - 1)[0] < cam + w * 2) addPlat(plats.get(plats.size() - 1)[0] + plats.get(plats.size() - 1)[2]);
        plats.removeIf(p -> p[0] + p[2] < cam - 50);
        for (float[] s : stars) {
            if (s[2] == 0 && Math.abs(s[0] - wx) < 9 && Math.abs(s[1] - (fy - 8)) < 11) {
                s[2] = 1;
                coins++;
                score += 10;
                burst(s[0] - cam, s[1], 8, 0xFFFFD166, 1.5f);
                sfx(SoundEvents.NOTE_BLOCK_BELL.value(), 1.4f + (coins % 5) * 0.1f);
            }
        }
        stars.removeIf(s -> s[0] < cam - 30);
        for (float[] d : drones) {
            d[3] += 0.08f;
            float dy = d[1] + Mth.sin(d[3]) * 6;
            if (d[2] == 0 && Math.abs(d[0] - wx) < 10 && Math.abs(dy - (fy - 8)) < 11) {
                if (vy > 0.5f && fy - 8 < dy) {
                    d[2] = 1;
                    vy = -6.5f;
                    stomp++;
                    score += 25;
                    burst(d[0] - cam, dy, 20, 0xFFFF6B6B, 2.5f);
                    shake(3, 6);
                    sfx(SoundEvents.PLAYER_ATTACK_CRIT, 1.2f);
                } else if (dashT > 0) {
                    d[2] = 1;
                    score += 25;
                    burst(d[0] - cam, dy, 20, 0xFFFF6B6B, 2.5f);
                } else {
                    die();
                    return;
                }
            }
        }
        drones.removeIf(d -> d[0] < cam - 30);
        score = Math.max(score, (int) (cam / 10) + coins * 10 + stomp * 25);
        if (fy > h + 20) die();
    }

    void die() {
        shake(6, 12);
        burst(fx, Math.min(h - 10, fy - 8), 40, 0xFF7FDBFF, 3);
        sfx(SoundEvents.FOX_HURT, 1f);
        gameOver();
    }

    boolean input(int k) {
        if (k == SPACE || up(k)) {
            if (onGround || jumps < 2) {
                vy = jumps == 0 && onGround ? -8.2f : -7f;
                jumps = onGround ? 1 : jumps + 1;
                onGround = false;
                burst(fx, fy, 6, 0xFFFFFFFF, 1.2f);
                sfx(SoundEvents.NOTE_BLOCK_HAT.value(), jumps == 1 ? 1.5f : 1.9f);
            }
            return true;
        }
        if (right(k) && dashCd <= 0) {
            dashT = 14;
            dashCd = 60;
            sfx(SoundEvents.FIRECHARGE_USE, 1.8f);
            return true;
        }
        return false;
    }

    boolean clickGame(double mx, double my, int b) {
        return input(SPACE);
    }

    void draw(GuiGraphics g, int mx, int my, float pt) {
        float c = pcam + (cam - pcam) * pt;
        g.fillGradient(x, y, x + w, y + h, 0xFF050A1F, 0xFF1B3A6B);
        for (int i = 0; i < 60; i++) {
            int sx = x + Math.floorMod(i * 71 - (int) (c * 0.05f), w), sy = y + 14 + Math.floorMod(i * 37, Math.max(1, h / 2));
            int tw = (int) (Mth.sin((ticks + i * 13) * 0.1f) * 60 + 150);
            g.fill(sx, sy, sx + 1, sy + 1, (tw << 24) | 0xFFFFFF);
        }
        g.fill(x + w - 50, y + 22, x + w - 30, y + 42, 0xFFF1FAEE);
        g.fill(x + w - 45, y + 22, x + w - 30, y + 37, 0xFF050A1F);
        layer(g, c * 0.2f, 0xFF0D1B3A, h * 0.35f, 37);
        layer(g, c * 0.45f, 0xFF112244, h * 0.25f, 23);
        for (float[] p : plats) {
            int px0 = x + (int) (p[0] - c), px1 = px0 + (int) p[2], top = y + (int) p[1];
            if (px1 < x || px0 > x + w) continue;
            g.fill(px0, top, px1, y + h, 0xFF1E1E2E);
            g.fill(px0, top, px1, top + 3, 0xFF7FDBFF);
            for (int wy = top + 8; wy < y + h - 4; wy += 9) for (int wx = px0 + 5; wx < px1 - 5; wx += 10) if (((wx - x + (int) c) * 3 + wy) % 7 < 3) g.fill(wx, wy, wx + 4, wy + 5, 0xFFFFD166);
        }
        for (float[] s : stars) {
            if (s[2] == 1) continue;
            int sx = x + (int) (s[0] - c), sy = y + (int) s[1] + (int) (Mth.sin((ticks + s[0]) * 0.15f) * 2);
            g.fill(sx - 1, sy - 4, sx + 2, sy + 5, 0xFFFFD166);
            g.fill(sx - 4, sy - 1, sx + 5, sy + 2, 0xFFFFD166);
            g.fill(sx, sy, sx + 1, sy + 1, 0xFFFFFFFF);
        }
        for (float[] d : drones) {
            if (d[2] == 1) continue;
            int dx = x + (int) (d[0] - c), dy = y + (int) (d[1] + Mth.sin(d[3]) * 6);
            g.fill(dx - 8, dy - 3, dx + 8, dy + 3, 0xFF3D3D4D);
            g.fill(dx - 3, dy - 1, dx + 3, dy + 2, (ticks / 6) % 2 == 0 ? 0xFFFF3B30 : 0xFF880000);
            g.fill(dx - 11, dy - 5, dx - 5, dy - 4, 0xFF9AA0A6);
            g.fill(dx + 5, dy - 5, dx + 11, dy - 4, 0xFF9AA0A6);
        }
        float yy = py + (fy - py) * pt;
        fox(g, x + (int) fx, y + (int) yy);
    }

    void layer(GuiGraphics g, float off, int col, float maxH, int seed) {
        for (int i = 0; i < 30; i++) {
            int bw = 14 + (i * seed) % 26, bh = (int) (maxH * (0.3f + ((i * 53 + seed) % 70) / 100f));
            int bx = x + Math.floorMod(i * 29 - (int) off, w + 60) - 30;
            g.fill(bx, y + h - bh, bx + bw, y + h, col);
        }
    }

    void fox(GuiGraphics g, int cx, int by) {
        int run = onGround ? (ticks / 3) % 2 : 0;
        int navy = 0xFF1B2A4A, light = 0xFF9ED8FF;
        if (dashT > 0) for (int i = 1; i < 5; i++) g.fill(cx - 8 - i * 6, by - 12, cx + 6 - i * 6, by - 4, (0x40 - i * 8 << 24) | 0x7FDBFF);
        g.fill(cx - 12, by - 12 - run, cx - 6, by - 8 - run, navy);
        g.fill(cx - 14, by - 13 - run, cx - 11, by - 10 - run, 0xFFFFFFFF);
        g.fill(cx - 7, by - 12, cx + 5, by - 4, navy);
        g.fill(cx - 5, by - 10, cx - 4, by - 9, 0xFFFFFFFF);
        g.fill(cx - 1, by - 7, cx, by - 6, 0xFFFFFFFF);
        g.fill(cx + 3, by - 15, cx + 10, by - 8, navy);
        g.fill(cx + 3, by - 18, cx + 5, by - 15, navy);
        g.fill(cx + 8, by - 18, cx + 10, by - 15, navy);
        g.fill(cx + 9, by - 11, cx + 12, by - 9, light);
        g.fill(cx + 7, by - 13, cx + 8, by - 12, 0xFFFFFFFF);
        g.fill(cx - 6, by - 4, cx - 4, by - (run == 0 ? 0 : 1), navy);
        g.fill(cx + 2, by - 4, cx + 4, by - (run == 1 ? 0 : 1), navy);
    }
}
