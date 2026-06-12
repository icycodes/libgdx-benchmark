import hashlib
import json
import os
import subprocess

import pytest


PROJECT_DIR = "/home/user/match-replay-server"
GRADLEW = os.path.join(PROJECT_DIR, "gradlew")
GRADLE_RUN_TIMEOUT_SECONDS = 600


def _sha256_hex(canonical_state: str) -> str:
    return hashlib.sha256(canonical_state.encode("utf-8")).hexdigest()


def _run_server(input_path: str, output_path: str) -> subprocess.CompletedProcess:
    if os.path.exists(output_path):
        os.remove(output_path)
    cmd = [
        GRADLEW,
        "--no-daemon",
        "-q",
        ":server:run",
        f"--args={input_path} {output_path}",
    ]
    return subprocess.run(
        cmd,
        cwd=PROJECT_DIR,
        capture_output=True,
        text=True,
        timeout=GRADLE_RUN_TIMEOUT_SECONDS,
    )


def _load_transcript(output_path: str) -> dict:
    assert os.path.isfile(output_path), (
        f"Expected transcript file to be created at {output_path}."
    )
    with open(output_path, "r", encoding="utf-8") as f:
        return json.load(f)


def _sort_players(transcript: dict) -> list:
    assert "players" in transcript and isinstance(transcript["players"], list), (
        "Transcript must contain a 'players' array."
    )
    return sorted(transcript["players"], key=lambda p: p["id"])


def _assert_transcript_shape(transcript: dict) -> None:
    for key in ("world", "players", "totalTicks", "commandsApplied", "stateHash"):
        assert key in transcript, f"Transcript is missing required key '{key}'."
    assert isinstance(transcript["world"], dict), "'world' must be an object."
    assert set(transcript["world"].keys()) == {"width", "height"}, (
        f"'world' object must contain exactly 'width' and 'height'; got {sorted(transcript['world'].keys())}."
    )
    assert isinstance(transcript["players"], list), "'players' must be an array."
    for entry in transcript["players"]:
        assert set(entry.keys()) == {"id", "startX", "startY", "finalX", "finalY"}, (
            f"Each player entry must contain exactly id/startX/startY/finalX/finalY; got {sorted(entry.keys())}."
        )
    assert isinstance(transcript["totalTicks"], int), "'totalTicks' must be an integer."
    assert isinstance(transcript["commandsApplied"], int), "'commandsApplied' must be an integer."
    assert isinstance(transcript["stateHash"], str), "'stateHash' must be a string."
    assert len(transcript["stateHash"]) == 64 and all(
        c in "0123456789abcdef" for c in transcript["stateHash"]
    ), "'stateHash' must be a 64-character lowercase hexadecimal SHA-256 digest."


# ---------------------------------------------------------------------------
# Project structure / configuration checks
# ---------------------------------------------------------------------------


def test_settings_gradle_declares_modules():
    settings_path = os.path.join(PROJECT_DIR, "settings.gradle")
    settings_kts_path = os.path.join(PROJECT_DIR, "settings.gradle.kts")
    content = ""
    if os.path.isfile(settings_path):
        with open(settings_path, "r", encoding="utf-8") as f:
            content = f.read()
    elif os.path.isfile(settings_kts_path):
        with open(settings_kts_path, "r", encoding="utf-8") as f:
            content = f.read()
    else:
        pytest.fail(
            "Expected a settings.gradle or settings.gradle.kts at the project root."
        )
    assert "core" in content, "settings.gradle must include the ':core' module."
    assert "server" in content, "settings.gradle must include the ':server' module."


def test_gradle_wrapper_executable():
    assert os.path.isfile(GRADLEW), (
        f"Gradle wrapper script not found at {GRADLEW}; the project must include a wrapper."
    )
    assert os.access(GRADLEW, os.X_OK), (
        f"Gradle wrapper at {GRADLEW} is not executable."
    )


