package com.gdx.game;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import java.util.concurrent.CountDownLatch;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1 || !args[0].startsWith("--scenario=")) {
            System.err.println("Error: Invalid arguments. Usage: --scenario=<file>");
            System.exit(1);
        }

        String scenarioPath = args[0].substring("--scenario=".length());
        Scenario scenario = null;
        try {
            scenario = ScenarioParser.parse(scenarioPath);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }

        try {
            HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
            config.updatesPerSecond = 0;

            CountDownLatch latch = new CountDownLatch(1);
            PhysicsSimulationListener listener = new PhysicsSimulationListener(scenario, latch);

            new HeadlessApplication(listener, config);

            latch.await();
            System.exit(0);
        } catch (Throwable t) {
            System.err.println("Error during simulation: " + t.getMessage());
            System.exit(1);
        }
    }
}
