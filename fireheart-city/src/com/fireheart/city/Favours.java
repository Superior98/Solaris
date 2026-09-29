package com.fireheart.city;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Residents ask the player for favours: bring them items, get paid (into your bank account if you have one). */
public final class Favours {
    private Favours() {}

    public static final int DAYS = 3;

    public static final class Request {
        public String resident = "";
        public String player = "";
        public String item = "";
        public int count;
        public int reward;
        public long day;
        public String why = "";
        public String noun = "";

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putString("resident", resident);
            t.putString("player", player);
            t.putString("item", item);
            t.putInt("count", count);
            t.putInt("reward", reward);
            t.putLong("day", day);
            t.putString("why", why);
            t.putString("noun", noun);
            return t;
        }

        static Request load(CompoundTag t) {
            Request r = new Request();
            r.resident = t.getString("resident");
            r.player = t.getString("player");
            r.item = t.getString("item");
            r.count = t.getInt("count");
            r.reward = t.getInt("reward");
            r.day = t.getLong("day");
            r.why = t.getString("why");
            r.noun = t.getString("noun");
            return r;
        }
    }

    private record Want(String item, int count, String noun, String why) {}

    static List<Want> wants(CityData.Profile p) {
        List<Want> w = new ArrayList<>();
        switch (p.job) {
            case BAKER -> { w.add(new Want("minecraft:sugar", 4, "sugar", "for a batch of cakes")); w.add(new Want("minecraft:egg", 6, "eggs", "for the morning pastries")); }
            case COOK -> { w.add(new Want("minecraft:beef", 4, "raw beef", "for the burger special")); w.add(new Want("minecraft:potato", 6, "potatoes", "for the chip fryer")); }
            case NOODLE_CHEF -> { w.add(new Want("minecraft:red_mushroom", 4, "red mushrooms", "for my secret broth")); w.add(new Want("minecraft:carrot", 5, "carrots", "for the veggie noodles")); }
            case GROCER -> w.add(new Want("minecraft:melon_slice", 8, "melon slices", "for the fruit stall"));
            case MECHANIC -> { w.add(new Want("minecraft:iron_ingot", 4, "iron ingots", "to patch up a rusty car")); w.add(new Want("minecraft:redstone", 6, "redstone", "for a tricky wiring job")); }
            case FACTORY_WORKER -> w.add(new Want("minecraft:iron_ingot", 6, "iron ingots", "to keep the press running"));
            case QUARRY_WORKER -> w.add(new Want("minecraft:coal", 6, "coal", "for the site stove"));
            case CRANE_OPERATOR -> w.add(new Want("minecraft:chain", 4, "chains", "to fix the crane hook"));
            case DOCKMASTER -> w.add(new Want("minecraft:string", 5, "string", "to mend the fishing nets"));
            case GARDENER -> { w.add(new Want("minecraft:bone_meal", 8, "bone meal", "for the flower beds")); w.add(new Want("minecraft:oak_sapling", 3, "oak saplings", "to plant along the road")); }
            case ATTENDANT -> w.add(new Want("minecraft:bucket", 2, "buckets", "for spilled diesel"));
            case CLOCKKEEPER -> w.add(new Want("minecraft:gold_ingot", 2, "gold ingots", "to repair the clock hands"));
            case LIBRARIAN -> { w.add(new Want("minecraft:book", 3, "books", "for the new mystery shelf")); w.add(new Want("minecraft:paper", 8, "paper", "for library cards")); }
            case ARCADE_KEEPER -> w.add(new Want("minecraft:glowstone_dust", 6, "glowstone dust", "for the cabinet lights"));
            case GUIDE -> w.add(new Want("minecraft:map", 2, "maps", "for the tourists"));
            case PILOT -> w.add(new Want("minecraft:feather", 6, "feathers", "for my lucky flight charm"));
            case BANKER -> w.add(new Want("minecraft:gold_nugget", 9, "gold nuggets", "to test the new coin scale"));
            case POSTMAN -> w.add(new Want("minecraft:paper", 10, "paper", "for envelopes"));
            case MUSICIAN -> w.add(new Want("minecraft:note_block", 2, "note blocks", "to tune a new song"));
            case RECEPTIONIST -> w.add(new Want("minecraft:sunflower", 3, "sunflowers", "to brighten up the lobby"));
            default -> {}
        }
        w.add(new Want("minecraft:poppy", 3, "poppies", "for someone special"));
        w.add(new Want("minecraft:bread", 3, "loaves of bread", "for a picnic at the pier"));
        w.add(new Want("minecraft:torch", 8, "torches", "for my apartment - it's so dark at night"));
        w.add(new Want("minecraft:cookie", 4, "cookies", "because I'm craving something sweet"));
        return w;
    }

    public static Request of(CityData d, String resident) {
        return d.civic.favours.get(resident);
    }

    public static Request ask(ServerLevel sl, CityData d, Resident r, CityData.Profile p, ServerPlayer pl) {
        String pn = pl.getName().getString();
        long day = Calendar.worldDay(sl);
        List<Want> ws = wants(p);
        Want w = ws.get(sl.random.nextInt(ws.size()));
        Request q = new Request();
        q.resident = p.id;
        q.player = pn;
        q.item = w.item;
        q.count = w.count;
        q.why = w.why;
        q.noun = w.noun;
        q.day = day;
        int cost = Math.max(1, Economy.price(w.item)) * w.count;
        q.reward = Math.max(8, Math.min(40, cost + 6 + sl.random.nextInt(6)));
        d.civic.favours.put(p.id, q);
        d.setDirty();
        r.getLookControl().setLookAt(pl, 30, 30);
        r.gesture(Resident.G_THINK, 60);
        r.sayTo("Oh, " + pn + "! Could you do me a favour? I need " + w.count + " " + w.noun + " " + w.why + ". I'll pay you " + q.reward + " coins!", 140);
        MutableComponent m = Component.literal("§6[Favour] §e" + p.name + "§7 asks for §f" + w.count + " " + w.noun + "§7 " + w.why + " · reward §a" + q.reward + " coins§7. Right-click " + p.name + " while carrying them. ");
        m.append(Component.literal("[all favours]").withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/favours"))));
        pl.sendSystemMessage(m);
        p.log(Calendar.day(sl)).note("I asked " + pn + " for " + w.count + " " + w.noun);
        return q;
    }

    static int count(Inventory inv, Item it) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(it)) n += inv.getItem(i).getCount();
        return n;
    }

    public static boolean tryComplete(ServerLevel sl, CityData d, Resident r, CityData.Profile p, ServerPlayer pl) {
        Request q = of(d, p.id);
        if (q == null || !q.player.equals(pl.getName().getString())) return false;
        Item it = Inv.item(q.item);
        Inventory inv = pl.getInventory();
        int have = count(inv, it);
        if (have == 0) return false;
        if (have < q.count) {
            r.sayTo("Still hoping for those " + q.count + " " + noun(q) + " - " + (have > 0 ? "you've got " + have + " so far!" : "no rush!"), 80);
            return true;
        }
        int left = q.count;
        ItemStack held = pl.getMainHandItem();
        if (held.is(it)) { int t = Math.min(left, held.getCount()); held.shrink(t); left -= t; }
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack st = inv.getItem(i);
            if (!st.is(it)) continue;
            int t = Math.min(left, st.getCount());
            st.shrink(t);
            left -= t;
        }
        inv.setChanged();
        String pn = pl.getName().getString();
        long rd = Calendar.day(sl);
        int tod = (int) Math.floorMod(sl.getDayTime(), 24000L);
        int paid = 0;
        String key = Bank.playerKey(pn);
        boolean account = Bank.holder(d, pn) != null;
        String dest = account ? Bank.sav(key) : CityData.CITY;
        int fromWallet = Math.min(q.reward, p.coins);
        if (fromWallet > 0 && d.pay(p.id, dest, fromWallet, "Favour for " + pn, rd, tod, false)) paid += fromWallet;
        int rest = Math.min(q.reward - paid, Bank.savings(d, p.id));
        if (rest > 0 && d.pay(Bank.sav(p.id), dest, rest, "Favour for " + pn, rd, tod, false)) paid += rest;
        if (!account && paid > 0) Bank.giveCash(pl, paid);
        p.add(q.item, q.count);
        d.civic.favours.remove(p.id);
        d.civic.favoursDone++;
        int mine = d.civic.helped.merge(pn, 1, Integer::sum);
        if (mine == 5) keyToCity(sl, d, pl);
        CityData.Rel pr = d.playerRel(p.id, pn);
        pr.aff = Math.min(100, pr.aff + 15);
        pr.fam = Math.min(100, pr.fam + 5);
        pr.facts.put("favour", noun(q));
        p.rep += 1;
        p.fun = Math.min(100, p.fun + 10);
        p.log(rd).note(pn + " brought me " + q.count + " " + noun(q));
        long day = Calendar.worldDay(sl);
        d.event(day, "favour", pn + " did " + p.name + " a favour and brought " + q.count + " " + noun(q), r.blockPosition(), p.id);
        Mind.playerEvent(d, p, pn, day, "{P} brought me " + q.count + " " + noun(q) + " " + q.why, 3, 6);
        r.gesture(Resident.G_CHEER, 60);
        r.showItem(q.item, 60);
        r.particles(ParticleTypes.HAPPY_VILLAGER, 12);
        sl.playSound(null, r.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.4f, 1.5f);
        String pay = paid <= 0 ? "I'm broke right now, but I owe you one!" : account ? paid + " coins are on their way to your bank account." : "Here's " + paid + " coins in gold.";
        r.sayTo("You're amazing, " + pn + "! Thank you so much! " + pay, 120);
        pl.sendSystemMessage(Component.literal("§6[Favour] §a" + p.name + " is thrilled! §7Reward: " + (paid > 0 ? "§a" + paid + " coins" + (account ? " (paid into your bank account)" : " (gold)") : "§cnone - they couldn't afford it")));
        Post.send(d, p.id, key, "Dear " + pn + ",\n\nThank you again for the " + noun(q) + " - just what I needed " + q.why + "!\n\nIf you ever need anything, you know where to find me: " + p.job.work().label + ".\n\nYour friend,\n" + p.name, "thanks", day);
        d.setDirty();
        return true;
    }

    static void keyToCity(ServerLevel sl, CityData d, ServerPlayer pl) {
        String pn = pl.getName().getString();
        CityData.Profile m = Mayor.mayor(d);
        String by = m == null ? "the people of Solaris" : "Mayor " + m.name;
        ItemStack key = new ItemStack(net.minecraft.world.item.Items.TRIPWIRE_HOOK);
        key.setHoverName(Component.literal("§6§lKey to Solaris"));
        CompoundTag disp = key.getOrCreateTagElement("display");
        net.minecraft.nbt.ListTag lore = new net.minecraft.nbt.ListTag();
        lore.add(net.minecraft.nbt.StringTag.valueOf("{\"text\":\"Awarded to " + pn + " by " + by + "\",\"color\":\"gray\",\"italic\":false}"));
        lore.add(net.minecraft.nbt.StringTag.valueOf("{\"text\":\"for helping 5 residents\",\"color\":\"dark_gray\",\"italic\":false}"));
        disp.put("Lore", lore);
        key.getOrCreateTag().put("Enchantments", new net.minecraft.nbt.ListTag());
        net.minecraft.nbt.CompoundTag ench = new net.minecraft.nbt.CompoundTag();
        ench.putString("id", "minecraft:unbreaking");
        ench.putShort("lvl", (short) 1);
        key.getTag().getList("Enchantments", 10).add(ench);
        key.getTag().putInt("HideFlags", 1);
        Bank.give(pl, key);
        long day = Calendar.worldDay(sl);
        d.event(day, "favour", by + " gave " + pn + " the Key to Solaris for helping so many residents", Mayor.PODIUM);
        Calendar.banner(pl, "§6§l🗝 Key to the City!", "§e" + by + " thanks you for helping 5 residents");
        Fireworks.finale(sl, pl.blockPosition().above(3));
        Post.send(d, m == null ? "city" : "mayor", Bank.playerKey(pn), "Dear " + pn + ",\n\nOn behalf of everyone in Solaris, thank you. You have helped five of our residents with their troubles, and the whole town is talking about it.\n\nPlease accept the Key to the City.\n\n" + (m == null ? "The City of Solaris" : "Mayor " + m.name), "mayor", day);
    }

    public static String noun(Request q) {
        return q.noun.isEmpty() ? q.item.replaceFirst("^.*:", "").replace('_', ' ') : q.noun;
    }

    public static void expire(ServerLevel sl, CityData d) {
        long day = Calendar.worldDay(sl);
        d.civic.favours.values().removeIf(q -> day - q.day > DAYS);
    }

    public static boolean maybeAsk(ServerLevel sl, CityData d, Resident r, CityData.Profile p, ServerPlayer pl, RandomSource rand) {
        if (of(d, p.id) != null) return false;
        if (!Resident.mayAddress(sl, pl.getName().getString()) && !r.isSpeaking()) return false;
        String pn = pl.getName().getString();
        int open = 0;
        for (Request q : d.civic.favours.values()) if (q.player.equals(pn)) open++;
        if (open >= 4) return false;
        CityData.Rel pr = d.playerRel(p.id, pn);
        if (!pr.met || pr.fam < 12) return false;
        String last = pr.facts.get("asked");
        long day = Calendar.worldDay(sl);
        if (last != null && day - Long.parseLong(last) < 2) return false;
        if (rand.nextFloat() > 0.2f) return false;
        pr.facts.put("asked", String.valueOf(day));
        ask(sl, d, r, p, pl);
        return true;
    }

    public static int list(ServerPlayer pl) {
        CityData d = CityData.get(pl.serverLevel());
        String pn = pl.getName().getString();
        long day = Calendar.worldDay(pl.serverLevel());
        int n = 0;
        pl.sendSystemMessage(Component.literal("§6=== Favours for " + pn + " §7(" + d.civic.favoursDone + " done so far)§6 ==="));
        for (Request q : d.civic.favours.values()) {
            if (!q.player.equals(pn)) continue;
            CityData.Profile p = d.profiles.get(q.resident);
            if (p == null) continue;
            long left = DAYS - (day - q.day);
            pl.sendSystemMessage(Component.literal("§e" + p.name + "§7 (" + p.job.work().label + "): §f" + q.count + " " + noun(q) + "§7 " + q.why + " · §a" + q.reward + " coins§7 · " + (left <= 0 ? "last day!" : left + " days left")));
            n++;
        }
        if (n == 0) pl.sendSystemMessage(Component.literal("§7Nobody needs anything right now. Chat with residents you know - they'll ask when they need a hand."));
        return 1;
    }
}
