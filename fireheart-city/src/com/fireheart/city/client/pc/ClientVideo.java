package com.fireheart.city.client.pc;

import com.fireheart.city.FireheartCity;
import com.fireheart.city.PcNet;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Records a short clip of what the player sees (about 7 frames a second, up to 9 seconds) and uploads it. */
@Mod.EventBusSubscriber(modid = FireheartCity.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientVideo {
    private ClientVideo() {}

    static final int MAX = 160, EVERY = 3;
    static final List<NativeImage> FRAMES = new ArrayList<>();
    static final java.util.ArrayDeque<PcNet.BlobUp> QUEUE = new java.util.ArrayDeque<>();
    static boolean recording, grab;
    static int ticks;

    public static int maxSeconds() {
        return MAX * EVERY / 20;
    }

    public static boolean uploading() {
        return !QUEUE.isEmpty();
    }

    public static void startFree() {
        start();
        Minecraft.getInstance().setScreen(null);
    }

    public static void hud(net.minecraft.client.gui.GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (!recording && QUEUE.isEmpty()) return;
        var f = mc.font;
        if (recording) {
            boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
            int s = seconds();
            String t = String.format("REC  %d:%02d / 0:%02d", s / 60, s % 60, maxSeconds());
            int tw = f.width(t) + 18;
            g.fill(w / 2 - tw / 2, 6, w / 2 + tw / 2, 20, 0x99000000);
            if (blink) g.fill(w / 2 - tw / 2 + 5, 10, w / 2 - tw / 2 + 11, 16, 0xFFFF3B30);
            g.drawString(f, t, w / 2 - tw / 2 + 14, 9, 0xFFFFFFFF, false);
            String k = "Press " + com.fireheart.city.client.ClientSetup.PHONE_KEY.getTranslatedKeyMessage().getString() + " to stop";
            g.drawCenteredString(f, k, w / 2, 23, 0xFFCCCCCC);
            int c = 0x88FFFFFF, m = 12, l = 18;
            g.fill(m, m, m + l, m + 2, c); g.fill(m, m, m + 2, m + l, c);
            g.fill(w - m - l, m, w - m, m + 2, c); g.fill(w - m - 2, m, w - m, m + l, c);
            g.fill(m, h - m - 2, m + l, h - m, c); g.fill(m, h - m - l, m + 2, h - m, c);
            g.fill(w - m - l, h - m - 2, w - m, h - m, c); g.fill(w - m - 2, h - m - l, w - m, h - m, c);
            int bw = 100, prog = FRAMES.size() * bw / MAX;
            g.fill(w / 2 - bw / 2, 34, w / 2 + bw / 2, 36, 0x55FFFFFF);
            g.fill(w / 2 - bw / 2, 34, w / 2 - bw / 2 + prog, 36, 0xFFFF3B30);
        } else {
            g.drawCenteredString(f, "Saving video... " + QUEUE.size(), w / 2, 9, 0xFFFFD166);
        }
    }

    public static boolean recording() {
        return recording;
    }

    public static int seconds() {
        return ticks / 20;
    }

    public static void start() {
        clear();
        recording = true;
        ticks = 0;
    }

    public static void stop() {
        if (!recording) return;
        recording = false;
        if (FRAMES.size() < 4) { clear(); return; }
        try {
            while (FRAMES.size() % PhotoCache.COLS != 0) FRAMES.add(copy(FRAMES.get(FRAMES.size() - 1)));
            int rows = FRAMES.size() / PhotoCache.COLS;
            NativeImage sheet = new NativeImage(PhotoCache.FW * PhotoCache.COLS, PhotoCache.FH * rows, false);
            for (int i = 0; i < FRAMES.size(); i++) {
                NativeImage f = FRAMES.get(i);
                int ox = (i % PhotoCache.COLS) * PhotoCache.FW, oy = (i / PhotoCache.COLS) * PhotoCache.FH;
                for (int y = 0; y < PhotoCache.FH; y++) for (int x = 0; x < PhotoCache.FW; x++) sheet.setPixelRGBA(ox + x, oy + y, f.getPixelRGBA(x, y));
            }
            byte[] png = sheet.asByteArray();
            String id = com.fireheart.city.Photos.newId();
            PhotoCache.put(id, sheet);
            int chunk = 30000, total = Math.max(1, (png.length + chunk - 1) / chunk);
            for (int i = 0; i < total; i++) {
                int a = i * chunk, b = Math.min(png.length, a + chunk);
                byte[] part = new byte[b - a];
                System.arraycopy(png, a, part, 0, part.length);
                QUEUE.add(new PcNet.BlobUp("video", id, "", i, total, part));
            }
            if (Minecraft.getInstance().player != null) Minecraft.getInstance().player.playSound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HAT.value(), 0.6f, 1.6f);
        } catch (Exception e) {
            FireheartCity.LOG.warn("Video save failed", e);
        }
        clear();
    }

    static NativeImage copy(NativeImage src) {
        NativeImage n = new NativeImage(src.getWidth(), src.getHeight(), false);
        n.copyFrom(src);
        return n;
    }

    static void clear() {
        for (NativeImage i : FRAMES) i.close();
        FRAMES.clear();
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        for (int i = 0; i < 4 && !QUEUE.isEmpty(); i++) {
            if (Minecraft.getInstance().getConnection() == null) { QUEUE.clear(); break; }
            PcNet.CHANNEL.sendToServer(QUEUE.poll());
        }
        if (!recording) return;
        if (Minecraft.getInstance().player == null) { recording = false; clear(); return; }
        ticks++;
        if (ticks % EVERY == 0) grab = true;
        if (FRAMES.size() >= MAX) stop();
    }

    @SubscribeEvent
    public static void onStage(RenderLevelStageEvent e) {
        if (!recording || !grab || e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        grab = false;
        try (NativeImage full = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
            FRAMES.add(scale(full));
        } catch (Exception ex) {
            recording = false;
        }
    }

    static NativeImage scale(NativeImage src) {
        int sw = src.getWidth(), sh = src.getHeight();
        int cw = sw, ch = sw * 9 / 16;
        if (ch > sh) { ch = sh; cw = sh * 16 / 9; }
        int ox = (sw - cw) / 2, oy = (sh - ch) / 2;
        NativeImage out = new NativeImage(PhotoCache.FW, PhotoCache.FH, false);
        for (int y = 0; y < PhotoCache.FH; y++) for (int x = 0; x < PhotoCache.FW; x++) {
            int c = src.getPixelRGBA(ox + x * cw / PhotoCache.FW, oy + y * ch / PhotoCache.FH);
            out.setPixelRGBA(x, y, c | 0xFF000000);
        }
        return out;
    }
}
