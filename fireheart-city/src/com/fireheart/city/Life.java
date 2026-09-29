package com.fireheart.city;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;

/** Once-a-day city life: birthdays with parties, couple anniversaries. */
public final class Life {
    private Life() {}

    public static void daily(ServerLevel sl, CityData d) {
        long day = Calendar.dayOf(sl.getDayTime());
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (tod < 500 || tod > 10000) return;
        for (CityData.Profile p : d.profiles.values()) {
            if (p.lastBirthday != day && Math.floorMod(day, 16) == p.birthdayIndex()) {
                p.lastBirthday = day;
                String spot = p.livesOnIsland() ? "isle_plaza" : "plaza";
                List<String> who = new ArrayList<>();
                who.add(p.id);
                for (String f : d.friendsOf(p.id)) if (!who.contains(f)) who.add(f);
                if (!p.partner.isEmpty() && !who.contains(p.partner)) who.add(p.partner);
                CityData.Event e = d.event(day, "birthday", "it's " + p.name + "'s birthday today" + (who.size() > 1 ? " - party at " + Place.label(spot) + " this evening" : ""), Place.get(spot).pos, who.toArray(new String[0]));
                for (CityData.Plan pl : d.plans) if (pl.day == day && pl.who.size() > 2 && !pl.what.equals("party") && !pl.who.get(0).equals(p.id)) pl.who.remove(p.id);
                d.plans.removeIf(pl -> pl.day == day && pl.who.contains(p.id) && !pl.what.equals("party") && pl.who.size() <= 2);
                d.addPlan(day, spot, "party", who.toArray(new String[0]));
                for (String f : who) {
                    if (f.equals(p.id) || !d.profiles.containsKey(f)) continue;
                    Post.send(d, p.id, f, "Dear " + d.profiles.get(f).name + ",\n\nIt's my birthday today! Come to my party at " + Place.label(spot) + " this evening after work. There will be cake!\n\nSee you there,\n" + p.name, "invite", day);
                }
                d.setDirty();
            }
            if (!p.partner.isEmpty() && p.partnerSince >= 0 && day > p.partnerSince && (day - p.partnerSince) % 8 == 0 && p.lastAnniv != day) {
                CityData.Profile q = d.profiles.get(p.partner);
                if (q == null) continue;
                p.lastAnniv = day;
                q.lastAnniv = day;
                long weeks = (day - p.partnerSince) / 8;
                d.event(day, "social", p.name + " and " + q.name + " are celebrating " + (weeks == 1 ? "one week" : weeks + " weeks") + " together", null, p.id, q.id);
                boolean busy = false;
                for (CityData.Plan pl : d.plansFor(p.id, day)) if (pl.what.equals("party")) busy = true;
                if (!busy) {
                    String spot = Place.DATE_SPOTS[(int) Math.floorMod(day + p.id.hashCode(), (long) Place.DATE_SPOTS.length)];
                    d.addPlan(day, spot, "date", p.id, q.id);
                }
                d.setDirty();
            }
        }
    }
}
