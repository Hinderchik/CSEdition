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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Утилита для создания пушек TaCZ (CS Edition).
 *
 * Правильный NBT-формат пушки (как в /give):
 *   tacz:modern_kinetic_gun{
 *     GunId: "tacz:ak47",                    // string — id оружия в TaCZ-датапаке
 *     GunFireMode: "AUTO",                   // string — "SEMI"/"AUTO"/"BURST"
 *     GunCurrentAmmoCount: 30,               // int — патронов сейчас
 *     GunMaxAmmoCount: 30,                   // int — размер магазина
 *     HasBulletInBarrel: 1b,                 // bool — патрон в стволе
 *     AmmoCount: 30,                         // (старый тег, для совместимости)
 *     AmmoCountMax: 30,                      // (старый тег, для совместимости)
 *     AttachmentMUZZLE: { id:"tacz:attachment", Count:1b, tag:{AttachmentId:"..."} },
 *     AttachmentGRIP:   { ... },
 *     AttachmentSCOPE:  { ... },
 *     AttachmentSTOCK:  { ... },
 *     AttachmentMAGAZINE:{ ... }
 *   }
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
     * Размеры магазинов для известных пушек (по умолчанию если нет в gun data).
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

    /**
     * Fire mode по умолчанию для каждой пушки (если gun data не указывает иначе).
     * "SEMI" = полуавтомат, "AUTO" = автомат, "BURST" = очередь.
     */
    private static final Map<String, String> DEFAULT_FIRE_MODES = new HashMap<>();
    static {
        DEFAULT_FIRE_MODES.put("tacz:glock_17", "SEMI");
        DEFAULT_FIRE_MODES.put("tacz:usp_45", "SEMI");
        DEFAULT_FIRE_MODES.put("tacz:p250", "SEMI");
        DEFAULT_FIRE_MODES.put("tacz:desert_eagle", "SEMI");
        DEFAULT_FIRE_MODES.put("tacz:p226", "SEMI");
        DEFAULT_FIRE_MODES.put("tacz:p320", "SEMI");
        DEFAULT_FIRE_MODES.put("tacz:m1911", "SEMI");
        DEFAULT_FIRE_MODES.put("tacz:cz75", "SEMI");
        DEFAULT_FIRE_MODES.put("tacz:hk45", "SEMI");
        DEFAULT_FIRE_MODES.put("tacz:mp5", "AUTO");
        DEFAULT_FIRE_MODES.put("tacz:mp7", "AUTO");
        DEFAULT_FIRE_MODES.put("tacz:mp9", "AUTO");
        DEFAULT_FIRE_MODES.put("tacz:p90", "AUTO");
        DEFAULT_FIRE_MODES.put("tacz:uzi", "AUTO");
        DEFAULT_FIRE_MODES.put("tacz:vector", "AUTO");
        DEFAULT_FIRE_MODES.put("tacz:ump_45", "AUTO");
        DEFAULT_FIRE_MODES.put("tacz:ak47", "AUTO");
        DEFAULT_FIRE_MODES.put("tacz:m4a1", "AUTO");
        DEFAULT_FIRE_MODES.put("tacz:m4a4", "AUTO");
        DEFAULT_FIRE_MODES.put("tacz:hk416", "AUTO");
        DEFAULT_FIRE_MODES.put("tacz:ar15", "AUTO");
        DEFAULT_FIRE_MODES.put("tacz:aug", "AUTO");
        DEFAULT_FIRE_MODES.put("tacz:sg556", "AUTO");
        DEFAULT_FIRE_MODES.put("tacz:scar_l", "AUTO");
        DEFAULT_FIRE_MODES.put("tacz:awp", "SEMI");
        DEFAULT_FIRE_MODES.put("tacz:m24", "SEMI");
        DEFAULT_FIRE_MODES.put("tacz:scar_20", "SEMI");
        DEFAULT_FIRE_MODES.put("tacz:m700", "SEMI");
        DEFAULT_FIRE_MODES.put("tacz:scout", "SEMI");
        DEFAULT_FIRE_MODES.put("tacz:m870", "SEMI");
        DEFAULT_FIRE_MODES.put("tacz:m1014", "SEMI");
        DEFAULT_FIRE_MODES.put("tacz:nova", "SEMI");
        DEFAULT_FIRE_MODES.put("tacz:xm1014", "SEMI");
        DEFAULT_FIRE_MODES.put("tacz:m249", "AUTO");
        DEFAULT_FIRE_MODES.put("tacz:rpk", "AUTO");
    }

    /**
     * Автоматически устанавливаемые аттачменты для конкретных пушек.
     * Ключ — GunId пушки, значение — map слота → AttachmentId.
     * Слоты: AttachmentMUZZLE, AttachmentGRIP, AttachmentSCOPE, AttachmentSTOCK, AttachmentMAGAZINE.
     */
    private static final Map<String, Map<String, String>> GUN_ATTACHMENTS = new LinkedHashMap<>();
    static {
        // MCS2 (мод MCS2-Knifepack с USP silencer)
        GUN_ATTACHMENTS.put("mcs2:cs_usp", Map.of("AttachmentMUZZLE", "mcs2:usp_silencer"));
        // Примеры для других модов — добавляй сюда по мере надобности
    }

    private static int magazineSizeFor(String gunId) {
        Integer m = MAGAZINE_SIZES.get(gunId);
        return m != null ? m : 30;
    }

    private static String fireModeFor(String gunId) {
        String m = DEFAULT_FIRE_MODES.get(gunId);
        return m != null ? m : "SEMI";
    }

    /**
     * Ищет официальный API TaCZ (GunItem.createGun).
     * Путь GunItem — com.tacz.guns.item (plural) в TaCZ 1.1.x.
     */
    private static Method getCreateGunMethod() {
        if (API_CHECKED) return CREATE_GUN_METHOD;
        API_CHECKED = true;
        String[] candidatePaths = {
            "com.tacz.guns.item.GunItem",     // 1.1.x
            "com.tacz.gun.item.GunItem",      // старые
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
            } catch (ClassNotFoundException ignored) {}
        }
        CSEditionMod.LOGGER.warn("[CS-Edition] TaCZ not found - using NBT fallback");
        return CREATE_GUN_METHOD;
    }

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
     * Создаёт ItemStack пушки TaCZ по её id.
     * Использует правильный NBT-формат: GunId + GunFireMode(string) + GunCurrentAmmoCount + attachments.
     */
    public static ItemStack createGun(String gunId) {
        if (gunId == null || gunId.isEmpty()) return ItemStack.EMPTY;
        ResourceLocation rl = new ResourceLocation(gunId);

        // 1) Официальный API TaCZ (если доступен)
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
                    return ensureLoaded(stack, gunId);
                }
            } catch (Exception e) {
                CSEditionMod.LOGGER.error("[CS-Edition] TaCZ API failed for {}: {}", gunId, e.getMessage());
            }
        }

        // 2) NBT-fallback через базовый gun-item
        Item baseItem = getBaseGunItem();
        if (baseItem != null) {
            return buildGunNbt(gunId, baseItem);
        }

        // 3) Прямой поиск в реестре (последний шанс)
        Item directItem = ForgeRegistries.ITEMS.getValue(rl);
        if (directItem != null) {
            return buildGunNbt(gunId, directItem);
        }

        CSEditionMod.LOGGER.warn("[CS-Edition] Gun not found: {} (TaCZ API: {})", gunId, API_AVAILABLE);
        return ItemStack.EMPTY;
    }

    /**
     * Строит ItemStack с правильным NBT для пушки.
     * Формат соответствует /give: GunId, GunFireMode (string), GunCurrentAmmoCount, attachments.
     */
    private static ItemStack buildGunNbt(String gunId, Item baseItem) {
        ItemStack stack = new ItemStack(baseItem);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("GunId", gunId);
        // Fire mode — STRING (не int!), значения "SEMI"/"AUTO"/"BURST"
        tag.putString("GunFireMode", fireModeFor(gunId));
        // Ammo — оба варианта для совместимости со старыми и новыми версиями TaCZ
        int mag = magazineSizeFor(gunId);
        tag.putInt("GunCurrentAmmoCount", mag);
        tag.putInt("GunMaxAmmoCount", mag);
        tag.putInt("AmmoCount", mag);
        tag.putInt("AmmoCountMax", mag);
        tag.putBoolean("HasBulletInBarrel", true);

        // Автоматические аттачменты для этой пушки
        applyAttachments(tag, gunId);

        stack.setTag(tag);
        CSEditionMod.LOGGER.debug("[CS-Edition] Built gun NBT for: {} (mag={}, mode={})",
                gunId, mag, fireModeFor(gunId));
        return stack;
    }

    /**
     * Добавляет автоматические аттачменты (глушитель, прицел и т.д.) для конкретной пушки.
     * Формат в NBT: AttachmentMUZZLE: { id:"tacz:attachment", Count:1b, tag:{AttachmentId:"..."} }
     */
    private static void applyAttachments(CompoundTag gunTag, String gunId) {
        Map<String, String> attachments = GUN_ATTACHMENTS.get(gunId);
        if (attachments == null) return;
        for (Map.Entry<String, String> entry : attachments.entrySet()) {
            CompoundTag attachmentItem = new CompoundTag();
            attachmentItem.putString("id", "tacz:attachment");
            attachmentItem.putByte("Count", (byte) 1);
            CompoundTag innerTag = new CompoundTag();
            innerTag.putString("AttachmentId", entry.getValue());
            attachmentItem.put("tag", innerTag);
            gunTag.put(entry.getKey(), attachmentItem);
            CSEditionMod.LOGGER.debug("[CS-Edition] Auto-attached {} -> {} to {}",
                    entry.getKey(), entry.getValue(), gunId);
        }
    }

    /**
     * Дополняет NBT пушки недостающими полями.
     * Если API уже вернул пушку с правильным NBT — не трогает её.
     */
    private static ItemStack ensureLoaded(ItemStack stack, String gunId) {
        if (stack.isEmpty() || !stack.hasTag()) return stack;
        CompoundTag tag = stack.getTag();
        if (tag == null) return stack;
        boolean modified = false;
        // Fire mode — только если нет
        if (!tag.contains("GunFireMode")) {
            tag.putString("GunFireMode", fireModeFor(gunId));
            modified = true;
        }
        // Ammo
        if (!tag.contains("GunCurrentAmmoCount")) {
            int mag = magazineSizeFor(gunId);
            tag.putInt("GunCurrentAmmoCount", mag);
            tag.putInt("GunMaxAmmoCount", mag);
            modified = true;
        }
        // Bullet in barrel
        if (!tag.contains("HasBulletInBarrel")) {
            tag.putBoolean("HasBulletInBarrel", true);
            modified = true;
        }
        // Авто-аттачменты — применяем всегда (они идемпотентны)
        applyAttachments(tag, gunId);
        modified = true;
        if (modified) stack.setTag(tag);
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
     * Выдаёт пушку игроку. Возвращает true если успешно.
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

    public static boolean giveGunToSlot(net.minecraft.server.level.ServerPlayer player, String gunId, int slot) {
        ItemStack stack = createGun(gunId);
        if (stack.isEmpty()) return false;
        var inv = player.getInventory();
        if (slot < 0 || slot >= inv.getContainerSize()) return giveGun(player, gunId);
        ItemStack existing = inv.getItem(slot);
        if (!existing.isEmpty()) {
            if (!player.getInventory().add(existing.copy())) {
                player.drop(existing.copy(), false);
            }
        }
        inv.setItem(slot, stack);
        return true;
    }

    /**
     * Строит команду /give для указанной пушки.
     * Используется как fallback / для отображения админу.
     *
     * Примеры:
     *   /give Steve tacz:modern_kinetic_gun{GunId:"tacz:ak47", GunFireMode:"AUTO", GunCurrentAmmoCount:30}
     *   /give Steve tacz:modern_kinetic_gun{GunId:"mcs2:cs_usp", GunFireMode:"SEMI", GunCurrentAmmoCount:12, AttachmentMUZZLE:{id:"tacz:attachment", Count:1b, tag:{AttachmentId:"mcs2:usp_silencer"}}}
     */
    public static String buildGiveCommand(String targetName, String gunId) {
        int mag = magazineSizeFor(gunId);
        String mode = fireModeFor(gunId);
        StringBuilder sb = new StringBuilder();
        sb.append("/give ").append(targetName)
          .append(" tacz:modern_kinetic_gun{")
          .append("GunId:\"").append(gunId).append("\"")
          .append(",GunFireMode:\"").append(mode).append("\"")
          .append(",GunCurrentAmmoCount:").append(mag)
          .append(",GunMaxAmmoCount:").append(mag);
        // Авто-аттачменты
        Map<String, String> attachments = GUN_ATTACHMENTS.get(gunId);
        if (attachments != null) {
            for (Map.Entry<String, String> entry : attachments.entrySet()) {
                sb.append(",").append(entry.getKey())
                  .append(":{id:\"tacz:attachment\",Count:1b,tag:{AttachmentId:\"")
                  .append(entry.getValue()).append("\"}}");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Нормализует id оружия: добавляет "tacz:" если нет namespace.
     */
    public static String normalizeGunId(String input) {
        if (input == null) return null;
        String s = input.trim();
        if (s.isEmpty()) return s;
        if (s.contains(":")) return s;
        return "tacz:" + s;
    }
}
