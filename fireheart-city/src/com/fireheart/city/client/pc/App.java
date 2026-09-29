package com.fireheart.city.client.pc;

import net.minecraft.client.gui.GuiGraphics;

/** One FireOS application running inside the ComputerScreen window. */
public abstract class App {
    protected OsScreen os;
    protected int x, y, w, h;

    public abstract String title();

    public int color() {
        return 0xFF3A6EA5;
    }

    public boolean game() {
        return false;
    }

    public void open(OsScreen os) {
        this.os = os;
    }

    public void layout(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public void addWidgets() {}

    public void addWidgetsRefresh() {}

    public void renderOver(GuiGraphics g, int mx, int my, float pt) {}

    public void tick() {}

    public abstract void render(GuiGraphics g, int mx, int my, float pt);

    public boolean key(int key) {
        return false;
    }

    public boolean typed(char c) {
        return false;
    }

    public boolean click(double mx, double my, int button) {
        return false;
    }

    public boolean scroll(double mx, double my, double delta) {
        return false;
    }

    public boolean drag(double mx, double my, int button, double dx, double dy) {
        return false;
    }

    public void close() {}

    public void onData() {}

    public boolean back() {
        return false;
    }

    public boolean compact() {
        return w < 250;
    }

    protected boolean inside(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public static final int LEFT = 263, RIGHT = 262, UP = 265, DOWN = 264, SPACE = 32, ENTER = 257, KP_ENTER = 335, ESC = 256, BACKSPACE = 259;
    public static final int KW = 87, KA = 65, KS = 83, KD = 68, KR = 82;

    protected static boolean left(int k) { return k == LEFT || k == KA; }
    protected static boolean right(int k) { return k == RIGHT || k == KD; }
    protected static boolean up(int k) { return k == UP || k == KW; }
    protected static boolean down(int k) { return k == DOWN || k == KS; }
}
