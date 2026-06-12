import os
import shutil
import subprocess


HOME_DIR = "/home/user"


def test_java_available():
    java_bin = shutil.which("java")
    assert java_bin is not None, "java binary not found in PATH (JDK is required to build libGDX projects)."
    result = subprocess.run(["java", "-version"], capture_output=True, text=True)
    assert result.returncode == 0, (
        f"`java -version` failed with exit code {result.returncode}. "
        f"stderr: {result.stderr}"
    )


def test_javac_available():
    javac_bin = shutil.which("javac")
    assert javac_bin is not None, "javac binary not found in PATH (JDK is required to build libGDX projects)."
    result = subprocess.run(["javac", "-version"], capture_output=True, text=True)
    assert result.returncode == 0, (
        f"`javac -version` failed with exit code {result.returncode}. "
        f"stderr: {result.stderr}"
    )


def test_gradle_available():
    gradle_bin = shutil.which("gradle")
    assert gradle_bin is not None, (
        "gradle binary not found in PATH (required to bootstrap the Gradle wrapper non-interactively)."
    )
    result = subprocess.run(["gradle", "--version"], capture_output=True, text=True)
    assert result.returncode == 0, (
        f"`gradle --version` failed with exit code {result.returncode}. "
        f"stderr: {result.stderr}"
    )


def test_home_directory_exists():
    assert os.path.isdir(HOME_DIR), f"Home directory {HOME_DIR} does not exist."


def test_workspace_writable():
    assert os.access(HOME_DIR, os.W_OK), f"Home directory {HOME_DIR} is not writable by the executor."
