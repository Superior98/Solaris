package com.fireheart.city;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Deeper bonds with players: friendship milestones and keepsakes, nicknames, anniversaries and proposals. */
public final class Bonds {
    private Bonds() {}

    /* ------------------------------------------------------------ Friendship milestones */

    static void milestones(ServerLevel sl, CityData d, ServerPlayer pl) {
        String pn = pl.getName().getString();
        for (CityData.Profile p : d.profiles.values()) {
            if (!d.playerNames(p.id).contains(pn)) continue;
            CityData.Rel r = d.playerRel(p.id, pn);
            int reached = (int) Perks.parse(d.setting(pn, "milestone:" + p.id, "0"));
            int next = r.aff >= 80 ? 80 : r.aff >= 50 ? 50 : 0;
            if (next <= reached) continue;
            Resident res = Perks.find(sl, d, p.name);
            boolean near = res != null && res.distanceTo(pl) < 12 && res.isFree();
            d.setSetting(pn, "milestone:" + p.id, String.valueOf(next));
            ItemStack gift = new ItemStack(next == 80 ? Items.NAUTILUS_SHELL : Items.AMETHYST_SHARD);
            gift.setHoverName(Component.literal(next == 80 ? "§dBest Friends Locket §7from " + p.name : "§bFriendship Bracelet §7from " + p.name));
            gift.getOrCreateTag().putString("SolKeepsake", p.id);
            if (!pl.getInventory().add(gift)) pl.drop(gift, false);
            String line = next == 80
                    ? "You're honestly one of my best friends, " + pn + ". I made you this locket. Keep it, okay?"
                    : "We've become proper friends, haven't we? I made you a friendship bracelet!";
            if (near) {
                res.getLookControl().setLookAt(pl, 30, 30);
                res.gesture(Resident.G_GIVE, 50);
                res.particles(ParticleTypes.HEART, 6);
                res.sayTo(line, 120);
            } else {
                Computers.deliver(sl, d, pn, p.id, line + " (I left it with the post office for you.)");
            }
            Perks.say(pl, "§d♥ " + p.name + (next == 80 ? " now counts you as a best friend!" : " now counts you as a friend!") + " §7You received a keepsake.");
            if (next == 80) Perks.unlock(pl, d, "bestie");
            p.mind.remember(p, Calendar.worldDay(sl), (int) Math.floorMod(sl.getDayTime(), 24000L), "player", next == 80 ? "{P} became one of my best friends" : "{P} and I became friends", "", 4, 7, "@" + pn);
            return;
        }
    }

    /* ------------------------------------------------------------ Nicknames */

    public static String nickname(CityData d, String pn, String rid) {
        return d.setting(pn, "nick:" + rid, "");
    }

    /** What this resident calls the player: pet name for a sweetheart, nickname for close friends, otherwise their name. */
    public static String callName(CityData d, CityData.Profile p, String pn, RandomSource rnd) {
        if (p.id.equals(Romance.sweetheart(d, pn))) return Romance.petName(d, pn, p, rnd);
        String n = nickname(d, pn, p.id);
        return n.isEmpty() || rnd.nextBoolean() ? pn : n;
    }

    static String makeNick(String pn, CityData.Profile p) {
        String base = pn.replaceAll("[^A-Za-z]", "");
        if (base.isEmpty()) base = pn;
        String low = base.toLowerCase(Locale.ROOT);
        if (low.contains("fox")) return "Foxy";
        if (low.contains("magma")) return "Lava";
        if (low.contains("fire")) return "Sparky";
        if (low.contains("stellar") || low.contains("star")) return "Star";
        String stem = base.length() > 4 ? base.substring(0, 4) : base;
        stem = stem.substring(0, 1).toUpperCase(Locale.ROOT) + stem.substring(1).toLowerCase(Locale.ROOT);
        return switch (Math.floorMod(p.id.hashCode(), 4)) {
            case 0 -> stem + "y";
            case 1 -> stem + "ster";
            case 2 -> "Lil' " + stem;
            default -> stem + "o";
        };
    }

