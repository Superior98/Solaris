package com.fireheart.city;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Games and trips residents take with players: hide and seek, races, guiding you somewhere, home visits and tidying litter. */
public final class Errands {
    private Errands() {}

    static final class Game {
        String kind;
        UUID player;
        String pname = "";
        UUID res;
        BlockPos goal;
        String label = "";
        long start, until;
        int phase;
        UUID item;
    }

    static final Map<UUID, Game> GAMES = new HashMap<>();

    static boolean busy(Resident r) {
        return GAMES.containsKey(r.getUUID());
    }

    static Game game(ServerPlayer pl, String kind) {
        for (Game g : GAMES.values()) if (g.player != null && g.player.equals(pl.getUUID()) && g.kind.equals(kind)) return g;
        return null;
    }

    public static void reset() {
        GAMES.clear();
    }

    /* ------------------------------------------------------------ Hide and seek */

    public static String hide(ServerPlayer pl, Resident r) {
        if (r.activityName().equals("work")) return "I'd love to, but I'm working! Ask me after my shift.";
        if (busy(r) || !r.isFree()) return "Maybe later - I'm in the middle of something!";
        if (game(pl, "hide") != null) return "We're already playing! Come find me!";
        ServerLevel sl = pl.serverLevel();
        BlockPos spot = hideout(sl, pl, r);
        if (spot == null) return "Hmm, there's nowhere good to hide around here. Somewhere else?";
        Game g = new Game();
        g.kind = "hide";
        g.player = pl.getUUID();
        g.pname = pl.getName().getString();
        g.res = r.getUUID();
        g.goal = spot;
        g.start = sl.getGameTime();
        g.until = g.start + 3600;
        GAMES.put(r.getUUID(), g);
        r.errand(spot, 3600, 1.35, "hide");
        pl.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, false));
        r.gesture(Resident.G_LAUGH, 30);
        pl.displayClientMessage(Component.literal("§eCount to 20... §7(3 minutes to find them)"), true);
        return "Hide and seek? YES. Close your eyes and count to twenty!";
    }

    static BlockPos hideout(ServerLevel sl, ServerPlayer pl, Resident r) {
        RandomSource rnd = sl.getRandom();
        BlockPos best = null;
        double bestD = -1;
        for (int i = 0; i < 40; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, dist = 12 + rnd.nextDouble() * 16;
            int x = (int) Math.floor(r.getX() + Math.cos(a) * dist), z = (int) Math.floor(r.getZ() + Math.sin(a) * dist);
            if (!sl.isLoaded(new BlockPos(x, (int) r.getY(), z))) continue;
            int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(y - r.getY()) > 6) y = (int) r.getY();
            BlockPos s = Nav.standable(sl, new BlockPos(x, y, z));
            if (s == null) continue;
            boolean hidden = !Nav.sees(sl, pl.getEyePosition(), Vec3.atBottomCenterOf(s).add(0, 1.2, 0), pl);
            double d = s.distSqr(pl.blockPosition());
            if (hidden && d > bestD) {
                bestD = d;
                best = s;
            }
        }
        return best;
    }

    /** Called when a player right-clicks a resident; true if that ended a game of hide and seek. */
    public static boolean found(ServerPlayer pl, Resident r) {
        Game g = GAMES.get(r.getUUID());
        if (g == null || !g.kind.equals("hide") || !g.player.equals(pl.getUUID())) return false;
        GAMES.remove(r.getUUID());
        r.endErrand();
        foundBy(pl.serverLevel(), g, r, pl);
        return true;
    }

    static void foundBy(ServerLevel sl, Game g, Resident r, ServerPlayer pl) {
        long secs = (sl.getGameTime() - g.start) / 20;
        r.getLookControl().setLookAt(pl, 30, 30);
        r.gesture(Resident.G_FACEPALM, 40);
        r.sayTo(r.pick("Aww, you found me! That took you " + secs + " seconds.", "How?! That was my best spot!", "Found! Okay, okay, you're good at this."), 90);
        CityData d = r.data();
        CityData.Rel pr = d.playerRel(r.profileId(), pl.getName().getString());
        pr.aff = Math.min(100, pr.aff + 3);
        r.profile().fun = Math.min(100, r.profile().fun + 10);
        Perks.reward(pl, d, 5, "Hide and seek with " + r.profile().name);
        Perks.unlock(pl, d, "seeker");
        Quests.bump(d, pl.getName().getString(), "game");
    }

    /* ------------------------------------------------------------ Races */

    public static String race(ServerPlayer pl, Resident r, String placeKey) {
        Place p = Place.get(placeKey);
        if (p == null) return null;
        if (r.activityName().equals("work")) return "Can't race on the clock! Catch me after work.";
        if (busy(r) || !r.isFree()) return "I'd love to, but I'm busy right now!";
        if (p.island != r.onIsland()) return "Race to " + p.label + "? That's a Sky Ferry ride away! Pick somewhere closer.";
        double dist = Math.sqrt(r.blockPosition().distSqr(p.pos));
        if (dist < 20) return "That's right there! Pick somewhere further away.";
        if (dist > 260) return "That's way too far to race! Somewhere closer?";
        ServerLevel sl = pl.serverLevel();
        Game g = new Game();
        g.kind = "race";
        g.player = pl.getUUID();
        g.pname = pl.getName().getString();
        g.res = r.getUUID();
        g.goal = p.entrance != null ? p.entrance : p.pos;
        g.label = p.label;
        g.start = sl.getGameTime();
        g.until = g.start + 60 + (long) (dist * 14);
        GAMES.put(r.getUUID(), g);
        r.getNavigation().stop();
        r.errand(r.blockPosition(), 70, 1.0, "race");
        r.gesture(Resident.G_STRETCH, 50);
        return "Race you to " + p.label + "! On your marks...";
    }

    static double raceSpeed(Resident r) {
        CityData.Profile p = r.profile();
        if (p == null) return 1.4;
        return switch (p.trait) {
            case ADVENTUROUS, CHEERFUL -> 1.55;
            case LAIDBACK, DREAMY -> 1.3;
            default -> 1.42;
        };
    }

    /* ------------------------------------------------------------ Guiding */

    public static String guide(ServerPlayer pl, Resident r, String placeKey) {
        Place p = Place.get(placeKey);
        if (p == null) return null;
        if (busy(r) || !r.isFree() || r.activityName().equals("work")) return "I can't leave right now, sorry! It's " + dirTo(r, p) + ".";
        if (p.island != r.onIsland()) return p.label + " is " + (p.island ? "up on Neon Heights" : "down in the city") + " - take the Sky Ferry from the Skyport!";
        double dist = Math.sqrt(r.blockPosition().distSqr(p.pos));
        if (dist < 12) return "You're practically there - it's right " + dirTo(r, p) + "!";
        ServerLevel sl = pl.serverLevel();
        Game g = new Game();
        g.kind = "guide";
        g.player = pl.getUUID();
        g.pname = pl.getName().getString();
        g.res = r.getUUID();
        g.goal = p.entrance != null ? p.entrance : p.pos;
        g.label = p.label;
        g.start = sl.getGameTime();
        g.until = g.start + 6000;
        GAMES.put(r.getUUID(), g);
        r.errand(g.goal, 6000, 0.95, "guide");
        r.gesture(Resident.G_POINT, 40);
        return "Sure! Follow me, I'll take you to " + p.label + ".";
    }

    static String dirTo(Resident r, Place p) {
        return Perks.compass(p.pos.getX() - r.getX(), p.pos.getZ() - r.getZ()) + " of here";
    }

    /* ------------------------------------------------------------ Home visits */

    public static String setHome(ServerPlayer pl, CityData d) {
        BlockPos b = pl.blockPosition();
        d.setSetting(pl.getName().getString(), "home", b.getX() + "," + b.getY() + "," + b.getZ());
        return "§6Home set to §f" + b.toShortString() + "§6. Close friends may drop by to visit when you're home. §7(/sol settings visits to turn off)";
    }

    static BlockPos home(CityData d, String pn) {
        String[] s = d.setting(pn, "home", "").split(",");
        if (s.length != 3) return null;
        return new BlockPos((int) Perks.parse(s[0]), (int) Perks.parse(s[1]), (int) Perks.parse(s[2]));
    }

    static void visits(ServerLevel sl, CityData d) {
        long day = Calendar.worldDay(sl);
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (tod < 10500 || tod > 12200) return;
        for (ServerPlayer pl : sl.players()) {
            String pn = pl.getName().getString();
            BlockPos home = home(d, pn);
            if (home == null || !Perks.opt(d, pn, "visits") || !pl.blockPosition().closerThan(home, 24) || d.setting(pn, "visited", "").equals(String.valueOf(day))) continue;
            if (sl.getRandom().nextInt(6) != 0) continue;
            Resident pick = null;
            for (Resident r : Crowd.all(sl, d)) {
                CityData.Profile p = r.profile();
                if (p == null || !r.isFree() || busy(r) || r.onIsland() != home.getY() > 150 || r.distanceToSqr(Vec3.atCenterOf(home)) > 200 * 200) continue;
                if (d.playerRel(p.id, pn).aff < 60 || !r.activityName().equals("leisure")) continue;
                pick = r;
                break;
            }
            if (pick == null) continue;
            d.setSetting(pn, "visited", String.valueOf(day));
            Game g = new Game();
            g.kind = "visit";
            g.player = pl.getUUID();
            g.pname = pn;
            g.res = pick.getUUID();
            g.goal = home;
            g.start = sl.getGameTime();
            g.until = g.start + 3000;
            GAMES.put(pick.getUUID(), g);
            pick.errand(home, 3000, 1.0, "visit");
        }
    }

    /* ------------------------------------------------------------ Litter */

    static void litter(ServerLevel sl, CityData d) {
        for (Resident r : Crowd.nearPlayers(sl, d)) {
            if (busy(r) || !r.idleHere() || r.getRandom().nextFloat() > 0.15f || !r.blockPosition().closerThan(Letters.CITY, 220)) continue;
            List<ItemEntity> items = sl.getEntitiesOfClass(ItemEntity.class, r.getBoundingBox().inflate(7, 2, 7), ie -> ie.isAlive() && ie.onGround() && ie.getAge() > 600 && !ie.isInWater());
            if (items.isEmpty()) continue;
            ItemEntity ie = items.get(0);
            boolean taken = false;
            for (Game g : GAMES.values()) if (ie.getUUID().equals(g.item)) taken = true;
            if (taken || !r.canSee(ie, 7)) continue;
            Game g = new Game();
            g.kind = "litter";
            g.res = r.getUUID();
            g.item = ie.getUUID();
            g.goal = ie.blockPosition();
            g.start = sl.getGameTime();
            g.until = g.start + 240;
            GAMES.put(r.getUUID(), g);
            r.errand(g.goal, 240, 0.9, "litter");
        }
    }

    /* ------------------------------------------------------------ Tick */

    public static void tick(ServerLevel sl, CityData d) {
        long now = sl.getGameTime();
        if (now % 100 == 51) visits(sl, d);
        if (now % 40 == 17 && FhcConfig.ambient()) litter(sl, d);
        if (GAMES.isEmpty()) return;
        Iterator<Map.Entry<UUID, Game>> it = GAMES.entrySet().iterator();
        while (it.hasNext()) {
            Game g = it.next().getValue();
            Resident r = sl.getEntity(g.res) instanceof Resident rr ? rr : null;
            ServerPlayer pl = g.player == null ? null : sl.getServer().getPlayerList().getPlayer(g.player);
            if (r == null || r.profile() == null || g.player != null && (pl == null || pl.level() != sl)) {
                if (r != null) r.endErrand();
                it.remove();
                continue;
            }
            boolean done = switch (g.kind) {
                case "hide" -> hideTick(sl, g, r, pl, now);
                case "race" -> raceTick(sl, g, r, pl, now);
                case "guide" -> guideTick(sl, g, r, pl, now);
                case "visit" -> visitTick(sl, d, g, r, pl, now);
                case "litter" -> litterTick(sl, g, r, now);
                default -> true;
            };
            if (done) {
                r.endErrand();
                it.remove();
            }
        }
    }

    static boolean hideTick(ServerLevel sl, Game g, Resident r, ServerPlayer pl, long now) {
        if (now - g.start > 120 && pl.distanceTo(r) < 2.2) {
            foundBy(sl, g, r, pl);
            return true;
        }
        if (now > g.until) {
            r.say(r.pick("I WIN! I was right here the whole time!", "Ha! You'll never find me! ...Oh, time's up. I win!"), 80);
            r.gesture(Resident.G_CHEER, 50);
            pl.displayClientMessage(Component.literal("§e" + r.profile().name + " was hiding at §f" + r.blockPosition().toShortString()), false);
            return true;
        }
        if (r.blockPosition().closerThan(g.goal, 2) && now % 200 == 0 && sl.getRandom().nextFloat() < 0.3f) {
            sl.sendParticles(ParticleTypes.NOTE, r.getX(), r.getY() + 2.2, r.getZ(), 1, 0.2, 0.1, 0.2, 0.5);
            r.say(r.pick("*giggles*", "*shh*", "*holds breath*"), 30);
        }
        return false;
    }

    static boolean raceTick(ServerLevel sl, Game g, Resident r, ServerPlayer pl, long now) {
        long t = now - g.start;
        if (g.phase == 0) {
            if (t == 20) pl.displayClientMessage(Component.literal("§e3..."), true);
            if (t == 40) pl.displayClientMessage(Component.literal("§62..."), true);
            if (t == 60) pl.displayClientMessage(Component.literal("§c1..."), true);
            if (t >= 70) {
                g.phase = 1;
                pl.displayClientMessage(Component.literal("§a§lGO!"), true);
                r.say("GO!", 30);
                r.errand(g.goal, (int) (g.until - now), raceSpeed(r), "race");
            }
            return false;
        }
        if (now % 40 == 0) r.gesture(Resident.G_JOG, 50);
        boolean playerIn = pl.blockPosition().closerThan(g.goal, 5);
        boolean resIn = r.blockPosition().closerThan(g.goal, 3);
        CityData d = r.data();
        if (playerIn && !resIn) {
            r.say(r.pick("No way! You're fast!", "*pant* Okay... you win... rematch!", "I let you win. Obviously."), 70);
            r.gesture(Resident.G_WINDED, 60);
            Perks.reward(pl, d, 8, "Won a race against " + r.profile().name);
            Perks.say(pl, "§a🏁 You beat " + r.profile().name + " to " + g.label + "! §e+8 coins");
            d.playerRel(r.profileId(), pl.getName().getString()).aff += 2;
            Perks.unlock(pl, d, "racer");
            Quests.bump(d, pl.getName().getString(), "game");
            return true;
        }
        if (resIn) {
            r.gesture(Resident.G_VICTORY, 60);
            r.say(r.pick("FIRST! Eat my dust, " + pl.getName().getString() + "!", "And the winner is... me!", "Too slow!"), 70);
            Perks.say(pl, "§c🏁 " + r.profile().name + " beat you to " + g.label + ".");
            return true;
        }
        if (now > g.until) {
            r.say("I think we both got lost. Call it a draw?", 60);
            return true;
        }
        return false;
    }

    static boolean guideTick(ServerLevel sl, Game g, Resident r, ServerPlayer pl, long now) {
        double far = pl.distanceTo(r);
        if (far > 64 || now > g.until) {
            r.say("Oh - I lost them. Oh well!", 40);
            return true;
        }
        if (far > 10) {
            if (!g.label.startsWith("~")) g.label = "~" + g.label;
            r.errandTarget = r.blockPosition();
            r.getLookControl().setLookAt(pl, 30, 30);
            if (now % 80 == 0) {
                r.gesture(Resident.G_WAVE, 30);
                r.say(r.pick("This way, " + pl.getName().getString() + "!", "Keep up!", "Over here!"), 40);
            }
        } else if (far < 6 && g.label.startsWith("~")) {
            g.label = g.label.substring(1);
            r.errandTarget = g.goal;
        }
        if (r.blockPosition().closerThan(g.goal, 5) && far < 12) {
            String label = g.label.startsWith("~") ? g.label.substring(1) : g.label;
            r.gesture(Resident.G_POINT, 40);
            r.sayTo(r.pick("Here we are - " + label + "!", "Ta-da! " + label + ". Enjoy!", "And this is " + label + ". Told you it wasn't far!"), 80);
            CityData d = r.data();
            d.playerRel(r.profileId(), pl.getName().getString()).fam += 3;
            Quests.bump(d, pl.getName().getString(), "game");
            return true;
        }
        return false;
    }

    static boolean visitTick(ServerLevel sl, CityData d, Game g, Resident r, ServerPlayer pl, long now) {
        if (g.phase == 0 && r.blockPosition().closerThan(g.goal, 4)) {
            g.phase = 1;
            g.until = now + 1400;
            r.errand(r.blockPosition(), 1400, 0.8, "visit");
            r.gesture(Resident.G_KNOCK, 40);
            for (int i = 0; i < 3; i++) sl.playSound(null, r.blockPosition(), SoundEvents.WOOD_HIT, SoundSource.NEUTRAL, 1f, 0.9f + i * 0.05f);
            CityData.Profile p = r.profile();
            if (pl.distanceTo(r) < 14) {
                r.getLookControl().setLookAt(pl, 30, 30);
                String gift = Skies.GIFTS[sl.getRandom().nextInt(Skies.GIFTS.length)];
                if (Perks.opt(d, g.pname, "gifts")) {
                    Pastimes.giveItem(pl, gift);
                    r.showItem(gift, 60);
                }
                r.sayTo(r.pick("*knock knock* Surprise! I was in the neighbourhood.", "Hi! Thought I'd drop by. Nice place!", "*knock knock* It's me! I brought " + Economy.label(gift) + "."), 100);
                p.log(r.routineDay()).note("I visited " + g.pname + " at home");
                d.playerRel(p.id, g.pname).fam += 3;
            } else {
                r.say("*knock knock* ...Nobody home?", 60);
                Computers.deliver(sl, d, g.pname, p.id, "I knocked on your door but you weren't in! Another time. ☺");
                return true;
            }
            return false;
        }
        if (g.phase == 1 && now % 200 == 0 && pl.distanceTo(r) < 10 && sl.getRandom().nextFloat() < 0.4f) {
            r.getLookControl().setLookAt(pl, 30, 30);
            r.say(r.pick("Love what you've done with the place.", "Is that new?", "Your home is so cosy!", "I should get going soon."), 60);
        }
        if (now > g.until) {
            if (g.phase == 1) r.say(r.pick("Thanks for having me! Bye!", "See you around, " + g.pname + "!"), 60);
            r.gesture(Resident.G_WAVE, 30);
            return true;
        }
        return false;
    }

    static boolean litterTick(ServerLevel sl, Game g, Resident r, long now) {
        if (!(sl.getEntity(g.item) instanceof ItemEntity ie) || !ie.isAlive()) return true;
        if (now > g.until) return true;
        if (r.distanceTo(ie) > 2.2) return false;
        ItemStack st = ie.getItem().copy();
        ie.discard();
        r.gesture(Resident.G_PICKUP, 20);
        ServerPlayer owner = null;
        double best = 10 * 10;
        for (ServerPlayer p : sl.players()) {
            double dd = p.distanceToSqr(r);
            if (dd < best) {
                best = dd;
                owner = p;
            }
        }
        if (owner != null) {
            r.getLookControl().setLookAt(owner, 30, 30);
            if (!owner.getInventory().add(st)) owner.drop(st, false);
            r.say(r.pick("Is this yours? You dropped it.", "Here - you dropped this!", "Found this on the ground. Yours?"), 60);
        } else {
            CityData.Profile p = r.profile();
            String id = String.valueOf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(st.getItem()));
            p.add(id, st.getCount());
            r.say(r.pick("Who leaves things lying around like this?", "*tidies up*", "Keep Solaris tidy!"), 50);
        }
        return true;
    }
}
