package com.fireheart.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Ember Heights front desk: the receptionist checks residents in on their way up to their apartments. */
public final class Reception {
    private Reception() {}

    public static final BlockPos DESK = new BlockPos(30, 71, 18);
    public static final BlockPos STAND = new BlockPos(30, 71, 17);
    public static final BlockPos BELL = new BlockPos(30, 72, 18);
    private static final BlockPos[] LINE_SPOTS = {new BlockPos(30, 71, 19), new BlockPos(29, 71, 20), new BlockPos(31, 71, 20), new BlockPos(28, 71, 21), new BlockPos(32, 71, 21), new BlockPos(30, 71, 21)};

    private static final Map<UUID, Long> DONE = new HashMap<>();
    private static final LinkedHashMap<UUID, Long> LINE = new LinkedHashMap<>();
    private static final Map<String, Long> PLAYER_SEEN = new HashMap<>();
    private static final Map<String, Long> KEY_DAY = new HashMap<>();
    private static final Map<Long, Integer> CHECKED = new HashMap<>();
    public static boolean debug = false;
    private static UUID serving;
    private static long servingAt;
    private static int stage;
    private static long idleAt;

    public static void reset() {
        DONE.clear();
        LINE.clear();
        PLAYER_SEEN.clear();
        KEY_DAY.clear();
        CHECKED.clear();
        serving = null;
        stage = 0;
    }

    static int checkedIn(long day) {
        return CHECKED.getOrDefault(day, 0);
    }

    public static Resident clerk(ServerLevel sl) {
        CityData d = CityData.get(sl);
        for (CityData.Profile p : d.profiles.values()) {
            if (p.job != Job.RECEPTIONIST || p.entity == null) continue;
            if (sl.getEntity(p.entity) instanceof Resident r && r.isAlive()) return r;
        }
        return null;
    }

    public static boolean onDuty(Resident r) {
        CityData.Profile p = r == null ? null : r.profile();
        return p != null && p.job == Job.RECEPTIONIST && r.activityName().equals("work") && Elevator.floorOfEntity(r) == 0 && r.convo == null;
    }

    public static boolean deskOpen(Resident r) {
        CityData.Profile p = r == null ? null : r.profile();
        return p != null && p.job == Job.RECEPTIONIST && r.activityName().equals("work") && Elevator.floorOfEntity(r) == 0;
    }

    public static boolean atDesk(Resident r) {
        return horiz(r, STAND) < 1.6;
    }

    public static BlockPos clerkTarget(Resident r) {
        CityData.Profile p = r.profile();
        if (p == null || p.job != Job.RECEPTIONIST || LINE.isEmpty() || !deskOpen(r)) return null;
        return STAND;
    }

    private static boolean headingUp(Resident g) {
        if (Elevator.floorOfEntity(g) != 0) return false;
        Place dest = g.destination();
        return dest != null && dest.island == g.onIsland() && Elevator.floorOfPos(dest.pos) > 0;
    }

    public static boolean busy(Resident r) {
        if (LINE.containsKey(r.getUUID())) return true;
        CityData.Profile p = r.profile();
        if (p == null) return false;
        if (p.job == Job.RECEPTIONIST) return deskOpen(r);
        if (DONE.containsKey(r.getUUID()) || !headingUp(r) || !(r.level() instanceof ServerLevel sl)) return false;
        return deskOpen(clerk(sl));
    }

    public static boolean holding(Resident g) {
        return LINE.containsKey(g.getUUID());
    }

    public static BlockPos guestSpot(Resident g) {
        CityData.Profile p = g.profile();
        if (p == null || p.job == Job.RECEPTIONIST || !(g.level() instanceof ServerLevel sl)) return null;
        UUID id = g.getUUID();
        int floor = Elevator.floorOfEntity(g);
        if (floor < 0) {
            DONE.remove(id);
            LINE.remove(id);
            return null;
        }
        if (id.equals(serving) && !DONE.containsKey(id)) return LINE_SPOTS[0];
        if (DONE.containsKey(id) || g.isFollowing() || !headingUp(g)) {
            LINE.remove(id);
            return null;
        }
        if (!deskOpen(clerk(sl))) {
            LINE.remove(id);
            return null;
        }
        LINE.putIfAbsent(id, sl.getGameTime());
        int idx = 0;
        for (UUID u : LINE.keySet()) {
            if (u.equals(id)) break;
            idx++;
        }
        return LINE_SPOTS[Math.min(idx, LINE_SPOTS.length - 1)];
    }

