<script setup>
import { computed, nextTick, ref, watch } from 'vue';
import {
  Heart,
  Mic2,
  Pause,
  Play,
  Sparkles,
  SkipBack,
  SkipForward,
  Volume2,
  VolumeX
} from '@lucide/vue';
import { tracks } from '../../data/music';
import { usePlayerStore } from '../../stores/player.store';
import { useAuthStore } from '../../stores/auth.store';
import { useLyricSelection } from '../../composables/useLyricSelection';
import { formatDuration } from '../../utils/formatDate';
import LyricCardModal from '../lyrics/LyricCardModal.vue';
import PremiumRequiredModal from '../premium/PremiumRequiredModal.vue';

const player = usePlayerStore();
const authStore = useAuthStore();
const premiumPromptOpen = ref(false);
const fullscreenLyricsBox = ref(null);
const lyricCardOpen = ref(false);
const lyricSelection = useLyricSelection();

const isLiked = computed(() => player.likedIds.has(player.currentTrack.id));

// Playback time is driven by the audio element's timeupdate event, so the
// store's `progress` is always current — no polling interval needed.
const progress = computed(() => player.progress);

const lyricLines = computed(() => {
  if (player.hasSyncedLyrics) return player.syncedLyrics;
  return Array.isArray(player.currentTrack.lyrics) ? player.currentTrack.lyrics : [];
});

const activeLyricLine = computed(() => {
  if (player.hasSyncedLyrics) return player.currentLyricLine;
  const count = lyricLines.value.length;
  if (!count || !player.duration) return null;
  return Math.min(count - 1, Math.floor((player.currentTime / player.duration) * count));
});

const selectedCardLines = computed(() => lyricSelection.sortedIndices.value
  .map((index) => lyricLines.value[index]?.text)
  .filter(Boolean));

function lyricLineState(index) {
  if (index === activeLyricLine.value) {
    return 'relative z-10 scale-110 text-[#20E878] opacity-100 drop-shadow-[0_0_22px_rgba(32,232,120,0.28)]';
  }
  if (activeLyricLine.value !== null && index < activeLyricLine.value) {
    return 'translate-y-3 scale-95 text-[#F4FFF7] opacity-[0.08]';
  }
  return 'scale-100 text-[#F4FFF7] opacity-25';
}

function closeLyrics() {
  player.fullscreenLyrics = false;
  lyricSelection.clear();
}

async function toggleLyrics() {
  if (!authStore.isPremium) {
    player.fullscreenLyrics = false;
    premiumPromptOpen.value = true;
    return;
  }
  if (player.fullscreenLyrics) {
    closeLyrics();
    return;
  }
  if (
    player.currentTrack?.lyricsType === 'SYNCED'
    && player.currentTrack?.slug
    && !player.hasSyncedLyrics
  ) {
    await player.loadSyncedLyrics(player.currentTrack.slug, player.currentTrack.id);
  }
  player.fullscreenLyrics = true;
}

watch(
  () => [activeLyricLine.value, player.fullscreenLyrics],
  async ([index, open]) => {
    if (!open || index === null) return;
    await nextTick();
    const activeLine = fullscreenLyricsBox.value?.querySelector(`[data-lyric-index="${index}"]`);
    activeLine?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }
);

watch(
  () => player.currentTrack.id,
  () => {
    lyricSelection.clear();
    lyricCardOpen.value = false;
  }
);

function seekTo(e) {
  const rect = e.currentTarget.getBoundingClientRect();
  const ratio = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
  player.seek(Math.floor(ratio * player.duration));
}
</script>

