package com.fireheart.city;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * Romance between players and residents. Each player picks their gender once (saved for good); residents only fall for,
 * confess to or date players of the opposite gender. Flirting, compliments, gifts and dates raise romance; with enough
 * of it a resident may confess, or say yes when asked out.
 */
public final class Romance {
    private Romance() {}

    static final Set<String> WOMEN = Set.of("mia", "ava", "rosa", "nina", "ivy", "zara", "luna", "nova", "nell", "remy", "ellie", "kira", "sofia");
    static final Map<UUID, String[]> PENDING = new HashMap<>();

    public static boolean female(String residentId) {
        return WOMEN.contains(residentId);
    }

    public static String gender(CityData d, String pn) {
        return d.setting(pn, "gender", "");
    }

    public static boolean compatible(CityData d, String pn, CityData.Profile p) {
        String g = gender(d, pn);
        if (g.isEmpty() || p == null) return false;
        return g.equals("m") == female(p.id);
    }

    public static String sweetheart(CityData d, String pn) {
        return d.setting(pn, "sweetheart", "");
    }

    public static String datingPlayer(CityData d, String residentId) {
        return d.setting("res:" + residentId, "sweetheart", "");
    }

    static void pair(CityData d, String pn, String rid, long day) {
        String old = sweetheart(d, pn);
        if (!old.isEmpty()) d.setSetting("res:" + old, "sweetheart", "");
        d.setSetting(pn, "sweetheart", rid);
        d.setSetting("res:" + rid, "sweetheart", pn);
        d.setSetting(pn, "sweetheartSince", String.valueOf(day));
        d.setDirty();
    }

    static void unpair(CityData d, String pn) {
        String rid = sweetheart(d, pn);
        if (!rid.isEmpty()) d.setSetting("res:" + rid, "sweetheart", "");
        d.setSetting(pn, "sweetheart", "");
        d.setDirty();
    }

    public static String petName(CityData d, String pn, CityData.Profile p, RandomSource rnd) {
        if (p == null || !p.id.equals(sweetheart(d, pn))) return pn;
        return Lines.pick(rnd, "babe", "love", "sweetheart", pn, "hun", pn);
    }

    public static void onLogin(ServerPlayer sp, CityData d) {
        if (!gender(d, sp.getName().getString()).isEmpty()) return;
        sp.getServer().execute(() -> askGender(sp));
    }

    public static void askGender(ServerPlayer sp) {
        MutableComponent m = Component.literal("§d♥ §fSolaris needs to know one thing for dating & romance: ");
        m.append(button("[ I'm a guy ]", "/sol gender male", 0x5DADEC, "Residents who are women may fall for you"));
        m.append(Component.literal(" "));
        m.append(button("[ I'm a girl ]", "/sol gender female", 0xF78FB3, "Residents who are men may fall for you"));
        m.append(Component.literal(" §8(saved permanently)"));
        sp.sendSystemMessage(m);
    }

