<script setup>
import { ref } from 'vue';
import BottomPlayer from '../components/player/BottomPlayer.vue';
import LeftSidebar from '../components/navigation/LeftSidebar.vue';
import RightSidebar from '../components/player/RightSidebar.vue';
import TopHeader from '../components/navigation/TopHeader.vue';

const navOpen = ref(false);
const infoOpen = ref(false);
</script>

<template>
  <div class="melodyhub-app-shell h-screen overflow-hidden p-2 text-white sm:p-3">
    <div class="grid h-[calc(100vh-7rem)] grid-cols-1 gap-2 lg:grid-cols-[224px_minmax(0,1fr)] xl:grid-cols-[224px_minmax(0,1fr)_312px]">
      <LeftSidebar :mobile-open="navOpen" @close="navOpen = false" />
      <section class="melodyhub-main-panel melodyhub-panel-edge min-w-0 overflow-hidden rounded-2xl">
        <TopHeader @toggle-nav="navOpen = true" @toggle-info="infoOpen = true" />
        <main class="h-[calc(100%-3.75rem)] overflow-y-auto rounded-b-2xl">
          <slot />
        </main>
      </section>
      <RightSidebar :mobile-open="infoOpen" @close="infoOpen = false" />
    </div>
    <BottomPlayer />
  </div>
</template>
