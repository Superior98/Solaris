package com.fireheart.city.client.pc;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

class MessengerApp extends App {
    private int sel = -1, listScroll, chatScroll;
    private EditBox box;
    private static final int LW = 92;

    public String title() { return "SolNet Messages"; }

    public int color() { return 0xFF2A9D8F; }

    public void addWidgets() {
        box = new EditBox(os.font(), x + LW + 6, y + h - 18, w - LW - 12, 14, Component.literal("message"));
        box.setMaxLength(200);
        box.setHint(Component.literal("§7Type a message, press Enter"));
        os.widget(box);
        if (sel < 0 && !os.data.residents.isEmpty()) select(latestContact());
    }

    private int latestContact() {
        for (int i = os.data.inbox.size() - 1; i >= 0; i--) {
            String id = os.data.inbox.get(i).split("\\|", 4)[0];
            for (int r = 0; r < os.data.residents.size(); r++) if (os.data.residents.get(r)[0].equals(id)) return r;
        }
        return 0;
    }

    void onMessage() {
        chatScroll = 0;
    }

    private List<String[]> convo() {
        List<String[]> out = new ArrayList<>();
        if (sel < 0 || sel >= os.data.residents.size()) return out;
        String id = os.data.residents.get(sel)[0];
        for (String l : os.data.inbox) {
            String[] p = l.split("\\|", 4);
            if (p.length == 4 && p[0].equals(id)) out.add(p);
        }
        return out;
    }

    private int unreadFrom(String id) {
        return os.unread(id);
    }

    String selectedId() {
        return sel >= 0 && sel < os.data.residents.size() ? os.data.residents.get(sel)[0] : null;
    }

