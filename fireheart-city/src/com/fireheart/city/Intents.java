package com.fireheart.city;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/**
 * The broader vocabulary residents understand in person, over text and on calls: meeting plans ("meet me at the
 * plaza at 5pm"), questions about their life, invitations, feelings, jokes, advice, insults and much more.
 * Text arrives normalised: lower case, only letters/digits/apostrophes, single spaces, padded with a space each side.
 */
public final class Intents {
    private Intents() {}

    static final Map<String, String> PLACES = new LinkedHashMap<>();

    static {
        String[][] a = {
                {"plaza", "plaza", "town square", "square", "fireheart plaza", "solaris plaza", "city centre", "city center", "downtown"},
                {"park", "park", "the park"},
                {"clock", "clock tower", "clocktower", "big clock"},
                {"pier", "pier", "old pier", "jetty"},
                {"boardwalk", "boardwalk"},
                {"marina", "marina", "harbour", "harbor", "docks", "boats"},
                {"diner", "diner", "diesel diner", "restaurant", "burger place"},
                {"market", "market", "green leaf", "grocery", "groceries", "supermarket"},
                {"supply", "supply", "create supply", "hardware store"},
                {"bakery", "bakery", "auto bakery", "bread place"},
                {"garage", "garage", "mechanic", "workshop"},
                {"factory", "factory"},
                {"gravel", "aggregates", "gravel pit", "quarry"},
                {"port", "container port", "shipping port", "cargo port"},
                {"fuel", "fuel station", "gas station", "petrol station"},
                {"skyport", "skyport", "sky port", "airport", "skyliner"},
                {"ferry_city", "ferry", "sky ferry", "ferry pad"},
                {"isle_plaza", "neon heights", "sky island", "the island", "floating island", "neon plaza", "island"},
                {"noodle", "noodle", "noodles", "noodle bar", "neon noodle", "luna's", "lunas", "ramen"},
                {"arcade", "arcade", "neon arcade", "games place"},
                {"memorial", "memorial", "benson's memorial", "bensons memorial"},
                {"gardens", "gardens", "sky gardens", "garden"},
                {"observatory", "observatory", "telescope"},
                {"organ", "sky organ", "organ", "music stage", "music area", "music hall", "concert"},
                {"library", "library", "books"},
                {"bank", "bank", "the bank"},
                {"atm", "atm", "cash machine"},
                {"post", "post office", "postoffice", "mail office"},
                {"tech", "tech store", "firetech", "soltech", "phone shop", "electronics store"},
                {"statue", "statue", "founder's statue", "founders statue", "your statue", "the founder", "giant statue"},
                {"skylaunch", "sky launch", "skylaunch", "launch tower", "skydive tower", "skydiving tower", "skydiving"}
        };
        for (String[] row : a) for (int i = 1; i < row.length; i++) PLACES.put(row[i], row[0]);
    }

    static boolean any(String t, String... keys) {
        for (String k : keys) if (t.contains(k)) return true;
        return false;
    }

    static String pick(RandomSource r, String... o) {
        return Lines.pick(r, o);
    }

    /** The place key mentioned in the text (longest alias wins), or null. */
    static String place(String t, CityData.Profile p) {
        if (any(t, " your place ", " your house ", " your home ", " your apartment ", " your flat ", " yours ")) return p.home;
        if (any(t, " your work ", " your job ", " where you work ", " your shop ", " your store ")) return p.job.workKey;
        String best = null;
        int len = 0;
        for (Map.Entry<String, String> e : PLACES.entrySet()) {
            String k = e.getKey();
            if (k.length() > len && Pattern.compile("\\b" + Pattern.quote(k) + "\\b").matcher(t).find() && Place.get(e.getValue()) != null) {
                best = e.getValue();
                len = k.length();
            }
        }
        return best;
    }

