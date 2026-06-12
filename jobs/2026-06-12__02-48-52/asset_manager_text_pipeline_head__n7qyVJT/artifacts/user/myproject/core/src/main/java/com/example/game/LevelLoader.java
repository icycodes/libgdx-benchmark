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

public class LevelLoader extends AsynchronousAssetLoader<LevelData, LevelLoader.LevelParameter> {
    private LevelData levelData;

    public LevelLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, LevelParameter parameter) {
        levelData = new LevelData();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.read(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    if (key.equals("name")) {
                        levelData.name = value;
                    } else if (key.equals("enemies")) {
                        levelData.enemies = Integer.parseInt(value);
                    } else if (key.equals("difficulty")) {
                        levelData.difficulty = Integer.parseInt(value);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading level: " + fileName, e);
        }
    }

    @Override
    public LevelData loadSync(AssetManager manager, String fileName, FileHandle file, LevelParameter parameter) {
        LevelData data = levelData;
        levelData = null;
        return data;
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, LevelParameter parameter) {
        return null;
    }

    public static class LevelParameter extends AssetLoaderParameters<LevelData> {
    }
}
