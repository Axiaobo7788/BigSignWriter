# Unicode/CJK work queue

This is the public work queue for the AI-assisted CJK extension in this fork.

## Completed

- [x] Audit the shared font, input, resource, UI and seven-target build architecture.
- [x] Pin a redistributable 12px bitmap source and package all required notices.
- [x] Implement reproducible regional bitmap generation and compact lazy pages.
- [x] Support Unicode scalars, supplementary characters and compatible JSON/user-font serialization.
- [x] Add bounded provider caches, fallback/inheritance rules and reload invalidation.
- [x] Make four-row sign insertion atomic and add optional cross-sign continuation.
- [x] Expose one multilingual CJK Pixel 12 entry with styled font-name previews.
- [x] Keep Continuous Writing disabled by default, skip missing glyphs quietly and position its controls without covering Done.
- [x] Complete English, Simplified/Traditional Chinese, Japanese and Korean UI resources.
- [x] Correct default U+9053 (`道`) through a documented OFL derivative BDF.
- [x] Pass all seven builds and 182 test executions; reproduce all 1,551 generated files.
- [x] Pass the isolated Minecraft 1.21.11 Fabric client fixture for sign/config screens, commits, layouts and locales.
- [x] Replace private scenario examples with generic samples and prepare the project-level AI development records for this fork.

## Manual acceptance

- [ ] Complete H1–H6 in [testing.md](testing.md) with real Chinese/Japanese/Korean IMEs and paste, including supplementary characters, queue editing and compact UI behavior.
- [ ] Complete H7–H8 for user-font copy/reload and client restart persistence.
- [ ] Complete H9 with an independent unmodified vanilla client and a representative server.
- [ ] Complete H10 for dye, glow, Force Unicode Font and representative resource packs.
- [ ] Complete H11 for startup/first-use responsiveness and release-JAR runtime smoke tests on supported loader families.
- [ ] Verify an actually compatible NeoForge runtime for the 26.3 target rather than relying on the inherited 26.2 compile dependency.
- [ ] Obtain native-speaker terminology review for `ja_jp`, `ko_kr` and `zh_tw`.

## Future improvements

- [ ] Add more locale files through Minecraft's normal resource fallback without changing the font or input architecture.
- [ ] Make locale-resource parity tests discover additional shipped language files automatically if more locales are added.
- [ ] Consider broader CJK extension coverage only from a pinned redistributable source with measured size and startup impact.
- [ ] Revisit denser encoding only if every output mask has verified vanilla geometry and the compatibility gain justifies the migration.
- [ ] Add hosted CI only after its target matrix, generated-data policy and runtime-fixture limits are documented.
