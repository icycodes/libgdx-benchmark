package com.example.matchreplay.server;

import com.badlogic.gdx.backends.headless.mock.input.MockInput;
import com.example.matchreplay.core.Command;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public final class ServerCommandInput extends MockInput {
    private Map<Integer, Command> currentTickCommands = Collections.emptyMap();

    public void setCurrentTickCommands(Map<Integer, Command> commandsByPlayerId) {
        currentTickCommands = Collections.unmodifiableMap(new TreeMap<>(commandsByPlayerId));
    }

    public Map<Integer, Command> getCurrentTickCommands() {
        return currentTickCommands;
    }
}
