package com.fireheart.city.client.sky;

import com.fireheart.city.FireheartCity;
import com.fireheart.city.PcNet;
import com.fireheart.city.Resident;
import com.fireheart.city.Skydive;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client side of skydiving: flies the local player (launch up the tube, freefall with steering, parachute, landing),
 * switches to third person, draws the spread-eagle poses, wind particles, wind sound, camera sway and the altimeter.
 */
@Mod.EventBusSubscriber(modid = FireheartCity.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SkyClient {
    private SkyClient() {}

    public static int phase;
    static int ticks;
    static double apex = Skydive.APEX, padX = Double.NaN, padZ = Double.NaN, top;
    static float dive, turn, turnO, flare;
    static CameraType oldCam;
    static WindSound wind;
    static final Map<Integer, int[]> OTHERS = new HashMap<>();
    static int chuteSeed;

    public static void handle(String line) {
        String[] p = line.split("\\|");
        Minecraft mc = Minecraft.getInstance();
        if (p[0].equals("#skyp") && p.length >= 3) {
            int id = parse(p[1]), ph = parse(p[2]);
            if (ph == 0) OTHERS.remove(id);
            else OTHERS.put(id, new int[]{ph, mc.player == null ? 0 : mc.player.tickCount});
            return;
        }
        if (p.length >= 3 && p[1].equals("launch")) {
            apex = parseD(p[2], Skydive.APEX);
            padX = p.length >= 6 ? parseD(p[3], Double.NaN) : Double.NaN;
            padZ = p.length >= 6 ? parseD(p[4], Double.NaN) : Double.NaN;
            top = p.length >= 6 ? parseD(p[5], 0) : 0;
            start();
        }
    }

    static int parse(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 0; }
    }

    static double parseD(String s, double def) {
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return def; }
    }

    static void start() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (phase == Skydive.NONE) oldCam = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        set(Skydive.LAUNCH, false);
        dive = turn = flare = 0;
        chuteSeed = mc.player.getRandom().nextInt(5);
        if (wind == null || wind.isStopped()) {
            wind = new WindSound();
            mc.getSoundManager().play(wind);
        }
        mc.player.playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.5f, 0.6f);
    }

    static void set(int ph, boolean report) {
        phase = ph;
        ticks = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            if (ph == 0) OTHERS.remove(mc.player.getId());
            else OTHERS.put(mc.player.getId(), new int[]{ph, mc.player.tickCount});
        }
        if (!report) return;
        String what = switch (ph) {
            case Skydive.FREEFALL -> "freefall";
            case Skydive.CHUTE -> "chute";
            case Skydive.LANDED -> "landed";
            default -> "done";
        };
        PcNet.CHANNEL.sendToServer(new PcNet.Act(BlockPos.ZERO, "sky", what, ""));
    }

    static void finish() {
        Minecraft mc = Minecraft.getInstance();
        set(Skydive.NONE, true);
        if (oldCam != null) mc.options.setCameraType(oldCam);
        oldCam = null;
    }

    static double ground(LocalPlayer p) {
        return p.level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(p.getX()), Mth.floor(p.getZ()));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.START || !e.player.level().isClientSide || !(e.player instanceof LocalPlayer p) || p != Minecraft.getInstance().player) return;
        if (phase == Skydive.NONE) return;
        if (!p.isAlive() || p.isSpectator()) { finish(); return; }
        ticks++;
        p.fallDistance = 0;
        float fwd = p.input == null ? 0 : p.input.forwardImpulse, side = p.input == null ? 0 : p.input.leftImpulse;
        boolean jump = p.input != null && p.input.jumping;
        Vec3 v = p.getDeltaMovement();
        float yaw = p.getYRot() * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw), fz = Mth.cos(yaw), rx = -fz, rz = fx;
        turnO = turn;
        switch (phase) {
            case Skydive.LAUNCH -> {
                if (p.isPassenger()) p.stopRiding();
                double vy = Math.min(2.6, 0.5 + ticks * 0.14);
                if (apex - p.getY() < 30) vy = Math.max(0.12, (apex - p.getY()) * 0.085);
                double vx = 0, vz = 0;
                if (!Double.isNaN(padX) && p.getY() < top + 3) {
                    vx = (padX - p.getX()) * 0.35;
                    vz = (padZ - p.getZ()) * 0.35;
                }
                p.setDeltaMovement(vx, vy, vz);
                if (p.getY() >= apex - 1.5 || ticks > 320) {
                    set(Skydive.FREEFALL, true);
                    p.setXRot(28f);
                    p.playSound(SoundEvents.PHANTOM_FLAP, 0.8f, 0.6f);
                }
            }
            case Skydive.FREEFALL -> {
                dive += (Math.max(0, fwd) - dive) * 0.07f;
                flare += (Math.max(0, -fwd) - flare) * 0.1f;
                turn += (-side - turn) * 0.1f;
                double glide = 0.12 + 0.62 * dive - 0.08 * flare;
                double tx = fx * glide + rx * -side * 0.28, tz = fz * glide + rz * -side * 0.28;
                double hx = v.x + (tx - v.x) * 0.08, hz = v.z + (tz - v.z) * 0.08;
                double vyT = -(0.95 + 0.75 * dive - 0.35 * flare);
                double vy = v.y + (vyT - v.y) * 0.06;
                p.setDeltaMovement(hx, vy, hz);
                double agl = p.getY() - ground(p);
                if (p.onGround() || p.isInWater()) { set(Skydive.LANDED, true); landFx(p); }
                else if (agl < 34 || jump && ticks > 12) {
                    set(Skydive.CHUTE, true);
                    p.setXRot(-8f);
                    p.playSound(SoundEvents.ARMOR_EQUIP_ELYTRA, 1.2f, 0.7f);
                    p.playSound(SoundEvents.WOOL_PLACE, 1.5f, 0.6f);
                }
            }
            case Skydive.CHUTE -> {
                turn += (-side - turn) * 0.08f;
                flare += (Math.max(0, -fwd) - flare) * 0.1f;
                p.setYRot(p.getYRot() + turn * 2.6f);
                double glide = 0.2 + 0.1 * Math.max(0, fwd) - 0.12 * flare;
                double hx = v.x + (fx * glide - v.x) * 0.1, hz = v.z + (fz * glide - v.z) * 0.1;
                double vyT = -(0.17 - 0.06 * flare);
                double vy = v.y + (vyT - v.y) * (ticks < 12 ? 0.3 : 0.15);
                p.setDeltaMovement(hx, vy, hz);
                if (p.onGround() || p.isInWater()) { set(Skydive.LANDED, true); landFx(p); }
            }
            case Skydive.LANDED -> {
                p.setDeltaMovement(v.x * 0.6, v.y, v.z * 0.6);
                if (ticks > 26) finish();
            }
            default -> {}
        }
    }

    static void landFx(LocalPlayer p) {
        p.playSound(SoundEvents.GENERIC_SMALL_FALL, 1f, 0.9f);
        p.playSound(SoundEvents.WOOL_BREAK, 1f, 0.7f);
        for (int i = 0; i < 24; i++) {
            double a = i / 24.0 * Math.PI * 2;
            p.level().addParticle(ParticleTypes.POOF, p.getX(), p.getY() + 0.1, p.getZ(), Math.cos(a) * 0.2, 0.02, Math.sin(a) * 0.2);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            if (phase != 0) phase = 0;
            OTHERS.clear();
            return;
        }
        if (mc.isPaused()) return;
        if (phase != Skydive.NONE) particles(mc.player, phase, ticks, true);
        for (Map.Entry<Integer, int[]> en : OTHERS.entrySet()) {
            if (en.getKey() == mc.player.getId()) continue;
            var ent = mc.level.getEntity(en.getKey());
            if (ent != null) particles(ent, en.getValue()[0], mc.player.tickCount - en.getValue()[1], false);
        }
        for (Resident r : mc.level.getEntitiesOfClass(Resident.class, mc.player.getBoundingBox().inflate(160, 320, 160), r -> r.skyPhase() > 0)) particles(r, r.skyPhase(), r.tickCount, false);
    }

    static void particles(net.minecraft.world.entity.Entity p, int ph, int t, boolean self) {
        var lvl = p.level();
        RandomSource r = lvl.random;
        Vec3 v = self ? p.getDeltaMovement() : new Vec3(p.getX() - p.xo, p.getY() - p.yo, p.getZ() - p.zo);
        double x = p.getX(), y = p.getY() + 0.9, z = p.getZ();
        switch (ph) {
            case Skydive.LAUNCH -> {
                for (int i = 0; i < 3; i++) lvl.addParticle(ParticleTypes.FLAME, x + r.nextGaussian() * 0.15, p.getY() - 0.2, z + r.nextGaussian() * 0.15, r.nextGaussian() * 0.03, -0.6, r.nextGaussian() * 0.03);
                lvl.addParticle(ParticleTypes.FIREWORK, x + r.nextGaussian() * 0.3, p.getY() - 0.5, z + r.nextGaussian() * 0.3, 0, -0.3, 0);
                if (t % 2 == 0) lvl.addParticle(ParticleTypes.CLOUD, x + r.nextGaussian() * 0.6, p.getY() - 1, z + r.nextGaussian() * 0.6, 0, -0.1, 0);
            }
            case Skydive.FREEFALL -> {
                int n = self ? 5 : 2;
                for (int i = 0; i < n; i++) {
                    double ox = r.nextGaussian() * 2.2, oz = r.nextGaussian() * 2.2, oy = -2 - r.nextDouble() * 3;
                    lvl.addParticle(ParticleTypes.CLOUD, x + ox + v.x * 4, y + oy + v.y * 3, z + oz + v.z * 4, -v.x * 0.5, -v.y * 0.9, -v.z * 0.5);
                }
                if (self && t % 2 == 0) {
                    double a = r.nextDouble() * Math.PI * 2;
                    lvl.addParticle(ParticleTypes.END_ROD, x + Math.cos(a) * 1.4, y - 3, z + Math.sin(a) * 1.4, -v.x * 0.3, -v.y * 1.4, -v.z * 0.3);
                }
                if (t % 3 == 0) lvl.addParticle(ParticleTypes.WHITE_ASH, x + r.nextGaussian(), y + r.nextGaussian(), z + r.nextGaussian(), 0, 0.5, 0);
            }
            case Skydive.CHUTE -> {
                if (t % 4 == 0) lvl.addParticle(ParticleTypes.CLOUD, x + r.nextGaussian() * 2.5, y + 3 + r.nextGaussian(), z + r.nextGaussian() * 2.5, -v.x * 0.3, 0.05, -v.z * 0.3);
            }
            default -> {}
        }
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent e) {
        if (phase == Skydive.FREEFALL && e.getPlayer() == Minecraft.getInstance().player) {
            double sp = e.getPlayer().getDeltaMovement().length();
            e.setNewFovModifier(e.getNewFovModifier() * (1f + (float) Math.min(0.35, sp * 0.14)));
        } else if (phase == Skydive.LAUNCH && e.getPlayer() == Minecraft.getInstance().player) {
            e.setNewFovModifier(e.getNewFovModifier() * 1.25f);
        }
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        if (phase == Skydive.NONE) return;
        Minecraft mc = Minecraft.getInstance();
        float pt = (float) e.getPartialTick();
        float t = ticks + pt;
        float tr = Mth.lerp(pt, turnO, turn);
        switch (phase) {
            case Skydive.LAUNCH -> {
                RandomSource r = mc.player.getRandom();
                e.setRoll(e.getRoll() + (r.nextFloat() - 0.5f) * 1.2f);
                e.setPitch(e.getPitch() + (r.nextFloat() - 0.5f) * 0.8f);
            }
            case Skydive.FREEFALL -> {
                double sp = mc.player.getDeltaMovement().length();
                float shake = (float) Math.min(0.6, sp * 0.25);
                e.setRoll(e.getRoll() + tr * 14f + Mth.sin(t * 0.07f) * 2.5f + (mc.player.getRandom().nextFloat() - 0.5f) * shake);
                e.setPitch(e.getPitch() + Mth.sin(t * 0.11f) * 1.2f);
            }
            case Skydive.CHUTE -> e.setRoll(e.getRoll() + tr * 8f + Mth.sin(t * 0.06f) * 2f);
            default -> {}
        }
    }

    public static boolean skydiving(net.minecraft.world.entity.Entity e) {
        return e == Minecraft.getInstance().player ? phase != Skydive.NONE : phaseOf(e) != Skydive.NONE;
    }

    static int phaseOf(net.minecraft.world.entity.Entity e) {
        int[] o = OTHERS.get(e.getId());
        return o == null ? 0 : o[0];
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderPlayer(RenderPlayerEvent.Pre e) {
        if (!(e.getEntity() instanceof AbstractClientPlayer pl)) return;
        Minecraft mc = Minecraft.getInstance();
        boolean self = pl == mc.player;
        int ph = self ? phase : phaseOf(pl);
        if (ph == Skydive.NONE) return;
        try {
            float pt = e.getPartialTick();
            int[] o = OTHERS.get(pl.getId());
            float t = self ? ticks + pt : pl.tickCount - (o == null ? 0 : o[1]) + pt;
            float dv, tr;
            if (self) {
                dv = dive;
                tr = Mth.lerp(pt, turnO, turn);
            } else {
                double vy = pl.getY() - pl.yo;
                dv = (float) Mth.clamp((-vy - 1.0) / 0.7, 0, 1);
                tr = 0;
            }
            float yaw = Mth.rotLerp(pt, pl.yRotO, pl.getYRot());
            PoseStack ps = e.getPoseStack();
            MultiBufferSource buf = e.getMultiBufferSource();
            PlayerModel<AbstractClientPlayer> m = e.getRenderer().getModel();
            ps.pushPose();
            SkyPose.body(ps, ph, t, dv, tr, yaw);
            ps.scale(-1f, -1f, 1f);
            ps.scale(0.9375f, 0.9375f, 0.9375f);
            ps.translate(0, -1.501f, 0);
            m.attackTime = 0;
            m.riding = false;
            m.young = false;
            m.crouching = ph == Skydive.LANDED && t >= 12 && t < 20;
            m.setAllVisible(true);
            m.setupAnim(pl, 0, 0, pl.tickCount + pt, 0, 0);
            SkyPose.limbs(m, ph, t, dv, tr);
            m.renderToBuffer(ps, buf.getBuffer(RenderType.entityTranslucent(pl.getSkinTextureLocation())), e.getPackedLight(), OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
            ps.popPose();
            if (ph == Skydive.CHUTE || ph == Skydive.LANDED) SkyPose.chute(ps, buf, e.getPackedLight(), yaw, ph, t, tr, self ? chuteSeed : pl.getId());
            e.setCanceled(true);
        } catch (Throwable t) {
            if (errors++ < 5) FireheartCity.LOG.error("Skydive render failed", t);
        }
    }

    static int errors;

    public static void hud(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (phase == Skydive.NONE || mc.player == null || mc.options.hideGui) return;
        Font f = mc.font;
        LocalPlayer p = mc.player;
        double agl = Math.max(0, p.getY() - ground(p));
        double kmh = p.getDeltaMovement().length() * 20 * 3.6;
        if (phase == Skydive.FREEFALL) {
            long now = System.currentTimeMillis();
            RandomSource r = RandomSource.create(now / 60);
            float sp = (float) Math.min(1, p.getDeltaMovement().length() / 1.7);
            for (int i = 0; i < (int) (18 * sp); i++) {
                int side = r.nextInt(4);
                int len = 20 + r.nextInt(50);
                int a = (int) (40 + 60 * sp) << 24;
                int x = side < 2 ? r.nextInt(w) : side == 2 ? r.nextInt(w / 6) : w - r.nextInt(w / 6);
                int y = side < 2 ? (side == 0 ? r.nextInt(h / 5) : h - r.nextInt(h / 5)) : r.nextInt(h);
                g.fill(x, y, x + 1, Math.min(h, y + len), a | 0xFFFFFF);
            }
        }
        int bx = w - 26, by = h / 2 - 60, bh = 120;
        g.fill(bx - 2, by - 2, bx + 8, by + bh + 2, 0x88000000);
        int fill = (int) (bh * Math.min(1, agl / Math.max(40, apex - 60)));
        g.fill(bx, by + bh - fill, bx + 6, by + bh, agl < 30 ? 0xFFFF4D4D : phase == Skydive.CHUTE ? 0xFF4CC9F0 : 0xFFFFB703);
        String alt = (int) agl + " m";
        g.drawString(f, alt, bx - 4 - f.width(alt), by + bh - fill - 4, 0xFFFFFFFF, true);
        String spd = (int) kmh + " km/h";
        g.drawString(f, spd, bx - 4 - f.width(spd), by + bh + 6, 0xFFFFFFFF, true);
        String hint = switch (phase) {
            case Skydive.LAUNCH -> "§6§lLAUNCHING!";
            case Skydive.FREEFALL -> "§fW §7dive  §fS §7flare  §fA/D §7slide  §fSPACE §7parachute";
            case Skydive.CHUTE -> "§fA/D §7steer  §fS §7flare to land softly";
            default -> "§a§lNICE LANDING!";
        };
        g.drawCenteredString(f, hint, w / 2, h - 72, 0xFFFFFFFF);
        if (phase == Skydive.FREEFALL && agl < 60 && (System.currentTimeMillis() / 250) % 2 == 0) g.drawCenteredString(f, "§c§lPULL!", w / 2, h / 2 + 20, 0xFFFFFFFF);
    }
}
