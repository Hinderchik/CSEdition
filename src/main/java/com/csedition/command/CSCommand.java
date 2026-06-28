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
 * Р В Р’В Р РЋРІвЂћСћР В Р’В Р РЋРІР‚СћР В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРІвЂћвЂ“ Р В Р’В Р РЋР’ВР В Р’В Р РЋРІР‚СћР В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В° CS Edition.
 *
 * Р В Р’В Р В РІвЂљВ¬Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В»Р В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚ВР В Р’В Р вЂ™Р’Вµ Р В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р’В Р РЋР’В:
 *   /cs start [mapId]   Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р вЂ™Р’В·Р В Р’В Р вЂ™Р’В°Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р РЋРІР‚СљР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р Р†Р вЂљР Р‹ (Р В Р’В Р РЋР’ВР В Р’В Р РЋРІР‚ВР В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚ВР В Р’В Р РЋР’ВР В Р Р‹Р РЋРІР‚СљР В Р’В Р РЋР’В 2 Р В Р’В Р РЋРІР‚ВР В Р’В Р РЋРІР‚вЂњР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В°)
 *   /cs stop            Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р РЋРІР‚СћР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р Р†Р вЂљР Р‹
 *   /cs status          Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р РЋРІР‚СљР В Р Р‹Р В РЎвЂњ Р В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’В°
 *   /cs maps            Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚ВР В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚Сњ Р В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В°Р В Р Р‹Р В РІР‚С™Р В Р Р‹Р Р†Р вЂљРЎв„ў
 *   /cs money <amount>  Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В¦Р В Р Р‹Р В Р вЂ°Р В Р’В Р РЋРІР‚вЂњР В Р’В Р РЋРІР‚В
 *
 * Р В Р’В Р В РІвЂљВ¬Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В»Р В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚ВР В Р’В Р вЂ™Р’Вµ Р В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’ВµР В Р’В Р вЂ™Р’В¶Р В Р’В Р РЋРІР‚ВР В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р’В Р РЋР’ВР В Р’В Р РЋРІР‚В:
 *   /cs mode <modeId>   Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р Р‹Р РЋРІР‚СљР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р’В Р РЋРІР‚СњР В Р Р‹Р РЋРІР‚СљР В Р Р‹Р Р†Р вЂљР’В°Р В Р’В Р РЋРІР‚ВР В Р’В Р Р†РІР‚С›РІР‚вЂњ Р В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’ВµР В Р’В Р вЂ™Р’В¶Р В Р’В Р РЋРІР‚ВР В Р’В Р РЋР’В
 *   /cs modes           Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚ВР В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚Сњ Р В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’ВµР В Р’В Р вЂ™Р’В¶Р В Р’В Р РЋРІР‚ВР В Р’В Р РЋР’ВР В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В 
 *   /cs setmapmode <mapId> <modeId> Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚ВР В Р’В Р В РІР‚В Р В Р Р‹Р В Р РЏР В Р’В Р вЂ™Р’В·Р В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В°Р В Р Р‹Р В РІР‚С™Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р РЋРІР‚Сљ Р В Р’В Р РЋРІР‚Сњ Р В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’ВµР В Р’В Р вЂ™Р’В¶Р В Р’В Р РЋРІР‚ВР В Р’В Р РЋР’ВР В Р Р‹Р РЋРІР‚Сљ
 *
 * Р В Р’В Р вЂ™Р’В Р В Р’В Р вЂ™Р’ВµР В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°Р В Р’В Р РЋРІР‚СњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚ВР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚ВР В Р’В Р вЂ™Р’Вµ Р В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В°Р В Р Р‹Р В РІР‚С™Р В Р Р‹Р Р†Р вЂљРЎв„ў (Р В Р’В Р В РІР‚В¦Р В Р’В Р вЂ™Р’В° Р В Р’В Р вЂ™Р’В»Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р РЋРІР‚Сљ, Р В Р’В Р вЂ™Р’В±Р В Р’В Р вЂ™Р’ВµР В Р’В Р вЂ™Р’В· Р В Р’В Р РЋРІР‚вЂќР В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’ВµР В Р’В Р вЂ™Р’В·Р В Р’В Р вЂ™Р’В°Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р РЋРІР‚СљР В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В°):
 *   /cs setlobby                    Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р Р‹Р РЋРІР‚СљР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р’В Р вЂ™Р’В»Р В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В±Р В Р’В Р вЂ™Р’В±Р В Р’В Р РЋРІР‚В Р В Р’В Р В РІР‚В¦Р В Р’В Р вЂ™Р’В° Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р’В Р РЋРІР‚СњР В Р Р‹Р РЋРІР‚СљР В Р Р‹Р Р†Р вЂљР’В°Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†РІР‚С›РІР‚вЂњ Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В·Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљР’В Р В Р’В Р РЋРІР‚ВР В Р’В Р РЋРІР‚В
 *   /cs addmap <id> [name]          Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В·Р В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ°/Р В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В±Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В°Р В Р Р‹Р В РІР‚С™Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р РЋРІР‚Сљ
 *   /cs delmap <id>                 Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р Р‹Р РЋРІР‚СљР В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°Р В Р’В Р вЂ™Р’В»Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В°Р В Р Р‹Р В РІР‚С™Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р РЋРІР‚Сљ
 *   /cs setspawn <mapId> <T|CT>     Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р СћРІР‚ВР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В±Р В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚вЂќР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В Р В Р’В Р В РІР‚В¦ Р В Р’В Р В РІР‚В¦Р В Р’В Р вЂ™Р’В° Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р’В Р РЋРІР‚СњР В Р Р‹Р РЋРІР‚СљР В Р Р‹Р Р†Р вЂљР’В°Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†РІР‚С›РІР‚вЂњ Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В·Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљР’В Р В Р’В Р РЋРІР‚ВР В Р’В Р РЋРІР‚В
 *   /cs clearspawns <mapId> <T|CT>  Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р РЋРІР‚СћР В Р Р‹Р Р†Р вЂљР Р‹Р В Р’В Р РЋРІР‚ВР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚вЂќР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В Р В Р’В Р В РІР‚В¦Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“ Р В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚СћР В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРІвЂћвЂ“
 *   /cs setbuyzone <mapId> <T|CT>   Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р Р‹Р РЋРІР‚СљР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р’В Р вЂ™Р’В·Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В¦Р В Р Р‹Р РЋРІР‚Сљ Р В Р’В Р вЂ™Р’В·Р В Р’В Р вЂ™Р’В°Р В Р’В Р РЋРІР‚СњР В Р Р‹Р РЋРІР‚СљР В Р’В Р РЋРІР‚вЂќР В Р’В Р вЂ™Р’В° (2 Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СћР В Р Р‹Р Р†Р вЂљР Р‹Р В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚В)
 *   /cs reload                     Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р РЋРІР‚вЂќР В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљР Р‹Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° maps.json Р В Р Р‹Р В РЎвЂњ Р В Р’В Р СћРІР‚ВР В Р’В Р РЋРІР‚ВР В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В°
 *
 * Р В Р’В Р РЋРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’Вµ Р В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚СћР В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРІвЂћвЂ“ (Р В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В»Р В Р Р‹Р В Р РЏ Р В Р’В Р РЋРІР‚СћР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В»Р В Р’В Р вЂ™Р’В°Р В Р’В Р СћРІР‚ВР В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚В):
 *   /cs test phase <phase>  Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚ВР В Р’В Р В РІР‚В¦Р В Р Р‹Р РЋРІР‚СљР В Р’В Р СћРІР‚ВР В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р’В Р вЂ™Р’В»Р В Р Р‹Р В Р вЂ°Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚Сћ Р В Р Р‹Р РЋРІР‚СљР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р Р‹Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’В°Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р РЋРІР‚Сљ
 *   /cs test team <T|CT>    Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р Р‹Р РЋРІР‚СљР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚СћР В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р СћРІР‚ВР В Р Р‹Р РЋРІР‚Сљ
 *   /cs test spawn          Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р’В Р вЂ™Р’В»Р В Р’В Р вЂ™Р’ВµР В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р Р‹Р В РІР‚С™Р В Р Р‹Р Р†Р вЂљРЎв„ў Р В Р’В Р В РІР‚В¦Р В Р’В Р вЂ™Р’В° Р В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚вЂќР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В Р В Р’В Р В РІР‚В¦ Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р’В Р РЋРІР‚СњР В Р Р‹Р РЋРІР‚СљР В Р Р‹Р Р†Р вЂљР’В°Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†РІР‚С›РІР‚вЂњ Р В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В°Р В Р Р‹Р В РІР‚С™Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р Р†Р вЂљРІвЂћвЂ“
 *   /cs test money <amount> Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В¦Р В Р Р‹Р В Р вЂ°Р В Р’В Р РЋРІР‚вЂњР В Р’В Р РЋРІР‚В
 *
 * Р В Р’В Р Р†Р вЂљРІвЂћСћР В Р Р‹Р В РЎвЂњР В Р’В Р вЂ™Р’Вµ Р В Р’В Р РЋРІР‚ВР В Р’В Р вЂ™Р’В·Р В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В¦Р В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚ВР В Р Р‹Р В Р РЏ Р В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СћР В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚В:
 *   - Р В Р’В Р В Р вЂ№Р В Р’В Р РЋРІР‚СћР В Р Р‹Р Р†Р вЂљР’В¦Р В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р Р‹Р В Р РЏР В Р Р‹Р В РІР‚в„–Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В РЎвЂњР В Р Р‹Р В Р РЏ Р В Р’В Р В РІР‚В  &lt;world&gt;/data/csedition/maps.json
 *   - Р В Р’В Р вЂ™Р’В Р В Р’В Р вЂ™Р’В°Р В Р Р‹Р В РЎвЂњР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’В»Р В Р’В Р вЂ™Р’В°Р В Р Р‹Р В РІР‚в„–Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В РЎвЂњР В Р Р‹Р В Р РЏ Р В Р’В Р В РІР‚В Р В Р Р‹Р В РЎвЂњР В Р’В Р вЂ™Р’ВµР В Р’В Р РЋР’В Р В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В»Р В Р’В Р РЋРІР‚ВР В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В¦Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р’В Р РЋР’В Р В Р Р‹Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’ВµР В Р’В Р вЂ™Р’В· PacketSyncMaps
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
                // === Р В Р’В Р РЋРІР‚в„ўР В Р’В Р СћРІР‚ВР В Р’В Р РЋР’ВР В Р’В Р РЋРІР‚ВР В Р’В Р В РІР‚В¦: Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р РЋРІР‚СљР В Р Р‹Р Р†РІР‚С™Р’В¬Р В Р’В Р РЋРІР‚СњР В Р Р‹Р РЋРІР‚Сљ Р В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚СњР В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚СћР В Р’В Р РЋР’ВР В Р Р‹Р РЋРІР‚Сљ Р В Р’В Р РЋРІР‚ВР В Р’В Р РЋРІР‚вЂњР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚СњР В Р Р‹Р РЋРІР‚Сљ ===
                .then(Commands.literal("give")
                        .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                .then(Commands.argument("gunId", StringArgumentType.string())
                                        .executes(ctx -> adminGive(ctx,
                                                net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "gunId"))))))
                // === Р В Р’В Р вЂ™Р’В Р В Р’В Р вЂ™Р’ВµР В Р’В Р вЂ™Р’В¶Р В Р’В Р РЋРІР‚ВР В Р’В Р РЋР’ВР В Р Р‹Р Р†Р вЂљРІвЂћвЂ“ ===
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
                // === Р В Р’В Р вЂ™Р’В Р В Р’В Р вЂ™Р’ВµР В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°Р В Р’В Р РЋРІР‚СњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚ВР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚ВР В Р’В Р вЂ™Р’Вµ Р В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В°Р В Р Р‹Р В РІР‚С™Р В Р Р‹Р Р†Р вЂљРЎв„ў ===
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
                // === Р В Р’В Р РЋРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’Вµ Р В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚СћР В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРІвЂћвЂ“ ===
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
                // === Р В Р’В Р РЋРІвЂћСћР В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В¦Р В Р Р‹Р Р†Р вЂљРЎвЂєР В Р’В Р РЋРІР‚ВР В Р’В Р РЋРІР‚вЂњ ===
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

    // ====================== Р В Р’В Р В РІвЂљВ¬Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В»Р В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚ВР В Р’В Р вЂ™Р’Вµ Р В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р’В Р РЋР’В ======================

    private int help(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§6Р В РІР‚в„ўР вЂ™Р’В§l=== CS Edition Commands ==="));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§e/cs start [mapId]Р В РІР‚в„ўР вЂ™Р’В§7 Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р вЂ™Р’В·Р В Р’В Р вЂ™Р’В°Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р РЋРІР‚СљР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р Р†Р вЂљР Р‹"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§e/cs stopР В РІР‚в„ўР вЂ™Р’В§7 Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р РЋРІР‚СћР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р Р†Р вЂљР Р‹"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§e/cs statusР В РІР‚в„ўР вЂ™Р’В§7 Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р РЋРІР‚СљР В Р Р‹Р В РЎвЂњ"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§e/cs mapsР В РІР‚в„ўР вЂ™Р’В§7 Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚ВР В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚Сњ Р В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В°Р В Р Р‹Р В РІР‚С™Р В Р Р‹Р Р†Р вЂљРЎв„ў"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§e/cs money <amount>Р В РІР‚в„ўР вЂ™Р’В§7 Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В¦Р В Р Р‹Р В Р вЂ°Р В Р’В Р РЋРІР‚вЂњР В Р’В Р РЋРІР‚В"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§e/cs give <player> <gunId>Р В РІР‚в„ўР вЂ™Р’В§7 Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р РЋРІР‚СљР В Р Р‹Р Р†РІР‚С™Р’В¬Р В Р’В Р РЋРІР‚СњР В Р Р‹Р РЋРІР‚Сљ Р В Р’В Р РЋРІР‚ВР В Р’В Р РЋРІР‚вЂњР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚СњР В Р Р‹Р РЋРІР‚Сљ"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§e/cs list gunsР В РІР‚в„ўР вЂ™Р’В§7 Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚ВР В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚Сњ Р В Р’В Р СћРІР‚ВР В Р’В Р РЋРІР‚СћР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р РЋРІР‚СљР В Р’В Р РЋРІР‚вЂќР В Р’В Р В РІР‚В¦Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р Р‹Р Р†Р вЂљР’В¦ Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р РЋРІР‚СљР В Р Р‹Р Р†РІР‚С™Р’В¬Р В Р’В Р вЂ™Р’ВµР В Р’В Р РЋРІР‚Сњ"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§6Р В РІР‚в„ўР вЂ™Р’В§l--- Р В Р’В Р вЂ™Р’В Р В Р’В Р вЂ™Р’ВµР В Р’В Р вЂ™Р’В¶Р В Р’В Р РЋРІР‚ВР В Р’В Р РЋР’ВР В Р Р‹Р Р†Р вЂљРІвЂћвЂ“ ---"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§e/cs mode <modeId>Р В РІР‚в„ўР вЂ™Р’В§7 Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р Р‹Р РЋРІР‚СљР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’ВµР В Р’В Р вЂ™Р’В¶Р В Р’В Р РЋРІР‚ВР В Р’В Р РЋР’В"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§e/cs modesР В РІР‚в„ўР вЂ™Р’В§7 Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚ВР В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚Сњ Р В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’ВµР В Р’В Р вЂ™Р’В¶Р В Р’В Р РЋРІР‚ВР В Р’В Р РЋР’ВР В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В "));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§e/cs setmapmode <mapId> <modeId>Р В РІР‚в„ўР вЂ™Р’В§7 Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚ВР В Р’В Р В РІР‚В Р В Р Р‹Р В Р РЏР В Р’В Р вЂ™Р’В·Р В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В°Р В Р Р‹Р В РІР‚С™Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р РЋРІР‚Сљ"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§6Р В РІР‚в„ўР вЂ™Р’В§l--- Р В Р’В Р вЂ™Р’В Р В Р’В Р вЂ™Р’ВµР В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°Р В Р’В Р РЋРІР‚СњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚ВР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚ВР В Р’В Р вЂ™Р’Вµ Р В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В°Р В Р Р‹Р В РІР‚С™Р В Р Р‹Р Р†Р вЂљРЎв„ў ---"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§e/cs setlobbyР В РІР‚в„ўР вЂ™Р’В§7 Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р вЂ™Р’В»Р В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В±Р В Р’В Р вЂ™Р’В±Р В Р’В Р РЋРІР‚В = Р В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†РІР‚С™Р’В¬Р В Р’В Р вЂ™Р’В° Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В·Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљР’В Р В Р’В Р РЋРІР‚ВР В Р Р‹Р В Р РЏ"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§e/cs addmap <id> [name]Р В РІР‚в„ўР вЂ™Р’В§7 Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В·Р В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В°Р В Р Р‹Р В РІР‚С™Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р РЋРІР‚Сљ"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§e/cs delmap <id>Р В РІР‚в„ўР вЂ™Р’В§7 Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р Р‹Р РЋРІР‚СљР В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°Р В Р’В Р вЂ™Р’В»Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В°Р В Р Р‹Р В РІР‚С™Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р РЋРІР‚Сљ"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§e/cs setspawn <mapId> <T|CT>Р В РІР‚в„ўР вЂ™Р’В§7 Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚вЂќР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В Р В Р’В Р В РІР‚В¦ = Р В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†РІР‚С™Р’В¬Р В Р’В Р вЂ™Р’В° Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В·Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљР’В Р В Р’В Р РЋРІР‚ВР В Р Р‹Р В Р РЏ"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§e/cs clearspawns <mapId> <T|CT>Р В РІР‚в„ўР вЂ™Р’В§7 Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р РЋРІР‚СћР В Р Р‹Р Р†Р вЂљР Р‹Р В Р’В Р РЋРІР‚ВР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ°"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§e/cs setbuyzone <mapId> <T|CT>Р В РІР‚в„ўР вЂ™Р’В§7 Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р вЂ™Р’В·Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В¦Р В Р’В Р вЂ™Р’В° Р В Р’В Р вЂ™Р’В·Р В Р’В Р вЂ™Р’В°Р В Р’В Р РЋРІР‚СњР В Р Р‹Р РЋРІР‚СљР В Р’В Р РЋРІР‚вЂќР В Р’В Р вЂ™Р’В°"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§e/cs reloadР В РІР‚в„ўР вЂ™Р’В§7 Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р РЋРІР‚вЂќР В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљР Р‹Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° maps.json"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§6Р В РІР‚в„ўР вЂ™Р’В§l--- Р В Р’В Р РЋРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ў ---"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§e/cs test phase <LOBBY|BUY_TIME|FIGHTING|ROUND_END>Р В РІР‚в„ўР вЂ™Р’В§7"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§e/cs test team <T|CT>Р В РІР‚в„ўР вЂ™Р’В§7"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§e/cs test spawnР В РІР‚в„ўР вЂ™Р’В§7 Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р’В Р вЂ™Р’В»Р В Р’В Р вЂ™Р’ВµР В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р Р‹Р В РІР‚С™Р В Р Р‹Р Р†Р вЂљРЎв„ў Р В Р’В Р В РІР‚В¦Р В Р’В Р вЂ™Р’В° Р В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚вЂќР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В Р В Р’В Р В РІР‚В¦"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§e/cs test setbuyzone <T|CT>Р В РІР‚в„ўР вЂ™Р’В§7 Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р вЂ™Р’В·Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В¦Р В Р’В Р вЂ™Р’В° Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚СњР В Р Р‹Р РЋРІР‚СљР В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚В Р В Р’В Р В РІР‚В Р В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚СњР В Р Р‹Р В РІР‚С™Р В Р Р‹Р РЋРІР‚СљР В Р’В Р РЋРІР‚вЂњ Р В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В°Р В Р Р‹Р В РЎвЂњ"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§e/cs test buy <gunId>Р В РІР‚в„ўР вЂ™Р’В§7 Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р РЋРІР‚СњР В Р Р‹Р РЋРІР‚СљР В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° (Р В Р Р‹Р В РЎвЂњ Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚СћР В Р’В Р Р†РІР‚С›РІР‚вЂњ)"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§e/cs test give <gunId>Р В РІР‚в„ўР вЂ™Р’В§7 Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р’В Р вЂ™Р’В±Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚вЂќР В Р’В Р вЂ™Р’В»Р В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚Сћ"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§7Р В Р’В Р вЂ™Р’В¤Р В Р’В Р вЂ™Р’В°Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’В» Р В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В°Р В Р Р‹Р В РІР‚С™Р В Р Р‹Р Р†Р вЂљРЎв„ў: Р В РІР‚в„ўР вЂ™Р’В§f" + MapConfig.getCurrentFile()));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§7Р В Р’В Р вЂ™Р’В¤Р В Р’В Р вЂ™Р’В°Р В Р’В Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’В» Р В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’ВµР В Р’В Р вЂ™Р’В¶Р В Р’В Р РЋРІР‚ВР В Р’В Р РЋР’ВР В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В : Р В РІР‚в„ўР вЂ™Р’В§f" + ModeConfig.getCurrentFile()));
        return 1;
    }

    private int start(CommandContext<CommandSourceStack> ctx, String mapId) {
        MatchManager mm = MatchManager.getInstance();
        if (mapId != null) {
            if (MapConfig.getMap(mapId) == null) {
                ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cUnknown map: " + mapId));
                return 0;
            }
            mm.setCurrentMap(mapId);
        }
        if (mm.getCurrentMapId() == null) {
            ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cNo map selected. Use /cs start <mapId>"));
            return 0;
        }
        mm.startNewRound();
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§aMatch started on map: " + mm.getCurrentMapId()
                + " (mode: " + mm.getCurrentModeId() + ")"));
        return 1;
    }

    private int stop(CommandContext<CommandSourceStack> ctx) {
        MatchManager mm = MatchManager.getInstance();
        mm.setPhase(GamePhase.LOBBY);
        for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
            mm.teleportToLobby(p);
        }
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§aMatch stopped. All players returned to lobby."));
        return 1;
    }

    private int status(CommandContext<CommandSourceStack> ctx) {
        MatchManager mm = MatchManager.getInstance();
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§6Phase: Р В РІР‚в„ўР вЂ™Р’В§f" + mm.getPhase()));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§6Map: Р В РІР‚в„ўР вЂ™Р’В§f" + mm.getCurrentMapId()));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§6Mode: Р В РІР‚в„ўР вЂ™Р’В§f" + mm.getCurrentModeId()));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§6Timer: Р В РІР‚в„ўР вЂ™Р’В§f" + (mm.getPhaseTicks() / 20) + "s"));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§6Players: Р В РІР‚в„ўР вЂ™Р’В§f" + ctx.getSource().getServer().getPlayerList().getPlayerCount()));
        return 1;
    }

    private int listMaps(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§6Р В РІР‚в„ўР вЂ™Р’В§l=== Maps ==="));
        for (MapData m : MapConfig.getMaps().values()) {
            String mode = m.getModeId().isEmpty() ? "any" : m.getModeId();
            ctx.getSource().sendSystemMessage(Component.literal(
                "Р В РІР‚в„ўР вЂ™Р’В§e" + m.getId() + " Р В РІР‚в„ўР вЂ™Р’В§7(" + m.getDisplayName() + ") Р В РІР‚в„ўР вЂ™Р’В§fmode=" + mode
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
            p.sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§a+$" + amount + " (now $" + pd.getMoney() + ")"));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cThis command must be run as a player"));
            return 0;
        }
    }

    // ====================== Р В Р’В Р вЂ™Р’В Р В Р’В Р вЂ™Р’ВµР В Р’В Р вЂ™Р’В¶Р В Р’В Р РЋРІР‚ВР В Р’В Р РЋР’ВР В Р Р‹Р Р†Р вЂљРІвЂћвЂ“ ======================

    /**
     * Р В Р’В Р РЋРІР‚в„ўР В Р’В Р СћРІР‚ВР В Р’В Р РЋР’ВР В Р’В Р РЋРІР‚ВР В Р’В Р В РІР‚В¦-Р В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚СћР В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°: Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р РЋРІР‚СљР В Р Р‹Р Р†РІР‚С™Р’В¬Р В Р’В Р РЋРІР‚СњР В Р Р‹Р РЋРІР‚Сљ Р В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚СњР В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚СћР В Р’В Р РЋР’ВР В Р Р‹Р РЋРІР‚Сљ Р В Р’В Р РЋРІР‚ВР В Р’В Р РЋРІР‚вЂњР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚СњР В Р Р‹Р РЋРІР‚Сљ Р В Р Р‹Р В РЎвЂњ Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В Р В Р’В Р РЋРІР‚ВР В Р’В Р вЂ™Р’В»Р В Р Р‹Р В Р вЂ°Р В Р’В Р В РІР‚В¦Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р РЋР’В NBT.
     * Р В Р’В Р вЂ™Р’ВР В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В»Р В Р Р‹Р В Р вЂ°Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р РЋРІР‚СљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎв„ў TaczHelper.giveGun (Р В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ў GunFireMode string, GunCurrentAmmoCount,
     * attachments Р В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СћР В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СћР В Р’В Р РЋР’В).
     *
     * Р В Р’В Р Р†Р вЂљРЎС›Р В Р Р‹Р В РЎвЂњР В Р’В Р вЂ™Р’В»Р В Р’В Р РЋРІР‚В Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р В РІР‚С™Р В Р Р‹Р В Р РЏР В Р’В Р РЋР’ВР В Р’В Р РЋРІР‚СћР В Р’В Р Р†РІР‚С›РІР‚вЂњ API Р В Р’В Р В РІР‚В¦Р В Р’В Р вЂ™Р’Вµ Р В Р Р‹Р В РЎвЂњР В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’В°Р В Р’В Р вЂ™Р’В±Р В Р’В Р РЋРІР‚СћР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р’В Р вЂ™Р’В» (Р В Р’В Р В РІР‚В¦Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎв„ў TaCZ / Р В Р’В Р вЂ™Р’В±Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р Р†РІР‚С›РІР‚вЂњ Р В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р’В Р РЋРІР‚Сњ / Р В Р’В Р РЋРІР‚ВР В Р’В Р В РІР‚В¦Р В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В¦Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р Р‹Р В РІР‚С™Р В Р Р‹Р В Р вЂ° Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В»Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В¦) Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ
     * Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В»Р В Р’В Р В РІР‚В¦Р В Р Р‹Р В Р РЏР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎв„ў Р В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚ВР В Р’В Р вЂ™Р’В»Р В Р Р‹Р В Р вЂ°Р В Р’В Р В РІР‚В¦Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р Р†РІР‚С›РІР‚вЂњ /give Р В Р Р‹Р В РЎвЂњ Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В»Р В Р’В Р В РІР‚В¦Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р РЋР’В NBT Р В Р’В Р РЋРІР‚СћР В Р Р‹Р Р†Р вЂљРЎв„ў Р В Р’В Р РЋРІР‚ВР В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚В Р В Р Р‹Р В РЎвЂњР В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РІР‚С™Р В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’В°. Р В Р’В Р Р†Р вЂљРЎС›Р В Р Р‹Р В РЎвЂњР В Р’В Р вЂ™Р’В»Р В Р’В Р РЋРІР‚В Р В Р’В Р РЋРІР‚В Р В Р Р‹Р В Р Р‰Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚Сћ Р В Р’В Р В РІР‚В¦Р В Р’В Р вЂ™Р’Вµ
     * Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р РЋР’ВР В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚вЂњР В Р’В Р вЂ™Р’В»Р В Р’В Р РЋРІР‚Сћ Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р В РІР‚В Р В Р’В Р РЋРІР‚СћР В Р’В Р СћРІР‚ВР В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ў Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СћР В Р Р‹Р Р†Р вЂљР Р‹Р В Р’В Р В РІР‚В¦Р В Р Р‹Р РЋРІР‚СљР В Р Р‹Р В РІР‚в„– Р В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚СћР В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р СћРІР‚ВР В Р Р‹Р РЋРІР‚Сљ Р В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В»Р В Р Р‹Р В Р РЏ Р В Р Р‹Р В РІР‚С™Р В Р Р‹Р РЋРІР‚СљР В Р Р‹Р Р†Р вЂљР Р‹Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚вЂњР В Р’В Р РЋРІР‚Сћ Р В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚ВР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚ВР В Р Р‹Р В Р РЏ.
     *
     * Р В Р’В Р вЂ™Р’ВР В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В»Р В Р Р‹Р В Р вЂ°Р В Р’В Р вЂ™Р’В·Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚ВР В Р’В Р вЂ™Р’Вµ: /cs give <player> <gunId>
     */
    private int adminGive(CommandContext<CommandSourceStack> ctx, ServerPlayer target, String gunId) {
        gunId = com.csedition.tacz.TaczHelper.normalizeGunId(gunId);
        int price = GunPriceTable.getPrice(gunId);
        if (price < 0) {
            ctx.getSource().sendSystemMessage(Component.literal(
                    "Р В РІР‚в„ўР вЂ™Р’В§cUnknown weapon: " + gunId + " (use /cs list guns)"));
            return 0;
        }
        // 1) Р В Р’В Р РЋРЎСџР В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В° Р В Р Р‹Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’ВµР В Р’В Р вЂ™Р’В· Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р В РІР‚С™Р В Р Р‹Р В Р РЏР В Р’В Р РЋР’ВР В Р’В Р РЋРІР‚СћР В Р’В Р Р†РІР‚С›РІР‚вЂњ API
        if (com.csedition.tacz.TaczHelper.giveGun(target, gunId)) {
            ctx.getSource().sendSystemMessage(Component.literal(
                    "Р В РІР‚в„ўР вЂ™Р’В§aGiven Р В РІР‚в„ўР вЂ™Р’В§e" + gunId + " Р В РІР‚в„ўР вЂ™Р’В§ato Р В РІР‚в„ўР вЂ™Р’В§e" + target.getName().getString()));
            target.sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§aYou received: Р В РІР‚в„ўР вЂ™Р’В§e" + gunId));
            return 1;
        }
        // 2) Р В Р’В Р вЂ™Р’В¤Р В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В»Р В Р’В Р вЂ™Р’В»Р В Р’В Р вЂ™Р’В±Р В Р Р‹Р В Р Р‰Р В Р’В Р РЋРІР‚Сњ: Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В»Р В Р’В Р В РІР‚В¦Р В Р Р‹Р В Р РЏР В Р’В Р вЂ™Р’ВµР В Р’В Р РЋР’В /give Р В Р’В Р РЋРІР‚СћР В Р Р‹Р Р†Р вЂљРЎв„ў Р В Р Р‹Р В РЎвЂњР В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РІР‚С™Р В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’В°
        String giveCmd = com.csedition.tacz.TaczHelper.buildGiveCommand(
                target.getName().getString(), gunId);
        if (tryRunGive(ctx, giveCmd)) {
            ctx.getSource().sendSystemMessage(Component.literal(
                    "Р В РІР‚в„ўР вЂ™Р’В§aGiven Р В РІР‚в„ўР вЂ™Р’В§e" + gunId + " Р В РІР‚в„ўР вЂ™Р’В§ato Р В РІР‚в„ўР вЂ™Р’В§e" + target.getName().getString()
                            + " Р В РІР‚в„ўР вЂ™Р’В§7(via /give fallback)"));
            return 1;
        }
        // 3) Р В Р’В Р В Р вЂ№Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р Р‹Р В РЎвЂњР В Р’В Р вЂ™Р’ВµР В Р’В Р РЋР’В Р В Р’В Р РЋРІР‚вЂќР В Р’В Р вЂ™Р’В»Р В Р’В Р РЋРІР‚СћР В Р Р‹Р Р†Р вЂљР’В¦Р В Р’В Р РЋРІР‚Сћ Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р В РІР‚В Р В Р’В Р РЋРІР‚СћР В Р’В Р СћРІР‚ВР В Р’В Р РЋРІР‚ВР В Р’В Р РЋР’В Р В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚СћР В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р СћРІР‚ВР В Р Р‹Р РЋРІР‚Сљ Р В Р Р‹Р В РІР‚С™Р В Р Р‹Р РЋРІР‚СљР В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В°Р В Р’В Р РЋР’ВР В Р’В Р РЋРІР‚В
        ctx.getSource().sendSystemMessage(Component.literal(
                "Р В РІР‚в„ўР вЂ™Р’В§cР В Р’В Р РЋРЎС™Р В Р’В Р вЂ™Р’Вµ Р В Р Р‹Р РЋРІР‚СљР В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°Р В Р’В Р вЂ™Р’В»Р В Р’В Р РЋРІР‚СћР В Р Р‹Р В РЎвЂњР В Р Р‹Р В Р вЂ° Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° " + gunId + " (Р В Р’В Р РЋРІР‚ВР В Р’В Р В РІР‚В¦Р В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В¦Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р Р‹Р В РІР‚С™Р В Р Р‹Р В Р вЂ° Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В»Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В¦?)."));
        ctx.getSource().sendSystemMessage(Component.literal(
                "Р В РІР‚в„ўР вЂ™Р’В§eР В Р’В Р Р†Р вЂљРІвЂћСћР В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В»Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚В Р В Р’В Р В РІР‚В Р В Р Р‹Р В РІР‚С™Р В Р Р‹Р РЋРІР‚СљР В Р Р‹Р Р†Р вЂљР Р‹Р В Р’В Р В РІР‚В¦Р В Р Р‹Р РЋРІР‚СљР В Р Р‹Р В РІР‚в„–: Р В РІР‚в„ўР вЂ™Р’В§f" + giveCmd));
        return 0;
    }

    /**
     * Р В Р’В Р Р†Р вЂљРІвЂћСћР В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В»Р В Р’В Р В РІР‚В¦Р В Р Р‹Р В Р РЏР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎв„ў /give Р В Р’В Р РЋРІР‚СћР В Р Р‹Р Р†Р вЂљРЎв„ў Р В Р’В Р РЋРІР‚ВР В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚В Р В Р Р‹Р В РЎвЂњР В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РІР‚С™Р В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’В°.
     * Р В Р’В Р вЂ™Р’ВР В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В»Р В Р Р‹Р В Р вЂ°Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р РЋРІР‚СљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В РЎвЂњР В Р Р‹Р В Р РЏ Р В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В°Р В Р’В Р РЋРІР‚Сњ fallback Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РЎвЂњР В Р’В Р вЂ™Р’В»Р В Р’В Р РЋРІР‚В TaczHelper.giveGun Р В Р’В Р В РІР‚В¦Р В Р’В Р вЂ™Р’Вµ Р В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚вЂќР В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В Р В Р’В Р РЋРІР‚ВР В Р’В Р вЂ™Р’В»Р В Р Р‹Р В РЎвЂњР В Р Р‹Р В Р РЏ.
     */
    private static boolean tryRunGive(CommandContext<CommandSourceStack> ctx, String giveCommand) {
        try {
            var server = ctx.getSource().getServer();
            // Strip leading "/" Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ performPrefixedCommand Р В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В¶Р В Р’В Р РЋРІР‚ВР В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎв„ў Р В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚СћР В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р СћРІР‚ВР В Р Р‹Р РЋРІР‚Сљ Р В Р’В Р вЂ™Р’В±Р В Р’В Р вЂ™Р’ВµР В Р’В Р вЂ™Р’В· Р В Р Р‹Р В РЎвЂњР В Р’В Р вЂ™Р’В»Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†РІР‚С™Р’В¬Р В Р’В Р вЂ™Р’В°
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
            ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cUnknown mode: " + modeId));
            return 0;
        }
        MatchManager.getInstance().setCurrentMode(modeId);
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§aMode set to: " + modeId));
        return 1;
    }

    private int listModes(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§6Р В РІР‚в„ўР вЂ™Р’В§l=== Game Modes ==="));
        for (GameMode m : ModeConfig.getModes().values()) {
            String tag = m.isBuiltIn() ? "" : " Р В РІР‚в„ўР вЂ™Р’В§7(custom)";
            ctx.getSource().sendSystemMessage(Component.literal(
                "Р В РІР‚в„ўР вЂ™Р’В§e" + m.getId() + " Р В РІР‚в„ўР вЂ™Р’В§7(" + m.getDisplayName() + ")" + tag
                + " Р В РІР‚в„ўР вЂ™Р’В§fbuy=" + m.getBuyTimeSeconds() + "s round=" + m.getRoundTimeSeconds() + "s"
                + " start=$" + m.getStartMoney()));
        }
        return 1;
    }

    /**
     * Р В Р’В Р В Р вЂ№Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚ВР В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚Сњ Р В Р’В Р СћРІР‚ВР В Р’В Р РЋРІР‚СћР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р РЋРІР‚СљР В Р’В Р РЋРІР‚вЂќР В Р’В Р В РІР‚В¦Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р Р‹Р Р†Р вЂљР’В¦ Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р РЋРІР‚СљР В Р Р‹Р Р†РІР‚С™Р’В¬Р В Р’В Р вЂ™Р’ВµР В Р’В Р РЋРІР‚Сњ Р В Р Р‹Р В РЎвЂњ Р В Р Р‹Р Р†Р вЂљР’В Р В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В¦Р В Р’В Р вЂ™Р’В°Р В Р’В Р РЋР’ВР В Р’В Р РЋРІР‚В Р В Р’В Р РЋРІР‚В Р В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р’В Р РЋРІР‚вЂњР В Р’В Р РЋРІР‚СћР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚ВР В Р Р‹Р В Р РЏР В Р’В Р РЋР’ВР В Р’В Р РЋРІР‚В.
     */
    private int listGuns(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§6Р В РІР‚в„ўР вЂ™Р’В§l=== Weapons (use /cs test buy <id> or /cs give <player> <id>) ==="));
        String lastCat = null;
        for (Map.Entry<String, Integer> e : GunPriceTable.getAll().entrySet()) {
            String cat = GunPriceTable.getCategory(e.getKey());
            if (!cat.equals(lastCat)) {
                ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§7--- " + cat + " ---"));
                lastCat = cat;
            }
            // Р В Р’В Р РЋРЎСџР В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В°Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В°Р В Р’В Р вЂ™Р’ВµР В Р’В Р РЋР’В Р В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚СћР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚СћР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚ВР В Р’В Р Р†РІР‚С›РІР‚вЂњ id (Р В Р’В Р вЂ™Р’В±Р В Р’В Р вЂ™Р’ВµР В Р’В Р вЂ™Р’В· "tacz:" Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎвЂєР В Р’В Р РЋРІР‚ВР В Р’В Р РЋРІР‚СњР В Р Р‹Р В РЎвЂњР В Р’В Р вЂ™Р’В°) Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В»Р В Р Р‹Р В Р РЏ Р В Р Р‹Р РЋРІР‚СљР В Р’В Р СћРІР‚ВР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В±Р В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В° Р В Р’В Р В РІР‚В Р В Р’В Р В РІР‚В Р В Р’В Р РЋРІР‚СћР В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°
            String shortId = e.getKey();
            if (shortId.startsWith("tacz:")) shortId = shortId.substring(5);
            ctx.getSource().sendSystemMessage(Component.literal(
                "Р В РІР‚в„ўР вЂ™Р’В§e" + shortId + " Р В РІР‚в„ўР вЂ™Р’В§7(" + e.getKey() + ") Р В РІР‚в„ўР вЂ™Р’В§f$" + e.getValue()));
        }
        return 1;
    }

    private int setMapMode(CommandContext<CommandSourceStack> ctx, String mapId, String modeId) {
        if (MapConfig.getMap(mapId) == null) {
            ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cUnknown map: " + mapId));
            return 0;
        }
        if (!modeId.isEmpty() && ModeConfig.getMode(modeId) == null) {
            ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cUnknown mode: " + modeId + " (use empty string to clear)"));
            return 0;
        }
        MapConfig.setMapMode(mapId, modeId);
        broadcastMaps();
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§aMap " + mapId + " mode set to: " + (modeId.isEmpty() ? "any" : modeId)));
        return 1;
    }

    // ====================== Р В Р’В Р вЂ™Р’В Р В Р’В Р вЂ™Р’ВµР В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°Р В Р’В Р РЋРІР‚СњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚ВР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚ВР В Р’В Р вЂ™Р’Вµ Р В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В°Р В Р Р‹Р В РІР‚С™Р В Р Р‹Р Р†Р вЂљРЎв„ў ======================

    private int setLobby(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            BlockPos pos = p.blockPosition();
            MapConfig.setLobbySpawn(pos);
            broadcastMaps();
            p.sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§aLobby set to " + pos));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cThis command must be run as a player"));
            return 0;
        }
    }

    private int addMap(CommandContext<CommandSourceStack> ctx, String id, String name) {
        if (name == null) name = id;
        MapConfig.addOrUpdateMap(id, name);
        broadcastMaps();
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§aMap added/updated: " + id + " (" + name + ")"));
        return 1;
    }

    private int delMap(CommandContext<CommandSourceStack> ctx, String id) {
        if (MapConfig.getMap(id) == null) {
            ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cUnknown map: " + id));
            return 0;
        }
        MapConfig.deleteMap(id);
        broadcastMaps();
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§aMap deleted: " + id));
        return 1;
    }

    private int addSpawn(CommandContext<CommandSourceStack> ctx, String mapId, String teamStr) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            if (MapConfig.getMap(mapId) == null) {
                p.sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cUnknown map: " + mapId));
                return 0;
            }
            Team team = parseTeam(teamStr);
            if (team == null) {
                p.sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cTeam must be T or CT"));
                return 0;
            }
            MapConfig.addSpawn(mapId, team, p.blockPosition());
            broadcastMaps();
            p.sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§aSpawn added for " + team + " on " + mapId + " at " + p.blockPosition()));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cThis command must be run as a player"));
            return 0;
        }
    }

    private int clearSpawns(CommandContext<CommandSourceStack> ctx, String mapId, String teamStr) {
        if (MapConfig.getMap(mapId) == null) {
            ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cUnknown map: " + mapId));
            return 0;
        }
        Team team = parseTeam(teamStr);
        if (team == null) {
            ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cTeam must be T or CT"));
            return 0;
        }
        MapConfig.clearSpawns(mapId, team);
        broadcastMaps();
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§aSpawns cleared for " + team + " on " + mapId));
        return 1;
    }

    /**
     * Р В Р’В Р В РІвЂљВ¬Р В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В° Р В Р’В Р вЂ™Р’В·Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В¦Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“ Р В Р’В Р вЂ™Р’В·Р В Р’В Р вЂ™Р’В°Р В Р’В Р РЋРІР‚СњР В Р Р‹Р РЋРІР‚СљР В Р’В Р РЋРІР‚вЂќР В Р’В Р вЂ™Р’В°: Р В Р’В Р РЋРІР‚вЂќР В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РІР‚С™Р В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В°Р В Р Р‹Р В Р РЏ Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СћР В Р Р‹Р Р†Р вЂљР Р‹Р В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В° Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р’В Р РЋРІР‚СњР В Р Р‹Р РЋРІР‚СљР В Р Р‹Р Р†Р вЂљР’В°Р В Р’В Р вЂ™Р’В°Р В Р Р‹Р В Р РЏ Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В·Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљР’В Р В Р’В Р РЋРІР‚ВР В Р Р‹Р В Р РЏ, Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СћР В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’В°Р В Р Р‹Р В Р РЏ Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚СћР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚ВР В Р’В Р В РІР‚В Р В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В»Р В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В¶Р В Р’В Р В РІР‚В¦Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р Р†РІР‚С›РІР‚вЂњ Р В Р Р‹Р РЋРІР‚СљР В Р’В Р РЋРІР‚вЂњР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В».
     * Р В Р’В Р вЂ™Р’ВР В Р’В Р РЋРІР‚вЂњР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚Сњ Р В Р’В Р СћРІР‚ВР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В»Р В Р’В Р вЂ™Р’В¶Р В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В¦ Р В Р’В Р В РІР‚В Р В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р’В Р В РІР‚В  Р В Р’В Р РЋРІР‚СћР В Р’В Р СћРІР‚ВР В Р’В Р РЋРІР‚ВР В Р’В Р В РІР‚В¦ Р В Р Р‹Р РЋРІР‚СљР В Р’В Р РЋРІР‚вЂњР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В», Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В»Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚СћР В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р СћРІР‚ВР В Р Р‹Р РЋРІР‚Сљ, Р В Р’В Р вЂ™Р’В·Р В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р’В Р РЋР’В Р В Р’В Р В РІР‚В  Р В Р’В Р СћРІР‚ВР В Р Р‹Р В РІР‚С™Р В Р Р‹Р РЋРІР‚СљР В Р’В Р РЋРІР‚вЂњР В Р’В Р РЋРІР‚СћР В Р’В Р Р†РІР‚С›РІР‚вЂњ Р В Р Р‹Р РЋРІР‚СљР В Р’В Р РЋРІР‚вЂњР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В» Р В Р’В Р РЋРІР‚В Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В»Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° Р В Р Р‹Р В РЎвЂњР В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В°.
     * Р В Р’В Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’В»Р В Р Р‹Р В Р РЏ Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚СћР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СћР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р Р†Р вЂљРІвЂћвЂ“: Р В Р’В Р РЋРІР‚вЂќР В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РІР‚С™Р В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В°Р В Р Р‹Р В Р РЏ Р В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚СћР В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В° Р В Р Р‹Р РЋРІР‚СљР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В»Р В Р’В Р РЋРІР‚ВР В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В°Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎв„ў min, Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СћР В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’В°Р В Р Р‹Р В Р РЏ Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ max.
     */
    private int setBuyZone(CommandContext<CommandSourceStack> ctx, String mapId, String teamStr) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            if (MapConfig.getMap(mapId) == null) {
                p.sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cUnknown map: " + mapId));
                return 0;
            }
            Team team = parseTeam(teamStr);
            if (team == null) {
                p.sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cTeam must be T or CT"));
                return 0;
            }
            MapData m = MapConfig.getMap(mapId);
            BlockPos current = p.blockPosition();
            BlockPos min, max;
            if (team == Team.T) {
                if (m.getTBuyZoneMin().equals(m.getTBuyZoneMax())) {
                    min = current;
                    max = current;
                    p.sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§eFirst corner set. Р В РІР‚в„ўР вЂ™Р’В§7Now stand in the opposite corner and run the command again."));
                } else {
                    min = m.getTBuyZoneMin();
                    max = current;
                    p.sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§aBuy zone for T set: " + min + " to " + max));
                }
            } else {
                if (m.getCtBuyZoneMin().equals(m.getCtBuyZoneMax())) {
                    min = current;
                    max = current;
                    p.sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§eFirst corner set. Р В РІР‚в„ўР вЂ™Р’В§7Now stand in the opposite corner and run the command again."));
                } else {
                    min = m.getCtBuyZoneMin();
                    max = current;
                    p.sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§aBuy zone for CT set: " + min + " to " + max));
                }
            }
            MapConfig.setBuyZone(mapId, team, min, max);
            broadcastMaps();
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cThis command must be run as a player"));
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

    // ====================== Р В Р’В Р РЋРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’Вµ Р В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚СћР В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРІвЂћвЂ“ ======================

    private int testPhase(CommandContext<CommandSourceStack> ctx, String phaseStr) {
        GamePhase phase;
        try {
            phase = GamePhase.valueOf(phaseStr.toUpperCase());
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cInvalid phase. Use: LOBBY, BUY_TIME, FIGHTING, ROUND_END"));
            return 0;
        }
        MatchManager.getInstance().setPhase(phase);
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§aPhase set to: " + phase));
        return 1;
    }

    private int testTeam(CommandContext<CommandSourceStack> ctx, String teamStr) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Team team = parseTeam(teamStr);
            if (team == null) {
                p.sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cTeam must be T or CT"));
                return 0;
            }
            MatchManager mm = MatchManager.getInstance();
            mm.getOrCreate(p).setTeam(team);
            p.sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§aTeam set to: " + team));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cThis command must be run as a player"));
            return 0;
        }
    }

    private int testSpawn(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            MatchManager mm = MatchManager.getInstance();
            MapData map = mm.getCurrentMap();
            if (map == null) {
                p.sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cNo map selected"));
                return 0;
            }
            Team team = mm.getOrCreate(p).getTeam();
            if (team == Team.NONE) team = Team.T;
            BlockPos spawn = map.getRandomSpawn(team, new java.util.Random());
            p.teleportTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
            p.sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§aTeleported to " + team + " spawn: " + spawn));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cThis command must be run as a player"));
            return 0;
        }
    }

    // ====================== Р В Р’В Р РЋРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’Вµ Р В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚СћР В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р СћРІР‚ВР В Р Р‹Р Р†Р вЂљРІвЂћвЂ“ Р В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р’В Р РЋРІР‚вЂњР В Р’В Р вЂ™Р’В°Р В Р’В Р вЂ™Р’В·Р В Р’В Р РЋРІР‚ВР В Р’В Р В РІР‚В¦Р В Р’В Р вЂ™Р’В° ======================

    /**
     * Р В Р’В Р РЋРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В°Р В Р Р‹Р В Р РЏ Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚СњР В Р Р‹Р РЋРІР‚СљР В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В°: Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В°Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎв„ў Р В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’ВµР В Р’В Р вЂ™Р’В°Р В Р’В Р вЂ™Р’В»Р В Р Р‹Р В Р вЂ°Р В Р’В Р В РІР‚В¦Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р Р†РІР‚С›РІР‚вЂњ handleBuyRequest (Р В Р Р‹Р В РЎвЂњ Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚СћР В Р’В Р Р†РІР‚С›РІР‚вЂњ Р В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В¦Р В Р’В Р вЂ™Р’ВµР В Р’В Р РЋРІР‚вЂњ, Р В Р’В Р вЂ™Р’В·Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В¦Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“, Р В Р Р‹Р Р†Р вЂљРЎвЂєР В Р’В Р вЂ™Р’В°Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“).
     * Р В Р’В Р РЋРЎСџР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚ВР В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚ВР В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎв„ў Р В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚СћР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚СћР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚ВР В Р’В Р вЂ™Р’Вµ id ("ak47") Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СћР В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚В Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎвЂєР В Р’В Р РЋРІР‚ВР В Р’В Р РЋРІР‚СњР В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚ВР В Р Р‹Р В РІР‚С™Р В Р Р‹Р РЋРІР‚СљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎв„ў "tacz:".
     */
    private int testBuy(CommandContext<CommandSourceStack> ctx, String gunId) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            gunId = com.csedition.tacz.TaczHelper.normalizeGunId(gunId);
            int price = GunPriceTable.getPrice(gunId);
            if (price < 0) {
                p.sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cUnknown weapon: " + gunId + " (use /cs test guns for list)"));
                return 0;
            }
            p.sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§eAttempting to buy " + gunId + " ($" + price + ")..."));
            MatchManager.getInstance().handleBuyRequest(p, gunId);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cThis command must be run as a player"));
            return 0;
        }
    }

    /**
     * Р В Р’В Р РЋРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В°Р В Р Р‹Р В Р РЏ Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’В°: Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљР’ВР В Р Р‹Р Р†Р вЂљРЎв„ў Р В Р’В Р РЋРІР‚СћР В Р Р‹Р В РІР‚С™Р В Р Р‹Р РЋРІР‚СљР В Р’В Р вЂ™Р’В¶Р В Р’В Р РЋРІР‚ВР В Р’В Р вЂ™Р’Вµ Р В Р’В Р вЂ™Р’В±Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚вЂќР В Р’В Р вЂ™Р’В»Р В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚Сћ, Р В Р’В Р вЂ™Р’В±Р В Р’В Р вЂ™Р’ВµР В Р’В Р вЂ™Р’В· Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚Сњ.
     * Р В Р’В Р РЋРЎСџР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚ВР В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚ВР В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎв„ў Р В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚СћР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚СћР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚ВР В Р’В Р вЂ™Р’Вµ id ("ak47") Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СћР В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚В Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎвЂєР В Р’В Р РЋРІР‚ВР В Р’В Р РЋРІР‚СњР В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚ВР В Р Р‹Р В РІР‚С™Р В Р Р‹Р РЋРІР‚СљР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎв„ў "tacz:".
     * Р В Р’В Р Р†Р вЂљРЎС›Р В Р Р‹Р В РЎвЂњР В Р’В Р вЂ™Р’В»Р В Р’В Р РЋРІР‚В API Р В Р’В Р В РІР‚В¦Р В Р’В Р вЂ™Р’Вµ Р В Р Р‹Р В РЎвЂњР В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’В°Р В Р’В Р вЂ™Р’В±Р В Р’В Р РЋРІР‚СћР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р’В Р вЂ™Р’В» Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В»Р В Р’В Р В РІР‚В¦Р В Р Р‹Р В Р РЏР В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎв„ў /give Р В Р’В Р РЋРІР‚СћР В Р Р‹Р Р†Р вЂљРЎв„ў Р В Р Р‹Р В РЎвЂњР В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РІР‚С™Р В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’В° Р В Р Р‹Р В РЎвЂњ Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В»Р В Р’В Р В РІР‚В¦Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р РЋР’В NBT.
     */
    private int testGive(CommandContext<CommandSourceStack> ctx, String gunId) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            gunId = com.csedition.tacz.TaczHelper.normalizeGunId(gunId);
            if (com.csedition.tacz.TaczHelper.giveGun(p, gunId)) {
                p.sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§aGiven: " + gunId));
                return 1;
            }
            // Р В Р’В Р вЂ™Р’В¤Р В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В»Р В Р’В Р вЂ™Р’В»Р В Р’В Р вЂ™Р’В±Р В Р Р‹Р В Р Р‰Р В Р’В Р РЋРІР‚Сњ: /give Р В Р Р‹Р В РЎвЂњ Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В»Р В Р’В Р В РІР‚В¦Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р РЋР’В NBT (GunFireMode, GunCurrentAmmoCount, attachments)
            String giveCmd = com.csedition.tacz.TaczHelper.buildGiveCommand(
                    p.getName().getString(), gunId);
            if (tryRunGive(ctx, giveCmd)) {
                p.sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§aGiven: " + gunId + " Р В РІР‚в„ўР вЂ™Р’В§7(via /give)"));
                return 1;
            }
            p.sendSystemMessage(Component.literal(
                    "Р В РІР‚в„ўР вЂ™Р’В§cР В Р’В Р РЋРЎС™Р В Р’В Р вЂ™Р’Вµ Р В Р Р‹Р РЋРІР‚СљР В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°Р В Р’В Р вЂ™Р’В»Р В Р’В Р РЋРІР‚СћР В Р Р‹Р В РЎвЂњР В Р Р‹Р В Р вЂ° Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ° " + gunId + " (Р В Р’В Р РЋРІР‚ВР В Р’В Р В РІР‚В¦Р В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В¦Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р Р‹Р В РІР‚С™Р В Р Р‹Р В Р вЂ° Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В»Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В¦)."));
            p.sendSystemMessage(Component.literal(
                    "Р В РІР‚в„ўР вЂ™Р’В§eР В Р’В Р Р†Р вЂљРІвЂћСћР В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В»Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚В Р В Р’В Р В РІР‚В Р В Р Р‹Р В РІР‚С™Р В Р Р‹Р РЋРІР‚СљР В Р Р‹Р Р†Р вЂљР Р‹Р В Р’В Р В РІР‚В¦Р В Р Р‹Р РЋРІР‚СљР В Р Р‹Р В РІР‚в„–: Р В РІР‚в„ўР вЂ™Р’В§f" + giveCmd));
            return 0;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cFailed: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Р В Р’В Р РЋРЎвЂєР В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В°Р В Р Р‹Р В Р РЏ Р В Р Р‹Р РЋРІР‚СљР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В° Р В Р’В Р вЂ™Р’В·Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В¦Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“ Р В Р’В Р вЂ™Р’В·Р В Р’В Р вЂ™Р’В°Р В Р’В Р РЋРІР‚СњР В Р Р‹Р РЋРІР‚СљР В Р’В Р РЋРІР‚вЂќР В Р’В Р вЂ™Р’В° Р В Р’В Р В РІР‚В Р В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚СњР В Р Р‹Р В РІР‚С™Р В Р Р‹Р РЋРІР‚СљР В Р’В Р РЋРІР‚вЂњ Р В Р’В Р РЋРІР‚ВР В Р’В Р РЋРІР‚вЂњР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В° (Р В Р’В Р РЋРІР‚СћР В Р’В Р СћРІР‚ВР В Р’В Р В РІР‚В¦Р В Р’В Р вЂ™Р’В° Р В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚СћР В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°).
     * Р В Р’В Р В Р вЂ№Р В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В·Р В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљР’ВР В Р Р‹Р Р†Р вЂљРЎв„ў Р В Р’В Р вЂ™Р’В·Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В¦Р В Р Р‹Р РЋРІР‚Сљ Р В РІР‚в„ўР вЂ™Р’В±8 Р В Р’В Р вЂ™Р’В±Р В Р’В Р вЂ™Р’В»Р В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В  Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚Сћ XZ Р В Р’В Р В РІР‚В Р В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚СњР В Р Р‹Р В РІР‚С™Р В Р Р‹Р РЋРІР‚СљР В Р’В Р РЋРІР‚вЂњ Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р’В Р РЋРІР‚СњР В Р Р‹Р РЋРІР‚СљР В Р Р‹Р Р†Р вЂљР’В°Р В Р’В Р вЂ™Р’ВµР В Р’В Р Р†РІР‚С›РІР‚вЂњ Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В·Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљР’В Р В Р’В Р РЋРІР‚ВР В Р’В Р РЋРІР‚В.
     * Y Р В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СћР В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚В Р В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В РЎвЂњР В Р Р‹Р В Р РЏ Р В Р’В Р РЋРІР‚СћР В Р Р‹Р Р†Р вЂљРЎв„ў -60 Р В Р’В Р СћРІР‚ВР В Р’В Р РЋРІР‚Сћ 222 (Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В»Р В Р’В Р В РІР‚В¦Р В Р’В Р вЂ™Р’В°Р В Р Р‹Р В Р РЏ Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚СћР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°).
     * Р В Р’В Р В РІвЂљВ¬Р В Р’В Р СћРІР‚ВР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В±Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚Сћ Р В Р’В Р СћРІР‚ВР В Р’В Р вЂ™Р’В»Р В Р Р‹Р В Р РЏ Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚ВР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚ВР В Р Р‹Р В Р РЏ Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚СњР В Р Р‹Р РЋРІР‚СљР В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚В: Р В Р’В Р В РІР‚В Р В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р’В Р вЂ™Р’В», Р В Р’В Р РЋРІР‚вЂќР В Р Р‹Р В РІР‚С™Р В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚ВР В Р Р‹Р В РЎвЂњР В Р’В Р вЂ™Р’В°Р В Р’В Р вЂ™Р’В» Р В Р вЂ Р В РІР‚С™Р Р†Р вЂљРЎСљ Р В Р’В Р РЋРІР‚В Р В Р’В Р РЋР’ВР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В¶Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚Сћ Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚СњР В Р Р‹Р РЋРІР‚СљР В Р’В Р РЋРІР‚вЂќР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В Р вЂ°.
     */
    private int testSetBuyZone(CommandContext<CommandSourceStack> ctx, String teamStr) {
        try {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Team team = parseTeam(teamStr);
            if (team == null) {
                p.sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cTeam must be T or CT"));
                return 0;
            }
            MatchManager mm = MatchManager.getInstance();
            MapData map = mm.getCurrentMap();
            if (map == null) {
                p.sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cNo map selected. Use /cs start <mapId> first."));
                return 0;
            }
            BlockPos playerPos = p.blockPosition();
            int radius = 8;
            // Y Р В Р’В Р вЂ™Р’В·Р В Р’В Р вЂ™Р’В°Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В»Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В РЎвЂњР В Р Р‹Р В Р РЏ Р В Р’В Р вЂ™Р’В°Р В Р’В Р В РІР‚В Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СћР В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’В°Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљР Р‹Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РЎвЂњР В Р’В Р РЋРІР‚СњР В Р’В Р РЋРІР‚В Р В Р’В Р В РІР‚В  MapConfig.setBuyZone (Р В Р’В Р РЋРІР‚СћР В Р Р‹Р Р†Р вЂљРЎв„ў -60 Р В Р’В Р СћРІР‚ВР В Р’В Р РЋРІР‚Сћ 222)
            BlockPos min = playerPos.offset(-radius, 0, -radius);
            BlockPos max = playerPos.offset(radius, 0, radius);
            MapConfig.setBuyZone(map.getId(), team, min, max);
            broadcastMaps();
            p.sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§aBuy zone for Р В РІР‚в„ўР вЂ™Р’В§e" + team + " Р В РІР‚в„ўР вЂ™Р’В§aset around you Р В РІР‚в„ўР вЂ™Р’В§7("
                    + (radius * 2 + 1) + "x" + (radius * 2 + 1) + " XZ, full Y)"));
            p.sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§7You can now buy weapons here. Use /cs test buy <gunId>."));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§cThis command must be run as a player: " + e.getMessage()));
            return 0;
        }
    }

    // ====================== Р В Р’В Р РЋРІвЂћСћР В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В¦Р В Р Р‹Р Р†Р вЂљРЎвЂєР В Р’В Р РЋРІР‚ВР В Р’В Р РЋРІР‚вЂњ ======================

    private int configSlots(CommandContext<CommandSourceStack> ctx, int amount) {
        CSConfig.setMaxInventorySlots(amount);
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§aMax inventory slots set to: " + CSConfig.getMaxInventorySlots()));
        return 1;
    }

    private int configKills(CommandContext<CommandSourceStack> ctx, int amount) {
        CSConfig.setKillsToWin(amount);
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§aKills to win set to: " + CSConfig.getKillsToWin()));
        return 1;
    }

    private int configClearInv(CommandContext<CommandSourceStack> ctx, String value) {
        boolean v = value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("on");
        CSConfig.setClearInventoryOnMatchEnd(v);
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§aClear inventory on match end: " + CSConfig.isClearInventoryOnMatchEnd()));
        return 1;
    }

    private int configKeptAdd(CommandContext<CommandSourceStack> ctx, String itemId) {
        CSConfig.addKeptItem(itemId);
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§aAdded to kept items: " + itemId));
        return 1;
    }

    private int configKeptRemove(CommandContext<CommandSourceStack> ctx, String itemId) {
        CSConfig.removeKeptItem(itemId);
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§aRemoved from kept items: " + itemId));
        return 1;
    }

    private int configKeptList(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§6Р В РІР‚в„ўР вЂ™Р’В§l=== Kept Items ==="));
        for (String s : CSConfig.getKeptItems()) {
            ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§e" + s));
        }
        return 1;
    }

    private int configShow(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§6Р В РІР‚в„ўР вЂ™Р’В§l=== CS Config ==="));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§eMax inventory slots: Р В РІР‚в„ўР вЂ™Р’В§f" + CSConfig.getMaxInventorySlots()));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§eKills to win: Р В РІР‚в„ўР вЂ™Р’В§f" + CSConfig.getKillsToWin()));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§eClear inventory on match end: Р В РІР‚в„ўР вЂ™Р’В§f" + CSConfig.isClearInventoryOnMatchEnd()));
        ctx.getSource().sendSystemMessage(Component.literal("Р В РІР‚в„ўР вЂ™Р’В§eKept items: Р В РІР‚в„ўР вЂ™Р’В§f" + CSConfig.getKeptItems().size()));
        return 1;
    }

    // ====================== Р В Р’В Р В РІвЂљВ¬Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚ВР В Р’В Р вЂ™Р’В»Р В Р’В Р РЋРІР‚ВР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р Р†Р вЂљРІвЂћвЂ“ ======================

    private Team parseTeam(String s) {
        if (s == null) return null;
        s = s.toUpperCase();
        if (s.equals("T") || s.equals("TERRORIST") || s.equals("TERRORISTS")) return Team.T;
        if (s.equals("CT") || s.equals("COUNTER") || s.equals("COUNTERTERRORIST")) return Team.CT;
        return null;
    }

    /**
     * Р В Р’В Р вЂ™Р’В Р В Р’В Р вЂ™Р’В°Р В Р Р‹Р В РЎвЂњР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’В»Р В Р’В Р вЂ™Р’В°Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎв„ў Р В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В±Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В»Р В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р В РІР‚В¦Р В Р’В Р В РІР‚В¦Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’Вµ Р В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В°Р В Р Р‹Р В РІР‚С™Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р Р†Р вЂљРІвЂћвЂ“ Р В Р’В Р В РІР‚В Р В Р Р‹Р В РЎвЂњР В Р’В Р вЂ™Р’ВµР В Р’В Р РЋР’В Р В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В»Р В Р’В Р РЋРІР‚ВР В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В¦Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р’В Р РЋР’В.
     * Р В Р’В Р Р†Р вЂљРІвЂћСћР В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’В·Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В°Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р В РЎвЂњР В Р Р‹Р В Р РЏ Р В Р’В Р РЋРІР‚вЂќР В Р’В Р РЋРІР‚СћР В Р Р‹Р В РЎвЂњР В Р’В Р вЂ™Р’В»Р В Р’В Р вЂ™Р’Вµ Р В Р’В Р вЂ™Р’В»Р В Р Р‹Р В РІР‚в„–Р В Р’В Р вЂ™Р’В±Р В Р’В Р РЋРІР‚СћР В Р’В Р РЋРІР‚вЂњР В Р’В Р РЋРІР‚Сћ Р В Р’В Р РЋРІР‚ВР В Р’В Р вЂ™Р’В·Р В Р’В Р РЋР’ВР В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В¦Р В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚ВР В Р Р‹Р В Р РЏ.
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
     * Р В Р’В Р вЂ™Р’В Р В Р’В Р вЂ™Р’В°Р В Р Р‹Р В РЎвЂњР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’В»Р В Р’В Р вЂ™Р’В°Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р Р†Р вЂљРЎв„ў Р В Р’В Р РЋРІР‚СћР В Р’В Р вЂ™Р’В±Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’В»Р В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р В РІР‚В¦Р В Р’В Р В РІР‚В¦Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’Вµ Р В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’ВµР В Р’В Р вЂ™Р’В¶Р В Р’В Р РЋРІР‚ВР В Р’В Р РЋР’ВР В Р Р‹Р Р†Р вЂљРІвЂћвЂ“ Р В Р’В Р В РІР‚В Р В Р Р‹Р В РЎвЂњР В Р’В Р вЂ™Р’ВµР В Р’В Р РЋР’В Р В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В»Р В Р’В Р РЋРІР‚ВР В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В¦Р В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В°Р В Р’В Р РЋР’В.
     */
    private void broadcastModes() {
        PacketSyncModes pkt = new PacketSyncModes(ModeConfig.toJson());
        for (ServerPlayer p : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            CSPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), pkt);
        }
    }
}
