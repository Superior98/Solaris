package com.fireheart.city;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.level.BlockEvent;

/** Weddings, resident diaries, reactions to player builds, the weekly city report and /sol help. */
public final class Finale {
    private Finale() {}

    static final String CITY = "~city";

    /* ------------------------------------------------------------ Weddings */

    static final BlockPos ALTAR = new BlockPos(-20, 71, 30);
    static long wedDay = -1;
    static String wedA = "", wedB = "";
    static int stage;

    static void weddingTick(ServerLevel sl, CityData d) {
        long day = Calendar.worldDay(sl);
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (wedDay != day) {
            wedDay = day;
            stage = 0;
            wedA = wedB = "";
            String plan = d.setting(CITY, "wedPlan", "");
            if (plan.startsWith(day + "|")) {
                String[] s = plan.split("\\|");
                if (s.length == 3) {
                    wedA = s[1];
                    wedB = s[2];
                    stage = 1;
                }
            }
        }
        if (stage == 0 && Calendar.weekend(day) && tod >= 7000 && tod < 10000 && sl.getGameTime() % 100 == 0) plan(sl, d, day);
        if (stage >= 1 && stage < 9) ceremony(sl, d, day, tod);
    }

    static void plan(ServerLevel sl, CityData d, long day) {
        if (d.setting(CITY, "wedLast", "-99").equals(String.valueOf(day)) || Perks.parse(d.setting(CITY, "wedLast", "-99")) > day - 7) return;
        for (CityData.Profile p : d.profiles.values()) {
            if (p.partner.isEmpty() || p.livesOnIsland()) continue;
            CityData.Profile q = d.profiles.get(p.partner);
            if (q == null || q.livesOnIsland() || !q.partner.equals(p.id)) continue;
            String key = "wed:" + Resident.pairKey(p.id, q.id);
            if (d.setting(CITY, key, "").equals("1")) continue;
            CityData.Rel r = d.peekRel(p.id, q.id);
            if (r == null || r.romance < 60) continue;
            wedA = p.id;
            wedB = q.id;
            stage = 1;
            d.setSetting(CITY, "wedPlan", day + "|" + p.id + "|" + q.id);
            d.setSetting(CITY, "wedLast", String.valueOf(day));
            List<String> who = new ArrayList<>();
            for (CityData.Profile x : d.profiles.values()) if (!x.livesOnIsland() && x.job != Job.POLICE && x.job != Job.FIREFIGHTER) who.add(x.id);
            d.addPlan(day, "plaza", "wedding", who.toArray(new String[0]));
            for (Resident res : Skies.residents(sl, d)) res.replan();
            d.news(day, p.name + " and " + q.name + " are getting married at Solaris Plaza this evening!");
            d.event(day, "love", p.name + " and " + q.name + " announced their wedding", ALTAR, p.id, q.id);
            for (ServerPlayer pl : sl.players()) {
                Perks.say(pl, "§d💍 " + p.name + " and " + q.name + " are getting married at Solaris Plaza at 17:00! §7Everyone's invited.");
                Calendar.banner(pl, "§d💍 Wedding today!", "§f" + p.name + " & " + q.name + " §7· Solaris Plaza, 17:00");
            }
            return;
        }
    }

    /** Admin: marry the first couple found, at today's 17:00 ceremony. */
    static String forceWedding(ServerLevel sl, CityData d) {
        long day = Calendar.worldDay(sl);
        for (CityData.Profile p : d.profiles.values()) {
            if (p.partner.isEmpty() || p.livesOnIsland()) continue;
            CityData.Profile q = d.profiles.get(p.partner);
            if (q == null || q.livesOnIsland() || d.setting(CITY, "wed:" + Resident.pairKey(p.id, q.id), "").equals("1")) continue;
            d.setSetting(CITY, "wedLast", "-99");
            CityData.Rel r = d.rel(p.id, q.id);
            r.romance = Math.max(r.romance, 60);
            wedDay = -1;
            stage = 0;
            d.setSetting(CITY, "wedPlan", "");
            plan(sl, d, day);
            wedDay = day;
            return stage == 1 ? "§6Wedding planned for §f" + d.profiles.get(wedA).name + " & " + d.profiles.get(wedB).name + "§6 today. §7(/time set 10800 for the ceremony)" : "§cCouldn't plan the wedding.";
        }
        return "§cNo unmarried city couples right now.";
    }

