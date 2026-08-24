import { apiClient } from './http';

export const albumService = {
  list(artistId, params = {}) {
    const query = new URLSearchParams({ artistId });
    if (params.page) query.set('page', params.page);
    if (params.size) query.set('size', params.size);
    return apiClient.get(`/api/studio/albums?${query}`);
  },

  get(artistId, albumId) {
    return apiClient.get(`/api/studio/albums/${albumId}?artistId=${artistId}`);
  },

  create(artistId, data) {
    return apiClient.post(`/api/studio/albums?artistId=${artistId}`, data);
  },

  update(artistId, albumId, data) {
    return apiClient.put(`/api/studio/albums/${albumId}?artistId=${artistId}`, data);
  },

  delete(artistId, albumId) {
    return apiClient.delete(`/api/studio/albums/${albumId}?artistId=${artistId}`);
  },

  getSongs(artistId, albumId) {
    return apiClient.get(`/api/studio/albums/${albumId}/songs?artistId=${artistId}`);
  },
};
