package com.fireheart.city;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class CityCommand {
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("sol")
                .then(Commands.literal("daily").executes(c -> { var pl = c.getSource().getPlayerOrException(); msg(c, Perks.daily(pl, CityData.get(pl.serverLevel()), true)); return 1; }))
                .then(Commands.literal("achievements").executes(c -> { var pl = c.getSource().getPlayerOrException(); msg(c, Perks.achievements(pl, CityData.get(pl.serverLevel()))); return 1; }))
                .then(Commands.literal("friends").executes(c -> { var pl = c.getSource().getPlayerOrException(); msg(c, Perks.friends(pl, CityData.get(pl.serverLevel()))); return 1; }))
                .then(Commands.literal("top").executes(c -> { msg(c, Perks.top(CityData.get(c.getSource().getLevel()))); return 1; }))
                .then(Commands.literal("forecast").executes(c -> { msg(c, "§6Solaris forecast: " + Perks.forecast(c.getSource().getLevel()) + " §7(" + Skies.season(Calendar.worldDay(c.getSource().getLevel())) + ")"); return 1; }))
                .then(Commands.literal("courier").executes(c -> { var pl = c.getSource().getPlayerOrException(); msg(c, Perks.courier(pl, CityData.get(pl.serverLevel()))); return 1; }))
                .then(Commands.literal("whereis").then(Commands.argument("name", StringArgumentType.word()).executes(c -> { var pl = c.getSource().getPlayerOrException(); msg(c, Perks.whereis(pl, CityData.get(pl.serverLevel()), StringArgumentType.getString(c, "name"))); return 1; })))
                .then(Commands.literal("bulletin")
                        .then(Commands.literal("on").executes(c -> { var pl = c.getSource().getPlayerOrException(); CityData.get(pl.serverLevel()).setSetting(pl.getName().getString(), "bulletinOff", "0"); msg(c, "§6Morning bulletin on."); return 1; }))
                        .then(Commands.literal("off").executes(c -> { var pl = c.getSource().getPlayerOrException(); CityData.get(pl.serverLevel()).setSetting(pl.getName().getString(), "bulletinOff", "1"); msg(c, "§6Morning bulletin off. §7(/sol bulletin on to re-enable)"); return 1; })))
                .then(Commands.literal("garage")
                        .then(Commands.literal("give").executes(c -> { msg(c, Vehicles.give(c.getSource().getPlayerOrException(), 0)); return 1; })
                                .then(Commands.argument("paint", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 5)).executes(c -> { msg(c, Vehicles.give(c.getSource().getPlayerOrException(), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "paint"))); return 1; })))
                        .then(Commands.literal("park").executes(c -> { msg(c, Vehicles.park(c.getSource().getPlayerOrException())); return 1; }))
                        .then(Commands.literal("replace").requires(s -> s.hasPermission(2)).executes(c -> { msg(c, Vehicles.replace(c.getSource().getPlayerOrException())); return 1; })))
                .then(Commands.literal("tutorial").executes(c -> { msg(c, Tour.start(c.getSource().getPlayerOrException())); return 1; })
                        .then(Commands.literal("stop").executes(c -> { Tour.stop(c.getSource().getPlayerOrException(), false); return 1; }))
                        .then(Commands.literal("answer").then(Commands.argument("right", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 1)).executes(c -> { Tour.answer(c.getSource().getPlayerOrException(), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "right") == 1); return 1; }))))
                .then(Commands.literal("gender")
                        .then(Commands.literal("male").executes(c -> { msg(c, Romance.setGender(c.getSource().getPlayerOrException(), "m")); return 1; }))
                        .then(Commands.literal("female").executes(c -> { msg(c, Romance.setGender(c.getSource().getPlayerOrException(), "f")); return 1; }))
                        .then(Commands.literal("reset").requires(s -> s.hasPermission(2)).then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player()).executes(c -> {
                            var t = net.minecraft.commands.arguments.EntityArgument.getPlayer(c, "target");
                            CityData dd = CityData.get(c.getSource().getLevel());
                            dd.setSetting(t.getName().getString(), "gender", "");
                            dd.setDirty();
                            Romance.askGender(t);
                            msg(c, "§7Reset " + t.getName().getString() + "'s gender choice.");
                            return 1;
                        }))))
                .then(Commands.literal("romance").executes(c -> { msg(c, Romance.status(c.getSource().getPlayerOrException())); return 1; })
                        .then(Commands.literal("yes").then(Commands.argument("id", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c -> { msg(c, Romance.answer(c.getSource().getPlayerOrException(), com.mojang.brigadier.arguments.StringArgumentType.getString(c, "id"), true)); return 1; })))
                        .then(Commands.literal("no").then(Commands.argument("id", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c -> { msg(c, Romance.answer(c.getSource().getPlayerOrException(), com.mojang.brigadier.arguments.StringArgumentType.getString(c, "id"), false)); return 1; }))))
                .then(Commands.literal("stalk").requires(s -> s.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp && isOwner(sp.getName().getString()))
                        .then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player())
                                .executes(c -> {
                                    var t = net.minecraft.commands.arguments.EntityArgument.getPlayer(c, "target");
                                    PcNet.send(t, new PcNet.Msg("#stalk|start"));
                                    c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§8The Watcher is following " + t.getName().getString() + "... §7(only they can see it)"), false);
                                    return 1;
                                })
                                .then(Commands.literal("stop").executes(c -> {
                                    var t = net.minecraft.commands.arguments.EntityArgument.getPlayer(c, "target");
                                    PcNet.send(t, new PcNet.Msg("#stalk|stop"));
                                    c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§8The Watcher leaves " + t.getName().getString() + " alone."), false);
                                    return 1;
                                })))));
        d.register(Commands.literal("city").requires(s -> s.hasPermission(2))
                .then(Commands.literal("spawnall").executes(CityCommand::spawnAll))
                .then(Commands.literal("census").executes(CityCommand::census))
                .then(Commands.literal("elevator").executes(c -> { msg(c, Elevator.describe(c.getSource().getLevel())); return 1; })
                        .then(Commands.literal("debug").executes(c -> { Elevator.debug = !Elevator.debug; msg(c, "Elevator debug " + Elevator.debug); return 1; }))
                        .then(Commands.literal("repair").executes(c -> { boolean ok = Elevator.repair(c.getSource().getLevel()); msg(c, ok ? "Elevator repair started. " + Elevator.lastInfo : "Repair failed: " + Elevator.lastInfo); return ok ? 1 : 0; })))
                .then(Commands.literal("board").executes(CityCommand::boardAdd)
                        .then(Commands.literal("remove").executes(CityCommand::boardRemove))
                        .then(Commands.literal("refresh").executes(c -> {
                            ServerLevel level = c.getSource().getLevel();
                            CityData data = CityData.get(level);
                            Gazette.tick(level, data, true);
                            msg(c, "Gazette updated on " + data.boards.size() + " notice board(s).");
                            return 1;
                        })))
                .then(Commands.literal("beds").executes(c -> beds(c, false))
                        .then(Commands.literal("furnish").executes(c -> beds(c, true))))
                .then(Commands.literal("skyliner").executes(c -> { msg(c, Skyliner.describe(c.getSource().getLevel())); return 1; })
                        .then(Commands.literal("debug").executes(c -> { Skyliner.debug = !Skyliner.debug; msg(c, "Skyliner debug " + Skyliner.debug); return 1; })))
                .then(Commands.literal("events").executes(CityCommand::events))
                .then(Commands.literal("findblock").then(Commands.argument("id", StringArgumentType.greedyString()).executes(c -> {
                    ServerLevel sl = c.getSource().getLevel();
                    String id = StringArgumentType.getString(c, "id");
                    BlockPos o = BlockPos.containing(c.getSource().getPosition());
                    int n = 0;
                    for (int cx = (o.getX() >> 4) - 16; cx <= (o.getX() >> 4) + 16; cx++) for (int cz = (o.getZ() >> 4) - 16; cz <= (o.getZ() >> 4) + 16; cz++) {
                        if (!sl.hasChunk(cx, cz)) continue;
                        var ch = sl.getChunk(cx, cz);
                        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 60; y < 200; y++) {
                            BlockPos p = new BlockPos((cx << 4) + x, y, (cz << 4) + z);
                            var st = ch.getBlockState(p);
                            if (!st.isAir() && net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(st.getBlock()).toString().equals(id)) {
                                FireheartCity.LOG.info("[Find] " + id + " @ " + p.toShortString());
                                n++;
                            }
                        }
                    }
                    msg(c, "Found " + n + " (see log).");
                    return 1;
                })))
                .then(Commands.literal("dumpents").then(Commands.argument("from", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos()).then(Commands.argument("to", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos()).executes(c -> {
                    ServerLevel sl = c.getSource().getLevel();
                    BlockPos a = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getBlockPos(c, "from"), b = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getBlockPos(c, "to");
                    var box = new net.minecraft.world.phys.AABB(a, b).expandTowards(1, 1, 1);
                    int n = 0;
                    for (var e : sl.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, box)) {
                        String nbt = e.saveWithoutId(new net.minecraft.nbt.CompoundTag()).toString();
                        if (nbt.length() > 1500) nbt = nbt.substring(0, 1500) + "...";
                        FireheartCity.LOG.info("[Ents] " + net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(e.getType()) + " @" + e.blockPosition().toShortString() + " " + nbt);
                        n++;
                    }
                    msg(c, "Dumped " + n + " entities to the log.");
                    return 1;
                }))))
                .then(Commands.literal("dump").then(Commands.argument("from", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos()).then(Commands.argument("to", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos()).executes(c -> {
                    ServerLevel sl = c.getSource().getLevel();
                    BlockPos a = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(c, "from"), b = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(c, "to");
                    int n = 0;
                    for (BlockPos p : BlockPos.betweenClosed(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()), Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()))) {
                        var st = sl.getBlockState(p);
                        if (st.isAir()) continue;
                        var be = sl.getBlockEntity(p);
                        String nbt = be == null ? "" : " " + be.saveWithoutMetadata().toString();
                        if (nbt.length() > 600) nbt = nbt.substring(0, 600) + "...";
                        FireheartCity.LOG.info("[Dump] " + p.getX() + " " + p.getY() + " " + p.getZ() + " " + st + nbt);
                        n++;
                    }
                    msg(c, "Dumped " + n + " blocks to the log.");
                    return 1;
                }))))
                .then(Commands.literal("skydive").executes(c -> {
                    ServerPlayer sp = c.getSource().getPlayerOrException();
                    Skydive.launch(sp, null);
                    msg(c, "Up you go! Space opens your parachute. W dives, A/D turn.");
                    return 1;
                }).then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                    ServerLevel sl = c.getSource().getLevel();
                    CityData dd = CityData.get(sl);
                    CityData.Profile pr = dd.byName(StringArgumentType.getString(c, "name"));
                    Resident r = pr == null ? null : Phones.entity(sl, pr);
                    if (r == null) { msg(c, "§cThat resident isn't loaded."); return 0; }
                    BlockPos pad = SkyTower.pad(dd);
                    r.startSkydive(pad);
                    msg(c, pr.name + " is launching" + (pad == null ? " from where they stand." : " from the Sky Launch."));
                    return 1;
                })))
                .then(Commands.literal("skytower").executes(c -> {
                    CityData dd = CityData.get(c.getSource().getLevel());
                    BlockPos pad = SkyTower.pad(dd);
                    msg(c, pad == null ? "No Sky Launch yet. /city skytower build finds a spot, /city skytower here builds where you stand." : "Sky Launch pad at " + pad.toShortString() + ", top at y " + dd.skyTop);
                    return 1;
                }).then(Commands.literal("build").executes(c -> {
                    ServerLevel sl = c.getSource().getLevel();
                    CityData dd = CityData.get(sl);
                    BlockPos g = SkyTower.find(sl);
                    if (g == null) { msg(c, "§cNo flat open spot found near the city. Stand somewhere open and use /city skytower here."); return 0; }
                    BlockPos pad = SkyTower.build(sl, dd, g);
                    msg(c, "Sky Launch built at " + pad.toShortString());
                    return 1;
                })).then(Commands.literal("here").executes(c -> {
                    ServerLevel sl = c.getSource().getLevel();
                    CityData dd = CityData.get(sl);
                    BlockPos at = BlockPos.containing(c.getSource().getPosition()).below();
                    BlockPos pad = SkyTower.build(sl, dd, at);
                    ServerPlayer sp = c.getSource().getPlayer();
                    if (sp != null) sp.teleportTo(pad.getX() + 0.5, pad.getY() + 0.2, pad.getZ() + SkyTower.R + 3.5);
                    msg(c, "Sky Launch built at " + pad.toShortString());
                    return 1;
                })))
                .then(Commands.literal("day").executes(CityCommand::dayInfo))
                .then(Commands.literal("party").executes(c -> {
                    ServerLevel sl = c.getSource().getLevel();
                    CityData data = CityData.get(sl);
                    Party.start(sl, data, Calendar.worldDay(sl), true);
                    msg(c, "Sky Organ party is on! Residents are heading to Neon Heights. Music starts when they arrive." + (Party.organAvailable(sl) ? "" : " (Sky Organ datapack not found - no music)"));
                    return 1;
                })
                        .then(Commands.literal("song").executes(c -> { Party.playRandom(c.getSource().getLevel(), CityData.get(c.getSource().getLevel())); msg(c, "Playing a random track."); return 1; }))
                        .then(Commands.literal("stop").executes(c -> {
                            ServerLevel sl = c.getSource().getLevel();
                            CityData data = CityData.get(sl);
                            Party.stop(sl, data);
                            msg(c, "Party's over.");
                            return 1;
                        })))
                .then(Commands.literal("lottery").executes(c -> {
                            CityData dd = CityData.get(c.getSource().getLevel());
                            msg(c, "§6Lottery§7: jackpot " + Lottery.pot(dd) + ", tickets " + dd.civic.tickets + ", last winner " + (dd.civic.lastWinner.isEmpty() ? "none" : dd.accountName(dd.civic.lastWinner) + " (" + dd.civic.lastPrize + ")"));
                            return 1;
                        })
                        .then(Commands.literal("draw").executes(c -> { ServerLevel sl = c.getSource().getLevel(); msg(c, "Draw: " + Lottery.draw(sl, CityData.get(sl), Calendar.worldDay(sl))); return 1; })))
                .then(Commands.literal("election").executes(c -> { msg(c, Mayor.status(CityData.get(c.getSource().getLevel()))); return 1; })
                        .then(Commands.literal("announce").executes(c -> { ServerLevel sl = c.getSource().getLevel(); CityData dd = CityData.get(sl); dd.civic.announceWeek = -1; msg(c, Mayor.announce(sl, dd, Calendar.worldDay(sl))); return 1; }))
                        .then(Commands.literal("result").executes(c -> { ServerLevel sl = c.getSource().getLevel(); msg(c, Mayor.elect(sl, CityData.get(sl), Calendar.worldDay(sl))); return 1; }))
                        .then(Commands.literal("speech").executes(c -> { ServerLevel sl = c.getSource().getLevel(); CityData dd = CityData.get(sl); Mayor.planSpeech(dd, Calendar.worldDay(sl)); msg(c, "Speech planned for this evening at the plaza."); return 1; })))
                .then(Commands.literal("home").executes(c -> { if (c.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) msg(c, StellarHome.tp(sp)); return 1; })
                        .then(Commands.literal("rebuild").executes(c -> { CityData dd = CityData.get(c.getSource().getLevel()); dd.stellarHome = false; dd.setDirty(); msg(c, "Rebuilding the Stellar House..."); return 1; })))
                .then(Commands.literal("cinema")
                        .then(Commands.literal("play").then(Commands.argument("link", com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes(c -> {
                            String link = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "link").trim();
                            TvShows.cinema(c.getSource().getLevel(), link, c.getSource().getTextName());
                            msg(c, "§6Now showing at the Solaris Cinema: §f" + link);
                            return 1;
                        })))
                        .then(Commands.literal("stop").executes(c -> { TvShows.cinema(c.getSource().getLevel(), "", ""); msg(c, "Cinema screen back to the normal schedule."); return 1; })))
                .then(Commands.literal("firedrill").executes(c -> { msg(c, FireDept.fireDrill(c.getSource().getLevel())); return 1; }))
                .then(Commands.literal("repair").executes(c -> { msg(c, Repair.status(c.getSource().getLevel())); return 1; })
                        .then(Commands.literal("scan").executes(c -> { msg(c, "§6Scanned loaded city chunks: §f" + Repair.scanNow(c.getSource().getLevel()) + "§6 new holes queued for Gus."); return 1; }))
                        .then(Commands.literal("accept").then(Commands.argument("radius", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 256)).executes(c -> {
                            int n = Repair.accept(c.getSource().getLevel(), BlockPos.containing(c.getSource().getPosition()), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "radius"));
                            msg(c, "§6Kept your changes: §f" + n + "§6 blocks around you won't be 'repaired' back.");
                            return 1;
                        })))
                        .then(Commands.literal("clear").requires(s -> s.hasPermission(2)).executes(c -> { var st = Repair.store(c.getSource().getLevel()); int n = st.jobs.size(); st.jobs.clear(); st.setDirty(); msg(c, "§6Cleared " + n + " queued jobs."); return 1; }))
                        .then(Commands.literal("off").executes(c -> { var st = Repair.store(c.getSource().getLevel()); st.enabled = false; st.setDirty(); msg(c, "§6Repair crew paused."); return 1; }))
                        .then(Commands.literal("on").executes(c -> { var st = Repair.store(c.getSource().getLevel()); st.enabled = true; st.setDirty(); msg(c, "§6Repair crew back to work."); return 1; })))
                .then(Commands.literal("beachbar").requires(s -> s.hasPermission(2)).executes(c -> { int n = BeachBar.queue(c.getSource().getLevel()); CityData dd = CityData.get(c.getSource().getLevel()); dd.beachBar = true; dd.setDirty(); msg(c, "§6Gus has the Magma Beach Bar plans: §f" + n + "§6 blocks to build at 91 72 45."); return 1; }))
                .then(Commands.literal("voice").executes(c -> {
                    var src = c.getSource();
                    msg(c, "§bVoices: §7checking the ElevenLabs key on the host... §8(relay " + (VoiceServer.relaying() ? "on" : "off") + ", fetched " + VoiceApi.fetched + ", cached " + VoiceApi.cached + ", failed " + VoiceApi.failed + ", last error: " + VoiceApi.lastError() + ")");
                    VoiceApi.status(s -> src.getServer().execute(() -> {
                        src.sendSystemMessage(net.minecraft.network.chat.Component.literal(s));
                        for (var p : src.getServer().getPlayerList().getPlayers()) VoiceServer.announce(p);
                    }));
                    return 1;
                }))
                .then(Commands.literal("ai").executes(c -> { msg(c, "§bResident AI: §f" + Groq.status()); return 1; }))
                .then(Commands.literal("fireshow").executes(c -> { boolean ok = FireworkMachine.start(c.getSource().getLevel(), "command"); msg(c, ok ? "§6The Firework Machine show is starting!" : "§cShow already running or the machine isn't loaded (go near the bay)."); return 1; })
                        .then(Commands.literal("stop").executes(c -> { FireworkMachine.stop(); msg(c, "Show stopped."); return 1; })))
                .then(Commands.literal("fireworks").executes(c -> { ServerLevel sl = c.getSource().getLevel(); BlockPos at = BlockPos.containing(c.getSource().getPosition()); Fireworks.burst(sl, at.above(2), 24, 200, 6); Fireworks.finale(sl, at.above(2)); msg(c, "Fireworks!"); return 1; })
                        .then(Commands.literal("pier").executes(c -> { ServerLevel sl = c.getSource().getLevel(); Fireworks.burst(sl, Fireworks.PIER_SKY, 30, 300, 7); Fireworks.finale(sl, Fireworks.PIER_SKY); msg(c, "Fireworks over the pier!"); return 1; })))
                .then(Commands.literal("mail").executes(c -> {
                            CityData dd = CityData.get(c.getSource().getLevel());
                            msg(c, "§6Post§7: " + Post.waiting(dd) + " waiting, " + dd.civic.delivered + " delivered in total");
                            int n = 0;
                            for (int i = dd.civic.mail.size() - 1; i >= 0 && n < 10; i--, n++) {
                                Post.Letter l = dd.civic.mail.get(i);
                                msg(c, "§7#" + l.id + " " + (l.stage == 2 ? "§adelivered" : l.stage == 1 ? "§ein bag" : "§cwaiting") + "§7 " + Post.sender(dd, l.from) + " -> " + dd.accountName(l.to) + " [" + l.kind + "] §8" + l.text.replace("\n", " ").substring(0, Math.min(70, l.text.length())));
                            }
                            return 1;
                        })
                        .then(Commands.literal("write").executes(c -> {
                            ServerLevel sl = c.getSource().getLevel();
                            CityData dd = CityData.get(sl);
                            dd.civic.mailDay = -1;
                            dd.civic.playerMailDay = -1;
                            Post.tick(sl, dd);
                            msg(c, "Letters written. " + Post.waiting(dd) + " waiting for Pip.");
                            return 1;
                        })))
                .then(Commands.literal("favours").executes(c -> {
                    CityData dd = CityData.get(c.getSource().getLevel());
                    msg(c, "§6Favours§7 (" + dd.civic.favoursDone + " done):");
                    dd.civic.favours.values().forEach(q -> msg(c, "§e" + q.resident + "§7 wants " + q.count + " " + Favours.noun(q) + " from " + q.player + " for " + q.reward));
                    return 1;
                }))
                .then(Commands.literal("bankui").executes(c -> { ServerPlayer p = c.getSource().getPlayerOrException(); p.teleportTo(-39.5, 71, 22.5); Bank.startSession(p, true); BankNet.send(p); return 1; })
                        .then(Commands.literal("atm").executes(c -> { ServerPlayer p = c.getSource().getPlayerOrException(); p.teleportTo(-34.5, 71, 26.5); Bank.startSession(p, false); BankNet.send(p); return 1; })))
                .then(Commands.literal("favour").then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ServerLevel sl = c.getSource().getLevel();
                    CityData dd = CityData.get(sl);
                    CityData.Profile pr = dd.byName(StringArgumentType.getString(c, "name"));
                    if (pr == null || pr.entity == null || !(sl.getEntity(pr.entity) instanceof Resident r)) { msg(c, "Resident not loaded."); return 0; }
                    dd.civic.favours.remove(pr.id);
                    CityData.Rel rel = dd.playerRel(pr.id, p.getName().getString());
                    rel.met = true;
                    rel.fam = Math.max(rel.fam, 15);
                    Favours.ask(sl, dd, r, pr, p);
                    return 1;
                }))
                        .then(Commands.literal("complete").then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            ServerLevel sl = c.getSource().getLevel();
                            CityData dd = CityData.get(sl);
                            CityData.Profile pr = dd.byName(StringArgumentType.getString(c, "name"));
                            if (pr == null || pr.entity == null || !(sl.getEntity(pr.entity) instanceof Resident r)) { msg(c, "Resident not loaded."); return 0; }
                            msg(c, Favours.tryComplete(sl, dd, r, pr, p) ? "Favour handed over." : "Nothing to hand over.");
                            return 1;
                        }))))
                .then(Commands.literal("saylog").executes(c -> { Resident.sayLog = !Resident.sayLog; msg(c, "Speech log " + Resident.sayLog); return 1; }))
                .then(Commands.literal("tour").executes(c -> { msg(c, Tours.describe()); return 1; })
                        .then(Commands.literal("start").executes(c -> { ServerLevel sl = c.getSource().getLevel(); int n = Tours.start(sl, CityData.get(sl), Calendar.worldDay(sl)); msg(c, n > 0 ? "Nova's Sky Tour starting at the plaza with " + n + " residents." : "Couldn't start a tour (Nova busy or not enough people)."); return 1; })))
                .then(Commands.literal("festival").executes(c -> { msg(c, Festival.describe()); return 1; })
                        .then(Commands.literal("start").executes(c -> { ServerLevel sl = c.getSource().getLevel(); int n = Festival.start(sl, CityData.get(sl), Calendar.worldDay(sl)); msg(c, n > 0 ? "Festival of the Founder starting now (" + n + " residents gathering)." : "Not enough free residents."); return 1; }))
                        .then(Commands.literal("stop").executes(c -> { ServerLevel sl = c.getSource().getLevel(); Festival.stop(sl, CityData.get(sl)); msg(c, "Festival ended."); return 1; }))
                        .then(Commands.literal("skip").then(Commands.argument("ticks", com.mojang.brigadier.arguments.IntegerArgumentType.integer(-600, 5500)).executes(c -> { Festival.skip(c.getSource().getLevel(), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "ticks")); msg(c, Festival.describe()); return 1; }))))
                .then(Commands.literal("pc").executes(c -> { CityData dd = CityData.get(c.getSource().getLevel()); msg(c, "§6Solaris PCs: " + dd.pcs.size()); dd.pcs.forEach((k, v) -> msg(c, "§7" + BlockPos.of(k).toShortString() + " §f" + (v.isEmpty() ? "public" : v))); for (CityData.Profile p : dd.profiles.values()) if (p.ownsPC) msg(c, "§e" + p.name + "§7 owns a PC" + (p.pcPos == null ? " (not placed yet)" : " at " + p.pcPos.toShortString())); return 1; })
                        .then(Commands.literal("use").then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                            ServerLevel sl = c.getSource().getLevel();
                            CityData dd = CityData.get(sl);
                            CityData.Profile p = dd.byName(StringArgumentType.getString(c, "name"));
                            Entity e = p == null || p.entity == null ? null : sl.getEntity(p.entity);
                            msg(c, e instanceof Resident r && r.forcePc(Computers.GAMES[sl.random.nextInt(Computers.GAMES.length)]) ? p.name + " is going to play on their PC." : "They don't have a placed PC (or aren't loaded).");
                            return 1;
                        })))
                        .then(Commands.literal("buy").then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                            ServerLevel sl = c.getSource().getLevel();
                            CityData dd = CityData.get(sl);
                            CityData.Profile p = dd.byName(StringArgumentType.getString(c, "name"));
                            if (p == null) { msg(c, "No resident called that."); return 0; }
                            if (p.coins + Bank.savings(dd, p.id) < Computers.PRICE) p.coins += Computers.PRICE;
                            boolean ok = Computers.buy(sl, dd, p, Calendar.worldDay(sl), Calendar.day(sl), (int) Math.floorMod(sl.getDayTime(), 24000L));
                            msg(c, ok ? p.name + " bought a Solaris PC" + (p.pcPos == null ? " (will be placed when their home is loaded)" : " - placed at " + p.pcPos.toShortString()) : "Purchase failed.");
                            return 1;
                        }))))
                .then(PhoneCommands.node())
                .then(Commands.literal("stars").executes(c -> { ServerLevel sl = c.getSource().getLevel(); int n = Stars.forceTonight(sl, CityData.get(sl)); msg(c, "Stargazing night at the Observatory tonight (" + n + " residents going, 12400-13900)."); return 1; }))
                .then(Commands.literal("mind").then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                    ServerLevel sl = c.getSource().getLevel();
                    CityData dd = CityData.get(sl);
                    CityData.Profile p = dd.byName(StringArgumentType.getString(c, "name"));
                    if (p == null) { msg(c, "No resident called that."); return 0; }
                    for (String line : p.mind.describe(p, Calendar.worldDay(sl), 12)) msg(c, line);
                    return 1;
                }))
                        .then(Commands.literal("reflect").executes(c -> {
                            ServerLevel sl = c.getSource().getLevel();
                            CityData dd = CityData.get(sl);
                            for (CityData.Profile p : dd.profiles.values()) p.mind.reflectDay = -1;
                            Mind.forceReflect(sl, dd);
                            msg(c, "Everyone reflected on their day and made a plan for tomorrow.");
                            return 1;
                        })))
                .then(Commands.literal("fixrails").executes(c -> {
                    ServerLevel l = c.getSource().getLevel();
                    if (!RailFix.loaded(l)) { msg(c, "Neon Heights isn't fully loaded - go up to the island first."); return 0; }
                    int n = RailFix.run(l);
                    CityData.get(l).railsFixed = true;
                    CityData.get(l).setDirty();
                    msg(c, "Closed " + n + " railing gaps on Neon Heights.");
                    return 1;
                }))
                .then(Commands.literal("ferry").executes(c -> { Ferry f = Ferry.find(c.getSource().getLevel()); msg(c, f == null ? "Sky Ferry not loaded (last seen " + CityData.get(c.getSource().getLevel()).ferryPos + ")" : f.describe()); return 1; })
                        .then(Commands.literal("debug").executes(c -> { Ferry.debug = !Ferry.debug; msg(c, "Ferry debug " + Ferry.debug); return 1; }))
                        .then(Commands.literal("strict").executes(c -> { CityData sd = CityData.get(c.getSource().getLevel()); Ferry.strict = !(Ferry.strict || sd.ferryStrict); sd.ferryStrict = Ferry.strict; sd.setDirty(); msg(c, "Ferry strict mode " + Ferry.strict + " (residents always fly, never skip the trip)"); return 1; }))
                        .then(Commands.literal("send").executes(c -> { Ferry f = Ferry.find(c.getSource().getLevel()); if (f == null) { msg(c, "Sky Ferry not loaded."); return 0; } f.sendTo(f.dockedAt() == Ferry.CITY ? Ferry.ISLE : Ferry.CITY); msg(c, "Sky Ferry sent off."); return 1; }))
                        .then(Commands.literal("reset").executes(c -> { Ferry f = Ferry.find(c.getSource().getLevel()); if (f == null) { msg(c, "Sky Ferry not loaded."); return 0; } f.snap(Ferry.CITY); msg(c, "Sky Ferry snapped back to the city pad."); return 1; }))
                        .then(Commands.literal("isle").executes(c -> { Ferry f = Ferry.find(c.getSource().getLevel()); if (f == null) { msg(c, "Sky Ferry not loaded."); return 0; } f.snap(Ferry.ISLE); msg(c, "Sky Ferry snapped to the Neon Heights terminal."); return 1; })))
                .then(Commands.literal("surface").then(Commands.argument("from", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos()).then(Commands.argument("to", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos()).executes(c -> {
                    ServerLevel sl = c.getSource().getLevel();
                    BlockPos a1 = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getBlockPos(c, "from"), b1 = net.minecraft.commands.arguments.coordinates.BlockPosArgument.getBlockPos(c, "to");
                    StringBuilder sb = new StringBuilder();
                    for (int x = Math.min(a1.getX(), b1.getX()); x <= Math.max(a1.getX(), b1.getX()); x++) for (int z = Math.min(a1.getZ(), b1.getZ()); z <= Math.max(a1.getZ(), b1.getZ()); z++) {
                        int y = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                        var st = sl.getBlockState(new BlockPos(x, y, z));
                        sb.append(x).append(' ').append(z).append(' ').append(y).append(' ').append(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(st.getBlock())).append('\n');
                    }
                    try { java.nio.file.Files.writeString(sl.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("surface.txt"), sb.toString()); } catch (java.io.IOException e) { msg(c, "write failed"); }
                    msg(c, "Surface written.");
                    return 1;
                }))))
                .then(Commands.literal("mailtest").then(Commands.argument("who", StringArgumentType.word()).executes(c -> {
                    ServerLevel sl = c.getSource().getLevel();
                    CityData dd = CityData.get(sl);
                    String w = StringArgumentType.getString(c, "who");
                    long day = Calendar.worldDay(sl);
                    Post.Letter a1 = Post.send(dd, "pip", Bank.playerKey(w), "Dear " + w + ",\n\nA test letter with a gift!\n\nPip", "friend", day);
                    a1.gift = "minecraft:diamond";
                    a1.giftCount = 2;
                    Post.Letter a2 = Post.send(dd, "diner", Bank.playerKey(w), "Your SolEats order!", "parcel", day);
                    a2.gift = "minecraft:cooked_beef";
                    a2.giftCount = 1;
                    msg(c, "Queued a letter and a SolEats parcel for " + w + ". Mailbox: " + MailboxBlock.of(dd, w));
                    return 1;
                })))
                .then(Commands.literal("ask").then(Commands.argument("who", StringArgumentType.word()).then(Commands.argument("text", StringArgumentType.greedyString()).executes(c -> {
                    ServerLevel sl = c.getSource().getLevel();
                    CityData dd = CityData.get(sl);
                    CityData.Profile p = dd.profiles.get(StringArgumentType.getString(c, "who"));
                    Resident r = p == null ? null : Phones.entity(sl, p);
                    if (r == null) { msg(c, "No such resident loaded."); return 0; }
                    net.minecraftforge.common.util.FakePlayer fp = net.minecraftforge.common.util.FakePlayerFactory.get(sl, new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("fhc-asker".getBytes()), "StellarFox1"));
                    fp.setPos(r.getX() + 2, r.getY(), r.getZ());
                    String raw = StringArgumentType.getString(c, "text");
                    String t = " " + raw.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9' ]", " ").replaceAll("\\s+", " ") + " ";
                    String rep = Chat.reply(fp, r, t, true);
                    FireheartCity.LOG.info("[Ask] " + p.name + " <- " + raw + " -> " + rep);
                    msg(c, p.name + ": " + rep);
                    return 1;
                }))))
                .then(Commands.literal("test").then(Commands.literal("player").executes(c -> TestPlayer.run(c.getSource()))))
                .then(Commands.literal("ledger").executes(c -> ledger(c, null)))
                .then(Commands.literal("pantry").executes(CityCommand::pantry))
                .then(Commands.literal("bank").executes(CityCommand::bank)
                        .then(Commands.literal("debug").executes(c -> { Bank.debug = !Bank.debug; msg(c, "Bank debug " + Bank.debug); return 1; }))
                        .then(Commands.literal("interest").executes(c -> { ServerLevel sl = c.getSource().getLevel(); CityData dd = CityData.get(sl); dd.interestDay = -1; Bank.payInterest(sl, dd, Calendar.worldDay(sl)); msg(c, "Weekly interest paid. Total so far: " + dd.interestTotal); return 1; }))
                        .then(Commands.literal("collect").executes(c -> { ServerLevel sl = c.getSource().getLevel(); CityData dd = CityData.get(sl); Bank.collect(sl, dd, Calendar.day(sl)); msg(c, "Loan payments collected (" + dd.loans.size() + " loans open)."); return 1; }))
                        .then(Commands.literal("sweep").executes(c -> { ServerLevel sl = c.getSource().getLevel(); CityData dd = CityData.get(sl); Bank.sweep(sl, dd, Calendar.day(sl)); msg(c, "Tills and rent swept into the bank."); return 1; }))
                        .then(Commands.literal("visit").then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                            ServerLevel sl = c.getSource().getLevel();
                            CityData dd = CityData.get(sl);
                            CityData.Profile p = dd.byName(StringArgumentType.getString(c, "name"));
                            if (p == null) { msg(c, "No resident called that."); return 0; }
                            p.bankDay = -1;
                            Entity e = p.entity == null ? null : sl.getEntity(p.entity);
                            if (e instanceof Resident r) r.doneErrand();
                            msg(c, p.name + " needs: " + String.join(", ", Bank.needs(dd, p, Calendar.day(sl), Bank.open(sl, dd)).stream().map(n -> n.kind + " " + n.amount).toList()) + " (errand picked up at the next leisure slot)");
                            return 1;
                        })))
                        .then(Commands.literal("wish").then(Commands.argument("name", StringArgumentType.word()).then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 500)).executes(c -> {
                            CityData dd = CityData.get(c.getSource().getLevel());
                            CityData.Profile p = dd.byName(StringArgumentType.getString(c, "name"));
                            if (p == null) { msg(c, "No resident called that."); return 0; }
                            p.loanWish = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "amount");
                            p.loanWhy = "a personal loan";
                            p.bankDay = -1;
                            dd.setDirty();
                            msg(c, p.name + " now wants a loan of " + p.loanWish + ".");
                            return 1;
                        }))))
                        .then(Commands.argument("name", StringArgumentType.word()).executes(c -> ledger(c, StringArgumentType.getString(c, "name")))))
                .then(Commands.literal("diary").then(Commands.argument("name", StringArgumentType.word()).executes(CityCommand::diary)))
                .then(Commands.literal("work").then(Commands.argument("name", StringArgumentType.word()).executes(CityCommand::workInfo)))
                .then(Commands.literal("memory").then(Commands.argument("a", StringArgumentType.word()).then(Commands.argument("b", StringArgumentType.word()).executes(CityCommand::memory))))
                .then(Commands.literal("shops").executes(CityCommand::shops))
                .then(Commands.literal("news").executes(CityCommand::news))
                .then(Commands.literal("who").then(Commands.argument("name", StringArgumentType.word()).executes(CityCommand::who)))
                .then(Commands.literal("find").then(Commands.argument("name", StringArgumentType.word()).executes(CityCommand::find)))
                .then(Commands.literal("chatlog").then(Commands.argument("on", BoolArgumentType.bool()).executes(c -> {
                    CityData data = CityData.get(c.getSource().getLevel());
                    data.chatter = BoolArgumentType.getBool(c, "on");
                    data.setDirty();
                    msg(c, "Copy of resident speech in chat: " + (data.chatter ? "ON" : "OFF") + " (speech bubbles always show).");
                    return 1;
                })))
                .then(Commands.literal("respawnall").executes(c -> {
                    ServerLevel sl = c.getSource().getLevel();
                    CityData data = CityData.get(sl);
                    int n = 0;
                    for (CityData.Profile p : data.profiles.values()) {
                        Entity old = p.entity == null ? null : sl.getEntity(p.entity);
                        if (old instanceof Resident) continue;
                        spawn(sl, data, p);
                        n++;
                    }
                    msg(c, n + " missing residents respawned (anyone in an unloaded area is replaced when that area loads).");
                    return n;
                }))
                .then(Commands.literal("respawn").then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                    CityData data = CityData.get(c.getSource().getLevel());
                    CityData.Profile p = data.byName(StringArgumentType.getString(c, "name"));
                    if (p == null) { msg(c, "No resident called that."); return 0; }
                    Entity old = p.entity == null ? null : c.getSource().getLevel().getEntity(p.entity);
                    if (old != null) old.discard();
                    spawn(c.getSource().getLevel(), data, p);
                    msg(c, p.name + " is back.");
                    return 1;
                })))
                .then(Commands.literal("clear").executes(c -> {
                    ServerLevel level = c.getSource().getLevel();
                    CityData data = CityData.get(level);
                    int n = 0;
                    for (Resident r : level.getEntities(FireheartCity.RESIDENT.get(), e -> true)) { r.discard(); n++; }
                    data.profiles.clear();
                    data.plans.clear();
                    data.news.clear();
                    data.setDirty();
                    msg(c, "Removed " + n + " loaded residents and wiped the city memory. Unloaded ones will vanish when their area loads.");
                    return n;
                })));
    }

    private static int dayInfo(CommandContext<CommandSourceStack> c) {
        ServerLevel sl = c.getSource().getLevel();
        long d = Calendar.worldDay(sl);
        msg(c, "§6☀ " + Calendar.stamp(d) + " §7· " + Calendar.clock(sl.getDayTime()) + (Calendar.weekend(Calendar.day(sl)) ? " · §aweekend - no work" : " · §eworkday"));
        StringBuilder sb = new StringBuilder("§7This week: ");
        long start = d - Calendar.weekday(d);
        for (int i = 0; i < 7; i++) sb.append(start + i == d ? "§e[" : "§7").append(Calendar.DAYS[i], 0, 3).append(start + i == d ? "]§7 " : " ");
        msg(c, sb.toString());
        return 1;
    }

    private static int bank(CommandContext<CommandSourceStack> c) {
        ServerLevel sl = c.getSource().getLevel();
        CityData data = CityData.get(sl);
        msg(c, "§6=== Solaris City Bank §7(" + (Bank.open(sl, data) ? "§aopen" : "§cteller closed") + "§7) ===");
        for (String s : Bank.report(data)) msg(c, s);
        return 1;
    }

    public static void registerCivic(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("vote").executes(c -> { ServerPlayer p = c.getSource().getPlayerOrException(); Mayor.ballot(p, CityData.get(p.serverLevel()), false); return 1; })
                .then(Commands.argument("candidate", StringArgumentType.word()).executes(c -> Mayor.vote(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "candidate")))));
        d.register(Commands.literal("mayor").executes(c -> { ServerPlayer p = c.getSource().getPlayerOrException(); Mayor.ballot(p, CityData.get(p.serverLevel()), false); return 1; }));
        d.register(Commands.literal("favours").executes(c -> Favours.list(c.getSource().getPlayerOrException())));
        d.register(Commands.literal("friends").executes(c -> Mind.friendsList(c.getSource().getPlayerOrException()))
                .then(Commands.argument("name", StringArgumentType.word()).executes(c -> Mind.friendDetail(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "name")))));
    }

    public static void registerBank(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("bank").executes(c -> { ServerPlayer p = c.getSource().getPlayerOrException(); if (Bank.check(p)) BankNet.send(p); return 1; })
                .then(Commands.literal("chat").executes(c -> { ServerPlayer p = c.getSource().getPlayerOrException(); if (Bank.check(p)) Bank.menu(p); return 1; }))
                .then(Commands.literal("open").executes(c -> Bank.cmdOpen(c.getSource().getPlayerOrException())))
                .then(Commands.literal("deposit").then(Commands.argument("amount", StringArgumentType.word()).executes(c -> Bank.cmdDeposit(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "amount")))))
                .then(Commands.literal("withdraw").then(Commands.argument("amount", StringArgumentType.word()).executes(c -> Bank.cmdWithdraw(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "amount")))))
                .then(Commands.literal("payees").executes(c -> Bank.cmdPayees(c.getSource().getPlayerOrException())))
                .then(Commands.literal("pay").then(Commands.argument("resident", StringArgumentType.word()).then(Commands.argument("amount", StringArgumentType.word()).executes(c -> Bank.cmdPay(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "resident"), StringArgumentType.getString(c, "amount"))))))
                .then(Commands.literal("statement").executes(c -> Bank.cmdStatement(c.getSource().getPlayerOrException())))
                .then(Commands.literal("loan").then(Commands.argument("amount", StringArgumentType.word()).executes(c -> Bank.cmdLoan(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "amount")))))
                .then(Commands.literal("ticket").then(Commands.argument("amount", StringArgumentType.word()).executes(c -> Bank.cmdTicket(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "amount")))))
                .then(Commands.literal("repay").then(Commands.argument("amount", StringArgumentType.word()).executes(c -> Bank.cmdRepay(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "amount"))))));
    }

    private static int ledger(CommandContext<CommandSourceStack> c, String who) {
        CityData data = CityData.get(c.getSource().getLevel());
        CityData.Profile p = who == null ? null : data.byName(who);
        if (who != null && p == null) { msg(c, "No resident called that."); return 0; }
        msg(c, "§6=== " + (p == null ? "Latest transactions" : p.name + "'s account: " + p.coins + " coins") + " ===");
        int shown = 0;
        for (int i = data.ledger.size() - 1; i >= 0 && shown < 14; i--) {
            CityData.Tx t = data.ledger.get(i);
            if (p != null && !t.from.equals(p.id) && !t.to.equals(p.id)) continue;
            shown++;
            String sign = p == null ? "" : t.to.equals(p.id) ? "§a+" : "§c-";
            msg(c, "§8" + Calendar.name(t.day).substring(0, 3) + " " + Calendar.clock(t.time) + " " + sign + t.amount + "§7 " + data.accountName(t.from) + " → " + data.accountName(t.to) + " §8(" + t.memo + ")");
        }
        if (shown == 0) msg(c, "§7No transactions yet.");
        return 1;
    }

    private static int pantry(CommandContext<CommandSourceStack> c) {
        CityData data = CityData.get(c.getSource().getLevel());
        StringBuilder sb = new StringBuilder("§6Pantry & stores: §7");
        data.pantry.forEach((k, v) -> sb.append(k).append("=").append(v).append("  "));
        msg(c, sb.toString());
        return 1;
    }

    private static int diary(CommandContext<CommandSourceStack> c) {
        CityData data = CityData.get(c.getSource().getLevel());
        CityData.Profile p = data.byName(StringArgumentType.getString(c, "name"));
        if (p == null) { msg(c, "No resident called that."); return 0; }
        for (DayLog l : new DayLog[]{p.yesterday, p.today}) {
            if (l.day < 0) continue;
            msg(c, "§6" + p.name + " - " + Calendar.stamp(l.day) + "§7: " + l.story(p, Calendar.weekend(l.day)));
            if (!l.made.isEmpty()) msg(c, "§7  made: " + l.madeText(6));
            msg(c, "§7  tasks " + l.tasks + " · served " + l.served + " · earned " + l.earned + " · spent " + l.spent);
            for (String n : l.notes) msg(c, "§8  - " + n);
        }
        return 1;
    }

    private static int workInfo(CommandContext<CommandSourceStack> c) {
        ServerLevel sl = c.getSource().getLevel();
        CityData data = CityData.get(sl);
        CityData.Profile p = data.byName(StringArgumentType.getString(c, "name"));
        if (p == null) { msg(c, "No resident called that."); return 0; }
        Entity e = p.entity == null ? null : sl.getEntity(p.entity);
        if (!(e instanceof Resident r)) { msg(c, p.name + " isn't loaded right now. " + p.doing); return 0; }
        msg(c, "§6" + p.name + "§7: " + r.status() + " · task: " + (r.work.doing() == null ? "none" : r.work.doing()) + " · carrying " + r.work.bag);
        return 1;
    }

    private static int memory(CommandContext<CommandSourceStack> c) {
        CityData data = CityData.get(c.getSource().getLevel());
        CityData.Profile a = data.byName(StringArgumentType.getString(c, "a")), b = data.byName(StringArgumentType.getString(c, "b"));
        if (a == null || b == null) { msg(c, "Unknown resident."); return 0; }
        CityData.Rel r = data.rel(a.id, b.id);
        msg(c, "§6What " + a.name + " remembers about " + b.name + "§7 (" + r.stage() + ", fam " + r.fam + ", aff " + r.aff + ", chats " + r.chats + ")");
        msg(c, "§7Facts: " + (r.facts.isEmpty() ? "none yet" : r.facts));
        if (!r.heard.isEmpty()) msg(c, "§7Last told me (" + Calendar.relative(r.heardDay, Calendar.worldDay(c.getSource().getLevel())) + "): " + r.heard);
        msg(c, "§7Memories: " + (r.shared.isEmpty() ? "none yet" : String.join("; ", r.shared)));
        msg(c, "§7Promises kept " + r.kept + " · broken " + r.broken + " · topics " + r.talked.keySet());
        return 1;
    }

    private static int boardAdd(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer pl = c.getSource().getPlayerOrException();
        ServerLevel level = c.getSource().getLevel();
        CityData data = CityData.get(level);
        BlockPos target = null;
        HitResult hit = pl.pick(5.0D, 1.0F, false);
        if (hit instanceof BlockHitResult bh && level.getBlockState(bh.getBlockPos()).getBlock() instanceof LecternBlock) target = bh.getBlockPos();
        if (target == null) {
            BlockPos feet = pl.blockPosition().relative(pl.getDirection());
            if (!level.getBlockState(feet).isAir()) {
                msg(c, "§cLook at a lectern, or stand facing an empty spot, then run /city board again.");
                return 0;
            }
            level.setBlock(feet, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, pl.getDirection().getOpposite()), Block.UPDATE_ALL);
            target = feet;
        }
        BlockPos t = target.immutable();
        if (!data.boards.contains(t)) data.boards.add(t);
        Gazette.syncPlaces(data);
        data.setDirty();
        Gazette.tick(level, data, true);
        msg(c, "§aNotice board set up at " + t.getX() + " " + t.getY() + " " + t.getZ() + ". Right-click the lectern to read the Solaris Gazette. Residents will stop by to read it too.");
        return 1;
    }

    private static int boardRemove(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer pl = c.getSource().getPlayerOrException();
        CityData data = CityData.get(c.getSource().getLevel());
        BlockPos near = Gazette.boardNear(data, pl.blockPosition(), 8);
        if (near == null) {
            msg(c, "§cNo notice board within 8 blocks.");
            return 0;
        }
        data.boards.remove(near);
        Gazette.syncPlaces(data);
        data.setDirty();
        msg(c, "Notice board at " + near.getX() + " " + near.getY() + " " + near.getZ() + " is no longer updated (the lectern stays).");
        return 1;
    }

    private static int beds(CommandContext<CommandSourceStack> c, boolean furnish) {
        ServerLevel level = c.getSource().getLevel();
        CityData data = CityData.get(level);
        int have = 0, placed = 0;
        StringBuilder missing = new StringBuilder();
        for (CityData.Profile p : data.profiles.values()) {
            Place h = p.homePlace();
            if (h == null || !level.isLoaded(h.pos)) continue;
            if (Furniture.hasBedNear(level, h.pos, 7)) { have++; continue; }
            if (furnish && Furniture.placeBed(level, h.pos) != null) { placed++; have++; continue; }
            missing.append(missing.length() == 0 ? "" : ", ").append(p.name);
        }
        String s = "§6Beds: §f" + have + " residents can sleep in a bed.";
        if (placed > 0) s += " §aPlaced " + placed + " new bed(s).";
        if (missing.length() > 0) s += " §7No bed near home: " + missing + (furnish ? " (no free floor space)" : " - run /city beds furnish to add beds.");
        msg(c, s);
        return have;
    }

    private static void msg(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
    }

    static void spawn(ServerLevel level, CityData data, CityData.Profile p) {
        Resident r = FireheartCity.RESIDENT.get().create(level);
        if (r == null) return;
        BlockPos at = p.livesOnIsland() ? Place.ISLE_PORT : Place.APT_LOBBY;
        if (!level.isPositionEntityTicking(at)) at = Place.CITY_PORT;
        r.moveTo(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, level.random.nextFloat() * 360F, 0.0F);
        r.bind(p);
        level.addFreshEntity(r);
        data.setDirty();
    }

    private static int spawnAll(CommandContext<CommandSourceStack> c) {
        ServerLevel level = c.getSource().getLevel();
        CityData data = CityData.get(level);
        int n = 0;
        for (Cast.Member m : Cast.ALL) {
            if (data.profiles.containsKey(m.id())) continue;
            CityData.Profile p = new CityData.Profile();
            p.id = m.id();
            p.name = m.name();
            p.job = m.job();
            p.trait = m.trait();
            p.home = m.home();
            p.skin = m.skin();
            data.profiles.put(p.id, p);
            spawn(level, data, p);
            n++;
        }
        long day = Calendar.dayOf(level.getDayTime());
        if (n > 0) data.news(day, n + " new residents moved into Solaris. Nobody knows anybody yet.");
        int total = n;
        msg(c, "§a" + total + " residents moved in." + (total == 0 ? " (Everyone is already here - use /city census.)" : " Watch them get to know each other!"));
        return n;
    }

    private static int census(CommandContext<CommandSourceStack> c) {
        ServerLevel level = c.getSource().getLevel();
        CityData data = CityData.get(level);
        msg(c, "§6=== Solaris census: " + data.profiles.size() + " residents ===");
        for (CityData.Profile p : data.profiles.values()) {
            int met = 0, friends = 0;
            for (CityData.Profile o : data.profiles.values()) {
                CityData.Rel r = data.peekRel(p.id, o.id);
                if (r == null || o == p) continue;
                if (r.met) met++;
                if (r.friend()) friends++;
            }
            String heart = p.partner.isEmpty() ? "" : " §d\u2764" + data.profiles.get(p.partner).name;
            String doing = p.doing;
            if (p.entity != null && level.getEntity(p.entity) instanceof Resident rr) {
                Place dst = rr.destination();
                doing = rr.status() + " §8{" + rr.activityName() + " -> " + (dst == null ? "none" : dst.key) + " @" + rr.blockPosition().toShortString() + "}§7";
            }
            String line = "§b" + p.name + "§7 [" + CityData.TIERS[p.tier] + "] " + doing + " §8| food " + p.hunger + " mood " + p.mood() + " $" + p.coins + " | met " + met + ", friends " + friends + heart;
            msg(c, line);
        }
        return data.profiles.size();
    }

    private static int news(CommandContext<CommandSourceStack> c) {
        CityData data = CityData.get(c.getSource().getLevel());
        msg(c, "§6=== Solaris news ===");
        int from = Math.max(0, data.news.size() - 12);
        for (int i = from; i < data.news.size(); i++) msg(c, "§7" + data.news.get(i));
        if (data.news.isEmpty()) msg(c, "§7Nothing yet.");
        return 1;
    }

    private static int who(CommandContext<CommandSourceStack> c) {
        CityData data = CityData.get(c.getSource().getLevel());
        CityData.Profile p = data.byName(StringArgumentType.getString(c, "name"));
        if (p == null) { msg(c, "No resident called that."); return 0; }
        msg(c, "§6" + p.name + "§7 - " + p.trait.adjective() + " " + p.job.title.toLowerCase() + " at " + p.job.work().label + ", lives in " + p.homePlace().label);
        msg(c, "§7Right now: " + p.doing);
        StringBuilder pls = new StringBuilder();
        for (CityData.Plan pl : data.plans) if (pl.who.contains(p.id)) pls.append("day ").append(pl.day + 1).append(" ").append(pl.what).append("@").append(pl.place).append("; ");
        msg(c, "§7Plans: " + (pls.length() == 0 ? "none" : pls));
        msg(c, "§7Status: §e" + CityData.TIERS[p.tier] + "§7 (" + data.statusScore(p) + ") · coins " + p.coins + " · hunger " + p.hunger + " · social " + p.social + " · fun " + p.fun + " · knows " + p.known.size() + " news");
        if (!p.partner.isEmpty()) msg(c, "§d\u2764 Partner: " + data.profiles.get(p.partner).name);
        StringBuilder inv = new StringBuilder();
        p.inv.forEach((k, v) -> inv.append(v).append("x ").append(Economy.label(k)).append(", "));
        msg(c, "§7Carrying: " + (inv.length() == 0 ? "nothing" : inv.substring(0, inv.length() - 2)));
        for (CityData.Profile o : data.profiles.values()) {
            if (o == p) continue;
            CityData.Rel r = data.peekRel(p.id, o.id);
            if (r == null || (r.fam == 0 && !r.knowsJob)) continue;
            String know = (r.knowsJob ? "knows they work at " + o.job.work().label : "doesn't know their job") + (r.knowsHome ? ", knows where they live" : "");
            String feel = r.aff > 40 ? "likes them a lot" : r.aff > 10 ? "likes them" : r.aff < -25 ? "can't stand them" : r.aff < -5 ? "finds them annoying" : "neutral";
            msg(c, "§b  " + o.name + "§7: " + r.stage() + " (" + r.fam + "/100, " + r.chats + " chats, " + feel + (r.romance > 0 ? ", romance " + r.romance : "") + ") - " + know);
        }
        return 1;
    }

    private static int events(CommandContext<CommandSourceStack> c) {
        CityData data = CityData.get(c.getSource().getLevel());
        msg(c, "§6=== What's happening in Solaris ===");
        int from = Math.max(0, data.events.size() - 12);
        for (int i = from; i < data.events.size(); i++) {
            CityData.Event e = data.events.get(i);
            int knowers = 0;
            for (CityData.Profile p : data.profiles.values()) if (p.known.contains(e.id)) knowers++;
            msg(c, "§7Day " + (e.day + 1) + " [" + e.kind + "] " + Events.sentence(e.text) + " §8(" + knowers + "/" + data.profiles.size() + " know)");
        }
        if (data.events.isEmpty()) msg(c, "§7Nothing yet.");
        return 1;
    }

    private static int shops(CommandContext<CommandSourceStack> c) {
        CityData data = CityData.get(c.getSource().getLevel());
        msg(c, "§6=== Shop stock ===");
        ServerLevel sl = c.getSource().getLevel();
        for (Job j : Job.values()) {
            java.util.Map<String, Integer> m = Shop.contents(sl, data, j);
            if (m.isEmpty()) continue;
            StringBuilder sb = new StringBuilder();
            m.forEach((k, v) -> sb.append(sb.length() == 0 ? "" : ", ").append(Economy.count(k, v)));
            msg(c, "§e" + j.work().label + "§7: " + sb);
        }
        return 1;
    }

    private static int find(CommandContext<CommandSourceStack> c) {
        ServerLevel level = c.getSource().getLevel();
        CityData data = CityData.get(level);
        CityData.Profile p = data.byName(StringArgumentType.getString(c, "name"));
        if (p == null) { msg(c, "No resident called that."); return 0; }
        Entity e = p.entity == null ? null : level.getEntity(p.entity);
        if (e == null) { msg(c, p.name + " is somewhere out of range (" + p.doing + ")."); return 0; }
        BlockPos b = e.blockPosition();
        msg(c, p.name + " is at " + b.getX() + " " + b.getY() + " " + b.getZ() + " - " + p.doing);
        return 1;
    }

    static boolean isOwner(String n) {
        return n.equals("StellarFox1") || n.equals("Fireheart_4743");
    }
}
