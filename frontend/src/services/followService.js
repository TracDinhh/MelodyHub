import { apiClient } from './http';

export const followService = {
  /**
   * Ids of every artist the current user follows (frontend hydration).
   */
  listIds() {
    return apiClient.get('/api/follows/ids');
  },

  /**
   * Paged list of followed artists.
   */
  list(params) {
    return apiClient.get('/api/follows', { params });
  },

  /**
   * Follow an artist by id. Returns { following, followerCount }.
   */
  follow(artistId) {
    return apiClient.post('/api/follows', { artistId });
  },

  /**
   * Unfollow an artist by id. Returns { following, followerCount }.
   */
  unfollow(artistId) {
    return apiClient.delete(`/api/follows/${encodeURIComponent(artistId)}`);
  }
};