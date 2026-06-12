package com.example.gdxgame;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

import java.util.concurrent.CountDownLatch;

public class Launcher {

    public static void main(String[] args) {
        String scenarioPath = null;

        for (String arg : args) {
            if (arg.startsWith("--scenario=")) {
                scenarioPath = arg.substring("--scenario=".length());
            }
        }

        if (scenarioPath == null) {
            System.err.println("Error: --scenario=<path> argument is required");
            System.exit(1);
            return;
        }

        Scenario scenario;
        try {
            scenario = Scenario.parse(scenarioPath);
        } catch (Scenario.ScenarioParseException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
            return;
        } catch (Exception e) {
            System.err.println("Error: Failed to read scenario file: " + e.getMessage());
            System.exit(1);
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 0;

        SimulationListener listener = new SimulationListener(scenario) {
            @Override
            public void dispose() {
                super.dispose();
                latch.countDown();
            }
        };

        new HeadlessApplication(listener, config);

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Exit with 0 after successful completion
        System.exit(0);
    }
}
