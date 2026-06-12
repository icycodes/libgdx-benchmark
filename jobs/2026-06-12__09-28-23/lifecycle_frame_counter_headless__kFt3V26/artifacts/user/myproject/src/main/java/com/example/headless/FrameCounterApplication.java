package com.example.headless;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;

public final class FrameCounterApplication extends ApplicationAdapter {
    private final int targetFrameCount;
    private int frameCount;

    public FrameCounterApplication(int targetFrameCount) {
        if (targetFrameCount < 1) {
            throw new IllegalArgumentException("targetFrameCount must be at least 1");
        }
        this.targetFrameCount = targetFrameCount;
    }

    @Override
    public void create() {
        frameCount = 0;
    }

    @Override
    public void render() {
        frameCount++;
        if (frameCount >= targetFrameCount) {
            Gdx.app.exit();

            // The headless backend implements exit() by posting a runnable that is
            // normally processed at the start of the next loop iteration. Execute
            // it now so the current render is the final counted frame.
            if (Gdx.app instanceof HeadlessApplication) {
                ((HeadlessApplication) Gdx.app).executeRunnables();
            }
        }
    }

    @Override
    public void dispose() {
        System.out.println("FRAME_COUNT: " + frameCount);
    }
}
