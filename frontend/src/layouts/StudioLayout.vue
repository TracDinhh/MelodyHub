<script setup>
import { computed, onMounted, ref } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { Check, ChevronsUpDown, Headphones, LayoutDashboard, LogOut, Menu, Mic2, Music2, Upload, UserCircle, X } from '@lucide/vue';
import { useAuthStore } from '../stores/auth.store';
import { useStudioStore } from '../stores/studio.store';
import logoUrl from '../assets/styles/icons/logo.png';

const authStore = useAuthStore();
const studioStore = useStudioStore();
const route = useRoute();
const router = useRouter();

const navOpen = ref(false);
const selectorOpen = ref(false);

const artistId = computed(() => Number(route.params.artistId));
const currentArtist = computed(() => studioStore.findArtist(artistId.value));

const navItems = computed(() => {
  const id = artistId.value;
  if (!id) return [];
  return [
    { label: 'Overview', to: { name: 'studio-artist-overview', params: { artistId: id } }, icon: LayoutDashboard },
    { label: 'Music', to: { name: 'studio-artist-music', params: { artistId: id } }, icon: Music2 },
    { label: 'Upload Song', to: { name: 'studio-artist-upload', params: { artistId: id } }, icon: Upload },
    { label: 'Profile', to: { name: 'studio-artist-profile', params: { artistId: id } }, icon: UserCircle }
  ];
});

async function loadArtists() {
  try {
    await studioStore.loadMyArtists();
  } catch {
    // Views surface their own errors; the selector simply stays empty.
  }
}

function selectArtist(id) {
  selectorOpen.value = false;
  router.push({ name: 'studio-artist-overview', params: { artistId: id } });
}

async function logout() {
  studioStore.clear();
  await authStore.logout();
  router.push({ name: 'artist-login' });
}

onMounted(loadArtists);
</script>

