package com.example.game.headless;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.AbsoluteFileHandleResolver;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.example.game.LevelData;
import com.example.game.LevelDataLoader;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HeadlessLauncher {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: HeadlessLauncher <manifest_path>");
            System.exit(1);
        }

        // Resolve manifest path against current working directory
        String manifestPath = new File(args[0]).getAbsolutePath();

        // Parse manifest and resolve all level paths to absolute before entering libGDX
        List<String> resolvedLevelPaths = new ArrayList<>();
        File manifestFile = new File(manifestPath);
        if (!manifestFile.isFile()) {
            System.err.println("Manifest file not found: " + manifestPath);
            System.exit(1);
        }
        File workingDir = new File(System.getProperty("user.dir"));
        try (BufferedReader reader = new BufferedReader(new java.io.FileReader(manifestFile, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                // Resolve relative paths against working directory
                String resolved = new File(workingDir, trimmed).getAbsolutePath();
                resolvedLevelPaths.add(resolved);
            }
        } catch (IOException e) {
            System.err.println("Failed to read manifest: " + e.getMessage());
            System.exit(1);
        }

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 60;
        new HeadlessApplication(new LevelCatalogListener(resolvedLevelPaths), config);
    }

    static class LevelCatalogListener extends ApplicationAdapter {

        private final List<String> levelPaths;
        private AssetManager manager;
        private int nextLoadIndex;
        private int totalEnemies;
        private int loadedCount;

        LevelCatalogListener(List<String> levelPaths) {
            this.levelPaths = levelPaths;
        }

        @Override
        public void create() {
            FileHandleResolver resolver = new AbsoluteFileHandleResolver();
            manager = new AssetManager(resolver);
            manager.setLoader(LevelData.class, new LevelDataLoader(resolver));

            nextLoadIndex = 0;
            totalEnemies = 0;
            loadedCount = 0;

            // Queue first load
            queueNext();
        }

        private void queueNext() {
            if (nextLoadIndex < levelPaths.size()) {
                String path = levelPaths.get(nextLoadIndex);
                nextLoadIndex++;
                manager.load(path, LevelData.class);
            }
        }

        @Override
        public void render() {
            if (manager == null) {
                return;
            }

            boolean finished = manager.update();

            if (finished) {
                // Retrieve the asset that just finished loading
                if (nextLoadIndex > 0 && nextLoadIndex <= levelPaths.size()) {
                    String path = levelPaths.get(nextLoadIndex - 1);
                    if (manager.isLoaded(path)) {
                        LevelData data = manager.get(path, LevelData.class);
                        System.out.printf(Locale.US, "LOADED %s enemies=%d difficulty=%d%n",
                                data.name, data.enemies, data.difficulty);
                        totalEnemies += data.enemies;
                        loadedCount++;
                    }
                }

                // Queue next or finish
                if (nextLoadIndex < levelPaths.size()) {
                    queueNext();
                } else {
                    // All done
                    System.out.printf(Locale.US, "TOTAL_LEVELS=%d%n", loadedCount);
                    System.out.printf(Locale.US, "TOTAL_ENEMIES=%d%n", totalEnemies);
                    float progress = levelPaths.isEmpty() ? 0.0f : manager.getProgress();
                    System.out.printf(Locale.US, "PROGRESS=%.2f%n", progress);
                    System.out.println("DONE");
                    System.out.flush();
                    manager.dispose();
                    manager = null;
                    Gdx.app.exit();
                }
            }
        }

        @Override
        public void dispose() {
            if (manager != null) {
                manager.dispose();
            }
        }
    }
}
