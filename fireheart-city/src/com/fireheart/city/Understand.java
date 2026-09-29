package com.fireheart.city;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/**
 * The residents' general understanding, used when no specific topic matched: works out what kind of sentence the
 * player said (a question, an opinion, news about themselves...), what it is about, and answers from what the
 * resident actually knows - echoing the player's own words so the reply fits.
 */
public final class Understand {
    private Understand() {}

    static final String[][] TOPICS = {
            {"food", "food", "eat", "eating", "hungry", "snack", "lunch", "dinner", "breakfast", "meal", "pizza", "burger", "noodles", "ramen", "bread", "cake", "cookie", "cookies", "steak", "soup", "salad", "fries", "pasta", "sushi", "chocolate", "ice cream", "candy", "fruit", "apple", "carrot", "potato", "cook", "cooking", "bake", "baking", "recipe"},
            {"drink", "drink", "coffee", "tea", "juice", "water", "milk", "soda", "thirsty", "smoothie", "hot chocolate"},
            {"weather", "weather", "rain", "raining", "sunny", "snow", "snowing", "storm", "thunder", "cloudy", "cold", "hot", "warm", "windy", "sky"},
            {"money", "money", "coins", "coin", "rich", "poor", "broke", "bank", "savings", "loan", "pay", "paid", "salary", "wage", "expensive", "cheap", "price", "cost", "buy", "bought", "shopping", "shop", "sell"},
            {"work", "work", "job", "boss", "shift", "career", "office", "working", "coworker", "colleague"},
            {"home", "house", "home", "apartment", "flat", "room", "bed", "bedroom", "kitchen", "garden", "neighbour", "neighbor", "furniture"},
            {"family", "family", "mum", "mom", "dad", "mother", "father", "brother", "sister", "parents", "kids", "children", "baby", "grandma", "grandpa", "cousin", "uncle", "aunt"},
            {"love", "love", "crush", "date", "dating", "girlfriend", "boyfriend", "romance", "romantic", "kiss", "married", "marry", "wedding", "partner", "relationship", "heart"},
            {"friends", "friend", "friends", "bestie", "buddy", "mate", "pal", "hang out", "party", "parties", "lonely"},
            {"pets", "pet", "pets", "dog", "dogs", "cat", "cats", "parrot", "bird", "birds", "animal", "animals", "horse", "fish", "wolf", "fox", "puppy", "kitten", "bunny", "rabbit", "axolotl"},
            {"music", "music", "song", "songs", "sing", "singing", "band", "concert", "guitar", "piano", "organ", "dance", "dancing", "rap", "album", "headphones", "radio"},
            {"games", "game", "games", "gaming", "play", "playing", "arcade", "minecraft", "console", "solbox", "snake", "2048", "score", "high score", "video game", "video games"},
            {"sport", "sport", "sports", "football", "soccer", "basketball", "run", "running", "swim", "swimming", "gym", "exercise", "workout", "fitness", "race", "racing", "parkour"},
            {"screens", "tv", "movie", "movies", "film", "show", "shows", "soltube", "video", "videos", "cinema", "youtube", "stream", "streaming", "series", "anime", "cartoon"},
            {"tech", "phone", "phones", "solphone", "computer", "pc", "tablet", "internet", "app", "apps", "solfeed", "tech", "robot", "robots", "ai", "wifi", "laptop", "texting", "text"},
            {"books", "book", "books", "read", "reading", "library", "story", "stories", "novel", "poem", "write", "writing", "school", "study", "studying", "learn", "learning", "homework", "exam", "teacher", "class"},
            {"machines", "machine", "machines", "engine", "engines", "factory", "create", "gear", "gears", "cog", "belt", "train", "trains", "railway", "car", "cars", "truck", "diesel", "fuel", "build", "building", "redstone", "mechanic"},
            {"sea", "sea", "ocean", "beach", "boat", "boats", "fishing", "pier", "marina", "island", "waves", "swim", "harbour", "harbor", "ship", "ships"},
            {"space", "space", "stars", "star", "moon", "sun", "planet", "planets", "galaxy", "universe", "observatory", "telescope", "alien", "aliens", "astronaut", "night sky", "rocket"},
            {"sleep", "sleep", "sleeping", "tired", "nap", "dream", "dreams", "nightmare", "bedtime", "awake", "insomnia"},
            {"health", "sick", "ill", "hurt", "pain", "doctor", "hospital", "medicine", "headache", "injured", "cough", "cold", "healthy", "hospital"},
            {"fashion", "clothes", "outfit", "shirt", "shoes", "hat", "dress", "hoodie", "jacket", "style", "fashion", "wear", "wearing", "skin", "hair", "haircut"},
            {"art", "art", "draw", "drawing", "paint", "painting", "photo", "photos", "picture", "pictures", "camera", "design", "build", "statue", "sculpture"},
            {"travel", "travel", "trip", "holiday", "vacation", "adventure", "explore", "exploring", "journey", "visit", "ferry", "fly", "flying", "skyliner", "airport"},
            {"danger", "zombie", "zombies", "creeper", "creepers", "skeleton", "monster", "monsters", "mob", "mobs", "fight", "sword", "war", "scary", "scared", "afraid", "ghost", "haunted", "police", "fire", "crime", "thief"},
            {"nature", "tree", "trees", "flower", "flowers", "forest", "mountain", "mountains", "river", "lake", "plants", "grass", "nature", "park", "leaves", "cherry blossom"},
            {"future", "future", "tomorrow", "next week", "someday", "plans", "plan", "goal", "goals", "dream job", "grow up", "retire"},
            {"past", "yesterday", "last week", "before", "used to", "remember", "history", "old days", "childhood", "when you were young"},
            {"feelings", "feel", "feeling", "feelings", "happy", "sad", "angry", "mad", "upset", "excited", "nervous", "anxious", "stressed", "bored", "lonely", "jealous", "proud", "embarrassed", "grateful"},
            {"city", "city", "solaris", "town", "downtown", "neighbourhood", "neighborhood", "street", "road", "roads", "buildings", "skyline", "founder", "mayor"},
    };

    static boolean has(String t, String k) {
        return t.contains(" " + k + " ");
    }

    static String topic(String t) {
        String best = null;
        int bestLen = 0;
        for (String[] row : TOPICS) for (int i = 1; i < row.length; i++) if (row[i].length() > bestLen && has(t, row[i])) { best = row[0]; bestLen = row[i].length(); }
        return best;
    }

