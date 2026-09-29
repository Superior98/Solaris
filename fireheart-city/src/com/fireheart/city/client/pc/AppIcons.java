package com.fireheart.city.client.pc;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Painted app icons (textures/gui/icons/&lt;name&gt;.png); falls back to the old glyph tiles if missing. */
final class AppIcons {
    private AppIcons() {}

    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();

    static ResourceLocation find(String label) {
        String key = label.toLowerCase().replaceAll("[^a-z0-9]", "");
        return CACHE.computeIfAbsent(key, k -> {
            ResourceLocation loc = new ResourceLocation("fireheartcity", "textures/gui/icons/" + k + ".png");
            return Minecraft.getInstance().getResourceManager().getResource(loc).isPresent() ? loc : null;
        });
    }

    static boolean draw(GuiGraphics g, String label, int x, int y, int size) {
        ResourceLocation loc = find(label);
        if (loc == null) return false;
        g.blit(loc, x, y, size, size, 0, 0, 64, 64, 64, 64);
        return true;
    }
}
