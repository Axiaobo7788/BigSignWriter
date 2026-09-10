package dev.chililisoup.bigsignwriter.resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.chililisoup.bigsignwriter.BigSignWriter;
import dev.chililisoup.bigsignwriter.font.BlockGlyphEncoder;
import dev.chililisoup.bigsignwriter.font.PagedBitmapGlyphProvider;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.Reader;
import java.util.BitSet;

/** Loads small metadata at reload; bitmap pages are opened only for typed/previewed glyphs. */
public final class BitmapFontLoader {
    private BitmapFontLoader() {}

    public static Loaded load(Identifier source, int signHeight, ResourceManager resources, Font font) throws IOException {
        String path = "bigsignwriter_bitmaps/" + source.getPath() + "/";
        Identifier manifestId = Identifier.fromNamespaceAndPath(source.getNamespace(), path + "manifest.json");
        try (Reader reader = resources.getResourceOrThrow(manifestId).openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            int height = json.get("height").getAsInt();
            int width = json.get("maxWidth").getAsInt();
            int count = json.get("glyphCount").getAsInt();
            if (json.get("format").getAsInt() != 1 || height != signHeight * 3
                    || height < 3 || height > 12 || width < 1 || width > 16 || count < 1 || count > 0x110000)
                throw new IOException("Invalid bitmap manifest dimensions/count/version");
            BitSet pages = new BitSet();
            for (var element : json.getAsJsonArray("pages")) {
                int page = element.getAsInt();
                if (page < 0 || page > 0x10FF || pages.get(page)) throw new IOException("Invalid/duplicate bitmap page");
                pages.set(page);
            }
            if (pages.isEmpty() || count > pages.cardinality() * 256) throw new IOException("Invalid bitmap coverage");
            // A font pack that changes these advances would corrupt row alignment.
            for (String cell : BlockGlyphEncoder.palette())
                if (font.width(cell) != BlockGlyphEncoder.CELL_ADVANCE)
                    throw new IOException("Active Minecraft font changes CJK block widths; use the default font pack");
            String sample = json.has("sample") ? json.get("sample").getAsString() : "";
            sample = sample.codePoints().limit(24).collect(StringBuilder::new, StringBuilder::appendCodePoint,
                    StringBuilder::append).toString();
            PagedBitmapGlyphProvider provider = new PagedBitmapGlyphProvider(page -> resources.getResourceOrThrow(
                    Identifier.fromNamespaceAndPath(source.getNamespace(), path + String.format(java.util.Locale.ROOT, "%04x.bin.gz", page))
            ).open(), pages, height, width, (page, error) -> BigSignWriter.LOGGER.warn(
                    BigSignWriter.LOGGER_PREFIX + "Cannot load bitmap font {} page {}", source, page, error));
            return new Loaded(provider, count, width * BlockGlyphEncoder.CELL_ADVANCE, sample);
        } catch (RuntimeException e) {
            throw new IOException("Malformed bitmap manifest " + source, e);
        }
    }

    public record Loaded(PagedBitmapGlyphProvider provider, int glyphCount, int maxWidth, String sample) {}
}
