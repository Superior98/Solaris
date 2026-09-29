package com.fireheart.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Real cooking for SolEats orders: the shop's cook walks to the stove, a pan appears with the raw ingredients, they
 * sizzle, get tossed and turn cooked, then the finished, steaming dish is plated on the counter before the courier
 * takes it.
 */
public final class Kitchen {
    private Kitchen() {}

    static final SoundEvent SIZZLE = snd("kitchen.sizzle"), CHOP = snd("kitchen.chop"), BUBBLE = snd("kitchen.bubble");

    static SoundEvent snd(String id) {
        return SoundEvent.createVariableRangeEvent(new ResourceLocation(FireheartCity.MODID, id));
    }

    static final class Session {
        UUID cook;
        BlockPos stove;
        String dish, player;
        int t;
        boolean arrived, boiling;
        final List<UUID> props = new ArrayList<>();
        UUID plate;
        String[] raw, cooked;
    }

    static final Map<UUID, Session> ACTIVE = new HashMap<>();
    static final int COOK_TIME = 260, PLATE_TIME = 60;

    public static void reset() {
        ACTIVE.clear();
    }

    static String[][] ingredients(String source) {
        return switch (source) {
            case "minecraft:cooked_beef" -> new String[][]{{"minecraft:beef"}, {"minecraft:cooked_beef"}};
            case "minecraft:baked_potato" -> new String[][]{{"minecraft:potato", "minecraft:potato"}, {"minecraft:baked_potato", "minecraft:baked_potato"}};
            case "farmersdelight:hamburger" -> new String[][]{{"farmersdelight:minced_beef", "farmersdelight:onion"}, {"farmersdelight:beef_patty", "farmersdelight:onion"}};
            case "farmersdelight:steak_and_potatoes" -> new String[][]{{"minecraft:beef", "minecraft:potato"}, {"minecraft:cooked_beef", "minecraft:baked_potato"}};
            case "minecraft:mushroom_stew", "farmersdelight:noodle_soup" -> new String[][]{{"minecraft:brown_mushroom", "farmersdelight:raw_pasta", "minecraft:red_mushroom"}, {"minecraft:brown_mushroom", "farmersdelight:raw_pasta", "minecraft:red_mushroom"}};
            case "farmersdelight:vegetable_noodles" -> new String[][]{{"farmersdelight:raw_pasta", "minecraft:carrot", "farmersdelight:cabbage"}, {"farmersdelight:raw_pasta", "minecraft:carrot", "farmersdelight:cabbage"}};
            case "farmersdelight:fried_rice" -> new String[][]{{"farmersdelight:rice", "minecraft:egg", "minecraft:carrot"}, {"farmersdelight:rice", "minecraft:egg", "minecraft:carrot"}};
            case "farmersdelight:dumplings" -> new String[][]{{"farmersdelight:wheat_dough", "farmersdelight:minced_beef"}, {"farmersdelight:wheat_dough", "farmersdelight:minced_beef"}};
            case "minecraft:bread" -> new String[][]{{"farmersdelight:wheat_dough|minecraft:wheat"}, {"minecraft:bread"}};
            case "minecraft:cookie" -> new String[][]{{"farmersdelight:wheat_dough|minecraft:wheat", "minecraft:cocoa_beans"}, {"minecraft:cookie", "minecraft:cookie"}};
            case "minecraft:pumpkin_pie" -> new String[][]{{"minecraft:pumpkin", "minecraft:egg"}, {"minecraft:pumpkin_pie"}};
            case "farmersdelight:apple_pie" -> new String[][]{{"minecraft:apple", "minecraft:sugar"}, {"farmersdelight:apple_pie"}};
            case "minecraft:cake" -> new String[][]{{"minecraft:egg", "minecraft:sugar", "minecraft:milk_bucket"}, {"minecraft:cake"}};
            default -> new String[][]{{"minecraft:wheat"}, {"minecraft:bread"}};
        };
    }

