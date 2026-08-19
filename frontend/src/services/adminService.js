import { apiClient } from './http';

export const adminService = {
  /**
   * List artist requests by status (defaults to PENDING). Returns a PagedResponse.
   */
  listArtistRequests({ status = 'PENDING', page = 1, size = 20 } = {}) {
    return apiClient.get('/api/admin/artist-requests', {
      params: { status, page, size }
    });
  },

  /**
   * Approve a pending request: promotes the user to ARTIST and creates their profile.
   */
  approveArtistRequest(id) {
    return apiClient.post(`/api/admin/artist-requests/${id}/approve`);
  },

  /**
   * Reject a pending request with an optional note.
   */
  rejectArtistRequest(id, reviewNote) {
    return apiClient.post(`/api/admin/artist-requests/${id}/reject`, { reviewNote });
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
   * List all songs, optionally filtered by status and search query.
   */
  listSongs({ status = '', q = '', page = 1, size = 20 } = {}) {
    return apiClient.get('/api/admin/songs', { params: { status, q, page, size } });
  },

  /**
   * Update a song's status (PUBLISHED, HIDDEN, DRAFT).
   */
  updateSongStatus(songId, status) {
    return apiClient.post(`/api/admin/songs/${songId}/status`, { status });
  },

  /**
   * Soft-delete a song.
   */
  deleteSong(songId) {
    return apiClient.delete(`/api/admin/songs/${songId}`);
  }
};
