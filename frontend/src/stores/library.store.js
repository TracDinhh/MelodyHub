import { ref } from 'vue';
import { defineStore } from 'pinia';
import { userService } from '../services/userService';
import { usePlayerStore } from './player.store';

export const useLibraryStore = defineStore('library', () => {
  const likedSongs = ref([]);
  const likedTotal = ref(0);
  const isLoadingLikedSongs = ref(false);
  const likedSongsError = ref('');

  async function loadLikedSongs() {
    isLoadingLikedSongs.value = true;
    likedSongsError.value = '';
    try {
      const response = await userService.getLikedSongs({ page: 1, size: 50 });
      likedSongs.value = response?.items || [];
      likedTotal.value = response?.total || 0;
      const player = usePlayerStore();
      for (const song of likedSongs.value) {
        player.setLiked(song.id, true);
      }
    } catch (error) {
      likedSongs.value = [];
      likedTotal.value = 0;
      likedSongsError.value = error?.message || 'Could not load liked songs.';
    } finally {
      isLoadingLikedSongs.value = false;
    }
  }

  function removeLikedSong(songId) {
    likedSongs.value = likedSongs.value.filter((song) => song.id !== songId);
    likedTotal.value = Math.max(0, likedTotal.value - 1);
  }

  function clearLikedSongs() {
    likedSongs.value = [];
    likedTotal.value = 0;
    likedSongsError.value = '';
  }

  window.addEventListener('melodyhub:session-cleared', clearLikedSongs);

  return {
    likedSongs,
    likedTotal,
    isLoadingLikedSongs,
    likedSongsError,
    loadLikedSongs,
    removeLikedSong,
    clearLikedSongs
  };
});
