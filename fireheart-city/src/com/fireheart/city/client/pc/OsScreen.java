package com.fireheart.city.client.pc;

import com.fireheart.city.PcNet;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Shared base of FireOS (PC) and FirePhone OS: data, networking and widget helpers used by apps. */
public abstract class OsScreen extends Screen {
    public PcNet.Data data;
    protected String toast = "";
    protected int toastTicks;

    protected final java.util.Map<String, Integer> unreadBy = new java.util.HashMap<>();

    protected OsScreen(Component title, PcNet.Data d) {
        super(title);
        this.data = d;
        loadUnread();
    }

    protected void loadUnread() {
        unreadBy.clear();
        for (String s : data.unread) {
            int bar = s.lastIndexOf('|');
            if (bar <= 0) continue;
            try { unreadBy.put(s.substring(0, bar), Integer.parseInt(s.substring(bar + 1))); } catch (NumberFormatException ignored) {}
        }
    }

    public int unread(String id) {
        return unreadBy.getOrDefault(id, 0);
    }

    public int unreadTotal() {
        int n = 0;
        for (int v : unreadBy.values()) n += v;
        return n;
    }

    public void markRead(String id) {
        if (id == null) return;
        if (unreadBy.remove(id) != null) send("read", id, "");
    }

    protected void countIncoming(String id, boolean looking) {
        if (looking) send("read", id, "");
        else unreadBy.merge(id, 1, Integer::sum);
    }

    public abstract boolean phone();

    public abstract void onMessage(String line);

    public abstract void openAppById(String id);

    public BlockPos pos() {
        return data.pos;
    }

    public Font font() {
        return font;
    }

    public void send(String kind, String a, String b) {
        PcNet.CHANNEL.sendToServer(new PcNet.Act(data.pos, kind, a, b));
    }

    public void showToast(String text) {
        toast = text;
        toastTicks = 100;
    }

    public <T extends GuiEventListener & Renderable & NarratableEntry> T widget(T w) {
        return addRenderableWidget(w);
    }

    public void clearWidgetsPublic() {
        clearWidgets();
    }

    public void removeW(GuiEventListener w) {
        removeWidget(w);
    }

    public void submitScore(String game, int score) {
        if (score > 0) send("score", game, String.valueOf(score));
    }

    public int best(String game, String who) {
        for (String s : data.scores) {
            String[] p = s.split("\\|");
            if (p.length == 3 && p[0].equals(game) && p[1].equals(who)) return Integer.parseInt(p[2]);
        }
        return 0;
    }

    public String residentName(String id) {
        for (String[] r : data.residents) if (r[0].equals(id)) return r[1];
        return id;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
