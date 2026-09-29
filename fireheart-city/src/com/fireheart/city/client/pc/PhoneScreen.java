package com.fireheart.city.client.pc;

import com.fireheart.city.PcNet;
import com.fireheart.city.Phones;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** FirePhone OS: a portrait phone with a home screen, dock, notifications and apps. */
public class PhoneScreen extends OsScreen {
    public static final int SW = 140, STATUS = 15, HOME = 10, HEAD = 14, R = 13;
    record Icon(String label, int color, String glyph, Supplier<App> make) {}

    private final List<Icon> icons = new ArrayList<>();
    private final List<Icon> dock = new ArrayList<>();
    private final List<Icon> hidden = new ArrayList<>();
    private App app;
    private int sel;
    int x0, y0, sh;
    private int bootTicks = 10, bannerTicks;
    private String banner = "";

    public PhoneScreen(PcNet.Data d) {
        super(Component.literal("SolPhone"), d);
        dock.add(new Icon("Phone", 0xFF2DC653, "☎", CallApp::new));
        dock.add(new Icon("Messages", 0xFF3A86FF, "✉", PhoneMessagesApp::new));
        dock.add(new Icon("SolFeed", 0xFFFF6B6B, "♥", FeedApp::new));
        dock.add(new Icon("Map", 0xFF52B788, "⌖", MapApp::new));
        icons.add(new Icon("SolTube", 0xFFD62828, "▶", TubeApp::new));
        icons.add(new Icon("Camera", 0xFF343A40, "◉", CameraApp::new));
        icons.add(new Icon("SolEats", 0xFFF77F00, "♨", EatsApp::new));
        icons.add(new Icon("Ferry", 0xFF7FC8F8, "✈", FerryApp::new));
        icons.add(new Icon("Weather", 0xFFFFB703, "☀", WeatherApp::new));
        icons.add(new Icon("News", 0xFFE76F51, "N", NewsApp::new));
        icons.add(new Icon("Bank", 0xFFD4A017, "$", BankApp::new));
        icons.add(new Icon("SolTech", 0xFF264653, "⚡", StoreApp::new));
        icons.add(new Icon("Settings", 0xFF6C757D, "⚙", SettingsApp::new));
        icons.add(new Icon("Notes", 0xFFE9C46A, "✎", NotesApp::new));
        icons.add(new Icon("Calc", 0xFF8D99AE, "=", CalcApp::new));
        icons.add(new Icon("Games", 0xFF5A189A, "♠", GamesApp::new));
        hidden.add(new Icon("Snake", 0xFF70E000, "S", SnakeGame::new));
        hidden.add(new Icon("2048", 0xFFEDC22E, "2", Game2048::new));
        hidden.add(new Icon("Flap", 0xFF48CAE4, "F", FlapGame::new));
        hidden.add(new Icon("Mines", 0xFFE63946, "✹", MinesGame::new));
        hidden.add(new Icon("Blocks", 0xFF9B5DE5, "▦", BlocksGame::new));
        hidden.add(new Icon("Scores", 0xFF0096C7, "★", ScoresApp::new));
        hidden.add(new Icon("Neon Drift", 0xFFFF2E88, "»", NeonDrift::new));
        hidden.add(new Icon("Fox Run", 0xFF1B3A6B, "✦", FoxRun::new));
        hidden.add(new Icon("Neon Maze", 0xFF00B4D8, "▣", NeonMaze::new));
        hidden.add(new Icon("Beat Solaris", 0xFF7209B7, "♪", BeatSol::new));
    }

    @Override
    public boolean phone() {
        return true;
    }

    int model() {
        return Math.max(1, SettingsApp.num(SettingsApp.parts(this)[3]));
    }

    int caseStyle() {
        return Math.floorMod(SettingsApp.num(SettingsApp.parts(this)[2]), SettingsApp.CASES.length);
    }

    private boolean viewfinder() {
        return app instanceof CameraApp c && c.viewfinder();
    }

    public void battery(int level, boolean charging) {
        data.battery = level;
        data.charging = charging;
        if (level <= 0) onClose();
    }

