package com.fireheart.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * magmagamer9's hotel: Marco runs the front desk (with the service bell), three residents live in rooms 1-3, and
 * every night a resident or couple books the guest room - the stay is paid into magmagamer9's bank account.
 * Residents also drop by magmagamer9's house to visit him.
 */
public final class Hotel {
    private Hotel() {}

    public static final String OWNER = "magmagamer9";
    public static final BlockPos DESK_STAND = new BlockPos(44, 72, 33);
    public static final BlockPos DESK = new BlockPos(45, 72, 33);
    public static final BlockPos BELL = new BlockPos(46, 73, 33);
    public static final BlockPos GUEST = new BlockPos(47, 72, 33);
    static final BlockPos[] LINE_SPOTS = {GUEST, new BlockPos(48, 72, 34), new BlockPos(48, 72, 35), new BlockPos(49, 72, 36), new BlockPos(47, 72, 36)};
    static final AABB LOBBY = new AABB(43.5, 71.5, 24.5, 56.5, 75, 38.5);
    static final String GUEST_ROOM = "hotel4", SUITE = "hotel5";
    public static final int PRICE = 12;
    static final BlockPos HOUSE = new BlockPos(-48, 71, 42);

    static final Map<UUID, Long> DONE = new HashMap<>();
    static final LinkedHashMap<UUID, Long> LINE = new LinkedHashMap<>();
    static final Map<String, String> STAYS = new HashMap<>();
    static final Map<String, Long> VISITED = new HashMap<>();
    static final Map<String, Long> PLAYER_SEEN = new HashMap<>();
    static UUID serving;
    static long servingAt, idleAt, bookedDay = -1;
    static int stage;
    static boolean alone;

    public static void reset() {
        DONE.clear();
        LINE.clear();
        STAYS.clear();
        VISITED.clear();
        PLAYER_SEEN.clear();
        serving = null;
        stage = 0;
        bookedDay = -1;
    }

    public static boolean isRoom(String key) {
        return key != null && key.startsWith("hotel") && key.length() == 6;
    }

    public static String stayRoom(String id) {
        return STAYS.get(id);
    }

    public static Resident clerk(ServerLevel sl) {
        for (CityData.Profile p : CityData.get(sl).profiles.values()) {
            if (p.job != Job.CONCIERGE || p.entity == null) continue;
            if (sl.getEntity(p.entity) instanceof Resident r && r.isAlive()) return r;
        }
        return null;
    }

    static boolean onDuty(Resident r) {
        return r != null && r.activityName().equals("work") && r.convo == null && horiz(r, DESK_STAND) < 2.2 && Math.abs(r.getY() - DESK_STAND.getY()) < 1.5;
    }

    static boolean inLobby(Entity e) {
        return LOBBY.contains(e.position());
    }

    static boolean headingToRoom(Resident g) {
        Place d = g.destination();
        return d != null && isRoom(d.key);
    }

    public static BlockPos guestSpot(Resident g) {
        CityData.Profile p = g.profile();
        if (p == null || p.job == Job.CONCIERGE || g.isFollowing()) return null;
        UUID id = g.getUUID();
        Long done = DONE.get(id);
        if (done != null && done == g.routineDay()) return null;
        if (id.equals(serving)) return GUEST;
        if (!inLobby(g) || !headingToRoom(g)) {
            LINE.remove(id);
            return null;
        }
        if (!(g.level() instanceof ServerLevel)) return null;
        LINE.putIfAbsent(id, g.level().getGameTime());
        int idx = 0;
        for (UUID u : LINE.keySet()) {
            if (u.equals(id)) break;
            idx++;
        }
        return LINE_SPOTS[Math.min(idx, LINE_SPOTS.length - 1)];
    }

    static final BlockPos LOBBY_IN = new BlockPos(49, 72, 36);

    public static BlockPos catchUpSpot(Resident g, Place dest) {
        if (!isRoom(dest.key) || inLobby(g)) return null;
        Long done = DONE.get(g.getUUID());
        return done != null && done == g.routineDay() ? null : LOBBY_IN;
    }

    public static boolean holding(Resident g) {
        return LINE.containsKey(g.getUUID());
    }

