import os
import re
import subprocess
import textwrap

import pytest

PROJECT_DIR = "/home/user/myproject"
GRADLEW = os.path.join(PROJECT_DIR, "gradlew")
OUTPUT_PATH = "/tmp/animation_output.txt"

RUN_TIMEOUT_SECONDS = 600


def _run_gradle(args, timeout=RUN_TIMEOUT_SECONDS):
    """Invoke the project's gradle wrapper (or system gradle) with --no-daemon --offline."""
    if os.path.isfile(GRADLEW) and os.access(GRADLEW, os.X_OK):
        cmd = [GRADLEW]
    else:
        cmd = ["gradle"]
    cmd += ["--no-daemon", "--offline"] + list(args)
    env = os.environ.copy()
    env.setdefault("GRADLE_OPTS", "-Dorg.gradle.daemon=false")
    return subprocess.run(
        cmd,
        cwd=PROJECT_DIR,
        capture_output=True,
        text=True,
        timeout=timeout,
        env=env,
    )


def _write(path, content):
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)


def _read(path):
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


def _cleanup_output():
    if os.path.exists(OUTPUT_PATH):
        os.remove(OUTPUT_PATH)


def _run_sampler(config_path, output_path=OUTPUT_PATH):
    """Run the sampler `run` task with the given config and output paths."""
    return _run_gradle(["run", f"--args={config_path} {output_path}"])


@pytest.fixture(scope="module", autouse=True)
def _ensure_offline_build_works():
    """Sanity-check that the project compiles before running scenarios."""
    result = _run_gradle(["build", "-x", "test"])
    assert result.returncode == 0, (
        "`./gradlew --no-daemon --offline build` failed.\n"
        f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )


def test_project_declares_libgdx_dependencies():
    """The Gradle build scripts must declare libGDX 1.14.2 (core, headless, natives-desktop)."""
    build_files = []
    for root, _dirs, files in os.walk(PROJECT_DIR):
        # Skip gradle internal directories
        if any(seg in root for seg in (os.sep + ".gradle", os.sep + "build", os.sep + ".idea")):
            continue
        for name in files:
            if name in ("build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts"):
                build_files.append(os.path.join(root, name))
    assert build_files, f"No Gradle build scripts were found under {PROJECT_DIR}."

    combined = "\n".join(_read(p) for p in build_files)
    assert re.search(r"com\.badlogicgames\.gdx[:\"']?\s*[:\"']?gdx[:\"']", combined), (
        "Gradle build scripts do not reference the `com.badlogicgames.gdx:gdx` artifact."
    )
    assert "gdx-backend-headless" in combined, (
        "Gradle build scripts do not reference `gdx-backend-headless`."
    )
    assert "1.14.2" in combined, (
        "Gradle build scripts do not pin libGDX to version 1.14.2."
    )
    assert "natives-desktop" in combined, (
        "Gradle build scripts do not reference the `natives-desktop` classifier from gdx-platform."
    )


def test_source_uses_headless_application_and_animation():
    """Java sources must use HeadlessApplication and Animation."""
    hits = {"HeadlessApplication": False, "Animation": False}
    for root, _dirs, files in os.walk(PROJECT_DIR):
        if any(seg in root for seg in (os.sep + ".gradle", os.sep + "build", os.sep + ".idea")):
            continue
        for name in files:
            if not name.endswith(".java"):
                continue
            content = _read(os.path.join(root, name))
            if "com.badlogic.gdx.backends.headless.HeadlessApplication" in content:
                hits["HeadlessApplication"] = True
            if "com.badlogic.gdx.graphics.g2d.Animation" in content:
                hits["Animation"] = True
    missing = [k for k, v in hits.items() if not v]
    assert not missing, (
        f"Java sources do not reference required libGDX classes: {missing}"
    )


