package com.fireheart.city;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

public final class Economy {
    public record Food(int restore, int price, String label) {}

    public static final Map<String, Food> FOOD = new HashMap<>();
    public static final Map<String, String> LABEL = new HashMap<>();

    static {
        food("minecraft:bread", 35, 3, "fresh bread");
        food("minecraft:cookie", 12, 1, "a cookie");
        food("minecraft:pumpkin_pie", 40, 4, "pumpkin pie");
        food("minecraft:cooked_beef", 45, 5, "a Diesel burger");
        food("minecraft:baked_potato", 30, 3, "a baked potato");
        food("minecraft:mushroom_stew", 50, 5, "neon ramen");
        food("minecraft:carrot", 15, 1, "a crunchy carrot");
        food("minecraft:apple", 20, 2, "an apple");
        food("minecraft:cooked_cod", 35, 4, "grilled fish");
        food("minecraft:cake", 60, 8, "a birthday cake");
        food("farmersdelight:apple_pie", 55, 7, "an apple pie");
        food("farmersdelight:vegetable_soup", 45, 5, "a bowl of vegetable soup");
        food("farmersdelight:stuffed_potato", 40, 4, "a stuffed potato");
        food("farmersdelight:hamburger", 55, 6, "a double Diesel burger");
        food("farmersdelight:steak_and_potatoes", 60, 7, "steak and potatoes");
        food("farmersdelight:fried_egg", 20, 2, "a fried egg");
        food("farmersdelight:noodle_soup", 50, 5, "a bowl of noodle soup");
        food("farmersdelight:vegetable_noodles", 50, 5, "vegetable noodles");
        food("farmersdelight:fried_rice", 45, 4, "fried rice");
        food("farmersdelight:dumplings", 40, 4, "dumplings");
        food("farmersdelight:fish_stew", 50, 6, "fish stew");
        food("farmersdelight:honey_cookie", 14, 1, "a honey cookie");
        label("create:cogwheel", "a cogwheel");
        label("create:iron_sheet", "an iron sheet");
        label("minecraft:iron_ingot", "an iron ingot");
        label("minecraft:flint", "some flint");
        label("minecraft:chain", "a chain");
        label("minecraft:poppy", "a poppy");
        label("minecraft:tripwire_hook", "a spare room key");
        label("minecraft:dandelion", "a dandelion");
        label("minecraft:bucket", "a bucket");
        label("minecraft:clock", "a pocket clock");
        label("minecraft:emerald", "an arcade token");
        label("minecraft:map", "a tour map");
        label("minecraft:paper", "a shuttle ticket");
        label("minecraft:book", "a library book");
        label("minecraft:wheat", "some wheat");
        label("create:wheat_flour", "a bag of flour");
        label("farmersdelight:wheat_dough", "some dough");
        label("minecraft:cobblestone", "some cobblestone");
        label("minecraft:gravel", "some gravel");
        label("create:shaft", "a shaft");
        label("minecraft:cod", "a fresh fish");
        label("minecraft:writable_book", "a bank ledger");
        label("minecraft:gold_nugget", "a gold coin");
        label("fireheartcity:phone", "a SolPhone");
        label("fireheartcity:computer", "a Solaris PC");
    }

    private static void food(String id, int restore, int price, String label) {
        FOOD.put(id, new Food(restore, price, label));
        LABEL.put(id, label);
    }

    private static void label(String id, String label) {
        LABEL.put(id, label);
    }

    public static String label(String id) {
        return LABEL.getOrDefault(id, "something");
    }

    private static final Map<String, String[]> NOUN = new HashMap<>();

