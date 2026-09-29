package com.fireheart.city;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.ItemFishedEvent;

/** Player activities: resident profiles, tipping, emotes, reputation, treasure hunts, fishing and birthdays. */
public final class Quests {
    private Quests() {}

    static CityData.Profile byName(CityData d, String name) {
        for (CityData.Profile q : d.profiles.values()) if (q.name.equalsIgnoreCase(name) || q.id.equalsIgnoreCase(name)) return q;
        return null;
    }

    /* ------------------------------------------------------------ Profile */

    public static String profile(ServerPlayer pl, CityData d, String name) {
        CityData.Profile p = byName(d, name);
        if (p == null) return "§cNobody called " + name + " lives in Solaris.";
        String pn = pl.getName().getString();
        CityData.Rel pr = d.playerRel(p.id, pn);
        if (!pr.met) return "§7You haven't met " + p.name + " yet. Go say hi!";
        CityData.Profile partner = p.partner.isEmpty() ? null : d.profiles.get(p.partner);
        List<String> fr = new ArrayList<>();
        for (String f : d.friendsOf(p.id)) if (d.profiles.get(f) != null) fr.add(d.profiles.get(f).name);
        Place home = p.homePlace();
        StringBuilder sb = new StringBuilder("§6§l" + p.name + " §r§7· " + p.jobTitle() + " at " + p.job.work().label);
        sb.append("\n§7Personality: §f").append(p.trait.adjective()).append(" §7· Mood: §f").append(p.mood()).append("% §7· Hunger: §f").append(p.hunger).append("%");
        sb.append("\n§7Loves: §f").append(Economy.label(Memory.favourite(p))).append(" §7and §f").append(Memory.hobby(p));
        if (pr.knowsHome || pr.aff >= 30) sb.append("\n§7Lives at: §f").append(home == null ? "nowhere yet" : home.label);
        if (partner != null) sb.append("\n§c❤ §7Partner: §f").append(partner.name);
        if (!fr.isEmpty()) sb.append("\n§7Friends: §f").append(String.join(", ", fr.subList(0, Math.min(6, fr.size())))).append(fr.size() > 6 ? " +" + (fr.size() - 6) : "");
        if (!p.goal.isEmpty()) sb.append("\n§7Saving for: §f").append(p.goal);
        sb.append("\n§7Right now: §f").append(p.doing == null || p.doing.isEmpty() ? "out and about" : p.doing);
        sb.append("\n§7Your friendship: §c").append("❤".repeat(Math.max(0, Math.min(5, (pr.aff + 10) / 20)))).append(" §8(").append(pr.aff).append(")");
        return sb.toString();
    }

    /* ------------------------------------------------------------ Tipping */

    public static String tip(ServerPlayer pl, CityData d, String name, int amount) {
        CityData.Profile p = byName(d, name);
        if (p == null) return "§cNobody called " + name + " lives in Solaris.";
        Resident r = Perks.find(pl.serverLevel(), d, p.name);
        if (r == null || r.distanceTo(pl) > 8) return "§7You need to be standing near " + p.name + " to tip them.";
        if (!Bank.takeCash(pl, amount)) return "§cYou don't have " + amount + " coins in gold on you.";
        ServerLevel sl = pl.serverLevel();
        p.coins += amount;
        String pn = pl.getName().getString();
        CityData.Rel pr = d.playerRel(p.id, pn);
        pr.met = true;
        long today = Calendar.worldDay(sl);
        String tk = "tips:" + p.id;
        String[] tv = d.setting(pn, tk, "-1:0").split(":");
        int tipsToday = tv.length == 2 && Perks.parse(tv[0]) == today ? (int) Perks.parse(tv[1]) : 0;
        d.setSetting(pn, tk, today + ":" + (tipsToday + 1));
        pr.aff = Math.min(100, pr.aff + Math.max(0, Math.min(10, 1 + amount / 5) / (1 + tipsToday)));
        Mind.playerEvent(d, p, pn, r.day(), "{P} tipped me " + amount + " coins", amount >= 20 ? 3 : 2, amount >= 20 ? 6 : 3);
        r.getLookControl().setLookAt(pl, 30, 30);
        r.showItem("minecraft:gold_nugget", 40);
        r.gesture(amount >= 20 ? Resident.G_CHEER : Resident.G_THUMBS, 50);
        r.particles(ParticleTypes.HAPPY_VILLAGER, 5);
        r.sayTo(amount >= 20 ? r.pick("Whoa, " + amount + " coins?! You're too generous, " + pn + "!", "I don't know what to say - thank you so much!") : r.pick("Aw, thanks " + pn + "!", "A tip! That's really kind.", "You didn't have to do that. But thanks!"), 80);
        sl.playSound(null, r.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.5f, 1.4f);
        bump(d, pn, "tip");
        if (amount >= 10) Letters.thankYou(sl, d, pn, p, "the generous tip");
        d.setDirty();
        return "§6You tipped " + p.name + " " + amount + " coins.";
    }

