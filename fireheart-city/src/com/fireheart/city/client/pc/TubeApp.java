package com.fireheart.city.client.pc;

import com.fireheart.city.Computers;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** FireTube: short animated videos made in Fireheart City. */
class TubeApp extends App {
    static final String[] CHANNELS = {"SlimeFails", "Jet's Flight Deck", "NeonNights", "Leo Cooks", "Pier Cam", "Zara's Clockwork", "Solaris Official", "Nova's Sky Tours", "Sam's Ocean Channel"};
    static final String[] VIEWS = {"12K views", "4.1K views", "8.8K views", "20K views", "6.3K views", "2.2K views", "31K views", "15K views", "9.4K views"};
    static final int DUR = 400;
    private int sel = -1, t, scroll;
    private boolean playing, liked;

    public String title() { return sel < 0 ? "SolTube" : titleOf(sel); }

    int count() {
        return Computers.VIDEOS.length + os.data.uploads.size();
    }

    String[] upl(int i) {
        return os.data.uploads.get(i - Computers.VIDEOS.length).split("\\|");
    }

    String titleOf(int i) {
        return i < Computers.VIDEOS.length ? Computers.VIDEOS[i] : upl(i)[1];
    }

    String channelOf(int i) {
        return i < Computers.VIDEOS.length ? CHANNELS[i] : upl(i)[2];
    }

    String viewsOf(int i) {
        return i < Computers.VIDEOS.length ? VIEWS[i] : "New upload";
    }

    String keyOf(int i) {
        return i < Computers.VIDEOS.length ? null : upl(i)[0];
    }

    void drawAny(GuiGraphics g, Font f, int i, int x, int y, int w, int h, int t) {
        if (i < Computers.VIDEOS.length) video(g, f, i, x, y, w, h, t);
        else PhotoCache.drawVideo(g, keyOf(i), t, x, y, w, h);
    }

    public int color() { return 0xFFD62828; }

    public boolean back() {
        if (sel < 0) return false;
        sel = -1;
        return true;
    }

    private int sentAt = -1000, sentSel = -2;

