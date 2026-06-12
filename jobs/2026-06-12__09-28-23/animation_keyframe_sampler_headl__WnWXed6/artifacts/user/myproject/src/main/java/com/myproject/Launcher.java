package com.myproject;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.List;

public class Launcher extends ApplicationAdapter {
    private final String configPath;
    private final String outputPath;

    public Launcher(String configPath, String outputPath) {
        this.configPath = configPath;
        this.outputPath = outputPath;
    }

    private static class Sample {
        String originalString;
        float time;

        Sample(String originalString, float time) {
            this.originalString = originalString;
            this.time = time;
        }
    }

    @Override
    public void create() {
        try {
            FileHandle handle = Gdx.files.absolute(configPath);
            if (!handle.exists()) {
                System.err.println("Configuration file not found: " + configPath);
                Gdx.app.exit();
                return;
            }

            String content = handle.readString("UTF-8");
            String[] lines = content.split("\\r?\\n");

            float frameDuration = 0;
            PlayMode playMode = null;
            List<Integer> keyFramesList = new ArrayList<>();
            List<Sample> samples = new ArrayList<>();

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                if (trimmed.startsWith("frameDuration")) {
                    String val = trimmed.substring("frameDuration".length()).trim();
                    frameDuration = Float.parseFloat(val);
                } else if (trimmed.startsWith("playMode")) {
                    String val = trimmed.substring("playMode".length()).trim();
                    playMode = PlayMode.valueOf(val);
                } else if (trimmed.startsWith("keyFrames")) {
                    String val = trimmed.substring("keyFrames".length()).trim();
                    String[] tokens = val.split("\\s+");
                    for (String t : tokens) {
                        if (!t.isEmpty()) {
                            keyFramesList.add(Integer.parseInt(t));
                        }
                    }
                } else if (trimmed.startsWith("sample")) {
                    String originalTimeStr = trimmed.substring("sample".length()).trim();
                    float time = Float.parseFloat(originalTimeStr);
                    samples.add(new Sample(originalTimeStr, time));
                }
            }

            Array<Integer> gdxKeyFrames = new Array<>(Integer.class);
            for (Integer kf : keyFramesList) {
                gdxKeyFrames.add(kf);
            }

            Animation<Integer> animation = new Animation<>(frameDuration, gdxKeyFrames);
            if (playMode != null) {
                animation.setPlayMode(playMode);
            }

            StringBuilder sb = new StringBuilder();
            for (Sample sample : samples) {
                Integer keyFrameValue = animation.getKeyFrame(sample.time);
                boolean finished = animation.isAnimationFinished(sample.time);
                sb.append(sample.originalString)
                  .append(" ")
                  .append(keyFrameValue)
                  .append(" ")
                  .append(finished)
                  .append("\n");
            }

            FileHandle outHandle = Gdx.files.absolute(outputPath);
            outHandle.writeString(sb.toString(), false, "UTF-8");

            Gdx.app.exit();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: Launcher <config_path> <output_path>");
            System.exit(1);
        }
        String configPath = args[0];
        String outputPath = args[1];

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 0;

        new HeadlessApplication(new Launcher(configPath, outputPath), config);
    }
}
