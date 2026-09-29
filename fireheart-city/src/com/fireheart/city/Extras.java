package com.fireheart.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

/**
 * v1.5 phone and city extras: ferry pickup, directions, bank transfers, FireEats food delivery, phone settings
 * (do not disturb, ringtones, cases), battery and charging, group chats and group calls, follows, photo posts,
 * broken phones and repairs, the FirePhone 2 launch, Ava's promotion, resident routines around devices.
 */
public final class Extras {
    private Extras() {}

    public static final BlockPos TABLET = new BlockPos(0, -1000, 0), CONSOLE = new BlockPos(0, -1001, 0);
    public static final String[] RINGTONES = {"Bell", "Chime", "Pling", "Harp", "Flute"};
    public static final String[] CASES = {"No case", "Clear", "Leather", "Sparkle", "Neon"};
    public static final String[] SCENES = {"gardens", "observatory", "pier", "marina", "plaza", "statue", "memorial", "organ", "clock", "park", "ferry_isle", "isle_plaza"};

    /* ================================================================ settings */

    public static boolean dnd(CityData d, String player) {
        return "1".equals(d.setting(player, "dnd", "0"));
    }

    public static int ringtone(CityData d, String player) {
        try { return Math.floorMod(Integer.parseInt(d.setting(player, "ring", "0")), RINGTONES.length); } catch (NumberFormatException e) { return 0; }
    }

    public static void ringSound(ServerPlayer pl, CityData d, long t) {
        int r = ringtone(d, pl.getName().getString());
        var snd = switch (r) {
            case 1 -> SoundEvents.NOTE_BLOCK_CHIME.value();
            case 2 -> SoundEvents.NOTE_BLOCK_PLING.value();
            case 3 -> SoundEvents.NOTE_BLOCK_HARP.value();
            case 4 -> SoundEvents.NOTE_BLOCK_FLUTE.value();
            default -> SoundEvents.NOTE_BLOCK_BELL.value();
        };
        pl.playNotifySound(snd, SoundSource.PLAYERS, 0.6f, t % 40 == 0 ? 1.4f : 1.2f);
    }

    public static List<String> follows(CityData d, String player) {
        String s = d.setting(player, "follows", "");
        List<String> l = new ArrayList<>();
        for (String x : s.split(",")) if (!x.isEmpty()) l.add(x);
        return l;
    }

    /* ================================================================ phone stack: battery, case, model */

