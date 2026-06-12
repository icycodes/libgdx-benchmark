package com.example.gdxgame;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

public final class HeadlessConfigLauncher {
    private HeadlessConfigLauncher() {
    }

    public static void main(String[] args) throws InterruptedException {
        String configPath;
        try {
            configPath = parseConfigPath(args);
        } catch (InvalidConfigException e) {
            System.err.println("Error: invalid config: " + e.getMessage());
            System.err.flush();
            System.exit(1);
            return;
        }

        ConfigSummaryApplication listener = new ConfigSummaryApplication(configPath);
        HeadlessApplicationConfiguration configuration = new HeadlessApplicationConfiguration();
        configuration.updatesPerSecond = 60;

        JoinableHeadlessApplication application = new JoinableHeadlessApplication(listener, configuration);
        application.joinMainLoop();

        System.out.flush();
        System.err.flush();
        if (!listener.isSuccessful()) {
            System.exit(1);
        }
    }

    private static String parseConfigPath(String[] args) throws InvalidConfigException {
        if (args.length != 1) {
            throw new InvalidConfigException("expected exactly one --config=<file> argument");
        }

        String arg = args[0];
        String prefix = "--config=";
        if (!arg.startsWith(prefix) || arg.length() == prefix.length()) {
            throw new InvalidConfigException("expected --config=<file> argument");
        }

        return arg.substring(prefix.length());
    }

    private static final class JoinableHeadlessApplication extends HeadlessApplication {
        private JoinableHeadlessApplication(
            ApplicationListener listener,
            HeadlessApplicationConfiguration configuration
        ) {
            super(listener, configuration);
        }

        private void joinMainLoop() throws InterruptedException {
            mainLoopThread.join();
        }
    }
}
