package com.fireheart.city;

import net.minecraft.client.Minecraft;

/** Client-only: feeds the local driver's movement keys into their vehicle. */
final class VehicleDrive {
    private VehicleDrive() {}

    static void drive(Vehicle v) {
        var mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || p.getVehicle() != v || mc.screen != null) {
            v.input(false, false, false, false, false);
            return;
        }
        var in = p.input;
        v.input(in.up, in.down, in.left, in.right, in.jumping);
    }
}
