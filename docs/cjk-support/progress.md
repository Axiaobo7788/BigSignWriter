# AI-assisted development progress

This public record summarizes the implementation milestones for this fork's Unicode/CJK extension. It intentionally omits tool transcripts, local paths, credentials, private examples and temporary debugging artifacts.

## 1. Repository and data-model audit

- Mapped the shared source, seven version/loader targets, font resources, user-font serialization, input mixin and sign editor UI.
- Identified UTF-16 `char` truncation, separate user/resource JSON paths, eager static metadata and partial per-row mutation on overflow.
- Chose Unicode scalar integers internally while retaining the existing one-character JSON string-key schema.

## 2. Reproducible bitmap source

- Pinned Fusion Pixel 12px BDF `2026.09.01` by SHA-256 and preserved its OFL notices.
- Implemented a standard-library Python generator for four regional resources and compact gzip pages.
- Verified all eight output palette shapes against Minecraft font assets. A 1×3 vertical mapping was selected because ASCII block overrides make a naïve 2×3 mapping geometrically inconsistent.
- Added generic generated samples, full coverage reporting, normal/hanging atlas verification and a standalone cache benchmark.

## 3. Unicode font integration

- Added scalar-safe parsing, preview, lookup and cursor handling, including supplementary-plane characters.
- Added the lazy bitmap provider and bounded caches, with strict page validation, negative caching and fresh providers after reload.
- Preserved static overrides, uppercase behavior, explicit/implicit parent fallback, empty aliases and cycle detection.
- Unified resource and user-font encoding so copied fonts retain licenses and bitmap references.

## 4. Atomic sign input and continuation

- Made glyph insertion atomic across all four sign rows and packet-length limits.
- Added a bounded, session-only continuation queue for text that does not fit the current physical sign.
- Changed Continuous Writing to an explicit setting that defaults off and works consistently with both bitmap and static fonts.
- Missing characters are skipped silently without blocking subsequent supported input.

## 5. Sign and configuration UI

- Moved continuation controls to their own row and added a compact layout above the title, preserving the original Done button and sign preview.
- Added a non-mutating capacity probe, accurate pending count, Clear behavior and menu visibility rules.
- Exposed one multilingual `CJK Pixel 12` entry rather than four language choices. The single font covers simplified/traditional Han, Kana and Hangul, with Chinese-preferred shared Han forms.
- Moved Continuous Writing directly above the custom character-separator controls.
- Fixed long option descriptions by measuring wrapped content and using the existing scroll container.

## 6. Localization

- Completed 86 matching UI keys for English, Simplified Chinese, Traditional Chinese, Japanese and Korean.
- Added tests for key parity, non-empty values and matching format arguments.
- Exercised all five resource locales in the isolated Minecraft client fixture.

## 7. Glyph correction

- Traced the single inner stroke in `道` to the pinned bitmap source rather than the encoder or packet path.
- Added a tracked OFL BDF correction for default `zh_hans` U+9053. It preserves the left radical, 12×12 cell, width and coverage while adding two separated inner strokes.
- Confirmed that only this glyph changes; the remaining 1,539 pages and all other glyphs remain unchanged.

## 8. Automated and client validation

- Added shared unit tests covering encoding, dimensions, complete packaged data, generic CJK samples, supplementary scalars, JSON round trips, inheritance, cache behavior, error paths, sign capacity, continuation, configuration and translations.
- Restored a pinned Gradle 9.6.1 wrapper and completed all seven build targets.
- Added an opt-in Fabric 1.21.11 client gametest fixture. It exercises real sign/config screens and integrated-server commits while keeping screenshots, worlds and compiled fixture output outside Git.
- Latest recorded validation: 26 tests per target, 182 executions total, zero failures/errors/skips; independent regeneration matches all 1,551 output files.

## 9. Public repository preparation

- Kept runtime bitmap pages, license notices, generator, regression sources and maintainer documentation in version control.
- Excluded build output, caches, logs, screenshots, downloaded Minecraft assets, launcher state, system prompts and machine-specific tool state.
- Replaced scenario-specific samples throughout documentation, manifests, tests and generated fixtures with generic text such as `你好世界`; all builds and the client fixture passed again.
- Added this handoff, progress log and work queue so future AI-assisted work can continue from verifiable project context rather than private session history.

## Current boundary

Implementation and local automated validation are complete. The remaining H1–H11 items are manual acceptance across real input methods, independent clients, resource packs and loader/runtime combinations. See [todo.md](todo.md) and [testing.md](testing.md).