    static final Pattern CLOCK = Pattern.compile("(?:\\bat |\\bby |\\baround |^| )(\\d{1,2})(?: (\\d{2}))? ?(am|pm|o'clock|oclock)?\\b");
    static final Pattern REL = Pattern.compile("\\bin (\\d+|a|an|one|two|three|four|five|ten|fifteen|twenty|thirty|half an|a couple of|a few) ?(minutes?|mins?|hours?|hrs?|h|m)\\b");

    static int num(String s) {
        return switch (s) {
            case "a", "an", "one" -> 1;
            case "two", "a couple of" -> 2;
            case "three", "a few" -> 3;
            case "four" -> 4;
            case "five" -> 5;
            case "ten" -> 10;
            case "fifteen" -> 15;
            case "twenty" -> 20;
            case "thirty" -> 30;
            default -> { try { yield Integer.parseInt(s); } catch (NumberFormatException e) { yield 1; } }
        };
    }

    static long todOf(int h, int m) {
        return Math.floorMod((h - 6) * 1000L + m * 1000L / 60, 24000L);
    }

    /** Absolute world day-time for the time mentioned in the text, or -1 if no time was mentioned. */
    static long time(String t, long now) {
        long tod = Math.floorMod(now, 24000L);
        boolean tomorrow = any(t, " tomorrow ", " tmrw ", " tmr ", " tomoz ");
        Matcher rm = REL.matcher(t);
        if (rm.find()) {
            int n = rm.group(1).equals("half an") ? 30 : num(rm.group(1));
            boolean hours = rm.group(2).startsWith("h") && !rm.group(1).equals("half an");
            return now + (hours ? n * 1000L : Math.max(1, n) * 1000L / 60);
        }
        if (any(t, " right now ", " now ", " asap ", " right away ", " straight away ", " immediately ")) return now + 150;
        long target = -1;
        if (any(t, " noon ", " midday ", " lunchtime ", " lunch time ")) target = 6000;
        else if (any(t, " midnight ")) target = 18000;
        else if (any(t, " sunset ", " dusk ")) target = 12000;
        else if (any(t, " sunrise ", " dawn ")) target = 23200;
        else if (any(t, " after work ", " after your shift ", " after your work ")) target = 10700;
        else {
            Matcher m = CLOCK.matcher(t);
            while (m.find()) {
                int h = Integer.parseInt(m.group(1));
                int mi = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
                String ap = m.group(3);
                String before = t.substring(0, m.start(1));
                if (ap == null && !before.endsWith("at ") && !before.endsWith("by ") && !before.endsWith("around ")) continue;
                if (h > 23 || mi > 59) continue;
                if ("pm".equals(ap) && h < 12) h += 12;
                else if ("am".equals(ap) && h == 12) h = 0;
                else if (ap == null || ap.startsWith("o")) {
                    if (h >= 1 && h <= 6) h += any(t, " morning ", " am ") ? 0 : 12;
                    else if (h >= 7 && h <= 11 && !any(t, " morning ")) {
                        long am = todOf(h, mi), pm = todOf(h + 12, mi);
                        boolean amPassed = Math.floorMod(am - tod, 24000L) > 20000;
                        if (amPassed && !tomorrow || any(t, " tonight ", " evening ")) h += 12;
                        else if (Math.floorMod(am - tod, 24000L) > Math.floorMod(pm - tod, 24000L) && !tomorrow) h += 12;
                    }
                }
                target = todOf(h, mi);
                break;
            }
            if (target < 0) {
                if (any(t, " tonight ", " this evening ", " in the evening ")) target = 13000;
                else if (any(t, " this afternoon ", " in the afternoon ")) target = 8000;
                else if (any(t, " this morning ", " in the morning ")) target = 2000;
                else if (tomorrow) target = 3000;
            }
        }
        if (target < 0) return -1;
        long delta = Math.floorMod(target - tod, 24000L);
        if (delta > 23400) delta = 150;
        long at = now + delta;
        if (tomorrow && delta < 24000 && Calendar.dayOf(at) == Calendar.dayOf(now)) at += 24000;
        return at;
    }

