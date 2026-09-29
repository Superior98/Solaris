package com.fireheart.city.client.pc;

import com.fireheart.city.Phones;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** FireTech: buy PCs and FirePhones in the store (cash) or online (bank, delivered by post). */
class StoreApp extends App {
    private int sel, color, giftIdx;
    private boolean bank;
    private final List<int[]> hits = new ArrayList<>();

    public String title() { return os.data.kiosk ? "SolTech - Store Terminal" : "SolTech Online"; }

    public int color() { return 0xFF264653; }

    public void open(OsScreen os) {
        super.open(os);
        bank = !os.data.inStore;
        color = os.data.phoneColor;
    }

    private List<String[]> items() {
        List<String[]> l = new ArrayList<>();
        for (String s : os.data.catalog) l.add(s.split("\\|", 4));
        return l;
    }

    private List<String[]> giftable() {
        List<String[]> l = new ArrayList<>();
        for (String[] r : os.data.residents) if (r.length > 4 && r[4].equals("0")) l.add(r);
        return l;
    }

    static void drawPhone(GuiGraphics g, int x, int y, int col) {
        g.fill(x, y, x + 12, y + 20, 0xFF000000 | Phones.RGB[Math.floorMod(col, Phones.RGB.length)]);
        g.fill(x + 1, y + 2, x + 11, y + 17, 0xFF101828);
        g.fill(x + 2, y + 3, x + 10, y + 8, 0xFF3A86FF);
        g.fill(x + 2, y + 9, x + 5, y + 12, 0xFFFF6B6B);
        g.fill(x + 7, y + 9, x + 10, y + 12, 0xFF2DC653);
        g.fill(x + 4, y + 18, x + 8, y + 19, 0xFF888888);
    }

