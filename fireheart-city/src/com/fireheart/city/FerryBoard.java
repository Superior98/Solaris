package com.fireheart.city;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;

/** Live departure boards (signs) at both Sky Ferry pads. */
public final class FerryBoard {
    private FerryBoard() {}

    public static final BlockPos CITY_BOARD = new BlockPos(13, 71, 88);
    public static final BlockPos ISLE_BOARD = new BlockPos(18, 181, 234);

    public static void tick(ServerLevel sl, CityData d) {
        Ferry f = Ferry.find(sl);
        update(sl, CITY_BOARD, f, Ferry.CITY);
        update(sl, ISLE_BOARD, f, Ferry.ISLE);
    }

    private static void update(ServerLevel sl, BlockPos pos, Ferry f, int side) {
        if (!sl.isLoaded(pos)) return;
        BlockEntity be = sl.getBlockEntity(pos);
        if (!(be instanceof SignBlockEntity sign)) return;
        String l2, l3, l4;
        if (f == null) {
            l2 = "Out of service";
            l3 = "";
            l4 = "";
        } else if (f.dockedAt() == side) {
            l2 = f.grounded() ? "§cGROUNDED - storm" : "§aNOW BOARDING";
            l3 = "to " + (side == Ferry.CITY ? "Neon Heights" : "the city");
            l4 = f.hasPilot() ? "Captain Jet" : "Autopilot";
        } else if (f.dockedAt() >= 0) {
            l2 = f.grounded() ? "§cGROUNDED - storm" : "At " + (side == Ferry.CITY ? "Neon Heights" : "the city pad");
            l3 = "Waiting: " + f.waitingAt(sl, side);
            l4 = f.hasPilot() ? "Captain Jet" : "Autopilot";
        } else {
            int eta = f.etaSeconds();
            boolean coming = f.targetSide() == side;
            l2 = coming ? "§eArriving ~" + eta + "s" : "Departed";
            l3 = coming ? "from " + (side == Ferry.CITY ? "Neon Heights" : "the city") : "next in ~" + (eta + 25) + "s";
            l4 = f.getPassengers().size() + " aboard";
        }
        SignText t = new SignText()
                .setMessage(0, Component.literal("SKY FERRY"))
                .setMessage(1, Component.literal(l2))
                .setMessage(2, Component.literal(l3))
                .setMessage(3, Component.literal(l4))
                .setColor(DyeColor.ORANGE)
                .setHasGlowingText(true);
        SignText old = sign.getFrontText();
        boolean same = true;
        for (int i = 0; i < 4; i++) if (!old.getMessage(i, false).getString().equals(t.getMessage(i, false).getString())) same = false;
        if (same) return;
        sign.setText(t, true);
        sign.setChanged();
        BlockState st = sl.getBlockState(pos);
        sl.sendBlockUpdated(pos, st, st, 3);
    }
}
