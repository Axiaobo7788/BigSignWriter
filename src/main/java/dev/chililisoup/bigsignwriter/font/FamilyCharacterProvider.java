package dev.chililisoup.bigsignwriter.font;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface FamilyCharacterProvider {
    Map<Integer, String[]> characters();

    default @Nullable BigGlyphProvider glyphProvider() { return null; }

    default java.util.Optional<String[]> ownGlyph(int codePoint) {
        String[] exact = this.characters().get(codePoint);
        if (exact != null) return java.util.Optional.of(exact);
        BigGlyphProvider dynamic = this.glyphProvider();
        return dynamic != null ? dynamic.getGlyph(codePoint) : java.util.Optional.empty();
    }

    boolean parentIsImplicit();

    @Nullable FamilyCharacterProvider parentFont();
}
