package com.example.core;

/**
 * Holds the result of projecting one world-space point and its round-trip.
 */
public class ProjectionResult {
    public final int   frame;
    public final float wx, wy;
    public final float sx, sy;
    public final float rx, ry;

    public ProjectionResult(int frame, float wx, float wy,
                            float sx, float sy,
                            float rx, float ry) {
        this.frame = frame;
        this.wx = wx;
        this.wy = wy;
        this.sx = sx;
        this.sy = sy;
        this.rx = rx;
        this.ry = ry;
    }

    public boolean isOk() {
        return Math.abs(rx - wx) <= 0.01f && Math.abs(ry - wy) <= 0.01f;
    }
}
