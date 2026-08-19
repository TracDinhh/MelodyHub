package com.melodyHub.lyrics;

import com.melodyHub.lyrics.provider.LrclibLyricsProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Orchestrates lyrics lookup across one or more {@link LyricsProvider} instances.
 * <p>
 * Currently uses LRCLIB. Additional providers can be added in the future without
 * changing the servlet or frontend.
 */
public class LyricsLookupService {

    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_ARTIST_LENGTH = 255;
    private static final int MAX_ALBUM_LENGTH = 255;
    private static final int MAX_DURATION = 36_000;   // 10 hours
    private static final int MIN_SCORE_THRESHOLD = 30; // Below this, skip the result

    private final LyricsProvider provider;

    public LyricsLookupService() {
        this(new LrclibLyricsProvider());
    }

    public LyricsLookupService(LyricsProvider provider) {
        this.provider = provider;
    }

    /**
     * Searches for lyrics and returns a normalized response ready for the frontend.
     *
     * @throws IllegalArgumentException if required parameters are missing/invalid
     */
    public LyricsLookupResponse lookup(String title, String artist,
                                        String album, Integer duration) {
        // Validate
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (artist == null || artist.isBlank()) {
            throw new IllegalArgumentException("Artist is required");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Title is too long");
        }
        if (artist.length() > MAX_ARTIST_LENGTH) {
            throw new IllegalArgumentException("Artist name is too long");
        }
        if (album != null && album.length() > MAX_ALBUM_LENGTH) {
            throw new IllegalArgumentException("Album name is too long");
        }
        if (duration != null && (duration < 0 || duration > MAX_DURATION)) {
            throw new IllegalArgumentException("Duration must be between 0 and " + MAX_DURATION);
        }

        String cleanTitle = title.trim();
        String cleanArtist = artist.trim();
        String cleanAlbum = (album != null && !album.isBlank()) ? album.trim() : null;

        try {
            List<LyricsSearchResult> rawResults = provider.search(
                    cleanTitle, cleanArtist, cleanAlbum, duration);

            if (rawResults == null || rawResults.isEmpty()) {
                return LyricsLookupResponse.notFound(provider.sourceName());
            }

            // Score and sort results
            List<ScoredResult> scored = new ArrayList<>();
            for (LyricsSearchResult result : rawResults) {
                int score = MatchScorer.score(result, cleanTitle, cleanArtist,
                        cleanAlbum, duration);
                if (score >= MIN_SCORE_THRESHOLD) {
                    scored.add(new ScoredResult(result, score));
                }
            }

            if (scored.isEmpty()) {
                return LyricsLookupResponse.notFound(provider.sourceName());
            }

            // Sort by score descending
            scored.sort(Comparator.comparingInt(ScoredResult::score).reversed());

            // Determine match type
            boolean isExact = scored.size() == 1
                    && scored.get(0).score() >= MatchScorer.AUTO_SELECT_THRESHOLD;
            String matchType = isExact ? "EXACT" : "SEARCH";

            // Build response candidates
            List<LyricsLookupResponse.Candidate> candidates = new ArrayList<>();
            for (ScoredResult sr : scored) {
                LyricsSearchResult r = sr.result();
                String lyricsType;
                LyricsLookupResponse.LyricsData lyricsData = null;
                String plainLyrics = null;

                if (r.syncedLyrics() != null && !r.syncedLyrics().isBlank()) {
                    lyricsType = "SYNCED";
                    List<LrcParser.LrcLine> parsed = LrcParser.parse(r.syncedLyrics(), duration);
                    if (!parsed.isEmpty()) {
                        List<LyricsLookupResponse.LyricsLine> lines = parsed.stream()
                                .map(l -> new LyricsLookupResponse.LyricsLine(
                                        l.startTime(), l.endTime(), l.text()))
                                .toList();
                        lyricsData = new LyricsLookupResponse.LyricsData(null, lines);
                    } else {
                        // Synced lyrics failed to parse — fall back to plain if available
                        if (r.plainLyrics() != null && !r.plainLyrics().isBlank()) {
                            lyricsType = "PLAIN";
                            plainLyrics = r.plainLyrics();
                        } else {
                            continue; // Skip this result entirely
                        }
                    }
                } else if (r.plainLyrics() != null && !r.plainLyrics().isBlank()) {
                    lyricsType = "PLAIN";
                    plainLyrics = r.plainLyrics();
                } else {
                    continue; // no usable lyrics
                }

                candidates.add(new LyricsLookupResponse.Candidate(
                        r.trackName(), r.artistName(), r.albumName(),
                        r.durationSec(), lyricsType, lyricsData,
                        plainLyrics, sr.score()));
            }

            if (candidates.isEmpty()) {
                return LyricsLookupResponse.notFound(provider.sourceName());
            }

            return new LyricsLookupResponse(
                    true, provider.sourceName(), matchType, null, candidates);

        } catch (LyricsProviderException e) {
            return LyricsLookupResponse.providerError(
                    provider.sourceName(), e.getProviderCode(), e.getMessage());
        }
    }

    private record ScoredResult(LyricsSearchResult result, int score) {}
}
