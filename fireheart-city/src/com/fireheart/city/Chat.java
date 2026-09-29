package com.fireheart.city;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraftforge.event.ServerChatEvent;

/** Lets players talk to residents by typing in chat ("hi Mia", "Leo, how are you?", "what are you doing?"). */
public final class Chat {
    private Chat() {}

    private static final Map<UUID, Long> COOLDOWN = new HashMap<>();
    private static final Map<UUID, Ctx> CTX = new HashMap<>();
    private static final Map<String, Long> STORY_TOLD = new HashMap<>();
    static final long FOCUS = 800, REPEAT = 1200;

    public static final class Ctx {
        UUID res;
        String topic = "";
        String lastQ = "";
        long at;
        long qAt;
        String meetPlace = "";
        long meetAt;
        String why = "", lastTopic = "", explain = "";
    }

    static Ctx focus(ServerPlayer pl, long now) {
        Ctx c = CTX.get(pl.getUUID());
        return c != null && now - c.at < FOCUS ? c : null;
    }

    public static void reset() {
        CTX.clear();
        STORY_TOLD.clear();
        COOLDOWN.clear();
    }

    public static void onChat(ServerChatEvent e) {
        ServerPlayer pl = e.getPlayer();
        String msg = e.getRawText();
        if (pl == null || msg == null || msg.startsWith("/")) return;
        pl.getServer().execute(() -> {
            try {
                handle(pl, msg);
            } catch (Throwable t) {
                FireheartCity.LOG.error("Resident chat reply failed", t);
            }
        });
    }

    static Resident pickListener(ServerPlayer pl, String lower) {
        ServerLevel sl = pl.serverLevel();
        List<Resident> near = sl.getEntitiesOfClass(Resident.class, pl.getBoundingBox().inflate(14, 6, 14), r -> r.profile() != null);
        Resident named = null;
        for (Resident r : near) {
            String n = r.profile().name.toLowerCase(Locale.ROOT);
            if (java.util.regex.Pattern.compile("\\b" + n + "\\b").matcher(lower).find()) {
                if (named == null || r.distanceToSqr(pl) < named.distanceToSqr(pl)) named = r;
            }
        }
        if (named != null) return named;
        Ctx c = focus(pl, sl.getGameTime());
        if (c != null) for (Resident r : near) if (r.getUUID().equals(c.res) && r.distanceToSqr(pl) < 16 * 16) return r;
        Resident best = null;
        double bd = 7 * 7;
        for (Resident r : near) {
            double d = r.distanceToSqr(pl);
            if (d < bd && r.hasLineOfSight(pl)) { bd = d; best = r; }
        }
        return best;
    }

    private static boolean any(String t, String... keys) {
        for (String k : keys) if (t.contains(k)) return true;
        return false;
    }

    static void handle(ServerPlayer pl, String raw) {
        String t = " " + raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9' ]", " ").replaceAll("\\s+", " ") + " ";
        Resident r = pickListener(pl, t);
        if (r == null) return;
        boolean named = java.util.regex.Pattern.compile("\\b" + r.profile().name.toLowerCase(Locale.ROOT) + "\\b").matcher(t).find();
        long now = pl.serverLevel().getGameTime();
        Long last = COOLDOWN.get(r.getUUID());
        if (last != null && now - last < 30) return;
        COOLDOWN.put(r.getUUID(), now);
        if (r.isSleeping() || r.activityName().equals("sleep") && !r.isFree()) {
            r.sayTo("Zzz... mmh? ...Zzz.", 50);
            return;
        }
        Ctx c = focus(pl, now);
        if (c != null && !r.getUUID().equals(c.res)) c = null;
        boolean fresh = c == null;
        if (c == null) {
            c = new Ctx();
            c.res = r.getUUID();
        }
        String reply = reply(pl, r, t, named, c, now);
        if (reply == null || reply.isEmpty()) {
            if (!fresh) c.at = now;
            return;
        }
        if (c.topic.equals("bye")) CTX.remove(pl.getUUID());
        else {
            c.at = now;
            CTX.put(pl.getUUID(), c);
        }
        r.getLookControl().setLookAt(pl, 30, 30);
        if (r.convo != null) r.leaveConversation("Oh, one sec - " + pl.getName().getString() + " is talking to me.");
        Resident.addressed(r.level(), pl.getName().getString());
        if (useAi(t, c) && Groq.available()) {
            String pn = pl.getName().getString();
            String convoKey = pn + "|" + r.profileId();
            String sys = Groq.persona(r, pn, reply);
            String said = raw.trim();
            r.sayTo("...", 120);
            final String fallback = reply;
            java.util.UUID rid = r.getUUID();
            Groq.ask(convoKey, sys, pn + ": " + said).thenAccept(ai -> pl.getServer().execute(() -> {
                if (!(pl.serverLevel().getEntity(rid) instanceof Resident rr) || !rr.isAlive()) return;
                String out = ai != null ? ai : fallback;
                rr.getLookControl().setLookAt(pl, 30, 30);
                rr.sayTo(out, Math.min(220, 60 + out.length() * 2));
                if (ai == null) Groq.remember(convoKey, "assistant", fallback);
            }));
            return;
        }
        r.sayTo(reply, Math.min(200, 60 + reply.length() * 2));
    }

