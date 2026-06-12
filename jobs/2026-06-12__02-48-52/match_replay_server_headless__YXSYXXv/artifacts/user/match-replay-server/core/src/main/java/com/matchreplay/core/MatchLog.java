package com.matchreplay.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchLog {
    public int width;
    public int height;
    public int totalTicks = 0;
    public int commandsApplied = 0;
    
    public Map<Integer, Player> players = new HashMap<>();
    // tick -> (player_id -> command)
    public Map<Integer, Map<Integer, String>> commands = new HashMap<>();
    
    public static class Player {
        public int id;
        public int startX;
        public int startY;
        public int finalX;
        public int finalY;
    }
}
