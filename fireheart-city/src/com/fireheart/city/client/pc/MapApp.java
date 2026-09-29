package com.fireheart.city.client.pc;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** SolPhone Maps: a satellite-style map baked from the real city with live residents, you-are-here, zoom and drag-pan. */
class MapApp extends App {
    static final ResourceLocation CITY_TEX = new ResourceLocation("fireheartcity", "textures/gui/map_city.png");
    static final ResourceLocation ISLE_TEX = new ResourceLocation("fireheartcity", "textures/gui/map_isle.png");
    static final int[] CITY_B = {-176, -112, 175, 175}, ISLE_B = {-80, 208, 79, 351};
    static final int CITY_TW = 704, CITY_TH = 576, ISLE_TW = 320, ISLE_TH = 288;

    static final Object[][] CITY = {
            {"Plaza", -20, 30, 0, 0xFFFFD166}, {"Clock Tower", -26, 20, 2, 0xFFFFD166}, {"Bank", -40, 22, 1, 0xFF06D6A0}, {"Bakery", -36, 9, 2, 0xFFF4A261},
            {"Diner", 7, 53, 1, 0xFFF4A261}, {"Supply Co.", 19, 53, 2, 0xFFADB5BD}, {"Market", 31, 53, 1, 0xFF95D5B2}, {"Library", 34, -34, 1, 0xFFBDE0FE},
            {"Post Office", 16, -35, 2, 0xFFBDE0FE}, {"Founder's Statue", -2, -46, 2, 0xFFE9ECEF}, {"Old Pier", -2, 72, 2, 0xFFADB5BD}, {"Marina", 22, 62, 1, 0xFF48CAE4},
            {"SolTech", 35, -1, 1, 0xFF4CC9F0}, {"Ember Heights", 25, 25, 0, 0xFFFF7B54}, {"Sky Ferry", 15, 86, 1, 0xFFC77DFF}, {"Factory", 2, 4, 1, 0xFFADB5BD},
            {"Container Port", -25, -21, 1, 0xFFADB5BD}, {"Garage", -2, -16, 2, 0xFFADB5BD}, {"Fuel", 19, -17, 2, 0xFFFFD166}, {"Skyport", -7, 90, 0, 0xFFE040FB},
            {"Police", 56, -36, 0, 0xFF4895EF}, {"Fire Station", 79, -36, 0, 0xFFEF476F}, {"Research Lab", 54, -17, 1, 0xFF80FFDB}, {"Cinema", 82, -17, 0, 0xFFFFC300},
            {"Stellar House", 82, 14, 0, 0xFF64DFDF}, {"Firework Machine", -2, 130, 0, 0xFFFF5E78}, {"Beach", 80, 42, 1, 0xFFFFE8A3}, {"Aggregates", -30, 52, 2, 0xFFADB5BD}};
    static final Object[][] ISLE = {
            {"Plaza", -4, 261, 0, 0xFFFFD166}, {"Noodle Bar", -26, 268, 1, 0xFFF4A261}, {"Arcade", -25, 283, 1, 0xFFE040FB}, {"Sky Organ", 47, 285, 0, 0xFFC77DFF},
            {"Sky Gardens", -47, 259, 1, 0xFF95D5B2}, {"Observatory", 46, 314, 0, 0xFFBDE0FE}, {"Ferry Terminal", 20, 234, 1, 0xFFC77DFF}, {"Pods", 13, 275, 2, 0xFFADB5BD},
            {"Memorial", -24, 251, 2, 0xFFE9ECEF}, {"Skyport", -11, 236, 0, 0xFFE040FB}, {"Hall of Lights", 30, 285, 1, 0xFFFFC300}};

    private boolean isle;
    private int listScroll;
    private String picked = "";
    private final List<Object[]> rows = new ArrayList<>();
    private final List<int[]> dots = new ArrayList<>();
    private final List<String[]> ids = new ArrayList<>();
    private int listX, listW;
    private int[] area = {0, 0, 1, 1};
    private float zoom = 2f;
    private double cx, cz;
    private boolean dragging, moved;
    private int ticks;

    public String title() { return "Maps"; }

