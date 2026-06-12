package com.matchreplay.server;

import com.badlogic.gdx.backends.headless.mock.input.MockInput;

/**
 * Minimal {@link MockInput} subclass required by the spec so that the server
 * module owns a concrete subclass of the headless mock-input class.
 *
 * <p>The simulation does <em>not</em> actually read from {@code Gdx.input};
 * per-tick commands are delivered through the parsed {@link com.matchreplay.core.MatchLog}
 * instead.  This class exists to satisfy the requirement that the server replace
 * {@code Gdx.input} with a custom {@code MockInput} subclass after constructing
 * the {@link com.badlogic.gdx.backends.headless.HeadlessApplication}.
 */
public class ReplayMockInput extends MockInput {
    // No overrides needed — the simulation uses the MatchLog directly.
}
