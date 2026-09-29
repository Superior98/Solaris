package com.fireheart.city.client;

import com.fireheart.city.Vehicle;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = "fireheartcity", value = Dist.CLIENT)
public final class GpsHud {
    private GpsHud() {}

    static boolean on, player;
    static double tx, ty, tz;
    static String label = "";
    static int ticks;
    static double lastDist = -1, speed;

    public static void handle(String line) {
        String[] p = line.split("\\|", 6);
        if (p.length < 2 || p[1].equals("off")) {
            on = false;
            return;
        }
        if (p.length < 6) return;
        try {
            tx = Integer.parseInt(p[1]) + 0.5;
            ty = Integer.parseInt(p[2]);
            tz = Integer.parseInt(p[3]) + 0.5;
        } catch (NumberFormatException e) {
            return;
        }
        player = p[4].equals("p");
        if (!on || !label.equals(p[5])) {
            ticks = 0;
            lastDist = -1;
        }
        label = p[5];
        on = true;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !on) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            on = false;
            return;
        }
        ticks++;
        double d = dist(mc);
        if (lastDist >= 0) speed = speed * 0.9 + Math.max(0, lastDist - d) * 0.1;
        lastDist = d;
        if (ticks % 4 != 0 || d < 4) return;
        Vec3 from = mc.player.getVehicle() instanceof Vehicle v ? v.position().add(0, 0.6, 0) : mc.player.position().add(0, 0.15, 0);
        Vec3 dir = new Vec3(tx - from.x, 0, tz - from.z).normalize();
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(0.25f, 0.8f, 1f), 1.1f);
        int phase = ticks / 4 % 4;
        for (int i = 0; i < 6; i++) {
            double s = 1.6 + i * 1.3 + phase * 0.33;
            if (s > d) break;
            double px = from.x + dir.x * s, pz = from.z + dir.z * s;
            double py = from.y;
            var bp = net.minecraft.core.BlockPos.containing(px, py, pz);
            for (int k = 0; k < 3 && mc.level.getBlockState(bp).isAir() && !mc.level.getBlockState(bp.below()).isFaceSturdy(mc.level, bp.below(), net.minecraft.core.Direction.UP); k++) bp = bp.below();
            for (int k = 0; k < 3 && !mc.level.getBlockState(bp).isAir(); k++) bp = bp.above();
            mc.level.addParticle(dust, px, bp.getY() + 0.12, pz, 0, 0, 0);
        }
    }

    static double dist(Minecraft mc) {
        double dx = tx - mc.player.getX(), dz = tz - mc.player.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    static String fmt(double m) {
        return m >= 1000 ? String.format("%.1f km", m / 1000) : (int) m + " m";
    }

    static String dirWord(float rel) {
        float a = Math.abs(rel);
        if (a < 20) return "straight ahead";
        if (a > 150) return "turn around";
        return (rel > 0 ? "right" : "left") + (a < 60 ? ", slightly" : "");
    }

    public static void hud(GuiGraphics g, int w, int h) {
        if (!on) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        Font f = mc.font;
        double dx = tx - mc.player.getX(), dz = tz - mc.player.getZ(), dy = ty - mc.player.getY();
        double d = Math.sqrt(dx * dx + dz * dz);
        float yawTo = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
        float rel = Mth.wrapDegrees(yawTo - mc.player.getYRot());
        boolean car = mc.player.getVehicle() instanceof Vehicle;
        int cx = w / 2, top = 6;
        int bw = car ? 170 : 150, bh = car ? 46 : 38;
        g.fill(cx - bw / 2, top, cx + bw / 2, top + bh, 0xB0101826);
        g.fill(cx - bw / 2, top, cx + bw / 2, top + 1, 0xFF3AC8FF);
        g.fill(cx - bw / 2, top + bh - 1, cx + bw / 2, top + bh, 0xFF3AC8FF);
        int ax = cx - bw / 2 + 20, ay = top + bh / 2;
        g.pose().pushPose();
        g.pose().translate(ax, ay, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(rel));
        int col = Math.abs(rel) < 25 ? 0xFF4CFF8A : Math.abs(rel) < 90 ? 0xFFFFD34C : 0xFFFF6A4C;
        for (int i = 0; i < 10; i++) g.fill(-i / 2 - 1, -10 + i, i / 2 + 1, -9 + i, col);
        g.fill(-2, -1, 2, 9, col);
        g.pose().popPose();
        String title = (player ? "§d◉ " : "§b✦ ") + label;
        g.drawString(f, f.plainSubstrByWidth(title, bw - 44), ax + 16, top + 5, 0xFFFFFFFF, false);
        String info = "§f" + fmt(d) + (Math.abs(dy) > 4 ? (dy > 0 ? " §7▲" : " §7▼") + (int) Math.abs(dy) : "") + " §8· §7" + dirWord(rel);
        g.drawString(f, info, ax + 16, top + 16, 0xFFFFFFFF, false);
        if (car) {
            double sp = Math.max(0.01, speed);
            int secs = (int) (d / sp / 20);
            String eta = speed < 0.05 ? "§7ETA --" : "§7ETA §f" + (secs >= 60 ? secs / 60 + "m " + secs % 60 + "s" : secs + "s");
            g.drawString(f, eta + " §8· §7Nav mode", ax + 16, top + 27, 0xFFFFFFFF, false);
        } else g.drawString(f, "§8/sol gps off to stop", ax + 16, top + 27, 0xFFFFFFFF, false);
        int barW = bw - 12;
        int tick = (int) (Mth.wrapDegrees(rel) / 180f * (barW / 2f));
        g.fill(cx - barW / 2, top + bh - 5, cx + barW / 2, top + bh - 4, 0x553AC8FF);
        g.fill(cx + tick - 1, top + bh - 7, cx + tick + 2, top + bh - 2, col);
    }
}
