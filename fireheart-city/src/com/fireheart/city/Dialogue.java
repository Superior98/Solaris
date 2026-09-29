package com.fireheart.city;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

public final class Dialogue {
    public record Line(boolean byA, String text, int gesture, Runnable action) {}

    public static final class Script {
        public final List<Line> lines = new ArrayList<>();
        public Runnable onDone = () -> {};
        public Runnable after = null;
        public String kind = "chat";

        Script a(String t) { return a(t, 0, null); }
        Script b(String t) { return b(t, 0, null); }
        Script a(String t, int g) { return a(t, g, null); }
        Script b(String t, int g) { return b(t, g, null); }
        Script a(String t, int g, Runnable r) { lines.add(new Line(true, t, g, r)); return this; }
        Script b(String t, int g, Runnable r) { lines.add(new Line(false, t, g, r)); return this; }
    }

    private static String pick(RandomSource r, String... opts) {
        return Lines.pick(r, opts);
    }

    static String greeting(long t) {
        if (t >= 22500 || t < 5000) return "Good morning";
        if (t < 11000) return "Good afternoon";
        return "Good evening";
    }

    static String here(Resident r) {
        Place best = null;
        double bd = 14 * 14;
        for (Place p : Place.ALL.values()) {
            double d = r.distanceToSqr(p.pos.getX() + 0.5, p.pos.getY(), p.pos.getZ() + 0.5);
            if (d < bd) { bd = d; best = p; }
        }
        if (best == null && Elevator.floorOfEntity(r) >= 0) return "Ember Heights";
        return best == null ? (r.getY() > 150 ? "Neon Heights" : "around town") : best.label;
    }

    private static boolean eligible(CityData.Profile pa, CityData.Profile pb, CityData.Rel ab, CityData.Rel ba) {
        return pa.partner.isEmpty() && pb.partner.isEmpty() && ab.friend() && ba.friend() && ab.aff >= 35 && ba.aff >= 35 && Economy.compat(pa.trait, pb.trait) >= 0;
    }

    private static void transfer(CityData.Profile from, CityData.Profile to, String item) {
        if (from.count(item) <= 0) return;
        from.add(item, -1);
        to.add(item, 1);
    }

