package com.myproject.core;

public class Scenario {
    public String viewport;
    public float worldWidth;
    public float worldHeight;
    public CameraPosition cameraPosition;
    public Frame[] frames;

    public static class CameraPosition {
        public float x;
        public float y;
    }

    public static class Frame {
        public int frame;
        public Resize resize;
        public Point[] points;
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
