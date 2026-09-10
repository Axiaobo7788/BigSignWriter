# Opt-in Minecraft UI regression smoke

This separate Fabric 1.21.11 test mod exercises the real shared production sign screen in a disposable flat singleplayer world. It is not packaged in any BigSignWriter JAR and does not touch a launcher profile, existing save or public server.

From the repository root with Java 25 for Gradle and a graphical desktop available:

```sh
JAVA_HOME=/path/to/jdk-25 ./gradlew \
  -I tools/cjk-ui-smoke/ui-smoke.init.gradle \
  :1.21.11-fabric:runTestClient --console=plain
```

The init script compiles the fixture against the 1.21.11 runtime using Java 21 and enables Fabric's client-gametest entrypoint. The client runner closes after the test. Check both exit status and `CJK_UI_SMOKE_PASS`; a launch without the test-mod entrypoint is not a pass.

Assertions cover normal and compact window bounds, unclipped capacity instructions, original Done visibility, disabled/enabled continuation with a non-mutating capacity probe, text retained between actual sign screens, vanilla commit to the integrated test server, hanging signs, one CJK menu entry with no language children and Clear. A mixed phrase `汉漢あア한𠁨` is entered with that one font and continued across six signs; every row of each sign is checked on the integrated server after vanilla Done. The fixture captures actual game screenshots in the development run's screenshots directory. Review them visually; an assertion alone is not visual acceptance.

Synthetic character events are used. The mixed phrase uses typeChar(int) per Unicode scalar because Fabric 4.3.5 typeChars iterates UTF-16 code units. This does not validate a real IME candidate window, the user's resource packs/GPU/modpack or an independent unmodified peer. Unit and all-loader build checks are separate.

The fixture changes only the isolated development profile language, reloads resources for all five locales, and restores that profile to `zh_cn` at the end. It does not change the user’s launcher options and is not an IME test.

The optional-writing fixture starts from disabled settings in this isolated profile, then checks the actual checkbox, reset, save and config reload. It verifies no queue/overflow HUD message while disabled, quiet missing Chinese followed by English in Default, skipped missing supplementary input, English overflow when enabled, queue termination on font change/disable, and normal Backspace after switching fonts. It reloads all five UI languages and captures sign, normal settings, compact settings and scrolled compact-description views. Description height is checked against the actual wrapped text height before scrolling.

When reusing test sign positions, wait for the client to observe removal before placing a fresh sign; a same-tick replacement can retain the old client block entity. Resource-reload futures must be awaited with the gametest scheduler’s waitFor before join, otherwise the test thread prevents the render thread from advancing. All preferences and saves modified by this fixture belong to ignored run/, not the user’s launcher.

The 道 follow-up captures normal and hanging previews and checks all four saved rows on the integrated server. The relocated continuous-writing option is reached by scrolling the actual Options list to its final input group before checkbox/reset/locale checks.
