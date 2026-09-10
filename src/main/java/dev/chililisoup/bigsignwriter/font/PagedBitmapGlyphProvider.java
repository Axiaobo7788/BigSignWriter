package dev.chililisoup.bigsignwriter.font;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.zip.GZIPInputStream;

/** Loads at most one 256-code-point bitmap page on a cold lookup; never scans coverage. */
public final class PagedBitmapGlyphProvider implements BigGlyphProvider {
    public static final int PAGE_CACHE_LIMIT = 16;
    public static final int GLYPH_CACHE_LIMIT = 512;
    private final PageSource source;
    private final BitSet availablePages;
    private final BitSet failedPages = new BitSet();
    private final int height;
    private final int maxWidth;
    private final BiConsumer<Integer, IOException> onError;
    private final Map<Integer, Map<Integer, BitmapGlyph>> pages = lru(PAGE_CACHE_LIMIT);
    private final Map<Integer, Optional<String[]>> glyphs = lru(GLYPH_CACHE_LIMIT);
    private long hits;
    private long misses;
    private long pageLoads;
    private long generationNanos;
    private long pageLoadNanos;

    public PagedBitmapGlyphProvider(PageSource source, BitSet availablePages, int height, int maxWidth,
                                    BiConsumer<Integer, IOException> onError) {
        if (height < 3 || height > 12 || height % 3 != 0 || maxWidth < 1 || maxWidth > 16)
            throw new IllegalArgumentException("Invalid bitmap dimensions");
        this.source = source;
        this.availablePages = (BitSet) availablePages.clone();
        this.height = height;
        this.maxWidth = maxWidth;
        this.onError = onError;
    }

    private static <K, V> Map<K, V> lru(int limit) {
        return new LinkedHashMap<>(limit, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) { return this.size() > limit; }
        };
    }

    @Override
    public synchronized Optional<String[]> getGlyph(int codePoint) {
        if (!UnicodeCodePoints.isScalar(codePoint)) return Optional.empty();
        Optional<String[]> cached = this.glyphs.get(codePoint);
        if (cached != null) {
            this.hits++;
            return cached.map(String[]::clone);
        }
        this.misses++;
        int page = codePoint >>> 8;
        Optional<String[]> result = Optional.empty();
        if (this.availablePages.get(page) && !this.failedPages.get(page)) {
            Map<Integer, BitmapGlyph> entries = this.pages.get(page);
            if (entries == null) {
                long start = System.nanoTime();
                try (InputStream stream = this.source.open(page)) {
                    entries = readPage(stream, page, this.height);
                    for (BitmapGlyph glyph : entries.values())
                        if (glyph.width() > this.maxWidth) throw new IOException("Glyph exceeds manifest width");
                    this.pages.put(page, entries);
                    this.pageLoads++;
                } catch (IOException e) {
                    entries = null;
                    this.failedPages.set(page);
                    this.onError.accept(page, e);
                } finally {
                    this.pageLoadNanos += System.nanoTime() - start;
                }
            }
            if (entries != null) {
                BitmapGlyph bitmap = entries.get(codePoint);
                if (bitmap != null) {
                    long start = System.nanoTime();
                    result = Optional.of(BlockGlyphEncoder.encode(bitmap));
                    this.generationNanos += System.nanoTime() - start;
                }
            }
        }
        this.glyphs.put(codePoint, result);
        return result.map(String[]::clone);
    }

    public static Map<Integer, BitmapGlyph> readPage(InputStream stream, int page, int height) throws IOException {
        if (stream == null) throw new IOException("Missing bitmap page");
        if (height < 3 || height > 12 || height % 3 != 0) throw new IOException("Invalid bitmap height");
        if (page < 0 || page > 0x10FF) throw new IOException("Invalid bitmap page number");
        try (DataInputStream data = new DataInputStream(new GZIPInputStream(stream))) {
            if (data.readInt() != 0x42535742 || data.readUnsignedByte() != 1 || data.readUnsignedByte() != height)
                throw new IOException("Invalid bitmap page header");
            int count = data.readUnsignedShort();
            if (count > 256) throw new IOException("Bitmap page has more than 256 entries");
            Map<Integer, BitmapGlyph> entries = new HashMap<>(count);
            for (int i = 0; i < count; i++) {
                int cp = (page << 8) | data.readUnsignedByte();
                int width = data.readUnsignedByte();
                int[] rows = new int[height];
                for (int y = 0; y < height; y++) rows[y] = data.readUnsignedShort();
                if (!UnicodeCodePoints.isScalar(cp) || entries.containsKey(cp))
                    throw new IOException("Invalid or duplicate bitmap code point");
                try {
                    entries.put(cp, new BitmapGlyph(width, rows));
                } catch (IllegalArgumentException e) {
                    throw new IOException("Invalid bitmap dimensions", e);
                }
            }
            if (data.read() != -1) throw new IOException("Trailing bitmap data");
            return Map.copyOf(entries);
        }
    }

    public synchronized Statistics statistics() {
        long pixelBytes = this.pages.values().stream().flatMap(page -> page.values().stream())
                .mapToLong(glyph -> (long) glyph.height() * Integer.BYTES).sum();
        long textBytes = this.glyphs.values().stream().flatMap(Optional::stream)
                .flatMap(java.util.Arrays::stream).mapToLong(line -> (long) line.length() * Character.BYTES).sum();
        return new Statistics(this.hits, this.misses, this.pageLoads, this.pages.size(), this.glyphs.size(),
                pixelBytes + textBytes, this.pageLoadNanos, this.generationNanos);
    }

    public record Statistics(long hits, long misses, long pageLoads, int cachedPages, int cachedGlyphs,
                             long payloadBytes, long pageLoadNanos, long generationNanos) {}

    @FunctionalInterface
    public interface PageSource { InputStream open(int page) throws IOException; }
}
