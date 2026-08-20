<script setup>
import { RouterLink } from 'vue-router';
import { FileClock, Music2, Plus } from '@lucide/vue';
import { useStudioStore } from '../../stores/studio.store';

const studioStore = useStudioStore();
</script>

<template>
  <div class="mx-auto w-full max-w-3xl px-5 py-12 pb-20 sm:px-8">
    <div class="mb-10">
      <p class="melodyhub-kicker">ARTIST STUDIO</p>
      <h1 class="melodyhub-section-title">Get Artist Access</h1>
      <p class="mt-3 text-sm leading-6 text-[#999]">
        You're not managing any artists yet. Choose how you'd like to get started.
      </p>
    </div>

    <div class="grid gap-5 sm:grid-cols-2">
      <RouterLink
        :to="{ name: 'studio-claim' }"
        class="group flex flex-col gap-4 border border-white/10 bg-[#111827] p-6 transition hover:border-[#16C65A]/40 hover:bg-[#111827]/80"
      >
        <span class="grid size-12 place-items-center rounded-xl bg-[#16C65A]/10 text-[#16C65A]">
          <Music2 :size="24" />
        </span>
        <span>
          <span class="block text-lg font-bold text-white">Claim an existing artist</span>
          <span class="mt-2 block text-sm leading-6 text-[#999]">
            Search the catalog for your artist page and request access to manage it.
          </span>
        </span>
        <span class="mt-auto inline-flex items-center gap-2 text-xs font-black text-[#16C65A]">
          CLAIM ARTIST →
        </span>
      </RouterLink>

      <RouterLink
        :to="{ name: 'studio-create' }"
        class="group flex flex-col gap-4 border border-white/10 bg-[#111827] p-6 transition hover:border-[#16C65A]/40 hover:bg-[#111827]/80"
      >
        <span class="grid size-12 place-items-center rounded-xl bg-[#8b5cf6]/10 text-[#a78bfa]">
          <Plus :size="24" />
        </span>
        <span>
          <span class="block text-lg font-bold text-white">Create a new artist</span>
          <span class="mt-2 block text-sm leading-6 text-[#999]">
            Start a brand-new artist profile from scratch and submit it for review.
          </span>
        </span>
        <span class="mt-auto inline-flex items-center gap-2 text-xs font-black text-[#a78bfa]">
          CREATE ARTIST →
        </span>
      </RouterLink>
    </div>

    <div v-if="studioStore.myArtists.length > 0" class="mt-8">
      <RouterLink
        :to="{ name: 'studio-entry' }"
        class="text-xs font-bold text-[#16C65A] hover:underline"
      >
        ← Back to my artists
      </RouterLink>
    </div>

    <div class="mt-8 border-t border-white/5 pt-6">
      <RouterLink
        :to="{ name: 'studio-requests' }"
        class="inline-flex items-center gap-2 text-sm font-bold text-[#bbb] transition hover:text-[#16C65A]"
      >
        <FileClock :size="16" /> Check the status of a previous request
      </RouterLink>
    </div>
  </div>
</template>