<script setup>
import { RouterLink } from 'vue-router';
import { ArrowRight, BarChart3, ChevronDown, Heart, Mic2, Music2, Upload } from '@lucide/vue';
import logoUrl from '../../assets/styles/icons/logo.png';
</script>

<template>
  <div class="min-h-screen bg-[#09090B] text-white">
    <!-- Navbar -->
    <header class="fixed inset-x-0 top-0 z-50 border-b border-white/5 bg-[#09090B]/80 backdrop-blur-xl">
      <div class="mx-auto flex h-16 max-w-6xl items-center justify-between px-5">
        <RouterLink :to="{ name: 'home' }">
          <img :src="logoUrl" alt="MelodyHub logo" class="h-10 w-32 object-contain object-left" />
        </RouterLink>
        <div class="flex items-center gap-3">
          <RouterLink
            :to="{ name: 'home' }"
            class="hidden text-sm font-medium text-[#71717A] transition hover:text-white sm:inline"
          >Listen on MelodyHub</RouterLink>
          <RouterLink
            :to="{ name: 'artist-login' }"
            class="inline-flex h-10 items-center gap-2 rounded-full bg-[#16C65A] px-5 text-sm font-bold text-black transition hover:bg-[#22C55E]"
          >
            <Mic2 :size="16" /> Login as Artist
          </RouterLink>
        </div>
      </div>
    </header>

    <!-- Hero -->
    <section class="relative isolate flex min-h-[85vh] items-center pt-16">
      <div class="absolute inset-0 bg-[radial-gradient(circle_at_30%_20%,rgba(22,198,90,0.18),transparent_50%),radial-gradient(circle_at_80%_70%,rgba(139,92,246,0.14),transparent_50%)]" />
      <div class="relative mx-auto max-w-6xl px-5 py-20 text-center sm:py-28">
        <div class="mx-auto mb-6 inline-flex items-center gap-2 rounded-full border border-[#16C65A]/30 bg-[#16C65A]/10 px-4 py-1.5 text-xs font-bold text-[#16C65A]">
          <Mic2 :size="14" /> MelodyHub for Artists
        </div>
        <h1 class="mx-auto max-w-3xl text-4xl font-extrabold leading-[1.08] tracking-tight text-[#F4FFF7] sm:text-6xl lg:text-7xl">
          Share your music<br />
          <span class="bg-gradient-to-r from-[#16C65A] to-[#06b6d4] bg-clip-text text-transparent">with the world</span>
        </h1>
        <p class="mx-auto mt-6 max-w-xl text-base leading-7 text-[#A1A1AA] sm:text-lg">
          Upload tracks, track your stats, and grow your audience — all in one place.
        </p>
        <div class="mt-10 flex flex-wrap items-center justify-center gap-4">
          <RouterLink
            :to="{ name: 'artist-login' }"
            class="inline-flex h-12 items-center gap-2.5 rounded-full bg-[#16C65A] px-7 text-sm font-bold text-black transition hover:scale-[1.03] hover:bg-[#22C55E] active:scale-95"
          >
            Get Started <ArrowRight :size="16" />
          </RouterLink>
          <a
            href="#features"
            class="inline-flex h-12 items-center gap-2 rounded-full border border-white/10 px-6 text-sm font-semibold text-[#F4FFF7] transition hover:border-[#16C65A]/50 hover:bg-white/[0.03]"
          >
            Learn More <ChevronDown :size="16" />
          </a>
        </div>
      </div>
    </section>

    <!-- Features -->
    <section id="features" class="border-t border-white/5 py-20 sm:py-28">
      <div class="mx-auto max-w-6xl px-5">
        <p class="text-center text-xs font-bold uppercase tracking-[0.2em] text-[#16C65A]">Why MelodyHub?</p>
        <h2 class="mt-3 text-center text-3xl font-bold text-[#F4FFF7] sm:text-4xl">Everything you need to grow</h2>

        <div class="mt-14 grid gap-6 sm:grid-cols-3">
          <div class="rounded-2xl border border-white/[0.06] bg-[#121214] p-7 transition hover:border-[#16C65A]/30">
            <div class="mb-5 grid size-12 place-items-center rounded-xl bg-[#16C65A]/10 text-[#16C65A]">
              <BarChart3 :size="24" />
            </div>
            <h3 class="text-lg font-bold text-[#F4FFF7]">Real-time Analytics</h3>
            <p class="mt-2 text-sm leading-6 text-[#71717A]">See how your songs are performing every day — plays, likes, trends at a glance.</p>
          </div>

          <div class="rounded-2xl border border-white/[0.06] bg-[#121214] p-7 transition hover:border-[#16C65A]/30">
            <div class="mb-5 grid size-12 place-items-center rounded-xl bg-[#8b5cf6]/10 text-[#a78bfa]">
              <Upload :size="24" />
            </div>
            <h3 class="text-lg font-bold text-[#F4FFF7]">Easy Upload</h3>
            <p class="mt-2 text-sm leading-6 text-[#71717A]">Drag & drop your tracks and cover art. Synced lyrics editor included.</p>
          </div>

          <div class="rounded-2xl border border-white/[0.06] bg-[#121214] p-7 transition hover:border-[#16C65A]/30">
            <div class="mb-5 grid size-12 place-items-center rounded-xl bg-[#f43f5e]/10 text-[#fb7185]">
              <Heart :size="24" />
            </div>
            <h3 class="text-lg font-bold text-[#F4FFF7]">Fan Insights</h3>
            <p class="mt-2 text-sm leading-6 text-[#71717A]">Know who loves your music. See likes, follows, and listening patterns.</p>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="border-t border-white/5 py-20 sm:py-28">
      <div class="mx-auto max-w-2xl px-5 text-center">
        <div class="mx-auto mb-5 grid size-16 place-items-center rounded-full bg-[#16C65A]/10">
          <Music2 :size="28" class="text-[#16C65A]" />
        </div>
        <h2 class="text-3xl font-bold text-[#F4FFF7] sm:text-4xl">Ready to share your sound?</h2>
        <p class="mt-4 text-base text-[#71717A]">
          Join MelodyHub as an artist and start reaching your audience today.
        </p>
        <div class="mt-8 flex flex-wrap items-center justify-center gap-4">
          <RouterLink
            :to="{ name: 'artist-login' }"
            class="inline-flex h-12 items-center gap-2 rounded-full bg-[#16C65A] px-7 text-sm font-bold text-black transition hover:bg-[#22C55E]"
          >
            <Mic2 :size="16" /> Login to Artist Studio
          </RouterLink>
          <RouterLink
            :to="{ name: 'studio-entry' }"
            class="inline-flex h-12 items-center gap-2 rounded-full border border-white/10 px-6 text-sm font-semibold text-[#A1A1AA] transition hover:border-[#16C65A]/50 hover:text-white"
          >
            Get Artist Access <ArrowRight :size="15" />
          </RouterLink>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="border-t border-white/5 py-8 text-center text-xs text-[#52525B]">
      <p>© 2026 MelodyHub · Built for Artists</p>
    </footer>
  </div>
</template>
