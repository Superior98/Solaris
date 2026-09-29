package com.fireheart.city;

import java.util.List;

/** What residents remember about each other: favourite things, facts learned, plans kept or broken. */
public final class Memory {
    private Memory() {}

    private static final String[] FAVS = {"minecraft:bread", "farmersdelight:apple_pie", "minecraft:cookie", "minecraft:cooked_beef",
            "minecraft:mushroom_stew", "farmersdelight:vegetable_soup", "minecraft:pumpkin_pie", "farmersdelight:dumplings",
            "minecraft:cooked_cod", "farmersdelight:fried_rice", "minecraft:cake", "farmersdelight:stuffed_potato"};

    public static String favourite(CityData.Profile p) {
        return FAVS[Math.floorMod(p.id.hashCode() * 17 + 3, FAVS.length)];
    }

    public static String hobby(CityData.Profile p) {
        return switch (p.trait) {
            case ADVENTUROUS -> "riding the Skyliner up to Neon Heights";
            case DREAMY -> "watching the clouds from the pier";
            case CURIOUS -> "reading at the library";
            case SHY -> "fishing at the boardwalk where it's quiet";
            case GRUMPY -> "sleeping in, honestly";
            case TALKATIVE -> "hanging out at the plaza and meeting people";
            case FRIENDLY -> "cooking for my friends";
            default -> "dancing at the Neon Arcade";
        };
    }

    public static long nextBirthday(CityData.Profile p, long day) {
        long d = day;
        while (Math.floorMod(d, 16) != p.birthdayIndex()) d++;
        return d;
    }

    public static void attend(CityData d, CityData.Profile p, String placeKey, long day) {
        for (CityData.Plan pl : d.plans) {
            if (pl.day == day && pl.who.contains(p.id) && pl.place.equals(placeKey) && pl.came.add(p.id)) {
                d.setDirty();
                for (String o : pl.who) {
                    if (o.equals(p.id)) continue;
                    CityData.Profile q = d.profiles.get(o);
                    if (q != null) p.log(day).note(describe(pl, p, q, true));
                    break;
                }
            }
        }
    }

    public static String describe(CityData.Plan pl, CityData.Profile me, CityData.Profile other, boolean firstPerson) {
        Place place = Place.get(pl.place);
        String where = place == null ? "town" : place.label;
        return switch (pl.what) {
            case "date" -> "I went on a date with " + other.name + " at " + where;
            case "visit" -> pl.place.equals(me.home) ? other.name + " came over to my place" : "I visited " + other.name + " at " + where;
            case "party" -> "I went to a birthday party at " + where;
            case "dance" -> "I danced the night away at the Sky Organ party";
            case "trip" -> "I took a trip to " + where + " with " + other.name;
            default -> "I hung out with " + other.name + " at " + where;
        };
    }

    public static String memoryOf(CityData.Plan pl, long today) {
        Place place = Place.get(pl.place);
        String where = place == null ? "town" : place.label;
        String when = Calendar.relative(pl.day, today);
        return switch (pl.what) {
            case "date" -> "our date at " + where + " " + when;
            case "visit" -> "the evening at " + where + " " + when;
            case "party" -> "the party at " + where + " " + when;
            case "dance" -> "dancing at the Sky Organ party " + when;
            case "trip" -> "our trip to " + where + " " + when;
            default -> "hanging out at " + where + " " + when;
        };
    }

    public static CityData.Plan pendingReview(CityData d, CityData.Profile a, CityData.Profile b, long day) {
        for (CityData.Plan pl : d.plans) {
            if (pl.day >= day || pl.day < day - 3) continue;
            if (pl.who.size() > 3 || java.util.Set.of("fireworks", "lottery", "worship", "tour", "stargaze", "dance", "speech", "campaign").contains(pl.what)) continue;
            if (!pl.who.contains(a.id) || !pl.who.contains(b.id)) continue;
            if (pl.reviewed.contains(a.id)) continue;
            return pl;
        }
        return null;
    }

    public static CityData.Plan todayTogether(CityData d, CityData.Profile a, CityData.Profile b, long day) {
        List<CityData.Plan> ps = d.plansFor(a.id, day);
        for (CityData.Plan pl : ps) if (pl.who.contains(b.id)) return pl;
        return null;
    }

    public static String unknownFact(CityData.Rel r, CityData.Profile about) {
        if (!r.facts.containsKey("food")) return "food";
        if (Pets.petName(about.id) != null && !r.facts.containsKey("pet")) return "pet";
        if (!r.facts.containsKey("hobby")) return "hobby";
        if (!r.facts.containsKey("birthday")) return "birthday";
        return null;
    }
}
