package com.fireheart.city;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/** Weekly mayoral elections: candidates announced Wednesday, campaigning, Saturday vote, Monday speech, weekly policy. */
public final class Mayor {
    private Mayor() {}

    public static final BlockPos PODIUM = new BlockPos(-20, 71, 28);
    public static final int VOTE_TOD = 6000;
    public static final int SPEECH_START = 10600;
    public static final int SPEECH_END = 11900;
    public static final int PLAYER_VOTES = 3;
    public static final String[] POLICIES = {"savings", "lunch", "festival", "wages"};

    public static String policyName(String p) {
        return switch (p) {
            case "savings" -> "Savings Boost";
            case "lunch" -> "Free Lunch Friday";
            case "festival" -> "Festival Week";
            case "wages" -> "Fair Wages";
            default -> "none";
        };
    }

    public static String policyText(String p) {
        return switch (p) {
            case "savings" -> "bank interest goes up from 3% to 5%";
            case "lunch" -> "the city pays for everyone's food on Fridays";
            case "festival" -> "fireworks over the pier every evening";
            case "wages" -> "a 20% wage bonus for every worker, paid by the city";
            default -> "no changes";
        };
    }

    public static String platform(CityData.Profile p) {
        switch (p.job) {
            case BANKER: return "savings";
            case COOK: case BAKER: case NOODLE_CHEF: case GROCER: return "lunch";
            case FACTORY_WORKER: case QUARRY_WORKER: case CRANE_OPERATOR: case MECHANIC: case ATTENDANT: return "wages";
            case MUSICIAN: case ARCADE_KEEPER: case PILOT: return "festival";
            default: break;
        }
        return switch (p.trait) {
            case FRIENDLY, CHEERFUL -> "lunch";
            case CURIOUS, SHY, GRUMPY -> "savings";
            default -> "festival";
        };
    }

    public static boolean isMayor(CityData d, String id) {
        return !d.civic.mayor.isEmpty() && d.civic.mayor.equals(id);
    }

    public static CityData.Profile mayor(CityData d) {
        return d.civic.mayor.isEmpty() ? null : d.profiles.get(d.civic.mayor);
    }

    public static boolean campaigning(CityData d, long day) {
        int wd = Calendar.weekday(day);
        return d.civic.announceWeek == d.civic.week(day) && d.civic.electionWeek != d.civic.week(day) && wd >= 2 && wd <= 5;
    }

    public static void tick(ServerLevel sl, CityData d) {
        long day = Calendar.worldDay(sl);
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        long week = d.civic.week(day);
        int wd = Calendar.weekday(day);
        if (d.civic.mayor.isEmpty() && d.civic.announceWeek != week && d.profiles.size() >= 4 && wd <= 4) announce(sl, d, day);
        else if (wd == 2 && tod > 500 && d.civic.announceWeek != week) announce(sl, d, day);
        if (d.civic.announceWeek == week && d.civic.electionWeek != week && (wd == 5 && tod >= VOTE_TOD || wd == 6)) elect(sl, d, day);
        if (wd == 0 && tod > 500 && tod < 9000 && d.civic.speechDay != day) planSpeech(d, day);
    }

    public static String announce(ServerLevel sl, CityData d, long day) {
        d.civic.announceWeek = d.civic.week(day);
        d.civic.candidates.clear();
        d.civic.votes.clear();
        List<CityData.Profile> all = new ArrayList<>(d.profiles.values());
        all.sort(Comparator.comparingInt((CityData.Profile p) -> d.statusScore(p) + (isMayor(d, p.id) ? 8 : 0)).reversed());
        java.util.Set<String> used = new java.util.HashSet<>();
        for (CityData.Profile p : all) {
            if (d.civic.candidates.size() >= 3) break;
            if (used.add(platform(p))) d.civic.candidates.add(p.id);
        }
        for (CityData.Profile p : all) {
            if (d.civic.candidates.size() >= 3) break;
            if (!d.civic.candidates.contains(p.id)) d.civic.candidates.add(p.id);
        }
        if (d.civic.candidates.size() < 2) return "not enough residents";
        List<String> names = new ArrayList<>();
        for (String c : d.civic.candidates) {
            CityData.Profile p = d.profiles.get(c);
            names.add(p.name + " (" + policyName(platform(p)) + ")");
            long rd = Calendar.day(sl);
            p.log(rd).note("I'm running for mayor");
        }
        String text = "the mayoral election is on Saturday: " + Economy.join(names);
        d.event(day, "election", text, PODIUM, d.civic.candidates.toArray(new String[0]));
        long sat = day + Math.max(0, 5 - Calendar.weekday(day));
        for (String c : d.civic.candidates) {
            CityData.Profile p = d.profiles.get(c);
            if (!p.livesOnIsland()) for (long dd = day; dd < sat; dd++) if (d.plansFor(c, dd).isEmpty()) d.addPlan(dd, "plaza", "campaign", c);
        }
        for (ServerPlayer pl : sl.players()) ballot(pl, d, true);
        d.setDirty();
        return text;
    }

