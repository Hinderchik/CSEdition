package com.csedition.command;

import com.csedition.config.MapConfig;
import com.csedition.data.MapData;
import com.csedition.data.Team;
import com.csedition.match.MatchManager;
import com.csedition.network.CSPackets;
import com.csedition.network.PacketMapList;
import com.csedition.network.PacketSyncMaps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Команды мода CS Edition.
 *
 * Управление матчем:
 *   /cs start [mapId]   — запустить матч (минимум 2 игрока)
 *   /cs stop            — остановить матч
 *   /cs status          — статус матча
 *   /cs maps            — список карт
 *   /cs money <amount>  — выдать деньги
 *
 * Редактирование карт (на лету, без перезапуска):
 *   /cs setlobby                    — установить лобби на текущей позиции
 *   /cs addmap <id> [name]          — создать/обновить карту
 *   /cs delmap <id>                 — удалить карту
 *   /cs setspawn <mapId> <T|CT>     — добавить спавн на текущей позиции
 *   /cs clearspawns <mapId> <T|CT>  — очистить спавны команды
 *   /cs setbuyzone <mapId> <T|CT>   — установить зону закупа (2 точки)
 *   /cs reload                     — перечитать maps.json с диска
 *
 * Все изменения автоматически:
 *   - Сохраняются в &lt;world&gt;/data/csedition/maps.json
 *   - Рассылаются всем клиентам через PacketSyncMaps
 */
public class CSCommand {

