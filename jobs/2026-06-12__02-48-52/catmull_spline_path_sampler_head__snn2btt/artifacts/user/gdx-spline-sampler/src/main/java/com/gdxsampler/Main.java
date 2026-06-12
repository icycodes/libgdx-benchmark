package com.gdxsampler;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

/**
 * Entry point for the headless CatmullRom spline sampler.
 *
 * Usage: Main <spline.json> <input.txt> <output.csv>
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        if (args.length != 3) {
            System.err.println("Usage: Main <spline.json> <input.txt> <output.csv>");
            System.exit(1);
        }

        String splineJson = args[0];
        String inputTxt   = args[1];
        String outputCsv  = args[2];

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 60;

        SplineSamplerApp listener = new SplineSamplerApp(splineJson, inputTxt, outputCsv);

        // Boot the headless application; the constructor starts the main loop
        // thread named "HeadlessApplication".
        HeadlessApplication app = new HeadlessApplication(listener, config);

        // Find the headless main-loop thread and join it so main() blocks until
        // Gdx.app.exit() has been called and the loop has terminated.
        Thread headlessThread = null;
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if ("HeadlessApplication".equals(t.getName())) {
                headlessThread = t;
                break;
            }
        }

        if (headlessThread != null) {
            headlessThread.join();
        } else {
            // Fallback: poll until the application signals it has stopped
            // by checking whether the thread appeared and vanished already.
            // In practice the constructor guarantees the thread exists before
            // returning, so this branch should never be reached.
            Thread.sleep(500);
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if ("HeadlessApplication".equals(t.getName())) {
                    t.join();
                    break;
                }
            }
        }
    }
}
