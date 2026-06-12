package com.example;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Scenario {
    public float gravityX, gravityY;
    public boolean hasGravity = false;

    public float bodyX, bodyY;
    public boolean hasBody = false;

    public float mass;
    public boolean hasMass = false;

    public int steps = -1;
    public boolean hasSteps = false;

    public static class Impulse {
        public int step;
        public float ix, iy;
        public Impulse(int step, float ix, float iy) {
            this.step = step;
            this.ix = ix;
            this.iy = iy;
        }
    }

    public List<Impulse> impulses = new ArrayList<>();

    public static Scenario parse(String path) throws Exception {
        Scenario s = new Scenario();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] tokens = line.split("\\s+");
                String keyword = tokens[0];
                switch (keyword) {
                    case "GRAVITY":
                        if (s.hasGravity) throw new Exception("Duplicate GRAVITY");
                        if (tokens.length != 3) throw new Exception("Invalid GRAVITY");
                        s.gravityX = Float.parseFloat(tokens[1]);
                        s.gravityY = Float.parseFloat(tokens[2]);
                        s.hasGravity = true;
                        break;
                    case "BODY":
                        if (s.hasBody) throw new Exception("Duplicate BODY");
                        if (tokens.length != 3) throw new Exception("Invalid BODY");
                        s.bodyX = Float.parseFloat(tokens[1]);
                        s.bodyY = Float.parseFloat(tokens[2]);
                        s.hasBody = true;
                        break;
                    case "MASS":
                        if (s.hasMass) throw new Exception("Duplicate MASS");
                        if (tokens.length != 2) throw new Exception("Invalid MASS");
                        s.mass = Float.parseFloat(tokens[1]);
                        s.hasMass = true;
                        break;
                    case "STEPS":
                        if (s.hasSteps) throw new Exception("Duplicate STEPS");
                        if (tokens.length != 2) throw new Exception("Invalid STEPS");
                        s.steps = Integer.parseInt(tokens[1]);
                        if (s.steps < 0) throw new Exception("Negative STEPS");
                        s.hasSteps = true;
                        break;
                    case "IMPULSE":
                        if (tokens.length != 4) throw new Exception("Invalid IMPULSE");
                        int step = Integer.parseInt(tokens[1]);
                        float ix = Float.parseFloat(tokens[2]);
                        float iy = Float.parseFloat(tokens[3]);
                        s.impulses.add(new Impulse(step, ix, iy));
                        break;
                    default:
                        throw new Exception("Unknown keyword: " + keyword);
                }
            }
        }
        
        if (!s.hasGravity) throw new Exception("Missing GRAVITY");
        if (!s.hasBody) throw new Exception("Missing BODY");
        if (!s.hasSteps) throw new Exception("Missing STEPS");
        
        for (Impulse imp : s.impulses) {
            if (imp.step < 0 || imp.step >= s.steps) {
                throw new Exception("IMPULSE step out of bounds");
            }
        }
        
        return s;
    }
}
