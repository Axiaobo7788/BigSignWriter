#!/usr/bin/env python3
"""Reproduce CJK Pixel 12 from the pinned OFL BDF release (stdlib only)."""
import argparse
import gzip
import hashlib
import io
import json
from pathlib import Path
import struct
import urllib.request
import zipfile

VERSION = "2026.09.01"
URL = f"https://github.com/TakWolf/fusion-pixel-font/releases/download/{VERSION}/fusion-pixel-font-12px-monospaced-bdf-v{VERSION}.zip"
SHA256 = "5b1cac9253fa9e20b9fea5fd582eeed985819a3ce4b7f7fb108f8d6e599ad211"
MVP = "你好世界欢迎使用中文测试字体示例天地山川日月星辰春夏秋冬东西南北上下左右大小多少"
PALETTE = ("\u00a0", "\U0001fb02", "\U0001fb0b", "\U0001fb0e", "\U0001fb2d", "\U0001fb30", "\U0001fb39", "\u258c")
LOCALES = {"zh_hans": "简体", "zh_hant": "繁體", "ja": "日本語", "ko": "한국어"}
CORRECTIONS = Path(__file__).resolve().parent / "corrections"


def read_bdf(text, rejected=None):
    """Place BDF BBX pixels in their advance cell; never crop or resample ink."""
    glyphs = {}
    for block in text.split("STARTCHAR ")[1:]:
        header, bitmap = block.split("BITMAP\n", 1)
        fields = dict(line.split(" ", 1) for line in header.splitlines()[1:] if " " in line)
        cp = int(fields["ENCODING"])
        if cp < 0:
            continue  # .notdef must not become a real character
        if cp > 0x10FFFF or 0xD800 <= cp <= 0xDFFF or cp in glyphs:
            raise ValueError(f"Invalid/duplicate scalar: {cp}")
        width = int(fields["DWIDTH"].split()[0])
        bw, bh, bx, by = map(int, fields["BBX"].split())
        if width not in (6, 12):
            if rejected is not None:
                rejected.append(f"U+{cp:04X}")
            continue
        if bx < 0 or bx + bw > width or 10 - by - bh < 0 or 10 - by > 12:
            if rejected is not None:
                rejected.append(f"U+{cp:04X}")
            continue  # Explicitly report oversized vertical ligatures; never crop.
        rows = [0] * 12
        source = bitmap.split("ENDCHAR", 1)[0].splitlines()
        if len(source) != bh:
            raise ValueError(f"Invalid bitmap height U+{cp:04X}")
        for y, raw in enumerate(source):
            bits = int(raw, 16)
            for x in range(bw):
                if bits & (1 << (len(raw) * 4 - 1 - x)):
                    tx, ty = bx + x, 10 - by - bh + y
                    if not (0 <= tx < width and 0 <= ty < 12):
                        raise ValueError(f"Ink outside cell U+{cp:04X}: {tx},{ty}")
                    rows[ty] |= 1 << (width - 1 - tx)
        glyphs[cp] = (width, rows)
    return glyphs


def encode(width, rows):
    if len(rows) != 12 or width not in (6, 12):
        raise ValueError("Expected a 6x12 or 12x12 glyph")
    return ["".join(PALETTE[sum(((rows[y + dy] >> (width - 1 - x)) & 1) << dy
                              for dy in range(3))] for x in range(width))
            for y in range(0, 12, 3)]


