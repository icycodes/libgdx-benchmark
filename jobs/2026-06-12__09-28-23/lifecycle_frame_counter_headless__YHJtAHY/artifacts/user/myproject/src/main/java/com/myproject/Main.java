package com.myproject;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Error: Missing argument N");
            System.exit(1);
        }

        int n;
        try {
            n = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Error: Argument N must be an integer");
            System.exit(1);
            return;
        }

        if (n < 1) {
            System.err.println("Error: Argument N must be >= 1");
            System.exit(1);
            return;
        }

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 1000; // deterministic tick rate, 1000 ticks per second

        FrameCounterListener listener = new FrameCounterListener(n);
        HeadlessApplication app = new HeadlessApplication(listener, config);

        // Wait for the headless main loop thread to finish
        try {
            java.lang.reflect.Field threadField = HeadlessApplication.class.getDeclaredField("mainLoopThread");
            threadField.setAccessible(true);
            Thread thread = (Thread) threadField.get(app);
            if (thread != null) {
                thread.join();
                System.exit(0);
                return;
            }
        } catch (Throwable t) {
            // Fallback to thread-name-based search
        }

        // Fallback: search by thread name with a timeout of 2 seconds
        long startTime = System.currentTimeMillis();
        boolean joined = false;
        while (!joined && (System.currentTimeMillis() - startTime < 2000)) {
            Thread[] threads = new Thread[Thread.activeCount() * 2];
            int count = Thread.enumerate(threads);
            Thread headlessThread = null;
            for (int i = 0; i < count; i++) {
                if (threads[i] != null && "HeadlessApplication".equals(threads[i].getName())) {
                    headlessThread = threads[i];
                    break;
                }
            }
            if (headlessThread != null) {
                try {
                    headlessThread.join();
                    joined = true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } else {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Exit with code 0
        System.exit(0);
    }
}
