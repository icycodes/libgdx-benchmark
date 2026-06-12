package com.example.gdxgame;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

/**
 * Entry point for the headless Box2D simulator.
 *
 * <p>Usage: {@code gdx-game --scenario=<path>}
 */
public class Launcher {

    public static void main(String[] args) {
        // ---------------------------------------------------------------
        // 1. Parse --scenario=<path> argument
        // ---------------------------------------------------------------
        String scenarioPath = null;
        for (String arg : args) {
            if (arg.startsWith("--scenario=")) {
                scenarioPath = arg.substring("--scenario=".length());
                break;
            }
        }

        if (scenarioPath == null || scenarioPath.isBlank()) {
            System.err.println("Error: missing --scenario=<file> argument");
            System.exit(1);
        }

        Path path = Paths.get(scenarioPath);
        if (!path.toFile().exists()) {
            System.err.println("Error: scenario file not found: " + scenarioPath);
            System.exit(1);
        }

        // ---------------------------------------------------------------
        // 2. Parse scenario file
        // ---------------------------------------------------------------
        Scenario scenario;
        try {
            scenario = ScenarioParser.parse(path);
        } catch (ScenarioParseException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
            return; // unreachable, but satisfies compiler
        } catch (IOException e) {
            System.err.println("Error: could not read scenario file: " + e.getMessage());
            System.exit(1);
            return;
        }

        // ---------------------------------------------------------------
        // 3. Boot HeadlessApplication and block until it finishes
        // ---------------------------------------------------------------
        CountDownLatch latch = new CountDownLatch(1);
        PhysicsSimulator simulator = new PhysicsSimulator(scenario, latch);

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        // 0 means "run render() as fast as possible without sleeping"
        config.updatesPerSecond = 0;

        // HeadlessApplication starts the main loop on its own thread
        new HeadlessApplication(simulator, config);

        // Block the launcher thread until dispose() counts down the latch
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Error: simulation interrupted: " + e.getMessage());
            System.exit(1);
        }
    }
}
