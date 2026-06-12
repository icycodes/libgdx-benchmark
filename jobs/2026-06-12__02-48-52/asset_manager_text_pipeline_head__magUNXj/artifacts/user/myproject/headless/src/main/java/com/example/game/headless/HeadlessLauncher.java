package com.example.game.headless;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;

import com.example.game.LevelData;
import com.example.game.LevelDataLoader;

import java.io.BufferedReader;
import java.io.File;
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

        new HeadlessApplication(new LevelCatalogListener(manifestPath), config);
    }

    /**
     * ApplicationListener that reads a manifest, loads all referenced levels
     * asynchronously through an AssetManager, then prints a summary and exits.
     */
    private static class LevelCatalogListener extends ApplicationAdapter {

        private final String manifestPath;

        private AssetManager manager;
        /** Ordered list of manifest entries (absolute paths) as they appeared in the manifest. */
        private List<String> levelPaths;
        /** Whether asset loading has been kicked off. */
        private boolean loadingStarted = false;
        /** Whether output has been printed. */
        private boolean done = false;

        LevelCatalogListener(String manifestPath) {
            this.manifestPath = manifestPath;
        }

        @Override
        public void create() {
            manager = new AssetManager();
            manager.setLoader(LevelData.class, new LevelDataLoader(manager.getFileHandleResolver()));

            // Resolve manifest to an absolute path
            String absManifestPath = new File(manifestPath).getAbsolutePath();
            FileHandle manifest = Gdx.files.absolute(absManifestPath);

            // Parse manifest
            levelPaths = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(manifest.read(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }
                    // Resolve each level path to absolute
                    String absLevelPath = new File(trimmed).getAbsolutePath();
                    levelPaths.add(absLevelPath);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to read manifest: " + manifestPath, e);
            }

            // Queue each level for loading
            for (String path : levelPaths) {
                manager.load(path, LevelData.class);
            }

            loadingStarted = true;
        }

        @Override
        public void render() {
            if (!loadingStarted || done) {
                return;
            }

            boolean finished = manager.update();

            if (finished) {
                done = true;

                int totalEnemies = 0;
                for (String path : levelPaths) {
                    LevelData level = manager.get(path, LevelData.class);
                    System.out.println("LOADED " + level.name + " enemies=" + level.enemies + " difficulty=" + level.difficulty);
                    totalEnemies += level.enemies;
                }

                System.out.println("TOTAL_LEVELS=" + levelPaths.size());
                System.out.println("TOTAL_ENEMIES=" + totalEnemies);
                System.out.printf(Locale.US, "PROGRESS=%.2f%n", manager.getProgress());
                System.out.println("DONE");
                System.out.flush();

                manager.dispose();
                Gdx.app.exit();
            }
        }

        @Override
        public void dispose() {
            if (manager != null && !done) {
                manager.dispose();
            }
        }
    }
}