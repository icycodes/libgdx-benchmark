package com.matchreplay.server;

import com.badlogic.gdx.backends.headless.mock.input.MockInput;
import com.matchreplay.core.Command;
import com.matchreplay.core.CommandProvider;
import java.util.List;
import java.util.Map;

public class ReplayInput extends MockInput implements CommandProvider {
    private final Map<Integer, List<Command>> commandsByTick;

    public ReplayInput(Map<Integer, List<Command>> commandsByTick) {
        this.commandsByTick = commandsByTick;
    }

    @Override
    public List<Command> getCommandsForTick(int tick) {
        return commandsByTick.get(tick);
    }
}
