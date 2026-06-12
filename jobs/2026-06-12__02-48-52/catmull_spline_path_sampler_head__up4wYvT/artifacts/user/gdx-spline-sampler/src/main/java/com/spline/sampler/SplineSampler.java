package com.spline.sampler;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.math.CatmullRomSpline;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public class SplineSampler extends ApplicationAdapter {

    private CatmullRomSpline<Vector2> spline;
    private List<Float> tValues;
    private int currentTick;
    private String outputCsvPath;
    private BufferedWriter writer;
    private CountDownLatch doneLatch;

    @Override
    public void create() {
        // Spline and t-values are already loaded in main() before the app starts.
        // Just open the output file for writing.
        try {
            writer = Files.newBufferedWriter(Paths.get(outputCsvPath), StandardCharsets.UTF_8);
            writer.write("tick,t,x,y,speed");
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Failed to open output CSV: " + outputCsvPath, e);
        }
    }

    @Override
    public void render() {
        if (currentTick >= tValues.size()) {
            Gdx.app.exit();
            return;
        }

        float t = tValues.get(currentTick);
        Vector2 position = new Vector2();
        Vector2 tangent = new Vector2();

        spline.valueAt(position, t);
        spline.derivativeAt(tangent, t);

        float speed = tangent.len();

        String line = String.format(Locale.ROOT, "%d,%.6f,%.6f,%.6f,%.6f",
                currentTick, t, position.x, position.y, speed);

        try {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write CSV row", e);
        }

        currentTick++;
    }

    @Override
    public void dispose() {
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                // Best effort
            }
        }
        if (doneLatch != null) {
            doneLatch.countDown();
        }
    }

    /**
     * Loads the spline definition from a JSON file using libGDX's JsonReader.
     */
    private static CatmullRomSpline<Vector2> loadSpline(String jsonPath) {
        JsonReader jsonReader = new JsonReader();
        JsonValue root;
        try {
            String content = new String(Files.readAllBytes(Paths.get(jsonPath)), StandardCharsets.UTF_8);
            root = jsonReader.parse(content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read spline JSON: " + jsonPath, e);
        }

        boolean continuous = root.getBoolean("continuous", false);
        JsonValue cpArray = root.get("controlPoints");

        List<Vector2> points = new ArrayList<>();
        if (cpArray != null) {
            for (JsonValue cp = cpArray.child; cp != null; cp = cp.next) {
                float x = cp.getFloat("x", 0f);
                float y = cp.getFloat("y", 0f);
                points.add(new Vector2(x, y));
            }
        }

        Vector2[] controlPoints = points.toArray(new Vector2[0]);
        return new CatmullRomSpline<>(controlPoints, continuous);
    }

    /**
     * Reads t values from the input text file, one per line, skipping blank lines.
     */
    private static List<Float> loadTValues(String inputPath) {
        List<Float> values = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputPath), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    values.add(Float.parseFloat(trimmed));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read input file: " + inputPath, e);
        }
        return values;
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: SplineSampler <spline.json> <input.txt> <output.csv>");
            System.exit(1);
        }

        String splineJsonPath = args[0];
        String inputTxtPath = args[1];
        String outputCsvPath = args[2];

        // Load spline and t-values before starting the headless application,
        // so that the listener has data ready in create().
        CatmullRomSpline<Vector2> spline = loadSpline(splineJsonPath);
        List<Float> tValues = loadTValues(inputTxtPath);

        SplineSampler sampler = new SplineSampler();
        sampler.spline = spline;
        sampler.tValues = tValues;
        sampler.currentTick = 0;
        sampler.outputCsvPath = outputCsvPath;

        CountDownLatch doneLatch = new CountDownLatch(1);
        sampler.doneLatch = doneLatch;

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 60;

        HeadlessApplication app = new HeadlessApplication(sampler, config);

        // Wait for the headless main loop thread to finish
        try {
            doneLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}