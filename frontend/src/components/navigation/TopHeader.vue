<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  AudioLines,
  ChevronDown,
  Crown,
  History,
  Library,
  ListMusic,
  LogIn,
  LogOut,
  Menu,
  Mic2,
  PanelRightOpen,
  Search,
  Settings2,
  ShieldCheck,
  UserRound,
  X
} from '@lucide/vue';
import { useAuthStore } from '../../stores/auth.store';
import { featuredArtist } from '../../data/music';

const emit = defineEmits(['toggle-nav', 'toggle-info']);
const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const dropdownOpen = ref(false);
const dropdown = ref(null);
const searchInput = ref('');

const user = computed(() => authStore.user || {});
const displayName = computed(() => authStore.displayName);
const email = computed(() =>
  authStore.isAuthenticated
    ? user.value.email || user.value.username || ''
    : 'Sign in to sync your library'
);
const badgeLabel = computed(() => {
  if (!authStore.isAuthenticated) return 'GUEST';
  return user.value.role || 'LISTENER';
});
const avatarUrl = computed(() => user.value.avatarUrl || featuredArtist.avatar);

function closeOnOutsideClick(event) {
  if (!dropdown.value?.contains(event.target)) dropdownOpen.value = false;
}

async function signOut() {
  dropdownOpen.value = false;
  if (authStore.isAuthenticated) await authStore.logout();
  await router.push({ name: 'login' });
}

function submitSearch() {
  const q = searchInput.value.trim();
  if (!q) return;
  router.push({ name: 'home', query: q ? { q } : {} });
  searchInput.value = '';
}

onMounted(() => document.addEventListener('click', closeOnOutsideClick));
onBeforeUnmount(() => document.removeEventListener('click', closeOnOutsideClick));
</script>

<template>
  <header class="sticky top-0 z-30 flex h-18 items-center gap-3 border-b border-white/[0.055] bg-[#121719]/76 px-4 backdrop-blur-xl sm:px-6">
    <div class="flex min-w-0 items-center gap-3">
      <button class="melodyhub-icon-btn lg:hidden" title="Open navigation" @click="emit('toggle-nav')">
        <Menu :size="20" />
      </button>
      <span class="hidden size-1.5 rounded-full bg-[#65e78c] shadow-[0_0_12px_#1DB954] sm:block" />
      <p class="min-w-0 truncate text-xs font-medium text-[#9aa39d] sm:text-sm">
        {{ route.meta.breadcrumb || route.meta.title }}
      </p>
    </div>

    <form class="ml-auto mr-auto w-full max-w-xs flex-1 sm:max-w-sm" @submit.prevent="submitSearch">
      <label class="flex h-9 cursor-text items-center gap-2 rounded-full border border-white/[0.07] bg-white/[0.05] px-3 text-[#A3A3A3] ring-1 ring-white/[0.05] transition focus-within:border-[#1DB954]/50 focus-within:bg-black/30 focus-within:ring-[#1DB954]/30">
        <Search :size="15" />
        <input
          v-model="searchInput"
          class="min-w-0 flex-1 bg-transparent text-sm text-white outline-none placeholder:text-[#737373]"
          placeholder="Search songs, artists..."
          aria-label="Search Melody Hub"
          autocomplete="off"
        />
        <button v-if="searchInput" type="button" class="grid place-items-center" @click="searchInput = ''">
          <X :size="14" />
        </button>
      </label>
    </form>

    <div class="flex items-center gap-2">
      <button class="melodyhub-icon-btn xl:hidden" title="Open track information" @click="emit('toggle-info')">
        <PanelRightOpen :size="19" />
      </button>
      <div ref="dropdown" class="relative">
        <button
          class="flex items-center gap-2 rounded-full p-1 pr-2 text-left transition hover:bg-white/5"
          aria-haspopup="menu"
          :aria-expanded="dropdownOpen"
          @click.stop="dropdownOpen = !dropdownOpen"
        >
          <span class="relative">
            <img :src="avatarUrl" alt="" class="size-9 rounded-full object-cover ring-2 ring-white/10" />
            <span class="absolute -right-1 -bottom-0.5 grid size-4 place-items-center rounded-full bg-[#1DB954] text-black ring-2 ring-[#0d0d0d]">
              <Crown :size="9" :stroke-width="3" />
            </span>
          </span>
          <span class="hidden max-w-28 sm:block">
            <span class="block truncate text-xs font-bold text-white">{{ displayName }}</span>
            <span class="block text-[9px] font-black tracking-wider text-[#1DB954]">{{ badgeLabel }}</span>
          </span>
          <ChevronDown :size="14" class="hidden text-[#777] sm:block" />
        </button>

        <div
          v-if="dropdownOpen"
          class="absolute right-0 mt-2 w-64 overflow-hidden rounded-lg border border-white/10 bg-[#171717]/95 p-2 shadow-2xl shadow-black/60 backdrop-blur-xl"
          role="menu"
        >
          <div class="border-b border-white/5 px-3 py-3">
            <p class="text-sm font-bold text-white">{{ displayName }}</p>
            <p class="mt-0.5 truncate text-xs text-[#777]">{{ email }}</p>
            <span class="mt-2 inline-flex items-center gap-1 rounded-full bg-[#1DB954]/10 px-2 py-1 text-[10px] font-black text-[#1DB954]">
              <Crown :size="11" /> {{ badgeLabel }}
            </span>
          </div>
          <RouterLink :to="{ name: 'library' }" class="melodyhub-menu-item" @click="dropdownOpen = false">
            <Library :size="16" /> Library
          </RouterLink>
          <RouterLink v-if="authStore.isAuthenticated" :to="{ name: 'playlists' }" class="melodyhub-menu-item" @click="dropdownOpen = false">
            <ListMusic :size="16" /> My playlists
          </RouterLink>
          <RouterLink v-if="authStore.isAuthenticated" :to="{ name: 'library-history' }" class="melodyhub-menu-item" @click="dropdownOpen = false">
            <History :size="16" /> Listen history
          </RouterLink>
          <RouterLink v-if="authStore.isUser" :to="{ name: 'become-an-artist' }" class="melodyhub-menu-item" @click="dropdownOpen = false">
            <Mic2 :size="16" /> Become an Artist
          </RouterLink>
          <RouterLink v-if="authStore.isArtist" :to="{ name: 'artist-dashboard' }" class="melodyhub-menu-item" @click="dropdownOpen = false">
            <Mic2 :size="16" /> Artist Dashboard
          </RouterLink>
          <RouterLink v-if="authStore.isAdmin" :to="{ name: 'admin-dashboard' }" class="melodyhub-menu-item" @click="dropdownOpen = false">
            <ShieldCheck :size="16" /> Admin Dashboard
          </RouterLink>
          <RouterLink :to="{ name: 'profile' }" class="melodyhub-menu-item" @click="dropdownOpen = false">
            <UserRound :size="16" /> Profile
          </RouterLink>
          <button class="melodyhub-menu-item"><AudioLines :size="16" /> Audio settings</button>
          <button class="melodyhub-menu-item"><Settings2 :size="16" /> Preferences</button>
          <button class="melodyhub-menu-item mt-1 border-t border-white/5 pt-3" @click="signOut">
            <component :is="authStore.isAuthenticated ? LogOut : LogIn" :size="16" />
            {{ authStore.isAuthenticated ? 'Log out' : 'Log in' }}
          </button>
        </div>
      </div>
    </div>
  </header>
</template>