    /** Gives the player a nickname once a resident is fond enough of them; returns the line announcing it, or null. */
    public static String maybeNickname(Resident r, CityData.Profile p, String pn, CityData.Rel pr) {
        CityData d = r.data();
        if (pr.aff < 60 || !nickname(d, pn, p.id).isEmpty() || p.id.equals(Romance.sweetheart(d, pn))) return null;
        String nick = makeNick(pn, p);
        d.setSetting(pn, "nick:" + p.id, nick);
        r.gesture(Resident.G_LAUGH, 40);
        return "You know what? I'm calling you \"" + nick + "\" from now on. It suits you.";
    }

    /* ------------------------------------------------------------ Anniversaries */

    static void anniversaries(ServerLevel sl, CityData d, long day) {
        if (d.setting(Finale.CITY, "annivDay", "").equals(String.valueOf(day))) return;
        d.setSetting(Finale.CITY, "annivDay", String.valueOf(day));
        for (CityData.Profile p : d.profiles.values()) {
            if (p.partner.isEmpty() || p.id.compareTo(p.partner) > 0) continue;
            CityData.Profile q = d.profiles.get(p.partner);
            if (q == null) continue;
            String wd = d.setting(Finale.CITY, "wedDay:" + Resident.pairKey(p.id, q.id), "");
            if (wd.isEmpty()) continue;
            long since = day - Perks.parse(wd);
            if (since <= 0 || since % 28 != 0) continue;
            String spot = Place.DATE_SPOTS[sl.getRandom().nextInt(Place.DATE_SPOTS.length)];
            d.addPlan(day, spot, "date", p.id, q.id);
            d.news(day, p.name + " and " + q.name + " are celebrating their wedding anniversary today!");
            d.event(day, "love", p.name + " and " + q.name + " celebrated " + (since / 28) + " month" + (since / 28 == 1 ? "" : "s") + " of marriage", Finale.ALTAR, p.id, q.id);
        }
        for (ServerPlayer pl : sl.players()) {
            String pn = pl.getName().getString();
            String rid = Romance.sweetheart(d, pn);
            String since = d.setting(pn, "sweetheartSince", "");
            if (rid.isEmpty() || since.isEmpty()) continue;
            long n = day - Perks.parse(since);
            if (n <= 0 || n % 28 != 0 || !Perks.opt(d, pn, "texts")) continue;
            CityData.Profile p = d.profiles.get(rid);
            if (p == null) continue;
            Computers.deliver(sl, d, pn, rid, "Happy anniversary, " + Romance.petName(d, pn, p, sl.getRandom()) + "! " + (n / 28) + " month" + (n / 28 == 1 ? "" : "s") + " together. ♥ Dinner tonight?");
        }
    }

    /* ------------------------------------------------------------ Goodnight texts */

    static void goodnight(ServerLevel sl, CityData d, long day, long tod) {
        if (tod < 14200 || tod > 14600) return;
        for (ServerPlayer pl : sl.players()) {
            String pn = pl.getName().getString();
            String rid = Romance.sweetheart(d, pn);
            if (rid.isEmpty() || !Perks.opt(d, pn, "texts") || d.setting(pn, "goodnight", "").equals(String.valueOf(day))) continue;
            CityData.Profile p = d.profiles.get(rid);
            if (p == null) continue;
            d.setSetting(pn, "goodnight", String.valueOf(day));
            String pet = Romance.petName(d, pn, p, sl.getRandom());
            Computers.deliver(sl, d, pn, rid, Lines.pick(sl.getRandom(), "Goodnight, " + pet + ". Sweet dreams ♥", "Heading to bed. Thinking of you, " + pet + ".", "Night night! See you tomorrow? ♥", "Today was nice. Goodnight, " + pet + "."));
        }
    }

    /* ------------------------------------------------------------ Proposals and player weddings */

    static String bride = "", groom = "";
    static long pwedDay = -1;
    static int pstage;
    static long vowAt;

