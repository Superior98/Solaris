package com.fireheart.city.client.pc;

import com.fireheart.city.FireheartCity;
import com.fireheart.city.TvBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Draws the live picture on every switched-on SolTube TV nearby, for everyone who can see it. */
@Mod.EventBusSubscriber(modid = FireheartCity.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TvRender {
    private TvRender() {}

    static final int W = ConsoleScreen.W, H = ConsoleScreen.H;

    record Show(String program, long startTick) {}

    static final Map<Long, Show> SHOWS = new HashMap<>();
    static final net.minecraft.client.renderer.MultiBufferSource.BufferSource OWN = net.minecraft.client.renderer.MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.BufferBuilder(4096));
    static final List<BlockPos> TVS = new ArrayList<>();
    static long tick;

    public static void handle(String line) {
        String[] p = line.split("\\|", 6);
        if (p.length < 6) return;
        try {
            BlockPos pos = new BlockPos(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
            long elapsed = Long.parseLong(p[4]);
            if (p[5].isEmpty()) SHOWS.remove(pos.asLong());
            else {
                Show old = SHOWS.get(pos.asLong());
                long start = tick - elapsed;
                if (old != null && old.program.equals(p[5]) && Math.abs(old.startTick - start) < 40) start = old.startTick;
                SHOWS.put(pos.asLong(), new Show(p[5], start));
            }
        } catch (NumberFormatException ignored) {}
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        tick++;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            TVS.clear();
            SHOWS.clear();
            return;
        }
        if (WM && tick % 20 == 0) {
            try {
                CinemaVideo.idle(tick);
            } catch (Throwable t) {
                FireheartCity.LOG.debug("Cinema idle check failed", t);
            }
        }
        if (tick % 40 != 0) return;
        TVS.clear();
        BlockPos c = mc.player.blockPosition();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = -24; x <= 24; x++) for (int z = -24; z <= 24; z++) for (int y = -8; y <= 8; y++) {
            m.set(c.getX() + x, c.getY() + y, c.getZ() + z);
            BlockState s = mc.level.getBlockState(m);
            if (s.getBlock() instanceof TvBlock && s.getValue(TvBlock.ON)) TVS.add(m.immutable());
        }
    }

    static final int CW = 374, CH = 146;
    static final boolean WM = net.minecraftforge.fml.ModList.get().isLoaded("watermedia");

    static void cinema(RenderLevelStageEvent e, Minecraft mc, Vec3 cam, PoseStack ps) {
        BlockPos booth = com.fireheart.city.Expansion.BOOTH_TV;
        if (cam.distanceToSqr(82, 77, -8) > 34 * 34 || cam.z > 1) return;
        if (!(mc.level.getBlockState(new BlockPos(71, 73, 1)).getBlock() == net.minecraft.world.level.block.Blocks.WHITE_CONCRETE)) return;
        Show urlShow = SHOWS.get(booth.asLong());
        String link = urlShow != null && urlShow.program.startsWith("url;") ? urlShow.program.substring(4) : null;
        GuiGraphics g = new GuiGraphics(mc, OWN);
        PoseStack gp = g.pose();
        gp.last().pose().set(ps.last().pose());
        gp.last().normal().set(ps.last().normal());
        gp.translate(94 - cam.x, 82 - cam.y, 1 - 0.02 - cam.z);
        if (link != null) {
            boolean drawn = false;
            if (WM) {
                try {
                    drawn = CinemaVideo.frame(link, urlShow.startTick, tick, ps, cam);
                } catch (Throwable t) {
                    FireheartCity.LOG.warn("Cinema video failed", t);
                }
            }
            if (!drawn) {
                String msg = WM ? CinemaVideo.status : "Install WATERMeDIA to play videos";
                g.drawManaged(() -> {
                    g.fill(0, 0, CW, CH, 0xFF000000);
                    g.drawCenteredString(mc.font, "SOLARIS CINEMA", CW / 2, CH / 2 - 16, 0xFFFFB703);
                    g.drawCenteredString(mc.font, msg.isEmpty() ? "Loading..." : msg, CW / 2, CH / 2, 0xFFFFFFFF);
                    OWN.endBatch();
                });
            }
            return;
        }
        gp.scale(-23f / CW, -9f / CH, -0.0005f);
        g.drawManaged(() -> {
            try {
                var f = mc.font;
                Show sh = SHOWS.get(booth.asLong());
                long gt = mc.level.getGameTime();
                if (sh != null && !sh.program.isEmpty()) {
                    int t = (int) Math.max(0, tick - sh.startTick);
                    String[] p = sh.program.split(";");
                    if (p[0].equals("tube") && p.length > 1) TubeApp.video(g, f, Math.floorMod(ClientPc.parse(p[1]), com.fireheart.city.Computers.VIDEOS.length), 0, 0, CW, CH, t % TubeApp.DUR);
                    else if (p[0].equals("vid") && p.length > 1) PhotoCache.drawVideo(g, p[1], t, 0, 0, CW, CH);
                    else g.fill(0, 0, CW, CH, 0xFF000000);
                } else {
                    int slot = 440;
                    int n = com.fireheart.city.Computers.VIDEOS.length;
                    int idx = (int) ((gt / slot) % n);
                    int t = (int) (gt % slot);
                    if (t < 40) {
                        g.fill(0, 0, CW, CH, 0xFF000000);
                        int a = (int) (Math.min(1, Math.min(t, 40 - t) / 10f) * 255);
                        g.pose().pushPose();
                        g.pose().translate(CW / 2f, CH / 2f - 20, 0);
                        g.pose().scale(2, 2, 1);
                        g.drawCenteredString(f, "NOW SHOWING", 0, 0, (Math.max(4, a) << 24) | 0xFFB703);
                        g.pose().popPose();
                        g.drawCenteredString(f, com.fireheart.city.Computers.VIDEOS[idx], CW / 2, CH / 2 + 6, (Math.max(4, a) << 24) | 0xFFFFFF);
                        g.drawCenteredString(f, "Solaris Cinema", CW / 2, CH / 2 + 22, (Math.max(4, a) << 24) | 0x888888);
                    } else TubeApp.video(g, f, idx, 0, 0, CW, CH, t - 40);
                }
                g.fillGradient(0, 0, CW, 6, 0x66000000, 0x00000000);
                g.fillGradient(0, CH - 6, CW, CH, 0x00000000, 0x66000000);
            } catch (RuntimeException ex) {
                FireheartCity.LOG.debug("Cinema draw failed", ex);
            }
            OWN.endBatch();
        });
    }

    @SubscribeEvent
    public static void onStage(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack();
        cinema(e, mc, cam, ps);
        if (TVS.isEmpty()) return;
        var buf = OWN;
        for (BlockPos pos : TVS) {
            BlockState s = mc.level.getBlockState(pos);
            if (!(s.getBlock() instanceof TvBlock) || !s.getValue(TvBlock.ON)) continue;
            if (Vec3.atCenterOf(pos).distanceToSqr(cam) > 40 * 40) continue;
            Direction f = s.getValue(TvBlock.FACING);
            float ang = switch (f) {
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> 0;
            };
            GuiGraphics g = new GuiGraphics(mc, buf);
            PoseStack gp = g.pose();
            gp.last().pose().set(ps.last().pose());
            gp.last().normal().set(ps.last().normal());
            gp.translate(pos.getX() + 0.5 - cam.x, pos.getY() + 0.5 - cam.y, pos.getZ() + 0.5 - cam.z);
            gp.mulPose(Axis.YP.rotationDegrees(-ang));
            gp.translate(15 / 16f - 0.5f, 13 / 16f - 0.5f, 6.8f / 16f - 0.5f);
            gp.scale(-(14 / 16f) / W, -(8 / 16f) / H, -0.0005f);
            g.drawManaged(() -> {
                try {
                    draw(g, pos, mc.getPartialTick());
                } catch (RuntimeException ex) {
                    FireheartCity.LOG.debug("TV draw failed", ex);
                }
                buf.endBatch();
            });
        }
    }

    static void draw(GuiGraphics g, BlockPos pos, float pt) {
        var f = Minecraft.getInstance().font;
        if (ConsoleScreen.showing(pos)) {
            ConsoleScreen.current.drawTv(g);
            return;
        }
        Show sh = SHOWS.get(pos.asLong());
        long gt = Minecraft.getInstance().level.getGameTime();
        if (sh == null) {
            int id = (int) ((gt / TubeApp.DUR + Math.floorMod(pos.asLong(), 7)) % com.fireheart.city.Computers.VIDEOS.length);
            TubeApp.video(g, f, id, 0, 0, W, H, (int) (gt % TubeApp.DUR));
            g.fill(W - 36, 4, W - 4, 14, 0x88000000);
            g.drawString(f, "§lSolTV", W - 33, 5, 0xFFFFB703, false);
            return;
        }
        int t = (int) Math.max(0, tick - sh.startTick);
        String[] p = sh.program.split(";");
        switch (p[0]) {
            case "tube" -> {
                int id = p.length > 1 ? Math.floorMod(ClientPc.parse(p[1]), com.fireheart.city.Computers.VIDEOS.length) : 0;
                int vt = t % TubeApp.DUR;
                TubeApp.video(g, f, id, 0, 0, W, H, vt);
                g.fill(0, H - 2, W * vt / TubeApp.DUR, H, 0xFFFF0000);
            }
            case "console" -> {
                String st = p.length > 1 ? p[1] : "menu";
                if (st.equals("boot")) ConsoleUi.boot(g, f, Math.min(t, ConsoleUi.BOOT), W, H);
                else if (st.equals("play")) ConsoleUi.watching(g, f, t, W, H, p.length > 2 ? p[2] : "Game", p.length > 3 ? ClientPc.parse(p[3]) : 0, p.length > 4 ? p[4] : "Someone");
                else ConsoleUi.menu(g, f, t, W, H, p.length > 2 ? ClientPc.parse(p[2]) : 0, "SolBox", "");
            }
            case "vid" -> {
                if (p.length > 1) PhotoCache.drawVideo(g, p[1], t, 0, 0, W, H);
            }
            case "photo" -> {
                if (p.length > 1) Pics.draw(g, p[1], 0, 0, W, H);
            }
            case "photos" -> {
                g.fill(0, 0, W, H, 0xFF000000);
                g.drawCenteredString(f, "§l" + (p.length > 1 ? p[1] : "") + "'s photos", W / 2, H / 2 - 4, 0xFFFFFFFF);
            }
            default -> g.fill(0, 0, W, H, 0xFF000000);
        }
    }
}
