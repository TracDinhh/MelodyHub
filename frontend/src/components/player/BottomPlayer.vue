<script setup>
import { computed, onBeforeUnmount, onMounted } from 'vue';
import {
  Download,
  Heart,
  ListMusic,
  Maximize2,
  Pause,
  Play,
  Repeat2,
  Shuffle,
  SkipBack,
  SkipForward,
  Volume2,
  VolumeX,
  X
} from '@lucide/vue';
import { tracks } from '../../data/music';
import { usePlayerStore } from '../../stores/player.store';
import { formatDuration } from '../../utils/formatDate';

const player = usePlayerStore();
let timer;

const isLiked = computed(() => player.likedIds.has(player.currentTrack.id));
const isDownloaded = computed(() => player.downloadedIds.has(player.currentTrack.id));

onMounted(() => {
  timer = window.setInterval(player.tick, 1000);
});
onBeforeUnmount(() => window.clearInterval(timer));
</script>

<template>
  <footer class="fixed inset-x-0 bottom-0 z-[60] h-24 border-t border-white/10 bg-[#0a0a0a]/95 px-3 backdrop-blur-2xl sm:px-5">
    <div class="grid h-full grid-cols-[minmax(0,1fr)_auto] items-center gap-3 lg:grid-cols-[minmax(210px,1fr)_minmax(360px,1.4fr)_minmax(220px,1fr)]">
      <div class="flex min-w-0 items-center gap-3">
        <img :src="player.currentTrack.cover" :alt="`${player.currentTrack.title} cover`" class="size-13 rounded-md object-cover sm:size-14" />
        <div class="min-w-0 flex-1">
          <p class="truncate text-xs font-bold text-white sm:text-sm">{{ player.currentTrack.title }}</p>
          <p class="truncate text-[10px] text-[#818181] sm:text-xs">{{ player.currentTrack.artist }}</p>
        </div>
        <button class="sonix-icon-btn hidden sm:grid" title="Like track" @click="player.toggleLike(player.currentTrack.id)">
          <Heart :size="17" :class="isLiked ? 'fill-[#1DB954] text-[#1DB954]' : ''" />
        </button>
        <button class="sonix-icon-btn hidden md:grid" title="Download for offline" @click="player.toggleDownload(player.currentTrack.id)">
          <Download :size="17" :class="isDownloaded ? 'text-[#1DB954]' : ''" />
        </button>
      </div>

      <div class="flex flex-col items-center">
        <div class="flex items-center gap-2 sm:gap-4">
          <button class="sonix-player-btn hidden sm:grid" title="Shuffle" :class="{ '!text-[#1DB954]': player.shuffle }" @click="player.shuffle = !player.shuffle">
            <Shuffle :size="17" />
          </button>
          <button class="sonix-player-btn hidden sm:grid" title="Previous track" @click="player.previous">
            <SkipBack :size="19" class="fill-current" />
          </button>
          <button class="grid size-10 place-items-center rounded-full bg-white text-black transition hover:scale-105 active:scale-95 sm:size-11" :title="player.isPlaying ? 'Pause' : 'Play'" @click="player.togglePlayback">
            <Pause v-if="player.isPlaying" :size="20" class="fill-current" />
            <Play v-else :size="20" class="ml-0.5 fill-current" />
          </button>
          <button class="sonix-player-btn" title="Next track" @click="player.next">
            <SkipForward :size="19" class="fill-current" />
          </button>
          <button class="sonix-player-btn hidden sm:grid" title="Repeat" :class="{ '!text-[#1DB954]': player.repeat }" @click="player.repeat = !player.repeat">
            <Repeat2 :size="17" />
          </button>
        </div>
        <div class="mt-2 hidden w-full items-center gap-2 text-[10px] text-[#777] sm:flex">
          <span class="w-8 text-right">{{ formatDuration(player.currentTime) }}</span>
          <input
            type="range"
            min="0"
            :max="player.duration"
            :value="player.currentTime"
            class="sonix-range flex-1"
            aria-label="Track progress"
            :style="{ '--range-progress': `${player.progress}%` }"
            @input="player.seek($event.target.value)"
          />
          <span class="w-8">{{ formatDuration(player.duration) }}</span>
        </div>
      </div>

      <div class="hidden items-center justify-end gap-2 lg:flex">
        <button class="sonix-icon-btn" :title="player.muted ? 'Unmute' : 'Mute'" @click="player.toggleMute">
          <VolumeX v-if="player.muted" :size="18" />
          <Volume2 v-else :size="18" />
        </button>
        <input
          type="range"
          min="0"
          max="100"
          :value="player.volume"
          class="sonix-range w-24"
          aria-label="Volume"
          :style="{ '--range-progress': `${player.volume}%` }"
          @input="player.setVolume($event.target.value)"
        />
        <button class="sonix-icon-btn" title="Open queue" @click="player.queueOpen = true">
          <ListMusic :size="18" />
        </button>
        <button class="sonix-icon-btn" title="Fullscreen lyrics" @click="player.fullscreenLyrics = true">
          <Maximize2 :size="17" />
        </button>
      </div>
    </div>
  </footer>

  <Teleport to="body">
    <div v-if="player.queueOpen" class="fixed inset-0 z-[80] bg-black/60 backdrop-blur-sm" @click.self="player.queueOpen = false">
      <aside class="absolute inset-y-0 right-0 w-[min(420px,92vw)] border-l border-white/10 bg-[#111] p-5 shadow-2xl">
        <header class="flex items-center justify-between">
          <div><p class="text-xs font-black text-white">UP NEXT</p><p class="mt-1 text-xs text-[#777]">{{ tracks.length }} songs</p></div>
          <button class="sonix-icon-btn" title="Close queue" @click="player.queueOpen = false"><X :size="18" /></button>
        </header>
        <div class="mt-5 space-y-1">
          <button
            v-for="track in tracks"
            :key="track.id"
            class="flex w-full items-center gap-3 rounded-lg p-2 text-left hover:bg-white/5"
            :class="{ 'bg-white/5': track.id === player.currentTrack.id }"
            @click="player.playTrack(track)"
          >
            <img :src="track.cover" alt="" class="size-11 rounded-md object-cover" />
            <span class="min-w-0 flex-1">
              <span class="block truncate text-sm font-bold" :class="track.id === player.currentTrack.id ? 'text-[#1DB954]' : 'text-white'">{{ track.title }}</span>
              <span class="block truncate text-xs text-[#777]">{{ track.artist }}</span>
            </span>
            <span class="text-xs text-[#666]">{{ formatDuration(track.duration) }}</span>
          </button>
        </div>
      </aside>
    </div>

    <div v-if="player.fullscreenLyrics" class="fixed inset-0 z-[90] flex flex-col bg-[#090909]/98 p-6 backdrop-blur-2xl">
      <header class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <img :src="player.currentTrack.cover" alt="" class="size-12 rounded-md object-cover" />
          <div><p class="font-bold text-white">{{ player.currentTrack.title }}</p><p class="text-xs text-[#777]">{{ player.currentTrack.artist }}</p></div>
        </div>
        <button class="sonix-icon-btn" title="Close fullscreen lyrics" @click="player.fullscreenLyrics = false"><X :size="22" /></button>
      </header>
      <div class="m-auto max-w-3xl space-y-5 text-center">
        <p v-for="(line, index) in player.currentTrack.lyrics" :key="index" class="text-2xl font-black sm:text-4xl" :class="index === Math.min(player.currentTrack.lyrics.length - 1, Math.floor((player.currentTime / player.duration) * player.currentTrack.lyrics.length)) ? 'text-[#1DB954]' : 'text-white/25'">
          {{ line }}
        </p>
      </div>
    </div>
  </Teleport>
</template>
