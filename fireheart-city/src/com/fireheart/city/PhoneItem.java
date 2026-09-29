package com.fireheart.city;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** The FirePhone: right-click to open FirePhone OS (messages, calls, FireFeed, map, games...). */
public class PhoneItem extends Item {
    public PhoneItem(Properties p) {
        super(p);
    }

    public static int color(ItemStack st) {
        return st.hasTag() ? Math.floorMod(st.getTag().getInt("Color"), Phones.COLORS.length) : 0;
    }

    public static ItemStack make(int color) {
        return make(color, 1);
    }

    public static ItemStack make(int color, int model) {
        ItemStack st = new ItemStack(FireheartCity.PHONE.get());
        st.getOrCreateTag().putInt("Color", Math.floorMod(color, Phones.COLORS.length));
        if (model > 1) st.getTag().putInt("Model", model);
        return st;
    }

    public static int model(ItemStack st) {
        return st.hasTag() ? Math.max(1, st.getTag().getInt("Model")) : 1;
    }

    @Override
    public Component getName(ItemStack st) {
        return Component.literal((model(st) >= 2 ? "SolPhone 2" : "SolPhone") + " (" + Phones.colorName(color(st)) + ")");
    }

    @Override
    public void appendHoverText(ItemStack st, Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("§7Right-click or press §eP§7 to open"));
        tip.add(Component.literal("§8Messages · Calls · SolFeed · Map · Games"));
        int b = Extras.battery(st);
        tip.add(Component.literal((b <= 20 ? "§c" : "§a") + "Battery " + b + "%"));
        int c = st.hasTag() ? st.getTag().getInt("Case") : 0;
        if (c > 0) tip.add(Component.literal("§7Case: " + Extras.CASES[Math.floorMod(c, Extras.CASES.length)]));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            try {
                level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.4f, 1.9f);
                Phones.open(sp);
            } catch (Throwable t) {
                FireheartCity.LOG.error("Phone open failed", t);
            }
        }
        return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
    }
}
