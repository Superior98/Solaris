package com.fireheart.city;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** FireTech: the city's electronics store. Sells Fireheart PCs and FirePhones to players and residents. */
public final class TechStore {
    private TechStore() {}

    public static final String KEY = "tech";
    public static final BlockPos CENTER = new BlockPos(35, 71, -1);
    public static final BlockPos ENTRANCE = new BlockPos(30, 71, -1);
    public static final BlockPos KIOSK = new BlockPos(37, 72, -1);
    public static final BlockPos COUNTER_FRONT = new BlockPos(36, 71, -1);
    public static final BlockPos CLERK = new BlockPos(38, 71, -1);
    public static final BlockPos[] DEMOS = {new BlockPos(33, 71, -6), new BlockPos(35, 71, -6), new BlockPos(37, 71, -6)};
    public static final int CASE_PRICE = 6;

    public static final int MAX_PHONES = 8, MAX_PCS = 4, WAGE = 12, WAGE_MANAGER = 16;
    public static final int PHONE2 = 60, TRADE_IN = 25, REPAIR = 10, HEADPHONES = 15, TV = 50, TABLET = 55, WATCH = 30, CONSOLE = 45, ACCESSORY = 10, SOLBOX = 150;

    public static int price(String what) {
        return switch (what) {
            case "pc" -> Computers.PRICE;
            case "phone", "gift" -> Phones.PRICE;
            case "phone2" -> PHONE2;
            case "trade" -> PHONE2 - TRADE_IN;
            case "repair" -> REPAIR;
            case "headphones" -> HEADPHONES;
            case "hpmagma" -> HEADPHONES + 20;
            case "tv" -> TV;
            case "tablet" -> TABLET;
            case "watch" -> WATCH;
            case "console" -> CONSOLE;
            case "solbox" -> SOLBOX;
            case "acc" -> ACCESSORY;
            case "case" -> CASE_PRICE;
            default -> -1;
        };
    }

    public static String deviceName(String what) {
        return switch (what) {
            case "pc" -> "Solaris PC";
            case "phone2", "trade" -> "SolPhone 2";
            case "repair" -> "screen repair";
            case "headphones" -> "SolBeats headphones";
            case "hpmagma" -> "SolBeats Magma Edition";
            case "tv" -> "SolTube TV";
            case "tablet" -> "SolPad tablet";
            case "watch" -> "SolWatch";
            case "console" -> "SolStation console";
            case "solbox" -> "SolBox console";
            case "acc" -> "phone case";
            default -> "SolPhone";
        };
    }

    static String stockKey(String what) {
        return switch (what) {
            case "pc", "phone2", "tv" -> what;
            case "trade" -> "phone2";
            case "phone", "gift" -> "phone";
            default -> "";
        };
    }

    public static List<String> catalog(CityData d, long day) {
        List<String> l = new ArrayList<>();
        boolean launched = Extras.launched(d, day);
        if (launched) {
            l.add("phone2|SolPhone 2|" + PHONE2 + "|Dynamic Island, a bigger battery and a triple camera. 8 colours." + stockNote(d, "phone2"));
            l.add("trade|Trade-in for SolPhone 2|" + (PHONE2 - TRADE_IN) + "|Hand in the SolPhone you carry and get " + TRADE_IN + " coins off a SolPhone 2." + stockNote(d, "phone2"));
        }
        l.add("phone|SolPhone|" + Phones.PRICE + "|Texts, calls, SolFeed, maps and games in your pocket. 8 colours." + stockNote(d, "phone"));
        l.add("pc|Solaris PC|" + Computers.PRICE + "|A full desktop with SolOS, games, SolTube and more." + stockNote(d, "pc"));
        l.add("tablet|SolPad tablet|" + TABLET + "|SolOS on the go - every PC app in your hands, anywhere.");
        l.add("tv|SolTube TV|" + TV + "|Watch SolTube on the big screen at home. Charges your phone too." + stockNote(d, "tv"));
        l.add("solbox|SolBox console|" + SOLBOX + "|Our flagship. Plug it in next to a SolTube TV: big-screen games, SolTube and your photos, with a controller.");
        l.add("console|SolStation console|" + CONSOLE + "|All five SolOS games, full screen, with high scores.");
        l.add("watch|SolWatch|" + WATCH + "|The time, messages and calls on your wrist (keep it in your inventory).");
        l.add("headphones|SolBeats headphones|" + HEADPHONES + "|Wear them and right-click to play music. Pick a colour below.");
        l.add("hpmagma|SolBeats Magma Edition|" + (HEADPHONES + 20) + "|Forged in lava: blackstone band, glowing magma cups and ember pads that smoulder while you listen.");
        l.add("acc|Phone case|" + ACCESSORY + "|Clear, leather, sparkle or neon - dress up the SolPhone you carry.");
        l.add("case|Colour swap|" + CASE_PRICE + "|Give the SolPhone you carry a fresh new colour.");
        l.add("gift|Gift a SolPhone|" + Phones.PRICE + "|Send a SolPhone to a resident who doesn't have one." + stockNote(d, "phone"));
        return l;
    }

