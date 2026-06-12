package com.example.gdxgame;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Parses a scenario file according to the specification.
 *
 * Throws ScenarioParseException on any malformed input.
 */
public class ScenarioParser {

    public static Scenario parse(Path path) throws IOException, ScenarioParseException {
        List<String> rawLines = Files.readAllLines(path, StandardCharsets.UTF_8);

        boolean hasGravity = false;
        boolean hasBody = false;
        boolean hasMass = false;
        boolean hasSteps = false;

        Scenario s = new Scenario();

        int lineNum = 0;
        for (String raw : rawLines) {
            lineNum++;
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] tokens = line.split("\\s+");
            String keyword = tokens[0];

            switch (keyword) {
                case "GRAVITY" -> {
                    if (hasGravity) {
                        throw new ScenarioParseException(
                                "line " + lineNum + ": duplicate GRAVITY keyword");
                    }
                    requireTokenCount(keyword, tokens, 3, lineNum);
                    s.gravityX = parseFloat(keyword, tokens[1], lineNum);
                    s.gravityY = parseFloat(keyword, tokens[2], lineNum);
                    hasGravity = true;
                }
                case "BODY" -> {
                    if (hasBody) {
                        throw new ScenarioParseException(
                                "line " + lineNum + ": duplicate BODY keyword");
                    }
                    requireTokenCount(keyword, tokens, 3, lineNum);
                    s.bodyX = parseFloat(keyword, tokens[1], lineNum);
                    s.bodyY = parseFloat(keyword, tokens[2], lineNum);
                    hasBody = true;
                }
                case "MASS" -> {
                    if (hasMass) {
                        throw new ScenarioParseException(
                                "line " + lineNum + ": duplicate MASS keyword");
                    }
                    requireTokenCount(keyword, tokens, 2, lineNum);
                    s.mass = parseFloat(keyword, tokens[1], lineNum);
                    hasMass = true;
                }
                case "IMPULSE" -> {
                    requireTokenCount(keyword, tokens, 4, lineNum);
                    int step = parseInt(keyword, tokens[1], lineNum);
                    float ix = parseFloat(keyword, tokens[2], lineNum);
                    float iy = parseFloat(keyword, tokens[3], lineNum);
                    s.impulses.add(new float[]{step, ix, iy});
                }
                case "STEPS" -> {
                    if (hasSteps) {
                        throw new ScenarioParseException(
                                "line " + lineNum + ": duplicate STEPS keyword");
                    }
                    requireTokenCount(keyword, tokens, 2, lineNum);
                    int n = parseInt(keyword, tokens[1], lineNum);
                    if (n < 0) {
                        throw new ScenarioParseException(
                                "line " + lineNum + ": STEPS value must be non-negative, got " + n);
                    }
                    s.steps = n;
                    hasSteps = true;
                }
                default -> throw new ScenarioParseException(
                        "line " + lineNum + ": unknown keyword '" + keyword + "'");
            }
        }

        // Validate required fields
        if (!hasGravity) {
            throw new ScenarioParseException("missing required GRAVITY declaration");
        }
        if (!hasBody) {
            throw new ScenarioParseException("missing required BODY declaration");
        }
        if (!hasSteps) {
            throw new ScenarioParseException("missing required STEPS declaration");
        }

        // Validate IMPULSE step indices are in [0, STEPS)
        for (float[] imp : s.impulses) {
            int step = (int) imp[0];
            if (step < 0 || step >= s.steps) {
                throw new ScenarioParseException(
                        "IMPULSE step " + step + " is outside valid range [0, " + s.steps + ")");
            }
        }

        return s;
    }

    // -------------------------------------------------------------------------

    private static void requireTokenCount(String keyword, String[] tokens, int expected, int lineNum)
            throws ScenarioParseException {
        if (tokens.length != expected) {
            throw new ScenarioParseException(
                    "line " + lineNum + ": " + keyword + " expects " + (expected - 1)
                            + " argument(s), got " + (tokens.length - 1));
        }
    }

    private static float parseFloat(String keyword, String token, int lineNum)
            throws ScenarioParseException {
        try {
            return Float.parseFloat(token);
        } catch (NumberFormatException e) {
            throw new ScenarioParseException(
                    "line " + lineNum + ": " + keyword + " - expected float, got '" + token + "'");
        }
    }

    private static int parseInt(String keyword, String token, int lineNum)
            throws ScenarioParseException {
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException e) {
            throw new ScenarioParseException(
                    "line " + lineNum + ": " + keyword + " - expected integer, got '" + token + "'");
        }
    }
}
