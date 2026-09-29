package com.fireheart.city;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

public final class Events {
    private static final class Zone {
        String player;
        int placed;
        int broken;
        long first;
        long last;
        long reported = -100000;
        long sx, sy, sz;
        int n;
        final Map<String, Integer> blocks = new HashMap<>();
    }

    private static final Map<Long, Zone> ZONES = new HashMap<>();
    private static final Map<String, Long> SIGHTINGS = new HashMap<>();
    private static Boolean wasRaining;
    private static Boolean wasThundering;

    private Events() {}

    public static String sentence(String t) {
        return t.isEmpty() ? t : t.substring(0, 1).toUpperCase() + t.substring(1);
    }

    public static String nearestLabel(BlockPos pos) {
        if (pos == null) return "town";
        Place best = null;
        double bd = Double.MAX_VALUE;
        for (Place p : Place.ALL.values()) {
            if (p.key.startsWith("apt")) continue;
            double d = p.pos.distSqr(pos);
            if (d < bd) { bd = d; best = p; }
        }
        if (Elevator.floorOfPos(pos) >= 0) return "Ember Heights";
        return best == null ? "town" : best.label;
    }

    private static void track(Level level, BlockPos pos, Player player, boolean place, String blockName) {
        if (level.isClientSide || level.dimension() != Level.OVERWORLD) return;
        long key = ((long) (pos.getX() >> 4) << 32) ^ ((pos.getZ() >> 4) & 0xffffffffL);
        Zone z = ZONES.computeIfAbsent(key, k -> new Zone());
        long now = level.getGameTime();
        if (now - z.last > 3600) {
            z.placed = 0; z.broken = 0; z.first = now; z.sx = 0; z.sy = 0; z.sz = 0; z.n = 0; z.blocks.clear();
        }
        z.player = player.getName().getString();
        z.last = now;
        if (place) {
            z.placed++;
            z.blocks.merge(blockName, 1, Integer::sum);
        } else z.broken++;
        z.sx += pos.getX(); z.sy += pos.getY(); z.sz += pos.getZ(); z.n++;
    }

