package com.fireheart.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Scheduled city happenings: the Sunday fishing tournament, Thursday quiz night, Friday karaoke, Saturday movie night,
 * the fortnightly fun run, summer beach days, Spooky Night, the Starlight Festival, Blossom Day and first snow / first bloom.
 */
public final class Happenings {
    private Happenings() {}

    static final int MON = 0, THU = 3, FRI = 4, SAT = 5, SUN = 6;
    static final Map<String, Long> FORCED = new HashMap<>();

    /** True if an admin forced this event to happen today with /city event. */
    public static boolean forced(String key, long day) {
        Long d = FORCED.get(key);
        return d != null && d == day;
    }

    public static String force(ServerLevel sl, CityData d, String key) {
        long day = Calendar.worldDay(sl);
        return switch (key) {
            case "wedding" -> Finale.forceWedding(sl, d);
            case "rainbow" -> { Skies.rainbow = 1600; Skies.doubleRainbow = sl.getRandom().nextBoolean(); yield "§6Rainbow in the northern sky."; }
            case "sick" -> {
                List<CityData.Profile> all = new ArrayList<>(d.profiles.values());
                if (all.isEmpty()) yield "§cNo residents.";
                CityData.Profile p = all.get(sl.getRandom().nextInt(all.size()));
                d.setSetting("res:" + p.id, Health.K, String.valueOf(day));
                for (Resident r : Crowd.all(sl, d)) if (p.id.equals(r.profileId())) r.replan();
                yield "§6" + p.name + " caught a cold for today.";
            }
            case "meteor", "lantern", "kindness", "spooky", "starlight", "blossom", "quiz", "karaoke", "movie", "fishing", "run", "beach" -> {
                FORCED.put(key, day);
                for (String k : d.settings.keySet().toArray(new String[0])) if (k.startsWith(Finale.CITY + "|hp:")) d.settings.remove(k);
                yield "§6Forced §f" + key + "§6 for today. " + switch (key) {
                    case "meteor" -> "Shooting stars start after 19:30 (/time set 13600).";
                    case "lantern" -> "Residents gather at 16:25 and lanterns fly 17:50-19:20 (/time set 10400).";
                    case "quiz" -> "Questions start 17:24 at the library (/time set 11300).";
                    case "karaoke" -> "Karaoke 17:00-18:30 at the beach bar (/time set 10300).";
                    case "movie" -> "Movie night 16:54-18:36 at the cinema (/time set 10100).";
                    case "fishing" -> "Tournament 09:00-14:00 at the boardwalk (/time set 2300).";
                    case "run" -> "Fun run starts 08:30 at the plaza (/time set 1100).";
                    case "beach" -> "Beach day plans at 08:30 (/time set 2600).";
                    case "spooky" -> "Spooky Night runs after 18:30 (/time set 12500).";
                    default -> "Runs today.";
                };
            }
            default -> "§7Events: wedding, rainbow, sick, meteor, lantern, kindness, spooky, starlight, blossom, quiz, karaoke, movie, fishing, run, beach";
        };
    }

    public static boolean spooky(long day) {
        return Math.floorMod(day, 28L) == 18 || forced("spooky", day);
    }

    public static boolean starlight(long day) {
        return Math.floorMod(day, 28L) == 25 || forced("starlight", day);
    }

    public static boolean blossom(long day) {
        return Math.floorMod(day, 28L) == 3 || forced("blossom", day);
    }

    public static boolean funRun(long day) {
        return Math.floorMod(day, 14L) == 5 || forced("run", day);
    }

    static boolean isDay(int wd, int want, String key, long day) {
        return wd == want || forced(key, day);
    }

