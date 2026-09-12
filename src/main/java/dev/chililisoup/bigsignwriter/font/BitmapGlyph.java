package dev.chililisoup.bigsignwriter.font;

/** Immutable bitmap. Bit width-1 is the leftmost pixel of each row. */
public final class BitmapGlyph {
    private final int width;
    private final int[] rows;

    public BitmapGlyph(int width, int[] rows) {
        if (width < 1 || width > 16 || rows.length < 3 || rows.length > 12 || rows.length % 3 != 0)
            throw new IllegalArgumentException("Bitmap must be 1..16 pixels wide and 3/6/9/12 pixels high");
        int mask = (1 << width) - 1;
        for (int row : rows)
            if ((row & ~mask) != 0) throw new IllegalArgumentException("Bitmap row exceeds its width");
        this.width = width;
        this.rows = rows.clone();
    }

    public int width() { return this.width; }
    public int height() { return this.rows.length; }
    public boolean pixel(int x, int y) { return (this.rows[y] & (1 << (this.width - 1 - x))) != 0; }
}
