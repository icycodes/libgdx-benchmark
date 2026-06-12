package com.example.gdxgame;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the parsed contents of a scenario file.
 */
public class Scenario {

    public float gravityX;
    public float gravityY;

    public float bodyX;
    public float bodyY;

    /** null means "use auto-computed mass from fixture density" */
    public Float mass = null;

    public int steps;

    /**
     * Each entry is [stepIndex, ix, iy].
     */
    public final List<float[]> impulses = new ArrayList<>();
}
