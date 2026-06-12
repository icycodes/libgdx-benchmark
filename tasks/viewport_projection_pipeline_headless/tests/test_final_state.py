import json
import os
import re
import shutil
import subprocess

import pytest


PROJECT_DIR = "/home/user/myproject"
OUTPUT_LOG = "/tmp/output.log"


# ---------------------------------------------------------------------------
# Project structure / wiring
# ---------------------------------------------------------------------------

def test_project_directory_exists():
    assert os.path.isdir(PROJECT_DIR), (
        f"Expected libGDX project directory at {PROJECT_DIR}."
    )


def test_gradle_wrapper_exists():
    gradlew = os.path.join(PROJECT_DIR, "gradlew")
    assert os.path.isfile(gradlew), (
        f"Gradle wrapper script {gradlew} is missing; the task requires a bootstrapped wrapper."
    )
    assert os.access(gradlew, os.X_OK), f"Gradle wrapper {gradlew} must be executable."


def test_core_and_headless_modules_present():
    core_build = os.path.join(PROJECT_DIR, "core", "build.gradle")
    headless_build = os.path.join(PROJECT_DIR, "headless", "build.gradle")
    assert os.path.isfile(core_build), (
        f"Expected core module build script at {core_build}."
    )
    assert os.path.isfile(headless_build), (
        f"Expected headless module build script at {headless_build}."
    )


def test_uses_libgdx_headless_backend_1_14_2():
    """The research plan pins libGDX to 1.14.2. Verify any build.gradle declares the headless backend."""
    found = False
    pattern = re.compile(
        r"gdx-backend-headless[\"':\s]*1\.14\.2", re.IGNORECASE
    )
    for root, _dirs, files in os.walk(PROJECT_DIR):
        if os.sep + ".gradle" + os.sep in root + os.sep:
            continue
        if os.sep + "build" + os.sep in root + os.sep:
            continue
        for fname in files:
            if fname.endswith(".gradle") or fname.endswith(".gradle.kts"):
                path = os.path.join(root, fname)
                try:
                    with open(path, encoding="utf-8") as f:
                        content = f.read()
                except OSError:
                    continue
                if pattern.search(content):
                    found = True
                    break
        if found:
            break
    assert found, (
        "No build.gradle under the project references `gdx-backend-headless:1.14.2`. "
        "The research plan pins libGDX to 1.14.2 and the task requires the headless backend."
    )


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _write_scenario(path: str, scenario: dict) -> None:
    with open(path, "w", encoding="utf-8") as f:
        json.dump(scenario, f)


def _run_launcher(scenario_path: str, output_path: str) -> subprocess.CompletedProcess:
    if os.path.exists(output_path):
        os.remove(output_path)
    cmd = [
        "./gradlew",
        "--no-daemon",
        "--console=plain",
        "headless:run",
        f"--args=--scenario={scenario_path} --output={output_path}",
    ]
    return subprocess.run(
        cmd,
        cwd=PROJECT_DIR,
        capture_output=True,
        text=True,
        timeout=600,
    )


def _read_log(path: str) -> list[str]:
    with open(path, encoding="utf-8") as f:
        return [line.rstrip("\n") for line in f.readlines()]


# ---------------------------------------------------------------------------
# Scenario 2 — FitViewport (from truth)
# ---------------------------------------------------------------------------

FIT_SCENARIO = {
    "viewport": "fit",
    "worldWidth": 100.0,
    "worldHeight": 100.0,
    "cameraPosition": {"x": 50.0, "y": 50.0},
    "frames": [
        {
            "frame": 0,
            "resize": {"width": 200, "height": 200},
            "points": [
                {"x": 0.0, "y": 0.0},
                {"x": 50.0, "y": 50.0},
                {"x": 100.0, "y": 100.0},
            ],
        },
        {
            "frame": 1,
            "resize": {"width": 400, "height": 200},
            "points": [{"x": 50.0, "y": 50.0}],
        },
    ],
}

FIT_EXPECTED = [
    "FRAME 0 PROJECT (0.000,0.000) -> (0.000,0.000)",
    "FRAME 0 PROJECT (50.000,50.000) -> (100.000,100.000)",
    "FRAME 0 PROJECT (100.000,100.000) -> (200.000,200.000)",
    "FRAME 1 PROJECT (50.000,50.000) -> (200.000,100.000)",
    "ROUNDTRIP 0 (0.000,0.000) -> (0.000,0.000) OK",
    "ROUNDTRIP 0 (50.000,50.000) -> (50.000,50.000) OK",
    "ROUNDTRIP 0 (100.000,100.000) -> (100.000,100.000) OK",
    "ROUNDTRIP 1 (50.000,50.000) -> (50.000,50.000) OK",
    "END frames=2 points=4",
]


def test_fit_viewport_scenario():
    scenario_path = "/tmp/fit.json"
    _write_scenario(scenario_path, FIT_SCENARIO)
    result = _run_launcher(scenario_path, OUTPUT_LOG)
    assert result.returncode == 0, (
        f"`gradlew headless:run` failed with exit code {result.returncode}.\n"
        f"STDOUT:\n{result.stdout}\nSTDERR:\n{result.stderr}"
    )
    assert os.path.isfile(OUTPUT_LOG), (
        f"Expected output log at {OUTPUT_LOG} but the file was not created."
    )
    lines = _read_log(OUTPUT_LOG)
    assert lines == FIT_EXPECTED, (
        "FitViewport scenario produced unexpected output.\n"
        f"Expected:\n{chr(10).join(FIT_EXPECTED)}\n\nActual:\n{chr(10).join(lines)}"
    )


# ---------------------------------------------------------------------------
# Scenario 3 — StretchViewport (from truth)
# ---------------------------------------------------------------------------

