# CJK / Unicode big-sign support

This development adds a general Unicode bitmap-glyph supplement to the existing BigSignWriter font pipeline. It supports direct code-point input and paste, including supplementary characters. Output remains ordinary vanilla Unicode text in the existing sign rows and packets. No server mod, custom blocks or custom Minecraft font texture is introduced.

Implementation, reproducible font generation, automated regression checks and all seven build targets are complete. Interactive Minecraft acceptance is separately recorded in testing.md; a build or atlas simulation does not prove IME, sign commit or vanilla peer visibility.

## 中文使用方法

1. 安装与你的 Minecraft / loader 匹配的构建，打开告示牌编辑界面。
2. 在原有字体选择器选择唯一的 `CJK Pixel 12`。同一个字体可以直接输入或混排简体、繁体、日文汉字／假名和韩文，无须展开或切换语言；列表用该字体绘制名称。
3. 默认每块牌独立编辑。可以通过 Minecraft 与系统所支持的输入法直接输入，或粘贴文字；不需要逐字打开符号菜单。
4. 每个 12×12 汉字保留全部点阵，占四行、60 像素宽。普通告示牌宽度 90、悬挂告示牌 60，因此通常每块牌放一个汉字。
5. 如需一次输入整句，在设置的“选项”页开启 **连续写牌**（默认关闭，位于“启用自定义字符间隔”上方）。开启后，超过当前牌容量的文字保存在续写队列。点原版 **完成（Done，原版确认按钮）** 保存当前牌，打开下一块牌，点击 **写入待写（Write pending）**；按同样步骤放置后面的字。一个 CJK 大字已占满四行，这不是换到当前牌的下一行。悬停按钮可查看待写内容和操作说明。
6. 连续写牌开启且存在待写文字时，退格先删除队列末尾的一个完整 Unicode 字符；Ctrl+退格或“清空”删除整个队列。切换字体、关闭连续写牌或退出客户端会清空队列。关闭时不积累待写文字，退格只编辑当前牌。

续写控件与原版“完成”按钮各占独立一排。空间充足时放在其下方，紧凑窗口放到标题上方，避开告示牌预览和木杆。当前牌放不下下一个字时，“写入待写”禁用；字体列表或符号菜单展开时，续写控件隐藏。

`你好世界` 需要四块牌，`欢迎使用中文` 需要六块牌。ASCII 混排沿用 Default 字体，并依据实际字宽填入当前牌；剩余内容按同样方式续写。

缺字会静默跳过，不显示 Unicode 编码提示，不阻塞后续可写字符；只有牌面容量不足才会暂停已开启的续写。

设置、字体信息和符号菜单已完整提供英文、简体中文、繁体中文、日文和韩文翻译，每种语言 86 条文案，跟随 Minecraft 的语言设置。字体名称和示例符号标识符保留其原名。

输入法的预编辑/候选窗口由 Minecraft、操作系统及已有输入法集成负责；本 mod 接收其提交的 Unicode 字符并转换。当前环境尚未完成所有操作系统的真实 IME 验收。

## Coverage and limitations

The single menu entry uses the zh_hans dataset, which already covers all four writing systems. All four retained source datasets have identical code-point coverage; shared Han glyph shapes use the Chinese-preferred default, without guessing language from context. Each dataset contains 36,193 bundled non-ASCII/non-PUA glyphs: 19,214 unified Han, 1,146 Extension B characters and all 11,172 precomposed Hangul syllables. Hiragana, Katakana, traditional Han and complex examples such as 警藏馨鬱 are covered by the pinned source. Positive supplementary sample: U+20068 `𠁨`. U+20000 is not in this source; the pipeline handles it correctly as missing, and JSON fonts can define it.

This is **not complete coverage of every CJK code point**. The source lacks 145 GB2312 Han; larger CJK extension blocks are partial. U+3031/U+3032 are oversized vertical Kana ligatures and are excluded rather than cropped. Private-use source logos are not advertised. An absent glyph is silently skipped and does not block subsequent supported text. This also applies to Chinese entered in a static English font that lacks it; select CJK Pixel 12 to write supported Chinese. Full source counts are in tools/cjk-font-generator/coverage.json.