    public static String propose(ServerPlayer pl, CityData d) {
        ServerLevel sl = pl.serverLevel();
        String pn = pl.getName().getString();
        String rid = Romance.sweetheart(d, pn);
        if (rid.isEmpty()) return "§7You need a sweetheart before you can propose. §8(Dating happens through chatting with residents.)";
        CityData.Profile p = d.profiles.get(rid);
        if (p == null) return "§cYour sweetheart isn't around any more.";
        if (d.setting(pn, "spouse", "").equals(rid)) return "§d" + p.name + " is already your spouse. ♥";
        Resident r = Perks.find(sl, d, p.name);
        if (r == null || r.distanceTo(pl) > 6) return "§7Find " + p.name + " first - a proposal should be in person!";
        CityData.Rel rel = d.playerRel(rid, pn);
        long day = Calendar.worldDay(sl);
        r.getLookControl().setLookAt(pl, 30, 30);
        if (rel.romance < 80 || p.mind.trustIn(pn) < 0) {
            r.gesture(Resident.G_SURPRISED, 40);
            r.sayTo(r.pick("Oh my gosh. " + pn + "... I love you, but I'm not ready for that yet.", "That's... a big question. Can we wait a little longer?"), 100);
            rel.romance = Math.max(0, rel.romance - 5);
            return "§7" + p.name + " isn't ready yet. §8(Spend more time together - dates, gifts, kind words.)";
        }
        r.gesture(Resident.G_HUG, 60);
        r.particles(ParticleTypes.HEART, 16);
        r.sayTo(r.pick("YES! Yes, a thousand times yes!", "I... yes! Of course I'll marry you, " + pn + "!"), 120);
        sl.playSound(null, r.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1f, 1.2f);
        long wedding = Math.floorMod(sl.getDayTime(), 24000L) < 9500 ? day : day + 1;
        d.setSetting(Finale.CITY, "pwed", wedding + "|" + pn + "|" + rid);
        List<String> who = new ArrayList<>();
        for (CityData.Profile x : d.profiles.values()) if (!x.livesOnIsland() && x.job != Job.POLICE && x.job != Job.FIREFIGHTER) who.add(x.id);
        d.addPlan(wedding, "plaza", "wedding", who.toArray(new String[0]));
        for (Resident res : Crowd.all(sl, d)) res.replan();
        d.news(day, p.name + " and " + pn + " are engaged!");
        d.event(day, "love", p.name + " said yes to " + pn + "'s proposal", r.blockPosition(), p.id);
        for (ServerPlayer o : sl.players()) Perks.say(o, "§d💍 " + pn + " proposed to " + p.name + " - and they said YES! §7The wedding is " + (wedding == day ? "today" : "tomorrow") + " at 17:00, Solaris Plaza.");
        return "";
    }

