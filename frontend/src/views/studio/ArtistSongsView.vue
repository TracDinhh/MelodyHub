<script setup>
import { computed, onMounted, ref } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { LoaderCircle, Music2, Pencil, Plus, UploadCloud } from '@lucide/vue';
import { studioService } from '../../services/studioService';

const route = useRoute();
const artistId = Number(route.params.artistId);

const songs = ref([]);
const total = ref(0);
const page = ref(1);
const size = ref(20);
const isLoading = ref(true);
const error = ref('');

const isEmpty = computed(() => !isLoading.value && songs.value.length === 0 && page.value === 1);
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)));

async function load() {
  isLoading.value = true;
  error.value = '';
  try {
    const paged = await studioService.listSongs(artistId, { page: page.value, size: size.value });
    songs.value = paged?.items || [];
    total.value = paged?.total || 0;
  } catch (requestError) {
    error.value = requestError.message || 'Unable to load your songs.';
  } finally {
    isLoading.value = false;
  }
}

function changePage(newPage) {
  if (newPage < 1 || newPage > totalPages.value || newPage === page.value) return;
  page.value = newPage;
  load();
}

function changeSize(newSize) {
  size.value = newSize;
  page.value = 1;
  load();
}

function formatDuration(seconds) {
  if (!seconds) return '—';
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}

function statusClass(status) {
  if (status === 'PUBLISHED') return 'bg-[#16C65A]/15 text-[#16C65A]';
  if (status === 'DRAFT') return 'bg-amber-500/15 text-amber-300';
  return 'bg-white/10 text-[#bbb]';
}

onMounted(load);
</script>

<template>
  <div class="mx-auto w-full max-w-6xl px-5 py-8 pb-12 sm:px-8">
    <div class="mb-6 flex flex-wrap items-center justify-between gap-3">
      <div class="flex items-center gap-3">
        <Music2 :size="28" class="text-[#16C65A]" />
        <div>
          <p class="melodyhub-kicker">STUDIO</p>
          <h1 class="melodyhub-section-title">My Songs <span class="text-sm font-normal text-[#666]">({{ total }})</span></h1>
        </div>
      </div>
      <RouterLink
        :to="{ name: 'studio-artist-upload', params: { artistId } }"
        class="inline-flex h-10 items-center gap-2 rounded-full bg-[#16C65A] px-5 text-xs font-black text-black transition hover:bg-[#22C55E]"
      >
        <Plus :size="16" /> Upload song
      </RouterLink>
    </div>

    <p v-if="error" class="mb-4 rounded-md bg-red-500/10 px-3 py-2 text-xs text-red-300" role="alert">{{ error }}</p>

    <div v-if="isLoading" class="flex min-h-64 items-center justify-center text-sm text-[#888]">
      <LoaderCircle :size="20" class="mr-3 animate-spin text-[#16C65A]" /> Loading songs
    </div>

    <div v-else-if="isEmpty" class="flex min-h-56 flex-col items-center justify-center gap-4 border border-white/10 bg-[#111827] text-center">
      <UploadCloud :size="34" class="text-[#16C65A]" />
      <p class="text-sm text-[#999]">You haven't uploaded any songs yet.</p>
      <RouterLink
        :to="{ name: 'studio-artist-upload', params: { artistId } }"
        class="inline-flex h-10 items-center gap-2 rounded-full bg-[#16C65A] px-5 text-xs font-black text-black transition hover:bg-[#22C55E]"
      >
        <Plus :size="16" /> Upload your first song
      </RouterLink>
    </div>

    <template v-else>
      <!-- Table header -->
      <div class="mb-2 hidden items-center gap-4 px-4 text-[10px] font-bold uppercase tracking-widest text-[#666] sm:flex">
        <span class="w-12 shrink-0"></span>
        <span class="flex-1">Title</span>
        <span class="w-24 text-center">Status</span>
        <span class="w-20 text-right">Plays</span>
        <span class="w-20 text-right">Likes</span>
        <span class="w-16 text-right">Duration</span>
        <span class="w-8"></span>
      </div>

      <!-- Song rows -->
      <ul class="space-y-1">
        <li
          v-for="song in songs"
          :key="song.id"
          class="group flex items-center gap-4 border border-white/[0.06] bg-[#111827] p-3 transition hover:border-white/10 hover:bg-[#111827]/80 sm:p-4"
        >
          <div class="size-12 shrink-0 overflow-hidden rounded bg-white/[0.04]">
            <img v-if="song.coverUrl" :src="song.coverUrl" :alt="song.title" class="h-full w-full object-cover" />
            <span v-else class="grid h-full w-full place-items-center text-[#555]"><Music2 :size="20" /></span>
          </div>
          <div class="min-w-0 flex-1">
            <p class="truncate text-sm font-bold text-white">{{ song.title }}</p>
            <p class="truncate text-xs text-[#555]">/songs/{{ song.slug }}</p>
          </div>
          <span
            class="hidden w-24 justify-center rounded-full px-2.5 py-1 text-center text-[11px] font-bold sm:inline-flex"
            :class="statusClass(song.status)"
          >{{ song.status }}</span>
          <span class="hidden w-20 text-right text-xs text-[#888] sm:block">{{ (song.playCount || 0).toLocaleString() }}</span>
          <span class="hidden w-20 text-right text-xs text-[#888] sm:block">{{ (song.likeCount || 0).toLocaleString() }}</span>
          <span class="hidden w-16 text-right text-xs text-[#888] sm:block">{{ formatDuration(song.durationSec) }}</span>
          <RouterLink
            :to="{ name: 'studio-artist-song-edit', params: { artistId, songId: song.id } }"
            class="grid size-8 place-items-center rounded-full text-[#888] transition hover:bg-white/10 hover:text-white"
            title="Edit song"
          >
            <Pencil :size="15" />
          </RouterLink>
        </li>
      </ul>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="mt-6 flex items-center justify-between gap-3">
        <div class="flex items-center gap-2">
          <span class="text-xs text-[#666]">Show</span>
          <select
            :value="size"
            class="rounded border border-white/10 bg-[#111827] px-2 py-1 text-xs text-white"
            @change="changeSize(Number($event.target.value))"
          >
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
          </select>
          <span class="text-xs text-[#666]">per page</span>
        </div>

        <div class="flex items-center gap-3 text-xs font-bold text-[#8EA696]">
          <button
            class="rounded-md border border-white/[0.08] px-3 py-2 transition hover:border-[#16C65A]/50 hover:text-[#16C65A] disabled:cursor-not-allowed disabled:opacity-40"
            :disabled="page === 1"
            @click="changePage(page - 1)"
          >Previous</button>
          <span>Page {{ page }} of {{ totalPages }}</span>
          <button
            class="rounded-md border border-white/[0.08] px-3 py-2 transition hover:border-[#16C65A]/50 hover:text-[#16C65A] disabled:cursor-not-allowed disabled:opacity-40"
            :disabled="page === totalPages"
            @click="changePage(page + 1)"
          >Next</button>
        </div>
      </div>
    </template>
  </div>
</template>