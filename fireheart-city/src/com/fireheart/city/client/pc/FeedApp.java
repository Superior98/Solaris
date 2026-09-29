package com.fireheart.city.client.pc;

import com.fireheart.city.Phones;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** FireFeed: the city's social network. Residents post about their lives; you can post, like and comment. */
class FeedApp extends App {
    private EditBox box;
    private int scroll, replyTo = -1;
    private String replyName = "";
    private final List<int[]> hits = new ArrayList<>();
    private final List<Object[]> names = new ArrayList<>();
    private final Btn btn = new Btn();
    private String profile;

    public String title() { return profile == null ? "SolFeed" : os.residentName(profile); }

    public boolean back() {
        if (profile == null) return false;
        profile = null;
        scroll = 0;
        return true;
    }

    public int color() { return 0xFFFF6B6B; }

    public void addWidgets() {
        box = new EditBox(os.font(), x + 4, y + 3, w - 8, 14, Component.literal("post"));
        box.setMaxLength(200);
        hint();
        os.widget(box);
    }

    private void hint() {
        if (box != null) box.setHint(Component.literal(replyTo >= 0 ? "§7Reply to " + replyName + "..." : "§7What's happening? (Enter to post)"));
    }

    static int nameColor(String key) {
        if (key.startsWith("player:")) return 0xFF1D4ED8;
        int[] c = {0xFFC0392B, 0xFF8E44AD, 0xFF16A085, 0xFFD35400, 0xFF2C3E50, 0xFF27AE60, 0xFFB7950B, 0xFF1F618D};
        return c[Math.floorMod(key.hashCode(), c.length)];
    }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        Font f = os.font();
        hits.clear();
        names.clear();
        btn.clear();
        g.fill(x, y, x + w, y + h, 0xFFF0F2F5);
        g.fill(x, y, x + w, y + 20, 0xFFFFFFFF);
        int top = y + 22;
        if (profile != null) {
            String job = "";
            for (String[] r : os.data.residents) if (r[0].equals(profile)) job = r[2];
            boolean fol = os.data.follows.contains(profile);
            int posts = 0;
            for (String raw : os.data.feed) { String[] q = raw.split(Phones.SEP, -1); if (q.length > 2 && q[2].equals(profile)) posts++; }
            g.fill(x, top - 2, x + w, top + 34, 0xFFFFFFFF);
            String nm = os.residentName(profile);
            if (!Faces.draw(g, os, profile, x + 6, top + 2, 24)) {
                PhoneScreen.roundRect(g, x + 6, top + 2, x + 30, top + 26, 12, nameColor(profile));
                g.drawCenteredString(f, "§l" + nm.substring(0, 1), x + 18, top + 10, 0xFFFFFFFF);
            }
            g.drawString(f, "§l" + nm, x + 36, top + 2, 0xFF111111, false);
            g.drawString(f, "§8" + f.plainSubstrByWidth(job, w - 90), x + 36, top + 12, 0xFF666666, false);
            g.drawString(f, "§8" + posts + " posts", x + 36, top + 22, 0xFF666666, false);
            String pid = profile;
            btn.draw(g, f, x + w - 50, top + 4, 44, 14, fol ? "✓ Following" : "+ Follow", fol ? 0xFFADB5BD : 0xFFFF6B6B, 0xFFFFFFFF, mx, my, () -> os.send("follow", pid, ""));
            top += 36;
        }
        g.enableScissor(x, top, x + w, y + h);
        int yy = top + 2 - scroll;
        int cw = w - 8;
        for (String raw : os.data.feed) {
            String[] p = raw.split(Phones.SEP, -1);
            if (p.length < 8) continue;
            if (profile != null && !p[2].equals(profile)) continue;
            int id = parse(p[0]);
            String scene = Pics.scene(p[6]);
            List<FormattedCharSequence> body = f.split(Component.literal(Pics.strip(p[6])), cw - 10);
            int picH = scene == null ? 0 : (cw - 10) * 9 / 16 + 4;
            String[] comments = p[7].isEmpty() ? new String[0] : p[7].split(Phones.CSEP);
            List<FormattedCharSequence> cl = new ArrayList<>();
            int shown = Math.min(3, comments.length);
            for (int i = comments.length - shown; i < comments.length; i++) cl.addAll(f.split(Component.literal("§8" + comments[i]), cw - 14));
            int ch = 14 + body.size() * 10 + picH + 14 + (cl.isEmpty() ? 0 : cl.size() * 9 + 4);
            if (yy + ch > top - 40 && yy < y + h + 10) {
                g.fill(x + 4, yy, x + 4 + cw, yy + ch, 0xFFFFFFFF);
                if (!Faces.draw(g, os, p[2], x + 8, yy + 3, 10)) {
                    PhoneScreen.roundRect(g, x + 8, yy + 3, x + 18, yy + 13, 5, nameColor(p[2]));
                    g.drawString(f, p[1].substring(0, 1).toUpperCase(), x + 11, yy + 4, 0xFFFFFFFF, false);
                }
                g.drawString(f, "§l" + p[1], x + 21, yy + 4, nameColor(p[2]), false);
                if (!p[2].startsWith("player:")) {
                    names.add(new Object[]{p[2], x + 8, yy + 2, x + 21 + f.width(p[1]) + 2, yy + 14});
                    if (os.data.follows.contains(p[2])) g.drawString(f, "§b✓", x + 23 + f.width("§l" + p[1]), yy + 4, 0xFF00B4D8, false);
                }
                String when = p[3];
                if (!os.phone()) g.drawString(f, "§7" + when, x + 4 + cw - f.width(when) - 4, yy + 4, 0xFF888888, false);
                int ty = yy + 16;
                for (FormattedCharSequence s : body) { g.drawString(f, s, x + 9, ty, 0xFF1C1E21, false); ty += 10; }
                if (scene != null) {
                    Pics.draw(g, scene, x + 9, ty + 1, cw - 10, picH - 4);
                    ty += picH;
                }
                boolean liked = p[5].equals("1");
                String heart = (liked ? "§c♥ " : "§7♡ ") + p[4];
                g.drawString(f, heart, x + 9, ty + 2, 0xFF555555, false);
                hits.add(new int[]{0, id, x + 7, ty, x + 9 + f.width(heart.replaceAll("§.", "")) + 4, ty + 11});
                String rep = "§7Reply" + (comments.length > shown ? " (" + comments.length + ")" : "");
                int rx = x + 44;
                g.drawString(f, rep, rx, ty + 2, 0xFF555555, false);
                hits.add(new int[]{1, id, rx - 2, ty, rx + f.width(rep.replaceAll("§.", "")) + 2, ty + 11, raw.indexOf(Phones.SEP)});
                if (os.phone()) g.drawString(f, "§7" + when, x + 4 + cw - f.width(when) - 4, ty + 2, 0xFF888888, false);
                ty += 14;
                if (!cl.isEmpty()) {
                    g.fill(x + 8, ty - 2, x + cw, ty + cl.size() * 9 + 1, 0xFFF3F4F6);
                    for (FormattedCharSequence s : cl) { g.drawString(f, s, x + 11, ty, 0xFF444444, false); ty += 9; }
                }
                if (replyTo == id) g.fill(x + 4, yy, x + 6, yy + ch, 0xFFFF6B6B);
            }
            yy += ch + 4;
        }
        if (os.data.feed.isEmpty()) g.drawCenteredString(f, "§8No posts yet.", x + w / 2, top + 20, 0xFF222222);
        g.disableScissor();
        maxScroll = Math.max(0, yy + scroll - (y + h) + 4);
    }

    private int maxScroll;

    static int parse(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return -1; }
    }

    public boolean click(double mx, double my, int b) {
        if (my < y + 22) return false;
        if (btn.click(mx, my)) return true;
        for (Object[] n : names) {
            if (mx >= (Integer) n[1] && mx < (Integer) n[3] && my >= (Integer) n[2] && my < (Integer) n[4]) {
                profile = (String) n[0];
                scroll = 0;
                return true;
            }
        }
        for (int[] hit : hits) {
            if (mx < hit[2] || mx >= hit[4] || my < hit[3] || my >= hit[5]) continue;
            if (hit[0] == 0) os.send("like", String.valueOf(hit[1]), "");
            else {
                replyTo = replyTo == hit[1] ? -1 : hit[1];
                replyName = "post";
                for (String raw : os.data.feed) {
                    String[] p = raw.split(Phones.SEP, -1);
                    if (p.length > 1 && parse(p[0]) == hit[1]) replyName = p[1];
                }
                hint();
                if (box != null) { box.setFocused(true); os.setFocused(box); }
            }
            return true;
        }
        return false;
    }

    public boolean scroll(double mx, double my, double d) {
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (d * 18)));
        return true;
    }

    public boolean key(int k) {
        if ((k == ENTER || k == KP_ENTER) && box != null) {
            String t = box.getValue().trim();
            if (!t.isEmpty()) {
                if (replyTo >= 0) os.send("comment", String.valueOf(replyTo), t);
                else {
                    os.send("post", t, "");
                    scroll = 0;
                }
                box.setValue("");
                replyTo = -1;
                hint();
            }
            return true;
        }
        if (box != null && box.isFocused()) return false;
        if (down(k)) { scroll = Math.min(maxScroll, scroll + 15); return true; }
        if (up(k)) { scroll = Math.max(0, scroll - 15); return true; }
        return false;
    }

    public boolean typed(char c) {
        if (box != null && !box.isFocused()) {
            box.setFocused(true);
            os.setFocused(box);
        }
        return false;
    }
}
