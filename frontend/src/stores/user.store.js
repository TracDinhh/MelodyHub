import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { userService } from '../services/userService';
import { useAuthStore } from './auth.store';

export const useUserStore = defineStore('user', () => {
  const authStore = useAuthStore();
  const artistProfile = ref(null);
  const isLoading = ref(false);
  const error = ref('');

  const user = computed(() => authStore.user);

  async function loadArtistProfile() {
    if (!authStore.isArtist) return null;

    isLoading.value = true;
    error.value = '';
    try {
      artistProfile.value = await userService.getArtistProfile();
      return artistProfile.value;
    } catch (requestError) {
      error.value = requestError.message;
      throw requestError;
    } finally {
      isLoading.value = false;
    }
  }

  return {
    user,
    artistProfile,
    isLoading,
    error,
    loadArtistProfile
  };
});
