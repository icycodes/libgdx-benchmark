package com.matchreplay.core;

public class CommandEntry implements Comparable<CommandEntry> {
    public final int tick;
    public final int playerId;
    public final Command command;

    public CommandEntry(int tick, int playerId, Command command) {
        this.tick = tick;
        this.playerId = playerId;
        this.command = command;
    }

    @Override
    public int compareTo(CommandEntry other) {
        int cmp = Integer.compare(this.tick, other.tick);
        if (cmp != 0) return cmp;
        return Integer.compare(this.playerId, other.playerId);
    }
}
