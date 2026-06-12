package com.example.viewportprojector;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Stable input model for viewport-projector JSON scenarios. */
public final class Scenario {
    public final String viewport;
    public final float worldWidth;
    public final float worldHeight;
    public final Point cameraPosition;
    public final List<Frame> frames;

    private Scenario(String viewport, float worldWidth, float worldHeight, Point cameraPosition, List<Frame> frames) {
        this.viewport = viewport;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.cameraPosition = cameraPosition;
        this.frames = frames;
    }

    public static Scenario load(String path) {
        JsonValue root = new JsonReader().parse(new FileHandle(path));
        String viewport = root.getString("viewport");
        float worldWidth = root.getFloat("worldWidth");
        float worldHeight = root.getFloat("worldHeight");

        JsonValue cameraValue = require(root, "cameraPosition");
        Point cameraPosition = new Point(cameraValue.getFloat("x"), cameraValue.getFloat("y"));

        List<Frame> frames = new ArrayList<>();
        JsonValue framesValue = require(root, "frames");
        for (JsonValue frameValue = framesValue.child; frameValue != null; frameValue = frameValue.next) {
            int frameNumber = frameValue.getInt("frame");

            Resize resize = null;
            JsonValue resizeValue = frameValue.get("resize");
            if (resizeValue != null && !resizeValue.isNull()) {
                resize = new Resize(resizeValue.getInt("width"), resizeValue.getInt("height"));
            }

            List<Point> points = new ArrayList<>();
            JsonValue pointsValue = frameValue.get("points");
            if (pointsValue != null && !pointsValue.isNull()) {
                for (JsonValue pointValue = pointsValue.child; pointValue != null; pointValue = pointValue.next) {
                    points.add(new Point(pointValue.getFloat("x"), pointValue.getFloat("y")));
                }
            }

            frames.add(new Frame(frameNumber, resize, points));
        }

        frames.sort(Comparator.comparingInt(frame -> frame.frame));
        return new Scenario(viewport, worldWidth, worldHeight, cameraPosition, frames);
    }

    private static JsonValue require(JsonValue root, String name) {
        JsonValue value = root.get(name);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("Missing required JSON field: " + name);
        }
        return value;
    }

    public Resize firstResize() {
        for (Frame frame : frames) {
            if (frame.resize != null) {
                return frame.resize;
            }
        }
        return null;
    }

    public static final class Frame {
        public final int frame;
        public final Resize resize;
        public final List<Point> points;

        private Frame(int frame, Resize resize, List<Point> points) {
            this.frame = frame;
            this.resize = resize;
            this.points = points;
        }
    }

    public static final class Resize {
        public final int width;
        public final int height;

        private Resize(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    public static final class Point {
        public final float x;
        public final float y;

        private Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
