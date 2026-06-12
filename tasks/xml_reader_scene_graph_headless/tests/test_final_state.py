import math
import os
import re
import subprocess
from pathlib import Path

import pytest

PROJECT_DIR = "/home/user/gdx-scene-loader"
TOLERANCE = 1e-3

LINE_PATTERN = re.compile(
    r"^(?P<path>[A-Za-z0-9_.]+)"
    r"\tworldX=(?P<wx>-?\d+\.\d{4})"
    r"\tworldY=(?P<wy>-?\d+\.\d{4})"
    r"\tworldRotationDeg=(?P<wr>-?\d+\.\d{4})"
    r"\tworldScaleX=(?P<wsx>-?\d+\.\d{4})"
    r"\tworldScaleY=(?P<wsy>-?\d+\.\d{4})$"
)


def _run_scene(tmp_path: Path, name: str, xml: str):
    scene_path = tmp_path / f"{name}.xml"
    output_path = tmp_path / f"{name}.out"
    scene_path.write_text(xml, encoding="utf-8")
    if output_path.exists():
        output_path.unlink()
    result = subprocess.run(
        [
            "./gradlew",
            "--no-daemon",
            ":app:run",
            "--quiet",
            f"--args=--scene={scene_path} --output={output_path}",
        ],
        cwd=PROJECT_DIR,
        capture_output=True,
        text=True,
        timeout=600,
    )
    return result, output_path


def _parse_output(output_path: Path):
    assert output_path.is_file(), f"Output file {output_path} was not created."
    raw = output_path.read_bytes()
    assert raw.endswith(b"\n"), (
        f"Output file {output_path} must end with a trailing newline."
    )
    text = raw.decode("utf-8")
    lines = text.split("\n")
    # Strip the trailing empty entry produced by the final newline.
    assert lines[-1] == "", "Output file must end with exactly one trailing newline."
    lines = lines[:-1]
    parsed = []
    for idx, line in enumerate(lines):
        match = LINE_PATTERN.match(line)
        assert match is not None, (
            f"Line {idx + 1} of {output_path} does not match the required format: {line!r}"
        )
        parsed.append({
            "path": match.group("path"),
            "worldX": float(match.group("wx")),
            "worldY": float(match.group("wy")),
            "worldRotationDeg": float(match.group("wr")),
            "worldScaleX": float(match.group("wsx")),
            "worldScaleY": float(match.group("wsy")),
        })
    return parsed


def _assert_close(actual_node, expected, tol=TOLERANCE):
    assert actual_node["path"] == expected["path"], (
        f"Expected dotted path {expected['path']!r}, got {actual_node['path']!r}."
    )
    for field in ("worldX", "worldY", "worldRotationDeg", "worldScaleX", "worldScaleY"):
        actual = actual_node[field]
        target = expected[field]
        assert math.isfinite(actual), (
            f"Field {field} for {expected['path']} is not finite: {actual}"
        )
        assert abs(actual - target) <= tol, (
            f"Field {field} for {expected['path']} differs from expected: "
            f"got {actual}, expected {target} (tolerance {tol})"
        )


@pytest.fixture(scope="session", autouse=True)
def warm_up_build():
    """Warm up Gradle so per-test invocations are not dominated by first-time setup."""
    subprocess.run(
        ["chmod", "+x", "./gradlew"], cwd=PROJECT_DIR, check=False
    )
    result = subprocess.run(
        ["./gradlew", "--no-daemon", ":app:classes", "--quiet"],
        cwd=PROJECT_DIR,
        capture_output=True,
        text=True,
        timeout=900,
    )
    assert result.returncode == 0, (
        f"Pre-warm Gradle build failed (exit {result.returncode}).\n"
        f"STDOUT:\n{result.stdout}\nSTDERR:\n{result.stderr}"
    )


def test_headless_backend_dependency_declared():
    build_files = [
        os.path.join(PROJECT_DIR, "app", "build.gradle"),
        os.path.join(PROJECT_DIR, "app", "build.gradle.kts"),
    ]
    found = next((p for p in build_files if os.path.isfile(p)), None)
    assert found is not None, (
        "Neither app/build.gradle nor app/build.gradle.kts exists."
    )
    content = Path(found).read_text(encoding="utf-8")
    assert "gdx-backend-headless" in content, (
        f"{found} does not declare the gdx-backend-headless dependency."
    )


def test_no_opengl_classes_used():
    src_dir = Path(PROJECT_DIR) / "app" / "src"
    assert src_dir.is_dir(), f"Java source directory {src_dir} does not exist."
    bad_pattern = re.compile(r"\bSpriteBatch\b|\bTexture\b|Gdx\.gl")
    offenders = []
    for path in src_dir.rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        if bad_pattern.search(text):
            offenders.append(str(path))
    assert not offenders, (
        f"OpenGL-related classes referenced in headless code: {offenders}"
    )


def test_single_root_node(tmp_path):
    xml = (
        "<scene>"
        "<node name=\"root\" x=\"1.5\" y=\"-2.25\" rotation=\"30\" scaleX=\"2\" scaleY=\"0.5\"/>"
        "</scene>"
    )
    result, output_path = _run_scene(tmp_path, "single", xml)
    assert result.returncode == 0, (
        f"Gradle run failed for single-node scene (exit {result.returncode}).\n"
        f"STDOUT:\n{result.stdout}\nSTDERR:\n{result.stderr}"
    )
    parsed = _parse_output(output_path)
    assert len(parsed) == 1, f"Expected exactly 1 line, got {len(parsed)}."
    _assert_close(parsed[0], {
        "path": "root",
        "worldX": 1.5,
        "worldY": -2.25,
        "worldRotationDeg": 30.0,
        "worldScaleX": 2.0,
        "worldScaleY": 0.5,
    })