    static boolean boils(String source) {
        return source.contains("stew") || source.contains("noodle") || source.contains("soup") || source.contains("dumpling");
    }

    static Job job(String shop) {
        for (String[] s : Extras.SHOPS) if (s[0].equals(shop)) return Job.valueOf(s[2]);
        return null;
    }

    /** Starts the cooking scene for a new order. Returns how long until the dish is ready (ticks). */
    public static int cook(ServerLevel sl, CityData d, String shop, String source, String pn) {
        Job j = job(shop);
        Place place = Place.get(shop);
        if (j == null || place == null) return 200;
        Resident cook = null;
        for (CityData.Profile p : d.profiles.values()) {
            if (p.job != j) continue;
            Resident r = Phones.entity(sl, p);
            if (r != null && r.activityName().equals("work") && r.distanceToSqr(Vec3.atCenterOf(place.pos)) < 30 * 30 && !ACTIVE.containsKey(r.getUUID())) cook = r;
        }
        if (cook == null) return 200;
        BlockPos stove = findStove(sl, place.pos);
        if (stove == null) stove = cook.blockPosition().relative(cook.getDirection()).below();
        Session s = new Session();
        s.cook = cook.getUUID();
        s.stove = stove;
        s.dish = source;
        s.player = pn;
        s.boiling = boils(source);
        String[][] ing = ingredients(source);
        s.raw = ing[0];
        s.cooked = ing[1];
        ACTIVE.put(cook.getUUID(), s);
        return COOK_TIME + PLATE_TIME + 60;
    }

