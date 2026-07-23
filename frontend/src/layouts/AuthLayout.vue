<script setup>
import { Disc3 } from '@lucide/vue';
</script>

<template>
  <main class="auth-layout">
    <div class="auth-layout__brand">
      <span class="brand__mark"><Disc3 :size="24" aria-hidden="true" /></span>
      <span>MelodyHub</span>
    </div>
    <div class="auth-layout__content">
      <slot />
    </div>
  </main>
</template>
