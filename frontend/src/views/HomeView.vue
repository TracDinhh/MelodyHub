<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ArrowRight, CheckCircle2, ChevronLeft, ChevronRight, Music2, Play } from '@lucide/vue';
import { playlists, podcasts, tracks } from '../data/music';
import { artistBrowseService } from '../services/artistBrowseService';
import { songService } from '../services/songService';
import { useAuthStore } from '../stores/auth.store';
import { usePlayerStore } from '../stores/player.store';

const route = useRoute();
const authStore = useAuthStore();
const player = usePlayerStore();
const artistScroller = ref(null);

// Newest published songs from the real API (ordered created_at DESC by the backend).
const newReleases = ref([]);
const newReleasesLoading = ref(true);

// Real artists from the public API.
const topArtists = ref([]);
const topArtistsLoading = ref(true);

async function loadTopArtists() {
  topArtistsLoading.value = true;
  try {
    const response = await artistBrowseService.list({ page: 1, size: 12 });
    topArtists.value = response?.items || [];
  } catch {
    topArtists.value = [];
  } finally {
    topArtistsLoading.value = false;
  }
}

function formatReleaseDate(value) {
  if (!value) return '';
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(value));
}

async function loadNewReleases() {
  newReleasesLoading.value = true;
  try {
    const response = await songService.listPublic({ page: 1, size: 8 });
    newReleases.value = response?.items || [];
  } catch {
    newReleases.value = [];
  } finally {
    newReleasesLoading.value = false;
  }
}

function toPlayerTrack(song) {
  return {
    id: song.id,
    title: song.title,
    cover: song.coverUrl,
    artist: '',
    album: '',
    duration: song.durationSec || 0,
    audioUrl: song.audioUrl
  };
}

function playNewRelease(song) {
  // Play the real audio and queue the whole new-releases list for next/prev.
  const list = newReleases.value.map(toPlayerTrack);
  player.playTrack(toPlayerTrack(song), list);
}

onMounted(() => {
  loadNewReleases();
  loadTopArtists();
});

const displayName = computed(
  () => authStore.user?.displayName || authStore.user?.username || 'Alex'
);
const greeting = computed(() => {
  const hour = new Date().getHours();
  if (hour < 12) return 'Good morning';
  if (hour < 18) return 'Good afternoon';
  return 'Good evening';
});
const sectionTitle = computed(() => {
  const names = {
    explore: 'Discover something new',
    radio: 'Radio made for you',
    artists: 'Artists in your orbit',
    albums: 'Albums worth a full listen',
    podcasts: 'Stories for your day'
  };
  return names[route.name] || `${greeting.value}, ${displayName.value}`;
});

function scrollArtists(direction) {
  artistScroller.value?.scrollBy({ left: direction * 320, behavior: 'smooth' });
}
</script>

