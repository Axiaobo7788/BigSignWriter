package dev.chililisoup.bigsignwriter.font;

/** Losslessly packs vertical triples, respecting vanilla's ASCII block overrides. */
public final class BlockGlyphEncoder {
    private static final int[] PALETTE = {0xA0, 0x1FB02, 0x1FB0B, 0x1FB0E, 0x1FB2D, 0x1FB30, 0x1FB39, 0x258C};
    public static final int CELL_ADVANCE = 5;

    private BlockGlyphEncoder() {}

    public static String[] palette() {
        return java.util.Arrays.stream(PALETTE).mapToObj(UnicodeCodePoints::toKey).toArray(String[]::new);
    }

    public static String[] encode(BitmapGlyph glyph) {
        String[] lines = new String[glyph.height() / 3];
        for (int line = 0; line < lines.length; line++) {
            StringBuilder text = new StringBuilder();
            for (int x = 0; x < glyph.width(); x++) {
                int mask = 0;
                for (int dy = 0; dy < 3; dy++)
                    if (glyph.pixel(x, line * 3 + dy)) mask |= 1 << dy;
                text.appendCodePoint(PALETTE[mask]);
            }
            lines[line] = text.toString();
        }
        return lines;
    }
}
