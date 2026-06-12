package com.example.matchreplay.core;

public final class CommandEntry {
    private final int tick;
    private final int playerId;
    private final Command command;

    public CommandEntry(int tick, int playerId, Command command) {
        if (tick < 0) {
            throw new IllegalArgumentException("Tick must be non-negative");
        }
        if (playerId < 1 || playerId > 4) {
            throw new IllegalArgumentException("Player id must be in [1, 4]");
        }
        this.tick = tick;
        this.playerId = playerId;
        this.command = command;
    }

    public int getTick() {
        return tick;
    }

    public int getPlayerId() {
        return playerId;
    }

    public Command getCommand() {
        return command;
    }
}