    static boolean meetAsk(String t) {
        return any(t, " meet me ", " meet up ", " meetup ", " meet you ", " lets meet ", " let's meet ", " wanna meet ", " want to meet ", " can we meet ", " could we meet ",
                " see you at ", " see u at ", " hang out at ", " hangout at ", " hang at ", " wanna go to ", " want to go to ", " join me at ", " join me for ", " come to the ", " come to ",
                " be at the ", " meet at ", " go to the ", " go out ", " grab food ", " grab lunch ", " grab dinner ", " get lunch ", " get dinner ", " get food ", " date at ", " on a date ", " wanna hang ", " want to hang ", " hang out ");
    }

    static boolean yes(String t) {
        return any(t, " yes ", " yeah ", " yep ", " yup ", " sure ", " ok ", " okay ", " k ", " fine ", " deal ", " sounds good ", " works ", " perfect ", " alright ", " ya ", " yea ", " of course ", " definitely ");
    }

    static boolean no(String t) {
        return any(t, " no ", " nah ", " nope ", " can't ", " cant ", " never mind ", " nevermind ", " forget it ");
    }

    /** Meeting plans; returns a reply or null if the text isn't about meeting. */
    static String meet(ServerPlayer pl, Resident r, String t, Chat.Ctx c, long now) {
        ServerLevel sl = pl.serverLevel();
        CityData d = r.data();
        CityData.Profile p = r.profile();
        String pn = pl.getName().getString();
        RandomSource rnd = r.getRandom();
        long dt = sl.getDayTime();
        Meets.nowDT = dt;
        Meets.Meet cur = Meets.with(p.id, pn);
        if (cur != null) {
            Place cp = Place.get(cur.place);
            String cl = cp == null ? "the spot" : cp.label;
            if (any(t, " cancel ", " can't make it ", " cant make it ", " can't come ", " cant come ", " not coming ", " call it off ", " rain check ", " another time ", " something came up ")) {
                Meets.cancel(sl, d, cur);
                c.topic = "";
                p.mind.trust.put(pn, Math.max(-100, p.mind.trustIn(pn) - 1));
                r.gesture(Resident.G_SAD, 40);
                return pick(rnd, "Aww, okay. Another time then!", "Oh... no worries. Let me know when you're free!", "That's okay! Rain check ☺");
            }
            if (any(t, " running late ", " be late ", " i'm late ", " im late ", " on my way ", " omw ", " coming ", " be there soon ", " almost there ", " nearly there ", " 5 min ", " five min ", " give me a minute ", " few minutes ")) {
                if (any(t, " late ")) Meets.delay(d, cur, 600);
                return pick(rnd, "No rush! I'll wait at " + cl + " ☺", "Okay! See you soon.", "Take your time, I'm not going anywhere.");
            }
            if (any(t, " i'm here ", " im here ", " i'm at ", " im at ", " i'm outside ", " im outside ", " arrived ", " i made it ", " where are you ", " where r u ", " wya ")) {
                boolean there = r.blockPosition().closerThan(cp == null ? r.blockPosition() : cp.pos, 9);
                if (there && pl.distanceTo(r) < 40) {
                    r.comeTo(pl, 600);
                    return pick(rnd, "I see you! Coming over!", "Oh there you are! One sec.", "Look to your left! ☺");
                }
                if (there) return "I'm at " + cl + " already! Where are you?";
                return "On my way to " + cl + " now!";
            }
            if (any(t, " still on ", " still meeting ", " are we still ", " don't forget ", " dont forget ", " remember our ", " what time are we ", " what time is our ", " when are we ", " where are we meeting ")) {
                return "Yep! " + cl.substring(0, 1).toUpperCase(Locale.ROOT) + cl.substring(1) + ", " + Meets.relative(cur.at) + ". Wouldn't miss it!";
            }
        }
        if (c.topic.equals("meet_offer") && !c.meetPlace.isEmpty()) {
            if (yes(t) && !no(t)) {
                Place pl2 = Place.get(c.meetPlace);
                c.topic = "";
                if (pl2 == null) return null;
                Meets.book(sl, d, p, pn, pl2, c.meetAt);
                r.gesture(Resident.G_THUMBS, 40);
                return pick(rnd, "Great! See you at " + pl2.label + " " + Meets.relative(c.meetAt) + " ☺", "It's a plan! " + cap(pl2.label) + ", " + Meets.relative(c.meetAt) + ".", "Perfect, see you then!");
            }
            if (no(t)) {
                c.topic = "";
                return pick(rnd, "Okay, no worries! Just let me know when works.", "Alright, another time then.");
            }
        }
        String key = place(t, p);
        long at = time(t, dt);
        boolean asked = meetAsk(t);
        if (c.topic.equals("meet_where") && key != null) {
            asked = true;
            if (at < 0) at = c.meetAt;
        }
        if (c.topic.equals("meet_when") && at >= 0 && !c.meetPlace.isEmpty()) {
            asked = true;
            if (key == null) key = c.meetPlace;
        }
        if (!asked) return null;
        if (key == null) {
            if (at < 0 && !any(t, " meet ")) return null;
            c.topic = "meet_where";
            c.meetAt = at < 0 ? dt + 300 : at;
            return pick(rnd, "Sure! Where do you want to meet?", "I'd love to! Where?", "Okay! Where should we meet" + (at < 0 ? "" : " " + Meets.relative(at)) + "?");
        }
        Place place = Place.get(key);
        if (place == null) return null;
        if (at < 0) {
            if (any(t, " later ", " sometime ", " some time ", " soon ")) {
                c.topic = "meet_when";
                c.meetPlace = key;
                return pick(rnd, cap(place.label) + "? Sounds fun! What time?", "Ooh yes! What time works for you?");
            }
            at = dt + 300;
        }
        int trust = p.mind.trustIn(pn);
        if (trust < -10) {
            c.topic = "";
            return pick(rnd, "Hmm... I don't think so, " + pn + ".", "I'd rather not, sorry.", "After everything? No thanks.");
        }
        long tod = Math.floorMod(at, 24000L);
        long rday = Calendar.dayOf(at + 1500);
        if (r.workingAt(tod, rday) && !key.equals(p.job.workKey)) {
            long off = at;
            for (int i = 0; i < 48 && r.workingAt(Math.floorMod(off, 24000L), Calendar.dayOf(off + 1500)); i++) off += 500;
            c.topic = "meet_offer";
            c.meetPlace = key;
            c.meetAt = off;
            return pick(rnd, "I'll be at work then! How about " + Meets.when(off) + ", once my shift's done?", "Ah, I'm working " + Meets.relative(at) + ". Could we do " + Meets.when(off) + " instead?");
        }
        if (Resident.asleepAt(tod) && trust < 30) {
            long morn = at;
            while (Resident.asleepAt(Math.floorMod(morn, 24000L))) morn += 500;
            morn += 2500;
            c.topic = "meet_offer";
            c.meetPlace = key;
            c.meetAt = morn;
            return pick(rnd, "That's way past my bedtime! How about " + Meets.relative(morn) + "?", "I'll be asleep by then... " + Meets.relative(morn) + " instead?");
        }
        for (CityData.Plan pl2 : d.plansFor(p.id, Calendar.dayOf(at))) {
            if ((pl2.what.equals("party") || pl2.what.equals("speech") || pl2.what.equals("dance")) && tod >= 10000 && tod <= 14000) {
                c.topic = "";
                return pick(rnd, "Oh, I've got plans then already - " + pl2.what + " at " + (Place.get(pl2.place) == null ? "town" : Place.label(pl2.place)) + ". Come with me instead?", "I'm busy then, sorry! Another time?");
            }
        }
        c.topic = "";
        Meets.book(sl, d, p, pn, place, at);
        r.gesture(at - dt < 400 ? Resident.G_THUMBS : Resident.G_NOD, 40);
        Mind.playerEvent(d, p, pn, r.day(), "{P} asked me to meet at " + place.label, 1, 3);
        boolean now2 = at - dt < 400;
        if (now2) return pick(rnd, "On my way to " + place.label + "!", "Sure, heading to " + place.label + " now!", "Okay! See you at " + place.label + " in a bit ☺");
        return pick(rnd, "Sure! See you at " + place.label + " " + Meets.relative(at) + " ☺", "Deal! " + cap(place.label) + ", " + Meets.relative(at) + ". I'll be there!", "Ooh, I'd love that. " + cap(Meets.relative(at)) + " at " + place.label + " it is!", "It's a date! Well, not a DATE date. Unless... anyway, " + Meets.relative(at) + "!");
    }

