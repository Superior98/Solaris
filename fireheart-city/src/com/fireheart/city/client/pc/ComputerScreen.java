package com.fireheart.city.client.pc;

import com.fireheart.city.PcNet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** FireOS: the desktop of the Fireheart PC. Icons open apps and games in a window. */
public class ComputerScreen extends OsScreen {
    public static final int W = 340, H = 214, BAR = 14;
    record Icon(String label, int color, String glyph, Supplier<App> make) {}

    private final List<Icon> icons = new ArrayList<>();
    private App app;
    private int sel;
    int x0, y0;
    private int bootTicks = 24;
    private boolean kioskOpened;

    public ComputerScreen(PcNet.Data d) {
        super(Component.literal("Solaris PC"), d);
        icons.add(new Icon("SolNet", 0xFFE76F51, "N", NewsApp::new));
        icons.add(new Icon("Messages", 0xFF2A9D8F, "M", MessengerApp::new));
        icons.add(new Icon("Notes", 0xFFE9C46A, "✎", NotesApp::new));
        icons.add(new Icon("Calc", 0xFF8D99AE, "=", CalcApp::new));
        icons.add(new Icon("Bank", 0xFFD4A017, "$", BankApp::new));
        icons.add(new Icon("Scores", 0xFF48CAE4, "★", ScoresApp::new));
        icons.add(new Icon("Snake", 0xFF70E000, "S", SnakeGame::new));
        icons.add(new Icon("Blocks", 0xFF9B5DE5, "▦", BlocksGame::new));
        icons.add(new Icon("Mines", 0xFFE63946, "✹", MinesGame::new));
        icons.add(new Icon("2048", 0xFFEDC22E, "2", Game2048::new));
        icons.add(new Icon("Ferry Flap", 0xFF7FC8F8, "F", FlapGame::new));
        icons.add(new Icon("Neon Drift", 0xFFFF2E88, "»", NeonDrift::new));
        icons.add(new Icon("Fox Run", 0xFF1B3A6B, "✦", FoxRun::new));
        icons.add(new Icon("Neon Maze", 0xFF00B4D8, "▣", NeonMaze::new));
        icons.add(new Icon("Beat Solaris", 0xFF7209B7, "♪", BeatSol::new));
        icons.add(new Icon("SolFeed", 0xFFFF6B6B, "♥", FeedApp::new));
        icons.add(new Icon("SolTube", 0xFFD62828, "▶", TubeApp::new));
        icons.add(new Icon("SolTech", 0xFF264653, "⚡", StoreApp::new));
        icons.add(new Icon("City Map", 0xFF52B788, "⌖", MapApp::new));
        if (d.device == 3) icons.removeIf(i -> !java.util.Set.of("Snake", "Blocks", "Mines", "2048", "Ferry Flap", "Scores").contains(i.label));
        if (d.device == 4) icons.removeIf(i -> !i.label.equals("SolTube"));
    }

    String osName() {
        return switch (data.device) {
            case 2 -> "SolPad";
            case 3 -> "SolStation";
            case 4 -> "SolTube TV";
            default -> "SolOS";
        };
    }

    @Override
    public boolean phone() {
        return false;
    }

    @Override
    public void openAppById(String id) {
        for (int i = 0; i < icons.size(); i++) if (icons.get(i).label.equals(id)) { openApp(i); return; }
    }

    public void update(PcNet.Data d) {
        this.data = d;
        loadUnread();
        if (app instanceof MessengerApp m) markRead(m.selectedId());
        if (app != null) {
            app.addWidgetsRefresh();
            app.onData();
        }
    }

    public void onMessage(String line) {
        data.inbox.add(line);
        String[] p = line.split("\\|", 4);
        if (p.length == 4 && p[1].equals("<")) {
            String name = p[0];
            for (String[] r : data.residents) if (r[0].equals(p[0])) name = r[1];
            countIncoming(p[0], app instanceof MessengerApp m && p[0].equals(m.selectedId()));
            showToast("New message from " + name);
        }
        if (app instanceof MessengerApp m) m.onMessage();
    }


