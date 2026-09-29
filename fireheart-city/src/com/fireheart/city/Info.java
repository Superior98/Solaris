package com.fireheart.city;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Player commands that look into city life: calendar, who's around, couples, gossip, memories, selfies and the photo album. */
public final class Info {
    private Info() {}

    public static String calendar(ServerLevel sl, CityData d) {
        long today = Calendar.worldDay(sl);
        StringBuilder sb = new StringBuilder("§6§lSolaris calendar §7(next 14 days)");
        String pw = d.setting(Finale.CITY, "pwed", ""), rw = d.setting(Finale.CITY, "wedPlan", "");
        for (long day = today; day < today + 14; day++) {
            List<String> ev = new ArrayList<>(Happenings.on(day));
            for (ServerPlayer pl : sl.players()) if (Quests.birthday(d, pl.getName().getString(), day)) ev.add("§d🎂 " + pl.getName().getString() + "'s birthday");
            if (pw.startsWith(day + "|")) {
                String[] s = pw.split("\\|");
                CityData.Profile p = s.length == 3 ? d.profiles.get(s[2]) : null;
                ev.add("§d💍 Wedding: " + (p == null ? "?" : p.name) + " & " + (s.length == 3 ? s[1] : "?"));
            }
            if (rw.startsWith(day + "|")) {
                String[] s = rw.split("\\|");
                CityData.Profile a = s.length == 3 ? d.profiles.get(s[1]) : null, b = s.length == 3 ? d.profiles.get(s[2]) : null;
                if (a != null && b != null) ev.add("§d💍 Wedding: " + a.name + " & " + b.name);
            }
            if (ev.isEmpty()) continue;
            sb.append("\n§e").append(day == today ? "Today" : day == today + 1 ? "Tomorrow" : Calendar.name(day)).append(" §8(day ").append(day + 1).append(", ").append(Skies.seasonName(day)).append(")");
            for (String e : ev) sb.append("\n  §7• §f").append(e);
        }
        return sb.toString();
    }

    public static String who(ServerPlayer pl, CityData d) {
        ServerLevel sl = pl.serverLevel();
        List<Resident> near = sl.getEntitiesOfClass(Resident.class, pl.getBoundingBox().inflate(48, 16, 48), r -> r.profile() != null && !r.inShuttle());
        if (near.isEmpty()) return "§7Nobody's around right now.";
        near.sort((a, b) -> Double.compare(a.distanceToSqr(pl), b.distanceToSqr(pl)));
        StringBuilder sb = new StringBuilder("§6§lWho's around §7(" + near.size() + " within 48 blocks)");
        for (int i = 0; i < Math.min(12, near.size()); i++) {
            Resident r = near.get(i);
            CityData.Profile p = r.profile();
            String doing = p.doing == null || p.doing.isEmpty() ? "out and about" : p.doing;
            sb.append("\n§b").append(p.name).append(" §8").append((int) r.distanceTo(pl)).append("m §7- ").append(doing).append(Health.sick(r) ? " §c(sick)" : "");
        }
        return sb.toString();
    }

    public static String couples(ServerLevel sl, CityData d) {
        StringBuilder sb = new StringBuilder("§d§lSolaris couples ♥");
        int n = 0;
        for (CityData.Profile p : d.profiles.values()) {
            if (p.partner.isEmpty() || p.id.compareTo(p.partner) > 0) continue;
            CityData.Profile q = d.profiles.get(p.partner);
            if (q == null) continue;
            boolean wed = d.setting(Finale.CITY, "wed:" + Resident.pairKey(p.id, q.id), "").equals("1");
            sb.append("\n§f").append(p.name).append(" §c❤ §f").append(q.name).append(wed ? " §7(married)" : "");
            n++;
        }
        for (ServerPlayer pl : sl.players()) {
            String pn = pl.getName().getString();
            String rid = Romance.sweetheart(d, pn);
            CityData.Profile p = rid.isEmpty() ? null : d.profiles.get(rid);
            if (p == null) continue;
            boolean wed = d.setting(pn, "spouse", "").equals(rid);
            sb.append("\n§f").append(pn).append(" §c❤ §f").append(p.name).append(wed ? " §7(married)" : " §7(dating)");
            n++;
        }
        return n == 0 ? "§7No couples in Solaris right now. Love is in the air, though..." : sb.toString();
    }

