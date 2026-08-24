<script setup>
import { computed, onMounted, ref } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { Disc3, LoaderCircle, Pencil, Plus, Trash2, X } from '@lucide/vue';
import { albumService } from '../../services/albumService';

const route = useRoute();
const artistId = Number(route.params.artistId);

const albums = ref([]);
const total = ref(0);
const page = ref(1);
const size = ref(20);
const isLoading = ref(true);
const error = ref('');

// Modal state
const showModal = ref(false);
const modalMode = ref('create'); // 'create' | 'edit'
const editingAlbum = ref(null);
const isSaving = ref(false);
const saveError = ref('');

// Form state
const form = ref({
  title: '',
  albumType: 'ALBUM',
  coverUrl: '',
  releaseDate: ''
});

const isEmpty = computed(() => !isLoading.value && albums.value.length === 0 && page.value === 1);
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)));

async function load() {
  isLoading.value = true;
  error.value = '';
  try {
    const paged = await albumService.list(artistId, { page: page.value, size: size.value });
    albums.value = paged?.items || [];
    total.value = paged?.total || 0;
  } catch (requestError) {
    error.value = requestError.message || 'Unable to load your albums.';
  } finally {
    isLoading.value = false;
  }
}

function changePage(newPage) {
  if (newPage < 1 || newPage > totalPages.value || newPage === page.value) return;
  page.value = newPage;
  load();
}

function changeSize(newSize) {
  size.value = newSize;
  page.value = 1;
  load();
}

function openCreate() {
  modalMode.value = 'create';
  editingAlbum.value = null;
  form.value = { title: '', albumType: 'ALBUM', coverUrl: '', releaseDate: '' };
  saveError.value = '';
  showModal.value = true;
}

function openEdit(album) {
  modalMode.value = 'edit';
  editingAlbum.value = album;
  form.value = {
    title: album.title,
    albumType: album.albumType || 'ALBUM',
    coverUrl: album.coverUrl || '',
    releaseDate: album.releaseDate ? album.releaseDate.split('T')[0] : ''
  };
  saveError.value = '';
  showModal.value = true;
}

function closeModal() {
  showModal.value = false;
  editingAlbum.value = null;
}

async function saveAlbum() {
  if (!form.value.title.trim()) {
    saveError.value = 'Title is required.';
    return;
  }
  isSaving.value = true;
  saveError.value = '';
  try {
    const data = {
      title: form.value.title.trim(),
      albumType: form.value.albumType,
      coverUrl: form.value.coverUrl.trim() || null,
      releaseDate: form.value.releaseDate || null
    };
    if (modalMode.value === 'create') {
      await albumService.create(artistId, data);
    } else {
      await albumService.update(artistId, editingAlbum.value.id, data);
    }
    closeModal();
    await load();
  } catch (requestError) {
    saveError.value = requestError.message || 'Unable to save album.';
  } finally {
    isSaving.value = false;
  }
}

async function deleteAlbum(album) {
  if (!confirm(`Delete album "${album.title}"? This cannot be undone.`)) return;
  try {
    await albumService.delete(artistId, album.id);
    await load();
  } catch (requestError) {
    error.value = requestError.message || 'Unable to delete album.';
  }
}

function albumTypeLabel(type) {
  if (type === 'EP') return 'EP';
  if (type === 'SINGLE') return 'Single';
  return 'Album';
}

function formatDate(dateStr) {
  if (!dateStr) return '—';
  const d = new Date(dateStr);
  return d.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
}

onMounted(load);
</script>

