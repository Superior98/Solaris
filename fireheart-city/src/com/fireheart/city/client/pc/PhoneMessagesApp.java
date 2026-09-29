package com.fireheart.city.client.pc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Messages on the FirePhone: conversation list, then a chat thread. */
class PhoneMessagesApp extends App {
    private String open;
    private EditBox box;
    private int scroll, chatScroll;

    public String title() { return open == null ? "Messages" : os.residentName(open); }

    public int color() { return 0xFF3A86FF; }

    String openId() { return open; }

    public boolean back() {
        if (open == null) return false;
        open = null;
        os.clearWidgetsPublic();
        return true;
    }

    void onMessage() { chatScroll = 0; }

    public void addWidgets() {
        box = null;
        if (open == null) return;
        box = new EditBox(os.font(), x + 3, y + h - 17, w - 22, 14, Component.literal("msg"));
        box.setMaxLength(200);
        box.setHint(Component.literal("§7Text message"));
        os.widget(box);
    }

    private void go(String id) {
        open = id;
        os.markRead(id);
        chatScroll = 0;
        os.clearWidgetsPublic();
        addWidgets();
    }

    private List<String[]> threads() {
        Map<String, String[]> last = new LinkedHashMap<>();
        for (String l : os.data.inbox) {
            String[] p = l.split("\\|", 4);
            if (p.length == 4) { last.remove(p[0]); last.put(p[0], p); }
        }
        List<String[]> out = new ArrayList<>();
        List<String> keys = new ArrayList<>(last.keySet());
        for (int i = keys.size() - 1; i >= 0; i--) out.add(last.get(keys.get(i)));
        for (String[] r : os.data.residents) if (!last.containsKey(r[0])) out.add(new String[]{r[0], "", "", ""});
        return out;
    }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        Font f = os.font();
        g.fill(x, y, x + w, y + h, 0xFFFFFFFF);
        if (open == null) {
            g.enableScissor(x, y, x + w, y + h);
            int yy = y + 2 - scroll;
            for (String[] t : threads()) {
                boolean hover = mx >= x && mx < x + w && my >= yy && my < yy + 22;
                if (hover) g.fill(x, yy, x + w, yy + 22, 0xFFF1F3F5);
                String n = os.residentName(t[0]);
                if (!Faces.draw(g, os, t[0], x + 4, yy + 4, 14)) {
                    PhoneScreen.roundRect(g, x + 4, yy + 4, x + 18, yy + 18, 7, FeedApp.nameColor(t[0]));
                    g.drawCenteredString(f, t[0].startsWith("group:") ? "☺" : n.substring(0, 1), x + 11, yy + 7, 0xFFFFFFFF);
                }
                g.drawString(f, "§l" + n, x + 22, yy + 3, 0xFF111111, false);
                int un = os.unread(t[0]);
                if (un > 0) {
                    String c = String.valueOf(Math.min(99, un));
                    int bw = f.width(c) + 5;
                    g.fill(x + w - bw - 4, yy + 3, x + w - 4, yy + 12, 0xFF3A86FF);
                    g.drawString(f, c, x + w - bw - 1, yy + 4, 0xFFFFFFFF, false);
                }
                String body = Pics.scene(t[3]) != null ? "◉ Photo " + Pics.strip(t[3]) : t[3];
                String prev = t[3].isEmpty() ? "§7Say hi!" : (t[1].equals(">") ? "You: " : t[1].equals("~") ? "☎ " : "") + body;
                g.drawString(f, (un > 0 ? "§0" : "§8") + f.plainSubstrByWidth(prev, w - 26), x + 22, yy + 12, 0xFF666666, false);
                g.fill(x + 22, yy + 21, x + w, yy + 22, 0xFFEEEEEE);
                yy += 22;
            }
            g.disableScissor();
            return;
        }
        int bottom = y + h - 20;
        List<Object[]> rows = new ArrayList<>();
        for (String l : os.data.inbox) {
            String[] p = l.split("\\|", 4);
            if (p.length != 4 || !p[0].equals(open)) continue;
            if (p[1].equals("~")) { rows.add(new Object[]{Component.literal("☎ " + p[3]).getVisualOrderText(), null}); rows.add(null); continue; }
            boolean mine = p[1].equals(">");
            String sc = Pics.scene(p[3]);
            String txt = Pics.strip(p[3]);
            if (sc != null) {
                rows.add(new Object[]{sc, mine, "pic"});
                for (int k = 0; k < 4; k++) rows.add(new Object[]{null, mine, "gap"});
            }
            if (!txt.isEmpty()) for (FormattedCharSequence s : f.split(Component.literal(txt), w - 30)) rows.add(new Object[]{s, mine});
            rows.add(null);
        }
        Receipts.addRows(rows, open, os);
        int lines = (bottom - y - 4) / 10;
        int start = Math.max(0, rows.size() - lines - chatScroll);
        g.enableScissor(x, y, x + w, bottom);
        int yy = y + 3;
        for (int i = start; i < rows.size() && yy < bottom - 2; i++) {
            Object[] row = rows.get(i);
            if (row == null) { yy += 3; continue; }
            if (row.length > 2 && row[2].equals("status")) {
                FormattedCharSequence st = (FormattedCharSequence) row[0];
                g.pose().pushPose();
                g.pose().translate(x + w - 5 - f.width(st) * 0.75f, yy, 0);
                g.pose().scale(0.75f, 0.75f, 1);
                g.drawString(f, st, 0, 0, 0xFF999999, false);
                g.pose().popPose();
                yy += 8;
                continue;
            }
            if (row.length > 2 && row[2].equals("typing")) {
                Receipts.drawTyping(g, f, x + 7, yy, os.residentName(open));
                yy += 10;
                continue;
            }
            if (row.length > 2) {
                if (row[2].equals("pic")) {
                    int pw = 64, px = (Boolean) row[1] ? x + w - pw - 5 : x + 5;
                    Pics.draw(g, (String) row[0], px, yy, pw, 46);
                }
                yy += 10;
                continue;
            }
            FormattedCharSequence s = (FormattedCharSequence) row[0];
            int tw = f.width(s);
            if (row[1] == null) { g.drawString(f, s, x + w / 2 - tw / 2, yy, 0xFF999999, false); yy += 10; continue; }
            boolean mine = (Boolean) row[1];
            int bx = mine ? x + w - tw - 7 : x + 5;
            PhoneScreen.roundRect(g, bx - 4, yy - 1, bx + tw + 4, yy + 9, 4, mine ? 0xFF0A84FF : 0xFFE9E9EB);
            g.drawString(f, s, bx, yy, mine ? 0xFFFFFFFF : 0xFF111111, false);
            yy += 10;
        }
        if (rows.isEmpty()) g.drawCenteredString(f, "§8No messages yet.", x + w / 2, y + 20, 0xFF000000);
        g.disableScissor();
        PhoneScreen.roundRect(g, x + w - 17, y + h - 17, x + w - 3, y + h - 3, 7, 0xFF0A84FF);
        g.drawCenteredString(f, "◉", x + w - 10, y + h - 14, 0xFFFFFFFF);
    }

    public boolean click(double mx, double my, int b) {
        if (open != null) {
            if (mx >= x + w - 17 && mx < x + w - 3 && my >= y + h - 17 && my < y + h - 3) {
                os.send("msg", open, "[pic:here] " + (box == null ? "" : box.getValue().trim()));
                if (box != null) box.setValue("");
                return true;
            }
            return false;
        }
        int idx = (int) ((my - y - 2 + scroll) / 22);
        List<String[]> t = threads();
        if (idx >= 0 && idx < t.size()) {
            go(t.get(idx)[0]);
            return true;
        }
        return false;
    }

    public boolean scroll(double mx, double my, double d) {
        if (open == null) scroll = Math.max(0, Math.min(threads().size() * 22 - h + 6, scroll - (int) (d * 15)));
        else chatScroll = Math.max(0, chatScroll + (int) Math.signum(d));
        return true;
    }

    public boolean key(int k) {
        if (open != null && k == BACKSPACE && (box == null || box.getValue().isEmpty())) {
            open = null;
            os.clearWidgetsPublic();
            return true;
        }
        if ((k == ENTER || k == KP_ENTER) && box != null && open != null) {
            String t = box.getValue().trim();
            if (!t.isEmpty()) {
                os.send("msg", open, t);
                box.setValue("");
                Receipts.keystroke(os, open, true);
            }
            return true;
        }
        if (box != null && open != null) Receipts.keystroke(os, open, box.getValue().isEmpty());
        return false;
    }

    public boolean typed(char c) {
        if (box != null && !box.isFocused()) {
            box.setFocused(true);
            os.setFocused(box);
        }
        if (open != null) Receipts.keystroke(os, open, false);
        return false;
    }
}
