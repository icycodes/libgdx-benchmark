package com.example.matchreplay.core;

public final class WorldSpec {
    private final int width;
    private final int height;

    public WorldSpec(int width, int height) {
        if (width < 1 || width > 64 || height < 1 || height > 64) {
            throw new IllegalArgumentException("World dimensions must be in [1, 64]");
        }
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
