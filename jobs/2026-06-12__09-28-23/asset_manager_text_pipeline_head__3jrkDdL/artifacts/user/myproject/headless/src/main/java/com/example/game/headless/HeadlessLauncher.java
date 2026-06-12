package com.example.game.headless;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.example.game.LevelCatalogApplication;

public class HeadlessLauncher {
    public static void main(String[] args) {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 60;
        new HeadlessApplication(new LevelCatalogApplication(args), config);
    }
}
