<script setup>
import { ArrowLeft, Disc3 } from '@lucide/vue';
import BaseButton from '../components/ui/BaseButton.vue';
</script>

<template>
  <section class="not-found">
    <span class="not-found__mark"><Disc3 :size="34" /></span>
    <p class="eyebrow">404</p>
    <h1>Page not found</h1>
    <p>The page you requested is not available.</p>
    <RouterLink :to="{ name: 'home' }" custom v-slot="{ navigate }">
      <BaseButton @click="navigate">
        <template #icon><ArrowLeft :size="18" /></template>
        Back to dashboard
      </BaseButton>
    </RouterLink>
  </section>
</template>
