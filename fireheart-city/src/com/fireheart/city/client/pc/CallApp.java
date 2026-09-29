package com.fireheart.city.client.pc;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Phone calls: contacts, dialing, live calls with typed speech, voicemail. */
class CallApp extends App {
    private EditBox box;
    private int scroll;
    private final List<int[]> hits = new ArrayList<>();

    public String title() { return ClientCall.state.isEmpty() ? "Contacts" : "Call"; }

    public int color() { return 0xFF2DC653; }

    public void addWidgets() {
        box = null;
        if (ClientCall.state.equals("live") || ClientCall.state.equals("voicemail")) {
            box = new EditBox(os.font(), x + 4, y + h - 38, w - 8, 14, Component.literal("say"));
            box.setMaxLength(160);
            box.setHint(Component.literal(ClientCall.state.equals("voicemail") ? "§7Leave a message..." : "§7Say something... (Enter)"));
            os.widget(box);
            os.setFocused(box);
            box.setFocused(true);
        }
    }

    void refresh() {
        os.clearWidgetsPublic();
        addWidgets();
    }

    private List<String[]> contacts() {
        List<String[]> l = new ArrayList<>();
        for (String[] r : os.data.residents) if (r.length > 4 && r[4].equals("1")) l.add(r);
        for (String[] r : os.data.residents) if (r.length > 4 && !r[4].equals("1")) l.add(r);
        return l;
    }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        Font f = os.font();
        hits.clear();
        if (ClientCall.state.isEmpty()) {
            g.fill(x, y, x + w, y + h, 0xFFFFFFFF);
            int yy = y + 2 - scroll;
            g.enableScissor(x, y, x + w, y + h);
            for (String[] r : contacts()) {
                boolean has = r[4].equals("1");
                boolean hover = mx >= x && mx < x + w && my >= yy && my < yy + 20;
                if (hover) g.fill(x, yy, x + w, yy + 20, 0xFFF1F3F5);
                if (!Faces.draw(g, os, r[0], x + 4, yy + 3, 14)) {
                    PhoneScreen.roundRect(g, x + 4, yy + 3, x + 18, yy + 17, 7, FeedApp.nameColor(r[0]));
                    g.drawCenteredString(f, r[0].startsWith("group:") ? "☺" : r[1].substring(0, 1), x + 11, yy + 6, 0xFFFFFFFF);
                }
                g.drawString(f, r[1], x + 22, yy + 2, has ? 0xFF111111 : 0xFF999999, false);
                g.pose().pushPose();
                g.pose().translate(x + 22, yy + 12, 0);
                g.pose().scale(0.7f, 0.7f, 1);
                g.drawString(f, has ? r[2] : "no SolPhone yet", 0, 0, 0xFF777777, false);
                g.pose().popPose();
                g.fill(x + w - 20, yy + 4, x + w - 6, yy + 16, has ? 0xFF2DC653 : 0xFFDDDDDD);
                g.drawCenteredString(f, "☎", x + w - 13, yy + 6, 0xFFFFFFFF);
                hits.add(new int[]{0, contacts().indexOf(r), x, yy, x + w, yy + 20});
                yy += 21;
            }
            g.disableScissor();
            return;
        }
        g.fillGradient(x, y, x + w, y + h, 0xFF1B263B, 0xFF0D1B2A);
        int cx = x + w / 2;
        String n = ClientCall.name.isEmpty() ? os.residentName(ClientCall.id) : ClientCall.name;
        if (!Faces.draw(g, os, ClientCall.id, cx - 14, y + 8, 28)) {
            PhoneScreen.roundRect(g, cx - 14, y + 8, cx + 14, y + 36, 14, FeedApp.nameColor(ClientCall.id));
            g.pose().pushPose();
            g.pose().translate(cx, y + 15, 0);
            g.pose().scale(2, 2, 1);
            g.drawCenteredString(f, n.isEmpty() ? "?" : n.substring(0, 1), 0, 0, 0xFFFFFFFF);
            g.pose().popPose();
        } else if (ClientCall.state.equals("live") && ClientCall.talking > 0) {
            int ring = 2 + (ClientCall.ticks / 3) % 3;
            g.fill(cx - 14 - ring, y + 37 + 1, cx + 14 + ring, y + 38, 0xFF2DC653);
        }
        g.drawCenteredString(f, "§l" + n, cx, y + 40, 0xFFFFFFFF);
        String st = switch (ClientCall.state) {
            case "ring_out" -> "Calling" + ".".repeat(1 + ClientCall.ticks / 10 % 3);
            case "incoming" -> "Incoming call...";
            case "live" -> ClientCall.timer();
            case "voicemail" -> "Voicemail";
            default -> "Call ended";
        };
        g.drawCenteredString(f, "§7" + st, cx, y + 51, 0xFFFFFFFF);
        int top = y + 63, bottom = box != null ? y + h - 42 : y + h - 26;
        List<Object[]> rows = new ArrayList<>();
        for (String[] l : ClientCall.lines) {
            for (FormattedCharSequence s : f.split(Component.literal(l[1]), w - 30)) rows.add(new Object[]{s, l[0]});
            rows.add(null);
        }
        int lines = (bottom - top) / 10;
        int start = Math.max(0, rows.size() - lines);
        g.enableScissor(x, top, x + w, bottom);
        int yy = top;
        for (int i = start; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            if (row == null) { yy += 2; continue; }
            FormattedCharSequence s = (FormattedCharSequence) row[0];
            String who = (String) row[1];
            int tw = f.width(s);
            if (who.equals("sys")) g.drawString(f, s, cx - tw / 2, yy, 0xFFADB5BD, false);
            else {
                boolean me = who.equals("me");
                int bx = me ? x + w - tw - 8 : x + 6;
                g.fill(bx - 2, yy - 1, bx + tw + 2, yy + 9, me ? 0xFF3A86FF : 0xFF415A77);
                g.drawString(f, s, bx, yy, 0xFFFFFFFF, false);
            }
            yy += 10;
        }
        g.disableScissor();
        int by = y + h - 20;
        if (ClientCall.ringing()) {
            button(g, f, x + 8, by, w / 2 - 12, "Answer", 0xFF2DC653, 1);
            button(g, f, x + w / 2 + 4, by, w / 2 - 12, "Decline", 0xFFE63946, 2);
        } else if (ClientCall.active()) button(g, f, x + 20, by, w - 40, "Hang up", 0xFFE63946, 2);
        else button(g, f, x + 20, by, w - 40, "Back to contacts", 0xFF415A77, 3);
    }

    private void button(GuiGraphics g, Font f, int bx, int by, int bw, String label, int col, int id) {
        g.fill(bx, by, bx + bw, by + 16, col);
        g.drawCenteredString(f, label, bx + bw / 2, by + 4, 0xFFFFFFFF);
        hits.add(new int[]{id, 0, bx, by, bx + bw, by + 16});
    }

    public boolean click(double mx, double my, int b) {
        for (int[] hit : hits) {
            if (mx < hit[2] || mx >= hit[4] || my < hit[3] || my >= hit[5]) continue;
            switch (hit[0]) {
                case 0 -> {
                    String[] r = contacts().get(hit[1]);
                    ClientCall.dial(r[0], r[1]);
                    os.send("call", r[0], "");
                    refresh();
                }
                case 1 -> os.send("answer", "", "");
                case 2 -> {
                    os.send("hangup", "", "");
                    if (ClientCall.ringing() || ClientCall.state.equals("ring_out")) ClientCall.handle(new String[]{"#call", "end", ClientCall.id, "Call ended."});
                }
                case 3 -> {
                    ClientCall.state = "";
                    ClientCall.lines.clear();
                    refresh();
                }
                default -> {}
            }
            return true;
        }
        return false;
    }

    public boolean scroll(double mx, double my, double d) {
        if (ClientCall.state.isEmpty()) scroll = Math.max(0, Math.min(os.data.residents.size() * 21 - h + 10, scroll - (int) (d * 15)));
        return true;
    }

    public void tick() {
        boolean needBox = ClientCall.state.equals("live") || ClientCall.state.equals("voicemail");
        if (needBox != (box != null)) refresh();
    }

    public boolean key(int k) {
        if ((k == ENTER || k == KP_ENTER) && box != null) {
            String t = box.getValue().trim();
            if (!t.isEmpty()) {
                ClientCall.mine(t);
                os.send("say", t, "");
                box.setValue("");
            }
            return true;
        }
        if (ClientCall.ringing() && (k == ENTER || k == SPACE)) {
            os.send("answer", "", "");
            return true;
        }
        return false;
    }
}