    static {
        noun("minecraft:bread", "loaf of bread", "loaves of bread");
        noun("minecraft:cookie", "cookie", "cookies");
        noun("farmersdelight:honey_cookie", "honey cookie", "honey cookies");
        noun("minecraft:pumpkin_pie", "pumpkin pie", "pumpkin pies");
        noun("farmersdelight:apple_pie", "apple pie", "apple pies");
        noun("minecraft:cake", "cake", "cakes");
        noun("farmersdelight:vegetable_soup", "pot of vegetable soup", "pots of vegetable soup");
        noun("farmersdelight:stuffed_potato", "stuffed potato", "stuffed potatoes");
        noun("minecraft:cooked_beef", "Diesel burger", "Diesel burgers");
        noun("farmersdelight:hamburger", "double burger", "double burgers");
        noun("farmersdelight:steak_and_potatoes", "steak plate", "steak plates");
        noun("minecraft:baked_potato", "baked potato", "baked potatoes");
        noun("farmersdelight:fried_egg", "fried egg", "fried eggs");
        noun("minecraft:mushroom_stew", "bowl of neon ramen", "bowls of neon ramen");
        noun("farmersdelight:noodle_soup", "noodle soup", "noodle soups");
        noun("farmersdelight:vegetable_noodles", "plate of veggie noodles", "plates of veggie noodles");
        noun("farmersdelight:fried_rice", "fried rice", "portions of fried rice");
        noun("farmersdelight:dumplings", "dumpling plate", "dumpling plates");
        noun("minecraft:cooked_cod", "grilled fish", "grilled fish");
        noun("farmersdelight:fish_stew", "fish stew", "fish stews");
        noun("minecraft:carrot", "carrot", "carrots");
        noun("minecraft:apple", "apple", "apples");
        noun("create:cogwheel", "cogwheel", "cogwheels");
        noun("create:iron_sheet", "iron sheet", "iron sheets");
        noun("create:shaft", "shaft", "shafts");
        noun("minecraft:iron_ingot", "iron ingot", "iron ingots");
        noun("minecraft:flint", "piece of flint", "pieces of flint");
        noun("minecraft:gravel", "load of gravel", "loads of gravel");
        noun("minecraft:chain", "chain", "chains");
        noun("minecraft:poppy", "poppy", "poppies");
        noun("minecraft:tripwire_hook", "room key", "room keys");
        noun("minecraft:dandelion", "dandelion", "dandelions");
        noun("minecraft:bucket", "bucket", "buckets");
        noun("minecraft:clock", "clock", "clocks");
        noun("minecraft:emerald", "arcade token", "arcade tokens");
        noun("minecraft:map", "tour map", "tour maps");
        noun("minecraft:paper", "shuttle ticket", "shuttle tickets");
        noun("minecraft:book", "book", "books");
        noun("minecraft:wheat", "bundle of wheat", "bundles of wheat");
        noun("minecraft:writable_book", "bank ledger", "bank ledgers");
        noun("farmersdelight:wheat_dough", "ball of dough", "balls of dough");
    }

    private static void noun(String id, String one, String many) {
        NOUN.put(id, new String[]{one, many});
    }

    public static String count(String id, int n) {
        String[] nn = NOUN.get(id);
        if (nn == null) {
            String l = label(id).replaceFirst("^(a|an|some) ", "");
            return n + " " + l;
        }
        return (n == 1 ? (startsVowel(nn[0]) ? "an " : "a ") + nn[0] : n + " " + nn[1]);
    }

    public static String plural(String id) {
        String[] nn = NOUN.get(id);
        return nn == null ? label(id).replaceFirst("^(a|an|some) ", "") : nn[1];
    }

    private static boolean startsVowel(String s) {
        return !s.isEmpty() && "aeiouAEIOU".indexOf(s.charAt(0)) >= 0;
    }

    public static String join(List<String> parts) {
        if (parts.isEmpty()) return "";
        if (parts.size() == 1) return parts.get(0);
        return String.join(", ", parts.subList(0, parts.size() - 1)) + " and " + parts.get(parts.size() - 1);
    }

    public static int price(String id) {
        Food f = FOOD.get(id);
        if (f != null) return f.price();
        return switch (id) {
            case "create:cogwheel", "create:iron_sheet", "minecraft:iron_ingot" -> 3;
            case "minecraft:book", "minecraft:clock" -> 4;
            case "minecraft:emerald", "minecraft:paper", "minecraft:map" -> 2;
            default -> 2;
        };
    }

    public static int wage(CityData.Profile p) {
        int base = switch (prestige(p.job)) { case 3 -> 20; case 2 -> 16; default -> 13; };
        return base + 3 * Math.max(0, p.level - 1);
    }

    public static String rentAccount(CityData.Profile p) {
        return p.livesOnIsland() ? "biz:rent_isle" : "biz:rent_city";
    }

    public static int rent(CityData.Profile p) {
        return p.livesOnIsland() ? 12 : 10;
    }

    public static String business(Job j) {
        return "biz:" + j.workKey;
    }

    public static boolean isFood(String id) {
        return FOOD.containsKey(id);
    }

    public static ItemStack stack(String id) {
        Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        if (it == null || it == Items.AIR) it = Items.PAPER;
        return new ItemStack(it);
    }

