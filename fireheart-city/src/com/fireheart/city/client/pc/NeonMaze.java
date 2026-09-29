package com.fireheart.city.client.pc;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/** Neon Maze 3D: a first-person raycast maze. Collect every data orb, then reach the exit portal before the clock runs out. */
class NeonMaze extends Gen2Game {
    int n, level, orbs, total, timeLeft;
    boolean[][] wall;
    boolean[][] orb;
    int exX, exY;
    float pxp, pyp, ang, ppx, ppy, pang;

    public String title() { return "Neon Maze 3D"; }

    public int color() { return 0xFF00B4D8; }

    int accent() { return 0xFF00F5D4; }

    String id() { return "maze3d"; }

    String help() { return "↑↓ move · ←→ turn · collect orbs, find the exit"; }

    String[][] achievementList() {
        return new String[][]{{"l1", "Escape Artist (clear a maze)"}, {"l3", "Maze Runner (level 3)"}, {"l5", "Labyrinth Legend (level 5)"}, {"fast", "Speedrun (clear with 60s left)"}};
    }

    int bestLevelThisRun, fastClears;

    boolean achieved(String k) {
        return switch (k) {
            case "l1" -> bestLevelThisRun >= 2;
            case "l3" -> bestLevelThisRun >= 3;
            case "l5" -> bestLevelThisRun >= 5;
            case "fast" -> fastClears > 0;
            default -> false;
        };
    }

    void reset() {
        level = 1;
        bestLevelThisRun = 1;
        fastClears = 0;
        build();
    }

