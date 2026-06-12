package com.gdxgame;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

public class Main {

    public static void main(String[] args) {
        // Parse --scenario=<file> argument
        String scenarioPath = null;
        for (String arg : args) {
            if (arg.startsWith("--scenario=")) {
                scenarioPath = arg.substring("--scenario=".length());
                break;
            }
        }

        if (scenarioPath == null) {
            System.err.println("Error: Missing --scenario=<file> argument");
            System.exit(1);
            return; // unreachable, but satisfies compiler
        }

        // Read the scenario file
        String content;
        try {
            content = new String(Files.readAllBytes(Paths.get(scenarioPath)), "UTF-8");
        } catch (IOException e) {
            System.err.println("Error: Could not read scenario file: " + e.getMessage());
            System.exit(1);
            return;
        }

        // Parse the scenario
        Scenario scenario;
        try {
            scenario = Scenario.parse(content);
        } catch (ScenarioParseException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
            return;
        }

        // Create the simulation and run it on a HeadlessApplication
        CountDownLatch completionLatch = new CountDownLatch(1);
        PhysicsSimulation simulation = new PhysicsSimulation(scenario, completionLatch);

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 0; // Run as fast as possible, no throttling

        new HeadlessApplication(simulation, config);

        // Block the main thread until the simulation completes
        try {
            completionLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Error: Interrupted while waiting for simulation to complete");
            System.exit(1);
        }
    }
}