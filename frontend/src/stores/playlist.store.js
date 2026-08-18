import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { playlistService } from '../services/playlistService';

const PAGE_SIZE = 20;
const SIDEBAR_PAGE_SIZE = 50;

export const usePlaylistStore = defineStore('playlists', () => {
  let pageRequestId = 0;
  let detailRequestId = 0;
  let sidebarRequestId = 0;
  const playlists = ref([]);
  const total = ref(0);
  const page = ref(1);
  const size = ref(PAGE_SIZE);
  const isLoading = ref(false);
  const error = ref('');

  const current = ref(null);
  const isDetailLoading = ref(false);
  const detailError = ref('');

  const sidebarPlaylists = ref([]);
  const isSidebarLoading = ref(false);
  const sidebarError = ref('');
  const ownerUserId = ref(null);

  const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)));

  async function loadPage(nextPage = 1) {
    const requestId = ++pageRequestId;
    isLoading.value = true;
    error.value = '';
    try {
      const response = await playlistService.list({ page: nextPage, size: size.value });
      if (requestId !== pageRequestId) return;
      playlists.value = response.items || [];
      total.value = response.total || 0;
      page.value = response.page || nextPage;
      size.value = response.size || PAGE_SIZE;
    } catch (requestError) {
      if (requestId !== pageRequestId) return;
      error.value = requestError.message;
      playlists.value = [];
      total.value = 0;
    } finally {
      if (requestId === pageRequestId) isLoading.value = false;
    }
  }

  async function loadDetail(playlistId) {
    const requestId = ++detailRequestId;
    isDetailLoading.value = true;
    detailError.value = '';
    try {
      const response = await playlistService.get(playlistId);
      if (requestId !== detailRequestId) return;
      current.value = response;
    } catch (requestError) {
      if (requestId !== detailRequestId) return;
      detailError.value = requestError.message;
      current.value = null;
    } finally {
      if (requestId === detailRequestId) isDetailLoading.value = false;
    }
  }

  async function loadSidebarPlaylists() {
    const requestId = ++sidebarRequestId;
    isSidebarLoading.value = true;
    sidebarError.value = '';
    try {
      const items = [];
      let nextPage = 1;
      let expectedTotal = Infinity;
      while (items.length < expectedTotal) {
        const response = await playlistService.list({ page: nextPage, size: SIDEBAR_PAGE_SIZE });
        if (requestId !== sidebarRequestId) return;
        const pageItems = response.items || [];
        items.push(...pageItems);
        expectedTotal = Number(response.total || 0);
        if (pageItems.length < SIDEBAR_PAGE_SIZE) break;
        nextPage += 1;
      }
      sidebarPlaylists.value = items;
    } catch (requestError) {
      if (requestId !== sidebarRequestId) return;
      sidebarError.value = requestError.message;
      sidebarPlaylists.value = [];
    } finally {
      if (requestId === sidebarRequestId) isSidebarLoading.value = false;
    }
  }

  async function create(payload) {
    const created = await playlistService.create(payload);
    playlists.value = [created, ...playlists.value];
    sidebarPlaylists.value = [
      created,
      ...sidebarPlaylists.value.filter((item) => item.id !== created.id)
    ];
    total.value += 1;
    return created;
  }

  async function update(playlistId, payload) {
    const updated = await playlistService.update(playlistId, payload);
    playlists.value = playlists.value.map((item) => (item.id === updated.id ? updated : item));
    sidebarPlaylists.value = sidebarPlaylists.value.map((item) =>
      item.id === updated.id ? updated : item
    );
    if (current.value && current.value.id === updated.id) {
      current.value = { ...current.value, ...updated };
    }
    return updated;
  }

  async function remove(playlistId) {
    await playlistService.remove(playlistId);
    playlists.value = playlists.value.filter((item) => item.id !== playlistId);
    sidebarPlaylists.value = sidebarPlaylists.value.filter((item) => item.id !== playlistId);
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
    const updateCount = (item) =>
      item.id === playlistId
        ? { ...item, songCount: Math.max(0, (item.songCount || 0) + delta) }
        : item;
    playlists.value = playlists.value.map(updateCount);
    sidebarPlaylists.value = sidebarPlaylists.value.map(updateCount);
  }

  function reset() {
    pageRequestId += 1;
    detailRequestId += 1;
    sidebarRequestId += 1;
    playlists.value = [];
    total.value = 0;
    page.value = 1;
    size.value = PAGE_SIZE;
    isLoading.value = false;
    error.value = '';
    current.value = null;
    isDetailLoading.value = false;
    detailError.value = '';
    sidebarPlaylists.value = [];
    isSidebarLoading.value = false;
    sidebarError.value = '';
    ownerUserId.value = null;
  }

  function switchOwner(userId) {
    const nextUserId = userId ?? null;
    if (ownerUserId.value === nextUserId) return false;
    reset();
    ownerUserId.value = nextUserId;
    return true;
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
    sidebarPlaylists,
    isSidebarLoading,
    sidebarError,
    ownerUserId,
    totalPages,
    loadPage,
    loadDetail,
    loadSidebarPlaylists,
    create,
    update,
    remove,
    addSong,
    removeSong,
    switchOwner,
    reset
  };
});
