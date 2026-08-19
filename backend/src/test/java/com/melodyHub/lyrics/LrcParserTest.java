package com.melodyHub.lyrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LrcParser}.
 */
class LrcParserTest {

    @Nested
    @DisplayName("Basic parsing")
    class BasicParsing {

        @Test
        @DisplayName("parses standard LRC lines with [mm:ss.xx] timestamps")
        void parsesStandardLines() {
            String lrc = """
                    [00:12.50]Line one
                    [00:16.80]Line two
                    [00:21.40]Line three
                    """;

            List<LrcParser.LrcLine> lines = LrcParser.parse(lrc, 180);

            assertEquals(3, lines.size());

            assertEquals(12.5, lines.get(0).startTime(), 0.01);
            assertEquals(16.8, lines.get(0).endTime(), 0.01);
            assertEquals("Line one", lines.get(0).text());

            assertEquals(16.8, lines.get(1).startTime(), 0.01);
            assertEquals(21.4, lines.get(1).endTime(), 0.01);
            assertEquals("Line two", lines.get(1).text());

            assertEquals(21.4, lines.get(2).startTime(), 0.01);
            // Last line endTime = duration
            assertEquals(180.0, lines.get(2).endTime(), 0.01);
            assertEquals("Line three", lines.get(2).text());
        }

        @Test
        @DisplayName("parses [mm:ss.xxx] three-digit millisecond timestamps")
        void parsesThreeDigitMilliseconds() {
            String lrc = "[01:30.456]Hello world\n[02:00.100]Goodbye";

            List<LrcParser.LrcLine> lines = LrcParser.parse(lrc, 200);

            assertEquals(2, lines.size());
            assertEquals(90.456, lines.get(0).startTime(), 0.01);
            assertEquals(120.1, lines.get(1).startTime(), 0.01);
        }

        @Test
        @DisplayName("returns empty list for null input")
        void returnsEmptyForNull() {
            assertTrue(LrcParser.parse(null, 100).isEmpty());
        }

        @Test
        @DisplayName("returns empty list for blank input")
        void returnsEmptyForBlank() {
            assertTrue(LrcParser.parse("   ", 100).isEmpty());
        }
    }

    @Nested
    @DisplayName("Metadata and malformed lines")
    class MetadataAndMalformed {

        @Test
        @DisplayName("ignores metadata tags")
        void ignoresMetadata() {
            String lrc = """
                    [ar:Linkin Park]
                    [ti:Numb]
                    [al:Meteora]
                    [by:Someone]
                    [offset:100]
                    [00:05.00]Actual lyrics
                    """;

            List<LrcParser.LrcLine> lines = LrcParser.parse(lrc, 200);

            assertEquals(1, lines.size());
            assertEquals("Actual lyrics", lines.get(0).text());
        }

        @Test
        @DisplayName("ignores malformed lines without crashing")
        void ignoresMalformedLines() {
            String lrc = """
                    This is not LRC
                    [bad timestamp]
                    [00:05.00]Valid line
                    [xx:yy.zz]Invalid
                    [00:10.00]Another valid line
                    """;

            List<LrcParser.LrcLine> lines = LrcParser.parse(lrc, 100);

            assertEquals(2, lines.size());
            assertEquals("Valid line", lines.get(0).text());
            assertEquals("Another valid line", lines.get(1).text());
        }

        @Test
        @DisplayName("skips empty text lines")
        void skipsEmptyTextLines() {
            String lrc = """
                    [00:05.00]First line
                    [00:10.00]
                    [00:15.00]Third line
                    """;

            List<LrcParser.LrcLine> lines = LrcParser.parse(lrc, 100);

            assertEquals(2, lines.size());
            assertEquals("First line", lines.get(0).text());
            assertEquals("Third line", lines.get(1).text());
            // endTime of first should be startTime of third (skipping empty)
            assertEquals(15.0, lines.get(0).endTime(), 0.01);
        }
    }

    @Nested
    @DisplayName("Sorting and endTime")
    class SortingAndEndTime {

        @Test
        @DisplayName("sorts unsorted input by startTime")
        void sortsUnsortedInput() {
            String lrc = """
                    [00:20.00]Third
                    [00:05.00]First
                    [00:10.00]Second
                    """;

            List<LrcParser.LrcLine> lines = LrcParser.parse(lrc, 100);

            assertEquals(3, lines.size());
            assertEquals("First", lines.get(0).text());
            assertEquals("Second", lines.get(1).text());
            assertEquals("Third", lines.get(2).text());
        }

        @Test
        @DisplayName("last line uses fallback endTime when no duration")
        void lastLineFallbackEndTime() {
            String lrc = "[00:10.00]Only line";

            List<LrcParser.LrcLine> lines = LrcParser.parse(lrc, null);

            assertEquals(1, lines.size());
            // 10.0 + 4.0 (default fallback)
            assertEquals(14.0, lines.get(0).endTime(), 0.01);
        }

        @Test
        @DisplayName("last line uses duration when available and valid")
        void lastLineUsesDuration() {
            String lrc = "[03:00.00]Final line";

            List<LrcParser.LrcLine> lines = LrcParser.parse(lrc, 200);

            assertEquals(1, lines.size());
            assertEquals(200.0, lines.get(0).endTime(), 0.01);
        }

        @Test
        @DisplayName("last line uses fallback when duration is less than startTime")
        void lastLineFallbackWhenDurationTooSmall() {
            String lrc = "[03:00.00]Final line";

            List<LrcParser.LrcLine> lines = LrcParser.parse(lrc, 100);

            assertEquals(1, lines.size());
            // 180 + 4 = 184 (fallback because duration 100 < startTime 180)
            assertEquals(184.0, lines.get(0).endTime(), 0.01);
        }
    }
}
