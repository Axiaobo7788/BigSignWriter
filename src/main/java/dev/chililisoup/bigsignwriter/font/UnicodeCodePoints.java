package dev.chililisoup.bigsignwriter.font;

/** Character identity is a Unicode scalar, never a UTF-16 code unit. */
public final class UnicodeCodePoints {
    private UnicodeCodePoints() {}

    public static boolean isScalar(int codePoint) {
        return Character.isValidCodePoint(codePoint)
                && (codePoint < Character.MIN_SURROGATE || codePoint > Character.MAX_SURROGATE);
    }

    public static int fromKey(String key) {
        if (key.isEmpty() || key.codePointCount(0, key.length()) != 1 || !isScalar(key.codePointAt(0)))
            throw new IllegalArgumentException("Character key must contain exactly one Unicode scalar");
        return key.codePointAt(0);
    }

    public static String toKey(int codePoint) {
        if (!isScalar(codePoint)) throw new IllegalArgumentException("Invalid Unicode scalar: " + codePoint);
        return new String(Character.toChars(codePoint));
    }

    /** Minecraft caret positions are UTF-16 offsets; keep them out of a surrogate pair. */
    public static int floorBoundary(String text, int offset) {
        int index = Math.max(0, Math.min(offset, text.length()));
        if (index > 0 && index < text.length() && Character.isHighSurrogate(text.charAt(index - 1))
                && Character.isLowSurrogate(text.charAt(index))) return index - 1;
        return index;
    }
}
