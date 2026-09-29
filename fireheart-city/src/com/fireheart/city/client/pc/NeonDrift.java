package com.fireheart.city.client.pc;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/** Neon Drift: a pseudo-3D night racer through Solaris - curves, hills, traffic, nitro and checkpoint time. */
class NeonDrift extends Gen2Game {
    static final int SEG = 200, DRAW = 90, LANES = 3;
    static final float ROAD = 2000, CAM_H = 1000, DEPTH = 0.84f;
    final List<float[]> segs = new ArrayList<>();
    final List<float[]> cars = new ArrayList<>();
    float pos, speed, px, nitro, timeLeft, prevPos;
    int checkpoints, overtakes, crashes;
    boolean usedNitro;

    public String title() { return "Neon Drift"; }

    public int color() { return 0xFFFF2E88; }

    int accent() { return 0xFFFF2E88; }

    String id() { return "drift"; }

    String help() { return "↑ gas · ↓ brake · ←→ steer · Space nitro"; }

    String[][] achievementList() {
        return new String[][]{{"first", "First Lap Around Solaris"}, {"speed", "Light Speed (300 km/h)"}, {"cp5", "Checkpoint Champion (5)"}, {"overtake20", "Traffic Weaver (20 overtakes)"}, {"clean", "Clean Driver (2 min no crash)"}};
    }

    boolean achieved(String k) {
        return switch (k) {
            case "first" -> checkpoints >= 1;
            case "speed" -> kmh() >= 300;
            case "cp5" -> checkpoints >= 5;
            case "overtake20" -> overtakes >= 20;
            case "clean" -> ticks > 2400 && crashes == 0;
            default -> false;
        };
    }

    int kmh() {
        return (int) (speed / 12000 * 320);
    }

    void reset() {
        segs.clear();
        cars.clear();
        float curve = 0, hill = 0, y = 0;
        for (int i = 0; i < 3000; i++) {
            if (i % 120 == 0) curve = (rnd.nextFloat() - 0.5f) * (i < 200 ? 0 : 7);
            if (i % 90 == 0) hill = (rnd.nextFloat() - 0.5f) * (i < 200 ? 0 : 60);
            y += hill * Mth.sin(i % 90 / 90f * Mth.PI);
            segs.add(new float[]{curve * Mth.sin(i % 120 / 120f * Mth.PI), y, i % 400 == 0 && i > 0 ? 1 : 0});
        }
        for (int i = 0; i < 60; i++) cars.add(new float[]{(300 + i * 50 + rnd.nextInt(30)) * SEG, (rnd.nextInt(LANES) - 1) * 0.66f, 3000 + rnd.nextInt(3000), rnd.nextInt(5), 0});
        pos = 0;
        prevPos = 0;
        speed = 0;
        px = 0;
        nitro = 1;
        timeLeft = 20 * 35;
        checkpoints = overtakes = crashes = 0;
        score = 0;
    }

    void step() {
        prevPos = pos;
        boolean gas = held(UP, KW), brake = held(DOWN, KS), left = held(LEFT, KA), right = held(RIGHT, KD), boost = held(SPACE) && nitro > 0;
        float max = boost ? 15500 : 12000;
        if (gas || boost) speed += (boost ? 260 : 150);
        else if (brake) speed -= 380;
        else speed -= 60;
        float st = Gamepad.steer();
        float steer = st != 0 ? st : (left ? -1 : 0) + (right ? 1 : 0);
        px += steer * 0.045f * (speed / 12000f + 0.2f);
        int si = (int) (pos / SEG) % segs.size();
        float curve = segs.get(si)[0];
        px -= curve * 0.0018f * (speed / 12000f);
        if (Math.abs(px) > 1.05f) {
            speed -= 280;
            if (ticks % 3 == 0) burst(w / 2f + (px > 0 ? 30 : -30), h - 30, 2, 0xFF8B6B4A, 1.5f);
        }
        px = Mth.clamp(px, -2f, 2f);
        speed = Mth.clamp(speed, 0, max);
        if (boost) {
            nitro = Math.max(0, nitro - 0.01f);
            usedNitro = true;
            if (ticks % 2 == 0) burst(w / 2f + (rnd.nextFloat() - 0.5f) * 20, h - 20, 1, 0xFF4CC9F0, 2);
        } else nitro = Math.min(1, nitro + 0.0015f);
        pos += speed / 20f;
        timeLeft--;
        score = (int) (pos / 1000) + checkpoints * 50 + overtakes * 5;
        int seg = (int) (pos / SEG);
        if (segs.get(seg % segs.size())[2] == 1 && (int) (prevPos / SEG) != seg) {
            checkpoints++;
            timeLeft += 20 * 25;
            sfx(SoundEvents.NOTE_BLOCK_PLING.value(), 1.6f);
            burst(w / 2f, h / 2f, 40, 0xFFFFD166, 3);
        }
        for (float[] c : cars) {
            float before = c[0] - pos;
            c[0] += c[2] / 20f;
            float rel = c[0] - pos;
            if (c[4] == 0 && before > 0 && rel <= 0) { overtakes++; c[4] = 1; }
            if (rel > -SEG && rel < SEG * 0.6f && Math.abs(c[1] - px) < 0.35f) {
                speed *= 0.35f;
                c[0] = pos + SEG * 1.5f;
                crashes++;
                shake(6, 12);
                burst(w / 2f, h - 40, 30, 0xFFFF6B35, 3);
                sfx(SoundEvents.GENERIC_EXPLODE, 1.5f);
            }
            if (rel < -SEG * 20) { c[0] = pos + SEG * (DRAW + rnd.nextInt(60)); c[1] = (rnd.nextInt(LANES) - 1) * 0.66f; c[4] = 0; }
        }
        if (ticks % 6 == 0 && speed > 500) sfx(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 0.5f + speed / 12000f);
        if (timeLeft <= 0) {
            gameOver();
            shake(3, 10);
        }
    }

