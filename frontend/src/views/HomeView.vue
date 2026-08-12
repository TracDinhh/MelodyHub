<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ArrowRight, CheckCircle2, ChevronLeft, ChevronRight, Music2, Play, Search, X } from '@lucide/vue';
import { playlists, podcasts, tracks } from '../data/music';
import AddToPlaylistButton from '../components/music/AddToPlaylistButton.vue';
import { artistBrowseService } from '../services/artistBrowseService';
import { songService } from '../services/songService';
import { useAuthStore } from '../stores/auth.store';
import { usePlayerStore } from '../stores/player.store';

const route = useRoute();
const authStore = useAuthStore();
const player = usePlayerStore();
const artistScroller = ref(null);

// Newest published songs from the real API.
const newReleases = ref([]);
const newReleasesLoading = ref(true);

// Real artists from the public API.
const topArtists = ref([]);
const topArtistsLoading = ref(true);

// ── Search state ───────────────────────────────────────────────
const searchSongs = ref([]);
const searchSongsTotal = ref(0);
const searchSongsPage = ref(1);
const searchArtists = ref([]);
const searchArtistsTotal = ref(0);
const searchArtistsPage = ref(1);
const searchLoading = ref(false);
const searchError = ref('');

const searchQuery = computed(() => {
  const q = route.query.q;
  return typeof q === 'string' ? q.trim() : '';
});

const isSearchMode = computed(() => Boolean(searchQuery.value));

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
    audioUrl: song.audioUrl,
    lyricsType: song.lyricsType || 'PLAIN',
    slug: song.slug
  };
}

async function loadSyncedLyrics(slug) {
  if (!slug) return;
  try {
    const data = await songService.getSyncedLyrics(slug);
    if (data.lyricsType === 'SYNCED' && data.lines) {
      player.syncedLyrics = data.lines;
    } else {
      player.syncedLyrics = [];
    }
  } catch (e) {
    player.syncedLyrics = [];
  }
}

function playNewRelease(song) {
  const list = newReleases.value.map(toPlayerTrack);
  player.playTrack(toPlayerTrack(song), list);
  if (song.lyricsType === 'SYNCED') {
    loadSyncedLyrics(song.slug);
  } else {
    player.syncedLyrics = [];
  }
}

function playSearchSong(song, list) {
  player.playTrack(toPlayerTrack(song), list);
  if (song.lyricsType === 'SYNCED') {
    loadSyncedLyrics(song.slug);
  } else {
    player.syncedLyrics = [];
  }
}

// ── Search ─────────────────────────────────────────────────────
const SEARCH_SIZE = 10;

async function doSearch() {
  if (!isSearchMode.value) return;

  searchLoading.value = true;
  searchError.value = '';
  searchSongs.value = [];
  searchArtists.value = [];

  try {
    const [songsRes, artistsRes] = await Promise.all([
      songService.listPublic({ q: searchQuery.value, page: searchSongsPage.value, size: SEARCH_SIZE }),
      artistBrowseService.list({ q: searchQuery.value, page: searchArtistsPage.value, size: SEARCH_SIZE })
    ]);

    searchSongs.value = songsRes?.items || [];
    searchSongsTotal.value = songsRes?.total || 0;
    searchArtists.value = artistsRes?.items || [];
    searchArtistsTotal.value = artistsRes?.total || 0;
  } catch (caught) {
    searchError.value = caught?.message || 'Search failed. Please try again.';
  } finally {
    searchLoading.value = false;
  }
}

async function loadMoreSongs() {
  if (searchSongsPage.value * SEARCH_SIZE >= searchSongsTotal.value) return;
  searchSongsPage.value += 1;
  searchLoading.value = true;
  try {
    const res = await songService.listPublic({
      q: searchQuery.value,
      page: searchSongsPage.value,
      size: SEARCH_SIZE
    });
    searchSongs.value = [...searchSongs.value, ...(res?.items || [])];
    searchSongsTotal.value = res?.total || searchSongsTotal.value;
  } catch {
    searchSongsPage.value -= 1;
  } finally {
    searchLoading.value = false;
  }
}

