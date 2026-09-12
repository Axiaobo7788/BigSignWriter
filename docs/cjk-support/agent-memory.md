# AI-assisted development handoff

This is the current public engineering handoff for the Unicode/CJK extension in this fork. The implementation, tests and documentation were developed with Codex assistance and reviewed through local builds and Minecraft client fixtures. This file is intended for future maintainers and coding agents; it contains no system prompts, session transcripts, credentials, machine-specific paths or private usage scenarios.

Read this file together with [architecture.md](architecture.md), [decisions.md](decisions.md), [testing.md](testing.md), [progress.md](progress.md) and [todo.md](todo.md) before changing the CJK pipeline.

## Current implementation

- Unicode identity uses scalar code points throughout font parsing, lookup, preview and input. Existing JSON keeps one-scalar string keys, including supplementary characters. Invalid surrogate keys are rejected.
- `BigGlyphProvider` supplements existing static fonts. Static glyphs retain precedence, followed by dynamic bitmap lookup and the existing uppercase/parent fallback rules. Empty aliases preserve provider ancestry and parent cycles are rejected.
- `PagedBitmapGlyphProvider` loads gzip pages on demand and keeps bounded LRU caches: 16 decoded pages and 512 positive or negative glyph results. Resource and user-font reloads create fresh providers.
- `SignInsertion` validates all four output rows before applying any mutation. Overflow never leaves a partial glyph on a sign.
- `PendingSignText` supports optional cross-sign continuation. Continuous Writing is disabled by default, applies to every big font when enabled, keeps at most 4,096 Unicode scalars and is session-only. The user still saves and opens each physical sign.
- Missing glyphs are skipped quietly. A missing glyph does not block later supported text. Changing font or disabling Continuous Writing clears pending text; reloading the same font ID preserves it.
- The sign toolbar has a separate row from the vanilla Done button. Compact layouts place it above the title, and font/symbol menus hide it while open. The same non-mutating capacity calculation controls whether Write pending is enabled.
- UI strings are complete for `en_us`, `zh_cn`, `zh_tw`, `ja_jp` and `ko_kr`, with 86 matching keys and format arguments. Other locales use Minecraft's normal English fallback.

## Font data and geometry

- Source: Fusion Pixel 12px monospaced BDF release `2026.09.01`, OFL-1.1.
- Pinned archive SHA-256: `5b1cac9253fa9e20b9fea5fd582eeed985819a3ce4b7f7fb108f8d6e599ad211`.
- Four regional resources are packaged: `zh_hans`, `zh_hant`, `ja` and `ko`. Each advertises 36,193 non-ASCII/non-PUA glyphs: 19,214 unified Han, 1,146 Extension B characters and all 11,172 precomposed Hangul syllables.
- The font selector exposes one `CJK Pixel 12` entry using Chinese-preferred shared Han forms. The other regional datasets remain available to resource/user font descriptors.
- Coverage is broad but not universal. The source lacks 145 GB2312 Han characters, larger extension blocks are partial, U+3031/U+3032 are excluded because their bitmap bounds exceed the 12px cell, and source PUA logos are not advertised.
- U+20068 `𠁨` is the positive supplementary test glyph. U+20000 is intentionally used as an absent-glyph test.
- The encoder packs one source column by three vertical pixels using NBSP, six full-width sextants and the ASCII left-half block. Every output cell advances five pixels with the default Minecraft font.
- A 12×12 Han glyph becomes four sign rows and advances 60 pixels. It normally occupies one physical regular or hanging sign.
- The default `zh_hans` U+9053 (`道`) uses a documented OFL derivative BDF correction that gives the eye two separated inner strokes. No runtime character special case is used.

## Validation baseline

- All seven retained build targets pass: Minecraft 1.21.10 Fabric/NeoForge, 1.21.11 Fabric/NeoForge, and the combined 26.1, 26.2 and 26.3 targets.
- Each target runs 26 tests: 182 executions total, with zero failures, errors or skips in the latest recorded matrix.
- Independent generation reproduces 1,551 output files byte-for-byte. The four regional datasets contain 1,540 compressed bitmap pages totaling about 1.71 MB.
- The opt-in Minecraft 1.21.11 Fabric client fixture exercises real sign/config screens, normal and hanging signs, integrated-server commits, continuation, missing glyphs, font changes, compact layouts and all five UI locales. It uses synthetic committed scalar events, so it is not a real IME test.
- `benchmark.json` records a standalone sample: zero page reads during provider construction, about 21 ms for the first glyph including page initialization/read, about 1.47 μs cache-hit p95 and about 48.75 μs distinct-lookup p95. These values are not Minecraft startup or frame-time measurements.

Run the full matrix with JDK 25 and the checked-in Gradle wrapper:

```sh
JAVA_HOME=/path/to/jdk-25 ./gradlew \
  :1.21.10-fabric:build :1.21.10-neoforge:build \
  :1.21.11-fabric:build :1.21.11-neoforge:build \
  :26.1:build :26.2:build :26.3:build --continue
```

Run the opt-in client fixture with:

```sh
JAVA_HOME=/path/to/jdk-25 ./gradlew \
  -I tools/cjk-ui-smoke/ui-smoke.init.gradle \
  :1.21.11-fabric:runTestClient --console=plain
```

## Invariants and pitfalls

- Keep public examples generic, such as `你好世界`. Do not publish player-specific names, world locations, screenshots, logs, launcher profiles or machine paths.
- Do not replace scalar-key JSON with decimal integer keys; user/resource fonts must continue through the shared codec.
- Keep the source frozen while a multi-target matrix is running. Tests and production generated through Stonecutter must use the same snapshot.
- Do not add `src/test/java` as a second source root; Stonecutter already preprocesses it.
- Configure NeoForge test dependencies only after Modstitch enables NeoForge. The 26.x test classes also depend on class-tweaker conversion.
- Fabric client gametest `typeChars` iterates UTF-16 units in the tested API version. The fixture intentionally sends supplementary input through `typeChar(int)`.
- During the client fixture, wait until the client observes sign removal before reusing a block position. Await resource reload through the gametest scheduler before joining its future.
- The 26.3 target compiles Minecraft `26.3-pre-1` while its inherited NeoForge compile dependency is `26.2.0.67`. A successful build does not prove a matching 26.3 NeoForge runtime.
- Resource packs or Force Unicode Font can alter the block-character metrics or shapes. The runtime rejects incompatible advances, while equal-advance shape changes still need visual review.

## Remaining evidence

Real IME preedit/candidate behavior, restart persistence, a separate unmodified vanilla client, dye/glow, representative resource packs and runtime coverage across loader families remain manual work. Use the H1–H11 checklist in [testing.md](testing.md); do not infer those results from compilation, generated images or the isolated Fabric fixture.
