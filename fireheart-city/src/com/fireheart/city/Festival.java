package com.fireheart.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Festival of the Founder. Runs on its own clock (game ticks since it began), so it works at any time of day:
 * gathering, sermon, chanting, offerings, a firework finale, then a feast with food stalls, lanterns, music,
 * dancing, gifts for the Founder and fireworks every half minute, after which the decorations are packed away.
 */
public final class Festival {
    private Festival() {}

    public static final BlockPos PATH = new BlockPos(-2, 71, -46);
    public static final BlockPos PRIEST = new BlockPos(-2, 77, -63);
    public static final BlockPos ALTAR = new BlockPos(-2, 77, -65);
    public static final BlockPos HEAD = new BlockPos(-2, 168, -76);
    private static final BlockPos SKY = new BlockPos(-2, 80, -50);
    public static final long LEAD = -600, GATHER = 0, SERMON = 400, CHANT = 800, OFFER = 1200, FINALE = 1700, FEAST = 1900, END = 5500;
    public static final long SUNSET = 10400;
    private static final Set<String> GUESTS = new HashSet<>();
    private static final Set<String> OFFERED = new HashSet<>();
    private static final Map<String, Integer> SPOT = new HashMap<>();
    private static final Set<String> BLESSED = new HashSet<>();
    private static final Map<String, Integer> GIFTS = new HashMap<>();
    private static final Set<String> HAMPER = new HashSet<>();
    private static long day = -1, startGame = -1, now = 0;
    private static String priest = "";
    private static int sermon, note;
    private static boolean miracle, decorated, closed = true;

    private static final String[] SERMON_LINES = {
            "Friends! Neighbours! We gather today beneath the gaze of our Founder.",
            "Before the Founder, this was empty land and open sea.",
            "The Founder raised Solaris, lit the neon of the sky island, and built the ferry that carries us!",
            "Every road we walk, every coin in the bank, every noodle at Luna's - all of it, the Founder's gift.",
            "So let us bow our heads, give thanks - and then let the feast begin!"
    };
    private static final String[] CHANTS = {"All hail the Founder!", "Praise the Founder!", "Bless Solaris!", "Founder, watch over us!", "Glory to the Founder!"};
    private static final String[] FEAST_LINES = {"Best festival ever!", "Have you tried the cake? Incredible.", "Happy Festival of the Founder!", "Look at the fireworks!", "I could dance all night!",
            "Another slice, please!", "The lanterns look so pretty.", "To the Founder!", "Who made these cookies? They're amazing.", "This is what Solaris is all about.", "*hums along to the music*", "Wooo!"};
    private static final String[] FOODS = {"minecraft:cake", "minecraft:cookie", "minecraft:pumpkin_pie", "minecraft:sweet_berries", "minecraft:golden_carrot", "minecraft:honey_bottle", "minecraft:apple", "minecraft:bread"};
    private static final int[] TUNE = {12, 14, 16, 19, 16, 14, 12, 9, 12, 14, 16, 12, 19, 21, 19, 16, 14, 12, 14, 16, 12, 9, 7, 12};
    private static final int[] PALETTE = {0xFF3B3B, 0xFFB43B, 0xFFF03B, 0x5BFF6A, 0x3BD4FF, 0xC23BFF, 0xFF4FD8, 0xFFD27A};

    public static boolean activeToday(long d) {
        return day == d;
    }

    public static boolean guest(String id, long d) {
        return day == d && GUESTS.contains(id);
    }

    public static long elapsed() {
        return startGame < 0 ? Long.MIN_VALUE / 2 : now - startGame;
    }

    public static boolean running() {
        long t = elapsed();
        return startGame >= 0 && t >= LEAD && t <= END;
    }

    public static boolean live(String id) {
        return running() && GUESTS.contains(id);
    }

    public static boolean feasting() {
        long t = elapsed();
        return startGame >= 0 && t >= FEAST && t <= END;
    }

