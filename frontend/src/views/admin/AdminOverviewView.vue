<script setup>
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import {
  LayoutDashboard,
  LoaderCircle,
  Mic2,
  Music2,
  RefreshCw,
  ShieldCheck,
  UserCheck,
  UserRound,
  Users
} from '@lucide/vue';
import { adminService } from '../../services/adminService';

const stats = ref(null);
const isLoading = ref(true);
const error = ref('');

const cards = computed(() => {
  const s = stats.value;
  if (!s) return [];
  return [
    { label: 'Total users', value: s.totalUsers, icon: Users, tint: 'text-[#1DB954]' },
    { label: 'Listeners', value: s.listeners, icon: UserRound, tint: 'text-sky-300' },
    { label: 'Artists', value: s.artists, icon: Mic2, tint: 'text-fuchsia-300' },
    { label: 'Admins', value: s.admins, icon: ShieldCheck, tint: 'text-amber-300' },
    { label: 'Artist profiles', value: s.artistProfiles, icon: Mic2, tint: 'text-fuchsia-300' },
    { label: 'Published songs', value: s.publishedSongs, icon: Music2, tint: 'text-emerald-300' }
  ];
});

async function load() {
  isLoading.value = true;
  error.value = '';
  try {
    stats.value = await adminService.getStats();
  } catch (requestError) {
    error.value = requestError.message || 'Unable to load statistics.';
  } finally {
    isLoading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="mx-auto w-full max-w-6xl px-5 py-8 pb-12 sm:px-8">
    <div class="mb-6 flex items-center justify-between gap-3">
      <div class="flex items-center gap-3">
        <LayoutDashboard :size="28" class="text-[#1DB954]" />
        <div>
          <p class="sonix-kicker">ADMIN</p>
          <h1 class="sonix-section-title">Overview</h1>
        </div>
      </div>
      <button
        class="inline-flex h-9 items-center gap-2 rounded-full border border-white/15 px-4 text-xs font-bold text-[#bbb] transition hover:border-[#1DB954]/70 hover:text-white disabled:opacity-50"
        :disabled="isLoading"
        @click="load"
      >
        <RefreshCw :size="14" :class="{ 'animate-spin': isLoading }" /> Refresh
      </button>
    </div>

    <p v-if="error" class="mb-4 rounded-md bg-red-500/10 px-3 py-2 text-xs text-red-300" role="alert">{{ error }}</p>

    <div v-if="isLoading" class="flex min-h-64 items-center justify-center text-sm text-[#888]">
      <LoaderCircle :size="20" class="mr-3 animate-spin text-[#1DB954]" /> Loading statistics
    </div>

    <template v-else-if="stats">
      <!-- Pending requests highlight -->
      <RouterLink
        :to="{ name: 'admin-artist-requests' }"
        class="mb-6 flex items-center justify-between gap-4 border p-5 transition"
        :class="stats.pendingArtistRequests > 0
          ? 'border-[#1DB954]/30 bg-[#1DB954]/10 hover:bg-[#1DB954]/15'
          : 'border-white/10 bg-[#121212] hover:border-white/20'"
      >
        <div class="flex items-center gap-4">
          <span
            class="grid size-12 shrink-0 place-items-center rounded-full"
            :class="stats.pendingArtistRequests > 0 ? 'bg-[#1DB954] text-black' : 'bg-white/10 text-[#888]'"
          >
            <UserCheck :size="22" />
          </span>
          <div>
            <p class="text-2xl font-black text-white">{{ stats.pendingArtistRequests }}</p>
            <p class="text-sm text-[#aaa]">Artist requests waiting for review</p>
          </div>
        </div>
        <span class="text-xs font-bold text-[#1DB954]">Review →</span>
      </RouterLink>

      <!-- Stat cards -->
      <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <div
          v-for="card in cards"
          :key="card.label"
          class="border border-white/10 bg-[#121212] p-5"
        >
          <div class="flex items-center justify-between">
            <p class="text-xs font-bold uppercase tracking-wide text-[#888]">{{ card.label }}</p>
            <component :is="card.icon" :size="18" :class="card.tint" />
          </div>
          <p class="mt-3 text-3xl font-black text-white">{{ card.value }}</p>
        </div>
      </div>
    </template>
  </div>
</template>