    public int color() { return 0xFF1B4965; }

    public void open(OsScreen os) {
        super.open(os);
        isle = os.data.pisle;
        recenter();
    }

    public void tick() { ticks++; }

    private int[] bounds() { return isle ? ISLE_B : CITY_B; }

    private void recenter() {
        double[] p = me();
        int[] b = bounds();
        if (p != null && os.data.pisle == isle && p[0] >= b[0] && p[0] <= b[2] && p[1] >= b[1] && p[1] <= b[3]) {
            cx = p[0];
            cz = p[1];
        } else if (isle) {
            cx = 0;
            cz = 272;
        } else {
            cx = 15;
            cz = 15;
        }
        zoom = isle ? 1.5f : 2f;
    }

    private double[] me() {
        var pl = Minecraft.getInstance().player;
        if (pl != null) return new double[]{pl.getX(), pl.getZ(), pl.getYRot()};
        return new double[]{os.data.px, os.data.pz, 0};
    }

    private double fit() {
        int[] b = bounds();
        return Math.min(area[2] / (double) (b[2] - b[0] + 1), area[3] / (double) (b[3] - b[1] + 1));
    }

    private double ppb() { return fit() * zoom; }

    private void clamp() {
        int[] b = bounds();
        double k = ppb(), hw = area[2] / k / 2, hh = area[3] / k / 2;
        double w = b[2] - b[0] + 1, h = b[3] - b[1] + 1;
        cx = hw * 2 >= w ? b[0] + w / 2 : Mth.clamp(cx, b[0] + hw, b[0] + w - hw);
        cz = hh * 2 >= h ? b[1] + h / 2 : Mth.clamp(cz, b[1] + hh, b[1] + h - hh);
    }

    private float sx(double wx) { return (float) (area[0] + area[2] / 2.0 + (wx - cx) * ppb()); }

    private float sy(double wz) { return (float) (area[1] + area[3] / 2.0 + (wz - cz) * ppb()); }

    private boolean inMap(double mx, double my) {
        return mx >= area[0] && mx < area[0] + area[2] && my >= area[1] && my < area[1] + area[3];
    }

    private boolean onScreen(float px, float py) {
        return px >= area[0] - 2 && px < area[0] + area[2] + 2 && py >= area[1] - 2 && py < area[1] + area[3] + 2;
    }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        Font f = os.font();
        dots.clear();
        ids.clear();
        boolean c = compact();
        g.fill(x, y, x + w, y + h, 0xFF0E1A2B);
        int mw = c ? w - 8 : Math.max(120, w - 128);
        int mh = c ? Math.min(mw, h * 3 / 5) : h - 20;
        int mx0 = x + 4, my0 = y + 16;
        area = new int[]{mx0, my0, mw, mh};
        clamp();
        g.drawString(f, isle ? "§lNeon Heights" : "§lSolaris City", x + 4, y + 4, 0xFFE0F2FF, false);
        String sw = isle ? "City ›" : "Sky ›";
        PhoneScreen.roundRect(g, x + w - f.width(sw) - 10, y + 2, x + w - 3, y + 13, 4, 0xFF1B4965);
        g.drawString(f, sw, x + w - f.width(sw) - 6, y + 4, 0xFFFFFFFF, false);

        g.fill(mx0 - 1, my0 - 1, mx0 + mw + 1, my0 + mh + 1, 0xFF3A86FF);
        g.fill(mx0, my0, mx0 + mw, my0 + mh, isle ? 0xFF0A1228 : 0xFF14306A);
        g.enableScissor(mx0, my0, mx0 + mw, my0 + mh);
        int[] b = bounds();
        int tw = isle ? ISLE_TW : CITY_TW, th = isle ? ISLE_TH : CITY_TH;
        float k = (float) ppb();
        g.pose().pushPose();
        g.pose().translate(sx(b[0]), sy(b[1]), 0);
        g.pose().scale(k * (b[2] - b[0] + 1) / tw, k * (b[3] - b[1] + 1) / th, 1);
        g.blit(isle ? ISLE_TEX : CITY_TEX, 0, 0, 0, 0, tw, th, tw, th);
        g.pose().popPose();
        if (isle) stars(g, mx0, my0, mw, mh);

