package com.melodyHub.lyrics;

import java.util.List;

/**
 * Normalized response from lyrics lookup. This is what the frontend receives.
 * The frontend never sees raw LRCLIB JSON.
 */
public record LyricsLookupResponse(
        boolean found,
        String source,
        String matchType,     // "EXACT", "SEARCH", or null
        String errorCode,     // null on success
        List<Candidate> candidates
) {
    /** Single lyrics candidate (may be SYNCED or PLAIN). */
    public record Candidate(
            String trackName,
            String artistName,
            String albumName,
            Integer durationSec,
            String lyricsType,         // "SYNCED" or "PLAIN"
            LyricsData lyrics,         // non-null when lyricsType == "SYNCED"
            String plainLyrics,        // non-null when lyricsType == "PLAIN"
            int score
    ) {}

    /** Structured synced lyrics data. */
    public record LyricsData(
            String language,
            List<LyricsLine> lines
    ) {}

    /** A single synced lyrics line. */
    public record LyricsLine(
            double startTime,
            double endTime,
            String text
    ) {}

    /** Factory for "not found" response. */
    public static LyricsLookupResponse notFound(String source) {
        return new LyricsLookupResponse(false, source, null, null, List.of());
    }

    /** Factory for provider error response. */
    public static LyricsLookupResponse providerError(String source, String errorCode,
                                                      String message) {
        return new LyricsLookupResponse(false, source, null, errorCode, List.of());
    }
}
