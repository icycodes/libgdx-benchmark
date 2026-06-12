# libGDX InputMultiplexer Handler Chain (Headless Backend)

## Background
Many libGDX games layer several input handlers on top of each other: a UI handler for menus, a gameplay handler for in-world controls, and a catch-all debug handler. libGDX provides the `com.badlogic.gdx.InputMultiplexer` class to chain `InputProcessor` instances so that the first processor returning `true` from a callback consumes the event.

Your task is to build a non-graphical libGDX **command-line application** that runs under the **headless backend** (`com.badlogicgames.gdx:gdx-backend-headless`). The app loads a recorded input script from a text file, replays it through an `InputMultiplexer` that contains three `InputProcessor` implementations (UI, Game, Debug), and writes a deterministic event log to an output file.

## Requirements
- Build a Gradle project that produces a runnable fat/shaded JAR at `/home/user/myproject/build/libs/multiplexer-headless.jar`.
- The application MUST boot libGDX through `HeadlessApplication` with an `ApplicationListener` that performs the work and then calls `Gdx.app.exit()`.
- The application MUST register an `InputMultiplexer` containing exactly three `InputProcessor` instances added in this order: `UiProcessor`, `GameProcessor`, `DebugProcessor`.
- Replay events from the input script through `Gdx.input.getInputProcessor()` (or directly through the multiplexer) and record which processor consumed each event.
- Write a deterministic event log plus a summary block to the output file.

## Input Script Format
The input file is a UTF-8 text file. Each non-empty, non-comment line describes one event. Lines beginning with `#` are comments. Supported event types:

- `keyDown <KEY>` — fires `InputProcessor.keyDown(keycode)`.
- `keyUp <KEY>` — fires `InputProcessor.keyUp(keycode)`.
- `touchDown <X> <Y>` — fires `InputProcessor.touchDown(x, y, 0, Buttons.LEFT)`.
- `touchUp <X> <Y>` — fires `InputProcessor.touchUp(x, y, 0, Buttons.LEFT)`.

`<KEY>` is the `com.badlogic.gdx.Input.Keys` constant name (e.g., `ESCAPE`, `ENTER`, `F1`, `W`, `A`, `S`, `D`, `SPACE`, `X`, `Y`, `Z`, `TAB`). `<X>` and `<Y>` are non-negative integers.

## Processor Routing Rules
- **UiProcessor** consumes (returns `true`):
  - `keyDown` for any of `ESCAPE`, `ENTER`, `F1`.
  - `touchDown` when `y < 100`.
  - All other callbacks return `false`.
- **GameProcessor** consumes (returns `true`):
  - `keyDown` for any of `W`, `A`, `S`, `D`, `SPACE`.
  - All `keyUp` events.
  - `touchDown` when `y >= 100`.
  - All `touchUp` events.
  - All other callbacks return `false`.
- **DebugProcessor** consumes every remaining event (always returns `true`).

Each event MUST be claimed by exactly one processor because the chain ends with the catch-all `DebugProcessor`.

## Output File Format
The output file MUST be UTF-8 with Unix line endings and contain two sections in this order:

```
EVENT_LOG:
<index> <raw line> -> <PROCESSOR>
...
SUMMARY:
UI=<count>
GAME=<count>
DEBUG=<count>
TOTAL=<count>
```

Details:
- `<index>` starts at `1` and counts only events (comments and blank lines are skipped).
- `<raw line>` is the original event line with leading/trailing whitespace stripped and internal whitespace collapsed to single spaces (e.g., `keyDown SPACE`, `touchDown 10 50`).
- `<PROCESSOR>` is one of `UI`, `GAME`, `DEBUG`.
- `SUMMARY` lists totals for each processor and a `TOTAL` line equal to the number of replayed events.
- The file MUST end with a trailing newline.

## Implementation Hints
- Use the official libGDX Gradle coordinates pinned to version `1.14.2` (`com.badlogicgames.gdx:gdx`, `com.badlogicgames.gdx:gdx-backend-headless`, `com.badlogicgames.gdx:gdx-platform:natives-desktop`).
- Boot the app with `new HeadlessApplication(listener, config)` and configure `updatesPerSecond` to `0` for an event-driven loop.
- Use a Gradle plugin such as `com.github.johnrengelman.shadow` (or the `gradle-shadow-plugin`) to produce the single runnable jar named exactly `multiplexer-headless.jar`. Set the `Main-Class` manifest attribute correctly.
- Translate `<KEY>` strings to keycodes with `com.badlogic.gdx.Input.Keys` (you can use reflection on the public static fields).
- Implement `UiProcessor`, `GameProcessor`, and `DebugProcessor` as plain `InputAdapter` subclasses that record the event into a shared log when they decide to consume it.
- Replay each event by calling the appropriate multiplexer method (`keyDown`, `keyUp`, `touchDown`, `touchUp`). The return value tells you which processor consumed it only indirectly; record the consumer inside the processor itself.
- Call `Gdx.app.exit()` once the replay completes and wait for `HeadlessApplication` to finish before the program returns (the main thread will already have invoked the listener).

## Acceptance Criteria
- Project path: `/home/user/myproject`
- Build command (already produces the jar on first run): `cd /home/user/myproject && ./gradlew --no-daemon shadowJar`
- Produced artifact: `/home/user/myproject/build/libs/multiplexer-headless.jar`
- Command: `java -jar /home/user/myproject/build/libs/multiplexer-headless.jar <input_file> <output_file>`
  - `<input_file>` is a path to a UTF-8 text file using the Input Script Format described above.
  - `<output_file>` is the path to a file the program MUST create or overwrite using the Output File Format described above.
  - The process MUST exit with status code `0` on success and a non-zero code on any I/O or parsing error.
- The application MUST run under the libGDX headless backend (no native windowing, no OpenGL calls).
- The output file MUST conform exactly to the Output File Format. Line ordering MUST match the order in the input script (comments and blank lines skipped).
- The summary line counts MUST match the actual processor that consumed each event.

