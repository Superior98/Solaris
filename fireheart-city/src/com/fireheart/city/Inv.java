package com.fireheart.city;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

/** Real container access (chests, hoppers, depots, cabinets, barrels) through Forge's item handler capability. */
public final class Inv {
    private Inv() {}

    public static Item item(String id) {
        Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return it == null ? Items.AIR : it;
    }

    public static String id(Item it) {
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(it);
        return rl == null ? "minecraft:air" : rl.toString();
    }

    public static IItemHandler handler(ServerLevel l, BlockPos pos) {
        if (!l.isLoaded(pos)) return null;
        BlockEntity be = l.getBlockEntity(pos);
        if (be == null) return null;
        return be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
    }

    public static int count(ServerLevel l, BlockPos pos, String id) {
        IItemHandler h = handler(l, pos);
        if (h == null) return 0;
        Item it = item(id);
        int n = 0;
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (s.getItem() == it) n += s.getCount();
        }
        return n;
    }

    public static int take(ServerLevel l, BlockPos pos, String id, int max) {
        IItemHandler h = handler(l, pos);
        if (h == null) return 0;
        Item it = item(id);
        int got = 0;
        for (int i = 0; i < h.getSlots() && got < max; i++) {
            ItemStack s = h.getStackInSlot(i);
            if (s.getItem() != it) continue;
            ItemStack out = h.extractItem(i, max - got, false);
            got += out.getCount();
        }
        return got;
    }

    public static String takeAny(ServerLevel l, BlockPos pos) {
        IItemHandler h = handler(l, pos);
        if (h == null) return null;
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (s.isEmpty()) continue;
            ItemStack out = h.extractItem(i, 1, false);
            if (!out.isEmpty()) return id(out.getItem());
        }
        return null;
    }

    public static int put(ServerLevel l, BlockPos pos, String id, int n) {
        IItemHandler h = handler(l, pos);
        if (h == null || n <= 0) return 0;
        Item it = item(id);
        if (it == Items.AIR) return 0;
        ItemStack rest = new ItemStack(it, n);
        for (int i = 0; i < h.getSlots() && !rest.isEmpty(); i++) rest = h.insertItem(i, rest, false);
        return n - rest.getCount();
    }

    public static String firstItem(ServerLevel l, BlockPos pos) {
        IItemHandler h = handler(l, pos);
        if (h == null) return null;
        for (int i = 0; i < h.getSlots(); i++) if (!h.getStackInSlot(i).isEmpty()) return id(h.getStackInSlot(i).getItem());
        return null;
    }

    public static int total(ServerLevel l, BlockPos pos) {
        IItemHandler h = handler(l, pos);
        if (h == null) return 0;
        int n = 0;
        for (int i = 0; i < h.getSlots(); i++) n += h.getStackInSlot(i).getCount();
        return n;
    }
}
