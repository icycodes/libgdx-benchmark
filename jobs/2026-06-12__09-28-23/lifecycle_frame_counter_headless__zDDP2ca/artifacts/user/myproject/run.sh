#!/usr/bin/env bash
set -euo pipefail

if [ $# -ne 1 ]; then
    echo "Usage: $0 <N>" >&2
    exit 1
fi

N="$1"
cd "$(dirname "$0")"
./gradlew --no-daemon -q run --args="$N"
