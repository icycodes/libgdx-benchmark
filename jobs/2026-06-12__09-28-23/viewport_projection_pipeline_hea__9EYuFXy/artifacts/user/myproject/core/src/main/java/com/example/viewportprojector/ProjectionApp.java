package com.example.viewportprojector;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

/**
 * Headless-safe libGDX application that replays viewport resize/project events and writes
 * deterministic projection logs.
 */
public final class ProjectionApp extends ApplicationAdapter {
    private final Scenario scenario;
    private final Path outputPath;
    private final CountDownLatch completionLatch = new CountDownLatch(1);
    private final List<String> projectLines = new ArrayList<>();
    private final List<ProjectedPoint> projectedPoints = new ArrayList<>();

    private OrthographicCamera camera;
    private Viewport viewport;
    private int nextFrameIndex;
    private boolean finished;
    private Throwable failure;

    public ProjectionApp(Scenario scenario, String outputPath) {
        this.scenario = scenario;
        this.outputPath = Paths.get(outputPath);
    }

    @Override
    public void create() {
        try {
            installNoopGlIfNeeded();

            camera = new OrthographicCamera();
            viewport = createViewport(scenario, camera);

            Scenario.Resize initialResize = scenario.firstResize();
            int initialWidth = initialResize != null ? initialResize.width : Math.max(1, Math.round(scenario.worldWidth));
            int initialHeight = initialResize != null ? initialResize.height : Math.max(1, Math.round(scenario.worldHeight));
            viewport.update(initialWidth, initialHeight, true);

            camera.position.set(scenario.cameraPosition.x, scenario.cameraPosition.y, 0f);
            camera.update();
        } catch (Throwable throwable) {
            failAndExit(throwable);
        }
    }

    @Override
    public void render() {
        if (finished) {
            return;
        }

        try {
            if (nextFrameIndex < scenario.frames.size()) {
                processFrame(scenario.frames.get(nextFrameIndex++));
            } else {
                writeOutputAndExit();
            }
        } catch (Throwable throwable) {
            failAndExit(throwable);
        }
    }

    public void awaitCompletion() throws InterruptedException {
        completionLatch.await();
    }

    public Throwable getFailure() {
        return failure;
    }

    private void processFrame(Scenario.Frame frame) {
        if (frame.resize != null) {
            viewport.update(frame.resize.width, frame.resize.height, true);
        }

        for (Scenario.Point point : frame.points) {
            Vector2 screen = viewport.project(new Vector2(point.x, point.y));
            projectLines.add(String.format(
                    Locale.US,
                    "FRAME %d PROJECT (%.3f,%.3f) -> (%.3f,%.3f)",
                    frame.frame,
                    point.x,
                    point.y,
                    screen.x,
                    screen.y));
            projectedPoints.add(new ProjectedPoint(frame.frame, point.x, point.y));
        }
    }

    private void writeOutputAndExit() throws IOException {
        finished = true;

        List<String> lines = new ArrayList<>(projectLines);
        for (ProjectedPoint point : projectedPoints) {
            Vector2 roundTrip = viewport.unproject(viewport.project(new Vector2(point.worldX, point.worldY)));
            String status = Math.abs(roundTrip.x - point.worldX) <= 0.01f
                    && Math.abs(roundTrip.y - point.worldY) <= 0.01f
                    ? "OK"
                    : "MISMATCH";
            lines.add(String.format(
                    Locale.US,
                    "ROUNDTRIP %d (%.3f,%.3f) -> (%.3f,%.3f) %s",
                    point.frame,
                    point.worldX,
                    point.worldY,
                    roundTrip.x,
                    roundTrip.y,
                    status));
        }
        lines.add(String.format(Locale.US, "END frames=%d points=%d", scenario.frames.size(), projectedPoints.size()));

        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(
                outputPath,
                lines,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);

        completionLatch.countDown();
        Gdx.app.postRunnable(() -> Gdx.app.exit());
    }

    private void failAndExit(Throwable throwable) {
        failure = throwable;
        finished = true;
        completionLatch.countDown();
        if (Gdx.app != null) {
            Gdx.app.postRunnable(() -> Gdx.app.exit());
        }
    }

    private static void installNoopGlIfNeeded() {
        if (Gdx.gl != null && Gdx.gl20 != null) {
            return;
        }

        GL20 noopGl = (GL20) Proxy.newProxyInstance(
                GL20.class.getClassLoader(),
                new Class<?>[]{GL20.class},
                (proxy, method, args) -> {
                    Class<?> returnType = method.getReturnType();
                    if (returnType == Void.TYPE) {
                        return null;
                    }
                    if (returnType == Boolean.TYPE) {
                        return false;
                    }
                    if (returnType == Byte.TYPE) {
                        return (byte) 0;
                    }
                    if (returnType == Short.TYPE) {
                        return (short) 0;
                    }
                    if (returnType == Integer.TYPE) {
                        return 0;
                    }
                    if (returnType == Long.TYPE) {
                        return 0L;
                    }
                    if (returnType == Float.TYPE) {
                        return 0f;
                    }
                    if (returnType == Double.TYPE) {
                        return 0d;
                    }
                    if (returnType == Character.TYPE) {
                        return (char) 0;
                    }
                    return null;
                });
        Gdx.gl = noopGl;
        Gdx.gl20 = noopGl;
    }

    private static Viewport createViewport(Scenario scenario, OrthographicCamera camera) {
        switch (scenario.viewport.toLowerCase(Locale.ROOT)) {
            case "fit":
                return new FitViewport(scenario.worldWidth, scenario.worldHeight, camera);
            case "extend":
                return new ExtendViewport(scenario.worldWidth, scenario.worldHeight, camera);
            case "stretch":
                return new StretchViewport(scenario.worldWidth, scenario.worldHeight, camera);
            case "screen":
                return new ScreenViewport(camera);
            default:
                throw new IllegalArgumentException("Unsupported viewport type: " + scenario.viewport);
        }
    }

    private static final class ProjectedPoint {
        private final int frame;
        private final float worldX;
        private final float worldY;

        private ProjectedPoint(int frame, float worldX, float worldY) {
            this.frame = frame;
            this.worldX = worldX;
            this.worldY = worldY;
        }
    }
}
