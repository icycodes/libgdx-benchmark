package com.example.scene;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;

import java.util.Locale;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Arguments arguments = Arguments.parse(args);

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 0;

        SceneLoaderApplication listener = new SceneLoaderApplication(arguments.scenePath, arguments.outputPath);
        JoinableHeadlessApplication application = new JoinableHeadlessApplication(listener, config);
        application.joinMainLoop();

        if (listener.failure != null) {
            if (listener.failure instanceof RuntimeException) {
                throw (RuntimeException) listener.failure;
            }
            throw new RuntimeException(listener.failure);
        }
    }

    private static final class JoinableHeadlessApplication extends HeadlessApplication {
        private JoinableHeadlessApplication(ApplicationListener listener, HeadlessApplicationConfiguration config) {
            super(listener, config);
        }

        private void joinMainLoop() throws InterruptedException {
            Thread thread = mainLoopThread;
            if (thread != null) {
                thread.join();
            }
        }
    }

    private static final class SceneLoaderApplication extends ApplicationAdapter {
        private final String scenePath;
        private final String outputPath;
        private Throwable failure;

        private SceneLoaderApplication(String scenePath, String outputPath) {
            this.scenePath = scenePath;
            this.outputPath = outputPath;
        }

        @Override
        public void create() {
            try {
                String transcript = loadTranscript();
                Gdx.files.absolute(outputPath).writeString(transcript, false, "UTF-8");
            } catch (Throwable throwable) {
                failure = throwable;
            } finally {
                Gdx.app.exit();
            }
        }

        private String loadTranscript() {
            FileHandle sceneFile = Gdx.files.absolute(scenePath);
            XmlReader.Element scene = new XmlReader().parse(sceneFile);
            if (!"scene".equals(scene.getName())) {
                throw new IllegalArgumentException("Root element must be <scene>.");
            }

            Array<XmlReader.Element> topLevelNodes = scene.getChildrenByName("node");
            if (scene.getChildCount() != 1 || topLevelNodes.size != 1) {
                throw new IllegalArgumentException("<scene> must contain exactly one top-level <node>.");
            }

            StringBuilder transcript = new StringBuilder();
            appendNode(transcript, topLevelNodes.first(), Transform.IDENTITY, "");
            return transcript.toString();
        }

        private void appendNode(StringBuilder transcript, XmlReader.Element node, Transform parentWorld, String parentPath) {
            String name = node.getAttribute("name", null);
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Every <node> must have a non-empty name attribute.");
            }
            if (!name.matches("[A-Za-z0-9_]+")) {
                throw new IllegalArgumentException("Invalid node name: " + name);
            }

            Transform local = new Transform(
                node.getFloatAttribute("x", 0f),
                node.getFloatAttribute("y", 0f),
                node.getFloatAttribute("rotation", 0f),
                node.getFloatAttribute("scaleX", 1f),
                node.getFloatAttribute("scaleY", 1f)
            );
            Transform world = local.toWorld(parentWorld);
            String path = parentPath.isEmpty() ? name : parentPath + "." + name;

            transcript.append(String.format(
                Locale.ROOT,
                "%s\tworldX=%.4f\tworldY=%.4f\tworldRotationDeg=%.4f\tworldScaleX=%.4f\tworldScaleY=%.4f\n",
                path,
                world.x,
                world.y,
                world.rotationDeg,
                world.scaleX,
                world.scaleY
            ));

            Array<XmlReader.Element> children = node.getChildrenByName("node");
            for (XmlReader.Element child : children) {
                appendNode(transcript, child, world, path);
            }
        }
    }

    private static final class Transform {
        private static final Transform IDENTITY = new Transform(0.0, 0.0, 0.0, 1.0, 1.0);

        private final double x;
        private final double y;
        private final double rotationDeg;
        private final double scaleX;
        private final double scaleY;

        private Transform(double x, double y, double rotationDeg, double scaleX, double scaleY) {
            this.x = x;
            this.y = y;
            this.rotationDeg = rotationDeg;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
        }

        private Transform toWorld(Transform parentWorld) {
            double localScaledX = x * parentWorld.scaleX;
            double localScaledY = y * parentWorld.scaleY;
            double radians = Math.toRadians(parentWorld.rotationDeg);
            double cos = Math.cos(radians);
            double sin = Math.sin(radians);
            double rotatedX = localScaledX * cos - localScaledY * sin;
            double rotatedY = localScaledX * sin + localScaledY * cos;

            return new Transform(
                parentWorld.x + rotatedX,
                parentWorld.y + rotatedY,
                parentWorld.rotationDeg + rotationDeg,
                parentWorld.scaleX * scaleX,
                parentWorld.scaleY * scaleY
            );
        }
    }

    private static final class Arguments {
        private final String scenePath;
        private final String outputPath;

        private Arguments(String scenePath, String outputPath) {
            this.scenePath = scenePath;
            this.outputPath = outputPath;
        }

        private static Arguments parse(String[] args) {
            String scenePath = null;
            String outputPath = null;

            for (String arg : args) {
                if (arg.startsWith("--scene=")) {
                    scenePath = arg.substring("--scene=".length());
                } else if (arg.startsWith("--output=")) {
                    outputPath = arg.substring("--output=".length());
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }

            if (scenePath == null || scenePath.isEmpty()) {
                throw new IllegalArgumentException("Missing required argument --scene=<scene_xml_path>.");
            }
            if (outputPath == null || outputPath.isEmpty()) {
                throw new IllegalArgumentException("Missing required argument --output=<output_txt_path>.");
            }
            return new Arguments(scenePath, outputPath);
        }
    }
}