    static String keyword(String t) {
        String best = null;
        int bestLen = 0;
        for (String[] row : TOPICS) for (int i = 1; i < row.length; i++) if (row[i].length() > bestLen && has(t, row[i])) { best = row[i]; bestLen = row[i].length(); }
        return best;
    }

    static String pick(RandomSource r, String... o) {
        return Lines.pick(r, o);
    }

    static String cap(String s) {
        return s == null || s.isEmpty() ? "" : s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    /** Swaps "my/me/i/you/your" so the resident can say the player's words back. */
    static String flip(String s) {
        String[] w = s.trim().split(" ");
        StringBuilder b = new StringBuilder();
        for (String x : w) {
            String y = switch (x) {
                case "my" -> "your";
                case "your" -> "my";
                case "me" -> "you";
                case "i" -> "you";
                case "you" -> "me";
                case "myself" -> "yourself";
                case "yourself" -> "myself";
                case "mine" -> "yours";
                case "yours" -> "mine";
                case "am" -> "are";
                case "i'm", "im" -> "you're";
                case "i've", "ive" -> "you've";
                case "i'll", "ill" -> "you'll";
                case "i'd" -> "you'd";
                case "we" -> "you";
                case "our" -> "your";
                case "was" -> "were";
                default -> x;
            };
            if (!y.isEmpty()) b.append(y).append(' ');
        }
        return b.toString().trim();
    }

    static String clip(String s, int words) {
        String[] w = s.trim().split(" ");
        if (w.length <= words) return s.trim();
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < words; i++) b.append(w[i]).append(' ');
        return b.toString().trim();
    }

    static int seed(CityData.Profile p, String thing) {
        return Math.floorMod((p.id + "|" + thing).hashCode(), 100);
    }

    /** How much the resident likes something: stable per resident and thing, nudged by personality. */
    static int opinion(CityData.Profile p, String thing) {
        int v = seed(p, thing);
        String tp = topic(" " + thing + " ");
        if (p.trait == Trait.CHEERFUL || p.trait == Trait.FRIENDLY) v += 15;
        if (p.trait == Trait.GRUMPY) v -= 20;
        if (p.trait == Trait.ADVENTUROUS && ("travel".equals(tp) || "sport".equals(tp) || "sea".equals(tp) || "danger".equals(tp))) v += 25;
        if (p.trait == Trait.DREAMY && ("space".equals(tp) || "art".equals(tp) || "music".equals(tp) || "books".equals(tp))) v += 25;
        if (p.trait == Trait.CURIOUS && ("tech".equals(tp) || "books".equals(tp) || "machines".equals(tp) || "space".equals(tp))) v += 25;
        if (p.trait == Trait.SHY && ("friends".equals(tp) || "danger".equals(tp))) v -= 15;
        if ("food".equals(tp) && thing.contains(Economy.label(Memory.favourite(p)).replaceFirst("^(a|an|some) ", ""))) v = 100;
        if ("danger".equals(tp)) v -= 30;
        return Math.max(0, Math.min(100, v));
    }

    static String opinionLine(CityData.Profile p, RandomSource r, String thing) {
        int o = opinion(p, thing);
        String T = cap(thing);
        if (o > 80) return pick(r, "I LOVE " + thing + "! Honestly one of my favourite things.", T + "? Yes. Absolutely yes.", "Are you kidding? I adore " + thing + ".", "Oh, " + thing + " is the best. Don't get me started!");
        if (o > 55) return pick(r, "I like " + thing + "! It's pretty great.", T + "? Yeah, I'm a fan.", "Sure, " + thing + " is nice. Why, do you like it too?", "I'd say " + thing + " is really good.");
        if (o > 35) return pick(r, T + " is okay, I guess. Not my favourite.", "Hmm, " + thing + "... it's fine. I don't think about it much.", "I don't mind " + thing + ". Could take it or leave it.", "Honestly? I'm neutral on " + thing + ".");
        if (o > 15) return pick(r, "Not really a fan of " + thing + ", to be honest.", T + "? Eh. Not my thing.", "I'm not big on " + thing + ". Sorry!", "Mm, " + thing + " isn't really for me.");
        return pick(r, "Ugh, " + thing + ". No thank you.", "I can't stand " + thing + "!", T + "? Please don't make me.", "Don't even mention " + thing + " around me.");
    }

