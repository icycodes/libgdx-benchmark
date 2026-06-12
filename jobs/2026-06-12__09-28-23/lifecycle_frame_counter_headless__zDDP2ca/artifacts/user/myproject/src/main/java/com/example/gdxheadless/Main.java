package com.example.gdxheadless;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

public class Main {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java -jar ... <N>");
            System.exit(1);
        }

        int N;
        try {
            N = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Argument must be an integer: " + args[0]);
            System.exit(1);
            return;
        }

        if (N < 1 || N > 1000) {
            System.err.println("N must be between 1 and 1000");
            System.exit(1);
            return;
        }

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 1000;

        FrameCounterListener listener = new FrameCounterListener(N);
        HeadlessApplication app = new HeadlessApplication(listener, config);

        // The main loop runs on a thread named "HeadlessApplication".
        // The field is protected, so we use reflection to access it.
        try {
            java.lang.reflect.Field field = HeadlessApplication.class.getDeclaredField("mainLoopThread");
            field.setAccessible(true);
            Thread mainLoopThread = (Thread) field.get(app);
            if (mainLoopThread != null) {
                mainLoopThread.join();
            }
        } catch (NoSuchFieldException | IllegalAccessException | InterruptedException e) {
            System.err.println("Error waiting for headless main loop: " + e.getMessage());
            System.exit(1);
        }

        System.exit(0);
    }
}
