package com.myproject;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;

public class FrameCounterListener extends ApplicationAdapter {
    private final int targetFrames;
    private int frameCount;

    public FrameCounterListener(int targetFrames) {
        this.targetFrames = targetFrames;
    }

    @Override
    public void create() {
        this.frameCount = 0;
    }

    @Override
    public void render() {
        if (frameCount >= targetFrames) {
            return;
        }
        frameCount++;
        if (frameCount == targetFrames) {
            Gdx.app.exit();
        }
    }

    @Override
    public void dispose() {
        System.out.println("FRAME_COUNT: " + frameCount);
    }
}
