package com.example.scene;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

public class Main {
    public static void main(String[] args) {
        String scenePath = null;
        String outputPath = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--scene=")) {
                scenePath = arg.substring("--scene=".length());
            } else if (arg.equals("--scene") && i + 1 < args.length) {
                scenePath = args[i + 1];
                i++;
            } else if (arg.startsWith("--output=")) {
                outputPath = arg.substring("--output=".length());
            } else if (arg.equals("--output") && i + 1 < args.length) {
                outputPath = args[i + 1];
                i++;
            }
        }

        if (scenePath == null || outputPath == null) {
            System.err.println("Usage: --scene=<scene_xml_path> --output=<output_txt_path>");
            System.exit(1);
        }

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 0;

        SceneLoaderListener listener = new SceneLoaderListener(scenePath, outputPath);
        HeadlessApplication app = new HeadlessApplication(listener, config);

        // Wait for the main loop thread to join
        try {
            java.lang.reflect.Field field = HeadlessApplication.class.getDeclaredField("mainLoopThread");
            field.setAccessible(true);
            Thread thread = (Thread) field.get(app);
            if (thread != null) {
                thread.join();
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not join HeadlessApplication mainLoopThread: " + e.getMessage());
        }
    }
}
