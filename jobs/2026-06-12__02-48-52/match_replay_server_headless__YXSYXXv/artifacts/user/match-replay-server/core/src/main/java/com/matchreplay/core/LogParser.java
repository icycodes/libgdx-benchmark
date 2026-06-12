package com.matchreplay.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import java.util.HashMap;
import java.util.Map;
import java.io.BufferedReader;
import java.io.IOException;

public class LogParser {
    public static MatchLog parse(String filePath) throws IOException {
        MatchLog log = new MatchLog();
        FileHandle file = Gdx.files.absolute(filePath);
        BufferedReader reader = new BufferedReader(file.reader("UTF-8"));
        
        String line;
        int maxTick = -1;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            
            String[] parts = line.split("\\s+");
            if (parts[0].equals("WORLD")) {
                log.width = Integer.parseInt(parts[1]);
                log.height = Integer.parseInt(parts[2]);
            } else if (parts[0].equals("START")) {
                int id = Integer.parseInt(parts[1]);
                MatchLog.Player p = new MatchLog.Player();
                p.id = id;
                p.startX = Integer.parseInt(parts[2]);
                p.startY = Integer.parseInt(parts[3]);
                p.finalX = p.startX;
                p.finalY = p.startY;
                log.players.put(id, p);
            } else {
                // Should be tick player_id command
                int tick = Integer.parseInt(parts[0]);
                int id = Integer.parseInt(parts[1]);
                String cmd = parts[2];
                
                if (tick > maxTick) maxTick = tick;
                
                log.commands.computeIfAbsent(tick, k -> new HashMap<>()).put(id, cmd);
            }
        }
        reader.close();
        
        if (maxTick >= 0) {
            log.totalTicks = maxTick + 1;
        } else {
            log.totalTicks = 0;
        }
        
        return log;
    }
}
