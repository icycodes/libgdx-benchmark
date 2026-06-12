import os
import shutil
import subprocess

import pytest


PROJECT_DIR = "/home/user/myproject"
JAR_PATH = os.path.join(PROJECT_DIR, "build", "libs", "multiplexer-headless.jar")


CASE1_INPUT = """# mixed events
keyDown ESCAPE
keyDown W
keyDown X
keyUp W
touchDown 10 50
touchDown 200 300
touchUp 200 300
keyDown F1
keyDown Y
"""

CASE1_EXPECTED = (
    "EVENT_LOG:\n"
    "1 keyDown ESCAPE -> UI\n"
    "2 keyDown W -> GAME\n"
    "3 keyDown X -> DEBUG\n"
    "4 keyUp W -> GAME\n"
    "5 touchDown 10 50 -> UI\n"
    "6 touchDown 200 300 -> GAME\n"
    "7 touchUp 200 300 -> GAME\n"
    "8 keyDown F1 -> UI\n"
    "9 keyDown Y -> DEBUG\n"
    "SUMMARY:\n"
    "UI=3\n"
    "GAME=4\n"
    "DEBUG=2\n"
    "TOTAL=9\n"
)


CASE2_INPUT = """keyDown ESCAPE
keyDown ENTER
keyDown F1
"""

CASE2_EXPECTED = (
    "EVENT_LOG:\n"
    "1 keyDown ESCAPE -> UI\n"
    "2 keyDown ENTER -> UI\n"
    "3 keyDown F1 -> UI\n"
    "SUMMARY:\n"
    "UI=3\n"
    "GAME=0\n"
    "DEBUG=0\n"
    "TOTAL=3\n"
)


CASE3_INPUT = """# comment line, should be skipped

keyDown TAB
keyDown Z
touchDown 0 99
touchDown 0 100
# another comment
keyUp Z
"""

CASE3_EXPECTED = (
    "EVENT_LOG:\n"
    "1 keyDown TAB -> DEBUG\n"
    "2 keyDown Z -> DEBUG\n"
    "3 touchDown 0 99 -> UI\n"
    "4 touchDown 0 100 -> GAME\n"
    "5 keyUp Z -> GAME\n"
    "SUMMARY:\n"
    "UI=1\n"
    "GAME=2\n"
    "DEBUG=2\n"
    "TOTAL=5\n"
)


def _ensure_jar_built():
    """Build the shaded jar once if it isn't already present."""
    if os.path.isfile(JAR_PATH):
        return
    gradlew = os.path.join(PROJECT_DIR, "gradlew")
    assert os.path.isfile(gradlew), f"Gradle wrapper not found at {gradlew}."
    result = subprocess.run(
        [gradlew, "--no-daemon", "shadowJar"],
        cwd=PROJECT_DIR,
        capture_output=True,
        text=True,
        timeout=900,
    )
    assert result.returncode == 0, (
        "Failed to build the shaded jar with ./gradlew --no-daemon shadowJar.\n"
        f"stdout={result.stdout}\nstderr={result.stderr}"
    )
    assert os.path.isfile(JAR_PATH), (
        f"Expected jar {JAR_PATH} to be produced by ./gradlew shadowJar but it was not found."
    )


@pytest.fixture(scope="module", autouse=True)
def jar_built():
    _ensure_jar_built()
    yield


def _run_case(tmp_path, case_name, input_text):
    input_path = tmp_path / f"{case_name}_input.txt"
    output_path = tmp_path / f"{case_name}_output.txt"
    input_path.write_text(input_text, encoding="utf-8")
    if output_path.exists():
        output_path.unlink()

    result = subprocess.run(
        ["java", "-jar", JAR_PATH, str(input_path), str(output_path)],
        cwd=PROJECT_DIR,
        capture_output=True,
        text=True,
        timeout=180,
    )
    assert result.returncode == 0, (
        f"java -jar invocation for {case_name} exited with {result.returncode}.\n"
        f"stdout={result.stdout}\nstderr={result.stderr}"
    )
    assert output_path.exists(), (
        f"Expected output file {output_path} was not produced for case {case_name}.\n"
        f"stdout={result.stdout}\nstderr={result.stderr}"
    )
    return output_path.read_text(encoding="utf-8")


def test_jar_artifact_exists():
    assert os.path.isfile(JAR_PATH), (
        f"Expected shaded jar at {JAR_PATH} but it is missing. Build it with ./gradlew shadowJar."
    )


def test_jar_contains_headless_backend():
    assert shutil.which("unzip") is not None, "unzip is required to inspect the JAR but is missing."
    result = subprocess.run(
        ["unzip", "-l", JAR_PATH],
        capture_output=True,
        text=True,
        timeout=60,
    )
    assert result.returncode == 0, (
        f"unzip -l on {JAR_PATH} failed: stderr={result.stderr!r}"
    )
    assert "com/badlogic/gdx/backends/headless/HeadlessApplication.class" in result.stdout, (
        "The shaded jar does not include the HeadlessApplication class; the headless backend is missing."
    )


def test_jar_contains_input_multiplexer():
    assert shutil.which("unzip") is not None, "unzip is required to inspect the JAR but is missing."
    result = subprocess.run(
        ["unzip", "-l", JAR_PATH],
        capture_output=True,
        text=True,
        timeout=60,
    )
    assert result.returncode == 0, (
        f"unzip -l on {JAR_PATH} failed: stderr={result.stderr!r}"
    )
    assert "com/badlogic/gdx/InputMultiplexer.class" in result.stdout, (
        "The shaded jar does not include InputMultiplexer; it must bundle the libGDX core."
    )


def test_case1_mixed_events(tmp_path):
    actual = _run_case(tmp_path, "case1", CASE1_INPUT)
    assert actual == CASE1_EXPECTED, (
        "Output for case 1 does not match expected content.\n"
        f"--- expected ---\n{CASE1_EXPECTED}\n--- actual ---\n{actual}"
    )


def test_case2_only_ui_events(tmp_path):
    actual = _run_case(tmp_path, "case2", CASE2_INPUT)
    assert actual == CASE2_EXPECTED, (
        "Output for case 2 does not match expected content.\n"
        f"--- expected ---\n{CASE2_EXPECTED}\n--- actual ---\n{actual}"
    )


def test_case3_comments_and_debug_fallthrough(tmp_path):
    actual = _run_case(tmp_path, "case3", CASE3_INPUT)
    assert actual == CASE3_EXPECTED, (
        "Output for case 3 does not match expected content.\n"
        f"--- expected ---\n{CASE3_EXPECTED}\n--- actual ---\n{actual}"
    )
