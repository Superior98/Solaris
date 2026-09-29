package com.fireheart.city.client;

import com.fireheart.city.BankNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The Fireheart City Bank window: account card, tabs for account, paying residents, lottery, loans and history. */
public class BankScreen extends Screen {
    private static final int W = 340, H = 222;
    private static final int GOLD = 0xFFE0B040, NAVY = 0xF0141B28, PANEL = 0xFF1E2738, LINE = 0xFF3A4660, TEXT = 0xFFE8E8F0, MUTED = 0xFF9AA3B8, GREEN = 0xFF6EE08A, RED = 0xFFFF7A7A;
    private static final String[] TABS = {"Account", "Pay a resident", "Lottery", "Loans", "History"};
    private BankNet.State s;
    private int tab;
    private int page;
    private String amount = "10";
    private String notice = "";
    private int noticeTicks;
    private int x0, y0;

    public BankScreen(BankNet.State s) {
        super(Component.literal("Solaris City Bank"));
        this.s = s;
        this.notice = s.notice;
        this.noticeTicks = s.notice.isEmpty() ? 0 : 200;
    }

    public void update(BankNet.State st) {
        this.s = st;
        if (!st.notice.isEmpty()) {
            notice = st.notice;
            noticeTicks = 200;
        }
        rebuildWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void send(String kind, String a, String b) {
        BankNet.CHANNEL.sendToServer(new BankNet.Action(kind, a, b));
    }

    private Button btn(String label, int x, int y, int w, Runnable r) {
        Button b = Button.builder(Component.literal(label), bb -> r.run()).bounds(x, y, w, 18).build();
        addRenderableWidget(b);
        return b;
    }

    private EditBox amountBox(int x, int y, int w) {
        EditBox e = new EditBox(font, x, y, w, 18, Component.literal("Amount"));
        e.setMaxLength(5);
        e.setFilter(v -> v.matches("\\d{0,5}"));
        e.setValue(amount);
        e.setResponder(v -> amount = v);
        addRenderableWidget(e);
        return e;
    }

    private String amt() {
        return amount.isEmpty() ? "0" : amount;
    }

    @Override
    protected void init() {
        x0 = (width - W) / 2;
        y0 = (height - H) / 2;
        btn("x", x0 + W - 20, y0 + 5, 14, this::onClose);
        if (!s.has) {
            int cx = x0 + 150, cy = y0 + 110;
            Button open = btn(s.teller ? "Open an account" : "See the teller inside", cx, cy, 170, () -> send("open", "", ""));
            open.active = s.teller && s.open;
            return;
        }
        for (int i = 0; i < TABS.length; i++) {
            int t = i;
            Button b = btn((tab == i ? "» " : "") + TABS[i], x0 + 10, y0 + 118 + i * 19, 124, () -> { tab = t; page = 0; rebuildWidgets(); });
            b.active = tab != i;
        }
        int rx = x0 + 146, ry = y0 + 36;
        switch (tab) {
            case 0 -> {
                amountBox(rx, ry + 22, 60);
                btn("10", rx + 64, ry + 22, 28, () -> { amount = "10"; rebuildWidgets(); });
                btn("50", rx + 94, ry + 22, 28, () -> { amount = "50"; rebuildWidgets(); });
                btn("100", rx + 124, ry + 22, 32, () -> { amount = "100"; rebuildWidgets(); });
                btn("Deposit", rx, ry + 48, 88, () -> send("deposit", amt(), ""));
                btn("Withdraw", rx + 92, ry + 48, 88, () -> send("withdraw", amt(), ""));
                Button all = btn("Deposit all my gold (" + s.cash + ")", rx, ry + 72, 180, () -> send("deposit", "all", ""));
                all.active = s.cash > 0;
                Button out = btn("Withdraw everything (" + s.balance + ")", rx, ry + 94, 180, () -> send("withdraw", "all", ""));
                out.active = s.balance > 0;
            }
            case 1 -> {
                amountBox(rx + 40, ry, 50);
                int per = 6, pages = Math.max(1, (s.residents.size() + per - 1) / per);
                page = Math.min(page, pages - 1);
                for (int i = 0; i < per; i++) {
                    int idx = page * per + i;
                    if (idx >= s.residents.size()) break;
                    String[] r = s.residents.get(idx);
                    Button b = btn("Pay", rx + 146, ry + 24 + i * 20, 36, () -> send("pay", r[0], amt()));
                    b.active = s.balance > 0;
                }
                Button prev = btn("<", rx + 100, ry, 20, () -> { page--; rebuildWidgets(); });
                prev.active = page > 0;
                Button next = btn(">", rx + 162, ry, 20, () -> { page++; rebuildWidgets(); });
                next.active = page < pages - 1;
            }
            case 2 -> {
                Button b1 = btn("Buy 1 ticket (" + s.price + ")", rx, ry + 92, 88, () -> send("ticket", "1", ""));
                Button b5 = btn("Buy 5 (" + 5 * s.price + ")", rx + 92, ry + 92, 88, () -> send("ticket", "5", ""));
                b1.active = s.tickets < s.maxTickets && s.balance >= s.price;
                b5.active = s.tickets + 5 <= s.maxTickets && s.balance >= 5 * s.price;
            }
            case 3 -> {
                if (s.owed > 0) {
                    amountBox(rx, ry + 62, 60);
                    btn("Repay amount", rx + 64, ry + 62, 116, () -> send("repay", amt(), ""));
                    Button all = btn("Repay it all (" + s.owed + ")", rx, ry + 86, 180, () -> send("repay", "all", ""));
                    all.active = s.balance >= s.owed;
                } else {
                    int[] opts = {50, 100, 250};
                    for (int i = 0; i < opts.length; i++) {
                        int o = opts[i];
                        int back = o + (o * s.loanRate + 99) / 100;
                        Button b = btn("Borrow " + o + "  (repay " + back + ")", rx, ry + 50 + i * 22, 180, () -> send("loan", String.valueOf(o), ""));
                        b.active = s.teller && s.open;
                    }
                }
            }
            default -> {}
        }
    }

    @Override
    public void tick() {
        if (noticeTicks > 0) noticeTicks--;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(x0 - 1, y0 - 1, x0 + W + 1, y0 + H + 1, GOLD);
        g.fill(x0, y0, x0 + W, y0 + H, NAVY);
        g.fillGradient(x0, y0, x0 + W, y0 + 26, 0xFF2C3A55, 0xFF1B2437);
        g.fill(x0, y0 + 26, x0 + W, y0 + 27, GOLD);
        g.drawString(font, "§l⛃ FIREHEART CITY BANK", x0 + 10, y0 + 9, GOLD, true);
        String where = s.teller ? (s.open ? "Teller window · Hugo" : "Teller closed") : "Cash machine · 24/7";
        g.drawString(font, where, x0 + W - 26 - font.width(where), y0 + 9, MUTED, false);

        int cx = x0 + 10, cy = y0 + 36;
        g.fillGradient(cx, cy, cx + 124, cy + 74, 0xFFF1D27A, 0xFFB98A2E);
        g.fill(cx, cy + 16, cx + 124, cy + 17, 0x55000000);
        g.drawString(font, "FIREHEART BANK", cx + 6, cy + 5, 0xFF4A3510, false);
        g.fill(cx + 100, cy + 4, cx + 116, cy + 14, 0xFFD9B35A);
        g.fill(cx + 102, cy + 6, cx + 114, cy + 12, 0xFFB8903C);
        if (s.has) {
            g.drawString(font, "FH-" + String.format("%04d", s.number), cx + 6, cy + 21, 0xFF3A2A0C, false);
            g.drawString(font, "BALANCE", cx + 6, cy + 35, 0xFF6A5020, false);
            g.pose().pushPose();
            g.pose().scale(2f, 2f, 1f);
            g.drawString(font, String.valueOf(s.balance), (cx + 6) / 2, (cy + 45) / 2, 0xFF2A1C05, false);
            g.pose().popPose();
            g.drawString(font, "coins", cx + 10 + font.width(String.valueOf(s.balance)) * 2, cy + 52, 0xFF6A5020, false);
            g.drawString(font, s.name, cx + 6, cy + 64, 0xFF3A2A0C, false);
        } else {
            g.drawString(font, "NO ACCOUNT", cx + 6, cy + 35, 0xFF6A5020, false);
        }

        int rx = x0 + 146, ry = y0 + 36;
        g.fill(rx - 6, y0 + 32, rx - 5, y0 + H - 22, LINE);
        if (!s.has) {
            g.drawString(font, "§lWelcome!", rx, ry, TEXT, false);
            drawWrapped(g, s.teller ? "Open a free savings account with Hugo. Your savings earn " + s.rate + "% interest every Monday, and you get a bank card." : "You don't have an account yet. Please see Hugo at the teller window inside (weekdays, work hours).", rx, ry + 16, 184, MUTED);
        } else switch (tab) {
            case 0 -> {
                g.drawString(font, "§lDeposit or withdraw", rx, ry, TEXT, false);
                g.drawString(font, "Amount (coins):", rx, ry + 12, MUTED, false);
                String carry = "Gold on you: §e" + s.cash;
                g.drawString(font, carry, rx + 184 - font.width(carry.replace("§e", "")), ry + 12, MUTED, false);
                drawWrapped(g, "1 gold nugget = 1 coin · ingot = 9 · block = 81. Savings earn " + s.rate + "% every Monday.", rx, ry + 122, 184, MUTED);
            }
            case 1 -> {
                g.drawString(font, "§lPay", rx, ry + 5, TEXT, false);
                int per = 6;
                for (int i = 0; i < per; i++) {
                    int idx = page * per + i;
                    if (idx >= s.residents.size()) break;
                    String[] r = s.residents.get(idx);
                    int y = ry + 24 + i * 20;
                    g.fill(rx - 2, y - 1, rx + 142, y + 19, i % 2 == 0 ? 0x33FFFFFF : 0x18FFFFFF);
                    g.drawString(font, r[1], rx + 2, y + 1, TEXT, false);
                    g.drawString(font, r[2], rx + 2, y + 10, MUTED, false);
                }
                int pages = Math.max(1, (s.residents.size() + per - 1) / per);
                String pg = (page + 1) + "/" + pages;
                g.drawString(font, pg, rx + 141 - font.width(pg) / 2, ry + 5, MUTED, false);
                g.drawString(font, "Paid from your savings account.", rx, ry + 148, MUTED, false);
            }
            case 2 -> {
                g.drawString(font, "§lSolaris Lottery", rx, ry, TEXT, false);
                g.drawString(font, "JACKPOT", rx, ry + 16, MUTED, false);
                g.pose().pushPose();
                g.pose().scale(2.5f, 2.5f, 1f);
                g.drawString(font, s.pot + "", (int) (rx / 2.5f), (int) ((ry + 27) / 2.5f), GOLD, true);
                g.pose().popPose();
                g.drawString(font, "coins", rx + font.width(s.pot + "") * 5 / 2 + 4, ry + 38, GOLD, false);
                g.drawString(font, "Draw: Sunday evening at the plaza", rx, ry + 54, MUTED, false);
                g.drawString(font, "Your tickets this week: §f" + s.tickets + "§7 / " + s.maxTickets, rx, ry + 66, MUTED, false);
                if (!s.lastWinner.isEmpty()) g.drawString(font, "Last winner: §e" + s.lastWinner + "§7 (" + s.lastPrize + ")", rx, ry + 78, MUTED, false);
                g.drawString(font, "Tickets are paid from your savings.", rx, ry + 116, MUTED, false);
            }
            case 3 -> {
                g.drawString(font, "§lLoans", rx, ry, TEXT, false);
                if (s.owed > 0) {
                    g.drawString(font, "You owe §c" + s.owed + " coins", rx, ry + 16, TEXT, false);
                    g.drawString(font, s.perDay + " coins are taken from your savings", rx, ry + 28, MUTED, false);
                    g.drawString(font, "each workday at closing time." + (s.missed > 0 ? " §cMissed: " + s.missed : ""), rx, ry + 38, MUTED, false);
                } else {
                    drawWrapped(g, s.teller && s.open ? s.loanRate + "% interest, repaid over 5 workdays straight from your savings." : "Loans are arranged with Hugo at the teller window, weekdays during work hours.", rx, ry + 16, 184, MUTED);
                }
            }
            default -> {
                g.drawString(font, "§lRecent transactions", rx, ry, TEXT, false);
                if (s.statement.isEmpty()) g.drawString(font, "Nothing yet.", rx, ry + 16, MUTED, false);
                for (int i = 0; i < s.statement.size(); i++) {
                    String line = s.statement.get(i);
                    while (font.width(line) > 186 && line.length() > 4) line = line.substring(0, line.length() - 2);
                    if (!line.equals(s.statement.get(i))) line = line.substring(0, line.length() - 1) + "…";
                    g.drawString(font, line, rx, ry + 14 + i * 11, TEXT, false);
                }
            }
        }
        if (noticeTicks > 0 && !notice.isEmpty()) {
            String n = notice.replaceAll("§.", "");
            boolean bad = notice.startsWith("§c");
            int a = Math.min(255, noticeTicks * 8);
            int col = (a << 24) | ((bad ? RED : GREEN) & 0xFFFFFF);
            while (font.width(n) > W - 20 && n.length() > 4) n = n.substring(0, n.length() - 2);
            g.fill(x0 + 1, y0 + H - 18, x0 + W - 1, y0 + H - 1, 0xAA000000);
            g.drawCenteredString(font, n, x0 + W / 2, y0 + H - 13, col);
        }
        super.render(g, mx, my, pt);
    }

    private void drawWrapped(GuiGraphics g, String text, int x, int y, int w, int color) {
        int line = 0;
        for (var seq : font.split(Component.literal(text), w)) g.drawString(font, seq, x, y + line++ * 10, color, false);
    }
}
