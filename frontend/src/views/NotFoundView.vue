<script setup>
import { ArrowLeft, Disc3 } from '@lucide/vue';
import BaseButton from '../components/ui/BaseButton.vue';
</script>

<template>
  <section class="mx-auto max-w-md text-center text-white">
    <span class="mx-auto grid size-16 place-items-center rounded-full bg-[#1DB954] text-black"><Disc3 :size="34" /></span>
    <p class="mt-6 text-xs font-black tracking-[0.2em] text-[#1DB954]">404</p>
    <h1 class="mt-2 text-3xl font-black">Page not found</h1>
    <p class="mt-2 text-sm text-[#777]">The page you requested is not available.</p>
    <RouterLink :to="{ name: 'home' }" custom v-slot="{ navigate }">
      <BaseButton class="mt-6" @click="navigate">
        <template #icon><ArrowLeft :size="18" /></template>
        Back to dashboard
      </BaseButton>
    </RouterLink>
  </section>
</template>