    static Resident entity(ServerLevel sl, CityData d, String id) {
        CityData.Profile p = d.profiles.get(id);
        return p == null || p.entity == null ? null : sl.getEntity(p.entity) instanceof Resident r ? r : null;
    }

    static void ceremony(ServerLevel sl, CityData d, long day, long tod) {
        if (tod < 10900 || tod > 12600) return;
        Resident a = entity(sl, d, wedA), b = entity(sl, d, wedB);
        CityData.Profile pa = d.profiles.get(wedA), pb = d.profiles.get(wedB);
        if (a == null || b == null || pa == null || pb == null) return;
        if (!a.blockPosition().closerThan(ALTAR, 10) || !b.blockPosition().closerThan(ALTAR, 10)) {
            if (tod > 12200 && stage < 8) {
                stage = 9;
                d.news(day, "The wedding of " + pa.name + " and " + pb.name + " has been postponed.");
                d.setSetting(CITY, "wedPlan", "");
            }
            return;
        }
        a.getLookControl().setLookAt(b, 30, 30);
        b.getLookControl().setLookAt(a, 30, 30);
        List<Resident> guests = sl.getEntitiesOfClass(Resident.class, new AABB(ALTAR).inflate(16, 6, 16), x -> x != a && x != b && x.profile() != null && !x.isSleeping());
        Resident officiant = null;
        CityData.Profile mayor = Mayor.mayor(d);
        if (mayor != null) officiant = entity(sl, d, mayor.id);
        if (officiant == a || officiant == b || officiant == null || officiant.distanceTo(a) > 16) officiant = guests.isEmpty() ? a : guests.get(0);
        long step = (tod - 10900) / 120;
        if (step < stage - 1) return;
        switch (stage) {
            case 1 -> {
                officiant.sayTo("Friends, family, neighbours of Solaris - we're gathered here today for " + pa.name + " and " + pb.name + ".", 140);
                officiant.gesture(Resident.G_WAVE, 60);
                for (Resident g : guests) if (g != officiant) g.getLookControl().setLookAt(a, 30, 30);
            }
            case 2 -> {
                a.sayTo(a.pick(pb.name + ", from the day we met, Solaris felt more like home.", pb.name + ", you make every day brighter. Even the rainy ones."), 140);
                a.gesture(Resident.G_SING, 80);
            }
            case 3 -> {
                b.sayTo(b.pick(pa.name + ", I'd ride a thousand Sky Ferries with you.", pa.name + ", you're my favourite person in this whole city."), 140);
                b.gesture(Resident.G_SING, 80);
            }
            case 4 -> officiant.sayTo(pa.name + ", do you take " + pb.name + "?", 100);
            case 5 -> {
                a.sayTo("I do!", 80);
                a.gesture(Resident.G_NOD, 40);
                officiant.sayTo("And " + pb.name + ", do you take " + pa.name + "?", 100);
            }
            case 6 -> {
                b.sayTo("I do!", 80);
                b.gesture(Resident.G_NOD, 40);
            }
            case 7 -> {
                officiant.sayTo("Then by the power vested in me by the City of Solaris - you're married!", 120);
                a.gesture(Resident.G_HUG, 80);
                b.gesture(Resident.G_HUG, 80);
                a.particles(ParticleTypes.HEART, 12);
                b.particles(ParticleTypes.HEART, 12);
                for (Resident g : guests) {
                    g.gesture(sl.getRandom().nextBoolean() ? Resident.G_CLAP : Resident.G_CONFETTI, 80);
                    if (sl.getRandom().nextFloat() < 0.3f) g.say(g.pick("Woooo!", "Congratulations!", "I'm not crying, YOU'RE crying.", "Kiss! Kiss!"), 60);
                }
                sl.playSound(null, ALTAR, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1.5f, 0.8f);
                Fireworks.burst(sl, ALTAR.above(6), 16, 120, 8);
                CityData.Rel r1 = d.rel(pa.id, pb.id), r2 = d.rel(pb.id, pa.id);
                r1.romance = 100;
                r2.romance = 100;
                d.setSetting(CITY, "wed:" + Resident.pairKey(pa.id, pb.id), "1");
                d.setSetting(CITY, "wedDay:" + Resident.pairKey(pa.id, pb.id), String.valueOf(day));
                Applause.confetti(sl, ALTAR.above(2), 40);
                d.setSetting(CITY, "wedPlan", "");
                d.event(day, "love", pa.name + " and " + pb.name + " got married at Solaris Plaza", ALTAR, pa.id, pb.id);
                pa.mind.remember(pa, day, (int) tod, "love", "I married " + pb.name + " at Solaris Plaza", "plaza", 5, 10);
                pb.mind.remember(pb, day, (int) tod, "love", "I married " + pa.name + " at Solaris Plaza", "plaza", 5, 10);
                pa.log(a.routineDay()).note("I got married to " + pb.name + "!");
                pb.log(b.routineDay()).note("I got married to " + pa.name + "!");
                for (ServerPlayer pl : sl.players()) if (pl.blockPosition().closerThan(ALTAR, 30)) Perks.unlock(pl, d, "wedding");
                d.setDirty();
            }
            case 8 -> Fireworks.finale(sl, ALTAR.above(8));
            default -> {}
        }
        stage++;
    }