    private List<Icon> all() {
        List<Icon> l = new ArrayList<>(icons);
        l.addAll(dock);
        l.addAll(hidden);
        return l;
    }

    @Override
    public void openAppById(String id) {
        List<Icon> l = all();
        for (int i = 0; i < l.size(); i++) if (l.get(i).label.equals(id)) { open(l.get(i)); return; }
    }

    public void update(PcNet.Data d) {
        this.data = d;
        loadUnread();
        if (app instanceof PhoneMessagesApp m && m.openId() != null) markRead(m.openId());
        if (app != null) {
            app.addWidgetsRefresh();
            app.onData();
        }
    }

    @Override
    public void onMessage(String line) {
        data.inbox.add(line);
        String[] p = line.split("\\|", 4);
        if (p.length == 4 && p[1].equals("<")) {
            boolean looking = app instanceof PhoneMessagesApp m && p[0].equals(m.openId());
            countIncoming(p[0], looking);
            if (!looking) showToast(residentName(p[0]) + ": " + p[3]);
        }
        if (app instanceof PhoneMessagesApp m) m.onMessage();
    }

    @Override
    public void showToast(String text) {
        banner = text;
        bannerTicks = 90;
    }

    public void onCall() {
        if ((ClientCall.active() || ClientCall.state.equals("ended")) && !(app instanceof CallApp)) openAppById("Phone");
        if (app instanceof CallApp c) c.refresh();
    }

    @Override
    protected void init() {
        sh = Math.min(236, height - 24);
        x0 = (width - SW) / 2;
        y0 = (height - sh) / 2;
        if (app != null) {
            layoutApp();
            app.addWidgets();
        } else if (ClientCall.active()) {
            bootTicks = 0;
            openAppById("Phone");
        }
    }

    private void layoutApp() {
        app.layout(x0, y0 + STATUS + HEAD, SW, sh - STATUS - HEAD - HOME);
    }

    private void open(Icon ic) {
        closeApp();
        app = ic.make.get();
        app.open(this);
        layoutApp();
        app.addWidgets();
    }

    void closeApp() {
        if (app != null) {
            app.close();
            clearWidgets();
            app = null;
        }
    }

    public void home() {
        closeApp();
    }

    @Override
    public void tick() {
        if (bootTicks > 0) bootTicks--;
        if (bannerTicks > 0) bannerTicks--;
        if (app != null) {
            if (app.game()) for (int k : Gamepad.poll()) app.key(k);
            try {
                app.tick();
            } catch (RuntimeException e) {
                com.fireheart.city.FireheartCity.LOG.warn("SolPhone app crashed", e);
                showToast("That app crashed - back to home.");
                home();
            }
        }
    }

    private int iconX(int i) {
        return x0 + 4 + (i % 4) * 33;
    }

    private int iconY(int i) {
        return y0 + STATUS + 8 + (i / 4) * 36;
    }

