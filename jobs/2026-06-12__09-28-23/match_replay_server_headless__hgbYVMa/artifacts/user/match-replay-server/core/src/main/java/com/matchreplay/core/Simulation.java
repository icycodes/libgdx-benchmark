package com.matchreplay.core;

import com.badlogic.gdx.Gdx;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Simulation {
    private int width;
    private int height;
    private final Map<Integer, Player> players = new TreeMap<>();
    private final Map<Integer, List<Command>> commandsByTick = new HashMap<>();
    private int totalTicks = 0;
    private int commandsApplied = 0;
    private int currentTick = 0;

    public void loadLog(List<String> lines) {
        int maxTick = -1;
        boolean hasAnyCommandLine = false;

        // First pass: parse WORLD and START to know which players are declared
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length == 0) continue;

            if (parts[0].equals("WORLD")) {
                this.width = Integer.parseInt(parts[1]);
                this.height = Integer.parseInt(parts[2]);
            } else if (parts[0].equals("START")) {
                int playerId = Integer.parseInt(parts[1]);
                int x = Integer.parseInt(parts[2]);
                int y = Integer.parseInt(parts[3]);
                players.put(playerId, new Player(playerId, x, y));
            }
        }

        // Second pass: parse commands
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length == 0) continue;

            if (!parts[0].equals("WORLD") && !parts[0].equals("START")) {
                // Command line: <tick> <player_id> <COMMAND>
                int tick = Integer.parseInt(parts[0]);
                int playerId = Integer.parseInt(parts[1]);
                String cmdStr = parts[2];
                CommandType type = CommandType.valueOf(cmdStr);

                hasAnyCommandLine = true;
                if (tick > maxTick) {
                    maxTick = tick;
                }

                // Check if the player is declared
                if (players.containsKey(playerId)) {
                    Command cmd = new Command(tick, playerId, type);
                    commandsByTick.computeIfAbsent(tick, k -> new ArrayList<>()).add(cmd);
                    commandsApplied++;
                }
            }
        }

        if (hasAnyCommandLine) {
            this.totalTicks = maxTick + 1;
        } else {
            this.totalTicks = 0;
        }
        this.currentTick = 0;
    }

    public void step() {
        if (currentTick >= totalTicks) {
            return;
        }

        List<Command> tickCommands = null;
        if (Gdx.input instanceof CommandProvider) {
            tickCommands = ((CommandProvider) Gdx.input).getCommandsForTick(currentTick);
        } else {
            tickCommands = commandsByTick.get(currentTick);
        }

        if (tickCommands != null) {
            // Create a copy of the list to avoid modifying the original list when sorting
            List<Command> sortedCommands = new ArrayList<>(tickCommands);
            // Sort by playerId ascending (stable sort)
            sortedCommands.sort(Comparator.comparingInt(Command::getPlayerId));

            for (Command cmd : sortedCommands) {
                Player player = players.get(cmd.getPlayerId());
                if (player != null) {
                    int newX = player.getX();
                    int newY = player.getY();
                    switch (cmd.getType()) {
                        case MOVE_UP:
                            newY++;
                            break;
                        case MOVE_DOWN:
                            newY--;
                            break;
                        case MOVE_LEFT:
                            newX--;
                            break;
                        case MOVE_RIGHT:
                            newX++;
                            break;
                        case NOOP:
                            break;
                    }
                    if (newX >= 0 && newX < width && newY >= 0 && newY < height) {
                        player.setX(newX);
                        player.setY(newY);
                    }
                }
            }
        }
        currentTick++;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Map<Integer, Player> getPlayers() {
        return players;
    }

    public Map<Integer, List<Command>> getCommandsByTick() {
        return commandsByTick;
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

    public String getStateHash() {
        StringBuilder sb = new StringBuilder();
        sb.append("W=").append(width).append(";H=").append(height).append(";P=");
        boolean first = true;
        for (Player p : players.values()) {
            if (!first) {
                sb.append("|");
            }
            sb.append(p.getId()).append(":").append(p.getX()).append(",").append(p.getY());
            first = false;
        }
        String canonicalStr = sb.toString();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(canonicalStr.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
