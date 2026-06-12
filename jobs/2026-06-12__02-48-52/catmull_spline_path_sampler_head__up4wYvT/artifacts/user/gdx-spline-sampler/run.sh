#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cd "$SCRIPT_DIR"

# Build the project if needed (Gradle handles up-to-date checks)
./gradlew build -x test --quiet

# Run with the three positional arguments
exec ./gradlew run --args="$1 $2 $3" --quiet