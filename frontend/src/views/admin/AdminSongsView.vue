<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import {
  AlertTriangle,
  ArrowDownUp,
  BadgeCheck,
  CheckCircle,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Clock,
  Eye,
  EyeOff,
  FileText,
  LoaderCircle,
  Music2,
  Play,
  RefreshCw,
  Search,
  Trash2,
  X,
  XCircle
} from '@lucide/vue';
import { adminService } from '../../services/adminService';

// ---- STATUS CONFIG ----
const STATUS_TABS = [
  { key: '', label: 'All', color: 'text-white' },
  { key: 'PUBLISHED', label: 'Published', color: 'text-emerald-300' },
  { key: 'SUBMITTED', label: 'Submitted', color: 'text-sky-300' },
  { key: 'REJECTED', label: 'Rejected', color: 'text-red-300' },
  { key: 'HIDDEN', label: 'Hidden', color: 'text-amber-300' },
  { key: 'DRAFT', label: 'Draft', color: 'text-[#999]' }
];

const STATUS_BADGE = {
  PUBLISHED: 'bg-emerald-400/15 text-emerald-300',
  SUBMITTED: 'bg-sky-400/15 text-sky-300',
  REJECTED: 'bg-red-400/15 text-red-300',
  HIDDEN: 'bg-amber-400/15 text-amber-300',
  DRAFT: 'bg-white/10 text-[#999]'
};

const SORT_OPTIONS = [
  { key: 'newest', label: 'Newest first' },
  { key: 'oldest', label: 'Oldest first' },
  { key: 'most_played', label: 'Most played' },
  { key: 'least_played', label: 'Least played' },
  { key: 'title_asc', label: 'Title A → Z' },
  { key: 'title_desc', label: 'Title Z → A' }
];

const PAGE_SIZES = [10, 20, 50];

// ---- STATE ----
const activeStatus = ref('');
const search = ref('');
const sortBy = ref('newest');
const songs = ref([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(20);
const isLoading = ref(true);
const error = ref('');
const statusCounts = ref({});
const expandedId = ref(null);

// Dropdown visibility
const sortOpen = ref(false);
const pageSizeOpen = ref(false);

// Modal
const confirmModal = ref(null);
const rejectModal = ref(null);
const rejectReason = ref('');
const rejectLoading = ref(false);
const actionLoading = ref(false);

// Toast
const toast = ref(null);
let toastTimer = null;

// ---- COMPUTED ----
const isEmpty = computed(() => !isLoading.value && songs.value.length === 0);
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)));
const canPrev = computed(() => currentPage.value > 1);
const canNext = computed(() => currentPage.value < totalPages.value);
const rangeStart = computed(() => total.value === 0 ? 0 : (currentPage.value - 1) * pageSize.value + 1);
const rangeEnd = computed(() => Math.min(currentPage.value * pageSize.value, total.value));

const totalSongs = computed(() => {
  return Object.values(statusCounts.value).reduce((a, b) => a + b, 0);
});

function statusCount(key) {
  if (!key) return totalSongs.value;
  return statusCounts.value[key] || 0;
}

// ---- HELPERS ----
function showToast(type, message) {
  clearTimeout(toastTimer);
  toast.value = { type, message };
  toastTimer = setTimeout(() => { toast.value = null; }, 3000);
}

