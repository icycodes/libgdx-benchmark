import os
import shutil
import subprocess


PROJECT_DIR = "/home/user/match-replay-server"


def test_java_available():
    assert shutil.which("java") is not None, (
        "java binary not found in PATH; a JDK is required to build the libGDX project."
    )


def test_javac_available():
    assert shutil.which("javac") is not None, (
        "javac binary not found in PATH; a JDK (not just a JRE) is required to compile the libGDX project."
    )


def test_gradle_available():
    assert shutil.which("gradle") is not None, (
        "gradle binary not found in PATH; Gradle is required to bootstrap the libGDX multi-module project."
    )


def test_java_version_runs():
    result = subprocess.run(
        ["java", "-version"],
        capture_output=True,
        text=True,
    )
    assert result.returncode == 0, (
        "`java -version` did not return successfully; the installed JDK does not appear functional."
    )


def test_gradle_version_runs():
    result = subprocess.run(
        ["gradle", "--version"],
        capture_output=True,
        text=True,
    )
    assert result.returncode == 0, (
        "`gradle --version` did not return successfully; the installed Gradle does not appear functional."
    )


def test_project_directory_exists():
    assert os.path.isdir(PROJECT_DIR), (
        f"Expected project directory {PROJECT_DIR} to exist before the task starts."
    )


def test_home_user_writable():
    assert os.access("/home/user", os.W_OK), (
        "/home/user must be writable so the executor can scaffold the libGDX project."
    )
