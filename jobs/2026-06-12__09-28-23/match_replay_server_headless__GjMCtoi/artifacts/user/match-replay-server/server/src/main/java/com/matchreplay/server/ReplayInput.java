package com.matchreplay.server;

import com.badlogic.gdx.backends.headless.mock.input.MockInput;
import com.matchreplay.core.CommandEntry;

import java.util.List;

/**
 * Custom MockInput subclass that exposes per-tick command data to the simulation.
 * The simulation reads commands from here each tick.
 */
public class ReplayInput extends MockInput {
    private List<CommandEntry> commands;
    private int totalTicks;

    public void setCommands(List<CommandEntry> commands, int totalTicks) {
        this.commands = commands;
        this.totalTicks = totalTicks;
    }

    public List<CommandEntry> getCommands() {
        return commands;
    }

    public int getTotalTicks() {
        return totalTicks;
    }
}
