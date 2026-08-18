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
  
  getSyncedLyrics(slug) {
    // Sent with the bearer token when present: synced lyrics are premium-gated,
    // so the backend needs the token to authorize the request. Guests simply
    // get the 402 and fall back to no lyrics.
    return apiClient.get(`/api/songs/${encodeURIComponent(slug)}/lyrics`);
  },

  getRelated(slug, params) {
    return apiClient.get(`/api/songs/${encodeURIComponent(slug)}/related`, {
      params,
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
  },
  
  updateSyncedLyrics(id, payload) {
    return apiClient.put(`/api/artist/songs/${encodeURIComponent(id)}/lyrics`, payload);
  }
};
