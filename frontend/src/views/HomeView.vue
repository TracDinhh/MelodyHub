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
    audioUrl: song.audioUrl
  };
}

function playNewRelease(song) {
  const list = newReleases.value.map(toPlayerTrack);
  player.playTrack(toPlayerTrack(song), list);
}

function playSearchSong(song, list) {
  player.playTrack(toPlayerTrack(song), list);
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
  <div class="mx-auto max-w-[1260px] space-y-11 px-4 py-6 sm:px-7 sm:py-8">
    <section class="relative min-h-[300px] overflow-hidden rounded-xl border border-white/[0.07] bg-[#111719] px-6 py-8 sm:min-h-[340px] sm:px-9 sm:py-10">
      <div
        class="absolute inset-0 bg-cover bg-center opacity-50"
        :style="{ backgroundImage: `url(${playlists[0].cover})` }"
      />
      <div class="melodyhub-hero-shine absolute inset-0" />
      <div class="relative flex min-h-[236px] max-w-xl flex-col justify-end sm:min-h-[260px]">
        <p class="melodyhub-kicker">YOUR DAILY MIX</p>
        <h1 class="mt-3 text-3xl font-black leading-tight text-white sm:text-5xl">{{ sectionTitle }}</h1>
        <p class="mt-3 max-w-lg text-sm leading-6 text-[#c3cac4]">
          A fresh mix of artists you love and the sounds shaping this week.
        </p>
        <div class="mt-6 flex flex-wrap items-center gap-4">
          <button class="inline-flex h-11 items-center gap-2 rounded-full bg-[#65e78c] px-5 text-xs font-black text-[#071108] shadow-[0_8px_22px_rgba(29,185,84,0.26)] transition hover:scale-[1.03]" @click="player.playTrack(tracks[0])">
          <Play :size="16" class="fill-current" /> PLAY MIX
          </button>
          <span class="text-xs font-semibold text-white/65">42 tracks &middot; updated today</span>
        </div>
      </div>
      <div class="absolute right-6 bottom-6 hidden w-40 border-l border-white/20 pl-4 sm:block">
        <p class="text-[10px] font-black tracking-[0.15em] text-[#8be8a8]">FEATURED MOOD</p>
        <p class="mt-1 text-sm font-bold text-white">Night Drive</p>
        <p class="mt-1 text-xs leading-5 text-white/65">Synthwave for city lights.</p>
      </div>
    </section>

    <!-- ── Search results (replaces playlists + top artists + new releases when active) ── -->
    <template v-if="isSearchMode">
      <section>
        <div class="mb-6 flex items-center justify-between">
          <div class="flex items-center gap-2">
            <Search :size="20" class="text-[#65e78c]" />
            <h2 class="text-xl font-black text-white">Search results</h2>
            <span class="text-sm text-[#777]">for "<span class="text-white">{{ searchQuery }}</span>"</span>
          </div>
          <button
            class="flex items-center gap-1.5 rounded-full border border-white/10 px-4 py-2 text-xs font-bold text-[#aaa] transition hover:border-white/25 hover:text-white"
            @click="$router.push({ name: 'home' })"
          >
            <X :size="14" /> Clear
          </button>
        </div>

        <!-- Error state -->
        <div v-if="searchError" class="rounded-lg border border-red-500/30 bg-red-500/10 px-4 py-5 text-center text-sm text-red-200">
          {{ searchError }}
          <button class="ml-3 text-xs font-bold text-[#65e78c] hover:underline" @click="doSearch">Retry</button>
        </div>

        <!-- Loading skeleton (first load) -->
        <div v-else-if="searchLoading && searchSongs.length === 0" class="space-y-6">
          <div>
            <p class="mb-2 text-[10px] font-black uppercase tracking-wider text-[#777]">Songs</p>
            <div class="grid gap-2 sm:grid-cols-2">
              <div v-for="n in 4" :key="n" class="flex items-center gap-3 rounded-lg p-2">
                <span class="size-13 shrink-0 animate-pulse rounded-md bg-white/5" />
                <span class="h-4 w-40 animate-pulse rounded bg-white/5" />
              </div>
            </div>
          </div>
          <div>
            <p class="mb-3 text-[10px] font-black uppercase tracking-wider text-[#777]">Artists</p>
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
            <p class="mb-2 text-[10px] font-black uppercase tracking-wider text-[#777]">
              Songs <span class="text-[#555]">({{ searchSongsTotal }})</span>
            </p>
            <div class="grid gap-2 sm:grid-cols-2">
              <div
                v-for="song in searchSongs"
                :key="song.id"
                class="group flex min-w-0 items-center gap-3 rounded-lg border border-transparent p-2 transition hover:border-white/[0.06] hover:bg-white/[0.045]"
              >
                <button
                  class="relative shrink-0"
                  :title="`Open ${song.title}`"
                  @click="$router.push({ name: 'song-detail', params: { slug: song.slug } })"
                >
                  <img
                    v-if="song.coverUrl"
                    :src="song.coverUrl"
                    :alt="`${song.title} cover`"
                    class="size-13 rounded-lg object-cover ring-1 ring-white/[0.08]"
                  />
                  <span v-else class="grid size-13 place-items-center rounded-md bg-white/[0.06] text-[#555]">
                    <Music2 :size="20" />
                  </span>
                </button>
                <div class="min-w-0 flex-1">
                  <RouterLink
                    :to="{ name: 'song-detail', params: { slug: song.slug } }"
                    class="block truncate text-sm font-bold text-white transition group-hover:text-[#8be8a8]"
                  >
                    {{ song.title }}
                  </RouterLink>
                  <p class="mt-1 truncate text-xs text-[#87918a]">
                    {{ song.artists?.map((a) => a.name).join(', ') || 'Unknown artist' }}
                  </p>
                </div>
                <button
                  class="melodyhub-icon-btn !size-9 shrink-0 opacity-0 transition group-hover:opacity-100"
                  title="Play song"
                  @click="playSearchSong(song, searchSongs)"
                >
                  <Play :size="15" class="fill-current" />
                </button>
                <AddToPlaylistButton :song-id="song.id" hide-until-hover size="sm" />
              </div>
            </div>
            <button
              v-if="hasMoreSongs"
              class="mt-3 text-xs font-bold text-[#65e78c] hover:underline disabled:opacity-50"
              :disabled="searchLoading"
              @click="loadMoreSongs"
            >
              {{ searchLoading ? 'Loading…' : `Show ${Math.min(10, searchSongsTotal - searchSongs.length)} more songs` }}
            </button>
          </div>

          <!-- Artists -->
          <div v-if="searchArtists.length > 0" :class="{ 'mt-6': searchSongs.length > 0 }">
            <p class="mb-3 text-[10px] font-black uppercase tracking-wider text-[#777]">
              Artists <span class="text-[#555]">({{ searchArtistsTotal }})</span>
            </p>
            <div class="flex flex-wrap gap-4">
              <RouterLink
                v-for="artist in searchArtists"
                :key="artist.id"
                :to="{ name: 'artist-detail', params: { slug: artist.slug } }"
                class="group w-32 text-center sm:w-40"
              >
                <span class="relative mx-auto block aspect-square overflow-hidden rounded-full bg-[#181818] p-1 ring-1 ring-white/10 transition group-hover:ring-[#65e78c]/70">
                  <img
                    v-if="artist.imageUrl"
                    :src="artist.imageUrl"
                    :alt="artist.name"
                    class="h-full w-full object-cover transition duration-500 group-hover:scale-105"
                  />
                  <span v-else class="grid h-full w-full place-items-center text-[#555]"><CheckCircle2 :size="26" /></span>
                </span>
                <span class="mt-3 block truncate text-sm font-bold text-white transition group-hover:text-[#8be8a8]">{{ artist.name }}</span>
                <span class="mt-1 block truncate text-xs text-[#87918a]">Artist</span>
              </RouterLink>
            </div>
            <button
              v-if="hasMoreArtists"
              class="mt-3 text-xs font-bold text-[#65e78c] hover:underline disabled:opacity-50"
              :disabled="searchLoading"
              @click="loadMoreArtists"
            >
              {{ searchLoading ? 'Loading…' : 'Show more artists' }}
            </button>
          </div>

          <!-- Empty state -->
          <div
            v-if="!searchLoading && searchSongs.length === 0 && searchArtists.length === 0"
            class="rounded-lg border border-white/10 bg-white/[0.02] px-6 py-16 text-center"
          >
            <Search :size="36" class="mx-auto mb-4 text-[#444]" />
            <p class="text-sm font-bold text-white">No results for "{{ searchQuery }}"</p>
            <p class="mt-2 text-xs text-[#777]">Try different keywords or check your spelling.</p>
          </div>
        </div>
      </section>
    </template>

    <!-- ── Default home content (hidden when searching) ── -->
    <template v-else>

    <section>
      <div class="mb-4 flex items-end justify-between">
        <div><p class="melodyhub-kicker">ON REPEAT</p><h2 class="melodyhub-section-title">Top artists</h2></div>
        <div class="flex gap-1">
          <button class="melodyhub-icon-btn" title="Previous artists" @click="scrollArtists(-1)"><ChevronLeft :size="18" /></button>
          <button class="melodyhub-icon-btn" title="Next artists" @click="scrollArtists(1)"><ChevronRight :size="18" /></button>
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
          <span class="relative mx-auto block aspect-square overflow-hidden rounded-full bg-[#181818] p-1 ring-1 ring-white/10 transition group-hover:ring-[#65e78c]/70">
            <img v-if="artist.imageUrl" :src="artist.imageUrl" :alt="artist.name" class="h-full w-full object-cover transition duration-500 group-hover:scale-105" />
            <span v-else class="grid h-full w-full place-items-center text-[#555]"><CheckCircle2 :size="26" /></span>
          </span>
          <span class="mt-3 block truncate text-sm font-bold text-white transition group-hover:text-[#8be8a8]">{{ artist.name }}</span>
          <span class="mt-1 block truncate text-xs text-[#87918a]">Artist</span>
        </RouterLink>
      </div>
    </section>

    <section>
      <div class="mb-4"><p class="melodyhub-kicker">JUST LANDED</p><h2 class="melodyhub-section-title">New releases</h2></div>

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
        <div
          v-for="song in newReleases"
          :key="song.id"
          class="group flex min-w-0 items-center gap-3 rounded-lg border border-transparent p-2 transition hover:border-white/[0.06] hover:bg-white/[0.045]"
        >
          <button
            class="relative shrink-0"
            :title="`Open ${song.title}`"
            @click="$router.push({ name: 'song-detail', params: { slug: song.slug } })"
          >
            <img
              v-if="song.coverUrl"
              :src="song.coverUrl"
              :alt="`${song.title} cover`"
              class="size-13 rounded-lg object-cover ring-1 ring-white/[0.08]"
            />
            <span v-else class="grid size-13 place-items-center rounded-md bg-white/[0.06] text-[#555]">
              <Music2 :size="20" />
            </span>
          </button>
          <RouterLink
            :to="{ name: 'song-detail', params: { slug: song.slug } }"
            class="min-w-0 flex-1"
          >
            <span class="block truncate text-sm font-bold text-white transition group-hover:text-[#8be8a8]">{{ song.title }}</span>
            <span class="mt-1 block truncate text-xs text-[#87918a]">/songs/{{ song.slug }}</span>
          </RouterLink>
          <button
            class="melodyhub-icon-btn !size-9 shrink-0"
            :title="`Play ${song.title}`"
            @click="playNewRelease(song)"
          >
            <Play :size="15" class="fill-current" />
          </button>
          <AddToPlaylistButton
            :song-id="song.id"
            hide-until-hover
            size="sm"
          />
          <span class="text-[10px] font-bold text-[#666]">{{ formatReleaseDate(song.createdAt) }}</span>
        </div>
      </div>
    </section>

    <section class="pb-8">
      <div class="mb-4 flex items-end justify-between">
        <div><p class="melodyhub-kicker">LISTEN DEEPER</p><h2 class="melodyhub-section-title">Podcasts for you</h2></div>
        <RouterLink :to="{ name: 'podcasts' }" class="text-xs font-bold text-[#8b8b8b] hover:text-white">Browse podcasts</RouterLink>
      </div>
      <div class="grid gap-3 sm:grid-cols-3">
        <article v-for="podcast in podcasts" :key="podcast.id" class="flex min-w-0 gap-3 rounded-lg border border-white/[0.055] bg-[#151a1b] p-3 transition hover:-translate-y-0.5 hover:border-white/10 hover:bg-[#192021]">
          <img :src="podcast.cover" :alt="podcast.title" class="size-20 rounded-md object-cover ring-1 ring-white/[0.08]" />
          <div class="min-w-0">
            <p class="line-clamp-2 text-sm font-bold text-white">{{ podcast.title }}</p>
            <p class="mt-1 truncate text-xs text-[#8b948e]">{{ podcast.host }}</p>
            <p class="mt-3 text-[10px] font-bold text-[#65e78c]">{{ podcast.length }}</p>
          </div>
        </article>
      </div>
    </section>
    </template>
  </div>
</template>