    public static String gossip(CityData d) {
        List<String> out = new ArrayList<>();
        for (int i = d.events.size() - 1; i >= 0 && out.size() < 8; i--) {
            CityData.Event e = d.events.get(i);
            if (!e.kind.equals("social") && !e.kind.equals("love") && !e.kind.equals("money") && !e.kind.equals("career") && !e.kind.equals("status") && !e.kind.equals("fun")) continue;
            out.add(Events.sentence(e.text));
        }
        if (out.isEmpty()) return "§7Nothing juicy lately. Suspicious...";
        String[] lead = {"I heard ", "Word is ", "Apparently ", "Rumour has it ", "Don't tell anyone, but ", "Guess what: "};
        StringBuilder sb = new StringBuilder("§d§lSolaris gossip");
        for (int i = 0; i < out.size(); i++) {
            String s = out.get(i);
            sb.append("\n§7• §f").append(lead[i % lead.length]).append(Character.toLowerCase(s.charAt(0))).append(s.substring(1)).append(".");
        }
        return sb.toString();
    }

    public static String memories(ServerPlayer pl, CityData d, String name) {
        CityData.Profile p = Quests.byName(d, name);
        if (p == null) return "§cNobody called " + name + " lives in Solaris.";
        String pn = pl.getName().getString();
        if (!d.playerRel(p.id, pn).met) return "§7" + p.name + " doesn't know you yet.";
        List<Mind.Ep> eps = new ArrayList<>();
        for (Mind.Ep e : p.mind.eps) if (e.involves("@" + pn)) eps.add(e);
        if (eps.isEmpty()) return "§7" + p.name + " doesn't have any special memories of you yet.";
        long today = Calendar.worldDay(pl.serverLevel());
        StringBuilder sb = new StringBuilder("§6§l" + p.name + " remembers...");
        for (int i = Math.max(0, eps.size() - 8); i < eps.size(); i++) {
            Mind.Ep e = eps.get(i);
            sb.append("\n§7• §f").append(Events.sentence(e.forPlayer(pn))).append(" §8(").append(Calendar.relative(e.day, today)).append(")");
        }
        int trust = p.mind.trustIn(pn);
        sb.append("\n§7Trust: ").append(trust > 30 ? "§atrusts you completely" : trust > 5 ? "§atrusts you" : trust >= -5 ? "§fneutral" : "§cwary of you");
        return sb.toString();
    }

    public static String selfie(ServerPlayer pl, CityData d, String name) {
        CityData.Profile p = Quests.byName(d, name);
        ServerLevel sl = pl.serverLevel();
        if (p == null) return "§cNobody called " + name + " lives in Solaris.";
        Resident r = Perks.find(sl, d, p.name);
        if (r == null || r.distanceTo(pl) > 5) return "§7Stand next to " + p.name + " for a selfie!";
        String pn = pl.getName().getString();
        CityData.Rel pr = d.playerRel(p.id, pn);
        if (!pr.met) return "§7Maybe say hi to " + p.name + " first?";
        if (pr.aff < 0) {
            r.gesture(Resident.G_HEADSHAKE, 40);
            r.sayTo("Uh, no thanks.", 50);
            return "";
        }
        long day = Calendar.worldDay(sl);
        r.getLookControl().setLookAt(pl, 30, 30);
        r.gesture(Resident.G_PHOTO, 45);
        r.showItem("fireheartcity:phone", 50);
        r.sayTo(r.pick("Say cheese!", "Everyone smile! *snap*", "Ooh, a selfie! Get my good side."), 60);
        sl.sendParticles(ParticleTypes.FLASH, r.getX(), r.getEyeY(), r.getZ(), 1, 0, 0, 0, 0);
        sl.playSound(null, r.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.NEUTRAL, 0.5f, 1.6f);
        Place here = r.destination();
        String where = here != null && r.blockPosition().closerThan(here.pos, 12) ? " at " + here.label : "";
        Phones.post(d, p.id, "Selfie with " + pn + where + "! 📸", day, Phones.tod(sl));
        if (!d.setting(pn, "selfie:" + p.id, "").equals(String.valueOf(day))) {
            d.setSetting(pn, "selfie:" + p.id, String.valueOf(day));
            pr.aff = Math.min(100, pr.aff + 2);
            Quests.bump(d, pn, "selfie");
        }
        return "§6" + p.name + " posted your selfie on SolFeed! §7(/sol album)";
    }

    public static String album(ServerPlayer pl, CityData d) {
        String pn = pl.getName().getString();
        List<Phones.Post> mine = new ArrayList<>();
        for (Phones.Post p : d.feed) if (p.text != null && p.text.contains(pn)) mine.add(p);
        if (mine.isEmpty()) return "§7No SolFeed posts mention you yet. Try §f/sol selfie <name>§7!";
        long today = Calendar.worldDay(pl.serverLevel());
        StringBuilder sb = new StringBuilder("§6§lYour Solaris album §7(" + mine.size() + " posts)");
        for (int i = Math.max(0, mine.size() - 10); i < mine.size(); i++) {
            Phones.Post p = mine.get(i);
            sb.append("\n§b").append(d.accountName(p.author)).append(" §8(").append(Calendar.relative(p.day, today)).append(")§7: §f").append(p.text);
        }
        return sb.toString();
    }
}
