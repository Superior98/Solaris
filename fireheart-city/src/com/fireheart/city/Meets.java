package com.fireheart.city;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * "Meet me at the plaza at 5pm": a resident agrees over chat, text or call, heads to the place ahead of time and waits
 * there. They greet the player on arrival and tag along, text when they get there, and are hurt if stood up.
 */
public final class Meets {
    private Meets() {}

    public static final class Meet {
        public String res, player, place;
        public long at;
        public int state;
        boolean texted, nudged;

        String save() {
            return res + "|" + player + "|" + place + "|" + at + "|" + state;
        }

        static Meet load(String s) {
            String[] p = s.split("\\|");
            if (p.length < 5) return null;
            Meet m = new Meet();
            m.res = p[0];
            m.player = p[1];
            m.place = p[2];
            m.at = Long.parseLong(p[3]);
            m.state = Integer.parseInt(p[4]);
            return m;
        }
    }

    static final List<Meet> ALL = new ArrayList<>();
    static final long EARLY = 900, WAIT = 2600;
    static long nowDT;
    private static boolean loaded;

    public static void reset() {
        ALL.clear();
        loaded = false;
    }

    static void load(CityData d) {
        if (loaded) return;
        loaded = true;
        ALL.clear();
        for (String s : d.meets) {
            try {
                Meet m = Meet.load(s);
                if (m != null) ALL.add(m);
            } catch (RuntimeException ignored) {}
        }
    }

    static void save(CityData d) {
        d.meets.clear();
        for (Meet m : ALL) d.meets.add(m.save());
        d.setDirty();
    }

    public static Meet active(String id) {
        for (Meet m : ALL) if (m.res.equals(id) && m.state < 2 && nowDT >= m.at - EARLY && nowDT <= m.at + WAIT) return m;
        return null;
    }

    public static boolean due(String id) {
        return active(id) != null;
    }

    public static Meet with(String id, String player) {
        for (Meet m : ALL) if (m.res.equals(id) && m.player.equals(player) && m.state < 2 && nowDT <= m.at + WAIT) return m;
        return null;
    }

    public static Meet book(ServerLevel sl, CityData d, CityData.Profile p, String player, Place pl, long at) {
        load(d);
        nowDT = sl.getDayTime();
        ALL.removeIf(m -> m.res.equals(p.id) && m.state < 2 && Math.abs(m.at - at) < 3000);
        Meet m = new Meet();
        m.res = p.id;
        m.player = player;
        m.place = pl.key;
        m.at = at;
        ALL.add(m);
        long day = Calendar.dayOf(at);
        d.addPlan(day, pl.key, "meet", p.id);
        p.mind.remember(p, Calendar.worldDay(sl), (int) Math.floorMod(sl.getDayTime(), 24000L), "plan", "I agreed to meet " + player + " at " + pl.label + " at " + when(at), pl.key, 2, 4, "@" + player);
        save(d);
        Resident r = Phones.entity(sl, p);
        if (r != null) r.replan();
        return m;
    }

    public static void cancel(ServerLevel sl, CityData d, Meet m) {
        m.state = 3;
        d.plans.removeIf(x -> x.what.equals("meet") && x.who.contains(m.res) && x.place.equals(m.place) && x.day == Calendar.dayOf(m.at));
        save(d);
        Resident r = Phones.entity(sl, d.profiles.get(m.res));
        if (r != null) r.replan();
    }

    public static void delay(CityData d, Meet m, long by) {
        m.at += by;
        save(d);
    }

    /** "5pm", "9:30am" or "noon" for a world day-time. */
    public static String when(long dayTime) {
        long tod = Math.floorMod(dayTime, 24000L);
        int h = (int) ((tod / 1000 + 6) % 24);
        int m = (int) (tod % 1000 * 60 / 1000);
        if (h == 12 && m < 5) return "noon";
        if (h == 0 && m < 5) return "midnight";
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return h12 + (m >= 5 ? ":" + String.format("%02d", m / 5 * 5) : "") + (h < 12 ? "am" : "pm");
    }

    public static String relative(long at) {
        long d = at - nowDT;
        if (d < 300) return "now";
        String t = when(at);
        return Calendar.dayOf(at) > Calendar.dayOf(nowDT) ? "tomorrow at " + t : "at " + t;
    }

