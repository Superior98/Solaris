package com.fireheart.city;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Realistic ceiling lights (panel, round, spotlight, pendant): place under a ceiling, right-click to switch. */
public class CeilingLightBlock extends Block {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    final VoxelShape shape;

    public CeilingLightBlock(Properties p, String kind) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(LIT, true));
        shape = switch (kind) {
            case "panel" -> Block.box(1, 15, 1, 15, 16, 15);
            case "round" -> Block.box(3, 14, 3, 13, 16, 13);
            case "spot" -> Block.box(5.5, 13, 5.5, 10.5, 16, 10.5);
            default -> Shapes.or(Block.box(7.5, 8, 7.5, 8.5, 16, 8.5), Block.box(4, 3, 4, 12, 8, 12));
        };
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return Shapes.empty();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(LIT);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            level.setBlock(pos, state.cycle(LIT), 3);
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.5f, state.getValue(LIT) ? 0.8f : 1.2f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
