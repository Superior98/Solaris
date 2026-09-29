package com.fireheart.city;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Sky and calendar flavour: seasons, rainbows after rain, shooting stars, Lantern Night and Kindness Day. */
public final class Skies {
    private Skies() {}

    static final String[] SEASONS = {"§a🌱 Spring", "§e☀ Summer", "§6🍂 Autumn", "§b❄ Winter"};
    static final String[] SEASON_NAMES = {"spring", "summer", "autumn", "winter"};

    public static int seasonIndex(long day) {
        return (int) Math.floorMod(Math.floorDiv(day, 7L), 4L);
    }

    public static String season(long day) {
        return SEASONS[seasonIndex(day)] + "§7";
    }

    public static String seasonName(long day) {
        return SEASON_NAMES[seasonIndex(day)];
    }

    public static boolean lanternNight(long day) {
        return Math.floorMod(day, 28L) == 10;
    }

    public static boolean kindnessDay(long day) {
        return Math.floorMod(day, 28L) == 20;
    }

    public static boolean meteorNight(long day) {
        return Math.floorMod(day, 14L) == 6;
    }

    public static String holiday(long day) {
        if (lanternNight(day)) return "Lantern Night §7- lanterns fly from Solaris Plaza at sunset";
        if (kindnessDay(day)) return "Kindness Day §7- everyone's handing out gifts and compliments";
        if (meteorNight(day)) return "a Meteor Shower night §7- look up after dark";
        return null;
    }

    static List<Resident> residents(ServerLevel sl, CityData d) {
        List<Resident> out = new ArrayList<>();
        for (CityData.Profile p : d.profiles.values()) if (p.entity != null && sl.getEntity(p.entity) instanceof Resident r) out.add(r);
        return out;
    }

    static boolean outside(ServerPlayer pl) {
        return pl.level().canSeeSky(pl.blockPosition().above());
    }

    public static void tick(ServerLevel sl, CityData d) {
        long gt = sl.getGameTime();
        if (gt % 20 == 5) rainbowCheck(sl, d);
        if (rainbow > 0) rainbowTick(sl, d);
        if (gt % 20 == 9) starCheck(sl, d);
        if (!stars.isEmpty()) starTick(sl);
        if (gt % 10 == 3) seasonTick(sl);
        long day = Calendar.worldDay(sl);
        if (lanternNight(day)) lanternTick(sl, d, day);
        else if (!lanterns.isEmpty()) lanterns.clear();
        if (kindnessDay(day) && gt % 40 == 21) kindnessTick(sl, d, day);
        if (gt % 10 == 7 && seasonIndex(day) == 3) auroraTick(sl, d);
        chimeTick(sl);
        if (gt % 20 == 11) mistTick(sl);
    }

    /* ------------------------------------------------------------ Aurora */

    static final DustParticleOptions[] AURORA = {
            new DustParticleOptions(new Vector3f(0.2f, 1f, 0.5f), 4f),
            new DustParticleOptions(new Vector3f(0.3f, 0.9f, 0.8f), 4f),
            new DustParticleOptions(new Vector3f(0.6f, 0.3f, 1f), 4f)
    };

    static void auroraTick(ServerLevel sl, CityData d) {
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (tod < 13500 || tod > 22000 || sl.isRaining()) return;
        double t = sl.getGameTime() * 0.01;
        for (ServerPlayer pl : sl.players()) {
            if (!outside(pl)) continue;
            if (sl.getGameTime() % 200 == 7) Perks.unlock(pl, d, "aurora");
            double baseZ = pl.getZ() - 90;
            for (int i = -30; i <= 30; i += 2) {
                double x = pl.getX() + i * 3;
                double z = baseZ + Math.sin(i * 0.15 + t) * 12;
                int band = Math.floorMod(i / 8, AURORA.length);
                for (int h = 0; h < 4; h++) {
                    if (sl.getRandom().nextFloat() > 0.5f) continue;
                    sl.sendParticles(pl, AURORA[band], true, x, 150 + h * 6 + Math.sin(i * 0.3 + t * 2) * 4, z, 1, 1.5, 2.5, 1.5, 0);
                }
            }
        }
    }

    /* ------------------------------------------------------------ Clock tower chimes */

    static final BlockPos CLOCK = new BlockPos(-26, 90, 20);
    static int chimesLeft, chimeGap;

