package com.example.matchreplay.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public final class Transcript {
    private final WorldSpec world;
    private final List<PlayerSnapshot> players;
    private final int totalTicks;
    private final int commandsApplied;
    private final String stateHash;

    public Transcript(WorldSpec world, List<PlayerSnapshot> players, int totalTicks, int commandsApplied, String stateHash) {
        this.world = world;
        this.players = players;
        this.totalTicks = totalTicks;
        this.commandsApplied = commandsApplied;
        this.stateHash = stateHash;
    }

    public static Transcript from(MatchLog matchLog, GridWorldSimulation simulation) {
        return new Transcript(
                matchLog.getWorld(),
                simulation.snapshotPlayers(),
                matchLog.getTotalTicks(),
                matchLog.getCommandsApplied(),
                sha256Hex(simulation.canonicalStateString()));
    }

    public String toJson() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"world\": { \"width\": ").append(world.getWidth())
                .append(", \"height\": ").append(world.getHeight()).append(" },\n");
        builder.append("  \"players\": [\n");
        for (int i = 0; i < players.size(); i++) {
            PlayerSnapshot player = players.get(i);
            builder.append("    { \"id\": ").append(player.getId())
                    .append(", \"startX\": ").append(player.getStartX())
                    .append(", \"startY\": ").append(player.getStartY())
                    .append(", \"finalX\": ").append(player.getFinalX())
                    .append(", \"finalY\": ").append(player.getFinalY())
                    .append(" }");
            if (i < players.size() - 1) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append("  ],\n");
        builder.append("  \"totalTicks\": ").append(totalTicks).append(",\n");
        builder.append("  \"commandsApplied\": ").append(commandsApplied).append(",\n");
        builder.append("  \"stateHash\": \"").append(stateHash).append("\"\n");
        builder.append("}\n");
        return builder.toString();
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                hex.append(String.format("%02x", value & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
