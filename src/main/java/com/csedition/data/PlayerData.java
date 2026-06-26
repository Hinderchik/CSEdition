package com.csedition.data;

import java.util.UUID;

/**
 * Серверские данные игрока в матче.
 * Хранятся в MatchManager.playerDataMap.
 */
public class PlayerData {
    public static final int START_MONEY = 800;
    public static final int KILL_REWARD = 300;
    public static final int ROUND_WIN_REWARD = 3000;

    private final UUID playerId;
    private Team team = Team.NONE;
    private int money = START_MONEY;
    private int kills = 0;
    private int deaths = 0;
    private boolean alive = true;

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

    public void resetForRound() {
        this.money = START_MONEY;
        this.alive = true;
    }

    public void onKill() {
        this.kills++;
        this.addMoney(KILL_REWARD);
    }

    public void onDeath() {
        this.deaths++;
        this.alive = false;
    }

    public void onRoundWin() {
        this.addMoney(ROUND_WIN_REWARD);
    }
}
