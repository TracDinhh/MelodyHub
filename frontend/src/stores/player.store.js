import { computed, ref } from 'vue';
import { defineStore } from 'pinia';

const EMPTY_TRACK = { id: null, title: '', artist: '', cover: '', duration: 0, audioUrl: null };

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

  const duration = computed(() => durationSec.value || currentTrack.value?.duration || 0);
  const progress = computed(() => (duration.value ? (currentTime.value / duration.value) * 100 : 0));
  const muted = computed(() => volume.value === 0);
  const hasTrack = computed(() => Boolean(currentTrack.value?.id));
  const currentIndex = computed(() =>
    queue.value.findIndex((track) => track.id === currentTrack.value.id)
  );

  if (audio) {
    audio.volume = volume.value / 100;
    audio.addEventListener('timeupdate', () => {
      currentTime.value = Math.floor(audio.currentTime);
    });
    audio.addEventListener('loadedmetadata', () => {
      if (Number.isFinite(audio.duration)) durationSec.value = Math.round(audio.duration);
    });
    audio.addEventListener('play', () => {
      isPlaying.value = true;
    });
    audio.addEventListener('pause', () => {
      isPlaying.value = false;
    });
    audio.addEventListener('ended', () => {
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

  function toggleInSet(target, id) {
    const nextSet = new Set(target.value);
    if (nextSet.has(id)) nextSet.delete(id);
    else nextSet.add(id);
    target.value = nextSet;
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
    duration,
    progress,
    muted,
    hasTrack,
    playTrack,
    togglePlayback,
    next,
    previous,
    seek,
    tick,
    setVolume,
    toggleMute,
    toggleLike: (id) => toggleInSet(likedIds, id),
    toggleDownload: (id) => toggleInSet(downloadedIds, id)
  };
});
