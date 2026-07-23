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
  }
};