    public static void tick(ServerLevel sl) {
        long now = sl.getGameTime();
        if (now % 5 != 0) return;
        Resident clerk = clerk(sl);
        if (!deskOpen(clerk)) {
            LINE.clear();
            serving = null;
            stage = 0;
            return;
        }
        if (clerk.convo != null) return;
        LINE.keySet().removeIf(u -> {
            boolean drop = !(sl.getEntity(u) instanceof Resident g) || !g.isAlive() || DONE.containsKey(u) || (u.equals(serving) ? Elevator.floorOfEntity(g) != 0 : !headingUp(g));
            if (drop && u.equals(serving) && debug) FireheartCity.LOG.info("[Desk] drop " + u + " floor=" + (sl.getEntity(u) == null ? "?" : Elevator.floorOfEntity(sl.getEntity(u))) + " dest=" + (sl.getEntity(u) instanceof Resident g2 && g2.destination() != null ? g2.destination().key : "null"));
            return drop;
        });
        if (serving != null && !LINE.containsKey(serving)) {
            serving = null;
            stage = 0;
        }
        LINE.entrySet().removeIf(en -> !en.getKey().equals(serving) && now - en.getValue() > 600 && sl.getEntity(en.getKey()) instanceof Resident g && horiz(g, LINE_SPOTS[0]) > 3.2);
        if (serving == null && atDesk(clerk)) {
            double best = 3.2;
            for (UUID u : LINE.keySet()) {
                Resident g = (Resident) sl.getEntity(u);
                if (g == null || g.convo != null || g.isSleeping()) continue;
                double dd = horiz(g, LINE_SPOTS[0]);
                if (dd < best) {
                    best = dd;
                    serving = u;
                }
            }
            if (serving != null) {
                servingAt = now;
                stage = 0;
            }
        }
        if (serving == null) {
            idle(sl, clerk, now);
            return;
        }
        Resident g = (Resident) sl.getEntity(serving);
        if (g == null || horiz(g, LINE_SPOTS[0]) > 4.5) {
            if (debug) FireheartCity.LOG.info("[Desk] walked off " + (g == null ? "null" : g.profileId() + " " + horiz(g, LINE_SPOTS[0])));
            LINE.remove(serving);
            serving = null;
            return;
        }
        clerk.getNavigation().stop();
        clerk.getLookControl().setLookAt(g, 30, 30);
        g.getLookControl().setLookAt(clerk, 30, 30);
        long t = now - servingAt;
        RandomSource rnd = clerk.getRandom();
        CityData.Profile gp = g.profile(), cp = clerk.profile();
        if (stage == 0) {
            stage = 1;
            clerk.sayLine(greet(clerk, gp, rnd), 100);
            clerk.gesture(Resident.G_WAVE, 30);
        } else if (stage == 1 && t >= 55) {
            stage = 2;
            g.sayLine(guestLine(gp, cp, rnd), 90);
        } else if (stage == 2 && t >= 110) {
            stage = 3;
            clerk.gesture(Resident.G_GIVE, 30);
            clerk.showItem("minecraft:tripwire_hook", 30);
            sl.playSound(null, BELL, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 0.5f, 1.4f);
            clerk.sayLine(handover(gp, rnd), 100);
        } else if (stage == 3 && t >= 150) {
            if (rnd.nextInt(3) == 0) g.sayTo(Lines.pick(rnd, "Thanks, " + cp.name + "!", "Night, " + cp.name + ".", "You're the best, " + cp.name + "."), 40);
            DONE.put(serving, now);
            LINE.remove(serving);
            serving = null;
            stage = 0;
            long day = clerk.routineDay();
            CHECKED.merge(day, 1, Integer::sum);
            if (gp != null) gp.log(g.routineDay()).note(cp.name + " checked me in at the Ember Heights front desk");
            CityData d = CityData.get(sl);
            if (gp != null) {
                CityData.Rel a = d.rel(cp.id, gp.id), b = d.rel(gp.id, cp.id);
                a.met = true;
                b.met = true;
                d.setDirty();
            }
        }
    }

    private static void idle(ServerLevel sl, Resident clerk, long now) {
        if (!atDesk(clerk)) return;
        for (ServerPlayer pl : sl.players()) {
            if (pl.isSpectator() || horiz(pl, LINE_SPOTS[0]) > 4.5 || Math.abs(pl.getY() - STAND.getY()) > 2) continue;
            String pn = pl.getName().getString();
            Long seen = PLAYER_SEEN.get(pn);
            if (seen != null && now - seen < 3600) continue;
            PLAYER_SEEN.put(pn, now);
            clerk.getLookControl().setLookAt(pl, 30, 30);
            clerk.sayTo(Lines.pick(clerk.getRandom(),
                    "Well hey there, " + pn + ". Checking in, or did you just come to see me?",
                    "Oh, it's you. Lobby just got a whole lot nicer.",
                    "Hey " + pn + ". Need a key? Or just a reason to hang around the desk?",
                    "Look who wandered in. Take your time, I'm not going anywhere."), 90);
            clerk.gesture(Resident.G_WAVE, 30);
            idleAt = now;
            return;
        }
        if (now - idleAt > 2400 && clerk.getRandom().nextInt(40) == 0) {
            idleAt = now;
            clerk.say(Lines.pick(clerk.getRandom(), "Quiet in here... I like it.", "*hums along to nothing in particular*", "Another chill shift at Ember Heights.", "If anyone needs me, I'll be right here. Probably.", "*spins a room key around her finger*"), 70);
        }
    }

