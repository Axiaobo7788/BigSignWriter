package dev.chililisoup.bigsignwriter.input;

import java.util.function.ToIntFunction;

/** Validate an entire insertion before any vanilla sign row is changed. */
public final class SignInsertion {
    private SignInsertion() {}

    public static boolean fits(String[] rows, int maxWidth, ToIntFunction<String> width) {
        if (rows.length == 0 || maxWidth < 0) return false;
        for (String row : rows)
            if (row == null || row.length() > 384 || width.applyAsInt(row) > maxWidth) return false;
        return true;
    }
}
