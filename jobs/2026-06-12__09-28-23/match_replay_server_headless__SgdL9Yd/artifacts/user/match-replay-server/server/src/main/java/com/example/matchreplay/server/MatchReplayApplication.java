package com.example.matchreplay.server;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.example.matchreplay.core.MatchLog;
import com.example.matchreplay.core.MatchLogParser;
import com.example.matchreplay.core.ReplayRunner;
import com.example.matchreplay.core.Transcript;

import java.util.concurrent.CountDownLatch;

public final class MatchReplayApplication extends ApplicationAdapter {
    private final String inputLogPath;
    private final CountDownLatch commandInputReady = new CountDownLatch(1);
    private ServerCommandInput commandInput;
    private MatchLog matchLog;
    private ReplayRunner replayRunner;
    private Transcript transcript;
    private RuntimeException failure;

    public MatchReplayApplication(String inputLogPath) {
        this.inputLogPath = inputLogPath;
    }

    public void setCommandInput(ServerCommandInput commandInput) {
        this.commandInput = commandInput;
        commandInputReady.countDown();
    }

    @Override
    public void create() {
        try {
            commandInputReady.await();
            matchLog = MatchLogParser.parse(Gdx.files.absolute(inputLogPath));
            replayRunner = new ReplayRunner(matchLog);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            fail(new RuntimeException("Interrupted while waiting for replay input", exception));
        } catch (RuntimeException exception) {
            fail(exception);
        }
    }

    @Override
    public void render() {
        if (failure != null || replayRunner == null) {
            Gdx.app.exit();
            return;
        }

        if (replayRunner.hasRemainingTicks()) {
            int tick = replayRunner.getNextTick();
            commandInput.setCurrentTickCommands(matchLog.getCommandsForTick(tick));
            replayRunner.applyNextTick(commandInput.getCurrentTickCommands());
        }

        if (!replayRunner.hasRemainingTicks()) {
            transcript = replayRunner.transcript();
            Gdx.app.exit();
        }
    }

    public Transcript getTranscript() {
        if (failure != null) {
            throw failure;
        }
        if (transcript == null) {
            throw new IllegalStateException("Replay did not produce a transcript");
        }
        return transcript;
    }

    private void fail(RuntimeException exception) {
        failure = exception;
        Gdx.app.exit();
    }
}
