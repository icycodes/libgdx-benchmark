import os
import re
import shutil
import subprocess
import tempfile

import pytest


PROJECT_DIR = "/home/user/gdx-game"
LAUNCHER = os.path.join(PROJECT_DIR, "build", "install", "gdx-game", "bin", "gdx-game")
GRADLEW = os.path.join(PROJECT_DIR, "gradlew")
BUILD_TIMEOUT = 900  # seconds (Box2D natives push first-build downloads up)
RUN_TIMEOUT = 180  # seconds

# Match `Final position: (<x>, <y>)` where coordinates are decimal floats
# (with `.` separator and optional leading `-`).
_FINAL_POSITION_RE = re.compile(
    r"Final position:\s*\(\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)\s*\)"
)


@pytest.fixture(scope="session", autouse=True)
def _build_distribution():
    """Build the runnable distribution before running any test."""
    assert os.path.isdir(PROJECT_DIR), (
        f"Project directory {PROJECT_DIR!r} does not exist; the executor never created it."
    )
    assert os.path.isfile(GRADLEW) and os.access(GRADLEW, os.X_OK), (
        f"Gradle wrapper {GRADLEW!r} is missing or not executable; "
        "the executor must bootstrap it via `gradle wrapper`."
    )

    result = subprocess.run(
        [GRADLEW, "--no-daemon", "--quiet", "installDist"],
        cwd=PROJECT_DIR,
        capture_output=True,
        text=True,
        timeout=BUILD_TIMEOUT,
    )
    assert result.returncode == 0, (
        "`./gradlew --no-daemon --quiet installDist` failed.\n"
        f"stdout:\n{result.stdout}\n"
        f"stderr:\n{result.stderr}"
    )

    assert os.path.isfile(LAUNCHER) and os.access(LAUNCHER, os.X_OK), (
        f"Expected runnable launcher at {LAUNCHER!r} after `installDist`, "
        "but the file is missing or not executable."
    )

    yield


@pytest.fixture()
def fixtures_dir():
    tmp = tempfile.mkdtemp(prefix="gdx-box2d-")
    try:
        yield tmp
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


def _write_fixture(directory: str, name: str, content: str) -> str:
    path = os.path.join(directory, name)
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(content)
    return path


def _run_launcher(scenario_path: str) -> subprocess.CompletedProcess:
    return subprocess.run(
        [LAUNCHER, f"--scenario={scenario_path}"],
        capture_output=True,
        text=True,
        timeout=RUN_TIMEOUT,
    )


def _parse_final_position(stdout: str) -> tuple[float, float]:
    matches = _FINAL_POSITION_RE.findall(stdout)
    assert matches, (
        "Launcher stdout did not contain a `Final position: (x, y)` line.\n"
        f"stdout:\n{stdout}"
    )
    assert len(matches) == 1, (
        "Launcher stdout contained multiple `Final position:` lines; expected exactly one.\n"
        f"stdout:\n{stdout}"
    )
    x_str, y_str = matches[0]
    return float(x_str), float(y_str)


def test_static_body_no_gravity_no_impulse(fixtures_dir):
    """Body sits at its initial position when gravity is zero and no impulses fire."""
    scenario = (
        "GRAVITY 0 0\n"
        "BODY 3.25 -2.5\n"
        "MASS 1.0\n"
        "STEPS 120\n"
    )
    path = _write_fixture(fixtures_dir, "static.txt", scenario)
    result = _run_launcher(path)

    assert result.returncode == 0, (
        "Launcher exited non-zero on a static-body scenario.\n"
        f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )
    x, y = _parse_final_position(result.stdout)
    assert abs(x - 3.25) < 0.001, (
        f"Static-body x coordinate should remain 3.25, got {x}."
    )
    assert abs(y - (-2.5)) < 0.001, (
        f"Static-body y coordinate should remain -2.5, got {y}."
    )


def test_single_impulse_no_gravity(fixtures_dir):
    """With no gravity and mass 1.0, a single impulse over 60 steps (1 sim second)
    must produce displacement equal to the impulse vector itself."""
    scenario = (
        "GRAVITY 0 0\n"
        "BODY 0 0\n"
        "MASS 1.0\n"
        "IMPULSE 0 2.0 -1.0\n"
        "STEPS 60\n"
    )
    path = _write_fixture(fixtures_dir, "impulse.txt", scenario)
    result = _run_launcher(path)

    assert result.returncode == 0, (
        "Launcher exited non-zero on a single-impulse scenario.\n"
        f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )
    x, y = _parse_final_position(result.stdout)
    assert abs(x - 2.0) < 0.01, (
        f"After impulse (2.0, -1.0) over 60 steps the x coordinate should be ~2.0, got {x}."
    )
    assert abs(y - (-1.0)) < 0.01, (
        f"After impulse (2.0, -1.0) over 60 steps the y coordinate should be ~-1.0, got {y}."
    )


