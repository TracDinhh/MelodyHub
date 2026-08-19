<script setup>
import { computed, onMounted, ref } from 'vue';
import {
  AlertTriangle,
  CheckCircle,
  ChevronLeft,
  ChevronRight,
  Eye,
  EyeOff,
  LoaderCircle,
  Music2,
  RefreshCw,
  Search,
  Trash2,
  X
} from '@lucide/vue';
import { adminService } from '../../services/adminService';

const STATUS_TABS = [
  { key: '', label: 'All' },
  { key: 'PUBLISHED', label: 'Published' },
  { key: 'HIDDEN', label: 'Hidden' },
  { key: 'DRAFT', label: 'Draft' }
];

const STATUS_BADGE = {
  PUBLISHED: 'bg-emerald-400/15 text-emerald-300',
  HIDDEN: 'bg-amber-400/15 text-amber-300',
  DRAFT: 'bg-white/10 text-[#999]'
};

const activeStatus = ref('');
const search = ref('');
const songs = ref([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(20);
const isLoading = ref(true);
const error = ref('');

// Confirmation modal
const confirmModal = ref(null); // { songId, songTitle, action, newStatus }
const actionLoading = ref(false);

// Toast
const toast = ref(null); // { type: 'success' | 'error', message }
let toastTimer = null;

const isEmpty = computed(() => !isLoading.value && songs.value.length === 0);
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)));
const canPrev = computed(() => currentPage.value > 1);
const canNext = computed(() => currentPage.value < totalPages.value);

function showToast(type, message) {
  clearTimeout(toastTimer);
  toast.value = { type, message };
  toastTimer = setTimeout(() => { toast.value = null; }, 3000);
}

async function load() {
  isLoading.value = true;
  error.value = '';
  try {
    const paged = await adminService.listSongs({
      status: activeStatus.value,
      q: search.value.trim(),
      page: currentPage.value,
      size: pageSize.value
    });
    songs.value = paged?.items || [];
    total.value = paged?.total || 0;
  } catch (requestError) {
    error.value = requestError.message || 'Unable to load songs.';
  } finally {
    isLoading.value = false;
  }
}

function switchStatus(key) {
  if (activeStatus.value === key) return;
  activeStatus.value = key;
  currentPage.value = 1;
  load();
}

function goPage(p) {
  if (p < 1 || p > totalPages.value || p === currentPage.value) return;
  currentPage.value = p;
  load();
}

function formatDate(value) {
  if (!value) return '';
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(value));
}

function formatDuration(sec) {
  if (!sec) return '0:00';
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}

function formatPlays(count) {
  if (!count) return '0';
  if (count >= 1000000) return (count / 1000000).toFixed(1) + 'M';
  if (count >= 1000) return (count / 1000).toFixed(1) + 'K';
  return count.toLocaleString();
}

function artistNames(song) {
  if (!song.artists || song.artists.length === 0) return 'Unknown';
  return song.artists.map(a => a.name).join(', ');
}

// --- Actions ---
function openStatusChange(song, newStatus) {
  const actionLabel = newStatus === 'PUBLISHED' ? 'Publish' : newStatus === 'HIDDEN' ? 'Hide' : 'Draft';
  confirmModal.value = {
    songId: song.id,
    songTitle: song.title,
    action: actionLabel,
    newStatus,
    type: 'status'
  };
}

function openDelete(song) {
  confirmModal.value = {
    songId: song.id,
    songTitle: song.title,
    action: 'Delete',
    type: 'delete'
  };
}

function closeModal() {
  confirmModal.value = null;
}

