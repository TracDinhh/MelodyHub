<script setup>
import { computed, onMounted, ref } from 'vue';
import { Heart, Music2, Play, Shuffle } from '@lucide/vue';
import { likeService } from '../services/likeService';
import { usePlayerStore } from '../stores/player.store';
import { toPlayerTrack } from '../utils/playerTrack';

const player = usePlayerStore();

const items = ref([]);
const total = ref(0);
const page = ref(1);
const size = ref(30);
const loading = ref(true);
const error = ref(null);
const busy = ref(new Set());
const hasMore = ref(false);

function toTrack(entry) {
  return toPlayerTrack(entry?.song, { artist: artistNames(entry) });
}

function artistNames(entry) {
  return (entry.artists || []).map((artist) => artist.name).filter(Boolean).join(', ');
}

function formatDuration(seconds) {
  const total = Math.max(0, Math.floor(seconds || 0));
  const mins = Math.floor(total / 60);
  const secs = total % 60;
  return `${mins}:${secs.toString().padStart(2, '0')}`;
}

const playableTracks = computed(() => items.value.map(toTrack).filter((track) => track.id));

async function load() {
  loading.value = true;
  error.value = null;
  try {
    const response = await likeService.list({ page: page.value, size: size.value });
    items.value = response?.items || [];
    total.value = response?.total || 0;
    hasMore.value = page.value * size.value < total.value;
  } catch (caught) {
    error.value = caught?.message || 'Could not load your liked songs.';
    items.value = [];
  } finally {
    loading.value = false;
  }
}

function play(entry) {
  player.playTrack(toTrack(entry), playableTracks.value);
}

function playAll() {
  const tracks = playableTracks.value;
  if (tracks.length) player.playTrack(tracks[0], tracks);
}

function shufflePlay() {
  const tracks = [...playableTracks.value];
  if (!tracks.length) return;
  for (let i = tracks.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [tracks[i], tracks[j]] = [tracks[j], tracks[i]];
  }
  player.playTrack(tracks[0], tracks);
}

async function unlike(entry) {
  const id = entry?.song?.id;
  if (!id || busy.value.has(id)) return;
  busy.value = new Set([...busy.value, id]);
  try {
    await likeService.unlike(id);
    player.setLiked(id, false);
    items.value = items.value.filter((row) => row.song?.id !== id);
    total.value = Math.max(0, total.value - 1);
  } finally {
    const next = new Set(busy.value);
    next.delete(id);
    busy.value = next;
  }
}

