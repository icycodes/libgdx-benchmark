package com.matchreplay.core;

public class PlayerState {
    public final int id;
    public final int startX;
    public final int startY;
    public int currentX;
    public int currentY;

    public PlayerState(int id, int startX, int startY) {
        this.id = id;
        this.startX = startX;
        this.startY = startY;
        this.currentX = startX;
        this.currentY = startY;
    }

    public int getFinalX() {
        return currentX;
    }

    public int getFinalY() {
        return currentY;
    }
}
