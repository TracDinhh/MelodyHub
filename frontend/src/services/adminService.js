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
  }
};
