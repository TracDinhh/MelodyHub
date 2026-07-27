<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { LoaderCircle, Play } from '@lucide/vue';
import TrackRow from '../components/music/TrackRow.vue';
import { artistBrowseService } from '../services/artistBrowseService';
import { usePlayerStore } from '../stores/player.store';

const route = useRoute();
const player = usePlayerStore();

const artist = ref(null);
const songs = ref([]);
const isLoading = ref(true);
const notFound = ref(false);

const playerTracks = computed(() =>
  songs.value.map((song) => ({
    id: song.id,
    title: song.title,
    artist: artist.value?.name || '',
    cover: song.coverUrl,
    plays: (song.playCount ?? 0).toLocaleString(),
    duration: song.durationSec || 0,
    audioUrl: song.audioUrl
  }))
);

async function load(slug) {
  isLoading.value = true;
  notFound.value = false;
  artist.value = null;
  songs.value = [];
  try {
    artist.value = await artistBrowseService.getBySlug(slug);
    const paged = await artistBrowseService.getSongs(slug, { page: 1, size: 50 });
    songs.value = paged?.items || [];
  } catch (error) {
    if (error.status === 404) notFound.value = true;
  } finally {
    isLoading.value = false;
  }
}

function playAll() {
  if (playerTracks.value.length) {
    player.playTrack(playerTracks.value[0], playerTracks.value);
  }
}

onMounted(() => load(route.params.slug));
watch(() => route.params.slug, (slug) => slug && load(slug));
</script>

<template>
  <div class="pb-10">
    <div v-if="isLoading" class="flex min-h-[60vh] items-center justify-center text-sm text-[#888]">
      <LoaderCircle :size="22" class="mr-3 animate-spin text-[#1DB954]" /> Loading artist
    </div>

    <div v-else-if="notFound || !artist" class="flex min-h-[60vh] flex-col items-center justify-center gap-3 text-center">
      <p class="text-lg font-black text-white">Artist not found</p>
      <RouterLink :to="{ name: 'home' }" class="text-xs font-bold text-[#1DB954]">Back to Home</RouterLink>
    </div>

    <template v-else>
      <section class="relative min-h-[300px] overflow-hidden bg-cover bg-center" :style="artist.imageUrl ? { backgroundImage: `url(${artist.imageUrl})` } : {}">
        <div class="absolute inset-0 bg-gradient-to-t from-[#0d0d0d] via-black/50 to-black/30" />
        <div class="relative flex min-h-[300px] items-end px-5 pb-7 sm:px-8">
          <div class="w-full">
            <p class="melodyhub-kicker">ARTIST</p>
            <h1 class="mt-2 text-5xl font-black text-white sm:text-7xl">{{ artist.name }}</h1>
            <p class="mt-3 text-sm text-white/70">{{ songs.length }} song{{ songs.length === 1 ? '' : 's' }}</p>
            <div class="mt-5 flex flex-wrap items-center gap-3">
              <button
                class="inline-flex h-11 items-center gap-2 rounded-full bg-[#1DB954] px-6 text-xs font-black text-black transition hover:scale-[1.03] disabled:opacity-50"
                :disabled="!songs.length"
                @click="playAll"
              >
                <Play :size="17" class="fill-current" /> PLAY ALL
              </button>
            </div>
          </div>
        </div>
      </section>

      <div class="px-4 py-7 sm:px-7">
        <section>
          <div class="mb-3 flex items-end justify-between px-3">
            <div><p class="melodyhub-kicker">ESSENTIALS</p><h2 class="melodyhub-section-title">Popular</h2></div>
            <p class="text-[10px] font-bold text-[#666]">PLAYS</p>
          </div>

          <div v-if="!songs.length" class="px-3 py-8 text-sm text-[#777]">
            This artist hasn't published any songs yet.
          </div>
          <div v-else class="overflow-x-auto">
            <TrackRow v-for="(track, index) in playerTracks" :key="track.id" :track="track" :index="index" />
          </div>
        </section>

        <section v-if="artist.bio" class="mt-10 grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
          <img v-if="artist.imageUrl" :src="artist.imageUrl" :alt="artist.name" class="max-h-[420px] w-full rounded-lg object-cover" />
          <div>
            <p class="melodyhub-kicker">ABOUT</p>
            <h2 class="melodyhub-section-title">{{ artist.name }}</h2>
            <p class="mt-5 whitespace-pre-line text-sm leading-7 text-[#999]">{{ artist.bio }}</p>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>
