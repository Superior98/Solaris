package com.fireheart.city.client.pc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;

/** Camera and album: the screen is a live viewfinder; shots are real photos of what you see, kept in your album. */
class CameraApp extends App {
    private final Btn btn = new Btn();
    private boolean roll, captioning, video, uploading;
    private int flash, picked = -1, scroll, timer;
    private String caption = "";

    public String title() { return captioning ? (uploading ? "Video title" : "Caption") : roll ? (picked >= 0 ? "Photo" : "Album") : ClientVideo.recording() ? "● REC " + ClientVideo.seconds() + "s" : "Camera"; }

    public int color() { return 0xFF212529; }

    public boolean viewfinder() {
        return !roll;
    }

    public boolean back() {
        if (captioning) { captioning = false; uploading = false; return true; }
        if (picked >= 0) { picked = -1; return true; }
        if (roll) { roll = false; return true; }
        return false;
    }

    public void tick() {
        if (flash > 0) flash--;
        if (timer > 0 && --timer == 0) fire();
    }

    static String[] entry(String s) {
        return s.split(":");
    }

    static String label(String[] p) {
        if (PhotoCache.isPhoto(p[0])) return p.length > 2 ? Pics.label(p[2]) : "Photo";
        return Pics.label(p[0]);
    }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        Font f = os.font();
        btn.clear();
        if (!roll) {
            int bar = 40;
            g.fill(x, y + h - bar, x + w, y + h, 0xFF000000);
            int cx = x + w / 2, cy = y + h - bar / 2;
            PhoneScreen.roundRect(g, cx - 13, cy - 13, cx + 13, cy + 13, 13, 0xFFFFFFFF);
            PhoneScreen.roundRect(g, cx - 10, cy - 10, cx + 10, cy + 10, 10, 0xFF000000);
            if (video) {
                if (ClientVideo.recording()) PhoneScreen.roundRect(g, cx - 6, cy - 6, cx + 6, cy + 6, 2, 0xFFFF3B30);
                else PhoneScreen.roundRect(g, cx - 9, cy - 9, cx + 9, cy + 9, 9, 0xFFFF3B30);
            } else PhoneScreen.roundRect(g, cx - 9, cy - 9, cx + 9, cy + 9, 9, timer > 0 ? 0xFFFF453A : 0xFFFFFFFF);
            btn.draw(g, f, cx - 13, cy - 13, 26, 26, "", 0x00000000, 0, mx, my, this::shoot);
            if (!ClientVideo.recording()) {
                btn.draw(g, f, x + w / 2 - 34, y + h - bar - 14, 32, 11, video ? "§7PHOTO" : "§ePHOTO", 0x88000000, 0xFFFFFFFF, mx, my, () -> video = false);
                btn.draw(g, f, x + w / 2 + 2, y + h - bar - 14, 32, 11, video ? "§eVIDEO" : "§7VIDEO", 0x88000000, 0xFFFFFFFF, mx, my, () -> video = true);
            } else {
                g.fill(x + 4, y + 4, x + 8, y + 8, (System.currentTimeMillis() / 400) % 2 == 0 ? 0xFFFF3B30 : 0x00000000);
                g.drawString(f, "REC " + ClientVideo.seconds() + "s / " + ClientVideo.maxSeconds() + "s", x + 11, y + 3, 0xFFFFFFFF, true);
            }
            String last = os.data.gallery.isEmpty() ? null : entry(os.data.gallery.get(0))[0];
            int tx = x + 10, ty = cy - 11;
            if (last != null) Pics.draw(g, last, tx, ty, 22, 22);
            else g.fill(tx, ty, tx + 22, ty + 22, 0xFF333333);
            btn.draw(g, f, tx, ty, 22, 22, "", 0x00000000, 0, mx, my, () -> roll = true);
            btn.draw(g, f, x + w - 32, cy - 7, 24, 14, timer > 0 ? String.valueOf(timer / 20 + 1) : "3s", 0xFF333333, 0xFFFFFFFF, mx, my, () -> { if (timer == 0) timer = 60; });
            g.fill(x + w / 3, y, x + w / 3 + 1, y + h - bar, 0x44FFFFFF);
            g.fill(x + 2 * w / 3, y, x + 2 * w / 3 + 1, y + h - bar, 0x44FFFFFF);
            g.fill(x, y + (h - bar) / 3, x + w, y + (h - bar) / 3 + 1, 0x44FFFFFF);
            g.fill(x, y + 2 * (h - bar) / 3, x + w, y + 2 * (h - bar) / 3 + 1, 0x44FFFFFF);
            if (timer > 0) g.drawCenteredString(f, "§l" + (timer / 20 + 1), x + w / 2, y + (h - bar) / 2 - 4, 0xFFFFFFFF);
            if (flash > 0) g.fill(x, y, x + w, y + h, (flash * 40 << 24) | 0xFFFFFF);
            return;
        }
        g.fill(x, y, x + w, y + h, 0xFF111111);
        if (picked >= 0 && picked < os.data.gallery.size()) {
            String[] p = entry(os.data.gallery.get(picked));
            int ph = w * 3 / 4;
            Pics.draw(g, p[0], x + 2, y + 6, w - 4, ph);
            int by = y + ph + 10;
            if (captioning) {
                PhoneScreen.roundRect(g, x + 4, by, x + w - 4, by + 30, 5, 0xFFFFFFFF);
                String shown = caption.isEmpty() ? "§7Write a caption..." : caption + ((System.currentTimeMillis() / 400) % 2 == 0 ? "_" : "");
                int ty = by + 4;
                for (var line : f.split(net.minecraft.network.chat.Component.literal(shown), w - 16)) {
                    g.drawString(f, line, x + 8, ty, 0xFF111111, false);
                    ty += 9;
                    if (ty > by + 24) break;
                }
                btn.draw(g, f, x + 6, by + 34, w - 12, 15, "Share", 0xFFFF6B6B, 0xFFFFFFFF, mx, my, () -> post(p));
                return;
            }
            g.drawCenteredString(f, label(p), x + w / 2, by, 0xFFFFFFFF);
            by += 12;
            int i = picked;
            if (PhotoCache.isVideo(p[0])) {
                btn.draw(g, f, x + 6, by, (w - 15) / 2, 14, "SolFeed", 0xFFFF6B6B, 0xFFFFFFFF, mx, my, () -> { captioning = true; uploading = false; caption = ""; });
                btn.draw(g, f, x + 9 + (w - 15) / 2, by, (w - 15) / 2, 14, "SolTube", 0xFFD62828, 0xFFFFFFFF, mx, my, () -> { captioning = true; uploading = true; caption = ""; });
            } else btn.draw(g, f, x + 6, by, w - 12, 14, "Post to SolFeed", 0xFFFF6B6B, 0xFFFFFFFF, mx, my, () -> { captioning = true; uploading = false; caption = ""; });
            btn.draw(g, f, x + 6, by + 17, (w - 15) / 2, 14, "Wallpaper", 0xFF3A86FF, 0xFFFFFFFF, mx, my, () -> {
                os.send("setting", "wall", p[0]);
                os.showToast("Wallpaper set!");
            });
            btn.draw(g, f, x + 9 + (w - 15) / 2, by + 17, (w - 15) / 2, 14, "Delete", 0xFF6C757D, 0xFFFFFFFF, mx, my, () -> {
                os.send("unsnap", String.valueOf(i), "");
                picked = -1;
            });
            return;
        }
        int cols = 3, cw = (w - 8) / cols;
        int yy = y + 4 - scroll;
        for (int i = 0; i < os.data.gallery.size(); i++) {
            String key = entry(os.data.gallery.get(i))[0];
            int tx = x + 4 + (i % cols) * cw, ty = yy + (i / cols) * cw;
            if (ty + cw < y || ty > y + h) continue;
            Pics.draw(g, key, tx + 1, ty + 1, cw - 2, cw - 2);
            int idx = i;
            btn.draw(g, f, tx, ty, cw, cw, "", 0x00000000, 0, mx, my, () -> picked = idx);
        }
        if (os.data.gallery.isEmpty()) g.drawCenteredString(f, "§7No photos yet.", x + w / 2, y + 30, 0xFFFFFFFF);
    }

    private void post(String[] p) {
        if (uploading) {
            os.send("tubeup", p[0], caption.trim().isEmpty() ? "My video at " + label(p) : caption.trim());
            captioning = false;
            uploading = false;
            caption = "";
            return;
        }
        String cap = caption.trim().isEmpty() ? label(p) : caption.trim();
        os.send("post", "[pic:" + p[0] + "] " + cap, "");
        os.showToast("Posted to SolFeed!");
        captioning = false;
        caption = "";
    }

    private void shoot() {
        if (video) {
            if (ClientVideo.recording()) { ClientVideo.stop(); flash = 3; }
            else if (ClientVideo.uploading()) { os.showToast("Still saving your last video..."); return; }
            else {
                if (Minecraft.getInstance().player != null) Minecraft.getInstance().player.playSound(SoundEvents.NOTE_BLOCK_HAT.value(), 0.6f, 1.2f);
                ClientVideo.startFree();
                return;
            }
            if (Minecraft.getInstance().player != null) Minecraft.getInstance().player.playSound(SoundEvents.NOTE_BLOCK_HAT.value(), 0.6f, ClientVideo.recording() ? 1.6f : 1.2f);
            return;
        }
        if (timer > 0) return;
        fire();
    }

    private void fire() {
        flash = 6;
        if (Minecraft.getInstance().player != null) Minecraft.getInstance().player.playSound(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 0.7f, 1.6f);
        ClientPhoto.request(false);
    }

    public boolean click(double mx, double my, int b) {
        return btn.click(mx, my);
    }

    public boolean typed(char c) {
        if (!captioning) return false;
        if (c >= ' ' && c != '|' && caption.length() < 120) caption += c;
        return true;
    }

    public boolean key(int k) {
        if (captioning) {
            if (k == BACKSPACE && !caption.isEmpty()) caption = caption.substring(0, caption.length() - 1);
            else if ((k == ENTER || k == KP_ENTER) && picked >= 0 && picked < os.data.gallery.size()) post(entry(os.data.gallery.get(picked)));
            else if (k == ESC) return false;
            return true;
        }
        if (!roll && (k == SPACE || k == ENTER)) { shoot(); return true; }
        return false;
    }

    public boolean scroll(double mx, double my, double d) {
        if (roll && picked < 0) scroll = Math.max(0, scroll - (int) (d * 16));
        return true;
    }
}
