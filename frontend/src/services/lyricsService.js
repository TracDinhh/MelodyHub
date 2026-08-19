import { apiClient } from './http';

/**
 * Service for lyrics lookup via the MelodyHub backend.
 * Frontend never calls LRCLIB directly.
 */
export const lyricsService = {
  /**
   * Searches for lyrics from external providers through the backend.
   *
   * @param {{ title: string, artist: string, album?: string, duration?: number }} params
   * @returns {Promise<import('../types/lyrics').LyricsLookupResponse>}
   */
  searchLyrics({ title, artist, album, duration }) {
    return apiClient.get('/api/artist/lyrics/search', {
      params: { title, artist, album, duration }
    });
  }
};