    /* ---------------------------------------------------------------- stock & staff */

    public static int stock(CityData d, String item) {
        if (item.isEmpty() || item.equals("repair") || item.equals("headphones") || item.equals("hpmagma")) return 99;
        return d.techStock.computeIfAbsent(item, k -> k.equals("pc") ? MAX_PCS - 1 : k.equals("tv") ? 4 : k.equals("phone2") ? 10 : MAX_PHONES - 2);
    }

    static String stockNote(CityData d, String item) {
        int n = stock(d, item);
        return n <= 0 ? " SOLD OUT - new stock arrives " + nextRestockName(d) + "." : n <= 2 ? " Only " + n + " left!" : " In stock: " + n + ".";
    }

    static String nextRestockName(CityData d) {
        if (d.techRestockDay == Long.MIN_VALUE) return "in a few days";
        return "on " + Calendar.name(d.techRestockDay + 3);
    }

    static boolean take(CityData d, String item) {
        int n = stock(d, item);
        if (n <= 0) return false;
        d.techStock.put(item, n - 1);
        d.setDirty();
        return true;
    }

    /** Every third day a stock delivery comes in (paid wholesale from FireTech's account); on Sundays Ava gets her wages. */
    static void business(ServerLevel sl, CityData d, long day) {
        long rd = Calendar.day(sl);
        int tod = (int) Math.floorMod(sl.getDayTime(), 24000L);
        restock(sl, d, day);
        wages(sl, d, day, rd, tod);
        d.setDirty();
    }

    static void restock(ServerLevel sl, CityData d, long day) {
        long rd = Calendar.day(sl);
        int tod = (int) Math.floorMod(sl.getDayTime(), 24000L);
        if (d.techRestockDay == Long.MIN_VALUE) d.techRestockDay = day;
        if (day - d.techRestockDay >= 3) {
            d.techRestockDay = day;
            int phones = MAX_PHONES - stock(d, "phone"), pcs = MAX_PCS - stock(d, "pc");
            if (Extras.launched(d, day)) d.techStock.put("phone2", Math.max(stock(d, "phone2"), 8));
            d.techStock.put("tv", Math.max(stock(d, "tv"), 4));
            int cost = phones * Phones.PRICE * 6 / 10 + pcs * Computers.PRICE * 6 / 10;
            int funds = d.balance("biz:tech");
            if (cost > funds && cost > 0) {
                double f = funds / (double) cost;
                phones = (int) (phones * f);
                pcs = (int) (pcs * f);
                cost = phones * Phones.PRICE * 6 / 10 + pcs * Computers.PRICE * 6 / 10;
            }
            if (phones + pcs > 0) {
                if (cost > 0) d.pay("biz:tech", CityData.CITY, cost, "SolTech stock delivery", rd, tod, false);
                d.techStock.put("phone", stock(d, "phone") + phones);
                d.techStock.put("pc", stock(d, "pc") + pcs);
                d.news(day, "A SolTech stock delivery came in on the cargo ship at the port: " + phones + " SolPhone" + (phones == 1 ? "" : "s") + " and " + pcs + " Solaris PC" + (pcs == 1 ? "" : "s") + ".");
                for (CityData.Profile p : d.profiles.values()) if (!p.wantDevice.isEmpty() && p.wantDay < day) p.wantDay = day - 1;
            }
            d.setDirty();
        }
    }