    private int dockY() {
        return y0 + sh - HOME - 32;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (!viewfinder()) renderBackground(g);
        int body = 0xFF000000 | Phones.RGB[Math.floorMod(data.phoneColor, Phones.RGB.length)];
        if (viewfinder()) {
            int[][] strips = {{x0 - 12, y0 - 12, x0 + SW + 12, y0 + STATUS + HEAD}, {x0 - 12, y0 + sh - 44, x0 + SW + 12, y0 + sh + 12}, {x0 - 12, y0, x0, y0 + sh}, {x0 + SW, y0, x0 + SW + 12, y0 + sh}};
            for (int[] st : strips) {
                g.enableScissor(st[0], st[1], st[2], st[3]);
                frame(g, body);
                g.disableScissor();
            }
        } else frame(g, body);
        g.enableScissor(x0, y0, x0 + SW, y0 + sh);
        if (bootTicks > 0) {
            g.fill(x0, y0, x0 + SW, y0 + sh, 0xFF000000);
            g.drawCenteredString(font, model() >= 2 ? "§6SolPhone §f2" : "§6SolPhone", x0 + SW / 2, y0 + sh / 2 - 4, 0xFFFFFFFF);
            island(g, 0);
            g.disableScissor();
            screenCorners(g, R, 0xFF0B0B0D);
            return;
        }
        if (!viewfinder()) wallpaper(g, body);
        if (app == null) homeScreen(g, mx, my);
        else {
            g.fill(x0, y0, x0 + SW, y0 + STATUS + HEAD, viewfinder() ? 0x99000000 : app.color());
            g.fill(x0, y0 + STATUS + HEAD - 1, x0 + SW, y0 + STATUS + HEAD, 0x33000000);
            g.drawString(font, "‹", x0 + 6, y0 + STATUS + 3, 0xFFFFFFFF, false);
            String t = app.title();
            if (font.width(t) > SW - 24) t = font.plainSubstrByWidth(t, SW - 28) + "…";
            g.drawCenteredString(font, "§l" + t, x0 + SW / 2, y0 + STATUS + 3, 0xFFFFFFFF);
            if (!viewfinder()) g.fill(x0, y0 + STATUS + HEAD, x0 + SW, y0 + sh - HOME, 0xFFF4F4F6);
            else g.fill(x0, y0 + sh - HOME, x0 + SW, y0 + sh, 0xFF000000);
        }
        if (app != null) {
            try {
                app.render(g, mx, my, pt);
            } catch (RuntimeException e) {
                com.fireheart.city.FireheartCity.LOG.warn("SolPhone app failed to draw", e);
                showToast("That app crashed - back to home.");
                home();
            }
        }
        g.disableScissor();
        super.render(g, mx, my, pt);
        if (app != null) app.renderOver(g, mx, my, pt);
        g.enableScissor(x0, y0, x0 + SW, y0 + sh);
        statusBar(g);
        island(g, ClientCall.active() && !(app instanceof CallApp) ? 1 : 0);
        if (bannerTicks > 0) {
            int drop = Math.min(Math.min(90 - bannerTicks, bannerTicks), 8);
            int by = y0 + 3 + drop * 2;
            List<net.minecraft.util.FormattedCharSequence> ls = font.split(Component.literal(banner), SW - 22);
            int bh = 10 + Math.min(2, ls.size()) * 10;
            roundRect(g, x0 + 5, by, x0 + SW - 5, by + bh, 9, 0xF2F2F2F7);
            for (int i = 0; i < Math.min(2, ls.size()); i++) g.drawString(font, ls.get(i), x0 + 11, by + 5 + i * 10, 0xFF111111, false);
        }
        roundRect(g, x0 + SW / 2 - 24, y0 + sh - 6, x0 + SW / 2 + 24, y0 + sh - 3, 1, app == null ? 0xDDFFFFFF : 0xFF1C1C1E);
        g.disableScissor();
        screenCorners(g, R, 0xFF0B0B0D);
    }

