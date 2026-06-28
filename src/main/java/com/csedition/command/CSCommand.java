package com.csedition.command;

import com.csedition.config.CSConfig;
import com.csedition.config.MapConfig;
import com.csedition.config.ModeConfig;
import com.csedition.data.GameMode;
import com.csedition.data.GamePhase;
import com.csedition.data.MapData;
import com.csedition.data.Team;
import com.csedition.match.GunPriceTable;
import com.csedition.match.MatchManager;
import com.csedition.network.CSPackets;
import com.csedition.network.PacketMapList;
import com.csedition.network.PacketSyncMaps;
import com.csedition.network.PacketSyncModes;
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
 * Управление режимами:
 *   /cs mode <modeId>   — установить текущий режим
 *   /cs modes           — список режимов
 *   /cs setmapmode <mapId> <modeId> — привязать карту к режиму
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
 * Тестовые команды (для отладки):
 *   /cs test phase <phase>  — принудительно установить фазу
 *   /cs test team <T|CT>    — установить команду
 *   /cs test spawn          — телепорт на спавн текущей карты
 *   /cs test money <amount> — выдать деньги
 *
 * Все изменения автоматически:
 *   - Сохраняются в &lt;world&gt;/data/csedition/maps.json
 *   - Рассылаются всем клиентам через PacketSyncMaps
 */
public class CSCommand {

    private static final SuggestionProvider<CommandSourceStack> MAP_SUGGESTIONS = (ctx, builder) ->
        SharedSuggestionProvider.suggest(MapConfig.getMaps().keySet(), builder);

