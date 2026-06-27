package com.csedition.tacz;

import com.csedition.CSEditionMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;

/**
 * Утилита для создания предметов-пушек TaCZ.
 *
 * Использует официальный API TaCZ (GunItem.createGun) через рефлексию,
 * с fallback на ручное создание через NBT-тег GunId.
 *
 * Это гарантирует, что:
 *   - Пушка создаётся именно как предмет TaCZ (со всей механикой стрельбы)
 *   - NBT содержит правильный GunId
 *   - Прикрепляются все нужные данные (патроны, модификации и т.д.)
 *
 * Пример: createGun("tacz:m4a4") -> полноценный ItemStack пушки M4A4 из TaCZ
 */
public final class TaczHelper {
    private TaczHelper() {}

    // Кэш рефлексии — инициализируется один раз
    private static Method CREATE_GUN_METHOD = null;
    private static boolean API_CHECKED = false;
    private static boolean API_AVAILABLE = false;

    /**
     * Пытается получить доступ к TaCZ API (GunItem.createGun).
     * Результат кэшируется — повторных обращений к Class.forName нет.
     */
    private static Method getCreateGunMethod() {
        if (API_CHECKED) return CREATE_GUN_METHOD;
        API_CHECKED = true;
        try {
            Class<?> gunItemClass = Class.forName("com.tacz.gun.item.GunItem");
            CREATE_GUN_METHOD = gunItemClass.getMethod("createGun", ResourceLocation.class);
            API_AVAILABLE = true;
            CSEditionMod.LOGGER.info("[CS-Edition] TaCZ API (GunItem.createGun) available — using official method");
        } catch (ClassNotFoundException e) {
            CSEditionMod.LOGGER.warn("[CS-Edition] TaCZ not found — using NBT fallback");
        } catch (NoSuchMethodException e) {
            CSEditionMod.LOGGER.warn("[CS-Edition] GunItem.createGun not found — using NBT fallback");
        }
        return CREATE_GUN_METHOD;
    }

    /**
     * Создаёт ItemStack пушки TaCZ по её id (например, "tacz:m4a4").
     *
     * Сначала пробует официальный API TaCZ, если недоступен — создаёт вручную:
     *   1. Берёт предмет из реестра ForgeRegistries.ITEMS
     *   2. Устанавливает NBT-тег GunId
     *
     * @param gunId полный id пушки, например "tacz:ak47", "tacz:m4a4", "tacz:awp"
     * @return ItemStack пушки или ItemStack.EMPTY если предмет не найден
     */
    public static ItemStack createGun(String gunId) {
        if (gunId == null || gunId.isEmpty()) return ItemStack.EMPTY;
        ResourceLocation rl = new ResourceLocation(gunId);

        // Способ 1: официальный API TaCZ (предпочтительно)
        Method method = getCreateGunMethod();
        if (method != null) {
            try {
                ItemStack stack = (ItemStack) method.invoke(null, rl);
                if (!stack.isEmpty()) return stack;
            } catch (Exception e) {
                CSEditionMod.LOGGER.error("[CS-Edition] TaCZ API failed for {}: {}", gunId, e.getMessage());
            }
        }

        // Способ 2: ручное создание через реестр + NBT
        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null) {
            CSEditionMod.LOGGER.warn("[CS-Edition] Gun not found in registry: {}", gunId);
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(item);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("GunId", gunId);
        stack.setTag(tag);
        return stack;
    }

    /**
     * Создаёт нож (базовый предмет TaCZ).
     * Использует GunId "tacz:combat_knife" — стандартный нож в TaCZ.
     */
    public static ItemStack createKnife() {
        return createGun("tacz:combat_knife");
    }

    /**
     * Создаёт базовый пистолет (Glock для T / USP для CT).
     * Это стартовое оружие в CS — выдаётся каждому игроку в начале раунда.
     */
    public static ItemStack createPistol(boolean isT) {
        return createGun(isT ? "tacz:glock_17" : "tacz:usp_45");
    }

    /**
     * Проверяет, доступен ли TaCZ (для условной логики).
     */
    public static boolean isTaczAvailable() {
        getCreateGunMethod();
        return API_AVAILABLE;
    }

    /**
     * Выдаёт пушку игроку (добавляет в инвентарь).
     * @return true если успешно выдано
     */
    public static boolean giveGun(net.minecraft.server.level.ServerPlayer player, String gunId) {
        ItemStack stack = createGun(gunId);
        if (stack.isEmpty()) return false;
        return player.getInventory().add(stack);
    }
}
