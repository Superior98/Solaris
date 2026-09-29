package com.fireheart.city.client.pc;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/** Beat Solaris: a four-lane rhythm game. The song is built from note blocks and gets faster every verse. */
class BeatSol extends Gen2Game {
    static final int[] KEYS = {LEFT, DOWN, UP, RIGHT};
    static final int[] ALT = {KA, KS, KW, KD};
    static final int[] LANE_COL = {0xFFFF2E88, 0xFF4CC9F0, 0xFF06D6A0, 0xFFFFD166};
    static final float[] SCALE = {0.5f, 0.56f, 0.63f, 0.67f, 0.75f, 0.84f, 0.94f, 1f, 1.12f, 1.26f, 1.33f, 1.5f};
    final List<float[]> notes = new ArrayList<>();
    final float[] flash = new float[4];
    int combo, maxCombo, perfect, misses, beat, bpmT, songPos;
    String judge = "";
    int judgeT;
    float speed;

    public String title() { return "Beat Solaris"; }

    public int color() { return 0xFF7209B7; }

    int accent() { return 0xFFC77DFF; }

    String id() { return "beat"; }

    String help() { return "←↓↑→ (or ASWD) as notes hit the line"; }

    String[][] achievementList() {
        return new String[][]{{"c25", "In the Groove (25 combo)"}, {"c100", "Unstoppable (100 combo)"}, {"p50", "Perfectionist (50 perfects)"}, {"v4", "Encore! (verse 4)"}};
    }

    boolean achieved(String k) {
        return switch (k) {
            case "c25" -> maxCombo >= 25;
            case "c100" -> maxCombo >= 100;
            case "p50" -> perfect >= 50;
            case "v4" -> songPos / 64 >= 3;
            default -> false;
        };
    }

    void reset() {
        notes.clear();
        combo = maxCombo = perfect = misses = 0;
        beat = 0;
        bpmT = 0;
        songPos = 0;
        speed = 2.2f;
        judge = "";
    }

    int hitY() {
        return h - 28;
    }

    void step() {
        int interval = Math.max(5, 10 - songPos / 64);
        speed = 2.2f + songPos / 64 * 0.35f;
        if (++bpmT >= interval) {
            bpmT = 0;
            songPos++;
            int s = songPos % 16;
            boolean on = s % 2 == 0 || rnd.nextFloat() < 0.25f + songPos / 400f;
            if (on) {
                int lane = (int) ((Mth.sin(songPos * 0.7f) + 1) * 1.99f) % 4;
                if (rnd.nextFloat() < 0.3f) lane = rnd.nextInt(4);
                notes.add(new float[]{lane, -10, 0, SCALE[(songPos * 5 + lane * 3) % SCALE.length]});
                if (songPos % 8 == 0 && rnd.nextFloat() < 0.35f) notes.add(new float[]{(lane + 2) % 4, -10, 0, SCALE[(songPos * 3) % SCALE.length]});
            }
            if (songPos % 4 == 0) sfx(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 0.8f);
            if (songPos % 4 == 2) sfx(SoundEvents.NOTE_BLOCK_SNARE.value(), 1f);
            if (songPos % 2 == 1) sfx(SoundEvents.NOTE_BLOCK_HAT.value(), 1.6f);
        }
        for (int i = notes.size() - 1; i >= 0; i--) {
            float[] n = notes.get(i);
            n[1] += speed;
            if (n[1] > hitY() + 18) {
                notes.remove(i);
                miss();
            }
        }
        for (int i = 0; i < 4; i++) flash[i] = Math.max(0, flash[i] - 0.1f);
        if (judgeT > 0) judgeT--;
        if (misses >= 12) gameOver();
    }

    void miss() {
        combo = 0;
        misses++;
        judge = "§cMISS";
        judgeT = 20;
        shake(2, 5);
    }