    static void playerWedding(ServerLevel sl, CityData d, long day, long tod) {
        String plan = d.setting(Finale.CITY, "pwed", "");
        if (!plan.startsWith(day + "|")) return;
        String[] s = plan.split("\\|");
        if (s.length != 3) return;
        if (pwedDay != day) {
            pwedDay = day;
            pstage = 1;
            groom = s[1];
            bride = s[2];
        }
        if (tod < 10900 || tod > 12800 || pstage >= 9) return;
        ServerPlayer pl = sl.getServer().getPlayerList().getPlayerByName(groom);
        CityData.Profile p = d.profiles.get(bride);
        Resident r = p == null ? null : Perks.find(sl, d, p.name);
        if (pl == null || r == null || !pl.blockPosition().closerThan(Finale.ALTAR, 10)) {
            if (pl != null && tod % 400 < 20 && pstage == 1) pl.displayClientMessage(Component.literal("§d💍 Your wedding is at Solaris Plaza! " + Perks.compass(Finale.ALTAR.getX() - pl.getX(), Finale.ALTAR.getZ() - pl.getZ())), true);
            if (tod > 12600) {
                pstage = 9;
                d.setSetting(Finale.CITY, "pwed", "");
                d.news(day, "The wedding of " + (p == null ? "a resident" : p.name) + " and " + groom + " was postponed.");
            }
            return;
        }
        if (!r.blockPosition().closerThan(Finale.ALTAR, 10)) r.follow(pl, 600);
        r.getLookControl().setLookAt(pl, 30, 30);
        Resident off = null;
        CityData.Profile mayor = Mayor.mayor(d);
        if (mayor != null && !mayor.id.equals(p.id)) off = Perks.find(sl, d, mayor.name);
        if (off == null || off.distanceTo(r) > 16) {
            for (Resident g : sl.getEntitiesOfClass(Resident.class, new net.minecraft.world.phys.AABB(Finale.ALTAR).inflate(16, 6, 16), x -> x != r && x.profile() != null)) { off = g; break; }
        }
        if (off == null) off = r;
        long step = (tod - 10900) / 120;
        if (step < pstage - 1) return;
        switch (pstage) {
            case 1 -> off.sayTo("Everyone, welcome! Today " + p.name + " marries " + groom + ".", 140);
            case 2 -> r.sayTo(r.pick(groom + ", you walked into Solaris and turned my whole world upside down.", "I never thought I'd find someone like you, " + groom + "."), 140);
            case 3 -> off.sayTo(p.name + ", do you take " + groom + "?", 100);
            case 4 -> {
                r.sayTo("I do! ♥", 80);
                off.sayTo(groom + ", do you take " + p.name + "? §7(say \"I do\" in chat)", 400);
                vowAt = sl.getGameTime();
                Perks.say(pl, "§d💍 Type §fI do§d in chat!");
            }
            case 5 -> {
                if (sl.getGameTime() - vowAt > 1200) {
                    off.sayTo("...Well, I'll take that as a yes!", 100);
                    pstage = 6;
                }
                return;
            }
            case 6 -> {
                off.sayTo("By the power vested in me by the City of Solaris - you're married! You may kiss!", 140);
                r.gesture(Resident.G_BLOW_KISS, 40);
                r.particles(ParticleTypes.HEART, 20);
                for (Resident g : sl.getEntitiesOfClass(Resident.class, new net.minecraft.world.phys.AABB(Finale.ALTAR).inflate(16, 6, 16), x -> x != r && x.profile() != null && !x.isSleeping())) {
                    g.gesture(sl.getRandom().nextBoolean() ? Resident.G_CLAP : Resident.G_CONFETTI, 60);
                    if (sl.getRandom().nextFloat() < 0.3f) g.say(g.pick("Congratulations!!", "Kiss! Kiss!", "Best wedding ever!", "I'm not crying!"), 60);
                }
                Applause.confetti(sl, Finale.ALTAR.above(2), 40);
                Fireworks.burst(sl, Finale.ALTAR.above(6), 18, 140, 8);
                d.setSetting(pn(pl), "spouse", p.id);
                d.setSetting(Finale.CITY, "pwed", "");
                CityData.Rel rel = d.playerRel(p.id, groom);
                rel.romance = 100;
                rel.aff = Math.min(100, rel.aff + 10);
                p.mind.remember(p, day, (int) tod, "love", "I married {P} at Solaris Plaza", "plaza", 5, 10, "@" + groom);
                p.log(r.routineDay()).note("I married " + groom + "!");
                d.event(day, "love", p.name + " married " + groom + " at Solaris Plaza", Finale.ALTAR, p.id);
                Calendar.banner(pl, "§d💍 Just married!", "§f" + groom + " & " + p.name);
                Perks.unlock(pl, d, "married");
                for (ServerPlayer o : sl.players()) if (o != pl && o.blockPosition().closerThan(Finale.ALTAR, 30)) Perks.unlock(o, d, "wedding");
                d.setDirty();
            }
            case 7 -> Fireworks.finale(sl, Finale.ALTAR.above(8));
            default -> {}
        }
        pstage++;
    }

    static String pn(ServerPlayer pl) {
        return pl.getName().getString();
    }

    /** Handles the player's "I do" during their wedding; returns the officiant's reply or null. */
    public static String vowChat(ServerPlayer pl, String t) {
        if (pstage != 5 || !pl.getName().getString().equals(groom) || !Intents.any(t, " i do ", " yes ", " i will ")) return null;
        pstage = 6;
        return "They said I do! ♥";
    }

    public static void reset() {
        bride = groom = "";
        pwedDay = -1;
        pstage = 0;
    }

    public static void tick(ServerLevel sl, CityData d) {
        long gt = sl.getGameTime();
        long day = Calendar.worldDay(sl);
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (gt % 200 == 83) for (ServerPlayer pl : sl.players()) milestones(sl, d, pl);
        if (gt % 200 == 97 && tod > 1000 && tod < 3000) anniversaries(sl, d, day);
        if (gt % 40 == 13) goodnight(sl, d, day, tod);
        if (gt % 20 == 7 && FhcConfig.weddings()) playerWedding(sl, d, day, tod);
    }
}
