package com.fireheart.city;

import java.util.Locale;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Resident pastimes: chat games, hugs and high fives, stories and trivia, dreams, jogging and small greeting touches. */
public final class Pastimes {
    private Pastimes() {}

    static boolean any(String t, String... keys) {
        return Intents.any(t, keys);
    }

    /* ------------------------------------------------------------ Chat */

    static final String[] RPS = {"rock", "paper", "scissors"};

    static final String[] STORIES = {
            "When I first moved here, Solaris was three houses and a very confused chicken. The chicken is still here. I think it's the mayor's advisor now.",
            "Once, the Sky Ferry got stuck above the marina for an hour. We had a sing-along. Worst hour of my life. Best hour of my life.",
            "They say if you stand on the old pier at midnight you can hear the organ from Neon Heights. I tried. I heard a seagull.",
            "Gus once rebuilt an entire wall while everyone was at lunch. Nobody even noticed it had fallen down.",
            "Legend says the Founder built the statue in one night. I say the Founder had a lot of help and very good coffee.",
            "My first day in town I got lost between the bakery and the bank. They're next to each other. I'm not proud of it.",
            "A creeper wandered into the plaza once during the fireworks. Bruno punted it into the sea. Everyone clapped. The creeper did not.",
    };

    static final String[] TRIVIA = {
            "Fun fact: the Sky Ferry flies higher than any bird in Solaris. Except the parrots. They cheat.",
            "Did you know Ember Heights has an elevator that remembers everyone's floor? Creepy. Convenient, but creepy.",
            "Fun fact: the Firework Machine can write SOLARIS across the whole sky. It takes forty rockets per letter.",
            "Did you know the Hall of Lights plays a different note for every step you take?",
            "Fun fact: Neon Heights gets colder at night because it's so high up. Bring a jacket!",
            "Did you know the Sky Launch goes over two hundred blocks up? My stomach still hasn't landed.",
            "Fun fact: every Wednesday and Saturday there's a firework show out at the bay.",
            "Did you know the library has more books than there are people in Solaris? By a lot.",
    };

    static final String[] COMPLIMENTS = {
            "You've got great taste. In everything. Mostly.", "Honestly? This city's better with you in it.", "You always know how to make me smile.",
            "You're like a sunrise over the marina - rare and kind of amazing.", "If kindness were coins, you'd be the richest person in Solaris.",
            "I like your style. Very you.", "Talking to you is the highlight of my day."
    };

    static final String[] ROASTS = {
            "Your building skills are... brave. Very brave.", "I've seen creepers with better fashion sense. Kidding! ...Mostly.",
            "You run like a chicken that just saw a fox.", "You're the reason the fire department has a drill every week."
    };

