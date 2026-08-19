package com.melodyHub.lyrics;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Scores how closely a {@link LyricsSearchResult} matches the artist's query.
 */
public final class MatchScorer {

    private static final int TITLE_EXACT = 40;
    private static final int ARTIST_EXACT = 30;
    private static final int ALBUM_EXACT = 10;
    private static final int DURATION_MATCH_2 = 20;
    private static final int DURATION_MATCH_5 = 15;
    private static final int DURATION_MATCH_10 = 5;

    /** Results with a score below this threshold should not be auto-selected. */
    public static final int AUTO_SELECT_THRESHOLD = 90;

    /** Precompiled pattern for non-alphanumeric/space characters. */
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9\\s]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private MatchScorer() {}

    /**
     * Computes a match score (0–100) for a result against the query parameters.
     */
    public static int score(LyricsSearchResult result,
                            String queryTitle, String queryArtist,
                            String queryAlbum, Integer queryDuration) {
        int score = 0;

        if (normalize(result.trackName()).equals(normalize(queryTitle))) {
            score += TITLE_EXACT;
        }
        if (normalize(result.artistName()).equals(normalize(queryArtist))) {
            score += ARTIST_EXACT;
        }
        if (queryAlbum != null && result.albumName() != null
                && normalize(result.albumName()).equals(normalize(queryAlbum))) {
            score += ALBUM_EXACT;
        }
        if (queryDuration != null && result.durationSec() != null) {
            int diff = Math.abs(queryDuration - result.durationSec());
            if (diff <= 2) {
                score += DURATION_MATCH_2;
            } else if (diff <= 5) {
                score += DURATION_MATCH_5;
            } else if (diff <= 10) {
                score += DURATION_MATCH_10;
            }
        }

        return Math.min(score, 100);
    }

    /**
     * Normalizes a string for comparison: lowercase, Unicode decomposed,
     * collapse whitespace, strip non-alphanumeric.
     */
    static String normalize(String value) {
        if (value == null) return "";
        String lower = value.toLowerCase().trim();
        // Decompose unicode and remove combining marks
        lower = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        lower = NON_ALNUM.matcher(lower).replaceAll("");
        lower = WHITESPACE.matcher(lower).replaceAll(" ").trim();
        return lower;
    }
}