    static String cap(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    static final String[] JOKES = {
            "Why did the creeper cross the road? To get to the other ssssside!",
            "What do you call a sleeping bull? A bulldozer.",
            "I told the bank teller a joke about interest. It didn't earn much.",
            "Why don't skeletons fight each other? They don't have the guts.",
            "What's a ferry pilot's favourite snack? Sky-scrapers. ...Okay, that one was bad.",
            "I tried to catch fog yesterday. Mist.",
            "Why did the noodle go to therapy? It felt a bit strained.",
            "What do you call a pig that does karate? A pork chop!",
            "I'm reading a book about anti-gravity. Impossible to put down.",
            "Why was the diesel engine so calm? It had good horse-power management."
    };

    /** Everything else residents understand; returns null when nothing matches. */
    static String more(ServerPlayer pl, Resident r, String t, Chat.Ctx c) {
        CityData d = r.data();
        CityData.Profile p = r.profile();
        String pn = pl.getName().getString();
        RandomSource rnd = r.getRandom();
        Mind m = p.mind;
        int trust = m.trustIn(pn);
        long day = r.day();
        String pet = Pets.playerPetLine(pl, r, t);
        if (pet != null) return pet;
        if (any(t, " where are you ", " where r u ", " where you at ", " wya ", " where are u ")) {
            String here = Dialogue.here(r);
            return "I'm " + (here.equals("around town") ? "out and about" : "at " + here) + (r.onIsland() ? ", up on Neon Heights!" : "!") + " " + pick(rnd, "Come say hi!", "Why, want to meet up?", "");
        }
        if (any(t, " what's your name ", " whats your name ", " who are you ", " your name ", " what are you called ")) return pick(rnd, "I'm " + p.name + "! " + cap(p.job.title) + " at " + p.job.work().label + ".", p.name + ". Nice to properly meet you, " + pn + "!");
        if (any(t, " what do you do ", " what's your job ", " whats your job ", " where do you work ", " your job ", " do you work ", " for a living ", " what is your job ")) return "I'm the " + p.job.title.toLowerCase(Locale.ROOT) + " at " + p.job.work().label + ". " + pick(rnd, "Keeps me busy!", "I actually love it.", "It pays the bills!", "Some days are harder than others, but it's good.");
        if (any(t, " how was work ", " how's work ", " hows work ", " work today ", " busy day ")) {
            DayLog lg = p.log(r.routineDay());
            return lg.notes.isEmpty() ? pick(rnd, "Pretty normal! Nothing exciting.", "Long, but fine.") : "Not bad! " + Events.sentence(DayLog.lower(lg.notes.get(lg.notes.size() - 1))) + ".";
        }
        if (any(t, " where do you live ", " where's your house ", " wheres your house ", " your home ", " where do you stay ")) {
            Place h = p.homePlace();
            return h == null ? "I'm between homes right now, honestly." : "I live in " + h.label + ". " + pick(rnd, "It's cosy!", "Come visit sometime!", "Great view.");
        }
        if (any(t, " how old ", " your age ", " your birthday ")) return pick(rnd, "Old enough to know better, young enough to do it anyway!", "A lady never tells. Or a gentleman. Or me.", "Ha! Old enough to remember when this city was just a few houses.");
        if (any(t, " are you single ", " boyfriend ", " girlfriend ", " married ", " dating ", " your partner ", " in a relationship ", " crush ")) {
            if (!p.partner.isEmpty() && d.profiles.get(p.partner) != null) return "I'm with " + d.profiles.get(p.partner).name + "! " + pick(rnd, "Very happy, too.", "Best thing that ever happened to me.");
            return pick(rnd, "Single! Not that I'm looking. ...Much.", "Nope, flying solo for now.", "Why, are you asking for a friend?");
        }
        if (any(t, " what time is it ", " what's the time ", " whats the time ", " time is it ")) return "It's " + Meets.when(pl.serverLevel().getDayTime()) + ". " + pick(rnd, "Time flies!", "Already?", "");
        if (any(t, " weather ", " raining ", " is it sunny ", " rain ", " storm ", " cold out ", " hot out ")) {
            ServerLevel sl = pl.serverLevel();
            return sl.isThundering() ? "It's storming! Stay inside!" : sl.isRaining() ? pick(rnd, "Raining again. Perfect noodle weather.", "Wet! Bring an umbrella.") : pick(rnd, "Lovely out! Perfect day for the pier.", "Sunny and nice. Can't complain!");
        }
        if (any(t, " how much money ", " are you rich ", " how many coins ", " your savings ", " are you broke ", " you poor ")) {
            int total = p.coins + Bank.savings(d, p.id);
            return total > 200 ? "I'm doing alright! Saving up, you know." : total > 50 ? "Getting by. Could always use more!" : "Honestly? Pretty broke right now.";
        }
        if (any(t, " hobby ", " hobbies ", " for fun ", " free time ", " like doing ", " like to do ", " enjoy doing ", " spare time ")) return "I love " + Memory.hobby(p) + ". " + pick(rnd, "Want to join sometime?", "It clears my head.", "Don't judge me!");
        if (any(t, " are you hungry ", " you hungry ", " want food ", " want some food ", " have you eaten ", " did you eat ", " what did you eat ", " had lunch ", " had dinner ")) {
            return p.hunger < 30 ? pick(rnd, "STARVING. Want to grab food? The Diesel Diner's close.", "So hungry. Noodles at Luna's later?") : pick(rnd, "Nope, I'm full! Had " + Economy.label(Memory.favourite(p)) + " earlier.", "I'm good, thanks!");
        }
        if (any(t, " favourite colour ", " favorite color ", " favourite color ", " favorite colour ")) return pick(rnd, "Orange! Like a sunset.", "Blue. Sky blue, specifically.", "Purple - like the neon on the island.", "Green, obviously.");
        if (any(t, " favourite music ", " favorite music ", " favourite song ", " favorite song ", " what music ", " like music ")) return pick(rnd, "Anything Remy plays on the street, honestly.", "I love the Sky Organ concerts.", "Upbeat stuff! Makes work go faster.");
        if (any(t, " favourite game ", " favorite game ", " play games ", " video games ", " gaming ")) return pick(rnd, "I'm a menace at the arcade, just so you know.", "Have you played the games at the Neon Arcade? I'm on the leaderboard!", "I'm more of a board game person.");
        if (any(t, " favourite place ", " favorite place ", " best place ", " where should i go ", " what should i do ", " recommend ", " any ideas ", " i'm bored ", " im bored ", " so bored ")) {
            String[] ideas = {"Try the Sky Launch - you'll scream, but in a good way.", "The Neon Arcade on the island. Beat Rex's score!", "Grab noodles at Luna's. Trust me.", "Watch the sunset from the old pier.", "The observatory at night is magical.", "Walk the Sky Gardens. So peaceful.", "Go see your statue! It's enormous."};
            return pick(rnd, ideas) + " " + pick(rnd, "Want me to come?", "", "You'll love it.");
        }
        if (any(t, " tell me a joke ", " joke ", " make me laugh ", " say something funny ")) {
            r.gesture(Resident.G_LAUGH, 40);
            return pick(rnd, JOKES);
        }
        if (any(t, " advice ", " what should i ", " help me decide ", " should i ")) return pick(rnd, "Honestly? Go with your gut. It's usually right.", "Sleep on it. Everything looks clearer in the morning.", "Do the thing you'll regret NOT doing.", "Ask Nell at the library - she's wiser than me!");
        if (any(t, " are you happy ", " are you sad ", " how do you feel ", " how are you feeling ", " you feeling ", " are you okay ", " are you alright ")) {
            int mood = p.mood();
            return mood > 70 ? pick(rnd, "Happy! Genuinely.", "Great, actually!") : mood > 40 ? "I'm alright. Could be better, could be worse." : pick(rnd, "Not great, to be honest...", "Bit down today. Thanks for asking though.");
        }
        if (any(t, " i'm sad ", " im sad ", " i feel sad ", " i'm upset ", " im upset ", " i'm lonely ", " im lonely ", " bad day ", " i'm stressed ", " im stressed ", " i'm tired ", " im tired ", " feel bad ")) {
            r.gesture(Resident.G_HUGSELF, 40);
            m.trust.put(pn, Math.min(100, trust + 1));
            return pick(rnd, "Aw, " + pn + "... want to talk about it? Or I could just keep you company.", "Sending a big hug your way. It'll get better, I promise.", "Want to grab something to eat? Always helps me.", "Hey, you've got a whole city that loves you. Don't forget that.");
        }
        if (any(t, " i'm happy ", " im happy ", " great day ", " good day ", " i'm excited ", " im excited ", " guess what ", " good news ")) {
            r.gesture(Resident.G_CHEER, 40);
            return pick(rnd, "Yay! Tell me everything!", "Love that for you!", "Ooh, what happened?", "That makes me happy too!");
        }
        if (any(t, " i miss you ", " miss you ", " missed you ")) {
            r.particles(net.minecraft.core.particles.ParticleTypes.HEART, 2);
            return trust > 20 ? pick(rnd, "Aww, I miss you too! Come visit!", "Missed you more ☺") : "Aw, that's sweet!";
        }
        if (any(t, " happy birthday ", " congrats ", " congratulations ", " well done ", " good job ", " proud of you ", " nice work ")) {
            r.gesture(Resident.G_CHEER, 40);
            return pick(rnd, "Aww, thank you!", "Thanks, " + pn + "! That means a lot.", "Hehe, thank you!");
        }
        if (any(t, " do you like me ", " are we friends ", " am i your friend ", " what do you think of me ", " do you trust me ")) {
            return trust > 40 ? pick(rnd, "Are you kidding? You're one of my favourite people!", "Of course we're friends, silly.") : trust > 5 ? pick(rnd, "Yeah! I like you, " + pn + ".", "Sure! We're getting there.") : trust > -10 ? "I don't really know you that well yet." : "Honestly? You haven't been very nice to me.";
        }
        if (any(t, " best friend ", " who do you like ", " closest friend ")) {
            CityData.Profile bf = null;
            int best = -1;
            for (CityData.Profile q : d.profiles.values()) {
                if (q == p) continue;
                CityData.Rel rel = d.peekRel(p.id, q.id);
                if (rel != null && rel.aff > best) { best = rel.aff; bf = q; }
            }
            return bf == null ? "Hmm, I'm still getting to know everyone!" : bf.name + ", probably! Don't tell the others.";
        }
        if (any(t, " who do you hate ", " enemy ", " rival ", " who annoys you ", " don't like anyone ")) {
            for (CityData.Profile q : d.profiles.values()) {
                CityData.Rel rel = d.peekRel(p.id, q.id);
                if (q != p && rel != null && rel.rival) return "Ugh. " + q.name + ". Next question.";
            }
            return "I get along with everyone! Mostly.";
        }
        if (any(t, " founder ", " the statue ", " your god ", " worship ")) return pick(rnd, "The Founder built all of this! We owe everything to them.", "Have you seen the statue? 96 blocks tall. Incredible.", "Wait... YOU'RE the Founder. Why are you asking me?!");
        if (any(t, " festival ")) return Festival.running() ? "The festival's on right now! Get to the statue!" : pick(rnd, "The Festival of the Founder is the best day of the year. Cake, fireworks, gifts!", "I can't wait for the next festival!");
        if (any(t, " city ", " solaris ", " this town ", " like it here ", " living here ")) return pick(rnd, "I love Solaris. Wouldn't live anywhere else.", "Best city in the world. Biased? Maybe.", "It's home. The sky island still blows my mind.");
        if (any(t, " skydive ", " sky launch ", " skydiving ")) return pick(rnd, "The Sky Launch? I did it once and screamed the whole way down.", "Skydiving is terrifying. 10 out of 10, would do again.");
        if (any(t, " are you busy ", " can you talk ", " you free ", " are you free ", " got a minute ")) return r.workBusy() ? "A little - I'm working. But for you, I've got a minute." : pick(rnd, "I'm free! What's up?", "Nope, not busy. What's going on?");
        if (any(t, " are you awake ", " you up ", " u up ")) return r.activityName().equals("sleep") ? "...I am now. What's up? *yawns*" : "Wide awake! What's up?";
        if (any(t, " call me ", " give me a call ", " ring me ")) return pick(rnd, "Sure, I'll call you later!", "Okay! Expect a call ☺");
        if (any(t, " what's up ", " whats up ", " wassup ", " wazzup ", " sup ", " wsg ", " wyd ")) return "Not much! I'm " + r.status() + ". You?";
        if (any(t, " good morning ", " gm ", " morning ")) return pick(rnd, "Good morning, " + pn + "! ☀", "Morning! Sleep well?");
        if (any(t, " good night ", " goodnight ", " gn ", " nighty night ")) return pick(rnd, "Goodnight, " + pn + "! Sleep well.", "Night night! ☾");
        if (any(t, " i'm home ", " im home ", " got home ")) return pick(rnd, "Welcome home! Get some rest.", "Yay, safe and sound!");
        if (any(t, " you're dumb ", " youre dumb ", " you're stupid ", " youre stupid ", " you're ugly ", " youre ugly ", " shut up ", " i hate you ", " you suck ", " idiot ", " loser ", " annoying ")) {
            m.trust.put(pn, Math.max(-100, trust - 6));
            Mind.playerEvent(d, p, pn, day, "{P} was rude to me", -3, 5);
            r.gesture(sl(r).random.nextBoolean() ? Resident.G_ANGRY : Resident.G_SAD, 50);
            return pick(rnd, "Wow. Okay. That was mean.", "...Rude.", "Why would you say that?", "I didn't deserve that, " + pn + ".");
        }
        if (any(t, " can you help ", " help me ", " i need help ", " need a favour ", " need a favor ")) return pick(rnd, "Of course! What do you need?", "Sure, what's up?", "For you? Anything. Within reason.");
        if (any(t, " i love ", " i like ") && !any(t, " you ", " u ")) return pick(rnd, "Ooh, me too!", "Nice! Good taste.", "Really? Tell me more!");
        if (any(t, " where is ", " where's the ", " wheres the ", " how do i get to ", " directions ")) {
            String k = place(t, p);
            Place pl2 = k == null ? null : Place.get(k);
            if (pl2 != null) {
                int dx = pl2.pos.getX() - pl.getBlockX(), dz = pl2.pos.getZ() - pl.getBlockZ();
                String dir = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? "east" : "west") : (dz > 0 ? "south" : "north");
                return cap(pl2.label) + " is about " + (int) Math.sqrt(dx * dx + dz * dz) + " blocks " + dir + " of you" + (pl2.island ? ", up on Neon Heights - take the Sky Ferry" : "") + ".";
            }
        }
        return null;
    }

    private static ServerLevel sl(Resident r) {
        return (ServerLevel) r.level();
    }
}
