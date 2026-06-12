# Headless High-Score Tracker with libGDX Preferences

## Background
You are building the offline match-results processor for a libGDX-based arcade game. The game runs as a headless background service that ingests match results, maintains per-player high scores using libGDX `Preferences`, and prints a deterministic transcript of every event it processes. The service must be driven entirely by the libGDX headless backend (`gdx-backend-headless`) so it can run on a CI agent without OpenGL, audio, or a real display.

The service is launched by Gradle, and it consumes one command per simulated frame from an input file. Each frame, the `ApplicationListener.render()` callback reads exactly one line from the input file via `Gdx.files.local(...)`, mutates a libGDX `Preferences` instance, and writes a single transcript line through `Gdx.app.log(...)`. When the input is exhausted, the application must persist preferences and call `Gdx.app.exit()` so the headless main-loop thread terminates cleanly.

## Requirements
- Implement a Gradle project at `/home/user/highscores` with a `core` module (game logic, an `ApplicationListener`) and a `headless` module that launches the listener using `HeadlessApplication`.
- Use libGDX `1.14.2` for `com.badlogicgames.gdx:gdx` and `com.badlogicgames.gdx:gdx-backend-headless`.
- The headless launcher MUST configure `HeadlessApplicationConfiguration.preferencesDirectory` to the absolute path `/home/user/highscores/prefs` and `updatesPerSecond` to `0` (run as fast as possible).
- The libGDX `Preferences` store MUST be named `arcade_scores` (i.e. `Gdx.app.getPreferences("arcade_scores")`).
- Input is read from a plain-text file. The file path is supplied as the FIRST command-line argument to the headless launcher and made available to the listener via `Gdx.files.local(...)` (the launcher MUST set the working directory or argument so the listener can resolve it). The input format is documented in detail below.
- Output is written to `/home/user/highscores/transcript.log`. The launcher MUST redirect `Gdx.app.log` to that file (e.g. by installing a custom `ApplicationLogger` or by piping stdout in the launcher) so the verifier can read the transcript independent of timestamps. Each line in `transcript.log` MUST follow the formats listed in **Acceptance Criteria**.
- After processing every input line, the application MUST call `prefs.flush()` and then `Gdx.app.exit()`. The launcher MUST `join()` the headless main-loop thread before the JVM returns.
- The project MUST be buildable and runnable with the bundled Gradle wrapper using:
  - `./gradlew --no-daemon --offline headless:run --args="/home/user/highscores/events.txt"`

## Input Format (events.txt)
Each line is one of the following commands. Commands are processed one per `render()` tick, in order. Tokens are separated by a single space and player names contain only `[A-Za-z0-9_]`.

- `SCORE <player> <int>` — Submit `<int>` as a candidate score for `<player>`. If the player is not yet in the preferences, or if `<int>` is strictly greater than the stored score, update the preferences entry to `<int>`. Otherwise, leave the preferences unchanged.
- `RESET <player>` — Remove the preferences entry for `<player>` if present; ignore if absent.
- `DUMP` — Emit the current contents of the preferences, sorted by player name in ascending lexicographic order.
- `FLUSH` — Call `prefs.flush()` immediately.
- `# ...` and blank lines — Ignore (do NOT emit a transcript line, do NOT consume a tick).

The end-of-file terminates processing.

## Implementation Hints
- Subclass `ApplicationAdapter` for the core logic; do not touch `Gdx.gl*`, `SpriteBatch`, or any GL-bound class anywhere in the code path executed under headless mode.
- Drive one command per tick from inside `render()` by tracking the current line index; do NOT block the render loop reading the whole file at once. Skip comment/blank lines in the same tick so each emitted transcript line corresponds to a real command.
- Use `Gdx.app.getPreferences("arcade_scores")` to obtain the store; remember that headless preferences are persisted to XML under the `preferencesDirectory` configured on the launcher.
- Install a custom `ApplicationLogger` (set via `Gdx.app.setApplicationLogger(...)`) that appends each `log(tag, message)` call as a single line to `transcript.log`. This avoids any interleaving with Gradle's stdout.
- Make the launcher write the transcript file with a `BufferedWriter`, flush it on `pause()`/`dispose()` of a `LifecycleListener`, and `join()` the main-loop thread before exiting.
- Use the `--offline` Gradle flag during development; the verifier will run Gradle with `--offline` and an offline Maven cache.

## Acceptance Criteria
- Project path: /home/user/highscores
- Command: `cd /home/user/highscores && ./gradlew --no-daemon --offline headless:run --args="<absolute-path-to-events-file>"`
- Library versions: `com.badlogicgames.gdx:gdx:1.14.2` and `com.badlogicgames.gdx:gdx-backend-headless:1.14.2`.
- The headless launcher MUST construct a `HeadlessApplication` with `HeadlessApplicationConfiguration` whose `preferencesDirectory` equals `/home/user/highscores/prefs` and whose `updatesPerSecond` equals `0`.
- After the run completes:
  - `/home/user/highscores/prefs/arcade_scores` MUST exist and MUST be a valid XML `<properties>` file (the format produced by `java.util.Properties.storeToXML`). Each surviving player MUST appear as `<entry key="<player>">N</entry>` where `N` is the player's final high score as a base-10 integer string.
  - `/home/user/highscores/transcript.log` MUST exist and MUST contain ONLY transcript lines in the order produced by the run (no Gradle output, no stack traces). The trailing newline at end of file is allowed.
- Transcript line formats (each `render()` tick that processes a non-skipped command emits exactly one line; lines have no leading/trailing whitespace):
  - For `SCORE <player> <n>` that updates the store: `UPDATE <player> <previous_or_NEW> -> <n>`
    - When no previous value existed, use the literal token `NEW`. Example: `UPDATE alice NEW -> 42`.
    - When a previous value existed, use its integer string. Example: `UPDATE bob 30 -> 75`.
  - For `SCORE <player> <n>` that does NOT update the store (because `<n>` is not strictly greater than the stored value): `KEEP <player> <stored> >= <n>`. Example: `KEEP alice 42 >= 10`.
  - For `RESET <player>` when the player existed: `RESET <player> was <previous>`. Example: `RESET alice was 42`.
  - For `RESET <player>` when the player did NOT exist: `RESET <player> missing`.
  - For `DUMP` with K players in the store: emit `DUMP <K>` on its own line, then K subsequent lines `  <player> <score>` (two leading spaces, single space between player and score) in ascending lexicographic order of player name. If K is 0, only `DUMP 0` is emitted.
  - For `FLUSH`: `FLUSH ok`.
- The launcher MUST `join()` the headless main-loop thread before returning, so `gradlew headless:run` exits with status `0` only after preferences are persisted and the transcript is fully flushed.

