package com.example.game;

public class LevelData {
    public final String name;
    public final int enemies;
    public final int difficulty;

    public LevelData(String name, int enemies, int difficulty) {
        if (name == null) throw new IllegalArgumentException("name must not be null");
        if (enemies < 0) throw new IllegalArgumentException("enemies must be non-negative");
        if (difficulty < 0) throw new IllegalArgumentException("difficulty must be non-negative");
        this.name = name;
        this.enemies = enemies;
        this.difficulty = difficulty;
    }
}