def dump(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")


def build(archive, root):
    raw = archive.read_bytes()
    if hashlib.sha256(raw).hexdigest() != SHA256:
        raise ValueError("Source checksum mismatch; refusing to generate")
    assets = root / "src/main/resources/assets/bigsignwriter"
    notices = root / "src/main/resources/META-INF/licenses/cjk-pixel-12"
    reports = {}
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        for name in ["OFL.txt", "LICENSES/ark-pixel/OFL.txt", "LICENSES/cubic-11/OFL.txt", "LICENSES/galmuri/LICENSE.txt"]:
            target = notices / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(z.read(name))
        for locale in LOCALES:
            rejected = []
            glyphs = read_bdf(z.read(f"fusion-pixel-12px-monospaced-{locale}.bdf").decode(), rejected)
            # Latin continues to use the existing Default parent. Whitespace/control
            # and PUA placeholder glyphs are not advertised as Unicode coverage.
            glyphs = {cp: value for cp, value in glyphs.items()
                      if cp >= 0xA0 and not 0xE000 <= cp <= 0xF8FF}
            # Small, documented derivative BDF corrections pass through the same
            # pixel parser/encoder as the pinned source; no runtime special cases.
            corrected = {}
            correction_path = CORRECTIONS / f"{locale}.bdf"
            if correction_path.exists():
                rejected_corrections = []
                corrected = read_bdf(correction_path.read_text(encoding="utf-8"), rejected_corrections)
                if rejected_corrections:
                    raise ValueError(f"Invalid correction dimensions: {rejected_corrections}")
                for cp, glyph in corrected.items():
                    if cp not in glyphs or glyph[0] != glyphs[cp][0]:
                        raise ValueError(f"Correction must preserve existing coverage/width: U+{cp:04X}")
                glyphs.update(corrected)
            target = assets / "bigsignwriter_bitmaps" / "cjk12" / locale
            target.mkdir(parents=True, exist_ok=True)
            pages = {}
            for cp, glyph in sorted(glyphs.items()):
                pages.setdefault(cp >> 8, []).append((cp, glyph))
            for page, entries in pages.items():
                # BSWB, format 1, height, entry count; each entry: low byte,
                # width and height unsigned 16-bit rows (big endian).
                data = bytearray(struct.pack(">4sBBH", b"BSWB", 1, 12, len(entries)))
                for cp, (width, rows) in entries:
                    data.extend(struct.pack(">BB12H", cp & 255, width, *rows))
                compressed = io.BytesIO()
                with gzip.GzipFile(fileobj=compressed, mode="wb", filename="", mtime=0, compresslevel=9) as output:
                    output.write(data)
                (target / f"{page:04x}.bin.gz").write_bytes(compressed.getvalue())
            manifest = {"format": 1, "height": 12, "maxWidth": 12, "glyphCount": len(glyphs),
                        "pages": list(pages), "sample": "你好世界繁體日本語한글", "sourceVersion": VERSION}
            dump(target / "manifest.json", manifest)
            descriptor_path = assets / "bigsignwriter" / f"cjk_pixel_12_{locale}.json"
            # Every regional dataset covers the same Unicode scalars, including
            # traditional Han, Kana and Hangul. Expose one multilingual font.
            # Keep the original main ID and Chinese-preferred shared Han shapes.
            if locale == "zh_hans":
                dump(descriptor_path, {
                    "name": "CJK Pixel 12", "credits": "TakWolf and upstream pixel-font authors",
                    "license": ["OFL-1.1; see META-INF/licenses/cjk-pixel-12"], "height": 4,
                    "characterSeparator": " ", "bitmapFont": "bigsignwriter:cjk12/zh_hans"})
            else:
                # Remove only obsolete generated menu descriptors on regeneration.
                descriptor_path.unlink(missing_ok=True)
            reports[locale] = {"oversizedExcluded": rejected,
                               "correctedGlyphs": [f"U+{cp:04X}" for cp in sorted(corrected)],
                               "glyphCount": len(glyphs), "pages": len(pages),
                               "han": sum(0x4E00 <= cp <= 0x9FFF for cp in glyphs),
                               "extensionB": sum(0x20000 <= cp <= 0x2A6DF for cp in glyphs),
                               "hangul": sum(0xAC00 <= cp <= 0xD7A3 for cp in glyphs),
                               "missingSamples": "".join(c for c in dict.fromkeys(MVP + "警藏馨鬱繁體漢字日本語仮名") if ord(c) not in glyphs),
                               "gb2312HanMissing": sum(ord(bytes([hi, lo]).decode("gb2312")) not in glyphs
                                    for hi in range(0xB0, 0xF8) for lo in range(0xA1, 0xFF)
                                    if not (hi == 0xD7 and lo >= 0xFA))}
            if locale == "zh_hans":
                dump(root / "tools/cjk-font-generator/generated/cjk_mvp.json", {
                    "name": "CJK Pixel 12 MVP", "credits": "TakWolf and upstream pixel-font authors",
                    "license": ["OFL-1.1; see META-INF/licenses/cjk-pixel-12"], "height": 4,
                    "characters": {c: encode(*glyphs[ord(c)]) for c in dict.fromkeys(MVP)}})
    dump(root / "tools/cjk-font-generator/coverage.json", {"version": VERSION, "sha256": SHA256, "locales": reports})
    return reports


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--archive", type=Path, required=True, help="Pinned BDF ZIP; downloaded if absent")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    args = parser.parse_args()
    if not args.archive.exists():
        args.archive.parent.mkdir(parents=True, exist_ok=True)
        with urllib.request.urlopen(URL, timeout=90) as source:
            args.archive.write_bytes(source.read())
    print(json.dumps(build(args.archive, args.root), ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
