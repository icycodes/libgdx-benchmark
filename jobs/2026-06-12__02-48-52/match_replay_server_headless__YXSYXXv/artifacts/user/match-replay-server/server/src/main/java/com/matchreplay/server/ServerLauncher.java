package com.matchreplay.server;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.matchreplay.core.MatchLog;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class ServerLauncher {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: ServerLauncher <input_log_path> <output_transcript_path>");
            System.exit(1);
        }
        
        String inputPath = args[0];
        String outputPath = args[1];
        
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 60;
        
        ReplayApp app = new ReplayApp(inputPath);
        HeadlessApplication headlessApp = new HeadlessApplication(app, config);
        
        CommandInput commandInput = new CommandInput();
        Gdx.input = commandInput;
        app.setCommandInput(commandInput);
        
        app.waitForCompletion();
        
        MatchLog log = app.getLog();
        if (log != null) {
            writeTranscript(log, outputPath);
        } else {
            System.exit(1);
        }
    }
    
    private static void writeTranscript(MatchLog log, String outputPath) throws Exception {
        List<MatchLog.Player> players = new ArrayList<>(log.players.values());
        players.sort((a, b) -> Integer.compare(a.id, b.id));
        
        StringBuilder stateStr = new StringBuilder();
        stateStr.append("W=").append(log.width).append(";H=").append(log.height).append(";P=");
        for (int i = 0; i < players.size(); i++) {
            MatchLog.Player p = players.get(i);
            stateStr.append(p.id).append(":").append(p.finalX).append(",").append(p.finalY);
            if (i < players.size() - 1) {
                stateStr.append("|");
            }
        }
        
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(stateStr.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        Transcript t = new Transcript();
        t.world = new WorldDef(log.width, log.height);
        t.players = players;
        t.totalTicks = log.totalTicks;
        t.commandsApplied = log.commandsApplied;
        t.stateHash = hexString.toString().toLowerCase();
        
        Json json = new Json();
        json.setOutputType(OutputType.json);
        json.setUsePrototypes(false);
        String jsonString = json.toJson(t);
        
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outputPath), StandardCharsets.UTF_8)) {
            writer.write(jsonString);
        }
    }
    
    public static class Transcript {
        public WorldDef world;
        public List<MatchLog.Player> players;
        public int totalTicks;
        public int commandsApplied;
        public String stateHash;
    }
    
    public static class WorldDef {
        public int width;
        public int height;
        public WorldDef() {}
        public WorldDef(int w, int h) { width = w; height = h; }
    }
}