    public static Script build(Resident a, Resident b, CityData data, long day, String kind) {
        if ("bank".equals(kind)) return Bank.script(a, b, data, day);
        RandomSource r = a.getRandom();
        CityData.Profile pa = a.profile(), pb = b.profile();
        CityData.Rel ab = data.rel(pa.id, pb.id), ba = data.rel(pb.id, pa.id);
        Script s = new Script();
        String where = here(a);
        long t = a.timeOfDay();
        int compat = Economy.compat(pa.trait, pb.trait);

        if (!ab.met || !ba.met) {
            s.kind = "intro";
            switch (pa.trait) {
                case SHY -> s.a("Um... hi. I'm " + pa.name + ". I don't think we've met.");
                case GRUMPY -> s.a("Hm. Haven't seen you before. " + pa.name + ".");
                case TALKATIVE -> s.a(greeting(t) + "! I don't think we've met - I'm " + pa.name + ", and you are?", Resident.G_WAVE);
                case LAIDBACK -> s.a(pick(r, "Hey. I'm " + pa.name + ". Figured we'd bump into each other eventually.", "Oh, hey you. " + pa.name + ". No rush, but I've been meaning to say hi."), Resident.G_WAVE);
                default -> s.a(pick(r, "Hi there! I don't think we've met - I'm " + pa.name + ".",
                        "I've seen you around but we've never actually talked - I'm " + pa.name + ".",
                        greeting(t) + "! I'm " + pa.name + ", nice to meet you."), Resident.G_WAVE);
            }
            if (pb.trait == Trait.GRUMPY) s.b(pb.name + ". Nice to meet you, I suppose.");
            else if (ba.knowsJob) s.b("Oh, you're the " + pa.job.title.toLowerCase() + "! I've heard about you. I'm " + pb.name + ".", Resident.G_WAVE);
            else s.b(pick(r, "Nice to meet you, " + pa.name + "! I'm " + pb.name + ".", "Hi " + pa.name + "! I'm " + pb.name + ". Funny we haven't met before!"), Resident.G_WAVE);
            s.a("So what do you do around here, " + pb.name + "?");
            s.b("I'm the " + pb.job.title.toLowerCase() + " at " + pb.job.work().label + "." + (pb.trait == Trait.FRIENDLY || pb.trait == Trait.CHEERFUL ? " Come by sometime!" : ""));
            s.a(pick(r, "Oh nice! I work at " + pa.job.work().label + " - I'm the " + pa.job.title.toLowerCase() + ".",
                    "Cool! I'm the " + pa.job.title.toLowerCase() + " over at " + pa.job.work().label + "."));
            s.b(pick(r, "Well, welcome to Solaris!", "See you around, " + pa.name + "!", "Glad to know a friendly face here."));
            s.onDone = () -> {
                int first = compat * 5 + r.nextInt(7) - 3;
                for (CityData.Rel x : new CityData.Rel[]{ab, ba}) { x.met = true; x.knowsJob = true; x.fam += 8; x.chats++; x.lastChatDay = day; x.aff += first; }
                data.news(day, pa.name + " met " + pb.name + " at " + where + ".");
            };
            return s;
        }

        boolean vendorB = b.activityName().equals("work") && Economy.sellsFood(pb.job) && b.blockPosition().closerThan(pb.job.work().pos, 8);
        if ("shop".equals(kind) && vendorB && pa.job != pb.job) return shop(a, b, pa, pb, ab, ba, data, day, r);

        boolean partners = pa.partner.equals(pb.id);
        boolean datePlan = false;
        for (CityData.Plan pl : data.plansFor(pa.id, day)) if (pl.what.equals("date") && pl.who.contains(pb.id)) datePlan = true;
        if ((partners || datePlan) && a.activityName().equals("leisure")) return date(a, b, pa, pb, ab, ba, data, day, r, partners);

        List<Runnable> effects = new ArrayList<>();
        boolean wasFriend = ab.friend() && ba.friend();
        boolean arguing = ab.rival ? r.nextFloat() < 0.6f : compat < 0 && r.nextFloat() < 0.3f || (pa.mood() < 30 && r.nextFloat() < 0.25f);
        boolean wantsPeace = String.valueOf(day).equals(ab.facts.get("makeup")) || String.valueOf(day).equals(ba.facts.get("makeup"));
        if (wantsPeace && ab.rival) arguing = r.nextFloat() < 0.15f;
        boolean makeUp = ab.rival && !arguing && r.nextFloat() < (wantsPeace ? 0.95f : 0.5f);

        if (ab.rival && !makeUp) s.a(pick(r, "Oh. It's you.", pb.name + ".", "Hmph. " + pb.name + "."));
        else if (partners) s.a(pick(r, "Hey you!", "There you are, " + pb.name + "!", "Hi sweetie!"), Resident.G_WAVE);
        else s.a(pick(r, greeting(t) + ", " + pb.name + "!", "Hey " + pb.name + "!", "Oh, " + pb.name + "! Fancy seeing you " + (where.contains("apartment") || where.contains("pod") || where.contains("home") ? "here" : "at " + where) + ".", pb.name + "! How's it going?"), Resident.G_WAVE);
        if (ab.rival && !makeUp) s.b(pick(r, "What do you want, " + pa.name + "?", "Ugh."));
        else s.b(pick(r, "Hey " + pa.name + "! Not bad, you?", "Hi " + pa.name + "! Good to see you.", pa.name + "! Just the person I wanted to see."), Resident.G_WAVE);

        if (makeUp) {
            s.a("Look... I'm sorry about before. Can we start over?", Resident.G_THINK);
            s.b(pick(r, "Yeah. I was being silly too. Friends?", "...Okay. Apology accepted."));
            s.a("Friends!", Resident.G_CHEER, () -> { a.particles(ParticleTypes.HAPPY_VILLAGER, 8); b.particles(ParticleTypes.HAPPY_VILLAGER, 8); });
            effects.add(() -> {
                ab.rival = false; ba.rival = false; ab.aff = Math.max(ab.aff, 10); ba.aff = Math.max(ba.aff, 10);
                data.event(day, "social", pa.name + " and " + pb.name + " made up after their argument", a.blockPosition(), pa.id, pb.id);
            });
        } else if (arguing) {
            String[][] fights = {
                    {"The clock tower is running two minutes slow, you know.", "It is NOT. Zara checks it every single day."},
                    {"Honestly, the diner's burgers are overrated.", "Take that back right now!"},
                    {"Neon Heights is way too bright at night.", "That's literally the whole point of it!"},
                    {"You took my spot at the pier again.", "It's a public pier! There's no 'your spot'!"},
                    {"Your music was so loud last night.", "What music? I was asleep by nine!"},
                    {"The shuttle is always late when you're on it.", "That's ridiculous and you know it."},
                    {"Why do you always cut in line for the elevator?", "I do not! You were busy staring at your feet!"}
            };
            String[] f = Lines.pickRow(r, fights);
            s.a(f[0], Resident.G_ANGRY);
            s.b(f[1], Resident.G_ANGRY, () -> b.particles(ParticleTypes.ANGRY_VILLAGER, 4));
            s.a(pick(r, "Hmph! Whatever.", "Fine. Be that way.", "I'm not arguing about this."), Resident.G_ANGRY, () -> a.particles(ParticleTypes.ANGRY_VILLAGER, 4));
            s.b(pick(r, "Fine!", "Good day to you too.", "Ugh."), Resident.G_ANGRY);
            effects.add(() -> {
                ab.aff -= 10 + r.nextInt(6);
                ba.aff -= 10 + r.nextInt(6);
                pa.social = Math.max(0, pa.social - 10);
                pb.social = Math.max(0, pb.social - 10);
                if (!ab.rival && ab.aff < -25 && ba.aff < -25) {
                    ab.rival = true; ba.rival = true;
                    data.event(day, "social", pa.name + " and " + pb.name + " had a big falling out", a.blockPosition(), pa.id, pb.id);
                }
                if (partners && ab.aff < 5) breakUp(data, pa, pb, day, a);
            });
            s.onDone = () -> { for (Runnable e : effects) e.run(); ab.chats++; ba.chats++; ab.lastChatDay = day; ba.lastChatDay = day; };
            return s;
        }

        int topics = 1 + r.nextInt(2);
        List<Integer> used = new ArrayList<>();
        topics -= memories(s, a, b, pa, pb, ab, ba, data, day, r, effects, partners);
        if (topics > 0) topics -= Mind.talk(s, a, b, pa, pb, data, day, r, effects);
        CityData.Event gossip = Events.gossipFor(data, pa, pb, day);
        if (gossip != null && r.nextFloat() < 0.65f) {
            CityData.Event g = gossip;
            boolean aboutA = g.text.contains(pa.name);
            String src = pa.heardFrom.get(g.id);
            if (!aboutA && src != null && !src.equals(pb.name)) s.a(src + " told me that " + g.text + "!", Resident.G_THINK);
            else s.a((aboutA ? "Guess what? " : pick(r, "Did you hear? ", "Have you heard? ", "So apparently ")) + Events.sentence(g.text) + "!", Resident.G_THINK);
            s.b(Events.reaction(g, pb, r));
            effects.add(() -> { pb.learn(g.id); pb.heard(g.id, pa.name); });
            topics--;
        }
        if (topics > 0 && !ab.talkedRecently("civic", day, 1) && r.nextFloat() < 0.5f && Talk.talk(s, a, b, pa, pb, data, day, r, effects)) {
            effects.add(() -> { ab.talked.put("civic", day); ba.talked.put("civic", day); });
            topics--;
        }
        if (topics > 0 && !ab.talkedRecently("money", day, 2) && r.nextFloat() < 0.35f && Bank.talk(s, a, b, pa, pb, data, day, r, effects)) {
            effects.add(() -> { ab.talked.put("money", day); ba.talked.put("money", day); });
            topics--;
        }
        if (pa.hunger < 35 && Economy.bestFood(pa) == null) {
            String food = Economy.bestFood(pb);
            s.a(pick(r, "I'm so hungry... I haven't eaten all day.", "My stomach is growling so loud."));
            if (food != null && (ba.friend() || pb.trait == Trait.FRIENDLY || pb.trait == Trait.CHEERFUL)) {
                s.b("Here, take my " + Economy.label(food).replace("a ", "").replace("an ", "") + "!", Resident.G_GIVE, () -> b.showItem(food, 40));
                s.a("You're a lifesaver, " + pb.name + "!", Resident.G_CHEER, () -> { transfer(pb, pa, food); a.showItem(food, 30); });
                effects.add(() -> { pb.rep += 3; ab.aff += 6; });
                s.after = () -> a.startEating(food);
            } else {
                s.b(pick(r, "You should grab something at the diner!", "The bakery should still have bread!", "Go eat something, silly!"));
            }
            topics--;
        }
        if (topics > 0 && ab.friend() && r.nextFloat() < 0.45f) {
            String iWant = Economy.surplus(pb, Economy.wants(pa.job));
            String theyWant = Economy.surplus(pa, Economy.wants(pb.job));
            if (iWant != null && theyWant != null) {
                s.a("Hey, got any spare " + Economy.label(iWant).replaceFirst("^(a|an|some) ", "") + "? I could trade you " + Economy.label(theyWant) + ".", Resident.G_THINK);
                s.b("Deal!", Resident.G_GIVE, () -> { b.showItem(iWant, 40); a.showItem(theyWant, 40); });
                s.a("Pleasure doing business!", Resident.G_GIVE, () -> { transfer(pb, pa, iWant); transfer(pa, pb, theyWant); a.showItem(iWant, 30); b.showItem(theyWant, 30); a.level().playSound(null, a.blockPosition(), net.minecraft.sounds.SoundEvents.ITEM_PICKUP, net.minecraft.sounds.SoundSource.NEUTRAL, 0.5f, 1.2f); });
                effects.add(() -> { pa.rep += 2; pb.rep += 2; ab.aff += 3; ba.aff += 3; });
                topics--;
            } else if (iWant == null && theyWant != null && (ab.bestFriend() || partners || r.nextFloat() < 0.3f)) {
                s.a("Oh! I've got " + Economy.label(theyWant) + " you might need. Here!", Resident.G_GIVE, () -> a.showItem(theyWant, 40));
                s.b("For me? Thanks, " + pa.name + "!", Resident.G_CHEER, () -> { transfer(pa, pb, theyWant); b.showItem(theyWant, 30); });
                effects.add(() -> { pa.rep += 3; ba.aff += 6; });
                topics--;
            }
        }
        if (topics > 0 && (pa.count("minecraft:poppy") > 0 || pa.count("minecraft:dandelion") > 0) && (ab.bestFriend() || ab.romance > 20)) {
            String fl = pa.count("minecraft:poppy") > 0 ? "minecraft:poppy" : "minecraft:dandelion";
            s.a("I picked this for you, " + pb.name + ".", Resident.G_GIVE, () -> a.showItem(fl, 40));
            s.b("Aww, it's beautiful! Thank you!", Resident.G_CHEER, () -> { transfer(pa, pb, fl); b.showItem(fl, 40); b.particles(ParticleTypes.HEART, 3); });
            effects.add(() -> { ba.aff += 8; ba.romance += 5; ab.romance += 3; });
            topics--;
        }
        boolean coworkers = pa.job.workKey.equals(pb.job.workKey);
        for (int i = 0; i < topics + 1 && used.size() < 3; i++) {
            if (i >= topics) break;
            int k = r.nextInt(12);
            if (used.contains(k) || ab.talkedRecently("k" + k, day, 2)) continue;
            used.add(k);
            final String tk = "k" + k;
            effects.add(() -> { ab.talked.put(tk, day); ba.talked.put(tk, day); });
            switch (k) {
                case 0 -> {
                    if (a.level().isThundering()) { s.a("What a storm, huh?"); s.b("I nearly got blown off the pier!"); }
                    else if (a.level().isRaining()) { s.a("Looks like rain again."); s.b(pick(r, "Good for Ivy's flowers at least.", "I forgot my umbrella, as usual.")); }
                    else if (t > 12000 && t < 22500) { s.a("The neon on the sky island looks amazing at night."); s.b("I know! You can see the beacon from everywhere."); }
                    else { s.a(pick(r, "Lovely weather today, isn't it?", "What a beautiful day.")); s.b(pick(r, "Perfect for a walk by the marina.", "Too nice to be stuck at work!")); }
                }
                case 1 -> {
                    s.a("How's work at " + pb.job.work().label + "?");
                    int st = Shop.stock((ServerLevel) a.level(), data, pb.job);
                    DayLog bl = pb.log(b.routineDay());
                    if (coworkers) s.b("Same as yours, silly - we work together!");
                    else if (b.weekendNow()) s.b("It's the weekend! I'm not even thinking about work.");
                    else if (!bl.made.isEmpty()) s.b("Good! I've " + (pb.job == Job.BAKER ? "baked " : "made ") + bl.madeText(2) + " so far today.");
                    else if (Economy.sellsFood(pb.job)) s.b(st == 0 ? "We sold out of everything today!" : st > 30 ? "Slow day. Tell everyone to come buy something!" : pick(r, pb.job.shout, "Busy! People can't get enough."));
                    else s.b(pick(r, pb.job.shout, "Busy! But I like it.", "Honestly? A bit tiring today."));
                    effects.add(() -> ab.knowsJob = true);
                }
                case 2 -> {
                    Place h = pa.homePlace();
                    if (h == null) break;
                    s.a("I've settled into " + h.label + " now.");
                    Place hb = pb.homePlace();
                    boolean sameBuilding = hb != null && h.key.startsWith("apt") && hb.key.startsWith("apt");
                    if (sameBuilding) {
                        s.b("No way, I'm in " + hb.label + "! We're neighbours!", Resident.G_CHEER);
                        effects.add(() -> { ba.knowsHome = true; ab.knowsHome = true; ab.fam += 4; ba.fam += 4; });
                    } else if (hb != null) {
                        s.b("Nice! I'm over in " + hb.label + ".");
                        effects.add(() -> { ba.knowsHome = true; ab.knowsHome = true; });
                    }
                }
                case 3 -> {
                    CityData.Profile third = null;
                    for (CityData.Profile p : data.profiles.values()) {
                        if (p.id.equals(pa.id) || p.id.equals(pb.id)) continue;
                        CityData.Rel at = data.peekRel(pa.id, p.id), bt = data.peekRel(pb.id, p.id);
                        if (at != null && at.knowsJob && (bt == null || !bt.knowsJob)) { third = p; break; }
                    }
                    if (third == null) break;
                    CityData.Profile tp = third;
                    CityData.Rel at = data.rel(pa.id, tp.id);
                    String opinion = at.rival ? "Don't get me started on them. " : at.friend() ? "Good friend of mine - " : "";
                    s.a("Have you met " + tp.name + "? " + opinion + "They're the " + tp.job.title.toLowerCase() + " at " + tp.job.work().label + ".");
                    s.b(pick(r, "Not yet! I'll say hi next time.", "Oh, I think I've seen them around!"));
                    effects.add(() -> { CityData.Rel bt = data.rel(pb.id, tp.id); bt.knowsJob = true; bt.fam += 1; if (at.rival) bt.aff -= 3; else if (at.friend()) bt.aff += 2; });
                }
                case 4 -> {
                    Player pl = a.level().getNearestPlayer(a, 14);
                    if (pl == null) {
                        s.a(pick(r, "Have you been up to Neon Heights yet?", "Did you see the Skyliner fly over this morning?"));
                        s.b(pick(r, "Not yet, is it worth the shuttle ride?", "Yes! It's like a spaceship.", "The spire up there is incredible."));
                    } else {
                        String n = pl.getName().getString();
                        boolean known = data.playerRel(pa.id, n).met;
                        s.a(known ? "Look, " + n + " is here! They built this whole city, you know." : "Who's that over there? I don't think I've met them.");
                        s.b(known ? pick(r, "I've heard! Hi " + n + "!", "The Skyliner was their idea too.") : "No idea - maybe say hello?", Resident.G_WAVE);
                    }
                }
                case 5 -> {
                    if (a.getY() > 150 && a.distanceToSqr(-24, 182, 251) < 400) {
                        s.a("Have you seen the memorial here? It's for Benson, Solaris's dog.");
                        s.b("I read the plaque. Such a good, loyal dog.");
                    } else {
                        s.a("What do you do on your days off?");
                        s.b(pb.trait == Trait.ADVENTUROUS ? "Take the shuttle up to Neon Heights, obviously!" : pick(r, "Sit by the fountain at the plaza.", "Watch the boats at the marina.", "Sleep, mostly."));
                    }
                }
                case 6 -> {
                    List<String> fr = data.friendsOf(pa.id);
                    fr.remove(pb.id);
                    if (fr.isEmpty()) { s.a("I'm still getting to know people here."); s.b("Same. It takes time in a new city."); }
                    else {
                        CityData.Profile f = data.profiles.get(fr.get(r.nextInt(fr.size())));
                        s.a("I hung out with " + f.name + " recently - they're great.");
                        s.b("I should get to know " + f.name + " better.");
                        effects.add(() -> { CityData.Rel bt = data.rel(pb.id, f.id); bt.fam += 1; bt.aff += 1; });
                    }
                }
                case 7 -> {
                    if (pb.tier >= 3 && pa.tier < pb.tier) {
                        s.a(pick(r, "Everyone in town talks about you, " + pb.name + "!", "You're kind of famous around here, you know."));
                        s.b(pb.trait == Trait.SHY ? "M-me? Really? Wow..." : pick(r, "Ha! I just like helping out.", "Well, I do make a mean " + Economy.label(Economy.products(pb.job).get(0)).replaceFirst("^(a|an|some) ", "") + "."));
                        effects.add(() -> ab.aff += 2);
                    } else if (pa.coins > 60) {
                        s.a("Business has been good lately. I might treat myself!");
                        s.b(pick(r, "Lucky! Treat me too?", "Save some for a rainy day."));
                    } else if (pa.coins < 5 && ba.friend() && pb.coins >= 30 && !ab.facts.containsKey("owe")) {
                        s.a("I'm almost out of coins... payday can't come soon enough.", Resident.G_THINK);
                        s.b("Here, borrow 10 coins. Pay me back when you can.", Resident.G_GIVE, () -> b.showItem("minecraft:gold_nugget", 40));
                        s.a("You're a real friend, " + pb.name + ". I won't forget this!", Resident.G_CHEER);
                        effects.add(() -> {
                            if (data.pay(pb.id, pa.id, 10, "Loan to " + pa.name, day, (int) t)) {
                                ab.facts.put("owe", "10");
                                ba.facts.put("lent", "10");
                                pa.log(a.routineDay()).note(pb.name + " lent me 10 coins");
                                pb.log(b.routineDay()).note("I lent " + pa.name + " 10 coins");
                                ab.aff += 6;
                            }
                        });
                    } else if (pa.coins < 5) {
                        s.a("I'm almost out of coins... payday can't come soon enough.");
                        s.b(pick(r, "Hang in there!", "Same, honestly."));
                    } else {
                        s.a("The clock tower's always right on time, isn't it?");
                        s.b("Zara takes that job very seriously.");
                    }
                }
                case 8 -> {
                    boolean both = pa.home.startsWith("apt") && pb.home.startsWith("apt");
                    if (both && Elevator.isBroken()) { s.a("The elevator's broken AGAIN."); s.b("I know! I had to take the service lift."); }
                    else if (both) { s.a(pick(r, "The elevator line this morning was so long.", "Do you ever chat with people while waiting for the elevator?")); s.b(pick(r, "Only one person at a time, those are the rules.", "It's the best part of my morning, honestly.")); }
                    else { s.a("I heard Ember Heights has a glass elevator now."); s.b("Fancy! Only one rider at a time though."); }
                }
                case 9 -> {
                    CityData.Profile couple = null;
                    for (CityData.Profile p : data.profiles.values()) if (!p.partner.isEmpty() && !p.id.equals(pa.id) && !p.id.equals(pb.id) && pa.known.size() > 0) { couple = p; break; }
                    if (couple != null && data.profiles.get(couple.partner) != null) {
                        s.a("Have you seen " + couple.name + " and " + data.profiles.get(couple.partner).name + " together? So cute.");
                        s.b(pick(r, "They're adorable!", "I saw them at the noodle bar, holding hands!"));
                    } else {
                        s.a("Do you think anyone in town is dating yet?");
                        s.b(pick(r, "Ooh, I have my suspicions...", "It's only been a few days!"));
                    }
                }
                case 10 -> {
                    CityData.Event e = Events.latestBuild(data);
                    if (e != null) {
                        s.a("Did you see what's going on near " + Events.nearestLabel(e.pos) + "?");
                        s.b(pb.known.contains(e.id) ? "Yes! Something new is going up." : "No? I'll have to go look!");
                        effects.add(() -> pb.learn(e.id));
                    } else {
                        s.a("Solaris keeps growing, doesn't it?");
                        s.b("There's always something new being built.");
                    }
                }
                default -> {
                    s.a(pick(r, "I heard the diner has a new burger.", "The Skyport is so busy lately.", "Somebody said there's a dance floor up on Neon Heights."));
                    s.b(pick(r, "Ha, I noticed that too.", "Really? I'll have to check it out.", "Solaris never gets boring."));
                }
            }
        }

        boolean spark = eligible(pa, pb, ab, ba) && r.nextFloat() < 0.15f + (pa.trait == Trait.DREAMY || pa.trait == Trait.CHEERFUL ? 0.1f : 0f);
        if (spark) {
            s.a(pick(r, "You know, I really like spending time with you, " + pb.name + ".", "Is it weird that I smile every time I see you?", "I always look forward to running into you."), Resident.G_THINK);
            s.b(pick(r, "Oh! I... really like spending time with you too.", "That's not weird. I feel the same way.", "Stop it, you're making me blush!"), Resident.G_CHEER, () -> { a.particles(ParticleTypes.HEART, 2); b.particles(ParticleTypes.HEART, 2); });
            effects.add(() -> {
                ab.romance += 15; ba.romance += 15;
                if (ab.romance >= 15 && ab.romance < 30) data.event(day, "social", "people say " + pa.name + " has a crush on " + pb.name, null, pa.id);
            });
        }

        long planDay = t < 10500 ? day : day + 1;
        boolean freeToPlan = data.plansFor(pa.id, planDay).isEmpty() && data.plansFor(pb.id, planDay).isEmpty();
        boolean askDate = eligible(pa, pb, ab, ba) && ab.romance >= 30 && freeToPlan;
        boolean canPlan = !askDate && (ab.fam >= 40 || ab.friend()) && r.nextFloat() < 0.5f && freeToPlan;
        String when = planDay == day ? (Calendar.weekend(planDay) ? "later today" : "after work") : (Calendar.weekend(planDay) ? "tomorrow" : "tomorrow after work");
        if (askDate) {
            String spot = Place.DATE_SPOTS[r.nextInt(Place.DATE_SPOTS.length)];
            s.a("Would you... want to go on a date with me? Maybe " + Place.label(spot) + ", " + when + "?", Resident.G_THINK);
            s.b(pb.trait == Trait.SHY ? "Y-yes! I'd love that!" : pick(r, "I'd love to!", "I thought you'd never ask!", "It's a date!"), Resident.G_CHEER, () -> { a.particles(ParticleTypes.HEART, 4); b.particles(ParticleTypes.HEART, 4); });
            effects.add(() -> {
                data.addPlan(planDay, spot, "date", pa.id, pb.id);
                data.event(day, "social", pa.name + " asked " + pb.name + " out on a date", a.blockPosition(), pa.id, pb.id);
            });
        } else if (canPlan) {
            boolean sky = r.nextFloat() < Math.max(pa.trait.skyChance, pb.trait.skyChance) + 0.1f;
            boolean homeVisit = ab.bestFriend() && r.nextFloat() < 0.5f;
            if (homeVisit) {
                Place h = pa.homePlace();
                s.a("Come over to my place " + when + "! I'll make something nice.");
                s.b("I'd love that, " + pa.name + "!", Resident.G_CHEER);
                effects.add(() -> { data.addPlan(planDay, pa.home, "visit", pa.id, pb.id); ba.knowsHome = true; data.news(day, pb.name + " is visiting " + pa.name + " at " + h.label + "."); });
            } else if (sky) {
                boolean onIsle = a.getY() > 150;
                String[] opts = onIsle ? Place.CITY_HANGOUTS : Place.ISLE_HANGOUTS;
                String pk0 = opts[r.nextInt(opts.length)];
                for (int i = 0; i < 8 && Place.get(pk0) == null; i++) pk0 = opts[r.nextInt(opts.length)];
                if (Place.get(pk0) == null) pk0 = onIsle ? "plaza" : "isle_plaza";
                final String pk = pk0;
                s.a(onIsle ? "Want to head down to the city " + when + "? We could go to " + Place.label(pk) + "." : "Let's take the shuttle up to Neon Heights " + when + "!");
                s.b(pick(r, "I'm in!", "Yes! I've been wanting to go.", "Deal. Meet you at the Skyport."), Resident.G_CHEER);
                effects.add(() -> { data.addPlan(planDay, pk, "trip", pa.id, pb.id); data.news(day, pa.name + " and " + pb.name + " planned a trip to " + (Place.get(pk) == null ? "town" : Place.label(pk)) + "."); });
            } else {
                String[] opts = a.getY() > 150 ? Place.ISLE_HANGOUTS : Place.CITY_HANGOUTS;
                String pk0 = opts[r.nextInt(opts.length)];
                for (int i = 0; i < 8 && Place.get(pk0) == null; i++) pk0 = opts[r.nextInt(opts.length)];
                if (Place.get(pk0) == null) pk0 = a.getY() > 150 ? "isle_plaza" : "plaza";
                final String pk = pk0;
                s.a("Want to hang out at " + (Place.get(pk) == null ? "the plaza" : Place.label(pk)) + " " + when + "?");
                s.b(pb.trait == Trait.GRUMPY ? "...Fine. But you're buying." : pick(r, "Sounds great, see you there!", "Sure! I'll be there.", "It's a plan - I'll remember!"));
                effects.add(() -> data.addPlan(planDay, pk, "hangout", pa.id, pb.id));
            }
        } else {
            s.a(pick(r, "Anyway, I'd better get going.", "Well, see you around!", "Catch you later, " + pb.name + "."), Resident.G_WAVE);
            s.b(pick(r, "Bye " + pa.name + "!", "See you!", "Take care!"), Resident.G_WAVE);
        }

        s.onDone = () -> {
            for (Runnable e : effects) e.run();
            int gain = 6 + (int) (4 * (pa.trait.chattiness + pb.trait.chattiness) / 2);
            ab.fam = Math.min(100, ab.fam + gain);
            ba.fam = Math.min(100, ba.fam + gain);
            int da = 2 + compat * 2 + r.nextInt(5) - 2 + (pa.mood() > 60 ? 1 : 0);
            int db = 2 + compat * 2 + r.nextInt(5) - 2 + (pb.mood() > 60 ? 1 : 0);
            ab.aff = Math.max(-100, Math.min(100, ab.aff + da));
            ba.aff = Math.max(-100, Math.min(100, ba.aff + db));
            ab.chats++;
            ba.chats++;
            ab.lastChatDay = day;
            ba.lastChatDay = day;
            if (!wasFriend && ab.friend() && ba.friend()) data.event(day, "social", pa.name + " and " + pb.name + " became friends", a.blockPosition(), pa.id, pb.id);
            else if (ab.bestFriend() && ba.bestFriend() && ab.chats == 8) data.event(day, "social", pa.name + " and " + pb.name + " are now best friends", a.blockPosition(), pa.id, pb.id);
        };
        return s;
    }