    /* ------------------------------------------------------------ Emotes */

    public static String emote(ServerPlayer pl, CityData d, String what) {
        ServerLevel sl = pl.serverLevel();
        String pn = pl.getName().getString();
        String e = what.toLowerCase(Locale.ROOT);
        int g;
        String verb;
        switch (e) {
            case "wave" -> { g = Resident.G_WAVE; verb = "waves"; }
            case "cheer" -> { g = Resident.G_CHEER; verb = "cheers"; }
            case "dance" -> { g = Resident.G_DANCE; verb = "starts dancing"; }
            case "bow" -> { g = Resident.G_BOW; verb = "takes a bow"; }
            case "clap" -> { g = Resident.G_CLAP; verb = "claps"; }
            case "laugh" -> { g = Resident.G_LAUGH; verb = "laughs"; }
            default -> { return "§7Emotes: wave, cheer, dance, bow, clap, laugh"; }
        }
        pl.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        if (e.equals("dance")) sl.sendParticles(ParticleTypes.NOTE, pl.getX(), pl.getY() + 2.2, pl.getZ(), 5, 0.5, 0.2, 0.5, 1.0);
        for (ServerPlayer o : sl.players()) if (o != pl && o.distanceTo(pl) < 24) Perks.say(o, "§7* " + pn + " " + verb);
        int n = 0;
        for (Resident r : sl.getEntitiesOfClass(Resident.class, pl.getBoundingBox().inflate(10), x -> x.profile() != null && !x.isSleeping() && !x.inShuttle())) {
            if (!r.hasLineOfSight(pl) || r.getRandom().nextFloat() > 0.8f) continue;
            CityData.Rel pr = d.playerRel(r.profileId(), pn);
            r.getLookControl().setLookAt(pl, 30, 30);
            boolean fan = pr.aff >= 20;
            int back = switch (e) {
                case "dance" -> fan ? Resident.G_DANCE : Resident.G_LAUGH;
                case "bow" -> Resident.G_CLAP;
                case "laugh" -> Resident.G_LAUGH;
                case "clap" -> Resident.G_CLAP;
                default -> g;
            };
            r.gesture(back, e.equals("dance") ? 100 : 40);
            if (n++ < 2 && !r.isSpeaking()) r.say(switch (e) {
                case "wave" -> r.pick("Hi " + pn + "!", "*waves back*", "Hey there!");
                case "cheer" -> r.pick("Woo! What are we celebrating?", "Yeah!!", "Go " + pn + "!");
                case "dance" -> fan ? r.pick("Dance party!", "Ooh, I know this move!", "Let's gooo!") : r.pick("Haha, what is that move?", "Nice... moves?");
                case "bow" -> r.pick("Bravo!", "*applauds*", "Encore!");
                case "clap" -> r.pick("What are we clapping for? *claps anyway*", "Yay!");
                default -> r.pick("Haha, what's so funny?", "Your laugh is contagious!");
            }, 50);
        }
        if (n >= 3) Perks.unlock(pl, d, "crowd");
        if (n > 0) bump(d, pn, "emote");
        return "";
    }

    /* ------------------------------------------------------------ Reputation */

    static final String[] RANKS = {"Stranger", "Newcomer", "Familiar Face", "Local", "Well Liked", "Beloved", "Solaris Legend"};

    public static int repScore(CityData d, String pn) {
        int total = 0, n = 0;
        for (CityData.Profile p : d.profiles.values()) {
            if (!d.playerNames(p.id).contains(pn)) continue;
            CityData.Rel r = d.playerRel(p.id, pn);
            if (!r.met) continue;
            total += Math.max(-50, r.aff) + r.fam / 4;
            n++;
        }
        return n == 0 ? 0 : total * Math.min(n, 20) / Math.max(1, d.profiles.size());
    }

