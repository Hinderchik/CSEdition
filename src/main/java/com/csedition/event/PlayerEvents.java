package com.csedition.event;

import com.csedition.data.GamePhase;
import com.csedition.match.MatchManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Серверные события игроков.
 * Подписан на шину Forge (зарегистрирован в CSEditionMod).
 */
public class PlayerEvents {

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            MatchManager.getInstance().onPlayerJoin(sp);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            MatchManager.getInstance().onPlayerLeave(sp);
        }
    }

    /**
     * В лобби отключаем любой урон.
     */
    @SubscribeEvent
    public void onLivingHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (MatchManager.getInstance().getPhase() == GamePhase.LOBBY) {
            if (event.getEntity() instanceof Player) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * Обработка убийств — начисление денег, проверка конца раунда.
     * Также убираем броню (Netherite) из слотов и оружие из инвентаря,
     * чтобы они НЕ выпали на землю. Нож в KNIFE_SLOT остаётся.
     * Очищаем ДО LivingDropsEvent — иначе предметы попадут в дроплист.
     */
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        // Удаляем броню из слотов — она не должна выпасть при смерти
        var inv = victim.getInventory();
        for (int i = 0; i < inv.armor.size(); i++) {
            inv.armor.set(i, net.minecraft.world.item.ItemStack.EMPTY);
        }
        // Очищаем основной инвентарь кроме слота с ножом (KNIFE_SLOT)
        // чтобы при респавне не было лишнего оружия на земле
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (i == com.csedition.match.MatchManager.KNIFE_SLOT) continue;
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        // Убираем модификатор брони чтобы при респавне не было лишних очков
        var armorAttr = victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.removeModifier(java.util.UUID.fromString("9c5b6f1e-3a2d-4e8b-9f1c-7a8b9c0d1e2f"));
        }
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            MatchManager.getInstance().onPlayerKill(victim, killer);
        } else {
            MatchManager.getInstance().onPlayerKill(victim, null);
        }
    }
}
