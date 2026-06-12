package com.example;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.utils.Array;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a keyframe-animation configuration file, constructs a libGDX
 * Animation&lt;Integer&gt;, samples it at each requested state-time,
 * and writes the results to an output file before requesting application exit.
 */
public class AnimationSampler extends ApplicationAdapter {

    private final String configPath;
    private final String outputPath;

    public AnimationSampler(String configPath, String outputPath) {
        this.configPath = configPath;
        this.outputPath = outputPath;
    }

    @Override
    public void create() {
        try {
            run();
        } catch (Exception e) {
            System.err.println("AnimationSampler error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        Gdx.app.exit();
    }

    private void run() throws Exception {
        // -- Parse configuration ---------------------------------------------
        float frameDuration = 0f;
        Animation.PlayMode playMode = null;
        Array<Integer> keyFrames = null;
        List<String> sampleTimeStrings = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(configPath), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                // Skip blank lines and comment lines
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                if (trimmed.startsWith("frameDuration ")) {
                    frameDuration = Float.parseFloat(
                            trimmed.substring("frameDuration ".length()).trim());

                } else if (trimmed.startsWith("playMode ")) {
                    String modeName = trimmed.substring("playMode ".length()).trim();
                    playMode = Animation.PlayMode.valueOf(modeName);

                } else if (trimmed.startsWith("keyFrames ")) {
                    String rest = trimmed.substring("keyFrames ".length()).trim();
                    String[] parts = rest.split("\\s+");
                    keyFrames = new Array<>(parts.length);
                    for (String part : parts) {
                        keyFrames.add(Integer.parseInt(part));
                    }

                } else if (trimmed.startsWith("sample ")) {
                    // Preserve the original token verbatim for output
                    String timeStr = trimmed.substring("sample ".length()).trim();
                    sampleTimeStrings.add(timeStr);
                }
            }
        }

        if (playMode == null) {
            throw new IllegalArgumentException("Config missing 'playMode' directive.");
        }
        if (keyFrames == null || keyFrames.size == 0) {
            throw new IllegalArgumentException("Config missing 'keyFrames' directive or it is empty.");
        }
        if (frameDuration <= 0f) {
            throw new IllegalArgumentException("Config missing or invalid 'frameDuration' directive.");
        }

        // -- Build animation -------------------------------------------------
        Animation<Integer> animation = new Animation<>(frameDuration, keyFrames, playMode);

        // -- Sample and write output -----------------------------------------
        // FileOutputStream(path, false) overwrites any existing file
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outputPath, false), StandardCharsets.UTF_8))) {

            for (String timeStr : sampleTimeStrings) {
                float stateTime = Float.parseFloat(timeStr);

                Integer keyFrameValue = animation.getKeyFrame(stateTime);
                boolean finished = animation.isAnimationFinished(stateTime);

                // Output line: "<time> <keyFrameValue> <finished>"
                writer.write(timeStr);
                writer.write(' ');
                writer.write(Integer.toString(keyFrameValue));
                writer.write(' ');
                writer.write(Boolean.toString(finished));
                writer.newLine();
            }
        }
    }
}
