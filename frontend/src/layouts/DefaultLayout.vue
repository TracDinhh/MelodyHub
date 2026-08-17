<script setup>
import { ref } from 'vue';
import BottomPlayer from '../components/player/BottomPlayer.vue';
import LeftSidebar from '../components/navigation/LeftSidebar.vue';
import TopHeader from '../components/navigation/TopHeader.vue';

const navOpen = ref(false);
</script>

<template>
  <div class="melodyhub-app-shell h-screen overflow-hidden text-white">
    <div class="grid min-h-0 h-[calc(100vh-6rem)] grid-cols-1 sm:h-[calc(100vh-6.5rem)] lg:grid-cols-[240px_minmax(0,1fr)]">
      <LeftSidebar :mobile-open="navOpen" @close="navOpen = false" />
      <section class="melodyhub-main-panel melodyhub-panel-edge min-h-0 min-w-0 overflow-hidden lg:rounded-tl-3xl">
        <TopHeader @toggle-nav="navOpen = true" />
        <main class="h-[calc(100%-4.5rem)] overflow-y-auto">
          <slot />
        </main>
      </section>
    </div>
    <BottomPlayer />
  </div>
</template>