    static String topicChat(CityData.Profile p, Resident r, RandomSource rnd, String tp, String kw) {
        String job = p.job.title.toLowerCase(Locale.ROOT);
        String fav = Economy.label(Memory.favourite(p));
        return switch (tp) {
            case "food" -> pick(rnd, "Food is my love language. My favourite is " + fav + ".", "Have you tried the noodles at Luna's? Life-changing.", "The Diesel Diner does the best burgers in Solaris.", "I could eat " + fav + " every day, honestly.", "Now I'm hungry. Thanks for that.");
            case "drink" -> pick(rnd, "I can't start the day without something warm to drink.", "Water! Stay hydrated, " + "friend.", "There's a little stall at the market with amazing juice.");
            case "weather" -> r.level().isRaining() ? pick(rnd, "It's raining right now, so I'm thinking noodles and a blanket.", "This rain! My hair is ruined.") : pick(rnd, "The weather's lovely today. Perfect for the pier.", "Blue skies over Solaris - can't beat it.", "I love a warm day. Makes everyone smile.");
            case "money" -> pick(rnd, "Money comes and goes. Mostly goes.", "I'm saving up" + (p.goal.isEmpty() ? " for something nice." : " for " + p.goal + "."), "The bank's been good to me - Hugo gives decent interest.", "Everything's getting pricier, have you noticed?");
            case "work" -> pick(rnd, "Being the " + job + " keeps me busy, but I like it.", "Work's work. The people make it fun.", "Some days at " + p.job.work().label + " fly by, some days drag.", "I'm proud of what I do, honestly.");
            case "home" -> pick(rnd, "Home is my favourite place. Cosy and quiet.", "I've been thinking about redecorating, actually.", "Nothing beats my own bed after a long day.");
            case "family" -> pick(rnd, "Family's important. This whole city feels like one big family to me.", "I miss my family sometimes. We should all get together more.", "Everyone in Solaris kind of looks out for each other, like family.");
            case "love" -> pick(rnd, "Love is complicated, isn't it?", p.partner.isEmpty() ? "Still waiting for the right person!" : "I'm lucky - I've got someone special.", "Ooh, are we talking romance? Tell me everything.", "Solaris at sunset is SO romantic, you know.");
            case "friends" -> pick(rnd, "Good friends are everything.", "I'd do anything for my friends.", "You're one of my friends too, you know!", "We should all hang out at the plaza sometime.");
            case "pets" -> pick(rnd, "Animals are the best! Have you met the cats around town?", "I've always wanted a pet of my own.", "Sam's parrot Captain is hilarious. It says 'ahoy' at everyone.", "Pets make everything better.");
            case "music" -> pick(rnd, "Music is my happy place. Remy's street songs are the best.", "Have you heard the Sky Organ? Incredible.", "I sing in the shower. Badly.", "I've got a playlist for every mood.");
            case "games" -> pick(rnd, "I'm a bit of a gamer, not gonna lie.", "The Neon Arcade has the best games. Rex keeps the leaderboard.", "Snake on the SolPhone is my weakness.", "Want to play something sometime? I'll go easy on you. Maybe.");
            case "sport" -> pick(rnd, "I try to stay active! A jog along the boardwalk does wonders.", "I'm not very sporty, honestly. I cheer though!", "Race you to the pier sometime?");
            case "screens" -> pick(rnd, "I watch way too much SolTube.", "Have you seen the SlimeFails channel? I cry laughing every time.", "A good movie night is unbeatable.", "Leo Cooks is my comfort channel.");
            case "tech" -> pick(rnd, "The SolPhone changed my life. I'm on SolFeed way too much.", "Tech is amazing. SolTech keeps releasing cool stuff.", "My screen time is... embarrassing.", "Did you see the new SolBox? It plays on your TV!");
            case "books" -> pick(rnd, "Nell at the library always has the best recommendations.", "I love a good story. Mysteries especially.", "Learning new things keeps the brain sharp!", "I should really read more.");
            case "machines" -> pick(rnd, "The factory machines are amazing - all those gears turning.", "Rosa at the garage can fix anything with an engine.", "Diesel engines power half of Solaris, you know.", "I love watching the belts at the factory. Weirdly relaxing.");
            case "sea" -> pick(rnd, "The ocean is so calming. I love the pier at sunset.", "Sam knows everything about boats. Ask him!", "The marina's beautiful this time of year.");
            case "space" -> pick(rnd, "The stars from the observatory are unreal.", "Do you think there's life out there?", "Nova does stargazing nights on Neon Heights - so magical.", "Space makes me feel tiny, in a good way.");
            case "sleep" -> pick(rnd, "Sleep is the best. I'm a big fan.", "I had the weirdest dream last night...", "I'm always tired. Is that normal?");
            case "health" -> pick(rnd, "Health comes first! Are you feeling okay?", "Drink water, eat your veggies, get some sleep. That's my advice.", "If you're hurt, rest up. Seriously.");
            case "fashion" -> pick(rnd, "Love your look, by the way.", "I'm trying a new style. Not sure it's working.", "Fashion is self-expression!");
            case "art" -> pick(rnd, "Art is everywhere in Solaris if you look.", "Have you seen the Founder's statue? That's art.", "I take photos of everything. SolFeed is full of them.");
            case "travel" -> pick(rnd, "I'd love to travel more. The sky island counts, right?", "The Sky Ferry ride is the best view in town.", "Adventure is out there!");
            case "danger" -> pick(rnd, "Yikes, don't talk about scary stuff!", "Stay safe out there, especially at night.", "I'd hide behind you if a creeper showed up, just saying.");
            case "nature" -> pick(rnd, "The park by the clock tower is so pretty.", "Nature is healing. Have you been to the Sky Gardens?", "I love the cherry trees in spring.");
            case "future" -> pick(rnd, "I think about the future a lot. " + (p.goal.isEmpty() ? "I just want to be happy." : "Right now I'm saving for " + p.goal + "."), "Who knows what tomorrow brings? That's the fun part.", "Big plans, " + "small steps.");
            case "past" -> {
                Mind.Ep e = p.mind.best(r.day() + 1, x -> x.imp >= 3);
                yield e != null ? "I still think about " + Calendar.relative(e.day, r.day()) + " - " + DayLog.lower(Events.sentence(e.text)) + "." : pick(rnd, "The old days were simpler, I guess.", "I don't dwell on the past much.");
            }
            case "feelings" -> {
                int mood = p.mood();
                yield mood > 70 ? pick(rnd, "Honestly, I'm feeling great lately.", "I'm in a really good mood today!") : mood > 40 ? pick(rnd, "Up and down, like everyone.", "I'm okay. Just okay.") : pick(rnd, "It's been a rough patch, honestly.", "Feelings are hard. Thanks for asking about them.");
            }
            case "city" -> pick(rnd, "Solaris is the best city in the world. I'm biased, but still.", "I love how the city keeps growing.", "The Founder really built something special here.", "Every street here has a story.");
            default -> null;
        };
    }

