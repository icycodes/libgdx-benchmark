package com.example.gdxsplinesampler;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.math.CatmullRomSpline;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SplineSamplerApp {
    private SplineSamplerApp() {
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length != 3) {
            System.err.println("Usage: SplineSamplerApp <spline.json> <input.txt> <output.csv>");
            System.exit(64);
        }

        HeadlessApplicationConfiguration configuration = new HeadlessApplicationConfiguration();
        configuration.updatesPerSecond = 60;

        HeadlessApplication application = new HeadlessApplication(
                new SplineSamplerListener(args[0], args[1], args[2]),
                configuration);

        joinHeadlessMainLoop(application);
    }

    private static void joinHeadlessMainLoop(HeadlessApplication application) throws InterruptedException {
        Thread loopThread = findThreadByReflection(application);
        if (loopThread == null) {
            loopThread = findThreadByName("HeadlessApplication");
        }
        if (loopThread == null) {
            throw new IllegalStateException("Unable to locate the HeadlessApplication main loop thread");
        }
        loopThread.join();
    }

    private static Thread findThreadByReflection(HeadlessApplication application) {
        try {
            java.lang.reflect.Field field = HeadlessApplication.class.getDeclaredField("mainLoopThread");
            field.setAccessible(true);
            Object value = field.get(application);
            return value instanceof Thread ? (Thread) value : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Thread findThreadByName(String name) {
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread != Thread.currentThread() && name.equals(thread.getName())) {
                return thread;
            }
        }
        return null;
    }

    private static final class SplineSamplerListener extends ApplicationAdapter {
        private final Path splineJsonPath;
        private final Path inputPath;
        private final Path outputPath;

        private CatmullRomSpline<Vector2> spline;
        private List<Float> tValues;
        private BufferedWriter writer;
        private int tick;
        private boolean exitRequested;

        private SplineSamplerListener(String splineJsonPath, String inputPath, String outputPath) {
            this.splineJsonPath = Paths.get(splineJsonPath);
            this.inputPath = Paths.get(inputPath);
            this.outputPath = Paths.get(outputPath);
        }

        @Override
        public void create() {
            try {
                spline = loadSpline(splineJsonPath);
                tValues = loadTValues(inputPath);
                writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8);
                writer.write("tick,t,x,y,speed");
                writer.newLine();
            } catch (IOException | RuntimeException ex) {
                closeWriterQuietly();
                throw new IllegalStateException("Failed to initialize spline sampler", ex);
            }
        }

        @Override
        public void render() {
            if (tick >= tValues.size()) {
                requestExitOnce();
                return;
            }

            float t = tValues.get(tick);
            Vector2 position = spline.valueAt(new Vector2(), t);
            Vector2 tangent = spline.derivativeAt(new Vector2(), t);
            float speed = tangent.len();

            try {
                writer.write(String.format(Locale.ROOT, "%d,%.6f,%.6f,%.6f,%.6f", tick, t, position.x, position.y, speed));
                writer.newLine();
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to write CSV row for tick " + tick, ex);
            }

            tick++;
            if (tick >= tValues.size()) {
                requestExitOnce();
            }
        }

        @Override
        public void dispose() {
            closeWriterQuietly();
        }

        private static CatmullRomSpline<Vector2> loadSpline(Path path) throws IOException {
            String jsonText = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            JsonValue root = new JsonReader().parse(jsonText);
            boolean continuous = root.getBoolean("continuous", false);
            JsonValue controlPointsJson = root.get("controlPoints");
            if (controlPointsJson == null || !controlPointsJson.isArray()) {
                throw new IllegalArgumentException("spline.json must contain a controlPoints array");
            }

            List<Vector2> points = new ArrayList<>();
            for (JsonValue pointJson = controlPointsJson.child; pointJson != null; pointJson = pointJson.next) {
                points.add(new Vector2(pointJson.getFloat("x"), pointJson.getFloat("y")));
            }
            if (points.size() < 4) {
                throw new IllegalArgumentException("controlPoints must contain at least four entries");
            }

            @SuppressWarnings("unchecked")
            Vector2[] pointArray = points.toArray(new Vector2[0]);
            return new CatmullRomSpline<>(pointArray, continuous);
        }

        private static List<Float> loadTValues(Path path) throws IOException {
            List<Float> values = new ArrayList<>();
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    values.add(Float.parseFloat(trimmed));
                }
            }
            return values;
        }

        private void requestExitOnce() {
            if (!exitRequested) {
                exitRequested = true;
                try {
                    writer.flush();
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed to flush CSV output", ex);
                }
                Gdx.app.exit();
            }
        }

        private void closeWriterQuietly() {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                    // Best-effort cleanup during application shutdown.
                } finally {
                    writer = null;
                }
            }
        }
    }
}
