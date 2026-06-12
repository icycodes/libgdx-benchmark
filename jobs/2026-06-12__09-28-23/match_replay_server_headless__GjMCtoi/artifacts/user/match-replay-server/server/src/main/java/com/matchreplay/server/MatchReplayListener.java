package com.matchreplay.server;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.matchreplay.core.Simulation;

/**
 * ApplicationListener that drives the simulation one tick per render() call.
 * After all ticks are processed, it calls Gdx.app.exit() to stop the headless loop.
 */
public class MatchReplayListener implements ApplicationListener {
    private final Simulation simulation;
    private final ReplayInput replayInput;
    private boolean finished;

    public MatchReplayListener(Simulation simulation, ReplayInput replayInput) {
        this.simulation = simulation;
        this.replayInput = replayInput;
        this.finished = false;
    }

    @Override
    public void create() {
        // Nothing to create
    }

    @Override
    public void resize(int width, int height) {
        // Not used
    }

    @Override
    public void render() {
        if (finished) {
            return;
        }

        boolean hasMore = simulation.tick();

        if (!hasMore) {
            finished = true;
            Gdx.app.exit();
        }
    }

    @Override
    public void pause() {
        // Not used
    }

    @Override
    public void resume() {
        // Not used
    }

    @Override
    public void dispose() {
        // Not used
    }

    public Simulation getSimulation() {
        return simulation;
    }
}