    boolean input(int k) {
        return true;
    }

    void draw(GuiGraphics g, int mx, int my, float pt) {
        float p = prevPos + (pos - prevPos) * pt;
        int horizon = y + h / 2 - 8;
        g.fillGradient(x, y, x + w, horizon, 0xFF0B0221, 0xFF3A0CA3);
        for (int i = 0; i < 40; i++) {
            int sx = x + Math.floorMod(i * 97 - (int) (px * 30), w), sy = y + 14 + Math.floorMod(i * 53, Math.max(1, horizon - y - 30));
            g.fill(sx, sy, sx + 1, sy + 1, 0x88FFFFFF);
        }
        int sunR = Math.min(w, h) / 6;
        for (int r = sunR; r > 0; r -= 2) {
            int yy = horizon - sunR + (sunR - r);
            if ((yy / 3) % 3 == 0 && r < sunR * 0.8) continue;
            g.fill(x + w / 2 - r, yy, x + w / 2 + r, yy + 2, 0xFFFF2E88 + ((sunR - r) * 2 << 8));
        }
        for (int i = 0; i < 16; i++) {
            int bw = 8 + (i * 37) % 22, bh = 12 + (i * 53) % 40;
            int bx = x + Math.floorMod(i * 41 - (int) (px * 50) - (int) (p / 4000), w + 40) - 20;
            g.fill(bx, horizon - bh, bx + bw, horizon, 0xFF140A33);
            for (int wy = horizon - bh + 3; wy < horizon - 2; wy += 5) for (int wx = bx + 2; wx < bx + bw - 2; wx += 4) if ((wx * 7 + wy * 3) % 5 == 0) g.fill(wx, wy, wx + 1, wy + 2, 0xFFFFD166);
        }
        g.fill(x, horizon, x + w, y + h, 0xFF10002B);
        int base = (int) (p / SEG);
        float camX = px * ROAD, dx = 0, xAcc = 0;
        float camY = CAM_H + segs.get(base % segs.size())[1];
        int maxY = y + h;
        float[][] proj = new float[DRAW + 1][];
        for (int n = 0; n <= DRAW; n++) {
            int idx = (base + n) % segs.size();
            float[] s = segs.get(idx);
            float z = (base + n) * SEG - p + SEG;
            if (z <= 1) z = 1;
            float scale = DEPTH / (z / 1000f);
            float sx = x + w / 2f + scale * (xAcc - camX) * w / 2f / 1000f;
            float sy = horizon + scale * (camY - s[1]) * h / 2f / 1000f * 0.5f;
            float sw = scale * ROAD * w / 2f / 1000f;
            proj[n] = new float[]{sx, sy, sw, idx};
            xAcc += dx;
            dx += s[0] * 60;
        }
        for (int n = DRAW; n >= 1; n--) {
            float[] a = proj[n - 1], b = proj[n];
            if (b[1] >= maxY || a[1] <= b[1]) continue;
            int idx = (int) b[3];
            boolean alt = (idx / 3) % 2 == 0;
            int y0 = (int) b[1], y1 = (int) Math.min(a[1], y + h);
            for (int yy = y0; yy < y1; yy++) {
                float k = (yy - b[1]) / Math.max(1, a[1] - b[1]);
                float cx = b[0] + (a[0] - b[0]) * k, hw = b[2] + (a[2] - b[2]) * k;
                g.fill(x, yy, x + w, yy + 1, alt ? 0xFF1B0B3B : 0xFF160833);
                int rum = (int) (hw * 0.12f);
                g.fill((int) (cx - hw - rum), yy, (int) (cx - hw), yy + 1, alt ? 0xFFFF2E88 : 0xFFF8F8F8);
                g.fill((int) (cx + hw), yy, (int) (cx + hw + rum), yy + 1, alt ? 0xFFFF2E88 : 0xFFF8F8F8);
                g.fill((int) (cx - hw), yy, (int) (cx + hw), yy + 1, alt ? 0xFF2A2A3A : 0xFF25253A);
                if (alt) for (int l = 1; l < LANES; l++) {
                    int lx = (int) (cx - hw + hw * 2 * l / LANES);
                    g.fill(lx - 1, yy, lx + 1, yy + 1, 0xAA4CC9F0);
                }
                if (segs.get(idx)[2] == 1) g.fill((int) (cx - hw), yy, (int) (cx + hw), yy + 1, (yy % 2 == 0) ? 0xFFFFFFFF : 0xFF000000);
            }
            maxY = Math.min(maxY, y0);
        }
        for (int i = cars.size() - 1; i >= 0; i--) {
            float[] c = cars.get(i);
            float rel = c[0] - p;
            int n = (int) (rel / SEG);
            if (n < 1 || n >= DRAW) continue;
            float[] pr = proj[n];
            float scale = pr[2] / (ROAD * w / 2f / 1000f);
            int cw = (int) (pr[2] * 0.42f), ch = (int) (cw * 0.55f);
            int cx = (int) (pr[0] + c[1] * pr[2]);
            int cy = (int) pr[1];
            if (cw < 2) continue;
            int[] cols = {0xFF06D6A0, 0xFFFFD166, 0xFF118AB2, 0xFFEF476F, 0xFFF8F8F8};
            car(g, cx, cy, cw, ch, cols[(int) c[3]], false);
        }
        int pcw = Math.min(64, w / 5), pch = pcw * 11 / 20;
        int pcx = x + w / 2 + (int) (Gamepad.steer() * 3), pcy = y + h - 10;
        car(g, pcx, pcy, pcw, pch, 0xFFFF2E88, true);
        var f = os.font();
        g.fill(x + 4, y + h - 26, x + 70, y + h - 4, 0x88000000);
        g.drawString(f, "§f§l" + kmh() + " §7km/h", x + 8, y + h - 23, 0xFFFFFFFF, false);
        g.fill(x + 8, y + h - 12, x + 66, y + h - 8, 0xFF222222);
        g.fill(x + 8, y + h - 12, x + 8 + (int) (58 * nitro), y + h - 8, 0xFF4CC9F0);
        String tl = "§e" + Math.max(0, (int) (timeLeft / 20)) + "s";
        g.drawString(f, tl, x + w / 2 - 6, y + 16, 0xFFFFFFFF, true);
        g.drawString(f, "§7CP §f" + checkpoints, x + w - 40, y + 16, 0xFFFFFFFF, true);
    }

