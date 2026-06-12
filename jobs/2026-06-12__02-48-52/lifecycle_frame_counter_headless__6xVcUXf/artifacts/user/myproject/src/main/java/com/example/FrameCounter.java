package com.example;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;

public class FrameCounter extends ApplicationAdapter {

    private int targetFrames;
    private int frameCount;
    private boolean exited;

    public FrameCounter(int targetFrames) {
        this.targetFrames = targetFrames;
    }

    @Override
    public void create() {
        frameCount = 0;
        exited = false;
    }

    @Override
    public void render() {
        if (exited) {
            return;
        }
        frameCount++;
        if (frameCount >= targetFrames) {
            exited = true;
            Gdx.app.exit();
        }
    }

    @Override
    public void dispose() {
        System.out.println("FRAME_COUNT: " + frameCount);
    }
}