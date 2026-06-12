#!/usr/bin/env bash
# run.sh — compile (if needed) and execute the headless CatmullRom spline sampler
#
# Usage: bash run.sh <spline.json> <input.txt> <output.csv>

set -euo pipefail

if [ "$#" -ne 3 ]; then
    echo "Usage: $0 <spline.json> <input.txt> <output.csv>" >&2
    exit 1
fi

SPLINE_JSON="$1"
INPUT_TXT="$2"
OUTPUT_CSV="$3"

# Resolve to absolute paths so the JVM can find the files regardless of
# the working directory from which the Gradle run task is launched.
SPLINE_JSON="$(realpath "$SPLINE_JSON")"
INPUT_TXT="$(realpath "$INPUT_TXT")"
# output.csv may not exist yet — resolve the directory and reconstruct
OUTPUT_CSV_DIR="$(realpath "$(dirname "$OUTPUT_CSV")")"
OUTPUT_CSV="${OUTPUT_CSV_DIR}/$(basename "$OUTPUT_CSV")"

# Change to the project root so Gradle can find build.gradle
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

exec ./gradlew --quiet run --args="\"$SPLINE_JSON\" \"$INPUT_TXT\" \"$OUTPUT_CSV\""
