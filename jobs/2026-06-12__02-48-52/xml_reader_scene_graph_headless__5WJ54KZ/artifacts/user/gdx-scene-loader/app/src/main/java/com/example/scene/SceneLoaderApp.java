package com.example.scene;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.XmlReader;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;

/**
 * A headless libGDX {@link ApplicationListener} that loads an XML scene graph,
 * computes world-space transforms for every node, and writes a depth-first
 * transcript to an output file.
 */
public class SceneLoaderApp implements ApplicationListener {

    private final String sceneXmlPath;
    private final String outputTxtPath;
    private final CountDownLatch completionLatch;

    public SceneLoaderApp(String sceneXmlPath, String outputTxtPath, CountDownLatch completionLatch) {
        this.sceneXmlPath = sceneXmlPath;
        this.outputTxtPath = outputTxtPath;
        this.completionLatch = completionLatch;
    }

    @Override
    public void create() {
        try {
            // Read the scene XML through libGDX file handle
            FileHandle sceneFile = Gdx.files.absolute(sceneXmlPath);
            String xml = sceneFile.readString("UTF-8");

            // Parse with libGDX XmlReader
            XmlReader xmlReader = new XmlReader();
            XmlReader.Element sceneElement = xmlReader.parse(xml);

            // Get the root node (first <node> child of <scene>)
            XmlReader.Element rootNode = sceneElement.getChildByName("node");

            // Build transcript with pre-order DFS traversal
            StringBuilder transcript = new StringBuilder();
            traverseNode(rootNode, null, transcript);

            // Write output file through libGDX file handle
            FileHandle outputFile = Gdx.files.absolute(outputTxtPath);
            outputFile.writeString(transcript.toString(), false, "UTF-8");

        } catch (Exception e) {
            throw new RuntimeException("Failed to process scene", e);
        } finally {
            // Signal completion and exit cleanly
            completionLatch.countDown();
            Gdx.app.exit();
        }
    }

    /**
     * Recursively traverse a scene node in pre-order, computing its world
     * transform and appending a line to the transcript.
     *
     * @param element      The XML element for this node
     * @param parentWorld  The parent's world transform (null for the root node)
     * @param transcript   The StringBuilder collecting output lines
     */
    private void traverseNode(XmlReader.Element element, WorldTransform parentWorld, StringBuilder transcript) {
        // Parse local transform attributes (defaults: x=0, y=0, rotation=0, scaleX=1, scaleY=1)
        String name = element.getAttribute("name");
        float localX = element.getFloatAttribute("x", 0f);
        float localY = element.getFloatAttribute("y", 0f);
        float localRotation = element.getFloatAttribute("rotation", 0f);
        float localScaleX = element.getFloatAttribute("scaleX", 1f);
        float localScaleY = element.getFloatAttribute("scaleY", 1f);

        // Compute world transform
        WorldTransform worldTransform;
        if (parentWorld == null) {
            // Root node: parent is identity
            worldTransform = new WorldTransform(localX, localY, localRotation, localScaleX, localScaleY);
        } else {
            // Step 1: Scale local offset by parent's world scale
            float lx = localX * parentWorld.scaleX;
            float ly = localY * parentWorld.scaleY;

            // Step 2: Rotate scaled offset by parent's world rotation (degrees, counter-clockwise)
            double rotRad = Math.toRadians(parentWorld.rotation);
            double cosDeg = Math.cos(rotRad);
            double sinDeg = Math.sin(rotRad);
            float rx = (float)(lx * cosDeg - ly * sinDeg);
            float ry = (float)(lx * sinDeg + ly * cosDeg);

            // Step 3: Translate by parent's world position
            float worldX = parentWorld.x + rx;
            float worldY = parentWorld.y + ry;

            // Step 4: Compose rotation
            float worldRotation = parentWorld.rotation + localRotation;

            // Step 5: Compose scale (component-wise)
            float worldScaleX = parentWorld.scaleX * localScaleX;
            float worldScaleY = parentWorld.scaleY * localScaleY;

            worldTransform = new WorldTransform(worldX, worldY, worldRotation, worldScaleX, worldScaleY);
        }

        // Build the dotted path for this node
        String dottedPath;
        if (parentWorld == null) {
            dottedPath = name;
        } else {
            dottedPath = parentWorld.path + "." + name;
        }
        worldTransform.path = dottedPath;

        // Append this node's line to the transcript
        String line = String.format(Locale.ROOT,
                "%s\tworldX=%.4f\tworldY=%.4f\tworldRotationDeg=%.4f\tworldScaleX=%.4f\tworldScaleY=%.4f",
                dottedPath,
                worldTransform.x,
                worldTransform.y,
                worldTransform.rotation,
                worldTransform.scaleX,
                worldTransform.scaleY);
        transcript.append(line).append('\n');

        // Recurse over children in document order
        for (int i = 0; i < element.getChildCount(); i++) {
            XmlReader.Element child = element.getChild(i);
            if ("node".equals(child.getName())) {
                traverseNode(child, worldTransform, transcript);
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        // No-op
    }

    @Override
    public void render() {
        // No-op; all work is done in create()
    }

    @Override
    public void pause() {
        // No-op
    }

    @Override
    public void resume() {
        // No-op
    }

    @Override
    public void dispose() {
        // No-op
    }

    /**
     * Simple struct holding a node's world-space transform and dotted path.
     */
    private static class WorldTransform {
        final float x;
        final float y;
        final float rotation;
        final float scaleX;
        final float scaleY;
        String path; // set after construction for path building

        WorldTransform(float x, float y, float rotation, float scaleX, float scaleY) {
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
        }
    }
}