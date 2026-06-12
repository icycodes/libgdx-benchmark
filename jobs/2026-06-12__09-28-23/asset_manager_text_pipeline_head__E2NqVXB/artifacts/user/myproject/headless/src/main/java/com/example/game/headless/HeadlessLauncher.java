package com.example.game.headless;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
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
    public static void main(final String[] args) {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 60;

        new HeadlessApplication(new ApplicationAdapter() {
            private AssetManager manager;
            private List<String> levelPaths;
            private boolean finished = false;

            @Override
            public void create() {
                if (args.length < 1) {
                    System.err.println("Error: Missing manifest file argument.");
                    Gdx.app.exit();
                    return;
                }

                String manifestPath = args[0];
                FileHandle manifestFile = Gdx.files.absolute(new File(manifestPath).getAbsolutePath());
                if (!manifestFile.exists()) {
                    System.err.println("Error: Manifest file does not exist: " + manifestPath);
                    Gdx.app.exit();
                    return;
                }

                levelPaths = new ArrayList<>();
                BufferedReader reader = null;
                try {
                    reader = manifestFile.reader(1024, "UTF-8");
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        levelPaths.add(line);
                    }
                } catch (Exception e) {
                    System.err.println("Error reading manifest: " + e.getMessage());
                    Gdx.app.exit();
                    return;
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException ignored) {
                        }
                    }
                }

                FileHandleResolver resolver = new FileHandleResolver() {
                    @Override
                    public FileHandle resolve(String fileName) {
                        return Gdx.files.absolute(new File(fileName).getAbsolutePath());
                    }
                };

                manager = new AssetManager(resolver);
                manager.setLoader(LevelData.class, new LevelDataLoader(resolver));

                for (String path : levelPaths) {
                    manager.load(path, LevelData.class);
                }
            }

            @Override
            public void render() {
                if (finished) {
                    return;
                }

                if (manager == null) {
                    finished = true;
                    Gdx.app.exit();
                    return;
                }

                if (manager.update()) {
                    finished = true;

                    int totalEnemies = 0;
                    for (String path : levelPaths) {
                        LevelData level = manager.get(path, LevelData.class);
                        System.out.println("LOADED " + level.getName() + " enemies=" + level.getEnemies() + " difficulty=" + level.getDifficulty());
                        totalEnemies += level.getEnemies();
                    }

                    System.out.println("TOTAL_LEVELS=" + levelPaths.size());
                    System.out.println("TOTAL_ENEMIES=" + totalEnemies);
                    System.out.printf(Locale.US, "PROGRESS=%.2f\n", manager.getProgress());
                    System.out.println("DONE");
                    System.out.flush();

                    manager.dispose();
                    Gdx.app.exit();
                }
            }
        }, config);
    }
}