def test_loop_play_mode():
    _cleanup_output()
    cfg = "/tmp/cfg_loop.txt"
    _write(
        cfg,
        textwrap.dedent(
            """\
            frameDuration 0.5
            playMode LOOP
            keyFrames 100 200 300
            sample 0.0
            sample 0.7
            sample 1.4
            sample 1.5
            sample 2.0
            """
        ),
    )
    result = _run_sampler(cfg)
    assert result.returncode == 0, (
        f"Sampler failed for LOOP scenario.\nstdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )
    expected = (
        "0.0 100 false\n"
        "0.7 200 false\n"
        "1.4 300 false\n"
        "1.5 100 true\n"
        "2.0 200 true\n"
    )
    actual = _read(OUTPUT_PATH)
    assert actual == expected, (
        f"LOOP output mismatch.\nExpected:\n{expected!r}\nActual:\n{actual!r}"
    )


def test_normal_play_mode_clamps_to_last_frame():
    _cleanup_output()
    cfg = "/tmp/cfg_normal.txt"
    _write(
        cfg,
        textwrap.dedent(
            """\
            frameDuration 0.25
            playMode NORMAL
            keyFrames 7 8 9 10
            sample 0.0
            sample 0.4
            sample 0.9
            sample 5.0
            """
        ),
    )
    result = _run_sampler(cfg)
    assert result.returncode == 0, (
        f"Sampler failed for NORMAL scenario.\nstdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )
    expected = (
        "0.0 7 false\n"
        "0.4 8 false\n"
        "0.9 10 false\n"
        "5.0 10 true\n"
    )
    actual = _read(OUTPUT_PATH)
    assert actual == expected, (
        f"NORMAL output mismatch.\nExpected:\n{expected!r}\nActual:\n{actual!r}"
    )


def test_reversed_play_mode():
    _cleanup_output()
    cfg = "/tmp/cfg_rev.txt"
    _write(
        cfg,
        textwrap.dedent(
            """\
            frameDuration 1.0
            playMode REVERSED
            keyFrames 1 2 3
            sample 0.0
            sample 1.0
            sample 2.5
            sample 4.0
            """
        ),
    )
    result = _run_sampler(cfg)
    assert result.returncode == 0, (
        f"Sampler failed for REVERSED scenario.\nstdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )
    expected = (
        "0.0 3 false\n"
        "1.0 2 false\n"
        "2.5 1 false\n"
        "4.0 1 true\n"
    )
    actual = _read(OUTPUT_PATH)
    assert actual == expected, (
        f"REVERSED output mismatch.\nExpected:\n{expected!r}\nActual:\n{actual!r}"
    )


def test_loop_pingpong_play_mode():
    _cleanup_output()
    cfg = "/tmp/cfg_pp.txt"
    _write(
        cfg,
        textwrap.dedent(
            """\
            frameDuration 1.0
            playMode LOOP_PINGPONG
            keyFrames 10 20 30
            sample 0.0
            sample 1.0
            sample 2.0
            sample 3.0
            sample 4.0
            """
        ),
    )
    result = _run_sampler(cfg)
    assert result.returncode == 0, (
        f"Sampler failed for LOOP_PINGPONG scenario.\nstdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )
    expected = (
        "0.0 10 false\n"
        "1.0 20 false\n"
        "2.0 30 false\n"
        "3.0 20 true\n"
        "4.0 10 true\n"
    )
    actual = _read(OUTPUT_PATH)
    assert actual == expected, (
        f"LOOP_PINGPONG output mismatch.\nExpected:\n{expected!r}\nActual:\n{actual!r}"
    )


def test_loop_reversed_play_mode_and_overwrites_output():
    # Pre-populate so we can verify overwrite-not-append semantics.
    _write(OUTPUT_PATH, "STALE\n")
    cfg = "/tmp/cfg_lr.txt"
    _write(
        cfg,
        textwrap.dedent(
            """\
            # play backwards forever
            frameDuration 0.5
            playMode LOOP_REVERSED
            keyFrames 1 2 3 4
            sample 0.0
            sample 0.6
            sample 1.1
            sample 1.6
            sample 2.1
            """
        ),
    )
    result = _run_sampler(cfg)
    assert result.returncode == 0, (
        f"Sampler failed for LOOP_REVERSED scenario.\nstdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )
    expected = (
        "0.0 4 false\n"
        "0.6 3 false\n"
        "1.1 2 false\n"
        "1.6 1 true\n"
        "2.1 4 true\n"
    )
    actual = _read(OUTPUT_PATH)
    assert "STALE" not in actual, (
        "Output file still contains stale content; the sampler must overwrite the file rather than append."
    )
    assert actual == expected, (
        f"LOOP_REVERSED output mismatch.\nExpected:\n{expected!r}\nActual:\n{actual!r}"
    )


def test_empty_sample_list_produces_empty_output_file():
    _cleanup_output()
    cfg = "/tmp/cfg_empty.txt"
    _write(
        cfg,
        textwrap.dedent(
            """\
            frameDuration 0.1
            playMode LOOP
            keyFrames 5 6
            """
        ),
    )
    result = _run_sampler(cfg)
    assert result.returncode == 0, (
        f"Sampler failed for empty-sample scenario.\nstdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )
    assert os.path.isfile(OUTPUT_PATH), (
        f"Expected output file {OUTPUT_PATH} to be created (even when empty)."
    )
    actual = _read(OUTPUT_PATH)
    assert actual == "", (
        f"Expected empty output file for a config without `sample` lines, got: {actual!r}"
    )
