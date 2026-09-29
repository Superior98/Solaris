package com.fireheart.city.client.pc;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.gui.GuiGraphics;

class SnakeGame extends GameApp {
    static final int C = 26, R = 15;
    private final Deque<int[]> body = new ArrayDeque<>();
    private int dx = 1, dy, ndx = 1, ndy, fx, fy, t;

    public String title() { return "Snake"; }

    public int color() { return 0xFF38B000; }

    String id() { return "snake"; }

    void reset() {
        body.clear();
        for (int i = 0; i < 3; i++) body.addFirst(new int[]{5 + i, R / 2});
        dx = 1; dy = 0; ndx = 1; ndy = 0; t = 0;
        food();
    }

    private void food() {
        while (true) {
            fx = rnd.nextInt(C);
            fy = rnd.nextInt(R);
            boolean hit = false;
            for (int[] b : body) if (b[0] == fx && b[1] == fy) hit = true;
            if (!hit) return;
        }
    }

    void step() {
        int speed = Math.max(1, 4 - score / 6);
        if (++t % speed != 0) return;
        dx = ndx; dy = ndy;
        int[] head = body.peekFirst();
        int nx = head[0] + dx, ny = head[1] + dy;
        if (nx < 0 || ny < 0 || nx >= C || ny >= R) { gameOver(); return; }
        for (int[] b : body) if (b[0] == nx && b[1] == ny) { gameOver(); return; }
        body.addFirst(new int[]{nx, ny});
        if (nx == fx && ny == fy) { score++; food(); } else body.removeLast();
    }

    boolean input(int k) {
        if (left(k) && dx != 1) { ndx = -1; ndy = 0; return true; }
        if (right(k) && dx != -1) { ndx = 1; ndy = 0; return true; }
        if (up(k) && dy != 1) { ndx = 0; ndy = -1; return true; }
        if (down(k) && dy != -1) { ndx = 0; ndy = 1; return true; }
        return false;
    }

    void draw(GuiGraphics g, int mx, int my, float pt) {
        int cell = Math.min((w - 8) / C, (h - 16) / R);
        int ox = x + (w - cell * C) / 2, oy = y + 13 + (h - 13 - cell * R) / 2;
        g.fill(ox - 1, oy - 1, ox + cell * C + 1, oy + cell * R + 1, 0xFF2D6A4F);
        g.fill(ox, oy, ox + cell * C, oy + cell * R, 0xFF081C15);
        g.fill(ox + fx * cell + 1, oy + fy * cell + 1, ox + fx * cell + cell - 1, oy + fy * cell + cell - 1, 0xFFE63946);
        int i = 0;
        for (int[] b : body) {
            int col = i++ == 0 ? 0xFFB7E4C7 : 0xFF52B788;
            g.fill(ox + b[0] * cell, oy + b[1] * cell, ox + b[0] * cell + cell - 1, oy + b[1] * cell + cell - 1, col);
        }
    }
}
