package com.fireheart.city.client.pc;

import com.fireheart.city.FireheartCity;
import com.fireheart.city.PcNet;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** Downloads real photos from the server on demand and keeps them as textures. */
public final class PhotoCache {
    private PhotoCache() {}

    private static final Map<String, ResourceLocation> TEX = new HashMap<>();
    private static final Map<String, int[]> SIZE = new HashMap<>();
    private static final Map<String, byte[][]> PARTS = new HashMap<>();
    private static final Map<String, Long> ASKED = new HashMap<>();

    public static boolean isPhoto(String key) {
        return key != null && (key.startsWith("ph_") || key.startsWith("vd_"));
    }

    public static boolean isVideo(String key) {
        return key != null && key.startsWith("vd_");
    }

    static final int FW = 128, FH = 72, COLS = 8;

    public static int frames(String key) {
        int[] sz = SIZE.get(id(key));
        return sz == null ? 0 : (sz[1] / FH) * COLS;
    }

    /** Draws one frame of a recorded video (frames are packed 8 across in a sprite sheet). */
    public static boolean drawVideo(GuiGraphics g, String key, int tick, int x, int y, int w, int h) {
        String id = id(key);
        ResourceLocation loc = TEX.get(id);
        if (loc == null) return draw(g, key, x, y, w, h);
        int[] sz = SIZE.get(id);
        int n = Math.max(1, (sz[1] / FH) * COLS);
        int fr = Math.floorMod(tick / 3, n);
        g.blit(loc, x, y, w, h, (fr % COLS) * FW, (fr / COLS) * FH, FW, FH, sz[0], sz[1]);
        return true;
    }

    static String id(String key) {
        return key.substring(3);
    }

    public static void put(String id, NativeImage img) {
        ResourceLocation old = TEX.remove(id);
        if (old != null) Minecraft.getInstance().getTextureManager().release(old);
        ResourceLocation loc = new ResourceLocation(FireheartCity.MODID, "photo/" + id);
        Minecraft.getInstance().getTextureManager().register(loc, new DynamicTexture(img));
        TEX.put(id, loc);
        SIZE.put(id, new int[]{img.getWidth(), img.getHeight()});
    }

    public static void receive(PcNet.Blob m) {
        if (m.kind.equals("voice")) { com.fireheart.city.client.VoiceClient.receive(m); return; }
        if (!m.kind.equals("photo")) return;
        if (m.meta.equals("missing")) {
            ASKED.put(m.id, Long.MAX_VALUE);
            return;
        }
        byte[][] parts = PARTS.computeIfAbsent(m.id, k -> new byte[m.total][]);
        if (parts.length != m.total || m.index >= parts.length) { PARTS.remove(m.id); return; }
        parts[m.index] = m.bytes;
        for (byte[] b : parts) if (b == null) return;
        PARTS.remove(m.id);
        int n = 0;
        for (byte[] b : parts) n += b.length;
        byte[] all = new byte[n];
        int o = 0;
        for (byte[] b : parts) { System.arraycopy(b, 0, all, o, b.length); o += b.length; }
        try {
            put(m.id, NativeImage.read(all));
        } catch (Exception e) {
            FireheartCity.LOG.warn("Bad photo " + m.id, e);
            ASKED.put(m.id, Long.MAX_VALUE);
        }
    }

    /** Draws the photo cropped to fill the box; returns false while it is still downloading. */
    public static boolean draw(GuiGraphics g, String key, int x, int y, int w, int h) {
        String id = id(key);
        ResourceLocation loc = TEX.get(id);
        if (loc == null) {
            long now = System.currentTimeMillis();
            Long t = ASKED.get(id);
            if (t == null || t != Long.MAX_VALUE && now - t > 8000) {
                ASKED.put(id, now);
                PcNet.CHANNEL.sendToServer(new PcNet.BlobUp("get", id, "", 0, 1, new byte[0]));
            }
            g.fill(x, y, x + w, y + h, 0xFF2B2D42);
            boolean missing = t != null && t == Long.MAX_VALUE;
            int dots = (int) (now / 300 % 4);
            String s = missing ? "photo lost" : ".".repeat(dots);
            g.drawCenteredString(Minecraft.getInstance().font, s, x + w / 2, y + h / 2 - 4, 0xFF8D99AE);
            return false;
        }
        int[] sz = SIZE.get(id);
        if (isVideo(key)) return drawVideo(g, key, (int) (System.currentTimeMillis() / 50), x, y, w, h);
        float iw = sz[0], ih = sz[1];
        float scale = Math.max(w / iw, h / ih);
        float uw = w / scale, vh = h / scale;
        float u0 = (iw - uw) / 2, v0 = (ih - vh) / 2;
        g.blit(loc, x, y, w, h, u0, v0, (int) uw, (int) vh, sz[0], sz[1]);
        return true;
    }
}
