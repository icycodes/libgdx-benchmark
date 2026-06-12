#!/bin/bash
set -e

if [ -z "$1" ]; then
    echo "Usage: $0 <N>" >&2
    exit 1
fi

./gradlew --no-daemon -q run --args="$1"