    static BlockPos findStove(ServerLevel sl, BlockPos c) {
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-10, -2, -10), c.offset(10, 3, 10))) {
            BlockState st = sl.getBlockState(p);
            if (st.isAir()) continue;
            var k = ForgeRegistries.BLOCKS.getKey(st.getBlock());
            if (k == null) continue;
            String n = k.getPath();
            if (!(n.contains("stove") || n.equals("smoker") || n.equals("furnace") || n.contains("campfire") || n.contains("oven") || n.contains("range") || n.contains("cooking_pot"))) continue;
            if (!sl.getBlockState(p.above()).isAir() && !n.contains("cooking_pot")) continue;
            double dd = p.distSqr(c);
            if (dd < bd) { bd = dd; best = p.immutable(); }
        }
        return best;
    }

    public static BlockPos target(Resident r) {
        Session s = ACTIVE.get(r.getUUID());
        if (s == null || s.arrived) return null;
        return s.stove;
    }

    public static boolean busy(Resident r) {
        return ACTIVE.containsKey(r.getUUID());
    }

    /** Runs the cook while an order is on the stove. Returns true while cooking. */
    public static boolean tick(Resident r, CityData.Profile p) {
        Session s = ACTIVE.get(r.getUUID());
        if (s == null) return false;
        ServerLevel sl = (ServerLevel) r.level();
        s.t++;
        Vec3 top = new Vec3(s.stove.getX() + 0.5, s.stove.getY() + 1.02, s.stove.getZ() + 0.5);
        if (!s.arrived) {
            if (r.distanceToSqr(top) < 3.2 * 3.2 || s.t > 400) {
                s.arrived = true;
                s.t = 0;
                r.getNavigation().stop();
                r.sayTo(r.pick("Order for " + s.player + "! Firing up the stove.", "One " + Economy.label(s.dish).replaceFirst("^(a|an|some) ", "") + " for " + s.player + ", coming up!", "SolEats order - let's cook!"), 60);
                setup(sl, s, top);
                sl.playSound(null, s.stove, CHOP, SoundSource.BLOCKS, 1f, 1f);
            } else if (s.t % 20 == 1) r.getNavigation().moveTo(top.x, top.y - 1, top.z, 1.1);
            return true;
        }
        r.getNavigation().stop();
        r.getLookControl().setLookAt(top.x, top.y, top.z);
        if (s.t < COOK_TIME) {
            if (s.t % 30 == 0) r.gesture(Resident.G_COOK, 30);
            cooking(sl, s, top);
            if (s.t == COOK_TIME / 2) swapCooked(sl, s);
            return true;
        }
        if (s.t == COOK_TIME) plate(sl, r, s, top);
        if (s.t > COOK_TIME && s.t < COOK_TIME + PLATE_TIME && s.t % 3 == 0) {
            sl.sendParticles(FireheartCity.STEAM.get(), top.x, top.y + 0.4, top.z, 2, 0.08, 0.02, 0.08, 0.01);
        }
        if (s.t >= COOK_TIME + PLATE_TIME) {
            clear(sl, s);
            ACTIVE.remove(r.getUUID());
            r.gesture(Resident.G_GIVE, 30);
            r.sayTo(r.pick("Order up! Courier's on the way, " + s.player + ".", "Plated and packed. Enjoy it hot!", "*ding* Order up!"), 60);
            sl.playSound(null, s.stove, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1f, 1.6f);
            return false;
        }
        return true;
    }

    static void setup(ServerLevel sl, Session s, Vec3 top) {
        String vessel = s.boiling ? "farmersdelight:cooking_pot|minecraft:cauldron" : "farmersdelight:skillet|minecraft:iron_trapdoor";
        Entity pan = display(sl, top, pick(vessel), s.boiling ? 0.75f : 0.8f, 0, s.boiling ? 0.25f : 0.02f, 0, s.boiling ? "fixed" : "ground");
        if (pan != null) s.props.add(pan.getUUID());
        for (int i = 0; i < s.raw.length; i++) {
            double a = i * Math.PI * 2 / s.raw.length;
            Entity e = display(sl, top, pick(s.raw[i]), 0.38f, (float) (Math.cos(a) * 0.14), s.boiling ? 0.45f : 0.12f, (float) (Math.sin(a) * 0.14), "ground");
            if (e != null) s.props.add(e.getUUID());
        }
    }

    static String pick(String spec) {
        for (String alt : spec.split("\\|")) if (ForgeRegistries.ITEMS.containsKey(new ResourceLocation(alt))) return alt;
        return "minecraft:bread";
    }

    static Entity display(ServerLevel sl, Vec3 at, String item, float scale, float dx, float dy, float dz, String mode) {
        CompoundTag t = new CompoundTag();
        t.putString("id", "minecraft:item_display");
        CompoundTag it = new CompoundTag();
        it.putString("id", item);
        it.putByte("Count", (byte) 1);
        t.put("item", it);
        t.putString("item_display", mode);
        t.put("transformation", Repair.transform(dx, dy, dz, scale));
        return spawn(sl, t, at);
    }

    static Entity spawn(ServerLevel sl, CompoundTag t, Vec3 at) {
        Entity e = EntityType.loadEntityRecursive(t, sl, en -> {
            en.moveTo(at.x, at.y, at.z, 0, 0);
            return en;
        });
        if (e != null) {
            e.getPersistentData().putBoolean("fhcKitchenProp", true);
            sl.addFreshEntity(e);
        }
        return e;
    }

    static void cooking(ServerLevel sl, Session s, Vec3 top) {
        if (s.boiling) {
            if (s.t % 2 == 0) sl.sendParticles(ParticleTypes.BUBBLE_POP, top.x, top.y + 0.55, top.z, 2, 0.15, 0.02, 0.15, 0.01);
            if (s.t % 2 == 0) sl.sendParticles(FireheartCity.STEAM.get(), top.x, top.y + 0.7, top.z, 1, 0.12, 0.03, 0.12, 0.01);
            if (s.t % 40 == 1) sl.playSound(null, s.stove, BUBBLE, SoundSource.BLOCKS, 0.7f, 1f);
        } else {
            if (s.t % 3 == 0) sl.sendParticles(ParticleTypes.SMOKE, top.x, top.y + 0.2, top.z, 1, 0.12, 0.02, 0.12, 0.01);
            if (s.t % 7 == 0) sl.sendParticles(ParticleTypes.SMALL_FLAME, top.x, top.y - 0.02, top.z, 1, 0.2, 0, 0.2, 0);
            if (s.t % 30 == 1) sl.playSound(null, s.stove, SIZZLE, SoundSource.BLOCKS, 0.8f, 0.9f + sl.random.nextFloat() * 0.2f);
            if (s.t % 55 == 20) toss(sl, s);
        }
    }

    static void toss(ServerLevel sl, Session s) {
        for (int i = 1; i < s.props.size(); i++) {
            Entity e = sl.getEntity(s.props.get(i));
            if (e == null) continue;
            CompoundTag u = e.saveWithoutId(new CompoundTag());
            double a = sl.random.nextDouble() * Math.PI * 2;
            u.put("transformation", Repair.transform((float) (Math.cos(a) * 0.12), 0.55f, (float) (Math.sin(a) * 0.12), 0.38f));
            u.putInt("interpolation_duration", 5);
            u.putInt("start_interpolation", 0);
            e.load(u);
            UUID id = e.getUUID();
            sl.getServer().tell(new net.minecraft.server.TickTask(sl.getServer().getTickCount() + 6, () -> {
                Entity en = sl.getEntity(id);
                if (en == null) return;
                CompoundTag v = en.saveWithoutId(new CompoundTag());
                double b = sl.random.nextDouble() * Math.PI * 2;
                v.put("transformation", Repair.transform((float) (Math.cos(b) * 0.14), 0.12f, (float) (Math.sin(b) * 0.14), 0.38f));
                v.putInt("interpolation_duration", 5);
                v.putInt("start_interpolation", 0);
                en.load(v);
            }));
        }
        sl.playSound(null, s.stove, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 0.4f, 1.6f);
    }

    static void swapCooked(ServerLevel sl, Session s) {
        for (int i = 1; i < s.props.size() && i - 1 < s.cooked.length; i++) {
            Entity e = sl.getEntity(s.props.get(i));
            if (e == null) continue;
            CompoundTag u = e.saveWithoutId(new CompoundTag());
            CompoundTag it = new CompoundTag();
            it.putString("id", pick(s.cooked[i - 1]));
            it.putByte("Count", (byte) 1);
            u.put("item", it);
            e.load(u);
        }
        sl.sendParticles(ParticleTypes.SMOKE, s.stove.getX() + 0.5, s.stove.getY() + 1.3, s.stove.getZ() + 0.5, 8, 0.15, 0.1, 0.15, 0.02);
    }

    static void plate(ServerLevel sl, Resident r, Session s, Vec3 top) {
        clear(sl, s);
        ItemStack dish = Dishes.make(s.dish, 1, sl.getGameTime());
        CompoundTag t = new CompoundTag();
        t.putString("id", "minecraft:item_display");
        t.put("item", dish.save(new CompoundTag()));
        t.putString("item_display", "ground");
        t.put("transformation", Repair.transform(0, 0.25f, 0, 0.9f));
        Entity e = spawn(sl, t, top);
        if (e != null) s.plate = e.getUUID();
        r.gesture(Resident.G_GIVE, 40);
        sl.playSound(null, s.stove, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.8f, 1.4f);
    }

    static void clear(ServerLevel sl, Session s) {
        for (UUID u : s.props) {
            Entity e = sl.getEntity(u);
            if (e != null) e.discard();
        }
        s.props.clear();
        if (s.plate != null) {
            Entity e = sl.getEntity(s.plate);
            if (e != null) e.discard();
            s.plate = null;
        }
    }

    public static void onJoin(net.minecraftforge.event.entity.EntityJoinLevelEvent e) {
        if (!e.getLevel().isClientSide() && e.loadedFromDisk() && e.getEntity().getPersistentData().getBoolean("fhcKitchenProp")) e.setCanceled(true);
    }
}