    /** Titanium-style band in the phone colour, thin black bezel and the side buttons. */
    private void frame(GuiGraphics g, int body) {
        int cs = caseStyle();
        if (cs > 0) {
            int cc = switch (cs) {
                case 1 -> 0x66DDEEFF;
                case 2 -> 0xFF6F4518;
                case 3 -> 0xFFE0AAFF;
                default -> 0xFF14142B;
            };
            roundRect(g, x0 - 10, y0 - 10, x0 + SW + 10, y0 + sh + 10, R + 10, cc);
            if (cs == 2) {
                for (int yy = y0 - 6; yy < y0 + sh + 6; yy += 6) g.fill(x0 - 9, yy, x0 - 8, yy + 3, 0xFFB08968);
                for (int yy = y0 - 6; yy < y0 + sh + 6; yy += 6) g.fill(x0 + SW + 8, yy, x0 + SW + 9, yy + 3, 0xFFB08968);
            } else if (cs == 3) {
                long t = System.currentTimeMillis() / 300;
                for (int i = 0; i < 14; i++) {
                    int px = (int) Math.floorMod(i * 37 + t * (i % 3 + 1), SW + 18) - 9, side = i % 4;
                    int sx = side < 2 ? x0 + px : side == 2 ? x0 - 9 : x0 + SW + 8;
                    int sy = side == 0 ? y0 - 9 : side == 1 ? y0 + sh + 8 : y0 + Math.floorMod(i * 53 + t, sh);
                    g.fill(sx, sy, sx + 1, sy + 1, 0xFFFFFFFF);
                }
            } else if (cs == 4) {
                int pulse = (int) (System.currentTimeMillis() / 20 % 510);
                int a = pulse > 255 ? 510 - pulse : pulse;
                roundRect(g, x0 - 11, y0 - 11, x0 + SW + 11, y0 - 9, 1, (a << 24) | 0xFF4D6D);
                roundRect(g, x0 - 11, y0 + sh + 9, x0 + SW + 11, y0 + sh + 11, 1, (a << 24) | 0x4CC9F0);
            }
        }
        int hi = lighten(body, 60), lo = darken(body, 50);
        g.fill(x0 - 9, y0 + 34, x0 - 7, y0 + 42, lo);
        g.fill(x0 - 9, y0 + 52, x0 - 7, y0 + 68, lo);
        g.fill(x0 - 9, y0 + 72, x0 - 7, y0 + 88, lo);
        g.fill(x0 + SW + 7, y0 + 56, x0 + SW + 9, y0 + 82, lo);
        roundRect(g, x0 - 7, y0 - 7, x0 + SW + 7, y0 + sh + 7, R + 7, lo);
        roundRect(g, x0 - 6, y0 - 6, x0 + SW + 6, y0 + sh + 6, R + 6, hi);
        roundRect(g, x0 - 5, y0 - 5, x0 + SW + 5, y0 + sh + 5, R + 5, body);
        roundRect(g, x0 - 4, y0 - 4, x0 + SW + 4, y0 + sh + 4, R + 4, 0xFF0B0B0D);
    }

    /** The Dynamic Island: a black pill that grows into a live call indicator. */
    private void island(GuiGraphics g, int mode) {
        int cx = x0 + SW / 2, top = y0 + 3;
        if (mode == 0) {
            int iw = model() >= 2 ? 15 : 19;
            roundRect(g, cx - iw, top, cx + iw, top + 10, 5, 0xFF000000);
            g.fill(cx + iw - 9, top + 4, cx + iw - 7, top + 6, 0xFF1A1F2E);
            return;
        }
        int w = 52;
        roundRect(g, cx - w, top, cx + w, top + 11, 5, 0xFF000000);
        boolean ring = ClientCall.ringing();
        int dot = ring && (ClientCall.ticks / 10) % 2 == 0 ? 0xFF7CFC9A : 0xFF2DC653;
        roundRect(g, cx - w + 4, top + 2, cx - w + 11, top + 9, 3, dot);
        String left = font.plainSubstrByWidth(ClientCall.name, w - 16);
        g.drawString(font, left, cx - w + 14, top + 2, 0xFFFFFFFF, false);
        String right = ring ? "☎" : ClientCall.timer();
        g.drawString(font, right, cx + w - 4 - font.width(right), top + 2, 0xFF2DC653, false);
    }

    static int lighten(int c, int a) {
        int r = Math.min(255, ((c >> 16) & 255) + a), gg = Math.min(255, ((c >> 8) & 255) + a), b = Math.min(255, (c & 255) + a);
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }

    static int darken(int c, int a) {
        int r = Math.max(0, ((c >> 16) & 255) - a), gg = Math.max(0, ((c >> 8) & 255) - a), b = Math.max(0, (c & 255) - a);
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }

    static int inset(int r, int i) {
        double d = r - i - 0.5;
        return (int) Math.round(r - Math.sqrt(Math.max(0, r * r - d * d)));
    }

    static void roundRect(GuiGraphics g, int x0, int y0, int x1, int y1, int r, int color) {
        r = Math.min(r, Math.min((x1 - x0) / 2, (y1 - y0) / 2));
        for (int i = 0; i < r; i++) {
            int in = inset(r, i);
            g.fill(x0 + in, y0 + i, x1 - in, y0 + i + 1, color);
            g.fill(x0 + in, y1 - i - 1, x1 - in, y1 - i, color);
        }
        g.fill(x0, y0 + r, x1, y1 - r, color);
    }

