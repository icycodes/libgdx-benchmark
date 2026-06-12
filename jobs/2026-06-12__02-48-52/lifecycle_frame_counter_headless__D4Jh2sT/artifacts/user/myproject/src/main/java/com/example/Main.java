package com.example;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

import java.lang.reflect.Field;

/**
 * Launcher for the headless libGDX frame-counter application.
 *
 * Usage: Main &lt;N&gt;
 *   N - number of render frames to execute before the application exits.
 *
 * The launcher:
 *   1. Parses N from the first command-line argument.
 *   2. Configures HeadlessApplicationConfiguration with a fast tick rate so
 *      the loop completes quickly without stalling.
 *   3. Constructs HeadlessApplication, which immediately starts its own
 *      thread named "HeadlessApplication" (stored in mainLoopThread field).
 *   4. Retrieves that thread via reflection on the mainLoopThread field and
 *      join()s it so the JVM does not exit before dispose() has printed the
 *      final FRAME_COUNT line.
 *   5. Exits with code 0.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: Main <N>");
            System.exit(1);
        }

        final int n = Integer.parseInt(args[0].trim());

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        // 1 000 ticks/second - even N=1 000 finishes in ~1 second of wall time.
        config.updatesPerSecond = 1000;

        // Constructing HeadlessApplication spins up the render loop on a
        // background thread and returns immediately.
        HeadlessApplication app = new HeadlessApplication(new FrameCounterApp(n), config);

        // Retrieve the mainLoopThread field from HeadlessApplication and join
        // it so the process does not exit before dispose() runs.
        Thread mainLoopThread = getMainLoopThread(app);
        if (mainLoopThread != null) {
            mainLoopThread.join();
        } else {
            // Fallback: scan all threads for the well-known name.
            Thread t = findHeadlessThread(5_000);
            if (t != null) {
                t.join();
            }
        }

        // dispose() has now been called; FRAME_COUNT has been printed.
        System.exit(0);
    }

    /** Reads HeadlessApplication.mainLoopThread via reflection. */
    private static Thread getMainLoopThread(HeadlessApplication app) {
        try {
            Field f = HeadlessApplication.class.getDeclaredField("mainLoopThread");
            f.setAccessible(true);
            return (Thread) f.get(app);
        } catch (Exception e) {
            System.err.println("Warning: could not read mainLoopThread field: " + e);
            return null;
        }
    }

    /**
     * Fallback: polls all live threads for one named "HeadlessApplication"
     * for up to timeoutMs milliseconds.
     */
    private static Thread findHeadlessThread(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if ("HeadlessApplication".equals(t.getName())) {
                    return t;
                }
            }
            Thread.sleep(5);
        }
        System.err.println("Warning: HeadlessApplication thread not found within timeout.");
        return null;
    }
}