<template>
  <div class="mx-auto max-w-[1180px] space-y-9 px-4 py-6 sm:px-6 sm:py-8">
    <section class="relative overflow-hidden rounded-xl bg-[#151515] px-6 py-7 sm:px-8">
      <div
        class="absolute inset-0 bg-cover bg-center opacity-25"
        :style="{ backgroundImage: `url(${playlists[0].cover})` }"
      />
      <div class="absolute inset-0 bg-gradient-to-r from-[#151515] via-[#151515]/85 to-transparent" />
      <div class="relative max-w-xl">
        <p class="text-xs font-black tracking-[0.16em] text-[#1DB954]">YOUR DAILY MIX</p>
        <h1 class="mt-2 text-2xl font-black text-white sm:text-4xl">{{ sectionTitle }}</h1>
        <p class="mt-3 max-w-lg text-sm leading-6 text-[#a3a3a3]">
          A fresh mix of artists you love and the sounds shaping this week.
        </p>
        <button class="mt-5 inline-flex h-10 items-center gap-2 rounded-full bg-[#1DB954] px-5 text-xs font-black text-black transition hover:scale-[1.03]" @click="player.playTrack(tracks[0])">
          <Play :size="16" class="fill-current" /> PLAY MIX
        </button>
      </div>
    </section>

    <section>
      <div class="mb-4 flex items-end justify-between">
        <div><p class="sonix-kicker">CURATED FOR YOU</p><h2 class="sonix-section-title">Trending playlists</h2></div>
        <RouterLink :to="{ name: 'explore' }" class="flex items-center gap-1 text-xs font-bold text-[#8b8b8b] hover:text-white">See all <ArrowRight :size="14" /></RouterLink>
      </div>
      <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <button
          v-for="(playlist, index) in playlists"
          :key="playlist.id"
          class="group min-w-0 text-left"
          @click="player.playTrack(tracks[index])"
        >
          <span class="relative block aspect-square overflow-hidden rounded-lg bg-[#181818]">
            <img :src="playlist.cover" :alt="playlist.title" class="h-full w-full object-cover transition duration-500 group-hover:scale-105" />
            <span class="absolute inset-0 bg-black/10 transition group-hover:bg-black/25" />
            <span class="absolute right-3 bottom-3 grid size-11 translate-y-3 place-items-center rounded-full bg-[#1DB954] text-black opacity-0 shadow-xl transition group-hover:translate-y-0 group-hover:opacity-100">
              <Play :size="19" class="ml-0.5 fill-current" />
            </span>
          </span>
          <span class="mt-3 block truncate text-sm font-bold text-white">{{ playlist.title }}</span>
          <span class="mt-1 block line-clamp-2 text-xs leading-5 text-[#777]">{{ playlist.subtitle }}</span>
        </button>
      </div>
    </section>

    <section>
      <div class="mb-4 flex items-end justify-between">
        <div><p class="sonix-kicker">ON REPEAT</p><h2 class="sonix-section-title">Top artists</h2></div>
        <div class="flex gap-1">
          <button class="sonix-icon-btn" title="Previous artists" @click="scrollArtists(-1)"><ChevronLeft :size="18" /></button>
          <button class="sonix-icon-btn" title="Next artists" @click="scrollArtists(1)"><ChevronRight :size="18" /></button>
        </div>
      </div>
      <div v-if="topArtistsLoading" class="flex gap-4 overflow-hidden">
        <div v-for="n in 6" :key="n" class="w-32 shrink-0 text-center sm:w-40">
          <span class="mx-auto block aspect-square animate-pulse rounded-full bg-white/5" />
          <span class="mx-auto mt-3 block h-4 w-20 animate-pulse rounded bg-white/5" />
        </div>
      </div>

      <div v-else-if="topArtists.length === 0" class="rounded-lg border border-white/10 bg-white/[0.02] px-4 py-8 text-center text-sm text-[#777]">
        No artists yet.
      </div>

      <div v-else ref="artistScroller" class="no-scrollbar flex snap-x gap-4 overflow-x-auto">
        <RouterLink
          v-for="artist in topArtists"
          :key="artist.id"
          :to="{ name: 'artist-detail', params: { slug: artist.slug } }"
          class="group w-32 shrink-0 snap-start text-center sm:w-40"
        >
          <span class="relative mx-auto block aspect-square overflow-hidden rounded-full bg-[#181818] ring-1 ring-white/5">
            <img v-if="artist.imageUrl" :src="artist.imageUrl" :alt="artist.name" class="h-full w-full object-cover transition duration-500 group-hover:scale-105" />
            <span v-else class="grid h-full w-full place-items-center text-[#555]"><CheckCircle2 :size="26" /></span>
          </span>
          <span class="mt-3 block truncate text-sm font-bold text-white">{{ artist.name }}</span>
          <span class="mt-1 block truncate text-xs text-[#777]">Artist</span>
        </RouterLink>
      </div>
    </section>

    <section>
      <div class="mb-4"><p class="sonix-kicker">JUST LANDED</p><h2 class="sonix-section-title">New releases</h2></div>

      <div v-if="newReleasesLoading" class="grid gap-2 sm:grid-cols-2">
        <div v-for="n in 4" :key="n" class="flex items-center gap-3 rounded-lg p-2">
          <span class="size-13 shrink-0 animate-pulse rounded-md bg-white/5" />
          <span class="h-4 w-40 animate-pulse rounded bg-white/5" />
        </div>
      </div>

      <div v-else-if="newReleases.length === 0" class="rounded-lg border border-white/10 bg-white/[0.02] px-4 py-8 text-center text-sm text-[#777]">
        No songs have been published yet.
      </div>

      <div v-else class="grid gap-2 sm:grid-cols-2">
        <button
          v-for="song in newReleases"
          :key="song.id"
          class="group flex min-w-0 items-center gap-3 rounded-lg p-2 text-left transition hover:bg-white/5"
          @click="playNewRelease(song)"
        >
          <span class="relative shrink-0">
            <img
              v-if="song.coverUrl"
              :src="song.coverUrl"
              :alt="`${song.title} cover`"
              class="size-13 rounded-md object-cover"
            />
            <span v-else class="grid size-13 place-items-center rounded-md bg-white/[0.06] text-[#555]">
              <Music2 :size="20" />
            </span>
            <span class="absolute inset-0 grid place-items-center rounded-md bg-black/50 opacity-0 transition group-hover:opacity-100">
              <Play :size="17" class="fill-white text-white" />
            </span>
          </span>
          <span class="min-w-0 flex-1">
            <span class="block truncate text-sm font-bold text-white">{{ song.title }}</span>
            <span class="mt-1 block truncate text-xs text-[#777]">/songs/{{ song.slug }}</span>
          </span>
          <span class="text-[10px] font-bold text-[#666]">{{ formatReleaseDate(song.createdAt) }}</span>
        </button>
      </div>
    </section>

    <section class="pb-8">
      <div class="mb-4 flex items-end justify-between">
        <div><p class="sonix-kicker">LISTEN DEEPER</p><h2 class="sonix-section-title">Podcasts for you</h2></div>
        <RouterLink :to="{ name: 'podcasts' }" class="text-xs font-bold text-[#8b8b8b] hover:text-white">Browse podcasts</RouterLink>
      </div>
      <div class="grid gap-3 sm:grid-cols-3">
        <article v-for="podcast in podcasts" :key="podcast.id" class="flex min-w-0 gap-3 rounded-lg bg-white/[0.035] p-3 transition hover:bg-white/[0.065]">
          <img :src="podcast.cover" :alt="podcast.title" class="size-20 rounded-md object-cover" />
          <div class="min-w-0">
            <p class="line-clamp-2 text-sm font-bold text-white">{{ podcast.title }}</p>
            <p class="mt-1 truncate text-xs text-[#777]">{{ podcast.host }}</p>
            <p class="mt-3 text-[10px] font-bold text-[#1DB954]">{{ podcast.length }}</p>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>
