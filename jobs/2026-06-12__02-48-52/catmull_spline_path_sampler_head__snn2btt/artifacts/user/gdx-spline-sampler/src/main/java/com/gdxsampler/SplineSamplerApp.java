package com.gdxsampler;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.CatmullRomSpline;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonReader;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ApplicationAdapter that loads a CatmullRomSpline from JSON, reads t-values
 * from a text file, and writes sampled position + tangent magnitude to a CSV.
 */
public class SplineSamplerApp extends ApplicationAdapter {

    private final String splineJsonPath;
    private final String inputTxtPath;
    private final String outputCsvPath;

    private CatmullRomSpline<Vector2> spline;
    private List<Float> tValues;
    private BufferedWriter csvWriter;

    private int tick = 0;

    public SplineSamplerApp(String splineJsonPath, String inputTxtPath, String outputCsvPath) {
        this.splineJsonPath = splineJsonPath;
        this.inputTxtPath   = inputTxtPath;
        this.outputCsvPath  = outputCsvPath;
    }

    @Override
    public void create() {
        try {
            // --- Parse spline JSON -------------------------------------------
            String jsonText = new String(Files.readAllBytes(Paths.get(splineJsonPath)),
                    StandardCharsets.UTF_8);

            JsonValue root = new JsonReader().parse(jsonText);

            boolean continuous = false;
            if (root.has("continuous")) {
                continuous = root.getBoolean("continuous");
            }

            JsonValue cpArray = root.get("controlPoints");
            if (cpArray == null || cpArray.size < 4) {
                throw new RuntimeException("controlPoints must contain at least 4 entries");
            }

            Vector2[] controlPoints = new Vector2[cpArray.size];
            for (int i = 0; i < cpArray.size; i++) {
                JsonValue cp = cpArray.get(i);
                controlPoints[i] = new Vector2(cp.getFloat("x"), cp.getFloat("y"));
            }

            spline = new CatmullRomSpline<>(controlPoints, continuous);

            // --- Read t values -----------------------------------------------
            tValues = new ArrayList<>();
            List<String> lines = Files.readAllLines(Paths.get(inputTxtPath), StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    tValues.add(Float.parseFloat(trimmed));
                }
            }

            // --- Open output CSV (overwrite) ---------------------------------
            csvWriter = new BufferedWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(outputCsvPath, false),
                            StandardCharsets.UTF_8));

            csvWriter.write("tick,t,x,y,speed");
            csvWriter.newLine();
            csvWriter.flush();

            Gdx.app.log("SplineSampler", "Loaded " + tValues.size() + " t-values. "
                    + controlPoints.length + " control points. continuous=" + continuous);

        } catch (Exception e) {
            Gdx.app.error("SplineSampler", "Error during create()", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void render() {
        // Guard: if we have already signalled exit, do nothing until the loop
        // thread actually stops calling render().
        if (csvWriter == null) {
            return;
        }

        if (tick >= tValues.size()) {
            // All t-values processed -- close the file then shut down.
            try {
                csvWriter.flush();
                csvWriter.close();
            } catch (Exception e) {
                Gdx.app.error("SplineSampler", "Error closing CSV", e);
            } finally {
                csvWriter = null;
            }
            Gdx.app.exit();
            return;
        }

        float t = tValues.get(tick);

        Vector2 pos   = new Vector2();
        Vector2 deriv = new Vector2();

        spline.valueAt(pos, t);
        spline.derivativeAt(deriv, t);

        float speed = deriv.len();

        try {
            String row = String.format(Locale.ROOT,
                    "%d,%.6f,%.6f,%.6f,%.6f",
                    tick, t, pos.x, pos.y, speed);
            csvWriter.write(row);
            csvWriter.newLine();
            csvWriter.flush();
        } catch (Exception e) {
            Gdx.app.error("SplineSampler", "Error writing CSV row at tick=" + tick, e);
            throw new RuntimeException(e);
        }

        tick++;
    }

    @Override
    public void dispose() {
        if (csvWriter != null) {
            try {
                csvWriter.flush();
                csvWriter.close();
            } catch (Exception ignored) {
            }
        }
    }
}