    public static void ballot(ServerPlayer pl, CityData d, boolean fresh) {
        if (d.civic.candidates.isEmpty() || d.civic.electionWeek == d.civic.announceWeek) {
            CityData.Profile m = mayor(d);
            pl.sendSystemMessage(Component.literal(m == null ? "§6[Election] §7No election is running right now. Candidates are announced on Wednesdays." : "§6[City Hall] §eMayor " + m.name + "§7 · policy: §f" + policyName(d.civic.policy) + "§7 (" + policyText(d.civic.policy) + "). Next candidates are announced on Wednesday."));
            return;
        }
        pl.sendSystemMessage(Component.literal(fresh ? "§6§l🗳 Solaris Election!§r §7Voting closes Saturday at noon. Your founder's vote counts " + PLAYER_VOTES + "x." : "§6🗳 Solaris Election §7- click a name to vote:"));
        String mine = d.civic.votes.get(Bank.playerKey(pl.getName().getString()));
        for (String c : d.civic.candidates) {
            CityData.Profile p = d.profiles.get(c);
            if (p == null) continue;
            String pol = platform(p);
            MutableComponent m = Component.literal("  ");
            m.append(Component.literal("[Vote " + p.name + "]").withStyle(Style.EMPTY.withColor(c.equals(mine) ? ChatFormatting.GREEN : ChatFormatting.GOLD)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/vote " + p.id))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(p.jobTitle() + " · " + CityData.TIERS[p.tier] + "\nPromise: " + policyText(pol))))));
            m.append(Component.literal(" §7" + p.jobTitle() + " - §f" + policyName(pol) + "§7: " + policyText(pol) + (c.equals(mine) ? " §a✔ your vote" : "")));
            pl.sendSystemMessage(m);
        }
    }

    public static int vote(ServerPlayer pl, String who) {
        CityData d = CityData.get(pl.serverLevel());
        long day = Calendar.worldDay(pl.serverLevel());
        if (d.civic.candidates.isEmpty() || d.civic.electionWeek == d.civic.announceWeek) {
            ballot(pl, d, false);
            return 0;
        }
        CityData.Profile p = d.byName(who);
        if (p == null || !d.civic.candidates.contains(p.id)) {
            pl.sendSystemMessage(Component.literal("§c" + who + " isn't on the ballot."));
            ballot(pl, d, false);
            return 0;
        }
        String name = pl.getName().getString();
        boolean fresh = !p.id.equals(d.civic.votes.get(Bank.playerKey(name)));
        d.civic.votes.put(Bank.playerKey(name), p.id);
        CityData.Rel pr = d.playerRel(p.id, name);
        if (fresh) Mind.playerEvent(d, p, name, day, "{P} voted for me for mayor", 3, 6);
        pr.aff = Math.min(100, pr.aff + 8);
        pr.facts.put("vote", String.valueOf(day));
        d.setDirty();
        pl.sendSystemMessage(Component.literal("§a✔ You voted for " + p.name + " (" + policyName(platform(p)) + "). You can change your vote until Saturday noon."));
        Entity e = p.entity == null ? null : pl.serverLevel().getEntity(p.entity);
        if (e instanceof Resident r && r.distanceToSqr(pl) < 20 * 20) {
            r.getLookControl().setLookAt(pl, 30, 30);
            r.gesture(Resident.G_CHEER, 40);
            r.sayTo("You've got my back, " + name + "? Thank you! I won't let you down!", 80);
        }
        return 1;
    }

    static int score(CityData d, CityData.Profile voter, CityData.Profile cand, RandomSource r) {
        if (voter.id.equals(cand.id)) return 1000;
        CityData.Rel rel = d.peekRel(voter.id, cand.id);
        int s = 0;
        if (rel != null) {
            s += rel.aff + rel.fam / 3;
            if (rel.rival) s -= 60;
            if (rel.friend()) s += 15;
        }
        if (voter.partner.equals(cand.id)) s += 80;
        if (platform(voter).equals(platform(cand))) s += 25;
        s += d.statusScore(cand) / 3;
        if (isMayor(d, cand.id)) s += 5;
        s += r.nextInt(20);
        return s;
    }

    public static String preference(CityData d, CityData.Profile voter, RandomSource r) {
        String best = null;
        int bs = Integer.MIN_VALUE;
        for (String c : d.civic.candidates) {
            CityData.Profile p = d.profiles.get(c);
            if (p == null) continue;
            int s = score(d, voter, p, RandomSource.create(voter.id.hashCode() * 31L + c.hashCode() * 7L + d.civic.announceWeek));
            if (s > bs) { bs = s; best = c; }
        }
        return best;
    }

    public static String elect(ServerLevel sl, CityData d, long day) {
        d.civic.electionWeek = d.civic.week(day);
        Map<String, Integer> tally = new LinkedHashMap<>();
        for (String c : d.civic.candidates) tally.put(c, 0);
        RandomSource r = sl.random;
        for (CityData.Profile v : d.profiles.values()) {
            String pick = preference(d, v, r);
            if (pick != null) {
                tally.merge(pick, 1, Integer::sum);
                d.civic.votes.putIfAbsent(v.id, pick);
            }
        }
        for (Map.Entry<String, String> e : d.civic.votes.entrySet()) if (e.getKey().startsWith("player:") && tally.containsKey(e.getValue())) tally.merge(e.getValue(), PLAYER_VOTES, Integer::sum);
        String win = null;
        int best = -1;
        for (Map.Entry<String, Integer> e : tally.entrySet()) {
            int v = e.getValue() * 100 + d.statusScore(d.profiles.get(e.getKey()));
            if (v > best) { best = v; win = e.getKey(); }
        }
        if (win == null) return "no candidates";
        String old = d.civic.mayor;
        CityData.Profile w = d.profiles.get(win);
        d.civic.mayor = win;
        d.civic.policy = platform(w);
        d.civic.electedDay = day;
        d.civic.lastTally.clear();
        d.civic.lastTally.putAll(tally);
        d.civic.terms.merge(win, 1, Integer::sum);
        w.rep += 8;
        long rd = Calendar.day(sl);
        w.log(rd).note(win.equals(old) ? "I was re-elected mayor" : "I was elected Mayor of Solaris");
        List<String> res = new ArrayList<>();
        for (Map.Entry<String, Integer> e : tally.entrySet()) res.add(d.profiles.get(e.getKey()).name + " " + e.getValue());
        String text = (win.equals(old) ? w.name + " was re-elected as mayor" : w.name + " was elected Mayor of Solaris") + " (" + String.join(", ", res) + ") - new policy: " + policyName(d.civic.policy);
        d.event(day, "election", text, PODIUM, win);
        Fireworks.finale(sl, new BlockPos(PODIUM.getX(), PODIUM.getY() + 1, PODIUM.getZ()));
        for (ServerPlayer pl : sl.players()) {
            Calendar.banner(pl, "§6§l🏛 MAYOR " + w.name.toUpperCase(), "§e" + policyName(d.civic.policy) + "§7: " + policyText(d.civic.policy));
            pl.sendSystemMessage(Component.literal("§6[Election results] §f" + String.join(" · ", res) + "§7 · your vote: " + voteName(d, Bank.playerKey(pl.getName().getString()))));
        }
        for (CityData.Profile p : d.profiles.values()) {
            if (p.entity == null) continue;
            Entity e = sl.getEntity(p.entity);
            if (e instanceof Resident res2) res2.refreshLooks(p);
        }
        if (w.entity != null && sl.getEntity(w.entity) instanceof Resident wr) {
            wr.gesture(Resident.G_CHEER, 80);
            wr.particles(ParticleTypes.TOTEM_OF_UNDYING, 25);
            wr.sayTo(wr.pick("Thank you, Solaris! I won't let you down!", "Mayor " + w.name + "... I like the sound of that!", "We did it! " + policyName(d.civic.policy) + " starts today!"), 100);
        }
        for (String c : d.civic.candidates) {
            if (c.equals(win)) continue;
            CityData.Profile l = d.profiles.get(c);
            if (l != null) l.log(rd).note("I lost the mayoral election to " + w.name);
        }
        for (Map.Entry<String, String> v : d.civic.votes.entrySet()) {
            if (!v.getKey().startsWith("player:")) continue;
            String pn = v.getKey().substring(7);
            String body = v.getValue().equals(win)
                    ? "Dear " + pn + ",\n\nThank you for voting for me! With your support, " + policyName(d.civic.policy) + " starts right away: " + policyText(d.civic.policy) + ".\n\nCome to my speech at Solaris Plaza on Monday evening!\n\nMayor " + w.name
                    : "Dear " + pn + ",\n\nThe votes are in and I'm your new mayor. I know you backed someone else, but I'll work hard for everyone.\n\nThis week: " + policyText(d.civic.policy) + ".\n\nMayor " + w.name;
            Post.send(d, "mayor", v.getKey(), body, "mayor", day);
        }
        d.setDirty();
        return text;
    }

    private static String voteName(CityData d, String key) {
        String v = d.civic.votes.get(key);
        CityData.Profile p = v == null ? null : d.profiles.get(v);
        return p == null ? "none" : p.name;
    }

    static void planSpeech(CityData d, long day) {
        d.civic.speechDay = day;
        CityData.Profile m = mayor(d);
        if (m == null) return;
        List<String> who = new ArrayList<>();
        who.add(m.id);
        for (CityData.Profile p : d.profiles.values()) {
            if (p.id.equals(m.id) || p.livesOnIsland() && !m.livesOnIsland()) continue;
            CityData.Rel r = d.peekRel(p.id, m.id);
            int chance = 45 + (r != null && r.friend() ? 30 : 0) - (r != null && r.rival ? 40 : 0);
            if (Math.floorMod(p.id.hashCode() + day * 13, 100) < chance) who.add(p.id);
        }
        String spot = m.livesOnIsland() ? "isle_plaza" : "plaza";
        d.plans.removeIf(pl -> pl.day == day && pl.what.equals("speech"));
        d.addPlan(day, spot, "speech", who.toArray(new String[0]));
        d.event(day, "election", "Mayor " + m.name + " is giving a speech at " + Place.label(spot) + " this evening", Place.get(spot).pos, who.toArray(new String[0]));
    }

    private static final String[] SPEECH_OPEN = {"Citizens of Solaris!", "Friends, neighbours, Solaris!", "Good evening, everyone! Gather round!"};

    public static String speechLine(CityData d, CityData.Profile m, int step, RandomSource r) {
        int savings = 0, couples = 0;
        for (CityData.Profile p : d.profiles.values()) {
            savings += Bank.savings(d, p.id);
            if (!p.partner.isEmpty()) couples++;
        }
        return switch (step % 7) {
            case 0 -> SPEECH_OPEN[r.nextInt(SPEECH_OPEN.length)];
            case 1 -> "This week my policy is " + policyName(d.civic.policy) + " - " + policyText(d.civic.policy) + "!";
            case 2 -> "Together we have " + savings + " coins saved at Hugo's bank. That's a city that plans ahead!";
            case 3 -> couples / 2 > 0 ? "And love is in the air - " + couples / 2 + (couples / 2 == 1 ? " couple" : " couples") + " in town now!" : "And I hear a few of you have crushes. Go on, ask them out!";
            case 4 -> d.civic.lastPrize > 0 ? "Last lottery jackpot: " + d.civic.lastPrize + " coins! Buy your tickets at the bank!" : "Don't forget, the lottery draw is Sunday evening right here!";
            case 5 -> "And a special thanks to Fireheart_4743 for building this wonderful city!";
            default -> "Thank you, Solaris! Now go enjoy your evening!";
        };
    }

    public static String opinion(CityData d, CityData.Profile p) {
        CityData.Profile m = mayor(d);
        if (m == null) return "";
        CityData.Rel r = d.peekRel(p.id, m.id);
        if (r != null && r.rival) return "Honestly? Not a fan.";
        if (platform(p).equals(d.civic.policy)) return policyName(d.civic.policy) + " was exactly what this city needed!";
        if (r != null && r.friend()) return "I'm proud of my friend!";
        return "So far, so good.";
    }

    public static String campaignLine(CityData d, CityData.Profile p, RandomSource r) {
        String pol = platform(p);
        String[] lines = {
                "Vote " + p.name + " for mayor! " + policyName(pol) + " for everyone!",
                "A vote for " + p.name + " means " + policyText(pol) + "!",
                "Election on Saturday! Remember the name: " + p.name + "!",
                "I'm " + p.name + ", " + p.jobTitle().toLowerCase() + ", and I want to be YOUR mayor!",
                "Solaris deserves " + policyName(pol) + ". Vote " + p.name + "!"
        };
        return lines[r.nextInt(lines.length)];
    }

    public static String status(CityData d) {
        CityData.Profile m = mayor(d);
        StringBuilder sb = new StringBuilder();
        sb.append(m == null ? "No mayor yet." : "Mayor " + m.name + " (" + policyName(d.civic.policy) + ": " + policyText(d.civic.policy) + ", " + d.civic.terms.getOrDefault(m.id, 1) + " term(s)).");
        if (!d.civic.candidates.isEmpty() && d.civic.electionWeek != d.civic.announceWeek) {
            List<String> c = new ArrayList<>();
            for (String id : d.civic.candidates) c.add(d.profiles.get(id).name + " [" + policyName(platform(d.profiles.get(id))) + "]");
            sb.append(" Candidates: ").append(String.join(", ", c)).append(".");
        }
        if (!d.civic.lastTally.isEmpty()) {
            List<String> t = new ArrayList<>();
            d.civic.lastTally.forEach((k, v) -> t.add(d.profiles.get(k).name + " " + v));
            sb.append(" Last result: ").append(String.join(", ", t)).append(".");
        }
        return sb.toString();
    }
}
