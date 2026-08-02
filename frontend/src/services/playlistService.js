import { apiClient } from './http';

export const playlistService = {
  list(params) {
    return apiClient.get('/api/playlists', { params });
  },

  get(playlistId) {
    return apiClient.get(`/api/playlists/${playlistId}`);
  },

  create(payload) {
    return apiClient.post('/api/playlists', payload);
  },

  update(playlistId, payload) {
    return apiClient.put(`/api/playlists/${playlistId}`, payload);
  },

  remove(playlistId) {
    return apiClient.delete(`/api/playlists/${playlistId}`);
  },

  addSong(playlistId, songId) {
    return apiClient.post(`/api/playlists/${playlistId}/songs`, { songId });
  },

  removeSong(playlistId, songId) {
    return apiClient.delete(`/api/playlists/${playlistId}/songs/${songId}`);
  }
};
