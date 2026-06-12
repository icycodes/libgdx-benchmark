package com.example;

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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SplineSampler extends ApplicationAdapter {
    private final String splineJsonPath;
    private final String inputTxtPath;
    private final String outputCsvPath;

    private CatmullRomSpline<Vector2> spline;
    private List<Float> tValues;
    private int currentTick = 0;
    private BufferedWriter csvWriter;

    public SplineSampler(String splineJsonPath, String inputTxtPath, String outputCsvPath) {
        this.splineJsonPath = splineJsonPath;
        this.inputTxtPath = inputTxtPath;
        this.outputCsvPath = outputCsvPath;
    }

    @Override
    public void create() {
        try {
            JsonReader jsonReader = new JsonReader();
            JsonValue root = jsonReader.parse(Gdx.files.absolute(splineJsonPath));
            boolean continuous = root.getBoolean("continuous", false);
            JsonValue pointsArray = root.get("controlPoints");
            
            Vector2[] controlPoints = new Vector2[pointsArray.size];
            int i = 0;
            for (JsonValue pointObj : pointsArray) {
                float x = pointObj.getFloat("x");
                float y = pointObj.getFloat("y");
                controlPoints[i++] = new Vector2(x, y);
            }
            
            spline = new CatmullRomSpline<Vector2>(controlPoints, continuous);

            tValues = new ArrayList<>();
            BufferedReader br = new BufferedReader(new FileReader(inputTxtPath));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    tValues.add(Float.parseFloat(line));
                }
            }
            br.close();

            csvWriter = new BufferedWriter(new FileWriter(outputCsvPath, false));
            csvWriter.write("tick,t,x,y,speed\n");

        } catch (Exception e) {
            e.printStackTrace();
            Gdx.app.exit();
        }
    }

    @Override
    public void render() {
        if (tValues == null) return; // If create failed
        
        if (currentTick < tValues.size()) {
            float t = tValues.get(currentTick);
            Vector2 position = new Vector2();
            Vector2 derivative = new Vector2();
            
            spline.valueAt(position, t);
            spline.derivativeAt(derivative, t);
            float speed = derivative.len();

            try {
                String row = String.format(Locale.ROOT, "%d,%.6f,%.6f,%.6f,%.6f\n",
                        currentTick, t, position.x, position.y, speed);
                csvWriter.write(row);
            } catch (IOException e) {
                e.printStackTrace();
            }

            currentTick++;
        } else {
            Gdx.app.exit();
        }
    }

    @Override
    public void dispose() {
        try {
            if (csvWriter != null) {
                csvWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: SplineSampler <spline.json> <input.txt> <output.csv>");
            System.exit(1);
        }

        String splineJsonPath = args[0];
        String inputTxtPath = args[1];
        String outputCsvPath = args[2];

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 60;

        SplineSampler listener = new SplineSampler(splineJsonPath, inputTxtPath, outputCsvPath);
        HeadlessApplication app = new HeadlessApplication(listener, config);

        Thread headlessThread = null;
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if ("HeadlessApplication".equals(t.getName())) {
                headlessThread = t;
                break;
            }
        }

        if (headlessThread != null) {
            try {
                headlessThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
