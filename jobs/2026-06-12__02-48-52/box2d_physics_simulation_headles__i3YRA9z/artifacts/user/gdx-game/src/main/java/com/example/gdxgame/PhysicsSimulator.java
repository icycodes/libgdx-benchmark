package com.example.gdxgame;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

/**
 * Drives the Box2D physics simulation inside a HeadlessApplication.
 *
 * Lifecycle:
 *   create()  - initialise Box2D world and body.
 *   render()  - called in a tight loop; each call executes the next
 *               physics step until all STEPS have run, then prints the result
 *               and calls Gdx.app.exit().
 *   dispose() - tears down the world and counts down the latch so
 *               the main thread can return.
 */
public class PhysicsSimulator extends ApplicationAdapter {

    private static final float DT = 1f / 60f;
    private static final int VELOCITY_ITERATIONS = 6;
    private static final int POSITION_ITERATIONS = 2;

    // Fixed body shape parameters
    private static final float FIXTURE_RADIUS = 0.5f;
    private static final float FIXTURE_DENSITY = 1.0f;
    private static final float FIXTURE_FRICTION = 0f;
    private static final float FIXTURE_RESTITUTION = 0f;

    private final Scenario scenario;
    private final CountDownLatch latch;

    // Box2D objects
    private World world;
    private Body body;

    // Simulation state
    private int currentStep = 0;
    private boolean done = false;

    // Per-step impulse map: step index -> list of (ix, iy)
    private final List<List<float[]>> impulseMap = new ArrayList<>();

    public PhysicsSimulator(Scenario scenario, CountDownLatch latch) {
        this.scenario = scenario;
        this.latch = latch;
    }

    // -------------------------------------------------------------------------
    // ApplicationAdapter lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void create() {
        // Initialise native Box2D
        Box2D.init();

        // Create world with scripted gravity
        world = new World(new Vector2(scenario.gravityX, scenario.gravityY), true);

        // Build the single dynamic body
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(scenario.bodyX, scenario.bodyY);
        bodyDef.linearDamping = 0f;
        bodyDef.angularDamping = 0f;
        body = world.createBody(bodyDef);

        // Circular fixture
        CircleShape circle = new CircleShape();
        circle.setRadius(FIXTURE_RADIUS);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.density = FIXTURE_DENSITY;
        fixtureDef.friction = FIXTURE_FRICTION;
        fixtureDef.restitution = FIXTURE_RESTITUTION;
        body.createFixture(fixtureDef);
        circle.dispose();

        // Override mass if requested
        if (scenario.mass != null) {
            MassData md = body.getMassData();   // get auto-computed I and center
            md.mass = scenario.mass;
            body.setMassData(md);
        }

        // Build impulse map indexed by step
        for (int i = 0; i < scenario.steps; i++) {
            impulseMap.add(new ArrayList<>());
        }
        for (float[] imp : scenario.impulses) {
            int step = (int) imp[0];
            impulseMap.get(step).add(new float[]{imp[1], imp[2]});
        }
    }

    @Override
    public void render() {
        if (done) {
            return;
        }
        if (currentStep >= scenario.steps) {
            // All steps done - print result and exit
            done = true;
            Vector2 pos = body.getPosition();
            System.out.printf(Locale.ROOT, "Final position: (%.4f, %.4f)%n", pos.x, pos.y);
            Gdx.app.exit();
            return;
        }

        // 1. Apply impulses for this step
        List<float[]> stepImpulses = impulseMap.get(currentStep);
        for (float[] imp : stepImpulses) {
            body.applyLinearImpulse(
                    new Vector2(imp[0], imp[1]),
                    body.getWorldCenter(),
                    true   // wake the body
            );
        }

        // 2. Advance physics
        world.step(DT, VELOCITY_ITERATIONS, POSITION_ITERATIONS);

        currentStep++;
    }

    @Override
    public void dispose() {
        if (world != null) {
            world.dispose();
            world = null;
        }
        // Release the main thread
        latch.countDown();
    }
}