    /** Everything happening on a given day, for the calendar and bulletin. */
    public static List<String> on(long day) {
        List<String> out = new ArrayList<>();
        int wd = Calendar.weekday(day);
        String hol = Skies.holiday(day);
        if (hol != null) out.add(hol);
        if (spooky(day)) out.add("Spooky Night §7- jack o'lanterns and treats after dark");
        if (starlight(day)) out.add("Starlight Festival §7- presents and a star over the plaza");
        if (blossom(day)) out.add("Blossom Day §7- flowers for everyone");
        if (funRun(day)) out.add("Solaris Fun Run §7- 08:30 from the plaza to the old pier");
        if (wd == SUN) out.add("Fishing Tournament §7- 09:00-14:00 at the boardwalk");
        if (wd == THU) out.add("Quiz Night §7- 17:30 at the library (answer in chat!)");
        if (wd == FRI && Place.get("beach_bar") != null) out.add("Karaoke Night §7- 17:00 at the Magma Beach Bar");
        if (wd == SAT && Place.get("cinema") != null) out.add("Movie Night §7- 16:30 at the Solaris Cinema");
        if (Skies.seasonIndex(day) == 1 && Calendar.weekend(day) && Place.get("beach") != null) out.add("Beach Day §7- everyone's heading to the beach");
        if (wd == 2 || wd == SAT) out.add("Firework show §7- over the bay after dark");
        return out;
    }

    static List<String> everyone(CityData d, java.util.function.Predicate<CityData.Profile> who) {
        List<String> ids = new ArrayList<>();
        for (CityData.Profile p : d.profiles.values()) if (!p.livesOnIsland() && p.job != Job.POLICE && p.job != Job.FIREFIGHTER && who.test(p)) ids.add(p.id);
        return ids;
    }

    static void plan(ServerLevel sl, CityData d, long day, String key, String place, String what, java.util.function.Predicate<CityData.Profile> who) {
        if (Place.get(place) == null || d.setting(Finale.CITY, "hp:" + key, "").equals(String.valueOf(day))) return;
        d.setSetting(Finale.CITY, "hp:" + key, String.valueOf(day));
        List<String> ids = everyone(d, who);
        if (ids.isEmpty()) return;
        d.addPlan(day, place, what, ids.toArray(new String[0]));
        for (Resident r : Crowd.all(sl, d)) if (ids.contains(r.profileId())) r.replan();
    }

    static List<Resident> at(ServerLevel sl, String place, double radius) {
        Place p = Place.get(place);
        if (p == null) return List.of();
        return sl.getEntitiesOfClass(Resident.class, new AABB(p.pos).inflate(radius, 8, radius), r -> r.profile() != null && !r.isSleeping() && r.convo == null);
    }

    public static void tick(ServerLevel sl, CityData d) {
        if (!FhcConfig.festivals()) return;
        long gt = sl.getGameTime();
        long day = Calendar.worldDay(sl);
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        int wd = Calendar.weekday(day);
        boolean sun = isDay(wd, SUN, "fishing", day), thu = isDay(wd, THU, "quiz", day), fri = isDay(wd, FRI, "karaoke", day), sat = isDay(wd, SAT, "movie", day);
        boolean beach = Skies.seasonIndex(day) == 1 && Calendar.weekend(day) || forced("beach", day);
        if (gt % 40 == 5) {
            if (sun) fishingPlan(sl, d, day, tod);
            if (thu && tod > 10200 && tod < 11000) plan(sl, d, day, "quiz", "library", "quiz", p -> p.trait == Trait.CURIOUS || p.trait == Trait.TALKATIVE || p.trait == Trait.FRIENDLY);
            if (fri && tod > 10200 && tod < 11000) plan(sl, d, day, "karaoke", "beach_bar", "karaoke", p -> p.trait != Trait.SHY && p.trait != Trait.GRUMPY);
            if (sat && tod > 10000 && tod < 10800) plan(sl, d, day, "movie", "cinema", "movie", p -> true);
            if (beach && tod > 2500 && tod < 4000 && !sl.isRaining()) plan(sl, d, day, "beach", "beach", "sunbathe", p -> p.trait != Trait.GRUMPY);
            if (funRun(day) && tod > 1000 && tod < 2400) plan(sl, d, day, "run", "plaza", "funrun", p -> p.trait == Trait.ADVENTUROUS || p.trait == Trait.CHEERFUL || p.trait == Trait.FRIENDLY || p.trait == Trait.TALKATIVE);
        }
        if (sun) fishingTick(sl, d, day, tod);
        if (thu) quizTick(sl, d, day, tod);
        if (fri && tod > 11000 && tod < 12500 && gt % 20 == 3) karaokeTick(sl, d, tod);
        if (sat && tod > 10900 && tod < 12600 && gt % 20 == 7) movieTick(sl);
        if (funRun(day)) runTick(sl, d, day, tod);
        if (gt % 20 == 15) {
            if (spooky(day)) spookyTick(sl, d, tod);
            if (starlight(day)) starlightTick(sl, d, day, tod);
            if (blossom(day)) blossomTick(sl, d, day);
            firsts(sl, d, day, tod);
        }
    }

