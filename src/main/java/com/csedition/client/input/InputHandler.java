package com.csedition.client.input;

import com.csedition.CSEditionMod;
import com.csedition.client.ClientState;
import com.csedition.client.keybind.KeyBindings;
import com.csedition.client.screen.BuyMenuScreen;
import com.csedition.client.screen.MapSelectScreen;
import com.csedition.data.GamePhase;
import com.csedition.network.CSPackets;
import com.csedition.network.PacketQuickBuy;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Обработка нажатий клавиш на клиенте.
 *
 * B  — открыть меню закупа (BUY_TIME) / выбор карты (LOBBY)
 * M  — открыть выбор карты (только в LOBBY)
 * G  — выбросить текущее оружие
 * Z  — быстрая закупка (последнее купленное)
 * X  — быстрая закупка (основное оружие)
 * C  — быстрая закупка (пистолет)
 * 4  — быстрая закупка (снаряжение)
 * F  — осмотреть оружие
 * N  — переключить ночное зрение
 */
@OnlyIn(Dist.CLIENT)
public class InputHandler {

    // Кэшированные ссылки — избегаем повторных статических обращений
    private static final net.minecraft.world.effect.MobEffect NIGHT_VISION =
        net.minecraft.world.effect.MobEffects.NIGHT_VISION;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        GamePhase phase = ClientState.getPhase();

        // === Меню ===
        if (KeyBindings.OPEN_BUY_MENU.consumeClick()) {
            if (phase == GamePhase.LOBBY) {
                mc.setScreen(new MapSelectScreen());
            } else if (phase == GamePhase.BUY_TIME) {
                mc.setScreen(new BuyMenuScreen());
            }
        }

        if (KeyBindings.OPEN_MAP_SELECT.consumeClick()) {
            if (phase == GamePhase.LOBBY) {
                mc.setScreen(new MapSelectScreen());
            }
        }

        // === Действия с оружием ===
        if (KeyBindings.DROP_WEAPON.consumeClick()) {
            dropCurrentWeapon(mc.player);
        }

        if (KeyBindings.INSPECT_WEAPON.consumeClick()) {
            // Осмотр оружия — визуальный эффект, реализуется через анимацию TaCZ
            // Здесь можно отправить пакет на сервер для синхронизации анимации
            mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }

        // === Быстрая закупка (только в BUY_TIME) ===
        if (phase == GamePhase.BUY_TIME) {
            if (KeyBindings.QUICK_BUY_LAST.consumeClick()) {
                CSPackets.CHANNEL.sendToServer(new PacketQuickBuy(PacketQuickBuy.Type.LAST));
            }
            if (KeyBindings.QUICK_BUY_PRIMARY.consumeClick()) {
                CSPackets.CHANNEL.sendToServer(new PacketQuickBuy(PacketQuickBuy.Type.PRIMARY));
            }
            if (KeyBindings.QUICK_BUY_SECONDARY.consumeClick()) {
                CSPackets.CHANNEL.sendToServer(new PacketQuickBuy(PacketQuickBuy.Type.SECONDARY));
            }
            if (KeyBindings.QUICK_BUY_UTILITY.consumeClick()) {
                CSPackets.CHANNEL.sendToServer(new PacketQuickBuy(PacketQuickBuy.Type.UTILITY));
            }
        }

        // === Ночное зрение ===
        if (KeyBindings.TOGGLE_NIGHT_VISION.consumeClick()) {
            toggleNightVision(mc.player);
        }
    }

    /**
     * Выбросить текущее оружие (как в CS — клавиша G).
     */
    private void dropCurrentWeapon(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return;

        // Не выбрасываем нож
        if (isKnife(stack)) return;

        player.drop(stack.copy(), true);
        stack.setCount(0);
    }

    private boolean isKnife(ItemStack stack) {
        String id = stack.getItem().toString().toLowerCase();
        return id.contains("knife") || id.contains("knife_pack");
    }

    /**
     * Переключить ночное зрение (эффект зелья).
     */
    private void toggleNightVision(Player player) {
        if (player.hasEffect(NIGHT_VISION)) {
            player.removeEffect(NIGHT_VISION);
        } else {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    NIGHT_VISION, 6000, 0, false, false
            ));
        }
    }
}
