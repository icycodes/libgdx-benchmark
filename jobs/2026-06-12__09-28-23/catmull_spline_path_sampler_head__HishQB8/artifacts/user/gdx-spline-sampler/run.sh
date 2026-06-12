#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo "Usage: $0 <spline.json> <input.txt> <output.csv>" >&2
  exit 64
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

if command -v gradle >/dev/null 2>&1; then
  GRADLE_CMD=(gradle)
elif [[ -x "${SCRIPT_DIR}/gradlew" ]]; then
  GRADLE_CMD=("${SCRIPT_DIR}/gradlew")
else
  echo "Gradle is required, but neither 'gradle' nor an executable './gradlew' was found." >&2
  exit 127
fi

"${GRADLE_CMD[@]}" --quiet run --args="$1 $2 $3"