        float lscale = Mth.clamp(0.45f + zoom * 0.12f, 0.5f, 0.85f);
        int maxRank = zoom < 1.6f ? 0 : zoom < 3f ? 1 : 2;
        for (Object[] p : isle ? ISLE : CITY) {
            int rank = (Integer) p[3];
            float px = sx((Integer) p[1]), py = sy((Integer) p[2]);
            if (!onScreen(px, py)) continue;
            int col = (Integer) p[4];
            g.fill((int) px - 2, (int) py - 2, (int) px + 2, (int) py + 2, 0xFF000000);
            g.fill((int) px - 1, (int) py - 1, (int) px + 1, (int) py + 1, col);
            if (rank > maxRank) continue;
            String name = (String) p[0];
            g.pose().pushPose();
            g.pose().translate(px + 3, py - 4 * lscale, 0);
            g.pose().scale(lscale, lscale, 1);
            int nw = f.width(name);
            g.fill(-2, -1, nw + 2, 9, 0xAA000000);
            g.fill(-2, -1, -1, 9, col);
            g.drawString(f, name, 0, 0, 0xFFFFFFFF, false);
            g.pose().popPose();
        }

        int hover = -1, i = 0;
        int face = zoom >= 3f ? 8 : 6;
        for (String e : os.data.map) {
            String[] p = e.split("\\|", 6);
            if (p.length < 6) continue;
            if (p[4].equals("1") != isle) continue;
            float px = sx(parse(p[2]) + 0.5), py = sy(parse(p[3]) + 0.5);
            boolean seen = onScreen(px, py);
            boolean on = p[0].equals(picked);
            if (seen) {
                int ix = (int) px - face / 2, iy = (int) py - face / 2;
                int ring = on ? 0xFFFFFFFF : FeedApp.nameColor(p[0]);
                if (on) {
                    float pulse = 3 + (ticks % 20) / 4f;
                    g.fill((int) (px - face / 2f - pulse), (int) (py - face / 2f - pulse), (int) (px + face / 2f + pulse), (int) (py + face / 2f + pulse), 0x33FFFFFF);
                }
                g.fill(ix - 1, iy - 1, ix + face + 1, iy + face + 1, ring);
                if (!Faces.draw(g, os, p[0], ix, iy, face)) g.fill(ix, iy, ix + face, iy + face, FeedApp.nameColor(p[0]));
                if (Math.abs(mx - px) <= face / 2 + 1 && Math.abs(my - py) <= face / 2 + 1) hover = ids.size();
            }
            dots.add(new int[]{seen ? (int) px : -9999, seen ? (int) py : -9999});
            ids.add(p);
            i++;
        }

        double[] me = me();
        if (me != null && os.data.pisle == isle) {
            float px = sx(me[0]), py = sy(me[1]);
            if (onScreen(px, py)) {
                float r = 6 + (ticks % 30) / 5f;
                int a = (int) (90 * (1 - (ticks % 30) / 30f));
                g.fill((int) (px - r), (int) (py - r), (int) (px + r), (int) (py + r), (a << 24) | 0x3A86FF);
                arrow(g, px, py, (float) me[2]);
            }
        }
        g.disableScissor();

        g.fill(mx0 + mw - 24, my0 + 2, mx0 + mw - 13, my0 + 13, 0xCC000000);
        g.fill(mx0 + mw - 12, my0 + 2, mx0 + mw - 1, my0 + 13, 0xCC000000);
        g.fill(mx0 + mw - 12, my0 + 15, mx0 + mw - 1, my0 + 26, 0xCC000000);
        g.drawCenteredString(f, "+", mx0 + mw - 18, my0 + 4, 0xFFFFFFFF);
        g.drawCenteredString(f, "-", mx0 + mw - 6, my0 + 4, 0xFFFFFFFF);
        g.drawCenteredString(f, "◎", mx0 + mw - 6, my0 + 17, 0xFF7FDBFF);
        g.fill(mx0 + 2, my0 + 2, mx0 + 13, my0 + 13, 0xCC000000);
        g.drawCenteredString(f, "N", mx0 + 8, my0 + 4, 0xFFFF5E5E);
        scaleBar(g, f, mx0 + 4, my0 + mh - 6);

