import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { authService } from '../services/authService';
import { tokenStorage } from '../services/http';

const USER_KEY = 'melodyhub.user';

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY));
  } catch {
    localStorage.removeItem(USER_KEY);
    return null;
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(tokenStorage.get());
  const user = ref(readStoredUser());
  const isLoading = ref(false);
  const initialized = ref(false);

  const isAuthenticated = computed(() => Boolean(token.value));
  const isArtist = computed(() => user.value?.role === 'ARTIST');
  const displayName = computed(
    () => user.value?.displayName || user.value?.username || 'MelodyHub listener'
  );

  function saveSession(authResponse) {
    token.value = authResponse.token;
    user.value = authResponse.user;
    tokenStorage.set(authResponse.token);
    localStorage.setItem(USER_KEY, JSON.stringify(authResponse.user));
  }

  function clearSession() {
    token.value = null;
    user.value = null;
    tokenStorage.clear();
    localStorage.removeItem(USER_KEY);
  }

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
        localStorage.setItem(USER_KEY, JSON.stringify(user.value));
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
    isArtist,
    displayName,
    login,
    logout,
    initialize,
    clearSession
  };
});
