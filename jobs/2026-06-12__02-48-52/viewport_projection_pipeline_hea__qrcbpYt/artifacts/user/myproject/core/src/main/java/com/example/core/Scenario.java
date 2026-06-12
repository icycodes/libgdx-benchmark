package com.example.core;

import java.util.List;

/**
 * Plain-Java representation of the JSON scenario file.
 * Field names match the JSON keys so libGDX Json can auto-populate them.
 */
public class Scenario {

    public String viewport;      // "fit" | "extend" | "stretch" | "screen"
    public float  worldWidth;
    public float  worldHeight;
    public CameraPosition cameraPosition;
    public List<Frame> frames;

    public static class CameraPosition {
        public float x;
        public float y;
    }

    public static class Frame {
        public int     frame;
        public Resize  resize;           // may be null
        public List<Point> points;       // may be empty/null
    }

    public static class Resize {
        public int width;
        public int height;
    }

    public static class Point {
        public float x;
        public float y;
    }
}
