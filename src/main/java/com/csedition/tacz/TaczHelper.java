package com.csedition.tacz;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Утилита для создания предметов-пушек TaCZ через NBT-тег GunId.
 * Не копирует механику стрельбы — TaCZ сам обрабатывает предмет.
 *
 * Пример: createGun("tacz:m4a4") -> ItemStack с NBT { GunId: "tacz:m4a4" }
 */
public final class TaczHelper {
    private TaczHelper() {}

    /**
     * Создаёт ItemStack пушки TaCZ по её id (например, "tacz:m4a4").
     * Если предмет не найден в реестре — возвращает пустой стек.
     */
    public static ItemStack createGun(String gunId) {
        ResourceLocation rl = new ResourceLocation(gunId);
        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null) return ItemStack.EMPTY;

        ItemStack stack = new ItemStack(item);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("GunId", gunId);
        stack.setTag(tag);
        return stack;
    }

    /**
     * Создаёт нож (базовый предмет TaCZ).
     */
    public static ItemStack createKnife() {
        return createGun("tacz:combat_knife");
    }

    /**
     * Создаёт базовый пистолет (Glock для T / USP для CT).
     */
    public static ItemStack createPistol(boolean isT) {
        return createGun(isT ? "tacz:glock_17" : "tacz:usp_45");
    }
}
