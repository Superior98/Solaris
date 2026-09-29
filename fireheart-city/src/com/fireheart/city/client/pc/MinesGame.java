package com.fireheart.city.client.pc;

import net.minecraft.client.gui.GuiGraphics;

class MinesGame extends GameApp {
    static final int C = 16, R = 10, MINES = 22;
    private boolean[][] mine, open, flag;
    private boolean placed;
    private int ticks, cx, cy;
    private boolean won;

    public String title() { return "Mines"; }

    public int color() { return 0xFFC1121F; }

    String id() { return "mines"; }

    String help() { return "Click to dig, right-click to flag (or arrows + Space/F)"; }

    void reset() {
        mine = new boolean[R][C];
        open = new boolean[R][C];
        flag = new boolean[R][C];
        placed = false;
        ticks = 0;
        won = false;
        cx = C / 2;
        cy = R / 2;
    }

    private void place(int sx, int sy) {
        int n = 0;
        while (n < MINES) {
            int x = rnd.nextInt(C), y = rnd.nextInt(R);
            if (mine[y][x] || Math.abs(x - sx) <= 1 && Math.abs(y - sy) <= 1) continue;
            mine[y][x] = true;
            n++;
        }
        placed = true;
    }

    private int around(int x, int y) {
        int n = 0;
        for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
            int nx = x + dx, ny = y + dy;
            if (nx >= 0 && ny >= 0 && nx < C && ny < R && mine[ny][nx]) n++;
        }
        return n;
    }

    private void dig(int x, int y) {
        if (x < 0 || y < 0 || x >= C || y >= R || open[y][x] || flag[y][x]) return;
        if (!placed) place(x, y);
        open[y][x] = true;
        if (mine[y][x]) { score = 0; gameOver(); return; }
        if (around(x, y) == 0) for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) if (dx != 0 || dy != 0) dig(x + dx, y + dy);
        int closed = 0;
        for (int r = 0; r < R; r++) for (int c = 0; c < C; c++) if (!open[r][c]) closed++;
        if (closed == MINES) {
            won = true;
            score = Math.max(50, 1000 - ticks / 20 * 5);
            gameOver();
        }
    }

    void step() {
        if (placed) ticks++;
    }

    boolean input(int k) {
        if (left(k)) { cx = Math.max(0, cx - 1); return true; }
        if (right(k)) { cx = Math.min(C - 1, cx + 1); return true; }
        if (up(k)) { cy = Math.max(0, cy - 1); return true; }
        if (down(k)) { cy = Math.min(R - 1, cy + 1); return true; }
        if (k == SPACE || k == ENTER) { dig(cx, cy); return true; }
        if (k == 70) { if (!open[cy][cx]) flag[cy][cx] = !flag[cy][cx]; return true; }
        return false;
    }

    private int cell() {
        return Math.min((w - 8) / C, (h - 18) / R);
    }

    boolean clickGame(double mx, double my, int b) {
        int cell = cell();
        int ox = x + (w - cell * C) / 2, oy = y + 14;
        int gx = (int) ((mx - ox) / cell), gy = (int) ((my - oy) / cell);
        if (mx < ox || my < oy || gx >= C || gy >= R) return false;
        cx = gx;
        cy = gy;
        if (b == 1) { if (!open[gy][gx]) flag[gy][gx] = !flag[gy][gx]; }
        else dig(gx, gy);
        return true;
    }

    void draw(GuiGraphics g, int mx, int my, float pt) {
        int cell = cell();
        int ox = x + (w - cell * C) / 2, oy = y + 14;
        var f = os.font();
        int[] nums = {0, 0xFF1976D2, 0xFF388E3C, 0xFFD32F2F, 0xFF7B1FA2, 0xFFFF8F00, 0xFF0097A7, 0xFF212121, 0xFF757575};
        for (int r = 0; r < R; r++) for (int c = 0; c < C; c++) {
            int x0 = ox + c * cell, y0 = oy + r * cell;
            boolean show = open[r][c] || over && mine[r][c];
            g.fill(x0, y0, x0 + cell - 1, y0 + cell - 1, show ? 0xFFD8D8D8 : 0xFF8D99AE);
            if (!show) g.fill(x0, y0, x0 + cell - 1, y0 + 2, 0x66FFFFFF);
            if (c == cx && r == cy && !over) g.fill(x0, y0, x0 + cell - 1, y0 + cell - 1, 0x55FFFF00);
            if (show && mine[r][c]) g.drawCenteredString(f, "✹", x0 + cell / 2, y0 + (cell - 8) / 2, 0xFF111111);
            else if (show) {
                int n = around(c, r);
                if (n > 0) g.drawCenteredString(f, String.valueOf(n), x0 + cell / 2, y0 + (cell - 8) / 2, nums[n]);
            } else if (flag[r][c]) g.drawCenteredString(f, "⚑", x0 + cell / 2, y0 + (cell - 8) / 2, 0xFFE63946);
        }
        if (over && won) g.drawCenteredString(f, "§aCleared in " + ticks / 20 + "s!", x + w / 2, y + h - 10, 0xFFFFFFFF);
        else if (placed && !over) g.drawString(f, "Time " + ticks / 20 + "s", x + w / 2 - 20, y + 3, 0xFFFFFFFF, false);
    }
}
