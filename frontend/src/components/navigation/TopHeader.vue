<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  AudioLines,
  ChevronDown,
  Crown,
  Heart,
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
  router.push({ name: 'home', query: { q } });
  searchInput.value = '';
}

onMounted(() => document.addEventListener('click', closeOnOutsideClick));
onBeforeUnmount(() => document.removeEventListener('click', closeOnOutsideClick));
</script>

<template>
  <header class="relative z-30 flex h-[3.75rem] items-center border-b border-white/[0.05]">

    <!-- Content -->
    <div class="relative flex w-full items-center gap-4 px-5 sm:px-6">
      <!-- Left: Hamburger + breadcrumb -->
      <button
        class="melodyhub-icon-btn lg:hidden"
        title="Open navigation"
        @click="emit('toggle-nav')"
      >
        <Menu :size="19" />
      </button>

      <div class="flex min-w-0 items-center gap-3">
        <p class="truncate text-xs font-medium tracking-wide text-[#5A6860] sm:text-sm">
          {{ route.meta.breadcrumb || route.meta.title }}
        </p>
      </div>

      <!-- Center: Search -->
      <form
        class="ml-auto mr-auto w-full max-w-[300px] flex-1 sm:max-w-[380px]"
        @submit.prevent="submitSearch"
      >
        <div class="group flex h-9 items-center gap-2.5 rounded-lg border border-white/[0.06] bg-white/[0.03] px-3.5 transition-all duration-200 hover:border-white/[0.10] focus-within:border-[#3DDE7C]/50 focus-within:bg-white/[0.04]">
          <Search :size="14" class="shrink-0 text-[#4E5A52] transition-colors group-focus-within:text-[#3DDE7C]" />
          <input
            v-model="searchInput"
            class="min-w-0 flex-1 bg-transparent text-sm text-[#EDE9E0] outline-none placeholder:text-[#3A4238]"
            placeholder="Search songs, artists..."
            aria-label="Search Melody Hub"
            autocomplete="off"
          />
          <button
            v-if="searchInput"
            type="button"
            class="grid size-5 shrink-0 place-items-center rounded text-[#4E5A52] transition-colors hover:text-[#EDE9E0]"
            @click="searchInput = ''"
          >
            <X :size="12" />
          </button>
        </div>
      </form>

      <!-- Right: Avatar + dropdown -->
      <div class="flex items-center gap-2">
        <button
          class="melodyhub-icon-btn xl:hidden"
          title="Open track info"
          @click="emit('toggle-info')"
        >
          <PanelRightOpen :size="18" />
        </button>

        <div ref="dropdown" class="relative">
          <button
            class="flex items-center gap-2.5 rounded-lg p-1 pr-3 text-left transition-all duration-200 hover:bg-white/[0.05]"
            :class="dropdownOpen ? 'bg-white/[0.06]' : ''"
            aria-haspopup="menu"
            :aria-expanded="dropdownOpen"
            @click.stop="dropdownOpen = !dropdownOpen"
          >
            <span class="relative block shrink-0">
              <img
                :src="avatarUrl"
                alt=""
                class="size-8 rounded-lg object-cover ring-1 ring-white/[0.08] transition-all duration-200"
                :class="dropdownOpen ? 'ring-[#3DDE7C]/40' : ''"
              />
              <span class="absolute -right-0.5 -bottom-0.5 grid size-4 place-items-center rounded bg-[#3DDE7C] text-[#0B0D0F]">
                <Crown :size="9" :stroke-width="3" />
              </span>
            </span>
            <span class="hidden max-w-[88px] sm:block">
              <span class="block truncate text-xs font-semibold text-[#EDE9E0]">{{ displayName }}</span>
              <span class="block text-[9px] font-bold uppercase tracking-widest text-[#3DDE7C]">{{ badgeLabel }}</span>
            </span>
            <ChevronDown
              :size="13"
              class="hidden text-[#4E5A52] transition-transform duration-200 sm:block"
              :class="dropdownOpen ? 'rotate-180 text-[#3DDE7C]' : ''"
            />
          </button>

          <!-- Dropdown -->
          <Transition
            enter-active-class="transition-all duration-200 ease-out"
            enter-from-class="opacity-0 translate-y-1"
            enter-to-class="opacity-100 translate-y-0"
            leave-active-class="transition-all duration-150 ease-in"
            leave-from-class="opacity-100 translate-y-0"
            leave-to-class="opacity-0 translate-y-1"
          >
            <div
              v-if="dropdownOpen"
              class="surface-glass absolute right-0 top-full z-50 mt-2 w-[260px] overflow-hidden rounded-xl"
            >
              <!-- User card -->
              <div class="flex items-center gap-3 border-b border-white/[0.06] px-4 py-4">
                <img
                  :src="avatarUrl"
                  alt=""
                  class="size-9 shrink-0 rounded-lg object-cover ring-1 ring-white/8"
                />
                <div class="min-w-0 flex-1">
                  <p class="truncate text-sm font-semibold text-[#EDE9E0]">{{ displayName }}</p>
                  <p class="truncate text-xs text-[#4E5A52]">{{ email }}</p>
                </div>
              </div>

              <!-- Nav -->
              <nav class="p-2">
                <RouterLink :to="{ name: 'library' }" class="melodyhub-menu-item" @click="dropdownOpen = false">
                  <Library :size="15" /> Library
                </RouterLink>
                <RouterLink v-if="authStore.isAuthenticated" :to="{ name: 'liked-songs' }" class="melodyhub-menu-item" @click="dropdownOpen = false">
                  <Heart :size="15" /> Liked songs
                </RouterLink>
                <RouterLink v-if="authStore.isAuthenticated" :to="{ name: 'playlists' }" class="melodyhub-menu-item" @click="dropdownOpen = false">
                  <ListMusic :size="15" /> My playlists
                </RouterLink>
                <RouterLink v-if="authStore.isAuthenticated" :to="{ name: 'library-history' }" class="melodyhub-menu-item" @click="dropdownOpen = false">
                  <History :size="15" /> Listen history
                </RouterLink>

                <div v-if="authStore.isUser || authStore.isArtist || authStore.isAdmin" class="my-1.5 border-t border-white/[0.05]" />

                <RouterLink v-if="authStore.isUser" :to="{ name: 'become-an-artist' }" class="melodyhub-menu-item" @click="dropdownOpen = false">
                  <Mic2 :size="15" /> Become an Artist
                </RouterLink>
                <RouterLink v-if="authStore.isArtist" :to="{ name: 'artist-dashboard' }" class="melodyhub-menu-item" @click="dropdownOpen = false">
                  <Mic2 :size="15" /> Artist Dashboard
                </RouterLink>
                <RouterLink v-if="authStore.isAdmin" :to="{ name: 'admin-dashboard' }" class="melodyhub-menu-item" @click="dropdownOpen = false">
                  <ShieldCheck :size="15" /> Admin Dashboard
                </RouterLink>

                <div class="my-1.5 border-t border-white/[0.05]" />

                <RouterLink :to="{ name: 'profile' }" class="melodyhub-menu-item" @click="dropdownOpen = false">
                  <UserRound :size="15" /> Profile
                </RouterLink>
                <button class="melodyhub-menu-item">
                  <AudioLines :size="15" /> Audio settings
                </button>
                <button class="melodyhub-menu-item">
                  <Settings2 :size="15" /> Preferences
                </button>

                <div class="my-1.5 border-t border-white/[0.05]" />

                <button class="melodyhub-menu-item w-full" @click="signOut">
                  <component :is="authStore.isAuthenticated ? LogOut : LogIn" :size="15" />
                  {{ authStore.isAuthenticated ? 'Log out' : 'Log in' }}
                </button>
              </nav>
            </div>
          </Transition>
        </div>
      </div>
    </div>
  </header>
</template>
