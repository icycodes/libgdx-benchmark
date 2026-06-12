import os
import shutil
import subprocess

HOME_DIR = "/home/user"


def test_home_directory_exists():
    assert os.path.isdir(HOME_DIR), f"Home directory {HOME_DIR} does not exist."


def test_java_available():
    java = shutil.which("java")
    assert java is not None, "java binary not found in PATH; a JDK is required to build the libGDX project."


def test_javac_available():
    javac = shutil.which("javac")
    assert javac is not None, "javac binary not found in PATH; a JDK (not just a JRE) is required to build the libGDX project."


def test_java_version_at_least_8():
    java = shutil.which("java")
    assert java is not None, "java binary not found in PATH."
    result = subprocess.run([java, "-version"], capture_output=True, text=True)
    # `java -version` writes to stderr on most distributions.
    output = (result.stderr or "") + (result.stdout or "")
    assert "version" in output.lower(), f"Could not parse `java -version` output: {output!r}"


def test_gradle_available():
    gradle = shutil.which("gradle")
    assert gradle is not None, "gradle binary not found in PATH; Gradle is required to build the libGDX project."


def test_bash_available():
    bash = shutil.which("bash")
    assert bash is not None, "bash not found in PATH; the launcher script run.sh requires bash."
