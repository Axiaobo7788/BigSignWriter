#!/usr/bin/env bash
set -euo pipefail
project_root=$(cd -- "$(dirname -- "$0")/../.." && pwd)
classes="$project_root/build/cjk-benchmark"
mkdir -p "$classes"
font_source="$project_root/src/main/java/dev/chililisoup/bigsignwriter/font"
javac --release 21 -d "$classes" \
    "$font_source/UnicodeCodePoints.java" \
    "$font_source/BitmapGlyph.java" \
    "$font_source/BlockGlyphEncoder.java" \
    "$font_source/BigGlyphProvider.java" \
    "$font_source/PagedBitmapGlyphProvider.java" \
    "$project_root/tools/cjk-font-generator/CjkBenchmark.java"
java -cp "$classes" CjkBenchmark "$project_root"
