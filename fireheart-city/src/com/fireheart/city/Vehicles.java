package com.fireheart.city;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Vehicle keys, parking and the garage commands. */
public final class Vehicles {
    private Vehicles() {}

    public static class Key extends Item {
        final int kind;

        public Key(int kind, Properties p) {
            super(p);
            this.kind = kind;
        }

        @Override
        public InteractionResult useOn(UseOnContext c) {
            Level lv = c.getLevel();
            if (lv.isClientSide) return InteractionResult.SUCCESS;
            BlockPos at = c.getClickedPos().relative(c.getClickedFace());
            if (kind == Vehicle.BOAT) {
                BlockPos w = c.getClickedPos();
                if (!lv.getFluidState(w).is(FluidTags.WATER) && !lv.getFluidState(at).is(FluidTags.WATER) && !lv.getFluidState(at.below()).is(FluidTags.WATER)) {
                    if (c.getPlayer() != null) c.getPlayer().displayClientMessage(Component.literal("§7Launch the speedboat on water."), true);
                    return InteractionResult.FAIL;
                }
                if (lv.getFluidState(w).is(FluidTags.WATER)) at = w;
            }
            float yaw = c.getPlayer() == null ? 0 : c.getPlayer().getYRot();
            int paint = c.getItemInHand().hasTag() ? c.getItemInHand().getTag().getInt("Paint") : 0;
            Vehicle v = spawn((ServerLevel) lv, kind, paint, c.getPlayer() == null ? "" : c.getPlayer().getName().getString(), at.getX() + 0.5, at.getY() + (kind == Vehicle.BOAT ? 0.1 : 0), at.getZ() + 0.5, yaw);
            if (v == null) return InteractionResult.FAIL;
            if (c.getPlayer() == null || !c.getPlayer().getAbilities().instabuild) c.getItemInHand().shrink(1);
            return InteractionResult.CONSUME;
        }

        @Override
        public Component getName(ItemStack s) {
            int paint = s.hasTag() ? Math.floorMod(s.getTag().getInt("Paint"), Vehicle.PAINTS.length) : 0;
            return Component.literal(Vehicle.NAMES[kind] + " Key §7(" + Vehicle.PAINTS[paint] + ")");
        }

        @Override
        public void appendHoverText(ItemStack s, Level l, List<Component> tip, TooltipFlag f) {
            tip.add(Component.literal(kind == Vehicle.BOAT ? "§7Right-click water to launch" : "§7Right-click the ground to park it here"));
            tip.add(Component.literal("§7W/S drive · A/D steer · Space handbrake · H horn · L lights"));
            tip.add(Component.literal("§7Hit it 3 times to pack it back into the key"));
        }
    }

    public static ItemStack keyFor(int kind, int paint) {
        Item it = switch (kind) {
            case Vehicle.BIKE -> FireheartCity.BIKE_KEY.get();
            case Vehicle.BOAT -> FireheartCity.BOAT_KEY.get();
            default -> FireheartCity.CAR_KEY.get();
        };
        ItemStack s = new ItemStack(it);
        s.getOrCreateTag().putInt("Paint", paint);
        return s;
    }

    public static Vehicle spawn(ServerLevel sl, int kind, int paint, String owner, double x, double y, double z, float yaw) {
        Vehicle v = FireheartCity.VEHICLE.get().create(sl);
        if (v == null) return null;
        v.setup(kind, paint, owner);
        v.moveTo(x, y, z, yaw, 0);
        if (!sl.noCollision(v, v.getBoundingBox().deflate(0.05))) v.moveTo(x, y + 1, z, yaw, 0);
        sl.addFreshEntity(v);
        sl.playSound(null, v.blockPosition(), Vehicle.sound("vehicle.start"), SoundSource.NEUTRAL, 0.8f, 1.2f);
        return v;
    }

    public static void act(ServerPlayer pl, String what) {
        if (!(pl.getVehicle() instanceof Vehicle v) || v.getControllingPassenger() != pl) return;
        if (what.equals("horn")) pl.serverLevel().playSound(null, v.blockPosition(), Vehicle.sound(v.kind() == Vehicle.BIKE ? "vehicle.horn_bike" : "vehicle.horn_car"), SoundSource.NEUTRAL, 2f, 1f);
        else if (what.equals("lights")) v.toggleLights();
    }

    public static String give(ServerPlayer pl, int paint) {
        for (int k = 0; k < 3; k++) {
            ItemStack s = keyFor(k, paint);
            if (!pl.getInventory().add(s)) pl.drop(s, false);
        }
        return "§6Here are your keys: " + String.join(", ", Vehicle.NAMES) + " §7(" + Vehicle.PAINTS[Math.floorMod(paint, Vehicle.PAINTS.length)] + ")";
    }

    static final double[][] SPOTS = {{2, 71, -20, 90}, {5, 71, -20, 90}, {12, 63, 74, 180}};

    public static String park(ServerPlayer pl) {
        ServerLevel sl = pl.serverLevel();
        String pn = pl.getName().getString();
        int moved = 0;
        for (Vehicle v : sl.getEntitiesOfClass(Vehicle.class, new AABB(-600, -64, -600, 600, 320, 600), v -> v.owner().equals(pn) && v.getPassengers().isEmpty())) {
            double[] s = SPOTS[v.kind()];
            BlockPos b = BlockPos.containing(s[0], s[1], s[2]);
            if (v.kind() == Vehicle.BOAT) b = water(sl, b);
            v.moveTo(b.getX() + 0.5, b.getY() + (v.kind() == Vehicle.BOAT ? 0.1 : 0), b.getZ() + 0.5, (float) s[3], 0);
            v.speed = 0;
            moved++;
        }
        return moved == 0 ? "§7None of your vehicles are loaded right now." : "§6Parked " + moved + " vehicle" + (moved == 1 ? "" : "s") + " at the garage and marina.";
    }

    static BlockPos water(ServerLevel sl, BlockPos near) {
        for (int r = 0; r < 12; r++) for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) for (int dy = 4; dy >= -4; dy--) {
            BlockPos q = near.offset(dx, dy, dz);
            if (sl.getFluidState(q).is(FluidTags.WATER) && sl.getBlockState(q.above()).isAir()) return q;
        }
        return near;
    }

    public static String replace(ServerPlayer pl) {
        ServerLevel sl = pl.serverLevel();
        var src = pl.createCommandSourceStack().withPermission(4).withSuppressedOutput();
        int removed = 0;
        for (String ship : new String[]{"fireheart-cr01", "fireheart-mb01", "fireheart-sea01"}) {
            try {
                removed += sl.getServer().getCommands().getDispatcher().execute("vs delete " + ship, src) > 0 ? 1 : 0;
            } catch (Exception ignored) {}
        }
        String pn = pl.getName().getString();
        int paint = 0;
        for (int k = 0; k < 3; k++) {
            double[] s = SPOTS[k];
            BlockPos b = BlockPos.containing(s[0], s[1], s[2]);
            if (k == Vehicle.BOAT) b = water(sl, b);
            boolean have = !sl.getEntitiesOfClass(Vehicle.class, new AABB(b).inflate(6), v -> v.owner().equals(pn)).isEmpty();
            if (!have) spawn(sl, k, paint, pn, b.getX() + 0.5, b.getY() + (k == Vehicle.BOAT ? 0.1 : 0), b.getZ() + 0.5, (float) s[3]);
        }
        return "§6Out with the old: removed " + removed + " old ship vehicle" + (removed == 1 ? "" : "s") + ". §fYour new Solaris Coupe and Street Bike are at the garage (2 71 -20), the Speedboat at the marina.";
    }
}