    static void chimeTick(ServerLevel sl) {
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (tod % 1000 == 0) {
            int hour = (int) ((tod / 1000 + 6) % 24);
            if (hour >= 7 && hour <= 22) {
                chimesLeft = hour % 12 == 0 ? 12 : hour % 12;
                chimeGap = 0;
            }
        }
        if (chimesLeft <= 0 || --chimeGap > 0) return;
        chimeGap = 24;
        chimesLeft--;
        for (ServerPlayer pl : sl.players()) {
            double dd = pl.distanceToSqr(Vec3.atCenterOf(CLOCK));
            if (dd > 160 * 160) continue;
            float vol = (float) Math.max(0.15, 1.0 - Math.sqrt(dd) / 170.0);
            pl.playNotifySound(SoundEvents.BELL_BLOCK, SoundSource.AMBIENT, vol, 0.6f);
        }
    }

    /* ------------------------------------------------------------ Morning mist */

    static final BlockPos MARINA = new BlockPos(18, 63, 64);

    static void mistTick(ServerLevel sl) {
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (!(tod > 22800 || tod < 1800) || sl.isRaining()) return;
        for (ServerPlayer pl : sl.players()) {
            if (pl.distanceToSqr(Vec3.atCenterOf(MARINA)) > 70 * 70) continue;
            for (int i = 0; i < 6; i++) {
                double x = MARINA.getX() + sl.getRandom().nextGaussian() * 25, z = MARINA.getZ() + sl.getRandom().nextGaussian() * 20;
                sl.sendParticles(pl, ParticleTypes.CLOUD, true, x, MARINA.getY() + 0.6, z, 1, 1.5, 0.1, 1.5, 0.004);
            }
        }
    }

    /* ------------------------------------------------------------ Rainbow */

    static boolean wasRaining;
    static int rainbow;
    static final BlockPos ARC = new BlockPos(-20, 68, -60);
    static final float[][] BANDS = {{1f, 0.1f, 0.1f}, {1f, 0.5f, 0f}, {1f, 0.95f, 0.1f}, {0.2f, 0.9f, 0.2f}, {0.2f, 0.5f, 1f}, {0.3f, 0.2f, 0.8f}, {0.6f, 0.2f, 0.9f}};

    static void rainbowCheck(ServerLevel sl, CityData d) {
        boolean raining = sl.isRaining();
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (wasRaining && !raining && tod < 11000 && rainbow <= 0) {
            rainbow = 1600;
            for (ServerPlayer pl : sl.players()) {
                if (pl.distanceToSqr(Vec3.atCenterOf(ARC)) > 220 * 220) continue;
                Perks.say(pl, "§d🌈 A rainbow over Solaris! §7Look north.");
                if (outside(pl)) Perks.unlock(pl, d, "rainbow");
            }
            int n = 0;
            for (Resident r : residents(sl, d)) {
                if (n >= 4 || !r.isFree() || !r.level().canSeeSky(r.blockPosition().above()) || sl.getRandom().nextFloat() > 0.5f) continue;
                n++;
                r.getLookControl().setLookAt(ARC.getX(), ARC.getY() + 30, ARC.getZ());
                r.gesture(Resident.G_POINT, 60);
                r.say(r.pick("Look! A rainbow!", "Oh wow, a double rainbow! ...No, just the one. Still lovely.", "Rainbow! Somebody take a picture!", "Now that's worth the rain."), 70);
            }
        }
        wasRaining = raining;
    }

    static void rainbowTick(ServerLevel sl, CityData d) {
        rainbow--;
        if (rainbow % 10 != 0 || sl.isRaining()) {
            if (sl.isRaining()) rainbow = 0;
            return;
        }
        float fade = Math.min(1f, rainbow / 400f);
        for (ServerPlayer pl : sl.players()) {
            if (pl.distanceToSqr(Vec3.atCenterOf(ARC)) > 220 * 220) continue;
            for (int b = 0; b < BANDS.length; b++) {
                DustParticleOptions dust = new DustParticleOptions(new Vector3f(BANDS[b][0], BANDS[b][1], BANDS[b][2]), 4f * fade + 0.5f);
                double rad = 48 - b * 1.6;
                for (int a = 0; a <= 36; a++) {
                    if (sl.getRandom().nextFloat() > 0.55f) continue;
                    double ang = Math.PI * a / 36.0;
                    sl.sendParticles(pl, dust, true, ARC.getX() + Math.cos(ang) * rad * 1.4, ARC.getY() + Math.sin(ang) * rad, ARC.getZ(), 1, 0.4, 0.4, 0.4, 0);
                }
            }
        }
    }

    /* ------------------------------------------------------------ Shooting stars */

    static final class Star {
        Vec3 pos;
        final Vec3 vel;
        int life;

        Star(Vec3 pos, Vec3 vel, int life) {
            this.pos = pos;
            this.vel = vel;
            this.life = life;
        }
    }

    static final List<Star> stars = new ArrayList<>();

