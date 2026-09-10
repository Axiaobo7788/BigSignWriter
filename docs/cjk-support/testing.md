# Validation record — 2026-09-10

Automated checks, source-asset review and interactive acceptance are distinct. Pending is not passed. The implementation and build deliverables are ready for human Minecraft acceptance; the checklist below states exactly what remains.

## Automated matrix

Final command, from the repository root with JDK 25 and the checked-in Gradle 9.6.1 wrapper:

```sh
JAVA_HOME=/path/to/jdk-25 ./gradlew \
  :1.21.10-fabric:build :1.21.10-neoforge:build \
  :1.21.11-fabric:build :1.21.11-neoforge:build \
  :26.1:build :26.2:build :26.3:build --console=plain --continue
```

Observed result: **BUILD SUCCESSFUL** across all seven targets. Each target's JUnit XML was parsed: **26 tests, 0 failures, 0 errors, 0 skipped**, 182 executions total. The latest run with generic examples took 56 seconds, with 102 tasks (60 executed / 42 up-to-date). This is an incremental matrix run, not a claim about clean-download build time or hosted CI.

| Target | Actual Minecraft compile dependency | Loader artifact | Java class target | Tests/build |
| --- | --- | --- | --- | --- |
| 1.21.10-fabric | 1.21.10 | Fabric | 21 | 26 / passed |
| 1.21.10-neoforge | 1.21.10 | NeoForge 21.10.64 | 21 | 26 / passed |
| 1.21.11-fabric | 1.21.11 | Fabric | 21 | 26 / passed |
| 1.21.11-neoforge | 1.21.11 | NeoForge 21.11.42 | 21 | 26 / passed |
| 26.1 | 26.1.2 | Combined Fabric / NeoForge | 25 | 26 / passed |
| 26.2 | 26.2 | Combined Fabric / NeoForge | 25 | 26 / passed |
| 26.3 | 26.3-pre-1 | Combined Fabric / NeoForge | 25 | 26 / passed |

The existing 26.3 target uses NeoForge 26.2.0.67 as a compile dependency. Its successful combined build does not prove a matching 26.3 NeoForge runtime. Existing Gradle 10 deprecation and Loom mixin-AP migration notices remain; they were not suppressed.

Reports: `versions/<target>/build/test-results/test/TEST-*.xml` and `versions/<target>/build/reports/tests/test/index.html`. Each packaged JAR was checked for ZIP integrity, expected Java class version, matching loader metadata, all 1,540 bitmap pages, one multilingual font descriptor, all five complete locale files (86 keys each), and byte-identical original font licenses and the documented derivative NOTICE. No Minecraft assets or test classes are packaged.

## Coverage of the 26 tests

| Area | Observed result and limits |
| --- | --- |
| Encoder and dimensions | All eight vertical triple patterns; immutable source pixels; invalid dimensions/row masks rejected. |
| Complete shipped font data | Every pixel in all four datasets decodes and reconstructs; manifest totals match 36,193 per region, and complete code-point sets are equal, so the one default font loses no regional character coverage. |
| Generic Chinese samples | 你好世界、欢迎使用中文、汉字、示例 resolve to four rows; independent Python MVP equals Java output for all 40 generic sample glyphs. |
| Complex/traditional/Japanese/Korean | 警藏馨鬱、繁體漢字、日本語仮名、ひらがな、カタカナ、한글 resolve in actual data. This is conversion coverage, not individual visual acceptance of every glyph. |
| Supplementary code points | Real source U+20068 𠁨 resolves. Missing U+20000 returns empty. Independent JSON U+20000/U+20001 keys remain distinct. Invalid surrogate keys are rejected; cursor offsets cannot split pairs. |
| Existing fonts / user JSON | Every bundled descriptor round-trips without changing glyphs, shared Gson adapter preserves scalar keys/license, unnamed legacy user files retain the default name. Actual config-screen copy/reload is a human check. |
| ASCII and mixed input | ABC / Hello / 123 resolve through the real Default font. `Minecraft 你好世界 Line 1` resolves through real CJK data plus Default fallback. |
| Inheritance/fallback | Exactly one bundled CJK descriptor retains the original main ID and has no regional children. Static override, dynamic lookup, local uppercase/explicit-parent precedence, empty alias ancestors and cycles covered. |
| Cache/errors/reload state | Lazy construction, hits, bounded eviction, defensive returned arrays, missing-page no-I/O, corrupt-page single reporting, malformed count/duplicates/truncation/trailing data, fresh provider cache covered. Actual Minecraft reload listener execution is separate. |
| Sign capacity and continuation | Whole-row validation rejects overflow/packet-length overflow. Four-sign queue simulation preserves the phrase and rejected code points; backspace edits a complete scalar; filtered/non-consuming peek skips unsupported glyphs without mutating queue state; invalid/oversized appends are atomic. This does not exercise the real sign GUI or commit packet. |
| Pending toolbar layout | Seven representative GUI heights and custom selector positions stay inside the viewport and avoid the original Done/font rows. The actual compact screenshot additionally caught sign-post occlusion; the fallback was moved above the title. |
| Continuous-writing config | New and legacy JSON default to off. Explicit opt-in and other settings survive the same config copy/JSON serialization path. Actual checkbox/save/reset behavior is separately exercised in the client fixture. |
| 道 glyph legibility | The default glyph has two separated inner horizontal strokes with closed outer edges in a seven-row eye. The upstream one-bar form fails this assertion. Encoder pixel reconstruction covers the corrected data too. |
| UI translations | All 86 UI keys exist and are nonblank in en_us/zh_cn/zh_tw/ja_jp/ko_kr; ordered format arguments match English. This verifies resource completeness, not native-speaker terminology approval. |

