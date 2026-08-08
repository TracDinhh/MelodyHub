import { apiClient, refreshTokenStorage } from './http';

export const authService = {
  register(account) {
    return apiClient.post('/api/auth/register', account, {
      authenticated: false
    });
  },

  login(credentials) {
    return apiClient.post('/api/auth/login', credentials, {
      authenticated: false
    });
  },

  getCurrentUser() {
    return apiClient.get('/api/auth/me');
  },

  logout() {
    return apiClient.post('/api/auth/logout', {
      refreshToken: refreshTokenStorage.get() || refreshTokenStorage.getPersistent()
    });
  },

  requestPasswordReset(email) {
    return apiClient.post('/api/auth/forgot-password', { email }, {
      authenticated: false
    });
  },

  resetPassword(token, newPassword) {
    return apiClient.post('/api/auth/reset-password', { token, newPassword }, {
      authenticated: false
    });
  }
};
