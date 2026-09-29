package com.fireheart.city.client.pc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;

/** Draws a resident's or player's face (with hat layer) from their skin, for contact lists and threads. */
final class Faces {
    private Faces() {}

    static ResourceLocation skin(OsScreen os, String id) {
        if (id == null || id.startsWith("group:")) return null;
        String name = id.startsWith("player:") ? id.substring(7) : null;
        if (name == null) {
            for (String[] r : os.data.residents) {
                if (r[0].equals(id) && r.length > 5) {
                    try {
                        int s = Integer.parseInt(r[5]);
                        if (s >= 0) return new ResourceLocation("fireheartcity", String.format("textures/entity/residents/skin_%02d.png", Math.floorMod(s, com.fireheart.city.Resident.SKINS)));
                    } catch (NumberFormatException ignored) {}
                }
            }
            return null;
        }
        var conn = Minecraft.getInstance().getConnection();
        PlayerInfo info = conn == null ? null : conn.getPlayerInfo(name);
        return info == null ? null : info.getSkinLocation();
    }

    /** Returns false if there's no face to draw (caller draws its own avatar instead). */
    static boolean draw(GuiGraphics g, OsScreen os, String id, int x, int y, int size) {
        ResourceLocation s = skin(os, id);
        if (s == null) return false;
        PhoneScreen.roundRect(g, x - 1, y - 1, x + size + 1, y + size + 1, 3, 0xFF222222);
        PlayerFaceRenderer.draw(g, s, x, y, size);
        return true;
    }
}
