import { apiClient } from './http';

export const songService = {
  listPublic(params) {
    return apiClient.get('/api/songs', {
      params,
      authenticated: false
    });
  },

  getPublic(slug) {
    return apiClient.get(`/api/songs/${encodeURIComponent(slug)}`, {
      authenticated: false
    });
  },

  listMine(params) {
    return apiClient.get('/api/artist/songs', { params });
  },

  getMine(identifier) {
    return apiClient.get(`/api/artist/songs/${encodeURIComponent(identifier)}`);
  },

  createMine(payload) {
    return apiClient.post('/api/artist/songs', payload);
  },

  updateMine(id, payload) {
    return apiClient.put(`/api/artist/songs/${encodeURIComponent(id)}`, payload);
  }
};
