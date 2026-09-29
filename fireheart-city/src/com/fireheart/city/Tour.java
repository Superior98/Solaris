package com.fireheart.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The Solaris Sky Tour: the player boards Nova's holo-drone and flies over the whole city. At every stop the drone
 * circles the building, it lights up with a beacon and an outline, and Nova explains what it is and how to use it.
 * A few stops ask the player to try something (open the SolPhone, wave at a resident). It ends with a quiz and fireworks.
 */
public final class Tour {
    private Tour() {}

    record Stop(String title, double x, double y, double z, double sx, double sz, double h, String[] lines, String task) {}

    static final Stop[] STOPS = {
            new Stop("Welcome to Solaris", -20, 72, 30, 12, 12, 8, new String[]{
                    "Hi, I'm Nova, your Sky Guide! Welcome aboard my holo-drone.",
                    "This is Solaris Plaza - the heart of the city. Everyone you see down there is a resident with a job, a home, friends and a memory.",
                    "Talk to them in normal chat: say hi, ask about their day, gossip, compliment them - they remember everything."}, ""),
            new Stop("Ember Heights", 25, 80, 25, 10, 10, 22, new String[]{
                    "Ember Heights! Most residents live in these apartments. The lift takes them between floors.",
                    "Ellie runs the front desk and checks everyone in. Ask her for a room key - she's... friendly."}, ""),
            new Stop("SolTech", 35, 72, -1, 8, 8, 8, new String[]{
                    "SolTech sells phones, PCs, TVs, consoles and SolBeats headphones - even the Magma Edition.",
                    "Your SolPhone has messages, SolEats, SolFeed, the camera, maps and games. Try it now: press your phone key!"}, "phone"),
            new Stop("Solaris City Bank", -40, 72, 22, 8, 8, 8, new String[]{
                    "The Bank keeps your coins safe and pays interest. Hugo can give loans - pay them back on time!",
                    "There's an ATM outside for quick deposits and withdrawals."}, ""),
            new Stop("Food Street", 19, 72, 53, 16, 6, 7, new String[]{
                    "The Diesel Diner, Create Supply Co. and Green Leaf Market. Residents come here to eat and shop.",
                    "Hungry? Order from SolEats on your phone and a courier brings it to you, fresh and steaming."}, ""),
            new Stop("Post Office & Library", 25, 72, -34, 14, 8, 8, new String[]{
                    "Pip delivers letters and parcels from the Post Office to the mailbox outside your home.",
                    "Next door, Nell runs the Solaris Library. Quiet, please."}, ""),
            new Stop("Police & Fire", 67, 72, -38, 18, 10, 12, new String[]{
                    "Solaris PD - Dex, Kira and Bruno fight monsters with martial arts, tasers and sidearms. Big threats? They call ALL units... and sometimes unleash an Ultimate.",
                    "Next door the Fire Dept - Hank and Sofia put out fires, even on rooftops. And if a creeper blows something up, Gus the repairman rebuilds it block by block."}, ""),
            new Stop("Lab & Cinema", 68, 74, -17, 18, 8, 10, new String[]{
                    "The Research Lab, and the Solaris Cinema - with WATERMeDIA you can play real videos on the big screen: /city cinema play <link>."}, ""),
            new Stop("Stellar House", 82, 88, 14, 17, 8, 14, new String[]{
                    "StellarFox1's futuristic house. Sliding doors, neon streaks, an infinity pool...",
                    "Rumour says there's something underneath it. Only its owners know how to get down there."}, ""),
            new Stop("The Beach", 80, 72, 42, 26, 16, 6, new String[]{
                    "The new beach! Gus built the Magma Beach Bar for magmagamer9 - grab a stool.",
                    "Up on the hill is magmagamer9's hotel. Residents like to visit both."}, ""),
            new Stop("Marina & Firework Machine", 10, 70, 100, 20, 30, 10, new String[]{
                    "The marina and the old pier. Out on the water: the Firework Machine. Hit the big button for a show - or wait for Wednesday and Saturday nights.",
                    "The Skyport launches Sky Ferries up to Neon Heights. Hold on - we're going up!"}, ""),
            new Stop("Neon Heights", -4, 185, 265, 40, 40, 16, new String[]{
                    "Neon Heights, the floating district! Noodle bar, arcade, pods, gardens and the Observatory.",
                    "The Sky Organ plays real songs - on the organ or in the Hall of Lights. Try skydiving off the edge, too!"}, ""),
            new Stop("Back to the Plaza", -20, 72, 30, 12, 12, 8, new String[]{
                    "That's Solaris! Make friends, find love, earn coins, and keep an eye on the news in SolFeed.",
                    "One last thing - a quick quiz. Get them right for a reward!"}, "quiz")};

