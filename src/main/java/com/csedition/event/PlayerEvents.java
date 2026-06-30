package com.csedition.event;

import com.csedition.data.GamePhase;
import com.csedition.match.MatchManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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

    @SubscribeEvent
    public void onLivingHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (MatchManager.getInstance().getPhase() == GamePhase.LOBBY) {
            if (event.getEntity() instanceof Player) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        var inv = victim.getInventory();
        for (int i = 0; i < inv.armor.size(); i++) {
            inv.armor.set(i, ItemStack.EMPTY);
        }
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (i == com.csedition.match.MatchManager.KNIFE_SLOT) continue;
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
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

    @SubscribeEvent
    public void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (MatchManager.getInstance().getPhase() != GamePhase.BUY_TIME) return;
        if (sp.isCreative() || sp.isSpectator()) return;
        sp.setDeltaMovement(0.0, sp.getDeltaMovement().y, 0.0);
        sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ(), sp.getYRot(), sp.getXRot());
    }
}
