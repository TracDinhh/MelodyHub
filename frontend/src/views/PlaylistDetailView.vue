<script setup>
import { computed, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ListMusic, Music2, Play, Trash2 } from '@lucide/vue';
import { usePlaylistStore } from '../stores/playlist.store';
import { usePlayerStore } from '../stores/player.store';

const route = useRoute();
const store = usePlaylistStore();
const player = usePlayerStore();

const playlist = computed(() => store.current);
const songs = computed(() => store.current?.songs || []);

function artistNames(song) {
  return (song.artists || []).map((artist) => artist.name).filter(Boolean).join(', ');
}

function toPlayerTrack(song) {
  return {
    id: song?.id,
    title: song?.title,
    cover: song?.coverUrl,
    artist: artistNames(song),
    album: '',
    duration: song?.durationSec || 0,
    audioUrl: song?.audioUrl
  };
}

function play(song) {
  const tracks = songs.value.map(toPlayerTrack).filter((track) => track.id);
  player.playTrack(toPlayerTrack(song), tracks);
}

function playAll() {
  if (songs.value.length === 0) return;
  play(songs.value[0]);
}

async function removeSong(song) {
  await store.removeSong(playlist.value.id, song.id);
}

function load() {
  store.loadDetail(route.params.id);
}

onMounted(load);
watch(() => route.params.id, load);
</script>

<template>
  <div class="mx-auto max-w-[1260px] space-y-7 px-4 py-8 sm:px-7">
    <div v-if="store.isDetailLoading && !playlist" class="space-y-4">
      <div class="h-40 animate-pulse rounded-xl bg-white/5" />
      <div v-for="n in 5" :key="n" class="h-14 animate-pulse rounded-lg bg-white/5" />
    </div>

    <div v-else-if="store.detailError" class="rounded-lg border border-red-500/30 bg-red-500/10 px-4 py-6 text-center text-sm text-red-200">
      {{ store.detailError }}
      <button class="ml-3 text-xs font-bold text-[#65e78c] hover:underline" @click="load">Retry</button>
    </div>

    <template v-else-if="playlist">
      <header class="flex flex-col gap-5 sm:flex-row sm:items-end">
        <div class="size-44 shrink-0 overflow-hidden rounded-xl border border-white/[0.06] bg-white/[0.05]">
          <img
            v-if="playlist.coverUrl"
            :src="playlist.coverUrl"
            :alt="`${playlist.name} cover`"
            class="size-full object-cover"
          />
          <span v-else class="grid size-full place-items-center text-[#3f4a44]">
            <ListMusic :size="56" />
          </span>
        </div>
        <div class="flex flex-col gap-2">
          <p class="melodyhub-kicker">PLAYLIST</p>
          <h1 class="text-3xl font-black text-white sm:text-4xl">{{ playlist.name }}</h1>
          <p v-if="playlist.description" class="max-w-xl text-sm text-[#87918a]">{{ playlist.description }}</p>
          <p class="text-xs font-semibold text-[#777]">
            {{ playlist.songCount }} {{ playlist.songCount === 1 ? 'song' : 'songs' }}
            <span v-if="playlist.isPublic" class="ml-1 text-[#65e78c]">· Public</span>
          </p>
          <button
            v-if="songs.length"
            class="mt-2 inline-flex h-11 w-fit items-center gap-2 rounded-full bg-[#65e78c] px-6 text-sm font-black text-[#071108] transition hover:bg-[#54d67b]"
            @click="playAll"
          >
            <Play :size="16" class="fill-current" /> Play
          </button>
        </div>
      </header>

      <div v-if="songs.length === 0" class="rounded-lg border border-white/10 bg-white/[0.02] px-6 py-16 text-center">
        <Music2 :size="42" class="mx-auto mb-4 text-[#444]" />
        <p class="text-sm font-bold text-white">This playlist is empty</p>
        <p class="mt-2 text-xs text-[#777]">Add songs from any song page to build it up.</p>
      </div>

      <ul v-else class="divide-y divide-white/[0.06] overflow-hidden rounded-lg border border-white/[0.06] bg-[#10151a]">
        <li
          v-for="(song, index) in songs"
          :key="song.id"
          class="group flex items-center gap-3 px-3 py-3 transition hover:bg-white/[0.04]"
        >
          <span class="w-6 shrink-0 text-center text-xs font-bold text-[#666]">{{ index + 1 }}</span>
          <button class="relative shrink-0" :title="`Play ${song.title}`" @click="play(song)">
            <img
              v-if="song.coverUrl"
              :src="song.coverUrl"
              :alt="`${song.title} cover`"
              class="size-12 rounded-md object-cover ring-1 ring-white/[0.08]"
            />
            <span v-else class="grid size-12 place-items-center rounded-md bg-white/[0.06] text-[#555]">
              <Music2 :size="18" />
            </span>
          </button>
          <div class="min-w-0 flex-1">
            <RouterLink
              :to="{ name: 'song-detail', params: { slug: song.slug } }"
              class="block truncate text-sm font-bold text-white transition group-hover:text-[#8be8a8]"
            >
              {{ song.title }}
            </RouterLink>
            <p class="mt-1 truncate text-xs text-[#87918a]">{{ artistNames(song) || 'Unknown artist' }}</p>
          </div>
          <button class="melodyhub-icon-btn !size-9 shrink-0" title="Play song" @click="play(song)">
            <Play :size="15" class="fill-current" />
          </button>
          <button
            class="melodyhub-icon-btn !size-9 shrink-0 hover:!text-red-300"
            title="Remove from playlist"
            @click="removeSong(song)"
          >
            <Trash2 :size="15" />
          </button>
        </li>
      </ul>
    </template>
  </div>
</template>
