<script setup>
import { computed, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ArrowRight, CheckCircle2, ChevronLeft, ChevronRight, Play } from '@lucide/vue';
import { artists, playlists, podcasts, tracks } from '../data/music';
import { useAuthStore } from '../stores/auth.store';
import { usePlayerStore } from '../stores/player.store';

const route = useRoute();
const authStore = useAuthStore();
const player = usePlayerStore();
const artistScroller = ref(null);

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
</script>

<template>
  <div class="mx-auto max-w-[1180px] space-y-9 px-4 py-6 sm:px-6 sm:py-8">
    <section class="relative overflow-hidden rounded-xl bg-[#151515] px-6 py-7 sm:px-8">
      <div
        class="absolute inset-0 bg-cover bg-center opacity-25"
        :style="{ backgroundImage: `url(${playlists[0].cover})` }"
      />
      <div class="absolute inset-0 bg-gradient-to-r from-[#151515] via-[#151515]/85 to-transparent" />
      <div class="relative max-w-xl">
        <p class="text-xs font-black tracking-[0.16em] text-[#1DB954]">YOUR DAILY MIX</p>
        <h1 class="mt-2 text-2xl font-black text-white sm:text-4xl">{{ sectionTitle }}</h1>
        <p class="mt-3 max-w-lg text-sm leading-6 text-[#a3a3a3]">
          A fresh mix of artists you love and the sounds shaping this week.
        </p>
        <button class="mt-5 inline-flex h-10 items-center gap-2 rounded-full bg-[#1DB954] px-5 text-xs font-black text-black transition hover:scale-[1.03]" @click="player.playTrack(tracks[0])">
          <Play :size="16" class="fill-current" /> PLAY MIX
        </button>
      </div>
    </section>

    <section>
      <div class="mb-4 flex items-end justify-between">
        <div><p class="sonix-kicker">CURATED FOR YOU</p><h2 class="sonix-section-title">Trending playlists</h2></div>
        <RouterLink :to="{ name: 'explore' }" class="flex items-center gap-1 text-xs font-bold text-[#8b8b8b] hover:text-white">See all <ArrowRight :size="14" /></RouterLink>
      </div>
      <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <button
          v-for="(playlist, index) in playlists"
          :key="playlist.id"
          class="group min-w-0 text-left"
          @click="player.playTrack(tracks[index])"
        >
          <span class="relative block aspect-square overflow-hidden rounded-lg bg-[#181818]">
            <img :src="playlist.cover" :alt="playlist.title" class="h-full w-full object-cover transition duration-500 group-hover:scale-105" />
            <span class="absolute inset-0 bg-black/10 transition group-hover:bg-black/25" />
            <span class="absolute right-3 bottom-3 grid size-11 translate-y-3 place-items-center rounded-full bg-[#1DB954] text-black opacity-0 shadow-xl transition group-hover:translate-y-0 group-hover:opacity-100">
              <Play :size="19" class="ml-0.5 fill-current" />
            </span>
          </span>
          <span class="mt-3 block truncate text-sm font-bold text-white">{{ playlist.title }}</span>
          <span class="mt-1 block line-clamp-2 text-xs leading-5 text-[#777]">{{ playlist.subtitle }}</span>
        </button>
      </div>
    </section>

    <section>
      <div class="mb-4 flex items-end justify-between">
        <div><p class="sonix-kicker">ON REPEAT</p><h2 class="sonix-section-title">Top artists</h2></div>
        <div class="flex gap-1">
          <button class="sonix-icon-btn" title="Previous artists" @click="scrollArtists(-1)"><ChevronLeft :size="18" /></button>
          <button class="sonix-icon-btn" title="Next artists" @click="scrollArtists(1)"><ChevronRight :size="18" /></button>
        </div>
      </div>
      <div ref="artistScroller" class="no-scrollbar flex snap-x gap-4 overflow-x-auto">
        <RouterLink
          v-for="artist in artists"
          :key="artist.id"
          :to="{ name: 'artist-detail', params: { slug: artist.slug } }"
          class="group w-32 shrink-0 snap-start text-center sm:w-40"
        >
          <span class="relative mx-auto block aspect-square overflow-hidden rounded-full bg-[#181818] ring-1 ring-white/5">
            <img :src="artist.avatar" :alt="artist.name" class="h-full w-full object-cover transition duration-500 group-hover:scale-105" />
          </span>
          <span class="mt-3 flex items-center justify-center gap-1 truncate text-sm font-bold text-white">
            {{ artist.name }} <CheckCircle2 v-if="artist.verified" :size="13" class="fill-[#1DB954] text-black" />
          </span>
          <span class="mt-1 block text-xs text-[#777]">{{ artist.genre }}</span>
        </RouterLink>
      </div>
    </section>

    <section>
      <div class="mb-4"><p class="sonix-kicker">JUST LANDED</p><h2 class="sonix-section-title">New releases</h2></div>
      <div class="grid gap-2 sm:grid-cols-2">
        <button
          v-for="track in tracks.slice(0, 6)"
          :key="track.id"
          class="group flex min-w-0 items-center gap-3 rounded-lg p-2 text-left transition hover:bg-white/5"
          @click="player.playTrack(track)"
        >
          <span class="relative shrink-0">
            <img :src="track.cover" :alt="`${track.title} cover`" class="size-13 rounded-md object-cover" />
            <span class="absolute inset-0 grid place-items-center rounded-md bg-black/50 opacity-0 transition group-hover:opacity-100">
              <Play :size="17" class="fill-white text-white" />
            </span>
          </span>
          <span class="min-w-0 flex-1">
            <span class="block truncate text-sm font-bold text-white">{{ track.title }}</span>
            <span class="mt-1 block truncate text-xs text-[#777]">{{ track.artist }} · {{ track.album }}</span>
          </span>
          <span class="text-[10px] font-bold text-[#666]">{{ track.released }}</span>
        </button>
      </div>
    </section>

    <section class="pb-8">
      <div class="mb-4 flex items-end justify-between">
        <div><p class="sonix-kicker">LISTEN DEEPER</p><h2 class="sonix-section-title">Podcasts for you</h2></div>
        <RouterLink :to="{ name: 'podcasts' }" class="text-xs font-bold text-[#8b8b8b] hover:text-white">Browse podcasts</RouterLink>
      </div>
      <div class="grid gap-3 sm:grid-cols-3">
        <article v-for="podcast in podcasts" :key="podcast.id" class="flex min-w-0 gap-3 rounded-lg bg-white/[0.035] p-3 transition hover:bg-white/[0.065]">
          <img :src="podcast.cover" :alt="podcast.title" class="size-20 rounded-md object-cover" />
          <div class="min-w-0">
            <p class="line-clamp-2 text-sm font-bold text-white">{{ podcast.title }}</p>
            <p class="mt-1 truncate text-xs text-[#777]">{{ podcast.host }}</p>
            <p class="mt-3 text-[10px] font-bold text-[#1DB954]">{{ podcast.length }}</p>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>
