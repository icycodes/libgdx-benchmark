import os
import shutil
import subprocess

import pytest

PROJECT_DIR = "/home/user/myproject"


def test_java_available():
    assert shutil.which("java") is not None, "java binary not found in PATH."


def test_gradlew_wrapper_present_and_executable():
    gradlew = os.path.join(PROJECT_DIR, "gradlew")
    assert os.path.isfile(gradlew), f"Gradle wrapper script {gradlew} is missing."
    assert os.access(gradlew, os.X_OK), f"Gradle wrapper script {gradlew} is not executable."


def test_project_directory_exists():
    assert os.path.isdir(PROJECT_DIR), f"Project directory {PROJECT_DIR} does not exist."


def test_settings_gradle_declares_subprojects():
    settings_path = os.path.join(PROJECT_DIR, "settings.gradle")
    assert os.path.isfile(settings_path), f"{settings_path} is missing."
    with open(settings_path) as f:
        content = f.read()
    assert "core" in content, "settings.gradle does not include the 'core' subproject."
    assert "headless" in content, "settings.gradle does not include the 'headless' subproject."


def test_core_subproject_exists():
    core_dir = os.path.join(PROJECT_DIR, "core")
    core_build = os.path.join(core_dir, "build.gradle")
    core_src = os.path.join(core_dir, "src", "main", "java")
    assert os.path.isdir(core_dir), f"{core_dir} subproject directory is missing."
    assert os.path.isfile(core_build), f"{core_build} is missing."
    assert os.path.isdir(core_src), f"{core_src} source root is missing."


def test_headless_subproject_exists():
    headless_dir = os.path.join(PROJECT_DIR, "headless")
    headless_build = os.path.join(headless_dir, "build.gradle")
    headless_src = os.path.join(headless_dir, "src", "main", "java")
    assert os.path.isdir(headless_dir), f"{headless_dir} subproject directory is missing."
    assert os.path.isfile(headless_build), f"{headless_build} is missing."
    assert os.path.isdir(headless_src), f"{headless_src} source root is missing."


def test_headless_launcher_mainclass_declared():
    headless_build = os.path.join(PROJECT_DIR, "headless", "build.gradle")
    with open(headless_build) as f:
        content = f.read()
    assert "com.example.game.headless.HeadlessLauncher" in content, (
        "headless/build.gradle does not declare mainClass 'com.example.game.headless.HeadlessLauncher'."
    )


def test_assets_directory_exists():
    assets_dir = os.path.join(PROJECT_DIR, "assets")
    assert os.path.isdir(assets_dir), f"{assets_dir} directory is missing."


def test_libgdx_dependency_declared():
    # The headless backend dependency is the headline integration; it MUST already be configured.
    found = False
    for sub in ("core", "headless"):
        build_path = os.path.join(PROJECT_DIR, sub, "build.gradle")
        if not os.path.isfile(build_path):
            continue
        with open(build_path) as f:
            content = f.read()
        if "com.badlogicgames.gdx" in content:
            found = True
            break
    assert found, "No libGDX (com.badlogicgames.gdx) dependency declared in core/ or headless/ build.gradle."


def test_headless_backend_dependency_declared():
    headless_build = os.path.join(PROJECT_DIR, "headless", "build.gradle")
    with open(headless_build) as f:
        content = f.read()
    assert "gdx-backend-headless" in content, (
        "headless/build.gradle does not depend on 'gdx-backend-headless'."
    )


def test_gradle_wrapper_offline_works():
    # The initial environment should already have a primed Gradle wrapper / dependency cache so
    # that the executor can build offline. A trivial 'help' task must succeed without network.
    result = subprocess.run(
        ["./gradlew", "--no-daemon", "--offline", "-q", "help"],
        cwd=PROJECT_DIR,
        capture_output=True,
        text=True,
        timeout=300,
    )
    assert result.returncode == 0, (
        f"Gradle wrapper failed to execute offline. stdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )
