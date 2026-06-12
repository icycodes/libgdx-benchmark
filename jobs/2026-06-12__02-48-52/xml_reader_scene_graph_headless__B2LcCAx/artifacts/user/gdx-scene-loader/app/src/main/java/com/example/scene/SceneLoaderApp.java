package com.example.scene;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;

import com.badlogic.gdx.utils.Array;
import java.util.Locale;

/**
 * ApplicationListener that:
 * <ol>
 *   <li>Reads the scene XML via {@code Gdx.files.absolute(scenePath)}.</li>
 *   <li>Parses it with {@code com.badlogic.gdx.utils.XmlReader}.</li>
 *   <li>Computes world-space transforms by composing the parent chain.</li>
 *   <li>Writes a pre-order DFS transcript to {@code Gdx.files.absolute(outputPath)}.</li>
 *   <li>Calls {@code Gdx.app.exit()} and notifies the waiting main thread.</li>
 * </ol>
 */
public class SceneLoaderApp extends ApplicationAdapter {

    private final String scenePath;
    private final String outputPath;
    private volatile boolean finished = false;

    public SceneLoaderApp(String scenePath, String outputPath) {
        this.scenePath = scenePath;
        this.outputPath = outputPath;
    }

    // -----------------------------------------------------------------------
    // ApplicationAdapter lifecycle
    // -----------------------------------------------------------------------

    @Override
    public void create() {
        try {
            // 1. Read the XML through Gdx.files so the file backend is exercised.
            FileHandle sceneHandle = Gdx.files.absolute(scenePath);
            String xmlContent = sceneHandle.readString("UTF-8");

            // 2. Parse with libGDX's XmlReader.
            XmlReader reader = new XmlReader();
            Element sceneRoot = reader.parse(xmlContent);

            // <scene> contains exactly one top-level <node>.
            Element rootNodeElement = sceneRoot.getChildByName("node");
            if (rootNodeElement == null) {
                throw new IllegalArgumentException(
                        "No <node> element found directly inside <scene>.");
            }

            // 3. Build transcript via pre-order DFS.
            StringBuilder transcript = new StringBuilder();
            WorldTransform identity = WorldTransform.identity();
            processNode(rootNodeElement, identity, "", transcript);

            // 4. Write output through Gdx.files.
            FileHandle outputHandle = Gdx.files.absolute(outputPath);
            outputHandle.writeString(transcript.toString(), false, "UTF-8");

        } catch (Exception e) {
            System.err.println("SceneLoaderApp error: " + e.getMessage());
            e.printStackTrace(System.err);
        } finally {
            // 5. Signal the main thread and exit the headless loop.
            markFinished();
            Gdx.app.exit();
        }
    }

    // -----------------------------------------------------------------------
    // Recursive DFS traversal
    // -----------------------------------------------------------------------

    /**
     * Processes one {@code <node>} element, appends its line to the transcript,
     * then recurses into its children.
     *
     * @param element    the current XML element
     * @param parentWorld the parent's world transform
     * @param parentPath  dotted path of the parent node (empty string for the root)
     * @param out         the transcript accumulator
     */
    private void processNode(Element element,
                             WorldTransform parentWorld,
                             String parentPath,
                             StringBuilder out) {

        // --- Parse local attributes -----------------------------------------
        String name = element.getAttribute("name");
        float localX        = element.getFloatAttribute("x",        0f);
        float localY        = element.getFloatAttribute("y",        0f);
        float localRotation = element.getFloatAttribute("rotation", 0f);
        float localScaleX   = element.getFloatAttribute("scaleX",   1f);
        float localScaleY   = element.getFloatAttribute("scaleY",   1f);

        // --- Compute world transform (per spec) ------------------------------
        // Step 1: scale the local offset by the parent's world scale.
        float lx = localX * parentWorld.scaleX;
        float ly = localY * parentWorld.scaleY;

        // Step 2: rotate the scaled offset by the parent's world rotation.
        float cosR = MathUtils.cosDeg(parentWorld.rotation);
        float sinR = MathUtils.sinDeg(parentWorld.rotation);
        float rx = lx * cosR - ly * sinR;
        float ry = lx * sinR + ly * cosR;

        // Step 3: translate by the parent's world position.
        float worldX = parentWorld.x + rx;
        float worldY = parentWorld.y + ry;

        // Step 4: compose rotation.
        float worldRotation = parentWorld.rotation + localRotation;

        // Step 5: compose scale (component-wise).
        float worldScaleX = parentWorld.scaleX * localScaleX;
        float worldScaleY = parentWorld.scaleY * localScaleY;

        // --- Build dotted path ----------------------------------------------
        String dottedPath = parentPath.isEmpty() ? name : parentPath + "." + name;

        // --- Append transcript line -----------------------------------------
        out.append(String.format(Locale.ROOT,
                "%s\tworldX=%.4f\tworldY=%.4f\tworldRotationDeg=%.4f\tworldScaleX=%.4f\tworldScaleY=%.4f\n",
                dottedPath, worldX, worldY, worldRotation, worldScaleX, worldScaleY));

        // --- Recurse into children ------------------------------------------
        WorldTransform myWorld = new WorldTransform(worldX, worldY, worldRotation, worldScaleX, worldScaleY);
        Array<Element> children = element.getChildrenByName("node");
        if (children != null) {
            for (Element child : children) {
                processNode(child, myWorld, dottedPath, out);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Synchronization helpers
    // -----------------------------------------------------------------------

    public synchronized boolean isFinished() {
        return finished;
    }

    private synchronized void markFinished() {
        finished = true;
        notifyAll();
    }

    // -----------------------------------------------------------------------
    // Inner value type: world-space transform
    // -----------------------------------------------------------------------

    private static final class WorldTransform {
        final float x, y, rotation, scaleX, scaleY;

        WorldTransform(float x, float y, float rotation, float scaleX, float scaleY) {
            this.x        = x;
            this.y        = y;
            this.rotation = rotation;
            this.scaleX   = scaleX;
            this.scaleY   = scaleY;
        }

        /** Identity transform: position (0,0), rotation 0 deg, scale (1,1). */
        static WorldTransform identity() {
            return new WorldTransform(0f, 0f, 0f, 1f, 1f);
        }
    }
}
