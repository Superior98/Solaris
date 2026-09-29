package com.fireheart.city;

public final class Cast {
    public record Member(String id, String name, Job job, Trait trait, String home, int skin, String voiceId) {}

    // Voice IDs are ElevenLabs' standard premade voices (present on every account, including free tier).
    private static final String RACHEL = "21m00Tcm4TlvDq8ikWAM";   // calm young female
    private static final String DOMI = "AZnzlk1XvdvUeBnXmlld";     // strong young female
    private static final String BELLA = "EXAVITQu4vr4xnSDxMaL";   // soft young female
    private static final String ELLI = "MF3mGyEYCl7XYWbV9V6O";    // young female
    public static final String SULTRY = "XB0fDUnXU5powFXDhCwa";   // calm, flirtatious female (Ellie)
    private static final String FREYA = "jsCqWAovK2LkecY7zXl4";   // bright young female
    private static final String GRACE = "oWAxZDx7w5VEj9dCyTzz";   // warm female
    private static final String ADAM = "pNInz6obpgDQGcFmaJgB";    // deep male
    private static final String ANTONI = "ErXwobaYiN019PkySvjV";  // well-rounded male
    private static final String ARNOLD = "VR6AewLTigWG4xSOukaG";  // crisp male
    private static final String JOSH = "TxGEqnHWrfWFTfGW9XjX";    // deep young male
    private static final String SAM = "yoZ06aMxZJJ28mfd3POQ";     // raspy male
    private static final String ETHAN = "g5CIjZEefAph4nQFvHAz";   // young male
    private static final String GEORGE = "JBFqnCBsd6RMkjVDRZzb";  // warm british male

    public static final Member[] ALL = {
            new Member("mia", "Mia", Job.BAKER, Trait.CHEERFUL, "apt1A", 22, BELLA),
            new Member("leo", "Leo", Job.COOK, Trait.TALKATIVE, "apt1B", 1, JOSH),
            new Member("ava", "Ava", Job.CLERK, Trait.CURIOUS, "apt2A", 26, FREYA),
            new Member("omar", "Omar", Job.GROCER, Trait.FRIENDLY, "apt2C", 3, ANTONI),
            new Member("rosa", "Rosa", Job.MECHANIC, Trait.GRUMPY, "apt3B", 4, DOMI),
            new Member("ben", "Ben", Job.FACTORY_WORKER, Trait.SHY, "apt3D", 5, ETHAN),
            new Member("kai", "Kai", Job.QUARRY_WORKER, Trait.SHY, "apt4A", 6, SAM),
            new Member("nina", "Nina", Job.CRANE_OPERATOR, Trait.ADVENTUROUS, "apt4C", 20, DOMI),
            new Member("sam", "Sam", Job.DOCKMASTER, Trait.FRIENDLY, "apt5B", 8, ARNOLD),
            new Member("ivy", "Ivy", Job.GARDENER, Trait.DREAMY, "apt5D", 21, GRACE),
            new Member("theo", "Theo", Job.ATTENDANT, Trait.TALKATIVE, "apt1C", 10, ADAM),
            new Member("zara", "Zara", Job.CLOCKKEEPER, Trait.CURIOUS, "apt3A", 25, ELLI),
            new Member("luna", "Luna", Job.NOODLE_CHEF, Trait.CHEERFUL, "pod1", 12, RACHEL),
            new Member("rex", "Rex", Job.ARCADE_KEEPER, Trait.ADVENTUROUS, "pod2", 13, ETHAN),
            new Member("nova", "Nova", Job.GUIDE, Trait.DREAMY, "pod3", 27, FREYA),
            new Member("jet", "Jet", Job.PILOT, Trait.ADVENTUROUS, "pod1", 15, ARNOLD),
            new Member("nell", "Nell", Job.LIBRARIAN, Trait.CURIOUS, "apt2B", 23, RACHEL),
            new Member("hugo", "Hugo", Job.BANKER, Trait.FRIENDLY, "apt5A", 17, GEORGE),
            new Member("pip", "Pip", Job.POSTMAN, Trait.CHEERFUL, "apt4B", 18, ETHAN),
            new Member("remy", "Remy", Job.MUSICIAN, Trait.DREAMY, "apt3C", 19, ELLI),
            new Member("ellie", "Ellie", Job.RECEPTIONIST, Trait.LAIDBACK, "apt4D", 24, SULTRY),
            new Member("dex", "Dex", Job.POLICE, Trait.ADVENTUROUS, "police_bunks", 28, JOSH),
            new Member("kira", "Kira", Job.POLICE, Trait.FRIENDLY, "apt2D", 29, DOMI),
            new Member("bruno", "Bruno", Job.POLICE, Trait.GRUMPY, "police_bunks", 30, ADAM),
            new Member("hank", "Hank", Job.FIREFIGHTER, Trait.CHEERFUL, "fire_bunks", 31, ARNOLD),
            new Member("sofia", "Sofia", Job.FIREFIGHTER, Trait.ADVENTUROUS, "fire_bunks", 32, FREYA),
            new Member("gus", "Gus", Job.REPAIR, Trait.CHEERFUL, "apt5C", 33, GEORGE),
            new Member("marco", "Marco", Job.CONCIERGE, Trait.FRIENDLY, "hotel5", 2, ANTONI),
            new Member("finn", "Finn", Job.GARDENER, Trait.CHEERFUL, "hotel1", 7, ETHAN),
            new Member("priya", "Priya", Job.CLERK, Trait.CURIOUS, "hotel2", 9, GRACE),
            new Member("mateo", "Mateo", Job.DOCKMASTER, Trait.LAIDBACK, "hotel3", 14, JOSH),
    };

    public static boolean nightShift(String residentId) {
        return "dex".equals(residentId) || "bruno".equals(residentId) || "sofia".equals(residentId);
    }

    public static boolean flirty(String residentId) {
        return "ellie".equals(residentId);
    }

    public static boolean isName(String word) {
        String w = word.replaceAll("[^A-Za-z]", "");
        for (Member m : ALL) if (m.name().equals(w)) return true;
        return w.equals("Fireheart") || w.startsWith("Fireheart_") || w.startsWith("StellarFox") || w.startsWith("magmagamer") || w.equals("Solaris");
    }

    public static int skinFor(String residentId, int fallback) {
        for (Member m : ALL) if (m.id().equals(residentId)) return m.skin();
        return fallback;
    }

    public static String voiceFor(String residentId) {
        for (Member m : ALL) if (m.id().equals(residentId)) return m.voiceId();
        return ADAM;
    }
}
