import json
import os
import re
import subprocess

import pytest

PROJECT_DIR = "/home/user/gdx-spline-sampler"
RUN_SH = os.path.join(PROJECT_DIR, "run.sh")
SPLINE_JSON = "/tmp/spline.json"
INPUT_TXT = "/tmp/input.txt"
INPUT2_TXT = "/tmp/input2.txt"
OUTPUT_CSV = "/tmp/spline-out.csv"

SPLINE_FIXTURE = {
    "continuous": False,
    "controlPoints": [
        {"x": 0.0, "y": 0.0},
        {"x": 10.0, "y": 0.0},
        {"x": 10.0, "y": 10.0},
        {"x": 0.0, "y": 10.0},
        {"x": -10.0, "y": 10.0},
        {"x": -10.0, "y": 0.0},
    ],
}

T_VALUES = ["0.0", "0.25", "0.5", "0.75", "1.0"]

# Expected positions computed analytically from CatmullRomSpline with continuous=false.
EXPECTED_POSITIONS = [
    (0.000000, 10.0000, 0.0000),
    (0.250000, 10.9375, 7.96875),
    (0.500000, 5.625, 10.625),
    (0.750000, -2.734375, 10.234375),
    (1.000000, -10.0000, 10.0000),
]

DATA_ROW_REGEX = re.compile(
    r"^\d+,-?\d+\.\d{6},-?\d+\.\d{6},-?\d+\.\d{6},\d+\.\d{6}$"
)


@pytest.fixture(scope="session", autouse=True)
def write_fixtures_and_run():
    """Set up fixture files, run the sampler, and tear down nothing."""
    assert os.path.isdir(PROJECT_DIR), f"Project directory {PROJECT_DIR} does not exist."
    assert os.path.isfile(RUN_SH), f"Launcher script {RUN_SH} does not exist."
    os.chmod(RUN_SH, 0o755)

    with open(SPLINE_JSON, "w", encoding="utf-8") as f:
        json.dump(SPLINE_FIXTURE, f)
    with open(INPUT_TXT, "w", encoding="utf-8") as f:
        f.write("\n".join(T_VALUES) + "\n")
    with open(INPUT2_TXT, "w", encoding="utf-8") as f:
        f.write("0.5\n")

    if os.path.exists(OUTPUT_CSV):
        os.remove(OUTPUT_CSV)

    result = subprocess.run(
        ["bash", RUN_SH, SPLINE_JSON, INPUT_TXT, OUTPUT_CSV],
        cwd=PROJECT_DIR,
        capture_output=True,
        text=True,
        timeout=600,
    )
    assert result.returncode == 0, (
        f"run.sh failed with code {result.returncode}.\n"
        f"stdout: {result.stdout}\nstderr: {result.stderr}"
    )
    yield


def _read_output_lines(path: str) -> list[str]:
    with open(path, "r", encoding="utf-8") as f:
        text = f.read()
    assert text.endswith("\n"), f"Output file {path} must end with a trailing newline."
    return text.rstrip("\n").split("\n")


def test_output_csv_created():
    assert os.path.isfile(OUTPUT_CSV), f"Expected {OUTPUT_CSV} to be created by run.sh."


def test_output_csv_line_count():
    lines = _read_output_lines(OUTPUT_CSV)
    assert len(lines) == 1 + len(T_VALUES), (
        f"Expected {1 + len(T_VALUES)} lines (1 header + {len(T_VALUES)} data rows) in "
        f"{OUTPUT_CSV}, got {len(lines)}: {lines!r}"
    )


def test_output_csv_header():
    lines = _read_output_lines(OUTPUT_CSV)
    assert lines[0] == "tick,t,x,y,speed", (
        f"Expected header 'tick,t,x,y,speed' on first line of {OUTPUT_CSV}, got: {lines[0]!r}"
    )


def test_output_csv_row_format():
    lines = _read_output_lines(OUTPUT_CSV)
    for i, row in enumerate(lines[1:]):
        assert DATA_ROW_REGEX.match(row), (
            f"Row {i} of {OUTPUT_CSV} does not match the required format "
            f"'<int>,<float:.6f>,<float:.6f>,<float:.6f>,<positive-float:.6f>': {row!r}"
        )


def test_output_csv_tick_column_order():
    lines = _read_output_lines(OUTPUT_CSV)
    for i, row in enumerate(lines[1:]):
        tick = row.split(",")[0]
        assert tick == str(i), (
            f"Row {i} of {OUTPUT_CSV} has tick column {tick!r}, expected {i}."
        )


def test_output_csv_t_column_order():
    lines = _read_output_lines(OUTPUT_CSV)
    expected_ts = ["0.000000", "0.250000", "0.500000", "0.750000", "1.000000"]
    for i, row in enumerate(lines[1:]):
        t_str = row.split(",")[1]
        assert t_str == expected_ts[i], (
            f"Row {i} of {OUTPUT_CSV} has t column {t_str!r}, expected {expected_ts[i]!r}."
        )


def test_output_csv_positions_match_catmullrom_math():
    lines = _read_output_lines(OUTPUT_CSV)
    tolerance = 1e-3
    for i, row in enumerate(lines[1:]):
        parts = row.split(",")
        t = float(parts[1])
        x = float(parts[2])
        y = float(parts[3])
        expected_t, expected_x, expected_y = EXPECTED_POSITIONS[i]
        assert abs(t - expected_t) < 1e-6, (
            f"Row {i}: t={t} != expected t={expected_t}."
        )
        assert abs(x - expected_x) < tolerance, (
            f"Row {i} (t={t}): x={x} differs from expected {expected_x} by more than {tolerance}."
        )
        assert abs(y - expected_y) < tolerance, (
            f"Row {i} (t={t}): y={y} differs from expected {expected_y} by more than {tolerance}."
        )


def test_output_csv_speed_strictly_positive():
    lines = _read_output_lines(OUTPUT_CSV)
    for i, row in enumerate(lines[1:]):
        speed = float(row.split(",")[4])
        assert speed > 0.0, (
            f"Row {i} of {OUTPUT_CSV} has speed={speed}; expected strictly positive tangent magnitude."
        )


def test_rerun_with_shorter_input_overwrites_output():
    # Re-run with /tmp/input2.txt (only t=0.5) and confirm the output is overwritten.
    result = subprocess.run(
        ["bash", RUN_SH, SPLINE_JSON, INPUT2_TXT, OUTPUT_CSV],
        cwd=PROJECT_DIR,
        capture_output=True,
        text=True,
        timeout=600,
    )
    assert result.returncode == 0, (
        f"Second run.sh invocation failed with code {result.returncode}.\n"
        f"stdout: {result.stdout}\nstderr: {result.stderr}"
    )
    lines = _read_output_lines(OUTPUT_CSV)
    assert len(lines) == 2, (
        f"Expected 2 lines (header + 1 row) after rerun, got {len(lines)}: {lines!r}"
    )
    assert lines[0] == "tick,t,x,y,speed", (
        f"Header missing after rerun; got: {lines[0]!r}"
    )
    parts = lines[1].split(",")
    assert parts[0] == "0", f"Expected tick=0 after rerun, got {parts[0]!r}."
    assert parts[1] == "0.500000", f"Expected t=0.500000 after rerun, got {parts[1]!r}."
    x = float(parts[2])
    y = float(parts[3])
    assert abs(x - 5.625) < 1e-3, f"After rerun, expected x≈5.625, got {x}."
    assert abs(y - 10.625) < 1e-3, f"After rerun, expected y≈10.625, got {y}."
    assert float(parts[4]) > 0.0, f"After rerun, expected speed>0, got {parts[4]}."