    public static List<String> products(Job j) {
        return switch (j) {
            case BAKER -> List.of("minecraft:bread", "minecraft:bread", "minecraft:cookie", "minecraft:pumpkin_pie", "farmersdelight:apple_pie", "minecraft:cake");
            case COOK -> List.of("minecraft:cooked_beef", "minecraft:cooked_beef", "minecraft:baked_potato", "farmersdelight:hamburger", "farmersdelight:steak_and_potatoes");
            case NOODLE_CHEF -> List.of("minecraft:mushroom_stew", "farmersdelight:noodle_soup", "farmersdelight:vegetable_noodles", "farmersdelight:fried_rice", "farmersdelight:dumplings");
            case GROCER -> List.of("minecraft:carrot", "minecraft:apple", "minecraft:apple");
            case DOCKMASTER -> List.of("minecraft:cooked_cod");
            case CLERK -> List.of("create:cogwheel");
            case MECHANIC -> List.of("minecraft:iron_ingot");
            case FACTORY_WORKER -> List.of("create:iron_sheet");
            case QUARRY_WORKER -> List.of("minecraft:flint");
            case CRANE_OPERATOR -> List.of("minecraft:chain");
            case GARDENER -> List.of("minecraft:poppy", "minecraft:dandelion");
            case ATTENDANT -> List.of("minecraft:bucket");
            case CLOCKKEEPER -> List.of("minecraft:clock");
            case ARCADE_KEEPER -> List.of("minecraft:emerald");
            case GUIDE -> List.of("minecraft:map");
            case PILOT -> List.of("minecraft:paper");
            case LIBRARIAN -> List.of("minecraft:book");
            case BANKER -> List.of("minecraft:writable_book");
            case POSTMAN -> List.of("minecraft:paper");
            case MUSICIAN -> List.of("minecraft:note_block");
            case RECEPTIONIST -> List.of("minecraft:tripwire_hook");
            case POLICE -> List.of("minecraft:paper");
            case FIREFIGHTER -> List.of("minecraft:bucket");
            case REPAIR -> List.of("minecraft:bricks");
        };
    }

    public static boolean sellsFood(Job j) {
        return isFood(products(j).get(0));
    }

    public static List<String> wants(Job j) {
        return switch (j) {
            case MECHANIC -> List.of("create:iron_sheet", "create:cogwheel");
            case FACTORY_WORKER -> List.of("minecraft:iron_ingot");
            case CLERK -> List.of("create:iron_sheet", "minecraft:iron_ingot");
            case CLOCKKEEPER -> List.of("create:cogwheel");
            case GARDENER -> List.of("minecraft:bucket");
            case QUARRY_WORKER -> List.of("minecraft:iron_ingot", "minecraft:chain");
            case CRANE_OPERATOR -> List.of("minecraft:chain", "create:iron_sheet");
            case ARCADE_KEEPER -> List.of("create:cogwheel");
            case PILOT -> List.of("minecraft:map", "minecraft:clock");
            case GUIDE -> List.of("minecraft:paper");
            case DOCKMASTER -> List.of("minecraft:chain");
            case ATTENDANT -> List.of("minecraft:iron_ingot");
            default -> List.of("minecraft:emerald");
        };
    }

    public static int prestige(Job j) {
        return switch (j) {
            case PILOT, CLOCKKEEPER, GUIDE, BANKER -> 3;
            case MECHANIC, COOK, BAKER, DOCKMASTER, NOODLE_CHEF, LIBRARIAN, POSTMAN, MUSICIAN -> 2;
            default -> 1;
        };
    }

    public static String bestFood(CityData.Profile p) {
        String best = null;
        int restore = -1;
        for (String k : p.inv.keySet()) {
            Food f = FOOD.get(k);
            if (f != null && f.restore() > restore) { best = k; restore = f.restore(); }
        }
        return best;
    }

    public static int foodCount(CityData.Profile p) {
        int n = 0;
        for (Map.Entry<String, Integer> e : p.inv.entrySet()) if (isFood(e.getKey())) n += e.getValue();
        return n;
    }

    public static String surplus(CityData.Profile p, List<String> wanted) {
        for (String w : wanted) if (p.count(w) > 0) return w;
        return null;
    }

    public static String anyNonFood(CityData.Profile p) {
        for (String k : p.inv.keySet()) if (!isFood(k)) return k;
        return null;
    }

    public static int compat(Trait a, Trait b) {
        if (a.ordinal() > b.ordinal()) { Trait t = a; a = b; b = t; }
        if (a == b) return a == Trait.TALKATIVE ? 0 : 1;
        return switch (a.name() + "+" + b.name()) {
            case "CHEERFUL+GRUMPY" -> -1;
            case "CHEERFUL+FRIENDLY" -> 2;
            case "CHEERFUL+TALKATIVE" -> 1;
            case "CHEERFUL+SHY" -> 1;
            case "SHY+TALKATIVE" -> -1;
            case "SHY+DREAMY" -> 1;
            case "SHY+FRIENDLY" -> 1;
            case "CURIOUS+ADVENTUROUS" -> 2;
            case "CURIOUS+DREAMY" -> 2;
            case "CURIOUS+GRUMPY" -> -1;
            case "GRUMPY+ADVENTUROUS" -> 0;
            case "GRUMPY+TALKATIVE" -> -2;
            case "GRUMPY+DREAMY" -> -1;
            case "ADVENTUROUS+DREAMY" -> 1;
            case "ADVENTUROUS+FRIENDLY" -> 1;
            case "FRIENDLY+TALKATIVE" -> 1;
            default -> 0;
        };
    }
}
