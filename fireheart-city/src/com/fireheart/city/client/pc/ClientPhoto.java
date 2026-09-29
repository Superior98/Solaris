package com.fireheart.city.client.pc;

import com.fireheart.city.FireheartCity;
import com.fireheart.city.PcNet;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Captures what the player sees (world only, no GUI) as a photo and uploads it. */
@Mod.EventBusSubscriber(modid = FireheartCity.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientPhoto {
    private ClientPhoto() {}

    static final int PW = 320, PH = 240;
    private static boolean pending;
    private static boolean selfie;

    public static void request(boolean front) {
        pending = true;
        selfie = front;
    }

    @SubscribeEvent
    public static void onStage(RenderLevelStageEvent e) {
        if (!pending || e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        pending = false;
        try (NativeImage full = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
            NativeImage out = crop(full);
            byte[] png = out.asByteArray();
            String id = com.fireheart.city.Photos.newId();
            PhotoCache.put(id, out);
            int chunk = 30000, total = Math.max(1, (png.length + chunk - 1) / chunk);
            for (int i = 0; i < total; i++) {
                int a = i * chunk, b = Math.min(png.length, a + chunk);
                byte[] part = new byte[b - a];
                System.arraycopy(png, a, part, 0, part.length);
                PcNet.CHANNEL.sendToServer(new PcNet.BlobUp("photo", id, selfie ? "selfie" : "", i, total, part));
            }
        } catch (Exception ex) {
            FireheartCity.LOG.warn("Photo capture failed", ex);
            PcNet.CHANNEL.sendToServer(new PcNet.Act(net.minecraft.core.BlockPos.ZERO, "snap", "", ""));
        }
    }

    static NativeImage crop(NativeImage src) {
        int sw = src.getWidth(), sh = src.getHeight();
        int cw = sw, ch = sw * PH / PW;
        if (ch > sh) { ch = sh; cw = sh * PW / PH; }
        int ox = (sw - cw) / 2, oy = (sh - ch) / 2;
        NativeImage out = new NativeImage(PW, PH, false);
        for (int y = 0; y < PH; y++) {
            for (int x = 0; x < PW; x++) {
                int x0 = ox + x * cw / PW, x1 = Math.max(x0 + 1, ox + (x + 1) * cw / PW);
                int y0 = oy + y * ch / PH, y1 = Math.max(y0 + 1, oy + (y + 1) * ch / PH);
                long r = 0, g = 0, b = 0;
                int n = 0;
                for (int yy = y0; yy < y1; yy += Math.max(1, (y1 - y0) / 3)) for (int xx = x0; xx < x1; xx += Math.max(1, (x1 - x0) / 3)) {
                    int c = src.getPixelRGBA(Math.min(sw - 1, xx), Math.min(sh - 1, yy));
                    r += c & 0xFF;
                    g += (c >> 8) & 0xFF;
                    b += (c >> 16) & 0xFF;
                    n++;
                }
                out.setPixelRGBA(x, y, 0xFF000000 | (int) (b / n) << 16 | (int) (g / n) << 8 | (int) (r / n));
            }
        }
        return out;
    }
}
