package com.example.scene;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

/**
 * Entry point. Parses --scene and --output CLI arguments, then boots a
 * HeadlessApplication that drives the scene-graph loader lifecycle.
 */
public class Main {

    public static void main(String[] args) {
        String scenePath = null;
        String outputPath = null;

        for (String arg : args) {
            if (arg.startsWith("--scene=")) {
                scenePath = arg.substring("--scene=".length());
            } else if (arg.startsWith("--output=")) {
                outputPath = arg.substring("--output=".length());
            }
        }

        if (scenePath == null || outputPath == null) {
            System.err.println("Usage: --scene=<path> --output=<path>");
            System.exit(1);
        }

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        // Run as fast as possible; we only need a single update cycle.
        config.updatesPerSecond = 0;

        SceneLoaderApp app = new SceneLoaderApp(scenePath, outputPath);

        // HeadlessApplication spawns an internal main-loop thread; we keep
        // a reference so the JVM doesn't exit before it joins.
        HeadlessApplication headless = new HeadlessApplication(app, config);

        // Wait for the application's main loop thread to finish.
        // HeadlessApplication#getMainLoopThread() returns the Thread;
        // we wait on it by joining via the application itself.
        // The app calls Gdx.app.exit() from create(), which signals the loop
        // to stop; the thread then terminates and the process can exit cleanly.
        synchronized (app) {
            while (!app.isFinished()) {
                try {
                    app.wait(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Exit with code 0.
        System.exit(0);
    }
}