    static MutableComponent button(String text, String cmd, int color, String hover) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(color).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover))));
    }

    public static String setGender(ServerPlayer sp, String g) {
        CityData d = CityData.get(sp.serverLevel());
        String pn = sp.getName().getString();
        String cur = gender(d, pn);
        if (!cur.isEmpty()) return "§7You already chose: §f" + (cur.equals("m") ? "guy" : "girl") + "§7. That choice is permanent.";
        d.setSetting(pn, "gender", g.startsWith("m") ? "m" : "f");
        d.setDirty();
        return "§d♥ §fSaved! §7Residents " + (g.startsWith("m") ? "who are women" : "who are men") + " may now catch feelings for you. Be charming.";
    }

    static boolean any(String t, String... keys) {
        for (String k : keys) if (t.contains(k)) return true;
        return false;
    }

    /** Romance lines in chat. Returns a reply, or null when the message isn't romantic. */
    public static String chat(ServerPlayer pl, Resident r, String t, RandomSource rnd) {
        CityData d = r.data();
        CityData.Profile p = r.profile();
        if (p == null) return null;
        String pn = pl.getName().getString();
        CityData.Rel rel = d.playerRel(p.id, pn);
        long day = r.day();
        boolean mine = p.id.equals(sweetheart(d, pn));
        boolean comp = compatible(d, pn, p);
        String[] pend = PENDING.get(pl.getUUID());
        if (pend != null && pend[0].equals(p.id) && any(t, " yes ", " yeah ", " yea ", " sure ", " of course ", " i do ", " me too ", " i like you too ", " love you too ", " absolutely ")) {
            PENDING.remove(pl.getUUID());
            return accept(pl, r, p, d, rnd);
        }
        if (pend != null && pend[0].equals(p.id) && any(t, " no ", " nope ", " sorry ", " just friends ", " not really ")) {
            PENDING.remove(pl.getUUID());
            return decline(pl, r, p, d, rnd);
        }
        if (any(t, " break up ", " breakup ", " dump you ", " we're done ", " were done ", " its over ", " it's over ")) {
            if (!mine) return null;
            unpair(d, pn);
            rel.romance = Math.max(0, rel.romance - 30);
            rel.aff -= 10;
            r.gesture(Resident.G_SAD, 60);
            r.particles(ParticleTypes.FALLING_WATER, 6);
            d.event(day, "social", p.name + " and " + pn + " broke up", r.blockPosition(), p.id);
            Mind.playerEvent(d, p, pn, day, "{P} broke up with me", -6, 9);
            return Lines.pick(rnd, "Oh. I... okay. I didn't see that coming.", "Wow. Okay. I hope you're happy, " + pn + ".", "...Right. I think I need to be alone for a bit.");
        }
        if (any(t, " be my girlfriend ", " be my boyfriend ", " go out with me ", " date me ", " be my partner ", " be mine ", " be my valentine ", " will you go out ", " want to go out ", " i have a crush on you ", " i like you ", " i love you ", " i have feelings for you ", " love you ")) {
            if (mine) {
                rel.romance = Math.min(100, rel.romance + 2);
                r.particles(ParticleTypes.HEART, 5);
                r.gesture(Resident.G_HUGSELF, 40);
                return Lines.pick(rnd, "I love you too, " + pn + ". ♥", "You say that every time and it still gets me.", "Stop it, you're making me blush in public! ...Love you too.");
            }
            if (!comp) {
                if (gender(d, pn).isEmpty()) {
                    askGender(pl);
                    return "Oh! Um... that's sweet. " + Lines.pick(rnd, "Tell me a bit about yourself first?", "I don't even know much about you yet!");
                }
                return Lines.pick(rnd, "Aw, " + pn + ", that's really sweet - but I don't see you that way. Friends?", "I'm flattered, honestly. But it's a friendship thing for me.", "You're great, " + pn + ". Just not... like that. Sorry!");
            }
            String other = datingPlayer(d, p.id);
            if (!other.isEmpty() && !other.equals(pn)) return "I'm actually seeing " + other + " right now. Sorry, " + pn + ".";
            if (!p.partner.isEmpty() && d.profiles.get(p.partner) != null) return "That's sweet, but I'm with " + d.profiles.get(p.partner).name + ".";
            if (rel.romance >= 25 && rel.aff >= 35) return accept(pl, r, p, d, rnd);
            rel.romance = Math.min(100, rel.romance + 3);
            r.gesture(Resident.G_THINK, 30);
            return Lines.pick(rnd, "Whoa - okay, I did NOT expect that. Can we... get to know each other a little more first?", "You're cute, " + pn + ". Take me on a date first and we'll see. ♥", "Haha, smooth. Buy me dinner at the diner first?");
        }
        if (any(t, " you're cute ", " youre cute ", " you're beautiful ", " youre beautiful ", " you're pretty ", " youre pretty ", " you're handsome ", " youre handsome ", " you look nice ", " you look good ", " gorgeous ", " you're hot ", " youre hot ", " nice smile ")) {
            if (!comp && !mine) return null;
            if (!rel.talkedRecently("romance:compliment", day, 1)) {
                rel.talked.put("romance:compliment", day);
                rel.romance = Math.min(100, rel.romance + 4);
                rel.aff = Math.min(100, rel.aff + 1);
            }
            r.particles(ParticleTypes.HEART, 2);
            r.gesture(Resident.G_HUGSELF, 30);
            if (mine) return Lines.pick(rnd, "Only for you, " + petName(d, pn, p, rnd) + ".", "You're biased. But thank you. ♥");
            if (rel.romance > 40) return Lines.pick(rnd, "Stop, you're making my face do the thing.", "You keep saying that and I'm going to start believing it...", "...You really think so? ♥");
            return Lines.pick(rnd, "Oh! Haha, thank you!", "Aww. You're sweet, " + pn + ".", "Flattery will get you... somewhere, maybe.");
        }
        if (any(t, " kiss ", " hug me ", " give me a hug ", " cuddle ", " hold hands ")) {
            if (mine) {
                r.getLookControl().setLookAt(pl, 30, 30);
                r.particles(ParticleTypes.HEART, 8);
                r.gesture(Resident.G_HUGSELF, 50);
                pl.serverLevel().playSound(null, r.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1f, 1.4f);
                rel.romance = Math.min(100, rel.romance + 1);
                return any(t, " kiss ") ? Lines.pick(rnd, "*kisses you* ...Mwah. Happy now?", "*quick kiss* Not in front of everyone!", "*kisses you back* ♥") : Lines.pick(rnd, "*wraps you in a big hug*", "*hugs you tight* I needed that.", "*holds your hand* ♥");
            }
            if (comp && rel.romance > 50) return Lines.pick(rnd, "Whoa there! ...Maybe. Ask me properly first.", "Heh. We're not there yet, " + pn + ".");
            return Lines.pick(rnd, "Uh... let's keep it friendly!", "Personal space, " + pn + "!");
        }
        if (mine && any(t, " date ", " go out tonight ", " dinner with me ", " date night ")) {
            rel.dates++;
            rel.romance = Math.min(100, rel.romance + 5);
            r.follow(pl, 20 * 180);
            r.particles(ParticleTypes.HEART, 4);
            return Lines.pick(rnd, "Date night! Lead the way, " + petName(d, pn, p, rnd) + ".", "Yes! I'll follow you anywhere. Well, most places.", "Ooh, where are we going? I'm right behind you.");
        }
        if (comp && rel.aff > 25 && !rel.talkedRecently("romance:spark", day, 1) && rnd.nextInt(4) == 0) {
            rel.talked.put("romance:spark", day);
            rel.romance = Math.min(100, rel.romance + 2 + (Cast.flirty(p.id) ? 2 : 0));
        }
        return null;
    }

    static String accept(ServerPlayer pl, Resident r, CityData.Profile p, CityData d, RandomSource rnd) {
        String pn = pl.getName().getString();
        long day = r.day();
        CityData.Rel rel = d.playerRel(p.id, pn);
        rel.romance = Math.max(rel.romance, 40);
        pair(d, pn, p.id, day);
        r.particles(ParticleTypes.HEART, 12);
        r.gesture(Resident.G_CHEER, 60);
        pl.serverLevel().playSound(null, r.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.8f, 1.6f);
        d.event(day, "social", p.name + " and " + pn + " are officially dating!", r.blockPosition(), p.id);
        Mind.playerEvent(d, p, pn, day, "{P} and I started dating", 10, 10);
        pl.displayClientMessage(Component.literal("§d♥ You and " + p.name + " are now dating! ♥"), true);
        return Lines.pick(rnd, "YES. Yes! I've wanted this for so long, " + pn + "!", "Really? Okay. Okay! I'm yours. ♥", "Took you long enough! ...Yes. A thousand times yes.");
    }

    static String decline(ServerPlayer pl, Resident r, CityData.Profile p, CityData d, RandomSource rnd) {
        String pn = pl.getName().getString();
        CityData.Rel rel = d.playerRel(p.id, pn);
        rel.romance = Math.max(0, rel.romance - 15);
        rel.facts.put("turnedDown", "1");
        r.gesture(Resident.G_SAD, 50);
        return Lines.pick(rnd, "Oh. No, that's... that's okay. Friends it is.", "Right. Okay. Forget I said anything! Haha... ha.", "I get it. Thanks for being honest, " + pn + ".");
    }

    /** Every minute: a resident with strong feelings might come and confess. */
    public static void tick(ServerLevel sl, CityData d) {
        RandomSource rnd = sl.random;
        for (ServerPlayer pl : sl.players()) {
            String sw = sweetheart(d, pl.getName().getString());
            if (sw.isEmpty()) continue;
            CityData.Profile p = d.profiles.get(sw);
            if (p == null || p.entity == null || !(sl.getEntity(p.entity) instanceof Resident r)) continue;
            if (r.distanceTo(pl) < 6 && rnd.nextInt(3) == 0) r.particles(ParticleTypes.HEART, 1);
            if (sl.getGameTime() % 1200 < 100 && rnd.nextInt(40) == 0 && r.isFree())
                Computers.deliver(sl, d, pl.getName().getString(), p.id, Lines.pick(rnd, "Thinking of you ♥", "Miss you already", "Can't wait to see you later ♥", "Just saw something that reminded me of you :)", "Hey you. ♥"));
        }
        if (sl.getGameTime() % 1200 >= 100) return;
        PENDING.values().removeIf(pend -> sl.getGameTime() - Long.parseLong(pend[1]) > 20 * 150);
        for (ServerPlayer pl : sl.players()) {
            String pn = pl.getName().getString();
            if (gender(d, pn).isEmpty() || !sweetheart(d, pn).isEmpty() || PENDING.containsKey(pl.getUUID())) continue;
            for (Resident r : sl.getEntitiesOfClass(Resident.class, pl.getBoundingBox().inflate(40, 12, 40), x -> x.profile() != null && x.isFree())) {
                CityData.Profile p = r.profile();
                if (!compatible(d, pn, p) || !p.partner.isEmpty() || !datingPlayer(d, p.id).isEmpty()) continue;
                CityData.Rel rel = d.playerRel(p.id, pn);
                if (rel.romance < 30 || rel.aff < 40 || rel.facts.containsKey("confessed") || rel.facts.containsKey("turnedDown")) continue;
                if (rnd.nextInt(8) != 0) continue;
                confess(sl, d, pl, r, p, rel, rnd);
                return;
            }
        }
    }

    static void confess(ServerLevel sl, CityData d, ServerPlayer pl, Resident r, CityData.Profile p, CityData.Rel rel, RandomSource rnd) {
        String pn = pl.getName().getString();
        rel.facts.put("confessed", "1");
        d.setDirty();
        PENDING.put(pl.getUUID(), new String[]{p.id, String.valueOf(sl.getGameTime())});
        r.comeTo(pl, 20 * 40);
        String line = Lines.pick(rnd,
                pn + "... can I tell you something? I really, really like you. Like, more than a friend.",
                "Okay, I've been practising this all day. " + pn + ", I have a crush on you. There. I said it.",
                "I can't stop thinking about you, " + pn + ". Would you... want to be more than friends?",
                "This is terrifying, but here goes: I like you, " + pn + ". A lot. Do you feel the same?");
        sl.getServer().tell(new net.minecraft.server.TickTask(sl.getServer().getTickCount() + 60, () -> {
            if (!r.isAlive()) return;
            r.getLookControl().setLookAt(pl, 30, 30);
            r.gesture(Resident.G_HUGSELF, 80);
            r.particles(ParticleTypes.HEART, 6);
            r.sayTo(line, 160);
            MutableComponent m = Component.literal("§d♥ " + p.name + " is confessing their feelings to you! ");
            m.append(button("[ Yes ♥ ]", "/sol romance yes " + p.id, 0xFF6FA8, "Start dating " + p.name));
            m.append(Component.literal(" "));
            m.append(button("[ Just friends ]", "/sol romance no " + p.id, 0xAAAAAA, "Let them down gently"));
            pl.sendSystemMessage(m);
        }));
        Computers.deliver(sl, d, pn, p.id, "Hey... can we talk? In person? It's kind of important ♥");
    }

    public static String answer(ServerPlayer pl, String rid, boolean yes) {
        ServerLevel sl = pl.serverLevel();
        CityData d = CityData.get(sl);
        String[] pend = PENDING.get(pl.getUUID());
        if (pend == null || !pend[0].equals(rid)) return "§7Nobody is waiting for an answer from you right now.";
        PENDING.remove(pl.getUUID());
        CityData.Profile p = d.profiles.get(rid);
        if (p == null || p.entity == null || !(sl.getEntity(p.entity) instanceof Resident r)) return "§7They're not around any more.";
        String reply = yes ? accept(pl, r, p, d, sl.random) : decline(pl, r, p, d, sl.random);
        r.getLookControl().setLookAt(pl, 30, 30);
        r.sayTo(reply, 120);
        return yes ? "§d♥ You said yes to " + p.name + "!" : "§7You told " + p.name + " you'd rather stay friends.";
    }

    public static String status(ServerPlayer pl) {
        CityData d = CityData.get(pl.serverLevel());
        String pn = pl.getName().getString();
        String g = gender(d, pn);
        String sw = sweetheart(d, pn);
        StringBuilder sb = new StringBuilder("§d♥ Romance §7| you: §f" + (g.isEmpty() ? "not chosen" : g.equals("m") ? "guy" : "girl"));
        if (!sw.isEmpty() && d.profiles.get(sw) != null) sb.append(" §7| dating §f").append(d.profiles.get(sw).name);
        int shown = 0;
        for (CityData.Profile p : d.profiles.values()) {
            if (!d.playerNames(p.id).contains(pn) || !compatible(d, pn, p)) continue;
            CityData.Rel rel = d.playerRel(p.id, pn);
            if (rel.romance <= 0) continue;
            if (shown++ == 0) sb.append("\n§7Sparks: ");
            sb.append("§f").append(p.name).append(" §d").append("♥".repeat(Math.max(1, Math.min(5, rel.romance / 20 + 1)))).append("§7  ");
        }
        return sb.toString();
    }
}
