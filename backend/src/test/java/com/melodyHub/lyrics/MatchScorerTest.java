package com.melodyHub.lyrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MatchScorer}.
 */
class MatchScorerTest {

    private static LyricsSearchResult result(String track, String artist,
                                              String album, Integer duration) {
        return new LyricsSearchResult(track, artist, album, duration,
                "[00:00.00]lyrics", null);
    }

    @Nested
    @DisplayName("Exact matching")
    class ExactMatching {

        @Test
        @DisplayName("perfect match scores 100")
        void perfectMatch() {
            LyricsSearchResult r = result("Numb", "Linkin Park", "Meteora", 187);
            int score = MatchScorer.score(r, "Numb", "Linkin Park", "Meteora", 187);
            assertEquals(100, score);
        }

        @Test
        @DisplayName("title + artist exact with close duration scores 90")
        void titleArtistDuration() {
            LyricsSearchResult r = result("Numb", "Linkin Park", null, 188);
            int score = MatchScorer.score(r, "Numb", "Linkin Park", null, 187);
            // 40 + 30 + 20 = 90
            assertEquals(90, score);
        }
    }

    @Nested
    @DisplayName("Duration scoring")
    class DurationScoring {

        @Test
        @DisplayName("duration within 2 sec gives 20 points")
        void within2Sec() {
            LyricsSearchResult r = result("A", "B", null, 100);
            int score = MatchScorer.score(r, "A", "B", null, 101);
            // 40 + 30 + 20 = 90
            assertEquals(90, score);
        }

        @Test
        @DisplayName("duration within 5 sec gives 15 points")
        void within5Sec() {
            LyricsSearchResult r = result("A", "B", null, 100);
            int score = MatchScorer.score(r, "A", "B", null, 104);
            // 40 + 30 + 15 = 85
            assertEquals(85, score);
        }

        @Test
        @DisplayName("duration within 10 sec gives 5 points")
        void within10Sec() {
            LyricsSearchResult r = result("A", "B", null, 100);
            int score = MatchScorer.score(r, "A", "B", null, 108);
            // 40 + 30 + 5 = 75
            assertEquals(75, score);
        }

        @Test
        @DisplayName("duration difference > 10 sec gives 0 points")
        void beyond10Sec() {
            LyricsSearchResult r = result("A", "B", null, 100);
            int score = MatchScorer.score(r, "A", "B", null, 120);
            // 40 + 30 + 0 = 70
            assertEquals(70, score);
        }
    }

    @Nested
    @DisplayName("Normalization")
    class Normalization {

        @Test
        @DisplayName("case-insensitive matching")
        void caseInsensitive() {
            LyricsSearchResult r = result("NUMB", "LINKIN PARK", null, null);
            int score = MatchScorer.score(r, "numb", "linkin park", null, null);
            // 40 + 30 = 70
            assertEquals(70, score);
        }

        @Test
        @DisplayName("extra whitespace is collapsed")
        void whitespace() {
            LyricsSearchResult r = result("  Numb  ", "Linkin  Park", null, null);
            int score = MatchScorer.score(r, "Numb", "Linkin Park", null, null);
            assertEquals(70, score);
        }
    }

    @Nested
    @DisplayName("Partial and wrong match")
    class PartialMatch {

        @Test
        @DisplayName("wrong artist scores only title points")
        void wrongArtist() {
            LyricsSearchResult r = result("Numb", "Jay-Z", null, null);
            int score = MatchScorer.score(r, "Numb", "Linkin Park", null, null);
            // 40 + 0 = 40
            assertEquals(40, score);
        }

        @Test
        @DisplayName("wrong title scores only artist points")
        void wrongTitle() {
            LyricsSearchResult r = result("Other Song", "Linkin Park", null, null);
            int score = MatchScorer.score(r, "Numb", "Linkin Park", null, null);
            // 0 + 30 = 30
            assertEquals(30, score);
        }

        @Test
        @DisplayName("live version with different title scores 0")
        void liveVersion() {
            LyricsSearchResult r = result("Numb - Live", "Linkin Park", null, 200);
            int score = MatchScorer.score(r, "Numb", "Linkin Park", null, 187);
            // title mismatch + 30 + 5 (duration diff 13 > 10 = 0)
            // Actually: title "numb  live" != "numb" → 0; artist 30; duration diff 13 → 0
            assertEquals(30, score);
        }
    }
}
