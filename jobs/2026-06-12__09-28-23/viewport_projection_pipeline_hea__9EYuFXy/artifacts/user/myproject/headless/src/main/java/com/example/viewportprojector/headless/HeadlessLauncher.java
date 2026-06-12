package com.example.viewportprojector.headless;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.example.viewportprojector.ProjectionApp;
import com.example.viewportprojector.Scenario;

import java.util.HashMap;
import java.util.Map;

/** CLI entry point for viewport-projector. */
public final class HeadlessLauncher {
    private static final String SCENARIO_ARG = "--scenario";
    private static final String OUTPUT_ARG = "--output";

    private HeadlessLauncher() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> parsedArgs = parseArgs(args);
        String scenarioPath = requireArg(parsedArgs, SCENARIO_ARG);
        String outputPath = requireArg(parsedArgs, OUTPUT_ARG);

        Scenario scenario = Scenario.load(scenarioPath);
        ProjectionApp app = new ProjectionApp(scenario, outputPath);

        HeadlessApplicationConfiguration configuration = new HeadlessApplicationConfiguration();
        configuration.updatesPerSecond = 60;

        new HeadlessApplication(app, configuration);
        Thread headlessThread = findHeadlessApplicationThread();

        app.awaitCompletion();
        if (headlessThread != null && headlessThread != Thread.currentThread()) {
            headlessThread.join();
        }

        if (app.getFailure() != null) {
            throw new RuntimeException("viewport-projector failed", app.getFailure());
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> parsed = new HashMap<>();
        for (String arg : args) {
            int separator = arg.indexOf('=');
            if (separator <= 0) {
                throw new IllegalArgumentException("Expected arguments in --name=value form, got: " + arg);
            }
            parsed.put(arg.substring(0, separator), arg.substring(separator + 1));
        }
        return parsed;
    }

    private static String requireArg(Map<String, String> args, String name) {
        String value = args.get(name);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Missing required argument: " + name + "=<path>");
        }
        return value;
    }

    private static Thread findHeadlessApplicationThread() {
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if ("HeadlessApplication".equals(thread.getName())) {
                return thread;
            }
        }
        return null;
    }
}