<template>
  <footer class="surface-glass fixed inset-x-2 bottom-2 z-[60] h-[5.5rem] rounded-2xl sm:inset-x-4 sm:bottom-4">
    <div class="grid h-full grid-cols-[1fr_minmax(320px,680px)_1fr] items-center gap-6 px-6 sm:px-8">

      <!-- Track info -->
      <div class="flex min-w-0 items-center gap-4">
        <div class="relative shrink-0">
          <img
            v-if="player.currentTrack.cover"
            :src="player.currentTrack.cover"
            :alt="`${player.currentTrack.title} cover`"
            class="size-14 rounded-xl object-cover"
          />
          <span
            v-else
            class="grid size-14 shrink-0 place-items-center rounded-xl bg-white/[0.04] text-[#71717A]"
          >
            <svg class="size-6" viewBox="0 0 24 24" fill="currentColor">
              <path d="M9 18V5l12-2v13" />
              <circle cx="6" cy="18" r="3" />
              <circle cx="18" cy="16" r="3" />
            </svg>
          </span>
        </div>
        <div class="min-w-0 flex-1">
          <p class="truncate text-base font-semibold text-[#F4FFF7]">{{ player.currentTrack.title }}</p>
          <p class="truncate text-xs text-[#A1A1AA]">{{ player.currentTrack.artist }}</p>
        </div>
        <button
          class="melodyhub-icon-btn size-9"
          :class="isLiked ? 'text-[#20E878]' : 'text-[#A1A1AA]'"
          @click="player.toggleLike(player.currentTrack.id)"
        >
          <Heart :size="17" :class="isLiked ? 'fill-current' : ''" />
        </button>
      </div>

      <!-- Controls -->
      <div class="flex w-full max-w-180 flex-col items-center gap-2.5 justify-self-center">
        <!-- Buttons row -->
        <div class="flex items-center gap-5">
          <button class="melodyhub-icon-btn size-9" @click="player.previous">
            <SkipBack :size="18" class="fill-current" />
          </button>

          <button
            class="flex size-11 shrink-0 items-center justify-center rounded-full bg-[#F4FFF7] text-[#0F0F12] transition-all duration-200 active:scale-95 hover:scale-105"
            @click="player.togglePlayback"
          >
            <Pause v-if="player.isPlaying" :size="20" class="fill-current" />
            <Play v-else :size="20" class="ml-0.5 fill-current" />
          </button>

          <button class="melodyhub-icon-btn size-9" @click="player.next">
            <SkipForward :size="18" class="fill-current" />
          </button>
        </div>

        <!-- Progress bar -->
        <div class="flex w-full items-center gap-3 sm:gap-4">
          <span class="w-10 shrink-0 text-right font-mono text-[11px] text-[#A1A1AA]">{{ formatDuration(player.currentTime) }}</span>
          <div
            class="group relative h-1 flex-1 cursor-pointer rounded-full bg-white/[0.08] transition-all duration-150 hover:h-1.5"
            @click="seekTo"
          >
            <div
              class="absolute inset-y-0 left-0 rounded-full bg-[#F4FFF7] transition-all"
              :style="{ width: `${progress}%` }"
            />
            <div
              class="absolute top-1/2 size-3 -translate-x-1/2 -translate-y-1/2 scale-0 rounded-full bg-[#F4FFF7] transition-transform group-hover:scale-100"
              :style="{ left: `${progress}%` }"
            />
          </div>
          <span class="w-10 shrink-0 font-mono text-[11px] text-[#A1A1AA]">{{ formatDuration(player.duration) }}</span>
        </div>
      </div>

      <!-- Volume -->
      <div class="hidden items-center justify-end gap-3 lg:flex">
        <button
          class="melodyhub-icon-btn size-9"
          :class="player.fullscreenLyrics ? 'text-[#20E878]' : 'text-[#A1A1AA]'"
          title="Show lyrics"
          @click="toggleLyrics"
        >
          <Mic2 :size="17" />
        </button>
        <button class="melodyhub-icon-btn size-9 text-[#A1A1AA]" @click="player.toggleMute">
          <VolumeX v-if="player.muted" :size="17" />
          <Volume2 v-else :size="17" />
        </button>
        <div class="w-28">
          <input
            type="range"
            min="0"
            max="100"
            :value="player.volume"
            class="melodyhub-range w-full"
            :style="{ '--range-progress': `${player.volume}%` }"
            @input="player.setVolume($event.target.value)"
          />
        </div>
      </div>
    </div>
  </footer>

  <PremiumRequiredModal :open="premiumPromptOpen" @close="premiumPromptOpen = false" />

  <!-- Queue panel -->
  <Teleport to="body">
    <Transition
      enter-active-class="transition-all duration-300 ease-out"
      enter-from-class="opacity-0 translate-x-6"
      enter-to-class="opacity-100 translate-x-0"
      leave-active-class="transition-all duration-200 ease-in"
      leave-from-class="opacity-100 translate-x-0"
      leave-to-class="opacity-0 translate-x-6"
    >
      <div
        v-if="player.queueOpen"
        class="surface-glass fixed inset-y-0 right-0 z-[80] w-[min(380px,92vw)]"
      >
        <div class="flex h-full flex-col">
          <header class="flex h-[3.75rem] shrink-0 items-center justify-between border-b border-white/[0.05] px-6">
            <div>
              <p class="text-xs font-bold uppercase tracking-[0.1em] text-[#F4FFF7]">Up Next</p>
              <p class="mt-0.5 font-mono text-[11px] text-[#71717A]">{{ tracks.length }} tracks</p>
            </div>
            <button class="melodyhub-icon-btn" @click="player.queueOpen = false">
              <svg class="size-4 text-[#A1A1AA]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
            </button>
          </header>

          <div class="flex-1 space-y-1 overflow-y-auto p-3">
            <button
              v-for="track in tracks"
              :key="track.id"
              class="group flex w-full items-center gap-3 rounded-lg p-2.5 text-left transition-all duration-200"
              :class="track.id === player.currentTrack.id
                ? 'bg-[#20E878]/[0.08]'
                : 'hover:bg-white/[0.04]'"
              @click="player.playTrack(track)"
            >
              <img :src="track.cover" alt="" class="size-10 shrink-0 rounded-md object-cover" />
              <span class="min-w-0 flex-1">
                <span
                  class="block truncate text-sm font-medium"
                  :class="track.id === player.currentTrack.id ? 'text-[#20E878]' : 'text-[#F4FFF7]'"
                >
                  {{ track.title }}
                </span>
                <span class="block truncate text-xs text-[#71717A]">{{ track.artist }}</span>
              </span>
              <span class="shrink-0 font-mono text-[10px] text-[#27272A]">
                {{ formatDuration(track.duration) }}
              </span>
              <span v-if="track.id === player.currentTrack.id" class="playing-bars shrink-0">
                <i /><i /><i />
              </span>
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Fullscreen lyrics -->
    <Transition
      enter-active-class="transition-all duration-400 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-all duration-300 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="player.fullscreenLyrics && authStore.isPremium"
        class="fixed left-0 right-0 top-[4.5rem] bottom-24 z-50 flex flex-col overflow-hidden bg-[#0F0F12] p-6 sm:bottom-[6.5rem] sm:p-10 lg:left-[240px] lg:rounded-tl-3xl"
      >
        <header class="flex shrink-0 items-center justify-between">
          <div class="flex items-center gap-4">
            <img :src="player.currentTrack.cover" alt="" class="size-11 rounded-lg object-cover" />
            <div>
              <p class="font-semibold text-[#F4FFF7]">{{ player.currentTrack.title }}</p>
              <p class="text-xs text-[#A1A1AA]">{{ player.currentTrack.artist }}</p>
            </div>
          </div>
          <button class="melodyhub-icon-btn" @click="closeLyrics">
            <svg class="size-5 text-[#A1A1AA]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
          </button>
        </header>

        <div
          ref="fullscreenLyricsBox"
          class="min-h-0 flex-1 overflow-y-auto scroll-smooth px-4"
        >
          <div class="mx-auto flex min-h-full w-full max-w-3xl flex-col items-center gap-7 py-[30vh]">
            <button
              v-for="(line, index) in lyricLines"
              :key="index"
              :data-lyric-index="index"
              type="button"
              class="w-full rounded-2xl px-5 py-2 text-center text-xl font-bold transition-[color,opacity,transform,background-color,box-shadow] duration-700 ease-out sm:text-3xl"
              :class="[
                lyricLineState(index),
                player.hasSyncedLyrics ? 'cursor-pointer hover:bg-white/[0.04]' : 'cursor-default',
                lyricSelection.isSelected(index)
                  ? 'bg-[#20E878]/10 ring-1 ring-inset ring-[#20E878]/60'
                  : ''
              ]"
              :disabled="!player.hasSyncedLyrics"
              :aria-pressed="player.hasSyncedLyrics ? lyricSelection.isSelected(index) : undefined"
              @click="lyricSelection.toggle(index)"
            >
              {{ player.hasSyncedLyrics ? line.text : line }}
            </button>
            <p v-if="!lyricLines.length" class="text-sm text-[#71717A]">No lyrics available for this track.</p>
          </div>
        </div>

        <div
          v-if="player.hasSyncedLyrics"
          class="flex shrink-0 flex-col gap-3 border-t border-white/[0.06] bg-[#0F0F12]/95 pt-4 sm:flex-row sm:items-center sm:justify-between"
        >
          <div>
            <p class="text-sm font-semibold text-[#F4FFF7]">Chọn 1–4 câu lyrics để tạo card</p>
            <p class="mt-0.5 text-xs text-[#71717A]">
              Đã chọn {{ lyricSelection.selected.value.size }}/{{ lyricSelection.MAX_LINES }} câu liền nhau
            </p>
          </div>
          <div class="flex items-center gap-2">
            <button
              v-if="lyricSelection.hasSelection.value"
              type="button"
              class="h-10 rounded-full px-4 text-xs font-bold text-[#A1A1AA] transition hover:bg-white/[0.05] hover:text-white"
              @click="lyricSelection.clear"
            >
              Bỏ chọn
            </button>
            <button
              type="button"
              class="inline-flex h-10 items-center justify-center gap-2 rounded-full bg-[#20E878] px-5 text-xs font-black text-[#09090B] transition hover:bg-[#64F4A1] disabled:cursor-not-allowed disabled:opacity-35"
              :disabled="!lyricSelection.hasSelection.value"
              @click="lyricCardOpen = true"
            >
              <Sparkles :size="15" /> Tạo lyric card
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>

  <LyricCardModal
    :open="lyricCardOpen"
    :lines="selectedCardLines"
    :title="player.currentTrack.title"
    :artist="player.currentTrack.artist"
    :cover-url="player.currentTrack.cover"
    @close="lyricCardOpen = false"
  />
</template>
