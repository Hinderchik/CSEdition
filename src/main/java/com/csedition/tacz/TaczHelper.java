package com.csedition.tacz;

import com.csedition.CSEditionMod;
import com.csedition.config.WeaponConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

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

    private static Method getCreateGunMethod() {
        if (API_CHECKED) return CREATE_GUN_METHOD;
        API_CHECKED = true;
        String[] candidatePaths = {
            "com.tacz.guns.item.GunItem",
            "com.tacz.gun.item.GunItem",
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

    public static ItemStack createGun(String gunId) {
        if (gunId == null || gunId.isEmpty()) return ItemStack.EMPTY;
        ResourceLocation rl = new ResourceLocation(gunId);

        Method method = getCreateGunMethod();
        if (method != null) {
            try {
                Object result = (method.getParameterTypes()[0] == ResourceLocation.class)
                        ? method.invoke(null, rl)
                        : method.invoke(null, gunId);
                if (result instanceof ItemStack stack && !stack.isEmpty()) {
                    return ensureLoaded(stack, gunId);
                }
            } catch (Exception e) {
                CSEditionMod.LOGGER.error("[CS-Edition] TaCZ API failed for {}: {}", gunId, e.getMessage());
            }
        }

        Item baseItem = getBaseGunItem();
        if (baseItem != null) return buildGunNbt(gunId, baseItem);

        Item directItem = ForgeRegistries.ITEMS.getValue(rl);
        if (directItem != null) return buildGunNbt(gunId, directItem);

        CSEditionMod.LOGGER.warn("[CS-Edition] Gun not found: {} (TaCZ API: {})", gunId, API_AVAILABLE);
        return ItemStack.EMPTY;
    }

    private static ItemStack buildGunNbt(String gunId, Item baseItem) {
        ItemStack stack = new ItemStack(baseItem);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("GunId", gunId);
        tag.putString("GunFireMode", WeaponConfig.getFireMode(gunId));
        int mag = WeaponConfig.getMagazineSize(gunId);
        tag.putInt("GunCurrentAmmoCount", mag);
        tag.putInt("GunMaxAmmoCount", mag);
        tag.putInt("AmmoCount", mag);
        tag.putInt("AmmoCountMax", mag);
        tag.putBoolean("HasBulletInBarrel", true);

        applyAttachments(tag, gunId);
        stack.setTag(tag);
        CSEditionMod.LOGGER.debug("[CS-Edition] Built gun NBT for: {} (mag={})", gunId, mag);
        return stack;
    }

    private static void applyAttachments(CompoundTag gunTag, String gunId) {
        Map<String, String> attachments = WeaponConfig.getAttachments(gunId);
        if (attachments.isEmpty()) return;
        for (Map.Entry<String, String> entry : attachments.entrySet()) {
            CompoundTag attachmentItem = new CompoundTag();
            attachmentItem.putString("id", "tacz:attachment");
            attachmentItem.putByte("Count", (byte) 1);
            CompoundTag innerTag = new CompoundTag();
            innerTag.putString("AttachmentId", entry.getValue());
            attachmentItem.put("tag", innerTag);
            gunTag.put(entry.getKey(), attachmentItem);
        }
    }

    private static ItemStack ensureLoaded(ItemStack stack, String gunId) {
        if (stack.isEmpty() || !stack.hasTag()) return stack;
        CompoundTag tag = stack.getTag();
        if (tag == null) return stack;
        boolean modified = false;
        if (!tag.contains("GunFireMode")) {
            tag.putString("GunFireMode", WeaponConfig.getFireMode(gunId));
            modified = true;
        }
        if (!tag.contains("GunCurrentAmmoCount")) {
            int mag = WeaponConfig.getMagazineSize(gunId);
            tag.putInt("GunCurrentAmmoCount", mag);
            tag.putInt("GunMaxAmmoCount", mag);
            modified = true;
        }
        if (!tag.contains("HasBulletInBarrel")) {
            tag.putBoolean("HasBulletInBarrel", true);
            modified = true;
        }
        applyAttachments(tag, gunId);
        modified = true;
        if (modified) stack.setTag(tag);
        return stack;
    }

    public static ItemStack createKnife() {
        return createGun("tacz:combat_knife");
    }

    public static ItemStack createPistol(boolean isT) {
        return createGun(isT ? "tacz:glock_17" : "mcs2:cs_usp");
    }

    public static boolean isTaczAvailable() {
        getCreateGunMethod();
        return API_AVAILABLE;
    }

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

    public static String buildGiveCommand(String targetName, String gunId) {
        int mag = WeaponConfig.getMagazineSize(gunId);
        String mode = WeaponConfig.getFireMode(gunId);
        Map<String, String> attachments = WeaponConfig.getAttachments(gunId);

        StringBuilder sb = new StringBuilder(128);
        sb.append("/give ").append(targetName)
          .append(" tacz:modern_kinetic_gun{")
          .append("GunId:\"").append(gunId).append("\"")
          .append(",GunFireMode:\"").append(mode).append("\"")
          .append(",GunCurrentAmmoCount:").append(mag)
          .append(",GunMaxAmmoCount:").append(mag);
        for (Map.Entry<String, String> entry : attachments.entrySet()) {
            sb.append(",").append(entry.getKey())
              .append(":{id:\"tacz:attachment\",Count:1b,tag:{AttachmentId:\"")
              .append(entry.getValue()).append("\"}}");
        }
        sb.append("}");
        return sb.toString();
    }

    public static String normalizeGunId(String input) {
        if (input == null) return null;
        String s = input.trim();
        if (s.isEmpty()) return s;
        if (s.contains(":")) return s;
        return "tacz:" + s;
    }

    // ====================== Ammo ======================

    /**
     * TaCZ ammo items are `tacz:ammo` with an NBT tag `AmmoId` specifying
     * the caliber (e.g. `tacz:9mm`, `tacz:5.56`, `tacz:7.62x39`).
     *
     * The ammo ID for a given gun is defined in TaCZ's data pack.
     * Without API access we use best-effort heuristics:
     *   1. If the gun ItemStack has an `AmmoId` tag (some TaCZ versions), use it
     *   2. Otherwise return the gun ID as-is (works for many default packs)
     *
     * @param gunId gun ID like "tacz:glock_17"
     * @return ammo ID like "tacz:9mm" or the gun ID as fallback
     */
    public static String getAmmoIdForGun(String gunId) {
        if (gunId == null || gunId.isEmpty()) return null;
        // Try to read AmmoId from gun data via reflection (if TaCZ API exposed it)
        try {
            // Some TaCZ versions store ammo id in gun index; try reflection on common paths
            Class<?> gunIndexClass = Class.forName("com.tacz.guns.resource.index.CommonGunIndex");
            Object ammoId = gunIndexClass.getMethod("getAmmoId", ResourceLocation.class)
                    .invoke(null, new ResourceLocation(gunId));
            if (ammoId != null) return ammoId.toString();
        } catch (Throwable ignored) {
            // API not available or method signature differs — fall through
        }
        // Fallback: use gun ID as ammo ID
        return gunId;
    }

    /**
     * Create an ammo ItemStack for the given gun.
     *
     * @param gunId gun ID (used to determine ammo type)
     * @param count stack size
     * @return ammo ItemStack, or ItemStack.EMPTY if creation failed
     */
    public static ItemStack createAmmo(String gunId, int count) {
        if (count <= 0) return ItemStack.EMPTY;
        String ammoId = getAmmoIdForGun(gunId);
        if (ammoId == null || ammoId.isEmpty()) return ItemStack.EMPTY;

        // Try to resolve tacz:ammo item
        Item ammoItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("tacz:ammo"));
        if (ammoItem == null) {
            CSEditionMod.LOGGER.warn("[CS-Edition] tacz:ammo item not found");
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(ammoItem, Math.min(count, 64));
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("AmmoId", ammoId);
        stack.setTag(tag);
        return stack;
    }

    /**
     * Give ammo to a player. If count > 64, gives multiple stacks.
     *
     * @param player target player
     * @param gunId  gun ID (determines ammo type)
     * @param count  total ammo count
     * @return true if all ammo was given, false if inventory was full
     */
    public static boolean giveAmmo(net.minecraft.server.level.ServerPlayer player, String gunId, int count) {
        if (count <= 0) return true;
        int remaining = count;
        while (remaining > 0) {
            int batch = Math.min(remaining, 64);
            ItemStack stack = createAmmo(gunId, batch);
            if (stack.isEmpty()) return false;

            var inv = player.getInventory();
            if (inv.add(stack)) {
                remaining -= batch;
            } else {
                // Try to place in empty slot
                boolean placed = false;
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    if (inv.getItem(i).isEmpty()) {
                        inv.setItem(i, stack);
                        remaining -= batch;
                        placed = true;
                        break;
                    }
                }
                if (!placed) {
                    player.drop(stack, false);
                    remaining -= batch;
                }
            }
        }
        return true;
    }
}