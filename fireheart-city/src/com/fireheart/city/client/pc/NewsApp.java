package com.fireheart.city.client.pc;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

class NewsApp extends App {
    private int scroll;

    public String title() { return "SolNet News"; }

    public int color() { return 0xFFE76F51; }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(x, y, x + w, y + 18, 0xFF264653);
        g.drawString(os.font(), (compact() ? "§lGAZETTE §r§7online" : "§lFIREHEART GAZETTE §r§7online edition"), x + 6, y + 5, 0xFFFFFFFF, false);
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String n : os.data.news) {
            lines.addAll(os.font().split(Component.literal("• " + n), w - 16));
            lines.add(FormattedCharSequence.EMPTY);
        }
        if (lines.isEmpty()) lines.add(Component.literal("No news yet.").getVisualOrderText());
        int per = (h - 24) / 10;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, lines.size() - per)));
        g.enableScissor(x, y + 20, x + w, y + h);
        for (int i = 0; i < per && i + scroll < lines.size(); i++) g.drawString(os.font(), lines.get(i + scroll), x + 8, y + 22 + i * 10, 0xFF222222, false);
        g.disableScissor();
        if (lines.size() > per) g.fill(x + w - 4, y + 20 + (h - 24) * scroll / lines.size(), x + w - 1, y + 20 + (h - 24) * (scroll + per) / lines.size(), 0xFF999999);
    }

    public boolean scroll(double mx, double my, double d) {
        scroll -= (int) Math.signum(d) * 3;
        return true;
    }

    public boolean key(int k) {
        if (down(k)) { scroll++; return true; }
        if (up(k)) { scroll--; return true; }
        return false;
    }
}
