package com.melodyHub.lyrics;

import java.util.List;

/**
 * A single lyrics result from a provider. Contains both raw synced LRC text
 * and plain lyrics when available.
 */
public record LyricsSearchResult(
        String trackName,
        String artistName,
        String albumName,
        Integer durationSec,
        String syncedLyrics,   
        String plainLyrics    
) {}
