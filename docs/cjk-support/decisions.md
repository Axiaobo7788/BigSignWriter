# Decisions

## D001 — extend the existing font model

FontFile, FontInfo and FontInfoExtractor remain the shared font model. Bitmap lookup integrates through this model; GraphicsHelper remains responsible for preview drawing.

## D002 — preserve one font pipeline

Upgrade character identity to int Unicode scalar values end to end. Keep existing JSON string keys and explicit/implicit uppercase fallback semantics. Unify user serialization with the same schema rather than allowing Gson to serialize integer keys as decimal text.

## D003 — bitmap and bounded on-demand generation

Use designed bitmap glyphs, retaining all 12 source rows in four text lines. The initial 2x3 packing hypothesis was tested and rejected; D005 records the verified replacement. The 40-character generated static JSON proves the conversion independently. The shipping font uses compact source bitmaps and demand conversion instead of expanding tens of thousands of four-line strings at startup. No system font, forced TTF downsizing or parallel Chinese-only font system is involved.

## D004 — preserve physical sign constraints

Continue using sign.getMaxTextLineWidth and existing vanilla rows/packets. Actual metrics establish one 60-pixel Han glyph per ordinary or hanging sign. Reject whole insertions on overflow; never leave partial rows. There is no existing cross-sign wrapping facility. Retain excess input in a bounded session queue, with explicit Continue on the next sign; placement and commit remain user actions. This lets a committed IME phrase/paste survive capacity limits without choosing or modifying other world blocks automatically.

## D005 — vanilla-safe vertical thirds

Verified Mojang 1.21.11 client bitmap providers and Unifont assets. Sextants use Unihex 8x16 bitmaps, rendered at half scale; ASCII overrides full/left/right block with a different size. Therefore naïve 2x3 -> one sextant gives inconsistent geometry for masks 21/42/63. The first encoder packs **one column by three rows**, using full-width sextants for partial thirds, ASCII left-half block for a filled column, and NBSP for blank. All cells have a four-pixel visible width and five-pixel advance under the default vanilla font. A 12x12 Han glyph is 60 pixels wide and four sign lines tall: one glyph per ordinary or hanging sign. This preserves every source pixel and uses existing legacy block compression without an unverified replacement glyph. Use normal space as the inter-glyph separator, distinct from NBSP inside glyphs. Faster 2x3 packing is deferred until every mask can retain correct vanilla geometry.

## D006 — pinned font input

Fusion Pixel 12px monospaced BDF release 2026.09.01, https://github.com/TakWolf/fusion-pixel-font/releases/tag/2026.09.01. Download archive SHA-256: 5b1cac9253fa9e20b9fea5fd582eeed985819a3ce4b7f7fb108f8d6e599ad211. OFL-1.1 permits redistribution with notices; retain OFL.txt and the archive's Ark Pixel, Cubic 11 and Galmuri licenses. Generated derivative family is named CJK Pixel 12, distinguishing the conversion from the upstream family. Source BDF is true 12px bitmap; no system font or vector rasterization. Measured coverage and regional datasets are recorded by the generator.

## D007 — bounded, observable caches

Split data by `codePoint >>> 8` into independently compressed pages. Each provider keeps at most 16 decoded pages and 512 encoded positive/negative results. A known-missing page performs no I/O, and a corrupt page logs once until reload. Source bitmaps and returned cached text have explicit ownership. Rebuild every provider on resource/user reload, retaining source descriptors rather than stale FontInfoExtraction objects. No disk cache or asynchronous scheduling is needed for this initial implementation. Standalone p95 distinct lookup is about 49 microseconds; the first lookup including class/page initialization was about 21 ms. These measurements do not establish actual game frame times or total startup latency.

## D008 — honest regional coverage

Ship zh_hans, zh_hant, ja and ko from the same pinned release, each with 36,193 non-ASCII/non-PUA glyphs. There are 19,214 unified Han and 1,146 Extension B glyphs per variant, plus all 11,172 precomposed Hangul syllables. Preserve regional shapes instead of claiming one shape suits every language. The source lacks 145 GB2312 Han; oversized vertical Kana ligatures U+3031/U+3032 are excluded rather than clipped. PUA branding glyphs are excluded. U+20068 is a real supported Extension B sample; U+20000 is correctly reported missing. Future coverage improvements can replace resources without changing the input architecture.

## D009 — keep existing font and symbol behavior