async function loadMore() {
  if (!hasMore.value || loading.value) return;
  page.value += 1;
  loading.value = true;
  try {
    const response = await likeService.list({ page: page.value, size: size.value });
    items.value = [...items.value, ...(response?.items || [])];
    total.value = response?.total || total.value;
    hasMore.value = page.value * size.value < total.value;
  } catch (caught) {
    page.value -= 1;
    error.value = caught?.message || 'Could not load more songs.';
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="mx-auto max-w-[1260px] space-y-7 px-4 py-8 sm:px-7">
    <header class="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div class="flex flex-col gap-2">
        <p class="melodyhub-kicker">YOUR LIBRARY</p>
        <div class="flex items-center gap-3">
          <span class="grid size-12 place-items-center rounded-xl bg-gradient-to-br from-[#20E878] to-[#0EA35C] text-black shadow-lg shadow-[#20E878]/20">
            <Heart :size="24" class="fill-current" />
          </span>
          <h1 class="font-display text-3xl font-bold text-white sm:text-4xl">Liked Songs</h1>
        </div>
        <p class="text-sm text-[#8EA696]">
          Every song you've hearted, in one place.
        </p>
      </div>

      <div v-if="items.length" class="flex items-center gap-2">
        <button
          class="inline-flex h-11 items-center gap-2 rounded-full bg-[#20E878] px-6 text-xs font-black text-[#0F0F12] transition hover:scale-[1.03]"
          @click="playAll"
        >
          <Play :size="16" class="fill-current" /> Play all
        </button>
        <button
          class="melodyhub-icon-btn !size-11 border border-white/10"
          title="Shuffle play"
          @click="shufflePlay"
        >
          <Shuffle :size="17" />
        </button>
      </div>
    </header>

    <div v-if="loading && items.length === 0" class="space-y-2">
      <div v-for="n in 8" :key="n" class="flex items-center gap-3 rounded-lg border border-white/[0.05] bg-white/[0.02] p-3">
        <span class="size-12 animate-pulse rounded-md bg-white/5" />
        <span class="h-4 w-48 animate-pulse rounded bg-white/5" />
      </div>
    </div>

    <div v-else-if="error" class="rounded-lg border border-red-500/30 bg-red-500/10 px-4 py-6 text-center text-sm text-red-200">
      {{ error }}
      <button class="ml-3 text-xs font-bold text-[#20E878] hover:underline" @click="load">Retry</button>
    </div>

    <div v-else-if="items.length === 0" class="rounded-lg border border-white/10 bg-white/[0.02] px-6 py-16 text-center">
      <Heart :size="42" class="mx-auto mb-4 text-[#444]" />
      <p class="text-sm font-bold text-white">No liked songs yet</p>
      <p class="mt-2 text-xs text-[#777]">Tap the heart on any song and it'll show up here.</p>
      <RouterLink :to="{ name: 'home' }" class="mt-5 inline-flex h-10 items-center gap-2 rounded-full bg-[#20E878] px-5 text-xs font-black text-[#0F0F12]">
        <Play :size="14" class="fill-current" /> Discover music
      </RouterLink>
    </div>

    <template v-else>
      <ul class="divide-y divide-white/[0.06] overflow-hidden rounded-lg border border-white/[0.06] bg-[#10151a]">
        <li
          v-for="(entry, index) in items"
          :key="entry.song?.id"
          class="group flex items-center gap-3 px-3 py-3 transition hover:bg-white/[0.04]"
        >
          <span class="hidden w-6 shrink-0 text-center text-xs font-bold text-[#555] sm:block">
            {{ index + 1 }}
          </span>

          <button
            class="relative shrink-0"
            :title="`Open ${entry.song?.title}`"
            @click="$router.push({ name: 'song-detail', params: { slug: entry.song?.slug } })"
          >
            <img
              v-if="entry.song?.coverUrl"
              :src="entry.song.coverUrl"
              :alt="`${entry.song.title} cover`"
              class="size-12 rounded-md object-cover ring-1 ring-white/[0.08]"
            />
            <span v-else class="grid size-12 place-items-center rounded-md bg-white/[0.06] text-[#555]">
              <Music2 :size="20" />
            </span>
          </button>

          <div class="min-w-0 flex-1">
            <RouterLink
              :to="{ name: 'song-detail', params: { slug: entry.song?.slug } }"
              class="block truncate text-sm font-bold text-white transition group-hover:text-[#20E878]"
            >
              {{ entry.song?.title }}
            </RouterLink>
            <p class="mt-1 truncate text-xs text-[#8EA696]">
              {{ artistNames(entry) || 'Unknown artist' }}
            </p>
          </div>

          <span class="hidden shrink-0 text-[11px] font-semibold text-[#777] sm:block">
            {{ formatDuration(entry.song?.durationSec) }}
          </span>

          <button
            class="melodyhub-icon-btn !size-9 shrink-0"
            title="Play song"
            @click="play(entry)"
          >
            <Play :size="15" class="fill-current" />
          </button>

          <button
            class="melodyhub-icon-btn !size-9 shrink-0 !text-[#20E878]"
            :disabled="busy.has(entry.song?.id)"
            :title="busy.has(entry.song?.id) ? 'Removing…' : 'Remove from Liked Songs'"
            @click="unlike(entry)"
          >
            <Heart :size="15" class="fill-current" />
          </button>
        </li>
      </ul>

      <div class="flex items-center justify-between pt-2 text-xs font-bold text-[#777]">
        <span>{{ total }} liked {{ total === 1 ? 'song' : 'songs' }}</span>
        <button
          v-if="hasMore"
          class="text-[#20E878] transition hover:underline disabled:opacity-50"
          :disabled="loading"
          @click="loadMore"
        >
          {{ loading ? 'Loading…' : 'Load more' }}
        </button>
      </div>
    </template>
  </div>
</template>
