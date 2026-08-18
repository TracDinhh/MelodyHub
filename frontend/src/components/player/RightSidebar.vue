<script setup>
import { computed, nextTick, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowUpRight, CheckCircle2, Crown, Lock, Play, Sparkles, X } from '@lucide/vue';
import { artists, featuredArtist, tracks } from '../../data/music';
import { usePlayerStore } from '../../stores/player.store';
import { useAuthStore } from '../../stores/auth.store';
import LyricCardModal from '../lyrics/LyricCardModal.vue';
import { useLyricSelection } from '../../composables/useLyricSelection';

defineProps({ mobileOpen: Boolean });
const emit = defineEmits(['close']);
const player = usePlayerStore();
const authStore = useAuthStore();
const router = useRouter();
const activeTab = ref('lyrics');
const tabs = [
  { id: 'lyrics', label: 'Lyrics' },
  { id: 'bio', label: 'Artist Bio' },
  { id: 'recommended', label: 'Recommended' }
];

const lyrics = computed(() => {
  if (player.hasSyncedLyrics) return player.syncedLyrics;
  return Array.isArray(player.currentTrack?.lyrics) ? player.currentTrack.lyrics : [];
});

const activeLine = computed(() => {
  if (player.hasSyncedLyrics) return player.currentLyricLine;
  const count = lyrics.value.length;
  if (!count || !player.duration) return null;
  return Math.min(count - 1, Math.floor((player.currentTime / player.duration) * count));
});

const lyricsLocked = computed(() =>
  player.currentTrack?.lyricsType === 'SYNCED' && !authStore.isPremium
);

function openPremium() {
  emit('close');
  router.push({ name: 'premium' });
}

// Lyric-card feature is only offered for synced lyrics (a real line to feature).
const lyricSelection = useLyricSelection();
const cardOpen = ref(false);
const cardLines = computed(() =>
  lyricSelection.sortedIndices.value
    .map((index) => player.syncedLyrics[index]?.text)
    .filter(Boolean)
);

function onLyricClick(index) {
  if (!player.hasSyncedLyrics) return;
  lyricSelection.toggle(index);
}

// Reset any selection whenever the track changes so lines never mismatch.
watch(
  () => player.currentTrack?.id,
  () => lyricSelection.clear()
);

watch(activeLine, async (index) => {
  await nextTick();
  document.querySelector(`[data-lyric="${index}"]`)?.scrollIntoView({
    behavior: 'smooth',
    block: 'center'
  });
});
</script>

