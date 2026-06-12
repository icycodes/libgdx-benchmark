package com.matchreplay.server;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.matchreplay.core.MatchLog;
import com.matchreplay.core.Simulation;
import com.matchreplay.core.TranscriptWriter;

import java.util.logging.Logger;

/**
 * Main entry point for the headless match-replay server.
 *
 * <p>Usage:
 * <pre>
 *   java -cp ... com.matchreplay.server.ServerApp &lt;input_log_path&gt; &lt;output_transcript_path&gt;
 * </pre>
 *
 * <p>Steps:
 * <ol>
 *   <li>Parse the match log from {@code input_log_path}.</li>
 *   <li>Construct a {@link HeadlessApplication} with a {@link ReplayListener}
 *       that drives the simulation one tick per {@code render()} call.</li>
 *   <li>Replace {@code Gdx.input} with a {@link ReplayMockInput} instance.</li>
 *   <li>Await the {@link ReplayListener#doneLatch} to ensure all ticks are
 *       finished and the headless main-loop thread has exited.</li>
 *   <li>Write the JSON transcript to {@code output_transcript_path}.</li>
 * </ol>
 */
public class ServerApp {

    private static final Logger LOG = Logger.getLogger(ServerApp.class.getName());

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: server <input_log_path> <output_transcript_path>");
            System.exit(1);
        }

        String inputLogPath      = args[0];
        String outputTranscriptPath = args[1];

        // ------------------------------------------------------------------
        // 1. Parse the match log.
        // ------------------------------------------------------------------
        LOG.info("Parsing match log: " + inputLogPath);
        MatchLog matchLog = MatchLog.parse(inputLogPath);
        LOG.info("Parsed: world=" + matchLog.worldWidth + "x" + matchLog.worldHeight
                + ", players=" + matchLog.players.size()
                + ", totalTicks=" + matchLog.totalTicks
                + ", commands=" + matchLog.commands.size());

        // ------------------------------------------------------------------
        // 2. Build and launch the HeadlessApplication.
        // ------------------------------------------------------------------
        ReplayListener listener = new ReplayListener(matchLog);

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        // At least 60 updates/second as required; we use a high value so the
        // simulation finishes quickly in CI / replay scenarios.
        config.updatesPerSecond = 240;

        LOG.info("Starting HeadlessApplication …");
        HeadlessApplication app = new HeadlessApplication(listener, config);

        // ------------------------------------------------------------------
        // 3. Replace Gdx.input with our custom MockInput subclass.
        //    The HeadlessApplication constructor starts the main-loop thread
        //    and calls listener.create() before returning (or very shortly
        //    after), so Gdx.app is guaranteed to be non-null here.
        // ------------------------------------------------------------------
        Gdx.input = new ReplayMockInput();

        // ------------------------------------------------------------------
        // 4. Wait for the listener to finish all ticks and call dispose().
        //    doneLatch is counted down inside dispose() after the loop exits.
        // ------------------------------------------------------------------
        listener.doneLatch.await();
        LOG.info("HeadlessApplication finished.");

        // ------------------------------------------------------------------
        // 5. Write the transcript.
        // ------------------------------------------------------------------
        Simulation simulation = listener.getSimulation();
        LOG.info("Writing transcript to: " + outputTranscriptPath);
        TranscriptWriter.write(
                matchLog.worldWidth,
                matchLog.worldHeight,
                simulation.getPlayers(),
                matchLog.totalTicks,
                simulation.getCommandsApplied(),
                outputTranscriptPath);

        LOG.info("Done. commandsApplied=" + simulation.getCommandsApplied());

        // Force JVM exit in case any non-daemon threads are still alive
        // (the HeadlessApplication may leave threads running).
        System.exit(0);
    }
}
