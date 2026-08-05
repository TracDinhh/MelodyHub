import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { authService } from '../services/authService';
import { refreshTokenStorage, tokenStorage } from '../services/http';

const USER_KEY = 'melodyhub.user';

function readStoredUser() {
  try {
    return JSON.parse(sessionStorage.getItem(USER_KEY));
  } catch {
    sessionStorage.removeItem(USER_KEY);
    return null;
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(tokenStorage.get());
  const user = ref(readStoredUser());
  const isLoading = ref(false);
  const initialized = ref(false);

  const isAuthenticated = computed(() => Boolean(token.value));
  const isUser = computed(() => user.value?.role === 'USER');
  const isArtist = computed(() => user.value?.role === 'ARTIST');
  const isAdmin = computed(() => user.value?.role === 'ADMIN');
  const displayName = computed(
    () => user.value?.displayName || user.value?.username || 'MelodyHub listener'
  );

  function saveSession(authResponse) {
    token.value = authResponse.token;
    user.value = authResponse.user;
    tokenStorage.set(authResponse.token);
    refreshTokenStorage.set(authResponse.refreshToken);
    sessionStorage.setItem(USER_KEY, JSON.stringify(authResponse.user));
  }

  function clearSession() {
    token.value = null;
    user.value = null;
    tokenStorage.clear();
    refreshTokenStorage.clear();
    sessionStorage.removeItem(USER_KEY);
  }

  window.addEventListener('melodyhub:unauthorized', clearSession);
  window.addEventListener('melodyhub:token-refreshed', (event) => {
    token.value = event.detail.token;
    user.value = event.detail.user;
  });
  window.addEventListener('melodyhub:profile-updated', (event) => {
    if (event.detail) {
      user.value = event.detail;
      sessionStorage.setItem(USER_KEY, JSON.stringify(event.detail));
    }
  });

  async function login(credentials) {
    isLoading.value = true;
    try {
      const authResponse = await authService.login(credentials);
      saveSession(authResponse);
      return authResponse.user;
    } finally {
      isLoading.value = false;
    }
  }

  async function register(account) {
    isLoading.value = true;
    try {
      const authResponse = await authService.register(account);
      saveSession(authResponse);
      return authResponse.user;
    } finally {
      isLoading.value = false;
    }
  }

  async function logout() {
    try {
      if (token.value) {
        await authService.logout();
      }
    } finally {
      clearSession();
    }
  }

  async function initialize() {
    if (initialized.value) return;

    if (token.value) {
      try {
        user.value = await authService.getCurrentUser();
        sessionStorage.setItem(USER_KEY, JSON.stringify(user.value));
      } catch {
        clearSession();
      }
    }
    initialized.value = true;
  }

  return {
    token,
    user,
    isLoading,
    initialized,
    isAuthenticated,
    isUser,
    isArtist,
    isAdmin,
    displayName,
    login,
    register,
    logout,
    initialize,
    clearSession,
    saveSession
  };
});
