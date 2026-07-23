import { request } from './http';

export const userService = {
  getCurrent() {
    return request('/api/auth/me');
  },

  getArtistProfile() {
    return request('/api/artist/profile');
  },

  updateArtistProfile(profile) {
    return request('/api/artist/profile', {
      method: 'PUT',
      body: profile
    });
  }
};
