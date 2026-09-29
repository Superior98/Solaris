package com.fireheart.city;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.util.RandomSource;

/** Small talk about city life: the election, the mayor, the lottery, letters, the busker, fireworks and favours. */
public final class Talk {
    private Talk() {}

    static String pick(RandomSource r, String... o) {
        return Lines.pick(r, o);
    }

    public static boolean talk(Dialogue.Script s, Resident a, Resident b, CityData.Profile pa, CityData.Profile pb, CityData d, long day, RandomSource r, List<Runnable> effects) {
        List<Runnable> opts = new ArrayList<>();
        CityData.Rel ab = d.rel(pa.id, pb.id), ba = d.rel(pb.id, pa.id);
        if (Mayor.campaigning(d, day) && d.civic.candidates.size() >= 2) {
            opts.add(() -> {
                String pref = Mayor.preference(d, pb, r);
                CityData.Profile c = pref == null ? null : d.profiles.get(pref);
                boolean bRuns = d.civic.candidates.contains(pb.id), aRuns = d.civic.candidates.contains(pa.id);
                s.a(aRuns ? "You'll vote for me on Saturday, right, " + pb.name + "?" : "Who are you voting for on Saturday?", Resident.G_THINK);
                if (bRuns) s.b(pick(r, "Myself, obviously! " + Mayor.policyName(Mayor.platform(pb)) + " for everyone!", "Me! Vote " + pb.name + "!"), Resident.G_CHEER);
                else if (c == null) s.b("I haven't decided yet.");
                else if (c.id.equals(pa.id)) s.b("You, of course! " + Mayor.policyName(Mayor.platform(pa)) + " sounds great.", Resident.G_CHEER);
                else s.b(pick(r, c.name + ". " + Mayor.policyName(Mayor.platform(c)) + " is exactly what we need.", "Probably " + c.name + " - I like " + Mayor.policyText(Mayor.platform(c)) + "."));
                if (aRuns && c != null && !c.id.equals(pa.id)) s.a(pick(r, "Hmph. Well, there's still time to change your mind!", "Ouch. I'll win you over yet."), Resident.G_THINK);
            });
        }
        CityData.Profile m = Mayor.mayor(d);
        if (m != null && !m.id.equals(pa.id) && !m.id.equals(pb.id)) {
            opts.add(() -> {
                boolean fresh = day - d.civic.electedDay <= 2;
                s.a(fresh ? "Can you believe " + m.name + " is our new mayor?" : "What do you think of Mayor " + m.name + " so far?");
                s.b(Mayor.opinion(d, pb).isEmpty() ? "Time will tell." : Mayor.opinion(d, pb));
            });
        }
        if (m != null && m.id.equals(pb.id)) {
            opts.add(() -> {
                s.a(pick(r, "Good evening, Mayor " + pb.name + "!", "How's life at the top, Mayor?"), Resident.G_WAVE);
                s.b(pick(r, "Busy! " + Mayor.policyName(d.civic.policy) + " is going really well.", "Ha! Just call me " + pb.name + ", please.", "Come to my speech on Monday!"), Resident.G_CHEER);
            });
        }
        String lt = Lottery.line(d, pa);
        if (lt != null) {
            opts.add(() -> {
                s.a(lt, Resident.G_CHEER);
                int bt = Lottery.tickets(d, pb.id);
                s.b(bt > 0 ? "Ha! I've got " + bt + " myself. May the luckiest win!" : pick(r, "Good luck! I never win anything.", "The jackpot's " + Lottery.pot(d) + " this week, right?", "Maybe I should buy one too..."));
            });
        }
        if (!d.civic.lastWinner.isEmpty() && day - d.civic.lastWinDay <= 2) {
            String w = d.accountName(d.civic.lastWinner);
            if (d.civic.lastWinner.equals(pa.id)) opts.add(() -> {
                s.a("I still can't believe I won " + d.civic.lastPrize + " coins in the lottery!", Resident.G_CHEER);
                s.b(pick(r, "Lucky you! Treat me to lunch?", "Don't spend it all at once!"), Resident.G_CHEER);
            });
            else opts.add(() -> {
                s.a("Did you see " + w + " win the lottery? " + d.civic.lastPrize + " coins!");
                s.b(pick(r, "Some people have all the luck.", "Good for them!", "Next week it's my turn."));
            });
        }
        if (pb.job == Job.MUSICIAN) opts.add(() -> {
            s.a(pick(r, "I loved your song at the plaza, " + pb.name + "!", "Play " + Music.SONGS[r.nextInt(Music.SONGS.length)].name() + " again tomorrow?"), Resident.G_CHEER);
            s.b(pick(r, "Thank you! Come by after work - I play every day.", "For you? Anything!", "Tips welcome, haha!"), Resident.G_WAVE);
        });
        else if (pa.job != Job.MUSICIAN) {
            CityData.Profile mu = null;
            for (CityData.Profile p : d.profiles.values()) if (p.job == Job.MUSICIAN) mu = p;
            if (mu != null && !mu.id.equals(pb.id)) {
                CityData.Profile muf = mu;
                opts.add(() -> {
                    s.a("Have you heard " + muf.name + " play at the plaza? Brilliant.");
                    s.b(pick(r, "Yes! I stopped and listened on my way home.", "Not yet, I'll go this week.", "I tipped them a couple of coins!"));
                });
            }
        }
        if (pb.job == Job.POSTMAN) opts.add(() -> {
            int n = 0;
            for (Post.Letter l : d.civic.mail) if (l.stage < 2 && l.to.equals(pa.id)) n++;
            int nn = n;
            s.a("Anything in the post for me, " + pb.name + "?", Resident.G_THINK);
            s.b(nn > 0 ? "Actually yes - " + (nn == 1 ? "a letter" : nn + " letters") + " in my bag! I'll drop it at your place today." : pick(r, "Nothing today, sorry!", "Not yet - but the day's young!"), nn > 0 ? Resident.G_CHEER : Resident.G_WAVE);
        });
        if (Calendar.weekday(day) == 5 && a.timeOfDay() < 12000) opts.add(() -> {
            s.a("Are you coming to the fireworks over the pier tonight?");
            s.b(pick(r, "Wouldn't miss it!", "Only if you save me a good spot.", "I'm bringing snacks!"), Resident.G_CHEER);
        });
        for (Map.Entry<String, Favours.Request> e : d.civic.favours.entrySet()) {
            if (!e.getKey().equals(pa.id)) continue;
            Favours.Request q = e.getValue();
            opts.add(() -> {
                s.a("I asked " + q.player + " to bring me " + q.count + " " + Favours.noun(q) + ". Fingers crossed!");
                s.b(pick(r, q.player + "? They'll come through, they always do.", "Ooh, what for?", "I hope they remember!"));
            });
        }
        ServerPlayerName helper = null;
        for (ServerPlayerName n : ServerPlayerName.of(d, pa.id)) if (n.rel.facts.containsKey("favour")) helper = n;
        if (helper != null) {
            String item = helper.rel.facts.get("favour");
            String who = helper.name;
            opts.add(() -> {
                s.a(who + " brought me " + item + " when I needed it. So kind!", Resident.G_CHEER);
                s.b(pick(r, "They're the best. They built this whole city, you know.", "That's so like them."));
            });
        }
        if (opts.isEmpty()) return false;
        opts.get(r.nextInt(opts.size())).run();
        return true;
    }

    public static boolean letterThanks(Dialogue.Script s, CityData.Profile pa, CityData.Profile pb, CityData.Rel ab, long day, RandomSource r, List<Runnable> effects) {
        String v = ab.facts.get("letter");
        if (v == null) return false;
        long when = Long.parseLong(v);
        if (day - when > 3) { ab.facts.remove("letter"); return false; }
        s.a(pick(r, "Thank you for your letter, " + pb.name + "! It made my day.", "I got your letter! That was so sweet of you."), Resident.G_CHEER);
        s.b(pick(r, "Aw, I'm glad it arrived! Pip is so reliable.", "I meant every word!", "You're welcome! Write back sometime?"), Resident.G_WAVE);
        effects.add(() -> { ab.facts.remove("letter"); ab.aff += 2; });
        return true;
    }

    record ServerPlayerName(String name, CityData.Rel rel) {
        static List<ServerPlayerName> of(CityData d, String resident) {
            List<ServerPlayerName> out = new ArrayList<>();
            for (String n : d.playerNames(resident)) out.add(new ServerPlayerName(n, d.playerRel(resident, n)));
            return out;
        }
    }
}
