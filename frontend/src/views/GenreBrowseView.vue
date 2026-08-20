<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ChevronLeft, ChevronRight, LoaderCircle, Music2, Play } from '@lucide/vue';
import { genreService } from '../services/genreService';
import { usePlayerStore } from '../stores/player.store';
import { toPlayerTrack } from '../utils/playerTrack';

const route = useRoute();
const router = useRouter();
const player = usePlayerStore();

const genre = ref(null);
const songs = ref([]);
const total = ref(0);
const page = ref(1);
const size = ref(20);
const isLoading = ref(true);
const notFound = ref(false);
const error = ref('');

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)));

function toTrack(song) {
  const artist = (song.artists || []).map((a) => a.name).join(' & ');
  return toPlayerTrack(song, { artist });
}

function playSong(song) {
  const track = toTrack(song);
  player.playTrack(track, songs.value.map(toTrack));
}

function goPage(p) {
  if (p < 1 || p > totalPages.value || p === page.value) return;
  page.value = p;
  load();
}

async function load() {
  isLoading.value = true;
  notFound.value = false;
  error.value = '';
  try {
    const slug = route.params.slug;
    const [catalog, paged] = await Promise.all([
      genreService.listGenres().catch(() => ({ items: [] })),
      genreService.getGenreSongs(slug, { page: page.value, size: size.value })
    ]);
    genre.value = (catalog?.items || []).find((g) => g.slug === slug) || { name: slug, slug };
    songs.value = paged?.items || [];
    total.value = paged?.total || 0;
  } catch (requestError) {
    if (requestError.status === 404) notFound.value = true;
    else error.value = requestError.message || 'Unable to load this genre.';
  } finally {
    isLoading.value = false;
  }
}

onMounted(() => load());
watch(() => route.params.slug, () => {
  page.value = 1;
  load();
});
</script>

<template>
  <div class="mx-auto w-full max-w-6xl px-5 py-10 pb-14 sm:px-8">
    <div v-if="isLoading" class="flex min-h-[40vh] items-center justify-center text-sm text-[#888]">
      <LoaderCircle :size="22" class="mr-3 animate-spin text-[#16C65A]" /> Loading genre
    </div>

    <div v-else-if="notFound" class="flex min-h-[40vh] flex-col items-center justify-center gap-3 text-center">
      <p class="text-lg font-black text-white">Genre not found</p>
      <RouterLink :to="{ name: 'home' }" class="text-xs font-bold text-[#16C65A]">Back to Home</RouterLink>
    </div>

    <template v-else>
      <div class="mb-8">
        <p class="melodyhub-kicker">GENRE</p>
        <h1 class="melodyhub-section-title text-4xl">{{ genre.name }}</h1>
        <p class="mt-2 text-sm text-[#999]">{{ total }} {{ total === 1 ? 'song' : 'songs' }}</p>
      </div>

      <p v-if="error" class="mb-4 rounded-md bg-red-500/10 px-3 py-2 text-xs text-red-300" role="alert">{{ error }}</p>

      <div v-if="songs.length === 0 && !error" class="flex min-h-48 flex-col items-center justify-center gap-2 border border-white/10 bg-[#111827] text-sm text-[#888]">
        <Music2 :size="28" class="text-[#555]" />
        <p>No songs in this genre yet.</p>
      </div>

      <div v-else class="grid gap-1 sm:grid-cols-2">
        <div
          v-for="song in songs"
          :key="song.id"
          class="group flex min-w-0 items-center gap-3 rounded-lg p-2 transition hover:bg-white/[0.03]"
        >
          <button :title="`Open ${song.title}`" @click="router.push({ name: 'song-detail', params: { slug: song.slug } })">
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
            @click="playSong(song)"
          >
            <Play :size="14" class="fill-current" />
          </button>
        </div>
      </div>

      <div v-if="totalPages > 1" class="mt-6 flex items-center justify-center gap-3">
        <button
          class="grid size-8 place-items-center rounded-md border border-white/10 text-[#bbb] transition hover:border-white/30 disabled:opacity-30"
          :disabled="page === 1"
          @click="goPage(page - 1)"
        ><ChevronLeft :size="15" /></button>
        <span class="text-xs font-bold text-[#8EA696]">Page {{ page }} of {{ totalPages }}</span>
        <button
          class="grid size-8 place-items-center rounded-md border border-white/10 text-[#bbb] transition hover:border-white/30 disabled:opacity-30"
          :disabled="page === totalPages"
          @click="goPage(page + 1)"
        ><ChevronRight :size="15" /></button>
      </div>
    </template>
  </div>
</template>
