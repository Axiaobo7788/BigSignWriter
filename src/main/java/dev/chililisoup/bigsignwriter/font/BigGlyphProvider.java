package dev.chililisoup.bigsignwriter.font;

import java.util.Optional;

/** Optional on-demand supplement to a font's static JSON characters. */
@FunctionalInterface
public interface BigGlyphProvider {
    Optional<String[]> getGlyph(int codePoint);
}
