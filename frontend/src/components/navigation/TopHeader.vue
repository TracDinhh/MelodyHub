<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  AudioLines,
  Check,
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
  if (authStore.isPremium) return 'PREMIUM';
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
  <header class="relative z-30 flex h-[4.5rem] items-center border-b border-white/[0.06] bg-[#0F0F12]/72 backdrop-blur-xl">

    <!-- Content -->
    <div class="relative flex w-full items-center gap-4 px-5 sm:px-8">
      <!-- Left: Hamburger + breadcrumb -->
      <button
        class="melodyhub-icon-btn lg:hidden"
        title="Open navigation"
        @click="emit('toggle-nav')"
      >
        <Menu :size="19" />
      </button>

      <div class="flex min-w-0 items-center gap-3">
        <p class="truncate text-sm font-bold tracking-tight text-[#F4FFF7] sm:text-base">
          {{ route.meta.breadcrumb || route.meta.title }}
        </p>
      </div>

      <!-- Center: Search -->
      <form
        class="ml-auto mr-auto w-full max-w-[320px] flex-1 sm:max-w-[480px]"
        @submit.prevent="submitSearch"
      >
        <div class="group flex h-10 items-center gap-2.5 rounded-full border border-white/[0.08] bg-black/20 px-4 transition-all duration-200 hover:border-white/[0.16] focus-within:border-[#20E878]/70 focus-within:bg-[#121214] focus-within:shadow-[0_0_0_3px_rgba(32,232,120,0.10)]">
          <Search :size="15" class="shrink-0 text-[#A1A1AA] transition-colors group-focus-within:text-[#20E878]" />
          <input
            v-model="searchInput"
            class="min-w-0 flex-1 bg-transparent text-sm text-[#F4FFF7] outline-none placeholder:text-[#71717A]"
            placeholder="What do you want to play?"
            aria-label="Search Melody Hub"
            autocomplete="off"
          />
          <button
            v-if="searchInput"
            type="button"
            class="grid size-5 shrink-0 place-items-center rounded text-[#A1A1AA] transition-colors hover:text-[#F4FFF7]"
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
            class="flex items-center gap-2.5 rounded-full p-1 pr-3 text-left transition-all duration-200 hover:bg-white/[0.06]"
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
                :class="dropdownOpen ? 'ring-[#20E878]/40' : ''"
              />
              <span class="absolute -right-0.5 -bottom-0.5 grid size-4 place-items-center rounded bg-[#20E878] text-[#09090B]">
                <Crown :size="9" :stroke-width="3" />
              </span>
            </span>
            <span class="hidden max-w-[128px] sm:block">
              <span class="block truncate text-sm font-bold leading-tight text-[#F4FFF7]">{{ displayName }}</span>
              <span class="mt-0.5 block text-[11px] font-bold uppercase leading-tight tracking-wider text-[#20E878]">{{ badgeLabel }}</span>
            </span>
            <ChevronDown
              :size="13"
              class="hidden text-[#A1A1AA] transition-transform duration-200 sm:block"
              :class="dropdownOpen ? 'rotate-180 text-[#20E878]' : ''"
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
                  <p class="truncate text-sm font-semibold text-[#F4FFF7]">{{ displayName }}</p>
                  <p class="truncate text-xs text-[#A1A1AA]">{{ email }}</p>
                </div>
              </div>

              <!-- Nav -->
              <nav class="p-2">
                <RouterLink :to="{ name: 'library' }" class="melodyhub-menu-item" @click="dropdownOpen = false">
                  <Library :size="15" /> Library
                </RouterLink>
                <RouterLink v-if="authStore.isAuthenticated" :to="{ name: 'playlists' }" class="melodyhub-menu-item" @click="dropdownOpen = false">
                  <ListMusic :size="15" /> My playlists
                </RouterLink>
                <RouterLink v-if="authStore.isAuthenticated" :to="{ name: 'library-liked' }" class="melodyhub-menu-item" @click="dropdownOpen = false">
                  <Heart :size="15" /> Liked Songs
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

                <RouterLink :to="{ name: 'premium' }" class="melodyhub-menu-item" @click="dropdownOpen = false">
                  <component :is="authStore.isPremium ? Check : Crown" :size="15" />
                  {{ authStore.isPremium ? 'Premium active' : 'Upgrade to Premium' }}
                </RouterLink>

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