    private static Script shop(Resident a, Resident b, CityData.Profile pa, CityData.Profile pb, CityData.Rel ab, CityData.Rel ba, CityData data, long day, RandomSource r) {
        Script s = new Script();
        s.kind = "shop";
        ServerLevel sl = (ServerLevel) a.level();
        String item = Shop.choose(sl, data, pb.job, true, null);
        String fav = Memory.favourite(pa);
        boolean favHere = Shop.contents(sl, data, pb.job).getOrDefault(fav, 0) > 0;
        if (favHere) item = fav;
        long rd = a.routineDay();
        int tod = (int) a.timeOfDay();
        if (item == null) {
            s.a(pick(r, pb.job.order, "Hi " + pb.name + "! What's good today?"));
            s.b(pick(r, "Sorry, we're all sold out! Come back in a bit.", "Oh no, you just missed the last one!"));
            s.a(pick(r, "Aw, okay. I'll try later.", "Guess I'll go hungry for now."));
            s.onDone = () -> { ab.chats++; ba.chats++; ab.fam += 2; ba.fam += 2; pb.log(b.routineDay()).note("I had to turn " + pa.name + " away - we were sold out"); };
            return s;
        }
        String it = item;
        int price = Economy.price(it);
        String noun = Economy.label(it).replaceFirst("^(a|an|some) ", "");
        boolean knowsFav = ba.facts.containsKey("food");
        if (favHere && knowsFav) {
            s.a("Hi " + pb.name + "!", Resident.G_WAVE);
            s.b("Let me guess - " + noun + ", right? Your favourite.", Resident.G_THINK);
            s.a("You know me too well!", Resident.G_CHEER);
        } else {
            s.a(pick(r, "One " + noun + ", please!", "Hi " + pb.name + "! Could I get " + Economy.label(it) + "?", favHere ? "Ooh, you have " + noun + "! My favourite. One please!" : pb.job.order));
        }
        if (pa.coins < price) {
            s.b("That'll be " + price + " coins.");
            s.a("Oh... I'm a bit short today.", Resident.G_THINK);
            if (ba.friend() || pb.trait == Trait.FRIENDLY || pb.trait == Trait.CHEERFUL) {
                s.b("Don't worry about it - this one's on the house!", Resident.G_GIVE, () -> b.showItem(it, 40));
                s.a("Really? Thank you so much!", Resident.G_CHEER, () -> {
                    if (Shop.take(sl, data, pb.job, it)) pa.add(it, 1);
                    a.showItem(it, 40);
                });
                s.onDone = () -> {
                    pb.rep += 4; ab.aff += 8; ab.chats++; ba.chats++;
                    ab.remember(pb.name + " gave me free " + noun);
                    pa.log(rd).note(pb.name + " gave me free " + noun);
                    pb.log(rd).note("I gave " + pa.name + " a free " + noun);
                    data.news(day, pb.name + " gave " + pa.name + " a free meal.");
                };
                s.after = () -> a.startEating(it);
            } else {
                s.b("Sorry, no coins, no " + noun + ".");
                s.onDone = () -> { ab.aff -= 3; ab.chats++; ba.chats++; };
            }
            return s;
        }
        s.b(pick(r, pb.job.reply, "Coming right up! That's " + price + " coins.", "Here you go, fresh! " + price + " coins, please."), Resident.G_GIVE, () -> b.showItem(it, 50));
        boolean tip = ba.friend() && pa.coins > price + 5 && r.nextFloat() < 0.35f;
        s.a("Thanks, " + pb.name + "! Here's " + price + " coins" + (tip ? ", and a little tip for you." : "."), Resident.G_GIVE, () -> {
            if (pa.coins >= price && Shop.take(sl, data, pb.job, it)) {
                data.pay(Civic.foodPayer(data, pa.id, rd), Economy.business(pb.job), price, noun + " at " + pb.job.work().label, rd, tod);
                if (tip) data.pay(pa.id, pb.id, 1, "Tip for " + pb.name, rd, tod);
                pa.add(it, 1);
                pb.log(b.routineDay()).served++;
                pb.log(b.routineDay()).note("Sold " + Economy.label(it) + " to " + pa.name);
                pa.log(rd).note("I bought " + Economy.label(it) + " from " + pb.name);
            }
            a.showItem(it, 60);
            b.showItem("minecraft:gold_nugget", 30);
            a.level().playSound(null, b.blockPosition(), net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.NEUTRAL, 0.3f, 1.5f);
        });
        s.b(pick(r, "Enjoy!", "Come again!", "See you tomorrow, " + pa.name + "!"), Resident.G_WAVE);
        s.onDone = () -> { pb.rep += 1; ab.fam += 4; ba.fam += 4; ab.chats++; ba.chats++; ab.lastChatDay = day; ba.lastChatDay = day; ab.knowsJob = true; if (favHere) ba.facts.put("food", fav); };
        s.after = () -> { if (pa.hunger < 75) a.startEating(it); };
        return s;
    }

