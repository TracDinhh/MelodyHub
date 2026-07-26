<script setup>
import { computed, onMounted, ref } from 'vue';
import { LoaderCircle, RefreshCw, Search, Users } from '@lucide/vue';
import { adminService } from '../../services/adminService';

const ROLE_TABS = [
  { key: '', label: 'All' },
  { key: 'USER', label: 'Users' },
  { key: 'ARTIST', label: 'Artists' },
  { key: 'ADMIN', label: 'Admins' }
];

const ROLE_BADGE = {
  USER: 'bg-white/10 text-[#ccc]',
  ARTIST: 'bg-[#1DB954]/15 text-[#1DB954]',
  ADMIN: 'bg-amber-400/15 text-amber-300'
};

const activeRole = ref('');
const search = ref('');
const users = ref([]);
const total = ref(0);
const isLoading = ref(true);
const error = ref('');

const isEmpty = computed(() => !isLoading.value && users.value.length === 0);

async function load() {
  isLoading.value = true;
  error.value = '';
  try {
    const paged = await adminService.listUsers({ role: activeRole.value, q: search.value.trim(), size: 50 });
    users.value = paged?.items || [];
    total.value = paged?.total || 0;
  } catch (requestError) {
    error.value = requestError.message || 'Unable to load users.';
  } finally {
    isLoading.value = false;
  }
}

function switchRole(role) {
  if (activeRole.value === role) return;
  activeRole.value = role;
  load();
}

function formatDate(value) {
  if (!value) return '';
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(value));
}

onMounted(load);
</script>

<template>
  <div class="mx-auto w-full max-w-6xl px-5 py-8 pb-12 sm:px-8">
    <div class="mb-6 flex items-center gap-3">
      <Users :size="28" class="text-[#1DB954]" />
      <div>
        <p class="sonix-kicker">ADMIN</p>
        <h1 class="sonix-section-title">Users <span class="text-sm font-normal text-[#666]">({{ total }})</span></h1>
      </div>
    </div>

    <div class="mb-6 flex flex-wrap items-center justify-between gap-3">
      <div class="flex flex-wrap gap-2">
        <button
          v-for="tab in ROLE_TABS"
          :key="tab.key"
          class="h-9 rounded-full px-4 text-xs font-bold transition"
          :class="activeRole === tab.key
            ? 'bg-[#1DB954] text-black'
            : 'border border-white/15 text-[#bbb] hover:border-[#1DB954]/70 hover:text-white'"
          @click="switchRole(tab.key)"
        >
          {{ tab.label }}
        </button>
      </div>
      <div class="flex gap-2">
        <label class="flex h-9 items-center gap-2 rounded-full bg-white/5 px-3 ring-1 ring-white/10 focus-within:ring-[#1DB954]/60">
          <Search :size="15" class="text-[#888]" />
          <input
            v-model="search"
            class="w-40 bg-transparent text-sm text-white outline-none placeholder:text-[#666]"
            placeholder="Search users"
            @keyup.enter="load"
          />
        </label>
        <button
          class="inline-flex h-9 items-center gap-2 rounded-full border border-white/15 px-4 text-xs font-bold text-[#bbb] transition hover:border-[#1DB954]/70 hover:text-white disabled:opacity-50"
          :disabled="isLoading"
          @click="load"
        >
          <RefreshCw :size="14" :class="{ 'animate-spin': isLoading }" /> Refresh
        </button>
      </div>
    </div>

    <p v-if="error" class="mb-4 rounded-md bg-red-500/10 px-3 py-2 text-xs text-red-300" role="alert">{{ error }}</p>

    <div v-if="isLoading" class="flex min-h-64 items-center justify-center text-sm text-[#888]">
      <LoaderCircle :size="20" class="mr-3 animate-spin text-[#1DB954]" /> Loading users
    </div>

    <div v-else-if="isEmpty" class="flex min-h-48 items-center justify-center border border-white/10 bg-[#121212] text-sm text-[#888]">
      No users found.
    </div>

    <div v-else class="overflow-x-auto border border-white/10 bg-[#121212]">
      <table class="w-full min-w-[640px] text-left text-sm">
        <thead class="border-b border-white/10 text-[11px] uppercase tracking-wide text-[#777]">
          <tr>
            <th class="px-4 py-3 font-bold">User</th>
            <th class="px-4 py-3 font-bold">Email</th>
            <th class="px-4 py-3 font-bold">Role</th>
            <th class="px-4 py-3 font-bold">Status</th>
            <th class="px-4 py-3 font-bold">Joined</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id" class="border-b border-white/5 last:border-0 hover:bg-white/[0.02]">
            <td class="px-4 py-3">
              <p class="font-bold text-white">{{ u.displayName || u.username }}</p>
              <p class="text-xs text-[#777]">@{{ u.username }}</p>
            </td>
            <td class="px-4 py-3 text-[#bbb]">{{ u.email }}</td>
            <td class="px-4 py-3">
              <span class="rounded-full px-2.5 py-1 text-[11px] font-bold" :class="ROLE_BADGE[u.role]">{{ u.role }}</span>
            </td>
            <td class="px-4 py-3">
              <span
                class="rounded-full px-2.5 py-1 text-[11px] font-bold"
                :class="u.status === 'BANNED' ? 'bg-red-400/15 text-red-300' : 'bg-white/10 text-[#ccc]'"
              >{{ u.status }}</span>
            </td>
            <td class="px-4 py-3 text-xs text-[#888]">{{ formatDate(u.createdAt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
