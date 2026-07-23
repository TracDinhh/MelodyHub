<script setup>
import { computed, onMounted, ref } from 'vue';
import {
  ChevronLeft,
  ChevronRight,
  Clock3,
  Disc3,
  Eye,
  Library,
  Music2,
  RefreshCw,
  Search
} from '@lucide/vue';
import BaseButton from '../components/ui/BaseButton.vue';
import BaseModal from '../components/ui/BaseModal.vue';
import { useAuth } from '../composables/useAuth';
import { useSongStore } from '../stores/song.store';
import { formatDate, formatDuration } from '../utils/formatDate';

const songStore = useSongStore();
const { isArtist, displayName } = useAuth();
const search = ref('');
const selectedSong = ref(null);

const visibleSongs = computed(() => {
  if (!isArtist.value || !search.value.trim()) return songStore.songs;
  const term = search.value.trim().toLowerCase();
  return songStore.songs.filter((song) => song.title.toLowerCase().includes(term));
});
const publishedCount = computed(
  () => songStore.songs.filter((song) => song.status === 'PUBLISHED').length
);
const draftCount = computed(
  () => songStore.songs.filter((song) => song.status === 'DRAFT').length
);
const pageRange = computed(() => {
  if (!songStore.total) return '0';
  const start = (songStore.page - 1) * songStore.size + 1;
  const end = Math.min(start + songStore.songs.length - 1, songStore.total);
  return `${start}-${end}`;
});

function submitSearch() {
  if (isArtist.value) return;
  songStore.loadPage(1, search.value);
}

function statusClass(status) {
  return {
    PUBLISHED: 'status-badge--published',
    DRAFT: 'status-badge--draft',
    HIDDEN: 'status-badge--hidden'
  }[status] || 'status-badge--hidden';
}

onMounted(() => songStore.loadPage());
</script>

<template>
  <div class="page">
    <header class="page-header">
      <div>
        <p class="eyebrow">{{ isArtist ? 'Artist workspace' : 'Music library' }}</p>
        <h1>{{ isArtist ? 'Your songs' : 'Song catalog' }}</h1>
        <p>{{ displayName }}, here is the latest from your library.</p>
      </div>
      <button
        type="button"
        class="icon-button icon-button--bordered"
        title="Refresh songs"
        :disabled="songStore.isLoading"
        @click="songStore.loadPage(songStore.page, search)"
      >
        <RefreshCw :size="19" :class="{ spin: songStore.isLoading }" aria-hidden="true" />
        <span class="visually-hidden">Refresh songs</span>
      </button>
    </header>

    <section class="metrics" aria-label="Song metrics">
      <article class="metric">
        <span class="metric__icon metric__icon--coral"><Library :size="20" /></span>
        <div><span>Total songs</span><strong>{{ songStore.total }}</strong></div>
      </article>
      <article class="metric">
        <span class="metric__icon metric__icon--teal"><Disc3 :size="20" /></span>
        <div><span>Published here</span><strong>{{ publishedCount }}</strong></div>
      </article>
      <article class="metric">
        <span class="metric__icon metric__icon--yellow"><Clock3 :size="20" /></span>
        <div><span>Drafts here</span><strong>{{ draftCount }}</strong></div>
      </article>
    </section>

    <section class="catalog" aria-labelledby="catalog-title">
      <header class="catalog__header">
        <div>
          <h2 id="catalog-title">Songs</h2>
          <p>{{ isArtist ? 'Private catalog' : 'Published catalog' }}</p>
        </div>
        <form class="search-control" role="search" @submit.prevent="submitSearch">
          <Search :size="18" aria-hidden="true" />
          <input
            v-model="search"
            type="search"
            placeholder="Search songs"
            aria-label="Search songs"
          />
          <button v-if="!isArtist" type="submit">Search</button>
        </form>
      </header>

      <div v-if="songStore.error" class="state-panel state-panel--error" role="alert">
        <strong>Could not load songs</strong>
        <span>{{ songStore.error }}</span>
        <BaseButton variant="secondary" @click="songStore.loadPage()">Try again</BaseButton>
      </div>

      <div v-else-if="songStore.isLoading" class="song-skeletons" aria-label="Loading songs">
        <div v-for="index in 5" :key="index" class="song-skeleton">
          <span></span><span></span><span></span>
        </div>
      </div>

      <div v-else-if="visibleSongs.length === 0" class="state-panel">
        <span class="state-panel__icon"><Music2 :size="28" /></span>
        <strong>No songs found</strong>
        <span>Try another search or refresh the catalog.</span>
      </div>

      <div v-else class="table-responsive">
        <table class="song-table">
          <thead>
            <tr>
              <th>Song</th>
              <th>Status</th>
              <th>Duration</th>
              <th>Added</th>
              <th><span class="visually-hidden">Actions</span></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="song in visibleSongs" :key="song.id">
              <td>
                <div class="song-cell">
                  <img v-if="song.coverUrl" :src="song.coverUrl" :alt="`${song.title} cover`" />
                  <span v-else class="song-cell__fallback"><Music2 :size="20" /></span>
                  <div><strong>{{ song.title }}</strong><span>{{ song.slug }}</span></div>
                </div>
              </td>
              <td>
                <span class="status-badge" :class="statusClass(song.status)">
                  {{ song.status }}
                </span>
              </td>
              <td>{{ formatDuration(song.durationSec) }}</td>
              <td>{{ formatDate(song.createdAt) }}</td>
              <td class="song-table__action">
                <button
                  type="button"
                  class="icon-button"
                  :title="`View ${song.title}`"
                  @click="selectedSong = song"
                >
                  <Eye :size="18" aria-hidden="true" />
                  <span class="visually-hidden">View {{ song.title }}</span>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <footer v-if="!songStore.isLoading && !songStore.error" class="catalog__footer">
        <span>Showing {{ pageRange }} of {{ songStore.total }}</span>
        <div class="pagination-controls">
          <button
            type="button"
            class="icon-button icon-button--bordered"
            title="Previous page"
            :disabled="songStore.page <= 1"
            @click="songStore.loadPage(songStore.page - 1, search)"
          >
            <ChevronLeft :size="18" />
            <span class="visually-hidden">Previous page</span>
          </button>
          <span>Page {{ songStore.page }} of {{ songStore.totalPages }}</span>
          <button
            type="button"
            class="icon-button icon-button--bordered"
            title="Next page"
            :disabled="songStore.page >= songStore.totalPages"
            @click="songStore.loadPage(songStore.page + 1, search)"
          >
            <ChevronRight :size="18" />
            <span class="visually-hidden">Next page</span>
          </button>
        </div>
      </footer>
    </section>

    <BaseModal
      :open="Boolean(selectedSong)"
      :title="selectedSong?.title || 'Song details'"
      @close="selectedSong = null"
    >
      <dl v-if="selectedSong" class="detail-list">
        <div><dt>Status</dt><dd>{{ selectedSong.status }}</dd></div>
        <div><dt>Slug</dt><dd>{{ selectedSong.slug }}</dd></div>
        <div><dt>Duration</dt><dd>{{ formatDuration(selectedSong.durationSec) }}</dd></div>
        <div><dt>Play count</dt><dd>{{ selectedSong.playCount }}</dd></div>
        <div><dt>Created</dt><dd>{{ formatDate(selectedSong.createdAt) }}</dd></div>
      </dl>
    </BaseModal>
  </div>
</template>