    static void starCheck(ServerLevel sl, CityData d) {
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        boolean shower = meteorNight(Calendar.worldDay(sl));
        if (tod < 13500 || tod > 22500 || sl.isRaining() || stars.size() > (shower ? 8 : 2)) return;
        RandomSource rnd = sl.getRandom();
        if (rnd.nextInt(shower ? 4 : 45) != 0) return;
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer pl : sl.players()) if (outside(pl)) out.add(pl);
        if (out.isEmpty()) return;
        ServerPlayer pl = out.get(rnd.nextInt(out.size()));
        double a = rnd.nextDouble() * Math.PI * 2;
        Vec3 start = pl.position().add(Math.cos(a) * 60, 70 + rnd.nextInt(30), Math.sin(a) * 60);
        Vec3 vel = new Vec3(-Math.cos(a) * 2.4 + rnd.nextGaussian() * 0.6, -0.9, -Math.sin(a) * 2.4 + rnd.nextGaussian() * 0.6);
        stars.add(new Star(start, vel, 24 + rnd.nextInt(10)));
        for (ServerPlayer p : out) {
            if (p.distanceToSqr(pl) > 120 * 120) continue;
            Perks.unlock(p, d, "wish");
            if (rnd.nextFloat() < (shower ? 0.08f : 0.5f)) Perks.say(p, shower ? "§b✧ §7Another one! The meteor shower is lighting up the sky." : "§b✧ §7A shooting star! §fMake a wish...");
        }
        for (Resident r : residents(sl, d)) {
            if (r.distanceToSqr(pl) > 60 * 60 || r.isSleeping() || !r.isFree() || !r.level().canSeeSky(r.blockPosition().above()) || rnd.nextFloat() > (shower ? 0.1f : 0.4f)) continue;
            r.getLookControl().setLookAt(start.x, start.y, start.z);
            r.gesture(Resident.G_POINT, 40);
            r.say(r.pick("A shooting star! Make a wish!", "Did you see that?! A shooting star!", "Quick, everyone make a wish!", "*closes eyes and wishes*"), 60);
        }
    }

    static void starTick(ServerLevel sl) {
        Iterator<Star> it = stars.iterator();
        while (it.hasNext()) {
            Star s = it.next();
            for (int k = 0; k < 3; k++) {
                Vec3 p = s.pos.add(s.vel.scale(k / 3.0));
                for (ServerPlayer pl : sl.players()) {
                    if (pl.distanceToSqr(p) > 250 * 250) continue;
                    sl.sendParticles(pl, ParticleTypes.END_ROD, true, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
                    if (k == 0) sl.sendParticles(pl, ParticleTypes.FIREWORK, true, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }
            s.pos = s.pos.add(s.vel);
            if (--s.life <= 0) it.remove();
        }
    }

    /* ------------------------------------------------------------ Seasons */

    static void seasonTick(ServerLevel sl) {
        int si = seasonIndex(Calendar.worldDay(sl));
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        ParticleOptions fx = si == 3 && !sl.isRaining() ? ParticleTypes.SNOWFLAKE : si == 0 && tod < 12500 ? ParticleTypes.CHERRY_LEAVES : null;
        if (fx == null) return;
        for (ServerPlayer pl : sl.players()) {
            if (!outside(pl) || pl.getY() > 150 && si == 0) continue;
            sl.sendParticles(pl, fx, false, pl.getX(), pl.getY() + 8, pl.getZ(), si == 3 ? 10 : 3, 10, 3, 10, si == 3 ? 0.02 : 0.0);
        }
    }

    /* ------------------------------------------------------------ Lantern Night */

    static final BlockPos PLAZA = new BlockPos(-20, 71, 30);
    static final List<Star> lanterns = new ArrayList<>();
    static long lanternPlanned = -1;
    static final DustParticleOptions GLOW = new DustParticleOptions(new Vector3f(1f, 0.6f, 0.15f), 1.8f);

    static void lanternTick(ServerLevel sl, CityData d, long day) {
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (tod >= 10400 && tod < 11800 && lanternPlanned != day) {
            lanternPlanned = day;
            List<String> who = new ArrayList<>();
            for (CityData.Profile p : d.profiles.values()) if (!p.livesOnIsland() && p.job != Job.POLICE && p.job != Job.FIREFIGHTER) who.add(p.id);
            if (!who.isEmpty()) d.addPlan(day, "plaza", "lantern", who.toArray(new String[0]));
            for (Resident r : residents(sl, d)) r.replan();
            d.news(day, "Lantern Night! Everyone's gathering at Solaris Plaza at sunset to send up lanterns.");
        }
        RandomSource rnd = sl.getRandom();
        if (tod >= 11800 && tod < 13400 && sl.getGameTime() % 10 == 0 && lanterns.size() < 60) {
            List<Vec3> from = new ArrayList<>();
            for (Resident r : sl.getEntitiesOfClass(Resident.class, new net.minecraft.world.phys.AABB(PLAZA).inflate(14, 6, 14), x -> x.isFree())) from.add(r.position().add(0, 2, 0));
            for (ServerPlayer pl : sl.players()) if (pl.blockPosition().closerThan(PLAZA, 14)) from.add(pl.position().add(0, 2, 0));
            if (!from.isEmpty() && rnd.nextFloat() < 0.6f) {
                Vec3 at = from.get(rnd.nextInt(from.size()));
                lanterns.add(new Star(at, new Vec3(rnd.nextGaussian() * 0.02, 0.07 + rnd.nextDouble() * 0.04, rnd.nextGaussian() * 0.02), 500));
                sl.playSound(null, BlockPos.containing(at), SoundEvents.FIRECHARGE_USE, SoundSource.AMBIENT, 0.15f, 1.8f);
            }
            if (sl.getGameTime() % 80 == 0) {
                for (Resident r : sl.getEntitiesOfClass(Resident.class, new net.minecraft.world.phys.AABB(PLAZA).inflate(14, 6, 14), x -> x.isFree())) {
                    r.showItem("minecraft:lantern", 90);
                    if (rnd.nextFloat() < 0.25f) {
                        r.gesture(Resident.G_CHEER, 40);
                        r.say(r.pick("Make a wish on your lantern!", "Look at them all floating up!", "Happy Lantern Night!", "I wished for... nope, not telling!", "This is my favourite night of the year."), 70);
                    }
                }
            }
        }
        Iterator<Star> it = lanterns.iterator();
        while (it.hasNext()) {
            Star s = it.next();
            s.pos = s.pos.add(s.vel.x + Math.sin((500 - s.life) * 0.05) * 0.01, s.vel.y, s.vel.z);
            if (sl.getGameTime() % 3 == 0) {
                for (ServerPlayer pl : sl.players()) {
                    if (pl.distanceToSqr(s.pos) > 200 * 200) continue;
                    sl.sendParticles(pl, GLOW, true, s.pos.x, s.pos.y, s.pos.z, 2, 0.08, 0.1, 0.08, 0);
                    if (s.life % 12 == 0) sl.sendParticles(pl, ParticleTypes.SMALL_FLAME, true, s.pos.x, s.pos.y - 0.2, s.pos.z, 1, 0.02, 0.02, 0.02, 0);
                }
            }
            if (--s.life <= 0) it.remove();
        }
    }

    /* ------------------------------------------------------------ Kindness Day */

    static final Set<String> kind = new HashSet<>();
    static long kindDay = -1;
    static final String[] GIFTS = {"minecraft:poppy", "minecraft:dandelion", "minecraft:cookie", "minecraft:apple", "minecraft:cornflower", "minecraft:sweet_berries"};

    static void kindnessTick(ServerLevel sl, CityData d, long day) {
        if (kindDay != day) {
            kindDay = day;
            kind.clear();
        }
        RandomSource rnd = sl.getRandom();
        for (Resident r : residents(sl, d)) {
            CityData.Profile p = r.profile();
            if (p == null || kind.contains(p.id) || !r.isFree() || r.isSpeaking() || rnd.nextFloat() > 0.08f) continue;
            for (Resident o : sl.getEntitiesOfClass(Resident.class, r.getBoundingBox().inflate(4), x -> x != r && x.isFree() && x.profile() != null)) {
                if (!r.canSee(o, 4)) continue;
                CityData.Profile op = o.profile();
                kind.add(p.id);
                String gift = GIFTS[rnd.nextInt(GIFTS.length)];
                r.getLookControl().setLookAt(o, 30, 30);
                o.getLookControl().setLookAt(r, 30, 30);
                r.showItem(gift, 50);
                r.gesture(Resident.G_GIVE, 40);
                r.sayTo(r.pick("Happy Kindness Day, " + op.name + "! This is for you.", op.name + ", you make this city better. Here!", "Just because - happy Kindness Day!", "I love your " + r.pick("smile", "style", "laugh", "energy") + ", " + op.name + "!"), 80);
                op.add(gift, 1);
                CityData.Rel a = d.rel(p.id, op.id), b = d.rel(op.id, p.id);
                a.aff = Math.min(100, a.aff + 3);
                b.aff = Math.min(100, b.aff + 3);
                o.particles(ParticleTypes.HEART, 3);
                p.log(r.routineDay()).note("I gave " + op.name + " a little gift for Kindness Day");
                d.setDirty();
                break;
            }
        }
    }
}
