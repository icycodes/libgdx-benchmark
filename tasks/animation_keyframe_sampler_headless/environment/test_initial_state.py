import os
import shutil
import subprocess

PROJECT_DIR = "/home/user/myproject"


def test_java_is_available():
    java_path = shutil.which("java")
    assert java_path is not None, "java binary not found in PATH."
    result = subprocess.run(
        ["java", "-version"], capture_output=True, text=True
    )
    assert result.returncode == 0, (
        f"`java -version` failed (rc={result.returncode}): {result.stderr}"
    )


def test_javac_is_available():
    assert shutil.which("javac") is not None, "javac binary not found in PATH."


def test_gradle_or_wrapper_is_available():
    gradle_in_path = shutil.which("gradle")
    wrapper_path = os.path.join(PROJECT_DIR, "gradlew")
    assert gradle_in_path is not None or os.path.isfile(wrapper_path), (
        "Neither `gradle` (in PATH) nor `gradlew` (under the project directory) is available."
    )


def test_project_directory_exists():
    assert os.path.isdir(PROJECT_DIR), (
        f"Project directory {PROJECT_DIR} does not exist."
    )


def test_libgdx_artifacts_are_cached_for_offline_build():
    """The Dockerfile must pre-warm the Gradle / Maven cache so that
    `./gradlew --offline` can resolve libGDX 1.14.2 without network access."""
    candidate_roots = [
        "/home/user/.gradle/caches/modules-2/files-2.1/com.badlogicgames.gdx",
        "/root/.gradle/caches/modules-2/files-2.1/com.badlogicgames.gdx",
        "/home/user/.m2/repository/com/badlogicgames/gdx",
        "/root/.m2/repository/com/badlogicgames/gdx",
    ]
    found = [p for p in candidate_roots if os.path.isdir(p)]
    assert found, (
        "No pre-populated libGDX artifacts were found in the Gradle or Maven cache; "
        f"checked: {candidate_roots}"
    )

    # At least one of: gdx, gdx-backend-headless, gdx-platform must be present
    expected_artifacts = ("gdx", "gdx-backend-headless", "gdx-platform")
    discovered = set()
    for root in found:
        try:
            for name in os.listdir(root):
                if name in expected_artifacts:
                    discovered.add(name)
        except OSError:
            continue
    missing = sorted(set(expected_artifacts) - discovered)
    assert not missing, (
        f"Missing pre-cached libGDX artifacts in {found}: {missing}"
    )
