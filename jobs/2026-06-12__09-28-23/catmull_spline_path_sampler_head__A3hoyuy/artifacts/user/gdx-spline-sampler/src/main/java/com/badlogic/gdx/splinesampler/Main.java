package com.badlogic.gdx.splinesampler;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

public class Main {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: java com.badlogic.gdx.splinesampler.Main <spline.json> <input.txt> <output.csv>");
            System.exit(1);
        }

        String splinePath = args[0];
        String inputPath = args[1];
        String outputPath = args[2];

        // Create configuration
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 60;

        // Create listener
        SplineSamplerListener listener = new SplineSamplerListener(splinePath, inputPath, outputPath);

        // Boot headless application
        new HeadlessApplication(listener, config);

        // Wait for HeadlessApplication main loop thread to terminate before returning
        Thread headlessThread = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            Thread[] threads = new Thread[Thread.activeCount() * 2];
            int count = Thread.enumerate(threads);
            for (int i = 0; i < count; i++) {
                if (threads[i] != null && "HeadlessApplication".equals(threads[i].getName())) {
                    headlessThread = threads[i];
                    break;
                }
            }
            if (headlessThread != null) {
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (headlessThread != null) {
            try {
                headlessThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
