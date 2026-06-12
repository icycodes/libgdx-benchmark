import os
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET

import pytest


PROJECT_DIR = "/home/user/highscores"
PREFS_DIR = os.path.join(PROJECT_DIR, "prefs")
PREFS_FILE = os.path.join(PREFS_DIR, "arcade_scores")
TRANSCRIPT_FILE = os.path.join(PROJECT_DIR, "transcript.log")
EVENTS_FILE = os.path.join(PROJECT_DIR, "events.txt")
GRADLE_CMD = [
    "./gradlew",
    "--no-daemon",
    "--offline",
    "headless:run",
    f"--args={EVENTS_FILE}",
]

EXPECTED_TRANSCRIPT = (
    "UPDATE alice NEW -> 42\n"
    "UPDATE bob NEW -> 30\n"
    "KEEP alice 42 >= 10\n"
    "UPDATE bob 30 -> 75\n"
    "DUMP 2\n"
    "  alice 42\n"
    "  bob 75\n"
    "FLUSH ok\n"
    "UPDATE carol NEW -> 50\n"
    "UPDATE alice 42 -> 90\n"
    "RESET bob was 75\n"
    "RESET dave missing\n"
    "DUMP 2\n"
    "  alice 90\n"
    "  carol 50\n"
)

EXPECTED_PREFS = {"alice": "90", "carol": "50"}

EVENTS_CONTENT = (
    "# Round 1\n"
    "SCORE alice 42\n"
    "SCORE bob 30\n"
    "SCORE alice 10\n"
    "SCORE bob 75\n"
    "DUMP\n"
    "FLUSH\n"
    "\n"
    "# Round 2\n"
    "SCORE carol 50\n"
    "SCORE alice 90\n"
    "RESET bob\n"
    "RESET dave\n"
    "DUMP\n"
)


def _clean_artifacts() -> None:
    if os.path.isdir(PREFS_DIR):
        shutil.rmtree(PREFS_DIR)
    if os.path.isfile(TRANSCRIPT_FILE):
        os.remove(TRANSCRIPT_FILE)


def _write_events_file() -> None:
    with open(EVENTS_FILE, "w", encoding="utf-8") as fh:
        fh.write(EVENTS_CONTENT)


def _run_gradle() -> subprocess.CompletedProcess:
    return subprocess.run(
        GRADLE_CMD,
        cwd=PROJECT_DIR,
        capture_output=True,
        text=True,
        timeout=900,
    )


@pytest.fixture(scope="module")
def first_run():
    """Cleanup artifacts, materialize the canonical events file, and run the project once."""
    _clean_artifacts()
    _write_events_file()
    result = _run_gradle()
    return result


def _normalize_transcript(raw: str) -> str:
    # Allow exactly one trailing newline to be optional.
    if raw.endswith("\n"):
        return raw
    return raw + "\n"


def test_first_run_exits_clean(first_run):
    assert first_run.returncode == 0, (
        f"Initial `gradlew headless:run` failed (rc={first_run.returncode}).\n"
        f"STDOUT:\n{first_run.stdout}\nSTDERR:\n{first_run.stderr}"
    )


def test_transcript_exists(first_run):
    assert os.path.isfile(TRANSCRIPT_FILE), (
        f"Expected transcript file at {TRANSCRIPT_FILE} but it was not created."
    )


def test_transcript_matches_expected(first_run):
    with open(TRANSCRIPT_FILE, "r", encoding="utf-8") as fh:
        actual = fh.read()
    # Permit a single trailing newline difference.
    normalized = _normalize_transcript(actual)
    assert normalized == EXPECTED_TRANSCRIPT, (
        "Transcript content does not match expected sequence.\n"
        f"--- expected ---\n{EXPECTED_TRANSCRIPT}\n--- actual ---\n{actual}"
    )


def test_prefs_file_exists(first_run):
    assert os.path.isfile(PREFS_FILE), (
        f"Expected libGDX Preferences XML at {PREFS_FILE} but it was not created."
    )


def test_prefs_file_is_valid_properties_xml(first_run):
    with open(PREFS_FILE, "r", encoding="utf-8") as fh:
        raw = fh.read()
    assert "<!DOCTYPE properties" in raw, (
        f"Preferences file at {PREFS_FILE} is missing the java.util.Properties DOCTYPE.\n"
        f"Content begins with: {raw[:200]!r}"
    )
    try:
        root = ET.fromstring(raw)
    except ET.ParseError as exc:
        raise AssertionError(
            f"Preferences file at {PREFS_FILE} is not parseable XML: {exc}"
        )
    assert root.tag == "properties", (
        f"Expected root element <properties>, got <{root.tag}> in {PREFS_FILE}."
    )