## Reproducibility and vanilla geometry

The pinned BDF ZIP SHA-256 is `5b1cac9253fa9e20b9fea5fd582eeed985819a3ce4b7f7fb108f8d6e599ad211`. Final regeneration into an independent temporary tree produced **1,551 files byte-identical** to the corresponding repository files. This latest regeneration includes the tracked derivative correction BDF. The generator fixes gzip filename/time headers and UTF-8/newline handling. NOTICE.txt is authored alongside the conversion; original font license bytes are copied unchanged.

`verify_vanilla.py` checks the actual Mojang 1.21.11 ASCII override and Unicode atlas masks for all eight output cells. Both normal (line pitch 10) and hanging (pitch 9) atlas simulations were rendered and visually inspected for 你好世界. Every Han remains 12 columns / 60 pixel advance / four text rows. The images are labelled **atlas simulations, not in-game screenshots**. Bytecode inspection confirms 90-pixel normal signs and 60-pixel hanging signs. The verifier can reproduce normal and hanging review artifacts from locally available Minecraft assets.

The 1.21.10 Unicode font asset is Unifont 16.0.03; 1.21.11/26.x metadata points to the same Unifont 17.0.01 asset. Their required sextant patterns were checked. This is source-asset evidence; the runtime also checks active palette advances before enabling a bitmap font. Third-party packs can alter shapes without changing widths, so visual compatibility is not guaranteed.

## Performance evidence

Run `bash tools/cjk-font-generator/benchmark.sh` with Java 21+. The saved local sample is [benchmark.json](benchmark.json):

| Measurement | Observed |
| --- | --- |
| Provider construction, including initial class work | 5.50 ms; zero bitmap page reads |
| First glyph including page read | 21.19 ms |
| Repeated cache-hit lookup | p50 1.40 μs / p95 1.47 μs |
| Distinct lookups | p50 7.61 μs / p95 48.75 μs |
| Cache after workload | 16 pages / 512 glyph results |
| Hits / misses / page reads | 20,000 / 2,001 / 56 |
| Retained bitmap/text payload | 251,554 bytes; excludes Java object/map overhead |
| Bundled compressed page data | About 1.71 MB across all four regional variants |

This is a standalone JVM measurement on this machine, with possibly warm filesystem caches and concurrent build activity. It does not measure full Minecraft startup, IME latency, rendering frame times or whole-process memory. The provider never eagerly enumerates all glyphs; manifest/sample work remains bounded. Actual in-game first-use/reload responsiveness remains on the manual checklist.

## Real Minecraft client regression fixture

Run the opt-in fixture against an isolated Minecraft 1.21.11 Fabric client:

```sh
JAVA_HOME=/path/to/jdk-25 ./gradlew \
  -I tools/cjk-ui-smoke/ui-smoke.init.gradle \
  :1.21.11-fabric:runTestClient --console=plain
```

The fixture is a separate test mod, excluded from delivered JARs. It creates a disposable integrated-server world in ignored `run/`; no existing launcher profile or world is required. See [the fixture instructions](../../tools/cjk-ui-smoke/README.md) for details.

The latest run on 2026-09-10 uses generic examples such as 你好世界 and logged `CJK_UI_SMOKE_PASS` and **BUILD SUCCESSFUL** in 1m 34s, exit 0. The following actual interactions passed:

- Normal and hanging sign previews, vanilla Done submission and integrated-server receipt of all four rows for `汉漢あア한𠁨` and the corrected `道`.
- Exactly one CJK Pixel 12 font entry, no regional children, and mixed-script typing without changing fonts.
- Normal 1280×854 and compact 640×480 windows at GUI scale 2; original Done visibility, independent pending toolbar, non-mutating capacity checks and next-sign continuation/Clear.
- Default-off input, ordinary Backspace, actual checkbox/reset/save/config reload, enabled CJK and English overflow, queue clearing on font change/disable, and same-ID resource reload.
- `马A` in Default skips the unsupported Chinese character, writes the expected A rows and leaves an existing HUD message untouched. Absent supplementary input does not block subsequent supported input.
- Resource reload and actual sign/settings screens in en_us, zh_cn, zh_tw, ja_jp and ko_kr; scrolling to the input-options group, long descriptions and compact description scrolling.

