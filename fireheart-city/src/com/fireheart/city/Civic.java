package com.fireheart.city;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/** Saved state for city-wide life: lottery, elections, mail, favours and shows. */
public final class Civic {
    public final Map<String, Integer> tickets = new LinkedHashMap<>();
    public long ticketWeek = -1;
    public long drawDay = -1;
    public String lastWinner = "";
    public int lastPrize;
    public long lastWinDay = -100;
    public int draws;

    public String mayor = "";
    public String policy = "";
    public long electedDay = -100;
    public long announceWeek = -1;
    public long electionWeek = -1;
    public long speechDay = -1;
    public final List<String> candidates = new ArrayList<>();
    public final Map<String, String> votes = new LinkedHashMap<>();
    public final Map<String, Integer> lastTally = new LinkedHashMap<>();
    public final Map<String, Integer> terms = new LinkedHashMap<>();

    public long showDay = -1;
    public long festivalDay = -1;

    public final List<Post.Letter> mail = new ArrayList<>();
    public int nextLetter = 1;
    public long mailDay = -1;
    public long playerMailDay = -1;
    public int delivered;

    public final Map<String, Favours.Request> favours = new LinkedHashMap<>();
    public int favoursDone;
    public final Map<String, Integer> helped = new LinkedHashMap<>();

    public static String foodPayer(CityData d, String buyer, long day) {
        return "lunch".equals(d.civic.policy) && Calendar.weekday(day) == 4 ? CityData.CITY : buyer;
    }

    public long week(long day) {
        return Math.floorDiv(day, 7L);
    }

    CompoundTag save() {
        CompoundTag t = new CompoundTag();
        CompoundTag tk = new CompoundTag();
        tickets.forEach(tk::putInt);
        t.put("tickets", tk);
        t.putLong("ticketWeek", ticketWeek);
        t.putLong("drawDay", drawDay);
        t.putString("lastWinner", lastWinner);
        t.putInt("lastPrize", lastPrize);
        t.putLong("lastWinDay", lastWinDay);
        t.putInt("draws", draws);
        t.putString("mayor", mayor);
        t.putString("policy", policy);
        t.putLong("electedDay", electedDay);
        t.putLong("announceWeek", announceWeek);
        t.putLong("electionWeek", electionWeek);
        t.putLong("speechDay", speechDay);
        ListTag cd = new ListTag();
        for (String c : candidates) cd.add(StringTag.valueOf(c));
        t.put("candidates", cd);
        CompoundTag vt = new CompoundTag();
        votes.forEach(vt::putString);
        t.put("votes", vt);
        CompoundTag lt = new CompoundTag();
        lastTally.forEach(lt::putInt);
        t.put("tally", lt);
        CompoundTag tm = new CompoundTag();
        terms.forEach(tm::putInt);
        t.put("terms", tm);
        t.putLong("showDay", showDay);
        t.putLong("festivalDay", festivalDay);
        ListTag ml = new ListTag();
        for (Post.Letter l : mail) ml.add(l.save());
        t.put("mail", ml);
        t.putInt("nextLetter", nextLetter);
        t.putLong("mailDay", mailDay);
        t.putLong("playerMailDay", playerMailDay);
        t.putInt("delivered", delivered);
        CompoundTag fv = new CompoundTag();
        favours.forEach((k, v) -> fv.put(k, v.save()));
        t.put("favours", fv);
        t.putInt("favoursDone", favoursDone);
        CompoundTag hp = new CompoundTag();
        helped.forEach(hp::putInt);
        t.put("helped", hp);
        return t;
    }

    static Civic load(CompoundTag t) {
        Civic c = new Civic();
        CompoundTag tk = t.getCompound("tickets");
        for (String k : tk.getAllKeys()) c.tickets.put(k, tk.getInt(k));
        c.ticketWeek = t.contains("ticketWeek") ? t.getLong("ticketWeek") : -1;
        c.drawDay = t.contains("drawDay") ? t.getLong("drawDay") : -1;
        c.lastWinner = t.getString("lastWinner");
        c.lastPrize = t.getInt("lastPrize");
        c.lastWinDay = t.contains("lastWinDay") ? t.getLong("lastWinDay") : -100;
        c.draws = t.getInt("draws");
        c.mayor = t.getString("mayor");
        c.policy = t.getString("policy");
        c.electedDay = t.contains("electedDay") ? t.getLong("electedDay") : -100;
        c.announceWeek = t.contains("announceWeek") ? t.getLong("announceWeek") : -1;
        c.electionWeek = t.contains("electionWeek") ? t.getLong("electionWeek") : -1;
        c.speechDay = t.contains("speechDay") ? t.getLong("speechDay") : -1;
        for (Tag x : t.getList("candidates", Tag.TAG_STRING)) c.candidates.add(x.getAsString());
        CompoundTag vt = t.getCompound("votes");
        for (String k : vt.getAllKeys()) c.votes.put(k, vt.getString(k));
        CompoundTag lt = t.getCompound("tally");
        for (String k : lt.getAllKeys()) c.lastTally.put(k, lt.getInt(k));
        CompoundTag tm = t.getCompound("terms");
        for (String k : tm.getAllKeys()) c.terms.put(k, tm.getInt(k));
        c.showDay = t.contains("showDay") ? t.getLong("showDay") : -1;
        c.festivalDay = t.contains("festivalDay") ? t.getLong("festivalDay") : -1;
        for (Tag x : t.getList("mail", Tag.TAG_COMPOUND)) c.mail.add(Post.Letter.load((CompoundTag) x));
        c.nextLetter = Math.max(1, t.getInt("nextLetter"));
        c.mailDay = t.contains("mailDay") ? t.getLong("mailDay") : -1;
        c.playerMailDay = t.contains("playerMailDay") ? t.getLong("playerMailDay") : -1;
        c.delivered = t.getInt("delivered");
        CompoundTag fv = t.getCompound("favours");
        for (String k : fv.getAllKeys()) c.favours.put(k, Favours.Request.load(fv.getCompound(k)));
        c.favoursDone = t.getInt("favoursDone");
        CompoundTag hp = t.getCompound("helped");
        for (String k : hp.getAllKeys()) c.helped.put(k, hp.getInt(k));
        return c;
    }
}
