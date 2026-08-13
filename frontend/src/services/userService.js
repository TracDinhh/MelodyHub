import { apiClient } from './http';

export const userService = {
  getCurrent() {
    return apiClient.get('/api/auth/me');
  },

  getArtistProfile() {
    return apiClient.get('/api/artist/profile');
  },

  updateArtistProfile(profile) {
    return apiClient.put('/api/artist/profile', profile);
  },

  updateMyProfile(payload) {
    return apiClient.patch('/api/users/me', payload);
  },

  getLikedSongs(params) {
    return apiClient.get('/api/users/me/liked-songs', { params });
  },

  likeSong(songId) {
    return apiClient.post(`/api/users/me/liked-songs/${encodeURIComponent(songId)}`);
  },

  unlikeSong(songId) {
    return apiClient.delete(`/api/users/me/liked-songs/${encodeURIComponent(songId)}`);
  }
};
