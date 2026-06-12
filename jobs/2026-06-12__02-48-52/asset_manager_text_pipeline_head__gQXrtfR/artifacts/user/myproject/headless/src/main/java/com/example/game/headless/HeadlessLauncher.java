package com.example.game.headless;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.example.game.LevelCatalogApp;

import java.io.File;

public class HeadlessLauncher {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: HeadlessLauncher <manifest-path>");
            System.exit(1);
        }

        // Capture the real working directory NOW, before HeadlessApplication
        // is constructed (libGDX internals may later alter user.dir).
        File workingDir = new File(System.getProperty("user.dir"));

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        // Drive render() at 60 Hz — fast enough for CI without busy-spinning.
        config.updatesPerSecond = 60;

        new HeadlessApplication(new LevelCatalogApp(args[0], workingDir), config);
    }
}
