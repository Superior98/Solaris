package com.fireheart.city.client.pc;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Tappable rounded buttons drawn by phone apps; register during render, hit-test on click. */
final class Btn {
    record Hit(int x0, int y0, int x1, int y1, Runnable run) {}

    private final List<Hit> hits = new ArrayList<>();

    void clear() {
        hits.clear();
    }

    int draw(GuiGraphics g, Font f, int x, int y, int w, int h, String label, int bg, int fg, int mx, int my, Runnable run) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
        PhoneScreen.roundRect(g, x, y, x + w, y + h, Math.min(6, h / 2), hover ? PhoneScreen.lighten(bg, 25) : bg);
        g.drawCenteredString(f, label, x + w / 2, y + (h - 8) / 2, fg);
        hits.add(new Hit(x, y, x + w, y + h, run));
        return x + w;
    }

    boolean click(double mx, double my) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (mx >= h.x0() && mx < h.x1() && my >= h.y0() && my < h.y1()) {
                h.run().run();
                return true;
            }
        }
        return false;
    }
}