    static void wages(ServerLevel sl, CityData d, long day, long rd, int tod) {
        if (Calendar.weekday(day) != 6 || d.techPayDay == day) return;
        d.techPayDay = day;
        int shifts = 3;
        for (CityData.Profile p : d.profiles.values()) {
            if (p.job != Job.CLERK) continue;
            int wage = Math.min(d.balance("biz:tech"), shifts * (d.avaPromoted ? WAGE_MANAGER : WAGE));
            if (wage > 0) {
                d.pay("biz:tech", p.id, wage, "SolTech wages (" + shifts + " shifts)", rd, tod, false);
                p.log(Calendar.day(sl)).note("I got paid " + wage + " coins for my shifts at SolTech");
            }
            break;
        }
    }

    /* ---------------------------------------------------------------- building */

    public static boolean build(ServerLevel sl, CityData d) {
        var fn = sl.getServer().getFunctions().get(new ResourceLocation(FireheartCity.MODID, "tech/build"));
        if (fn.isEmpty()) {
            FireheartCity.LOG.error("SolTech build function fireheartcity:tech/build is missing");
            return false;
        }
        d.techBuilt = true;
        d.setDirty();
        sl.getServer().getFunctions().execute(fn.get(), sl.getServer().createCommandSourceStack().withSuppressedOutput().withPermission(4));
        FireheartCity.LOG.info("SolTech store built (fireheartcity:tech/build)");
        return true;
    }

    static boolean warned;

    public static boolean lotClear(ServerLevel sl) {
        int solid = 0;
        for (int x = 31; x <= 39; x++) for (int z = -7; z <= 5; z++) for (int y = 71; y <= 77; y++) {
            var st = sl.getBlockState(new BlockPos(x, y, z));
            if (st.isAir() || st.canBeReplaced() || st.is(net.minecraft.tags.BlockTags.FLOWERS) || st.is(net.minecraft.tags.BlockTags.SAPLINGS)) continue;
            solid++;
        }
        return solid <= 2;
    }

    public static void ensure(ServerLevel sl, CityData d) {
        if (!d.techBuilt && !sl.players().isEmpty() && sl.isLoaded(CENTER) && sl.getGameTime() > 400) {
            if (lotClear(sl)) build(sl, d);
            else if (!warned) {
                warned = true;
                FireheartCity.LOG.warn("SolTech lot (31..39, -7..5) is not empty - not building automatically");
                for (ServerPlayer pl : sl.players()) pl.sendSystemMessage(Component.literal("§6[SolTech] §eThe empty lot east of the fuel station (x31-39, z-7-5) has something built on it, so SolTech wasn't built there. Clear it, or run §f/city phone build§e to build anyway."));
            }
        }
        if (Computers.isPc(sl, KIOSK) && !"store".equals(d.pcs.get(KIOSK.asLong()))) {
            d.pcs.put(KIOSK.asLong(), "store");
            d.setDirty();
        }
    }

    public static boolean inStore(BlockPos p) {
        return p.getX() >= 31 && p.getX() <= 39 && p.getZ() >= -7 && p.getZ() <= 5 && p.getY() >= 70 && p.getY() <= 77;
    }

    public static Resident clerk(ServerLevel sl, CityData d) {
        for (CityData.Profile p : d.profiles.values()) {
            if (p.job != Job.CLERK) continue;
            Resident r = Phones.entity(sl, p);
            if (r != null && r.activityName().equals("work") && r.blockPosition().closerThan(CENTER, 9)) return r;
        }
        return null;
    }

