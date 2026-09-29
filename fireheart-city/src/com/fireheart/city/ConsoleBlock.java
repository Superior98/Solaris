package com.fireheart.city;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The SolBox console: right-click to switch it on; it plays on the nearest SolTube TV and you hold the controller. */
public class ConsoleBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty ON = BooleanProperty.create("on");
    private static final VoxelShape NS = Block.box(2, 0, 4, 14, 4, 12);
    private static final VoxelShape EW = Block.box(4, 0, 2, 12, 4, 14);

    public ConsoleBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ON, false));
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return s.getValue(FACING).getAxis() == Direction.Axis.Z ? NS : EW;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, ON);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState s, Rotation r) {
        return s.setValue(FACING, r.rotate(s.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState s, Mirror m) {
        return s.rotate(m.getRotation(s.getValue(FACING)));
    }

    public static BlockPos findTv(Level level, BlockPos pos) {
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-6, -3, -6), pos.offset(6, 4, 6))) {
            if (!(level.getBlockState(p).getBlock() instanceof TvBlock)) continue;
            double d = p.distSqr(pos);
            if (d < bd) { bd = d; best = p.immutable(); }
        }
        return best;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.CONSUME;
        BlockPos tv = findTv(level, pos);
        if (tv == null) {
            sp.displayClientMessage(Component.literal("§cThe SolBox needs a SolTube TV nearby (within 6 blocks)."), true);
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.6f, 0.6f);
            return InteractionResult.CONSUME;
        }
        if (!state.getValue(ON)) level.setBlock(pos, state.setValue(ON, true), 3);
        BlockState ts = level.getBlockState(tv);
        if (!ts.getValue(TvBlock.ON)) level.setBlock(tv, ts.setValue(TvBlock.ON, true), 3);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.5f, 1.6f);
        Computers.openFor(sp, pos);
        return InteractionResult.CONSUME;
    }
}
