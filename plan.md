# libGDX Research Plan

## 1. Library Overview

* **Description**: libGDX is a mature, cross-platform Java game-development framework that exposes a unified API over OpenGL/OpenGL ES while providing concrete back-ends for desktop (LWJGL3), Android, iOS (RoboVM/MOE), Web (GWT/TeaVM), and a non-rendering **headless** back-end for servers, simulations, and tests. It bundles 2D rendering (`SpriteBatch`, `Stage`/`Scene2D`), 3D rendering, math, physics integrations (Box2D, Bullet), audio, input, asset management, and ECS support (Ashley).
* **Ecosystem Role**: Sits between low-level OpenGL/LWJGL3 and high-level game engines (e.g., Unity). Plays the same role on the JVM as MonoGame does on .NET — a "code-first" framework rather than an editor-driven engine. Gradle is the canonical build system, with sub-modules per target back-end (`core`, `lwjgl3`, `android`, `html`, `ios`).
* **Project Setup (non-interactive CLI)**:
  The official `gdx-liftoff` setup tool is GUI-only, so the reproducible/non-interactive setup uses Gradle directly. The following commands produce an equivalent multi-module layout:

  ```bash
  # 1. Create the project skeleton (no prompts)
  mkdir -p my-gdx-game/core/src/main/java/com/example/game
  mkdir -p my-gdx-game/lwjgl3/src/main/java/com/example/game/lwjgl3
  mkdir -p my-gdx-game/headless/src/main/java/com/example/game/headless
  mkdir -p my-gdx-game/tests/src/test/java/com/example/game
  mkdir -p my-gdx-game/assets
  cd my-gdx-game

  # 2. Bootstrap the Gradle wrapper non-interactively
  gradle wrapper --gradle-version 8.10 --distribution-type bin

  # 3. Write settings.gradle, build.gradle, and module build.gradle files
  #    (declare gdx, gdx-backend-lwjgl3, gdx-backend-headless,
  #     and gdx-platform:natives-desktop in core/headless modules)

  # 4. Resolve dependencies and verify the build
  ./gradlew --no-daemon build

  # 5. Run the desktop launcher (LWJGL3) without prompts
  ./gradlew --no-daemon lwjgl3:run

  # 6. Run the headless launcher (server / CI / tests)
  ./gradlew --no-daemon headless:run

  # 7. Produce a runnable jar
  ./gradlew --no-daemon lwjgl3:jar
  ```

  Required Maven coordinates (pin to `gdxVersion=1.14.2`):
  * `com.badlogicgames.gdx:gdx:1.14.2`
  * `com.badlogicgames.gdx:gdx-backend-lwjgl3:1.14.2`
  * `com.badlogicgames.gdx:gdx-backend-headless:1.14.2`
  * `com.badlogicgames.gdx:gdx-platform:1.14.2:natives-desktop`

  Docs: [Creating a Project](https://libgdx.com/wiki/start/project-generation), [Dependency Management with Gradle](https://libgdx.com/wiki/articles/dependency-management-with-gradle).

## 2. Core Primitives & APIs

### 2.1 Application Framework
* **`ApplicationListener`** — entry-point interface implementing `create()`, `render()`, `resize()`, `pause()`, `resume()`, `dispose()`. The render loop is invoked once per frame. ([life cycle docs](https://libgdx.com/wiki/app/the-life-cycle))
* **`ApplicationAdapter`** — empty default implementation, typical superclass.
* **`Gdx` static facade** — `Gdx.app`, `Gdx.graphics`, `Gdx.files`, `Gdx.input`, `Gdx.audio`, `Gdx.net`, populated by whichever back-end booted the app.

```java
public class MyGame extends ApplicationAdapter {
    @Override public void create()  { /* load resources */ }
    @Override public void render()  { /* update + draw */ }
    @Override public void dispose() { /* free GPU resources */ }
}
```

### 2.2 Rendering Primitives
* **`SpriteBatch`**, **`Texture`**, **`TextureRegion`**, **`BitmapFont`** for 2D drawing.
* **`OrthographicCamera`**, **`Viewport`** (`FitViewport`, `ExtendViewport`) for resolution-independent rendering.
* **`Stage` + `Actor` + `Scene2D` UI** (`Table`, `Skin`, `Label`, `TextButton`) — implements `InputProcessor`. ([Scene2D](https://libgdx.com/wiki/graphics/2d/scene2d/scene2d), [Scene2D.ui](https://libgdx.com/wiki/graphics/2d/scene2d/scene2d-ui))
* **`ShapeRenderer`** for debug/primitive drawing.

### 2.3 Input
* **`Input`** interface (accessed via `Gdx.input`) — polling (`isKeyPressed`, `isKeyJustPressed`, `getX/Y`, `justTouched`) plus event delivery via `setInputProcessor(InputProcessor)`.
* **`InputProcessor`** / **`InputAdapter`** — `keyDown/keyUp/keyTyped/touchDown/...` callbacks.
* **`InputMultiplexer`** — chains multiple processors.

### 2.4 Files, Assets, Math, Utilities
* **`Files` / `FileHandle`** — `Gdx.files.internal/local/external/absolute(path)`.
* **`AssetManager`** — asynchronous, ref-counted resource loading.
* `Vector2`/`Vector3`, `Matrix4`, `MathUtils`, `Rectangle`, `Polygon`.
* **`Disposable`** contract — anything wrapping native memory (textures, batches, stages) must be `.dispose()`d.

### 2.5 Headless Back-end (essential for testing & dataset tasks)
The headless back-end lives in `com.badlogicgames.gdx:gdx-backend-headless`. It replaces graphics, audio and input with mock objects so the rest of libGDX still runs (`Gdx.files`, `Array`, math, `Preferences`, `ApplicationListener` ticks, JSON, etc.).

* **`HeadlessApplication`** — concrete `Application` that owns the main loop thread, wires `Gdx.app/files/net/audio/graphics/input`, and ticks `listener.render()` on a configurable cadence. Source: [`HeadlessApplication.java`](https://github.com/libgdx/libgdx/blob/master/backends/gdx-backend-headless/src/com/badlogic/gdx/backends/headless/HeadlessApplication.java).

  ```java
  HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
  config.updatesPerSecond = 60;          // 0 = run as fast as possible
  config.preferencesDirectory = "build/tmp/prefs";

  ApplicationListener game = new MyGame();
  new HeadlessApplication(game, config); // boots a dedicated "HeadlessApplication" thread
  ```

  Notes:
  * `Gdx.graphics` is a `MockGraphics` — calling OpenGL through `Gdx.gl*` will NPE; keep render code behind a thin "renderer" abstraction so the headless run skips it.
  * `exit()` flips `running=false` from inside a posted `Runnable`, so cleanup is asynchronous; tests should `join()` the main loop thread.
  * `executeRunnables()` drains `postRunnable(...)` queues each tick — useful for injecting test stimuli.

* **`MockInput`** — default `Input` implementation used by `HeadlessApplication`. Every method returns zero / `false` / no-op. Source: [`MockInput.java`](https://github.com/libgdx/libgdx/blob/master/backends/gdx-backend-headless/src/com/badlogic/gdx/backends/headless/mock/input/MockInput.java).

  Recommended pattern: subclass `MockInput`, override the polling/event methods needed by the game, and inject the subclass after the `HeadlessApplication` is constructed:

  ```java
  public class ScriptedInput extends MockInput {
      private final Deque<Integer> keyQueue;        // keycodes from a text file
      private int currentKey = -1;

      public ScriptedInput(Deque<Integer> keys) { this.keyQueue = keys; }

      public void tick() {                          // advance one frame's input
          currentKey = keyQueue.isEmpty() ? -1 : keyQueue.poll();
          InputProcessor p = getInputProcessor();
          if (currentKey >= 0 && p != null) {
              p.keyDown(currentKey);
              p.keyUp(currentKey);
          }
      }
      @Override public boolean isKeyPressed(int key)     { return key == currentKey; }
      @Override public boolean isKeyJustPressed(int key) { return key == currentKey; }
  }

  ScriptedInput scripted = new ScriptedInput(loadKeysFromFile("input.txt"));
  new HeadlessApplication(game, config);
  Gdx.input = scripted;                              // replace MockInput on the live app
  ```

* **`MockGraphics`**, **`MockAudio`**, **`HeadlessFiles`**, **`HeadlessNet`**, **`HeadlessPreferences`** — sibling mocks. `MockGraphics.getDeltaTime()` reflects the configured `updatesPerSecond`, useful for deterministic simulations.

* **Headless Gradle dependency**:
  ```gradle
  dependencies {
      implementation "com.badlogicgames.gdx:gdx:1.14.2"
      implementation "com.badlogicgames.gdx:gdx-backend-headless:1.14.2"
      runtimeOnly    "com.badlogicgames.gdx:gdx-platform:1.14.2:natives-desktop"
  }
  ```

  Run non-interactively:
  ```bash
  ./gradlew --no-daemon headless:run --args="--input=input.txt"
  ./gradlew --no-daemon tests:test  --tests com.example.game.HeadlessTests
  ```

  Reference: [Headless Backend Javadoc](https://javadoc.io/doc/com.badlogicgames.gdx/gdx-backend-headless/latest/index.html), MVN: [LibGDX Headless Backend](https://mvnrepository.com/artifact/com.badlogicgames.gdx/gdx-backend-headless).

## 3. Real-World Use Cases & Templates

* **gdx-liftoff templates** — multiple ready-made `ApplicationListener` skeletons (Scene2D, ECS, Box2D, Headless server) generated by the official setup. ([gdx-liftoff Guide](https://github.com/libgdx/gdx-liftoff/blob/master/Guide.md))
* **`TomGrill/gdx-testing`** — canonical skeleton for JUnit + Mockito + headless-backend testing of libGDX games. ([repo](https://github.com/TomGrill/gdx-testing))
* **Multiplayer game servers** — the headless back-end is intended as the server runtime for games whose simulation code is shared with the LWJGL3 client (see [issue #7481](https://github.com/libgdx/libgdx/issues/7481)).
* **CI-runnable simulations / replay players** — use `HeadlessApplication` with a fixed `updatesPerSecond` and a scripted input source to deterministically replay game traces in CI.
* **Common integration patterns**:
  * Share `core` module between the LWJGL3 launcher and the headless launcher; only the launcher modules differ.
  * Inject a renderer abstraction so the headless build skips all `Gdx.gl*` calls.
  * Pipe gameplay state to `System.out` for verification in tests.

## 4. Developer Friction Points

1. **Tests crash because the headless backend doesn't fully mock libGDX** — classes like `SpriteBatch`, `BitmapFont`, FreeType, or anything touching `Gdx.gl20`/`Gdx.gl30` still require an OpenGL context. Developers must either avoid them in headless builds or stub them. See [Issue #3383: Headless Backend doesn't properly mock all backend pieces](https://github.com/libgdx/libgdx/issues/3383) and [Issue #5995: It would be possible to unit test a GDX game if not for one thing](https://github.com/libgdx/libgdx/issues/5995).
2. **`MockInput` does literally nothing** — every method returns 0/false, so any game that polls `Gdx.input.isKeyPressed` will appear "idle". Subclassing `MockInput` (or replacing `Gdx.input` after `HeadlessApplication` construction) is required for any input-driven scenario.
3. **`HeadlessApplication` runs on its own thread** — test code that calls `app.exit()` and then asserts must `join` the main-loop thread; the `dispose()` callback also fires asynchronously. The lifecycle order (`pause` -> `dispose`) is also called twice on some paths, a known quirk.
4. **Native loading from a plain JUnit test** — without the LWJGL3 launcher you must either pull in `gdx-backend-headless` (which calls `HeadlessNativesLoader.load()` for you) or invoke `new HeadlessNativesLoader().load()` + assign `Gdx.files = new HeadlessFiles()` manually (see [issue #7481](https://github.com/libgdx/libgdx/issues/7481), [StackOverflow](https://stackoverflow.com/questions/79202912/how-do-i-make-unit-tests-for-a-libgdx-application-that-uses-spritebatch-and-othe)).

## 5. Evaluation Ideas

* (Simple) Implement an `ApplicationAdapter` that counts rendered frames and exits after N ticks under `HeadlessApplication`.
* (Simple) Read a configuration value from a `FileHandle` via `Gdx.files.internal` and log it through `Gdx.app.log`.
* (Medium) Build a deterministic tick-based simulation (e.g., bouncing ball with `MockGraphics.getDeltaTime`) and assert end-state via JUnit.
* (Medium) Implement a custom `MockInput` subclass that replays a keystroke sequence from a text file and drive an `InputProcessor`-based game with it.
* (Medium) Wire a Scene2D `Stage` whose `act/draw` is exercised under headless mode without invoking GL draw calls.
* (Hard) Implement a turn-based game loop where each tick consumes one line of input from a file and prints the resulting game state, validated against an expected-output fixture.
* (Hard) Build a headless authoritative server that loads the same `core` module a graphical client would, accepts a recorded input log, and produces a reproducible match transcript.
* (Hard) Integrate Box2D with the headless backend to run a physics simulation in CI and validate body positions after a scripted impulse sequence.

## 6. Sources

1. [libGDX — Creating a Project](https://libgdx.com/wiki/start/project-generation) — official setup, project layout, gdx-liftoff.
2. [gdx-liftoff README](https://github.com/libgdx/gdx-liftoff) — features, headless checkbox, JDK requirements.
3. [gdx-liftoff Guide.md](https://github.com/libgdx/gdx-liftoff/blob/master/Guide.md) — project layout differences vs. legacy gdx-setup.
4. [libGDX — The Life Cycle](https://libgdx.com/wiki/app/the-life-cycle) — `ApplicationListener` contract.
5. [libGDX — Scene2D](https://libgdx.com/wiki/graphics/2d/scene2d/scene2d) and [Scene2D.ui](https://libgdx.com/wiki/graphics/2d/scene2d/scene2d-ui) — UI/scene-graph primitives.
6. [libGDX — Dependency management with Gradle](https://libgdx.com/wiki/articles/dependency-management-with-gradle) — Maven coordinates and Gradle config.
7. [`HeadlessApplication.java`](https://github.com/libgdx/libgdx/blob/master/backends/gdx-backend-headless/src/com/badlogic/gdx/backends/headless/HeadlessApplication.java) — source for the headless application class.
8. [`HeadlessApplicationConfiguration.java`](https://github.com/libgdx/libgdx/blob/master/backends/gdx-backend-headless/src/com/badlogic/gdx/backends/headless/HeadlessApplicationConfiguration.java) — `updatesPerSecond`, `preferencesDirectory` fields.
9. [`MockInput.java`](https://github.com/libgdx/libgdx/blob/master/backends/gdx-backend-headless/src/com/badlogic/gdx/backends/headless/mock/input/MockInput.java) — full source of the no-op input implementation.
10. [LibGDX Headless Backend on Maven Central](https://mvnrepository.com/artifact/com.badlogicgames.gdx/gdx-backend-headless) — coordinates and versions.
11. [Headless Backend Javadoc](https://javadoc.io/doc/com.badlogicgames.gdx/gdx-backend-headless/1.1.0/overview-tree.html) — class hierarchy reference.
12. [Issue #3383 — Headless Backend doesn't properly mock all the backend pieces](https://github.com/libgdx/libgdx/issues/3383) — friction point.
13. [Issue #5995 — Unit testing a GDX game](https://github.com/libgdx/libgdx/issues/5995) — friction point.
14. [Issue #7481 — Using the headless backend classes](https://github.com/libgdx/libgdx/issues/7481) — manual native-loading recipe.
15. [`TomGrill/gdx-testing`](https://github.com/TomGrill/gdx-testing) — reference JUnit + Mockito + headless template.
16. [StackOverflow — Unit testing SpriteBatch/OpenGL classes](https://stackoverflow.com/questions/79202912/how-do-i-make-unit-tests-for-a-libgdx-application-that-uses-spritebatch-and-othe) — community workarounds.
17. [StackOverflow — Testing in libGDX with JUnit](https://stackoverflow.com/questions/77699096/testing-in-libgdx-with-junit-on-intellij) — practical headless-test setup.
18. [Reddit — libGDX Testing](https://www.reddit.com/r/libgdx/comments/reqbe8/libgdx_testing/) — community discussion of testing patterns.

## Notes for Task Generation
* All tasks should require the task executor to use the headless backend to build the app.
* If a task requires input, the task executor should implement `MockInput` based on a text file input sequence. The file format must be clearly described in the task description. Final tests should include cases that use an input sequence text file and validate the output.