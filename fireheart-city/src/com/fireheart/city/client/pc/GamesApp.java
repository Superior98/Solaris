package com.fireheart.city.client.pc;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Games folder on the SolPhone: the Classics that run on every model, and SolPlay Gen 2 titles for newer hardware. */
class GamesApp extends App {
    static final String[][] GAMES = {{"Snake", "S", "FF70E000"}, {"2048", "2", "FFEDC22E"}, {"Flap", "F", "FF48CAE4"}, {"Mines", "✹", "FFE63946"}, {"Blocks", "▦", "FF9B5DE5"}, {"Scores", "★", "FF0096C7"}};
    static final String[][] GEN2 = {{"Neon Drift", "»", "FFFF2E88"}, {"Fox Run", "✦", "FF1B3A6B"}, {"Neon Maze", "▣", "FF00B4D8"}, {"Beat Solaris", "♪", "FF7209B7"}};
    private final Btn btn = new Btn();
    private int scroll;

    public String title() { return "Games"; }

    public int color() { return 0xFF5A189A; }

    boolean gen2() {
        return !(os instanceof PhoneScreen ps) || ps.model() >= 2;
    }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        Font f = os.font();
        btn.clear();
        g.fillGradient(x, y, x + w, y + h, 0xFF3C096C, 0xFF10002B);
        g.enableScissor(x, y, x + w, y + h);
        int yy = y + 4 - scroll;
        g.drawString(f, "§d§lSOLPLAY GEN 2", x + 6, yy, 0xFFFFFFFF, false);
        yy += 11;
        boolean ok = gen2();
        int iw = (w - 14) / 2;
        for (int i = 0; i < GEN2.length; i++) {
            int ix = x + 6 + (i % 2) * (iw + 2), iy = yy + (i / 2) * 42;
            int col = (int) Long.parseLong(GEN2[i][2], 16);
            String id = GEN2[i][0];
            PhoneScreen.roundRect(g, ix, iy, ix + iw, iy + 38, 6, ok ? col : 0xFF333344);
            g.fillGradient(ix + 1, iy + 20, ix + iw - 1, iy + 37, 0x00000000, 0x88000000);
            g.pose().pushPose();
            g.pose().translate(ix + 12, iy + 8, 0);
            g.pose().scale(2, 2, 1);
            g.drawCenteredString(f, GEN2[i][1], 0, 0, ok ? 0xFFFFFFFF : 0xFF777777);
            g.pose().popPose();
            g.drawString(f, "§l" + f.plainSubstrByWidth(id, iw - 26), ix + 24, iy + 6, ok ? 0xFFFFFFFF : 0xFF999999, false);
            String pre = gid(id) + ".";
            long stars = Gen2Game.store().keySet().stream().filter(k -> k.toString().startsWith(pre)).count();
            g.drawString(f, ok ? "§7" + stars + " ★" : "§8Needs SolPhone 2", ix + 24, iy + 17, 0xFFFFFFFF, false);
            if (ok) btn.draw(g, f, ix, iy, iw, 38, "", 0x00000000, 0, mx, my, () -> os.openAppById(id));
            else btn.draw(g, f, ix, iy, iw, 38, "", 0x00000000, 0, mx, my, () -> os.showToast("Gen 2 games need a SolPhone 2 - upgrade at SolTech!"));
        }
        yy += ((GEN2.length + 1) / 2) * 42 + 4;
        g.drawString(f, "§7§lCLASSICS", x + 6, yy, 0xFFFFFFFF, false);
        yy += 11;
        for (int i = 0; i < GAMES.length; i++) {
            int ix = x + 10 + (i % 3) * 42, iy = yy + (i / 3) * 46;
            String id = GAMES[i][0];
            int col = (int) Long.parseLong(GAMES[i][2], 16);
            btn.draw(g, f, ix, iy, 32, 30, AppIcons.find(id) != null ? "" : "§l" + GAMES[i][1], AppIcons.find(id) != null ? 0x00000000 : col, 0xFFFFFFFF, mx, my, () -> os.openAppById(id));
            AppIcons.draw(g, id, ix + 1, iy, 30);
            g.drawCenteredString(f, id, ix + 16, iy + 33, 0xFFFFFFFF);
        }
        g.disableScissor();
    }

    static String gid(String label) {
        return switch (label) {
            case "Neon Drift" -> "drift";
            case "Fox Run" -> "foxrun";
            case "Neon Maze" -> "maze3d";
            default -> "beat";
        };
    }

    public boolean click(double mx, double my, int b) {
        if (mx < x || mx >= x + w || my < y || my >= y + h) return false;
        return btn.click(mx, my);
    }

    public boolean scroll(double mx, double my, double d) {
        scroll = Math.max(0, Math.min(220, scroll - (int) (d * 16)));
        return true;
    }
}
