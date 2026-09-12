package dev.chililisoup.bigsignwriter.gui.sign;

/** A separate 20px toolbar row; the vanilla Done button keeps its original bounds. */
public final class PendingTextLayout {
    private static final int HEIGHT = 20;
    private static final int GAP = 6;
    private static final int MARGIN = 4;

    private PendingTextLayout() {}

    public static int rowY(int screenHeight, int selectorY, int doneY, int doneHeight) {
        int below = Math.max(selectorY + HEIGHT, doneY + doneHeight) + GAP;
        if (below + HEIGHT <= screenHeight - MARGIN && below >= MARGIN) return below;
        // At the minimum GUI height the sign's wooden post reaches the font row.
        // Use the free band above the title instead of covering the sign preview.
        int above = Math.min(12, Math.min(selectorY, doneY) - HEIGHT - GAP);
        if (above >= MARGIN && above + HEIGHT <= screenHeight - MARGIN) return above;
        // Custom toolbar positions may split the free space; find a remaining band.
        for (int y = screenHeight - MARGIN - HEIGHT; y >= MARGIN; y--) {
            if (separate(y, selectorY, HEIGHT) && separate(y, doneY, doneHeight)) return y;
        }
        return -1;
    }

    private static boolean separate(int y, int otherY, int otherHeight) {
        return y + HEIGHT + GAP <= otherY || y >= otherY + otherHeight + GAP;
    }
}
