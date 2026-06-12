# libGDX BinaryHeap Event Scheduler (Headless Backend)

## Background
Many simulation and replay tools built with libGDX need a deterministic event scheduler that fires logical events at scheduled ticks. libGDX ships its own collection classes that are commonly used in such code paths; in particular `com.badlogic.gdx.utils.BinaryHeap` is a min-heap optimized for game-loop scheduling.

Your job is to build a small command-line tool that:
- Boots libGDX through the **headless backend** (`com.badlogicgames.gdx:gdx-backend-headless`),
- Loads a plain-text event log,
- Uses `BinaryHeap` to dispatch events in a deterministic per-tick order,
- Writes the dispatch log to a file and exits cleanly.

The project must be a Gradle project rooted at `/home/user/myproject`. A pre-configured Gradle wrapper, `settings.gradle`, and `build.gradle` (with libGDX core + headless backend + native desktop runtime declared, pinned to `gdxVersion = 1.14.2`) are already provided. You must add the source files.

## Requirements
1. The application must boot a `HeadlessApplication` with a fixed `updatesPerSecond` (use `60`). The `ApplicationListener` must keep ticking until all scheduled events have been emitted, then call `Gdx.app.exit()`.
2. The application must read an event log file passed as the first command-line argument and write the dispatch log to the file passed as the second command-line argument. Both paths are interpreted relative to the project root (i.e. the working directory used by Gradle's `run` task).
3. All events must be loaded into a `com.badlogic.gdx.utils.BinaryHeap` instance using a subclass of `BinaryHeap.Node`. The `value` of each node must equal the scheduled tick (as a float).
4. Each call to `render()` represents exactly one logical tick. Before any event is processed, the tick counter is `0`. The counter is incremented at the start of every `render()` invocation, so the first event is evaluated against tick `1`.
5. During each tick, after the counter advances, all events whose scheduled tick is less than or equal to the current tick must be popped from the heap and emitted in deterministic order. When two events share the same tick, emit them in ascending lexicographic order of their `event_id`.
6. Each emitted event must be written to the output file as a single line in the exact format:

   ```
   [tick=<n>] <event_id>: <message>
   ```

   where `<n>` is the current tick counter (an integer), `<event_id>` is the event's identifier, and `<message>` is the event's message verbatim. The file must end with a single trailing newline.
7. Once the heap is empty, the application must flush the log file, call `Gdx.app.exit()`, and the main process must wait for the `HeadlessApplication` thread to finish before terminating. The process must exit with status `0`.
8. The input file format is:
   - One event per line: `<tick> <event_id> <message>` separated by ASCII spaces; the message is the remainder of the line and may itself contain spaces.
   - `<tick>` is a non-negative integer.
   - `<event_id>` is a non-empty token without whitespace.
   - Lines whose first non-whitespace character is `#` are comments and must be ignored.
   - Blank lines must be ignored.
   - Encoding is UTF-8.
9. The output file must contain only the dispatch lines; comments and blank input lines must not appear in the output. The order of dispatch lines must match the ordering rules in requirement 5.

## Implementation Hints
- The Gradle `run` task is already wired through the Application plugin to a main class named `com.example.scheduler.Main`. Use that fully qualified name for your entry point.
- `BinaryHeap.Node` exposes a `getValue()`/protected `value` field and is added to the heap with `BinaryHeap.add(node)`. Pop the minimum with `BinaryHeap.pop()` and peek with `BinaryHeap.peek()`.
- Because `BinaryHeap` does not guarantee a stable order among equal-priority nodes, gather all entries with the same tick into a temporary list during a tick and sort that list by `event_id` before writing.
- Use `Gdx.files.absolute(path)` (or `new java.io.File(path)`) to read the input log and write the output log; `Gdx.files.internal(...)` will not work for paths under the project root in this layout.
- `HeadlessApplication` runs on its own thread. After the application listener calls `Gdx.app.exit()`, the main thread should `join` the headless thread (it is the only non-daemon thread the headless backend starts) before `main` returns.
- Pin the libGDX coordinates as declared in the provided `build.gradle`; do not change versions or add other dependencies.

## Acceptance Criteria
- Project path: `/home/user/myproject`
- Command: `cd /home/user/myproject && ./gradlew --no-daemon --console=plain --offline run --args="<events_file> <output_file>"`
- Input argument format: `<events_file>` is the path (relative to the project root) of the event-log input file; `<output_file>` is the path (relative to the project root) where the dispatch log will be written.
- The process must exit with status `0`.
- The output file must be created (overwriting any existing file) and must contain the dispatch lines in the format `[tick=<n>] <event_id>: <message>` followed by a trailing newline.
- The application must use the libGDX headless backend (`com.badlogicgames.gdx:gdx-backend-headless`) and must use `com.badlogic.gdx.utils.BinaryHeap` to schedule events.

