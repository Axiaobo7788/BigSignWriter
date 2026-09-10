# Font and Unicode architecture

Unicode bitmap fonts extend the existing shared font pipeline. Resource loading, font inheritance and sign insertion remain shared across Fabric and NeoForge targets.

## Build and shared source

Stonecutter 0.9.6 preprocesses shared `src/main` **and `src/test`**. `settings.gradle.kts` declares 26.3 (Minecraft 26.3-pre-1, active), 26.2, 26.1 (26.1.2), 1.21.11 Fabric/NeoForge and 1.21.10 Fabric/NeoForge. The 26.x targets use `build-multiloader.gradle.kts` (Fabric Loom plus NeoForge compile dependencies). Older targets use `build.gradle.kts` with Loom or ModDevGradle. No target was removed.

Java 25 runs Gradle/Modstitch and compiles 26.x; 1.21.x compilation targets Java 21. Gradle distribution is 9.6.1. JUnit is configured in `buildSrc/src/main/kotlin/mod-build-common.gradle.kts`. NeoForge test source sets receive Minecraft dependencies through ModDevGradle's `addModdingDependenciesTo`, after Modstitch enables the platform. Tests do not start a Minecraft server.

## Data model and schema

`FontFile.CODEC` defines name (default Font for legacy user files), optional credits/license/height/characterSeparator/parentFont/characters/symbols, and optional bitmapFont. `characters` remains a JSON object keyed by one Unicode string scalar, with arrays of output text lines. `symbols` retains arbitrary string IDs. Missing height defaults to four. Resource font descriptors live at `assets/<namespace>/bigsignwriter/*.json`.

Internal character maps/coverage sets now use Integer code points. `UnicodeCodePoints` validates scalar keys and converts to/from strings. It rejects empty/multiple-scalar/unpaired-surrogate keys. `FontFile.GsonAdapter` delegates user file loading/copying to the same codec, preserving UTF-8 keys, licenses and bitmap resource references. Encoded caches are not serialized. This fixes the old CODEPOINT -> first-char truncation and numeric-value encoding bug without changing existing glyph rows.

`String[]` still represents text rows, not character identity. TextFieldHelper cursor positions and substring offsets stay UTF-16 indices; `floorBoundary` prevents cutting inside a surrogate pair. Changing every string index into a code-point index would break Minecraft's API contract.

## Reload and inheritance

`BigFontResourceProvider` loads font descriptors and retains source FontFiles. `BigFontManager.prepare` reads `config/bigsignwriter/fonts/*.json` without clearing live UI lists off-thread. `apply` merges current user/resource sources, creates fresh extractions, validates them, replaces font/symbol lists and reselects the prior valid/visible font ID. This avoids stale inherited/provider state on manual user reload as well as resource reload. Fabric/NeoForge order final extraction after vanilla fonts and resource descriptors.

`FontInfoExtractor.prepareFonts` connects source relationships and detects explicit cycles without loading Minecraft glyphs. Extraction validates height/row counts and equal Minecraft pixel widths for static glyphs. It computes finite static coverage, symbol maps and metadata. Explicit parents require matching height and retain ancestors through empty aliases; four-row fonts without an explicit parent retain the existing Default fallback. The single CJK Pixel 12 descriptor retains its original main ID cjk_pixel_12_zh_hans and implicit Default fallback. It already covers simplified/traditional Han, Kana and Hangul, so it has no regional children or language menu. The three obsolete regional menu descriptors are removed; source bitmap datasets remain available to resource-pack/user fonts. Shared Han code points retain the Chinese-preferred default shape. Broken fonts remain inspectable in config and are excluded from selectable typing fonts.

`FamilyCharacterProvider.ownGlyph` checks static JSON overrides before an optional BigGlyphProvider. `GlyphLookup.find` performs exact lookup and preserves the original uppercase/parent precedence: local uppercase beats an implicit parent; an explicit parent's exact character beats a remembered uppercase fallback. The traversal is iterative and cycle-safe. Provider misses use the same parent chain. No separate Chinese-specific lookup system exists.

## Bitmap resources and demand loading

`bitmapFont: "namespace:path"` resolves `assets/namespace/bigsignwriter_bitmaps/path/manifest.json`. `BitmapFontLoader` reads this small manifest during extraction, validates metadata and the active Minecraft palette advances, then captures the current ResourceManager. It does not load/rasterize the full glyph collection. The sample is capped at 24 scalars for previews.

`PagedBitmapGlyphProvider` opens `<codePoint >>> 8, four hexadecimal digits>.bin.gz` only when a requested scalar belongs to an advertised page. Each page contains at most 256 bitmap entries, with strict header/dimension/duplicate/truncation/trailing-data validation. Widths must stay within the manifest. `BitmapGlyph` is immutable and validates row masks. `BlockGlyphEncoder` packs each vertical triple losslessly into vanilla text.

Each provider bounds its in-memory LRU to 16 decoded pages and 512 encoded positive/negative results. Missing pages do not trigger reads; malformed advertised pages are logged once per provider lifetime. Returned encoded row arrays are copied so callers cannot corrupt the cache. Reload discards providers and all positive/negative/error state. There is no runtime network access, font rasterizer or disk cache.

