package com.fireheart.city.client;

import com.fireheart.city.FireheartCity;
import com.fireheart.city.PcNet;
import com.fireheart.city.Vehicle;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Engine sound, speedometer and the horn/headlight keys for Solaris vehicles. */
@Mod.EventBusSubscriber(modid = FireheartCity.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class VehicleClient {
    private VehicleClient() {}

    public static final KeyMapping HORN = new KeyMapping("key.fireheartcity.horn", 72, "key.categories.fireheartcity");
    public static final KeyMapping LIGHTS = new KeyMapping("key.fireheartcity.lights", 76, "key.categories.fireheartcity");
    static final Map<Integer, Engine> ENGINES = new HashMap<>();

    static SoundEvent ev(String id) {
        return SoundEvent.createVariableRangeEvent(new ResourceLocation(FireheartCity.MODID, id));
    }

    static final class Engine extends AbstractTickableSoundInstance {
        final Vehicle v;

        Engine(Vehicle v) {
            super(ev(v.kind() == Vehicle.BIKE ? "vehicle.engine_bike" : v.kind() == Vehicle.BOAT ? "vehicle.engine_boat" : "vehicle.engine_car"), SoundSource.NEUTRAL, RandomSource.create());
            this.v = v;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01f;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            this.x = v.getX();
            this.y = v.getY();
            this.z = v.getZ();
        }

        @Override
        public void tick() {
            if (v.isRemoved() || v.getPassengers().isEmpty()) {
                stop();
                return;
            }
            x = v.getX();
            y = v.getY();
            z = v.getZ();
            float r = Mth.clamp(v.rpm, 0.1f, 1.05f);
            float load = v.throttle ? 1f : 0.55f;
            float tp = (v.kind() == Vehicle.BOAT ? 0.6f : 0.5f) + r * (v.kind() == Vehicle.BIKE ? 1.35f : 1.1f) - (v.shiftT > 0 ? 0.12f : 0);
            pitch = Mth.lerp(0.35f, pitch, tp);
            volume = Mth.lerp(0.3f, volume, 0.35f + r * 0.45f * load + (v.throttle ? 0.12f : 0));
        }

        void end() {
            stop();
        }
    }

    static final class Skid extends AbstractTickableSoundInstance {
        final Vehicle v;

        Skid(Vehicle v) {
            super(ev("vehicle.skid_loop"), SoundSource.NEUTRAL, RandomSource.create());
            this.v = v;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01f;
            this.x = v.getX();
            this.y = v.getY();
            this.z = v.getZ();
        }

        @Override
        public void tick() {
            if (v.isRemoved() || v.getPassengers().isEmpty()) {
                stop();
                return;
            }
            x = v.getX();
            y = v.getY();
            z = v.getZ();
            float want = v.skidding ? Math.min(0.9f, 0.3f + Math.abs(v.speed) * 0.6f) : 0f;
            volume = Mth.lerp(want > volume ? 0.5f : 0.25f, volume, want);
            pitch = 0.85f + Math.abs(v.speed) * 0.2f;
            if (volume < 0.02f && !v.skidding) stop();
        }
    }

    static final Map<Integer, Skid> SKIDS = new HashMap<>();

    @SubscribeEvent
    public static void fov(net.minecraftforge.client.event.ComputeFovModifierEvent e) {
        if (e.getPlayer().getVehicle() instanceof Vehicle v) e.setNewFovModifier(e.getNewFovModifier() * (1f + Math.min(1f, Math.abs(v.speed) / Vehicle.maxSpeed(v.kind())) * 0.12f));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            ENGINES.clear();
            return;
        }
        for (var ent : mc.level.entitiesForRendering()) {
            if (!(ent instanceof Vehicle v) || v.getPassengers().isEmpty() || v.distanceToSqr(mc.player) > 48 * 48) continue;
            Engine en = ENGINES.get(v.getId());
            if (en == null || en.isStopped()) {
                en = new Engine(v);
                ENGINES.put(v.getId(), en);
                mc.getSoundManager().play(en);
            }
        }
        ENGINES.values().removeIf(Engine::isStopped);
        for (var ent : mc.level.entitiesForRendering()) {
            if (!(ent instanceof Vehicle v) || !v.skidding || v.distanceToSqr(mc.player) > 40 * 40) continue;
            Skid sk = SKIDS.get(v.getId());
            if (sk == null || sk.isStopped()) {
                sk = new Skid(v);
                SKIDS.put(v.getId(), sk);
                mc.getSoundManager().play(sk);
            }
        }
        SKIDS.values().removeIf(Skid::isStopped);
        while (HORN.consumeClick()) if (mc.player.getVehicle() instanceof Vehicle) PcNet.CHANNEL.sendToServer(new PcNet.Act(BlockPos.ZERO, "vehicle", "horn", ""));
        while (LIGHTS.consumeClick()) if (mc.player.getVehicle() instanceof Vehicle) PcNet.CHANNEL.sendToServer(new PcNet.Act(BlockPos.ZERO, "vehicle", "lights", ""));
    }

    public static void hud(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof Vehicle v) || mc.options.hideGui) return;
        var f = mc.font;
        int kmh = (int) (Math.abs(v.speed) * 20 * 3.6f);
        int cx = w - 70, cy = h - 62, r = 40;
        g.fill(cx - r - 6, cy - r / 2 - 12, cx + r + 6, cy + r / 2 + 18, 0x99000000);
        float max = Vehicle.maxSpeed(v.kind()) * 72;
        for (int i = 0; i <= 24; i++) {
            float a = Mth.PI * (0.85f + 1.3f * i / 24f);
            int x0 = cx + (int) (Mth.cos(a) * (r - 4)), y0 = cy + (int) (Mth.sin(a) * (r - 4)) / 2 + 8;
            boolean lit = i / 24f <= Math.min(1, v.rpm);
            g.fill(x0 - 1, y0 - 1, x0 + 2, y0 + 2, lit ? (i > 20 ? 0xFFFF3B30 : i > 16 ? 0xFFFFC23B : 0xFF4CC9F0) : 0x44FFFFFF);
        }
        g.pose().pushPose();
        g.pose().translate(cx, cy - 4, 0);
        g.pose().scale(1.9f, 1.9f, 1);
        g.drawCenteredString(f, String.valueOf(kmh), 0, 0, 0xFFFFFFFF);
        g.pose().popPose();
        g.drawCenteredString(f, "§7km/h", cx, cy + 12, 0xFFFFFFFF);
        String gear = v.kind() == Vehicle.BOAT ? (v.speed < -0.01f ? "R" : Math.abs(v.speed) < 0.01f ? "N" : "F") : v.gear < 0 ? "R" : Math.abs(v.speed) < 0.01f && !v.throttle ? "N" : String.valueOf(v.gear);
        g.drawString(f, "§e" + gear, cx + r - 6, cy + 10, 0xFFFFFFFF, false);
        g.drawString(f, "§8" + (int) (v.rpm * (v.kind() == Vehicle.BIKE ? 11000 : 7000)) + " rpm", cx - r, cy + 10, 0xFFFFFFFF, false);
        int bw = (int) ((r * 2) * Math.min(1, Math.abs(v.speed) / (max / 72)));
        g.fill(cx - r, cy + r / 2 + 12, cx - r + bw, cy + r / 2 + 14, 0xFF4CC9F0);
        String flags = (v.lights() ? "§e◉ " : "§8◉ ") + (v.drifting ? "§6DRIFT " : "") + (v.braking ? "§cBRAKE" : "");
        g.drawCenteredString(f, flags, cx, cy - r / 2 - 8, 0xFFFFFFFF);
        g.drawString(f, "§8W/S throttle/brake · A/D steer · Space handbrake · H horn · L lights", 6, h - 38, 0xFFFFFFFF, false);
    }
}
