package com.matchreplay.server;

import com.badlogic.gdx.backends.headless.mock.input.MockInput;
import com.matchreplay.core.CommandProvider;
import java.util.Map;

public class CommandInput extends MockInput implements CommandProvider {
    private Map<Integer, String> currentCommands;
    
    public void setCurrentCommands(Map<Integer, String> commands) {
        this.currentCommands = commands;
    }
    
    @Override
    public Map<Integer, String> getCurrentCommands() {
        return currentCommands;
    }
}