    private static int memories(Script s, Resident a, Resident b, CityData.Profile pa, CityData.Profile pb, CityData.Rel ab, CityData.Rel ba, CityData data, long day, RandomSource r, List<Runnable> effects, boolean partners) {
        int used = 0;
        long t = a.timeOfDay();
        long rd = a.routineDay();
        boolean weekend = a.weekendNow();

        if (ab.facts.containsKey("owe") && pa.coins >= 25) {
            int amt = 10;
            s.a("Oh! Before I forget - here's the " + amt + " coins I owe you.", Resident.G_GIVE, () -> a.showItem("minecraft:gold_nugget", 40));
            s.b(pick(r, "You remembered! Thanks, " + pa.name + ".", "Honest as always. Thanks!"), Resident.G_CHEER);
            effects.add(() -> {
                if (data.pay(pa.id, pb.id, amt, "Paid back loan", rd, (int) t)) {
                    ab.facts.remove("owe");
                    ba.facts.remove("lent");
                    ba.aff += 5;
                    ba.kept++;
                    pa.log(rd).note("I paid " + pb.name + " back");
                }
            });
            used++;
        }

        if (Talk.letterThanks(s, pa, pb, ab, day, r, effects)) used++;

        CityData.Plan past = Memory.pendingReview(data, pa, pb, day);
        if (past != null && used < 2) {
            boolean aCame = past.came.contains(pa.id), bCame = past.came.contains(pb.id);
            String mem = Memory.memoryOf(past, day);
            Place pl = Place.get(past.place);
            String where = pl == null ? "town" : pl.label;
            String when = Calendar.relative(past.day, day);
            CityData.Plan pp = past;
            if (aCame && bCame) {
                if (past.what.equals("date")) s.a(pick(r, "I keep thinking about " + mem + ".", "Last night at " + where + " was perfect."), Resident.G_THINK, () -> { a.particles(ParticleTypes.HEART, 2); });
                else if (past.what.equals("visit") && past.place.equals(pb.home)) s.a("Thanks again for having me over " + when + "!", Resident.G_WAVE);
                else if (past.what.equals("visit")) s.a("I had such a nice time when you came over " + when + ".", Resident.G_WAVE);
                else s.a("That was so much fun at " + where + " " + when + "!", Resident.G_CHEER);
                s.b(pick(r, "Right? We should do it again soon.", "Best part of my week!", "Totally. Same time next week?"), Resident.G_CHEER);
                effects.add(() -> { ab.aff += 3; ba.aff += 3; ab.kept++; ba.kept++; ab.remember(mem); ba.remember(mem); });
            } else if (aCame) {
                s.a("Hey... I waited for you at " + where + " " + when + ". What happened?", Resident.G_THINK);
                String excuse = pb.yesterday.day == past.day && !pb.yesterday.notes.isEmpty() ? "I'm so sorry! " + Events.sentence(pb.yesterday.notes.get(pb.yesterday.notes.size() - 1)) + " and totally lost track of time." : pick(r, "Oh no, I completely forgot! I'm so sorry.", "Something came up... I'll make it up to you, promise.");
                s.b(excuse, Resident.G_THINK);
                s.a(pa.trait == Trait.GRUMPY ? "Hmph. Don't do it again." : pick(r, "It's okay. Just don't forget next time!", "Alright, you're forgiven. This time."));
                effects.add(() -> { ab.aff -= 5; ba.broken++; ab.remember(pb.name + " stood me up " + when); });
            } else if (bCame) {
                s.a(pick(r, "Hey " + pb.name + "...", "Oh, " + pb.name + "."));
                s.b("You never showed up at " + where + " " + when + "! I waited ages.", Resident.G_ANGRY);
                s.a(pick(r, "I'm so sorry, I completely forgot. Let me make it up to you?", "Ugh, I know, I'm the worst. Sorry!"), Resident.G_THINK);
                effects.add(() -> { ba.aff -= 5; ab.broken++; });
            } else {
                s.a("We were supposed to meet at " + where + " " + when + ", weren't we?", Resident.G_THINK);
                s.b("Ha! We both forgot. Next time, for real.");
            }
            effects.add(() -> { pp.reviewed.add(pa.id); pp.reviewed.add(pb.id); });
            used++;
        } else {
            CityData.Plan today = Memory.todayTogether(data, pa, pb, day);
            if (today != null && !ab.talkedRecently("remind" + today.day, day, 1) && used < 2) {
                Place pl = Place.get(today.place);
                String where = pl == null ? "town" : pl.label;
                boolean before = t < 10500 || t >= 22500;
                if (before) {
                    s.a("Still on for " + where + (weekend ? " later?" : " after work?"), Resident.G_THINK);
                    s.b(pick(r, "Wouldn't miss it!", "Of course! I've been looking forward to it.", "Yep, it's in my head all day."), Resident.G_CHEER);
                    CityData.Plan tp = today;
                    effects.add(() -> { ab.talked.put("remind" + tp.day, day); ba.talked.put("remind" + tp.day, day); });
                    used++;
                }
            }
        }

        if (used < 2 && ab.heardDay >= day - 2 && ab.heardDay < day && !ab.heard.isEmpty() && !ab.talkedRecently("heard", day, 1) && r.nextFloat() < 0.5f) {
            String h = ab.heard;
            String when = Calendar.relative(ab.heardDay, day);
            s.a("You told me " + when + " that " + DayLog.lower(h) + ". Anything exciting today?", Resident.G_THINK);
            s.b(pb.log(b.routineDay()).empty() ? "Not yet! Ask me later." : pb.log(b.routineDay()).story(pb, b.weekendNow()));
            effects.add(() -> { ab.talked.put("heard", day); });
            used++;
        }

        boolean late = t >= 10500 && t < 22500 || weekend && t >= 6000 && t < 22500;
        if (used < 2 && late && !ab.talkedRecently("day", day, 1) && (r.nextFloat() < 0.75f || partners)) {
            DayLog lb = pb.log(b.routineDay()), la = pa.log(rd);
            s.a(partners ? "How was your day, love?" : pick(r, "So how was your day, " + pb.name + "?", "How'd today go?"));
            String sb = lb.story(pb, b.weekendNow());
            s.b(sb);
            String sa = la.story(pa, weekend);
            s.a((la.empty() ? "" : pick(r, "Nice! ", "Ha, busy! ", "Sounds good. ")) + "Mine? " + sa);
            s.b(pick(r, "Sounds like a good day.", "We earned a rest tonight.", la.served > 5 ? "Wow, the whole town's eating your food!" : "Tomorrow will be even better."));
            String ha = la.highlight(), hb = lb.highlight();
            effects.add(() -> {
                ab.talked.put("day", day); ba.talked.put("day", day);
                if (!hb.isEmpty()) { ab.heard = hb; ab.heardDay = day; }
                if (!ha.isEmpty()) { ba.heard = ha; ba.heardDay = day; }
                ab.fam = Math.min(100, ab.fam + 3); ba.fam = Math.min(100, ba.fam + 3);
            });
            used++;
        }

        int wd = Calendar.weekday(rd);
        if (used < 2 && !ab.talkedRecently("wd", day, 1) && r.nextFloat() < 0.35f && (wd == 0 || wd == 4 || wd >= 5)) {
            if (wd == 0) { s.a("Back to work already... where did the weekend go?"); s.b(pick(r, "Tell me about it.", "Only four more days!")); }
            else if (wd == 4) { s.a("Happy Friday! I can't wait for the weekend."); s.b(pick(r, "Same! Any plans?", "Two whole days off. Bliss.")); }
            else { s.a(pick(r, "Isn't it nice not working today?", "I love weekends in Solaris.")); s.b(pick(r, "Best part of the week!", "I slept in so late.", "No alarm, no work, just fun.")); }
            effects.add(() -> ab.talked.put("wd", day));
            used++;
        }

        if (used < 2 && (ab.friend() || ab.fam >= 30) && r.nextFloat() < 0.45f) {
            String unknown = Memory.unknownFact(ab, pb);
            if (unknown != null && !ab.talkedRecently("ask" + unknown, day, 1)) {
                String fav = Memory.favourite(pb);
                switch (unknown) {
                    case "food" -> { s.a("What's your favourite food, " + pb.name + "?"); s.b("Easy - " + Economy.plural(fav) + "! I could eat them every day."); effects.add(() -> ab.facts.put("food", fav)); }
                    case "pet" -> { String pet = Pets.petName(pb.id); s.a("Do you have any pets?"); s.b("Yes! A little one called " + pet + ". Best roommate ever.", Resident.G_CHEER); effects.add(() -> ab.facts.put("pet", pet)); }
                    case "hobby" -> { String hb = Memory.hobby(pb); s.a("What do you like doing when you're not working?"); s.b("I love " + hb + "."); effects.add(() -> ab.facts.put("hobby", hb)); }
                    default -> {
                        long bd = Memory.nextBirthday(pb, day);
                        s.a("When's your birthday, by the way?");
                        s.b(bd == day ? "Today, actually!" : "Day " + (bd + 1) + " - that's " + (bd - day < 7 ? "this " : "a " ) + Calendar.name(bd) + "!", Resident.G_CHEER);
                        effects.add(() -> ab.facts.put("birthday", String.valueOf(bd)));
                    }
                }
                final String ku = unknown;
                effects.add(() -> ab.talked.put("ask" + ku, day));
                used++;
            } else if (!ab.facts.isEmpty() && !ab.talkedRecently("fact", day, 1)) {
                List<String> keys = new ArrayList<>(ab.facts.keySet());
                keys.remove("owe");
                keys.remove("lent");
                if (!keys.isEmpty()) {
                    String k = keys.get(r.nextInt(keys.size()));
                    String v = ab.facts.get(k);
                    switch (k) {
                        case "pet" -> { s.a("How's " + v + " doing?"); s.b(pick(r, v + " is spoiled rotten, as always.", v + " slept on my bed all night.", "Ha, you remembered! " + v + " is great.")); }
                        case "food" -> {
                            if (pa.count(v) > 0) {
                                s.a("I got you " + Economy.label(v) + " - I remember it's your favourite!", Resident.G_GIVE, () -> a.showItem(v, 40));
                                s.b("You remembered! That's so sweet.", Resident.G_CHEER, () -> { transfer(pa, pb, v); b.showItem(v, 40); });
                                effects.add(() -> { ba.aff += 7; ab.remember("gave " + pb.name + " their favourite " + Economy.plural(v)); });
                            } else {
                                s.a("I saw fresh " + Economy.plural(v) + " today and thought of you.");
                                s.b("Now I'm hungry! Thanks a lot, " + pa.name + ".");
                            }
                        }
                        case "hobby" -> { s.a("Been " + v + " lately?"); s.b(pick(r, "Every chance I get!", "Not as much as I'd like, work's been busy.")); }
                        case "birthday" -> {
                            long bd = Long.parseLong(v);
                            long next = Memory.nextBirthday(pb, day);
                            if (next - day <= 2 && next - day > 0) { s.a("Your birthday's coming up, isn't it?"); s.b("You remembered! It's on " + Calendar.name(next) + ".", Resident.G_CHEER); effects.add(() -> ba.aff += 4); }
                            else if (next == day) { s.a("Happy birthday, " + pb.name + "!", Resident.G_CHEER); s.b("Thank you! You're sweet for remembering."); effects.add(() -> ba.aff += 6); }
                            else { s.a("I've still got your birthday in my head - Day " + (next + 1) + ", right?"); s.b("Right! Good memory."); }
                            if (bd < day) effects.add(() -> ab.facts.put("birthday", String.valueOf(next)));
                        }
                        default -> {}
                    }
                    effects.add(() -> ab.talked.put("fact", day));
                    used++;
                }
            }
        }

        if (used < 2 && !ab.shared.isEmpty() && !ab.talkedRecently("shared", day, 2) && r.nextFloat() < 0.25f) {
            String m = ab.shared.get(r.nextInt(ab.shared.size()));
            if (!m.contains("stood me up")) {
                s.a("Remember " + m + "? I still think about that.", Resident.G_THINK);
                s.b(pick(r, "How could I forget!", "Good times. We need more of those.", "Ha! Of course I remember."), Resident.G_CHEER);
                effects.add(() -> { ab.talked.put("shared", day); ab.aff += 1; ba.aff += 1; });
                used++;
            }
        }
        return used;
    }