<template>
  <div class="mx-auto w-full max-w-6xl px-5 py-8 pb-12 sm:px-8">
    <div class="mb-6 flex flex-wrap items-center justify-between gap-3">
      <div class="flex items-center gap-3">
        <Disc3 :size="28" class="text-[#16C65A]" />
        <div>
          <p class="melodyhub-kicker">STUDIO</p>
          <h1 class="melodyhub-section-title">My Albums <span class="text-sm font-normal text-[#666]">({{ total }})</span></h1>
        </div>
      </div>
      <button
        type="button"
        class="inline-flex h-10 items-center gap-2 rounded-full bg-[#16C65A] px-5 text-xs font-black text-black transition hover:bg-[#22C55E]"
        @click="openCreate"
      >
        <Plus :size="16" /> Create album
      </button>
    </div>

    <p v-if="error" class="mb-4 rounded-md bg-red-500/10 px-3 py-2 text-xs text-red-300" role="alert">{{ error }}</p>

    <div v-if="isLoading" class="flex min-h-64 items-center justify-center text-sm text-[#888]">
      <LoaderCircle :size="20" class="mr-3 animate-spin text-[#16C65A]" /> Loading albums
    </div>

    <div v-else-if="isEmpty" class="flex min-h-56 flex-col items-center justify-center gap-4 border border-white/10 bg-[#111827] text-center">
      <Disc3 :size="34" class="text-[#16C65A]" />
      <p class="text-sm text-[#999]">You haven't created any albums yet.</p>
      <button
        type="button"
        class="inline-flex h-10 items-center gap-2 rounded-full bg-[#16C65A] px-5 text-xs font-black text-black transition hover:bg-[#22C55E]"
        @click="openCreate"
      >
        <Plus :size="16" /> Create your first album
      </button>
    </div>

    <template v-else>
      <!-- Albums grid -->
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <div
          v-for="album in albums"
          :key="album.id"
          class="group relative flex gap-4 rounded-lg border border-white/[0.06] bg-[#111827] p-4 transition hover:border-white/15"
        >
          <div class="size-24 shrink-0 overflow-hidden rounded bg-white/[0.04]">
            <img v-if="album.coverUrl" :src="album.coverUrl" :alt="album.title" class="h-full w-full object-cover" />
            <div v-else class="grid h-full w-full place-items-center">
              <Disc3 :size="28" class="text-[#555]" />
            </div>
          </div>
          <div class="min-w-0 flex-1">
            <p class="truncate text-sm font-bold text-white">{{ album.title }}</p>
            <p class="mt-0.5 text-xs text-[#666]">{{ albumTypeLabel(album.albumType) }}</p>
            <p class="mt-1 text-xs text-[#555]">
              {{ album.songCount }} {{ album.songCount === 1 ? 'song' : 'songs' }}
            </p>
            <p v-if="album.releaseDate" class="mt-1 text-xs text-[#555]">{{ formatDate(album.releaseDate) }}</p>
            <div class="mt-3 flex items-center gap-2">
              <button
                type="button"
                class="rounded-md border border-white/10 bg-white/5 px-3 py-1.5 text-xs font-medium text-[#bbb] transition hover:border-white/20 hover:bg-white/10 hover:text-white"
                @click="openEdit(album)"
              >
                <Pencil :size="12" class="mr-1 inline" /> Edit
              </button>
              <button
                type="button"
                class="rounded-md border border-red-500/20 bg-red-500/10 px-3 py-1.5 text-xs font-medium text-red-400 transition hover:border-red-500/40 hover:bg-red-500/20"
                @click="deleteAlbum(album)"
              >
                <Trash2 :size="12" class="mr-1 inline" /> Delete
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="mt-6 flex items-center justify-between gap-3">
        <div class="flex items-center gap-2">
          <span class="text-xs text-[#666]">Show</span>
          <select
            :value="size"
            class="rounded border border-white/10 bg-[#111827] px-2 py-1 text-xs text-white"
            @change="changeSize(Number($event.target.value))"
          >
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
          </select>
          <span class="text-xs text-[#666]">per page</span>
        </div>

        <div class="flex items-center gap-3 text-xs font-bold text-[#8EA696]">
          <button
            class="rounded-md border border-white/[0.08] px-3 py-2 transition hover:border-[#16C65A]/50 hover:text-[#16C65A] disabled:cursor-not-allowed disabled:opacity-40"
            :disabled="page === 1"
            @click="changePage(page - 1)"
          >Previous</button>
          <span>Page {{ page }} of {{ totalPages }}</span>
          <button
            class="rounded-md border border-white/[0.08] px-3 py-2 transition hover:border-[#16C65A]/50 hover:text-[#16C65A] disabled:cursor-not-allowed disabled:opacity-40"
            :disabled="page === totalPages"
            @click="changePage(page + 1)"
          >Next</button>
        </div>
      </div>
    </template>

    <!-- Create/Edit Modal -->
    <Teleport to="body">
      <div v-if="showModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
        <div class="w-full max-w-md rounded-xl border border-white/10 bg-[#121214] p-6 shadow-2xl">
          <div class="mb-4 flex items-center justify-between">
            <h2 class="text-lg font-bold text-white">{{ modalMode === 'create' ? 'Create Album' : 'Edit Album' }}</h2>
            <button type="button" class="text-[#888] hover:text-white" @click="closeModal">
              <X :size="20" />
            </button>
          </div>

          <div class="space-y-4">
            <div>
              <label class="mb-1.5 block text-xs font-bold text-[#999]">Title *</label>
              <input
                v-model="form.title"
                type="text"
                placeholder="Album title"
                class="w-full rounded-lg border border-white/10 bg-white/5 px-3 py-2.5 text-sm text-white placeholder-[#555] focus:border-[#16C65A]/60 focus:outline-none"
              />
            </div>

            <div>
              <label class="mb-1.5 block text-xs font-bold text-[#999]">Album Type</label>
              <select
                v-model="form.albumType"
                class="w-full rounded-lg border border-white/10 bg-white/5 px-3 py-2.5 text-sm text-white focus:border-[#16C65A]/60 focus:outline-none"
              >
                <option value="ALBUM">Album</option>
                <option value="EP">EP</option>
                <option value="SINGLE">Single</option>
              </select>
            </div>

            <div>
              <label class="mb-1.5 block text-xs font-bold text-[#999]">Cover Image URL</label>
              <input
                v-model="form.coverUrl"
                type="url"
                placeholder="https://..."
                class="w-full rounded-lg border border-white/10 bg-white/5 px-3 py-2.5 text-sm text-white placeholder-[#555] focus:border-[#16C65A]/60 focus:outline-none"
              />
            </div>

            <div>
              <label class="mb-1.5 block text-xs font-bold text-[#999]">Release Date</label>
              <input
                v-model="form.releaseDate"
                type="date"
                class="w-full rounded-lg border border-white/10 bg-white/5 px-3 py-2.5 text-sm text-white focus:border-[#16C65A]/60 focus:outline-none"
              />
            </div>
          </div>

          <p v-if="saveError" class="mt-3 text-xs text-red-400">{{ saveError }}</p>

          <div class="mt-5 flex justify-end gap-3">
            <button
              type="button"
              class="rounded-lg border border-white/10 px-4 py-2 text-sm font-medium text-[#bbb] transition hover:border-white/20 hover:text-white"
              @click="closeModal"
            >
              Cancel
            </button>
            <button
              type="button"
              class="inline-flex items-center gap-2 rounded-lg bg-[#16C65A] px-4 py-2 text-sm font-bold text-black transition hover:bg-[#22C55E]"
              :disabled="isSaving"
              @click="saveAlbum"
            >
              <LoaderCircle v-if="isSaving" :size="16" class="animate-spin" />
              {{ modalMode === 'create' ? 'Create' : 'Save' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