    private static double horiz(Entity e, BlockPos p) {
        double dx = e.getX() - (p.getX() + 0.5), dz = e.getZ() - (p.getZ() + 0.5);
        return Math.sqrt(dx * dx + dz * dz);
    }

    static String unit(CityData.Profile p) {
        return p == null || p.home == null || !p.home.startsWith("apt") ? "" : p.home.substring(3);
    }

    private static String greet(Resident clerk, CityData.Profile gp, RandomSource rnd) {
        String n = gp == null ? "hon" : gp.name;
        long t = clerk.timeOfDay();
        if (t >= 12500) return Lines.pick(rnd, "Hey " + n + ", welcome back. Long day?", "Evening, " + n + ". No rush, no rush.", "Well look who it is. Home already, " + n + "?", "There you are, " + n + ". I was starting to miss you.");
        return Lines.pick(rnd, "Hey " + n + ". Popping home for a bit?", "Oh hey, " + n + ". Forget something upstairs?", "Hi " + n + ". Lobby's all yours, take it easy.");
    }

    private static String guestLine(CityData.Profile gp, CityData.Profile cp, RandomSource rnd) {
        String c = cp == null ? "Ellie" : cp.name;
        String u = unit(gp);
        if (gp == null) return "Hi " + c + ".";
        return switch (gp.trait) {
            case GRUMPY -> Lines.pick(rnd, "Just my key, " + c + ". Please.", "Long day. Don't ask.");
            case SHY -> Lines.pick(rnd, "H-hi " + c + "... just checking in.", "Um, hi. " + u + ", please.");
            case CHEERFUL, TALKATIVE -> Lines.pick(rnd, c + "! Hi! Checking in!", "Hey " + c + "! You would not believe my day.");
            case DREAMY -> Lines.pick(rnd, "Oh, hi " + c + ". I was miles away.", "Hi " + c + ". The sky looked amazing on the way home.");
            default -> Lines.pick(rnd, "Hey " + c + ", checking in" + (u.isEmpty() ? "." : " - " + u + "."), "Hi " + c + ". Just heading up.");
        };
    }

    private static String handover(CityData.Profile gp, RandomSource rnd) {
        String u = unit(gp);
        List<String> o = new ArrayList<>(List.of(
                (u.isEmpty() ? "Here's your key." : "Apartment " + u + ", same as always.") + " Elevator's all yours.",
                "Key's yours. Don't have too much fun up there.",
                "Checked in. Sleep tight, okay?",
                "All done. See? Painless."));
        if (gp != null && gp.trait == Trait.GRUMPY) o.add("Easy, tiger. Here you go.");
        if (gp != null && gp.trait == Trait.SHY) o.add("Here you go, sweetie. Night.");
        if (gp != null && !gp.partner.isEmpty()) o.add("Say hi to your other half for me.");
        return o.get(rnd.nextInt(o.size()));
    }

    static String chat(ServerPlayer pl, Resident r, String t, RandomSource rnd) {
        CityData.Profile p = r.profile();
        if (p == null || p.job != Job.RECEPTIONIST) return null;
        if (!any(t, " check in ", " checkin ", " check me in ", " checking in ", " room ", " key ", " a room ", " book ", " stay the night ", " hotel ")) return null;
        String pn = pl.getName().getString();
        if (!r.activityName().equals("work")) return Lines.pick(rnd, "Desk's closed right now, cutie. Catch me there after breakfast... ish.", "Off the clock, " + pn + ". But nice try.");
        if (!r.blockPosition().closerThan(STAND, 6)) return "Meet me at the front desk in the Ember Heights lobby and I'll sort you out.";
        long day = r.day();
        Long had = KEY_DAY.get(pn);
        if (had != null && had == day) return Lines.pick(rnd, "You already have a key, silly. Unless you just wanted an excuse to talk to me.", "Checked in already, " + pn + ". But I don't mind you coming back.");
        KEY_DAY.put(pn, day);
        ItemStack key = new ItemStack(Items.TRIPWIRE_HOOK);
        key.setHoverName(Component.literal("Ember Heights Room Key"));
        if (!pl.getInventory().add(key)) pl.drop(key, false);
        ServerLevel sl = pl.serverLevel();
        sl.playSound(null, BELL, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 0.5f, 1.4f);
        r.gesture(Resident.G_GIVE, 30);
        r.showItem("minecraft:tripwire_hook", 30);
        r.particles(ParticleTypes.HEART, 1);
        return Lines.pick(rnd, "All checked in, " + pn + ". Guest suite, top floor. The view's gorgeous. Kinda like you.",
                "Here's your key. If you get lonely up there, you know where the desk is.",
                "One room key for " + pn + ". I picked the nice one. Don't tell anyone.");
    }

