package com.matchreplay.core;

import com.badlogic.gdx.Gdx;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Simulation {
    private MatchLog log;
    
    public Simulation(MatchLog log) {
        this.log = log;
    }
    
    public void tick(int tick) {
        if (!(Gdx.input instanceof CommandProvider)) {
            return;
        }
        
        CommandProvider provider = (CommandProvider) Gdx.input;
        Map<Integer, String> tickCmds = provider.getCurrentCommands();
        if (tickCmds == null) return;
        
        List<Integer> playerIds = new ArrayList<>(tickCmds.keySet());
        Collections.sort(playerIds);
        
        for (int id : playerIds) {
            MatchLog.Player p = log.players.get(id);
            if (p == null) continue; // Ignore commands for undeclared players
            
            log.commandsApplied++;
            
            String cmd = tickCmds.get(id);
            if (cmd.equals("MOVE_UP")) {
                p.finalY++;
            } else if (cmd.equals("MOVE_DOWN")) {
                p.finalY--;
            } else if (cmd.equals("MOVE_RIGHT")) {
                p.finalX++;
            } else if (cmd.equals("MOVE_LEFT")) {
                p.finalX--;
            }
            
            // Clamp
            if (p.finalX < 0) p.finalX = 0;
            if (p.finalX >= log.width) p.finalX = log.width - 1;
            if (p.finalY < 0) p.finalY = 0;
            if (p.finalY >= log.height) p.finalY = log.height - 1;
        }
    }
}
