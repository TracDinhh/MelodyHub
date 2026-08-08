import axios from 'axios';

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');
const TOKEN_KEY = 'access_token';
const REFRESH_TOKEN_KEY = 'refresh_token';
const USER_KEY = 'melodyhub.user';

// Authentication is tab-scoped. Remove credentials left by older builds.
localStorage.removeItem(TOKEN_KEY);
localStorage.removeItem(REFRESH_TOKEN_KEY);
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

export const REMEMBER_KEY = 'melodyhub.remember';
export const tokenStorage = {
  key: TOKEN_KEY,
  get: () => sessionStorage.getItem(TOKEN_KEY),
  set: (token, remember = false) => {
    const storage = remember ? localStorage : sessionStorage;
    storage.setItem(TOKEN_KEY, token);
  },
  clear: () => {
    sessionStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(TOKEN_KEY);
  },
  getPersistent: () => localStorage.getItem(TOKEN_KEY)
};

export const refreshTokenStorage = {
  key: REFRESH_TOKEN_KEY,
  get: () => sessionStorage.getItem(REFRESH_TOKEN_KEY),
  set: (token, remember = false) => {
    const storage = remember ? localStorage : sessionStorage;
    storage.setItem(REFRESH_TOKEN_KEY, token);
  },
  clear: () => {
    sessionStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  },
  getPersistent: () => localStorage.getItem(REFRESH_TOKEN_KEY)
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

const refreshClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15_000,
  headers: {
    Accept: 'application/json',
    'Content-Type': 'application/json'
  }
});

let refreshPromise = null;

function clearSession() {
  tokenStorage.clear();
  refreshTokenStorage.clear();
  sessionStorage.removeItem(USER_KEY);
}

function toHttpError(error) {
  const status = error.response?.status || 0;
  const payload = error.response?.data;
  const code = payload?.code || (status === 0 ? 'NETWORK_ERROR' : 'REQUEST_FAILED');
  const message =
    payload?.message ||
    (status === 0
      ? 'Unable to connect to the MelodyHub API'
      : `Request failed with status ${status}`);

  return new HttpError(message, status, code, payload);
}

async function refreshSession() {
  if (!refreshPromise) {
    refreshPromise = refreshClient
      .post('/api/auth/refresh', {
        refreshToken: refreshTokenStorage.get() || refreshTokenStorage.getPersistent()
      })
      .then((response) => {
        const authResponse = response.data;
        const remember = localStorage.getItem(REMEMBER_KEY) === 'true';
        tokenStorage.set(authResponse.token, remember);
        refreshTokenStorage.set(authResponse.refreshToken, remember);
        sessionStorage.setItem(USER_KEY, JSON.stringify(authResponse.user));
        window.dispatchEvent(new CustomEvent('melodyhub:token-refreshed', { detail: authResponse }));
        return authResponse.token;
      })
      .catch((error) => {
        clearSession();
        window.dispatchEvent(new CustomEvent('melodyhub:unauthorized'));
        throw toHttpError(error);
      })
      .finally(() => {
        refreshPromise = null;
      });
  }

  return refreshPromise;
}

apiClient.interceptors.request.use((config) => {
  const token = tokenStorage.get();
  if (config.authenticated !== false && token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => (response.status === 204 ? null : response.data),
  async (error) => {
    if (axios.isCancel(error)) {
      return Promise.reject(error);
    }

    const status = error.response?.status || 0;
    const originalRequest = error.config || {};

    if (
      status === 401 &&
      originalRequest.authenticated !== false &&
      !originalRequest._retry &&
      refreshTokenStorage.get()
    ) {
      originalRequest._retry = true;
      const token = await refreshSession();
      originalRequest.headers = originalRequest.headers || {};
      originalRequest.headers.Authorization = `Bearer ${token}`;
      return apiClient(originalRequest);
    }

    if (status === 401 && originalRequest.authenticated !== false) {
      clearSession();
      window.dispatchEvent(new CustomEvent('melodyhub:unauthorized'));
    }

    return Promise.reject(toHttpError(error));
  }
);
