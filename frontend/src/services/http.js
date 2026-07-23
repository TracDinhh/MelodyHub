const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');
const TOKEN_KEY = 'melodyhub.token';

export class HttpError extends Error {
  constructor(message, status, code, payload) {
    super(message);
    this.name = 'HttpError';
    this.status = status;
    this.code = code;
    this.payload = payload;
  }
}

function createUrl(path, query) {
  const search = new URLSearchParams();
  Object.entries(query || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, String(value));
    }
  });
  const queryString = search.toString();
  return `${API_BASE_URL}${path}${queryString ? `?${queryString}` : ''}`;
}

export async function request(path, options = {}) {
  const {
    method = 'GET',
    body,
    query,
    headers = {},
    signal,
    authenticated = true
  } = options;
  const requestHeaders = { Accept: 'application/json', ...headers };
  const token = localStorage.getItem(TOKEN_KEY);

  if (authenticated && token) {
    requestHeaders.Authorization = `Bearer ${token}`;
  }

  let requestBody = body;
  if (body !== undefined && !(body instanceof FormData)) {
    requestHeaders['Content-Type'] = 'application/json';
    requestBody = JSON.stringify(body);
  }

  const response = await fetch(createUrl(path, query), {
    method,
    headers: requestHeaders,
    body: requestBody,
    signal
  });

  const contentType = response.headers.get('content-type') || '';
  const payload = contentType.includes('application/json') ? await response.json() : null;

  if (!response.ok) {
    throw new HttpError(
      payload?.message || `Request failed with status ${response.status}`,
      response.status,
      payload?.code || 'REQUEST_FAILED',
      payload
    );
  }

  return response.status === 204 ? null : payload;
}

export const tokenStorage = {
  key: TOKEN_KEY,
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY)
};
