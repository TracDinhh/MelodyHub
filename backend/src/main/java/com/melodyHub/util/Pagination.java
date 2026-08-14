package com.melodyHub.util;

/**
 * Pagination arithmetic shared across services. Computes the SQL {@code OFFSET}
 * as a {@code long} to avoid {@code int} overflow on very large page numbers,
 * clamping to {@link Integer#MAX_VALUE} rather than wrapping to a negative value.
 */
public final class Pagination {
    private Pagination() {
    }

    /** Returns {@code (page - 1) * size} clamped to a non-negative int. */
    public static int offset(int page, int size) {
        long offset = (long) (page - 1) * size;
        if (offset < 0) {
            return 0;
        }
        if (offset > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) offset;
    }
}
