package com.example.game.headless;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.AbsoluteFileHandleResolver;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.example.game.LevelData;
import com.example.game.LevelLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HeadlessLauncher {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: HeadlessLauncher <manifest_path>");
            System.exit(1);
        }

        String manifestPath = args[0];
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 60;
        new HeadlessApplication(new App(manifestPath), config);
    }

    private static class App extends ApplicationAdapter {
        private final String manifestPath;
        private AssetManager manager;
        private List<String> levelPaths = new ArrayList<>();
        private boolean finished = false;

        public App(String manifestPath) {
            this.manifestPath = manifestPath;
        }

        @Override
        public void create() {
            manager = new AssetManager(new AbsoluteFileHandleResolver());
            manager.setLoader(LevelData.class, new LevelLoader(new AbsoluteFileHandleResolver()));

            File manifestFile = new File(manifestPath);
            if (manifestFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(manifestFile), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        
                        String absolutePath = new File(line).getAbsolutePath();
                        levelPaths.add(absolutePath);
                        manager.load(absolutePath, LevelData.class);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void render() {
            if (finished) return;
            
            if (manager.update()) {
                finished = true;
                
                int totalLevels = 0;
                int totalEnemies = 0;
                
                for (String path : levelPaths) {
                    if (manager.isLoaded(path)) {
                        LevelData data = manager.get(path, LevelData.class);
                        System.out.printf("LOADED %s enemies=%d difficulty=%d\n", data.name, data.enemies, data.difficulty);
                        totalLevels++;
                        totalEnemies += data.enemies;
                    }
                }
                
                System.out.printf("TOTAL_LEVELS=%d\n", totalLevels);
                System.out.printf("TOTAL_ENEMIES=%d\n", totalEnemies);
                System.out.printf(Locale.US, "PROGRESS=%.2f\n", manager.getProgress());
                System.out.println("DONE");
                System.out.flush();
                
                manager.dispose();
                Gdx.app.exit();
            }
        }
    }
}
