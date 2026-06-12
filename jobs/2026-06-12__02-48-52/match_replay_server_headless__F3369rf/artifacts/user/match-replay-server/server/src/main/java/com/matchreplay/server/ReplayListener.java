package com.matchreplay.server;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.matchreplay.core.MatchLog;
import com.matchreplay.core.Simulation;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

/**
 * {@link ApplicationAdapter} that drives one tick of the simulation per
 * {@link #render()} call.
 *
 * <p>After the last tick has been applied it calls {@link Gdx#app}{@code .exit()}
 * (which is asynchronous: it posts a {@code Runnable} that flips the headless
 * main-loop's {@code running} flag to {@code false} on the very next iteration).
 * Once the loop exits the framework calls {@link #dispose()}, which counts down
 * the {@link #doneLatch} so the main thread can safely proceed to write the
 * transcript.
 */
public class ReplayListener extends ApplicationAdapter {

    private static final Logger LOG = Logger.getLogger(ReplayListener.class.getName());

    private final MatchLog       matchLog;
    private       Simulation     simulation;

    /**
     * Counted down to {@code 0} inside {@link #dispose()} so the launching
     * thread knows the headless loop has fully stopped.
     */
    public final CountDownLatch doneLatch = new CountDownLatch(1);

    /**
     * Set to {@code true} inside {@link #render()} once all ticks are done
     * and {@link Gdx#app}{@code .exit()} has been called.  Guards against the
     * off-chance that {@code render()} is invoked an extra time before the flag
     * propagates.
     */
    private volatile boolean exitRequested = false;

    public ReplayListener(MatchLog matchLog) {
        this.matchLog = matchLog;
    }

    // -----------------------------------------------------------------------
    // ApplicationListener lifecycle
    // -----------------------------------------------------------------------

    @Override
    public void create() {
        simulation = new Simulation(matchLog);
        LOG.info("ReplayListener.create(): simulation initialised, totalTicks=" + matchLog.totalTicks);
    }

    @Override
    public void render() {
        if (exitRequested) return;

        if (simulation.getCurrentTick() < matchLog.totalTicks) {
            simulation.tick();
        }

        // Once we have completed all required ticks, signal the application to stop.
        if (simulation.getCurrentTick() >= matchLog.totalTicks) {
            exitRequested = true;
            LOG.info("All ticks applied. Requesting exit.");
            Gdx.app.exit();
        }
    }

    @Override
    public void dispose() {
        LOG.info("ReplayListener.dispose(): signalling main thread.");
        doneLatch.countDown();
    }

    // -----------------------------------------------------------------------
    // Results (safe to read only after doneLatch reaches 0)
    // -----------------------------------------------------------------------

    public Simulation getSimulation() {
        return simulation;
    }
}