    static final Pattern DO_YOU_LIKE = Pattern.compile(" (?:do|dont|don't) you (?:like|love|enjoy|hate|prefer) (.+?) $");
    static final Pattern WHAT_THINK = Pattern.compile(" what (?:do|did) you think (?:of|about) (.+?) $");
    static final Pattern HOW_FEEL_ABOUT = Pattern.compile(" how (?:do|did) you feel about (.+?) $");
    static final Pattern HAVE_EVER = Pattern.compile(" (?:have|did) you ever (.+?) $");
    static final Pattern CAN_YOU = Pattern.compile(" (?:can|could) you (.+?) $");
    static final Pattern WOULD_YOU = Pattern.compile(" (?:would|will|wanna|want to) you (.+?) $|^ would you (.+?) $| do you want to (.+?) $| do you wanna (.+?) $");
    static final Pattern ARE_YOU = Pattern.compile(" (?:are|r) (?:you|u) (?:a |an |the )?(.+?) $");
    static final Pattern DID_YOU = Pattern.compile(" did you (.+?) $");
    static final Pattern DO_YOU = Pattern.compile(" (?:do|does) you (.+?) $");
    static final Pattern WHAT_IS = Pattern.compile(" (?:what is|what's|whats|what are|what r|who is|who's|whos|who are) (?:a |an |the )?(.+?) $");
    static final Pattern WHERE_IS = Pattern.compile(" (?:where is|where's|wheres|where are|where can i (?:find|get|buy)|where do i (?:find|get|buy)) (?:a |an |the |some )?(.+?) $");
    static final Pattern HOW_DO = Pattern.compile(" how (?:do|can|should) (?:i|you|we|one) (.+?) $");
    static final Pattern HOW_MANY = Pattern.compile(" how (?:many|much) (.+?) $");
    static final Pattern WHY = Pattern.compile(" why (?:do|does|did|are|is|were|was|can't|cant|don't|dont|would|should)? ?(.+?) $");
    static final Pattern WHEN = Pattern.compile(" when (?:do|does|did|is|are|will|was|were) (.+?) $");
    static final Pattern OR_Q = Pattern.compile(" (?:do you prefer |which is better |which do you like more |would you rather |do you like )?(.+?) or (.+?) $");
    static final Pattern I_LIKE = Pattern.compile(" i (?:really |kinda |kind of |also )?(like|love|adore|enjoy|hate|dislike|can't stand|cant stand) (.+?) $");
    static final Pattern I_DID = Pattern.compile(" i (?:just |finally |accidentally |recently )?(built|made|found|got|bought|won|lost|finished|started|saw|met|killed|caught|beat|broke|fixed|crafted|mined|planted|cooked|drew|painted|wrote|learned|explored|visited|tamed|named|adopted|decorated) (.+?) $");
    static final Pattern I_WILL = Pattern.compile(" i(?:'m| am|m) (?:going to|gonna|about to|planning to) (.+?) $| i (?:will|'ll|ll) (.+?) $");
    static final Pattern I_AM = Pattern.compile(" (?:i'm|im|i am) (?:so |really |very |kinda |a bit |pretty )?(.+?) $");
    static final Pattern MY_IS = Pattern.compile(" my (.+?) (?:is|are|was|were) (.+?) $");
    static final Pattern I_THINK = Pattern.compile(" i (?:think|believe|feel like|guess|reckon) (.+?) $");
    static final Pattern I_WANT = Pattern.compile(" i (?:want|wanna|need|wish i had|would like) (?:to )?(.+?) $");
    static final Pattern I_HAVE = Pattern.compile(" i (?:have|got|own|'ve got|ve got) (?:a |an |some |the )?(.+?) $");

    static String group(Matcher m) {
        for (int i = 1; i <= m.groupCount(); i++) if (m.group(i) != null && !m.group(i).isBlank()) return m.group(i).trim();
        return "";
    }

    static String strip(String s) {
        return s.replaceAll("^(the|a|an|some|really|so|very|to) ", "").replaceAll(" (though|tho|too|lol|haha|then|now|today|anyway|right|ok|okay|please)$", "").trim();
    }

