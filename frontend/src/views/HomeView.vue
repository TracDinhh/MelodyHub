<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ArrowRight, CheckCircle2, ChevronLeft, ChevronRight, Music2, Play, Search, X } from '@lucide/vue';
import { playlists, podcasts, tracks } from '../data/music';
import AddToPlaylistButton from '../components/music/AddToPlaylistButton.vue';
import { artistBrowseService } from '../services/artistBrowseService';
import { genreService } from '../services/genreService';
import { songService } from '../services/songService';
import { useAuthStore } from '../stores/auth.store';
import { usePlayerStore } from '../stores/player.store';
import { toPlayerTrack } from '../utils/playerTrack';

const route = useRoute();
const authStore = useAuthStore();
const player = usePlayerStore();
const artistScroller = ref(null);
const genreScroller = ref(null);

// Newest published songs from the real API.
const newReleases = ref([]);
const newReleasesTotal = ref(0);
const newReleasesPage = ref(1);
const NEW_RELEASES_SIZE = 6;
const newReleasesLoading = ref(true);

// Published songs grouped by genre. Home shows a compact preview; the genre
// page remains the source for the complete paginated list.
const genreSections = ref([]);
const genreSectionsLoading = ref(true);
const selectedGenreSlug = ref('');
const GENRE_SECTION_SIZE = 6;

const visibleGenreSections = computed(() =>
  selectedGenreSlug.value
    ? genreSections.value.filter((section) => section.genre.slug === selectedGenreSlug.value)
    : genreSections.value
);

const newReleasesTotalPages = computed(() =>
  Math.max(1, Math.ceil(newReleasesTotal.value / NEW_RELEASES_SIZE))
);

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
    const response = await songService.listPublic({
      page: newReleasesPage.value,
      size: NEW_RELEASES_SIZE
    });
    newReleases.value = response?.items || [];
    newReleasesTotal.value = response?.total || 0;
  } catch {
    newReleases.value = [];
    newReleasesTotal.value = 0;
  } finally {
    newReleasesLoading.value = false;
  }
}

async function loadGenreSections() {
  genreSectionsLoading.value = true;
  try {
    const catalogResponse = await genreService.listGenres();
    const genres = Array.isArray(catalogResponse)
      ? catalogResponse
      : (catalogResponse?.items || []);

    const responses = await Promise.allSettled(
      genres.map(async (genre) => {
        const paged = await genreService.getGenreSongs(genre.slug, {
          page: 1,
          size: GENRE_SECTION_SIZE
        });
        return {
          genre,
          songs: paged?.items || [],
          total: paged?.total || 0
        };
      })
    );

    genreSections.value = responses
      .filter((response) => response.status === 'fulfilled' && response.value.songs.length > 0)
      .map((response) => response.value);
    selectedGenreSlug.value = '';
  } catch {
    genreSections.value = [];
    selectedGenreSlug.value = '';
  } finally {
    genreSectionsLoading.value = false;
  }
}

async function changeNewReleasesPage(nextPage) {
  if (
    nextPage < 1 ||
    nextPage > newReleasesTotalPages.value ||
    nextPage === newReleasesPage.value ||
    newReleasesLoading.value
  ) return;

  newReleasesPage.value = nextPage;
  await loadNewReleases();
}

function toTrack(song) {
  return toPlayerTrack(song, { artist: '' });
}

function playNewRelease(song) {
  const list = newReleases.value.map(toTrack);
  player.playTrack(toTrack(song), list);
}

function playGenreSong(song, songs) {
  player.playTrack(toTrack(song), songs.map(toTrack));
}

function selectGenre(slug) {
  selectedGenreSlug.value = slug;
}

