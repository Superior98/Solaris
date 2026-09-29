package com.fireheart.city.client.pc;

import net.minecraft.client.gui.GuiGraphics;

class BlocksGame extends GameApp {
    static final int C = 10, R = 20;
    static final int[][][] SHAPES = {
            {{0, 1}, {1, 1}, {2, 1}, {3, 1}}, {{0, 0}, {1, 0}, {0, 1}, {1, 1}}, {{1, 0}, {0, 1}, {1, 1}, {2, 1}},
            {{0, 0}, {0, 1}, {1, 1}, {2, 1}}, {{2, 0}, {0, 1}, {1, 1}, {2, 1}}, {{1, 0}, {2, 0}, {0, 1}, {1, 1}}, {{0, 0}, {1, 0}, {1, 1}, {2, 1}}};
    static final int[] COLS = {0xFF48CAE4, 0xFFFFD60A, 0xFF9D4EDD, 0xFF4361EE, 0xFFFF9E00, 0xFF38B000, 0xFFE63946};
    private int[][] grid;
    private int[][] cur;
    private int kind, next, px, py, t, lines, level;

    public String title() { return "Blocks"; }

    public int color() { return 0xFF7B2CBF; }

    String id() { return "blocks"; }

    String help() { return "←→ move, ↑ rotate, ↓ drop, Space slam"; }

    void reset() {
        grid = new int[R][C];
        lines = 0;
        level = 1;
        next = rnd.nextInt(7);
        spawn();
    }

    private void spawn() {
        kind = next;
        next = rnd.nextInt(7);
        cur = new int[4][2];
        for (int i = 0; i < 4; i++) cur[i] = SHAPES[kind][i].clone();
        px = 3;
        py = 0;
        if (!fits(cur, px, py)) gameOver();
    }

    private boolean fits(int[][] s, int ox, int oy) {
        for (int[] c : s) {
            int cx = c[0] + ox, cy = c[1] + oy;
            if (cx < 0 || cx >= C || cy >= R) return false;
            if (cy >= 0 && grid[cy][cx] != 0) return false;
        }
        return true;
    }

    private void lock() {
        for (int[] c : cur) if (c[1] + py >= 0) grid[c[1] + py][c[0] + px] = kind + 1;
        int cleared = 0;
        for (int r = R - 1; r >= 0; r--) {
            boolean full = true;
            for (int c = 0; c < C; c++) if (grid[r][c] == 0) full = false;
            if (full) {
                for (int rr = r; rr > 0; rr--) grid[rr] = grid[rr - 1].clone();
                grid[0] = new int[C];
                cleared++;
                r++;
            }
        }
        int[] pts = {0, 100, 300, 500, 800};
        score += pts[cleared] * level;
        lines += cleared;
        level = 1 + lines / 8;
        spawn();
    }

    void step() {
        int speed = Math.max(2, 16 - level * 2);
        if (++t % speed != 0) return;
        if (fits(cur, px, py + 1)) py++;
        else lock();
    }

    boolean input(int k) {
        if (left(k)) { if (fits(cur, px - 1, py)) px--; return true; }
        if (right(k)) { if (fits(cur, px + 1, py)) px++; return true; }
        if (down(k)) { if (fits(cur, px, py + 1)) { py++; score++; } return true; }
        if (up(k)) {
            if (kind == 1) return true;
            int[][] r = new int[4][2];
            for (int i = 0; i < 4; i++) { r[i][0] = 2 - cur[i][1]; r[i][1] = cur[i][0]; }
            for (int kick : new int[]{0, -1, 1, -2, 2}) if (fits(r, px + kick, py)) { cur = r; px += kick; break; }
            return true;
        }
        if (k == SPACE) {
            while (fits(cur, px, py + 1)) { py++; score += 2; }
            lock();
            return true;
        }
        return false;
    }

    void draw(GuiGraphics g, int mx, int my, float pt) {
        int cell = Math.max(4, (h - 18) / R);
        int ox = x + w / 2 - cell * C / 2, oy = y + 14;
        g.fill(ox - 2, oy - 2, ox + cell * C + 2, oy + cell * R + 2, 0xFF3C096C);
        g.fill(ox, oy, ox + cell * C, oy + cell * R, 0xFF10002B);
        for (int r = 0; r < R; r++) for (int c = 0; c < C; c++) if (grid[r][c] != 0) cellAt(g, ox, oy, cell, c, r, COLS[grid[r][c] - 1]);
        if (cur != null && !over) for (int[] c : cur) if (c[1] + py >= 0) cellAt(g, ox, oy, cell, c[0] + px, c[1] + py, COLS[kind]);
        var f = os.font();
        int sx = ox + cell * C + 12;
        g.drawString(f, "Next", sx, oy, 0xFFFFFFFF, false);
        for (int[] c : SHAPES[next]) g.fill(sx + c[0] * 7, oy + 12 + c[1] * 7, sx + c[0] * 7 + 6, oy + 12 + c[1] * 7 + 6, COLS[next]);
        g.drawString(f, "Lines " + lines, sx, oy + 34, 0xFFFFFFFF, false);
        g.drawString(f, "Level " + level, sx, oy + 46, 0xFFFFFFFF, false);
    }

    private void cellAt(GuiGraphics g, int ox, int oy, int cell, int c, int r, int col) {
        g.fill(ox + c * cell, oy + r * cell, ox + c * cell + cell - 1, oy + r * cell + cell - 1, col);
        g.fill(ox + c * cell, oy + r * cell, ox + c * cell + cell - 1, oy + r * cell + 1, 0x55FFFFFF);
    }
}