        int ly = c ? my0 + mh + 4 : y + 4;
        int lx = c ? x + 4 : mx0 + mw + 6;
        int lw = c ? w - 8 : w - mw - 14;
        listX = lx;
        listW = lw;
        g.enableScissor(lx, ly, lx + lw, y + h);
        int yy = ly - listScroll;
        rows.clear();
        for (String[] p : ids) {
            boolean on = p[0].equals(picked);
            rows.add(new Object[]{p[0], yy});
            if (on) {
                g.fill(lx - 1, yy - 1, lx + lw, yy + 19, 0x443A86FF);
                PhoneScreen.roundRect(g, lx + lw - 30, yy + 2, lx + lw - 2, yy + 15, 5, 0xFF3A86FF);
                g.drawCenteredString(f, "➤ Go", lx + lw - 16, yy + 5, 0xFFFFFFFF);
            }
            int tx = lx;
            if (Faces.draw(g, os, p[0], lx + 1, yy + 2, 14)) tx = lx + 19;
            int avail = lw - (tx - lx) - (on ? 34 : 2);
            g.drawString(f, "§l" + f.plainSubstrByWidth(p[1], avail), tx, yy, FeedApp.nameColor(p[0]), false);
            g.drawString(f, f.plainSubstrByWidth(p[5], avail), tx, yy + 9, 0xFF9DB4C8, false);
            yy += 21;
        }
        if (ids.isEmpty()) g.drawString(f, "§7Nobody here right now.", lx, ly, 0xFFFFFFFF, false);
        g.disableScissor();

