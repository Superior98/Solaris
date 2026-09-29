package com.fireheart.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** SolNav GPS: pick a destination (or a player) and a HUD compass, road-line and distance guide you there, on foot or in a vehicle. */
public final class Gps {
    private Gps() {}

    record Target(BlockPos pos, String label, UUID follow) {}

    static final Map<UUID, Target> ACTIVE = new HashMap<>();

    static final Map<String, String[]> GROUPS = new LinkedHashMap<>();

    static {
        GROUPS.put("Homes", new String[]{"stellar_house", "magma_house", "hotel", "reception"});
        GROUPS.put("Food & Fun", new String[]{"diner", "market", "bakery", "beach_bar", "beach", "cinema", "pier", "boardwalk", "plaza", "park"});
        GROUPS.put("Services", new String[]{"bank", "post", "tech", "library", "police", "fire", "lab", "garage", "fuel", "marina", "clock"});
        GROUPS.put("Travel", new String[]{"skyport", "ferry_city", "port", "factory", "gravel", "supply", "statue"});
        GROUPS.put("Neon Heights", new String[]{"isle_plaza", "noodle", "arcade", "gardens", "observatory", "memorial"});
    }

    public static List<String> keys() {
        List<String> out = new ArrayList<>();
        for (String[] g : GROUPS.values()) for (String k : g) if (Place.get(k) != null) out.add(k);
        out.add("secret_base");
        return out;
    }

    static String name(String key) {
        Place p = Place.get(key);
        if (p == null) return key.replace('_', ' ');
        String l = p.label;
        return l.startsWith("the ") ? Character.toUpperCase(l.charAt(4)) + l.substring(5) : Character.toUpperCase(l.charAt(0)) + l.substring(1);
    }

    public static void menu(ServerPlayer pl) {
        pl.sendSystemMessage(Component.literal("§b§l✦ SolNav GPS §7- click where you want to go"));
        for (Map.Entry<String, String[]> g : GROUPS.entrySet()) {
            MutableComponent line = Component.literal("§6" + g.getKey() + ": ");
            boolean first = true;
            for (String k : g.getValue()) {
                if (Place.get(k) == null) continue;
                if (!first) line.append(Component.literal("§8 · "));
                first = false;
                line.append(link("§f" + name(k), "/sol gps " + k, "Navigate to " + name(k)));
            }
            pl.sendSystemMessage(line);
        }
        MutableComponent ppl = Component.literal("§6Players: ");
        boolean any = false;
        for (ServerPlayer o : pl.server.getPlayerList().getPlayers()) {
            if (o == pl) continue;
            if (any) ppl.append(Component.literal("§8 · "));
            any = true;
            ppl.append(link("§d" + o.getName().getString(), "/sol gps player " + o.getName().getString(), "Find " + o.getName().getString() + " (updates live)"));
        }
        if (!any) ppl.append(Component.literal("§8nobody else online"));
        pl.sendSystemMessage(ppl);
        MutableComponent tail = Component.literal("§6Other: ");
        if (StellarHome.owner(pl)) tail.append(link("§bSecret base", "/sol gps secret_base", "Stellar Command (owners only)")).append(Component.literal("§8 · "));
        tail.append(link("§7Where am I?", "/sol gps here", "Your position and nearest landmark")).append(Component.literal("§8 · "));
        tail.append(link("§cStop navigation", "/sol gps off", "Turn off the GPS"));
        pl.sendSystemMessage(tail);
    }

