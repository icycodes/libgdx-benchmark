package com.example.matchreplay.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class MatchLog {
    private final WorldSpec world;
    private final List<PlayerStart> playerStarts;
    private final Map<Integer, Map<Integer, Command>> commandsByTick;
    private final int totalTicks;
    private final int commandsApplied;

    public MatchLog(WorldSpec world, List<PlayerStart> playerStarts, List<CommandEntry> commandEntries) {
        if (world == null) {
            throw new IllegalArgumentException("WORLD directive is required");
        }
        if (playerStarts == null || playerStarts.isEmpty()) {
            throw new IllegalArgumentException("At least one START directive is required");
        }
        this.world = world;
        this.playerStarts = sortedPlayerStarts(playerStarts);

        Map<Integer, PlayerStart> declaredPlayers = new TreeMap<>();
        for (PlayerStart start : this.playerStarts) {
            declaredPlayers.put(start.getId(), start);
        }

        Map<Integer, Map<Integer, Command>> mutableCommands = new TreeMap<>();
        int maxTick = -1;
        int countedCommands = 0;
        for (CommandEntry entry : commandEntries) {
            maxTick = Math.max(maxTick, entry.getTick());
            if (!declaredPlayers.containsKey(entry.getPlayerId())) {
                continue;
            }
            mutableCommands
                    .computeIfAbsent(entry.getTick(), ignored -> new TreeMap<>())
                    .put(entry.getPlayerId(), entry.getCommand());
            countedCommands++;
        }
        this.totalTicks = maxTick + 1;
        this.commandsApplied = countedCommands;

        Map<Integer, Map<Integer, Command>> immutableCommands = new TreeMap<>();
        for (Map.Entry<Integer, Map<Integer, Command>> tickEntry : mutableCommands.entrySet()) {
            immutableCommands.put(tickEntry.getKey(), Collections.unmodifiableMap(new TreeMap<>(tickEntry.getValue())));
        }
        this.commandsByTick = Collections.unmodifiableMap(immutableCommands);
    }

    public WorldSpec getWorld() {
        return world;
    }

    public List<PlayerStart> getPlayerStarts() {
        return playerStarts;
    }

    public Map<Integer, Command> getCommandsForTick(int tick) {
        Map<Integer, Command> commands = commandsByTick.get(tick);
        return commands == null ? Collections.emptyMap() : commands;
    }

    public int getTotalTicks() {
        return totalTicks;
    }

    public int getCommandsApplied() {
        return commandsApplied;
    }

    private static List<PlayerStart> sortedPlayerStarts(List<PlayerStart> playerStarts) {
        Map<Integer, PlayerStart> startsById = new TreeMap<>();
        for (PlayerStart start : playerStarts) {
            if (start.getId() < 1 || start.getId() > 4) {
                throw new IllegalArgumentException("Player id must be in [1, 4]");
            }
            if (startsById.put(start.getId(), start) != null) {
                throw new IllegalArgumentException("Duplicate START directive for player " + start.getId());
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(startsById.values()));
    }
}
