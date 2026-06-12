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
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Asynchronous asset loader that parses level text files into {@link LevelData} instances.
 * <p>
 * Parsing happens in {@link #loadAsync} (worker thread). {@link #loadSync} simply
 * returns the result computed asynchronously.
 */
public class LevelDataLoader extends AsynchronousAssetLoader<LevelData, AssetLoaderParameters<LevelData>> {

    /** Holds the parsed LevelData between loadAsync and loadSync. */
    private LevelData loadedLevel;

    public LevelDataLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file,
                          AssetLoaderParameters<LevelData> parameter) {
        Map<String, String> values = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.read(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq >= 0) {
                    String key = line.substring(0, eq).trim();
                    String value = line.substring(eq + 1).trim();
                    values.put(key, value);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read level file: " + fileName, e);
        }

        String name = values.getOrDefault("name", "");
        int enemies = Integer.parseInt(values.getOrDefault("enemies", "0"));
        int difficulty = Integer.parseInt(values.getOrDefault("difficulty", "0"));

        loadedLevel = new LevelData(name, enemies, difficulty);
    }

    @Override
    public LevelData loadSync(AssetManager manager, String fileName, FileHandle file,
                              AssetLoaderParameters<LevelData> parameter) {
        LevelData result = loadedLevel;
        loadedLevel = null; // allow GC
        return result;
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file,
                                                   AssetLoaderParameters<LevelData> parameter) {
        return null; // no dependencies
    }
}