package com.csedition.network;

import com.csedition.CSEditionMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Регистрация сетевого канала CS-Edition.
 * Использует SimpleChannel из Forge Networking.
 *
 * Канал: csedition:main
 * Пакеты:
 *   0 — C2S: запрос покупки (GunId)
 *   1 — S2C: обновление денег игрока
 *   2 — S2C: обновление фазы матча
 *   3 — C2S: выбор карты (mapId)
 *   4 — S2C: синхронизация списка карт
 */
public class CSPackets {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CSEditionMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        CHANNEL.registerMessage(id++, PacketBuyGun.class,
                PacketBuyGun::encode, PacketBuyGun::decode, PacketBuyGun::handle);
        CHANNEL.registerMessage(id++, PacketMoneyUpdate.class,
                PacketMoneyUpdate::encode, PacketMoneyUpdate::decode, PacketMoneyUpdate::handle);
        CHANNEL.registerMessage(id++, PacketPhaseUpdate.class,
                PacketPhaseUpdate::encode, PacketPhaseUpdate::decode, PacketPhaseUpdate::handle);
        CHANNEL.registerMessage(id++, PacketSelectMap.class,
                PacketSelectMap::encode, PacketSelectMap::decode, PacketSelectMap::handle);
        CHANNEL.registerMessage(id++, PacketMapList.class,
                PacketMapList::encode, PacketMapList::decode, PacketMapList::handle);
        CHANNEL.registerMessage(id++, PacketQuickBuy.class,
                PacketQuickBuy::encode, PacketQuickBuy::decode, PacketQuickBuy::handle);
    }
}
