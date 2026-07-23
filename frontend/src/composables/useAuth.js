import { storeToRefs } from 'pinia';
import { useAuthStore } from '../stores/auth.store';

export function useAuth() {
  const store = useAuthStore();
  const {
    user,
    isLoading,
    isAuthenticated,
    isArtist,
    displayName
  } = storeToRefs(store);

  return {
    user,
    isLoading,
    isAuthenticated,
    isArtist,
    displayName,
    login: store.login,
    logout: store.logout
  };
}