<template>
  <div class="h-screen overflow-hidden bg-[#09090B] text-white">
    <div class="grid h-screen grid-cols-1 lg:grid-cols-[240px_minmax(0,1fr)]">
      <!-- Sidebar overlay (mobile) -->
      <div
        v-if="navOpen"
        class="fixed inset-0 z-40 bg-black/70 backdrop-blur-sm lg:hidden"
        @click="navOpen = false"
      />

      <!-- Sidebar -->
      <aside
        class="fixed inset-y-0 left-0 z-50 flex w-60 flex-col border-r border-white/5 bg-[#0b0b0b] transition-transform duration-300 lg:static lg:z-auto lg:translate-x-0"
        :class="navOpen ? 'translate-x-0' : '-translate-x-full'"
      >
        <header class="flex h-18 items-center justify-between px-5">
          <img :src="logoUrl" alt="MelodyHub logo" class="h-12 w-40 object-contain object-left" />
          <button class="melodyhub-icon-btn lg:hidden" title="Close menu" @click="navOpen = false">
            <X :size="19" />
          </button>
        </header>

        <div class="mx-4 mb-3 inline-flex items-center justify-center gap-1.5 rounded-full border border-[#16C65A]/20 bg-[#16C65A]/10 py-1 text-[10px] font-bold text-[#16C65A]">
          <Mic2 :size="11" /> ARTIST STUDIO
        </div>

        <!-- Artist selector -->
        <div class="relative mx-3 mb-3">
          <button
            type="button"
            class="flex w-full items-center gap-2.5 rounded-md border border-white/10 bg-white/[0.03] px-3 py-2.5 text-left transition hover:border-[#16C65A]/40"
            :disabled="studioStore.isLoading"
            @click="selectorOpen = !selectorOpen"
          >
            <span class="grid size-8 shrink-0 place-items-center overflow-hidden rounded bg-[#16C65A]/15 text-[#16C65A]">
              <img v-if="currentArtist?.imageUrl" :src="currentArtist.imageUrl" :alt="currentArtist.name" class="h-full w-full object-cover" />
              <Mic2 v-else :size="15" />
            </span>
            <span class="min-w-0 flex-1">
              <span class="block truncate text-sm font-bold text-white">{{ currentArtist?.name || 'Select artist' }}</span>
              <span v-if="currentArtist?.memberRole" class="block text-[10px] font-bold uppercase tracking-wider text-[#16C65A]">
                {{ currentArtist.memberRole }}
              </span>
            </span>
            <ChevronsUpDown :size="14" class="shrink-0 text-[#71717A]" />
          </button>

          <Transition
            enter-active-class="transition-all duration-150 ease-out"
            enter-from-class="opacity-0 -translate-y-1"
            enter-to-class="opacity-100 translate-y-0"
            leave-active-class="transition-all duration-100 ease-in"
            leave-from-class="opacity-100 translate-y-0"
            leave-to-class="opacity-0 -translate-y-1"
          >
            <div
              v-if="selectorOpen"
              class="absolute left-0 right-0 top-full z-50 mt-1 overflow-hidden rounded-lg border border-white/10 bg-[#121214] shadow-2xl shadow-black/60"
            >
              <p v-if="studioStore.myArtists.length === 0" class="px-3 py-3 text-xs text-[#71717A]">No artists yet.</p>
              <ul v-else class="max-h-64 overflow-y-auto py-1">
                <li v-for="artist in studioStore.myArtists" :key="artist.artistId">
                  <button
                    type="button"
                    class="flex w-full items-center gap-2.5 px-3 py-2 text-left transition hover:bg-white/[0.05]"
                    @click="selectArtist(artist.artistId)"
                  >
                    <span class="grid size-7 shrink-0 place-items-center overflow-hidden rounded bg-white/[0.06] text-[#16C65A]">
                      <img v-if="artist.imageUrl" :src="artist.imageUrl" :alt="artist.name" class="h-full w-full object-cover" />
                      <Mic2 v-else :size="13" />
                    </span>
                    <span class="min-w-0 flex-1">
                      <span class="block truncate text-xs font-bold text-white">{{ artist.name }}</span>
                      <span class="block text-[10px] font-bold uppercase tracking-wider text-[#16C65A]">{{ artist.memberRole }}</span>
                    </span>
                    <Check v-if="Number(artist.artistId) === artistId" :size="14" class="shrink-0 text-[#16C65A]" />
                  </button>
                </li>
              </ul>
            </div>
          </Transition>
        </div>

        <nav class="space-y-1 px-3" aria-label="Studio navigation">
          <RouterLink
            v-for="item in navItems"
            :key="item.to.name"
            :to="item.to"
            class="group relative flex h-11 items-center gap-3 rounded-md px-3 text-sm font-semibold text-[#8f8f8f] transition hover:bg-white/5 hover:text-white"
            active-class="bg-white/[0.06] !text-[#16C65A] before:absolute before:-left-3 before:h-6 before:w-0.5 before:rounded-r before:bg-[#16C65A]"
            @click="navOpen = false"
          >
            <component :is="item.icon" :size="18" />
            <span>{{ item.label }}</span>
          </RouterLink>
        </nav>

        <div class="mt-auto border-t border-white/5 p-4">
          <!-- Switch to listener -->
          <RouterLink
            :to="{ name: 'home' }"
            class="mb-3 flex items-center gap-2 rounded-md px-3 py-2 text-xs font-medium text-[#71717A] transition hover:bg-white/5 hover:text-[#16C65A]"
          >
            <Headphones :size="15" />
            <span>Switch to Listener</span>
          </RouterLink>

          <div class="mb-3 flex items-center gap-2 text-xs text-[#888]">
            <Mic2 :size="15" class="text-[#16C65A]" />
            <span class="truncate">{{ authStore.displayName }}</span>
          </div>

          <button
            class="inline-flex h-9 w-full items-center justify-center gap-2 rounded-full border border-white/15 text-xs font-bold text-[#bbb] transition hover:border-red-400/60 hover:text-red-300"
            @click="logout"
          >
            <LogOut :size="15" /> Sign out
          </button>
        </div>
      </aside>

      <!-- Content -->
      <section class="min-w-0 overflow-hidden bg-[#0d0d0d]">
        <header class="flex h-18 items-center gap-3 border-b border-white/5 px-5 lg:hidden">
          <button class="melodyhub-icon-btn" title="Open menu" @click="navOpen = true">
            <Menu :size="20" />
          </button>
          <span class="text-sm font-black tracking-[0.16em]">MELODYHUB ARTIST STUDIO</span>
        </header>
        <main class="h-[calc(100%-4.5rem)] overflow-y-auto lg:h-full">
          <slot />
        </main>
      </section>
    </div>
  </div>
</template>