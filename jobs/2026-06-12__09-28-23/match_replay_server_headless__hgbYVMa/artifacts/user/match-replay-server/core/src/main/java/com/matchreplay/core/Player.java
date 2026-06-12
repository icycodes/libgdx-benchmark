package com.matchreplay.core;

public class Player {
    private final int id;
    private final int startX;
    private final int startY;
    private int x;
    private int y;

    public Player(int id, int startX, int startY) {
        this.id = id;
        this.startX = startX;
        this.startY = startY;
        this.x = startX;
        this.y = startY;
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

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
}
