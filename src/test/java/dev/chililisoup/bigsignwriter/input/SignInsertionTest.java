package dev.chililisoup.bigsignwriter.input;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SignInsertionTest {
    @Test
    void rejectsAllRowsWhenAnyRowOverflows() {
        assertFalse(SignInsertion.fits(new String[]{"A", "too wide", "A", "A"}, 5, String::length));
        assertTrue(SignInsertion.fits(new String[]{"12345", "12345", "12345", "12345"}, 5, String::length));
        assertFalse(SignInsertion.fits(new String[]{"ok", null}, 90, String::length));
        assertFalse(SignInsertion.fits(new String[]{"x".repeat(385)}, 900, String::length));
    }
}
