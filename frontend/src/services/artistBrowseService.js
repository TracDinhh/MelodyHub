import { apiClient } from './http';

export const artistBrowseService = {
  list(params) {
    return apiClient.get('/api/artists', { params, authenticated: false });
  },

  /**
   * Artist detail. Sent with the bearer token when present so the response can
   * include the current user's `following` state; anonymous requests work too.
   */
  getBySlug(slug) {
    return apiClient.get(`/api/artists/${encodeURIComponent(slug)}`);
  },

  getSongs(slug, params) {
    return apiClient.get(`/api/artists/${encodeURIComponent(slug)}/songs`, {
      params,
      authenticated: false
    });
  }
};