    void build() {
        n = 9 + level * 2;
        if (n % 2 == 0) n++;
        wall = new boolean[n][n];
        orb = new boolean[n][n];
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) wall[i][j] = true;
        carve(1, 1);
        for (int k = 0; k < n; k++) {
            int a = 1 + rnd.nextInt(n - 2), b = 1 + rnd.nextInt(n - 2);
            if (a % 2 != b % 2) wall[a][b] = false;
        }
        total = 0;
        for (int k = 0; k < 3 + level * 2; k++) {
            int a = 1 + rnd.nextInt(n - 2), b = 1 + rnd.nextInt(n - 2);
            if (!wall[a][b] && !(a == 1 && b == 1) && !orb[a][b]) { orb[a][b] = true; total++; }
        }
        exX = n - 2;
        exY = n - 2;
        wall[exX][exY] = false;
        orbs = 0;
        pxp = ppx = 1.5f;
        pyp = ppy = 1.5f;
        ang = pang = 0.8f;
        timeLeft = 20 * (60 + level * 25);
    }

    void carve(int cx, int cy) {
        wall[cx][cy] = false;
        int[][] d = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}};
        for (int i = 3; i > 0; i--) { int j = rnd.nextInt(i + 1); int[] t = d[i]; d[i] = d[j]; d[j] = t; }
        for (int[] v : d) {
            int nx = cx + v[0], ny = cy + v[1];
            if (nx > 0 && ny > 0 && nx < n - 1 && ny < n - 1 && wall[nx][ny]) {
                wall[cx + v[0] / 2][cy + v[1] / 2] = false;
                carve(nx, ny);
            }
        }
    }

    boolean solid(float a, float b) {
        int i = (int) a, j = (int) b;
        return i < 0 || j < 0 || i >= n || j >= n || wall[i][j];
    }

    void step() {
        ppx = pxp;
        ppy = pyp;
        pang = ang;
        float st = Gamepad.steer();
        if (st != 0) ang += st * 0.08f;
        else {
            if (held(LEFT, KA)) ang -= 0.075f;
            if (held(RIGHT, KD)) ang += 0.075f;
        }
        float mv = (held(UP, KW) ? 0.075f : 0) - (held(DOWN, KS) ? 0.05f : 0);
        float nx = pxp + Mth.cos(ang) * mv, ny = pyp + Mth.sin(ang) * mv;
        if (!solid(nx + Math.signum(nx - pxp) * 0.2f, pyp)) pxp = nx;
        if (!solid(pxp, ny + Math.signum(ny - pyp) * 0.2f)) pyp = ny;
        int ci = (int) pxp, cj = (int) pyp;
        if (orb[ci][cj]) {
            orb[ci][cj] = false;
            orbs++;
            score += 20;
            sfx(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f + orbs * 0.05f);
            burst(w / 2f, h / 2f, 20, 0xFF00F5D4, 2.5f);
        }
        if (ci == exX && cj == exY && orbs >= total) {
            score += 100 + timeLeft / 20;
            if (timeLeft > 20 * 60) fastClears++;
            level++;
            bestLevelThisRun = Math.max(bestLevelThisRun, level);
            sfx(SoundEvents.PLAYER_LEVELUP, 1.4f);
            burst(w / 2f, h / 2f, 60, 0xFFFFD166, 3.5f);
            build();
        }
        if (--timeLeft <= 0) gameOver();
    }

    boolean input(int k) {
        return true;
    }

    void draw(GuiGraphics g, int mx, int my, float pt) {
        float px = ppx + (pxp - ppx) * pt, py = ppy + (pyp - ppy) * pt, a = pang + (ang - pang) * pt;
        int top = y + 12, hh = h - 12;
        g.fillGradient(x, top, x + w, top + hh / 2, 0xFF03071E, 0xFF240046);
        g.fillGradient(x, top + hh / 2, x + w, y + h, 0xFF10002B, 0xFF3C096C);
        for (int gy = top + hh / 2 + 2; gy < y + h; gy += 4 + (gy - top - hh / 2) / 6) g.fill(x, gy, x + w, gy + 1, 0x2200F5D4);
        int cols = Math.min(w, 200);
        float fov = 1.05f;
        float[] zbuf = new float[cols];
        for (int c = 0; c < cols; c++) {
            float ra = a - fov / 2 + fov * c / cols;
            float dx = Mth.cos(ra), dy = Mth.sin(ra);
            int mx0 = (int) px, my0 = (int) py;
            float ddx = Math.abs(1 / (dx == 0 ? 1e-6f : dx)), ddy = Math.abs(1 / (dy == 0 ? 1e-6f : dy));
            int sx = dx < 0 ? -1 : 1, sy = dy < 0 ? -1 : 1;
            float sdx = dx < 0 ? (px - mx0) * ddx : (mx0 + 1 - px) * ddx, sdy = dy < 0 ? (py - my0) * ddy : (my0 + 1 - py) * ddy;
            int side = 0;
            boolean hit = false;
            for (int it = 0; it < 64 && !hit; it++) {
                if (sdx < sdy) { sdx += ddx; mx0 += sx; side = 0; } else { sdy += ddy; my0 += sy; side = 1; }
                if (mx0 < 0 || my0 < 0 || mx0 >= n || my0 >= n || wall[mx0][my0]) hit = true;
            }
            float dist = side == 0 ? sdx - ddx : sdy - ddy;
            dist *= Mth.cos(ra - a);
            zbuf[c] = dist;
            int lh = (int) (hh / Math.max(0.1f, dist));
            int y0 = top + hh / 2 - lh / 2, y1 = top + hh / 2 + lh / 2;
            float wallX = side == 0 ? py + (sdx - ddx) * dy : px + (sdy - ddy) * dx;
            wallX -= Mth.floor(wallX);
            float shade = Mth.clamp(1.2f / (1 + dist * 0.35f), 0.15f, 1f) * (side == 1 ? 0.8f : 1f);
            boolean seam = wallX < 0.04f || wallX > 0.96f;
            int base = seam ? 0x00F5D4 : ((int) (wallX * 8) % 2 == 0 ? 0x5A189A : 0x3C096C);
            int col = mul(base, shade);
            int x0 = x + c * w / cols, x1 = x + (c + 1) * w / cols;
            g.fill(x0, Math.max(top, y0), x1, Math.min(y + h, y1), 0xFF000000 | col);
            int band = y0 + lh / 5;
            if (band > top && band < y + h) g.fill(x0, band, x1, band + Math.max(1, lh / 40), 0xFF000000 | mul(0xFF2E88, shade));
        }
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
            boolean isExit = i == exX && j == exY;
            if (!orb[i][j] && !isExit) continue;
            float ox = i + 0.5f - px, oy = j + 0.5f - py;
            float d = Mth.sqrt(ox * ox + oy * oy);
            float rel = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(oy, ox) - a));
            if (Math.abs(rel) > 35 || d < 0.2f) continue;
            int sc = (int) ((rel + 30) / 60f * cols);
            if (sc < 0 || sc >= cols || zbuf[sc] < d) continue;
            int size = (int) (hh * (isExit ? 0.7f : 0.25f) / d);
            int cx = x + sc * w / cols, cy = top + hh / 2 + (isExit ? 0 : (int) (Mth.sin((ticks + i * 7) * 0.12f) * size * 0.3f));
            int col = isExit ? (orbs >= total ? 0xFF00F5D4 : 0xFF555555) : 0xFFFFD166;
            g.fill(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2, (col & 0xFFFFFF) | 0x55000000);
            g.fill(cx - size / 4, cy - size / 4, cx + size / 4, cy + size / 4, col);
        }
        int cell = Math.max(2, Math.min(4, 60 / n)), mx0 = x + w - n * cell - 4, my0 = y + 16;
        g.fill(mx0 - 1, my0 - 1, mx0 + n * cell + 1, my0 + n * cell + 1, 0x99000000);
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
            if (wall[i][j]) g.fill(mx0 + i * cell, my0 + j * cell, mx0 + i * cell + cell, my0 + j * cell + cell, 0x885A189A);
            if (orb[i][j]) g.fill(mx0 + i * cell + 1, my0 + j * cell + 1, mx0 + i * cell + cell - 1, my0 + j * cell + cell - 1, 0xFFFFD166);
        }
        g.fill(mx0 + exX * cell, my0 + exY * cell, mx0 + exX * cell + cell, my0 + exY * cell + cell, 0xFF00F5D4);
        g.fill(mx0 + (int) (px * cell) - 1, my0 + (int) (py * cell) - 1, mx0 + (int) (px * cell) + 1, my0 + (int) (py * cell) + 1, 0xFFFF2E88);
        var f = os.font();
        g.drawString(f, "§bLv " + level + "  §eOrbs " + orbs + "/" + total + "  §f" + timeLeft / 20 + "s", x + 4, y + 16, 0xFFFFFFFF, true);
        g.fill(x + w / 2 - 3, top + hh / 2, x + w / 2 + 4, top + hh / 2 + 1, 0x88FFFFFF);
        g.fill(x + w / 2, top + hh / 2 - 3, x + w / 2 + 1, top + hh / 2 + 4, 0x88FFFFFF);
    }

    static int mul(int c, float k) {
        int r = (int) (((c >> 16) & 255) * k), g = (int) (((c >> 8) & 255) * k), b = (int) ((c & 255) * k);
        return Math.min(255, r) << 16 | Math.min(255, g) << 8 | Math.min(255, b);
    }
}
