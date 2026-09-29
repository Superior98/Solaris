package com.fireheart.city;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Small helper for placing structures from block-state strings (vanilla or modded, with fallbacks). */
public final class Builder {
    final ServerLevel sl;
    final Map<String, BlockState> cache = new HashMap<>();
    int placed;

    Builder(ServerLevel sl) {
        this.sl = sl;
    }

    /** "mod:block[prop=v]" or "a|b" (first that exists). */
    BlockState st(String spec) {
        return cache.computeIfAbsent(spec, s -> {
            for (String alt : s.split("\\|")) {
                try {
                    return BlockStateParser.parseForBlock(sl.holderLookup(net.minecraft.core.registries.Registries.BLOCK), alt.trim(), false).f_234748_();
                } catch (Exception ignored) {}
            }
            return Blocks.STONE.defaultBlockState();
        });
    }

    void set(int x, int y, int z, String spec) {
        sl.setBlock(new BlockPos(x, y, z), st(spec), 2 | 16);
        placed++;
    }

    void set(int x, int y, int z, BlockState s) {
        sl.setBlock(new BlockPos(x, y, z), s, 2 | 16);
        placed++;
    }

    void fill(int x1, int y1, int z1, int x2, int y2, int z2, String spec) {
        BlockState s = st(spec);
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, s);
    }

    void air(int x1, int y1, int z1, int x2, int y2, int z2) {
        fill(x1, y1, z1, x2, y2, z2, "minecraft:air");
    }

    /** Walls of a box (no floor/roof). */
    void walls(int x1, int y1, int z1, int x2, int y2, int z2, String spec) {
        fill(x1, y1, z1, x2, y2, z1, spec);
        fill(x1, y1, z2, x2, y2, z2, spec);
        fill(x1, y1, z1, x1, y2, z2, spec);
        fill(x2, y1, z1, x2, y2, z2, spec);
    }

    void nbt(int x, int y, int z, String tag) {
        BlockEntity be = sl.getBlockEntity(new BlockPos(x, y, z));
        if (be == null) return;
        try {
            CompoundTag t = be.saveWithFullMetadata();
            t.merge(TagParser.parseTag(tag));
            be.load(t);
            be.setChanged();
        } catch (Exception ignored) {}
    }

    void sign(int x, int y, int z, String spec, String... lines) {
        set(x, y, z, spec);
        BlockEntity be = sl.getBlockEntity(new BlockPos(x, y, z));
        if (be instanceof SignBlockEntity s) {
            var text = s.getFrontText();
            for (int i = 0; i < Math.min(4, lines.length); i++) text = text.setMessage(i, Component.literal(lines[i]));
            text = text.setHasGlowingText(true);
            s.setText(text, true);
            s.setChanged();
        }
    }

    /** Street light: fence post with a lantern on top. */
    void lamp(int x, int y, int z) {
        set(x, y, z, "minecraft:dark_oak_fence");
        set(x, y + 1, z, "minecraft:dark_oak_fence");
        set(x, y + 2, z, "minecraft:dark_oak_fence");
        set(x, y + 3, z, "minecraft:lantern");
    }

    void update(int x1, int y1, int z1, int x2, int y2, int z2) {
        for (int x = x1; x <= x2; x += 16) for (int z = z1; z <= z2; z += 16) {
            var ch = sl.getChunkAt(new BlockPos(x, y1, z));
            ch.setUnsaved(true);
        }
    }
}
