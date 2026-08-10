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
  Trash2,
  X
} from '@lucide/vue';
import { playlists, tracks } from '../../data/music';

defineProps({ mobileOpen: Boolean });
const emit = defineEmits(['close']);

const playlistItems = ref(playlists.slice(0, 5));
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
  playlistItems.value = playlistItems.value.filter((p) => p.id !== id);
}
</script>

<template>
  <!-- Mobile overlay -->
  <div
    v-if="mobileOpen"
    class="fixed inset-0 z-40 bg-black/50 backdrop-blur-sm lg:hidden"
    @click="emit('close')"
  />

  <!-- Sidebar: console housing -->
  <aside
    class="surface-panel fixed inset-y-0 left-0 z-50 flex w-[224px] flex-col transition-transform duration-300 lg:static lg:z-auto lg:translate-x-0"
    :class="mobileOpen ? 'translate-x-0' : '-translate-x-full'"
  >
    <!-- Logo strip -->
    <header class="flex h-[3.75rem] shrink-0 items-center justify-between border-b border-white/[0.05] px-4">
      <RouterLink :to="{ name: 'home' }" class="flex items-center gap-2" @click="emit('close')">
        <span class="flex size-8 shrink-0 items-center justify-center rounded-lg bg-[#3DDE7C] text-[#0B0D0F]">
          <Disc3 :size="19" :stroke-width="2.5" />
        </span>
        <div class="leading-none">
          <p class="font-display text-base tracking-[0.18em] text-[#EDE9E0]">MELODY</p>
          <p class="text-[9px] font-bold uppercase tracking-[0.32em] text-[#3DDE7C]">Hub</p>
        </div>
      </RouterLink>
      <button class="melodyhub-icon-btn hidden lg:grid" title="More options">
        <MoreHorizontal :size="17" />
      </button>
      <button class="melodyhub-icon-btn lg:hidden" title="Close menu" @click="emit('close')">
        <X :size="17" />
      </button>
    </header>

    <!-- Section label -->
    <div class="px-4 pt-4">
      <p class="text-[9px] font-bold uppercase tracking-[0.28em] text-[#3A4A3E]">Navigation</p>
    </div>

    <!-- Nav items -->
    <nav class="mt-1.5 space-y-0.5 px-3" aria-label="Primary navigation">
      <RouterLink
        v-for="item in navItems"
        :key="item.route"
        :to="{ name: item.route }"
        class="nav-item"
        active-class="active"
        @click="emit('close')"
      >
        <component :is="item.icon" :size="17" class="shrink-0 transition-transform duration-200" />
        <span>{{ item.label }}</span>
      </RouterLink>
    </nav>

    <!-- Section label -->
    <div class="mt-5 px-4">
      <div class="flex items-center justify-between">
        <p class="text-[9px] font-bold uppercase tracking-[0.28em] text-[#3A4A3E]">Playlists</p>
        <button
          class="grid size-6 place-items-center rounded text-[#3A4A3E] transition-colors duration-200 hover:bg-white/[0.04] hover:text-[#EDE9E0]"
          title="Create playlist"
          @click="addPlaylist"
        >
          <Plus :size="13" />
        </button>
      </div>
    </div>

    <!-- Playlist list -->
    <div class="mt-1.5 flex-1 space-y-0.5 overflow-y-auto px-3 pr-1">
      <div
        v-for="playlist in playlistItems"
        :key="playlist.id"
        class="group flex h-9 cursor-pointer items-center gap-2 rounded-lg px-2.5 text-[#5A6860] transition-all duration-200 hover:bg-white/[0.04] hover:text-[#EDE9E0]"
      >
        <ListMusic :size="14" class="shrink-0 text-[#3A4A3E] transition-colors group-hover:text-[#3DDE7C]" />
        <span class="min-w-0 flex-1 truncate text-xs font-medium">{{ playlist.title }}</span>
        <button
          class="grid size-6 shrink-0 place-items-center rounded text-[#2A3830] opacity-0 transition-all duration-200 hover:bg-white/10 hover:text-red-400 group-hover:opacity-100"
          @click.stop="deletePlaylist(playlist.id)"
        >
          <Trash2 :size="12" />
        </button>
      </div>
    </div>

    <!-- Recently saved widget -->
    <div class="mx-3 mb-3 mt-2 overflow-hidden rounded-xl border border-white/[0.05]">
      <img
        :src="tracks[2].cover"
        :alt="`${tracks[2].title} cover`"
        class="h-14 w-full object-cover"
        style="opacity: 0.65;"
      />
      <div class="border-t border-white/[0.04] px-3.5 py-3">
        <p class="text-[9px] font-bold uppercase tracking-[0.18em] text-[#3A4A3E]">Recently saved</p>
        <p class="mt-1 truncate text-xs font-semibold text-[#EDE9E0]">{{ tracks[2].title }}</p>
        <p class="mt-0.5 truncate text-[11px] text-[#3A4A3E]">{{ tracks[2].artist }}</p>
      </div>
    </div>
  </aside>
</template>
