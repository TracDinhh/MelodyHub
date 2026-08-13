import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { useListenTrackerStore } from './listenTracker.store';
import { userService } from '../services/userService';

const EMPTY_TRACK = { id: null, title: '', artist: '', cover: '', duration: 0, audioUrl: null, lyricsType: 'PLAIN', lyrics: [] };

export const usePlayerStore = defineStore('player', () => {
  // Single real audio element that actually plays sound.
  const audio = typeof Audio !== 'undefined' ? new Audio() : null;
  
  const currentTrack = ref({ ...EMPTY_TRACK });
  const queue = ref([]);
  const isPlaying = ref(false);
  const currentTime = ref(0);
  const durationSec = ref(0);
  const volume = ref(72);
  const previousVolume = ref(72);
  const shuffle = ref(false);
  const repeat = ref(false);
  const queueOpen = ref(false);
  const fullscreenLyrics = ref(false);
  const likedIds = ref(new Set());
  const downloadedIds = ref(new Set());
  
  // Synced lyrics data
  const syncedLyrics = ref([]); // Array of { startTime, endTime, text }
  let lyricsRequestId = 0;
  
  const duration = computed(() => durationSec.value || currentTrack.value?.duration || 0);
  const progress = computed(() => (duration.value ? (currentTime.value / duration.value) * 100 : 0));
  const muted = computed(() => volume.value === 0);
  const hasTrack = computed(() => Boolean(currentTrack.value?.id));
  const currentIndex = computed(() =>
    queue.value.findIndex((track) => track.id === currentTrack.value.id)
  );
  
  // Current active lyric line based on currentTime
  const currentLyricLine = computed(() => {
    if (!syncedLyrics.value.length) return null;
    const time = currentTime.value;
    for (let i = syncedLyrics.value.length - 1; i >= 0; i--) {
      if (time >= syncedLyrics.value[i].startTime) {
        return i;
      }
    }
    return null;
  });
  
  const hasSyncedLyrics = computed(() => 
    currentTrack.value?.lyricsType === 'SYNCED' && syncedLyrics.value.length > 0
  );

  // Resolve the listener lazily so loading the player does not eagerly load
  // auth, HTTP, and history services.
  let trackerInstance = null;

  function getTracker() {
    if (!trackerInstance) {
      try {
        trackerInstance = useListenTrackerStore();
      } catch {
        trackerInstance = null;
      }
    }
    return trackerInstance;
  }

  if (audio) {
    audio.volume = volume.value / 100;
    audio.addEventListener('timeupdate', () => {
      // Keep fractional time so synced lyrics highlight at sub-second precision.
      currentTime.value = audio.currentTime;
      const tracker = getTracker();
      if (tracker && currentTrack.value?.id) {
        tracker.noteProgress(currentTrack.value.id, Math.floor(audio.currentTime));
      }
    });
    audio.addEventListener('loadedmetadata', () => {
      if (Number.isFinite(audio.duration)) durationSec.value = Math.round(audio.duration);
    });
    audio.addEventListener('play', () => {
      isPlaying.value = true;
      const tracker = getTracker();
      if (tracker && currentTrack.value?.id) {
        tracker.recordStartedTrack(currentTrack.value.id);
      }
    });
    audio.addEventListener('pause', () => {
      isPlaying.value = false;
    });
    audio.addEventListener('ended', () => {
      const tracker = getTracker();
      if (tracker && currentTrack.value?.id) {
        tracker.tryRecord(currentTrack.value.id);
      }
      if (repeat.value) {
        audio.currentTime = 0;
        audio.play().catch(() => {});
      } else {
        next();
      }
    });
  }

  function load(track) {
    currentTrack.value = { ...EMPTY_TRACK, ...track };
    currentTime.value = 0;
    durationSec.value = track?.duration || 0;
    syncedLyrics.value = [];
    const requestId = ++lyricsRequestId;

    if (track?.lyricsType === 'SYNCED' && track?.slug) {
      void loadSyncedLyrics(track.slug, track.id, requestId);
    }

    if (!audio) return;
    if (track?.audioUrl) {
      audio.src = track.audioUrl;
      audio.play().catch(() => {});
    } else {
      // Mock/legacy track without a real audio URL.
      audio.pause();
      audio.removeAttribute('src');
      audio.load();
      isPlaying.value = false;
    }
  }
  
  async function loadSyncedLyrics(slug, trackId = currentTrack.value?.id, requestId = ++lyricsRequestId) {
    if (!slug || !trackId) return;
    try {
      const response = await fetch(`/api/songs/${encodeURIComponent(slug)}/lyrics`);
      if (!response.ok) throw new Error('Could not load synced lyrics');
      const data = await response.json();
      if (requestId !== lyricsRequestId || currentTrack.value?.id !== trackId) return;
      if (data.lyricsType === 'SYNCED' && data.lines) {
        syncedLyrics.value = data.lines;
      } else {
        syncedLyrics.value = [];
      }
    } catch {
      if (requestId !== lyricsRequestId || currentTrack.value?.id !== trackId) return;
      syncedLyrics.value = [];
    }
  }

  function playTrack(track, list = null) {
    if (Array.isArray(list) && list.length) {
      queue.value = list;
    } else if (!queue.value.some((item) => item.id === track.id)) {
      queue.value = [track];
    }

    if (currentTrack.value?.id === track.id) {
      togglePlayback();
      return;
    }
    const tracker = getTracker();
    if (tracker) tracker.recordStartedTrack(track.id);
    load(track);
  }

  function togglePlayback() {
    if (!audio || !hasTrack.value) return;
    if (audio.paused) {
      if (currentTrack.value.audioUrl) audio.play().catch(() => {});
    } else {
      audio.pause();
    }
  }

  function next() {
    if (!queue.value.length) return;
    const index = shuffle.value
      ? Math.floor(Math.random() * queue.value.length)
      : (currentIndex.value + 1) % queue.value.length;
    load(queue.value[index]);
  }

  function previous() {
    if (currentTime.value > 5) {
      seek(0);
      return;
    }
    if (!queue.value.length) return;
    const index = (currentIndex.value - 1 + queue.value.length) % queue.value.length;
    load(queue.value[index]);
  }

  function seek(value) {
    const seconds = Number(value);
    currentTime.value = seconds;
    if (audio && currentTrack.value?.audioUrl) audio.currentTime = seconds;
  }

  // Kept for compatibility with the interval in BottomPlayer; real time comes
  // from the audio element's timeupdate event, so this is a no-op.
  function tick() {}

  function setVolume(value) {
    volume.value = Number(value);
    if (volume.value > 0) previousVolume.value = volume.value;
    if (audio) audio.volume = volume.value / 100;
  }

  function toggleMute() {
    if (volume.value === 0) setVolume(previousVolume.value || 70);
    else {
      previousVolume.value = volume.value;
      setVolume(0);
    }
  }

  function setLiked(id, liked) {
    const nextSet = new Set(likedIds.value);
    if (liked) nextSet.add(id);
    else nextSet.delete(id);
    likedIds.value = nextSet;
  }

  function clearLikedSongs() {
    likedIds.value = new Set();
  }

  window.addEventListener('melodyhub:session-cleared', clearLikedSongs);

  async function toggleLike(id) {
    if (!id) return false;
    const liked = likedIds.value.has(id);
    const response = liked ? await userService.unlikeSong(id) : await userService.likeSong(id);
    setLiked(id, response?.liked ?? !liked);
    return response?.liked ?? !liked;
  }

  return {
    currentTrack,
    queue,
    isPlaying,
    currentTime,
    volume,
    shuffle,
    repeat,
    queueOpen,
    fullscreenLyrics,
    likedIds,
    downloadedIds,
    syncedLyrics,
    duration,
    progress,
    muted,
    hasTrack,
    hasSyncedLyrics,
    currentLyricLine,
    playTrack,
    loadSyncedLyrics,
    togglePlayback,
    next,
    previous,
    seek,
    tick,
    setVolume,
    toggleMute,
    setLiked,
    clearLikedSongs,
    toggleLike,
    toggleDownload: (id) => {
      const nextSet = new Set(downloadedIds.value);
      if (nextSet.has(id)) nextSet.delete(id);
      else nextSet.add(id);
      downloadedIds.value = nextSet;
    }
  };
});