def test_server_runtime_classpath_uses_headless_backend():
    result = subprocess.run(
        [
            GRADLEW,
            "--no-daemon",
            "-q",
            ":server:dependencies",
            "--configuration",
            "runtimeClasspath",
        ],
        cwd=PROJECT_DIR,
        capture_output=True,
        text=True,
        timeout=GRADLE_RUN_TIMEOUT_SECONDS,
    )
    assert result.returncode == 0, (
        f"`gradlew :server:dependencies` failed:\nstdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )
    combined = result.stdout + result.stderr
    assert "gdx-backend-headless" in combined, (
        "The server runtime classpath must include `gdx-backend-headless`."
    )
    assert "gdx-platform" in combined and "natives-desktop" in combined, (
        "The server runtime classpath must include `gdx-platform` with the natives-desktop classifier."
    )
    assert "gdx-backend-lwjgl3" not in combined, (
        "The server module must not depend on `gdx-backend-lwjgl3`."
    )


# ---------------------------------------------------------------------------
# Test Case A — two players, several ticks
# ---------------------------------------------------------------------------


MATCH_A = """# Match A — two players on an 8x8 grid
WORLD 8 8
START 1 0 0
START 2 7 7
0 1 MOVE_RIGHT
0 2 MOVE_LEFT
1 1 MOVE_UP
1 2 MOVE_DOWN
2 1 MOVE_RIGHT
2 2 MOVE_LEFT
3 1 NOOP
3 2 MOVE_DOWN
"""


def test_case_a_two_players_multiple_ticks(tmp_path):
    input_path = str(tmp_path / "match_a.log")
    output_path = str(tmp_path / "transcript_a.json")
    with open(input_path, "w", encoding="utf-8", newline="\n") as f:
        f.write(MATCH_A)

    result = _run_server(input_path, output_path)
    assert result.returncode == 0, (
        f"Server invocation for Match A failed.\nstdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )
    transcript = _load_transcript(output_path)
    _assert_transcript_shape(transcript)

    assert transcript["world"] == {"width": 8, "height": 8}, (
        f"world should be 8x8 for Match A; got {transcript['world']}."
    )
    assert transcript["totalTicks"] == 4, (
        f"totalTicks should be 4 for Match A; got {transcript['totalTicks']}."
    )
    assert transcript["commandsApplied"] == 8, (
        f"commandsApplied should be 8 for Match A; got {transcript['commandsApplied']}."
    )
    players_sorted = _sort_players(transcript)
    assert players_sorted == [
        {"id": 1, "startX": 0, "startY": 0, "finalX": 2, "finalY": 1},
        {"id": 2, "startX": 7, "startY": 7, "finalX": 5, "finalY": 5},
    ], f"Match A players do not match expected end state; got {players_sorted}."

    expected_hash = _sha256_hex("W=8;H=8;P=1:2,1|2:5,5")
    assert transcript["stateHash"] == expected_hash, (
        f"Match A stateHash mismatch.\nExpected: {expected_hash}\nGot:      {transcript['stateHash']}"
    )


# ---------------------------------------------------------------------------
# Test Case B — clamping + undeclared player
# ---------------------------------------------------------------------------


MATCH_B = """# Match B — clamping + a stray command for an undeclared player
WORLD 3 3
START 1 0 0
0 1 MOVE_LEFT
0 1 MOVE_DOWN
1 1 MOVE_RIGHT
1 2 MOVE_UP
2 1 MOVE_UP
"""


def test_case_b_clamping_and_unknown_player(tmp_path):
    input_path = str(tmp_path / "match_b.log")
    output_path = str(tmp_path / "transcript_b.json")
    with open(input_path, "w", encoding="utf-8", newline="\n") as f:
        f.write(MATCH_B)

    result = _run_server(input_path, output_path)
    assert result.returncode == 0, (
        f"Server invocation for Match B failed.\nstdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )
    transcript = _load_transcript(output_path)
    _assert_transcript_shape(transcript)

    assert transcript["world"] == {"width": 3, "height": 3}, (
        f"world should be 3x3 for Match B; got {transcript['world']}."
    )
    assert transcript["totalTicks"] == 3, (
        f"totalTicks should be 3 for Match B; got {transcript['totalTicks']}."
    )
    assert transcript["commandsApplied"] == 4, (
        "commandsApplied should be 4 for Match B (the stray player-2 command must be ignored); "
        f"got {transcript['commandsApplied']}."
    )
    players_sorted = _sort_players(transcript)
    assert players_sorted == [
        {"id": 1, "startX": 0, "startY": 0, "finalX": 1, "finalY": 1},
    ], f"Match B players do not match expected end state; got {players_sorted}."

    expected_hash = _sha256_hex("W=3;H=3;P=1:1,1")
    assert transcript["stateHash"] == expected_hash, (
        f"Match B stateHash mismatch.\nExpected: {expected_hash}\nGot:      {transcript['stateHash']}"
    )


# ---------------------------------------------------------------------------
# Test Case C — determinism + no commands
# ---------------------------------------------------------------------------


MATCH_C = """# Match C — declares players but issues no commands
WORLD 4 4
START 1 2 3
START 2 3 0
START 3 0 0
"""


def test_case_c_no_commands_is_deterministic(tmp_path):
    input_path = str(tmp_path / "match_c.log")
    output_path = str(tmp_path / "transcript_c.json")
    with open(input_path, "w", encoding="utf-8", newline="\n") as f:
        f.write(MATCH_C)

    # First run.
    result1 = _run_server(input_path, output_path)
    assert result1.returncode == 0, (
        f"First Match C invocation failed.\nstdout:\n{result1.stdout}\nstderr:\n{result1.stderr}"
    )
    transcript1 = _load_transcript(output_path)
    _assert_transcript_shape(transcript1)

    assert transcript1["world"] == {"width": 4, "height": 4}, (
        f"world should be 4x4 for Match C; got {transcript1['world']}."
    )
    assert transcript1["totalTicks"] == 0, (
        f"totalTicks should be 0 for Match C; got {transcript1['totalTicks']}."
    )
    assert transcript1["commandsApplied"] == 0, (
        f"commandsApplied should be 0 for Match C; got {transcript1['commandsApplied']}."
    )
    players_sorted = _sort_players(transcript1)
    assert players_sorted == [
        {"id": 1, "startX": 2, "startY": 3, "finalX": 2, "finalY": 3},
        {"id": 2, "startX": 3, "startY": 0, "finalX": 3, "finalY": 0},
        {"id": 3, "startX": 0, "startY": 0, "finalX": 0, "finalY": 0},
    ], f"Match C players do not match expected end state; got {players_sorted}."

    expected_hash = _sha256_hex("W=4;H=4;P=1:2,3|2:3,0|3:0,0")
    assert transcript1["stateHash"] == expected_hash, (
        f"Match C stateHash mismatch.\nExpected: {expected_hash}\nGot:      {transcript1['stateHash']}"
    )

    # Second run must be deterministic.
    result2 = _run_server(input_path, output_path)
    assert result2.returncode == 0, (
        f"Second Match C invocation failed.\nstdout:\n{result2.stdout}\nstderr:\n{result2.stderr}"
    )
    transcript2 = _load_transcript(output_path)
    _assert_transcript_shape(transcript2)
    assert transcript2["stateHash"] == transcript1["stateHash"], (
        "Match C is non-deterministic: stateHash differed between runs."
    )
    assert _sort_players(transcript2) == _sort_players(transcript1), (
        "Match C is non-deterministic: players array differed between runs."
    )