def test_gravity_drop_no_impulse(fixtures_dir):
    """Free-fall under gravity (0, -10) with mass 1.0 for 30 steps.

    Semi-implicit Euler displacement: y_disp = g * dt^2 * n*(n+1)/2
    = -10 * (1/3600) * 465 = -1.29167
    """
    scenario = (
        "GRAVITY 0 -10\n"
        "BODY 0 0\n"
        "MASS 1.0\n"
        "STEPS 30\n"
    )
    path = _write_fixture(fixtures_dir, "gravity.txt", scenario)
    result = _run_launcher(path)

    assert result.returncode == 0, (
        "Launcher exited non-zero on a gravity-drop scenario.\n"
        f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )
    x, y = _parse_final_position(result.stdout)
    assert abs(x - 0.0) < 0.01, (
        f"Free-fall x coordinate should remain ~0.0, got {x}."
    )
    expected_y = -1.29167
    assert abs(y - expected_y) < 0.02, (
        f"Free-fall y coordinate after 30 steps under gravity (0,-10) should be ~{expected_y}, got {y}."
    )


def test_combined_gravity_and_multiple_impulses(fixtures_dir):
    """Two upward kicks under gravity (0, -10) for 60 steps with comments/blank lines.

    Each 5 N s impulse contributes ~1.2083 m of upward displacement before the
    velocity returns to zero 30 steps later. Two consecutive kicks give ~2.4167 m.
    """
    scenario = (
        "# Initial conditions\n"
        "GRAVITY 0 -10\n"
        "BODY 0 0\n"
        "MASS 1.0\n"
        "\n"
        "# Impulse schedule: kick up at step 0 and again at step 30\n"
        "IMPULSE 0 0 5\n"
        "IMPULSE 30 0 5\n"
        "\n"
        "STEPS 60\n"
    )
    path = _write_fixture(fixtures_dir, "combined.txt", scenario)
    result = _run_launcher(path)

    assert result.returncode == 0, (
        "Launcher exited non-zero on a combined gravity+impulse scenario.\n"
        f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )
    x, y = _parse_final_position(result.stdout)
    assert abs(x - 0.0) < 0.01, (
        f"Combined-scenario x coordinate should remain ~0.0, got {x}."
    )
    expected_y = 2.4167
    assert abs(y - expected_y) < 0.05, (
        "Combined-scenario y coordinate should be ~"
        f"{expected_y} after two 5 N s upward impulses under gravity (0,-10), got {y}."
    )


def test_missing_required_header_errors(fixtures_dir):
    """A scenario missing the required `GRAVITY` header must error out."""
    scenario = (
        "BODY 0 0\n"
        "STEPS 10\n"
    )
    path = _write_fixture(fixtures_dir, "missing.txt", scenario)
    result = _run_launcher(path)

    assert result.returncode != 0, (
        "Launcher must exit non-zero when the scenario is missing a required `GRAVITY` header.\n"
        f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )
    assert "Error:" in result.stderr, (
        "Launcher must report malformed scenarios via a line starting with `Error:` on stderr. "
        f"Got stderr:\n{result.stderr}"
    )
    assert "Final position:" not in result.stdout, (
        "Launcher must not print a `Final position:` line for a malformed scenario. "
        f"Got stdout:\n{result.stdout}"
    )


def test_impulse_step_out_of_range_errors(fixtures_dir):
    """An IMPULSE whose `step` is outside `[0, STEPS)` must be rejected."""
    scenario = (
        "GRAVITY 0 0\n"
        "BODY 0 0\n"
        "IMPULSE 10 1 0\n"
        "STEPS 5\n"
    )
    path = _write_fixture(fixtures_dir, "badimpulse.txt", scenario)
    result = _run_launcher(path)

    assert result.returncode != 0, (
        "Launcher must exit non-zero when an IMPULSE references a step outside [0, STEPS).\n"
        f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )
    assert "Error:" in result.stderr, (
        "Launcher must report malformed scenarios via a line starting with `Error:` on stderr. "
        f"Got stderr:\n{result.stderr}"
    )
    assert "Final position:" not in result.stdout, (
        "Launcher must not print a `Final position:` line for a malformed scenario. "
        f"Got stdout:\n{result.stdout}"
    )