    static MutableComponent link(String text, String cmd, String hover) {
        return Component.literal(text).setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover).withStyle(ChatFormatting.GRAY))));
    }

    public static String to(ServerPlayer pl, String query) {
        String q = query.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if (q.equals("secret_base") || q.equals("secret") || q.equals("base")) {
            if (!StellarHome.owner(pl)) return "§cSolNav: that destination is classified.";
            set(pl, StellarHome.HATCH, "Secret base hatch (sneak on it)");
            return "§bSolNav: heading to the secret lift hatch in your study. §7Sneak on the hatch to drop down.";
        }
        Place p = Place.get(q);
        if (p == null) {
            for (String k : keys()) {
                Place c = Place.get(k);
                if (c != null && (c.label.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)) || k.contains(q))) { p = c; break; }
            }
        }
        if (p == null) {
            ServerPlayer o = pl.server.getPlayerList().getPlayerByName(query.trim());
            if (o != null) return toPlayer(pl, o);
            return "§cSolNav: I couldn't find \"" + query + "\". §7Try /sol gps for the list.";
        }
        set(pl, p.entrance != null ? p.entrance : p.pos, name(p.key));
        return "§bSolNav: §fnavigating to §e" + name(p.key) + "§f. §7Follow the arrow at the top of your screen.";
    }

    public static String toPlayer(ServerPlayer pl, ServerPlayer o) {
        if (o == pl) return "§7SolNav: you're already here. Hi!";
        ACTIVE.put(pl.getUUID(), new Target(o.blockPosition(), o.getName().getString(), o.getUUID()));
        send(pl, ACTIVE.get(pl.getUUID()));
        return "§bSolNav: §ftracking §d" + o.getName().getString() + "§f live. §7The arrow follows them as they move.";
    }

    public static void set(ServerPlayer pl, BlockPos pos, String label) {
        Target t = new Target(pos, label, null);
        ACTIVE.put(pl.getUUID(), t);
        send(pl, t);
    }

    public static void clear(ServerPlayer pl, boolean say) {
        ACTIVE.remove(pl.getUUID());
        PcNet.send(pl, new PcNet.Msg("#gps|off"));
        if (say) pl.displayClientMessage(Component.literal("§7SolNav: navigation off."), true);
    }

    static void send(ServerPlayer pl, Target t) {
        PcNet.send(pl, new PcNet.Msg("#gps|" + t.pos().getX() + "|" + t.pos().getY() + "|" + t.pos().getZ() + "|" + (t.follow() != null ? "p" : "d") + "|" + t.label().replace('|', '/')));
    }

    public static String here(ServerPlayer pl) {
        BlockPos b = pl.blockPosition();
        Place best = null;
        double bd = Double.MAX_VALUE;
        for (String k : keys()) {
            Place p = Place.get(k);
            if (p == null) continue;
            double d = p.pos.distSqr(b);
            if (d < bd) { bd = d; best = p; }
        }
        String near = best == null ? "" : " §7- nearest landmark: §f" + name(best.key) + " §7(" + (int) Math.sqrt(bd) + "m)";
        return "§bSolNav: §fyou're at §e" + b.getX() + " " + b.getY() + " " + b.getZ() + near;
    }

    public static void tick(ServerLevel sl) {
        if (sl.dimension() != Level.OVERWORLD || sl.getGameTime() % 20 != 5 || ACTIVE.isEmpty()) return;
        ACTIVE.entrySet().removeIf(e -> {
            ServerPlayer pl = sl.getServer().getPlayerList().getPlayer(e.getKey());
            if (pl == null) return true;
            Target t = e.getValue();
            if (t.follow() != null) {
                ServerPlayer o = sl.getServer().getPlayerList().getPlayer(t.follow());
                if (o == null) {
                    pl.displayClientMessage(Component.literal("§7SolNav: " + t.label() + " went offline."), true);
                    PcNet.send(pl, new PcNet.Msg("#gps|off"));
                    return true;
                }
                Target nt = new Target(o.blockPosition(), t.label(), t.follow());
                e.setValue(nt);
                send(pl, nt);
                return false;
            }
            double dx = pl.getX() - (t.pos().getX() + 0.5), dz = pl.getZ() - (t.pos().getZ() + 0.5);
            if (dx * dx + dz * dz < 5 * 5 && Math.abs(pl.getY() - t.pos().getY()) < 6) {
                pl.displayClientMessage(Component.literal("§a✔ SolNav: you have arrived at §f" + t.label()), true);
                pl.playNotifySound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.5f);
                PcNet.send(pl, new PcNet.Msg("#gps|off"));
                return true;
            }
            return false;
        });
    }

    public static void onLogin(ServerPlayer pl) {
        CityData d = CityData.get(pl.serverLevel());
        String pn = pl.getName().getString();
        if (!d.setting(pn, "gpsGiven", "").isEmpty()) return;
        d.setSetting(pn, "gpsGiven", "1");
        d.setDirty();
        ItemStack st = new ItemStack(FireheartCity.GPS.get());
        if (!pl.getInventory().add(st)) pl.drop(st, false);
        pl.sendSystemMessage(Component.literal("§b✦ You got a §lSolNav GPS§b! §7Right-click it (or type §f/sol gps§7) to pick a destination. It works in cars too."));
    }

    public static final class GpsItem extends Item {
        public GpsItem(Properties p) {
            super(p);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                if (sp.isShiftKeyDown() && ACTIVE.containsKey(sp.getUUID())) clear(sp, true);
                else menu(sp);
            }
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack st, Level level, List<Component> tip, net.minecraft.world.item.TooltipFlag flag) {
            tip.add(Component.literal("§7Right-click: pick a destination"));
            tip.add(Component.literal("§7Sneak + right-click: stop navigation"));
        }
    }
}
