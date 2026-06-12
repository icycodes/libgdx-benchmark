package com.example.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Headless ApplicationListener that queues manifest entries in AssetManager and
 * lets the render loop drive asynchronous loading to completion.
 */
public final class LevelCatalogApplication extends ApplicationAdapter {
    private final String[] args;
    private final List<String> levelPaths = new ArrayList<>();
    private AssetManager manager;
    private boolean finished;
    private boolean disposed;

    public LevelCatalogApplication(String[] args) {
        this.args = args == null ? new String[0] : args.clone();
    }

    @Override
    public void create() {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected exactly one argument: <manifest_path>");
        }

        WorkingDirectoryFileHandleResolver resolver = new WorkingDirectoryFileHandleResolver();
        manager = new AssetManager(resolver);
        manager.setLoader(LevelData.class, new LevelDataLoader(resolver));

        readManifest(Path.of(args[0]));
        for (String levelPath : levelPaths) {
            manager.load(levelPath, LevelData.class);
        }
    }

    @Override
    public void render() {
        if (finished || manager == null) {
            return;
        }

        if (manager.update()) {
            finished = true;
            printSummary();
            disposeManager();
            System.out.flush();
            Gdx.app.exit();
        }
    }

    @Override
    public void dispose() {
        disposeManager();
    }

    private void readManifest(Path manifestPath) {
        try (BufferedReader reader = Files.newBufferedReader(manifestPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                levelPaths.add(trimmed);
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read manifest " + manifestPath, exception);
        }
    }

    private void printSummary() {
        int totalEnemies = 0;
        for (String levelPath : levelPaths) {
            LevelData level = manager.get(levelPath, LevelData.class);
            totalEnemies += level.getEnemies();
            System.out.printf(
                Locale.US,
                "LOADED %s enemies=%d difficulty=%d%n",
                level.getName(),
                level.getEnemies(),
                level.getDifficulty()
            );
        }

        System.out.printf(Locale.US, "TOTAL_LEVELS=%d%n", levelPaths.size());
        System.out.printf(Locale.US, "TOTAL_ENEMIES=%d%n", totalEnemies);
        System.out.printf(Locale.US, "PROGRESS=%.2f%n", manager.getProgress());
        System.out.println("DONE");
    }

    private void disposeManager() {
        if (!disposed && manager != null) {
            disposed = true;
            manager.dispose();
        }
    }
}
