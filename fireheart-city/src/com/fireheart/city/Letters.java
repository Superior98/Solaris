package com.fireheart.city;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.BlockEvent;

/** Letters to residents (with replies), horoscopes, personal stats, lucky finds and lightning reactions. */
public final class Letters {
    private Letters() {}

    /* ------------------------------------------------------------ Letters */

    record Reply(String player, String resident, String text, long due) {}

    static final List<Reply> PENDING = new ArrayList<>();

    public static String send(ServerPlayer pl, CityData d, String name, String message) {
        CityData.Profile p = Quests.byName(d, name);
        if (p == null) return "§cNobody called " + name + " lives in Solaris.";
        String pn = pl.getName().getString();
        ServerLevel sl = pl.serverLevel();
        long day = Calendar.worldDay(sl);
        String k = "letters:" + day;
        int sent = (int) Perks.parse(d.setting(pn, k, "0"));
        if (sent >= 5) return "§7The post office has taken enough of your letters for today.";
        d.setSetting(pn, k, String.valueOf(sent + 1));
        int total = (int) Perks.parse(d.setting(pn, "lettersSent", "0")) + 1;
        d.setSetting(pn, "lettersSent", String.valueOf(total));
        CityData.Rel pr = d.playerRel(p.id, pn);
        String low = " " + message.toLowerCase(java.util.Locale.ROOT) + " ";
        boolean kind = Intents.any(low, " thank", " love", " miss ", " friend", " great ", " best ", " happy ", " sorry ", " proud ", " beautiful ", " amazing ");
        boolean rude = Intents.any(low, " hate ", " stupid ", " ugly ", " idiot ", " dumb ", " loser ");
        pr.met = true;
        pr.aff = Math.max(-100, Math.min(100, pr.aff + (rude ? -5 : kind ? 4 : 2)));
        Mind.playerEvent(d, p, pn, day, rude ? "{P} sent me a nasty letter" : "{P} sent me a letter", rude ? -3 : kind ? 3 : 2, rude ? 5 : 3);
        p.log(day).note(rude ? "I got a rude letter from " + pn : "I got a letter from " + pn);
        String reply = replyFor(p, pn, low, kind, rude, pr.aff, sl.getRandom());
        PENDING.add(new Reply(pn, p.id, reply, sl.getGameTime() + 1200 + sl.getRandom().nextInt(2400)));
        if (total >= 5) Perks.unlock(pl, d, "penpal");
        d.setDirty();
        sl.playSound(null, pl.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 1.0f);
        return "§6[Solaris Post] §fYour letter to §b" + p.name + "§f is on its way. §7They'll probably write back.";
    }

    static String replyFor(CityData.Profile p, String pn, String low, boolean kind, boolean rude, int aff, RandomSource rnd) {
        if (rude) return Lines.pick(rnd, "I don't know what I did to deserve that letter, " + pn + ". Please don't write to me like that again.", "Wow. Okay. That hurt.");
        String sign = "\n§7- " + p.name;
        String body;
        if (Intents.any(low, " sorry ", " apolog")) body = Lines.pick(rnd, "Thank you for the apology. It means a lot. We're good.", "Apology accepted! Let's put it behind us.");
        else if (Intents.any(low, " love ") && aff >= 60) body = Lines.pick(rnd, "Your letter made me blush! You're really special to me too.", "I read your letter three times. Thank you, " + pn + ".");
        else if (Intents.any(low, " miss ")) body = "I miss you too! Come find me - I'm usually around " + p.job.work().label + ".";
        else if (Intents.any(low, " thank")) body = Lines.pick(rnd, "You're welcome! That's what friends are for.", "No, thank YOU. Seriously.");
        else if (Intents.any(low, " how are you ", " how's life ", " hows life ")) body = "I'm doing " + (p.mood() > 60 ? "great" : p.mood() > 35 ? "alright" : "not so great, honestly") + ". Work at " + p.job.work().label + " keeps me busy. How about you?";
        else if (kind) body = Lines.pick(rnd, "Your letter made my whole day!", "I pinned your letter to my fridge. Don't tell anyone.");
        else body = Lines.pick(rnd, "Thanks for writing! It's nice to get real post for once.", "Got your letter! Life in Solaris is good. Come visit " + p.job.work().label + " sometime.", "I don't get many letters - this was a lovely surprise.");
        return body + " " + Lines.pick(rnd, "P.S. Try the " + Economy.label(Memory.favourite(p)) + " - trust me.", "P.S. Say hi next time you see me!", "") + sign;
    }

