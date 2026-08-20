<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import {
  BarChart3,
  Heart,
  LayoutDashboard,
  LoaderCircle,
  Music2,
  RefreshCw,
  Users
} from '@lucide/vue';
import { studioService } from '../../services/studioService';
import StatChart from '../admin/components/StatChart.vue';

const route = useRoute();
const artistId = Number(route.params.artistId);

const stats = ref(null);
const analytics = ref(null);
const isLoading = ref(true);
const error = ref('');

const cards = computed(() => {
  const s = stats.value;
  if (!s) return [];
  return [
    { label: 'Total Songs', value: s.totalSongs, icon: Music2, tint: 'text-[#16C65A]' },
    { label: 'Total Plays', value: (s.totalPlays || 0).toLocaleString(), icon: BarChart3, tint: 'text-sky-300' },
    { label: 'Total Likes', value: (s.totalLikes || 0).toLocaleString(), icon: Heart, tint: 'text-rose-300' },
    { label: 'Total Followers', value: (s.totalFollowers || 0).toLocaleString(), icon: Users, tint: 'text-violet-300' },
    {
      label: 'Status Breakdown',
      value: `${s.publishedSongs || 0} / ${s.draftSongs || 0} / ${s.hiddenSongs || 0}`,
      sub: 'Pub / Draft / Hidden',
      icon: Music2,
      tint: 'text-amber-300'
    }
  ];
});

// Shorten ISO dates (yyyy-MM-dd) to MM-dd for compact axis labels.
function shortDate(iso) {
  return typeof iso === 'string' && iso.length === 10 ? iso.slice(5) : iso;
}

// --- Listens by day (area) ---
const listensCategories = computed(() =>
  (analytics.value?.listensByDay || []).map((d) => shortDate(d.date))
);
const listensSeries = computed(() => [
  { name: 'Listens', data: (analytics.value?.listensByDay || []).map((d) => d.count) }
]);

// --- Likes by day (bar) ---
const likesCategories = computed(() =>
  (analytics.value?.likesByDay || []).map((d) => shortDate(d.date))
);
const likesSeries = computed(() => [
  { name: 'Likes', data: (analytics.value?.likesByDay || []).map((d) => d.count) }
]);

// --- Top songs (horizontal bar) ---
const topSongsCategories = computed(() =>
  (analytics.value?.topSongs || []).map((s) => s.title)
);
const topSongsSeries = computed(() => [
  { name: 'Plays', data: (analytics.value?.topSongs || []).map((s) => s.playCount) }
]);

// --- Songs by status (donut) ---
const statusLabels = computed(() =>
  (analytics.value?.songsByStatus || []).map((r) => r.label)
);
const statusSeries = computed(() =>
  (analytics.value?.songsByStatus || []).map((r) => r.count)
);

const hasSeries = (arr) => Array.isArray(arr) && arr.some((v) => v > 0);

async function load() {
  isLoading.value = true;
  error.value = '';
  try {
    const [statsResult, analyticsResult] = await Promise.all([
      studioService.getStats(artistId),
      studioService.getAnalytics(artistId)
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
          <p class="melodyhub-kicker">STUDIO</p>
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
      <LoaderCircle :size="20" class="mr-3 animate-spin text-[#16C65A]" /> Loading your stats
    </div>

    <template v-else-if="stats">
      <!-- Stat cards -->
      <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
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
          <p v-if="card.sub" class="mt-1 text-[10px] text-[#71717A]">{{ card.sub }}</p>
        </div>
      </div>

      <!-- Charts -->
      <div v-if="analytics" class="mt-8 grid gap-4 lg:grid-cols-2">
        <!-- Listens by day -->
        <div class="border border-white/10 bg-[#111827] p-5">
          <p class="mb-3 text-xs font-bold uppercase tracking-wide text-[#888]">Listens (last 30 days)</p>
          <StatChart type="area" :series="listensSeries" :categories="listensCategories" />
        </div>

        <!-- Likes by day -->
        <div class="border border-white/10 bg-[#111827] p-5">
          <p class="mb-3 text-xs font-bold uppercase tracking-wide text-[#888]">Likes (last 30 days)</p>
          <StatChart type="bar" :series="likesSeries" :categories="likesCategories" />
        </div>

        <!-- Songs by status -->
        <div v-if="hasSeries(statusSeries)" class="border border-white/10 bg-[#111827] p-5">
          <p class="mb-3 text-xs font-bold uppercase tracking-wide text-[#888]">Songs by status</p>
          <StatChart type="donut" :series="statusSeries" :labels="statusLabels" />
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
