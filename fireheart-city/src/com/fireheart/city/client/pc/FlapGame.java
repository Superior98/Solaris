package com.fireheart.city.client.pc;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

class FlapGame extends GameApp {
    private double fy, vy, py;
    private final List<double[]> isles = new ArrayList<>();
    private int t;

    public String title() { return "Ferry Flap"; }

    public int color() { return 0xFF0077B6; }

    String id() { return "flap"; }

    String help() { return "Space / ↑ / click to lift the Sky Ferry"; }

    void reset() {
        fy = h / 2.0;
        py = fy;
        vy = 0;
        isles.clear();
        t = 0;
    }

    void step() {
        py = fy;
        vy = Math.min(4.5, vy + 0.38);
        fy += vy;
        t++;
        if (t % 48 == 1) isles.add(new double[]{w + 10, 30 + rnd.nextInt(Math.max(20, h - 110)), 0});
        for (double[] is : isles) {
            is[0] -= 2.4;
            if (is[2] == 0 && is[0] + 22 < 40) { is[2] = 1; score++; }
        }
        isles.removeIf(is -> is[0] < -40);
        if (fy < 14 || fy > h - 8) gameOver();
        for (double[] is : isles) {
            double gapTop = is[1], gapBot = is[1] + 58;
            if (40 + 10 > is[0] && 40 - 10 < is[0] + 22 && (fy - 5 < gapTop || fy + 5 > gapBot)) gameOver();
        }
    }

    private void flap() {
        vy = -4.6;
    }

    boolean input(int k) {
        if (k == SPACE || up(k)) { flap(); return true; }
        return false;
    }

    boolean clickGame(double mx, double my, int b) {
        flap();
        return true;
    }

    public boolean key(int k) {
        if (started && !over && k == SPACE) { flap(); return true; }
        return super.key(k);
    }

    void draw(GuiGraphics g, int mx, int my, float pt) {
        g.fillGradient(x, y, x + w, y + h, 0xFF48CAE4, 0xFFFFB4A2);
        g.fill(x, y + h - 6, x + w, y + h, 0xFF0077B6);
        for (double[] is : isles) {
            int ix = x + (int) is[0];
            int top = y + (int) is[1], bot = y + (int) is[1] + 58;
            g.fill(ix, y + 12, ix + 22, top, 0xFF6C584C);
            g.fill(ix - 3, top - 6, ix + 25, top, 0xFF52B788);
            g.fill(ix, bot, ix + 22, y + h - 6, 0xFF6C584C);
            g.fill(ix - 3, bot, ix + 25, bot + 5, 0xFF52B788);
        }
        double yy = started && !over ? py + (fy - py) * pt : fy;
        int fx = x + 40, fyy = y + (int) yy;
        g.fill(fx - 9, fyy - 10, fx + 9, fyy - 2, 0xFFFF7F11);
        g.fill(fx - 7, fyy - 11, fx + 7, fyy - 1, 0xFFFFFFFF);
        g.fill(fx - 9, fyy - 7, fx + 9, fyy - 5, 0xFFFFD60A);
        g.fill(fx - 1, fyy - 2, fx + 1, fyy + 1, 0xFF6C584C);
        g.fill(fx - 7, fyy + 1, fx + 7, fyy + 5, 0xFF8B5E34);
    }
}
