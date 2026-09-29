package com.fireheart.city;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A SolEats delivery box left at the front door; right-click to open it and take the food. */
public class ParcelBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 8, 13);

    public ParcelBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    public static void leave(ServerLevel sl, CityData d, BlockPos pos, Direction facing, String owner, List<ItemStack> items) {
        sl.setBlock(pos, FireheartCity.PARCEL.get().defaultBlockState().setValue(FACING, facing), 3);
        List<String> l = new ArrayList<>();
        l.add(owner);
        for (ItemStack s : items) if (!s.isEmpty()) l.add(s.save(new CompoundTag()).toString());
        d.parcels.put(pos.asLong(), l);
        d.setDirty();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState now, boolean moving) {
        if (!now.is(this) && level instanceof ServerLevel sl) {
            CityData d = CityData.get(sl);
            List<String> l = d.parcels.remove(pos.asLong());
            if (l != null) for (int i = 1; i < l.size(); i++) {
                try { Block.popResource(level, pos, ItemStack.of(TagParser.parseTag(l.get(i)))); } catch (Exception ignored) {}
            }
            d.setDirty();
        }
        super.onRemove(state, level, pos, now, moving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel sl)) return InteractionResult.CONSUME;
        CityData d = CityData.get(sl);
        List<String> l = d.parcels.remove(pos.asLong());
        if (l != null) {
            for (int i = 1; i < l.size(); i++) {
                try {
                    ItemStack s = ItemStack.of(TagParser.parseTag(l.get(i)));
                    if (!sp.getInventory().add(s)) sp.drop(s, false);
                } catch (Exception ignored) {}
            }
            sp.displayClientMessage(Component.literal("§6You opened your SolEats delivery. Enjoy!"), true);
        }
        d.setDirty();
        sl.playSound(null, pos, SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.BLOCKS, 0.8f, 1f);
        sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, Blocks.BIRCH_PLANKS.defaultBlockState()), pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 20, 0.25, 0.2, 0.25, 0.05);
        sl.removeBlock(pos, false);
        return InteractionResult.CONSUME;
    }
}
