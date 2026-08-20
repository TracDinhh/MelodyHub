import { apiClient } from './http';

export const adminService = {
  /**
   * List artist access requests by status (defaults to PENDING). Returns a PagedResponse.
   */
  listArtistRequests({ status = 'PENDING', page = 1, size = 20 } = {}) {
    return apiClient.get('/api/admin/artist-access-requests', {
      params: { status, page, size }
    });
  },

  /**
   * Approve a pending access request (CLAIM/CREATE). Creates the membership and,
   * for CREATE_ARTIST, the artist profile in a single transaction.
   */
  approveArtistRequest(id) {
    return apiClient.post(`/api/admin/artist-access-requests/${id}/approve`);
  },

  /**
   * Reject a pending access request with an optional note.
   */
  rejectArtistRequest(id, reviewNote) {
    return apiClient.post(`/api/admin/artist-access-requests/${id}/reject`, { reviewNote });
  },

  /**
   * List users, optionally filtered by role and a search query.
   */
  listUsers({ role = '', q = '', page = 1, size = 50 } = {}) {
    return apiClient.get('/api/admin/users', { params: { role, q, page, size } });
  },

  /**
   * List artists, optionally filtered by a search query.
   */
  listArtists({ q = '', page = 1, size = 50 } = {}) {
    return apiClient.get('/api/admin/artists', { params: { q, page, size } });
  },

  /**
   * Aggregate counts for the admin overview dashboard.
   */
  getStats() {
    return apiClient.get('/api/admin/stats');
  },

  /**
   * Detailed analytics (time series + distributions) for the dashboard charts.
   */
  getAnalytics() {
    return apiClient.get('/api/admin/analytics');
  },

  /**
   * List all songs, optionally filtered by status, search query, and sort order.
   */
  listSongs({ status = '', q = '', sort = 'newest', page = 1, size = 20 } = {}) {
    return apiClient.get('/api/admin/songs', { params: { status, q, sort, page, size } });
  },

  /**
   * Get per-status song counts for admin summary badges.
   */
  getSongStatusCounts() {
    return apiClient.get('/api/admin/songs/counts');
  },

  /**
   * Update a song's status (PUBLISHED ↔ HIDDEN only).
   */
  updateSongStatus(songId, status) {
    return apiClient.post(`/api/admin/songs/${songId}/status`, { status });
  },

  /**
   * Approve a SUBMITTED song; publishes it to the catalog.
   */
  approveSong(songId) {
    return apiClient.post(`/api/admin/songs/${songId}/approve`);
  },

  /**
   * Reject a SUBMITTED song with a reason the artist will see.
   */
  rejectSong(songId, reviewNote) {
    return apiClient.post(`/api/admin/songs/${songId}/reject`, { reviewNote });
  },

  /**
   * Soft-delete a song.
   */
  deleteSong(songId) {
    return apiClient.delete(`/api/admin/songs/${songId}`);
  }
};
