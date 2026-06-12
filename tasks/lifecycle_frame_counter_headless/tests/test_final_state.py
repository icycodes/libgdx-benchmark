import os
import re
import subprocess

PROJECT_DIR = "/home/user/myproject"
RUN_SCRIPT = os.path.join(PROJECT_DIR, "run.sh")


def _grep_recursive(pattern: str, root: str) -> list[str]:
    """Return a list of files under root whose contents contain pattern."""
    matches: list[str] = []
    for dirpath, dirnames, filenames in os.walk(root):
        # Skip generated/build/cache directories to keep grep fast and focused
        dirnames[:] = [
            d
            for d in dirnames
            if d
            not in {
                ".git",
                ".gradle",
                "build",
                "out",
                "node_modules",
                ".idea",
                "target",
            }
        ]
        for name in filenames:
            full = os.path.join(dirpath, name)
            try:
                with open(full, "r", encoding="utf-8", errors="ignore") as f:
                    if pattern in f.read():
                        matches.append(full)
            except OSError:
                continue
    return matches


def _run_app(n: int) -> subprocess.CompletedProcess:
    assert os.path.isfile(RUN_SCRIPT), (
        f"run.sh script not found at {RUN_SCRIPT}; the executor must provide it."
    )
    # Ensure the script is executable; tolerate either bash invocation or +x.
    result = subprocess.run(
        ["bash", RUN_SCRIPT, str(n)],
        capture_output=True,
        text=True,
        cwd=PROJECT_DIR,
        timeout=600,
    )
    return result


def test_run_script_present():
    assert os.path.isfile(RUN_SCRIPT), (
        f"Expected run.sh at {RUN_SCRIPT} but it is missing."
    )


def test_headless_backend_declared_in_gradle():
    """The Gradle build must depend on com.badlogicgames.gdx:gdx-backend-headless:1.14.2."""
    matches = _grep_recursive("gdx-backend-headless:1.14.2", PROJECT_DIR)
    assert matches, (
        "No Gradle build file under "
        f"{PROJECT_DIR} references 'gdx-backend-headless:1.14.2'. "
        "The libGDX headless backend dependency must be declared at version 1.14.2."
    )


def test_application_uses_headless_application_class():
    """The application source must use the HeadlessApplication class."""
    matches = _grep_recursive("HeadlessApplication", PROJECT_DIR)
    assert matches, (
        "No source file under "
        f"{PROJECT_DIR} references 'HeadlessApplication'. The application "
        "must boot through com.badlogic.gdx.backends.headless.HeadlessApplication."
    )


def test_listener_calls_gdx_app_exit():
    """The lifecycle must be driven by Gdx.app.exit() from inside the listener."""
    matches = _grep_recursive("Gdx.app.exit", PROJECT_DIR)
    assert matches, (
        "No source file under "
        f"{PROJECT_DIR} references 'Gdx.app.exit'. The ApplicationListener "
        "must terminate the headless main loop via Gdx.app.exit()."
    )


def test_frame_count_five():
    result = _run_app(5)
    assert result.returncode == 0, (
        f"`bash run.sh 5` exited with code {result.returncode}.\n"
        f"stdout:\n{result.stdout}\n\nstderr:\n{result.stderr}"
    )
    lines = result.stdout.splitlines()
    matching = [ln for ln in lines if ln == "FRAME_COUNT: 5"]
    assert len(matching) == 1, (
        "Expected exactly one stdout line equal to 'FRAME_COUNT: 5' from "
        f"`bash run.sh 5`, but found {len(matching)}.\n"
        f"stdout:\n{result.stdout}\n\nstderr:\n{result.stderr}"
    )


def test_frame_count_one():
    result = _run_app(1)
    assert result.returncode == 0, (
        f"`bash run.sh 1` exited with code {result.returncode}.\n"
        f"stdout:\n{result.stdout}\n\nstderr:\n{result.stderr}"
    )
    lines = result.stdout.splitlines()
    matching = [ln for ln in lines if ln == "FRAME_COUNT: 1"]
    assert len(matching) == 1, (
        "Expected exactly one stdout line equal to 'FRAME_COUNT: 1' from "
        f"`bash run.sh 1`, but found {len(matching)}.\n"
        f"stdout:\n{result.stdout}\n\nstderr:\n{result.stderr}"
    )


def test_frame_count_seventeen():
    result = _run_app(17)
    assert result.returncode == 0, (
        f"`bash run.sh 17` exited with code {result.returncode}.\n"
        f"stdout:\n{result.stdout}\n\nstderr:\n{result.stderr}"
    )
    lines = result.stdout.splitlines()
    matching = [ln for ln in lines if ln == "FRAME_COUNT: 17"]
    assert len(matching) == 1, (
        "Expected exactly one stdout line equal to 'FRAME_COUNT: 17' from "
        f"`bash run.sh 17`, but found {len(matching)}.\n"
        f"stdout:\n{result.stdout}\n\nstderr:\n{result.stderr}"
    )


def test_no_stray_frame_count_lines():
    """Ensure FRAME_COUNT is only printed once per run, not on every render."""
    result = _run_app(3)
    assert result.returncode == 0, (
        f"`bash run.sh 3` exited with code {result.returncode}.\n"
        f"stdout:\n{result.stdout}\n\nstderr:\n{result.stderr}"
    )
    frame_count_lines = [
        ln for ln in result.stdout.splitlines() if re.match(r"^FRAME_COUNT: ", ln)
    ]
    assert len(frame_count_lines) == 1, (
        "Expected exactly one 'FRAME_COUNT: ...' line in stdout for `bash run.sh 3`, "
        f"but found {len(frame_count_lines)}: {frame_count_lines!r}\n"
        f"Full stdout:\n{result.stdout}"
    )
    assert frame_count_lines[0] == "FRAME_COUNT: 3", (
        f"Expected the single FRAME_COUNT line to be 'FRAME_COUNT: 3', "
        f"got {frame_count_lines[0]!r}.\nFull stdout:\n{result.stdout}"
    )
