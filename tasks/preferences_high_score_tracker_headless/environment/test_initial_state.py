import os
import shutil
import subprocess

PROJECT_DIR = "/home/user/highscores"
GRADLEW = os.path.join(PROJECT_DIR, "gradlew")
EVENTS_FILE = os.path.join(PROJECT_DIR, "events.txt")


def test_java_available():
    """The JDK must be available in PATH because libGDX runs on the JVM."""
    assert shutil.which("java") is not None, (
        "java binary not found in PATH; a JDK is required to build and run libGDX."
    )


def test_java_can_report_version():
    """`java -version` must succeed so the executor can run Gradle builds."""
    result = subprocess.run(
        ["java", "-version"],
        capture_output=True,
        text=True,
    )
    assert result.returncode == 0, (
        f"`java -version` failed with code {result.returncode}: "
        f"stdout={result.stdout!r} stderr={result.stderr!r}"
    )


def test_project_dir_exists():
    """The task explicitly names /home/user/highscores as the project path."""
    assert os.path.isdir(PROJECT_DIR), (
        f"Expected project directory at {PROJECT_DIR}, but it is missing."
    )


def test_gradle_wrapper_present():
    """The task requires running via the bundled Gradle wrapper."""
    assert os.path.isfile(GRADLEW), (
        f"Expected Gradle wrapper script at {GRADLEW}, but it is missing."
    )
    assert os.access(GRADLEW, os.X_OK), (
        f"Gradle wrapper at {GRADLEW} is not executable."
    )


def test_events_file_present():
    """The verifier drives the project using /home/user/highscores/events.txt."""
    assert os.path.isfile(EVENTS_FILE), (
        f"Expected events input file at {EVENTS_FILE}, but it is missing."
    )