    public static void roll(ServerLevel sl, CityData d) {
        long today = Calendar.worldDay(sl);
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (d.festivalRolled == today || tod < 8000 || tod > 9800) return;
        d.festivalRolled = today;
        d.setDirty();
        if (today - d.festivalDay < 5 || sl.isRaining()) return;
        int chance = Calendar.weekday(today) == 6 ? 10 : 4;
        if (sl.random.nextInt(100) < chance) start(sl, d, today, sl.getGameTime() + (SUNSET - tod));
    }

    public static int start(ServerLevel sl, CityData d, long today) {
        return start(sl, d, today, sl.getGameTime() - LEAD);
    }

    public static int start(ServerLevel sl, CityData d, long today, long at) {
        if (!d.festivalDeco.isEmpty()) cleanup(sl, d);
        GUESTS.clear();
        OFFERED.clear();
        SPOT.clear();
        BLESSED.clear();
        GIFTS.clear();
        HAMPER.clear();
        miracle = false;
        decorated = false;
        closed = false;
        sermon = 0;
        note = 0;
        List<String> who = new ArrayList<>();
        for (CityData.Profile p : d.profiles.values()) {
            boolean busy = false;
            for (CityData.Plan pl : d.plansFor(p.id, today)) if (pl.what.equals("party") || pl.what.equals("dance")) busy = true;
            if (!busy) who.add(p.id);
        }
        if (who.size() < 4) { closed = true; return 0; }
        for (ItemEntity old : sl.getEntitiesOfClass(ItemEntity.class, new AABB(ALTAR).inflate(5, 3, 3))) old.discard();
        CityData.Profile mayor = Mayor.mayor(d);
        CityData.Profile pr = mayor != null && who.contains(mayor.id) ? mayor : null;
        if (pr == null) for (String id : who) { CityData.Profile p = d.profiles.get(id); if (pr == null || p.tier > pr.tier) pr = p; }
        priest = pr.id;
        int i = 0;
        for (String id : who) if (!id.equals(priest)) SPOT.put(id, i++);
        GUESTS.addAll(who);
        day = today;
        startGame = at;
        now = sl.getGameTime();
        d.festivalDay = today;
        d.festivalStart = at;
        d.plans.removeIf(pl -> pl.day == today && pl.what.equals("worship"));
        d.addPlan(today, "statue", "worship", who.toArray(new String[0]));
        d.event(today, "festival", "the whole city gathered at the Founder's statue for the Festival of the Founder", PATH, who.toArray(new String[0]));
        boolean soon = at - now <= -LEAD + 20;
        for (ServerPlayer p : sl.players()) Calendar.banner(p, "§6§lFESTIVAL OF THE FOUNDER", soon ? "§eThe residents are gathering at your statue now" : "§eThe residents gather at your statue at sunset");
        Party.replanAll(sl);
        d.setDirty();
        return who.size();
    }

    public static void restore(CityData d, long today) {
        if (startGame >= 0 || !closed || d.festivalStart < 0 || d.festivalDay != today) return;
        for (CityData.Plan pl : d.plans) {
            if (pl.day != today || !pl.what.equals("worship")) continue;
            GUESTS.clear();
            GUESTS.addAll(pl.who);
            SPOT.clear();
            int i = 0;
            CityData.Profile mayor = Mayor.mayor(d);
            priest = mayor != null && pl.who.contains(mayor.id) ? mayor.id : pl.who.get(0);
            for (String id : pl.who) if (!id.equals(priest)) SPOT.put(id, i++);
            day = today;
            startGame = d.festivalStart;
            decorated = !d.festivalDeco.isEmpty();
            closed = false;
        }
    }

