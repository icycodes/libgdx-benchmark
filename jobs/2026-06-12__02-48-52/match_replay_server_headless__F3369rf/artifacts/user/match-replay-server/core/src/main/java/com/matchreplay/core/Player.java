package com.matchreplay.core;

/**
 * Mutable runtime state of a single player inside the simulation.
 * The id, startX, and startY are recorded for transcript generation.
 */
public class Player {

    public final int id;
    public final int startX;
    public final int startY;

    /** Current position -- mutated by the simulation each tick. */
    public int x;
    public int y;

    public Player(int id, int startX, int startY) {
        this.id     = id;
        this.startX = startX;
        this.startY = startY;
        this.x      = startX;
        this.y      = startY;
    }

    @Override
    public String toString() {
        return "Player{id=" + id + ", x=" + x + ", y=" + y + "}";
    }
}
