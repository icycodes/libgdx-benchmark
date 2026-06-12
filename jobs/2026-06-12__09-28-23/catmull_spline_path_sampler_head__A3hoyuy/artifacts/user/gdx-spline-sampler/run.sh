#!/bin/bash
set -e

if [ "$#" -ne 3 ]; then
    echo "Usage: $0 <spline.json> <input.txt> <output.csv>"
    exit 1
fi

# Resolve absolute paths, even if files do not exist yet (using -m)
SPLINE_JSON=$(realpath -m "$1")
INPUT_TXT=$(realpath -m "$2")
OUTPUT_CSV=$(realpath -m "$3")

# Navigate to the project directory
cd /home/user/gdx-spline-sampler

# Execute Gradle run task with arguments
gradle run --args="\"$SPLINE_JSON\" \"$INPUT_TXT\" \"$OUTPUT_CSV\""