    public static void tick(ServerLevel sl, CityData d) {
        if (sl.dimension() != Level.OVERWORLD) return;
        long now = sl.getGameTime();
        if (now % 5 != 0 || d.profiles.isEmpty()) return;
        long tod = sl.getDayTime() % 24000;
        if (tod >= 1000 && tod < 1400 && !STAYS.isEmpty()) STAYS.clear();
        if (now % 200 == 0) book(sl, d, tod);
        if (now % 40 == 0) visits(sl, d);
        Resident clerk = clerk(sl);
        LINE.keySet().removeIf(u -> !(sl.getEntity(u) instanceof Resident g) || !g.isAlive() || (!u.equals(serving) && (!inLobby(g) || !headingToRoom(g))));
        if (serving != null && !LINE.containsKey(serving)) {
            serving = null;
            stage = 0;
        }
        if (serving == null) {
            for (UUID u : LINE.keySet()) {
                if (sl.getEntity(u) instanceof Resident g && horiz(g, GUEST) < 1.6 && g.convo == null) {
                    serving = u;
                    servingAt = now;
                    stage = 0;
                    break;
                }
            }
        }
        if (serving == null) {
            idle(sl, clerk, now);
            return;
        }
        Resident g = (Resident) sl.getEntity(serving);
        if (g == null || horiz(g, GUEST) > 4) {
            LINE.remove(serving);
            serving = null;
            return;
        }
        long t = now - servingAt;
        RandomSource rnd = g.getRandom();
        CityData.Profile gp = g.profile();
        String gn = gp == null ? "there" : gp.name;
        boolean staff = onDuty(clerk);
        g.getNavigation().stop();
        g.getLookControl().setLookAt(staff ? clerk.getX() : DESK.getX() + 0.5, staff ? clerk.getEyeY() : DESK.getY() + 1.2, staff ? clerk.getZ() : DESK.getZ() + 0.5, 30, 30);
        if (staff) {
            clerk.getNavigation().stop();
            clerk.getLookControl().setLookAt(g, 30, 30);
        }
        boolean stay = gp != null && STAYS.containsKey(gp.id);
        if (stage == 0) {
            stage = 1;
            alone = !staff;
            g.swing(InteractionHand.MAIN_HAND);
            ring(sl);
            if (staff) {
                clerk.sayLine(stay ? Lines.pick(rnd, "Welcome to magmagamer9's hotel, " + gn + "! You're booked into the guest room tonight.", "Evening, " + gn + "! Reservation for one night, right here.")
                        : Lines.pick(rnd, "Welcome home, " + gn + ". Long day?", "Hey " + gn + "! Back already?", "Evening, " + gn + ". Your room's all made up."), 90);
                clerk.gesture(Resident.G_WAVE, 30);
            } else g.sayLine(Lines.pick(rnd, "*ding* Hello? ...Marco?", "*ding ding* Anyone at the desk?"), 60);
        } else if (stage == 1 && t >= 55) {
            stage = 2;
            if (alone) g.sayLine(Lines.pick(rnd, "Guess I'll grab my own key then.", "Nobody's here. I'll sign myself in."), 60);
            else g.sayLine(stay ? Lines.pick(rnd, "Can't wait - I've heard the rooms here are amazing.", "Thanks! A night away is exactly what I need.") : Lines.pick(rnd, "Just my key, thanks Marco.", "Hey Marco. Heading up to my room.", "Long day. Bed's calling."), 80);
        } else if (stage == 2 && t >= 105) {
            stage = 3;
            if (!alone) {
                clerk.gesture(Resident.G_GIVE, 30);
                clerk.showItem("minecraft:tripwire_hook", 30);
                clerk.sayLine(stay ? Lines.pick(rnd, "Room 4, up the stairs. Enjoy your stay - compliments of the owner, magmagamer9.", "Here's your key to Room 4. Breakfast is... well, the diner. Sleep well!")
                        : Lines.pick(rnd, "Here you go. Sleep well, " + gn + ".", "Key's yours. Night, " + gn + "!", "All checked in. See you at breakfast."), 90);
            } else g.showItem("minecraft:tripwire_hook", 30);
            sl.playSound(null, BELL, SoundEvents.ARMOR_EQUIP_CHAIN, SoundSource.BLOCKS, 0.6f, 1.6f);
        } else if (stage == 3 && t >= 145) {
            DONE.put(serving, g.routineDay());
            FireheartCity.LOG.info("[Hotel] checked in " + gn + (alone ? " (self check-in)" : " with Marco") + (stay ? " for a paid stay" : ""));
            LINE.remove(serving);
            serving = null;
            stage = 0;
            if (gp != null) gp.log(g.routineDay()).note(stay ? "I checked into magmagamer9's hotel for the night" : "Marco checked me in at the hotel");
        }
    }

