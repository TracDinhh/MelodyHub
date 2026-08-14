<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ListMusic, Music2, Plus, Trash2 } from '@lucide/vue';
import { usePlaylistStore } from '../stores/playlist.store';

const store = usePlaylistStore();

const showCreate = ref(false);
const creating = ref(false);
const createError = ref('');
const form = reactive({ name: '', description: '', isPublic: false });
const busy = ref(new Set());

function openCreate() {
  form.name = '';
  form.description = '';
  form.isPublic = false;
  createError.value = '';
  showCreate.value = true;
}

async function submitCreate() {
  if (!form.name.trim()) {
    createError.value = 'Please give your playlist a name.';
    return;
  }
  creating.value = true;
  createError.value = '';
  try {
    await store.create({
      name: form.name.trim(),
      description: form.description.trim() || null,
      isPublic: form.isPublic
    });
    showCreate.value = false;
  } catch (caught) {
    createError.value = caught?.message || 'Could not create the playlist.';
  } finally {
    creating.value = false;
  }
}

async function remove(playlist) {
  const id = playlist?.id;
  if (!id || busy.value.has(id)) return;
  if (!window.confirm(`Delete "${playlist.name}"? This can't be undone.`)) return;
  busy.value = new Set([...busy.value, id]);
  try {
    await store.remove(id);
  } finally {
    const next = new Set(busy.value);
    next.delete(id);
    busy.value = next;
  }
}

onMounted(() => store.loadPage(1));
</script>

<template>
  <div class="mx-auto max-w-[1260px] space-y-7 px-4 py-8 sm:px-7">
    <header class="flex flex-wrap items-end justify-between gap-4">
      <div class="flex flex-col gap-2">
        <p class="melodyhub-kicker">YOUR LIBRARY</p>
        <div class="flex items-center gap-3">
          <ListMusic :size="32" class="text-[#20E878]" />
          <h1 class="text-3xl font-black text-white sm:text-4xl">My playlists</h1>
        </div>
        <p class="text-sm text-[#8EA696]">Collections of the songs you love, in the order you want them.</p>
      </div>
      <button
        class="inline-flex h-11 items-center gap-2 rounded-full bg-[#20E878] px-5 text-sm font-black text-[#0F0F12] transition hover:bg-[#54d67b]"
        @click="openCreate"
      >
        <Plus :size="16" /> New playlist
      </button>
    </header>

    <div v-if="store.isLoading && store.playlists.length === 0" class="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
      <div v-for="n in 8" :key="n" class="aspect-square animate-pulse rounded-xl bg-white/5" />
    </div>

    <div v-else-if="store.error" class="rounded-lg border border-red-500/30 bg-red-500/10 px-4 py-6 text-center text-sm text-red-200">
      {{ store.error }}
      <button class="ml-3 text-xs font-bold text-[#20E878] hover:underline" @click="store.loadPage(1)">Retry</button>
    </div>

    <div v-else-if="store.playlists.length === 0" class="rounded-lg border border-white/10 bg-white/[0.02] px-6 py-16 text-center">
      <ListMusic :size="42" class="mx-auto mb-4 text-[#444]" />
      <p class="text-sm font-bold text-white">No playlists yet</p>
      <p class="mt-2 text-xs text-[#777]">Create your first playlist and start adding songs.</p>
      <button
        class="mt-5 inline-flex h-10 items-center gap-2 rounded-full bg-[#20E878] px-5 text-xs font-black text-[#0F0F12]"
        @click="openCreate"
      >
        <Plus :size="14" /> New playlist
      </button>
    </div>

    <div v-else class="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
      <div
        v-for="playlist in store.playlists"
        :key="playlist.id"
        class="group relative flex flex-col rounded-xl border border-white/[0.06] bg-[#10151a] p-3 transition hover:border-white/15 hover:bg-white/[0.03]"
      >
        <RouterLink :to="{ name: 'playlist-detail', params: { id: playlist.id } }" class="block">
          <div class="mb-3 aspect-square overflow-hidden rounded-lg">
            <img
              v-if="playlist.coverUrl"
              :src="playlist.coverUrl"
              :alt="`${playlist.name} cover`"
              class="size-full object-cover"
            />
            <span v-else class="grid size-full place-items-center bg-white/[0.05] text-[#3f4a44]">
              <Music2 :size="40" />
            </span>
          </div>
          <p class="truncate text-sm font-bold text-white group-hover:text-[#FDA4AF]">{{ playlist.name }}</p>
          <p class="mt-1 text-xs text-[#8EA696]">
            {{ playlist.songCount }} {{ playlist.songCount === 1 ? 'song' : 'songs' }}
            <span v-if="playlist.isPublic" class="ml-1 text-[#20E878]">· Public</span>
          </p>
        </RouterLink>
        <button
          class="melodyhub-icon-btn !size-8 absolute right-3 top-3 opacity-0 transition hover:!text-red-300 group-hover:opacity-100"
          :disabled="busy.has(playlist.id)"
          title="Delete playlist"
          @click="remove(playlist)"
        >
          <Trash2 :size="14" />
        </button>
      </div>
    </div>

    <!-- Create modal -->
    <div
      v-if="showCreate"
      class="fixed inset-0 z-50 grid place-items-center bg-black/60 p-4"
      @click.self="showCreate = false"
    >
      <div class="w-full max-w-md rounded-2xl border border-white/10 bg-[#10151a] p-6 shadow-2xl">
        <h2 class="text-lg font-black text-white">New playlist</h2>
        <form class="mt-5 space-y-4" @submit.prevent="submitCreate">
          <div>
            <label class="mb-1 block text-xs font-bold text-[#8EA696]">Name</label>
            <input
              v-model="form.name"
              type="text"
              maxlength="150"
              placeholder="My favourite songs"
              class="w-full rounded-lg border border-white/10 bg-black/30 px-3 py-2 text-sm text-white outline-none focus:border-[#20E878]"
            />
          </div>
          <div>
            <label class="mb-1 block text-xs font-bold text-[#8EA696]">Description (optional)</label>
            <textarea
              v-model="form.description"
              rows="3"
              maxlength="500"
              class="w-full resize-none rounded-lg border border-white/10 bg-black/30 px-3 py-2 text-sm text-white outline-none focus:border-[#20E878]"
            />
          </div>
          <label class="flex items-center gap-2 text-sm text-[#c8d2cc]">
            <input v-model="form.isPublic" type="checkbox" class="size-4 accent-[#20E878]" />
            Make this playlist public
          </label>
          <p v-if="createError" class="text-xs text-red-300">{{ createError }}</p>
          <div class="flex justify-end gap-3 pt-2">
            <button
              type="button"
              class="h-10 rounded-full px-4 text-sm font-bold text-[#8EA696] hover:text-white"
              @click="showCreate = false"
            >
              Cancel
            </button>
            <button
              type="submit"
              class="inline-flex h-10 items-center gap-2 rounded-full bg-[#20E878] px-5 text-sm font-black text-[#0F0F12] disabled:opacity-50"
              :disabled="creating"
            >
              {{ creating ? 'Creating…' : 'Create' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
