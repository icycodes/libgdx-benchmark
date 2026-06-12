import os
import shutil
import subprocess


PROJECT_DIR = "/home/user/myproject"


def test_java_available():
    assert shutil.which("java") is not None, (
        "java binary not found in PATH; a JDK is required to build and run the libGDX project."
    )


def test_javac_available():
    assert shutil.which("javac") is not None, (
        "javac binary not found in PATH; a JDK is required to compile the libGDX project."
    )


def test_project_directory_exists():
    assert os.path.isdir(PROJECT_DIR), (
        f"Project directory {PROJECT_DIR} does not exist."
    )


def test_gradle_wrapper_present():
    wrapper = os.path.join(PROJECT_DIR, "gradlew")
    assert os.path.isfile(wrapper), (
        f"Gradle wrapper {wrapper} is missing; the project must include the Gradle wrapper."
    )
    assert os.access(wrapper, os.X_OK), (
        f"Gradle wrapper {wrapper} is not executable."
    )


def test_gradle_wrapper_runs():
    wrapper = os.path.join(PROJECT_DIR, "gradlew")
    result = subprocess.run(
        [wrapper, "--version"],
        cwd=PROJECT_DIR,
        capture_output=True,
        text=True,
        timeout=180,
    )
    assert result.returncode == 0, (
        f"./gradlew --version failed with return code {result.returncode}: "
        f"stdout={result.stdout!r} stderr={result.stderr!r}"
    )
    assert "Gradle" in result.stdout, (
        f"./gradlew --version output does not look like Gradle: {result.stdout!r}"
    )