    /* ---------------------------------------------------------------- players */

    public static void playerBuy(ServerPlayer pl, CityData d, String item, String pay, BlockPos from) {
        ServerLevel sl = pl.serverLevel();
        String pn = pl.getName().getString();
        String k = Bank.playerKey(pn);
        long rd = Calendar.day(sl), day = Calendar.worldDay(sl);
        int tod = (int) Math.floorMod(sl.getDayTime(), 24000L);
        String[] parts = item.split(":", 2);
        String what = parts[0];
        String arg = parts.length > 1 ? parts[1] : "";
        int price = price(what);
        if (price < 0 || what.equals("repair")) return;
        if ((what.equals("phone2") || what.equals("trade")) && !Extras.launched(d, day)) { fail(pl, "The SolPhone 2 isn't out yet!"); return; }
        if (what.equals("trade") && !Phones.playerHasPhone(pl)) { fail(pl, "Bring the SolPhone you want to trade in."); return; }
        if (what.equals("acc") && !Phones.playerHasPhone(pl)) { fail(pl, "You need a SolPhone to put a case on."); return; }
        boolean here = pl.blockPosition().closerThan(CENTER, 14);
        CityData.Profile target = null;
        if (what.equals("gift")) {
            target = d.profiles.get(arg.contains(":") ? arg.substring(0, arg.indexOf(':')) : arg);
            if (target == null || target.ownsPhone) { fail(pl, target == null ? "Pick a resident first." : target.name + " already has a SolPhone!"); return; }
        }
        if (what.equals("case") && !Phones.playerHasPhone(pl)) { fail(pl, "You need a SolPhone to recolour."); return; }
        String stockItem = stockKey(what);
        if (!stockItem.isEmpty() && stock(d, stockItem) <= 0) { fail(pl, "Sold out! New stock arrives " + nextRestockName(d) + "."); return; }
        boolean cash = "cash".equals(pay);
        if (cash) {
            if (!here) { fail(pl, "Cash only at the SolTech counter - pay by bank to order online."); return; }
            if (!Bank.takeCash(pl, price)) { fail(pl, "Not enough gold on you (" + price + " coins)."); return; }
            d.pay(CityData.CITY, "biz:tech", price, "SolTech sale to " + pn, rd, tod, false);
        } else {
            if (Bank.holder(d, pn) == null) { fail(pl, "You need a Solaris Bank account to pay online."); return; }
            if (!d.pay(Bank.sav(k), "biz:tech", price, "SolTech: " + label(what), rd, tod, false)) { fail(pl, "Not enough savings (" + price + " coins)."); return; }
        }
        if (!stockItem.isEmpty()) take(d, stockItem);
        int giftColor = -1;
        if (what.equals("gift") && arg.contains(":")) giftColor = Math.floorMod(Phones.parse(arg.substring(arg.indexOf(':') + 1)), Phones.COLORS.length);
        int color = Math.floorMod(Phones.parse(arg), Phones.COLORS.length);
        ItemStack goods = switch (what) {
            case "pc" -> new ItemStack(FireheartCity.COMPUTER_ITEM.get());
            case "phone" -> PhoneItem.make(color);
            case "phone2" -> PhoneItem.make(color, 2);
            case "tv" -> new ItemStack(FireheartCity.TV_ITEM.get());
            case "tablet" -> new ItemStack(FireheartCity.TABLET.get());
            case "watch" -> new ItemStack(FireheartCity.WATCH.get());
            case "console" -> new ItemStack(FireheartCity.CONSOLE.get());
            case "solbox" -> new ItemStack(FireheartCity.SOLBOX_ITEM.get());
            case "headphones" -> DeviceItem.headphones(color, false);
            case "hpmagma" -> DeviceItem.headphones(0, true);
            default -> ItemStack.EMPTY;
        };
        d.techSales++;
        String msg;
        if (what.equals("trade")) {
            ItemStack old = Extras.phoneStack(pl);
            int keep = PhoneItem.color(old);
            old.shrink(1);
            Post.give(pl, PhoneItem.make(color, 2));
            msg = "Traded in your " + Phones.colorName(keep) + " SolPhone - enjoy your " + Phones.colorName(color) + " SolPhone 2!";
        } else if (what.equals("acc")) {
            Extras.phoneStack(pl).getOrCreateTag().putInt("Case", Math.max(1, Math.floorMod(Phones.parse(arg), Extras.CASES.length)));
            msg = "Your SolPhone now has a " + Extras.CASES[Math.max(1, Math.floorMod(Phones.parse(arg), Extras.CASES.length))].toLowerCase() + " case!";
        } else if (what.equals("case")) {
            ItemStack ph = pl.getMainHandItem().getItem() instanceof PhoneItem ? pl.getMainHandItem() : ItemStack.EMPTY;
            Inventory inv = pl.getInventory();
            for (int i = 0; i < inv.getContainerSize() && ph.isEmpty(); i++) if (inv.getItem(i).getItem() instanceof PhoneItem) ph = inv.getItem(i);
            ph.getOrCreateTag().putInt("Color", color);
            msg = "Your SolPhone is now " + Phones.colorName(color) + "!";
        } else if (what.equals("gift")) {
            Phones.give(sl, d, target, giftColor >= 0 ? giftColor : pickColor(target), " - a gift from " + pn + "!");
            Mind.playerEvent(d, target, pn, day, "{P} bought me a SolPhone as a gift", 5, 8);
            Computers.deliver(sl, d, pn, target.id, "OMG " + pn + "!!! You got me a SolPhone?! THANK YOU. I love it ♥");
            Resident r = Phones.entity(sl, target);
            if (r != null) {
                r.gesture(Resident.G_CHEER, 60);
                r.say("A present? For me?! A SolPhone!", 80);
            }
            d.news(day, pn + " gave " + target.name + " a SolPhone as a gift.");
            msg = target.name + " got the phone - check your messages!";
        } else if (here) {
            Post.give(pl, goods);
            msg = "Enjoy your new " + label(what) + (what.equals("phone") ? " in " + Phones.colorName(color) : "") + "!";
        } else {
            Post.Letter l = Post.send(d, "tech", k, "Hi " + pn + "!\n\nThanks for ordering from SolTech online. Your " + label(what) + " is in this parcel.\n\nEnjoy!\n- The SolTech team", "parcel", day);
            l.gift = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(goods.getItem()).toString();
            l.giftCount = 1;
            if (goods.hasTag()) l.giftNbt = goods.getTag().toString();
            msg = "Order placed! Pip will bring your parcel on the next post round.";
        }
        sl.playSound(null, pl.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.4f);
        Resident c = clerk(sl, d);
        if (c != null && here) {
            c.getLookControl().setLookAt(pl, 30, 30);
            c.gesture(Resident.G_GIVE, 40);
            c.sayTo(c.pick("Thanks for shopping at SolTech, " + pn + "!", "Great choice! Enjoy it, " + pn + ".", "Ooh, that's my favourite one!"), 70);
        }
        d.event(day, "shopping", pn + " bought " + (what.equals("gift") ? "a SolPhone for " + target.name : "a " + label(what)) + " at SolTech", CENTER);
        Phones.send(pl, "#toast|SolTech|" + msg);
        PcNet.send(pl, Computers.data(pl, from));
        d.setDirty();
    }

