package com.example.matchreplay.core;

public final class PlayerSnapshot {
    private final int id;
    private final int startX;
    private final int startY;
    private final int finalX;
    private final int finalY;

    public PlayerSnapshot(int id, int startX, int startY, int finalX, int finalY) {
        this.id = id;
        this.startX = startX;
        this.startY = startY;
        this.finalX = finalX;
        this.finalY = finalY;
    }

    public int getId() {
        return id;
    }

    public int getStartX() {
        return startX;
    }

    public int getStartY() {
        return startY;
    }

    public int getFinalX() {
        return finalX;
    }

    public int getFinalY() {
        return finalY;
    }
}
