package com.example.matchreplay.core;

public final class PlayerStart {
    private final int id;
    private final int x;
    private final int y;

    public PlayerStart(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public int getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
