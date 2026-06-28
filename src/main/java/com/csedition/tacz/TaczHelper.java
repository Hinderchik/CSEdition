package com.csedition.tacz;

import com.csedition.CSEditionMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Утилита для создания пушек TaCZ (CS Edition).
 * Использует официальный API через рефлексию, с надёжным NBT-fallback.
 *
 * В TaCZ все пушки - это один базовый предмет, а конкретная модель
 * определяется через NBT-тег GunId. Поэтому fallback ищет известные
 * gun-item, а не id пушки (например "tacz:m4a4" - не предмет в реестре).
 *
 * Fallback дополнительно прописывает AmmoCount / AmmoCountMax по таблице
 * размеров магазинов, чтобы пушка была заряжена и сразу стреляла.
 */
public final class TaczHelper {
    private TaczHelper() {}

    private static Method CREATE_GUN_METHOD = null;
    private static Class<?> GUN_ITEM_CLASS = null;
    private static boolean API_CHECKED = false;
    private static boolean API_AVAILABLE = false;
    private static Item cachedGunItem = null;
    private static boolean gunItemLookupDone = false;

    private static final String[] KNOWN_GUN_ITEM_IDS = {
            "tacz:modern_kinetic_gun",
            "tacz:gun",
            "tacz:kinetic_gun",
            "tacz:modern_kinetic_gun_gray",
            "tacz:modern_kinetic_gun_debug",
    };

    /**
     * Размеры магазинов для известных пушек.
     * Используется в NBT-fallback, когда официальный API недоступен
     * и не задаёт AmmoCount/AmmoCountMax сам.
     */
    private static final Map<String, Integer> MAGAZINE_SIZES = new HashMap<>();
    static {
        // Пистолеты
        MAGAZINE_SIZES.put("tacz:glock_17", 17);
        MAGAZINE_SIZES.put("tacz:usp_45", 12);
        MAGAZINE_SIZES.put("tacz:p226", 15);
        MAGAZINE_SIZES.put("tacz:p320", 17);
        MAGAZINE_SIZES.put("tacz:m1911", 7);
        MAGAZINE_SIZES.put("tacz:deagle", 7);
        MAGAZINE_SIZES.put("tacz:cz75", 16);
        MAGAZINE_SIZES.put("tacz:hk45", 8);
        // SMG
        MAGAZINE_SIZES.put("tacz:mp5", 30);
        MAGAZINE_SIZES.put("tacz:mp7", 30);
        MAGAZINE_SIZES.put("tacz:mp9", 30);
        MAGAZINE_SIZES.put("tacz:p90", 50);
        MAGAZINE_SIZES.put("tacz:uzi", 32);
        MAGAZINE_SIZES.put("tacz:vector", 25);
        // Винтовки
        MAGAZINE_SIZES.put("tacz:ak47", 30);
        MAGAZINE_SIZES.put("tacz:m4a1", 30);
        MAGAZINE_SIZES.put("tacz:m4a4", 30);
        MAGAZINE_SIZES.put("tacz:hk416", 30);
        MAGAZINE_SIZES.put("tacz:ar15", 30);
        MAGAZINE_SIZES.put("tacz:aug", 30);
        MAGAZINE_SIZES.put("tacz:sg556", 30);
        MAGAZINE_SIZES.put("tacz:scar_l", 30);
        // Снайперские
        MAGAZINE_SIZES.put("tacz:awp", 5);
        MAGAZINE_SIZES.put("tacz:m24", 5);
        MAGAZINE_SIZES.put("tacz:scar_20", 10);
        MAGAZINE_SIZES.put("tacz:m700", 5);
        // Дробовики
        MAGAZINE_SIZES.put("tacz:m870", 5);
        MAGAZINE_SIZES.put("tacz:m1014", 6);
        // LMG
        MAGAZINE_SIZES.put("tacz:m249", 100);
        MAGAZINE_SIZES.put("tacz:rpk", 75);
    }

    private static int magazineSizeFor(String gunId) {
        Integer m = MAGAZINE_SIZES.get(gunId);
        return m != null ? m : 30; // дефолт — 30 (как у большинства автоматов)
    }

