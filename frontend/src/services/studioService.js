import { apiClient } from './http';

/**
 * Studio API client. All management calls require an authenticated user who is
 * a member of the artist in the URL path; artistId is always part of the path.
 */
export const studioService = {
  /**
   * Artists the current user is a member of. Used by the Studio entry view.
   */
  getMyArtists() {
    return apiClient.get('/api/me/artists');
  },

  /**
   * Public artist search, used by the CLAIM flow to find existing artists.
   */
  searchArtists(q) {
    return apiClient.get('/api/artists/search', {
      params: { q },
      authenticated: false
    });
  },

  // ─── Artist profile ──────────────────────────────────────────────────────

  getProfile(artistId) {
    return apiClient.get(`/api/studio/artists/${artistId}/profile`);
  },

  updateProfile(artistId, payload) {
    return apiClient.put(`/api/studio/artists/${artistId}/profile`, payload);
  },

  // ─── Stats ───────────────────────────────────────────────────────────────

  getStats(artistId) {
    return apiClient.get(`/api/studio/artists/${artistId}/stats`);
  },

  getAnalytics(artistId) {
    return apiClient.get(`/api/studio/artists/${artistId}/analytics`);
  },

  // ─── Songs ───────────────────────────────────────────────────────────────

  listSongs(artistId, params) {
    return apiClient.get(`/api/studio/artists/${artistId}/songs`, { params });
  },

  getSong(artistId, songId) {
    return apiClient.get(`/api/studio/artists/${artistId}/songs/${songId}`);
  },

  createSong(artistId, payload) {
    return apiClient.post(`/api/studio/artists/${artistId}/songs`, payload);
  },

  updateSong(artistId, songId, payload) {
    return apiClient.put(`/api/studio/artists/${artistId}/songs/${songId}`, payload);
  },

  /**
   * Submit a DRAFT/REJECTED song for admin review. Moves it to SUBMITTED.
   */
  submitForReview(artistId, songId) {
    return apiClient.post(`/api/studio/artists/${artistId}/songs/${songId}/submit`);
  },

  updateSyncedLyrics(artistId, songId, payload) {
    return apiClient.put(`/api/studio/artists/${artistId}/songs/${songId}/lyrics`, payload);
  },

  /**
   * Public synced-lyrics lookup used by the edit form. Sent with the bearer
   * token when present (synced lyrics are premium-gated).
   */
  getSyncedLyrics(slug) {
    return apiClient.get(`/api/songs/${encodeURIComponent(slug)}/lyrics`);
  }
};