The run captured 34 actual game screenshots, including both corrected 道 previews and each language's normal/compact settings. These were visually reviewed. Input is delivered as committed Unicode scalar events; this does not test a real IME's preedit/candidate behavior, an independent vanilla peer, or every loader/modpack/resource pack. Translation key/argument checks do not replace native-speaker terminology review.

## 道 glyph correction audit

The pinned BDF source gives 道 a six-row eye with one internal horizontal stroke. Previous bundled pixels match the source exactly. The tracked derivative BDF gives the eye seven rows and two separated internal strokes, preserving the left radical, 12×12 cell, 60-pixel advance and coverage.

A full comparison with the prior generated data confirms that only U+9053 in zh_hans/0090 changes; every other glyph and the other 1,539 pages remain unchanged. Regeneration with the pinned source archive plus the tracked correction BDF reproduces all 1,551 generated files. The all-pages encoder test reconstructs the corrected pixels, and a dedicated regression checks both inner strokes and their separating gaps. Original font licenses and the derivative NOTICE remain packaged. Existing signs must be rewritten because they contain the already-converted Unicode characters.

## Human Minecraft acceptance — still required

Record Minecraft version, loader/version, OS/input method, resource packs, Force Unicode Font setting and screenshots/log evidence for each run. Use a disposable world and the matching development JAR. Do not mark any row passed solely from this document or an atlas image.

| ID | Procedure | Expected result | Status |
| --- | --- | --- | --- |
| H1 | Enable Continuous writing in Settings → Options, above the custom separator controls. Open a normal sign; select CJK Pixel 12; commit `你好世界` through a real Chinese IME, then repeat using paste. | Four-row 你 on the first sign; queue shows 好世界. Candidate/preedit behavior belongs to the host IME; committed characters convert without surrogate corruption. | Human pending |
| H2 | Save that sign, place/open three more signs and click Write pending once per sign. Reopen each saved sign. | 你 / 好 / 世 / 界 in order; no partial glyphs, lost queue characters or unexpected auto-commit. Inspect preview and committed world appearance. | Human pending |
| H3 | Repeat H1/H2 on hanging signs; test other generic phrases and complex glyphs. | One 60-pixel Han fits each 60-pixel hanging sign; ordinary sign widths remain enforced; inspect legibility at normal play distance. | Human pending |
| H4 | Select CJK Pixel 12 once; input or mix 繁體漢字、日本語仮名、Kana、한글 and paste 𠁨. Try absent U+20000. | All scripts work without switching font; shared Han uses Chinese-preferred forms; real supplementary glyph is intact; absent glyph is skipped silently, does not remain stuck at the front of the queue and does not block later supported text. | Human pending |
| H5 | Use Default/other old fonts for ABC, Hello, 123 and old symbols. Use CJK for `Minecraft 你好世界 Line 1`. Exercise left/right/delete, separators and mixed heights. | Existing fonts/symbols remain usable; CJK mixed input respects real widths and continuation preserves order. | Human pending |
| H6 | Enable continuous writing; with queued text, open/close font dropdown, symbol picker and config; resize GUI/window; use Backspace/Ctrl+Backspace/Clear. Separately switch fonts/vanilla or disable the option. Repeat ordinary editing with the option off. | No overlapping queue controls while dropdown/symbol picker is open; count/tooltip accurate; one complete pending scalar removed by Backspace; changing font or disabling the option clears pending text. With the option off there is no queue/toolbar/full-sign hint; Backspace edits the sign. All five UI locales follow the game language and long descriptions can scroll. | Human pending |
| H7 | Copy a CJK font to the user directory, add static overrides including a supplementary key and an empty-parent alias; use the existing user reload control. Then F3+T. | Copy retains license/bitmap reference, static override wins, new provider replaces cached results, selected valid ID is restored and symbols do not duplicate. | Human pending |
| H8 | Restart the client and reopen committed signs. | Committed Unicode signs persist; fonts load again; the documented session-only queue is empty after exit. | Human pending |
| H9 | Join an unmodified vanilla server and view the signs with a second matching vanilla client that has no BigSignWriter installed. | All rows appear using vanilla glyphs; no server mod/resource pack is required. Record both client and server versions. | Human pending |
| H10 | Apply dyes and glow ink; compare default fonts, Force Unicode Font and chosen resource packs. Restore default pack and reload. | Dye/glow remains legible; incompatible advance changes disable CJK with an error; restored default pack recovers. Equal-advance shape changes need visual judgement. | Human pending |
| H11 | Compare startup and first CJK use/reload responsiveness with baseline on representative hardware; test a release JAR on each loader family. | No full-font eager loading or unacceptable visible pauses; no mixin/reload errors. The 26.3 NeoForge runtime must use an actually compatible upstream loader. | Human pending |

Record the version, loader, resource packs, input method and steps when reporting any remaining runtime or visual issue. Successful local builds and integrated-server tests do not imply that every manual row has passed.
