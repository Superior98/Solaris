package com.fireheart.city;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.IItemHandler;

/** Fireheart Post: residents, the bank and the mayor write letters; Pip the postman carries them around town. */
public final class Post {
    private Post() {}

    public static final String KEY = "post";
    public static final BlockPos SORT = new BlockPos(15, 71, -38);
    public static final BlockPos SORT_STAND = new BlockPos(15, 71, -37);
    public static final BlockPos COUNTER = new BlockPos(17, 71, -36);
    public static final BlockPos COUNTER_STAND = new BlockPos(17, 71, -37);
    public static final BlockPos STAMP = new BlockPos(13, 71, -37);
    public static final BlockPos STAMP_STAND = new BlockPos(14, 71, -37);
    public static final BlockPos MAILBOX = new BlockPos(14, 71, -32);
    public static final BlockPos MAILBOX_STAND = new BlockPos(15, 71, -31);
    public static final BlockPos POSTBOX = new BlockPos(18, 71, -32);

    public static final class Letter {
        public int id;
        public String from = "";
        public String to = "";
        public String text = "";
        public String kind = "";
        public long day;
        public int stage;
        public int tries;
        public String gift = "";
        public int giftCount;
        public String giftNbt = "";
        public String extras = "";
        public long placed, ready, out;
        public String cook = "";
        public int notified;

        public java.util.List<ItemStack> extraStacks() {
            java.util.List<ItemStack> out = new java.util.ArrayList<>();
            if (extras.isEmpty()) return out;
            for (String e : extras.split(",")) {
                String[] p = e.split("\\*");
                if (p[0].startsWith("dish:")) {
                    String src = p[0].substring(5);
                    long at = 0;
                    int ix = src.indexOf('@');
                    if (ix > 0) { try { at = Long.parseLong(src.substring(ix + 1)); } catch (NumberFormatException ignored) {} src = src.substring(0, ix); }
                    out.add(Dishes.make(src, p.length > 1 ? Math.max(1, Integer.parseInt(p[1])) : 1, at));
                    continue;
                }
                net.minecraft.world.item.Item it = Inv.item(p[0]);
                if (it != null) out.add(new ItemStack(it, p.length > 1 ? Math.max(1, Integer.parseInt(p[1])) : 1));
            }
            return out;
        }

        public ItemStack giftStack() {
            if (gift.isEmpty() || giftCount <= 0) return ItemStack.EMPTY;
            if (giftNbt.contains("Dish:") && Dishes.BY_SOURCE.containsKey(gift)) {
                try { return Dishes.make(gift, giftCount, net.minecraft.nbt.TagParser.parseTag(giftNbt).getLong("CookedAt")); } catch (Exception ignored) {}
            }
            net.minecraft.world.item.Item it = Inv.item(gift);
            if (it == null) return ItemStack.EMPTY;
            ItemStack st = new ItemStack(it, giftCount);
            if (!giftNbt.isEmpty()) {
                try { st.setTag(net.minecraft.nbt.TagParser.parseTag(giftNbt)); } catch (Exception ignored) {}
            }
            return st;
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putInt("id", id);
            t.putString("from", from);
            t.putString("to", to);
            t.putString("text", text);
            t.putString("kind", kind);
            t.putLong("day", day);
            t.putInt("stage", stage);
            t.putInt("tries", tries);
            t.putString("gift", gift);
            t.putInt("giftCount", giftCount);
            t.putString("giftNbt", giftNbt);
            t.putString("extras", extras);
            t.putLong("placed", placed);
            t.putLong("ready", ready);
            t.putLong("out", out);
            t.putString("cook", cook);
            t.putInt("notified", notified);
            return t;
        }

        static Letter load(CompoundTag t) {
            Letter l = new Letter();
            l.id = t.getInt("id");
            l.from = t.getString("from");
            l.to = t.getString("to");
            l.text = t.getString("text");
            l.kind = t.getString("kind");
            l.day = t.getLong("day");
            l.stage = t.getInt("stage");
            l.tries = t.getInt("tries");
            l.gift = t.getString("gift");
            l.giftCount = t.getInt("giftCount");
            l.giftNbt = t.getString("giftNbt");
            l.extras = t.getString("extras");
            l.placed = t.getLong("placed");
            l.ready = t.getLong("ready");
            l.out = t.getLong("out");
            l.cook = t.getString("cook");
            l.notified = t.getInt("notified");
            return l;
        }