    public static BlockPos target(Resident r) {
        long t = elapsed();
        String id = r.profileId();
        int i = SPOT.getOrDefault(id, 0);
        if (t >= FEAST) {
            int h = Math.floorMod((id + (t / 400)).hashCode(), 1 << 20);
            return new BlockPos(-7 + h % 11, 71, -54 + (h / 11) % 15);
        }
        if (id.equals(priest)) return PRIEST;
        if (t >= OFFER && t < FINALE && !OFFERED.contains(id)) return new BlockPos(-6 + (i % 5) * 2, 77, -63);
        int col = i % 5, row = i / 5;
        return new BlockPos(-6 + col * 2, 71, -52 + row * 2);
    }

    public static void tick(ServerLevel sl, CityData d) {
        long today = Calendar.worldDay(sl);
        now = sl.getGameTime();
        restore(d, today);
        long t = elapsed();
        idleDevotion(sl);
        if (startGame >= 0 && t > END) {
            if (!closed) finish(sl, d);
            return;
        }
        if (!running() || t < GATHER - 200 || !sl.isPositionEntityTicking(PATH)) return;
        List<Resident> crowd = sl.getEntitiesOfClass(Resident.class, new AABB(PATH).inflate(16, 12, 26), r -> r.profile() != null && GUESTS.contains(r.profileId()));
        Resident pr = null;
        for (Resident r : crowd) if (r.profileId().equals(priest)) pr = r;
        List<ServerPlayer> players = sl.getEntitiesOfClass(ServerPlayer.class, new AABB(PATH).inflate(60, 60, 60));
        if (t >= SERMON && t < CHANT && pr != null && pr.blockPosition().closerThan(PRIEST, 12) && sermon < SERMON_LINES.length && now % 80 < 20) {
            pr.gesture(sermon == SERMON_LINES.length - 1 ? Resident.G_CHEER : Resident.G_POINT, 60);
            pr.sayTo(SERMON_LINES[sermon++], 90);
            for (Resident r : crowd) if (r != pr) r.getLookControl().setLookAt(pr, 30, 30);
        }
        if (t >= CHANT && t < OFFER) {
            boolean down = (now / 20) % 2 == 0;
            for (Resident r : crowd) {
                if (r == pr) continue;
                r.getLookControl().setLookAt(HEAD.getX() + 0.5, HEAD.getY(), HEAD.getZ() + 0.5, 30, 30);
                r.setPose(down ? Pose.CROUCHING : Pose.STANDING);
                if (!down && sl.random.nextFloat() < 0.2f) r.gesture(Resident.G_BOW, 30);
            }
            if (now % 60 < 20 && !crowd.isEmpty()) {
                Resident c = crowd.get(sl.random.nextInt(crowd.size()));
                c.say(CHANTS[sl.random.nextInt(CHANTS.length)], 50);
                sl.playSound(null, PATH, SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL, 0.6f, 0.7f);
            }
            if (now % 40 < 20) sl.sendParticles(ParticleTypes.END_ROD, HEAD.getX() + 0.5, HEAD.getY() - 20, HEAD.getZ() + 0.5, 12, 6, 30, 2, 0.02);
        } else standAll(crowd);
        if (t >= OFFER && t < FINALE) offerings(sl, crowd, pr);
        if (t >= FINALE && t < FEAST) {
            if (now % 40 < 20) Fireworks.finale(sl, HEAD);
            for (Resident r : crowd) { r.gesture(Resident.G_CHEER, 40); if (sl.random.nextFloat() < 0.2f) r.say("All hail the Founder!", 40); }
        }
        if (t >= FEAST) feast(sl, d, crowd, players, t, today);
        founder(sl, d, crowd, players, today);
        for (Resident r : crowd) {
            DayLog lg = r.profile().log(r.routineDay());
            if (lg.once("festival")) lg.note("I honoured the Founder at the great statue during the Festival of the Founder");
            if (t >= FEAST && lg.once("festival_feast")) lg.note("I ate cake, danced and watched the fireworks at the Founder's feast");
        }
    }

