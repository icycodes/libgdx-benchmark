package com.example.game;

public class LevelData {
    private String name;
    private int enemies;
    private int difficulty;

    public LevelData() {
    }

    public LevelData(String name, int enemies, int difficulty) {
        this.name = name;
        this.enemies = enemies;
        this.difficulty = difficulty;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getEnemies() {
        return enemies;
    }

    public void setEnemies(int enemies) {
        this.enemies = enemies;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    @Override
    public String toString() {
        return "LevelData{" +
                "name='" + name + '\'' +
                ", enemies=" + enemies +
                ", difficulty=" + difficulty +
                '}';
    }
}
