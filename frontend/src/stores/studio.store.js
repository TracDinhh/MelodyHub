import { ref } from 'vue';
import { defineStore } from 'pinia';
import { studioService } from '../services/studioService';

/**
 * Caches the current user's managed artists for display/selector performance.
 *
 * NOTE: this store only caches — it does NOT own which artist is active. The
 * active artist context is the URL (`route.params.artistId`), so page refresh
 * and back/forward navigation always preserve the correct artist.
 */
export const useStudioStore = defineStore('studio', () => {
  const myArtists = ref([]);
  const isLoading = ref(false);
  const error = ref('');
  const loaded = ref(false);

  /**
   * Loads (or returns the cached) list of artists the user can manage.
   * Pass `force = true` to bypass the cache.
   */
  async function loadMyArtists(force = false) {
    if (loaded.value && !force) return myArtists.value;

    isLoading.value = true;
    error.value = '';
    try {
      myArtists.value = await studioService.getMyArtists();
      loaded.value = true;
      return myArtists.value;
    } catch (requestError) {
      error.value = requestError.message || 'Unable to load your artists.';
      throw requestError;
    } finally {
      isLoading.value = false;
    }
  }

  function findArtist(artistId) {
    const id = Number(artistId);
    return myArtists.value.find((artist) => Number(artist.artistId) === id) || null;
  }

  function clear() {
    myArtists.value = [];
    loaded.value = false;
    error.value = '';
  }

  return {
    myArtists,
    isLoading,
    error,
    loaded,
    loadMyArtists,
    findArtist,
    clear
  };
});