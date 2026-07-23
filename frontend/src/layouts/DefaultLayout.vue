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
  <div class="h-screen overflow-hidden bg-[#090909] text-white">
    <div class="grid h-[calc(100vh-6rem)] grid-cols-1 lg:grid-cols-[240px_minmax(0,1fr)] xl:grid-cols-[240px_minmax(0,1fr)_320px]">
      <LeftSidebar :mobile-open="navOpen" @close="navOpen = false" />
      <section class="min-w-0 overflow-hidden bg-[#0d0d0d]">
        <TopHeader @toggle-nav="navOpen = true" @toggle-info="infoOpen = true" />
        <main class="h-[calc(100%-4.5rem)] overflow-y-auto">
          <slot />
        </main>
      </section>
      <RightSidebar :mobile-open="infoOpen" @close="infoOpen = false" />
    </div>
    <BottomPlayer />
  </div>
</template>