async function confirmAction() {
  if (!confirmModal.value || actionLoading.value) return;
  actionLoading.value = true;

  try {
    if (confirmModal.value.type === 'status') {
      await adminService.updateSongStatus(confirmModal.value.songId, confirmModal.value.newStatus);
      showToast('success', `"${confirmModal.value.songTitle}" → ${confirmModal.value.newStatus}`);
    } else {
      await adminService.deleteSong(confirmModal.value.songId);
      showToast('success', `"${confirmModal.value.songTitle}" deleted`);
    }
    closeModal();
    await load();
  } catch (e) {
    showToast('error', e.message || 'Action failed');
  } finally {
    actionLoading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="mx-auto w-full max-w-6xl px-5 py-8 pb-12 sm:px-8">
    <!-- Header -->
    <div class="mb-6 flex items-center gap-3">
      <Music2 :size="28" class="text-[#16C65A]" />
      <div>
        <p class="melodyhub-kicker">ADMIN</p>
        <h1 class="melodyhub-section-title">Songs <span class="text-sm font-normal text-[#666]">({{ total }})</span></h1>
      </div>
    </div>

    <!-- Filters -->
    <div class="mb-6 flex flex-wrap items-center justify-between gap-3">
      <div class="flex flex-wrap gap-2">
        <button
          v-for="tab in STATUS_TABS"
          :key="tab.key"
          class="h-9 rounded-full px-4 text-xs font-bold transition"
          :class="activeStatus === tab.key
            ? 'bg-[#16C65A] text-black'
            : 'border border-white/15 text-[#bbb] hover:border-[#16C65A]/70 hover:text-white'"
          @click="switchStatus(tab.key)"
        >
          {{ tab.label }}
        </button>
      </div>
      <div class="flex gap-2">
        <label class="flex h-9 items-center gap-2 rounded-full bg-white/5 px-3 ring-1 ring-white/10 focus-within:ring-[#16C65A]/60">
          <Search :size="15" class="text-[#888]" />
          <input
            v-model="search"
            class="w-40 bg-transparent text-sm text-white outline-none placeholder:text-[#666]"
            placeholder="Search songs"
            @keyup.enter="currentPage = 1; load()"
          />
        </label>
        <button
          class="inline-flex h-9 items-center gap-2 rounded-full border border-white/15 px-4 text-xs font-bold text-[#bbb] transition hover:border-[#16C65A]/70 hover:text-white disabled:opacity-50"
          :disabled="isLoading"
          @click="load"
        >
          <RefreshCw :size="14" :class="{ 'animate-spin': isLoading }" /> Refresh
        </button>
      </div>
    </div>

    <!-- Error -->
    <p v-if="error" class="mb-4 rounded-md bg-red-500/10 px-3 py-2 text-xs text-red-300" role="alert">{{ error }}</p>

    <!-- Loading -->
    <div v-if="isLoading" class="flex min-h-64 items-center justify-center text-sm text-[#888]">
      <LoaderCircle :size="20" class="mr-3 animate-spin text-[#16C65A]" /> Loading songs
    </div>

    <!-- Empty -->
    <div v-else-if="isEmpty" class="flex min-h-48 items-center justify-center border border-white/10 bg-[#111827] text-sm text-[#888]">
      No songs found.
    </div>

    <!-- Table -->
    <div v-else class="overflow-x-auto border border-white/10 bg-[#111827]">
      <table class="w-full min-w-[800px] text-left text-sm">
        <thead class="border-b border-white/10 text-[11px] uppercase tracking-wide text-[#777]">
          <tr>
            <th class="px-4 py-3 font-bold">Song</th>
            <th class="px-4 py-3 font-bold">Artist</th>
            <th class="px-4 py-3 font-bold text-center">Duration</th>
            <th class="px-4 py-3 font-bold text-center">Plays</th>
            <th class="px-4 py-3 font-bold text-center">Status</th>
            <th class="px-4 py-3 font-bold">Created</th>
            <th class="px-4 py-3 font-bold text-center">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="song in songs" :key="song.id" class="border-b border-white/5 last:border-0 hover:bg-white/[0.02]">
            <!-- Song -->
            <td class="px-4 py-3">
              <div class="flex items-center gap-3">
                <img
                  v-if="song.coverUrl"
                  :src="song.coverUrl"
                  :alt="song.title"
                  class="size-10 shrink-0 rounded-md object-cover"
                />
                <div v-else class="grid size-10 shrink-0 place-items-center rounded-md bg-white/10">
                  <Music2 :size="16" class="text-[#666]" />
                </div>
                <div class="min-w-0">
                  <p class="truncate font-bold text-white">{{ song.title }}</p>
                  <p class="truncate text-xs text-[#777]">{{ song.lyricsType }}</p>
                </div>
              </div>
            </td>
            <!-- Artist -->
            <td class="px-4 py-3 text-[#bbb]">
              <span class="line-clamp-1">{{ artistNames(song) }}</span>
            </td>
            <!-- Duration -->
            <td class="px-4 py-3 text-center text-[#999]">{{ formatDuration(song.durationSec) }}</td>
            <!-- Plays -->
            <td class="px-4 py-3 text-center font-mono text-[#bbb]">{{ formatPlays(song.playCount) }}</td>
            <!-- Status -->
            <td class="px-4 py-3 text-center">
              <span class="rounded-full px-2.5 py-1 text-[11px] font-bold" :class="STATUS_BADGE[song.status] || 'bg-white/10 text-[#ccc]'">
                {{ song.status }}
              </span>
            </td>
            <!-- Created -->
            <td class="px-4 py-3 text-xs text-[#888]">{{ formatDate(song.createdAt) }}</td>
            <!-- Actions -->
            <td class="px-4 py-3">
              <div class="flex items-center justify-center gap-1">
                <!-- Publish -->
                <button
                  v-if="song.status !== 'PUBLISHED'"
                  class="grid size-8 place-items-center rounded-md text-emerald-400 transition hover:bg-emerald-400/15"
                  title="Publish"
                  @click="openStatusChange(song, 'PUBLISHED')"
                >
                  <Eye :size="15" />
                </button>
                <!-- Hide -->
                <button
                  v-if="song.status !== 'HIDDEN'"
                  class="grid size-8 place-items-center rounded-md text-amber-400 transition hover:bg-amber-400/15"
                  title="Hide"
                  @click="openStatusChange(song, 'HIDDEN')"
                >
                  <EyeOff :size="15" />
                </button>
                <!-- Delete -->
                <button
                  class="grid size-8 place-items-center rounded-md text-red-400 transition hover:bg-red-400/15"
                  title="Delete"
                  @click="openDelete(song)"
                >
                  <Trash2 :size="15" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="mt-4 flex items-center justify-between text-xs text-[#888]">
      <span>Page {{ currentPage }} of {{ totalPages }}</span>
      <div class="flex gap-1">
        <button
          class="grid size-8 place-items-center rounded-md border border-white/10 transition hover:border-white/30 disabled:opacity-30"
          :disabled="!canPrev"
          @click="goPage(currentPage - 1)"
        >
          <ChevronLeft :size="15" />
        </button>
        <button
          class="grid size-8 place-items-center rounded-md border border-white/10 transition hover:border-white/30 disabled:opacity-30"
          :disabled="!canNext"
          @click="goPage(currentPage + 1)"
        >
          <ChevronRight :size="15" />
        </button>
      </div>
    </div>

    <!-- Confirm Modal -->
    <Teleport to="body">
      <div v-if="confirmModal" class="fixed inset-0 z-[9999] flex items-center justify-center bg-black/70 backdrop-blur-sm" @click.self="closeModal">
        <div class="relative w-full max-w-md rounded-2xl border border-white/10 bg-[#111827] p-6 shadow-2xl">
          <button class="absolute right-4 top-4 text-[#888] hover:text-white" @click="closeModal"><X :size="18" /></button>

          <div class="mb-4 flex items-center gap-3">
            <div
              class="grid size-10 place-items-center rounded-full"
              :class="confirmModal.type === 'delete' ? 'bg-red-500/15 text-red-400' : 'bg-amber-500/15 text-amber-400'"
            >
              <AlertTriangle :size="20" />
            </div>
            <h3 class="text-lg font-bold text-white">
              {{ confirmModal.action }} song?
            </h3>
          </div>

          <p class="mb-6 text-sm text-[#aaa]">
            <template v-if="confirmModal.type === 'delete'">
              This will permanently delete "<span class="font-bold text-white">{{ confirmModal.songTitle }}</span>".
              This action cannot be undone.
            </template>
            <template v-else>
              Change "<span class="font-bold text-white">{{ confirmModal.songTitle }}</span>" status to
              <span class="font-bold" :class="confirmModal.newStatus === 'PUBLISHED' ? 'text-emerald-300' : 'text-amber-300'">{{ confirmModal.newStatus }}</span>?
            </template>
          </p>

          <div class="flex justify-end gap-3">
            <button
              class="h-9 rounded-full border border-white/15 px-5 text-xs font-bold text-[#bbb] transition hover:border-white/30 hover:text-white"
              :disabled="actionLoading"
              @click="closeModal"
            >
              Cancel
            </button>
            <button
              class="h-9 rounded-full px-5 text-xs font-bold text-white transition disabled:opacity-50"
              :class="confirmModal.type === 'delete' ? 'bg-red-500 hover:bg-red-600' : 'bg-[#16C65A] hover:bg-[#13a84d]'"
              :disabled="actionLoading"
              @click="confirmAction"
            >
              <LoaderCircle v-if="actionLoading" :size="14" class="mr-1 inline animate-spin" />
              {{ confirmModal.action }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Toast -->
    <Teleport to="body">
      <Transition
        enter-active-class="transition duration-300 ease-out"
        enter-from-class="translate-y-4 opacity-0"
        enter-to-class="translate-y-0 opacity-100"
        leave-active-class="transition duration-200 ease-in"
        leave-from-class="translate-y-0 opacity-100"
        leave-to-class="translate-y-4 opacity-0"
      >
        <div
          v-if="toast"
          class="fixed bottom-6 right-6 z-[9999] flex items-center gap-2 rounded-xl border px-4 py-3 text-sm font-medium shadow-xl"
          :class="toast.type === 'success'
            ? 'border-emerald-500/30 bg-emerald-950/90 text-emerald-300'
            : 'border-red-500/30 bg-red-950/90 text-red-300'"
        >
          <CheckCircle v-if="toast.type === 'success'" :size="16" />
          <AlertTriangle v-else :size="16" />
          {{ toast.message }}
        </div>
      </Transition>
    </Teleport>
  </div>
</template>
