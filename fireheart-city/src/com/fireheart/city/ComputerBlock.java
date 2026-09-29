package com.fireheart.city;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The Fireheart PC: a desktop computer with a working FireOS interface (apps and games). */
public class ComputerBlock extends HorizontalDirectionalBlock {
    public static final IntegerProperty SCREEN = IntegerProperty.create("screen", 0, 3);
    private static final VoxelShape[] SHAPES = new VoxelShape[4];

    static {
        for (Direction d : Direction.Plane.HORIZONTAL) SHAPES[d.get2DDataValue()] = shape(d);
    }

    public ComputerBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SCREEN, 0));
    }

    private static VoxelShape box(Direction d, double x0, double y0, double z0, double x1, double y1, double z1) {
        double ax0 = x0, az0 = z0, ax1 = x1, az1 = z1;
        switch (d) {
            case SOUTH -> { ax0 = 16 - x1; ax1 = 16 - x0; az0 = 16 - z1; az1 = 16 - z0; }
            case EAST -> { ax0 = 16 - z1; ax1 = 16 - z0; az0 = x0; az1 = x1; }
            case WEST -> { ax0 = z0; ax1 = z1; az0 = 16 - x1; az1 = 16 - x0; }
            default -> {}
        }
        return Block.box(ax0, y0, az0, ax1, y1, az1);
    }

    private static VoxelShape shape(Direction d) {
        return Shapes.or(box(d, 2, 0, 1, 14, 1, 6), box(d, 5, 0, 8, 11, 4, 12), box(d, 1, 4, 9, 15, 13, 11));
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return SHAPES[s.getValue(FACING).get2DDataValue()];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, SCREEN);
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

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) {
            try {
                if (state.getValue(SCREEN) == 0) level.setBlock(pos, state.setValue(SCREEN, 1), 3);
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.5f, 1.6f);
                Computers.openFor(sp, pos);
            } catch (Throwable t) {
                FireheartCity.LOG.error("Computer open failed", t);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide && !old.is(this) && level instanceof ServerLevel sl) Computers.register(sl, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState now, boolean moving) {
        if (!level.isClientSide && !now.is(this) && level instanceof ServerLevel sl) Computers.unregister(sl, pos);
        super.onRemove(state, level, pos, now, moving);
    }
}