    private void screenCorners(GuiGraphics g, int r, int color) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        for (int i = 0; i < r; i++) {
            int in = inset(r, i);
            if (in <= 0) continue;
            g.fill(x0, y0 + i, x0 + in, y0 + i + 1, color);
            g.fill(x0 + SW - in, y0 + i, x0 + SW, y0 + i + 1, color);
            g.fill(x0, y0 + sh - i - 1, x0 + in, y0 + sh - i, color);
            g.fill(x0 + SW - in, y0 + sh - i - 1, x0 + SW, y0 + sh - i, color);
        }
        g.pose().popPose();
    }

    private void wallpaper(GuiGraphics g, int body) {
        Wallpapers.draw(g, setting(4, "0"), x0, y0, SW, sh, body);
    }

    String setting(int i, String def) {
        PhoneHud.previews = !settingRaw(6).equals("0");
        return settingRaw(i).isEmpty() ? def : settingRaw(i);
    }

    String settingRaw(int i) {
        String[] p = data.settings.split("\\|");
        return p.length > i ? p[i] : "";
    }

    String settingOld(int i, String def) {
        String[] p = data.settings.split("\\|");
        return p.length > i && !p[i].isEmpty() ? p[i] : def;
    }

    static String twelve(String hhmm) {
        try {
            String[] p = hhmm.split(":");
            int h = Integer.parseInt(p[0].trim());
            return (h % 12 == 0 ? 12 : h % 12) + ":" + p[1];
        } catch (RuntimeException e) {
            return hhmm;
        }
    }

    private void statusBar(GuiGraphics g) {
        String clock = data.clock;
        int sp = clock.indexOf(' ');
        if (sp > 0) clock = clock.substring(sp + 1);
        if (!setting(5, "0").equals("1")) clock = twelve(clock);
        g.drawString(font, "§l" + clock, x0 + 12, y0 + 4, 0xFFFFFFFF, false);
        int sx = x0 + SW - 42, by = y0 + 11;
        for (int i = 0; i < 4; i++) g.fill(sx + i * 3, by - 2 - i * 2, sx + i * 3 + 2, by, 0xFFFFFFFF);
        int wx = x0 + SW - 27;
        g.fill(wx + 1, by - 7, wx + 6, by - 6, 0xFFFFFFFF);
        g.fill(wx, by - 6, wx + 1, by - 5, 0xFFFFFFFF);
        g.fill(wx + 6, by - 6, wx + 7, by - 5, 0xFFFFFFFF);
        g.fill(wx + 2, by - 4, wx + 5, by - 3, 0xFFFFFFFF);
        g.fill(wx + 3, by - 1, wx + 4, by, 0xFFFFFFFF);
        roundRect(g, x0 + SW - 18, by - 7, x0 + SW - 6, by, 2, 0xAAFFFFFF);
        int bl = Math.max(1, 9 * Math.max(0, Math.min(100, data.battery)) / 100);
        roundRect(g, x0 + SW - 17, by - 6, x0 + SW - 17 + bl, by - 1, 1, data.charging ? 0xFF2DC653 : data.battery <= 20 ? 0xFFFF453A : 0xFFFFFFFF);
        g.fill(x0 + SW - 5, by - 5, x0 + SW - 4, by - 2, 0xAAFFFFFF);
        if (data.charging) g.drawString(font, "⚡", x0 + SW - 26, by - 8, 0xFFFFD60A, false);
    }

    private void drawIcon(GuiGraphics g, Icon ic, int ix, int iy, boolean hover, boolean label) {
        int cx = ix + 16;
        if (hover) roundRect(g, ix + 1, iy - 1, ix + 31, iy + 33, 6, 0x33FFFFFF);
        if (!AppIcons.draw(g, ic.label, cx - 11, iy + 1, 22)) {
            roundRect(g, cx - 12, iy + 1, cx + 12, iy + 23, 6, 0xFF000000 | (ic.color & 0xFFFFFF));
            roundRect(g, cx - 12, iy + 1, cx + 12, iy + 7, 3, 0x22FFFFFF);
            g.drawCenteredString(font, "§l" + ic.glyph, cx, iy + 8, 0xFFFFFFFF);
        }
        if (label) {
            g.pose().pushPose();
            g.pose().translate(cx, iy + 25, 0);
            g.pose().scale(0.7f, 0.7f, 1f);
            g.drawCenteredString(font, ic.label, 0, 0, 0xFFFFFFFF);
            g.pose().popPose();
        }
    }

    private void homeScreen(GuiGraphics g, int mx, int my) {
        for (int i = 0; i < icons.size(); i++) {
            int ix = iconX(i), iy = iconY(i);
            boolean hover = mx >= ix && mx < ix + 33 && my >= iy && my < iy + 34 || i == sel;
            drawIcon(g, icons.get(i), ix, iy, hover, !setting(7, "1").equals("0"));
        }
        int dy = dockY();
        roundRect(g, x0 + 3, dy - 3, x0 + SW - 3, dy + 28, 8, 0x55FFFFFF);
        for (int i = 0; i < dock.size(); i++) {
            int ix = x0 + 4 + i * 33;
            boolean hover = mx >= ix && mx < ix + 33 && my >= dy && my < dy + 26 || sel == icons.size() + i;
            drawIcon(g, dock.get(i), ix, dy, hover, false);
            int unread = unreadTotal();
            if (i == 1 && unread > 0) {
                g.fill(ix + 24, dy - 1, ix + 32, dy + 7, 0xFFE63946);
                g.drawCenteredString(font, String.valueOf(Math.min(9, unread)), ix + 28, dy, 0xFFFFFFFF);
            }
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        com.fireheart.city.client.DeviceAnim.activity(true);
        if (bootTicks > 0) return true;
        if (key == App.ESC) {
            if (app != null) {
                if (!app.back()) home();
                return true;
            }
            onClose();
            return true;
        }
        if (app != null) {
            if (app.key(key)) return true;
            return super.keyPressed(key, scan, mods);
        }
        int n = icons.size() + dock.size();
        if (sel >= n) sel = 0;
        if (key == App.RIGHT) sel = (sel + 1) % n;
        else if (key == App.LEFT) sel = (sel + n - 1) % n;
        else if (key == App.DOWN) sel = Math.min(n - 1, sel + 4);
        else if (key == App.UP) sel = Math.max(0, sel - 4);
        else if (key == App.ENTER || key == App.KP_ENTER || key == App.SPACE) open(all().get(sel));
        else return super.keyPressed(key, scan, mods);
        return true;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        com.fireheart.city.client.DeviceAnim.activity(true);
        if (app != null && app.typed(c)) return true;
        return super.charTyped(c, mods);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        com.fireheart.city.client.DeviceAnim.activity(false);
        if (bootTicks > 0) return true;
        if (my >= y0 + sh - HOME && my < y0 + sh && mx >= x0 && mx < x0 + SW) {
            home();
            return true;
        }
        if (ClientCall.active() && !(app instanceof CallApp) && my >= y0 + 2 && my < y0 + 15 && mx > x0 + SW / 2 - 52 && mx < x0 + SW / 2 + 52) {
            openAppById("Phone");
            return true;
        }
        if (app != null) {
            if (my >= y0 + STATUS && my < y0 + STATUS + HEAD && mx >= x0 && mx < x0 + 22) {
                if (!app.back()) home();
                return true;
            }
            if (super.mouseClicked(mx, my, button)) return true;
            return app.click(mx, my, button);
        }
        for (int i = 0; i < icons.size(); i++) {
            int ix = iconX(i), iy = iconY(i);
            if (mx >= ix && mx < ix + 33 && my >= iy && my < iy + 34) {
                open(icons.get(i));
                return true;
            }
        }
        int dy = dockY();
        for (int i = 0; i < dock.size(); i++) {
            int ix = x0 + 4 + i * 33;
            if (mx >= ix && mx < ix + 33 && my >= dy && my < dy + 26) {
                open(dock.get(i));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (app != null && app.drag(mx, my, button, dx, dy)) return true;
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (app != null && app.scroll(mx, my, delta)) return true;
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public void removed() {
        if (app != null) app.close();
        send("phone_close", "", "");
        super.removed();
    }
}
