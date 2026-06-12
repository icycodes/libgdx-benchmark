package com.example;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;

import java.lang.reflect.Field;

/**
 * A minimal libGDX ApplicationAdapter that:
 *   1. Resets a frame counter in create().
 *   2. Increments the counter on every render() call.
 *   3. Calls Gdx.app.exit() once the counter has reached the target tick count N.
 *   4. Prints "FRAME_COUNT: <N>" to stdout in dispose().
 *
 * Background on why we reach into HeadlessApplication.running directly:
 *   Gdx.app.exit() posts a Runnable to HeadlessApplication's runnable queue.
 *   That runnable sets running=false, but the queue is drained at the TOP of
 *   the next loop tick - BEFORE render() is called.  This means one additional
 *   render() invocation fires between exit() being called and the loop actually
 *   stopping, producing a frame count of N+1.
 *
 *   To get an exact count of N we set running=false directly via reflection
 *   inside render() right after incrementing to targetFrames.  The check at
 *   the bottom of the HeadlessApplication loop (after render() returns) then
 *   immediately exits without scheduling another render(), and dispose() sees
 *   the correct value.
 */
public class FrameCounterApp extends ApplicationAdapter {

    private final int targetFrames;
    private int frameCount;

    public FrameCounterApp(int targetFrames) {
        this.targetFrames = targetFrames;
    }

    @Override
    public void create() {
        frameCount = 0;
    }

    @Override
    public void render() {
        frameCount++;
        if (frameCount >= targetFrames) {
            stopLoop();
        }
    }

    @Override
    public void dispose() {
        // Exact format required by the acceptance criteria.
        System.out.println("FRAME_COUNT: " + frameCount);
        System.out.flush();
    }

    /**
     * Stops the HeadlessApplication main loop synchronously by setting the
     * protected {@code running} field to {@code false} via reflection.
     *
     * This is equivalent to what Gdx.app.exit() eventually does, but without
     * the one-tick delay caused by the runnable queue, ensuring that no extra
     * render() invocation occurs after we have counted exactly targetFrames.
     */
    private void stopLoop() {
        // First, delegate through the official API so any registered
        // LifecycleListeners are properly notified.
        Gdx.app.exit();

        // Then also set the flag directly so the loop checks it immediately
        // after this render() returns, without waiting for the next runnable
        // drain cycle.
        try {
            HeadlessApplication app = (HeadlessApplication) Gdx.app;
            Field runningField = HeadlessApplication.class.getDeclaredField("running");
            runningField.setAccessible(true);
            runningField.set(app, false);
        } catch (Exception e) {
            // If reflection fails we fall back to exit()-only behaviour.
            // The frame count may be off by one in that case.
            System.err.println("Warning: could not set running=false directly: " + e);
        }
    }
}
