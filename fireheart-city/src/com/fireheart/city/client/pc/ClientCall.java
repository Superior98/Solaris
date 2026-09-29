package com.fireheart.city.client.pc;

import java.util.ArrayList;
import java.util.List;

/** Client-side state of the current phone call. */
public final class ClientCall {
    private ClientCall() {}

    public static String state = "", id = "", name = "", info = "";
    public static final List<String[]> lines = new ArrayList<>();
    public static int ticks, endTicks, talking;

    public static boolean active() {
        return !state.isEmpty() && !state.equals("ended");
    }

    public static boolean ringing() {
        return state.equals("incoming");
    }

    public static void dial(String rid, String rname) {
        state = "ring_out";
        id = rid;
        name = rname;
        info = "Calling...";
        lines.clear();
        ticks = 0;
    }

    public static void mine(String text) {
        lines.add(new String[]{"me", text});
    }

    public static void handle(String[] p) {
        if (p.length < 4) return;
        String kind = p[1];
        if (!p[2].equals(id) && !kind.equals("incoming") && !kind.equals("ring_out") && !state.isEmpty() && !state.equals("ended")) return;
        switch (kind) {
            case "ring_out" -> { if (!state.equals("ring_out")) dial(p[2], p[3]); }
            case "incoming" -> {
                state = "incoming";
                id = p[2];
                name = p[3];
                info = "Incoming call";
                lines.clear();
                ticks = 0;
            }
            case "live" -> {
                state = "live";
                id = p[2];
                name = p[3];
                info = "";
                ticks = 0;
            }
            case "voicemail" -> {
                state = "voicemail";
                info = "Voicemail";
                lines.add(new String[]{"sys", p[3]});
                lines.add(new String[]{"sys", "Type a message and press Enter to leave it."});
            }
            case "line" -> lines.add(new String[]{"them", p[3]});
            case "end" -> {
                if (state.isEmpty()) { id = p[2]; name = ""; }
                state = "ended";
                info = p[3];
                lines.add(new String[]{"sys", p[3]});
                endTicks = 80;
            }
            default -> {}
        }
    }

    public static void tick() {
        if (!state.isEmpty()) ticks++;
        if (talking > 0) {
            talking--;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (!com.fireheart.city.client.VoiceClient.available() && mc.player != null && talking % 3 == 0 && mc.player.getRandom().nextFloat() < 0.8f)
                mc.player.playSound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BIT.value(), 0.25f, 0.8f + mc.player.getRandom().nextFloat() * 0.6f);
        }
        if (!state.equals("live")) talking = 0;
        if (state.equals("ended") && --endTicks <= 0) {
            state = "";
            lines.clear();
        }
    }

    public static String timer() {
        int s = ticks / 20;
        return String.format("%d:%02d", s / 60, s % 60);
    }
}