        if (hover >= 0) {
            String[] p = ids.get(hover);
            String tip = p[1] + " - " + p[5];
            int tw2 = Math.min(f.width(tip), w - 10);
            int tx = Math.min(mx + 6, x + w - tw2 - 6);
            g.fill(tx - 2, my - 12, tx + tw2 + 2, my - 1, 0xEE000000);
            g.drawString(f, f.plainSubstrByWidth(tip, tw2), tx, my - 10, 0xFFFFFFFF, false);
        }
    }

    private void stars(GuiGraphics g, int x0, int y0, int w0, int h0) {
        for (int s = 0; s < 40; s++) {
            int sx = x0 + Math.floorMod(s * 97 + (int) (cx * 0.3), w0), sy = y0 + Math.floorMod(s * 61 + (int) (cz * 0.3), h0);
            int a = 60 + (int) (60 * Mth.sin((ticks + s * 7) * 0.15f));
            g.fill(sx, sy, sx + 1, sy + 1, (Math.max(0, a) << 24) | 0xFFFFFF);
        }
    }

    private static void arrow(GuiGraphics g, float px, float py, float yaw) {
        float a = (float) Math.toRadians(yaw);
        float dx = -Mth.sin(a), dz = Mth.cos(a);
        g.fill((int) px - 3, (int) py - 3, (int) px + 4, (int) py + 4, 0xFFFFFFFF);
        g.fill((int) px - 2, (int) py - 2, (int) px + 3, (int) py + 3, 0xFF3A86FF);
        for (int s = 3; s <= 7; s++) {
            int ax = Math.round(px + dx * s), ay = Math.round(py + dz * s);
            int r = s < 6 ? 1 : 0;
            g.fill(ax - r, ay - r, ax + r + 1, ay + r + 1, 0xFFFFFFFF);
        }
    }

    private void scaleBar(GuiGraphics g, Font f, int x0, int y0) {
        double k = ppb();
        int[] steps = {10, 20, 50, 100, 200};
        int m = steps[steps.length - 1];
        for (int s : steps) if (s * k >= 30) { m = s; break; }
        int len = (int) (m * k);
        g.fill(x0 - 1, y0 - 9, x0 + len + 2, y0 + 3, 0x99000000);
        g.fill(x0, y0, x0 + len, y0 + 1, 0xFFFFFFFF);
        g.fill(x0, y0 - 3, x0 + 1, y0 + 1, 0xFFFFFFFF);
        g.fill(x0 + len - 1, y0 - 3, x0 + len, y0 + 1, 0xFFFFFFFF);
        g.pose().pushPose();
        g.pose().translate(x0 + 2, y0 - 8, 0);
        g.pose().scale(0.6f, 0.6f, 1);
        g.drawString(f, m + " blocks", 0, 0, 0xFFFFFFFF, false);
        g.pose().popPose();
    }

    static int parse(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    private void zoomAt(double mx, double my, float nz) {
        double wx = cx + (mx - area[0] - area[2] / 2.0) / ppb(), wz = cz + (my - area[1] - area[3] / 2.0) / ppb();
        zoom = Mth.clamp(nz, 1f, 10f);
        cx = wx - (mx - area[0] - area[2] / 2.0) / ppb();
        cz = wz - (my - area[1] - area[3] / 2.0) / ppb();
        clamp();
    }

    public boolean click(double mx, double my, int btn) {
        dragging = false;
        moved = false;
        if (my < y + 14 && mx > x + w - 40) {
            isle = !isle;
            listScroll = 0;
            recenter();
            return true;
        }
        int ax = area[0] + area[2], ay = area[1];
        if (my >= ay + 2 && my < ay + 13 && mx >= ax - 24 && mx < ax - 1) {
            zoomAt(area[0] + area[2] / 2.0, area[1] + area[3] / 2.0, mx < ax - 12 ? zoom * 1.5f : zoom / 1.5f);
            return true;
        }
        if (my >= ay + 15 && my < ay + 26 && mx >= ax - 12 && mx < ax - 1) {
            isle = os.data.pisle;
            recenter();
            return true;
        }
        for (int d = 0; d < dots.size(); d++) {
            int[] p = dots.get(d);
            if (Math.abs(mx - p[0]) <= 5 && Math.abs(my - p[1]) <= 5) {
                String id = ids.get(d)[0];
                picked = picked.equals(id) ? "" : id;
                return true;
            }
        }
        if (mx >= listX && mx < listX + listW && !inMap(mx, my)) {
            for (Object[] r : rows) {
                int ry = (Integer) r[1];
                if (my < ry - 1 || my >= ry + 20) continue;
                String id = (String) r[0];
                if (id.equals(picked) && mx >= listX + listW - 30) {
                    os.send("directions", id, "");
                    os.showToast("Follow the sparkles to " + os.residentName(id) + "!");
                } else {
                    picked = id;
                    focus(id);
                }
                return true;
            }
        }
        if (inMap(mx, my)) {
            dragging = true;
            return true;
        }
        return false;
    }

    private void focus(String id) {
        for (String e : os.data.map) {
            String[] p = e.split("\\|", 6);
            if (p.length < 6 || !p[0].equals(id) || p[4].equals("1") != isle) continue;
            cx = parse(p[2]);
            cz = parse(p[3]);
            if (zoom < 3f) zoom = 3f;
            clamp();
        }
    }

    public boolean drag(double mx, double my, int btn, double dx, double dy) {
        if (!dragging) return false;
        moved = true;
        cx -= dx / ppb();
        cz -= dy / ppb();
        clamp();
        return true;
    }

    public boolean scroll(double mx, double my, double d) {
        if (inMap(mx, my)) {
            zoomAt(mx, my, d > 0 ? zoom * 1.25f : zoom / 1.25f);
            return true;
        }
        listScroll = Math.max(0, Math.min(ids.size() * 21, listScroll - (int) (d * 15)));
        return true;
    }

    public boolean key(int k) {
        if (k == SPACE || k == ENTER) { isle = !isle; recenter(); return true; }
        double step = 24 / ppb();
        if (k == LEFT) { cx -= step; clamp(); return true; }
        if (k == RIGHT) { cx += step; clamp(); return true; }
        if (k == UP) { cz -= step; clamp(); return true; }
        if (k == DOWN) { cz += step; clamp(); return true; }
        if (k == KW) { listScroll = Math.max(0, listScroll - 15); return true; }
        if (k == KS) { listScroll += 15; return true; }
        return false;
    }
}
