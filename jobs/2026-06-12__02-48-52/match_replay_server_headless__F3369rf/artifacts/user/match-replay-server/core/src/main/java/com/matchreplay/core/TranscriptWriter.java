package com.matchreplay.core;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Serialises the final simulation state to the required JSON transcript format
 * and computes the canonical {@code stateHash}.
 *
 * <p>This class has no libGDX dependency and can be used from any module.
 */
public class TranscriptWriter {

    private TranscriptWriter() {}

    // -----------------------------------------------------------------------
    // Public entry point
    // -----------------------------------------------------------------------

    /**
     * Write the transcript JSON to {@code outputPath}.
     *
     * @param worldWidth      world width from the match log
     * @param worldHeight     world height from the match log
     * @param players         final player states, sorted by id ascending
     * @param totalTicks      total simulation ticks
     * @param commandsApplied number of command lines applied
     * @param outputPath      absolute path of the output file to create/overwrite
     */
    public static void write(int worldWidth, int worldHeight,
                              List<Player> players,
                              int totalTicks, int commandsApplied,
                              String outputPath) throws IOException {
        String stateHash = computeStateHash(worldWidth, worldHeight, players);
        String json      = buildJson(worldWidth, worldHeight, players,
                                     totalTicks, commandsApplied, stateHash);

        try (Writer w = new OutputStreamWriter(
                new FileOutputStream(outputPath), StandardCharsets.UTF_8)) {
            w.write(json);
        }
    }

    // -----------------------------------------------------------------------
    // State hash
    // -----------------------------------------------------------------------

    /**
     * Compute the lowercase-hex SHA-256 of the canonical state string:
     * {@code W=<W>;H=<H>;P=<id1>:<x1>,<y1>|<id2>:<x2>,<y2>|...}
     */
    public static String computeStateHash(int worldWidth, int worldHeight, List<Player> players) {
        String canonical = buildCanonicalState(worldWidth, worldHeight, players);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /** Canonical state string without the hash -- exposed for testing. */
    public static String buildCanonicalState(int worldWidth, int worldHeight, List<Player> players) {
        StringBuilder sb = new StringBuilder();
        sb.append("W=").append(worldWidth)
          .append(";H=").append(worldHeight)
          .append(";P=");
        for (int i = 0; i < players.size(); i++) {
            if (i > 0) sb.append('|');
            Player p = players.get(i);
            sb.append(p.id).append(':').append(p.x).append(',').append(p.y);
        }
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // JSON serialisation (hand-rolled to avoid extra dependencies)
    // -----------------------------------------------------------------------

    private static String buildJson(int worldWidth, int worldHeight,
                                    List<Player> players,
                                    int totalTicks, int commandsApplied,
                                    String stateHash) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"world\": { \"width\": ").append(worldWidth)
          .append(", \"height\": ").append(worldHeight).append(" },\n");

        sb.append("  \"players\": [\n");
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            sb.append("    { \"id\": ").append(p.id)
              .append(", \"startX\": ").append(p.startX)
              .append(", \"startY\": ").append(p.startY)
              .append(", \"finalX\": ").append(p.x)
              .append(", \"finalY\": ").append(p.y)
              .append(" }");
            if (i < players.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        sb.append("  \"totalTicks\": ").append(totalTicks).append(",\n");
        sb.append("  \"commandsApplied\": ").append(commandsApplied).append(",\n");
        sb.append("  \"stateHash\": \"").append(stateHash).append("\"\n");
        sb.append("}\n");
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Utilities
    // -----------------------------------------------------------------------

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}
