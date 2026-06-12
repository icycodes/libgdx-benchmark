package com.badlogic.gdx.splinesampler;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.CatmullRomSpline;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SplineSamplerListener extends ApplicationAdapter {
    private final String splinePath;
    private final String inputPath;
    private final String outputPath;

    private List<Float> tValues;
    private CatmullRomSpline<Vector2> spline;
    private FileHandle outputFile;
    private int currentIndex = 0;

    public SplineSamplerListener(String splinePath, String inputPath, String outputPath) {
        this.splinePath = splinePath;
        this.inputPath = inputPath;
        this.outputPath = outputPath;
    }

    @Override
    public void create() {
        tValues = new ArrayList<>();
        try {
            // Ensure parent directories for output exist
            File parentFile = new File(outputPath).getAbsoluteFile().getParentFile();
            if (parentFile != null && !parentFile.exists()) {
                parentFile.mkdirs();
            }

            // Resolve file handles using absolute paths for safety
            FileHandle splineFile = Gdx.files.absolute(new File(splinePath).getAbsolutePath());
            FileHandle inputFile = Gdx.files.absolute(new File(inputPath).getAbsolutePath());
            outputFile = Gdx.files.absolute(new File(outputPath).getAbsolutePath());

            // 1. Load spline definition from JSON
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(splineFile);
            
            JsonValue continuousNode = root.get("continuous");
            boolean continuous = (continuousNode != null) ? continuousNode.asBoolean() : false;

            JsonValue controlPointsJson = root.get("controlPoints");
            if (controlPointsJson == null) {
                throw new IllegalArgumentException("Missing 'controlPoints' in spline JSON");
            }

            int size = controlPointsJson.size;
            Vector2[] controlPoints = new Vector2[size];
            int idx = 0;
            for (JsonValue cp = controlPointsJson.child; cp != null; cp = cp.next) {
                float x = cp.getFloat("x");
                float y = cp.getFloat("y");
                controlPoints[idx++] = new Vector2(x, y);
            }

            // 2. Construct CatmullRomSpline
            spline = new CatmullRomSpline<>(controlPoints, continuous);

            // 3. Read t values from input file
            try (BufferedReader br = new BufferedReader(inputFile.reader("UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) {
                        continue;
                    }
                    try {
                        float t = Float.parseFloat(line);
                        tValues.add(t);
                    } catch (NumberFormatException e) {
                        System.err.println("Warning: Skipping invalid float line: " + line);
                    }
                }
            }

            // 4. Overwrite output file and write header
            outputFile.writeString("tick,t,x,y,speed\n", false, "UTF-8");

        } catch (Exception e) {
            System.err.println("Error initializing SplineSamplerListener:");
            e.printStackTrace();
            Gdx.app.exit();
        }
    }

    @Override
    public void render() {
        if (tValues == null) {
            Gdx.app.exit();
            return;
        }

        if (currentIndex < tValues.size()) {
            float t = tValues.get(currentIndex);

            Vector2 outValue = new Vector2();
            Vector2 outDerivative = new Vector2();
            
            spline.valueAt(outValue, t);
            spline.derivativeAt(outDerivative, t);
            float speed = outDerivative.len();

            String row = String.format(Locale.ROOT, "%d,%.6f,%.6f,%.6f,%.6f\n",
                    currentIndex, t, outValue.x, outValue.y, speed);

            outputFile.writeString(row, true, "UTF-8");

            currentIndex++;
        }

        if (currentIndex >= tValues.size()) {
            Gdx.app.exit();
        }
    }
}
