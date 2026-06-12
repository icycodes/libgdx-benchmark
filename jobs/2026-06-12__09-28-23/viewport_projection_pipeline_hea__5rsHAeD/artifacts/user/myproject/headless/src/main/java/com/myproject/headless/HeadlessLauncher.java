package com.myproject.headless;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.myproject.core.ProjectionApp;

public class HeadlessLauncher {
    public static void main(String[] args) {
        String scenarioPath = null;
        String outputPath = null;

        for (String arg : args) {
            if (arg.startsWith("--scenario=")) {
                scenarioPath = arg.substring("--scenario=".length());
            } else if (arg.startsWith("--output=")) {
                outputPath = arg.substring("--output=".length());
            }
        }

        if (scenarioPath == null || outputPath == null) {
            System.err.println("Error: Missing required arguments.");
            System.err.println("Usage: --scenario=<path-to-json> --output=<path-to-log>");
            System.exit(1);
        }

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 60;

        // Boot HeadlessApplication
        new HeadlessApplication(new ProjectionApp(scenarioPath, outputPath), config);

        // Find and join the HeadlessApplication main loop thread to exit cleanly
        Thread mainLoopThread = null;
        for (int i = 0; i < 100; i++) {
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if ("HeadlessApplication".equals(t.getName())) {
                    mainLoopThread = t;
                    break;
                }
            }
            if (mainLoopThread != null) {
                break;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        if (mainLoopThread != null) {
            try {
                mainLoopThread.join();
            } catch (InterruptedException e) {
                System.err.println("Main loop thread join interrupted:");
                e.printStackTrace();
            }
        }

        // Terminate cleanly
        System.exit(0);
    }
}