    private void select(int idx) {
        sel = idx;
        chatScroll = 0;
        os.markRead(selectedId());
    }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        var f = os.font();
        g.fill(x, y, x + LW, y + h, 0xFFE8F4F2);
        int per = h / 13;
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, os.data.residents.size() - per)));
        for (int i = 0; i < per && i + listScroll < os.data.residents.size(); i++) {
            int idx = i + listScroll;
            String[] r = os.data.residents.get(idx);
            int ry = y + i * 13;
            if (idx == sel) g.fill(x, ry, x + LW, ry + 13, 0xFF2A9D8F);
            else if (mx >= x && mx < x + LW && my >= ry && my < ry + 13) g.fill(x, ry, x + LW, ry + 13, 0x332A9D8F);
            g.fill(x + 3, ry + 4, x + 7, ry + 8, r[4].equals("1") ? 0xFF3A86FF : r[3].equals("1") ? 0xFF3CCB5A : 0xFFB0B0B0);
            int un = unreadFrom(r[0]);
            g.drawString(f, un > 0 ? "§l" + r[1] : r[1], x + 10, ry + 3, idx == sel ? 0xFFFFFFFF : 0xFF1F3B38, false);
            if (un > 0) {
                String n = String.valueOf(Math.min(99, un));
                int bw = f.width(n) + 4;
                g.fill(x + LW - bw - 2, ry + 2, x + LW - 2, ry + 11, 0xFFE63946);
                g.drawString(f, n, x + LW - bw, ry + 3, 0xFFFFFFFF, false);
            }
        }
        int cx = x + LW + 4, cw = w - LW - 8;
        if (sel >= 0 && sel < os.data.residents.size()) {
            String[] r = os.data.residents.get(sel);
            g.drawString(f, "§l" + r[1] + "§r §8" + r[2] + (r[4].equals("1") ? " §9● SolPhone" : r[3].equals("1") ? " §2● online" : " §7○ offline"), cx + 2, y + 2, 0xFF222222, false);
        }
        g.fill(cx, y + 12, cx + cw, y + h - 22, 0xFFFFFFFF);
        List<String[]> msgs = convo();
        List<Object[]> rows = new ArrayList<>();
        for (String[] m : msgs) {
            if (m[1].equals("~")) {
                rows.add(new Object[]{Component.literal("☎ " + m[3]).getVisualOrderText(), null});
                rows.add(null);
                continue;
            }
            boolean mine = m[1].equals(">");
            String sc = Pics.scene(m[3]);
            if (sc != null) {
                rows.add(new Object[]{sc, mine, "pic"});
                for (int k = 0; k < 4; k++) rows.add(new Object[]{null, mine, "gap"});
            }
            String txt = Pics.strip(m[3]);
            if (!txt.isEmpty()) for (FormattedCharSequence s : f.split(Component.literal(txt), cw - 40)) rows.add(new Object[]{s, mine});
            rows.add(null);
        }
        if (sel >= 0 && sel < os.data.residents.size()) Receipts.addRows(rows, os.data.residents.get(sel)[0], os);
        int lines = (h - 36) / 10;
        int start = Math.max(0, rows.size() - lines - chatScroll);
        g.enableScissor(cx, y + 12, cx + cw, y + h - 22);
        int yy = y + 14;
        for (int i = start; i < rows.size() && yy < y + h - 24; i++) {
            Object[] row = rows.get(i);
            if (row == null) { yy += 3; continue; }
            if (row.length > 2 && row[2].equals("status")) {
                FormattedCharSequence st = (FormattedCharSequence) row[0];
                g.drawString(f, st, cx + cw - 6 - f.width(st), yy, 0xFF999999, false);
                yy += 10;
                continue;
            }
            if (row.length > 2 && row[2].equals("typing")) {
                Receipts.drawTyping(g, f, cx + 6, yy, os.data.residents.get(sel)[1]);
                yy += 10;
                continue;
            }
            if (row.length > 2) {
                if (row[2].equals("pic")) Pics.draw(g, (String) row[0], (Boolean) row[1] ? cx + cw - 72 : cx + 4, yy, 68, 46);
                yy += 10;
                continue;
            }
            FormattedCharSequence s = (FormattedCharSequence) row[0];
            if (row[1] == null) {
                g.drawString(f, s, cx + cw / 2 - f.width(s) / 2, yy, 0xFF999999, false);
                yy += 10;
                continue;
            }
            boolean mine = (Boolean) row[1];
            int tw = f.width(s);
            int bx = mine ? cx + cw - tw - 8 : cx + 4;
            g.fill(bx - 2, yy - 1, bx + tw + 2, yy + 9, mine ? 0xFF3A86FF : 0xFFE4E6EB);
            g.drawString(f, s, bx, yy, mine ? 0xFFFFFFFF : 0xFF111111, false);
            yy += 10;
        }
        g.disableScissor();
        if (msgs.isEmpty()) g.drawString(f, "§8Say hi! They'll write back.", cx + 6, y + 20, 0xFF222222, false);
    }

    public boolean click(double mx, double my, int b) {
        if (mx >= x && mx < x + LW && my >= y && my < y + h) {
            int idx = (int) ((my - y) / 13) + listScroll;
            if (idx < os.data.residents.size()) select(idx);
            return true;
        }
        return false;
    }

    public boolean scroll(double mx, double my, double d) {
        if (mx < x + LW) listScroll -= (int) Math.signum(d);
        else chatScroll = Math.max(0, chatScroll + (int) Math.signum(d));
        return true;
    }

    public boolean key(int k) {
        if ((k == ENTER || k == KP_ENTER) && box != null && sel >= 0) {
            String t = box.getValue().trim();
            if (!t.isEmpty()) {
                os.send("msg", os.data.residents.get(sel)[0], t);
                box.setValue("");
            }
            return true;
        }
        if (box != null && box.isFocused()) return false;
        if (down(k)) { select(Math.min(os.data.residents.size() - 1, sel + 1)); return true; }
        if (up(k)) { select(Math.max(0, sel - 1)); return true; }
        return false;
    }

    public boolean typed(char c) {
        if (box != null && !box.isFocused()) {
            box.setFocused(true);
            os.setFocused(box);
        }
        if (sel >= 0 && sel < os.data.residents.size()) Receipts.keystroke(os, os.data.residents.get(sel)[0], false);
        return false;
    }
}
