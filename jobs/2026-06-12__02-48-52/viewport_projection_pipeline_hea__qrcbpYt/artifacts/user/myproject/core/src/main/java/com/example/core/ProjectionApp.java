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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * ApplicationAdapter that runs the projection scenario and writes results to
 * the output log file. Designed for HeadlessApplication; OrthographicCamera,
 * Vector2, and Viewport math are plain Java (no GL context needed).
 * Gdx.gl must be set to a no-op GL20 before create() is called so that
 * Viewport.update() does not crash on the glViewport call.
 */
public class ProjectionApp extends ApplicationAdapter {

    private final Scenario scenario;
    private final String   outputPath;

    private Viewport viewport;

    // Accumulated results - PROJECT lines and ROUNDTRIP data stored together.
    private final List<ProjectionResult> results = new ArrayList<>();

    // Counters for the END line
    private int framesProcessed = 0;
    private int pointsProjected = 0;

    // Set to true once all frames have been processed
    private volatile boolean done = false;

    public ProjectionApp(Scenario scenario, String outputPath) {
        this.scenario   = scenario;
        this.outputPath = outputPath;
    }

    public boolean isDone() {
        return done;
    }

    // -----------------------------------------------------------------------
    // ApplicationAdapter lifecycle
    // -----------------------------------------------------------------------

    @Override
    public void create() {
        OrthographicCamera camera = new OrthographicCamera();

        // Instantiate the requested Viewport subclass
        String vtype = scenario.viewport == null ? "fit" : scenario.viewport.toLowerCase(Locale.US);
        switch (vtype) {
            case "extend":
                viewport = new ExtendViewport(scenario.worldWidth, scenario.worldHeight, camera);
                break;
            case "stretch":
                viewport = new StretchViewport(scenario.worldWidth, scenario.worldHeight, camera);
                break;
            case "screen":
                viewport = new ScreenViewport(camera);
                break;
            case "fit":
            default:
                viewport = new FitViewport(scenario.worldWidth, scenario.worldHeight, camera);
                break;
        }

        // Set camera position from scenario (z stays 0)
        if (scenario.cameraPosition != null) {
            camera.position.set(scenario.cameraPosition.x, scenario.cameraPosition.y, 0f);
        }

        // Sort frames by ascending frame number for deterministic processing
        List<Scenario.Frame> frames = new ArrayList<>(scenario.frames);
        frames.sort(Comparator.comparingInt(f -> f.frame));

        // Apply an initial viewport size from the first resize found, so that
        // project() never sees screen-size 0 (which would produce NaN).
        int initW = 800, initH = 480;
        for (Scenario.Frame f : frames) {
            if (f.resize != null) {
                initW = f.resize.width;
                initH = f.resize.height;
                break;
            }
        }
        viewport.update(initW, initH, true);

        // Process every frame in order
        for (Scenario.Frame frame : frames) {
            if (frame.resize != null) {
                viewport.update(frame.resize.width, frame.resize.height, true);
            }

            framesProcessed++;

            List<Scenario.Point> points = frame.points;
            if (points == null) continue;

            for (Scenario.Point p : points) {
                // Project: world -> screen (fresh Vector2 each call)
                Vector2 projected = viewport.project(new Vector2(p.x, p.y));
                float sx = projected.x;
                float sy = projected.y;

                // Round-trip: unproject(project(point)) with fresh Vector2
                Vector2 roundTrip = viewport.unproject(viewport.project(new Vector2(p.x, p.y)));
                float rx = roundTrip.x;
                float ry = roundTrip.y;

                results.add(new ProjectionResult(frame.frame, p.x, p.y, sx, sy, rx, ry));
                pointsProjected++;
            }
        }

        // Write output file
        writeOutput();

        // Signal completion and schedule a clean exit on the main loop thread
        done = true;
        Gdx.app.postRunnable(() -> Gdx.app.exit());
    }

    @Override
    public void render() {
        // Nothing to render - all work is done in create()
    }

    // -----------------------------------------------------------------------
    // Output writing
    // -----------------------------------------------------------------------

    private void writeOutput() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath, false))) {

            // PROJECT lines
            for (ProjectionResult r : results) {
                bw.write(formatProject(r));
                bw.newLine();
            }

            // ROUNDTRIP lines
            for (ProjectionResult r : results) {
                bw.write(formatRoundtrip(r));
                bw.newLine();
            }

            // END line
            bw.write("END frames=" + framesProcessed + " points=" + pointsProjected);
            bw.newLine();

        } catch (IOException e) {
            System.err.println("[ProjectionApp] Failed to write output: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private static String fmt(float v) {
        return String.format(Locale.US, "%.3f", v);
    }

    private static String formatProject(ProjectionResult r) {
        return "FRAME " + r.frame
                + " PROJECT (" + fmt(r.wx) + "," + fmt(r.wy) + ")"
                + " -> (" + fmt(r.sx) + "," + fmt(r.sy) + ")";
    }

    private static String formatRoundtrip(ProjectionResult r) {
        return "ROUNDTRIP " + r.frame
                + " (" + fmt(r.wx) + "," + fmt(r.wy) + ")"
                + " -> (" + fmt(r.rx) + "," + fmt(r.ry) + ")"
                + " " + (r.isOk() ? "OK" : "MISMATCH");
    }
}