    boolean input(int k) {
        for (int lane = 0; lane < 4; lane++) {
            if (k != KEYS[lane] && k != ALT[lane]) continue;
            flash[lane] = 1;
            float[] best = null;
            float bd = 999;
            for (float[] n : notes) if ((int) n[0] == lane) {
                float d = Math.abs(n[1] - hitY());
                if (d < bd) { bd = d; best = n; }
            }
            if (best == null || bd > 26) {
                sfx(SoundEvents.NOTE_BLOCK_BASS.value(), 0.5f);
                return true;
            }
            notes.remove(best);
            combo++;
            maxCombo = Math.max(maxCombo, combo);
            int mult = 1 + Math.min(4, combo / 10);
            if (bd < 7) { perfect++; score += 100 * mult; judge = "§b§lPERFECT"; }
            else if (bd < 15) { score += 60 * mult; judge = "§aGREAT"; }
            else { score += 25 * mult; judge = "§eGOOD"; }
            if (misses > 0 && combo % 20 == 0) misses--;
            judgeT = 20;
            SoundEvent[] voices = {SoundEvents.NOTE_BLOCK_PLING.value(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundEvents.NOTE_BLOCK_FLUTE.value()};
            sfx(voices[lane], best[3] * 1.5f);
            int lw = w / 6;
            burst(w / 2f - lw * 2 + lw * lane + lw / 2f, hitY(), bd < 7 ? 16 : 8, LANE_COL[lane], 2.2f);
            return true;
        }
        return false;
    }

    void draw(GuiGraphics g, int mx, int my, float pt) {
        g.fillGradient(x, y, x + w, y + h, 0xFF10002B, 0xFF240046);
        float pulse = (bpmT + pt) / Math.max(5, 10 - songPos / 64);
        for (int i = 0; i < 10; i++) {
            int r = (int) ((i * 18 + pulse * 18) % 180);
            int a = (int) (40 * (1 - r / 180f));
            g.fill(x + w / 2 - r, y + h / 2 - 1, x + w / 2 + r, y + h / 2, (a << 24) | 0xC77DFF);
        }
        int lw = w / 6, lx0 = x + w / 2 - lw * 2;
        for (int lane = 0; lane < 4; lane++) {
            int lx = lx0 + lw * lane;
            g.fill(lx + 1, y + 12, lx + lw - 1, y + h, 0x33000000);
            int fa = (int) (flash[lane] * 120);
            if (fa > 0) g.fillGradient(lx + 1, y + 12, lx + lw - 1, y + hitY(), 0x00000000, (fa << 24) | (LANE_COL[lane] & 0xFFFFFF));
            g.fill(lx + 2, y + hitY() - 2, lx + lw - 2, y + hitY() + 2, (flash[lane] > 0.5f ? 0xFFFFFFFF : LANE_COL[lane]));
        }
        String[] arrows = {"←", "↓", "↑", "→"};
        var f = os.font();
        for (int lane = 0; lane < 4; lane++) g.drawCenteredString(f, arrows[lane], lx0 + lw * lane + lw / 2, y + hitY() + 6, LANE_COL[lane]);
        for (float[] n : notes) {
            int lane = (int) n[0];
            int ny = y + (int) (n[1] + speed * pt);
            int nx = lx0 + lw * lane;
            g.fill(nx + 4, ny - 4, nx + lw - 4, ny + 4, LANE_COL[lane]);
            g.fill(nx + 6, ny - 2, nx + lw - 6, ny + 2, 0xFFFFFFFF);
        }
        if (judgeT > 0) {
            g.pose().pushPose();
            g.pose().translate(x + w / 2f, y + h / 2f - 20 - (20 - judgeT), 0);
            g.pose().scale(1.6f, 1.6f, 1);
            g.drawCenteredString(f, judge, 0, 0, 0xFFFFFFFF);
            g.pose().popPose();
        }
        if (combo >= 5) g.drawCenteredString(f, "§f" + combo + " §7combo  §dx" + (1 + Math.min(4, combo / 10)), x + w / 2, y + 16, 0xFFFFFFFF);
        g.fill(x + 4, y + h - 8, x + 4 + Math.max(0, 60 - misses * 5), y + h - 4, misses > 8 ? 0xFFFF3B30 : 0xFF06D6A0);
    }
}