    public static String rank(int score) {
        int[] at = {0, 5, 15, 30, 45, 60, 80};
        int i = 0;
        for (int k = 0; k < at.length; k++) if (score >= at[k]) i = k;
        return RANKS[i];
    }

    public static String rep(ServerPlayer pl, CityData d) {
        String pn = pl.getName().getString();
        int s = repScore(d, pn);
        String rk = rank(s);
        int[] at = {0, 5, 15, 30, 45, 60, 80};
        String next = "";
        for (int k = 0; k < at.length; k++) if (at[k] > s) { next = " §7· next: §f" + RANKS[k] + " §7at " + at[k]; break; }
        if (rk.equals("Solaris Legend")) Perks.unlock(pl, d, "legend");
        return "§6Your reputation in Solaris: §e§l" + rk + " §r§7(" + s + ")" + next;
    }

    /* ------------------------------------------------------------ Treasure hunt */

    record Clue(String place, String riddle) {}

    static final Clue[] CLUES = {
            new Clue("clock", "I have a face but no mouth, hands but no fingers. Find me in the park."),
            new Clue("pier", "Old wooden legs that stand in the sea, where the fireworks bloom - come look under me."),
            new Clue("library", "I hold a thousand stories but never tell one aloud."),
            new Clue("bakery", "Follow your nose to where the ovens never sleep."),
            new Clue("bank", "Coins go in, coins come out, and Hugo watches it all."),
            new Clue("marina", "Where the boats rest and the gulls argue."),
            new Clue("isle_plaza", "Above the clouds, in the square that glows."),
            new Clue("arcade", "High scores and flashing lights, up where the city floats."),
            new Clue("diner", "Burgers, shakes, and a diesel name."),
            new Clue("statue", "The Founder stands tall and watches over all."),
            new Clue("observatory", "Where the island looks at the stars."),
            new Clue("skyport", "Where the sky ships come and go."),
    };

    static Clue todays(long day, String pn) {
        Clue c = CLUES[Math.floorMod((int) (day * 31 + pn.hashCode()), CLUES.length)];
        return Place.get(c.place()) != null ? c : CLUES[0];
    }

    public static String hint(ServerPlayer pl, CityData d) {
        String pn = pl.getName().getString();
        long day = Calendar.worldDay(pl.serverLevel());
        if (d.setting(pn, "treasure", "").equals(String.valueOf(day))) return "§6[Treasure] §7You already found today's treasure.";
        Place p = Place.get(todays(day, pn).place());
        if (p == null) return "§6[Treasure] §7The map is smudged today...";
        int dist = (int) Math.sqrt(pl.blockPosition().distSqr(p.pos));
        int last = (int) Perks.parse(d.setting(pn, "treasureLast", "-1"));
        d.setSetting(pn, "treasureLast", String.valueOf(dist));
        String feel = last < 0 ? "" : dist < last - 3 ? "§aWarmer! " : dist > last + 3 ? "§bColder... " : "§7About the same. ";
        String how = dist < 15 ? "It's very close!" : dist < 40 ? "You're close." : dist < 100 ? "It's a fair walk away." : (p.island != pl.getY() > 150 ? "It's not even on this level of the city." : "It's far from here.");
        return "§6[Treasure] " + feel + "§f" + how;
    }

    public static String treasure(ServerPlayer pl, CityData d) {
        String pn = pl.getName().getString();
        long day = Calendar.worldDay(pl.serverLevel());
        if (d.setting(pn, "treasure", "").equals(String.valueOf(day))) return "§6[Treasure] §7You already found today's treasure. A new clue appears tomorrow!";
        Clue c = todays(day, pn);
        return "§6[Treasure] §fToday's riddle: §e\"" + c.riddle() + "\" §7Stand at the spot to claim the prize.";
    }

