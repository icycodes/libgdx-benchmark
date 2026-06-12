package com.animation.sampler;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

public class Launcher {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: <config_path> <output_path>");
            System.exit(1);
        }

        String configPath = args[0];
        String outputPath = args[1];

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 0;

        new HeadlessApplication(new AnimationListener(configPath, outputPath), config);
    }
}
