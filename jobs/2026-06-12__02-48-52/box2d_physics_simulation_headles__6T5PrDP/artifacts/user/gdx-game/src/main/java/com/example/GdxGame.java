package com.example;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.MassData;
import com.badlogic.gdx.physics.box2d.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public class GdxGame extends ApplicationAdapter {
    private final Scenario scenario;
    private final CountDownLatch latch;
    private World world;
    private Body body;
    private int currentStep = 0;
    private boolean finished = false;

    public GdxGame(Scenario scenario, CountDownLatch latch) {
        this.scenario = scenario;
        this.latch = latch;
    }

    @Override
    public void create() {
        world = new World(new Vector2(scenario.gravityX, scenario.gravityY), true);

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(scenario.bodyX, scenario.bodyY);
        bodyDef.linearDamping = 0f;
        bodyDef.angularDamping = 0f;

        body = world.createBody(bodyDef);

        CircleShape circle = new CircleShape();
        circle.setRadius(0.5f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.density = 1.0f;
        fixtureDef.friction = 0f;
        fixtureDef.restitution = 0f;

        body.createFixture(fixtureDef);
        circle.dispose();

        if (scenario.hasMass) {
            MassData massData = body.getMassData();
            massData.mass = scenario.mass;
            body.setMassData(massData);
        }
    }

    @Override
    public void render() {
        if (currentStep < scenario.steps) {
            // Apply impulses for current step
            for (Scenario.Impulse imp : scenario.impulses) {
                if (imp.step == currentStep) {
                    body.applyLinearImpulse(new Vector2(imp.ix, imp.iy), body.getWorldCenter(), true);
                }
            }

            world.step(1f / 60f, 6, 2);
            currentStep++;
        }

        if (currentStep >= scenario.steps && !finished) {
            finished = true;
            Vector2 pos = body.getPosition();
            System.out.printf(Locale.ROOT, "Final position: (%.4f, %.4f)\n", pos.x, pos.y);
            Gdx.app.exit();
        }
    }

    @Override
    public void dispose() {
        if (world != null) {
            world.dispose();
        }
        latch.countDown();
    }

    public static void main(String[] args) {
        String scenarioPath = null;
        for (String arg : args) {
            if (arg.startsWith("--scenario=")) {
                scenarioPath = arg.substring("--scenario=".length());
            }
        }

        if (scenarioPath == null) {
            System.err.println("Error: Missing --scenario=<file> argument");
            System.exit(1);
        }

        Scenario scenario;
        try {
            scenario = Scenario.parse(scenarioPath);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
            return;
        }

        com.badlogic.gdx.physics.box2d.Box2D.init();

        CountDownLatch latch = new CountDownLatch(1);
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 0; // run as fast as possible

        new HeadlessApplication(new GdxGame(scenario, latch), config);

        try {
            latch.await();
        } catch (InterruptedException e) {
            System.err.println("Error: interrupted");
            System.exit(1);
        }
        System.exit(0);
    }
}
