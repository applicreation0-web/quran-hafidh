package com.quransafeguard.hifz.preview;

import java.util.function.IntUnaryOperator;

/**
 * Pure, read-only cursor for the compact Cap Hifdh forecast navigator.
 * Skips empty forecast years without inventing events, while allowing the
 * independent year controls to display an empty year explicitly.
 */
final class CapHifzNavigation {
    private CapHifzNavigation() {}

    static final class Cursor {
        final int year;
        final int index;

        Cursor(int year, int index) {
            this.year = year;
            this.index = index;
        }
    }

    static Cursor move(int year, int index, int direction, int firstYear, int lastYear,
            IntUnaryOperator checkpointCount) {
        if ((direction != 1 && direction != -1) || firstYear > lastYear
                || year < firstYear || year > lastYear || index < 0
                || checkpointCount == null)
            throw new IllegalArgumentException("Invalid forecast navigation");
        int count = Math.max(0, checkpointCount.applyAsInt(year));
        int candidate = index + direction;
        if (candidate >= 0 && candidate < count) return new Cursor(year, candidate);
        for (int y = year + direction; y >= firstYear && y <= lastYear; y += direction) {
            int available = Math.max(0, checkpointCount.applyAsInt(y));
            if (available > 0) return new Cursor(y, direction == 1 ? 0 : available - 1);
        }
        return new Cursor(year, index);
    }

    static boolean canMove(Cursor current, int direction, int firstYear, int lastYear,
            IntUnaryOperator checkpointCount) {
        Cursor next = move(current.year, current.index, direction,
            firstYear, lastYear, checkpointCount);
        return next.year != current.year || next.index != current.index;
    }
}