        public boolean toPlayer() {
            return to.startsWith("player:");
        }
    }

    public static Letter send(CityData d, String from, String to, String text, String kind, long day) {
        Letter l = new Letter();
        l.id = d.civic.nextLetter++;
        l.from = from;
        l.to = to;
        l.text = text;
        l.kind = kind;
        l.day = day;
        d.civic.mail.add(l);
        while (d.civic.mail.size() > 80) {
            Letter old = null;
            for (Letter x : d.civic.mail) if (x.stage == 2) { old = x; break; }
            d.civic.mail.remove(old != null ? old : d.civic.mail.get(0));
        }
        d.setDirty();
        return l;
    }

    public static String sender(CityData d, String from) {
        return switch (from) {
            case "bank" -> "Solaris City Bank";
            case "mayor" -> "The Mayor's Office";
            case "city" -> "City of Solaris";
            case "tech" -> "SolTech";
            default -> {
                CityData.Profile p = d.profiles.get(from);
                yield p == null ? from : p.name;
            }
        };
    }

    public static int waiting(CityData d) {
        int n = 0;
        for (Letter l : d.civic.mail) if (l.stage < 2) n++;
        return n;
    }

    // ------------------------------------------------------------ writing letters

    public static void tick(ServerLevel sl, CityData d) {
        long day = Calendar.worldDay(sl);
        long tod = Math.floorMod(sl.getDayTime(), 24000L);
        if (tod < 500 || tod > 9000) return;
        RandomSource r = sl.random;
        if (d.civic.mailDay != day) {
            d.civic.mailDay = day;
            for (CityData.Profile p : d.profiles.values()) {
                if (r.nextFloat() > 0.22f) continue;
                String to = !p.partner.isEmpty() ? p.partner : null;
                List<String> fr = d.friendsOf(p.id);
                if (fr.isEmpty()) for (CityData.Profile o : d.profiles.values()) {
                    CityData.Rel rr = d.peekRel(p.id, o.id);
                    if (rr != null && rr.met && rr.fam >= 25 && !rr.rival) fr.add(o.id);
                }
                if (to == null || r.nextBoolean()) to = fr.isEmpty() ? to : fr.get(r.nextInt(fr.size()));
                if (to == null || to.equals(p.id)) continue;
                CityData.Profile q = d.profiles.get(to);
                if (q == null) continue;
                send(d, p.id, to, friendLetter(d, p, q, r, day), "friend", day);
            }
            d.setDirty();
        }
        if (d.civic.playerMailDay != day && tod > 2000) {
            d.civic.playerMailDay = day;
            for (ServerPlayer pl : sl.players()) playerLetter(sl, d, pl.getName().getString(), day, r);
        }
    }

    static String friendLetter(CityData d, CityData.Profile p, CityData.Profile q, RandomSource r, long day) {
        CityData.Rel rel = d.rel(p.id, q.id);
        StringBuilder sb = new StringBuilder("Dear " + q.name + ",\n\n");
        boolean partner = p.partner.equals(q.id);
        List<String> bits = new ArrayList<>();
        if (!rel.shared.isEmpty()) bits.add("I keep thinking about " + rel.shared.get(rel.shared.size() - 1) + ". Let's do it again soon!");
        if (partner) bits.add("I just wanted to say I love you. Every day with you is my favourite day.");
        if (!p.yesterday.notes.isEmpty()) bits.add("Guess what? " + Events.sentence(DayLog.lower(p.yesterday.notes.get(p.yesterday.notes.size() - 1))) + "!");
        String pet = rel.facts.get("pet");
        if (pet != null) bits.add("Give " + pet + " a scratch behind the ears from me.");
        if (p.goalCost > 0 && Bank.savings(d, p.id) > 0) bits.add("I'm up to " + Bank.savings(d, p.id) + " coins saved for " + p.goal + "!");
        String l = Lottery.line(d, p);
        if (l != null) bits.add(l);
        bits.add(r.nextBoolean() ? "Come and see me at " + p.job.work().label + " sometime!" : "Solaris wouldn't be the same without you.");
        java.util.Collections.shuffle(bits, new java.util.Random(r.nextLong()));
        for (int i = 0; i < Math.min(2, bits.size()); i++) sb.append(bits.get(i)).append("\n\n");
        sb.append(partner ? "All my love,\n" : rel.bestFriend() ? "Your best friend,\n" : "Your friend,\n").append(p.name);
        return sb.toString();
    }