    static void treasureCheck(ServerLevel sl, CityData d) {
        long day = Calendar.worldDay(sl);
        for (ServerPlayer pl : sl.players()) {
            String pn = pl.getName().getString();
            if (d.setting(pn, "treasure", "").equals(String.valueOf(day))) continue;
            Clue c = todays(day, pn);
            Place p = Place.get(c.place());
            if (p == null || !pl.blockPosition().closerThan(p.pos, 6)) continue;
            d.setSetting(pn, "treasure", String.valueOf(day));
            int coins = 15 + sl.getRandom().nextInt(16);
            Perks.reward(pl, d, coins, "Treasure hunt prize");
            ItemStack bonus = new ItemStack(sl.getRandom().nextInt(4) == 0 ? Items.EMERALD : Items.COOKIE, sl.getRandom().nextInt(3) + 1);
            if (!pl.getInventory().add(bonus)) pl.drop(bonus, false);
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pl.getX(), pl.getY() + 1, pl.getZ(), 40, 0.6, 0.8, 0.6, 0.3);
            sl.playSound(null, pl.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
            Calendar.banner(pl, "§6✦ Treasure found!", "§e+" + coins + " coins §7at " + p.label);
            bump(d, pn, "treasure");
            int found = (int) Perks.parse(d.setting(pn, "treasures", "0")) + 1;
            d.setSetting(pn, "treasures", String.valueOf(found));
            if (found >= 5) Perks.unlock(pl, d, "treasure5");
        }
    }

    /* ------------------------------------------------------------ Fishing */

    public static void onFished(ItemFishedEvent e) {
        try {
            if (!(e.getEntity() instanceof ServerPlayer pl)) return;
            ServerLevel sl = pl.serverLevel();
            CityData d = CityData.get(sl);
            String pn = pl.getName().getString();
            int n = (int) Perks.parse(d.setting(pn, "fish", "0")) + 1;
            d.setSetting(pn, "fish", String.valueOf(n));
            bump(d, pn, "fish");
            Happenings.playerFish(sl, d, pn);
            if (n >= 10) Perks.unlock(pl, d, "angler");
            boolean big = false;
            for (ItemStack st : e.getDrops()) if (st.is(Items.SALMON) || st.is(Items.PUFFERFISH) || st.is(Items.TROPICAL_FISH) || !st.isEdible()) big = true;
            int k = 0;
            for (Resident r : sl.getEntitiesOfClass(Resident.class, pl.getBoundingBox().inflate(12), x -> x.profile() != null && !x.isSleeping() && x.isFree())) {
                if (k++ >= 2 || !r.hasLineOfSight(pl) || r.getRandom().nextFloat() > 0.6f) continue;
                r.getLookControl().setLookAt(pl, 30, 30);
                r.gesture(Resident.G_CHEER, 40);
                r.say(big ? r.pick("Whoa, what did you pull up?!", "Now THAT's a catch, " + pn + "!") : n % 10 == 0 ? "That's " + n + " fish, " + pn + "! You're a natural." : r.pick("Nice catch!", "Fish on!", "Ooh, dinner!"), 50);
            }
        } catch (Throwable t) {
            FireheartCity.LOG.error("Fishing reaction failed", t);
        }
    }

    /* ------------------------------------------------------------ Birthdays */

    public static String setBirthday(ServerPlayer pl, CityData d, int dom) {
        d.setSetting(pl.getName().getString(), "birthday", String.valueOf(dom));
        return "§6Your birthday is now day §e" + dom + "§6 of every 28-day month in Solaris. §7(Today is day " + (Math.floorMod(Calendar.worldDay(pl.serverLevel()), 28L) + 1) + ".)";
    }

    public static boolean birthday(CityData d, String pn, long day) {
        String b = d.setting(pn, "birthday", "");
        return !b.isEmpty() && Perks.parse(b) == Math.floorMod(day, 28L) + 1;
    }

    static void birthdayCheck(ServerLevel sl, CityData d) {
        long day = Calendar.worldDay(sl);
        for (ServerPlayer pl : sl.players()) {
            String pn = pl.getName().getString();
            if (!birthday(d, pn, day) || d.setting(pn, "bday", "").equals(String.valueOf(day))) continue;
            d.setSetting(pn, "bday", String.valueOf(day));
            Calendar.banner(pl, "§d🎂 Happy Birthday, " + pn + "!", "§eFrom everyone in Solaris");
            ItemStack cake = new ItemStack(Items.CAKE);
            if (!pl.getInventory().add(cake)) pl.drop(cake, false);
            Perks.reward(pl, d, 25, "Birthday present from the City of Solaris");
            Perks.unlock(pl, d, "birthday");
            Fireworks.burst(sl, pl.blockPosition().above(3), 6, 40, 4);
            d.news(day, "It's " + pn + "'s birthday today!");
            for (ServerPlayer o : sl.players()) if (o != pl) Perks.say(o, "§d🎂 It's " + pn + "'s birthday today! §7Say happy birthday!");
        }
    }

