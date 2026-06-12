package com.example.gdxgame;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Scenario {

    public static class Impulse {
        public final int step;
        public final float ix;
        public final float iy;

        public Impulse(int step, float ix, float iy) {
            this.step = step;
            this.ix = ix;
            this.iy = iy;
        }
    }

    public float gravityX;
    public float gravityY;
    public float bodyX;
    public float bodyY;
    public Float mass; // null if not specified
    public int steps;
    public final List<Impulse> impulses = new ArrayList<>();

    private boolean hasGravity;
    private boolean hasBody;
    private boolean hasMass;
    private boolean hasSteps;

    public static Scenario parse(String path) throws IOException, ScenarioParseException {
        Scenario scenario = new Scenario();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                scenario.parseLine(trimmed, lineNumber);
            }
        }
        scenario.validate();
        return scenario;
    }

    private void parseLine(String line, int lineNumber) throws ScenarioParseException {
        String[] tokens = line.split("\\s+");
        if (tokens.length == 0) {
            return;
        }
        String keyword = tokens[0];

        switch (keyword) {
            case "GRAVITY":
                if (hasGravity) {
                    throw new ScenarioParseException("Duplicate GRAVITY at line " + lineNumber);
                }
                if (tokens.length != 3) {
                    throw new ScenarioParseException("GRAVITY requires 2 numeric arguments at line " + lineNumber);
                }
                try {
                    gravityX = Float.parseFloat(tokens[1]);
                    gravityY = Float.parseFloat(tokens[2]);
                } catch (NumberFormatException e) {
                    throw new ScenarioParseException("Invalid numeric value in GRAVITY at line " + lineNumber);
                }
                hasGravity = true;
                break;

            case "BODY":
                if (hasBody) {
                    throw new ScenarioParseException("Duplicate BODY at line " + lineNumber);
                }
                if (tokens.length != 3) {
                    throw new ScenarioParseException("BODY requires 2 numeric arguments at line " + lineNumber);
                }
                try {
                    bodyX = Float.parseFloat(tokens[1]);
                    bodyY = Float.parseFloat(tokens[2]);
                } catch (NumberFormatException e) {
                    throw new ScenarioParseException("Invalid numeric value in BODY at line " + lineNumber);
                }
                hasBody = true;
                break;

            case "MASS":
                if (hasMass) {
                    throw new ScenarioParseException("Duplicate MASS at line " + lineNumber);
                }
                if (tokens.length != 2) {
                    throw new ScenarioParseException("MASS requires 1 numeric argument at line " + lineNumber);
                }
                try {
                    mass = Float.parseFloat(tokens[1]);
                } catch (NumberFormatException e) {
                    throw new ScenarioParseException("Invalid numeric value in MASS at line " + lineNumber);
                }
                hasMass = true;
                break;

            case "IMPULSE":
                if (tokens.length != 4) {
                    throw new ScenarioParseException("IMPULSE requires 3 numeric arguments at line " + lineNumber);
                }
                try {
                    int step = Integer.parseInt(tokens[1]);
                    float ix = Float.parseFloat(tokens[2]);
                    float iy = Float.parseFloat(tokens[3]);
                    impulses.add(new Impulse(step, ix, iy));
                } catch (NumberFormatException e) {
                    throw new ScenarioParseException("Invalid numeric value in IMPULSE at line " + lineNumber);
                }
                break;

            case "STEPS":
                if (hasSteps) {
                    throw new ScenarioParseException("Duplicate STEPS at line " + lineNumber);
                }
                if (tokens.length != 2) {
                    throw new ScenarioParseException("STEPS requires 1 numeric argument at line " + lineNumber);
                }
                try {
                    steps = Integer.parseInt(tokens[1]);
                } catch (NumberFormatException e) {
                    throw new ScenarioParseException("Invalid numeric value in STEPS at line " + lineNumber);
                }
                if (steps < 0) {
                    throw new ScenarioParseException("STEPS must be non-negative at line " + lineNumber);
                }
                hasSteps = true;
                break;

            default:
                throw new ScenarioParseException("Unknown keyword '" + keyword + "' at line " + lineNumber);
        }
    }

    private void validate() throws ScenarioParseException {
        if (!hasGravity) {
            throw new ScenarioParseException("Missing required GRAVITY");
        }
        if (!hasBody) {
            throw new ScenarioParseException("Missing required BODY");
        }
        if (!hasSteps) {
            throw new ScenarioParseException("Missing required STEPS");
        }
        for (Impulse imp : impulses) {
            if (imp.step < 0 || imp.step >= steps) {
                throw new ScenarioParseException(
                        "IMPULSE step " + imp.step + " is outside [0, " + steps + ")");
            }
        }
    }

    public static class ScenarioParseException extends Exception {
        public ScenarioParseException(String message) {
            super(message);
        }
    }
}