    /**
     * Ищет официальный API TaCZ (GunItem.createGun).
     * Поддерживает две сигнатуры: ResourceLocation и String.
     * Путь GunItem — com.tacz.guns.item (plural) в TaCZ 1.1.x.
     */
    private static Method getCreateGunMethod() {
        if (API_CHECKED) return CREATE_GUN_METHOD;
        API_CHECKED = true;
        // Список возможных путей для разных версий TaCZ
        String[] candidatePaths = {
            "com.tacz.guns.item.GunItem",     // 1.1.x
            "com.tacz.gun.item.GunItem",      // старые версии
        };
        for (String path : candidatePaths) {
            try {
                GUN_ITEM_CLASS = Class.forName(path);
                try {
                    CREATE_GUN_METHOD = GUN_ITEM_CLASS.getMethod("createGun", ResourceLocation.class);
                    API_AVAILABLE = true;
                    CSEditionMod.LOGGER.info("[CS-Edition] TaCZ API OK ({})", path);
                    return CREATE_GUN_METHOD;
                } catch (NoSuchMethodException e1) {
                    try {
                        CREATE_GUN_METHOD = GUN_ITEM_CLASS.getMethod("createGun", String.class);
                        API_AVAILABLE = true;
                        CSEditionMod.LOGGER.info("[CS-Edition] TaCZ API (String) OK ({})", path);
                        return CREATE_GUN_METHOD;
                    } catch (NoSuchMethodException e2) {
                        CSEditionMod.LOGGER.warn("[CS-Edition] GunItem.createGun not found at {}", path);
                    }
                }
            } catch (ClassNotFoundException e) {
                // пробуем следующий путь
            }
        }
        CSEditionMod.LOGGER.warn("[CS-Edition] TaCZ not found - using NBT fallback");
        return CREATE_GUN_METHOD;
    }

    /**
     * Возвращает базовый gun-item TaCZ (например, tacz:modern_kinetic_gun).
     * Используется в fallback, когда официальный API недоступен.
     */
    private static Item getBaseGunItem() {
        if (gunItemLookupDone) return cachedGunItem;
        gunItemLookupDone = true;
        for (String id : KNOWN_GUN_ITEM_IDS) {
            try {
                ResourceLocation rl = new ResourceLocation(id);
                Item item = ForgeRegistries.ITEMS.getValue(rl);
                if (item != null) {
                    cachedGunItem = item;
                    CSEditionMod.LOGGER.info("[CS-Edition] TaCZ gun-item resolved by id: {}", id);
                    return cachedGunItem;
                }
            } catch (Exception ignored) {}
        }
        if (GUN_ITEM_CLASS != null) {
            for (String fieldName : new String[]{"INSTANCE", "instance", "ITEM"}) {
                try {
                    Field f = GUN_ITEM_CLASS.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    Object instance = f.get(null);
                    if (instance instanceof Item item) {
                        cachedGunItem = item;
                        CSEditionMod.LOGGER.info("[CS-Edition] TaCZ gun-item resolved via field {}", fieldName);
                        return cachedGunItem;
                    }
                } catch (NoSuchFieldException | IllegalAccessException ignored) {}
            }
        }
        CSEditionMod.LOGGER.warn("[CS-Edition] TaCZ base gun-item not resolved - guns will not work");
        return null;
    }

    /**
     * Создаёт ItemStack пушки TaCZ по её id (например, "tacz:m4a4").
     * 1) Официальный API (GunItem.createGun).
     * 2) NBT-fallback через базовый gun-item с прописанным ammo.
     * 3) Прямой поиск предмета в реестре (последний шанс).
     */
    public static ItemStack createGun(String gunId) {
        if (gunId == null || gunId.isEmpty()) return ItemStack.EMPTY;
        ResourceLocation rl = new ResourceLocation(gunId);

        // 1) Официальный API TaCZ
        Method method = getCreateGunMethod();
        if (method != null) {
            try {
                Object result;
                if (method.getParameterTypes()[0] == ResourceLocation.class) {
                    result = method.invoke(null, rl);
                } else {
                    result = method.invoke(null, gunId);
                }
                if (result instanceof ItemStack stack && !stack.isEmpty()) {
                    // Дополнительная проверка: если API вернул пушку без ammo, дополним
                    return ensureLoaded(stack, gunId);
                }
            } catch (Exception e) {
                CSEditionMod.LOGGER.error("[CS-Edition] TaCZ API failed for {}: {}", gunId, e.getMessage());
            }
        }

        // 2) Надёжный NBT-fallback через базовый gun-item
        Item baseItem = getBaseGunItem();
        if (baseItem != null) {
            ItemStack stack = new ItemStack(baseItem);
            CompoundTag tag = stack.getOrCreateTag();
            tag.putString("GunId", gunId);
            // НЕ ставим GunFireMode — TaCZ использует default из gun data,
            // и неверное значение даёт "Unknown fire mode, unable to shoot"
            int mag = magazineSizeFor(gunId);
            tag.putInt("AmmoCount", mag);
            tag.putInt("AmmoCountMax", mag);
            // Патрон в стволе — чтобы можно было стрелять сразу
            if (!tag.contains("HasBulletInBarrel")) {
                tag.putBoolean("HasBulletInBarrel", true);
            }
            stack.setTag(tag);
            CSEditionMod.LOGGER.debug("[CS-Edition] Created gun via NBT fallback: {} (mag={})", gunId, mag);
            return stack;
        }

        // 3) Последний шанс - прямой поиск в реестре
        Item directItem = ForgeRegistries.ITEMS.getValue(rl);
        if (directItem != null) {
            ItemStack stack = new ItemStack(directItem);
            CompoundTag tag = stack.getOrCreateTag();
            tag.putString("GunId", gunId);
            int mag = magazineSizeFor(gunId);
            tag.putInt("AmmoCount", mag);
            tag.putInt("AmmoCountMax", mag);
            stack.setTag(tag);
            return stack;
        }

        CSEditionMod.LOGGER.warn("[CS-Edition] Gun not found: {} (TaCZ API: {})", gunId, API_AVAILABLE);
        return ItemStack.EMPTY;
    }