    private static final SuggestionProvider<CommandSourceStack> MAP_SUGGESTIONS = (ctx, builder) ->
        SharedSuggestionProvider.suggest(MapConfig.getMaps().keySet(), builder);

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("cs")
                .then(Commands.literal("help").executes(this::help))
                .then(Commands.literal("start")
                        .executes(ctx -> start(ctx, null))
                        .then(Commands.argument("mapId", StringArgumentType.string())
                                .suggests(MAP_SUGGESTIONS)
                                .executes(ctx -> start(ctx, StringArgumentType.getString(ctx, "mapId")))))
                .then(Commands.literal("stop").executes(this::stop))
                .then(Commands.literal("status").executes(this::status))
                .then(Commands.literal("maps").executes(this::listMaps))
                .then(Commands.literal("money")
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                .executes(this::giveMoney)))
                // === Редактирование карт ===
                .then(Commands.literal("setlobby").executes(this::setLobby))
                .then(Commands.literal("addmap")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(ctx -> addMap(ctx, StringArgumentType.getString(ctx, "id"), null))
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .executes(ctx -> addMap(ctx,
                                                StringArgumentType.getString(ctx, "id"),
                                                StringArgumentType.getString(ctx, "name"))))))
                .then(Commands.literal("delmap")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .suggests(MAP_SUGGESTIONS)
                                .executes(ctx -> delMap(ctx, StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("setspawn")
                        .then(Commands.argument("mapId", StringArgumentType.string())
                                .suggests(MAP_SUGGESTIONS)
                                .then(Commands.argument("team", StringArgumentType.string())
                                        .executes(ctx -> addSpawn(ctx,
                                                StringArgumentType.getString(ctx, "mapId"),
                                                StringArgumentType.getString(ctx, "team"))))))
                .then(Commands.literal("clearspawns")
                        .then(Commands.argument("mapId", StringArgumentType.string())
                                .suggests(MAP_SUGGESTIONS)
                                .then(Commands.argument("team", StringArgumentType.string())
                                        .executes(ctx -> clearSpawns(ctx,
                                                StringArgumentType.getString(ctx, "mapId"),
                                                StringArgumentType.getString(ctx, "team"))))))
                .then(Commands.literal("setbuyzone")
                        .then(Commands.argument("mapId", StringArgumentType.string())
                                .suggests(MAP_SUGGESTIONS)
                                .then(Commands.argument("team", StringArgumentType.string())
                                        .executes(ctx -> setBuyZone(ctx,
                                                StringArgumentType.getString(ctx, "mapId"),
                                                StringArgumentType.getString(ctx, "team")))))
                .then(Commands.literal("reload").executes(this::reload))
        );
    }

    // ====================== Управление матчем ======================

    private int help(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.literal("§6§l=== CS Edition Commands ==="));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs start [mapId]§7 — запустить матч"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs stop§7 — остановить матч"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs status§7 — статус"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs maps§7 — список карт"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs money <amount>§7 — выдать деньги"));
        ctx.getSource().sendSystemMessage(Component.literal("§6§l--- Редактирование карт ---"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs setlobby§7 — лобби = ваша позиция"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs addmap <id> [name]§7 — создать карту"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs delmap <id>§7 — удалить карту"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs setspawn <mapId> <T|CT>§7 — спавн = ваша позиция"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs clearspawns <mapId> <T|CT>§7 — очистить"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs setbuyzone <mapId> <T|CT>§7 — зона закупа"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs reload§7 — перечитать maps.json"));
        ctx.getSource().sendSystemMessage(Component.literal("§7Файл: §f" + MapConfig.getCurrentFile()));
        return 1;
    }

    private int start(CommandContext<CommandSourceStack> ctx, String mapId) {
        MatchManager mm = MatchManager.getInstance();
        if (mapId != null) {
            if (MapConfig.getMap(mapId) == null) {
                ctx.getSource().sendSystemMessage(Component.literal("§cUnknown map: " + mapId));
                return 0;
            }
            mm.setCurrentMap(mapId);
        }
        if (mm.getCurrentMapId() == null) {
            ctx.getSource().sendSystemMessage(Component.literal("§cNo map selected. Use /cs start <mapId>"));
            return 0;
        }
        mm.startNewRound();
        ctx.getSource().sendSystemMessage(Component.literal("§aMatch started on map: " + mm.getCurrentMapId()));
        return 1;
    }

    private int stop(CommandContext<CommandSourceStack> ctx) {
        MatchManager mm = MatchManager.getInstance();
        mm.setPhase(com.csedition.data.GamePhase.LOBBY);
        for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
            mm.teleportToLobby(p);
        }
        ctx.getSource().sendSystemMessage(Component.literal("§aMatch stopped. All players returned to lobby."));
        return 1;
    }

    private int status(CommandContext<CommandSourceStack> ctx) {
        MatchManager mm = MatchManager.getInstance();
        ctx.getSource().sendSystemMessage(Component.literal("§6Phase: §f" + mm.getPhase()));
        ctx.getSource().sendSystemMessage(Component.literal("§6Map: §f" + mm.getCurrentMapId()));
        ctx.getSource().sendSystemMessage(Component.literal("§6Timer: §f" + (mm.getPhaseTicks() / 20) + "s"));
        ctx.getSource().sendSystemMessage(Component.literal("§6Players: §f" + ctx.getSource().getServer().getPlayerList().getPlayerCount()));
        return 1;
    }

    private int listMaps(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.literal("§6§l=== Maps ==="));
        for (MapData m : MapConfig.getMaps().values()) {
            ctx.getSource().sendSystemMessage(Component.literal(
                "§e" + m.getId() + " §7(" + m.getDisplayName() + ") §fT:" + m.getTSpawns().size() + " CT:" + m.getCtSpawns().size()));
        }
        return 1;
    }

    private int giveMoney(CommandContext<CommandSourceStack> ctx) {
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            MatchManager mm = MatchManager.getInstance();
            com.csedition.data.PlayerData pd = mm.getOrCreate(p);
            pd.addMoney(amount);
            mm.sendMoneyUpdate(p, pd);
            p.sendSystemMessage(Component.literal("§a+$" + amount + " (now $" + pd.getMoney() + ")"));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("§cThis command must be run as a player"));
            return 0;
        }
    }

    // ====================== Редактирование карт ======================

    private int setLobby(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            BlockPos pos = p.blockPosition();
            MapConfig.setLobbySpawn(pos);
            broadcastMaps();
            p.sendSystemMessage(Component.literal("§aLobby set to " + pos));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("§cThis command must be run as a player"));
            return 0;
        }
    }

    private int addMap(CommandContext<CommandSourceStack> ctx, String id, String name) {
        if (name == null) name = id;
        MapConfig.addOrUpdateMap(id, name);
        broadcastMaps();
        ctx.getSource().sendSystemMessage(Component.literal("§aMap added/updated: " + id + " (" + name + ")"));
        return 1;
    }

    private int delMap(CommandContext<CommandSourceStack> ctx, String id) {
        if (MapConfig.getMap(id) == null) {
            ctx.getSource().sendSystemMessage(Component.literal("§cUnknown map: " + id));
            return 0;
        }
        MapConfig.deleteMap(id);
        broadcastMaps();
        ctx.getSource().sendSystemMessage(Component.literal("§aMap deleted: " + id));
        return 1;
    }

    private int addSpawn(CommandContext<CommandSourceStack> ctx, String mapId, String teamStr) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            if (MapConfig.getMap(mapId) == null) {
                p.sendSystemMessage(Component.literal("§cUnknown map: " + mapId));
                return 0;
            }
            Team team = parseTeam(teamStr);
            if (team == null) {
                p.sendSystemMessage(Component.literal("§cTeam must be T or CT"));
                return 0;
            }
            MapConfig.addSpawn(mapId, team, p.blockPosition());
            broadcastMaps();
            p.sendSystemMessage(Component.literal("§aSpawn added for " + team + " on " + mapId + " at " + p.blockPosition()));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("§cThis command must be run as a player"));
            return 0;
        }
    }

    private int clearSpawns(CommandContext<CommandSourceStack> ctx, String mapId, String teamStr) {
        if (MapConfig.getMap(mapId) == null) {
            ctx.getSource().sendSystemMessage(Component.literal("§cUnknown map: " + mapId));
            return 0;
        }
        Team team = parseTeam(teamStr);
        if (team == null) {
            ctx.getSource().sendSystemMessage(Component.literal("§cTeam must be T or CT"));
            return 0;
        }
        MapConfig.clearSpawns(mapId, team);
        broadcastMaps();
        ctx.getSource().sendSystemMessage(Component.literal("§aSpawns cleared for " + team + " on " + mapId));
        return 1;
    }

    /**
     * Установка зоны закупа: первая точка — текущая позиция, вторая — противоположный угол.
     * Игрок должен встать в один угол, выполнить команду, затем в другой угол и выполнить снова.
     * Для простоты: первая команда устанавливает min, вторая — max.
     */
    private int setBuyZone(CommandContext<CommandSourceStack> ctx, String mapId, String teamStr) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            if (MapConfig.getMap(mapId) == null) {
                p.sendSystemMessage(Component.literal("§cUnknown map: " + mapId));
                return 0;
            }
            Team team = parseTeam(teamStr);
            if (team == null) {
                p.sendSystemMessage(Component.literal("§cTeam must be T or CT"));
                return 0;
            }
            MapData m = MapConfig.getMap(mapId);
            BlockPos current = p.blockPosition();
            BlockPos min, max;
            if (team == Team.T) {
                // Если min ещё не задан (равен max), это первая точка
                if (m.getTBuyZoneMin().equals(m.getTBuyZoneMax())) {
                    min = current;
                    max = current;
                    p.sendSystemMessage(Component.literal("§eFirst corner set. §7Now stand in the opposite corner and run the command again."));
                } else {
                    min = m.getTBuyZoneMin();
                    max = current;
                    p.sendSystemMessage(Component.literal("§aBuy zone for T set: " + min + " to " + max));
                }
            } else {
                if (m.getCtBuyZoneMin().equals(m.getCtBuyZoneMax())) {
                    min = current;
                    max = current;
                    p.sendSystemMessage(Component.literal("§eFirst corner set. §7Now stand in the opposite corner and run the command again."));
                } else {
                    min = m.getCtBuyZoneMin();
                    max = current;
                    p.sendSystemMessage(Component.literal("§aBuy zone for CT set: " + min + " to " + max));
                }
            }
            MapConfig.setBuyZone(mapId, team, min, max);
            broadcastMaps();
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("§cThis command must be run as a player"));
            return 0;
        }
    }

    private int reload(CommandContext<CommandSourceStack> ctx) {
        MapConfig.load();
        broadcastMaps();
        ctx.getSource().sendSystemMessage(Component.literal("§aMaps reloaded from " + MapConfig.getCurrentFile()));
        return 1;
    }

    // ====================== Утилиты ======================

    private Team parseTeam(String s) {
        if (s == null) return null;
        s = s.toUpperCase();
        if (s.equals("T") || s.equals("TERRORIST") || s.equals("TERRORISTS")) return Team.T;
        if (s.equals("CT") || s.equals("COUNTER") || s.equals("COUNTERTERRORIST")) return Team.CT;
        return null;
    }

    /**
     * Рассылает обновлённые карты всем клиентам.
     * Вызывается после любого изменения.
     */
    private void broadcastMaps() {
        // Список карт
        List<PacketMapList.MapEntry> entries = new ArrayList<>();
        for (MapData m : MapConfig.getMaps().values()) {
            entries.add(new PacketMapList.MapEntry(m.getId(), m.getDisplayName()));
        }
        PacketMapList listPkt = new PacketMapList(entries);
        // Полный JSON
        PacketSyncMaps jsonPkt = new PacketSyncMaps(MapConfig.toJson());
        for (ServerPlayer p : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            CSPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), listPkt);
            CSPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), jsonPkt);
        }
    }
}
