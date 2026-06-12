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
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

/**
 * AssetManager loader for the tiny key=value level description format.
 */
public final class LevelDataLoader extends AsynchronousAssetLoader<LevelData, LevelDataLoader.LevelDataParameter> {
    private LevelData loadedLevel;

    public LevelDataLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, LevelDataParameter parameter) {
        return null;
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, LevelDataParameter parameter) {
        loadedLevel = parseLevel(fileName, resolve(fileName));
    }

    @Override
    public LevelData loadSync(AssetManager manager, String fileName, FileHandle file, LevelDataParameter parameter) {
        LevelData level = loadedLevel;
        loadedLevel = null;
        return level;
    }

    private LevelData parseLevel(String fileName, FileHandle file) {
        String name = null;
        Integer enemies = null;
        Integer difficulty = null;
        Set<String> seenKeys = new HashSet<>();

        try (Reader reader = file.reader("UTF-8"); BufferedReader buffered = new BufferedReader(reader)) {
            String line;
            int lineNumber = 0;
            while ((line = buffered.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                int equalsIndex = trimmed.indexOf('=');
                if (equalsIndex < 0) {
                    throw new IllegalArgumentException("Invalid level line " + lineNumber + " in " + fileName + ": expected key=value");
                }

                String key = trimmed.substring(0, equalsIndex).trim();
                String value = trimmed.substring(equalsIndex + 1).trim();
                if (!seenKeys.add(key)) {
                    throw new IllegalArgumentException("Duplicate key '" + key + "' in " + fileName);
                }

                switch (key) {
                    case "name":
                        name = value;
                        break;
                    case "enemies":
                        enemies = parseNonNegativeInteger(fileName, key, value);
                        break;
                    case "difficulty":
                        difficulty = parseNonNegativeInteger(fileName, key, value);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown key '" + key + "' in " + fileName);
                }
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read level file " + fileName, exception);
        }

        if (name == null || enemies == null || difficulty == null) {
            throw new IllegalArgumentException("Level file " + fileName + " must contain name, enemies, and difficulty");
        }
        return new LevelData(name, enemies, difficulty);
    }

    private int parseNonNegativeInteger(String fileName, String key, String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                throw new IllegalArgumentException("Key '" + key + "' in " + fileName + " must be non-negative");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Key '" + key + "' in " + fileName + " must be a non-negative integer", exception);
        }
    }

    public static final class LevelDataParameter extends AssetLoaderParameters<LevelData> {
    }
}
