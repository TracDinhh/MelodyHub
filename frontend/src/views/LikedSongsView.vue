<script setup>
import { computed, onMounted } from 'vue';
import { Heart, LoaderCircle, Play } from '@lucide/vue';
import TrackRow from '../components/music/TrackRow.vue';
import { usePlayerStore } from '../stores/player.store';
import { useLibraryStore } from '../stores/library.store';

const player = usePlayerStore();
const library = useLibraryStore();

const likedPlayerTracks = computed(() => library.likedSongs.map((song) => ({
  id: song.id,
  slug: song.slug,
  title: song.title,
  cover: song.coverUrl,
  artist: (song.artists || []).map((artist) => artist.name).join(', '),
  album: '',
  plays: song.playCount?.toLocaleString() || '',
  duration: song.durationSec || 0,
  audioUrl: song.audioUrl,
  lyricsType: song.lyricsType || 'PLAIN'
})));

function playLikedSongs() {
  if (likedPlayerTracks.value.length) {
    player.playTrack(likedPlayerTracks.value[0], likedPlayerTracks.value);
  }
}

function handleLikedChange({ songId, liked }) {
  if (!liked) library.removeLikedSong(songId);
}

onMounted(() => library.loadLikedSongs());
</script>

<template>
  <main class="mx-auto max-w-[1260px] px-4 py-8 sm:px-7">
    <header class="border-b border-white/[0.06] pb-7 sm:flex sm:items-end sm:justify-between">
      <div>
        <p class="melodyhub-kicker">YOUR LIBRARY</p>
        <div class="mt-2 flex items-center gap-3">
          <span class="grid size-11 place-items-center rounded-full bg-[#3DDE7C]/15 text-[#3DDE7C]"><Heart :size="20" class="fill-current" /></span>
          <div>
            <h1 class="text-2xl font-black text-white sm:text-3xl">Liked songs</h1>
            <p class="mt-1 text-xs text-[#87918a]">{{ library.likedTotal }} {{ library.likedTotal === 1 ? 'song' : 'songs' }} saved to your library</p>
          </div>
        </div>
      </div>
      <button
        v-if="likedPlayerTracks.length"
        class="mt-5 inline-flex h-10 items-center gap-2 rounded-full bg-[#65e78c] px-5 text-xs font-black text-[#071108] sm:mt-0"
        @click="playLikedSongs"
      >
        <Play :size="15" class="fill-current" /> Play liked songs
      </button>
    </header>

    <div v-if="library.isLoadingLikedSongs" class="flex min-h-36 items-center justify-center text-sm text-[#87918a]">
      <LoaderCircle :size="18" class="mr-2 animate-spin text-[#65e78c]" /> Loading liked songs
    </div>
    <div v-else-if="library.likedSongsError" class="py-8 text-sm text-red-300">
      {{ library.likedSongsError }}
      <button class="ml-2 text-xs font-bold text-[#65e78c] hover:underline" @click="library.loadLikedSongs">Retry</button>
    </div>
    <div v-else-if="likedPlayerTracks.length" class="overflow-x-auto py-4">
      <div class="mb-2 grid min-w-[680px] grid-cols-[40px_minmax(240px,1.5fr)_minmax(130px,0.8fr)_100px_40px_40px_40px] gap-3 px-3 text-[10px] font-black tracking-wider text-[#606060]">
        <span>#</span><span>TITLE</span><span>PLAYS</span><span>DURATION</span><span></span><span></span><span></span>
      </div>
      <TrackRow
        v-for="(track, index) in likedPlayerTracks"
        :key="track.id"
        :track="track"
        :index="index"
        :song-slug="track.slug"
        @liked-change="handleLikedChange"
      />
    </div>
    <div v-else class="py-10 text-sm text-[#87918a]">Songs you like will appear here.</div>
  </main>
</template>
