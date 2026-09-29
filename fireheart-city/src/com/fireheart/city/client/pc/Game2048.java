package com.fireheart.city.client.pc;

import net.minecraft.client.gui.GuiGraphics;

class Game2048 extends GameApp {
    private int[][] b = new int[4][4];

    public String title() { return "2048"; }

    public int color() { return 0xFFBB8B00; }

    String id() { return "2048"; }

    void reset() {
        b = new int[4][4];
        add();
        add();
    }

    private void add() {
        int free = 0;
        for (int[] r : b) for (int v : r) if (v == 0) free++;
        if (free == 0) return;
        int k = rnd.nextInt(free);
        for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) if (b[y][x] == 0 && k-- == 0) b[y][x] = rnd.nextInt(10) == 0 ? 4 : 2;
    }

    void step() {}

    private boolean slide(int dx, int dy) {
        boolean moved = false;
        for (int i = 0; i < 4; i++) {
            int[] line = new int[4];
            for (int j = 0; j < 4; j++) line[j] = get(i, j, dx, dy);
            int[] out = new int[4];
            int n = 0;
            for (int j = 0; j < 4; j++) {
                if (line[j] == 0) continue;
                if (n > 0 && out[n - 1] == line[j] && out[n - 1] > 0 && !merged[n - 1]) { out[n - 1] *= 2; score += out[n - 1]; merged[n - 1] = true; }
                else { out[n++] = line[j]; }
            }
            java.util.Arrays.fill(merged, false);
            for (int j = 0; j < 4; j++) {
                if (get(i, j, dx, dy) != out[j]) moved = true;
                set(i, j, dx, dy, out[j]);
            }
        }
        return moved;
    }

    private final boolean[] merged = new boolean[4];

    private int get(int i, int j, int dx, int dy) {
        if (dx == -1) return b[i][j];
        if (dx == 1) return b[i][3 - j];
        if (dy == -1) return b[j][i];
        return b[3 - j][i];
    }

    private void set(int i, int j, int dx, int dy, int v) {
        if (dx == -1) b[i][j] = v;
        else if (dx == 1) b[i][3 - j] = v;
        else if (dy == -1) b[j][i] = v;
        else b[3 - j][i] = v;
    }

    private boolean canMove() {
        for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) {
            if (b[y][x] == 0) return true;
            if (x < 3 && b[y][x] == b[y][x + 1]) return true;
            if (y < 3 && b[y][x] == b[y + 1][x]) return true;
        }
        return false;
    }

    boolean input(int k) {
        boolean m;
        if (left(k)) m = slide(-1, 0);
        else if (right(k)) m = slide(1, 0);
        else if (up(k)) m = slide(0, -1);
        else if (down(k)) m = slide(0, 1);
        else return false;
        if (m) add();
        if (!canMove()) gameOver();
        return true;
    }

    void draw(GuiGraphics g, int mx, int my, float pt) {
        int t = Math.min((h - 22) / 4, 38);
        int ox = x + w / 2 - t * 2, oy = y + 16;
        g.fill(ox - 4, oy - 4, ox + t * 4 + 4, oy + t * 4 + 4, 0xFFBBADA0);
        var f = os.font();
        for (int y0 = 0; y0 < 4; y0++) for (int x0 = 0; x0 < 4; x0++) {
            int v = b[y0][x0];
            int col = switch (v) {
                case 0 -> 0xFFCDC1B4; case 2 -> 0xFFEEE4DA; case 4 -> 0xFFEDE0C8; case 8 -> 0xFFF2B179; case 16 -> 0xFFF59563;
                case 32 -> 0xFFF67C5F; case 64 -> 0xFFF65E3B; case 128 -> 0xFFEDCF72; case 256 -> 0xFFEDCC61; case 512 -> 0xFFEDC850;
                default -> 0xFFEDC22E;
            };
            int px = ox + x0 * t, py = oy + y0 * t;
            g.fill(px + 2, py + 2, px + t - 2, py + t - 2, col);
            if (v > 0) g.drawCenteredString(f, "§l" + v, px + t / 2, py + t / 2 - 4, v <= 4 ? 0xFF776E65 : 0xFFFFFFFF);
        }
    }
}
