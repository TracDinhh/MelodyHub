<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  ArrowLeft,
  Heart,
  Lock,
  LoaderCircle,
  Music2,
  Pause,
  Play,
  Sparkles
} from '@lucide/vue';
import { songService } from '../services/songService';
import { usePlayerStore } from '../stores/player.store';
import { useAuthStore } from '../stores/auth.store';
import AddToPlaylistButton from '../components/music/AddToPlaylistButton.vue';
import LyricCardModal from '../components/lyrics/LyricCardModal.vue';
import { useLyricSelection } from '../composables/useLyricSelection';
import { formatDuration } from '../utils/formatDate';
import { toPlayerTrack } from '../utils/playerTrack';

const route = useRoute();
const router = useRouter();
const player = usePlayerStore();
const authStore = useAuthStore();

const song = ref(null);
const related = ref([]);
const lyricLines = ref([]);
const isLoading = ref(true);
const notFound = ref(false);

const artistLabel = computed(() => {
  if (!song.value?.artists?.length) return 'Unknown artist';
  return song.value.artists.map((artist) => artist.name).join(' & ');
});

const artistList = computed(() => song.value?.artists ?? []);

const relatedTrackList = computed(() => related.value.map(toTrack));

async function load(slug) {
  isLoading.value = true;
  notFound.value = false;
  song.value = null;
  related.value = [];
  lyricLines.value = [];
  try {
    const [detail, relatedResponse] = await Promise.all([
      songService.getPublic(slug),
      songService.getRelated(slug, { size: 8 }).catch(() => ({ items: [] }))
    ]);
    song.value = detail;
    related.value = relatedResponse?.items || [];

    // Seed the store's liked set from the detail payload so the heart reflects
    // the server's isLiked even before/without a full hydrate call.
    if (detail?.id != null && typeof detail.isLiked === 'boolean') {
      player.setLiked(detail.id, detail.isLiked);
    }

    if (detail.lyricsType === 'SYNCED' && authStore.isPremium) {
      const lyricsResponse = await songService.getSyncedLyrics(detail.slug).catch(() => null);
      if (song.value?.id === detail.id && lyricsResponse?.lyricsType === 'SYNCED') {
        lyricLines.value = lyricsResponse.lines || [];
      }
    }
  } catch (error) {
    if (error.status === 404) notFound.value = true;
  } finally {
    isLoading.value = false;
  }
}

function playSong() {
  if (!song.value) return;
  const main = toTrack(song.value);
  const queue = [main, ...relatedTrackList.value.filter((track) => track.id !== main.id)];
  player.playTrack(main, queue);
}

function playRelated(track) {
  const queue = [toTrack(song.value), ...relatedTrackList.value.filter((t) => t.id !== track.id)];
  player.playTrack(track, queue);
}

function toTrack(detail) {
  const artist = (detail.artists || []).map((a) => a.name).join(' & ');
  return toPlayerTrack(detail, { artist });
}

const isPlaying = computed(() => player.isPlaying && player.currentTrack.id === song.value?.id);
const lyricsLocked = computed(() => song.value?.lyricsType === 'SYNCED' && !authStore.isPremium);

function openPremium() {
  router.push({ name: 'premium' });
}

// Heart state comes from the player store's liked set (persisted to the
// backend), falling back to the detail payload's isLiked on first load.
const isLiked = computed(() =>
  song.value?.id ? player.likedIds.has(song.value.id) : false
);

function toggleLike() {
  if (song.value?.id) player.toggleLike(song.value.id);
}

// Lyric-card feature: pick 1..4 contiguous lines, then open the card modal.
const lyricSelection = useLyricSelection();
const cardOpen = ref(false);
const cardLines = computed(() =>
  lyricSelection.sortedIndices.value
    .map((index) => lyricLines.value[index]?.text)
    .filter(Boolean)
);

function openLyricCard() {
  if (!lyricSelection.hasSelection.value) return;
  cardOpen.value = true;
}

function closeLyricCard() {
  cardOpen.value = false;
}

// Auto-scroll the active lyric line to the middle of the lyrics box while playing.
const lyricsBox = ref(null);
watch(
  () => player.currentLyricLine,
  (index) => {
    if (index == null || !lyricsBox.value) return;
    if (player.currentTrack.id !== song.value?.id) return;
    nextTick(() => {
      const el = lyricsBox.value?.querySelector(`[data-line="${index}"]`);
      if (el) {
        lyricsBox.value.scrollTop =
          el.offsetTop - lyricsBox.value.clientHeight / 2 + el.clientHeight / 2;
      }
    });
  }
);

onMounted(() => load(route.params.slug));
watch(() => route.params.slug, (slug) => slug && load(slug));
</script>