    static String persona(String kind, Resident r, String pn, RandomSource rnd) {
        CityData.Profile p = r.profile();
        if (p == null || p.trait != Trait.LAIDBACK) return null;
        boolean flirt = Cast.flirty(p.id);
        String job = p.job.title.toLowerCase(Locale.ROOT);
        return switch (kind) {
            case "meet" -> flirt ? "Well, hey. I'm " + p.name + ", I run the front desk at Ember Heights. And you must be " + pn + "... I've heard things. Good things." : "Hey. I'm " + p.name + ", the " + job + ". You're " + pn + ", right?";
            case "hi" -> flirt ? Lines.pick(rnd, "Hey you. Was hoping you'd swing by.", "Oh, hi " + pn + ". Day just got better.", "Mm, hey " + pn + ". What's up?") : Lines.pick(rnd, "Heyyy, " + pn + ".", "Oh hey. What's up?");
            case "bye" -> flirt ? Lines.pick(rnd, "Leaving already? Fine... see you later, " + pn + ".", "Bye, " + pn + ". Don't be a stranger.", "Later, handsome. Or pretty. Whichever you're going for today.") : Lines.pick(rnd, "Later!", "Catch you around.");
            case "thanks" -> Lines.pick(rnd, "Anytime. Really.", "No worries, " + pn + ".", flirt ? "For you? Always." : "All good.");
            case "compliment" -> flirt ? Lines.pick(rnd, "Careful, " + pn + ". Keep talking like that and I might believe you.", "Aww. You're not so bad yourself.", "Stop it... no, actually, keep going.") : Lines.pick(rnd, "Ha, thanks. You too.", "Aw, that's sweet.");
            case "love" -> flirt ? Lines.pick(rnd, "Whoa there, " + pn + ". At least buy me noodles first.", "Heh. Say that again when I'm off the clock.") : null;
            case "how" -> Lines.pick(rnd, "Honestly? Pretty chill.", "Can't complain. Going with the flow.", "Good, good. Taking it easy.");
            default -> null;
        };
    }

    static String flirt(ServerPlayer pl, Resident r, String t, RandomSource rnd) {
        CityData.Profile p = r.profile();
        if (p == null || !Cast.flirty(p.id)) return null;
        String pn = pl.getName().getString();
        if (any(t, " date ", " go out ", " dinner with me ", " hang out with me ")) return Lines.pick(rnd, "A date? Sure, why not. Pick somewhere and I'll go with the flow.", "Mm, I'd like that. The cinema, maybe? I'll let you choose the seats.");
        if (any(t, " cute ", " pretty ", " beautiful ", " gorgeous ", " hot ", " lovely ")) {
            r.particles(ParticleTypes.HEART, 2);
            return Lines.pick(rnd, "Me? Stop. ...Okay, don't stop.", "You're making me blush at work, " + pn + ". Very unprofessional of you.", "Takes one to know one.");
        }
        if (any(t, " single ", " boyfriend ", " girlfriend ", " dating anyone ", " seeing anyone ")) {
            String partner = p.partner.isEmpty() ? null : r.data().profiles.get(p.partner) == null ? null : r.data().profiles.get(p.partner).name;
            return partner != null ? "I'm with " + partner + ". Doesn't mean I can't be nice to you, though." : Lines.pick(rnd, "Single. Why, you asking for a friend?", "Nobody special right now. Maybe that changes. Who knows.");
        }
        if (any(t, " wink ", " flirt ", " flirting ")) return Lines.pick(rnd, "Who, me? Never. *winks*", "I'm just friendly. Very, very friendly.");
        if (any(t, " relax ", " chill ", " stressed ", " busy ")) return Lines.pick(rnd, "Life's too short to rush, " + pn + ". Sit in the lobby with me for a bit.", "Breathe. Whatever it is, it'll sort itself out. It usually does.");
        return null;
    }

    private static boolean any(String t, String... keys) {
        for (String k : keys) if (t.contains(k)) return true;
        return false;
    }
}
