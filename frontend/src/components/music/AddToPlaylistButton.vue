<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { Check, ListMusic, Plus } from '@lucide/vue';
import { usePlaylistStore } from '../../stores/playlist.store';
import { useAuthStore } from '../../stores/auth.store';

const props = defineProps({
  songId: { type: [Number, String], required: true },
  hideUntilHover: { type: Boolean, default: false },
  size: { type: String, default: 'md', validator: (value) => ['sm', 'md'].includes(value) }
});

const store = usePlaylistStore();
const authStore = useAuthStore();

const open = ref(false);
const loading = ref(false);
const error = ref('');
const busyId = ref(null);
const addedId = ref(null);
const trigger = ref(null);
const menu = ref(null);
const menuStyle = ref({});

const MENU_WIDTH = 256;
const BUTTON_SIZE = props.size === 'sm' ? '!size-8' : '!size-11';
const ICON_SIZE = props.size === 'sm' ? 14 : 18;

function positionMenu() {
  const rect = trigger.value?.getBoundingClientRect();
  if (!rect) return;
  const left = Math.min(rect.right - MENU_WIDTH, window.innerWidth - MENU_WIDTH - 12);
  menuStyle.value = {
    position: 'fixed',
    top: `${rect.bottom + 8}px`,
    left: `${Math.max(12, left)}px`,
    width: `${MENU_WIDTH}px`
  };
}

async function toggle() {
  open.value = !open.value;
  if (!open.value) return;
  await nextTick();
  positionMenu();
  if (store.playlists.length === 0) {
    loading.value = true;
    error.value = '';
    try {
      await store.loadPage(1);
    } catch (caught) {
      error.value = caught?.message || 'Could not load playlists.';
    } finally {
      loading.value = false;
      await nextTick();
      positionMenu();
    }
  }
}

async function add(playlist) {
  if (busyId.value) return;
  busyId.value = playlist.id;
  error.value = '';
  try {
    await store.addSong(playlist.id, Number(props.songId));
    addedId.value = playlist.id;
    setTimeout(() => {
      if (addedId.value === playlist.id) addedId.value = null;
    }, 1500);
  } catch (caught) {
    error.value = caught?.message || 'Could not add to playlist.';
  } finally {
    busyId.value = null;
  }
}

function onOutside(event) {
  if (trigger.value?.contains(event.target) || menu.value?.contains(event.target)) return;
  open.value = false;
}

function onReflow() {
  if (open.value) positionMenu();
}

onMounted(() => {
  document.addEventListener('click', onOutside);
  window.addEventListener('resize', onReflow);
  window.addEventListener('scroll', onReflow, true);
});
onBeforeUnmount(() => {
  document.removeEventListener('click', onOutside);
  window.removeEventListener('resize', onReflow);
  window.removeEventListener('scroll', onReflow, true);
});
</script>

<template>
  <div v-if="authStore.isAuthenticated" ref="trigger" :class="['inline-flex', { 'opacity-0 transition group-hover:opacity-100 focus-within:opacity-100': hideUntilHover }]">
    <button
      :class="['melodyhub-icon-btn', BUTTON_SIZE]"
      title="Add to playlist"
      @click.stop="toggle"
    >
      <Plus :size="ICON_SIZE" />
    </button>

    <Teleport to="body">
      <div
        v-if="open"
        ref="menu"
        :style="menuStyle"
        class="z-[80] overflow-hidden rounded-lg border border-white/10 bg-[#171717]/95 p-2 shadow-2xl shadow-black/60 backdrop-blur-xl"
      >
        <p class="px-3 py-2 text-[11px] font-black uppercase tracking-wider text-[#777]">Add to playlist</p>

        <div v-if="loading" class="px-3 py-4 text-center text-xs text-[#777]">Loading…</div>
        <p v-else-if="error" class="px-3 py-3 text-xs text-red-300">{{ error }}</p>
        <div v-else-if="store.playlists.length === 0" class="px-3 py-4 text-center text-xs text-[#777]">
          No playlists yet.
          <RouterLink :to="{ name: 'playlists' }" class="mt-1 block font-bold text-[#65e78c] hover:underline" @click="open = false">
            Create one
          </RouterLink>
        </div>

        <ul v-else class="max-h-64 overflow-y-auto">
          <li v-for="playlist in store.playlists" :key="playlist.id">
            <button
              class="flex w-full items-center gap-2 rounded-md px-3 py-2 text-left text-sm text-[#c8d2cc] transition hover:bg-white/5 disabled:opacity-50"
              :disabled="busyId === playlist.id"
              @click="add(playlist)"
            >
              <ListMusic :size="15" class="shrink-0 text-[#777]" />
              <span class="min-w-0 flex-1 truncate">{{ playlist.name }}</span>
              <Check v-if="addedId === playlist.id" :size="15" class="shrink-0 text-[#65e78c]" />
            </button>
          </li>
        </ul>
      </div>
    </Teleport>
  </div>
</template>
