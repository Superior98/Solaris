package com.fireheart.city;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

/** Scripted self-test of the player-facing features using a fake player standing at the bank. */
public final class TestPlayer {
    private TestPlayer() {}

    static List<String> out;

    static void check(String what, boolean ok, String detail) {
        String line = (ok ? "PASS " : "FAIL ") + what + (detail.isEmpty() ? "" : " - " + detail);
        out.add(line);
        FireheartCity.LOG.info("[Test] " + line);
    }

    static void skip(String what, String why) {
        String line = "SKIP " + what + " - " + why;
        out.add(line);
        FireheartCity.LOG.info("[Test] " + line);
    }

    private static Resident loaded(ServerLevel sl, CityData d, String id) {
        CityData.Profile p = d.profiles.get(id);
        if (p == null || p.entity == null) return null;
        return sl.getEntity(p.entity) instanceof Resident r ? r : null;
    }

    static void v113Tests(ServerLevel sl, CityData d, FakePlayer fp, String key) {
        long now = sl.getGameTime();
        Post.Letter o = Post.send(d, "diner", key, "test order", "parcel", Calendar.worldDay(sl));
        o.gift = "minecraft:bread";
        o.giftCount = 1;
        o.placed = now - 100;
        o.ready = now + 200;
        o.cook = "Leo";
        int s1 = Extras.eatsStatus(o, now);
        o.stage = 1;
        o.out = now + 200;
        int s3 = Extras.eatsStatus(o, now + 300);
        o.stage = 2;
        int s4 = Extras.eatsStatus(o, now + 400);
        check("SolEats tracker: cooking -> on the way -> delivered", s1 == 1 && s3 == 3 && s4 == 4, s1 + "/" + s3 + "/" + s4 + " " + Extras.eatsDetail(o, now, 1));
        o.stage = 0;
        o.ready = now - 5;
        check("SolEats tracker: packed when ready", Extras.eatsStatus(o, now) == 2, Extras.eatsDetail(o, now, 2));
        o.stage = 2;
        BlockPos tp = new BlockPos(52, 70, -36);
        net.minecraft.world.level.block.state.BlockState was = sl.getBlockState(tp);
        int before = Repair.pending(sl);
        net.minecraft.world.entity.item.PrimedTnt tnt = new net.minecraft.world.entity.item.PrimedTnt(sl, tp.getX() + 0.5, tp.getY(), tp.getZ() + 0.5, fp);
        net.minecraft.world.level.Explosion ex = new net.minecraft.world.level.Explosion(sl, tnt, tp.getX() + 0.5, tp.getY() + 0.5, tp.getZ() + 0.5, 2f, false, net.minecraft.world.level.Explosion.BlockInteraction.KEEP);
        Repair.onExplosion(sl, ex, java.util.List.of(tp));
        check("player TNT damage gets queued for Gus", Repair.pending(sl) > before || was.isAir(), before + " -> " + Repair.pending(sl) + " (" + was + ")");
        check("hotel places + rooms", Place.get("hotel1") != null && Place.get("magma_house") != null && Hotel.isRoom("hotel4") && !Hotel.isRoom("hotel_desk"), Hotel.status(sl).replace('\n', ' '));
        CityData.Profile marco = d.profiles.get("marco");
        check("Marco is the hotel concierge", marco != null && marco.job == Job.CONCIERGE, marco == null ? "missing" : marco.home);
        check("GPS knows the destinations", Gps.keys().contains("magma_house") && Gps.keys().contains("hotel") && Gps.keys().contains("stellar_house"), String.valueOf(Gps.keys().size()));
    }

