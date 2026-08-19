package com.melodyHub.lyrics;

import java.util.List;

/**
 * Abstraction for searching lyrics from external providers.
 * Implementations can be swapped (LRCLIB, Musixmatch, AI, etc.)
 * without changing the rest of the application.
 */
public interface LyricsProvider {

    /**
     * Searches for lyrics matching the given criteria.
     *
     * @param trackName  the song title (required)
     * @param artistName the performing artist (required)
     * @param albumName  the album name (optional, may be null)
     * @param durationSec the track duration in seconds (optional, may be null)
     * @return a list of matching results, or an empty list when nothing is found
     * @throws LyricsProviderException when the provider is unavailable or encounters an error
     */
    List<LyricsSearchResult> search(String trackName, String artistName,
                                     String albumName, Integer durationSec)
            throws LyricsProviderException;

    /**
     * Returns the display name of this provider (e.g. "LRCLIB").
     */
    String sourceName();
}
