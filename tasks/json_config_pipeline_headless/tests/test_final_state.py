import os
import shutil
import subprocess
import tempfile

import pytest


PROJECT_DIR = "/home/user/gdx-game"
LAUNCHER = os.path.join(PROJECT_DIR, "build", "install", "gdx-game", "bin", "gdx-game")
GRADLEW = os.path.join(PROJECT_DIR, "gradlew")
BUILD_TIMEOUT = 600  # seconds
RUN_TIMEOUT = 120  # seconds

SUMMARY_PREFIXES = (
    "Game:",
    "Levels:",
    "Enemies:",
    "Total HP:",
    "Strongest enemy:",
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
    tmp = tempfile.mkdtemp(prefix="gdx-config-")
    try:
        yield tmp
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


def _write_fixture(directory: str, name: str, content: str) -> str:
    path = os.path.join(directory, name)
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(content)
    return path


def _run_launcher(config_path: str) -> subprocess.CompletedProcess:
    return subprocess.run(
        [LAUNCHER, f"--config={config_path}"],
        capture_output=True,
        text=True,
        timeout=RUN_TIMEOUT,
    )


def _assert_summary_lines_in_order(stdout: str, expected_lines):
    """Assert that the expected_lines appear in stdout in the given order."""
    lines = stdout.splitlines()
    idx = 0
    for line in lines:
        if idx < len(expected_lines) and line.strip() == expected_lines[idx]:
            idx += 1
    assert idx == len(expected_lines), (
        f"Expected the following lines to appear in stdout in order, but got idx={idx}.\n"
        f"Expected:\n" + "\n".join(expected_lines) + f"\n\nActual stdout:\n{stdout}"
    )


def _assert_no_summary_lines(stdout: str):
    for line in stdout.splitlines():
        stripped = line.strip()
        for prefix in SUMMARY_PREFIXES:
            assert not stripped.startswith(prefix), (
                f"stdout must not contain a summary line on error, "
                f"but found {stripped!r}.\nFull stdout:\n{stdout}"
            )


def test_single_level_single_enemy(fixtures_dir):
    config = (
        "{\n"
        '  "game": "demo",\n'
        '  "levels": [\n'
        '    {"name": "Forest", "enemies": [{"type": "goblin", "hp": 10}]}\n'
        "  ]\n"
        "}\n"
    )
    path = _write_fixture(fixtures_dir, "single.json", config)
    result = _run_launcher(path)

    assert result.returncode == 0, (
        "Launcher exited non-zero on a valid single-level config.\n"
        f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )
    _assert_summary_lines_in_order(
        result.stdout,
        [
            "Game: demo",
            "Levels: 1",
            "Enemies: 1",
            "Total HP: 10",
            "Strongest enemy: goblin (10)",
        ],
    )


def test_multi_level_multi_enemy(fixtures_dir):
    config = (
        "{\n"
        '  "game": "adventure",\n'
        '  "levels": [\n'
        '    {"name": "Forest", "enemies": [\n'
        '      {"type": "goblin", "hp": 10},\n'
        '      {"type": "orc", "hp": 25}\n'
        "    ]},\n"
        '    {"name": "Cave", "enemies": [\n'
        '      {"type": "bat", "hp": 5},\n'
        '      {"type": "dragon", "hp": 100}\n'
        "    ]}\n"
        "  ]\n"
        "}\n"
    )
    path = _write_fixture(fixtures_dir, "multi.json", config)
    result = _run_launcher(path)

    assert result.returncode == 0, (
        "Launcher exited non-zero on a valid multi-level config.\n"
        f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )
    _assert_summary_lines_in_order(
        result.stdout,
        [
            "Game: adventure",
            "Levels: 2",
            "Enemies: 4",
            "Total HP: 140",
            "Strongest enemy: dragon (100)",
        ],
    )


def test_tie_break_first_wins(fixtures_dir):
    config = (
        "{\n"
        '  "game": "tie",\n'
        '  "levels": [\n'
        '    {"name": "A", "enemies": [\n'
        '      {"type": "first", "hp": 30},\n'
        '      {"type": "second", "hp": 30}\n'
        "    ]},\n"
        '    {"name": "B", "enemies": [\n'
        '      {"type": "third", "hp": 30}\n'
        "    ]}\n"
        "  ]\n"
        "}\n"
    )
    path = _write_fixture(fixtures_dir, "tie.json", config)
    result = _run_launcher(path)

    assert result.returncode == 0, (
        "Launcher exited non-zero on a valid tie-break config.\n"
        f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )
    _assert_summary_lines_in_order(
        result.stdout,
        [
            "Game: tie",
            "Levels: 2",
            "Enemies: 3",
            "Total HP: 90",
            "Strongest enemy: first (30)",
        ],
    )


def test_no_enemies(fixtures_dir):
    config = (
        "{\n"
        '  "game": "peaceful",\n'
        '  "levels": [\n'
        '    {"name": "Meadow", "enemies": []},\n'
        '    {"name": "Lake", "enemies": []}\n'
        "  ]\n"
        "}\n"
    )
    path = _write_fixture(fixtures_dir, "empty_enemies.json", config)
    result = _run_launcher(path)

    assert result.returncode == 0, (
        "Launcher exited non-zero on a valid no-enemies config.\n"
        f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )
    _assert_summary_lines_in_order(
        result.stdout,
        [
            "Game: peaceful",
            "Levels: 2",
            "Enemies: 0",
            "Total HP: 0",
            "Strongest enemy: none",
        ],
    )


def test_malformed_json_triggers_error(fixtures_dir):
    # Truncated/invalid JSON.
    path = _write_fixture(fixtures_dir, "malformed.json", '{"game": "oops",')
    result = _run_launcher(path)

    assert result.returncode != 0, (
        "Launcher must exit with a non-zero status code on malformed JSON. "
        "Got exit code 0.\n"
        f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )
    assert any(
        line.startswith("Error: invalid config:")
        for line in result.stderr.splitlines()
    ), (
        "Launcher must report malformed JSON on stderr in the format "
        "`Error: invalid config: <reason>`. Got stderr:\n"
        f"{result.stderr}"
    )
    _assert_no_summary_lines(result.stdout)


def test_missing_required_field_triggers_error(fixtures_dir):
    config = (
        "{\n"
        '  "game": "broken",\n'
        '  "levels": [\n'
        '    {"name": "NoEnemiesField"}\n'
        "  ]\n"
        "}\n"
    )
    path = _write_fixture(fixtures_dir, "missing_field.json", config)
    result = _run_launcher(path)

    assert result.returncode != 0, (
        "Launcher must exit with a non-zero status code when a required field "
        "(`enemies`) is missing from a level. Got exit code 0.\n"
        f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )
    assert any(
        line.startswith("Error: invalid config:")
        for line in result.stderr.splitlines()
    ), (
        "Launcher must report a missing required field on stderr in the format "
        "`Error: invalid config: <reason>`. Got stderr:\n"
        f"{result.stderr}"
    )
    _assert_no_summary_lines(result.stdout)
