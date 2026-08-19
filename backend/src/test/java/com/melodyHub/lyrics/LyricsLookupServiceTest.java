package com.melodyHub.lyrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LyricsLookupService} using a fake in-memory provider.
 */
class LyricsLookupServiceTest {

    /**
     * Fake provider that returns configurable results.
     */
    static class FakeProvider implements LyricsProvider {
        List<LyricsSearchResult> results = List.of();
        LyricsProviderException exception = null;

        @Override
        public String sourceName() {
            return "FAKE";
        }

        @Override
        public List<LyricsSearchResult> search(String trackName, String artistName,
                                                 String albumName, Integer durationSec)
                throws LyricsProviderException {
            if (exception != null) throw exception;
            return results;
        }
    }

    @Nested
    @DisplayName("Input validation")
    class Validation {

        @Test
        @DisplayName("throws when title is null")
        void nullTitle() {
            LyricsLookupService service = new LyricsLookupService(new FakeProvider());
            assertThrows(IllegalArgumentException.class,
                    () -> service.lookup(null, "Artist", null, null));
        }

        @Test
        @DisplayName("throws when artist is blank")
        void blankArtist() {
            LyricsLookupService service = new LyricsLookupService(new FakeProvider());
            assertThrows(IllegalArgumentException.class,
                    () -> service.lookup("Title", "  ", null, null));
        }

        @Test
        @DisplayName("throws when duration is negative")
        void negativeDuration() {
            LyricsLookupService service = new LyricsLookupService(new FakeProvider());
            assertThrows(IllegalArgumentException.class,
                    () -> service.lookup("Title", "Artist", null, -5));
        }
    }

    @Nested
    @DisplayName("Search results")
    class SearchResults {

        @Test
        @DisplayName("returns not-found when provider returns empty list")
        void notFound() {
            FakeProvider provider = new FakeProvider();
            provider.results = List.of();

            LyricsLookupService service = new LyricsLookupService(provider);
            LyricsLookupResponse response = service.lookup("Song", "Artist", null, 200);

            assertFalse(response.found());
            assertEquals("FAKE", response.source());
            assertTrue(response.candidates().isEmpty());
        }

        @Test
        @DisplayName("returns synced lyrics when available")
        void syncedLyrics() {
            FakeProvider provider = new FakeProvider();
            provider.results = List.of(
                    new LyricsSearchResult("Song", "Artist", "Album", 200,
                            "[00:05.00]Hello\n[00:10.00]World", null)
            );

            LyricsLookupService service = new LyricsLookupService(provider);
            LyricsLookupResponse response = service.lookup("Song", "Artist", "Album", 200);

            assertTrue(response.found());
            assertEquals(1, response.candidates().size());

            LyricsLookupResponse.Candidate candidate = response.candidates().get(0);
            assertEquals("SYNCED", candidate.lyricsType());
            assertNotNull(candidate.lyrics());
            assertEquals(2, candidate.lyrics().lines().size());
            assertEquals("Hello", candidate.lyrics().lines().get(0).text());
        }

        @Test
        @DisplayName("falls back to plain lyrics when synced is null")
        void plainFallback() {
            FakeProvider provider = new FakeProvider();
            provider.results = List.of(
                    new LyricsSearchResult("Song", "Artist", null, 200,
                            null, "Line 1\nLine 2\nLine 3")
            );

            LyricsLookupService service = new LyricsLookupService(provider);
            LyricsLookupResponse response = service.lookup("Song", "Artist", null, 200);

            assertTrue(response.found());
            assertEquals("PLAIN", response.candidates().get(0).lyricsType());
            assertEquals("Line 1\nLine 2\nLine 3", response.candidates().get(0).plainLyrics());
        }

        @Test
        @DisplayName("handles provider exception gracefully")
        void providerException() {
            FakeProvider provider = new FakeProvider();
            provider.exception = new LyricsProviderException("PROVIDER_UNAVAILABLE",
                    "Connection refused");

            LyricsLookupService service = new LyricsLookupService(provider);
            LyricsLookupResponse response = service.lookup("Song", "Artist", null, null);

            assertFalse(response.found());
            assertEquals("PROVIDER_UNAVAILABLE", response.errorCode());
        }

        @Test
        @DisplayName("filters out low-score results")
        void filtersLowScore() {
            FakeProvider provider = new FakeProvider();
            // Title and artist both mismatch → score should be very low
            provider.results = List.of(
                    new LyricsSearchResult("Completely Different", "Other Artist", null, 200,
                            "[00:05.00]Hello", null)
            );

            LyricsLookupService service = new LyricsLookupService(provider);
            LyricsLookupResponse response = service.lookup("My Song", "My Artist", null, 200);

            // Score: title=0, artist=0, duration=20 → 20, below threshold of 30
            assertFalse(response.found());
        }
    }
}