    private static Script date(Resident a, Resident b, CityData.Profile pa, CityData.Profile pb, CityData.Rel ab, CityData.Rel ba, CityData data, long day, RandomSource r, boolean partners) {
        Script s = new Script();
        s.kind = "date";
        String where = here(a);
        s.a(partners ? pick(r, "There's my favourite person!", "Hi, love!") : pick(r, "You came! You look great.", "Hi! I was hoping you'd show up."), Resident.G_WAVE, () -> { a.particles(ParticleTypes.HEART, 3); b.particles(ParticleTypes.HEART, 3); });
        s.b(pick(r, "Of course I came!", "I wouldn't miss it.", "Hi, you!"), Resident.G_CHEER);
        String[][] talks = {
                {"Isn't " + where + " beautiful this time of day?", "It's even better with you here."},
                {"Tell me something nobody else knows about you.", "I used to be scared of the shuttle. Don't tell anyone!"},
                {"If you could live anywhere in Solaris, where would it be?", "Somewhere with a view of the sea. With you."},
                {"I saved you the last " + Economy.label(Economy.products(pa.job).get(0)).replaceFirst("^(a|an|some) ", "") + " from work today.", "You're the sweetest."},
                {"Look, the Skyliner is flying over!", "Let's ride it together sometime."}
        };
        String[] tk = Lines.pickRow(r, talks);
        s.a(tk[0], Resident.G_THINK);
        s.b(tk[1], 0, () -> { a.particles(ParticleTypes.HEART, 2); b.particles(ParticleTypes.HEART, 2); });
        boolean couple = !partners && ab.dates >= 1 && ab.romance >= 55;
        if (couple) {
            s.a("So... I was wondering. Would you want to make this official?", Resident.G_THINK);
            s.b(pick(r, "Yes! A thousand times yes!", "I thought you'd never ask!"), Resident.G_CHEER, () -> { a.particles(ParticleTypes.HEART, 8); b.particles(ParticleTypes.HEART, 8); a.gesture(Resident.G_CHEER, 50); });
        } else {
            s.a(pick(r, "This was really nice.", "Same time tomorrow?"), Resident.G_WAVE);
            s.b(pick(r, "Definitely.", "Yes please!", "I'd like that a lot."), Resident.G_WAVE);
        }
        s.onDone = () -> {
            ab.dates++; ba.dates++;
            ab.romance = Math.min(100, ab.romance + 20);
            ba.romance = Math.min(100, ba.romance + 20);
            ab.aff = Math.min(100, ab.aff + 5);
            ba.aff = Math.min(100, ba.aff + 5);
            ab.chats++; ba.chats++;
            ab.lastChatDay = day; ba.lastChatDay = day;
            pa.fun = Math.min(100, pa.fun + 15); pb.fun = Math.min(100, pb.fun + 15);
            data.plans.removeIf(p -> p.what.equals("date") && p.who.contains(pa.id) && p.who.contains(pb.id));
            if (couple) {
                pa.partner = pb.id;
                pb.partner = pa.id;
                pa.partnerSince = day;
                pb.partnerSince = day;
                a.refreshLooks(pa);
                b.refreshLooks(pb);
                data.event(day, "social", pa.name + " and " + pb.name + " are officially a couple", a.blockPosition(), pa.id, pb.id);
            } else if (!partners && ab.dates == 1) {
                data.event(day, "social", pa.name + " and " + pb.name + " went on a date at " + where, a.blockPosition(), pa.id, pb.id);
            }
        };
        return s;
    }

    static void breakUp(CityData data, CityData.Profile pa, CityData.Profile pb, long day, Resident a) {
        pa.partner = "";
        pb.partner = "";
        pa.partnerSince = -1;
        pb.partnerSince = -1;
        CityData.Rel ab = data.rel(pa.id, pb.id), ba = data.rel(pb.id, pa.id);
        ab.romance = 0;
        ba.romance = 0;
        a.refreshLooks(pa);
        data.event(day, "social", pa.name + " and " + pb.name + " broke up", a.blockPosition(), pa.id, pb.id);
    }
}
