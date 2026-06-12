package com.example.scene;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.XmlReader;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public class Main {

    public static void main(String[] args) {
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
            System.err.println("Usage: --scene=<scene_xml_path> --output=<output_txt_path>");
            System.exit(1);
        }

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 0; // run as fast as possible

        CountDownLatch doneLatch = new CountDownLatch(1);
        SceneLoaderListener listener = new SceneLoaderListener(scenePath, outputPath, doneLatch);
        new HeadlessApplication(listener, config);

        // Wait for the listener to signal completion (called from dispose())
        try {
            doneLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class SceneLoaderListener implements ApplicationListener {

        private final String scenePath;
        private final String outputPath;
        private final CountDownLatch doneLatch;

        SceneLoaderListener(String scenePath, String outputPath, CountDownLatch doneLatch) {
            this.scenePath = scenePath;
            this.outputPath = outputPath;
            this.doneLatch = doneLatch;
        }

        @Override
        public void create() {
            try {
                FileHandle sceneFile = Gdx.files.absolute(scenePath);
                String xmlContent = sceneFile.readString("UTF-8");

                XmlReader xmlReader = new XmlReader();
                XmlReader.Element root = xmlReader.parse(xmlContent);

                // The root element is <scene>, get the single top-level <node>
                XmlReader.Element topNode = root.getChildByName("node");

                StringBuilder transcript = new StringBuilder();
                processNode(topNode, 0, 0, 0, 1, 1, "", transcript);

                FileHandle outputFile = Gdx.files.absolute(outputPath);
                outputFile.writeString(transcript.toString(), false, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
                doneLatch.countDown();
                System.exit(1);
                return;
            }

            Gdx.app.exit();
        }

        private void processNode(
                XmlReader.Element node,
                float parentWorldX,
                float parentWorldY,
                float parentWorldRotation,
                float parentWorldScaleX,
                float parentWorldScaleY,
                String parentPath,
                StringBuilder transcript) {

            String name = node.getAttribute("name");
            float localX = node.getFloatAttribute("x", 0f);
            float localY = node.getFloatAttribute("y", 0f);
            float localRotation = node.getFloatAttribute("rotation", 0f);
            float localScaleX = node.getFloatAttribute("scaleX", 1f);
            float localScaleY = node.getFloatAttribute("scaleY", 1f);

            // Step 1: Scale local offset by parent's world scale
            float lx = localX * parentWorldScaleX;
            float ly = localY * parentWorldScaleY;

            // Step 2: Rotate scaled offset by parent's world rotation (CCW, degrees)
            float cos = MathUtils.cosDeg(parentWorldRotation);
            float sin = MathUtils.sinDeg(parentWorldRotation);
            float rx = lx * cos - ly * sin;
            float ry = lx * sin + ly * cos;

            // Step 3: Translate by parent's world position
            float worldX = parentWorldX + rx;
            float worldY = parentWorldY + ry;

            // Step 4: Compose rotation
            float worldRotation = parentWorldRotation + localRotation;

            // Step 5: Compose scale
            float worldScaleX = parentWorldScaleX * localScaleX;
            float worldScaleY = parentWorldScaleY * localScaleY;

            // Build dotted path
            String dottedPath = parentPath.isEmpty() ? name : parentPath + "." + name;

            // Append line to transcript
            transcript.append(String.format(Locale.ROOT,
                    "%s\tworldX=%.4f\tworldY=%.4f\tworldRotationDeg=%.4f\tworldScaleX=%.4f\tworldScaleY=%.4f\n",
                    dottedPath, worldX, worldY, worldRotation, worldScaleX, worldScaleY));

            // Recurse into children
            for (XmlReader.Element child : node.getChildrenByName("node")) {
                processNode(child, worldX, worldY, worldRotation, worldScaleX, worldScaleY,
                        dottedPath, transcript);
            }
        }

        @Override
        public void resize(int width, int height) {
        }

        @Override
        public void render() {
        }

        @Override
        public void pause() {
        }

        @Override
        public void resume() {
        }

        @Override
        public void dispose() {
            doneLatch.countDown();
        }
    }
}
