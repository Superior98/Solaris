package com.fireheart.city;

import net.minecraftforge.common.ForgeConfigSpec;

/** config/fireheartcity-common.toml: how chatty phones are, auto-builds and ferry behaviour. */
public final class FhcConfig {
    private FhcConfig() {}

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue TEXT_RATE, CALL_RATE;
    public static final ForgeConfigSpec.BooleanValue AUTO_BUILD, FERRY_STRICT, PHONE_BREAKS, TIME_MOVES;
    public static final ForgeConfigSpec.BooleanValue WEDDINGS, SKY_EFFECTS, CHIMES, SNOWBALLS, AMBIENT, LUCKY_FINDS, FESTIVALS;
    public static final ForgeConfigSpec.IntValue AMBIENT_RANGE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("phones");
        TEXT_RATE = b.comment("How often residents text, post and scroll (1.0 = normal, 0 = never, 2.0 = twice as often)").defineInRange("textRate", 1.0, 0.0, 5.0);
        CALL_RATE = b.comment("How often residents phone each other and you (1.0 = normal)").defineInRange("callRate", 1.0, 0.0, 5.0);
        PHONE_BREAKS = b.comment("Residents sometimes crack or lose their phones").define("phonesBreak", true);
        b.pop();
        b.push("world");
        AUTO_BUILD = b.comment("Build the Neon Heights expansion and the SolTech store automatically").define("autoBuild", true);
        TIME_MOVES = b.comment("Keep the day/night cycle running (clocks, watches and resident routines need it)").define("timeMoves", true);
        FERRY_STRICT = b.comment("Residents always ride the Sky Ferry instead of skipping the trip when no player is near").define("ferryStrict", false);
        b.pop();
        b.push("life");
        AMBIENT = b.comment("Everyday resident moments (picnics, card games, jokes, selfies, snowball fights...)").define("ambientMoments", true);
        AMBIENT_RANGE = b.comment("Only residents within this many blocks of a player run ambient moments (saves CPU)").defineInRange("ambientRange", 96, 16, 512);
        WEDDINGS = b.comment("Resident couples get married at Solaris Plaza").define("weddings", true);
        FESTIVALS = b.comment("Seasonal festivals and city events (Lantern Night, Kindness Day, quiz, karaoke, fun run...)").define("festivals", true);
        SKY_EFFECTS = b.comment("Rainbows, shooting stars, aurora, fireflies, constellations and seasonal particles").define("skyEffects", true);
        CHIMES = b.comment("The clock tower rings the hour").define("chimes", true);
        SNOWBALLS = b.comment("Residents throw snowballs in winter").define("snowballs", true);
        LUCKY_FINDS = b.comment("Breaking grass and flowers in the city can turn up small finds").define("luckyFinds", true);
        b.pop();
        SPEC = b.build();
    }

    static double text() {
        try { return TEXT_RATE.get(); } catch (Throwable t) { return 1.0; }
    }

    static double call() {
        try { return CALL_RATE.get(); } catch (Throwable t) { return 1.0; }
    }

    static boolean autoBuild() {
        try { return AUTO_BUILD.get(); } catch (Throwable t) { return true; }
    }

    static boolean ferryStrict() {
        try { return FERRY_STRICT.get(); } catch (Throwable t) { return false; }
    }

    static boolean timeMoves() {
        try { return TIME_MOVES.get(); } catch (Throwable t) { return true; }
    }

    static boolean flag(ForgeConfigSpec.BooleanValue v) {
        try { return v.get(); } catch (Throwable t) { return true; }
    }

    static boolean ambient() { return flag(AMBIENT); }

    static boolean weddings() { return flag(WEDDINGS); }

    static boolean festivals() { return flag(FESTIVALS); }

    static boolean skyEffects() { return flag(SKY_EFFECTS); }

    static boolean chimes() { return flag(CHIMES); }

    static boolean snowballs() { return flag(SNOWBALLS); }

    static boolean luckyFinds() { return flag(LUCKY_FINDS); }

    static int ambientRange() {
        try { return AMBIENT_RANGE.get(); } catch (Throwable t) { return 96; }
    }

    static boolean phonesBreak() {
        try { return PHONE_BREAKS.get(); } catch (Throwable t) { return true; }
    }
}
