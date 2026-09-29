package com.fireheart.city;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The FireTube TV: right-click to watch FireTube on the big screen. Charges a FirePhone nearby. */
public class TvBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty ON = BooleanProperty.create("on");
    private static final VoxelShape NS = Shapes.or(Block.box(5, 0, 5, 11, 2, 11), Block.box(7, 2, 7, 9, 4, 9), Block.box(0, 4, 6, 16, 16, 10));
    private static final VoxelShape EW = Shapes.or(Block.box(5, 0, 5, 11, 2, 11), Block.box(7, 2, 7, 9, 4, 9), Block.box(6, 4, 0, 10, 16, 16));

    public TvBlock(Properties p) {
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

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) {
            try {
                if (pos.equals(Expansion.BOOTH_TV)) {
                    String link = bookLink(sp.getItemInHand(hand));
                    if (link != null) {
                        TvShows.cinema(sp.serverLevel(), link, sp.getName().getString());
                        sp.displayClientMessage(net.minecraft.network.chat.Component.literal("§6Now showing on the big screen: §f" + link), true);
                        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.8f, 1.2f);
                        return InteractionResult.CONSUME;
                    }
                }
                if (!state.getValue(ON)) level.setBlock(pos, state.setValue(ON, true), 3);
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.5f, 1.4f);
                Computers.openFor(sp, pos);
            } catch (Throwable t) {
                FireheartCity.LOG.error("TV open failed", t);
            }
        }
        return InteractionResult.CONSUME;
    }

    static String bookLink(ItemStack st) {
        if (st.isEmpty() || !st.hasTag() || !(st.is(net.minecraft.world.item.Items.WRITABLE_BOOK) || st.is(net.minecraft.world.item.Items.WRITTEN_BOOK))) return null;
        var pages = st.getTag().getList("pages", 8);
        if (pages.isEmpty()) return null;
        String text = pages.getString(0);
        if (st.is(net.minecraft.world.item.Items.WRITTEN_BOOK)) {
            try {
                var comp = net.minecraft.network.chat.Component.Serializer.fromJson(text);
                if (comp != null) text = comp.getString();
            } catch (Exception ignored) {}
        }
        text = text.trim().replace("\n", " ").split("\\s+")[0];
        return text.isEmpty() ? null : text;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState st, LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, st, by, stack);
        if (!level.isClientSide && level instanceof ServerLevel sl) {
            CityData d = CityData.get(sl);
            d.tvs.put(pos.asLong(), by instanceof Player p ? p.getName().getString() : "");
            d.setDirty();
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState now, boolean moving) {
        if (!level.isClientSide && !now.is(this) && level instanceof ServerLevel sl) {
            CityData d = CityData.get(sl);
            d.tvs.remove(pos.asLong());
            d.pcs.remove(pos.asLong());
            d.setDirty();
        }
        super.onRemove(state, level, pos, now, moving);
    }
}
