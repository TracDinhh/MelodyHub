import { computed, ref, watch } from 'vue';
import { defineStore } from 'pinia';
import { useAuthStore } from '../stores/auth.store';
import { listenHistoryService } from '../services/listenHistoryService';

const MIN_LISTEN_SECONDS = 30;
const PENDING_TRACK_KEY = 'melodyhub.listen_history_pending';
const PERSIST_DEBOUNCE_MS = 1000;

function readPending() {
  try {
    return JSON.parse(sessionStorage.getItem(PENDING_TRACK_KEY)) || {};
  } catch {
    return {};
  }
}

function writePending(value) {
  sessionStorage.setItem(PENDING_TRACK_KEY, JSON.stringify(value));
}

/**
 * Tracks the user's listening progress and records an entry once a track has
 * been played for at least {@code MIN_LISTEN_SECONDS} seconds. Uses an
 * in-flight set so a song is only recorded once per session.
 */
export const useListenTrackerStore = defineStore('listen-tracker', () => {
  const auth = useAuthStore();
  const isAuthenticated = computed(() => auth.isAuthenticated);
  const recordedIds = ref(new Set());
  const pendingBySongId = ref(readPending());

  // Debounce sessionStorage writes so we don't stringify + persist on every
  // timeupdate (~4x/sec). The latest value is flushed at most once per second.
  let persistTimer = null;
  function schedulePersist() {
    if (persistTimer) return;
    persistTimer = setTimeout(() => {
      persistTimer = null;
      writePending(pendingBySongId.value);
    }, PERSIST_DEBOUNCE_MS);
  }

  function pendingFor(songId) {
    return pendingBySongId.value[songId] || 0;
  }

  function noteProgress(songId, currentTime) {
    if (!isAuthenticated.value || !songId || recordedIds.value.has(songId)) return;
    const sec = Math.floor(currentTime || 0);
    // Only touch reactive state / storage when the whole-second value changes.
    if (pendingBySongId.value[songId] === sec) return;
    pendingBySongId.value = { ...pendingBySongId.value, [songId]: sec };
    schedulePersist();
    // Record as soon as the listen threshold is crossed, rather than waiting
    // for the pause/ended events (which may never fire if the user navigates).
    if (sec >= MIN_LISTEN_SECONDS) {
      void tryRecord(songId);
    }
  }

  async function tryRecord(songId) {
    if (!isAuthenticated.value || !songId || recordedIds.value.has(songId)) return;
    const playedSec = pendingFor(songId);
    if (playedSec < MIN_LISTEN_SECONDS) return;

    recordedIds.value = new Set([...recordedIds.value, songId]);
    try {
      await listenHistoryService.record(songId, playedSec);
    } catch {
      recordedIds.value.delete(songId);
    }
  }

  function resetOnTrackChange(newSongId) {
    if (!newSongId) return;
    if (recordedIds.value.has(newSongId)) return;
    if (pendingBySongId.value[newSongId] == null) {
      pendingBySongId.value = { ...pendingBySongId.value, [newSongId]: 0 };
      writePending(pendingBySongId.value);
    }
  }

  function clear() {
    if (persistTimer) {
      clearTimeout(persistTimer);
      persistTimer = null;
    }
    recordedIds.value = new Set();
    pendingBySongId.value = {};
    sessionStorage.removeItem(PENDING_TRACK_KEY);
  }

  watch(
    () => auth.isAuthenticated,
    (now) => {
      if (!now) clear();
    }
  );

  return {
    recordedIds,
    pendingBySongId,
    noteProgress,
    tryRecord,
    resetOnTrackChange,
    clear,
    MIN_LISTEN_SECONDS
  };
});
