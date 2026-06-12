package com.matchreplay.core;

public enum Command {
    MOVE_UP,
    MOVE_DOWN,
    MOVE_LEFT,
    MOVE_RIGHT,
    NOOP;

    public static Command fromString(String s) {
        return valueOf(s.toUpperCase());
    }
}
