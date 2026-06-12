package com.gdx.game;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Launcher {
    public static void main(String[] args) {
        String configPath = null;
        for (String arg : args) {
            if (arg.startsWith("--config=")) {
                configPath = arg.substring("--config=".length());
            }
        }

        if (configPath == null || configPath.isEmpty()) {
            System.err.println("Error: invalid config: missing or empty --config=<path> argument");
            System.exit(1);
        }

        final String path = configPath;
        final AtomicBoolean success = new AtomicBoolean(false);
        final AtomicReference<String> errorMessage = new AtomicReference<>(null);
        final CountDownLatch latch = new CountDownLatch(1);

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();

        ConfigApp app = new ConfigApp(path, success, errorMessage, latch);
        HeadlessApplication headlessApp = null;
        try {
            headlessApp = new HeadlessApplication(app, config);
        } catch (Exception e) {
            System.err.println("Error: invalid config: failed to start headless application: " + e.getMessage());
            System.exit(1);
        }

        // Wait for the application's create() to complete
        try {
            boolean completed = latch.await(15, TimeUnit.SECONDS);
            if (!completed) {
                errorMessage.compareAndSet(null, "timeout waiting for application initialization");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorMessage.compareAndSet(null, "interrupted waiting for application initialization");
        }

        // Join the main loop thread to ensure everything is flushed and terminated
        if (headlessApp != null) {
            try {
                java.lang.reflect.Field field = HeadlessApplication.class.getDeclaredField("mainLoopThread");
                field.setAccessible(true);
                Thread mainLoopThread = (Thread) field.get(headlessApp);
                if (mainLoopThread != null) {
                    mainLoopThread.join(5000); // wait up to 5 seconds
                }
            } catch (Exception e) {
                // Ignore reflection exceptions
            }
        }

        if (success.get()) {
            System.exit(0);
        } else {
            String err = errorMessage.get();
            if (err == null) {
                err = "unknown error";
            }
            System.err.println("Error: invalid config: " + err);
            System.exit(1);
        }
    }
}
