package com.matchreplay.core;

public class Command {
    private final int tick;
    private final int playerId;
    private final CommandType type;

    public Command(int tick, int playerId, CommandType type) {
        this.tick = tick;
        this.playerId = playerId;
        this.type = type;
    }

    public int getTick() {
        return tick;
    }

    public int getPlayerId() {
        return playerId;
    }

    public CommandType getType() {
        return type;
    }
}
