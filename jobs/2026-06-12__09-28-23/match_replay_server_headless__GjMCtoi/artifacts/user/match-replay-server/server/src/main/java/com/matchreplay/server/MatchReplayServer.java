package com.matchreplay.server;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.matchreplay.core.MatchLogParser;
import com.matchreplay.core.PlayerState;
import com.matchreplay.core.Simulation;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MatchReplayServer {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: MatchReplayServer <input_log_path> <output_transcript_path>");
            System.exit(1);
        }

        String inputPath = args[0];
        String outputPath = args[1];

        // Parse the match log
        MatchLogParser parser = new MatchLogParser();
        parser.parse(inputPath);

        // Create simulation
        Simulation simulation = new Simulation(parser);

        // Create custom input
        ReplayInput replayInput = new ReplayInput();
        replayInput.setCommands(new ArrayList<>(parser.getCommands()), parser.getTotalTicks());

        // Create listener
        MatchReplayListener listener = new MatchReplayListener(simulation, replayInput);

        // Configure headless application for fast ticking
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 60;

        // Create headless application
        HeadlessApplication app = new HeadlessApplication(listener, config);

        // Replace Gdx.input with our custom input after construction
        Gdx.input = replayInput;

        // Wait for the main loop thread to finish
        try {
            Thread mainLoopThread = getMainLoopThread(app);
            if (mainLoopThread != null) {
                mainLoopThread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Write transcript
        writeTranscript(outputPath, simulation);
    }

    private static Thread getMainLoopThread(HeadlessApplication app) {
        try {
            java.lang.reflect.Field field = HeadlessApplication.class.getDeclaredField("mainLoopThread");
            field.setAccessible(true);
            return (Thread) field.get(app);
        } catch (Exception e) {
            // Fallback: just sleep a bit
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    private static void writeTranscript(String path, Simulation simulation) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\n");

        // world
        json.append("  \"world\": { \"width\": ").append(simulation.getWorldWidth())
            .append(", \"height\": ").append(simulation.getWorldHeight()).append(" },\n");

        // players - sorted by id ascending
        List<PlayerState> sortedPlayers = new ArrayList<>(simulation.getPlayers().values());
        Collections.sort(sortedPlayers, new Comparator<PlayerState>() {
            @Override
            public int compare(PlayerState a, PlayerState b) {
                return Integer.compare(a.id, b.id);
            }
        });

        json.append("  \"players\": [\n");
        for (int i = 0; i < sortedPlayers.size(); i++) {
            PlayerState p = sortedPlayers.get(i);
            json.append("    { \"id\": ").append(p.id)
                .append(", \"startX\": ").append(p.startX)
                .append(", \"startY\": ").append(p.startY)
                .append(", \"finalX\": ").append(p.getFinalX())
                .append(", \"finalY\": ").append(p.getFinalY()).append(" }");
            if (i < sortedPlayers.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("  ],\n");

        // totalTicks
        json.append("  \"totalTicks\": ").append(simulation.getTotalTicks()).append(",\n");

        // commandsApplied
        json.append("  \"commandsApplied\": ").append(simulation.getCommandsApplied()).append(",\n");

        // stateHash
        json.append("  \"stateHash\": \"").append(simulation.computeStateHash()).append("\"\n");

        json.append("}\n");

        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(path), StandardCharsets.UTF_8)) {
            writer.write(json.toString());
        }
    }
}
