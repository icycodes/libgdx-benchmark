import os
import re
import subprocess

PROJECT_DIR = "/home/user/myproject"
VERIFY_DIR = os.path.join(PROJECT_DIR, "verify")
OUTPUT_DIR = os.path.join(PROJECT_DIR, "output")
JAVA_SRC_DIR = os.path.join(PROJECT_DIR, "src", "main", "java")

GRADLE_CMD = [
    "./gradlew",
    "--no-daemon",
    "--console=plain",
    "--offline",
    "run",
]


def _run_scheduler(events_rel_path: str, output_rel_path: str) -> subprocess.CompletedProcess:
    return subprocess.run(
        GRADLE_CMD + ["--args=" + f"{events_rel_path} {output_rel_path}"],
        cwd=PROJECT_DIR,
        capture_output=True,
        text=True,
        timeout=600,
    )


def _write_fixture(rel_path: str, content: str) -> None:
    os.makedirs(VERIFY_DIR, exist_ok=True)
    abs_path = os.path.join(PROJECT_DIR, rel_path)
    os.makedirs(os.path.dirname(abs_path), exist_ok=True)
    with open(abs_path, "w", encoding="utf-8") as fh:
        fh.write(content)


def _cleanup_output(rel_path: str) -> None:
    abs_path = os.path.join(PROJECT_DIR, rel_path)
    if os.path.exists(abs_path):
        os.remove(abs_path)


def test_build_gradle_pins_libgdx_dependencies():
    build_gradle = os.path.join(PROJECT_DIR, "build.gradle")
    with open(build_gradle, "r", encoding="utf-8") as fh:
        content = fh.read()
    for dep in (
        "com.badlogicgames.gdx:gdx:1.14.2",
        "com.badlogicgames.gdx:gdx-backend-headless:1.14.2",
        "com.badlogicgames.gdx:gdx-platform:1.14.2:natives-desktop",
    ):
        assert dep in content, f"build.gradle must keep dependency {dep!r}."


def test_source_uses_binary_heap():
    assert os.path.isdir(JAVA_SRC_DIR), (
        f"Expected Java sources under {JAVA_SRC_DIR}; the executor must create the application."
    )
    found = False
    for root, _dirs, files in os.walk(JAVA_SRC_DIR):
        for fname in files:
            if fname.endswith(".java"):
                with open(os.path.join(root, fname), "r", encoding="utf-8") as fh:
                    if "com.badlogic.gdx.utils.BinaryHeap" in fh.read():
                        found = True
                        break
        if found:
            break
    assert found, (
        "No Java source under src/main/java references com.badlogic.gdx.utils.BinaryHeap; "
        "the scheduler must use libGDX's BinaryHeap."
    )


def test_basic_ordering_across_distinct_ticks():
    rel_input = "verify/events_basic.txt"
    rel_output = "output/verify.log"
    _write_fixture(
        rel_input,
        (
            "# basic event log\n"
            "3 hello first-message\n"
            "1 boot system online\n"
            "5 shutdown gracefully terminating\n"
            "\n"
            "2 tick payload-two\n"
        ),
    )
    _cleanup_output(rel_output)

    result = _run_scheduler(rel_input, rel_output)
    assert result.returncode == 0, (
        f"Gradle run failed (rc={result.returncode}).\n"
        f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )

    abs_out = os.path.join(PROJECT_DIR, rel_output)
    assert os.path.isfile(abs_out), f"Expected output file {abs_out} to be created."
    with open(abs_out, "r", encoding="utf-8") as fh:
        produced = fh.read()

    expected = (
        "[tick=1] boot: system online\n"
        "[tick=2] tick: payload-two\n"
        "[tick=3] hello: first-message\n"
        "[tick=5] shutdown: gracefully terminating\n"
    )
    assert produced == expected, (
        "Dispatch log mismatch for distinct-tick fixture.\n"
        f"--- expected ---\n{expected}--- actual ---\n{produced}"
    )


def test_tie_breaking_among_same_tick_events():
    rel_input = "verify/events_dup.txt"
    rel_output = "output/verify_dup.log"
    _write_fixture(
        rel_input,
        (
            "2 zeta last entry at tick 2\n"
            "2 alpha first entry at tick 2\n"
            "2 mid middle entry at tick 2\n"
            "1 alpha message at tick 1\n"
        ),
    )
    _cleanup_output(rel_output)

    result = _run_scheduler(rel_input, rel_output)
    assert result.returncode == 0, (
        f"Gradle run failed (rc={result.returncode}).\n"
        f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )

    abs_out = os.path.join(PROJECT_DIR, rel_output)
    assert os.path.isfile(abs_out), f"Expected output file {abs_out} to be created."
    with open(abs_out, "r", encoding="utf-8") as fh:
        produced = fh.read()

    expected = (
        "[tick=1] alpha: message at tick 1\n"
        "[tick=2] alpha: first entry at tick 2\n"
        "[tick=2] mid: middle entry at tick 2\n"
        "[tick=2] zeta: last entry at tick 2\n"
    )
    assert produced == expected, (
        "Dispatch log mismatch for same-tick fixture (tie-breaking failed).\n"
        f"--- expected ---\n{expected}--- actual ---\n{produced}"
    )


def test_empty_or_comment_only_input():
    rel_input = "verify/events_empty.txt"
    rel_output = "output/verify_empty.log"
    _write_fixture(
        rel_input,
        (
            "# this file has no events\n"
            "# only comments\n"
            "\n"
        ),
    )
    _cleanup_output(rel_output)

    result = _run_scheduler(rel_input, rel_output)
    assert result.returncode == 0, (
        f"Gradle run failed for empty input (rc={result.returncode}).\n"
        f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )

    abs_out = os.path.join(PROJECT_DIR, rel_output)
    assert os.path.isfile(abs_out), f"Expected output file {abs_out} to be created even when input is empty."
    with open(abs_out, "r", encoding="utf-8") as fh:
        produced = fh.read()

    # The spec allows an empty file or a single trailing newline.
    assert produced in ("", "\n"), (
        "For a comment-only input, the output file must be empty or contain only a single newline. "
        f"Got: {produced!r}"
    )


def test_output_lines_match_dispatch_format():
    # Re-use the basic fixture's output and validate the line format pattern.
    abs_out = os.path.join(PROJECT_DIR, "output", "verify.log")
    assert os.path.isfile(abs_out), (
        "Run the basic ordering test before format validation; verify.log is missing."
    )
    with open(abs_out, "r", encoding="utf-8") as fh:
        lines = [line for line in fh.read().splitlines() if line]

    pattern = re.compile(r"^\[tick=\d+\] [^\s:]+: .+$")
    for line in lines:
        assert pattern.match(line), f"Dispatch line does not match expected format: {line!r}"
