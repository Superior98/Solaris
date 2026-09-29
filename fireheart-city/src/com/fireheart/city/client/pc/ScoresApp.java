package com.fireheart.city.client.pc;

import com.fireheart.city.Computers;
import net.minecraft.client.gui.GuiGraphics;

class ScoresApp extends App {
    public String title() { return "High Scores"; }

    public int color() { return 0xFF0096C7; }

    private int scroll;

    public boolean scroll(double mx, double my, double d) {
        scroll = Math.max(0, Math.min(Computers.GAMES.length * 80 - h + 10, scroll - (int) (d * 20)));
        return true;
    }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        var f = os.font();
        int cols = compact() ? 1 : 3;
        int colW = (w - 10) / cols;
        g.enableScissor(x, y, x + w, y + h);
        for (int gi = 0; gi < Computers.GAMES.length; gi++) {
            int cx = x + 6 + (gi % cols) * colW, cy = y + 6 + (gi / cols) * 80 - (compact() ? scroll : 0);
            g.fill(cx, cy, cx + colW - 6, cy + 74, 0xFFE3E7EE);
            g.drawString(f, "§l" + Computers.GAME_NAMES[gi], cx + 4, cy + 3, 0xFF0B3C5D, false);
            int row = 0;
            for (String s : os.data.scores) {
                String[] p = s.split("\\|");
                if (p.length != 3 || !p[0].equals(Computers.GAMES[gi])) continue;
                boolean me = p[1].equals(os.data.player);
                g.drawString(f, (row + 1) + ". " + (me ? "§9" : "") + p[1], cx + 4, cy + 15 + row * 10, 0xFF333333, false);
                g.drawString(f, p[2], cx + colW - 10 - f.width(p[2]), cy + 15 + row * 10, 0xFF333333, false);
                row++;
            }
            if (row == 0) g.drawString(f, "§8No scores yet", cx + 4, cy + 15, 0xFF333333, false);
        }
        g.disableScissor();
    }
}
