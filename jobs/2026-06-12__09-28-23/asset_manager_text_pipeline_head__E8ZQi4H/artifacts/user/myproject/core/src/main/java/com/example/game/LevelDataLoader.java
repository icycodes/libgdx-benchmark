package com.example.game;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import java.io.BufferedReader;
import java.io.IOException;

public class LevelDataLoader extends AsynchronousAssetLoader<LevelData, LevelDataLoader.LevelDataParameter> {

    private LevelData result;

    public LevelDataLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, LevelDataParameter parameter) {
        return null; // no dependencies
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, LevelDataParameter parameter) {
        result = null;
        String name = null;
        int enemies = -1;
        int difficulty = -1;

        try (BufferedReader reader = new BufferedReader(file.reader("UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eqIdx = trimmed.indexOf('=');
                if (eqIdx < 0) {
                    continue;
                }
                String key = trimmed.substring(0, eqIdx).trim();
                String value = trimmed.substring(eqIdx + 1).trim();
                switch (key) {
                    case "name":
                        name = value;
                        break;
                    case "enemies":
                        enemies = Integer.parseInt(value);
                        break;
                    case "difficulty":
                        difficulty = Integer.parseInt(value);
                        break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load level: " + fileName, e);
        }

        if (name == null) {
            throw new RuntimeException("Level file missing 'name' key: " + fileName);
        }
        if (enemies < 0) {
            throw new RuntimeException("Level file missing 'enemies' key: " + fileName);
        }
        if (difficulty < 0) {
            throw new RuntimeException("Level file missing 'difficulty' key: " + fileName);
        }

        result = new LevelData(name, enemies, difficulty);
    }

    @Override
    public LevelData loadSync(AssetManager manager, String fileName, FileHandle file, LevelDataParameter parameter) {
        LevelData r = this.result;
        this.result = null;
        return r;
    }

    public static class LevelDataParameter extends AssetLoaderParameters<LevelData> {
    }
}
