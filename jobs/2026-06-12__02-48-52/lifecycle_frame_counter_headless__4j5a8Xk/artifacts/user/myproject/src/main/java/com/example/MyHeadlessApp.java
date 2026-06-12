package com.example;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;

public class MyHeadlessApp extends ApplicationAdapter {
    private final int targetFrames;
    private int frames = 0;

    public MyHeadlessApp(int targetFrames) {
        this.targetFrames = targetFrames;
    }

    @Override
    public void create() {
        frames = 0;
    }

    @Override
    public void render() {
        if (frames >= targetFrames) {
            return;
        }
        frames++;
        if (frames >= targetFrames) {
            Gdx.app.exit();
        }
    }

    @Override
    public void dispose() {
        System.out.println("FRAME_COUNT: " + frames);
    }
}