    static String label(String what) {
        return switch (what) {
            case "case" -> "phone colour swap";
            case "gift" -> "SolPhone gift";
            default -> deviceName(what);
        };
    }

    static void fail(ServerPlayer pl, String why) {
        Phones.send(pl, "#toast|SolTech|" + why);
        pl.displayClientMessage(Component.literal("§c" + why), true);
    }

    /* ---------------------------------------------------------------- residents */

    public static void daily(ServerLevel sl, CityData d, long day) {
        RandomSource r = sl.random;
        business(sl, d, day);
        Extras.daily(sl, d, day);
        for (CityData.Profile p : d.profiles.values()) {
            if (!p.wantDevice.isEmpty() && day - p.wantDay >= 2) orderOnline(sl, d, p, day);
            if (p.wantDevice.isEmpty() && p.coins + Bank.savings(d, p.id) >= TV + 30 && !p.hasTv && r.nextInt(100) < 6) want(d, p, "tv", day);
            else if (p.wantDevice.isEmpty() && p.coins + Bank.savings(d, p.id) >= HEADPHONES + 15 && !p.headphones && r.nextInt(100) < (p.trait == Trait.DREAMY || p.trait == Trait.CHEERFUL ? 10 : 4)) want(d, p, "headphones", day);
            if (!p.wantDevice.isEmpty() || p.ownsPhone) continue;
            int money = p.coins + Bank.savings(d, p.id);
            if (money < Phones.PRICE + 15) continue;
            int chance = switch (p.trait) {
                case CURIOUS, TALKATIVE, ADVENTUROUS -> 40;
                case CHEERFUL, FRIENDLY -> 30;
                case GRUMPY -> 8;
                default -> 20;
            };
            for (String f : d.friendsOf(p.id)) if (d.profiles.get(f) != null && d.profiles.get(f).ownsPhone) chance += 6;
            CityData.Profile q = d.profiles.get(p.partner);
            if (q != null && q.ownsPhone) chance += 20;
            if (r.nextInt(100) < chance) want(d, p, "phone", day);
        }
    }

