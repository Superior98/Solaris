package com.fireheart.city;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/** Real jobs: each resident walks between work stations, uses real containers and cooks real recipes. */
public final class Work {
    public interface Fx {
        String run(Resident r, ServerLevel l, CityData d, CityData.Profile p);
    }

    public static final class Task {
        final String name;
        final BlockPos block;
        final int ticks;
        final String anim;
        final String hold;
        final Fx start;
        final Fx done;
        BlockPos stand;
        BlockPos fixed;
        Music.Song music;
        int maxTravel = 700;

        Task(String name, BlockPos block, int ticks, String anim, String hold, Fx start, Fx done) {
            this.name = name;
            this.block = block;
            this.ticks = ticks;
            this.anim = anim;
            this.hold = hold;
            this.start = start;
            this.done = done;
        }
    }

    static BlockPos at(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    static final BlockPos BAKERY_WHEAT = at(-28, 75, 4);
    static final BlockPos BAKERY_BREAD = at(-39, 70, 5);
    static final BlockPos BAKERY_FLOUR = at(-28, 72, 4);
    static final BlockPos BAKERY_BOARD = at(-40, 72, 9);
    static final BlockPos BAKERY_POT = at(-38, 72, 11);
    static final BlockPos BAKERY_SKILLET = at(-37, 72, 11);
    static final BlockPos BAKERY_COUNTER = at(-37, 71, 6);
    static final BlockPos BAKERY_CABINET = at(-40, 71, 8);
    static final BlockPos FACTORY_SHEETS = at(6, 70, 7);
    static final BlockPos FACTORY_INGOTS = at(0, 73, 6);
    static final BlockPos FARM_CHEST = at(-4, 70, -3);
    static final BlockPos SUPPLY_DEPOT = at(17, 71, 53);
    static final BlockPos SUPPLY_BARREL = at(19, 71, 57);
    static final BlockPos MARKET_BARREL = at(33, 71, 54);
    static final BlockPos MARKET_STALL = at(29, 71, 54);
    static final BlockPos DINER_BARREL = at(7, 71, 57);
    static final BlockPos DINER_SMOKER = at(8, 71, 57);
    static final BlockPos DINER_FURNACE = at(9, 71, 57);
    static final int[] GRAVEL_X = {-36, -34, -32, -30, -28, -26};
    static final String[] BAKE_KEYS = {"bakery:sugar", "bakery:egg", "bakery:milk", "bakery:cocoa", "bakery:apple", "bakery:pumpkin"};

    public final Map<String, Integer> bag = new LinkedHashMap<>();
    private final ArrayDeque<Task> queue = new ArrayDeque<>();
    private Task cur;
    private int timer;
    private int travel;
    private boolean arrived;
    private int cycle;
    private long lastSay = -10000;

    public BlockPos target() {
        return cur == null ? null : cur.stand;
    }

    public String doing() {
        return cur == null ? null : cur.name;
    }

    public boolean busy() {
        return cur != null && arrived;
    }

    public void reset() {
        cur = null;
        queue.clear();
        arrived = false;
    }

    int bagCount(String id) {
        return bag.getOrDefault(id, 0);
    }

    void bagAdd(String id, int n) {
        int v = bagCount(id) + n;
        if (v <= 0) bag.remove(id); else bag.put(id, v);
    }

    public void tick(Resident r, CityData d, CityData.Profile p) {
        ServerLevel l = (ServerLevel) r.level();
        if (cur == null) {
            if (queue.isEmpty()) plan(r, l, d, p);
            cur = queue.poll();
            if (cur == null) return;
            cur.stand = cur.fixed != null ? cur.fixed : stand(l, cur.block);
            timer = cur.ticks;
            travel = 0;
            arrived = false;
        }
        if (cur.stand == null) { cur = null; return; }
        double dx = r.getX() - (cur.stand.getX() + 0.5), dz = r.getZ() - (cur.stand.getZ() + 0.5);
        boolean near = dx * dx + dz * dz < 1.6 * 1.6 && Math.abs(r.getY() - cur.stand.getY()) < 1.5;
        if (!arrived) {
            if (near) {
                arrived = true;
                r.getNavigation().stop();
                if (cur.hold != null) r.showItem(cur.hold, cur.ticks + 10);
                sound(l, cur, true);
                if (cur.start != null) safe(r, l, d, p, cur.start);
                long now = l.getGameTime();
                if (now - lastSay > 1200 && l.getNearestPlayer(r, 12) != null && r.getRandom().nextFloat() < 0.4f) {
                    lastSay = now;
                    r.say(line(cur.name), 60);
                }
            } else if (++travel > cur.maxTravel) {
                cur = null;
            } else if (travel % 20 == 1 && r.distanceToSqr(cur.stand.getX() + 0.5, cur.stand.getY(), cur.stand.getZ() + 0.5) < 5 * 5) {
                r.getNavigation().moveTo(cur.stand.getX() + 0.5, cur.stand.getY(), cur.stand.getZ() + 0.5, 0.8);
            }
            return;
        }
        r.getLookControl().setLookAt(cur.block.getX() + 0.5, cur.block.getY() + 0.5, cur.block.getZ() + 0.5);
        if (cur.fixed != null && timer % 10 == 0) {
            double fx = r.getX() - (cur.fixed.getX() + 0.5), fz = r.getZ() - (cur.fixed.getZ() + 0.5);
            if (fx * fx + fz * fz > 0.3 * 0.3) r.getMoveControl().setWantedPosition(cur.fixed.getX() + 0.5, cur.fixed.getY(), cur.fixed.getZ() + 0.5, 0.5);
        }
        if (cur.music != null) Music.play(r, l, cur.music, cur.ticks - timer);
        else if (timer % 10 == 0) r.swing(InteractionHand.MAIN_HAND);
        if (timer % 8 == 0) effect(l, cur);
        if (timer % 40 == 20) sound(l, cur, false);
        if (--timer > 0) return;
        Task t = cur;
        cur = null;
        p.log(r.routineDay()).tasks++;
        String res = safe(r, l, d, p, t.done);
        if (res != null && !res.isEmpty()) {
            p.log(r.routineDay()).note(res);
            long now = l.getGameTime();
            if (now - lastSay > 600 && l.getNearestPlayer(r, 10) != null && r.getRandom().nextFloat() < 0.5f) {
                lastSay = now;
                r.say(spoken(res), 70);
            }
        }
        d.setDirty();
    }

    private static String safe(Resident r, ServerLevel l, CityData d, CityData.Profile p, Fx fx) {
        if (fx == null) return null;
        try {
            return fx.run(r, l, d, p);
        } catch (Throwable t) {
            FireheartCity.LOG.error("Work step failed for " + p.id, t);
            return null;
        }
    }

    static String spoken(String note) {
        if (note.startsWith("I ")) return note + "!";
        return Events.sentence(note) + "!";
    }

    static String line(String task) {
        return switch (task) {
            case "knead dough" -> "Time to knead some dough!";
            case "bake at the stove" -> "Into the oven you go!";
            case "collect fresh bread" -> "Let's see what the bread line made.";
            case "stock the counter" -> "Fresh stuff for the counter!";
            case "cook soup" -> "Soup's on!";
            case "deliver wheat to the Auto Bakery" -> "Wheat delivery for Mia!";
            case "grill" -> "Order up!";
            case "serve customers at the counter" -> "Next customer, please!";
            case "count the cash in the vault" -> "Let's count the vault...";
            case "refill the cash machine" -> "Topping up the cash machine.";
            default -> "Back to work...";
        };
    }

    private static void effect(ServerLevel l, Task t) {
        double x = t.block.getX() + 0.5, y = t.block.getY() + 1.0, z = t.block.getZ() + 0.5;
        ParticleOptions p = switch (t.anim) {
            case "cook" -> ParticleTypes.CAMPFIRE_COSY_SMOKE;
            case "chop" -> ParticleTypes.CRIT;
            case "tinker" -> ParticleTypes.ELECTRIC_SPARK;
            case "water" -> ParticleTypes.SPLASH;
            case "sweep" -> ParticleTypes.POOF;
            case "read" -> ParticleTypes.ENCHANT;
            case "count" -> ParticleTypes.WAX_ON;
            case "grow" -> ParticleTypes.HAPPY_VILLAGER;
            default -> null;
        };
        if (p == null) return;
        l.sendParticles(p, x, y, z, t.anim.equals("cook") ? 1 : 3, 0.25, 0.1, 0.25, 0.01);
        if (t.anim.equals("cook")) l.sendParticles(ParticleTypes.FLAME, x, y - 0.4, z, 1, 0.2, 0.0, 0.2, 0.0);
    }

    private static void sound(ServerLevel l, Task t, boolean first) {
        SoundEvent s = switch (t.anim) {
            case "cook" -> first ? SoundEvents.FIRECHARGE_USE : SoundEvents.CAMPFIRE_CRACKLE;
            case "chop" -> SoundEvents.WOOD_HIT;
            case "tinker" -> SoundEvents.ANVIL_USE;
            case "water" -> SoundEvents.BUCKET_EMPTY;
            case "carry" -> first ? SoundEvents.BARREL_OPEN : null;
            case "read" -> SoundEvents.BOOK_PAGE_TURN;
            case "count" -> SoundEvents.CHAIN_STEP;
            case "sweep" -> SoundEvents.WOOL_STEP;
            default -> null;
        };
        if (s != null) l.playSound(null, t.block, s, SoundSource.BLOCKS, t.anim.equals("tinker") ? 0.25f : 0.5f, 0.9f + l.random.nextFloat() * 0.2f);
    }

    static boolean solid(ServerLevel l, BlockPos p) {
        return !l.getBlockState(p).getCollisionShape(l, p).isEmpty();
    }

    public static BlockPos stand(ServerLevel l, BlockPos b) {
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (int dy = -3; dy <= 1; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos c = b.offset(dx, dy, dz);
                    if (!l.isLoaded(c) || solid(l, c) || solid(l, c.above()) || !solid(l, c.below())) continue;
                    if (!l.getFluidState(c).isEmpty()) continue;
                    double score = dx * dx + dz * dz + Math.abs(dy + 1) * 0.6;
                    if (dx == 0 && dz == 0) score += 3;
                    if (score < bd) { bd = score; best = c; }
                }
            }
        }
        return best;
    }

    private static final Map<String, List<BlockPos>> SCAN = new HashMap<>();
    private static final Map<String, Long> SCAN_AT = new HashMap<>();

    static List<BlockPos> scan(ServerLevel l, Place place, String... keys) {
        String k = place.key + "|" + String.join(",", keys);
        long now = l.getGameTime();
        List<BlockPos> hit = SCAN.get(k);
        if (hit != null && now - SCAN_AT.getOrDefault(k, 0L) < 12000) return hit;
        hit = new ArrayList<>();
        BlockPos c = place.pos;
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                for (int dy = -1; dy <= 4; dy++) {
                    BlockPos p = c.offset(dx, dy, dz);
                    if (!l.isLoaded(p)) continue;
                    BlockState st = l.getBlockState(p);
                    if (st.isAir()) continue;
                    ResourceLocation id = ForgeRegistries.BLOCKS.getKey(st.getBlock());
                    if (id == null) continue;
                    String path = id.getPath();
                    for (String key : keys) if (path.contains(key)) { hit.add(p.immutable()); break; }
                }
            }
        }
        SCAN.put(k, hit);
        SCAN_AT.put(k, now);
        return hit;
    }

    private Task t(String name, BlockPos b, int ticks, String anim, String hold, Fx done) {
        return new Task(name, b, ticks, anim, hold, null, done);
    }

    private void plan(Resident r, ServerLevel l, CityData d, CityData.Profile p) {
        cycle++;
        switch (p.job) {
            case BAKER -> planBaker(r, l, d, p);
            case FACTORY_WORKER -> planFactory(l, d, p);
            case GROCER -> planGrocer(l, d, p);
            case CRANE_OPERATOR -> planCrane(r, l, d, p);
            case QUARRY_WORKER -> planQuarry(l, d, p);
            case COOK -> planDiner(l, d, p);
            case CLERK -> {
                if (TechStore.KEY.equals(r.workPlace().key)) planTech(r, l, d, p);
                else planClerk(l, d, p);
            }
            case NOODLE_CHEF -> planKitchen(r, l, d, p, new String[]{"stove", "oven", "pot", "range", "microwave", "skillet", "cutting"},
                    new String[]{"minecraft:mushroom_stew", "farmersdelight:noodle_soup", "farmersdelight:vegetable_noodles", "farmersdelight:fried_rice", "farmersdelight:dumplings"}, null);
            case DOCKMASTER -> planGeneric(r, l, d, p, new String[]{"barrel", "fence", "lantern", "chest", "sign"}, new String[]{"check the moorings", "coil the ropes", "catch fish for the grill"}, "carry");
            case MECHANIC -> planGeneric(r, l, d, p, new String[]{"anvil", "grindstone", "toolbox", "smithing", "barrel"}, new String[]{"hammer out a dent", "sharpen the tools", "fix an engine part", "tune up a gearbox"}, "tinker");
            case GARDENER -> planGeneric(r, l, d, p, new String[]{"poppy", "tulip", "dandelion", "azalea", "petals", "rose", "allium", "orchid", "bush", "cornflower", "oxeye"}, new String[]{"water the flowers", "prune the bushes", "pull some weeds"}, "water");
            case CLOCKKEEPER -> planGeneric(r, l, d, p, new String[]{"gearbox", "cuckoo", "shaft", "cogwheel", "lantern"}, new String[]{"oil the gears", "check the time", "tighten the clock shaft"}, "tinker");
            case ATTENDANT -> planGeneric(r, l, d, p, new String[]{"tank", "pump", "barrel", "pipe", "canopy"}, new String[]{"top up the diesel pumps", "check the fuel gauges", "sweep the forecourt"}, "tinker");
            case ARCADE_KEEPER -> planGeneric(r, l, d, p, new String[]{"jukebox", "note_block", "nixie", "chair", "button"}, new String[]{"tune the note block", "polish the game cabinets", "fix a sticky button"}, "tinker");
            case LIBRARIAN -> planGeneric(r, l, d, p, new String[]{"bookshelf", "lectern", "desk"}, new String[]{"reshelve returned books", "catalogue new arrivals", "dust the bookshelves"}, "read");
            case GUIDE -> planGeneric(r, l, d, p, new String[]{"lamp", "beacon", "seat", "end_rod"}, new String[]{"give a tour of the plaza", "point out the spire", "hand out tour maps"}, "none");
            case PILOT -> planGeneric(r, l, d, p, new String[]{"hazard", "end_rod", "button", "sign"}, new String[]{"run pre-flight checks", "check the landing lights", "log the flight schedule"}, "tinker");
            case BANKER -> planBanker(r, l, d, p);
            case POSTMAN -> planPostman(r, l, d, p);
            case MUSICIAN -> planMusician(r, l, d, p);
            case RECEPTIONIST -> planReception(r, l, d, p);
            case CONCIERGE -> {
                queue.add(fixed("look after the hotel front desk", Hotel.DESK, Hotel.DESK_STAND, 600, "none", "minecraft:tripwire_hook", (rr, ll, dd, pp) -> "I looked after the front desk at magmagamer9's hotel"));
                queue.add(fixed(cycle % 2 == 0 ? "sort the room keys" : "polish the service bell", Hotel.DESK, Hotel.DESK_STAND, 100, "tinker", "minecraft:tripwire_hook", null));
            }
            case POLICE -> planPolice(r, l, d, p);
            case FIREFIGHTER -> planFire(r, l, d, p);
            case REPAIR -> planGeneric(r, l, d, p, new String[]{"anvil", "grindstone", "smithing", "barrel", "scaffolding"}, new String[]{"sort the spare bricks", "sharpen the chisels", "check the city blueprints", "oil the tool belt"}, "tinker");
        }
    }

    static final int[][] BEAT_A = {{-20, 71, 30}, {-20, 71, 18}, {-34, 71, 24}, {7, 71, 50}, {31, 71, 50}, {15, 71, 26}, {-2, 71, 70}};
    static final int[][] BEAT_B = {{30, 71, -2}, {33, 71, -28}, {16, 71, -32}, {19, 71, -14}, {-2, 71, -14}, {-2, 71, -40}, {56, 71, -30}, {82, 71, -24}};

    private void planPolice(Resident r, ServerLevel l, CityData d, CityData.Profile p) {
        if (cycle % 5 == 0) {
            queue.add(fixed("file incident reports", new BlockPos(54, 72, -39), new BlockPos(54, 71, -40), 160, "read", "minecraft:paper", (rr, ll, dd, pp) -> "I filed the day's incident reports"));
            return;
        }
        int[][] beat = (p.id.hashCode() + cycle) % 2 == 0 ? BEAT_A : BEAT_B;
        for (int i = 0; i < 3; i++) {
            int[] w = beat[(cycle * 3 + i + Math.abs(p.id.hashCode())) % beat.length];
            BlockPos at = new BlockPos(w[0], w[1], w[2]);
            Task t = fixed(i == 2 ? "check in on the neighbourhood" : "patrol the streets", at, at, 60, "none", "minecraft:lightning_rod", i == 2 ? (rr, ll, dd, pp) -> {
                rr.gesture(Resident.G_NOD, 20);
                return null;
            } : null);
            t.maxTravel = 2400;
            queue.add(t);
        }
    }

    private void planFire(Resident r, ServerLevel l, CityData d, CityData.Profile p) {
        String[] jobs = {"wash the fire truck", "check the hoses", "slide down the fire pole", "inspect the city's buildings", "polish the brass bell"};
        String pick = jobs[cycle % jobs.length];
        switch (pick) {
            case "wash the fire truck" -> queue.add(fixed(pick, new BlockPos(74, 72, -41), new BlockPos(76, 71, -41), 140, "sweep", "minecraft:water_bucket", (rr, ll, dd, pp) -> "I washed the fire truck until it sparkled"));
            case "check the hoses" -> queue.add(fixed(pick, new BlockPos(87, 72, -40), new BlockPos(86, 71, -40), 110, "tinker", "minecraft:water_bucket", null));
            case "slide down the fire pole" -> queue.add(fixed(pick, new BlockPos(86, 71, -50), new BlockPos(85, 71, -50), 60, "none", null, (rr, ll, dd, pp) -> { rr.gesture(Resident.G_CHEER, 30); return null; }));
            case "inspect the city's buildings" -> {
                int[][] stops = {{-36, 71, 12}, {7, 71, 50}, {31, 71, 50}, {15, 71, 26}, {33, 71, -28}};
                for (int i = 0; i < 2; i++) {
                    int[] w = stops[(cycle + i) % stops.length];
                    Task t = fixed("inspect the city's buildings", new BlockPos(w[0], w[1], w[2]), new BlockPos(w[0], w[1], w[2]), 70, "read", "minecraft:paper", null);
                    t.maxTravel = 2400;
                    queue.add(t);
                }
            }
            default -> queue.add(fixed(pick, new BlockPos(78, 72, -36), new BlockPos(78, 71, -37), 90, "tinker", null, null));
        }
    }

    static final BlockPos LOBBY_PLANT = new BlockPos(20, 71, 21);
    static final BlockPos LOBBY_BOARD = new BlockPos(24, 71, 20);

    private void planReception(Resident r, ServerLevel l, CityData d, CityData.Profile p) {
        queue.add(fixed("look after the front desk", Reception.DESK, Reception.STAND, 600, "none", "minecraft:tripwire_hook", (rr, ll, dd, pp) -> {
            int n = Reception.checkedIn(rr.routineDay());
            return n == 0 ? null : "I checked " + n + (n == 1 ? " resident" : " residents") + " in at the front desk";
        }));
        String[] side = {"sort the room keys", "flick through a magazine", "sort the room keys", "flick through a magazine", "straighten the lobby carpet"};
        String pick = side[cycle % side.length];
        switch (pick) {
            case "sort the room keys" -> queue.add(fixed(pick, Reception.DESK, Reception.STAND, 80, "tinker", "minecraft:tripwire_hook", null));
            case "flick through a magazine" -> queue.add(fixed(pick, Reception.DESK, Reception.STAND, 100, "read", "minecraft:paper", null));
            case "straighten the lobby carpet" -> queue.add(fixed(pick, LOBBY_PLANT, new BlockPos(21, 71, 23), 60, "sweep", null, null));
            default -> queue.add(fixed(pick, LOBBY_BOARD, new BlockPos(24, 71, 21), 70, "read", null, null));
        }
    }

    private Task fixed(String name, BlockPos block, BlockPos stand, int ticks, String anim, String hold, Fx done) {
        Task t = new Task(name, block, ticks, anim, hold, null, done);
        t.fixed = stand;
        return t;
    }

    static final BlockPos BUSK = new BlockPos(-20, 71, 31);

    public boolean playing() {
        return cur != null && arrived && cur.music != null;
    }

    public int musicLeft() {
        return cur != null && cur.music != null ? timer : 0;
    }

    private void planMusician(Resident r, ServerLevel l, CityData d, CityData.Profile p) {
        BlockPos spot = BUSK;
        boolean birthday = false;
        long day = r.day();
        for (CityData.Plan pp : d.plans) if (pp.day == day && pp.what.equals("party") && pp.place.equals("plaza")) birthday = true;
        Music.Song s = birthday && cycle % 2 == 0 ? Music.BIRTHDAY : Music.SONGS[cycle % Music.SONGS.length];
        queue.add(fixed("tune the guitar", spot.north(), spot, 50, "tinker", "minecraft:note_block", null));
        Task play = fixed("play " + s.name(), spot.north(3), spot, s.ticks() + 12, "music", "minecraft:note_block", (rr, ll, dd, pp) -> {
            DayLog lg = pp.log(rr.routineDay());
            lg.make("minecraft:note_block", 1);
            return "I played " + s.name() + " at the plaza";
        });
        play.music = s;
        queue.add(play);
        queue.add(fixed("take a bow", spot.north(3), spot, 40, "none", null, (rr, ll, dd, pp) -> {
            rr.gesture(Resident.G_WAVE, 40);
            int n = ll.getEntitiesOfClass(Resident.class, rr.getBoundingBox().inflate(10), x -> x != rr && x.listeningTo(rr)).size();
            if (n > 0) rr.say(rr.pick("Thank you, thank you! You've been a wonderful audience!", "Thanks for listening, everyone!", "Tips are very welcome!"), 60);
            return null;
        }));
    }

    private void planPostman(Resident r, ServerLevel l, CityData d, CityData.Profile p) {
        List<Post.Letter> bag = new ArrayList<>();
        for (Post.Letter x : d.civic.mail) if (x.stage == 1 && bag.size() < 4) bag.add(x);
        if (bag.isEmpty()) {
            int waiting = 0;
            for (Post.Letter x : d.civic.mail) if (x.stage == 0) waiting++;
            if (waiting > 0) {
                queue.add(fixed("sort the mail", Post.SORT, Post.SORT_STAND, 90, "carry", "minecraft:paper", (rr, ll, dd, pp) -> {
                    int n = 0;
                    for (Post.Letter x : dd.civic.mail) if (x.stage == 0 && n < 5 && x.toPlayer()) { x.stage = 1; n++; }
                    for (Post.Letter x : dd.civic.mail) if (x.stage == 0 && n < 5) { x.stage = 1; n++; }
                    dd.setDirty();
                    return "I sorted " + n + (n == 1 ? " letter" : " letters") + " for delivery";
                }));
                return;
            }
            queue.add(fixed("serve customers at the counter", Post.COUNTER, Post.COUNTER_STAND, 240, "none", "minecraft:paper", null));
            queue.add(fixed(cycle % 2 == 0 ? "stamp parcels" : "sweep the post office", Post.STAMP, Post.STAMP_STAND, 90, cycle % 2 == 0 ? "tinker" : "sweep", "minecraft:paper", null));
            return;
        }
        for (Post.Letter x : bag) {
            BlockPos[] ms = Post.mailSpot(l, d, x);
            if (ms != null) {
                String who0 = x.to.substring(7);
                boolean parcel = x.kind.equals("parcel");
                Task mt = fixed(parcel ? "bring " + who0 + "'s SolEats order to their door" : "put " + who0 + "'s post in their mailbox", ms[0], ms[1], 50, "carry", parcel ? "fireheartcity:delivery_box" : "minecraft:paper", (rr, ll, dd, pp) -> Post.toPlayerBox(rr, ll, dd, x, ms[0], ms[1]));
                mt.maxTravel = 3000;
                queue.add(mt);
                continue;
            }
            BlockPos to = Post.target(l, d, x);
            if (to == null) { x.stage = 2; continue; }
            String who = x.toPlayer() ? x.to.substring(7) : d.accountName(x.to);
            Task t = fixed("deliver a letter to " + who, to, to, 30, "carry", "minecraft:paper", (rr, ll, dd, pp) -> Post.deliver(rr, ll, dd, x));
            t.maxTravel = 2400;
            queue.add(t);
        }
        Task back = fixed("walk back to the post office", Post.COUNTER, Post.COUNTER_STAND, 20, "none", "minecraft:paper", null);
        back.maxTravel = 2400;
        queue.add(back);
    }

    private void planBanker(Resident r, ServerLevel l, CityData d, CityData.Profile p) {
        queue.add(fixed("serve customers at the counter", Bank.COUNTER, Bank.TELLER, 500, "none", "minecraft:writable_book", null));
        String[] side = {"count the cash in the vault", "refill the cash machine", "file loan paperwork", "update the ledger"};
        String pick = side[cycle % side.length];
        switch (pick) {
            case "count the cash in the vault" -> queue.add(fixed(pick, Bank.VAULT_GOLD, Bank.VAULT_STAND, 110, "count", "minecraft:gold_nugget", (rr, ll, dd, pp) -> {
                int total = dd.balance(Bank.RESERVE);
                for (java.util.Map.Entry<String, Integer> e : dd.savings.entrySet()) total += e.getValue();
                return "I counted " + total + " coins in the vault";
            }));
            case "refill the cash machine" -> queue.add(fixed(pick, Bank.ATM_BACK, Bank.ATM_BACK_STAND, 90, "tinker", "minecraft:gold_ingot", (rr, ll, dd, pp) -> "I refilled the cash machine"));
            case "file loan paperwork" -> queue.add(fixed(pick, Bank.DESK, Bank.DESK_STAND, 110, "read", "minecraft:paper", (rr, ll, dd, pp) -> {
                int n = 0;
                for (Bank.Loan ln : dd.loans.values()) if (ln.owed > 0) n++;
                return n == 0 ? "I tidied the loan files - nobody owes us anything" : "I checked " + n + (n == 1 ? " open loan" : " open loans");
            }));
            default -> queue.add(fixed(pick, Bank.LEDGER, Bank.LEDGER_STAND, 90, "read", "minecraft:writable_book", null));
        }
    }

    private void planBaker(Resident r, ServerLevel l, CityData d, CityData.Profile p) {
        for (String k : BAKE_KEYS) if (!d.pantry.containsKey(k)) d.pantry.put(k, 6);
        queue.add(t("check the wheat hopper", BAKERY_WHEAT, 50, "none", null, (rr, ll, dd, pp) -> {
            int w = Inv.count(ll, BAKERY_WHEAT, "minecraft:wheat");
            dd.pantry.put("bakery:needwheat", w < 32 ? 1 : 0);
            return w < 16 ? "The mill is almost out of wheat" : null;
        }));
        queue.add(t("collect fresh bread", BAKERY_BREAD, 60, "carry", "minecraft:bread", (rr, ll, dd, pp) -> {
            int n = Inv.take(ll, BAKERY_BREAD, "minecraft:bread", 6);
            if (n <= 0) return "The bread line hasn't made any bread";
            bagAdd("minecraft:bread", n);
            return "Collected " + n + " fresh loaves from the bread line";
        }));
        queue.add(t("scoop flour", BAKERY_FLOUR, 50, "carry", "create:wheat_flour", (rr, ll, dd, pp) -> {
            int n = Inv.take(ll, BAKERY_FLOUR, "create:wheat_flour", 3);
            bagAdd("create:wheat_flour", n);
            return n == 0 ? "The millstone was out of flour" : null;
        }));
        queue.add(new Task("knead dough", BAKERY_BOARD, 110, "chop", "farmersdelight:wheat_dough",
                (rr, ll, dd, pp) -> { if (bagCount("create:wheat_flour") > 0) Inv.put(ll, BAKERY_BOARD, "farmersdelight:wheat_dough", 1); return null; },
                (rr, ll, dd, pp) -> {
                    Inv.take(ll, BAKERY_BOARD, "farmersdelight:wheat_dough", 1);
                    int f = bagCount("create:wheat_flour");
                    if (f <= 0) return null;
                    bagAdd("create:wheat_flour", -f);
                    bagAdd("farmersdelight:wheat_dough", f);
                    pp.log(rr.routineDay()).make("farmersdelight:wheat_dough", f);
                    return null;
                }));
        queue.add(t("bake at the stove", BAKERY_POT, 160, "cook", "farmersdelight:wheat_dough", (rr, ll, dd, pp) -> bake(rr, dd, pp)));
        if (cycle % 2 == 0) queue.add(t("cook soup", BAKERY_SKILLET, 120, "cook", "minecraft:bowl", (rr, ll, dd, pp) -> {
            String out = cycle % 4 == 0 ? "farmersdelight:stuffed_potato" : "farmersdelight:vegetable_soup";
            bagAdd(out, 2);
            pp.log(rr.routineDay()).make(out, 2);
            return "I made " + Economy.count(out, 2) + " from the veggie crates";
        }));
        queue.add(t("stock the counter", BAKERY_COUNTER, 60, "carry", "minecraft:bread", (rr, ll, dd, pp) -> stockUp(ll, dd, Job.BAKER)));
    }

    private record Recipe(String out, int n, int dough, String[] needs, String verb) {}

    private static final Recipe[] BAKES = {
            new Recipe("farmersdelight:apple_pie", 1, 1, new String[]{"bakery:apple", "bakery:sugar"}, "baked"),
            new Recipe("minecraft:cookie", 8, 1, new String[]{"bakery:cocoa", "bakery:sugar"}, "baked"),
            new Recipe("minecraft:pumpkin_pie", 1, 1, new String[]{"bakery:pumpkin", "bakery:sugar", "bakery:egg"}, "baked"),
            new Recipe("minecraft:cake", 1, 2, new String[]{"bakery:milk", "bakery:egg", "bakery:sugar"}, "decorated")
    };

    private String bake(Resident r, CityData d, CityData.Profile p) {
        List<String> made = new ArrayList<>();
        int dough = bagCount("farmersdelight:wheat_dough");
        for (int i = 0; i < BAKES.length && dough > 0; i++) {
            Recipe rc = BAKES[(i + cycle) % BAKES.length];
            if (dough < rc.dough) continue;
            boolean ok = true;
            for (String n : rc.needs) if (d.pantry(n) <= 0) ok = false;
            if (!ok) continue;
            for (String n : rc.needs) d.pantryAdd(n, -1);
            dough -= rc.dough;
            bagAdd(rc.out, rc.n);
            p.log(r.routineDay()).make(rc.out, rc.n);
            made.add(Economy.count(rc.out, rc.n));
            break;
        }
        if (dough > 0) {
            bagAdd("minecraft:bread", dough);
            p.log(r.routineDay()).make("minecraft:bread", dough);
            made.add(Economy.count("minecraft:bread", dough));
        }
        bag.remove("farmersdelight:wheat_dough");
        if (made.isEmpty()) return null;
        boolean low = false;
        for (String k : BAKE_KEYS) if (d.pantry(k) <= 0) low = true;
        return "I baked " + Economy.join(made) + (low ? " - running low on baking supplies though" : "");
    }

    private String stockUp(ServerLevel l, CityData d, Job j) {
        int n = 0;
        List<String> parts = new ArrayList<>();
        for (String id : new ArrayList<>(bag.keySet())) {
            if (!Economy.isFood(id) && !Shop.sellable(j, id)) continue;
            int c = bagCount(id);
            int put = Shop.put(l, d, j, id, c);
            bagAdd(id, -c);
            if (put > 0) { n += put; parts.add(Economy.count(id, put)); }
        }
        if (n == 0) return null;
        return "Stocked " + j.work().label + " with " + Economy.join(parts.subList(0, Math.min(3, parts.size())));
    }

    private static CityData.Profile worker(CityData d, Job j) {
        for (CityData.Profile q : d.profiles.values()) if (q.job == j) return q;
        return null;
    }

    private static void tell(CityData d, Job j, long day, String note) {
        CityData.Profile q = worker(d, j);
        if (q != null) q.log(day).note(note);
    }

    private static void b2b(ServerLevel l, CityData d, String from, String to, int amount, String memo) {
        long day = Calendar.day(l);
        int tod = (int) Math.floorMod(l.getDayTime(), 24000L);
        if (!d.pay(from, to, amount, memo, day, tod)) d.pay(CityData.CITY, to, amount, memo + " (city grant)", day, tod);
    }

    static final BlockPos FACTORY_FLOOR = at(-2, 71, 0);
    static final BlockPos FARM_FIELD = at(-8, 71, 5);

    private void planFactory(ServerLevel l, CityData d, CityData.Profile p) {
        if (!d.pantry.containsKey("factory:iron")) d.pantry.put("factory:iron", 64);
        queue.add(t("inspect the machines", FACTORY_FLOOR, 60, "read", "", (rr, ll, dd, pp) -> {
            List<Maintenance.Machine> bad = Maintenance.broken(ll);
            for (Maintenance.Machine m : bad) {
                queue.addFirst(t("fix " + m.name(), m.engine(), 100, "tinker", "create:wrench", (r2, l2, d2, p2) -> {
                    if (Maintenance.fault(l2, m) == null) return null;
                    boolean ok = Maintenance.fix(l2, m);
                    if (ok) {
                        p2.log(r2.routineDay()).note("I fixed " + m.name() + " - its engine had stalled");
                        d2.news(r2.day(), p2.name + " got " + m.name() + " running again.");
                        r2.gesture(Resident.G_THUMBS, 40);
                    }
                    return ok ? r2.pick("There we go - " + m.name() + " is running again!", "Fixed! Just needed fuel and a kick.", "Good as new.") : "I couldn't get " + m.name() + " going...";
                }));
            }
            return bad.isEmpty() ? rr.pick("All machines running smoothly.", "Everything's humming along nicely.") : "Uh oh, " + bad.size() + " machine" + (bad.size() == 1 ? "" : "s") + " need fixing!";
        }));
        queue.add(t("tend the wheat field", FARM_FIELD, 80, "grow", "minecraft:iron_hoe", (rr, ll, dd, pp) -> {
            int n = Maintenance.harvest(ll);
            if (n <= 0) return null;
            bagAdd("minecraft:wheat", n);
            pp.log(rr.routineDay()).make("minecraft:wheat", n);
            return rr.pick("Harvested " + n + " wheat and replanted the field.", "Good crop today - " + n + " bundles of wheat!");
        }));
        queue.add(t("collect iron sheets", FACTORY_SHEETS, 60, "carry", "create:iron_sheet", (rr, ll, dd, pp) -> {
            int n = Inv.take(ll, FACTORY_SHEETS, "create:iron_sheet", 8);
            bagAdd("create:iron_sheet", n);
            if (n > 0) { pp.log(rr.routineDay()).make("create:iron_sheet", n); if (pp.count("create:iron_sheet") < 5) pp.add("create:iron_sheet", 1); }
            return n > 0 ? null : "The press line was empty";
        }));
        queue.add(t("refill the ingot chest", FACTORY_INGOTS, 60, "carry", "minecraft:iron_ingot", (rr, ll, dd, pp) -> {
            int have = Inv.count(ll, FACTORY_INGOTS, "minecraft:iron_ingot");
            if (have >= 64) return null;
            if (dd.pantry("factory:iron") < 16) return "We're running low on iron - hope Nina unloads a shipment soon";
            int put = Inv.put(ll, FACTORY_INGOTS, "minecraft:iron_ingot", 16);
            dd.pantryAdd("factory:iron", -put);
            return put > 0 ? "I loaded " + put + " iron ingots into the press line" : null;
        }));
        queue.add(t("check the wheat farm", FARM_CHEST, 60, "carry", "minecraft:wheat", (rr, ll, dd, pp) -> {
            int n = Inv.take(ll, FARM_CHEST, "minecraft:wheat", 32);
            bagAdd("minecraft:wheat", n);
            return n > 0 ? "Harvested " + n + " wheat from the farm" : null;
        }));
        if (bagCount("minecraft:wheat") > 0 || d.pantry("bakery:needwheat") > 0 || cycle % 2 == 0) {
            queue.add(t("deliver wheat to the Auto Bakery", BAKERY_WHEAT, 50, "carry", "minecraft:wheat", (rr, ll, dd, pp) -> {
                int n = bagCount("minecraft:wheat");
                if (n <= 0) return null;
                int put = Inv.put(ll, BAKERY_WHEAT, "minecraft:wheat", n);
                bagAdd("minecraft:wheat", -put);
                if (put <= 0) return null;
                dd.pantry.put("bakery:needwheat", 0);
                b2b(ll, dd, "biz:bakery", "biz:factory", Math.max(1, put / 8), "Wheat delivery");
                tell(dd, Job.BAKER, rr.day(), pp.name + " dropped off " + put + " wheat for the mill");
                return "I delivered " + put + " wheat to the Auto Bakery";
            }));
        }
        queue.add(t("deliver iron sheets to Create Supply Co.", SUPPLY_DEPOT, 50, "carry", "create:iron_sheet", (rr, ll, dd, pp) -> {
            int n = bagCount("create:iron_sheet");
            if (n <= 0) return null;
            int put = Shop.put(ll, dd, Job.CLERK, "create:iron_sheet", n);
            bagAdd("create:iron_sheet", -n);
            if (put <= 0) return null;
            b2b(ll, dd, "biz:supply", "biz:factory", Math.max(1, put), "Iron sheets");
            tell(dd, Job.CLERK, rr.day(), pp.name + " brought " + put + " iron sheets from the factory");
            return "I delivered " + put + " iron sheets to Create Supply Co.";
        }));
    }

    private void planGrocer(ServerLevel l, CityData d, CityData.Profile p) {
        if (!d.pantry.containsKey("market:goods")) d.pantry.put("market:goods", 8);
        queue.add(t("unpack a crate of produce", MARKET_BARREL, 80, "carry", "minecraft:carrot", (rr, ll, dd, pp) -> {
            boolean shipped = dd.pantry("market:goods") > 0;
            if (shipped) dd.pantryAdd("market:goods", -1);
            int a = Shop.put(ll, dd, Job.GROCER, "minecraft:apple", shipped ? 4 : 1);
            int c = Shop.put(ll, dd, Job.GROCER, "minecraft:carrot", 3);
            pp.log(rr.routineDay()).make("minecraft:apple", a);
            pp.log(rr.routineDay()).make("minecraft:carrot", c);
            return shipped ? null : "We're waiting on a shipment from the port";
        }));
        queue.add(t("tidy the stalls", MARKET_STALL, 60, "sweep", null, null));
        boolean bakeryLow = false;
        for (String k : BAKE_KEYS) if (d.pantry(k) < 3) bakeryLow = true;
        if (bakeryLow && d.pantry("market:goods") >= 2) {
            queue.add(t("deliver baking supplies to the Auto Bakery", BAKERY_CABINET, 60, "carry", "minecraft:egg", (rr, ll, dd, pp) -> {
                if (dd.pantry("market:goods") < 2) return null;
                dd.pantryAdd("market:goods", -2);
                for (String k : BAKE_KEYS) dd.pantryAdd(k, 4);
                Inv.put(ll, BAKERY_CABINET, "minecraft:egg", 2);
                Inv.put(ll, BAKERY_CABINET, "minecraft:sugar", 2);
                b2b(ll, dd, "biz:bakery", "biz:market", 10, "Baking supplies");
                tell(dd, Job.BAKER, rr.day(), pp.name + " dropped off eggs, sugar, milk and apples");
                return "I delivered baking supplies to the Auto Bakery";
            }));
        }
        if (d.pantry("diner:supplies") < 6 && d.pantry("market:goods") >= 2) {
            queue.add(t("deliver groceries to the Diesel Diner", DINER_BARREL, 60, "carry", "minecraft:potato", (rr, ll, dd, pp) -> {
                if (dd.pantry("market:goods") < 2) return null;
                dd.pantryAdd("market:goods", -2);
                dd.pantryAdd("diner:supplies", 8);
                Inv.put(ll, DINER_BARREL, "minecraft:potato", 4);
                Inv.put(ll, DINER_BARREL, "minecraft:beef", 2);
                b2b(ll, dd, "biz:diner", "biz:market", 10, "Groceries");
                tell(dd, Job.COOK, rr.day(), pp.name + " delivered a crate of groceries");
                return "I delivered groceries to the Diesel Diner";
            }));
        }
    }

    private void planCrane(Resident r, ServerLevel l, CityData d, CityData.Profile p) {
        Place port = Job.CRANE_OPERATOR.work();
        List<BlockPos> spots = new ArrayList<>();
        for (int i = 0; i < 3; i++) spots.add(port.pos.offset(r.getRandom().nextInt(13) - 6, 0, r.getRandom().nextInt(13) - 6));
        queue.add(t("operate the gantry crane", spots.get(0), 120, "tinker", "minecraft:chain", null));
        queue.add(t("unload a container", spots.get(1), 140, "carry", "minecraft:chain", (rr, ll, dd, pp) -> {
            dd.pantryAdd("market:goods", 3);
            dd.pantryAdd("factory:iron", 32);
            b2b(ll, dd, "biz:factory", "biz:port", 8, "Iron shipment");
            b2b(ll, dd, "biz:market", "biz:port", 6, "Grocery shipment");
            pp.log(rr.routineDay()).make("minecraft:chain", 1);
            tell(dd, Job.GROCER, rr.day(), pp.name + " unloaded a fresh shipment at the port");
            return "I unloaded a container of iron and groceries";
        }));
        queue.add(t("sign the shipping papers", spots.get(2), 60, "none", "minecraft:paper", null));
    }

    private void planQuarry(ServerLevel l, CityData d, CityData.Profile p) {
        int x = GRAVEL_X[cycle % GRAVEL_X.length];
        BlockPos in = at(x, 75, 54), out = at(x, 71, 54);
        queue.add(t("load cobblestone into the crusher", in, 70, "carry", "minecraft:cobblestone", (rr, ll, dd, pp) -> {
            if (Inv.count(ll, in, "minecraft:cobblestone") + Inv.count(ll, in, "minecraft:gravel") >= 64) return null;
            boolean gravelLine = x == -32 || x == -28;
            int put = Inv.put(ll, in, gravelLine ? "minecraft:gravel" : "minecraft:cobblestone", 32);
            return put > 0 ? "I loaded " + put + (gravelLine ? " gravel" : " cobblestone") + " into the crushers" : null;
        }));
        queue.add(t("haul away the gravel", out, 70, "carry", "minecraft:gravel", (rr, ll, dd, pp) -> {
            int n = 0;
            String got = null;
            for (int i = 0; i < 16; i++) {
                String id = Inv.takeAny(ll, out);
                if (id == null) break;
                got = id;
                n++;
            }
            if (n == 0) return null;
            pp.log(rr.routineDay()).make(got, n);
            if (pp.count("minecraft:flint") < 5) pp.add("minecraft:flint", 1);
            b2b(ll, dd, CityData.CITY, "biz:gravel", Math.max(1, n / 4), "Gravel contract");
            return "Hauled " + Economy.count(got, n) + " out of the crushers";
        }));
    }

    private void planDiner(ServerLevel l, CityData d, CityData.Profile p) {
        if (!d.pantry.containsKey("diner:supplies")) d.pantry.put("diner:supplies", 10);
        queue.add(t("grab ingredients", DINER_BARREL, 50, "carry", "minecraft:beef", null));
        queue.add(t("grill", (cycle % 2 == 0) ? DINER_SMOKER : DINER_FURNACE, 150, "cook", "minecraft:cooked_beef", (rr, ll, dd, pp) -> {
            String[] menu = {"minecraft:cooked_beef", "farmersdelight:hamburger", "farmersdelight:steak_and_potatoes", "farmersdelight:fried_egg"};
            List<String> made = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                if (dd.pantry("diner:supplies") > 0) {
                    dd.pantryAdd("diner:supplies", -1);
                    String dish = menu[(cycle + i) % menu.length];
                    int n = dish.contains("egg") ? 2 : 1;
                    bagAdd(dish, n);
                    pp.log(rr.routineDay()).make(dish, n);
                    made.add(Economy.count(dish, n));
                } else {
                    bagAdd("minecraft:baked_potato", 2);
                    pp.log(rr.routineDay()).make("minecraft:baked_potato", 2);
                    made.add(Economy.count("minecraft:baked_potato", 2));
                }
            }
            return "I cooked " + Economy.join(made) + (dd.pantry("diner:supplies") <= 0 ? " - we're out of beef, need a delivery from Omar" : "");
        }));
        queue.add(t("serve up at the counter", Job.COOK.work().pos, 50, "carry", "minecraft:cooked_beef", (rr, ll, dd, pp) -> stockUp(ll, dd, Job.COOK)));
    }

    private void planClerk(ServerLevel l, CityData d, CityData.Profile p) {
        queue.add(t("sort the stockroom", SUPPLY_BARREL, 80, "carry", "minecraft:paper", null));
        queue.add(t("assemble cogwheels", at(21, 71, 57), 120, "tinker", "create:cogwheel", (rr, ll, dd, pp) -> {
            int c = Shop.put(ll, dd, Job.CLERK, "create:cogwheel", 2);
            int s = Shop.put(ll, dd, Job.CLERK, "create:shaft", 2);
            pp.log(rr.routineDay()).make("create:cogwheel", c);
            if (pp.count("create:cogwheel") < 5) pp.add("create:cogwheel", 1);
            return c + s > 0 ? null : "The shelves are packed full";
        }));
        queue.add(t("arrange the display", at(22, 71, 53), 60, "carry", "create:cogwheel", null));
    }

    private void planTech(Resident r, ServerLevel l, CityData d, CityData.Profile p) {
        BlockPos demo = TechStore.DEMOS[cycle % TechStore.DEMOS.length];
        queue.add(fixed("demo SolOS on the display PC", demo, demo.south(), 110, "tinker", null, (rr, ll, dd, pp) -> {
            Computers.setScreen(ll, demo, 2);
            return "I showed off the demo PCs at SolTech";
        }));
        queue.add(fixed("polish the display phones", new BlockPos(35, 73, 5), new BlockPos(35, 71, 3), 80, "tinker", "fireheartcity:phone", null));
        queue.add(fixed("ring up sales at the counter", TechStore.KIOSK, TechStore.CLERK, 120, "tinker", null, (rr, ll, dd, pp) -> {
            Computers.setScreen(ll, TechStore.KIOSK, 1);
            int sold = 0;
            for (CityData.Tx t : dd.ledger) if (t.to.equals("biz:tech") && t.day == rr.routineDay()) sold++;
            return sold > 0 ? "I rang up " + sold + " sale" + (sold == 1 ? "" : "s") + " at SolTech" : null;
        }));
    }

    private void planKitchen(Resident r, ServerLevel l, CityData d, CityData.Profile p, String[] keys, String[] menu, String supplyKey) {
        Place wp = p.job.work();
        List<BlockPos> st = scan(l, wp, keys);
        BlockPos a = st.isEmpty() ? wp.pos : st.get(r.getRandom().nextInt(st.size()));
        BlockPos b = st.isEmpty() ? wp.pos : st.get(r.getRandom().nextInt(st.size()));
        queue.add(t("prep ingredients", a, 90, "chop", "minecraft:carrot", null));
        queue.add(t("cook", b, 150, "cook", "minecraft:bowl", (rr, ll, dd, pp) -> {
            String dish = menu[cycle % menu.length];
            bagAdd(dish, 2);
            pp.log(rr.routineDay()).make(dish, 2);
            return "I cooked " + Economy.count(dish, 2);
        }));
        queue.add(t("serve up at the counter", wp.pos, 50, "carry", "minecraft:bowl", (rr, ll, dd, pp) -> stockUp(ll, dd, pp.job)));
    }

    private void planGeneric(Resident r, ServerLevel l, CityData d, CityData.Profile p, String[] keys, String[] verbs, String anim) {
        Place wp = r.workPlace();
        List<BlockPos> st = scan(l, wp, keys);
        int n = 2 + r.getRandom().nextInt(2);
        for (int i = 0; i < n; i++) {
            BlockPos b = st.isEmpty() ? wp.pos.offset(r.getRandom().nextInt(7) - 3, 0, r.getRandom().nextInt(7) - 3) : st.get(r.getRandom().nextInt(st.size()));
            String verb = verbs[(cycle + i) % verbs.length];
            boolean last = i == n - 1;
            queue.add(t(verb, b, 90 + r.getRandom().nextInt(70), anim, p.job.tool == null ? null : Inv.id(p.job.tool), last ? (rr, ll, dd, pp) -> produce(rr, ll, dd, pp, verb) : null));
        }
    }

    private String produce(Resident r, ServerLevel l, CityData d, CityData.Profile p, String verb) {
        List<String> prods = Economy.products(p.job);
        String item = prods.get(r.getRandom().nextInt(prods.size()));
        if (Economy.isFood(item)) {
            Shop.put(l, d, p.job, item, 2);
            p.log(r.routineDay()).make(item, 2);
            return "I " + past(verb) + " and made " + Economy.count(item, 2);
        }
        if (p.count(item) < 5) p.add(item, 1);
        p.log(r.routineDay()).make(item, 1);
        return r.getRandom().nextFloat() < 0.5f ? "I " + past(verb) : null;
    }

    static String past(String verb) {
        String[] w = verb.split(" ", 2);
        String v = switch (w[0]) {
            case "catch" -> "caught";
            case "give" -> "gave";
            case "run" -> "ran";
            case "fix" -> "fixed";
            case "check" -> "checked";
            case "log" -> "logged";
            case "top" -> "topped";
            case "sweep" -> "swept";
            case "hand" -> "handed";
            case "point" -> "pointed";
            case "tune" -> "tuned";
            case "polish" -> "polished";
            case "reshelve" -> "reshelved";
            case "catalogue" -> "catalogued";
            case "dust" -> "dusted";
            case "water" -> "watered";
            case "prune" -> "pruned";
            case "pull" -> "pulled";
            case "oil" -> "oiled";
            case "tighten" -> "tightened";
            case "hammer" -> "hammered";
            case "sharpen" -> "sharpened";
            case "coil" -> "coiled";
            default -> w[0].endsWith("e") ? w[0] + "d" : w[0] + "ed";
        };
        return w.length > 1 ? v + " " + w[1] : v;
    }
}