    static void idle(ServerLevel sl, Resident clerk, long now) {
        if (!onDuty(clerk)) return;
        for (ServerPlayer pl : sl.players()) {
            if (pl.isSpectator() || !inLobby(pl)) continue;
            String pn = pl.getName().getString();
            Long seen = PLAYER_SEEN.get(pn);
            if (seen != null && now - seen < 3600) continue;
            PLAYER_SEEN.put(pn, now);
            clerk.getLookControl().setLookAt(pl, 30, 30);
            clerk.gesture(Resident.G_WAVE, 30);
            if (pn.equalsIgnoreCase(OWNER)) clerk.sayTo(Lines.pick(clerk.getRandom(), "Boss! Everything's running smoothly. We've got guests tonight.", "Welcome back, magmagamer9. The desk looks great, by the way.", "Hey boss. Want the nightly report? Rooms are full and the bell works."), 100);
            else clerk.sayTo(Lines.pick(clerk.getRandom(), "Welcome to magmagamer9's hotel, " + pn + "! Ring the bell if you want a room.", "Hi " + pn + "! Checking in? Just ring the bell.", "Evening, " + pn + ". Nicest hotel in Solaris - don't tell Ember Heights."), 100);
            idleAt = now;
            return;
        }
        if (now - idleAt > 2400 && clerk.getRandom().nextInt(40) == 0) {
            idleAt = now;
            clerk.say(Lines.pick(clerk.getRandom(), "*polishes the service bell*", "Another quiet night at the desk.", "magmagamer9 built a great place, honestly.", "*sorts the room keys*"), 70);
        }
    }

