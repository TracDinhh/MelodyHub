<script setup>
import { ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { LayoutDashboard, LogOut, Menu, Mic2, ShieldCheck, UserCheck, Users, X } from '@lucide/vue';
import { useAuthStore } from '../stores/auth.store';
import logoUrl from '../assets/styles/icons/logo.png';

const authStore = useAuthStore();
const router = useRouter();
const navOpen = ref(false);

const navItems = [
  { label: 'Overview', route: 'admin-dashboard', icon: LayoutDashboard },
  { label: 'Artist Requests', route: 'admin-artist-requests', icon: UserCheck },
  { label: 'Users', route: 'admin-users', icon: Users },
  { label: 'Artists', route: 'admin-artists', icon: Mic2 }
];

async function logout() {
  await authStore.logout();
  router.push({ name: 'login' });
}
</script>

<template>
  <div class="h-screen overflow-hidden bg-[#090909] text-white">
    <div class="grid h-screen grid-cols-1 lg:grid-cols-[240px_minmax(0,1fr)]">
      <!-- Sidebar -->
      <div
        v-if="navOpen"
        class="fixed inset-0 z-40 bg-black/70 backdrop-blur-sm lg:hidden"
        @click="navOpen = false"
      />
      <aside
        class="fixed inset-y-0 left-0 z-50 flex w-60 flex-col border-r border-white/5 bg-[#0b0b0b] transition-transform duration-300 lg:static lg:z-auto lg:translate-x-0"
        :class="navOpen ? 'translate-x-0' : '-translate-x-full'"
      >
        <header class="flex h-18 items-center justify-between px-5">
          <img :src="logoUrl" alt="MelodyHub logo" class="h-12 w-40 object-contain object-left" />
          <button class="melodyhub-icon-btn lg:hidden" title="Close menu" @click="navOpen = false">
            <X :size="19" />
          </button>
        </header>

        <nav class="mt-4 space-y-1 px-3" aria-label="Admin navigation">
          <RouterLink
            v-for="item in navItems"
            :key="item.route"
            :to="{ name: item.route }"
            class="group relative flex h-11 items-center gap-3 rounded-md px-3 text-sm font-semibold text-[#8f8f8f] transition hover:bg-white/5 hover:text-white"
            active-class="bg-white/[0.06] !text-[#1DB954] before:absolute before:-left-3 before:h-6 before:w-0.5 before:rounded-r before:bg-[#1DB954]"
            @click="navOpen = false"
          >
            <component :is="item.icon" :size="18" />
            <span>{{ item.label }}</span>
          </RouterLink>
        </nav>

        <div class="mt-auto border-t border-white/5 p-4">
          <div class="mb-3 flex items-center gap-2 text-xs text-[#888]">
            <ShieldCheck :size="15" class="text-[#1DB954]" />
            <span class="truncate">{{ authStore.displayName }}</span>
          </div>
          <button
            class="inline-flex h-9 w-full items-center justify-center gap-2 rounded-full border border-white/15 text-xs font-bold text-[#bbb] transition hover:border-red-400/60 hover:text-red-300"
            @click="logout"
          >
            <LogOut :size="15" /> Sign out
          </button>
        </div>
      </aside>

      <!-- Content -->
      <section class="min-w-0 overflow-hidden bg-[#0d0d0d]">
        <header class="flex h-18 items-center gap-3 border-b border-white/5 px-5 lg:hidden">
          <button class="melodyhub-icon-btn" title="Open menu" @click="navOpen = true">
            <Menu :size="20" />
          </button>
          <span class="text-sm font-black tracking-[0.16em]">MELODY HUB ADMIN</span>
        </header>
        <main class="h-[calc(100%-4.5rem)] overflow-y-auto lg:h-full">
          <slot />
        </main>
      </section>
    </div>
  </div>
</template>
