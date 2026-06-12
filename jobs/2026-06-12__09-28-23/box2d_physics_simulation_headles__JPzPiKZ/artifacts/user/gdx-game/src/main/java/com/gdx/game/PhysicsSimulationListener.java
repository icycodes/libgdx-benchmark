package com.gdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public class PhysicsSimulationListener extends ApplicationAdapter {
    private final Scenario scenario;
    private final CountDownLatch latch;
    
    private World world;
    private Body body;
    private int currentStep = 0;
    private boolean finished = false;

    public PhysicsSimulationListener(Scenario scenario, CountDownLatch latch) {
        this.scenario = scenario;
        this.latch = latch;
    }

    @Override
    public void create() {
        try {
            // Initialize Box2D JNI bindings
            Box2D.init();

            // Create gravity vector
            Vector2 gravity = new Vector2(scenario.gravityX, scenario.gravityY);
            world = new World(gravity, true);

            // Create body definition
            BodyDef bodyDef = new BodyDef();
            bodyDef.type = BodyDef.BodyType.DynamicBody;
            bodyDef.position.set(scenario.bodyX, scenario.bodyY);
            bodyDef.linearDamping = 0f;
            bodyDef.angularDamping = 0f;

            body = world.createBody(bodyDef);

            // Create circular shape of radius 0.5m
            CircleShape circle = new CircleShape();
            circle.setRadius(0.5f);

            // Create fixture definition
            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.shape = circle;
            fixtureDef.density = 1.0f;
            fixtureDef.friction = 0f;
            fixtureDef.restitution = 0f;

            body.createFixture(fixtureDef);
            circle.dispose();

            // Apply mass override if present
            if (scenario.massOverride != null) {
                MassData massData = body.getMassData();
                massData.mass = scenario.massOverride;
                body.setMassData(massData);
            }
        } catch (Throwable t) {
            System.err.println("Error: Failed to initialize simulation: " + t.getMessage());
            System.exit(1);
        }
    }

    @Override
    public void render() {
        if (finished) {
            return;
        }

        try {
            if (currentStep < scenario.steps) {
                // 1. Apply every IMPULSE k in order of appearance
                for (Scenario.Impulse impulse : scenario.impulses) {
                    if (impulse.step == currentStep) {
                        body.applyLinearImpulse(new Vector2(impulse.ix, impulse.iy), body.getWorldCenter(), true);
                    }
                }
                // 2. Step the world
                world.step(1f / 60f, 6, 2);
                currentStep++;
            } else {
                finished = true;
                // Print final position
                Vector2 pos = body.getPosition();
                System.out.printf(Locale.ROOT, "Final position: (%.4f, %.4f)%n", pos.x, pos.y);
                System.out.flush();
                Gdx.app.exit();
            }
        } catch (Throwable t) {
            System.err.println("Error: Failed during simulation step: " + t.getMessage());
            System.exit(1);
        }
    }

    @Override
    public void dispose() {
        if (world != null) {
            world.dispose();
        }
        latch.countDown();
    }
}
