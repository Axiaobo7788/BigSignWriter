package dev.chililisoup.bigsignwriter.font;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UnicodeFontTest {
    private final Gson gson = new GsonBuilder().registerTypeAdapter(FontFile.class, new FontFile.GsonAdapter()).create();

    @Test
    void jsonAndUserCopyKeepScalarKeysAndLicense() {
        String text = """
                {"name":"Unicode", "license":["OFL-1.1"], "characters":{
                "A":["a"],"1":["one"],"米":["rice"],"𠀀":["ext0"],"𠀁":["ext1"]}}
                """;
        FontFile font = this.gson.fromJson(text, FontFile.class);
        assertEquals(5, font.getCharacters().size());
        assertEquals("ext0", font.getCharacters().get(0x20000)[0]);
        assertEquals("ext1", font.getCharacters().get(0x20001)[0]);
        assertEquals(JsonParser.parseString(text), JsonParser.parseString(this.gson.toJson(font.copyWithUnsafeCharacters())));
        assertEquals("one", font.getCharacters().get((int) '1')[0]);
    }

    @Test
    void legacyUserFontMayOmitItsDisplayName() {
        FontFile font = this.gson.fromJson("{\"characters\":{\"米\":[\"x\"]}}", FontFile.class);
        assertEquals("Font", font.name);
        assertEquals("x", font.getCharacters().get((int) '米')[0]);
    }

    @Test
    void everyBundledJsonRoundTripsWithoutChangingItsGlyphs() throws Exception {
        try (var files = Files.list(Path.of("src/main/resources/assets/bigsignwriter/bigsignwriter"))) {
            for (Path path : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                JsonObject original = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                FontFile font = FontFile.CODEC.parse(JsonOps.INSTANCE, original).getOrThrow();
                JsonObject encoded = FontFile.CODEC.encodeStart(JsonOps.INSTANCE, font).getOrThrow().getAsJsonObject();
                assertEquals(original, encoded, path.toString());
                FontFile user = this.gson.fromJson(this.gson.toJson(font), FontFile.class);
                for (int cp : font.getCharacters().keySet())
                    assertArrayEquals(font.getCharacters().get(cp), user.getCharacters().get(cp), path + " U+" + cp);
            }
        }
    }

    @Test
    void rejectsMalformedAndSurrogateKeys() {
        for (String key : new String[]{"", "AB", "\uD840", "\uDC00"}) {
            assertThrows(IllegalArgumentException.class, () -> UnicodeCodePoints.fromKey(key));
            JsonObject json = new JsonObject();
            json.addProperty("name", "broken");
            JsonObject chars = new JsonObject();
            chars.add(key, JsonParser.parseString("[\"x\"]"));
            json.add("characters", chars);
            assertTrue(FontFile.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
        }
        assertThrows(IllegalArgumentException.class, () -> UnicodeCodePoints.toKey(0x110000));
        assertEquals(0x20000, UnicodeCodePoints.fromKey("𠀀"));
        assertEquals(3, "米𠀀A".codePoints().count());
    }

    @Test
    void exactStaticThenDynamicThenParentsPreservesUppercaseRules() {
        TestFont parent = new TestFont(Map.of((int) 'a', new String[]{"parent lower"}), null, true, null);
        TestFont implicit = new TestFont(Map.of((int) 'A', new String[]{"own upper"}), null, true, parent);
        TestFont explicit = new TestFont(implicit.characters, null, false, parent);
        assertEquals("own upper", GlyphLookup.find('a', implicit).orElseThrow()[0]);
        assertEquals("parent lower", GlyphLookup.find('a', explicit).orElseThrow()[0]);
        BigGlyphProvider dynamic = cp -> cp == 0x20000 ? Optional.of(new String[]{"dynamic"}) : Optional.empty();
        TestFont font = new TestFont(Map.of(0x20000, new String[]{"override"}), dynamic, false, explicit);
        assertEquals("override", GlyphLookup.find(0x20000, font).orElseThrow()[0]);
        assertEquals("dynamic", GlyphLookup.find(0x20000, new TestFont(Map.of(), dynamic, false, parent)).orElseThrow()[0]);
        assertTrue(GlyphLookup.find(0xD840, font).isEmpty());
        assertTrue(GlyphLookup.find(0x20001, font).isEmpty());
    }

    @Test
    void asciiDefaultStillResolvesBothCasesAndDigits() throws Exception {
        FontFile file = this.gson.fromJson(Files.readString(Path.of(
                "src/main/resources/assets/bigsignwriter/bigsignwriter/default.json")), FontFile.class);
        TestFont font = new TestFont(file.getCharacters(), null, true, null);
        for (int cp : "ABC Hello 123".codePoints().toArray())
            assertTrue(GlyphLookup.find(cp, font).isPresent(), "Missing " + UnicodeCodePoints.toKey(cp));
    }

    @Test
    void lookupTerminatesForCyclicCustomProviders() {
        FamilyCharacterProvider cyclic = new FamilyCharacterProvider() {
            public Map<Integer, String[]> characters() { return Map.of(); }
            public boolean parentIsImplicit() { return false; }
            public FamilyCharacterProvider parentFont() { return this; }
        };
        assertTrue(GlyphLookup.find('A', cyclic).isEmpty());
    }

    @Test
    void emptyAliasesKeepExplicitBitmapAncestorsAndCyclesAreRejected() {
        Identifier rootId = Identifier.parse("test:root");
        Identifier aliasId = Identifier.parse("test:alias");
        Identifier childId = Identifier.parse("test:child");
        FontFile root = this.gson.fromJson("{\"name\":\"Root\",\"bitmapFont\":\"test:bitmap\"}", FontFile.class);
        FontFile alias = this.gson.fromJson("{\"name\":\"Alias\",\"parentFont\":\"test:root\"}", FontFile.class);
        FontFile child = this.gson.fromJson("{\"name\":\"Child\",\"parentFont\":\"test:alias\"}", FontFile.class);
        var fonts = FontInfoExtractor.prepareFonts(Map.of(rootId, root, aliasId, alias, childId, child));
        assertSame(fonts.get(aliasId), fonts.get(childId).parentFont());
        assertSame(fonts.get(rootId), fonts.get(childId).parentFont().parentFont());

        FontFile loop = this.gson.fromJson("{\"name\":\"Loop\",\"parentFont\":\"test:child\"}", FontFile.class);
        var cyclic = FontInfoExtractor.prepareFonts(Map.of(aliasId, loop, childId, child));
        assertNull(cyclic.get(aliasId).parentFont());
        assertNull(cyclic.get(childId).parentFont());
    }

    @Test
    void bundledCjkHasOneMultilingualEntryWithTheOriginalMainResourceId() throws Exception {
        Path directory = Path.of("src/main/resources/assets/bigsignwriter/bigsignwriter");
        try (var files = Files.list(directory)) {
            var descriptors = files.filter(p -> p.getFileName().toString().startsWith("cjk_pixel_12"))
                    .toList();
            assertEquals(java.util.List.of(directory.resolve("cjk_pixel_12_zh_hans.json")), descriptors);
            FontFile cjk = this.gson.fromJson(Files.readString(descriptors.getFirst()), FontFile.class);
            assertEquals("CJK Pixel 12", cjk.name);
            assertTrue(cjk.parentFont().isEmpty());
            assertEquals(Identifier.parse("bigsignwriter:cjk12/zh_hans"), cjk.bitmapFont().orElseThrow());
        }
    }

    @Test
    void caretOffsetsNeverSplitASupplementaryInputOrOutputGlyph() {
        String text = "A𠀀🬂B";
        assertEquals(1, UnicodeCodePoints.floorBoundary(text, 2));
        assertEquals(3, UnicodeCodePoints.floorBoundary(text, 4));
        assertEquals(5, UnicodeCodePoints.floorBoundary(text, 5));
        assertEquals(0, UnicodeCodePoints.floorBoundary(text, -1));
        assertEquals(text.length(), UnicodeCodePoints.floorBoundary(text, 999));
    }

    private record TestFont(Map<Integer, String[]> characters, BigGlyphProvider glyphProvider,
                            boolean parentIsImplicit, FamilyCharacterProvider parentFont) implements FamilyCharacterProvider {}
}
