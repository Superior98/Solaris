package com.fireheart.city.client.pc;

import net.minecraft.client.gui.GuiGraphics;

class BankApp extends App {
    private final Btn btn = new Btn();
    private int payIdx, amtIdx;
    static final int[] AMOUNTS = {5, 10, 20, 50};

    public boolean click(double mx, double my, int b) {
        return btn.click(mx, my);
    }
    public String title() { return "Solaris City Bank - Online"; }

    public int color() { return 0xFFB8860B; }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        var f = os.font();
        g.fill(x, y, x + w, y + h, 0xFFF4F4F6);
        g.fillGradient(x + 8, y + 8, x + Math.min(150, w - 8), y + 70, 0xFFF1D27A, 0xFFB98A2E);
        g.drawString(f, "FIREHEART BANK", x + 14, y + 13, 0xFF4A3510, false);
        if (os.data.hasAccount) {
            g.drawString(f, "Savings", x + 14, y + 30, 0xFF6A5020, false);
            g.pose().pushPose();
            g.pose().scale(2f, 2f, 1f);
            g.drawString(f, String.valueOf(os.data.savings), (x + 14) / 2, (y + 41) / 2, 0xFF2A1C05, false);
            g.pose().popPose();
            g.drawString(f, os.data.player, x + 14, y + 58, 0xFF3A2A0C, false);
        } else g.drawString(f, "No account yet", x + 14, y + 34, 0xFF6A5020, false);
        g.drawString(f, "Gold in your pockets: §6" + os.data.coins, x + 8, y + 78, 0xFF333333, false);
        btn.clear();
        if (os.data.hasAccount && os.data.residents.stream().anyMatch(r -> !r[0].startsWith("group:"))) {
            java.util.List<String[]> people = os.data.residents.stream().filter(r -> !r[0].startsWith("group:")).toList();
            payIdx = Math.floorMod(payIdx, people.size());
            String[] who = people.get(payIdx);
            int amt = AMOUNTS[Math.floorMod(amtIdx, AMOUNTS.length)];
            int py = compact() ? y + h - 36 : y + 118, px = x + 8, pw = compact() ? w - 16 : 148;
            PhoneScreen.roundRect(g, px - 2, py - 2, px + pw + 2, py + 32, 5, 0xFFFFFFFF);
            g.drawString(f, "§lSend money", px + 2, py + 1, 0xFF4A3510, false);
            btn.draw(g, f, px, py + 13, pw / 2 - 12, 14, f.plainSubstrByWidth(who[1], pw / 2 - 16), 0xFFE9C46A, 0xFF2A1C05, mx, my, () -> payIdx++);
            btn.draw(g, f, px + pw / 2 - 10, py + 13, 26, 14, amt + "", 0xFFE9C46A, 0xFF2A1C05, mx, my, () -> amtIdx++);
            btn.draw(g, f, px + pw / 2 + 20, py + 13, pw / 2 - 20, 14, "Send", 0xFF2DC653, 0xFFFFFFFF, mx, my, () -> os.send("pay", who[0], String.valueOf(amt)));
        }
        if (!compact()) {
            g.drawString(f, "§8Deposits and withdrawals: Hugo's", x + 8, y + 92, 0xFF333333, false);
            g.drawString(f, "§8counter or the cash machine.", x + 8, y + 102, 0xFF333333, false);
        }
        int rx = compact() ? x + 8 : x + 164, ry = compact() ? y + 92 : y + 8;
        int max = compact() ? Math.max(0, (h - 142) / 10) : 12;
        int cut = compact() ? 22 : 34;
        g.drawString(f, "§lRecent", rx, ry, 0xFF222222, false);
        int i = 0;
        for (String s : os.data.statement) {
            if (i >= max) break;
            String t = s.length() > cut ? s.substring(0, cut - 1) + "…" : s;
            int col = s.contains(" +") ? 0xFF2E7D32 : 0xFFC62828;
            g.drawString(f, t, rx, ry + 12 + i++ * 10, col, false);
        }
        if (os.data.statement.isEmpty()) g.drawString(f, "§8No transactions yet.", rx, ry + 12, 0xFF222222, false);
    }
}
