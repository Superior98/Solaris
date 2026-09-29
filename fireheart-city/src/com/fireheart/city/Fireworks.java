package com.fireheart.city;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Firework shows: over the sea by the pier on Saturday nights and festival weeks, over the plaza for big moments. */
public final class Fireworks {
    private Fireworks() {}

    public static final BlockPos PIER_SKY = new BlockPos(-1, 72, 84);
    public static final BlockPos PLAZA_SKY = new BlockPos(-20, 72, 25);
    public static final int SHOW_START = 12300;
    public static final int SHOW_END = 13000;

    private record Shot(long at, double x, double y, double z, int flight, int style) {}

    private static final List<Shot> QUEUE = new ArrayList<>();
    private static final int[] PALETTE = {0xFF3B3B, 0xFFB43B, 0xFFF03B, 0x5BFF6A, 0x3BD4FF, 0x5B6BFF, 0xC23BFF, 0xFF4FD8, 0xFFFFFF, 0xFFD27A};
    public static int launched;

    public static void burst(ServerLevel sl, BlockPos c, int rockets, int ticks, int spread) {
        RandomSource r = sl.random;
        long now = sl.getGameTime();
        for (int i = 0; i < rockets; i++) {
            long at = now + (ticks <= 0 ? 0 : r.nextInt(ticks));
            double x = c.getX() + 0.5 + (r.nextDouble() - 0.5) * spread * 2;
            double z = c.getZ() + 0.5 + (r.nextDouble() - 0.5) * spread * 2;
            QUEUE.add(new Shot(at, x, c.getY(), z, 1 + r.nextInt(3), r.nextInt(5)));
        }
    }

    public static void finale(ServerLevel sl, BlockPos c) {
        long now = sl.getGameTime();
        RandomSource r = sl.random;
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8;
            QUEUE.add(new Shot(now + i * 2, c.getX() + 0.5 + Math.cos(a) * 6, c.getY(), c.getZ() + 0.5 + Math.sin(a) * 6, 2, 1));
        }
        for (int i = 0; i < 10; i++) QUEUE.add(new Shot(now + 40 + r.nextInt(20), c.getX() + 0.5 + r.nextGaussian() * 3, c.getY(), c.getZ() + 0.5 + r.nextGaussian() * 3, 3, 4));
    }

    public static boolean active() {
        return !QUEUE.isEmpty();
    }

    public static void tick(ServerLevel sl, CityData d) {
        long now = sl.getGameTime();
        if (now % 20 == 0) schedule(sl, d);
        if (QUEUE.isEmpty()) return;
        Iterator<Shot> it = QUEUE.iterator();
        int fired = 0;
        while (it.hasNext() && fired < 6) {
            Shot s = it.next();
            if (s.at > now) continue;
            it.remove();
            BlockPos bp = BlockPos.containing(s.x, s.y, s.z);
            if (!sl.isLoaded(bp)) continue;
            FireworkRocketEntity e = new FireworkRocketEntity(sl, s.x, s.y, s.z, rocket(sl.random, s.flight, s.style));
            sl.addFreshEntity(e);
            fired++;
            launched++;
        }
        if (QUEUE.size() > 400) QUEUE.subList(0, QUEUE.size() - 400).clear();
    }

    static void schedule(ServerLevel sl, CityData d) {
        if (d.fireworkMachineBuilt) return;
        long day = Calendar.worldDay(sl);
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (tod < SHOW_START || tod > SHOW_END) return;
        boolean saturday = Calendar.weekday(day) == 5;
        boolean festival = "festival".equals(d.civic.policy);
        if (!saturday && !festival) return;
        if (d.civic.showDay == day) {
            if (tod >= SHOW_END - 40 && d.civic.festivalDay != day) {
                d.civic.festivalDay = day;
                finale(sl, PIER_SKY);
            } else if (QUEUE.size() < 6 && tod < SHOW_END - 60) burst(sl, PIER_SKY, 5, 60, 7);
            return;
        }
        d.civic.showDay = day;
        d.setDirty();
        burst(sl, PIER_SKY, 8, 80, 7);
        d.event(day, "show", festival && !saturday ? "the Festival Week fireworks lit up the sky over the pier" : "the Saturday night fireworks show lit up the pier", PIER_SKY);
        for (net.minecraft.server.level.ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(PIER_SKY.getX(), p.getY(), PIER_SKY.getZ()) < 200 * 200)
                p.displayClientMessage(net.minecraft.network.chat.Component.literal("§d✦ §fFireworks over the pier! §d✦"), true);
        }
    }

    private static long planned = -1;

    public static void planShow(ServerLevel sl, CityData d) {
        long day = Calendar.worldDay(sl);
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (planned == day || tod < 500 || tod > 9500) return;
        planned = day;
        boolean show = Calendar.weekday(day) == 5 || Calendar.weekday(day) == 2 || "festival".equals(d.civic.policy);
        if (!show) return;
        List<String> who = new ArrayList<>();
        for (CityData.Profile p : d.profiles.values()) {
            if (p.livesOnIsland() && Calendar.weekday(day) != 5) continue;
            boolean busy = false;
            for (CityData.Plan pl : d.plansFor(p.id, day)) if (!pl.what.equals("campaign")) busy = true;
            if (!busy && Math.floorMod(p.id.hashCode() + day * 7, 100) < 65) who.add(p.id);
        }
        if (who.isEmpty()) return;
        d.plans.removeIf(pl -> pl.day == day && pl.what.equals("fireworks"));
        d.addPlan(day, "pier", "fireworks", who.toArray(new String[0]));
    }

    public static boolean showing(ServerLevel sl, CityData d) {
        long day = Calendar.worldDay(sl);
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        return d.civic.showDay == day && tod >= SHOW_START && tod <= SHOW_END + 40;
    }

    static ItemStack rocket(RandomSource r, int flight, int style) {
        ItemStack st = new ItemStack(Items.FIREWORK_ROCKET);
        CompoundTag fw = new CompoundTag();
        fw.putByte("Flight", (byte) flight);
        ListTag ex = new ListTag();
        int n = style == 4 ? 2 : 1;
        for (int k = 0; k < n; k++) {
            CompoundTag e = new CompoundTag();
            e.putByte("Type", (byte) (style == 4 ? r.nextInt(5) : style));
            int c1 = PALETTE[r.nextInt(PALETTE.length)], c2 = PALETTE[r.nextInt(PALETTE.length)];
            e.putIntArray("Colors", r.nextBoolean() ? new int[]{c1} : new int[]{c1, c2});
            e.putIntArray("FadeColors", new int[]{PALETTE[r.nextInt(PALETTE.length)]});
            e.putBoolean("Flicker", r.nextFloat() < 0.4f);
            e.putBoolean("Trail", r.nextFloat() < 0.5f);
            ex.add(e);
        }
        fw.put("Explosions", ex);
        st.getOrCreateTag().put("Fireworks", fw);
        return st;
    }
}