    public static String reply(ServerPlayer pl, Resident r, String t, Chat.Ctx c) {
        CityData d = r.data();
        CityData.Profile p = r.profile();
        RandomSource rnd = r.getRandom();
        String pn = pl.getName().getString();
        String tp = topic(t);
        String kw = keyword(t);
        Matcher m;

        if (t.trim().matches("(why|how come|why not)")) {
            if (!c.why.isEmpty()) { String w = c.why; c.why = ""; return w; }
            return pick(rnd, "Why? Honestly... it just feels right.", "Good question! I guess that's just how I am.", "Because! Ha. Okay, I don't really know.");
        }
        if (t.trim().matches("(what about you|and you|you|how about you|wbu|hbu|what about u|and u)")) {
            if (!c.lastTopic.isEmpty()) {
                String line = topicChat(p, r, rnd, c.lastTopic, "");
                if (line != null) return line;
            }
            return "Me? I'm " + r.status() + ". Nothing too exciting!";
        }
        if (t.trim().matches("(really|for real|seriously|no way|are you sure|fr|srsly|u sure)")) return pick(rnd, "Really really!", "Hand on heart.", "Would I lie to you?", "Swear on the Founder's statue!");
        if (t.trim().matches("(me too|same|same here|me neither|mood|relatable|so true|true|facts|exactly|agreed|i agree)")) return pick(rnd, "See? We get each other.", "Great minds think alike!", "Ha! Twins.", "I knew I liked you.");
        if (t.trim().matches("(tell me more|go on|and then|what happened|continue|keep going|then what)")) {
            if (!c.lastTopic.isEmpty()) { String line = topicChat(p, r, rnd, c.lastTopic, ""); if (line != null) return line; }
            DayLog lg = p.log(r.routineDay());
            if (!lg.notes.isEmpty()) return "Well... " + Events.sentence(DayLog.lower(lg.notes.get(rnd.nextInt(lg.notes.size())))).replaceAll("[.!]+$", "") + ".";
            return "That's all there is to it, really!";
        }
        if (t.trim().matches("(lol|lmao|haha|hahaha|xd|rofl|hehe|lmfao|ha)")) { r.gesture(Resident.G_LAUGH, 30); return pick(rnd, "Haha!", "Glad I could make you laugh.", "Hehe.", "You're funny, " + pn + "."); }
        if (t.trim().matches("(what|huh|wdym|what do you mean|pardon|sorry what|come again|eh)")) return pick(rnd, "Ha, never mind! I'm rambling.", "I mean, you know. Life!", "Forget it, it was a silly thing to say.");
        if (t.trim().matches("(ok|okay|k|kk|alright|fine|cool|nice|neat|sweet|great|awesome|good|wow|oh|ah|i see|got it|gotcha|makes sense|fair|fair enough)")) return pick(rnd, "Yep!", "Mm-hm.", "Right?", "So, anything else going on with you?", "Anyway! What are you up to today?");

        if ((m = OR_Q.matcher(t)).find() && (t.contains(" prefer ") || t.contains(" better ") || t.contains(" rather ") || t.contains(" which ") || t.startsWith(" do you like ") || m.group(1).split(" ").length <= 4)) {
            String a = strip(m.group(1)), b = strip(m.group(2));
            if (!a.isEmpty() && !b.isEmpty() && a.split(" ").length <= 5 && b.split(" ").length <= 5) {
                String win = opinion(p, a) >= opinion(p, b) ? a : b, lose = win.equals(a) ? b : a;
                c.why = pick(rnd, cap(win) + " just makes me happier than " + lose + ".", "I've had more good memories with " + win + ".");
                return pick(rnd, cap(win) + ", easily!", "Hmm... " + win + ". Sorry, " + lose + "!", "Tough one. I'll go with " + win + ".", cap(win) + "! No contest.");
            }
        }
        if ((m = DO_YOU_LIKE.matcher(t)).find() || (m = WHAT_THINK.matcher(t)).find() || (m = HOW_FEEL_ABOUT.matcher(t)).find()) {
            String thing = strip(m.group(1));
            if (thing.equals("me")) return null;
            c.lastTopic = tp == null ? "" : tp;
            int o = opinion(p, thing);
            c.why = o > 55 ? pick(rnd, "It just makes me happy!", "Good memories, I guess.") : pick(rnd, "It's just never clicked for me.", "Bad experience once. Long story.");
            return opinionLine(p, rnd, flip(thing));
        }
        if ((m = HAVE_EVER.matcher(t)).find()) {
            String act = flip(strip(m.group(1)));
            boolean yes = seed(p, act) > 45 || p.trait == Trait.ADVENTUROUS && seed(p, act) > 20;
            return yes ? pick(rnd, "Once! I'll never forget it.", "Yes! Years ago. It was wild.", "I have, actually. Would do it again!", "Ha, yes - don't ask how it went.") : pick(rnd, "Never! But I'd love to " + act + " someday.", "Not yet. Is it fun?", "Nope. Have you?", "I haven't, but it's on my list!");
        }
        if ((m = CAN_YOU.matcher(t)).find()) {
            String act = flip(strip(m.group(1)));
            if (act.matches("(help|help you).*")) return pick(rnd, "Of course! What do you need?", "Always. What's up?");
            if (act.startsWith("tell") || act.startsWith("say") || act.startsWith("explain")) return pick(rnd, "Sure! Ask away.", "I'll try my best!");
            boolean can = seed(p, act) > 35;
            return can ? pick(rnd, "Sure, I can " + act + "! Not perfectly, but I can.", "Of course I can " + act + ". Watch me!", "Yep! I'm actually decent at it.") : pick(rnd, "Me? " + cap(act) + "? Not really, sorry!", "I wish I could " + act + ".", "Ha, I'd be terrible at that.");
        }
        if ((m = WOULD_YOU.matcher(t)).find()) {
            String act = flip(strip(group(m)));
            boolean yes = opinion(p, act) > 40;
            return yes ? pick(rnd, "Would I " + act + "? Absolutely!", "Yes! Count me in.", "Ooh, I'd love to " + act + ".", "Sure, why not!") : pick(rnd, "Hmm, I don't think I'd " + act + ".", "Probably not, honestly.", "Not really my thing, sorry!");
        }
        if ((m = ARE_YOU.matcher(t)).find()) {
            String adj = strip(m.group(1));
            int mood = p.mood();
            return switch (adj) {
                case "ok", "okay", "alright", "good", "fine", "well" -> mood > 45 ? pick(rnd, "I'm good, thanks for asking!", "Doing well!") : pick(rnd, "Not really, but I'll be fine.", "I've had better days.");
                case "happy" -> mood > 60 ? "Very! Life is good." : "I'm trying to be.";
                case "sad", "upset", "depressed", "down" -> mood < 40 ? "A little... thanks for noticing." : "No, I'm alright! Why, do I look sad?";
                case "hungry", "starving" -> p.hunger < 35 ? "Starving, actually!" : "Nope, I ate not long ago.";
                case "tired", "sleepy" -> r.timeOfDay() > 12500 ? "Exhausted. Bedtime soon." : "Not too bad!";
                case "bored" -> p.fun < 35 ? "SO bored. Entertain me!" : "Nope, I'm having a nice day.";
                case "busy" -> r.workBusy() ? "A bit - I'm working. What's up?" : "Not at all!";
                case "single" -> p.partner.isEmpty() ? "I am! Why, are you asking for a friend?" : "Nope, taken!";
                case "rich" -> p.coins + Bank.savings(d, p.id) > 200 ? "Comfortable, let's say." : "Ha! I wish.";
                case "real", "human", "a robot", "robot", "an ai", "ai", "alive" -> pick(rnd, "Last time I checked!", "As real as the Founder's statue.", "What kind of question is that? Ha.");
                case "scared", "afraid" -> "Of what?! Now I am.";
                case "mad", "angry", "annoyed" -> p.mind.trustIn(pn) < 0 ? "A little, yeah." : "At you? Never!";
                case "free" -> r.isFree() && !r.workBusy() ? "I'm free! What did you have in mind?" : "Not right now, sorry!";
                case "my friend", "friends with me" -> p.mind.trustIn(pn) > 5 ? "Of course I am!" : "We're getting there!";
                case "there", "here", "awake", "up" -> "I'm here! What's up?";
                default -> {
                    if (adj.startsWith("scared of ") || adj.startsWith("afraid of ") || adj.startsWith("frightened of ")) {
                        String th = adj.replaceFirst("^(scared|afraid|frightened) of ", "");
                        yield opinion(p, th) < 40 ? pick(rnd, "Terrified of " + th + "! Don't even joke.", "Honestly? Yes. " + cap(th) + " freak me out.") : pick(rnd, "Of " + th + "? Nah, I'm braver than I look.", "Not really! Should I be?");
                    }
                    if (adj.startsWith("going ") || adj.startsWith("coming ")) {
                        yield pick(rnd, "Maybe! Are you?", "I might be. Why, want to go together?", "Hmm, I hadn't planned to, but I could!");
                    }
                    if (adj.startsWith("good at ")) {
                        String th = adj.substring(8);
                        yield seed(p, th) > 45 ? "I like to think I'm pretty good at " + th + "!" : "At " + th + "? Ha, not at all.";
                    }
                    if (adj.startsWith("in love") || adj.startsWith("dating")) yield p.partner.isEmpty() ? "Not right now!" : "Yes! With " + d.profiles.get(p.partner).name + ".";
                    boolean yes = seed(p, adj) > 50;
                    yield yes ? pick(rnd, "I'd like to think so!", "Maybe a little " + adj + ", yeah.", "Ha, some people say I'm " + adj + ".") : pick(rnd, adj.length() > 16 ? "Ha, I don't think so!" : cap(adj) + "? Me? No way!", "Not really, no.", "What makes you say that?");
                }
            };
        }
        if ((m = DID_YOU.matcher(t)).find()) {
            String act = flip(strip(m.group(1)));
            DayLog lg = p.log(r.routineDay());
            for (String n : lg.notes) for (String w : act.split(" ")) if (w.length() > 3 && n.toLowerCase(Locale.ROOT).contains(w)) return "I did! " + Events.sentence(DayLog.lower(n)) + ".";
            if (act.contains("sleep")) return p.mood() > 40 ? "Like a baby, thanks!" : "Not really... tossed and turned.";
            if (act.contains("eat")) return p.hunger > 50 ? "Yep, I'm full!" : "Not yet! I'm starving.";
            return pick(rnd, "Not today, no.", "Hmm, no, I didn't. Should I have?", "Nope! Did you?");
        }
        if ((m = WHERE_IS.matcher(t)).find()) {
            String thing = strip(m.group(1));
            String k = Intents.place(" " + thing + " ", p);
            String shop = null;
            String tt = " " + thing + " ";
            if (topic(tt) != null && topic(tt).equals("food")) shop = "diner";
            if (has(tt, "bread") || has(tt, "cake") || has(tt, "pastry")) shop = "bakery";
            if (has(tt, "vegetables") || has(tt, "veggies") || has(tt, "carrots") || has(tt, "fruit") || has(tt, "groceries")) shop = "market";
            if (has(tt, "noodles") || has(tt, "ramen")) shop = "noodle";
            if (has(tt, "phone") || has(tt, "tv") || has(tt, "headphones") || has(tt, "tablet") || has(tt, "console") || has(tt, "computer")) shop = "tech";
            if (has(tt, "cogs") || has(tt, "tools") || has(tt, "gears") || has(tt, "shafts") || has(tt, "belts")) shop = "supply";
            if (has(tt, "money") || has(tt, "coins") || has(tt, "cash")) shop = "bank";
            if (has(tt, "books") || has(tt, "book")) shop = "library";
            if (has(tt, "fuel") || has(tt, "diesel")) shop = "fuel";
            if (has(tt, "letter") || has(tt, "parcel") || has(tt, "mail") || has(tt, "package")) shop = "post";
            if (k == null) k = shop;
            Place place = k == null ? null : Place.get(k);
            if (place != null) {
                int dx = place.pos.getX() - pl.getBlockX(), dz = place.pos.getZ() - pl.getBlockZ();
                String dir = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? "east" : "west") : (dz > 0 ? "south" : "north");
                return (shop != null && k.equals(shop) && Intents.place(tt, p) == null ? "Try " + place.label + " - " : cap(place.label) + " is ") + "about " + (int) Math.sqrt(dx * dx + dz * dz) + " blocks " + dir + " of here" + (place.island ? ", up on Neon Heights (take the Sky Ferry)" : "") + ".";
            }
            for (CityData.Profile q : d.profiles.values()) if (thing.contains(q.name.toLowerCase(Locale.ROOT))) {
                Resident qr = Phones.entity((net.minecraft.server.level.ServerLevel) r.level(), q);
                return qr == null ? "I haven't seen " + q.name + " around lately." : q.name + "? Last I saw, they were " + qr.status() + ".";
            }
            return pick(rnd, "Hmm, I'm not sure where to find " + flip(thing) + ". Try the SolPhone map?", "No idea, sorry! Ask Pip - postmen know every corner of town.", "I don't think we have " + flip(thing) + " in Solaris... yet!");
        }
        if ((m = HOW_MANY.matcher(t)).find()) {
            String thing = m.group(1);
            if (thing.contains("resident") || thing.contains("people")) return "There are " + d.profiles.size() + " of us living in Solaris right now!";
            if (thing.contains("coin") || thing.contains("money")) return "Let's see... " + p.coins + " coins in my pocket, and " + Bank.savings(d, p.id) + " in the bank. Don't tell anyone!";
            if (thing.contains("friend")) {
                int n = 0;
                for (CityData.Profile q : d.profiles.values()) { CityData.Rel rel = d.peekRel(p.id, q.id); if (q != p && rel != null && rel.friend()) n++; }
                return n == 0 ? "Still making friends! You count though." : n + " good friends, and you make " + (n + 1) + "!";
            }
            if (thing.contains("old") || thing.contains("year")) return "A lady never tells - and neither do I.";
            return pick(rnd, "Hmm, a lot? I've never counted!", "At least seven. Maybe eleven. Who's counting?", "More than you'd think!");
        }
        if ((m = HOW_DO.matcher(t)).find()) {
            String act = flip(strip(m.group(1)));
            if (act.contains("money") || act.contains("rich") || act.contains("coins")) return "Get a job, save at the bank, and don't spend it all at the arcade. That's my secret!";
            if (act.contains("island") || act.contains("neon heights") || act.contains("sky")) return "Take the Sky Ferry from the pad by the pier - it's quick!";
            if (act.contains("friend")) return "Just be yourself and say hi! Everyone here is super nice.";
            if (act.contains("skydive")) return "Step on the Sky Launch pad and hold on to your stomach!";
            return pick(rnd, "Practice! Lots of practice.", "Honestly? Look it up at the library - Nell will know.", "I'd start small and figure it out as you go.", "Hmm, I've never tried to " + act + ". Tell me if you figure it out!");
        }
        if ((m = WHEN.matcher(t)).find()) {
            String q = m.group(1);
            if (q.contains("work") || q.contains("shift") || q.contains("finish") || q.contains("off")) {
                long off = r.level().getDayTime();
                for (int i = 0; i < 48 && r.workingAt(Math.floorMod(off, 24000L), Calendar.dayOf(off + 1500)); i++) off += 500;
                return r.workingAt(r.timeOfDay(), r.routineDay()) ? "I finish around " + Meets.when(off) + "." : "I'm off right now! I usually start early in the morning.";
            }
            if (q.contains("sleep") || q.contains("bed")) return "Usually around 8 or 9pm. I need my beauty sleep!";
            if (q.contains("eat") || q.contains("lunch")) return "Lunch is around noon, if I remember!";
            if (q.contains("festival")) return Festival.running() ? "It's on right now!" : "Nobody knows exactly - it just happens, usually on a nice evening. Keep an eye on SolFeed!";
            if (q.contains("birthday")) return "Ooh, that's a secret. You'll have to guess!";
            return pick(rnd, "Hmm, soon I think!", "Not sure, honestly. Keep an eye on SolFeed.", "Whenever the Founder decides, probably!");
        }
        if ((m = WHY.matcher(t)).find() && t.startsWith(" why ")) {
            String q = m.group(1);
            if (q.contains("you") && (q.contains("sad") || q.contains("upset") || q.contains("angry"))) return p.hunger < 30 ? "Honestly I'm just hungry. Hangry, even." : p.social < 30 ? "I've been a bit lonely lately." : "Oh, nothing serious. Just one of those days.";
            if (q.contains("you") && (q.contains("happy") || q.contains("smiling"))) return pick(rnd, "Because you're here!", "Good day, good food, good people.");
            if (q.contains("work") || q.contains("job")) return "Gotta pay the bills! And I do actually like being the " + p.job.title.toLowerCase(Locale.ROOT) + ".";
            if (q.contains("sky") && q.contains("blue")) return "Something about light scattering. Nell explained it once and I nodded a lot.";
            return pick(rnd, "Good question. I think it's just how things are in Solaris.", "Honestly? Nobody really knows.", "Because the Founder made it that way!", "I've wondered that too...");
        }
        if ((m = WHAT_IS.matcher(t)).find()) {
            String thing = strip(m.group(1));
            String def = Knowledge.define(thing, p, d);
            if (def != null) return def;
            if (thing.startsWith("your ") || thing.startsWith("ur ")) {
                String what = thing.replaceFirst("^(your|ur) ", "");
                if (what.contains("name")) return "I'm " + p.name + "!";
                if (what.contains("job")) return "I'm the " + p.job.title.toLowerCase(Locale.ROOT) + ".";
                if (what.contains("dream")) return p.goal.isEmpty() ? "To be happy and make my friends happy." : "Honestly? " + cap(p.goal) + ". I'm saving up!";
                if (what.contains("hobby")) return "I love " + Memory.hobby(p) + ".";
                if (what.contains("fav")) return "My favourite food is " + Economy.label(Memory.favourite(p)) + "!";
                return pick(rnd, "My " + what + "? Hmm, good question. Let me think about that one.", "Ooh, personal! I'll tell you later.", "My " + what + "? That's a secret!");
            }
            c.lastTopic = "explain";
            c.explain = thing;
            return pick(rnd, cap(flip(thing)) + "? Hmm, I'm not sure. What is it?", "I've never heard of " + flip(thing) + "! Explain?", "No idea, honestly. Tell me!");
        }
        if (c.lastTopic.equals("explain") && !c.explain.isEmpty()) {
            String e = c.explain;
            c.lastTopic = "";
            c.explain = "";
            return pick(rnd, "Ohh, so " + flip(e) + " is " + clip(flip(t.trim()), 8) + "? Makes sense now!", "Huh! I learn something new every day.", "Interesting - I'll remember that about " + flip(e) + ".");
        }
        if (t.startsWith(" do you ") && (m = DO_YOU.matcher(t)).find()) {
            String act = flip(strip(m.group(1)));
            if (act.startsWith("know ")) {
                String who = act.substring(5);
                for (CityData.Profile q : d.profiles.values()) if (who.contains(q.name.toLowerCase(Locale.ROOT))) {
                    CityData.Rel rel = d.peekRel(p.id, q.id);
                    return rel == null || !rel.met ? "I don't think I've met " + q.name + " yet." : "Of course! " + q.name + " is the " + q.job.title.toLowerCase(Locale.ROOT) + ". " + (rel.friend() ? "We're friends!" : "Nice enough.");
                }
                String def = Knowledge.define(who, p, d);
                if (def != null) return "Sure! " + def;
                return pick(rnd, "Hmm, " + who + "? Doesn't ring a bell.", "I don't think so! Should I?");
            }
            if (act.startsWith("have ")) {
                String thing = act.substring(5);
                if (thing.contains("phone")) return p.ownsPhone ? "Yep, my SolPhone! Text me anytime." : "Not yet - I'm saving up for one.";
                if (thing.contains("pet")) return Pets.petName(p.id) != null ? "I do! " + Pets.petName(p.id) + " is the love of my life." : "No, but I'd love one!";
                if (thing.contains("pc") || thing.contains("computer")) return p.ownsPC ? "Yep, at home!" : "Nope, I use the library ones.";
                if (thing.contains("partner") || thing.contains("girlfriend") || thing.contains("boyfriend")) return p.partner.isEmpty() ? "Nope, single!" : "I do! " + d.profiles.get(p.partner).name + ".";
                if (thing.contains("money") || thing.contains("coins")) return p.coins > 20 ? "A little, why? Ha." : "Barely!";
                return seed(p, thing) > 50 ? "I do, actually!" : "No, I don't. Wish I did!";
            }
            if (act.startsWith("live")) { Place h = p.homePlace(); return h == null ? "Not really anywhere right now." : "I live in " + h.label + "."; }
            if (act.startsWith("work")) return "I work at " + p.job.work().label + " as the " + p.job.title.toLowerCase(Locale.ROOT) + ".";
            if (act.startsWith("remember")) return Mind.playerLine(p, pn, r.day() + 1, rnd) != null ? Mind.playerLine(p, pn, r.day() + 1, rnd) : "Hmm, remind me?";
            if (act.startsWith("want") || act.startsWith("wanna")) return opinion(p, act) > 40 ? pick(rnd, "Yes please!", "Sure, sounds fun!") : pick(rnd, "Not right now, thanks.", "Maybe another time!");
            if (act.startsWith("believe")) return pick(rnd, "I believe in the Founder, and in good food. That's it.", "Hmm, I'm not sure. Do you?");
            boolean yes = opinion(p, act) > 45;
            return yes ? pick(rnd, "I do! Why?", "Yep, all the time.", "Of course!", "Yes - is that weird?") : pick(rnd, "Not really, no.", "Nope! Do you?", "Hmm, not often.");
        }

