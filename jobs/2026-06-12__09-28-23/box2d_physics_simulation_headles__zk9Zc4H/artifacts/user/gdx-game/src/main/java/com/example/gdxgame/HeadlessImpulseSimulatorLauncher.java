package com.example.gdxgame;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.MassData;
import com.badlogic.gdx.physics.box2d.World;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public final class HeadlessImpulseSimulatorLauncher {
    private HeadlessImpulseSimulatorLauncher() {
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length != 1 || !args[0].startsWith("--scenario=") || args[0].length() == "--scenario=".length()) {
            System.err.println("Error: expected exactly one --scenario=<file> argument");
            System.exit(1);
        }

        CountDownLatch finished = new CountDownLatch(1);
        AtomicInteger exitCode = new AtomicInteger(0);
        HeadlessApplicationConfiguration configuration = new HeadlessApplicationConfiguration();
        configuration.updatesPerSecond = 0;

        new HeadlessApplication(
            new SimulationApplication(Path.of(args[0].substring("--scenario=".length())), finished, exitCode),
            configuration
        );

        finished.await();
        if (exitCode.get() != 0) {
            System.exit(exitCode.get());
        }
    }

    private static final class SimulationApplication extends ApplicationAdapter {
        private static final float TIME_STEP = 1f / 60f;
        private static final int VELOCITY_ITERATIONS = 6;
        private static final int POSITION_ITERATIONS = 2;

        private final Path scenarioPath;
        private final CountDownLatch finished;
        private final AtomicInteger exitCode;
        private boolean completed;
        private World world;

        private SimulationApplication(Path scenarioPath, CountDownLatch finished, AtomicInteger exitCode) {
            this.scenarioPath = scenarioPath;
            this.finished = finished;
            this.exitCode = exitCode;
        }

        @Override
        public void render() {
            if (completed) {
                return;
            }
            completed = true;

            try {
                Scenario scenario = ScenarioParser.parse(scenarioPath);
                Vector2 finalPosition = runScenario(scenario);
                System.out.printf(Locale.ROOT, "Final position: (%.4f, %.4f)%n", finalPosition.x, finalPosition.y);
            } catch (ScenarioException | IOException | RuntimeException e) {
                exitCode.set(1);
                System.err.println("Error: " + e.getMessage());
            } finally {
                Gdx.app.exit();
            }
        }

        private Vector2 runScenario(Scenario scenario) {
            world = new World(scenario.gravity, true);
            Body body = createBody(scenario);

            for (int step = 0; step < scenario.steps; step++) {
                for (Impulse impulse : scenario.impulsesForStep(step)) {
                    body.applyLinearImpulse(impulse.vector(), body.getWorldCenter(), true);
                }
                world.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
            }

            return new Vector2(body.getPosition());
        }

        private Body createBody(Scenario scenario) {
            BodyDef bodyDef = new BodyDef();
            bodyDef.type = BodyDef.BodyType.DynamicBody;
            bodyDef.position.set(scenario.initialPosition);
            bodyDef.linearDamping = 0f;
            bodyDef.angularDamping = 0f;

            Body body = world.createBody(bodyDef);
            CircleShape shape = new CircleShape();
            try {
                shape.setRadius(0.5f);

                FixtureDef fixtureDef = new FixtureDef();
                fixtureDef.shape = shape;
                fixtureDef.density = 1.0f;
                fixtureDef.friction = 0f;
                fixtureDef.restitution = 0f;
                body.createFixture(fixtureDef);
            } finally {
                shape.dispose();
            }

            if (scenario.massOverride != null) {
                MassData massData = body.getMassData();
                massData.mass = scenario.massOverride;
                body.setMassData(massData);
            }

            return body;
        }

        @Override
        public void dispose() {
            if (world != null) {
                world.dispose();
                world = null;
            }
            finished.countDown();
        }
    }

    private record Scenario(Vector2 gravity, Vector2 initialPosition, Float massOverride, int steps, List<Impulse> impulses) {
        private List<Impulse> impulsesForStep(int step) {
            List<Impulse> result = new ArrayList<>();
            for (Impulse impulse : impulses) {
                if (impulse.step == step) {
                    result.add(impulse);
                }
            }
            return result;
        }
    }

    private record Impulse(int step, float ix, float iy, int order) {
        private Vector2 vector() {
            return new Vector2(ix, iy);
        }
    }

    private static final class ScenarioParser {
        private ScenarioParser() {
        }

        private static Scenario parse(Path path) throws IOException, ScenarioException {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            Vector2 gravity = null;
            Vector2 initialPosition = null;
            Float mass = null;
            Integer steps = null;
            List<Impulse> impulses = new ArrayList<>();
            int impulseOrder = 0;

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] tokens = line.split("\\s+");
                String keyword = tokens[0];
                int lineNumber = i + 1;
                switch (keyword) {
                    case "GRAVITY" -> {
                        requireTokenCount(tokens, 3, lineNumber);
                        if (gravity != null) {
                            throw new ScenarioException("duplicate GRAVITY at line " + lineNumber);
                        }
                        gravity = new Vector2(parseFloat(tokens[1], lineNumber), parseFloat(tokens[2], lineNumber));
                    }
                    case "BODY" -> {
                        requireTokenCount(tokens, 3, lineNumber);
                        if (initialPosition != null) {
                            throw new ScenarioException("duplicate BODY at line " + lineNumber);
                        }
                        initialPosition = new Vector2(parseFloat(tokens[1], lineNumber), parseFloat(tokens[2], lineNumber));
                    }
                    case "MASS" -> {
                        requireTokenCount(tokens, 2, lineNumber);
                        if (mass != null) {
                            throw new ScenarioException("duplicate MASS at line " + lineNumber);
                        }
                        mass = parseFloat(tokens[1], lineNumber);
                        if (mass <= 0f) {
                            throw new ScenarioException("MASS must be positive at line " + lineNumber);
                        }
                    }
                    case "IMPULSE" -> {
                        requireTokenCount(tokens, 4, lineNumber);
                        int step = parseInt(tokens[1], lineNumber);
                        if (step < 0) {
                            throw new ScenarioException("IMPULSE step must be non-negative at line " + lineNumber);
                        }
                        impulses.add(new Impulse(
                            step,
                            parseFloat(tokens[2], lineNumber),
                            parseFloat(tokens[3], lineNumber),
                            impulseOrder++
                        ));
                    }
                    case "STEPS" -> {
                        requireTokenCount(tokens, 2, lineNumber);
                        if (steps != null) {
                            throw new ScenarioException("duplicate STEPS at line " + lineNumber);
                        }
                        steps = parseInt(tokens[1], lineNumber);
                        if (steps < 0) {
                            throw new ScenarioException("STEPS must be non-negative at line " + lineNumber);
                        }
                    }
                    default -> throw new ScenarioException("unknown keyword '" + keyword + "' at line " + lineNumber);
                }
            }

            if (gravity == null) {
                throw new ScenarioException("missing required GRAVITY");
            }
            if (initialPosition == null) {
                throw new ScenarioException("missing required BODY");
            }
            if (steps == null) {
                throw new ScenarioException("missing required STEPS");
            }

            for (Impulse impulse : impulses) {
                if (impulse.step >= steps) {
                    throw new ScenarioException("IMPULSE step outside [0, STEPS): " + impulse.step);
                }
            }

            impulses.sort(Comparator.comparingInt(Impulse::step).thenComparingInt(Impulse::order));
            return new Scenario(gravity, initialPosition, mass, steps, List.copyOf(impulses));
        }

        private static void requireTokenCount(String[] tokens, int expected, int lineNumber) throws ScenarioException {
            if (tokens.length != expected) {
                throw new ScenarioException("wrong number of tokens at line " + lineNumber);
            }
        }

        private static float parseFloat(String token, int lineNumber) throws ScenarioException {
            try {
                return Float.parseFloat(token);
            } catch (NumberFormatException e) {
                throw new ScenarioException("invalid float '" + token + "' at line " + lineNumber);
            }
        }

        private static int parseInt(String token, int lineNumber) throws ScenarioException {
            try {
                return Integer.parseInt(token);
            } catch (NumberFormatException e) {
                throw new ScenarioException("invalid integer '" + token + "' at line " + lineNumber);
            }
        }
    }

    private static final class ScenarioException extends Exception {
        private ScenarioException(String message) {
            super(message);
        }
    }
}