    static void drawPc(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 22, y + 15, 0xFF3A3D44);
        g.fill(x + 2, y + 2, x + 20, y + 13, 0xFF1D3557);
        g.fillGradient(x + 2, y + 2, x + 20, y + 13, 0xFF1D3557, 0xFFE76F51);
        g.fill(x + 9, y + 15, x + 13, y + 18, 0xFF3A3D44);
        g.fill(x + 4, y + 18, x + 18, y + 20, 0xFF55585F);
    }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        Font f = os.font();
        hits.clear();
        g.fill(x, y, x + w, y + h, 0xFFF7F7F9);
        List<String[]> items = items();
        if (items.isEmpty()) return;
        sel = Math.floorMod(sel, items.size());
        boolean c = compact();
        int lw = c ? w : 128;
        int rowH = c ? 15 : 30;
        for (int i = 0; i < items.size(); i++) {
            String[] it = items.get(i);
            int rx = x + 2, ry = y + 2 + i * rowH;
            boolean on = i == sel;
            g.fill(rx, ry, rx + lw - 4, ry + rowH - 2, on ? 0xFF2A9D8F : 0xFFFFFFFF);
            if (c) {
                g.drawString(f, it[1], rx + 4, ry + 3, on ? 0xFFFFFFFF : 0xFF1B1B1B, false);
                g.drawString(f, it[2], rx + lw - 8 - f.width(it[2]), ry + 3, on ? 0xFFFFE08A : 0xFFB7791F, false);
            } else {
                if (it[0].equals("pc")) drawPc(g, rx + 3, ry + (rowH - 22) / 2 + 1);
                else drawPhone(g, rx + 7, ry + (rowH - 22) / 2 + 1, color);
                g.drawString(f, it[1], rx + 30, ry + 3, on ? 0xFFFFFFFF : 0xFF1B1B1B, false);
                g.drawString(f, "§6" + it[2] + " coins", rx + 30, ry + 14, 0xFF444444, false);
            }
            hits.add(new int[]{0, i, rx, ry, rx + lw - 4, ry + rowH - 2});
        }
        int dx = c ? x + 4 : x + lw + 4, dy = c ? y + 4 + items.size() * rowH : y + 4, dw = c ? w - 8 : w - lw - 8;
        String[] it = items.get(sel);
        List<FormattedCharSequence> desc = f.split(Component.literal(it[3]), dw);
        for (FormattedCharSequence s : desc) { g.drawString(f, s, dx, dy, 0xFF333333, false); dy += 10; }
        dy += 4;
        String what = it[0];
        if (what.equals("phone") || what.equals("case") || what.equals("gift") || what.equals("headphones")) {
            g.drawString(f, "§8Colour: §0" + Phones.COLORS[color], dx, dy, 0xFF000000, false);
            dy += 11;
            int sw = Math.min(14, (dw - 2) / 8);
            for (int i = 0; i < Phones.COLORS.length; i++) {
                int sx = dx + i * sw;
                if (i == color) g.fill(sx - 1, dy - 1, sx + sw, dy + sw, 0xFF000000);
                g.fill(sx, dy, sx + sw - 2, dy + sw - 2, 0xFF000000 | Phones.RGB[i]);
                hits.add(new int[]{1, i, sx, dy, sx + sw - 1, dy + sw - 1});
            }
            dy += sw + 6;
        }
        if (what.equals("gift")) {
            List<String[]> gl = giftable();
            if (gl.isEmpty()) {
                g.drawString(f, "§8Everyone has a phone already!", dx, dy, 0xFF000000, false);
                dy += 12;
            } else {
                giftIdx = Math.floorMod(giftIdx, gl.size());
                String nm = gl.get(giftIdx)[1];
                g.drawString(f, "§8For: ", dx, dy, 0xFF000000, false);
                int bx = dx + f.width("For: ");
                g.drawString(f, "◀ §l" + nm + "§r ▶", bx, dy, 0xFF1D4ED8, false);
                hits.add(new int[]{2, -1, bx, dy - 1, bx + 10, dy + 9});
                int rx2 = bx + f.width("◀ " + nm + " ") ;
                hits.add(new int[]{2, 1, rx2, dy - 1, rx2 + 12, dy + 9});
                dy += 13;
            }
        }
        g.drawString(f, "§8Pay with:", dx, dy, 0xFF000000, false);
        dy += 11;
        int half = Math.min(60, (dw - 4) / 2);
        boolean cashOk = os.data.inStore;
        g.fill(dx, dy, dx + half, dy + 14, !bank ? 0xFF2A9D8F : cashOk ? 0xFFE9ECEF : 0xFFF1F1F1);
        g.drawCenteredString(f, cashOk ? "Gold (" + os.data.coins + ")" : "§7Cash: in store", dx + half / 2, dy + 3, !bank ? 0xFFFFFFFF : 0xFF333333);
        hits.add(new int[]{3, 0, dx, dy, dx + half, dy + 14});
        g.fill(dx + half + 4, dy, dx + half * 2 + 4, dy + 14, bank ? 0xFF2A9D8F : 0xFFE9ECEF);
        g.drawCenteredString(f, os.data.hasAccount ? "Bank (" + os.data.savings + ")" : "§7No account", dx + half + 4 + half / 2, dy + 3, bank ? 0xFFFFFFFF : 0xFF333333);
        hits.add(new int[]{3, 1, dx + half + 4, dy, dx + half * 2 + 4, dy + 14});
        dy += 20;
        int bw = half * 2 + 4;
        boolean hover = mx >= dx && mx < dx + bw && my >= dy && my < dy + 16;
        g.fill(dx, dy, dx + bw, dy + 16, hover ? 0xFFF4A261 : 0xFFE76F51);
        g.drawCenteredString(f, "§lBuy - " + it[2] + " coins", dx + bw / 2, dy + 4, 0xFFFFFFFF);
        hits.add(new int[]{4, 0, dx, dy, dx + bw, dy + 16});
        dy += 20;
        if (bank && !os.data.inStore && !what.equals("case") && !what.equals("gift")) g.drawString(f, "§8Delivered by Solaris Post.", dx, dy, 0xFF000000, false);
    }

    public boolean click(double mx, double my, int b) {
        for (int[] hit : hits) {
            if (mx < hit[2] || mx >= hit[4] || my < hit[3] || my >= hit[5]) continue;
            switch (hit[0]) {
                case 0 -> sel = hit[1];
                case 1 -> color = hit[1];
                case 2 -> giftIdx += hit[1];
                case 3 -> bank = hit[1] == 1;
                case 4 -> buy();
                default -> {}
            }
            return true;
        }
        return false;
    }

    private void buy() {
        List<String[]> items = items();
        if (items.isEmpty()) return;
        String what = items.get(sel)[0];
        String arg = what.equals("gift") ? (giftable().isEmpty() ? "" : giftable().get(Math.floorMod(giftIdx, giftable().size()))[0] + ":" + color) : String.valueOf(color);
        os.send("buy", what + ":" + arg, bank ? "bank" : "cash");
    }

    public boolean key(int k) {
        List<String[]> items = items();
        if (items.isEmpty()) return false;
        if (down(k)) { sel = (sel + 1) % items.size(); return true; }
        if (up(k)) { sel = (sel + items.size() - 1) % items.size(); return true; }
        if (left(k)) { color = (color + 7) % 8; giftIdx--; return true; }
        if (right(k)) { color = (color + 1) % 8; giftIdx++; return true; }
        if (k == ENTER || k == KP_ENTER) { buy(); return true; }
        return false;
    }
}
