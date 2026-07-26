<script setup>
import { computed, onMounted, ref } from 'vue';
import { CheckCircle2, LoaderCircle, RefreshCw, ShieldCheck, UserRound, XCircle } from '@lucide/vue';
import { adminService } from '../../services/adminService';

const TABS = [
  { key: 'PENDING', label: 'Pending' },
  { key: 'APPROVED', label: 'Approved' },
  { key: 'REJECTED', label: 'Rejected' }
];

const activeTab = ref('PENDING');
const requests = ref([]);
const total = ref(0);
const isLoading = ref(true);
const error = ref('');
const actingId = ref(null); // id currently being approved/rejected

const isEmpty = computed(() => !isLoading.value && requests.value.length === 0);

async function load() {
  isLoading.value = true;
  error.value = '';
  try {
    const paged = await adminService.listArtistRequests({ status: activeTab.value, page: 1, size: 50 });
    requests.value = paged?.items || [];
    total.value = paged?.total || 0;
  } catch (requestError) {
    error.value = requestError.message || 'Unable to load artist requests.';
  } finally {
    isLoading.value = false;
  }
}

function switchTab(tab) {
  if (activeTab.value === tab) return;
  activeTab.value = tab;
  load();
}

async function approve(item) {
  if (actingId.value) return;
  actingId.value = item.id;
  error.value = '';
  try {
    await adminService.approveArtistRequest(item.id);
    await load();
  } catch (requestError) {
    error.value = requestError.message || 'Unable to approve this request.';
  } finally {
    actingId.value = null;
  }
}

async function reject(item) {
  if (actingId.value) return;
  const note = window.prompt(`Reject "${item.artistName}"? Optionally add a reason:`, '');
  if (note === null) return; // cancelled
  actingId.value = item.id;
  error.value = '';
  try {
    await adminService.rejectArtistRequest(item.id, note.trim() || null);
    await load();
  } catch (requestError) {
    error.value = requestError.message || 'Unable to reject this request.';
  } finally {
    actingId.value = null;
  }
}

function formatDate(value) {
  if (!value) return '';
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(
    new Date(value)
  );
}

onMounted(load);
</script>

<template>
  <div class="mx-auto w-full max-w-6xl px-5 py-8 pb-12 sm:px-8">
    <div class="mb-6 flex items-center gap-3">
      <ShieldCheck :size="28" class="text-[#1DB954]" />
      <div>
        <p class="sonix-kicker">ADMIN</p>
        <h1 class="sonix-section-title">Artist Requests</h1>
      </div>
    </div>

    <!-- Tabs + refresh -->
    <div class="mb-6 flex flex-wrap items-center justify-between gap-3">
      <div class="flex gap-2">
        <button
          v-for="tab in TABS"
          :key="tab.key"
          class="h-9 rounded-full px-4 text-xs font-bold transition"
          :class="activeTab === tab.key
            ? 'bg-[#1DB954] text-black'
            : 'border border-white/15 text-[#bbb] hover:border-[#1DB954]/70 hover:text-white'"
          @click="switchTab(tab.key)"
        >
          {{ tab.label }}
        </button>
      </div>
      <button
        class="inline-flex h-9 items-center gap-2 rounded-full border border-white/15 px-4 text-xs font-bold text-[#bbb] transition hover:border-[#1DB954]/70 hover:text-white disabled:opacity-50"
        :disabled="isLoading"
        @click="load"
      >
        <RefreshCw :size="14" :class="{ 'animate-spin': isLoading }" /> Refresh
      </button>
    </div>

    <p v-if="error" class="mb-4 rounded-md bg-red-500/10 px-3 py-2 text-xs text-red-300" role="alert">
      {{ error }}
    </p>

    <div v-if="isLoading" class="flex min-h-64 items-center justify-center text-sm text-[#888]">
      <LoaderCircle :size="20" class="mr-3 animate-spin text-[#1DB954]" /> Loading requests
    </div>

    <div v-else-if="isEmpty" class="flex min-h-48 items-center justify-center border border-white/10 bg-[#121212] text-sm text-[#888]">
      No {{ activeTab.toLowerCase() }} requests.
    </div>

    <ul v-else class="space-y-3">
      <li
        v-for="item in requests"
        :key="item.id"
        class="flex flex-col gap-4 border border-white/10 bg-[#121212] p-4 sm:flex-row sm:items-center sm:p-5"
      >
        <!-- Avatar -->
        <div class="size-16 shrink-0 overflow-hidden rounded-lg bg-white/[0.04]">
          <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.artistName" class="h-full w-full object-cover" />
          <span v-else class="grid h-full w-full place-items-center text-[#555]">
            <UserRound :size="24" />
          </span>
        </div>

        <!-- Info -->
        <div class="min-w-0 flex-1">
          <p class="truncate text-base font-black text-white">{{ item.artistName }}</p>
          <p class="mt-0.5 truncate text-xs text-[#888]">/artist/{{ item.slug }}</p>
          <p class="mt-1 truncate text-xs text-[#aaa]">
            Requested by
            <span class="text-[#ddd]">{{ item.requesterDisplayName || item.requesterUsername }}</span>
            <span class="text-[#666]"> · {{ item.requesterEmail }}</span>
          </p>
          <p v-if="item.bio" class="mt-2 line-clamp-2 text-xs leading-5 text-[#999]">{{ item.bio }}</p>
          <p class="mt-1 text-[11px] text-[#666]">Submitted {{ formatDate(item.createdAt) }}</p>
        </div>

        <!-- Actions (only for pending) -->
        <div v-if="activeTab === 'PENDING'" class="flex shrink-0 gap-2">
          <button
            class="inline-flex h-9 items-center gap-2 rounded-full bg-[#1DB954] px-4 text-xs font-black text-black transition hover:bg-[#20ca5c] disabled:opacity-50"
            :disabled="actingId !== null"
            @click="approve(item)"
          >
            <LoaderCircle v-if="actingId === item.id" :size="14" class="animate-spin" />
            <CheckCircle2 v-else :size="14" /> Approve
          </button>
          <button
            class="inline-flex h-9 items-center gap-2 rounded-full border border-red-400/40 px-4 text-xs font-bold text-red-300 transition hover:bg-red-400/10 disabled:opacity-50"
            :disabled="actingId !== null"
            @click="reject(item)"
          >
            <XCircle :size="14" /> Reject
          </button>
        </div>
      </li>
    </ul>
  </div>
</template>
