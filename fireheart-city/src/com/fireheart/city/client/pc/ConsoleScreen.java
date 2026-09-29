package com.fireheart.city.client.pc;

import com.fireheart.city.PcNet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/**
 * Playing the SolBox: the picture is on the TV in the world (everyone nearby sees it) while you hold the controller.
 * This screen only takes the controls; F shows the picture full screen too.
 */
public class ConsoleScreen extends OsScreen {
    static final int W = 256, H = 146;
    static ConsoleScreen current;
    final BlockPos tv;
    int stage, t, sel, video;
    boolean full;
    GameApp game;
    private int lastSent = -100;
    private String lastProgram = "";

    public ConsoleScreen(PcNet.Data d) {
        super(Component.literal("SolBox"), d);
        tv = d.tv == 0 ? null : BlockPos.of(d.tv);
    }

    static final int BOOTING = 0, MENU = 1, PLAY = 2, TUBE = 3, PHOTOS = 4;

    @Override
    protected void init() {
        current = this;
        var mc = Minecraft.getInstance();
        if (tv != null && mc.player != null && t == 0) {
            Vec3 eye = mc.player.getEyePosition();
            Vec3 c = Vec3.atCenterOf(tv).add(0, 0.1, 0);
            Vec3 dv = c.subtract(eye);
            float yaw = (float) (Math.toDegrees(Math.atan2(-dv.x, dv.z)));
            float pitch = (float) (-Math.toDegrees(Math.atan2(dv.y, Math.sqrt(dv.x * dv.x + dv.z * dv.z))));
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
            mc.player.yHeadRot = yaw;
            mc.player.yRotO = yaw;
            mc.player.xRotO = pitch;
        }
        if (tv == null) full = true;
    }

    public boolean phone() {
        return false;
    }

    public void onMessage(String line) {}

    public void openAppById(String id) {}

    public void update(PcNet.Data d) {
        this.data = d;
    }

    static boolean showing(BlockPos pos) {
        ConsoleScreen c = current;
        return c != null && Minecraft.getInstance().screen == c && c.tv != null && c.tv.equals(pos);
    }

    void sound(net.minecraft.sounds.SoundEvent e, float pitch) {
        var p = Minecraft.getInstance().player;
        if (p != null) p.playSound(e, 0.6f, pitch);
    }