<template>
  <div class="pb-10">
    <div v-if="isLoading" class="flex min-h-[60vh] items-center justify-center text-sm text-[#888]">
      <LoaderCircle :size="22" class="mr-3 animate-spin text-[#16C65A]" /> Loading song
    </div>

    <div v-else-if="notFound || !song" class="flex min-h-[60vh] flex-col items-center justify-center gap-3 text-center">
      <p class="text-lg font-black text-white">Song not found</p>
      <RouterLink :to="{ name: 'home' }" class="text-xs font-bold text-[#16C65A]">Back to Home</RouterLink>
    </div>

    <template v-else>
      <RouterLink :to="{ name: 'home' }" class="melodyhub-icon-btn ml-4 mt-4 inline-flex" title="Back">
        <ArrowLeft :size="18" />
      </RouterLink>

      <section
        class="relative mx-4 mt-3 min-h-[280px] overflow-hidden rounded-xl bg-cover bg-center sm:mx-7"
        :style="song.coverUrl ? { backgroundImage: `url(${song.coverUrl})` } : {}"
      >
        <div class="absolute inset-0 bg-gradient-to-t from-[#0d0d0d] via-black/60 to-black/30" />
        <div class="relative flex min-h-[280px] flex-col gap-5 px-5 py-7 sm:flex-row sm:items-end sm:gap-7 sm:px-8">
          <img
            v-if="song.coverUrl"
            :src="song.coverUrl"
            :alt="`${song.title} cover`"
            class="size-48 shrink-0 rounded-lg object-cover shadow-2xl shadow-black/60 ring-1 ring-white/10 sm:size-56"
          />
          <span
            v-else
            class="grid size-48 shrink-0 place-items-center rounded-lg bg-[#181818] text-[#444] sm:size-56"
          >
            <Music2 :size="56" />
          </span>

          <div class="min-w-0 flex-1">
            <p class="melodyhub-kicker">SONG</p>
            <h1 class="mt-2 text-4xl font-black text-white sm:text-6xl">{{ song.title }}</h1>
            <div class="mt-3 flex flex-wrap items-center gap-x-2 gap-y-1 text-sm text-white/75">
              <span
                v-for="artist in artistList"
                :key="artist.id"
              >
                <RouterLink :to="{ name: 'artist-detail', params: { slug: artist.slug } }" class="font-bold text-white hover:text-[#FDA4AF]">
                  {{ artist.name }}
                </RouterLink>
              </span>
              <span v-if="song.album" class="text-white/40">·</span>
              <RouterLink
                v-if="song.album"
                :to="{ name: 'home' }"
                class="text-white/75 hover:text-white"
              >
                {{ song.album.title }}
              </RouterLink>
              <span class="text-white/40">·</span>
              <span>{{ (song.playCount ?? 0).toLocaleString() }} plays</span>
              <span class="text-white/40">·</span>
              <span>{{ formatDuration(song.durationSec) }}</span>
            </div>

            <div v-if="song.genres?.length" class="mt-4 flex flex-wrap gap-2">
              <RouterLink
                v-for="genre in song.genres"
                :key="genre.id"
                :to="{ name: 'genre-browse', params: { slug: genre.slug } }"
                class="rounded-full border border-white/15 bg-white/5 px-3 py-1 text-[11px] font-bold text-white/80 transition hover:border-[#16C65A]/60 hover:text-[#16C65A]"
              >{{ genre.name }}</RouterLink>
            </div>

            <div class="mt-5 flex flex-wrap items-center gap-3">
              <button
                class="inline-flex h-11 items-center gap-2 rounded-full bg-[#16C65A] px-6 text-xs font-black text-black transition hover:scale-[1.03] disabled:opacity-50"
                :disabled="!song.audioUrl"
                @click="playSong"
              >
                <Pause v-if="isPlaying" :size="17" class="fill-current" />
                <Play v-else :size="17" class="fill-current" />
                {{ isPlaying ? 'PAUSE' : 'PLAY' }}
              </button>
              <button
                class="melodyhub-icon-btn !size-11"
                :title="isLiked ? 'Remove from your Liked Songs' : 'Save to your Liked Songs'"
                @click="toggleLike"
              >
                <Heart :size="18" :class="isLiked ? 'fill-[#16C65A] text-[#16C65A]' : ''" />
              </button>
              <AddToPlaylistButton :song-id="song.id" />
            </div>
          </div>
        </div>
      </section>

      <div class="px-4 py-7 sm:px-7">
        <!-- Synced Lyrics Display -->
        <section
          v-if="song.lyricsType === 'SYNCED' && lyricLines.length > 0"
          class="mb-6 rounded-lg border border-white/[0.06] bg-[#111] p-5"
        >
          <div class="mb-3 flex items-end justify-between">
            <div>
              <p class="melodyhub-kicker">LYRICS</p>
              <h2 class="melodyhub-section-title">Synced Lyrics</h2>
            </div>
            <p class="text-[10px] font-bold text-[#16C65A]">● SYNCED</p>
          </div>
          <p class="mb-2 text-[11px] text-[#666]">Tap up to {{ lyricSelection.MAX_LINES }} lines to turn them into a shareable card.</p>
          <div ref="lyricsBox" class="max-h-[340px] space-y-1 overflow-y-auto scroll-smooth text-center">
            <button
              v-for="(line, index) in lyricLines"
              :key="index"
              :data-line="index"
              type="button"
              class="block w-full rounded-md px-2 py-1.5 text-base transition-all duration-300 sm:text-xl"
              :class="{
                'bg-[#20E878]/15 ring-1 ring-inset ring-[#20E878]/40': lyricSelection.isSelected(index),
                'scale-105 font-bold text-[#16C65A]': player.currentLyricLine === index && player.currentTrack.id === song.id,
                'text-[#555]': player.currentTrack.id === song.id && player.currentLyricLine !== null && index < player.currentLyricLine,
                'text-[#888]': !(player.currentLyricLine === index && player.currentTrack.id === song.id) && !(player.currentTrack.id === song.id && player.currentLyricLine !== null && index < player.currentLyricLine)
              }"
              @click="lyricSelection.toggle(index)"
            >
              {{ line.text }}
            </button>
          </div>
          <div v-if="lyricSelection.hasSelection.value" class="mt-4 flex items-center justify-center gap-2">
            <button
              class="inline-flex h-10 items-center gap-2 rounded-full bg-[#20E878] px-5 text-xs font-black text-[#09090B] transition hover:bg-[#64F4A1]"
              @click="openLyricCard"
            >
              <Sparkles :size="15" /> Create lyric card ({{ lyricSelection.sortedIndices.value.length }})
            </button>
            <button
              class="inline-flex h-10 items-center rounded-full border border-white/10 px-4 text-xs font-bold text-[#A1A1AA] transition hover:border-white/25"
              @click="lyricSelection.clear()"
            >
              Clear
            </button>
          </div>
        </section>

        <section v-else-if="lyricsLocked" class="mb-6 rounded-lg border border-[#20E878]/20 bg-[#20E878]/[0.05] p-5 text-center">
          <Lock :size="24" class="mx-auto text-[#20E878]" />
          <h2 class="mt-3 text-lg font-bold text-white">Synced lyrics are a Premium feature</h2>
          <p class="mt-2 text-sm text-[#A1A1AA]">Upgrade to follow every line in time with the music.</p>
          <button class="mt-4 inline-flex h-9 items-center rounded-full bg-[#20E878] px-4 text-xs font-bold text-[#09090B]" @click="openPremium">Unlock synced lyrics</button>
        </section>

        <!-- Plain Lyrics Display (never used for SYNCED songs, so JSON never leaks) -->
        <section v-else-if="song.lyricsType !== 'SYNCED' && song.lyrics" class="mb-6 rounded-lg border border-white/[0.06] bg-[#111] p-5">
          <div class="mb-3 flex items-end justify-between">
            <div>
              <p class="melodyhub-kicker">LYRICS</p>
              <h2 class="melodyhub-section-title">Words</h2>
            </div>
            <p v-if="song.lyricsType !== 'SYNCED'" class="text-[10px] font-bold text-[#666]">PLAIN</p>
          </div>
          <p class="whitespace-pre-line text-sm leading-7 text-[#bbb]">{{ song.lyrics }}</p>
        </section>

        <section v-if="related.length" class="mt-8">
          <div class="mb-3 flex items-end justify-between px-1">
            <div>
              <p class="melodyhub-kicker">MORE TO PLAY</p>
              <h2 class="melodyhub-section-title">Related songs</h2>
            </div>
          </div>
          <div class="grid grid-cols-2 gap-x-4 gap-y-5 sm:grid-cols-4">
            <button
              v-for="track in relatedTrackList"
              :key="track.id"
              class="group min-w-0 text-left"
              @click="playRelated(track)"
            >
              <span class="relative block aspect-square overflow-hidden rounded-lg bg-[#181818] ring-1 ring-white/[0.07]">
                <img
                  v-if="track.cover"
                  :src="track.cover"
                  :alt="`${track.title} cover`"
                  class="h-full w-full object-cover transition duration-500 group-hover:scale-105"
                />
                <span v-else class="grid h-full w-full place-items-center text-[#555]">
                  <Music2 :size="32" />
                </span>
                <span class="absolute inset-0 grid place-items-center bg-black/40 opacity-0 transition group-hover:opacity-100">
                  <Play :size="22" class="ml-0.5 fill-white text-white" />
                </span>
              </span>
              <span class="mt-3 block truncate text-sm font-bold text-white transition group-hover:text-[#FDA4AF]">{{ track.title }}</span>
              <span class="mt-1 block truncate text-xs text-[#8EA696]">{{ track.artist }}</span>
            </button>
          </div>
        </section>
      </div>

    </template>

    <LyricCardModal
      :open="cardOpen"
      :lines="cardLines"
      :title="song?.title || ''"
      :artist="artistLabel"
      :cover-url="song?.coverUrl || ''"
      @close="closeLyricCard"
    />
  </div>
</template>
