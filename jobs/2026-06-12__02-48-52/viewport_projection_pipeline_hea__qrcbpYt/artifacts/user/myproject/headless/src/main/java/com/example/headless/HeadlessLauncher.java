package com.example.headless;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.example.core.ProjectionApp;
import com.example.core.Scenario;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Entry point for the headless viewport-projector tool.
 *
 * Usage (Gradle):
 *   ./gradlew --no-daemon --console=plain headless:run \
 *       --args="--scenario=<abs-path> --output=<abs-path>"
 */
public class HeadlessLauncher {

    public static void main(String[] args) throws Exception {
        String scenarioPath = null;
        String outputPath   = null;

        for (String arg : args) {
            if (arg.startsWith("--scenario=")) {
                scenarioPath = arg.substring("--scenario=".length());
            } else if (arg.startsWith("--output=")) {
                outputPath = arg.substring("--output=".length());
            }
        }

        if (scenarioPath == null || outputPath == null) {
            System.err.println("Usage: --scenario=<path> --output=<path>");
            System.exit(1);
        }

        System.out.println("[HeadlessLauncher] scenario=" + scenarioPath);
        System.out.println("[HeadlessLauncher] output="   + outputPath);

        // Install a no-op GL20 so Viewport.update() -> HdpiUtils.glViewport()
        // does not throw NPE when Gdx.gl is null in the headless backend.
        Gdx.gl   = new MockGL20();
        Gdx.gl20 = new MockGL20();

        // Parse scenario JSON
        Scenario scenario = parseScenario(scenarioPath);

        // Configure and start the headless application
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 60;

        ProjectionApp app = new ProjectionApp(scenario, outputPath);
        new HeadlessApplication(app, config);

        // Poll until ProjectionApp.create() has finished processing all frames
        // and has posted Gdx.app.exit() onto the runnable queue.
        while (!app.isDone()) {
            Thread.sleep(10);
        }

        // Give the postRunnable (Gdx.app.exit()) time to execute so the
        // HeadlessApplication thread stops naturally.
        Thread appThread = findHeadlessThread();
        if (appThread != null) {
            appThread.join(5000);
        }

        System.out.println("[HeadlessLauncher] Done. Exiting with code 0.");
        System.exit(0);
    }

    // -----------------------------------------------------------------------
    // JSON parsing - uses libGDX JsonReader (no extra dependency needed)
    // -----------------------------------------------------------------------

    private static Scenario parseScenario(String path) throws IOException {
        String json = new String(Files.readAllBytes(Paths.get(path)));
        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(json);

        Scenario s = new Scenario();
        s.viewport    = root.getString("viewport", "fit");
        s.worldWidth  = root.getFloat("worldWidth",  800f);
        s.worldHeight = root.getFloat("worldHeight", 480f);

        JsonValue cp = root.get("cameraPosition");
        if (cp != null) {
            s.cameraPosition = new Scenario.CameraPosition();
            s.cameraPosition.x = cp.getFloat("x", 0f);
            s.cameraPosition.y = cp.getFloat("y", 0f);
        }

        s.frames = new ArrayList<>();
        JsonValue framesArr = root.get("frames");
        if (framesArr != null) {
            for (JsonValue fv = framesArr.child; fv != null; fv = fv.next) {
                Scenario.Frame frame = new Scenario.Frame();
                frame.frame = fv.getInt("frame", 0);

                JsonValue rv = fv.get("resize");
                if (rv != null) {
                    frame.resize = new Scenario.Resize();
                    frame.resize.width  = rv.getInt("width",  800);
                    frame.resize.height = rv.getInt("height", 480);
                }

                frame.points = new ArrayList<>();
                JsonValue pts = fv.get("points");
                if (pts != null) {
                    for (JsonValue pv = pts.child; pv != null; pv = pv.next) {
                        Scenario.Point p = new Scenario.Point();
                        p.x = pv.getFloat("x", 0f);
                        p.y = pv.getFloat("y", 0f);
                        frame.points.add(p);
                    }
                }

                s.frames.add(frame);
            }
        }

        return s;
    }

    // -----------------------------------------------------------------------
    // Helper: locate the HeadlessApplication main-loop thread by name
    // -----------------------------------------------------------------------

    private static Thread findHeadlessThread() {
        ThreadGroup root = Thread.currentThread().getThreadGroup();
        while (root.getParent() != null) {
            root = root.getParent();
        }
        Thread[] threads = new Thread[root.activeCount() * 2 + 16];
        int count = root.enumerate(threads, true);
        for (int i = 0; i < count; i++) {
            if (threads[i] != null && "HeadlessApplication".equals(threads[i].getName())) {
                return threads[i];
            }
        }
        return null;
    }
}
