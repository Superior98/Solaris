package com.fireheart.city;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Makes players' pet parrots extra cute: they dance whenever their owner is listening to music, flutter over to
 * greet them, nuzzle them with hearts, chirp and bob happily, and love being petted (sneak + right-click).
 */
public final class ParrotLove {
    private ParrotLove() {}

    static final Map<UUID, Long> GREETED = new HashMap<>();
    static final Map<UUID, Long> PETTED = new HashMap<>();

    static boolean music(ServerPlayer pl) {
        var head = pl.getItemBySlot(EquipmentSlot.HEAD);
        long now = pl.serverLevel().getGameTime();
        if (head.getItem() instanceof DeviceItem d && d.kind.equals("headphones") && head.hasTag() && head.getTag().getLong("PlayUntil") > now) return true;
        for (var s : pl.getInventory().items) if (s.getItem() instanceof DeviceItem d && d.kind.equals("headphones") && s.hasTag() && s.getTag().getLong("PlayUntil") > now) return true;
        return Festival.feasting() && pl.blockPosition().closerThan(Festival.PATH, 30);
    }

    public static void tick(ServerLevel sl) {
        long now = sl.getGameTime();
        for (ServerPlayer pl : sl.players()) {
            List<Parrot> birds = sl.getEntitiesOfClass(Parrot.class, pl.getBoundingBox().inflate(24), p -> p.isTame() && pl.getUUID().equals(p.getOwnerUUID()));
            boolean tunes = music(pl);
            for (Parrot p : birds) {
                p.setRecordPlayingNearby(pl.blockPosition(), tunes);
                String name = p.hasCustomName() ? p.getCustomName().getString() : "your parrot";
                Long g = GREETED.get(p.getUUID());
                double dist = p.distanceTo(pl);
                if ((g == null || now - g > 6000) && dist < 16 && !p.isOrderedToSit()) {
                    GREETED.put(p.getUUID(), now);
                    p.getNavigation().moveTo(pl, 1.4);
                    sl.playSound(null, p.blockPosition(), SoundEvents.PARROT_AMBIENT, SoundSource.NEUTRAL, 1f, 1.4f);
                    sl.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 0.6, p.getZ(), 3, 0.2, 0.2, 0.2, 0);
                    pl.displayClientMessage(Component.literal("§a" + cap(name) + " is happy to see you! ♥"), true);
                    continue;
                }
                if (p.isOrderedToSit() || p.isPassenger()) {
                    if (sl.random.nextFloat() < 0.04f) sl.sendParticles(ParticleTypes.NOTE, p.getX(), p.getY() + 0.7, p.getZ(), 1, 0.1, 0.1, 0.1, 0.5);
                    continue;
                }
                float roll = sl.random.nextFloat();
                if (dist < 5 && roll < 0.06f) {
                    Vec3 v = pl.position().subtract(p.position()).normalize().scale(0.2);
                    p.setDeltaMovement(v.x, 0.35, v.z);
                    p.getLookControl().setLookAt(pl, 30, 30);
                    sl.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 0.6, p.getZ(), 1, 0.1, 0.1, 0.1, 0);
                    sl.playSound(null, p.blockPosition(), SoundEvents.PARROT_AMBIENT, SoundSource.NEUTRAL, 0.7f, 1.6f + sl.random.nextFloat() * 0.3f);
                } else if (dist > 10 && dist < 24 && roll < 0.2f) {
                    p.getNavigation().moveTo(pl, 1.3);
                } else if (roll < 0.03f) {
                    p.setDeltaMovement(0, 0.4, 0);
                    sl.sendParticles(ParticleTypes.NOTE, p.getX(), p.getY() + 0.7, p.getZ(), 1, 0.1, 0.1, 0.1, sl.random.nextFloat());
                    sl.playSound(null, p.blockPosition(), SoundEvents.PARROT_AMBIENT, SoundSource.NEUTRAL, 0.6f, 1.8f);
                }
            }
        }
    }

    static String cap(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public static void onInteract(PlayerInteractEvent.EntityInteract e) {
        if (e.getLevel().isClientSide || !(e.getTarget() instanceof Parrot p) || !(e.getEntity() instanceof ServerPlayer pl)) return;
        if (e.getHand() != InteractionHand.MAIN_HAND || !pl.isShiftKeyDown() || !pl.getMainHandItem().isEmpty() || !p.isTame() || !pl.getUUID().equals(p.getOwnerUUID())) return;
        ServerLevel sl = pl.serverLevel();
        long now = sl.getGameTime();
        Long last = PETTED.get(p.getUUID());
        e.setCanceled(true);
        e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        if (last != null && now - last < 20) return;
        PETTED.put(p.getUUID(), now);
        p.getLookControl().setLookAt(pl, 30, 30);
        p.setDeltaMovement(0, 0.25, 0);
        sl.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 0.7, p.getZ(), 5, 0.3, 0.2, 0.3, 0);
        sl.playSound(null, p.blockPosition(), SoundEvents.PARROT_AMBIENT, SoundSource.NEUTRAL, 1f, 1.9f);
        String name = p.hasCustomName() ? p.getCustomName().getString() : "Your parrot";
        String[] lines = {" leans into your hand. ♥", " chirps happily!", " fluffs up its feathers.", " nibbles your finger gently.", " does a little dance!", " says: \"Pretty bird! Pretty bird!\""};
        pl.displayClientMessage(Component.literal("§a" + name + lines[sl.random.nextInt(lines.length)]), true);
        p.heal(2);
    }
}
