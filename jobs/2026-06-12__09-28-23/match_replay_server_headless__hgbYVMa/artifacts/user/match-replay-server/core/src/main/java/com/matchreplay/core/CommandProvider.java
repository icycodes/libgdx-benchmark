package com.matchreplay.core;

import java.util.List;

public interface CommandProvider {
    List<Command> getCommandsForTick(int tick);
}