Statistics expose hits, misses, page loads, current cache sizes, encoding/page-read nanoseconds and pixel/text payload bytes. The payload figure excludes object overhead and is not a whole-JVM memory measurement. Standalone measurements are in benchmark.json.

## Input -> glyph -> sign

1. `AbstractSignEditScreenMixin` builds `SignEditContext` from the vanilla sign, messages, TextFieldHelper, current-line callbacks and active Minecraft Font.
2. FontSelectionWidget chooses a valid font; the existing symbol picker remains available for static symbols.
3. A committed CharacterEvent passes its **full code point** to BigFontTyper. Ctrl+V/OS paste is explicitly routed through Unicode text conversion. OS/Minecraft IME preedit remains owned by the host input system.
4. `BigSignWriter.getBigChar` delegates to GlyphLookup, then static/provider/parent resolution. Dynamic bitmap pixels become a String[] of ordinary Unicode rows.
5. BigFontTyper prepares all candidate rows using existing separators/gap fillers and the actual Font.width. `SignInsertion.fits` validates the complete insertion before any setMessage callback runs. The continuation button uses that same preparation as a non-mutating capacity probe. Row overflow/packet-length overflow rejects the whole glyph.
6. PersistentConfig.continuousWriting defaults to false, including legacy JSON. Disabled mode edits each sign independently, creates no queue and silently ignores missing glyphs/overflow. Enabled mode uses PendingSignText for any selected big font, including static English fonts. Missing glyphs are consumed silently; only lack of space stops draining. A filtered, non-mutating peek prevents unsupported queued glyphs from disabling continuation after reload. Clear and Ctrl+Backspace discard the queue; Backspace edits pending text only in enabled mode. Saving/loading a disabled config or selecting a different font ID clears the queue. Reloading the same font ID preserves it. No sign is auto-committed; no queue is saved across client exit.
7. The original vanilla message/commit packet path sends only those Unicode strings. No custom server payload, block or font atlas is introduced.

## Geometry and UI

Mojang 1.21.11 bytecode confirms normal sign width 90 with line pitch 10, hanging sign width 60 with pitch 9. SignEditContext obtains those values from the actual sign object, also preserving dye/glow color handling and four-row limits.

Vanilla's ASCII `█/▌/▐` bitmap overrides differ from Unihex sextant geometry. The accepted encoder therefore packs 1×3 pixels using full-width sextants for six partial masks, ASCII left-half block for a full column, and NBSP for a blank column. Each cell advances five pixels. A 12×12 Han glyph is 60 pixels wide/four rows high: normally one Han per physical sign. Distinct source pixels are retained; small gaps arise from vanilla character advances/line pitch. Palette changes from resource packs/Force Unicode Font produce a validation error; equal-width shape changes still require visual review.

GraphicsHelper remains preview drawing/scaling; no existing bitmap converter existed to reuse. FontInfo previews iterate code points and render the font name, just like existing static fonts; the separate coverage page keeps its bounded sample. PendingTextLayout reserves an independent toolbar row using the actual Done bounds, with a compact-window fallback above the title to avoid the sign preview/post. FontsTab reports static coverage separately from on-demand bitmap counts and renders only finite static characters plus a short sample. Non-US static symbols and supplemental keys use proper scalar strings/counts; thousands of dynamic CJK glyphs are never expanded into SymbolGroup/menus.

## Verification boundaries

Pure logic tests cover JSON/user-copy round trips for all bundled fonts, ASCII/fallback precedence, surrogate keys/carets, all shipped bitmap pixels, real/missing Extension B glyphs, dimensions, malformed pages, bounded caches and full-phrase continuation. The opt-in tools/cjk-ui-smoke fixture also exercises the real 1.21.11 Fabric sign screens and integrated-server commit, with synthetic Unicode input and actual screenshots. The generator is independently repeatable. Atlas simulations verify source assets and packing, not an interactive sign screen.

Build reports, runtime launch evidence and exact remaining human checks are recorded in testing.md. Real IME candidate behavior, sign preview/commit, peer visibility, glow/dye, restart and resource-pack interactions must never be inferred from a compilation or a synthetic image.

## UI localization

All UI components use translation keys. en_us, zh_cn, zh_tw, ja_jp and ko_kr have the same 86 keys and matching format arguments. Original font names and the sample symbol identifier remain literal identifiers. Done references in continuation messages are passed as Minecraft’s translatable gui.done component. The new setting reuses OptionsTab and its existing checkbox, reset, working-copy and save mechanisms; no new settings page or CJK-only input pipeline is introduced.

OptionsTab measures the selected explanation at the actual panel width and sizes its content to the wrapped text. It retains that explanation while the pointer moves into the existing scroll area, allowing long translations to be read at compact window sizes. Selecting another option resets the explanation scroll position.

## Derivative glyph corrections

The generator can read tracked per-dataset BDF corrections beside its script, applying them after source filtering and before page encoding. Corrections must target existing scalars with the same width and valid uncropped dimensions. Currently only default zh_hans U+9053 is corrected; coverage.json and the packaged NOTICE record it. The runtime provider, Unicode encoder, cache and sign packet path do not change. Continuous writing is placed in the final input-options group above the character-separator controls.