    @Override
    public void tick() {
        t++;
        if (stage == BOOTING) {
            if (t == 20) sound(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.8f);
            if (t == 35) sound(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.0f);
            if (t == 50) sound(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.2f);
            if (t == 65) sound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.5f);
            if (t >= ConsoleUi.BOOT) { stage = MENU; t = 0; }
        }
        for (int k : Gamepad.poll()) keyPressed(k, 0, 0);
        if (stage == PLAY && game != null) {
            try {
                game.tick();
            } catch (RuntimeException e) {
                com.fireheart.city.FireheartCity.LOG.warn("SolBox game crashed", e);
                game = null;
                stage = MENU;
                showToast("The game crashed - back to the menu. Sorry!");
            }
        }
        if (stage == TUBE && t >= TubeApp.DUR) { video = (video + 1) % com.fireheart.city.Computers.VIDEOS.length; t = 0; }
        String prog = program();
        if (tv != null && (!prog.equals(lastProgram) || t - lastSent > 40 || t < lastSent)) {
            boolean changed = !prog.equals(lastProgram);
            lastProgram = prog;
            lastSent = t;
            send("tvshow", prog, tv.asLong() + ";" + (changed ? t : -1));
        }
    }

    String program() {
        return switch (stage) {
            case BOOTING -> "console;boot";
            case MENU -> "console;menu;" + sel;
            case PLAY -> "console;play;" + (game == null ? "" : game.title()) + ";" + (game == null ? 0 : game.score) + ";" + data.player;
            case TUBE -> "tube;" + video;
            default -> {
                if (data.gallery.isEmpty()) yield "photos;" + data.player;
                yield "photo;" + data.gallery.get((t / 100) % data.gallery.size()).split(":")[0];
            }
        };
    }

    void drawTv(GuiGraphics g) {
        var f = font();
        switch (stage) {
            case BOOTING -> ConsoleUi.boot(g, f, t, W, H);
            case MENU -> ConsoleUi.menu(g, f, t, W, H, sel, data.player, data.clock.contains(" ") ? data.clock.substring(data.clock.indexOf(' ') + 1) : data.clock);
            case PLAY -> {
                if (game != null) {
                    game.layout(0, 0, W, H);
                    try {
                        game.render(g, -1, -1, net.minecraft.client.Minecraft.getInstance().getFrameTime());
                    } catch (RuntimeException e) {
                        com.fireheart.city.FireheartCity.LOG.warn("SolBox game failed to draw", e);
                        game = null;
                        stage = MENU;
                    }
                }
            }
            case TUBE -> {
                TubeApp.video(g, f, video, 0, 0, W, H, t);
                g.fill(0, H - 2, W * t / TubeApp.DUR, H, 0xFFFF0000);
                g.drawString(f, "§lSolTube §r§7· " + com.fireheart.city.Computers.VIDEOS[video], 5, 5, 0xFFFFFFFF, true);
            }
            default -> {
                List<String> keys = new ArrayList<>();
                for (String s : data.gallery) keys.add(s.split(":")[0]);
                ConsoleUi.slideshow(g, f, keys, t, W, H);
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int sw = width, sh = height;
        if (full) {
            g.fill(0, 0, sw, sh, 0xEE000000);
            float s = Math.min((sw - 20) / (float) W, (sh - 30) / (float) H);
            g.pose().pushPose();
            g.pose().translate((sw - W * s) / 2f, (sh - H * s) / 2f - 6, 0);
            g.pose().scale(s, s, 1);
            g.enableScissor((int) ((sw - W * s) / 2f), (int) ((sh - H * s) / 2f - 6), (int) ((sw + W * s) / 2f), (int) ((sh + H * s) / 2f - 6));
            drawTv(g);
            g.disableScissor();
            g.pose().popPose();
        }
        String hint = tv == null ? "§cNo TV connected - playing on the controller screen" : "§7SolBox  ·  §fArrows/WASD §7move  ·  §fEnter §7select  ·  §fP §7pause  ·  §fBackspace §7back  ·  §fF §7" + (full ? "TV view" : "full screen") + "  ·  §fEsc §7power off" + (Gamepad.present ? "  ·  §a🎮 controller" : "");
        int hw = font.width(hint.replaceAll("§.", "")) + 12;
        g.fill(sw / 2 - hw / 2, sh - 18, sw / 2 + hw / 2, sh - 4, 0x99000000);
        g.drawCenteredString(font, hint, sw / 2, sh - 15, 0xFFFFFFFF);
        if (toastTicks > 0) {
            toastTicks--;
            g.drawCenteredString(font, toast, sw / 2, 10, 0xFFFFFFFF);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g) {}

    GameApp make(int i) {
        return switch (i) {
            case 0 -> new NeonDrift();
            case 1 -> new FoxRun();
            case 2 -> new NeonMaze();
            case 3 -> new BeatSol();
            case 4 -> new SnakeGame();
            case 5 -> new Game2048();
            case 6 -> new FlapGame();
            case 7 -> new MinesGame();
            default -> new BlocksGame();
        };
    }

    static GameApp suspended;
    static int suspendedSel = -1;
    static long suspendedAt;

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        com.fireheart.city.client.DeviceAnim.activity(true);
        if (key == App.ESC) { onClose(); return true; }
        if (key == 70) { full = !full; return true; }
        if (stage == BOOTING) return true;
        if (key == App.BACKSPACE && stage != MENU) {
            if (stage == PLAY && game != null && game.started && !game.over) {
                suspended = game;
                suspendedSel = sel;
                suspendedAt = System.currentTimeMillis();
                showToast("Game saved - pick it again to resume");
            }
            stage = MENU;
            t = 0;
            game = null;
            sound(SoundEvents.UI_BUTTON_CLICK.value(), 0.8f);
            return true;
        }
        switch (stage) {
            case MENU -> {
                if (App.left(key)) { sel = Math.floorMod(sel - 1, ConsoleUi.TILES.length); sound(SoundEvents.NOTE_BLOCK_HAT.value(), 1.6f); }
                else if (App.right(key)) { sel = (sel + 1) % ConsoleUi.TILES.length; sound(SoundEvents.NOTE_BLOCK_HAT.value(), 1.6f); }
                else if (key == App.ENTER || key == App.KP_ENTER || key == App.SPACE) {
                    sound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.4f);
                    t = 0;
                    if (sel < ConsoleUi.GAME_TILES) {
                        if (suspended != null && suspendedSel == sel && System.currentTimeMillis() - suspendedAt < 10 * 60_000L) {
                            game = suspended;
                            game.open(this);
                            showToast("Resumed where you left off");
                            if (game instanceof Gen2Game g2) g2.paused = true;
                        } else {
                            game = make(sel);
                            game.open(this);
                        }
                        suspended = null;
                        game.layout(0, 0, W, H);
                        stage = PLAY;
                    } else if (sel == ConsoleUi.GAME_TILES) { stage = TUBE; video = 0; }
                    else stage = PHOTOS;
                }
            }
            case PLAY -> { if (game != null) game.key(key); }
            case TUBE -> {
                if (App.right(key)) { video = (video + 1) % com.fireheart.city.Computers.VIDEOS.length; t = 0; }
                else if (App.left(key)) { video = Math.floorMod(video - 1, com.fireheart.city.Computers.VIDEOS.length); t = 0; }
            }
            case PHOTOS -> {
                if (App.right(key)) t = (t / 100 + 1) * 100;
                else if (App.left(key)) t = Math.max(0, (t / 100 - 1) * 100);
            }
            default -> {}
        }
        return true;
    }

    @Override
    public void onClose() {
        if (stage == PLAY && game != null && game.started && !game.over) {
            suspended = game;
            suspendedSel = sel;
            suspendedAt = System.currentTimeMillis();
        }
        if (tv != null) send("tvoff", String.valueOf(tv.asLong()), "");
        current = null;
        super.onClose();
    }
}
