import { request } from './http';

export const songService = {
  listPublic(params) {
    return request('/api/songs', {
      query: params,
      authenticated: false
    });
  },

  getPublic(slug) {
    return request(`/api/songs/${encodeURIComponent(slug)}`, {
      authenticated: false
    });
  },

  listMine(params) {
    return request('/api/artist/songs', { query: params });
  },

  getMine(identifier) {
    return request(`/api/artist/songs/${encodeURIComponent(identifier)}`);
  }
};
