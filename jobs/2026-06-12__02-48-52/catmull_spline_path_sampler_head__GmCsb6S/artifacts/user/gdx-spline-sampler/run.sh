#!/bin/bash
set -e

if [ "$#" -ne 3 ]; then
    echo "Usage: $0 <spline.json> <input.txt> <output.csv>"
    exit 1
fi

SPLINE_PATH="$1"
if [[ "$SPLINE_PATH" != /* ]]; then
    SPLINE_PATH="$PWD/$SPLINE_PATH"
fi

INPUT_PATH="$2"
if [[ "$INPUT_PATH" != /* ]]; then
    INPUT_PATH="$PWD/$INPUT_PATH"
fi

OUTPUT_PATH="$3"
if [[ "$OUTPUT_PATH" != /* ]]; then
    OUTPUT_PATH="$PWD/$OUTPUT_PATH"
fi

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

gradle run --args="\"$SPLINE_PATH\" \"$INPUT_PATH\" \"$OUTPUT_PATH\"" --quiet
