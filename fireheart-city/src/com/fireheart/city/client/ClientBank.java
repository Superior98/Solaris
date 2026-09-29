package com.fireheart.city.client;

import com.fireheart.city.BankNet;
import net.minecraft.client.Minecraft;

public final class ClientBank {
    private ClientBank() {}

    public static void show(BankNet.State s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof BankScreen bs) bs.update(s);
        else mc.setScreen(new BankScreen(s));
    }
}
