package com.example.scene;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

import java.util.concurrent.CountDownLatch;

/**
 * Entry point for the headless scene graph loader.
 *
 * <p>Usage: {@code --scene=<xml_path> --output=<txt_path>}</p>
 *
 * <p>Boots a {@link HeadlessApplication} with {@code updatesPerSecond = 0},
 * processes the scene XML, writes the transcript, and exits cleanly.</p>
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        String scenePath = null;
        String outputPath = null;

        for (String arg : args) {
            if (arg.startsWith("--scene=")) {
                scenePath = arg.substring("--scene=".length());
            } else if (arg.startsWith("--output=")) {
                outputPath = arg.substring("--output=".length());
            }
        }

        if (scenePath == null || outputPath == null) {
            System.err.println("Usage: --scene=<xml_path> --output=<txt_path>");
            System.exit(1);
        }

        CountDownLatch completionLatch = new CountDownLatch(1);

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 0; // run as fast as possible

        SceneLoaderApp appListener = new SceneLoaderApp(scenePath, outputPath, completionLatch);
        new HeadlessApplication(appListener, config);

        // Wait for the application to finish processing before returning
        completionLatch.await();
    }
}