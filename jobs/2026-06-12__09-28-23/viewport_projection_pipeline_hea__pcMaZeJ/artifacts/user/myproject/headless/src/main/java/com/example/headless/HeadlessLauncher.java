package com.example.headless;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.utils.Json;
import com.example.core.ProjectionApp;
import com.example.core.Scenario;

import java.io.FileReader;
import java.io.IOException;

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

        // Parse the scenario JSON.
        Scenario scenario;
        try {
            Json json = new Json();
            scenario = json.fromJson(Scenario.class, new FileReader(scenarioPath));
        } catch (IOException e) {
            System.err.println("Failed to read scenario file: " + scenarioPath);
            e.printStackTrace();
            System.exit(1);
            return;
        }

        // Create the application listener.
        ProjectionApp app = new ProjectionApp(scenario, outputPath);

        // Configure and start the headless application.
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 60;

        HeadlessApplication application = new HeadlessApplication(app, config);

        // Wait for the headless main-loop thread to finish.
        // The app calls Gdx.app.postRunnable(() -> Gdx.app.exit()) after writing output.
        try {
            // The headless backend runs on a thread named "HeadlessApplication".
            // We need to find and join it.
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if ("HeadlessApplication".equals(t.getName())) {
                    t.join();
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
