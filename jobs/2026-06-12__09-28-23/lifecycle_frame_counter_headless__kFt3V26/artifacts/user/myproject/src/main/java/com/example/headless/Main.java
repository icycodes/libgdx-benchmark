package com.example.headless;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

public final class Main {
    private static final int UPDATES_PER_SECOND = 1000;
    private static final String HEADLESS_THREAD_NAME = "HeadlessApplication";

    private Main() {
    }

    public static void main(String[] args) throws InterruptedException {
        int targetFrameCount = parseTargetFrameCount(args);

        HeadlessApplicationConfiguration configuration = new HeadlessApplicationConfiguration();
        configuration.updatesPerSecond = UPDATES_PER_SECOND;

        new HeadlessApplication(new FrameCounterApplication(targetFrameCount), configuration);

        Thread headlessThread = findHeadlessApplicationThread();
        if (headlessThread != null) {
            headlessThread.join();
        }
    }

    private static int parseTargetFrameCount(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected exactly one integer argument: N");
        }

        int targetFrameCount;
        try {
            targetFrameCount = Integer.parseInt(args[0]);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("N must be an integer", exception);
        }

        if (targetFrameCount < 1 || targetFrameCount > 1000) {
            throw new IllegalArgumentException("N must be between 1 and 1000");
        }
        return targetFrameCount;
    }

    private static Thread findHeadlessApplicationThread() throws InterruptedException {
        long deadlineNanos = System.nanoTime() + 5_000_000_000L;
        Thread currentThread = Thread.currentThread();

        while (System.nanoTime() < deadlineNanos) {
            for (Thread thread : Thread.getAllStackTraces().keySet()) {
                if (thread != currentThread && HEADLESS_THREAD_NAME.equals(thread.getName())) {
                    return thread;
                }
            }
            Thread.sleep(1L);
        }

        return null;
    }
}