    /* ------------------------------------------------------------ Diaries */

    public static String diary(ServerPlayer pl, CityData d, String name) {
        CityData.Profile p = Quests.byName(d, name);
        if (p == null) return "§cNobody called " + name + " lives in Solaris.";
        CityData.Rel pr = d.playerRel(p.id, pl.getName().getString());
        if (!pr.met || pr.aff < 50) return "§7" + p.name + " would rather keep their diary private. §8(Become closer friends first.)";
        long rd = Calendar.day(pl.serverLevel());
        StringBuilder sb = new StringBuilder("§6§l" + p.name + "'s diary");
        DayLog today = p.log(rd);
        DayLog yest = p.log(rd - 1);
        if (yest != null && !yest.empty()) sb.append("\n§7Yesterday: §f").append(yest.story(p, Calendar.weekend(rd - 1)));
        sb.append("\n§7Today: §f").append(today == null || today.empty() ? "Nothing much yet." : today.story(p, Calendar.weekend(rd)));
        Mind m = p.mind;
        if (!m.thought.isEmpty()) sb.append("\n§7Thinking about: §f").append(m.thought);
        if (!m.intent.isEmpty() && m.intentDay == Calendar.worldDay(pl.serverLevel())) sb.append("\n§7Hoping to: §f").append(m.intent);
        Perks.unlock(pl, d, "confidant");
        return sb.toString();
    }

    /* ------------------------------------------------------------ Player builds */