        if ((m = I_LIKE.matcher(t)).find()) {
            String verb = m.group(1), thing = flip(strip(m.group(2)));
            boolean pos = verb.equals("like") || verb.equals("love") || verb.equals("adore") || verb.equals("enjoy");
            int o = opinion(p, thing);
            c.lastTopic = tp == null ? "" : tp;
            if (pos) return o > 55 ? pick(rnd, "Me too! " + cap(thing) + " is great.", "Same! We have good taste.", "Yes! " + cap(thing) + " forever.") : pick(rnd, "Really? I'm not sure about " + thing + ", but I'm glad you like it!", "Ha, to each their own! What do you like about " + thing + "?");
            return o > 55 ? pick(rnd, "Noooo, " + thing + " is great!", "What? How could you hate " + thing + "?") : pick(rnd, "Ugh, me too!", "Finally someone who gets it.", "Right? " + cap(thing) + " is the worst.");
        }
        if ((m = I_DID.matcher(t)).find()) {
            String verb = m.group(1), thing = flip(strip(m.group(2)));
            Mind.playerEvent(d, p, pn, r.day(), "{P} told me they " + verb + " " + clip(thing, 6), 1, 2);
            return switch (verb) {
                case "built", "made", "crafted", "decorated", "drew", "painted", "wrote", "cooked" -> pick(rnd, "You " + verb + " " + thing + "? That's amazing! Can I see?", "Wow, " + verb + " it yourself? I'm impressed!", "No way! You'll have to show me " + thing + ".");
                case "found", "got", "bought", "won", "caught", "tamed", "adopted" -> pick(rnd, "You " + verb + " " + thing + "? Lucky!", "Ooh, congrats on " + thing + "!", "Nice! What are you going to do with it?");
                case "lost", "broke" -> pick(rnd, "Oh no, you " + verb + " " + thing + "? I'm sorry!", "Aw, that's rough. Can it be fixed?", "Nooo! Want help looking?");
                case "killed", "beat" -> pick(rnd, "You " + verb + " " + thing + "? Wow, remind me never to get on your bad side!", "Ha! Victory!", "Brave! I'd have run away.");
                case "met", "saw", "visited", "explored" -> pick(rnd, "You " + verb + " " + thing + "? How was it?", "Ooh, tell me more about " + thing + "!", "That sounds like an adventure!");
                default -> pick(rnd, "You " + verb + " " + thing + "? Nice one!", "Look at you go!", "That's awesome, " + pn + ".");
            };
        }
        if ((m = I_WILL.matcher(t)).find()) {
            String act = flip(strip(group(m)));
            return pick(rnd, "Ooh, you're going to " + act + "? Good luck!", "That sounds fun! Tell me how it goes.", "Wait, can I come?", "Nice! Don't forget to post about it on SolFeed.");
        }
        if ((m = I_WANT.matcher(t)).find()) {
            String act = flip(strip(m.group(1)));
            return pick(rnd, "Then go for it! What's stopping you?", "I hope you get to " + act + "!", "Ooh, I want that too.", "You deserve it! Want help?");
        }
        if ((m = MY_IS.matcher(t)).find()) {
            String thing = m.group(1), how = m.group(2);
            if (how.matches(".*(bad|broken|sick|ill|dead|died|lost|gone|sad|annoying|mean|hurt|injured|missing|terrible|awful|angry|upset).*")) return pick(rnd, "Oh no, your " + thing + " is " + how + "? I'm so sorry.", "Aw, that's awful. I hope your " + thing + " is okay soon.", "Oh no! Is there anything I can do?");
            if (how.matches(".*(great|good|amazing|awesome|cool|cute|beautiful|nice|best|happy|fun|huge|big|new|finished|done).*")) return pick(rnd, "Your " + thing + " is " + how + "? Love that!", "That's great to hear!", "Ooh, I want to see your " + thing + "!");
            return pick(rnd, "Your " + thing + " is " + how + "? Tell me more!", "Really? Your " + thing + "?", "Huh, I didn't know that about your " + thing + ".");
        }
        if ((m = I_HAVE.matcher(t)).find()) {
            String thing = flip(strip(m.group(1)));
            return pick(rnd, "You have " + thing + "? Lucky!", "Ooh, show me " + thing + " sometime!", "Nice! How long have you had " + thing + "?");
        }
        if ((m = I_THINK.matcher(t)).find()) {
            String idea = flip(strip(m.group(1)));
            return seed(p, idea) > 40 ? pick(rnd, "I think so too!", "You might be right about that.", "Hmm, yeah, I agree.") : pick(rnd, "Really? I'm not so sure.", "Interesting take! I see it differently.", "Hmm, maybe. I'll think about it.");
        }
        if ((m = I_AM.matcher(t)).find()) {
            String state = strip(m.group(1));
            if (state.matches("(back|here|home)")) return pick(rnd, "Welcome back!", "Yay, you're here!");
            if (state.matches("(.*hungry.*|.*starving.*)")) return "Go get some food! The Diesel Diner's great.";
            if (state.matches("(.*sick.*|.*hurt.*|.*injured.*)")) return "Oh no! Rest up and feel better, okay?";
            if (state.matches("(bored|so bored)")) return "Let's fix that - want to try the Sky Launch?";
            if (state.matches("(tired|sleepy|exhausted)")) return "Get some sleep! Seriously, you deserve it.";
            if (state.matches("(.*busy.*)")) return "Don't overwork yourself!";
            if (state.matches("(.*years old.*|\\d+)")) return "Oh nice! Great age.";
            if (state.matches("(the founder|founder)")) return "I know! We all know. You're kind of famous.";
            return pick(rnd, "You're " + flip(state) + "? " + pick(rnd, "Tell me more!", "How come?", "I get that."), "Oh, I didn't know you were " + flip(state) + "!", "Thanks for telling me that.");
        }

