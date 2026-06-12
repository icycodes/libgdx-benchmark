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

/**
 * Asynchronous libGDX asset loader for {@link LevelData}.
 *
 * <p>Parsing happens in {@link #loadAsync} (worker thread). {@link #loadSync}
 * simply hands back the result that was stored by {@code loadAsync}.</p>
 */
public class LevelLoader extends AsynchronousAssetLoader<LevelData, LevelLoader.LevelParameter> {

    /** Optional parameter class — no fields needed for now, but required by the API. */
    public static class LevelParameter extends AssetLoaderParameters<LevelData> {}

    /** Intermediate result produced on the worker thread. */
    private LevelData loaded;

    public LevelLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    // -----------------------------------------------------------------------
    // AsynchronousAssetLoader contract
    // -----------------------------------------------------------------------

    /**
     * Called on the GL/render thread to declare dependencies.
     * Level files have no further asset dependencies.
     */
    @Override
    public Array<AssetDescriptor> getDependencies(
            String fileName,
            FileHandle file,
            LevelParameter parameter) {
        return null;
    }

    /**
     * Called on the worker thread. Parse the level file and cache the result.
     */
    @Override
    public void loadAsync(
            AssetManager manager,
            String fileName,
            FileHandle file,
            LevelParameter parameter) {
        loaded = null;
        loaded = parse(file);
    }

    /**
     * Called on the GL/render thread after {@link #loadAsync} completes.
     * Returns the pre-parsed {@link LevelData}.
     */
    @Override
    public LevelData loadSync(
            AssetManager manager,
            String fileName,
            FileHandle file,
            LevelParameter parameter) {
        LevelData result = loaded;
        loaded = null;
        return result;
    }

    // -----------------------------------------------------------------------
    // Parsing
    // -----------------------------------------------------------------------

    private static LevelData parse(FileHandle file) {
        String name = null;
        int enemies = 0;
        int difficulty = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.read(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq < 0) continue;

                String key   = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();

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
                    default:
                        // ignore unknown keys
                        break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read level file: " + file.path(), e);
        }

        if (name == null) {
            throw new RuntimeException("Level file is missing 'name' key: " + file.path());
        }
        return new LevelData(name, enemies, difficulty);
    }
}
