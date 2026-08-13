<script setup>
import { onMounted, ref } from 'vue';
import { History, Music2, Play, Trash2 } from '@lucide/vue';
import { listenHistoryService } from '../services/listenHistoryService';
import { usePlayerStore } from '../stores/player.store';

const player = usePlayerStore();

const items = ref([]);
const total = ref(0);
const page = ref(1);
const size = ref(20);
const loading = ref(true);
const error = ref(null);
const busy = ref(new Set());

const hasMore = ref(false);

function toPlayerTrack(entry) {
  const song = entry?.song;
  return {
    id: song?.id,
    title: song?.title,
    cover: song?.coverUrl,
    artist: '',
    album: '',
    duration: song?.durationSec || 0,
    audioUrl: song?.audioUrl,
    lyricsType: song?.lyricsType || 'PLAIN',
    slug: song?.slug
  };
}

function artistNames(entry) {
  return (entry.artists || []).map((artist) => artist.name).filter(Boolean).join(', ');
}

function formatListenedAt(value) {
  if (!value) return '';
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' })
    .format(new Date(value));
}

async function load() {
  loading.value = true;
  error.value = null;
  try {
    const response = await listenHistoryService.list({ page: page.value, size: size.value });
    items.value = response?.items || [];
    total.value = response?.total || 0;
    hasMore.value = page.value * size.value < total.value;
  } catch (caught) {
    error.value = caught?.message || 'Could not load your listening history.';
    items.value = [];
  } finally {
    loading.value = false;
  }
}

function play(entry) {
  const tracks = items.value.map(toPlayerTrack).filter((track) => track.id);
  player.playTrack(toPlayerTrack(entry), tracks);
}

async function remove(entry) {
  const id = entry?.id;
  if (!id || busy.value.has(id)) return;
  busy.value = new Set([...busy.value, id]);
  try {
    await listenHistoryService.remove(id);
    items.value = items.value.filter((row) => row.id !== id);
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
    const response = await listenHistoryService.list({ page: page.value, size: size.value });
    const next = response?.items || [];
    items.value = [...items.value, ...next];
    total.value = response?.total || total.value;
    hasMore.value = page.value * size.value < total.value;
  } catch (caught) {
    page.value -= 1;
    error.value = caught?.message || 'Could not load more history.';
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="mx-auto max-w-[1260px] space-y-7 px-4 py-8 sm:px-7">
    <header class="flex flex-col gap-2">
      <p class="melodyhub-kicker">YOUR LIBRARY</p>
      <div class="flex items-center gap-3">
        <History :size="32" class="text-[#65e78c]" />
        <h1 class="text-3xl font-black text-white sm:text-4xl">Listen history</h1>
      </div>
      <p class="text-sm text-[#87918a]">
        Songs you've listened to are recorded here once you've played at least 30 seconds.
      </p>
    </header>

    <div v-if="loading && items.length === 0" class="space-y-2">
      <div v-for="n in 6" :key="n" class="flex items-center gap-3 rounded-lg border border-white/[0.05] bg-white/[0.02] p-3">
        <span class="size-13 animate-pulse rounded-md bg-white/5" />
        <span class="h-4 w-40 animate-pulse rounded bg-white/5" />
      </div>
    </div>

    <div v-else-if="error" class="rounded-lg border border-red-500/30 bg-red-500/10 px-4 py-6 text-center text-sm text-red-200">
      {{ error }}
      <button class="ml-3 text-xs font-bold text-[#65e78c] hover:underline" @click="load">Retry</button>
    </div>

    <div v-else-if="items.length === 0" class="rounded-lg border border-white/10 bg-white/[0.02] px-6 py-16 text-center">
      <Music2 :size="42" class="mx-auto mb-4 text-[#444]" />
      <p class="text-sm font-bold text-white">No listening history yet</p>
      <p class="mt-2 text-xs text-[#777]">Start playing a song and we'll track it here.</p>
      <RouterLink :to="{ name: 'home' }" class="mt-5 inline-flex h-10 items-center gap-2 rounded-full bg-[#65e78c] px-5 text-xs font-black text-[#071108]">
        <Play :size="14" class="fill-current" /> Discover music
      </RouterLink>
    </div>

    <template v-else>
      <ul class="divide-y divide-white/[0.06] overflow-hidden rounded-lg border border-white/[0.06] bg-[#10151a]">
        <li
          v-for="entry in items"
          :key="entry.id"
          class="group flex items-center gap-3 px-3 py-3 transition hover:bg-white/[0.04]"
        >
          <button
            class="relative shrink-0"
            :title="`Open ${entry.song?.title}`"
            @click="$router.push({ name: 'song-detail', params: { slug: entry.song?.slug } })"
          >
            <img
              v-if="entry.song?.coverUrl"
              :src="entry.song.coverUrl"
              :alt="`${entry.song.title} cover`"
              class="size-13 rounded-md object-cover ring-1 ring-white/[0.08]"
            />
            <span v-else class="grid size-13 place-items-center rounded-md bg-white/[0.06] text-[#555]">
              <Music2 :size="20" />
            </span>
          </button>

          <div class="min-w-0 flex-1">
            <RouterLink
              :to="{ name: 'song-detail', params: { slug: entry.song?.slug } }"
              class="block truncate text-sm font-bold text-white transition group-hover:text-[#8be8a8]"
            >
              {{ entry.song?.title }}
            </RouterLink>
            <p class="mt-1 truncate text-xs text-[#87918a]">
              {{ artistNames(entry) || 'Unknown artist' }}
            </p>
          </div>

          <div class="hidden text-right text-[11px] font-semibold text-[#777] sm:block">
            <p>{{ formatListenedAt(entry.listenedAt) }}</p>
            <p class="mt-1 text-[#666]">{{ entry.playedSec || 0 }}s listened</p>
          </div>

          <button
            class="melodyhub-icon-btn !size-9 shrink-0"
            title="Play song"
            @click="play(entry)"
          >
            <Play :size="15" class="fill-current" />
          </button>

          <button
            class="melodyhub-icon-btn !size-9 shrink-0 hover:!text-red-300"
            :disabled="busy.has(entry.id)"
            :title="busy.has(entry.id) ? 'Removing…' : 'Remove from history'"
            @click="remove(entry)"
          >
            <Trash2 :size="15" />
          </button>
        </li>
      </ul>

      <div class="flex items-center justify-between pt-2 text-xs font-bold text-[#777]">
        <span>{{ total }} {{ total === 1 ? 'song' : 'songs' }} in your history</span>
        <button
          v-if="hasMore"
          class="text-[#65e78c] transition hover:underline disabled:opacity-50"
          :disabled="loading"
          @click="loadMore"
        >
          {{ loading ? 'Loading…' : 'Load more' }}
        </button>
      </div>
    </template>
  </div>
</template>
