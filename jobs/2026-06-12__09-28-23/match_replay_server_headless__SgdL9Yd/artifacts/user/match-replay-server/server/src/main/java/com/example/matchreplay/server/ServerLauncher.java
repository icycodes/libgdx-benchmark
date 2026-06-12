package com.example.matchreplay.server;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ServerLauncher {
    private ServerLauncher() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: server <input_log_path> <output_transcript_path>");
        }

        MatchReplayApplication listener = new MatchReplayApplication(args[0]);
        HeadlessApplicationConfiguration configuration = new HeadlessApplicationConfiguration();
        configuration.updatesPerSecond = 240;

        HeadlessApplication application = new HeadlessApplication(listener, configuration);
        ServerCommandInput commandInput = new ServerCommandInput();
        Gdx.input = commandInput;
        listener.setCommandInput(commandInput);

        joinMainLoopThread(application);

        Path outputPath = Path.of(args[1]);
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(outputPath, listener.getTranscript().toJson(), StandardCharsets.UTF_8);
    }

    private static void joinMainLoopThread(HeadlessApplication application) throws Exception {
        Field mainLoopThreadField = HeadlessApplication.class.getDeclaredField("mainLoopThread");
        mainLoopThreadField.setAccessible(true);
        Thread mainLoopThread = (Thread) mainLoopThreadField.get(application);
        if (mainLoopThread != null) {
            mainLoopThread.join();
        }
    }
}
