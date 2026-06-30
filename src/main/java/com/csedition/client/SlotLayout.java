package com.csedition.client;

/**
 * Hotbar slot layout:
 *   slot 0: Primary weapon (rifle, sniper, smg, shotgun, heavy)
 *   slot 1: Secondary weapon (pistol)
 *   slot 2: Knife (from MCS2/Knifepack - KNIFE_SLOT in MatchManager)
 *   slot 3: Utility (grenades)
 */
public final class SlotLayout {
    public static final int PRIMARY = 0;
    public static final int SECONDARY = 1;
    public static final int KNIFE_SLOT = 2;
    public static final int UTILITY = 3;
    public static final int COUNT = 4;

    private SlotLayout() {}

    /**
     * Returns the hotbar slot index for a given weapon ID based on its category.
     *   pistol      -> 1
     *   rifle/smg/sniper/shotgun/heavy -> 0
     *   utility     -> 3
     *   knife (lrtactical:melee) -> 3
     *   unknown     -> first free slot among 0/1/3
     */
    public static int slotFor(String gunId, String category, boolean[] occupied) {
        if ("knife".equals(category)) return UTILITY;
        return switch (category) {
            case "pistol" -> SECONDARY;
            case "rifle", "smg", "sniper", "shotgun", "heavy" -> PRIMARY;
            case "utility" -> UTILITY;
            default -> firstFree(occupied);
        };
    }

    private static int firstFree(boolean[] occupied) {
        int[] order = { PRIMARY, SECONDARY, UTILITY };
        for (int s : order) {
            if (!occupied[s]) return s;
        }
        return -1;
    }
}
