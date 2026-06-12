package com.example.spline;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.CatmullRomSpline;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SplineSampler extends ApplicationAdapter {

    private final String splineJsonPath;
    private final String inputTxtPath;
    private final String outputCsvPath;

    private final List<Float> tValues = new ArrayList<>();
    private CatmullRomSpline<Vector2> spline;
    private int tick;
    private PrintWriter writer;
    private boolean finished;

    public SplineSampler(String splineJsonPath, String inputTxtPath, String outputCsvPath) {
        this.splineJsonPath = splineJsonPath;
        this.inputTxtPath = inputTxtPath;
        this.outputCsvPath = outputCsvPath;
    }

    @Override
    public void create() {
        Gdx.app.log("SplineSampler", "Loading spline from: " + splineJsonPath);
        loadSpline();
        Gdx.app.log("SplineSampler", "Loading t values from: " + inputTxtPath);
        loadTValues();
        Gdx.app.log("SplineSampler", "Opening output file: " + outputCsvPath);
        openOutput();

        tick = 0;
    }

    private void loadSpline() {
        try {
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(new FileReader(splineJsonPath));

            boolean continuous = root.getBoolean("continuous", false);
            JsonValue pointsArray = root.get("controlPoints");

            List<Vector2> controlPoints = new ArrayList<>();
            for (JsonValue point : pointsArray) {
                float x = point.getFloat("x");
                float y = point.getFloat("y");
                controlPoints.add(new Vector2(x, y));
            }

            spline = new CatmullRomSpline<>(
                controlPoints.toArray(new Vector2[0]),
                continuous
            );

            Gdx.app.log("SplineSampler", "Loaded " + controlPoints.size() + " control points, continuous=" + continuous);
        } catch (Exception e) {
            Gdx.app.error("SplineSampler", "Failed to load spline JSON: " + e.getMessage(), e);
            throw new RuntimeException("Failed to load spline JSON", e);
        }
    }

    private void loadTValues() {
        try (BufferedReader br = new BufferedReader(new FileReader(inputTxtPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                tValues.add(Float.parseFloat(line));
            }
        } catch (Exception e) {
            Gdx.app.error("SplineSampler", "Failed to load input t values: " + e.getMessage(), e);
            throw new RuntimeException("Failed to load input t values", e);
        }
        Gdx.app.log("SplineSampler", "Loaded " + tValues.size() + " t values");
    }

    private void openOutput() {
        try {
            writer = new PrintWriter(
                new OutputStreamWriter(
                    new FileOutputStream(outputCsvPath),
                    StandardCharsets.UTF_8
                )
            );
            writer.println("tick,t,x,y,speed");
        } catch (Exception e) {
            Gdx.app.error("SplineSampler", "Failed to open output file: " + e.getMessage(), e);
            throw new RuntimeException("Failed to open output file", e);
        }
    }

    @Override
    public void render() {
        if (tick >= tValues.size()) {
            if (!finished) {
                Gdx.app.log("SplineSampler", "All " + tValues.size() + " t values processed. Exiting.");
                writer.close();
                finished = true;
            }
            Gdx.app.exit();
            return;
        }

        float t = tValues.get(tick);
        Vector2 out = new Vector2();

        spline.valueAt(out, t);
        float x = out.x;
        float y = out.y;

        spline.derivativeAt(out, t);
        float speed = out.len();

        writer.printf(Locale.ROOT, "%d,%.6f,%.6f,%.6f,%.6f%n", tick, t, x, y, speed);
        writer.flush();

        tick++;
    }

    @Override
    public void dispose() {
        if (writer != null) {
            writer.close();
        }
    }
}
