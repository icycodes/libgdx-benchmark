package com.matchreplay.core;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MatchLogParser {
    private int worldWidth = -1;
    private int worldHeight = -1;
    private final Map<Integer, PlayerState> players = new LinkedHashMap<>();
    private final List<CommandEntry> commands = new ArrayList<>();

    public void parse(String path) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\\s+");
                if (parts.length == 0) continue;

                if ("WORLD".equals(parts[0])) {
                    if (parts.length != 3) {
                        throw new IllegalArgumentException("Invalid WORLD directive: " + line);
                    }
                    worldWidth = Integer.parseInt(parts[1]);
                    worldHeight = Integer.parseInt(parts[2]);
                } else if ("START".equals(parts[0])) {
                    if (parts.length != 4) {
                        throw new IllegalArgumentException("Invalid START directive: " + line);
                    }
                    int playerId = Integer.parseInt(parts[1]);
                    int x = Integer.parseInt(parts[2]);
                    int y = Integer.parseInt(parts[3]);
                    players.put(playerId, new PlayerState(playerId, x, y));
                } else {
                    // Command line: <tick> <player_id> <COMMAND>
                    if (parts.length != 3) {
                        throw new IllegalArgumentException("Invalid command line: " + line);
                    }
                    int tick = Integer.parseInt(parts[0]);
                    int playerId = Integer.parseInt(parts[1]);
                    Command command = Command.fromString(parts[2]);
                    commands.add(new CommandEntry(tick, playerId, command));
                }
            }
        }

        if (worldWidth < 0 || worldHeight < 0) {
            throw new IllegalArgumentException("Missing WORLD directive");
        }
        if (players.isEmpty()) {
            throw new IllegalArgumentException("Missing START directives");
        }
    }

    public int getWorldWidth() {
        return worldWidth;
    }

    public int getWorldHeight() {
        return worldHeight;
    }

    public Map<Integer, PlayerState> getPlayers() {
        return Collections.unmodifiableMap(players);
    }

    public List<CommandEntry> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    public int getTotalTicks() {
        if (commands.isEmpty()) {
            return 0;
        }
        int maxTick = 0;
        for (CommandEntry cmd : commands) {
            if (cmd.tick > maxTick) {
                maxTick = cmd.tick;
            }
        }
        return maxTick + 1;
    }
}
