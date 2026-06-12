#!/usr/bin/env bash
# run.sh – Boot the libGDX headless frame-counter for N render frames.
#
# Usage: bash run.sh <N>
#   N  A positive integer (1 <= N <= 1000) specifying how many render frames
#      to execute before the application shuts down.
#
# Output: A single line "FRAME_COUNT: <N>" is printed to stdout.
# Exit code: 0 on success.

set -euo pipefail

if [ "$#" -lt 1 ]; then
    echo "Usage: $0 <N>" >&2
    exit 1
fi

N="$1"

# Change to the directory containing this script so ./gradlew is always found
# regardless of where the caller invokes run.sh from.
cd "$(dirname "$(realpath "$0")")"

exec ./gradlew --no-daemon -q run --args="$N"
