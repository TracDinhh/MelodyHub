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
import StatChart from './components/StatChart.vue';

const stats = ref(null);
const analytics = ref(null);
const isLoading = ref(true);
const error = ref('');

const cards = computed(() => {
  const s = stats.value;
  if (!s) return [];
  return [
    { label: 'Total users', value: s.totalUsers, icon: Users, tint: 'text-[#16C65A]' },
    { label: 'Listeners', value: s.listeners, icon: UserRound, tint: 'text-sky-300' },
    { label: 'Admins', value: s.admins, icon: ShieldCheck, tint: 'text-amber-300' },
    { label: 'Artist profiles', value: s.artistProfiles, icon: Mic2, tint: 'text-fuchsia-300' },
    { label: 'Published songs', value: s.publishedSongs, icon: Music2, tint: 'text-emerald-300' }
  ];
});

// Shorten ISO dates (yyyy-MM-dd) to MM-dd for compact axis labels.
function shortDate(iso) {
  return typeof iso === 'string' && iso.length === 10 ? iso.slice(5) : iso;
}

// --- User growth (area) ---
const userGrowthCategories = computed(() =>
  (analytics.value?.userGrowth || []).map((d) => shortDate(d.date))
);
const userGrowthSeries = computed(() => [
  { name: 'New users', data: (analytics.value?.userGrowth || []).map((d) => d.count) }
]);

// --- Listens by day (bar) ---
const listensCategories = computed(() =>
  (analytics.value?.listensByDay || []).map((d) => shortDate(d.date))
);
const listensSeries = computed(() => [
  { name: 'Listens', data: (analytics.value?.listensByDay || []).map((d) => d.count) }
]);

// --- Users by role (donut) ---
const usersByRoleLabels = computed(() =>
  (analytics.value?.usersByRole || []).map((r) => r.label)
);
const usersByRoleSeries = computed(() =>
  (analytics.value?.usersByRole || []).map((r) => r.count)
);

// --- Songs by status (donut) ---
const songStatusLabels = computed(() =>
  (analytics.value?.songsByStatus || []).map((r) => r.label)
);
const songStatusSeries = computed(() =>
  (analytics.value?.songsByStatus || []).map((r) => r.count)
);

// --- Artist request funnel (bar) ---
const funnelCategories = computed(() =>
  (analytics.value?.artistRequestFunnel || []).map((r) => r.label)
);
const funnelSeries = computed(() => [
  { name: 'Requests', data: (analytics.value?.artistRequestFunnel || []).map((r) => r.count) }
]);

// --- Top songs (horizontal bar) ---
const topSongsCategories = computed(() =>
  (analytics.value?.topSongs || []).map((s) => s.title)
);
const topSongsSeries = computed(() => [
  { name: 'Plays', data: (analytics.value?.topSongs || []).map((s) => s.playCount) }
]);

const hasSeries = (arr) => Array.isArray(arr) && arr.some((v) => v > 0);

