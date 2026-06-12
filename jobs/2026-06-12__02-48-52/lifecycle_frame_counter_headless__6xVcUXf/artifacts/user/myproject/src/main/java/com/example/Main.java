package com.example;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

public class Main {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: Main <N>");
            System.exit(1);
        }

        int n = Integer.parseInt(args[0]);

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 1000;

        FrameCounter listener = new FrameCounter(n);
        HeadlessApplication app = new HeadlessApplication(listener, config);

        // Wait for the headless main loop thread to finish
        Thread appThread = null;
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if ("HeadlessApplication".equals(t.getName())) {
                appThread = t;
                break;
            }
        }

        if (appThread != null) {
            try {
                appThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.exit(0);
    }
}