    /** A resident's birthday greeting for a player they know, or null. */
    public static String birthdayLine(Resident r, String pn) {
        CityData d = r.data();
        long day = r.day();
        if (!birthday(d, pn, day)) return null;
        String k = "bdg:" + r.profileId();
        if (d.setting(pn, k, "").equals(String.valueOf(day))) return null;
        d.setSetting(pn, k, String.valueOf(day));
        r.gesture(Resident.G_CHEER, 50);
        r.particles(ParticleTypes.HEART, 4);
        return r.pick("HAPPY BIRTHDAY, " + pn + "! 🎂", "It's your birthday?! Happy birthday, " + pn + "!", "Happy birthday! Any big plans?");
    }

    /* ------------------------------------------------------------ Daily quests */

    record Task(String key, String text, int need) {}

    static final Task[] TASKS = {
            new Task("talk", "Chat with 3 residents", 3), new Task("courier", "Deliver a courier parcel", 1), new Task("treasure", "Find today's treasure", 1),
            new Task("fish", "Catch 3 fish", 3), new Task("tip", "Tip a resident", 1), new Task("hug", "Hug 2 residents", 2),
            new Task("letter", "Send a letter", 1), new Task("emote", "Emote near residents twice", 2), new Task("gift", "Give a resident a gift", 1),
            new Task("game", "Play hide and seek, race, or get guided somewhere", 1), new Task("rps", "Win rock-paper-scissors", 1), new Task("selfie", "Take a selfie with a resident", 1)
    };

    static Task[] today(String pn, long day) {
        java.util.Random r = new java.util.Random(pn.hashCode() * 6151L + day * 409);
        java.util.List<Task> pool = new java.util.ArrayList<>(java.util.List.of(TASKS));
        java.util.Collections.shuffle(pool, r);
        return new Task[]{pool.get(0), pool.get(1), pool.get(2)};
    }

    /** Counts one step of a daily-quest activity for the player. */
    static long currentDay = -1;

    public static void bump(CityData d, String pn, String key) {
        if (pn == null || pn.isEmpty() || currentDay < 0) return;
        String[] v = d.setting(pn, "qd:" + key, "-1:0").split(":");
        int n = v.length == 2 && Perks.parse(v[0]) == currentDay ? (int) Perks.parse(v[1]) : 0;
        d.setSetting(pn, "qd:" + key, currentDay + ":" + (n + 1));
    }

    static int count(CityData d, String pn, String key, long day) {
        String[] v = d.setting(pn, "qd:" + key, "-1:0").split(":");
        return v.length == 2 && Perks.parse(v[0]) == day ? (int) Perks.parse(v[1]) : 0;
    }

    public static String quests(ServerPlayer pl, CityData d) {
        String pn = pl.getName().getString();
        long day = Calendar.worldDay(pl.serverLevel());
        StringBuilder sb = new StringBuilder("§6§lToday's quests §7(" + Calendar.stamp(day) + ")");
        String done = d.setting(pn, "qdone:" + day, "");
        for (Task t : today(pn, day)) {
            int n = Math.min(t.need(), count(d, pn, t.key(), day));
            boolean ok = done.contains("|" + t.key() + "|");
            sb.append("\n").append(ok ? "§a✔ " : "§7☐ ").append("§f").append(t.text()).append(" §8(").append(n).append("/").append(t.need()).append(") §e15 coins");
        }
        return sb.append("\n§7Finish all three for a §e20§7 coin bonus.").toString();
    }

