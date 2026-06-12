import os
import shutil
import subprocess

PROJECT_DIR = "/home/user/myproject"


def test_project_directory_exists():
    assert os.path.isdir(PROJECT_DIR), (
        f"Project directory {PROJECT_DIR} does not exist."
    )


def test_java_available():
    assert shutil.which("java") is not None, "java binary not found in PATH."
    result = subprocess.run(
        ["java", "-version"], capture_output=True, text=True
    )
    assert result.returncode == 0, (
        f"`java -version` failed with exit code {result.returncode}: "
        f"stdout={result.stdout!r} stderr={result.stderr!r}"
    )


def test_javac_available():
    assert shutil.which("javac") is not None, (
        "javac (JDK compiler) not found in PATH; a JDK (not just a JRE) is required."
    )


def test_gradle_available():
    assert shutil.which("gradle") is not None, (
        "gradle binary not found in PATH; required to bootstrap the libGDX project."
    )
    result = subprocess.run(
        ["gradle", "--version"], capture_output=True, text=True
    )
    assert result.returncode == 0, (
        f"`gradle --version` failed with exit code {result.returncode}: "
        f"stdout={result.stdout!r} stderr={result.stderr!r}"
    )
