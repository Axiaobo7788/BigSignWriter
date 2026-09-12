<p align="center"><img width="128" alt="Big Sign Writer Logo" src="src/main/resources/assets/bigsignwriter/icon.png"></p>

<h1 align="center">Big Sign Writer</h1>

A client-side utility mod that lets you easily add large, multi-line characters and symbols to your signs! Each of the mod's characters/symbols are made up of regular Unicode characters from the vanilla Minecraft font, allowing this mod to work on most servers and be seen by most players, even without the mod!

**Certain resource packs may cause issues.**

<p align="center">
 <a href="https://modrinth.com/mod/bigsignwriter"><img src="https://img.shields.io/badge/Modrinth-00AF5C?style=for-the-badge&logo=modrinth&labelColor=16181C" alt="Modrinth downloads"></a>
 <a href="https://curseforge.com/minecraft/mc-mods/big-sign-writer"><img src="https://img.shields.io/badge/CurseForge-F16436?style=for-the-badge&logo=curseforge&labelColor=0D0D0D" alt="CurseForge downloads"></a>
</p>

<hr>

> [!IMPORTANT]
> ## Upstream development continues on [Codeberg](https://codeberg.org/chililisoup/BigSignWriter)
> This fork contains an independent, AI-assisted Unicode/CJK extension. It is not part of the upstream release.

<hr>

<p align="center">
<img alt="'BIG SIGN WRITER' written across several signs in large text" src="https://cdn.modrinth.com/data/cached_images/e349049404a0248aae271832dce2551e29134458_0.webp">
</p>

<hr>

Big Sign Writer adds plenty of fonts and hundreds of symbols ready for use, easily accessible from vanilla Minecraft's sign edit screen.

The mod is highly configurable, with an in-game config screen accessible by default through the sign edit screen, or [Mod Menu](https://modrinth.com/mod/modmenu) on Fabric and NeoForge's mod list.

For more information, check out the [wiki](https://codeberg.org/chililisoup/BigSignWriter/wiki)!


## CJK / 中文大字

Choose **CJK Pixel 12** in the existing sign font selector and type or paste simplified/traditional Chinese, Japanese or Korean, including mixed text, without switching fonts. The dropdown has one CJK entry, with its name drawn in that font. The OFL bitmap source uses 12-pixel glyphs with the documented correction below; the generated sign text uses vanilla Unicode blocks and needs no server mod or additional font resource pack.

A Han glyph takes four lines and 60 pixels, normally **one character per sign**. Signs are edited independently by default. For whole phrases such as `你好世界`, enable **Continuous Writing / 连续写牌** in **Options**, just above the custom character separator controls, fill the first sign, choose **Done**, then open each next sign and click **Write pending / 写入待写**. The pending toolbar occupies its own row and never covers Done; Write pending is disabled when the current sign cannot fit the next glyph. Hover the button to inspect the queue. While continuous writing is enabled, Backspace edits pending text first; Clear discards it. Changing fonts, disabling the option or exiting the client clears the queue. Missing glyphs are silently skipped. Without continuous writing, no queue is created and Backspace edits the current sign.

The complete mod UI follows the Minecraft language setting in English, Simplified Chinese, Traditional Chinese, Japanese and Korean; untranslated game languages fall back to English.

The bundled source has broad CJK coverage, including traditional Han, Kana, Hangul and some supplementary characters, but does not contain every Unicode character. Existing ASCII fonts remain available. Keep the default Minecraft font; resource packs and Force Unicode Font can change the block geometry.

Read [CJK usage, coverage and validation](docs/cjk-support/README.md), the [manual acceptance checklist](docs/cjk-support/testing.md), and the [AI-assisted development handoff](docs/cjk-support/agent-memory.md). Font generation is reproducible with the [CJK font tools](tools/cjk-font-generator/README.md).

The bundled default CJK Pixel 12 includes a documented OFL derivative correction for 道, restoring two separated inner horizontal strokes. Existing signs must be rewritten to use the corrected shape because signs store the converted vanilla characters.