    static void deliver(ServerLevel sl, CityData d) {
        long now = sl.getGameTime();
        Iterator<Reply> it = PENDING.iterator();
        while (it.hasNext()) {
            Reply r = it.next();
            if (now < r.due()) continue;
            ServerPlayer pl = sl.getServer().getPlayerList().getPlayerByName(r.player());
            if (pl == null) {
                if (now - r.due() > 72000) it.remove();
                continue;
            }
            it.remove();
            CityData.Profile p = d.profiles.get(r.resident());
            if (p == null) continue;
            Perks.say(pl, "§6[Solaris Post] §fA letter from §b" + p.name + "§f:\n§e\"" + r.text() + "§e\"");
            pl.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 1.2f);
            ItemStack paper = new ItemStack(Items.PAPER);
            paper.setHoverName(net.minecraft.network.chat.Component.literal("§eLetter from " + p.name));
            if (!pl.getInventory().add(paper)) pl.drop(paper, false);
        }
    }

    /* ------------------------------------------------------------ Horoscope */

    static final String[] SIGNS = {"the Creeper", "the Fox", "the Parrot", "the Sky Ferry", "the Anvil", "the Lantern", "the Beacon", "the Axolotl"};
    static final String[] OMENS = {
            "A surprise is waiting where the bread is warm.", "Someone is thinking about you more than you know.", "Coins flow to those who help others today.",
            "Avoid dark corners after sunset. Seriously.", "A new friendship is closer than it looks.", "The sky has a gift for you tonight.",
            "Your luck doubles near water.", "Say yes to the next invitation.", "An old face will bring good news.", "Today, generosity is its own reward."
    };

    public static String horoscope(ServerPlayer pl, CityData d) {
        String pn = pl.getName().getString();
        long day = Calendar.worldDay(pl.serverLevel());
        java.util.Random r = new java.util.Random(pn.hashCode() * 7919L + day);
        List<CityData.Profile> all = new ArrayList<>(d.profiles.values());
        String friend = all.isEmpty() ? "a stranger" : all.get(r.nextInt(all.size())).name;
        String[] places = Place.CITY_HANGOUTS;
        Place lucky = Place.get(places[r.nextInt(places.length)]);
        int stars = 1 + r.nextInt(5);
        return "§d✦ Horoscope for " + pn + " §7(" + Calendar.stamp(day) + ")" +
                "\n§7Sign of the day: §f" + SIGNS[Math.floorMod(pn.hashCode(), SIGNS.length)] +
                "\n§7Luck: §e" + "★".repeat(stars) + "§8" + "★".repeat(5 - stars) +
                "\n§f" + OMENS[r.nextInt(OMENS.length)] +
                "\n§7Lucky resident: §b" + friend + " §7· Lucky place: §f" + (lucky == null ? "the plaza" : lucky.label) +
                "\n§7Lucky number: §f" + (1 + r.nextInt(99));
    }

    /* ------------------------------------------------------------ Stats */

    public static String stats(ServerPlayer pl, CityData d) {
        String pn = pl.getName().getString();
        int ach = 0;
        for (Perks.Ach a : Perks.ACHS) if (d.setting(pn, "ach:" + a.id(), "0").equals("1")) ach++;
        int met = 0;
        for (CityData.Profile p : d.profiles.values()) if (d.playerNames(p.id).contains(pn) && d.playerRel(p.id, pn).met) met++;
        return "§6§l" + pn + "'s Solaris stats" +
                "\n§7Reputation: §e" + Quests.rank(Quests.repScore(d, pn)) + " §7· Residents met: §f" + met + "/" + d.profiles.size() +
                "\n§7Achievements: §f" + ach + "/" + Perks.ACHS.length + " §7· Visit streak: §f" + d.setting(pn, "streak", "0") +
                "\n§7Deliveries: §f" + d.setting(pn, "couriers", "0") + " §7· Treasures: §f" + d.setting(pn, "treasures", "0") + " §7· Fish: §f" + d.setting(pn, "fish", "0") +
                "\n§7Letters sent: §f" + d.setting(pn, "lettersSent", "0") + " §7· Lucky finds: §f" + d.setting(pn, "lucky", "0") +
                "\n§7Bank savings: §f" + Bank.savings(d, Bank.playerKey(pn)) + " coins" + (d.setting(pn, "birthday", "").isEmpty() ? "" : " §7· Birthday: day §f" + d.setting(pn, "birthday", ""));
    }

    /* ------------------------------------------------------------ Lucky finds */

    static final BlockPos CITY = new BlockPos(-20, 71, 30);

    public static void onBreak(BlockEvent.BreakEvent e) {
        try {
            if (!(e.getPlayer() instanceof ServerPlayer pl) || !(e.getLevel() instanceof ServerLevel sl)) return;
            BlockState st = e.getState();
            boolean plant = st.is(net.minecraft.tags.BlockTags.FLOWERS) || st.is(net.minecraft.world.level.block.Blocks.GRASS) || st.is(net.minecraft.world.level.block.Blocks.TALL_GRASS) || st.is(net.minecraft.world.level.block.Blocks.FERN);
            if (!plant || !e.getPos().closerThan(CITY, 220) || sl.getRandom().nextInt(40) != 0) return;
            CityData d = CityData.get(sl);
            String pn = pl.getName().getString();
            long day = Calendar.worldDay(sl);
            int today = (int) Perks.parse(d.setting(pn, "luckyDay:" + day, "0"));
            if (today >= 3) return;
            d.setSetting(pn, "luckyDay:" + day, String.valueOf(today + 1));
            d.setSetting(pn, "lucky", String.valueOf(Perks.parse(d.setting(pn, "lucky", "0")) + 1));
            RandomSource r = sl.getRandom();
            float roll = r.nextFloat();
            ItemStack find = roll < 0.05f ? new ItemStack(Items.EMERALD) : roll < 0.4f ? new ItemStack(Items.GOLD_NUGGET, 1 + r.nextInt(3)) : roll < 0.7f ? new ItemStack(Items.COOKIE) : new ItemStack(Items.SWEET_BERRIES, 2);
            Vec3 at = Vec3.atCenterOf(e.getPos());
            net.minecraft.world.entity.item.ItemEntity ie = new net.minecraft.world.entity.item.ItemEntity(sl, at.x, at.y, at.z, find);
            sl.addFreshEntity(ie);
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y, at.z, 8, 0.3, 0.3, 0.3, 0);
            pl.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.4f);
            Perks.say(pl, "§a✿ Lucky find! §7Something was hidden in the " + st.getBlock().getName().getString().toLowerCase(java.util.Locale.ROOT) + ".");
            Perks.unlock(pl, d, "lucky");
        } catch (Throwable t) {
            FireheartCity.LOG.error("Lucky find failed", t);
        }
    }

    /* ------------------------------------------------------------ Lightning */

    public static void onJoin(EntityJoinLevelEvent e) {
        try {
            if (!(e.getEntity() instanceof LightningBolt bolt) || !(e.getLevel() instanceof ServerLevel sl) || e.loadedFromDisk()) return;
            Vec3 at = bolt.position();
            int n = 0;
            for (Resident r : sl.getEntitiesOfClass(Resident.class, bolt.getBoundingBox().inflate(40), x -> x.profile() != null && !x.isSleeping() && !x.inShuttle())) {
                if (r.profile().job == Job.POLICE) continue;
                r.getLookControl().setLookAt(at.x, at.y + 4, at.z);
                r.gesture(Resident.G_SURPRISED, 30);
                if (n++ < 2 && sl.getRandom().nextFloat() < 0.6f) r.say(r.pick("WHOA!", "That was close!", "My hair's standing up!", "Everyone inside, NOW!", "Did that just hit " + (r.distanceToSqr(at) < 100 ? "right next to me?!" : "the city?!")), 40);
            }
        } catch (Throwable t) {
            FireheartCity.LOG.error("Lightning reaction failed", t);
        }
    }

    public static void tick(ServerLevel sl, CityData d) {
        if (sl.getGameTime() % 40 == 29 && !PENDING.isEmpty()) deliver(sl, d);
    }
}