function playSearchSong(song, list) {
  player.playTrack(toTrack(song), list);
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
  loadGenreSections();
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

function scrollGenres(direction) {
  genreScroller.value?.scrollBy({ left: direction * 280, behavior: 'smooth' });
}

const hasMoreSongs = computed(() => searchSongs.value.length < searchSongsTotal.value);
const hasMoreArtists = computed(() => searchArtists.value.length < searchArtistsTotal.value);
</script>

<template>
  <div class="mx-auto max-w-[1400px] space-y-12 px-4 py-6 sm:px-8 sm:py-9">

    <!-- Editorial feature: a clear primary action, like modern streaming homepages. -->
    <section class="relative isolate grid min-h-[340px] overflow-hidden rounded-3xl border border-white/[0.07] bg-[#121214] sm:min-h-[380px] lg:grid-cols-[minmax(0,1fr)_360px]">
      <div class="absolute inset-0 bg-[radial-gradient(circle_at_18%_0%,rgba(244,63,94,0.26),transparent_42%),radial-gradient(circle_at_86%_100%,rgba(127,29,29,0.24),transparent_46%),linear-gradient(120deg,#251116_0%,#121214_58%,#09090B_100%)]" />
      <div class="relative flex flex-col justify-end px-7 py-8 sm:px-10 sm:py-11">
        <p class="melodyhub-kicker">Made for your next listen</p>
        <h1 class="mt-3 max-w-2xl font-display text-4xl font-bold leading-[1.02] tracking-tight text-[#F4FFF7] sm:text-6xl">
          {{ sectionTitle }}
        </h1>
        <p class="mt-4 max-w-lg text-[0.95rem] leading-7 text-[#C4C4CC] sm:text-base">
          Find new sounds, revisit favourites, and keep every great track close to your library.
        </p>
        <div class="mt-7 flex flex-wrap items-center gap-3">
          <button
            class="inline-flex h-11 items-center gap-2.5 rounded-full bg-[#20E878] px-6 text-sm font-bold text-[#09090B] transition hover:scale-[1.03] hover:bg-[#64F4A1] active:scale-95"
            @click="player.playTrack(tracks[0])"
          >
            <Play :size="16" class="fill-current" /> Play now
          </button>
          <RouterLink :to="{ name: 'explore' }" class="inline-flex h-11 items-center gap-2 rounded-full border border-white/[0.12] px-5 text-sm font-semibold text-[#F4FFF7] transition hover:border-[#20E878]/60 hover:bg-white/[0.05]">
            Explore <ArrowRight :size="15" />
          </RouterLink>
        </div>
      </div>

      <div class="relative hidden items-center justify-center p-8 lg:flex">
        <div class="absolute inset-0 bg-gradient-to-l from-transparent via-[#09090B]/15 to-[#121214]" />
        <img :src="playlists[0].cover" alt="Featured MelodyHub playlist" class="relative aspect-square w-full max-w-[272px] rounded-2xl object-cover shadow-2xl shadow-black/50 ring-1 ring-white/[0.12]" />
        <div class="absolute bottom-10 left-8 rounded-full border border-white/[0.10] bg-[#09090B]/70 px-3 py-1.5 text-[10px] font-bold uppercase tracking-[0.16em] text-[#FDA4AF] backdrop-blur">
          42 tracks · updated today
        </div>
      </div>
    </section>

    <!-- ── Search results ──────────────────────────────────────────── -->
    <template v-if="isSearchMode">
      <section>
        <div class="mb-6 flex items-center gap-3">
          <div class="flex size-9 items-center justify-center rounded-lg border border-white/[0.06] bg-white/[0.03]">
            <Search :size="16" class="text-[#20E878]" />
          </div>
          <div>
            <p class="text-xs font-bold uppercase tracking-wide text-[#71717A]">Search results</p>
            <p class="text-sm font-semibold text-[#F4FFF7]">"<span class="text-[#20E878]">{{ searchQuery }}</span>"</p>
          </div>
        </div>

        <!-- Error -->
        <div v-if="searchError" class="rounded-xl border border-red-500/20 bg-red-500/[0.06] px-4 py-4 text-sm text-red-300">
          {{ searchError }}
          <button class="ml-3 text-xs font-bold text-[#20E878] hover:underline" @click="doSearch">Retry</button>
        </div>

        <!-- Loading -->
        <div v-else-if="searchLoading && searchSongs.length === 0" class="space-y-6">
          <div>
            <p class="mb-2 text-[10px] font-bold uppercase tracking-widest text-[#71717A]">Songs</p>
            <div class="grid gap-2 sm:grid-cols-2">
              <div v-for="n in 4" :key="n" class="flex items-center gap-3 rounded-lg p-2">
                <span class="size-12 shrink-0 animate-pulse rounded-lg bg-white/5" />
                <span class="h-4 w-36 animate-pulse rounded bg-white/5" />
              </div>
            </div>
          </div>
          <div>
            <p class="mb-3 text-[10px] font-bold uppercase tracking-widest text-[#71717A]">Artists</p>
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
            <p class="mb-2 text-[10px] font-bold uppercase tracking-widest text-[#71717A]">Songs <span class="text-[#27272A]">({{ searchSongsTotal }})</span></p>
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
                  <span v-else class="grid size-12 place-items-center rounded-lg bg-white/[0.04] text-[#71717A]">
                    <Music2 :size="18" />
                  </span>
                </button>
                <div class="min-w-0 flex-1">
                  <RouterLink
                    :to="{ name: 'song-detail', params: { slug: song.slug } }"
                    class="block truncate text-sm font-medium text-[#F4FFF7] transition group-hover:text-[#20E878]"
                  >{{ song.title }}</RouterLink>
                  <p class="truncate text-xs text-[#71717A]">{{ song.artists?.map((a) => a.name).join(', ') || 'Unknown artist' }}</p>
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
              class="mt-3 text-xs font-medium text-[#20E878] hover:underline disabled:opacity-40"
              :disabled="searchLoading"
              @click="loadMoreSongs"
            >
              {{ searchLoading ? 'Loading…' : `Show ${Math.min(10, searchSongsTotal - searchSongs.length)} more` }}
            </button>
          </div>

          <!-- Artists -->
          <div v-if="searchArtists.length > 0" :class="{ 'mt-6': searchSongs.length > 0 }">
            <p class="mb-3 text-[10px] font-bold uppercase tracking-widest text-[#71717A]">Artists <span class="text-[#27272A]">({{ searchArtistsTotal }})</span></p>
            <div class="flex flex-wrap gap-5">
              <RouterLink
                v-for="artist in searchArtists"
                :key="artist.id"
                :to="{ name: 'artist-detail', params: { slug: artist.slug } }"
                class="group w-28 text-center sm:w-32"
              >
                <span class="relative mx-auto block aspect-square overflow-hidden rounded-full bg-[#1B1B1F] ring-1 ring-white/[0.06] transition-all duration-300 group-hover:ring-[#20E878]/60">
                  <img v-if="artist.imageUrl" :src="artist.imageUrl" :alt="artist.name" class="h-full w-full object-cover transition duration-500 group-hover:scale-105" />
                  <span v-else class="grid h-full w-full place-items-center text-[#71717A]"><CheckCircle2 :size="22" /></span>
                </span>
                <span class="mt-3 block truncate text-sm font-medium text-[#F4FFF7] transition group-hover:text-[#20E878]">{{ artist.name }}</span>
                <span class="mt-0.5 block truncate text-xs text-[#71717A]">Artist</span>
              </RouterLink>
            </div>
            <button
              v-if="hasMoreArtists"
              class="mt-3 text-xs font-medium text-[#20E878] hover:underline disabled:opacity-40"
              :disabled="searchLoading"
              @click="loadMoreArtists"
            >
              {{ searchLoading ? 'Loading…' : 'Show more' }}
            </button>
          </div>

          <!-- Empty -->
          <div
            v-if="!searchLoading && searchSongs.length === 0 && searchArtists.length === 0"
            class="rounded-xl border border-white/[0.05] bg-[#121214] px-6 py-14 text-center"
          >
            <Search :size="32" class="mx-auto mb-4 text-[#27272A]" />
            <p class="text-sm font-medium text-[#A1A1AA]">No results for "<span class="text-[#F4FFF7]">{{ searchQuery }}</span>"</p>
            <p class="mt-2 text-xs text-[#71717A]">Try different keywords or check your spelling.</p>
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
      <div v-else-if="topArtists.length === 0" class="rounded-xl border border-white/[0.05] bg-[#121214] px-4 py-8 text-center text-sm text-[#71717A]">
        No artists yet.
      </div>
      <div v-else ref="artistScroller" class="no-scrollbar flex snap-x gap-5 overflow-x-auto pb-1">
        <RouterLink
          v-for="artist in topArtists"
          :key="artist.id"
          :to="{ name: 'artist-detail', params: { slug: artist.slug } }"
          class="group w-28 shrink-0 snap-start text-center sm:w-32"
        >
          <span class="relative mx-auto block aspect-square overflow-hidden rounded-full bg-[#1B1B1F] ring-1 ring-white/[0.06] transition-all duration-300 group-hover:ring-[#20E878]/60">
            <img v-if="artist.imageUrl" :src="artist.imageUrl" :alt="artist.name" class="h-full w-full object-cover transition duration-500 group-hover:scale-105" />
            <span v-else class="grid h-full w-full place-items-center text-[#71717A]"><CheckCircle2 :size="22" /></span>
          </span>
          <span class="mt-3 block truncate text-sm font-medium text-[#F4FFF7] transition group-hover:text-[#20E878]">{{ artist.name }}</span>
          <span class="mt-0.5 block truncate text-xs text-[#71717A]">Artist</span>
        </RouterLink>
      </div>
    </section>

    <!-- New releases -->
    <section>
      <div class="mb-4"><p class="melodyhub-kicker">Just Landed</p><h2 class="melodyhub-section-title">New releases</h2></div>
      <div v-if="newReleasesLoading" class="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
        <div v-for="n in 6" :key="n" class="flex items-center gap-3 rounded-lg p-2.5">
          <span class="size-14 shrink-0 animate-pulse rounded-lg bg-white/5" />
          <span class="h-4 w-36 animate-pulse rounded bg-white/5" />
        </div>
      </div>
      <div v-else-if="newReleases.length === 0" class="rounded-xl border border-white/[0.05] bg-[#121214] px-4 py-8 text-center text-sm text-[#71717A]">
        No songs have been published yet.
      </div>
      <div v-else class="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
        <div
          v-for="song in newReleases"
          :key="song.id"
          class="group flex min-w-0 items-center gap-3 rounded-xl border border-transparent p-2.5 transition hover:border-white/[0.06] hover:bg-white/[0.03]"
        >
          <button :title="`Open ${song.title}`" @click="$router.push({ name: 'song-detail', params: { slug: song.slug } })">
            <img
              v-if="song.coverUrl"
              :src="song.coverUrl"
              :alt="`${song.title} cover`"
              class="size-14 rounded-lg object-cover ring-1 ring-white/[0.06]"
            />
            <span v-else class="grid size-14 place-items-center rounded-lg bg-white/[0.04] text-[#71717A]">
              <Music2 :size="20" />
            </span>
          </button>
          <RouterLink
            :to="{ name: 'song-detail', params: { slug: song.slug } }"
            class="min-w-0 flex-1"
          >
            <span class="block truncate text-[0.95rem] font-semibold text-[#F4FFF7] transition group-hover:text-[#20E878]">{{ song.title }}</span>
            <span class="mt-0.5 block truncate text-xs text-[#71717A]">{{ formatReleaseDate(song.createdAt) }}</span>
          </RouterLink>
          <button class="melodyhub-icon-btn !size-8 shrink-0" @click="playNewRelease(song)">
            <Play :size="14" class="fill-current" />
          </button>
          <AddToPlaylistButton :song-id="song.id" hide-until-hover size="sm" />
        </div>
      </div>
      <div
        v-if="newReleasesTotalPages > 1"
        class="mt-4 flex items-center justify-center gap-3 text-xs font-bold text-[#8EA696]"
      >
        <button
          class="rounded-md border border-white/[0.08] px-3 py-2 transition hover:border-[#20E878]/50 hover:text-[#20E878] disabled:cursor-not-allowed disabled:opacity-40"
          :disabled="newReleasesPage === 1 || newReleasesLoading"
          @click="changeNewReleasesPage(newReleasesPage - 1)"
        >
          Previous
        </button>
        <span>Page {{ newReleasesPage }} of {{ newReleasesTotalPages }}</span>
        <button
          class="rounded-md border border-white/[0.08] px-3 py-2 transition hover:border-[#20E878]/50 hover:text-[#20E878] disabled:cursor-not-allowed disabled:opacity-40"
          :disabled="newReleasesPage === newReleasesTotalPages || newReleasesLoading"
          @click="changeNewReleasesPage(newReleasesPage + 1)"
        >
          Next
        </button>
      </div>
    </section>

    <!-- Songs grouped by genre -->
    <section>
      <div class="mb-5">
        <p class="melodyhub-kicker">Browse by sound</p>
        <h2 class="melodyhub-section-title">Music for every mood</h2>
      </div>

      <div v-if="genreSectionsLoading">
        <div class="no-scrollbar mb-5 flex gap-2 overflow-x-auto pb-2">
          <span v-for="item in 5" :key="item" class="h-9 w-24 shrink-0 animate-pulse rounded-full bg-white/5" />
        </div>
        <div class="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
          <div v-for="card in 6" :key="card" class="space-y-3">
            <div class="aspect-square animate-pulse rounded-2xl bg-white/5" />
            <div class="h-4 w-3/4 animate-pulse rounded bg-white/5" />
          </div>
        </div>
      </div>

      <div v-else-if="genreSections.length > 0">
        <div class="mb-6 flex items-center gap-2">
          <button
            type="button"
            class="melodyhub-icon-btn hidden shrink-0 sm:grid"
            aria-label="Show previous genres"
            @click="scrollGenres(-1)"
          >
            <ChevronLeft :size="18" />
          </button>
          <div ref="genreScroller" class="no-scrollbar flex min-w-0 flex-1 gap-2 overflow-x-auto scroll-smooth pb-1" aria-label="Filter songs by genre">
            <button
              type="button"
              :aria-pressed="!selectedGenreSlug"
              class="shrink-0 rounded-full border px-4 py-2 text-sm font-semibold transition"
              :class="!selectedGenreSlug
                ? 'border-[#20E878] bg-[#20E878] text-[#09090B]'
                : 'border-white/[0.10] bg-white/[0.04] text-[#C4C4CC] hover:border-[#20E878]/50 hover:text-[#F4FFF7]'"
              @click="selectGenre('')"
            >All</button>
            <button
              v-for="section in genreSections"
              :key="section.genre.id"
              type="button"
              :aria-pressed="selectedGenreSlug === section.genre.slug"
              class="shrink-0 rounded-full border px-4 py-2 text-sm font-semibold transition"
              :class="selectedGenreSlug === section.genre.slug
                ? 'border-[#20E878] bg-[#20E878] text-[#09090B]'
                : 'border-white/[0.10] bg-white/[0.04] text-[#C4C4CC] hover:border-[#20E878]/50 hover:text-[#F4FFF7]'"
              @click="selectGenre(section.genre.slug)"
            >{{ section.genre.name }}</button>
          </div>
          <button
            type="button"
            class="melodyhub-icon-btn hidden shrink-0 sm:grid"
            aria-label="Show more genres"
            @click="scrollGenres(1)"
          >
            <ChevronRight :size="18" />
          </button>
        </div>

        <article v-for="section in visibleGenreSections" :key="section.genre.id" class="mb-10 last:mb-0">
          <div class="mb-4 flex items-end justify-between gap-4">
            <div>
              <h3 class="text-xl font-bold tracking-tight text-[#F4FFF7]">{{ section.genre.name }}</h3>
              <p class="mt-1 text-xs text-[#71717A]">
                {{ section.total }} {{ section.total === 1 ? 'song' : 'songs' }}
              </p>
            </div>
            <RouterLink
              :to="{ name: 'genre-browse', params: { slug: section.genre.slug } }"
              class="inline-flex shrink-0 items-center gap-1.5 text-xs font-semibold text-[#20E878] transition hover:text-[#64F4A1]"
            >
              View all <ArrowRight :size="14" />
            </RouterLink>
          </div>

          <div class="no-scrollbar grid auto-cols-[minmax(158px,1fr)] grid-flow-col gap-4 overflow-x-auto pb-2 lg:grid-flow-row lg:grid-cols-6">
            <article
              v-for="song in section.songs"
              :key="song.id"
              class="group min-w-0 rounded-2xl border border-white/[0.05] bg-[#121214] p-3 transition duration-200 hover:-translate-y-1 hover:border-white/[0.10] hover:bg-[#18181B]"
            >
              <div class="relative aspect-square overflow-hidden rounded-xl bg-white/[0.04]">
                <RouterLink :to="{ name: 'song-detail', params: { slug: song.slug } }" class="block h-full">
                  <img
                    v-if="song.coverUrl"
                    :src="song.coverUrl"
                    :alt="`${song.title} cover`"
                    class="h-full w-full object-cover transition duration-500 group-hover:scale-105"
                  />
                  <span v-else class="grid h-full w-full place-items-center text-[#52525B]">
                    <Music2 :size="28" />
                  </span>
                </RouterLink>
                <button
                  :aria-label="`Play ${song.title}`"
                  class="absolute bottom-2.5 right-2.5 grid size-10 translate-y-2 place-items-center rounded-full bg-[#20E878] text-[#09090B] opacity-0 shadow-lg shadow-black/40 transition duration-200 hover:scale-105 group-hover:translate-y-0 group-hover:opacity-100 focus-visible:translate-y-0 focus-visible:opacity-100"
                  @click="playGenreSong(song, section.songs)"
                >
                  <Play :size="17" class="ml-0.5 fill-current" />
                </button>
              </div>

              <div class="mt-3 flex min-w-0 items-start gap-1">
                <RouterLink
                  :to="{ name: 'song-detail', params: { slug: song.slug } }"
                  class="min-w-0 flex-1"
                >
                  <span class="block truncate text-sm font-semibold text-[#F4FFF7] transition group-hover:text-[#20E878]">{{ song.title }}</span>
                  <span class="mt-1 block truncate text-[11px] text-[#71717A]">
                    {{ song.genres?.map((genre) => genre.name).join(' · ') || section.genre.name }}
                  </span>
                </RouterLink>
                <AddToPlaylistButton :song-id="song.id" hide-until-hover size="sm" />
              </div>
            </article>
          </div>
        </article>
      </div>
    </section>

    <!-- Podcasts -->
    <section class="pb-8">
      <div class="mb-4 flex items-end justify-between">
        <div><p class="melodyhub-kicker">Listen Deeper</p><h2 class="melodyhub-section-title">Podcasts</h2></div>
        <RouterLink :to="{ name: 'podcasts' }" class="text-xs font-medium text-[#71717A] hover:text-[#F4FFF7]">Browse all</RouterLink>
      </div>
      <div class="grid gap-3 sm:grid-cols-3">
        <article
          v-for="podcast in podcasts"
          :key="podcast.id"
          class="group flex min-w-0 gap-3 rounded-xl border border-white/[0.05] bg-[#121214] p-3.5 transition-all duration-200 hover:-translate-y-0.5 hover:border-white/[0.08] hover:bg-[#1B1B1F]"
        >
          <img :src="podcast.cover" :alt="podcast.title" class="size-18 shrink-0 rounded-lg object-cover" />
          <div class="min-w-0">
            <p class="line-clamp-2 text-sm font-medium text-[#F4FFF7]">{{ podcast.title }}</p>
            <p class="mt-1 truncate text-xs text-[#71717A]">{{ podcast.host }}</p>
            <p class="mt-3 font-mono text-[10px] font-medium text-[#20E878]">{{ podcast.length }}</p>
          </div>
        </article>
      </div>
    </section>
    </template>
  </div>
</template>
