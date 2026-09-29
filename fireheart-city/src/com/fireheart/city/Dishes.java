package com.fireheart.city;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * SolEats dishes: every menu item arrives as a plated, fully modelled dish (burger stack, ramen bowl with chopsticks,
 * steamer of dumplings...). A dish stays hot for a few minutes after it leaves the kitchen - it steams while hot and a
 * hot meal warms you up.
 */
public final class Dishes {
    private Dishes() {}

    public record Dish(String key, String name, String source, boolean bowl, boolean hot) {}

    public static final Map<String, Dish> BY_SOURCE = new LinkedHashMap<>();
    public static final Map<String, Dish> BY_KEY = new LinkedHashMap<>();
    public static final List<Dish> ALL = List.of(
            new Dish("steak", "Grilled Steak", "minecraft:cooked_beef", false, true),
            new Dish("baked_potato", "Loaded Baked Potato", "minecraft:baked_potato", false, true),
            new Dish("burger", "Diesel Double Burger", "farmersdelight:hamburger", false, true),
            new Dish("steak_potatoes", "Steak & Potatoes", "farmersdelight:steak_and_potatoes", false, true),
            new Dish("mushroom_stew", "Mushroom Stew", "minecraft:mushroom_stew", true, true),
            new Dish("ramen", "Neon Ramen", "farmersdelight:noodle_soup", true, true),
            new Dish("veg_noodles", "Veggie Noodles", "farmersdelight:vegetable_noodles", true, true),
            new Dish("fried_rice", "Wok Fried Rice", "farmersdelight:fried_rice", true, true),
            new Dish("dumplings", "Steamed Dumplings", "farmersdelight:dumplings", false, true),
            new Dish("bread", "Fresh Loaf", "minecraft:bread", false, true),
            new Dish("cookies", "Choc Chip Cookies", "minecraft:cookie", false, false),
            new Dish("pumpkin_pie", "Pumpkin Pie", "minecraft:pumpkin_pie", false, false),
            new Dish("apple_pie", "Lattice Apple Pie", "farmersdelight:apple_pie", false, false),
            new Dish("cake", "Celebration Cake", "minecraft:cake", false, false));
    public static final long HOT_TICKS = 20 * 60 * 15;

    static {
        for (Dish d : ALL) {
            BY_SOURCE.put(d.source(), d);
            BY_KEY.put(d.key(), d);
        }
    }

    public static int index(ItemStack s) {
        Dish d = of(s);
        return d == null ? 0 : ALL.indexOf(d);
    }

    public static Dish of(ItemStack s) {
        return s.hasTag() ? BY_KEY.get(s.getTag().getString("Dish")) : null;
    }

    public static ItemStack make(String sourceId, int count, long cookedAt) {
        Dish d = BY_SOURCE.get(sourceId);
        if (d == null) return new ItemStack(Inv.item(sourceId), count);
        ItemStack st = new ItemStack(FireheartCity.DISH.get(), count);
        CompoundTag t = st.getOrCreateTag();
        t.putString("Dish", d.key());
        t.putLong("CookedAt", cookedAt);
        return st;
    }

    public static String nbt(String sourceId, long cookedAt) {
        Dish d = BY_SOURCE.get(sourceId);
        return d == null ? "" : "{Dish:\"" + d.key() + "\",CookedAt:" + cookedAt + "L}";
    }

    /** 0..1 heat: 1 fresh out of the kitchen, 0 cold. */
    public static float heat(ItemStack s, Level level) {
        Dish d = of(s);
        if (d == null || !d.hot() || level == null) return 0;
        long age = level.getGameTime() - s.getTag().getLong("CookedAt");
        if (age < 0) return 1;
        return Math.max(0, 1 - age / (float) HOT_TICKS);
    }

    public static class DishItem extends Item {
        public DishItem(Properties p) {
            super(p);
        }

        @Override
        public FoodProperties getFoodProperties(ItemStack stack, LivingEntity entity) {
            Dish d = of(stack);
            Item src = d == null ? Items.BREAD : Inv.item(d.source());
            FoodProperties fp = src.getFoodProperties(new ItemStack(src), entity);
            if (fp == null && d != null && d.key().equals("cake")) return new FoodProperties.Builder().nutrition(12).saturationMod(0.4f).build();
            return fp != null ? fp : new FoodProperties.Builder().nutrition(6).saturationMod(0.6f).build();
        }

        @Override
        public Component getName(ItemStack stack) {
            Dish d = of(stack);
            return Component.literal(d == null ? "SolEats Dish" : d.name());
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity e) {
            float h = heat(stack, level);
            Dish d = of(stack);
            ItemStack out = super.finishUsingItem(stack, level, e);
            if (!level.isClientSide) {
                if (h > 0.05f) {
                    e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, (int) (60 + 140 * h), 0));
                    if (e instanceof ServerPlayer sp) sp.displayClientMessage(Component.literal("§6♨ Mmm, still hot!"), true);
                }
                if (d != null && d.bowl() && e instanceof ServerPlayer sp && !sp.getAbilities().instabuild) {
                    if (out.isEmpty()) return new ItemStack(Items.BOWL);
                    sp.getInventory().add(new ItemStack(Items.BOWL));
                }
            }
            return out;
        }

        @Override
        public void appendHoverText(ItemStack st, Level level, List<Component> tip, TooltipFlag flag) {
            Dish d = of(st);
            if (d == null) return;
            float h = heat(st, level);
            if (d.hot()) tip.add(Component.literal(h > 0.6f ? "§c♨ Piping hot" : h > 0.05f ? "§6♨ Warm" : "§7Gone cold"));
            tip.add(Component.literal("§8From SolEats"));
        }
    }
}
