package com.fireheart.city.client.pc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/** FirePhone settings: do not disturb, ringtone, case, battery and model info. */
class SettingsApp extends App {
    static final String[] RINGTONES = {"Bell", "Chime", "Pling", "Harp", "Flute"};
    static final String[] CASES = {"No case", "Clear", "Leather", "Sparkle", "Neon"};
    private final Btn btn = new Btn();

    public String title() { return "Settings"; }

    public int color() { return 0xFF6C757D; }

    static String[] parts(OsScreen os) {
        String[] p = os.data.settings.split("\\|");
        String[] def = {"0", "0", "0", "1", "0", "0", "1", "1"};
        for (int i = 0; i < Math.min(p.length, def.length); i++) if (!p[i].isEmpty()) def[i] = p[i];
        return def;
    }

    static int num(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    static SoundEvent ring(int i) {
        return switch (i) {
            case 1 -> SoundEvents.NOTE_BLOCK_CHIME.value();
            case 2 -> SoundEvents.NOTE_BLOCK_PLING.value();
            case 3 -> SoundEvents.NOTE_BLOCK_HARP.value();
            case 4 -> SoundEvents.NOTE_BLOCK_FLUTE.value();
            default -> SoundEvents.NOTE_BLOCK_BELL.value();
        };
    }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        Font f = os.font();
        btn.clear();
        String[] s = parts(os);
        boolean dnd = s[0].equals("1");
        int ring = Math.floorMod(num(s[1]), RINGTONES.length), cs = Math.floorMod(num(s[2]), CASES.length), model = Math.max(1, num(s[3]));
        g.fill(x, y, x + w, y + h, 0xFFF2F2F7);
        int yy = y + 6;
        section(g, f, yy, "SolPhone" + (model >= 2 ? " 2" : ""), "Battery " + os.data.battery + "%" + (os.data.charging ? " ⚡ charging" : ""));
        int bw = w - 24;
        g.fill(x + 12, yy + 22, x + 12 + bw, yy + 26, 0xFFDDDDDD);
        int fill = bw * Math.max(0, Math.min(100, os.data.battery)) / 100;
        g.fill(x + 12, yy + 22, x + 12 + fill, yy + 26, os.data.battery <= 20 ? 0xFFE63946 : 0xFF2DC653);
        yy += 36;
        row(g, f, yy, "Do Not Disturb", "");
        btn.draw(g, f, x + w - 40, yy + 3, 32, 13, dnd ? "ON" : "OFF", dnd ? 0xFF5E60CE : 0xFFADB5BD, 0xFFFFFFFF, mx, my, () -> os.send("setting", "dnd", dnd ? "0" : "1"));
        yy += 22;
        row(g, f, yy, "Ringtone", RINGTONES[ring]);
        btn.draw(g, f, x + w - 40, yy + 3, 32, 13, "▶", 0xFF3A86FF, 0xFFFFFFFF, mx, my, () -> {
            int n = (ring + 1) % RINGTONES.length;
            if (Minecraft.getInstance().player != null) Minecraft.getInstance().player.playSound(ring(n), 0.6f, 1.2f);
            os.send("setting", "ring", String.valueOf(n));
        });
        yy += 22;
        row(g, f, yy, "Case", CASES[cs]);
        btn.draw(g, f, x + w - 40, yy + 3, 32, 13, "›", 0xFF3A86FF, 0xFFFFFFFF, mx, my, () -> os.send("setting", "case", String.valueOf((cs + 1) % CASES.length)));
        yy += 22;
        String wall = s[4];
        row(g, f, yy, "Wallpaper", PhotoCache.isPhoto(wall) ? "Photo" : Wallpapers.NAMES[Math.floorMod(num(wall), Wallpapers.NAMES.length)]);
        btn.draw(g, f, x + w - 40, yy + 3, 32, 13, "›", 0xFF3A86FF, 0xFFFFFFFF, mx, my, () -> os.send("setting", "wall", String.valueOf(PhotoCache.isPhoto(wall) ? 0 : (num(wall) + 1) % Wallpapers.NAMES.length)));
        yy += 22;
        boolean h24 = s[5].equals("1");
        row(g, f, yy, "24-hour clock", "");
        btn.draw(g, f, x + w - 40, yy + 3, 32, 13, h24 ? "ON" : "OFF", h24 ? 0xFF2DC653 : 0xFFADB5BD, 0xFFFFFFFF, mx, my, () -> os.send("setting", "h24", h24 ? "0" : "1"));
        yy += 22;
        boolean prev = !s[6].equals("0");
        row(g, f, yy, "Message previews", "");
        btn.draw(g, f, x + w - 40, yy + 3, 32, 13, prev ? "ON" : "OFF", prev ? 0xFF2DC653 : 0xFFADB5BD, 0xFFFFFFFF, mx, my, () -> os.send("setting", "preview", prev ? "0" : "1"));
        yy += 22;
        boolean labels = !s[7].equals("0");
        row(g, f, yy, "App labels", "");
        btn.draw(g, f, x + w - 40, yy + 3, 32, 13, labels ? "ON" : "OFF", labels ? 0xFF2DC653 : 0xFFADB5BD, 0xFFFFFFFF, mx, my, () -> os.send("setting", "labels", labels ? "0" : "1"));
        PhoneHud.previews = prev;
    }

    private void section(GuiGraphics g, Font f, int yy, String a, String b) {
        PhoneScreen.roundRect(g, x + 4, yy, x + w - 4, yy + 30, 6, 0xFFFFFFFF);
        g.drawString(f, "§l" + a, x + 10, yy + 4, 0xFF111111, false);
        g.drawString(f, b, x + w - 10 - f.width(b), yy + 4, 0xFF666666, false);
    }

    private void row(GuiGraphics g, Font f, int yy, String a, String b) {
        PhoneScreen.roundRect(g, x + 4, yy, x + w - 4, yy + 19, 6, 0xFFFFFFFF);
        g.drawString(f, a, x + 10, yy + 6, 0xFF111111, false);
        if (!b.isEmpty()) g.drawString(f, "§8" + b, x + w - 46 - f.width(b), yy + 6, 0xFF666666, false);
    }

    public boolean click(double mx, double my, int b) {
        return btn.click(mx, my);
    }
}
