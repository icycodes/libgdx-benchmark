package com.example.matchreplay.core;

import java.util.Map;

public final class ReplayRunner {
    private final MatchLog matchLog;
    private final GridWorldSimulation simulation;
    private int nextTick;

    public ReplayRunner(MatchLog matchLog) {
        this.matchLog = matchLog;
        this.simulation = new GridWorldSimulation(matchLog);
        this.nextTick = 0;
    }

    public boolean hasRemainingTicks() {
        return nextTick < matchLog.getTotalTicks();
    }

    public int getNextTick() {
        return nextTick;
    }

    public void applyNextTick(Map<Integer, Command> commandsByPlayerId) {
        if (!hasRemainingTicks()) {
            return;
        }
        simulation.applyTick(commandsByPlayerId);
        nextTick++;
    }

    public Transcript transcript() {
        return Transcript.from(matchLog, simulation);
    }
}
