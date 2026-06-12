package com.animation.sampler;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.utils.Array;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class AnimationListener extends ApplicationAdapter {

    private final String configPath;
    private final String outputPath;

    public AnimationListener(String configPath, String outputPath) {
        this.configPath = configPath;
        this.outputPath = outputPath;
    }

    @Override
    public void create() {
        try {
            FileHandle configFile = Gdx.files.absolute(configPath);
            Config config = parseConfig(configFile.readString("UTF-8"));

            Animation<Integer> animation = new Animation<Integer>(config.frameDuration, config.keyFrames);
            animation.setPlayMode(config.playMode);

            FileHandle outputFile = Gdx.files.absolute(outputPath);
            StringBuilder sb = new StringBuilder();

            for (Sample sample : config.samples) {
                float stateTime = sample.time;
                int keyFrameValue = animation.getKeyFrame(stateTime);
                boolean finished = animation.isAnimationFinished(stateTime);
                sb.append(sample.rawTime)
                  .append(' ')
                  .append(keyFrameValue)
                  .append(' ')
                  .append(finished)
                  .append('\n');
            }

            outputFile.writeString(sb.toString(), false, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            Gdx.app.exit();
        }
    }

    private Config parseConfig(String content) {
        Config config = new Config();
        String[] lines = content.split("\\r?\\n");

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            int spaceIdx = line.indexOf(' ');
            if (spaceIdx < 0) {
                throw new IllegalArgumentException("Invalid config line: " + rawLine);
            }

            String directive = line.substring(0, spaceIdx);
            String args = line.substring(spaceIdx + 1).trim();

            switch (directive) {
                case "frameDuration":
                    if (config.frameDurationSet) {
                        throw new IllegalArgumentException("Duplicate frameDuration directive");
                    }
                    config.frameDuration = Float.parseFloat(args);
                    config.frameDurationSet = true;
                    break;
                case "playMode":
                    if (config.playModeSet) {
                        throw new IllegalArgumentException("Duplicate playMode directive");
                    }
                    config.playMode = Animation.PlayMode.valueOf(args);
                    config.playModeSet = true;
                    break;
                case "keyFrames":
                    if (config.keyFramesSet) {
                        throw new IllegalArgumentException("Duplicate keyFrames directive");
                    }
                    String[] parts = args.split("\\s+");
                    Array<Integer> frames = new Array<>(true, parts.length, Integer.class);
                    for (String part : parts) {
                        frames.add(Integer.parseInt(part));
                    }
                    config.keyFrames = frames;
                    config.keyFramesSet = true;
                    break;
                case "sample":
                    float sampleTime = Float.parseFloat(args);
                    config.samples.add(new Sample(sampleTime, args));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown directive: " + directive);
            }
        }

        if (!config.frameDurationSet) {
            throw new IllegalArgumentException("Missing frameDuration directive");
        }
        if (!config.playModeSet) {
            throw new IllegalArgumentException("Missing playMode directive");
        }
        if (!config.keyFramesSet) {
            throw new IllegalArgumentException("Missing keyFrames directive");
        }

        return config;
    }

    private static class Config {
        float frameDuration;
        boolean frameDurationSet;
        Animation.PlayMode playMode;
        boolean playModeSet;
        Array<Integer> keyFrames;
        boolean keyFramesSet;
        List<Sample> samples = new ArrayList<Sample>();
    }

    private static class Sample {
        final float time;
        final String rawTime;

        Sample(float time, String rawTime) {
            this.time = time;
            this.rawTime = rawTime;
        }
    }
}