    public static void want(CityData d, CityData.Profile p, String what, long day) {
        p.wantDevice = what;
        p.wantDay = day;
        p.mind.remember(p, day, 0, "want", what.equals("repair") ? "I need to get my SolPhone screen fixed at SolTech" : "I really want a " + deviceName(what) + " from SolTech", KEY, 1, 3);
        d.setDirty();
    }

    static int pay(ServerLevel sl, CityData d, CityData.Profile p, int price, String memo) {
        long rd = Calendar.day(sl);
        int tod = (int) Math.floorMod(sl.getDayTime(), 24000L);
        if (p.coins < price) {
            int need = Math.min(Bank.savings(d, p.id), price - p.coins);
            if (need > 0) d.pay(Bank.sav(p.id), p.id, need, "Withdrawal for " + memo, rd, tod, false);
        }
        if (p.coins < price) return -1;
        d.pay(p.id, "biz:tech", price, memo + " at SolTech", rd, tod, false);
        return price;
    }

    static int pickColor(CityData.Profile p) {
        return switch (p.trait) {
            case GRUMPY -> 0;
            case SHY -> 1;
            case ADVENTUROUS -> 2;
            case DREAMY -> 6;
            case CHEERFUL -> 5;
            case CURIOUS -> 3;
            case FRIENDLY -> 4;
            default -> 7;
        };
    }

