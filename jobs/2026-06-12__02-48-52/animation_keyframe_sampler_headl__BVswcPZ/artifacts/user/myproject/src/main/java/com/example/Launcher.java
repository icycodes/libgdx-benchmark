package com.example;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

public class Launcher {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: Launcher <config_path> <output_path>");
            System.exit(1);
        }

        String configPath = args[0];
        String outputPath = args[1];

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        // Run as fast as possible; we do not rely on delta time
        config.updatesPerSecond = 0;

        new HeadlessApplication(new AnimationSampler(configPath, outputPath), config);
    }
}
