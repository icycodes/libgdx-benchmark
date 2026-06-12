package com.example.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.AbsoluteFileHandleResolver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ApplicationListener that drives an asynchronous level-catalog pipeline.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>{@link #create} — reads the manifest, queues every level for loading.</li>
 *   <li>{@link #render} — called repeatedly; each call drives one
 *       {@code AssetManager.update()} tick until loading is complete, then prints
 *       the summary and exits.</li>
 * </ol>
 *
 * <p>{@code workingDir} must be captured by the caller <em>before</em>
 * constructing the {@link com.badlogic.gdx.backends.headless.HeadlessApplication}
 * because libGDX may change {@code user.dir} internally.
 */
public class LevelCatalogApp extends ApplicationAdapter {

    /** Manifest path as supplied on the command line. */
    private final String manifestPath;

    /** Caller's working directory, captured before HeadlessApplication is created. */
    private final File workingDir;

    /** Ordered list of absolute asset paths read from the manifest. */
    private final List<String> levelPaths = new ArrayList<>();

    private AssetManager manager;
    private boolean loadingQueued = false;
    private boolean done = false;

    /**
     * @param manifestPath path to the manifest file (absolute or relative to {@code workingDir})
     * @param workingDir   directory against which relative paths are resolved
     */
    public LevelCatalogApp(String manifestPath, File workingDir) {
        this.manifestPath = manifestPath;
        this.workingDir = workingDir;
    }

    // -----------------------------------------------------------------------
    // ApplicationAdapter callbacks
    // -----------------------------------------------------------------------

    @Override
    public void create() {
        // AbsoluteFileHandleResolver passes the path straight through to
        // java.io — exactly what we need since we hand it canonical absolute
        // paths.
        manager = new AssetManager(new AbsoluteFileHandleResolver());
        manager.setLoader(LevelData.class, new LevelLoader(new AbsoluteFileHandleResolver()));

        try {
            readManifest();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read manifest: " + manifestPath, e);
        }

        // Queue all levels for async loading.
        for (String path : levelPaths) {
            manager.load(path, LevelData.class);
        }
        loadingQueued = true;
    }

    @Override
    public void render() {
        if (done || !loadingQueued) return;

        // Drive the async loading pipeline one tick at a time (no finishLoading).
        boolean finished = manager.update();

        if (finished) {
            done = true;
            printSummary();
            manager.dispose();
            Gdx.app.exit();
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /** Resolve a path against {@link #workingDir} and return the canonical absolute path. */
    private String resolve(String path) {
        File f = new File(path);
        if (!f.isAbsolute()) {
            f = new File(workingDir, path);
        }
        return f.getAbsolutePath();
    }

    private void readManifest() throws IOException {
        String absoluteManifest = resolve(manifestPath);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(absoluteManifest), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                levelPaths.add(resolve(line));
            }
        }
    }

    private void printSummary() {
        PrintStream out;
        try {
            out = new PrintStream(System.out, /*autoFlush=*/true, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            out = System.out;
        }

        int totalEnemies = 0;

        for (String path : levelPaths) {
            LevelData level = manager.get(path, LevelData.class);
            out.println("LOADED " + level.name + " enemies=" + level.enemies
                    + " difficulty=" + level.difficulty);
            totalEnemies += level.enemies;
        }

        out.println("TOTAL_LEVELS=" + levelPaths.size());
        out.println("TOTAL_ENEMIES=" + totalEnemies);

        float progress = manager.getProgress();
        out.printf(Locale.US, "PROGRESS=%.2f%n", progress);

        out.println("DONE");
        out.flush();
    }
}
