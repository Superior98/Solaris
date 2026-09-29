package com.fireheart.city;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
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

/** A player's mailbox: the postman puts letters and gifts inside and raises the red flag. */
public class MailboxBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty FLAG = BooleanProperty.create("flag");
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    private static final VoxelShape SHAPE = Shapes.or(Block.box(7, 0, 7, 9, 10, 9), Block.box(4, 10, 3, 12, 16, 13));

    public MailboxBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FLAG, false).setValue(OPEN, false));
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, FLAG, OPEN);
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
    public void setPlacedBy(Level level, BlockPos pos, BlockState st, LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, st, by, stack);
        if (level instanceof ServerLevel sl && by instanceof Player p) {
            CityData d = CityData.get(sl);
            d.mailboxes.put(pos.asLong(), p.getName().getString());
            d.setDirty();
            p.displayClientMessage(Component.literal("§6This is now your mailbox - the postman will leave your letters and gifts here."), true);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState now, boolean moving) {
        if (!now.is(this) && level instanceof ServerLevel sl) {
            CityData d = CityData.get(sl);
            for (ItemStack s : contents(d, pos)) Block.popResource(level, pos, s);
            d.mailboxes.remove(pos.asLong());
            d.mail.remove(pos.asLong());
            d.setDirty();
        }
        super.onRemove(state, level, pos, now, moving);
    }

    public static List<ItemStack> contents(CityData d, BlockPos pos) {
        List<ItemStack> out = new ArrayList<>();
        for (String s : d.mail.getOrDefault(pos.asLong(), List.of())) {
            try { out.add(ItemStack.of(TagParser.parseTag(s))); } catch (Exception ignored) {}
        }
        return out;
    }

    public static void store(CityData d, BlockPos pos, List<ItemStack> items) {
        List<String> l = new ArrayList<>();
        for (ItemStack s : items) if (!s.isEmpty()) l.add(s.save(new CompoundTag()).toString());
        if (l.isEmpty()) d.mail.remove(pos.asLong());
        else d.mail.put(pos.asLong(), l);
        d.setDirty();
    }

    public static BlockPos of(CityData d, String player) {
        for (var e : d.mailboxes.entrySet()) if (e.getValue().equalsIgnoreCase(player)) return BlockPos.of(e.getKey());
        return null;
    }

    public static boolean deposit(ServerLevel sl, CityData d, BlockPos pos, List<ItemStack> items) {
        if (!(sl.getBlockState(pos).getBlock() instanceof MailboxBlock)) return false;
        List<ItemStack> in = contents(d, pos);
        in.addAll(items);
        while (in.size() > 9) Block.popResource(sl, pos.above(), in.remove(0));
        store(d, pos, in);
        sl.setBlock(pos, sl.getBlockState(pos).setValue(FLAG, true), 3);
        return true;
    }

    public static void setOpen(ServerLevel sl, BlockPos pos, boolean open) {
        BlockState s = sl.getBlockState(pos);
        if (s.getBlock() instanceof MailboxBlock && s.getValue(OPEN) != open) {
            sl.setBlock(pos, s.setValue(OPEN, open), 3);
            sl.playSound(null, pos, open ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.6f, 1.3f);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel sl)) return InteractionResult.CONSUME;
        CityData d = CityData.get(sl);
        String owner = d.mailboxes.get(pos.asLong());
        String me = sp.getName().getString();
        if (owner == null) {
            d.mailboxes.put(pos.asLong(), me);
            d.setDirty();
            sp.displayClientMessage(Component.literal("§6You claimed this mailbox."), true);
            owner = me;
        }
        if (!owner.equalsIgnoreCase(me) && !sp.hasPermissions(2)) {
            sp.displayClientMessage(Component.literal("§7This is " + owner + "'s mailbox."), true);
            return InteractionResult.CONSUME;
        }
        SimpleContainer box = new SimpleContainer(9);
        List<ItemStack> in = contents(d, pos);
        for (int i = 0; i < Math.min(9, in.size()); i++) box.setItem(i, in.get(i));
        box.addListener(c -> {
            List<ItemStack> now = new ArrayList<>();
            for (int i = 0; i < box.getContainerSize(); i++) now.add(box.getItem(i).copy());
            store(d, pos, now);
            BlockState st = sl.getBlockState(pos);
            if (st.getBlock() instanceof MailboxBlock && box.isEmpty() && st.getValue(FLAG)) sl.setBlock(pos, st.setValue(FLAG, false), 3);
        });
        setOpen(sl, pos, true);
        Phones.LATER.add(new Object[]{sl.getGameTime() + 60, (Runnable) () -> setOpen(sl, pos, false)});
        sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1, id, inv, box, 1), Component.literal(owner + "'s Mailbox")));
        return InteractionResult.CONSUME;
    }
}
