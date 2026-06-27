package com.csedition.data;

import java.util.UUID;

/**
 * Серверские данные игрока в матче.
 * Хранятся в MatchManager.playerDataMap.
 *
 * Деньги и награды теперь параметризованы через GameMode.
 */
public class PlayerData {
    public static final int DEFAULT_START_MONEY = 800;
    public static final int DEFAULT_KILL_REWARD = 300;
    public static final int DEFAULT_ROUND_WIN_REWARD = 3000;

    private final UUID playerId;
    private Team team = Team.NONE;
    private int money = DEFAULT_START_MONEY;
    private int kills = 0;
    private int deaths = 0;
    private boolean alive = true;
    private String lastBought = null;  // Последнее купленное оружие (для Z)

    public PlayerData(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() { return playerId; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public int getMoney() { return money; }
    public void setMoney(int money) { this.money = Math.max(0, money); }
    public void addMoney(int amount) { this.money = Math.max(0, this.money + amount); }
    public boolean trySpend(int amount) {
        if (money >= amount) { money -= amount; return true; }
        return false;
    }
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }
    public String getLastBought() { return lastBought; }
    public void setLastBought(String lastBought) { this.lastBought = lastBought; }

    /**
     * Сбрасывает состояние для нового раунда.
     * @param startMoney стартовое количество денег (из режима)
     */
    public void resetForRound(int startMoney) {
        this.money = startMoney;
        this.alive = true;
    }

    /**
     * Сбрасывает состояние для нового раунда (дефолтные деньги).
     */
    public void resetForRound() {
        resetForRound(DEFAULT_START_MONEY);
    }

    /**
     * Вызывается при убийстве. Добавляет награду.
     * @param reward количество денег за убийство (из режима)
     */
    public void onKill(int reward) {
        this.kills++;
        this.addMoney(reward);
    }

    /**
     * Вызывается при смерти.
     */
    public void onDeath() {
        this.deaths++;
        this.alive = false;
    }

    /**
     * Вызывается при победе в раунде.
     * @param reward количество денег за победу (из режима)
     */
    public void onRoundWin(int reward) {
        this.addMoney(reward);
    }

    /**
     * Полный сброс статистики (после окончания матча).
     */
    public void resetStats() {
        this.kills = 0;
        this.deaths = 0;
        this.money = DEFAULT_START_MONEY;
        this.alive = true;
        this.lastBought = null;
    }
}