    public static void playerLetter(ServerLevel sl, CityData d, String player, long day, RandomSource r) {
        List<CityData.Profile> fans = new ArrayList<>();
        for (CityData.Profile p : d.profiles.values()) {
            CityData.Rel pr = d.playerRel(p.id, player);
            if (pr.met && pr.fam >= 12) fans.add(p);
        }
        if (fans.isEmpty() || r.nextFloat() > 0.8f) return;
        CityData.Profile p = fans.get(r.nextInt(fans.size()));
        CityData.Rel pr = d.playerRel(p.id, player);
        List<String> bits = new ArrayList<>();
        long bd = Memory.nextBirthday(p, day);
        if (bd - day <= 3) bits.add("My birthday is " + (bd == day ? "today" : "on " + Calendar.name(bd)) + "! There's a party at " + (p.livesOnIsland() ? "the Neon Heights plaza" : "Solaris Plaza") + " in the evening. You're invited!");
        if (p.goalCost > 0) bits.add("I'm saving up for " + p.goal + " - " + Bank.savings(d, p.id) + " of " + p.goalCost + " coins so far. Hugo at the bank says I'm doing great.");
        if (!p.lastGoal.isEmpty() && day - p.lastGoalDay <= 4) bits.add("I finally bought " + p.lastGoal + "! Come and see it!");
        if (!p.yesterday.empty()) bits.add("Yesterday? " + p.yesterday.story(p, Calendar.weekend(p.yesterday.day)));
        CityData.Event e = Events.freshestUnshared(d, p, player);
        if (e != null) bits.add("Have you heard? " + Events.sentence(e.text) + "!");
        if (!p.partner.isEmpty() && d.profiles.get(p.partner) != null) bits.add(d.profiles.get(p.partner).name + " and I are doing wonderfully, by the way.");
        String lt = Lottery.line(d, p);
        if (lt != null) bits.add(lt);
        if (!d.civic.mayor.isEmpty() && d.profiles.get(d.civic.mayor) != null) bits.add(d.civic.mayor.equals(p.id) ? "Being Mayor is hard work, but I love it." : "What do you think of Mayor " + d.profiles.get(d.civic.mayor).name + "? " + Mayor.opinion(d, p));
        if (pr.aff > 15) bits.add("Thank you for everything you do for Solaris. It really feels like home.");
        java.util.Collections.shuffle(bits, new java.util.Random(r.nextLong()));
        StringBuilder sb = new StringBuilder("Dear " + player + ",\n\n");
        for (int i = 0; i < Math.min(2, bits.size()); i++) sb.append(bits.get(i)).append("\n\n");
        sb.append(pr.aff > 30 ? "Your good friend,\n" : "Best wishes,\n").append(p.name).append("\n").append(p.jobTitle()).append(", ").append(p.job.work().label);
        send(d, p.id, Bank.playerKey(player), sb.toString(), "friend", day);
    }

    // ------------------------------------------------------------ delivery

