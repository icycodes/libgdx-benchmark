package com.example;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import java.util.concurrent.CountDownLatch;

public class Launcher {
    public static void main(String[] args) {
        String configPath = null;
        for (String arg : args) {
            if (arg.startsWith("--config=")) {
                configPath = arg.substring("--config=".length());
            }
        }

        if (configPath == null) {
            System.err.println("Error: invalid config: missing --config argument");
            System.exit(1);
        }

        CountDownLatch latch = new CountDownLatch(1);
        int[] exitCode = new int[] { 0 };

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 60; 
        
        new HeadlessApplication(new ConfigProcessor(configPath, latch, exitCode), config);

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.exit(exitCode[0]);
    }
}
