package com.example.scene;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;

import java.lang.reflect.Field;
import java.util.Locale;

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
        config.updatesPerSecond = 0;

        SceneLoaderApp appListener = new SceneLoaderApp(scenePath, outputPath);
        HeadlessApplication app = new HeadlessApplication(appListener, config);

        try {
            Field field = HeadlessApplication.class.getDeclaredField("mainLoopThread");
            field.setAccessible(true);
            Thread thread = (Thread) field.get(app);
            if (thread != null) {
                thread.join();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class SceneLoaderApp implements ApplicationListener {
    private final String scenePath;
    private final String outputPath;

    public SceneLoaderApp(String scenePath, String outputPath) {
        this.scenePath = scenePath;
        this.outputPath = outputPath;
    }

    @Override
    public void create() {
        try {
            FileHandle sceneFile = Gdx.files.absolute(scenePath);
            XmlReader xmlReader = new XmlReader();
            Element rootElement = xmlReader.parse(sceneFile);

            StringBuilder transcript = new StringBuilder();

            Element firstNode = rootElement.getChildByName("node");
            if (firstNode != null) {
                processNode(firstNode, "", 0, 0, 0, 1, 1, transcript);
            }

            FileHandle outputFile = Gdx.files.absolute(outputPath);
            outputFile.writeString(transcript.toString(), false, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Gdx.app.exit();
        }
    }

    private void processNode(Element node, String parentPath,
                             float parentWorldX, float parentWorldY,
                             float parentWorldRot, float parentWorldScaleX, float parentWorldScaleY,
                             StringBuilder transcript) {
        String name = node.getAttribute("name");
        float localX = node.getFloatAttribute("x", 0f);
        float localY = node.getFloatAttribute("y", 0f);
        float localRot = node.getFloatAttribute("rotation", 0f);
        float localScaleX = node.getFloatAttribute("scaleX", 1f);
        float localScaleY = node.getFloatAttribute("scaleY", 1f);

        float lx = localX * parentWorldScaleX;
        float ly = localY * parentWorldScaleY;

        float cos = (float) Math.cos(Math.toRadians(parentWorldRot));
        float sin = (float) Math.sin(Math.toRadians(parentWorldRot));

        float rx = lx * cos - ly * sin;
        float ry = lx * sin + ly * cos;

        float worldX = parentWorldX + rx;
        float worldY = parentWorldY + ry;
        float worldRot = parentWorldRot + localRot;
        float worldScaleX = parentWorldScaleX * localScaleX;
        float worldScaleY = parentWorldScaleY * localScaleY;

        String currentPath = parentPath.isEmpty() ? name : parentPath + "." + name;

        transcript.append(String.format(Locale.ROOT, "%s\tworldX=%.4f\tworldY=%.4f\tworldRotationDeg=%.4f\tworldScaleX=%.4f\tworldScaleY=%.4f\n",
                currentPath, worldX, worldY, worldRot, worldScaleX, worldScaleY));

        for (int i = 0; i < node.getChildCount(); i++) {
            Element child = node.getChild(i);
            if ("node".equals(child.getName())) {
                processNode(child, currentPath, worldX, worldY, worldRot, worldScaleX, worldScaleY, transcript);
            }
        }
    }

    @Override public void resize(int width, int height) {}
    @Override public void render() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void dispose() {}
}
