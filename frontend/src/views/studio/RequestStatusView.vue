<script setup>
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { ArrowRight, CheckCircle2, Clock3, FileClock, LoaderCircle, RefreshCw, XCircle } from '@lucide/vue';
import { artistAccessService } from '../../services/artistAccessService';
import { useStudioStore } from '../../stores/studio.store';

const studioStore = useStudioStore();

const requests = ref([]);
const isLoading = ref(true);
const error = ref('');

const isEmpty = computed(() => !isLoading.value && requests.value.length === 0);

function statusBadge(status) {
  if (status === 'PENDING') return 'bg-amber-500/15 text-amber-300';
  if (status === 'APPROVED') return 'bg-[#16C65A]/15 text-[#16C65A]';
  return 'bg-red-400/15 text-red-300';
}

function typeLabel(type) {
  return type === 'CLAIM_ARTIST' ? 'Claim artist' : 'Create artist';
}

function formatDate(value) {
  if (!value) return '';
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(
    new Date(value)
  );
}

async function load() {
  isLoading.value = true;
  error.value = '';
  try {
    requests.value = await artistAccessService.getMyRequests();
  } catch (requestError) {
    error.value = requestError.message || 'Unable to load your requests.';
  } finally {
    isLoading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="mx-auto w-full max-w-3xl px-5 py-12 pb-20 sm:px-8">
    <div class="mb-8 flex items-center justify-between gap-3">
      <div>
        <p class="melodyhub-kicker">ARTIST STUDIO</p>
        <h1 class="melodyhub-section-title">Request Status</h1>
        <p class="mt-3 text-sm leading-6 text-[#999]">Track your artist access requests.</p>
      </div>
      <button
        class="inline-flex h-9 shrink-0 items-center gap-2 rounded-full border border-white/15 px-4 text-xs font-bold text-[#bbb] transition hover:border-[#16C65A]/70 hover:text-white disabled:opacity-50"
        :disabled="isLoading"
        @click="load"
      >
        <RefreshCw :size="14" :class="{ 'animate-spin': isLoading }" /> Refresh
      </button>
    </div>

    <p v-if="error" class="mb-4 rounded-md bg-red-500/10 px-3 py-2 text-xs text-red-300" role="alert">{{ error }}</p>

    <div v-if="isLoading" class="flex min-h-48 items-center justify-center text-sm text-[#888]">
      <LoaderCircle :size="20" class="mr-3 animate-spin text-[#16C65A]" /> Loading requests
    </div>

    <div v-else-if="isEmpty" class="flex min-h-48 flex-col items-center justify-center gap-4 border border-white/10 bg-[#111827] text-center">
      <FileClock :size="30" class="text-[#16C65A]" />
      <p class="text-sm text-[#999]">You haven't submitted any artist access requests yet.</p>
      <RouterLink
        :to="{ name: 'studio-access' }"
        class="inline-flex h-10 items-center gap-2 rounded-full bg-[#16C65A] px-5 text-xs font-black text-black transition hover:bg-[#22C55E]"
      >
        Get artist access <ArrowRight :size="15" />
      </RouterLink>
    </div>

    <ul v-else class="space-y-3">
      <li
        v-for="request in requests"
        :key="request.id"
        class="border border-white/10 bg-[#111827] p-5"
      >
        <div class="flex flex-wrap items-center gap-3">
          <span class="rounded-full px-2.5 py-1 text-[11px] font-bold" :class="statusBadge(request.status)">
            {{ request.status }}
          </span>
          <span class="rounded-full bg-white/10 px-2.5 py-1 text-[11px] font-bold text-[#bbb]">
            {{ typeLabel(request.requestType) }}
          </span>
          <span v-if="request.relationship" class="text-[11px] text-[#71717A]">{{ request.relationship }}</span>
        </div>

        <p class="mt-3 text-base font-bold text-white">
          {{ request.existingArtistName || request.requestedArtistName || 'Artist request' }}
        </p>

        <p class="mt-1 flex items-center gap-1.5 text-xs text-[#71717A]">
          <Clock3 :size="13" /> Submitted {{ formatDate(request.createdAt) }}
        </p>

        <p v-if="request.reviewNote" class="mt-3 rounded-md bg-white/[0.03] px-3 py-2 text-xs leading-5 text-[#bbb]">
          <span class="font-bold text-red-300">Review note:</span> {{ request.reviewNote }}
        </p>
      </li>
    </ul>

    <div class="mt-8 flex flex-wrap gap-4 border-t border-white/5 pt-6">
      <RouterLink
        v-if="studioStore.myArtists.length > 0"
        :to="{ name: 'studio-entry' }"
        class="inline-flex items-center gap-2 text-sm font-bold text-[#16C65A] hover:underline"
      >
        <CheckCircle2 :size="16" /> Go to my artist studio
      </RouterLink>
      <RouterLink
        :to="{ name: 'studio-access' }"
        class="inline-flex items-center gap-2 text-sm font-bold text-[#bbb] transition hover:text-[#16C65A]"
      >
        Request access to another artist
      </RouterLink>
    </div>
  </div>
</template>