package com.example;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Please provide N");
            System.exit(1);
        }
        int targetFrames = Integer.parseInt(args[0]);

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 1000;

        MyHeadlessApp app = new MyHeadlessApp(targetFrames);
        HeadlessApplication headlessApp = new HeadlessApplication(app, config);

        Thread headlessThread = null;
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if ("HeadlessApplication".equals(t.getName())) {
                headlessThread = t;
                break;
            }
        }

        if (headlessThread != null) {
            try {
                headlessThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Could not find HeadlessApplication thread.");
            System.exit(1);
        }
    }
}
