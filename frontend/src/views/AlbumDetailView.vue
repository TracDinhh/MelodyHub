<script setup>
import { onMounted, ref } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { Disc3, LoaderCircle, Music2, Play } from '@lucide/vue';
import { usePlayerStore } from '../stores/player.store';

const route = useRoute();
const router = useRouter();
const playerStore = usePlayerStore();

const album = ref(null);
const albumSongs = ref([]);
const isLoading = ref(true);
const error = ref('');

const API_BASE = import.meta.env.VITE_API_BASE_URL || '';

async function load() {
  const slug = route.params.slug;
  isLoading.value = true;
  error.value = '';

  try {
    const response = await fetch(`${API_BASE}/api/albums/${slug}`);
    if (!response.ok) {
      error.value = 'Album not found.';
      return;
    }
    const data = await response.json();
    album.value = data;
    albumSongs.value = data.songs || [];
  } catch (e) {
    error.value = 'Unable to load album.';
  } finally {
    isLoading.value = false;
  }
}

function formatDuration(seconds) {
  if (!seconds) return '';
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}

function playSong(song) {
  const track = {
    id: song.id,
    title: song.title,
    slug: song.slug,
    coverUrl: song.coverUrl,
    durationSec: song.durationSec,
    audioUrl: song.audioUrl,
    artists: song.artists
  };
  playerStore.playTrack(track);
}

function playAll() {
  if (!albumSongs.value.length) return;
  const firstSong = albumSongs.value[0];
  const track = {
    id: firstSong.id,
    title: firstSong.title,
    slug: firstSong.slug,
    coverUrl: firstSong.coverUrl,
    durationSec: firstSong.durationSec,
    audioUrl: firstSong.audioUrl,
    artists: firstSong.artists
  };
  playerStore.playTrack(track, albumSongs.value.map(s => ({
    id: s.id,
    title: s.title,
    slug: s.slug,
    coverUrl: s.coverUrl,
    durationSec: s.durationSec,
    audioUrl: s.audioUrl,
    artists: s.artists
  })));
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  return new Date(dateStr).getFullYear();
}

onMounted(load);
</script>

<template>
  <div class="mx-auto w-full max-w-5xl px-5 py-8 pb-12 sm:px-8">
    <div v-if="isLoading" class="flex min-h-64 items-center justify-center text-sm text-[#888]">
      <LoaderCircle :size="20" class="mr-3 animate-spin text-[#16C65A]" /> Loading album
    </div>

    <div v-else-if="error" class="flex min-h-64 items-center justify-center text-sm text-red-400">
      {{ error }}
    </div>

    <div v-else-if="album">
      <!-- Album Header -->
      <div class="mb-8 flex flex-col gap-6 sm:flex-row sm:items-end">
        <div class="size-52 shrink-0 overflow-hidden rounded-lg bg-[#222] shadow-xl sm:size-60">
          <img v-if="album.coverUrl" :src="album.coverUrl" :alt="album.title" class="h-full w-full object-cover" />
          <div v-else class="grid h-full w-full place-items-center">
            <Disc3 :size="64" class="text-[#333]" />
          </div>
        </div>
        <div class="flex-1">
          <p class="text-xs font-bold uppercase tracking-wider text-[#999]">Album</p>
          <h1 class="mt-1 text-4xl font-black text-white sm:text-5xl">{{ album.title }}</h1>
          <div class="mt-3 flex flex-wrap items-center gap-3 text-sm text-[#999]">
            <span class="font-medium text-white">{{ album.artistName || 'Unknown Artist' }}</span>
            <span v-if="album.releaseDate">· {{ formatDate(album.releaseDate) }}</span>
            <span v-if="album.songCount">· {{ album.songCount }} songs</span>
          </div>
          <button
            class="mt-5 inline-flex items-center gap-2 rounded-full bg-[#16C65A] px-6 py-2.5 text-xs font-black text-black transition hover:bg-[#22C55E]"
            @click="playAll"
          >
            <Play :size="16" fill="currentColor" /> Play
          </button>
        </div>
      </div>

      <!-- Songs -->
      <div v-if="albumSongs.length > 0">
        <ul class="space-y-1">
          <li
            v-for="(song, index) in albumSongs"
            :key="song.id"
            class="group flex items-center gap-4 rounded-md px-3 py-2 transition hover:bg-white/5"
          >
            <span class="w-8 text-center text-xs text-[#555] group-hover:hidden">{{ index + 1 }}</span>
            <button class="hidden text-[#16C65A] group-hover:block" @click="playSong(song)">
              <Play :size="16" fill="currentColor" />
            </button>
            <div class="size-10 shrink-0 overflow-hidden rounded">
              <img v-if="song.coverUrl" :src="song.coverUrl" :alt="song.title" class="h-full w-full object-cover" />
              <div v-else class="grid h-full w-full place-items-center bg-[#222]">
                <Music2 :size="16" class="text-[#555]" />
              </div>
            </div>
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm font-medium text-white">{{ song.title }}</p>
            </div>
            <span class="text-xs text-[#555]">{{ formatDuration(song.durationSec) }}</span>
          </li>
        </ul>
      </div>
      <div v-else class="py-12 text-center text-[#555]">
        <Music2 :size="32" class="mx-auto mb-3 text-[#444]" />
        <p>No songs in this album yet.</p>
      </div>
    </div>
  </div>
</template>