Use Minecraft's default font. A resource pack or Force Unicode Font setting that changes the compression palette's advances disables the CJK font with an error. A pack can also change shapes without changing advances, which requires visual review. Other players must have compatible vanilla output glyphs; validation covers the project's supported versions, not arbitrary older clients or Bedrock translation plugins. Dye/glow and actual peer visibility remain explicit manual checks.

## Font schema and user fonts

Existing `characters: {"A": ["..."], "米": ["..."]}` JSON retains its string-key schema. A key must be exactly one Unicode scalar; supplementary keys are supported and unpaired surrogate keys are rejected. Static characters override dynamic glyphs. A provider miss falls through existing uppercase and explicit/implicit parent rules.

An optional `bitmapFont` resource ID adds on-demand support:

```json
{
  "name": "My Unicode Font",
  "height": 4,
  "parentFont": "bigsignwriter:default",
  "characterSeparator": " ",
  "bitmapFont": "bigsignwriter:cjk12/zh_hans"
}
```

Load user descriptors through `config/bigsignwriter/fonts/*.json` and use the existing reload control. Copying a font preserves its license and bitmap resource reference; it does not serialize its cache. Resource packs can supply `assets/<namespace>/bigsignwriter_bitmaps/<path>/manifest.json` and page files. See the generator README for the binary format. Both user and resource fonts now use the same JSON codec.

Resource reload rebuilds providers and caches. The symbol menu retains existing static symbols; it does not enumerate tens of thousands of CJK entries. Font metadata lists on-demand coverage and displays a bounded sample.

## Design and validation

The font source is Fusion Pixel 12px BDF 2026.09.01, OFL-1.1. Modified font data is named CJK Pixel 12; required notices are packaged in META-INF/licenses/cjk-pixel-12. Font data is about 1.71 MB compressed across four variants. Implementation/generator remain under BigSignWriter's MPL-2.0.

One output character packs three vertical source pixels using vanilla full-width sextants, NBSP and the ASCII left-half block. A naïve 2×3 mapping was rejected because vanilla's ASCII block overrides have different geometry. Bitmap pages load on demand; caches are limited to 16 pages and 512 positive/negative glyph results. There is no runtime download or disk cache.

See [architecture.md](architecture.md) for the data flow, [decisions.md](decisions.md) for design tradeoffs, and [testing.md](testing.md) for reproducible checks and remaining manual acceptance.

## Building and reproducing checks

Run the checked-in Gradle wrapper with Java 25. Minecraft 1.21.x outputs still target Java 21. The wrapper pins Gradle 9.6.1 and its distribution SHA-256; the system Gradle installation is not required.

```sh
JAVA_HOME=/path/to/jdk-25 ./gradlew \
  :1.21.10-fabric:build :1.21.10-neoforge:build \
  :1.21.11-fabric:build :1.21.11-neoforge:build \
  :26.1:build :26.2:build :26.3:build --continue
```

JARs are written to `versions/<target>/build/libs/`; JUnit XML/HTML reports are under that target's `build/test-results/test/` and `build/reports/tests/test/`. `26.1` currently builds against Minecraft 26.1.2, and `26.3` against 26.3-pre-1. The inherited 26.3 configuration compiles its NeoForge entrypoint against 26.2.0.67; an actual 26.3 NeoForge runtime is not verified by that build.

Font generation, independent regeneration, vanilla atlas review and the standalone cache benchmark are documented in [the tool README](../../tools/cjk-font-generator/README.md). Minecraft verification assets and downloaded toolchains stay outside source control.

## “道”字点阵修正（2026-09-10）

原始 12px 点阵中，“道”的内部仅有一条横画。本项目以独立 BDF 修正默认 CJK Pixel 12 的该字形，调整右侧“首”的纵向空间，显示两条分开的内部横画；左侧走之旁、12×12 大小和四行/60 像素宽度保持不变。修改随生成器重复生成，并记入字体 NOTICE。只修正这个字，其余字形保持原样。已写在世界里的“道”保存的是转换后的方块字符，需要删除后重新输入才会更新。
