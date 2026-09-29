package com.fireheart.city.client;

import com.fireheart.city.FireheartCity;
import com.fireheart.city.PhoneItem;
import com.fireheart.city.Phones;
import com.fireheart.city.client.pc.PhoneHud;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FireheartCity.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    public static final KeyMapping PHONE_KEY = new KeyMapping("key.fireheartcity.phone", 80, "key.categories.fireheartcity");

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(FireheartCity.RESIDENT.get(), ResidentRenderer::new);
        event.registerEntityRenderer(FireheartCity.FERRY.get(), FerryRenderer::new);
        event.registerEntityRenderer(FireheartCity.VEHICLE.get(), VehicleRenderer::new);
    }

    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(VehicleRenderer.CAR, VehicleRenderer::car);
        event.registerLayerDefinition(VehicleRenderer.BIKE, VehicleRenderer::bike);
        event.registerLayerDefinition(VehicleRenderer.BOAT, VehicleRenderer::boat);
    }

    @SubscribeEvent
    public static void onParticles(net.minecraftforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(FireheartCity.STEAM.get(), Steam.Provider::new);
    }

    @SubscribeEvent
    public static void onKeys(RegisterKeyMappingsEvent event) {
        event.register(PHONE_KEY);
        event.register(VehicleClient.HORN);
        event.register(VehicleClient.LIGHTS);
    }

    @SubscribeEvent
    public static void onColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint == 0 ? 0xFF000000 | Phones.RGB[PhoneItem.color(stack)] : -1, FireheartCity.PHONE.get());
        event.register((stack, tint) -> {
            if (com.fireheart.city.DeviceItem.magma(stack)) return -1;
            int c = Phones.RGB[com.fireheart.city.DeviceItem.headphoneColor(stack)];
            if (tint == 0) return 0xFF000000 | c;
            if (tint == 1) {
                int r = Math.min(255, ((c >> 16) & 255) / 2 + 128), g = Math.min(255, ((c >> 8) & 255) / 2 + 128), b = Math.min(255, (c & 255) / 2 + 128);
                return 0xFF000000 | r << 16 | g << 8 | b;
            }
            return -1;
        }, FireheartCity.HEADPHONES.get());
    }

    @SubscribeEvent
    public static void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.minecraft.client.renderer.item.ItemProperties.register(FireheartCity.PHONE.get(), new net.minecraft.resources.ResourceLocation(FireheartCity.MODID, "model"),
                (stack, level, entity, seed) -> PhoneItem.model(stack) >= 2 ? 1f : 0f));
        event.enqueueWork(() -> net.minecraft.client.renderer.item.ItemProperties.register(FireheartCity.DISH.get(), new net.minecraft.resources.ResourceLocation(FireheartCity.MODID, "dish"),
                (stack, level, entity, seed) -> com.fireheart.city.Dishes.index(stack)));
        event.enqueueWork(() -> net.minecraft.client.renderer.item.ItemProperties.register(FireheartCity.HEADPHONES.get(), new net.minecraft.resources.ResourceLocation(FireheartCity.MODID, "magma"),
                (stack, level, entity, seed) -> com.fireheart.city.DeviceItem.magma(stack) ? 1f : 0f));
    }

    @SubscribeEvent
    public static void onOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("vehicle_hud", (gui, g, pt, w, h) -> VehicleClient.hud(g, w, h));
        event.registerAboveAll("tour_hud", (gui, g, pt, w, h) -> com.fireheart.city.client.TourHud.hud(g, w, h, pt));
        event.registerAboveAll("stalker_fx", (gui, g, pt, w, h) -> com.fireheart.city.client.Stalker.hud(g, w, h));
        event.registerAboveAll("cinema_fx", (gui, g, pt, w, h) -> com.fireheart.city.client.CinemaFx.hud(g, w, h, pt));
        event.registerAboveAll("video_hud", (gui, g, pt, w, h) -> com.fireheart.city.client.pc.ClientVideo.hud(g, w, h));
        event.registerAboveAll("phone_hud", (gui, g, pt, w, h) -> PhoneHud.render(g, w, h));
        event.registerAboveAll("skydive_hud", (gui, g, pt, w, h) -> com.fireheart.city.client.sky.SkyClient.hud(g, w, h));
    }
}