        if (tp != null) {
            c.lastTopic = tp;
            String line = topicChat(p, r, rnd, tp, kw);
            if (line != null) {
                boolean q = t.startsWith(" what ") || t.startsWith(" how ") || t.startsWith(" who ") || t.startsWith(" is ") || t.startsWith(" are ") || t.startsWith(" do ") || t.startsWith(" can ") || t.startsWith(" should ");
                return line + (rnd.nextInt(3) == 0 && !q ? " " + pick(rnd, "What about you?", "Do you like " + kw + "?", "Anyway, how's your day going?") : "");
            }
        }
        return null;
    }

    /** Called when absolutely nothing matched: keep the conversation going instead of a blank "interesting". */
    public static String fallback(ServerPlayer pl, Resident r, String t, Chat.Ctx c) {
        RandomSource rnd = r.getRandom();
        String pn = pl.getName().getString();
        String[] w = t.trim().split(" ");
        boolean question = w.length > 0 && w[0].matches("(what|where|when|who|why|how|which|is|are|do|does|did|can|could|would|will|should|have|has|was|were|am)");
        String gist = clip(flip(t.trim()), 7);
        if (question) return pick(rnd, "Hmm, good question! I'd say... probably yes? Don't quote me.", "You know, I've never really thought about that. What do you think?", "Oh, that's a tricky one. Ask me again after lunch!", "Honestly? I'm not sure, " + pn + ". Nell at the library might know!", "I have a theory, but it's a bit silly. What's yours?");
        if (w.length <= 2) return pick(rnd, cap(gist) + "?", "Ha, " + gist + " indeed.", "Hm? " + cap(gist) + "?", "Say more!");
        return pick(rnd, "Wait, " + gist + "? Tell me more!", "Ha, " + gist + " - I didn't expect that!", "Huh, so " + gist + "? That's new to me.", "I hear you. So what happened next?", "Oh? " + cap(gist) + "... how do you feel about that?");
    }
}
