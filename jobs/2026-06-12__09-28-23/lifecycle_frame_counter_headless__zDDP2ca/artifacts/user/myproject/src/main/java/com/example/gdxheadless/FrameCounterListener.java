package com.example.gdxheadless;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;

public class FrameCounterListener implements ApplicationListener {

    private final int targetFrames;
    private int frameCount;
    private boolean exiting;

    public FrameCounterListener(int targetFrames) {
        this.targetFrames = targetFrames;
    }

    @Override
    public void create() {
        frameCount = 0;
        exiting = false;
    }

    @Override
    public void render() {
        if (exiting) {
            return;
        }
        frameCount++;
        if (frameCount >= targetFrames) {
            Gdx.app.exit();
            exiting = true;
        }
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        System.out.println("FRAME_COUNT: " + frameCount);
    }
}
