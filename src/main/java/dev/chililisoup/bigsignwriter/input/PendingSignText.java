package dev.chililisoup.bigsignwriter.input;

import dev.chililisoup.bigsignwriter.font.UnicodeCodePoints;

import java.util.ArrayDeque;
import java.util.OptionalInt;
import java.util.function.IntPredicate;

/** Session-only continuation across physical signs. A glyph rejected for lack of space stays at the front. */
public final class PendingSignText {
    public static final int LIMIT = 4096;
    private final ArrayDeque<Integer> pending = new ArrayDeque<>();

    public boolean append(String text) {
        int[] codePoints = text.codePoints().filter(cp -> !Character.isISOControl(cp)).toArray();
        if (codePoints.length + this.pending.size() > LIMIT) return false;
        for (int cp : codePoints) if (!UnicodeCodePoints.isScalar(cp)) return false;
        for (int cp : codePoints) this.pending.addLast(cp);
        return true;
    }

    public int drain(IntPredicate insert) {
        int count = 0;
        while (!this.pending.isEmpty() && insert.test(this.pending.getFirst())) {
            this.pending.removeFirst();
            count++;
        }
        return count;
    }

    public int size() { return this.pending.size(); }
    public OptionalInt nextCodePoint() {
        return this.pending.isEmpty() ? OptionalInt.empty() : OptionalInt.of(this.pending.getFirst());
    }
    public OptionalInt nextCodePoint(IntPredicate predicate) {
        return this.pending.stream().mapToInt(Integer::intValue).filter(predicate).findFirst();
    }
    public boolean isEmpty() { return this.pending.isEmpty(); }
    public void clear() { this.pending.clear(); }
    public void backspace() { this.pending.pollLast(); }
    public String preview() {
        StringBuilder text = new StringBuilder();
        this.pending.stream().limit(32).forEach(text::appendCodePoint);
        if (this.pending.size() > 32) text.append('…');
        return text.toString();
    }
}