async function load() {
  isLoading.value = true;
  error.value = '';
  try {
    const [statsResult, analyticsResult] = await Promise.all([
      adminService.getStats(),
      adminService.getAnalytics()
    ]);
    stats.value = statsResult;
    analytics.value = analyticsResult;
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
        <LayoutDashboard :size="28" class="text-[#16C65A]" />
        <div>
          <p class="melodyhub-kicker">ADMIN</p>
          <h1 class="melodyhub-section-title">Overview</h1>
        </div>
      </div>
      <button
        class="inline-flex h-9 items-center gap-2 rounded-full border border-white/15 px-4 text-xs font-bold text-[#bbb] transition hover:border-[#16C65A]/70 hover:text-white disabled:opacity-50"
        :disabled="isLoading"
        @click="load"
      >
        <RefreshCw :size="14" :class="{ 'animate-spin': isLoading }" /> Refresh
      </button>
    </div>

    <p v-if="error" class="mb-4 rounded-md bg-red-500/10 px-3 py-2 text-xs text-red-300" role="alert">{{ error }}</p>

    <div v-if="isLoading" class="flex min-h-64 items-center justify-center text-sm text-[#888]">
      <LoaderCircle :size="20" class="mr-3 animate-spin text-[#16C65A]" /> Loading statistics
    </div>

    <template v-else-if="stats">
      <!-- Pending requests highlight -->
      <RouterLink
        :to="{ name: 'admin-artist-requests' }"
        class="mb-6 flex items-center justify-between gap-4 border p-5 transition"
        :class="stats.pendingArtistRequests > 0
          ? 'border-[#16C65A]/30 bg-[#16C65A]/10 hover:bg-[#16C65A]/15'
          : 'border-white/10 bg-[#111827] hover:border-white/20'"
      >
        <div class="flex items-center gap-4">
          <span
            class="grid size-12 shrink-0 place-items-center rounded-full"
            :class="stats.pendingArtistRequests > 0 ? 'bg-[#16C65A] text-black' : 'bg-white/10 text-[#888]'"
          >
            <UserCheck :size="22" />
          </span>
          <div>
            <p class="text-2xl font-black text-white">{{ stats.pendingArtistRequests }}</p>
            <p class="text-sm text-[#aaa]">Artist requests waiting for review</p>
          </div>
        </div>
        <span class="text-xs font-bold text-[#16C65A]">Review →</span>
      </RouterLink>

      <!-- Stat cards -->
      <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <div
          v-for="card in cards"
          :key="card.label"
          class="border border-white/10 bg-[#111827] p-5"
        >
          <div class="flex items-center justify-between">
            <p class="text-xs font-bold uppercase tracking-wide text-[#888]">{{ card.label }}</p>
            <component :is="card.icon" :size="18" :class="card.tint" />
          </div>
          <p class="mt-3 text-3xl font-black text-white">{{ card.value }}</p>
        </div>
      </div>

      <!-- Charts -->
      <div v-if="analytics" class="mt-8 grid gap-4 lg:grid-cols-2">
        <!-- User growth -->
        <div class="border border-white/10 bg-[#111827] p-5">
          <p class="mb-3 text-xs font-bold uppercase tracking-wide text-[#888]">New users (last 30 days)</p>
          <StatChart type="area" :series="userGrowthSeries" :categories="userGrowthCategories" />
        </div>

        <!-- Listens by day -->
        <div class="border border-white/10 bg-[#111827] p-5">
          <p class="mb-3 text-xs font-bold uppercase tracking-wide text-[#888]">Listens (last 30 days)</p>
          <StatChart type="bar" :series="listensSeries" :categories="listensCategories" />
        </div>

        <!-- Users by role -->
        <div v-if="hasSeries(usersByRoleSeries)" class="border border-white/10 bg-[#111827] p-5">
          <p class="mb-3 text-xs font-bold uppercase tracking-wide text-[#888]">Users by role</p>
          <StatChart type="donut" :series="usersByRoleSeries" :labels="usersByRoleLabels" />
        </div>

        <!-- Songs by status -->
        <div v-if="hasSeries(songStatusSeries)" class="border border-white/10 bg-[#111827] p-5">
          <p class="mb-3 text-xs font-bold uppercase tracking-wide text-[#888]">Songs by status</p>
          <StatChart type="donut" :series="songStatusSeries" :labels="songStatusLabels" />
        </div>

        <!-- Artist request funnel -->
        <div v-if="hasSeries(funnelSeries[0].data)" class="border border-white/10 bg-[#111827] p-5">
          <p class="mb-3 text-xs font-bold uppercase tracking-wide text-[#888]">Artist requests</p>
          <StatChart
            type="bar"
            :series="funnelSeries"
            :categories="funnelCategories"
            :extra-options="{ plotOptions: { bar: { distributed: true } }, legend: { show: false } }"
          />
        </div>

        <!-- Top songs -->
        <div v-if="hasSeries(topSongsSeries[0].data)" class="border border-white/10 bg-[#111827] p-5 lg:col-span-2">
          <p class="mb-3 text-xs font-bold uppercase tracking-wide text-[#888]">Top songs by plays</p>
          <StatChart
            type="bar"
            :height="360"
            :series="topSongsSeries"
            :categories="topSongsCategories"
            :extra-options="{ plotOptions: { bar: { horizontal: true, borderRadius: 4 } } }"
          />
        </div>
      </div>
    </template>
  </div>
</template>
