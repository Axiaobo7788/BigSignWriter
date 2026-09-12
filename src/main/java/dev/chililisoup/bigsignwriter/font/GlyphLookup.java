package dev.chililisoup.bigsignwriter.font;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;

/** Preserves exact/static/dynamic and explicit-parent/uppercase precedence. */
public final class GlyphLookup {
    private GlyphLookup() {}

    public static Optional<String[]> find(int codePoint, FamilyCharacterProvider font) {
        if (!UnicodeCodePoints.isScalar(codePoint)) return Optional.empty();
        String[] fallback = null;
        Set<FamilyCharacterProvider> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while (font != null && seen.add(font)) {
            Optional<String[]> exact = font.ownGlyph(codePoint);
            if (exact.isPresent()) return exact;
            if (fallback == null) {
                int upper = Character.toUpperCase(codePoint);
                if (upper != codePoint) fallback = font.ownGlyph(upper).orElse(null);
                if (fallback != null && font.parentIsImplicit()) return Optional.of(fallback);
            }
            font = font.parentFont();
        }
        return Optional.ofNullable(fallback);
    }
}
