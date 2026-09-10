package dev.chililisoup.bigsignwriter.input;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class PendingSignTextTest {
    @Test
    void fullPhraseContinuesAcrossFourPhysicalSignsWithoutDroppingText() {
        PendingSignText queue = new PendingSignText();
        assertTrue(queue.append("你好世界"));
        ArrayList<Integer> written = new ArrayList<>();
        for (int sign = 0; sign < 4; sign++) {
            int initialSize = written.size();
            assertEquals(1, queue.drain(cp -> {
                if (written.size() != initialSize) return false;
                written.add(cp);
                return true;
            }));
            assertEquals(3 - sign, queue.size());
        }
        assertEquals("你好世界", written.stream().collect(StringBuilder::new, StringBuilder::appendCodePoint,
                StringBuilder::append).toString());
    }

    @Test
    void rejectionAndEditingPreserveSupplementaryCodePoints() {
        PendingSignText queue = new PendingSignText();
        assertTrue(queue.append("米𠀀"));
        assertEquals((int) '米', queue.nextCodePoint().orElseThrow());
        assertEquals(0x20000, queue.nextCodePoint(cp -> cp > 0xffff).orElseThrow());
        assertTrue(queue.nextCodePoint(cp -> cp == 'A').isEmpty());
        assertEquals(2, queue.size(), "Reading the next glyph must not consume pending text");
        assertEquals(0, queue.drain(cp -> false));
        assertEquals(2, queue.size());
        queue.backspace();
        assertEquals("米", queue.preview());
        queue.clear();
        assertTrue(queue.isEmpty());
        assertTrue(queue.nextCodePoint().isEmpty());
    }

    @Test
    void rejectsAnInvalidOrOversizedAppendAtomically() {
        PendingSignText queue = new PendingSignText();
        assertTrue(queue.append("米"));
        assertFalse(queue.append("x".repeat(PendingSignText.LIMIT)));
        assertFalse(queue.append("A\uD840"));
        assertEquals("米", queue.preview());
    }
}