function formatDate(value) {
  if (!value) return '—';
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

function toggleExpand(id) {
  expandedId.value = expandedId.value === id ? null : id;
}

// ---- DATA LOADING ----
async function loadCounts() {
  try {
    statusCounts.value = await adminService.getSongStatusCounts();
  } catch (e) { /* silent */ }
}

async function load() {
  isLoading.value = true;
  error.value = '';
  try {
    const paged = await adminService.listSongs({
      status: activeStatus.value,
      q: search.value.trim(),
      sort: sortBy.value,
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

function setSort(key) {
  sortBy.value = key;
  sortOpen.value = false;
  currentPage.value = 1;
  load();
}

function setPageSize(size) {
  pageSize.value = size;
  pageSizeOpen.value = false;
  currentPage.value = 1;
  load();
}

function goPage(p) {
  if (p < 1 || p > totalPages.value || p === currentPage.value) return;
  currentPage.value = p;
  load();
}

function doSearch() {
  currentPage.value = 1;
  load();
}

// ---- ACTIONS ----
function openStatusChange(song, newStatus) {
  const actionLabel = newStatus === 'PUBLISHED' ? 'Publish' : 'Hide';
  confirmModal.value = { songId: song.id, songTitle: song.title, action: actionLabel, newStatus, type: 'status' };
}

function openApprove(song) {
  confirmModal.value = { songId: song.id, songTitle: song.title, action: 'Approve', type: 'approve' };
}

function openReject(song) {
  rejectModal.value = { songId: song.id, songTitle: song.title };
  rejectReason.value = '';
}

function openDelete(song) {
  confirmModal.value = { songId: song.id, songTitle: song.title, action: 'Delete', type: 'delete' };
}

function closeModal() { confirmModal.value = null; }

function closeReject() { rejectModal.value = null; }

async function confirmAction() {
  if (!confirmModal.value || actionLoading.value) return;
  actionLoading.value = true;
  try {
    if (confirmModal.value.type === 'approve') {
      await adminService.approveSong(confirmModal.value.songId);
      showToast('success', `"${confirmModal.value.songTitle}" published`);
    } else if (confirmModal.value.type === 'status') {
      await adminService.updateSongStatus(confirmModal.value.songId, confirmModal.value.newStatus);
      showToast('success', `"${confirmModal.value.songTitle}" → ${confirmModal.value.newStatus}`);
    } else {
      await adminService.deleteSong(confirmModal.value.songId);
      showToast('success', `"${confirmModal.value.songTitle}" deleted`);
    }
    closeModal();
    await Promise.all([load(), loadCounts()]);
  } catch (e) {
    showToast('error', e.message || 'Action failed');
  } finally {
    actionLoading.value = false;
  }
}

async function confirmReject() {
  if (!rejectModal.value || rejectLoading.value) return;
  const note = rejectReason.value.trim();
  if (!note) {
    showToast('error', 'A review note is required.');
    return;
  }
  rejectLoading.value = true;
  try {
    await adminService.rejectSong(rejectModal.value.songId, note);
    showToast('success', `"${rejectModal.value.songTitle}" rejected`);
    closeReject();
    await Promise.all([load(), loadCounts()]);
  } catch (e) {
    showToast('error', e.message || 'Action failed');
  } finally {
    rejectLoading.value = false;
  }
}

// Close dropdowns on outside click
function onClickOutside(event) {
  if (sortOpen.value && !event.target.closest('#sort-dropdown')) sortOpen.value = false;
  if (pageSizeOpen.value && !event.target.closest('#pagesize-dropdown')) pageSizeOpen.value = false;
}

onMounted(() => {
  Promise.all([load(), loadCounts()]);
  document.addEventListener('click', onClickOutside);
});
</script>

<template>
  <div class="mx-auto w-full max-w-6xl px-5 py-8 pb-12 sm:px-8">
    <!-- Header -->
    <div class="mb-6 flex items-center justify-between gap-3">
      <div class="flex items-center gap-3">
        <Music2 :size="28" class="text-[#16C65A]" />
        <div>
          <p class="melodyhub-kicker">ADMIN</p>
          <h1 class="melodyhub-section-title">Song Management</h1>
        </div>
      </div>
      <button
        class="inline-flex h-9 items-center gap-2 rounded-full border border-white/15 px-4 text-xs font-bold text-[#bbb] transition hover:border-[#16C65A]/70 hover:text-white disabled:opacity-50"
        :disabled="isLoading"
        @click="Promise.all([load(), loadCounts()])"
      >
        <RefreshCw :size="14" :class="{ 'animate-spin': isLoading }" /> Refresh
      </button>
    </div>

    <!-- Status tabs with counts -->
    <div class="mb-5 flex flex-wrap gap-2">
      <button
        v-for="tab in STATUS_TABS"
        :key="tab.key"
        class="flex h-9 items-center gap-2 rounded-full px-4 text-xs font-bold transition"
        :class="activeStatus === tab.key
          ? 'bg-[#16C65A] text-black'
          : 'border border-white/15 text-[#bbb] hover:border-[#16C65A]/70 hover:text-white'"
        @click="switchStatus(tab.key)"
      >
        {{ tab.label }}
        <span
          class="rounded-full px-1.5 py-0.5 text-[10px] font-bold"
          :class="activeStatus === tab.key ? 'bg-black/20 text-black' : 'bg-white/10 text-[#888]'"
        >{{ statusCount(tab.key) }}</span>
      </button>
    </div>

    <!-- Toolbar: Search + Sort + PageSize -->
    <div class="mb-5 flex flex-wrap items-center gap-3">
      <!-- Search -->
      <label class="flex h-9 flex-1 items-center gap-2 rounded-full bg-white/5 px-3 ring-1 ring-white/10 focus-within:ring-[#16C65A]/60 sm:max-w-xs">
        <Search :size="15" class="shrink-0 text-[#888]" />
        <input
          v-model="search"
          class="min-w-0 flex-1 bg-transparent text-sm text-white outline-none placeholder:text-[#666]"
          placeholder="Search by title..."
          @keyup.enter="doSearch"
        />
      </label>

      <!-- Sort dropdown -->
      <div id="sort-dropdown" class="relative">
        <button
          class="flex h-9 items-center gap-2 rounded-full border border-white/15 px-3 text-xs font-medium text-[#bbb] transition hover:border-white/30 hover:text-white"
          @click.stop="sortOpen = !sortOpen"
        >
          <ArrowDownUp :size="14" />
          {{ SORT_OPTIONS.find(o => o.key === sortBy)?.label }}
          <ChevronDown :size="12" />
        </button>
        <Transition
          enter-active-class="transition duration-150 ease-out"
          enter-from-class="scale-95 opacity-0"
          enter-to-class="scale-100 opacity-100"
          leave-active-class="transition duration-100 ease-in"
          leave-from-class="scale-100 opacity-100"
          leave-to-class="scale-95 opacity-0"
        >
          <div v-if="sortOpen" class="absolute right-0 z-30 mt-1 w-44 overflow-hidden rounded-xl border border-white/10 bg-[#1a1a2e] shadow-2xl">
            <button
              v-for="opt in SORT_OPTIONS"
              :key="opt.key"
              class="flex w-full items-center px-3 py-2 text-xs text-[#bbb] transition hover:bg-white/5 hover:text-white"
              :class="{ '!text-[#16C65A] font-bold': sortBy === opt.key }"
              @click.stop="setSort(opt.key)"
            >
              {{ opt.label }}
            </button>
          </div>
        </Transition>
      </div>

      <!-- Page size dropdown -->
      <div id="pagesize-dropdown" class="relative">
        <button
          class="flex h-9 items-center gap-2 rounded-full border border-white/15 px-3 text-xs font-medium text-[#bbb] transition hover:border-white/30 hover:text-white"
          @click.stop="pageSizeOpen = !pageSizeOpen"
        >
          {{ pageSize }} / page
          <ChevronDown :size="12" />
        </button>
        <Transition
          enter-active-class="transition duration-150 ease-out"
          enter-from-class="scale-95 opacity-0"
          enter-to-class="scale-100 opacity-100"
          leave-active-class="transition duration-100 ease-in"
          leave-from-class="scale-100 opacity-100"
          leave-to-class="scale-95 opacity-0"
        >
          <div v-if="pageSizeOpen" class="absolute right-0 z-30 mt-1 w-28 overflow-hidden rounded-xl border border-white/10 bg-[#1a1a2e] shadow-2xl">
            <button
              v-for="s in PAGE_SIZES"
              :key="s"
              class="flex w-full items-center px-3 py-2 text-xs text-[#bbb] transition hover:bg-white/5 hover:text-white"
              :class="{ '!text-[#16C65A] font-bold': pageSize === s }"
              @click.stop="setPageSize(s)"
            >
              {{ s }} items
            </button>
          </div>
        </Transition>
      </div>
    </div>

    <!-- Error -->
    <p v-if="error" class="mb-4 rounded-md bg-red-500/10 px-3 py-2 text-xs text-red-300" role="alert">{{ error }}</p>

    <!-- Loading -->
    <div v-if="isLoading" class="flex min-h-64 items-center justify-center text-sm text-[#888]">
      <LoaderCircle :size="20" class="mr-3 animate-spin text-[#16C65A]" /> Loading songs
    </div>

    <!-- Empty -->
    <div v-else-if="isEmpty" class="flex min-h-48 flex-col items-center justify-center gap-2 border border-white/10 bg-[#111827] text-sm text-[#888]">
      <Music2 :size="28" class="text-[#555]" />
      <p>No songs found.</p>
    </div>

    <!-- Table -->
    <div v-else class="overflow-x-auto border border-white/10 bg-[#111827] rounded-lg">
      <table class="w-full min-w-[800px] text-left text-sm">
        <thead class="border-b border-white/10 text-[11px] uppercase tracking-wide text-[#777]">
          <tr>
            <th class="px-4 py-3 font-bold" style="width: 35%">Song</th>
            <th class="px-4 py-3 font-bold" style="width: 18%">Artist</th>
            <th class="px-4 py-3 font-bold text-center" style="width: 8%">Duration</th>
            <th class="px-4 py-3 font-bold text-center" style="width: 10%">Plays</th>
            <th class="px-4 py-3 font-bold text-center" style="width: 10%">Status</th>
            <th class="px-4 py-3 font-bold" style="width: 10%">Created</th>
            <th class="px-4 py-3 font-bold text-center" style="width: 9%">Actions</th>
          </tr>
        </thead>
        <tbody>
          <template v-for="song in songs" :key="song.id">
            <!-- Main row -->
            <tr
              class="border-b border-white/5 last:border-0 cursor-pointer transition"
              :class="expandedId === song.id ? 'bg-white/[0.04]' : 'hover:bg-white/[0.02]'"
              @click="toggleExpand(song.id)"
            >
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
                    <p class="truncate text-xs text-[#777]">/songs/{{ song.slug }}</p>
                  </div>
                </div>
              </td>
              <td class="px-4 py-3 text-[#bbb]">
                <span class="line-clamp-1">{{ artistNames(song) }}</span>
              </td>
              <td class="px-4 py-3 text-center text-[#999]">{{ formatDuration(song.durationSec) }}</td>
              <td class="px-4 py-3 text-center font-mono text-[#bbb]">{{ formatPlays(song.playCount) }}</td>
              <td class="px-4 py-3 text-center">
                <span class="rounded-full px-2.5 py-1 text-[11px] font-bold" :class="STATUS_BADGE[song.status]">
                  {{ song.status }}
                </span>
              </td>
              <td class="px-4 py-3 text-xs text-[#888]">{{ formatDate(song.createdAt) }}</td>
              <td class="px-4 py-3" @click.stop>
                <div class="flex items-center justify-center gap-1">
                  <button
                    v-if="song.status === 'SUBMITTED'"
                    class="grid size-8 place-items-center rounded-md text-emerald-400 transition hover:bg-emerald-400/15"
                    title="Approve & publish"
                    @click="openApprove(song)"
                  ><BadgeCheck :size="15" /></button>
                  <button
                    v-if="song.status === 'SUBMITTED'"
                    class="grid size-8 place-items-center rounded-md text-red-400 transition hover:bg-red-400/15"
                    title="Reject with reason"
                    @click="openReject(song)"
                  ><XCircle :size="15" /></button>
                  <button
                    v-if="song.status === 'HIDDEN'"
                    class="grid size-8 place-items-center rounded-md text-emerald-400 transition hover:bg-emerald-400/15"
                    title="Publish"
                    @click="openStatusChange(song, 'PUBLISHED')"
                  ><Eye :size="15" /></button>
                  <button
                    v-if="song.status === 'PUBLISHED'"
                    class="grid size-8 place-items-center rounded-md text-amber-400 transition hover:bg-amber-400/15"
                    title="Hide"
                    @click="openStatusChange(song, 'HIDDEN')"
                  ><EyeOff :size="15" /></button>
                  <button
                    class="grid size-8 place-items-center rounded-md text-red-400 transition hover:bg-red-400/15"
                    title="Delete"
                    @click="openDelete(song)"
                  ><Trash2 :size="15" /></button>
                </div>
              </td>
            </tr>

            <!-- Expanded detail row -->
            <tr v-if="expandedId === song.id">
              <td colspan="7" class="border-b border-white/5 bg-white/[0.02] px-4 py-4">
                <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                  <div class="flex items-start gap-2">
                    <Play :size="14" class="mt-0.5 shrink-0 text-[#16C65A]" />
                    <div>
                      <p class="text-[10px] font-bold uppercase tracking-wide text-[#777]">Total Plays</p>
                      <p class="text-sm font-bold text-white">{{ (song.playCount || 0).toLocaleString() }}</p>
                    </div>
                  </div>
                  <div class="flex items-start gap-2">
                    <Clock :size="14" class="mt-0.5 shrink-0 text-sky-400" />
                    <div>
                      <p class="text-[10px] font-bold uppercase tracking-wide text-[#777]">Duration</p>
                      <p class="text-sm font-bold text-white">{{ formatDuration(song.durationSec) }}</p>
                    </div>
                  </div>
                  <div class="flex items-start gap-2">
                    <FileText :size="14" class="mt-0.5 shrink-0 text-fuchsia-400" />
                    <div>
                      <p class="text-[10px] font-bold uppercase tracking-wide text-[#777]">Lyrics</p>
                      <p class="text-sm font-bold text-white">{{ song.lyricsType }}</p>
                    </div>
                  </div>
                  <div class="flex items-start gap-2">
                    <Music2 :size="14" class="mt-0.5 shrink-0 text-amber-400" />
                    <div>
                      <p class="text-[10px] font-bold uppercase tracking-wide text-[#777]">Updated</p>
                      <p class="text-sm font-bold text-white">{{ formatDate(song.updatedAt) }}</p>
                    </div>
                  </div>
                </div>
                <div v-if="song.artists && song.artists.length" class="mt-3 flex flex-wrap gap-2">
                  <span
                    v-for="a in song.artists"
                    :key="a.id"
                    class="flex items-center gap-2 rounded-full bg-white/5 px-3 py-1.5 text-xs text-[#ccc]"
                  >
                    <img
                      v-if="a.imageUrl"
                      :src="a.imageUrl"
                      :alt="a.name"
                      class="size-5 rounded-full object-cover"
                    />
                    <span class="font-medium">{{ a.name }}</span>
                  </span>
                </div>
                <div v-if="song.genres && song.genres.length" class="mt-3 flex flex-wrap gap-2">
                  <span
                    v-for="g in song.genres"
                    :key="g.id"
                    class="flex items-center gap-2 rounded-full bg-white/5 px-3 py-1.5 text-xs text-[#ccc]"
                  >
                    <span class="font-medium">{{ g.name }}</span>
                  </span>
                </div>
                <div v-if="song.submittedAt" class="mt-3 grid gap-3 sm:grid-cols-2">
                  <div>
                    <p class="text-[10px] font-bold uppercase tracking-wide text-[#777]">Submitted</p>
                    <p class="text-sm text-white">{{ formatDate(song.submittedAt) }}</p>
                  </div>
                  <div v-if="song.reviewedAt">
                    <p class="text-[10px] font-bold uppercase tracking-wide text-[#777]">Reviewed</p>
                    <p class="text-sm text-white">{{ formatDate(song.reviewedAt) }}</p>
                  </div>
                </div>
                <div v-if="song.reviewNote" class="mt-3 rounded-md border border-amber-500/20 bg-amber-500/5 px-3 py-2">
                  <p class="text-[10px] font-bold uppercase tracking-wide text-amber-300">Review note</p>
                  <p class="mt-0.5 text-xs text-[#ccc]">{{ song.reviewNote }}</p>
                </div>
                <div v-if="song.audioUrl" class="mt-3">
                  <audio :src="song.audioUrl" controls preload="none" class="h-8 w-full max-w-md rounded-lg" />
                </div>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>

    <!-- Pagination bar -->
    <div v-if="!isLoading && !isEmpty" class="mt-4 flex flex-wrap items-center justify-between gap-3 text-xs text-[#888]">
      <span>
        Showing <span class="font-bold text-white">{{ rangeStart }}</span>–<span class="font-bold text-white">{{ rangeEnd }}</span>
        of <span class="font-bold text-white">{{ total.toLocaleString() }}</span> songs
      </span>
      <div class="flex items-center gap-1">
        <button
          class="grid size-8 place-items-center rounded-md border border-white/10 transition hover:border-white/30 disabled:opacity-30"
          :disabled="!canPrev"
          @click="goPage(currentPage - 1)"
        ><ChevronLeft :size="15" /></button>

        <span class="px-2 text-xs text-[#aaa]">{{ currentPage }} / {{ totalPages }}</span>

        <button
          class="grid size-8 place-items-center rounded-md border border-white/10 transition hover:border-white/30 disabled:opacity-30"
          :disabled="!canNext"
          @click="goPage(currentPage + 1)"
        ><ChevronRight :size="15" /></button>
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
              :class="confirmModal.type === 'delete' ? 'bg-red-500/15 text-red-400'
                : confirmModal.type === 'approve' ? 'bg-emerald-500/15 text-emerald-400'
                : 'bg-amber-500/15 text-amber-400'"
            ><AlertTriangle :size="20" /></div>
            <h3 class="text-lg font-bold text-white">{{ confirmModal.action }} song?</h3>
          </div>
          <p class="mb-6 text-sm text-[#aaa]">
            <template v-if="confirmModal.type === 'delete'">
              This will permanently delete "<span class="font-bold text-white">{{ confirmModal.songTitle }}</span>".
              This action cannot be undone.
            </template>
            <template v-else-if="confirmModal.type === 'approve'">
              Publish "<span class="font-bold text-white">{{ confirmModal.songTitle }}</span>" to the public catalog?
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
            >Cancel</button>
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

    <!-- Reject modal -->
    <Teleport to="body">
      <div v-if="rejectModal" class="fixed inset-0 z-[9999] flex items-center justify-center bg-black/70 backdrop-blur-sm" @click.self="closeReject">
        <div class="relative w-full max-w-md rounded-2xl border border-white/10 bg-[#111827] p-6 shadow-2xl">
          <button class="absolute right-4 top-4 text-[#888] hover:text-white" @click="closeReject"><X :size="18" /></button>
          <div class="mb-4 flex items-center gap-3">
            <div class="grid size-10 place-items-center rounded-full bg-red-500/15 text-red-400"><XCircle :size="20" /></div>
            <div>
              <h3 class="text-lg font-bold text-white">Reject song</h3>
              <p class="truncate text-xs text-[#888]">{{ rejectModal.songTitle }}</p>
            </div>
          </div>
          <p class="mb-2 text-xs text-[#aaa]">The artist will see this note and can resubmit after fixing it.</p>
          <textarea
            v-model="rejectReason"
            rows="3"
            maxlength="500"
            placeholder="Why is this song being rejected?"
            class="w-full rounded-lg border border-white/10 bg-white/[0.03] px-3 py-2 text-sm text-white outline-none placeholder:text-[#666] focus:border-red-400/60"
          ></textarea>
          <p class="mt-1 text-right text-[10px] text-[#666]">{{ rejectReason.length }}/500</p>
          <div class="mt-4 flex justify-end gap-3">
            <button
              class="h-9 rounded-full border border-white/15 px-5 text-xs font-bold text-[#bbb] transition hover:border-white/30 hover:text-white"
              :disabled="rejectLoading"
              @click="closeReject"
            >Cancel</button>
            <button
              class="h-9 rounded-full bg-red-500 px-5 text-xs font-bold text-white transition hover:bg-red-600 disabled:opacity-50"
              :disabled="rejectLoading"
              @click="confirmReject"
            >
              <LoaderCircle v-if="rejectLoading" :size="14" class="mr-1 inline animate-spin" />
              Reject song
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
