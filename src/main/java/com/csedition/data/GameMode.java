package com.csedition.data;

import java.util.List;

public class GameMode {
    private final String id;
    private final String displayName;
    private final String description;
    private final int startMoney;
    private final int killReward;
    private final int roundWinReward;
    private final int buyTimeSeconds;
    private final int roundTimeSeconds;
    private final int roundsToWin;
    private final boolean respawn;
    private final boolean allowBuy;
    private final List<String> startWeaponsT;
    private final List<String> startWeaponsCT;
    private final boolean builtIn;

    public GameMode(String id, String displayName, String description,
                    int startMoney, int killReward, int roundWinReward,
                    int buyTimeSeconds, int roundTimeSeconds,
                    int roundsToWin,
                    boolean respawn, boolean allowBuy,
                    List<String> startWeaponsT, List<String> startWeaponsCT,
                    boolean builtIn) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.startMoney = startMoney;
        this.killReward = killReward;
        this.roundWinReward = roundWinReward;
        this.buyTimeSeconds = buyTimeSeconds;
        this.roundTimeSeconds = roundTimeSeconds;
        this.roundsToWin = roundsToWin;
        this.respawn = respawn;
        this.allowBuy = allowBuy;
        this.startWeaponsT = startWeaponsT;
        this.startWeaponsCT = startWeaponsCT;
        this.builtIn = builtIn;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getStartMoney() { return startMoney; }
    public int getKillReward() { return killReward; }
    public int getRoundWinReward() { return roundWinReward; }
    public int getBuyTimeSeconds() { return buyTimeSeconds; }
    public int getRoundTimeSeconds() { return roundTimeSeconds; }
    public int getRoundsToWin() { return roundsToWin; }
    public boolean isRespawn() { return respawn; }
    public boolean isAllowBuy() { return allowBuy; }
    public List<String> getStartWeaponsT() { return startWeaponsT; }
    public List<String> getStartWeaponsCT() { return startWeaponsCT; }
    public boolean isBuiltIn() { return builtIn; }

    public List<String> getStartWeapons(Team team) {
        return team == Team.T ? startWeaponsT : startWeaponsCT;
    }

    public static GameMode classic() {
        return new GameMode("classic", "Classic",
                "Standard CS rules: pistol + knife, buy in BUY_TIME",
                800, 300, 3000, 15, 120, 8,
                false, true,
                List.of("tacz:glock_17"),
                List.of("tacz:usp_45"),
                true);
    }

    public static GameMode deathmatch() {
        return new GameMode("deathmatch", "Deathmatch",
                "Free-for-all, respawn, all weapons free",
                0, 0, 0, 0, 0, 1,
                true, false,
                List.of("tacz:ak47", "tacz:glock_17"),
                List.of("tacz:m4a4", "tacz:usp_45"),
                true);
    }

    public static GameMode gungame() {
        return new GameMode("gungame", "Gun Game",
                "Each kill = next weapon. Knife = win",
                0, 0, 0, 0, 0, 1,
                true, false,
                List.of("tacz:glock_17"),
                List.of("tacz:usp_45"),
                true);
    }

    public static GameMode pistolOnly() {
        return new GameMode("pistol_only", "Pistol Only",
                "Only pistols allowed",
                800, 300, 3000, 15, 120, 8,
                false, true,
                List.of("tacz:glock_17"),
                List.of("tacz:usp_45"),
                true);
    }
}
