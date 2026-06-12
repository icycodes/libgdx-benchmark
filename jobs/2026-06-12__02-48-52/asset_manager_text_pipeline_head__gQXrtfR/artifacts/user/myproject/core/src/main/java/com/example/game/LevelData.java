package com.example.game;

/**
 * Immutable value type representing a parsed level description.
 */
public class LevelData {
    public final String name;
    public final int enemies;
    public final int difficulty;

    public LevelData(String name, int enemies, int difficulty) {
        this.name = name;
        this.enemies = enemies;
        this.difficulty = difficulty;
    }

    @Override
    public String toString() {
        return "LevelData{name='" + name + "', enemies=" + enemies + ", difficulty=" + difficulty + "}";
    }
}
