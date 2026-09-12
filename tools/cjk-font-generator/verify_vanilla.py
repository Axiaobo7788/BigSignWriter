#!/usr/bin/env python3
"""Check the actual Minecraft palette assets and render an atlas-based review.

Requires Pillow only for this optional review, not for font generation.
Does not launch Minecraft or establish IME/commit/multiplayer acceptance.
"""
import argparse
import io
import json
import zipfile
from pathlib import Path
from PIL import Image, ImageDraw
from generate import PALETTE


def verify(client, unifont, mvp, output, line_height=10):
    with zipfile.ZipFile(unifont) as z:
        glyphs = {int(line.split(":")[0], 16): line.split(":")[1]
                  for name in z.namelist() if name.endswith(".hex")
                  for line in z.read(name).decode().splitlines() if ":" in line}
    with zipfile.ZipFile(client) as z:
        providers = json.loads(z.read("assets/minecraft/font/include/default.json"))["providers"]
        provider = next(p for p in providers if p.get("file") == "minecraft:font/ascii.png")
        atlas = Image.open(io.BytesIO(z.read("assets/minecraft/textures/font/ascii.png"))).convert("RGBA")
        y = next(i for i, row in enumerate(provider["chars"]) if PALETTE[7] in row)
        x = provider["chars"][y].index(PALETTE[7])
        cell = atlas.crop((x * 8, y * 8, x * 8 + 8, y * 8 + 8))
        assert all(bool(cell.getpixel((xx, yy))[3]) == (xx < 4) for yy in range(8) for xx in range(8))
    assert glyphs[0xA0] == "00" * 16
    for mask, char in enumerate(PALETTE):
        if mask in (0, 7):
            continue
        expected = "".join(("FF" if mask & (1 << part) else "00") * n
                           for part, n in enumerate([5, 6, 5]))
        assert glyphs[ord(char)] == expected, f"Changed vanilla shape U+{ord(char):X}"
    font = json.loads(mvp.read_text())
    assert all(len(rows) == 4 and all(len(row) == 12 for row in rows) for rows in font["characters"].values())
    board = Image.new("RGB", (1040, 235), "#edf0f2")
    draw = ImageDraw.Draw(board)
    draw.text((25, 15), "CJK Pixel 12 / VANILLA FONT ATLAS SIMULATION (not an in-game screenshot)", fill="#172027")
    for ix, char in enumerate("你好世界"):
        ox = 25 + ix * 250
        draw.text((ox, 43), f"U+{ord(char):04X} | 60 px x 4 sign lines", fill="#172027")
        image = Image.new("RGB", (120, line_height * 8), "#d5b583")
        pixels = ImageDraw.Draw(image)
        for y, row in enumerate(font["characters"][char]):
            for x, part in enumerate(row):
                if part == PALETTE[0]:
                    continue
                bits = "FF" * 16 if part == PALETTE[7] else glyphs[ord(part)]
                for yy in range(16):
                    value = int(bits[yy * 2:yy * 2 + 2], 16)
                    for xx in range(8):
                        if value >> (7 - xx) & 1:
                            pixels.point((x * 10 + xx, y * line_height * 2 + yy), fill="#18130e")
        board.paste(image.resize((240, line_height * 16), Image.Resampling.NEAREST), (ox, 67))
    output.parent.mkdir(parents=True, exist_ok=True)
    board.save(output)
    print(f"Verified 8 vanilla palette patterns; {len(font['characters'])} MVP glyphs, 4 rows of 60 px. Review: {output}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--client", type=Path, required=True)
    parser.add_argument("--unifont", type=Path, required=True)
    parser.add_argument("--mvp", type=Path, default=Path(__file__).parent / "generated/cjk_mvp.json")
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--line-height", type=int, choices=[9, 10], default=10, help="9 for hanging signs; 10 for normal signs")
    args = parser.parse_args()
    verify(args.client, args.unifont, args.mvp, args.output, args.line_height)
