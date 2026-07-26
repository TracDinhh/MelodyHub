<script setup>
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { LoaderCircle, Music2, Pencil, Plus, UploadCloud } from '@lucide/vue';
import { songService } from '../../services/songService';

const songs = ref([]);
const total = ref(0);
const isLoading = ref(true);
const error = ref('');

const isEmpty = computed(() => !isLoading.value && songs.value.length === 0);

async function load() {
  isLoading.value = true;
  error.value = '';
  try {
    const paged = await songService.listMine({ page: 1, size: 50 });
    songs.value = paged?.items || [];
    total.value = paged?.total || 0;
  } catch (requestError) {
    error.value = requestError.message || 'Unable to load your songs.';
  } finally {
    isLoading.value = false;
  }
}

function formatDuration(seconds) {
  if (!seconds) return '—';
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}

onMounted(load);
</script>

<template>
  <div class="mx-auto w-full max-w-5xl px-5 py-8 pb-12 sm:px-8">
    <div class="mb-6 flex flex-wrap items-center justify-between gap-3">
      <div>
        <p class="sonix-kicker">ARTIST</p>
        <h1 class="sonix-section-title">My Songs <span class="text-sm font-normal text-[#666]">({{ total }})</span></h1>
      </div>
      <RouterLink
        :to="{ name: 'artist-song-upload' }"
        class="inline-flex h-10 items-center gap-2 rounded-full bg-[#1DB954] px-5 text-xs font-black text-black transition hover:bg-[#20ca5c]"
      >
        <Plus :size="16" /> Upload song
      </RouterLink>
    </div>

    <p v-if="error" class="mb-4 rounded-md bg-red-500/10 px-3 py-2 text-xs text-red-300" role="alert">{{ error }}</p>

    <div v-if="isLoading" class="flex min-h-64 items-center justify-center text-sm text-[#888]">
      <LoaderCircle :size="20" class="mr-3 animate-spin text-[#1DB954]" /> Loading songs
    </div>

    <div v-else-if="isEmpty" class="flex min-h-56 flex-col items-center justify-center gap-4 border border-white/10 bg-[#121212] text-center">
      <UploadCloud :size="34" class="text-[#1DB954]" />
      <p class="text-sm text-[#999]">You haven't uploaded any songs yet.</p>
      <RouterLink
        :to="{ name: 'artist-song-upload' }"
        class="inline-flex h-10 items-center gap-2 rounded-full bg-[#1DB954] px-5 text-xs font-black text-black transition hover:bg-[#20ca5c]"
      >
        <Plus :size="16" /> Upload your first song
      </RouterLink>
    </div>

    <ul v-else class="space-y-2">
      <li
        v-for="song in songs"
        :key="song.id"
        class="flex items-center gap-4 border border-white/10 bg-[#121212] p-3 sm:p-4"
      >
        <div class="size-12 shrink-0 overflow-hidden rounded bg-white/[0.04]">
          <img v-if="song.coverUrl" :src="song.coverUrl" :alt="song.title" class="h-full w-full object-cover" />
          <span v-else class="grid h-full w-full place-items-center text-[#555]"><Music2 :size="20" /></span>
        </div>
        <div class="min-w-0 flex-1">
          <p class="truncate text-sm font-bold text-white">{{ song.title }}</p>
          <p class="truncate text-xs text-[#777]">/songs/{{ song.slug }}</p>
        </div>
        <span
          class="rounded-full px-2.5 py-1 text-[11px] font-bold"
          :class="song.status === 'PUBLISHED' ? 'bg-[#1DB954]/15 text-[#1DB954]' : 'bg-white/10 text-[#bbb]'"
        >{{ song.status }}</span>
        <span class="w-12 text-right text-xs text-[#888]">{{ formatDuration(song.durationSec) }}</span>
        <RouterLink
          :to="{ name: 'artist-song-edit', params: { id: song.id } }"
          class="grid size-8 place-items-center rounded-full text-[#888] transition hover:bg-white/10 hover:text-white"
          title="Edit song"
        >
          <Pencil :size="15" />
        </RouterLink>
      </li>
    </ul>
  </div>
</template>
