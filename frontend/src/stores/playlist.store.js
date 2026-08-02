import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { playlistService } from '../services/playlistService';

const PAGE_SIZE = 20;

export const usePlaylistStore = defineStore('playlists', () => {
  const playlists = ref([]);
  const total = ref(0);
  const page = ref(1);
  const size = ref(PAGE_SIZE);
  const isLoading = ref(false);
  const error = ref('');

  const current = ref(null);
  const isDetailLoading = ref(false);
  const detailError = ref('');

  const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)));

  async function loadPage(nextPage = 1) {
    isLoading.value = true;
    error.value = '';
    try {
      const response = await playlistService.list({ page: nextPage, size: size.value });
      playlists.value = response.items || [];
      total.value = response.total || 0;
      page.value = response.page || nextPage;
      size.value = response.size || PAGE_SIZE;
    } catch (requestError) {
      error.value = requestError.message;
      playlists.value = [];
      total.value = 0;
    } finally {
      isLoading.value = false;
    }
  }

  async function loadDetail(playlistId) {
    isDetailLoading.value = true;
    detailError.value = '';
    try {
      current.value = await playlistService.get(playlistId);
    } catch (requestError) {
      detailError.value = requestError.message;
      current.value = null;
    } finally {
      isDetailLoading.value = false;
    }
  }

  async function create(payload) {
    const created = await playlistService.create(payload);
    playlists.value = [created, ...playlists.value];
    total.value += 1;
    return created;
  }

  async function update(playlistId, payload) {
    const updated = await playlistService.update(playlistId, payload);
    playlists.value = playlists.value.map((item) => (item.id === updated.id ? updated : item));
    if (current.value && current.value.id === updated.id) {
      current.value = { ...current.value, ...updated };
    }
    return updated;
  }

  async function remove(playlistId) {
    await playlistService.remove(playlistId);
    playlists.value = playlists.value.filter((item) => item.id !== playlistId);
    total.value = Math.max(0, total.value - 1);
    if (current.value && current.value.id === playlistId) {
      current.value = null;
    }
  }

  async function addSong(playlistId, songId) {
    const result = await playlistService.addSong(playlistId, songId);
    if (result?.added) {
      bumpSongCount(playlistId, 1);
    }
    return result?.added ?? false;
  }

  async function removeSong(playlistId, songId) {
    const result = await playlistService.removeSong(playlistId, songId);
    if (result?.removed && current.value && current.value.id === playlistId) {
      current.value = {
        ...current.value,
        songs: (current.value.songs || []).filter((song) => song.id !== songId),
        songCount: Math.max(0, (current.value.songCount || 0) - 1)
      };
    }
    if (result?.removed) {
      bumpSongCount(playlistId, -1);
    }
    return result?.removed ?? false;
  }

  function bumpSongCount(playlistId, delta) {
    playlists.value = playlists.value.map((item) =>
      item.id === playlistId
        ? { ...item, songCount: Math.max(0, (item.songCount || 0) + delta) }
        : item
    );
  }

  return {
    playlists,
    total,
    page,
    size,
    isLoading,
    error,
    current,
    isDetailLoading,
    detailError,
    totalPages,
    loadPage,
    loadDetail,
    create,
    update,
    remove,
    addSong,
    removeSong
  };
});