    public static void reset() {
        FORCED.clear();
        TOURNEY.clear();
        tourneyDay = -1;
        QUIZ_SCORES.clear();
        quizDay = -1;
        quizQ = -1;
        RUNNERS.clear();
        FINISHED.clear();
        runDay = -1;
    }

    /* ------------------------------------------------------------ Fishing tournament */

    static final Map<String, Integer> TOURNEY = new HashMap<>();
    static long tourneyDay = -1;
    static boolean tourneyOpen;

    static void fishingPlan(ServerLevel sl, CityData d, long day, long tod) {
        if (tod > 2200 && tod < 2800) plan(sl, d, day, "fish", "boardwalk", "fishing", p -> p.trait == Trait.SHY || p.trait == Trait.LAIDBACK || p.trait == Trait.ADVENTUROUS || p.trait == Trait.CURIOUS);
    }

    static void fishingTick(ServerLevel sl, CityData d, long day, long tod) {
        if (tourneyDay != day) {
            tourneyDay = day;
            TOURNEY.clear();
            tourneyOpen = false;
        }
        if (!tourneyOpen && tod >= 3000 && tod < 8000) {
            tourneyOpen = true;
            for (CityData.Profile p : d.profiles.values()) TOURNEY.put(p.id, -p.fish);
            for (ServerPlayer pl : sl.players()) Perks.say(pl, "§b🎣 The Solaris Fishing Tournament has started at the boardwalk! §7Catch the most fish by 14:00 to win.");
        }
        if (tourneyOpen && tod >= 8000) {
            tourneyOpen = false;
            String best = null;
            int bestN = 0;
            for (CityData.Profile p : d.profiles.values()) {
                int n = TOURNEY.getOrDefault(p.id, -p.fish) + p.fish;
                if (n > bestN) { bestN = n; best = p.name; }
            }
            ServerPlayer champ = null;
            for (Map.Entry<String, Integer> e : TOURNEY.entrySet()) {
                if (!e.getKey().startsWith("@")) continue;
                if (e.getValue() > bestN) {
                    bestN = e.getValue();
                    best = e.getKey().substring(1);
                    champ = sl.getServer().getPlayerList().getPlayerByName(best);
                }
            }
            if (best == null) {
                d.news(day, "Nobody caught a thing at this week's fishing tournament. The fish won.");
                return;
            }
            d.news(day, best + " won the Solaris Fishing Tournament with " + bestN + " fish!");
            for (ServerPlayer pl : sl.players()) Perks.say(pl, "§b🎣 Tournament over! §f" + best + "§b wins with §f" + bestN + "§b fish!");
            if (champ != null) {
                Perks.reward(champ, d, 30, "Fishing Tournament champion");
                Calendar.banner(champ, "§b🎣 Tournament champion!", "§e+30 coins");
                Perks.unlock(champ, d, "champion");
            }
        }
    }

    /** Counts a player's catch towards the tournament while it's running. */
    static void playerFish(ServerLevel sl, CityData d, String pn) {
        if (!tourneyOpen || tourneyDay != Calendar.worldDay(sl)) return;
        TOURNEY.merge("@" + pn, 1, Integer::sum);
    }

    /* ------------------------------------------------------------ Quiz night */

    record Q(String q, String... a) {}

