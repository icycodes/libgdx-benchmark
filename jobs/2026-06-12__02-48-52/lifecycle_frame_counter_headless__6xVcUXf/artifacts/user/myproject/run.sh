#!/bin/bash
cd "$(dirname "$0")"
./gradlew --no-daemon -q run --args="$1"