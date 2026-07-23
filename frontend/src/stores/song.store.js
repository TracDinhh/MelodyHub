import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { songService } from '../services/songService';
import { useAuthStore } from './auth.store';

const PAGE_SIZE = 10;

export const useSongStore = defineStore('songs', () => {
  const authStore = useAuthStore();
  const songs = ref([]);
  const total = ref(0);
  const page = ref(1);
  const size = ref(PAGE_SIZE);
  const query = ref('');
  const isLoading = ref(false);
  const error = ref('');

  const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)));

  async function loadPage(nextPage = 1, nextQuery = query.value) {
    isLoading.value = true;
    error.value = '';
    query.value = nextQuery.trim();

    try {
      const params = {
        page: nextPage,
        size: size.value
      };
      if (!authStore.isArtist && query.value) {
        params.q = query.value;
      }

      const response = authStore.isArtist
        ? await songService.listMine(params)
        : await songService.listPublic(params);
      songs.value = response.items || [];
      total.value = response.total || 0;
      page.value = response.page || nextPage;
      size.value = response.size || PAGE_SIZE;
    } catch (requestError) {
      error.value = requestError.message;
      songs.value = [];
      total.value = 0;
    } finally {
      isLoading.value = false;
    }
  }

  return {
    songs,
    total,
    page,
    size,
    query,
    isLoading,
    error,
    totalPages,
    loadPage
  };
});