    static void questCheck(ServerLevel sl, CityData d) {
        long day = Calendar.worldDay(sl);
        for (ServerPlayer pl : sl.players()) {
            String pn = pl.getName().getString();
            String done = d.setting(pn, "qdone:" + day, "");
            int finished = 0;
            for (Task t : today(pn, day)) {
                boolean ok = done.contains("|" + t.key() + "|");
                if (!ok && count(d, pn, t.key(), day) >= t.need()) {
                    done = done + "|" + t.key() + "|";
                    d.setSetting(pn, "qdone:" + day, done);
                    Perks.reward(pl, d, 15, "Daily quest: " + t.text());
                    Perks.say(pl, "§6[Quest] §a✔ " + t.text() + " §e+15 coins");
                    pl.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.6f);
                    ok = true;
                }
                if (ok) finished++;
            }
            if (finished == 3 && !done.contains("|all|")) {
                d.setSetting(pn, "qdone:" + day, done + "|all|");
                Perks.reward(pl, d, 20, "All daily quests done");
                int total = (int) Perks.parse(d.setting(pn, "questsDone", "0")) + 1;
                d.setSetting(pn, "questsDone", String.valueOf(total));
                Calendar.banner(pl, "§6All quests done!", "§e+20 bonus coins §7· " + total + " days completed");
                if (total >= 10) Perks.unlock(pl, d, "quests10");
            }
        }
    }

    /* ------------------------------------------------------------ Wishlists */

    public static String wishlist(ServerPlayer pl, CityData d, String name) {
        CityData.Profile p = byName(d, name);
        if (p == null) return "§cNobody called " + name + " lives in Solaris.";
        String pn = pl.getName().getString();
        if (!d.playerRel(p.id, pn).met) return "§7You'd need to meet " + p.name + " first.";
        String trait = switch (p.trait) {
            case CHEERFUL -> "cake";
            case SHY, CURIOUS -> "a good book";
            case ADVENTUROUS -> "an emerald";
            case FRIENDLY, DREAMY -> "fresh flowers";
            case TALKATIVE -> "cookies";
            case LAIDBACK -> "sweet berries";
            default -> "a diamond (to cheer them up)";
        };
        StringBuilder sb = new StringBuilder("§6§l" + p.name + "'s wishlist");
        sb.append("\n§7♥ Favourite food: §f").append(Economy.label(Memory.favourite(p))).append(" §8(counts extra)");
        sb.append("\n§7♥ Would love: §f").append(trait);
        if (!p.wantDevice.isEmpty()) sb.append("\n§7♥ Saving up for: §f").append(TechStore.deviceName(p.wantDevice)).append(" §8(from SolTech)");
        else if (!p.goal.isEmpty()) sb.append("\n§7♥ Dreaming of: §f").append(p.goal);
        if (p.hunger < 40) sb.append("\n§7♥ Right now: §fanything to eat - they're hungry!");
        if (Letters.lucky(d, pn, Calendar.worldDay(pl.serverLevel()), p.id)) sb.append("\n§d✦ Your horoscope says gifts to " + p.name + " count double today!");
        return sb.toString();
    }

    /* ------------------------------------------------------------ Donations */

    public static String donate(ServerPlayer pl, CityData d, int amount) {
        if (!Bank.takeCash(pl, amount)) return "§cYou don't have " + amount + " coins in gold on you.";
        ServerLevel sl = pl.serverLevel();
        String pn = pl.getName().getString();
        long before = Perks.parse(d.setting(Finale.CITY, "spirit", "0"));
        long after = before + amount;
        d.setSetting(Finale.CITY, "spirit", String.valueOf(after));
        int mine = (int) Perks.parse(d.setting(pn, "donated", "0")) + amount;
        d.setSetting(pn, "donated", String.valueOf(mine));
        if (mine >= 100) Perks.unlock(pl, d, "donor");
        for (Resident r : sl.getEntitiesOfClass(Resident.class, pl.getBoundingBox().inflate(10), x -> x.profile() != null && x.isFree())) {
            r.gesture(Resident.G_CLAP, 40);
            r.say(r.pick("That's so generous!", "Thank you for helping the city, " + pn + "!", "Solaris thanks you!"), 50);
            break;
        }
        if (before / 250 != after / 250) {
            long day = Calendar.worldDay(sl);
            d.news(day, "Thanks to generous donors, the Solaris City Fund reached " + (after / 250 * 250) + " coins! Celebration at the plaza!");
            for (CityData.Profile p : d.profiles.values()) p.fun = Math.min(100, p.fun + 10);
            Fireworks.burst(sl, Finale.ALTAR.above(6), 12, 100, 7);
            Applause.confetti(sl, Finale.ALTAR.above(2), 30);
            for (ServerPlayer o : sl.players()) Calendar.banner(o, "§6✦ City Fund milestone!", "§e" + (after / 250 * 250) + " coins raised · thank you, " + pn);
        }
        d.setDirty();
        return "§6You donated §e" + amount + "§6 coins to the Solaris City Fund. §7(City spirit: " + after + " · you've given " + mine + ")";
    }

    public static void tick(ServerLevel sl, CityData d) {
        long gt = sl.getGameTime();
        currentDay = Calendar.worldDay(sl);
        if (gt % 20 == 15) treasureCheck(sl, d);
        if (gt % 100 == 67) birthdayCheck(sl, d);
        if (gt % 40 == 27) questCheck(sl, d);
    }
}
