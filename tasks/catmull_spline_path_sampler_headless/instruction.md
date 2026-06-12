# libGDX Headless CatmullRom Spline Path Sampler

## Background
libGDX exposes a `CatmullRomSpline<Vector2>` class under `com.badlogic.gdx.math` that lets games animate objects along smooth piecewise polynomial paths defined by control points. Designers usually want to validate the sampled curve and its tangent (speed) deterministically in CI without spinning up an OpenGL context. The `gdx-backend-headless` backend makes that possible by booting an `ApplicationListener` on a dedicated thread, ticking `render()` at a configured rate, and mocking out `Gdx.graphics`, `Gdx.input` and friends.

You must build a small libGDX project that runs under the headless backend, loads a spline from JSON, advances one `t` value per render tick, and writes the resulting position and tangent magnitude to a CSV file.

## Requirements
- Single-module Gradle (Groovy DSL) project that pulls `com.badlogicgames.gdx:gdx`, `com.badlogicgames.gdx:gdx-backend-headless`, and `com.badlogicgames.gdx:gdx-platform:...:natives-desktop` at version `1.14.2`.
- A bash launcher `run.sh` that compiles the project (if needed) and runs the headless sampler with three positional arguments: `<spline.json>`, `<input.txt>`, `<output.csv>`.
- A Java `ApplicationListener` (extending `ApplicationAdapter`) that:
  - Loads the spline definition from the JSON file in `create()`.
  - Reads the list of `t` values from the input text file (one float per line, ignoring blank lines).
  - Constructs a `CatmullRomSpline<Vector2>` from the control points.
  - On every `render()` call, consumes the next `t`, calls `spline.valueAt(...)` and `spline.derivativeAt(...)`, and appends one CSV row to the output file.
  - Calls `Gdx.app.exit()` once all `t` values have been processed.
- The application MUST be booted with `HeadlessApplication` (no LWJGL3 launcher) and MUST NOT touch `Gdx.gl*`.
- The `main(String[] args)` method:
  - Validates that exactly three arguments are supplied.
  - Creates a `HeadlessApplicationConfiguration` with `updatesPerSecond = 60`.
  - Boots a `HeadlessApplication` with the listener and configuration.
  - Waits for the headless main loop thread to terminate before returning.
- The Gradle `run` task must accept `--args` so it can be re-invoked from `run.sh` with the three paths.

## Input File Formats
- **`spline.json`** parsed with libGDX's `com.badlogic.gdx.utils.Json` (or `JsonReader`/`JsonValue`):
  ```json
  {
    "continuous": false,
    "controlPoints": [
      { "x": 0.0, "y": 0.0 },
      { "x": 10.0, "y": 0.0 }
    ]
  }
  ```
  When `continuous` is omitted, treat it as `false`. The `controlPoints` array always contains at least four entries.
- **`input.txt`**: one `t` value per line. Each `t` is a float (parsed with `Float.parseFloat`), clamped to `[0, 1]` is NOT required (use the value as-is). Empty/whitespace-only lines are skipped.

## Output File Format
- UTF-8 CSV at the path passed as the third argument. Overwrite if it exists.
- Header row (first line): `tick,t,x,y,speed`
- One data row per `t` value, in the order they appear in `input.txt`, with the schema:
  - `tick`: zero-based integer (the render tick index, starting at 0 for the first processed t).
  - `t`: the original `t` value, formatted with `%.6f` using `Locale.ROOT`.
  - `x`, `y`: the sampled position from `spline.valueAt(out, t)`, formatted with `%.6f` using `Locale.ROOT`.
  - `speed`: the magnitude (`.len()`) of the tangent returned by `spline.derivativeAt(out, t)`, formatted with `%.6f` using `Locale.ROOT`.
- The output file MUST contain only the header plus one row per `t`, and a final newline after the last row.

## Implementation Hints
- See https://libgdx.com/wiki/math-utils/path-interface-and-splines for `CatmullRomSpline<Vector2>` usage (`valueAt(out, t)` and `derivativeAt(out, t)`).
- See https://libgdx.com/wiki/articles/dependency-management-with-gradle for the Maven coordinates and the `application` Gradle plugin (`mainClassName`) that exposes the `run` task.
- See https://github.com/libgdx/libgdx/blob/master/backends/gdx-backend-headless/src/com/badlogic/gdx/backends/headless/HeadlessApplication.java for the `HeadlessApplication` lifecycle: `exit()` posts a runnable that flips `running=false`, and the main loop thread (named `HeadlessApplication`) must be joined to wait for completion.
- Bundle `gdx-platform:1.14.2:natives-desktop` at runtime so `HeadlessNativesLoader.load()` (invoked from the headless backend constructor) finds the desktop natives jar.
- Make sure logging from `Gdx.app.log` goes to stdout; do not redirect it via custom appenders.
- Use `Locale.ROOT` when formatting floats so that decimal separators are always `.`.

## Acceptance Criteria
- Project path: /home/user/gdx-spline-sampler
- Command: `bash /home/user/gdx-spline-sampler/run.sh <spline.json> <input.txt> <output.csv>`
  - `<spline.json>`: absolute or relative path to a spline definition (format described above).
  - `<input.txt>`: absolute or relative path to a t-value list (format described above).
  - `<output.csv>`: absolute or relative path the program must overwrite with the sampled rows.
- After a successful run, `<output.csv>` exists with the header `tick,t,x,y,speed` followed by exactly one row per non-blank `t` line from `<input.txt>`, in the same order, ending with a trailing newline.
- `tick`, `t`, `x`, `y`, `speed` columns must follow the formatting rules above.
- The Gradle module name and package layout are free to choose, but the runnable entry point must be invoked through `bash run.sh`.
- The program MUST use `HeadlessApplication` (no `Lwjgl3Application`) and MUST NOT throw a `NullPointerException` from accessing `Gdx.gl*`.
- A second invocation with a different input set must produce a deterministic output that overwrites the previous file.

