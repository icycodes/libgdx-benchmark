package com.example.gdxgame;

public record Summary(String game, int levelCount, long enemyCount, long totalHp, String strongestEnemy) {
    public String format() {
        return "Game: " + game + "\n"
            + "Levels: " + levelCount + "\n"
            + "Enemies: " + enemyCount + "\n"
            + "Total HP: " + totalHp + "\n"
            + "Strongest enemy: " + strongestEnemy + "\n";
    }
}
