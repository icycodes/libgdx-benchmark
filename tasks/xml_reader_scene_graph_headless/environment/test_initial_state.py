import os
import shutil
import subprocess

PROJECT_DIR = "/home/user/gdx-scene-loader"


def test_java_available():
    java_path = shutil.which("java")
    assert java_path is not None, "java binary not found in PATH."
    result = subprocess.run(
        ["java", "-version"], capture_output=True, text=True
    )
    assert result.returncode == 0, (
        f"`java -version` failed with code {result.returncode}: {result.stderr}"
    )


def test_project_directory_exists():
    assert os.path.isdir(PROJECT_DIR), (
        f"Project directory {PROJECT_DIR} does not exist."
    )


def test_gradle_wrapper_exists():
    gradlew = os.path.join(PROJECT_DIR, "gradlew")
    assert os.path.isfile(gradlew), (
        f"Gradle wrapper script {gradlew} does not exist."
    )
    assert os.access(gradlew, os.X_OK), (
        f"Gradle wrapper script {gradlew} is not executable."
    )
    wrapper_jar = os.path.join(
        PROJECT_DIR, "gradle", "wrapper", "gradle-wrapper.jar"
    )
    assert os.path.isfile(wrapper_jar), (
        f"Gradle wrapper jar {wrapper_jar} does not exist."
    )


def test_settings_gradle_includes_app_module():
    candidates = [
        os.path.join(PROJECT_DIR, "settings.gradle"),
        os.path.join(PROJECT_DIR, "settings.gradle.kts"),
    ]
    found = next((p for p in candidates if os.path.isfile(p)), None)
    assert found is not None, (
        f"Neither settings.gradle nor settings.gradle.kts exists in {PROJECT_DIR}."
    )
    with open(found, "r", encoding="utf-8") as fh:
        content = fh.read()
    assert ":app" in content or "'app'" in content or '"app"' in content, (
        f"settings file {found} does not declare an :app subproject."
    )


def test_app_module_build_gradle_exists_with_headless_backend():
    candidates = [
        os.path.join(PROJECT_DIR, "app", "build.gradle"),
        os.path.join(PROJECT_DIR, "app", "build.gradle.kts"),
    ]
    found = next((p for p in candidates if os.path.isfile(p)), None)
    assert found is not None, (
        f"app module build script not found under {PROJECT_DIR}/app."
    )
    with open(found, "r", encoding="utf-8") as fh:
        content = fh.read()
    assert "gdx-backend-headless" in content, (
        f"{found} does not declare the gdx-backend-headless dependency."
    )
    assert "1.14.2" in content, (
        f"{found} does not pin libGDX to version 1.14.2."
    )


def test_app_module_source_directory_exists():
    src_dir = os.path.join(PROJECT_DIR, "app", "src", "main", "java")
    assert os.path.isdir(src_dir), (
        f"Expected Java source directory {src_dir} does not exist."
    )
