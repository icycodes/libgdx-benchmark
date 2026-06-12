package com.example.game;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import java.io.BufferedReader;
import java.io.IOException;

public class LevelDataLoader extends AsynchronousAssetLoader<LevelData, LevelDataLoader.LevelParameter> {
    private final ObjectMap<String, LevelData> loadedLevels = new ObjectMap<>();

    public LevelDataLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, LevelParameter parameter) {
        FileHandle resolvedFile = resolve(fileName);
        LevelData data = parseLevel(resolvedFile);
        synchronized (loadedLevels) {
            loadedLevels.put(fileName, data);
        }
    }

    @Override
    public LevelData loadSync(AssetManager manager, String fileName, FileHandle file, LevelParameter parameter) {
        synchronized (loadedLevels) {
            return loadedLevels.remove(fileName);
        }
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, LevelParameter parameter) {
        return null;
    }

    private LevelData parseLevel(FileHandle file) {
        String name = "";
        int enemies = 0;
        int difficulty = 0;

        BufferedReader reader = null;
        try {
            reader = file.reader(1024, "UTF-8");
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx != -1) {
                    String key = line.substring(0, eqIdx).trim();
                    String val = line.substring(eqIdx + 1).trim();
                    if (key.equals("name")) {
                        name = val;
                    } else if (key.equals("enemies")) {
                        enemies = Integer.parseInt(val);
                    } else if (key.equals("difficulty")) {
                        difficulty = Integer.parseInt(val);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading level file: " + file.path(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }

        return new LevelData(name, enemies, difficulty);
    }

    public static class LevelParameter extends AssetLoaderParameters<LevelData> {
    }
}
