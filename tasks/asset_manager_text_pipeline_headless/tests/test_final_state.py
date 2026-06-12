import os
import re
import subprocess

import pytest

PROJECT_DIR = "/home/user/myproject"
VERIFY_DIR = os.path.join(PROJECT_DIR, "verify")
LEVELS_DIR = os.path.join(VERIFY_DIR, "levels")

LEVEL_A = """name=Forest
enemies=4
difficulty=1
"""

LEVEL_B = """# boss stage
difficulty=5
enemies=12
name=Castle
"""

LEVEL_C = """name=  Cave 
enemies = 7
difficulty=3
"""

MANIFEST_THREE = """# demo manifest
verify/levels/level_a.txt

verify/levels/level_b.txt
verify/levels/level_c.txt
"""

MANIFEST_EMPTY = """# nothing here

"""


def _run_gradle(args_value: str) -> subprocess.CompletedProcess:
    return subprocess.run(
        ["./gradlew", "--no-daemon", "--offline", "-q", "headless:run", f"--args={args_value}"],
        cwd=PROJECT_DIR,
        capture_output=True,
        text=True,
        timeout=600,
    )


def _extract_payload_lines(stdout: str) -> list[str]:
    """Return only the lines emitted by the application (filter Gradle noise)."""
    keep_prefixes = ("LOADED ", "TOTAL_LEVELS=", "TOTAL_ENEMIES=", "PROGRESS=", "DONE")
    out: list[str] = []
    for raw in stdout.splitlines():
        line = raw.rstrip()
        if not line:
            continue
        if line.startswith(keep_prefixes) or line == "DONE":
            out.append(line)
    return out


@pytest.fixture(scope="module", autouse=True)
def _ensure_fixtures():
    os.makedirs(LEVELS_DIR, exist_ok=True)
    with open(os.path.join(LEVELS_DIR, "level_a.txt"), "w") as f:
        f.write(LEVEL_A)
    with open(os.path.join(LEVELS_DIR, "level_b.txt"), "w") as f:
        f.write(LEVEL_B)
    with open(os.path.join(LEVELS_DIR, "level_c.txt"), "w") as f:
        f.write(LEVEL_C)
    with open(os.path.join(VERIFY_DIR, "manifest.txt"), "w") as f:
        f.write(MANIFEST_THREE)
    with open(os.path.join(VERIFY_DIR, "empty.txt"), "w") as f:
        f.write(MANIFEST_EMPTY)
    yield


def test_build_succeeds():
    result = subprocess.run(
        ["./gradlew", "--no-daemon", "--offline", "-q", "build"],
        cwd=PROJECT_DIR,
        capture_output=True,
        text=True,
        timeout=900,
    )
    assert result.returncode == 0, (
        f"`gradlew build` failed.\nstdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )


def test_three_level_manifest_outputs_summary_in_order():
    result = _run_gradle("verify/manifest.txt")
    assert result.returncode == 0, (
        f"Headless run exited with code {result.returncode}.\nstdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )
    lines = _extract_payload_lines(result.stdout)
    expected = [
        "LOADED Forest enemies=4 difficulty=1",
        "LOADED Castle enemies=12 difficulty=5",
        "LOADED Cave enemies=7 difficulty=3",
        "TOTAL_LEVELS=3",
        "TOTAL_ENEMIES=23",
        "PROGRESS=1.00",
        "DONE",
    ]
    assert lines == expected, (
        f"Unexpected output for happy-path manifest.\nExpected:\n{expected}\nGot:\n{lines}\nFull stdout:\n{result.stdout}"
    )


def test_empty_manifest_outputs_zero_summary():
    result = _run_gradle("verify/empty.txt")
    assert result.returncode == 0, (
        f"Headless run with empty manifest exited with code {result.returncode}.\nstdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )
    lines = _extract_payload_lines(result.stdout)
    expected = [
        "TOTAL_LEVELS=0",
        "TOTAL_ENEMIES=0",
        "PROGRESS=0.00",
        "DONE",
    ]
    assert lines == expected, (
        f"Unexpected output for empty manifest.\nExpected:\n{expected}\nGot:\n{lines}\nFull stdout:\n{result.stdout}"
    )


def _grep_sources(pattern: str) -> list[str]:
    roots = [
        os.path.join(PROJECT_DIR, "core", "src"),
        os.path.join(PROJECT_DIR, "headless", "src"),
    ]
    hits: list[str] = []
    regex = re.compile(pattern)
    for root in roots:
        if not os.path.isdir(root):
            continue
        for dirpath, _dirnames, filenames in os.walk(root):
            for name in filenames:
                if not name.endswith(".java"):
                    continue
                full = os.path.join(dirpath, name)
                try:
                    with open(full, "r", encoding="utf-8", errors="ignore") as f:
                        content = f.read()
                except OSError:
                    continue
                if regex.search(content):
                    hits.append(full)
    return hits


def test_uses_asynchronous_asset_loader():
    hits = _grep_sources(r"AsynchronousAssetLoader")
    assert hits, (
        "No source file references `AsynchronousAssetLoader`. The pipeline must use a custom "
        "AsynchronousAssetLoader subclass registered with AssetManager."
    )


def test_registers_custom_loader_with_asset_manager():
    hits = _grep_sources(r"\.setLoader\s*\(")
    assert hits, (
        "No source file calls `AssetManager.setLoader(...)`. The custom loader must be "
        "registered with the AssetManager."
    )


def test_does_not_block_with_finish_loading():
    hits = _grep_sources(r"finishLoading\s*\(")
    assert not hits, (
        f"Found `finishLoading()` usage in {hits}. The pipeline must drive AssetManager.update() "
        "from the render loop instead of blocking."
    )


def test_uses_headless_application():
    hits = _grep_sources(
        r"com\.badlogic\.gdx\.backends\.headless\.HeadlessApplication"
    )
    assert hits, (
        "No source file imports `com.badlogic.gdx.backends.headless.HeadlessApplication`. "
        "The launcher must boot the headless backend."
    )


def test_does_not_use_lwjgl3_or_gl_classes():
    bad_hits = _grep_sources(
        r"com\.badlogic\.gdx\.backends\.lwjgl3\.|com\.badlogic\.gdx\.graphics\.glutils"
    )
    assert not bad_hits, (
        f"Sources reference LWJGL3 or GL utility classes ({bad_hits}); the task must run "
        "headless without any OpenGL dependency."
    )
