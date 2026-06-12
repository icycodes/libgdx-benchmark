package com.myproject.core;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.List;

public class ProjectionApp extends ApplicationAdapter {
    private final String scenarioPath;
    private final String outputPath;

    private Scenario scenario;
    private OrthographicCamera camera;
    private Viewport viewport;

    private int frameIndex = 0;
    private final List<String> projectLines = new ArrayList<>();
    private final List<RoundTripItem> allPointsToRoundTrip = new ArrayList<>();
    private int totalPointsCount = 0;
    private boolean finished = false;

    private static class RoundTripItem {
        int frameNum;
        Vector2 worldPoint;

        RoundTripItem(int frameNum, Vector2 worldPoint) {
            this.frameNum = frameNum;
            this.worldPoint = worldPoint;
        }
    }

    private static class SymmetricalCamera extends OrthographicCamera {
        @Override
        public Vector3 unproject(Vector3 screenCoords, float viewportX, float viewportY, float viewportWidth, float viewportHeight) {
            float x = screenCoords.x - viewportX;
            float y = screenCoords.y - viewportY;
            screenCoords.x = (2 * x) / viewportWidth - 1;
            screenCoords.y = (2 * y) / viewportHeight - 1;
            screenCoords.z = 2 * screenCoords.z - 1;
            screenCoords.prj(invProjectionView);
            return screenCoords;
        }
    }

    public ProjectionApp(String scenarioPath, String outputPath) {
        this.scenarioPath = scenarioPath;
        this.outputPath = outputPath;
    }

    @Override
    public void create() {
        try {
            // Mock GL20 to prevent NullPointerExceptions in headless mode when viewports call glViewport
            GL20 mockGL = (GL20) Proxy.newProxyInstance(
                    GL20.class.getClassLoader(),
                    new Class[]{GL20.class},
                    (proxy, method, methodArgs) -> {
                        Class<?> returnType = method.getReturnType();
                        if (returnType == void.class) return null;
                        if (returnType == boolean.class) return false;
                        if (returnType == int.class) return 0;
                        if (returnType == float.class) return 0f;
                        return null;
                    }
            );
            Gdx.gl = mockGL;
            Gdx.gl20 = mockGL;

            // Mock Graphics to return correct viewport dimensions for unproject calculations
            final Graphics originalGraphics = Gdx.graphics;
            Graphics mockGraphics = (Graphics) Proxy.newProxyInstance(
                    Graphics.class.getClassLoader(),
                    new Class[]{Graphics.class},
                    (proxy, method, methodArgs) -> {
                        if ("getWidth".equals(method.getName())) {
                            return viewport != null ? viewport.getScreenWidth() : (originalGraphics != null ? originalGraphics.getWidth() : 0);
                        }
                        if ("getHeight".equals(method.getName())) {
                            return viewport != null ? viewport.getScreenHeight() : (originalGraphics != null ? originalGraphics.getHeight() : 0);
                        }
                        if (originalGraphics != null) {
                            return method.invoke(originalGraphics, methodArgs);
                        }
                        return null;
                    }
            );
            Gdx.graphics = mockGraphics;

            // Read and parse scenario
            Json json = new Json();
            scenario = json.fromJson(Scenario.class, Gdx.files.absolute(scenarioPath));

            if (scenario.frames != null) {
                Arrays.sort(scenario.frames, (f1, f2) -> Integer.compare(f1.frame, f2.frame));
            }

            // Create camera
            camera = new SymmetricalCamera();

            // Create viewport
            if ("fit".equalsIgnoreCase(scenario.viewport)) {
                viewport = new FitViewport(scenario.worldWidth, scenario.worldHeight, camera);
            } else if ("extend".equalsIgnoreCase(scenario.viewport)) {
                viewport = new ExtendViewport(scenario.worldWidth, scenario.worldHeight, camera);
            } else if ("stretch".equalsIgnoreCase(scenario.viewport)) {
                viewport = new StretchViewport(scenario.worldWidth, scenario.worldHeight, camera);
            } else if ("screen".equalsIgnoreCase(scenario.viewport)) {
                viewport = new ScreenViewport(camera);
            } else {
                throw new IllegalArgumentException("Unknown viewport type: " + scenario.viewport);
            }

            // Sensible default for first update
            int initialWidth = 800;
            int initialHeight = 600;
            if (scenario.frames != null) {
                for (Scenario.Frame f : scenario.frames) {
                    if (f.resize != null) {
                        initialWidth = f.resize.width;
                        initialHeight = f.resize.height;
                        break;
                    }
                }
            }
            viewport.update(initialWidth, initialHeight, true);

            // Apply cameraPosition once during create()
            if (scenario.cameraPosition != null) {
                camera.position.set(scenario.cameraPosition.x, scenario.cameraPosition.y, 0f);
                camera.update();
            }

        } catch (Exception e) {
            System.err.println("Error initializing ProjectionApp:");
            e.printStackTrace();
            Gdx.app.exit();
        }
    }

    @Override
    public void render() {
        if (finished) return;

        if (scenario != null && scenario.frames != null && frameIndex < scenario.frames.length) {
            Scenario.Frame currentFrame = scenario.frames[frameIndex];

            if (currentFrame.resize != null) {
                viewport.update(currentFrame.resize.width, currentFrame.resize.height, true);
            }

            if (currentFrame.points != null) {
                for (Scenario.Point p : currentFrame.points) {
                    Vector2 worldPoint = new Vector2(p.x, p.y);
                    Vector2 screenPoint = viewport.project(new Vector2(worldPoint));

                    String line = String.format(Locale.US, "FRAME %d PROJECT (%.3f,%.3f) -> (%.3f,%.3f)",
                            currentFrame.frame, worldPoint.x, worldPoint.y, screenPoint.x, screenPoint.y);
                    projectLines.add(line);

                    allPointsToRoundTrip.add(new RoundTripItem(currentFrame.frame, worldPoint));
                    totalPointsCount++;
                }
            }

            frameIndex++;
        } else {
            finished = true;
            writeOutputAndExit();
        }
    }

    private void writeOutputAndExit() {
        try {
            File file = new File(outputPath);
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }

            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                // Write all project lines
                for (String line : projectLines) {
                    writer.println(line);
                }

                // Write all round-trip lines
                for (RoundTripItem item : allPointsToRoundTrip) {
                    Vector2 testVec = new Vector2(item.worldPoint);
                    viewport.project(testVec);
                    viewport.unproject(testVec);

                    float rx = testVec.x;
                    float ry = testVec.y;
                    float wx = item.worldPoint.x;
                    float wy = item.worldPoint.y;

                    String status = (Math.abs(rx - wx) <= 0.01f && Math.abs(ry - wy) <= 0.01f) ? "OK" : "MISMATCH";

                    String line = String.format(Locale.US, "ROUNDTRIP %d (%.3f,%.3f) -> (%.3f,%.3f) %s",
                            item.frameNum, wx, wy, rx, ry, status);
                    writer.println(line);
                }

                // Write the END line
                int numFrames = (scenario != null && scenario.frames != null) ? scenario.frames.length : 0;
                writer.println(String.format(Locale.US, "END frames=%d points=%d", numFrames, totalPointsCount));
            }
        } catch (Exception e) {
            System.err.println("Error writing output file:");
            e.printStackTrace();
        } finally {
            Gdx.app.postRunnable(() -> Gdx.app.exit());
        }
    }
}
