<script setup>
import { onMounted, watch } from 'vue';
import { RouterLink } from 'vue-router';
import {
  Album,
  Antenna,
  Compass,
  Disc3,
  Home,
  Crown,
  ListMusic,
  Mic2,
  MoreHorizontal,
  X
} from '@lucide/vue';
import { useAuthStore } from '../../stores/auth.store';
import { usePlaylistStore } from '../../stores/playlist.store';
import logoUrl from '../../assets/styles/icons/logo.png';

defineProps({ mobileOpen: Boolean });
const emit = defineEmits(['close']);

const authStore = useAuthStore();
const playlistStore = usePlaylistStore();
const navItems = [
  { label: 'Home', route: 'home', icon: Home },
  { label: 'Explore', route: 'explore', icon: Compass },
  { label: 'Radio', route: 'radio', icon: Antenna },
  { label: 'Artists', route: 'artists', icon: Mic2 },
  { label: 'Albums', route: 'albums', icon: Album },
  { label: 'Podcasts', route: 'podcasts', icon: Disc3 }
];

function loadPlaylists() {
  if (
    authStore.isAuthenticated
    && !playlistStore.isSidebarLoading
    && playlistStore.sidebarPlaylists.length === 0
  ) {
    void playlistStore.loadSidebarPlaylists();
  }
}

onMounted(loadPlaylists);
watch(
  () => authStore.isAuthenticated,
  (isAuthenticated) => {
    if (isAuthenticated) loadPlaylists();
    else playlistStore.reset();
  }
);
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
    class="surface-panel fixed inset-y-0 left-0 z-50 flex min-h-0 w-[240px] flex-col border-r border-white/[0.06] transition-transform duration-300 lg:static lg:z-auto lg:translate-x-0"
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

    <template v-if="authStore.isAuthenticated">
      <div class="mt-7 flex items-center justify-between px-5">
        <p class="text-[10px] font-bold uppercase tracking-[0.18em] text-[#A1A1AA]">Your playlists</p>
        <RouterLink
          :to="{ name: 'playlists' }"
          class="text-[10px] font-bold text-[#20E878] transition hover:text-[#64F4A1]"
          @click="emit('close')"
        >
          View all
        </RouterLink>
      </div>

      <nav class="mt-2 min-h-0 flex-1 space-y-1 overflow-y-auto px-3" aria-label="Your playlists">
        <RouterLink
          v-for="playlist in playlistStore.sidebarPlaylists"
          :key="playlist.id"
          :to="{ name: 'playlist-detail', params: { id: playlist.id } }"
          class="nav-item"
          active-class="active"
          @click="emit('close')"
        >
          <ListMusic :size="16" class="shrink-0" />
          <span class="min-w-0 flex-1 truncate">{{ playlist.name }}</span>
          <span class="shrink-0 text-[10px] text-[#71717A]">{{ playlist.songCount || 0 }}</span>
        </RouterLink>

        <p
          v-if="playlistStore.isSidebarLoading && playlistStore.sidebarPlaylists.length === 0"
          class="px-3 py-3 text-xs text-[#71717A]"
        >
          Loading playlists…
        </p>
        <button
          v-else-if="playlistStore.sidebarError"
          type="button"
          class="w-full rounded-lg px-3 py-3 text-left text-xs text-red-300 transition hover:bg-white/[0.04]"
          @click="playlistStore.loadSidebarPlaylists"
        >
          Could not load playlists. <span class="font-bold text-[#20E878]">Retry</span>
        </button>
        <p
          v-else-if="playlistStore.sidebarPlaylists.length === 0"
          class="px-3 py-3 text-xs text-[#71717A]"
        >
          No playlists yet.
        </p>
      </nav>
    </template>

    <RouterLink
      :to="{ name: 'premium' }"
      class="mx-3 mb-4 mt-auto flex shrink-0 items-center gap-3 rounded-xl border border-[#20E878]/25 bg-[#20E878]/[0.08] px-3.5 py-3 text-sm font-semibold text-[#F4FFF7] transition hover:bg-[#20E878]/[0.14]"
      @click="emit('close')"
    >
      <Crown :size="16" class="text-[#20E878]" />
      <span>{{ authStore.isPremium ? 'Premium active' : 'Upgrade to Premium' }}</span>
    </RouterLink>
  </aside>
</template>
