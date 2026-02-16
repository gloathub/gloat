#!/usr/bin/env bash

set -e

# Usage: compile.sh <source_file> <output_file> <format>
# format can be: clj, glj, go, js (for wasm)

SOURCE_FILE="$1"
OUTPUT_FILE="$2"
FORMAT="$3"

if [[ -z "$SOURCE_FILE" || -z "$FORMAT" ]]; then
  echo "Usage: compile.sh <source_file> <output_file> <format>" >&2
  exit 1
fi

# Change to project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$PROJECT_DIR"

if [[ "$FORMAT" == "js" ]]; then
  # WASM compilation needs output file
  exec make --no-print-directory shell CMD="gloat $SOURCE_FILE -o $OUTPUT_FILE -t js"
else
  # Other formats go to stdout
  exec make --no-print-directory shell CMD="gloat $SOURCE_FILE -t $FORMAT"
fi
