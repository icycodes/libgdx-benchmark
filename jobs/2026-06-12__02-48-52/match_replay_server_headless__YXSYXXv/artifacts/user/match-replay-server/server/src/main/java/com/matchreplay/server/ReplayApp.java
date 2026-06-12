package com.matchreplay.server;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.matchreplay.core.LogParser;
import com.matchreplay.core.MatchLog;
import com.matchreplay.core.Simulation;

import java.util.concurrent.CountDownLatch;

public class ReplayApp extends ApplicationAdapter {
    private String inputPath;
    private MatchLog log;
    private Simulation sim;
    private int currentTick = 0;
    private CountDownLatch latch = new CountDownLatch(1);
    private CommandInput commandInput;
    
    public ReplayApp(String inputPath) {
        this.inputPath = inputPath;
    }
    
    public void setCommandInput(CommandInput commandInput) {
        this.commandInput = commandInput;
    }
    
    @Override
    public void create() {
        try {
            log = LogParser.parse(inputPath);
            sim = new Simulation(log);
        } catch (Exception e) {
            e.printStackTrace();
            Gdx.app.exit();
        }
    }
    
    @Override
    public void render() {
        if (log == null) return;
        
        if (currentTick < log.totalTicks) {
            if (commandInput != null) {
                commandInput.setCurrentCommands(log.commands.get(currentTick));
            }
            sim.tick(currentTick);
            currentTick++;
        } else {
            Gdx.app.exit();
        }
    }
    
    @Override
    public void dispose() {
        latch.countDown();
    }
    
    public void waitForCompletion() throws InterruptedException {
        latch.await();
    }
    
    public MatchLog getLog() {
        return log;
    }
}
