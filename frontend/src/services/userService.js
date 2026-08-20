import { apiClient } from './http';

export const userService = {
  getCurrent() {
    return apiClient.get('/api/auth/me');
  },

  updateMyProfile(payload) {
    return apiClient.patch('/api/users/me', payload);
  }
};