    static final Map<String, Integer> PLACED = new LinkedHashMap<>();

    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        try {
            if (!(e.getEntity() instanceof ServerPlayer pl) || !(e.getLevel() instanceof ServerLevel sl)) return;
            if (!e.getPos().closerThan(ALTAR, 220)) return;
            String pn = pl.getName().getString();
            int n = PLACED.merge(pn, 1, Integer::sum);
            if (n % 50 == 0) {
                CityData d = CityData.get(sl);
                int total = (int) Perks.parse(d.setting(pn, "built", "0")) + 50;
                d.setSetting(pn, "built", String.valueOf(total));
                if (total >= 500) Perks.unlock(pl, d, "architect");
            }
            if (n % 25 != 0) return;
            for (Resident r : sl.getEntitiesOfClass(Resident.class, pl.getBoundingBox().inflate(14), x -> x.profile() != null && x.isFree() && !x.isSpeaking())) {
                if (!r.hasLineOfSight(pl) || !Hobbies.ready("build:" + r.profileId(), sl.getGameTime(), 6000)) continue;
                r.getLookControl().setLookAt(pl, 30, 30);
                r.gesture(n % 50 == 0 ? Resident.G_THUMBS : Resident.G_THINK, 40);
                r.say(r.pick("Ooh, what are you building, " + pn + "?", "That's coming along nicely!", "Is that going to be a new shop? Please say it's a new shop.", "Gus is going to be jealous of that build.", "Can I move in when it's done?"), 60);
                break;
            }
        } catch (Throwable t) {
            FireheartCity.LOG.error("Build reaction failed", t);
        }
    }

    /* ------------------------------------------------------------ Weekly report */

    public static String report(ServerLevel sl, CityData d) {
        long day = Calendar.worldDay(sl);
        Map<String, Integer> kinds = new LinkedHashMap<>();
        List<String> heads = new ArrayList<>();
        for (int i = d.events.size() - 1; i >= 0; i--) {
            CityData.Event e = d.events.get(i);
            if (e.day < day - 6) break;
            kinds.merge(e.kind, 1, Integer::sum);
            if (heads.size() < 4) heads.add(Events.sentence(e.text));
        }
        int wealth = 0, couples = 0, friends = 0, mood = 0;
        CityData.Profile richest = null;
        for (CityData.Profile p : d.profiles.values()) {
            int w = p.coins + Bank.savings(d, p.id);
            wealth += w;
            if (richest == null || w > richest.coins + Bank.savings(d, richest.id)) richest = p;
            if (!p.partner.isEmpty()) couples++;
            friends += d.friendsOf(p.id).size();
            mood += p.mood();
        }
        int n = Math.max(1, d.profiles.size());
        StringBuilder sb = new StringBuilder("§6§lSolaris weekly report §r§7(week ending " + Calendar.stamp(day) + ")");
        sb.append("\n§7Residents: §f").append(d.profiles.size()).append(" §7· Average mood: §f").append(mood / n).append("% §7· Season: ").append(Skies.season(day));
        sb.append("\n§7Money in the city: §f").append(wealth).append(" coins §7· Richest: §f").append(richest == null ? "-" : richest.name);
        sb.append("\n§7Couples: §f").append(couples / 2).append(" §7· Friendships: §f").append(friends / 2);
        if (!kinds.isEmpty()) {
            sb.append("\n§7This week: §f");
            int i = 0;
            for (Map.Entry<String, Integer> k : kinds.entrySet()) sb.append(i++ > 0 ? "§7, §f" : "").append(k.getValue()).append(" ").append(k.getKey());
        }
        for (String h : heads) sb.append("\n§8• §f").append(h).append(".");
        return sb.toString();
    }

    /* ------------------------------------------------------------ Help */

    static final String[][] HELP = {
            {"You", "daily", "achievements", "stats", "rep", "quests", "settings", "home set", "birthday <1-28>"},
            {"People", "friends", "who", "whereis <name>", "profile <name>", "wishlist <name>", "memories <name>", "diary <name>", "couples"},
            {"Do", "courier", "treasure", "treasure hint", "tip <name> <coins>", "mail <name> <msg>", "emote <wave|cheer|dance|bow|clap|laugh>", "selfie <name>", "donate <coins>", "propose"},
            {"City", "calendar", "forecast", "report", "gossip", "top", "album", "horoscope", "bulletin on|off"},
            {"More", "tutorial", "garage", "romance"}
    };

    public static void sendHelp(net.minecraft.commands.CommandSourceStack src) {
        net.minecraft.network.chat.MutableComponent m = net.minecraft.network.chat.Component.literal("§6§l/sol commands §7(click one)");
        for (String[] row : HELP) {
            m.append(net.minecraft.network.chat.Component.literal("\n§7" + row[0] + ": "));
            for (int i = 1; i < row.length; i++) {
                String cmd = "/sol " + row[i];
                boolean args = row[i].contains("<");
                String base = args ? cmd.substring(0, cmd.indexOf('<')) : cmd;
                m.append(net.minecraft.network.chat.Component.literal("§e" + row[i] + (i < row.length - 1 ? "§8, " : "")).withStyle(net.minecraft.network.chat.Style.EMPTY.withClickEvent(new net.minecraft.network.chat.ClickEvent(args ? net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND : net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, base))));
            }
        }
        m.append(net.minecraft.network.chat.Component.literal("\n§7Say to residents in chat: §fjoke, story, fun fact, sing, hug, high five, rock paper scissors, flip a coin, race me to <place>, show me the way to <place>, hide and seek, what should I build?"));
        src.sendSuccess(() -> m, false);
    }

    public static void tick(ServerLevel sl, CityData d) {
        if (sl.getGameTime() % 20 == 19 && FhcConfig.weddings()) weddingTick(sl, d);
    }

    public static void reset() {
        wedDay = -1;
        wedA = wedB = "";
        stage = 0;
        PLACED.clear();
    }
}
