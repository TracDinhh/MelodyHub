package com.melodyHub.dto.request;

import java.util.List;

/**
 * Request DTO for updating synced lyrics.
 */
public class SyncedLyricsRequest {
    
    private List<LyricLine> lines;
    
    public SyncedLyricsRequest() {}
    
    public SyncedLyricsRequest(List<LyricLine> lines) {
        this.lines = lines;
    }
    
    public List<LyricLine> getLines() {
        return lines;
    }
    
    public void setLines(List<LyricLine> lines) {
        this.lines = lines;
    }
    
    public record LyricLine(
        double startTime,
        double endTime,
        String text
    ) {}
}
