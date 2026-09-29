package com.fireheart.city;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingFallEvent;

/**
 * Server side of skydiving. The Sky Launch pad counts down, then the client flies the player (launch, freefall,
 * parachute, landing) and reports each phase back so everyone nearby sees the pose. Divers never take fall damage.
 */
public final class Skydive {
    private Skydive() {}

    public static final int NONE = 0, LAUNCH = 1, FREEFALL = 2, CHUTE = 3, LANDED = 4;
    public static final int APEX = 360;

    static final class Diver {
        int phase;
        long since;
        boolean hadFly;
        BlockPos pad;
        int countdown = -1;
    }

    static final Map<UUID, Diver> DIVERS = new HashMap<>();
    static final Map<UUID, Long> SAFE = new HashMap<>();

    public static boolean diving(Player p) {
        Diver d = DIVERS.get(p.getUUID());
        return d != null && d.phase > NONE;
    }

    /** Right-click on the pad: a three second countdown while you stay on it. */
    public static void request(ServerPlayer pl, BlockPos pad) {
        Diver d = DIVERS.get(pl.getUUID());
        if (d != null && (d.phase > NONE || d.countdown >= 0)) return;
        d = new Diver();
        d.pad = pad;
        d.countdown = 60;
        DIVERS.put(pl.getUUID(), d);
        pl.displayClientMessage(Component.literal("§6§lSKY LAUNCH §fstand still... §e3"), true);
        pl.serverLevel().playSound(null, pad, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 1f, 0.8f);
    }

