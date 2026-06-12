package com.matchreplay.core;

/**
 * Immutable record of a single command entry from the match log:
 * the tick at which it is applied, which player it targets, and the
 * command itself.
 */
public final class TickCommand {

    public final int     tick;
    public final int     playerId;
    public final Command command;

    public TickCommand(int tick, int playerId, Command command) {
        this.tick     = tick;
        this.playerId = playerId;
        this.command  = command;
    }

    @Override
    public String toString() {
        return "TickCommand{tick=" + tick + ", playerId=" + playerId + ", command=" + command + "}";
    }
}
