package com.matchreplay.core;

/**
 * Per-tick movement command for a single player.
 */
public enum Command {
    MOVE_UP,
    MOVE_DOWN,
    MOVE_LEFT,
    MOVE_RIGHT,
    NOOP;

    /**
     * Parse a command token from the match log.
     *
     * @param token the raw string token, e.g. "MOVE_UP"
     * @return the matching Command
     * @throws IllegalArgumentException if the token is unrecognised
     */
    public static Command parse(String token) {
        return Command.valueOf(token.trim().toUpperCase());
    }
}
