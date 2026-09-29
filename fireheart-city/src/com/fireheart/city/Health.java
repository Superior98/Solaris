package com.fireheart.city;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/** Residents catch colds (more often in winter), stay home coughing, and get better sooner with soup and visits. */
public final class Health {
    private Health() {}

    static final String K = "sickUntil";
    static final java.util.Set<String> SOUPS = java.util.Set.of("minecraft:mushroom_stew", "minecraft:rabbit_stew", "minecraft:beetroot_soup", "minecraft:suspicious_stew",
            "farmersdelight:chicken_soup", "farmersdelight:vegetable_soup", "farmersdelight:noodle_soup", "farmersdelight:pumpkin_soup", "farmersdelight:tomato_sauce");

    public static boolean sick(CityData d, String id, long day) {
        return Perks.parse(d.setting("res:" + id, K, "-1")) >= day;
    }

    public static boolean sick(Resident r) {
        CityData.Profile p = r.profile();
        return p != null && sick(r.data(), p.id, Calendar.dayOf(r.level().getDayTime()));
    }

    static void daily(ServerLevel sl, CityData d, long day) {
        if (d.setting(Finale.CITY, "healthDay", "").equals(String.valueOf(day))) return;
        d.setSetting(Finale.CITY, "healthDay", String.valueOf(day));
        int season = Skies.seasonIndex(day);
        float chance = season == 3 ? 0.04f : season == 2 ? 0.02f : 0.008f;
        int sickNow = 0;
        for (CityData.Profile p : d.profiles.values()) if (sick(d, p.id, day)) sickNow++;
        for (CityData.Profile p : d.profiles.values()) {
            if (sickNow >= 2 || sick(d, p.id, day) || sl.getRandom().nextFloat() > chance) continue;
            if (p.job == Job.POLICE || p.job == Job.FIREFIGHTER || p.job == Job.PILOT) continue;
            long until = day + sl.getRandom().nextInt(2);
            d.setSetting("res:" + p.id, K, String.valueOf(until));
            sickNow++;
            p.mind.intentKind = "rest";
            p.mind.intentDay = day;
            p.mind.intent = "stay in bed and get better";
            d.news(day, p.name + " is off sick with a cold today. Get well soon!");
            p.log(day).note("I woke up with a horrible cold and stayed home");
            for (String fid : d.friendsOf(p.id)) {
                CityData.Profile f = d.profiles.get(fid);
                if (f == null || f.livesOnIsland() != p.livesOnIsland() || sick(d, f.id, day)) continue;
                d.addPlan(day, p.home, "visit", f.id, p.id);
                d.setSetting("res:" + p.id, "soupFrom", f.id);
                break;
            }
            for (Resident r : Crowd.all(sl, d)) if (p.id.equals(r.profileId())) r.replan();
        }
    }

    static void tick(ServerLevel sl, CityData d) {
        long day = Calendar.worldDay(sl);
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (sl.getGameTime() % 200 == 131 && tod > 200 && tod < 3000) daily(sl, d, day);
        if (sl.getGameTime() % 40 != 11) return;
        for (Resident r : Crowd.nearPlayers(sl, d)) {
            CityData.Profile p = r.profile();
            if (p == null || !sick(d, p.id, day) || r.isSleeping() && sl.getRandom().nextFloat() > 0.2f) continue;
            if (sl.getRandom().nextFloat() < 0.25f && !r.isSpeaking()) {
                r.gesture(Resident.G_COUGH, 30);
                r.say(r.pick("*cough cough*", "*sniffle*", "Ugh, my head...", "*ACHOO* ...sorry.", "I feel awful."), 40);
                sl.sendParticles(ParticleTypes.POOF, r.getX(), r.getEyeY(), r.getZ(), 2, 0.1, 0.05, 0.1, 0.01);
            }
            String helper = d.setting("res:" + p.id, "soupFrom", "");
            if (helper.isEmpty()) continue;
            for (Resident f : sl.getEntitiesOfClass(Resident.class, r.getBoundingBox().inflate(4, 2, 4), x -> helper.equals(x.profileId()) && !x.isSleeping())) {
                d.setSetting("res:" + p.id, "soupFrom", "");
                f.getLookControl().setLookAt(r, 30, 30);
                r.getLookControl().setLookAt(f, 30, 30);
                f.showItem("minecraft:mushroom_stew", 80);
                f.gesture(Resident.G_GIVE, 50);
                f.sayTo(f.pick("I brought you soup. Doctor's orders!", "Get well soon, " + p.name + ". Here - soup!", "Homemade soup. You'll be better in no time."), 90);
                r.sayTo(r.pick("You're the best. *sniff*", "Aww, thank you...", "Soup! I love you. *cough*"), 80);
                d.setSetting("res:" + p.id, K, String.valueOf(day));
                CityData.Rel a = d.rel(f.profileId(), p.id), b = d.rel(p.id, f.profileId());
                a.aff = Math.min(100, a.aff + 4);
                b.aff = Math.min(100, b.aff + 6);
                p.log(r.routineDay()).note(f.profile().name + " brought me soup while I was sick");
                break;
            }
        }
    }

    /** A player gave a sick resident some soup: they feel better straight away. */
    public static void onGift(Resident r, CityData.Profile p, String itemId, Player pl) {
        CityData d = r.data();
        long day = Calendar.dayOf(r.level().getDayTime());
        if (!sick(d, p.id, day) || !SOUPS.contains(itemId)) return;
        d.setSetting("res:" + p.id, K, String.valueOf(day - 1));
        r.particles(ParticleTypes.HAPPY_VILLAGER, 8);
        r.sayTo(r.pick("Soup! Oh, " + pl.getName().getString() + ", you're a lifesaver. I feel better already!", "Warm soup when I'm sick... you're the best. *sniff* Thank you!"), 100);
        d.playerRel(p.id, pl.getName().getString()).aff = Math.min(100, d.playerRel(p.id, pl.getName().getString()).aff + 5);
        p.mind.intentKind = "";
        r.replan();
        if (pl instanceof net.minecraft.server.level.ServerPlayer sp) Perks.unlock(sp, d, "nurse");
    }
}
