package com.example.core;

import java.util.List;

public class Scenario {
    public String viewport;
    public double worldWidth;
    public double worldHeight;
    public CameraPosition cameraPosition;
    public List<Frame> frames;

    public static class CameraPosition {
        public double x;
        public double y;
    }

    public static class Frame {
        public int frame;
        public Resize resize;
        public List<Point> points;
    }

    public static class Resize {
        public int width;
        public int height;
    }

    public static class Point {
        public double x;
        public double y;
    }
}
