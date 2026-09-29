package com.fireheart.city.client.pc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Client side of read receipts and typing indicators. */
final class Receipts {
    private Receipts() {}

    static final Map<String, Long> READ = new HashMap<>();
    static final Map<String, Long> TYPING = new HashMap<>();
    static long lastTypingSent;
    static String typingTo = "";

    static boolean handle(String line) {
        String[] p = line.split("\\|");
        if (line.startsWith("#rd|") && p.length >= 2) {
            READ.put(p[1], System.currentTimeMillis());
            return true;
        }
        if (line.startsWith("#ty|") && p.length >= 3) {
            if (p[2].equals("1")) TYPING.put(p[1], System.currentTimeMillis() + 9000);
            else TYPING.remove(p[1]);
            return true;
        }
        if (p.length >= 2 && !line.startsWith("#")) {
            if (p[1].equals("<")) TYPING.remove(p[0]);
            if (p[1].equals(">")) READ.remove(p[0]);
        }
        return false;
    }

    static boolean typing(String id) {
        Long t = TYPING.get(id);
        return t != null && System.currentTimeMillis() < t;
    }

    static String status(String id, List<String> inbox) {
        String last = null;
        for (String l : inbox) {
            String[] p = l.split("\\|", 4);
            if (p.length == 4 && p[0].equals(id) && !p[1].equals("~")) last = p[1];
        }
        if (!">".equals(last)) return null;
        Long r = READ.get(id);
        return r != null ? "§9✓✓ §7Read" : "§7✓ Delivered";
    }

    static void addRows(List<Object[]> rows, String id, OsScreen os) {
        String st = status(id, os.data.inbox);
        if (st != null) rows.add(new Object[]{Component.literal(st).getVisualOrderText(), null, "status"});
        if (typing(id)) {
            rows.add(new Object[]{null, false, "typing"});
            rows.add(new Object[]{null, false, "gap"});
        }
    }

    static void drawTyping(GuiGraphics g, Font f, int x, int y, String name) {
        PhoneScreen.roundRect(g, x - 2, y - 1, x + 26, y + 11, 5, 0xFFE9E9EB);
        long t = System.currentTimeMillis() / 180;
        for (int i = 0; i < 3; i++) {
            int up = (t % 4) == i ? 2 : 0;
            g.fill(x + 4 + i * 7, y + 4 - up, x + 7 + i * 7, y + 7 - up, 0xFF8E8E93);
        }
        g.drawString(f, "§8" + name + " is typing…", x + 30, y + 1, 0xFF888888, false);
    }

    static void keystroke(OsScreen os, String id, boolean empty) {
        if (id == null || !id.startsWith("player:")) return;
        long now = System.currentTimeMillis();
        if (empty) {
            if (!typingTo.isEmpty()) os.send("typing", typingTo, "0");
            typingTo = "";
            return;
        }
        if (!typingTo.equals(id) || now - lastTypingSent > 3000) {
            os.send("typing", id, "1");
            typingTo = id;
            lastTypingSent = now;
        }
    }
}