<template>
  <div
    v-if="mobileOpen"
    class="fixed inset-0 z-40 bg-black/70 backdrop-blur-sm xl:hidden"
    @click="emit('close')"
  />
  <aside
    class="melodyhub-panel-edge fixed inset-y-0 right-0 z-50 flex w-[min(340px,90vw)] flex-col rounded-l-xl border border-white/[0.055] bg-[#101417] transition-transform duration-300 xl:static xl:z-auto xl:w-80 xl:rounded-xl xl:translate-x-0"
    :class="mobileOpen ? 'translate-x-0' : 'translate-x-full'"
  >
    <header class="flex h-18 items-center justify-between px-5">
      <h2 class="text-xs font-black tracking-wide text-white">NOW PLAYING</h2>
      <button class="melodyhub-icon-btn xl:hidden" title="Close track information" @click="emit('close')">
        <X :size="18" />
      </button>
    </header>

    <div class="flex items-center gap-3 px-5 pb-4">
      <img v-if="player.currentTrack.cover" :src="player.currentTrack.cover" :alt="`${player.currentTrack.title} cover`" class="size-14 rounded-lg object-cover" />
      <span v-else class="grid size-14 place-items-center rounded-lg bg-white/[0.06] text-[#555]"><Play :size="18" /></span>
      <div class="min-w-0">
        <p class="truncate text-sm font-bold text-white">{{ player.currentTrack.title || 'Nothing playing' }}</p>
        <p class="truncate text-xs text-[#858585]">{{ player.currentTrack.artist }}</p>
      </div>
    </div>

    <div class="grid grid-cols-3 border-y border-white/5 px-3">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        class="relative h-11 text-[11px] font-bold transition"
        :class="activeTab === tab.id ? 'text-white after:absolute after:inset-x-3 after:bottom-0 after:h-0.5 after:bg-[#16C65A]' : 'text-[#737373] hover:text-white'"
        @click="activeTab = tab.id"
      >
        {{ tab.label }}
      </button>
    </div>

    <div class="min-h-0 flex-1 overflow-y-auto">
      <div v-if="activeTab === 'lyrics'" class="space-y-5 px-6 py-8">
        <div v-if="lyricsLocked" class="flex min-h-56 flex-col items-center justify-center text-center">
          <span class="grid size-10 place-items-center rounded-xl bg-[#20E878] text-[#09090B]"><Crown :size="20" /></span>
          <Lock :size="18" class="mt-4 text-[#20E878]" />
          <p class="mt-3 text-sm font-black text-white">Synced lyrics are Premium</p>
          <p class="mt-2 text-xs leading-5 text-[#858585]">Unlock karaoke lyrics and lyric cards.</p>
          <button class="mt-4 inline-flex h-9 items-center gap-2 rounded-full bg-[#20E878] px-4 text-xs font-black text-[#09090B]" @click="openPremium"><Crown :size="14" /> View plans</button>
        </div>
        <template v-else>
          <component
          :is="player.hasSyncedLyrics ? 'button' : 'p'"
          v-for="(line, index) in lyrics"
          :key="`${player.currentTrack.id}-${index}`"
          :data-lyric="index"
          :type="player.hasSyncedLyrics ? 'button' : undefined"
          class="block w-full origin-left rounded-md text-left transition-all duration-500"
          :class="[
            index === activeLine ? 'text-lg font-black text-[#16C65A]' : 'text-sm font-semibold text-[#6d6d6d]',
            player.hasSyncedLyrics ? 'px-2 py-1' : '',
            lyricSelection.isSelected(index) ? 'bg-[#20E878]/15 ring-1 ring-inset ring-[#20E878]/40' : ''
          ]"
          @click="onLyricClick(index)"
        >
          {{ player.hasSyncedLyrics ? line.text : line }}
          </component>
          <p v-if="!lyrics.length" class="text-sm text-[#6d6d6d]">No lyrics available.</p>
          <button
          v-if="lyricSelection.hasSelection.value"
          class="sticky bottom-0 inline-flex w-full items-center justify-center gap-2 rounded-full bg-[#20E878] py-2.5 text-xs font-black text-[#09090B] transition hover:bg-[#64F4A1]"
          @click="cardOpen = true"
        >
          <Sparkles :size="14" /> Create lyric card ({{ lyricSelection.sortedIndices.value.length }})
          </button>
        </template>
      </div>

      <div v-else-if="activeTab === 'bio'" class="p-5">
        <img :src="featuredArtist.avatar" :alt="featuredArtist.name" class="h-52 w-full rounded-lg object-cover" />
        <div class="mt-4 flex items-center gap-1.5">
          <h3 class="font-bold text-white">{{ featuredArtist.name }}</h3>
          <CheckCircle2 :size="15" class="fill-[#16C65A] text-black" />
        </div>
        <p class="mt-1 text-xs font-bold text-[#9a9a9a]">{{ featuredArtist.followers }} followers</p>
        <p class="mt-4 text-sm leading-6 text-[#8b8b8b]">{{ featuredArtist.bio }}</p>
        <RouterLink
          :to="{ name: 'artist-detail', params: { slug: featuredArtist.slug } }"
          class="mt-4 inline-flex items-center gap-1 text-xs font-bold text-[#16C65A]"
          @click="emit('close')"
        >
          View artist <ArrowUpRight :size="14" />
        </RouterLink>
      </div>

      <div v-else class="space-y-1 p-3">
        <button
          v-for="track in tracks.slice(3, 8)"
          :key="track.id"
          class="group flex w-full items-center gap-3 rounded-lg p-2 text-left hover:bg-white/5"
          @click="player.playTrack(track)"
        >
          <span class="relative">
            <img :src="track.cover" :alt="`${track.title} cover`" class="size-11 rounded-md object-cover" />
            <span class="absolute inset-0 grid place-items-center rounded-md bg-black/50 opacity-0 group-hover:opacity-100">
              <Play :size="15" class="fill-white text-white" />
            </span>
          </span>
          <span class="min-w-0 flex-1">
            <span class="block truncate text-xs font-bold text-white">{{ track.title }}</span>
            <span class="block truncate text-[11px] text-[#777]">{{ track.artist }}</span>
          </span>
        </button>
      </div>
    </div>

    <LyricCardModal
      :open="cardOpen"
      :lines="cardLines"
      :title="player.currentTrack.title || ''"
      :artist="player.currentTrack.artist || ''"
      :cover-url="player.currentTrack.cover || ''"
      @close="cardOpen = false"
    />
  </aside>
</template>
