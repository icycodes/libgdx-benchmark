package com.example.gdxgame;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

public final class Launcher {

    public static void main(String[] args) {
        String configPath = null;
        for (String arg : args) {
            if (arg.startsWith("--config=")) {
                configPath = arg.substring("--config=".length());
                break;
            }
        }

        if (configPath == null) {
            System.err.println("Error: invalid config: missing --config=<path> argument");
            System.exit(1);
        }

        HeadlessApplicationConfiguration appConfig = new HeadlessApplicationConfiguration();
        // Set updatesPerSecond to a negative value so render() is never called,
        // and the main loop exits immediately after create() -> exit().
        appConfig.updatesPerSecond = -1;

        ConfigListener listener = new ConfigListener(configPath);

        HeadlessApplication app = new HeadlessApplication(listener, appConfig);

        // HeadlessApplication.mainLoopThread is protected, so we subclass to expose it.
        // We use an anonymous subclass that gives us access to the thread.
        // Actually, we can just poll until the app is no longer running, or use
        // a simple approach: the mainLoopThread is started in the constructor,
        // so we can retrieve it via reflection or by subclassing.
        // The simplest approach: use a CountDownLatch in the listener.
        listener.awaitExit();

        if (listener.hasError()) {
            System.exit(1);
        } else {
            System.exit(0);
        }
    }
}
