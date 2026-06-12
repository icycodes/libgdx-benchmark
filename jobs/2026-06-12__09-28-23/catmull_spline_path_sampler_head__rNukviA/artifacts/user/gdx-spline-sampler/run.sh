#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

SPLINE_JSON="$1"
INPUT_TXT="$2"
OUTPUT_CSV="$3"

# Build the project if needed (Gradle will skip if up-to-date)
./gradlew build -q

# Run the headless sampler
./gradlew run -q --args="'$SPLINE_JSON' '$INPUT_TXT' '$OUTPUT_CSV'"
