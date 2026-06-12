package com.example.spline;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

public class Main {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: java -jar gdx-spline-sampler.jar <spline.json> <input.txt> <output.csv>");
            System.exit(1);
        }

        String splineJsonPath = args[0];
        String inputTxtPath = args[1];
        String outputCsvPath = args[2];

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 60;

        SplineSampler listener = new SplineSampler(splineJsonPath, inputTxtPath, outputCsvPath);
        HeadlessApplication app = new HeadlessApplication(listener, config);

        // Wait for the headless main loop thread to terminate
        try {
            Thread appThread = findHeadlessThread();
            if (appThread != null) {
                appThread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Main thread interrupted while waiting for headless loop to finish.");
        }
    }

    /**
     * Finds the HeadlessApplication main loop thread by name.
     * The HeadlessApplication constructor starts a thread named "HeadlessApplication".
     */
    private static Thread findHeadlessThread() {
        Thread[] threads = new Thread[Thread.activeCount() * 2];
        int count = Thread.enumerate(threads);
        for (int i = 0; i < count; i++) {
            if ("HeadlessApplication".equals(threads[i].getName())) {
                return threads[i];
            }
        }
        return null;
    }
}
