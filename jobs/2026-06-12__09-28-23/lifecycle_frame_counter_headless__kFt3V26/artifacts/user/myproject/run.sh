#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 N" >&2
  exit 2
fi

./gradlew --no-daemon -q run --args="$1"
