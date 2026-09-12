package dev.chililisoup.bigsignwriter.font;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class BitmapGlyphTest {
    private static final Path DATA = Path.of("src/main/resources/assets/bigsignwriter/bigsignwriter_bitmaps/cjk12");

    @Test
    void everyVerticalTripleHasTheExpectedVanillaShape() {
        String[] expected = {" ", "🬂", "🬋", "🬎", "🬭", "🬰", "🬹", "▌"};
        for (int mask = 0; mask < 8; mask++) {
            int[] rows = {mask & 1, (mask >> 1) & 1, (mask >> 2) & 1};
            assertArrayEquals(new String[]{expected[mask]}, BlockGlyphEncoder.encode(new BitmapGlyph(1, rows)));
        }
    }

    @Test
    void dimensionsAndOwnershipAreValidated() {
        assertThrows(IllegalArgumentException.class, () -> new BitmapGlyph(0, new int[12]));
        assertThrows(IllegalArgumentException.class, () -> new BitmapGlyph(17, new int[12]));
        assertThrows(IllegalArgumentException.class, () -> new BitmapGlyph(12, new int[13]));
        assertThrows(IllegalArgumentException.class, () -> new BitmapGlyph(1, new int[]{2, 0, 0}));
        int[] rows = {1, 0, 0};
        BitmapGlyph glyph = new BitmapGlyph(1, rows);
        rows[0] = 0;
        assertTrue(glyph.pixel(0, 0));
    }

    @Test
    void allShippedPagesDecodeAndPreserveEverySourcePixel() throws Exception {
        Map<Integer, Integer> masks = new HashMap<>();
        String[] palette = BlockGlyphEncoder.palette();
        for (int i = 0; i < palette.length; i++) masks.put(palette[i].codePointAt(0), i);
        java.util.Set<Integer> multilingualCoverage = null;
        for (String locale : new String[]{"zh_hans", "zh_hant", "ja", "ko"}) {
            var manifest = JsonParser.parseString(Files.readString(DATA.resolve(locale).resolve("manifest.json"))).getAsJsonObject();
            int count = 0;
            var coverage = new java.util.HashSet<Integer>();
            for (var page : manifest.getAsJsonArray("pages")) {
                Map<Integer, BitmapGlyph> glyphs = read(locale, page.getAsInt());
                count += glyphs.size();
                coverage.addAll(glyphs.keySet());
                for (BitmapGlyph glyph : glyphs.values()) {
                    String[] encoded = BlockGlyphEncoder.encode(glyph);
                    assertEquals(4, encoded.length);
                    for (int y = 0; y < 4; y++) {
                        int[] cells = encoded[y].codePoints().toArray();
                        assertEquals(glyph.width(), cells.length);
                        for (int x = 0; x < cells.length; x++) {
                            int mask = masks.get(cells[x]);
                            for (int dy = 0; dy < 3; dy++)
                                assertEquals(glyph.pixel(x, y * 3 + dy), (mask & (1 << dy)) != 0);
                        }
                    }
                }
            }
            assertEquals(manifest.get("glyphCount").getAsInt(), count);
            if (multilingualCoverage == null) multilingualCoverage = coverage;
            else assertEquals(multilingualCoverage, coverage, "One menu font must retain every region's code-point coverage");
        }
    }

    @Test
    void daoHasTwoSeparatedInnerHorizontalStrokes() throws Exception {
        BitmapGlyph glyph = read("zh_hans", 0x90).get(0x9053);
        assertEquals(12, glyph.width());
        // The eye spans y=3..9: top, two inner strokes and bottom, separated
        // by three open interiors. The upstream six-row eye has only one bar.
        for (int y = 3; y <= 9; y++) {
            assertTrue(glyph.pixel(4, y));
            assertTrue(glyph.pixel(9, y));
            for (int x = 5; x < 9; x++)
                assertEquals(y % 2 == 1, glyph.pixel(x, y), "Eye interior at " + x + "," + y);
        }
        assertEquals(4, provider("zh_hans").getGlyph('道').orElseThrow().length);
    }

    @Test
    void genericSamplesMvpAndSupplementaryGlyphsResolve() throws Exception {
        var provider = provider("zh_hans");
        for (int cp : "你好世界欢迎使用中文汉字示例中文测试字符间隔字体与符号警藏馨鬱繁體漢字日本語仮名ひらがなカタカナ한글𠁨".codePoints().toArray())
            assertEquals(4, provider.getGlyph(cp).orElseThrow(() -> new AssertionError("Missing U+" + Integer.toHexString(cp))).length);
        assertTrue(provider.getGlyph(0x20000).isEmpty(), "An absent Extension B glyph must not become a placeholder");
        var json = JsonParser.parseString(Files.readString(Path.of("tools/cjk-font-generator/generated/cjk_mvp.json")))
                .getAsJsonObject().getAsJsonObject("characters");
        for (var entry : json.entrySet()) {
            String[] actual = provider.getGlyph(entry.getKey().codePointAt(0)).orElseThrow();
            for (int y = 0; y < 4; y++) assertEquals(entry.getValue().getAsJsonArray().get(y).getAsString(), actual[y]);
        }

        FontFile defaultFont = FontFile.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(Files.readString(
                Path.of("src/main/resources/assets/bigsignwriter/bigsignwriter/default.json")))).getOrThrow();
        FamilyCharacterProvider ascii = new FamilyCharacterProvider() {
            public Map<Integer, String[]> characters() { return defaultFont.getCharacters(); }
            public boolean parentIsImplicit() { return true; }
            public FamilyCharacterProvider parentFont() { return null; }
        };
        FamilyCharacterProvider mixed = new FamilyCharacterProvider() {
            public Map<Integer, String[]> characters() { return Map.of(); }
            public BigGlyphProvider glyphProvider() { return provider; }
            public boolean parentIsImplicit() { return true; }
            public FamilyCharacterProvider parentFont() { return ascii; }
        };
        for (int cp : "Minecraft 你好世界 繁體漢字 日本語ひらがなカタカナ 한글 𠁨 Line 1".codePoints().toArray())
            assertEquals(4, GlyphLookup.find(cp, mixed).orElseThrow().length);
    }

    @Test
    void cacheIsLazyBoundedAndDoesNotExposeMutableRows() throws Exception {
        var provider = provider("zh_hans");
        assertEquals(0, provider.statistics().pageLoads());
        String[] first = provider.getGlyph('米').orElseThrow();
        first[0] = "corrupt";
        assertNotEquals("corrupt", provider.getGlyph('米').orElseThrow()[0]);
        assertEquals(1, provider.statistics().hits());
        assertEquals(1, provider.statistics().pageLoads());
        for (int cp = 0x4E00; cp < 0x9FFF; cp += 7) provider.getGlyph(cp);
        assertTrue(provider.statistics().cachedPages() <= PagedBitmapGlyphProvider.PAGE_CACHE_LIMIT);
        assertTrue(provider.statistics().cachedGlyphs() <= PagedBitmapGlyphProvider.GLYPH_CACHE_LIMIT);
        assertEquals(0, provider("zh_hans").statistics().pageLoads(), "Reload starts with a fresh cache");
    }

    @Test
    void missingPagesAreNotReadAndMalformedPagesAreReportedOnce() {
        BitSet pages = new BitSet();
        pages.set(0x20);
        AtomicInteger errors = new AtomicInteger();
        AtomicInteger reads = new AtomicInteger();
        var provider = new PagedBitmapGlyphProvider(page -> {
            reads.incrementAndGet();
            return new ByteArrayInputStream(new byte[]{1, 2, 3});
        }, pages, 12, 12, (page, error) -> errors.incrementAndGet());
        assertTrue(provider.getGlyph(0x3000).isEmpty());
        assertEquals(0, reads.get());
        assertTrue(provider.getGlyph(0x2000).isEmpty());
        assertTrue(provider.getGlyph(0x2001).isEmpty());
        assertEquals(1, reads.get());
        assertEquals(1, errors.get());
    }

    @Test
    void rejectsOversizedDuplicateTruncatedAndTrailingData() throws Exception {
        assertThrows(IOException.class, () -> PagedBitmapGlyphProvider.readPage(null, 0, 12));
        for (int mode = 0; mode < 4; mode++) {
            int scenario = mode;
            byte[] data = invalidPage(scenario);
            assertThrows(IOException.class, () -> PagedBitmapGlyphProvider.readPage(new ByteArrayInputStream(data), 0x20, 12));
        }
    }

    private static byte[] invalidPage(int mode) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(new GZIPOutputStream(bytes))) {
            data.writeInt(0x42535742); data.writeByte(1); data.writeByte(12);
            data.writeShort(mode == 0 ? 257 : mode == 1 ? 2 : 1);
            if (mode != 2) for (int i = 0; i < (mode == 1 ? 2 : 1); i++) {
                data.writeByte(0); data.writeByte(12);
                for (int row = 0; row < 12; row++) data.writeShort(0);
            }
            if (mode == 3) data.writeByte(1);
        }
        return bytes.toByteArray();
    }

    private static Map<Integer, BitmapGlyph> read(String locale, int page) throws IOException {
        return PagedBitmapGlyphProvider.readPage(Files.newInputStream(DATA.resolve(locale).resolve(String.format("%04x.bin.gz", page))), page, 12);
    }

    private static PagedBitmapGlyphProvider provider(String locale) throws IOException {
        var manifest = JsonParser.parseString(Files.readString(DATA.resolve(locale).resolve("manifest.json"))).getAsJsonObject();
        BitSet pages = new BitSet();
        manifest.getAsJsonArray("pages").forEach(page -> pages.set(page.getAsInt()));
        return new PagedBitmapGlyphProvider(page -> Files.newInputStream(DATA.resolve(locale).resolve(String.format("%04x.bin.gz", page))),
                pages, 12, 12, (page, e) -> fail(e));
    }
}
