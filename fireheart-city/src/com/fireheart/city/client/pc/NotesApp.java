package com.fireheart.city.client.pc;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;

class NotesApp extends App {
    private MultiLineEditBox box;

    public String title() { return "Notes"; }

    public int color() { return 0xFFE9A23B; }

    public void addWidgets() {
        String keep = box != null ? box.getValue() : os.data.notes;
        box = new MultiLineEditBox(os.font(), x + 4, y + 4, w - 8, h - 18, Component.literal("Write anything - it's saved on SolNet."), Component.literal("Notes"));
        box.setCharacterLimit(4000);
        box.setValue(keep);
        os.widget(box);
        os.setFocused(box);
    }

    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(x, y, x + w, y + h, 0xFFFFF8DC);
        g.drawString(os.font(), "§8Saved automatically when you close Notes.", x + 4, y + h - 11, 0xFF666666, false);
    }

    public void close() {
        if (box != null) os.send("notes", box.getValue(), "");
    }
}
