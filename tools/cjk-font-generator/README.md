# Reproducible CJK font tools

The generator uses the **2026.09.01 Fusion Pixel 12px monospaced BDF** release. It verifies SHA-256 before reading anything. Only Python 3.10+ and its standard library are needed for generation. Glyph data is converted from original bitmap pixels plus explicitly documented derivative BDF corrections; no system font, TTF rasterization, manual character JSON or runtime network request is used.

From the repository root:

```sh
python3 tools/cjk-font-generator/generate.py --archive work/fusion-pixel-12px-bdf.zip
```

The ZIP is downloaded from the pinned release if absent. `--root /path/to/temporary/root` regenerates into an independent tree for byte comparison. Output includes four regional source sets, one multilingual font descriptor, OFL/upstream notices, `coverage.json`, and the 40-character static `generated/cjk_mvp.json`. The packaged NOTICE.txt is maintained alongside the generator because it describes the conversion itself.

The menu descriptor is named CJK Pixel 12 and uses the existing zh_hans resource ID. That source already covers simplified/traditional Han, Kana and Hangul; all four source datasets have equal code-point coverage. The three former regional menu descriptors are removed when regenerating. Other source datasets remain available for resource-pack/user-font references.

Each page contains up to 256 Unicode scalars and is independently gzip-compressed. Header, all integers big endian: ASCII `BSWB`, u8 format=1, u8 bitmap height, u16 entry count. Entries: u8 low code-point byte, u8 width, then `height` u16 row masks. The filename supplies the high code-point bits. Row bit `width-1` is the leftmost pixel. No glyph/row outside its bounds is accepted. Oversized BDF ligatures are recorded and excluded, never clipped.

`manifest.json`: format, height (3/6/9/12), maxWidth (1..16), glyphCount, unique available page numbers, sample (preview only; runtime caps at 24 scalars), sourceVersion. Normal output uses 12-pixel height and four text lines. The Java reader/encoder is the runtime authority; unit tests compare its output against the independent Python MVP and reconstruct every bitmap pixel in all four datasets.

For an optional atlas review, install Pillow in your own environment and provide locally downloaded official Minecraft assets:

```sh
python3 tools/cjk-font-generator/verify_vanilla.py --client /path/to/client.jar --unifont /path/to/unifont.zip --output /path/to/preview.png
# Hanging signs use a 9-pixel line pitch:
python3 tools/cjk-font-generator/verify_vanilla.py --client /path/to/client.jar --unifont /path/to/unifont.zip --line-height 9 --output /path/to/hanging.png
```

This verifies the actual Unihex masks and ASCII block override. The rendered image is a simulation using those assets, not a game screenshot. Do not commit or redistribute Minecraft assets.

Measure the core independently with Java 21+:

```sh
bash tools/cjk-font-generator/benchmark.sh
```

The benchmark reports page/glyph cache statistics, cold lookup, p50/p95 timings, and cached pixel/text payload bytes. It excludes Java object overhead, Minecraft startup/rendering and IME latency. Filesystem caches may already be warm.

## Documented glyph corrections

`corrections/zh_hans.bdf` contains only U+9053 道. The pinned upstream glyph compresses the eye into six rows, leaving only one interior horizontal stroke. This derivative moves the top of the right-hand 首 up one pixel and gives its eye seven rows: top at y=3, inner strokes at y=5 and y=7, bottom at y=9. The left-hand 辶, cell width and 12-row height stay unchanged. No resampling is used. The correction is OFL-1.1 like the source; packaged NOTICE.txt records it.

The generator reads corrections through the same BDF parser, rejects invalid dimensions or changes to existing coverage/width, and records corrected code points in coverage.json. This changes only U+9053 in the zh_hans page 0090; the other 1,539 pages and all other glyphs remain byte-identical to the previous package. The optional regional datasets retain the original source forms. Reproduction requires both the pinned archive and the tracked correction BDF. No manually encoded BigSignWriter character JSON or special case in Java is introduced.
