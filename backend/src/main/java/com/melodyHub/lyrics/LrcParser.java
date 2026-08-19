package com.melodyHub.lyrics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses LRC-format synced lyrics into structured lines.
 * <p>
 * Supported timestamp formats:
 * <ul>
 *   <li>{@code [mm:ss.xx]}   – two-digit fractional seconds</li>
 *   <li>{@code [mm:ss.xxx]}  – three-digit fractional seconds</li>
 * </ul>
 * Metadata tags ({@code [ar:], [ti:], [al:], [by:], [offset:]}) are ignored.
 * Malformed lines are silently skipped.
 */
public final class LrcParser {

    /** Matches [mm:ss.xx] or [mm:ss.xxx] followed by text. */
    private static final Pattern LINE_PATTERN =
            Pattern.compile("^\\[(\\d{1,3}):(\\d{2})[.:](\\d{2,3})]\\s*(.*)$");

    /** Matches metadata tags like [ar:Artist Name]. */
    private static final Pattern METADATA_PATTERN =
            Pattern.compile("^\\[(ar|ti|al|by|offset|re|ve):");

    private static final double DEFAULT_END_TIME_OFFSET = 4.0;

    private LrcParser() {}

    /**
     * Parses raw LRC text into a list of lyric lines with startTime/endTime in seconds.
     *
     * @param lrcText     the raw LRC string from the provider
     * @param durationSec total track duration in seconds; used for the last line's endTime
     * @return sorted list of parsed lines; empty list if input is null/empty
     */
    public static List<LrcLine> parse(String lrcText, Integer durationSec) {
        if (lrcText == null || lrcText.isBlank()) {
            return List.of();
        }

        List<LrcLine> lines = new ArrayList<>();
        String[] rawLines = lrcText.split("\\r?\\n");

        for (String raw : rawLines) {
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            // Skip metadata lines
            if (METADATA_PATTERN.matcher(trimmed).find()) {
                continue;
            }

            Matcher matcher = LINE_PATTERN.matcher(trimmed);
            if (!matcher.matches()) {
                continue; // malformed – skip silently
            }

            int minutes = Integer.parseInt(matcher.group(1));
            int seconds = Integer.parseInt(matcher.group(2));
            String fractionalStr = matcher.group(3);
            String text = matcher.group(4).trim();

            // Skip empty text lines
            if (text.isEmpty()) {
                continue;
            }

            double fractional;
            if (fractionalStr.length() == 3) {
                fractional = Integer.parseInt(fractionalStr) / 1000.0;
            } else {
                fractional = Integer.parseInt(fractionalStr) / 100.0;
            }

            double startTime = minutes * 60.0 + seconds + fractional;
            // Round to 2 decimal places to avoid floating-point noise
            startTime = Math.round(startTime * 100.0) / 100.0;

            lines.add(new LrcLine(startTime, 0, text));
        }

        // Sort by startTime
        lines.sort(Comparator.comparingDouble(LrcLine::startTime));

        // Compute endTime: next line's startTime, or duration for the last line
        for (int i = 0; i < lines.size(); i++) {
            double endTime;
            if (i < lines.size() - 1) {
                endTime = lines.get(i + 1).startTime();
            } else {
                // Last line
                if (durationSec != null && durationSec > 0
                        && durationSec > lines.get(i).startTime()) {
                    endTime = durationSec;
                } else {
                    endTime = lines.get(i).startTime() + DEFAULT_END_TIME_OFFSET;
                }
            }
            endTime = Math.round(endTime * 100.0) / 100.0;
            lines.set(i, new LrcLine(lines.get(i).startTime(), endTime, lines.get(i).text()));
        }

        return lines;
    }

    /**
     * Represents a single parsed lyric line.
     *
     * @param startTime start time in seconds
     * @param endTime   end time in seconds
     * @param text      the lyric text
     */
    public record LrcLine(double startTime, double endTime, String text) {}
}