    /**
     * Если в NBT пушки нет AmmoCount/AmmoCountMax, дописывает значения из таблицы.
     * Нужно на случай когда TaCZ API вернул пушку без ammo-тегов.
     * НЕ трогает GunFireMode — TaCZ сам разберётся из gun data.
     */
    private static ItemStack ensureLoaded(ItemStack stack, String gunId) {
        if (stack.isEmpty() || !stack.hasTag()) return stack;
        CompoundTag tag = stack.getTag();
        if (tag == null) return stack;
        if (!tag.contains("AmmoCount") || !tag.contains("AmmoCountMax")) {
            int mag = magazineSizeFor(gunId);
            if (!tag.contains("AmmoCount")) tag.putInt("AmmoCount", mag);
            if (!tag.contains("AmmoCountMax")) tag.putInt("AmmoCountMax", mag);
            stack.setTag(tag);
        }
        if (!tag.contains("HasBulletInBarrel")) {
            tag.putBoolean("HasBulletInBarrel", true);
            stack.setTag(tag);
        }
        return stack;
    }

    public static ItemStack createKnife() {
        return createGun("tacz:combat_knife");
    }

    public static ItemStack createPistol(boolean isT) {
        return createGun(isT ? "tacz:glock_17" : "tacz:usp_45");
    }

    public static boolean isTaczAvailable() {
        getCreateGunMethod();
        return API_AVAILABLE;
    }

    /**
     * Выдаёт пушку игроку. Сначала пытается добавить через Inventory.add
     * (он сам находит свободный слот, предпочитая хотбар).
     * Если не вышло — ищет пустой слот вручную.
     */
    public static boolean giveGun(net.minecraft.server.level.ServerPlayer player, String gunId) {
        ItemStack stack = createGun(gunId);
        if (stack.isEmpty()) {
            CSEditionMod.LOGGER.warn("[CS-Edition] Cannot give gun {} to {} - stack empty",
                    gunId, player.getName().getString());
            return false;
        }
        var inv = player.getInventory();
        if (inv.add(stack)) return true;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).isEmpty()) {
                inv.setItem(i, stack);
                return true;
            }
        }
        CSEditionMod.LOGGER.warn("[CS-Edition] Inventory full, cannot give gun {} to {}",
                gunId, player.getName().getString());
        return false;
    }

    /**
     * Выдаёт пушку в указанный слот хотбара (0..8).
     * Если слот занят — заменяет (старое выбрасывает игроку).
     * Если вне хотбара — кладёт как есть.
     */
    public static boolean giveGunToSlot(net.minecraft.server.level.ServerPlayer player, String gunId, int slot) {
        ItemStack stack = createGun(gunId);
        if (stack.isEmpty()) return false;
        var inv = player.getInventory();
        if (slot < 0 || slot >= inv.getContainerSize()) return giveGun(player, gunId);
        ItemStack existing = inv.getItem(slot);
        if (!existing.isEmpty()) {
            // Выбрасываем старый предмет игроку
            if (!player.getInventory().add(existing.copy())) {
                player.drop(existing.copy(), false);
            }
        }
        inv.setItem(slot, stack);
        return true;
    }

    /**
     * Нормализует id оружия: если не указано пространство имён,
     * добавляет префикс "tacz:".
     *   "ak47"        -> "tacz:ak47"
     *   "tacz:ak47"   -> "tacz:ak47"
     *   "tacz:"       -> "tacz:"
     *   null/empty    -> null/empty
     */
    public static String normalizeGunId(String input) {
        if (input == null) return null;
        String s = input.trim();
        if (s.isEmpty()) return s;
        if (s.contains(":")) return s;
        return "tacz:" + s;
    }
}
