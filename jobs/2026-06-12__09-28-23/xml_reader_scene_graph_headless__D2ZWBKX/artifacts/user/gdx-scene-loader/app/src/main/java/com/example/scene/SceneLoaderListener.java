package com.example.scene;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;

import java.util.Locale;

public class SceneLoaderListener implements ApplicationListener {
    private final String scenePath;
    private final String outputPath;
    private final StringBuilder transcript = new StringBuilder();

    public SceneLoaderListener(String scenePath, String outputPath) {
        this.scenePath = scenePath;
        this.outputPath = outputPath;
    }

    @Override
    public void create() {
        try {
            // 1. Parse XML
            XmlReader reader = new XmlReader();
            Element root = reader.parse(Gdx.files.absolute(scenePath));

            // The root element is <scene> and contains exactly one top-level <node>.
            Element topNode = root.getChildByName("node");
            if (topNode != null) {
                // 2. Traversal & Transform Composition
                // Identity transform for parent of root
                Transform parentTransform = new Transform(0.0, 0.0, 0.0, 1.0, 1.0);
                traverse(topNode, parentTransform, "");
            }

            // 3. Write transcript to output file
            Gdx.files.absolute(outputPath).writeString(transcript.toString(), false, "UTF-8");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error processing scene graph: " + e.getMessage());
            System.exit(1);
        } finally {
            Gdx.app.exit();
        }
    }

    private void traverse(Element node, Transform parent, String parentPath) {
        String name = node.getAttribute("name");
        String dottedPath = parentPath.isEmpty() ? name : parentPath + "." + name;

        double nx = node.getFloatAttribute("x", 0.0f);
        double ny = node.getFloatAttribute("y", 0.0f);
        double nRotation = node.getFloatAttribute("rotation", 0.0f);
        double nScaleX = node.getFloatAttribute("scaleX", 1.0f);
        double nScaleY = node.getFloatAttribute("scaleY", 1.0f);

        // Transform composition rules:
        // 1. Scale local offset by parent's world scale
        double lx = nx * parent.worldScaleX;
        double ly = ny * parent.worldScaleY;

        // 2. Rotate the scaled offset by parent's world rotation
        double rad = Math.toRadians(parent.worldRotation);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        double rx = lx * cos - ly * sin;
        double ry = lx * sin + ly * cos;

        // 3. Translate by parent's world position
        double worldX = parent.worldX + rx;
        double worldY = parent.worldY + ry;

        // 4. Compose rotation
        double worldRotation = parent.worldRotation + nRotation;

        // 5. Compose scale
        double worldScaleX = parent.worldScaleX * nScaleX;
        double worldScaleY = parent.worldScaleY * nScaleY;

        Transform current = new Transform(worldX, worldY, worldRotation, worldScaleX, worldScaleY);

        // Append to transcript
        transcript.append(String.format(Locale.ROOT,
            "%s\tworldX=%.4f\tworldY=%.4f\tworldRotationDeg=%.4f\tworldScaleX=%.4f\tworldScaleY=%.4f\n",
            dottedPath, worldX, worldY, worldRotation, worldScaleX, worldScaleY));

        // Traverse children
        Array<Element> children = node.getChildrenByName("node");
        if (children != null) {
            for (Element child : children) {
                traverse(child, current, dottedPath);
            }
        }
    }

    @Override
    public void resize(int width, int height) {}

    @Override
    public void render() {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {}

    private static class Transform {
        final double worldX;
        final double worldY;
        final double worldRotation;
        final double worldScaleX;
        final double worldScaleY;

        Transform(double worldX, double worldY, double worldRotation, double worldScaleX, double worldScaleY) {
            this.worldX = worldX;
            this.worldY = worldY;
            this.worldRotation = worldRotation;
            this.worldScaleX = worldScaleX;
            this.worldScaleY = worldScaleY;
        }
    }
}
