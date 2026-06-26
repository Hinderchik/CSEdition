package com.csedition.command;

import com.csedition.config.MapConfig;
import com.csedition.data.GamePhase;
import com.csedition.data.MapData;
import com.csedition.data.PlayerData;
import com.csedition.data.Team;
import com.csedition.match.MatchManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Команда /cs для управления матчем.
 * Работает и на выделенном сервере, и в integrated server (LAN/одиночка).
 *
 * Подкоманды:
 *   /cs start <mapId>  — запустить матч на указанной карте
 *   /cs stop           — остановить матч, вернуть в лобби
 *   /cs status         — показать текущее состояние
 *   /cs maps           — список доступных карт
 *   /cs money <amount> — выдать себе деньги (для теста)
 */
public class CSCommand {

    private static final SuggestionProvider<CommandSourceStack> MAP_SUGGESTIONS = (ctx, builder) -> {
        MapConfig.getMaps().keySet().forEach(builder::suggest);
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("cs")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("start")
                    .then(Commands.argument("mapId", StringArgumentType.string())
                        .suggests(MAP_SUGGESTIONS)
                        .executes(CSCommand::startMatch)))
                .then(Commands.literal("stop")
                    .executes(CSCommand::stopMatch))
                .then(Commands.literal("status")
                    .executes(CSCommand::showStatus))
                .then(Commands.literal("maps")
                    .executes(CSCommand::listMaps))
                .then(Commands.literal("money")
                    .then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                        .executes(CSCommand::giveMoney)))
        );
    }

    private static int startMatch(CommandContext<CommandSourceStack> ctx) {
        String mapId = StringArgumentType.getString(ctx, "mapId");
        MapData map = MapConfig.getMap(mapId);
        if (map == null) {
            ctx.getSource().sendFailure(Component.literal("Map not found: " + mapId));
            return 0;
        }
        MatchManager mm = MatchManager.getInstance();
        // Распределяем команды
        List<ServerPlayer> players = ctx.getSource().getServer().getPlayerList().getPlayers();
        Collections.sort(players, (a, b) -> a.getUUID().compareTo(b.getUUID()));
        for (int i = 0; i < players.size(); i++) {
            ServerPlayer sp = players.get(i);
            PlayerData pd = mm.getOrCreate(sp);
            pd.setTeam(i < players.size() / 2 ? Team.T : Team.CT);
        }
        // Устанавливаем карту и запускаем
        mm.setCurrentMap(mapId);
        mm.startNewRound();
        ctx.getSource().sendSuccess(() -> Component.literal("Match started on " + mapId), true);
        return 1;
    }

    private static int stopMatch(CommandContext<CommandSourceStack> ctx) {
        MatchManager mm = MatchManager.getInstance();
        mm.setPhase(GamePhase.LOBBY);
        for (ServerPlayer sp : ctx.getSource().getServer().getPlayerList().getPlayers()) {
            mm.teleportToLobby(sp);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Match stopped, all returned to lobby"), true);
        return 1;
    }

    private static int showStatus(CommandContext<CommandSourceStack> ctx) {
        MatchManager mm = MatchManager.getInstance();
        String mapId = mm.getCurrentMapId() == null ? "none" : mm.getCurrentMapId();
        ctx.getSource().sendSuccess(() -> Component.literal(
            "Phase: " + mm.getPhase() + " | Map: " + mapId + " | Ticks: " + mm.getPhaseTicks()
        ), false);
        return 1;
    }

    private static int listMaps(CommandContext<CommandSourceStack> ctx) {
        StringBuilder sb = new StringBuilder("Maps: ");
        for (MapData m : MapConfig.getMaps().values()) {
            sb.append(m.getId()).append(" (").append(m.getDisplayName()).append("), ");
        }
        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int giveMoney(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer sp)) {
            ctx.getSource().sendFailure(Component.literal("Only players can use this"));
            return 0;
        }
        int amount = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "amount");
        PlayerData pd = MatchManager.getInstance().getOrCreate(sp);
        pd.addMoney(amount);
        MatchManager.getInstance().sendMoneyUpdate(sp, pd);
        ctx.getSource().sendSuccess(() -> Component.literal("Money set to " + pd.getMoney()), false);
        return 1;
    }
}