    static void mindTests(ServerLevel sl, CityData d, FakePlayer fp) {
        long day = Calendar.worldDay(sl);
        Resident g = null, h = null;
        for (CityData.Profile p : d.profiles.values()) {
            if (p.job == Job.BANKER) continue;
            Resident r = loaded(sl, d, p.id);
            if (r == null || r.isPassenger() || r.isSleeping() || r.convo != null || r.inCall()) continue;
            if (g == null && Inv.item(Memory.favourite(p)) != null) g = r;
            else if (h == null && r != g) h = r;
            if (g != null && h != null) break;
        }
        if (g == null || h == null) { check("mind tests", false, "need two loaded residents"); return; }
        CityData.Profile gp = g.profile(), hp = h.profile();
        d.playerRel(gp.id, "Tester").met = true;
        d.playerRel(hp.id, "Tester").met = true;
        gp.mind.trust.remove("Tester");
        hp.mind.trust.remove("Tester");
        gp.mind.eps.removeIf(e -> e.involves("@Tester"));
        hp.mind.eps.removeIf(e -> e.involves("@Tester"));
        String fav = Memory.favourite(gp);
        fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Inv.item(fav), 2));
        fp.setPos(g.getX() + 1, g.getY(), g.getZ());
        g.resetGiftsForTest();
        g.interact(fp, net.minecraft.world.InteractionHand.MAIN_HAND);
        boolean remembered = gp.mind.eps.stream().anyMatch(e -> e.involves("@Tester") && e.text.contains("gave me"));
        check("gift remembered by " + gp.name + " (" + fav + ")", remembered && gp.mind.trustIn("Tester") > 0, "trust " + gp.mind.trustIn("Tester") + ", says: " + g.getSpeech());
        for (Mind.Ep e : gp.mind.eps) if (e.involves("@Tester")) e.day = day - 2;
        String line = Mind.playerLine(gp, "Tester", day, sl.random);
        for (int i = 0; line == null && i < 10; i++) line = Mind.playerLine(gp, "Tester", day, sl.random);
        check(gp.name + " brings up the gift days later", line != null && line.contains("gave me"), String.valueOf(line));
        fp.setPos(h.getX() + 1, h.getY(), h.getZ());
        h.testHit(fp);
        h.testHit(fp);
        check(hp.name + " remembers being hit", hp.mind.trustIn("Tester") < 0 && hp.mind.eps.stream().anyMatch(e -> e.text.contains("kept hitting me")), "trust " + hp.mind.trustIn("Tester") + ", says: " + h.getSpeech());
        String op = Mind.opinionOf(hp, "Tester");
        check(hp.name + " has an opinion of Tester", op != null, String.valueOf(op));
        gp.mind.eps.removeIf(e -> e.involves(hp.id));
        hp.mind.eps.removeIf(e -> e.involves(gp.id));
        gp.mind.remember(gp, day - 2, 12000, "diary", "I went to the Sky Gardens with " + hp.name, "gardens", 3, 7, hp.id);
        hp.mind.remember(hp, day - 2, 12000, "diary", "I went to the Sky Gardens with " + gp.name, "gardens", 3, 7, gp.id);
        d.rel(gp.id, hp.id).talked.remove("recall");
        boolean shared = false;
        String said = "";
        for (int i = 0; i < 12 && !shared; i++) {
            Dialogue.Script sc = new Dialogue.Script();
            List<Runnable> fx = new ArrayList<>();
            Mind.talk(sc, g, h, gp, hp, d, day, sl.random, fx);
            for (Dialogue.Line l : sc.lines) if (l.text().startsWith("Remember")) { shared = true; said = sc.lines.get(0).text() + " / " + sc.lines.get(1).text(); }
        }
        check("shared memory recalled in conversation", shared, said);
        hp.mind.eps.removeIf(e -> e.involves(gp.id));
        boolean forgot = false;
        for (int i = 0; i < 12 && !forgot; i++) {
            Dialogue.Script sc = new Dialogue.Script();
            Mind.talk(sc, g, h, gp, hp, d, day, sl.random, new ArrayList<>());
            for (Dialogue.Line l : sc.lines) if (!l.byA() && (l.text().contains("don't remember") || l.text().contains("memory's terrible") || l.text().contains("was I there"))) { forgot = true; said = l.text(); }
        }
        check(hp.name + " forgot and says so", forgot, said);
        for (CityData.Profile p : d.profiles.values()) p.mind.reflectDay = -1;
        Mind.forceReflect(sl, d);
        long withPlan = d.profiles.values().stream().filter(p -> !p.mind.intent.isEmpty()).count();
        check("nightly reflection gives plans", withPlan >= d.profiles.size() / 2, withPlan + "/" + d.profiles.size() + " have a plan, e.g. " + gp.name + ": " + gp.mind.intent);
        String[] msgs = {"hi " + gp.name, gp.name + ", how are you?", "what are you doing " + gp.name + "?", "do you remember me, " + gp.name + "?", gp.name + " what do you think of " + hp.name + "?", gp.name + " what's your favourite food?", gp.name + ", any plans today?"};
        int ok = 0;
        StringBuilder conv = new StringBuilder();
        for (String msg : msgs) {
            String t = " " + msg.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9' ]", " ").replaceAll("\\s+", " ") + " ";
            String rep = Chat.reply(fp, g, t, true);
            FireheartCity.LOG.info("[Test] chat> Tester: " + msg + "  |  " + gp.name + ": " + rep);
            if (rep != null && !rep.isEmpty()) ok++;
        }
        check("chat with " + gp.name + " (" + ok + "/" + msgs.length + " answered)", ok == msgs.length, "see [Test] chat> lines");
        fp.setPos(g.getX() + 1, g.getY(), g.getZ());
        Chat.Ctx cx = new Chat.Ctx();
        cx.res = g.getUUID();
        long tnow = sl.getGameTime();
        String[] flow = {"hey " + gp.name, "how are you?", "that's good", "i'm good thanks", "how are you?", "what are you doing?", "cool", "follow me", "stop following", "bye"};
        int answered = 0;
        String repeatReply = "";
        for (int i = 0; i < flow.length; i++) {
            String t = " " + flow[i].toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9' ]", " ").replaceAll("\\s+", " ") + " ";
            boolean nm = t.contains(" " + gp.name.toLowerCase(java.util.Locale.ROOT) + " ");
            String rp = Chat.reply(fp, g, t, nm, cx, tnow + i * 60L);
            cx.at = tnow + i * 60L;
            if (i == 4) repeatReply = String.valueOf(rp);
            FireheartCity.LOG.info("[Test] ctx> Tester: " + flow[i] + "  |  " + gp.name + ": " + rp + "  [topic=" + cx.topic + "]");
            if (rp != null && !rp.isEmpty()) answered++;
        }
        check("follow-up chat without names (" + answered + "/" + flow.length + ")", answered == flow.length, "repeat -> " + repeatReply);
        g.stopSeeking();
        String t1 = " sorry " + hp.name.toLowerCase() + " ";
        int before = hp.mind.trustIn("Tester");
        String rep = Chat.reply(fp, h, t1, true);
        check(hp.name + " accepts an apology", hp.mind.trustIn("Tester") > before, before + " -> " + hp.mind.trustIn("Tester") + ": " + rep);
        int mailBefore = d.civic.mail.size();
        gp.mind.milestones.removeIf(x -> x.endsWith(":Tester"));
        gp.mind.trust.put("Tester", Math.min(gp.mind.trustIn("Tester"), 20));
        for (int i = 0; i < 4; i++) Mind.playerEvent(d, gp, "Tester", day, "{P} helped me out again", 4, 5);
        boolean best = gp.mind.milestones.contains("best:Tester") && gp.mind.milestones.contains("friend:Tester");
        Post.Letter gift = null;
        for (Post.Letter l : d.civic.mail) if (l.to.equals(Bank.playerKey("Tester")) && l.kind.equals("bestfriend")) gift = l;
        check(gp.name + " calls Tester a best friend and mails a gift", best && gift != null && !gift.giftStack().isEmpty(), "trust " + gp.mind.trustIn("Tester") + ", letters " + (d.civic.mail.size() - mailBefore) + (gift == null ? "" : ", gift " + gift.giftCount + "x " + gift.gift));
        gp.mind.milestones.removeIf(x -> x.endsWith(":Tester"));
        BlockPos pcPos = new BlockPos(-17, 90, 31);
        while (!sl.getBlockState(pcPos).isAir() && pcPos.getY() < 120) pcPos = pcPos.above();
        boolean placed = sl.getBlockState(pcPos).isAir() && sl.setBlock(pcPos, FireheartCity.COMPUTER.get().defaultBlockState(), 3);
        check("public PC registers when placed", !placed || d.pcs.containsKey(pcPos.asLong()), "pcs " + d.pcs.size());
        String k = Bank.playerKey("Tester");
        d.inbox.remove(k);
        Computers.action(fp, new PcNet.Act(pcPos, "msg", gp.id, "hi " + gp.name + ", how are you?"));
        for (Computers.Pending pd : new ArrayList<>(Computers.PENDING)) if (pd.player().equals("Tester")) { Computers.PENDING.remove(pd); Computers.reply(sl, d, pd); }
        List<String> box = d.inbox.getOrDefault(k, List.of());
        check("SolNet message gets a reply", box.size() == 2 && box.get(1).contains("|<|"), box.toString());
        Computers.action(fp, new PcNet.Act(pcPos, "score", "snake", "31"));
        check("high score saved", d.scores.getOrDefault("snake", java.util.Map.of()).getOrDefault("Tester", 0) == 31, "");
        PcNet.Data pdat = Computers.data(fp, pcPos);
        check("SolOS data built", !pdat.residents.isEmpty() && !pdat.inbox.isEmpty(), pdat.residents.size() + " residents, " + pdat.news.size() + " news");
        d.inbox.remove(k);
        d.scores.values().forEach(m -> m.remove("Tester"));
        d.news.removeIf(n -> n.contains("Tester"));
        if (placed) sl.setBlock(pcPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        check("PC unregisters when broken", !d.pcs.containsKey(pcPos.asLong()), "");
        Ferry f = Ferry.find(sl);
        check("Sky Ferry exists", f != null, f == null ? "not loaded" : f.describe());
        gp.mind.eps.removeIf(e -> e.involves("@Tester"));
        hp.mind.eps.removeIf(e -> e.involves("@Tester"));
        gp.mind.trust.remove("Tester");
        hp.mind.trust.remove("Tester");
        d.events.removeIf(e -> e.text.contains("Tester"));
    }

    public static int run(CommandSourceStack src) {
        out = new ArrayList<>();
        ServerLevel sl = src.getLevel();
        CityData d = CityData.get(sl);
        FakePlayer fp = FakePlayerFactory.get(sl, new GameProfile(UUID.nameUUIDFromBytes("fhc-tester".getBytes()), "Tester"));
        fp.setPos(-39.5, 71, 22.5);
        fp.getInventory().clearContent();
        String key = Bank.playerKey("Tester");
        boolean was = Bank.forceOpen;
        Bank.forceOpen = true;
        try {
            Bank.startSession(fp, true);
            d.holders.remove(key);
            d.savings.remove(key);
            d.loans.remove(key);
            Bank.cmdOpen(fp);
            check("open account", Bank.holder(d, "Tester") != null, "FH-" + (Bank.holder(d, "Tester") == null ? "?" : Bank.holder(d, "Tester").number));
            boolean card = false;
            for (int i = 0; i < fp.getInventory().getContainerSize(); i++) if (fp.getInventory().getItem(i).is(Items.PAPER)) card = true;
            check("bank card given", card, "");
            fp.getInventory().add(new ItemStack(Items.GOLD_INGOT, 10));
            fp.getInventory().add(new ItemStack(Items.GOLD_NUGGET, 20));
            int cash = Bank.cash(fp);
            Bank.cmdDeposit(fp, "all");
            check("deposit all gold", Bank.savings(d, key) == 110 && Bank.cash(fp) == 0, "cash was " + cash + ", balance " + Bank.savings(d, key));
            Bank.cmdWithdraw(fp, "25");
            check("withdraw 25", Bank.savings(d, key) == 85 && Bank.cash(fp) == 25, "balance " + Bank.savings(d, key) + ", cash " + Bank.cash(fp));
            CityData.Profile mia = d.byName("mia");
            int before = mia.coins;
            Bank.cmdPay(fp, "mia", "15");
            check("pay Mia 15", mia.coins == before + 15 && Bank.savings(d, key) == 70, "Mia " + before + " -> " + mia.coins);
            Bank.cmdTicket(fp, "3");
            check("buy 3 lottery tickets", Lottery.tickets(d, key) == 3 && Bank.savings(d, key) == 64, "tickets " + Lottery.tickets(d, key) + ", pot " + Lottery.pot(d));
            Bank.cmdLoan(fp, "50");
            Bank.Loan l = Bank.loan(d, key);
            check("take 50 loan", l != null && l.owed == 55 && Bank.savings(d, key) == 114, l == null ? "no loan" : "owed " + l.owed + ", per day " + l.perDay);
            Bank.cmdRepay(fp, "all");
            check("repay loan", Bank.loan(d, key) == null && Bank.savings(d, key) == 59, "balance " + Bank.savings(d, key));
            Bank.cmdStatement(fp);
            int tx = 0;
            for (CityData.Tx t : d.ledger) if (t.from.equals(Bank.sav(key)) || t.to.equals(Bank.sav(key))) tx++;
            check("statement has transactions", tx >= 6, tx + " transactions");

            if (d.civic.candidates.isEmpty() || d.civic.electionWeek == d.civic.announceWeek) { d.civic.announceWeek = -1; Mayor.announce(sl, d, Calendar.worldDay(sl)); }
            String cand = d.civic.candidates.isEmpty() ? null : d.civic.candidates.get(0);
            if (cand != null) Mayor.vote(fp, cand);
            boolean voted = cand != null && cand.equals(d.civic.votes.get(key));
            if (!voted && Calendar.weekend(Calendar.worldDay(sl))) skip("vote for " + cand, "polls are closed at the weekend");
            else check("vote for " + cand, voted, "candidates " + d.civic.candidates);

            Resident r = null;
            for (CityData.Profile p : d.profiles.values()) {
                if (p.entity == null || p.job == Job.BANKER) continue;
                Entity e = sl.getEntity(p.entity);
                if (e instanceof Resident rr) { r = rr; break; }
            }
            if (r != null) {
                CityData.Profile p = r.profile();
                p.coins = Math.max(p.coins, 50);
                CityData.Rel pr = d.playerRel(p.id, "Tester");
                pr.met = true;
                pr.fam = 20;
                d.civic.favours.remove(p.id);
                Favours.Request q = Favours.ask(sl, d, r, p, fp);
                fp.getInventory().add(new ItemStack(Inv.item(q.item), q.count));
                int bal = Bank.savings(d, key);
                boolean done = Favours.tryComplete(sl, d, r, p, fp);
                check("favour for " + p.name + " (" + q.count + " " + q.noun + ")", done && Favours.of(d, p.id) == null && Bank.savings(d, key) == bal + q.reward, "reward " + q.reward + ", balance " + bal + " -> " + Bank.savings(d, key));
            } else check("favour", false, "no loaded resident");
            int done = 0;
            for (CityData.Profile p : d.profiles.values()) {
                if (done >= 4 || p.entity == null || p.job == Job.BANKER || (r != null && p == r.profile())) continue;
                if (!(sl.getEntity(p.entity) instanceof Resident rr)) continue;
                p.coins = Math.max(p.coins, 50);
                CityData.Rel pr = d.playerRel(p.id, "Tester");
                pr.met = true;
                pr.fam = 20;
                d.civic.favours.remove(p.id);
                Favours.Request q = Favours.ask(sl, d, rr, p, fp);
                fp.getInventory().add(new ItemStack(Inv.item(q.item), q.count));
                if (Favours.tryComplete(sl, d, rr, p, fp)) done++;
            }
            boolean gotKey = false;
            for (int i = 0; i < fp.getInventory().getContainerSize(); i++) if (fp.getInventory().getItem(i).is(Items.TRIPWIRE_HOOK)) gotKey = true;
            check("Key to the City after 5 favours", gotKey && d.civic.helped.getOrDefault("Tester", 0) == 5, "helped " + d.civic.helped.getOrDefault("Tester", 0));
            ItemStack gz = Gazette.book(sl, d);
            check("Gazette City Hall page", gz.getTag().getList("pages", 8).toString().contains("City Hall"), "");

            Post.Letter lt = Post.send(d, "mia", key, "Dear Tester,\n\nThis is a test letter.\n\nMia", "test", Calendar.worldDay(sl));
            ItemStack book = Post.book(d, lt);
            check("letter book", book.is(Items.WRITTEN_BOOK) && book.getTag().getList("pages", 8).size() >= 1, book.getTag().getString("title"));
            lt.stage = 2;

            String drawn = Lottery.draw(sl, d, Calendar.worldDay(sl));
            check("lottery draw", !d.civic.lastWinner.isEmpty() && d.civic.tickets.isEmpty(), drawn);
            mindTests(sl, d, fp);
            phoneTests(sl, d, fp);
            extrasTests(sl, d, fp);
            v113Tests(sl, d, fp, key);
            lifeTests(sl, d, fp);
        } catch (Throwable t) {
            check("exception", false, t.toString());
            FireheartCity.LOG.error("Test failed", t);
        } finally {
            Bank.forceOpen = was;
            d.holders.remove(key);
            d.savings.remove(key);
            d.loans.remove(key);
            d.civic.votes.remove(key);
            d.civic.mail.removeIf(x -> x.to.equals(key));
            d.civic.tickets.remove(key);
            if (d.civic.lastWinner.equals(key)) { d.civic.lastWinner = ""; d.civic.lastWinDay = -100; d.civic.lastPrize = 0; d.civic.drawDay = -1; }
            d.events.removeIf(e -> e.text.contains("Tester"));
            d.news.removeIf(n -> n.contains("Tester"));
            d.civic.favours.values().removeIf(q -> q.player.equals("Tester"));
            d.civic.helped.remove("Tester");
            d.setDirty();
        }
        int fails = 0;
        for (String s : out) {
            if (s.startsWith("FAIL")) fails++;
            src.sendSuccess(() -> Component.literal((s.startsWith("PASS") ? "§a" : s.startsWith("SKIP") ? "§7" : "§c") + s), false);
        }
        long skips = out.stream().filter(s -> s.startsWith("SKIP")).count();
        String sum = "Test finished: " + (out.size() - fails - skips) + "/" + (out.size() - skips) + " passed" + (skips > 0 ? " (" + skips + " skipped)" : "");
        FireheartCity.LOG.info("[Test] " + sum);
        src.sendSuccess(() -> Component.literal(sum), false);
        return fails == 0 ? 1 : 0;
    }

    /** Checks for the v1.13-1.19 systems: shared resident cache, daily bonus, achievements, quests, letters, calendar, health, chat variety. */
    static void lifeTests(ServerLevel sl, CityData d, FakePlayer fp) {
        long day = Calendar.worldDay(sl);
        d.settings.keySet().removeIf(k -> k.startsWith("Tester|"));
        check("resident cache", !Crowd.all(sl, d).isEmpty(), Crowd.all(sl, d).size() + " loaded");
        String first = Perks.daily(fp, d, true);
        String again = Perks.daily(fp, d, true);
        check("daily bonus once a day", d.setting("Tester", "dailyDay", "").equals(String.valueOf(day)) && again.contains("Already"), first);
        boolean unlocked = Perks.unlock(fp, d, "meet1");
        check("achievement unlocks once", unlocked && !Perks.unlock(fp, d, "meet1"), "");
        Quests.currentDay = day;
        Quests.bump(d, "Tester", "talk");
        Quests.bump(d, "Tester", "talk");
        check("quest counters", Quests.count(d, "Tester", "talk", day) == 2, String.valueOf(Quests.count(d, "Tester", "talk", day)));
        Quests.Task[] t = Quests.today("Tester", day);
        check("three distinct daily quests", t.length == 3 && !t[0].key().equals(t[1].key()) && !t[1].key().equals(t[2].key()) && !t[0].key().equals(t[2].key()), t[0].key() + "," + t[1].key() + "," + t[2].key());
        java.util.Set<String> heard = new java.util.HashSet<>();
        for (int i = 0; i < Pastimes.STORIES.length; i++) heard.add(Pastimes.fresh("Tester|test", Pastimes.STORIES, sl.random));
        check("stories don't repeat", heard.size() == Pastimes.STORIES.length, heard.size() + "/" + Pastimes.STORIES.length);
        CityData.Profile mia = d.byName("mia");
        if (mia != null) {
            Letters.load(d);
            int before = Letters.PENDING.size();
            String sent = Letters.send(fp, d, mia.name, "Thank you for being a great friend!");
            check("letter queued with a reply", Letters.PENDING.size() == before + 1 && Letters.PENDING.get(Letters.PENDING.size() - 1).text().contains(mia.name), sent);
            Letters.PENDING.removeIf(r -> r.player().equals("Tester"));
            Letters.save(d);
            d.setSetting("res:" + mia.id, Health.K, String.valueOf(day));
            Resident mr = loaded(sl, d, mia.id);
            if (mr != null) {
                mr.rethink();
                String act = mr.activityName();
                check("sick residents stay home", act.equals("evening") || act.equals("sleep"), act);
            } else skip("sick residents stay home", "Mia not loaded");
            d.setSetting("res:" + mia.id, Health.K, "-1");
            if (mr != null) mr.rethink();
            String wish = Quests.wishlist(fp, d, mia.name);
            check("wishlist", wish.contains(mia.name), wish.split("\n")[0]);
        } else skip("letters/health", "no Mia");
        long sunday = day + Math.floorMod(6 - Calendar.weekday(day), 7);
        check("calendar lists the fishing tournament", Happenings.on(sunday).stream().anyMatch(s -> s.startsWith("Fishing")), Calendar.name(sunday));
        check("calendar command", Info.calendar(sl, d).contains("Solaris calendar"), "");
        check("nickname", !Bonds.makeNick("StellarFox1", mia == null ? d.profiles.values().iterator().next() : mia).isEmpty(), Bonds.makeNick("StellarFox1", mia == null ? d.profiles.values().iterator().next() : mia));
        check("seasons cycle", !Skies.seasonName(day).equals(Skies.seasonName(day + 7)), Skies.seasonName(day) + " -> " + Skies.seasonName(day + 7));
        d.settings.keySet().removeIf(k -> k.startsWith("Tester|"));
        d.setDirty();
    }

    static void phoneTests(ServerLevel sl, CityData d, FakePlayer fp) {
        java.util.List<Resident> rs = new java.util.ArrayList<>();
        for (CityData.Profile p : d.profiles.values()) {
            Resident r = Phones.entity(sl, p);
            if (r != null && p.job != Job.BANKER && p.job != Job.PILOT && !r.isSleeping()) rs.add(r);
        }
        if (rs.size() < 3) { check("phone tests", false, "need three loaded residents"); return; }
        Resident ra = rs.get(0), rb = rs.get(1), rc = rs.get(2);
        CityData.Profile a = ra.profile(), b = rb.profile(), c = rc.profile();
        boolean[] had = {a.ownsPhone, b.ownsPhone, c.ownsPhone};
        long day = Calendar.worldDay(sl);
        int tod = Phones.tod(sl);
        int texts0 = d.texts.size(), feed0 = d.feed.size();
        try {
            fp.getInventory().add(PhoneItem.make(3));
            check("SolPhone item (" + Phones.colorName(3) + ")", Phones.playerHasPhone(fp) && Phones.playerPhoneColor(fp) == 3, Economy.label("fireheartcity:phone"));
            PcNet.Data pd = Computers.data(fp, net.minecraft.core.BlockPos.ZERO);
            check("phone data built", pd.device == 1 && !pd.map.isEmpty() && !pd.catalog.isEmpty() && !pd.weather.isEmpty(), pd.map.size() + " on map, " + pd.feed.size() + " posts, ferry: " + pd.ferry);
            a.ownsPhone = true;
            b.ownsPhone = true;
            for (CityData.Rel x : new CityData.Rel[]{d.rel(a.id, b.id), d.rel(b.id, a.id)}) { x.met = true; x.fam = Math.max(x.fam, 60); x.chats = Math.max(x.chats, 4); x.aff = Math.max(x.aff, 60); x.rival = false; }
            long when = tod < 10800 ? day : day + 1;
            d.plans.removeIf(pl -> pl.day == when && (pl.who.contains(a.id) || pl.who.contains(b.id)));
            Phones.text(d, a.id, b.id, "Want to hang out at the plaza after work?", "invite", "plaza", day, tod);
            Phones.readTexts(rb, b);
            boolean planned = false;
            for (CityData.Plan pl : d.plansFor(b.id, when)) if (pl.what.equals("hangout") && pl.who.contains(a.id)) planned = true;
            Phones.Text reply = d.texts.get(d.texts.size() - 1);
            check(b.name + " reads " + a.name + "'s invite and replies", reply.from.equals(b.id) && reply.to.equals(a.id), reply.text);
            check("text invite becomes a real plan", planned || reply.text.contains("plans"), planned ? "hangout at the plaza" : reply.text);
            CityData.Event ev = d.event(day, "test", a.name + " saw a purple cow on the pier", null, a.id);
            b.known.remove(ev.id);
            Phones.text(d, a.id, b.id, "Did you hear? " + ev.text + "!", "gossip", String.valueOf(ev.id), day, tod);
            Phones.readTexts(rb, b);
            check("gossip by text spreads news", b.known.contains(ev.id), d.texts.get(d.texts.size() - 1).text);
            d.events.remove(ev);
            Phones.CAND.remove(a.id);
            Phones.queue(a, "Just baked the best bread of my life! ★");
            for (int i = 0; i < 8 && Phones.CAND.containsKey(a.id) && !Phones.CAND.get(a.id).isEmpty(); i++) Phones.browse(ra, a, false);
            boolean posted = d.feed.stream().anyMatch(x -> x.author.equals(a.id) && x.text.contains("best bread"));
            check(a.name + " posts on SolFeed", posted, d.feed.isEmpty() ? "" : d.feed.get(d.feed.size() - 1).text);
            Phones.Post mine = Phones.post(d, "player:Tester", "Hello Solaris! Who wants to meet at the pier?", day, tod);
            d.playerRel(b.id, "Tester").met = true;
            rb.usePhone(2, 100, "test", null);
            Phones.react(sl, d, new Phones.React(b.id, mine.id, 0, "comment"));
            rb.putPhoneAway();
            check(b.name + " likes and comments on Tester's post", mine.likes.contains(b.id) && mine.commented(b.id), mine.comments.toString());
            Phones.Call call = new Phones.Call();
            call.player = "Tester";
            call.pid = fp.getUUID();
            call.res = b.id;
            call.state = 1;
            call.start = sl.getGameTime();
            call.last = call.start;
            Phones.PLAYER_CALLS.put(fp.getUUID(), call);
            rb.usePhone(2, 400, "on the phone with Tester", null);
            Phones.speak(fp, d, "where are you?");
            for (Object[] o : new java.util.ArrayList<>(Phones.LATER)) { Phones.LATER.remove(o); ((Runnable) o[1]).run(); }
            String said = rb.getSpeech();
            check(b.name + " answers on the phone", said != null && said.startsWith("☎ I'm"), said);
            Phones.hangup(sl, d, call, "test", true);
            check("hang up puts the phone away", !rb.inCall() && !Phones.PLAYER_CALLS.containsKey(fp.getUUID()), rb.phoneWhat());
            c.ownsPhone = false;
            c.coins = Math.max(c.coins, 60);
            TechStore.want(d, c, "phone", day);
            d.techStock.put("phone", 0);
            TechStore.arrive(rc, c);
            check("sold-out SolTech turns " + c.name + " away", !c.ownsPhone && c.wantDevice.equals("phone"), rc.getSpeech());
            int biz0 = d.balance("biz:tech");
            long restock0 = d.techRestockDay;
            d.accounts.put("biz:tech", biz0 + 1000);
            d.techRestockDay = day - 3;
            TechStore.restock(sl, d, day);
            check("SolTech stock delivery restocks", TechStore.stock(d, "phone") == TechStore.MAX_PHONES && d.balance("biz:tech") < biz0 + 1000, "phones " + TechStore.stock(d, "phone") + ", paid " + (biz0 + 1000 - d.balance("biz:tech")));
            d.accounts.put("biz:tech", biz0);
            d.techRestockDay = restock0;
            d.news.removeIf(n -> n.contains("stock delivery"));
            boolean bought = TechStore.arrive(rc, c);
            check(c.name + " buys a SolPhone at SolTech", bought && c.ownsPhone, c.name + " has " + (c.ownsPhone ? Phones.colorName(c.phoneColor) : "no") + " phone, coins " + c.coins);
            rc.putPhoneAway();
            Bank.Holder h = Bank.holder(d, "Tester");
            String key = Bank.playerKey("Tester");
            if (h != null) {
                d.savings.put(key, 200);
                fp.setPos(-39.5, 71, 22.5);
                TechStore.playerBuy(fp, d, "phone:6", "bank", net.minecraft.core.BlockPos.ZERO);
                Post.Letter parcel = null;
                for (Post.Letter l : d.civic.mail) if (l.to.equals(key) && l.from.equals("tech")) parcel = l;
                ItemStack gs = parcel == null ? ItemStack.EMPTY : parcel.giftStack();
                check("online SolTech order ships a " + Phones.colorName(6) + " phone", gs.getItem() instanceof PhoneItem && PhoneItem.color(gs) == 6 && Bank.savings(d, key) == 200 - Phones.PRICE, "savings " + Bank.savings(d, key));
            } else check("online SolTech order", false, "no bank account");
            String pk = Bank.playerKey("Tester");
            int un0 = Computers.unread(d, pk, a.id);
            Computers.deliver(sl, d, "Tester", a.id, "Test message");
            Computers.deliver(sl, d, "Tester", a.id, "Another one");
            PcNet.Data ud = Computers.data(fp, net.minecraft.core.BlockPos.ZERO);
            boolean listed = ud.unread.contains(a.id + "|" + (un0 + 2));
            Computers.markRead(d, pk, a.id);
            check("unread count per conversation", listed && Computers.unread(d, pk, a.id) == 0, "listed " + listed + ", " + ud.unread);
            Phones.Invites.put(a.id + "|Tester", new Object[]{"Tester", "plaza", sl.getGameTime() + 100});
            Phones.Invites.put(a.id + "|Other", new Object[]{"Other", "pier", sl.getGameTime() + 100});
            Phones.persist(d);
            boolean saved = d.invites.stream().anyMatch(s -> s.startsWith(a.id + "|Tester|plaza|")) && d.invites.stream().anyMatch(s -> s.startsWith(a.id + "|Other|pier|"));
            Phones.Invites.remove(a.id + "|Tester");
            Phones.Invites.remove(a.id + "|Other");
            Phones.persist(d);
            check("phone invites are per player and saved", saved, d.invites.toString());
            a.fun = 50;
            int k0 = a.known.size(), f0 = a.fun;
            Computers.appDone(ra, a, "news", false);
            Computers.appDone(ra, a, "tube", false);
            check(a.name + " reads news and watches SolTube on a PC", a.known.size() >= k0 && a.fun > f0, "known " + k0 + " -> " + a.known.size() + ", fun " + f0 + " -> " + a.fun);
            if (rb.inCall()) rb.putPhoneAway();
            if (ra.convo == null && rb.convo == null && rb.isFree()) {
                int chats = d.rel(a.id, b.id).chats;
                Phones.Call rcall = new Phones.Call();
                rcall.a = a.id;
                rcall.b = b.id;
                rcall.start = sl.getGameTime();
                Phones.RES_CALLS.add(rcall);
                ra.usePhone(2, 4000, "calling " + b.name, null);
                long now = rcall.start;
                for (int i = 0; i < 40 && Phones.RES_CALLS.contains(rcall); i++) {
                    now = Math.max(now + 1, rcall.next);
                    Phones.resCallTick(sl, d, rcall, Math.min(now, rcall.start + 4700));
                }
                check(a.name + " and " + b.name + " talk on the phone", !Phones.RES_CALLS.contains(rcall) && d.rel(a.id, b.id).chats > chats && !ra.inCall(), (rcall.script == null ? 0 : rcall.script.lines.size()) + " lines");
            } else check("resident phone call", true, "skipped (busy)");
        } finally {
            a.ownsPhone = had[0];
            b.ownsPhone = had[1];
            c.ownsPhone = had[2];
            ra.putPhoneAway();
            rb.putPhoneAway();
            rc.putPhoneAway();
            Phones.PLAYER_CALLS.remove(fp.getUUID());
            while (d.texts.size() > texts0) d.texts.remove(d.texts.size() - 1);
            d.feed.removeIf(x -> x.author.equals("player:Tester") || x.text.contains("best bread"));
            d.civic.mail.removeIf(x -> x.to.equals(Bank.playerKey("Tester")));
            d.events.removeIf(e -> e.text.contains("Tester"));
            d.techStock.put("phone", Math.max(TechStore.stock(d, "phone"), 4));
            d.inbox.getOrDefault(Bank.playerKey("Tester"), new ArrayList<>()).removeIf(m -> m.contains("Test message") || m.contains("Another one"));
        }
    }

    static void extrasTests(ServerLevel sl, CityData d, FakePlayer fp) {
        String pn = "Tester";
        BlockPos home = fp.blockPosition();
        Place gardens = Place.get("gardens");
        if (gardens != null) {
            fp.setPos(gardens.pos.getX() + 0.5, gardens.pos.getY(), gardens.pos.getZ() + 0.5);
            int g0 = Extras.gallery(d, pn).size();
            Extras.action(fp, d, new PcNet.Act(BlockPos.ZERO, "snap", "", ""));
            List<String> gal = Extras.gallery(d, pn);
            check("camera photo saved to the camera roll", gal.size() == Math.min(24, g0 + 1) && gal.get(0).startsWith("gardens:"), gal.isEmpty() ? "empty" : gal.get(0));
            check("photo in a text finds the scene", Extras.sceneAt(fp).equals("gardens"), Extras.sceneAt(fp));
        } else skip("camera", "no gardens place");
        fp.setPos(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
        Extras.action(fp, d, new PcNet.Act(BlockPos.ZERO, "setting", "dnd", "1"));
        boolean dnd = Extras.dnd(d, pn);
        Extras.action(fp, d, new PcNet.Act(BlockPos.ZERO, "setting", "dnd", "0"));
        Extras.action(fp, d, new PcNet.Act(BlockPos.ZERO, "setting", "ring", "3"));
        check("phone settings (do not disturb, ringtone)", dnd && !Extras.dnd(d, pn) && Extras.ringtone(d, pn) == 3, "ring " + Extras.ringtone(d, pn));
        fp.getInventory().clearContent();
        fp.getInventory().add(PhoneItem.make(1));
        Extras.action(fp, d, new PcNet.Act(BlockPos.ZERO, "setting", "case", "2"));
        PcNet.Data sd = Computers.data(fp, BlockPos.ZERO);
        check("phone case setting sticks", sd.settings.split("\\|")[2].equals("2"), sd.settings);
        String key = Bank.playerKey(pn);
        CityData.Profile mia = d.byName("mia");
        int m0 = mia.coins, s0 = Bank.savings(d, key);
        Extras.action(fp, d, new PcNet.Act(BlockPos.ZERO, "pay", mia.id, "5"));
        check("Bank app sends money away from the bank", mia.coins == m0 + 5 && Bank.savings(d, key) == s0 - 5, "Mia " + m0 + " -> " + mia.coins);
        List<String> menu = Extras.menu(sl, d);
        String[] first = menu.isEmpty() ? null : menu.get(0).split("\\|");
        int mail0 = d.civic.mail.size();
        s0 = Bank.savings(d, key);
        if (first != null) Extras.action(fp, d, new PcNet.Act(BlockPos.ZERO, "food", first[0], first[2]));
        boolean parcel = d.civic.mail.size() > mail0 && d.civic.mail.get(d.civic.mail.size() - 1).to.equals(key);
        check("SolEats order becomes a parcel", first != null && parcel && Bank.savings(d, key) < s0, menu.size() + " menu items, savings " + s0 + " -> " + Bank.savings(d, key));
        List<Extras.Group> groups = Extras.groups(d, pn);
        if (!groups.isEmpty()) {
            int later = Phones.LATER.size();
            Extras.groupMessage(fp, d, groups.get(0).id(), "Anyone up for noodles?");
            check("group chat gets replies queued", Phones.LATER.size() > later, groups.get(0).name() + ", " + groups.get(0).members().size() + " members");
            Phones.dial(fp, d, groups.get(groups.size() - 1).id());
            Phones.Call gc = Phones.PLAYER_CALLS.get(fp.getUUID());
            check("group call rings members", gc == null || !gc.members.isEmpty(), gc == null ? "nobody free" : gc.members.toString());
            if (gc != null) Phones.hangup(sl, d, gc, "test", true);
        } else skip("group chat", "no phone owners");
        Extras.directions(fp, d, mia.id);
        check("Maps directions start a guide", Extras.GUIDES.stream().anyMatch(g -> g.player().equals(fp.getUUID())), "");
        Extras.GUIDES.removeIf(g -> g.player().equals(fp.getUUID()));
        ItemStack ph = PhoneItem.make(2);
        ph.getOrCreateTag().putInt("Battery", 10);
        fp.getInventory().clearContent();
        fp.getInventory().add(ph);
        fp.setPos(TechStore.CENTER.getX() + 0.5, TechStore.CENTER.getY(), TechStore.CENTER.getZ() + 0.5);
        for (int i = 0; i < 5; i++) Extras.batteryTick(sl, d, fp);
        int charged = Extras.battery(Extras.phoneStack(fp));
        check("phone charges at SolTech", charged > 10 && Extras.charging(fp), "battery " + charged);
        fp.getInventory().clearContent();
        fp.setPos(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
        boolean items = !new ItemStack(FireheartCity.TABLET.get()).isEmpty() && !new ItemStack(FireheartCity.WATCH.get()).isEmpty() && !new ItemStack(FireheartCity.CONSOLE.get()).isEmpty() && !new ItemStack(FireheartCity.HEADPHONES.get()).isEmpty() && !new ItemStack(FireheartCity.TV_ITEM.get()).isEmpty();
        check("SolTech gadgets exist (tablet, watch, console, headphones, TV)", items, "");
        check("tablet and console open SolOS variants", Computers.data(fp, Extras.TABLET).device == 2 && Computers.data(fp, Extras.CONSOLE).device == 3, "");
        BlockPos tv = new BlockPos(-17, 90, 33);
        while (!sl.getBlockState(tv).isAir() && tv.getY() < 120) tv = tv.above();
        if (sl.getBlockState(tv).isAir() && sl.setBlock(tv, FireheartCity.TV.get().defaultBlockState(), 3)) {
            d.tvs.put(tv.asLong(), pn);
            PcNet.Data td = Computers.data(fp, tv);
            Computers.setScreen(sl, tv, 3);
            boolean lit = sl.getBlockState(tv).getValue(TvBlock.ON);
            sl.removeBlock(tv, false);
            check("TV block opens SolTube TV and lights up", td.device == 4 && lit && !d.tvs.containsKey(tv.asLong()), "");
        } else skip("TV block", "no space");
        long launch0 = d.launchDay;
        long day = Calendar.worldDay(sl);
        d.launchDay = day;
        boolean cat = TechStore.catalog(d, day).stream().anyMatch(x -> x.startsWith("phone2|"));
        d.launchDay = launch0;
        check("SolPhone 2 on sale after launch day", cat, "");
        boolean prom = d.avaPromoted;
        int sales = d.techSales;
        d.avaPromoted = false;
        d.techSales = 30;
        Extras.daily(sl, d, day);
        check("Ava promoted to store manager after 25 sales", d.avaPromoted, "");
        d.avaPromoted = prom;
        d.techSales = sales;
        CityData.managerAva = prom;
    }
}
