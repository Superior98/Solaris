package com.fireheart.city.client.pc;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Phone notifications and call alerts drawn on the HUD while no screen is open. */
public final class PhoneHud {
    private PhoneHud() {}

    private static final Deque<String[]> QUEUE = new ArrayDeque<>();
    private static String[] cur;
    private static int ticks;

    private static String lastMsg = "";
    private static int msgTicks;

    public static boolean previews = true;

    public static void toast(String title, String body) {
        if (!previews && !title.equals("Sky Ferry") && !title.equals("Bank")) body = "New message";
        if (body.contains("[pic:")) body = Pics.strip(body) + " (photo)";
        lastMsg = title + ": " + body;
        msgTicks = 400;
        QUEUE.addLast(new String[]{title, body});
        while (QUEUE.size() > 4) QUEUE.removeFirst();
    }

    public static void tick() {
        ClientCall.tick();
        if (msgTicks > 0) msgTicks--;
        if (ticks > 0) ticks--;
        if (ticks <= 0) {
            cur = QUEUE.pollFirst();
            if (cur != null) {
                ticks = 110;
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) mc.player.playSound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.value(), 0.35f, 1.8f);
            }
        }
    }

    public static void ringCard(GuiGraphics g, Font f, int x, int y) {
        int w = 150;
        int pulse = (ClientCall.ticks / 10) % 2 == 0 ? 0xF02DC653 : 0xF024A148;
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(x, y, x + w, y + 30, pulse);
        g.drawString(f, "§l☎ " + ClientCall.name, x + 6, y + 5, 0xFFFFFFFF, false);
        g.drawString(f, "is calling... press §e[P]§f to answer", x + 6, y + 17, 0xFFFFFFFF, false);
        g.pose().popPose();
    }

    static void watch(GuiGraphics g, Font f, Minecraft mc, int sw, int sh) {
        long t = Math.floorMod(mc.level.getDayTime() + 6000, 24000L);
        int hh = (int) (t / 1000), mm = (int) (t % 1000 * 60 / 1000);
        String clock = String.format("%02d:%02d", hh, mm);
        int x = 6, y = 10;
        PhoneScreen.roundRect(g, x + 8, y - 4, x + 32, y + 2, 2, 0xFF3A3A3C);
        PhoneScreen.roundRect(g, x + 8, y + 36, x + 32, y + 42, 2, 0xFF3A3A3C);
        PhoneScreen.roundRect(g, x, y, x + 40, y + 38, 9, 0xFF1C1C1E);
        PhoneScreen.roundRect(g, x + 2, y + 2, x + 38, y + 36, 8, 0xFF000000);
        g.drawCenteredString(f, "§l" + clock, x + 20, y + 8, 0xFFFFFFFF);
        boolean ring = ClientCall.ringing();
        String sub = ring ? "§a☎ " + ClientCall.name : msgTicks > 0 ? "§b✉ new" : "§7" + (mc.level.isRaining() ? "☂ rain" : "☀ clear");
        g.pose().pushPose();
        g.pose().translate(x + 20, y + 22, 0);
        g.pose().scale(0.7f, 0.7f, 1f);
        g.drawCenteredString(f, f.plainSubstrByWidth(sub, 52), 0, 0, 0xFFFFFFFF);
        g.pose().popPose();
        if (msgTicks > 0 && msgTicks % 40 < 20) g.fill(x + 33, y + 5, x + 36, y + 8, 0xFF0A84FF);
    }

    public static void render(GuiGraphics g, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen instanceof OsScreen) return;
        Font f = mc.font;
        if (mc.player != null && mc.level != null && mc.player.getInventory().contains(new net.minecraft.world.item.ItemStack(com.fireheart.city.FireheartCity.WATCH.get()))) watch(g, f, mc, sw, sh);
        int w = 150, x = sw - w - 6, y = 6;
        if (ClientCall.ringing()) {
            ringCard(g, f, x, y);
            y += 34;
        } else if (ClientCall.active()) {
            g.fill(x + w - 90, y, x + w, y + 13, 0xE02DC653);
            g.drawString(f, "☎ " + ClientCall.name + " " + ClientCall.timer(), x + w - 86, y + 3, 0xFFFFFFFF, false);
            y += 17;
        }
        if (cur != null && ticks > 0) {
            int tx = x + Math.max(0, w + 10 - (110 - ticks) * 16) + Math.max(0, w + 10 - ticks * 16);
            List<FormattedCharSequence> ls = f.split(Component.literal(cur[1]), w - 12);
            int n = Math.min(3, ls.size());
            int h = 16 + n * 10;
            g.fill(tx, y, tx + w, y + h, 0xF0F8F9FA);
            g.fill(tx, y, tx + 3, y + h, 0xFFFF6B6B);
            g.drawString(f, "§l" + f.plainSubstrByWidth(cur[0], w - 12), tx + 7, y + 4, 0xFF111111, false);
            for (int i = 0; i < n; i++) g.drawString(f, ls.get(i), tx + 7, y + 15 + i * 10, 0xFF333333, false);
        }
    }
}
