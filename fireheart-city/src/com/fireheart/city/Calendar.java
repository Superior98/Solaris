package com.fireheart.city;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class Calendar {
    public static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    private Calendar() {}

    public static int weekday(long day) {
        return (int) Math.floorMod(day, 7L);
    }

    public static String name(long day) {
        return DAYS[weekday(day)];
    }

    public static boolean weekend(long day) {
        return weekday(day) >= 5;
    }

    public static String stamp(long day) {
        return name(day) + ", Day " + (day + 1);
    }

    /** Days added when /time set moved the clock backwards, so the city's calendar never jumps back. */
    public static long OFFSET;

    public static long dayOf(long dayTime) {
        return Math.floorDiv(dayTime + OFFSET, 24000L);
    }

    public static long day(ServerLevel sl) {
        return dayOf(sl.getDayTime() + 1500L);
    }

    public static long worldDay(ServerLevel sl) {
        return dayOf(sl.getDayTime());
    }

    private static boolean healed;

    /** Once per start: if anything remembered is dated after today (old clock resets), move today past it. */
    static void heal(ServerLevel sl, CityData d) {
        healed = true;
        long max = Long.MIN_VALUE;
        for (CityData.Event e : d.events) max = Math.max(max, e.day);
        for (CityData.Profile p : d.profiles.values()) for (Mind.Ep e : p.mind.eps) max = Math.max(max, e.day);
        max = Math.max(max, d.maxChatDay());
        OFFSET = d.dayOffset;
        long today = worldDay(sl);
        if (max != Long.MIN_VALUE && max > today) {
            d.dayOffset += (max - today) * 24000L;
            OFFSET = d.dayOffset;
            d.setDirty();
            FireheartCity.LOG.info("Solaris calendar repaired: memories went up to day " + max + ", today is now day " + worldDay(sl));
        }
    }

    public static void resetHeal() {
        healed = false;
    }

    /** Keeps the day count going forward when someone uses /time set to go back in time. */
    public static void track(ServerLevel sl, CityData d) {
        if (!healed) heal(sl, d);
        long dt = sl.getDayTime();
        if (d.lastDayTime >= 0 && dt < d.lastDayTime - 20) {
            long lost = Math.floorDiv(d.lastDayTime, 24000L) - Math.floorDiv(dt, 24000L);
            if (lost > 0) {
                d.dayOffset += lost * 24000L;
                FireheartCity.LOG.info("Clock moved back - the Solaris calendar keeps counting (offset " + d.dayOffset / 24000L + " days)");
            }
            if (Math.floorMod(dt, 24000L) < Math.floorMod(d.lastDayTime, 24000L)) {
                d.dayOffset += 24000L;
                FireheartCity.LOG.info("Time set to earlier in the day - treating it as the next day");
            }
            d.setDirty();
        }
        d.lastDayTime = dt;
        OFFSET = d.dayOffset;
    }

    public static String relative(long when, long today) {
        long diff = today - when;
        if (diff < 0) return "the other day";
        if (diff == 0) return "today";
        if (diff == 1) return "yesterday";
        if (diff < 7) return "on " + name(when);
        return "last week";
    }

    public static String clock(long dayTime) {
        long tod = Math.floorMod(dayTime, 24000L);
        int h = (int) ((tod / 1000 + 6) % 24);
        int m = (int) (tod % 1000 * 60 / 1000);
        return String.format("%02d:%02d", h, m);
    }

    public static void tick(ServerLevel sl, CityData d) {
        long day = worldDay(sl);
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (d.announcedDay == day || tod > 3000) return;
        d.announcedDay = day;
        d.setDirty();
        String sub = switch (weekday(day)) {
            case 0 -> "A fresh week - back to work, Solaris!";
            case 4 -> "Last workday of the week!";
            case 5 -> "Weekend! Everyone has the day off.";
            case 6 -> "Weekend! Rent is due at Ember Heights today.";
            default -> "Shops and workplaces are open.";
        };
        for (ServerPlayer p : sl.players()) show(p, day, sub);
    }

    public static void banner(ServerPlayer p, String title, String sub) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(15, 80, 25));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(sub)));
    }

    public static void show(ServerPlayer p, long day, String sub) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(15, 70, 25));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§6☀ " + name(day))));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§eDay " + (day + 1) + " §7· " + sub)));
    }
}
