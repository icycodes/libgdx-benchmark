package com.example.game;

/**
 * Immutable value object containing the parsed data for one level description.
 */
public final class LevelData {
    private final String name;
    private final int enemies;
    private final int difficulty;

    public LevelData(String name, int enemies, int difficulty) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        this.name = name;
        this.enemies = enemies;
        this.difficulty = difficulty;
    }

    public String getName() {
        return name;
    }

    public int getEnemies() {
        return enemies;
    }

    public int getDifficulty() {
        return difficulty;
    }
}
