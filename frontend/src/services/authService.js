import { request } from './http';

export const authService = {
  login(credentials) {
    return request('/api/auth/login', {
      method: 'POST',
      body: credentials,
      authenticated: false
    });
  },

  getCurrentUser() {
    return request('/api/auth/me');
  },

  logout() {
    return request('/api/auth/logout', { method: 'POST' });
  }
};
