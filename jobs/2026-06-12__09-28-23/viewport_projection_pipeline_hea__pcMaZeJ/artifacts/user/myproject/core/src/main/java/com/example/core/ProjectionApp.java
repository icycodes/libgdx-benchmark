package com.example.core;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProjectionApp extends ApplicationAdapter {

    private final Scenario scenario;
    private final String outputPath;

    private Viewport viewport;
    private OrthographicCamera camera;

    // Accumulated log lines: PROJECT lines first, then ROUNDTRIP lines, then END line.
    private final List<String> projectLines = new ArrayList<>();
    private final List<RoundTripEntry> roundTripEntries = new ArrayList<>();

    private int totalPoints = 0;
    private int frameIndex = 0;
    private boolean lastFrameResizeApplied = false;

    public ProjectionApp(Scenario scenario, String outputPath) {
        this.scenario = scenario;
        this.outputPath = outputPath;
    }

    @Override
    public void create() {
        camera = new OrthographicCamera();
        camera.position.set(
            (float) scenario.cameraPosition.x,
            (float) scenario.cameraPosition.y,
            0
        );

        float worldWidth = (float) scenario.worldWidth;
        float worldHeight = (float) scenario.worldHeight;

        switch (scenario.viewport) {
            case "fit":
                viewport = new FitViewport(worldWidth, worldHeight, camera);
                break;
            case "extend":
                viewport = new ExtendViewport(worldWidth, worldHeight, camera);
                break;
            case "stretch":
                viewport = new StretchViewport(worldWidth, worldHeight, camera);
                break;
            case "screen":
                viewport = new ScreenViewport(camera);
                break;
            default:
                throw new IllegalArgumentException("Unknown viewport type: " + scenario.viewport);
        }

        // Apply initial resize using the first frame's resize dimensions if available,
        // otherwise default to worldWidth/worldHeight.
        if (!scenario.frames.isEmpty() && scenario.frames.get(0).resize != null) {
            Scenario.Resize r = scenario.frames.get(0).resize;
            viewport.update(r.width, r.height, true);
        } else {
            viewport.update((int) worldWidth, (int) worldHeight, true);
        }
    }

    @Override
    public void render() {
        if (frameIndex >= scenario.frames.size()) {
            // All frames processed: write output and exit.
            writeOutput();
            Gdx.app.postRunnable(() -> Gdx.app.exit());
            return;
        }

        Scenario.Frame frame = scenario.frames.get(frameIndex);

        // Apply resize for this frame if present. Note: the FIRST frame's resize
        // was already applied in create(). If the first frame has a resize, we
        // should re-apply it here to match the spec: "when resize is present, call
        // viewport.update BEFORE projecting that frame's points."
        // Actually, re-reading the spec: the first frame's resize is applied in
        // create() as the "sensible default". But the spec also says for each frame
        // "when resize is present, call viewport.update BEFORE projecting". So for
        // the first frame, if it has a resize, we call update again (same dimensions,
        // idempotent). For subsequent frames with resizes, this is the real update.
        if (frame.resize != null) {
            viewport.update(frame.resize.width, frame.resize.height, true);
            lastFrameResizeApplied = true;
        } else {
            lastFrameResizeApplied = false;
        }

        // Project all points in this frame.
        if (frame.points != null) {
            for (Scenario.Point p : frame.points) {
                Vector2 world = new Vector2((float) p.x, (float) p.y);
                Vector2 screen = viewport.project(world);

                String line = String.format(Locale.US,
                    "FRAME %d PROJECT (%.3f,%.3f) -> (%.3f,%.3f)",
                    frame.frame, p.x, p.y, screen.x, screen.y);
                projectLines.add(line);

                roundTripEntries.add(new RoundTripEntry(frame.frame, p.x, p.y));
                totalPoints++;
            }
        }

        frameIndex++;
    }

    private void writeOutput() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            // Write all PROJECT lines.
            for (String line : projectLines) {
                writer.write(line);
                writer.newLine();
            }

            // Write all ROUNDTRIP lines.
            for (RoundTripEntry entry : roundTripEntries) {
                Vector2 world = new Vector2((float) entry.wx, (float) entry.wy);
                Vector2 screen = viewport.project(new Vector2(world));
                Vector2 unprojected = viewport.unproject(screen);

                double rx = unprojected.x;
                double ry = unprojected.y;
                String status = (Math.abs(rx - entry.wx) <= 0.01 && Math.abs(ry - entry.wy) <= 0.01)
                    ? "OK" : "MISMATCH";

                String line = String.format(Locale.US,
                    "ROUNDTRIP %d (%.3f,%.3f) -> (%.3f,%.3f) %s",
                    entry.frame, entry.wx, entry.wy, rx, ry, status);
                writer.write(line);
                writer.newLine();
            }

            // Write END line.
            writer.write(String.format(Locale.US,
                "END frames=%d points=%d", scenario.frames.size(), totalPoints));
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write output file: " + outputPath, e);
        }
    }

    private static class RoundTripEntry {
        final int frame;
        final double wx;
        final double wy;

        RoundTripEntry(int frame, double wx, double wy) {
            this.frame = frame;
            this.wx = wx;
            this.wy = wy;
        }
    }
}
