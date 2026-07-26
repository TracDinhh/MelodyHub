<script setup>
import { computed } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { ArrowLeft, ShieldCheck, Sparkles } from '@lucide/vue';

const route = useRoute();
const isAdminWorkspace = computed(() => route.meta.workspace === 'admin');
const title = computed(() => (isAdminWorkspace.value ? 'Admin access is active' : 'Artist access is active'));
const Icon = computed(() => (isAdminWorkspace.value ? ShieldCheck : Sparkles));
</script>

<template>
  <div class="mx-auto flex min-h-[60vh] w-full max-w-3xl items-center px-5 py-10 sm:px-8">
    <section class="border border-white/10 bg-[#121212] p-7 sm:p-9">
      <component :is="Icon" :size="30" class="text-[#1DB954]" />
      <p class="sonix-kicker mt-6">{{ isAdminWorkspace ? 'ADMIN' : 'ARTIST' }}</p>
      <h1 class="sonix-section-title">{{ title }}</h1>
      <p class="mt-3 max-w-xl text-sm leading-6 text-[#999]">Your workspace will appear here as its next phase is delivered.</p>
      <RouterLink :to="{ name: 'home' }" class="mt-6 inline-flex h-10 items-center gap-2 rounded-full border border-white/15 px-5 text-xs font-bold text-[#bbb] transition hover:border-[#1DB954]/70 hover:text-white">
        <ArrowLeft :size="16" /> Back to Home
      </RouterLink>
    </section>
  </div>
</template>