async function loadMoreArtists() {
  if (searchArtistsPage.value * SEARCH_SIZE >= searchArtistsTotal.value) return;
  searchArtistsPage.value += 1;
  searchLoading.value = true;
  try {
    const res = await artistBrowseService.list({
      q: searchQuery.value,
      page: searchArtistsPage.value,
      size: SEARCH_SIZE
    });
    searchArtists.value = [...searchArtists.value, ...(res?.items || [])];
    searchArtistsTotal.value = res?.total || searchArtistsTotal.value;
  } catch {
    searchArtistsPage.value -= 1;
  } finally {
    searchLoading.value = false;
  }
}

watch(searchQuery, () => {
  searchSongsPage.value = 1;
  searchArtistsPage.value = 1;
  doSearch();
}, { immediate: false });

watch(() => route.query.q, () => {
  if (!isSearchMode.value) {
    searchSongs.value = [];
    searchArtists.value = [];
  }
});

onMounted(() => {
  loadNewReleases();
  loadTopArtists();
  if (isSearchMode.value) doSearch();
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
  if (isSearchMode.value) return `Results for "${searchQuery.value}"`;
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

const hasMoreSongs = computed(() => searchSongs.value.length < searchSongsTotal.value);
const hasMoreArtists = computed(() => searchArtists.value.length < searchArtistsTotal.value);
</script>

<template>
  <div class="mx-auto max-w-[1240px] space-y-12 px-4 py-6 sm:px-7 sm:py-8">

    <!-- ── Hero: clean editorial ─────────────────────────────── -->
    <section class="relative min-h-[260px] overflow-hidden rounded-xl border border-white/[0.05] sm:min-h-[300px]">
      <!-- Background image -->
      <div
        class="absolute inset-0 bg-cover bg-center opacity-35"
        :style="{ backgroundImage: `url(${playlists[0].cover})` }"
      />

      <!-- Gradient overlays -->
      <div class="absolute inset-0 bg-gradient-to-r from-[#0E1218] via-[#0E1218]/70 to-transparent" />
      <div class="absolute inset-0 bg-gradient-to-t from-[#0E1218] via-transparent to-transparent" />

      <!-- Content: left-aligned editorial -->
      <div class="relative flex min-h-[260px] flex-col justify-end px-7 pb-8 sm:min-h-[300px] sm:px-9 sm:pb-10">
        <div class="max-w-lg">
          <p class="melodyhub-kicker">MelodyHub</p>
          <h1 class="mt-3 text-2xl font-bold leading-tight text-[#EDE9E0] sm:text-4xl">
            {{ sectionTitle }}
          </h1>
          <p class="mt-3 max-w-md text-sm leading-relaxed text-[#5A6860]">
            A curated listening space. Discover indie artists, build your library, and tune in.
          </p>
          <div class="mt-6 flex flex-wrap items-center gap-3">
            <button
              class="inline-flex h-10 items-center gap-2.5 rounded-lg bg-[#3DDE7C] px-5 text-xs font-bold text-[#0B0D0F] transition-all duration-200 active:scale-95 hover:brightness-105"
              @click="player.playTrack(tracks[0])"
            >
              <Play :size="15" class="fill-current" /> Play Mix
            </button>
            <span class="font-mono text-[11px] text-[#3A4A3E]">42 tracks &middot; updated today</span>
          </div>
        </div>
      </div>

      <!-- Featured mood: right panel -->
      <div class="absolute bottom-8 right-6 hidden w-36 border-l border-white/[0.07] pl-5 sm:bottom-10 sm:right-9 sm:block">
        <p class="text-[9px] font-bold uppercase tracking-[0.2em] text-[#3A4A3E]">Featured mood</p>
        <p class="mt-2 text-base font-bold text-[#EDE9E0]">Night Drive</p>
        <p class="mt-1 text-xs leading-relaxed text-[#4E5A52]">Synthwave for city lights.</p>
      </div>
    </section>

    <!-- ── Search results ──────────────────────────────────────────── -->
    <template v-if="isSearchMode">
      <section>
        <div class="mb-6 flex items-center gap-3">
          <div class="flex size-9 items-center justify-center rounded-lg border border-white/[0.06] bg-white/[0.03]">
            <Search :size="16" class="text-[#3DDE7C]" />
          </div>
          <div>
            <p class="text-xs font-bold uppercase tracking-wide text-[#3A4A3E]">Search results</p>
            <p class="text-sm font-semibold text-[#EDE9E0]">"<span class="text-[#3DDE7C]">{{ searchQuery }}</span>"</p>
          </div>
        </div>

        <!-- Error -->
        <div v-if="searchError" class="rounded-xl border border-red-500/20 bg-red-500/[0.06] px-4 py-4 text-sm text-red-300">
          {{ searchError }}
          <button class="ml-3 text-xs font-bold text-[#3DDE7C] hover:underline" @click="doSearch">Retry</button>
        </div>

        <!-- Loading -->
        <div v-else-if="searchLoading && searchSongs.length === 0" class="space-y-6">
          <div>
            <p class="mb-2 text-[10px] font-bold uppercase tracking-widest text-[#3A4A3E]">Songs</p>
            <div class="grid gap-2 sm:grid-cols-2">
              <div v-for="n in 4" :key="n" class="flex items-center gap-3 rounded-lg p-2">
                <span class="size-12 shrink-0 animate-pulse rounded-lg bg-white/5" />
                <span class="h-4 w-36 animate-pulse rounded bg-white/5" />
              </div>
            </div>
          </div>
          <div>
            <p class="mb-3 text-[10px] font-bold uppercase tracking-widest text-[#3A4A3E]">Artists</p>
            <div class="flex gap-4">
              <div v-for="n in 4" :key="n" class="w-32 shrink-0 text-center">
                <span class="mx-auto block aspect-square animate-pulse rounded-full bg-white/5" />
                <span class="mx-auto mt-3 block h-4 w-20 animate-pulse rounded bg-white/5" />
              </div>
            </div>
          </div>
        </div>

        <!-- Results -->
        <div v-else>
          <!-- Songs -->
          <div v-if="searchSongs.length > 0">
            <p class="mb-2 text-[10px] font-bold uppercase tracking-widest text-[#3A4A3E]">Songs <span class="text-[#2A3830]">({{ searchSongsTotal }})</span></p>
            <div class="grid gap-1 sm:grid-cols-2">
              <div
                v-for="song in searchSongs"
                :key="song.id"
                class="group flex min-w-0 items-center gap-3 rounded-lg p-2 transition hover:bg-white/[0.03]"
              >
                <button :title="`Open ${song.title}`" @click="$router.push({ name: 'song-detail', params: { slug: song.slug } })">
                  <img
                    v-if="song.coverUrl"
                    :src="song.coverUrl"
                    :alt="`${song.title} cover`"
                    class="size-12 rounded-lg object-cover ring-1 ring-white/[0.06]"
                  />
                  <span v-else class="grid size-12 place-items-center rounded-lg bg-white/[0.04] text-[#3A4A3E]">
                    <Music2 :size="18" />
                  </span>
                </button>
                <div class="min-w-0 flex-1">
                  <RouterLink
                    :to="{ name: 'song-detail', params: { slug: song.slug } }"
                    class="block truncate text-sm font-medium text-[#EDE9E0] transition group-hover:text-[#3DDE7C]"
                  >{{ song.title }}</RouterLink>
                  <p class="truncate text-xs text-[#3A4A3E]">{{ song.artists?.map((a) => a.name).join(', ') || 'Unknown artist' }}</p>
                </div>
                <button
                  class="melodyhub-icon-btn !size-8 shrink-0 opacity-0 transition group-hover:opacity-100"
                  @click="playSearchSong(song, searchSongs)"
                >
                  <Play :size="14" class="fill-current" />
                </button>
                <AddToPlaylistButton :song-id="song.id" hide-until-hover size="sm" />
              </div>
            </div>
            <button
              v-if="hasMoreSongs"
              class="mt-3 text-xs font-medium text-[#3DDE7C] hover:underline disabled:opacity-40"
              :disabled="searchLoading"
              @click="loadMoreSongs"
            >
              {{ searchLoading ? 'Loading…' : `Show ${Math.min(10, searchSongsTotal - searchSongs.length)} more` }}
            </button>
          </div>

          <!-- Artists -->
          <div v-if="searchArtists.length > 0" :class="{ 'mt-6': searchSongs.length > 0 }">
            <p class="mb-3 text-[10px] font-bold uppercase tracking-widest text-[#3A4A3E]">Artists <span class="text-[#2A3830]">({{ searchArtistsTotal }})</span></p>
            <div class="flex flex-wrap gap-5">
              <RouterLink
                v-for="artist in searchArtists"
                :key="artist.id"
                :to="{ name: 'artist-detail', params: { slug: artist.slug } }"
                class="group w-28 text-center sm:w-32"
              >
                <span class="relative mx-auto block aspect-square overflow-hidden rounded-full bg-[#1A2030] ring-1 ring-white/[0.06] transition-all duration-300 group-hover:ring-[#3DDE7C]/60">
                  <img v-if="artist.imageUrl" :src="artist.imageUrl" :alt="artist.name" class="h-full w-full object-cover transition duration-500 group-hover:scale-105" />
                  <span v-else class="grid h-full w-full place-items-center text-[#3A4A3E]"><CheckCircle2 :size="22" /></span>
                </span>
                <span class="mt-3 block truncate text-sm font-medium text-[#EDE9E0] transition group-hover:text-[#3DDE7C]">{{ artist.name }}</span>
                <span class="mt-0.5 block truncate text-xs text-[#3A4A3E]">Artist</span>
              </RouterLink>
            </div>
            <button
              v-if="hasMoreArtists"
              class="mt-3 text-xs font-medium text-[#3DDE7C] hover:underline disabled:opacity-40"
              :disabled="searchLoading"
              @click="loadMoreArtists"
            >
              {{ searchLoading ? 'Loading…' : 'Show more' }}
            </button>
          </div>

          <!-- Empty -->
          <div
            v-if="!searchLoading && searchSongs.length === 0 && searchArtists.length === 0"
            class="rounded-xl border border-white/[0.05] bg-[#131820] px-6 py-14 text-center"
          >
            <Search :size="32" class="mx-auto mb-4 text-[#2A3830]" />
            <p class="text-sm font-medium text-[#5A6860]">No results for "<span class="text-[#EDE9E0]">{{ searchQuery }}</span>"</p>
            <p class="mt-2 text-xs text-[#3A4A3E]">Try different keywords or check your spelling.</p>
          </div>
        </div>
      </section>
    </template>

    <!-- ── Default home content ──────────────────────────────────────── -->
    <template v-else>

    <!-- Top artists -->
    <section>
      <div class="mb-4 flex items-end justify-between">
        <div><p class="melodyhub-kicker">Artists</p><h2 class="melodyhub-section-title">Top artists</h2></div>
        <div class="flex gap-1">
          <button class="melodyhub-icon-btn" @click="scrollArtists(-1)"><ChevronLeft :size="17" /></button>
          <button class="melodyhub-icon-btn" @click="scrollArtists(1)"><ChevronRight :size="17" /></button>
        </div>
      </div>
      <div v-if="topArtistsLoading" class="flex gap-5 overflow-hidden">
        <div v-for="n in 6" :key="n" class="w-28 shrink-0 text-center sm:w-32">
          <span class="mx-auto block aspect-square animate-pulse rounded-full bg-white/5" />
          <span class="mx-auto mt-3 block h-4 w-20 animate-pulse rounded bg-white/5" />
        </div>
      </div>
      <div v-else-if="topArtists.length === 0" class="rounded-xl border border-white/[0.05] bg-[#131820] px-4 py-8 text-center text-sm text-[#3A4A3E]">
        No artists yet.
      </div>
      <div v-else ref="artistScroller" class="no-scrollbar flex snap-x gap-5 overflow-x-auto pb-1">
        <RouterLink
          v-for="artist in topArtists"
          :key="artist.id"
          :to="{ name: 'artist-detail', params: { slug: artist.slug } }"
          class="group w-28 shrink-0 snap-start text-center sm:w-32"
        >
          <span class="relative mx-auto block aspect-square overflow-hidden rounded-full bg-[#1A2030] ring-1 ring-white/[0.06] transition-all duration-300 group-hover:ring-[#3DDE7C]/60">
            <img v-if="artist.imageUrl" :src="artist.imageUrl" :alt="artist.name" class="h-full w-full object-cover transition duration-500 group-hover:scale-105" />
            <span v-else class="grid h-full w-full place-items-center text-[#3A4A3E]"><CheckCircle2 :size="22" /></span>
          </span>
          <span class="mt-3 block truncate text-sm font-medium text-[#EDE9E0] transition group-hover:text-[#3DDE7C]">{{ artist.name }}</span>
          <span class="mt-0.5 block truncate text-xs text-[#3A4A3E]">Artist</span>
        </RouterLink>
      </div>
    </section>

    <!-- New releases -->
    <section>
      <div class="mb-4"><p class="melodyhub-kicker">Just Landed</p><h2 class="melodyhub-section-title">New releases</h2></div>
      <div v-if="newReleasesLoading" class="grid gap-1 sm:grid-cols-2">
        <div v-for="n in 4" :key="n" class="flex items-center gap-3 rounded-lg p-2">
          <span class="size-12 shrink-0 animate-pulse rounded-lg bg-white/5" />
          <span class="h-4 w-36 animate-pulse rounded bg-white/5" />
        </div>
      </div>
      <div v-else-if="newReleases.length === 0" class="rounded-xl border border-white/[0.05] bg-[#131820] px-4 py-8 text-center text-sm text-[#3A4A3E]">
        No songs have been published yet.
      </div>
      <div v-else class="grid gap-1 sm:grid-cols-2">
        <div
          v-for="song in newReleases"
          :key="song.id"
          class="group flex min-w-0 items-center gap-3 rounded-lg p-2 transition hover:bg-white/[0.03]"
        >
          <button :title="`Open ${song.title}`" @click="$router.push({ name: 'song-detail', params: { slug: song.slug } })">
            <img
              v-if="song.coverUrl"
              :src="song.coverUrl"
              :alt="`${song.title} cover`"
              class="size-12 rounded-lg object-cover ring-1 ring-white/[0.06]"
            />
            <span v-else class="grid size-12 place-items-center rounded-lg bg-white/[0.04] text-[#3A4A3E]">
              <Music2 :size="18" />
            </span>
          </button>
          <RouterLink
            :to="{ name: 'song-detail', params: { slug: song.slug } }"
            class="min-w-0 flex-1"
          >
            <span class="block truncate text-sm font-medium text-[#EDE9E0] transition group-hover:text-[#3DDE7C]">{{ song.title }}</span>
            <span class="mt-0.5 block truncate text-xs text-[#3A4A3E]">{{ formatReleaseDate(song.createdAt) }}</span>
          </RouterLink>
          <button class="melodyhub-icon-btn !size-8 shrink-0" @click="playNewRelease(song)">
            <Play :size="14" class="fill-current" />
          </button>
          <AddToPlaylistButton :song-id="song.id" hide-until-hover size="sm" />
        </div>
      </div>
    </section>

    <!-- Podcasts -->
    <section class="pb-8">
      <div class="mb-4 flex items-end justify-between">
        <div><p class="melodyhub-kicker">Listen Deeper</p><h2 class="melodyhub-section-title">Podcasts</h2></div>
        <RouterLink :to="{ name: 'podcasts' }" class="text-xs font-medium text-[#3A4A3E] hover:text-[#EDE9E0]">Browse all</RouterLink>
      </div>
      <div class="grid gap-3 sm:grid-cols-3">
        <article
          v-for="podcast in podcasts"
          :key="podcast.id"
          class="group flex min-w-0 gap-3 rounded-xl border border-white/[0.05] bg-[#131820] p-3.5 transition-all duration-200 hover:-translate-y-0.5 hover:border-white/[0.08] hover:bg-[#1A2030]"
        >
          <img :src="podcast.cover" :alt="podcast.title" class="size-18 shrink-0 rounded-lg object-cover" />
          <div class="min-w-0">
            <p class="line-clamp-2 text-sm font-medium text-[#EDE9E0]">{{ podcast.title }}</p>
            <p class="mt-1 truncate text-xs text-[#3A4A3E]">{{ podcast.host }}</p>
            <p class="mt-3 font-mono text-[10px] font-medium text-[#3DDE7C]">{{ podcast.length }}</p>
          </div>
        </article>
      </div>
    </section>
    </template>
  </div>
</template>
