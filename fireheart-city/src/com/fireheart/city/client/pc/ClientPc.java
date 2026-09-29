package com.fireheart.city.client.pc;

import com.fireheart.city.PcNet;
import net.minecraft.client.Minecraft;

public final class ClientPc {
    private ClientPc() {}

    public static void show(PcNet.Data d) {
        Minecraft mc = Minecraft.getInstance();
        if (d.device == 5) {
            if (mc.screen instanceof ConsoleScreen cs) cs.update(d);
            else mc.setScreen(new ConsoleScreen(d));
            return;
        }
        if (d.device == 1) {
            if (mc.screen instanceof PhoneScreen ps) ps.update(d);
            else mc.setScreen(new PhoneScreen(d));
            return;
        }
        if (mc.screen instanceof ComputerScreen cs && cs.pos().equals(d.pos)) cs.update(d);
        else mc.setScreen(new ComputerScreen(d));
    }

    static int parse(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    public static void message(String line) {
        Minecraft mc = Minecraft.getInstance();
        if (line.startsWith("#tour|")) {
            com.fireheart.city.client.TourHud.handle(line);
            return;
        }
        if (line.startsWith("#gps|")) {
            com.fireheart.city.client.GpsHud.handle(line);
            return;
        }
        if (line.startsWith("#stalk|")) {
            com.fireheart.city.client.Stalker.handle(line);
            return;
        }
        if (line.startsWith("#fx|")) {
            com.fireheart.city.client.CinemaFx.handle(line);
            return;
        }
        if (line.startsWith("#voice|")) {
            com.fireheart.city.client.VoiceClient.relay(line.endsWith("1"));
            return;
        }
        if (line.startsWith("#call|")) {
            String[] p = line.split("\\|", 4);
            ClientCall.handle(p);
            if (mc.screen instanceof PhoneScreen ps) ps.onCall();
            return;
        }
        if (line.startsWith("#callvoice|")) {
            String[] p = line.split("\\|", 4);
            if (p.length == 4 && mc.player != null) {
                String text = p[3];
                ClientCall.talking = Math.min(160, 10 + text.length() * 2);
                com.fireheart.city.client.VoiceClient.speakCall(-100000L - parse(p[1]), p[2], text, mc.player.getX(), mc.player.getY() + 1.6, mc.player.getZ());
            }
            return;
        }
        if (line.startsWith("#hit|")) {
            com.fireheart.city.client.HitAnim.handle(line);
            return;
        }
        if (line.startsWith("#dev")) {
            com.fireheart.city.client.DeviceAnim.handle(line);
            return;
        }
        if (line.startsWith("#sky")) {
            com.fireheart.city.client.sky.SkyClient.handle(line);
            return;
        }
        if (line.startsWith("#tv|")) {
            TvRender.handle(line);
            return;
        }
        if (line.startsWith("#photo|")) {
            if (mc.screen instanceof OsScreen os) os.showToast("Saved to your album");
            return;
        }
        if (line.startsWith("#battery|")) {
            String[] p = line.split("\\|");
            int lvl = p.length > 1 ? parse(p[1]) : 0;
            boolean ch = p.length > 2 && p[2].equals("1");
            if (mc.screen instanceof PhoneScreen ps) ps.battery(lvl, ch);
            return;
        }
        if (line.startsWith("#toast|")) {
            String[] p = line.split("\\|", 3);
            String title = p.length > 1 ? p[1] : "", body = p.length > 2 ? p[2] : "";
            if (mc.screen instanceof OsScreen os) os.showToast(title + ": " + body);
            else PhoneHud.toast(title, body);
            return;
        }
        if (Receipts.handle(line)) return;
        if (mc.screen instanceof OsScreen os) os.onMessage(line);
        else {
            String[] p = line.split("\\|", 4);
            if (p.length == 4 && p[1].equals("<")) PhoneHud.toast("Message", p[3]);
        }
    }
}
