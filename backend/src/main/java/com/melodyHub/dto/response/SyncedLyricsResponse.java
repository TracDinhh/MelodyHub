package com.melodyHub.dto.response;

import java.util.List;

/**
 * Response DTO for synced lyrics with timestamps.
 * startTime and endTime are in seconds.
 */
public record SyncedLyricsResponse(
    int songId,
    String lyricsType,
    List<LyricLine> lines
) {
    public record LyricLine(
        double startTime,
        double endTime,
        String text
    ) {}
}
