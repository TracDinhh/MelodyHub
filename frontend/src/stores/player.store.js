import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { tracks } from '../data/music';

export const usePlayerStore = defineStore('player', () => {
  const currentTrack = ref(tracks[0]);
  const isPlaying = ref(false);
  const currentTime = ref(72);
  const volume = ref(72);
  const previousVolume = ref(72);
  const shuffle = ref(false);
  const repeat = ref(false);
  const queueOpen = ref(false);
  const fullscreenLyrics = ref(false);
  const likedIds = ref(new Set([1, 3]));
  const downloadedIds = ref(new Set());

  const duration = computed(() => currentTrack.value.duration);
  const progress = computed(() => (currentTime.value / duration.value) * 100);
  const muted = computed(() => volume.value === 0);
  const currentIndex = computed(() =>
    tracks.findIndex((track) => track.id === currentTrack.value.id)
  );

  function playTrack(track) {
    if (currentTrack.value.id === track.id) {
      isPlaying.value = !isPlaying.value;
      return;
    }
    currentTrack.value = track;
    currentTime.value = 0;
    isPlaying.value = true;
  }

  function togglePlayback() {
    isPlaying.value = !isPlaying.value;
  }

  function next() {
    const index = shuffle.value
      ? Math.floor(Math.random() * tracks.length)
      : (currentIndex.value + 1) % tracks.length;
    currentTrack.value = tracks[index];
    currentTime.value = 0;
  }

  function previous() {
    if (currentTime.value > 5) {
      currentTime.value = 0;
      return;
    }
    const index = (currentIndex.value - 1 + tracks.length) % tracks.length;
    currentTrack.value = tracks[index];
    currentTime.value = 0;
  }

  function seek(value) {
    currentTime.value = Number(value);
  }

  function tick() {
    if (!isPlaying.value) return;
    if (currentTime.value >= duration.value) {
      if (repeat.value) currentTime.value = 0;
      else next();
      return;
    }
    currentTime.value += 1;
  }

  function setVolume(value) {
    volume.value = Number(value);
    if (volume.value > 0) previousVolume.value = volume.value;
  }

  function toggleMute() {
    if (volume.value === 0) volume.value = previousVolume.value || 70;
    else {
      previousVolume.value = volume.value;
      volume.value = 0;
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