    /** A resident who came to the store to buy something. Returns true when handled. */
    public static boolean arrive(Resident r, CityData.Profile p) {
        ServerLevel sl = (ServerLevel) r.level();
        CityData d = r.data();
        if (p.wantDevice.isEmpty()) return false;
        String what = p.wantDevice;
        int price = price(what);
        if (price < 0) { p.wantDevice = ""; return false; }
        Resident c = clerk(sl, d);
        if ((what.equals("phone2") && !Extras.launched(d, Calendar.worldDay(sl))) || stock(d, stockKey(what)) <= 0) {
            if (c != null && c != r) c.sayTo("Sorry " + p.name + ", we're sold out! New stock comes " + nextRestockName(d) + ".", 80);
            r.say(r.pick("Sold out?! Aww... I'll come back after the delivery.", "No SolPhones left? Typical."), 60);
            p.wantDay = Calendar.worldDay(sl);
            r.replan();
            return true;
        }
        if (pay(sl, d, p, price, deviceName(what)) < 0) {
            r.say(r.pick("Oh no... I can't afford it yet.", "Hmm, a bit too pricey for me this week."), 60);
            p.wantDevice = "";
            return true;
        }
        p.wantDevice = "";
        take(d, stockKey(what));
        d.techSales++;
        long day = Calendar.worldDay(sl);
        if (c != null && c != r) {
            c.getLookControl().setLookAt(r, 30, 30);
            c.gesture(Resident.G_GIVE, 40);
            c.sayTo(switch (what) {
                case "pc" -> "One Solaris PC! Pip will deliver it to your place tomorrow, " + p.name + ".";
                case "repair" -> "Good as new, " + p.name + "! Try not to drop it again ☺";
                case "phone2" -> "Your SolPhone 2, " + p.name + "! Enjoy the Dynamic Island.";
                case "tv" -> "One SolTube TV - we'll set it up at your place today.";
                case "headphones" -> "SolBeats! Great choice, " + p.name + ".";
                default -> "Here you go, " + p.name + " - one " + Phones.colorName(pickColor(p)) + " SolPhone!";
            }, 80);
        }
        r.gesture(Resident.G_CHEER, 50);
        if (what.equals("pc")) {
            p.pcDeliverDay = day + 1;
            p.log(Calendar.day(sl)).note("I bought a Solaris PC at SolTech - it gets delivered tomorrow");
            r.say(r.pick("My very own PC! I can't wait for the delivery.", "Finally! Snake high score, here I come."), 70);
        } else if (what.equals("repair")) {
            p.phoneBroken = false;
            p.log(Calendar.day(sl)).note("Ava fixed my cracked SolPhone screen at SolTech");
            r.say(r.pick("Phew, my phone lives!", "Thank you! I missed SolFeed so much."), 70);
        } else if (what.equals("tv")) {
            p.hasTv = true;
            p.log(Calendar.day(sl)).note("I bought a SolTube TV for my place");
            r.say(r.pick("Movie nights at my place from now on!", "A TV! My couch is going to love this."), 70);
        } else if (what.equals("headphones")) {
            p.headphones = true;
            p.log(Calendar.day(sl)).note("I bought SolBeats headphones");
            r.say(r.pick("Now I can listen to music everywhere ♪", "Ooh, the bass!"), 70);
        } else if (what.equals("phone2")) {
            p.phoneModel = 2;
            p.log(Calendar.day(sl)).note("I got the new SolPhone 2 on launch day");
            p.mind.remember(p, day, (int) r.timeOfDay(), "shopping", "I got the SolPhone 2", KEY, 3, 6);
            Phones.queue(p, "Got the SolPhone 2!! The Dynamic Island is SO cool ★ #SolPhone2");
            r.say(r.pick("It's even better in person!", "SolPhone 2! Worth the queue!"), 70);
        } else {
            Phones.give(sl, d, p, pickColor(p), " at SolTech");
            r.say(r.pick("It's perfect! Look at the colour!", "My first SolPhone! How do I work this...", "Ooh, shiny!"), 70);
            r.usePhone(1, 120, "setting up their new SolPhone", () -> Phones.browse(r, p, false));
        }
        sl.playSound(null, r.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.6f, 1.4f);
        r.replan();
        return true;
    }

    static void orderOnline(ServerLevel sl, CityData d, CityData.Profile p, long day) {
        String what = p.wantDevice;
        int price = price(what);
        if (price < 0 || what.equals("phone2") && !Extras.launched(d, day) || stock(d, stockKey(what)) <= 0) return;
        if (pay(sl, d, p, price, deviceName(what) + " (online)") < 0) {
            p.wantDevice = "";
            return;
        }
        p.wantDevice = "";
        take(d, stockKey(what));
        d.techSales++;
        switch (what) {
            case "pc" -> p.pcDeliverDay = day + 1;
            case "repair" -> { p.phoneBroken = false; p.log(Calendar.day(sl)).note("I mailed my cracked SolPhone to SolTech and it came back fixed"); }
            case "phone2" -> p.phoneModel = 2;
            case "tv" -> p.hasTv = true;
            case "headphones" -> p.headphones = true;
            default -> Phones.give(sl, d, p, pickColor(p), " - it came in the post");
        }
    }
}