    static final Q[] QUIZ = {
            new Q("What's the name of the apartment tower in Solaris?", "ember heights", "ember"),
            new Q("What's the floating island above the city called?", "neon heights"),
            new Q("Which mob hisses before it explodes?", "creeper"),
            new Q("How many days does a season last in Solaris?", "7", "seven"),
            new Q("What's the name of the city's phone?", "solphone", "sol phone"),
            new Q("Which ore is rarest: diamond, emerald or gold?", "emerald"),
            new Q("How many hearts of health does a player have?", "10", "ten"),
            new Q("What's the bank teller's name?", "hugo"),
            new Q("What colour is redstone dust?", "red"),
            new Q("Which block makes a nether portal frame?", "obsidian"),
            new Q("What does a composter turn plants into?", "bone meal", "bonemeal"),
            new Q("Which animal gives you wool?", "sheep"),
            new Q("What's the Sky Ferry pilot's name?", "jet"),
            new Q("What's the noodle bar on Neon Heights called?", "neon noodle", "noodle bar"),
            new Q("Water flowing onto lava source makes which block?", "obsidian"),
            new Q("What do you feed a horse to breed it: golden apples or bread?", "golden apple", "golden apples", "golden carrot"),
    };

    static final Map<String, Integer> QUIZ_SCORES = new LinkedHashMap<>();
    static long quizDay = -1, quizAskedAt;
    static int quizQ = -1, quizN;
    static boolean quizAnswered;
    static final BlockPos LIBRARY = new BlockPos(34, 71, -34);

    static void quizTick(ServerLevel sl, CityData d, long day, long tod) {
        if (quizDay != day) {
            quizDay = day;
            quizN = 0;
            quizQ = -1;
            QUIZ_SCORES.clear();
        }
        long now = sl.getGameTime();
        if (tod >= 11000 && tod < 11020 && now % 20 == 0) {
            for (ServerPlayer pl : sl.players()) Perks.say(pl, "§e❓ Quiz Night starts in a moment at the library! §7Stand nearby and answer in chat.");
        }
        if (tod < 11400 || quizN >= 5 && quizQ < 0) return;
        if (quizQ < 0 && quizN < 5) {
            java.util.Random r = new java.util.Random(day * 977L + quizN);
            quizQ = r.nextInt(QUIZ.length);
            quizAskedAt = now;
            quizAnswered = false;
            quizN++;
            for (ServerPlayer pl : near(sl)) Perks.say(pl, "§e❓ Question " + quizN + "/5: §f" + QUIZ[quizQ].q());
            List<Resident> host = at(sl, "library", 18);
            if (!host.isEmpty()) host.get(0).sayTo("Question " + quizN + ": " + QUIZ[quizQ].q(), 120);
            return;
        }
        if (quizQ >= 0 && now - quizAskedAt == 240 && !quizAnswered) {
            List<Resident> crowd = at(sl, "library", 18);
            if (!crowd.isEmpty() && sl.getRandom().nextFloat() < 0.6f) {
                Resident r = crowd.get(sl.getRandom().nextInt(crowd.size()));
                r.gesture(Resident.G_CHEER, 40);
                r.sayTo("Ooh! It's " + QUIZ[quizQ].a()[0] + "!", 80);
                QUIZ_SCORES.merge(r.profile().name, 1, Integer::sum);
                for (ServerPlayer pl : near(sl)) Perks.say(pl, "§7" + r.profile().name + " got it: §f" + QUIZ[quizQ].a()[0]);
                quizAnswered = true;
            }
        }
        if (quizQ >= 0 && now - quizAskedAt >= 400) {
            if (!quizAnswered) for (ServerPlayer pl : near(sl)) Perks.say(pl, "§7Time's up! The answer was §f" + QUIZ[quizQ].a()[0] + "§7.");
            quizQ = -1;
            if (quizN >= 5) finishQuiz(sl, d, day);
        }
    }

