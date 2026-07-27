<script setup>
import { computed, onMounted, ref } from 'vue';
import { LoaderCircle, Mic2, RefreshCw, Search, UserRound } from '@lucide/vue';
import { adminService } from '../../services/adminService';

const search = ref('');
const artists = ref([]);
const total = ref(0);
const isLoading = ref(true);
const error = ref('');

const isEmpty = computed(() => !isLoading.value && artists.value.length === 0);

async function load() {
  isLoading.value = true;
  error.value = '';
  try {
    const paged = await adminService.listArtists({ q: search.value.trim(), size: 50 });
    artists.value = paged?.items || [];
    total.value = paged?.total || 0;
  } catch (requestError) {
    error.value = requestError.message || 'Unable to load artists.';
  } finally {
    isLoading.value = false;
  }
}

function formatDate(value) {
  if (!value) return '';
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(value));
}

onMounted(load);
</script>

<template>
  <div class="mx-auto w-full max-w-6xl px-5 py-8 pb-12 sm:px-8">
    <div class="mb-6 flex items-center gap-3">
      <Mic2 :size="28" class="text-[#1DB954]" />
      <div>
        <p class="melodyhub-kicker">ADMIN</p>
        <h1 class="melodyhub-section-title">Artists <span class="text-sm font-normal text-[#666]">({{ total }})</span></h1>
      </div>
    </div>

    <div class="mb-6 flex items-center justify-end gap-2">
      <label class="flex h-9 items-center gap-2 rounded-full bg-white/5 px-3 ring-1 ring-white/10 focus-within:ring-[#1DB954]/60">
        <Search :size="15" class="text-[#888]" />
        <input
          v-model="search"
          class="w-40 bg-transparent text-sm text-white outline-none placeholder:text-[#666]"
          placeholder="Search artists"
          @keyup.enter="load"
        />
      </label>
      <button
        class="inline-flex h-9 items-center gap-2 rounded-full border border-white/15 px-4 text-xs font-bold text-[#bbb] transition hover:border-[#1DB954]/70 hover:text-white disabled:opacity-50"
        :disabled="isLoading"
        @click="load"
      >
        <RefreshCw :size="14" :class="{ 'animate-spin': isLoading }" /> Refresh
      </button>
    </div>

    <p v-if="error" class="mb-4 rounded-md bg-red-500/10 px-3 py-2 text-xs text-red-300" role="alert">{{ error }}</p>

    <div v-if="isLoading" class="flex min-h-64 items-center justify-center text-sm text-[#888]">
      <LoaderCircle :size="20" class="mr-3 animate-spin text-[#1DB954]" /> Loading artists
    </div>

    <div v-else-if="isEmpty" class="flex min-h-48 items-center justify-center border border-white/10 bg-[#121212] text-sm text-[#888]">
      No artists found.
    </div>

    <ul v-else class="grid gap-3 sm:grid-cols-2">
      <li
        v-for="artist in artists"
        :key="artist.id"
        class="flex items-center gap-4 border border-white/10 bg-[#121212] p-4"
      >
        <div class="size-14 shrink-0 overflow-hidden rounded-full bg-white/[0.04]">
          <img v-if="artist.imageUrl" :src="artist.imageUrl" :alt="artist.name" class="h-full w-full object-cover" />
          <span v-else class="grid h-full w-full place-items-center text-[#555]"><UserRound :size="22" /></span>
        </div>
        <div class="min-w-0 flex-1">
          <p class="truncate text-base font-black text-white">{{ artist.name }}</p>
          <p class="truncate text-xs text-[#888]">/artist/{{ artist.slug }}</p>
          <p v-if="artist.linkedUsername" class="mt-1 truncate text-xs text-[#aaa]">
            Account: <span class="text-[#ddd]">@{{ artist.linkedUsername }}</span>
            <span class="text-[#666]"> · {{ artist.linkedEmail }}</span>
          </p>
          <p v-else class="mt-1 text-xs text-[#666]">No linked account</p>
          <p class="mt-1 text-[11px] text-[#666]">Since {{ formatDate(artist.createdAt) }}</p>
        </div>
      </li>
    </ul>
  </div>
</template>