    public static void tick(ServerLevel sl, CityData d) {
        load(d);
        nowDT = sl.getDayTime();
        boolean dirty = false;
        for (Meet m : ALL) {
            if (m.state >= 2) continue;
            CityData.Profile p = d.profiles.get(m.res);
            Place place = Place.get(m.place);
            if (p == null || place == null) { m.state = 3; dirty = true; continue; }
            if (nowDT > m.at + WAIT) {
                m.state = 3;
                dirty = true;
                long day = Calendar.worldDay(sl);
                d.plans.removeIf(x -> x.what.equals("meet") && x.who.contains(m.res) && x.place.equals(m.place) && x.day == Calendar.dayOf(m.at));
                p.mind.trust.put(m.player, Math.max(-100, p.mind.trustIn(m.player) - 4));
                Mind.playerEvent(d, p, m.player, day, "{P} didn't show up when we were meant to meet at " + place.label, -2, 5);
                Computers.deliver(sl, d, m.player, p.id, Lines.pick(sl.random, "I waited at " + place.label + " for ages... is everything okay? :(", "Guess you couldn't make it. I'm heading home.", "Hey, I was at " + place.label + " but you never came. Next time?"));
                Resident r = Phones.entity(sl, p);
                if (r != null) { r.replan(); r.gesture(Resident.G_SAD, 60); }
                continue;
            }
            if (nowDT < m.at - EARLY) continue;
            Resident r = Phones.entity(sl, p);
            if (r == null) continue;
            ServerPlayer pl = sl.getServer().getPlayerList().getPlayerByName(m.player);
            boolean there = r.blockPosition().closerThan(place.pos, 9);
            if (pl != null && pl.level() == sl && r.distanceTo(pl) < 6 && (there || nowDT >= m.at - 200)) {
                m.state = 2;
                dirty = true;
                d.plans.removeIf(x -> x.what.equals("meet") && x.who.contains(m.res) && x.place.equals(m.place) && x.day == Calendar.dayOf(m.at));
                r.getLookControl().setLookAt(pl, 30, 30);
                r.gesture(nowDT > m.at + 600 ? Resident.G_WAVE : Resident.G_CHEER, 50);
                r.particles(ParticleTypes.HEART, 3);
                boolean late = nowDT > m.at + 600;
                r.sayTo(late ? Lines.pick(sl.random, "There you are! I was starting to worry.", "Finally! Fashionably late, " + m.player + "?", "You made it! Better late than never ☺") : Lines.pick(sl.random, "Hey " + m.player + "! Right on time ☺", "There you are! So, what's the plan?", m.player + "! Perfect timing.", "Hi! I've been looking forward to this."), 100);
                p.mind.trust.put(m.player, Math.min(100, p.mind.trustIn(m.player) + 3));
                Mind.playerEvent(d, p, m.player, Calendar.worldDay(sl), "I met up with {P} at " + place.label + " like we planned", 3, 5);
                r.replan();
                r.follow(pl, 3600);
                continue;
            }
            if (!there) continue;
            if (!m.texted && nowDT >= m.at - 100) {
                m.texted = true;
                if (pl == null || r.distanceTo(pl) > 30) Computers.deliver(sl, d, m.player, p.id, Lines.pick(sl.random, "I'm at " + place.label + "! ☺", "Here! Where are you?", "Made it to " + place.label + ". See you soon!"));
            }
            if (!m.nudged && nowDT >= m.at + 1300) {
                m.nudged = true;
                if (pl == null || r.distanceTo(pl) > 30) Computers.deliver(sl, d, m.player, p.id, Lines.pick(sl.random, "Still waiting at " + place.label + "... you coming?", "Hellooo? I'm still here :)", "Did you forget about me? ;("));
            }
            if (r.isFree() && sl.random.nextFloat() < 0.12f && !r.speaking()) {
                switch (sl.random.nextInt(5)) {
                    case 0 -> { r.gesture(Resident.G_STRETCH, 40); }
                    case 1 -> { r.usePhone(1, 60, "checking the time", null); }
                    case 2 -> { r.say(Lines.pick(sl.random, "*checks the time*", "Where's " + m.player + "?", "Hmm, any minute now...", "*looks around*"), 50); }
                    case 3 -> { r.gesture(Resident.G_HUGSELF, 40); }
                    default -> r.getLookControl().setLookAt(r.getX() + sl.random.nextGaussian() * 8, r.getEyeY(), r.getZ() + sl.random.nextGaussian() * 8, 20, 20);
                }
            }
        }
        long cut = nowDT - 48000;
        if (ALL.removeIf(m -> m.state >= 2 && m.at < cut)) dirty = true;
        if (dirty) save(d);
    }

    public static BlockPos waitSpot(Resident r) {
        Meet m = active(r.profileId());
        if (m == null) return null;
        Place p = Place.get(m.place);
        return p == null ? null : p.pos;
    }
}
