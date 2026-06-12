# Animation Keyframe Sampler (libGDX Headless Backend)

## Background
libGDX ships a generic `com.badlogic.gdx.graphics.g2d.Animation<T>` class used to drive frame-by-frame animation playback in games. The class is purely time-based logic (no OpenGL state), so it can be exercised entirely under the `gdx-backend-headless` runtime for deterministic offline simulations, CI checks, or replay tooling.

Your task is to build a small libGDX 1.14.2 project that boots the **headless** backend and uses `Animation<Integer>` to sample integer keyframes against a user-supplied playback configuration, writing the results to disk.

## Requirements
- Create a Gradle project that depends on libGDX 1.14.2 (`gdx`, `gdx-backend-headless`, `gdx-platform:natives-desktop`).
- Implement an `ApplicationListener` (or `ApplicationAdapter`) that constructs an `Animation<Integer>` from a configuration file and writes one output line per requested sample time, then calls `Gdx.app.exit()`.
- The launcher must use `com.badlogic.gdx.backends.headless.HeadlessApplication` and `HeadlessApplicationConfiguration` to host the listener. No graphical backend, audio, or `Gdx.gl*` access is permitted.
- The launcher main method must accept two command-line arguments: an input configuration file path and an output file path.
- The application must support all five values of `Animation.PlayMode`: `NORMAL`, `LOOP`, `REVERSED`, `LOOP_REVERSED`, `LOOP_PINGPONG`.
- The `finished` flag in the output is taken from `Animation.isAnimationFinished(stateTime)` (which depends on absolute elapsed time, not play mode).

## Implementation Hints
- Refer to the libGDX wiki entries on the [application lifecycle](https://libgdx.com/wiki/app/the-life-cycle) and [2D Animation](https://libgdx.com/wiki/graphics/2d/2d-animation) for the `Animation` API and `getKeyFrame(stateTime)` semantics.
- Use the [`HeadlessApplication`](https://github.com/libgdx/libgdx/blob/master/backends/gdx-backend-headless/src/com/badlogic/gdx/backends/headless/HeadlessApplication.java) source as a reference for how to boot the runtime; setting `HeadlessApplicationConfiguration.updatesPerSecond = 0` makes the loop run as fast as possible, which is appropriate here since you do not depend on `getDeltaTime()`.
- Read and write files through `Gdx.files.absolute(...)` so the launcher works regardless of the working directory. Argument paths are absolute when invoked by the verifier.
- `Animation<Integer>` is fully type-safe — pass a `com.badlogic.gdx.utils.Array<Integer>` (or vararg) of `Integer` keyframes when constructing it. The `getKeyFrame(stateTime)` overload without the `looping` parameter is the one that respects the animation's own `PlayMode`.
- The Gradle `application` plugin (with `mainClass`) is a convenient way to expose a `run` task that forwards CLI arguments via `--args`.
- Keep the project self-contained: no other Maven repositories besides Maven Central are needed.

## Acceptance Criteria
- Project path: `/home/user/myproject`
- The project builds successfully with: `./gradlew --no-daemon --offline build`
- Command: `./gradlew --no-daemon --offline run --args="<config_path> <output_path>"`
- Input configuration file format (UTF-8 text, one directive per line; blank lines and lines starting with `#` are ignored):
  - `frameDuration <float>` — keyframe duration in seconds (single occurrence, required).
  - `playMode <NORMAL|LOOP|REVERSED|LOOP_REVERSED|LOOP_PINGPONG>` — single occurrence, required.
  - `keyFrames <int> <int> ...` — whitespace-separated integer keyframes, at least one value (single occurrence, required).
  - `sample <float>` — one sample time per line; the directive may appear any number of times (including zero). The order of the resulting output lines must match the order of `sample` directives in the file.
- Output file format (UTF-8 text). For every `sample <time>` directive, write exactly one line:
  - `<time> <keyFrameValue> <finished>` with single-space separators and a trailing newline.
  - `<time>` is the original string from the configuration line (verbatim, no reformatting).
  - `<keyFrameValue>` is the integer returned by `Animation.getKeyFrame(stateTime)` using the configured play mode (decimal integer, no leading zeros).
  - `<finished>` is the literal string `true` or `false` returned by `Animation.isAnimationFinished(stateTime)`.
- The application must exit cleanly with exit code `0` after writing all samples; it must not require any keyboard input.
- If the output file already exists it must be overwritten (not appended).
- The build must succeed without network access after the initial Docker image build (the verifier runs `./gradlew --offline`).

