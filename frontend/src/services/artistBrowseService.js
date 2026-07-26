import { apiClient } from './http';

export const artistBrowseService = {
  list(params) {
    return apiClient.get('/api/artists', { params, authenticated: false });
  },

  getBySlug(slug) {
    return apiClient.get(`/api/artists/${encodeURIComponent(slug)}`, { authenticated: false });
  },

  getSongs(slug, params) {
    return apiClient.get(`/api/artists/${encodeURIComponent(slug)}/songs`, {
      params,
      authenticated: false
    });
  }
};