    private static void offerings(ServerLevel sl, List<Resident> crowd, Resident pr) {
        for (Resident r : crowd) {
            String id = r.profileId();
            if (r == pr || OFFERED.contains(id)) continue;
            if (r.getY() > 76 && r.blockPosition().closerThan(ALTAR, 7)) {
                OFFERED.add(id);
                CityData.Profile p = r.profile();
                String item = p.goal.isEmpty() || sl.random.nextBoolean() ? Memory.favourite(p) : Economy.products(p.job).get(0);
                net.minecraft.world.item.Item it = Inv.item(item);
                if (it != null) {
                    ItemEntity ie = new ItemEntity(sl, ALTAR.getX() + 0.5 + (sl.random.nextDouble() - 0.5) * 3, ALTAR.getY() + 0.2, ALTAR.getZ() + 0.5 + (sl.random.nextDouble() - 0.5), new ItemStack(it));
                    ie.setNeverPickUp();
                    ie.setUnlimitedLifetime();
                    ie.addTag("fhc_offering");
                    trimOfferings(sl);
                    ie.setDeltaMovement(0, 0.1, 0);
                    sl.addFreshEntity(ie);
                }
                r.gesture(Resident.G_GIVE, 40);
                r.showItem(item, 40);
                r.say(r.pick("For you, O Founder!", "Please accept this humble " + Economy.label(item).replaceFirst("^(a|an|some) ", "") + "!", "Thank you for everything, Founder.", "Bless this city!"), 60);
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, ALTAR.getX() + 0.5, ALTAR.getY() + 0.5, ALTAR.getZ() + 0.5, 6, 1, 0.3, 0.5, 0);
            }
        }
    }

    private static void feast(ServerLevel sl, CityData d, List<Resident> crowd, List<ServerPlayer> players, long t, long today) {
        if (!decorated) {
            decorated = true;
            decorate(sl, d);
            for (ServerPlayer p : sl.players()) Calendar.banner(p, "§d§lTHE FEAST BEGINS", "§eFood, music, gifts and fireworks at the Founder's statue");
        }
        int pitch = TUNE[note++ % TUNE.length];
        float f = (float) Math.pow(2.0, (pitch - 12) / 12.0);
        sl.playSound(null, PATH, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.RECORDS, 1.2f, f);
        if (note % 2 == 0) sl.playSound(null, PATH, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.RECORDS, 1.0f, f * 0.5f);
        if (note % 4 == 0) sl.playSound(null, PATH, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.RECORDS, 0.8f, 1f);
        sl.sendParticles(ParticleTypes.NOTE, PATH.getX() + 0.5, PATH.getY() + 3, PATH.getZ() + 0.5, 2, 4, 1, 4, 1);
        long ft = t - FEAST;
        if (ft % 600 < 20) Fireworks.burst(sl, SKY, 12, 120, 14);
        if (ft % 600 >= 300 && ft % 600 < 320) Fireworks.burst(sl, HEAD.below(40), 6, 60, 10);
        if (t >= END - 300 && t < END - 280) { Fireworks.finale(sl, HEAD); Fireworks.finale(sl, SKY); }
        for (Resident r : crowd) {
            if (r.onPhone() || r.isSleeping() || sl.random.nextFloat() > 0.3f) continue;
            float roll = sl.random.nextFloat();
            if (roll < 0.3f) r.gesture(Resident.G_DANCE, 80);
            else if (roll < 0.55f) {
                String food = FOODS[sl.random.nextInt(FOODS.length)];
                r.showItem(food, 60);
                r.gesture(Resident.G_EAT, 60);
                sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Economy.stack(food)), r.getX(), r.getEyeY() - 0.2, r.getZ(), 6, 0.1, 0.1, 0.1, 0.05);
                CityData.Profile p = r.profile();
                p.hunger = Math.min(20, p.hunger + 3);
            } else if (roll < 0.7f) r.gesture(sl.random.nextBoolean() ? Resident.G_CLAP : Resident.G_LAUGH, 40);
            else if (roll < 0.8f) r.gesture(Resident.G_CHEER, 40);
            else if (roll < 0.9f && !r.speaking()) r.say(FEAST_LINES[sl.random.nextInt(FEAST_LINES.length)], 60);
        }
        for (ServerPlayer pl : players) {
            if (pl.distanceToSqr(PATH.getX(), PATH.getY(), PATH.getZ()) > 40 * 40) continue;
            String pn = pl.getName().getString();
            if (HAMPER.add(pn)) {
                Resident giver = null;
                for (Resident r : crowd) if (r.profileId().equals(priest)) giver = r;
                if (giver == null && !crowd.isEmpty()) giver = crowd.get(0);
                hamper(pl);
                if (giver != null) {
                    giver.getLookControl().setLookAt(pl, 30, 30);
                    giver.gesture(Resident.G_GIVE, 50);
                    giver.sayTo("Founder " + pn + "! On behalf of all Solaris - your festival hamper!", 100);
                }
                pl.displayClientMessage(Component.literal("§6You received the Founder's Festival Hamper!"), true);
                sl.playSound(null, pl.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.3f);
            }
            for (Resident r : crowd) {
                String key = "gift|" + r.profileId() + "|" + pn;
                if (GIFTS.getOrDefault(pn, 0) >= 6 || BLESSED.contains(key) || r.distanceTo(pl) > 4 || sl.random.nextFloat() > 0.35f) continue;
                BLESSED.add(key);
                GIFTS.merge(pn, 1, Integer::sum);
                ItemStack g = gift(sl, r.profile());
                String gid = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(g.getItem()).toString();
                give(pl, g);
                r.getLookControl().setLookAt(pl, 30, 30);
                r.gesture(Resident.G_GIVE, 40);
                r.showItem(gid, 40);
                if (Resident.mayAddress(sl, pn)) {
                    r.sayTo(r.pick("Happy festival, " + pn + "! This is for you.", "A little gift for our Founder!", "I made this for you, " + pn + "!", "Here - you deserve it, Founder."), 80);
                    Resident.addressed(sl, pn);
                }
                Mind.playerEvent(d, r.profile(), pn, today, "I gave {P} a gift at the Festival of the Founder", 1, 4);
                break;
            }
        }
    }

    private static void founder(ServerLevel sl, CityData d, List<Resident> crowd, List<ServerPlayer> players, long today) {
        for (ServerPlayer pl : players) {
            String pn = pl.getName().getString();
            boolean onAltar = pl.blockPosition().closerThan(ALTAR, 6) && pl.getY() >= 76;
            if (onAltar && !miracle) {
                miracle = true;
                d.event(today, "festival", "the Founder " + pn + " appeared at the statue during the Festival of the Founder", ALTAR, GUESTS.toArray(new String[0]));
                for (Resident r : crowd) {
                    r.getLookControl().setLookAt(pl, 30, 30);
                    r.gesture(Resident.G_CHEER, 80);
                    r.particles(ParticleTypes.TOTEM_OF_UNDYING, 6);
                }
                if (!crowd.isEmpty()) crowd.get(0).sayTo("A MIRACLE! The Founder has appeared before us!", 120);
                Fireworks.finale(sl, ALTAR.above(3));
            }
            for (Resident r : crowd) {
                if (BLESSED.contains(r.profileId() + "|" + pn) || r.distanceTo(pl) > 10) continue;
                BLESSED.add(r.profileId() + "|" + pn);
                r.getLookControl().setLookAt(pl, 30, 30);
                if (!feasting()) { r.setPose(Pose.CROUCHING); r.gesture(Resident.G_BOW, 40); }
                else r.gesture(Resident.G_WAVE, 40);
                if (Resident.mayAddress(sl, pn)) {
                    r.sayTo(r.pick("The Founder walks among us!", pn + "! It's really you!", "Founder " + pn + ", we are not worthy!", "Bless you, " + pn + "!"), 80);
                    Resident.addressed(sl, pn);
                }
                Mind.playerEvent(d, r.profile(), pn, today, "{P}, our Founder, came to the Festival of the Founder", 2, 6);
            }
        }
    }

    private static void hamper(ServerPlayer pl) {
        ItemStack cake = new ItemStack(Items.CAKE);
        cake.setHoverName(Component.literal("§6Founder's Festival Cake"));
        give(pl, cake);
        give(pl, rocket(pl.getRandom().nextInt(PALETTE.length), 16));
        give(pl, new ItemStack(Items.GOLDEN_APPLE, 3));
        give(pl, new ItemStack(Items.COOKIE, 12));
        give(pl, new ItemStack(Items.PUMPKIN_PIE, 4));
    }

    private static void give(ServerPlayer pl, ItemStack s) {
        if (!pl.getInventory().add(s)) pl.drop(s, false);
    }

    private static ItemStack rocket(int color, int n) {
        ItemStack s = new ItemStack(Items.FIREWORK_ROCKET, n);
        CompoundTag fw = s.getOrCreateTagElement("Fireworks");
        fw.putByte("Flight", (byte) 2);
        ListTag ex = new ListTag();
        CompoundTag e = new CompoundTag();
        e.putByte("Type", (byte) (color % 5));
        e.putIntArray("Colors", new int[]{PALETTE[color], PALETTE[(color + 3) % PALETTE.length]});
        e.putBoolean("Trail", true);
        e.putBoolean("Flicker", color % 2 == 0);
        ex.add(e);
        fw.put("Explosions", ex);
        return s;
    }

    private static ItemStack gift(ServerLevel sl, CityData.Profile p) {
        return switch (sl.random.nextInt(8)) {
            case 0 -> new ItemStack(Items.COOKIE, 6);
            case 1 -> rocket(sl.random.nextInt(PALETTE.length), 8);
            case 2 -> new ItemStack(Items.PUMPKIN_PIE, 2);
            case 3 -> new ItemStack(sl.random.nextBoolean() ? Items.CORNFLOWER : Items.ALLIUM, 3);
            case 4 -> new ItemStack(Items.HONEY_BOTTLE, 2);
            case 5 -> new ItemStack(Items.GOLDEN_CARROT, 4);
            default -> {
                net.minecraft.world.item.Item it = Inv.item(Memory.favourite(p));
                yield it == null || it == Items.AIR ? new ItemStack(Items.CAKE) : new ItemStack(it, Math.min(4, it.getMaxStackSize()));
            }
        };
    }

    private static void decorate(ServerLevel sl, CityData d) {
        d.festivalDeco.clear();
        int[] zs = {-53, -49, -45, -41};
        for (int side : new int[]{-8, 4}) {
            for (int z : zs) {
                BlockPos g = ground(sl, side, z);
                if (g == null) continue;
                place(sl, d, g, Blocks.BARREL.defaultBlockState());
                Block cake = side < 0 ? Blocks.RED_CANDLE_CAKE : Blocks.YELLOW_CANDLE_CAKE;
                place(sl, d, g.above(), cake.defaultBlockState().setValue(CandleCakeBlock.LIT, true));
            }
            for (int z = -55; z <= -39; z += 4) {
                BlockPos g = ground(sl, side, z);
                if (g != null) place(sl, d, g, Blocks.LANTERN.defaultBlockState());
            }
        }
        d.setDirty();
    }

    private static BlockPos ground(ServerLevel sl, int x, int z) {
        for (int y = 74; y >= 69; y--) {
            BlockPos p = new BlockPos(x, y, z);
            if (sl.getBlockState(p).isAir() && sl.getBlockState(p.above()).isAir() && sl.getBlockState(p.below()).isFaceSturdy(sl, p.below(), net.minecraft.core.Direction.UP)) return p;
        }
        return null;
    }

    private static void place(ServerLevel sl, CityData d, BlockPos p, BlockState st) {
        sl.setBlock(p, st, 3);
        d.festivalDeco.add(p.asLong());
    }

    private static void cleanup(ServerLevel sl, CityData d) {
        for (int i = d.festivalDeco.size() - 1; i >= 0; i--) {
            BlockPos p = BlockPos.of(d.festivalDeco.get(i));
            Block b = sl.getBlockState(p).getBlock();
            if (b instanceof CandleCakeBlock || b == Blocks.CAKE || b instanceof LanternBlock || b == Blocks.BARREL) sl.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        }
        d.festivalDeco.clear();
        d.setDirty();
    }

    private static void finish(ServerLevel sl, CityData d) {
        if (!sl.isPositionEntityTicking(PATH)) return;
        closed = true;
        cleanup(sl, d);
        d.festivalStart = -1;
        d.setDirty();
        List<Resident> crowd = sl.getEntitiesOfClass(Resident.class, new AABB(PATH).inflate(20, 14, 30), r -> r.profile() != null);
        standAll(crowd);
        for (Resident r : crowd) {
            r.replan();
            if (GUESTS.contains(r.profileId()) && sl.random.nextFloat() < 0.3f) r.say(r.pick("What a night!", "Until next year, Founder.", "My feet hurt from dancing!", "Best. Festival. Ever."), 60);
        }
        startGame = -1;
        Party.replanAll(sl);
        for (ServerPlayer p : sl.players()) p.displayClientMessage(Component.literal("§6The Festival of the Founder has ended."), true);
    }

    private static void standAll(List<Resident> crowd) {
        for (Resident r : crowd) if (r.getPose() == Pose.CROUCHING) r.setPose(Pose.STANDING);
    }

    private static void idleDevotion(ServerLevel sl) {
        if (now % 200 >= 20 || !sl.isPositionEntityTicking(PATH) || running()) return;
        for (Resident r : sl.getEntitiesOfClass(Resident.class, new AABB(PATH).inflate(10, 6, 16), x -> x.profile() != null && x.isFree())) {
            if (sl.random.nextFloat() > 0.15f) continue;
            r.getLookControl().setLookAt(HEAD.getX() + 0.5, HEAD.getY(), HEAD.getZ() + 0.5, 30, 30);
            r.gesture(Resident.G_BOW, 30);
            r.say(r.pick("*bows to the Founder's statue*", "Morning, Founder.", "Thank you, Founder.", "Still can't believe how tall it is."), 50);
        }
    }

    public static String describe() {
        if (day < 0) return "No festival yet.";
        long t = elapsed();
        String phase = startGame < 0 ? "over" : t < GATHER ? "gathering in " + (-t / 20) + "s" : t < SERMON ? "gathering" : t < CHANT ? "sermon" : t < OFFER ? "chanting" : t < FINALE ? "offerings" : t < FEAST ? "finale" : t <= END ? "feast (" + ((END - t) / 20) + "s left)" : "over";
        return "Festival day " + day + " [" + phase + "]: " + GUESTS.size() + " guests, priest " + priest + ", sermon line " + sermon + ", " + OFFERED.size() + " offerings, " + GIFTS.values().stream().mapToInt(Integer::intValue).sum() + " gifts" + (miracle ? ", MIRACLE" : "");
    }

    public static void stop(ServerLevel sl, CityData d) {
        closed = false;
        finish(sl, d);
    }

    public static void skip(ServerLevel sl, long to) {
        if (startGame >= 0) startGame = sl.getGameTime() - to;
    }

    static final int MAX_OFFERINGS = 24;

    static void trimOfferings(ServerLevel sl) {
        List<ItemEntity> l = sl.getEntitiesOfClass(ItemEntity.class, new AABB(ALTAR).inflate(7, 4, 7), e -> e.getTags().contains("fhc_offering") || e.hasPickUpDelay());
        if (l.size() < MAX_OFFERINGS) return;
        l.sort((a, b) -> b.getAge() - a.getAge());
        for (int i = 0; i <= l.size() - MAX_OFFERINGS; i++) l.get(i).discard();
    }
}
