<script setup>
import { ref } from 'vue';
import { ListMusic, Play, Trash2 } from '@lucide/vue';
import TrackRow from '../components/music/TrackRow.vue';
import { playlists, tracks } from '../data/music';
import { usePlayerStore } from '../stores/player.store';

const player = usePlayerStore();
const deleted = ref(false);
</script>

<template>
  <div class="pb-10">
    <section class="relative overflow-hidden px-5 py-8 sm:px-8">
      <div class="absolute inset-0 bg-gradient-to-b from-[#16C65A]/15 to-transparent" />
      <div class="relative flex flex-col gap-6 sm:flex-row sm:items-end">
        <div class="grid aspect-square w-44 shrink-0 place-items-center overflow-hidden rounded-lg bg-[#181818] shadow-2xl shadow-black/50">
          <img v-if="!deleted" :src="playlists[0].cover" alt="Night Drive playlist cover" class="h-full w-full object-cover" />
          <ListMusic v-else :size="54" class="text-[#444]" />
        </div>
        <div>
          <p class="text-xs font-black tracking-[0.14em] text-white">PLAYLIST</p>
          <h1 class="mt-2 text-4xl font-black text-white sm:text-6xl">{{ deleted ? 'Playlist removed' : 'Night Drive' }}</h1>
          <p class="mt-3 text-sm text-[#999]">{{ deleted ? 'Create another playlist from the sidebar.' : '42 tracks · 2 hr 48 min · Made by Alex Morgan' }}</p>
          <div v-if="!deleted" class="mt-5 flex flex-wrap gap-3">
            <button class="inline-flex h-11 items-center gap-2 rounded-full bg-[#16C65A] px-6 text-xs font-black text-black" @click="player.playTrack(tracks[0])">
              <Play :size="17" class="fill-current" /> PLAY ALL
            </button>
            <button class="inline-flex h-11 items-center gap-2 rounded-full border border-white/15 px-5 text-xs font-bold text-[#aaa] hover:border-red-400/60 hover:text-red-400" @click="deleted = true">
              <Trash2 :size="16" /> Delete playlist
            </button>
          </div>
        </div>
      </div>
    </section>

    <section v-if="!deleted" class="px-4 py-5 sm:px-7">
      <div class="mb-3 grid min-w-[680px] grid-cols-[40px_minmax(240px,1.5fr)_minmax(130px,0.8fr)_100px_50px_40px] gap-3 px-3 text-[10px] font-black tracking-wider text-[#606060]">
        <span>#</span><span>TITLE</span><span>ALBUM</span><span>DURATION</span><span></span><span></span>
      </div>
      <div class="overflow-x-auto">
        <TrackRow v-for="(track, index) in tracks" :key="track.id" :track="track" :index="index" show-album />
      </div>
    </section>
  </div>
</template>