    static void ring(ServerLevel sl) {
        SoundEvent s = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("another_furniture", "block.service_bell.use"));
        if (s != null) sl.playSound(null, BELL, s, SoundSource.BLOCKS, 1f, 1f);
        else sl.playSound(null, BELL, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.9f, 2f);
        sl.sendParticles(ParticleTypes.NOTE, BELL.getX() + 0.5, BELL.getY() + 0.6, BELL.getZ() + 0.5, 1, 0, 0, 0, 0.5);
    }

    static boolean eligible(CityData.Profile p) {
        return p.entity != null && !p.livesOnIsland() && p.job != Job.CONCIERGE && p.job != Job.POLICE && p.job != Job.FIREFIGHTER && p.job != Job.REPAIR && !isRoom(p.home) && !Cast.nightShift(p.id);
    }

    static void book(ServerLevel sl, CityData d, long tod) {
        long day = Calendar.day(sl);
        if (bookedDay == day || tod < 6000 || tod > 12000) return;
        bookedDay = day;
        List<CityData.Profile> pool = new ArrayList<>();
        for (CityData.Profile p : d.profiles.values()) if (eligible(p)) pool.add(p);
        if (pool.isEmpty() || sl.random.nextFloat() < 0.25f) return;
        CityData.Profile a = pool.get(sl.random.nextInt(pool.size()));
        List<CityData.Profile> party = new ArrayList<>(List.of(a));
        CityData.Profile b = a.partner.isEmpty() ? null : d.profiles.get(a.partner);
        if (b != null && eligible(b)) party.add(b);
        int price = PRICE * party.size();
        String to = Bank.holder(d, OWNER) != null ? Bank.sav(Bank.playerKey(OWNER)) : "biz:hotel";
        int tod2 = (int) tod;
        boolean paid = d.pay(a.id, to, price, "Night at magmagamer9's hotel", day, tod2, true) || d.pay(Bank.sav(a.id), to, price, "Night at magmagamer9's hotel", day, tod2, true);
        if (!paid) return;
        for (CityData.Profile p : party) {
            STAYS.put(p.id, GUEST_ROOM);
            p.log(day).note("I booked a night at magmagamer9's hotel");
            p.fun = Math.min(100, p.fun + 8);
        }
        String who = party.size() == 2 ? a.name + " and " + b.name : a.name;
        d.news(day, who + " booked a night at magmagamer9's hotel.");
        FireheartCity.LOG.info("[Hotel] " + who + " booked Room 4 (" + price + " coins to " + to + ")");
        if (sl.getEntity(a.entity) instanceof Resident r) r.say(Lines.pick(r.getRandom(), "I just booked a night at magmagamer9's hotel! Treat yourself, right?", "Staycation tonight - magmagamer9's hotel!"), 80);
        ServerPlayer owner = sl.getServer().getPlayerList().getPlayerByName(OWNER);
        if (owner != null) {
            owner.sendSystemMessage(Component.literal("§6[Your Hotel] §e" + who + " booked Room 4 tonight §7(+" + price + " coins" + (to.startsWith("sav:") ? " to your bank" : "") + ")"));
            Phones.toast(owner, "Hotel booking", who + " is staying tonight (+" + price + " coins)");
        }
        d.setDirty();
    }

    static void visits(ServerLevel sl, CityData d) {
        ServerPlayer owner = sl.getServer().getPlayerList().getPlayerByName(OWNER);
        long day = Calendar.day(sl);
        for (Resident r : sl.getEntitiesOfClass(Resident.class, new AABB(HOUSE).inflate(5, 3, 5), r -> r.profile() != null)) {
            CityData.Profile p = r.profile();
            Place dest = r.destination();
            if (dest == null || !dest.key.equals("magma_house")) continue;
            Long last = VISITED.get(p.id);
            if (last != null && last == day) continue;
            VISITED.put(p.id, day);
            RandomSource rnd = r.getRandom();
            FireheartCity.LOG.info("[Hotel] " + p.name + " visited magmagamer9's house (" + (owner != null ? "online" : "offline") + ")");
            if (owner != null && owner.level() == sl && owner.distanceToSqr(r) < 20 * 20) {
                r.getLookControl().setLookAt(owner, 30, 30);
                r.gesture(Resident.G_WAVE, 40);
                r.sayTo(Lines.pick(rnd, "Hey magmagamer9! I was in the neighbourhood, thought I'd say hi!", "magmagamer9! Nice place you've got. Mind if I hang around a bit?", "Knock knock! Just checking in on my favourite hotel owner.", "Hi magmagamer9! I brought good vibes. And no gift. Sorry."), 100);
                Mind.playerEvent(d, p, OWNER, day, "I visited " + OWNER + " at his house", 2, 2);
                p.social = Math.min(100, p.social + 10);
                p.log(day).note("I visited magmagamer9 at his house");
            } else {
                sl.playSound(null, HOUSE, SoundEvents.WOOD_HIT, SoundSource.NEUTRAL, 1f, 0.9f);
                sl.playSound(null, HOUSE, SoundEvents.WOOD_HIT, SoundSource.NEUTRAL, 1f, 1f);
                r.gesture(Resident.G_KNOCK, 40);
                r.say(Lines.pick(rnd, "*knock knock* magmagamer9? ...Not home. I'll leave a note.", "*knocks* Guess he's out. Maybe at the hotel?"), 80);
                Post.Letter l = Post.send(d, p.id, Bank.playerKey(OWNER), "Hi magmagamer9!\n\nI came by your house today but you weren't home. Just wanted to say hi!\n\n- " + p.name, "letter", Calendar.worldDay(sl));
                l.day = Calendar.worldDay(sl);
                p.log(day).note("I dropped by magmagamer9's house but he wasn't home");
            }
            d.setDirty();
        }
    }

    public static double visitScore(Resident r, ServerLevel sl) {
        ServerPlayer o = sl.getServer().getPlayerList().getPlayerByName(OWNER);
        if (o == null) return -0.9;
        return o.blockPosition().closerThan(HOUSE, 40) ? 1.1 : 0.1;
    }

    public static void onLogin(ServerPlayer sp) {
        if (!sp.getName().getString().equalsIgnoreCase(OWNER)) return;
        CityData d = CityData.get(sp.serverLevel());
        if (!d.setting(OWNER, "hotelIntro", "").isEmpty()) return;
        d.setSetting(OWNER, "hotelIntro", "1");
        d.setDirty();
        sp.sendSystemMessage(Component.literal("§6[Your Hotel] §eMarco now runs your front desk and the service bell works! Finn, Priya and Mateo moved into Rooms 1-3, Marco lives in Room 5, and residents book Room 4 most nights - the money goes straight to your bank account."));
    }

    public static void onClick(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().isClientSide() || e.getHand() != InteractionHand.MAIN_HAND || !(e.getEntity() instanceof ServerPlayer pl) || !e.getPos().equals(BELL)) return;
        try {
            ServerLevel sl = pl.serverLevel();
            ring(sl);
            Resident clerk = clerk(sl);
            long now = sl.getGameTime();
            String pn = pl.getName().getString();
            Long seen = PLAYER_SEEN.get("bell:" + pn);
            if (seen != null && now - seen < 60) return;
            PLAYER_SEEN.put("bell:" + pn, now);
            if (!onDuty(clerk)) {
                pl.displayClientMessage(Component.literal("§7*ding* ...nobody's at the desk right now. Marco works noon till midnight."), true);
                return;
            }
            clerk.getLookControl().setLookAt(pl, 30, 30);
            clerk.gesture(Resident.G_GIVE, 30);
            clerk.showItem("minecraft:tripwire_hook", 30);
            ItemStack key = new ItemStack(Items.TRIPWIRE_HOOK);
            key.setHoverName(Component.literal(pn.equalsIgnoreCase(OWNER) ? "Master Key - magmagamer9's Hotel" : "Room 4 Key - magmagamer9's Hotel"));
            if (!pl.getInventory().add(key)) pl.drop(key, false);
            clerk.sayTo(pn.equalsIgnoreCase(OWNER) ? Lines.pick(clerk.getRandom(), "Master key for the boss. Every room's yours, obviously.", "Here you go, boss. Want me to ring the bell for you too?")
                    : Lines.pick(clerk.getRandom(), "Welcome, " + pn + "! Room 4, up the stairs. Enjoy your stay.", "Checked in! Room 4's got the best view. Night, " + pn + "."), 100);
        } catch (Throwable t) {
            FireheartCity.LOG.error("Hotel bell failed", t);
        }
    }

    public static String status(ServerLevel sl) {
        CityData d = CityData.get(sl);
        StringBuilder b = new StringBuilder("§6magmagamer9's hotel§7: ");
        Resident c = clerk(sl);
        b.append(c == null ? "no concierge yet" : "Marco is " + (onDuty(c) ? "§aat the desk" : "§e" + c.status()));
        for (int i = 1; i <= 5; i++) {
            String k = "hotel" + i;
            List<String> who = new ArrayList<>();
            for (CityData.Profile p : d.profiles.values()) if (k.equals(p.home) && !STAYS.containsKey(p.id) || k.equals(STAYS.get(p.id))) who.add(p.name);
            b.append("\n§7 Room ").append(i).append(": §f").append(who.isEmpty() ? "empty" : String.join(", ", who));
        }
        b.append("\n§7 In line at the desk: ").append(LINE.size());
        return b.toString();
    }

    public static String moveIn(ServerLevel sl, boolean tp) {
        CityData d = CityData.get(sl);
        int n = 0;
        for (CityData.Profile p : d.profiles.values()) {
            if (!isRoom(p.home) || p.entity == null) continue;
            if (sl.getEntity(p.entity) instanceof Resident r && tp) {
                Place h = Place.get(p.home);
                r.teleportTo(h.pos.getX() + 0.5, h.pos.getY(), h.pos.getZ() + 0.5);
                n++;
            }
        }
        return "§7Moved " + n + " hotel residents into their rooms.";
    }

    static double horiz(Entity e, BlockPos p) {
        double dx = e.getX() - (p.getX() + 0.5), dz = e.getZ() - (p.getZ() + 0.5);
        return Math.sqrt(dx * dx + dz * dz);
    }
}
