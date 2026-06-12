package com.example.headless;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.example.core.ProjectionApp;

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
            System.err.println("Usage: --scenario=<path> --output=<path>");
            System.exit(1);
        }
        
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 60;
        
        ProjectionApp app = new ProjectionApp(scenarioPath, outputPath);
        new HeadlessApplication(app, config);
        
        // Join the main loop thread to ensure clean exit
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if ("HeadlessApplication".equals(t.getName())) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }
}
