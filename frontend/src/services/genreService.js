import { apiClient } from './http';

export const genreService = {
  /**
   * Full genre catalog (system master data). Public.
   */
  listGenres() {
    return apiClient.get('/api/genres', { authenticated: false });
  },

  /**
   * Songs in a genre (PUBLISHED only). Returns a PagedResponse.
   */
  getGenreSongs(slug, { page = 1, size = 20 } = {}) {
    return apiClient.get(`/api/genres/${encodeURIComponent(slug)}/songs`, {
      params: { page, size },
      authenticated: false
    });
  }
};