def test_parent_child_chain_identity_rotation(tmp_path):
    xml = (
        "<scene>"
        "<node name=\"root\" x=\"10\" y=\"20\" rotation=\"0\" scaleX=\"1\" scaleY=\"1\">"
        "<node name=\"child\" x=\"5\" y=\"0\" rotation=\"45\" scaleX=\"1\" scaleY=\"1\"/>"
        "</node>"
        "</scene>"
    )
    result, output_path = _run_scene(tmp_path, "chain", xml)
    assert result.returncode == 0, (
        f"Gradle run failed for parent-child scene (exit {result.returncode}).\n"
        f"STDOUT:\n{result.stdout}\nSTDERR:\n{result.stderr}"
    )
    parsed = _parse_output(output_path)
    assert len(parsed) == 2, f"Expected 2 lines, got {len(parsed)}."
    _assert_close(parsed[0], {
        "path": "root",
        "worldX": 10.0,
        "worldY": 20.0,
        "worldRotationDeg": 0.0,
        "worldScaleX": 1.0,
        "worldScaleY": 1.0,
    })
    _assert_close(parsed[1], {
        "path": "root.child",
        "worldX": 15.0,
        "worldY": 20.0,
        "worldRotationDeg": 45.0,
        "worldScaleX": 1.0,
        "worldScaleY": 1.0,
    })


def test_three_level_chain_with_rotation(tmp_path):
    xml = (
        "<scene>"
        "<node name=\"root\" x=\"10\" y=\"20\" rotation=\"0\" scaleX=\"1\" scaleY=\"1\">"
        "<node name=\"child\" x=\"5\" y=\"0\" rotation=\"45\" scaleX=\"1\" scaleY=\"1\">"
        "<node name=\"leaf\" x=\"3\" y=\"3\" rotation=\"0\" scaleX=\"0.5\" scaleY=\"0.5\"/>"
        "</node>"
        "</node>"
        "</scene>"
    )
    result, output_path = _run_scene(tmp_path, "rot", xml)
    assert result.returncode == 0, (
        f"Gradle run failed for three-level scene (exit {result.returncode}).\n"
        f"STDOUT:\n{result.stdout}\nSTDERR:\n{result.stderr}"
    )
    parsed = _parse_output(output_path)
    assert len(parsed) == 3, f"Expected 3 lines, got {len(parsed)}."
    expected = [
        {
            "path": "root",
            "worldX": 10.0,
            "worldY": 20.0,
            "worldRotationDeg": 0.0,
            "worldScaleX": 1.0,
            "worldScaleY": 1.0,
        },
        {
            "path": "root.child",
            "worldX": 15.0,
            "worldY": 20.0,
            "worldRotationDeg": 45.0,
            "worldScaleX": 1.0,
            "worldScaleY": 1.0,
        },
        {
            "path": "root.child.leaf",
            "worldX": 15.0,
            "worldY": 20.0 + 3.0 * math.sqrt(2.0),  # 3 + 3 rotated by 45 = (0, 3*sqrt(2))
            "worldRotationDeg": 45.0,
            "worldScaleX": 0.5,
            "worldScaleY": 0.5,
        },
    ]
    for actual, exp in zip(parsed, expected):
        _assert_close(actual, exp)


def test_branching_tree_with_parent_scale(tmp_path):
    xml = (
        "<scene>"
        "<node name=\"root\" x=\"0\" y=\"0\" rotation=\"90\" scaleX=\"2\" scaleY=\"2\">"
        "<node name=\"a\" x=\"1\" y=\"0\" rotation=\"0\" scaleX=\"1\" scaleY=\"1\">"
        "<node name=\"a1\" x=\"0\" y=\"1\" rotation=\"0\" scaleX=\"1\" scaleY=\"1\"/>"
        "</node>"
        "<node name=\"b\" x=\"0\" y=\"1\" rotation=\"-90\" scaleX=\"0.5\" scaleY=\"0.5\"/>"
        "</node>"
        "</scene>"
    )
    result, output_path = _run_scene(tmp_path, "tree", xml)
    assert result.returncode == 0, (
        f"Gradle run failed for branching scene (exit {result.returncode}).\n"
        f"STDOUT:\n{result.stdout}\nSTDERR:\n{result.stderr}"
    )
    parsed = _parse_output(output_path)
    assert len(parsed) == 4, f"Expected 4 lines, got {len(parsed)}."
    expected = [
        {
            "path": "root",
            "worldX": 0.0,
            "worldY": 0.0,
            "worldRotationDeg": 90.0,
            "worldScaleX": 2.0,
            "worldScaleY": 2.0,
        },
        {
            # local (1,0) scaled by (2,2) -> (2,0); rotated by 90 -> (0,2)
            "path": "root.a",
            "worldX": 0.0,
            "worldY": 2.0,
            "worldRotationDeg": 90.0,
            "worldScaleX": 2.0,
            "worldScaleY": 2.0,
        },
        {
            # local (0,1) scaled by (2,2) -> (0,2); rotated by 90 -> (-2,0)
            # plus parent world (0,2)
            "path": "root.a.a1",
            "worldX": -2.0,
            "worldY": 2.0,
            "worldRotationDeg": 90.0,
            "worldScaleX": 2.0,
            "worldScaleY": 2.0,
        },
        {
            # b: local (0,1) scaled by (2,2) -> (0,2); rotated 90 -> (-2,0)
            "path": "root.b",
            "worldX": -2.0,
            "worldY": 0.0,
            "worldRotationDeg": 0.0,
            "worldScaleX": 1.0,
            "worldScaleY": 1.0,
        },
    ]
    for actual, exp in zip(parsed, expected):
        _assert_close(actual, exp)
