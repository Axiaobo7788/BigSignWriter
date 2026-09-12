import dev.chililisoup.bigsignwriter.font.PagedBitmapGlyphProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Locale;

/** Standalone core benchmark: no Minecraft launch, source rasterization or system font. */
public final class CjkBenchmark {
    public static void main(String[] args) throws Exception {
        Path data = Path.of(args[0]).resolve("src/main/resources/assets/bigsignwriter/bigsignwriter_bitmaps/cjk12/zh_hans");
        BitSet pages = new BitSet();
        try (var files = Files.list(data)) {
            files.filter(path -> path.toString().endsWith(".bin.gz"))
                    .forEach(path -> pages.set(Integer.parseInt(path.getFileName().toString().substring(0, 4), 16)));
        }
        long start = System.nanoTime();
        var provider = new PagedBitmapGlyphProvider(page -> Files.newInputStream(data.resolve(String.format(Locale.ROOT, "%04x.bin.gz", page))),
                pages, 12, 12, (page, error) -> { throw new IllegalStateException(error); });
        long construction = System.nanoTime() - start;
        var initial = provider.statistics();
        start = System.nanoTime();
        provider.getGlyph('米').orElseThrow();
        long first = System.nanoTime() - start;
        // Warm JVM/JIT separately from the measured cache-hit sample.
        for (int i = 0; i < 10000; i++) provider.getGlyph('米');
        long[] hits = new long[10000];
        for (int i = 0; i < hits.length; i++) {
            start = System.nanoTime();
            provider.getGlyph('米');
            hits[i] = System.nanoTime() - start;
        }
        Arrays.sort(hits);
        long[] lookups = new long[2000];
        for (int i = 0; i < lookups.length; i++) {
            start = System.nanoTime();
            provider.getGlyph(0x4E00 + i * 7);
            lookups[i] = System.nanoTime() - start;
        }
        Arrays.sort(lookups);
        var stats = provider.statistics();
        System.out.printf(Locale.ROOT, """
                {
                  "providerConstructionMicros": %.3f,
                  "pagesReadAtConstruction": %d,
                  "firstGlyphIncludingPageReadMicros": %.3f,
                  "cacheHitP50Micros": %.3f,
                  "cacheHitP95Micros": %.3f,
                  "distinctLookupP50Micros": %.3f,
                  "distinctLookupP95Micros": %.3f,
                  "hits": %d, "misses": %d, "pageLoads": %d,
                  "cachedPages": %d, "cachedGlyphs": %d,
                  "cachedPixelAndTextPayloadBytes": %d,
                  "totalPageReadMicros": %.3f,
                  "totalEncodingMicros": %.3f,
                  "limits": "16 pages / 512 glyph results",
                  "scope": "Standalone JVM; filesystem cache may be warm. Payload excludes object overhead. Not Minecraft startup time."
                }
                """, construction / 1000.0, initial.pageLoads(), first / 1000.0,
                hits[5000] / 1000.0, hits[9500] / 1000.0, lookups[1000] / 1000.0, lookups[1900] / 1000.0,
                stats.hits(), stats.misses(), stats.pageLoads(), stats.cachedPages(), stats.cachedGlyphs(),
                stats.payloadBytes(), stats.pageLoadNanos() / 1000.0, stats.generationNanos() / 1000.0);
    }
}
