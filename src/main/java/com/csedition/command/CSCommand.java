package com.csedition.command;

import com.csedition.CSEditionMod;
import com.csedition.config.CSConfig;
import com.csedition.config.MapConfig;
import com.csedition.config.WeaponConfig;
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
import java.util.Map;

/**
 * РљРѕРјР°РЅРґС‹ РјРѕРґР° CS Edition.
 *
 * РЈРїСЂР°РІР»РµРЅРёРµ РјР°С‚С‡РµРј:
 *   /cs start [mapId]   вЂ” Р·Р°РїСѓСЃС‚РёС‚СЊ РјР°С‚С‡ (РјРёРЅРёРјСѓРј 2 РёРіСЂРѕРєР°)
 *   /cs stop            вЂ” РѕСЃС‚Р°РЅРѕРІРёС‚СЊ РјР°С‚С‡
 *   /cs status          вЂ” СЃС‚Р°С‚СѓСЃ РјР°С‚С‡Р°
 *   /cs maps            вЂ” СЃРїРёСЃРѕРє РєР°СЂС‚
 *   /cs money <amount>  вЂ” РІС‹РґР°С‚СЊ РґРµРЅСЊРіРё
 *
 * РЈРїСЂР°РІР»РµРЅРёРµ СЂРµР¶РёРјР°РјРё:
 *   /cs mode <modeId>   вЂ” СѓСЃС‚Р°РЅРѕРІРёС‚СЊ С‚РµРєСѓС‰РёР№ СЂРµР¶РёРј
 *   /cs modes           вЂ” СЃРїРёСЃРѕРє СЂРµР¶РёРјРѕРІ
 *   /cs setmapmode <mapId> <modeId> вЂ” РїСЂРёРІСЏР·Р°С‚СЊ РєР°СЂС‚Сѓ Рє СЂРµР¶РёРјСѓ
 *
 * Р РµРґР°РєС‚РёСЂРѕРІР°РЅРёРµ РєР°СЂС‚ (РЅР° Р»РµС‚Сѓ, Р±РµР· РїРµСЂРµР·Р°РїСѓСЃРєР°):
 *   /cs setlobby                    вЂ” СѓСЃС‚Р°РЅРѕРІРёС‚СЊ Р»РѕР±Р±Рё РЅР° С‚РµРєСѓС‰РµР№ РїРѕР·РёС†РёРё
 *   /cs addmap <id> [name]          вЂ” СЃРѕР·РґР°С‚СЊ/РѕР±РЅРѕРІРёС‚СЊ РєР°СЂС‚Сѓ
 *   /cs delmap <id>                 вЂ” СѓРґР°Р»РёС‚СЊ РєР°СЂС‚Сѓ
 *   /cs setspawn <mapId> <T|CT>     вЂ” РґРѕР±Р°РІРёС‚СЊ СЃРїР°РІРЅ РЅР° С‚РµРєСѓС‰РµР№ РїРѕР·РёС†РёРё
 *   /cs clearspawns <mapId> <T|CT>  вЂ” РѕС‡РёСЃС‚РёС‚СЊ СЃРїР°РІРЅС‹ РєРѕРјР°РЅРґС‹
 *   /cs setbuyzone <mapId> <T|CT>   вЂ” СѓСЃС‚Р°РЅРѕРІРёС‚СЊ Р·РѕРЅСѓ Р·Р°РєСѓРїР° (2 С‚РѕС‡РєРё)
 *   /cs reload                     вЂ” РїРµСЂРµС‡РёС‚Р°С‚СЊ maps.json СЃ РґРёСЃРєР°
 *
 * РўРµСЃС‚РѕРІС‹Рµ РєРѕРјР°РЅРґС‹ (РґР»СЏ РѕС‚Р»Р°РґРєРё):
 *   /cs test phase <phase>  вЂ” РїСЂРёРЅСѓРґРёС‚РµР»СЊРЅРѕ СѓСЃС‚Р°РЅРѕРІРёС‚СЊ С„Р°Р·Сѓ
 *   /cs test team <T|CT>    вЂ” СѓСЃС‚Р°РЅРѕРІРёС‚СЊ РєРѕРјР°РЅРґСѓ
 *   /cs test spawn          вЂ” С‚РµР»РµРїРѕСЂС‚ РЅР° СЃРїР°РІРЅ С‚РµРєСѓС‰РµР№ РєР°СЂС‚С‹
 *   /cs test money <amount> вЂ” РІС‹РґР°С‚СЊ РґРµРЅСЊРіРё
 *
 * Р’СЃРµ РёР·РјРµРЅРµРЅРёСЏ Р°РІС‚РѕРјР°С‚РёС‡РµСЃРєРё:
 *   - РЎРѕС…СЂР°РЅСЏСЋС‚СЃСЏ РІ &lt;world&gt;/data/csedition/maps.json
 *   - Р Р°СЃСЃС‹Р»Р°СЋС‚СЃСЏ РІСЃРµРј РєР»РёРµРЅС‚Р°Рј С‡РµСЂРµР· PacketSyncMaps
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
                // === РђРґРјРёРЅ: РІС‹РґР°С‚СЊ РїСѓС€РєСѓ РєРѕРЅРєСЂРµС‚РЅРѕРјСѓ РёРіСЂРѕРєСѓ ===
                .then(Commands.literal("give")
                        .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                .then(Commands.argument("gunId", StringArgumentType.string())
                                        .executes(ctx -> adminGive(ctx,
                                                net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "gunId"))))))
                // === Р РµР¶РёРјС‹ ===
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
                // === Р РµРґР°РєС‚РёСЂРѕРІР°РЅРёРµ РєР°СЂС‚ ===
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
                // === РўРµСЃС‚РѕРІС‹Рµ РєРѕРјР°РЅРґС‹ ===
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
                // === РљРѕРЅС„РёРі ===
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

    // ====================== РЈРїСЂР°РІР»РµРЅРёРµ РјР°С‚С‡РµРј ======================

    private int help(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.literal("В§6В§l=== CS Edition Commands ==="));
        ctx.getSource().sendSystemMessage(Component.literal("В§e/cs start [mapId]В§7 вЂ” Р·Р°РїСѓСЃС‚РёС‚СЊ РјР°С‚С‡"));
        ctx.getSource().sendSystemMessage(Component.literal("В§e/cs stopВ§7 вЂ” РѕСЃС‚Р°РЅРѕРІРёС‚СЊ РјР°С‚С‡"));
        ctx.getSource().sendSystemMessage(Component.literal("В§e/cs statusВ§7 вЂ” СЃС‚Р°С‚СѓСЃ"));
        ctx.getSource().sendSystemMessage(Component.literal("В§e/cs mapsВ§7 вЂ” СЃРїРёСЃРѕРє РєР°СЂС‚"));
        ctx.getSource().sendSystemMessage(Component.literal("В§e/cs money <amount>В§7 вЂ” РІС‹РґР°С‚СЊ РґРµРЅСЊРіРё"));
        ctx.getSource().sendSystemMessage(Component.literal("В§e/cs give <player> <gunId>В§7 вЂ” РІС‹РґР°С‚СЊ РїСѓС€РєСѓ РёРіСЂРѕРєСѓ"));
        ctx.getSource().sendSystemMessage(Component.literal("В§e/cs list gunsВ§7 вЂ” СЃРїРёСЃРѕРє РґРѕСЃС‚СѓРїРЅС‹С… РїСѓС€РµРє"));
        ctx.getSource().sendSystemMessage(Component.literal("В§6В§l--- Р РµР¶РёРјС‹ ---"));
        ctx.getSource().sendSystemMessage(Component.literal("В§e/cs mode <modeId>В§7 вЂ” СѓСЃС‚Р°РЅРѕРІРёС‚СЊ СЂРµР¶РёРј"));
        ctx.getSource().sendSystemMessage(Component.literal("В§e/cs modesВ§7 вЂ” СЃРїРёСЃРѕРє СЂРµР¶РёРјРѕРІ"));
        ctx.getSource().sendSystemMessage(Component.literal("В§e/cs setmapmode <mapId> <modeId>В§7 вЂ” РїСЂРёРІСЏР·Р°С‚СЊ РєР°СЂС‚Сѓ"));
        ctx.getSource().sendSystemMessage(Component.literal("В§6В§l--- Р РµРґР°РєС‚РёСЂРѕРІР°РЅРёРµ РєР°СЂС‚ ---"));
        ctx.getSource().sendSystemMessage(Component.literal("В§e/cs setlobbyВ§7 вЂ” Р»РѕР±Р±Рё = РІР°С€Р° РїРѕР·РёС†РёСЏ"));
        ctx.getSource().sendSystemMessage(Component.literal("В§e/cs addmap <id> [name]В§7 вЂ” СЃРѕР·РґР°С‚СЊ РєР°СЂС‚Сѓ"));
        ctx.getSource().sendSystemMessage(Component.literal("В§e/cs delmap <id>В§7 вЂ” СѓРґР°Р»РёС‚СЊ РєР°СЂС‚Сѓ"));
        ctx.getSource().sendSystemMessage(Component.literal("В§e/cs setspawn <mapId> <T|CT>В§7 вЂ” СЃРїР°РІРЅ = РІР°С€Р° РїРѕР·РёС†РёСЏ"));
        ctx.getSource().sendSystemMessage(Component.literal("В§e/cs clearspawns <mapId> <T|CT>В§7 вЂ” РѕС‡РёСЃС‚РёС‚СЊ"));
        ctx.getSource().sendSystemMessage(Component.literal("В§e/cs setbuyzone <mapId> <T|CT>В§7 вЂ” Р·РѕРЅР° Р·Р°РєСѓРїР°"));
        ctx.getSource().sendSystemMessage(Component.literal("В§e/cs reloadВ§7 вЂ” РїРµСЂРµС‡РёС‚Р°С‚СЊ maps.json"));
        ctx.getSource().sendSystemMessage(Component.literal("В§6В§l--- РўРµСЃС‚ ---"));
        ctx.getSource().sendSystemMessage(Component.literal("В§e/cs test phase <LOBBY|BUY_TIME|FIGHTING|ROUND_END>В§7"));
        ctx.getSource().sendSystemMessage(Component.literal("В§e/cs test team <T|CT>В§7"));
        ctx.getSource().sendSystemMessage(Component.literal("В§e/cs test spawnВ§7 вЂ” С‚РµР»РµРїРѕСЂС‚ РЅР° СЃРїР°РІРЅ"));
        ctx.getSource().sendSystemMessage(Component.literal("В§e/cs test setbuyzone <T|CT>В§7 вЂ” Р·РѕРЅР° РїРѕРєСѓРїРєРё РІРѕРєСЂСѓРі РІР°СЃ"));
        ctx.getSource().sendSystemMessage(Component.literal("В§e/cs test buy <gunId>В§7 вЂ” РєСѓРїРёС‚СЊ (СЃ РїСЂРѕРІРµСЂРєРѕР№)"));
        ctx.getSource().sendSystemMessage(Component.literal("В§e/cs test give <gunId>В§7 вЂ” РІС‹РґР°С‚СЊ Р±РµСЃРїР»Р°С‚РЅРѕ"));
        ctx.getSource().sendSystemMessage(Component.literal("В§7Р¤Р°Р№Р» РєР°СЂС‚: В§f" + MapConfig.getCurrentFile()));
        ctx.getSource().sendSystemMessage(Component.literal("В§7Р¤Р°Р№Р» СЂРµР¶РёРјРѕРІ: В§f" + ModeConfig.getCurrentFile()));
        return 1;
    }

    private int start(CommandContext<CommandSourceStack> ctx, String mapId) {
        MatchManager mm = MatchManager.getInstance();
        if (mapId != null) {
            if (MapConfig.getMap(mapId) == null) {
                ctx.getSource().sendSystemMessage(Component.literal("В§cUnknown map: " + mapId));
                return 0;
            }
            mm.setCurrentMap(mapId);
        }
        if (mm.getCurrentMapId() == null) {
            ctx.getSource().sendSystemMessage(Component.literal("В§cNo map selected. Use /cs start <mapId>"));
            return 0;
        }
        mm.startNewRound();
        ctx.getSource().sendSystemMessage(Component.literal("В§aMatch started on map: " + mm.getCurrentMapId()
                + " (mode: " + mm.getCurrentModeId() + ")"));
        return 1;
    }

    private int stop(CommandContext<CommandSourceStack> ctx) {
        MatchManager mm = MatchManager.getInstance();
        mm.setPhase(GamePhase.LOBBY);
        for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
            mm.teleportToLobby(p);
        }
        ctx.getSource().sendSystemMessage(Component.literal("В§aMatch stopped. All players returned to lobby."));
        return 1;
    }

    private int status(CommandContext<CommandSourceStack> ctx) {
        MatchManager mm = MatchManager.getInstance();
        ctx.getSource().sendSystemMessage(Component.literal("В§6Phase: В§f" + mm.getPhase()));
        ctx.getSource().sendSystemMessage(Component.literal("В§6Map: В§f" + mm.getCurrentMapId()));
        ctx.getSource().sendSystemMessage(Component.literal("В§6Mode: В§f" + mm.getCurrentModeId()));
        ctx.getSource().sendSystemMessage(Component.literal("В§6Timer: В§f" + (mm.getPhaseTicks() / 20) + "s"));
        ctx.getSource().sendSystemMessage(Component.literal("В§6Players: В§f" + ctx.getSource().getServer().getPlayerList().getPlayerCount()));
        return 1;
    }

    private int listMaps(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.literal("В§6В§l=== Maps ==="));
        for (MapData m : MapConfig.getMaps().values()) {
            String mode = m.getModeId().isEmpty() ? "any" : m.getModeId();
            ctx.getSource().sendSystemMessage(Component.literal(
                "В§e" + m.getId() + " В§7(" + m.getDisplayName() + ") В§fmode=" + mode
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
            p.sendSystemMessage(Component.literal("В§a+$" + amount + " (now $" + pd.getMoney() + ")"));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("В§cThis command must be run as a player"));
            return 0;
        }
    }

    // ====================== Р РµР¶РёРјС‹ ======================

    /**
     * РђРґРјРёРЅ-РєРѕРјР°РЅРґР°: РІС‹РґР°С‚СЊ РїСѓС€РєСѓ РєРѕРЅРєСЂРµС‚РЅРѕРјСѓ РёРіСЂРѕРєСѓ СЃ РїСЂР°РІРёР»СЊРЅС‹Рј NBT.
     * РСЃРїРѕР»СЊР·СѓРµС‚ TaczHelper.giveGun (СЃС‚Р°РІРёС‚ GunFireMode string, GunCurrentAmmoCount,
     * attachments Р°РІС‚РѕРјР°С‚РѕРј).
     *
     * Р•СЃР»Рё РїСЂСЏРјРѕР№ API РЅРµ СЃСЂР°Р±РѕС‚Р°Р» (РЅРµС‚ TaCZ / Р±РёС‚С‹Р№ СЃС‚РµРє / РёРЅРІРµРЅС‚Р°СЂСЊ РїРѕР»РѕРЅ) вЂ”
     * РІС‹РїРѕР»РЅСЏРµС‚ РІР°РЅРёР»СЊРЅС‹Р№ /give СЃ РїРѕР»РЅС‹Рј NBT РѕС‚ РёРјРµРЅРё СЃРµСЂРІРµСЂР°. Р•СЃР»Рё Рё СЌС‚Рѕ РЅРµ
     * РїРѕРјРѕРіР»Рѕ вЂ” РІС‹РІРѕРґРёС‚ С‚РѕС‡РЅСѓСЋ РєРѕРјР°РЅРґСѓ РґР»СЏ СЂСѓС‡РЅРѕРіРѕ РєРѕРїРёСЂРѕРІР°РЅРёСЏ.
     *
     * РСЃРїРѕР»СЊР·РѕРІР°РЅРёРµ: /cs give <player> <gunId>
     */
    private int adminGive(CommandContext<CommandSourceStack> ctx, ServerPlayer target, String gunId) {
        gunId = com.csedition.tacz.TaczHelper.normalizeGunId(gunId);
        int price = GunPriceTable.getPrice(gunId);
        if (price < 0) {
            ctx.getSource().sendSystemMessage(Component.literal(
                    "В§cUnknown weapon: " + gunId + " (use /cs list guns)"));
            return 0;
        }
        // 1) РџРѕРїС‹С‚РєР° С‡РµСЂРµР· РїСЂСЏРјРѕР№ API
        if (com.csedition.tacz.TaczHelper.giveGun(target, gunId)) {
            ctx.getSource().sendSystemMessage(Component.literal(
                    "В§aGiven В§e" + gunId + " В§ato В§e" + target.getName().getString()));
            target.sendSystemMessage(Component.literal("В§aYou received: В§e" + gunId));
            return 1;
        }
        // 2) Р¤РѕР»Р»Р±СЌРє: РІС‹РїРѕР»РЅСЏРµРј /give РѕС‚ СЃРµСЂРІРµСЂР°
        String giveCmd = com.csedition.tacz.TaczHelper.buildGiveCommand(
                target.getName().getString(), gunId);
        if (tryRunGive(ctx, giveCmd)) {
            ctx.getSource().sendSystemMessage(Component.literal(
                    "В§aGiven В§e" + gunId + " В§ato В§e" + target.getName().getString()
                            + " В§7(via /give fallback)"));
            return 1;
        }
        // 3) РЎРѕРІСЃРµРј РїР»РѕС…Рѕ вЂ” РІС‹РІРѕРґРёРј РєРѕРјР°РЅРґСѓ СЂСѓРєР°РјРё
        ctx.getSource().sendSystemMessage(Component.literal(
                "В§cРќРµ СѓРґР°Р»РѕСЃСЊ РІС‹РґР°С‚СЊ " + gunId + " (РёРЅРІРµРЅС‚Р°СЂСЊ РїРѕР»РѕРЅ?)."));
        ctx.getSource().sendSystemMessage(Component.literal(
                "В§eР’С‹РїРѕР»РЅРё РІСЂСѓС‡РЅСѓСЋ: В§f" + giveCmd));
        return 0;
    }

    /**
     * Р’С‹РїРѕР»РЅСЏРµС‚ /give РѕС‚ РёРјРµРЅРё СЃРµСЂРІРµСЂР°.
     * РСЃРїРѕР»СЊР·СѓРµС‚СЃСЏ РєР°Рє fallback РµСЃР»Рё TaczHelper.giveGun РЅРµ СЃРїСЂР°РІРёР»СЃСЏ.
     */
    private static boolean tryRunGive(CommandContext<CommandSourceStack> ctx, String giveCommand) {
        try {
            var server = ctx.getSource().getServer();
            // Strip leading "/" вЂ” performPrefixedCommand РѕР¶РёРґР°РµС‚ РєРѕРјР°РЅРґСѓ Р±РµР· СЃР»РµС€Р°
            String cmd = giveCommand.startsWith("/") ? giveCommand.substring(1) : giveCommand;
            int result = server.getCommands().performPrefixedCommand(
                    ctx.getSource().withSuppressedOutput(), cmd);
            return result > 0;
        } catch (Exception e) {
            CSEditionMod.LOGGER.warn("[CS-Edition] /give fallback failed: {}", e.getMessage());
            return false;
        }
    }

    private int setMode(CommandContext<CommandSourceStack> ctx, String modeId) {
        if (ModeConfig.getMode(modeId) == null) {
            ctx.getSource().sendSystemMessage(Component.literal("В§cUnknown mode: " + modeId));
            return 0;
        }
        MatchManager.getInstance().setCurrentMode(modeId);
        ctx.getSource().sendSystemMessage(Component.literal("В§aMode set to: " + modeId));
        return 1;
    }

    private int listModes(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.literal("В§6В§l=== Game Modes ==="));
        for (GameMode m : ModeConfig.getModes().values()) {
            String tag = m.isBuiltIn() ? "" : " В§7(custom)";
            ctx.getSource().sendSystemMessage(Component.literal(
                "В§e" + m.getId() + " В§7(" + m.getDisplayName() + ")" + tag
                + " В§fbuy=" + m.getBuyTimeSeconds() + "s round=" + m.getRoundTimeSeconds() + "s"
                + " start=$" + m.getStartMoney()));
        }
        return 1;
    }

    /**
     * РЎРїРёСЃРѕРє РґРѕСЃС‚СѓРїРЅС‹С… РїСѓС€РµРє СЃ С†РµРЅР°РјРё Рё РєР°С‚РµРіРѕСЂРёСЏРјРё.
     */
    private int listGuns(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.literal("В§6В§l=== Weapons (use /cs test buy <id> or /cs give <player> <id>) ==="));
        String lastCat = null;
        for (Map.Entry<String, Integer> e : GunPriceTable.getAll().entrySet()) {
            String cat = GunPriceTable.getCategory(e.getKey());
            if (!cat.equals(lastCat)) {
                ctx.getSource().sendSystemMessage(Component.literal("В§7--- " + cat + " ---"));
                lastCat = cat;
            }
            // РџРѕРєР°Р·С‹РІР°РµРј РєРѕСЂРѕС‚РєРёР№ id (Р±РµР· "tacz:" РїСЂРµС„РёРєСЃР°) вЂ” РґР»СЏ СѓРґРѕР±СЃС‚РІР° РІРІРѕРґР°
            String shortId = e.getKey();
            if (shortId.startsWith("tacz:")) shortId = shortId.substring(5);
            ctx.getSource().sendSystemMessage(Component.literal(
                "В§e" + shortId + " В§7(" + e.getKey() + ") В§f$" + e.getValue()));
        }
        return 1;
    }

    private int setMapMode(CommandContext<CommandSourceStack> ctx, String mapId, String modeId) {
        if (MapConfig.getMap(mapId) == null) {
            ctx.getSource().sendSystemMessage(Component.literal("В§cUnknown map: " + mapId));
            return 0;
        }
        if (!modeId.isEmpty() && ModeConfig.getMode(modeId) == null) {
            ctx.getSource().sendSystemMessage(Component.literal("В§cUnknown mode: " + modeId + " (use empty string to clear)"));
            return 0;
        }
        MapConfig.setMapMode(mapId, modeId);
        broadcastMaps();
        ctx.getSource().sendSystemMessage(Component.literal("В§aMap " + mapId + " mode set to: " + (modeId.isEmpty() ? "any" : modeId)));
        return 1;
    }

    // ====================== Р РµРґР°РєС‚РёСЂРѕРІР°РЅРёРµ РєР°СЂС‚ ======================

    private int setLobby(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            BlockPos pos = p.blockPosition();
            MapConfig.setLobbySpawn(pos);
            broadcastMaps();
            p.sendSystemMessage(Component.literal("В§aLobby set to " + pos));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("В§cThis command must be run as a player"));
            return 0;
        }
    }

    private int addMap(CommandContext<CommandSourceStack> ctx, String id, String name) {
        if (name == null) name = id;
        MapConfig.addOrUpdateMap(id, name);
        broadcastMaps();
        ctx.getSource().sendSystemMessage(Component.literal("В§aMap added/updated: " + id + " (" + name + ")"));
        return 1;
    }

    private int delMap(CommandContext<CommandSourceStack> ctx, String id) {
        if (MapConfig.getMap(id) == null) {
            ctx.getSource().sendSystemMessage(Component.literal("В§cUnknown map: " + id));
            return 0;
        }
        MapConfig.deleteMap(id);
        broadcastMaps();
        ctx.getSource().sendSystemMessage(Component.literal("В§aMap deleted: " + id));
        return 1;
    }

    private int addSpawn(CommandContext<CommandSourceStack> ctx, String mapId, String teamStr) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            if (MapConfig.getMap(mapId) == null) {
                p.sendSystemMessage(Component.literal("В§cUnknown map: " + mapId));
                return 0;
            }
            Team team = parseTeam(teamStr);
            if (team == null) {
                p.sendSystemMessage(Component.literal("В§cTeam must be T or CT"));
                return 0;
            }
            MapConfig.addSpawn(mapId, team, p.blockPosition());
            broadcastMaps();
            p.sendSystemMessage(Component.literal("В§aSpawn added for " + team + " on " + mapId + " at " + p.blockPosition()));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("В§cThis command must be run as a player"));
            return 0;
        }
    }

    private int clearSpawns(CommandContext<CommandSourceStack> ctx, String mapId, String teamStr) {
        if (MapConfig.getMap(mapId) == null) {
            ctx.getSource().sendSystemMessage(Component.literal("В§cUnknown map: " + mapId));
            return 0;
        }
        Team team = parseTeam(teamStr);
        if (team == null) {
            ctx.getSource().sendSystemMessage(Component.literal("В§cTeam must be T or CT"));
            return 0;
        }
        MapConfig.clearSpawns(mapId, team);
        broadcastMaps();
        ctx.getSource().sendSystemMessage(Component.literal("В§aSpawns cleared for " + team + " on " + mapId));
        return 1;
    }

    /**
     * РЈСЃС‚Р°РЅРѕРІРєР° Р·РѕРЅС‹ Р·Р°РєСѓРїР°: РїРµСЂРІР°СЏ С‚РѕС‡РєР° вЂ” С‚РµРєСѓС‰Р°СЏ РїРѕР·РёС†РёСЏ, РІС‚РѕСЂР°СЏ вЂ” РїСЂРѕС‚РёРІРѕРїРѕР»РѕР¶РЅС‹Р№ СѓРіРѕР».
     * РРіСЂРѕРє РґРѕР»Р¶РµРЅ РІСЃС‚Р°С‚СЊ РІ РѕРґРёРЅ СѓРіРѕР», РІС‹РїРѕР»РЅРёС‚СЊ РєРѕРјР°РЅРґСѓ, Р·Р°С‚РµРј РІ РґСЂСѓРіРѕР№ СѓРіРѕР» Рё РІС‹РїРѕР»РЅРёС‚СЊ СЃРЅРѕРІР°.
     * Р”Р»СЏ РїСЂРѕСЃС‚РѕС‚С‹: РїРµСЂРІР°СЏ РєРѕРјР°РЅРґР° СѓСЃС‚Р°РЅР°РІР»РёРІР°РµС‚ min, РІС‚РѕСЂР°СЏ вЂ” max.
     */
    private int setBuyZone(CommandContext<CommandSourceStack> ctx, String mapId, String teamStr) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            if (MapConfig.getMap(mapId) == null) {
                p.sendSystemMessage(Component.literal("В§cUnknown map: " + mapId));
                return 0;
            }
            Team team = parseTeam(teamStr);
            if (team == null) {
                p.sendSystemMessage(Component.literal("В§cTeam must be T or CT"));
                return 0;
            }
            MapData m = MapConfig.getMap(mapId);
            BlockPos current = p.blockPosition();
            BlockPos min, max;
            if (team == Team.T) {
                if (m.getTBuyZoneMin().equals(m.getTBuyZoneMax())) {
                    min = current;
                    max = current;
                    p.sendSystemMessage(Component.literal("В§eFirst corner set. В§7Now stand in the opposite corner and run the command again."));
                } else {
                    min = m.getTBuyZoneMin();
                    max = current;
                    p.sendSystemMessage(Component.literal("В§aBuy zone for T set: " + min + " to " + max));
                }
            } else {
                if (m.getCtBuyZoneMin().equals(m.getCtBuyZoneMax())) {
                    min = current;
                    max = current;
                    p.sendSystemMessage(Component.literal("В§eFirst corner set. В§7Now stand in the opposite corner and run the command again."));
                } else {
                    min = m.getCtBuyZoneMin();
                    max = current;
                    p.sendSystemMessage(Component.literal("В§aBuy zone for CT set: " + min + " to " + max));
                }
            }
            MapConfig.setBuyZone(mapId, team, min, max);
            broadcastMaps();
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("В§cThis command must be run as a player"));
            return 0;
        }
    }

    private int reload(CommandContext<CommandSourceStack> ctx) {
        MapConfig.load();
        ModeConfig.load();
        broadcastMaps();
        broadcastModes();
        ctx.getSource().sendSystemMessage(Component.literal("В§aMaps,\ modes,\ weapons\ reloaded"));
        return 1;
    }

    // ====================== РўРµСЃС‚РѕРІС‹Рµ РєРѕРјР°РЅРґС‹ ======================

    private int testPhase(CommandContext<CommandSourceStack> ctx, String phaseStr) {
        GamePhase phase;
        try {
            phase = GamePhase.valueOf(phaseStr.toUpperCase());
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("В§cInvalid phase. Use: LOBBY, BUY_TIME, FIGHTING, ROUND_END"));
            return 0;
        }
        MatchManager.getInstance().setPhase(phase);
        ctx.getSource().sendSystemMessage(Component.literal("В§aPhase set to: " + phase));
        return 1;
    }

    private int testTeam(CommandContext<CommandSourceStack> ctx, String teamStr) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Team team = parseTeam(teamStr);
            if (team == null) {
                p.sendSystemMessage(Component.literal("В§cTeam must be T or CT"));
                return 0;
            }
            MatchManager mm = MatchManager.getInstance();
            mm.getOrCreate(p).setTeam(team);
            p.sendSystemMessage(Component.literal("В§aTeam set to: " + team));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("В§cThis command must be run as a player"));
            return 0;
        }
    }

    private int testSpawn(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            MatchManager mm = MatchManager.getInstance();
            MapData map = mm.getCurrentMap();
            if (map == null) {
                p.sendSystemMessage(Component.literal("В§cNo map selected"));
                return 0;
            }
            Team team = mm.getOrCreate(p).getTeam();
            if (team == Team.NONE) team = Team.T;
            BlockPos spawn = map.getRandomSpawn(team, new java.util.Random());
            p.teleportTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
            p.sendSystemMessage(Component.literal("В§aTeleported to " + team + " spawn: " + spawn));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("В§cThis command must be run as a player"));
            return 0;
        }
    }

    // ====================== РўРµСЃС‚РѕРІС‹Рµ РєРѕРјР°РЅРґС‹ РјР°РіР°Р·РёРЅР° ======================

    /**
     * РўРµСЃС‚РѕРІР°СЏ РїРѕРєСѓРїРєР°: РІС‹Р·С‹РІР°РµС‚ СЂРµР°Р»СЊРЅС‹Р№ handleBuyRequest (СЃ РїСЂРѕРІРµСЂРєРѕР№ РґРµРЅРµРі, Р·РѕРЅС‹, С„Р°Р·С‹).
     * РџСЂРёРЅРёРјР°РµС‚ РєРѕСЂРѕС‚РєРёРµ id ("ak47") вЂ” Р°РІС‚РѕРјР°С‚РёС‡РµСЃРєРё РїСЂРµС„РёРєСЃРёСЂСѓРµС‚ "tacz:".
     */
    private int testBuy(CommandContext<CommandSourceStack> ctx, String gunId) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            gunId = com.csedition.tacz.TaczHelper.normalizeGunId(gunId);
            int price = GunPriceTable.getPrice(gunId);
            if (price < 0) {
                p.sendSystemMessage(Component.literal("В§cUnknown weapon: " + gunId + " (use /cs test guns for list)"));
                return 0;
            }
            p.sendSystemMessage(Component.literal("В§eAttempting to buy " + gunId + " ($" + price + ")..."));
            MatchManager.getInstance().handleBuyRequest(p, gunId);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("В§cThis command must be run as a player"));
            return 0;
        }
    }

    /**
     * РўРµСЃС‚РѕРІР°СЏ РІС‹РґР°С‡Р°: РІС‹РґР°С‘С‚ РѕСЂСѓР¶РёРµ Р±РµСЃРїР»Р°С‚РЅРѕ, Р±РµР· РїСЂРѕРІРµСЂРѕРє.
     * РџСЂРёРЅРёРјР°РµС‚ РєРѕСЂРѕС‚РєРёРµ id ("ak47") вЂ” Р°РІС‚РѕРјР°С‚РёС‡РµСЃРєРё РїСЂРµС„РёРєСЃРёСЂСѓРµС‚ "tacz:".
     * Р•СЃР»Рё API РЅРµ СЃСЂР°Р±РѕС‚Р°Р» вЂ” РІС‹РїРѕР»РЅСЏРµС‚ /give РѕС‚ СЃРµСЂРІРµСЂР° СЃ РїРѕР»РЅС‹Рј NBT.
     */
    private int testGive(CommandContext<CommandSourceStack> ctx, String gunId) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            gunId = com.csedition.tacz.TaczHelper.normalizeGunId(gunId);
            if (com.csedition.tacz.TaczHelper.giveGun(p, gunId)) {
                p.sendSystemMessage(Component.literal("В§aGiven: " + gunId));
                return 1;
            }
            // Р¤РѕР»Р»Р±СЌРє: /give СЃ РїРѕР»РЅС‹Рј NBT (GunFireMode, GunCurrentAmmoCount, attachments)
            String giveCmd = com.csedition.tacz.TaczHelper.buildGiveCommand(
                    p.getName().getString(), gunId);
            if (tryRunGive(ctx, giveCmd)) {
                p.sendSystemMessage(Component.literal("В§aGiven: " + gunId + " В§7(via /give)"));
                return 1;
            }
            p.sendSystemMessage(Component.literal(
                    "В§cРќРµ СѓРґР°Р»РѕСЃСЊ РІС‹РґР°С‚СЊ " + gunId + " (РёРЅРІРµРЅС‚Р°СЂСЊ РїРѕР»РѕРЅ)."));
            p.sendSystemMessage(Component.literal(
                    "В§eР’С‹РїРѕР»РЅРё РІСЂСѓС‡РЅСѓСЋ: В§f" + giveCmd));
            return 0;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("В§cFailed: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * РўРµСЃС‚РѕРІР°СЏ СѓСЃС‚Р°РЅРѕРІРєР° Р·РѕРЅС‹ Р·Р°РєСѓРїР° РІРѕРєСЂСѓРі РёРіСЂРѕРєР° (РѕРґРЅР° РєРѕРјР°РЅРґР°).
     * РЎРѕР·РґР°С‘С‚ Р·РѕРЅСѓ В±8 Р±Р»РѕРєРѕРІ РїРѕ XZ РІРѕРєСЂСѓРі С‚РµРєСѓС‰РµР№ РїРѕР·РёС†РёРё.
     * Y Р°РІС‚РѕРјР°С‚РёС‡РµСЃРєРё СЃС‚Р°РІРёС‚СЃСЏ РѕС‚ -60 РґРѕ 222 (РїРѕР»РЅР°СЏ РІС‹СЃРѕС‚Р°).
     * РЈРґРѕР±РЅРѕ РґР»СЏ С‚РµСЃС‚РёСЂРѕРІР°РЅРёСЏ РїРѕРєСѓРїРєРё: РІСЃС‚Р°Р», РїСЂРѕРїРёСЃР°Р» вЂ” Рё РјРѕР¶РЅРѕ РїРѕРєСѓРїР°С‚СЊ.
     */
    private int testSetBuyZone(CommandContext<CommandSourceStack> ctx, String teamStr) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Team team = parseTeam(teamStr);
            if (team == null) {
                p.sendSystemMessage(Component.literal("В§cTeam must be T or CT"));
                return 0;
            }
            MatchManager mm = MatchManager.getInstance();
            MapData map = mm.getCurrentMap();
            if (map == null) {
                p.sendSystemMessage(Component.literal("В§cNo map selected. Use /cs start <mapId> first."));
                return 0;
            }
            BlockPos playerPos = p.blockPosition();
            int radius = 8;
            // Y Р·Р°РїРѕР»РЅРёС‚СЃСЏ Р°РІС‚РѕРјР°С‚РёС‡РµСЃРєРё РІ MapConfig.setBuyZone (РѕС‚ -60 РґРѕ 222)
            BlockPos min = playerPos.offset(-radius, 0, -radius);
            BlockPos max = playerPos.offset(radius, 0, radius);
            MapConfig.setBuyZone(map.getId(), team, min, max);
            broadcastMaps();
            p.sendSystemMessage(Component.literal("В§aBuy zone for В§e" + team + " В§aset around you В§7("
                    + (radius * 2 + 1) + "x" + (radius * 2 + 1) + " XZ, full Y)"));
            p.sendSystemMessage(Component.literal("В§7You can now buy weapons here. Use /cs test buy <gunId>."));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("В§cThis command must be run as a player: " + e.getMessage()));
            return 0;
        }
    }

    // ====================== РљРѕРЅС„РёРі ======================

    private int configSlots(CommandContext<CommandSourceStack> ctx, int amount) {
        CSConfig.setMaxInventorySlots(amount);
        ctx.getSource().sendSystemMessage(Component.literal("В§aMax inventory slots set to: " + CSConfig.getMaxInventorySlots()));
        return 1;
    }

    private int configKills(CommandContext<CommandSourceStack> ctx, int amount) {
        CSConfig.setKillsToWin(amount);
        ctx.getSource().sendSystemMessage(Component.literal("В§aKills to win set to: " + CSConfig.getKillsToWin()));
        return 1;
    }

    private int configClearInv(CommandContext<CommandSourceStack> ctx, String value) {
        boolean v = value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("on");
        CSConfig.setClearInventoryOnMatchEnd(v);
        ctx.getSource().sendSystemMessage(Component.literal("В§aClear inventory on match end: " + CSConfig.isClearInventoryOnMatchEnd()));
        return 1;
    }

    private int configKeptAdd(CommandContext<CommandSourceStack> ctx, String itemId) {
        CSConfig.addKeptItem(itemId);
        ctx.getSource().sendSystemMessage(Component.literal("В§aAdded to kept items: " + itemId));
        return 1;
    }

    private int configKeptRemove(CommandContext<CommandSourceStack> ctx, String itemId) {
        CSConfig.removeKeptItem(itemId);
        ctx.getSource().sendSystemMessage(Component.literal("В§aRemoved from kept items: " + itemId));
        return 1;
    }

    private int configKeptList(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.literal("В§6В§l=== Kept Items ==="));
        for (String s : CSConfig.getKeptItems()) {
            ctx.getSource().sendSystemMessage(Component.literal("В§e" + s));
        }
        return 1;
    }

    private int configShow(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.literal("В§6В§l=== CS Config ==="));
        ctx.getSource().sendSystemMessage(Component.literal("В§eMax inventory slots: В§f" + CSConfig.getMaxInventorySlots()));
        ctx.getSource().sendSystemMessage(Component.literal("В§eKills to win: В§f" + CSConfig.getKillsToWin()));
        ctx.getSource().sendSystemMessage(Component.literal("В§eClear inventory on match end: В§f" + CSConfig.isClearInventoryOnMatchEnd()));
        ctx.getSource().sendSystemMessage(Component.literal("В§eKept items: В§f" + CSConfig.getKeptItems().size()));
        return 1;
    }

    // ====================== РЈС‚РёР»РёС‚С‹ ======================

    private Team parseTeam(String s) {
        if (s == null) return null;
        s = s.toUpperCase();
        if (s.equals("T") || s.equals("TERRORIST") || s.equals("TERRORISTS")) return Team.T;
        if (s.equals("CT") || s.equals("COUNTER") || s.equals("COUNTERTERRORIST")) return Team.CT;
        return null;
    }

    /**
     * Р Р°СЃСЃС‹Р»Р°РµС‚ РѕР±РЅРѕРІР»С‘РЅРЅС‹Рµ РєР°СЂС‚С‹ РІСЃРµРј РєР»РёРµРЅС‚Р°Рј.
     * Р’С‹Р·С‹РІР°РµС‚СЃСЏ РїРѕСЃР»Рµ Р»СЋР±РѕРіРѕ РёР·РјРµРЅРµРЅРёСЏ.
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
     * Р Р°СЃСЃС‹Р»Р°РµС‚ РѕР±РЅРѕРІР»С‘РЅРЅС‹Рµ СЂРµР¶РёРјС‹ РІСЃРµРј РєР»РёРµРЅС‚Р°Рј.
     */
    private void broadcastModes() {
        PacketSyncModes pkt = new PacketSyncModes(ModeConfig.toJson());
        for (ServerPlayer p : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            CSPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), pkt);
        }
    }
}