    public static BlockPos target(ServerLevel sl, CityData d, Letter l) {
        if (l.toPlayer()) {
            ServerPlayer pl = sl.getServer().getPlayerList().getPlayerByName(l.to.substring(7));
            if (pl == null || pl.level() != sl || pl.getY() > 150 || l.tries >= 2) return MAILBOX_STAND;
            if (pl.blockPosition().distSqr(COUNTER) > 110 * 110) return MAILBOX_STAND;
            BlockPos s = Work.stand(sl, pl.blockPosition());
            return s == null ? MAILBOX_STAND : s;
        }
        CityData.Profile p = d.profiles.get(l.to);
        if (p == null) return null;
        if (p.livesOnIsland()) return Place.CITY_PORT;
        Place h = p.homePlace();
        if (h == null) return null;
        return h.key.startsWith("apt") ? Place.APT_LOBBY : h.pos;
    }

    public static String deliver(Resident postman, ServerLevel sl, CityData d, Letter l) {
        long day = Calendar.worldDay(sl);
        if (l.toPlayer()) {
            String name = l.to.substring(7);
            ServerPlayer pl = sl.getServer().getPlayerList().getPlayerByName(name);
            if (pl != null && pl.level() == sl && pl.distanceToSqr(postman) < 7 * 7) {
                give(pl, book(d, l));
                for (ItemStack ex : l.extraStacks()) give(pl, ex);
                if (!l.giftStack().isEmpty()) {
                    give(pl, l.giftStack());
                    pl.sendSystemMessage(Component.literal("§6[Solaris Post] §eThere's a parcel with it: §f" + l.giftCount + "x " + Economy.label(l.gift)));
                }
                postman.getLookControl().setLookAt(pl, 30, 30);
                postman.gesture(Resident.G_GIVE, 40);
                postman.showItem("minecraft:written_book", 40);
                postman.sayTo(postman.pick("Letter for you, " + name + "! It's from " + sender(d, l.from) + ".", "Special delivery for " + name + "!", "Post! One letter from " + sender(d, l.from) + "."), 80);
                sl.playSound(null, pl.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
                pl.sendSystemMessage(Component.literal("§6[Solaris Post] §eYou received a letter from " + sender(d, l.from) + "."));
                l.stage = 2;
                d.civic.delivered++;
                return "I delivered a letter to " + name + " in person";
            }
            if (postman.blockPosition().closerThan(MAILBOX, 3)) {
                if (toMailbox(sl, d, l)) {
                    postman.showItem("minecraft:written_book", 30);
                    return "I put a letter in " + name + "'s mailbox";
                }
                return null;
            }
            l.tries++;
            if (sl.random.nextFloat() < 0.5f) postman.sayTo(postman.pick("Hmm, where did " + name + " go?", name + "! Wait up, I've got post for you!"), 50);
            return null;
        }
        CityData.Profile p = d.profiles.get(l.to);
        l.stage = 2;
        d.civic.delivered++;
        if (p == null) return null;
        if (d.profiles.containsKey(l.from)) {
            CityData.Rel rel = d.rel(p.id, l.from);
            rel.aff = Math.min(100, rel.aff + 3);
            rel.facts.put("letter", String.valueOf(day));
        }
        p.log(Calendar.day(sl)).note("I got a letter from " + sender(d, l.from));
        p.social = Math.min(100, p.social + 6);
        postman.gesture(Resident.G_GIVE, 30);
        postman.showItem("minecraft:paper", 30);
        sl.playSound(null, postman.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.NEUTRAL, 0.6f, 1.2f);
        d.setDirty();
        return p.livesOnIsland() ? "I sent " + p.name + "'s letter up to Neon Heights on the Sky Ferry" : "I delivered a letter to " + p.name;
    }

    /** The player's own mailbox and the spot in front of it, or null if they haven't placed one. */
    public static BlockPos[] mailSpot(ServerLevel sl, CityData d, Letter l) {
        if (!l.toPlayer()) return null;
        BlockPos mb = MailboxBlock.of(d, l.to.substring(7));
        if (mb == null || !sl.isLoaded(mb) || !(sl.getBlockState(mb).getBlock() instanceof MailboxBlock)) return null;
        net.minecraft.core.Direction f = sl.getBlockState(mb).getValue(MailboxBlock.FACING);
        return new BlockPos[]{mb, mb.relative(f)};
    }

    static BlockPos doorstep(ServerLevel sl, BlockPos mb, BlockPos stand) {
        net.minecraft.core.Direction f = sl.getBlockState(mb).getValue(MailboxBlock.FACING);
        BlockPos[] tries = {mb.relative(f.getClockWise()), mb.relative(f.getCounterClockWise()), stand.relative(f.getClockWise()), stand.relative(f.getCounterClockWise()), mb.relative(f.getClockWise(), 2), mb.relative(f.getCounterClockWise(), 2)};
        for (BlockPos p : tries) for (int dy = 1; dy >= -1; dy--) {
            BlockPos q = p.above(dy);
            if (sl.getBlockState(q).isAir() && sl.getBlockState(q.below()).isFaceSturdy(sl, q.below(), net.minecraft.core.Direction.UP)) return q;
        }
        return null;
    }

    /** Postman at the player's mailbox: letters and gifts go inside (flag up); food orders are left in a box at the door. */
    public static String toPlayerBox(Resident postman, ServerLevel sl, CityData d, Letter l, BlockPos mb, BlockPos stand) {
        String name = l.to.substring(7);
        if (!(sl.getBlockState(mb).getBlock() instanceof MailboxBlock)) return deliver(postman, sl, d, l);
        postman.getLookControl().setLookAt(mb.getX() + 0.5, mb.getY() + 0.8, mb.getZ() + 0.5, 30, 30);
        ServerPlayer pl = sl.getServer().getPlayerList().getPlayerByName(name);
        l.stage = 2;
        d.civic.delivered++;
        d.setDirty();
        if (l.kind.equals("parcel")) {
            BlockPos at = doorstep(sl, mb, stand);
            java.util.List<ItemStack> items = new java.util.ArrayList<>();
            items.add(l.giftStack());
            items.addAll(l.extraStacks());
            items.add(book(d, l));
            if (at == null) {
                MailboxBlock.deposit(sl, d, mb, items);
            } else {
                net.minecraft.core.Direction f = sl.getBlockState(mb).getValue(MailboxBlock.FACING);
                ParcelBlock.leave(sl, d, at, f, name, items);
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, at.getX() + 0.5, at.getY() + 0.3, at.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.01);
            }
            postman.gesture(Resident.G_GIVE, 40);
            postman.showItem("fireheartcity:delivery_box", 30);
            sl.playSound(null, stand, SoundEvents.WOOL_PLACE, SoundSource.NEUTRAL, 1f, 0.9f);
            postman.say(postman.pick("SolEats delivery for " + name + "!", "One " + Economy.label(l.gift).replaceFirst("^(a|an|some) ", "") + ", left at the door.", "Food's here! *knocks*"), 70);
            sl.playSound(null, stand, SoundEvents.WOOD_HIT, SoundSource.NEUTRAL, 1f, 1f);
            if (pl != null) Phones.toast(pl, "SolEats", "Your " + Economy.label(l.gift) + " was left at your front door.");
            return "I left a SolEats box at " + name + "'s front door";
        }
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        items.add(book(d, l));
        if (!l.giftStack().isEmpty()) items.add(l.giftStack());
        MailboxBlock.setOpen(sl, mb, true);
        postman.gesture(Resident.G_GIVE, 50);
        postman.showItem(l.giftStack().isEmpty() ? "minecraft:written_book" : "fireheartcity:delivery_box", 40);
        Phones.LATER.add(new Object[]{sl.getGameTime() + 25, (Runnable) () -> {
            MailboxBlock.deposit(sl, d, mb, items);
            sl.playSound(null, mb, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 1f, 1f);
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, mb.getX() + 0.5, mb.getY() + 1.1, mb.getZ() + 0.5, 5, 0.3, 0.2, 0.3, 0);
        }});
        Phones.LATER.add(new Object[]{sl.getGameTime() + 45, (Runnable) () -> MailboxBlock.setOpen(sl, mb, false)});
        postman.say(postman.pick("Post for " + name + "!", "One letter" + (l.giftStack().isEmpty() ? "" : " and a parcel") + " for " + name + ".", "*pops the post in the mailbox*", "Flag's up - " + name + " has mail!"), 70);
        if (pl != null) Phones.toast(pl, "Solaris Post", "You've got mail from " + sender(d, l.from) + (l.giftStack().isEmpty() ? "" : " (with a parcel)") + " - check your mailbox!");
        return "I put " + name + "'s post in their mailbox";
    }

    static boolean toMailbox(ServerLevel sl, CityData d, Letter l) {
        IItemHandler h = Inv.handler(sl, MAILBOX);
        if (h == null) return false;
        ItemStack rest = book(d, l);
        for (int i = 0; i < h.getSlots() && !rest.isEmpty(); i++) rest = h.insertItem(i, rest, false);
        if (!rest.isEmpty()) return false;
        ItemStack parcel = l.giftStack();
        for (int i = 0; i < h.getSlots() && !parcel.isEmpty(); i++) parcel = h.insertItem(i, parcel, false);
        for (ItemStack ex : l.extraStacks()) for (int i = 0; i < h.getSlots() && !ex.isEmpty(); i++) ex = h.insertItem(i, ex, false);
        l.stage = 2;
        d.civic.delivered++;
        d.setDirty();
        return true;
    }

    static void give(ServerPlayer pl, ItemStack st) {
        if (!pl.getInventory().add(st) && !st.isEmpty()) pl.drop(st, false);
    }

    public static ItemStack book(CityData d, Letter l) {
        ItemStack st = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = st.getOrCreateTag();
        String s = sender(d, l.from);
        String title = "Letter from " + s;
        tag.putString("title", title.length() > 32 ? title.substring(0, 32) : title);
        tag.putString("author", s);
        tag.putBoolean("resolved", true);
        ListTag pages = new ListTag();
        String head = "§8Solaris Post · Day " + (l.day + 1) + "§r\n\n";
        StringBuilder cur = new StringBuilder(head);
        for (String para : l.text.split("\n\n")) {
            if (cur.length() + para.length() > 230 && cur.length() > head.length()) {
                pages.add(StringTag.valueOf(json(cur.toString())));
                cur = new StringBuilder();
            }
            cur.append(para).append("\n\n");
        }
        if (cur.length() > 0) pages.add(StringTag.valueOf(json(cur.toString().trim())));
        tag.put("pages", pages);
        return st;
    }

    static String json(String s) {
        StringBuilder b = new StringBuilder("{\"text\":\"");
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                default -> b.append(ch);
            }
        }
        return b.append("\"}").toString();
    }

    static final BlockPos BRO_MAILBOX = new BlockPos(-46, 71, 39);

    /** Once: a mailbox by magmagamer9's front door, facing the road. */
    public static void setupMailboxes(ServerLevel sl, CityData d) {
        if (d.mailboxSetup || sl.getChunkSource().getChunkNow(BRO_MAILBOX.getX() >> 4, BRO_MAILBOX.getZ() >> 4) == null) return;
        d.mailboxSetup = true;
        d.setDirty();
        if (!sl.getBlockState(BRO_MAILBOX).isAir() && !sl.getBlockState(BRO_MAILBOX).canBeReplaced()) return;
        sl.setBlock(BRO_MAILBOX, FireheartCity.MAILBOX.get().defaultBlockState().setValue(MailboxBlock.FACING, net.minecraft.core.Direction.EAST), 3);
        d.mailboxes.put(BRO_MAILBOX.asLong(), "magmagamer9");
        MailboxBlock.deposit(sl, d, BRO_MAILBOX, java.util.List.of(book(d, welcome())));
        FireheartCity.LOG.info("Placed magmagamer9's mailbox at " + BRO_MAILBOX.toShortString());
    }

    static Letter welcome() {
        Letter l = new Letter();
        l.from = "post";
        l.to = "player:magmagamer9";
        l.text = "Dear magmagamer9,\n\nWelcome to your new mailbox! From now on I'll leave your letters and gifts right here, and SolEats orders at your front door.\n\nSee you on my rounds!\nPip, Solaris Post";
        return l;
    }
}
