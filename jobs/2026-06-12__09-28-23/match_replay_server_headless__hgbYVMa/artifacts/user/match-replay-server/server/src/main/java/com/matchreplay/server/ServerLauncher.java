package com.matchreplay.server;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.matchreplay.core.Player;
import com.matchreplay.core.Simulation;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ServerLauncher {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: ServerLauncher <input_log_path> <output_transcript_path>");
            System.exit(1);
        }

        String inputLogPath = args[0];
        String outputTranscriptPath = args[1];

        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(inputLogPath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to read input log: " + e.getMessage());
            System.exit(1);
            return;
        }

        Simulation simulation = new Simulation();
        simulation.loadLog(lines);

        ApplicationAdapter listener = new ApplicationAdapter() {
            @Override
            public void create() {
                ReplayInput replayInput = new ReplayInput(simulation.getCommandsByTick());
                Gdx.input = replayInput;
            }

            @Override
            public void render() {
                if (simulation.getCurrentTick() < simulation.getTotalTicks()) {
                    simulation.step();
                } else {
                    Gdx.app.exit();
                }
            }
        };

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 1000;

        HeadlessApplication app = new HeadlessApplication(listener, config);

        // Replace Gdx.input with custom MockInput subclass after construction as well,
        // to strictly satisfy the requirement and avoid any race condition.
        ReplayInput replayInput = new ReplayInput(simulation.getCommandsByTick());
        Gdx.input = replayInput;

        // Wait for the headless main-loop thread to finish
        try {
            Field threadField = HeadlessApplication.class.getDeclaredField("mainLoopThread");
            threadField.setAccessible(true);
            Thread thread = (Thread) threadField.get(app);
            if (thread != null) {
                thread.join();
            }
        } catch (Exception e) {
            System.err.println("Error waiting for main loop thread: " + e.getMessage());
        }

        // Write the transcript
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"world\": {\n");
        json.append("    \"width\": ").append(simulation.getWidth()).append(",\n");
        json.append("    \"height\": ").append(simulation.getHeight()).append("\n");
        json.append("  },\n");
        json.append("  \"players\": [\n");

        int playerIndex = 0;
        int numPlayers = simulation.getPlayers().size();
        for (Player p : simulation.getPlayers().values()) {
            json.append("    {\n");
            json.append("      \"id\": ").append(p.getId()).append(",\n");
            json.append("      \"startX\": ").append(p.getStartX()).append(",\n");
            json.append("      \"startY\": ").append(p.getStartY()).append(",\n");
            json.append("      \"finalX\": ").append(p.getX()).append(",\n");
            json.append("      \"finalY\": ").append(p.getY()).append("\n");
            json.append("    }");
            if (playerIndex < numPlayers - 1) {
                json.append(",");
            }
            json.append("\n");
            playerIndex++;
        }

        json.append("  ],\n");
        json.append("  \"totalTicks\": ").append(simulation.getTotalTicks()).append(",\n");
        json.append("  \"commandsApplied\": ").append(simulation.getCommandsApplied()).append(",\n");
        json.append("  \"stateHash\": \"").append(simulation.getStateHash()).append("\"\n");
        json.append("}\n");

        try {
            // Ensure parent directories exist for output path
            java.nio.file.Path outPath = Paths.get(outputTranscriptPath);
            if (outPath.getParent() != null) {
                Files.createDirectories(outPath.getParent());
            }
            Files.write(outPath, json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("Failed to write transcript: " + e.getMessage());
            System.exit(1);
        }

        // Explicitly exit with 0 to ensure the Gradle invocation returns 0
        System.exit(0);
    }
}