    static boolean useAi(String t, Ctx c) {
        if (c.topic.startsWith("meet") || c.topic.equals("follow") || c.topic.equals("rps") || Pastimes.noAi(t)) return false;
        return !any(t, " follow ", " come with ", " come here ", " walk with me ", " lets go ", " let's go ", " stop following ", " wait here ", " stay here ", " go home ",
                " meet ", " check in ", " checkin ", " check me in ", " checking in ", " room ", " key ", " sorry ", " apologi", " forgive ", " my bad ");
    }

    static String qKey(String t) {
        if (any(t, " what are you doing ", " whatcha doing ", " what're you doing ", " where are you going ", " where you going ", " what you up to ", " up to ")) return "doing";
        if (any(t, " how are you ", " how's it going ", " hows it going ", " how are things ", " you ok ", " you okay ", " how was your day ", " how's your day ", " hows your day ")) return "how";
        if (any(t, " remember ", " memory ", " recall ")) return "remember";
        if (any(t, " dream", " sleep well ", " slept ")) return "dream";
        if (any(t, " plan ", " plans ", " tomorrow ", " doing today ", " goal ", " saving for ")) return "plan";
        if (any(t, " favourite ", " favorite ", " like to eat ", " love to eat ")) return "fav";
        if (any(t, " news ", " gossip ", " what's new ", " whats new ", " anything new ", " heard ")) return "news";
        return "";
    }

    static String reply(ServerPlayer pl, Resident r, String t, boolean named) {
        long now = pl.serverLevel().getGameTime();
        Ctx c = CTX.computeIfAbsent(UUID.nameUUIDFromBytes(("phone|" + pl.getUUID() + "|" + r.getUUID()).getBytes()), k -> new Ctx());
        c.res = r.getUUID();
        String out = reply(pl, r, t, named, c, now);
        c.at = now;
        return out;
    }

    static String reply(ServerPlayer pl, Resident r, String t, boolean named, Ctx c, long now) {
        String out = reply0(pl, r, t, named, c, now);
        String q = qKey(t);
        if (out != null && !q.isEmpty()) {
            c.lastQ = q;
            c.qAt = now;
        }
        return out;
    }

