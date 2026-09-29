package com.fireheart.city;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** FireTech gadgets: FirePad tablet, FireStation console, FireWatch and FireBeats headphones. */
public class DeviceItem extends Item implements net.minecraft.world.item.Equipable {
    public final String kind;

    public DeviceItem(String kind, Properties p) {
        super(p);
        this.kind = kind;
    }

    public static ItemStack headphones(int color, boolean magma) {
        ItemStack s = new ItemStack(FireheartCity.HEADPHONES.get());
        s.getOrCreateTag().putInt("Color", color);
        if (magma) {
            s.getTag().putBoolean("Magma", true);
            s.setHoverName(Component.literal("§6SolBeats §cMagma Edition").withStyle(st -> st.withItalic(false)));
        }
        return s;
    }

    public static int headphoneColor(ItemStack s) {
        return s.hasTag() ? Math.floorMod(s.getTag().getInt("Color"), Phones.COLORS.length) : 0;
    }

    public static boolean magma(ItemStack s) {
        return s.hasTag() && s.getTag().getBoolean("Magma");
    }

    static final List<SoundEvent> TRACKS = List.of(SoundEvents.MUSIC_DISC_CAT, SoundEvents.MUSIC_DISC_BLOCKS, SoundEvents.MUSIC_DISC_CHIRP, SoundEvents.MUSIC_DISC_MALL, SoundEvents.MUSIC_DISC_MELLOHI, SoundEvents.MUSIC_DISC_STAL, SoundEvents.MUSIC_DISC_STRAD, SoundEvents.MUSIC_DISC_FAR, SoundEvents.MUSIC_DISC_WAIT);
    static final String[] TRACK_NAMES = {"Cat", "Blocks", "Chirp", "Mall", "Mellohi", "Stal", "Strad", "Far", "Wait"};

    @Override
    public net.minecraft.world.entity.EquipmentSlot getEquipmentSlot() {
        return kind.equals("headphones") ? net.minecraft.world.entity.EquipmentSlot.HEAD : net.minecraft.world.entity.EquipmentSlot.MAINHAND;
    }

    @Override
    public net.minecraft.world.entity.EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return kind.equals("headphones") ? net.minecraft.world.entity.EquipmentSlot.HEAD : null;
    }

    /** Takes the headphones in both hands and puts them on; music starts once they're on. */
    static void wear(ServerPlayer sp, InteractionHand hand, ItemStack st) {
        ItemStack head = sp.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, st.copy());
        sp.setItemInHand(hand, head);
        Extras.relayAll(sp, "#deva|" + sp.getId() + "|don");
        sp.serverLevel().playSound(null, sp.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 0.6f, 1.3f);
        WORN_START.put(sp.getUUID(), sp.serverLevel().getGameTime() + 14);
    }

    static final java.util.Map<java.util.UUID, Long> WORN_START = new java.util.HashMap<>();

    /** Every second: worn headphones keep playing (next track when one ends); taking them off stops the music. */
    public static void tickWorn(ServerPlayer sp) {
        ItemStack head = sp.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        long now = sp.serverLevel().getGameTime();
        boolean worn = head.getItem() instanceof DeviceItem di && di.kind.equals("headphones");
        Long start = WORN_START.get(sp.getUUID());
        if (worn) {
            if (start != null && now < start) return;
            if (magma(head)) {
                sp.serverLevel().sendParticles(ParticleTypes.SMALL_FLAME, sp.getX(), sp.getY() + 1.75, sp.getZ(), 2, 0.25, 0.1, 0.25, 0.005);
                if (sp.getRandom().nextInt(3) == 0) sp.serverLevel().sendParticles(ParticleTypes.LAVA, sp.getX(), sp.getY() + 1.9, sp.getZ(), 1, 0.2, 0.05, 0.2, 0);
            }
            long until = head.hasTag() ? head.getTag().getLong("PlayUntil") : 0;
            if (until <= now && !head.getOrCreateTag().getBoolean("Paused")) toggleMusic(sp, head);
            WORN_START.put(sp.getUUID(), 0L);
        } else if (start != null) {
            WORN_START.remove(sp.getUUID());
            sp.connection.send(new ClientboundStopSoundPacket(null, SoundSource.RECORDS));
            for (ItemStack s : sp.getInventory().items) if (s.getItem() instanceof DeviceItem d && d.kind.equals("headphones") && s.hasTag()) s.getTag().putLong("PlayUntil", 0);
            sp.displayClientMessage(Component.literal("§d♫ Headphones off"), true);
        }
    }

    @Override
    public void appendHoverText(ItemStack st, Level level, List<Component> tip, TooltipFlag flag) {
        switch (kind) {
            case "tablet" -> tip.add(Component.literal("§7Right-click to open SolOS anywhere"));
            case "console" -> tip.add(Component.literal("§7Right-click to play the SolOS games"));
            case "watch" -> {
                tip.add(Component.literal("§7Keep it in your inventory: time and messages on your HUD"));
                tip.add(Component.literal("§7Right-click to read your latest message"));
            }
            case "headphones" -> {
                tip.add(Component.literal(magma(st) ? "§6Magma Edition §7- smoulders while you listen" : "§7Colour: §f" + Phones.COLORS[headphoneColor(st)]));
                tip.add(Component.literal("§7Right-click to put them on - music plays while you wear them"));
                tip.add(Component.literal("§7Sneak + right-click to skip to the next track"));
            }
            default -> {}
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
        try {
            switch (kind) {
                case "tablet" -> {
                    level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.4f, 1.7f);
                    Computers.openFor(sp, Extras.TABLET);
                }
                case "console" -> {
                    level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.5f, 1.2f);
                    Computers.openFor(sp, Extras.CONSOLE);
                }
                case "watch" -> Extras.watchGlance(sp);
                case "headphones" -> {
                    if (player.isShiftKeyDown()) toggleMusic(sp, st);
                    else wear(sp, hand, st);
                }
                default -> {}
            }
        } catch (Throwable t) {
            FireheartCity.LOG.error("Device use failed", t);
        }
        return InteractionResultHolder.sidedSuccess(st, false);
    }

    static void toggleMusic(ServerPlayer sp, ItemStack st) {
        long now = sp.serverLevel().getGameTime();
        boolean playing = st.hasTag() && st.getTag().getLong("PlayUntil") > now;
        sp.connection.send(new ClientboundStopSoundPacket(null, SoundSource.RECORDS));
        if (playing) {
            st.getOrCreateTag().putLong("PlayUntil", 0);
            sp.displayClientMessage(Component.literal("§d♫ Music stopped"), true);
            return;
        }
        int i = Math.floorMod(st.getOrCreateTag().getInt("Track") + 1, TRACKS.size());
        st.getTag().putInt("Track", i);
        st.getTag().putLong("PlayUntil", now + 20 * 60 * 3);
        sp.connection.send(new ClientboundSoundEntityPacket(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(TRACKS.get(i)), SoundSource.RECORDS, sp, 0.6f, 1f, sp.getRandom().nextLong()));
        sp.serverLevel().sendParticles(ParticleTypes.NOTE, sp.getX(), sp.getY() + 2.1, sp.getZ(), 3, 0.3, 0.2, 0.3, 0.5);
        sp.displayClientMessage(Component.literal("§d♫ SolBeats: now playing §f" + TRACK_NAMES[i]), true);
    }
}
