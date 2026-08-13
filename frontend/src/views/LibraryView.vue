<script setup>
import { Heart, History, ListMusic } from '@lucide/vue';
</script>

<template>
  <main class="mx-auto max-w-[1260px] px-4 py-8 sm:px-7">
    <header class="border-b border-white/[0.06] pb-7">
      <p class="melodyhub-kicker">YOUR LIBRARY</p>
      <h1 class="mt-2 text-3xl font-black text-white sm:text-4xl">My library</h1>
      <p class="mt-2 text-sm text-[#87918a]">Keep the music and collections you want to return to close at hand.</p>
    </header>

    <nav class="mt-6 grid gap-3 sm:grid-cols-3" aria-label="Library sections">
      <RouterLink
        :to="{ name: 'liked-songs' }"
        class="group flex min-h-32 flex-col justify-between border border-white/[0.08] bg-white/[0.02] p-5 transition hover:border-[#65e78c]/45 hover:bg-white/[0.045]"
      >
        <span class="grid size-10 place-items-center rounded-full bg-[#3DDE7C]/15 text-[#65e78c]"><Heart :size="18" class="fill-current" /></span>
        <span>
          <span class="block text-sm font-black text-white group-hover:text-[#8be8a8]">Liked songs</span>
          <span class="mt-1 block text-xs text-[#87918a]">Songs saved to your library</span>
        </span>
      </RouterLink>

      <RouterLink
        :to="{ name: 'library-history' }"
        class="group flex min-h-32 flex-col justify-between border border-white/[0.08] bg-white/[0.02] p-5 transition hover:border-[#65e78c]/45 hover:bg-white/[0.045]"
      >
        <span class="grid size-10 place-items-center rounded-full bg-sky-400/10 text-sky-300"><History :size="19" /></span>
        <span>
          <span class="block text-sm font-black text-white group-hover:text-[#8be8a8]">Listen history</span>
          <span class="mt-1 block text-xs text-[#87918a]">Recently played songs</span>
        </span>
      </RouterLink>

      <RouterLink
        :to="{ name: 'playlists' }"
        class="group flex min-h-32 flex-col justify-between border border-white/[0.08] bg-white/[0.02] p-5 transition hover:border-[#65e78c]/45 hover:bg-white/[0.045]"
      >
        <span class="grid size-10 place-items-center rounded-full bg-amber-300/10 text-amber-200"><ListMusic :size="19" /></span>
        <span>
          <span class="block text-sm font-black text-white group-hover:text-[#8be8a8]">Playlists</span>
          <span class="mt-1 block text-xs text-[#87918a]">Your saved collections</span>
        </span>
      </RouterLink>
    </nav>
  </main>
</template>