    static String reply0(ServerPlayer pl, Resident r, String t, boolean named, Ctx c, long now) {
        CityData d = r.data();
        CityData.Profile p = r.profile();
        String pn = pl.getName().getString();
        CityData.Rel pr = d.playerRel(p.id, pn);
        Mind m = p.mind;
        long day = r.day();
        RandomSource rnd = r.getRandom();
        int trust = m.trustIn(pn);
        boolean cold = trust <= -25;
        boolean focused = !c.topic.isEmpty() || c.at > 0 && now - c.at < FOCUS;
        if (!named && !focused && !any(t, " hi ", " hello ", " hey ", " how are you ", " what are you doing ", " remember ", " sorry ", " thank", " bye ", " news ", " plan", " favourite ", " favorite ", " dream", " you ", " follow ", " come with ", " come here ", " meet ", " hang out ", " joke ", " what time ")) return null;
        if (!pr.met) {
            pr.met = true;
            pr.fam = 10;
            d.news(day, p.name + " met " + pn + ".");
            d.setDirty();
            String pm = Reception.persona("meet", r, pn, rnd);
            return pm != null ? pm : "Oh! Hi there - I'm " + p.name + ", the " + p.job.title.toLowerCase(Locale.ROOT) + " at " + p.job.work().label + ". You must be " + pn + "!";
        }
        pr.fam = Math.min(100, pr.fam + 1);
        if (!cold) {
            String rom = Romance.chat(pl, r, t, rnd);
            if (rom != null) return rom;
        }
        String fun = Pastimes.chat(pl, r, t, c);
        if (fun != null) return fun;
        d.setDirty();
        if (any(t, " sorry ", " apologi", " my bad ", " forgive ")) {
            if (trust < 0) {
                m.trust.put(pn, Math.min(10, trust + 12));
                Mind.playerEvent(d, p, pn, day, "{P} apologised to me", 2, 5);
                r.gesture(Resident.G_THINK, 40);
                return pick(rnd, "...Alright. Apology accepted, " + pn + ". Let's start over.", "Thank you for saying that. I'll let it go.", "Hmph. Fine. But I'm keeping an eye on you!");
            }
            return pick(rnd, "Sorry? For what? You've been lovely!", "No need to apologise, silly.");
        }
        if (!cold || c.topic.startsWith("meet")) {
            String mt = Intents.meet(pl, r, t, c, now);
            if (mt != null) return mt;
        }
        if (!cold && any(t, " follow me ", " come with me ", " come here ", " follow ", " walk with me ", " lets go ", " let's go ")) {
            c.topic = "follow";
            if (r.following(pl)) return pick(rnd, "I'm right behind you!", "Coming, coming!", "Still with you, " + pn + ".");
            if (r.distanceTo(pl) > 16) return pick(rnd, "Follow you? You're not even here, " + pn + "!", "Where are you? Come find me first!");
            if (r.workBusy()) return pick(rnd, "I'd love to, but I'm working right now. Maybe after my shift?", "Sorry " + pn + ", I can't leave work!");
            if (r.activityName().equals("sleep")) return "It's way past my bedtime... tomorrow?";
            if (!r.isFree()) return pick(rnd, "Give me a moment, I'm in the middle of something.", "Hang on, I'm a bit busy right now.");
            if (trust < 0) return pick(rnd, "Hmm... I'd rather not, " + pn + ".", "Why? Where are we going?");
            r.follow(pl, 2400);
            return pick(rnd, "Sure, lead the way!", "Okay, I'm coming with you!", "Where are we off to? Lead on!");
        }
        if (any(t, " stop following ", " wait here ", " stay here ", " stay ", " stop ", " you can go ", " go home ", " leave me ", " go away ", " that's all ", " thats all ") && r.following(pl)) {
            r.stopSeeking();
            c.topic = "";
            return any(t, " go away ", " leave me ") ? pick(rnd, "Oh. Okay then...", "Fine, fine. I'm going!") : pick(rnd, "Okay, I'll wait here!", "Alright, I'll head back then. That was fun!", "Sure thing. See you later, " + pn + "!");
        }
        String qk = qKey(t);
        if (!qk.isEmpty() && qk.equals(c.lastQ) && now - c.qAt < REPEAT) {
            c.qAt = now;
            return pick(rnd, "You just asked me that, " + pn + "!", "Ha, same answer as a minute ago!", "Didn't I just tell you? Nothing's changed!", "Deja vu! You asked me that already.");
        }
        if (c.topic.equals("asked_you") && qk.isEmpty()) {
            c.topic = "";
            if (any(t, " bad ", " not good ", " not great ", " sad ", " tired ", " awful ", " terrible ", " meh ", " rough ", " sick ", " bored ")) return pick(rnd, "Aw, sorry to hear that. Hope it gets better!", "Oh no. Want to hang out for a bit? Might help.", "That's rough. Hang in there, " + pn + ".");
            if (any(t, " good ", " fine ", " great ", " well ", " ok ", " okay ", " alright ", " awesome ", " amazing ", " not bad ", " fantastic ", " pretty good ")) return pick(rnd, "Glad to hear it!", "Good! That's what I like to hear.", "Nice! Keep it that way.");
        }
        if (focused && qk.isEmpty() && any(t, " i'm good ", " im good ", " i'm fine ", " im fine ", " i'm great ", " im great ", " i'm ok ", " im ok ", " i'm okay ", " im okay ")) {
            c.topic = "";
            return pick(rnd, "Good to hear!", "Glad you're doing well, " + pn + ".", "Nice!");
        }
        if (focused && qk.isEmpty() && any(t, " please don't ", " please dont ", " don't ", " dont ", " no ", " nah ", " nope ")) {
            String was = c.topic;
            c.topic = "";
            if (was.equals("follow")) return "Oh, okay. Never mind then!";
            return pick(rnd, "Okay, okay!", "Alright, no worries.", "Fair enough!");
        }
        if (focused && qk.isEmpty() && !named && t.trim().split(" ").length <= 4 && any(t, " ok ", " okay ", " k ", " cool ", " nice ", " lol ", " haha ", " yeah ", " yup ", " yes ", " sure ", " good ", " that's good ", " thats good ", " oh ", " wow ", " fun ", " same ", " true ", " mhm ", " hmm ", " interesting ", " neat ")) {
            String was = c.topic;
            c.topic = "";
            if (was.equals("how")) {
                c.topic = "asked_you";
                return pick(rnd, "Anyway, how about you, " + pn + "?", "And you? How are you doing?", "What about you, how's your day?");
            }
            if (was.equals("doing")) return pick(rnd, "What about you, what are you up to?", "Yeah! Just a normal day, really.");
            if (was.equals("news")) return pick(rnd, "I know, right?", "That's what I heard, anyway!");
            return pick(rnd, "Mm-hm!", "Yeah!", "Ha, right?", "Heh.");
        }
        if (cold && !any(t, " bye ")) return pick(rnd, "I don't really want to talk to you right now, " + pn + ".", "Hmph. What do you want?", "After what you did? No thanks.");
        if (any(t, " remember ", " memory ", " recall ")) {
            String line = Mind.playerLine(p, pn, day + 1, rnd);
            if (line != null) return line;
            Mind.Ep e = m.best(day + 1, x -> x.imp >= 4);
            if (e != null) return "What I remember most lately? " + Events.sentence(e.forPlayer(pn)) + " - " + Calendar.relative(e.day, day) + ".";
            return "Hmm, I don't think we've done much together yet! We should change that.";
        }
        if (any(t, " what are you doing ", " whatcha doing ", " what're you doing ", " where are you going ", " where you going ", " what you up to ", " up to ")) {
            c.topic = "doing";
            String why = r.leisureReason();
            return "I'm " + r.status() + "." + (why.isEmpty() || r.status().contains(why) ? "" : " " + Events.sentence(why) + "!");
        }
        if (any(t, " how are you ", " how's it going ", " hows it going ", " how are things ", " you ok ", " you okay ", " how was your day ", " how's your day ", " hows your day ")) {
            int mood = p.mood();
            String laid = mood > 25 ? Reception.persona("how", r, pn, rnd) : null;
            String feel = laid != null ? laid : mood > 75 ? pick(rnd, "Honestly? Fantastic!", "Really good, thanks for asking!") : mood > 45 ? pick(rnd, "Not bad, not bad.", "Pretty good!") : mood > 25 ? pick(rnd, "Meh. Could be better.", "A bit tired, to be honest.") : "Not great, honestly...";
            if (p.hunger < 25) feel += " I'm starving, though.";
            DayLog lg = p.log(r.routineDay());
            String sk = p.id + "|" + pn;
            Long told = STORY_TOLD.get(sk);
            String story = "";
            if (!lg.empty() && (told == null || told != day)) {
                story = " " + lg.story(p, r.weekendNow());
                STORY_TOLD.put(sk, day);
            }
            if (story.isEmpty() || rnd.nextInt(3) == 0) {
                c.topic = "asked_you";
                return feel + story + " " + pick(rnd, "How about you, " + pn + "?", "And you?", "How are you doing?");
            }
            c.topic = "how";
            return feel + story;
        }
        if (any(t, " dream", " sleep well ", " slept ")) {
            String dr = m.dreamToday(day);
            return dr != null ? "Funny you ask! " + dr : pick(rnd, "I slept like a log - no dreams I can remember.", "Can't remember my dreams at all, sadly.");
        }
        if (any(t, " plan ", " plans ", " tomorrow ", " doing today ", " goal ", " saving for ")) {
            String plan = !m.intent.isEmpty() && m.intentDay == day ? "Today I'm hoping to " + m.intent + "." : "No big plans today.";
            String goal = p.goal.isEmpty() ? "" : " I'm saving up for " + p.goal + " - " + Bank.savings(d, p.id) + " of " + p.goalCost + " coins so far.";
            return plan + goal;
        }
        if (any(t, " favourite ", " favorite ", " like to eat ", " love to eat ")) {
            String fav = Economy.label(Memory.favourite(p));
            String place = null;
            int best = 15;
            for (Map.Entry<String, Integer> e : m.places.entrySet()) if (e.getValue() > best && Place.get(e.getKey()) != null && !e.getKey().startsWith("apt") && !e.getKey().startsWith("pod") && !e.getKey().equals(p.home) && !e.getKey().equals(p.job.workKey)) { best = e.getValue(); place = Place.label(e.getKey()); }
            return "My favourite food is " + fav + "." + (place != null ? " And lately I love hanging out at " + place + "." : " And my favourite thing to do is " + Memory.hobby(p) + ".");
        }
        if (any(t, " news ", " gossip ", " what's new ", " whats new ", " anything new ", " heard ")) {
            CityData.Event fresh = Events.freshestUnshared(d, p, pn);
            if (fresh != null) {
                Events.markTold(d, p, pn, fresh);
                c.topic = "news";
                return "Did you hear? " + Events.sentence(fresh.text) + "!";
            }
            return "Nothing new that I know of. Quiet week!";
        }
        if (!cold) {
            String desk = Reception.chat(pl, r, t, rnd);
            if (desk != null) return desk;
            String fl = Reception.flirt(pl, r, t, rnd);
            if (fl != null) return fl;
        }
        if (!any(t, " love you ")) {
            String more = Intents.more(pl, r, t, c);
            if (more != null) return more;
        }
        for (CityData.Profile q : d.profiles.values()) {
            if (q == p) continue;
            if (!java.util.regex.Pattern.compile("\\b" + q.name.toLowerCase(Locale.ROOT) + "\\b").matcher(t).find()) continue;
            CityData.Rel rel = d.peekRel(p.id, q.id);
            if (q.id.equals(p.partner)) return q.name + "? " + pick(rnd, "They're my everything. Don't tell them I said that!", "We're together, you know. Best thing that ever happened to me.");
            if (rel == null || !rel.met) return "I don't think I've met " + q.name + " yet, actually.";
            if (rel.rival) return pick(rnd, "Ugh, " + q.name + ". Don't get me started.", q.name + " and I don't exactly get along.");
            if (rel.bestFriend()) return q.name + " is one of my best friends! " + (m.eps.stream().anyMatch(e -> e.involves(q.id)) ? "We've had some good times." : "");
            if (rel.friend()) return q.name + "? We're friends! Nice person.";
            return q.name + "? I know them a little. Seems alright.";
        }
        if (any(t, " thank ", " thanks ", " thx ", " ty ") && Reception.persona("thanks", r, pn, rnd) != null) return Reception.persona("thanks", r, pn, rnd);
        if (any(t, " thank ", " thanks ", " thx ", " ty ")) return pick(rnd, "Anytime, " + pn + "!", "You're welcome!", "Happy to help!");
        if (any(t, " love you ", " you're great ", " youre great ", " you're awesome ", " youre awesome ", " you're the best ", " youre the best ", " you're cool ", " youre cool ", " you're nice ", " youre nice ", " you're amazing ", " youre amazing ", " you are ")) {
            if (!m.lastVisit.containsKey("compliment:" + day)) {
                m.lastVisit.put("compliment:" + day, day);
                m.trust.put(pn, Math.min(100, trust + 2));
            }
            r.particles(net.minecraft.core.particles.ParticleTypes.HEART, 2);
            String pc = Reception.persona(any(t, " love you ") ? "love" : "compliment", r, pn, rnd);
            if (pc != null) return pc;
            return pick(rnd, "Aww, stop it! You're making me blush.", "Right back at you, " + pn + "!", "Ha! Thanks, " + pn + ".");
        }
        boolean bye = any(t, " bye ", " goodbye ", " see ya ", " goodnight ", " good night ", " see you later ", " see you soon ", " cya ", " gotta go ", " ttyl ") || t.trim().matches("(later|see you|bye bye|laters|peace|night)");
        if (bye) {
            c.topic = "bye";
            if (r.following(pl)) r.stopSeeking();
        } else if (c.topic.equals("bye")) c.topic = "";
        if (bye && Reception.persona("bye", r, pn, rnd) != null) return Reception.persona("bye", r, pn, rnd);
        if (bye) return pick(rnd, "Bye, " + pn + "!", "See you around!", "Take care, " + pn + "!");
        if (any(t, " hi ", " hello ", " hey ", " yo ", " morning ", " evening ", " sup ")) {
            String memo = Mind.playerLine(p, pn, day, rnd);
            String ph = Reception.persona("hi", r, pn, rnd);
            if (ph != null) return ph + (memo != null ? " " + memo : "");
            return Dialogue.greeting(r.timeOfDay()) + ", " + pn + "!" + (memo != null ? " " + memo : trust > 20 ? " Always nice to see you." : "");
        }
        if (!named && !focused) return null;
        String u = Understand.reply(pl, r, t, c);
        if (u != null) return u;
        c.topic = "";
        return Understand.fallback(pl, r, t, c);
    }

    private static String pick(RandomSource r, String... o) {
        return Lines.pick(r, o);
    }
}
