package com.fireheart.city.client.pc;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Live Sky Ferry status: where it is, who's waiting, who's flying. */
class FerryApp extends App {
    private int t, asked;
    private final Btn btn = new Btn();

    public boolean click(double mx, double my, int b) {
        return btn.click(mx, my);
    }

    public String title() { return "Sky Ferry"; }

    public int color() { return 0xFF0077B6; }

    public void tick() {
        t++;
        if (t % 100 == 0) os.send("refresh", "", "");
    }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        Font f = os.font();
        String[] p = os.data.ferry.split("\\|", 4);
        String status = p[0];
        int sh = Math.min(90, h / 2);
        g.fillGradient(x, y, x + w, y + sh, 0xFF48CAE4, 0xFFCAF0F8);
        int cityX = x + 14, cityY = y + sh - 12, isleX = x + w - 30, isleY = y + 16;
        g.fill(x, y + sh - 8, x + w / 2, y + sh, 0xFF6C757D);
        g.fill(isleX - 14, isleY + 6, isleX + 26, isleY + 14, 0xFF7209B7);
        g.fill(isleX - 8, isleY + 14, isleX + 20, isleY + 18, 0xFF5A189A);
        float k;
        if (status.contains("city pad")) k = 0;
        else if (status.contains("terminal")) k = 1;
        else if (status.contains("to Neon")) k = 0.2f + 0.6f * ((t % 120) / 120f);
        else if (status.contains("to the city")) k = 0.8f - 0.6f * ((t % 120) / 120f);
        else k = 0;
        int fx = (int) (cityX + (isleX - cityX) * k), fy = (int) (cityY - 10 + (isleY - cityY + 10) * k);
        g.fill(fx - 8, fy - 6, fx + 8, fy, 0xFFE63946);
        g.fill(fx - 8, fy - 4, fx + 8, fy - 3, 0xFFFFFFFF);
        g.fill(fx - 5, fy + 1, fx + 5, fy + 4, 0xFF7F5539);
        int yy = y + sh + 6;
        List<FormattedCharSequence> ls = f.split(Component.literal("§l" + status), w - 12);
        for (FormattedCharSequence s : ls) { g.drawString(f, s, x + 6, yy, 0xFF023E8A, false); yy += 10; }
        yy += 4;
        if (p.length >= 4) {
            g.drawString(f, "Waiting at the city pad: §l" + p[1], x + 6, yy, 0xFF333333, false);
            g.drawString(f, "Waiting on Neon Heights: §l" + p[2], x + 6, yy + 11, 0xFF333333, false);
            g.drawString(f, "Captain: §l" + p[3], x + 6, yy + 22, 0xFF333333, false);
        }
        btn.clear();
        if (os.phone()) {
            boolean cool = asked > 0 && t - asked < 400;
            btn.draw(g, f, x + 6, y + h - 32, w - 12, 16, cool ? "Ferry is on its way!" : "✈ Request pickup", cool ? 0xFF6C757D : 0xFF0077B6, 0xFFFFFFFF, mx, my, () -> {
                if (asked > 0 && t - asked < 400) return;
                asked = t;
                os.send("ferry_call", "", "");
            });
        }
        g.drawString(f, "§8Right-click the ferry to board.", x + 6, y + h - 11, 0xFF333333, false);
    }
}
