package com.fireheart.city.client.pc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * SolPlay Gen 2: the modern game engine for new hardware (SolPhone 2, PCs, SolPads and the SolBox). Adds a 3-2-1
 * countdown, pause (P), smooth frame interpolation, particles, screen shake, sound, persistent achievements and a
 * proper results screen on top of the classic GameApp.
 */
abstract class Gen2Game extends GameApp {
    protected int ticks, countdown, shakeT;
    protected float shake;
    protected boolean paused;
    protected final List<float[]> parts = new ArrayList<>();
    private String banner = "";
    private int bannerT;
    static Properties achievements;

    abstract int accent();

    abstract String[][] achievementList();

    void onStart() {}

    @Override
    protected void restart() {
        super.restart();
        countdown = 60;
        paused = false;
        parts.clear();
        ticks = 0;
        onStart();
    }

    @Override
    public void tick() {
        if (bannerT > 0) bannerT--;
        if (shakeT > 0) shakeT--;
        for (int i = parts.size() - 1; i >= 0; i--) {
            float[] p = parts.get(i);
            p[0] += p[2];
            p[1] += p[3];
            p[3] += p[6];
            p[4]--;
            if (p[4] <= 0) parts.remove(i);
        }
        if (!started || over || paused) return;
        if (countdown > 0) {
            if (countdown % 20 == 0) sfx(SoundEvents.NOTE_BLOCK_HAT.value(), countdown == 20 ? 2f : 1.4f);
            countdown--;
            if (countdown == 0) sfx(SoundEvents.NOTE_BLOCK_BELL.value(), 1.6f);
            return;
        }
        ticks++;
        step();
        checkAchievements();
    }

    void burst(float px, float py, int n, int color, float speed) {
        for (int i = 0; i < n && parts.size() < 400; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            float s = speed * (0.4f + rnd.nextFloat());
            parts.add(new float[]{px, py, (float) Math.cos(a) * s, (float) Math.sin(a) * s, 14 + rnd.nextInt(14), color, 0.08f});
        }
    }

    void shake(float amount, int t) {
        shake = Math.max(shake, amount);
        shakeT = t;
    }

    void sfx(SoundEvent e, float pitch) {
        var p = Minecraft.getInstance().player;
        if (p != null) p.playSound(e, 0.45f, pitch);
    }

