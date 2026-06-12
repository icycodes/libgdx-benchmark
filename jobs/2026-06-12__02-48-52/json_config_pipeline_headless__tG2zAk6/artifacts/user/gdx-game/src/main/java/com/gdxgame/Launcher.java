package com.gdxgame;

import java.util.concurrent.CountDownLatch;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

/**
 * Entry point that boots a {@link GameConfig} listener inside a
 * {@link HeadlessApplication}, waits for it to finish, and exits with the
 * appropriate status code.
 */
public class Launcher {

    public static void main(String[] args) {
        String configPath = null;
        for (String arg : args) {
            if (arg.startsWith("--config=")) {
                configPath = arg.substring("--config=".length());
            }
        }

        if (configPath == null || configPath.isEmpty()) {
            System.err.println("Error: invalid config: no config path provided");
            System.exit(1);
        }

        CountDownLatch latch = new CountDownLatch(1);
        GameConfig listener = new GameConfig(configPath, latch);
        HeadlessApplicationConfiguration cfg = new HeadlessApplicationConfiguration();
        HeadlessApplication app = new HeadlessApplication(listener, cfg);

        // Block until the listener signals it is done
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Wait for the headless main-loop thread to actually finish
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.exit(listener.getExitCode());
    }
}