    static List<ServerPlayer> near(ServerLevel sl) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer pl : sl.players()) if (pl.blockPosition().closerThan(LIBRARY, 48)) out.add(pl);
        return out;
    }

    static void finishQuiz(ServerLevel sl, CityData d, long day) {
        if (QUIZ_SCORES.isEmpty()) return;
        String best = null;
        int bs = -1;
        for (Map.Entry<String, Integer> e : QUIZ_SCORES.entrySet()) if (e.getValue() > bs) { bs = e.getValue(); best = e.getKey(); }
        for (ServerPlayer pl : near(sl)) Perks.say(pl, "§e❓ Quiz Night winner: §f" + best + " §7with " + bs + " point" + (bs == 1 ? "" : "s") + "!");
        d.news(day, best + " won Quiz Night at the library!");
        ServerPlayer champ = sl.getServer().getPlayerList().getPlayerByName(best);
        if (champ != null) {
            Perks.reward(champ, d, 25, "Quiz Night winner");
            Perks.unlock(champ, d, "quizwhiz");
            Applause.forAchievement(champ, "Quiz Night");
        }
    }

    /** Checks a chat message against the current quiz question; true if it was an answer attempt that should not go to residents. */
    public static boolean quizAnswer(ServerPlayer pl, String msg) {
        if (quizQ < 0 || quizAnswered || !pl.blockPosition().closerThan(LIBRARY, 48)) return false;
        String t = " " + msg.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9' ]", " ").replaceAll("\\s+", " ") + " ";
        for (String a : QUIZ[quizQ].a()) {
            if (!t.contains(" " + a + " ")) continue;
            quizAnswered = true;
            String pn = pl.getName().getString();
            QUIZ_SCORES.merge(pn, 1, Integer::sum);
            for (ServerPlayer o : near(pl.serverLevel())) Perks.say(o, "§a✔ " + pn + " got it! §f" + QUIZ[quizQ].a()[0]);
            pl.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.8f);
            return true;
        }
        return false;
    }

    /* ------------------------------------------------------------ Karaoke */

    static final String[] LYRICS = {
            "♪ Solaris nights, neon lights, dancing till the ferry flies ♪", "♪ Don't stop believin'... in the Sky Ferry schedule ♪",
            "♪ I will survive! As long as the diner's open ♪", "♪ We will, we will, BUILD YOU! ♪", "♪ Here comes the sun, doo-doo-doo-doo ♪",
            "♪ Sweet Caroline... BAH BAH BAAAH ♪", "♪ I'm on top of the world, 'ey! (Neon Heights, specifically) ♪"
    };

    static void karaokeTick(ServerLevel sl, CityData d, long tod) {
        List<Resident> crowd = at(sl, "beach_bar", 12);
        if (crowd.isEmpty()) return;
        long now = sl.getGameTime();
        if (now % 200 < 20) {
            Resident singer = crowd.get((int) ((now / 200) % crowd.size()));
            singer.gesture(Resident.G_SING, 190);
            singer.sayTo(Lines.pick(sl.getRandom(), LYRICS), 180);
            float[] tune = {0.8f, 1.0f, 1.2f, 1.35f, 1.5f};
            for (int i = 0; i < 4; i++) sl.playSound(null, singer.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.RECORDS, 0.6f, tune[sl.getRandom().nextInt(tune.length)]);
            for (Resident r : crowd) {
                if (r == singer) continue;
                r.getLookControl().setLookAt(singer, 30, 30);
                r.gesture(sl.getRandom().nextBoolean() ? Resident.G_CLAP : Resident.G_DANCE, 120);
            }
        }
        for (Resident r : crowd) if (sl.getRandom().nextFloat() < 0.1f) sl.sendParticles(ParticleTypes.NOTE, r.getX(), r.getY() + 2.2, r.getZ(), 1, 0.3, 0.1, 0.3, sl.getRandom().nextDouble());
    }

    /* ------------------------------------------------------------ Movie night */

    static void movieTick(ServerLevel sl) {
        for (Resident r : at(sl, "cinema", 20)) {
            if (sl.getRandom().nextFloat() > 0.04f || r.isSpeaking()) continue;
            if (sl.getRandom().nextBoolean()) r.showItem("minecraft:cookie", 120);
            int kind = sl.getRandom().nextInt(4);
            r.gesture(kind == 0 ? Resident.G_SURPRISED : kind == 1 ? Resident.G_LAUGH : kind == 2 ? Resident.G_SAD : Resident.G_EAT, 40);
            r.say(kind == 0 ? r.pick("*gasp*", "NO WAY!", "Behind you!!") : kind == 1 ? r.pick("*laughs*", "Hahaha!") : kind == 2 ? r.pick("*sniff*", "This part always gets me...") : r.pick("*munch munch*", "Pass the popcorn!"), 40);
        }
    }

    /* ------------------------------------------------------------ Fun run */

    static final Map<java.util.UUID, Boolean> RUNNERS = new LinkedHashMap<>();
    static final List<String> FINISHED = new ArrayList<>();
    static long runDay = -1;
    static int runState;

    static void runTick(ServerLevel sl, CityData d, long day, long tod) {
        Place start = Place.get("plaza"), finish = Place.get("pier");
        if (start == null || finish == null) return;
        if (runDay != day) {
            runDay = day;
            runState = 0;
            RUNNERS.clear();
            FINISHED.clear();
        }
        long now = sl.getGameTime();
        if (runState == 0 && tod >= 2000 && tod < 2400) {
            runState = 1;
            for (ServerPlayer pl : sl.players()) Perks.say(pl, "§a🏃 The Solaris Fun Run starts at 08:30 from the plaza! §7Be there to join - finish line at the old pier.");
        }
        if (runState == 1 && tod >= 2500) {
            runState = 2;
            for (Resident r : sl.getEntitiesOfClass(Resident.class, new AABB(start.pos).inflate(16, 6, 16), x -> x.profile() != null && x.isFree())) {
                if (RUNNERS.size() >= 10) break;
                RUNNERS.put(r.getUUID(), false);
                r.errand(finish.pos, 3000, 1.2 + sl.getRandom().nextDouble() * 0.35, "funrun");
                r.gesture(Resident.G_JOG, 60);
            }
            for (ServerPlayer pl : sl.players()) {
                if (!pl.blockPosition().closerThan(start.pos, 16)) continue;
                RUNNERS.put(pl.getUUID(), true);
                Calendar.banner(pl, "§a🏃 GO!", "§fRun to the old pier!");
            }
            if (RUNNERS.isEmpty()) {
                runState = 4;
                return;
            }
            Applause.confetti(sl, start.pos.above(2), 20);
            sl.playSound(null, start.pos, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.NEUTRAL, 1.5f, 1f);
        }
        if (runState != 2 || now % 10 != 0) return;
        for (Map.Entry<java.util.UUID, Boolean> e : RUNNERS.entrySet()) {
            net.minecraft.world.entity.Entity en = e.getValue() ? sl.getServer().getPlayerList().getPlayer(e.getKey()) : sl.getEntity(e.getKey());
            if (en == null) continue;
            String name = en instanceof Resident r ? r.profile().name : en.getName().getString();
            if (FINISHED.contains(name) || !en.blockPosition().closerThan(finish.pos, 5)) continue;
            FINISHED.add(name);
            int place = FINISHED.size();
            for (ServerPlayer pl : sl.players()) if (pl.blockPosition().closerThan(finish.pos, 60) || RUNNERS.containsKey(pl.getUUID())) Perks.say(pl, "§a🏁 " + name + " finishes " + ordinal(place) + "!");
            if (en instanceof Resident r) {
                r.endErrand();
                r.gesture(place == 1 ? Resident.G_VICTORY : Resident.G_WINDED, 60);
                r.say(place == 1 ? "FIRST PLACE! Woooo!" : r.pick("*pant* Made it!", "Never... again... *pant*", "Personal best!"), 60);
            } else if (en instanceof ServerPlayer pl) {
                int prize = place == 1 ? 20 : place == 2 ? 12 : place == 3 ? 8 : 4;
                Perks.reward(pl, d, prize, "Fun Run - " + ordinal(place) + " place");
                Calendar.banner(pl, "§a🏁 " + ordinal(place) + " place!", "§e+" + prize + " coins");
                Perks.unlock(pl, d, "runner");
            }
        }
        if (FINISHED.size() >= RUNNERS.size() || tod > 5500) {
            runState = 3;
            if (!FINISHED.isEmpty()) d.news(day, FINISHED.get(0) + " won the Solaris Fun Run!" + (FINISHED.size() > 1 ? " " + FINISHED.get(1) + " came second." : ""));
            for (Map.Entry<java.util.UUID, Boolean> e : RUNNERS.entrySet()) if (!e.getValue() && sl.getEntity(e.getKey()) instanceof Resident r) r.endErrand();
        }
    }

    static String ordinal(int n) {
        return n + (n % 100 >= 11 && n % 100 <= 13 ? "th" : switch (n % 10) { case 1 -> "st"; case 2 -> "nd"; case 3 -> "rd"; default -> "th"; });
    }

    /* ------------------------------------------------------------ Spooky Night */

    static void spookyTick(ServerLevel sl, CityData d, long tod) {
        if (tod < 12500 || tod > 18000) return;
        for (Resident r : Crowd.nearPlayers(sl, d)) {
            if (r.isSleeping() || !Hobbies.outside(r)) continue;
            if (sl.getRandom().nextFloat() < 0.2f) r.showItem("minecraft:jack_o_lantern", 120);
            if (sl.getRandom().nextFloat() < 0.15f) sl.sendParticles(ParticleTypes.SOUL, r.getX(), r.getY() + 0.5, r.getZ(), 2, 0.4, 0.3, 0.4, 0.01);
            if (r.isFree() && !r.isSpeaking() && sl.getRandom().nextFloat() < 0.02f) r.say(r.pick("Boo! ...Did I scare you?", "Happy Spooky Night!", "Did you hear that?!", "*spooky noises*", "Trick or treat!"), 50);
        }
        if (sl.getGameTime() % 60 == 15) for (ServerPlayer pl : sl.players()) {
            if (!pl.blockPosition().closerThan(Finale.ALTAR, 40) || !Perks.opt(pl, "sky")) continue;
            sl.sendParticles(pl, ParticleTypes.SOUL_FIRE_FLAME, true, Finale.ALTAR.getX() + 0.5, Finale.ALTAR.getY() + 1, Finale.ALTAR.getZ() + 0.5, 12, 4, 0.5, 4, 0.01);
        }
    }

    /* ------------------------------------------------------------ Starlight Festival */

    static final DustParticleOptions STARLIGHT = new DustParticleOptions(new Vector3f(1f, 0.95f, 0.6f), 2.2f);

    static void starlightTick(ServerLevel sl, CityData d, long day, long tod) {
        if (tod > 12500 && tod < 18000) {
            Vec3 c = Vec3.atCenterOf(Finale.ALTAR).add(0, 26, 0);
            for (ServerPlayer pl : sl.players()) {
                if (pl.distanceToSqr(c) > 140 * 140 || !Perks.opt(pl, "sky")) continue;
                for (int i = 0; i < 10; i++) {
                    double a1 = Math.PI / 2 + i * Math.PI / 5;
                    double rad = i % 2 == 0 ? 7 : 3;
                    double a2 = Math.PI / 2 + (i + 1) * Math.PI / 5, rad2 = (i + 1) % 2 == 0 ? 7 : 3;
                    for (int k = 0; k < 5; k++) {
                        double f = k / 5.0;
                        double x = Math.cos(a1) * rad * (1 - f) + Math.cos(a2) * rad2 * f, y = Math.sin(a1) * rad * (1 - f) + Math.sin(a2) * rad2 * f;
                        sl.sendParticles(pl, STARLIGHT, true, c.x + x, c.y + y, c.z, 1, 0.05, 0.05, 0.05, 0);
                    }
                }
            }
        }
        if (sl.getGameTime() % 40 == 15) Skies.kindnessTick(sl, d, day);
    }

    /* ------------------------------------------------------------ Blossom Day */

    static void blossomTick(ServerLevel sl, CityData d, long day) {
        for (Resident r : Crowd.nearPlayers(sl, d)) {
            if (r.isSleeping() || !Hobbies.outside(r) || sl.getRandom().nextFloat() > 0.1f) continue;
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, r.getX(), r.getY() + 2.5, r.getZ(), 4, 0.8, 0.3, 0.8, 0);
            if (sl.getRandom().nextFloat() < 0.3f) r.showItem(sl.getRandom().nextBoolean() ? "minecraft:poppy" : "minecraft:cornflower", 100);
        }
        if (sl.getGameTime() % 40 == 15) Skies.kindnessTick(sl, d, day);
    }

    /* ------------------------------------------------------------ First snow / first bloom */

    static void firsts(ServerLevel sl, CityData d, long day, long tod) {
        int dom = (int) Math.floorMod(day, 28L);
        boolean snow = dom == 21, bloom = dom == 0;
        if (!snow && !bloom || tod > 6000) return;
        if (!d.setting(Finale.CITY, "first", "").equals(String.valueOf(day))) {
            d.setSetting(Finale.CITY, "first", String.valueOf(day));
            d.news(day, snow ? "The first snow of winter fell over Solaris this morning!" : "The first blossoms of spring are out all over Solaris!");
            int n = 0;
            for (Resident r : Crowd.nearPlayers(sl, d)) {
                if (n++ >= 5 || !r.isFree() || !Hobbies.outside(r)) continue;
                r.gesture(Resident.G_LOOKUP, 60);
                r.say(snow ? r.pick("It's snowing! First snow!", "SNOW! Everyone, look!", "Winter's really here!") : r.pick("Look, the first blossoms!", "Spring is here! Smell that?", "Flowers everywhere!"), 70);
            }
        }
        for (ServerPlayer pl : sl.players()) {
            if (!Skies.outside(pl) || !Perks.opt(pl, "sky")) continue;
            sl.sendParticles(pl, snow ? ParticleTypes.SNOWFLAKE : ParticleTypes.CHERRY_LEAVES, false, pl.getX(), pl.getY() + 10, pl.getZ(), snow ? 40 : 12, 12, 3, 12, snow ? 0.02 : 0.0);
        }
    }

    /** A seasonal treat or present a resident gives a player on a festival day, or null. */
    public static String festivalGreeting(Resident r, CityData d, String pn, long day, RandomSource rnd, net.minecraft.world.entity.player.Player pl) {
        String key = spooky(day) ? "spooky" : starlight(day) ? "starlight" : blossom(day) ? "blossom" : null;
        if (key == null || d.setting(pn, key + ":" + r.profileId(), "").equals(String.valueOf(day))) return null;
        long tod = Math.floorMod(r.level().getDayTime(), 24000L);
        if (key.equals("spooky") && tod < 12500) return null;
        d.setSetting(pn, key + ":" + r.profileId(), String.valueOf(day));
        r.gesture(Resident.G_GIVE, 40);
        switch (key) {
            case "spooky" -> {
                Pastimes.giveItem(pl, "minecraft:cookie");
                return rnd.nextBoolean() ? "Trick or treat! ...I'll give YOU a treat. Here!" : "BOO! Haha, got you. Have a cookie.";
            }
            case "starlight" -> {
                net.minecraft.world.item.ItemStack gift = new net.minecraft.world.item.ItemStack(rnd.nextInt(4) == 0 ? net.minecraft.world.item.Items.EMERALD : net.minecraft.world.item.Items.CAKE);
                gift.setHoverName(net.minecraft.network.chat.Component.literal("§ePresent from " + r.profile().name));
                if (!pl.getInventory().add(gift)) pl.drop(gift, false);
                return "Happy Starlight Festival! I got you a little something.";
            }
            default -> {
                Pastimes.giveItem(pl, rnd.nextBoolean() ? "minecraft:poppy" : "minecraft:allium");
                return "Happy Blossom Day! A flower for you.";
            }
        }
    }
}
