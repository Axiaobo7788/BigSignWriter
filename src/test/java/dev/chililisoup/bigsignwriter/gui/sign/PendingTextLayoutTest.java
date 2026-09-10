package dev.chililisoup.bigsignwriter.gui.sign;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PendingTextLayoutTest {
    @Test
    void continuationNeverCoversDoneOrTheFontRowAtSupportedGuiSizes() {
        for (int height : new int[]{240, 270, 300, 407, 427, 540, 720}) {
            int selector = height / 4 + 120;
            int done = height / 4 + 144;
            int row = PendingTextLayout.rowY(height, selector, done, 20);
            assertTrue(row >= 4 && row + 20 <= height - 4, "Outside viewport " + height);
            assertTrue(row + 20 <= selector || row >= selector + 20, "Font overlap " + height);
            assertTrue(row + 20 <= done || row >= done + 20, "Done overlap " + height);
        }
    }

    @Test
    void customControlPositionsStillReserveASeparateVisibleRow() {
        for (int selector : new int[]{4, 40, 120, 210}) {
            int row = PendingTextLayout.rowY(240, selector, 180, 20);
            assertTrue(row >= 4 && row + 20 <= 236);
            assertTrue(row + 20 <= selector || row >= selector + 20);
            assertTrue(row + 20 <= 180 || row >= 200);
        }
    }
}