    void drawParticles(GuiGraphics g) {
        for (float[] p : parts) {
            int a = (int) Mth.clamp(p[4] / 14f * 255, 0, 255);
            int c = ((int) p[5] & 0xFFFFFF) | (a << 24);
            int px = x + (int) p[0], py = y + (int) p[1];
            g.fill(px, py, px + 2, py + 2, c);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(x, y, x + w, y + h, 0xFF05060A);
        g.enableScissor(x, y, x + w, y + h);
        g.pose().pushPose();
        if (shakeT > 0) g.pose().translate((rnd.nextFloat() - 0.5f) * shake * shakeT / 10f, (rnd.nextFloat() - 0.5f) * shake * shakeT / 10f, 0);
        draw(g, mx, my, paused || countdown > 0 || over || !started ? 0 : pt);
        drawParticles(g);
        g.pose().popPose();
        g.disableScissor();
        Font f = os.font();
        g.fill(x, y, x + w, y + 12, 0x88000000);
        g.drawString(f, "§l" + title() + " §8· §7Gen 2", x + 4, y + 2, accent(), false);
        String sc = "§f" + score + "  §8best §b" + Math.max(os.best(id(), os.data.player), over ? score : 0);
        g.drawString(f, sc, x + w - f.width(sc.replaceAll("§.", "")) - 4, y + 2, 0xFFFFFFFF, false);
        if (started && !over && countdown > 0) {
            String n = countdown > 40 ? "3" : countdown > 20 ? "2" : "1";
            float k = (countdown % 20) / 20f;
            g.pose().pushPose();
            g.pose().translate(x + w / 2f, y + h / 2f, 0);
            g.pose().scale(2 + k * 2, 2 + k * 2, 1);
            g.drawCenteredString(f, n, 0, -4, ((int) (k * 255) << 24) | 0xFFFFFF);
            g.pose().popPose();
        }
        if (paused) {
            g.fill(x, y, x + w, y + h, 0xAA000000);
            g.drawCenteredString(f, "§l❚❚ PAUSED", x + w / 2, y + h / 2 - 10, 0xFFFFFFFF);
            g.drawCenteredString(f, "§7P resume · Backspace quit", x + w / 2, y + h / 2 + 4, 0xFFFFFFFF);
        }
        if (!started || over) {
            int ow = Math.min(120, w / 2 - 4);
            g.fillGradient(x + w / 2 - ow, y + h / 2 - 34, x + w / 2 + ow, y + h / 2 + 34, 0xEE0B0F1A, 0xEE141B2E);
            g.fill(x + w / 2 - ow, y + h / 2 - 34, x + w / 2 + ow, y + h / 2 - 33, accent());
            g.drawCenteredString(f, over ? "§c§lGAME OVER" : "§l" + title(), x + w / 2, y + h / 2 - 26, 0xFFFFFFFF);
            if (over) g.drawCenteredString(f, "§fScore §e" + score + (score >= os.best(id(), os.data.player) && score > 0 ? "  §a★ NEW BEST" : ""), x + w / 2, y + h / 2 - 14, 0xFFFFFFFF);
            else g.drawCenteredString(f, "§7" + help(), x + w / 2, y + h / 2 - 14, 0xFFFFFFFF);
            g.drawCenteredString(f, "§8" + unlockedCount() + "/" + achievementList().length + " achievements", x + w / 2, y + h / 2 + 2, 0xFFFFFFFF);
            g.drawCenteredString(f, "§e" + (compact() ? "Space / tap" : "Space to " + (over ? "play again" : "start")) + " §8· P pause", x + w / 2, y + h / 2 + 16, 0xFFFFFFFF);
        }
        if (bannerT > 0) {
            int bw = f.width(banner) + 20;
            int by = y + 16 + (bannerT > 70 ? (80 - bannerT) * 2 - 20 : 0);
            g.fill(x + w / 2 - bw / 2, by, x + w / 2 + bw / 2, by + 14, 0xE0101010);
            g.fill(x + w / 2 - bw / 2, by + 13, x + w / 2 + bw / 2, by + 14, 0xFFFFD166);
            g.drawCenteredString(f, banner, x + w / 2, by + 3, 0xFFFFD166);
        }
    }

    @Override
    public boolean key(int k) {
        if (k == 80 && started && !over) {
            paused = !paused;
            sfx(SoundEvents.UI_BUTTON_CLICK.value(), paused ? 0.8f : 1.2f);
            return true;
        }
        if (paused) return true;
        if (countdown > 0 && started && !over) return true;
        return super.key(k);
    }

    static boolean held(int... keys) {
        long win = Minecraft.getInstance().getWindow().getWindow();
        for (int k : keys) if (com.mojang.blaze3d.platform.InputConstants.isKeyDown(win, k)) return true;
        return Gamepad.held(keys);
    }

    static Properties store() {
        if (achievements != null) return achievements;
        achievements = new Properties();
        try {
            Path p = file();
            if (Files.exists(p)) try (var in = Files.newInputStream(p)) { achievements.load(in); }
        } catch (IOException ignored) {}
        return achievements;
    }

    static Path file() throws IOException {
        Path d = FMLPaths.CONFIGDIR.get().resolve("fireheartcity");
        Files.createDirectories(d);
        return d.resolve("solplay.properties");
    }

    int unlockedCount() {
        int n = 0;
        for (String[] a : achievementList()) if (store().containsKey(id() + "." + a[0])) n++;
        return n;
    }

    abstract boolean achieved(String key);

    void checkAchievements() {
        if (ticks % 10 != 0) return;
        for (String[] a : achievementList()) {
            String k = id() + "." + a[0];
            if (store().containsKey(k) || !achieved(a[0])) continue;
            store().setProperty(k, String.valueOf(System.currentTimeMillis()));
            try (var out = Files.newOutputStream(file())) { store().store(out, "SolPlay achievements"); } catch (IOException ignored) {}
            banner = "★ " + a[1];
            bannerT = 80;
            sfx(SoundEvents.PLAYER_LEVELUP, 1.8f);
        }
    }
}
