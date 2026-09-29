package com.fireheart.city;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class DayLog {
    public long day = -1;
    public final Map<String, Integer> made = new LinkedHashMap<>();
    public int served;
    public int earned;
    public int spent;
    public int tasks;
    public final List<String> notes = new ArrayList<>();
    public final Set<String> done = new LinkedHashSet<>();
    public transient CityData.Profile owner;

    public void make(String item, int n) {
        made.merge(item, n, Integer::sum);
    }

    public void note(String text) {
        if (text == null || text.isEmpty() || notes.contains(text)) return;
        notes.add(text);
        if (owner != null) Mind.fromNote(owner, day, text);
        while (notes.size() > 8) notes.remove(0);
    }

    public boolean once(String key) {
        return done.add(key);
    }

    public boolean empty() {
        return made.isEmpty() && notes.isEmpty() && served == 0 && tasks == 0;
    }

    public String madeText(int max) {
        List<String> parts = new ArrayList<>();
        made.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(max).forEach(e -> parts.add(Economy.count(e.getKey(), e.getValue())));
        return Economy.join(parts);
    }

    public String highlight() {
        for (int i = notes.size() - 1; i >= 0; i--) {
            String n = notes.get(i);
            if (!n.startsWith("I ")) return n;
        }
        if (!made.isEmpty()) return "you made " + madeText(2);
        return notes.isEmpty() ? "" : notes.get(notes.size() - 1);
    }

    public String story(CityData.Profile p, boolean weekend) {
        List<String> bits = new ArrayList<>();
        if (!made.isEmpty()) bits.add((p.job == Job.BAKER ? "I baked " : "I made ") + madeText(3));
        if (served > 0) bits.add("served " + served + (served == 1 ? " customer" : " customers"));
        String extra = null;
        for (int i = notes.size() - 1; i >= 0 && extra == null; i--) if (!notes.get(i).startsWith("I ") || bits.isEmpty()) extra = notes.get(i);
        if (bits.isEmpty() && extra == null) {
            if (weekend) return "Pretty lazy, honestly. Just enjoying the weekend.";
            if (tasks > 0) return "Busy! I got through " + tasks + " jobs at " + p.job.work().label + ".";
            return "Quiet so far. Nothing much happened yet.";
        }
        StringBuilder sb = new StringBuilder();
        if (!bits.isEmpty()) sb.append(Economy.join(bits));
        if (extra != null) {
            if (sb.length() > 0) sb.append(", and ");
            sb.append(extra.startsWith("I ") && sb.length() > 0 ? extra.substring(2) : lower(extra));
        }
        if (earned > 0 && weekendPay(weekend)) sb.append(". Got paid ").append(earned).append(" coins too");
        String out = sb.toString();
        return Events.sentence(out) + (out.endsWith("!") ? "" : "!");
    }

    private static boolean weekendPay(boolean weekend) {
        return !weekend;
    }

    static String lower(String s) {
        if (s.isEmpty()) return s;
        if (s.length() > 1 && Character.isUpperCase(s.charAt(1))) return s;
        String first = s.split(" ")[0];
        if (first.equals("I") || first.startsWith("I'")) return s;
        return Character.isUpperCase(s.charAt(0)) && Cast.isName(first) ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putLong("day", day);
        CompoundTag m = new CompoundTag();
        made.forEach(m::putInt);
        t.put("made", m);
        t.putInt("served", served);
        t.putInt("earned", earned);
        t.putInt("spent", spent);
        t.putInt("tasks", tasks);
        ListTag n = new ListTag();
        for (String s : notes) n.add(StringTag.valueOf(s));
        t.put("notes", n);
        ListTag dn = new ListTag();
        for (String s : done) dn.add(StringTag.valueOf(s));
        t.put("done", dn);
        return t;
    }

    static DayLog load(CompoundTag t) {
        DayLog l = new DayLog();
        l.day = t.getLong("day");
        CompoundTag m = t.getCompound("made");
        for (String k : m.getAllKeys()) l.made.put(k, m.getInt(k));
        l.served = t.getInt("served");
        l.earned = t.getInt("earned");
        l.spent = t.getInt("spent");
        l.tasks = t.getInt("tasks");
        for (Tag s : t.getList("notes", Tag.TAG_STRING)) l.notes.add(s.getAsString());
        for (Tag s : t.getList("done", Tag.TAG_STRING)) l.done.add(s.getAsString());
        return l;
    }
}