    static final class State {
        int stop, t, waitTask;
        UUID seat;
        Vec3 from, home;
        float homeYaw, homePitch;
        ServerBossEvent bar;
        boolean taskDone;
        int quiz = -1, correct;
    }

    static final Map<UUID, State> ACTIVE = new HashMap<>();
    static final int TRAVEL = 120, ORBIT_PER_LINE = 150;

    public static void onJoin(net.minecraftforge.event.entity.EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !e.getEntity().getPersistentData().getBoolean("fhcTourSeat")) return;
        for (State s : ACTIVE.values()) if (e.getEntity().getUUID().equals(s.seat)) return;
        if (e.loadedFromDisk()) e.setCanceled(true);
    }

    public static boolean touring(ServerPlayer pl) {
        return ACTIVE.containsKey(pl.getUUID());
    }

    public static void offer(ServerPlayer sp) {
        MutableComponent m = Component.literal("§b✈ §fNew to Solaris? ");
        m.append(Component.literal("[ Take Nova's Sky Tour ]").withStyle(Style.EMPTY.withColor(0x7FDBFF).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sol tutorial"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("A flying guided tour of the whole city (~5 min). Sneak to leave any time.")))));
        m.append(Component.literal(" §8(or /sol tutorial any time)"));
        sp.sendSystemMessage(m);
    }

    public static String start(ServerPlayer pl) {
        if (ACTIVE.containsKey(pl.getUUID())) return "§7You're already on the tour!";
        ServerLevel sl = pl.serverLevel();
        State s = new State();
        s.home = pl.position();
        s.homeYaw = pl.getYRot();
        s.homePitch = pl.getXRot();
        if (pl.isPassenger()) pl.stopRiding();
        ArmorStand seat = EntityType.ARMOR_STAND.create(sl);
        if (seat == null) return "§cCouldn't start the tour.";
        seat.moveTo(pl.getX(), pl.getY(), pl.getZ(), pl.getYRot(), 0);
        seat.setInvisible(true);
        seat.setNoGravity(true);
        seat.setInvulnerable(true);
        seat.setSilent(true);
        seat.getPersistentData().putBoolean("fhcTourSeat", true);
        sl.addFreshEntity(seat);
        pl.startRiding(seat, true);
        s.seat = seat.getUUID();
        s.from = seat.position();
        s.bar = new ServerBossEvent(Component.literal("✈ Solaris Sky Tour"), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_12);
        s.bar.addPlayer(pl);
        s.bar.setProgress(0);
        ACTIVE.put(pl.getUUID(), s);
        sl.playSound(null, pl.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1f, 1.4f);
        PcNet.send(pl, new PcNet.Msg("#tour|start"));
        return "§b✈ Boarding Nova's holo-drone... §7(sneak to leave the tour)";
    }

    public static void stop(ServerPlayer pl, boolean finished) {
        State s = ACTIVE.remove(pl.getUUID());
        if (s == null) return;
        ServerLevel sl = pl.serverLevel();
        pl.stopRiding();
        var seat = sl.getEntity(s.seat);
        if (seat != null) seat.discard();
        s.bar.removeAllPlayers();
        pl.teleportTo(sl, s.home.x, s.home.y, s.home.z, s.homeYaw, s.homePitch);
        pl.fallDistance = 0;
        PcNet.send(pl, new PcNet.Msg("#tour|end"));
        if (finished) {
            CityData d = CityData.get(sl);
            d.setSetting(pl.getName().getString(), "toured", "1");
            d.setDirty();
        } else pl.displayClientMessage(Component.literal("§7Tour cancelled. §b/sol tutorial §7to fly again."), true);
    }

    static Vec3 orbitPoint(Stop st, double ang) {
        double r = Math.max(st.sx(), st.sz()) + 14;
        return new Vec3(st.x() + Math.cos(ang) * r, st.y() + st.h() + 12, st.z() + Math.sin(ang) * r);
    }

    static double startAngle(Stop st, Vec3 from) {
        return Math.atan2(from.z - st.z(), from.x - st.x());
    }

    public static void tick(ServerLevel sl) {
        if (ACTIVE.isEmpty()) return;
        for (var e : new ArrayList<>(ACTIVE.entrySet())) {
            ServerPlayer pl = sl.getServer().getPlayerList().getPlayer(e.getKey());
            State s = e.getValue();
            var seat = sl.getEntity(s.seat);
            if (pl == null || seat == null) {
                ACTIVE.remove(e.getKey());
                if (seat != null) seat.discard();
                if (pl != null) s.bar.removePlayer(pl);
                continue;
            }
            if (pl.serverLevel() != sl) continue;
            if (pl.getVehicle() != seat) {
                if (pl.isShiftKeyDown() || s.t > 5) { stop(pl, false); continue; }
                pl.startRiding(seat, true);
            }
            step(sl, pl, s, seat);
        }
    }

    static void step(ServerLevel sl, ServerPlayer pl, State s, net.minecraft.world.entity.Entity seat) {
        if (s.stop >= STOPS.length) { stop(pl, true); return; }
        Stop st = STOPS[s.stop];
        s.t++;
        double a0 = startAngle(st, s.from);
        Vec3 arrive = orbitPoint(st, a0);
        int talk = st.lines().length * ORBIT_PER_LINE;
        Vec3 at;
        if (s.t <= TRAVEL) {
            double k = s.t / (double) TRAVEL;
            double e = k * k * (3 - 2 * k);
            Vec3 lin = s.from.lerp(arrive, e);
            double lift = Math.sin(k * Math.PI) * Math.min(40, 8 + s.from.distanceTo(arrive) * 0.15);
            at = lin.add(0, lift, 0);
            if (s.t == TRAVEL) {
                PcNet.send(pl, new PcNet.Msg("#tour|stop|" + (s.stop + 1) + "|" + STOPS.length + "|" + st.title() + "|" + st.x() + "|" + (st.y() + st.h() / 2) + "|" + st.z()));
                sl.playSound(null, pl.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f);
                s.bar.setName(Component.literal("✈ " + st.title() + "  §7(" + (s.stop + 1) + "/" + STOPS.length + ")"));
            }
        } else {
            int ot = s.t - TRAVEL;
            double ang = a0 + ot * 0.004;
            at = orbitPoint(st, ang);
            if (ot % ORBIT_PER_LINE == 1 && ot / ORBIT_PER_LINE < st.lines().length) {
                PcNet.send(pl, new PcNet.Msg("#tour|say|" + st.lines()[ot / ORBIT_PER_LINE]));
            }
            highlight(sl, pl, st, ot);
            boolean waiting = !st.task().isEmpty() && ot >= talk - 20 && !s.taskDone && s.waitTask < 200;
            if (waiting) {
                s.waitTask++;
                s.t--;
                if (st.task().equals("quiz") && s.quiz < 0) {
                    s.quiz = 0;
                    askQuiz(pl, s);
                }
                if (st.task().equals("quiz")) s.waitTask = Math.min(s.waitTask, 150);
            } else if (ot >= talk) {
                s.stop++;
                s.t = 0;
                s.from = seat.position();
                s.taskDone = false;
                s.waitTask = 0;
                s.bar.setProgress(Math.min(1f, s.stop / (float) STOPS.length));
                if (s.stop >= STOPS.length) {
                    finale(sl, pl, s);
                    stop(pl, true);
                    return;
                }
            }
        }
        seat.teleportTo(at.x, at.y, at.z);
        if (s.t % 3 == 0) sl.sendParticles(pl, new DustParticleOptions(new Vector3f(0.4f, 0.85f, 1f), 1.2f), true, at.x, at.y - 0.2, at.z, 3, 0.4, 0.1, 0.4, 0);
        if (s.t % 5 == 0) sl.sendParticles(pl, ParticleTypes.END_ROD, true, at.x, at.y - 0.4, at.z, 1, 0.3, 0.05, 0.3, 0.01);
    }

    static void highlight(ServerLevel sl, ServerPlayer pl, Stop st, int ot) {
        if (ot % 4 != 0) return;
        double x1 = st.x() - st.sx(), x2 = st.x() + st.sx(), z1 = st.z() - st.sz(), z2 = st.z() + st.sz(), y0 = st.y() - 1, y1 = st.y() + st.h();
        DustParticleOptions c = new DustParticleOptions(new Vector3f(0.35f, 0.9f, 1f), 1.6f);
        double k = (ot % 80) / 80.0;
        for (int i = 0; i < 4; i++) {
            double t = (k + i / 4.0) % 1;
            double per = t * 4;
            double px, pz;
            if (per < 1) { px = x1 + (x2 - x1) * per; pz = z1; }
            else if (per < 2) { px = x2; pz = z1 + (z2 - z1) * (per - 1); }
            else if (per < 3) { px = x2 - (x2 - x1) * (per - 2); pz = z2; }
            else { px = x1; pz = z2 - (z2 - z1) * (per - 3); }
            for (double y = y0; y <= y1; y += 2.5) sl.sendParticles(pl, c, true, px, y, pz, 1, 0, 0, 0, 0);
        }
        for (double y = y1; y < y1 + 30; y += 1.5) sl.sendParticles(pl, ParticleTypes.END_ROD, true, st.x(), y, st.z(), 1, 0.05, 0.1, 0.05, 0);
    }

    public static void onPhoneOpen(ServerPlayer pl) {
        State s = ACTIVE.get(pl.getUUID());
        if (s == null || s.stop >= STOPS.length || !STOPS[s.stop].task().equals("phone") || s.taskDone) return;
        s.taskDone = true;
        PcNet.send(pl, new PcNet.Msg("#tour|say|Perfect! That's your SolPhone. Close it and let's keep flying!"));
        pl.serverLevel().playSound(null, pl.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.8f);
    }

    static final String[][] QUIZ = {
            {"Who rebuilds things that creepers blow up?", "Gus", "Hugo", "Pip"},
            {"How do you talk to residents?", "Just type in chat", "Punch them", "Shout into a well"},
            {"Where can you order food to your door?", "SolEats", "The Bank", "The Observatory"}};

    static void askQuiz(ServerPlayer pl, State s) {
        if (s.quiz >= QUIZ.length) {
            s.taskDone = true;
            int reward = s.correct * 15;
            PcNet.send(pl, new PcNet.Msg("#tour|say|You got " + s.correct + "/" + QUIZ.length + "! " + (reward > 0 ? "Here's " + reward + " coins - welcome to Solaris!" : "Welcome to Solaris anyway!")));
            if (reward > 0) {
                CityData d = CityData.get(pl.serverLevel());
                String pn = pl.getName().getString();
                if (Bank.holder(d, pn) != null) d.pay(CityData.CITY, Bank.sav(Bank.playerKey(pn)), reward, "Sky Tour quiz prize", Calendar.worldDay(pl.serverLevel()), 0, false);
                else Post.give(pl, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLD_NUGGET, reward));
            }
            return;
        }
        String[] q = QUIZ[s.quiz];
        int[] order = {1, 2, 3};
        java.util.Random rnd = new java.util.Random(pl.getUUID().hashCode() + s.quiz);
        for (int i = 2; i > 0; i--) { int j = rnd.nextInt(i + 1); int t = order[i]; order[i] = order[j]; order[j] = t; }
        MutableComponent m = Component.literal("§b✈ Q" + (s.quiz + 1) + ": §f" + q[0] + "  ");
        for (int o : order) {
            m.append(Component.literal("[" + q[o] + "]").withStyle(Style.EMPTY.withColor(0x7FDBFF).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sol tutorial answer " + (o == 1 ? 1 : 0)))));
            m.append(Component.literal(" "));
        }
        pl.sendSystemMessage(m);
        PcNet.send(pl, new PcNet.Msg("#tour|say|" + q[0]));
    }

    public static void answer(ServerPlayer pl, boolean right) {
        State s = ACTIVE.get(pl.getUUID());
        if (s == null || s.quiz < 0 || s.quiz >= QUIZ.length) return;
        if (right) s.correct++;
        pl.serverLevel().playSound(null, pl.blockPosition(), right ? SoundEvents.PLAYER_LEVELUP : SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.7f, right ? 1.6f : 1f);
        pl.displayClientMessage(Component.literal(right ? "§a✔ Correct!" : "§c✘ Not quite - it was " + QUIZ[s.quiz][1] + "."), true);
        s.quiz++;
        s.waitTask = 0;
        askQuiz(pl, s);
    }

    static void finale(ServerLevel sl, ServerPlayer pl, State s) {
        Fireworks.burst(sl, pl.blockPosition().above(8), 10, 60, 4);
        pl.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal("§b§lWelcome to Solaris")));
        pl.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.literal("§fTour complete - go make some friends!")));
    }
}
