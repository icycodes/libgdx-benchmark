package com.gdxgame;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

/**
 * Launcher entry-point.
 *
 * Usage: gdx-game --config=<absolute-path-to-json>
 *
 * The launcher:
 *  1. Parses the --config=<path> argument.
 *  2. Starts a HeadlessApplication that runs ConfigSummaryApp on its own thread.
 *  3. Blocks (joins) the headless main-loop thread so all output is flushed
 *     before the JVM exits.
 *  4. Exits with status 0 on success, non-zero on failure.
 */
public class Main {

    public static void main(String[] args) {
        // --- parse --config=<path> -------------------------------------------
        String configPath = null;
        for (String arg : args) {
            if (arg.startsWith("--config=")) {
                configPath = arg.substring("--config=".length());
                break;
            }
        }

        if (configPath == null || configPath.isEmpty()) {
            System.err.println("Error: invalid config: --config=<path> argument is required");
            System.exit(1);
        }

        // --- boot the headless application -----------------------------------
        HeadlessApplicationConfiguration cfg = new HeadlessApplicationConfiguration();
        // -1 means run the render loop as fast as possible; we exit in create()
        // so a single iteration is fine.  Using 0 would busy-spin, -1 is safer.
        cfg.updatesPerSecond = -1;

        ConfigSummaryApp app = new ConfigSummaryApp(configPath);

        HeadlessApplication headless = new HeadlessApplication(app, cfg);

        // --- wait for the headless loop to finish ----------------------------
        // HeadlessApplication runs on the thread named "HeadlessApplication".
        // We look it up by name and join it so the main thread blocks until
        // Gdx.app.exit() has been processed and all output has been written.
        Thread headlessThread = null;
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if ("HeadlessApplication".equals(t.getName())) {
                headlessThread = t;
                break;
            }
        }

        if (headlessThread != null) {
            try {
                headlessThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            // Fallback: sleep briefly to let the single-iteration loop finish.
            // This path is almost never taken in practice.
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // --- exit with appropriate status code --------------------------------
        if (app.failed) {
            System.exit(1);
        }
        System.exit(0);
    }
}