    void car(GuiGraphics g, int cx, int by, int cw, int ch, int col, boolean player) {
        int x0 = cx - cw / 2, x1 = cx + cw / 2, y0 = by - ch;
        g.fill(x0 + cw / 10, by - 2, x1 - cw / 10, by, 0x66000000);
        g.fill(x0, y0 + ch / 3, x1, by - ch / 6, col);
        g.fill(x0 + cw / 6, y0, x1 - cw / 6, y0 + ch / 3 + 1, darker(col));
        g.fill(x0 + cw / 5, y0 + ch / 12, x1 - cw / 5, y0 + ch / 3, 0xFF0B1A2A);
        g.fill(x0 + 1, by - ch / 6, x0 + cw / 5, by, 0xFF111111);
        g.fill(x1 - cw / 5, by - ch / 6, x1 - 1, by, 0xFF111111);
        int lh = Math.max(1, ch / 8);
        g.fill(x0 + 1, y0 + ch / 3 + 2, x0 + cw / 5, y0 + ch / 3 + 2 + lh, player && held(DOWN, KS) ? 0xFFFF0000 : 0xFFFF4D6D);
        g.fill(x1 - cw / 5, y0 + ch / 3 + 2, x1 - 1, y0 + ch / 3 + 2 + lh, player && held(DOWN, KS) ? 0xFFFF0000 : 0xFFFF4D6D);
        if (player) g.fill(x0 + 2, by - ch / 6 - 1, x1 - 2, by - ch / 6, 0xFF4CC9F0);
    }

    static int darker(int c) {
        int r = ((c >> 16) & 255) * 3 / 4, gg = ((c >> 8) & 255) * 3 / 4, b = (c & 255) * 3 / 4;
        return 0xFF000000 | r << 16 | gg << 8 | b;
    }
}
