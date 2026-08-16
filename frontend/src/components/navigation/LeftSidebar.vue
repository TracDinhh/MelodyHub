<script setup>
import { ref } from 'vue';
import { RouterLink } from 'vue-router';
import {
  Album,
  Antenna,
  Compass,
  Disc3,
  Heart,
  History,
  Home,
  ListMusic,
  Mic2,
  MoreHorizontal,
  Plus,
  Trash2,
  X
} from '@lucide/vue';
import { playlists, tracks } from '../../data/music';
import { useAuthStore } from '../../stores/auth.store';
import logoUrl from '../../assets/styles/icons/logo.png';

defineProps({ mobileOpen: Boolean });
const emit = defineEmits(['close']);

const authStore = useAuthStore();
const playlistItems = ref(playlists.slice(0, 5));
const navItems = [
  { label: 'Home', route: 'home', icon: Home },
  { label: 'Explore', route: 'explore', icon: Compass },
  { label: 'Radio', route: 'radio', icon: Antenna },
  { label: 'Artists', route: 'artists', icon: Mic2 },
  { label: 'Albums', route: 'albums', icon: Album },
  { label: 'Podcasts', route: 'podcasts', icon: Disc3 }
];
// Library shortcuts only make sense for a signed-in listener.
const libraryItems = [
  { label: 'Liked Songs', route: 'library-liked', icon: Heart },
  { label: 'Listen history', route: 'library-history', icon: History }
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
    class="surface-panel fixed inset-y-0 left-0 z-50 flex w-[240px] flex-col border-r border-white/[0.06] transition-transform duration-300 lg:static lg:z-auto lg:translate-x-0"
    :class="mobileOpen ? 'translate-x-0' : '-translate-x-full'"
  >
    <!-- Logo strip -->
    <header class="flex h-[4.5rem] shrink-0 items-center justify-between px-5">
      <RouterLink :to="{ name: 'home' }" class="flex min-w-0 flex-1 items-center" @click="emit('close')">
        <img :src="logoUrl" alt="MelodyHub logo" class="h-[54px] w-[164px] shrink-0 object-contain object-left" />
      </RouterLink>
      <button class="melodyhub-icon-btn hidden lg:grid" title="More options">
        <MoreHorizontal :size="17" />
      </button>
      <button class="melodyhub-icon-btn lg:hidden" title="Close menu" @click="emit('close')">
        <X :size="17" />
      </button>
    </header>

    <!-- Section label -->
    <div class="px-5 pt-4">
      <p class="text-[10px] font-bold uppercase tracking-[0.18em] text-[#A1A1AA]">Menu</p>
    </div>

    <!-- Nav items -->
    <nav class="mt-2 space-y-1 px-3" aria-label="Primary navigation">
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
    <div class="mt-7 px-5">
      <div class="flex items-center justify-between">
        <p class="text-[10px] font-bold uppercase tracking-[0.18em] text-[#A1A1AA]">Your library</p>
        <button
          class="grid size-7 place-items-center rounded-full bg-white/[0.05] text-[#C4C4CC] transition-colors duration-200 hover:bg-[#20E878] hover:text-[#09090B]"
          title="Create playlist"
          @click="addPlaylist"
        >
          <Plus :size="13" />
        </button>
      </div>
    </div>

    <!-- Library shortcuts (signed-in listeners only) -->
    <nav v-if="authStore.isAuthenticated" class="mt-2 space-y-1 px-3" aria-label="Library">
      <RouterLink
        v-for="item in libraryItems"
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

    <!-- Playlist list -->
    <div class="mt-2 flex-1 space-y-1 overflow-y-auto px-3 pr-2">
      <div
        v-for="playlist in playlistItems"
        :key="playlist.id"
        class="group flex h-10 cursor-pointer items-center gap-2.5 rounded-xl px-3 text-[#A1A1AA] transition-all duration-200 hover:bg-white/[0.05] hover:text-[#F4FFF7]"
      >
        <ListMusic :size="15" class="shrink-0 text-[#71717A] transition-colors group-hover:text-[#20E878]" />
        <span class="min-w-0 flex-1 truncate text-xs font-medium">{{ playlist.title }}</span>
        <button
          class="grid size-6 shrink-0 place-items-center rounded text-[#27272A] opacity-0 transition-all duration-200 hover:bg-white/10 hover:text-red-400 group-hover:opacity-100"
          @click.stop="deletePlaylist(playlist.id)"
        >
          <Trash2 :size="12" />
        </button>
      </div>
    </div>

    <!-- Recently saved widget -->
    <div class="mx-3 mb-4 mt-3 overflow-hidden rounded-2xl border border-white/[0.07] bg-white/[0.025]">
      <img
        :src="tracks[2].cover"
        :alt="`${tracks[2].title} cover`"
        class="h-16 w-full object-cover"
        style="opacity: 0.72;"
      />
      <div class="border-t border-white/[0.05] px-3.5 py-3.5">
        <p class="text-[9px] font-bold uppercase tracking-[0.16em] text-[#A1A1AA]">Recently saved</p>
        <p class="mt-1 truncate text-xs font-semibold text-[#F4FFF7]">{{ tracks[2].title }}</p>
        <p class="mt-0.5 truncate text-[11px] text-[#A1A1AA]">{{ tracks[2].artist }}</p>
      </div>
    </div>
  </aside>
</template>