    public void tick() {
        if (sel >= 0 && playing && ++t >= DUR) playing = false;
        if (os.data.device == 4 && os.data.tv != 0) {
            int show = sel >= 0 && playing ? sel : -1;
            if (show != sentSel || show >= 0 && Math.abs(t - sentAt) > 60) {
                boolean changed = show != sentSel;
                sentSel = show;
                sentAt = t;
                os.send("tvshow", show >= 0 ? (show < Computers.VIDEOS.length ? "tube;" + show : "vid;" + keyOf(show)) : "", os.data.tv + ";" + (changed || show >= 0 ? t : -1));
            }
        }
    }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        Font f = os.font();
        g.fill(x, y, x + w, y + h, 0xFF0F0F0F);
        if (sel < 0) {
            int tw = os.phone() ? w - 8 : (w - 12) / 2;
            int th = tw * 9 / 16;
            int per = os.phone() ? 1 : 2;
            g.enableScissor(x, y, x + w, y + h);
            for (int i = 0; i < count(); i++) {
                int cx = x + 4 + (i % per) * (tw + 4), cy = y + 4 + (i / per) * (th + 26) - scroll;
                if (cy + th <= y || cy >= y + h) continue;
                g.enableScissor(cx, Math.max(cy, y), cx + tw, Math.min(cy + th, y + h));
                drawAny(g, f, i, cx, cy, tw, th, 140 + (int) (Math.sin(i) * 50));
                g.disableScissor();
                boolean hover = mx >= cx && mx < cx + tw && my >= cy && my < cy + th + 22;
                if (hover) g.fill(cx, cy, cx + tw, cy + th, 0x33FFFFFF);
                g.fill(cx + tw - 26, cy + 2, cx + tw - 2, cy + 11, 0xCC000000);
                g.drawString(f, i < Computers.VIDEOS.length ? "0:20" : "clip", cx + tw - 24, cy + 3, 0xFFFFFFFF, false);
                g.drawString(f, f.plainSubstrByWidth(titleOf(i), tw), cx, cy + th + 3, 0xFFFFFFFF, false);
                g.drawString(f, "§7" + channelOf(i) + " · " + viewsOf(i), cx, cy + th + 13, 0xFFAAAAAA, false);
            }
            g.disableScissor();
            return;
        }
        int vw = w - 8, vh = Math.min(vw * 9 / 16, h - 46);
        int vx = x + 4, vy = y + 4;
        g.enableScissor(vx, vy, vx + vw, vy + vh);
        drawAny(g, f, sel, vx, vy, vw, vh, t);
        if (!playing) {
            g.fill(vx, vy, vx + vw, vy + vh, 0x66000000);
            g.drawCenteredString(f, t >= DUR ? "↻ Replay" : "▶", vx + vw / 2, vy + vh / 2 - 4, 0xFFFFFFFF);
        }
        g.disableScissor();
        g.fill(vx, vy + vh, vx + vw, vy + vh + 2, 0xFF555555);
        g.fill(vx, vy + vh, vx + vw * Math.min(t, DUR) / DUR, vy + vh + 2, 0xFFFF0000);
        int s = Math.min(t, DUR) / 20;
        g.drawString(f, "0:" + String.format("%02d", s) + " / 0:20", vx, vy + vh + 5, 0xFFAAAAAA, false);
        g.drawString(f, f.plainSubstrByWidth(titleOf(sel), vw), vx, vy + vh + 16, 0xFFFFFFFF, false);
        g.drawString(f, "§7" + channelOf(sel), vx, vy + vh + 26, 0xFFAAAAAA, false);
        String like = (liked ? "§c♥ Liked" : "§f♡ Like");
        g.drawString(f, like, vx + vw - f.width(like.replaceAll("§.", "")), vy + vh + 26, 0xFFFFFFFF, false);
        g.drawString(f, "§8‹ Back (Backspace)", vx, y + h - 10, 0xFFFFFFFF, false);
    }

    public boolean click(double mx, double my, int b) {
        if (sel < 0) {
            int tw = os.phone() ? w - 8 : (w - 12) / 2;
            int th = tw * 9 / 16;
            int per = os.phone() ? 1 : 2;
            for (int i = 0; i < count(); i++) {
                int cx = x + 4 + (i % per) * (tw + 4), cy = y + 4 + (i / per) * (th + 26) - scroll;
                if (mx >= cx && mx < cx + tw && my >= cy && my < cy + th + 22 && my >= y) {
                    sel = i;
                    t = 0;
                    playing = true;
                    liked = false;
                    return true;
                }
            }
            return false;
        }
        int vw = w - 8, vh = Math.min(vw * 9 / 16, h - 46);
        if (my >= y + h - 12) { sel = -1; return true; }
        if (my > y + 4 + vh + 20 && mx > x + w - 50) { liked = !liked; return true; }
        if (my < y + 4 + vh) {
            if (t >= DUR) t = 0;
            playing = !playing;
            return true;
        }
        return false;
    }

    public boolean key(int k) {
        if (sel >= 0 && k == SPACE) {
            if (t >= DUR) t = 0;
            playing = !playing;
            return true;
        }
        if (sel >= 0 && k == BACKSPACE) { sel = -1; return true; }
        if (sel >= 0 && left(k)) { t = Math.max(0, t - 60); return true; }
        if (sel >= 0 && right(k)) { t = Math.min(DUR, t + 60); return true; }
        if (sel < 0 && down(k)) { scroll += 20; return true; }
        if (sel < 0 && up(k)) { scroll = Math.max(0, scroll - 20); return true; }
        return false;
    }

    public boolean scroll(double mx, double my, double d) {
        if (sel < 0) scroll = Math.max(0, Math.min(count() * 110, scroll - (int) (d * 20)));
        return true;
    }

    static void caption(GuiGraphics g, Font f, String s, int x, int y, int w, int h) {
        int cw = f.width(s);
        g.fill(x + w / 2 - cw / 2 - 3, y + h - 14, x + w / 2 + cw / 2 + 3, y + h - 3, 0xAA000000);
        g.drawCenteredString(f, s, x + w / 2, y + h - 12, 0xFFFFFFFF);
    }

    static void video(GuiGraphics g, Font f, int id, int x, int y, int w, int h, int t) {
        float s = w / 160f;
        switch (id) {
            case 0 -> {
                g.fillGradient(x, y, x + w, y + h, 0xFF8ECAE6, 0xFFE0F4FF);
                int ground = y + h - (int) (18 * s);
                g.fill(x, ground, x + w, y + h, 0xFF6A994E);
                g.fill(x + (int) (95 * s), ground - (int) (26 * s), x + (int) (140 * s), ground, 0xFF7F5539);
                float px;
                float py;
                int phase = t % 200;
                if (phase < 60) { px = 20 + phase * 0.6f; py = Math.abs(Mth.sin(phase * 0.3f)) * 10; }
                else if (phase < 110) { float k = (phase - 60) / 50f; px = 56 + k * 44; py = Mth.sin(k * Mth.PI) * 40; }
                else if (phase < 150) { px = 100; py = 26 - (phase - 110) * 1.2f; if (py < -30) py = -30; }
                else { px = 100; py = -30; }
                int sz = (int) (14 * s);
                int sx = x + (int) (px * s), sy = ground - sz - (int) (py * s);
                g.fill(sx, sy, sx + sz, sy + sz, 0xAA70E000);
                g.fill(sx + 2, sy + 2, sx + sz - 2, sy + sz - 2, 0xFF52B788);
                g.fill(sx + sz / 4, sy + sz / 3, sx + sz / 4 + 2, sy + sz / 3 + 2, 0xFF000000);
                g.fill(sx + sz * 3 / 4 - 2, sy + sz / 3, sx + sz * 3 / 4, sy + sz / 3 + 2, 0xFF000000);
                caption(g, f, phase < 60 ? "him: i can make that jump" : phase < 110 ? "the jump:" : phase < 150 ? "..." : "GONE WRONG", x, y, w, h);
            }
            case 1 -> {
                float day = (t % 400) / 400f;
                int sky = lerpColor(0xFF7FC8F8, 0xFFFF9F68, Mth.sin(day * Mth.PI));
                g.fillGradient(x, y, x + w, y + h, sky, 0xFFFFE5B4);
                for (int i = 0; i < 5; i++) {
                    int cx = x + Math.floorMod((int) (i * 47 * s - t * (0.6f + i * 0.2f) * s), w + 40) - 20;
                    int cy = y + (int) ((10 + i * 13) * s);
                    g.fill(cx, cy, cx + (int) (26 * s), cy + (int) (7 * s), 0xCCFFFFFF);
                }
                g.fill(x + w - (int) (40 * s), y + (int) (20 * s), x + w, y + (int) (30 * s), 0xFF6D6875);
                g.fill(x, y + h - (int) (14 * s), x + (int) (50 * s), y + h, 0xFF6D6875);
                float k = (t % 200) / 200f;
                int fx = x + (int) ((20 + k * 110) * s), fy = y + h - (int) ((22 + k * 70) * s);
                g.fill(fx, fy, fx + (int) (22 * s), fy + (int) (9 * s), 0xFFE63946);
                g.fill(fx, fy + (int) (3 * s), fx + (int) (22 * s), fy + (int) (5 * s), 0xFFFFFFFF);
                g.fill(fx + (int) (6 * s), fy + (int) (10 * s), fx + (int) (16 * s), fy + (int) (14 * s), 0xFF7F5539);
                caption(g, f, "city pad → Neon Heights · 21s", x, y, w, h);
            }
            case 2 -> {
                g.fillGradient(x, y, x + w, y + h, 0xFF0B0B2B, 0xFF3A0CA3);
                for (int i = 0; i < 30; i++) {
                    int sx = x + Math.floorMod(i * 53, w), sy = y + Math.floorMod(i * 29, Math.max(1, h / 2));
                    if ((t / 6 + i) % 7 != 0) g.fill(sx, sy, sx + 1, sy + 1, 0xFFFFFFFF);
                }
                int[] cols = {0xFFFF3FD2, 0xFF2EF2FF, 0xFF9DFF4A, 0xFFFFE04A};
                for (int i = 0; i < 8; i++) {
                    int bw = (int) ((12 + i % 3 * 4) * s), bh = (int) ((30 + (i * 17) % 40) * s);
                    int bx = x + (int) (i * 20 * s);
                    g.fill(bx, y + h - bh, bx + bw, y + h, 0xFF14142B);
                    g.fill(bx, y + h - bh, bx + bw, y + h - bh + 2, cols[i % 4]);
                    for (int wy = y + h - bh + 5; wy < y + h - 3; wy += 5) if (Math.floorMod(wy + i + t / 10, 3) != 0) g.fill(bx + 2, wy, bx + bw - 2, wy + 2, (cols[Math.floorMod(i + wy, 4)] & 0x00FFFFFF) | 0x99000000);
                }
                caption(g, f, "Neon Heights · 4K", x, y, w, h);
            }
            case 3 -> {
                g.fill(x, y, x + w, y + h, 0xFFF4E3C1);
                g.fill(x, y + h - (int) (30 * s), x + w, y + h, 0xFF8D6346);
                int cx = x + w / 2, base = y + h - (int) (34 * s);
                int layers = Math.min(6, t / 50);
                int[] lc = {0xFFD4A373, 0xFF6B4226, 0xFFFFD60A, 0xFF70E000, 0xFFE63946, 0xFFD4A373};
                String[] ln = {"bun", "patty", "cheese", "lettuce", "tomato", "bun!"};
                for (int i = 0; i < layers; i++) {
                    int lw = (int) ((i == 0 || i == 5 ? 46 : 50) * s), lh = (int) ((i == 1 ? 8 : 5) * s);
                    int drop = i == layers - 1 ? Math.max(0, 20 - (t % 50)) : 0;
                    int ly = base - (int) (i * 6 * s) - (int) (drop * s);
                    g.fill(cx - lw / 2, ly - lh, cx + lw / 2, ly, lc[i]);
                }
                caption(g, f, layers == 0 ? "Leo: okay chat, the Double Diesel" : "+ " + ln[Math.max(0, layers - 1)], x, y, w, h);
            }
            case 4 -> {
                g.fillGradient(x, y, x + w, y + h, 0xFF03071E, 0xFF370617);
                g.fill(x, y + h - (int) (10 * s), x + w, y + h, 0xFF023E8A);
                for (int b = 0; b < 3; b++) {
                    int bt = (t + b * 45) % 130;
                    int bx = x + (int) ((40 + b * 40) * s), by = y + (int) (40 * s) - b * 4;
                    int col = new int[]{0xFFFF006E, 0xFFFFBE0B, 0xFF3A86FF}[b];
                    if (bt < 30) g.fill(bx, y + h - (int) (bt * 2.2f * s), bx + 2, y + h - (int) (bt * 2.2f * s) + 4, 0xFFFFFFFF);
                    else if (bt < 90) {
                        float r = (bt - 30) * 0.5f * s;
                        int a = (int) (255 * (1 - (bt - 30) / 60f));
                        for (int i = 0; i < 12; i++) {
                            float ang = i / 12f * Mth.TWO_PI;
                            int px = bx + (int) (Mth.cos(ang) * r), py = by + (int) (Mth.sin(ang) * r);
                            g.fill(px, py, px + 2, py + 2, (a << 24) | (col & 0xFFFFFF));
                        }
                    }
                }
                caption(g, f, "live from the old pier", x, y, w, h);
            }
            case 6 -> skyline(g, f, x, y, w, h, t, s);
            case 7 -> festival(g, f, x, y, w, h, t, s);
            case 8 -> ocean(g, f, x, y, w, h, t, s);
            default -> {
                g.fillGradient(x, y, x + w, y + h, 0xFF2B2D42, 0xFF8D99AE);
                int n = 10 - Math.min(9, t / 40);
                String[] facts = {"It's never been late. Ever.", "Zara oils it every morning.", "The bell weighs 2 tonnes.", "Pigeons love it.", "It has 1,208 gears.",
                        "It once ran backwards (it didn't).", "The hands are made of copper.", "You can see it from the island.", "It chimes at noon.", "It's the heart of the city."};
                g.pose().pushPose();
                g.pose().translate(x + w / 2f, y + h / 2f - 14 * s, 0);
                g.pose().scale(3 * s, 3 * s, 1);
                g.drawCenteredString(f, "#" + n, 0, 0, 0xFFFFD166);
                g.pose().popPose();
                caption(g, f, facts[10 - n], x, y, w, h);
            }
        }
    }

    static void skyline(GuiGraphics g, Font f, int x, int y, int w, int h, int t, float s) {
        float k = (t % DUR) / (float) DUR;
        float night = Mth.clamp((k - 0.35f) / 0.35f, 0, 1);
        int top = lerpColor(0xFF4EA8DE, 0xFF0B1026, night), bot = lerpColor(0xFFFFD6A5, 0xFF3A0CA3, night);
        g.fillGradient(x, y, x + w, y + h, top, bot);
        int sunY = y + (int) (h * (0.15f + k * 1.1f));
        if (sunY < y + h) {
            int sx = x + w * 3 / 4, r = (int) (9 * s);
            g.fill(sx - r, sunY - r, sx + r, sunY + r, lerpColor(0xFFFFF3B0, 0xFFFF7B54, k * 2));
        }
        for (int i = 0; i < 40; i++) {
            int a = (int) (night * 255 * (((t / 5 + i) % 9) == 0 ? 0.4f : 1));
            if (a > 8) g.fill(x + Math.floorMod(i * 61, w), y + Math.floorMod(i * 37, Math.max(1, h / 2)), x + Math.floorMod(i * 61, w) + 1, y + Math.floorMod(i * 37, Math.max(1, h / 2)) + 1, (a << 24) | 0xFFFFFF);
        }
        int base = y + h - (int) (10 * s);
        int[] bh = {30, 48, 36, 70, 44, 58, 90, 40, 62, 34, 52, 76, 38, 46};
        int n = bh.length, bw = w / n;
        for (int i = 0; i < n; i++) {
            int bx = x + i * bw, top2 = base - (int) (bh[i] * s * 0.8f);
            g.fill(bx, top2, bx + bw - 1, base, lerpColor(0xFF6D6875, 0xFF14142B, night));
            for (int wy = top2 + 3; wy < base - 2; wy += (int) Math.max(3, 4 * s)) for (int wx = bx + 2; wx < bx + bw - 3; wx += (int) Math.max(3, 4 * s)) {
                boolean on = Math.floorMod(wx * 7 + wy * 13 + i, 5) != 0 && night > Math.floorMod(wx + wy, 10) / 12f;
                if (on) g.fill(wx, wy, wx + Math.max(1, (int) (2 * s)), wy + Math.max(1, (int) (2 * s)), 0xFFFFE08A);
            }
        }
        g.fill(x, base, x + w, y + h, lerpColor(0xFF1D6FA3, 0xFF03045E, night));
        int fx = x + Math.floorMod((int) (t * 1.2f * s), w + 40) - 20, fy = y + (int) (h * 0.3f);
        g.fill(fx, fy, fx + (int) (18 * s), fy + (int) (6 * s), 0xFFE63946);
        g.fill(fx + (int) (5 * s), fy + (int) (7 * s), fx + (int) (13 * s), fy + (int) (10 * s), 0xFF7F5539);
        if (night > 0.5f && (t / 8) % 2 == 0) g.fill(fx + (int) (17 * s), fy + (int) (2 * s), fx + (int) (19 * s), fy + (int) (4 * s), 0xFFFF0000);
        caption(g, f, night < 0.3f ? "Solaris by day" : night < 0.9f ? "golden hour" : "the city never sleeps", x, y, w, h);
    }

    static void festival(GuiGraphics g, Font f, int x, int y, int w, int h, int t, float s) {
        g.fillGradient(x, y, x + w, y + h, 0xFF0B0B2B, 0xFF2B1055);
        int cx = x + w / 2, base = y + h - (int) (8 * s);
        int sw = (int) (18 * s), sh = (int) (60 * s);
        g.fill(cx - sw / 2, base - sh, cx + sw / 2, base, 0xFF050510);
        g.fill(cx - sw / 2 - (int) (6 * s), base - sh + (int) (14 * s), cx + sw / 2 + (int) (6 * s), base - sh + (int) (34 * s), 0xFF050510);
        g.fill(cx - (int) (5 * s), base - sh - (int) (10 * s), cx + (int) (5 * s), base - sh, 0xFF050510);
        g.fill(x, base, x + w, y + h, 0xFF0A0A18);
        for (int i = 0; i < 18; i++) {
            int px = x + (int) ((i * 9 + 4) * s) % w;
            g.fill(px, base - (int) ((3 + (i % 3)) * s), px + (int) (2 * s), base, 0xFF1B1B3A);
        }
        int[] cols = {0xFFFF006E, 0xFFFFBE0B, 0xFF3A86FF, 0xFF8338EC, 0xFF06D6A0};
        for (int b = 0; b < 5; b++) {
            int bt = (t + b * 37) % 110;
            int bx = x + (int) ((20 + b * 30) * s), by = y + (int) ((18 + (b % 3) * 12) * s);
            if (bt < 25) g.fill(bx, base - (int) (bt * (base - by) / 25f), bx + 1, base - (int) (bt * (base - by) / 25f) + 3, 0xFFFFFFFF);
            else if (bt < 85) {
                float r = (bt - 25) * 0.45f * s;
                int a = (int) (255 * (1 - (bt - 25) / 60f));
                for (int i = 0; i < 16; i++) {
                    float ang = i / 16f * Mth.TWO_PI;
                    int px = bx + (int) (Mth.cos(ang) * r), py = by + (int) (Mth.sin(ang) * r);
                    g.fill(px, py, px + 2, py + 2, (a << 24) | (cols[b] & 0xFFFFFF));
                }
            }
        }
        caption(g, f, t % 200 < 100 ? "All hail the Founder!" : "cake, music & fireworks", x, y, w, h);
    }

    static void ocean(GuiGraphics g, Font f, int x, int y, int w, int h, int t, float s) {
        g.fillGradient(x, y, x + w, y + h, 0xFF48CAE4, 0xFF03045E);
        for (int i = 0; i < 6; i++) {
            int rx = x + Math.floorMod(i * 29 + t / 3, w);
            g.fillGradient(rx, y, rx + (int) (6 * s), y + h, 0x22FFFFFF, 0x00FFFFFF);
        }
        g.fill(x, y + h - (int) (8 * s), x + w, y + h, 0xFFE9C46A);
        for (int i = 0; i < 6; i++) {
            int kx = x + (int) ((10 + i * 26) * s), kh = (int) ((14 + (i * 7) % 12) * s);
            for (int j = 0; j < kh; j += 2) g.fill(kx + (int) (Mth.sin((t + j * 6 + i * 20) * 0.08f) * 2 * s), y + h - (int) (8 * s) - j, kx + (int) (2 * s) + (int) (Mth.sin((t + j * 6 + i * 20) * 0.08f) * 2 * s), y + h - (int) (8 * s) - j + 2, 0xFF2D6A4F);
        }
        int[] fc = {0xFFFF9F1C, 0xFFFFBF69, 0xFFE63946, 0xFFF4F1DE, 0xFF90E0EF};
        for (int i = 0; i < 7; i++) {
            int dir = i % 2 == 0 ? 1 : -1;
            int fx = x + Math.floorMod((int) (dir * t * (0.5f + i * 0.15f) * s + i * 40), w + 20) - 10, fy = y + (int) ((16 + i * 9) * s) + (int) (Mth.sin(t * 0.1f + i) * 2 * s);
            int fw = (int) (8 * s), fh = (int) (4 * s);
            g.fill(fx, fy, fx + fw, fy + fh, fc[i % fc.length]);
            g.fill(dir > 0 ? fx - (int) (3 * s) : fx + fw, fy, dir > 0 ? fx : fx + fw + (int) (3 * s), fy + fh, fc[i % fc.length]);
        }
        int wt = t % DUR;
        if (wt > 180 && wt < 380) {
            int wx = x + w - (int) ((wt - 180) * 1.1f * s), wy = y + (int) (30 * s);
            g.fill(wx, wy, wx + (int) (46 * s), wy + (int) (14 * s), 0xFF34495E);
            g.fill(wx + (int) (46 * s), wy + (int) (2 * s), wx + (int) (56 * s), wy + (int) (12 * s), 0xFF34495E);
            g.fill(wx + (int) (6 * s), wy + (int) (4 * s), wx + (int) (8 * s), wy + (int) (6 * s), 0xFFFFFFFF);
        }
        for (int i = 0; i < 10; i++) {
            int bx = x + Math.floorMod(i * 47, w), by = y + h - Math.floorMod(t * 2 + i * 31, h);
            g.fill(bx, by, bx + 2, by + 2, 0x88FFFFFF);
        }
        caption(g, f, wt > 180 && wt < 380 ? "a whale! in Solaris Bay!" : "narrated by Sam the Dockmaster", x, y, w, h);
    }

    static int lerpColor(int a, int b, float k) {
        k = Mth.clamp(k, 0, 1);
        int r = (int) Mth.lerp(k, (a >> 16) & 255, (b >> 16) & 255), gg = (int) Mth.lerp(k, (a >> 8) & 255, (b >> 8) & 255), bb = (int) Mth.lerp(k, a & 255, b & 255);
        return 0xFF000000 | (r << 16) | (gg << 8) | bb;
    }
}
