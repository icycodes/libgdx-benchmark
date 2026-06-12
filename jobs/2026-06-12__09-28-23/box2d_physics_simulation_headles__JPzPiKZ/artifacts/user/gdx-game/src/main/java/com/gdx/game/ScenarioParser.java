package com.gdx.game;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ScenarioParser {

    public static Scenario parse(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Scenario file does not exist: " + filePath);
        }

        Scenario scenario = new Scenario();
        boolean hasGravity = false;
        boolean hasBody = false;
        boolean hasMass = false;
        boolean hasSteps = false;

        List<Scenario.Impulse> tempImpulses = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\s+");
                String keyword = parts[0];

                switch (keyword) {
                    case "GRAVITY":
                        if (parts.length != 3) {
                            throw new IllegalArgumentException("GRAVITY requires exactly 2 arguments, got " + (parts.length - 1) + " at line " + lineNumber);
                        }
                        if (hasGravity) {
                            throw new IllegalArgumentException("Duplicate GRAVITY definition at line " + lineNumber);
                        }
                        try {
                            scenario.gravityX = Float.parseFloat(parts[1]);
                            scenario.gravityY = Float.parseFloat(parts[2]);
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Invalid numeric value for GRAVITY at line " + lineNumber, e);
                        }
                        hasGravity = true;
                        break;

                    case "BODY":
                        if (parts.length != 3) {
                            throw new IllegalArgumentException("BODY requires exactly 2 arguments, got " + (parts.length - 1) + " at line " + lineNumber);
                        }
                        if (hasBody) {
                            throw new IllegalArgumentException("Duplicate BODY definition at line " + lineNumber);
                        }
                        try {
                            scenario.bodyX = Float.parseFloat(parts[1]);
                            scenario.bodyY = Float.parseFloat(parts[2]);
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Invalid numeric value for BODY at line " + lineNumber, e);
                        }
                        hasBody = true;
                        break;

                    case "MASS":
                        if (parts.length != 2) {
                            throw new IllegalArgumentException("MASS requires exactly 1 argument, got " + (parts.length - 1) + " at line " + lineNumber);
                        }
                        if (hasMass) {
                            throw new IllegalArgumentException("Duplicate MASS definition at line " + lineNumber);
                        }
                        try {
                            scenario.massOverride = Float.parseFloat(parts[1]);
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Invalid numeric value for MASS at line " + lineNumber, e);
                        }
                        hasMass = true;
                        break;

                    case "STEPS":
                        if (parts.length != 2) {
                            throw new IllegalArgumentException("STEPS requires exactly 1 argument, got " + (parts.length - 1) + " at line " + lineNumber);
                        }
                        if (hasSteps) {
                            throw new IllegalArgumentException("Duplicate STEPS definition at line " + lineNumber);
                        }
                        try {
                            float stepsF = Float.parseFloat(parts[1]);
                            if (stepsF < 0 || stepsF != (int) stepsF) {
                                throw new IllegalArgumentException("STEPS must be a non-negative integer, got " + parts[1] + " at line " + lineNumber);
                            }
                            scenario.steps = (int) stepsF;
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Invalid numeric value for STEPS at line " + lineNumber, e);
                        }
                        hasSteps = true;
                        break;

                    case "IMPULSE":
                        if (parts.length != 4) {
                            throw new IllegalArgumentException("IMPULSE requires exactly 3 arguments, got " + (parts.length - 1) + " at line " + lineNumber);
                        }
                        int step;
                        float ix, iy;
                        try {
                            float stepF = Float.parseFloat(parts[1]);
                            if (stepF < 0 || stepF != (int) stepF) {
                                throw new IllegalArgumentException("IMPULSE step must be a non-negative integer, got " + parts[1] + " at line " + lineNumber);
                            }
                            step = (int) stepF;
                            ix = Float.parseFloat(parts[2]);
                            iy = Float.parseFloat(parts[3]);
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Invalid numeric value for IMPULSE at line " + lineNumber, e);
                        }
                        tempImpulses.add(new Scenario.Impulse(step, ix, iy));
                        break;

                    default:
                        throw new IllegalArgumentException("Unknown keyword: " + keyword + " at line " + lineNumber);
                }
            }
        }

        // Validate required fields
        if (!hasGravity) {
            throw new IllegalArgumentException("Missing required GRAVITY definition");
        }
        if (!hasBody) {
            throw new IllegalArgumentException("Missing required BODY definition");
        }
        if (!hasSteps) {
            throw new IllegalArgumentException("Missing required STEPS definition");
        }

        // Validate impulses
        for (Scenario.Impulse impulse : tempImpulses) {
            if (impulse.step < 0 || impulse.step >= scenario.steps) {
                throw new IllegalArgumentException("IMPULSE step " + impulse.step + " is outside valid range [0, " + scenario.steps + ")");
            }
            scenario.impulses.add(impulse);
        }

        return scenario;
    }
}
