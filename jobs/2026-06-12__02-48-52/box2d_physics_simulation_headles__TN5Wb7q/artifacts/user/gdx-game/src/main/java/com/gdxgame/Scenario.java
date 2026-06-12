package com.gdxgame;

import java.util.ArrayList;
import java.util.List;

public class Scenario {

    public final float gravityX;
    public final float gravityY;
    public final float bodyX;
    public final float bodyY;
    public final Float massOverride;
    public final int totalSteps;
    public final List<Impulse> impulses;

    public Scenario(float gravityX, float gravityY, float bodyX, float bodyY,
                    Float massOverride, int totalSteps, List<Impulse> impulses) {
        this.gravityX = gravityX;
        this.gravityY = gravityY;
        this.bodyX = bodyX;
        this.bodyY = bodyY;
        this.massOverride = massOverride;
        this.totalSteps = totalSteps;
        this.impulses = impulses;
    }

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

    public static Scenario parse(String content) throws ScenarioParseException {
        boolean hasGravity = false;
        boolean hasBody = false;
        boolean hasMass = false;
        boolean hasSteps = false;

        float gravityX = 0, gravityY = 0;
        float bodyX = 0, bodyY = 0;
        Float massOverride = null;
        int totalSteps = 0;
        List<Impulse> impulses = new ArrayList<>();

        String[] lines = content.split("\n");
        int lineNum = 0;

        for (String rawLine : lines) {
            lineNum++;
            String line = rawLine.trim();

            // Skip blank lines and comments
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] tokens = line.split("\\s+");
            String keyword = tokens[0];

            switch (keyword) {
                case "GRAVITY":
                    if (hasGravity) {
                        throw new ScenarioParseException("Duplicate GRAVITY on line " + lineNum);
                    }
                    if (tokens.length != 3) {
                        throw new ScenarioParseException("GRAVITY requires 2 values on line " + lineNum);
                    }
                    try {
                        gravityX = Float.parseFloat(tokens[1]);
                        gravityY = Float.parseFloat(tokens[2]);
                    } catch (NumberFormatException e) {
                        throw new ScenarioParseException("Non-numeric value in GRAVITY on line " + lineNum);
                    }
                    hasGravity = true;
                    break;

                case "BODY":
                    if (hasBody) {
                        throw new ScenarioParseException("Duplicate BODY on line " + lineNum);
                    }
                    if (tokens.length != 3) {
                        throw new ScenarioParseException("BODY requires 2 values on line " + lineNum);
                    }
                    try {
                        bodyX = Float.parseFloat(tokens[1]);
                        bodyY = Float.parseFloat(tokens[2]);
                    } catch (NumberFormatException e) {
                        throw new ScenarioParseException("Non-numeric value in BODY on line " + lineNum);
                    }
                    hasBody = true;
                    break;

                case "MASS":
                    if (hasMass) {
                        throw new ScenarioParseException("Duplicate MASS on line " + lineNum);
                    }
                    if (tokens.length != 2) {
                        throw new ScenarioParseException("MASS requires 1 value on line " + lineNum);
                    }
                    try {
                        massOverride = Float.parseFloat(tokens[1]);
                    } catch (NumberFormatException e) {
                        throw new ScenarioParseException("Non-numeric value in MASS on line " + lineNum);
                    }
                    hasMass = true;
                    break;

                case "IMPULSE":
                    if (tokens.length != 4) {
                        throw new ScenarioParseException("IMPULSE requires 3 values on line " + lineNum);
                    }
                    int step;
                    float ix, iy;
                    try {
                        step = Integer.parseInt(tokens[1]);
                        ix = Float.parseFloat(tokens[2]);
                        iy = Float.parseFloat(tokens[3]);
                    } catch (NumberFormatException e) {
                        throw new ScenarioParseException("Non-numeric value in IMPULSE on line " + lineNum);
                    }
                    impulses.add(new Impulse(step, ix, iy));
                    break;

                case "STEPS":
                    if (hasSteps) {
                        throw new ScenarioParseException("Duplicate STEPS on line " + lineNum);
                    }
                    if (tokens.length != 2) {
                        throw new ScenarioParseException("STEPS requires 1 value on line " + lineNum);
                    }
                    try {
                        totalSteps = Integer.parseInt(tokens[1]);
                    } catch (NumberFormatException e) {
                        throw new ScenarioParseException("Non-numeric value in STEPS on line " + lineNum);
                    }
                    if (totalSteps < 0) {
                        throw new ScenarioParseException("STEPS must be non-negative on line " + lineNum);
                    }
                    hasSteps = true;
                    break;

                default:
                    throw new ScenarioParseException("Unknown keyword '" + keyword + "' on line " + lineNum);
            }
        }

        // Validate required fields
        if (!hasGravity) {
            throw new ScenarioParseException("Missing required GRAVITY");
        }
        if (!hasBody) {
            throw new ScenarioParseException("Missing required BODY");
        }
        if (!hasSteps) {
            throw new ScenarioParseException("Missing required STEPS");
        }

        // Validate impulse step bounds
        for (Impulse imp : impulses) {
            if (imp.step < 0 || imp.step >= totalSteps) {
                throw new ScenarioParseException("IMPULSE step " + imp.step +
                    " is outside [0, " + totalSteps + ")");
            }
        }

        return new Scenario(gravityX, gravityY, bodyX, bodyY, massOverride, totalSteps, impulses);
    }
}