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
 * Main command dispatcher for CS Edition.
 *
 * Match control (/cs start, /cs stop, /cs status, /cs maps, /cs money):
 *   /cs start [mapId]   start a new match on given map (needs 2+ players)
 *   /cs stop            stop current match, return all players to lobby
 *   /cs status          show current match status
 *   /cs maps            list configured maps
 *   /cs money <amount>  add money to your balance
 *
 * Modes (/cs mode, /cs modes, /cs setmapmode):
 *   /cs mode <modeId>   set the current game mode
 *   /cs modes           list available game modes
 *   /cs setmapmode <mapId> <modeId> assign a mode to a map
 *
 * Map management (requires creative-permission player):
 *   /cs setlobby                    set lobby spawn to your position
 *   /cs addmap <id> [name]          add/update a map entry
 *   /cs delmap <id>                 remove a map entry
 *   /cs setspawn <mapId> <T|CT>     add a spawn point at your position
 *   /cs clearspawns <mapId> <T|CT>  clear spawn points for a team
 *   /cs setbuyzone <mapId> <T|CT>   set the buy zone area (2 corners)
 *   /cs reload                     reload maps.json from disk
 *
 * Test commands (op-only, for debugging without a full match):
 *   /cs test phase <phase>  forcibly switch to a game phase
 *   /cs test team <T|CT>    set your team
 *   /cs test spawn          teleport to a random spawn for your team
 *   /cs test money <amount> add money to your balance
 *
 * Config (/cs config):
 *   /cs config slots <n>     set max inventory slots
 *   /cs config kills <n>     set kills to win
 *   /cs config clearinv <bool>  clear inventory on round end
 *   /cs config kept add <itemId>    add to kept items list
 *   /cs config kept remove <itemId> remove from kept items
 *   /cs config kept list            show kept items
 *   /cs config show                 show current config
 *
 * Data files (reload to apply changes):
 *   - Maps: <world>/data/csedition/maps.json
 *   - Modes & weapons: loaded from client via PacketSyncModes / PacketSyncMaps
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
                // Admin: give a weapon directly to a player via TaCZ API
                .then(Commands.literal("give")
                        .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                .then(Commands.argument("gunId", StringArgumentType.string())
                                        .executes(ctx -> adminGive(ctx,
                                                net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "gunId"))))))
                // Admin: give ammo to a player for a specific gun
                .then(Commands.literal("giveammo")
                        .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                .then(Commands.argument("gunId", StringArgumentType.string())
                                        .executes(ctx -> adminGiveAmmo(ctx,
                                                net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "gunId"),
                                                com.csedition.config.WeaponConfig.getMagazineSize(
                                                        com.csedition.tacz.TaczHelper.normalizeGunId(
                                                                StringArgumentType.getString(ctx, "gunId"))) * 3))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 999))
                                                .executes(ctx -> adminGiveAmmo(ctx,
                                                        net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "gunId"),
                                                        IntegerArgumentType.getInteger(ctx, "amount")))))))
                // Modes
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
                // Map management (requires a player with creative permissions)
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
                // Test commands (op-only)
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
                // Config
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

    // ====================== Match management ======================

    private int help(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.literal("§6§l=== CS Edition Commands ==="));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs start [mapId]§7 start a new match on the given map"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs stop§7 stop the current match and return all players to lobby"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs status§7 show current match status"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs maps§7 list configured maps"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs money <amount>§7 add money to your balance"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs give <player> <gunId>§7 give a weapon directly to a player"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs list guns§7 list all configured weapon prices"));
        ctx.getSource().sendSystemMessage(Component.literal("§6§l--- Modes ---"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs mode <modeId>§7 set the current game mode"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs modes§7 list available game modes"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs setmapmode <mapId> <modeId>§7 assign a mode to a map"));
        ctx.getSource().sendSystemMessage(Component.literal("§6§l--- Map management ---"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs setlobby§7 set lobby = your current position"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs addmap <id> [name]§7 add or update a map entry"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs delmap <id>§7 remove a map entry"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs setspawn <mapId> <T|CT>§7 add spawn = your position"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs clearspawns <mapId> <T|CT>§7 clear spawn points for a team"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs setbuyzone <mapId> <T|CT>§7 set the buy zone area (2 corners)"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs reload§7 reload maps.json from disk"));
        ctx.getSource().sendSystemMessage(Component.literal("§6§l--- Test ---"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs test phase <LOBBY|BUY_TIME|FIGHTING|ROUND_END>§7"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs test team <T|CT>§7"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs test spawn§7 teleport to a random spawn for your team"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs test setbuyzone <T|CT>§7 set buy zone around your feet (8 blocks)"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs test buy <gunId>§7 try to buy (requires buy zone)"));
        ctx.getSource().sendSystemMessage(Component.literal("§e/cs test give <gunId>§7 give yourself a weapon"));
        ctx.getSource().sendSystemMessage(Component.literal("§7Map file: §f" + MapConfig.getCurrentFile()));
        ctx.getSource().sendSystemMessage(Component.literal("§7Modes file: §f" + ModeConfig.getCurrentFile()));
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

    // ====================== Modes ======================

    /**
     * Admin: give a weapon to a target player.
     * Tries TaCZ API first (proper GunFireMode string, GunCurrentAmmoCount,
     * attachments are preserved). Falls back to /give if API is unavailable.
     *
     * If the API is not available (missing TaCZ / classloader mismatch between
     * dedicated server and client / obfuscated mappings), we build a manual
     * /give command with the required NBT tags instead of failing completely.
     * This means the weapon always has ammo even when the API path is blocked.
     *
     * Usage: /cs give <player> <gunId>
     */
    private int adminGive(CommandContext<CommandSourceStack> ctx, ServerPlayer target, String gunId) {
        gunId = com.csedition.tacz.TaczHelper.normalizeGunId(gunId);
        int price = GunPriceTable.getPrice(gunId);
        if (price < 0) {
            ctx.getSource().sendSystemMessage(Component.literal(
                    "§cUnknown weapon: " + gunId + " (use /cs list guns)"));
            return 0;
        }
        // 1) Try TaCZ API
        if (com.csedition.tacz.TaczHelper.giveGun(target, gunId)) {
            ctx.getSource().sendSystemMessage(Component.literal(
                    "§aGiven §e" + gunId + " §ato §e" + target.getName().getString()));
            target.sendSystemMessage(Component.literal("§aYou received: §e" + gunId));
            return 1;
        }
        // 2) Fallback: build /give with full NBT tags
        String giveCmd = com.csedition.tacz.TaczHelper.buildGiveCommand(
                target.getName().getString(), gunId);
        if (tryRunGive(ctx, giveCmd)) {
            ctx.getSource().sendSystemMessage(Component.literal(
                    "§aGiven §e" + gunId + " §ato §e" + target.getName().getString()
                            + " §7(via /give fallback)"));
            return 1;
        }
        // 3) Both methods failed
        ctx.getSource().sendSystemMessage(Component.literal(
                "§cCould not give weapon " + gunId + " (check server logs)."));
        ctx.getSource().sendSystemMessage(Component.literal(
                "§eTried manual command: §f" + giveCmd));
        return 0;
    }

    /**
     * Runs a /give command as the command source with suppressed output.
     * Used as a fallback when TaczHelper.giveGun is unavailable.
     */
    private static boolean tryRunGive(CommandContext<CommandSourceStack> ctx, String giveCommand) {
        try {
            var server = ctx.getSource().getServer();
            // Strip leading "/" so performPrefixedCommand doesn't double-prefix it
            String cmd = giveCommand.startsWith("/") ? giveCommand.substring(1) : giveCommand;
            int result = server.getCommands().performPrefixedCommand(
                    ctx.getSource().withSuppressedOutput(), cmd);
            return result > 0;
        } catch (Exception e) {
            CSEditionMod.LOGGER.warn("[CS-Edition] /give fallback failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Admin: give ammo to a target player for a specific gun.
     * Usage: /cs giveammo <player> <gunId> [amount]
     * Default amount = magazineSize * 3 (3 full mags).
     */
    private int adminGiveAmmo(CommandContext<CommandSourceStack> ctx, ServerPlayer target, String gunId, int amount) {
        gunId = com.csedition.tacz.TaczHelper.normalizeGunId(gunId);
        if (GunPriceTable.getPrice(gunId) < 0) {
            ctx.getSource().sendSystemMessage(Component.literal(
                    "§cUnknown weapon: " + gunId + " (use /cs list guns)"));
            return 0;
        }
        if (amount <= 0) {
            ctx.getSource().sendSystemMessage(Component.literal("§cAmount must be > 0"));
            return 0;
        }
        if (com.csedition.tacz.TaczHelper.giveAmmo(target, gunId, amount)) {
            ctx.getSource().sendSystemMessage(Component.literal(
                    "§aGiven §e" + amount + " §aammo for §e" + gunId + " §ato §e" + target.getName().getString()));
            target.sendSystemMessage(Component.literal(
                    "§aYou received: §e" + amount + " §aammo for §e" + gunId));
            return 1;
        }
        ctx.getSource().sendSystemMessage(Component.literal(
                "§cFailed to give ammo (tacz:ammo item missing?)"));
        return 0;
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
     * Display the full weapon price table grouped by category.
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
            // Show only the short id (without "tacz:" prefix) so it's easier to use
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

    // ====================== Map management ======================

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
     * Sets the buy zone for a team on a map. First call: stores your position as min and max.
     * Second call: keeps stored min, sets your position as max. This forms the cuboid.
     * Run the command at the corner with smallest coordinates, then at the opposite corner.
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
        ctx.getSource().sendSystemMessage(Component.literal("Maps, modes, weapons reloaded"));
        return 1;
    }

    // ====================== Test commands (op-only) ======================

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

    // ====================== Test commands - buy zone ======================

    /**
     * Test helper for buy zone: stores player position with normalized min/max.
     * Usage: /cs test buy <gunId> triggers handleBuyRequest (which checks money,
     * zone, ammo). Player id ("ak47") is normalized to add the "tacz:" prefix
     * if it was missing.
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
     * Test helper for buy zone: try to give yourself a weapon, bypassing the
     * normal purchase flow. Falls back to /give with full NBT tags.
     * Player id ("ak47") is normalized to add the "tacz:" prefix if missing.
     * If the API is unavailable we use a manual /give command with required NBT
     * (GunFireMode, GunCurrentAmmoCount, attachments).
     */
    private int testGive(CommandContext<CommandSourceStack> ctx, String gunId) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            gunId = com.csedition.tacz.TaczHelper.normalizeGunId(gunId);
            if (com.csedition.tacz.TaczHelper.giveGun(p, gunId)) {
                p.sendSystemMessage(Component.literal("§aGiven: " + gunId));
                return 1;
            }
            // Fallback: /give with full NBT (GunFireMode, GunCurrentAmmoCount, attachments)
            String giveCmd = com.csedition.tacz.TaczHelper.buildGiveCommand(
                    p.getName().getString(), gunId);
            if (tryRunGive(ctx, giveCmd)) {
                p.sendSystemMessage(Component.literal("§aGiven: " + gunId + " §7(via /give)"));
                return 1;
            }
            p.sendSystemMessage(Component.literal(
                    "§cCould not give weapon " + gunId + " (check server logs)."));
            p.sendSystemMessage(Component.literal(
                    "§eTried manual command: §f" + giveCmd));
            return 0;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("§cFailed: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Test command to set the buy zone for your current team to an 8-block cube
     * around your feet. Y coordinate is ignored and clamped to MapConfig.setBuyZone
     * (which fixes -60 to 222 to keep spawn areas reasonable).
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

    // ====================== Config ======================

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

    // ====================== Helpers ======================

    private Team parseTeam(String s) {
        if (s == null) return null;
        s = s.toUpperCase();
        if (s.equals("T") || s.equals("TERRORIST") || s.equals("TERRORISTS")) return Team.T;
        if (s.equals("CT") || s.equals("COUNTER") || s.equals("COUNTERTERRORIST")) return Team.CT;
        return null;
    }

    /**
     * Broadcasts updated map list to all connected players.
     * Sends both a typed packet (PacketMapList) and a JSON packet (PacketSyncMaps)
     * so old clients and new clients can both render the changes.
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
     * Broadcasts updated mode list to all connected players (JSON form).
     */
    private void broadcastModes() {
        PacketSyncModes pkt = new PacketSyncModes(ModeConfig.toJson());
        for (ServerPlayer p : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            CSPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), pkt);
        }
    }
}
