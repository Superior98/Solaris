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
            float sp = Math.abs(v.speed) / Vehicle.maxSpeed(v.kind());
            float gear = v.kind() == Vehicle.BOAT ? sp : (sp * 4) % 1;
            pitch = 0.55f + sp * 0.9f + gear * 0.25f;
            volume = 0.45f + sp * 0.55f;
        }

        void end() {
            stop();
        }
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
        while (HORN.consumeClick()) if (mc.player.getVehicle() instanceof Vehicle) PcNet.CHANNEL.sendToServer(new PcNet.Act(BlockPos.ZERO, "vehicle", "horn", ""));
        while (LIGHTS.consumeClick()) if (mc.player.getVehicle() instanceof Vehicle) PcNet.CHANNEL.sendToServer(new PcNet.Act(BlockPos.ZERO, "vehicle", "lights", ""));
    }

    public static void hud(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof Vehicle v) || mc.options.hideGui) return;
        var f = mc.font;
        int kmh = (int) (Math.abs(v.speed) * 20 * 3.6f);
        int cx = w - 60, cy = h - 60, r = 36;
        g.fill(cx - r - 4, cy - r / 2 - 8, cx + r + 4, cy + r / 2 + 14, 0x88000000);
        float max = Vehicle.maxSpeed(v.kind()) * 72;
        for (int i = 0; i <= 20; i++) {
            float a = Mth.PI * (0.85f + 1.3f * i / 20f);
            int x0 = cx + (int) (Mth.cos(a) * (r - 4)), y0 = cy + (int) (Mth.sin(a) * (r - 4)) / 2 + 8;
            boolean lit = i / 20f <= kmh / max;
            g.fill(x0 - 1, y0 - 1, x0 + 2, y0 + 2, lit ? (i > 16 ? 0xFFFF3B30 : 0xFF4CC9F0) : 0x55FFFFFF);
        }
        g.pose().pushPose();
        g.pose().translate(cx, cy - 2, 0);
        g.pose().scale(1.8f, 1.8f, 1);
        g.drawCenteredString(f, String.valueOf(kmh), 0, 0, 0xFFFFFFFF);
        g.pose().popPose();
        g.drawCenteredString(f, "§7km/h", cx, cy + 14, 0xFFFFFFFF);
        String flags = (v.lights() ? "§e◉ " : "§8◉ ") + (v.speed < -0.01f ? "§cR" : "§aD");
        g.drawCenteredString(f, flags, cx, cy - r / 2 - 4, 0xFFFFFFFF);
        g.drawString(f, "§8H horn · L lights · Space handbrake", 6, h - 38, 0xFFFFFFFF, false);
    }
}
