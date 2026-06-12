package com.gdxgame;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public class PhysicsSimulation implements ApplicationListener {

    private final Scenario scenario;
    private final CountDownLatch completionLatch;

    private World world;
    private Body body;
    private int currentStep;
    private boolean finished;

    public PhysicsSimulation(Scenario scenario, CountDownLatch completionLatch) {
        this.scenario = scenario;
        this.completionLatch = completionLatch;
        this.currentStep = 0;
        this.finished = false;
    }

    @Override
    public void create() {
        // Create the Box2D world with the scenario's gravity
        world = new World(new Vector2(scenario.gravityX, scenario.gravityY), true);

        // Create the dynamic body at the specified initial position
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(scenario.bodyX, scenario.bodyY);
        bodyDef.linearDamping = 0f;
        bodyDef.angularDamping = 0f;

        body = world.createBody(bodyDef);

        // Create a circular fixture with radius 0.5m, density 1.0
        CircleShape shape = new CircleShape();
        shape.setRadius(0.5f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1.0f;
        fixtureDef.friction = 0f;
        fixtureDef.restitution = 0f;

        body.createFixture(fixtureDef);
        shape.dispose();

        // Override mass if specified
        if (scenario.massOverride != null) {
            MassData massData = body.getMassData();
            massData.mass = scenario.massOverride;
            body.setMassData(massData);
        }
    }

    @Override
    public void render() {
        if (finished) {
            return;
        }

        if (currentStep < scenario.totalSteps) {
            // Apply all impulses for this step, in file order
            for (Scenario.Impulse imp : scenario.impulses) {
                if (imp.step == currentStep) {
                    Vector2 worldCenter = body.getWorldCenter();
                    body.applyLinearImpulse(imp.ix, imp.iy, worldCenter.x, worldCenter.y, true);
                }
            }

            // Advance the physics simulation
            world.step(1f / 60f, 6, 2);
            currentStep++;
        }

        if (currentStep >= scenario.totalSteps) {
            // Print final position
            Vector2 pos = body.getPosition();
            String output = String.format(Locale.ROOT, "Final position: (%.4f, %.4f)", pos.x, pos.y);
            System.out.println(output);
            finished = true;
            Gdx.app.exit();
        }
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void dispose() {
        if (world != null) {
            world.dispose();
        }
        completionLatch.countDown();
    }
}