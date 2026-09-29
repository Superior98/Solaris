package com.fireheart.city.client.pc;

import java.util.Random;
import net.minecraft.client.gui.GuiGraphics;

abstract class GameApp extends App {
    protected final Random rnd = new Random();
    protected boolean over, started;
    protected int score;
    private boolean submitted;

    abstract String id();

    abstract void reset();

    abstract void step();

    abstract void draw(GuiGraphics g, int mx, int my, float pt);

    public boolean game() { return true; }

    public void open(OsScreen os) {
        super.open(os);
        reset();
    }

    public void tick() {
        if (started && !over) step();
    }

    protected void gameOver() {
        if (over) return;
        over = true;
        if (!submitted) {
            submitted = true;
            os.submitScore(id(), score);
        }
    }

    protected void restart() {
        over = false;
        started = true;
        submitted = false;
        score = 0;
        reset();
    }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(x, y, x + w, y + h, 0xFF101418);
        draw(g, mx, my, pt);
        var f = os.font();
        g.drawString(f, "Score §e" + score, x + 4, y + 3, 0xFFFFFFFF, false);
        int best = Math.max(os.best(id(), os.data.player), over ? score : 0);
        String b = "Best §b" + best;
        g.drawString(f, b, x + w - f.width(b.replaceAll("§.", "")) - 4, y + 3, 0xFFFFFFFF, false);
        if (!started || over) {
            int ow = Math.min(90, w / 2 - 2);
            g.fill(x + w / 2 - ow, y + h / 2 - 22, x + w / 2 + ow, y + h / 2 + 22, 0xDD000000);
            g.drawCenteredString(f, over ? "§c§lGAME OVER§r  score " + score : "§l" + title(), x + w / 2, y + h / 2 - 14, 0xFFFFFFFF);
            g.drawCenteredString(f, "§7" + help(), x + w / 2, y + h / 2 - 2, 0xFFFFFFFF);
            g.drawCenteredString(f, "§e" + (compact() ? "Space / click" : "Press Space to " + (over ? "play again" : "start")), x + w / 2, y + h / 2 + 10, 0xFFFFFFFF);
        }
    }

    String help() {
        return "Arrow keys / WASD";
    }

    public boolean key(int k) {
        if ((!started || over) && (k == SPACE || k == ENTER)) {
            restart();
            return true;
        }
        if (started && !over) return input(k);
        return false;
    }

    abstract boolean input(int k);

    public boolean click(double mx, double my, int b) {
        if (!started || over) {
            restart();
            return true;
        }
        return clickGame(mx, my, b);
    }

    boolean clickGame(double mx, double my, int b) {
        return false;
    }
}
