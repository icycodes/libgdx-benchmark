import os
import shutil
import subprocess

PROJECT_DIR = "/home/user/myproject"


def test_java_available():
    assert shutil.which("java") is not None, "java binary not found in PATH."


def test_gradle_wrapper_executable():
    wrapper = os.path.join(PROJECT_DIR, "gradlew")
    assert os.path.isfile(wrapper), f"Gradle wrapper {wrapper} does not exist."
    assert os.access(wrapper, os.X_OK), f"Gradle wrapper {wrapper} is not executable."


def test_project_directory_exists():
    assert os.path.isdir(PROJECT_DIR), f"Project directory {PROJECT_DIR} does not exist."


def test_settings_gradle_exists():
    settings = os.path.join(PROJECT_DIR, "settings.gradle")
    assert os.path.isfile(settings), f"{settings} does not exist."


def test_build_gradle_declares_libgdx_dependencies():
    build_gradle = os.path.join(PROJECT_DIR, "build.gradle")
    assert os.path.isfile(build_gradle), f"{build_gradle} does not exist."
    with open(build_gradle, "r", encoding="utf-8") as fh:
        content = fh.read()
    assert "com.badlogicgames.gdx:gdx:1.14.2" in content, (
        "build.gradle must declare com.badlogicgames.gdx:gdx:1.14.2."
    )
    assert "com.badlogicgames.gdx:gdx-backend-headless:1.14.2" in content, (
        "build.gradle must declare com.badlogicgames.gdx:gdx-backend-headless:1.14.2."
    )
    assert "com.badlogicgames.gdx:gdx-platform:1.14.2:natives-desktop" in content, (
        "build.gradle must declare com.badlogicgames.gdx:gdx-platform:1.14.2:natives-desktop."
    )


def test_build_gradle_configures_main_class():
    build_gradle = os.path.join(PROJECT_DIR, "build.gradle")
    with open(build_gradle, "r", encoding="utf-8") as fh:
        content = fh.read()
    assert "com.example.scheduler.Main" in content, (
        "build.gradle must wire the application plugin to com.example.scheduler.Main."
    )


def test_gradle_wrapper_runs():
    # Verifies that the bundled Gradle distribution is usable offline.
    result = subprocess.run(
        ["./gradlew", "--no-daemon", "--offline", "--version"],
        cwd=PROJECT_DIR,
        capture_output=True,
        text=True,
        timeout=180,
    )
    assert result.returncode == 0, (
        f"`./gradlew --version` failed (rc={result.returncode}).\n"
        f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}"
    )
    assert "Gradle" in result.stdout, "Expected `./gradlew --version` to print a Gradle version banner."


def test_no_main_java_yet():
    # The executor is expected to create the Main class; it must not exist in the initial state.
    main_java = os.path.join(
        PROJECT_DIR, "src", "main", "java", "com", "example", "scheduler", "Main.java"
    )
    assert not os.path.exists(main_java), (
        f"Initial state must not include {main_java}; the executor is expected to create it."
    )
