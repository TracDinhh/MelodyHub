<script setup>
import { ref } from 'vue';
import { Check, CheckCircle2, MoreHorizontal, Play } from '@lucide/vue';
import TrackRow from '../components/music/TrackRow.vue';
import { featuredArtist, playlists, tracks } from '../data/music';
import { usePlayerStore } from '../stores/player.store';

const player = usePlayerStore();
const following = ref(true);
const activeTab = ref('Popular');
const tabs = ['Popular', 'Albums', 'Songs', 'Fans Also Like', 'About'];
</script>

<template>
  <div class="pb-10">
    <section
      class="relative min-h-[330px] overflow-hidden bg-cover bg-center"
      :style="{ backgroundImage: `url(${featuredArtist.hero})` }"
    >
      <div class="absolute inset-0 bg-gradient-to-t from-[#0d0d0d] via-black/45 to-black/20" />
      <div class="relative flex min-h-[330px] items-end px-5 pb-7 sm:px-8">
        <div class="w-full">
          <div class="flex items-center gap-2 text-xs font-bold text-white/80">
            <CheckCircle2 :size="18" class="fill-[#1DB954] text-black" /> Verified artist
          </div>
          <h1 class="mt-2 text-5xl font-black text-white sm:text-7xl">{{ featuredArtist.name }}</h1>
          <p class="mt-3 text-sm text-white/70">{{ featuredArtist.monthlyListeners }} monthly listeners</p>
          <div class="mt-5 flex flex-wrap items-center gap-3">
            <button class="inline-flex h-11 items-center gap-2 rounded-full bg-[#1DB954] px-6 text-xs font-black text-black transition hover:scale-[1.03]" @click="player.playTrack(tracks[0])">
              <Play :size="17" class="fill-current" /> PLAY ALL
            </button>
            <button class="inline-flex h-11 items-center gap-2 rounded-full border border-white/25 px-5 text-xs font-bold text-white transition hover:border-white">
              <Check v-if="following" :size="15" /> {{ following ? 'Following' : 'Follow' }}
            </button>
            <button class="sonix-icon-btn !border !border-white/20" title="Artist options"><MoreHorizontal :size="19" /></button>
          </div>
        </div>
      </div>
    </section>

    <nav class="no-scrollbar flex overflow-x-auto border-b border-white/5 px-5 sm:px-8">
      <button
        v-for="tab in tabs"
        :key="tab"
        class="relative h-13 shrink-0 px-4 text-xs font-bold transition"
        :class="activeTab === tab ? 'text-white after:absolute after:inset-x-4 after:bottom-0 after:h-0.5 after:bg-[#1DB954]' : 'text-[#707070] hover:text-white'"
        @click="activeTab = tab"
      >
        {{ tab }}
      </button>
    </nav>

    <div class="px-4 py-7 sm:px-7">
      <section v-if="activeTab === 'Popular' || activeTab === 'Songs'">
        <div class="mb-3 flex items-end justify-between px-3">
          <div><p class="sonix-kicker">ESSENTIALS</p><h2 class="sonix-section-title">Popular</h2></div>
          <p class="text-[10px] font-bold text-[#666]">MONTHLY PLAYS</p>
        </div>
        <div class="overflow-x-auto">
          <TrackRow v-for="(track, index) in tracks.slice(0, 5)" :key="track.id" :track="track" :index="index" />
        </div>
      </section>

      <section v-else-if="activeTab === 'Albums'" class="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
        <article v-for="playlist in playlists" :key="playlist.id">
          <img :src="playlist.cover" :alt="playlist.title" class="aspect-square w-full rounded-lg object-cover" />
          <h3 class="mt-3 text-sm font-bold text-white">{{ playlist.title }}</h3>
          <p class="mt-1 text-xs text-[#777]">Album · 2026</p>
        </article>
      </section>

      <section v-else-if="activeTab === 'About'" class="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
        <img :src="featuredArtist.avatar" :alt="featuredArtist.name" class="max-h-[420px] w-full rounded-lg object-cover" />
        <div>
          <p class="sonix-kicker">ABOUT</p>
          <h2 class="sonix-section-title">{{ featuredArtist.name }}</h2>
          <p class="mt-5 text-sm leading-7 text-[#999]">{{ featuredArtist.bio }}</p>
          <div class="mt-6 grid grid-cols-2 gap-3">
            <div class="rounded-lg bg-white/5 p-4"><p class="text-xl font-black text-white">{{ featuredArtist.followers }}</p><p class="text-xs text-[#777]">Followers</p></div>
            <div class="rounded-lg bg-white/5 p-4"><p class="text-xl font-black text-white">{{ featuredArtist.monthlyListeners }}</p><p class="text-xs text-[#777]">Monthly listeners</p></div>
          </div>
        </div>
      </section>

      <section v-else>
        <p class="sonix-kicker">DISCOVER</p>
        <h2 class="sonix-section-title">{{ activeTab }}</h2>
        <p class="mt-4 max-w-xl text-sm leading-6 text-[#777]">More artists selected from the same late-night alternative R&B scene.</p>
      </section>
    </div>
  </div>
</template>