Static JSON overrides win over bitmap glyphs, followed by the established uppercase and parent rules. Explicit parent chains must survive empty aliases; cycle detection prevents recursion failures. Default fallback remains implicit for the single CJK Pixel 12 entry described in D012. User descriptors/copies share the resource codec and preserve licenses. Static symbols remain available; dynamic coverage is reported as a count and bounded preview instead of constructing thousands of menu entries. Input and output use Unicode scalars while Minecraft cursor offsets remain UTF-16 indices at valid scalar boundaries.

## D010 — validation and delivery boundary

Keep all seven existing version/loader targets. Add shared JUnit checks, correct ModDevGradle test dependency initialization, and declare the existing class-tweaker output dependency for 26.x tests. Restore the missing official wrapper with a pinned distribution checksum. Tests cover pure logic and real bundled data; atlas simulations use locally obtained vanilla assets and are labelled as simulations. No Mojang assets are redistributed. Actual IME candidate integration, committed signs, resource-pack appearance, dye/glow and unmodified peers require the explicit human checklist in testing.md. These are never inferred from a successful build.

## D011 — independent sign controls and styled font previews

Anchor the queue to a free row below Done/selector, or above the title in compact windows to avoid the sign preview and wooden post; do not move or rename Done. Label the action Write pending and explain that a four-row CJK glyph continues on the next physical sign. Disable it when a non-mutating call to the same insertion preparation says the next glyph does not fit. Keep Clear available and hide queue controls while font/symbol menus are open.

Font selection previews draw each font's name in its style; bounded sample strings remain limited to coverage previews. The menu has one multilingual CJK entry with no children, as described in D012.

## D012 — one multilingual CJK menu entry (2026-09-10)

The dropdown offers exactly one CJK Pixel 12 that accepts simplified/traditional Chinese, Japanese and Korean without a language choice. Comparison of all shipped pages proves the four regional datasets cover identical sets of 36,193 Unicode scalars; the main dataset already includes traditional Han, Kana and Hangul.

Use descriptor ID cjk_pixel_12_zh_hans with the display name CJK Pixel 12. The four source bitmap datasets remain usable as bitmapFont resources. Unified Han uses the Chinese-preferred default glyph shape; language-dependent forms cannot be inferred from a shared scalar alone. Verify one actual menu entry with no children, mixed-script input/continuation/commit without selecting another font, and equal source coverage.

## D013 — optional continuous writing, silent missing glyphs and complete UI locales (2026-09-10)

Continuous writing defaults off in the existing Options tab and config JSON. When enabled, it works with any big font and retains overflow for the next sign. Disabling or selecting a different font ends the queue, as explained in the option description. Same-ID reload is not a font change. Ordinary Backspace/space behavior is unaffected when off.

Treat a missing glyph as consumed input, not a capacity failure: skip it and keep processing the remainder. Do not emit missing-glyph HUD messages or replace it with another font’s glyph. Font-loading/schema errors remain inspectable in configuration. Full-sign hints are limited to enabled continuous writing and use Minecraft’s localized Done label.

Ship full en_us/zh_cn/zh_tw/ja_jp/ko_kr resources with matching keys/format arguments; other languages fall back to English. Keep font names and the ASCII sample symbol ID unchanged. Check actual normal/compact settings and sign UI in all five languages, and exercise disabled/enabled editing, saved/reset settings and font changes in the isolated client fixture. These checks are distinct from a native-speaker terminology review or a real IME/peer acceptance run.

Actual compact screenshots exposed an existing fixed-height explanation panel that clipped long translations. Reuse its existing scrolling container, measure wrapped content height and retain the selected explanation when moving to the scrollbar. This narrow correction applies to all option descriptions; it does not restructure the settings page.

## D014 — source glyph correction and input-option grouping (2026-09-10)

All four pinned source variants give 道 a six-row eye with one inner horizontal stroke. Correct the default zh_hans glyph with a small, tracked OFL derivative BDF: move only the right-hand top section up one pixel, expand the eye to seven rows with separated inner strokes at y=5 and y=7, and retain the full left-hand radical, 12x12 cell, width and coverage. Apply it at generation time through the existing BDF parser. Document it in NOTICE and coverage.json and verify all other glyphs/pages remain unchanged. Keep other regional source resources intact and avoid runtime per-character handling.

Move Continuous writing to the final input-options group, immediately before Enable custom character separator and Custom character separator. Default-off behavior, persistence and translations are unchanged. The game fixture scrolls the real options list to reach this group before exercising its existing interactions and all five localized views.

Saved signs contain the already-converted vanilla block characters, so existing 道 signs require rewriting; changing the client font does not retroactively regenerate them.
