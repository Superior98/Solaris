package com.fireheart.city;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;

public final class Place {
    public final String key;
    public final String label;
    public final BlockPos pos;
    public final BlockPos entrance;
    public final boolean island;

    private Place(String key, String label, BlockPos pos, BlockPos entrance) {
        this.key = key;
        this.label = label;
        this.pos = pos;
        this.entrance = entrance;
        this.island = pos.getY() > 150;
    }

    public static final Map<String, Place> ALL = new LinkedHashMap<>();
    public static final BlockPos APT_LOBBY = new BlockPos(21, 71, 24);
    public static final BlockPos CITY_PORT = new BlockPos(-7, 71, 90);
    public static final BlockPos ISLE_PORT = new BlockPos(-11, 181, 236);

    private static void add(String key, String label, int x, int y, int z) {
        ALL.put(key, new Place(key, label, new BlockPos(x, y, z), null));
    }

    private static void add(String key, String label, int x, int y, int z, BlockPos entrance) {
        ALL.put(key, new Place(key, label, new BlockPos(x, y, z), entrance));
    }

    static {
        add("bakery", "the Auto Bakery", -36, 71, 9);
        add("diner", "the Diesel Diner", 7, 71, 53);
        add("supply", "Create Supply Co.", 19, 71, 53);
        add("market", "Green Leaf Market", 31, 71, 53);
        add("garage", "the garage", -2, 71, -16);
        add("factory", "the factory", 2, 71, 4);
        add("gravel", "Solaris Aggregates", -30, 71, 52);
        add("port", "the container port", -25, 71, -21);
        add("marina", "the marina", 22, 64, 62);
        add("fuel", "the fuel station", 19, 71, -17);
        add("plaza", "Solaris Plaza", -20, 71, 30);
        add("park", "the park by the clock tower", -20, 71, 18);
        add("clock", "the clock tower", -26, 71, 20);
        add("pier", "the old pier", -2, 71, 72);
        add("boardwalk", "the marina boardwalk", 9, 64, 67);
        add("skyport", "the Skyport", -7, 71, 90);
        add("isle_pad", "the Neon Heights skyport", -11, 181, 236);
        add("isle_plaza", "the Neon Heights plaza", -4, 181, 261);
        add("noodle", "the Neon Noodle Bar", -26, 181, 268);
        add("arcade", "the Neon Arcade", -25, 181, 283);
        add("memorial", "Benson's memorial", -24, 182, 251);
        add("library", "the Solaris Library", 34, 71, -34, new BlockPos(33, 71, -29));
        add("organ", "the Sky Organ stage", 47, 181, 285);
        add("bank", "the Solaris City Bank", -40, 71, 22, new BlockPos(-34, 71, 22));
        add("atm", "the bank's cash machine", -35, 71, 26);
        add("post", "the Solaris Post Office", 16, 71, -35);
        add("ferry_city", "the Sky Ferry pad", 15, 71, 86);
        add("ferry_isle", "the Sky Ferry terminal", 20, 181, 234);
        add("gardens", "the Sky Gardens", -47, 181, 259);
        add("observatory", "the Neon Heights Observatory", 46, 181, 314);
        add("tour", "Nova's Sky Tour", -4, 181, 261);
        add("statue", "the Founder's statue", -2, 71, -46);
        add("police", "the Solaris Police Station", 56, 71, -36);
        add("fire", "the Solaris Fire Station", 79, 71, -36);
        add("lab", "the Solaris Research Lab", 54, 71, -17);
        add("cinema", "the Solaris Cinema", 82, 71, -17);
        add("tech", "the SolTech store", 35, 71, -1, new BlockPos(30, 71, -1));
        add("reception", "the Ember Heights front desk", 30, 71, 17);
        add("beach_bar", "the Magma Beach Bar", 91, 72, 45);
        add("hotel", "magmagamer9's hotel", 51, 78, 33);
        add("beach", "the beach", 86, 72, 30);
        add("police_bunks", "the police bunkhouse", 67, 71, -46);
        add("fire_bunks", "the fire station bunks", 78, 71, -49);
        String[] letters = {"A", "B", "C", "D"};
        int[][] spots = {{21, 20}, {29, 20}, {21, 29}, {29, 29}};
        int[] ys = {75, 79, 83, 87, 91};
        for (int f = 0; f < ys.length; f++) {
            for (int u = 0; u < 4; u++) {
                String k = "apt" + (f + 1) + letters[u];
                add(k, "apartment " + (f + 1) + letters[u] + " at Ember Heights", spots[u][0], ys[f], spots[u][1]);
            }
        }
        add("pod1", "Pod A-01 on Neon Heights", 15, 181, 260, new BlockPos(10, 181, 260));
        add("pod2", "Pod A-02 on Neon Heights", 18, 181, 275, new BlockPos(13, 181, 275));
        add("pod3", "Pod A-03 on Neon Heights", 9, 181, 290, new BlockPos(9, 181, 285));
    }

    public static Place addDynamic(String key, String label, BlockPos pos) {
        Place p = new Place(key, label, pos, null);
        ALL.put(key, p);
        return p;
    }

    public static Place get(String key) {
        return ALL.get(key);
    }

    public static String label(String key) {
        Place p = key == null ? null : ALL.get(key);
        return p != null ? p.label : key == null ? "somewhere" : key.replace('_', ' ');
    }

    public static final String[] CITY_HANGOUTS = {"plaza", "park", "pier", "boardwalk", "diner", "market", "clock", "library", "tech", "cinema", "beach_bar", "hotel", "beach"};
    public static final String[] ISLE_HANGOUTS = {"isle_plaza", "noodle", "arcade", "memorial", "gardens", "observatory"};
    public static final String[] CITY_INDOOR = {"diner", "market", "supply", "library", "tech"};
    public static final String[] ISLE_INDOOR = {"noodle", "arcade"};
    public static final String[] CITY_FUN = {"pier", "boardwalk", "marina", "plaza"};
    public static final String[] ISLE_FUN = {"arcade", "isle_plaza", "observatory", "gardens"};
    public static final String[] DATE_SPOTS = {"noodle", "arcade", "pier", "boardwalk", "isle_plaza", "plaza", "gardens", "observatory", "cinema"};

    public static boolean indoor(String key) {
        for (String k : CITY_INDOOR) if (k.equals(key)) return true;
        for (String k : ISLE_INDOOR) if (k.equals(key)) return true;
        return key.startsWith("apt") || key.startsWith("pod") || key.equals("bakery") || key.equals("factory") || key.equals("garage") || key.equals("library") || key.equals("bank") || key.equals("post");
    }
}
