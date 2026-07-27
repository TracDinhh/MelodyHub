<script setup>
import { ref } from 'vue';
import { RouterLink } from 'vue-router';
import {
  Album,
  Antenna,
  Compass,
  Disc3,
  Home,
  ListMusic,
  Mic2,
  MoreHorizontal,
  Plus,
  Search,
  SlidersHorizontal,
  Trash2,
  X
} from '@lucide/vue';
import { playlists, tracks } from '../../data/music';

defineProps({ mobileOpen: Boolean });
const emit = defineEmits(['close']);

const playlistItems = ref(playlists.slice(0, 4));
const navItems = [
  { label: 'Home', route: 'home', icon: Home },
  { label: 'Explore', route: 'explore', icon: Compass },
  { label: 'Radio', route: 'radio', icon: Antenna },
  { label: 'Artists', route: 'artists', icon: Mic2 },
  { label: 'Albums', route: 'albums', icon: Album },
  { label: 'Podcasts', route: 'podcasts', icon: Disc3 }
];

function addPlaylist() {
  playlistItems.value.push({
    id: Date.now(),
    title: `New playlist ${playlistItems.value.length + 1}`,
    tracks: 0
  });
}

function deletePlaylist(id) {
  playlistItems.value = playlistItems.value.filter((playlist) => playlist.id !== id);
}
</script>

<template>
  <div
    v-if="mobileOpen"
    class="fixed inset-0 z-40 bg-black/70 backdrop-blur-sm lg:hidden"
    @click="emit('close')"
  />
  <aside
    class="melodyhub-panel-edge fixed inset-y-0 left-0 z-50 flex w-60 flex-col rounded-r-xl border border-white/[0.055] bg-[#101417] transition-transform duration-300 lg:static lg:z-auto lg:rounded-xl lg:translate-x-0"
    :class="mobileOpen ? 'translate-x-0' : '-translate-x-full'"
  >
    <header class="flex h-18 items-center justify-between px-5">
      <RouterLink :to="{ name: 'home' }" class="flex items-center gap-2.5" @click="emit('close')">
        <span class="grid size-9 place-items-center rounded-xl bg-[#1DB954] text-black shadow-[0_7px_20px_rgba(29,185,84,0.22)]">
          <Disc3 :size="21" :stroke-width="2.5" />
        </span>
        <span class="text-base font-black tracking-[0.12em] text-white">MELODY HUB</span>
      </RouterLink>
      <button class="melodyhub-icon-btn hidden lg:grid" title="More options">
        <MoreHorizontal :size="19" />
      </button>
      <button class="melodyhub-icon-btn lg:hidden" title="Close menu" @click="emit('close')">
        <X :size="19" />
      </button>
    </header>

    <div class="px-4 pb-3">
      <label class="flex h-10 items-center gap-2 rounded-lg bg-black/20 px-3 text-[#A3A3A3] ring-1 ring-white/[0.07] transition focus-within:bg-black/35 focus-within:ring-[#1DB954]/60">
        <Search :size="16" />
        <input
          class="min-w-0 flex-1 bg-transparent text-sm text-white outline-none placeholder:text-[#737373]"
          placeholder="Search"
          aria-label="Search Melody Hub"
        />
        <SlidersHorizontal :size="15" />
      </label>
    </div>

    <nav class="space-y-1 px-3" aria-label="Primary navigation">
      <RouterLink
        v-for="item in navItems"
        :key="item.route"
        :to="{ name: item.route }"
        class="group relative flex h-10 items-center gap-3 rounded-md px-3 text-sm font-semibold text-[#8f9893] transition hover:bg-white/[0.06] hover:text-white"
        active-class="bg-[#1DB954]/[0.10] !text-[#65e78c] before:absolute before:-left-3 before:h-6 before:w-0.5 before:rounded-r before:bg-[#1DB954]"
        @click="emit('close')"
      >
        <component :is="item.icon" :size="18" />
        <span>{{ item.label }}</span>
      </RouterLink>
    </nav>

    <section class="mt-7 min-h-0 flex-1 px-3">
      <div class="mb-2 flex items-center justify-between px-3">
        <h2 class="text-[10px] font-black tracking-[0.16em] text-[#737373]">MY PLAYLISTS</h2>
        <button class="melodyhub-icon-btn !size-7" title="Create playlist" @click="addPlaylist">
          <Plus :size="16" />
        </button>
      </div>
      <div class="max-h-full space-y-0.5 overflow-y-auto pr-1">
        <div
          v-for="playlist in playlistItems"
          :key="playlist.id"
          class="group flex h-9 items-center gap-2 rounded-md px-3 text-sm text-[#909090] hover:bg-white/5 hover:text-white"
        >
          <ListMusic :size="15" class="shrink-0" />
          <span class="min-w-0 flex-1 truncate">{{ playlist.title }}</span>
          <button
            class="grid size-7 shrink-0 place-items-center rounded-full text-[#666] opacity-0 transition hover:bg-white/10 hover:text-red-400 group-hover:opacity-100"
            :title="`Delete ${playlist.title}`"
            @click.stop="deletePlaylist(playlist.id)"
          >
            <Trash2 :size="14" />
          </button>
        </div>
      </div>
    </section>

    <div class="m-3 overflow-hidden rounded-lg bg-[#171c1d] ring-1 ring-white/[0.06]">
      <img :src="tracks[2].cover" :alt="`${tracks[2].title} cover`" class="h-20 w-full object-cover opacity-80" />
      <div class="border-t border-white/[0.05] p-3">
        <p class="text-[10px] font-bold uppercase text-[#84918a]">Recently saved</p>
        <p class="mt-1 truncate text-xs font-bold text-white">{{ tracks[2].title }}</p>
        <p class="truncate text-[11px] text-[#888]">{{ tracks[2].artist }}</p>
      </div>
    </div>
  </aside>
</template>