    public static void onBreak(BlockEvent.BreakEvent e) {
        try {
            if (e.getPlayer() != null && e.getLevel() instanceof Level l) track(l, e.getPos(), e.getPlayer(), false, "");
            if (e.getPlayer() != null && e.getLevel() instanceof ServerLevel sl0) Repair.onPlayerBreak(sl0, e.getPos(), e.getPlayer());
        } catch (Throwable t) {
            if (errors++ < 20) FireheartCity.LOG.error("Solaris event failed", t);
        }
    }

    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        try {
            if (e.getEntity() instanceof Player p && e.getLevel() instanceof Level l)
                track(l, e.getPos(), p, true, e.getPlacedBlock().getBlock().getName().getString().toLowerCase());
            if (e.getEntity() instanceof Player && e.getLevel() instanceof ServerLevel sl0) Repair.onPlayerPlace(sl0, e.getPos());
        } catch (Throwable t) {
            if (errors++ < 20) FireheartCity.LOG.error("Solaris event failed", t);
        }
    }

    public static void onExplosion(ExplosionEvent.Detonate e) {
        try {
            Level l = e.getLevel();
            if (!(l instanceof ServerLevel sl) || l.dimension() != Level.OVERWORLD) return;
            Vec3 v = e.getExplosion().getPosition();
            BlockPos pos = BlockPos.containing(v);
            Repair.onExplosion(sl, e.getExplosion(), e.getAffectedBlocks());
            CityData d = CityData.get(sl);
            long day = Calendar.dayOf(sl.getDayTime());
            List<Resident> near = sl.getEntitiesOfClass(Resident.class, new AABB(pos).inflate(40), r -> r.profile() != null);
            if (near.isEmpty() && pos.distSqr(new BlockPos(0, 70, 30)) > 200 * 200) return;
            String[] ids = near.stream().map(Resident::profileId).toArray(String[]::new);
            d.event(day, "explosion", "there was a loud explosion near " + nearestLabel(pos), pos, ids);
            for (Resident r : near) {
                if (r.distanceToSqr(v) < 30 * 30) {
                    r.say(r.pick("WHOA! What was that?!", "Did something just explode?!", "My ears are ringing!"), 60);
                    r.gesture(Resident.G_CHEER, 30);
                }
            }
        } catch (Throwable t) {
            if (errors++ < 20) FireheartCity.LOG.error("Solaris event failed", t);
        }
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        try {
            if (!(e.getEntity().level() instanceof ServerLevel sl)) return;
            CityData d = CityData.get(sl);
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
                VoiceServer.announce(sp);
                Romance.onLogin(sp, d);
                if (!d.setting(sp.getName().getString(), "toured", "0").equals("1")) Tour.offer(sp);
                long wd = Calendar.worldDay(sl);
                Calendar.show(sp, wd, Calendar.weekend(wd) ? "It's the weekend - the city is off work." : "It's a workday in Solaris.");
                if (Mayor.campaigning(d, wd)) Mayor.ballot(sp, d, false);
                String pn0 = sp.getName().getString();
                if (MailboxBlock.of(d, pn0) == null && !d.setting(pn0, "gotMailbox", "0").equals("1")) {
                    d.setSetting(pn0, "gotMailbox", "1");
                    net.minecraft.world.item.ItemStack mbx = new net.minecraft.world.item.ItemStack(FireheartCity.MAILBOX_ITEM.get());
                    if (!sp.getInventory().add(mbx)) sp.drop(mbx, false);
                    sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6[Solaris Post] §eHere's a mailbox! Place it by your front door - letters and gifts go inside, and SolEats deliveries are left at the door."));
                }
                int box = Inv.total(sl, Post.MAILBOX);
                if (box > 0) sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6[Solaris Post] §eYou have " + box + " letter" + (box == 1 ? "" : "s") + " waiting in your mailbox at the post office."));
                int fav = 0;
                for (Favours.Request q : d.civic.favours.values()) if (q.player.equals(sp.getName().getString())) fav++;
                if (fav > 0) sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6[Favours] §e" + fav + " resident" + (fav == 1 ? " is" : "s are") + " waiting on a favour from you. §7Type /favours"));
            }
            if (d.profiles.isEmpty()) return;
            long day = Calendar.dayOf(sl.getDayTime());
            String n = e.getEntity().getName().getString();
            d.event(day, "player", n + " came back to Solaris", null);
        } catch (Throwable t) {
            if (errors++ < 20) FireheartCity.LOG.error("Solaris event failed", t);
        }
    }

    public static void onStopped(ServerStoppedEvent e) {
        Phones.PLAYER_CALLS.clear();
        Phones.RES_CALLS.clear();
        Phones.OPEN_PHONE.clear();
        Phones.LATER.clear();
        Resident.resetPairs();
        Chat.reset();
        Meets.reset();
        Rebrand.reset();
        Skydive.reset();
        Calendar.resetHeal();
        Phones.reset();
        Elevator.reset();
        Reception.reset();
        OrganConsole.reset();
        FireworkMachine.reset();
        Police.reset();
        FireDept.reset();
        Repair.reset();
        Traders.reset();
        Receipts.reset();
        Kitchen.reset();
        ZONES.clear();
        SIGHTINGS.clear();
        wasRaining = null;
        wasThundering = null;
    }

    private static int errors;

    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.level instanceof ServerLevel sl) || sl.dimension() != Level.OVERWORLD) return;
        try {
            Calendar.track(sl, CityData.get(sl));
            Lines.tick(sl.getGameTime());
            if (sl.getGameTime() % 200 == 13 && FhcConfig.timeMoves() && !sl.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DAYLIGHT)) {
                sl.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(true, sl.getServer());
                FireheartCity.LOG.info("Turned the day/night cycle back on so clocks, watches and routines keep time (config: world.timeMoves)");
            }
            Skydive.tick(sl);
            AutoDoor.tickAll(sl, CityData.get(sl));
            if (sl.getGameTime() % 200 == 29) Maintenance.tick(sl, CityData.get(sl));
            if (sl.getGameTime() % 100 == 7) {
                SkyTower.fixDuplicate(sl, CityData.get(sl));
                SkyTower.statue(sl, CityData.get(sl));
                Post.setupMailboxes(sl, CityData.get(sl));
                Expansion.tick(sl, CityData.get(sl));
                if (FhcConfig.autoBuild()) SkyTower.ensure(sl, CityData.get(sl));
                else SkyTower.register(CityData.get(sl));
            }
            Elevator.tick(sl);
            Reception.tick(sl);
            if (sl.getGameTime() % 200 == 37) newcomers(sl, CityData.get(sl));
            Skyliner.tick(sl);
            Ferry.tickManager(sl);
            if (sl.getGameTime() % 20 == 7) Computers.tick(sl, CityData.get(sl));
            if (sl.getGameTime() % 20 == 13) Phones.tick(sl, CityData.get(sl));
            Phones.callTick(sl, CityData.get(sl));
            Extras.tick(sl, CityData.get(sl));
            if (sl.getGameTime() % 20 == 11) Festival.tick(sl, CityData.get(sl));
            if (sl.getGameTime() % 20 == 13) Meets.tick(sl, CityData.get(sl));
            if (sl.getGameTime() % 100 == 17) Rebrand.tick(sl, CityData.get(sl));
            if (sl.getGameTime() % 100 == 41) TvShows.tick(sl);
            if (sl.getGameTime() % 20 == 9) ParrotLove.tick(sl);
            if (sl.getGameTime() % 2 == 0) MusicHall.tick(sl, CityData.get(sl));
            OrganConsole.tick(sl, CityData.get(sl));
            FireworkMachine.tick(sl, CityData.get(sl));
            if (sl.getGameTime() % 10 == 4) Police.dispatch(sl, CityData.get(sl));
            FireDept.tick(sl, CityData.get(sl));
            StellarHome.tick(sl, CityData.get(sl));
            Repair.tick(sl, CityData.get(sl));
            if (sl.getGameTime() % 200 == 91) BeachBar.tick(sl, CityData.get(sl));
            if (sl.getGameTime() % 40 == 23) Traders.scan(sl, CityData.get(sl));
            if (sl.getGameTime() % 100 == 57) Romance.tick(sl, CityData.get(sl));
            Tour.tick(sl);
            if (sl.getGameTime() % 100 == 73) FireDept.buildBunks(sl, CityData.get(sl));
            if (sl.getGameTime() % 100 == 71) Police.buildBunks(sl, CityData.get(sl));
            if (sl.getGameTime() % 20 == 3) for (net.minecraft.server.level.ServerPlayer sp : sl.players()) DeviceItem.tickWorn(sp);
            if (sl.getGameTime() % 2 == 0) Fireworks.tick(sl, CityData.get(sl));
            worldTick(sl);
        } catch (Throwable t) {
            if (errors++ < 20) FireheartCity.LOG.error("Solaris world tick failed", t);
        }
    }

    private static void newcomers(ServerLevel sl, CityData d) {
        if (d.profiles.size() < 5 || !sl.isPositionEntityTicking(Place.APT_LOBBY)) return;
        for (Cast.Member m : Cast.ALL) {
            if (d.profiles.containsKey(m.id())) continue;
            CityData.Profile p = new CityData.Profile();
            p.id = m.id();
            p.name = m.name();
            p.job = m.job();
            p.trait = m.trait();
            p.home = m.home();
            p.skin = m.skin();
            d.profiles.put(p.id, p);
            CityCommand.spawn(sl, d, p);
            long day = Calendar.dayOf(sl.getDayTime());
            d.news(day, p.name + " moved into Solaris and started work as the " + p.job.title.toLowerCase() + " at " + p.job.work().label + ".");
            FireheartCity.LOG.info("New resident arrived: " + p.name + " (" + p.job.title + ")");
        }
    }

    private static void worldTick(ServerLevel sl) {
        long now = sl.getGameTime();
        if (now % 100 != 0) return;
        CityData d = CityData.get(sl);
        if (now % 1200 == 0) Gazette.tick(sl, d, false);
        Calendar.tick(sl, d);
        Party.tick(sl, d);
        if (d.profiles.isEmpty()) return;
        Bank.tick(sl, d);
        Lottery.tick(sl, d);
        Mayor.tick(sl, d);
        Fireworks.planShow(sl, d);
        Stars.plan(sl, d);
        Stars.tick(sl, d);
        Tours.plan(sl, d);
        Festival.roll(sl, d);
        Tours.tick(sl, d);
        Mind.friendMail(sl, d);
        FerryBoard.tick(sl, d);
        Post.tick(sl, d);
        if (now % 1200 == 0) Favours.expire(sl, d);
        Life.daily(sl, d);
        Mind.nightly(sl, d);
        if (!d.skyxBuilt && FhcConfig.autoBuild() && !sl.players().isEmpty() && now > 600) {
            var fn = sl.getServer().getFunctions().get(new net.minecraft.resources.ResourceLocation("skyx", "build"));
            if (fn.isPresent()) {
                d.skyxBuilt = true;
                d.setDirty();
                sl.getServer().getFunctions().execute(fn.get(), sl.getServer().createCommandSourceStack().withSuppressedOutput().withPermission(4));
                FireheartCity.LOG.info("Neon Heights expansion build started (skyx:build)");
            }
        }
        if (now % 200 == 0) RailFix.tick(sl, d);
        Pets.tick(sl, d);
        long day = Calendar.dayOf(sl.getDayTime());
        boolean rain = sl.isRaining(), thunder = sl.isThundering();
        if (wasRaining != null) {
            if (thunder && !wasThundering) everyone(d, d.event(day, "weather", "a thunderstorm rolled over Solaris", null));
            else if (rain && !wasRaining) everyone(d, d.event(day, "weather", "it started raining in Solaris", null));
            else if (!rain && wasRaining) everyone(d, d.event(day, "weather", "the rain finally stopped", null));
        }
        wasRaining = rain;
        wasThundering = thunder;
        Iterator<Map.Entry<Long, Zone>> it = ZONES.entrySet().iterator();
        while (it.hasNext()) {
            Zone z = it.next().getValue();
            if (now - z.last > 12000) { it.remove(); continue; }
            int total = z.placed + z.broken;
            if (total < 12 || now - z.reported < 12000 || now - z.last < 200) continue;
            z.reported = now;
            BlockPos c = new BlockPos((int) (z.sx / z.n), (int) (z.sy / z.n), (int) (z.sz / z.n));
            String what;
            if (z.broken > z.placed * 2) what = z.player + " was tearing something down near " + nearestLabel(c);
            else {
                String top = null;
                int best = 0;
                for (Map.Entry<String, Integer> b : z.blocks.entrySet()) if (b.getValue() > best) { best = b.getValue(); top = b.getKey(); }
                what = z.player + " was building something" + (top != null && best >= 5 ? " out of " + top : "") + " near " + nearestLabel(c);
            }
            List<Resident> near = sl.getEntitiesOfClass(Resident.class, new AABB(c).inflate(24), r -> r.profile() != null);
            String[] ids = near.stream().map(Resident::profileId).toArray(String[]::new);
            d.event(day, "build", what, c, ids);
            boolean reacted = false;
            for (Resident r : near) {
                if (reacted || !r.isFree() || r.distanceToSqr(Vec3.atCenterOf(c)) > 20 * 20) continue;
                r.getLookControl().setLookAt(c.getX(), c.getY(), c.getZ());
                r.say(r.pick("Ooh, what's " + z.player + " building over there?", "Look! Something new is going up!", "I wonder what that's going to be..."), 70);
                r.gesture(Resident.G_THINK, 40);
                reacted = true;
            }
            z.placed = 0;
            z.broken = 0;
        }
    }

    private static void everyone(CityData d, CityData.Event e) {
        for (CityData.Profile p : d.profiles.values()) p.learn(e.id);
    }

    public static void sighting(CityData d, Resident r, String what) {
        long day = r.day();
        String place = nearestLabel(r.blockPosition());
        String key = day + "|" + place + "|" + what;
        Long last = SIGHTINGS.get(key);
        if (last != null) return;
        SIGHTINGS.put(key, r.level().getGameTime());
        if (SIGHTINGS.size() > 200) SIGHTINGS.clear();
        d.event(day, "monster", "a " + what + " was spotted near " + place, r.blockPosition(), r.profileId());
    }

    public static void notice(CityData d, Resident r) {
        CityData.Profile p = r.profile();
        long day = r.day();
        for (int i = d.events.size() - 1; i >= 0; i--) {
            CityData.Event e = d.events.get(i);
            if (e.day < day - 2) break;
            if (e.pos == null || p.known.contains(e.id)) continue;
            if (!e.kind.equals("build") && !e.kind.equals("explosion")) continue;
            if (r.distanceToSqr(Vec3.atCenterOf(e.pos)) > 14 * 14) continue;
            p.learn(e.id);
            if (r.isFree() && r.getRandom().nextFloat() < 0.6f) {
                r.getLookControl().setLookAt(e.pos.getX(), e.pos.getY(), e.pos.getZ());
                r.say(e.kind.equals("build") ? r.pick("Huh! This wasn't here before.", "Oh wow, something new!", "Somebody's been busy around here.") : r.pick("Yikes, look at this mess.", "So THIS is where that bang came from."), 60);
                r.gesture(Resident.G_THINK, 40);
            }
            return;
        }
    }

    public static void seePlayer(CityData d, Resident r, String pn) {
        CityData.Profile p = r.profile();
        long day = r.day();
        for (int i = d.events.size() - 1; i >= 0; i--) {
            CityData.Event e = d.events.get(i);
            if (e.day < day - 1) break;
            if (e.kind.equals("player") && e.text.startsWith(pn) && !p.known.contains(e.id)) {
                p.learn(e.id);
                return;
            }
        }
    }

    public static CityData.Event gossipFor(CityData d, CityData.Profile a, CityData.Profile b, long day) {
        for (int i = d.events.size() - 1; i >= 0; i--) {
            CityData.Event e = d.events.get(i);
            if (e.day < day - 3) break;
            if (e.kind.equals("weather")) continue;
            if (!a.known.contains(e.id) || b.known.contains(e.id)) continue;
            if (e.text.contains(b.name)) continue;
            return e;
        }
        return null;
    }

    public static CityData.Event latestBuild(CityData d) {
        for (int i = d.events.size() - 1; i >= 0; i--) if (d.events.get(i).kind.equals("build")) return d.events.get(i);
        return null;
    }

    public static CityData.Event freshestUnshared(CityData d, CityData.Profile p, String pn) {
        LinkedHashSet<Integer> told = d.playerKnown.get(pn);
        long day = d.events.isEmpty() ? 0 : d.events.get(d.events.size() - 1).day;
        for (int i = d.events.size() - 1; i >= 0; i--) {
            CityData.Event e = d.events.get(i);
            if (e.day < day - 2) break;
            if (!p.known.contains(e.id) || e.kind.equals("weather") || e.kind.equals("player")) continue;
            if (e.text.startsWith(pn)) continue;
            if (told != null && told.contains(e.id)) continue;
            return e;
        }
        return null;
    }

    public static void markTold(CityData d, CityData.Profile p, String pn, CityData.Event e) {
        LinkedHashSet<Integer> set = d.playerKnown.computeIfAbsent(pn, k -> new LinkedHashSet<>());
        set.add(e.id);
        while (set.size() > 80) set.remove(set.iterator().next());
        d.setDirty();
    }

    public static String reaction(CityData.Event e, CityData.Profile b, RandomSource r) {
        String t = e.text;
        return switch (e.kind) {
            case "build" -> r.nextBoolean() ? "Really? I wonder what they're making!" : "I'll have to go take a look!";
            case "explosion" -> "I heard that bang! I thought it was thunder.";
            case "monster" -> b.trait == Trait.ADVENTUROUS ? "Ha! I'd have chased it off myself." : "Yikes! I'm staying inside after dark.";
            case "elevator" -> t.contains("working again") ? "Finally! My legs thank them." : "Not again! I'm taking the ladder.";
            case "status" -> "Good for them, they deserve it!";
            case "lottery" -> t.contains("rolls over") ? "Next week's jackpot will be huge!" : t.contains(b.name) ? "I still can't believe it!" : r.nextBoolean() ? "Lucky! I really need to buy a ticket." : "Next week it's my turn, I can feel it.";
            case "election" -> t.contains("elected") ? (t.contains(b.name) ? "I know! I'm still pinching myself." : r.nextBoolean() ? "Good choice, if you ask me." : "Hmm, let's see what they actually do.") : t.contains("speech") ? "I'll be there!" : "I've already made up my mind who I'm voting for.";
            case "favour" -> "That's so kind! Solaris really looks after us.";
            case "show" -> "The fireworks were incredible!";
            case "bank" -> {
                if (t.contains("paid off")) yield "Good for them! Debt-free feels amazing.";
                if (t.contains("took out")) yield b.trait == Trait.GRUMPY ? "A loan? Hope they can pay it back." : "Well, Hugo wouldn't lend it if he didn't trust them.";
                if (t.contains("saved up and bought")) yield r.nextBoolean() ? "Wow! Saving really does pay off." : "Lucky! I'm still saving for " + b.goal + ".";
                if (t.contains("fallen behind")) yield "Uh oh. Hugo won't be happy about that.";
                if (t.contains("interest")) yield "Free coins every Monday - I love it.";
                if (t.contains("opened")) yield "A real bank! About time Solaris had one.";
                yield "Money, money, money!";
            }
            case "player" -> "Oh nice! I should say hi.";
            case "social" -> {
                if (t.contains("couple") || t.contains("date") || t.contains("crush")) yield r.nextBoolean() ? "No way! They're so cute together!" : "Aww, I knew it!";
                if (t.contains("falling out") || t.contains("broke up")) yield "Oh no... that's awful. Drama!";
                if (t.contains("made up")) yield "Oh good, I hated seeing them fight.";
                if (t.contains("best friends")) yield "They're inseparable, those two.";
                yield r.nextBoolean() ? "Oh, that's nice to hear!" : "Solaris is really coming together.";
            }
            default -> "Huh! I had no idea.";
        };
    }
}