    @Override
    protected void init() {
        x0 = (width - W) / 2;
        y0 = (height - H) / 2 - 6;
        if (data.device == 4 && app == null && !kioskOpened) {
            kioskOpened = true;
            bootTicks = 0;
            openApp(0);
            return;
        }
        if (data.kiosk && app == null && !kioskOpened) {
            kioskOpened = true;
            bootTicks = 0;
            openApp(13);
            return;
        }
        if (app != null) {
            layoutApp();
            app.addWidgets();
        }
    }

    private void layoutApp() {
        app.layout(x0 + 6, y0 + 20, W - 12, H - BAR - 26);
    }

    void openApp(int i) {
        closeApp();
        sel = i;
        app = icons.get(i).make.get();
        app.open(this);
        layoutApp();
        app.addWidgets();
        send(app.game() ? "game" : app instanceof TubeApp ? "video" : "desktop", "", "");
    }

    void closeApp() {
        if (app != null) {
            app.close();
            clearWidgets();
            boolean wasGame = app.game() || app instanceof TubeApp;
            app = null;
            if (wasGame) send("desktop", "", "");
        }
    }


    @Override
    public void tick() {
        if (bootTicks > 0) bootTicks--;
        if (toastTicks > 0) toastTicks--;
        if (app != null) {
            if (app.game()) for (int k : Gamepad.poll()) app.key(k);
            try {
                app.tick();
            } catch (RuntimeException e) {
                com.fireheart.city.FireheartCity.LOG.warn("SolOS app crashed", e);
                showToast("That app crashed - restarted it.");
                closeApp();
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        if (data.device == 2) {
            PhoneScreen.roundRect(g, x0 - 12, y0 - 12, x0 + W + 12, y0 + H + 12, 14, 0xFFADB5BD);
            PhoneScreen.roundRect(g, x0 - 11, y0 - 11, x0 + W + 11, y0 + H + 11, 13, 0xFF0B0B0D);
        } else if (data.device == 3) {
            PhoneScreen.roundRect(g, x0 - 10, y0 - 10, x0 + W + 10, y0 + H + 12, 8, 0xFF10002B);
            g.fill(x0 - 10, y0 + H + 10, x0 + W + 10, y0 + H + 12, 0xFF7B2CBF);
        } else if (data.device == 4) {
            g.fill(x0 - 10, y0 - 10, x0 + W + 10, y0 + H + 10, 0xFF050505);
            g.fill(x0 + W / 2 - 30, y0 + H + 10, x0 + W / 2 + 30, y0 + H + 16, 0xFF1C1D21);
        } else {
        g.fill(x0 - 8, y0 - 8, x0 + W + 8, y0 + H + 10, 0xFF1C1D21);
        g.fill(x0 - 7, y0 - 7, x0 + W + 7, y0 + H + 9, 0xFF2B2D31);
        g.fill(x0 + W / 2 - 24, y0 + H + 10, x0 + W / 2 + 24, y0 + H + 16, 0xFF232428);
        g.fill(x0 + W / 2 - 40, y0 + H + 16, x0 + W / 2 + 40, y0 + H + 19, 0xFF1C1D21);
        g.fill(x0 + W - 2, y0 + H + 4, x0 + W + 1, y0 + H + 6, bootTicks > 0 ? 0xFFFFB000 : 0xFF3CFF6E);
        }
        g.enableScissor(x0, y0, x0 + W, y0 + H);
        if (bootTicks > 0) {
            g.fill(x0, y0, x0 + W, y0 + H, 0xFF000000);
            g.drawCenteredString(font, "§6" + osName(), x0 + W / 2, y0 + H / 2 - 12, 0xFFFFFFFF);
            int bw = 120, prog = (24 - bootTicks) * bw / 24;
            g.fill(x0 + W / 2 - bw / 2, y0 + H / 2 + 4, x0 + W / 2 + bw / 2, y0 + H / 2 + 8, 0xFF333333);
            g.fill(x0 + W / 2 - bw / 2, y0 + H / 2 + 4, x0 + W / 2 - bw / 2 + prog, y0 + H / 2 + 8, 0xFFE76F51);
            g.disableScissor();
            return;
        }
        wallpaper(g);
        if (app == null) desktop(g, mx, my);
        else window(g, mx, my, pt);
        taskbar(g);
        g.disableScissor();
        super.render(g, mx, my, pt);
        if (app != null) app.renderOver(g, mx, my, pt);
        if (ClientCall.ringing()) PhoneHud.ringCard(g, font, width - 156, 6);
    }

    private void wallpaper(GuiGraphics g) {
        int dh = H - BAR;
        g.fillGradient(x0, y0, x0 + W, y0 + dh, 0xFF1D3557, 0xFFE76F51);
        int[] bx = {12, 34, 48, 70, 96, 120, 150, 172, 200, 228, 250, 276, 300, 318};
        int[] bh = {30, 52, 40, 64, 36, 80, 46, 58, 34, 70, 42, 54, 38, 28};
        for (int i = 0; i < bx.length; i++) {
            int bw = 16 + (i * 7) % 12;
            g.fill(x0 + bx[i], y0 + dh - bh[i], x0 + bx[i] + bw, y0 + dh, 0xCC14213D);
            for (int wy = y0 + dh - bh[i] + 4; wy < y0 + dh - 4; wy += 7) for (int wx = x0 + bx[i] + 3; wx < x0 + bx[i] + bw - 3; wx += 5) if (((wx * 7 + wy * 13) % 5) < 2) g.fill(wx, wy, wx + 2, wy + 3, 0xFFFFD166);
        }
        g.fill(x0 + 250, y0 + 26, x0 + 300, y0 + 34, 0x88FFFFFF);
        g.fill(x0 + 244, y0 + 34, x0 + 306, y0 + 44, 0x77FFFFFF);
    }

    static final int IW = 52, IH = 44;

    private int iconX(int i) {
        return x0 + 10 + (i % 5) * (IW + 12);
    }

    private int iconY(int i) {
        return y0 + 10 + (i / 5) * (IH + 8);
    }

    private void desktop(GuiGraphics g, int mx, int my) {
        for (int i = 0; i < icons.size(); i++) {
            Icon ic = icons.get(i);
            int ix = iconX(i), iy = iconY(i);
            boolean hover = mx >= ix && mx < ix + IW && my >= iy && my < iy + IH;
            if (hover || i == sel) g.fill(ix, iy, ix + IW, iy + IH, 0x44FFFFFF);
            int cx = ix + IW / 2;
            if (!AppIcons.draw(g, ic.label, cx - 12, iy + 3, 24)) {
                g.fill(cx - 12, iy + 3, cx + 12, iy + 27, 0xFF000000 | (ic.color & 0xFFFFFF));
                g.fill(cx - 12, iy + 3, cx + 12, iy + 7, 0x33FFFFFF);
                g.drawCenteredString(font, "§l" + ic.glyph, cx, iy + 11, 0xFFFFFFFF);
            }
            g.drawCenteredString(font, ic.label, cx, iy + 31, 0xFFFFFFFF);
            int unread = unreadTotal();
            if (i == 1 && unread > 0) {
                g.fill(cx + 8, iy + 1, cx + 17, iy + 10, 0xFFE63946);
                g.drawCenteredString(font, String.valueOf(Math.min(9, unread)), cx + 13, iy + 2, 0xFFFFFFFF);
            }
        }
        String hello = "Hi, " + data.player + (data.kiosk ? " §7(SolTech store terminal)" : data.owner.isEmpty() ? "" : " §7(this is " + data.owner + "'s PC)");
        g.drawString(font, hello, x0 + 10, y0 + H - BAR - 22, 0xFFFFFFFF, true);
        g.drawString(font, "§7Arrow keys + Enter, or click an icon. Esc closes.", x0 + 10, y0 + H - BAR - 11, 0xFFFFFFFF, true);
    }

    private void window(GuiGraphics g, int mx, int my, float pt) {
        int wx = x0 + 3, wy = y0 + 3, ww = W - 6, wh = H - BAR - 6;
        g.fill(wx - 1, wy - 1, wx + ww + 1, wy + wh + 1, 0xFF0B0B0F);
        g.fill(wx, wy, wx + ww, wy + 14, app.color());
        g.drawString(font, "§l" + app.title(), wx + 5, wy + 3, 0xFFFFFFFF, false);
        boolean hx = mx >= wx + ww - 14 && mx < wx + ww && my >= wy && my < wy + 14;
        g.fill(wx + ww - 14, wy, wx + ww, wy + 14, hx ? 0xFFE63946 : 0x33000000);
        g.drawCenteredString(font, "×", wx + ww - 7, wy + 3, 0xFFFFFFFF);
        g.fill(wx, wy + 14, wx + ww, wy + wh, 0xFFF4F4F6);
        try {
            app.render(g, mx, my, pt);
        } catch (RuntimeException e) {
            com.fireheart.city.FireheartCity.LOG.warn("SolOS app failed to draw", e);
            showToast("That app crashed - closed it.");
            closeApp();
            return;
        }
    }

    private void taskbar(GuiGraphics g) {
        int ty = y0 + H - BAR;
        g.fill(x0, ty, x0 + W, y0 + H, 0xEE101216);
        g.fill(x0 + 2, ty + 2, x0 + 44, y0 + H - 2, 0xFFE76F51);
        String on = data.device == 3 ? "Games" : data.device == 4 ? "TV" : data.device == 2 ? "SolPad" : "SolOS";
        g.drawString(font, "§l" + on, x0 + 5, ty + 3, 0xFFFFFFFF, false);
        if (app != null) {
            g.fill(x0 + 48, ty + 2, x0 + 48 + font.width(app.title()) + 8, y0 + H - 2, 0x44FFFFFF);
            g.drawString(font, app.title(), x0 + 52, ty + 3, 0xFFFFFFFF, false);
        }
        String right = toastTicks > 0 ? "§e✉ " + toast : "§f" + data.clock;
        g.drawString(font, right, x0 + W - font.width(right.replaceAll("§.", "")) - 6, ty + 3, 0xFFFFFFFF, false);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        com.fireheart.city.client.DeviceAnim.activity(true);
        if (ClientCall.ringing() && com.fireheart.city.client.ClientSetup.PHONE_KEY.matches(key, scan)) {
            answerFromPc();
            return true;
        }
        if (bootTicks > 0) return true;
        if (key == App.ESC) {
            if (data.device == 4 && app != null && !app.back()) {
                onClose();
                return true;
            }
            if (app != null) {
                if (!app.back()) closeApp();
                return true;
            }
            onClose();
            return true;
        }
        if (app != null) {
            if (app.key(key)) return true;
            return super.keyPressed(key, scan, mods);
        }
        if (key == App.RIGHT) sel = (sel + 1) % icons.size();
        else if (key == App.LEFT) sel = (sel + icons.size() - 1) % icons.size();
        else if (key == App.DOWN) sel = Math.min(icons.size() - 1, sel + 5);
        else if (key == App.UP) sel = Math.max(0, sel - 5);
        else if (key == App.ENTER || key == App.KP_ENTER || key == App.SPACE) openApp(sel);
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
        if (ClientCall.ringing() && mx >= width - 156 && mx < width - 6 && my >= 6 && my < 36) {
            answerFromPc();
            return true;
        }
        if (bootTicks > 0) return true;
        if (app != null) {
            int wx = x0 + 3, wy = y0 + 3, ww = W - 6;
            if (mx >= wx + ww - 14 && mx < wx + ww && my >= wy && my < wy + 14) {
                closeApp();
                return true;
            }
            if (super.mouseClicked(mx, my, button)) return true;
            return app.click(mx, my, button);
        }
        for (int i = 0; i < icons.size(); i++) {
            int ix = iconX(i), iy = iconY(i);
            if (mx >= ix && mx < ix + IW && my >= iy && my < iy + IH) {
                openApp(i);
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

    private void answerFromPc() {
        PcNet.CHANNEL.sendToServer(new PcNet.Act(BlockPos.ZERO, "phone_open", "", ""));
        PcNet.CHANNEL.sendToServer(new PcNet.Act(BlockPos.ZERO, "answer", "", ""));
    }

    @Override
    public void removed() {
        if (app != null) app.close();
        send("close", "", "");
        super.removed();
    }
}