    /** Replies to chat games and small requests, or null. {@code t} is the normalised text padded with spaces. */
    public static String chat(ServerPlayer pl, Resident r, String t, Chat.Ctx c) {
        RandomSource rnd = r.getRandom();
        CityData d = r.data();
        CityData.Profile p = r.profile();
        String pn = pl.getName().getString();
        boolean askRps = any(t, " rock paper scissors ", " rps ", " roshambo ");
        if (c.topic.equals("rps") || askRps) {
            String mine = null;
            int picks = 0;
            if (!askRps) for (String s : RPS) if (t.contains(" " + s + " ")) { mine = s; picks++; }
            if (picks > 1) mine = null;
            if (mine == null) {
                if (c.topic.equals("rps") && !askRps) {
                    c.topic = "";
                    return null;
                }
                c.topic = "rps";
                r.gesture(Resident.G_THINK, 40);
                return rnd.nextBoolean() ? "Ooh, you're on! Rock, paper, scissors... say your pick!" : "Best game ever invented. Ready? Tell me: rock, paper or scissors?";
            }
            c.topic = "";
            String theirs = RPS[rnd.nextInt(3)];
            int a = idx(mine), b = idx(theirs);
            r.showItem(theirs.equals("rock") ? "minecraft:cobblestone" : theirs.equals("paper") ? "minecraft:paper" : "minecraft:shears", 50);
            if (a == b) {
                r.gesture(Resident.G_SHRUG, 40);
                return "I picked " + theirs + " too! Great minds. Again?";
            }
            if ((a + 1) % 3 == b) {
                r.gesture(Resident.G_CHEER, 50);
                r.particles(ParticleTypes.HAPPY_VILLAGER, 4);
                return theirs.substring(0, 1).toUpperCase(Locale.ROOT) + theirs.substring(1) + "! I WIN! " + Lines.pick(rnd, "Undefeated champion of Solaris!", "Rematch? I'll go easy on you. I won't.", "Better luck next time, " + pn + "!");
            }
            r.gesture(Resident.G_FACEPALM, 50);
            Perks.unlock(pl, d, "rps");
            return "I picked " + theirs + "... you win! " + Lines.pick(rnd, "Beginner's luck!", "Best of three?", "Ugh, I always pick " + theirs + ".");
        }
        if (any(t, " flip a coin ", " coin flip ", " heads or tails ", " toss a coin ")) {
            r.showItem("minecraft:gold_nugget", 40);
            r.gesture(Resident.G_THINK, 30);
            r.level().playSound(null, r.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.NEUTRAL, 0.6f, 1.8f);
            return "*flips a coin* ... " + (rnd.nextBoolean() ? "Heads!" : "Tails!") + " " + Lines.pick(rnd, "The coin has spoken.", "No take-backs.", "");
        }
        if (any(t, " roll a dice ", " roll a die ", " roll the dice ", " roll dice ", " dice roll ")) {
            int n = 1 + rnd.nextInt(6);
            r.gesture(Resident.G_THINK, 30);
            return "*rolls* ... a " + n + "!" + (n == 6 ? " Yes! Lucky six!" : n == 1 ? " Oof. Snake eyes, well, snake eye." : "");
        }
        if (any(t, " hug ", " hugs ", " cuddle ")) {
            int trust = p.mind.trustIn(pn);
            CityData.Rel rel = d.playerRel(p.id, pn);
            if (trust < -10 || rel.aff < 0) {
                r.gesture(Resident.G_HEADSHAKE, 40);
                return Lines.pick(rnd, "Uh... maybe a handshake. From a distance.", "I'm good, thanks.");
            }
            r.getLookControl().setLookAt(pl, 30, 30);
            r.gesture(Resident.G_HUG, 50);
            r.particles(ParticleTypes.HEART, 5);
            rel.aff = Math.min(100, rel.aff + (rel.aff < 40 ? 2 : 1));
            d.setDirty();
            return Lines.pick(rnd, "*big hug* Aww, I needed that.", "*squeezes you* You give the best hugs, " + pn + ".", "Come here! *hug*");
        }
        if (any(t, " high five ", " highfive ", " hi five ", " up top ", " fist bump ")) {
            r.getLookControl().setLookAt(pl, 30, 30);
            r.gesture(Resident.G_HIGHFIVE, 30);
            r.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            r.level().playSound(null, r.blockPosition(), SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.NEUTRAL, 0.8f, 1.5f);
            return Lines.pick(rnd, "*SLAP* Yeah!", "Up top! ...Nailed it.", "*fist bump* Boom.");
        }
        if (any(t, " dance for me ", " can you dance ", " show me your moves ", " let's dance ", " lets dance ", " dance with me ")) {
            r.gesture(Resident.G_DANCE, 100);
            if (r.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.NOTE, r.getX(), r.getY() + 2.2, r.getZ(), 6, 0.5, 0.2, 0.5, 1.0);
            return Lines.pick(rnd, "You asked for it! *busts out moves*", "Stand back, I'm a professional. *dances*", "The Sky Organ taught me this one!");
        }
        if (any(t, " tell me a story ", " story ", " tell me something interesting ")) {
            r.gesture(Resident.G_THINK, 40);
            return Lines.pick(rnd, STORIES);
        }
        if (any(t, " fun fact ", " tell me a fact ", " trivia ", " did you know ", " teach me something ")) {
            r.gesture(Resident.G_POINT, 40);
            return Lines.pick(rnd, TRIVIA);
        }
        if (any(t, " compliment me ", " say something nice ", " am i cool ", " do you like me ", " am i nice ")) {
            r.gesture(Resident.G_THUMBS, 40);
            d.playerRel(p.id, pn).fam++;
            return Lines.pick(rnd, COMPLIMENTS);
        }
        if (any(t, " roast me ", " insult me ", " be mean ")) {
            r.gesture(Resident.G_LAUGH, 40);
            return Lines.pick(rnd, ROASTS) + " Love you, though.";
        }
        if (any(t, " what season ", " which season ", " is it winter ", " is it summer ", " is it spring ", " is it autumn ", " is it fall ")) {
            String s = Skies.seasonName(Calendar.worldDay((ServerLevel) r.level()));
            return "It's " + s + "! " + switch (s) {
                case "spring" -> "Everything's blooming. I keep sneezing.";
                case "summer" -> "Beach season! See you at the Magma Beach Bar.";
                case "autumn" -> "Cosy season. Hot soup and long walks.";
                default -> "Brr. Snowflakes everywhere. I love it, secretly.";
            };
        }
        if (any(t, " sing ", " sing me ", " sing a song ", " sing something ")) {
            r.gesture(Resident.G_SING, 90);
            if (r.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.NOTE, r.getX(), r.getY() + 2.2, r.getZ(), 8, 0.5, 0.3, 0.5, 1.0);
                for (int i = 0; i < 3; i++) sl.playSound(null, r.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.NEUTRAL, 0.6f, 0.8f + i * 0.2f + rnd.nextFloat() * 0.1f);
            }
            return Lines.pick(rnd, "♪ Solaris, Solaris, city of light... ♪ Okay that's all I've got.", "♪ Take me up to Neon Heights, where the sky is always bright ♪", "♪ La la la, the ferry's late again ♪", "♪ Oh I'd walk a thousand blocks... ♪ You didn't hear that.");
        }
        if (any(t, " what should i build ", " build ideas ", " building ideas ", " idea for a build ", " what to build ")) {
            String[] ideas = {"a lighthouse on the old pier", "a treehouse in the park", "a cosy café by the marina", "a skybridge between Ember Heights and the library", "an ice cream stand for the beach", "a secret garden behind the bank", "a train station for the Steam Rails", "a bandstand for Remy in the plaza", "a floating fishing hut off the boardwalk", "a rooftop cinema on Ember Heights"};
            r.gesture(Resident.G_THINK, 40);
            return "Ooh! How about " + Lines.pick(rnd, ideas) + "? " + Lines.pick(rnd, "I'd visit every day.", "Solaris needs one.", "Just saying.");
        }
        if (any(t, " rate me ", " what do you think of me ", " do you trust me ", " are we friends ")) {
            CityData.Rel rel = d.playerRel(p.id, pn);
            int trust = p.mind.trustIn(pn);
            r.gesture(rel.aff >= 50 ? Resident.G_THUMBS : rel.aff >= 0 ? Resident.G_THINK : Resident.G_HEADSHAKE, 40);
            if (rel.aff >= 80) return "Are you kidding? You're one of my favourite people in Solaris!";
            if (rel.aff >= 50) return "We're definitely friends. I'm always happy to see you.";
            if (rel.aff >= 20) return "I like you! We should hang out more.";
            if (rel.aff >= 0 && trust >= -10) return "You seem nice. I don't know you that well yet, though.";
            return "Honestly? You've got some making up to do.";
        }
        if (any(t, " courier ", " any jobs ", " need a job ", " can i help ", " any work ", " need help ")) {
            return "Pip's always looking for couriers! Type /sol courier and you'll get a parcel to deliver around town.";
        }
        return null;
    }

    static int idx(String s) {
        return s.equals("rock") ? 0 : s.equals("paper") ? 1 : 2;
    }

    public static boolean noAi(String t) {
        return any(t, " rock ", " paper ", " scissors ", " rps ", " flip a coin ", " coin flip ", " heads or tails ", " roll a dice ", " roll a die ", " roll the dice ", " roll dice ");
    }

    /* ------------------------------------------------------------ Dreams */

    public static String dream(Resident r, CityData.Profile p) {
        RandomSource rnd = r.getRandom();
        CityData d = r.data();
        if (rnd.nextFloat() < 0.55f) return "Zzz...";
        String partner = p.partner.isEmpty() || d.profiles.get(p.partner) == null ? null : d.profiles.get(p.partner).name;
        String job = p.job.title.toLowerCase(Locale.ROOT);
        String[] opts = {
                "Zzz... *mumbles* five more minutes...",
                "Zzz... no, the " + Economy.label(Memory.favourite(p)) + " is MINE...",
                "Zzz... *mumbles* best " + job + " in Solaris... mm...",
                "Zzz... flying... over Neon Heights... wheee...",
                partner != null ? "Zzz... " + partner + "... *smiles*" : "Zzz... who's that... cute...",
                "Zzz... the ferry's leaving without me... wait...",
                "Zzz... *snore* ...*snort*... Zzz.",
                "Zzz... one more round of " + Computers.gameName(Computers.GAMES[rnd.nextInt(Computers.GAMES.length)]) + "..."
        };
        return opts[rnd.nextInt(opts.length)];
    }

    /* ------------------------------------------------------------ Jogging */

    public static void jogTick(Resident r, CityData.Profile p, Place dest) {
        if (!r.isFree() || r.isSeated()) return;
        if (r.getNavigation().isDone() || r.getRandom().nextInt(4) == 0) {
            Vec3 to = LandRandomPos.getPos(r, 12, 3);
            if (to != null && to.distanceToSqr(Vec3.atCenterOf(dest.pos)) < 20 * 20) r.getNavigation().moveTo(to.x, to.y, to.z, 1.25);
        }
        if (r.level() instanceof ServerLevel sl && r.getRandom().nextFloat() < 0.3f) sl.sendParticles(ParticleTypes.CLOUD, r.getX(), r.getY() + 0.1, r.getZ(), 1, 0.1, 0.0, 0.1, 0.0);
        if (r.getRandom().nextFloat() < 0.06f) {
            r.getNavigation().stop();
            r.gesture(Resident.G_WINDED, 60);
            r.say(r.pick("*pant* *pant* ...okay, breather.", "Hands on knees... just a sec...", "Whew! Stitch!"), 50);
            return;
        }
        r.gesture(Resident.G_JOG, 50);
        p.fun = Math.min(100, p.fun + 1);
        if (r.getRandom().nextFloat() < 0.04f) r.say(r.pick("*huff* *puff* ...one more lap!", "Feel the burn!", "Morning run! Best way to start the day.", "My legs hate me right now.", "Runner's high, baby!"), 50);
        DayLog lg = p.log(r.routineDay());
        if (lg.once("jogged")) lg.note("I went for a jog around " + dest.label);
    }

    /* ------------------------------------------------------------ Greetings */

    /** Something extra a resident adds when greeting a player they know, or null. */
    public static String greetExtra(Resident r, CityData.Profile p, Player pl, CityData.Rel pr) {
        RandomSource rnd = r.getRandom();
        CityData d = r.data();
        String pn = pl.getName().getString();
        long day = r.day();
        if (Skies.kindnessDay(day) && !d.setting(pn, "kind:" + p.id, "").equals(String.valueOf(day))) {
            d.setSetting(pn, "kind:" + p.id, String.valueOf(day));
            String gift = Skies.GIFTS[rnd.nextInt(Skies.GIFTS.length)];
            giveItem(pl, gift);
            r.showItem(gift, 50);
            r.gesture(Resident.G_GIVE, 40);
            return "Happy Kindness Day! This is for you. " + Lines.pick(rnd, COMPLIMENTS);
        }
        if (pr.aff >= 60 && rnd.nextFloat() < 0.35f && !d.setting(pn, "gift:" + p.id, "").equals(String.valueOf(day))) {
            d.setSetting(pn, "gift:" + p.id, String.valueOf(day));
            String gift = Economy.isFood(Memory.favourite(p)) && rnd.nextBoolean() ? Memory.favourite(p) : Skies.GIFTS[rnd.nextInt(Skies.GIFTS.length)];
            giveItem(pl, gift);
            r.showItem(gift, 50);
            r.gesture(Resident.G_GIVE, 40);
            r.particles(ParticleTypes.HEART, 3);
            return Lines.pick(rnd, "I saw this and thought of you - here, " + Economy.label(gift) + "!", "Got you a little something: " + Economy.label(gift) + ". Don't make it weird.", "For my favourite visitor! " + Economy.label(gift) + ".");
        }
        if (rnd.nextFloat() < 0.3f) {
            String h = heldRemark(pl.getMainHandItem(), rnd);
            if (h != null) return h;
        }
        String act = r.activityName();
        long tod = Math.floorMod(r.level().getDayTime(), 24000L);
        if (act.equals("evening") && tod > 12400 && rnd.nextFloat() < 0.4f) return Lines.pick(rnd, "I'm heading home - good night, " + pn + "!", "Long day. Sleep well, " + pn + "!", "Night! Don't stay out too late.");
        if (act.equals("morning") && rnd.nextFloat() < 0.35f) return Lines.pick(rnd, "Up early too, huh? Off to work soon.", "Morning! Coffee first, then work.", "Early bird catches the worm!");
        return null;
    }

    static void giveItem(Player pl, String id) {
        ItemStack st = Economy.stack(id);
        if (st.isEmpty()) return;
        if (!pl.getInventory().add(st)) pl.drop(st, false);
    }

    static String heldRemark(ItemStack st, RandomSource rnd) {
        if (st.isEmpty()) return null;
        if (st.is(Items.DIAMOND_SWORD) || st.is(Items.NETHERITE_SWORD)) return Lines.pick(rnd, "Whoa, careful where you point that thing!", "Planning to fight a dragon?");
        if (st.is(Items.DIAMOND) || st.is(Items.EMERALD)) return Lines.pick(rnd, "Is that a real " + (st.is(Items.DIAMOND) ? "diamond" : "emerald") + "?! Fancy!", "Ooh, shiny. Rich much?");
        if (st.is(Items.FISHING_ROD)) return "Going fishing? The boardwalk's the best spot.";
        if (st.is(Items.CAKE)) return "Is that cake?! Is it someone's birthday?";
        if (st.is(Items.BOOK) || st.is(Items.WRITABLE_BOOK)) return "A book! Nell would approve.";
        if (st.is(Items.FLINT_AND_STEEL)) return "Please don't. Hank has enough to do.";
        if (st.is(Items.TNT)) return "Is that... TNT? Gus is going to be so upset.";
        if (st.getItem() instanceof PhoneItem) return "Nice phone! What colour is that?";
        if (st.isEdible()) return "Ooh, snacks! Sharing is caring...";
        return null;
    }
}
