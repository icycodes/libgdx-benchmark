package com.matchreplay.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class Simulation {
    private final int worldWidth;
    private final int worldHeight;
    private final Map<Integer, PlayerState> players;
    private final List<CommandEntry> commands;
    private final int totalTicks;
    private int commandsApplied;
    private int currentTick;

    public Simulation(MatchLogParser parser) {
        this.worldWidth = parser.getWorldWidth();
        this.worldHeight = parser.getWorldHeight();
        this.players = parser.getPlayers();
        this.commands = new ArrayList<>(parser.getCommands());
        Collections.sort(this.commands);
        this.totalTicks = parser.getTotalTicks();
        this.commandsApplied = 0;
        this.currentTick = 0;
    }

    public int getWorldWidth() {
        return worldWidth;
    }

    public int getWorldHeight() {
        return worldHeight;
    }

    public Map<Integer, PlayerState> getPlayers() {
        return players;
    }

    public int getTotalTicks() {
        return totalTicks;
    }

    public int getCommandsApplied() {
        return commandsApplied;
    }

    public int getCurrentTick() {
        return currentTick;
    }

    /** Apply all commands for the given tick. Returns true if there are more ticks to process. */
    public boolean tick() {
        if (currentTick >= totalTicks) {
            return false;
        }

        // Collect and apply commands for this tick
        int cmdIdx = 0;
        while (cmdIdx < commands.size()) {
            CommandEntry cmd = commands.get(cmdIdx);
            if (cmd.tick == currentTick) {
                PlayerState player = players.get(cmd.playerId);
                if (player != null) {
                    applyCommand(player, cmd.command);
                    commandsApplied++;
                }
                // Commands referencing undeclared players are ignored (not counted)
                cmdIdx++;
            } else if (cmd.tick > currentTick) {
                break;
            } else {
                cmdIdx++;
            }
        }

        currentTick++;
        return currentTick < totalTicks;
    }

    private void applyCommand(PlayerState player, Command command) {
        switch (command) {
            case MOVE_UP:
                player.currentY = Math.min(player.currentY + 1, worldHeight - 1);
                break;
            case MOVE_DOWN:
                player.currentY = Math.max(player.currentY - 1, 0);
                break;
            case MOVE_RIGHT:
                player.currentX = Math.min(player.currentX + 1, worldWidth - 1);
                break;
            case MOVE_LEFT:
                player.currentX = Math.max(player.currentX - 1, 0);
                break;
            case NOOP:
                // No change
                break;
        }
    }

    public String computeStateHash() {
        StringBuilder sb = new StringBuilder();
        sb.append("W=").append(worldWidth);
        sb.append(";H=").append(worldHeight);
        sb.append(";P=");

        List<PlayerState> sortedPlayers = new ArrayList<>(players.values());
        Collections.sort(sortedPlayers, new Comparator<PlayerState>() {
            @Override
            public int compare(PlayerState a, PlayerState b) {
                return Integer.compare(a.id, b.id);
            }
        });

        boolean first = true;
        for (PlayerState p : sortedPlayers) {
            if (!first) sb.append("|");
            sb.append(p.id).append(":").append(p.currentX).append(",").append(p.currentY);
            first = false;
        }

        String stateString = sb.toString();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(stateString.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
