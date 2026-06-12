package com.example.animationsampler;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.List;

/**
 * Boots libGDX's headless backend and samples an Animation<Integer> from a text configuration file.
 */
public final class AnimationSamplerLauncher {
    private AnimationSamplerLauncher() {
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: AnimationSamplerLauncher <config_path> <output_path>");
            System.exit(1);
        }

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            throwable.printStackTrace(System.err);
            System.exit(1);
        });

        HeadlessApplicationConfiguration configuration = new HeadlessApplicationConfiguration();
        configuration.updatesPerSecond = 0;

        new HeadlessApplication(new SamplerApplication(args[0], args[1]), configuration);
    }

    private static final class SamplerApplication extends ApplicationAdapter {
        private final String inputPath;
        private final String outputPath;
        private boolean sampled;

        private SamplerApplication(String inputPath, String outputPath) {
            this.inputPath = inputPath;
            this.outputPath = outputPath;
        }

        @Override
        public void create() {
            runSampler();
            Gdx.app.exit();
        }

        @Override
        public void render() {
            if (!sampled) {
                runSampler();
                Gdx.app.exit();
            }
        }

        private void runSampler() {
            sampled = true;

            SamplerConfig config = SamplerConfig.read(Gdx.files.absolute(inputPath));

            Animation<Integer> animation = new Animation<>(config.frameDuration, config.keyFrames);
            animation.setPlayMode(config.playMode);

            StringBuilder output = new StringBuilder();
            for (Sample sample : config.samples) {
                Integer keyFrame = animation.getKeyFrame(sample.stateTime);
                boolean finished = animation.isAnimationFinished(sample.stateTime);
                output.append(sample.originalTime)
                        .append(' ')
                        .append(keyFrame)
                        .append(' ')
                        .append(finished)
                        .append('\n');
            }

            Gdx.files.absolute(outputPath).writeString(output.toString(), false, "UTF-8");
        }
    }

    private static final class SamplerConfig {
        private final float frameDuration;
        private final Animation.PlayMode playMode;
        private final Array<Integer> keyFrames;
        private final List<Sample> samples;

        private SamplerConfig(float frameDuration,
                              Animation.PlayMode playMode,
                              Array<Integer> keyFrames,
                              List<Sample> samples) {
            this.frameDuration = frameDuration;
            this.playMode = playMode;
            this.keyFrames = keyFrames;
            this.samples = samples;
        }

        private static SamplerConfig read(FileHandle file) {
            String content = file.readString("UTF-8");
            String[] lines = content.split("\\R", -1);

            Float frameDuration = null;
            Animation.PlayMode playMode = null;
            Array<Integer> keyFrames = null;
            List<Sample> samples = new ArrayList<>();

            for (int i = 0; i < lines.length; i++) {
                String trimmed = lines[i].trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                String[] parts = trimmed.split("\\s+");
                String directive = parts[0];
                int lineNumber = i + 1;

                switch (directive) {
                    case "frameDuration":
                        requireUnset(frameDuration == null, directive, lineNumber);
                        requirePartCount(parts, 2, directive, lineNumber);
                        frameDuration = parseFloat(parts[1], directive, lineNumber);
                        break;
                    case "playMode":
                        requireUnset(playMode == null, directive, lineNumber);
                        requirePartCount(parts, 2, directive, lineNumber);
                        playMode = parsePlayMode(parts[1], lineNumber);
                        break;
                    case "keyFrames":
                        requireUnset(keyFrames == null, directive, lineNumber);
                        if (parts.length < 2) {
                            throw new IllegalArgumentException("keyFrames requires at least one integer at line " + lineNumber);
                        }
                        keyFrames = new Array<>(Integer.class);
                        for (int j = 1; j < parts.length; j++) {
                            keyFrames.add(parseInteger(parts[j], directive, lineNumber));
                        }
                        break;
                    case "sample":
                        requirePartCount(parts, 2, directive, lineNumber);
                        samples.add(new Sample(parts[1], parseFloat(parts[1], directive, lineNumber)));
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown directive '" + directive + "' at line " + lineNumber);
                }
            }

            if (frameDuration == null) {
                throw new IllegalArgumentException("Missing required frameDuration directive");
            }
            if (playMode == null) {
                throw new IllegalArgumentException("Missing required playMode directive");
            }
            if (keyFrames == null) {
                throw new IllegalArgumentException("Missing required keyFrames directive");
            }

            return new SamplerConfig(frameDuration, playMode, keyFrames, samples);
        }

        private static void requireUnset(boolean unset, String directive, int lineNumber) {
            if (!unset) {
                throw new IllegalArgumentException("Duplicate " + directive + " directive at line " + lineNumber);
            }
        }

        private static void requirePartCount(String[] parts, int expected, String directive, int lineNumber) {
            if (parts.length != expected) {
                throw new IllegalArgumentException(directive + " expects " + (expected - 1)
                        + " value(s) at line " + lineNumber);
            }
        }

        private static float parseFloat(String value, String directive, int lineNumber) {
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid float for " + directive + " at line " + lineNumber + ": " + value, ex);
            }
        }

        private static int parseInteger(String value, String directive, int lineNumber) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid integer for " + directive + " at line " + lineNumber + ": " + value, ex);
            }
        }

        private static Animation.PlayMode parsePlayMode(String value, int lineNumber) {
            try {
                Animation.PlayMode playMode = Animation.PlayMode.valueOf(value);
                switch (playMode) {
                    case NORMAL:
                    case LOOP:
                    case REVERSED:
                    case LOOP_REVERSED:
                    case LOOP_PINGPONG:
                        return playMode;
                    default:
                        throw new IllegalArgumentException("Unsupported playMode at line " + lineNumber + ": " + value);
                }
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid playMode at line " + lineNumber + ": " + value, ex);
            }
        }
    }

    private static final class Sample {
        private final String originalTime;
        private final float stateTime;

        private Sample(String originalTime, float stateTime) {
            this.originalTime = originalTime;
            this.stateTime = stateTime;
        }
    }
}
