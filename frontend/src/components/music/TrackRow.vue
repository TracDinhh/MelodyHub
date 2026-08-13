<script setup>
import { computed, ref } from 'vue';
import { Heart, MoreHorizontal, Pause, Play } from '@lucide/vue';
import { useRouter } from 'vue-router';
import AddToPlaylistButton from './AddToPlaylistButton.vue';
import { useAuthStore } from '../../stores/auth.store';
import { usePlayerStore } from '../../stores/player.store';
import { formatDuration } from '../../utils/formatDate';

const props = defineProps({
  track: { type: Object, required: true },
  index: { type: Number, required: true },
  showAlbum: { type: Boolean, default: false },
  songSlug: { type: String, default: null }
});
const emit = defineEmits(['liked-change']);

const auth = useAuthStore();
const player = usePlayerStore();
const router = useRouter();
const menuOpen = ref(false);
const isCurrent = computed(() => player.currentTrack.id === props.track.id);
const isLiked = computed(() => player.likedIds.has(props.track.id));
const likeBusy = ref(false);

function openDetail() {
  if (!props.songSlug) return;
  router.push({ name: 'song-detail', params: { slug: props.songSlug } });
}

async function toggleLike() {
  if (!auth.isAuthenticated || likeBusy.value) return;
  likeBusy.value = true;
  try {
    const liked = await player.toggleLike(props.track.id);
    emit('liked-change', { songId: props.track.id, liked });
  } finally {
    likeBusy.value = false;
  }
}
</script>

<template>
  <div class="group grid min-w-[680px] grid-cols-[40px_minmax(240px,1.5fr)_minmax(130px,0.8fr)_100px_40px_40px_40px] items-center gap-3 rounded-md px-3 py-2 text-sm transition hover:bg-white/[0.045]">
    <button class="grid size-8 place-items-center text-[#777] group-hover:text-white" :title="`Play ${track.title}`" @click="player.playTrack(track)">
      <span v-if="isCurrent && player.isPlaying" class="playing-bars" aria-label="Currently playing"><i></i><i></i><i></i></span>
      <Pause v-else-if="isCurrent" :size="16" class="fill-current" />
      <span v-else class="group-hover:hidden">{{ index + 1 }}</span>
      <Play v-if="!isCurrent" :size="15" class="hidden fill-current group-hover:block" />
    </button>
    <div class="flex min-w-0 items-center gap-3">
      <button v-if="songSlug" class="shrink-0" :title="`Open ${track.title}`" @click.stop="openDetail">
        <img :src="track.cover" :alt="`${track.title} cover`" class="size-11 rounded-md object-cover" />
      </button>
      <img v-else :src="track.cover" :alt="`${track.title} cover`" class="size-11 rounded-md object-cover" />
      <div class="min-w-0">
        <button
          v-if="songSlug"
          class="block max-w-full truncate text-left font-bold transition hover:text-[#8be8a8]"
          :class="isCurrent ? 'text-[#1DB954]' : 'text-white'"
          @click.stop="openDetail"
        >
          {{ track.title }}
        </button>
        <p v-else class="truncate font-bold" :class="isCurrent ? 'text-[#1DB954]' : 'text-white'">{{ track.title }}</p>
        <p class="truncate text-xs text-[#777]">{{ track.artist }}<span v-if="track.featured"> feat. {{ track.featured }}</span></p>
      </div>
    </div>
    <p class="truncate text-xs text-[#777]">{{ showAlbum ? track.album : track.plays }}</p>
    <p class="text-xs text-[#777]">{{ formatDuration(track.duration) }}</p>
    <button class="melodyhub-icon-btn !size-8" :disabled="!auth.isAuthenticated || likeBusy" :title="isLiked ? 'Remove from favorites' : 'Add to favorites'" @click="toggleLike">
      <Heart :size="16" :class="isLiked ? 'fill-[#1DB954] text-[#1DB954]' : ''" />
    </button>
    <AddToPlaylistButton
      v-if="track.id != null"
      :song-id="track.id"
      hide-until-hover
      size="sm"
    />
    <div class="relative">
      <button class="melodyhub-icon-btn !size-8" title="Track options" @click="menuOpen = !menuOpen"><MoreHorizontal :size="17" /></button>
      <div v-if="menuOpen" class="absolute right-0 top-8 z-20 w-40 rounded-lg border border-white/10 bg-[#202020] p-1.5 text-xs text-[#bbb] shadow-xl">
        <button class="w-full rounded-md px-3 py-2 text-left hover:bg-white/10 hover:text-white">Add to queue</button>
        <button v-if="songSlug" class="w-full rounded-md px-3 py-2 text-left hover:bg-white/10 hover:text-white" @click="openDetail">Open song page</button>
        <button class="w-full rounded-md px-3 py-2 text-left hover:bg-white/10 hover:text-white">Go to album</button>
        <button class="w-full rounded-md px-3 py-2 text-left hover:bg-white/10 hover:text-white">Share</button>
      </div>
    </div>
  </div>
</template>