def test_prefs_contents_match_expected(first_run):
    with open(PREFS_FILE, "r", encoding="utf-8") as fh:
        root = ET.fromstring(fh.read())
    entries = {}
    for entry in root.findall("entry"):
        key = entry.get("key")
        assert key is not None, (
            f"Preferences entry without a key attribute found in {PREFS_FILE}."
        )
        entries[key] = (entry.text or "").strip()
    assert entries == EXPECTED_PREFS, (
        "Final libGDX preferences contents do not match expected high-score state.\n"
        f"expected={EXPECTED_PREFS}\nactual={entries}"
    )


def _read_java_sources(root_dir: str) -> str:
    chunks = []
    for dirpath, _dirnames, filenames in os.walk(root_dir):
        for name in filenames:
            if name.endswith(".java"):
                path = os.path.join(dirpath, name)
                try:
                    with open(path, "r", encoding="utf-8") as fh:
                        chunks.append(fh.read())
                except OSError:
                    continue
    return "\n".join(chunks)


def test_headless_launcher_uses_required_configuration(first_run):
    headless_dir = os.path.join(PROJECT_DIR, "headless")
    assert os.path.isdir(headless_dir), (
        f"Expected headless launcher module under {headless_dir}."
    )
    source = _read_java_sources(headless_dir)
    assert "HeadlessApplicationConfiguration" in source, (
        "Headless launcher source does not reference HeadlessApplicationConfiguration."
    )
    # preferencesDirectory must be set to the required absolute path.
    prefs_dir_pattern = re.compile(
        r"preferencesDirectory\s*=\s*\"/home/user/highscores/prefs\"?"
    )
    assert prefs_dir_pattern.search(source) is not None, (
        "Headless launcher does not configure preferencesDirectory to "
        "\"/home/user/highscores/prefs\"."
    )
    # updatesPerSecond must be set to 0.
    ups_pattern = re.compile(r"updatesPerSecond\s*=\s*0\b")
    assert ups_pattern.search(source) is not None, (
        "Headless launcher does not configure updatesPerSecond to 0."
    )


def test_build_scripts_reference_required_dependencies(first_run):
    candidates = [
        os.path.join(PROJECT_DIR, "build.gradle"),
        os.path.join(PROJECT_DIR, "headless", "build.gradle"),
        os.path.join(PROJECT_DIR, "core", "build.gradle"),
        os.path.join(PROJECT_DIR, "build.gradle.kts"),
        os.path.join(PROJECT_DIR, "headless", "build.gradle.kts"),
        os.path.join(PROJECT_DIR, "core", "build.gradle.kts"),
    ]
    combined = ""
    for path in candidates:
        if os.path.isfile(path):
            with open(path, "r", encoding="utf-8") as fh:
                combined += fh.read() + "\n"
    assert "com.badlogicgames.gdx:gdx:1.14.2" in combined, (
        "Build scripts do not declare a dependency on com.badlogicgames.gdx:gdx:1.14.2."
    )
    assert "com.badlogicgames.gdx:gdx-backend-headless:1.14.2" in combined, (
        "Build scripts do not declare a dependency on "
        "com.badlogicgames.gdx:gdx-backend-headless:1.14.2."
    )


def test_second_run_is_deterministic(first_run):
    """Re-running the same command must produce the identical transcript and preferences."""
    _clean_artifacts()
    _write_events_file()
    second = _run_gradle()
    assert second.returncode == 0, (
        f"Second `gradlew headless:run` failed (rc={second.returncode}).\n"
        f"STDOUT:\n{second.stdout}\nSTDERR:\n{second.stderr}"
    )
    with open(TRANSCRIPT_FILE, "r", encoding="utf-8") as fh:
        actual = _normalize_transcript(fh.read())
    assert actual == EXPECTED_TRANSCRIPT, (
        "Second-run transcript differs from the first run; the project is not deterministic."
    )
    with open(PREFS_FILE, "r", encoding="utf-8") as fh:
        root = ET.fromstring(fh.read())
    entries = {
        entry.get("key"): (entry.text or "").strip()
        for entry in root.findall("entry")
    }
    assert entries == EXPECTED_PREFS, (
        "Second-run preferences differ from the first run.\n"
        f"expected={EXPECTED_PREFS}\nactual={entries}"
    )
