package com.matchreplay.core;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parses and holds the contents of a match log file.
 *
 * <p>Format (UTF-8, LF line endings):
 * <ul>
 *   <li>Lines starting with {@code #} or blank lines are ignored.</li>
 *   <li>Exactly one {@code WORLD <W> <H>} directive.</li>
 *   <li>One or more {@code START <player_id> <x> <y>} directives.</li>
 *   <li>Zero or more {@code <tick> <player_id> <COMMAND>} lines.</li>
 * </ul>
 */
public class MatchLog {

    /** World width. */
    public final int worldWidth;
    /** World height. */
    public final int worldHeight;

    /**
     * Players ordered by id ascending, exactly as declared by START directives.
     * Positions are still at their start values; the simulation mutates them.
     */
    public final List<Player> players;

    /**
     * All command lines that reference a declared player, in parse order.
     * The simulation will sort per-tick entries by player id before applying.
     */
    public final List<TickCommand> commands;

    /**
     * Total number of simulation ticks: {@code max(tick) + 1} across all
     * command lines, or {@code 0} if there are no command lines.
     */
    public final int totalTicks;

    private MatchLog(int worldWidth, int worldHeight,
                     List<Player> players, List<TickCommand> commands, int totalTicks) {
        this.worldWidth  = worldWidth;
        this.worldHeight = worldHeight;
        this.players     = Collections.unmodifiableList(players);
        this.commands    = Collections.unmodifiableList(commands);
        this.totalTicks  = totalTicks;
    }

    // -----------------------------------------------------------------------
    // Parsing
    // -----------------------------------------------------------------------

    /**
     * Parse the match log at the given absolute file path.
     *
     * @param filePath absolute path to the log file
     * @return a fully-parsed {@link MatchLog}
     * @throws IOException              if the file cannot be read
     * @throws IllegalArgumentException if the file is malformed
     */
    public static MatchLog parse(String filePath) throws IOException {
        List<String> lines = readLines(filePath);
        return parseLines(lines);
    }

    private static List<String> readLines(String filePath) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    static MatchLog parseLines(List<String> lines) {
        int worldWidth  = -1;
        int worldHeight = -1;

        // Preserve declaration order; keyed by player id for lookup.
        List<Player>      players       = new ArrayList<>();
        boolean[]         playerDeclared = new boolean[5]; // ids 1..4
        List<TickCommand> allCommands   = new ArrayList<>();
        int               maxTick       = -1;

        for (String rawLine : lines) {
            String line = rawLine.trim();

            // Skip blank lines and comments.
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] tokens = line.split("\\s+");

            if (tokens[0].equalsIgnoreCase("WORLD")) {
                if (tokens.length != 3) {
                    throw new IllegalArgumentException("Malformed WORLD directive: " + line);
                }
                worldWidth  = Integer.parseInt(tokens[1]);
                worldHeight = Integer.parseInt(tokens[2]);
                if (worldWidth  < 1 || worldWidth  > 64) throw new IllegalArgumentException("WORLD width out of range: "  + worldWidth);
                if (worldHeight < 1 || worldHeight > 64) throw new IllegalArgumentException("WORLD height out of range: " + worldHeight);

            } else if (tokens[0].equalsIgnoreCase("START")) {
                if (tokens.length != 4) {
                    throw new IllegalArgumentException("Malformed START directive: " + line);
                }
                int playerId = Integer.parseInt(tokens[1]);
                int sx       = Integer.parseInt(tokens[2]);
                int sy       = Integer.parseInt(tokens[3]);
                if (playerId < 1 || playerId > 4) {
                    throw new IllegalArgumentException("player_id out of range [1,4]: " + playerId);
                }
                if (worldWidth == -1) {
                    throw new IllegalArgumentException("START before WORLD directive");
                }
                if (sx < 0 || sx >= worldWidth || sy < 0 || sy >= worldHeight) {
                    throw new IllegalArgumentException(
                            "START position (" + sx + "," + sy + ") out of world bounds");
                }
                if (playerDeclared[playerId]) {
                    throw new IllegalArgumentException("Duplicate START for player_id " + playerId);
                }
                playerDeclared[playerId] = true;
                players.add(new Player(playerId, sx, sy));

            } else {
                // Must be a command line: <tick> <player_id> <COMMAND>
                if (tokens.length != 3) {
                    throw new IllegalArgumentException("Malformed command line: " + line);
                }
                int     tick     = Integer.parseInt(tokens[0]);
                int     playerId = Integer.parseInt(tokens[1]);
                Command cmd      = Command.parse(tokens[2]);

                // Ignore commands for undeclared players (do not count them).
                if (playerId < 1 || playerId > 4 || !playerDeclared[playerId]) {
                    continue;
                }

                allCommands.add(new TickCommand(tick, playerId, cmd));
                if (tick > maxTick) maxTick = tick;
            }
        }

        if (worldWidth == -1) {
            throw new IllegalArgumentException("No WORLD directive found in match log");
        }
        if (players.isEmpty()) {
            throw new IllegalArgumentException("No START directives found in match log");
        }

        // Sort players by id ascending for deterministic output.
        players.sort((a, b) -> Integer.compare(a.id, b.id));

        int totalTicks = (maxTick >= 0) ? maxTick + 1 : 0;
        return new MatchLog(worldWidth, worldHeight, players, allCommands, totalTicks);
    }
}