STRETCH_SCENARIO = {
    "viewport": "stretch",
    "worldWidth": 10.0,
    "worldHeight": 10.0,
    "cameraPosition": {"x": 5.0, "y": 5.0},
    "frames": [
        {
            "frame": 0,
            "resize": {"width": 800, "height": 600},
            "points": [
                {"x": 0.0, "y": 0.0},
                {"x": 10.0, "y": 10.0},
                {"x": 5.0, "y": 5.0},
            ],
        }
    ],
}

STRETCH_EXPECTED = [
    "FRAME 0 PROJECT (0.000,0.000) -> (0.000,0.000)",
    "FRAME 0 PROJECT (10.000,10.000) -> (800.000,600.000)",
    "FRAME 0 PROJECT (5.000,5.000) -> (400.000,300.000)",
    "ROUNDTRIP 0 (0.000,0.000) -> (0.000,0.000) OK",
    "ROUNDTRIP 0 (10.000,10.000) -> (10.000,10.000) OK",
    "ROUNDTRIP 0 (5.000,5.000) -> (5.000,5.000) OK",
    "END frames=1 points=3",
]


def test_stretch_viewport_scenario():
    scenario_path = "/tmp/stretch.json"
    _write_scenario(scenario_path, STRETCH_SCENARIO)
    result = _run_launcher(scenario_path, OUTPUT_LOG)
    assert result.returncode == 0, (
        f"`gradlew headless:run` failed with exit code {result.returncode}.\n"
        f"STDOUT:\n{result.stdout}\nSTDERR:\n{result.stderr}"
    )
    lines = _read_log(OUTPUT_LOG)
    assert lines == STRETCH_EXPECTED, (
        "StretchViewport scenario produced unexpected output.\n"
        f"Expected:\n{chr(10).join(STRETCH_EXPECTED)}\n\nActual:\n{chr(10).join(lines)}"
    )


# ---------------------------------------------------------------------------
# Scenario 4 — Empty-frame counting (from truth)
# ---------------------------------------------------------------------------

EMPTY_SCENARIO = {
    "viewport": "stretch",
    "worldWidth": 4.0,
    "worldHeight": 4.0,
    "cameraPosition": {"x": 2.0, "y": 2.0},
    "frames": [
        {"frame": 0, "resize": {"width": 400, "height": 400}, "points": []},
        {
            "frame": 1,
            "resize": {"width": 400, "height": 400},
            "points": [{"x": 2.0, "y": 2.0}],
        },
        {"frame": 2, "resize": {"width": 400, "height": 400}, "points": []},
    ],
}

EMPTY_EXPECTED = [
    "FRAME 1 PROJECT (2.000,2.000) -> (200.000,200.000)",
    "ROUNDTRIP 1 (2.000,2.000) -> (2.000,2.000) OK",
    "END frames=3 points=1",
]


def test_empty_frame_counting():
    scenario_path = "/tmp/empty.json"
    _write_scenario(scenario_path, EMPTY_SCENARIO)
    result = _run_launcher(scenario_path, OUTPUT_LOG)
    assert result.returncode == 0, (
        f"`gradlew headless:run` failed with exit code {result.returncode}.\n"
        f"STDOUT:\n{result.stdout}\nSTDERR:\n{result.stderr}"
    )
    lines = _read_log(OUTPUT_LOG)
    assert lines == EMPTY_EXPECTED, (
        "Empty-frame scenario produced unexpected output.\n"
        f"Expected:\n{chr(10).join(EMPTY_EXPECTED)}\n\nActual:\n{chr(10).join(lines)}"
    )


# ---------------------------------------------------------------------------
# Scenario 5 — Output overwrite (from truth)
# ---------------------------------------------------------------------------

def test_output_is_overwritten_between_runs():
    # Run stretch scenario first.
    stretch_path = "/tmp/stretch.json"
    _write_scenario(stretch_path, STRETCH_SCENARIO)
    first = _run_launcher(stretch_path, OUTPUT_LOG)
    assert first.returncode == 0, (
        f"First run (stretch) failed: {first.stderr}"
    )
    assert os.path.isfile(OUTPUT_LOG), "Output log missing after first run."

    # Run empty scenario immediately afterwards, WITHOUT deleting the output file.
    empty_path = "/tmp/empty.json"
    _write_scenario(empty_path, EMPTY_SCENARIO)
    cmd = [
        "./gradlew",
        "--no-daemon",
        "--console=plain",
        "headless:run",
        f"--args=--scenario={empty_path} --output={OUTPUT_LOG}",
    ]
    second = subprocess.run(
        cmd, cwd=PROJECT_DIR, capture_output=True, text=True, timeout=600
    )
    assert second.returncode == 0, (
        f"Second run (empty) failed: {second.stderr}"
    )
    lines = _read_log(OUTPUT_LOG)
    assert lines == EMPTY_EXPECTED, (
        "Output log was not overwritten between runs.\n"
        f"Expected only the empty scenario's lines:\n{chr(10).join(EMPTY_EXPECTED)}\n\n"
        f"Actual:\n{chr(10).join(lines)}"
    )


# ---------------------------------------------------------------------------
# Tool availability sanity (relied on by every other test above).
# ---------------------------------------------------------------------------

def test_gradlew_executable():
    gradlew = os.path.join(PROJECT_DIR, "gradlew")
    assert shutil.which(gradlew) or os.access(gradlew, os.X_OK), (
        f"gradlew at {gradlew} must be executable to run the headless launcher."
    )


@pytest.fixture(autouse=True)
def _cleanup_tmp_files():
    yield
    for path in ("/tmp/output.log", "/tmp/fit.json", "/tmp/stretch.json", "/tmp/empty.json"):
        if os.path.exists(path):
            try:
                os.remove(path)
            except OSError:
                pass