    public static void launch(ServerPlayer pl, BlockPos pad) {
        Diver d = DIVERS.computeIfAbsent(pl.getUUID(), k -> new Diver());
        d.countdown = -1;
        d.pad = pad;
        d.hadFly = pl.getAbilities().mayfly;
        pl.getAbilities().mayfly = true;
        pl.getAbilities().flying = false;
        pl.onUpdateAbilities();
        pl.fallDistance = 0;
        ServerLevel sl = pl.serverLevel();
        if (pl.isPassenger()) pl.stopRiding();
        if (pl.isSleeping()) pl.stopSleeping();
        if (pad != null) pl.teleportTo(pad.getX() + 0.5, pad.getY() + 0.2, pad.getZ() + 0.5);
        else {
            int sky = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, pl.getBlockX(), pl.getBlockZ());
            if (sky > pl.getY() + 1) pl.teleportTo(pl.getX(), sky + 0.1, pl.getZ());
        }
        int top = pad == null ? (int) pl.getY() : CityData.get(sl).skyTop;
        phase(pl, d, LAUNCH);
        PcNet.send(pl, new PcNet.Msg("#sky|launch|" + Math.max(APEX, top + 120) + "|" + (pad == null ? "x" : pad.getX() + 0.5 + "|" + (pad.getZ() + 0.5) + "|" + top)));
        sl.playSound(null, pl.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 2f, 0.6f);
        sl.playSound(null, pl.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6f, 1.6f);
        sl.sendParticles(ParticleTypes.EXPLOSION, pl.getX(), pl.getY(), pl.getZ(), 3, 0.5, 0.2, 0.5, 0);
        cheer(sl, pl.blockPosition(), pl.getName().getString());
    }

    static void cheer(ServerLevel sl, BlockPos at, String who) {
        for (Resident r : sl.getEntitiesOfClass(Resident.class, new net.minecraft.world.phys.AABB(at).inflate(16), x -> x.profile() != null && x.isFree())) {
            if (r.getRandom().nextFloat() < 0.5f) {
                r.getLookControl().setLookAt(at.getX() + 0.5, at.getY() + 20, at.getZ() + 0.5);
                r.gesture(Resident.G_CHEER, 60);
                r.say(r.pick("Whoa, look at " + who + " go!", "WOOO! Go " + who + "!", "They're flying!!", "I'm next!"), 60);
            }
        }
    }

    static void phase(ServerPlayer pl, Diver d, int phase) {
        d.phase = phase;
        d.since = pl.serverLevel().getGameTime();
        String line = "#skyp|" + pl.getId() + "|" + phase;
        for (ServerPlayer o : pl.serverLevel().players()) if (o != pl && o.distanceToSqr(pl) < 256 * 256) PcNet.send(o, new PcNet.Msg(line));
        if (phase >= FREEFALL && !d.hadFly && !pl.isCreative() && !pl.isSpectator()) {
            pl.getAbilities().mayfly = false;
            pl.getAbilities().flying = false;
            pl.onUpdateAbilities();
        }
    }

    /** The client reports its phase: freefall, chute, landed, done. */
    public static void report(ServerPlayer pl, String what) {
        Diver d = DIVERS.get(pl.getUUID());
        if (d == null) return;
        pl.fallDistance = 0;
        switch (what) {
            case "freefall" -> phase(pl, d, FREEFALL);
            case "chute" -> {
                phase(pl, d, CHUTE);
                pl.serverLevel().playSound(null, pl.blockPosition(), SoundEvents.ARMOR_EQUIP_ELYTRA, SoundSource.PLAYERS, 1.5f, 0.7f);
            }
            case "landed" -> {
                phase(pl, d, LANDED);
                SAFE.put(pl.getUUID(), pl.serverLevel().getGameTime() + 100);
                CityData cd = CityData.get(pl.serverLevel());
                long day = Calendar.worldDay(pl.serverLevel());
                String pn = pl.getName().getString();
                {
                    for (Resident r : pl.serverLevel().getEntitiesOfClass(Resident.class, pl.getBoundingBox().inflate(12), x -> x.profile() != null && x.isFree())) {
                        r.getLookControl().setLookAt(pl, 30, 30);
                        r.say(r.pick("Nice landing, " + pn + "!", "That was AMAZING!", "You fell from the sky! Literally!", "10 out of 10 landing!"), 60);
                        CityData.Profile p = r.profile();
                        if (p != null) Mind.playerEvent(cd, p, pn, day, "I watched {P} skydive down from the Sky Launch", 2, 4);
                        break;
                    }
                }
            }
            case "done" -> end(pl);
            default -> {}
        }
    }

    public static void end(ServerPlayer pl) {
        Diver d = DIVERS.remove(pl.getUUID());
        if (d != null && d.phase > NONE) {
            SAFE.put(pl.getUUID(), pl.serverLevel().getGameTime() + 100);
            phase(pl, d, NONE);
            if (!d.hadFly && !pl.isCreative() && !pl.isSpectator() && pl.getAbilities().mayfly) {
                pl.getAbilities().mayfly = false;
                pl.getAbilities().flying = false;
                pl.onUpdateAbilities();
            }
        }
    }

    public static void tick(ServerLevel sl) {
        long now = sl.getGameTime();
        Iterator<Map.Entry<UUID, Diver>> it = DIVERS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Diver> e = it.next();
            ServerPlayer pl = sl.getServer().getPlayerList().getPlayer(e.getKey());
            Diver d = e.getValue();
            if (pl == null) { it.remove(); continue; }
            if (pl.serverLevel() != sl) continue;
            if (d.countdown >= 0) {
                if (d.pad != null && pl.blockPosition().distSqr(d.pad) > 2.5) {
                    pl.displayClientMessage(Component.literal("§7Launch cancelled - stay on the pad."), true);
                    it.remove();
                    continue;
                }
                d.countdown--;
                if (d.countdown % 20 == 0 && d.countdown > 0) {
                    pl.displayClientMessage(Component.literal("§6§lSKY LAUNCH §fstand still... §e" + d.countdown / 20), true);
                    sl.playSound(null, d.pad, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 1f, d.countdown == 20 ? 1.4f : 1.0f);
                }
                if (d.pad != null) sl.sendParticles(ParticleTypes.FLAME, d.pad.getX() + 0.5, d.pad.getY() + 0.1, d.pad.getZ() + 0.5, 3, 0.6, 0, 0.6, 0.01);
                if (d.countdown <= 0) {
                    pl.displayClientMessage(Component.literal("§c§lLAUNCH!"), true);
                    launch(pl, d.pad);
                }
                continue;
            }
            pl.fallDistance = 0;
            if (d.phase == LAUNCH && d.pad != null && pl.getY() < CityData.get(sl).skyTop + 2)
                sl.sendParticles(ParticleTypes.FIREWORK, pl.getX(), pl.getY() - 0.5, pl.getZ(), 4, 0.3, 0.3, 0.3, 0.05);
            if (now - d.since > 20 * 180) end(pl);
        }
        SAFE.values().removeIf(t -> t < now);
    }

    public static void onFall(LivingFallEvent e) {
        if (e.getEntity() instanceof Resident r && r.skyPhase() > 0) { e.setCanceled(true); return; }
        if (!(e.getEntity() instanceof ServerPlayer pl)) return;
        if (diving(pl) || SAFE.containsKey(pl.getUUID())) {
            e.setDistance(0);
            e.setCanceled(true);
        }
    }

    public static void reset() {
        DIVERS.clear();
        SAFE.clear();
    }
}
