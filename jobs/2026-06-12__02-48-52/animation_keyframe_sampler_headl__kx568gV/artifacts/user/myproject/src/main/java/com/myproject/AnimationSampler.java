package com.myproject;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.utils.Array;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnimationSampler extends ApplicationAdapter {

    private static String configPath;
    private static String outputPath;

    private float frameDuration;
    private Animation.PlayMode playMode;
    private Array<Integer> keyFrames;
    private List<Sample> samples;

    private static class Sample {
        final String timeString;
        final float timeValue;

        Sample(String timeString, float timeValue) {
            this.timeString = timeString;
            this.timeValue = timeValue;
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: AnimationSampler <config_path> <output_path>");
            System.exit(1);
        }
        configPath = args[0];
        outputPath = args[1];

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 0;
        new HeadlessApplication(new AnimationSampler(), config);
    }

    @Override
    public void create() {
        try {
            parseConfig();
            Animation<Integer> animation = new Animation<>(frameDuration, keyFrames, playMode);
            writeOutput(animation);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            Gdx.app.exit();
        }
    }

    private void parseConfig() throws IOException {
        FileHandle file = Gdx.files.absolute(configPath);
        BufferedReader reader = new BufferedReader(file.reader());

        keyFrames = new Array<>();
        samples = new ArrayList<>();
        playMode = null;
        frameDuration = -1;

        String line;
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            if (trimmed.startsWith("frameDuration ")) {
                frameDuration = Float.parseFloat(trimmed.substring("frameDuration ".length()).trim());
            } else if (trimmed.startsWith("playMode ")) {
                String modeStr = trimmed.substring("playMode ".length()).trim();
                playMode = Animation.PlayMode.valueOf(modeStr);
            } else if (trimmed.startsWith("keyFrames ")) {
                String framesStr = trimmed.substring("keyFrames ".length()).trim();
                String[] parts = framesStr.split("\\s+");
                keyFrames.clear();
                for (String part : parts) {
                    keyFrames.add(Integer.parseInt(part));
                }
            } else if (trimmed.startsWith("sample ")) {
                String timeStr = trimmed.substring("sample ".length());
                float timeValue = Float.parseFloat(timeStr.trim());
                samples.add(new Sample(timeStr, timeValue));
            }
        }
        reader.close();
    }

    private void writeOutput(Animation<Integer> animation) {
        StringBuilder sb = new StringBuilder();
        for (Sample sample : samples) {
            int keyFrameValue = animation.getKeyFrame(sample.timeValue);
            boolean finished = animation.isAnimationFinished(sample.timeValue);
            sb.append(sample.timeString).append(' ').append(keyFrameValue).append(' ').append(finished).append('\n');
        }
        FileHandle outFile = Gdx.files.absolute(outputPath);
        outFile.writeString(sb.toString(), false);
    }
}