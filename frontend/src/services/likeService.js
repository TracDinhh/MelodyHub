import { apiClient } from './http';

export const likeService = {
  // Ids of every song the current user has liked — used to hydrate local state.
  listIds() {
    return apiClient.get('/api/likes/ids');
  },

  // Paged list of liked songs (song payload + likedAt + artists).
  list(params) {
    return apiClient.get('/api/likes', { params });
  },

  like(songId) {
    return apiClient.post('/api/likes', { songId });
  },

  unlike(songId) {
    return apiClient.delete(`/api/likes/${encodeURIComponent(songId)}`);
  }
};