    public static ItemStack phoneStack(ServerPlayer pl) {
        ItemStack h = pl.getMainHandItem();
        if (h.getItem() instanceof PhoneItem) return h;
        Inventory inv = pl.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).getItem() instanceof PhoneItem) return inv.getItem(i);
        return ItemStack.EMPTY;
    }

    public static int battery(ItemStack st) {
        return st.isEmpty() ? 0 : st.hasTag() && st.getTag().contains("Battery") ? st.getTag().getInt("Battery") : 100;
    }

    public static boolean flat(ServerPlayer pl) {
        ItemStack st = phoneStack(pl);
        return !st.isEmpty() && battery(st) <= 0;
    }

    static final Map<UUID, Integer> DRAIN = new HashMap<>();
    static final Map<UUID, Boolean> CHARGING = new HashMap<>();

    public static boolean charging(ServerPlayer pl) {
        return CHARGING.getOrDefault(pl.getUUID(), false);
    }

    static boolean nearCharger(ServerLevel sl, CityData d, ServerPlayer pl) {
        if (pl.isSleeping()) return true;
        BlockPos at = pl.blockPosition();
        if (at.closerThan(TechStore.CENTER, 9)) return true;
        for (Long k : d.pcs.keySet()) if (BlockPos.of(k).closerThan(at, 5)) return true;
        for (Long k : d.tvs.keySet()) if (BlockPos.of(k).closerThan(at, 5)) return true;
        return false;
    }

    static void batteryTick(ServerLevel sl, CityData d) {
        for (ServerPlayer pl : sl.players()) batteryTick(sl, d, pl);
    }

    static void batteryTick(ServerLevel sl, CityData d, ServerPlayer pl) {
        {
            ItemStack st = phoneStack(pl);
            if (st.isEmpty()) return;
            int b = battery(st);
            boolean ch = nearCharger(sl, d, pl);
            boolean was = CHARGING.getOrDefault(pl.getUUID(), false);
            CHARGING.put(pl.getUUID(), ch);
            int acc = DRAIN.getOrDefault(pl.getUUID(), 0);
            int nb = b;
            if (ch) nb = Math.min(100, b + 3);
            else {
                boolean open = Phones.OPEN_PHONE.contains(pl.getUUID());
                boolean call = Phones.PLAYER_CALLS.containsKey(pl.getUUID());
                acc += call ? 12 : open ? 8 : 1;
                if (acc >= 240) { acc = 0; nb = Math.max(0, b - 1); }
            }
            DRAIN.put(pl.getUUID(), acc);
            if (nb != b) st.getOrCreateTag().putInt("Battery", nb);
            if (ch && !was && b < 100) pl.displayClientMessage(Component.literal("§a⚡ SolPhone charging (" + nb + "%)"), true);
            if (nb == 20 && b == 21) Phones.toast(pl, "Battery", "20% battery left. Charge next to a PC, a TV, at SolTech or while you sleep.");
            if (nb <= 0 && b > 0) {
                Phones.hangup(sl, d, Phones.PLAYER_CALLS.get(pl.getUUID()), "Your battery died.", false);
                Phones.OPEN_PHONE.remove(pl.getUUID());
                Phones.send(pl, "#battery|0");
                pl.displayClientMessage(Component.literal("§cYour SolPhone battery is flat!"), true);
            } else if ((nb != b || ch != was) && Phones.OPEN_PHONE.contains(pl.getUUID())) Phones.send(pl, "#battery|" + nb + "|" + (ch ? 1 : 0));
        }
    }

    public static void watchGlance(ServerPlayer pl) {
        CityData d = CityData.get(pl.serverLevel());
        List<String> box = d.inbox.getOrDefault(Bank.playerKey(pl.getName().getString()), List.of());
        String last = null;
        for (int i = box.size() - 1; i >= 0 && last == null; i--) {
            String[] p = box.get(i).split("\\|", 4);
            if (p.length == 4 && p[1].equals("<")) last = displayName(d, pl.getName().getString(), p[0]) + ": " + p[3];
        }
        String clock = Calendar.clock(pl.serverLevel().getDayTime());
        pl.displayClientMessage(Component.literal("§b⌚ " + clock + " §7| §f" + (last == null ? "No messages yet" : last.length() > 70 ? last.substring(0, 67) + "..." : last)), true);
        pl.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.4f, 1.8f);
    }

    /* ================================================================ directions */

    record Guide(UUID player, String resident, long until) {}

    static final List<Guide> GUIDES = new ArrayList<>();

    public static void directions(ServerPlayer pl, CityData d, String id) {
        CityData.Profile p = d.profiles.get(id);
        if (p == null) return;
        GUIDES.removeIf(g -> g.player().equals(pl.getUUID()));
        GUIDES.add(new Guide(pl.getUUID(), id, pl.serverLevel().getGameTime() + 2400));
        Phones.toast(pl, "Maps", "Directions to " + p.name + " - follow the sparkles!");
    }

    static void guideTick(ServerLevel sl, CityData d) {
        Iterator<Guide> it = GUIDES.iterator();
        while (it.hasNext()) {
            Guide g = it.next();
            ServerPlayer pl = sl.getServer().getPlayerList().getPlayer(g.player());
            CityData.Profile p = d.profiles.get(g.resident());
            Resident r = p == null ? null : Phones.entity(sl, p);
            if (pl == null || p == null || sl.getGameTime() > g.until()) { it.remove(); continue; }
            BlockPos target = r != null ? r.blockPosition() : p.homePlace() == null ? null : p.homePlace().pos;
            if (target == null) { it.remove(); continue; }
            boolean isle = r != null ? r.onIsland() : p.livesOnIsland();
            if (isle != (pl.getY() > 150)) target = isle ? Ferry.CITY_PAD : Ferry.ISLE_PAD;
            double dx = target.getX() + 0.5 - pl.getX(), dz = target.getZ() + 0.5 - pl.getZ(), dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 4 && isle == (pl.getY() > 150)) {
                pl.displayClientMessage(Component.literal("§a✔ You found " + p.name + "!"), true);
                it.remove();
                continue;
            }
            String dir = compass(dx, dz);
            String via = isle != (pl.getY() > 150) ? " §7(take the Sky Ferry)" : "";
            pl.displayClientMessage(Component.literal("§b➤ " + p.name + " §f" + (int) dist + " m " + dir + via), true);
            DustParticleOptions dust = new DustParticleOptions(new Vector3f(0.3f, 0.8f, 1f), 1.2f);
            for (int i = 1; i <= 8; i++) {
                double k = Math.min(dist, i * 1.2) / dist;
                sl.sendParticles(pl, dust, true, pl.getX() + dx * k, pl.getY() + 0.3, pl.getZ() + dz * k, 1, 0.05, 0.05, 0.05, 0);
            }
        }
    }

    static String compass(double dx, double dz) {
        double a = Math.toDegrees(Math.atan2(dx, -dz));
        String[] n = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        return n[Math.floorMod((int) Math.round(a / 45.0), 8)];
    }

    /* ================================================================ FireEats */

    public static final String[][] SHOPS = {{"diner", "Diesel Diner", "COOK"}, {"noodle", "Neon Noodles", "NOODLE_CHEF"}, {"bakery", "Solaris Bakery", "BAKER"}};
    public static final int DELIVERY = 3;

    static final String[] SHOP_DESC = {"Burgers, fries & shakes since day one", "Hot neon ramen, straight from the sky island", "Fresh bread, cakes and pastries"};
    static final String[] SHOP_RATING = {"4.8", "4.9", "4.7"};

    public static List<String> menu(ServerLevel sl, CityData d) {
        List<String> l = new ArrayList<>();
        for (int k = 0; k < SHOPS.length; k++) {
            String[] s = SHOPS[k];
            boolean open = false;
            for (CityData.Profile p : d.profiles.values()) {
                if (p.job != Job.valueOf(s[2])) continue;
                Resident r = Phones.entity(sl, p);
                if (r == null || r.activityName().equals("work") || r.activityName().equals("lunch") || r.activityName().equals("leisure")) open = true;
            }
            LinkedHashMap<String, Integer> seen = new LinkedHashMap<>();
            for (String id : Economy.products(Job.valueOf(s[2]))) seen.putIfAbsent(id, Economy.price(id));
            for (Map.Entry<String, Integer> e : seen.entrySet()) l.add(s[0] + "|" + s[1] + "|" + e.getKey() + "|" + Economy.label(e.getKey()) + "|" + e.getValue() + "|" + (open ? 1 : 0) + "|" + SHOP_RATING[k] + "|" + SHOP_DESC[k]);
        }
        return l;
    }

    static void order(ServerPlayer pl, CityData d, String shop, String cart) {
        ServerLevel sl = pl.serverLevel();
        String pn = pl.getName().getString(), k = Bank.playerKey(pn);
        String[] s = null;
        for (String[] x : SHOPS) if (x[0].equals(shop)) s = x;
        if (s == null) return;
        String[] parts = cart.split(";");
        String items = parts[0];
        int tip = parts.length > 1 ? Math.max(0, Math.min(20, Phones.parse(parts[1]))) : 0;
        List<String[]> lines = new ArrayList<>();
        int price = DELIVERY + tip;
        for (String e : items.split(",")) {
            String[] q = e.split("\\*");
            String item = q[0];
            int n = q.length > 1 ? Math.max(1, Math.min(16, Phones.parse(q[1]))) : 1;
            if (!Economy.products(Job.valueOf(s[2])).contains(item)) continue;
            lines.add(new String[]{item, String.valueOf(n)});
            price += Economy.price(item) * n;
        }
        if (lines.isEmpty()) return;
        if (Bank.holder(d, pn) == null) { Phones.toast(pl, "SolEats", "You need a Solaris Bank account to order."); return; }
        long rd = Calendar.day(sl);
        int tod = Phones.tod(sl);
        if (!d.pay(Bank.sav(k), Economy.business(Job.valueOf(s[2])), price, "SolEats: " + s[1], rd, tod, false)) { Phones.toast(pl, "SolEats", "Not enough savings (" + price + " coins)."); return; }
        StringBuilder desc = new StringBuilder();
        for (String[] ln : lines) desc.append(desc.length() == 0 ? "" : ", ").append(ln[1]).append("x ").append(Economy.label(ln[0]).replaceFirst("^(a|an|some) ", ""));
        Post.Letter l = Post.send(d, shop, k, "Hi " + pn + "!\n\nYour SolEats order from " + s[1] + " is here: " + desc + ".\n\n" + (tip > 0 ? "Thanks for the " + tip + " coin tip! " : "") + "Enjoy!", "parcel", Calendar.worldDay(sl));
        long cookedAt = sl.getGameTime() + Kitchen.cook(sl, d, s[0], lines.get(0)[0], pn);
        l.gift = lines.get(0)[0];
        l.giftCount = Integer.parseInt(lines.get(0)[1]);
        l.giftNbt = Dishes.nbt(l.gift, cookedAt);
        StringBuilder ex = new StringBuilder();
        for (int i = 1; i < lines.size(); i++) ex.append(ex.length() == 0 ? "" : ",").append(Dishes.BY_SOURCE.containsKey(lines.get(i)[0]) ? "dish:" + lines.get(i)[0] + "@" + cookedAt : lines.get(i)[0]).append("*").append(lines.get(i)[1]);
        l.extras = ex.toString();
        if (tip > 0) for (CityData.Profile p : d.profiles.values()) if (p.job == Job.POSTMAN) p.coins += tip;
        Phones.toast(pl, "SolEats", "Order placed at " + s[1] + " (" + price + "¢). Track it in SolEats!");
        for (CityData.Profile p : d.profiles.values()) {
            if (p.job != Job.valueOf(s[2])) continue;
            Resident r = Phones.entity(sl, p);
            if (r != null && r.activityName().equals("work")) {
                r.say(r.pick("Order in for " + pn + "! Coming right up.", "SolEats order! On it.", "Ooh, " + pn + " ordered again!"), 60);
                r.gesture(Resident.G_THUMBS, 30);
            }
        }
        d.setDirty();
    }

    public static List<String> orders(CityData d, String pn) {
        List<String> out = new ArrayList<>();
        String k = Bank.playerKey(pn);
        for (Post.Letter l : d.civic.mail) {
            if (!l.kind.equals("parcel") || !l.to.equals(k)) continue;
            String shop = l.from;
            for (String[] s : SHOPS) if (s[0].equals(l.from)) shop = s[1];
            out.add(l.id + "|" + shop + "|" + Economy.label(l.gift) + (l.extras.isEmpty() ? "" : " + more") + "|" + l.stage + "|" + l.day);
        }
        while (out.size() > 6) out.remove(0);
        return out;
    }

    /* ================================================================ group chats */

    public record Group(String id, String name, List<String> members) {}

    public static List<Group> groups(CityData d, String pn) {
        List<Group> out = new ArrayList<>();
        List<String> isle = new ArrayList<>(), friends = new ArrayList<>(), fans = new ArrayList<>();
        for (CityData.Profile p : d.profiles.values()) {
            if (!p.ownsPhone) continue;
            if (p.livesOnIsland()) isle.add(p.id);
            if (p.mind.trustIn(pn) >= 25) friends.add(p.id);
            fans.add(p.id);
        }
        if (isle.size() >= 2) out.add(new Group("group:neon", "Neon Heights Residents", isle));
        if (friends.size() >= 2) out.add(new Group("group:friends", pn + "'s Crew", friends));
        if (fans.size() >= 3) out.add(new Group("group:fireheart", "Solaris Chat", fans));
        return out;
    }

    public static Group group(CityData d, String pn, String id) {
        for (Group g : groups(d, pn)) if (g.id().equals(id)) return g;
        return null;
    }

    public static String displayName(CityData d, String pn, String id) {
        if (id.startsWith("group:")) {
            Group g = group(d, pn, id);
            return g == null ? "Group chat" : g.name();
        }
        CityData.Profile p = d.profiles.get(id);
        return p == null ? id : p.name;
    }

    static void groupMessage(ServerPlayer pl, CityData d, String gid, String text) {
        ServerLevel sl = pl.serverLevel();
        String pn = pl.getName().getString();
        Group g = group(d, pn, gid);
        if (g == null) return;
        Computers.addInbox(d, Bank.playerKey(pn), gid, ">", Calendar.worldDay(sl), text);
        PcNet.send(pl, new PcNet.Msg(gid + "|>|" + Calendar.worldDay(sl) + "|" + text));
        List<String> m = new ArrayList<>(g.members());
        java.util.Collections.shuffle(m, new java.util.Random(sl.random.nextLong()));
        int n = Math.min(m.size(), 1 + sl.random.nextInt(3));
        String t = Phones.norm(text);
        for (int i = 0; i < n; i++) {
            CityData.Profile p = d.profiles.get(m.get(i));
            if (p == null || p.phoneBroken) continue;
            long due = sl.getGameTime() + 60 + i * 80 + sl.random.nextInt(100);
            Phones.LATER.add(new Object[]{due, (Runnable) () -> {
                Resident r = Phones.entity(sl, p);
                String rep = null;
                if (r != null) rep = Chat.reply(pl, r, t, true);
                if (text.contains("[pic:")) rep = Phones.pick(sl.random, "Ooh nice photo!", "Wow, where was this?", "Great shot, " + pn + "!");
                if (rep == null || rep.isEmpty()) rep = Phones.pick(sl.random, "Haha, yes!", "Same here!", "Love that ☺", "Count me in!", "Totally agree with " + pn + ".", "Lol");
                Computers.deliver(sl, d, pn, gid, p.name + ": " + rep);
                if (r != null && r.isFree() && !r.onPhone()) r.usePhone(1, 40, "texting in the group chat", null);
            }});
        }
    }

    private static long groupDay = -1;

    static void groupChatter(ServerLevel sl, CityData d, long day, int tod) {
        if (groupDay == day || tod < 4000 || tod > 11500 || sl.random.nextFloat() > 0.02f) return;
        groupDay = day;
        for (ServerPlayer pl : sl.players()) {
            String pn = pl.getName().getString();
            for (Group g : groups(d, pn)) {
                if (sl.random.nextFloat() < 0.4f) continue;
                CityData.Profile a = d.profiles.get(g.members().get(sl.random.nextInt(g.members().size())));
                if (a == null) continue;
                CityData.Event ev = Events.gossipFor(d, a, a, day);
                String line = ev != null && sl.random.nextBoolean() ? "Did everyone hear? " + Events.sentence(ev.text) + "!" : Phones.pick(sl.random, "Anyone up for noodles tonight?", "Who's going to the plaza later?", "The ferry is SO busy today lol", "Good morning everyone ☀", "Has anyone seen my umbrella?", "Stargazing Friday? ★");
                Computers.deliver(sl, d, pn, g.id(), a.name + ": " + line);
                CityData.Profile b = d.profiles.get(g.members().get(sl.random.nextInt(g.members().size())));
                if (b != null && b != a) {
                    String rep = Phones.pick(sl.random, "I'm in!", "Haha same", "Sounds good ☺", "Can't tonight, sorry!", "Omg yes");
                    Phones.LATER.add(new Object[]{sl.getGameTime() + 200, (Runnable) () -> Computers.deliver(sl, d, pn, g.id(), b.name + ": " + rep)});
                }
            }
        }
    }

    /* ================================================================ photos */

    public static String sceneAt(ServerPlayer pl) {
        Place best = null;
        double bd = 48 * 48;
        for (String k : Phones.SCENIC) {
            Place p = Place.get(k);
            if (p == null) continue;
            double dd = p.pos.distSqr(pl.blockPosition());
            if (dd < bd) { bd = dd; best = p; }
        }
        return best != null ? best.key : pl.getY() > 150 ? "isle_plaza" : "city";
    }

    public static List<String> gallery(CityData d, String pn) {
        List<String> l = new ArrayList<>();
        for (String x : d.setting(pn, "gallery", "").split(",")) if (!x.isEmpty()) l.add(x);
        return l;
    }

    static void snap(ServerPlayer pl, CityData d) {
        String pn = pl.getName().getString();
        List<String> g = gallery(d, pn);
        String key = sceneAt(pl);
        g.add(0, key + ":" + Calendar.worldDay(pl.serverLevel()));
        while (g.size() > 24) g.remove(g.size() - 1);
        d.setSetting(pn, "gallery", String.join(",", g));
        pl.serverLevel().playSound(null, pl.blockPosition(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 0.6f, 1.6f);
        for (Resident r : pl.serverLevel().getEntitiesOfClass(Resident.class, pl.getBoundingBox().inflate(6), x -> x.profile() != null && x.isFree())) {
            if (r.getRandom().nextFloat() < 0.4f) r.say(r.pick("Ooh, take one of me!", "Say cheese!", "Is that for SolFeed?", "*strikes a pose*"), 50);
            break;
        }
    }

    public static String sceneFor(Resident r) {
        Place p = Phones.scenicNear(r);
        return p == null ? null : p.key;
    }

    /* ================================================================ broken & lost phones, FirePhone 2, promotion */

    static void daily(ServerLevel sl, CityData d, long day) {
        RandomSource rnd = sl.random;
        for (CityData.Profile p : d.profiles.values()) {
            if (!p.ownsPhone || !p.wantDevice.isEmpty() || !FhcConfig.phonesBreak()) continue;
            float roll = rnd.nextFloat();
            if (!p.phoneBroken && roll < 0.025f) {
                p.phoneBroken = true;
                p.wantDevice = "repair";
                p.wantDay = day;
                p.mind.remember(p, day, 3000, "phone", "I dropped my SolPhone and cracked the screen", "", -2, 5);
                d.event(day, "shopping", p.name + " dropped their SolPhone and cracked the screen", null, p.id);
            } else if (roll > 0.992f) {
                p.ownsPhone = false;
                p.phoneBroken = false;
                p.wantDevice = "phone";
                p.wantDay = day;
                p.mind.remember(p, day, 3000, "phone", "I lost my SolPhone somewhere", "", -2, 5);
                d.event(day, "shopping", p.name + " lost their SolPhone", null, p.id);
            }
        }
        launch(sl, d, day);
        if (!d.avaPromoted && d.techSales >= 25) {
            d.avaPromoted = true;
            CityData.managerAva = true;
            CityData.Profile ava = d.profiles.get("ava");
            if (ava != null) {
                ava.mind.remember(ava, day, 6000, "work", "I got promoted to SolTech Store Manager!", TechStore.KEY, 3, 9);
                d.event(day, "work", "Ava was promoted to SolTech Store Manager after " + d.techSales + " sales", TechStore.CENTER, ava.id);
                Phones.queue(ava, "Big news: I'm the new SolTech Store Manager!! ★ #SolTech");
            }
        }
        for (CityData.Profile p : d.profiles.values()) {
            if (p.pcDeliverDay >= 0 && day >= p.pcDeliverDay) {
                p.pcDeliverDay = -1;
                Computers.buy(sl, d, p, day, Calendar.day(sl), Phones.tod(sl), false);
                p.mind.remember(p, day, 4000, "home", "Pip delivered my new Solaris PC", "", 2, 5);
                d.news(day, "Pip delivered a brand-new Solaris PC to " + p.name + "'s home.");
            }
        }
        d.setDirty();
    }

    public static boolean launched(CityData d, long day) {
        return d.launchDay != Long.MIN_VALUE && day >= d.launchDay;
    }

    static void launch(ServerLevel sl, CityData d, long day) {
        if (d.launchDay == Long.MIN_VALUE) {
            long sat = day + 3;
            while (Calendar.weekday(sat) != 5) sat++;
            d.launchDay = sat;
            d.news(day, "SolTech announced the SolPhone 2! Launch day is " + Calendar.name(sat) + " - doors open at 9am, queue early!");
            d.event(day, "shopping", "SolTech announced the SolPhone 2", TechStore.CENTER);
            return;
        }
        if (day == d.launchDay - 1) {
            for (CityData.Profile p : d.profiles.values()) {
                if (!p.ownsPhone || p.phoneModel >= 2 || !p.wantDevice.isEmpty()) continue;
                int money = p.coins + Bank.savings(d, p.id);
                boolean keen = p.trait == Trait.CURIOUS || p.trait == Trait.ADVENTUROUS || p.trait == Trait.TALKATIVE || p.trait == Trait.CHEERFUL;
                if (money >= TechStore.PHONE2 && (keen || sl.random.nextFloat() < 0.25f)) {
                    p.wantDevice = "phone2";
                    p.wantDay = day;
                    p.mind.remember(p, day, 9000, "want", "I'm going to queue for the SolPhone 2 tomorrow morning", TechStore.KEY, 2, 5);
                }
            }
        }
    }

    /** Launch-day morning: fans line up outside FireTech until the doors open at 9am. */
    public static BlockPos queueSpot(Resident r, CityData d) {
        CityData.Profile p = r.profile();
        if (p == null || !"phone2".equals(p.wantDevice) || Calendar.worldDay((ServerLevel) r.level()) != d.launchDay) return null;
        int tod = (int) r.timeOfDay();
        if (tod < 200 || tod > 3000 || r.onIsland()) return null;
        int idx = 0;
        for (CityData.Profile q : d.profiles.values()) {
            if (q == p) break;
            if ("phone2".equals(q.wantDevice)) idx++;
        }
        return new BlockPos(TechStore.ENTRANCE.getX() - 2 - idx, TechStore.ENTRANCE.getY(), TechStore.ENTRANCE.getZ() + (idx % 2));
    }

    static long openedDay = -1;

    static void launchTick(ServerLevel sl, CityData d, long day, int tod) {
        if (day != d.launchDay || openedDay == day || tod < 3000) return;
        openedDay = day;
        d.launchDone = true;
        int sold = 0;
        for (CityData.Profile p : d.profiles.values()) {
            if (!"phone2".equals(p.wantDevice)) continue;
            Resident r = Phones.entity(sl, p);
            if (r != null && r.blockPosition().closerThan(TechStore.ENTRANCE, 12) && TechStore.arrive(r, p)) sold++;
        }
        d.news(day, "The SolPhone 2 launched at SolTech" + (sold > 0 ? " - " + sold + " fans queued outside since dawn!" : "!"));
        d.event(day, "shopping", "the SolPhone 2 launched at SolTech", TechStore.CENTER);
        for (ServerPlayer pl : sl.players()) Phones.toast(pl, "SolTech", "The SolPhone 2 is out now! Trade in your old phone for " + (TechStore.PHONE2 - TechStore.TRADE_IN) + " coins.");
    }

    /* ================================================================ resident routines */

    static void routines(ServerLevel sl, CityData d, long day, int tod) {
        for (CityData.Profile p : d.profiles.values()) {
            if (Math.floorMod(p.id.hashCode() + sl.getGameTime() / 20, 40) != 3) continue;
            Resident r = Phones.entity(sl, p);
            if (r == null || !r.isFree() || r.onPhone() || r.usingPc() || r.isSleeping()) continue;
            String act = r.activityName();
            RandomSource rnd = r.getRandom();
            if (p.ownsPhone && !p.phoneBroken && tod > 23300 || p.ownsPhone && !p.phoneBroken && tod < 900) {
                if (rnd.nextFloat() < 0.3f && p.log(r.routineDay()).once("morningnews")) {
                    r.usePhone(1, 80, "reading the morning news", () -> {
                        for (int i = d.events.size() - 1; i >= 0 && i >= d.events.size() - 3; i--) p.learn(d.events.get(i).id);
                        if (rnd.nextFloat() < 0.4f) r.say(Phones.pick(rnd, "Huh, interesting news today.", "Morning headlines... wow.", "Coffee and the news ☕"), 50);
                    });
                    continue;
                }
            }
            if (act.equals("work") && tod > 5800 && tod < 6600 && p.ownsPhone && !p.phoneBroken && rnd.nextFloat() < 0.35f && p.log(r.routineDay()).once("lunchscroll")) {
                r.usePhone(1, 120, "scrolling SolFeed on their lunch break", () -> Phones.browse(r, p, false));
                continue;
            }
            if (p.headphones && (act.equals("leisure") || act.equals("commute") || act.equals("evening")) && rnd.nextFloat() < 0.25f) {
                r.setMusic(200 + rnd.nextInt(200));
                continue;
            }
            if (p.hasTv && act.equals("evening") && r.atHome() && rnd.nextFloat() < 0.3f) {
                r.watchTv(400 + rnd.nextInt(400));
            }
        }
    }

    /** Evening gaming nights: friends visiting a PC owner play games together. */
    static void gamingNights(ServerLevel sl, CityData d, long day) {
        for (CityData.Plan pl : new ArrayList<>(d.plans)) {
            if (pl.day != day || !pl.what.equals("gaming") || pl.who.size() < 2) continue;
            CityData.Profile host = d.profiles.get(pl.who.get(1));
            CityData.Profile guest = d.profiles.get(pl.who.get(0));
            if (host == null || guest == null || host.pcPos == null) continue;
            Resident h = Phones.entity(sl, host), g = Phones.entity(sl, guest);
            if (h == null || g == null || !h.blockPosition().closerThan(host.pcPos, 6) || !g.blockPosition().closerThan(host.pcPos, 6)) continue;
            if (!guest.log(g.routineDay()).once("gaming:" + host.id)) continue;
            String game = Computers.GAME_NAMES[sl.random.nextInt(Computers.GAME_NAMES.length)];
            h.say(Phones.pick(sl.random, "Okay, " + guest.name + ", you're going down at " + game + "!", "Best of three? " + game + "!"), 60);
            g.say(Phones.pick(sl.random, "Ha! Bring it on.", "I've been practising, just so you know."), 60);
            Computers.setScreen(sl, host.pcPos, 2);
            host.fun = Math.min(100, host.fun + 20);
            guest.fun = Math.min(100, guest.fun + 20);
            host.social = Math.min(100, host.social + 15);
            guest.social = Math.min(100, guest.social + 15);
            CityData.Rel a = d.rel(host.id, guest.id), b = d.rel(guest.id, host.id);
            a.aff = Math.min(100, a.aff + 4);
            b.aff = Math.min(100, b.aff + 4);
            host.log(h.routineDay()).note("I had a " + game + " night with " + guest.name + " at my place");
            guest.log(g.routineDay()).note("I played " + game + " with " + host.name + " at their place");
            d.news(day, host.name + " and " + guest.name + " had a " + game + " gaming night.");
        }
    }

    /* ================================================================ in-person reactions to posts */

    public static String postLine(CityData d, CityData.Profile p, String pn, long day) {
        String key = "player:" + pn;
        for (int i = d.feed.size() - 1; i >= 0; i--) {
            Phones.Post f = d.feed.get(i);
            if (day - f.day > 2) break;
            if (!f.author.equals(key) || !(f.likes.contains(p.id) || f.commented(p.id))) continue;
            if (p.mind.lastVisit.containsKey("seenpost:" + f.id)) continue;
            p.mind.lastVisit.put("seenpost:" + f.id, day);
            String t = f.text.replaceAll("\\[(pic|scene):[^\\]]*\\]", "").trim();
            if (f.text.contains("[pic:")) return "I saw your photo on SolFeed - great shot!";
            if (t.length() > 40) t = t.substring(0, 37) + "...";
            return "I saw your post on SolFeed - \"" + t + "\" ☺";
        }
        return null;
    }

    /* ================================================================ ticking & actions */

    public static void tick(ServerLevel sl, CityData d) {
        long now = sl.getGameTime();
        if (now % 20 == 3) {
            batteryTick(sl, d);
            long day = Calendar.worldDay(sl);
            int tod = Phones.tod(sl);
            groupChatter(sl, d, day, tod);
            launchTick(sl, d, day, tod);
            routines(sl, d, day, tod);
            if (tod > 12000 && tod < 17000) gamingNights(sl, d, day);
        }
        if (now % 10 == 5) guideTick(sl, d);
        if (now % 60 == 11) devRefresh(sl);
        CityData.managerAva = d.avaPromoted;
    }

    public static boolean action(ServerPlayer pl, CityData d, PcNet.Act a) {
        ServerLevel sl = pl.serverLevel();
        String pn = pl.getName().getString();
        switch (a.kind) {
            case "ferry_call" -> {
                int side = pl.getY() > 150 ? Ferry.ISLE : Ferry.CITY;
                Ferry.call(sl, side);
                Phones.toast(pl, "Sky Ferry", "Pickup requested - the ferry is heading to " + (side == Ferry.ISLE ? "the Neon Heights terminal." : "the city pad by the pier."));
            }
            case "directions" -> directions(pl, d, a.a);
            case "sky" -> Skydive.report(pl, a.a);
            case "devmode" -> {
                int m = Phones.parse(a.a);
                if (m <= 0) DEV_MODE.remove(pl.getUUID());
                else DEV_MODE.put(pl.getUUID(), m);
                relay(pl, "#dev|" + pl.getId() + "|" + Math.max(0, m));
            }
            case "devact" -> relay(pl, "#deva|" + pl.getId() + "|" + (a.a.equals("type") ? "type" : "tap"));
            case "tubeup" -> {
                String key = a.a.replaceAll("[^a-z0-9_]", "");
                if (!key.startsWith("vd_")) return true;
                String title = a.b.replace("|", "/").trim();
                if (title.isEmpty()) title = "My video";
                if (title.length() > 60) title = title.substring(0, 60);
                d.uploads.removeIf(u -> u.startsWith(key + "|"));
                d.uploads.add(key + "|" + title + "|" + pn + "|" + Calendar.worldDay(pl.serverLevel()) + "|0");
                while (d.uploads.size() > 60) d.uploads.remove(0);
                d.setDirty();
                Phones.post(d, Bank.playerKey(pn), "[pic:" + key + "] New on SolTube: " + title, Calendar.worldDay(pl.serverLevel()), Phones.tod(pl.serverLevel()));
                d.news(Calendar.worldDay(pl.serverLevel()), pn + " uploaded a new SolTube video: \"" + title + "\"");
                Phones.toast(pl, "SolTube", "\"" + title + "\" is live on SolTube!");
                PcNet.send(pl, Computers.data(pl, a.pos));
            }
            case "tvshow" -> {
                BlockPos tv = BlockPos.of(Long.parseLong(a.b.split(";")[0]));
                String[] rest = a.b.split(";", 3);
                TvShows.set(pl, tv, a.a, rest.length > 1 ? Phones.parse(rest[1]) : -1);
            }
            case "tvoff" -> {
                BlockPos tv = BlockPos.of(Long.parseLong(a.a));
                if (tv.distSqr(pl.blockPosition()) < 32 * 32) TvShows.clear(pl.serverLevel(), tv);
            }
            case "snap" -> {
                snap(pl, d);
                PcNet.send(pl, Computers.data(pl, a.pos));
            }
            case "unsnap" -> {
                List<String> g = gallery(d, pn);
                int i = Phones.parse(a.a);
                if (i >= 0 && i < g.size()) g.remove(i);
                d.setSetting(pn, "gallery", String.join(",", g));
                PcNet.send(pl, Computers.data(pl, a.pos));
            }
            case "pay" -> {
                CityData.Profile p = d.profiles.get(a.a);
                if (p != null && Bank.appPay(pl, p.name, a.b) > 0) Phones.toast(pl, "Bank", "Sent " + a.b + " coins to " + p.name + ".");
                PcNet.send(pl, Computers.data(pl, a.pos));
            }
            case "food" -> {
                order(pl, d, a.a, a.b);
                PcNet.send(pl, Computers.data(pl, a.pos));
            }
            case "setting" -> {
                switch (a.a) {
                    case "dnd", "ring", "wall", "h24", "preview", "labels" -> d.setSetting(pn, a.a, a.b.replaceAll("[|,:]", ""));
                    case "case" -> {
                        ItemStack st = phoneStack(pl);
                        if (!st.isEmpty()) st.getOrCreateTag().putInt("Case", Math.floorMod(Phones.parse(a.b), CASES.length));
                    }
                    default -> { return true; }
                }
                PcNet.send(pl, Computers.data(pl, a.pos));
            }
            case "follow" -> {
                List<String> f = follows(d, pn);
                if (!f.remove(a.a)) {
                    f.add(a.a);
                    CityData.Profile p = d.profiles.get(a.a);
                    if (p != null) {
                        p.mind.trust.put(pn, Math.min(100, p.mind.trustIn(pn) + 2));
                        Phones.toast(pl, "SolFeed", "You're now following " + p.name + ".");
                    }
                }
                d.setSetting(pn, "follows", String.join(",", f));
                PcNet.send(pl, Computers.data(pl, a.pos));
            }
            default -> { return false; }
        }
        return true;
    }

    static final Map<UUID, Integer> DEV_MODE = new HashMap<>();

    static void relayAll(ServerPlayer pl, String line) {
        for (ServerPlayer o : pl.serverLevel().players()) if (o.distanceToSqr(pl) < 96 * 96) PcNet.send(o, new PcNet.Msg(line));
    }

    static void relay(ServerPlayer pl, String line) {
        for (ServerPlayer o : pl.serverLevel().players()) if (o != pl && o.distanceToSqr(pl) < 96 * 96) PcNet.send(o, new PcNet.Msg(line));
    }

    /** Every few seconds, re-tell everyone which players are using a device (covers players who just arrived). */
    static void devRefresh(ServerLevel sl) {
        DEV_MODE.keySet().removeIf(id -> sl.getServer().getPlayerList().getPlayer(id) == null);
        for (Map.Entry<UUID, Integer> en : DEV_MODE.entrySet()) {
            ServerPlayer pl = sl.getServer().getPlayerList().getPlayer(en.getKey());
            if (pl != null && pl.serverLevel() == sl) relay(pl, "#dev|" + pl.getId() + "|" + en.getValue());
        }
    }

    public static void fill(ServerPlayer pl, CityData d, PcNet.Data o) {
        String pn = pl.getName().getString();
        ItemStack st = phoneStack(pl);
        o.battery = st.isEmpty() ? 100 : battery(st);
        o.charging = charging(pl);
        o.settings = (dnd(d, pn) ? "1" : "0") + "|" + ringtone(d, pn) + "|" + (st.hasTag() ? st.getTag().getInt("Case") : 0) + "|" + (st.hasTag() ? Math.max(1, st.getTag().getInt("Model")) : 1) + "|" + d.setting(pn, "wall", "0") + "|" + d.setting(pn, "h24", "0") + "|" + d.setting(pn, "preview", "1") + "|" + d.setting(pn, "labels", "1");
        o.follows.addAll(follows(d, pn));
        o.menu.addAll(menu(pl.serverLevel(), d));
        o.orders.addAll(orders(d, pn));
        o.gallery.addAll(gallery(d, pn));
        for (Group g : groups(d, pn)) o.residents.add(new String[]{g.id(), g.name(), "Group chat · " + g.members().size() + " members", "0", "1", "-1"});
    }
}
