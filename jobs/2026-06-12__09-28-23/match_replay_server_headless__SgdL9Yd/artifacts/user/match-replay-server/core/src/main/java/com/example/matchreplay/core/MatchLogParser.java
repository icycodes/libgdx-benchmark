package com.example.matchreplay.core;

import com.badlogic.gdx.files.FileHandle;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class MatchLogParser {
    private MatchLogParser() {
    }

    public static MatchLog parse(FileHandle fileHandle) {
        if (fileHandle == null || !fileHandle.exists()) {
            throw new IllegalArgumentException("Input match log does not exist");
        }
        return parse(fileHandle.readString(String.valueOf(StandardCharsets.UTF_8)));
    }

    public static MatchLog parse(String source) {
        WorldSpec world = null;
        List<PlayerStart> starts = new ArrayList<>();
        List<CommandEntry> commands = new ArrayList<>();

        String[] lines = source.split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index].trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\\s+");
            try {
                if ("WORLD".equals(parts[0])) {
                    if (parts.length != 3) {
                        throw new IllegalArgumentException("WORLD requires width and height");
                    }
                    if (world != null) {
                        throw new IllegalArgumentException("Only one WORLD directive is allowed");
                    }
                    world = new WorldSpec(parseInt(parts[1], "width"), parseInt(parts[2], "height"));
                } else if ("START".equals(parts[0])) {
                    if (parts.length != 4) {
                        throw new IllegalArgumentException("START requires player_id, x, and y");
                    }
                    starts.add(new PlayerStart(
                            parseInt(parts[1], "player_id"),
                            parseInt(parts[2], "x"),
                            parseInt(parts[3], "y")));
                } else {
                    if (parts.length != 3) {
                        throw new IllegalArgumentException("Command line requires tick, player_id, and COMMAND");
                    }
                    commands.add(new CommandEntry(
                            parseInt(parts[0], "tick"),
                            parseInt(parts[1], "player_id"),
                            Command.valueOf(parts[2])));
                }
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Invalid match log line " + (index + 1) + ": " + line, exception);
            }
        }

        MatchLog matchLog = new MatchLog(world, starts, commands);
        validateStartsInsideWorld(matchLog);
        return matchLog;
    }

    private static int parseInt(String raw, String name) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be an integer: " + raw, exception);
        }
    }

    private static void validateStartsInsideWorld(MatchLog matchLog) {
        int width = matchLog.getWorld().getWidth();
        int height = matchLog.getWorld().getHeight();
        for (PlayerStart start : matchLog.getPlayerStarts()) {
            if (start.getX() < 0 || start.getX() >= width || start.getY() < 0 || start.getY() >= height) {
                throw new IllegalArgumentException("START position for player " + start.getId() + " is outside the world");
            }
        }
    }
}
