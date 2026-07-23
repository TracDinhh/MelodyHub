import axios from 'axios';

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');
const TOKEN_KEY = 'access_token';
const USER_KEY = 'user';

// Authentication is tab-scoped. Remove credentials left by older builds.
localStorage.removeItem(TOKEN_KEY);
localStorage.removeItem(USER_KEY);

export class HttpError extends Error {
  constructor(message, status, code, payload) {
    super(message);
    this.name = 'HttpError';
    this.status = status;
    this.code = code;
    this.payload = payload;
  }
}

export const tokenStorage = {
  key: TOKEN_KEY,
  get: () => sessionStorage.getItem(TOKEN_KEY),
  set: (token) => sessionStorage.setItem(TOKEN_KEY, token),
  clear: () => sessionStorage.removeItem(TOKEN_KEY)
};

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15_000,
  headers: {
    Accept: 'application/json'
  },
  paramsSerializer: {
    serialize(params) {
      const search = new URLSearchParams();
      Object.entries(params || {}).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          search.set(key, String(value));
        }
      });
      return search.toString();
    }
  }
});

apiClient.interceptors.request.use((config) => {
  const token = tokenStorage.get();
  if (config.authenticated !== false && token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => (response.status === 204 ? null : response.data),
  (error) => {
    if (axios.isCancel(error)) {
      return Promise.reject(error);
    }

    const status = error.response?.status || 0;
    const payload = error.response?.data;
    const code = payload?.code || (status === 0 ? 'NETWORK_ERROR' : 'REQUEST_FAILED');
    const message =
      payload?.message ||
      (status === 0
        ? 'Unable to connect to the MelodyHub API'
        : `Request failed with status ${status}`);

    if (status === 401 && error.config?.authenticated !== false) {
      tokenStorage.clear();
      sessionStorage.removeItem(USER_KEY);
      window.dispatchEvent(new CustomEvent('melodyhub:unauthorized'));
    }

    return Promise.reject(new HttpError(message, status, code, payload));
  }
);
