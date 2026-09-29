package com.fireheart.city;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** What residents know about the world: Solaris places, gadgets, people and common things. */
public final class Knowledge {
    private Knowledge() {}

    static final Map<String, String> FACTS = new LinkedHashMap<>();

    static void f(String answer, String... keys) {
        for (String k : keys) FACTS.put(k, answer);
    }

    static {
        f("Solaris is our city! Founded by the Founder - the one with the giant statue.", "solaris", "this city", "the city", "this place", "this town");
        f("The Founder built Solaris from nothing. There's a 96-block statue of them by the sacred path.", "founder", "the founder", "stellarfox1", "stellarfox");
        f("That's the Founder's statue - 96 blocks tall. We hold the Festival of the Founder there.", "statue", "the statue", "founder's statue");
        f("The Festival of the Founder! Sermon, chanting, offerings, then a feast with cake, music, gifts and fireworks.", "festival", "the festival", "festival of the founder");
        f("Neon Heights is the floating sky island - arcade, noodle bar, gardens and the observatory. Take the Sky Ferry up!", "neon heights", "sky island", "the island", "floating island");
        f("The Sky Ferry flies between the city pad by the pier and Neon Heights. Jet's the best pilot.", "sky ferry", "ferry", "the ferry");
        f("The Sky Launch fires you straight up a glass tube so you can skydive over the city. Terrifying. Amazing.", "sky launch", "skylaunch", "skydiving", "skydive", "launch tower");
        f("The SolPhone! Texts, calls, SolFeed, maps, camera, games - everything.", "solphone", "phone", "firephone");
        f("SolFeed is where everyone posts photos and news. I'm on it way too much.", "solfeed", "firefeed");
        f("SolTube is our video site. SlimeFails and Leo Cooks are the best channels.", "soltube", "firetube", "youtube");
        f("SolEats delivers food right to you. Pip brings it round.", "soleats", "fireeats");
        f("SolTech is the tech store - phones, TVs, headphones, and the new SolBox console.", "soltech", "firetech", "tech store");
        f("The Magma Beach Bar is down on the new beach east of Ember Heights - Gus built it for magmagamer9.", "beach bar", "magma beach", "bar");
        f("magmagamer9's hotel is up on the hill by the beach, around 51 78 33.", "hotel", "magmagamer9");
        f("Gus is the city's repair technician - if a creeper blows something up, he rebuilds it.", "gus", "repair", "fix", "broken");
        f("The SolBox is the newest console - plug it in near a TV and play on the big screen with a controller!", "solbox", "console", "xbox");
        f("The Sky Organ is the big music stage up on Neon Heights. The concerts are unreal.", "sky organ", "organ", "music area");
        f("The bank is where Hugo keeps everyone's coins safe. Decent interest too!", "bank", "the bank");
        f("Coins are Solaris money. Gold nuggets and ingots, basically.", "coins", "coin", "money");
        f("The library - Nell's kingdom. Shh.", "library");
        f("The Diesel Diner does burgers, fries and shakes. Classic.", "diner", "diesel diner");
        f("Luna's Neon Noodle Bar up on Neon Heights. Best ramen anywhere.", "noodle bar", "noodles", "ramen", "luna's");
        f("The Neon Arcade on the island. Rex keeps the high score board.", "arcade");
        f("The factory makes iron sheets and parts. Ben runs the floor and keeps the machines going.", "factory");
        f("The clock tower in the park. Zara keeps it ticking - it's never late!", "clock tower", "clock");
        f("The old pier is my favourite sunset spot.", "pier", "the pier");
        f("The observatory's on Neon Heights. Nova runs stargazing nights.", "observatory");
        f("Ember Heights is the apartment tower. Lots of us live there! Ellie runs the front desk in the lobby and checks everyone in.", "ember heights", "apartments", "apartment tower");
        f("The front desk's in the Ember Heights lobby. Ellie checks everyone in - she's super chill. And a bit of a flirt.", "front desk", "reception", "receptionist", "lobby", "check in");
        f("The post office - Pip sorts everything there, then delivers it all over town.", "post office", "post", "mail");
        f("Create is how all our machines work - gears, belts, engines and trains.", "create", "create mod");
        f("Diesel powers the engines around town. The fuel station's by the garage.", "diesel", "fuel");
        f("Creepers? Green, hissy, explode. Stay away from them!", "creeper", "creepers");
        f("Zombies come out at night. Stay inside after dark!", "zombie", "zombies");
        f("Minecraft? Isn't that... everything? Ha.", "minecraft");
        f("Diamonds are super rare and super shiny. I've only ever seen a couple.", "diamond", "diamonds");
        f("The sun's what makes Solaris so bright - it's in the name!", "the sun", "sun");
        f("The moon! Look at it from the observatory - you can see every crater.", "the moon", "moon");
        f("Love is when someone makes the ordinary feel special. Cheesy, but true.", "love");
        f("The meaning of life? Good food, good friends, good sunsets.", "meaning of life", "the meaning of life", "life");
        f("That's magmagamer9 - the Founder's brother! He lives out west. Has a very cute parrot.", "magmagamer9", "magmagamer", "your brother", "the founder's brother");
    }

    public static String define(String thing, CityData.Profile me, CityData d) {
        String t = thing.toLowerCase(Locale.ROOT).trim();
        for (CityData.Profile q : d.profiles.values()) {
            if (!t.equals(q.name.toLowerCase(Locale.ROOT))) continue;
            if (q == me) return "That's me! " + me.name + ", the " + me.job.title.toLowerCase(Locale.ROOT) + ".";
            CityData.Rel rel = d.peekRel(me.id, q.id);
            String how = rel == null || !rel.met ? "I haven't really met them." : rel.rival ? "We don't get along." : rel.friend() ? "We're friends!" : "Nice enough.";
            return q.name + " is the " + q.job.title.toLowerCase(Locale.ROOT) + " at " + q.job.work().label + ". " + how;
        }
        String a = FACTS.get(t);
        if (a != null) return a;
        for (Map.Entry<String, String> e : FACTS.entrySet()) if (e.getKey().length() > 4 && t.contains(e.getKey())) return e.getValue();
        return null;
    }
}