    private static final SuggestionProvider<CommandSourceStack> MODE_SUGGESTIONS = (ctx, builder) ->
        SharedSuggestionProvider.suggest(ModeConfig.getModes().keySet(), builder);

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
                // === Админ: выдать пушку конкретному игроку ===
                .then(Commands.literal("give")
                        .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                .then(Commands.argument("gunId", StringArgumentType.string())
                                        .executes(ctx -> adminGive(ctx,
                                                net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "gunId"))))))
                // === Режимы ===
                .then(Commands.literal("mode")
                        .then(Commands.argument("modeId", StringArgumentType.string())
                                .suggests(MODE_SUGGESTIONS)
                                .executes(ctx -> setMode(ctx, StringArgumentType.getString(ctx, "modeId")))))
                .then(Commands.literal("modes").executes(this::listModes))
                .then(Commands.literal("list")
                        .then(Commands.literal("guns").executes(this::listGuns))
                        .then(Commands.literal("modes").executes(this::listModes)))
                .then(Commands.literal("setmapmode")
                        .then(Commands.argument("mapId", StringArgumentType.string())
                                .suggests(MAP_SUGGESTIONS)
                                .then(Commands.argument("modeId", StringArgumentType.string())
                                        .suggests(MODE_SUGGESTIONS)
                                        .executes(ctx -> setMapMode(ctx,
                                                StringArgumentType.getString(ctx, "mapId"),
                                                StringArgumentType.getString(ctx, "modeId"))))))
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
                                                StringArgumentType.getString(ctx, "team"))))))
                .then(Commands.literal("reload").executes(this::reload))
                // === Тестовые команды ===
                .then(Commands.literal("test")
                        .then(Commands.literal("phase")
                                .then(Commands.argument("phase", StringArgumentType.string())
                                        .executes(ctx -> testPhase(ctx, StringArgumentType.getString(ctx, "phase")))))
                        .then(Commands.literal("team")
                                .then(Commands.argument("team", StringArgumentType.string())
                                        .executes(ctx -> testTeam(ctx, StringArgumentType.getString(ctx, "team")))))
                        .then(Commands.literal("spawn").executes(this::testSpawn))
                        .then(Commands.literal("money")
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(this::giveMoney)))
                        .then(Commands.literal("buy")
                                .then(Commands.argument("gunId", StringArgumentType.string())
                                        .executes(ctx -> testBuy(ctx, StringArgumentType.getString(ctx, "gunId")))))
                        .then(Commands.literal("give")
                                .then(Commands.argument("gunId", StringArgumentType.string())
                                        .executes(ctx -> testGive(ctx, StringArgumentType.getString(ctx, "gunId")))))
                        .then(Commands.literal("setbuyzone")
                                .then(Commands.argument("team", StringArgumentType.string())
                                        .executes(ctx -> testSetBuyZone(ctx, StringArgumentType.getString(ctx, "team"))))))
                // === Конфиг ===
                .then(Commands.literal("config")
                        .then(Commands.literal("slots")
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(ctx -> configSlots(ctx, IntegerArgumentType.getInteger(ctx, "amount")))))
                        .then(Commands.literal("kills")
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(ctx -> configKills(ctx, IntegerArgumentType.getInteger(ctx, "amount")))))
                        .then(Commands.literal("clearinv")
                                .then(Commands.argument("value", StringArgumentType.string())
                                        .executes(ctx -> configClearInv(ctx, StringArgumentType.getString(ctx, "value")))))
                        .then(Commands.literal("kept")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("itemId", StringArgumentType.string())
                                                .executes(ctx -> configKeptAdd(ctx, StringArgumentType.getString(ctx, "itemId")))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("itemId", StringArgumentType.string())
                                                .executes(ctx -> configKeptRemove(ctx, StringArgumentType.getString(ctx, "itemId")))))
                                .then(Commands.literal("list").executes(this::configKeptList)))
                        .then(Commands.literal("show").executes(this::configShow)))
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
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs give <player> <gunId>§7 — выдать пушку игроку"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs list guns§7 — список доступных пушек"));
        ctx.getSource().sendSystemMessage(Component.literal("§6§l--- Режимы ---"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs mode <modeId>§7 — установить режим"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs modes§7 — список режимов"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs setmapmode <mapId> <modeId>§7 — привязать карту"));
        ctx.getSource().sendSystemMessage(Component.literal("§6§l--- Редактирование карт ---"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs setlobby§7 — лобби = ваша позиция"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs addmap <id> [name]§7 — создать карту"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs delmap <id>§7 — удалить карту"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs setspawn <mapId> <T|CT>§7 — спавн = ваша позиция"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs clearspawns <mapId> <T|CT>§7 — очистить"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs setbuyzone <mapId> <T|CT>§7 — зона закупа"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs reload§7 — перечитать maps.json"));
        ctx.getSource().sendSystemMessage(Component.literal("§6§l--- Тест ---"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs test phase <LOBBY|BUY_TIME|FIGHTING|ROUND_END>§7"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs test team <T|CT>§7"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs test spawn§7 — телепорт на спавн"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs test setbuyzone <T|CT>§7 — зона покупки вокруг вас"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs test buy <gunId>§7 — купить (с проверкой)"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs test give <gunId>§7 — выдать бесплатно"));
        ctx.getSource().sendSystemMessage(Component.literal("§7Файл карт: §f" + MapConfig.getCurrentFile()));
        ctx.getSource().sendSystemMessage(Component.literal("§7Файл режимов: §f" + ModeConfig.getCurrentFile()));
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
        ctx.getSource().sendSystemMessage(Component.literal("§aMatch started on map: " + mm.getCurrentMapId()
                + " (mode: " + mm.getCurrentModeId() + ")"));
        return 1;
    }

    private int stop(CommandContext<CommandSourceStack> ctx) {
        MatchManager mm = MatchManager.getInstance();
        mm.setPhase(GamePhase.LOBBY);
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
        ctx.getSource().sendSystemMessage(Component.literal("§6Mode: §f" + mm.getCurrentModeId()));
        ctx.getSource().sendSystemMessage(Component.literal("§6Timer: §f" + (mm.getPhaseTicks() / 20) + "s"));
        ctx.getSource().sendSystemMessage(Component.literal("§6Players: §f" + ctx.getSource().getServer().getPlayerList().getPlayerCount()));
        return 1;
    }

    private int listMaps(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.literal("§6§l=== Maps ==="));
        for (MapData m : MapConfig.getMaps().values()) {
            String mode = m.getModeId().isEmpty() ? "any" : m.getModeId();
            ctx.getSource().sendSystemMessage(Component.literal(
                "§e" + m.getId() + " §7(" + m.getDisplayName() + ") §fmode=" + mode
                + " T:" + m.getTSpawns().size() + " CT:" + m.getCtSpawns().size()));
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

    // ====================== Режимы ======================

    /**
     * Админ-команда: выдать пушку конкретному игроку.
     * Эквивалент ручного /give Player tacz:modern_kinetic_gun{GunId:"tacz:..."}
     * Принимает короткие id ("ak47") — автоматически префиксирует "tacz:".
     *
     * Использование: /cs give <player> <gunId>
     */
    private int adminGive(CommandContext<CommandSourceStack> ctx, ServerPlayer target, String gunId) {
        gunId = com.csedition.tacz.TaczHelper.normalizeGunId(gunId);
        // Проверяем что пушка есть в нашей таблице (чтобы не выдавать мусор)
        int price = GunPriceTable.getPrice(gunId);
        if (price < 0) {
            ctx.getSource().sendSystemMessage(Component.literal(
                    "§cUnknown weapon: " + gunId + " (use /cs list guns)"));
            return 0;
        }
        boolean ok = com.csedition.tacz.TaczHelper.giveGun(target, gunId);
        if (ok) {
            ctx.getSource().sendSystemMessage(Component.literal(
                    "§aGiven §e" + gunId + " §ato §e" + target.getName().getString()));
            target.sendSystemMessage(Component.literal(
                    "§aYou received: §e" + gunId));
            return 1;
        } else {
            ctx.getSource().sendSystemMessage(Component.literal(
                    "§cFailed to give " + gunId + " to " + target.getName().getString()
                            + " (inventory full?)"));
            return 0;
        }
    }

    private int setMode(CommandContext<CommandSourceStack> ctx, String modeId) {
        if (ModeConfig.getMode(modeId) == null) {
            ctx.getSource().sendSystemMessage(Component.literal("§cUnknown mode: " + modeId));
            return 0;
        }
        MatchManager.getInstance().setCurrentMode(modeId);
        ctx.getSource().sendSystemMessage(Component.literal("§aMode set to: " + modeId));
        return 1;
    }

    private int listModes(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.literal("§6§l=== Game Modes ==="));
        for (GameMode m : ModeConfig.getModes().values()) {
            String tag = m.isBuiltIn() ? "" : " §7(custom)";
            ctx.getSource().sendSystemMessage(Component.literal(
                "§e" + m.getId() + " §7(" + m.getDisplayName() + ")" + tag
                + " §fbuy=" + m.getBuyTimeSeconds() + "s round=" + m.getRoundTimeSeconds() + "s"
                + " start=$" + m.getStartMoney()));
        }
        return 1;
    }

    /**
     * Список доступных пушек с ценами и категориями.
     */
    private int listGuns(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.literal("§6§l=== Weapons (use /cs test buy <id> or /cs give <player> <id>) ==="));
        String lastCat = null;
        for (Map.Entry<String, Integer> e : GunPriceTable.getAll().entrySet()) {
            String cat = GunPriceTable.getCategory(e.getKey());
            if (!cat.equals(lastCat)) {
                ctx.getSource().sendSystemMessage(Component.literal("§7--- " + cat + " ---"));
                lastCat = cat;
            }
            // Показываем короткий id (без "tacz:" префикса) — для удобства ввода
            String shortId = e.getKey();
            if (shortId.startsWith("tacz:")) shortId = shortId.substring(5);
            ctx.getSource().sendSystemMessage(Component.literal(
                "§e" + shortId + " §7(" + e.getKey() + ") §f$" + e.getValue()));
        }
        return 1;
    }

    private int setMapMode(CommandContext<CommandSourceStack> ctx, String mapId, String modeId) {
        if (MapConfig.getMap(mapId) == null) {
            ctx.getSource().sendSystemMessage(Component.literal("§cUnknown map: " + mapId));
            return 0;
        }
        if (!modeId.isEmpty() && ModeConfig.getMode(modeId) == null) {
            ctx.getSource().sendSystemMessage(Component.literal("§cUnknown mode: " + modeId + " (use empty string to clear)"));
            return 0;
        }
        MapConfig.setMapMode(mapId, modeId);
        broadcastMaps();
        ctx.getSource().sendSystemMessage(Component.literal("§aMap " + mapId + " mode set to: " + (modeId.isEmpty() ? "any" : modeId)));
        return 1;
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
        ModeConfig.load();
        broadcastMaps();
        broadcastModes();
        ctx.getSource().sendSystemMessage(Component.literal("§aMaps and modes reloaded"));
        return 1;
    }

    // ====================== Тестовые команды ======================

    private int testPhase(CommandContext<CommandSourceStack> ctx, String phaseStr) {
        GamePhase phase;
        try {
            phase = GamePhase.valueOf(phaseStr.toUpperCase());
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("§cInvalid phase. Use: LOBBY, BUY_TIME, FIGHTING, ROUND_END"));
            return 0;
        }
        MatchManager.getInstance().setPhase(phase);
        ctx.getSource().sendSystemMessage(Component.literal("§aPhase set to: " + phase));
        return 1;
    }

    private int testTeam(CommandContext<CommandSourceStack> ctx, String teamStr) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Team team = parseTeam(teamStr);
            if (team == null) {
                p.sendSystemMessage(Component.literal("§cTeam must be T or CT"));
                return 0;
            }
            MatchManager mm = MatchManager.getInstance();
            mm.getOrCreate(p).setTeam(team);
            p.sendSystemMessage(Component.literal("§aTeam set to: " + team));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("§cThis command must be run as a player"));
            return 0;
        }
    }

    private int testSpawn(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            MatchManager mm = MatchManager.getInstance();
            MapData map = mm.getCurrentMap();
            if (map == null) {
                p.sendSystemMessage(Component.literal("§cNo map selected"));
                return 0;
            }
            Team team = mm.getOrCreate(p).getTeam();
            if (team == Team.NONE) team = Team.T;
            BlockPos spawn = map.getRandomSpawn(team, new java.util.Random());
            p.teleportTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
            p.sendSystemMessage(Component.literal("§aTeleported to " + team + " spawn: " + spawn));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("§cThis command must be run as a player"));
            return 0;
        }
    }

    // ====================== Тестовые команды магазина ======================

    /**
     * Тестовая покупка: вызывает реальный handleBuyRequest (с проверкой денег, зоны, фазы).
     * Принимает короткие id ("ak47") — автоматически префиксирует "tacz:".
     */
    private int testBuy(CommandContext<CommandSourceStack> ctx, String gunId) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            gunId = com.csedition.tacz.TaczHelper.normalizeGunId(gunId);
            int price = GunPriceTable.getPrice(gunId);
            if (price < 0) {
                p.sendSystemMessage(Component.literal("§cUnknown weapon: " + gunId + " (use /cs test guns for list)"));
                return 0;
            }
            p.sendSystemMessage(Component.literal("§eAttempting to buy " + gunId + " ($" + price + ")..."));
            MatchManager.getInstance().handleBuyRequest(p, gunId);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("§cThis command must be run as a player"));
            return 0;
        }
    }

    /**
     * Тестовая выдача: выдаёт оружие бесплатно, без проверок.
     * Принимает короткие id ("ak47") — автоматически префиксирует "tacz:".
     */
    private int testGive(CommandContext<CommandSourceStack> ctx, String gunId) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            gunId = com.csedition.tacz.TaczHelper.normalizeGunId(gunId);
            com.csedition.tacz.TaczHelper.giveGun(p, gunId);
            p.sendSystemMessage(Component.literal("§aGiven: " + gunId));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("§cFailed: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Тестовая установка зоны закупа вокруг игрока (одна команда).
     * Создаёт зону ±8 блоков по XZ вокруг текущей позиции.
     * Y автоматически ставится от -60 до 222 (полная высота).
     * Удобно для тестирования покупки: встал, прописал — и можно покупать.
     */
    private int testSetBuyZone(CommandContext<CommandSourceStack> ctx, String teamStr) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Team team = parseTeam(teamStr);
            if (team == null) {
                p.sendSystemMessage(Component.literal("§cTeam must be T or CT"));
                return 0;
            }
            MatchManager mm = MatchManager.getInstance();
            MapData map = mm.getCurrentMap();
            if (map == null) {
                p.sendSystemMessage(Component.literal("§cNo map selected. Use /cs start <mapId> first."));
                return 0;
            }
            BlockPos playerPos = p.blockPosition();
            int radius = 8;
            // Y заполнится автоматически в MapConfig.setBuyZone (от -60 до 222)
            BlockPos min = playerPos.offset(-radius, 0, -radius);
            BlockPos max = playerPos.offset(radius, 0, radius);
            MapConfig.setBuyZone(map.getId(), team, min, max);
            broadcastMaps();
            p.sendSystemMessage(Component.literal("§aBuy zone for §e" + team + " §aset around you §7("
                    + (radius * 2 + 1) + "x" + (radius * 2 + 1) + " XZ, full Y)"));
            p.sendSystemMessage(Component.literal("§7You can now buy weapons here. Use /cs test buy <gunId>."));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("§cThis command must be run as a player: " + e.getMessage()));
            return 0;
        }
    }

    // ====================== Конфиг ======================

    private int configSlots(CommandContext<CommandSourceStack> ctx, int amount) {
        CSConfig.setMaxInventorySlots(amount);
        ctx.getSource().sendSystemMessage(Component.literal("§aMax inventory slots set to: " + CSConfig.getMaxInventorySlots()));
        return 1;
    }

    private int configKills(CommandContext<CommandSourceStack> ctx, int amount) {
        CSConfig.setKillsToWin(amount);
        ctx.getSource().sendSystemMessage(Component.literal("§aKills to win set to: " + CSConfig.getKillsToWin()));
        return 1;
    }

    private int configClearInv(CommandContext<CommandSourceStack> ctx, String value) {
        boolean v = value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("on");
        CSConfig.setClearInventoryOnMatchEnd(v);
        ctx.getSource().sendSystemMessage(Component.literal("§aClear inventory on match end: " + CSConfig.isClearInventoryOnMatchEnd()));
        return 1;
    }

    private int configKeptAdd(CommandContext<CommandSourceStack> ctx, String itemId) {
        CSConfig.addKeptItem(itemId);
        ctx.getSource().sendSystemMessage(Component.literal("§aAdded to kept items: " + itemId));
        return 1;
    }

    private int configKeptRemove(CommandContext<CommandSourceStack> ctx, String itemId) {
        CSConfig.removeKeptItem(itemId);
        ctx.getSource().sendSystemMessage(Component.literal("§aRemoved from kept items: " + itemId));
        return 1;
    }

    private int configKeptList(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.literal("§6§l=== Kept Items ==="));
        for (String s : CSConfig.getKeptItems()) {
            ctx.getSource().sendSystemMessage(Component.literal("§e" + s));
        }
        return 1;
    }

    private int configShow(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.literal("§6§l=== CS Config ==="));
        ctx.getSource().sendSystemMessage(Component.literal("§eMax inventory slots: §f" + CSConfig.getMaxInventorySlots()));
        ctx.getSource().sendSystemMessage(Component.literal("§eKills to win: §f" + CSConfig.getKillsToWin()));
        ctx.getSource().sendSystemMessage(Component.literal("§eClear inventory on match end: §f" + CSConfig.isClearInventoryOnMatchEnd()));
        ctx.getSource().sendSystemMessage(Component.literal("§eKept items: §f" + CSConfig.getKeptItems().size()));
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
        List<PacketMapList.MapEntry> entries = new ArrayList<>();
        for (MapData m : MapConfig.getMaps().values()) {
            entries.add(new PacketMapList.MapEntry(m.getId(), m.getDisplayName(), m.getModeId()));
        }
        PacketMapList listPkt = new PacketMapList(entries);
        PacketSyncMaps jsonPkt = new PacketSyncMaps(MapConfig.toJson());
        for (ServerPlayer p : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            CSPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), listPkt);
            CSPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), jsonPkt);
        }
    }

    /**
     * Рассылает обновлённые режимы всем клиентам.
     */
    private void broadcastModes() {
        PacketSyncModes pkt = new PacketSyncModes(ModeConfig.toJson());
        for (ServerPlayer p : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            CSPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), pkt);
        }
    }
}
