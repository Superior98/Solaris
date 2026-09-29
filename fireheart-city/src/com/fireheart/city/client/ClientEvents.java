package com.fireheart.city.client;

import com.fireheart.city.FireheartCity;
import com.fireheart.city.PcNet;
import com.fireheart.city.client.pc.ClientCall;
import com.fireheart.city.client.pc.PhoneHud;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FireheartCity.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        PhoneHud.tick();
        while (ClientSetup.PHONE_KEY.consumeClick()) {
            if (mc.screen != null) continue;
            if (com.fireheart.city.client.pc.ClientVideo.recording()) { com.fireheart.city.client.pc.ClientVideo.stop(); continue; }
            PcNet.CHANNEL.sendToServer(new PcNet.Act(BlockPos.ZERO, "phone_open", "", ""));
            if (ClientCall.ringing()) PcNet.CHANNEL.sendToServer(new PcNet.Act(BlockPos.ZERO, "answer", "", ""));
        }
    }
}
