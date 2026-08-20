<script setup>
import { onMounted, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { ChevronRight, FileClock, LoaderCircle, Mic2, RefreshCw } from '@lucide/vue';
import { useStudioStore } from '../../stores/studio.store';

const router = useRouter();
const studioStore = useStudioStore();

const resolved = ref(false);

async function load() {
  resolved.value = false;
  try {
    const artists = await studioStore.loadMyArtists(true);
    if (artists.length === 0) {
      router.replace({ name: 'studio-access' });
    } else if (artists.length === 1) {
      router.replace({ name: 'studio-artist-overview', params: { artistId: artists[0].artistId } });
    }
  } finally {
    resolved.value = true;
  }
}

function openArtist(artistId) {
  router.push({ name: 'studio-artist-overview', params: { artistId } });
}

onMounted(load);
</script>

<template>
  <div class="mx-auto w-full max-w-3xl px-5 py-12 pb-20 sm:px-8">
    <div class="mb-8">
      <p class="melodyhub-kicker">ARTIST STUDIO</p>
      <h1 class="melodyhub-section-title">Welcome</h1>
      <p class="mt-3 text-sm leading-6 text-[#999]">Pick the artist you want to manage.</p>
    </div>

    <div v-if="studioStore.isLoading && !resolved" class="flex min-h-48 items-center justify-center text-sm text-[#888]">
      <LoaderCircle :size="20" class="mr-3 animate-spin text-[#16C65A]" /> Loading your artists
    </div>

    <p v-else-if="studioStore.error" class="mb-4 rounded-md bg-red-500/10 px-3 py-2 text-xs text-red-300" role="alert">
      {{ studioStore.error }}
    </p>

    <div v-else-if="resolved" class="space-y-3">
      <button
        v-for="artist in studioStore.myArtists"
        :key="artist.artistId"
        type="button"
        class="flex w-full items-center gap-4 border border-white/10 bg-[#111827] p-4 text-left transition hover:border-[#16C65A]/40 hover:bg-[#111827]/80"
        @click="openArtist(artist.artistId)"
      >
        <span class="grid size-12 shrink-0 place-items-center overflow-hidden rounded-lg bg-[#16C65A]/15 text-[#16C65A]">
          <img v-if="artist.imageUrl" :src="artist.imageUrl" :alt="artist.name" class="h-full w-full object-cover" />
          <Mic2 v-else :size="22" />
        </span>
        <span class="min-w-0 flex-1">
          <span class="block truncate text-base font-bold text-white">{{ artist.name }}</span>
          <span class="block text-xs text-[#71717A]">@{{ artist.slug }}</span>
        </span>
        <span class="rounded-full px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider" :class="artist.memberRole === 'OWNER' ? 'bg-[#16C65A]/15 text-[#16C65A]' : 'bg-white/10 text-[#bbb]'">
          {{ artist.memberRole }}
        </span>
        <ChevronRight :size="18" class="shrink-0 text-[#71717A]" />
      </button>

      <div class="mt-8 flex flex-wrap items-center gap-4">
        <RouterLink
          :to="{ name: 'studio-access' }"
          class="inline-flex h-10 items-center gap-2 rounded-full border border-white/15 px-5 text-xs font-bold text-[#bbb] transition hover:border-[#16C65A]/70 hover:text-white"
        >
          <Mic2 :size="15" /> Get access to another artist
        </RouterLink>
        <RouterLink
          :to="{ name: 'studio-requests' }"
          class="inline-flex h-10 items-center gap-2 rounded-full border border-white/15 px-5 text-xs font-bold text-[#bbb] transition hover:border-[#16C65A]/70 hover:text-white"
        >
          <FileClock :size="15" /> Request status
        </RouterLink>
        <button
          class="inline-flex h-10 items-center gap-2 rounded-full border border-white/15 px-4 text-xs font-bold text-[#bbb] transition hover:border-[#16C65A]/70 hover:text-white"
          @click="load"
        >
          <RefreshCw :size="14" /> Refresh
        </button>
      </div>
    </div>
  </div>
</template>