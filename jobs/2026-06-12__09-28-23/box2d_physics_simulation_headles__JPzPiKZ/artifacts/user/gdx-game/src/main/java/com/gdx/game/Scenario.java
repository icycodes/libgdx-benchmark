package com.gdx.game;

import java.util.ArrayList;
import java.util.List;

public class Scenario {
    public float gravityX;
    public float gravityY;
    public float bodyX;
    public float bodyY;
    public Float massOverride = null; // null if not present
    public int steps;
    public List<Impulse> impulses = new ArrayList<>();

    public static class Impulse {
        public int step;
        public float ix;
        public float iy;

        public Impulse(int step, float ix, float iy) {
            this.step = step;
            this.ix = ix;
            this.iy = iy;
        }
    }
}
