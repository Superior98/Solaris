package com.fireheart.city;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Admin commands for FirePhones, FireFeed and FireTech: /city phone ... */
public final class PhoneCommands {
    private PhoneCommands() {}

    static void msg(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
    }

    static CityData.Profile prof(CommandContext<CommandSourceStack> c, String arg) {
        return CityData.get(c.getSource().getLevel()).byName(StringArgumentType.getString(c, arg));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("phone")
                .executes(c -> {
                    CityData d = CityData.get(c.getSource().getLevel());
                    StringBuilder sb = new StringBuilder("§6SolPhone owners: §f");
                    for (CityData.Profile p : d.profiles.values()) if (p.ownsPhone) sb.append(p.name).append(" (").append(Phones.colorName(p.phoneColor)).append(") ");
                    msg(c, sb.toString());
                    StringBuilder w = new StringBuilder("§6Wants: §f");
                    for (CityData.Profile p : d.profiles.values()) if (!p.wantDevice.isEmpty()) w.append(p.name).append("→").append(p.wantDevice).append(" ");
                    msg(c, w.toString());
                    msg(c, "§7Texts: " + d.texts.size() + " · Posts: " + d.feed.size() + " · Calls live: " + (Phones.PLAYER_CALLS.size() + Phones.RES_CALLS.size()));
                    return 1;
                })
                .then(Commands.literal("give").executes(c -> {
                    ServerPlayer pl = c.getSource().getPlayerOrException();
                    Post.give(pl, PhoneItem.make(pl.getRandom().nextInt(Phones.COLORS.length)));
                    return 1;
                }).then(Commands.argument("color", IntegerArgumentType.integer(0, 7)).executes(c -> {
                    Post.give(c.getSource().getPlayerOrException(), PhoneItem.make(IntegerArgumentType.getInteger(c, "color")));
                    return 1;
                })))
                .then(Commands.literal("grant").then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                    ServerLevel sl = c.getSource().getLevel();
                    CityData.Profile p = prof(c, "name");
                    if (p == null) { msg(c, "No resident called that."); return 0; }
                    Phones.give(sl, CityData.get(sl), p, TechStore.pickColor(p), " (admin)");
                    msg(c, p.name + " now has a SolPhone.");
                    return 1;
                })))
                .then(Commands.literal("want").then(Commands.argument("name", StringArgumentType.word()).then(Commands.argument("what", StringArgumentType.word()).executes(c -> {
                    ServerLevel sl = c.getSource().getLevel();
                    CityData d = CityData.get(sl);
                    CityData.Profile p = prof(c, "name");
                    if (p == null) { msg(c, "No resident called that."); return 0; }
                    String w = StringArgumentType.getString(c, "what");
                    TechStore.want(d, p, w.equals("pc") ? "pc" : "phone", Calendar.worldDay(sl));
                    if (p.coins < 100) p.coins += 100;
                    Resident r = Phones.entity(sl, p);
                    if (r != null) r.replan();
                    msg(c, p.name + " now wants a " + p.wantDevice + " (goes to SolTech at leisure time).");
                    return 1;
                }))))
                .then(Commands.literal("text").then(Commands.argument("from", StringArgumentType.word()).then(Commands.argument("to", StringArgumentType.word()).executes(c -> {
                    ServerLevel sl = c.getSource().getLevel();
                    CityData d = CityData.get(sl);
                    CityData.Profile a = prof(c, "from"), b = prof(c, "to");
                    Resident r = Phones.entity(sl, a);
                    if (a == null || b == null || r == null) { msg(c, "Need two residents (sender loaded)."); return 0; }
                    String[] m = Phones.message(sl, d, r, a, b, Calendar.worldDay(sl), Phones.tod(sl), sl.random);
                    Phones.text(d, a.id, b.id, m[0], m[1], m[2], Calendar.worldDay(sl), Phones.tod(sl));
                    msg(c, a.name + " → " + b.name + " [" + m[1] + "]: " + m[0]);
                    return 1;
                }))))
                .then(Commands.literal("read").then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                    ServerLevel sl = c.getSource().getLevel();
                    CityData.Profile p = prof(c, "name");
                    Resident r = Phones.entity(sl, p);
                    if (r == null) { msg(c, "Not loaded."); return 0; }
                    int n = Phones.unread(CityData.get(sl), p.id).size();
                    r.usePhone(1, 40, "reading texts", () -> Phones.readTexts(r, p));
                    msg(c, p.name + " is reading " + n + " text(s).");
                    return 1;
                })))
                .then(Commands.literal("texts").executes(c -> {
                    CityData d = CityData.get(c.getSource().getLevel());
                    for (int i = Math.max(0, d.texts.size() - 12); i < d.texts.size(); i++) {
                        Phones.Text t = d.texts.get(i);
                        msg(c, "§7#" + t.id + " §e" + Phones.authorName(d, t.from) + "§7→§e" + Phones.authorName(d, t.to) + " §8[" + t.kind + (t.read ? ",read" : "") + "] §f" + t.text);
                    }
                    return 1;
                }))
                .then(Commands.literal("feed").executes(c -> {
                    CityData d = CityData.get(c.getSource().getLevel());
                    for (int i = Math.max(0, d.feed.size() - 10); i < d.feed.size(); i++) {
                        Phones.Post p = d.feed.get(i);
                        msg(c, "§7#" + p.id + " §b" + Phones.authorName(d, p.author) + "§f: " + p.text + " §c♥" + p.likes.size() + " §7💬" + p.comments.size());
                    }
                    return 1;
                }))
                .then(Commands.literal("browse").then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                    ServerLevel sl = c.getSource().getLevel();
                    CityData.Profile p = prof(c, "name");
                    Resident r = Phones.entity(sl, p);
                    if (r == null) { msg(c, "Not loaded."); return 0; }
                    r.usePhone(1, 40, "scrolling SolFeed on their phone", () -> Phones.browse(r, p, false));
                    return 1;
                })))
                .then(Commands.literal("callme").then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                    ServerLevel sl = c.getSource().getLevel();
                    ServerPlayer pl = c.getSource().getPlayerOrException();
                    CityData.Profile p = prof(c, "name");
                    Resident r = Phones.entity(sl, p);
                    if (r == null || !p.ownsPhone) { msg(c, "They need a phone and to be loaded."); return 0; }
                    msg(c, Phones.incoming(sl, CityData.get(sl), pl, p, r) ? p.name + " is calling you." : "Couldn't call.");
                    return 1;
                })))
                .then(Commands.literal("call").then(Commands.argument("a", StringArgumentType.word()).then(Commands.argument("b", StringArgumentType.word()).executes(c -> {
                    ServerLevel sl = c.getSource().getLevel();
                    CityData d = CityData.get(sl);
                    CityData.Profile a = prof(c, "a"), b = prof(c, "b");
                    Resident ra = Phones.entity(sl, a), rb = Phones.entity(sl, b);
                    if (ra == null || rb == null || !a.ownsPhone || !b.ownsPhone) { msg(c, "Both need phones and to be loaded."); return 0; }
                    Phones.Call call = new Phones.Call();
                    call.a = a.id;
                    call.b = b.id;
                    call.start = sl.getGameTime();
                    call.next = call.start + 40;
                    Phones.RES_CALLS.add(call);
                    ra.usePhone(2, 4000, "calling " + b.name, null);
                    rb.ringPhone(40);
                    msg(c, a.name + " is calling " + b.name + ".");
                    return 1;
                }))))
                .then(Commands.literal("debug").executes(c -> { Phones.debug = !Phones.debug; msg(c, "Phone debug " + Phones.debug); return 1; }))
                .then(Commands.literal("build").executes(c -> {
                    ServerLevel sl = c.getSource().getLevel();
                    CityData d = CityData.get(sl);
                    boolean ok = TechStore.build(sl, d);
                    TechStore.ensure(sl, d);
                    msg(c, ok ? "SolTech built at " + TechStore.CENTER.toShortString() : "Build function missing or area not loaded.");
                    return 1;
                }));
    }
}
