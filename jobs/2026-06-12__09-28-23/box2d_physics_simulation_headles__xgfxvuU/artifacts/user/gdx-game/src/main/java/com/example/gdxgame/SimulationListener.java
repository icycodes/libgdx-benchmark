package com.example.gdxgame;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

import java.util.*;

public class SimulationListener extends ApplicationAdapter {

    private final Scenario scenario;
    private World world;
    private Body body;

    // Group impulses by step index
    private final Map<Integer, List<Scenario.Impulse>> impulsesByStep;

    private int currentStep;
    private boolean finished;

    public SimulationListener(Scenario scenario) {
        this.scenario = scenario;
        this.impulsesByStep = new HashMap<>();
        for (Scenario.Impulse imp : scenario.impulses) {
            impulsesByStep.computeIfAbsent(imp.step, k -> new ArrayList<>()).add(imp);
        }
    }

    @Override
    public void create() {
        world = new World(new Vector2(scenario.gravityX, scenario.gravityY), false);

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(scenario.bodyX, scenario.bodyY);
        bodyDef.linearDamping = 0f;
        bodyDef.angularDamping = 0f;

        body = world.createBody(bodyDef);

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
        if (scenario.mass != null) {
            MassData massData = body.getMassData();
            massData.mass = scenario.mass;
            body.setMassData(massData);
        }

        currentStep = 0;
        finished = false;
    }

    @Override
    public void render() {
        if (finished) {
            return;
        }

        if (currentStep >= scenario.steps) {
            // Simulation complete
            Vector2 pos = body.getPosition();
            System.out.printf(Locale.ROOT, "Final position: (%.4f, %.4f)%n", pos.x, pos.y);
            finished = true;
            Gdx.app.exit();
            return;
        }

        // Apply impulses for this step
        List<Scenario.Impulse> stepImpulses = impulsesByStep.get(currentStep);
        if (stepImpulses != null) {
            for (Scenario.Impulse imp : stepImpulses) {
                body.applyLinearImpulse(
                        new Vector2(imp.ix, imp.iy),
                        body.getWorldCenter(),
                        true
                );
            }
        }

        // Advance physics
        world.step(1f / 60f, 6, 2);
        currentStep++;
    }

    @Override
    public void dispose() {
        if (world != null) {
            world.dispose();
        }
    }
}
